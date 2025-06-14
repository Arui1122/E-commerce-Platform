package com.ecommerce.notification.listener;

import com.ecommerce.notification.dto.UserEventMessage;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {
    
    private final NotificationService notificationService;
    
    @KafkaListener(topics = "user.registered", groupId = "notification-service")
    public void handleUserRegistered(UserEventMessage userEvent) {
        log.info("Received user registered event: {}", userEvent);
        
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("username", userEvent.getUsername());
        templateData.put("firstName", userEvent.getFirstName());
        templateData.put("lastName", userEvent.getLastName());
        
        notificationService.sendEmailNotification(
            userEvent.getEmail(),
            "Welcome to E-commerce Platform",
            "welcome",
            templateData
        );
    }
    
    @KafkaListener(topics = "user.password-reset", groupId = "notification-service")
    public void handlePasswordReset(UserEventMessage userEvent) {
        log.info("Received password reset event: {}", userEvent);
        
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("username", userEvent.getUsername());
        templateData.put("firstName", userEvent.getFirstName());
        templateData.put("resetLink", "https://ecommerce.com/reset-password");
        
        notificationService.sendEmailNotification(
            userEvent.getEmail(),
            "Password Reset Request",
            "password-reset",
            templateData
        );
    }
    
    @KafkaListener(topics = "user.profile-updated", groupId = "notification-service")
    public void handleProfileUpdated(UserEventMessage userEvent) {
        log.info("Received profile updated event: {}", userEvent);
        
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("username", userEvent.getUsername());
        templateData.put("firstName", userEvent.getFirstName());
        templateData.put("lastName", userEvent.getLastName());
        
        notificationService.sendEmailNotification(
            userEvent.getEmail(),
            "Profile Updated Successfully",
            "profile-updated",
            templateData
        );
    }
}
