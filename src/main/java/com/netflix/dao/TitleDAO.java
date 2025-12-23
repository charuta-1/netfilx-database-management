package com.netflix.dao;

import com.netflix.model.*;
import com.netflix.util.DatabaseConnection;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Repository
public class TitleDAO {

    private ActorDAO actorDAO = new ActorDAO();
    private DirectorDAO directorDAO = new DirectorDAO();
    private CountryDAO countryDAO = new CountryDAO();
    private GenreDAO genreDAO = new GenreDAO();
    private RatingDAO ratingDAO = new RatingDAO();
    private DurationDAO durationDAO = new DurationDAO();

    private static final String AGGREGATED_SELECT_CORE = """
        SELECT
            t.title_id,
            t.show_id,
            t.title,
            t.type,
            t.description,
            t.date_added,
            t.release_year,
            t.rating_id,
            t.duration_id,
            t.created_at,
            t.updated_at,
            r.code AS rating_code,
            d.unit,
            d.value,
            GROUP_CONCAT(DISTINCT g.name ORDER BY g.name SEPARATOR '||') AS genre_list,
            GROUP_CONCAT(DISTINCT c.name ORDER BY c.name SEPARATOR '||') AS country_list,
            GROUP_CONCAT(DISTINCT dir.full_name ORDER BY dir.full_name SEPARATOR '||') AS director_list,
            GROUP_CONCAT(DISTINCT a.full_name ORDER BY a.full_name SEPARATOR '||') AS cast_list
        FROM title t
        LEFT JOIN rating r ON t.rating_id = r.rating_id
        LEFT JOIN duration d ON t.duration_id = d.duration_id
        LEFT JOIN title_genre tg ON t.title_id = tg.title_id
        LEFT JOIN genre g ON tg.genre_id = g.genre_id
        LEFT JOIN title_country tc ON t.title_id = tc.title_id
        LEFT JOIN country c ON tc.country_id = c.country_id
        LEFT JOIN title_director td ON t.title_id = td.title_id
        LEFT JOIN director dir ON td.director_id = dir.director_id
        LEFT JOIN title_cast tcast ON t.title_id = tcast.title_id
        LEFT JOIN actor a ON tcast.actor_id = a.actor_id
        """;

    private static final String AGGREGATED_GROUP_BY = """
        GROUP BY
            t.title_id,
            t.show_id,
            t.title,
            t.type,
            t.description,
            t.date_added,
            t.release_year,
            t.rating_id,
            t.duration_id,
            t.created_at,
            t.updated_at,
            r.code,
            d.unit,
            d.value
        """;

    private static final String ORDER_BY_TITLE = "ORDER BY t.title";

    public static class TitleFilter {
        private final String search;
        private final List<String> types;
        private final List<String> ratings;
        private final List<Integer> genreIds;
        private final List<String> countries;
        private final Integer yearMin;
        private final Integer yearMax;

        private TitleFilter(Builder builder) {
            this.search = builder.search;
            this.types = builder.types == null ? Collections.emptyList() : List.copyOf(builder.types);
            this.ratings = builder.ratings == null ? Collections.emptyList() : List.copyOf(builder.ratings);
            this.genreIds = builder.genreIds == null ? Collections.emptyList() : List.copyOf(builder.genreIds);
            this.countries = builder.countries == null ? Collections.emptyList() : List.copyOf(builder.countries);
            this.yearMin = builder.yearMin;
            this.yearMax = builder.yearMax;
        }

        public static Builder builder() { return new Builder(); }

        public static TitleFilter empty() { return builder().build(); }

        public String getSearch() { return search; }
        public List<String> getTypes() { return types; }
        public List<String> getRatings() { return ratings; }
        public List<Integer> getGenreIds() { return genreIds; }
        public List<String> getCountries() { return countries; }
        public Integer getYearMin() { return yearMin; }
        public Integer getYearMax() { return yearMax; }

        public static class Builder {
            private String search;
            private List<String> types;
            private List<String> ratings;
            private List<Integer> genreIds;
            private List<String> countries;
            private Integer yearMin;
            private Integer yearMax;

