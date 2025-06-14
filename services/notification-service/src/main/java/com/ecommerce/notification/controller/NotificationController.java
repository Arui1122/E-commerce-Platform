package com.ecommerce.notification.controller;

import com.ecommerce.notification.dto.NotificationMessage;
import com.ecommerce.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Notification management APIs")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @PostMapping("/send")
    @Operation(summary = "Send notification", description = "Send a notification message")
    public ResponseEntity<Map<String, Object>> sendNotification(@Valid @RequestBody NotificationMessage message) {
        message.setTimestamp(LocalDateTime.now());
        notificationService.sendNotification(message);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Notification sent successfully",
            "timestamp", LocalDateTime.now()
        ));
    }
    
    @PostMapping("/send-email")
    @Operation(summary = "Send email notification", description = "Send an email notification")
    public ResponseEntity<Map<String, Object>> sendEmail(
            @RequestParam("to") String to,
            @RequestParam("subject") String subject,
            @RequestParam("template") String template,
            @RequestBody(required = false) Map<String, Object> data) {
        
        notificationService.sendEmailNotification(to, subject, template, data);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Email sent successfully",
            "timestamp", LocalDateTime.now()
        ));
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check notification service health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "notification-service",
            "timestamp", LocalDateTime.now()
        ));
    }
}
