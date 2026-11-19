"""Use Case 1 / User Story 1.2: Profile Management."""

from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app import models, schemas, security
from app.database import get_db
from app.services.encryption_service import encrypt_data, mask_account_number

router = APIRouter(prefix="/profile", tags=["Profile"])


@router.get("", response_model=schemas.UserProfile)
def view_profile(current_user: models.User = Depends(security.get_current_user)):
    return current_user


@router.put("", response_model=schemas.UserProfile)
def update_profile(
    payload: schemas.UpdateProfileRequest,
    current_user: models.User = Depends(security.get_current_user),
    db: Session = Depends(get_db),
):
    if payload.username:
        current_user.username = payload.username
    if payload.upi_id:
        current_user.upi_id = payload.upi_id
    db.commit()
    db.refresh(current_user)
    return current_user


@router.post("/upi-id", response_model=schemas.UserProfile)
def add_upi_id(
    payload: schemas.AddUpiIdRequest,
    current_user: models.User = Depends(security.get_current_user),
    db: Session = Depends(get_db),
):
    existing = db.query(models.User).filter(models.User.upi_id == payload.upi_id).first()
    if existing and existing.user_id != current_user.user_id:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="UPI ID already in use")

    current_user.upi_id = payload.upi_id
    db.commit()
    db.refresh(current_user)
    return current_user


@router.post("/bank-accounts", response_model=schemas.BankAccountResponse, status_code=status.HTTP_201_CREATED)
def add_bank_account(
    payload: schemas.AddBankAccountRequest,
    current_user: models.User = Depends(security.get_current_user),
    db: Session = Depends(get_db),
):
    account = models.BankAccount(
        user_id=current_user.user_id,
        account_number=encrypt_data(payload.account_number),
        ifsc_code=payload.ifsc_code.upper(),
        account_holder_name=payload.account_holder_name,
    )
    db.add(account)
    db.commit()
    db.refresh(account)
    return schemas.BankAccountResponse(
        account_id=account.account_id,
        masked_account_number=mask_account_number(payload.account_number),
        ifsc_code=account.ifsc_code,
        account_holder_name=account.account_holder_name,
        is_verified=account.is_verified,
    )


@router.get("/bank-accounts", response_model=list[schemas.BankAccountResponse])
def list_bank_accounts(
    current_user: models.User = Depends(security.get_current_user),
    db: Session = Depends(get_db),
):
    accounts = db.query(models.BankAccount).filter(models.BankAccount.user_id == current_user.user_id).all()
    from app.services.encryption_service import decrypt_data

    return [
        schemas.BankAccountResponse(
            account_id=a.account_id,
            masked_account_number=mask_account_number(decrypt_data(a.account_number)),
            ifsc_code=a.ifsc_code,
            account_holder_name=a.account_holder_name,
            is_verified=a.is_verified,
        )
        for a in accounts
    ]
