package Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import Utils.DBUtil;

/**
 * 词汇量卡片测试服务 — 从 vocab_test 表抽词、AI 生成选项、计算得分。
 *
 * 与 VocabTestService（词频段估算）不同，本服务采用 AI 释义选择题模式，
 * 包含真词（4 选 1 + 认识）和伪词（4 个假释义 + 不认识）的诚信检测。
 */
public class VocabTestCardService {

    private static final String AI_URL = "https://api.deepseek.com/chat/completions";
    private static final String AI_KEY = "sk-a74d91663df347dd92087fdac3d8aa9e";
    private static final String AI_MODEL = "deepseek-v4-flash";

    private static final int TOTAL_REAL = 90;
    private static final int TOTAL_PSEUDO = 10;
    public static final int TOTAL_WORDS = TOTAL_REAL + TOTAL_PSEUDO;

    /** 单次 AI 批量生成的词数上限 */
    private static final int BATCH_SIZE = 10;

    /** AI 选项缓存：wordLower → CachedOptions */
    private static final Map<String, CachedOptions> optionCache = new ConcurrentHashMap<>();

    private final HttpClient httpClient;

    public VocabTestCardService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    // ==================== 词汇抽样 ====================

    /**
     * 从 vocab_test 表随机抽取真词和伪词，混合打乱后返回。
     */
    public List<CardWord> sampleWords() {
        List<CardWord> words = new ArrayList<>();

        // 真词：is_pseudo = 0
        words.addAll(queryRandomWords(false, TOTAL_REAL));
        // 伪词：is_pseudo = 1
        words.addAll(queryRandomWords(true, TOTAL_PSEUDO));

        // 打乱顺序
        Collections.shuffle(words, new Random());
        return words;
    }

    private List<CardWord> queryRandomWords(boolean isPseudo, int count) {
        List<CardWord> result = new ArrayList<>();
        String sql = "SELECT vocab_id, word FROM vocab_test WHERE is_pseudo = ? ORDER BY RAND() LIMIT ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, isPseudo ? 1 : 0);
            ps.setInt(2, count);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new CardWord(
                        rs.getInt("vocab_id"),
                        rs.getString("word"),
                        isPseudo
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[VocabTestCard] 查询词汇失败: " + e.getMessage());
        }
        return result;
    }

    // ==================== AI 选项生成 ====================

    /**
     * 为一批单词生成选项（优先从缓存取，未命中则调用 AI）。
     */
    public List<WordOptions> generateOptions(List<CardWord> batch) {
        List<CardWord> needAI = new ArrayList<>();
        List<WordOptions> results = new ArrayList<>();

        for (CardWord cw : batch) {
            String key = cw.word.toLowerCase();
            CachedOptions cached = optionCache.get(key);
            if (cached != null) {
                results.add(new WordOptions(cw.vocabId, cw.word, cw.isPseudo,
                        cached.options, cached.correctIndex));
            } else {
                needAI.add(cw);
            }
        }

        if (!needAI.isEmpty()) {
            List<WordOptions> aiResults = callAIForOptions(needAI);
            for (WordOptions wo : aiResults) {
                optionCache.put(wo.word.toLowerCase(), new CachedOptions(wo.options, wo.correctIndex));
            }
            results.addAll(aiResults);
        }

        return results;
    }

