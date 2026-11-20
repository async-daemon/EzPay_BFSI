# EzPay — Secure and Efficient Digital Payment Solution

A BFSI digital payments application implemented from the `EzPay_BFSI_BRD_Batch1`
Business Requirements Document (36-page BRD with use cases, class diagram,
ER diagram, sequence diagram and architecture diagram).

This repository now contains **two implementations**:

| | Stack | Status |
|---|---|---|
| **`backend-java/` + `frontend-angular/`** | Java 17 (Spring Boot/Spring MVC) + Hibernate + Angular + Oracle-ready | **Primary** — matches the BRD's own architecture diagram |
| **`python-fastapi-version/`** | Python (FastAPI) + vanilla JS + SQLite | Earlier build (kept for reference, not deleted) |

## Framework named in the BRD vs. this build

The BRD's architecture diagram specifies:

| Layer | BRD spec | This build | Note |
|---|---|---|---|
| Backend | **Java 1.8**, Spring MVC, Hibernate (ORM) | **Java 17** (LTS), Spring Boot/Spring MVC, Hibernate via Spring Data JPA | Java 8 is end-of-life and not installed on this machine; Java 17 is the closest supported LTS and runs the same Spring MVC/Hibernate stack. Spring Boot is used only as the standard way to package Spring MVC + an embedded servlet container — the web layer is still plain Spring MVC (`@RestController`, `@RequestMapping`, Hibernate-backed JPA repositories), not a different framework. |
| Frontend | **Angular** | **Angular 21** (standalone components, Router, Reactive/Template forms, HttpClient) | Matches the BRD directly. |
| Database | **Oracle 11g** | **H2** (file-based, zero-setup) by default; **Oracle** via a Spring profile | Oracle 11g itself is end-of-life and no longer offered as a free download without an Oracle.com account. The `oracle` Spring profile points Hibernate at a real Oracle instance (11g R2+, including the free "Oracle Database Free" / `gvenzl/oracle-free` Docker image) with no code change — see "Switching to Oracle" below. **Verified live** against a real `gvenzl/oracle-free` container: register → OTP → login → profile → encrypted bank account → UPI payment → transaction history → notifications → reminders → forgot-password all confirmed working. This run caught one real Oracle-only bug, now fixed — see below. |

The BRD's layered design (Presentation → Business → Data Access) is preserved
throughout: Controllers → Services → Repositories/Entities.

## System requirements & software to install

| Requirement | Used for | Already on this machine? |
|---|---|---|
| **JDK 17+** | Backend | ✅ Temurin 17.0.21 |
| **Maven 3.9+** | Backend build | ✅ 3.9.9 |
| **Node.js 18+** | Frontend | ✅ 22.17.1 |
| **Angular CLI** | Frontend | ✅ 21.2.13 (`npm i -g @angular/cli` if missing elsewhere) |
| **Docker Desktop** (optional) | Running a real Oracle database locally | Installed; start it manually if you want to test against Oracle |
| A modern browser | Using the app | — |

Nothing else needs installing — Maven and npm pull all library dependencies
(Spring Boot, Hibernate, JJWT, Angular packages, etc.) automatically on first
build.

## Project layout

```
EzPay_BFSI/
├── backend-java/                  Java 17 / Spring MVC / Hibernate backend
│   ├── pom.xml
│   └── src/main/java/com/ezpay/backend/
│       ├── entity/                JPA entities (User, Transaction, ...)
│       ├── repository/            Spring Data JPA repositories
│       ├── dto/                   Request/response records
│       ├── security/              JWT, bcrypt (via spring-security-crypto), OTP store
│       ├── service/                Business logic per BRD use case
│       ├── controller/             @RestController endpoints
│       ├── web/                   @CurrentUser (Depends-on-token equivalent)
│       └── config/                 CORS + MVC wiring
│   └── src/test/java/...          13 MockMvc integration tests (all 6 use cases)
├── frontend-angular/              Angular 21 SPA
│   └── src/app/
│       ├── core/                  ApiService, AuthService, interceptor, guards
│       ├── shared/                Shared auth-shell layout component
│       ├── pages/                 login, register, otp, forgot/reset-password,
│       │                          dashboard, payments, transactions, reminders,
│       │                          notifications, profile
│       └── shell/                 Authenticated app shell (nav + section router)
├── python-fastapi-version/        Earlier Python/FastAPI + vanilla JS build (kept as-is)
└── .gitignore
```

## Running it locally

### 1. Backend (`backend-java/`)

```bash
cd backend-java
mvn spring-boot:run
```

The API is now at `http://127.0.0.1:8081`. On first run it auto-creates the
H2 schema (`backend-java/ezpaydb.mv.db`). An H2 web console is available at
`http://127.0.0.1:8081/h2-console` (JDBC URL `jdbc:h2:file:./ezpaydb`, user
`sa`, empty password) if you want to inspect the data directly.

### 2. Frontend (`frontend-angular/`)

```bash
cd frontend-angular
npm install   # first time only
ng serve
```

Open `http://localhost:4200`. If the backend runs on a different host/port,
set it before the app loads, e.g. in `src/index.html`:
`<script>window.EZPAY_API_BASE = "http://your-host:8081";</script>`

### 3. Tests

```bash
cd backend-java
mvn test
```

13 integration tests (MockMvc, in-memory H2) cover registration/OTP/login,
profile + UPI/bank account management, UPI and bank-transfer payments, the
fraud-detection high-value + REQUIRES_VERIFICATION path, transaction history
and status tracking, reminders (create/send/mark-paid), notifications, the
forgot/reset-password flow, and auth enforcement (missing/invalid token).

The full flow was also verified live end-to-end in a real browser against
both servers running together (register → OTP → dashboard → UPI payment →
bank transfer → reminders → notifications → profile → logout/login).

