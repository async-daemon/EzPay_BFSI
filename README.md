# EzPay — Secure and Efficient Digital Payment Solution

A BFSI digital payments application implemented from the `EzPay_BFSI_BRD_Batch1`
Business Requirements Document (36-page BRD with use cases, class diagram,
ER diagram, sequence diagram and architecture diagram).

- **Backend:** Python (FastAPI + SQLAlchemy) — built in Python per explicit
  instruction. The BRD's own architecture diagram specified Java 1.8/Spring
  MVC + Hibernate; this implementation keeps the same **layered design**
  (Presentation → Application/Business → Data Access) but realizes the
  Application/Data layers in FastAPI/SQLAlchemy instead.
- **Frontend:** Static responsive HTML/CSS/vanilla JS SPA (no build step).
  The BRD's architecture diagram named Angular; a build-tool-free frontend
  was used here to keep the app runnable with zero extra tooling. It can be
  swapped for an Angular app against the same REST API if desired.
- **Database:** SQLite by default (zero setup). The BRD calls for Oracle
  11g in production; since all access goes through SQLAlchemy, pointing
  `DATABASE_URL` at an Oracle DSN (via the `oracledb` driver) is a config
  change, not a code change.

## Requirements → implementation map

| BRD Use Case | Implementation |
|---|---|
| 1. User Management (register/login/MFA, profile, password recovery) | `backend/app/routers/auth.py`, `profile.py` |
| 2. Payment Processing (UPI, bank transfer) | `backend/app/routers/payments.py` |
| 3. Transaction Management (history, status tracking) | `backend/app/routers/transactions.py` |
| 4. Notification System (confirmations, reminders) | `backend/app/routers/reminders.py`, `notifications.py`, `services/notification_service.py` |
| 5. Security & Compliance (encryption, fraud detection) | `backend/app/services/encryption_service.py`, `services/fraud_detection.py` |
| 6. User Interface (responsive, intuitive navigation) | `frontend/` (mobile-first CSS, single-page nav) |

The data model mirrors the BRD's class/ER diagrams: `User`, `Authentication`
(session), `PasswordResetRequest`, `UPITransaction`/`BankTransfer` (unified
as `Transaction` with a `type` column), `PaymentReminder`, plus a
`BankAccount` table backing `User.addBankAccount()` and a `Notification`
table backing the push/email log — see `backend/app/models.py`.

## Project layout

```
EzPay_BFSI/
├── backend/
│   ├── app/
│   │   ├── main.py            FastAPI app, CORS, router wiring
│   │   ├── config.py          Settings from environment (.env)
│   │   ├── database.py        SQLAlchemy engine/session
│   │   ├── models.py          ORM models (User, Transaction, Reminder, ...)
│   │   ├── schemas.py         Pydantic request/response models
│   │   ├── security.py        Password hashing, JWT, OTP, reset tokens
│   │   ├── routers/           auth, profile, payments, transactions,
│   │   │                      reminders, notifications
│   │   └── services/          encryption_service, fraud_detection,
│   │                          notification_service
│   ├── tests/                 pytest smoke tests (one per use case)
│   ├── requirements.txt
│   ├── requirements-dev.txt
│   └── .env.example
└── frontend/
    ├── index.html             Single-page app shell (all views)
    ├── css/styles.css         Responsive, mobile-first styling
    └── js/
        ├── api.js             fetch wrapper + auth/session storage
        └── app.js             View routing + all UI logic
```

## Running it locally

### 1. Backend

```bash
cd backend
python -m venv .venv
.venv\Scripts\activate          # Windows
pip install -r requirements.txt
copy .env.example .env          # then edit if needed
python -m uvicorn app.main:app --reload --port 8008
```

The API is now at `http://127.0.0.1:8008` (interactive docs at `/docs`).
On first run it auto-creates the SQLite schema (`backend/ezpay.db`).

### 2. Frontend

The frontend is static — serve it with any static file server:

```bash
cd frontend
python -m http.server 5500
```

Open `http://127.0.0.1:5500`. If the backend runs on a different host/port,
set it before the page scripts load, e.g. add to `index.html`:
`<script>window.EZPAY_API_BASE = "http://your-host:8008";</script>`

### 3. Tests

```bash
cd backend
pip install -r requirements-dev.txt
pytest tests/ -v
```

10 smoke tests cover registration/OTP/login, profile updates, password
recovery, UPI/bank-transfer payments, the fraud-detection high-value check,
transaction history, reminders, notifications, and auth enforcement.

## Notable design choices / dev-mode conveniences

- **OTP & password reset are mocked.** There's no SMS/email gateway wired
  up, so `/auth/register` and `/auth/forgot-password` return the OTP /
  reset token directly in the API response (`dev_otp` / `dev_reset_token`)
  so the app is usable end-to-end without external services. Set
  `DEV_MODE = False` in `backend/app/routers/auth.py` and wire a real
  provider before shipping to production.
- **Fraud detection** (`services/fraud_detection.py`) is a simple,
  explainable rule set (high-value threshold + transaction velocity) since
  the BRD specifies no ML/vendor requirement — transactions above the
  threshold return HTTP 202 "requires verification" instead of silently
  succeeding.
- **Sensitive data at rest** (bank account numbers) is encrypted with
  Fernet symmetric encryption (`services/encryption_service.py`) and only
  ever returned to the client masked (`****1234`).
- **Security headers / NFRs:** JWT-based auth with server-side session
  revocation (`Authentication` table), bcrypt password hashing, and CORS
  restricted to configured origins per the BRD's Security Requirements
  table.

## Known gaps vs. a production deployment

- No real SMS/email/push provider, UPI switch, or core banking integration
  — all three are mocked/simulated, matching what a BRD-stage build can
  verify end-to-end without live BFSI infrastructure.
- No Alembic migrations yet; `Base.metadata.create_all()` is fine for
  SQLite/dev but a real Oracle rollout should use proper migrations.
- No rate limiting / WAF — add at the API gateway layer in production.
