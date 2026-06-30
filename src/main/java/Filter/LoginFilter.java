package Filter;

import Listener.AppContextListener;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

/**
 * 登录拦截过滤器 —— 拦截未登录用户访问受保护的 API 资源。
 *
 * 公开路径（无需登录）：
 *   /api/user/login, /api/user/register, /api/user/logout, /api/connect-test,
 *   /api/word/search, /api/word/lookup,
 *   /api/article/list, /api/article/detail
 *
 * 其余 /api/* 路径均需登录后才能访问。
 * 静态资源（非 /api/ 前缀）直接放行。
 *
 * 额外检查：若用户已被管理员强制下线（userId 在 kickedUserIds 集合中），
 * 则销毁其会话并返回 401。
 */
@WebFilter("/*")
public class LoginFilter implements Filter {

    /** 无需登录即可访问的 API 路径（精确匹配） */
    private static final Set<String> PUBLIC_API_PATHS = Set.of(
            "/api/user/login",
            "/api/user/register",
            "/api/user/logout",
            "/api/connect-test"
    );

    /** 无需登录即可访问的 API 路径前缀 */
    private static final String[] PUBLIC_API_PREFIXES = {
            "/api/word/",
            "/api/article/list",
            "/api/article/detail"
    };

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("[AAEL] LoginFilter 初始化完成");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        String relativePath = path.substring(contextPath.length());

        // 1. 公开路径直接放行
        if (isPublicPath(relativePath)) {
            chain.doFilter(req, res);
            return;
        }

        // 2. API 路径需要校验登录状态
        if (relativePath.startsWith("/api/")) {
            HttpSession session = request.getSession(false);

            if (session == null || session.getAttribute("userId") == null) {
                // 未登录，返回 401
                sendUnauthorized(response, "请先登录");
                return;
            }

            // 3. 检查是否被管理员强制下线
            Long userId = (Long) session.getAttribute("userId");
            if (isKicked(request.getServletContext(), userId)) {
                // 清理会话并移除下线标记
                session.removeAttribute("userId");
                session.removeAttribute("username");
                session.removeAttribute("role");
                session.invalidate();

                clearKickedFlag(request.getServletContext(), userId);

                sendUnauthorized(response, "您已被管理员强制下线，请重新登录");
                return;
            }
        }

        // 4. 放行
        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
        System.out.println("[AAEL] LoginFilter 销毁");
    }

    /**
     * 判断路径是否为公开路径（无需登录）
     */
    private boolean isPublicPath(String path) {
        // 精确匹配
        if (PUBLIC_API_PATHS.contains(path)) {
            return true;
        }
        // 前缀匹配（公开 API）
        for (String prefix : PUBLIC_API_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        // 非 API 路径（静态资源、页面等）直接放行
        if (!path.startsWith("/api/")) {
            return true;
        }
        return false;
    }

    /**
     * 检查用户是否已被管理员强制下线
     */
    private boolean isKicked(ServletContext ctx, Long userId) {
        if (userId == null) return false;
        @SuppressWarnings("unchecked")
        Set<Long> kickedSet = (Set<Long>) ctx.getAttribute(AppContextListener.KICKED_USER_IDS);
        return kickedSet != null && kickedSet.contains(userId);
    }

    /**
     * 清除用户的强制下线标记
     */
    private void clearKickedFlag(ServletContext ctx, Long userId) {
        if (userId == null) return;
        @SuppressWarnings("unchecked")
        Set<Long> kickedSet = (Set<Long>) ctx.getAttribute(AppContextListener.KICKED_USER_IDS);
        if (kickedSet != null) {
            kickedSet.remove(userId);
        }
    }

    /**
     * 发送 401 未授权响应
     */
    private void sendUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(
                "{\"success\":false,\"message\":\"" + message + "\",\"code\":401}");
    }
}