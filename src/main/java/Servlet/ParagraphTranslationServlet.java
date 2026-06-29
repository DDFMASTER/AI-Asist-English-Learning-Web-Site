package Servlet;

import Service.AIService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * 段落翻译接口。
 * POST /api/article/translate-paragraph
 * Body: { "paragraph": "英文段落文本..." }
 * 返回 AI 翻译的中文译文。
 */
@WebServlet("/api/article/translate-paragraph")
public class ParagraphTranslationServlet extends HttpServlet {

    private final AIService aiService = new AIService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String paragraph = readParagraph(request);

        if (paragraph == null || paragraph.isBlank()) {
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"段落文本不能为空\"}");
            return;
        }

        AIService.TranslationResult result = aiService.translateParagraph(paragraph);
        response.getWriter().write(result.toJson());
    }

    /** 从 POST body 中提取 paragraph 字段的值 */
    private String readParagraph(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String body = sb.toString();
        // 简单的 JSON 解析：提取 "paragraph" 字段
        for (String key : new String[]{"\"paragraph\":\"", "\"paragraph\": \"", "\"paragraph\" :\""}) {
            int start = body.indexOf(key);
            if (start != -1) {
                start += key.length();
                StringBuilder val = new StringBuilder();
                for (int i = start; i < body.length(); i++) {
                    char c = body.charAt(i);
                    if (c == '\\' && i + 1 < body.length()) {
                        char next = body.charAt(i + 1);
                        val.append(c);
                        val.append(next);
                        i++;
                    } else if (c == '"') {
                        break;
                    } else {
                        val.append(c);
                    }
                }
                return unescapeJson(val.toString());
            }
        }
        return body; // fallback: 整个 body 当作段落
    }

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
}
