package Servlet;

import Service.UserService;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 用户经验值更新接口
 *
 * POST /api/user/experience
 * 参数: userId (Long) — 用户ID
 *       xp    (int)  — 经验值变动量（正数累加，负数扣减）
 *
 * 响应:
 *   { success: true, message: "经验值更新成功", newExperience: 150, addedXp: 20 }
 *   { success: false, message: "用户不存在" }
 */
@WebServlet("/api/user/experience")
public class UserExperienceServlet extends HttpServlet {
    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        // 读取参数
        String userIdParam = request.getParameter("userId");
        String xpParam = request.getParameter("xp");

        if (userIdParam == null || xpParam == null) {
            response.getWriter().write(
                    JsonUtil.error("缺少必要参数 userId 或 xp"));
            return;
        }

        Long userId;
        int xpToAdd;
        try {
            userId = Long.parseLong(userIdParam);
            xpToAdd = Integer.parseInt(xpParam);
        } catch (NumberFormatException e) {
            response.getWriter().write(
                    JsonUtil.error("参数格式错误: userId 和 xp 必须为整数"));
            return;
        }

        if (xpToAdd == 0) {
            response.getWriter().write(
                    JsonUtil.error("xp 不能为 0"));
            return;
        }

        // 调用 Service 更新经验值
        int newExperience = userService.addExperience(userId, xpToAdd);

        if (newExperience == -1) {
            response.getWriter().write(
                    JsonUtil.error("用户不存在"));
            return;
        }

        // 构建成功响应
        String extra = "\"newExperience\":" + JsonUtil.numVal(newExperience)
                + ",\"addedXp\":" + JsonUtil.numVal(xpToAdd);

        response.getWriter().write(
                JsonUtil.buildResponse(true, "经验值更新成功", extra));
    }

    /**
     * GET 请求：查询当前经验值
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String userIdParam = request.getParameter("userId");
        if (userIdParam == null) {
            response.getWriter().write(
                    JsonUtil.error("缺少参数 userId"));
            return;
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdParam);
        } catch (NumberFormatException e) {
            response.getWriter().write(
                    JsonUtil.error("userId 格式错误"));
            return;
        }

        // 这里我们复用 addExperience(0) 来获取当前经验值不太合适
        // 改为直接查询
        Entities.User user = new DAO.UserDAOImpl().findById(userId);
        if (user == null) {
            response.getWriter().write(
                    JsonUtil.error("用户不存在"));
            return;
        }

        int experience = user.getExperience() != null ? user.getExperience() : 0;
        String extra = "\"experience\":" + JsonUtil.numVal(experience);

        response.getWriter().write(
                JsonUtil.buildResponse(true, "查询成功", extra));
    }
}
