package com.ecommerce.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String type;
    private String recipient;
    private String subject;
    private String template;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
    private String source;
}
