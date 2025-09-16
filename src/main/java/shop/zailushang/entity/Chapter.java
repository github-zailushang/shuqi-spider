package shop.zailushang.entity;

import java.nio.channels.FileChannel;

// 章节内容
public class Chapter {
    // 下载时
    public record Chapter4Read(String authorName, String bookName, String chapterId, String chapterName,
                               String contUrlSuffix, String chapterOrdid) {
    }

    // 选择时
    public record Chapter4Select(String bookName, String chapterName, String chapterOrdid, String jsonCiphertext) {
    }

    // 解析时
    public record Chapter4Parse(String bookName, String chapterName, String chapterOrdid, String jsonCiphertext) {
    }

    // 解密时
    public record Chapter4Decode(String bookName, String chapterName, String chapterOrdid, String ciphertext) {
    }

    // 排版时
    public record Chapter4Format(String bookName, String chapterName, String chapterOrdid, String unformattedChapterContent) {
    }

    // 保存时
    public record Chapter4Write(String bookName, String chapterName, String chapterOrdid, String chapterContext) {
    }

    // 文件合并时
    public record Chapter4Merge(Integer orderId, FileChannel fileChannel, String bookName, Long skip) {
        public Chapter4Merge(Integer orderId, FileChannel fileChannel, String bookName) {
            this(orderId, fileChannel, bookName, -1L);
        }
    }
}