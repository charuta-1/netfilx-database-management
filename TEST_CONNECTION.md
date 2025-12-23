# Netflix Management System - Connection Test Guide

## Database Schema ✓
- ✅ Users table with authentication
- ✅ User profiles with maturity ratings
- ✅ Titles (movies/TV shows) with metadata
- ✅ Watch history tracking
- ✅ User ratings (thumbs up/down)
- ✅ Watchlist management
- ✅ Genre preferences for recommendations
- ✅ Junction tables for many-to-many relationships

## Backend API Endpoints ✓

### Authentication & Users
- POST `/api/users/register` - Register new user
- POST `/api/users/login` - User login
- GET `/api/users/{userId}` - Get user details

### Profiles
- POST `/api/profiles` - Create profile
- GET `/api/profiles/user/{userId}` - Get user profiles
- GET `/api/profiles/{profileId}` - Get profile by ID
- PUT `/api/profiles/{profileId}` - Update profile

### Content Management (Titles)
- GET `/api/titles` - Get all titles
- POST `/api/titles` - Add new title
- PUT `/api/titles/{id}` - Update title
- DELETE `/api/titles/{id}` - Delete title
- GET `/api/genres` - Get all genres
- GET `/api/actors` - Get all actors
- GET `/api/directors` - Get all directors
- GET `/api/countries` - Get all countries
- GET `/api/ratings` - Get all ratings

### Recommendations (AI-Based)
- GET `/api/recommendations/{profileId}` - Get personalized recommendations
- POST `/api/recommendations/preferences/{profileId}` - Set genre preferences
- GET `/api/recommendations/preferences/{profileId}` - Get genre preferences
- POST `/api/recommendations/{profileId}/by-genres` - Get genre-based recommendations
- POST `/api/recommendations/preferences/{profileId}/add/{genreId}` - Add genre preference
- DELETE `/api/recommendations/preferences/{profileId}/remove/{genreId}` - Remove preference

### Watchlist
- GET `/api/watchlist/{profileId}` - Get profile watchlist
- POST `/api/watchlist` - Add to watchlist
- DELETE `/api/watchlist/{profileId}/{titleId}` - Remove from watchlist

### Watch History
- GET `/api/watch-history/{profileId}` - Get watch history
- POST `/api/watch-history` - Add watch history entry
- PUT `/api/watch-history/{watchId}/complete` - Mark as completed

### User Ratings
- GET `/api/user-ratings/{profileId}` - Get profile ratings
- POST `/api/user-ratings` - Rate a title
- PUT `/api/user-ratings` - Update rating

### Admin
- POST `/api/admin/login` - Admin login

### Health Check
- GET `/api/health` - System health status

## Testing Steps

### 1. Start MySQL Database
```bash
# Ensure MySQL is running
mysql -u root -p
USE netflix_db;
SHOW TABLES;
```

### 2. Build and Run Spring Boot Application
```bash
cd "C:\Users\piyus\Documents\SQL project"
mvnw clean package
java -jar target/netflix-database-manager-1.0.0.jar
```

### 3. Test Backend Connection
Open browser: http://localhost:8082/api/health

Expected Response:
```json
{
  "status": "UP",
  "db": "OK",
  "counts": {
    "titles": 8807,
    "actors": 7691,
    "directors": 4528,
    "countries": 123,
    "genres": 42,
    "ratings": 14
  }
}
```

### 4. Test Frontend
Open: http://localhost:8082/index.html

### 5. Test API Endpoints
```bash
# Get all genres
curl http://localhost:8082/api/genres

# Get all titles (first 10)
curl http://localhost:8082/api/titles

# Login test user
curl -X POST http://localhost:8082/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo1@example.com","password":"password1"}'
```

## Frontend-Backend Integration

### API Base URL
- Development: `http://localhost:8082/api`
- Frontend automatically detects: `${window.location.origin}/api`

### CORS Configuration ✓
- Allowed Origins: localhost:3000, localhost:5500, 127.0.0.1:5500
- Allowed Methods: GET, POST, PUT, DELETE, OPTIONS
- Credentials: Enabled

## Database Connection
- URL: `jdbc:mysql://localhost:3306/netflix_db`
- Username: `root`
- Password: `asdfghjkl`
- Driver: `com.mysql.cj.jdbc.Driver`

## Port Configuration
- Backend API: Port 8082
- MySQL: Port 3306

## Troubleshooting

### Database Connection Issues
1. Verify MySQL is running
2. Check credentials in `application.properties`
3. Ensure `netflix_db` database exists
4. Run schema.sql to create tables

### CORS Issues
1. Check browser console for CORS errors
2. Verify CORS configuration in NetflixApplication.java
3. Ensure frontend origin is in allowed list

### API Not Responding
1. Check if Spring Boot is running on port 8082
2. Verify no port conflicts
3. Check application logs for errors

