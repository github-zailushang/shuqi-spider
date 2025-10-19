package shop.zailushang.component;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.entity.Chapter;
import shop.zailushang.entity.PartBook;
import shop.zailushang.flow.FlowEngine;
import shop.zailushang.util.BookCache;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static shop.zailushang.component.Task.taskExecutor;

/**
 * 组件：执行文件合并操作
 */
@FunctionalInterface
public interface Merger extends Task<List<Chapter.Chapter4Merge>, Chapter.Chapter4Clean> {

    @Override
    default CompletableFuture<Chapter.Chapter4Clean> execute(List<Chapter.Chapter4Merge> chapter4Merges) throws Exception {
        return merge(chapter4Merges);
    }

    CompletableFuture<Chapter.Chapter4Clean> merge(List<Chapter.Chapter4Merge> chapter4Merges) throws Exception;

    // 组件名
    static String name() {
        return "「撰」";
    }

    @Slf4j
    class Mergers {

        static {
            log.info("\u001B[35m敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}\u001B[0m", Merger.name());
        }

        public static Merger fileMerger() {
            return chapter4Merges -> CompletableFuture.completedFuture(chapter4Merges)
                    .whenCompleteAsync((_, _) -> log.info("{} - 执行文件合并操作 待合并文件数量 => {}", Merger.name(), chapter4Merges.size()), taskExecutor())
                    .thenApplyAsync(PartBook::of, taskExecutor())
                    .thenApplyAsync(PartBook::compute, taskExecutor())// 提交异步任务
                    .whenCompleteAsync((result, _) -> log.info("{} - 执行文件合并操作 成功合并文件数量 => {}", Merger.name(), result.successful()), taskExecutor())
                    .whenCompleteAsync((_, _) -> BookCache.removeFileChannel(FlowEngine.BOOK_NAME.get()), taskExecutor())// 合并完成时关闭文件通道
                    .thenApplyAsync(_ -> Chapter.Chapter4Clean.of(chapter4Merges), taskExecutor());// 继续向后传递文件列表
        }
    }
}
