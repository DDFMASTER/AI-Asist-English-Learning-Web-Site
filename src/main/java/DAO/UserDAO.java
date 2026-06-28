package DAO;

import Entities.User;

import java.time.LocalDateTime;

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
}
