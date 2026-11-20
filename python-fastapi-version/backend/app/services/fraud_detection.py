"""User Story 5.2: Fraud Prevention.

Simple, explainable rule-based checks (BRD gives no ML/vendor requirement).
Returns a verdict so callers can block or flag-and-continue the transaction.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta

from sqlalchemy.orm import Session

from app import models

HIGH_VALUE_THRESHOLD = 200_000.0
VELOCITY_WINDOW_MINUTES = 10
VELOCITY_MAX_TRANSACTIONS = 5


@dataclass
class FraudVerdict:
    is_suspicious: bool
    requires_additional_verification: bool
    reason: str | None = None


def assess_transaction(db: Session, user: models.User, amount: float) -> FraudVerdict:
    if amount <= 0:
        return FraudVerdict(True, False, "Transaction amount must be positive.")

    if amount >= HIGH_VALUE_THRESHOLD:
        return FraudVerdict(
            True, True, f"Amount exceeds the high-value threshold of ₹{HIGH_VALUE_THRESHOLD:,.0f}."
        )

    window_start = datetime.utcnow() - timedelta(minutes=VELOCITY_WINDOW_MINUTES)
    recent_count = (
        db.query(models.Transaction)
        .filter(models.Transaction.user_id == user.user_id, models.Transaction.date >= window_start)
        .count()
    )
    if recent_count >= VELOCITY_MAX_TRANSACTIONS:
        return FraudVerdict(
            True,
            True,
            f"More than {VELOCITY_MAX_TRANSACTIONS} transactions in the last "
            f"{VELOCITY_WINDOW_MINUTES} minutes.",
        )

    return FraudVerdict(False, False, None)
