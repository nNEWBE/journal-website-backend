# Email Setup Guide — Brevo (Free SMTP)

> **300 emails/day free · No credit card required · Works out-of-the-box with Spring Boot**

---

## Why Brevo?

| Service | Free Tier | Credit Card | SMTP Support | Deliverability |
|---|---|---|---|---|
| **Brevo** ✅ | **300/day (~9,000/mo)** | ❌ Not required | ✅ Yes | ⭐⭐⭐⭐⭐ |
| Resend | 3,000/mo | ❌ Not required | Via API only | ⭐⭐⭐⭐⭐ |
| SendGrid | 100/day | ✅ Required | ✅ Yes | ⭐⭐⭐⭐ |
| Mailgun | 1,000/mo (3 months only) | ✅ Required | ✅ Yes | ⭐⭐⭐⭐ |
| Gmail SMTP | 500/day | ✅ Google account | ✅ Yes | ⭐⭐⭐ |

---

## Step 1 — Create a Free Brevo Account

1. Open **[https://app.brevo.com/account/register](https://app.brevo.com/account/register)**
2. Sign up with your email address — no credit card needed
3. Verify your email via the confirmation link Brevo sends you
4. Complete the basic profile setup (you can skip optional steps)

> [!NOTE]
> The free account gives you **300 emails/day** (~9,000/month) forever with no credit card.

---

## Step 2 — Generate Your SMTP Key

1. Log in at **[https://app.brevo.com](https://app.brevo.com)**
2. Click your **profile avatar** (top-right corner) → select **"SMTP & API"**
3. Click the **"SMTP"** tab
4. Note your SMTP server details:
   - **Server:** `smtp-relay.brevo.com`
   - **Port:** `587`
   - **Login (username):** your Brevo account login email
5. Under **"SMTP Keys"** → click **"Generate a new SMTP key"**
6. Name it `gbjournal-smtp-key` → click **Create**
7. **Copy the key immediately** — it is shown only once

> [!IMPORTANT]
> Your SMTP **username** is your **Brevo login email**, NOT the key name.
> Store the SMTP key in a safe place — you cannot retrieve it again.

---

## Step 3 — Verify Your Sender Domain *(Recommended)*

Verifying your domain prevents emails from landing in spam:

1. In Brevo → **Settings → Senders & IP → Domains**
2. Click **"Add a domain"** → enter `gonouniversity.edu.bd`
3. Brevo will show you DNS records to add:
   - **SPF** — a TXT record on your root domain
   - **DKIM** — a TXT record on a `brevo._domainkey` subdomain
4. Add those records in your domain registrar / DNS management panel
   *(Contact your university IT team if needed)*
5. Back in Brevo, click **"Verify"** — DNS propagation takes a few minutes

> [!TIP]
> Domain verification is optional for testing but **required for production** to ensure emails don't go to spam.

---

## Step 4 — Configure Locally

Never commit real credentials to git. Set them as **environment variables** in your terminal before starting the app.

### PowerShell (Windows)
```powershell
$env:BREVO_SMTP_USER = "your-brevo-login-email@example.com"
$env:BREVO_SMTP_KEY  = "your-brevo-smtp-key"

# Then start the backend
$env:JAVA_HOME = "C:\Users\Shuvo Debnath\.jdks\temurin-25.0.3"
$env:Path      = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew bootRun --args="--spring.profiles.active=dev"
```

### VS Code (Permanent) — `.vscode/launch.json`
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "GBJ Backend (dev)",
      "request": "launch",
      "mainClass": "com.research.gbjournal.GbjournalApplication",
      "args": "--spring.profiles.active=dev",
      "env": {
        "BREVO_SMTP_USER": "your-brevo-login-email@example.com",
        "BREVO_SMTP_KEY": "your-brevo-smtp-key"
      }
    }
  ]
}
```

> [!NOTE]
> The `application-dev.yaml` already reads these variables:
> ```yaml
> spring:
>   mail:
>     host: smtp-relay.brevo.com
>     port: 587
>     username: ${BREVO_SMTP_USER:your-brevo-login-email@example.com}
>     password: ${BREVO_SMTP_KEY:your-brevo-smtp-key}
> ```
> No code changes are needed — just set the environment variables.

---

## Step 5 — Test Email Delivery

1. Start the backend with env vars set (Step 4)
2. Login as the demo author:
   ```http
   POST http://localhost:8080/api/v1/auth/login
   Content-Type: application/json

   { "email": "author@gonouniversity.edu.bd", "password": "demopass" }
   ```
3. Create a draft submission:
   ```http
   POST http://localhost:8080/api/v1/submissions
   Authorization: Bearer <access_token>
   Content-Type: application/json

   {
     "title": "Test Paper",
     "type": "Research Article",
     "abstractText": "Abstract text here.",
     "keywords": "test, email",
     "copyrightAgreed": true
   }
   ```
4. Submit it (replace `{id}` with the returned submission ID):
   ```http
   POST http://localhost:8080/api/v1/submissions/{id}/submit
   Authorization: Bearer <access_token>
   ```
5. Check **[Brevo Transactional Email Logs](https://app.brevo.com/email-dashboard)** — the confirmation email appears within seconds

---

## Step 6 — Production Deployment

Set these environment variables on your server or deployment platform:

### Linux / macOS Server
```bash
export BREVO_SMTP_USER="your-brevo-login-email@example.com"
export BREVO_SMTP_KEY="your-brevo-smtp-key"
export JWT_SECRET="your-256bit-production-jwt-secret"
export DATABASE_URL="jdbc:postgresql://your-db-host:5432/gbjournal"
export DATABASE_USERNAME="gbjournal"
export DATABASE_PASSWORD="your-db-password"
export APP_BASE_URL="https://api.gonouniversity.edu.bd"
export JOURNAL_URL="https://journal.gonouniversity.edu.bd"
export CORS_ORIGINS="https://journal.gonouniversity.edu.bd"

