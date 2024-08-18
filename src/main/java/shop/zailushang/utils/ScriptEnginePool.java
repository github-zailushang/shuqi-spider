package shop.zailushang.utils;

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
 * JS 引擎池
 * 为何使用它？
 * 因为调用js引擎执行解密操作，是处于线程环境下的操作（因在下载章节内容时，开启了异步）
 * 但js不允许多线程访问，使用一个 JS 引擎对象去加锁串行的话，效率太慢，故在初始化时 缓存1000个 JS引擎对象来执行解密操作
 */
public class ScriptEnginePool {

    // 阻塞队列
    private static final BlockingDeque<ScriptEngine> blockingDeque;

    static {
        // 缓存1000个JS引擎对象
        blockingDeque = IntStream.rangeClosed(1, 1000)
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
            blockingDeque.put(scriptEngine);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}