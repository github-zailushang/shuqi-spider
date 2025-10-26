package shop.zailushang.component;

import shop.zailushang.util.Assert;
import shop.zailushang.util.RateLimitUnits;
import shop.zailushang.util.ScopedExecutors;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
        return t -> execute(t).thenComposeAsync(next, taskExecutor());
    }

    /**
     * 一致性任务
     */
    static <T> Task<T, T> identity() {
        return CompletableFuture::completedFuture;
    }

    /**
     * 空任务
     */
    static <T, R> Task<T, R> empty() {
        return _ -> CompletableFuture.completedFuture((R) null);
    }

    /**
     * 方法重载自 {@link Task#parallelTask(Function, Task, Function)}
     */
    static <T, R> Task<List<T>, List<R>> parallelTask(Function<List<T>, List<T>> before, Task<? super T, R> task) {
        return parallelTask(before, task, Function.identity());
    }

    /**
     * 方法重载自 {@link Task#parallelTask(Function, Task, Function)}
     */
    static <T, R> Task<List<T>, List<R>> parallelTask(Task<? super T, R> task) {
        return parallelTask(Function.identity(), task, Function.identity());
    }

    /**
     * 方法重载自 {@link Task#parallelTask(Function, Task, Function)}
     */
    static <T, R> Task<List<T>, List<R>> parallelTask(Task<? super T, R> task, Function<List<R>, List<R>> andThen) {
        return parallelTask(Function.identity(), task, andThen);
    }

    /**
     * 并行任务
     */
    @SuppressWarnings("unchecked")
    static <T, R> Task<List<T>, List<R>> parallelTask(Function<List<T>, List<T>> before, Task<? super T, R> task, Function<List<R>, List<R>> andThen) {
        Assert.isTrue(task, Assert::isNotNull, () -> new NullPointerException("The future depends on what you do today. — Mahatma Gandhi"));
        return items -> {
            var array = before.apply(items) // 前置处理
                    .stream().map(task).toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(array)
                    .thenApplyAsync(_ -> Arrays.stream(array).map(future -> (R) future.join()).toList(), taskExecutor())
                    .thenApplyAsync(andThen, taskExecutor());// 后置处理
        };
    }

    /*
     * 流控任务专员（装饰器模式）
     */
    static <T, R> Task<T, ? extends R> withRateLimit(Task<? super T, R> innerTask, long timeout) {
        Assert.isTrue(innerTask, Assert::isNotNull, () -> new NullPointerException("The only way to do great work is to love what you do. — Steve Jobs"));
        return t -> CompletableFuture.completedFuture(t)
                .thenApplyAsync(RateLimitUnits::acquire, taskExecutor()) // 执行任务前获取信号量
                .thenComposeAsync(innerTask, CompletableFuture.delayedExecutor(timeout, TimeUnit.SECONDS, taskExecutor()))// 使用包装后带延时的线程池
                .whenCompleteAsync(RateLimitUnits::release, taskExecutor()); // 任务结束时释放信号量
    }

    /**
     * 任务专用线程池（使用包装的虚拟线程池）
     */
    static Executor taskExecutor() {
        return ScopedExecutors.newScopedExecutor();
    }
}