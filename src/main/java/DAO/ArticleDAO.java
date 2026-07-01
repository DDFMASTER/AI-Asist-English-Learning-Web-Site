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

    // ========== 管理员方法 ==========

    /**
     * 根据 ID 查询文章完整信息（含 content）
     * @param articleId 文章ID
     * @return 文章对象，不存在返回 null
     */
    Article findById(Long articleId);

    /**
     * 分页查询文章列表（不含 content，按 article_id 倒序）
     * @param offset 偏移量
     * @param limit  每页条数
     * @return 文章列表
     */
    List<Article> findAllPaginated(int offset, int limit);

    /**
     * 查询文章总数
     * @return 文章总数
     */
    int countAll();

    /**
     * 新增文章
     * @param article 文章对象（title, content, source, difficulty）
     * @return 影响行数
     */
    int insert(Article article);

    /**
     * 删除文章
     * @param articleId 文章ID
     * @return 影响行数
     */
    int deleteById(Long articleId);
}