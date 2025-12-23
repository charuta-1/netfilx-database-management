-- Create the database with appropriate character set and collation
CREATE DATABASE IF NOT EXISTS netflix_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE netflix_db;

-- =================================================================
-- DIMENSION TABLES
-- =================================================================

-- RATING table with added description for context
CREATE TABLE rating (
                        rating_id INT AUTO_INCREMENT PRIMARY KEY,
                        code VARCHAR(10) NOT NULL UNIQUE,
                        description VARCHAR(255) NULL -- Added for clarity, e.g., 'Parents Strongly Cautioned'
) ENGINE=InnoDB;

-- DURATION table to store movie lengths or TV show seasons
CREATE TABLE duration (
                          duration_id INT AUTO_INCREMENT PRIMARY KEY,
                          unit ENUM('min','season') NOT NULL,
                          value INT NOT NULL,
                          UNIQUE KEY uq_duration (unit, value)
) ENGINE=InnoDB;

-- COUNTRY table
CREATE TABLE country (
                         country_id INT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- GENRE table
CREATE TABLE genre (
                       genre_id INT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- DIRECTOR table with an index for faster searching
CREATE TABLE director (
                          director_id INT AUTO_INCREMENT PRIMARY KEY,
                          full_name VARCHAR(150) NOT NULL UNIQUE,
                          INDEX idx_director_name (full_name) -- CHANGE: Added index for search performance
) ENGINE=InnoDB;

-- ACTOR table with an index for faster searching
CREATE TABLE actor (
                       actor_id INT AUTO_INCREMENT PRIMARY KEY,
                       full_name VARCHAR(150) NOT NULL UNIQUE,
                       INDEX idx_actor_name (full_name) -- CHANGE: Added index for search performance
) ENGINE=InnoDB;


-- =================================================================
-- FACT TABLE (TITLE)
-- =================================================================

CREATE TABLE title (
                       title_id INT AUTO_INCREMENT PRIMARY KEY,
                       show_id VARCHAR(10) NOT NULL UNIQUE,
                       title VARCHAR(255) NOT NULL,
                       type ENUM('Movie','TV Show') NOT NULL,
                       description TEXT NULL,
                       date_added DATE NULL,
                       release_year INT NOT NULL,
                       rating_id INT NULL,
                       duration_id INT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       CONSTRAINT fk_title_rating FOREIGN KEY (rating_id) REFERENCES rating(rating_id),
                       CONSTRAINT fk_title_duration FOREIGN KEY (duration_id) REFERENCES duration(duration_id),
                       INDEX idx_title_name (title), -- CHANGE: Added index for title searches
                       FULLTEXT INDEX idx_description_fulltext (description) -- CHANGE: Added FULLTEXT index for description searches
) ENGINE=InnoDB;

-- To store the main account login information
CREATE TABLE users (
                       user_id INT AUTO_INCREMENT PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       date_of_birth DATE NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- To store individual viewing profiles under one account
CREATE TABLE user_profiles (
                               profile_id INT AUTO_INCREMENT PRIMARY KEY,
                               user_id INT NOT NULL,
                               profile_name VARCHAR(50) NOT NULL,
                               maturity_rating_override VARCHAR(10) NULL,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                               UNIQUE KEY uq_user_profile_name (user_id, profile_name)
) ENGINE=InnoDB;

CREATE TABLE watch_history (
                               watch_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               profile_id INT NOT NULL,
                               title_id INT NOT NULL,
                               watched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Simple flag to see if they finished, which is a strong positive signal
                               is_completed BOOLEAN DEFAULT FALSE,
                               FOREIGN KEY (profile_id) REFERENCES user_profiles(profile_id) ON DELETE CASCADE,
                               FOREIGN KEY (title_id) REFERENCES title(title_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE user_ratings (
                              rating_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              profile_id INT NOT NULL,
                              title_id INT NOT NULL,
    -- 'thumbs_up' or 'thumbs_down' is simple and effective
                              rating_value ENUM('thumbs_up', 'thumbs_down') NOT NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (profile_id) REFERENCES user_profiles(profile_id) ON DELETE CASCADE,
                              FOREIGN KEY (title_id) REFERENCES title(title_id) ON DELETE CASCADE,
    -- A user can only rate a title once per profile
                              UNIQUE KEY uq_profile_title_rating (profile_id, title_id)
) ENGINE=InnoDB;

CREATE TABLE watchlist (
                           watchlist_id INT AUTO_INCREMENT PRIMARY KEY,
                           profile_id INT NOT NULL,
                           title_id INT NOT NULL,
                           added_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           FOREIGN KEY (profile_id) REFERENCES user_profiles(profile_id) ON DELETE CASCADE,
                           FOREIGN KEY (title_id) REFERENCES title(title_id) ON DELETE CASCADE,
    -- A title can only be in a profile's watchlist once
                           UNIQUE KEY uq_profile_title (profile_id, title_id)
) ENGINE=InnoDB;

CREATE TABLE user_genre_preferences (
                                        preference_id INT AUTO_INCREMENT PRIMARY KEY,
                                        profile_id INT NOT NULL,
                                        genre_id INT NOT NULL,
                                        FOREIGN KEY (profile_id) REFERENCES user_profiles(profile_id) ON DELETE CASCADE,
                                        FOREIGN KEY (genre_id) REFERENCES genre(genre_id) ON DELETE CASCADE,
                                        UNIQUE KEY uq_profile_genre (profile_id, genre_id)
);

-- =================================================================
-- JUNCTION TABLES (MANY-TO-MANY RELATIONSHIPS)
-- =================================================================

CREATE TABLE title_country (
                               title_id INT NOT NULL,
                               country_id INT NOT NULL,
                               PRIMARY KEY (title_id, country_id),
                               CONSTRAINT fk_tc_title FOREIGN KEY (title_id) REFERENCES title(title_id) ON DELETE CASCADE,
                               CONSTRAINT fk_tc_country FOREIGN KEY (country_id) REFERENCES country(country_id)
) ENGINE=InnoDB;

CREATE TABLE title_genre (
                             title_id INT NOT NULL,
                             genre_id INT NOT NULL,
                             PRIMARY KEY (title_id, genre_id),
                             CONSTRAINT fk_tg_title FOREIGN KEY (title_id) REFERENCES title(title_id) ON DELETE CASCADE,
                             CONSTRAINT fk_tg_genre FOREIGN KEY (genre_id) REFERENCES genre(genre_id)
) ENGINE=InnoDB;

CREATE TABLE title_director (
                                title_id INT NOT NULL,
                                director_id INT NOT NULL,
                                PRIMARY KEY (title_id, director_id),
                                CONSTRAINT fk_td_title FOREIGN KEY (title_id) REFERENCES title(title_id) ON DELETE CASCADE,
                                CONSTRAINT fk_td_director FOREIGN KEY (director_id) REFERENCES director(director_id)
) ENGINE=InnoDB;

CREATE TABLE title_cast (
                            title_id INT NOT NULL,
                            actor_id INT NOT NULL,
                            PRIMARY KEY (title_id, actor_id),
                            CONSTRAINT fk_ta_title FOREIGN KEY (title_id) REFERENCES title(title_id) ON DELETE CASCADE,
                            CONSTRAINT fk_ta_actor FOREIGN KEY (actor_id) REFERENCES actor(actor_id)
) ENGINE=InnoDB;


-- =================================================================
-- AUDITING
-- =================================================================

CREATE TABLE audit_title_update (
                                    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    title_id INT NOT NULL,
                                    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    old_rating_id INT NULL,
                                    new_rating_id INT NULL,
                                    old_duration_id INT NULL,
                                    new_duration_id INT NULL,
                                    CONSTRAINT fk_audit_title FOREIGN KEY (title_id) REFERENCES title(title_id) ON DELETE CASCADE
) ENGINE=InnoDB;

DELIMITER $$
CREATE TRIGGER trg_title_update_audit
    AFTER UPDATE ON title
    FOR EACH ROW
BEGIN
    -- CHANGE: Corrected logic to only fire when values have actually changed.
    IF NOT (OLD.rating_id <=> NEW.rating_id AND OLD.duration_id <=> NEW.duration_id) THEN
        INSERT INTO audit_title_update (title_id, old_rating_id, new_rating_id, old_duration_id, new_duration_id)
        VALUES (OLD.title_id, OLD.rating_id, NEW.rating_id, OLD.duration_id, NEW.duration_id);
END IF;
END$$
DELIMITER ;


-- =================================================================
-- VIEWS FOR SIMPLIFIED QUERIES
-- =================================================================

CREATE OR REPLACE VIEW v_title_with_details AS
SELECT
    t.title_id,
    t.show_id,
    t.title,
    t.type,
    t.release_year,
    r.code AS rating,
    d.unit,
    d.value,
    t.date_added
FROM title t
         LEFT JOIN rating r ON t.rating_id = r.rating_id
         LEFT JOIN duration d ON t.duration_id = d.duration_id;

CREATE OR REPLACE VIEW v_top_countries AS
SELECT
    c.name AS country,
    COUNT(tc.title_id) AS num_titles
FROM country c
         JOIN title_country tc ON c.country_id = tc.country_id
GROUP BY c.country_id, c.name
ORDER BY num_titles DESC;

CREATE OR REPLACE VIEW v_top_genres AS
SELECT
    g.name AS genre,
    COUNT(tg.title_id) AS num_titles
FROM genre g
         JOIN title_genre tg ON g.genre_id = tg.genre_id
GROUP BY g.genre_id, g.name
ORDER BY num_titles DESC;

select * from title;