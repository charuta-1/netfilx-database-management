package com.netflix.dao;

import com.netflix.model.SubscriptionPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import org.springframework.lang.NonNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
public class SubscriptionPlanDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SubscriptionPlanDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<SubscriptionPlan> findAll() {
        String sql = "SELECT plan_id, plan_name, price, quality, screens_allowed FROM SubscriptionPlans ORDER BY price";
        return jdbcTemplate.query(sql, new SubscriptionPlanRowMapper());
    }

    public SubscriptionPlan findById(int planId) {
        String sql = "SELECT plan_id, plan_name, price, quality, screens_allowed FROM SubscriptionPlans WHERE plan_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new SubscriptionPlanRowMapper(), planId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public SubscriptionPlan findByName(String name) {
        String sql = "SELECT plan_id, plan_name, price, quality, screens_allowed FROM SubscriptionPlans WHERE LOWER(plan_name) = LOWER(?)";
        try {
            return jdbcTemplate.queryForObject(sql, new SubscriptionPlanRowMapper(), name);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public SubscriptionPlan save(SubscriptionPlan plan) {
        String sql = "INSERT INTO SubscriptionPlans (plan_name, price, quality, screens_allowed) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, plan.getPlanName());
            ps.setBigDecimal(2, plan.getPrice());
            ps.setString(3, plan.getQuality());
            ps.setInt(4, plan.getScreensAllowed());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new RuntimeException("Failed to create subscription plan");
        }

        return findById(key.intValue());
    }

    public SubscriptionPlan update(int planId, SubscriptionPlan plan) {
        String sql = "UPDATE SubscriptionPlans SET plan_name = ?, price = ?, quality = ?, screens_allowed = ? WHERE plan_id = ?";
        int updated = jdbcTemplate.update(sql, plan.getPlanName(), plan.getPrice(), plan.getQuality(), plan.getScreensAllowed(), planId);
        if (updated == 0) {
            throw new RuntimeException("Subscription plan not found: " + planId);
        }
        return findById(planId);
    }

    public void delete(int planId) {
        String sql = "DELETE FROM SubscriptionPlans WHERE plan_id = ?";
        jdbcTemplate.update(sql, planId);
    }

    private static class SubscriptionPlanRowMapper implements RowMapper<SubscriptionPlan> {
        @Override
        public SubscriptionPlan mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            SubscriptionPlan plan = new SubscriptionPlan();
            plan.setPlanId(rs.getInt("plan_id"));
            plan.setPlanName(rs.getString("plan_name"));
            plan.setPrice(rs.getBigDecimal("price"));
            plan.setQuality(rs.getString("quality"));
            plan.setScreensAllowed(rs.getInt("screens_allowed"));
            return plan;
        }
    }
}
