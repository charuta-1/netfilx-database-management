# Database Connection & CRUD Operations Test Guide

## 🔐 ADMIN CREDENTIALS
**Username:** `admin`  
**Password:** `admin123`

These credentials are stored in `application.properties`:
- netflix.admin.username=admin
- netflix.admin.password=admin123

## 🔌 DATABASE CONNECTION DETAILS
**Database:** `netflix_db`  
**Host:** `localhost:3306`  
**Username:** `root`  
**Password:** `asdfghjkl`  
**JDBC URL:** `jdbc:mysql://localhost:3306/netflix_db?useSSL=false&serverTimezone=UTC`

## ✅ SPRING JDBC INTEGRATION STATUS
Your project uses **Spring JDBC** with JdbcTemplate, NOT plain JDBC:
- ✅ All DAOs use `JdbcTemplate` (Spring's database access tool)
- ✅ DataSource is auto-configured by Spring Boot
- ✅ Connection pooling is handled automatically
- ✅ All CRUD operations are properly implemented

## 📊 ALL DAO CLASSES WITH CRUD OPERATIONS

### 1. UserDAO ✅
**Operations:**
- ✅ **CREATE:** `save(User user)` - Insert new user
- ✅ **READ:** `findByEmail()`, `findByUsername()`, `findById()`, `findAll()`
- ✅ **UPDATE:** `update(User user)` - Update user info
- ✅ **DELETE:** `deleteById(int userId)` - Delete user

### 2. UserProfileDAO ✅
**Operations:**
- ✅ **CREATE:** `save(UserProfile profile)` - Insert new profile
- ✅ **READ:** `findByUserId()`, `findById()`, `findAll()`, `countByUserId()`
- ✅ **UPDATE:** `update(UserProfile profile)` - Update profile
- ✅ **DELETE:** `deleteById(int profileId)` - Delete profile

### 3. TitleDAO ✅
**Operations:**
- ✅ **CREATE:** `insertTitle()` - Insert new title with relationships
- ✅ **READ:** `getAllTitles()`, `getTitleById()`, `searchTitles()`
- ✅ **UPDATE:** Title update operations
- ✅ **DELETE:** Title delete operations

### 4. WatchHistoryDAO ✅
**Operations:**
- ✅ **CREATE:** `add(WatchHistory history)` - Add watch history
- ✅ **READ:** `findByProfileId()`, `findByProfileIdWithLimit()`, `findCompletedByProfileId()`, `findLatestByProfileAndTitle()`
- ✅ **UPDATE:** `updateCompletionStatus()` - Update completion status
- ✅ **DELETE:** `deleteOldEntries()` - Delete old watch history

### 5. UserRatingDAO ✅
**Operations:**
- ✅ **CREATE:** Insert user ratings
- ✅ **READ:** Fetch ratings by user/profile
- ✅ **UPDATE:** Update ratings
- ✅ **DELETE:** Delete ratings

### 6. WatchlistDAO ✅
**Operations:**
- ✅ **CREATE:** Add to watchlist
- ✅ **READ:** Get watchlist items
- ✅ **UPDATE:** Update watchlist
- ✅ **DELETE:** Remove from watchlist

### 7. UserGenrePreferenceDAO ✅
**Operations:**
- ✅ **CREATE:** Save genre preferences
- ✅ **READ:** Get user preferences
- ✅ **UPDATE:** Update preferences
- ✅ **DELETE:** Delete preferences

## 🧪 HOW TO TEST DATABASE CONNECTION

### Method 1: Run the Application
```cmd
cd "C:\Users\piyus\Documents\SQL project"
build-and-run.bat
```

### Method 2: Check Logs
When the application starts, look for these log messages:
- ✅ "HikariPool-1 - Starting..." - Connection pool initialized
- ✅ "HikariPool-1 - Start completed" - Database connected
- ❌ "Unable to connect to database" - Connection failed

### Method 3: Test API Endpoints
After starting the server, test these endpoints:

**1. Test Admin Login:**
```bash
curl -X POST http://localhost:8082/api/admin/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

**2. Test User Registration:**
```bash
curl -X POST http://localhost:8082/api/users/register -H "Content-Type: application/json" -d "{\"email\":\"test@test.com\",\"username\":\"testuser\",\"password\":\"test123\",\"dateOfBirth\":\"1990-01-01\"}"
```

**3. Test Fetching All Titles:**
```bash
curl http://localhost:8082/api/titles
```

## 📝 DATABASE TABLES IN YOUR SCHEMA

Based on your schema, you have these tables with data:
1. ✅ **users** - User accounts
2. ✅ **user_profiles** - User profiles (multiple per user)
3. ✅ **title** - Movies and TV shows (Netflix content)
4. ✅ **watch_history** - User viewing history
5. ✅ **user_ratings** - User ratings for titles
6. ✅ **watchlist** - User's saved watchlist
7. ✅ **user_genre_preferences** - User genre preferences for recommendations
8. ✅ **rating** - Content ratings (PG, R, etc.)
9. ✅ **duration** - Duration information
10. ✅ **country** - Countries
11. ✅ **genre** - Genres
12. ✅ **director** - Directors
13. ✅ **actor** - Actors
14. ✅ **SubscriptionPlans** - Subscription plans
15. ✅ **UserSubscriptions** - User subscriptions

## 🔍 VERIFY DATA IN DATABASE

Run these SQL queries in MySQL Workbench:

```sql
-- Check if you have users
SELECT COUNT(*) as user_count FROM users;
SELECT * FROM users LIMIT 5;

-- Check if you have Netflix titles
SELECT COUNT(*) as title_count FROM title;
SELECT * FROM title LIMIT 5;

-- Check if you have user profiles
SELECT COUNT(*) as profile_count FROM user_profiles;
SELECT * FROM user_profiles LIMIT 5;

-- Check watch history
SELECT COUNT(*) as history_count FROM watch_history;

-- Check ratings
SELECT COUNT(*) as rating_count FROM user_ratings;
```

## 🐛 TROUBLESHOOTING

### Issue: Cannot connect to database
**Solution:** 
1. Check if MySQL is running
2. Verify credentials in `application.properties`
3. Check if database `netflix_db` exists

### Issue: User login doesn't work
**Possible causes:**
1. No users in database - Register a new user first
2. Wrong password - Password is hashed with BCrypt
3. Database not connected

### Issue: Admin dashboard shows no data
**Check:**
1. Database connection is active
2. Tables have data (run SQL queries above)
3. Backend API is returning data (check browser console for errors)

## 📡 API ENDPOINTS AVAILABLE

### User Authentication
- POST `/api/users/register` - Register new user
- POST `/api/users/login` - User login
- GET `/api/users/{userId}` - Get user by ID

### Admin Authentication
- POST `/api/admin/login` - Admin login (use admin/admin123)

### Titles (Netflix Content)
- GET `/api/titles` - Get all titles
- GET `/api/titles/{id}` - Get title by ID
- POST `/api/titles` - Add new title (admin)
- PUT `/api/titles/{id}` - Update title (admin)
- DELETE `/api/titles/{id}` - Delete title (admin)

### User Profiles
- POST `/api/profiles` - Create profile
- GET `/api/profiles/user/{userId}` - Get user profiles
- PUT `/api/profiles/{profileId}` - Update profile
- DELETE `/api/profiles/{profileId}` - Delete profile

### Watch History
- POST `/api/watch-history` - Add watch history
- GET `/api/watch-history/profile/{profileId}` - Get watch history

### Ratings
- POST `/api/ratings` - Rate a title
- GET `/api/ratings/profile/{profileId}` - Get user ratings

### Recommendations
- GET `/api/recommendations/profile/{profileId}` - Get recommendations

## ✨ NEXT STEPS

1. Start the application with `build-and-run.bat`
2. Open browser to `http://localhost:8082`
3. Login with admin credentials: **admin / admin123**
4. Check if data loads from database
5. Try adding/editing/deleting records to test CRUD operations

