package shop.zailushang.component;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.entity.Chapter;
import shop.zailushang.utils.Assert;
import shop.zailushang.utils.ScriptEnginePool;

import javax.script.Invocable;
import java.util.concurrent.CompletableFuture;

/**
 * 组件：解密加密的章节内容
 */
@FunctionalInterface
public interface Decoder extends Task<Chapter.Chapter4Decode, Chapter.Chapter4Format> {

    @Override
    default CompletableFuture<Chapter.Chapter4Format> execute(Chapter.Chapter4Decode chapter) throws Exception {
        return decode(chapter);
    }

    CompletableFuture<Chapter.Chapter4Format> decode(Chapter.Chapter4Decode content) throws Exception;

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
            return chapter4Decode -> {
                // 调用 js引擎池 解密章节内容
                var scriptEngine = ScriptEnginePool.acquire();
                log.info("{} - 执行解密操作", Decoder.name());
                var ciphertext = chapter4Decode.ciphertext();
                Assert.isTrue(ciphertext, Assert::isNotNull, () -> new NullPointerException("无法下载VIP章节，如已开通VIP账号，请自行添加VIP权限校验。"));
                var unformattedChapterContent = (String) ((Invocable) scriptEngine).invokeFunction("_decode", ciphertext);
                // 归还 js引擎对象
                ScriptEnginePool.release(scriptEngine);
                var chapter4Format = new Chapter.Chapter4Format(chapter4Decode.bookName(), chapter4Decode.chapterName(), chapter4Decode.chapterOrdid(), unformattedChapterContent);
                return CompletableFuture.completedFuture(chapter4Format);
            };
        }
    }
}