"""EzPay — Secure and Efficient Digital Payment Solution.

FastAPI backend implementing the six use cases from the EzPay BFSI BRD:
User Management, Payment Processing, Transaction Management, Notification
System, Security & Compliance, and the supporting data model from the
BRD's class/ER diagrams. See ../../README.md for the requirements mapping.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.database import Base, engine
from app.routers import auth, notifications, payments, profile, reminders, transactions

settings = get_settings()

# Dev convenience: auto-create tables on startup. For Oracle/production,
# prefer a proper migration tool (e.g. Alembic) instead.
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="EzPay API",
    description="Secure and efficient digital payment solution — BFSI BRD implementation.",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(profile.router)
app.include_router(payments.router)
app.include_router(transactions.router)
app.include_router(reminders.router)
app.include_router(notifications.router)


@app.get("/", tags=["Health"])
def health_check():
    return {"status": "ok", "service": "EzPay API"}
