package Listener;

import Entities.RequestLogEntry;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 请求日志监听器 —— 记录每次 HTTP 请求的 IP、访问路径、访问时间等信息。
 *
 * 日志存储在 ServletContext 的 "requestLogs" 属性中，
 * 使用同步列表实现线程安全，最多保留 1000 条记录（FIFO）。
 */
@WebListener
public class RequestLogListener implements ServletRequestListener {

    /** 最大日志条目数 */
    private static final int MAX_LOG_ENTRIES = 1000;

    /** ServletContext 中存储请求日志列表的 key */
    public static final String REQUEST_LOGS = "requestLogs";

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        ServletRequest request = sre.getServletRequest();

        // 只记录 HTTP 请求，跳过静态资源请求以减少日志量
        if (!(request instanceof HttpServletRequest)) {
            return;
        }

        HttpServletRequest httpReq = (HttpServletRequest) request;
        String path = httpReq.getRequestURI();

        // 跳过静态资源请求
        if (isStaticResource(path)) {
            return;
        }

        RequestLogEntry entry = new RequestLogEntry();
        entry.setIp(getClientIp(httpReq));
        entry.setPath(path);
        entry.setMethod(httpReq.getMethod());
        entry.setUserAgent(truncate(httpReq.getHeader("User-Agent"), 255));
        entry.setTimestamp(LocalDateTime.now());
        entry.setQueryString(httpReq.getQueryString());
        entry.setSessionId(getSessionId(httpReq));
        entry.setUsername(getUsername(httpReq));

        ServletContext ctx = sre.getServletContext();
        List<RequestLogEntry> logs = getLogList(ctx);

        synchronized (logs) {
            if (logs.size() >= MAX_LOG_ENTRIES) {
                logs.remove(0); // 移除最旧记录
            }
            logs.add(entry);
        }
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        // 无需处理
    }

    /**
     * 从 ServletContext 中获取或创建日志列表
     */
    @SuppressWarnings("unchecked")
    static List<RequestLogEntry> getLogList(ServletContext ctx) {
        List<RequestLogEntry> logs = (List<RequestLogEntry>) ctx.getAttribute(REQUEST_LOGS);
        if (logs == null) {
            logs = Collections.synchronizedList(new ArrayList<>());
            ctx.setAttribute(REQUEST_LOGS, logs);
        }
        return logs;
    }

    /**
     * 获取客户端真实 IP（支持反向代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 获取当前请求的会话 ID
     */
    private String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null ? session.getId() : null;
    }

    /**
     * 获取当前请求的用户名（从会话中获取）
     */
    private String getUsername(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String username = (String) session.getAttribute("username");
            if (username != null) {
                return username;
            }
        }
        return null;
    }

    /**
     * 判断是否为静态资源路径
     */
    private boolean isStaticResource(String path) {
        if (path == null) return true;
        String lower = path.toLowerCase();
        return lower.endsWith(".js")
                || lower.endsWith(".css")
                || lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".gif")
                || lower.endsWith(".svg")
                || lower.endsWith(".ico")
                || lower.endsWith(".woff")
                || lower.endsWith(".woff2")
                || lower.endsWith(".ttf")
                || lower.endsWith(".map");
    }

    /**
     * 截断字符串到指定长度
     */
    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen);
    }
}