package Servlet;

import Service.AdminService;
import Service.MonitorService;
import Utils.JsonUtil;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 管理员 — 查看实时在线用户
 *
 * GET /api/admin/monitor/online-users?adminUserId=<adminId>
 *
 * 返回在线人数和在线用户列表（userId、username、role、studyPurpose、sessionId）
 */
@WebServlet("/api/admin/monitor/online-users")
public class AdminOnlineUsersServlet extends HttpServlet {

    private final AdminService adminService = new AdminService();
    private final MonitorService monitorService = new MonitorService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        // 管理员身份校验
        Long adminUserId = parseLong(request.getParameter("adminUserId"));
        if (!adminService.isAdmin(adminUserId)) {
            response.getWriter().write(JsonUtil.error("无管理员权限"));
            return;
        }

        ServletContext ctx = getServletContext();

        int onlineCount = monitorService.getOnlineCount(ctx);
        List<MonitorService.OnlineUserInfo> onlineUsers = monitorService.getOnlineUsers(ctx);

        // 确保管理员自己出现在列表中（有时 SessionListener 未及时捕获）
        boolean adminInList = false;
        for (MonitorService.OnlineUserInfo u : onlineUsers) {
            if (u.getUserId().equals(adminUserId)) { adminInList = true; break; }
        }
        if (!adminInList) {
            Entities.User adminUser = new DAO.UserDAOImpl().findById(adminUserId);
            if (adminUser != null) {
                onlineUsers = new java.util.ArrayList<>(onlineUsers);
                onlineUsers.add(new MonitorService.OnlineUserInfo(
                    adminUserId, adminUser.getUsername(), adminUser.getRole(),
                    adminUser.getStudyPurpose(), "当前会话", 0, 0, false));
                onlineCount = onlineUsers.size();
            }
        }

        // 构建 JSON 响应
        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,");
        json.append("\"onlineCount\":").append(onlineCount).append(",");
        json.append("\"onlineUsers\":[");

        boolean first = true;
        for (MonitorService.OnlineUserInfo u : onlineUsers) {
            if (!first) json.append(",");
            first = false;
            json.append("{");
            json.append("\"userId\":").append(JsonUtil.numVal(u.getUserId())).append(",");
            json.append("\"username\":").append(JsonUtil.strVal(u.getUsername())).append(",");
            json.append("\"role\":").append(JsonUtil.strVal(u.getRole())).append(",");
            json.append("\"studyPurpose\":").append(JsonUtil.strVal(u.getStudyPurpose())).append(",");
            json.append("\"sessionId\":").append(JsonUtil.strVal(u.getSessionId()));
            json.append("}");
        }

        json.append("]}");
        response.getWriter().write(json.toString());

        // 记录操作日志
        adminService.logAction(adminUserId, "monitor", null,
                "view_online_users", "count=" + onlineCount);
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