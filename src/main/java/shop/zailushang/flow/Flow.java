package shop.zailushang.flow;

import shop.zailushang.component.*;
import shop.zailushang.component.Formatter;
import shop.zailushang.entity.Tao;
import shop.zailushang.util.Assert;
import shop.zailushang.entity.Chapter;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static shop.zailushang.component.Task.taskExecutor;

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
     * 一致性流程
     */
    @SuppressWarnings("unused")
    static <T> Flow<T, T> identity() {
        return Task::<T>identity;
    }

    /**
     * 空流程
     */
    static <T, R> Flow<T, R> empty() {
        return Task::<T, R>empty;
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
        public static Flow<Tao, String> bidFlow() {
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
            final var atoLong = new AtomicLong(0L);
            return () ->
                    pendingDownloads -> CompletableFuture.completedFuture(pendingDownloads)
                            .thenApplyAsync(List::parallelStream, taskExecutor())// 开启并行流加速提交
                            .thenApplyAsync(chapter4ReadStream -> chapter4ReadStream.sorted(Comparator.comparing(Chapter.Chapter4Read::chapterOrdid)), taskExecutor())// 并行流不影响排序语义
                            .thenApplyAsync(chapter4ReadStream -> chapter4ReadStream.limit(FlowEngine.IS_TEST ? 20 : Long.MAX_VALUE), taskExecutor())// 测试模式下仅下载前 20 章
                            .thenApplyAsync(chapter4ReadStream -> chapter4ReadStream.map(Flows.contentFlow()::start), taskExecutor())// 并发式启动下载任务
                            .thenApplyAsync(Stream::toList, taskExecutor())// 提前收集源
                            .thenApplyAsync(List::stream, taskExecutor())
                            .thenApplyAsync(chapter4MergeStream -> chapter4MergeStream.sorted(Comparator.comparing(Chapter.Chapter4Merge::orderId)), taskExecutor())// 重新排序
                            .thenApplyAsync(chapter4MergeStream -> chapter4MergeStream.map(chapter4Merge -> Chapter.Chapter4Merge.of(chapter4Merge, atoLong)), taskExecutor())// 设置 skip 属性
                            .thenApplyAsync(Stream::toList, taskExecutor());
        }

        // 部分 下载章节内容 的流程组装[针对一条章节内容]
        public static Flow<Chapter.Chapter4Read, Chapter.Chapter4Merge> contentFlow() {
            return () -> Reader.Readers.contentReader()
                    .thenAsync(Selector.Selectors.contentSelector())
                    .thenAsync(Parser.Parsers.contentParser())
                    .thenAsync(Decoder.Decoders.contentDecoder())
                    .thenAsync(Formatter.Formatters.contentFormatter())
                    .thenAsync(FlowEngine.IS_DEBUG ? Writer.Writers.consoleWriter() : Writer.Writers.fileWriter());
        }

        // 完整 合并文件 的流程组装
        public static Flow<List<Chapter.Chapter4Merge>, Tao> mergeFlow() {
            return FlowEngine.IS_DEBUG ? Flow.empty() :
                    () -> Merger.Mergers.fileMerger().thenAsync(Cleaner.Cleaners.fileCleaner());
        }
    }
}