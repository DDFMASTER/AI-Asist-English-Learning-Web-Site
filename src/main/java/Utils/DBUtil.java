package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
    private static final String URL =
            "jdbc:mysql://8.146.204.179:3306/aaeldb"
                    + "?useUnicode=true"
                    + "&characterEncoding=utf8"
                    + "&serverTimezone=Asia/Shanghai"
                    + "&useSSL=false"
                    + "&allowPublicKeyRetrieval=true";

    private static final String USER = "AAELDB";
    private static final String PASSWORD = "mCeeTPHyM6ZPwyMS";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver 加载失败", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
