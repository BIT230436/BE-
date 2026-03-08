package com.example.be.repository;

import com.example.be.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {
    List<WorkSchedule> findByGroupId(Long groupId);
}
