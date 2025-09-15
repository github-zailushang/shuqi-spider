package shop.zailushang.entity;

import java.nio.channels.FileChannel;

// 章节内容
public class Chapter {
    // 下载时
    public record Chapter4Download(String authorName, String bookName, String chapterId, String chapterName,
                                   String contUrlSuffix, String chapterOrdid) {
    }

    // 保存时
    public record Chapter4Save(String bookName, String chapterName, String chapterOrdid, String chapterContext) {
    }

    // 解码时
    public record Chapter4Decode(String bookName, String chapterName, String chapterOrdid, String jsonCiphertext) {
    }

    // 文件合并时
    public record Chapter4Merge(Integer orderId, FileChannel fileChannel, String bookName, Long skip) {
        public Chapter4Merge(Integer orderId, FileChannel fileChannel, String bookName) {
            this(orderId, fileChannel, bookName, -1L);
        }
    }
}