/* EzPay frontend — vanilla JS SPA driving the FastAPI backend in api.js. */

let pendingMobile = null; // set after register(), used by the OTP step

// ---------- icon / markup helpers ----------

function icon(name, cls = "") {
  return `<svg class="icon ${cls}"><use href="#i-${name}"/></svg>`;
}

function emptyState(iconName, text) {
  return `<div class="emptyState">${icon(iconName)}<span>${text}</span></div>`;
}

function skeletonList(count = 3) {
  return Array.from({ length: count }, () => `<div class="skeleton"></div>`).join("");
}

// ---------- helpers ----------

function showToast(message, kind = "") {
  const toast = document.getElementById("toast");
  const iconName = kind === "success" ? "check-circle" : kind === "error" ? "alert-circle" : "bell";
  toast.innerHTML = `${icon(iconName)}<span>${message}</span>`;
  toast.className = `toast ${kind}`;
  requestAnimationFrame(() => toast.classList.remove("hidden"));
  clearTimeout(showToast._t);
  showToast._t = setTimeout(() => toast.classList.add("hidden"), 3500);
}

function setFormMsg(id, message, kind = "") {
  const el = document.getElementById(id);
  el.textContent = message || "";
  el.className = `formMsg ${kind}`;
}

function formatMoney(amount) {
  return `₹${Number(amount).toLocaleString("en-IN", { minimumFractionDigits: 2 })}`;
}

function formatDate(iso) {
  const d = new Date(iso);
  return d.toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

function pill(status) {
  const map = { COMPLETED: "check-circle", PAID: "check-circle", BLOCKED: "alert-circle", FAILED: "alert-circle" };
  const ic = map[status] ? icon(map[status]) : "";
  return `<span class="pill ${status.toLowerCase()}">${ic}${status.replace(/_/g, " ")}</span>`;
}

function setButtonLoading(form, isLoading) {
  const btn = form.querySelector('button[type="submit"]');
  if (!btn) return;
  btn.classList.toggle("loading", isLoading);
  btn.disabled = isLoading;
}

async function withLoading(form, fn) {
  setButtonLoading(form, true);
  try {
    return await fn();
  } finally {
    setButtonLoading(form, false);
  }
}

/** Animate a number from its current displayed value up to `target`. */
function animateCount(el, target) {
  const start = parseInt(el.textContent, 10) || 0;
  if (start === target) {
    el.textContent = target;
    return;
  }
  const duration = 600;
  const startTime = performance.now();
  function step(now) {
    const progress = Math.min((now - startTime) / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    el.textContent = Math.round(start + (target - start) * eased);
    if (progress < 1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
}

function switchTab(groupSelector, btnAttr, panelAttr, value) {
  document.querySelectorAll(groupSelector).forEach((btn) => {
    btn.classList.toggle("active", btn.getAttribute(btnAttr) === value);
  });
  document.querySelectorAll(`[${panelAttr}]`).forEach((panel) => {
    panel.classList.toggle("active", panel.getAttribute(panelAttr) === value);
  });
}

// ---------- password visibility toggles ----------

document.querySelectorAll(".toggleVisibility").forEach((btn) => {
  const input = btn.previousElementSibling;
  btn.addEventListener("click", () => {
    const show = input.type === "password";
    input.type = show ? "text" : "password";
    btn.querySelector("use").setAttribute("href", show ? "#i-eye-off" : "#i-eye");
    btn.setAttribute("aria-label", show ? "Hide password" : "Show password");
  });
});

function showAuthPanel(name) {
  document.querySelectorAll(".tabPanel[data-panel]").forEach((p) => {
    p.classList.toggle("active", p.dataset.panel === name);
  });
}

// ---------- navigation ----------

function showView(view) {
  document.querySelectorAll(".view").forEach((v) => v.classList.remove("active"));
  document.getElementById(`view-${view}`).classList.add("active");
  document.querySelectorAll(".navBtn[data-view]").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.view === view);
  });
  closeMobileNav();

  if (view === "dashboard") loadDashboard();
  if (view === "payments") loadPaymentsView();
  if (view === "transactions") loadTransactions();
  if (view === "reminders") loadReminders();
  if (view === "notifications") loadNotifications();
  if (view === "profile") loadProfile();
}

function enterApp() {
  document.getElementById("mainNav").classList.remove("hidden");
  document.getElementById("navToggle").classList.remove("hidden");
  document.getElementById("dashUsername").textContent = Auth.username();
  showView("dashboard");
  refreshNotifBadge();
}

function exitApp() {
  Auth.clear();
  document.getElementById("mainNav").classList.add("hidden");
  document.getElementById("navToggle").classList.add("hidden");
  document.querySelectorAll(".view").forEach((v) => v.classList.remove("active"));
  document.getElementById("view-auth").classList.add("active");
  showAuthPanel("login");
}

// ---------- auth handlers ----------

document.querySelectorAll(".tabBtn[data-tab]").forEach((btn) => {
  btn.addEventListener("click", () => {
    switchTab(".tabBtn[data-tab]", "data-tab", "data-panel", btn.dataset.tab);
  });
});

document.getElementById("loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  setFormMsg("loginMsg", "");
  const form = e.target;
  const data = new FormData(form);
  try {
    const res = await withLoading(form, () =>
      Api.login({ identifier: data.get("identifier"), password: data.get("password") })
    );
    Auth.setSession(res.access_token, res.user_id, res.username);
    showToast(`Welcome back, ${res.username}!`, "success");
    enterApp();
  } catch (err) {
    setFormMsg("loginMsg", err.message, "error");
  }
});

