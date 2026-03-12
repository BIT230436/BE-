package com.example.be.controller;

import com.example.be.entity.WorkSchedule;
import com.example.be.repository.DepartmentRepository;
import com.example.be.repository.WorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final WorkScheduleRepository workScheduleRepository;
    private final DepartmentRepository departmentRepository;

    // ============ WORK SCHEDULES ============

    @GetMapping("/api/work-schedules")
    public ResponseEntity<?> getWorkSchedules(
            @RequestParam(required = false) Long groupId) {

        List<WorkSchedule> schedules = (groupId != null)
                ? workScheduleRepository.findByGroupId(groupId)
                : workScheduleRepository.findAll();

        List<Map<String, Object>> result = schedules.stream().map(s -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", s.getId());
            map.put("applyType", s.getApplyType());
            map.put("groupId", s.getGroupId());
            map.put("groupName", s.getGroupName());
            map.put("internId", s.getInternId());
            map.put("internName", s.getInternName());
            map.put("startTime", s.getStartTime());
            map.put("endTime", s.getEndTime());
            map.put("daysPerWeek", s.getDaysPerWeek());
            map.put("workDays", s.getWorkDays() != null
                    ? Arrays.asList(s.getWorkDays().split(","))
                    : List.of());
            map.put("note", s.getNote());
            map.put("createdAt", s.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/work-schedules")
    public ResponseEntity<?> createWorkSchedule(@RequestBody Map<String, Object> body) {
        WorkSchedule schedule = buildSchedule(null, body);
        workScheduleRepository.save(schedule);
        return ResponseEntity.ok(Map.of("success", true, "data", schedule.getId()));
    }

    @PutMapping("/api/work-schedules/{id}")
    public ResponseEntity<?> updateWorkSchedule(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        WorkSchedule existing = workScheduleRepository.findById(id)
                .orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        buildSchedule(existing, body);
        workScheduleRepository.save(existing);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/api/work-schedules/{id}")
    public ResponseEntity<?> deleteWorkSchedule(@PathVariable Long id) {
        if (!workScheduleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        workScheduleRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ============ INTERN GROUPS (backed by departments) ============

    @GetMapping("/api/intern-groups")
    public ResponseEntity<?> getInternGroups() {
        List<Map<String, Object>> groups = departmentRepository.findAll().stream()
                .map(d -> Map.<String, Object>of(
                        "id", d.getId(),
                        "name", d.getNameDepartment()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(groups);
    }

    // ============ HELPER ============

    private WorkSchedule buildSchedule(WorkSchedule target, Map<String, Object> body) {
        final WorkSchedule schedule = (target != null) ? target : new WorkSchedule();

        schedule.setApplyType(str(body.get("applyType")));
        schedule.setStartTime(str(body.get("startTime")));
        schedule.setEndTime(str(body.get("endTime")));
        schedule.setNote(str(body.get("note")));

        Object dp = body.get("daysPerWeek");
        if (dp != null) schedule.setDaysPerWeek(((Number) dp).intValue());

        Object workDays = body.get("workDays");
        if (workDays instanceof List<?>) {
            schedule.setWorkDays(((List<?>) workDays).stream()
                    .map(Object::toString).collect(Collectors.joining(",")));
        } else if (workDays instanceof String) {
            schedule.setWorkDays((String) workDays);
        }

        Object groupIdObj = body.get("groupId");
        if (groupIdObj != null) {
            final Long groupId = ((Number) groupIdObj).longValue();
            schedule.setGroupId(groupId);
            departmentRepository.findById(groupId)
                    .ifPresent(d -> schedule.setGroupName(d.getNameDepartment()));
        } else {
            schedule.setGroupId(null);
            schedule.setGroupName(null);
        }

        Object internIdObj = body.get("internId");
        if (internIdObj != null) {
            schedule.setInternId(((Number) internIdObj).longValue());
            schedule.setInternName(str(body.get("internName")));
        } else {
            schedule.setInternId(null);
            schedule.setInternName(null);
        }

        return schedule;
    }

    private String str(Object val) {
        return val != null ? val.toString() : null;
    }
}
