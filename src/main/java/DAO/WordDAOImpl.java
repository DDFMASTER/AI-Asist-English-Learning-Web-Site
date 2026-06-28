package DAO;

import Entities.AiWordDic;
import Entities.WordBase;
import Utils.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WordDAOImpl implements WordDAO {

    /** 学习阶段 → 数据库表名的白名单映射，防止 SQL 注入 */
    private static final Map<String, String> STAGE_TABLE = new LinkedHashMap<>();
    static {
        STAGE_TABLE.put("初中", "junior");
        STAGE_TABLE.put("高中", "senior");
        STAGE_TABLE.put("四级", "cet_4");
        STAGE_TABLE.put("六级", "cet_6");
        STAGE_TABLE.put("考研", "graduate");
        STAGE_TABLE.put("托福", "toefl");
    }

    @Override
    public List<WordBase> findByWordAndStage(String word, String stage) {
        String tableName = STAGE_TABLE.get(stage);
        if (tableName == null) {
            throw new IllegalArgumentException("无效的学习阶段: " + stage);
        }

        // 使用白名单确定的表名，安全拼接
        String sql = "SELECT id, word, phonetic, translation FROM " + tableName
                   + " WHERE word = ?";

        List<WordBase> results = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, word);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WordBase wb = new WordBase();
                    wb.setId(rs.getLong("id"));
                    wb.setWord(rs.getString("word"));
                    wb.setPhonetic(rs.getString("phonetic"));
                    wb.setTranslation(rs.getString("translation"));
                    results.add(wb);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查词失败: word=" + word + ", stage=" + stage, e);
        }
        return results;
    }

    @Override
    public Map<String, List<WordBase>> findByWordInAllTables(String word) {
        Map<String, List<WordBase>> result = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : STAGE_TABLE.entrySet()) {
            String stageLabel = entry.getKey();
            String tableName = entry.getValue();

            // 表名来自白名单，安全拼接
            String sql = "SELECT id, word, phonetic, translation FROM " + tableName
                       + " WHERE word = ?";

            List<WordBase> entries = new ArrayList<>();

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, word);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        WordBase wb = new WordBase();
                        wb.setId(rs.getLong("id"));
                        wb.setWord(rs.getString("word"));
                        wb.setPhonetic(rs.getString("phonetic"));
                        wb.setTranslation(rs.getString("translation"));
                        entries.add(wb);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("查词失败: word=" + word + ", table=" + tableName, e);
            }

            if (!entries.isEmpty()) {
                result.put(stageLabel, entries);
            }
        }

        return result;
    }

    @Override
    public AiWordDic findLatestAiDic(String word) {
        String sql = "SELECT aidic_id, word, phonetic, translation, explanation, " +
                     "like_count, dislike_count, is_removed, created_at " +
                     "FROM ai_word_dic " +
                     "WHERE word = ? AND is_removed = 0 " +
                     "ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, word);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAiWordDic(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询AI词典失败: " + word, e);
        }
        return null;
    }

    @Override
    public int insertAiDic(AiWordDic aiWordDic) {
        String sql = "INSERT INTO ai_word_dic (word, phonetic, translation, explanation, " +
                     "like_count, dislike_count, is_removed, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, aiWordDic.getWord());
            ps.setString(2, aiWordDic.getPhonetic());
            ps.setString(3, aiWordDic.getTranslation());
            ps.setString(4, aiWordDic.getExplanation());
            ps.setInt(5, aiWordDic.getLikeCount() != null ? aiWordDic.getLikeCount() : 0);
            ps.setInt(6, aiWordDic.getDislikeCount() != null ? aiWordDic.getDislikeCount() : 0);
            ps.setBoolean(7, aiWordDic.getIsRemoved() != null && aiWordDic.getIsRemoved());
            ps.setTimestamp(8, aiWordDic.getCreatedAt() != null
                    ? Timestamp.valueOf(aiWordDic.getCreatedAt())
                    : new Timestamp(System.currentTimeMillis()));

            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("插入AI词典条目失败", e);
        }
    }

    private AiWordDic mapAiWordDic(ResultSet rs) throws Exception {
        AiWordDic dic = new AiWordDic();
        dic.setAidicId(rs.getLong("aidic_id"));
        dic.setWord(rs.getString("word"));
        dic.setPhonetic(rs.getString("phonetic"));
        dic.setTranslation(rs.getString("translation"));
        dic.setExplanation(rs.getString("explanation"));
        dic.setLikeCount(rs.getInt("like_count"));
        dic.setDislikeCount(rs.getInt("dislike_count"));
        dic.setIsRemoved(rs.getBoolean("is_removed"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            dic.setCreatedAt(ts.toLocalDateTime());
        }
        return dic;
    }
}
