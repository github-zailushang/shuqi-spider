package shop.zailushang.component;

import lombok.extern.slf4j.Slf4j;
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

    // 组件名
    static String name() {
        return "「译」";
    }

    @Slf4j
    class Decoders {

        static {
            log.info("敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}", Decoder.name());
        }

        public static Decoder contentDecoder() {
            return chapter -> {
                // 调用 js引擎池 解密章节内容
                var scriptEngine = ScriptEnginePool.acquire();
                try {
                    log.info("{} - 执行解密操作", Decoder.name());
                    var chapterContext = Optional.ofNullable(chapter.chapterContext()).orElseThrow(() -> new RuntimeException("无法下载VIP章节，如已开通VIP账号，自行添加VIP权限校验。"));
                    chapterContext = (String) ((Invocable) scriptEngine).invokeFunction("_decode", chapterContext);
                    var chapter4Save = new Chapter.Chapter4Save(chapter.bookName(), chapter.chapterName(), chapter.chapterOrdid(), chapterContext);
                    return CompletableFuture.completedFuture(chapter4Save);
                } catch (Exception e) {
                    log.error("敕令：「心念不纯，符窍无光！僭请神明，触怒天罡！伏请三清垂慈，赦宥愚诚！」");
                    throw new RuntimeException(e);
                } finally {
                    // 归还 js引擎对象
                    ScriptEnginePool.release(scriptEngine);
                }
            };
        }
    }
}