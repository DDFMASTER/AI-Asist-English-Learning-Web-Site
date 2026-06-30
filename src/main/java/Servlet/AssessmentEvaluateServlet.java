package Servlet;

import Service.AIService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * AI 测评评估接口。
 * POST /api/assessment/evaluate
 * Body: { "questions": [...], "answers": {...} }
 * 返回 AI 评估的分项得分、总分和 CEFR 等级。
 */
@WebServlet("/api/assessment/evaluate")
public class AssessmentEvaluateServlet extends HttpServlet {

    private final AIService aiService = new AIService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String body = readBody(request);
        String questions = extractRawValue(body, "questions");
        String answers = extractRawValue(body, "answers");

        if (questions == null || answers == null) {
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"缺少必要参数\"}");
            return;
        }

        AIService.AssessmentEvaluateResult result =
                aiService.evaluateAssessment(questions, answers);
        response.getWriter().write(result.toJson());
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * 从 JSON body 中提取字段的原始 JSON 值（支持嵌套对象和数组）。
     * 通过括号匹配来正确处理嵌套结构。
     */
    private String extractRawValue(String body, String field) {
        String searchKey = "\"" + field + "\":";
        int start = body.indexOf(searchKey);
        if (start == -1) {
            // try with spaces
            searchKey = "\"" + field + "\" :";
            start = body.indexOf(searchKey);
            if (start == -1) return null;
        }
        start += searchKey.length();

        // 跳过空白
        while (start < body.length() && Character.isWhitespace(body.charAt(start))) {
            start++;
        }

        if (start >= body.length()) return null;

        char first = body.charAt(start);
        if (first == '{' || first == '[') {
            // 提取平衡的括号内容
            char close = (first == '{') ? '}' : ']';
            int depth = 1;
            StringBuilder val = new StringBuilder();
            val.append(first);
            boolean inString = false;
            for (int i = start + 1; i < body.length() && depth > 0; i++) {
                char c = body.charAt(i);
                val.append(c);

                if (inString) {
                    if (c == '\\' && i + 1 < body.length()) {
                        val.append(body.charAt(i + 1));
                        i++;
                    } else if (c == '"') {
                        inString = false;
                    }
                } else {
                    if (c == '"') {
                        inString = true;
                    } else if (c == close) {
                        depth--;
                    } else if (c == first) {
                        depth++;
                    }
                }
            }
            return val.toString();
        }

        // 否则作为简单值提取
        StringBuilder val = new StringBuilder();
        for (int i = start; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"') {
                // 字符串值
                i++;
                while (i < body.length()) {
                    c = body.charAt(i);
                    if (c == '\\' && i + 1 < body.length()) {
                        val.append(body.charAt(i + 1));
                        i++;
                    } else if (c == '"') {
                        break;
                    } else {
                        val.append(c);
                    }
                    i++;
                }
                return val.toString();
            }
            if (c == ',' || c == '}' || c == ']') break;
            val.append(c);
        }
        return val.toString().trim();
    }
}
