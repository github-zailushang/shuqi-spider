package shop.zailushang.component;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.entity.Chapter;
import shop.zailushang.entity.Content;
import shop.zailushang.flow.FlowEngine;
import shop.zailushang.utils.JsonUtils;

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
            return source -> CompletableFuture.completedFuture(source)
                    .whenComplete((r, e) -> log.info("{} - 执行解析bid内容操作", Parser.name()));
        }

        // 章节列表解析器
        public static Parser<String, List<Chapter.Chapter4Read>> chapterParser() {
            // 需要的属性集合 bookName 在外层，需单独处理
            final var recognizedProperties = List.of("chapterId", "contUrlSuffix", "chapterOrdid", "chapterName");
            return chapterSource -> CompletableFuture.completedFuture(chapterSource)
                    .whenComplete((r, e) -> log.info("{} - 执行解析章节列表操作", Parser.name()))
                    .thenApplyAsync(cs -> {
                        // 根节点
                        var rootJsonNode = JsonUtils.readTree(cs);
                        // 章节列表
                        var chapterList = rootJsonNode.get("chapterList");
                        // {chapterList:[{volumeList:[{chapterId,chapterName,contUrlSuffix}]}]}
                        return IntStream.range(0, chapterList.size())
                                .mapToObj(chapterList::get)
                                .map(chapter -> chapter.get("volumeList"))
                                .flatMap(volumeList -> IntStream.range(0, volumeList.size())
                                        .mapToObj(volumeList::get)
                                        .toList()
                                        .stream())
                                .map(jsonNode -> {
                                    // 构建目标 ObjectNode 对象
                                    var targetObjectNode = JsonUtils.createObjectNode();
                                    // 从根节点添加 bookName 属性
                                    targetObjectNode.putIfAbsent("bookName", rootJsonNode.get("bookName"));
                                    // 添加其余属性
                                    recognizedProperties.forEach(property -> targetObjectNode.putIfAbsent(property, jsonNode.get(property)));
                                    return targetObjectNode;
                                })
                                .map(chapterJsonNode -> JsonUtils.treeToValue(chapterJsonNode, Chapter.Chapter4Read.class))
                                .toList();
                    }, FlowEngine.IO_TASK_EXECUTOR);
        }


        // 章节内容解析器
        public static Parser<Chapter.Chapter4Parse, Chapter.Chapter4Decode> contentParser() {
            // 构建为 Chapter4Decode
            return chapter4Parse -> CompletableFuture.completedFuture(chapter4Parse)
                    .whenComplete((r, e) -> log.info("{} - 执行解析章节内容操作", Parser.name()))
                    .thenApplyAsync(Chapter.Chapter4Parse::jsonCiphertext, FlowEngine.IO_TASK_EXECUTOR)
                    .thenApplyAsync(jsonCiphertext -> JsonUtils.readValue(jsonCiphertext, Content.class), FlowEngine.IO_TASK_EXECUTOR)
                    .thenApplyAsync(content -> new Chapter.Chapter4Decode(chapter4Parse.bookName(), chapter4Parse.chapterName(), chapter4Parse.chapterOrdid(), content.ChapterContent()), FlowEngine.IO_TASK_EXECUTOR);
        }
    }
}