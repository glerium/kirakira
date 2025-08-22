package com.kirakira.mapper;

import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.kirakira.entity.Problem;

public class ProblemRowMapper implements RowMapper<Problem> {
    @Override
    public Problem mapRow(ResultSet rs, int rowNum) throws SQLException {
        Problem problem = new Problem();
        problem.setId(rs.getInt("id"));
        problem.setProblemId(rs.getString("problem_id"));
        problem.setContestId(rs.getString("contest_id"));
        problem.setRating(rs.getInt("rating"));
        return problem;
    }
}