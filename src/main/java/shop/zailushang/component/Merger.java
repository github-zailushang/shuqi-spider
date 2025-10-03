package shop.zailushang.component;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.entity.Chapter;
import shop.zailushang.entity.PartBook;
import shop.zailushang.flow.FlowEngine;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
                    .whenComplete((r, e) -> log.info("{} - 执行文件合并操作 待合并文件数量 => {}", Merger.name(), chapter4Merges.size()))
                    .thenApplyAsync(PartBook::new, FlowEngine.IO_TASK_EXECUTOR)
                    .thenApplyAsync(PartBook::compute, FlowEngine.IO_TASK_EXECUTOR)// 提交异步任务
                    .whenComplete((result, throwable) -> log.info("{} - 执行文件合并操作 成功合并文件数量 => {}", Merger.name(), result.successful()))
                    .thenApplyAsync(unused -> new Chapter.Chapter4Clean(chapter4Merges.getFirst().bookName(), chapter4Merges.stream().map(Chapter.Chapter4Merge::filePath).toList()), FlowEngine.IO_TASK_EXECUTOR);// 继续向后传递文件列表
        }
    }
}
