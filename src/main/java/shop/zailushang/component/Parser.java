package shop.zailushang.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import shop.zailushang.entity.Chapter;
import shop.zailushang.entity.Content;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * 组件：从选择的元素中进一步解析想要的内容
 */
@FunctionalInterface
public interface Parser<T, R> extends Task<T, R> {
    @Override
    default CompletableFuture<R> execute(T source) throws Exception {
        return parse(source);
    }

    CompletableFuture<R> parse(T source) throws Exception;

    static <T> T jsonParser(String json, Class<T> clazz) {
        try {
            return new ObjectMapper().readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 组件名
    static String name() {
        return "「析」";
    }

    @Slf4j
    class Parsers {

        static {
            log.info("\u001B[35m敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}\u001B[0m", Parser.name());
        }

        // bid解析器
        public static Parser<String, String> bidParser() {
            // bid 无需额外解析，只是过个流程
            return source -> {
                log.info("{} - 执行解析bid内容操作", Parser.name());
                return CompletableFuture.completedFuture(source);
            };
        }

        // 章节列表解析器
        public static Parser<String, List<Chapter.Chapter4Read>> chapterParser() {
            // 从最内层的json对象上移除这些属性，因为后续用不上，如果不手动移除，则要求在转换的对象上有这些属性，否则转json会失败
            // updated  2024年11月5日 网站新增属性添加至忽略列表 [dateOpen chapterLockDesc vipPriorityRead]
            var ignoreProperties = List.of("payStatus", "chapterPrice", "wordCount", "chapterUpdateTime",
                    "shortContUrlSuffix", "oriPrice", "shelf", "isBuy", "isFreeRead", "dateOpen", "chapterLockDesc", "vipPriorityRead");
            // 提取属性向后传递，构建最终对象，在最内层的json对象上添加此属性
            var addProperties = List.of("bookName", "authorName");

            return chapterSource -> {
                log.info("{} - 执行解析章节列表操作", Parser.name());
                var jsonNode = new ObjectMapper().readTree(chapterSource);
                // 章节列表
                var chapterList = jsonNode.get("chapterList");

                // {chapterList:[{volumeList:[{chapterId,chapterName,contUrlSuffix}]}]}
                return CompletableFuture.completedFuture(
                        IntStream.range(0, chapterList.size())
                                .mapToObj(chapterList::get)
                                .map(volumeOuter -> volumeOuter.get("volumeList"))
                                .flatMap(volumes -> IntStream.range(0, volumes.size())
                                        .mapToObj(volumes::get)
                                        .toList()
                                        .stream())
                                .peek(chapter -> {
                                    // 剔除忽略属性
                                    ignoreProperties.forEach(property -> ((ObjectNode) chapter).remove(property));
                                    // 添加外层属性
                                    addProperties.forEach(property -> ((ObjectNode) chapter).putIfAbsent(property, jsonNode.get(property)));
                                })
                                .map(chapterJsonNode -> {
                                    try {
                                        return new ObjectMapper().treeToValue(chapterJsonNode, Chapter.Chapter4Read.class);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .toList()
                );
            };
        }

        // 章节内容解析器
        public static Parser<Chapter.Chapter4Parse, Chapter.Chapter4Decode> contentParser() {
            return chapter4Parse -> {
                log.info("{} - 执行解析章节内容操作", Parser.name());
                // json 转换为 章节内容对象[尚需解密]
                var bookName = chapter4Parse.bookName();
                var chapterName = chapter4Parse.chapterName();
                var chapterOrdid = chapter4Parse.chapterOrdid();
                var jsonCiphertext = chapter4Parse.jsonCiphertext();
                var content = Parser.jsonParser(jsonCiphertext, Content.class);
                // 构建下一步[解密]，需要的对象
                return CompletableFuture.completedFuture(new Chapter.Chapter4Decode(bookName, chapterName, chapterOrdid, content.ChapterContent()));
            };
        }
    }
}