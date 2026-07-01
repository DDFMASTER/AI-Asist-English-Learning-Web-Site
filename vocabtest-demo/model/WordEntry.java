package vocabtest.model;

/**
 * 表示COCA词频表中的一个词条，包含单词文本及其在词频表中的排位（1-based）。
 * 去重后每个独立词仅保留首次出现的排位。
 */
public class WordEntry {
    private final String word;
    private final int rank;

    public WordEntry(String word, int rank) {
        this.word = word;
        this.rank = rank;
    }

    public String getWord() {
        return word;
    }

    /** COCA词频排位，从1开始 */
    public int getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return word + " (#" + rank + ")";
    }
}
