package com.example.be.controller;

import com.example.be.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Lấy thống kê tổng quan về hoàn thành chương trình
     */
    @GetMapping("/intern-completion")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> getInternCompletionStats() {
        try {
            Map<String, Object> stats = statisticsService.getInternCompletionStats();
            return ResponseEntity.ok(Map.of("success", true, "data", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy thống kê: " + e.getMessage()
            ));
        }
    }

    /**
     * Lấy thống kê theo chương trình với filters
     */
    @GetMapping("/program-completion")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> getProgramCompletionStats(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long mentorId,
            @RequestParam(required = false) String major) {
        try {
            var stats = statisticsService.getProgramCompletionStats(programId, mentorId, major);
            return ResponseEntity.ok(Map.of("success", true, "data", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy thống kê chương trình: " + e.getMessage()
            ));
        }
    }

    /**
     * Lấy xu hướng hoàn thành theo thời gian
     */
    @GetMapping("/progress-timeline")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> getProgressTimeline(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long mentorId,
            @RequestParam(required = false) String major) {
        try {
            var timeline = statisticsService.getProgressTimeline(programId, mentorId, major);
            return ResponseEntity.ok(Map.of("success", true, "data", timeline));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy xu hướng: " + e.getMessage()
            ));
        }
    }

    /**
     * Lấy danh sách programs cho filter
     */
    @GetMapping("/programs")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> getAllPrograms() {
        try {
            var programs = statisticsService.getAllPrograms();
            return ResponseEntity.ok(Map.of("success", true, "data", programs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách chương trình: " + e.getMessage()
            ));
        }
    }

    /**
     * Lấy danh sách mentors cho filter
     */
    @GetMapping("/mentors")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> getAllMentors() {
        try {
            var mentors = statisticsService.getAllMentors();
            return ResponseEntity.ok(Map.of("success", true, "data", mentors));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách mentor: " + e.getMessage()
            ));
        }
    }

    /**
     * Lấy danh sách majors cho filter
     */
    @GetMapping("/majors")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> getAllMajors() {
        try {
            var majors = statisticsService.getAllMajors();
            return ResponseEntity.ok(Map.of("success", true, "data", majors));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách chuyên ngành: " + e.getMessage()
            ));
        }
    }

    /**
     * Thống kê số TTS theo trường đại học
     */
    @GetMapping("/candidate-sources/schools")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> getInternsBySchool(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            var data = statisticsService.getInternsBySchool(programId, startDate, endDate);
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Thống kê số TTS theo ngành học
     */
    @GetMapping("/candidate-sources/majors")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> getInternsByMajor(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            var data = statisticsService.getInternsByMajor(programId, startDate, endDate);
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Timeline tuyển dụng theo tháng
     */
    @GetMapping("/candidate-sources/timeline")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> getRecruitmentTimeline(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            var data = statisticsService.getRecruitmentTimeline(programId, startDate, endDate);
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}