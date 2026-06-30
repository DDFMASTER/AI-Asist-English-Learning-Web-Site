package Entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 请求日志条目 —— 记录单次 HTTP 请求的关键信息。
 * 用于管理员查看 IP 请求日志。
 */
public class RequestLogEntry {

    private String ip;              // 客户端 IP
    private String path;            // 请求路径
    private String method;          // HTTP 方法（GET/POST/...）
    private String userAgent;       // 浏览器 User-Agent
    private LocalDateTime timestamp; // 请求时间
    private String sessionId;       // 会话 ID
    private String username;        // 当前登录用户名（可能为 null）
    private String queryString;     // 请求参数

    public RequestLogEntry() {}

    public RequestLogEntry(String ip, String path, String method,
                           String userAgent, LocalDateTime timestamp,
                           String sessionId, String username, String queryString) {
        this.ip = ip;
        this.path = path;
        this.method = method;
        this.userAgent = userAgent;
        this.timestamp = timestamp;
        this.sessionId = sessionId;
        this.username = username;
        this.queryString = queryString;
    }

    // ========== Getters & Setters ==========

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getQueryString() { return queryString; }
    public void setQueryString(String queryString) { this.queryString = queryString; }

    /**
     * 将时间戳格式化为 "yyyy-MM-dd HH:mm:ss"
     */
    public String getFormattedTime() {
        if (timestamp == null) return "null";
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %s %s",
                getFormattedTime(), method, path,
                username != null ? "(" + username + ")" : "");
    }
}