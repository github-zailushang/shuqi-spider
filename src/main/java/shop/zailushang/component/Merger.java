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
    default CompletableFuture<Chapter.Chapter4Clean> execute(List<Chapter.Chapter4Merge> sources) throws Exception {
        return merge(sources);
    }

    CompletableFuture<Chapter.Chapter4Clean> merge(List<Chapter.Chapter4Merge> sources) throws Exception;

    // 组件名
    static String name() {
        return "「撰」";
    }

    @Slf4j
    class Mergers {

        static {
            log.info("敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}", Merger.name());
        }

        public static Merger fileMerger() {
            return sources -> {
                log.info("{} - 执行文件合并操作 待合并文件数量 => {}", Merger.name(), sources.size());


                // 章节序号集合
                var orderIds = sources.stream()
                        .map(Chapter.Chapter4Merge::orderId)
                        .toList();

                // 组装 IOForkJoinTask 所需任务数据
                var partBook = new PartBook(sources, orderIds.getFirst(), orderIds.getLast(), FlowEngine.IO_TASK_EXECUTOR);

                var bookName = sources.getFirst().bookName();
                var paths = sources.stream().map(Chapter.Chapter4Merge::filePath).toList();

                // 提交异步任务
                return CompletableFuture.completedFuture(partBook)
                        .thenApplyAsync(PartBook::compute, FlowEngine.IO_TASK_EXECUTOR)
                        .whenComplete((result, throwable) -> log.info("{} - 执行文件合并操作 成功合并文件数量 => {}", Merger.name(), result.successful()))
                        .thenApplyAsync(unused -> new Chapter.Chapter4Clean(bookName, paths), FlowEngine.IO_TASK_EXECUTOR);// 继续向后传递文件列表
            };
        }
    }
}
