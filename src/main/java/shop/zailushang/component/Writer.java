package shop.zailushang.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shop.zailushang.entity.Chapter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;


/**
 * 组件：保存章节内容，将排版后的章节内容写入文件，每章为一个文件
 */
@FunctionalInterface
public interface Writer extends Task<Chapter.Chapter4Save, Chapter.Chapter4Merge> {
    @Override
    default CompletableFuture<Chapter.Chapter4Merge> execute(Chapter.Chapter4Save chapter) {
        return write(chapter);
    }

    CompletableFuture<Chapter.Chapter4Merge> write(Chapter.Chapter4Save content);

    class Writers {
        private static final Logger logger = LoggerFactory.getLogger(Writers.class);

        // 将章节内容打印在控制台，调试时用
        @SuppressWarnings("unused")
        public static Writer consoleWriter() {
            return chapter -> {
                logger.info("{} - 执行文件写入操作[控制台]", Thread.currentThread().getName());
                var part = "-".repeat(15);
                var separator = String.format("%s\t%s\t%s", part, chapter.chapterName(), part);
                System.out.println(separator);
                System.out.println(chapter.chapterContext());
                return CompletableFuture.completedFuture((Chapter.Chapter4Merge) null);
            };
        }

        // 将章节内容写入文件
        public static Writer fileWriter() {
            // 默认写入盘符: D盘
            final var basePath = "D:";
            return chapter -> {
                logger.info("{} - 执行文件写入操作[文件系统]", Thread.currentThread().getName());
                // 书名
                var bookName = chapter.bookName();
                // 章节序号为 1 - N 的连续自然数，实际章节标题中的章节序号与此不一定相符，因为网文作者有时请假或其他删减原因之类的，导致章节不连续，但ordid保证连续性
                var fileName = chapter.chapterOrdid();
                // 文件后缀
                var fileType = "txt";
                // 使用书名作为文件夹名
                var folderPath = Paths.get(String.format("%s/%s", basePath, bookName));
                // 章节文件名 e.g ==> D:/斗破苍穹/1.txt
                var filePath = Paths.get(String.format("%s/%s/%s.%s", basePath, bookName, fileName, fileType));
                try {
                    // 此处为并发环境, double check 以创建文件夹，实则是多余操作
                    // Files.createDirectories(folderPath) 行为：不存在则创建，存在时则不作任何操作，故为线程安全操作
                    // 多余的判断行为姑且就算做是用于提醒注意当前操作环境吧
                    if (Files.notExists(folderPath)) {
                        // 使用 String 作为锁对象时，intern 确保使用同一对象
                        synchronized (bookName.intern()) {
                            if (Files.notExists(folderPath)) Files.createDirectories(folderPath);
                        }
                    }
                    Files.writeString(filePath, chapter.chapterContext(), StandardCharsets.UTF_8);
                    return CompletableFuture.completedFuture(new Chapter.Chapter4Merge(Integer.valueOf(chapter.chapterOrdid()), folderPath, filePath, bookName));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }
}