document.getElementById("registerForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  setFormMsg("registerMsg", "");
  document.getElementById("devOtpBanner").classList.add("hidden");
  const form = e.target;
  const data = new FormData(form);
  const payload = Object.fromEntries(data.entries());
  try {
    const res = await withLoading(form, () => Api.register(payload));
    pendingMobile = res.mobile_number;
    document.getElementById("otpMobile").textContent = res.mobile_number;
    showAuthPanel("otp");

    const banner = document.getElementById("devOtpBanner");
    if (res.dev_otp) {
      document.getElementById("devOtpValue").textContent = res.dev_otp;
      banner.classList.remove("hidden");
      showToast("No SMS gateway configured — your code is shown below", "");
    } else {
      banner.classList.add("hidden");
      showToast("Verification code sent to your mobile number", "success");
    }
  } catch (err) {
    setFormMsg("registerMsg", err.message, "error");
  }
});

document.getElementById("devOtpFill").addEventListener("click", () => {
  const code = document.getElementById("devOtpValue").textContent;
  const input = document.querySelector('#otpForm [name="otp"]');
  input.value = code;
  input.focus();
});

document.getElementById("otpForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  setFormMsg("otpMsg", "");
  const form = e.target;
  const data = new FormData(form);
  try {
    const res = await withLoading(form, () => Api.verifyOtp({ mobile_number: pendingMobile, otp: data.get("otp") }));
    Auth.setSession(res.access_token, res.user_id, res.username);
    showToast("Account verified. Welcome to EzPay!", "success");
    enterApp();
  } catch (err) {
    setFormMsg("otpMsg", err.message, "error");
  }
});

document.getElementById("forgotForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  setFormMsg("forgotMsg", "");
  const form = e.target;
  const data = new FormData(form);
  try {
    const res = await withLoading(form, () => Api.forgotPassword({ identifier: data.get("identifier") }));
    setFormMsg("forgotMsg", res.message, "success");
    if (res.dev_reset_token) {
      showAuthPanel("reset");
      document.querySelector('#resetForm [name="reset_token"]').value = res.dev_reset_token;
      showToast("Dev mode: reset token pre-filled below", "success");
    }
  } catch (err) {
    setFormMsg("forgotMsg", err.message, "error");
  }
});

document.getElementById("resetForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  setFormMsg("resetMsg", "");
  const form = e.target;
  const data = new FormData(form);
  try {
    await withLoading(form, () =>
      Api.resetPassword({ reset_token: data.get("reset_token"), new_password: data.get("new_password") })
    );
    setFormMsg("resetMsg", "Password reset. You can now log in.", "success");
    setTimeout(() => showAuthPanel("login"), 1200);
  } catch (err) {
    setFormMsg("resetMsg", err.message, "error");
  }
});

document.getElementById("logoutBtn").addEventListener("click", async () => {
  try {
    await Api.logout();
  } catch (_) {
    /* ignore network errors on logout */
  }
  showToast("Logged out", "success");
  exitApp();
});

// ---------- nav ----------

