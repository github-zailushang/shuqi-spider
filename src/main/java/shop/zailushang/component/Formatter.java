package shop.zailushang.component;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.entity.Chapter;
import shop.zailushang.flow.FlowEngine;
import shop.zailushang.utils.Assert;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 組件：内容格式化器，调整解密后的章节内容排版
 * 操作包括：
 *      用\n 替换 <br/>
 *      去除首尾空白
 *      去除空白行
 *      拼接章节标题
 *      章节尾部拼接双换行
 */
@FunctionalInterface
public interface Formatter extends Task<Chapter.Chapter4Format, Chapter.Chapter4Write> {

    @Override
    default CompletableFuture<Chapter.Chapter4Write> execute(Chapter.Chapter4Format chapter) throws Exception {
        return format(chapter);
    }

    CompletableFuture<Chapter.Chapter4Write> format(Chapter.Chapter4Format chapter) throws Exception;

    // 组件名
    static String name() {
        return "「椠」";
    }

    @Slf4j
    class Formatters {

        static {
            log.info("\u001B[35m敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}\u001B[0m", Formatter.name());
        }

        public static Formatter contentFormatter() {
            return chapter -> CompletableFuture.completedFuture(chapter)
                    .whenComplete((r, e) -> log.info("{} - 执行章节内容格式化操作", Formatter.name()))
                    .thenApplyAsync(Chapter.Chapter4Format::unformattedChapterContent, FlowEngine.IO_TASK_EXECUTOR)
                    .thenApplyAsync(unformattedChapterContent -> unformattedChapterContent.replaceAll("<br/>", "\n"), FlowEngine.IO_TASK_EXECUTOR)// 替换换行符
                    .thenApplyAsync(String::lines, FlowEngine.IO_TASK_EXECUTOR)
                    .thenApplyAsync(stringStream -> stringStream.filter(Assert::strNotBlank), FlowEngine.IO_TASK_EXECUTOR)// 去除空白行
                    .thenApplyAsync(stringStream -> stringStream.map(String::strip), FlowEngine.IO_TASK_EXECUTOR)// 去除行首行尾空格
                    .thenApplyAsync(stringStream -> stringStream.collect(Collectors.joining("\n")), FlowEngine.IO_TASK_EXECUTOR)// 重新拼接换行
                    .thenApplyAsync(chapterContext -> String.format("%s\n%s\n\n", chapter.chapterName(), chapterContext), FlowEngine.IO_TASK_EXECUTOR)// 拼接章节名，行尾添加两个换行符，方便后续文件合并
                    .thenApplyAsync(chapterContext -> new Chapter.Chapter4Write(chapter.bookName(), chapter.chapterName(), chapter.chapterOrdid(), chapterContext), FlowEngine.IO_TASK_EXECUTOR);
        }
    }
}