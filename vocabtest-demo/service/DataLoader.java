package vocabtest.service;

import vocabtest.model.WordEntry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * 数据加载器：从文件中加载COCA词频表和伪词表。
 * COCA去重：同一单词多次出现时保留首次（最低排位）。
 * 分词频段：按排位将单词划分到9个词频段。
 */
public class DataLoader {

    /** 9个词频段的排位区间（1-based，闭区间） */
    public static final int[][] BAND_RANGES = {
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

    /** 词频段数量 */
    public static final int BAND_COUNT = BAND_RANGES.length;

    /** 默认掌握的功能词数（排位1-100视为已知） */
    public static final int KNOWN_FUNCTION_WORDS = 100;

    /**
     * 加载COCA词频表，去重（保留首次出现），按排位升序返回。
     * @param dataDir 数据目录路径
     * @return 去重后的词条列表
     */
    public static List<WordEntry> loadCOCA(String dataDir) throws IOException {
        Path path = Paths.get(dataDir, "COCA.txt");
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        // LinkedHashMap保持插入顺序，确保去重时保留首次出现的排位
        Map<String, Integer> wordToRank = new LinkedHashMap<>();
        int rank = 1;
        for (String line : lines) {
            String word = line.strip();
            if (!word.isEmpty()) {
                wordToRank.putIfAbsent(word, rank);
            }
            rank++;
        }

        List<WordEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> e : wordToRank.entrySet()) {
            entries.add(new WordEntry(e.getKey(), e.getValue()));
        }
        return entries;
    }

    /**
     * 将去重后的词条按排位分组到9个词频段。
     * @param entries 去重后的词条列表
     * @return 9个列表，每个列表包含属于该频段的词条
     */
    public static List<List<WordEntry>> groupIntoBands(List<WordEntry> entries) {
        List<List<WordEntry>> bands = new ArrayList<>(BAND_COUNT);
        for (int i = 0; i < BAND_COUNT; i++) {
            bands.add(new ArrayList<>());
        }

        for (WordEntry entry : entries) {
            int rank = entry.getRank();
            // 排位1-100为默认掌握的功能词，不纳入测试
            if (rank <= KNOWN_FUNCTION_WORDS) {
                continue;
            }
            for (int b = 0; b < BAND_COUNT; b++) {
                if (rank >= BAND_RANGES[b][0] && rank <= BAND_RANGES[b][1]) {
                    bands.get(b).add(entry);
                    break;
                }
            }
        }
        return bands;
    }

    /**
     * 加载伪词表。
     * @param dataDir 数据目录路径
     * @return 伪词列表
     */
    public static List<String> loadFakeWords(String dataDir) throws IOException {
        Path path = Paths.get(dataDir, "fakewords.txt");
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<String> fakeWords = new ArrayList<>();
        for (String line : lines) {
            String word = line.strip();
            if (!word.isEmpty()) {
                fakeWords.add(word);
            }
        }
        return fakeWords;
    }

    /**
     * 获取第b个频段的总词数（用于估算公式中的N_i）。
     */
    public static int[] getBandTotalCounts(List<List<WordEntry>> bands) {
        int[] counts = new int[BAND_COUNT];
        for (int i = 0; i < BAND_COUNT; i++) {
            counts[i] = bands.get(i).size();
        }
        return counts;
    }
}
