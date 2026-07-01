package Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 词汇量测试服务 — 端口自 vocabtest-demo。
 * 负责加载词频数据、分层抽样、词汇量估算。
 */
public class VocabTestService {

    // ==================== 词频段配置 ====================
    private static final int[][] BAND_RANGES = {
        {101, 500},
        {501, 1500},
        {1501, 3000},
        {3001, 5000},
        {5001, 7500},
        {7501, 10000},
        {10001, 13000},
        {13001, 16000},
        {16001, 20000}
    };
    public static final int BAND_COUNT = BAND_RANGES.length;
    private static final int KNOWN_FUNCTION_WORDS = 100;
    private static final double Z_95 = 1.96;

    // ==================== 单例 ====================
    private static VocabTestService instance;
    private final List<List<String>> bands = new ArrayList<>(BAND_COUNT);
    private final List<String> fakeWords = new ArrayList<>();
    private final int[] bandTotalCounts = new int[BAND_COUNT];
    private final Random random = new Random();

    private VocabTestService() {
        for (int i = 0; i < BAND_COUNT; i++) {
            bands.add(new ArrayList<>());
        }
        loadData();
    }

    public static synchronized VocabTestService getInstance() {
        if (instance == null) {
            instance = new VocabTestService();
        }
        return instance;
    }

    // ==================== 数据加载 ====================

    private void loadData() {
        try {
            loadCOCA();
            loadFakeWords();
        } catch (IOException e) {
            throw new RuntimeException("词汇测试数据加载失败", e);
        }
    }

