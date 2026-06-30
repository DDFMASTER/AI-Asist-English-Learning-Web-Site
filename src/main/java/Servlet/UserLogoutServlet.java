package Servlet;

import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * 用户登出 —— 销毁服务端会话。
 *
 * POST /api/user/logout
 *
 * 无需参数，直接销毁当前请求关联的 HttpSession。
 * 登出后前端应清除本地存储的 token 和用户信息。
 */
@WebServlet("/api/user/logout")
public class UserLogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);

        if (session != null) {
            Long userId = (Long) session.getAttribute("userId");
            String username = (String) session.getAttribute("username");

            System.out.println("[AAEL] 用户主动登出: userId=" + userId
                    + ", username=" + username);

            // 销毁会话（会触发 SessionListener.sessionDestroyed）
            session.invalidate();
        }

        response.getWriter().write(JsonUtil.success("已登出"));
    }

    /**
     * 也支持 GET 请求（兼容某些前端实现）
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        doPost(request, response);
    }
}