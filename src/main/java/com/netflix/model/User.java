package com.netflix.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

public class User {
    private int userId;
    private String email;
    private String username;
    private String passwordHash;
    private LocalDate dateOfBirth; // from users.date_of_birth
    private LocalDateTime createdAt;

    public User() {}

    public User(int userId, String email, String username, String passwordHash, LocalDateTime createdAt) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * Calculate age in years from dateOfBirth. Returns 0 if DOB is null or in the future.
     */
    public int calculateAge() {
        if (dateOfBirth == null) return 0;
        LocalDate today = LocalDate.now();
        if (dateOfBirth.isAfter(today)) return 0;
        return Period.between(dateOfBirth, today).getYears();
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", passwordHash='" + passwordHash + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}