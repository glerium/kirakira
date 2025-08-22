package com.kirakira.mapper;

import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import com.kirakira.entity.Submission;

public class SubmissionRowMapper implements RowMapper<Submission> {
    @Override
    public Submission mapRow(ResultSet rs, int rowNum) throws SQLException {
        Submission submission = Submission.builder()
            .id(rs.getInt("id"))
            .codeforcesId(rs.getString("codeforces_id"))
            .problemId(rs.getString("problem_id"))
            .submissionId(rs.getString("submission_id"))
            .submissionTime(rs.getObject("submission_time", LocalDateTime.class))
            .build();
        return submission;
    }
}