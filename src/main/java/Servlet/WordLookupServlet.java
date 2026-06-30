package Servlet;

import Service.WordService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/word/lookup")
public class WordLookupServlet extends HttpServlet {

    private final WordService wordService = new WordService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String word = request.getParameter("word");
        String studyPurpose = request.getParameter("studyPurpose");

        if (word == null || word.isBlank()) {
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"单词参数不能为空\"}");
            return;
        }

        WordService.WordLookupResult result;
        if (studyPurpose != null && !studyPurpose.isBlank()) {
            // 按用户学习阶段过滤，只展示当前词书的释义
            result = wordService.lookupWordByStage(word.trim(), studyPurpose.trim());
        } else {
            // 未指定学习阶段时回退到全词库查询（兼容旧版调用）
            result = wordService.lookupWordInAllStages(word.trim());
        }
        response.getWriter().write(result.toJson());
    }
}
