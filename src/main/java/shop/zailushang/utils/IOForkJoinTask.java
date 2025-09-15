package shop.zailushang.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 仿 ForkJoinPool 思路实现 IO密集型任务分治
 * 为何使用它？
 * Stream并行流，ForkJoinPool 同理，针对的场景是 CPU密集型任务（Stream并行流内部使用的就是ForkJoinPool）
 * 文件写入属于IO密集型任务，明显不在此列，不是不能用，而是不合适，故而，文件IO、网络IO，我们应该使用自定义的IO型线程池
 * 再将任务拆分算法封装、提交线程池代码按照 ForkJoinPool 思想封装，终有此工具类
 */
public interface IOForkJoinTask<T extends IOForkJoinTask<T>> {

    default Logger logger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    static String name() {
        return "「「「器之二」」」";
    }

    // 线程池
    ExecutorService executor();

    // 起始索引
    Integer startIndex();

    // 结束索引
    Integer endIndex();

    // 可以处理的资源数量
    Integer capacity();

    // 是否需要拆分（超出预期可处理数量）
    default Boolean needsFork() {
        return (endIndex() - startIndex() + 1) > capacity();
    }

    // 要在线程内执行的任务的起始点
    default Result compute() {
        if (needsFork()) {
            // 拆分并提交子任务
            CompletableFuture<Result>[] futures = fork();
            // 阻塞等待子任务执行完毕
            return join(futures);
        } else {
            return doCompute();
        }
    }

    // 执行具体的任务操作由子类实现
    Result doCompute();

    // 任务拆分
    @SuppressWarnings("unchecked")
    default CompletableFuture<Result>[] fork() {
        // 执行拆分
        var tasks = doFork();
        return Arrays.stream(tasks)
                .map(task -> CompletableFuture.supplyAsync(task::compute, executor())) // 将子任务提交至线程池
                .toArray(CompletableFuture[]::new);
    }

    // 具体拆分算法由子类实现
    IOForkJoinTask<T>[] doFork();

    // 当前任务阻塞等待子任务的返回结果
    @SuppressWarnings("unchecked")
    default Result join(CompletableFuture<Result>... futures) {
        // 返回值用以计算任务成功数量
        logger().info("{} - 等待子任务返回 ...", IOForkJoinTask.name());
        // 合并子任务返回结果
        var result = Arrays.stream(futures)
                .map(CompletableFuture::join)
                .reduce(Result.ZERO, Result::reduce);

        logger().info("{} - 返回结果:{}", IOForkJoinTask.name(), result);
        return result;
    }

    /**
     * 返回的结果封装
     *
     * @param successful 成功处理的资源数量
     * @param byteSize   成功处理的字节数
     */
    record Result(Integer successful, Long byteSize) {
        public static final Result ZERO = new Result(0, 0L);

        public static Result reduce(Result left, Result right) {
            return new Result(left.successful + right.successful, left.byteSize + right.byteSize);
        }
    }
}