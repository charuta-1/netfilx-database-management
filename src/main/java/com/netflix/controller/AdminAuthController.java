package com.netflix.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminAuthController {

    @Value("${netflix.admin.username:admin}")
    private String adminUsername;

    @Value("${netflix.admin.password:admin123}")
    private String adminPassword;

    /**
     * Admin login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody AdminLoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (request == null) {
                response.put("success", false);
                response.put("message", "Request body is required");
                return ResponseEntity.status(400).body(response);
            }

            System.out.println("Admin login attempt for username: " + request.getUsername());

            if (request.getUsername() == null || request.getPassword() == null) {
                response.put("success", false);
                response.put("message", "Username and password are required");
                return ResponseEntity.status(400).body(response);
            }

            if (adminUsername.equals(request.getUsername()) &&
                adminPassword.equals(request.getPassword())) {

                Map<String, String> admin = new HashMap<>();
                admin.put("username", adminUsername);
                admin.put("role", "admin");

                response.put("success", true);
                response.put("message", "Admin login successful");
                response.put("admin", admin);

                System.out.println("Admin login successful");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Invalid admin credentials");

                System.out.println("Admin login failed: Invalid credentials");
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            System.err.println("Error during admin login: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "An error occurred during login: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // DTO for admin login request
    public static class AdminLoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
