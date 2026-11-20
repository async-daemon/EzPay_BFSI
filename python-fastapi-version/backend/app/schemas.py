from datetime import datetime

from pydantic import BaseModel, EmailStr, Field, field_validator


# ---------- Use Case 1: User Management ----------


class RegisterRequest(BaseModel):
    username: str = Field(min_length=2, max_length=100)
    email: EmailStr
    mobile_number: str = Field(min_length=10, max_length=15)
    password: str = Field(min_length=8, max_length=128)

    @field_validator("mobile_number")
    @classmethod
    def mobile_digits_only(cls, v: str) -> str:
        if not v.replace("+", "").isdigit():
            raise ValueError("Mobile number must contain digits only")
        return v

    @field_validator("password")
    @classmethod
    def password_strength(cls, v: str) -> str:
        if not any(c.isupper() for c in v) or not any(c.isdigit() for c in v):
            raise ValueError("Password must contain at least one uppercase letter and one digit")
        return v


class RegisterResponse(BaseModel):
    message: str
    mobile_number: str
    dev_otp: str | None = None  # only populated outside production, see routers/auth.py


class VerifyOtpRequest(BaseModel):
    mobile_number: str
    otp: str


class LoginRequest(BaseModel):
    identifier: str  # email or mobile number
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: str
    username: str


class ForgotPasswordRequest(BaseModel):
    identifier: str  # email or mobile number


class ForgotPasswordResponse(BaseModel):
    message: str
    dev_reset_token: str | None = None


class ResetPasswordRequest(BaseModel):
    reset_token: str
    new_password: str = Field(min_length=8, max_length=128)


class UserProfile(BaseModel):
    user_id: str
    username: str
    email: EmailStr
    mobile_number: str
    upi_id: str | None
    registration_date: datetime

    class Config:
        from_attributes = True


class UpdateProfileRequest(BaseModel):
    username: str | None = Field(default=None, min_length=2, max_length=100)
    upi_id: str | None = None


class AddUpiIdRequest(BaseModel):
    upi_id: str = Field(min_length=3, max_length=100)


class AddBankAccountRequest(BaseModel):
    account_number: str = Field(min_length=6, max_length=34)
    ifsc_code: str = Field(min_length=11, max_length=11)
    account_holder_name: str


class BankAccountResponse(BaseModel):
    account_id: str
    masked_account_number: str
    ifsc_code: str
    account_holder_name: str
    is_verified: bool


# ---------- Use Case 2: Payment Processing ----------


class UpiPaymentRequest(BaseModel):
    recipient_upi_id: str
    amount: float = Field(gt=0)
    note: str | None = Field(default=None, max_length=255)


class BankTransferRequest(BaseModel):
    from_account_id: str
    recipient_account_number: str
    recipient_ifsc_code: str
    amount: float = Field(gt=0)
    purpose: str | None = Field(default=None, max_length=255)


class TransactionResponse(BaseModel):
    transaction_id: str
    type: str
    amount: float
    counterparty: str
    note: str | None
    date: datetime
    status: str
    failure_reason: str | None

    class Config:
        from_attributes = True


# ---------- Use Case 3: Transaction Management ----------


class TransactionHistoryQuery(BaseModel):
    type: str | None = None
    status: str | None = None
    date_from: datetime | None = None
    date_to: datetime | None = None


# ---------- Use Case 4: Notification System ----------


class NotificationResponse(BaseModel):
    notification_id: str
    channel: str
    category: str
    message: str
    created_at: datetime
    is_read: bool

    class Config:
        from_attributes = True


class CreateReminderRequest(BaseModel):
    title: str = Field(min_length=2, max_length=255)
    amount: float = Field(gt=0)
    due_date: datetime


class ReminderResponse(BaseModel):
    reminder_id: str
    title: str
    amount: float
    due_date: datetime
    status: str

    class Config:
        from_attributes = True
