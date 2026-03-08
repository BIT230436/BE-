-- Run this once in your MySQL database to create the work_schedules table
CREATE TABLE IF NOT EXISTS work_schedules (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    apply_type VARCHAR(20)  NOT NULL,
    group_id   BIGINT       NULL,
    group_name VARCHAR(255) NULL,
    intern_id  BIGINT       NULL,
    intern_name VARCHAR(255) NULL,
    start_time VARCHAR(5)   NOT NULL,
    end_time   VARCHAR(5)   NOT NULL,
    days_per_week INT       NULL,
    work_days  VARCHAR(100) NULL,
    note       VARCHAR(500) NULL,
    created_at DATETIME     NULL
);