document.querySelectorAll(".navBtn[data-view]").forEach((btn) => {
  btn.addEventListener("click", () => showView(btn.dataset.view));
});

function closeMobileNav() {
  document.getElementById("mainNav").classList.remove("open");
  const toggleIcon = document.querySelector("#navToggle use");
  if (toggleIcon) toggleIcon.setAttribute("href", "#i-menu");
}

document.getElementById("navToggle").addEventListener("click", () => {
  const nav = document.getElementById("mainNav");
  const open = nav.classList.toggle("open");
  const toggleIcon = document.querySelector("#navToggle use");
  if (toggleIcon) toggleIcon.setAttribute("href", open ? "#i-close" : "#i-menu");
});

// ---------- dashboard ----------

async function loadDashboard() {
  const container = document.getElementById("dashRecent");
  container.innerHTML = skeletonList(3);
  try {
    const [profile, txns, reminders] = await Promise.all([
      Api.getProfile(),
      Api.listTransactions(),
      Api.listReminders(),
    ]);
    document.getElementById("dashUsername").textContent = profile.username;
    document.getElementById("dashUpi").textContent = profile.upi_id || "Not set";
    animateCount(document.getElementById("dashTxnCount"), txns.length);
    animateCount(document.getElementById("dashReminderCount"), reminders.filter((r) => r.status !== "PAID").length);

    const recent = txns.slice(0, 5);
    container.innerHTML = recent.length
      ? recent.map(txnItem).join("")
      : emptyState("wallet", "No transactions yet. Make your first payment!");
  } catch (err) {
    container.innerHTML = emptyState("alert-circle", "Could not load dashboard data.");
    showToast(err.message, "error");
  }
}

// ---------- payments ----------

document.querySelectorAll(".tabBtn[data-ptab]").forEach((btn) => {
  btn.addEventListener("click", () => {
    switchTab(".tabBtn[data-ptab]", "data-ptab", "data-ppanel", btn.dataset.ptab);
  });
});

async function loadPaymentsView() {
  await populateBankAccountSelect();
  await renderBankAccounts();
}

async function populateBankAccountSelect() {
  const select = document.getElementById("fromAccountSelect");
  select.innerHTML = "";
  try {
    const accounts = await Api.listBankAccounts();
    if (!accounts.length) {
      select.innerHTML = `<option value="">Add a bank account first</option>`;
      return;
    }
    accounts.forEach((a) => {
      const opt = document.createElement("option");
      opt.value = a.account_id;
      opt.textContent = `${a.account_holder_name} — ${a.masked_account_number} (${a.ifsc_code})`;
      select.appendChild(opt);
    });
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function renderBankAccounts() {
  const container = document.getElementById("bankAccountsList");
  container.innerHTML = skeletonList(2);
  try {
    const accounts = await Api.listBankAccounts();
    container.innerHTML = accounts.length
      ? accounts
          .map(
            (a) => `
        <div class="listItem">
          <div class="row">
            <div class="itemIcon gold">${icon("bank")}</div>
            <div class="main">
              <span class="title">${a.account_holder_name}</span>
              <span class="sub">${a.masked_account_number} · ${a.ifsc_code}</span>
            </div>
          </div>
          ${a.is_verified ? pill("VERIFIED") : pill("PROCESSING")}
        </div>`
          )
          .join("")
      : emptyState("bank", "No bank accounts added yet.");
  } catch (err) {
    showToast(err.message, "error");
  }
}

document.getElementById("upiForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  setFormMsg("upiMsg", "");
  const form = e.target;
  const data = new FormData(form);
  try {
    const txn = await withLoading(form, () =>
      Api.payUpi({
        recipient_upi_id: data.get("recipient_upi_id"),
        amount: parseFloat(data.get("amount")),
        note: data.get("note") || null,
      })
    );
    setFormMsg("upiMsg", `Payment ${txn.status.toLowerCase()}. Txn ID: ${txn.transaction_id}`, "success");
    showToast("Payment sent successfully", "success");
    form.reset();
    refreshNotifBadge();
  } catch (err) {
    setFormMsg("upiMsg", err.message, "error");
  }
});

document.getElementById("bankForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  setFormMsg("bankMsg", "");
  const form = e.target;
  const data = new FormData(form);
  if (!data.get("from_account_id")) {
    setFormMsg("bankMsg", "Please add a bank account first.", "error");
    return;
  }
  try {
    const txn = await withLoading(form, () =>
      Api.payBankTransfer({
        from_account_id: data.get("from_account_id"),
        recipient_account_number: data.get("recipient_account_number"),
        recipient_ifsc_code: data.get("recipient_ifsc_code").toUpperCase(),
        amount: parseFloat(data.get("amount")),
        purpose: data.get("purpose") || null,
      })
    );
    setFormMsg("bankMsg", `Transfer ${txn.status.toLowerCase()}. Txn ID: ${txn.transaction_id}`, "success");
    showToast("Transfer completed", "success");
    form.reset();
    refreshNotifBadge();
  } catch (err) {
    setFormMsg("bankMsg", err.message, "error");
  }
});

