package Service;

import DAO.UserDAO;
import DAO.UserDAOImpl;
import Entities.RequestLogEntry;
import Entities.User;
import Listener.AppContextListener;
import Listener.RequestLogListener;
import jakarta.servlet.ServletContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 监控服务 —— 为管理员提供在线用户统计、请求日志查阅、强制登出等功能。
 */
public class MonitorService {

    private final UserDAO userDAO = new UserDAOImpl();

    /**
     * 在线用户信息 VO
     */
    public static class OnlineUserInfo {
        private final Long userId;
        private final String username;
        private final String role;
        private final String studyPurpose;
        private final String sessionId;
        private final long loginTime;
        private final long sessionCreatedAt;
        private final boolean isTimeout;

        public OnlineUserInfo(Long userId, String username, String role,
                              String studyPurpose, String sessionId,
                              long loginTime, long sessionCreatedAt, boolean isTimeout) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.studyPurpose = studyPurpose;
            this.sessionId = sessionId;
            this.loginTime = loginTime;
            this.sessionCreatedAt = sessionCreatedAt;
            this.isTimeout = isTimeout;
        }

        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public String getStudyPurpose() { return studyPurpose; }
        public String getSessionId() { return sessionId; }
        public long getLoginTime() { return loginTime; }
        public long getSessionCreatedAt() { return sessionCreatedAt; }
        public boolean isTimeout() { return isTimeout; }
    }

    /**
     * 获取当前在线用户列表
     */
    public List<OnlineUserInfo> getOnlineUsers(ServletContext ctx) {
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Long, String> userSessionMap =
                (ConcurrentHashMap<Long, String>) ctx.getAttribute(AppContextListener.USER_SESSION_MAP);

        if (userSessionMap == null || userSessionMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<OnlineUserInfo> result = new ArrayList<>();

        for (Long userId : userSessionMap.keySet()) {
            User user = userDAO.findById(userId);
            if (user == null) continue;

            String sessionId = userSessionMap.get(userId);
            long loginTime = 0;
            long sessionCreatedAt = 0;
            boolean timeout = false;

            // 尝试获取会话详情
            if (sessionId != null) {
                try {
                    // 通过 sessionId 获取会话（需要遍历，Tomcat 不直接支持）
                    // 这里我们通过监听器记录的属性来获取
                } catch (Exception ignored) {}
            }

            result.add(new OnlineUserInfo(
                    userId,
                    user.getUsername(),
                    user.getRole(),
                    user.getStudyPurpose(),
                    sessionId != null ? sessionId : "未知",
                    loginTime,
                    sessionCreatedAt,
                    timeout
            ));
        }

        // 按 userId 排序
        result.sort(Comparator.comparing(OnlineUserInfo::getUserId));
        return result;
    }

    /**
     * 获取在线人数
     */
    public int getOnlineCount(ServletContext ctx) {
        AtomicInteger count = (AtomicInteger) ctx.getAttribute(AppContextListener.ONLINE_COUNT);
        return count != null ? count.get() : 0;
    }

    /**
     * 获取请求日志（最近 N 条）
     */
    public List<RequestLogEntry> getRecentLogs(ServletContext ctx, int limit) {
        @SuppressWarnings("unchecked")
        List<RequestLogEntry> logs =
                (List<RequestLogEntry>) ctx.getAttribute(RequestLogListener.REQUEST_LOGS);

        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }

        synchronized (logs) {
            int size = logs.size();
            int fromIndex = Math.max(0, size - limit);
            // 返回副本，倒序（最新在前）
            List<RequestLogEntry> result = new ArrayList<>(logs.subList(fromIndex, size));
            Collections.reverse(result);
            return result;
        }
    }

    /**
     * 获取全部请求日志数量
     */
    public int getTotalLogCount(ServletContext ctx) {
        @SuppressWarnings("unchecked")
        List<RequestLogEntry> logs =
                (List<RequestLogEntry>) ctx.getAttribute(RequestLogListener.REQUEST_LOGS);
        return logs != null ? logs.size() : 0;
    }

    /**
     * 强制用户下线（通过 userId）。
     * 将目标用户 ID 加入 kickedUserIds 集合，
     * 用户的下一次请求将被 LoginFilter 拦截并强制销毁会话。
     *
     * @return null 表示成功，否则返回错误消息
     */
    public String kickUser(ServletContext ctx, Long targetUserId, Long adminUserId) {
        if (targetUserId == null) {
            return "缺少目标用户ID";
        }
        if (targetUserId.equals(adminUserId)) {
            return "不能强制自己下线";
        }

        // 检查目标用户是否存在
        User target = userDAO.findById(targetUserId);
        if (target == null) {
            return "用户不存在";
        }

        // 检查目标用户是否为管理员（不能踢管理员）
        if ("admin".equals(target.getRole())) {
            return "不能强制管理员下线";
        }

        // 查找用户是否在线
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Long, String> userSessionMap =
                (ConcurrentHashMap<Long, String>) ctx.getAttribute(AppContextListener.USER_SESSION_MAP);

        if (userSessionMap == null || !userSessionMap.containsKey(targetUserId)) {
            return "该用户当前不在线";
        }

        // 将用户加入强制下线集合
        @SuppressWarnings("unchecked")
        Set<Long> kickedSet = (Set<Long>) ctx.getAttribute(AppContextListener.KICKED_USER_IDS);
        if (kickedSet != null) {
            kickedSet.add(targetUserId);
        }

        // 从在线用户映射中移除（等待 LoginFilter 处理实际会话销毁）
        userSessionMap.remove(targetUserId);

        System.out.println("[AAEL] 管理员强制用户下线: targetUserId=" + targetUserId
                + " | adminUserId=" + adminUserId);

        return null; // 成功
    }

    /**
     * 清空请求日志
     */
    public void clearLogs(ServletContext ctx) {
        @SuppressWarnings("unchecked")
        List<RequestLogEntry> logs =
                (List<RequestLogEntry>) ctx.getAttribute(RequestLogListener.REQUEST_LOGS);
        if (logs != null) {
            synchronized (logs) {
                logs.clear();
            }
        }
    }
}