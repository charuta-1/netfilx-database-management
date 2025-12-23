package com.netflix.service;

import com.netflix.dao.SubscriptionPlanDAO;
import com.netflix.dao.UserDAO;
import com.netflix.dao.UserSubscriptionDAO;
import com.netflix.model.SubscriptionPlan;
import com.netflix.model.User;
import com.netflix.model.UserSubscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionPlanDAO subscriptionPlanDAO;
    private final UserSubscriptionDAO userSubscriptionDAO;
    private final UserDAO userDAO;

    @Autowired
    public SubscriptionService(SubscriptionPlanDAO subscriptionPlanDAO,
                               UserSubscriptionDAO userSubscriptionDAO,
                               UserDAO userDAO) {
        this.subscriptionPlanDAO = subscriptionPlanDAO;
        this.userSubscriptionDAO = userSubscriptionDAO;
        this.userDAO = userDAO;
    }

    // ============================= Plans =============================

    public List<SubscriptionPlan> getAllPlans() {
        return subscriptionPlanDAO.findAll();
    }

    public SubscriptionPlan createPlan(SubscriptionPlan plan) {
        validatePlan(plan);
        SubscriptionPlan existing = subscriptionPlanDAO.findByName(plan.getPlanName());
        if (existing != null) {
            throw new RuntimeException("A plan with this name already exists");
        }
        return subscriptionPlanDAO.save(plan);
    }

    public SubscriptionPlan updatePlan(int planId, SubscriptionPlan plan) {
        validatePlan(plan);
        SubscriptionPlan existing = subscriptionPlanDAO.findById(planId);
        if (existing == null) {
            throw new RuntimeException("Subscription plan not found");
        }

        SubscriptionPlan duplicateName = subscriptionPlanDAO.findByName(plan.getPlanName());
        if (duplicateName != null && duplicateName.getPlanId() != planId) {
            throw new RuntimeException("Another plan already uses this name");
        }

        plan.setPlanId(planId);
        return subscriptionPlanDAO.update(planId, plan);
    }

    public void deletePlan(int planId) {
        subscriptionPlanDAO.delete(planId);
    }

    private void validatePlan(SubscriptionPlan plan) {
        if (plan.getPlanName() == null || plan.getPlanName().trim().isEmpty()) {
            throw new RuntimeException("Plan name is required");
        }
        if (plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Price must be a positive value");
        }
        if (plan.getQuality() == null || plan.getQuality().trim().isEmpty()) {
            throw new RuntimeException("Quality is required");
        }
        if (plan.getScreensAllowed() <= 0) {
            throw new RuntimeException("Screens allowed must be greater than zero");
        }
    }

    // ============================= User Subscriptions =============================

    public UserSubscription getActiveSubscriptionForUser(int userId) {
        return userSubscriptionDAO.findActiveByUserId(userId);
    }

    public List<UserSubscription> getSubscriptionHistoryForUser(int userId) {
        return userSubscriptionDAO.findByUserId(userId);
    }

    @Transactional
    public UserSubscription subscribeUserToPlan(int userId, int planId) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        SubscriptionPlan plan = subscriptionPlanDAO.findById(planId);
        if (plan == null) {
            throw new RuntimeException("Subscription plan not found");
        }

        LocalDateTime now = LocalDateTime.now();
        userSubscriptionDAO.deactivateActiveSubscriptions(userId, "canceled", now);
        return userSubscriptionDAO.create(userId, planId);
    }

    @Transactional
    public UserSubscription cancelActiveSubscription(int userId) {
        UserSubscription active = userSubscriptionDAO.findActiveByUserId(userId);
        if (active == null) {
            throw new RuntimeException("No active subscription found");
        }

        LocalDateTime now = LocalDateTime.now();
        userSubscriptionDAO.updateStatus(active.getSubscriptionId(), "canceled", now);
        active.setStatus("canceled");
        active.setEndDate(now);
        return active;
    }

    @Transactional
    public UserSubscription cancelSubscriptionById(int subscriptionId, String status) {
        UserSubscription subscription = userSubscriptionDAO.findById(subscriptionId);
        if (subscription == null) {
            throw new RuntimeException("Subscription not found");
        }

        if ("active".equalsIgnoreCase(subscription.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            String finalStatus = status != null ? status : "canceled";
            userSubscriptionDAO.updateStatus(subscriptionId, finalStatus, now);
            subscription.setStatus(finalStatus);
            subscription.setEndDate(now);
            subscription.setUpdatedAt(now);
        }
        return subscription;
    }
}
