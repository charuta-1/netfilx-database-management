package com.netflix.controller;

import com.netflix.model.User;
import com.netflix.model.UserProfile;
import com.netflix.model.UserSubscription;
import com.netflix.service.SubscriptionService;
import com.netflix.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final SubscriptionService subscriptionService;

    @Autowired
    public UserController(UserService userService, SubscriptionService subscriptionService) {
        this.userService = userService;
        this.subscriptionService = subscriptionService;
    }

    /**
     * Register new user
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (request == null) {
                response.put("success", false);
                response.put("message", "Request body is required");
                return ResponseEntity.badRequest().body(response);
            }

            System.out.println("Registration attempt for email: " + request.getEmail());

            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Username is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                response.put("success", false);
                response.put("message", "Password is required");
                return ResponseEntity.badRequest().body(response);
            }

            LocalDate dob = null;
            if (request.getDateOfBirth() != null && !request.getDateOfBirth().trim().isEmpty()) {
                try {
                    dob = LocalDate.parse(request.getDateOfBirth().trim());
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("message", "Invalid date format. Use YYYY-MM-DD");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            User user = userService.registerUser(
                request.getEmail().trim(),
                request.getUsername().trim(),
                request.getPassword(),
                dob
            );

            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("user", user);

            System.out.println("User registered successfully: " + user.getEmail());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.err.println("Registration error: " + e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            System.err.println("Unexpected registration error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "An unexpected error occurred during registration");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * User login
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody UserLoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (request == null) {
                response.put("success", false);
                response.put("message", "Request body is required");
                return ResponseEntity.badRequest().body(response);
            }

            System.out.println("Login attempt for email: " + request.getEmail());

            // Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                response.put("success", false);
                response.put("message", "Password is required");
                return ResponseEntity.badRequest().body(response);
            }

            User user = userService.authenticateUser(request.getEmail().trim(), request.getPassword());
            List<UserProfile> profiles = userService.getUserProfiles(user.getUserId());
            UserSubscription subscription = subscriptionService.getActiveSubscriptionForUser(user.getUserId());

            response.put("success", true);
            response.put("message", "Login successful");
            response.put("user", user);
            response.put("profiles", profiles);
            response.put("subscription", subscription);

            System.out.println("User login successful: " + user.getEmail());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            System.err.println("Login error: " + e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            System.err.println("Unexpected login error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "An unexpected error occurred during login");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable int userId) {
        try {
            User user = userService.getUserById(userId);
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(user);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update user information
     */
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable int userId, @RequestBody UserUpdateRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (request == null) {
                response.put("success", false);
                response.put("message", "Request body is required");
                return ResponseEntity.badRequest().body(response);
            }

            User user = userService.updateUser(userId, request.getEmail(), request.getUsername());

            response.put("success", true);
            response.put("message", "User updated successfully");
            response.put("user", user);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Change user password
     */
    @PutMapping("/{userId}/password")
    public ResponseEntity<?> changePassword(@PathVariable int userId, @RequestBody PasswordChangeRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (request == null) {
                response.put("success", false);
                response.put("message", "Request body is required");
                return ResponseEntity.badRequest().body(response);
            }

            userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());

            response.put("success", true);
            response.put("message", "Password changed successfully");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get user profiles
     */
    @GetMapping("/{userId}/profiles")
    public ResponseEntity<List<UserProfile>> getUserProfiles(@PathVariable int userId) {
        List<UserProfile> profiles = userService.getUserProfiles(userId);
        return ResponseEntity.ok(profiles);
    }

    // Request DTOs
    public static class UserRegistrationRequest {
        private String email;
        private String username;
        private String password;
        private String dateOfBirth;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    }

    public static class UserLoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class UserUpdateRequest {
        private String email;
        private String username;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    public static class PasswordChangeRequest {
        private String oldPassword;
        private String newPassword;

        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}