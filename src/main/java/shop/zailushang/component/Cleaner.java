package shop.zailushang.component;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.entity.Chapter;
import shop.zailushang.entity.Tao;
import shop.zailushang.util.CheckedExceptionFucker;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static shop.zailushang.component.Task.taskExecutor;

/**
 * 组件：合并完成后删除零散的章节文件
 */
@FunctionalInterface
public interface Cleaner<T, R> extends Task<T, R> {

    @Override
    default CompletableFuture<R> execute(T t) throws Exception {
        return clean(t);
    }

    CompletableFuture<R> clean(T t) throws Exception;

    // 组件名
    static String name() {
        return "「涤」";
    }

    @Slf4j
    class Cleaners {

        static {
            log.info("\u001B[35m敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}\u001B[0m", Cleaner.name());
        }

        public static Cleaner<Chapter.Chapter4Clean, Tao> fileCleaner() {
            final var cleanParallelTask = Task.parallelTask(
                    paths -> paths.stream().filter(Chapter.Chapter4Clean::needDelete).toList() // 前置筛选（二元决策：全有或全无）
                    , singleCleaner()); // 删除逻辑
            return chapter4Clean -> CompletableFuture.completedFuture(chapter4Clean)
                    .whenCompleteAsync((_, _) -> log.info("{} - 执行文件删除操作", Cleaner.name()), taskExecutor())
                    .thenApplyAsync(Chapter.Chapter4Clean::paths, taskExecutor())
                    .thenComposeAsync(cleanParallelTask, taskExecutor())
                    .thenApplyAsync(_ -> Tao.TAO, taskExecutor()); // 百川入海，万法归宗，回归本道
        }

        // 删除单个文件
        static Cleaner<Path, Path> singleCleaner() {
            return path -> CompletableFuture.completedFuture(path)
                    .thenApplyAsync(CheckedExceptionFucker::deleteIfExists, taskExecutor())
                    .whenCompleteAsync((_, _) -> log.info("{} - 删除文件成功：{}", Cleaner.name(), path), taskExecutor());
        }
    }
}
