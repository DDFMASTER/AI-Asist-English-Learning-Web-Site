package Servlet;

import Entities.User;
import DAO.UserDAOImpl;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 用户个人信息接口。
 * GET /api/user/profile?userId=xxx  或  ?username=xxx
 * 返回用户完整信息（含经验值、学习阶段等），供前端同步最新用户数据。
 */
@WebServlet("/api/user/profile")
public class UserProfileServlet extends HttpServlet {

    private final UserDAOImpl userDAO = new UserDAOImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        User user = null;

        // 优先用 userId 查询
        String userIdParam = request.getParameter("userId");
        if (userIdParam != null && !userIdParam.isBlank()) {
            try {
                Long userId = Long.parseLong(userIdParam);
                user = userDAO.findById(userId);
            } catch (NumberFormatException e) {
                response.getWriter().write(
                        JsonUtil.error("userId 格式错误"));
                return;
            }
        }

        // userId 查不到则尝试 username
        if (user == null) {
            String username = request.getParameter("username");
            if (username != null && !username.isBlank()) {
                user = userDAO.findByUsername(username);
            }
        }

        if (user == null) {
            response.getWriter().write(
                    JsonUtil.error("用户不存在，请提供有效的 userId 或 username"));
            return;
        }

        // 构建用户信息 JSON
        String extra = "\"user\":{"
                + "\"userId\":" + JsonUtil.numVal(user.getUserId())
                + ",\"username\":" + JsonUtil.strVal(user.getUsername())
                + ",\"role\":" + JsonUtil.strVal(user.getRole())
                + ",\"studyPurpose\":" + JsonUtil.strVal(user.getStudyPurpose())
                + ",\"experience\":" + JsonUtil.numVal(user.getExperience())
                + ",\"cefrProgress\":" + JsonUtil.numVal(user.getCefrProgress())
                + ",\"literacy\":" + JsonUtil.numVal(user.getLiteracy())
                + "}";

        response.getWriter().write(
                JsonUtil.buildResponse(true, "查询成功", extra));
    }
}