document.getElementById("bankAddForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  setFormMsg("bankAddMsg", "");
  const form = e.target;
  const data = new FormData(form);
  try {
    await withLoading(form, () =>
      Api.addBankAccount({
        account_number: data.get("account_number"),
        ifsc_code: data.get("ifsc_code").toUpperCase(),
        account_holder_name: data.get("account_holder_name"),
      })
    );
    setFormMsg("bankAddMsg", "Bank account added.", "success");
    showToast("Bank account added", "success");
    form.reset();
    await populateBankAccountSelect();
    await renderBankAccounts();
  } catch (err) {
    setFormMsg("bankAddMsg", err.message, "error");
  }
});

// ---------- transactions ----------

function txnItem(t) {
  const sign = t.type === "UPI" ? "To" : "To A/C";
  const iconName = t.type === "UPI" ? "phone" : "bank";
  return `
    <div class="listItem">
      <div class="row">
        <div class="itemIcon">${icon(iconName)}</div>
        <div class="main">
          <span class="title">${t.type.replace("_", " ")} · ${sign} ${t.counterparty}</span>
          <span class="sub">${formatDate(t.date)}${t.note ? " · " + t.note : ""}</span>
          ${t.failure_reason ? `<span class="sub">${t.failure_reason}</span>` : ""}
        </div>
      </div>
      <div class="side">
        <span class="amount">${formatMoney(t.amount)}</span>
        ${pill(t.status)}
      </div>
    </div>`;
}

async function loadTransactions() {
  const type = document.getElementById("filterType").value;
  const status = document.getElementById("filterStatus").value;
  const params = new URLSearchParams();
  if (type) params.set("type", type);
  if (status) params.set("status", status);
  const qs = params.toString() ? `?${params.toString()}` : "";

  const container = document.getElementById("txnList");
  container.innerHTML = skeletonList(4);
  try {
    const txns = await Api.listTransactions(qs);
    container.innerHTML = txns.length
      ? txns.map(txnItem).join("")
      : emptyState("clock", "No transactions found.");
  } catch (err) {
    container.innerHTML = emptyState("alert-circle", "Could not load transactions.");
    showToast(err.message, "error");
  }
}

document.getElementById("filterType").addEventListener("change", loadTransactions);
document.getElementById("filterStatus").addEventListener("change", loadTransactions);
document.getElementById("refreshTxns").addEventListener("click", loadTransactions);

// ---------- reminders ----------

document.getElementById("reminderForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  setFormMsg("reminderMsg", "");
  const form = e.target;
  const data = new FormData(form);
  try {
    await withLoading(form, () =>
      Api.createReminder({
        title: data.get("title"),
        amount: parseFloat(data.get("amount")),
        due_date: new Date(data.get("due_date")).toISOString(),
      })
    );
    setFormMsg("reminderMsg", "Reminder added.", "success");
    showToast("Reminder added", "success");
    form.reset();
    loadReminders();
  } catch (err) {
    setFormMsg("reminderMsg", err.message, "error");
  }
});

function reminderItem(r) {
  return `
    <div class="listItem" data-id="${r.reminder_id}">
      <div class="row">
        <div class="itemIcon gold">${icon("calendar")}</div>
        <div class="main">
          <span class="title">${r.title}</span>
          <span class="sub">Due ${formatDate(r.due_date)}</span>
        </div>
      </div>
      <div class="side">
        <span class="amount">${formatMoney(r.amount)}</span>
        ${pill(r.status)}
        ${
          r.status !== "PAID"
            ? `<button class="secondaryBtn" data-action="mark-paid" data-id="${r.reminder_id}">${icon("check-circle")} Mark Paid</button>`
            : ""
        }
      </div>
    </div>`;
}

