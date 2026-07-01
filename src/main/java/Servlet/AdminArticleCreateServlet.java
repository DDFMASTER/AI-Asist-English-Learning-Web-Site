package Servlet;

import Service.AdminService;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 管理员 — 创建文章
 *
 * POST /api/admin/article/create
 * Body: adminUserId, title, content, source(选填), difficulty
 */
@WebServlet("/api/admin/article/create")
@jakarta.servlet.annotation.MultipartConfig(
    maxFileSize = 5 * 1024 * 1024,      // 单文件最大 5MB
    maxRequestSize = 10 * 1024 * 1024    // 整个请求最大 10MB
)
public class AdminArticleCreateServlet extends HttpServlet {
    private final AdminService adminService = new AdminService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        try {
            Long adminUserId = parseLong(request.getParameter("adminUserId"));
            if (!adminService.isAdmin(adminUserId)) {
                response.getWriter().write(JsonUtil.error("无管理员权限"));
                return;
            }

            String title = request.getParameter("title");
            String content = request.getParameter("content");
            String source = request.getParameter("source");
            String difficulty = request.getParameter("difficulty");

            // 诊断日志
            String ct = request.getContentType();
            System.out.println("[ArticleCreate] contentType=" + ct
                + " titleLen=" + (title != null ? title.length() : 0)
                + " contentLen=" + (content != null ? content.length() : 0)
                + " source=" + source
                + " difficulty=" + difficulty);

            String err = adminService.createArticle(title, content, source, difficulty);
            if (err != null) {
                response.getWriter().write(JsonUtil.error(err));
                return;
            }

            // 记录日志
            adminService.logAction(adminUserId, "article", 0L,
                    "create", "title=" + title);

            response.getWriter().write(JsonUtil.success("文章创建成功"));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write(JsonUtil.error("服务器错误: " + e.getMessage()));
        }
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
