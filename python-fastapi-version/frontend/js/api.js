/**
 * Thin wrapper around the EzPay FastAPI backend.
 * Change API_BASE if the backend runs on a different host/port.
 */
const API_BASE = window.EZPAY_API_BASE || "http://127.0.0.1:8008";

const Auth = {
  getToken() {
    return localStorage.getItem("ezpay_token");
  },
  setSession(token, userId, username) {
    localStorage.setItem("ezpay_token", token);
    localStorage.setItem("ezpay_user_id", userId);
    localStorage.setItem("ezpay_username", username);
  },
  clear() {
    localStorage.removeItem("ezpay_token");
    localStorage.removeItem("ezpay_user_id");
    localStorage.removeItem("ezpay_username");
  },
  isLoggedIn() {
    return Boolean(this.getToken());
  },
  username() {
    return localStorage.getItem("ezpay_username") || "";
  },
};

class ApiError extends Error {
  constructor(message, status, payload) {
    super(message);
    this.status = status;
    this.payload = payload;
  }
}

async function apiRequest(path, { method = "GET", body, auth = true } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (auth && Auth.getToken()) {
    headers["Authorization"] = `Bearer ${Auth.getToken()}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (networkErr) {
    throw new ApiError(
      "Could not reach the EzPay server. Is the backend running?",
      0,
      null
    );
  }

  if (response.status === 204) return null;

  let data = null;
  try {
    data = await response.json();
  } catch (_) {
    /* no JSON body */
  }

  if (!response.ok) {
    const detail = data && data.detail ? data.detail : response.statusText;
    const message = typeof detail === "string" ? detail : JSON.stringify(detail);
    throw new ApiError(message, response.status, data);
  }

  return data;
}

const Api = {
  register: (payload) => apiRequest("/auth/register", { method: "POST", body: payload, auth: false }),
  verifyOtp: (payload) => apiRequest("/auth/verify-otp", { method: "POST", body: payload, auth: false }),
  login: (payload) => apiRequest("/auth/login", { method: "POST", body: payload, auth: false }),
  logout: () => apiRequest("/auth/logout", { method: "POST" }),
  forgotPassword: (payload) => apiRequest("/auth/forgot-password", { method: "POST", body: payload, auth: false }),
  resetPassword: (payload) => apiRequest("/auth/reset-password", { method: "POST", body: payload, auth: false }),

  getProfile: () => apiRequest("/profile"),
  updateProfile: (payload) => apiRequest("/profile", { method: "PUT", body: payload }),
  addUpiId: (payload) => apiRequest("/profile/upi-id", { method: "POST", body: payload }),
  addBankAccount: (payload) => apiRequest("/profile/bank-accounts", { method: "POST", body: payload }),
  listBankAccounts: () => apiRequest("/profile/bank-accounts"),

  payUpi: (payload) => apiRequest("/payments/upi", { method: "POST", body: payload }),
  payBankTransfer: (payload) => apiRequest("/payments/bank-transfer", { method: "POST", body: payload }),

  listTransactions: (query = "") => apiRequest(`/transactions${query}`),

  createReminder: (payload) => apiRequest("/reminders", { method: "POST", body: payload }),
  listReminders: () => apiRequest("/reminders"),
  markReminderPaid: (id) => apiRequest(`/reminders/${id}/mark-paid`, { method: "POST" }),
  sendReminderNow: (id) => apiRequest(`/reminders/${id}/send`, { method: "POST" }),

  listNotifications: () => apiRequest("/notifications"),
  markNotificationRead: (id) => apiRequest(`/notifications/${id}/read`, { method: "POST" }),
};
