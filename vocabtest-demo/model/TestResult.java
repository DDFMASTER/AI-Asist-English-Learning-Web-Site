package vocabtest.model;

/**
 * 词汇量测试结果，包含估算词汇量及95%置信区间。
 */
public class TestResult {
    private final int estimatedVocabulary;
    private final double lowerCI;
    private final double upperCI;
    /** 各频段校正后的认识率 */
    private final double[] bandCorrectedRates;
    /** 各频段原始认识率 */
    private final double[] bandRawRates;
    /** 伪词命中率 */
    private final double fakeHitRate;
    /** 各频段总词数 */
    private final int[] bandTotalCounts;
    /** 各频段测试词数 */
    private final int[] bandSampleCounts;
    /** 各频段认识词数 */
    private final int[] bandKnownCounts;

    public TestResult(int estimatedVocabulary, double lowerCI, double upperCI,
                      double[] bandCorrectedRates, double[] bandRawRates,
                      double fakeHitRate, int[] bandTotalCounts,
                      int[] bandSampleCounts, int[] bandKnownCounts) {
        this.estimatedVocabulary = estimatedVocabulary;
        this.lowerCI = lowerCI;
        this.upperCI = upperCI;
        this.bandCorrectedRates = bandCorrectedRates;
        this.bandRawRates = bandRawRates;
        this.fakeHitRate = fakeHitRate;
        this.bandTotalCounts = bandTotalCounts;
        this.bandSampleCounts = bandSampleCounts;
        this.bandKnownCounts = bandKnownCounts;
    }

    public int getEstimatedVocabulary() {
        return estimatedVocabulary;
    }

    public double getLowerCI() {
        return lowerCI;
    }

    public double getUpperCI() {
        return upperCI;
    }

    public double[] getBandCorrectedRates() {
        return bandCorrectedRates;
    }

    public double[] getBandRawRates() {
        return bandRawRates;
    }

    public double getFakeHitRate() {
        return fakeHitRate;
    }

    public int[] getBandTotalCounts() {
        return bandTotalCounts;
    }

    public int[] getBandSampleCounts() {
        return bandSampleCounts;
    }

    public int[] getBandKnownCounts() {
        return bandKnownCounts;
    }
}
