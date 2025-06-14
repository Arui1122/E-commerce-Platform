package com.ecommerce.notification.service;

import com.ecommerce.notification.dto.NotificationMessage;

public interface NotificationService {
    void sendNotification(NotificationMessage message);
    void sendEmailNotification(String to, String subject, String template, Object data);
}
