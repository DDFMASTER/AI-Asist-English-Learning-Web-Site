package Servlet;

import Service.VocabTestCardService;
import Service.VocabTestCardService.CardWord;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 词汇量卡片测试 — 获取测试词表
 * GET /api/vocabtest/cards
 * 返回 100 个测试词（90 真词 + 10 伪词），词表存入 session。
 */
@WebServlet("/api/vocabtest/cards")
public class VocabTestCardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        VocabTestCardService service = new VocabTestCardService();
        List<CardWord> words = service.sampleWords();

        // 存入 session
        HttpSession session = request.getSession(true);
        session.setAttribute("vocabTestCardWords", new ArrayList<>(words));

        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,\"total\":").append(words.size());
        json.append(",\"words\":[");
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) json.append(",");
            CardWord cw = words.get(i);
            json.append("{");
            json.append("\"vocabId\":").append(cw.vocabId).append(",");
            json.append("\"word\":").append(JsonUtil.strVal(cw.word)).append(",");
            json.append("\"isPseudo\":").append(cw.isPseudo);
            json.append("}");
        }
        json.append("]}");
        response.getWriter().write(json.toString());
    }
}
