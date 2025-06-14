package com.ecommerce.user.service;

import com.ecommerce.events.UserEvent;
import com.ecommerce.user.entity.User;

public interface UserEventService {
    
    void publishUserRegisteredEvent(User user);
    
    void publishUserProfileUpdatedEvent(User user);
    
    void publishPasswordResetEvent(User user);
}
