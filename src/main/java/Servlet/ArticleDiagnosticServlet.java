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
import java.util.*;

/**
 * 诊断工具：按难度统计数据库中的文章数量与前3篇标题
 * GET /api/article/diagnostic
 */
@WebServlet("/api/article/diagnostic")
public class ArticleDiagnosticServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        Map<String, List<String>> difficultyMap = new LinkedHashMap<>();
        int total = 0;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT difficulty, title FROM article ORDER BY article_id DESC");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String diff = rs.getString("difficulty");
                String title = rs.getString("title");
                difficultyMap.computeIfAbsent(diff, k -> new ArrayList<>()).add(title);
                total++;
            }
        } catch (Exception e) {
            response.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            return;
        }

        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,\"total\":").append(total).append(",\"byDifficulty\":{");
        boolean firstDiff = true;
        for (Map.Entry<String, List<String>> entry : difficultyMap.entrySet()) {
            if (!firstDiff) json.append(",");
            firstDiff = false;
            json.append("\"").append(entry.getKey()).append("\":{");
            json.append("\"count\":").append(entry.getValue().size()).append(",");
            json.append("\"samples\":[");
            for (int i = 0; i < Math.min(3, entry.getValue().size()); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(esc(entry.getValue().get(i))).append("\"");
            }
            json.append("]}");
        }
        json.append("}}");
        response.getWriter().write(json.toString());
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
