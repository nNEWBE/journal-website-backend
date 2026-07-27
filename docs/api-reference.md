# API Reference — GBJ Journal Management Backend

Base URL (dev): `http://localhost:8080/api/v1`

All protected endpoints require:
```
Authorization: Bearer <access_token>
```

---

## Authentication — `/api/v1/auth`

### POST `/auth/login`
Login and receive access + refresh tokens.

**Request:**
```json
{
  "email": "author@gonouniversity.edu.bd",
  "password": "demopass"
}
```

**Response `200`:**
```json
{
  "accessToken": "eyJhbG...",
  "refreshToken": "550e8400-e29b-41d4-a716-...",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "user": {
    "id": 5,
    "fullName": "Ayesha Siddique",
    "email": "author@gonouniversity.edu.bd",
    "role": "author",
    "institution": "Gono Bishwabidyalay"
  }
}
```

---

### POST `/auth/register`
Register a new author account.

**Request:**
```json
{
  "fullName": "Dr. Rahim Uddin",
  "email": "rahim@example.com",
  "password": "SecurePass123!",
  "institution": "Gono Bishwabidyalay",
  "department": "Computer Science",
  "country": "BD"
}
```

**Response `201`:** Same shape as `/auth/login`

---

### POST `/auth/refresh`
Exchange a refresh token for a new access token (single-use rotation).

**Request:**
```json
{ "refreshToken": "550e8400-e29b-41d4-a716-..." }
```

**Response `200`:** Same shape as `/auth/login`

---

### POST `/auth/logout` 🔒
Revoke the current refresh token.

**Request:**
```json
{ "refreshToken": "550e8400-e29b-41d4-a716-..." }
```

**Response `200`:** `{ "message": "Logged out successfully." }`

---

### GET `/auth/me` 🔒
Get the authenticated user's profile.

**Response `200`:**
```json
{
  "id": 5,
  "fullName": "Ayesha Siddique",
  "email": "author@gonouniversity.edu.bd",
  "role": "author",
  "institution": "Gono Bishwabidyalay",
  "department": "Biology",
  "country": "BD",
  "orcid": "0000-0000-0000-0001"
}
```

---

### PUT `/auth/profile` 🔒
Update the authenticated user's profile.

**Request:**
```json
{
  "fullName": "Ayesha Siddique",
  "institution": "Updated University",
  "department": "Biochemistry",
  "country": "BD",
  "orcid": "0000-0000-0000-0001",
  "researchInterests": "Genomics, Proteomics"
}
```

---

## Articles — `/api/v1/articles`

### GET `/articles`
Paginated public article listing with optional filters.

**Query params:**
| Param | Type | Description |
|---|---|---|
| `q` | string | Full-text search (title + abstract) |
| `type` | string | Filter by article type |
| `topic` | string | Filter by topic |
| `issueLabel` | string | Filter by issue label |
| `sort` | string | `latest` \| `mostViewed` \| `mostDownloaded` \| `mostCited` |
| `page` | int | Page number (0-based, default 0) |
| `size` | int | Page size (default 10) |

