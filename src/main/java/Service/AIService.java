package Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

/**
 * AI 服务 — 调用 DeepSeek API 为英文单词生成例句。
 * 使用 Java 内置 HttpClient，无需额外依赖。
 */
public class AIService {

    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String API_KEY = "sk-***" + ""; // 部署时替换为真实 key
    private static final String MODEL = "deepseek-v4-flash";
    private static final int TIMEOUT_SECONDS = 15;

    private static final String SYSTEM_PROMPT =
            "用户将向你提供一个英文单词。请为该单词生成 3 个不同难度/场景的英文例句，每个例句附带中文翻译。\n" +
            "\n" +
            "要求：\n" +
            "1. 例句应覆盖不同语境（如日常对话、学术写作、新闻阅读）\n" +
            "2. 例句长度适中（10-25 个单词）\n" +
            "3. 中文翻译准确自然\n" +
            "\n" +
            "请严格以如下 JSON 格式输出（不要输出其他内容）：\n" +
            "{\n" +
            "  \"word\": \"单词\",\n" +
            "  \"examples\": [\n" +
            "    { \"en\": \"英文例句1\", \"zh\": \"中文翻译1\" },\n" +
            "    { \"en\": \"英文例句2\", \"zh\": \"中文翻译2\" },\n" +
            "    { \"en\": \"英文例句3\", \"zh\": \"中文翻译3\" }\n" +
            "  ]\n" +
            "}";

    private final HttpClient httpClient;

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    // ========== 段落翻译 ==========

    private static final String TRANSLATION_SYSTEM_PROMPT =
            "你是一个专业的中英翻译助手。请将用户提供的英文段落翻译成流畅、准确的中文。\n" +
            "\n" +
            "要求：\n" +
            "1. 翻译自然流畅，符合中文表达习惯\n" +
            "2. 保留原文的语气和风格\n" +
            "3. 直接返回中文译文，不要添加任何解释或说明\n" +
            "\n" +
            "请严格以如下 JSON 格式输出（不要输出其他内容）：\n" +
            "{\n" +
            "  \"translation\": \"中文译文\"\n" +
            "}";

    // ========== 文化背景分析 ==========

    private static final String CULTURE_SYSTEM_PROMPT =
            "你是一位文化教育助手。用户将提供一篇英文文章。请分析文章中任何值得向中国英语学习者解释"
            + "的背景知识，包括但不限于：\n"
            + "- 西方节日、习俗、礼仪\n"
            + "- 历史事件、人物\n"
            + "- 地理、地名、建筑\n"
            + "- 品牌、机构、组织\n"
            + "- 饮食、艺术、体育等文化现象\n"
            + "- 任何对非英语母语者可能不熟悉的内容\n\n"
            + "要求：\n"
            + "1. 只要有值得讲解的内容就必须列出，宁多勿漏\n"
            + "2. 标题用中文（10字以内）\n"
            + "3. 英文讲解约50词，精炼清晰\n"
            + "4. 同时提供中文翻译\n"
            + "5. 如果文章完全没有文化背景知识则返回空数组\n\n"
            + "请严格以如下 JSON 格式输出：\n"
            + "{\n"
            + "  \"notes\": [\n"
            + "    { \"title\": \"文化点标题\", \"content\": \"English explanation (~50 words)\", \"zh\": \"中文翻译\" }\n"
            + "  ]\n"
            + "}";

