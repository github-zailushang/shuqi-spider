package shop.zailushang.component;

import shop.zailushang.flow.FlowEngine;
import shop.zailushang.utils.Assert;
import shop.zailushang.utils.RateLimitUnits;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/*
 * 抽象通用节点
 */
@FunctionalInterface
public interface Task<T, R> extends Function<T, CompletableFuture<R>> {

    @Override
    default CompletableFuture<R> apply(T param) {
        try {
            return execute(param);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    CompletableFuture<R> execute(T param) throws Exception;

    /*
     * 高阶函数：利用函数式编程的函数组合特性，来组装两个任务
     * 同步调用链
     */
    default <V> Task<T, V> then(Task<? super R, V> next) {
        Assert.isTrue(next, Assert::isNotNull, () -> new NullPointerException("An unexamined life is not worth living. — Socrates"));
        return t -> execute(t).thenCompose(next);
    }

    /*
     * 高阶函数：利用函数式编程的函数组合特性，来组装两个任务
     * 异步调用链
     */
    default <V> Task<T, V> thenAsync(Task<? super R, V> next) {
        Assert.isTrue(next, Assert::isNotNull, () -> new NullPointerException("An unexamined life is not worth living. — Socrates"));
        return t -> execute(t).thenComposeAsync(next, FlowEngine.IO_TASK_EXECUTOR);
    }

    /**
     * 一致性任务
     */
    static <T> Task<T, T> identity() {
        return CompletableFuture::<T>completedFuture;
    }

    /**
     * 空任务
     */
    static <T, R> Task<T, R> empty() {
        return t -> CompletableFuture.completedFuture(null);
    }

    /*
     * 流控任务专员（装饰器模式）
     */
    static <T, R> Task<T, ? extends R> withRateLimit(Task<? super T, R> innerTask, long timeout) {
        Assert.isTrue(innerTask, Assert::isNotNull, () -> new NullPointerException("The only way to do great work is to love what you do. — Steve Jobs"));
        return t -> CompletableFuture.completedFuture(t)
                .thenApplyAsync(RateLimitUnits::<T>acquire, FlowEngine.IO_TASK_EXECUTOR) // 执行任务前获取信号量
                .thenComposeAsync(innerTask::<R>apply, CompletableFuture.delayedExecutor(timeout, TimeUnit.SECONDS, FlowEngine.IO_TASK_EXECUTOR))// 使用包装带延时的线程池
                .whenComplete(RateLimitUnits::release); // 任务结束时释放信号量
    }
}