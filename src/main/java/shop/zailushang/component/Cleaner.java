package shop.zailushang.component;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.entity.Chapter;
import shop.zailushang.entity.Tao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static shop.zailushang.component.Task.parallelTask;
import static shop.zailushang.component.Task.taskExecutor;

/**
 * 组件：合并完成后删除零散的章节文件
 */
@FunctionalInterface
public interface Cleaner extends Task<Chapter.Chapter4Clean, Tao> {

    @Override
    default CompletableFuture<Tao> execute(Chapter.Chapter4Clean chapter4Clean) throws Exception {
        return clean(chapter4Clean);
    }

    CompletableFuture<Tao> clean(Chapter.Chapter4Clean chapter4Clean) throws Exception;

    // 删除文件
    static CompletableFuture<Path> clean0(Path path) {
        try {
            Files.deleteIfExists(path);
            return CompletableFuture.completedFuture(path);
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
            return chapter4Clean -> {
                log.info("{} - 执行文件删除操作", Cleaner.name());
                return parallelTask(
                        paths -> paths.stream().filter(Chapter.Chapter4Clean::needDelete).toList(),
                        Cleaner::clean0,
                        paths -> paths.stream().peek(path -> log.info("{} - 删除文件成功：{}", Cleaner.name(), path)).toList())
                        .apply(chapter4Clean.paths())
                        .thenApplyAsync(_ -> Tao.TAO, taskExecutor()); // 大道至简，万法归宗
            };
        }
    }
}
