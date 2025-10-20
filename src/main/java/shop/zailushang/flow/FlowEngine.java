package shop.zailushang.flow;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.util.Assert;
import shop.zailushang.util.ScopedExecutors;

import java.net.http.HttpClient;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

@Slf4j
public class FlowEngine implements AutoCloseable {
    // 是否启用调试模式
    public static final boolean IS_DEBUG = false;
    // 是否启用测试模式
    public static final boolean IS_TEST = true;
    // 是否启用本地解密
    public static final boolean USE_NATIVE = true;
    // 删除文件标识
    public static final boolean NEED_DELETE = true;
    // 默认文件夹路径 e.g. D:/斗破苍穹
    public static final String FOLDER_FORMATTER = "D:/%s";
    // 每个线程默认处理的章节数量
    public static final Integer DEFAULT_CAPACITY = 5;
    // 线程本地变量：传递当前下载的书籍名称
    public static final ScopedValue<String> BOOK_NAME = ScopedValue.newInstance();
    /**
     * io密集型任务线程池 ：使用自定义代理虚拟线程池
     * 此处使用 StableValue 延迟初始化 executor，因 executor 中需要使用绑定了属性的 BOOK_NAME
     * BOOK_NAME 属性绑定的正确时机在调用 {@link FlowEngine#start(String)} 方法时，executor 正确的初始化时机，应滞于其后
     * 这里如直接使用 public static final ExecutorService EXECUTOR = ... 会将 executor 的初始化提前至类初始化阶段
     * 此时属性尚未绑定，调用 key.get() 会导致 NoSuchElementException
     */
    @SuppressWarnings("preview")
    public static final Supplier<ExecutorService> EXECUTOR_SERVICE_SUPPLIER = StableValue.supplier(() -> ScopedExecutors.newVirtualThreadPerTaskExecutor(BOOK_NAME));
    // http客户端（延迟的传递性：依赖延迟，自身亦当延迟初始化）
    @SuppressWarnings("preview")
    public static final Supplier<HttpClient> HTTP_CLIENT_SUPPLIER = StableValue.supplier(() -> HttpClient.newBuilder().executor(EXECUTOR_SERVICE_SUPPLIER.get()).build());

    // 单例模式：静态实例对象
    // 使用 volatile 修饰，防止指令重排导致的 NPE 问题
    private static volatile FlowEngine DEFAULT_FLOW_ENGINE;

    private FlowEngine() {
        Assert.isTrue(DEFAULT_FLOW_ENGINE, Assert::isNull, () -> new IllegalStateException("Don’t judge each day by the harvest you reap but by the seeds that you plant. — Robert Louis Stevenson"));
    }

    // 花式 dcl 单例
    public static FlowEngine getDefaultFlowEngine() {
        return Optional.ofNullable(DEFAULT_FLOW_ENGINE).orElseGet(() -> {
            synchronized (FlowEngine.class) {
                DEFAULT_FLOW_ENGINE = Optional.ofNullable(DEFAULT_FLOW_ENGINE).orElseGet(FlowEngine::new);
                return DEFAULT_FLOW_ENGINE;
            }
        });
    }

    // 启动流程引擎，设置书籍名称的作用域变量
    public void start(String bookName) {
        ScopedValue.where(BOOK_NAME, bookName).run(this::start);
    }

    // 组装串联流程
    private void start() {
        try {
            log.info("""
                    \u001B[93m敕令：「
                                                                  天地自然，秽气分散！
                                                                  洞中玄虚，晃朗太元！
                                                                  焚香启告，迳达九天！
                                                                  今开法坛：
                                                                  一请，三清道祖垂慈！
                                                                  二请，四御天尊降鉴！
                                                                  三请，雷部将帅听宣！
                                                                  四请，五营神兵列阵！
                                                                  坛场肃靖，万神拱卫！
                                                                  急急如律令！！！
                                                                」\u001B[0m
                    """);
            // 获取 bid 流程
            var bidFlow = Flow.Flows.bidFlow();
            log.info("\u001B[93m敕令：「一笔天地动，风雷随法涌。」\u001B[0m");
            // 获取章节列表流程
            var chapterFlow = Flow.Flows.chapterFlow();
            log.info("\u001B[93m敕令：「二笔祖师剑，神威降尘寰。」\u001B[0m");
            // 组装并启动流程
            var pendingDownloads = bidFlow.thenAsync(chapterFlow).start(null);
            // 获取章节内容流程
            var contentListFlow = Flow.Flows.contentListFlow();
            log.info("\u001B[93m敕令：「三笔凶神灭，煞气皆溃裂。」\u001B[0m");
            // 启动获取章节内容流程
            var sources = contentListFlow.start(pendingDownloads);
            // 获取文件合并流程
            var mergedFlow = Flow.Flows.mergeFlow();
            log.info("\u001B[93m敕令：「四笔煞无形，乾坤朗朗清。」\u001B[0m");
            // 启动文件合并流程
            mergedFlow.start(sources);
            log.info("\u001B[93m敕令：「笔收星芒，符镇八荒，朱砂既凝，邪魔永丧。」 ~ 「镇」\u001B[0m");
        } catch (Exception e) {
            log.error("\u001B[91m敕令：「心念不纯，符窍无光！僭请神明，触怒天罡！伏请三清垂慈，赦宥愚诚！」\u001B[0m");
            throw e;
        }
    }

    public void end() {
        log.info("\u001B[92m敕令：「香云奉送，祖师归坛；神兵返驾，各归玄庭！弟子稽首，再沐恩光！散坛！」\u001B[0m");
        FlowEngine.HTTP_CLIENT_SUPPLIER.get().close();
        FlowEngine.EXECUTOR_SERVICE_SUPPLIER.get().shutdown();
    }

    @Override
    public void close() {
        end();
    }
}