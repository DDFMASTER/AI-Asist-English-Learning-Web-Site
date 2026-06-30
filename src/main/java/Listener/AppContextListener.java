package Listener;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 全局上下文监听器 —— 负责 Web 应用启动时的初始化与关闭时的资源释放。
 *
 * 初始化时创建：
 *   - onlineCount     (AtomicInteger) 在线人数计数器
 *   - userSessionMap  (ConcurrentHashMap<Long, String>) 用户ID → 会话ID 映射
 *   - kickedUserIds   (ConcurrentHashMap.newKeySet<Long>()) 被强制下线的用户ID集合
 *   - appStartTime    (LocalDateTime) 应用启动时间
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    /** ServletContext 中存储在线用户数的 key */
    public static final String ONLINE_COUNT = "onlineCount";

    /** ServletContext 中存储用户-会话映射的 key */
    public static final String USER_SESSION_MAP = "userSessionMap";

    /** ServletContext 中存储被强制下线用户 ID 集合的 key */
    public static final String KICKED_USER_IDS = "kickedUserIds";

    /** ServletContext 中存储应用启动时间的 key */
    public static final String APP_START_TIME = "appStartTime";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();

        // 初始化全局属性
        ctx.setAttribute(ONLINE_COUNT, new AtomicInteger(0));
        ctx.setAttribute(USER_SESSION_MAP, new ConcurrentHashMap<Long, String>());
        ctx.setAttribute(KICKED_USER_IDS, ConcurrentHashMap.newKeySet());
        ctx.setAttribute(APP_START_TIME, LocalDateTime.now());

        // 设置会话超时时间（30 分钟）
        ctx.setSessionTimeout(30);

        System.out.println("[AAEL] ========================================");
        System.out.println("[AAEL]        应 用 启 动 成 功");
        System.out.println("[AAEL] 启动时间 : " + LocalDateTime.now());
        System.out.println("[AAEL] 上下文路径: " + ctx.getContextPath());
        System.out.println("[AAEL] 会话超时  : 30 分钟");
        System.out.println("[AAEL] ========================================");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();

        AtomicInteger count = (AtomicInteger) ctx.getAttribute(ONLINE_COUNT);
        int onlineUsers = count != null ? count.get() : 0;

        LocalDateTime startTime = (LocalDateTime) ctx.getAttribute(APP_START_TIME);

        System.out.println("[AAEL] ========================================");
        System.out.println("[AAEL]        应 用 正 在 关 闭");
        System.out.println("[AAEL] 关闭时间 : " + LocalDateTime.now());
        System.out.println("[AAEL] 启动时间 : " + startTime);
        System.out.println("[AAEL] 在线用户 : " + onlineUsers);
        System.out.println("[AAEL] ========================================");

        // 清理全局属性
        ctx.removeAttribute(ONLINE_COUNT);
        ctx.removeAttribute(USER_SESSION_MAP);
        ctx.removeAttribute(KICKED_USER_IDS);
        ctx.removeAttribute("requestLogs");
        ctx.removeAttribute(APP_START_TIME);
    }
}