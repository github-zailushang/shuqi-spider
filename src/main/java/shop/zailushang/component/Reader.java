package shop.zailushang.component;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.entity.Chapter;
import shop.zailushang.flow.FlowEngine;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * 组件：发送请求，获取响应文本
 */
@FunctionalInterface
public interface Reader<T, R> extends Task<T, R> {
    @Override
    default CompletableFuture<R> execute(T param) throws Exception {
        return read(param);
    }

    CompletableFuture<R> read(T param) throws Exception;

    // 发送请求,获取响应文本
    static CompletableFuture<String> read0(String uri) {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                //.header("cookie", "") // 此处添加 VIP账号权限
                .build();

        return FlowEngine.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(HttpResponse::body, FlowEngine.IO_TASK_EXECUTOR);
    }

    // 组件名
    static String name() {
        return "「载」";
    }

    /**
     * 工厂类： 使用静态工厂 && 策略 && 伪单例（但需为纯函数），下同
     * {@link Reader.Readers}   {@link Selector.Selectors}  {@link Parser.Parsers}
     * {@link Decoder.Decoders} {@link Formatter.Formatters} {@link Writer.Writers}
     * {@link Merger.Mergers} {@link Cleaner.Cleaners} {@link shop.zailushang.flow.Flow.Flows}
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

                // 流控移至专员处理 withRateLimit
                return CompletableFuture.completedFuture(contentUri)
                        .thenComposeAsync(Task.<String, String>withRateLimit(Reader::read0, FlowEngine.TIMEOUT), FlowEngine.IO_TASK_EXECUTOR)
                        .thenApplyAsync(jsonStr -> new Chapter.Chapter4Select(bookName, chapterName, chapterOrdid, jsonStr));
            };
        }
    }
}