package shop.zailushang.component;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import shop.zailushang.entity.Chapter;

import java.util.concurrent.CompletableFuture;

/**
 * 组件：从响应文本中挑选所需的元素
 */
@FunctionalInterface
public interface Selector<T, R> extends Task<T, R> {
    @Override
    default CompletableFuture<R> execute(T doc) throws Exception {
        return select(doc);
    }

    CompletableFuture<R> select(T doc) throws Exception;

    @SuppressWarnings("unused")
    static <T> Selector<T, T> identity() {
        return CompletableFuture::completedFuture;
    }

    // 组件名
    static String name() {
        return "「择」";
    }

    @Slf4j
    class Selectors {

        static {
            log.info("敕令：「天圆地方，律令九章，吾今下笔，万鬼伏藏。」 ~ {}", Selector.name());
        }

        // bid元素选择器
        public static Selector<String, String> bidSelector() {
            // bid 所在的元素地址
            // <span class="btn js-addShelf disable" data-bid="53258" data-clog="shelf-shelf$$bid=53258">+书架</span>
            // 网站调整了 bid 元素位置
            final var bidXpath = "/html/body/div[1]/div[3]/div/div[4]/div/span[2]";

            return bidDoc -> {
                log.info("{} - 执行选择bid元素操作", Selector.name());
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
            final var chapterXpath = "/html/body/i[5]";
            return chapterDoc -> {
                log.info("{} - 执行选择章节列表元素操作", Selector.name());
                return CompletableFuture.completedFuture(
                        Jsoup.parse(chapterDoc)
                                .selectXpath(chapterXpath)
                                .text());
            };
        }

        // 章节内容元素选择器
        public static Selector<Chapter.Chapter4Select, Chapter.Chapter4Parse> contentSelector() {
            // 章节内容直接为 json 字符串，无需额外选择器，走个流程
            return chapter4Select -> {
                log.info("{} - 执行选择章节内容元素操作", Selector.name());
                var chapter4Parse = new Chapter.Chapter4Parse(chapter4Select.bookName(), chapter4Select.chapterName(), chapter4Select.chapterOrdid(), chapter4Select.jsonCiphertext());
                return CompletableFuture.completedFuture(chapter4Parse);
            };
        }
    }
}