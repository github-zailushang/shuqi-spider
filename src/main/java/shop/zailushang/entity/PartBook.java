package shop.zailushang.entity;

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

    public String name() {
        return "「「「器之三」」」";
    }

    // 默认每个线程处理 100章内容
    public PartBook(List<Chapter.Chapter4Merge> sources, Integer startIndex, Integer endIndex, ExecutorService executor) {
        this(sources, startIndex, endIndex, 100, executor);
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
                    var sourceChannel = chapter4Merge.fileChannel();
                    var skip = chapter4Merge.skip();
                    try (sourceChannel) {
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