    /**
     * 分析文章中的文化背景知识。
     * @param articleContent 文章全文
     * @return CulturalNotesResult
     */
    public CulturalNotesResult analyzeCulture(String articleContent) {
        CulturalNotesResult result = new CulturalNotesResult();
        long startTime = System.currentTimeMillis();

        // 发送完整文章（最多 8000 字符）
        String content = articleContent.length() > 8000
                ? articleContent.substring(0, 8000)
                : articleContent;

        String responseContent = callDeepSeek(CULTURE_SYSTEM_PROMPT, content, 25, result);

        if (responseContent != null) {
            List<CulturalNote> notes = new ArrayList<>();
            int pos = 0;
            while ((pos = responseContent.indexOf("\"title\":\"", pos)) != -1) {
                pos += 9;
                String title = extractStringValue(responseContent, pos);
                pos = responseContent.indexOf("\"content\":\"", pos);
                if (pos == -1) break;
                pos += 11;
                String noteContent = extractStringValue(responseContent, pos);
                // 尝试提取 zh 字段
                String zh = "";
                int zhPos = responseContent.indexOf("\"zh\":\"", pos);
                if (zhPos != -1) {
                    zhPos += 6;
                    zh = extractStringValue(responseContent, zhPos);
                    if (zh == null) zh = "";
                }
                if (title != null && noteContent != null) {
                    notes.add(new CulturalNote(title, noteContent, zh));
                }
            }
            result.notes = notes.toArray(new CulturalNote[0]);
        }

        result.duration = System.currentTimeMillis() - startTime;
        return result;
    }

    // ========== 阅读理解出题 ==========

    private static final String QUIZ_SYSTEM_PROMPT =
            "You are an English reading comprehension teacher. Follow these steps:\n\n"
            + "Step 1: Read the article carefully and identify its MAIN IDEA "
            + "(the author's primary argument, thesis, or central message).\n\n"
            + "Step 2: Based on the main idea, create 2-3 reading comprehension questions. "
            + "Questions MUST be about:\n"
            + "  - What is the main idea / author's primary argument?\n"
            + "  - Which part of the text best supports or reveals the main idea?\n"
            + "  - What can be inferred from the author's tone or choice of evidence?\n\n"
            + "Rules:\n"
            + "1. All questions and options MUST be in ENGLISH\n"
            + "2. Each question has 4 options (A/B/C/D), only one correct\n"
            + "3. Answer: 0=A, 1=B, 2=C, 3=D\n"
            + "4. Brief explanation in English (60 words max)\n"
            + "5. Generate at least 2 questions, max 3\n\n"
            + "Output STRICT JSON (NO spaces after colons):\n"
            + "{\n"
            + "  \"mainIdea\":\"One sentence summary.\",\n"
            + "  \"questions\":[\n"
            + "    {\n"
            + "      \"question\":\"What is the main argument?\",\n"
            + "      \"optionA\":\"First option\",\n"
            + "      \"optionB\":\"Second option\",\n"
            + "      \"optionC\":\"Third option\",\n"
            + "      \"optionD\":\"Fourth option\",\n"
            + "      \"answer\":0,\n"
            + "      \"explanation\":\"The answer is A because...\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    /**
     * 基于文章生成阅读理解选择题。
     * @param articleContent 文章全文
     * @return QuizResult
     */
    public QuizResult generateQuiz(String articleContent) {
        QuizResult result = new QuizResult();
        long startTime = System.currentTimeMillis();

        // 发送完整文章（最多 8000 字符）
        String content = articleContent.length() > 8000
                ? articleContent.substring(0, 8000)
                : articleContent;

        String responseContent = callDeepSeek(QUIZ_SYSTEM_PROMPT, content, 25, result);

        if (responseContent != null) {
            // 提取 mainIdea
            int miPos = responseContent.indexOf("\"mainIdea\":\"");
            if (miPos != -1) {
                miPos += 12;
                result.mainIdea = extractStringValue(responseContent, miPos);
            }

            List<QuizQuestion> questions = new ArrayList<>();
            int pos = 0;
            while ((pos = responseContent.indexOf("\"question\":\"", pos)) != -1) {
                pos += 12;
                String question = extractStringValue(responseContent, pos);

                // 提取 optionA/B/C/D（独立字段，稳定可靠）
                String[] opts = new String[4];
                String[] optKeys = {"\"optionA\":\"", "\"optionB\":\"", "\"optionC\":\"", "\"optionD\":\""};
                for (int i = 0; i < 4; i++) {
                    int optPos = responseContent.indexOf(optKeys[i], pos);
                    if (optPos == -1) break;
                    optPos += optKeys[i].length();
                    opts[i] = extractStringValue(responseContent, optPos);
                }

                // 提取 answer
                pos = responseContent.indexOf("\"answer\":", pos);
                if (pos == -1) break;
                pos += 9;
                int answer = 0;
                while (pos < responseContent.length() && responseContent.charAt(pos) >= '0'
                        && responseContent.charAt(pos) <= '9') {
                    answer = answer * 10 + (responseContent.charAt(pos) - '0');
                    pos++;
                }

                // 提取 explanation
                pos = responseContent.indexOf("\"explanation\":\"", pos);
                if (pos == -1) break;
                pos += 15;
                String explanation = extractStringValue(responseContent, pos);

                if (question != null && opts[0] != null && opts[1] != null
                        && opts[2] != null && opts[3] != null) {
                    questions.add(new QuizQuestion(
                            question,
                            opts,
                            answer,
                            explanation != null ? explanation : ""
                    ));
                }
            }
            result.questions = questions.toArray(new QuizQuestion[0]);
            System.out.println("[AIService] 出题结果: 提取到 " + result.questions.length
                    + " 道题, mainIdea=" + (result.mainIdea != null ? "有" : "无"));

            if (result.questions.length == 0) {
                result.error = "AI 返回内容解析失败，content长度=" + responseContent.length()
                        + "，前200字符: "
                        + (responseContent.length() > 200
                            ? responseContent.substring(0, 200) + "..."
                            : responseContent);
            }
        }

        result.duration = System.currentTimeMillis() - startTime;
        return result;
    }

