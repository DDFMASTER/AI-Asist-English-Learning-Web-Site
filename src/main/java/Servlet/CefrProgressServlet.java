package Servlet;

import DAO.UserDAOImpl;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * CEFR 等级进度 — 查询与更新
 *
 * GET  /api/user/cefr-progress?userId=   → { success, cefrProgress }
 * POST /api/user/cefr-progress            → 参数 userId, progress(0-100)
 */
@WebServlet("/api/user/cefr-progress")
public class CefrProgressServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        Long userId = parseLong(request.getParameter("userId"));
        if (userId == null) {
            response.getWriter().write(JsonUtil.error("缺少 userId"));
            return;
        }
        var user = new UserDAOImpl().findById(userId);
        if (user == null) {
            response.getWriter().write(JsonUtil.error("用户不存在"));
            return;
        }
        int progress = user.getCefrProgress() != null ? user.getCefrProgress() : 0;
        String extra = "\"cefrProgress\":" + JsonUtil.numVal(progress);
        response.getWriter().write(JsonUtil.buildResponse(true, "查询成功", extra));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        Long userId = parseLong(request.getParameter("userId"));
        var dao = new UserDAOImpl();

        // 升级场景：同时更新 literacy 和 level
        String literacyStr = request.getParameter("literacy");
        if (literacyStr != null) {
            int literacy = Integer.parseInt(literacyStr);
            dao.updateLiteracy(userId, literacy);
        }

        // 更新进度
        String progressStr = request.getParameter("progress");
        if (progressStr != null) {
            int progress = Integer.parseInt(progressStr);
            if (progress < 0) progress = 0;
            if (progress > 100) progress = 100;
            dao.updateCefrProgress(userId, progress);
        } else {
            response.getWriter().write(JsonUtil.error("缺少 progress 参数"));
            return;
        }

        String extra = "\"cefrProgress\":" + JsonUtil.numVal(
                Integer.parseInt(progressStr));
        response.getWriter().write(JsonUtil.buildResponse(true, "更新成功", extra));
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }
}
