package shop.zailushang.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shop.zailushang.entity.Chapter;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 組件：内容格式化器，调整解密后的章节内容排版、操作包括：用\n 替换 <br/>, 去除收尾空白，拼接章节标题
 */
@FunctionalInterface
public interface Formatter extends Task<Chapter.Chapter4Save, Chapter.Chapter4Save> {

    @Override
    default CompletableFuture<Chapter.Chapter4Save> execute(Chapter.Chapter4Save chapter) {
        return format(chapter);
    }

    CompletableFuture<Chapter.Chapter4Save> format(Chapter.Chapter4Save chapter);

    class Formatters {
        private static final Logger logger = LoggerFactory.getLogger(Formatters.class);

        public static Formatter contentFormatter() {
            return chapter -> {
                logger.info("{} - 执行章节内容格式化操作", Thread.currentThread().getName());
                var chapterContext = chapter.chapterContext()
                        // 替换换行符
                        .replaceAll("<br/>", "\n")
                        .lines()
                        // 去除首尾空白
                        .map(String::strip)
                        // 去除空白符后需要另行添加行尾添加换行
                        .collect(Collectors.joining("\n"))
                        // 拼接章节标题、行尾添加两个换行，方便后续文件合并时操作
                        .transform(str -> String.format("%s\n%s\n\n", chapter.chapterName(), str));
                return CompletableFuture.completedFuture(new Chapter.Chapter4Save(chapter.bookName(), chapter.chapterName(), chapter.chapterOrdid(), chapterContext));
            };
        }
    }
}