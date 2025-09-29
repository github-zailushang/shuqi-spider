package shop.zailushang.flow;

import shop.zailushang.component.*;
import shop.zailushang.component.Formatter;
import shop.zailushang.utils.Assert;
import shop.zailushang.entity.Chapter;

import java.util.*;
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
        return head().apply(t).join();
    }

    /**
     * 将传入的类型包装成 CompletableFuture
     */
    static <T> Flow<T, T> identity() {
        return () -> CompletableFuture::completedFuture;
    }

    /**
     * 根据章节列表flow 返回的条目，来生成对应数量的，下载章节流程（因后续的下载章节流程对应的是一条章节，需要将章节列表处理成 N 条下载章节流程）
     */
    static <T> List<Flow<T, T>> startParallel(Integer size) {
        return IntStream.range(0, size)
                .mapToObj(unused -> Flow.<T>identity())
                .toList();
    }

    /**
     * 流程组装：同步调用链
     */
    @SuppressWarnings("unused")
    default <V> Flow<T, V> then(Flow<? super R, V> next) {
        Assert.isTrue(next, Assert::isNotNull, () -> new NullPointerException("If I looked compared to others far, is because I stand on giant’s shoulder. — Newton"));
        return () -> head().then(next.head());
    }

    /**
     * 流程组装：异步调用链
     */
    default <V> Flow<T, V> thenAsync(Flow<? super R, V> next) {
        Assert.isTrue(next, Assert::isNotNull, () -> new NullPointerException("If I looked compared to others far, is because I stand on giant’s shoulder. — Newton"));
        return () -> head().thenAsync(next.head());
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
                    .thenAsync(Selector.Selectors.bidSelector())
                    .thenAsync(Parser.Parsers.bidParser());
        }

        // 完整 下载章节列表 的流程组装
        public static Flow<String, List<Chapter.Chapter4Read>> chapterFlow() {
            return () -> Reader.Readers.chapterReader()
                    .thenAsync(Selector.Selectors.chapterSelector())
                    .thenAsync(Parser.Parsers.chapterParser());
        }

        // 完整 下载章节内容 的流程组装[针对所有章节内容]
        public static Flow<List<Chapter.Chapter4Read>, List<Chapter.Chapter4Merge>> contentListFlow() {
            final var contentFlow = Flow.Flows.contentFlow();
            return () ->
                    downloads -> {
                        var contentFlowStarts = Flow.<Chapter.Chapter4Read>startParallel(downloads.size());
                        final var parallelFlows = contentFlowStarts.stream()
                                //.limit(20) // 仅下载前 20章： 用于测试时，控制下载章节数量
                                .map(flow -> flow.thenAsync(contentFlow))
                                .toList();

                        // 因后面的 skip 属性强依赖排序结果来计算，此处开启并行流，会导致 skip 计算异常，表现为在后续的文件写入时，文件指针会乱序
                        // 故在此处需要提前收集源，再另行排序
                        var sources = IntStream.range(0, parallelFlows.size())
                                .parallel() // 开启并行流加速提交
                                .mapToObj(index -> parallelFlows.get(index).start(downloads.get(index)))
                                .toList();

                        var atoLong = new AtomicLong(0);
                        sources = sources.stream()
                                .sorted(Comparator.comparing(Chapter.Chapter4Merge::orderId)) // 章节按照 1 ~ N连续自然数顺序 排序
                                .map(merge -> merge.identity(merge, atoLong))// 设置 skip 属性
                                .toList();
                        return CompletableFuture.completedFuture(sources);
                    };
        }

        // 部分 下载章节内容 的流程组装[针对一条章节内容]
        public static Flow<Chapter.Chapter4Read, Chapter.Chapter4Merge> contentFlow() {
            return () -> Reader.Readers.contentReader()
                    .thenAsync(Selector.Selectors.contentSelector())
                    .thenAsync(Parser.Parsers.contentParser())
                    .thenAsync(Decoder.Decoders.contentDecoder())
                    .thenAsync(Formatter.Formatters.contentFormatter())
                    .thenAsync(Writer.Writers.fileWriter());
        }

        // 完整 合并文件 的流程组装
        public static Flow<List<Chapter.Chapter4Merge>, Void> mergeFlow() {
            return () -> Merger.Mergers.fileMerger()
                    .thenAsync(Cleaner.Cleaners.fileCleaner());
        }
    }
}