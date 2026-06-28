package DAO;

import Entities.Article;

import java.util.List;

public interface ArticleDAO {
    /**
     * 按难度查询文章列表
     * @param difficulty 文章难度筛选，为 null 或空时返回全部难度
     * @return 按 article_id 倒序排列的文章列表，最多 20 条
     */
    List<Article> findByDifficulty(String difficulty);
}