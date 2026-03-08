package com.example.be.notification.service;

import com.example.be.notification.entity.Notification;
import com.example.be.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topic;
    private final NotificationRepository repository;
    private final SseEmitterService sseEmitterService;

    /**
     * Gửi notification: Lưu DB + Publish Redis (fallback SSE trực tiếp nếu Redis down)
     */
    @Transactional
    public void publish(String userId, String type, String title, String message) {
        try {
            // 1. Lưu vào DB trước (để có log)
            Notification notification = repository.save(Notification.builder()
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .message(message)
                    .build());

            // 2. Push realtime qua Redis
            try {
                redisTemplate.convertAndSend(topic.getTopic(), notification);
                log.info("✅ Notification sent via Redis: userId={}, type={}", userId, type);
            } catch (Exception redisEx) {
                log.warn("⚠️ Redis unavailable, falling back to direct SSE: {}", redisEx.getMessage());
                // Fallback: push trực tiếp qua SSE mà không cần Redis
                sseEmitterService.sendToUser(userId, notification);
                log.info("✅ Notification sent via SSE fallback: userId={}, type={}", userId, type);
            }

        } catch (Exception e) {
            log.error("❌ Failed to publish notification: {}", e.getMessage());
            // Không throw exception để không ảnh hưởng business logic
        }
    }
}
