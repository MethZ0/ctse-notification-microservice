package com.methush.notification.service;

import com.methush.notification.dto.NotificationRequest;
import com.methush.notification.dto.NotificationResponse;
import com.methush.notification.model.Notification;
import com.methush.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;
    private final RestTemplate restTemplate;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.username:no-reply@example.com}")
    private String senderEmail;

    @Value("${gateway.url:http://localhost:8000}")
    private String gatewayUrl;

    /**
     * Entry point: processes a notification by type.
     * Supported types:
     *   - "order_confirmation"   → sends an order placed email to the customer
     *   - "order_status_update"  → sends a status changed email to the customer
     *   - "LOW_STOCK"            → sends a low stock warning email to ALL admin users
     *
     * @param request    the notification payload
     * @param authHeader the Authorization header from the inbound request (forwarded to gateway)
     */
    public NotificationResponse processNotification(NotificationRequest request, String authHeader) {
        log.info("[NOTIFICATION] type={} orderId={} email={} productId={}",
                request.getType(), request.getOrderId(), request.getEmail(), request.getProductId());

        boolean emailSent = false;
        String normalizedType = request.getType() != null ? request.getType().toLowerCase() : "";

        switch (normalizedType) {

            case "order_confirmation" -> {
                String subject = "✅ Order Confirmed — " + request.getOrderId();
                String htmlBody = emailTemplateService.buildOrderConfirmationHtml(request.getOrderId());
                emailSent = sendHtmlEmail(request.getEmail(), subject, htmlBody);

                notificationRepository.save(Notification.builder()
                        .type(request.getType()).email(request.getEmail())
                        .orderId(request.getOrderId()).timestamp(LocalDateTime.now())
                        .isEmailSent(emailSent).build());
            }

            case "order_status_update" -> {
                String status = request.getOrderStatus() != null ? request.getOrderStatus() : "Updated";
                String subject = "📦 Order " + request.getOrderId() + " Status Updated to: " + status;
                String htmlBody = emailTemplateService.buildOrderStatusUpdateHtml(request.getOrderId(), status);
                emailSent = sendHtmlEmail(request.getEmail(), subject, htmlBody);

                notificationRepository.save(Notification.builder()
                        .type(request.getType()).email(request.getEmail())
                        .orderId(request.getOrderId()).orderStatus(status)
                        .timestamp(LocalDateTime.now()).isEmailSent(emailSent).build());
            }

            case "low_stock" -> {
                String alertMessage = request.getMessage() != null
                        ? request.getMessage()
                        : "A product has dropped below the low stock threshold.";
                String productId = request.getProductId();

                List<String> adminEmails = fetchAdminEmails(authHeader);
                log.info("[LOW_STOCK] Found {} admin(s) to notify. productId={}", adminEmails.size(), productId);

                for (String adminEmail : adminEmails) {
                    String subject = "⚠️ Low Stock Alert — Action Required";
                    String htmlBody = emailTemplateService.buildLowStockAlertHtml(alertMessage, productId);
                    boolean sent = sendHtmlEmail(adminEmail, subject, htmlBody);
                    emailSent = emailSent || sent;

                    notificationRepository.save(Notification.builder()
                            .type("LOW_STOCK").email(adminEmail)
                            .orderId(productId).orderStatus(alertMessage)
                            .timestamp(LocalDateTime.now()).isEmailSent(sent).build());
                }
            }

            default -> log.warn("[NOTIFICATION] Unknown type '{}', ignoring.", request.getType());
        }

        return NotificationResponse.builder()
                .success(true)
                .message(emailSent
                        ? "Notification sent via email"
                        : "Notification logged (email disabled or no admins found)")
                .orderId(request.getOrderId() != null ? request.getOrderId() : request.getProductId())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Fetches all admin emails by calling GET {gatewayUrl}/auth/adminUsers.
     * Forwards the original Authorization header so the auth service accepts the call.
     */
    @SuppressWarnings("unchecked")
    private List<String> fetchAdminEmails(String authHeader) {
        try {
            String adminUrl = gatewayUrl + "/auth/adminUsers";
            log.info("[LOW_STOCK] Fetching admins from: {}", adminUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isBlank()) {
                headers.set("Authorization", authHeader);
            }

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    adminUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> adminUsers =
                        (List<Map<String, Object>>) response.getBody().get("adminUsers");
                if (adminUsers != null) {
                    return adminUsers.stream()
                            .map(u -> (String) u.get("email"))
                            .filter(e -> e != null && !e.isBlank())
                            .toList();
                }
            }
        } catch (Exception e) {
            log.error("[LOW_STOCK] Failed to fetch admin users: {}", e.getMessage());
        }
        return Collections.emptyList();
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
     * @return true if sent successfully, false otherwise
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
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("[EMAIL SENT] '{}' to {}", subject, to);
            return true;
        } catch (MessagingException e) {
            log.error("[EMAIL FAILED] Could not send '{}' to {}: {}", subject, to, e.getMessage());
            return false;
        }
    }
}
