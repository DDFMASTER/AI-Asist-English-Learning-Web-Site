package Servlet;

import Service.AdminService;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 管理员 — 创建用户（默认密码 123456）
 *
 * POST /api/admin/user/create
 * Body: adminUserId, username, studyPurpose
 */
@WebServlet("/api/admin/user/create")
public class AdminUserCreateServlet extends HttpServlet {
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

        String username = request.getParameter("username");
        String studyPurpose = request.getParameter("studyPurpose");

        String err = adminService.createUser(username, studyPurpose);
        if (err != null) {
            response.getWriter().write(JsonUtil.error(err));
            return;
        }

        // 记录日志
        adminService.logAction(adminUserId, "user", 0L,
                "create", "username=" + username);

        response.getWriter().write(JsonUtil.success("用户创建成功"));
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
