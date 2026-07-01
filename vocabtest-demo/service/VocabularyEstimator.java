package vocabtest.service;

import vocabtest.model.TestItem;
import vocabtest.model.TestResult;

import java.util.List;

/**
 * 词汇量估算器。
 * 采用温和矫正公式，并计算含协方差的95%置信区间。
 *
 * 估算公式：
 *   r_i = max(0, p_i - 0.5 × p_f)
 *   V = 100 + Σ N_i × r_i
 *
 * 方差（含协方差项）：
 *   Var(V) = Σ N_i² × p_i(1-p_i)/n_i + 0.25 × (Σ N_i)² × p_f(1-p_f)/n_f
 *
 * 协方差源于伪词命中率 p_f 同时影响所有频段的矫正率 r_i，
 * 导致 Cov(r_i, r_j) = 0.25 × Var(p_f)  (i≠j)。
 */
public class VocabularyEstimator {

    /** 默认掌握的功能词数 */
    private static final int KNOWN_FUNCTION_WORDS = 100;

    /** 95%置信区间的z值 */
    private static final double Z_95 = 1.96;

    /**
     * 根据用户的勾选结果估算词汇量。
     * @param items 所有测试项
     * @param bandTotalCounts 各频段去重后总词数 N_i
     * @return 测试结果（含估算词汇量与置信区间）
     */
    public static TestResult estimate(List<TestItem> items, int[] bandTotalCounts) {
        int bandCount = DataLoader.BAND_COUNT;

        // 统计各频段及伪词的测试数与认识数
        int[] n = new int[bandCount];   // 各频段测试词数
        int[] k = new int[bandCount];   // 各频段认识词数
        int nFake = 0;
        int kFake = 0;

        for (TestItem item : items) {
            if (item.isFake()) {
                nFake++;
                if (item.isChecked()) {
                    kFake++;
                }
            } else {
                int b = item.getBandIndex();
                if (b >= 0 && b < bandCount) {
                    n[b]++;
                    if (item.isChecked()) {
                        k[b]++;
                    }
                }
            }
        }

        // 计算各频段原始认识率 p_i
        double[] p = new double[bandCount];
        for (int i = 0; i < bandCount; i++) {
            p[i] = (n[i] > 0) ? (double) k[i] / n[i] : 0.0;
        }

        // 伪词命中率 p_f
        double pf = (nFake > 0) ? (double) kFake / nFake : 0.0;

        // 矫正后的各频段认识率 r_i = max(0, p_i - 0.5 × pf)
        double[] r = new double[bandCount];
        for (int i = 0; i < bandCount; i++) {
            r[i] = Math.max(0.0, p[i] - 0.5 * pf);
        }

        // 估算总词汇量 V = 100 + Σ N_i × r_i
        double totalV = KNOWN_FUNCTION_WORDS;
        double totalN = 0; // Σ N_i
        for (int i = 0; i < bandCount; i++) {
            totalV += bandTotalCounts[i] * r[i];
            totalN += bandTotalCounts[i];
        }

        // 方差计算（含协方差项）
        // Var(V) = Σ N_i² × p_i(1-p_i)/n_i + 0.25 × (Σ N_i)² × p_f(1-p_f)/n_f
        double varianceV = 0.0;

        // 第一部分：各频段独立方差
        for (int i = 0; i < bandCount; i++) {
            if (n[i] > 0) {
                double N_i = bandTotalCounts[i];
                varianceV += N_i * N_i * p[i] * (1.0 - p[i]) / n[i];
            }
        }

        // 第二部分：伪词命中率带来的协方差项
        // 0.25 × (Σ N_i)² × pf(1-pf)/nf
        if (nFake > 0) {
            varianceV += 0.25 * totalN * totalN * pf * (1.0 - pf) / nFake;
        }

        double sigmaV = Math.sqrt(varianceV);

        double lowerCI = totalV - Z_95 * sigmaV;
        double upperCI = totalV + Z_95 * sigmaV;

        return new TestResult(
            (int) Math.round(totalV),
            lowerCI,
            upperCI,
            r, p, pf,
            bandTotalCounts,
            n, k
        );
    }
}
