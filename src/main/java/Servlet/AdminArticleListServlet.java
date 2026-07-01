package Servlet;

import DAO.ArticleDAO;
import DAO.ArticleDAOImpl;
import Entities.Article;
import Service.AdminService;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 管理员 — 分页查询文章列表
 *
 * GET /api/admin/article/list
 * 参数: adminUserId (Long) — 管理员ID
 *       page       (int)  — 页码，从 1 开始，默认 1
 *       pageSize   (int)  — 每页条数，默认 10，最大 50
 *
 * 响应:
 *   { success, articles, total, page, pageSize }
 */
@WebServlet("/api/admin/article/list")
public class AdminArticleListServlet extends HttpServlet {
    private final AdminService adminService = new AdminService();
    private final ArticleDAO articleDAO = new ArticleDAOImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        // 管理员身份校验
        Long adminUserId = parseLong(request.getParameter("adminUserId"));
        if (!adminService.isAdmin(adminUserId)) {
            response.getWriter().write(JsonUtil.error("无管理员权限"));
            return;
        }

        // 解析分页参数
        int page = 1;
        int pageSize = 10;
        String pageParam = request.getParameter("page");
        if (pageParam != null && !pageParam.isBlank()) {
            try { page = Integer.parseInt(pageParam); } catch (NumberFormatException ignored) {}
        }
        String sizeParam = request.getParameter("pageSize");
        if (sizeParam != null && !sizeParam.isBlank()) {
            try {
                pageSize = Integer.parseInt(sizeParam);
                if (pageSize < 1) pageSize = 1;
                if (pageSize > 50) pageSize = 50;
            } catch (NumberFormatException ignored) {}
        }
        if (page < 1) page = 1;

        int offset = (page - 1) * pageSize;
        int total = articleDAO.countAll();
        List<Article> articles = articleDAO.findAllPaginated(offset, pageSize);

        // 构建 JSON
        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,");
        json.append("\"total\":").append(total).append(",");
        json.append("\"page\":").append(page).append(",");
        json.append("\"pageSize\":").append(pageSize).append(",");
        json.append("\"articles\":[");
        boolean first = true;
        for (Article a : articles) {
            if (!first) json.append(",");
            first = false;
            json.append("{");
            json.append("\"articleId\":").append(JsonUtil.numVal(a.getArticleId())).append(",");
            json.append("\"title\":").append(JsonUtil.strVal(a.getTitle())).append(",");
            json.append("\"source\":").append(JsonUtil.strVal(a.getSource())).append(",");
            json.append("\"difficulty\":").append(JsonUtil.strVal(a.getDifficulty())).append(",");
            json.append("\"articleLikeCount\":").append(JsonUtil.numVal(a.getArticleLikeCount())).append(",");
            json.append("\"vocquizNum\":").append(JsonUtil.numVal(a.getVocquizNum())).append(",");
            json.append("\"comquizNum\":").append(JsonUtil.numVal(a.getComquizNum()));
            json.append("}");
        }
        json.append("]}");
        response.getWriter().write(json.toString());
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }
}
