package shop.zailushang.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shop.zailushang.entity.Chapter;
import shop.zailushang.flow.FlowEngine;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 组件：发送请求，获取响应文本
 */
public interface Reader<T, R> extends Task<T, R> {

    @Override
    default CompletableFuture<R> execute(T param) {
        return read(param);
    }

    CompletableFuture<R> read(T param);

    // 发送请求,获取响应文本
    static CompletableFuture<String> read0(String uri) {
        try (var httpClient = HttpClient.newBuilder().build()) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body);
        }
    }

    /**
     * 工厂类： 使用静态工厂 && 策略
     * 这里甚至还是"单例"，使用 lambda 表达式创建对象，在不依赖外部状态的情况下，始终为同一个对象
     * 所以，即使多次调用 bidReader(),返回的也是同一个对象,一旦涉及到外部状态，那就会重复生成对象了
     * 后续：{@link Selector.Selectors} {@link Parser.Parsers} {@link Decoder.Decoders}
     * {@link Formatter.Formatters} {@link Writer.Writes} 同理，不再重复注释
     */
    class Readers {
        private static final Logger logger = LoggerFactory.getLogger(Readers.class);

        // 获取bid的http请求器
        public static Reader<String, String> bidReader() {
            // 获取BID的请求地址
            final var bidUriFormatter = "https://www.shuqi.com/search?keyword=%s&page=1";
            return bookName -> {
                var bidUri = bidUriFormatter.formatted(bookName);
                logger.info("{} - 执行获取bid操作 url => {}", Thread.currentThread().getName(), bidUri);
                return CompletableFuture.completedFuture(bidUri)
                        .thenCompose(Reader::read0);
            };
        }

        // 获取章节列表的http请求器
        public static Reader<String, String> chapterReader() {
            // 获取章节列表的请求地址
            final var chapterUriFormatter = "https://www.shuqi.com/reader?bid=%s";
            return bid -> {
                var chapterUri = chapterUriFormatter.formatted(bid);
                logger.info("{} - 执行获取章节列表操作 url => {}", Thread.currentThread().getName(), chapterUri);
                return CompletableFuture.completedFuture(chapterUri)
                        .thenCompose(Reader::read0);
            };
        }

        // 获取章节内容的http请求器
        public static Reader<Chapter.Chapter4Download, String> contentReader() {
            // 获取章节内容请求地址
            var contentUriFormatter = "https://c13.shuqireader.com/pcapi/chapter/contentfree/%s";
            return chapter4Download -> {
                var contentUri = contentUriFormatter.formatted(chapter4Download.contUrlSuffix());
                logger.info("{} - 执行获取章节内容操作 url => {}", Thread.currentThread().getName(), contentUri);
                // 异步执行
                return CompletableFuture.completedFuture(contentUri)
                        .thenApplyAsync(uri -> {
                            try {
                                // 流控 -- start
                                // 控制最大并发数
                                FlowEngine.SEMAPHORE.acquire();
                                // 休眠1s
                                TimeUnit.SECONDS.sleep(1);
                                // 流控 -- end
                                return read0(uri);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            } finally {
                                FlowEngine.SEMAPHORE.release();
                            }
                        }, FlowEngine.IO_TASK_EXECUTOR)
                        // 很low的传参方式，但后续执行下载时需要需要这些参数，只能由此往后传递
                        // 拼接后续流程所需参数：书名/章节序号.txt 作为章节文件名
                        // 取值时是逆序，后来居上
                        .thenApply(CompletableFuture::join)
                        .thenApply(jsonStr -> String.format("%s#%s", chapter4Download.chapterOrdid(), jsonStr))// [2]
                        .thenApply(jsonStr -> String.format("%s#%s", chapter4Download.chapterName(), jsonStr))// [1]
                        .thenApply(jsonStr -> String.format("%s#%s", chapter4Download.bookName(), jsonStr));// [0]
            };
        }
    }
}