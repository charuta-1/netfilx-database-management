package com.netflix.service;

import com.netflix.dao.UserProfileDAO;
import com.netflix.dao.WatchlistDAO;
import com.netflix.dao.WatchHistoryDAO;
import com.netflix.dao.UserRatingDAO;
import com.netflix.model.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserProfileService {

    private final UserProfileDAO userProfileDAO;
    private final WatchlistDAO watchlistDAO;
    private final WatchHistoryDAO watchHistoryDAO;
    private final UserRatingDAO userRatingDAO;

    private static final int MAX_PROFILES_PER_USER = 5;

    @Autowired
    public UserProfileService(UserProfileDAO userProfileDAO, WatchlistDAO watchlistDAO,
                             WatchHistoryDAO watchHistoryDAO, UserRatingDAO userRatingDAO) {
        this.userProfileDAO = userProfileDAO;
        this.watchlistDAO = watchlistDAO;
        this.watchHistoryDAO = watchHistoryDAO;
        this.userRatingDAO = userRatingDAO;
    }

    /**
     * Create a new user profile
     */
    public UserProfile createProfile(int userId, String profileName) {
        // Check profile limit
        int profileCount = userProfileDAO.countByUserId(userId);
        if (profileCount >= MAX_PROFILES_PER_USER) {
            throw new RuntimeException("Maximum number of profiles reached");
        }

        // Create new profile
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setProfileName(profileName);

        userProfileDAO.save(profile);

        // Return the saved profile
        List<UserProfile> profiles = userProfileDAO.findByUserId(userId);
        return profiles.stream()
                .filter(p -> p.getProfileName().equals(profileName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all profiles for a user
     */
    public List<UserProfile> getProfilesByUserId(int userId) {
        return userProfileDAO.findByUserId(userId);
    }

    /**
     * Get profile by ID
     */
    public UserProfile getProfileById(int profileId) {
        return userProfileDAO.findById(profileId);
    }

    /**
     * Update profile name
     */
    public UserProfile updateProfile(int profileId, String profileName) {
        UserProfile profile = userProfileDAO.findById(profileId);
        if (profile == null) {
            throw new RuntimeException("Profile not found");
        }

        profile.setProfileName(profileName);
        userProfileDAO.update(profile);

        return profile;
    }

    /**
     * Delete profile and all associated data
     */
    public void deleteProfile(int profileId) {
        UserProfile profile = userProfileDAO.findById(profileId);
        if (profile == null) {
            throw new RuntimeException("Profile not found");
        }

        // Delete associated data
        watchlistDAO.clearByProfileId(profileId);
        userRatingDAO.deleteByProfileId(profileId);
        // Note: Watch history might be kept for analytics, but could be deleted if needed

        // Delete profile
        userProfileDAO.deleteById(profileId);
    }

    /**
     * Get profile statistics
     */
    public ProfileStats getProfileStats(int profileId) {
        UserProfile profile = userProfileDAO.findById(profileId);
        if (profile == null) {
            throw new RuntimeException("Profile not found");
        }

        int watchlistCount = watchlistDAO.countByProfileId(profileId);
        int watchHistoryCount = watchHistoryDAO.countByProfileId(profileId);
        UserRatingDAO.RatingStats ratingStats = userRatingDAO.getProfileRatingStats(profileId);

        return new ProfileStats(profile, watchlistCount, watchHistoryCount, ratingStats);
    }

    /**
     * Helper class for profile statistics
     */
    public static class ProfileStats {
        private final UserProfile profile;
        private final int watchlistCount;
        private final int watchHistoryCount;
        private final UserRatingDAO.RatingStats ratingStats;

        public ProfileStats(UserProfile profile, int watchlistCount, int watchHistoryCount, 
                           UserRatingDAO.RatingStats ratingStats) {
            this.profile = profile;
            this.watchlistCount = watchlistCount;
            this.watchHistoryCount = watchHistoryCount;
            this.ratingStats = ratingStats;
        }

        // Getters
        public UserProfile getProfile() { return profile; }
        public int getWatchlistCount() { return watchlistCount; }
        public int getWatchHistoryCount() { return watchHistoryCount; }
        public UserRatingDAO.RatingStats getRatingStats() { return ratingStats; }
    }
}