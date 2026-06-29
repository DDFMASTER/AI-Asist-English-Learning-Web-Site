package Servlet;

import Service.AdminService;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 管理员 — 删除文章
 *
 * POST /api/admin/article/delete
 * Body: adminUserId, articleId
 */
@WebServlet("/api/admin/article/delete")
public class AdminArticleDeleteServlet extends HttpServlet {
    private final AdminService adminService = new AdminService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        Long adminUserId = parseLong(request.getParameter("adminUserId"));
        if (!adminService.isAdmin(adminUserId)) {
            response.getWriter().write(JsonUtil.error("无管理员权限"));
            return;
        }

        Long articleId = parseLong(request.getParameter("articleId"));

        String err = adminService.deleteArticle(articleId);
        if (err != null) {
            response.getWriter().write(JsonUtil.error(err));
            return;
        }

        // 记录日志
        adminService.logAction(adminUserId, "article", articleId,
                "delete", null);

        response.getWriter().write(JsonUtil.success("文章已删除"));
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
