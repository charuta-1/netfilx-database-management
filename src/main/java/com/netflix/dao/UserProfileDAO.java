package com.netflix.dao;

import com.netflix.model.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class UserProfileDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserProfileDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Inserts a new user profile
     */
    public void save(UserProfile profile) {
        String sql = "INSERT INTO user_profiles (user_id, profile_name) VALUES (?, ?)";
        jdbcTemplate.update(sql, profile.getUserId(), profile.getProfileName());
    }

    /**
     * Finds all profiles for a specific user
     */
    public List<UserProfile> findByUserId(int userId) {
        String sql = "SELECT profile_id, user_id, profile_name, maturity_rating_override, created_at FROM user_profiles WHERE user_id = ? ORDER BY created_at";
        return jdbcTemplate.query(sql, new UserProfileRowMapper(), userId);
    }

    /**
     * Finds a profile by its ID
     */
    public UserProfile findById(int profileId) {
        String sql = "SELECT profile_id, user_id, profile_name, maturity_rating_override, created_at FROM user_profiles WHERE profile_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new UserProfileRowMapper(), profileId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Updates a user profile
     */
    public void update(UserProfile profile) {
        String sql = "UPDATE user_profiles SET profile_name = ?, maturity_rating_override = ? WHERE profile_id = ?";
        jdbcTemplate.update(sql, profile.getProfileName(), profile.getMaturityRatingOverride(), profile.getProfileId());
    }

    /**
     * Deletes a profile
     */
    public void deleteById(int profileId) {
        String sql = "DELETE FROM user_profiles WHERE profile_id = ?";
        jdbcTemplate.update(sql, profileId);
    }

    /**
     * Get all profiles
     */
    public List<UserProfile> findAll() {
        String sql = "SELECT profile_id, user_id, profile_name, maturity_rating_override, created_at FROM user_profiles";
        return jdbcTemplate.query(sql, new UserProfileRowMapper());
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM user_profiles";
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
        return result != null ? result : 0;
    }

    /**
     * Count profiles by user ID
     */
    public int countByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM user_profiles WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count : 0;
    }

    /**
     * Row mapper for UserProfile objects
     */
    private static class UserProfileRowMapper implements RowMapper<UserProfile> {
        @Override
    public UserProfile mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            UserProfile profile = new UserProfile();
            profile.setProfileId(rs.getInt("profile_id"));
            profile.setUserId(rs.getInt("user_id"));
            profile.setProfileName(rs.getString("profile_name"));
            profile.setMaturityRatingOverride(rs.getString("maturity_rating_override"));

            Timestamp timestamp = rs.getTimestamp("created_at");
            if (timestamp != null) {
                profile.setCreatedAt(timestamp.toLocalDateTime());
            }

            return profile;
        }
    }
}