package shop.zailushang.component;

import com.fasterxml.jackson.databind.ObjectMapper;
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
            // 需要的属性集合 bookName 在外层，需单独处理
            var recognizedProperties = List.of("chapterId", "contUrlSuffix", "chapterOrdid", "chapterName");

            return chapterSource -> {
                log.info("{} - 执行解析章节列表操作", Parser.name());
                var objectMapper = new ObjectMapper();
                // 根节点
                var rootJsonNode = objectMapper.readTree(chapterSource);
                // 章节列表
                var chapterList = rootJsonNode.get("chapterList");

                // {chapterList:[{volumeList:[{chapterId,chapterName,contUrlSuffix}]}]}
                return CompletableFuture.completedFuture(
                        IntStream.range(0, chapterList.size())
                                .mapToObj(chapterList::get)
                                .map(chapter -> chapter.get("volumeList"))
                                .flatMap(volumeList -> IntStream.range(0, volumeList.size())
                                        .mapToObj(volumeList::get)
                                        .toList()
                                        .stream())
                                .map(jsonNode -> {
                                    // 构建目标 ObjectNode 对象
                                    var targetObjectNode = objectMapper.createObjectNode();
                                    // 从根节点添加 bookName 属性
                                    targetObjectNode.putIfAbsent("bookName", rootJsonNode.get("bookName"));
                                    // 添加其余属性
                                    recognizedProperties.forEach(property -> targetObjectNode.putIfAbsent(property, jsonNode.get(property)));
                                    return targetObjectNode;
                                })
                                .map(chapterJsonNode -> {
                                    try {
                                        return objectMapper.treeToValue(chapterJsonNode, Chapter.Chapter4Read.class);
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