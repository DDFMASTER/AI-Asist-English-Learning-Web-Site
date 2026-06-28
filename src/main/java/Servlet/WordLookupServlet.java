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

        if (word == null || word.isBlank()) {
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"单词参数不能为空\"}");
            return;
        }

        WordService.WordLookupResult result =
                wordService.lookupWordInAllStages(word.trim());
        response.getWriter().write(result.toJson());
    }
}
