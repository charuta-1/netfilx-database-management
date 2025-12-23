package com.netflix.controller;

import com.netflix.model.UserProfile;
import com.netflix.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profiles")
@CrossOrigin(origins = "*")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Autowired
    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * Create new profile
     */
    @PostMapping
    public ResponseEntity<?> createProfile(@RequestBody ProfileCreateRequest request) {
        try {
            UserProfile profile = userProfileService.createProfile(
                request.getUserId(),
                request.getProfileName()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile created successfully");
            response.put("profile", profile);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get profiles by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserProfile>> getProfilesByUserId(@PathVariable int userId) {
        List<UserProfile> profiles = userProfileService.getProfilesByUserId(userId);
        return ResponseEntity.ok(profiles);
    }

    /**
     * Get profile by ID
     */
    @GetMapping("/{profileId}")
    public ResponseEntity<?> getProfile(@PathVariable int profileId) {
        UserProfile profile = userProfileService.getProfileById(profileId);
        if (profile == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Profile not found");
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profile);
    }

    /**
     * Update profile
     */
    @PutMapping("/{profileId}")
    public ResponseEntity<?> updateProfile(@PathVariable int profileId, @RequestBody ProfileUpdateRequest request) {
        try {
            UserProfile profile = userProfileService.updateProfile(profileId, request.getProfileName());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("profile", profile);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Delete profile
     */
    @DeleteMapping("/{profileId}")
    public ResponseEntity<?> deleteProfile(@PathVariable int profileId) {
        try {
            userProfileService.deleteProfile(profileId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile deleted successfully");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    // Request DTOs
    public static class ProfileCreateRequest {
        private int userId;
        private String profileName;
        private String maturityRatingOverride;

        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        public String getProfileName() { return profileName; }
        public void setProfileName(String profileName) { this.profileName = profileName; }
        public String getMaturityRatingOverride() { return maturityRatingOverride; }
        public void setMaturityRatingOverride(String maturityRatingOverride) {
            this.maturityRatingOverride = maturityRatingOverride;
        }
    }

    public static class ProfileUpdateRequest {
        private String profileName;
        private String maturityRatingOverride;

        public String getProfileName() { return profileName; }
        public void setProfileName(String profileName) { this.profileName = profileName; }
        public String getMaturityRatingOverride() { return maturityRatingOverride; }
        public void setMaturityRatingOverride(String maturityRatingOverride) {
            this.maturityRatingOverride = maturityRatingOverride;
        }
    }
}