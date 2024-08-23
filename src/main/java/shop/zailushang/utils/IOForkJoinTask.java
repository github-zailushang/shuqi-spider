package shop.zailushang.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 仿 ForkJoinPool 思路实现 IO密集型任务分治
 */
public interface IOForkJoinTask<T extends IOForkJoinTask<T>> {

    default Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    // 线程池
    ExecutorService executor();

    // 起始索引
    Integer startIndex();

    // 结束索引
    Integer endIndex();

    // 可以处理的资源数量
    Integer capacity();

    // 是否需要拆分
    default Boolean needsFork() {
        return (endIndex() - startIndex()) > capacity();
    }

    // 要在线程内执行的任务的起始点
    default Result compute() {
        var logger = logger();

        if (needsFork()) {
            // push 子任务
            CompletableFuture<Result>[] futures = fork();
            // 阻塞当前任务
            join(futures);
            // 返回值用以计算任务成功数量
            logger.info("{} - 等待返回 ...", Thread.currentThread().getName());
            // 合并子任务返回结果
            var result = Arrays.stream(futures)
                    .map(CompletableFuture::join)
                    .reduce(Result.ZERO, Result::reduce);

            logger.info("{} - 返回结果:{}", Thread.currentThread().getName(), result);
            return result;
        } else {
            return doCompute();
        }
    }

    // 执行具体的任务操作由子类实现
    Result doCompute();

    // 任务拆分
    @SuppressWarnings("unchecked")
    default CompletableFuture<Result>[] fork() {
        var tasks = doFork();
        var left = tasks[0];
        var right = tasks[1];

        // 提交子任务至线程池异步执行
        var leftFuture = CompletableFuture.completedFuture(left)
                .thenApplyAsync(IOForkJoinTask::compute, executor());

        // 提交子任务至线程池异步执行
        var rightFuture = CompletableFuture.completedFuture(right)
                .thenApplyAsync(IOForkJoinTask::compute, executor());

        // 将 future 对象返回，给后面 join 方法调用
        return new CompletableFuture[]{leftFuture, rightFuture};
    }

    // 具体拆分算法由子类实现
    IOForkJoinTask<T>[] doFork();

    // 当前任务阻塞等待子任务的返回结果
    @SuppressWarnings("unchecked")
    default void join(CompletableFuture<Result>... args) {
        CompletableFuture.allOf(args)
                .join();
    }

    /**
     * 返回的结果封装
     * @param successful    成功处理的资源数量
     * @param byteSize      成功处理的字节数
     */
    record Result(Integer successful, Long byteSize) {

        public static final Result ZERO = new Result(0, 0L);

        public static Result reduce(Result left, Result right) {
            return new Result(left.successful + right.successful, left.byteSize + right.byteSize);
        }
    }
}