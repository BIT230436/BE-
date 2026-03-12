package com.example.be.service;

import com.example.be.entity.InternProfile;
import com.example.be.entity.User;
import com.example.be.repository.InternProfileRepository;
import com.example.be.repository.InternRepository;
import com.example.be.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class InternContextService {

    private final InternRepository internRepository;
    private final UserRepository userRepository;
    private final InternProfileRepository internProfileRepository;
    private final JdbcTemplate jdbcTemplate;

    public InternContextService(InternRepository internRepository,
                                UserRepository userRepository,
                                InternProfileRepository internProfileRepository,
                                JdbcTemplate jdbcTemplate) {
        this.internRepository = internRepository;
        this.userRepository = userRepository;
        this.internProfileRepository = internProfileRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ✅ Lấy internId từ userId
    // Thử findByUser_Id trước; nếu chưa liên kết thì fallback theo email và tự link lại.
    public Long getInternIdFromUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với id = " + userId));

        // 1. Thử tìm qua user_id trực tiếp
        var byUserId = internProfileRepository.findByUser_Id(userId);
        if (byUserId.isPresent()) {
            return byUserId.get().getId();
        }

        // 2. Fallback: tìm theo email rồi tự link user_id để các lần sau không cần fallback nữa
        String email = user.getEmail();
        if (email == null) return null;

        var byEmail = internProfileRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            InternProfile profile = byEmail.get();
            // Gán user_id cho profile để link lại
            try {
                jdbcTemplate.update(
                        "UPDATE intern_profiles SET user_id = ? WHERE intern_id = ?",
                        userId, profile.getId());
            } catch (Exception e) {
                // Bỏ qua nếu update thất bại; vẫn trả về internId để tiếp tục
            }
            return profile.getId();
        }

        return null;
    }
}
