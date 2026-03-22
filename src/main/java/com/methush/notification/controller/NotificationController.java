package com.methush.notification.controller;

import com.methush.notification.dto.NotificationRequest;
import com.methush.notification.dto.NotificationResponse;
import com.methush.notification.model.Notification;
import com.methush.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints for sending and managing notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Primary endpoint — called by order-service and inventory-service via the API Gateway.
     * Handles: "order_confirmation", "order_status_update", "LOW_STOCK"
     *
     * Route: POST /notification/send
     */
    @PostMapping("/notification/send")
    @Operation(summary = "Send a notification", description = "Accepts a notification request from the API Gateway and sends an HTML email.")
    public ResponseEntity<NotificationResponse> sendNotification(
            @RequestBody NotificationRequest request,
            HttpServletRequest httpRequest) {
        // Forward the Authorization header so LOW_STOCK handler can call /auth/adminUsers
        String authHeader = httpRequest.getHeader("Authorization");
        NotificationResponse response = notificationService.processNotification(request, authHeader);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Returns paginated notification history stored in MongoDB.
     * Route: GET /notifications?page=0&size=10
     */
    @GetMapping("/notifications")
    @Operation(summary = "Get notification history", description = "Returns a paginated list of all notifications, newest first.")
    public ResponseEntity<Page<Notification>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationService.getRecentNotifications(page, size));
    }
}