    // ========== 通用 API 调用 ==========

    /**
     * 调用 DeepSeek API，返回提取的 content 字符串。
     * 出错时设置 result.error 并返回 null。
     */
    private String callDeepSeek(String systemPrompt, String userContent,
                                int timeoutSeconds, AIResultBase result) {
        try {
            String requestBody = buildRequestBody(systemPrompt, userContent);

            System.out.println("[AIService] 请求 URL: " + API_URL);
            System.out.println("[AIService] 请求体(前300字符): "
                    + (requestBody.length() > 300
                        ? requestBody.substring(0, 300) + "..."
                        : requestBody));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            result.httpStatus = response.statusCode();
            System.out.println("[AIService] 响应状态: " + response.statusCode());

            if (response.statusCode() != 200) {
                result.rawResponse = response.body();
                result.error = "AI 服务返回错误状态码: " + response.statusCode();
                return null;
            }

            String body = response.body();
            result.rawResponse = body;
            System.out.println("[AIService] 响应体(前500字符): "
                    + (body.length() > 500 ? body.substring(0, 500) + "..." : body));

            String content = extractContent(body);
            if (content == null || content.isEmpty()) {
                result.error = "AI 服务返回内容为空";
                return null;
            }

            // 去除可能的 markdown 代码块包裹（```json ... ```）
            content = stripMarkdownFence(content);

            // 规范化 JSON：去除字符串外的空格（AI 可能输出 "key": "val" 而非 "key":"val"）
            content = normalizeJson(content);

            System.out.println("[AIService] 提取的 content(前300字符): "
                    + (content.length() > 300 ? content.substring(0, 300) + "..." : content));

            return content;

        } catch (Exception e) {
            result.error = "AI 调用失败: " + e.getMessage();
            System.out.println("[AIService] 异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ========== 单词例句生成 ==========

    /**
     * 为指定单词生成 3 个例句。
     * @param word 英文单词
     * @return AIExampleResult，包含例句列表或错误信息
     */
    public AIExampleResult generateExamples(String word) {
        AIExampleResult result = new AIExampleResult();
        result.word = word;
        long startTime = System.currentTimeMillis();

        String content = callDeepSeek(SYSTEM_PROMPT, word, TIMEOUT_SECONDS, result);

        if (content != null) {
            parseExamples(content, result);

            if (result.examples == null || result.examples.length == 0) {
                result.error = "AI 例句解析失败：未能从响应中提取例句，耗时: "
                        + (System.currentTimeMillis() - startTime)
                        + "ms，AI 返回内容: "
                        + (content.length() > 300 ? content.substring(0, 300) + "..." : content);
                result.rawResponse = "=== AI 返回的 content ===\n" + content
                        + "\n\n=== API 原始响应 ===\n" + result.rawResponse;
            }
        }

        result.duration = System.currentTimeMillis() - startTime;
        return result;
    }

    /**
     * 翻译英文段落为中文。
     * @param paragraph 英文段落文本
     * @return TranslationResult，包含中文译文或错误信息
     */
    public TranslationResult translateParagraph(String paragraph) {
        TranslationResult result = new TranslationResult();
        result.originalText = paragraph;
        long startTime = System.currentTimeMillis();

        String content = callDeepSeek(TRANSLATION_SYSTEM_PROMPT, paragraph, 20, result);

        if (content != null) {
            // 提取 translation 字段
            for (String key : new String[]{"\"translation\":\"", "\"translation\": \"", "\"translation\" :\""}) {
                int start = content.indexOf(key);
                if (start != -1) {
                    start += key.length();
                    String translation = extractStringValue(content, start);
                    if (translation != null && !translation.isEmpty()) {
                        result.translation = translation;
                        break;
                    }
                }
            }
            if (result.translation == null) {
                result.error = "翻译结果解析失败";
            }
        }

        result.duration = System.currentTimeMillis() - startTime;
        return result;
    }

    /**
     * 构建发给 DeepSeek 的请求体 JSON（通用版本）
     */
    private String buildRequestBody(String systemPrompt, String userContent) {
        String safeSystem = escapeJson(systemPrompt);
        String safeUser = escapeJson(userContent);

        return "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + safeSystem + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + safeUser + "\"}"
                + "],"
                + "\"response_format\":{\"type\":\"json_object\"}"
                + "}";
    }

    /**
     * 规范化 JSON：去除字符串外的空格/换行/制表符，
     * 使 "key": "val" 变为 "key":"val"，确保统一的紧凑格式便于解析。
     */
    private String normalizeJson(String json) {
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
                sb.append(c);
            } else if (!inString && (c == ' ' || c == '\n' || c == '\r' || c == '\t')) {
                // 跳过字符串外的空白
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 去除 AI 输出中可能包裹的 markdown 代码块标记
     */
    private String stripMarkdownFence(String content) {
        String trimmed = content.trim();
        // 去掉开头的 ```json 或 ```
        if (trimmed.startsWith("```")) {
            int endOfFirstLine = trimmed.indexOf('\n');
            if (endOfFirstLine != -1) {
                trimmed = trimmed.substring(endOfFirstLine + 1);
            }
        }
        // 去掉结尾的 ```
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }
        return trimmed;
    }

    /**
     * 从 DeepSeek 响应中提取 choices[0].message.content
     */
    private String extractContent(String responseBody) {
        // 尝试多种可能的 key 格式（有无空格）
        for (String key : new String[]{"\"content\":\"", "\"content\": \"", "\"content\" :\""}) {
            int start = responseBody.indexOf(key);
            if (start != -1) {
                start += key.length();

                // 从 content 开始位置找到未转义的闭合引号
                StringBuilder content = new StringBuilder();
                for (int i = start; i < responseBody.length(); i++) {
                    char c = responseBody.charAt(i);
                    if (c == '\\' && i + 1 < responseBody.length()) {
                        char next = responseBody.charAt(i + 1);
                        content.append(c);
                        content.append(next);
                        i++;
                    } else if (c == '"') {
                        break;
                    } else {
                        content.append(c);
                    }
                }

                // 将 JSON 字符串中的转义序列还原
                return unescapeJson(content.toString());
            }
        }
        return null;
    }

    /**
     * 解析 AI 返回的 content JSON，提取 examples
     */
    private void parseExamples(String contentJson, AIExampleResult result) {
        List<String[]> examples = new ArrayList<>();

        // 尝试多种 key 格式（有无空格）
        String[] enKeys = {"\"en\":\"", "\"en\": \"", "\"en\" :\""};
        String[] zhKeys = {"\"zh\":\"", "\"zh\": \"", "\"zh\" :\""};

        int pos = 0;
        while (pos < contentJson.length() && examples.size() < 3) {
            // 找 "en" 键
            int enStart = -1;
            int enKeyLen = 0;
            for (String k : enKeys) {
                int idx = contentJson.indexOf(k, pos);
                if (idx != -1 && (enStart == -1 || idx < enStart)) {
                    enStart = idx;
                    enKeyLen = k.length();
                }
            }
            if (enStart == -1) break;

            pos = enStart + enKeyLen;
            String en = extractStringValue(contentJson, pos);

            // 找紧随的 "zh" 键
            int zhStart = -1;
            int zhKeyLen = 0;
            for (String k : zhKeys) {
                int idx = contentJson.indexOf(k, pos);
                if (idx != -1 && (zhStart == -1 || idx < zhStart)) {
                    zhStart = idx;
                    zhKeyLen = k.length();
                }
            }
            if (zhStart == -1) break;

            pos = zhStart + zhKeyLen;
            String zh = extractStringValue(contentJson, pos);

            if (en != null && zh != null) {
                examples.add(new String[]{en, zh});
            }
        }

        result.examples = examples.toArray(new String[0][]);
    }

    /**
     * 从指定位置提取 "..." 中的字符串值，返回位置更新到闭合引号之后
     */
    private String extractStringValue(String json, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default -> { sb.append('\\'); sb.append(next); }
                }
                i++;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 将 JSON 转义字符串还原（处理 \\\", \\n, \\\\ 等）
     */
    private String unescapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"' -> { sb.append('"'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case 'n' -> { sb.append('\n'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case '/' -> { sb.append('/'); i++; }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * JSON 字符串转义
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * AI 结果基类，包含所有结果共有的字段
     */
    public static class AIResultBase {
        public String error;
        public int httpStatus;
        public long duration;
        public String rawResponse;
    }

    /**
     * AI 例句生成结果
     */
    public static class AIExampleResult extends AIResultBase {
        public String word;
        public String[][] examples; // 每个元素 [en, zh]

        public boolean isSuccess() {
            return error == null && examples != null && examples.length > 0;
        }

        /**
         * 序列化为前端可用的 JSON
         */
        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"success\":").append(isSuccess());
            sb.append(",\"word\":\"").append(escapeJsonStatic(word)).append("\"");
            sb.append(",\"httpStatus\":").append(httpStatus);
            sb.append(",\"duration\":").append(duration);

            if (error != null) {
                sb.append(",\"message\":\"").append(escapeJsonStatic(error)).append("\"");
            }

            sb.append(",\"examples\":[");
            if (examples != null) {
                for (int i = 0; i < examples.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append("{");
                    sb.append("\"en\":\"").append(escapeJsonStatic(examples[i][0])).append("\",");
                    sb.append("\"zh\":\"").append(escapeJsonStatic(examples[i][1])).append("\"");
                    sb.append("}");
                }
            }
            sb.append("]");

            // 返回原始/调试信息（前端诊断用，截断防过大）
            if (rawResponse != null && !rawResponse.isEmpty()) {
                String truncated = rawResponse.length() > 3000
                        ? rawResponse.substring(0, 3000) + "...[截断]"
                        : rawResponse;
                sb.append(",\"rawResponse\":\"")
                        .append(escapeJsonStatic(truncated))
                        .append("\"");
            }

            sb.append("}");
            return sb.toString();
        }

        private static String escapeJsonStatic(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder(s.length());
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\b' -> sb.append("\\b");
                    case '\f' -> sb.append("\\f");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    // ========== 文化背景分析结果 ==========

    public static class CulturalNote {
        public final String title;
        public final String content;
        public final String zh;

        public CulturalNote(String title, String content, String zh) {
            this.title = title;
            this.content = content;
            this.zh = zh;
        }
    }

    public static class CulturalNotesResult extends AIResultBase {
        public CulturalNote[] notes;

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"success\":").append(error == null);
            sb.append(",\"httpStatus\":").append(httpStatus);
            sb.append(",\"duration\":").append(duration);

            if (error != null) {
                sb.append(",\"message\":\"").append(escapeJsonStatic(error)).append("\"");
            }

            sb.append(",\"notes\":[");
            if (notes != null) {
                for (int i = 0; i < notes.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append("{");
                    sb.append("\"title\":\"").append(escapeJsonStatic(notes[i].title)).append("\",");
                    sb.append("\"content\":\"").append(escapeJsonStatic(notes[i].content)).append("\",");
                    sb.append("\"zh\":\"").append(escapeJsonStatic(notes[i].zh)).append("\"");
                    sb.append("}");
                }
            }
            sb.append("]");

            if (rawResponse != null && !rawResponse.isEmpty()) {
                String truncated = rawResponse.length() > 3000
                        ? rawResponse.substring(0, 3000) + "..."
                        : rawResponse;
                sb.append(",\"rawResponse\":\"")
                        .append(escapeJsonStatic(truncated))
                        .append("\"");
            }

            sb.append("}");
            return sb.toString();
        }
    }

    // ========== 阅读理解出题结果 ==========

    public static class QuizQuestion {
        public final String question;
        public final String[] options;
        public final int answer;
        public final String explanation;

        public QuizQuestion(String question, String[] options, int answer, String explanation) {
            this.question = question;
            this.options = options;
            this.answer = answer;
            this.explanation = explanation;
        }
    }

    public static class QuizResult extends AIResultBase {
        public String mainIdea;
        public QuizQuestion[] questions;

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"success\":").append(error == null);
            sb.append(",\"httpStatus\":").append(httpStatus);
            sb.append(",\"duration\":").append(duration);

            if (error != null) {
                sb.append(",\"message\":\"").append(escapeJsonStatic(error)).append("\"");
            }

            sb.append(",\"mainIdea\":\"")
                    .append(escapeJsonStatic(mainIdea != null ? mainIdea : ""))
                    .append("\"");
            sb.append(",\"questions\":[");
            if (questions != null) {
                for (int i = 0; i < questions.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append("{");
                    sb.append("\"question\":\"")
                            .append(escapeJsonStatic(questions[i].question)).append("\",");
                    sb.append("\"options\":[");
                    for (int j = 0; j < questions[i].options.length; j++) {
                        if (j > 0) sb.append(",");
                        sb.append("\"")
                                .append(escapeJsonStatic(questions[i].options[j]))
                                .append("\"");
                    }
                    sb.append("],");
                    sb.append("\"answer\":").append(questions[i].answer).append(",");
                    sb.append("\"explanation\":\"")
                            .append(escapeJsonStatic(questions[i].explanation)).append("\"");
                    sb.append("}");
                }
            }
            sb.append("]");

            if (rawResponse != null && !rawResponse.isEmpty()) {
                String truncated = rawResponse.length() > 3000
                        ? rawResponse.substring(0, 3000) + "..."
                        : rawResponse;
                sb.append(",\"rawResponse\":\"")
                        .append(escapeJsonStatic(truncated))
                        .append("\"");
            }

            sb.append("}");
            return sb.toString();
        }
    }

    // ========== 段落翻译结果 ==========

    public static class TranslationResult extends AIResultBase {
        public String originalText;
        public String translation;

        public boolean isSuccess() {
            return error == null && translation != null && !translation.isEmpty();
        }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"success\":").append(isSuccess());
            sb.append(",\"httpStatus\":").append(httpStatus);
            sb.append(",\"duration\":").append(duration);

            if (error != null) {
                sb.append(",\"message\":\"").append(escapeJsonStatic(error)).append("\"");
            }
            if (translation != null) {
                sb.append(",\"translation\":\"").append(escapeJsonStatic(translation)).append("\"");
            }

            sb.append("}");
            return sb.toString();
        }
    }

    private static String escapeJsonStatic(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