    private void loadCOCA() throws IOException {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("vocabtest/COCA.txt");
        if (is == null) {
            throw new FileNotFoundException("vocabtest/COCA.txt not found in resources");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            Map<String, Integer> wordToRank = new LinkedHashMap<>();
            int rank = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.strip();
                if (!word.isEmpty()) {
                    wordToRank.putIfAbsent(word, rank);
                }
                rank++;
            }
            // 按频段分组
            for (Map.Entry<String, Integer> e : wordToRank.entrySet()) {
                int r = e.getValue();
                if (r <= KNOWN_FUNCTION_WORDS) continue;
                for (int b = 0; b < BAND_COUNT; b++) {
                    if (r >= BAND_RANGES[b][0] && r <= BAND_RANGES[b][1]) {
                        bands.get(b).add(e.getKey());
                        break;
                    }
                }
            }
            for (int i = 0; i < BAND_COUNT; i++) {
                bandTotalCounts[i] = bands.get(i).size();
            }
        }
    }

    private void loadFakeWords() throws IOException {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("vocabtest/fakewords.txt");
        if (is == null) {
            throw new FileNotFoundException("vocabtest/fakewords.txt not found in resources");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.strip();
                if (!word.isEmpty()) {
                    fakeWords.add(word);
                }
            }
        }
    }

    // ==================== 抽样 ====================

    /**
     * 生成初始 100 个测试词（真词 70-87 + 伪词 13-30，各组 7-13 个）。
     */
    public List<TestWord> sampleInitial() {
        Set<String> usedReal = new HashSet<>();
        Set<String> usedFake = new HashSet<>();
        return sampleWords(usedReal, usedFake, 100);
    }

    /**
     * 生成新增 10 个测试词（伪词 0-3 个）。
     */
    public List<TestWord> sampleMore(Set<String> usedReal, Set<String> usedFake) {
        return sampleWords(usedReal, usedFake, 10);
    }

    private List<TestWord> sampleWords(Set<String> usedReal, Set<String> usedFake, int total) {
        List<TestWord> items = new ArrayList<>();
        int groupCount = BAND_COUNT + 1; // 9 real bands + 1 fake
        int minPerGroup = (total == 100) ? 7 : 0;
        int maxPerGroup = (total == 100) ? 13 : 3;

        int[] alloc = new int[groupCount];
        int allocated = 0;

        // 分配伪词
        int fakeAlloc = (total == 100) ? 7 + random.nextInt(7)  // 7-13
                      : random.nextInt(Math.min(4, total) + 1);   // 0-3
        alloc[BAND_COUNT] = Math.min(fakeAlloc, total);
        allocated += alloc[BAND_COUNT];

        // 分配真词
        int remaining = total - allocated;
        for (int b = 0; b < BAND_COUNT; b++) {
            alloc[b] = minPerGroup;
            remaining -= minPerGroup;
        }
        while (remaining > 0) {
            int idx = random.nextInt(BAND_COUNT);
            if (alloc[idx] < maxPerGroup) {
                alloc[idx]++;
                remaining--;
            }
        }

        // 从各频段抽样真词
        for (int b = 0; b < BAND_COUNT; b++) {
            List<String> pool = bands.get(b);
            List<String> available = new ArrayList<>();
            for (String w : pool) {
                if (!usedReal.contains(w)) available.add(w);
            }
            Collections.shuffle(available, random);
            int take = Math.min(alloc[b], available.size());
            for (int i = 0; i < take; i++) {
                String w = available.get(i);
                usedReal.add(w);
                items.add(new TestWord(w, false, b));
            }
        }

        // 抽样伪词
        List<String> availableFake = new ArrayList<>();
        for (String fw : fakeWords) {
            if (!usedFake.contains(fw)) availableFake.add(fw);
        }
        Collections.shuffle(availableFake, random);
        int takeFake = Math.min(alloc[BAND_COUNT], availableFake.size());
        for (int i = 0; i < takeFake; i++) {
            String fw = availableFake.get(i);
            usedFake.add(fw);
            items.add(new TestWord(fw, true, -1));
        }

        // 打乱顺序
        Collections.shuffle(items, random);
        return items;
    }

    // ==================== 词汇量估算 ====================

    /**
     * 根据用户回答估算词汇量。
     * @param answers 按顺序，"1"=认识, "0"=不认识
     * @param testWords 测试词列表
     * @return 估算结果
     */
    public EstimateResult estimate(List<String> answers, List<TestWord> testWords) {
        int[] n = new int[BAND_COUNT];  // 各频段测试数
        int[] k = new int[BAND_COUNT];  // 各频段认识数
        int nFake = 0;
        int kFake = 0;

        for (int i = 0; i < testWords.size() && i < answers.size(); i++) {
            TestWord tw = testWords.get(i);
            boolean known = "1".equals(answers.get(i));
            if (tw.isFake) {
                nFake++;
                if (known) kFake++;
            } else {
                int b = tw.bandIndex;
                if (b >= 0 && b < BAND_COUNT) {
                    n[b]++;
                    if (known) k[b]++;
                }
            }
        }

        // 原始认识率
        double[] pRaw = new double[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            pRaw[i] = (n[i] > 0) ? (double) k[i] / n[i] : 0.0;
        }

        // 伪词命中率
        double pf = (nFake > 0) ? (double) kFake / nFake : 0.0;

        // 矫正率 r_i = max(0, p_i - 0.5 * pf)
        double[] r = new double[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            r[i] = Math.max(0.0, pRaw[i] - 0.5 * pf);
        }

        // 总词汇量
        double totalV = KNOWN_FUNCTION_WORDS;
        double totalN = 0;
        for (int i = 0; i < BAND_COUNT; i++) {
            totalV += bandTotalCounts[i] * r[i];
            totalN += bandTotalCounts[i];
        }

        // 方差
        double varianceV = 0.0;
        for (int i = 0; i < BAND_COUNT; i++) {
            if (n[i] > 0) {
                varianceV += bandTotalCounts[i] * bandTotalCounts[i]
                        * pRaw[i] * (1.0 - pRaw[i]) / n[i];
            }
        }
        if (nFake > 0) {
            varianceV += 0.25 * totalN * totalN * pf * (1.0 - pf) / nFake;
        }
        double sigmaV = Math.sqrt(varianceV);

        return new EstimateResult(
            (int) Math.round(totalV),
            totalV - Z_95 * sigmaV,
            totalV + Z_95 * sigmaV,
            r, pRaw, pf,
            n, k, nFake, kFake
        );
    }

    // ==================== 内部类 ====================

    public static class TestWord {
        public final String word;
        public final boolean isFake;
        public final int bandIndex;

        public TestWord(String word, boolean isFake, int bandIndex) {
            this.word = word;
            this.isFake = isFake;
            this.bandIndex = bandIndex;
        }
    }

    public static class EstimateResult {
        public final int estimatedVocabulary;
        public final double lowerCI;
        public final double upperCI;
        public final double[] bandCorrectedRates;
        public final double[] bandRawRates;
        public final double fakeHitRate;
        public final int[] bandSampleCounts;
        public final int[] bandKnownCounts;
        public final int fakeTotal;
        public final int fakeKnown;

        public EstimateResult(int estimatedVocabulary, double lowerCI, double upperCI,
                              double[] bandCorrectedRates, double[] bandRawRates,
                              double fakeHitRate, int[] bandSampleCounts,
                              int[] bandKnownCounts, int fakeTotal, int fakeKnown) {
            this.estimatedVocabulary = estimatedVocabulary;
            this.lowerCI = lowerCI;
            this.upperCI = upperCI;
            this.bandCorrectedRates = bandCorrectedRates;
            this.bandRawRates = bandRawRates;
            this.fakeHitRate = fakeHitRate;
            this.bandSampleCounts = bandSampleCounts;
            this.bandKnownCounts = bandKnownCounts;
            this.fakeTotal = fakeTotal;
            this.fakeKnown = fakeKnown;
        }
    }
}
