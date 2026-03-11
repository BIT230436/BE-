package com.example.be.controller;

import com.example.be.entity.Gps;
import com.example.be.repository.GpsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gps")
@CrossOrigin(origins = "*")
public class GpsController {

    private final GpsRepository gpsRepository;

    public GpsController(GpsRepository gpsRepository) {
        this.gpsRepository = gpsRepository;
    }

    // GET /api/gps - Lấy tất cả GPS
    @GetMapping
    public List<Gps> getAllGps() {
        return gpsRepository.findAll();
    }

    // POST /api/gps - Thêm GPS mới { gps, userId }
    @PostMapping
    public ResponseEntity<Gps> addGps(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String gpsValue = body.get("gps").toString();

        Gps gps = new Gps();
        gps.setUserId(userId);
        gps.setGps(gpsValue);
        gps.setActive(false);

        return ResponseEntity.ok(gpsRepository.save(gps));
    }

    // PUT /api/gps - Cập nhật GPS { gpsId, newGps, userId }
    @PutMapping
    public ResponseEntity<?> updateGps(@RequestBody Map<String, Object> body) {
        Long gpsId = Long.valueOf(body.get("gpsId").toString());
        String newGpsValue = body.get("newGps").toString();

        return gpsRepository.findById(gpsId)
                .map(gps -> {
                    gps.setGps(newGpsValue);
                    return ResponseEntity.ok(gpsRepository.save(gps));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/gps/activate/{userId}/{gpsId} - Kích hoạt GPS
    @PutMapping("/activate/{userId}/{gpsId}")
    public ResponseEntity<?> activateGps(@PathVariable Long userId, @PathVariable Long gpsId) {
        // Deactivate all GPS for this user first
        List<Gps> userGpsList = gpsRepository.findByUserId(userId);
        for (Gps g : userGpsList) {
            g.setActive(false);
        }
        gpsRepository.saveAll(userGpsList);

        // Activate the selected GPS
        return gpsRepository.findById(gpsId)
                .map(gps -> {
                    gps.setActive(true);
                    return ResponseEntity.ok(gpsRepository.save(gps));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/gps/{userId}/{gpsId} - Xóa GPS
    @DeleteMapping("/{userId}/{gpsId}")
    public ResponseEntity<?> deleteGps(@PathVariable Long userId, @PathVariable Long gpsId) {
        if (!gpsRepository.existsById(gpsId)) {
            return ResponseEntity.notFound().build();
        }
        gpsRepository.deleteById(gpsId);
        return ResponseEntity.ok().build();
    }
}