            public Builder search(String search) {
                this.search = search;
                return this;
            }

            public Builder types(List<String> types) {
                this.types = types;
                return this;
            }

            public Builder ratings(List<String> ratings) {
                this.ratings = ratings;
                return this;
            }

            public Builder genreIds(List<Integer> genreIds) {
                this.genreIds = genreIds;
                return this;
            }

            public Builder countries(List<String> countries) {
                this.countries = countries;
                return this;
            }

            public Builder yearMin(Integer yearMin) {
                this.yearMin = yearMin;
                return this;
            }

            public Builder yearMax(Integer yearMax) {
                this.yearMax = yearMax;
                return this;
            }

            public TitleFilter build() {
                return new TitleFilter(this);
            }
        }
    }

    public static class PagedResult<T> {
        private final List<T> items;
        private final int totalItems;
        private final int page;
        private final int pageSize;

        public PagedResult(List<T> items, int totalItems, int page, int pageSize) {
            this.items = items;
            this.totalItems = totalItems;
            this.page = page;
            this.pageSize = pageSize;
        }

        public List<T> getItems() { return items; }
        public int getTotalItems() { return totalItems; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }

        public int getTotalPages() {
            if (pageSize <= 0) {
                return 0;
            }
            return (int) Math.ceil((double) totalItems / (double) pageSize);
        }
    }

    public static class TitleTypeCounts {
        private final int total;
        private final int movieCount;
        private final int tvShowCount;

        public TitleTypeCounts(int total, int movieCount, int tvShowCount) {
            this.total = total;
            this.movieCount = movieCount;
            this.tvShowCount = tvShowCount;
        }

        public int getTotal() { return total; }
        public int getMovieCount() { return movieCount; }
        public int getTvShowCount() { return tvShowCount; }
    }

    // Create a new title with all relationships
    public boolean insertTitle(Title title, List<String> countries, List<String> genres,
                               List<String> directors, List<String> cast) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Insert main title record
            String titleSql = "INSERT INTO title (show_id, title, type, description, date_added, release_year, rating_id, duration_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement titleStmt = conn.prepareStatement(titleSql, Statement.RETURN_GENERATED_KEYS);

            titleStmt.setString(1, title.getShowId());
            titleStmt.setString(2, title.getTitle());
            titleStmt.setString(3, title.getType());
            titleStmt.setString(4, title.getDescription());
            // handle nullable date
            if (title.getDateAdded() != null) {
                titleStmt.setDate(5, title.getDateAdded());
            } else {
                titleStmt.setNull(5, Types.DATE);
            }
            titleStmt.setInt(6, title.getReleaseYear());
            // handle nullable rating
            if (title.getRatingId() > 0) {
                titleStmt.setInt(7, title.getRatingId());
            } else {
                titleStmt.setNull(7, Types.INTEGER);
            }
            // handle nullable duration
            if (title.getDurationId() > 0) {
                titleStmt.setInt(8, title.getDurationId());
            } else {
                titleStmt.setNull(8, Types.INTEGER);
            }

            int rowsAffected = titleStmt.executeUpdate();
            if (rowsAffected == 0) {
                conn.rollback();
                return false;
            }

            // Get generated title ID, then insert junction table rows and commit
            ResultSet generatedKeys = titleStmt.getGeneratedKeys();
            int titleId = 0;
            if (generatedKeys.next()) {
                titleId = generatedKeys.getInt(1);
            } else {
                conn.rollback();
                return false;
            }

