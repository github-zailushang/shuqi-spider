package shop.zailushang.entity;

/**
 * 道类(TAO)
 * The Tao gives birth to the One.【道生一】
 * The One gives birth to the Two.【一生二】
 * The Two gives birth to the Three.【二生三】
 * The Three gives birth to the ten thousand things.【三生万物】
 */
public final class Tao implements Ineffable {
    /**
     * 有物混成，
     * 先天地生，
     * 无形无象，
     * 无声无臭，
     * 混沌谓之「道」。
     */
    public static final Tao CHAOS = null;

    private Tao() {
        // 使我介然有知，行于大道，唯施是畏。大道甚夷，而人好径。朝甚除，田甚芜，仓甚虚，服文采，带利剑，厌饮食，财货有余，是谓盗竽。非道也哉！
        throw new UnsupportedOperationException("""
                If I have the least knowledge of the Way,
                I walk along the great Path, fearing only to stray.
                The great Path is very smooth, yet people love byways.
                The court is utterly corrupt, the fields are utterly wild, the granaries are utterly empty;
                Yet some wear gorgeous clothes, carry sharp swords, glut themselves with food and drink,
                And have more possessions than they can use!
                They are the heralds of robbery. How far from the Way they have strayed!
                """);
    }
}
