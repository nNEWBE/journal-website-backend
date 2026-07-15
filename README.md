# Gono Bishwabidyalay Journal Management Portal - Backend

This is the backend API service for the **Gono Bishwabidyalay Journal Management Portal**. It provides the database persistence, secure REST APIs, role-based access control (RBAC), and editorial workflows required to power the academic publishing platform.

---

## 📖 Overview

The backend is built as a robust, secure, and scalable **Spring Boot** application using **Java 25**. It supports the frontend Next.js application by offering APIs for manuscript submission, peer-review tracking, editorial queue management, publication archives, and role governance.

---

## 🛠️ Tech Stack

- **Language:** Java 25 (LTS)
- **Framework:** Spring Boot 4.1.0
- **Build Tool:** Gradle
- **Security:** Spring Security (Token-based Authentication & Role-Based Access Control)
- **Database:** PostgreSQL (Production) / H2 (Development Console)
- **ORM:** Spring Data JPA / Hibernate
- **Containers:** Docker Compose (for local PostgreSQL database instance)
- **Utilities:** Lombok, Spring Boot DevTools, Validation API
- **Testing:** JUnit 5, Spring Boot Starter Test

---

## 🔑 User Roles & Permissions

The system implements strict Role-Based Access Control (RBAC) supporting the following roles:

*   **Super Admin:** Overall platform administration, role updates, system-level audits, and journal office configuration.
*   **Admin:** Configures journal site settings, manages issue volumes, updates submission policies, and configures article types.
*   **Editor:** Triages submitted manuscripts, assigns reviewers, makes final decision proposals, and schedules accepted work for upcoming issues.
*   **Reviewer:** Evaluates assigned manuscripts, manages invitations, and submits structured review recommendations.
*   **Author:** Submits new manuscripts, uploads files/PDFs, responds to revisions, and tracks workflow status.
*   **Visitor:** Anonymous access to public journals, article search, current/archived issues, editorial board details, and public policies.

---

## 📂 Project Structure

```text
journal-management-backend/
├── compose.yaml                      # Docker Compose file for local PostgreSQL
├── build.gradle                      # Build and dependency configuration
├── gradlew & gradlew.bat             # Gradle wrapper scripts
└── src/
    ├── main/
    │   ├── java/com/research/gbjournal/
    │   │   ├── GbjournalApplication.java  # Main application class
    │   │   ├── config/               # Security and global configurations (CORS, JWT)
    │   │   ├── controller/           # REST Controller API endpoints
    │   │   ├── entity/               # JPA Entities / Database Models
    │   │   ├── repository/           # Spring Data JPA Repositories
    │   │   ├── service/              # Business Logic Interfaces and Implementations
    │   │   └── dto/                  # Data Transfer Objects for API requests/responses
    │   └── resources/
    │       ├── application.yaml      # General application configuration
    │       └── application-dev.yaml  # Local development environment profile
    └── test/                         # Unit and integration test suites
```

---

## 🚀 Getting Started

### Prerequisites

Ensure you have the following installed on your local machine:
- **JDK 25**
- **Docker & Docker Compose** (for PostgreSQL database)
- **Git**

### 1. Database Setup

Spin up the local PostgreSQL database using the provided Docker Compose file:

```bash
docker compose up -d
```

This starts a Postgres instance with the parameters configured in `compose.yaml`:
*   **Host:** `localhost`
*   **Port:** `5432`
*   **Database:** `mydatabase`
*   **User:** `myuser`
*   **Password:** `secret`

### 2. Configuration (`application.yaml`)

By default, the application is named `gbjournal`. To connect to your database, you can configure your connection credentials inside your `application.yaml` or through active profiles.

Example Configuration:
```yaml
spring:
  application:
    name: gbjournal
  datasource:
    url: jdbc:postgresql://localhost:5432/mydatabase
    username: myuser
    password: secret
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  h2:
    console:
      enabled: true
      path: /h2-console
```

### 3. Run the Development Server

Execute the bootRun task using the Gradle wrapper:

```bash
# On Linux/macOS
./gradlew bootRun

# On Windows (PowerShell/CMD)
./gradlew.bat bootRun
```

The server will start up on `http://localhost:8080` (by default).

### 4. Running Tests

Run the test suite using:

```bash
./gradlew test
```

---

## 🛰️ Planned REST API Endpoints

### Authentication
*   `POST /api/auth/register` - Register a new user (default Author role).
*   `POST /api/auth/login` - Authenticate user and retrieve JWT token.

### Public Article & Issue Discovery
*   `GET /api/articles` - Retrieve and search articles (filters for keywords, author, topic, issue).
*   `GET /api/articles/{id}` - Get full metadata, abstract, and sections of an article.
*   `GET /api/issues/current` - Retrieve table of contents and metadata of the current issue.
*   `GET /api/issues` - Browse previous volume/issue archives.

### Manuscript & Submissions (Author)
*   `POST /api/submissions` - Start/save a new manuscript wizard submission.
*   `GET /api/submissions/my` - Fetch submissions initiated by the logged-in author.
*   `POST /api/submissions/{id}/files` - Upload PDF manuscripts or supplementary files.
*   `POST /api/submissions/{id}/submit` - Finalize manuscript check and submit to editor queue.

### Peer Review (Reviewer)
*   `GET /api/reviews/assigned` - List peer review invitations and assignments.
*   `POST /api/reviews/{id}/respond` - Accept or decline a review invitation.
*   `POST /api/reviews/{id}/submit` - Submit structured reviews and decision recommendations.

### Editorial Queue (Editor)
*   `GET /api/editor/queue` - Retrieve pending, active, and completed manuscript queues.
*   `POST /api/editor/assign-reviewer` - Assign peer reviewers to a manuscript.
*   `POST /api/editor/decision` - Issue editorial decisions (Accept, Revisions, Reject).

### System Settings (Admin / Super Admin)
*   `PUT /api/admin/policies` - Update submission guidelines, ethical regulations, and reviewer policies.
*   `POST /api/admin/issues/create` - Create new volume and issue slots.
*   `PUT /api/admin/users/{id}/roles` - Oversee system-role governance.

---

## 🔒 Security & CORS

To connect the Next.js frontend running on `http://localhost:3000` with the backend on `http://localhost:8080`, ensure CORS is configured in `WebMvcConfigurer`.

Example Configuration:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

---

## 📄 License

This project is configured for Gono Bishwabidyalay. All rights reserved.
