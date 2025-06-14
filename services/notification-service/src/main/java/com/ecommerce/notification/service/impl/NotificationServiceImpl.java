package com.ecommerce.notification.service.impl;

import com.ecommerce.notification.dto.NotificationMessage;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${notification.email.from}")
    private String fromEmail;
    
    @Override
    public void sendNotification(NotificationMessage message) {
        try {
            switch (message.getType().toUpperCase()) {
                case "EMAIL":
                    sendEmailNotification(
                        message.getRecipient(),
                        message.getSubject(),
                        message.getTemplate(),
                        message.getData()
                    );
                    break;
                case "SMS":
                    // TODO: Implement SMS notification
                    log.info("SMS notification sent to: {}", message.getRecipient());
                    break;
                case "PUSH":
                    // TODO: Implement push notification
                    log.info("Push notification sent to: {}", message.getRecipient());
                    break;
                default:
                    log.warn("Unknown notification type: {}", message.getType());
            }
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void sendEmailNotification(String to, String subject, String template, Object data) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            
            // Process template with data
            Context context = new Context();
            if (data instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) data;
                dataMap.forEach(context::setVariable);
            } else {
                context.setVariable("data", data);
            }
            
            String htmlContent = templateEngine.process(template, context);
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            log.info("Email sent successfully to: {}", to);
            
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }
}
