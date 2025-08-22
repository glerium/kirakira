package com.kirakira.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kirakira.client.CodeforcesClient;
import com.kirakira.entity.GroupUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class GroupUserRepository {
    private final JdbcTemplate jdbcTemplate;

    public GroupUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * 获取指定群组中的所有 QQ 号对应的 Codeforces ID 列表
     * @param groupId 目标群组 ID
     * @return 该群组下所有 QQ 号对应的 Codeforces ID 映射
     */
    public boolean checkIfCodeforcesIdExists(String groupId, String codeforcesId) {
        String sql = "SELECT COUNT(*) FROM group_user WHERE group_id = ? AND codeforces_id = ?";
        Long queryResults = jdbcTemplate.queryForObject(sql, Long.class, groupId, codeforcesId);
        return queryResults != null && queryResults > 0;
    }

    /**
     * 检查 Codeforces ID 是否已绑定到该 QQ 号
     * @param groupId 群号
     * @param qqId QQ 号
     * @param codeforcesId Codeforces ID
     * @return 如果存在绑定，则返回 true，否则返回 false
     */
    public boolean checkIfBindingExists(String groupId, String qqId, String codeforcesId) {
        String sql = "SELECT COUNT(*) FROM group_user WHERE group_id = ? AND user_qq_id = ? AND codeforces_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, groupId, qqId, codeforcesId);
        return count != null && count > 0;
    }

    public List<String> enumerateAllCodeforcesId() {
        String sql = "SELECT DISTINCT codeforces_id FROM group_user";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    public List<String> enumerateGroupsByCodeforcesId(String cfId) {
        String sql = "SELECT DISTINCT group_id FROM group_user WHERE LOWER(codeforces_id) = LOWER(?)";
        return jdbcTemplate.queryForList(sql, String.class, cfId);
    }
    
    /**
     * 获取指定群组中的所有 Codeforces 用户 ID
     * @param groupId 目标群组 ID
     * @return 该群组下所有用户的 Codeforces ID 列表（可能为空列表）
     */
    public Map<String, List<String>> enumerateCodeforcesIdFromGroup(String groupId) {
        String sql = "SELECT user_qq_id, codeforces_id FROM group_user WHERE group_id = ?";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, groupId);

        Map<String, List<String>> userCodeforcesMap = new HashMap<>();
        for (Map<String, Object> row : results) {
            String qqId = row.get("user_qq_id").toString();
            String codeforcesId = row.get("codeforces_id").toString();

            userCodeforcesMap.computeIfAbsent(qqId, k -> new ArrayList<>()).add(codeforcesId);
        }

        return userCodeforcesMap;
    }

    public List<String> enumerateCodeforcesIdOfSingleUser(String groupId, String qqId) {
        String sql = "SELECT codeforces_id FROM group_user WHERE group_id = ? AND user_qq_id = ?";

        return jdbcTemplate.queryForList(sql, String.class, groupId, qqId);
    }

    public List<String> enumerateGroupList() {
        String sql = "SELECT DISTINCT group_id FROM group_user";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * 通过 Codeforces ID 查询关联的 QQ 号
     * @param codeforcesId 用户的 Codeforces ID
     * @return 对应的 QQ 号，未找到时返回 null
     */
    public String queryQqIdByCodeforcesId(String codeforcesId) {
        String sql = "SELECT user_qq_id FROM group_user WHERE codeforces_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, codeforcesId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * 添加群组用户
     * @param groupUser 用户对象
     * @return 插入是否成功（影响行数 > 0）
     */
    public boolean addGroupUser(GroupUser groupUser) {
        String sql = "INSERT INTO group_user (group_id, user_qq_id, codeforces_id) VALUES (?, ?, ?)";
        int affectedRows = jdbcTemplate.update(
            sql,
            groupUser.getGroupId(),
            groupUser.getUserQqId(),
            groupUser.getCodeforcesId()
        );
        return affectedRows > 0;
    }

    /**
     * 删除指定的 Codeforces 绑定
     * @param groupId 群号
     * @param codeforcesId Codeforces ID
     * @return 如果删除成功，返回 true；否则返回 false
     */
    public boolean removeGroupUser(String groupId, String codeforcesId) {
        String sql = "DELETE FROM group_user WHERE group_id = ? AND codeforces_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, groupId, codeforcesId);
        return rowsAffected > 0;
    }
}
