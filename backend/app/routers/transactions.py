"""Use Case 3: Transaction Management (3.1 history, 3.2 status tracking)."""

from __future__ import annotations

from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app import models, schemas, security
from app.database import get_db

router = APIRouter(prefix="/transactions", tags=["Transactions"])


@router.get("", response_model=list[schemas.TransactionResponse])
def view_transaction_history(
    type: str | None = Query(default=None, description="UPI or BANK_TRANSFER"),
    status_filter: str | None = Query(default=None, alias="status"),
    date_from: datetime | None = None,
    date_to: datetime | None = None,
    current_user: models.User = Depends(security.get_current_user),
    db: Session = Depends(get_db),
):
    q = db.query(models.Transaction).filter(models.Transaction.user_id == current_user.user_id)
    if type:
        q = q.filter(models.Transaction.type == type.upper())
    if status_filter:
        q = q.filter(models.Transaction.status == status_filter.upper())
    if date_from:
        q = q.filter(models.Transaction.date >= date_from)
    if date_to:
        q = q.filter(models.Transaction.date <= date_to)

    transactions = q.order_by(models.Transaction.date.desc()).all()
    if not transactions:
        # BRD alternative course: "If there are no transactions..." — an
        # empty list (200 OK) lets the UI show that message itself.
        return []
    return transactions


@router.get("/{transaction_id}", response_model=schemas.TransactionResponse)
def track_transaction_status(
    transaction_id: str,
    current_user: models.User = Depends(security.get_current_user),
    db: Session = Depends(get_db),
):
    txn = (
        db.query(models.Transaction)
        .filter(
            models.Transaction.transaction_id == transaction_id,
            models.Transaction.user_id == current_user.user_id,
        )
        .first()
    )
    if not txn:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Transaction not found")
    return txn
