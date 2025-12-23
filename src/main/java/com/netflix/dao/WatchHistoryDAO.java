package com.netflix.dao;

import com.netflix.model.WatchHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.lang.NonNull;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class WatchHistoryDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public WatchHistoryDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Adds a new entry to the watch history
     */
    public void add(WatchHistory history) {
        String sql = "INSERT INTO watch_history (profile_id, title_id, is_completed) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, history.getProfileId(), history.getTitleId(), history.isCompleted());
    }

    /**
     * Retrieves the watch history for a profile
     */
    public List<WatchHistory> findByProfileId(int profileId) {
        String sql = "SELECT watch_id, profile_id, title_id, watched_at, is_completed FROM watch_history WHERE profile_id = ? ORDER BY watched_at DESC";
        return jdbcTemplate.query(sql, new WatchHistoryRowMapper(), profileId);
    }

    /**
     * Retrieves the watch history for a profile with pagination
     */
    public List<WatchHistory> findByProfileIdWithLimit(int profileId, int limit, int offset) {
        String sql = "SELECT watch_id, profile_id, title_id, watched_at, is_completed FROM watch_history WHERE profile_id = ? ORDER BY watched_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new WatchHistoryRowMapper(), profileId, limit, offset);
    }

    /**
     * Retrieves only completed watch history for a profile
     */
    public List<WatchHistory> findCompletedByProfileId(int profileId) {
        String sql = "SELECT watch_id, profile_id, title_id, watched_at, is_completed FROM watch_history WHERE profile_id = ? AND is_completed = true ORDER BY watched_at DESC";
        return jdbcTemplate.query(sql, new WatchHistoryRowMapper(), profileId);
    }

    /**
     * Gets the latest watch entry for a specific title and profile
     */
    public WatchHistory findLatestByProfileAndTitle(int profileId, int titleId) {
        String sql = "SELECT watch_id, profile_id, title_id, watched_at, is_completed FROM watch_history WHERE profile_id = ? AND title_id = ? ORDER BY watched_at DESC LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, new WatchHistoryRowMapper(), profileId, titleId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Updates the completion status of a watch history entry
     */
    public void updateCompletionStatus(long watchId, boolean isCompleted) {
        String sql = "UPDATE watch_history SET is_completed = ? WHERE watch_id = ?";
        jdbcTemplate.update(sql, isCompleted, watchId);
    }

    /**
     * Counts the total number of watch history entries for a profile
     */
    public int countByProfileId(int profileId) {
        String sql = "SELECT COUNT(*) FROM watch_history WHERE profile_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, profileId);
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM watch_history";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Deletes watch history entries older than specified days
     */
    public void deleteOldEntries(int profileId, int daysOld) {
        String sql = "DELETE FROM watch_history WHERE profile_id = ? AND watched_at < DATE_SUB(NOW(), INTERVAL ? DAY)";
        jdbcTemplate.update(sql, profileId, daysOld);
    }

    /**
     * Row mapper for WatchHistory objects
     */
    private static class WatchHistoryRowMapper implements RowMapper<WatchHistory> {
        @Override
    public WatchHistory mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            WatchHistory history = new WatchHistory();
            history.setWatchId(rs.getLong("watch_id"));
            history.setProfileId(rs.getInt("profile_id"));
            history.setTitleId(rs.getInt("title_id"));
            history.setCompleted(rs.getBoolean("is_completed"));

            Timestamp timestamp = rs.getTimestamp("watched_at");
            if (timestamp != null) {
                history.setWatchedAt(timestamp.toLocalDateTime());
            }

            return history;
        }
    }
}