java -jar build/libs/gbjournal-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Docker Compose
```yaml
services:
  backend:
    image: gbjournal-backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      BREVO_SMTP_USER: ${BREVO_SMTP_USER}
      BREVO_SMTP_KEY: ${BREVO_SMTP_KEY}
      JWT_SECRET: ${JWT_SECRET}
      DATABASE_URL: jdbc:postgresql://db:5432/gbjournal
      DATABASE_USERNAME: gbjournal
      DATABASE_PASSWORD: ${DB_PASSWORD}
      APP_BASE_URL: https://api.gonouniversity.edu.bd
      JOURNAL_URL: https://journal.gonouniversity.edu.bd
      CORS_ORIGINS: https://journal.gonouniversity.edu.bd
```

---

## Email Triggers

| User Action | Email Recipient | Template | Header Color |
|---|---|---|---|
| Author **submits** manuscript | Author | `submission-confirmation.html` | Dark blue/green gradient |
| Author **withdraws** manuscript | Author | `editorial-decision.html` | Dark gradient |
| Editor **accepts** | Author | `editorial-decision.html` | 🟢 Green |
| Editor **rejects** | Author | `editorial-decision.html` | 🔴 Red |
| Editor **requests revision** | Author | `editorial-decision.html` | 🟡 Amber |
| Editor **moves to copyediting** | Author | `editorial-decision.html` | Dark gradient |
| Editor **schedules for issue** | Author | `editorial-decision.html` | 🟢 Green |
| Editor **assigns reviewer** | Reviewer | `reviewer-invitation.html` | Purple gradient |

> All emails are sent **asynchronously** — SMTP failures are caught and logged as warnings.
> They will **never** block or crash the API response.

---

## Troubleshooting

| Problem | Solution |
|---|---|
| Emails going to spam | Complete domain verification in Brevo (Step 3) |
| `525 5.7.1 Unauthorized IP address` | Go to Brevo → **SMTP & API → Authorized IPs**, click **Add an IP address** and whitelist your current IP address |
| `AuthenticationFailedException` | Confirm `BREVO_SMTP_USER` = your **login email / login ID** (`b366cd001@smtp-brevo.com`), not the key name |
| `MailConnectException` | Port 587 must be open outbound on your network / server firewall |
| No emails in Brevo logs | Check Spring logs for `"Failed to send email"` — env vars may not be set |
| Daily limit reached (300/day) | Upgrade Brevo plan or wait until midnight UTC for the counter to reset |
| Email looks broken in Outlook | Outlook ignores many CSS properties — test across clients with [Litmus](https://litmus.com) |
