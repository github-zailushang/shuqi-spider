package shop.zailushang.flow;

import shop.zailushang.component.*;
import shop.zailushang.utils.Assert;
import shop.zailushang.entity.Chapter;
import shop.zailushang.utils.IOForkJoinTask;

import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/**
 * 抽象流程：组装多个 Task，形成一条任务链
 */
public interface Flow<T, R> {

    // 任务头结点
    Task<T, R> head();

    // 启动此任务
    default R start(T t) {
        return head().execute(t).join();
    }

    /**
     * 其中 CompletableFuture::completedFuture 等价于 t -> CompletableFuture.completedFuture(t)
     * 对应 flow 的头结点： 对传入的类型包装成 CompletableFuture并返回
     */
    static <T> Flow<T, T> identity() {
        return () -> CompletableFuture::completedFuture;
    }

    /**
     * 根据 章节列表flow 返回的条目，来生成对应数量的，下载章节流程
     * 因为后续的下载章节流程对应的是一条章节，需要将章节列表处理成 N 条下载章节流程
     */
    static <T> List<Flow<T, T>> startParallel(Integer size) {
        return IntStream.range(0, size)
                .mapToObj(unused -> Flow.<T>identity())
                .toList();
    }

    // 每条flow 由多个 task#then 组装而成，而 flow 和 flow 之间的 then，实则也是用 头结点的 then 来组装
    default <V> Flow<T, V> then(Flow<? super R, V> next) {
        Assert.isTrue(next, Assert::isNotNull, () -> new NullPointerException("If I looked compared to others far, is because I stand on giant’s shoulder. — Newton"));
        return () -> head().then(next.head());
    }

    /**
     * 关于流程的组装，这里想稍稍多谈一点，其实一开始想用「模板方法模式」组装多个任务成一条抽象流程，用「迭代器」组装多条流程
     * 但实际操作时，发现参数和返回值的不统一，不太可行，因为迭代需要提供统一的调用方式，强行统一的话，只能用更宽泛的类型来接受
     * 并且在使用时使用 instanceof 判断强转，我所不欲也，要将上一个处理器的返回作为下一个处理器的输入，明显更符合流式编程、管道模式
     * 用泛型来描述前后两者的关系还是比较容易的，故而选用了一种 a.then(b).then(c) 的模式来设计代码
     */
    class Flows {
        // 完整 下载bid 的流程组装
        public static Flow<String, String> bidFlow() {
            return () -> Reader.Readers.bidReader()
                    .then(Selector.Selectors.bidSelector())
                    .then(Parser.Parsers.bidParser());
        }

        // 完整 下载章节列表 的流程组装
        public static Flow<String, List<Chapter.Chapter4Download>> chapterFlow() {
            return () -> Reader.Readers.chapterReader()
                    .then(Selector.Selectors.chapterSelector())
                    .then(Parser.Parsers.chapterParser());
        }

        // 完整 下载章节内容 的流程组装[针对所有章节内容]
        public static Flow<List<Chapter.Chapter4Download>, List<Chapter.Chapter4Merge>> contentListFlow() {
            final var contentedFlow = Flow.Flows.contentFlow();
            return () ->
                    downloads -> {
                        var contentFlowStarts = Flow.<Chapter.Chapter4Download>startParallel(downloads.size());
                        final var parallelFlows = contentFlowStarts.stream()
                                //.limit(30) // 仅下载前 30章： 用于测试时，控制下载章节数量
                                .map(flow -> flow.then(contentedFlow))
                                .toList();

                        var atoLong = new AtomicLong(0);
                        var sources = IntStream.range(0, parallelFlows.size())
                                .mapToObj(index -> parallelFlows.get(index).start(downloads.get(index)))
                                .sorted(Comparator.comparing(chapter4Merge ->
                                        // 文件名的数字顺序
                                        Integer.valueOf(chapter4Merge.filePath().getFileName().toString().transform(str -> str.substring(0, str.lastIndexOf(".")))))
                                )
                                .map(merge -> {
                                    try {
                                        // 这里设置每章的 skip 跳过字节数，因为用了 recode,final 类设计，无setter可用，只能我转我自己，多了 skip : atoLong.getAndAdd(size)
                                        long size = Files.size(merge.filePath());
                                        return new Chapter.Chapter4Merge(merge.orderId(), merge.folderPath(), merge.filePath(), merge.bookName(), atoLong.getAndAdd(size));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .toList();
                        return CompletableFuture.completedFuture(sources);
                    };
        }

        // 部分 下载章节内容 的流程组装[针对一条章节内容]
        public static Flow<Chapter.Chapter4Download, Chapter.Chapter4Merge> contentFlow() {
            return () -> Reader.Readers.contentReader()
                    .then(Selector.Selectors.contentSelector())
                    .then(Parser.Parsers.contentParser())
                    .then(Decoder.Decoders.contentDecoder())
                    .then(Formatter.Formatters.contentFormatter())
                    .then(Writer.Writes.fileWrite());
        }

        // 完整 合并文件 的流程组装
        public static Flow<List<Chapter.Chapter4Merge>, IOForkJoinTask.Result> mergeFlow() {
            return Merger.Mergers::fileMerger;
        }
    }
}