package shop.zailushang.entity;

import shop.zailushang.utils.IOForkJoinTask;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * IOForkJoinTask 实际子类
 *
 * @param sources     要处理的资源
 * @param startIndex  起始索引
 * @param endIndex    结束索引
 * @param capacity    可以处理的资源数
 * @param totalLength 要处理的资源的总字节数
 *                    对应为电子书所有章节加起来的总字节数，因为要使用 RandomAccessFile 配合多线程实现文件复制
 *                    使用时，一定要先设置文件的总大小，再移动文件指针，否则肯定乱序、覆盖写等问题
 *                    {@link RandomAccessFile#setLength} 设置文件大小
 *                    {@link RandomAccessFile#seek} 移动文件指针
 * @param executor    线程池
 */
public record PartBook(List<Chapter.Chapter4Merge> sources, Integer startIndex, Integer endIndex, Integer capacity,
                       Long totalLength, ExecutorService executor) implements IOForkJoinTask<PartBook> {

    @Override
    public Result doCompute() {
        var logger = logger();

        logger.info(String.format("%s - 准备合并 [%s ~ %s]", Thread.currentThread().getName(), startIndex, endIndex));
        // 获取由本线程处理的首个资源
        var start = sources.get(startIndex - 1);
        var bookName = start.bookName();
        var folderPath = start.folderPath();
        // 跳过字节数 ： 比如本线程负责合并 50 ~ 120，则在写入文件时，先设置跳过前 49 章的总字节数
        var skip = start.skip();

        // Chapter4Merge mapTo FileChannel
        var sourceChannels = sources.stream()
                .sorted(Comparator.comparing(Chapter.Chapter4Merge::orderId))// 文件排序
                .skip(startIndex - 1)
                .limit(endIndex - startIndex + 1)
                .map(Chapter.Chapter4Merge::filePath)
                .map(path -> {
                    try {
                        return FileChannel.open(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        // 合并的文件名以书名命名 e.g D:/斗破苍穹/斗破苍穹.txt
        var target = String.format("%s/%s.txt", folderPath.toString(), bookName);
        try (var targetFile = new RandomAccessFile(target, "rw"); var targetFileChannel = targetFile.getChannel()) {
            // 设置文件总长度
            targetFile.setLength(totalLength);
            // 移动文件指针
            targetFile.seek(skip);
            var atoLong = new AtomicLong(0);
            sourceChannels.forEach(sourceChannel -> {
                try (sourceChannel) {
                    // 弃用，使用下面的直接映射内存方式复制文件更高效
                    // long byteSize = sourceChannel.transferTo(0, sourceChannel.size(), targetFileChannel);
                    int byteSize = targetFileChannel.write(sourceChannel.map(FileChannel.MapMode.READ_ONLY, 0, sourceChannel.size()));
                    atoLong.addAndGet(byteSize);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            var successful = endIndex - startIndex + 1;
            var byteSize = atoLong.get();
            logger.info(String.format("%s - 合并完成 [%s ~ %s : %s, %s]", Thread.currentThread().getName(), startIndex, endIndex, successful, byteSize));
            return new Result(successful, byteSize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 二分法拆分任务
    @Override
    @SuppressWarnings("unchecked")
    public IOForkJoinTask<PartBook>[] doFork() {
        var medianIndex = (startIndex + endIndex) >> 1;// = (endIndex - startIndex) / 2 + startIndex
        var left = new PartBook(sources, startIndex, medianIndex, capacity, totalLength, executor);
        var right = new PartBook(sources, medianIndex + 1, endIndex, capacity, totalLength, executor);
        logger().info(String.format("%s - 执行拆分 left[%s ~ %s],right[%s ~ %s]", Thread.currentThread().getName(), left.startIndex, left.endIndex, right.startIndex, right.endIndex));
        return new IOForkJoinTask[]{left, right};
    }
}