package shop.zailushang.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.concurrent.*;

public class FlowEngine implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(FlowEngine.class);
    // io密集型任务线程池 ：使用虚拟线程池
    public static final ExecutorService IO_TASK_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    // http客户端
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .executor(FlowEngine.IO_TASK_EXECUTOR)
            .build();

    // cpu密集型任务线程池，核心线程数 = cpu核心数 + 1
    @SuppressWarnings("unused")
    public static final Executor CPU_TASK_EXECUTOR = new ForkJoinPool(Runtime.getRuntime().availableProcessors() + 1);

    // 在下载章节内容时，最大允许并发数
    public static final Integer MAX_ALLOWED = 3;
    public static final Semaphore SEMAPHORE = new Semaphore(MAX_ALLOWED);

    // 单例模式：静态实例对象
    // 使用 volatile 修饰，防止指令重排导致的 NPE 问题
    public static volatile FlowEngine defaultFlowEngine;

    private FlowEngine() {
        if (defaultFlowEngine != null)
            throw new IllegalStateException("Don’t judge each day by the harvest you reap but by the seeds that you plant. — Robert Louis Stevenson");
    }

    // dcl 单例
    public static FlowEngine getDefaultFlowEngine() {
        if (defaultFlowEngine == null)
            synchronized (FlowEngine.class) {
                if (defaultFlowEngine == null) defaultFlowEngine = new FlowEngine();
            }
        return defaultFlowEngine;
    }

    // 组装串联流程
    public void start(String bookName) {
        logger.info("{} - 下载{}流程启动", Thread.currentThread().getName(), bookName);
        var bidFlow = Flow.Flows.bidFlow();
        var chapterFlow = Flow.Flows.chapterFlow();

        var downloads = bidFlow.then(chapterFlow)
                .start(bookName);

        var contentListFlow = Flow.Flows.contentListFlow();
        var sources = contentListFlow.start(downloads);

        // 执行文件合并流程
        var mergedFlow = Flow.Flows.mergeFlow();
        var result = mergedFlow.start(sources);
        logger.info("{} - 下载{}流程完成 - {}", Thread.currentThread().getName(), bookName, result);
    }

    public void end() {
        FlowEngine.HTTP_CLIENT.close();
        FlowEngine.IO_TASK_EXECUTOR.shutdown();
        logger.info("{} - 流程结束", Thread.currentThread().getName());
    }

    @Override
    public void close() {
        end();
    }
}