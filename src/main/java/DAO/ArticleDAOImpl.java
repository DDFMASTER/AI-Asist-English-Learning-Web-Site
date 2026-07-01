package DAO;

import Entities.Article;
import Utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ArticleDAOImpl implements ArticleDAO {

    @Override
    public List<Article> findByDifficulty(String difficulty) {
        List<Article> articles = new ArrayList<>();

        String sql = "SELECT article_id, title, difficulty, article_like_count, " +
                     "explanation_like_count, explanation_dislike_count, " +
                     "vocquiz_num, comquiz_num " +
                     "FROM article";

        if (difficulty != null && !difficulty.isBlank()) {
            sql += " WHERE difficulty = ?";
        }
        sql += " ORDER BY article_id DESC LIMIT 20";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (difficulty != null && !difficulty.isBlank()) {
                ps.setString(1, difficulty);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Article article = new Article();
                    article.setArticleId(rs.getLong("article_id"));
                    article.setTitle(rs.getString("title"));
                    article.setDifficulty(rs.getString("difficulty"));
                    article.setArticleLikeCount(rs.getInt("article_like_count"));
                    article.setExplanationLikeCount(rs.getInt("explanation_like_count"));
                    article.setExplanationDislikeCount(rs.getInt("explanation_dislike_count"));
                    article.setVocquizNum(rs.getInt("vocquiz_num"));
                    article.setComquizNum(rs.getInt("comquiz_num"));
                    articles.add(article);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询文章列表失败", e);
        }
        return articles;
    }

    // ========== 管理员方法 ==========

    @Override
    public List<Article> findAllPaginated(int offset, int limit) {
        List<Article> articles = new ArrayList<>();
        String sql = "SELECT article_id, title, source, difficulty, article_like_count, " +
                     "explanation_like_count, explanation_dislike_count, " +
                     "vocquiz_num, comquiz_num " +
                     "FROM article ORDER BY article_id DESC LIMIT ? OFFSET ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Article article = new Article();
                    article.setArticleId(rs.getLong("article_id"));
                    article.setTitle(rs.getString("title"));
                    article.setSource(rs.getString("source"));
                    article.setDifficulty(rs.getString("difficulty"));
                    article.setArticleLikeCount(rs.getInt("article_like_count"));
                    article.setExplanationLikeCount(rs.getInt("explanation_like_count"));
                    article.setExplanationDislikeCount(rs.getInt("explanation_dislike_count"));
                    article.setVocquizNum(rs.getInt("vocquiz_num"));
                    article.setComquizNum(rs.getInt("comquiz_num"));
                    articles.add(article);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("分页查询文章列表失败", e);
        }
        return articles;
    }

    @Override
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM article";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("查询文章总数失败", e);
        }
        return 0;
    }

    @Override
    public Article findById(Long articleId) {
        String sql = "SELECT article_id, title, content, source, difficulty, " +
                     "article_like_count, explanation, explanation_like_count, " +
                     "explanation_dislike_count, vocquiz_num, comquiz_num " +
                     "FROM article WHERE article_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, articleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Article article = new Article();
                    article.setArticleId(rs.getLong("article_id"));
                    article.setTitle(rs.getString("title"));
                    article.setContent(rs.getString("content"));
                    article.setSource(rs.getString("source"));
                    article.setDifficulty(rs.getString("difficulty"));
                    article.setArticleLikeCount(rs.getInt("article_like_count"));
                    article.setExplanation(rs.getString("explanation"));
                    article.setExplanationLikeCount(rs.getInt("explanation_like_count"));
                    article.setExplanationDislikeCount(rs.getInt("explanation_dislike_count"));
                    article.setVocquizNum(rs.getInt("vocquiz_num"));
                    article.setComquizNum(rs.getInt("comquiz_num"));
                    return article;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询文章失败: " + articleId, e);
        }
        return null;
    }

    @Override
    public int insert(Article article) {
        String sql = "INSERT INTO article (title, content, source, difficulty) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, article.getTitle());
            ps.setString(2, article.getContent());
            ps.setString(3, article.getSource());
            ps.setString(4, article.getDifficulty());
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("新增文章失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int deleteById(Long articleId) {
        String sql = "DELETE FROM article WHERE article_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, articleId);
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("删除文章失败: " + articleId, e);
        }
    }
}