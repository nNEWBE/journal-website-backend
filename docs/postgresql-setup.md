# PostgreSQL Setup & Implementation Guide

This guide provides step-by-step instructions for configuring and connecting a **PostgreSQL** database to the Gono Bishwabidyalay Journal Management (GBJ) backend, both for **Local Development** and **Production Deployment**.

---

## 📋 Architecture & Supported PostgreSQL Options

The backend is configured exclusively for **PostgreSQL** (No H2 database is used in any environment).

You can connect to either:
1. **Supabase Cloud PostgreSQL (Recommended)** — Low latency, managed cloud database with SSL and pooled connections.
2. **Local PostgreSQL** — Installed via PostgreSQL installer for Windows or Docker.

---

## Option A: Supabase Cloud PostgreSQL (Recommended)

1. Connect to **[Supabase](https://supabase.com)** project `irftwqhxtojjjdsporgk`.
2. Connection details are configured in `.env`:

```env
DATABASE_URL=jdbc:postgresql://aws-0-ap-southeast-2.pooler.supabase.com:5432/postgres?sslmode=require
DATABASE_USERNAME=gbjournal_user.irftwqhxtojjjdsporgk
DATABASE_PASSWORD=your-secure-password
```

---

## Option B: Run PostgreSQL via Docker (Local)

If you have Docker Desktop installed:

### Step 1 — Run PostgreSQL Container
Run the following command in PowerShell/Terminal:

```powershell
docker run --name gbjournal-postgres -e POSTGRES_DB=gbjournal -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=shuvo1234 -p 5432:5432 -d postgres:16-alpine
```

### Step 2 — Update `.env` File
Open your `.env` file in the backend root directory and set:

```env
DATABASE_URL=jdbc:postgresql://localhost:5432/gbjournal
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=shuvo1234
```

---

## Option C: Install PostgreSQL Locally on Windows

### Step 1 — Download & Install PostgreSQL
1. Download the installer from **[PostgreSQL Official Site](https://www.postgresql.org/download/windows/)** (Version 15 or 16).
2. Run the installer and keep default components selected (PostgreSQL Server, pgAdmin 4, Command Line Tools).
3. Set a superuser (`postgres`) password (e.g. `postgres123`) when prompted.
4. Keep the default port `5432`.

### Step 2 — Create Database and User
Open **SQL Shell (psql)** or **pgAdmin 4** and execute:

```sql
-- Create database
CREATE DATABASE gbjournal;

-- Create dedicated application user
CREATE USER gbjournal WITH ENCRYPTED PASSWORD 'secretpass';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE gbjournal TO gbjournal;
\c gbjournal
GRANT ALL ON SCHEMA public TO gbjournal;
```

### Step 3 — Update `.env` File
Update your backend `.env` file:

```env
DATABASE_URL=jdbc:postgresql://localhost:5432/gbjournal
DATABASE_USERNAME=gbjournal
DATABASE_PASSWORD=secretpass
```

---

## 🚀 Running the Application with PostgreSQL

### 1. Running with Dev Profile using PostgreSQL
Make sure `.env` contains your database credentials, then start Spring Boot:

```powershell
$env:JAVA_HOME = "C:\Users\Shuvo Debnath\.jdks\temurin-25.0.3"
$env:Path      = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew bootRun --args="--spring.profiles.active=dev"
```

### 2. Running with Production Profile
For staging/production deployment, activate the `prod` profile:

```powershell
.\gradlew bootRun --args="--spring.profiles.active=prod"
```

---

## 🔍 Database Table Schema Created Automatically

When the backend starts up, Spring Data JPA / Hibernate automatically creates all required tables and relationships:

| Table | Description |
|---|---|
| `users` | User accounts, roles (`AUTHOR`, `REVIEWER`, `EDITOR`, `ADMIN`, `SUPER_ADMIN`), hashed passwords |
| `refresh_tokens` | Single-use refresh token rotation data |
| `articles` | Published articles, DOIs, view/download counters |
| `article_sections` | Article full-text section content |
| `article_authors` | Co-author metadata for published articles |
| `article_keywords` | Article keywords index |
| `issues` | Journal issues & volume archives |
| `submissions` | Manuscript submissions & workflow lifecycle status |
| `submission_authors` | Manuscript co-author details |
| `submission_files` | Uploaded manuscript files (PDF, figures, cover letters) |
| `review_assignments` | Peer review tasks, scores, and evaluation comments |
| `board_members` | Editorial board member profiles and display order |

---

## 🛠️ Verification & Troubleshooting

### Check Database Connection
Look for these logs on application startup:

```
INFO  --- [gbjournal] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
INFO  --- [gbjournal] com.zaxxer.hikari.pool.HikariPool      : HikariPool-1 - Added connection conn0: url=jdbc:postgresql://...
INFO  --- [gbjournal] org.hibernate.orm.connections.pooling  : HHH10001005: Database info: PostgreSQL
```

### Common Errors & Solutions

| Error | Cause | Solution |
|---|---|---|
| `Connection refused: connect` | PostgreSQL is not running or wrong port | Start PostgreSQL service / container (`docker start gbjournal-postgres`) |
| `FATAL: password authentication failed for user` | Wrong username or password in `.env` | Double check `DATABASE_USERNAME` and `DATABASE_PASSWORD` in `.env` |
| `FATAL: database "gbjournal" does not exist` | Database has not been created yet | Run `CREATE DATABASE gbjournal;` in SQL shell |
| `SSL error: PSQLException` | Cloud provider requires SSL | Add `?sslmode=require` to `DATABASE_URL` |
