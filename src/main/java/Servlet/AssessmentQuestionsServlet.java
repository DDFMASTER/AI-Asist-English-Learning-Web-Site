package Servlet;

import Service.AIService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 轮询获取已生成的测评题目。
 * GET /api/assessment/questions?sessionId=xxx
 * 返回当前已生成的所有题目，以及是否已全部完成。
 */
@WebServlet("/api/assessment/questions")
public class AssessmentQuestionsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        String sessionId = request.getParameter("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"缺少 sessionId\"}");
            return;
        }

        List<AIService.AssessmentQuestion> questions =
                AssessmentGenerateServlet.SESSIONS.get(sessionId);

        if (questions == null) {
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"会话不存在或已过期\"}");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true");
        sb.append(",\"sessionId\":\"").append(AssessmentGenerateServlet.esc(sessionId)).append("\"");

        // 线程安全地复制列表
        List<AIService.AssessmentQuestion> snapshot;
        synchronized (questions) {
            snapshot = List.copyOf(questions);
        }

        sb.append(",\"totalTarget\":10");
        sb.append(",\"count\":").append(snapshot.size());
        sb.append(",\"complete\":").append(snapshot.size() >= 10);
        sb.append(",\"questions\":[");
        for (int i = 0; i < snapshot.size(); i++) {
            if (i > 0) sb.append(",");
            AssessmentGenerateServlet.appendQuestionJson(sb, snapshot.get(i));
        }
        sb.append("]}");
        response.getWriter().write(sb.toString());
    }
}
