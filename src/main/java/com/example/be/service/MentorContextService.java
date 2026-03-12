package com.example.be.service;

import com.example.be.entity.Mentors;
import com.example.be.entity.User;
import com.example.be.repository.MentorRepository;
import com.example.be.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class MentorContextService {

    private final MentorRepository mentorRepository;
    private final UserRepository userRepository;

    public MentorContextService(MentorRepository mentorRepository, UserRepository userRepository) {
        this.mentorRepository = mentorRepository;
        this.userRepository = userRepository;
    }

    // ✅ Lấy mentorId từ userId
    public Long getMentorIdFromUserId(Long userId) {
        // Kiểm tra user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với id = " + userId));

        // Tìm mentor tương ứng với user
        return mentorRepository.findByUser_Id(userId)
            .map(Mentors::getId)
            .orElseGet(() -> createMissingMentorProfile(user).getId());
        }

        private Mentors createMissingMentorProfile(User user) {
        if (user.getRole() == null || user.getRole().getName() == null) {
            throw new RuntimeException("User chưa có role để xác định mentor profile");
        }

        if (!"MENTOR".equalsIgnoreCase(user.getRole().getName())) {
            throw new RuntimeException("User không phải mentor: " + user.getId());
        }

        Mentors mentor = Mentors.builder()
            .user(user)
            .fullName(user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName()
                : user.getEmail())
            .build();

        return mentorRepository.save(mentor);
    }
}
