"""Use Case 4: Notification System (transaction confirmations + reminders).

Push/email delivery is mocked with a log line and an in-app Notification
row; swap `_dispatch` for a real provider (FCM/APNs + SMTP) in production.
"""

from __future__ import annotations

import logging

from sqlalchemy.orm import Session

from app import models

logger = logging.getLogger("ezpay.notifications")


def _dispatch(channel: str, user: models.User, message: str) -> None:
    logger.info("[%s -> %s] %s", channel.upper(), user.email or user.mobile_number, message)


def notify(db: Session, user: models.User, category: str, message: str) -> models.Notification:
    for channel in ("push", "email"):
        _dispatch(channel, user, message)

    notification = models.Notification(
        user_id=user.user_id,
        channel="push+email",
        category=category,
        message=message,
    )
    db.add(notification)
    db.commit()
    db.refresh(notification)
    return notification


def notify_transaction_confirmation(db: Session, user: models.User, txn: models.Transaction) -> None:
    message = (
        f"Your {txn.type.replace('_', ' ').title()} of ₹{txn.amount:,.2f} to {txn.counterparty} "
        f"is {txn.status.lower()}. Txn ID: {txn.transaction_id}"
    )
    notify(db, user, "confirmation", message)


def notify_payment_reminder(db: Session, user: models.User, reminder: models.PaymentReminder) -> None:
    message = (
        f"Reminder: '{reminder.title}' of ₹{reminder.amount:,.2f} is due on "
        f"{reminder.due_date.strftime('%d-%b-%Y')}."
    )
    notify(db, user, "reminder", message)
