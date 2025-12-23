package com.netflix.service;

import com.netflix.dao.UserDAO;
import com.netflix.dao.UserProfileDAO;
import com.netflix.model.User;
import com.netflix.model.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserDAO userDAO;
    private final UserProfileDAO userProfileDAO;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserDAO userDAO, UserProfileDAO userProfileDAO) {
        this.userDAO = userDAO;
        this.userProfileDAO = userProfileDAO;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Register a new user (legacy signature)
     */
    public User registerUser(String email, String username, String password) {
        return registerUser(email, username, password, null);
    }

    /**
     * Register a new user with optional date of birth
     */
    public User registerUser(String email, String username, String password, LocalDate dateOfBirth) {
        try {
            // Check if user already exists
            if (userDAO.findByEmail(email) != null) {
                throw new RuntimeException("User with this email already exists");
            }

            if (userDAO.findByUsername(username) != null) {
                throw new RuntimeException("User with this username already exists");
            }

            // Create new user
            User user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setDateOfBirth(dateOfBirth);

            // Save and get the generated user_id
            int userId = userDAO.save(user);
            user.setUserId(userId);

            // Return user without password hash
            User savedUser = userDAO.findById(userId);
            if (savedUser != null) {
                savedUser.setPasswordHash(null);
                return savedUser;
            } else {
                throw new RuntimeException("Failed to retrieve saved user");
            }
        } catch (Exception e) {
            System.err.println("Error registering user: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to register user: " + e.getMessage());
        }
    }

    /**
     * Authenticate user login
     */
    public User authenticateUser(String email, String password) {
        try {
            User user = userDAO.findByEmail(email);

            if (user == null) {
                throw new RuntimeException("User not found");
            }

            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new RuntimeException("Invalid password");
            }

            // Return user without password hash
            user.setPasswordHash(null);
            return user;
        } catch (Exception e) {
            System.err.println("Error authenticating user: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Get user by ID
     */
    public User getUserById(int userId) {
        User user = userDAO.findById(userId);
        if (user != null) {
            user.setPasswordHash(null);
        }
        return user;
    }

    /**
     * Retrieve all users without password hashes (admin use)
     */
    public List<User> getAllUsers() {
        return userDAO.findAll()
                .stream()
                .peek(user -> user.setPasswordHash(null))
                .collect(Collectors.toList());
    }

    /**
     * Update user information
     */
    public User updateUser(int userId, String email, String username) {
        return updateUser(userId, email, username, null, false);
    }

    public User updateUser(int userId, String email, String username, LocalDate dateOfBirth) {
        return updateUser(userId, email, username, dateOfBirth, true);
    }

    public User updateUser(int userId, String email, String username, LocalDate dateOfBirth, boolean updateDateOfBirth) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

    String sanitizedEmail = email != null ? email.trim() : null;
    String sanitizedUsername = username != null ? username.trim() : null;

    ensureUniqueEmail(userId, sanitizedEmail);
    ensureUniqueUsername(userId, sanitizedUsername);

    user.setEmail(sanitizedEmail);
    user.setUsername(sanitizedUsername);
        if (updateDateOfBirth) {
            user.setDateOfBirth(dateOfBirth);
        }

        userDAO.update(user);

        user.setPasswordHash(null);
        return user;
    }

    public void adminSetPassword(int userId, String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("New password is required");
        }

        User user = userDAO.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userDAO.update(user);
    }

    /**
     * Change user password
     */
    public void changePassword(int userId, String oldPassword, String newPassword) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userDAO.update(user);
    }

    /**
     * Delete user account
     */
    public void deleteUser(int userId) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        userDAO.deleteById(userId);
    }

    /**
     * Get user profiles
     */
    public List<UserProfile> getUserProfiles(int userId) {
        return userProfileDAO.findByUserId(userId);
    }

    private void ensureUniqueEmail(int userId, String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        User existingByEmail = userDAO.findByEmail(email.trim());
        if (existingByEmail != null && existingByEmail.getUserId() != userId) {
            throw new RuntimeException("Another user already uses this email");
        }
    }

    private void ensureUniqueUsername(int userId, String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Username is required");
        }
        User existingByUsername = userDAO.findByUsername(username.trim());
        if (existingByUsername != null && existingByUsername.getUserId() != userId) {
            throw new RuntimeException("Another user already uses this username");
        }
    }
}