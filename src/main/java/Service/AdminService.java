package Service;

import DAO.*;
import Entities.AdminActionLog;
import Entities.Article;
import Entities.User;
import Utils.DBUtil;
import Utils.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;

public class AdminService {
    private final UserDAO userDAO = new UserDAOImpl();
    private final ArticleDAO articleDAO = new ArticleDAOImpl();
    private final AdminActionLogDAO logDAO = new AdminActionLogDAOImpl();

    private static final String[] VALID_STAGES = {"初中", "高中", "四级", "六级", "考研", "托福", "期刊", "原著"};

    /**
     * 校验用户是否为管理员
     */
    public boolean isAdmin(Long userId) {
        if (userId == null) return false;
        User user = userDAO.findById(userId);
        return user != null && "admin".equals(user.getRole());
    }

    // ==================== 用户管理 ====================

    /**
     * 获取全部用户列表
     */
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    /**
     * 管理员创建新用户（默认密码 123456）
     * @return 错误消息，null 表示成功
     */
    public String createUser(String username, String studyPurpose) {
        if (isBlank(username)) {
            return "用户名不能为空";
        }
        if (username.length() > 16) {
            return "用户名不能超过16个字符";
        }
        if (isBlank(studyPurpose)) {
            return "请选择学习阶段";
        }

        boolean valid = false;
        for (String s : VALID_STAGES) {
            if (s.equals(studyPurpose)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            return "无效的学习阶段";
        }

        User existing = userDAO.findByUsername(username);
        if (existing != null) {
            return "用户名已存在";
        }

        String salt = PasswordUtil.generateSalt();
        String hashedPwd = PasswordUtil.md5Hash("123456", salt);

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
        return rows > 0 ? null : "创建用户失败，请稍后重试";
    }

    /**
     * 删除用户
     * @return 错误消息，null 表示成功
     */
    public String deleteUser(Long targetUserId, Long adminUserId) {
        if (targetUserId == null) {
            return "缺少目标用户ID";
        }
        if (targetUserId.equals(adminUserId)) {
            return "不能删除自己的账户";
        }

        User target = userDAO.findById(targetUserId);
        if (target == null) {
            return "用户不存在";
        }

        // 级联清理：删除用户的投票记录和学习记录
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 删除用户的学习记录
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM user_learned_article WHERE user_id = ?")) {
                    ps.setLong(1, targetUserId);
                    ps.executeUpdate();
                }
                // 删除用户的各处投票
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM ai_word_vote WHERE user_id = ?")) {
                    ps.setLong(1, targetUserId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM ai_article_explain_vote WHERE user_id = ?")) {
                    ps.setLong(1, targetUserId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM ai_vocquiz_vote WHERE user_id = ?")) {
                    ps.setLong(1, targetUserId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM ai_comquiz_vote WHERE user_id = ?")) {
                    ps.setLong(1, targetUserId);
                    ps.executeUpdate();
                }
                // 删除用户本身
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM user WHERE user_id = ?")) {
                    ps.setLong(1, targetUserId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("删除用户失败: " + targetUserId, e);
        }
    }

    /**
     * 重置用户密码为 123456
     * @return 错误消息，null 表示成功
     */
    public String resetPassword(Long targetUserId) {
        if (targetUserId == null) {
            return "缺少目标用户ID";
        }

        User target = userDAO.findById(targetUserId);
        if (target == null) {
            return "用户不存在";
        }

        String newSalt = PasswordUtil.generateSalt();
        String newHash = PasswordUtil.md5Hash("123456", newSalt);
        userDAO.updatePassword(targetUserId, newHash, newSalt);
        return null;
    }

    /**
     * 设置用户角色
     * @return 错误消息，null 表示成功
     */
    public String setRole(Long targetUserId, String role) {
        if (targetUserId == null) {
            return "缺少目标用户ID";
        }
        if (!"normal".equals(role) && !"vip".equals(role)) {
            return "无效的角色，仅支持 normal 或 vip";
        }

        User target = userDAO.findById(targetUserId);
        if (target == null) {
            return "用户不存在";
        }
        if ("admin".equals(target.getRole())) {
            return "不能修改管理员角色";
        }

        userDAO.updateRole(targetUserId, role);
        return null;
    }

    // ==================== 文章管理 ====================

    /**
     * 创建文章
     * @return 错误消息，null 表示成功
     */
    public String createArticle(String title, String content, String source, String difficulty) {
        if (isBlank(title)) {
            return "文章标题不能为空";
        }
        if (isBlank(content)) {
            return "文章内容不能为空";
        }
        if (isBlank(difficulty)) {
            return "请选择难度等级";
        }

        boolean valid = false;
        for (String s : VALID_STAGES) {
            if (s.equals(difficulty)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            return "无效的难度等级";
        }

        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setSource(source);
        article.setDifficulty(difficulty);
        article.setArticleLikeCount(0);
        article.setExplanationLikeCount(0);
        article.setExplanationDislikeCount(0);
        article.setVocquizNum(0);
        article.setComquizNum(0);

        int rows = articleDAO.insert(article);
        return rows > 0 ? null : "创建文章失败，请稍后重试";
    }

    /**
     * 删除文章（级联清理关联数据）
     * @return 错误消息，null 表示成功
     */
    public String deleteArticle(Long articleId) {
        if (articleId == null) {
            return "缺少文章ID";
        }

        Article article = articleDAO.findById(articleId);
        if (article == null) {
            return "文章不存在";
        }

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 删除关联的词汇题
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM quiz_vocabulary WHERE article_id = ?")) {
                    ps.setLong(1, articleId);
                    ps.executeUpdate();
                }
                // 删除关联的阅读理解题
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM quiz_comprehension WHERE article_id = ?")) {
                    ps.setLong(1, articleId);
                    ps.executeUpdate();
                }
                // 删除用户学习记录
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM user_learned_article WHERE article_id = ?")) {
                    ps.setLong(1, articleId);
                    ps.executeUpdate();
                }
                // 删除文章讲解投票
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM ai_article_explain_vote WHERE article_id = ?")) {
                    ps.setLong(1, articleId);
                    ps.executeUpdate();
                }
                // 删除文章本身
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM article WHERE article_id = ?")) {
                    ps.setLong(1, articleId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("删除文章失败: " + articleId, e);
        }
    }

    // ==================== 审计日志 ====================

    /**
     * 记录管理员操作
     */
    public void logAction(Long adminUserId, String targetType, Long targetId,
                          String actionType, String parameter) {
        AdminActionLog log = new AdminActionLog();
        log.setUserId(adminUserId);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setActionType(actionType);
        log.setParameter(parameter);
        log.setTimestamp(LocalDateTime.now());
        logDAO.insert(log);
    }

    // ==================== 工具方法 ====================

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
