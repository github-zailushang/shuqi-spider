package shop.zailushang.component;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.entity.Chapter;
import shop.zailushang.entity.Tao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
            return chapter4Clean -> CompletableFuture.completedFuture(chapter4Clean)
                    .whenCompleteAsync((_, _) -> log.info("{} - 执行文件删除操作", Cleaner.name()), taskExecutor())
                    .thenApplyAsync(Chapter.Chapter4Clean::paths, taskExecutor())
                    .thenApplyAsync(List::parallelStream, taskExecutor())
                    .thenApplyAsync(pathStream -> pathStream.filter(Chapter.Chapter4Clean::needDelete), taskExecutor())// 过滤 是否删除
                    .thenApplyAsync(pathStream -> pathStream.map(CompletableFuture::completedFuture), taskExecutor())// 每条路径创建异步任务删除
                    .thenApplyAsync(completedFutureStream -> completedFutureStream.map(pathCompletedFuture -> pathCompletedFuture.thenApplyAsync(Cleaner::clean, taskExecutor())), taskExecutor())// 执行删除任务
                    .thenApplyAsync(completedFutureStream -> completedFutureStream.map(pathCompletedFuture -> pathCompletedFuture.whenCompleteAsync((path, _) -> log.info("{} - 删除文件成功：{}", Cleaner.name(), path), taskExecutor())), taskExecutor())// log
                    .whenCompleteAsync((completedFutureStream, _) -> completedFutureStream.forEach(CompletableFuture::join), taskExecutor())// 等待所有任务完成
                    .thenApplyAsync(_ -> Tao.CHAOS, taskExecutor());// 回归本道
        }
    }
}
