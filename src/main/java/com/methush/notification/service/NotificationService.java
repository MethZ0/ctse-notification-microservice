package com.methush.notification.service;

import com.methush.notification.dto.NotificationRequest;
import com.methush.notification.dto.NotificationResponse;
import com.methush.notification.model.Notification;
import com.methush.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.username:no-reply@example.com}")
    private String senderEmail;

    /**
     * Entry point: processes a notification by type.
     * Supported types:
     *   - "order_confirmation"   → sends an order placed email
     *   - "order_status_update"  → sends an order status changed email
     */
    public NotificationResponse processNotification(NotificationRequest request) {
        log.info("[NOTIFICATION] type={} orderId={} email={}", request.getType(), request.getOrderId(), request.getEmail());

        boolean emailSent = false;

        switch (request.getType()) {
            case "order_confirmation" -> {
                String subject = "✅ Order Confirmed — " + request.getOrderId();
                String htmlBody = emailTemplateService.buildOrderConfirmationHtml(request.getOrderId());
                emailSent = sendHtmlEmail(request.getEmail(), subject, htmlBody);
            }
            case "order_status_update" -> {
                String status = request.getOrderStatus() != null ? request.getOrderStatus() : "Updated";
                String subject = "📦 Order " + request.getOrderId() + " Status Updated to: " + status;
                String htmlBody = emailTemplateService.buildOrderStatusUpdateHtml(request.getOrderId(), status);
                emailSent = sendHtmlEmail(request.getEmail(), subject, htmlBody);
            }
            default -> log.warn("[NOTIFICATION] Unknown type '{}', saving without sending email.", request.getType());
        }

        Notification notification = Notification.builder()
                .type(request.getType())
                .email(request.getEmail())
                .orderId(request.getOrderId())
                .orderStatus(request.getOrderStatus())
                .timestamp(LocalDateTime.now())
                .isEmailSent(emailSent)
                .build();

        notificationRepository.save(notification);

        return NotificationResponse.builder()
                .success(true)
                .message(emailSent ? "Notification sent via email" : "Notification logged (email disabled or failed)")
                .orderId(request.getOrderId())
                .timestamp(notification.getTimestamp())
                .build();
    }

    /**
     * Fetch recent notifications ordered by timestamp descending.
     */
    public Page<Notification> getRecentNotifications(int page, int size) {
        return notificationRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
    }

    /**
     * Sends an HTML email using JavaMailSender.
     *
     * @return true if the email was sent successfully, false otherwise
     */
    private boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        if (!emailEnabled) {
            log.info("[EMAIL DISABLED] Would have sent '{}' to {}", subject, to);
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML
            mailSender.send(message);
            log.info("[EMAIL SENT] '{}' to {}", subject, to);
            return true;
        } catch (MessagingException e) {
            log.error("[EMAIL FAILED] Could not send '{}' to {}: {}", subject, to, e.getMessage());
            return false;
        }
    }
}

