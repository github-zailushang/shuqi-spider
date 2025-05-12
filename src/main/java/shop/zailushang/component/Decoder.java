package shop.zailushang.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shop.zailushang.entity.Chapter;
import shop.zailushang.utils.ScriptEnginePool;

import javax.script.Invocable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 组件：解密加密的章节内容
 */
@FunctionalInterface
public interface Decoder extends Task<Chapter.Chapter4Save, Chapter.Chapter4Save> {

    @Override
    default CompletableFuture<Chapter.Chapter4Save> execute(Chapter.Chapter4Save chapter) {
        return decode(chapter);
    }

    CompletableFuture<Chapter.Chapter4Save> decode(Chapter.Chapter4Save content);

    class Decoders {
        private static final Logger logger = LoggerFactory.getLogger(Decoder.class);

        public static Decoder contentDecoder() {
            return chapter -> {
                // 调用 js引擎池 解密章节内容
                var scriptEngine = ScriptEnginePool.use();
                try {
                    logger.info("{} - 执行解密操作", Thread.currentThread().getName());
                    var chapterContext = Optional.ofNullable(chapter.chapterContext()).orElseThrow(() -> new RuntimeException("无法下载VIP章节，如已开通VIP账号，添加权限校验。"));
                    chapterContext = (String) ((Invocable) scriptEngine).invokeFunction("_decode", chapterContext);
                    return CompletableFuture.completedFuture(
                            new Chapter.Chapter4Save(
                                    chapter.bookName(),
                                    chapter.chapterName(),
                                    chapter.chapterOrdid(),
                                    chapterContext
                            )
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 归还 js引擎对象
                    ScriptEnginePool.release(scriptEngine);
                }
            };
        }
    }
}