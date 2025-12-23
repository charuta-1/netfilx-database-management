package com.netflix.controller;

import com.netflix.model.UserRating;
import com.netflix.service.UserRatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ratings")
@CrossOrigin(origins = "*")
public class UserRatingController {

    private final UserRatingService userRatingService;

    @Autowired
    public UserRatingController(UserRatingService userRatingService) {
        this.userRatingService = userRatingService;
    }

    /**
     * Rate a title
     */
    @PostMapping
    public ResponseEntity<?> rateTitle(@RequestBody RatingRequest request) {
        try {
            UserRating.RatingValue ratingValue = UserRating.RatingValue.fromString(request.getRatingValue());
            userRatingService.rateTitle(request.getProfileId(), request.getTitleId(), ratingValue);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rating saved");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid rating value");

            return ResponseEntity.badRequest().body(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get user's rating for a title
     */
    @GetMapping("/{profileId}/{titleId}")
    public ResponseEntity<?> getUserRating(@PathVariable int profileId, @PathVariable int titleId) {
        Optional<UserRating> rating = userRatingService.getUserRating(profileId, titleId);

        if (rating.isPresent()) {
            return ResponseEntity.ok(rating.get());
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("hasRating", false);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Remove user's rating
     */
    @DeleteMapping("/{profileId}/{titleId}")
    public ResponseEntity<?> removeRating(@PathVariable int profileId, @PathVariable int titleId) {
        try {
            userRatingService.removeRating(profileId, titleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rating removed");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get all ratings by a profile
     */
    @GetMapping("/profile/{profileId}")
    public ResponseEntity<List<UserRating>> getProfileRatings(@PathVariable int profileId) {
        List<UserRating> ratings = userRatingService.getProfileRatings(profileId);
        return ResponseEntity.ok(ratings);
    }

    /**
     * Get all ratings for a title
     */
    @GetMapping("/title/{titleId}")
    public ResponseEntity<List<UserRating>> getTitleRatings(@PathVariable int titleId) {
        List<UserRating> ratings = userRatingService.getTitleRatings(titleId);
        return ResponseEntity.ok(ratings);
    }

    /**
     * Get rating statistics for a title
     */
    @GetMapping("/title/{titleId}/stats")
    public ResponseEntity<UserRatingService.TitleRatingStats> getTitleRatingStats(@PathVariable int titleId) {
        UserRatingService.TitleRatingStats stats = userRatingService.getTitleRatingStats(titleId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Toggle rating (thumbs up/down)
     */
    @PostMapping("/toggle")
    public ResponseEntity<?> toggleRating(@RequestBody RatingRequest request) {
        try {
            UserRating.RatingValue ratingValue = UserRating.RatingValue.fromString(request.getRatingValue());
            String result = userRatingService.toggleRating(request.getProfileId(), request.getTitleId(), ratingValue);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("action", result);
            response.put("message", getToggleMessage(result, ratingValue));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid rating value");

            return ResponseEntity.badRequest().body(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    private String getToggleMessage(String action, UserRating.RatingValue rating) {
        String ratingText = rating == UserRating.RatingValue.THUMBS_UP ? "thumbs up" : "thumbs down";

        switch (action) {
            case "added":
                return "Rated " + ratingText;
            case "updated":
                return "Updated to " + ratingText;
            case "removed":
                return "Rating removed";
            default:
                return "Rating updated";
        }
    }

    // Request DTO
    public static class RatingRequest {
        private int profileId;
        private int titleId;
        private String ratingValue; // "thumbs_up" or "thumbs_down"

        // Getters and setters
        public int getProfileId() { return profileId; }
        public void setProfileId(int profileId) { this.profileId = profileId; }
        public int getTitleId() { return titleId; }
        public void setTitleId(int titleId) { this.titleId = titleId; }
        public String getRatingValue() { return ratingValue; }
        public void setRatingValue(String ratingValue) { this.ratingValue = ratingValue; }
    }
}