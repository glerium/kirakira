DROP TABLE IF EXISTS group_user;
DROP TABLE IF EXISTS submission;
DROP TABLE IF EXISTS problem;

-- 群对应用户表
CREATE TABLE group_user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    group_id VARCHAR(50),
    user_qq_id VARCHAR(50),
    codeforces_id VARCHAR(50)
);

-- 用户过题表
CREATE TABLE submission (
    id INT PRIMARY KEY AUTO_INCREMENT,
    codeforces_id VARCHAR(50),
    problem_id VARCHAR(20),
    submission_id VARCHAR(20)
);

-- 题目表
CREATE TABLE problem (
    id INT PRIMARY KEY AUTO_INCREMENT,
    problem_id VARCHAR(20) UNIQUE,
    contest_id VARCHAR(20),
    rating INT
);