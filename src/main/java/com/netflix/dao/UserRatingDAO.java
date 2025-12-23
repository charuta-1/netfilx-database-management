package com.netflix.dao;

import com.netflix.model.UserRating;
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
import java.util.Optional;

@Repository
public class UserRatingDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserRatingDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Saves or updates a user's rating for a title
     */
    public void save(UserRating rating) {
        // First check if rating exists
        Optional<UserRating> existingRating = findByProfileAndTitle(rating.getProfileId(), rating.getTitleId());

        if (existingRating.isPresent()) {
            // Update existing rating
            String updateSql = "UPDATE user_ratings SET rating_value = ? WHERE profile_id = ? AND title_id = ?";
            jdbcTemplate.update(updateSql, rating.getRatingValue().getValue(), 
                               rating.getProfileId(), rating.getTitleId());
        } else {
            // Insert new rating
            String insertSql = "INSERT INTO user_ratings (profile_id, title_id, rating_value) VALUES (?, ?, ?)";
            jdbcTemplate.update(insertSql, rating.getProfileId(), rating.getTitleId(), 
                               rating.getRatingValue().getValue());
        }
    }

    /**
     * Finds a specific rating by profile and title
     */
    public Optional<UserRating> findByProfileAndTitle(int profileId, int titleId) {
        String sql = "SELECT rating_id, profile_id, title_id, rating_value, created_at FROM user_ratings WHERE profile_id = ? AND title_id = ?";
        try {
            UserRating rating = jdbcTemplate.queryForObject(sql, new UserRatingRowMapper(), profileId, titleId);
            return Optional.of(rating);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Finds all ratings by a specific profile
     */
    public List<UserRating> findByProfileId(int profileId) {
        String sql = "SELECT rating_id, profile_id, title_id, rating_value, created_at FROM user_ratings WHERE profile_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new UserRatingRowMapper(), profileId);
    }

    /**
     * Finds all ratings for a specific title
     */
    public List<UserRating> findByTitleId(int titleId) {
        String sql = "SELECT rating_id, profile_id, title_id, rating_value, created_at FROM user_ratings WHERE title_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new UserRatingRowMapper(), titleId);
    }

    /**
     * Gets thumbs up count for a title
     */
    public int getThumbsUpCount(int titleId) {
        String sql = "SELECT COUNT(*) FROM user_ratings WHERE title_id = ? AND rating_value = 'thumbs_up'";
        return jdbcTemplate.queryForObject(sql, Integer.class, titleId);
    }

    /**
     * Gets thumbs down count for a title
     */
    public int getThumbsDownCount(int titleId) {
        String sql = "SELECT COUNT(*) FROM user_ratings WHERE title_id = ? AND rating_value = 'thumbs_down'";
        return jdbcTemplate.queryForObject(sql, Integer.class, titleId);
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM user_ratings";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Removes a rating
     */
    public void remove(int profileId, int titleId) {
        String sql = "DELETE FROM user_ratings WHERE profile_id = ? AND title_id = ?";
        jdbcTemplate.update(sql, profileId, titleId);
    }

    /**
     * Delete all ratings for a profile
     */
    public void deleteByProfileId(int profileId) {
        String sql = "DELETE FROM user_ratings WHERE profile_id = ?";
        jdbcTemplate.update(sql, profileId);
    }

    /**
     * Get aggregated rating statistics for a profile
     */
    public RatingStats getProfileRatingStats(int profileId) {
        String sql = "SELECT " +
                "SUM(CASE WHEN rating_value = 'thumbs_up' THEN 1 ELSE 0 END) AS thumbs_up, " +
                "SUM(CASE WHEN rating_value = 'thumbs_down' THEN 1 ELSE 0 END) AS thumbs_down, " +
                "COUNT(*) AS total " +
                "FROM user_ratings WHERE profile_id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            int up = rs.getInt("thumbs_up");
            int down = rs.getInt("thumbs_down");
            int total = rs.getInt("total");
            return new RatingStats(up, down, total);
        }, profileId);
    }

    /**
     * Row mapper for UserRating objects
     */
    private static class UserRatingRowMapper implements RowMapper<UserRating> {
        @Override
    public UserRating mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            UserRating rating = new UserRating();
            rating.setRatingId(rs.getLong("rating_id"));
            rating.setProfileId(rs.getInt("profile_id"));
            rating.setTitleId(rs.getInt("title_id"));
            rating.setRatingValue(UserRating.RatingValue.fromString(rs.getString("rating_value")));

            Timestamp timestamp = rs.getTimestamp("created_at");
            if (timestamp != null) {
                rating.setCreatedAt(timestamp.toLocalDateTime());
            }

            return rating;
        }
    }

    /**
     * Aggregated rating stats holder
     */
    public static class RatingStats {
        private final int thumbsUp;
        private final int thumbsDown;
        private final int total;

        public RatingStats(int thumbsUp, int thumbsDown, int total) {
            this.thumbsUp = thumbsUp;
            this.thumbsDown = thumbsDown;
            this.total = total;
        }

        public int getThumbsUp() { return thumbsUp; }
        public int getThumbsDown() { return thumbsDown; }
        public int getTotal() { return total; }
        public double getPositivePercentage() {
            return total > 0 ? (double) thumbsUp / total * 100.0 : 0.0;
        }
    }
}