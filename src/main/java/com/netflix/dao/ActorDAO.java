package com.netflix.dao;

import com.netflix.model.Actor;
import com.netflix.util.DatabaseConnection;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ActorDAO {

    // Create a new actor
    public boolean insertActor(Actor actor) {
        String sql = "INSERT INTO actor (full_name) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, actor.getFullName());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error inserting actor: " + e.getMessage());
            return false;
        }
    }

    // Get all actors
    public List<Actor> getAllActors() {
        List<Actor> actors = new ArrayList<>();
        String sql = "SELECT * FROM actor ORDER BY full_name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Actor actor = new Actor();
                actor.setActorId(rs.getInt("actor_id"));
                actor.setFullName(rs.getString("full_name"));
                actors.add(actor);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching actors: " + e.getMessage());
        }

        return actors;
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) AS total FROM actor";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (SQLException e) {
            System.err.println("Error counting actors: " + e.getMessage());
        }
        return 0;
    }

    // Get actor by ID
    public Actor getActorById(int actorId) {
        String sql = "SELECT * FROM actor WHERE actor_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, actorId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Actor actor = new Actor();
                actor.setActorId(rs.getInt("actor_id"));
                actor.setFullName(rs.getString("full_name"));
                return actor;
            }

        } catch (SQLException e) {
            System.err.println("Error fetching actor by ID: " + e.getMessage());
        }

        return null;
    }

    // Search actors by name
    public List<Actor> searchActorsByName(String name) {
        List<Actor> actors = new ArrayList<>();
        String sql = "SELECT * FROM actor WHERE full_name LIKE ? ORDER BY full_name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + name + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Actor actor = new Actor();
                actor.setActorId(rs.getInt("actor_id"));
                actor.setFullName(rs.getString("full_name"));
                actors.add(actor);
            }

        } catch (SQLException e) {
            System.err.println("Error searching actors: " + e.getMessage());
        }

        return actors;
    }

    // Find or create actor
    public int findOrCreateActor(String fullName) {
        // First try to find existing
        String selectSql = "SELECT actor_id FROM actor WHERE full_name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {

            pstmt.setString(1, fullName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("actor_id");
            }

        } catch (SQLException e) {
            System.err.println("Error finding actor: " + e.getMessage());
        }

        // If not found, create new
        String insertSql = "INSERT INTO actor (full_name) VALUES (?)";
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
            System.err.println("Error creating actor: " + e.getMessage());
        }

        return -1;
    }

    // Update actor
    public boolean updateActor(Actor actor) {
        String sql = "UPDATE actor SET full_name = ? WHERE actor_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, actor.getFullName());
            pstmt.setInt(2, actor.getActorId());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating actor: " + e.getMessage());
            return false;
        }
    }

    // Delete actor
    public boolean deleteActor(int actorId) {
        String sql = "DELETE FROM actor WHERE actor_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, actorId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting actor: " + e.getMessage());
            return false;
        }
    }
}