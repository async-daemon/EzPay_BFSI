"""Maps to the ER/class diagram's `EncryptionService` (encryptData/decryptData).

Used to encrypt sensitive fields at rest (e.g. bank account numbers) per the
Security Requirements table: "Implement end-to-end encryption for all data
transmissions" / data protection compliance.
"""

from __future__ import annotations

from cryptography.fernet import Fernet

from app.config import get_settings

settings = get_settings()


def _get_fernet() -> Fernet:
    key = settings.field_encryption_key
    if not key:
        # Dev-time fallback so the app runs without extra setup; a real
        # deployment must set FIELD_ENCRYPTION_KEY (see .env.example).
        key = Fernet.generate_key().decode()
    return Fernet(key.encode() if isinstance(key, str) else key)


_fernet = _get_fernet()


def encrypt_data(plain_text: str) -> str:
    if plain_text is None:
        return plain_text
    return _fernet.encrypt(plain_text.encode()).decode()


def decrypt_data(cipher_text: str) -> str:
    if cipher_text is None:
        return cipher_text
    return _fernet.decrypt(cipher_text.encode()).decode()


def mask_account_number(account_number: str) -> str:
    """Display helper: never show a full account number back to the client."""
    if not account_number or len(account_number) <= 4:
        return "****"
    return "*" * (len(account_number) - 4) + account_number[-4:]
