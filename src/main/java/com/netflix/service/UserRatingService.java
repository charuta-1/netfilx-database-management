package com.netflix.service;

import com.netflix.dao.UserRatingDAO;
import com.netflix.model.UserRating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserRatingService {

    private final UserRatingDAO userRatingDAO;

    @Autowired
    public UserRatingService(UserRatingDAO userRatingDAO) {
        this.userRatingDAO = userRatingDAO;
    }

    /**
     * Rate a title (thumbs up/down)
     */
    public void rateTitle(int profileId, int titleId, UserRating.RatingValue ratingValue) {
        UserRating rating = new UserRating();
        rating.setProfileId(profileId);
        rating.setTitleId(titleId);
        rating.setRatingValue(ratingValue);

        userRatingDAO.save(rating);
    }

    /**
     * Get user's rating for a title
     */
    public Optional<UserRating> getUserRating(int profileId, int titleId) {
        return userRatingDAO.findByProfileAndTitle(profileId, titleId);
    }

    /**
     * Remove user's rating for a title
     */
    public void removeRating(int profileId, int titleId) {
        userRatingDAO.remove(profileId, titleId);
    }

    /**
     * Get all ratings by a profile
     */
    public List<UserRating> getProfileRatings(int profileId) {
        return userRatingDAO.findByProfileId(profileId);
    }

    /**
     * Get all ratings for a title
     */
    public List<UserRating> getTitleRatings(int titleId) {
        return userRatingDAO.findByTitleId(titleId);
    }

    /**
     * Get rating statistics for a title
     */
    public TitleRatingStats getTitleRatingStats(int titleId) {
        int thumbsUp = userRatingDAO.getThumbsUpCount(titleId);
        int thumbsDown = userRatingDAO.getThumbsDownCount(titleId);

        return new TitleRatingStats(titleId, thumbsUp, thumbsDown);
    }

    /**
     * Get profile rating statistics
     */
    public UserRatingDAO.RatingStats getProfileRatingStats(int profileId) {
        return userRatingDAO.getProfileRatingStats(profileId);
    }

    /**
     * Toggle rating (remove if same rating, update if different)
     */
    public String toggleRating(int profileId, int titleId, UserRating.RatingValue newRating) {
        Optional<UserRating> existingRating = userRatingDAO.findByProfileAndTitle(profileId, titleId);

        if (existingRating.isPresent()) {
            if (existingRating.get().getRatingValue() == newRating) {
                // Same rating - remove it
                userRatingDAO.remove(profileId, titleId);
                return "removed";
            } else {
                // Different rating - update it
                rateTitle(profileId, titleId, newRating);
                return "updated";
            }
        } else {
            // No existing rating - add new one
            rateTitle(profileId, titleId, newRating);
            return "added";
        }
    }

    /**
     * Helper class for title rating statistics
     */
    public static class TitleRatingStats {
        private final int titleId;
        private final int thumbsUp;
        private final int thumbsDown;

        public TitleRatingStats(int titleId, int thumbsUp, int thumbsDown) {
            this.titleId = titleId;
            this.thumbsUp = thumbsUp;
            this.thumbsDown = thumbsDown;
        }

        public int getTitleId() { return titleId; }
        public int getThumbsUp() { return thumbsUp; }
        public int getThumbsDown() { return thumbsDown; }
        public int getTotal() { return thumbsUp + thumbsDown; }
        public double getPositivePercentage() {
            int total = getTotal();
            return total > 0 ? (double) thumbsUp / total * 100 : 0;
        }
    }
}