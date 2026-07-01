package vocabtest.service;

import vocabtest.model.TestItem;
import vocabtest.model.WordEntry;

import java.util.*;

/**
 * 分层抽样器：按词频段和伪词进行分层随机抽样。
 * 初始测试抽取恰好100个单词（含伪词），
 * "更多单词"每次抽取10个单词（伪词0-3个）。
 */
public class WordSampler {

    private final Random random = new Random();

    /** 10个采样组：索引0-8为9个真词频段，索引9为伪词组 */
    private static final int GROUP_COUNT = DataLoader.BAND_COUNT + 1;
    private static final int FAKE_GROUP_INDEX = DataLoader.BAND_COUNT; // 9

    /** 初始总测试词数 */
    private static final int INITIAL_TOTAL = 100;
    /** 每组最小采样数 */
    private static final int MIN_PER_GROUP = 7;
    /** 每组最大采样数 */
    private static final int MAX_PER_GROUP = 13;

    /** 更多单词每次数量 */
    private static final int MORE_TOTAL = 10;
    /** 更多单词中伪词最大数量 */
    private static final int MORE_MAX_FAKE = 3;

    // 记录已使用的真词（按频段）和伪词，防止重复
    private final List<Set<String>> usedRealWords;
    private final Set<String> usedFakeWords;

    public WordSampler() {
        usedRealWords = new ArrayList<>(DataLoader.BAND_COUNT);
        for (int i = 0; i < DataLoader.BAND_COUNT; i++) {
            usedRealWords.add(new HashSet<>());
        }
        usedFakeWords = new HashSet<>();
    }

    /**
     * 初始抽样：恰好100个单词。
     * @param bands 9个真词频段的词条列表
     * @param fakeWords 伪词列表
     * @return 100个TestItem
     */
    public List<TestItem> sampleInitial(List<List<WordEntry>> bands, List<String> fakeWords) {
        int[] allocation = allocateInitial();
        return sampleByAllocation(bands, fakeWords, allocation);
    }

    /**
     * "更多单词"抽样：10个单词，伪词0-3个。
     * @param bands 9个真词频段的词条列表
     * @param fakeWords 伪词列表
     * @return 10个TestItem
     */
    public List<TestItem> sampleMore(List<List<WordEntry>> bands, List<String> fakeWords) {
        int[] allocation = allocateMore();
        return sampleByAllocation(bands, fakeWords, allocation);
    }

    /**
     * 分配初始100词的各组合计数：每组7-13，总和恰好100。
     */
    private int[] allocateInitial() {
        int[] alloc = new int[GROUP_COUNT];
        Arrays.fill(alloc, MIN_PER_GROUP);
        int remaining = INITIAL_TOTAL - MIN_PER_GROUP * GROUP_COUNT; // 100 - 70 = 30

        // 随机将剩余30分配到各组（不超过上限13）
        while (remaining > 0) {
            int idx = random.nextInt(GROUP_COUNT);
            if (alloc[idx] < MAX_PER_GROUP) {
                alloc[idx]++;
                remaining--;
            }
        }
        return alloc;
    }

    /**
     * 分配"更多单词"10词的各组合计数：伪词0-3，真词各频段随机分配。
     */
    private int[] allocateMore() {
        int[] alloc = new int[GROUP_COUNT];
        int fakeCount = random.nextInt(MORE_MAX_FAKE + 1); // 0-3
        alloc[FAKE_GROUP_INDEX] = fakeCount;

        int realRemaining = MORE_TOTAL - fakeCount; // 7-10
        // 随机分配到9个真词频段
        for (int i = 0; i < realRemaining; i++) {
            int bandIdx = random.nextInt(DataLoader.BAND_COUNT);
            alloc[bandIdx]++;
        }
        return alloc;
    }

    /**
     * 按分配数量从各频段和伪词池中抽样，确保不重复。
     */
    private List<TestItem> sampleByAllocation(List<List<WordEntry>> bands,
                                               List<String> fakeWords,
                                               int[] allocation) {
        List<TestItem> items = new ArrayList<>();

        // 从9个真词频段抽样
        for (int b = 0; b < DataLoader.BAND_COUNT; b++) {
            int need = allocation[b];
            List<WordEntry> pool = bands.get(b);
            Set<String> used = usedRealWords.get(b);

            List<WordEntry> available = new ArrayList<>();
            for (WordEntry we : pool) {
                if (!used.contains(we.getWord())) {
                    available.add(we);
                }
            }

            // 随机选取（若可用词不足，全部取出）
            Collections.shuffle(available, random);
            int take = Math.min(need, available.size());
            for (int i = 0; i < take; i++) {
                WordEntry we = available.get(i);
                used.add(we.getWord());
                items.add(new TestItem(we.getWord(), false, b));
            }
        }

        // 从伪词池抽样
        int fakeNeed = allocation[FAKE_GROUP_INDEX];
        List<String> availableFake = new ArrayList<>();
        for (String fw : fakeWords) {
            if (!usedFakeWords.contains(fw)) {
                availableFake.add(fw);
            }
        }
        Collections.shuffle(availableFake, random);
        int takeFake = Math.min(fakeNeed, availableFake.size());
        for (int i = 0; i < takeFake; i++) {
            String fw = availableFake.get(i);
            usedFakeWords.add(fw);
            items.add(new TestItem(fw, true, -1));
        }

        // 打乱顺序使伪词与真词混合显示
        Collections.shuffle(items, random);
        return items;
    }

    /**
     * 重置已使用的词记录（用于重新开始测试）。
     */
    public void reset() {
        for (Set<String> s : usedRealWords) {
            s.clear();
        }
        usedFakeWords.clear();
    }
}
