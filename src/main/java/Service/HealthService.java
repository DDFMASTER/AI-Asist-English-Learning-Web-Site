package Service;

import Utils.DBUtil;

import java.sql.Connection;

public class HealthService {

    /**
     * 测试数据库连接是否正常
     * @return 连接状态描述
     */
    public ConnectionTestResult testConnection() {
        boolean connected = false;
        String message;

        try (Connection conn = DBUtil.getConnection()) {
            connected = conn != null && !conn.isClosed();
            message = connected ? "数据库连接正常" : "数据库连接异常";
        } catch (Exception e) {
            message = "数据库连接失败: " + e.getMessage();
        }

        return new ConnectionTestResult(connected, message);
    }

    public static class ConnectionTestResult {
        public final boolean dbConnected;
        public final String dbMessage;

        public ConnectionTestResult(boolean dbConnected, String dbMessage) {
            this.dbConnected = dbConnected;
            this.dbMessage = dbMessage;
        }
    }
}