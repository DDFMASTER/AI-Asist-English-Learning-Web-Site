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
}