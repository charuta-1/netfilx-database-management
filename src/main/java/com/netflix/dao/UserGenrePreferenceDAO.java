package com.netflix.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Repository
public class UserGenrePreferenceDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserGenrePreferenceDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Save a full set of preferences for a profile (clears existing first)
     */
    public void save(int profileId, List<Integer> genreIds) {
        clearPreferences(profileId);
        if (genreIds == null || genreIds.isEmpty()) return;

        String sql = "INSERT INTO user_genre_preferences (profile_id, genre_id) VALUES (?, ?)";
        List<Object[]> batch = new ArrayList<>();
        for (Integer genreId : genreIds) {
            batch.add(new Object[]{profileId, genreId});
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    /**
     * Get all preferred genre IDs for a profile
     */
    public List<Integer> findGenreIdsByProfileId(int profileId) {
        String sql = "SELECT genre_id FROM user_genre_preferences WHERE profile_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt("genre_id"), profileId);
    }

    /**
     * Check if a profile has any preferences
     */
    public boolean hasPreferences(int profileId) {
        String sql = "SELECT COUNT(*) FROM user_genre_preferences WHERE profile_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, profileId);
        return count != null && count > 0;
    }

    /**
     * Add a single preference (ignores duplicates thanks to unique key)
     */
    public void addPreference(int profileId, int genreId) {
        String sql = "INSERT IGNORE INTO user_genre_preferences (profile_id, genre_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, profileId, genreId);
    }

    /**
     * Remove a single preference
     */
    public void removePreference(int profileId, int genreId) {
        String sql = "DELETE FROM user_genre_preferences WHERE profile_id = ? AND genre_id = ?";
        jdbcTemplate.update(sql, profileId, genreId);
    }

    /**
     * Clear all preferences for a profile
     */
    public void clearPreferences(int profileId) {
        String sql = "DELETE FROM user_genre_preferences WHERE profile_id = ?";
        jdbcTemplate.update(sql, profileId);
    }
}

