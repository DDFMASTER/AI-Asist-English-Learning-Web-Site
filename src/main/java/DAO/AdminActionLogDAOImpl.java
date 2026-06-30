package DAO;

import Entities.AdminActionLog;
import Utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class AdminActionLogDAOImpl implements AdminActionLogDAO {

    @Override
    public int insert(AdminActionLog log) {
        String sql = "INSERT INTO admin_action_log (user_id, target_type, target_id, " +
                     "action_type, parameter, timestamp) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, log.getUserId());
            ps.setString(2, log.getTargetType());
            ps.setLong(3, log.getTargetId());
            ps.setString(4, log.getActionType());
            ps.setString(5, log.getParameter());
            ps.setTimestamp(6, log.getTimestamp() != null
                    ? Timestamp.valueOf(log.getTimestamp()) : null);

            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("插入管理员操作日志失败", e);
        }
    }
}
