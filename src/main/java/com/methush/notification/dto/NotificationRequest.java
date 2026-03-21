package com.methush.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationRequest {

    @NotBlank(message = "orderId is mandatory")
    private String orderId;

    @NotBlank(message = "userId is mandatory")
    private String userId;

    @NotBlank(message = "message is mandatory")
    private String message;

}
