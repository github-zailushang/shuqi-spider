package shop.zailushang.utils;

import lombok.extern.slf4j.Slf4j;

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
 * JS 引擎池：缓存JS引擎对象
 */
@Slf4j
public class ScriptEnginePool {
    // 阻塞队列
    private static final BlockingDeque<ScriptEngine> blockingDeque;
    private static final String NAME = "「三清铃」";

    static {
        // 缓存 200 个JS引擎对象
        log.info("{} - 执行初始化js引擎池", NAME);
        blockingDeque = IntStream.rangeClosed(1, 200)
                .mapToObj(unused -> createScriptEngine())
                .collect(Collectors.toCollection(LinkedBlockingDeque::new));
    }


    /**
     * 生成JS引擎
     */
    private static ScriptEngine createScriptEngine() {
        var resourceStream = ClassLoader.getSystemClassLoader().getResourceAsStream("decode.js");
        Assert.isTrue(resourceStream, Assert::isNotNull, () -> new NullPointerException("js脚本未找到"));
        var scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
        try (var inputStreamReader = new InputStreamReader(resourceStream)) {
            scriptEngine.eval(inputStreamReader);
            return scriptEngine;
        } catch (IOException | ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 借用一个JS引擎对象
     */
    public static ScriptEngine acquire() {
        try {
            log.info("{} - 使用js引擎执行解密操作", NAME);
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
            log.info("{} - 归还js引擎至引擎池", NAME);
            blockingDeque.put(scriptEngine);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}