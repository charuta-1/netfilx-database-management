- Corrected script to load all data
SET NAMES 'utf8mb4';
-- Load dimension tables
LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/actors.csv'
INTO TABLE actor CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(actor_id, full_name);

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/directors.csv'
INTO TABLE director CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(director_id, full_name);

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/ratings.csv'
INTO TABLE rating CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(rating_id, code, description);

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/durations.csv'
INTO TABLE duration CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(duration_id, unit, value);

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/countries.csv'
IGNORE
INTO TABLE country CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(country_id, name);

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/genres.csv'
INTO TABLE genre CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(genre_id, name);

-- Load titles (fact table)
-- This final version handles blank dates, blank numbers, and the date format.

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/titles.csv'
INTO TABLE title CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
-- Load all columns that need processing into temporary variables
(title_id, show_id, title, type, description, @date_added, release_year, @rating_id, @duration_id)
SET
    -- First, check if the date string is blank. If not, convert it.
    date_added = STR_TO_DATE(NULLIF(@date_added, ''), '%M %e, %Y'),

    -- If rating_id from the file is blank, insert NULL.
    rating_id = NULLIF(@rating_id, ''),

    -- If duration_id from the file is blank, insert NULL.
    duration_id = NULLIF(@duration_id, '');    
    
-- Load link tables
LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/title_countries.csv'
INTO TABLE title_country CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(title_id, country_id);

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/title_genres.csv'
INTO TABLE title_genre CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(title_id, genre_id);

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/title_directors_FIXED.csv'
INTO TABLE title_director CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(title_id, director_id);

LOAD DATA INFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/title_cast_FIXED.csv'
INTO TABLE title_cast CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(title_id, actor_id);

INSERT INTO SubscriptionPlans (plan_name, price, quality, screens_allowed) VALUES
                                                                               ('Basic', 9.99, 'SD', 1),
                                                                               ('Standard', 15.49, 'HD', 2),
                                                                               ('Premium', 19.99, 'UHD', 4);

-- 2. Populate Users (no dependencies)
-- NOTE: In a real application, the password_hash would be a secure hash (e.g., from bcrypt).
INSERT INTO users (email, username, password_hash, date_of_birth) VALUES
                                                                      ('alice@example.com', 'alice', 'hashed_password_for_alice', '1990-05-15'),
                                                                      ('bob@example.com', 'bob', 'hashed_password_for_bob', '1988-11-22');

-- 3. Populate UserSubscriptions (depends on users and SubscriptionPlans)
-- Let's say Alice has an active Premium plan and Bob had a Standard plan that is now canceled.
INSERT INTO UserSubscriptions (user_id, plan_id, status) VALUES
                                                             (1, 3, 'active'), -- Alice gets the Premium plan (plan_id=3)
                                                             (2, 2, 'canceled'); -- Bob had the Standard plan (plan_id=2)

-- 4. Populate UserProfiles (depends on users)
-- Each user can have multiple profiles.
INSERT INTO user_profiles (user_id, profile_name) VALUES
                                                      (1, 'Alice'),   -- Alice's main profile
                                                      (1, 'Kids'),     -- Alice's kids profile
                                                      (2, 'Bob');      -- Bob's only profile

-- 5. Populate user interaction tables (depends on user_profiles)
-- We will assume some title_id's (e.g., 8805 for Zombieland) and genre_id's (e.g., 8 for Comedies)
-- exist from your previous data loading.

-- Watch History (profile_id's are 1, 2, 3 for 'Alice', 'Kids', 'Bob' respectively)
INSERT INTO watch_history (profile_id, title_id, is_completed) VALUES
                                                                   (1, 8805, TRUE),   -- Alice finished watching Zombieland (title_id=8805)
                                                                   (1, 8803, FALSE),  -- Alice started watching Zodiac (title_id=8803)
                                                                   (3, 8805, TRUE);   -- Bob also finished watching Zombieland

-- User Ratings
INSERT INTO user_ratings (profile_id, title_id, rating_value) VALUES
                                                                  (1, 8805, 'thumbs_up'), -- Alice liked Zombieland
                                                                  (3, 8805, 'thumbs_down'); -- Bob disliked Zombieland

-- Watchlist
INSERT INTO watchlist (profile_id, title_id) VALUES
                                                 (1, 8803), -- Alice added Zodiac to her watchlist
                                                 (2, 8806); -- The 'Kids' profile has Zoom (title_id=8806) on the watchlist

-- User Genre Preferences
INSERT INTO user_genre_preferences (profile_id, genre_id) VALUES
                                                              (1, 8),  -- Alice prefers Comedies (genre_id=8)
                                                              (1, 15), -- Alice also likes Horror Movies (genre_id=15)
                                                              (3, 8);  -- Bob also likes Comedies


