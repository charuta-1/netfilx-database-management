package com.netflix.dao;

import com.netflix.model.User;
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
import java.util.List;

@Repository
public class UserDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDAO(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Inserts a new user into the database and returns the generated user_id
     */
    public int save(User user) {
        String sql = "INSERT INTO users (email, username, password_hash, date_of_birth) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPasswordHash());

            if (user.getDateOfBirth() != null) {
                ps.setDate(4, java.sql.Date.valueOf(user.getDateOfBirth()));
            } else {
                ps.setNull(4, java.sql.Types.DATE);
            }

            return ps;
        }, keyHolder);

        // Return the generated user_id with null check
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new RuntimeException("Failed to retrieve generated user ID");
        }
        return key.intValue();
    }

    /**
     * Finds a user by their email address
     */
    public User findByEmail(String email) {
        String sql = "SELECT user_id, email, username, password_hash, date_of_birth, created_at FROM users WHERE email = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new UserRowMapper(), email);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Finds a user by their username
     */
    public User findByUsername(String username) {
        String sql = "SELECT user_id, email, username, password_hash, date_of_birth, created_at FROM users WHERE username = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new UserRowMapper(), username);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Finds a user by their user ID
     */
    public User findById(int userId) {
        String sql = "SELECT user_id, email, username, password_hash, date_of_birth, created_at FROM users WHERE user_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new UserRowMapper(), userId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Updates user information
     */
    public void update(User user) {
        String sql = "UPDATE users SET email = ?, username = ?, password_hash = ?, date_of_birth = ? WHERE user_id = ?";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPasswordHash());
            if (user.getDateOfBirth() != null) {
                ps.setDate(4, java.sql.Date.valueOf(user.getDateOfBirth()));
            } else {
                ps.setNull(4, java.sql.Types.DATE);
            }
            ps.setInt(5, user.getUserId());
            return ps;
        });
    }

    /**
     * Deletes a user by ID
     */
    public void deleteById(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }

    /**
     * Get all users
     */
    public List<User> findAll() {
        String sql = "SELECT user_id, email, username, password_hash, date_of_birth, created_at FROM users";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM users";
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
        return result != null ? result : 0;
    }

    /**
     * Row mapper for User objects
     */
    private static class UserRowMapper implements RowMapper<User> {

        @Override
        public User mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setUserId(rs.getInt("user_id"));
            user.setEmail(rs.getString("email"));
            user.setUsername(rs.getString("username"));
            user.setPasswordHash(rs.getString("password_hash"));

            java.sql.Date dob = rs.getDate("date_of_birth");
            if (dob != null) {
                user.setDateOfBirth(dob.toLocalDate());
            }

            Timestamp timestamp = rs.getTimestamp("created_at");
            if (timestamp != null) {
                user.setCreatedAt(timestamp.toLocalDateTime());
            }

            return user;
        }
    }
}