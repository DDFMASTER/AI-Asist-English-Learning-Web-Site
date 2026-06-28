package Service;

import DAO.UserDAO;
import DAO.UserDAOImpl;
import Entities.User;
import Utils.PasswordUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserService {
    private final UserDAO userDAO = new UserDAOImpl();

    /**
     * 登录：校验用户名和密码，成功返回用户对象，失败返回 null
     */
    public User login(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return null;
        }

        User user = userDAO.findByUsername(username);
        if (user == null) {
            return null;
        }

        if (!PasswordUtil.verify(password, user.getSalt(), user.getPassword())) {
            return null;
        }

        // 更新最近登录时间
        userDAO.updateLoginTime(user.getUserId(), LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());

        return user;
    }

    /**
     * 注册新用户
     * @return 错误消息，null 表示成功
     */
    public String register(String username, String password, String studyPurpose) {
        if (isBlank(username)) {
            return "用户名不能为空";
        }
        if (username.length() > 16) {
            return "用户名不能超过16个字符";
        }
        if (isBlank(password)) {
            return "密码不能为空";
        }
        if (isBlank(studyPurpose)) {
            return "请选择学习阶段";
        }
        // 校验学习阶段合法性
        String[] validStages = {"初中", "高中", "四级", "六级", "考研", "托福"};
        boolean valid = false;
        for (String s : validStages) {
            if (s.equals(studyPurpose)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            return "无效的学习阶段";
        }

        // 检查用户名是否已存在
        User existing = userDAO.findByUsername(username);
        if (existing != null) {
            return "用户名已存在";
        }

        // 生成盐值和密码密文
        String salt = PasswordUtil.generateSalt();
        String hashedPwd = PasswordUtil.md5Hash(password, salt);

        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPwd);
        user.setSalt(salt);
        user.setRole("normal");
        user.setStudyPurpose(studyPurpose);
        user.setLiteracy(0);
        user.setExperience(0);
        user.setCreatedAt(LocalDateTime.now());

        int rows = userDAO.insert(user);
        return rows > 0 ? null : "注册失败，请稍后重试";
    }

    /**
     * 累加用户经验值（完成任务时调用）
     * @param userId 用户ID
     * @param xpToAdd 要添加的经验值
     * @return 新的总经验值，-1 表示用户不存在，-2 表示已达每日上限
     */
    public int addExperience(Long userId, int xpToAdd) {
        User user = userDAO.findById(userId);
        if (user == null) {
            return -1;
        }

        int currentXp = user.getExperience() != null ? user.getExperience() : 0;
        int newXp = currentXp + xpToAdd;

        userDAO.updateExperience(userId, newXp);
        return newXp;
    }

    /**
     * 每日签到：返回获得的经验值，-1 表示今日已签到，-2 表示用户不存在
     */
    public int checkin(Long userId) {
        User user = userDAO.findById(userId);
        if (user == null) {
            return -2;
        }

        // 判断今天是否已签到
        if (user.getLastCheckin() != null) {
            LocalDate lastDate = user.getLastCheckin().toLocalDate();
            LocalDate today = LocalDate.now();
            if (lastDate.equals(today)) {
                return -1; // 今日已签到
            }
        }

        // 经验值 +10
        int newExp = user.getExperience() + 10;
        userDAO.updateCheckin(userId, LocalDateTime.now(), newExp);
        return 10;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
