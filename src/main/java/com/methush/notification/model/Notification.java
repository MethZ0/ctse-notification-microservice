package com.methush.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;

    /** Notification type: "order_confirmation" or "order_status_update" */
    private String type;

    /** Recipient email address */
    private String email;

    /** Order ID (e.g. "ORD#0001") */
    private String orderId;

    /** New order status — only set for "order_status_update" notifications */
    private String orderStatus;

    private LocalDateTime timestamp;
    private boolean isEmailSent;

}

