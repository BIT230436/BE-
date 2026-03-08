package com.example.be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "apply_type", nullable = false)
    private String applyType; // "group" | "individual"

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "intern_id")
    private Long internId;

    @Column(name = "intern_name")
    private String internName;

    @Column(name = "start_time", nullable = false, length = 5)
    private String startTime; // "HH:mm"

    @Column(name = "end_time", nullable = false, length = 5)
    private String endTime; // "HH:mm"

    @Column(name = "days_per_week")
    private Integer daysPerWeek;

    @Column(name = "work_days", length = 100)
    private String workDays; // comma-separated: "MONDAY,TUESDAY,..."

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
