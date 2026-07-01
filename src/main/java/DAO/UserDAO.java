package DAO;

import Entities.User;

import java.time.LocalDateTime;
import java.util.List;

public interface UserDAO {
    User findByUsername(String username);

    User findById(Long userId);

    int insert(User user);

    int updateLoginTime(Long userId, LocalDateTime time);

    int updateCheckin(Long userId, LocalDateTime time, int experience);

    /**
     * 更新用户总经验值（累加）
     * @param userId 用户ID
     * @param experience 新的总经验值
     */
    int updateExperience(Long userId, int experience);

    /**
     * 更新用户词汇量
     * @param userId  用户ID
     * @param literacy 词汇量
     */
    int updateLiteracy(Long userId, int literacy);

    /**
     * 更新用户 CEFR 等级进度
     * @param userId  用户ID
     * @param progress 进度值 0-100
     */
    int updateCefrProgress(Long userId, int progress);

    // ========== 管理员方法 ==========

    /**
     * 查询全部用户
     * @return 按 user_id 倒序排列的用户列表
     */
    List<User> findAll();

    /**
     * 删除用户
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteById(Long userId);

    /**
     * 重置用户密码（同时更新 salt 和 password）
     * @param userId 用户ID
     * @param password 新密码密文
     * @param salt 新盐值
     * @return 影响行数
     */
    int updatePassword(Long userId, String password, String salt);

    /**
     * 更新用户角色
     * @param userId 用户ID
     * @param role 新角色（normal / vip）
     * @return 影响行数
     */
    int updateRole(Long userId, String role);
}
