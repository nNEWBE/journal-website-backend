# Gono Bishwabidyalay Journal Management — Backend

A secure, production-ready REST API for the **Gono Bishwabidyalay (GBJ) Journal Management System** built with Spring Boot 4.1 and Java 25.

---

## Table of Contents
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Demo Accounts](#demo-accounts)
- [API Overview](#api-overview)
- [Documentation](#documentation)
- [Environment Variables](#environment-variables)
- [Project Structure](#project-structure)
- [Security Architecture](#security-architecture)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 (Temurin) |
| Framework | Spring Boot 4.1.0 |
| Security | Spring Security 6.5 + JJWT 0.12.6 |
| Database | PostgreSQL / Supabase PostgreSQL |
| Build | Gradle 9.5 |
| Email | Spring Mail + Brevo SMTP |
| Templates | Thymeleaf |
| Password Hashing | BCrypt (cost 12) |

---

## Getting Started

### Prerequisites
- **JDK 25** (Temurin)
- **PostgreSQL Database** (Local PostgreSQL or [Supabase PostgreSQL](https://supabase.com))

### 1. Fill in your credentials

Open [`.env`](./.env) and verify your PostgreSQL credentials:

```env
# Supabase Cloud PostgreSQL:
DATABASE_URL=jdbc:postgresql://aws-0-ap-southeast-2.pooler.supabase.com:5432/postgres?sslmode=require
DATABASE_USERNAME=gbjournal_user.irftwqhxtojjjdsporgk
DATABASE_PASSWORD=your-password
```

### 2. Start the backend

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

The app **automatically loads `.env`** on startup.

| Endpoint | URL |
|---|---|
| API base | `http://localhost:8080/api/v1` |
| Health Check | `http://localhost:8080/api/v1/health` |

---

## Demo Accounts

Seeded automatically on every startup:

| Role | Email | Password |
|---|---|---|
| Super Admin | `superadmin@gonouniversity.edu.bd` | `demopass` |
| Admin | `admin@gonouniversity.edu.bd` | `demopass` |
| Editor | `editor@gonouniversity.edu.bd` | `demopass` |
| Reviewer | `reviewer@gonouniversity.edu.bd` | `demopass` |
| Author | `author@gonouniversity.edu.bd` | `demopass` |

---

## API Overview

| Area | Base Path | Access |
|---|---|---|
| Authentication | `/api/v1/auth/**` | Public / Authenticated |
| Articles | `/api/v1/articles/**` | Public (GET) |
| Issues | `/api/v1/issues/**` | Public |
| Submissions | `/api/v1/submissions/**` | AUTHOR+ |
| Reviewer Tasks | `/api/v1/reviewer/**` | REVIEWER+ |
| Editorial | `/api/v1/editor/**` | EDITOR+ |
| Administration | `/api/v1/admin/**` | ADMIN+ |
| File Downloads | `/api/v1/files/**` | Public |
| Metadata | `/api/v1/topics`, `/api/v1/article-types`, `/api/v1/editorial-board` | Public |

---

## Documentation

Additional guides are in the [`docs/`](./docs) folder:

| Guide | Description |
|---|---|
| [📧 Email Setup](./docs/email-setup.md) | Step-by-step Brevo (free SMTP) setup for dev and production |
| [🐘 PostgreSQL Setup](./docs/postgresql-setup.md) | Step-by-step guide for local Docker, local install & cloud PostgreSQL |
| [📖 API Reference](./docs/api-reference.md) | All REST endpoints with request/response examples |

> Copy [`.env.example`](./.env.example) → `.env` and fill in your credentials.

---

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `BREVO_SMTP_USER` | For email | Your Brevo login email |
| `BREVO_SMTP_KEY` | For email | Brevo SMTP key |
| `JWT_SECRET` | **Prod only** | 256-bit HMAC secret for JWT signing |
| `DATABASE_URL` | **Prod only** | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | **Prod only** | PostgreSQL username |
| `DATABASE_PASSWORD` | **Prod only** | PostgreSQL password |
| `APP_BASE_URL` | **Prod only** | e.g. `https://api.gonouniversity.edu.bd` |
| `JOURNAL_URL` | **Prod only** | e.g. `https://journal.gonouniversity.edu.bd` |
| `CORS_ORIGINS` | **Prod only** | Comma-separated allowed frontend origins |

---

## Production Deployment

```bash
# Build the JAR
./gradlew clean bootJar

# Set environment variables, then run:
java -jar build/libs/gbjournal-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## Project Structure

```
src/main/java/com/research/gbjournal/
├── config/          # MailProperties, DataInitializer
├── controller/      # REST controllers (Auth, Article, Issue, Submission, ...)
├── dto/             # Request/Response DTOs
├── entity/          # JPA entities (User, Submission, Article, Issue, ...)
├── exception/       # GlobalExceptionHandler, custom exceptions
├── repository/      # Spring Data JPA repositories
├── security/        # JWT, SecurityConfig, filters, handlers
└── service/         # Business logic (Auth, Email, Submission, Review, ...)

src/main/resources/
├── templates/email/ # Thymeleaf HTML email templates
├── application.yaml
├── application-dev.yaml    # PostgreSQL (Supabase / Local) + Brevo SMTP (dev)
└── application-prod.yaml   # PostgreSQL (Supabase Cloud) + Brevo SMTP (prod)

docs/
└── email-setup.md   # Step-by-step Brevo email configuration guide
```

---

## Security Architecture

- **Access Tokens** — Short-lived JWTs (15 min), HMAC-SHA256 signed
- **Refresh Tokens** — Long-lived (7 days), stored in DB with **single-use rotation**
- **Token Reuse Detection** — Revokes all user sessions if a stolen token is detected
- **Password Hashing** — BCrypt cost factor 12
- **RBAC** — Route-level + method-level via `@PreAuthorize`
- **CORS** — Configured for Next.js frontend origin
- **File Security** — UUID-based filenames; path traversal guard
