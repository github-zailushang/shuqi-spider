package shop.zailushang.component;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.entity.Chapter;
import shop.zailushang.flow.FlowEngine;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 组件：发送请求，获取响应文本
 */
@FunctionalInterface
public interface Reader<T, R> extends Task<T, R> {
    @Override
    default CompletableFuture<R> execute(T param) {
        return read(param);
    }

    CompletableFuture<R> read(T param);

    // 发送请求,获取响应文本
    static CompletableFuture<String> read0(String uri) {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                //.header("cookie", "") // 此处添加 VIP账号权限
                .build();

        return FlowEngine.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(HttpResponse::body, FlowEngine.IO_TASK_EXECUTOR);
    }

    // 流控专员
    static <T, R> Reader<T, R> rateLimitedRead(Reader<? super T, ? extends R> innerReader, long timeout) {
        return uri -> {
            // 流控 -- start
            try {
                // 别改！别改！别改！后果自负！！！
                FlowEngine.SEMAPHORE.acquire();
                // 直接休眠指定秒数，这里就不额外计算了，徒添复杂度
                TimeUnit.SECONDS.sleep(timeout);
                // 需阻塞等待任务结束
                var result = innerReader.execute(uri)
                        .join();
                return CompletableFuture.completedFuture(result);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                FlowEngine.SEMAPHORE.release();
            }
            // 流控 -- end
        };
    }

    // 组件名
    static String name() {
        return "「载」";
    }

    /**
     * 工厂类： 使用静态工厂 && 策略
     * 这里甚至还是"单例"，使用 lambda 表达式创建对象，在不依赖外部状态的情况下（纯函数），返回的对象始终为同一个
     * 所以，即使多次调用 bidReader(),返回的也是同一个对象,一旦涉及到外部状态，那就会重复生成对象了
     * 后续：{@link Selector.Selectors} {@link Parser.Parsers} {@link Decoder.Decoders}
     * {@link Formatter.Formatters} {@link Writer.Writers} 同理，不再重复注释
     */
    @Slf4j
    class Readers {

        static {
            log.info("敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}", Reader.name());
        }

        // 获取bid的http请求器
        public static Reader<String, String> bidReader() {
            // 获取BID的请求地址
            final var bidUriFormatter = "https://www.shuqi.com/search?keyword=%s&page=1";
            return bookName -> {
                var bidUri = bidUriFormatter.formatted(bookName);
                log.info("{} - 执行获取bid操作 url => {}", Reader.name(), bidUri);
                return CompletableFuture.completedFuture(bidUri)
                        .thenComposeAsync(Reader::read0, FlowEngine.IO_TASK_EXECUTOR);
            };
        }

        // 获取章节列表的http请求器
        public static Reader<String, String> chapterReader() {
            // 获取章节列表的请求地址
            final var chapterUriFormatter = "https://www.shuqi.com/reader?bid=%s";
            return bid -> {
                var chapterUri = chapterUriFormatter.formatted(bid);
                log.info("{} - 执行获取章节列表操作 url => {}", Reader.name(), chapterUri);
                return CompletableFuture.completedFuture(chapterUri)
                        .thenComposeAsync(Reader::read0, FlowEngine.IO_TASK_EXECUTOR);
            };
        }

        // 获取章节内容的http请求器
        public static Reader<Chapter.Chapter4Read, Chapter.Chapter4Select> contentReader() {
            // 获取章节内容请求地址
            final var contentUriFormatter = "https://c13.shuqireader.com/pcapi/chapter/contentfree/%s";
            return chapter4Read -> {
                var contentUri = contentUriFormatter.formatted(chapter4Read.contUrlSuffix());
                log.info("{} - 执行获取章节内容操作 url => {}", Reader.name(), contentUri);
                var bookName = chapter4Read.bookName();
                var chapterName = chapter4Read.chapterName();
                var chapterOrdid = chapter4Read.chapterOrdid();

                // 流控移至专员处理 rateLimitedRead
                return CompletableFuture.completedFuture(contentUri)
                        .thenComposeAsync(Reader.<String, String>rateLimitedRead(Reader::read0, FlowEngine.TIMEOUT)::execute, FlowEngine.IO_TASK_EXECUTOR)
                        .thenApplyAsync(jsonStr -> new Chapter.Chapter4Select(bookName, chapterName, chapterOrdid, jsonStr));
            };
        }
    }
}