package Servlet;

import Service.HealthService;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/connect-test")
public class ConnectTestServlet extends HttpServlet {
    private final HealthService healthService = new HealthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        HealthService.ConnectionTestResult result = healthService.testConnection();

        response.getWriter().write(
                "{\"message\":\"后端连接成功\""
                + ",\"dbConnected\":" + result.dbConnected
                + ",\"dbMessage\":\"" + JsonUtil.escapeJson(result.dbMessage) + "\""
                + "}");
    }
}
