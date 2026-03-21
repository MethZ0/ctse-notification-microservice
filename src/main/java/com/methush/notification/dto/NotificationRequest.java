package com.methush.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationRequest {

    /**
     * Notification type.
     * Supported values: "order_confirmation", "order_status_update"
     */
    @NotBlank(message = "type is mandatory")
    private String type;

    /**
     * Recipient email address (the user's email).
     */
    @NotBlank(message = "email is mandatory")
    @Email(message = "email must be a valid email address")
    private String email;

    /**
     * The order ID (e.g. "ORD#0001").
     */
    @NotBlank(message = "orderId is mandatory")
    private String orderId;

    /**
     * New order status. Required only for "order_status_update" notifications.
     * E.g. "Shipped", "Delivered", "Cancelled"
     */
    private String orderStatus;

}
