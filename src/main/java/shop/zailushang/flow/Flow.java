package shop.zailushang.flow;

import shop.zailushang.component.*;
import shop.zailushang.component.Formatter;
import shop.zailushang.utils.Assert;
import shop.zailushang.entity.Chapter;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

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
        return Task::<T>identity;
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
     * 工厂类： 使用静态工厂 && 策略 && 伪单例（但需为纯函数），下同
     * {@link Flow.Flows}       {@link Reader.Readers}      {@link Selector.Selectors}
     * {@link Parser.Parsers}   {@link Decoder.Decoders}    {@link Formatter.Formatters}
     * {@link Writer.Writers}   {@link Merger.Mergers}      {@link Cleaner.Cleaners}
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
            final var atoLong = new AtomicLong(0);
            return () ->
                    downloads -> CompletableFuture.completedFuture(downloads)
                            .thenApplyAsync(List::parallelStream, FlowEngine.IO_TASK_EXECUTOR)// 开启并行流加速提交
                            .thenApplyAsync(flowStream -> flowStream.sorted(Comparator.comparing(Chapter.Chapter4Read::chapterOrdid)), FlowEngine.IO_TASK_EXECUTOR)// 并行流不影响排序语义
                            .thenApplyAsync(flowStream -> flowStream.limit(FlowEngine.IS_TEST ? 20 : Long.MAX_VALUE), FlowEngine.IO_TASK_EXECUTOR)// 测试模式下仅下载前 20 章
                            .thenApplyAsync(flowStream -> flowStream.map(download -> Flow.<Chapter.Chapter4Read>identity().thenAsync(contentFlow).start(download)), FlowEngine.IO_TASK_EXECUTOR)
                            .thenApplyAsync(Stream::toList, FlowEngine.IO_TASK_EXECUTOR)// 提前收集源
                            .thenApplyAsync(List::stream, FlowEngine.IO_TASK_EXECUTOR)
                            .thenApplyAsync(flowStream -> flowStream.sorted(Comparator.comparing(Chapter.Chapter4Merge::orderId)), FlowEngine.IO_TASK_EXECUTOR)// 重新排序
                            .thenApplyAsync(flowStream -> flowStream.map(merge -> merge.identity(merge, atoLong)), FlowEngine.IO_TASK_EXECUTOR)// 设置 skip 属性
                            .thenApplyAsync(Stream::toList, FlowEngine.IO_TASK_EXECUTOR);
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