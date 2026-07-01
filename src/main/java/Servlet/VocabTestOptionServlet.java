package Servlet;

import Service.VocabTestCardService;
import Service.VocabTestCardService.CardWord;
import Service.VocabTestCardService.WordOptions;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 词汇量卡片测试 — AI 生成选项（支持批量预生成）
 *
 * POST /api/vocabtest/options
 * Body: { "words": [{"vocabId":1,"word":"apple","isPseudo":false}, ...] }
 *
 * 响应: { "success":true, "results":[{vocabId, word, options:["释义1",...], correctIndex, isPseudo}] }
 *
 * 选项已缓存于 VocabTestCardService 中，同词不会重复调用 AI。
 */
@WebServlet("/api/vocabtest/options")
public class VocabTestOptionServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        // 读取请求体
        StringBuilder bodyBuilder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                bodyBuilder.append(line);
            }
        }
        String body = bodyBuilder.toString();

        // 解析 words 数组
        List<CardWord> batch = parseWords(body);
        if (batch.isEmpty()) {
            response.getWriter().write(JsonUtil.error("请求中无有效单词"));
            return;
        }

        VocabTestCardService service = new VocabTestCardService();
        List<WordOptions> results = service.generateOptions(batch);

        // 构建响应 JSON
        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,\"results\":[");
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) json.append(",");
            WordOptions wo = results.get(i);
            json.append("{");
            json.append("\"vocabId\":").append(wo.vocabId).append(",");
            json.append("\"word\":").append(JsonUtil.strVal(wo.word)).append(",");
            json.append("\"isPseudo\":").append(wo.isPseudo).append(",");
            json.append("\"options\":[");
            for (int j = 0; j < wo.options.size(); j++) {
                if (j > 0) json.append(",");
                json.append(JsonUtil.strVal(wo.options.get(j)));
            }
            json.append("],");
            json.append("\"correctIndex\":").append(wo.correctIndex);
            json.append("}");
        }
        json.append("]}");
        response.getWriter().write(json.toString());
    }

    /** 从 JSON body 解析 words 数组 */
    private List<CardWord> parseWords(String body) {
        List<CardWord> result = new ArrayList<>();
        int arrIdx = body.indexOf("\"words\"");
        if (arrIdx < 0) return result;
        int bracketIdx = body.indexOf("[", arrIdx);
        if (bracketIdx < 0) return result;

        int pos = bracketIdx + 1;
        while (pos < body.length()) {
            int objStart = body.indexOf("{", pos);
            if (objStart < 0 || objStart >= body.indexOf("]", pos)) break;

            int objEnd = body.indexOf("}", objStart);
            if (objEnd < 0) break;
            String obj = body.substring(objStart + 1, objEnd);

            int vocabId = extractInt(obj, "vocabId");
            String word = extractString(obj, "word");
            boolean isPseudo = extractBoolean(obj, "isPseudo");

            if (vocabId > 0 && word != null) {
                result.add(new CardWord(vocabId, word, isPseudo));
            }

            pos = objEnd + 1;
        }
        return result;
    }

    private int extractInt(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return 0;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return 0;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && json.charAt(end) >= '0' && json.charAt(end) <= '9') end++;
        try { return Integer.parseInt(json.substring(start, end)); } catch (NumberFormatException e) { return 0; }
    }

    private String extractString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return null;
        int startQuote = json.indexOf("\"", colon + 1);
        if (startQuote < 0) return null;
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote < 0) return null;
        return json.substring(startQuote + 1, endQuote);
    }

    private boolean extractBoolean(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return false;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return false;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        return json.substring(start).startsWith("true");
    }
}
