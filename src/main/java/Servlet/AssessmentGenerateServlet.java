package Servlet;

import Service.AIService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 测评出题接口（渐进式生成）。
 * POST /api/assessment/generate
 * Body: { "studyPurpose": "四级" }
 * 立即返回第一道题 + sessionId，剩余题目后台异步生成。
 */
@WebServlet("/api/assessment/generate")
public class AssessmentGenerateServlet extends HttpServlet {

    private final AIService aiService = new AIService();

    /** 会话 → 已生成的题目列表（线程安全） */
    public static final ConcurrentHashMap<String, List<AIService.AssessmentQuestion>> SESSIONS
            = new ConcurrentHashMap<>();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String body = readBody(request);
        String studyPurpose = extractField(body, "studyPurpose");
        if (studyPurpose == null || studyPurpose.isBlank()) {
            studyPurpose = "四级";
        }
        final String sp = studyPurpose.trim();
        final String sessionId = UUID.randomUUID().toString();

        // 1. 同步生成第一道题（约 15-20 秒）
        AIService.AssessmentQuestion first = aiService.generateSingleQuestion(sp);

        List<AIService.AssessmentQuestion> questions = new ArrayList<>();
        if (first != null) {
            questions.add(first);
        }
        SESSIONS.put(sessionId, questions);

        // 2. 后台线程逐道生成剩余 9 道题，失败自动重试
        final AIService svc = aiService;
        new Thread(() -> {
            try {
                List<AIService.AssessmentQuestion> list = SESSIONS.get(sessionId);
                int failures = 0;
                for (int i = 0; i < 9; i++) {
                    AIService.AssessmentQuestion q = null;
                    // 每题最多重试 3 次
                    for (int retry = 0; retry < 3; retry++) {
                        try {
                            q = svc.generateSingleQuestion(sp);
                            if (q != null) break;
                        } catch (Exception e) {
                            System.err.println("[Assessment] 第" + (i + 2) + "题生成失败(重试" + (retry + 1) + "/3): " + e.getMessage());
                        }
                        if (retry < 2) Thread.sleep(3000);
                    }
                    if (q != null && list != null) {
                        synchronized (list) {
                            list.add(q);
                        }
                    } else {
                        failures++;
                        System.err.println("[Assessment] 第" + (i + 2) + "题最终生成失败，跳过");
                    }
                    if (i < 8) Thread.sleep(2000);
                }
                System.out.println("[Assessment] 后台生成完成，成功" + (9 - failures) + "/9 题，sessionId=" + sessionId);
            } catch (InterruptedException e) {
                System.out.println("[Assessment] 后台生成被中断，sessionId=" + sessionId);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 10 分钟后清理会话（给用户充足的答题时间）
                try { Thread.sleep(600_000); } catch (InterruptedException ignored) {}
                SESSIONS.remove(sessionId);
                System.out.println("[Assessment] 会话清理完成，sessionId=" + sessionId);
            }
        }).start();

        // 3. 立即返回首题和 sessionId
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":").append(first != null);
        sb.append(",\"sessionId\":\"").append(sessionId).append("\"");
        sb.append(",\"totalTarget\":10");
        sb.append(",\"questions\":[");
        if (first != null) {
            appendQuestionJson(sb, first);
        }
        sb.append("]");
        if (first == null) {
            sb.append(",\"message\":\"首题生成失败，请重试\"");
        }
        sb.append("}");
        response.getWriter().write(sb.toString());
    }

    /** 将单个 AssessmentQuestion 序列化为 JSON */
    static void appendQuestionJson(StringBuilder sb, AIService.AssessmentQuestion q) {
        sb.append("{");
        sb.append("\"passage\":\"").append(esc(q.passage)).append("\",");
        sb.append("\"question\":\"").append(esc(q.question)).append("\",");
        sb.append("\"optionA\":\"").append(esc(q.options[0])).append("\",");
        sb.append("\"optionB\":\"").append(esc(q.options[1])).append("\",");
        sb.append("\"optionC\":\"").append(esc(q.options[2])).append("\",");
        sb.append("\"optionD\":\"").append(esc(q.options[3])).append("\",");
        sb.append("\"answer\":").append(q.answer).append(",");
        sb.append("\"explanation\":\"").append(esc(q.explanation)).append("\"");
        sb.append("}");
    }

    static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private String extractField(String body, String field) {
        for (String key : new String[]{
                "\"" + field + "\":\"",
                "\"" + field + "\": \"",
                "\"" + field + "\" :\""}) {
            int start = body.indexOf(key);
            if (start != -1) {
                start += key.length();
                StringBuilder val = new StringBuilder();
                for (int i = start; i < body.length(); i++) {
                    char c = body.charAt(i);
                    if (c == '\\' && i + 1 < body.length()) {
                        val.append(body.charAt(i + 1));
                        i++;
                    } else if (c == '"') {
                        break;
                    } else {
                        val.append(c);
                    }
                }
                return val.toString();
            }
        }
        return null;
    }
}
