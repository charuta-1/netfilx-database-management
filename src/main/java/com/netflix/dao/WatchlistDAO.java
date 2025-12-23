package com.netflix.dao;

import com.netflix.model.Watchlist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class WatchlistDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public WatchlistDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Adds a movie/show to a profile's watchlist
     */
    public void add(int profileId, int titleId) {
        String sql = "INSERT INTO watchlist (profile_id, title_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, profileId, titleId);
    }

    /**
     * Removes an item from the watchlist
     */
    public void remove(int profileId, int titleId) {
        String sql = "DELETE FROM watchlist WHERE profile_id = ? AND title_id = ?";
        jdbcTemplate.update(sql, profileId, titleId);
    }

    /**
     * Retrieves all watchlist items for a profile
     */
    public List<Watchlist> findByProfileId(int profileId) {
        String sql = "SELECT watchlist_id, profile_id, title_id, added_date FROM watchlist WHERE profile_id = ? ORDER BY added_date DESC";
        return jdbcTemplate.query(sql, new WatchlistRowMapper(), profileId);
    }

    /**
     * Checks if a title is in a profile's watchlist
     */
    public boolean exists(int profileId, int titleId) {
        String sql = "SELECT COUNT(*) FROM watchlist WHERE profile_id = ? AND title_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, profileId, titleId);
        return count != null && count > 0;
    }

    /**
     * Gets a specific watchlist item
     */
    public Watchlist findByProfileAndTitle(int profileId, int titleId) {
        String sql = "SELECT watchlist_id, profile_id, title_id, added_date FROM watchlist WHERE profile_id = ? AND title_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new WatchlistRowMapper(), profileId, titleId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Counts the number of items in a profile's watchlist
     */
    public int countByProfileId(int profileId) {
        String sql = "SELECT COUNT(*) FROM watchlist WHERE profile_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, profileId);
    }

    /**
     * Clears all items from a profile's watchlist
     */
    public void clearByProfileId(int profileId) {
        String sql = "DELETE FROM watchlist WHERE profile_id = ?";
        jdbcTemplate.update(sql, profileId);
    }

    /**
     * Row mapper for Watchlist objects
     */
    private static class WatchlistRowMapper implements RowMapper<Watchlist> {
        @Override
        public Watchlist mapRow(ResultSet rs, int rowNum) throws SQLException {
            Watchlist watchlist = new Watchlist();
            watchlist.setWatchlistId(rs.getInt("watchlist_id"));
            watchlist.setProfileId(rs.getInt("profile_id"));
            watchlist.setTitleId(rs.getInt("title_id"));

            Timestamp timestamp = rs.getTimestamp("added_date");
            if (timestamp != null) {
                watchlist.setAddedDate(timestamp.toLocalDateTime());
            }

            return watchlist;
        }
    }
}