package DAO;

import Entities.User;
import Utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class UserDAOImpl implements UserDAO {

    @Override
    public User findByUsername(String username) {
        String sql = "SELECT user_id, username, password, salt, profile, role, " +
                     "study_purpose, literacy, last_literacy, experience, " +
                     "last_checkin, created_at, last_login " +
                     "FROM user WHERE username = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询用户失败: " + username, e);
        }
        return null;
    }

    @Override
    public User findById(Long userId) {
        String sql = "SELECT user_id, username, password, salt, profile, role, " +
                     "study_purpose, literacy, last_literacy, experience, " +
                     "last_checkin, created_at, last_login " +
                     "FROM user WHERE user_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询用户失败: " + userId, e);
        }
        return null;
    }

    @Override
    public int insert(User user) {
        String sql = "INSERT INTO user (username, password, salt, profile, role, " +
                     "study_purpose, literacy, last_literacy, experience, " +
                     "last_checkin, created_at, last_login) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getSalt());
            ps.setString(4, user.getProfile());
            ps.setString(5, user.getRole() != null ? user.getRole() : "normal");
            ps.setString(6, user.getStudyPurpose());
            ps.setInt(7, user.getLiteracy() != null ? user.getLiteracy() : 0);
            ps.setTimestamp(8, toTimestamp(user.getLastLiteracy()));
            ps.setInt(9, user.getExperience() != null ? user.getExperience() : 0);
            ps.setTimestamp(10, toTimestamp(user.getLastCheckin()));
            ps.setTimestamp(11, toTimestamp(
                    user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now()));
            ps.setTimestamp(12, toTimestamp(user.getLastLogin()));

            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("插入用户失败: " + user.getUsername(), e);
        }
    }

    @Override
    public int updateLoginTime(Long userId, LocalDateTime time) {
        String sql = "UPDATE user SET last_login = ? WHERE user_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, toTimestamp(time));
            ps.setLong(2, userId);
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("更新登录时间失败", e);
        }
    }

    @Override
    public int updateCheckin(Long userId, LocalDateTime time, int experience) {
        String sql = "UPDATE user SET last_checkin = ?, experience = ? WHERE user_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, toTimestamp(time));
            ps.setInt(2, experience);
            ps.setLong(3, userId);
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("更新签到失败", e);
        }
    }

    @Override
    public int updateExperience(Long userId, int experience) {
        String sql = "UPDATE user SET experience = ? WHERE user_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, experience);
            ps.setLong(2, userId);
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("更新用户经验值失败: userId=" + userId, e);
        }
    }

    private User mapRow(ResultSet rs) throws Exception {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setSalt(rs.getString("salt"));
        user.setProfile(rs.getString("profile"));
        user.setRole(rs.getString("role"));
        user.setStudyPurpose(rs.getString("study_purpose"));
        user.setLiteracy(rs.getInt("literacy"));
        user.setLastLiteracy(toLocalDateTime(rs.getTimestamp("last_literacy")));
        user.setExperience(rs.getInt("experience"));
        user.setLastCheckin(toLocalDateTime(rs.getTimestamp("last_checkin")));
        user.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        user.setLastLogin(toLocalDateTime(rs.getTimestamp("last_login")));
        return user;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private Timestamp toTimestamp(LocalDateTime dt) {
        return dt != null ? Timestamp.valueOf(dt) : null;
    }
}
