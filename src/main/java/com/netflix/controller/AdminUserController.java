package com.netflix.controller;

import com.netflix.dao.UserProfileDAO;
import com.netflix.model.SubscriptionPlan;
import com.netflix.model.User;
import com.netflix.model.UserSubscription;
import com.netflix.service.SubscriptionService;
import com.netflix.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
public class AdminUserController {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final UserProfileDAO userProfileDAO;

    @Autowired
    public AdminUserController(UserService userService,
                               SubscriptionService subscriptionService,
                               UserProfileDAO userProfileDAO) {
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.userProfileDAO = userProfileDAO;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> listUsers() {
        List<User> users = userService.getAllUsers();
        List<AdminUserResponse> response = users.stream()
                .map(user -> buildResponse(user, subscriptionService.getActiveSubscriptionForUser(user.getUserId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody AdminUserCreateRequest request) {
        try {
            if (request == null) {
                return badRequest("Request body is required");
            }

            String email = safeTrim(request.getEmail());
            String username = safeTrim(request.getUsername());
            String password = request.getPassword();

            if (email == null || email.isEmpty()) {
                return badRequest("Email is required");
            }
            if (username == null || username.isEmpty()) {
                return badRequest("Username is required");
            }
            if (password == null || password.trim().isEmpty()) {
                return badRequest("Password is required");
            }

            LocalDate dob = parseDate(request.getDateOfBirth());

            User user = userService.registerUser(email, username, password, dob);

            UserSubscription subscription = null;
            if (request.getSubscriptionPlanId() != null) {
                subscription = subscriptionService.subscribeUserToPlan(user.getUserId(), request.getSubscriptionPlanId());
            } else {
                subscription = subscriptionService.getActiveSubscriptionForUser(user.getUserId());
            }

            User sanitizedUser = userService.getUserById(user.getUserId());
            AdminUserResponse response = buildResponse(sanitizedUser, subscription);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable int userId, @RequestBody AdminUserUpdateRequest request) {
        try {
            if (request == null) {
                return badRequest("Request body is required");
            }

            User existing = userService.getUserById(userId);
            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse("User not found"));
            }

            String email = safeTrim(request.getEmail());
            if (email == null) {
                email = existing.getEmail();
            }

            String username = safeTrim(request.getUsername());
            if (username == null) {
                username = existing.getUsername();
            }

            User updated;
            if (request.getDateOfBirth() != null) {
                LocalDate dob = parseDate(request.getDateOfBirth());
                updated = userService.updateUser(userId, email, username, dob);
            } else {
                updated = userService.updateUser(userId, email, username);
            }

            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                userService.adminSetPassword(userId, request.getPassword());
            }

            if (request.getSubscriptionPlanId() != null) {
                subscriptionService.subscribeUserToPlan(userId, request.getSubscriptionPlanId());
            } else if (Boolean.TRUE.equals(request.getCancelSubscription())) {
                UserSubscription active = subscriptionService.getActiveSubscriptionForUser(userId);
                if (active != null) {
                    subscriptionService.cancelActiveSubscription(userId);
                }
            }

            User sanitized = userService.getUserById(updated.getUserId());
            UserSubscription subscription = subscriptionService.getActiveSubscriptionForUser(userId);
            AdminUserResponse response = buildResponse(sanitized, subscription);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable int userId) {
        try {
            userService.deleteUser(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User deleted");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return badRequest(e.getMessage());
        }
    }

    private AdminUserResponse buildResponse(User user, UserSubscription subscription) {
        int profileCount = userProfileDAO.countByUserId(user.getUserId());
        return new AdminUserResponse(user, subscription, profileCount);
    }

    private LocalDate parseDate(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid date format. Use YYYY-MM-DD");
        }
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.badRequest().body(errorResponse(message));
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    public static class AdminUserCreateRequest {
        private String email;
        private String username;
        private String password;
        private String dateOfBirth;
        private Integer subscriptionPlanId;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

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

        public String getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }

        public Integer getSubscriptionPlanId() {
            return subscriptionPlanId;
        }

        public void setSubscriptionPlanId(Integer subscriptionPlanId) {
            this.subscriptionPlanId = subscriptionPlanId;
        }
    }

    public static class AdminUserUpdateRequest extends AdminUserCreateRequest {
        private Boolean cancelSubscription;

        public Boolean getCancelSubscription() {
            return cancelSubscription;
        }

        public void setCancelSubscription(Boolean cancelSubscription) {
            this.cancelSubscription = cancelSubscription;
        }
    }

    public static class AdminUserResponse {
        private final int userId;
        private final String email;
        private final String username;
        private final String dateOfBirth;
        private final String createdAt;
        private final int profileCount;
        private final SubscriptionSummary subscription;

        public AdminUserResponse(User user, UserSubscription subscription, int profileCount) {
            this.userId = user.getUserId();
            this.email = user.getEmail();
            this.username = user.getUsername();
            this.dateOfBirth = user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null;
            LocalDateTime created = user.getCreatedAt();
            this.createdAt = created != null ? created.toString() : null;
            this.profileCount = profileCount;
            this.subscription = subscription != null ? new SubscriptionSummary(subscription) : null;
        }

        public int getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getUsername() {
            return username;
        }

        public String getDateOfBirth() {
            return dateOfBirth;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public int getProfileCount() {
            return profileCount;
        }

        public SubscriptionSummary getSubscription() {
            return subscription;
        }
    }

    public static class SubscriptionSummary {
        private final int subscriptionId;
        private final int planId;
        private final String planName;
        private final BigDecimal price;
        private final String quality;
        private final Integer screensAllowed;
        private final String status;
        private final String startDate;
        private final String endDate;
        private final String updatedAt;

        public SubscriptionSummary(UserSubscription subscription) {
            this.subscriptionId = subscription.getSubscriptionId();
            this.planId = subscription.getPlanId();
            SubscriptionPlan plan = subscription.getPlan();
            if (plan != null) {
                this.planName = plan.getPlanName();
                this.price = plan.getPrice();
                this.quality = plan.getQuality();
                this.screensAllowed = plan.getScreensAllowed();
            } else {
                this.planName = null;
                this.price = null;
                this.quality = null;
                this.screensAllowed = null;
            }
            this.status = subscription.getStatus();
            this.startDate = optionalDate(subscription.getStartDate());
            this.endDate = optionalDate(subscription.getEndDate());
            this.updatedAt = optionalDate(subscription.getUpdatedAt());
        }

        private String optionalDate(LocalDateTime value) {
            return value != null ? value.toString() : null;
        }

        public int getSubscriptionId() {
            return subscriptionId;
        }

        public int getPlanId() {
            return planId;
        }

        public String getPlanName() {
            return planName;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public String getQuality() {
            return quality;
        }

        public Integer getScreensAllowed() {
            return screensAllowed;
        }

        public String getStatus() {
            return status;
        }

        public String getStartDate() {
            return startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }
    }
}
