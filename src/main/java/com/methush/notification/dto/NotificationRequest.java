package com.methush.notification.dto;

import lombok.Data;

@Data
public class NotificationRequest {

    /**
     * Notification type.
     * Supported values: "order_confirmation", "order_status_update", "LOW_STOCK"
     */
    private String type;

    // ── Order notification fields ──────────────────────────────────────────
    /** Recipient email address (user's email for order notifications). */
    private String email;

    /** The order ID (e.g. "ORD#0001"). */
    private String orderId;

    /** New order status — required for "order_status_update" notifications. */
    private String orderStatus;

    // ── Low stock alert fields (sent by inventory service) ─────────────────
    /** Human-readable alert message from the inventory service. */
    private String message;

    /** MongoDB product ID that triggered the low stock alert. */
    private String productId;
}
