package shop.zailushang.util;

import lombok.extern.slf4j.Slf4j;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * JS 引擎池：缓存JS引擎对象
 */
@Slf4j
public class ScriptEnginePool {
    // JS脚本
    private static final String JS_SCRIPT;
    // 阻塞队列
    private static final BlockingDeque<ScriptEngine> BLOCKING_DEQUE;
    // 脚本引擎管理器
    private static final ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();
    // 组件名称
    private static final String NAME = "「三清铃」";

    static {
        // 缓存 200 个JS引擎对象
        log.info("{} - 执行初始化js引擎池", NAME);
        try (var resourceStream = ClassLoader.getSystemClassLoader().getResourceAsStream("decode.js")) {
            Assert.isTrue(resourceStream, Assert::isNotNull, () -> new NullPointerException("To be, or not to be, that is the question. — William Shakespeare, Hamlet"));
            JS_SCRIPT = new String(resourceStream.readAllBytes());
            BLOCKING_DEQUE = IntStream.rangeClosed(1, 200)
                    .mapToObj(_ -> createScriptEngine())
                    .collect(Collectors.toCollection(LinkedBlockingDeque::new));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 生成JS引擎
     */
    private static ScriptEngine createScriptEngine() {
        try {
            var scriptEngine = SCRIPT_ENGINE_MANAGER.getEngineByName("JavaScript");
            scriptEngine.eval(JS_SCRIPT);
            return scriptEngine;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 借用一个JS引擎对象
     */
    public static ScriptEngine acquire() {
        try {
            log.info("{} - 使用js引擎执行解密操作", NAME);
            return BLOCKING_DEQUE.take();
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
            BLOCKING_DEQUE.put(scriptEngine);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}