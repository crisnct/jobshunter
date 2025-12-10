# JobsHunter

Java 25 + Spring Boot 4.0.0 service that keeps a user profile (prompt, CV, phone, time interval), searches for new jobs with ChatGPT, and can push results to WhatsApp via Twilio.
![Jobshunter logo](application/src/main/resources/static/images/JobsHunterLogo.svg)

## What it does
- JWT auth flow: `/api/auth/register`, `/api/auth/login`, `/api/auth/verify`.
- Per-user prompt, phone number, time interval, and CV file uploaded to OpenAI file storage for context.
- Scheduled job hunting (`jobshunter.scheduler.frequency`, `jobshunter.iterationPerUser`, `jobshunter.iterationDelay`) with URL validation, duplicate filtering, and HTML checks for `jobshunter.expiredKeywords`.
- Job search via OpenAI Responses API (model `gpt-5.1`, `toolsType=web_search`).
- WhatsApp notifications via Twilio Content API; falls back to plain-text WhatsApp when Twilio template is missing for the locale (error 63027) or content variables cannot be serialized.
- MySQL persistence managed by Liquibase.

## Contributors
| Name                                           | Role                                                                | Commits | Contribution                  | Sponsorship |
|------------------------------------------------|---------------------------------------------------------------------|---------|-------------------------------|-------------|
| [Cristian Țone](https://github.com/crisnct)    | • Product Owner<br/>• Main developer of the project on backend side | 64      | • Implemented core of the app | 52eur       |
| [Andrei Lazăr](https://github.com/AyanoCode13) | • Backend Developer                                                 | 1       | -                             | -           |
| TBD                                            |  TBD                                                                | 0       | -                             | -           |

Display total commits per user:
```
git shortlog -s -n
```

## License
- Licensed under Apache License 2.0 (see `LICENSE`).
- The project is completely free and can be used by anyone.

## Requirements
- Java 25
- Spring Boot 4.0.0
- Maven 3.9+.
- MySQL 8.x reachable with the configured credentials.
- Optional: Twilio account with WhatsApp-enabled sender.
- Optional: OpenAI API key with access to Responses API + file uploads.

## Configuration (env vars / `application.yml`)
Server listens on port `8081`.

```yaml
jobshunter:
  expiredKeywords: expired,no longer exists,This job was available,is no longer active,job is no longer available
  iterationPerUser: 5
  iterationDelay: 30000
  scheduler:
    frequency: 3600000   # ms between scheduled runs
  whatsapp:
    account-sid: ${TWILIO_ACCOUNT_SID:}
    auth-token: ${TWILIO_AUTH_TOKEN:}
    from-number: ${TWILIO_WHATSAPP_FROM:}
    # to-number: ${JOBSHUNTER_WHATSAPP_TO_NUMBER:}   # optional fallback if user phone is missing
    jobsNotifyMessageSID: ${TWILIO_JOBS_NOTIFY_MESSAGE_SID:}
  chatGpt:
    apiKey: ${CHATGPT_API_KEY:}
    model: gpt-5.1
    temperature: 0
    maxTokens: 2000
    toolsType: web_search
spring:
  datasource:
    url: ${MYSQL_URL:jdbc:mysql://localhost:3306/jobshunter?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true}
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:root}
  liquibase:
    enabled: ${LIQUIBASE_ENABLED:true}
security:
  jwt:
    secret: ${JWT_SECRET:change-me-dev-secret-please-keep-long}
    expiration-ms: ${JWT_EXPIRATION_MS:86400000}
```

Minimum env for local dev:
```bash
export MYSQL_URL="jdbc:mysql://localhost:3306/jobshunter?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true"
export MYSQL_USER="root"
export MYSQL_PASSWORD="root"
export JWT_SECRET="change-me-dev-secret-please-keep-long"
```

Twilio (optional):
```bash
export TWILIO_ACCOUNT_SID="ACxxxx"
export TWILIO_AUTH_TOKEN="secret"
export TWILIO_WHATSAPP_FROM="whatsapp:+000000000000"
```

OpenAI (optional, required for job search + CV upload):
```bash
export CHATGPT_API_KEY="sk-..."
```

## Twilio WhatsApp template (Content SID) setup
The notifier sends WhatsApp messages using Twilio Content API with variables `jobs_links1` and `timestamp`, then falls back to plain text if Twilio returns 63027 (“Template does not exist for a language and locale”).

1. In Twilio Console, create a Content Template for WhatsApp (https://console.twilio.com/us1/develop/content/create).
2. Add template variables named `jobs_links1` and `timestamp` in the WhatsApp body.
3. Approve/publish a variation for the recipient locale. If you see 63027, add a variation for that language/locale.
4. Copy the Content SID (starts with `HX`) into `TWILIO_JOBS_NOTIFY_MESSAGE_SID`.
5. Ensure `TWILIO_WHATSAPP_FROM` is WhatsApp-enabled and recipients are approved/sandboxed as required by your account.
6. If the template is unavailable, the code automatically sends a plain-text fallback built from `src/main/resources/messageTemplates/jobsNotify.txt` (WhatsApp rules still apply: 24h session, approved recipient).

## How to run locally
1. Start MySQL (example): `docker run --name jobshunter-mysql -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 -d mysql:8`
2. Export env vars (see above).
3. Build & run:
   ```bash
   mvn clean package
   java -jar target/jobshunter-1.0.0.jar
   # or, for dev:
   mvn spring-boot:run
   ```
   App runs at `http://localhost:8081`.

## API quick start
1) Register and log in to obtain JWT:
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","email":"demo@example.com","password":"secret","phoneNumber":"+40123456789"}'
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"secret"}'
# Use Authorization: Bearer <token>
```
2) Upload CV to OpenAI file storage:
```bash
curl -X POST http://localhost:8081/api/cv/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@cv.pdf"
```
3) Update prompt / time interval:
```bash
curl -X POST "http://localhost:8081/api/user/prompt" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Remote Java backend roles"}'
curl -X PATCH "http://localhost:8081/api/user/time-interval?minutes=60" \
  -H "Authorization: Bearer <token>"
```
4) Trigger a search (optional WhatsApp push):
```bash
curl -X POST "http://localhost:8081/api/user/search?notifyOnWhatsupp=true" \
  -H "Authorization: Bearer <token>"
```
`notifyOnWhatsupp=true` sends WhatsApp if credentials and numbers are valid; otherwise results are returned in the response.

## Scheduling
- Background search runs every `jobshunter.scheduler.frequency` ms (default 1h) per user.
- `jobshunter.iterationPerUser` controls repeated calls per user; `jobshunter.iterationDelay` is the pause between iterations.
- Per-user cooldown uses the stored `timeInterval` (minutes); scheduled runs skip users whose cooldown has not expired.

## Testing
```bash
mvn verify
```

Liquibase migrations run automatically on startup when `LIQUIBASE_ENABLED=true` (default).
