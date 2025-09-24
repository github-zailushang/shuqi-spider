package shop.zailushang.flow;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.utils.BookCache;

import java.net.http.HttpClient;
import java.util.concurrent.*;

@Slf4j
public class FlowEngine implements AutoCloseable {
    // 休眠时间
    public static final long TIMEOUT = 2L;
    // io密集型任务线程池 ：使用虚拟线程池
    public static final ExecutorService IO_TASK_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    // http客户端
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .executor(FlowEngine.IO_TASK_EXECUTOR)
            .build();

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
        log.info("敕令：「天地自然，秽气分散！洞中玄虚，晃朗太元！焚香启告，迳达九天！今开法坛。一请，三清道祖垂慈，二请，四御天尊降鉴！三请，雷部将帅听宣，四请，五营神兵列阵！坛场肃靖，万神拱卫！急急如律令！」");

        // 获取 bid 流程
        var bidFlow = Flow.Flows.bidFlow();
        log.info("敕令：「一笔天地动，风雷随法涌。」");
        // 获取章节列表流程
        var chapterFlow = Flow.Flows.chapterFlow();
        log.info("敕令：「二笔祖师剑，神威降尘寰。」");
        // 组装并启动流程
        var downloads = bidFlow.thenAsync(chapterFlow)
                .start(bookName);
        // 获取章节内容流程
        var contentListFlow = Flow.Flows.contentListFlow();
        log.info("敕令：「三笔凶神灭，煞气皆溃裂。」");
        // 启动获取章节内容流程
        var sources = contentListFlow.start(downloads);
        // 执行文件合并流程
        var mergedFlow = Flow.Flows.mergeFlow();
        log.info("敕令：「四笔煞无形，乾坤朗朗清。」");
        mergedFlow.start(sources);

        // 关闭文件通道
        BookCache.removeFileChannel(bookName);
        log.info("敕令：「笔收星芒，符镇八荒，朱砂既凝，邪魔永丧。」 ~ 「收」");
    }

    public void end() {
        log.info("敕令：「香云奉送，祖师归坛；神兵返驾，各归玄庭！弟子稽首，再沐恩光！散坛！」");
        FlowEngine.HTTP_CLIENT.close();
        FlowEngine.IO_TASK_EXECUTOR.shutdown();
    }

    @Override
    public void close() {
        end();
    }
}