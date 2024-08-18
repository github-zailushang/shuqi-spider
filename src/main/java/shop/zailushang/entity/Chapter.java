package shop.zailushang.entity;

import java.nio.file.Path;

// 章节内容
public class Chapter {
    // 下载时
    public record Chapter4Download(String authorName, String bookName, String chapterId, String chapterName,
                                   String contUrlSuffix, String chapterOrdid) {
    }

    // 保存时
    public record Chapter4Save(String bookName, String chapterName, String chapterOrdid, String chapterContext) {
    }

    // 文件合并时
    public record Chapter4Merge(Integer orderId, Path folderPath, Path filePath, String bookName, Long skip) {
        public Chapter4Merge(Integer orderId, Path folderPath, Path filePath, String bookName) {
            this(orderId, folderPath, filePath, bookName, 0L);
        }
    }
}