"""Auth primitives backing the `Authentication` entity and User Story 1.1
(secure authentication, multi-factor via OTP) and 1.3 (password recovery).
"""

from __future__ import annotations

import random
import secrets
import time
from datetime import datetime, timedelta

import bcrypt
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from jose import JWTError, jwt
from sqlalchemy.orm import Session

from app.config import get_settings
from app.database import get_db
from app import models

settings = get_settings()
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="auth/login")

# bcrypt's algorithm only uses the first 72 bytes of input; longer passwords
# are truncated (matches the RegisterRequest max_length=128 validator).
_BCRYPT_MAX_BYTES = 72


def hash_password(password: str) -> str:
    truncated = password.encode("utf-8")[:_BCRYPT_MAX_BYTES]
    return bcrypt.hashpw(truncated, bcrypt.gensalt()).decode("utf-8")


def verify_password(plain_password: str, hashed_password: str) -> bool:
    truncated = plain_password.encode("utf-8")[:_BCRYPT_MAX_BYTES]
    return bcrypt.checkpw(truncated, hashed_password.encode("utf-8"))


def create_access_token(user_id: str, session_token: str) -> str:
    expire = datetime.utcnow() + timedelta(minutes=settings.access_token_expire_minutes)
    payload = {"sub": user_id, "sid": session_token, "exp": expire}
    return jwt.encode(payload, settings.jwt_secret_key, algorithm=settings.jwt_algorithm)


def decode_access_token(token: str) -> dict:
    try:
        return jwt.decode(token, settings.jwt_secret_key, algorithms=[settings.jwt_algorithm])
    except JWTError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate credentials",
        ) from exc


def get_current_user(
    token: str = Depends(oauth2_scheme), db: Session = Depends(get_db)
) -> models.User:
    payload = decode_access_token(token)
    user_id = payload.get("sub")
    session_token = payload.get("sid")

    session = (
        db.query(models.Authentication)
        .filter(
            models.Authentication.session_token == session_token,
            models.Authentication.is_valid.is_(True),
        )
        .first()
    )
    if session is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Session expired, please log in again")

    user = db.query(models.User).filter(models.User.user_id == user_id).first()
    if user is None or not user.is_active:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")
    return user


# --- OTP (mobile verification for registration, User Story 1.1 step 5-6) ---
# Dev-mode in-memory store: {mobile_number: (code, expires_at)}.
# A production deployment would back this with Redis/the DB and a real SMS
# gateway instead of returning the code in the API response.
_otp_store: dict[str, tuple[str, float]] = {}
OTP_TTL_SECONDS = 300


def generate_otp(mobile_number: str) -> str:
    code = f"{random.randint(0, 999999):06d}"
    _otp_store[mobile_number] = (code, time.time() + OTP_TTL_SECONDS)
    return code


def verify_otp(mobile_number: str, code: str) -> bool:
    record = _otp_store.get(mobile_number)
    if not record:
        return False
    stored_code, expires_at = record
    if time.time() > expires_at:
        del _otp_store[mobile_number]
        return False
    if secrets.compare_digest(stored_code, code):
        del _otp_store[mobile_number]
        return True
    return False


def generate_reset_token() -> str:
    return secrets.token_urlsafe(32)
