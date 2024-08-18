package shop.zailushang.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shop.zailushang.entity.Chapter;
import shop.zailushang.entity.PartBook;
import shop.zailushang.flow.FlowEngine;
import shop.zailushang.utils.IOForkJoinTask;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 组件：执行文件合并操作
 */
@FunctionalInterface
public interface Merger extends Task<List<Chapter.Chapter4Merge>, IOForkJoinTask.Result> {

    @Override
    default CompletableFuture<IOForkJoinTask.Result> execute(List<Chapter.Chapter4Merge> sources) {
        return merge(sources);
    }

    CompletableFuture<IOForkJoinTask.Result> merge(List<Chapter.Chapter4Merge> sources);

    class Mergers {
        private static final Logger logger = LoggerFactory.getLogger(Mergers.class);

        public static Merger fileMerger() {
            return sources -> {
                logger.info("{} - 执行文件合并操作 size => {}", Thread.currentThread().getName(), sources.size());

                // 章节序号集合
                var orderIds = sources.stream()
                        .map(Chapter.Chapter4Merge::orderId)
                        .toList();

                // 获取开始索引和结束索引
                var startIndex = orderIds.stream().min(Integer::compareTo).get();
                var endIndex = orderIds.stream().max(Integer::compareTo).get();

                // 计算整本书的字节数 也等于：最后一章的跳过字节数skip + 自身字节数
                var totalLength = sources.stream()
                        .map(Chapter.Chapter4Merge::filePath)
                        .map(path -> {
                            try {
                                return Files.size(path);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .reduce(0L, Long::sum);

                // capacity 表示，每个线程合并处理 100 章内容，此数值不宜设置过小，如拆分粒度过细，则任务过多会导致另类“死锁”问题，线程池的资源耗尽，全部线程都陷入阻塞等待。
                var capacity = 100;
                // 组装 IOForkJoinTask 所需任务数据
                var partBook = new PartBook(sources, startIndex, endIndex, capacity, totalLength, FlowEngine.IO_TASK_EXECUTOR);

                return CompletableFuture.completedFuture(partBook)
                        .thenApplyAsync(PartBook::compute, FlowEngine.IO_TASK_EXECUTOR);
            };
        }
    }
}
