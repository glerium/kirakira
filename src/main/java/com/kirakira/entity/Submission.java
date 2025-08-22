package com.kirakira.entity;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Submission {
    private Integer id;             // 记录ID (数据库自增主键)
    private String codeforcesId;    // Codeforces账号ID
    private String problemId;       // 题目ID (格式如 "1234A")
    private String submissionId; 
    private LocalDateTime submissionTime;

    public void setCodeforcesId(String codeforcesId) {
        if (codeforcesId != null) {
            this.codeforcesId = codeforcesId.toLowerCase();
        } else {
            this.codeforcesId = null;
        }
    }
}