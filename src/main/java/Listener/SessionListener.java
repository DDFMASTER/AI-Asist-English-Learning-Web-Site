package Listener;

import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 会话监听器 —— 统计在线人数，追踪用户上下线状态。
 *
 * 功能：
 *   1. 会话创建 / 销毁时更新在线人数
 *   2. 用户登录（设置 userId 属性）时建立 userId → sessionId 映射
 *   3. 用户登出或会话超时时清理映射
 *   4. 支持管理员强制用户下线（通过 kickedUserIds 集合）
 */
@WebListener
public class SessionListener implements HttpSessionListener, HttpSessionAttributeListener {

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        ServletContext ctx = session.getServletContext();

        AtomicInteger count = (AtomicInteger) ctx.getAttribute(AppContextListener.ONLINE_COUNT);
        if (count != null) {
            int current = count.incrementAndGet();
            System.out.println("[AAEL] 会话创建: " + session.getId()
                    + " | 当前在线会话数: " + current);
        }

        // 记录会话创建时间
        session.setAttribute("__sessionCreatedAt", System.currentTimeMillis());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        ServletContext ctx = session.getServletContext();

        // 更新在线人数
        AtomicInteger count = (AtomicInteger) ctx.getAttribute(AppContextListener.ONLINE_COUNT);
        if (count != null) {
            int current = count.decrementAndGet();
            System.out.println("[AAEL] 会话销毁: " + session.getId()
                    + " | 当前在线会话数: " + current);
        }

        // 清理用户-会话映射
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<Long, String> map =
                    (ConcurrentHashMap<Long, String>) ctx.getAttribute(AppContextListener.USER_SESSION_MAP);
            if (map != null) {
                String removed = map.remove(userId);
                if (removed != null) {
                    System.out.println("[AAEL] 用户下线: userId=" + userId
                            + " | sessionId=" + session.getId()
                            + " | 原因: " + (isTimeout(session) ? "超时" : "主动登出"));
                }
            }

            // 清理强制下线标记
            @SuppressWarnings("unchecked")
            Set<Long> kickedSet = (Set<Long>) ctx.getAttribute(AppContextListener.KICKED_USER_IDS);
            if (kickedSet != null) {
                kickedSet.remove(userId);
            }
        }
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent se) {
        if ("userId".equals(se.getName())) {
            Long userId = (Long) se.getValue();
            if (userId == null) return;

            HttpSession session = se.getSession();
            ServletContext ctx = session.getServletContext();

            @SuppressWarnings("unchecked")
            ConcurrentHashMap<Long, String> map =
                    (ConcurrentHashMap<Long, String>) ctx.getAttribute(AppContextListener.USER_SESSION_MAP);
            if (map != null) {
                // 如果该用户已有一个旧会话，先清理
                String oldSessionId = map.get(userId);
                if (oldSessionId != null && !oldSessionId.equals(session.getId())) {
                    map.remove(userId);
                    System.out.println("[AAEL] 用户重复登录: userId=" + userId
                            + " | 旧会话已清理: " + oldSessionId);
                }
                map.put(userId, session.getId());
            }

            // 记录用户上线时间
            session.setAttribute("__userLoginTime", System.currentTimeMillis());

            System.out.println("[AAEL] 用户上线: userId=" + userId
                    + " | sessionId=" + session.getId());
        }
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent se) {
        if ("userId".equals(se.getName())) {
            Long userId = (Long) se.getValue();
            if (userId == null) return;

            HttpSession session = se.getSession();
            ServletContext ctx = session.getServletContext();

            @SuppressWarnings("unchecked")
            ConcurrentHashMap<Long, String> map =
                    (ConcurrentHashMap<Long, String>) ctx.getAttribute(AppContextListener.USER_SESSION_MAP);
            if (map != null) {
                map.remove(userId);
            }

            System.out.println("[AAEL] 用户登出: userId=" + userId
                    + " | sessionId=" + session.getId());
        }
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent se) {
        if ("userId".equals(se.getName())) {
            Long oldUserId = (Long) se.getValue();           // 旧值
            Long newUserId = (Long) se.getSession().getAttribute("userId"); // 新值

            HttpSession session = se.getSession();
            ServletContext ctx = session.getServletContext();

            @SuppressWarnings("unchecked")
            ConcurrentHashMap<Long, String> map =
                    (ConcurrentHashMap<Long, String>) ctx.getAttribute(AppContextListener.USER_SESSION_MAP);
            if (map != null) {
                if (oldUserId != null && !oldUserId.equals(newUserId)) {
                    map.remove(oldUserId);
                }
                if (newUserId != null) {
                    map.put(newUserId, session.getId());
                }
            }
        }
    }

    /**
     * 判断会话是否因超时而销毁。
     * 若会话的最后访问时间距现在超过最大不活动间隔，则为超时。
     */
    private boolean isTimeout(HttpSession session) {
        try {
            int maxInactive = session.getMaxInactiveInterval();
            if (maxInactive <= 0) return false;
            long lastAccessed = session.getLastAccessedTime();
            long now = System.currentTimeMillis();
            return (now - lastAccessed) > (maxInactive * 1000L);
        } catch (Exception e) {
            return false;
        }
    }
}