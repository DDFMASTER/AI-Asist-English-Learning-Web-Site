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

@WebServlet("/api/article/list")
public class ArticleListServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String difficulty = request.getParameter("difficulty");

        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,\"articles\":[");

        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT article_id, title, source, difficulty, " +
                         "article_like_count, explanation_like_count, " +
                         "explanation_dislike_count, vocquiz_num, comquiz_num, " +
                         "CHAR_LENGTH(content) AS content_length " +
                         "FROM article";

            if (difficulty != null && !difficulty.isBlank()) {
                sql += " WHERE difficulty = ?";
            }
            sql += " ORDER BY article_id DESC LIMIT 20";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (difficulty != null && !difficulty.isBlank()) {
                    ps.setString(1, difficulty);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        first = false;

                        int contentLen = rs.getInt("content_length");
                        int wordCount = Math.max(1, contentLen / 5);       // 英文平均 5 字符/词
                        int readTime  = Math.max(1, wordCount / 200);      // 200 词/分钟

                        json.append("{");
                        json.append("\"articleId\":").append(rs.getLong("article_id")).append(",");
                        json.append("\"title\":\"").append(escapeJson(rs.getString("title"))).append("\",");
                        json.append("\"source\":\"").append(escapeJson(rs.getString("source"))).append("\",");
                        json.append("\"difficulty\":\"").append(escapeJson(rs.getString("difficulty"))).append("\",");
                        json.append("\"wordCount\":").append(wordCount).append(",");
                        json.append("\"readTime\":").append(readTime).append(",");
                        json.append("\"articleLikeCount\":").append(rs.getInt("article_like_count")).append(",");
                        json.append("\"explanationLikeCount\":").append(rs.getInt("explanation_like_count")).append(",");
                        json.append("\"explanationDislikeCount\":").append(rs.getInt("explanation_dislike_count")).append(",");
                        json.append("\"vocquizNum\":").append(rs.getInt("vocquiz_num")).append(",");
                        json.append("\"comquizNum\":").append(rs.getInt("comquiz_num"));
                        json.append("}");
                    }
                }
            }
        } catch (Exception e) {
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"查询文章列表失败: " + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        json.append("]}");
        response.getWriter().write(json.toString());
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
