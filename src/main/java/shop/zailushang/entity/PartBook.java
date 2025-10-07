package shop.zailushang.entity;

import shop.zailushang.flow.FlowEngine;
import shop.zailushang.utils.BookCache;
import shop.zailushang.utils.IOForkJoinTask;

import java.nio.channels.FileChannel;
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
        var logger = logger();

        logger.info("{} - 准备合并 [{} ~ {}]", name, startIndex, endIndex);

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
                        var mappedByteBuffer = sourceChannel.map(FileChannel.MapMode.READ_ONLY, 0, sourceChannel.size());
                        var byteSize = targetFileChannel.write(mappedByteBuffer, skip);
                        atoLong.addAndGet(byteSize);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        var successful = endIndex - startIndex + 1;
        var byteSize = atoLong.get();
        logger.info("{} - 合并完成 [{} ~ {} : {}, {}]", name, startIndex, endIndex, successful, byteSize);
        return new Result(successful, byteSize);
    }

    // 二分法拆分任务
    @Override
    @SuppressWarnings("unchecked")
    public IOForkJoinTask<PartBook>[] doFork() {
        var medianIndex = (startIndex + endIndex) >> 1;// = (endIndex - startIndex) / 2 + startIndex
        var left = new PartBook(sources, startIndex, medianIndex, capacity, executor);
        var right = new PartBook(sources, medianIndex + 1, endIndex, capacity, executor);
        logger().info("{} - 执行拆分 left[{} ~ {}],right[{} ~ {}]", name(), left.startIndex, left.endIndex, right.startIndex, right.endIndex);
        return new IOForkJoinTask[]{left, right};
    }
}