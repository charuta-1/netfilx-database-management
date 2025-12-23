package com.netflix.dao;

import com.netflix.model.SubscriptionPlan;
import com.netflix.model.UserSubscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class UserSubscriptionDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserSubscriptionDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public UserSubscription findById(int subscriptionId) {
        String sql = baseSelect() + " WHERE us.subscription_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new UserSubscriptionRowMapper(), subscriptionId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public UserSubscription findActiveByUserId(int userId) {
        String sql = baseSelect() + " WHERE us.user_id = ? AND us.status = 'active' ORDER BY us.start_date DESC LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, new UserSubscriptionRowMapper(), userId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<UserSubscription> findByUserId(int userId) {
        String sql = baseSelect() + " WHERE us.user_id = ? ORDER BY us.start_date DESC";
        return jdbcTemplate.query(sql, new UserSubscriptionRowMapper(), userId);
    }

    public UserSubscription create(int userId, int planId) {
        String sql = "INSERT INTO UserSubscriptions (user_id, plan_id, status) VALUES (?, ?, 'active')";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.setInt(2, planId);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new RuntimeException("Failed to create subscription");
        }
        return findById(key.intValue());
    }

    public void updateStatus(int subscriptionId, String status, LocalDateTime endDate) {
        String sql = "UPDATE UserSubscriptions SET status = ?, end_date = ?, updated_at = CURRENT_TIMESTAMP WHERE subscription_id = ?";
        Timestamp end = endDate != null ? Timestamp.valueOf(endDate) : null;
        jdbcTemplate.update(sql, status, end, subscriptionId);
    }

    public int deactivateActiveSubscriptions(int userId, String status, LocalDateTime endDate) {
        String sql = "UPDATE UserSubscriptions SET status = ?, end_date = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND status = 'active'";
        Timestamp end = endDate != null ? Timestamp.valueOf(endDate) : null;
        return jdbcTemplate.update(sql, status, end, userId);
    }

    private String baseSelect() {
        return "SELECT us.subscription_id, us.user_id, us.plan_id, us.start_date, us.end_date, us.status, " +
                "us.created_at, us.updated_at, sp.plan_name, sp.price, sp.quality, sp.screens_allowed " +
                "FROM UserSubscriptions us JOIN SubscriptionPlans sp ON us.plan_id = sp.plan_id";
    }

    private static class UserSubscriptionRowMapper implements RowMapper<UserSubscription> {
        @Override
        public UserSubscription mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            UserSubscription subscription = new UserSubscription();
            subscription.setSubscriptionId(rs.getInt("subscription_id"));
            subscription.setUserId(rs.getInt("user_id"));
            subscription.setPlanId(rs.getInt("plan_id"));

            Timestamp startTs = rs.getTimestamp("start_date");
            if (startTs != null) {
                subscription.setStartDate(startTs.toLocalDateTime());
            }

            Timestamp endTs = rs.getTimestamp("end_date");
            if (endTs != null) {
                subscription.setEndDate(endTs.toLocalDateTime());
            }

            subscription.setStatus(rs.getString("status"));

            Timestamp createdTs = rs.getTimestamp("created_at");
            if (createdTs != null) {
                subscription.setCreatedAt(createdTs.toLocalDateTime());
            }

            Timestamp updatedTs = rs.getTimestamp("updated_at");
            if (updatedTs != null) {
                subscription.setUpdatedAt(updatedTs.toLocalDateTime());
            }

            SubscriptionPlan plan = new SubscriptionPlan();
            plan.setPlanId(rs.getInt("plan_id"));
            plan.setPlanName(rs.getString("plan_name"));
            plan.setPrice(rs.getBigDecimal("price"));
            plan.setQuality(rs.getString("quality"));
            plan.setScreensAllowed(rs.getInt("screens_allowed"));
            subscription.setPlan(plan);

            return subscription;
        }
    }
}
