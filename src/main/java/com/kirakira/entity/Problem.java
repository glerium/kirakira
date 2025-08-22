package com.kirakira.entity;

import lombok.Data;

@Data
public class Problem {
    private Integer id;          // 记录ID (数据库自增主键)
    private String problemId; // 题目唯一标识 (如 "1234A")
    private String contestId; // 所属比赛ID (如 "1234")
    private Integer rating;   // 题目难度评分
}
