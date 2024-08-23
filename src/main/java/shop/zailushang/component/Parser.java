package shop.zailushang.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    default CompletableFuture<R> execute(T source) {
        return parse(source);
    }

    CompletableFuture<R> parse(T source);

    @SuppressWarnings("unused")
    static <V> Parser<V, V> identity() {
        return CompletableFuture::completedFuture;
    }

    static <T> T jsonParser(String json, Class<T> clazz) {
        try {
            return new ObjectMapper().readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    class Parsers {
        private static final Logger logger = LoggerFactory.getLogger(Parsers.class);

        // bid解析器
        public static Parser<String, String> bidParser() {
            // 不方便加日志，弃用
//            return Parser.identity();
            // bid 无需额外解析，只是过个流程
            return source -> {
                logger.info("{} - 执行解析bid内容操作", Thread.currentThread().getName());
                return CompletableFuture.completedFuture(source);
            };
        }

        // 章节列表解析器
        public static Parser<String, List<Chapter.Chapter4Download>> chapterParser() {
            // 从最内层的json对象上移除这些属性，因为后续用不上，如果不手动移除，则要求在转换的对象上有这些属性，否则转json会失败
            var ignoreProperties = List.of("payStatus", "chapterPrice", "wordCount", "chapterUpdateTime",
                    "shortContUrlSuffix", "oriPrice", "shelf", "isBuy", "isFreeRead");
            // 提取属性向后传递，构建最终对象，在最内层的json对象上添加此属性
            var addProperties = List.of("bookName", "authorName");

            return chapterSource -> {
                logger.info("{} - 执行解析章节列表操作", Thread.currentThread().getName());
                try {
                    var jsonNode = new ObjectMapper().readValue(chapterSource, JsonNode.class);
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
                                    .map(JsonNode::toString)
                                    .map(chapterStr -> {
                                        try {
                                            return new ObjectMapper().readValue(chapterStr, Chapter.Chapter4Download.class);
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                                    .toList()
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }

        // 章节内容解析器
        public static Parser<String, Chapter.Chapter4Save> contentParser() {
            return contentSource -> {
                logger.info("{} - 执行解析章节内容操作", Thread.currentThread().getName());
                // contentReader -> contentSelector -> contentParse
                // 处理拼接的参数，前面 contentReader 下载完成后拼接专递过来的
                var strArr = contentSource.split("#");
                var bookName = strArr[0];
                var chapterName = strArr[1];
                var chapterOrdid = strArr[2];
                var jsonStr = strArr[3];
                // json 转换为 章节内容对象[尚需解密]
                var content = Parser.jsonParser(jsonStr, Content.class);
                // 构建下一步[解密]，需要的对象
                return CompletableFuture.completedFuture(new Chapter.Chapter4Save(bookName, chapterName, chapterOrdid, content.ChapterContent()));
            };
        }
    }
}