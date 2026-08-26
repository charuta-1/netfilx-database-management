# 🎬 Netflix Database Manager

A full-stack Netflix-inspired platform for managing movies, TV shows, users, profiles, subscriptions, watchlists, ratings, and personalized recommendations.

The project combines a **Spring Boot REST API**, **MySQL database**, and **HTML/CSS/JavaScript frontend** to provide separate experiences for users and administrators.

---

## ✨ Features

### 👤 User Features

* User registration and login
* Multiple user profiles
* Age and maturity-based content filtering
* Browse movies and TV shows
* Search and filter content
* Personalized recommendations
* Watchlist management
* Thumbs up/down ratings
* Watch history tracking
* Genre preferences

### 🔐 Admin Features

* Secure admin login
* Manage movies and TV shows
* Manage actors and directors
* View and manage users
* Monitor system data and statistics

### 🧠 Recommendation System

The platform provides personalized recommendations based on:

* Watch history
* User preferences
* Genre preferences
* Profile maturity settings

---

## 🛠️ Technologies Used

### Backend

* Java 17
* Spring Boot 2.7
* Spring JDBC
* Spring Security Crypto

### Database

* MySQL 8
* JDBC Template
* HikariCP

### Frontend

* HTML5
* CSS
* JavaScript
* Fetch API

### Tools

* Maven
* MySQL Workbench

---

## 🏗️ System Architecture

```text id="nf7xk2"
Frontend
   │
   ▼
Spring Boot REST Controllers
   │
   ▼
Service Layer
   │
   ▼
DAO / Repository Layer
   │
   ▼
MySQL Database
```

The frontend communicates with the Spring Boot backend through REST APIs. The backend processes business logic, handles recommendations, and interacts with the MySQL database.

---

## 📂 Project Structure

```text id="k9p3ds"
Netflix-Database-Manager/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/netflix/
│       │       ├── controller/
│       │       ├── service/
│       │       ├── dao/
│       │       ├── model/
│       │       └── config/
│       │
│       └── resources/
│           ├── application.properties
│           └── frontend/
│
├── pom.xml
├── netflix schema.sql
├── load.sql
└── README.md
```

---

## ⚙️ Prerequisites

Make sure you have the following installed:

* Java JDK 17
* Maven
* MySQL 8.0+

---

## 🚀 Setup and Installation

### 1. Configure the Database

Create a MySQL database:

```sql id="y2nb8a"
CREATE DATABASE netflix_db;
```

Import the database schema:

```bash id="m2zqtw"
mysql -u <username> -p netflix_db < "netflix schema.sql"
```

### 2. Run the Application

Using Maven:

```bash id="zx6h4f"
mvn spring-boot:run
```

The application will start on:

```text id="tq2mc1"
http://localhost:8082
```

---

## 🖥️ Application Pages

| Page                     | Description                                             |
| ------------------------ | ------------------------------------------------------- |
| `login.html`             | User login and registration                             |
| `02-user-dashboard.html` | Browse content, watchlist, ratings, and recommendations |
| `admin.html`             | Admin dashboard for content and user management         |



---

## 🎯 Project Objectives

The main objective of this project is to demonstrate a complete full-stack application using Java, Spring Boot, MySQL, and REST APIs.

The project focuses on:

* Database design and management
* CRUD operations
* User and profile management
* Personalized recommendations
* REST API development
* Full-stack integration

---