            if (countries != null && !countries.isEmpty()) {
                for (String countryName : countries) {
                    int countryId = countryDAO.findOrCreateCountry(countryName);
                    if (countryId > 0) {
                        insertTitleCountry(conn, titleId, countryId);
                    }
                }
            }
            if (genres != null && !genres.isEmpty()) {
                for (String genreName : genres) {
                    int genreId = genreDAO.findOrCreateGenre(genreName);
                    if (genreId > 0) {
                        insertTitleGenre(conn, titleId, genreId);
                    }
                }
            }
            if (directors != null && !directors.isEmpty()) {
                for (String directorName : directors) {
                    int directorId = directorDAO.findOrCreateDirector(directorName);
                    if (directorId > 0) {
                        insertTitleDirector(conn, titleId, directorId);
                    }
                }
            }
            if (cast != null && !cast.isEmpty()) {
                for (String actorName : cast) {
                    int actorId = actorDAO.findOrCreateActor(actorName);
                    if (actorId > 0) {
                        insertTitleCast(conn, titleId, actorId);
                    }
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Error inserting title: " + e.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        }
    }

    // Get all titles with basic information + relationships for frontend
    public List<Title> getAllTitles() {
        List<Title> titles = new ArrayList<>();
        String aggregationSql = buildAggregatedQuery(null, null, ORDER_BY_TITLE, null, null);

        try (Connection conn = DatabaseConnection.getConnection();
             Statement setup = conn.createStatement()) {

            setup.execute("SET SESSION group_concat_max_len = 8192");

            try (PreparedStatement pstmt = conn.prepareStatement(aggregationSql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    titles.add(mapAggregatedTitle(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching titles: " + e.getMessage());
        }

        return titles;
    }

    // Get title by ID with all relationships
    public Title getTitleById(int titleId) {
        String sql = buildAggregatedQuery("WHERE t.title_id = ?", null, null, null, null);

        try (Connection conn = DatabaseConnection.getConnection();
             Statement setup = conn.createStatement()) {

            setup.execute("SET SESSION group_concat_max_len = 8192");

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, titleId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapAggregatedTitle(rs);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching title by ID: " + e.getMessage());
        }

        return null;
    }

    // Search titles by various criteria
    public List<Title> searchTitles(String titleSearch, String type, String genre,
                                   String country, Integer year) {
        List<Title> titles = new ArrayList<>();
        StringBuilder whereClause = new StringBuilder("WHERE 1=1");
        List<Object> parameters = new ArrayList<>();

        if (genre != null && !genre.trim().isEmpty()) {
            whereClause.append(" AND g.name = ?");
            parameters.add(genre);
        }

        if (country != null && !country.trim().isEmpty()) {
            whereClause.append(" AND c.name = ?");
            parameters.add(country);
        }

        if (titleSearch != null && !titleSearch.trim().isEmpty()) {
            whereClause.append(" AND LOWER(t.title) LIKE LOWER(?)");
            parameters.add("%" + titleSearch + "%");
        }

        if (type != null && !type.trim().isEmpty()) {
            whereClause.append(" AND t.type = ?");
            parameters.add(type);
        }

        if (year != null) {
            whereClause.append(" AND t.release_year = ?");
            parameters.add(year);
        }

        String sql = buildAggregatedQuery(whereClause.toString(), null, ORDER_BY_TITLE, null, null);

        try (Connection conn = DatabaseConnection.getConnection();
             Statement setup = conn.createStatement()) {

            setup.execute("SET SESSION group_concat_max_len = 8192");

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < parameters.size(); i++) {
                    pstmt.setObject(i + 1, parameters.get(i));
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        titles.add(mapAggregatedTitle(rs));
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error searching titles: " + e.getMessage());
        }

        return titles;
    }

    // Delete title and all its relationships
    public boolean deleteTitle(int titleId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Delete junction table records first
            deleteTitleCountries(conn, titleId);
            deleteTitleGenres(conn, titleId);
            deleteTitleDirectors(conn, titleId);
            deleteTitleCast(conn, titleId);

            // Delete main title record
            String sql = "DELETE FROM title WHERE title_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, titleId);

            int rowsAffected = pstmt.executeUpdate();

            conn.commit();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting title: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Error rolling back transaction: " + rollbackEx.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
            }
        }
    }

    // Update core fields of a Title
    public boolean updateTitle(Title title) {
        String sql = "UPDATE title SET show_id = ?, title = ?, type = ?, description = ?, date_added = ?, release_year = ?, rating_id = ?, duration_id = ?, updated_at = CURRENT_TIMESTAMP WHERE title_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title.getShowId());
            pstmt.setString(2, title.getTitle());
            pstmt.setString(3, title.getType());
            pstmt.setString(4, title.getDescription());
            if (title.getDateAdded() != null) {
                pstmt.setDate(5, title.getDateAdded());
            } else {
                pstmt.setNull(5, Types.DATE);
            }
            pstmt.setInt(6, title.getReleaseYear());
            if (title.getRatingId() > 0) {
                pstmt.setInt(7, title.getRatingId());
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }
            if (title.getDurationId() > 0) {
                pstmt.setInt(8, title.getDurationId());
            } else {
                pstmt.setNull(8, Types.INTEGER);
            }
            pstmt.setInt(9, title.getTitleId());

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating title: " + e.getMessage());
            return false;
        }
    }

    // Helper methods for junction tables
    private void insertTitleCountry(Connection conn, int titleId, int countryId) throws SQLException {
        String sql = "INSERT IGNORE INTO title_country (title_id, country_id) VALUES (?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, titleId);
        pstmt.setInt(2, countryId);
        pstmt.executeUpdate();
    }

    private void insertTitleGenre(Connection conn, int titleId, int genreId) throws SQLException {
        String sql = "INSERT IGNORE INTO title_genre (title_id, genre_id) VALUES (?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, titleId);
        pstmt.setInt(2, genreId);
        pstmt.executeUpdate();
    }

    private void insertTitleDirector(Connection conn, int titleId, int directorId) throws SQLException {
        String sql = "INSERT IGNORE INTO title_director (title_id, director_id) VALUES (?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, titleId);
        pstmt.setInt(2, directorId);
        pstmt.executeUpdate();
    }

    private void insertTitleCast(Connection conn, int titleId, int actorId) throws SQLException {
        String sql = "INSERT IGNORE INTO title_cast (title_id, actor_id) VALUES (?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, titleId);
        pstmt.setInt(2, actorId);
        pstmt.executeUpdate();
    }

    // Get related data methods
    private Title mapAggregatedTitle(ResultSet rs) throws SQLException {
        Title title = createTitleFromResultSet(rs);

        String ratingCode = rs.getString("rating_code");
        if (ratingCode != null) {
            Rating rating = new Rating();
            rating.setRatingId(rs.getInt("rating_id"));
            rating.setCode(ratingCode);
            title.setRating(rating);
        }

        String unit = rs.getString("unit");
        int value = rs.getInt("value");
        if (unit != null && value > 0) {
            Duration duration = new Duration();
            duration.setDurationId(rs.getInt("duration_id"));
            duration.setUnit(unit);
            duration.setValue(value);
            title.setDuration(duration);
        }

        title.setGenres(mapGenres(rs.getString("genre_list")));
        title.setCountries(mapCountries(rs.getString("country_list")));
        title.setDirectors(mapDirectors(rs.getString("director_list")));
        title.setCast(mapCast(rs.getString("cast_list")));

        return title;
    }

    private String buildAggregatedQuery(String whereClause, String havingClause, String orderClause, Integer limit, Integer offset) {
        StringBuilder sb = new StringBuilder(AGGREGATED_SELECT_CORE);
        if (whereClause != null && !whereClause.isBlank()) {
            sb.append(" ").append(whereClause);
        }
        sb.append(" ").append(AGGREGATED_GROUP_BY);
        if (havingClause != null && !havingClause.isBlank()) {
            sb.append(" HAVING ").append(havingClause);
        }
        if (orderClause != null && !orderClause.isBlank()) {
            sb.append(" ").append(orderClause);
        }
        if (limit != null && limit > 0) {
            sb.append(" LIMIT ").append(limit);
            if (offset != null && offset >= 0) {
                sb.append(" OFFSET ").append(offset);
            }
        }
        return sb.toString();
    }

    private List<Genre> mapGenres(String raw) {
        List<String> parts = splitAggregatedList(raw);
        if (parts.isEmpty()) {
            return new ArrayList<>();
        }
        List<Genre> genres = new ArrayList<>();
        for (String name : parts) {
            Genre genre = new Genre();
            genre.setName(name);
            genres.add(genre);
        }
        return genres;
    }

    private List<Country> mapCountries(String raw) {
        List<String> parts = splitAggregatedList(raw);
        if (parts.isEmpty()) {
            return new ArrayList<>();
        }
        List<Country> countries = new ArrayList<>();
        for (String name : parts) {
            Country country = new Country();
            country.setName(name);
            countries.add(country);
        }
        return countries;
    }

    private List<Director> mapDirectors(String raw) {
        List<String> parts = splitAggregatedList(raw);
        if (parts.isEmpty()) {
            return new ArrayList<>();
        }
        List<Director> directors = new ArrayList<>();
        for (String name : parts) {
            Director director = new Director();
            director.setFullName(name);
            directors.add(director);
        }
        return directors;
    }

    private List<Actor> mapCast(String raw) {
        List<String> parts = splitAggregatedList(raw);
        if (parts.isEmpty()) {
            return new ArrayList<>();
        }
        List<Actor> cast = new ArrayList<>();
        for (String name : parts) {
            Actor actor = new Actor();
            actor.setFullName(name);
            cast.add(actor);
        }
        return cast;
    }

    private List<String> splitAggregatedList(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }

        String[] tokens = raw.split("\\|\\|");
        List<String> values = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            String trimmed = token == null ? null : token.trim();
            if (trimmed != null && !trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    // Delete junction table records
    private void deleteTitleCountries(Connection conn, int titleId) throws SQLException {
        String sql = "DELETE FROM title_country WHERE title_id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, titleId);
        pstmt.executeUpdate();
    }

    private void deleteTitleGenres(Connection conn, int titleId) throws SQLException {
        String sql = "DELETE FROM title_genre WHERE title_id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, titleId);
        pstmt.executeUpdate();
    }

    private void deleteTitleDirectors(Connection conn, int titleId) throws SQLException {
        String sql = "DELETE FROM title_director WHERE title_id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, titleId);
        pstmt.executeUpdate();
    }

    private void deleteTitleCast(Connection conn, int titleId) throws SQLException {
        String sql = "DELETE FROM title_cast WHERE title_id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, titleId);
        pstmt.executeUpdate();
    }

    // Helper method to create Title object from ResultSet
    private Title createTitleFromResultSet(ResultSet rs) throws SQLException {
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
        title.setCreatedAt(rs.getTimestamp("created_at"));
        title.setUpdatedAt(rs.getTimestamp("updated_at"));

        return title;
    }

    // ===================== New methods =====================
    public List<Title> getTitlesByActorId(int actorId) {
        List<Title> titles = new ArrayList<>();
        String sql = buildAggregatedQuery("WHERE tcast.actor_id = ?", null, ORDER_BY_TITLE, null, null);
        try (Connection conn = DatabaseConnection.getConnection();
             Statement setup = conn.createStatement()) {

            setup.execute("SET SESSION group_concat_max_len = 8192");

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, actorId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        titles.add(mapAggregatedTitle(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching titles by actor: " + e.getMessage());
        }
        return titles;
    }

    public List<Title> getTitlesByDirectorId(int directorId) {
        List<Title> titles = new ArrayList<>();
        String sql = buildAggregatedQuery("WHERE td.director_id = ?", null, ORDER_BY_TITLE, null, null);
        try (Connection conn = DatabaseConnection.getConnection();
             Statement setup = conn.createStatement()) {

            setup.execute("SET SESSION group_concat_max_len = 8192");

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, directorId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        titles.add(mapAggregatedTitle(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching titles by director: " + e.getMessage());
        }
        return titles;
    }

    public TitleTypeCounts getTitleTypeCounts() {
        String sql = """
            SELECT
                COUNT(*) AS total_count,
                SUM(CASE WHEN t.type = 'Movie' THEN 1 ELSE 0 END) AS movie_count,
                SUM(CASE WHEN t.type = 'TV Show' THEN 1 ELSE 0 END) AS tv_count
            FROM title t
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                int total = rs.getInt("total_count");
                int movie = rs.getInt("movie_count");
                int tv = rs.getInt("tv_count");
                return new TitleTypeCounts(total, movie, tv);
            }

        } catch (SQLException e) {
            System.err.println("Error counting titles: " + e.getMessage());
        }

        return new TitleTypeCounts(0, 0, 0);
    }

    public PagedResult<Title> getTitlesPaged(TitleFilter filter, int page, int pageSize) {
        if (pageSize <= 0) {
            pageSize = 50;
        }
        if (page <= 0) {
            page = 1;
        }

        int offset = (page - 1) * pageSize;
        TitleFilter effectiveFilter = filter != null ? filter : TitleFilter.empty();

        StringBuilder where = new StringBuilder("WHERE 1=1");
        List<Object> whereParams = new ArrayList<>();

        if (!effectiveFilter.getTypes().isEmpty()) {
            where.append(" AND t.type IN (").append(buildInClausePlaceholders(effectiveFilter.getTypes().size())).append(")");
            whereParams.addAll(effectiveFilter.getTypes());
        }

        if (!effectiveFilter.getRatings().isEmpty()) {
            where.append(" AND r.code IN (").append(buildInClausePlaceholders(effectiveFilter.getRatings().size())).append(")");
            whereParams.addAll(effectiveFilter.getRatings());
        }

        if (!effectiveFilter.getGenreIds().isEmpty()) {
            where.append(" AND tg.genre_id IN (").append(buildInClausePlaceholders(effectiveFilter.getGenreIds().size())).append(")");
            whereParams.addAll(effectiveFilter.getGenreIds());
        }

        if (!effectiveFilter.getCountries().isEmpty()) {
            where.append(" AND c.name IN (").append(buildInClausePlaceholders(effectiveFilter.getCountries().size())).append(")");
            whereParams.addAll(effectiveFilter.getCountries());
        }

        if (effectiveFilter.getYearMin() != null) {
            where.append(" AND t.release_year >= ?");
            whereParams.add(effectiveFilter.getYearMin());
        }

        if (effectiveFilter.getYearMax() != null) {
            where.append(" AND t.release_year <= ?");
            whereParams.add(effectiveFilter.getYearMax());
        }

        StringBuilder having = new StringBuilder();
        List<Object> havingParams = new ArrayList<>();
        if (effectiveFilter.getSearch() != null && !effectiveFilter.getSearch().isBlank()) {
            String like = "%" + effectiveFilter.getSearch().toLowerCase(Locale.ROOT) + "%";
            having.append("(LOWER(t.title) LIKE ? OR LOWER(IFNULL(t.description, '')) LIKE ? OR LOWER(IFNULL(genre_list, '')) LIKE ? OR LOWER(IFNULL(director_list, '')) LIKE ? OR LOWER(IFNULL(cast_list, '')) LIKE ?)");
            for (int i = 0; i < 5; i++) {
                havingParams.add(like);
            }
        }

        String whereClause = where.toString();
        String havingClause = having.length() > 0 ? having.toString() : null;

        String dataSql = buildAggregatedQuery(whereClause, havingClause, ORDER_BY_TITLE, pageSize, offset);
        String countSql = "SELECT COUNT(*) FROM (" + buildAggregatedQuery(whereClause, havingClause, null, null, null) + ") aggregated_titles";

        List<Title> titles = new ArrayList<>();
        int totalItems = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement setup = conn.createStatement()) {

            setup.execute("SET SESSION group_concat_max_len = 8192");

            try (PreparedStatement dataStmt = conn.prepareStatement(dataSql)) {
                int index = 1;
                for (Object param : whereParams) {
                    dataStmt.setObject(index++, param);
                }
                for (Object param : havingParams) {
                    dataStmt.setObject(index++, param);
                }

                try (ResultSet rs = dataStmt.executeQuery()) {
                    while (rs.next()) {
                        titles.add(mapAggregatedTitle(rs));
                    }
                }
            }

            try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                int index = 1;
                for (Object param : whereParams) {
                    countStmt.setObject(index++, param);
                }
                for (Object param : havingParams) {
                    countStmt.setObject(index++, param);
                }

                try (ResultSet rs = countStmt.executeQuery()) {
                    if (rs.next()) {
                        totalItems = rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching paged titles: " + e.getMessage());
        }

        return new PagedResult<>(titles, totalItems, page, pageSize);
    }

    private String buildInClausePlaceholders(int size) {
        if (size <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("?");
        }
        return sb.toString();
    }
}
