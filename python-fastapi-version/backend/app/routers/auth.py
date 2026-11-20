"""Use Case 1 / User Story 1.1 & 1.3: registration+login, password recovery."""

from __future__ import annotations

from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import or_
from sqlalchemy.orm import Session

from app import models, schemas, security
from app.database import get_db

router = APIRouter(prefix="/auth", tags=["Authentication"])

# Set to False once a real SMS/email provider is wired up; while True the
# OTP/reset token are echoed back in the API response so the app is usable
# without an external gateway.
DEV_MODE = True


@router.post("/register", response_model=schemas.RegisterResponse, status_code=status.HTTP_201_CREATED)
def register(payload: schemas.RegisterRequest, db: Session = Depends(get_db)):
    existing = (
        db.query(models.User)
        .filter(or_(models.User.email == payload.email, models.User.mobile_number == payload.mobile_number))
        .first()
    )
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Email or mobile number already registered")

    user = models.User(
        username=payload.username,
        email=payload.email,
        mobile_number=payload.mobile_number,
        password=security.hash_password(payload.password),
    )
    db.add(user)
    db.commit()

    otp = security.generate_otp(payload.mobile_number)
    return schemas.RegisterResponse(
        message="Account created. Enter the verification code sent to your mobile number to activate login.",
        mobile_number=payload.mobile_number,
        dev_otp=otp if DEV_MODE else None,
    )


@router.post("/verify-otp", response_model=schemas.TokenResponse)
def verify_otp(payload: schemas.VerifyOtpRequest, db: Session = Depends(get_db)):
    if not security.verify_otp(payload.mobile_number, payload.otp):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid or expired verification code")

    user = db.query(models.User).filter(models.User.mobile_number == payload.mobile_number).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")

    return _issue_session(db, user)


@router.post("/login", response_model=schemas.TokenResponse)
def login(payload: schemas.LoginRequest, db: Session = Depends(get_db)):
    user = (
        db.query(models.User)
        .filter(or_(models.User.email == payload.identifier, models.User.mobile_number == payload.identifier))
        .first()
    )
    if not user or not security.verify_password(payload.password, user.password):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")
    if not user.is_active:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Account is inactive")

    return _issue_session(db, user)


def _issue_session(db: Session, user: models.User) -> schemas.TokenResponse:
    session = models.Authentication(user_id=user.user_id)
    db.add(session)
    db.commit()
    db.refresh(session)

    token = security.create_access_token(user.user_id, session.session_token)
    return schemas.TokenResponse(access_token=token, user_id=user.user_id, username=user.username)


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
def logout(
    current_user: models.User = Depends(security.get_current_user),
    token: str = Depends(security.oauth2_scheme),
    db: Session = Depends(get_db),
):
    payload = security.decode_access_token(token)
    session = (
        db.query(models.Authentication)
        .filter(models.Authentication.session_token == payload.get("sid"))
        .first()
    )
    if session:
        session.is_valid = False
        db.commit()
    return None


@router.post("/forgot-password", response_model=schemas.ForgotPasswordResponse)
def forgot_password(payload: schemas.ForgotPasswordRequest, db: Session = Depends(get_db)):
    user = (
        db.query(models.User)
        .filter(or_(models.User.email == payload.identifier, models.User.mobile_number == payload.identifier))
        .first()
    )
    # Always return 200 to avoid leaking which identifiers are registered.
    generic_message = "If that account exists, a password reset link has been sent."
    if not user:
        return schemas.ForgotPasswordResponse(message=generic_message)

    reset_token = security.generate_reset_token()
    reset_request = models.PasswordResetRequest(user_id=user.user_id, reset_token=reset_token)
    db.add(reset_request)
    db.commit()

    return schemas.ForgotPasswordResponse(message=generic_message, dev_reset_token=reset_token if DEV_MODE else None)


@router.post("/reset-password", status_code=status.HTTP_204_NO_CONTENT)
def reset_password(payload: schemas.ResetPasswordRequest, db: Session = Depends(get_db)):
    reset_request = (
        db.query(models.PasswordResetRequest)
        .filter(
            models.PasswordResetRequest.reset_token == payload.reset_token,
            models.PasswordResetRequest.used.is_(False),
        )
        .first()
    )
    if not reset_request or reset_request.expiration_date < datetime.utcnow():
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Reset link is invalid or has expired")

    user = db.query(models.User).filter(models.User.user_id == reset_request.user_id).first()
    user.password = security.hash_password(payload.new_password)
    reset_request.used = True
    db.commit()
    return None
