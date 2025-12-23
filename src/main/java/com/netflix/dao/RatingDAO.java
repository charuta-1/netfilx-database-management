package com.netflix.dao;

import com.netflix.model.Rating;
import com.netflix.util.DatabaseConnection;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RatingDAO {

    // Create a new rating
    public boolean insertRating(Rating rating) {
        String sql = "INSERT INTO rating (code, description) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, rating.getCode());
            pstmt.setString(2, rating.getDescription());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error inserting rating: " + e.getMessage());
            return false;
        }
    }

    // Get all ratings
    public List<Rating> getAllRatings() {
        List<Rating> ratings = new ArrayList<>();
        String sql = "SELECT * FROM rating ORDER BY rating_id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Rating rating = new Rating();
                rating.setRatingId(rs.getInt("rating_id"));
                rating.setCode(rs.getString("code"));
                rating.setDescription(rs.getString("description"));
                ratings.add(rating);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching ratings: " + e.getMessage());
        }

        return ratings;
    }

    // Get rating by ID
    public Rating getRatingById(int ratingId) {
        String sql = "SELECT * FROM rating WHERE rating_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ratingId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Rating rating = new Rating();
                rating.setRatingId(rs.getInt("rating_id"));
                rating.setCode(rs.getString("code"));
                rating.setDescription(rs.getString("description"));
                return rating;
            }

        } catch (SQLException e) {
            System.err.println("Error fetching rating by ID: " + e.getMessage());
        }

        return null;
    }

    // Update rating
    public boolean updateRating(Rating rating) {
        String sql = "UPDATE rating SET code = ?, description = ? WHERE rating_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, rating.getCode());
            pstmt.setString(2, rating.getDescription());
            pstmt.setInt(3, rating.getRatingId());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating rating: " + e.getMessage());
            return false;
        }
    }

    // Delete rating
    public boolean deleteRating(int ratingId) {
        String sql = "DELETE FROM rating WHERE rating_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ratingId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting rating: " + e.getMessage());
            return false;
        }
    }

    // Get rating by code
    public Rating getRatingByCode(String code) {
        String sql = "SELECT * FROM rating WHERE code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Rating rating = new Rating();
                rating.setRatingId(rs.getInt("rating_id"));
                rating.setCode(rs.getString("code"));
                rating.setDescription(rs.getString("description"));
                return rating;
            }

        } catch (SQLException e) {
            System.err.println("Error fetching rating by code: " + e.getMessage());
        }

        return null;
    }

    // Alias method for consistency
    public Rating findByCode(String code) {
        return getRatingByCode(code);
    }
}