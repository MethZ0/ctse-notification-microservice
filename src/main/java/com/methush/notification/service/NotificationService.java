package com.methush.notification.service;

import com.methush.notification.dto.NotificationRequest;
import com.methush.notification.model.Notification;
import com.methush.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    /**
     * Processes a notification request: saves to MongoDB, logs it, and optionally sends an email.
     */
    public Notification sendNotification(NotificationRequest request) {
        log.info("[NOTIFICATION] To user {}: {} (Order: {})", request.getUserId(), request.getMessage(), request.getOrderId());
        
        boolean emailSent = false;
        if (emailEnabled && isValidEmail(request.getUserId())) {
            emailSent = sendEmail(request.getUserId(), "Order Notification: " + request.getOrderId(), request.getMessage());
        }

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .orderId(request.getOrderId())
                .message(request.getMessage())
                .timestamp(LocalDateTime.now())
                .isEmailSent(emailSent)
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Fetch recent notifications ordered by timestamp descending.
     */
    public Page<Notification> getRecentNotifications(int page, int size) {
        return notificationRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
    }

    private boolean sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.debug("Email successfully sent to {}", to);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            return false;
        }
    }
    
    // Very basic email valid check, assuming userId could be an email
    private boolean isValidEmail(String email) {
        return email != null && email.contains("@");
    }
}
