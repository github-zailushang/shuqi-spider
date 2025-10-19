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
     * 动态代理模式，代理一个虚拟线程池，线程池中的任务会自动"继承"提交任务线程中的 ScopedValue 值
     * 调用前置要求：必须事先设置了 ScopedValue 值，在 ScopedValue 有效域中调用
     * 即: ScopedValue.where(key, value).run(()->ScopedExecutors.newVirtualThreadPerTaskExecutor(key))
     * 返回的 executor 会"继承"上一级域中的 ScopedValue 值并重新设定，「子又生孙，孙又生子；子又有子，子又有孙；子子孙孙无穷匮也」
     * 从而实现链式传递 ScopedValue 值
     */
    public static <T> ExecutorService newVirtualThreadPerTaskExecutor(ScopedValue<T> key) {
        // 注意：当前操作环境为提交任务的线程
        var cls = DELEGATE.getClass();
        // 在提交任务的线程中获取 ScopedValue 值
        var value = key.get();
        return (ExecutorService) Proxy.newProxyInstance(cls.getClassLoader(), cls.getInterfaces(), (_, method, args) -> {
            // 仅代理提交方法 execute（execute 会在 CompletableFuture 中调用，submit 则不必理会，无人在意）
            if ("execute".equals(method.getName()) && args.length > 0 && args[0] instanceof Runnable r)
                // 只包装参数，不改变统一调用方式
                // 为每一个新开启的虚拟线程设置 ScopedValue 值
                args[0] = (Runnable) () -> ScopedValue.where(key, value).run(r);// 注意：此处操作环境为新开启的虚拟线程
            // 调用原始方法，但参数经过处理
            return method.invoke(DELEGATE, args);
        });
    }
}