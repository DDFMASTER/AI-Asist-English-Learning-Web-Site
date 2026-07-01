package Servlet;

import DAO.UserDAOImpl;
import Service.VocabTestService;
import Service.VocabTestService.EstimateResult;
import Service.VocabTestService.TestWord;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * 词汇量测试 — 提交结果
 * POST /api/vocabtest/submit
 * 参数: userId (Long), answers (逗号分隔的 "1"/"0")
 * 词表从 session 中获取。
 */
@WebServlet("/api/vocabtest/submit")
public class VocabTestSubmitServlet extends HttpServlet {

    private static final String AI_API_URL = "https://api.deepseek.com/chat/completions";
    private static final String AI_API_KEY = "sk-a74d91663df347dd92087fdac3d8aa9e";
    private static final String AI_MODEL = "deepseek-v4-flash";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        Long userId = parseLong(request.getParameter("userId"));
        String answersStr = request.getParameter("answers");
        if (userId == null || answersStr == null) {
            response.getWriter().write(JsonUtil.error("缺少参数"));
            return;
        }

        // 从 session 获取测试词表
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("vocabTestWords") == null) {
            response.getWriter().write(JsonUtil.error("测试会话已过期，请刷新重试"));
            return;
        }
        @SuppressWarnings("unchecked")
        List<TestWord> words = (List<TestWord>) session.getAttribute("vocabTestWords");

        // 解析回答
        String[] parts = answersStr.split(",");
        java.util.List<String> answers = java.util.List.of(parts);

        // 使用词频段公式精确估算
        VocabTestService service = VocabTestService.getInstance();
        EstimateResult result = service.estimate(answers, words);

        // 调用 AI 分析 CEFR 等级
        String cefrLevel = analyzeCEFR(result.estimatedVocabulary);

        // 更新 literacy
        new UserDAOImpl().updateLiteracy(userId, result.estimatedVocabulary);
        // 清除 session 中的词表
        session.removeAttribute("vocabTestWords");

        // 构建响应
        String extra = "\"estimatedVocab\":" + JsonUtil.numVal(result.estimatedVocabulary)
                + ",\"lowerCI\":" + JsonUtil.numVal((int) result.lowerCI)
                + ",\"upperCI\":" + JsonUtil.numVal((int) result.upperCI)
                + ",\"cefrLevel\":" + JsonUtil.strVal(cefrLevel)
                + ",\"cefrLabel\":" + JsonUtil.strVal(getCEFRLabel(cefrLevel))
                + ",\"fakeHitRate\":" + JsonUtil.numVal((int) Math.round(result.fakeHitRate * 100));

        response.getWriter().write(JsonUtil.buildResponse(true, "测试完成", extra));
    }

    private String analyzeCEFR(int estimatedVocab) {
        try {
            String prompt = String.format(
                "用户完成了一项英语词汇量测试，估算词汇量约为 %d 词。" +
                "请根据词汇量数据，判断用户的 CEFR 英语等级（A1/A2/B1/B2/C1/C2）。" +
                "参考标准：A1(0-1500),A2(1500-3000),B1(3000-5000),B2(5000-8000),C1(8000-12000),C2(12000+)。" +
                "请严格以 JSON 格式输出：{\"level\":\"B1\",\"label\":\"中级\"}",
                estimatedVocab
            );

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15)).build();

            String body = String.format(
                "{\"model\":\"%s\",\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"你是英语能力评估专家，只输出JSON。\"}," +
                "{\"role\":\"user\",\"content\":%s}]," +
                "\"temperature\":0.3,\"max_tokens\":100}",
                AI_MODEL, JsonUtil.strVal(prompt)
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(AI_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + AI_API_KEY)
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String respBody = resp.body();

            int levelIdx = respBody.indexOf("\"level\"");
            if (levelIdx > 0) {
                int colonIdx = respBody.indexOf(":", levelIdx);
                int startQuote = respBody.indexOf("\"", colonIdx + 1);
                int endQuote = respBody.indexOf("\"", startQuote + 1);
                if (startQuote > 0 && endQuote > startQuote) {
                    return respBody.substring(startQuote + 1, endQuote);
                }
            }
        } catch (Exception e) {
            System.err.println("[VocabTest] AI 分析失败: " + e.getMessage());
        }
        return fallbackCEFR(estimatedVocab);
    }

    private String fallbackCEFR(int vocab) {
        if (vocab < 1500) return "A1";
        if (vocab < 3000) return "A2";
        if (vocab < 5000) return "B1";
        if (vocab < 8000) return "B2";
        if (vocab < 12000) return "C1";
        return "C2";
    }

    private String getCEFRLabel(String level) {
        switch (level) {
            case "A1": return "初级";
            case "A2": return "初级上";
            case "B1": return "中级";
            case "B2": return "中高级";
            case "C1": return "高级";
            case "C2": return "精通";
            default: return "中级";
        }
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }
}
