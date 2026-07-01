package Servlet;

import DAO.UserDAOImpl;
import Service.VocabTestCardService;
import Service.VocabTestCardService.CardTestResult;
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
 * 词汇量卡片测试 — 提交结果
 * POST /api/vocabtest/cardresult
 * Body: { "userId": 1, "answers": ["A","know","dontknow",...], "optionResults": [...] }
 *
 * 词表从 session 获取。计算得分后更新用户 literacy 并返回估算词汇量。
 */
@WebServlet("/api/vocabtest/cardresult")
public class VocabTestCardResultServlet extends HttpServlet {

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

        Long userId = extractLong(body, "userId");
        if (userId == null) {
            response.getWriter().write(JsonUtil.error("缺少 userId"));
            return;
        }

        // 从 session 获取测试词表
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("vocabTestCardWords") == null) {
            response.getWriter().write(JsonUtil.error("测试会话已过期，请重新开始"));
            return;
        }
        @SuppressWarnings("unchecked")
        List<CardWord> words = (List<CardWord>) session.getAttribute("vocabTestCardWords");

        // 解析 answers
        List<String> answers = parseAnswers(body);
        if (answers.isEmpty()) {
            response.getWriter().write(JsonUtil.error("缺少 answers"));
            return;
        }

        // 计算得分
        VocabTestCardService service = new VocabTestCardService();
        CardTestResult result = service.calculateResult(answers, words, new ArrayList<>());

        // 更新用户词汇量
        new UserDAOImpl().updateLiteracy(userId, result.estimatedVocab);
        session.removeAttribute("vocabTestCardWords");

        // 缓存到本地（前端使用）
        String extra = "\"estimatedVocab\":" + JsonUtil.numVal(result.estimatedVocab)
                + ",\"cefrLevel\":" + JsonUtil.strVal(result.cefrLevel)
                + ",\"cefrLabel\":" + JsonUtil.strVal(result.cefrLabel)
                + ",\"correct\":" + result.correct
                + ",\"total\":" + result.total
                + ",\"realCorrect\":" + result.realCorrect
                + ",\"realTotal\":" + result.realTotal
                + ",\"pseudoCorrect\":" + result.pseudoCorrect
                + ",\"pseudoTotal\":" + result.pseudoTotal
                + ",\"honestyPercent\":" + result.honestyPercent;

        response.getWriter().write(JsonUtil.buildResponse(true, "测试完成", extra));
    }

    private List<String> parseAnswers(String body) {
        List<String> result = new ArrayList<>();
        int arrIdx = body.indexOf("\"answers\"");
        if (arrIdx < 0) return result;
        int bracketIdx = body.indexOf("[", arrIdx);
        if (bracketIdx < 0) return result;
        int endIdx = body.indexOf("]", bracketIdx);
        if (endIdx < 0) return result;

        String arrStr = body.substring(bracketIdx + 1, endIdx);
        int pos = 0;
        while (pos < arrStr.length()) {
            int qStart = arrStr.indexOf("\"", pos);
            if (qStart < 0) break;
            int qEnd = arrStr.indexOf("\"", qStart + 1);
            if (qEnd < 0) break;
            result.add(arrStr.substring(qStart + 1, qEnd));
            pos = qEnd + 1;
        }
        return result;
    }

    private Long extractLong(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && json.charAt(end) >= '0' && json.charAt(end) <= '9') end++;
        try { return Long.parseLong(json.substring(start, end)); } catch (NumberFormatException e) { return null; }
    }
}
