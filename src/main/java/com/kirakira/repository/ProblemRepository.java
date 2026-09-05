package com.kirakira.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kirakira.entity.Problem;

@Repository
public class ProblemRepository {
    private final JdbcTemplate jdbcTemplate;

    public ProblemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean checkIfProblemExists(String problemId) {
        String sql = "SELECT COUNT(*) FROM problem WHERE problem_id = ?";
        var problemCount = jdbcTemplate.queryForObject(sql, Long.class, problemId);
        return problemCount > 0;
    }

    public boolean addProblem(Problem problem) {
        String sql = "INSERT INTO problem (problem_id, contest_id, rating) VALUES (?, ?, ?)";
        int affectedRows = jdbcTemplate.update(
            sql,
            problem.getProblemId(),
            problem.getContestId(),
            problem.getRating()
        );
        return affectedRows > 0;
    }

    public Problem queryProblemById(String problemId) {
        String sql = "SELECT * FROM problem WHERE problem_id = ?";
        var queryResults = jdbcTemplate.queryForObject(sql, Problem.class, problemId);
        return queryResults;
    }

    public Integer queryRatingById(String problemId) {
        var problem = queryProblemById(problemId);
        return problem != null ? problem.getRating() : null;
    }
}