### Switching to Oracle

```bash
# 1. Run a free Oracle database locally (no Oracle.com account needed).
#    Note: this machine already has another Oracle listener on port 1521
#    (a pre-existing install, untouched) — map the container to 1522 instead
#    if you hit "ports are not available":
docker run -d --name ezpay-oracle -p 1522:1521 \
  -e ORACLE_PASSWORD=ezpay_dev_pw gvenzl/oracle-free:slim

# 2. Create the application user/schema (the image only creates SYSTEM/SYS):
docker exec ezpay-oracle bash -c "echo \"
CREATE USER ezpay IDENTIFIED BY ezpay_dev_pw;
GRANT CONNECT, RESOURCE, CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW, UNLIMITED TABLESPACE TO ezpay;
EXIT;\" | sqlplus -s system/ezpay_dev_pw@//localhost:1521/FREEPDB1"

# 3. Start the backend with the oracle profile (adjust the port if you used 1521):
cd backend-java
EZPAY_ORACLE_URL="jdbc:oracle:thin:@//localhost:1522/FREEPDB1" \
  mvn spring-boot:run -Dspring-boot.run.profiles=oracle
```

See `backend-java/src/main/resources/application-oracle.properties` for the
connection settings (all overridable via `EZPAY_ORACLE_URL` /
`EZPAY_ORACLE_USER` / `EZPAY_ORACLE_PASSWORD` env vars) — no Java or Hibernate
code changes are needed; Hibernate generates Oracle-dialect SQL automatically.

## Application flow (per BRD use cases)

1. **Register** → `POST /auth/register` creates the account and generates a
   mobile OTP (dev mode returns it as `devOtp` since no SMS gateway is wired
   up — shown directly in the Angular OTP screen with a "use it" button).
2. **Verify OTP** → `POST /auth/verify-otp` activates the account and issues
   a JWT + server-side session row.
3. **Login / Logout** → `POST /auth/login` / `/auth/logout` (session
   revocation via the `authentication` table).
4. **Profile** → view/update profile, add a UPI ID, add/list bank accounts
   (account numbers encrypted at rest with AES-256-GCM, shown masked, e.g.
   `****1234`).
5. **Payments** → UPI or bank transfer — each payment runs through fraud
   detection (high-value threshold ≥ ₹200,000, or > 5 transactions in 10
   minutes) before being recorded and triggering a confirmation
   notification. A flagged payment returns HTTP 202 with a `REQUIRES_VERIFICATION`
   status (the Angular app surfaces this as an error toast, not a false
   "payment succeeded" message).
6. **Transactions** → history with type/status filters and per-transaction
   status tracking.
7. **Reminders** → create, send now, mark paid — each action logs a
   notification.
8. **Notifications** → in-app list + mark-as-read.
9. **Forgot/reset password** → mirrors the OTP flow with a dev-mode reset
   token shown directly in the Angular UI.

## Notable design choices / dev-mode conveniences

- **OTP & password reset are mocked**, same as the original build — no real
  SMS/email gateway, so the dev token is returned directly in the API
  response and surfaced in the UI. Flip `AuthService.DEV_MODE` to `false`
  before shipping to production.
- **Fraud detection** (`FraudDetectionService`) is a simple, explainable rule
  set (high-value threshold + transaction velocity), matching the BRD's lack
  of an ML/vendor requirement.
- **Sensitive data at rest** (bank account numbers) is encrypted with
  AES-256-GCM (`EncryptionService`) — the Java equivalent of the Python
  build's Fernet — and only ever returned to the client masked.
- **Security:** JWT bearer tokens + a server-side `AuthenticationSession`
  table (so logout actually revokes a token, not just a client-side delete),
  bcrypt password hashing via `spring-security-crypto`'s
  `BCryptPasswordEncoder`, and CORS restricted to configured origins.
- **A blocked/flagged transaction is committed before the error response is
  returned** (`@Transactional(noRollbackFor = ApiException.class)`) so a
  REQUIRES_VERIFICATION/BLOCKED transaction and its notification are not
  silently rolled back by Spring's default exception-triggers-rollback
  behavior — matching the original "commit, then raise" Python logic.
- **The Angular API layer treats HTTP 202 as an error condition** for
  payment endpoints (not Angular's default "2xx = success"), since the
  backend intentionally returns 202 for REQUIRES_VERIFICATION — this was
  caught and fixed during live browser testing (see git history).
- **`Transaction`'s date column is mapped to `txn_date`, not `date`** —
  `DATE` is a reserved word in Oracle, so Hibernate's auto-schema-creation
  failed with `ORA-03050` against a real Oracle instance even though it
  worked fine on H2/SQLite (which are more lenient about reserved words).
  Caught during live Oracle verification and fixed with an explicit
  `@Column(name = "txn_date")` — no Java-side or API-facing change.

## Known gaps vs. a production deployment

- No real SMS/email/push provider, UPI switch, or core banking integration —
  all are mocked/simulated.
- No Flyway/Liquibase migrations yet; `spring.jpa.hibernate.ddl-auto=update`
  is fine for dev but a real Oracle rollout should use proper migrations.
- No rate limiting / WAF — add at the API gateway layer in production.
- Angular build has no unit test specs for the new pages yet (only the
  default CLI-generated `app.spec.ts`); coverage today is the Java backend's
  13 integration tests plus full manual browser verification.

## Legacy Python/FastAPI build

The original Python/FastAPI + vanilla JS implementation is preserved as-is at
[`python-fastapi-version/`](python-fastapi-version/) with its own README —
see that folder for its setup instructions. It is not maintained further now
that the Java/Angular build matches the BRD's intended stack, but nothing in
it was deleted.
