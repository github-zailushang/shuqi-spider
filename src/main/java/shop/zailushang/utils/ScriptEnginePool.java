package shop.zailushang.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * JS 引擎池：用缓存池思想，避免重复的创建和销毁资源
 * 这里为何使用它呢？
 * 因为调用js引擎执行解密操作，是处于多线程环境下的操作，在下载章节内容时，开启了异步，后续的一系列连续操作均为异步操作(thenCompose)
 * 但js是单线程语言，多线程访问会报错，一开始我是使用一个 JS 引擎对象去加锁串行，但效率实在太慢
 * 故在此初始化缓存200个JS引擎对象来执行解密操作，各用各的，互不干扰
 */
public class ScriptEnginePool {

    // 阻塞队列
    private static final BlockingDeque<ScriptEngine> blockingDeque;

    private static final Logger logger = LoggerFactory.getLogger(ScriptEnginePool.class);

    static {
        // 缓存1000个JS引擎对象
        logger.info("{} - 执行初始化js引擎池", Thread.currentThread().getName());
        blockingDeque = IntStream.rangeClosed(1, 200)
                .mapToObj(unused -> createScriptEngine())
                .collect(Collectors.toCollection(LinkedBlockingDeque::new));
    }

    /**
     * 生成JS引擎
     */
    private static ScriptEngine createScriptEngine() {
        var scriptEngineManager = new ScriptEngineManager();
        var scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
        try (var inputStreamReader = new InputStreamReader(ScriptEnginePool.class.getResourceAsStream("/decode.js"))) {
            scriptEngine.eval(inputStreamReader);
            return scriptEngine;
        } catch (IOException | ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 借用一个JS引擎对象
     */
    public static ScriptEngine use() {
        try {
            logger.info("{} - 使用js引擎执行解密操作", Thread.currentThread().getName());
            return blockingDeque.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 归还一个JS引擎对象
     */
    public static void release(ScriptEngine scriptEngine) {
        try {
            logger.info("{} - 归还js引擎至引擎池", Thread.currentThread().getName());
            blockingDeque.put(scriptEngine);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}