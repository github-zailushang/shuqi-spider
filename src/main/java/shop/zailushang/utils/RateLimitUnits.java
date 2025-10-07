package shop.zailushang.utils;

import java.util.concurrent.Semaphore;

// f**k checked exception in lambda
public class RateLimitUnits {
    // 在下载章节内容时，最大允许并发数
    public static final Integer MAX_ALLOWED = 3;
    // 信号量
    public static final Semaphore SEMAPHORE = new Semaphore(MAX_ALLOWED);

    // 获取信号量
    public static <T> T acquire(T t) {
        try {
            SEMAPHORE.acquire();
            return t;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // 释放信号量
    public static <R, E extends Throwable> void release(R r, E e) {
        SEMAPHORE.release();
    }
}
