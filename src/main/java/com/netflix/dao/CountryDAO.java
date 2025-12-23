package com.netflix.dao;

import com.netflix.model.Country;
import com.netflix.util.DatabaseConnection;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CountryDAO {

    // Create a new country
    public boolean insertCountry(Country country) {
        String sql = "INSERT INTO country (name) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, country.getName());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error inserting country: " + e.getMessage());
            return false;
        }
    }

    // Get all countries
    public List<Country> getAllCountries() {
        List<Country> countries = new ArrayList<>();
        String sql = "SELECT * FROM country ORDER BY name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Country country = new Country();
                country.setCountryId(rs.getInt("country_id"));
                country.setName(rs.getString("name"));
                countries.add(country);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching countries: " + e.getMessage());
        }

        return countries;
    }

    // Get country by ID
    public Country getCountryById(int countryId) {
        String sql = "SELECT * FROM country WHERE country_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, countryId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Country country = new Country();
                country.setCountryId(rs.getInt("country_id"));
                country.setName(rs.getString("name"));
                return country;
            }

        } catch (SQLException e) {
            System.err.println("Error fetching country by ID: " + e.getMessage());
        }

        return null;
    }

    // Find or create country
    public int findOrCreateCountry(String name) {
        // First try to find existing
        String selectSql = "SELECT country_id FROM country WHERE name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql)) {

            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("country_id");
            }

        } catch (SQLException e) {
            System.err.println("Error finding country: " + e.getMessage());
        }

        // If not found, create new
        String insertSql = "INSERT INTO country (name) VALUES (?)";
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
            System.err.println("Error creating country: " + e.getMessage());
        }

        return -1;
    }

    // Delete country
    public boolean deleteCountry(int countryId) {
        String sql = "DELETE FROM country WHERE country_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, countryId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting country: " + e.getMessage());
            return false;
        }
    }
}