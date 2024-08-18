package shop.zailushang.entity;

import shop.zailushang.utils.IOForkJoinTask;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

public record PartBook(List<Chapter.Chapter4Merge> sources, Integer startIndex, Integer endIndex, Integer capacity,
                       Long totalLength, ExecutorService executor) implements IOForkJoinTask<PartBook> {

    @Override
    public Result doCompute() {
        var logger = logger();

        logger.info(String.format("%s - 准备合并 [%s ~ %s]", Thread.currentThread().getName(), startIndex, endIndex));
        // 所在的文件夹路径 每个对象的路径均一样
        var start = sources.get(startIndex - 1);
        var bookName = start.bookName();
        var folderPath = start.folderPath();
        var skip = start.skip();

        var sourceChannels = sources.stream()
                .sorted(Comparator.comparing(Chapter.Chapter4Merge::orderId))
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

        var target = String.format("%s/%s.txt", folderPath.toString(), bookName);
        try (var targetFile = new RandomAccessFile(target, "rw"); var targetFileChannel = targetFile.getChannel()) {
            // 设置文件总长度
            targetFile.setLength(totalLength);
            // 移动文件指针
            targetFile.seek(skip);
            var atoLong = new AtomicLong(0);
            sourceChannels.forEach(sourceChannel -> {
                try (sourceChannel) {
//                    sourceChannel.transferTo(0, sourceChannel.size(), targetFileChannel); 使用下面的直接映射内存方式复制文件更高效
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