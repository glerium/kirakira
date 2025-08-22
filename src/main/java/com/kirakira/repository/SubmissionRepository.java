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
    
    public boolean checkIfSubmissionExists(String submissionId) {
        String sql = "SELECT COUNT(*) FROM submission WHERE submission_id = ?";
        var count = jdbcTemplate.queryForObject(sql, Long.class, submissionId);
        return count > 0;
    }
    
    public boolean checkIfUserFinishedProblem(String problemId, String codeforcesId) {
        String sql = "SELECT COUNT(*) FROM submission WHERE problem_id = ? AND codeforces_id = ?";
        var count = jdbcTemplate.queryForObject(sql, Long.class, problemId, codeforcesId.toLowerCase());
        // System.out.println(problemId + " " + codeforcesId + " " + count);
        return count > 0;
    }

    public boolean insertSubmission(Submission submission) {
        String sql = "INSERT INTO submission (codeforces_id, problem_id, submission_id, submission_time) VALUES (?, ?, ?, ?)";
        var affectedRows = jdbcTemplate.update(
            sql,
            submission.getCodeforcesId(),
            submission.getProblemId(),
            submission.getSubmissionId(),
            submission.getSubmissionTime()
        );
        return affectedRows > 0;
    }
}
