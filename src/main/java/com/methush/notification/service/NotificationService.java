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
     * Entry point — routes the notification by type:
     *   - "order_confirmation"   → fetches order details, sends rich confirmation email
     *   - "order_status_update"  → fetches order details, sends status update email
     *   - "LOW_STOCK"            → fetches all admin emails, sends low stock alert
     */
    public NotificationResponse processNotification(NotificationRequest request, String authHeader) {
        log.info("[NOTIFICATION] type={} orderId={} email={}",
                request.getType(), request.getOrderId(), request.getEmail());

        boolean emailSent = false;
        String normalizedType = request.getType() != null ? request.getType().toLowerCase() : "";

        switch (normalizedType) {

            case "order_confirmation" -> {
                // Fetch full order details using the MongoDB _id passed as orderId
                Map<String, Object> order = fetchOrderDetails(request.getOrderId(), authHeader);

                // Use human-readable orderId (e.g. ORD#0003) from the order object if available
                String displayId = order != null && order.get("orderId") != null
                        ? (String) order.get("orderId")
                        : request.getOrderId();

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = order != null
                        ? (List<Map<String, Object>>) order.get("items")
                        : Collections.emptyList();

                double total = order != null && order.get("total") != null
                        ? ((Number) order.get("total")).doubleValue() : 0.0;

                String deliveryAddress = order != null
                        ? (String) order.get("deliveryAddress") : null;

                String subject = "✅ Order Confirmed — " + displayId;
                String htmlBody = emailTemplateService.buildOrderConfirmationHtml(
                        displayId, items, total, deliveryAddress);
                emailSent = sendHtmlEmail(request.getEmail(), subject, htmlBody);

                notificationRepository.save(Notification.builder()
                        .type(request.getType()).email(request.getEmail())
                        .orderId(displayId).timestamp(LocalDateTime.now())
                        .isEmailSent(emailSent).build());
            }

            case "order_status_update" -> {
                // Fetch full order details to get the human-readable orderId
                Map<String, Object> order = fetchOrderDetails(request.getOrderId(), authHeader);

                String displayId = order != null && order.get("orderId") != null
                        ? (String) order.get("orderId")
                        : request.getOrderId();

                double total = order != null && order.get("total") != null
                        ? ((Number) order.get("total")).doubleValue() : 0.0;

                String status = request.getOrderStatus() != null ? request.getOrderStatus() : "Updated";
                String subject = "📦 Order " + displayId + " Status Updated to: " + status;
                String htmlBody = emailTemplateService.buildOrderStatusUpdateHtml(displayId, status, total);
                emailSent = sendHtmlEmail(request.getEmail(), subject, htmlBody);

                notificationRepository.save(Notification.builder()
                        .type(request.getType()).email(request.getEmail())
                        .orderId(displayId).orderStatus(status)
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
                .message(emailSent ? "Notification sent via email" : "Notification logged (email disabled or no recipients)")
                .orderId(request.getOrderId() != null ? request.getOrderId() : request.getProductId())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Fetches order details from the order service via the API Gateway.
     * Uses the MongoDB _id received in the notification payload.
     *
     * @return Map containing order fields (orderId, items, total, status, etc.), or null on failure
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchOrderDetails(String mongoId, String authHeader) {
        if (mongoId == null || mongoId.isBlank()) return null;
        try {
            String url = gatewayUrl + "/orders/" + mongoId;
            log.info("[ORDER_FETCH] Fetching order details from: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isBlank()) {
                headers.set("Authorization", authHeader);
            }

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody().get("order");
            }
        } catch (Exception e) {
            log.error("[ORDER_FETCH] Failed to fetch order {}: {}", mongoId, e.getMessage());
        }
        return null;
    }

    /**
     * Fetches all admin emails by calling GET {gatewayUrl}/auth/adminUsers.
     */
    @SuppressWarnings("unchecked")
    private List<String> fetchAdminEmails(String authHeader) {
        try {
            String adminUrl = gatewayUrl + "/auth/adminUsers";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isBlank()) {
                headers.set("Authorization", authHeader);
            }

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    adminUrl, HttpMethod.GET,
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

    public Page<Notification> getRecentNotifications(int page, int size) {
        return notificationRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
    }

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
