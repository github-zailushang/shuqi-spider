package shop.zailushang.component;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 组件：从响应文本中挑选所需的元素
 */
@FunctionalInterface
public interface Selector<T, R> extends Task<T, R> {
    @Override
    default CompletableFuture<R> execute(T doc) {
        return select(doc);
    }

    CompletableFuture<R> select(T doc);

    @SuppressWarnings("unused")
    static <T> Selector<T, T> identity() {
        return CompletableFuture::completedFuture;
    }

    class Selectors {
        public static final Logger logger = LoggerFactory.getLogger(Selectors.class);

        // bid元素选择器
        public static Selector<String, String> bidSelector() {
            // bid 所在的元素地址
            // <span class="btn js-addShelf disable" data-bid="53258" data-clog="shelf-shelf$$bid=53258">+书架</span>
            final var bidXpath = "/html/body/div/div[3]/div[2]/div/div/div[2]/span[2]";

            return bidDoc -> {
                logger.info("{} - 执行选择bid元素操作", Thread.currentThread().getName());
                return CompletableFuture.completedFuture(
                        Jsoup.parse(bidDoc)
                                .selectXpath(bidXpath)
                                .getFirst()
                                .attr("data-bid")
                );
            };
        }

        // 章节列表元素选择器
        public static Selector<String, String> chapterSelector() {
            var chapterXpath = "/html/body/i[5]";
            return chapterDoc -> {
                logger.info("{} - 执行选择章节列表元素操作", Thread.currentThread().getName());
                return CompletableFuture.completedFuture(
                        Jsoup.parse(chapterDoc)
                                .selectXpath(chapterXpath)
                                .text());
            };
        }

        // 章节内容元素选择器
        public static Selector<String, String> contentSelector() {
            // 不方便加日志，弃用
//            return Selector.identity();
            // 章节内容直接为 json 字符串，无需额外选择器，走个流程
            return doc -> {
                logger.info("{} - 执行选择章节内容元素操作", Thread.currentThread().getName());
                return CompletableFuture.completedFuture(doc);
            };
        }
    }
}