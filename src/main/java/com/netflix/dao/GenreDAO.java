package com.netflix.dao;

import com.netflix.model.Genre;
import com.netflix.util.DatabaseConnection;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GenreDAO {

    // Create a new genre
    public boolean insertGenre(Genre genre) {
        String sql = "INSERT INTO genre (name) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, genre.getName());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error inserting genre: " + e.getMessage());
            return false;
        }
    }

    // Get all genres
    public List<Genre> getAllGenres() {
        List<Genre> genres = new ArrayList<>();
        String sql = "SELECT * FROM genre ORDER BY name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Genre genre = new Genre();
                genre.setGenreId(rs.getInt("genre_id"));
                genre.setName(rs.getString("name"));
                genres.add(genre);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching genres: " + e.getMessage());
        }

        return genres;
    }

    // Get genre by ID
    public Genre getGenreById(int genreId) {
        String sql = "SELECT * FROM genre WHERE genre_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, genreId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Genre genre = new Genre();
                genre.setGenreId(rs.getInt("genre_id"));
                genre.setName(rs.getString("name"));
                return genre;
            }

        } catch (SQLException e) {
            System.err.println("Error fetching genre by ID: " + e.getMessage());
        }

        return null;
    }

    // Find or create genre
    public int findOrCreateGenre(String name) {
        // First try to find existing
        String selectSql = "SELECT genre_id FROM genre WHERE name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {

            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("genre_id");
            }

        } catch (SQLException e) {
            System.err.println("Error finding genre: " + e.getMessage());
        }

        // If not found, create new
        String insertSql = "INSERT INTO genre (name) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, name);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error creating genre: " + e.getMessage());
        }

        return -1;
    }

    // Delete genre
    public boolean deleteGenre(int genreId) {
        String sql = "DELETE FROM genre WHERE genre_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, genreId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting genre: " + e.getMessage());
            return false;
        }
    }
}