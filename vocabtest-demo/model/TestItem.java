package vocabtest.model;

/**
 * 测试界面中的一个测试项，可以是真词或伪词。
 */
public class TestItem {
    private final String word;
    private final boolean isFake;
    /** 所属频段索引（0-8对应9个真词频段），伪词为-1 */
    private final int bandIndex;
    private boolean checked;

    public TestItem(String word, boolean isFake, int bandIndex) {
        this.word = word;
        this.isFake = isFake;
        this.bandIndex = bandIndex;
        this.checked = false;
    }

    public String getWord() {
        return word;
    }

    public boolean isFake() {
        return isFake;
    }

    public int getBandIndex() {
        return bandIndex;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
