package com.ecommerce.user.service.impl;

import com.ecommerce.events.UserEvent;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventServiceImpl implements UserEventService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String USER_REGISTERED_TOPIC = "user.registered";
    private static final String USER_PROFILE_UPDATED_TOPIC = "user.profile-updated";
    private static final String USER_PASSWORD_RESET_TOPIC = "user.password-reset";
    
    @Override
    public void publishUserRegisteredEvent(User user) {
        try {
            UserEvent event = new UserEvent("USER_REGISTERED", user.getId(), user.getUsername(), user.getEmail());
            event.setFirstName(user.getFirstName());
            event.setLastName(user.getLastName());
            event.setPhone(user.getPhone());
            
            kafkaTemplate.send(USER_REGISTERED_TOPIC, user.getId().toString(), event);
            log.info("Published USER_REGISTERED event for user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to publish USER_REGISTERED event for user: {}", user.getUsername(), e);
        }
    }
    
    @Override
    public void publishUserProfileUpdatedEvent(User user) {
        try {
            UserEvent event = new UserEvent("USER_PROFILE_UPDATED", user.getId(), user.getUsername(), user.getEmail());
            event.setFirstName(user.getFirstName());
            event.setLastName(user.getLastName());
            event.setPhone(user.getPhone());
            
            kafkaTemplate.send(USER_PROFILE_UPDATED_TOPIC, user.getId().toString(), event);
            log.info("Published USER_PROFILE_UPDATED event for user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to publish USER_PROFILE_UPDATED event for user: {}", user.getUsername(), e);
        }
    }
    
    @Override
    public void publishPasswordResetEvent(User user) {
        try {
            UserEvent event = new UserEvent("USER_PASSWORD_RESET", user.getId(), user.getUsername(), user.getEmail());
            event.setFirstName(user.getFirstName());
            event.setLastName(user.getLastName());
            
            kafkaTemplate.send(USER_PASSWORD_RESET_TOPIC, user.getId().toString(), event);
            log.info("Published USER_PASSWORD_RESET event for user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to publish USER_PASSWORD_RESET event for user: {}", user.getUsername(), e);
        }
    }
}
