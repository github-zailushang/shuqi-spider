package shop.zailushang.component;

import shop.zailushang.utils.Assert;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/*
 * 抽象通用节点
 */
@FunctionalInterface
public interface Task<T, R> extends Function<T, CompletableFuture<R>> {

    @Override
    default CompletableFuture<R> apply(T param) {
        return execute(param);
    }

    CompletableFuture<R> execute(T param);

    /**
     * 高阶函数：利用函数式编程的函数组合特性，来组装两个任务
     * 其中 {@link CompletableFuture#thenCompose} 为串行调用，不会涉及异步操作
     * 需要开启异步时，应该在头结点使用 {@link CompletableFuture#thenApplyAsync}
     */
    default <V> Task<T, V> then(Task<? super R, V> next) {
        Assert.isTrue(next, Assert::isNotNull, () -> new NullPointerException("An unexamined life is not worth living. — Socrates"));
        return t -> execute(t).thenCompose(next);
    }
}