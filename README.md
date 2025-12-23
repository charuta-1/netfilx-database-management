# Netflix Database Manager

![Java](https://img.shields.io/badge/Java-17-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.5-6DB33F?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)

A full-stack Netflix-style platform that blends a Spring Boot + MySQL backend with lightweight HTML/JS frontends for both end users and administrators. The system supports intelligent recommendations, granular maturity controls, detailed content management, and subscription-aware user journeys.

---

## 📚 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Highlights](#highlights)
- [Project Layout](#project-layout)
- [Prerequisites](#prerequisites)
- [Setup](#setup)
- [Running the Platform](#running-the-platform)
- [Using the Frontend Apps](#using-the-frontend-apps)
- [API Cheat Sheet](#api-cheat-sheet)
- [Database & Data Loading](#database--data-loading)
- [Verification & QA](#verification--qa)
- [Troubleshooting](#troubleshooting)
- [Next Steps](#next-steps)

---

## Overview

The Netflix Database Manager delivers a production-style environment for managing content, users, profiles, and recommendations. It combines:

- A **Spring Boot REST API** that encapsulates business logic, database access, and recommendation strategies.
- A **MySQL schema** populated with Netflix metadata (titles, cast, genres, plans) and tailored tables for watch history, ratings, and preferences.
- Two **static single-page apps** (user and admin) served directly by Spring, providing an intuitive experience inspired by the Netflix UI.

The project is ideal for demonstrating full-stack CRUD flows, recommendation logic, and admin-grade dashboards in a familiar domain.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17, Spring Boot 2.7, Spring JDBC, Spring Security Crypto |
| Database | MySQL 8.x, HikariCP, JDBC Template |
| Frontend | HTML5, Vanilla JavaScript, Fetch API, Netflix-inspired styling |
| Build & Tooling | Maven, Lombok-free POJOs, Windows helper scripts |

---

## Architecture

```
┌───────────────────────────────┐
│          Frontend             │
│ ┌───────────────┐ ┌────────┐ │
│ │ login.html     │ │ admin  │ │
│ │ user dashboard │ │ portal │ │
│ └──────┬────────┘ └────┬───┘ │
└────────┼────────────────┼────┘
         ▼                ▼
   Spring MVC static resource handler
         ▼                ▼
┌─────────────────────────────────────┐
│         REST Controllers            │
│ NetflixController, UserController,  │
│ RecommendationController, ...       │
└────────────────┬────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│          Service Layer              │
│ NetflixService, UserService,        │
│ RecommendationService, ...          │
└────────────────┬────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│              DAO Layer              │
│ JdbcTemplate-based repositories     │
└────────────────┬────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│              MySQL 8 DB             │
│ netflix_db schema + seed data       │
└─────────────────────────────────────┘
```

All HTTP traffic flows through REST controllers, which orchestrate services and DAOs. Static assets are served from `classpath:/frontend`, letting you access the UI without an additional web server.

---

## Highlights

### Backend intelligence
- Age-aware recommendation engine that respects profile overrides and watch history.
- Robust CRUD coverage for titles, cast, countries, genres, ratings, subscriptions, and profiles.
- Defensive service layer with input validation, detailed error responses, and audit-style logging.
- Dedicated health endpoint (`/api/health`) reporting entity counts and DB connectivity.

### User experience
- Netflix-inspired dashboard (`02-user-dashboard.html`) with browse filters, watchlist toggles, and thumbs up/down ratings.
- Local profile state normalization to prevent stale IDs and show consistent maturity labels.
- Age-based filtering baked into browsing and recommendations, including parental overrides.

### Admin experience
- Secure login (`admin/admin123` by default) with configurable credentials.
- Content management forms for titles, actors, directors, and system stats.
- Scripts (`start-fixed-server.bat`, `restart-server.bat`, `kill-server.bat`) streamline dev workflows on Windows.

---

## Project Layout

```
.
├── pom.xml
├── load.sql                     # Optional data loader for netflix_db
├── netflix schema.sql           # Schema + seed definitions
├── start-fixed-server.bat       # Build + run helper
├── src
│   └── main
│       ├── java
│       │   └── com
│       │       └── netflix
│       │           ├── config/              # MVC + static resource config
│       │           ├── controller/          # REST endpoints
│       │           ├── dao/                 # JdbcTemplate repositories
│       │           ├── main/                # Spring Boot entry point
│       │           ├── model/               # Domain objects & DTOs
│       │           ├── service/             # Business logic
│       │           ├── sql/                 # SQL helpers & mappers
│       │           └── util/                # Shared utility classes
│       └── resources
│           ├── application.properties       # Datasource + CORS + admin creds
│           └── frontend/                    # login.html, admin.html, JS bundles
└── target/...
```

Additional documentation lives in `DATABASE_TEST_INFO.md` (API/SQL testing guide) and `ALL_FIXES_COMPLETE.md` (bug fix changelog).

---

## Prerequisites

- Java Development Kit **17**
- Maven **3.8+**
- MySQL **8.0+** with a database named `netflix_db`
- (Optional) MySQL Workbench for inspecting data
- Windows PowerShell for provided helper scripts (or translate commands to your shell)

---

## Setup

1. **Clone or copy the project** to your workspace of choice.
2. **Configure MySQL credentials** in `src/main/resources/application.properties`.
   - Defaults assume `root / asdfghjkl` on `localhost:3306`.
   - Update `spring.datasource.username`, `spring.datasource.password`, and `server.port` as needed.
3. **Create and seed the database (once):**
   - Create the schema: `mysql -u <user> -p < netflix\ schema.sql`
   - Optionally load sample data: `mysql -u <user> -p netflix_db < load.sql`
4. **Install dependencies** by running `mvn -q dependency:go-offline` (useful for first-time setup).

> 💡 Tip: Use the provided `.bat` scripts if you prefer a single-click setup on Windows.

---

## Running the Platform

### Option A – Maven (dev friendly)
1. From the project root run `mvn spring-boot:run`.
2. The server boots on `http://localhost:8082` (change via `server.port`).

### Option B – Packaged JAR
1. Build with `mvn clean package`.
2. Launch `java -jar target/netflix-database-manager-1.0.0.jar`.

### Option C – Windows scripts
- `start-fixed-server.bat`: builds and starts the app.
- `restart-server.bat`: restarts the running instance.
- `kill-server.bat`: terminates Java processes if ports are stuck.

Logs will display JDBC/Hikari messages confirming database connectivity. Look for `Tomcat started on port(s): 8082` and `Started NetflixApplication`.

---

## Using the Frontend Apps

All static assets are served from `classpath:/frontend`, so you can open them directly once the backend is running:

| URL | Purpose |
|-----|---------|
| `http://localhost:8082/login.html` | End-user login + registration flow |
| `http://localhost:8082/02-user-dashboard.html` | Profile dashboard, browse, watchlist, ratings |
| `http://localhost:8082/admin.html` | Admin console for content & user management |

### Default credentials

- **Admin:** `admin / admin123` (override via `application.properties`)
- **User:** register via UI or seed `users` table; passwords are stored with BCrypt

### User journey snapshot

1. Create an account (DOB optional but recommended for age filtering).
2. Set up profiles and, if needed, override maturity levels.
3. Explore the dashboard: filter by type, genre, country, release year, and maturity rating.
4. Manage watchlist items and submit thumbs up/down ratings; changes sync with the backend via REST.

---

## API Cheat Sheet

| Area | Method & Endpoint | Notes |
|------|-------------------|-------|
| Health | `GET /api/health` | Returns service status plus entity counts |
| Titles | `GET /api/titles` | Full catalog with normalized IDs & metadata |
| Titles | `POST /api/titles` | Admin-only create with auto `show_id` normalization |
| Actors/Directors | `GET /api/actors`, `GET /api/directors` | Lightweight DTO responses |
| Users | `POST /api/users/register` | Registers user, hashes password, optional DOB |
| Users | `POST /api/users/login` | Returns user, profiles, subscription snapshot |
| Profiles | `POST /api/profiles` | Create profile with override maturity rating |
| Watchlist | `POST /api/watchlist`, `GET /api/watchlist/{profileId}` | Manage saved titles |
| Ratings | `POST /api/ratings` | Thumbs up/down per profile |
| Watch History | `POST /api/watch-history` | Track completed titles |
| Recommendations | `GET /api/recommendations/{profileId}` | Hybrid engine (history + preferences) |
| Genre Preferences | `POST /api/recommendations/preferences/{profileId}` | Set onboarding genres |
| Admin | `POST /api/admin/login` | Validates against configured credentials |

Detailed payloads and additional endpoints (e.g., subscription plans) are documented in `DATABASE_TEST_INFO.md`.

---

## Database & Data Loading

- `netflix schema.sql` – defines all tables, relationships, indexes, and base lookup data.
- `load.sql` – optional bulk insert of Netflix titles, cast, countries, etc. Expect large imports (thousands of rows).
- Tables of interest: `users`, `user_profiles`, `title`, `watch_history`, `user_ratings`, `watchlist`, `user_genre_preferences`, `SubscriptionPlans`, `UserSubscriptions`.

### Quick sanity queries

```sql
USE netflix_db;
SELECT COUNT(*) AS titles FROM title;
SELECT COUNT(*) AS users FROM users;
SELECT * FROM user_profiles LIMIT 5;
```

---

## Verification & QA

- Hit `http://localhost:8082/api/health` to confirm DB connectivity.
- Use the cURL samples in `DATABASE_TEST_INFO.md` for smoke testing registration, login, and admin flows.
- Browser console (F12) fetch scripts are provided in `ALL_FIXES_COMPLETE.md` to validate JSON payloads quickly.
- Successful startup logs should include `HikariPool-1 - Start completed`.

---

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|--------------|-----|
| `HTTP 500` during auth | Missing input validation or duplicate email | See enhanced error messaging in `UserController`; check payload |
| CORS error | Accessing from new origin | Add origin to `spring.web.cors.allowed-origins` or adjust allowed patterns |
| DB connection refused | MySQL down or wrong credentials | Verify service status, credentials, or update JDBC URL |
| Static assets 404 | Server not running or port changed | Restart backend, confirm `server.port`, access `http://localhost:<port>/login.html` |

---

## Next Steps

- Externalize secrets (admin credentials, DB password) into environment variables or a vault.
- Containerize the stack with Docker Compose for reproducible deployments.
- Extend automated testing with Spring Boot slices or Postman collections.
- Integrate a modern frontend framework (React/Vue) reusing the REST API.

Enjoy building, managing, and exploring your Netflix-inspired platform! 🍿