async function loadReminders() {
  const container = document.getElementById("reminderList");
  container.innerHTML = skeletonList(3);
  try {
    const reminders = await Api.listReminders();
    container.innerHTML = reminders.length
      ? reminders.map(reminderItem).join("")
      : emptyState("calendar", "No reminders yet.");

    container.querySelectorAll('[data-action="mark-paid"]').forEach((btn) => {
      btn.addEventListener("click", async () => {
        try {
          await Api.markReminderPaid(btn.dataset.id);
          showToast("Reminder marked as paid", "success");
          loadReminders();
        } catch (err) {
          showToast(err.message, "error");
        }
      });
    });
  } catch (err) {
    container.innerHTML = emptyState("alert-circle", "Could not load reminders.");
    showToast(err.message, "error");
  }
}

// ---------- notifications ----------

function notifItem(n) {
  return `
    <div class="listItem" data-id="${n.notification_id}" style="${n.is_read ? "opacity:0.6;" : ""}">
      <div class="row">
        <div class="itemIcon">${icon("bell")}</div>
        <div class="main">
          <span class="title">${n.category[0].toUpperCase()}${n.category.slice(1)}</span>
          <span class="sub">${n.message}</span>
          <span class="sub">${formatDate(n.created_at)}</span>
        </div>
      </div>
      ${!n.is_read ? `<button class="secondaryBtn" data-action="mark-read" data-id="${n.notification_id}">${icon("check-circle")} Mark read</button>` : ""}
    </div>`;
}

async function loadNotifications() {
  const container = document.getElementById("notifList");
  container.innerHTML = skeletonList(3);
  try {
    const notifications = await Api.listNotifications();
    container.innerHTML = notifications.length
      ? notifications.map(notifItem).join("")
      : emptyState("bell", "No notifications yet.");

    container.querySelectorAll('[data-action="mark-read"]').forEach((btn) => {
      btn.addEventListener("click", async () => {
        try {
          await Api.markNotificationRead(btn.dataset.id);
          loadNotifications();
          refreshNotifBadge();
        } catch (err) {
          showToast(err.message, "error");
        }
      });
    });
    refreshNotifBadge();
  } catch (err) {
    container.innerHTML = emptyState("alert-circle", "Could not load notifications.");
    showToast(err.message, "error");
  }
}

async function refreshNotifBadge() {
  try {
    const notifications = await Api.listNotifications();
    const unread = notifications.filter((n) => !n.is_read).length;
    const badge = document.getElementById("notifBadge");
    if (unread > 0) {
      badge.textContent = unread;
      badge.classList.remove("hidden");
    } else {
      badge.classList.add("hidden");
    }
  } catch (_) {
    /* non-fatal */
  }
}

// ---------- profile ----------

async function loadProfile() {
  try {
    const profile = await Api.getProfile();
    document.getElementById("profileUsername").value = profile.username;
    document.getElementById("profileEmail").value = profile.email;
    document.getElementById("profileMobile").value = profile.mobile_number;
    document.getElementById("profileUpi").value = profile.upi_id || "";
    document.getElementById("profileHeaderName").textContent = profile.username;
    document.getElementById("profileHeaderEmail").textContent = profile.email;
    document.getElementById("profileAvatar").textContent = profile.username.trim().charAt(0).toUpperCase() || "U";
  } catch (err) {
    showToast(err.message, "error");
  }
}

document.getElementById("profileForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  setFormMsg("profileMsg", "");
  const form = e.target;
  const data = new FormData(form);
  try {
    await withLoading(form, () =>
      Api.updateProfile({
        username: data.get("username"),
        upi_id: data.get("upi_id") || null,
      })
    );
    setFormMsg("profileMsg", "Profile updated.", "success");
    showToast("Profile updated", "success");
    document.getElementById("dashUsername").textContent = data.get("username");
    document.getElementById("profileHeaderName").textContent = data.get("username");
    document.getElementById("profileAvatar").textContent = data.get("username").trim().charAt(0).toUpperCase() || "U";
    localStorage.setItem("ezpay_username", data.get("username"));
  } catch (err) {
    setFormMsg("profileMsg", err.message, "error");
  }
});

// ---------- bootstrap ----------

if (Auth.isLoggedIn()) {
  enterApp();
} else {
  showAuthPanel("login");
}
