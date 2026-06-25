package Utils;

import java.sql.Connection;
import java.sql.Statement;

public class DBConnectTest {
    public static void main(String[] args) {
        String sql = """
                CREATE TABLE IF NOT EXISTS ConnectTest (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(255) NOT NULL
                ) DEFAULT CHARSET=utf8mb4
                """;

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            System.out.println("数据库连接成功，ConnectTest 表创建成功！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
