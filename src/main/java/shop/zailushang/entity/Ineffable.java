package shop.zailushang.entity;

/**
 * 不可道 ：「道可道，非常道，道之不清，言之不明」
 */
public interface Ineffable {
    default void explain() {
        throw new UnsupportedOperationException("The Tao that can be trodden is not the enduring and unchanging Tao. — Lao Tzu");
    }
}