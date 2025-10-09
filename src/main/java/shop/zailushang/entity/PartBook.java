package shop.zailushang.entity;

import lombok.extern.slf4j.Slf4j;
import shop.zailushang.flow.FlowEngine;
import shop.zailushang.utils.BookCache;
import shop.zailushang.utils.IOForkJoinTask;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * IOForkJoinTask 实际子类
 *
 * @param sources    要处理的资源
 * @param startIndex 起始索引
 * @param endIndex   结束索引
 * @param capacity   可以处理的资源数
 * @param executor   线程池
 */
@Slf4j
public record PartBook(List<Chapter.Chapter4Merge> sources, Integer startIndex, Integer endIndex, Integer capacity,
                       ExecutorService executor) implements IOForkJoinTask<PartBook> {

    // 从 sources 构造
    public static PartBook of(List<Chapter.Chapter4Merge> sources) {
        var orderIds = sources.stream().map(Chapter.Chapter4Merge::orderId).toList();
        return new PartBook(sources, orderIds.getFirst(), orderIds.getLast(), FlowEngine.DEFAULT_CAPACITY, FlowEngine.IO_TASK_EXECUTOR);
    }

    public String name() {
        return "「镇坛木」";
    }

    @Override
    public Result doCompute() {
        var name = name();
        log.info("{} - 准备合并 [{} ~ {}]", name, startIndex, endIndex);

        // 获取由当前线程处理的首个资源
        var first = sources.get(startIndex - 1);
        var bookName = first.bookName();
        // 获取目标文件通道
        var targetFileChannel = BookCache.getFileChannel(bookName);
        var atoLong = new AtomicLong(0);
        sources.stream()
                .skip(startIndex - 1)
                .limit(endIndex - startIndex + 1)
                .forEach(chapter4Merge -> {
                    var skip = chapter4Merge.skip();
                    try (var sourceChannel = chapter4Merge.fileChannel()) {
                        var byteSize = targetFileChannel.transferFrom(sourceChannel, skip, sourceChannel.size());// 零拷贝
                        atoLong.addAndGet(byteSize);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        var successful = endIndex - startIndex + 1;
        var byteSize = atoLong.get();
        log.info("{} - 合并完成 [{} ~ {} : {}, {}]", name, startIndex, endIndex, successful, byteSize);
        return new Result(successful, byteSize);
    }

    // 二分法拆分任务
    @Override
    public PartBook[] doFork() {
        var medianIndex = (startIndex + endIndex) >> 1;// = (endIndex - startIndex) / 2 + startIndex
        var left = new PartBook(sources, startIndex, medianIndex, capacity, executor);
        var right = new PartBook(sources, medianIndex + 1, endIndex, capacity, executor);
        log.info("{} - 执行拆分 left[{} ~ {}],right[{} ~ {}]", name(), left.startIndex, left.endIndex, right.startIndex, right.endIndex);
        return new PartBook[]{left, right};
    }
}