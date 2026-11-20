"""Use Case 4 / User Story 4.2: Payment Reminders."""

from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app import models, schemas, security
from app.database import get_db
from app.services.notification_service import notify_payment_reminder

router = APIRouter(prefix="/reminders", tags=["Reminders"])


@router.post("", response_model=schemas.ReminderResponse, status_code=status.HTTP_201_CREATED)
def create_reminder(
    payload: schemas.CreateReminderRequest,
    current_user: models.User = Depends(security.get_current_user),
    db: Session = Depends(get_db),
):
    reminder = models.PaymentReminder(
        user_id=current_user.user_id,
        title=payload.title,
        amount=payload.amount,
        due_date=payload.due_date,
    )
    db.add(reminder)
    db.commit()
    db.refresh(reminder)
    return reminder


@router.get("", response_model=list[schemas.ReminderResponse])
def list_reminders(
    current_user: models.User = Depends(security.get_current_user),
    db: Session = Depends(get_db),
):
    from datetime import datetime, timedelta

    reminders = (
        db.query(models.PaymentReminder)
        .filter(models.PaymentReminder.user_id == current_user.user_id)
        .order_by(models.PaymentReminder.due_date.asc())
        .all()
    )

    now = datetime.utcnow()
    for r in reminders:
        if r.status == "PAID":
            continue
        if r.due_date < now:
            r.status = "OVERDUE"
        elif r.due_date <= now + timedelta(days=3):
            r.status = "DUE_SOON"
        else:
            r.status = "UPCOMING"
    db.commit()
    return reminders


@router.post("/{reminder_id}/send", status_code=status.HTTP_204_NO_CONTENT)
def send_reminder_now(
    reminder_id: str,
    current_user: models.User = Depends(security.get_current_user),
    db: Session = Depends(get_db),
):
    reminder = (
        db.query(models.PaymentReminder)
        .filter(
            models.PaymentReminder.reminder_id == reminder_id,
            models.PaymentReminder.user_id == current_user.user_id,
        )
        .first()
    )
    if not reminder:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Reminder not found")

    notify_payment_reminder(db, current_user, reminder)
    return None


@router.post("/{reminder_id}/mark-paid", response_model=schemas.ReminderResponse)
def mark_reminder_paid(
    reminder_id: str,
    current_user: models.User = Depends(security.get_current_user),
    db: Session = Depends(get_db),
):
    reminder = (
        db.query(models.PaymentReminder)
        .filter(
            models.PaymentReminder.reminder_id == reminder_id,
            models.PaymentReminder.user_id == current_user.user_id,
        )
        .first()
    )
    if not reminder:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Reminder not found")

    reminder.status = "PAID"
    db.commit()
    db.refresh(reminder)
    return reminder
