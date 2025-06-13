package com.ecommerce.user.controller;

import com.ecommerce.user.dto.*;
import com.ecommerce.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for user registration, authentication and profile management")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Register a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<com.ecommerce.user.dto.ApiResponse<UserResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        
        UserResponse user = userService.registerUser(request);
        com.ecommerce.user.dto.ApiResponse<UserResponse> response = 
            new com.ecommerce.user.dto.ApiResponse<>(
                HttpStatus.CREATED.value(),
                "User registered successfully", 
                user
            );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<com.ecommerce.user.dto.ApiResponse<JwtAuthenticationResponse>> loginUser(
            @Valid @RequestBody LoginRequest request) {
        
        JwtAuthenticationResponse authResponse = userService.authenticateUser(request);
        com.ecommerce.user.dto.ApiResponse<JwtAuthenticationResponse> response = 
            com.ecommerce.user.dto.ApiResponse.success("Login successful", authResponse);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieve user information by user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.ecommerce.user.dto.ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        
        UserResponse user = userService.getUserById(userId);
        com.ecommerce.user.dto.ApiResponse<UserResponse> response = 
            com.ecommerce.user.dto.ApiResponse.success("User retrieved successfully", user);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile/{username}")
    @Operation(summary = "Get user by username", description = "Retrieve user information by username")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.ecommerce.user.dto.ApiResponse<UserResponse>> getUserByUsername(
            @Parameter(description = "Username") @PathVariable String username) {
        
        UserResponse user = userService.getUserByUsername(username);
        com.ecommerce.user.dto.ApiResponse<UserResponse> response = 
            com.ecommerce.user.dto.ApiResponse.success("User retrieved successfully", user);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user", description = "Updates user profile information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.ecommerce.user.dto.ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        
        UserResponse user = userService.updateUser(userId, request);
        com.ecommerce.user.dto.ApiResponse<UserResponse> response = 
            com.ecommerce.user.dto.ApiResponse.success("User updated successfully", user);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user", description = "Deactivate user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<com.ecommerce.user.dto.ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        
        userService.deleteUser(userId);
        com.ecommerce.user.dto.ApiResponse<Void> response = 
            com.ecommerce.user.dto.ApiResponse.error(HttpStatus.OK.value(), "User deactivated successfully");
        
        return ResponseEntity.ok(response);
    }
}
