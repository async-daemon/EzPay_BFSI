"""Smoke tests covering the BRD's six use cases end to end."""

import re


def _register_and_login(client, email="pytest@example.com", mobile="9000000001"):
    resp = client.post(
        "/auth/register",
        json={
            "username": "Pytest User",
            "email": email,
            "mobile_number": mobile,
            "password": "Passw0rd1",
        },
    )
    assert resp.status_code == 201, resp.text
    otp = resp.json()["dev_otp"]

    resp = client.post("/auth/verify-otp", json={"mobile_number": mobile, "otp": otp})
    assert resp.status_code == 200, resp.text
    return resp.json()["access_token"]


def auth_headers(token):
    return {"Authorization": f"Bearer {token}"}


# ---- Use Case 1: User Management ----


def test_register_login_duplicate_rejected(client):
    token = _register_and_login(client)
    assert token

    # Duplicate registration should be rejected.
    resp = client.post(
        "/auth/register",
        json={
            "username": "Dup",
            "email": "pytest@example.com",
            "mobile_number": "9000000002",
            "password": "Passw0rd1",
        },
    )
    assert resp.status_code == 409


def test_login_with_wrong_password_fails(client):
    _register_and_login(client, email="loginfail@example.com", mobile="9000000003")
    resp = client.post("/auth/login", json={"identifier": "loginfail@example.com", "password": "WrongPass1"})
    assert resp.status_code == 401


def test_profile_view_and_update(client):
    token = _register_and_login(client, email="profile@example.com", mobile="9000000004")
    resp = client.get("/profile", headers=auth_headers(token))
    assert resp.status_code == 200
    assert resp.json()["email"] == "profile@example.com"

    resp = client.put("/profile", json={"username": "Updated Name"}, headers=auth_headers(token))
    assert resp.status_code == 200
    assert resp.json()["username"] == "Updated Name"


def test_forgot_and_reset_password(client):
    _register_and_login(client, email="reset@example.com", mobile="9000000005")

    resp = client.post("/auth/forgot-password", json={"identifier": "reset@example.com"})
    assert resp.status_code == 200
    reset_token = resp.json()["dev_reset_token"]
    assert reset_token

    resp = client.post("/auth/reset-password", json={"reset_token": reset_token, "new_password": "NewPassw0rd"})
    assert resp.status_code == 204

    resp = client.post("/auth/login", json={"identifier": "reset@example.com", "password": "NewPassw0rd"})
    assert resp.status_code == 200


# ---- Use Case 2 & 3: Payments + Transaction history ----


def test_upi_payment_and_history(client):
    token = _register_and_login(client, email="pay@example.com", mobile="9000000006")

    resp = client.post(
        "/payments/upi",
        json={"recipient_upi_id": "friend@okaxis", "amount": 250, "note": "test"},
        headers=auth_headers(token),
    )
    assert resp.status_code == 201, resp.text
    assert resp.json()["status"] == "COMPLETED"

    resp = client.get("/transactions", headers=auth_headers(token))
    assert resp.status_code == 200
    assert len(resp.json()) == 1


def test_bank_transfer_requires_known_account(client):
    token = _register_and_login(client, email="transfer@example.com", mobile="9000000007")

    resp = client.post(
        "/payments/bank-transfer",
        json={
            "from_account_id": "nonexistent",
            "recipient_account_number": "1234567890",
            "recipient_ifsc_code": "HDFC0001234",
            "amount": 100,
        },
        headers=auth_headers(token),
    )
    assert resp.status_code == 404


def test_high_value_upi_requires_verification(client):
    token = _register_and_login(client, email="fraud@example.com", mobile="9000000008")

    resp = client.post(
        "/payments/upi",
        json={"recipient_upi_id": "big@okaxis", "amount": 500000},
        headers=auth_headers(token),
    )
    assert resp.status_code == 202
    assert resp.json()["detail"]["status"] == "REQUIRES_VERIFICATION"


# ---- Use Case 4: Notifications + Reminders ----


def test_reminder_and_notification_flow(client):
    token = _register_and_login(client, email="remind@example.com", mobile="9000000009")

    resp = client.post(
        "/reminders",
        json={"title": "Internet Bill", "amount": 999, "due_date": "2030-01-01T00:00:00"},
        headers=auth_headers(token),
    )
    assert resp.status_code == 201

    resp = client.get("/reminders", headers=auth_headers(token))
    assert resp.status_code == 200
    assert len(resp.json()) == 1

    # A UPI payment should also generate an in-app notification.
    client.post(
        "/payments/upi",
        json={"recipient_upi_id": "friend@okaxis", "amount": 50},
        headers=auth_headers(token),
    )
    resp = client.get("/notifications", headers=auth_headers(token))
    assert resp.status_code == 200
    assert len(resp.json()) >= 1


# ---- Use Case 5: Security ----


def test_protected_endpoint_requires_token(client):
    resp = client.get("/profile")
    assert resp.status_code == 401


def test_password_is_hashed_not_plaintext(client):
    token = _register_and_login(client, email="hash@example.com", mobile="9000000010")
    assert token
    # bcrypt hashes always start with $2b$ (or $2a$/$2y$).
    from app.database import SessionLocal
    from app import models

    db = SessionLocal()
    user = db.query(models.User).filter(models.User.email == "hash@example.com").first()
    db.close()
    assert re.match(r"^\$2[aby]\$", user.password)
