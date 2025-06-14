package com.ecommerce.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEventMessage {
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String eventType; // REGISTERED, PROFILE_UPDATED, PASSWORD_RESET
}
