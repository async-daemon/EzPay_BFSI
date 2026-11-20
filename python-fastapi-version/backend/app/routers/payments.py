"""Use Case 2: Payment Processing (User stories 2.1 UPI, 2.2 Bank Transfer)."""

from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app import models, schemas, security
from app.database import get_db
from app.services.encryption_service import decrypt_data
from app.services.fraud_detection import assess_transaction
from app.services.notification_service import notify_transaction_confirmation

router = APIRouter(prefix="/payments", tags=["Payments"])


@router.post("/upi", response_model=schemas.TransactionResponse, status_code=status.HTTP_201_CREATED)
def make_upi_payment(
    payload: schemas.UpiPaymentRequest,
    current_user: models.User = Depends(security.get_current_user),
    db: Session = Depends(get_db),
):
    if not payload.recipient_upi_id or "@" not in payload.recipient_upi_id:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid UPI ID")

    verdict = assess_transaction(db, current_user, payload.amount)
    txn = models.Transaction(
        user_id=current_user.user_id,
        type="UPI",
        amount=payload.amount,
        counterparty=payload.recipient_upi_id,
        note=payload.note,
    )

    if verdict.is_suspicious:
        txn.status = "BLOCKED" if not verdict.requires_additional_verification else "REQUIRES_VERIFICATION"
        txn.failure_reason = verdict.reason
        db.add(txn)
        db.commit()
        db.refresh(txn)
        notify_transaction_confirmation(db, current_user, txn)
        raise HTTPException(
            status_code=status.HTTP_202_ACCEPTED if txn.status == "REQUIRES_VERIFICATION" else status.HTTP_403_FORBIDDEN,
            detail={"reason": verdict.reason, "transaction_id": txn.transaction_id, "status": txn.status},
        )

    # Simulated UPI Service round-trip (BRD: "UPI Service processes the payment").
    txn.status = "COMPLETED"
    db.add(txn)
    db.commit()
    db.refresh(txn)

    notify_transaction_confirmation(db, current_user, txn)
    return txn


@router.post("/bank-transfer", response_model=schemas.TransactionResponse, status_code=status.HTTP_201_CREATED)
def make_bank_transfer(
    payload: schemas.BankTransferRequest,
    current_user: models.User = Depends(security.get_current_user),
    db: Session = Depends(get_db),
):
    source_account = (
        db.query(models.BankAccount)
        .filter(
            models.BankAccount.account_id == payload.from_account_id,
            models.BankAccount.user_id == current_user.user_id,
        )
        .first()
    )
    if not source_account:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Source bank account not found")

    if len(payload.recipient_ifsc_code) != 11:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid recipient IFSC code")

    verdict = assess_transaction(db, current_user, payload.amount)
    masked_source = decrypt_data(source_account.account_number)[-4:]
    txn = models.Transaction(
        user_id=current_user.user_id,
        type="BANK_TRANSFER",
        amount=payload.amount,
        counterparty=f"{payload.recipient_account_number[-4:].rjust(len(payload.recipient_account_number), '*')}"
        f" ({payload.recipient_ifsc_code})",
        note=f"{payload.purpose or ''} [from ****{masked_source}]".strip(),
    )

    if verdict.is_suspicious:
        txn.status = "BLOCKED" if not verdict.requires_additional_verification else "REQUIRES_VERIFICATION"
        txn.failure_reason = verdict.reason
        db.add(txn)
        db.commit()
        db.refresh(txn)
        notify_transaction_confirmation(db, current_user, txn)
        raise HTTPException(
            status_code=status.HTTP_202_ACCEPTED if txn.status == "REQUIRES_VERIFICATION" else status.HTTP_403_FORBIDDEN,
            detail={"reason": verdict.reason, "transaction_id": txn.transaction_id, "status": txn.status},
        )

    # Simulated Banking Service round-trip (BRD: "Banking Service processes the transfer").
    txn.status = "COMPLETED"
    db.add(txn)
    db.commit()
    db.refresh(txn)

    notify_transaction_confirmation(db, current_user, txn)
    return txn
