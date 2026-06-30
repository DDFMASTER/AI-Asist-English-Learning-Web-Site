package Servlet;

import Service.AdminService;
import Service.MonitorService;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 管理员 — 强制用户下线
 *
 * POST /api/admin/monitor/kick-user
 *
 * 参数（form-urlencoded）：
 *   adminUserId   - 管理员用户 ID
 *   targetUserId  - 要强制下线的目标用户 ID
 *
 * 成功后目标用户的会话将失效，下次请求时需要重新登录。
 */
@WebServlet("/api/admin/monitor/kick-user")
public class AdminKickUserServlet extends HttpServlet {

    private final AdminService adminService = new AdminService();
    private final MonitorService monitorService = new MonitorService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        // 管理员身份校验
        Long adminUserId = parseLong(request.getParameter("adminUserId"));
        if (!adminService.isAdmin(adminUserId)) {
            response.getWriter().write(JsonUtil.error("无管理员权限"));
            return;
        }

        // 获取目标用户 ID
        Long targetUserId = parseLong(request.getParameter("targetUserId"));
        if (targetUserId == null) {
            response.getWriter().write(JsonUtil.error("缺少目标用户ID"));
            return;
        }

        // 执行强制下线
        String error = monitorService.kickUser(
                getServletContext(), targetUserId, adminUserId);

        if (error != null) {
            response.getWriter().write(JsonUtil.error(error));
            return;
        }

        response.getWriter().write(JsonUtil.success("用户已被强制下线"));

        // 记录操作日志
        adminService.logAction(adminUserId, "user", targetUserId,
                "kick_user", "forced_logout");
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