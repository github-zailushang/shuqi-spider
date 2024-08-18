package shop.zailushang.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public interface IOForkJoinTask<T extends IOForkJoinTask<T>> {

    default Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    ExecutorService executor();

    Integer startIndex();

    Integer endIndex();

    Integer capacity();

    default Boolean needsFork() {
        return (endIndex() - startIndex()) > capacity();
    }

    default Result compute() {
        var logger = logger();

        if (needsFork()) {
            // push 子任务
            CompletableFuture<Result>[] futures = fork();
            // 阻塞当前任务
            join(futures);
            // 返回值用以计算任务成功数量
            logger.info("{} - 等待返回 ...", Thread.currentThread().getName());
            var result = Arrays.stream(futures)
                    .map(CompletableFuture::join)
                    .reduce(Result.ZERO, Result::reduce);

            logger.info("{} - 返回结果:{}", Thread.currentThread().getName(), result);
            return result;
        } else {
            return doCompute();
        }
    }

    Result doCompute();

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

    IOForkJoinTask<T>[] doFork();

    @SuppressWarnings("unchecked")
    default void join(CompletableFuture<Result>... args) {
        CompletableFuture.allOf(args)
                .join();
    }

    record Result(Integer successful, Long byteSize) {

        public static final Result ZERO = new Result(0, 0L);

        public static Result reduce(Result left, Result right) {
            return new Result(left.successful + right.successful, left.byteSize + right.byteSize);
        }
    }
}