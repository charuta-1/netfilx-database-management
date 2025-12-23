package com.netflix.service;

import com.netflix.dao.*;
import com.netflix.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final UserDAO userDAO;
    private final UserProfileDAO userProfileDAO;
    private final UserGenrePreferenceDAO userGenrePreferenceDAO;
    private final WatchHistoryDAO watchHistoryDAO;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public RecommendationService(UserDAO userDAO, 
                                UserProfileDAO userProfileDAO,
                                UserGenrePreferenceDAO userGenrePreferenceDAO,
                                WatchHistoryDAO watchHistoryDAO,
                                DataSource dataSource) {
        this.userDAO = userDAO;
        this.userProfileDAO = userProfileDAO;
        this.userGenrePreferenceDAO = userGenrePreferenceDAO;
        this.watchHistoryDAO = watchHistoryDAO;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Main method to get personalized recommendations for a user profile.
     * Implements hybrid recommendation strategy with age-based filtering.
     * 
     * @param profileId The profile ID to get recommendations for
     * @param limit Maximum number of recommendations to return
     * @return List of recommended titles
     */
    public List<Title> getPersonalizedRecommendations(int profileId, int limit) {
        // 1. Fetch the user profile
        UserProfile profile = userProfileDAO.findById(profileId);
        if (profile == null) {
            throw new RuntimeException("Profile not found: " + profileId);
        }

        // 2. Fetch the main user account to get date of birth
        User user = userDAO.findById(profile.getUserId());
        if (user == null) {
            throw new RuntimeException("User not found for profile: " + profileId);
        }

        // 3. Calculate age and determine allowed ratings
        int userAge = user.calculateAge();
        List<String> allowedRatings = getAllowedRatingsForAge(userAge, profile.getMaturityRatingOverride());

        // 4. Check if user has watch history
        List<WatchHistory> watchHistory = watchHistoryDAO.findByProfileId(profileId);

        if (!watchHistory.isEmpty()) {
            // User has watch history - use behavior-based recommendations
            return getBehaviorBasedRecommendations(profileId, allowedRatings, limit);
        } else {
            // New user without history - use genre preference recommendations
            return getNewUserRecommendations(profileId, allowedRatings, limit);
        }
    }

    /**
     * Determines appropriate content ratings based on user age and profile override.
     * 
     * @param age User's age
     * @param maturityRatingOverride Optional rating override from profile
     * @return List of allowed rating codes
     */
    private List<String> getAllowedRatingsForAge(int age, String maturityRatingOverride) {
        List<String> allowedRatings = new ArrayList<>();

        // If there's a profile-specific override, use that
        if (maturityRatingOverride != null && !maturityRatingOverride.trim().isEmpty()) {
            return getUpToRating(maturityRatingOverride);
        }

        // Age-based rating determination
        if (age < 7) {
            // Very young children
            allowedRatings.addAll(Arrays.asList("G", "TV-Y", "TV-Y7"));
        } else if (age < 13) {
            // Children
            allowedRatings.addAll(Arrays.asList("G", "PG", "TV-Y", "TV-Y7", "TV-G", "TV-PG"));
        } else if (age < 17) {
            // Teens
            allowedRatings.addAll(Arrays.asList("G", "PG", "PG-13", "TV-Y", "TV-Y7", "TV-G", "TV-PG", "TV-14"));
        } else {
            // Adults - all ratings allowed
            allowedRatings.addAll(Arrays.asList("G", "PG", "PG-13", "R", "NC-17", "TV-Y", "TV-Y7", "TV-G", "TV-PG", "TV-14", "TV-MA"));
        }

        return allowedRatings;
    }

    /**
     * Helper method to get all ratings up to and including the specified rating.
     */
    private List<String> getUpToRating(String maxRating) {
        List<String> allRatings = Arrays.asList("G", "PG", "PG-13", "R", "NC-17", "TV-Y", "TV-Y7", "TV-G", "TV-PG", "TV-14", "TV-MA");
        List<String> restrictiveOrder = Arrays.asList("G", "TV-Y", "TV-Y7", "TV-G", "PG", "TV-PG", "PG-13", "TV-14", "R", "TV-MA", "NC-17");

        int maxIndex = restrictiveOrder.indexOf(maxRating);
        if (maxIndex == -1) {
            return allRatings; // If unknown rating, allow all
        }

        return restrictiveOrder.subList(0, maxIndex + 1);
    }

    /**
     * Get recommendations for users with watch history (behavior-based).
     * 
     * @param profileId Profile ID
     * @param allowedRatings List of allowed content ratings
     * @param limit Maximum recommendations to return
     * @return List of recommended titles
     */
    private List<Title> getBehaviorBasedRecommendations(int profileId, List<String> allowedRatings, int limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT t.*, r.code as rating_code, d.unit, d.value ");
        sql.append("FROM title t ");
        sql.append("LEFT JOIN rating r ON t.rating_id = r.rating_id ");
        sql.append("LEFT JOIN duration d ON t.duration_id = d.duration_id ");
        sql.append("JOIN title_genre tg ON t.title_id = tg.title_id ");
        sql.append("JOIN genre g ON tg.genre_id = g.genre_id ");
        sql.append("WHERE g.genre_id IN (");
        sql.append("  SELECT DISTINCT g2.genre_id ");
        sql.append("  FROM watch_history wh ");
        sql.append("  JOIN title t2 ON wh.title_id = t2.title_id ");
        sql.append("  JOIN title_genre tg2 ON t2.title_id = tg2.title_id ");
        sql.append("  JOIN genre g2 ON tg2.genre_id = g2.genre_id ");
        sql.append("  WHERE wh.profile_id = ? ");
        sql.append(") ");
        sql.append("AND t.title_id NOT IN (");
        sql.append("  SELECT title_id FROM watch_history WHERE profile_id = ?");
        sql.append(") ");

        // Add rating filter if we have allowed ratings
        if (!allowedRatings.isEmpty()) {
            sql.append("AND (r.code IN (");
            sql.append(String.join(",", Collections.nCopies(allowedRatings.size(), "?")));
            sql.append(") OR r.code IS NULL) ");
        }

        sql.append("ORDER BY t.release_year DESC, t.title ");
        sql.append("LIMIT ?");

        List<Object> params = new ArrayList<>();
        params.add(profileId); // For genre matching
        params.add(profileId); // For exclusion of already watched
        params.addAll(allowedRatings); // For rating filter
        params.add(limit); // For limit

        return jdbcTemplate.query(sql.toString(), new TitleRowMapper(), params.toArray());
    }

    /**
     * Get recommendations for new users without watch history (genre preference-based).
     * 
     * @param profileId Profile ID
     * @param allowedRatings List of allowed content ratings
     * @param limit Maximum recommendations to return
     * @return List of recommended titles
     */
    private List<Title> getNewUserRecommendations(int profileId, List<String> allowedRatings, int limit) {
        // Get user's preferred genres
        List<Integer> preferredGenres = userGenrePreferenceDAO.findGenreIdsByProfileId(profileId);

        if (preferredGenres.isEmpty()) {
            // No preferences set - return popular content for age
            return getPopularContentForAge(allowedRatings, limit);
        }

        // Build SQL for genre + age-based recommendations
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT t.*, r.code as rating_code, d.unit, d.value, ");
        sql.append("  COUNT(ur.rating_id) as rating_count, ");
        sql.append("  SUM(CASE WHEN ur.rating_value = 'thumbs_up' THEN 1 ELSE 0 END) as thumbs_up ");
        sql.append("FROM title t ");
        sql.append("LEFT JOIN rating r ON t.rating_id = r.rating_id ");
        sql.append("LEFT JOIN duration d ON t.duration_id = d.duration_id ");
        sql.append("JOIN title_genre tg ON t.title_id = tg.title_id ");
        sql.append("LEFT JOIN user_ratings ur ON t.title_id = ur.title_id ");
        sql.append("WHERE tg.genre_id IN (");
        sql.append(String.join(",", Collections.nCopies(preferredGenres.size(), "?")));
        sql.append(") ");

        // Add rating filter if we have allowed ratings
        if (!allowedRatings.isEmpty()) {
            sql.append("AND (r.code IN (");
            sql.append(String.join(",", Collections.nCopies(allowedRatings.size(), "?")));
            sql.append(") OR r.code IS NULL) ");
        }

        sql.append("GROUP BY t.title_id ");
        sql.append("ORDER BY ");
        sql.append("  (SUM(CASE WHEN ur.rating_value = 'thumbs_up' THEN 1 ELSE 0 END) / GREATEST(COUNT(ur.rating_id), 1)) DESC, ");
        sql.append("  rating_count DESC, ");
        sql.append("  t.release_year DESC ");
        sql.append("LIMIT ?");

        List<Object> params = new ArrayList<>();
        // Add preferred genre IDs
        for (Integer genreId : preferredGenres) {
            params.add(genreId);
        }
        // Add allowed ratings
        params.addAll(allowedRatings);
        // Add limit
        params.add(limit);

        return jdbcTemplate.query(sql.toString(), new TitleRowMapper(), params.toArray());
    }

    /**
     * Get popular content appropriate for age when no genre preferences are set.
     */
    private List<Title> getPopularContentForAge(List<String> allowedRatings, int limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT t.*, r.code as rating_code, d.unit, d.value, ");
        sql.append("  COUNT(ur.rating_id) as rating_count ");
        sql.append("FROM title t ");
        sql.append("LEFT JOIN rating r ON t.rating_id = r.rating_id ");
        sql.append("LEFT JOIN duration d ON t.duration_id = d.duration_id ");
        sql.append("LEFT JOIN user_ratings ur ON t.title_id = ur.title_id ");
        sql.append("WHERE 1=1 ");

        if (!allowedRatings.isEmpty()) {
            sql.append("AND (r.code IN (");
            sql.append(String.join(",", Collections.nCopies(allowedRatings.size(), "?")));
            sql.append(") OR r.code IS NULL) ");
        }

        sql.append("GROUP BY t.title_id ");
        sql.append("ORDER BY rating_count DESC, t.release_year DESC ");
        sql.append("LIMIT ?");

        List<Object> params = new ArrayList<>(allowedRatings);
        params.add(limit);

        return jdbcTemplate.query(sql.toString(), new TitleRowMapper(), params.toArray());
    }

    /**
     * Get genre-based recommendations for a profile.
     */
    public List<Title> getGenreBasedRecommendations(int profileId, List<Integer> genreIds, int limit) {
        UserProfile profile = userProfileDAO.findById(profileId);
        if (profile == null) {
            throw new RuntimeException("Profile not found: " + profileId);
        }

        User user = userDAO.findById(profile.getUserId());
        List<String> allowedRatings = getAllowedRatingsForAge(user.calculateAge(), profile.getMaturityRatingOverride());

        if (genreIds.isEmpty()) {
            return getPopularContentForAge(allowedRatings, limit);
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT t.*, r.code as rating_code, d.unit, d.value ");
        sql.append("FROM title t ");
        sql.append("LEFT JOIN rating r ON t.rating_id = r.rating_id ");
        sql.append("LEFT JOIN duration d ON t.duration_id = d.duration_id ");
        sql.append("JOIN title_genre tg ON t.title_id = tg.title_id ");
        sql.append("WHERE tg.genre_id IN (");
        sql.append(String.join(",", Collections.nCopies(genreIds.size(), "?")));
        sql.append(") ");

        if (!allowedRatings.isEmpty()) {
            sql.append("AND (r.code IN (");
            sql.append(String.join(",", Collections.nCopies(allowedRatings.size(), "?")));
            sql.append(") OR r.code IS NULL) ");
        }

        sql.append("ORDER BY t.release_year DESC ");
        sql.append("LIMIT ?");

        List<Object> params = new ArrayList<>();
        for (Integer genreId : genreIds) {
            params.add(genreId);
        }
        params.addAll(allowedRatings);
        params.add(limit);

        return jdbcTemplate.query(sql.toString(), new TitleRowMapper(), params.toArray());
    }

    /**
     * Row mapper for Title objects with rating and duration info.
     */
    private static class TitleRowMapper implements RowMapper<Title> {
        @Override
        public Title mapRow(ResultSet rs, int rowNum) throws SQLException {
            Title title = new Title();
            title.setTitleId(rs.getInt("title_id"));
            title.setShowId(rs.getString("show_id"));
            title.setTitle(rs.getString("title"));
            title.setType(rs.getString("type"));
            title.setDescription(rs.getString("description"));
            title.setDateAdded(rs.getDate("date_added"));
            title.setReleaseYear(rs.getInt("release_year"));
            title.setRatingId(rs.getInt("rating_id"));
            title.setDurationId(rs.getInt("duration_id"));

            Timestamp createdTimestamp = rs.getTimestamp("created_at");
            if (createdTimestamp != null) {
                title.setCreatedAt(createdTimestamp);
            }

            Timestamp updatedTimestamp = rs.getTimestamp("updated_at");
            if (updatedTimestamp != null) {
                title.setUpdatedAt(updatedTimestamp);
            }

            return title;
        }
    }
}