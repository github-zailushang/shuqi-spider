package shop.zailushang.component;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.entity.Chapter;
import shop.zailushang.flow.FlowEngine;
import shop.zailushang.utils.Assert;
import shop.zailushang.utils.ScriptEnginePool;

import javax.script.Invocable;
import java.util.Base64;
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

    // js 解密
    @SuppressWarnings("unused")
    static String withJsDecode(String ciphertext) {
        // 调用 js引擎池 解密章节内容
        var scriptEngine = ScriptEnginePool.acquire();
        try {
            return (String) ((Invocable) scriptEngine).invokeFunction("_decode", ciphertext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ScriptEnginePool.release(scriptEngine);
        }
    }

    // java 本地实现解密（随js脚本更迭）
    static String withNativeDecode(String ciphertext) {
        StringBuilder transformed = new StringBuilder();
        for (char c : ciphertext.toCharArray()) {
            if (Character.isLetter(c)) {
                int e = c / 97;
                char lowerChar = Character.toLowerCase(c);
                int i = (lowerChar - 83) % 26;
                if (i == 0) i = 26;
                char decodedChar = (char) (i + (e == 0 ? 64 : 96));
                transformed.append(decodedChar);
            } else {
                transformed.append(c);
            }
        }
        String step1 = transformed.toString();
        String cleaned = step1.replaceAll("[^A-Za-z0-9+/=]", "");
        byte[] decodedBytes = Base64.getDecoder().decode(cleaned);
        StringBuilder result = new StringBuilder();
        int index = 0;
        while (index < decodedBytes.length) {
            int b = decodedBytes[index] & 0xFF;
            if (b < 128) {
                result.append((char) b);
                index++;
            } else if (b > 191 && b < 224) {
                int b2 = decodedBytes[index + 1] & 0xFF;
                result.append((char) ((31 & b) << 6 | (63 & b2)));
                index += 2;
            } else {
                int b2 = decodedBytes[index + 1] & 0xFF;
                int b3 = decodedBytes[index + 2] & 0xFF;
                result.append((char) ((15 & b) << 12 | (63 & b2) << 6 | (63 & b3)));
                index += 3;
            }
        }
        return result.toString();
    }

    // 组件名
    static String name() {
        return "「译」";
    }

    @Slf4j
    class Decoders {

        static {
            log.info("\u001B[35m敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}\u001B[0m", Decoder.name());
        }

        public static Decoder contentDecoder() {
            return chapter4Decode -> CompletableFuture.completedFuture(chapter4Decode)
                    .whenComplete((r, e) -> log.info("{} - 执行解密操作", Decoder.name()))
                    .thenApplyAsync(Chapter.Chapter4Decode::ciphertext, FlowEngine.IO_TASK_EXECUTOR)
                    .whenComplete((ciphertext, e) -> Assert.isTrue(ciphertext, Assert::isNotNull, () -> new NullPointerException("无法下载VIP章节，如已开通VIP账号，请自行添加VIP权限校验。")))
                    .thenApplyAsync(Decoder::withNativeDecode, FlowEngine.IO_TASK_EXECUTOR)// 改用 java 本地实现的解密方法
                    .thenApplyAsync(unformattedChapterContent -> new Chapter.Chapter4Format(chapter4Decode.bookName(), chapter4Decode.chapterName(), chapter4Decode.chapterOrdid(), unformattedChapterContent), FlowEngine.IO_TASK_EXECUTOR);
        }
    }
}