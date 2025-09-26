package shop.zailushang.component;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.entity.Chapter;
import shop.zailushang.flow.FlowEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * 组件：合并完成后删除零散的章节文件
 */
@FunctionalInterface
public interface Cleaner extends Task<Chapter.Chapter4Clean, Void> {

    @Override
    default CompletableFuture<Void> execute(Chapter.Chapter4Clean chapter) throws Exception {
        return clean(chapter);
    }

    CompletableFuture<Void> clean(Chapter.Chapter4Clean chapter) throws Exception;

    // 删除文件
    static Path clean(Path path) {
        try {
            Files.deleteIfExists(path);
            return path;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 组件名
    static String name() {
        return "「涤」";
    }

    @Slf4j
    class Cleaners {

        static {
            log.info("\u001B[35m敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}\u001B[0m", Cleaner.name());
        }

        public static Cleaner fileCleaner() {
            return chapter -> {
                log.info("{} - 执行文件删除操作", Cleaner.name());
                if (FlowEngine.NEED_DELETE) {
                    chapter.paths()
                            .stream()
                            .map(CompletableFuture::completedFuture)
                            .forEach(future -> future.thenApplyAsync(Cleaner::clean, FlowEngine.IO_TASK_EXECUTOR)
                                    .whenComplete((path, throwable) -> log.info("{} - 删除文件成功：{}", Cleaner.name(), path))
                                    .join());
                }
                return CompletableFuture.completedFuture(null);
            };
        }
    }
}
