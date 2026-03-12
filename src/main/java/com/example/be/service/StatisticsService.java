package com.example.be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Lấy thống kê tổng quan về hoàn thành
     */
    public Map<String, Object> getInternCompletionStats() {
        String sql = """
                SELECT 
                    COUNT(*) as total,
                    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
                    SUM(CASE WHEN status != 'COMPLETED' OR status IS NULL THEN 1 ELSE 0 END) as notCompleted
                FROM intern_profiles
                WHERE status IS NOT NULL
                """;

        Map<String, Object> result = jdbcTemplate.queryForMap(sql);
        
        Long total = ((Number) result.get("total")).longValue();
        Long completed = ((Number) result.get("completed")).longValue();
        Long notCompleted = ((Number) result.get("notCompleted")).longValue();
        Double completionRate = total > 0 ? (completed * 100.0 / total) : 0.0;

        return Map.of(
                "total", total,
                "completed", completed,
                "notCompleted", notCompleted,
                "completionRate", Math.round(completionRate * 10) / 10.0
        );
    }

    /**
     * Lấy thống kê theo chương trình với filters
     */
    public List<Map<String, Object>> getProgramCompletionStats(Long programId, Long mentorId, String major) {
        StringBuilder sql = new StringBuilder("""
                SELECT 
                    COALESCE(prog.title, 'Chưa có chương trình') as programName,
                    COUNT(ip.intern_id) as total,
                    SUM(CASE WHEN ip.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
                    SUM(CASE WHEN ip.status = 'IN_PROGRESS' OR ip.status = 'active' THEN 1 ELSE 0 END) as inProgress,
                    SUM(CASE WHEN ip.status IS NULL OR ip.status NOT IN ('COMPLETED', 'IN_PROGRESS', 'active') THEN 1 ELSE 0 END) as notStarted
                FROM intern_profiles ip
                LEFT JOIN intern_programs prog ON ip.program_id = prog.program_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (programId != null) {
            sql.append(" AND ip.program_id = ?");
            params.add(programId);
        }

        if (mentorId != null) {
            sql.append(" AND prog.mentor_id = ?");
            params.add(mentorId);
        }

        if (major != null && !major.trim().isEmpty()) {
            sql.append(" AND ip.major_id = ?");
            params.add(Integer.parseInt(major));
        }

        sql.append(" GROUP BY prog.title ORDER BY total DESC");

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        // Tính completion rate cho mỗi program
        results.forEach(row -> {
            Long total = ((Number) row.get("total")).longValue();
            Long completed = ((Number) row.get("completed")).longValue();
            Double completionRate = total > 0 ? (completed * 100.0 / total) : 0.0;
            row.put("completionRate", Math.round(completionRate * 10) / 10.0);
        });

        return results;
    }

    /**
     * Lấy xu hướng hoàn thành theo thời gian (6 tháng gần nhất)
     */
    public List<Map<String, Object>> getProgressTimeline(Long programId, Long mentorId, String major) {
        // Tạo danh sách 6 tháng gần nhất
        List<Map<String, Object>> timeline = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String monthStr = month.format(formatter);

            StringBuilder sql = new StringBuilder("""
                    SELECT 
                        COUNT(*) as total,
                        SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed
                    FROM intern_profiles ip
                    LEFT JOIN intern_programs prog ON ip.program_id = prog.program_id
                    WHERE DATE_FORMAT(ip.available_from, '%Y-%m') <= ?
                    """);

            List<Object> params = new ArrayList<>();
            params.add(monthStr);

            if (programId != null) {
                sql.append(" AND ip.program_id = ?");
                params.add(programId);
            }

            if (mentorId != null) {
                sql.append(" AND prog.mentor_id = ?");
                params.add(mentorId);
            }

            if (major != null && !major.trim().isEmpty()) {
                sql.append(" AND ip.major_id = ?");
                params.add(Integer.parseInt(major));
            }

            try {
                Map<String, Object> result = jdbcTemplate.queryForMap(sql.toString(), params.toArray());
                Long total = ((Number) result.get("total")).longValue();
                Long completed = ((Number) result.get("completed")).longValue();
                Double completionRate = total > 0 ? (completed * 100.0 / total) : 0.0;

                timeline.add(Map.of(
                        "month", monthStr,
                        "completionRate", Math.round(completionRate * 10) / 10.0,
                        "totalInterns", total
                ));
            } catch (Exception e) {
                timeline.add(Map.of(
                        "month", monthStr,
                        "completionRate", 0.0,
                        "totalInterns", 0L
                ));
            }
        }

        return timeline;
    }

    /**
     * Lấy danh sách programs
     */
    public List<Map<String, Object>> getAllPrograms() {
        String sql = """
                SELECT DISTINCT prog.program_id as id, prog.title as name
                FROM intern_programs prog
                WHERE prog.title IS NOT NULL
                ORDER BY prog.title
                """;

        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Lấy danh sách mentors
     */
    public List<Map<String, Object>> getAllMentors() {
        String sql = """
                SELECT DISTINCT m.mentor_id as id, m.fullname as fullName
                FROM mentors m
                WHERE m.fullname IS NOT NULL
                ORDER BY m.fullname
                """;

        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Lấy danh sách majors
     */
    public List<Map<String, Object>> getAllMajors() {
        String sql = """
                SELECT major_id as id, name_major as name
                FROM majors
                WHERE name_major IS NOT NULL
                ORDER BY name_major
                """;

        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Thống kê số TTS theo trường đại học
     */
    public List<Map<String, Object>> getInternsBySchool(Long programId, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("""
                SELECT u.name_uni AS school, COUNT(ip.intern_id) AS count
                FROM intern_profiles ip
                LEFT JOIN universities u ON ip.uni_id = u.uni_id
                WHERE u.name_uni IS NOT NULL
                """);
        List<Object> params = new ArrayList<>();
        if (programId != null) { sql.append(" AND ip.program_id = ?"); params.add(programId); }
        if (startDate != null && !startDate.isEmpty()) { sql.append(" AND ip.available_from >= ?"); params.add(startDate); }
        if (endDate != null && !endDate.isEmpty()) { sql.append(" AND ip.available_from <= ?"); params.add(endDate); }
        sql.append(" GROUP BY u.name_uni ORDER BY count DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /**
     * Thống kê số TTS theo ngành học
     */
    public List<Map<String, Object>> getInternsByMajor(Long programId, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("""
                SELECT m.name_major AS major, COUNT(ip.intern_id) AS count
                FROM intern_profiles ip
                LEFT JOIN majors m ON ip.major_id = m.major_id
                WHERE m.name_major IS NOT NULL
                """);
        List<Object> params = new ArrayList<>();
        if (programId != null) { sql.append(" AND ip.program_id = ?"); params.add(programId); }
        if (startDate != null && !startDate.isEmpty()) { sql.append(" AND ip.available_from >= ?"); params.add(startDate); }
        if (endDate != null && !endDate.isEmpty()) { sql.append(" AND ip.available_from <= ?"); params.add(endDate); }
        sql.append(" GROUP BY m.name_major ORDER BY count DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /**
     * Thống kê timeline tuyển dụng theo tháng
     */
    public List<Map<String, Object>> getRecruitmentTimeline(Long programId, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("""
                SELECT DATE_FORMAT(ip.available_from, '%Y-%m') AS month, COUNT(ip.intern_id) AS count
                FROM intern_profiles ip
                WHERE ip.available_from IS NOT NULL
                """);
        List<Object> params = new ArrayList<>();
        if (programId != null) { sql.append(" AND ip.program_id = ?"); params.add(programId); }
        if (startDate != null && !startDate.isEmpty()) { sql.append(" AND ip.available_from >= ?"); params.add(startDate); }
        if (endDate != null && !endDate.isEmpty()) { sql.append(" AND ip.available_from <= ?"); params.add(endDate); }
        sql.append(" GROUP BY month ORDER BY month");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }
}