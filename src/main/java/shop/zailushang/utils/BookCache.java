package shop.zailushang.utils;

import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 书籍缓存类：用于记录一些书籍在下载过程中的所需数据
public class BookCache {
    // 默认文件夹路径 e.g. D:/斗破苍穹
    private static final String FOLDER_FORMATTER = "D:/%s";
    // 默认文件路径 e.g. D:/斗破苍穹/1.txt or D:/斗破苍穹/斗破苍穹.txt
    private static final String FILE_PATH_FORMATTER = "D:/%s/%s.txt";
    // 文件通道缓存
    private static final Map<String, FileChannel> FILE_CHANNEL_MAP = new ConcurrentHashMap<>();

    // 获取文件夹路径
    public static Path getFolderPath(String bookName) {
        try {
            return Paths.get(String.format(FOLDER_FORMATTER, bookName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 获取文件路径
    public static Path getFilePath(String bookName, String chapterOrdid) {
        try {
            return Paths.get(String.format(FILE_PATH_FORMATTER, bookName, chapterOrdid));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 获取文件通道
    public static FileChannel getFileChannel(String bookName) {
        return FILE_CHANNEL_MAP.computeIfAbsent(bookName, k -> {
            try {
                // 文件夹路径
                var folderPath = getFolderPath(bookName);
                // 计算合并后的文件总长度（字节）
                var totalLength = Files.list(folderPath)
                        .map(path -> {
                            try {
                                return Files.size(path);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .reduce(0L, Long::sum);
                // 合并后的目标文件路径 e.g. D:/斗破苍穹/斗破苍穹.txt
                var targetFileChannel = FileChannel.open(Paths.get(String.format(FILE_PATH_FORMATTER, bookName, bookName)), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                // 预设置文件总大小，避免重复扩容，提升写入性能
                targetFileChannel.truncate(totalLength);
                return targetFileChannel;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // 删除文件通道
    public static void removeFileChannel(String bookName) {
        FILE_CHANNEL_MAP.computeIfPresent(bookName, (k, v) -> {
            try {
                // 关闭文件通道
                v.close();
                // 删除文件通道缓存
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
