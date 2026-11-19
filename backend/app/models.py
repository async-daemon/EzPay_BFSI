import uuid
from datetime import datetime, timedelta

from sqlalchemy import Boolean, DateTime, Float, ForeignKey, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


def _uuid() -> str:
    return str(uuid.uuid4())


class User(Base):
    """Maps to the BRD class/ER diagram's `User` entity."""

    __tablename__ = "users"

    user_id: Mapped[str] = mapped_column("userId", String(36), primary_key=True, default=_uuid)
    username: Mapped[str] = mapped_column(String(100), nullable=False)
    password: Mapped[str] = mapped_column(String(255), nullable=False)
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True, nullable=False)
    mobile_number: Mapped[str] = mapped_column(String(15), unique=True, index=True, nullable=False)
    registration_date: Mapped[datetime] = mapped_column("registrationDate", DateTime, default=datetime.utcnow)
    upi_id: Mapped[str | None] = mapped_column(String(100), unique=True, nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)

    bank_accounts: Mapped[list["BankAccount"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    sessions: Mapped[list["Authentication"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    reset_requests: Mapped[list["PasswordResetRequest"]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    transactions: Mapped[list["Transaction"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    reminders: Mapped[list["PaymentReminder"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    notifications: Mapped[list["Notification"]] = relationship(back_populates="user", cascade="all, delete-orphan")


class BankAccount(Base):
    """Backs User.addBankAccount() / viewBankTransfers()."""

    __tablename__ = "bank_accounts"

    account_id: Mapped[str] = mapped_column(String(36), primary_key=True, default=_uuid)
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.userId"), nullable=False)
    account_number: Mapped[str] = mapped_column(String(255), nullable=False)
    ifsc_code: Mapped[str] = mapped_column(String(20), nullable=False)
    account_holder_name: Mapped[str] = mapped_column(String(100), nullable=False)
    is_verified: Mapped[bool] = mapped_column(Boolean, default=True)

    user: Mapped[User] = relationship(back_populates="bank_accounts")


class Authentication(Base):
    """Maps to the ER diagram's `Authentication` entity (an active login session)."""

    __tablename__ = "authentication"

    session_token: Mapped[str] = mapped_column("sessionToken", String(36), primary_key=True, default=_uuid)
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.userId"), nullable=False)
    login_time: Mapped[datetime] = mapped_column("loginTime", DateTime, default=datetime.utcnow)
    is_valid: Mapped[bool] = mapped_column(Boolean, default=True)

    user: Mapped[User] = relationship(back_populates="sessions")


class PasswordResetRequest(Base):
    """Maps to the ER diagram's `PasswordResetRequest` entity (User story 1.3)."""

    __tablename__ = "password_reset_requests"

    request_id: Mapped[str] = mapped_column("requestId", String(36), primary_key=True, default=_uuid)
    user_id: Mapped[str] = mapped_column("userId", String(36), ForeignKey("users.userId"), nullable=False)
    reset_token: Mapped[str] = mapped_column("resetToken", String(64), nullable=False)
    expiration_date: Mapped[datetime] = mapped_column(
        "expirationDate", DateTime, default=lambda: datetime.utcnow() + timedelta(minutes=30)
    )
    used: Mapped[bool] = mapped_column(Boolean, default=False)

    user: Mapped[User] = relationship(back_populates="reset_requests")


class Transaction(Base):
    """Unified ledger entry backing `TransactionHistory` (User story 3.1/3.2).

    Created alongside every UPITransaction / BankTransfer so the user has a
    single history/status view, matching the `Transaction` entity on the ER
    diagram (transactionId, type, amount, date, status).
    """

    __tablename__ = "transactions"

    transaction_id: Mapped[str] = mapped_column("transactionId", String(36), primary_key=True, default=_uuid)
    user_id: Mapped[str] = mapped_column("userId", String(36), ForeignKey("users.userId"), nullable=False)
    type: Mapped[str] = mapped_column(String(20), nullable=False)  # "UPI" | "BANK_TRANSFER"
    amount: Mapped[float] = mapped_column(Float, nullable=False)
    counterparty: Mapped[str] = mapped_column(String(255), nullable=False)
    note: Mapped[str | None] = mapped_column(String(255), nullable=True)
    date: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    status: Mapped[str] = mapped_column(String(20), default="INITIATED")  # INITIATED|PROCESSING|COMPLETED|FAILED|BLOCKED
    failure_reason: Mapped[str | None] = mapped_column(String(255), nullable=True)

    user: Mapped[User] = relationship(back_populates="transactions")


class PaymentReminder(Base):
    """Maps to the ER diagram's `PaymentReminder` entity (User story 4.2)."""

    __tablename__ = "payment_reminders"

    reminder_id: Mapped[str] = mapped_column("reminderId", String(36), primary_key=True, default=_uuid)
    user_id: Mapped[str] = mapped_column("userId", String(36), ForeignKey("users.userId"), nullable=False)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    amount: Mapped[float] = mapped_column(Float, nullable=False)
    due_date: Mapped[datetime] = mapped_column("dueDate", DateTime, nullable=False)
    status: Mapped[str] = mapped_column(String(20), default="UPCOMING")  # UPCOMING|DUE_SOON|OVERDUE|PAID

    user: Mapped[User] = relationship(back_populates="reminders")


class Notification(Base):
    """In-app notification log backing Use Case 4 (push/email are mocked here)."""

    __tablename__ = "notifications"

    notification_id: Mapped[str] = mapped_column(String(36), primary_key=True, default=_uuid)
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.userId"), nullable=False)
    channel: Mapped[str] = mapped_column(String(20), default="push")  # push|email
    category: Mapped[str] = mapped_column(String(30), nullable=False)  # confirmation|reminder|security
    message: Mapped[str] = mapped_column(String(500), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    is_read: Mapped[bool] = mapped_column(Boolean, default=False)

    user: Mapped[User] = relationship(back_populates="notifications")
