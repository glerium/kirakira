package com.kirakira.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kirakira.entity.Submission;

@Repository
public class SubmissionRepository {
    private final JdbcTemplate jdbcTemplate;

    public SubmissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * 检查提交是否已存在
     * @param submissionId 提交 ID
     * @return 如果存在返回 true，否则返回 false
     */
    public boolean checkIfSubmissionExists(String submissionId) {
        String sql = "SELECT COUNT(*) FROM submission WHERE submission_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, submissionId);
        return count != null && count > 0;
    }
    
    /**
     * 检查用户是否已完成该题目
     * @param problemId 题目 ID
     * @param codeforcesId Codeforces 用户 ID
     * @return 如果已完成返回 true，否则返回 false
     */
    public boolean checkIfUserFinishedProblem(String problemId, String codeforcesId) {
        String sql = "SELECT COUNT(*) FROM submission WHERE problem_id = ? AND codeforces_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, problemId, codeforcesId.toLowerCase());
        return count != null && count > 0;
    }

    /**
     * 插入新的提交记录
     * @param submission 提交对象
     * @return 如果插入成功返回 true，否则返回 false
     */
    public boolean insertSubmission(Submission submission) {
        String sql = "INSERT INTO submission (codeforces_id, problem_id, submission_id, submission_time) VALUES (?, ?, ?, ?)";
        int affectedRows = jdbcTemplate.update(
            sql,
            submission.getCodeforcesId(),
            submission.getProblemId(),
            submission.getSubmissionId(),
            submission.getSubmissionTime()
        );
        return affectedRows > 0;
    }
}
