package Servlet;

import Entities.User;
import Service.AdminService;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 管理员 — 用户列表
 *
 * GET /api/admin/user/list?adminUserId=<id>
 */
@WebServlet("/api/admin/user/list")
public class AdminUserListServlet extends HttpServlet {
    private final AdminService adminService = new AdminService();

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

        List<User> users = adminService.getAllUsers();

        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,\"users\":[");
        boolean first = true;
        for (User u : users) {
            if (!first) json.append(",");
            first = false;
            json.append("{");
            json.append("\"userId\":").append(JsonUtil.numVal(u.getUserId())).append(",");
            json.append("\"username\":").append(JsonUtil.strVal(u.getUsername())).append(",");
            json.append("\"role\":").append(JsonUtil.strVal(u.getRole())).append(",");
            json.append("\"studyPurpose\":").append(JsonUtil.strVal(u.getStudyPurpose())).append(",");
            json.append("\"literacy\":").append(JsonUtil.numVal(u.getLiteracy())).append(",");
            json.append("\"experience\":").append(JsonUtil.numVal(u.getExperience())).append(",");
            json.append("\"lastLogin\":").append(JsonUtil.formatDateTime(u.getLastLogin())).append(",");
            json.append("\"createdAt\":").append(JsonUtil.formatDateTime(u.getCreatedAt()));
            json.append("}");
        }
        json.append("]}");
        response.getWriter().write(json.toString());
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
