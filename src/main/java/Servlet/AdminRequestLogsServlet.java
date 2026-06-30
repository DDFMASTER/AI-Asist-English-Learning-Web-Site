package Servlet;

import Entities.RequestLogEntry;
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
 * 管理员 — 查阅 IP 请求日志
 *
 * GET /api/admin/monitor/request-logs?adminUserId=<adminId>&limit=<limit>
 *
 * 参数 limit 可选，默认返回最近 100 条，最大 500 条。
 * 返回请求日志列表（ip、path、method、timestamp、username、sessionId）
 */
@WebServlet("/api/admin/monitor/request-logs")
public class AdminRequestLogsServlet extends HttpServlet {

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

        // 获取 limit 参数（默认 100，最大 500）
        int limit = 100;
        String limitParam = request.getParameter("limit");
        if (limitParam != null && !limitParam.isBlank()) {
            try {
                limit = Integer.parseInt(limitParam);
                if (limit < 1) limit = 1;
                if (limit > 500) limit = 500;
            } catch (NumberFormatException ignored) {
                // 使用默认值
            }
        }

        ServletContext ctx = getServletContext();

        int totalCount = monitorService.getTotalLogCount(ctx);
        List<RequestLogEntry> logs = monitorService.getRecentLogs(ctx, limit);

        // 构建 JSON 响应
        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,");
        json.append("\"totalCount\":").append(totalCount).append(",");
        json.append("\"returnedCount\":").append(logs.size()).append(",");
        json.append("\"logs\":[");

        boolean first = true;
        for (RequestLogEntry entry : logs) {
            if (!first) json.append(",");
            first = false;
            json.append("{");
            json.append("\"ip\":").append(JsonUtil.strVal(entry.getIp())).append(",");
            json.append("\"path\":").append(JsonUtil.strVal(entry.getPath())).append(",");
            json.append("\"method\":").append(JsonUtil.strVal(entry.getMethod())).append(",");
            json.append("\"timestamp\":").append(JsonUtil.strVal(entry.getFormattedTime())).append(",");
            json.append("\"username\":").append(JsonUtil.strVal(entry.getUsername())).append(",");
            json.append("\"sessionId\":").append(JsonUtil.strVal(entry.getSessionId())).append(",");
            json.append("\"queryString\":").append(JsonUtil.strVal(entry.getQueryString())).append(",");
            json.append("\"userAgent\":").append(JsonUtil.strVal(entry.getUserAgent()));
            json.append("}");
        }

        json.append("]}");
        response.getWriter().write(json.toString());

        // 记录操作日志
        adminService.logAction(adminUserId, "monitor", null,
                "view_request_logs", "limit=" + limit);
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