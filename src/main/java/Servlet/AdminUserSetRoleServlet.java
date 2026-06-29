package Servlet;

import Service.AdminService;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 管理员 — 设置用户角色（normal / vip）
 *
 * POST /api/admin/user/set-role
 * Body: adminUserId, userId, role
 */
@WebServlet("/api/admin/user/set-role")
public class AdminUserSetRoleServlet extends HttpServlet {
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

        Long targetUserId = parseLong(request.getParameter("userId"));
        String role = request.getParameter("role");

        String err = adminService.setRole(targetUserId, role);
        if (err != null) {
            response.getWriter().write(JsonUtil.error(err));
            return;
        }

        // 记录日志
        adminService.logAction(adminUserId, "user", targetUserId,
                "set_role", "role=" + role);

        response.getWriter().write(JsonUtil.success("角色设置成功"));
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
