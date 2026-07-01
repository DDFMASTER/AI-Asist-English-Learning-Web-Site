package Servlet;

import Service.VocabTestService;
import Service.VocabTestService.TestWord;
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
 * 词汇量测试 — 获取测试单词
 * GET /api/vocabtest/words
 * 返回 100 个测试词，并将词表存入 session 供提交时使用。
 */
@WebServlet("/api/vocabtest/words")
public class VocabTestWordsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        VocabTestService service = VocabTestService.getInstance();
        List<TestWord> words = service.sampleInitial();

        // 存入 session 以便提交时估算
        HttpSession session = request.getSession(true);
        session.setAttribute("vocabTestWords", new ArrayList<>(words));

        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,\"words\":[");
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) json.append(",");
            json.append("{\"word\":").append(JsonUtil.strVal(words.get(i).word))
                .append(",\"index\":").append(i).append("}");
        }
        json.append("]}");
        response.getWriter().write(json.toString());
    }
}
