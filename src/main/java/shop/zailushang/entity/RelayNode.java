package shop.zailushang.entity;

import com.fasterxml.jackson.databind.JsonNode;
import shop.zailushang.util.CheckedExceptionFucker;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 中继节点，存储 chapterListNode && bookNameNode
 */
public record RelayNode(JsonNode chapterListNode, JsonNode bookNameNode) {
    // 从根节点构建中继节点
    public static RelayNode of(JsonNode rootNode) {
        return new RelayNode(rootNode.get("chapterList"), rootNode.get("bookName"));
    }

    public List<Chapter.Chapter4Read> map2Chapter4ReadList() {
        // 需要的属性集合 bookName 在外层，已单独存放
        final var recognizedProperties = List.of("chapterId", "contUrlSuffix", "chapterOrdid", "chapterName");
        // {chapterList:[{volumeList:[{chapterId,chapterName,contUrlSuffix}]}]}
        return IntStream.range(0, chapterListNode.size())
                .mapToObj(chapterListNode::get)
                .map(chapter -> chapter.get("volumeList"))
                .flatMap(volumeList -> IntStream.range(0, volumeList.size())
                        .mapToObj(volumeList::get)
                        .toList()
                        .stream())
                .map(jsonNode -> {
                    // 构建目标 ObjectNode 对象
                    var targetObjectNode = CheckedExceptionFucker.createObjectNode();
                    // 从根节点添加 bookName 属性
                    targetObjectNode.putIfAbsent("bookName", bookNameNode);
                    // 添加其余属性
                    recognizedProperties.forEach(property -> targetObjectNode.putIfAbsent(property, jsonNode.get(property)));
                    return targetObjectNode;
                })
                .map(chapterJsonNode -> CheckedExceptionFucker.treeToValue(chapterJsonNode, Chapter.Chapter4Read.class))
                .toList();
    }
}
