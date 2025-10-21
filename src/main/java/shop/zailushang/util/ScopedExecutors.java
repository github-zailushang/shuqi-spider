package shop.zailushang.util;

import java.lang.reflect.Proxy;
import java.util.concurrent.*;

/**
 * CompletableFuture 专用虚拟线程池工具类，为每一个新开启的虚拟线程设置 ScopedValue 值
 */
public class ScopedExecutors {
    // 代理对象
    private static final ExecutorService DELEGATE = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 动态代理模式。
     * <p>
     * 返回一个经过代理的专用虚拟线程池。向此线程池中提交任务时，任务会自动"继承"提交任务线程（上一级域）中设置的 {@code ScopedValue} 值，并在执行时重新绑定。
     * <p>
     * <b>实际调用链路：</b>
     * <pre>{@code
     * ScopedValue.where(key, value).run(() -> {
     *     // 初始化此线程池之前，必须事先设置了 ScopedValue 值，即必须在 ScopedValue 有效域中调用
     *     // 此处为 main 线程（下载单本时）或者 ForkJoinPool 线程（下载多本时）
     *     var executor = ScopedExecutors.newVirtualThreadPerTaskExecutor(key);
     *
     *     CompletableFuture.supplyAsync(() -> {}, executor) // virtualThread-1，"继承"上一级域【main || forkJoinPool】中的 ScopedValue 并重新设定值，执行任务1
     *         .thenApply(..., executor) // virtualThread-2，"继承"上一级域【virtualThread-1】中的 ScopedValue 并重新设定值，执行任务2
     *         .thenApply(..., executor); // virtualThread-3，"继承"上一级域【virtualThread-2】中的 ScopedValue 并重新设定值，执行任务3
     *         // 子又生孙，孙又生子；子又有子，子又有孙；子子孙孙无穷匮也，子孙之乐，得之心而寓之 ScopedValue 也。最终实现 ScopedValue 值在异步任务中链式传递。
     * });
     * }</pre>
     */
    public static <T> ExecutorService newVirtualThreadPerTaskExecutor(ScopedValue<T> key) {
        // 操作环境提醒【初始化线程】
        var cls = DELEGATE.getClass();
        // 兜底初始值，仅捕获一次
        final var initialValue = key.get();
        return (ExecutorService) Proxy.newProxyInstance(cls.getClassLoader(), cls.getInterfaces(), (_, method, args) -> {
            // 仅代理提交方法 execute（execute 会在 CompletableFuture 中调用，submit 则不必理会，无人在意）
            if ("execute".equals(method.getName()) && args.length > 0 && args[0] instanceof Runnable r) {
                // 操作环境提醒【提交任务线程】，从提交任务的线程域中获取 ScopedValue 值，每次方法调用时捕获，未绑定时，则取初始默认值
                var upperLevelValue = key.isBound() ? key.get() : initialValue;
                // 操作环境提醒【新开启的虚拟线程】，包装参数 => 为每一个新开启的虚拟线程设置 ScopedValue 值
                args[0] = (Runnable) () -> ScopedValue.where(key, upperLevelValue).run(r);
            }
            // 只包装参数，不改变统一调用方式，调用原始方法，但参数经过处理
            return method.invoke(DELEGATE, args);
        });
    }
}