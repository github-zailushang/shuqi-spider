package shop.zailushang.component;

import shop.zailushang.flow.FlowEngine;
import shop.zailushang.utils.Assert;

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

    /*
     * 流控任务专员（装饰器模式）
     */
    static <T, R> Task<T, ? extends R> withRateLimit(Task<? super T, R> innerTask, long timeout) {
        Assert.isTrue(innerTask, Assert::isNotNull, () -> new NullPointerException("The only way to do great work is to love what you do.” — Steve Jobs"));
        return uri -> {
            // 别改！别改！别改！后果自负！！！
            FlowEngine.SEMAPHORE.acquire();
            // 直接休眠指定秒数，这里就不额外计算了，徒添复杂度
            TimeUnit.SECONDS.sleep(timeout);
            // 需阻塞等待任务结束
            return innerTask.execute(uri).whenComplete((r, e) -> FlowEngine.SEMAPHORE.release());
        };
    }
}