package com.example.be.repository;

import com.example.be.entity.Gps;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GpsRepository extends JpaRepository<Gps, Long> {
    List<Gps> findByUserId(Long userId);
}