**Response `200`:**
```json
{
  "content": [
    {
      "id": 1,
      "slug": "genomics-bangladesh-population",
      "title": "Genomic Diversity in the Bangladesh Population",
      "authors": [{ "name": "Dr. Ayesha Siddique", "affiliation": "Gono Bishwabidyalay" }],
      "type": "Research Article",
      "topic": "Genomics",
      "issueLabel": "Volume 1, Issue 1",
      "publishedDate": "2024-01-15",
      "abstractText": "...",
      "keywords": ["genomics", "Bangladesh"],
      "doi": "10.1234/gbj.2024.001",
      "pdfUrl": "/api/v1/files/abc123.pdf",
      "metrics": { "views": 245, "downloads": 89, "citations": 12 }
    }
  ],
  "totalElements": 50,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

---

### GET `/articles/{slug}`
Article detail view (also increments view counter).

**Response `200`:** Single article with full sections array included.

---

### POST `/articles/{slug}/download`
Record a PDF download (increments download counter).

**Response `200`:** `{ "downloads": 90 }`

---

## Issues — `/api/v1/issues`

### GET `/issues`
List all published issues.

### GET `/issues/current`
Get the current issue with articles grouped by type (Table of Contents).

**Response `200`:**
```json
{
  "id": 4,
  "issueKey": "2026-2",
  "issueLabel": "Volume 3, Issue 2",
  "year": "2026",
  "month": "July 2026",
  "articleCount": 5,
  "tableOfContents": {
    "Research Article": [ { "id": 1, "title": "...", "slug": "..." } ],
    "Review Article": [ { "id": 3, "title": "...", "slug": "..." } ]
  }
}
```

### GET `/issues/{issueKey}`
Get a specific issue by key (e.g. `2026-2`).

---

## Metadata — Public

### GET `/topics`
List all distinct article topics.

**Response:** `["Genomics", "Public Health", "Biochemistry", ...]`

### GET `/article-types`
List all distinct article types.

**Response:** `["Research Article", "Review Article", "Case Report", ...]`

### GET `/editorial-board`
List all editorial board members in display order.

**Response:**
```json
[
  {
    "id": 1,
    "name": "Prof. Dr. Laila Rahman",
    "role": "Editor-in-Chief",
    "affiliation": "Gono Bishwabidyalay",
    "department": "Medicine",
    "email": "editor@gonouniversity.edu.bd",
    "displayOrder": 1
  }
]
```

---

## Files — `/api/v1/files`

### GET `/files/{storedFilename}`
Download a file by its UUID-based stored filename.

**Response:** File stream with appropriate `Content-Type` header.

---

## Submissions — `/api/v1/submissions` 🔒

### POST `/submissions`
Create a new draft manuscript.

**Request:**
```json
{
  "title": "Antibiotic Resistance in Dhaka Urban Hospitals",
  "runningTitle": "Antibiotic Resistance Dhaka",
  "type": "Research Article",
  "abstractText": "This study examines...",
  "keywords": "antibiotic, resistance, Bangladesh",
  "topic": "Microbiology",
  "coverLetter": "Dear Editor...",
  "conflictOfInterest": "None declared.",
  "fundingStatement": "This research was funded by...",
  "ethicsStatement": "Approved by IRB #2026-001.",
  "dataAvailability": "Data available on request.",
  "aiDeclaration": "No AI tools were used.",
  "copyrightAgreed": true,
  "authors": [
    {
      "name": "Dr. Karim Hossain",
      "email": "karim@example.com",
      "affiliation": "Dhaka Medical College",
      "orcid": "0000-0000-0000-0002",
      "authorOrder": 1,
      "corresponding": true
    }
  ]
}
```

**Response `201`:** Full submission object with generated `submissionId`.

---

### GET `/submissions/my`
List the authenticated author's own manuscripts.

---

### GET `/submissions/{id}`
Get a single submission detail (must be owner, editor, or admin).

---

### POST `/submissions/{id}/files`
Upload a file attachment for a submission.

**Request:** `multipart/form-data`
- `file` — the file (PDF, DOCX, image, etc.)
- `fileType` — one of: `MANUSCRIPT`, `COVER_LETTER`, `FIGURE`, `SUPPLEMENTARY`, `REVISED`

---

### POST `/submissions/{id}/submit`
Move manuscript from `DRAFT` → `SUBMITTED`.
> Sends submission confirmation email to author.

### POST `/submissions/{id}/withdraw`
Withdraw a manuscript (author only).
> Sends withdrawal confirmation email to author.

---

## Reviewer — `/api/v1/reviewer` 🔒 *(REVIEWER+)*

### GET `/reviewer/assignments`
List the authenticated reviewer's current assignments.

### POST `/reviewer/assignments/{id}/accept`
Accept a review invitation.

### POST `/reviewer/assignments/{id}/decline`
Decline a review invitation.

### POST `/reviewer/assignments/{id}/submit`
Submit a completed peer review.

**Request:**
```json
{
  "recommendation": "ACCEPT",
  "score": 85,
  "commentsToEditor": "Solid methodology, minor revisions needed.",
  "commentsToAuthor": "Please clarify the statistical methods in Section 3.",
  "confidentialComments": "I believe this is a strong paper."
}
```

`recommendation` values: `ACCEPT` | `MINOR_REVISION` | `MAJOR_REVISION` | `REJECT`

---

## Editorial — `/api/v1/editor` 🔒 *(EDITOR+)*

### GET `/editor/submissions`
List active submissions for editorial management.

**Query params:** `status`, `type`, `page`, `size`

### POST `/editor/submissions/{id}/assign-editor`
Assign a managing editor.

**Request:** `{ "editorId": 3 }`

### POST `/editor/submissions/{id}/assign-reviewer`
Invite a peer reviewer.

**Request:**
```json
{
  "reviewerId": 4,
  "dueDate": "2026-08-27"
}
```
> Sends review invitation email to reviewer.

### POST `/editor/submissions/{id}/decision`
Make an editorial decision.

**Request:**
```json
{
  "decision": "ACCEPT",
  "note": "Congratulations, your paper has been accepted."
}
```

`decision` values: `ACCEPT` | `REJECT` | `REVISION_REQUESTED`

> Sends decision notification email to author.

### POST `/editor/submissions/{id}/copyediting`
Move an accepted manuscript to copyediting.

### POST `/editor/submissions/{id}/schedule`
Schedule a manuscript for issue publication.

---

## Admin — `/api/v1/admin` 🔒 *(ADMIN+)*

### GET `/admin/stats`
Get real-time dashboard statistics.

**Response `200`:**
```json
{
  "liveSubmissions": 12,
  "underReview": 5,
  "accepted": 3,
  "publishedArticles": 45,
  "activeReviewers": 8,
  "publishedIssues": 4
}
```

### PUT `/admin/issues/{id}/set-current`
Set a specific issue as the current/featured issue.

---

## Error Responses

All errors follow a consistent JSON structure:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Only DRAFT submissions can be submitted.",
  "timestamp": "2026-07-27T06:00:00Z",
  "path": "/api/v1/submissions/1/submit"
}
```

| HTTP Status | Meaning |
|---|---|
| `400 Bad Request` | Invalid input, rule violation |
| `401 Unauthorized` | Missing or invalid JWT |
| `403 Forbidden` | Insufficient role/permission |
| `404 Not Found` | Resource does not exist |
| `500 Internal Server Error` | Unexpected server error |
