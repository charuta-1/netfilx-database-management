package com.netflix.dao;

import com.netflix.model.Director;
import com.netflix.util.DatabaseConnection;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DirectorDAO {

    // Create a new director
    public boolean insertDirector(Director director) {
        String sql = "INSERT INTO director (full_name) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, director.getFullName());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error inserting director: " + e.getMessage());
            return false;
        }
    }

    // Get all directors
    public List<Director> getAllDirectors() {
        List<Director> directors = new ArrayList<>();
        String sql = "SELECT * FROM director ORDER BY full_name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Director director = new Director();
                director.setDirectorId(rs.getInt("director_id"));
                director.setFullName(rs.getString("full_name"));
                directors.add(director);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching directors: " + e.getMessage());
        }

        return directors;
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) AS total FROM director";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            System.err.println("Error counting directors: " + e.getMessage());
        }
        return 0;
    }

    // Get director by ID
    public Director getDirectorById(int directorId) {
        String sql = "SELECT * FROM director WHERE director_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, directorId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Director director = new Director();
                director.setDirectorId(rs.getInt("director_id"));
                director.setFullName(rs.getString("full_name"));
                return director;
            }

        } catch (SQLException e) {
            System.err.println("Error fetching director by ID: " + e.getMessage());
        }

        return null;
    }

    // Search directors by name
    public List<Director> searchDirectorsByName(String name) {
        List<Director> directors = new ArrayList<>();
        String sql = "SELECT * FROM director WHERE full_name LIKE ? ORDER BY full_name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + name + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Director director = new Director();
                director.setDirectorId(rs.getInt("director_id"));
                director.setFullName(rs.getString("full_name"));
                directors.add(director);
            }

        } catch (SQLException e) {
            System.err.println("Error searching directors: " + e.getMessage());
        }

        return directors;
    }

    // Find or create director
    public int findOrCreateDirector(String fullName) {
        // First try to find existing
        String selectSql = "SELECT director_id FROM director WHERE full_name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {

            pstmt.setString(1, fullName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("director_id");
            }

        } catch (SQLException e) {
            System.err.println("Error finding director: " + e.getMessage());
        }

        // If not found, create new
        String insertSql = "INSERT INTO director (full_name) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, fullName);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error creating director: " + e.getMessage());
        }

        return -1;
    }

    // Update director
    public boolean updateDirector(Director director) {
        String sql = "UPDATE director SET full_name = ? WHERE director_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, director.getFullName());
            pstmt.setInt(2, director.getDirectorId());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating director: " + e.getMessage());
            return false;
        }
    }

    // Delete director
    public boolean deleteDirector(int directorId) {
        String sql = "DELETE FROM director WHERE director_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, directorId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting director: " + e.getMessage());
            return false;
        }
    }
}