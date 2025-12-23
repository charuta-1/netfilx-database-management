package com.netflix.controller;

import com.netflix.model.SubscriptionPlan;
import com.netflix.model.UserSubscription;
import com.netflix.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getPlans() {
        return ResponseEntity.ok(subscriptionService.getAllPlans());
    }

    @PostMapping("/plans")
    public ResponseEntity<?> createPlan(@RequestBody SubscriptionPlan plan) {
        try {
            SubscriptionPlan created = subscriptionService.createPlan(plan);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @PutMapping("/plans/{planId}")
    public ResponseEntity<?> updatePlan(@PathVariable int planId, @RequestBody SubscriptionPlan plan) {
        try {
            SubscriptionPlan updated = subscriptionService.updatePlan(planId, plan);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/plans/{planId}")
    public ResponseEntity<?> deletePlan(@PathVariable int planId) {
        subscriptionService.deletePlan(planId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Plan deleted");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserSubscription(@PathVariable int userId,
                                                 @RequestParam(name = "history", defaultValue = "false") boolean includeHistory) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("activeSubscription", subscriptionService.getActiveSubscriptionForUser(userId));
        if (includeHistory) {
            response.put("subscriptions", subscriptionService.getSubscriptionHistoryForUser(userId));
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/user/{userId}/subscribe")
    public ResponseEntity<?> subscribeUser(@PathVariable int userId, @RequestBody SubscriptionRequest request) {
        try {
            UserSubscription subscription = subscriptionService.subscribeUserToPlan(userId, request.getPlanId());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("subscription", subscription);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @PostMapping("/user/{userId}/cancel")
    public ResponseEntity<?> cancelActiveSubscription(@PathVariable int userId) {
        try {
            UserSubscription subscription = subscriptionService.cancelActiveSubscription(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("subscription", subscription);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{subscriptionId}/cancel")
    public ResponseEntity<?> cancelSubscription(@PathVariable int subscriptionId, @RequestBody(required = false) CancelRequest request) {
        try {
            String status = request != null ? request.getStatus() : null;
            UserSubscription subscription = subscriptionService.cancelSubscriptionById(subscriptionId, status);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("subscription", subscription);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    public static class SubscriptionRequest {
        private int planId;

        public int getPlanId() {
            return planId;
        }

        public void setPlanId(int planId) {
            this.planId = planId;
        }
    }

    public static class CancelRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
