package com.example.be.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "gps")
public class Gps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gps_id")
    private Long gpsId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "gps", nullable = false)
    private String gps;

    @Column(name = "active")
    private Boolean active = false;

    public Gps() {}

    public Long getGpsId() {
        return gpsId;
    }

    public void setGpsId(Long gpsId) {
        this.gpsId = gpsId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getGps() {
        return gps;
    }

    public void setGps(String gps) {
        this.gps = gps;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
