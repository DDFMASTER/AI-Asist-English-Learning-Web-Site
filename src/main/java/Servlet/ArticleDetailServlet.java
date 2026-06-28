package Servlet;

import Utils.DBUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 文章详情接口
 *
 * GET /api/article/detail?id=<articleId>
 *
 * 返回文章完整信息，包含 content 正文
 */
@WebServlet("/api/article/detail")
public class ArticleDetailServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"缺少文章ID\"}");
            return;
        }

        long articleId;
        try {
            articleId = Long.parseLong(idParam);
        } catch (NumberFormatException e) {
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"文章ID格式错误\"}");
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT article_id, title, content, source, difficulty, " +
                         "article_like_count, explanation, explanation_like_count, " +
                         "explanation_dislike_count, vocquiz_num, comquiz_num " +
                         "FROM article WHERE article_id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, articleId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String title = rs.getString("title");
                        String content = rs.getString("content");
                        String source = rs.getString("source");
                        String difficulty = rs.getString("difficulty");
                        int contentLen = content != null ? content.length() : 0;
                        int wordCount = Math.max(1, contentLen / 5);
                        int readTime = Math.max(1, wordCount / 200);

                        StringBuilder json = new StringBuilder();
                        json.append("{");
                        json.append("\"success\":true,");
                        json.append("\"article\":{");
                        json.append("\"articleId\":").append(rs.getLong("article_id")).append(",");
                        json.append("\"title\":").append(jsonStr(title)).append(",");
                        json.append("\"content\":").append(jsonStr(content)).append(",");
                        json.append("\"source\":").append(jsonStr(source)).append(",");
                        json.append("\"difficulty\":").append(jsonStr(difficulty)).append(",");
                        json.append("\"wordCount\":").append(wordCount).append(",");
                        json.append("\"readTime\":").append(readTime).append(",");
                        json.append("\"articleLikeCount\":").append(rs.getInt("article_like_count")).append(",");
                        json.append("\"explanation\":").append(jsonStr(rs.getString("explanation"))).append(",");
                        json.append("\"explanationLikeCount\":").append(rs.getInt("explanation_like_count")).append(",");
                        json.append("\"explanationDislikeCount\":").append(rs.getInt("explanation_dislike_count")).append(",");
                        json.append("\"vocquizNum\":").append(rs.getInt("vocquiz_num")).append(",");
                        json.append("\"comquizNum\":").append(rs.getInt("comquiz_num"));
                        json.append("}}");

                        response.getWriter().write(json.toString());
                    } else {
                        response.getWriter().write(
                                "{\"success\":false,\"message\":\"文章不存在\"}");
                    }
                }
            }
        } catch (Exception e) {
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"查询文章详情失败: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private String jsonStr(String s) {
        if (s == null) return "null";
        return "\"" + escapeJson(s) + "\"";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
