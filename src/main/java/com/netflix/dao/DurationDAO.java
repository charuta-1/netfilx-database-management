package com.netflix.dao;

import com.netflix.model.Duration;
import com.netflix.util.DatabaseConnection;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DurationDAO {

    private String normalizeUnit(String unit) {
        if (unit == null) return "min";
        String u = unit.trim().toLowerCase();
        if (u.endsWith("s")) u = u.substring(0, u.length()-1); // seasons -> season, mins -> min
        if (u.startsWith("min") || u.equals("minute")) return "min";
        if (u.startsWith("season")) return "season";
        // default to min
        return "min";
    }

    // Create a new duration
    public boolean insertDuration(Duration duration) {
        String sql = "INSERT INTO duration (unit, value) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, normalizeUnit(duration.getUnit()));
            pstmt.setInt(2, duration.getValue());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error inserting duration: " + e.getMessage());
            return false;
        }
    }

    // Get all durations
    public List<Duration> getAllDurations() {
        List<Duration> durations = new ArrayList<>();
        String sql = "SELECT * FROM duration ORDER BY duration_id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Duration duration = new Duration();
                duration.setDurationId(rs.getInt("duration_id"));
                duration.setUnit(rs.getString("unit"));
                duration.setValue(rs.getInt("value"));
                durations.add(duration);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching durations: " + e.getMessage());
        }

        return durations;
    }

    // Get duration by ID
    public Duration getDurationById(int durationId) {
        String sql = "SELECT * FROM duration WHERE duration_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, durationId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Duration duration = new Duration();
                duration.setDurationId(rs.getInt("duration_id"));
                duration.setUnit(rs.getString("unit"));
                duration.setValue(rs.getInt("value"));
                return duration;
            }

        } catch (SQLException e) {
            System.err.println("Error fetching duration by ID: " + e.getMessage());
        }

        return null;
    }

    // Find or create duration
    public int findOrCreateDuration(String unit, int value) {
        String norm = normalizeUnit(unit);
        // First try to find existing
        String selectSql = "SELECT duration_id FROM duration WHERE unit = ? AND value = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {

            pstmt.setString(1, norm);
            pstmt.setInt(2, value);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("duration_id");
            }

        } catch (SQLException e) {
            System.err.println("Error finding duration: " + e.getMessage());
        }

        // If not found, create new
        String insertSql = "INSERT INTO duration (unit, value) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, norm);
            pstmt.setInt(2, value);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error creating duration: " + e.getMessage());
        }

        return -1;
    }

    // Find duration by unit and value
    public Duration findByUnitAndValue(String unit, int value) {
        String norm = normalizeUnit(unit);
        String sql = "SELECT * FROM duration WHERE unit = ? AND value = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, norm);
            pstmt.setInt(2, value);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Duration duration = new Duration();
                duration.setDurationId(rs.getInt("duration_id"));
                duration.setUnit(rs.getString("unit"));
                duration.setValue(rs.getInt("value"));
                return duration;
            }

        } catch (SQLException e) {
            System.err.println("Error finding duration by unit and value: " + e.getMessage());
        }

        return null;
    }
}