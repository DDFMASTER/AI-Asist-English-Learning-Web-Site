package Service;

import DAO.ArticleDAO;
import DAO.ArticleDAOImpl;
import Entities.Article;

import java.util.List;

public class ArticleService {
    private final ArticleDAO articleDAO = new ArticleDAOImpl();

    /**
     * 获取文章列表
     * @param difficulty 难度筛选，为 null 或空时返回全部难度
     * @return 按 article_id 倒序排列的文章列表，最多 20 条
     */
    public List<Article> getArticleList(String difficulty) {
        return articleDAO.findByDifficulty(difficulty);
    }
}