    /**
     * 调用 AI 生成选项（批量）。
     */
    private List<WordOptions> callAIForOptions(List<CardWord> batch) {
        List<WordOptions> results = new ArrayList<>();

        try {
            String prompt = buildBatchPrompt(batch);
            String requestBody = "{"
                    + "\"model\":\"" + AI_MODEL + "\","
                    + "\"messages\":["
                    + "{\"role\":\"system\",\"content\":\"你是一个英语词汇测试助手。严格只输出 JSON。\"},"
                    + "{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}"
                    + "],"
                    + "\"response_format\":{\"type\":\"json_object\"},"
                    + "\"temperature\":0.7"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AI_URL))
                    .header("Authorization", "Bearer " + AI_KEY)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                String content = extractContent(body);
                if (content != null) {
                    List<WordOptions> parsed = parseBatchResult(content, batch);
                    // 过滤掉选项为空的结果（AI 可能返回空字符串）
                    for (WordOptions wo : parsed) {
                        if (wo.options != null && !wo.options.isEmpty()) {
                            boolean allNonEmpty = true;
                            for (String opt : wo.options) {
                                if (opt == null || opt.trim().isEmpty()) {
                                    allNonEmpty = false;
                                    break;
                                }
                            }
                            if (allNonEmpty) {
                                results.add(wo);
                            }
                        }
                    }
                }
            } else {
                System.err.println("[VocabTestCard] AI 返回错误: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("[VocabTestCard] AI 调用失败: " + e.getMessage());
        }

        // 如果 AI 失败，为每个未生成的词提供 fallback
        Set<Integer> doneIds = new HashSet<>();
        for (WordOptions wo : results) doneIds.add(wo.vocabId);
        for (CardWord cw : batch) {
            if (!doneIds.contains(cw.vocabId)) {
                results.add(fallbackOptions(cw));
            }
        }

        // 真词的选项随机打乱（确保正确答案不总是在 A）
        Random rand = new Random();
        for (WordOptions wo : results) {
            if (!wo.isPseudo && wo.correctIndex >= 0 && wo.correctIndex < wo.options.size()) {
                String correct = wo.options.get(wo.correctIndex);
                List<String> shuffled = new ArrayList<>(wo.options);
                java.util.Collections.shuffle(shuffled, rand);
                int newIdx = shuffled.indexOf(correct);
                wo.options.clear();
                wo.options.addAll(shuffled);
                wo.correctIndex = newIdx;
            }
        }

        return results;
    }

    /** 构建批量 AI prompt */
    private String buildBatchPrompt(List<CardWord> batch) {
        StringBuilder sb = new StringBuilder();
        sb.append("为以下英语单词生成中文释义选项，用于词汇量测试。\n\n");

        for (int i = 0; i < batch.size(); i++) {
            CardWord cw = batch.get(i);
            sb.append("单词").append(i + 1).append(": \"").append(cw.word).append("\"");
            if (cw.isPseudo) {
                sb.append("（这是一个虚构的假词，不存在于英语中）\n");
                sb.append("要求：生成 4 个中文释义，全部都是错误释义。"
                        + "4 个释义应覆盖不同领域（如动物、工具、情感、动作），彼此差异要大，读起来要像真词的释义。\n");
            } else {
                sb.append("\n要求：生成 4 个中文释义，其中 1 个是正确的，其余 3 个是错误的。\n");
                sb.append("关键规则：\n");
                sb.append("  1) 4 个释义必须属于明显不同的语义类别，不能全是近义词，要让真正认识该词的人一眼就能分辨\n");
                sb.append("  2) 错误释义要与正确释义意思相差足够远，不要玩文字游戏\n");
                sb.append("  3) 正确释义必须随机放在 4 个位置中的任意一个，不要总是放在第一个\n");
                sb.append("  4) 语言自然口语化，像词典释义而非学术论文\n");
            }
            sb.append("\n");
        }

        sb.append("请严格以如下 JSON 格式输出：\n");
        sb.append("{\n");
        sb.append("  \"words\": [\n");
        sb.append("    {\n");
        sb.append("      \"index\": 1,\n");
        sb.append("      \"options\": [\"释义1\", \"释义2\", \"释义3\", \"释义4\"],\n");
        sb.append("      \"correctIndex\": 2\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("注意：\n");
        sb.append("- 真词的 correctIndex 是 0-3 的整数，指向 options 中正确释义的位置（随机分布）\n");
        sb.append("- 假词的 correctIndex 设为 -1\n");
        sb.append("- 每个释义 2-10 个汉字，简洁自然的口语化表达\n");
        sb.append("- 正确释义和错误释义的语义差距要足够大，不要只在细微处做文章");

        return sb.toString();
    }

    /** 从 AI 响应中提取 content 字段 */
    private String extractContent(String responseBody) {
        int contentIdx = responseBody.indexOf("\"content\"");
        if (contentIdx < 0) return null;
        int colonIdx = responseBody.indexOf(":", contentIdx);
        if (colonIdx < 0) return null;
        int startQuote = responseBody.indexOf("\"", colonIdx + 1);
        if (startQuote < 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = startQuote + 1; i < responseBody.length(); i++) {
            char c = responseBody.charAt(i);
            if (c == '\\' && i + 1 < responseBody.length()) {
                char next = responseBody.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case 'n': sb.append('\n'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    default: sb.append(c); break;
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 解析 AI 批量返回结果 */
    private List<WordOptions> parseBatchResult(String content, List<CardWord> batch) {
        List<WordOptions> results = new ArrayList<>();

        int wordsIdx = content.indexOf("\"words\"");
        if (wordsIdx < 0) return results;
        int bracketIdx = content.indexOf("[", wordsIdx);
        if (bracketIdx < 0) return results;

        for (int i = 0; i < batch.size(); i++) {
            CardWord cw = batch.get(i);

            // 找下一个 "index"
            int idxPos = content.indexOf("\"index\"", bracketIdx);
            if (idxPos < 0) break;
            int idxColon = content.indexOf(":", idxPos);
            if (idxColon < 0) break;
            int idxStart = idxColon + 1;
            while (idxStart < content.length() && content.charAt(idxStart) == ' ') idxStart++;
            int idxEnd = idxStart;
            while (idxEnd < content.length() && content.charAt(idxEnd) >= '0' && content.charAt(idxEnd) <= '9') idxEnd++;
            int aiIndex = 0;
            try { aiIndex = Integer.parseInt(content.substring(idxStart, idxEnd)); } catch (NumberFormatException e) {}
            bracketIdx = idxEnd;

            // 找 options 数组
            int optIdx = content.indexOf("\"options\"", bracketIdx);
            if (optIdx < 0) break;
            int optBracket = content.indexOf("[", optIdx);
            if (optBracket < 0) break;
            // 提取 4 个字符串
            List<String> opts = new ArrayList<>();
            int cur = optBracket + 1;
            for (int j = 0; j < 4 && cur < content.length(); j++) {
                int qStart = content.indexOf("\"", cur);
                if (qStart < 0) break;
                StringBuilder opt = new StringBuilder();
                int k = qStart + 1;
                while (k < content.length()) {
                    char c = content.charAt(k);
                    if (c == '\\' && k + 1 < content.length()) { k += 2; continue; }
                    if (c == '"') { k++; break; }
                    opt.append(c);
                    k++;
                }
                opts.add(opt.toString());
                cur = k;
            }
            bracketIdx = cur;

            // 找 correctIndex
            int corrIdx = content.indexOf("\"correctIndex\"", bracketIdx);
            int correctIndex = cw.isPseudo ? -1 : 0;
            if (corrIdx >= 0) {
                int corrColon = content.indexOf(":", corrIdx);
                if (corrColon >= 0) {
                    int ciStart = corrColon + 1;
                    while (ciStart < content.length() && content.charAt(ciStart) == ' ') ciStart++;
                    int ciEnd = ciStart;
                    if (ciEnd < content.length() && content.charAt(ciEnd) == '-') ciEnd++;
                    while (ciEnd < content.length() && content.charAt(ciEnd) >= '0' && content.charAt(ciEnd) <= '9') ciEnd++;
                    try { correctIndex = Integer.parseInt(content.substring(ciStart, ciEnd)); } catch (NumberFormatException e) {}
                }
                bracketIdx = corrIdx + 1;
            }

            if (!opts.isEmpty()) {
                results.add(new WordOptions(cw.vocabId, cw.word, cw.isPseudo, opts, correctIndex));
            }
        }

        return results;
    }

    /** 当 AI 调用失败时使用 fallback 选项 */
    private WordOptions fallbackOptions(CardWord cw) {
        if (cw.isPseudo) {
            return new WordOptions(cw.vocabId, cw.word, true,
                    Arrays.asList("一种传统乐器", "一种化学物质", "一种海洋生物", "一种哲学概念"), -1);
        } else {
            return new WordOptions(cw.vocabId, cw.word, false,
                    Arrays.asList("(正确答案)", "(错误选项1)", "(错误选项2)", "(错误选项3)"), 0);
        }
    }

    // ==================== 结果计算 ====================

    /**
     * 计算测试结果。
     * @param answers 用户每题的回答: "A"/"B"/"C"/"D"/"know"/"dontknow"
     * @param words 测试词列表
     * @param wordOptionsList 每题对应的选项
     */
    public CardTestResult calculateResult(List<String> answers, List<CardWord> words,
                                          List<WordOptions> wordOptionsList) {
        int total = answers.size();
        int correct = 0;
        int pseudoTotal = 0;
        int pseudoCorrect = 0;
        int realTotal = 0;
        int realCorrect = 0;

        for (int i = 0; i < total; i++) {
            CardWord cw = words.get(i);
            String ans = answers.get(i);
            WordOptions wo = i < wordOptionsList.size() ? wordOptionsList.get(i) : null;

            boolean isCorrect;
            if (cw.isPseudo) {
                pseudoTotal++;
                // 伪词：只有选"不认识"才算对
                isCorrect = "dontknow".equals(ans);
                if (isCorrect) pseudoCorrect++;
            } else {
                realTotal++;
                // 真词：选对选项 或 选"认识" 都算对
                if ("know".equals(ans)) {
                    isCorrect = true;
                } else if (wo != null && ans != null && ans.length() == 1) {
                    int selectedIdx = ans.charAt(0) - 'A';
                    isCorrect = (selectedIdx == wo.correctIndex);
                } else {
                    isCorrect = false;
                }
                if (isCorrect) realCorrect++;
            }
            if (isCorrect) correct++;
        }

        // 估算词汇量（基于正确率 + 伪词诚信修正）
        double realRate = realTotal > 0 ? (double) realCorrect / realTotal : 0;
        double pseudoRate = pseudoTotal > 0 ? (double) pseudoCorrect / pseudoTotal : 0;
        // 诚信度修正：伪词正确率低说明诚实，加权系数 0-1
        double honestyFactor = pseudoRate; // 伪词正确率越高越诚实（选不认识多）
        double adjustedRate = realRate * (0.7 + 0.3 * honestyFactor);

        // 词汇量映射：正确率 * 参考词汇量
        int estimatedVocab = (int) Math.round(adjustedRate * 12000);
        estimatedVocab = Math.max(500, Math.min(20000, estimatedVocab));

        String cefrLevel;
        if (estimatedVocab < 1500) cefrLevel = "A1";
        else if (estimatedVocab < 3000) cefrLevel = "A2";
        else if (estimatedVocab < 5000) cefrLevel = "B1";
        else if (estimatedVocab < 8000) cefrLevel = "B2";
        else if (estimatedVocab < 12000) cefrLevel = "C1";
        else cefrLevel = "C2";

        String cefrLabel;
        switch (cefrLevel) {
            case "A1": cefrLabel = "初级"; break;
            case "A2": cefrLabel = "初级上"; break;
            case "B1": cefrLabel = "中级"; break;
            case "B2": cefrLabel = "中高级"; break;
            case "C1": cefrLabel = "高级"; break;
            case "C2": cefrLabel = "精通"; break;
            default: cefrLabel = "中级";
        }

        return new CardTestResult(correct, total, pseudoCorrect, pseudoTotal,
                realCorrect, realTotal, estimatedVocab, cefrLevel, cefrLabel,
                (int) Math.round(honestyFactor * 100));
    }

    // ==================== JSON 转义 ====================

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    // ==================== 内部类 ====================

    /** 测试词汇（从数据库抽取） */
    public static class CardWord {
        public final int vocabId;
        public final String word;
        public final boolean isPseudo;

        public CardWord(int vocabId, String word, boolean isPseudo) {
            this.vocabId = vocabId;
            this.word = word;
            this.isPseudo = isPseudo;
        }
    }

    /** AI 生成的选项 */
    public static class WordOptions {
        public final int vocabId;
        public final String word;
        public final boolean isPseudo;
        public final List<String> options;
        public int correctIndex; // -1 表示伪词（全部错误），shuffle 后会修改

        public WordOptions(int vocabId, String word, boolean isPseudo,
                          List<String> options, int correctIndex) {
            this.vocabId = vocabId;
            this.word = word;
            this.isPseudo = isPseudo;
            this.options = options;
            this.correctIndex = correctIndex;
        }
    }

    /** 缓存条目 */
    private static class CachedOptions {
        final List<String> options;
        final int correctIndex;

        CachedOptions(List<String> options, int correctIndex) {
            this.options = options;
            this.correctIndex = correctIndex;
        }
    }

    /** 测试结果 */
    public static class CardTestResult {
        public final int correct;
        public final int total;
        public final int pseudoCorrect;
        public final int pseudoTotal;
        public final int realCorrect;
        public final int realTotal;
        public final int estimatedVocab;
        public final String cefrLevel;
        public final String cefrLabel;
        public final int honestyPercent;

        public CardTestResult(int correct, int total, int pseudoCorrect, int pseudoTotal,
                             int realCorrect, int realTotal, int estimatedVocab,
                             String cefrLevel, String cefrLabel, int honestyPercent) {
            this.correct = correct;
            this.total = total;
            this.pseudoCorrect = pseudoCorrect;
            this.pseudoTotal = pseudoTotal;
            this.realCorrect = realCorrect;
            this.realTotal = realTotal;
            this.estimatedVocab = estimatedVocab;
            this.cefrLevel = cefrLevel;
            this.cefrLabel = cefrLabel;
            this.honestyPercent = honestyPercent;
        }
    }
}
