import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import * as m from './models';

const API_BASE = (window as any).EZPAY_API_BASE || 'http://127.0.0.1:8081';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private readonly http: HttpClient) {}

  // --- Use Case 1: User Management ---
  register(payload: m.RegisterRequest): Observable<m.RegisterResponse> {
    return this.http.post<m.RegisterResponse>(`${API_BASE}/auth/register`, payload);
  }

  verifyOtp(payload: m.VerifyOtpRequest): Observable<m.TokenResponse> {
    return this.http.post<m.TokenResponse>(`${API_BASE}/auth/verify-otp`, payload);
  }

  login(payload: m.LoginRequest): Observable<m.TokenResponse> {
    return this.http.post<m.TokenResponse>(`${API_BASE}/auth/login`, payload);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${API_BASE}/auth/logout`, {});
  }

  forgotPassword(payload: m.ForgotPasswordRequest): Observable<m.ForgotPasswordResponse> {
    return this.http.post<m.ForgotPasswordResponse>(`${API_BASE}/auth/forgot-password`, payload);
  }

  resetPassword(payload: m.ResetPasswordRequest): Observable<void> {
    return this.http.post<void>(`${API_BASE}/auth/reset-password`, payload);
  }

  getProfile(): Observable<m.UserProfile> {
    return this.http.get<m.UserProfile>(`${API_BASE}/profile`);
  }

  updateProfile(payload: m.UpdateProfileRequest): Observable<m.UserProfile> {
    return this.http.put<m.UserProfile>(`${API_BASE}/profile`, payload);
  }

  addUpiId(payload: m.AddUpiIdRequest): Observable<m.UserProfile> {
    return this.http.post<m.UserProfile>(`${API_BASE}/profile/upi-id`, payload);
  }

  addBankAccount(payload: m.AddBankAccountRequest): Observable<m.BankAccountResponse> {
    return this.http.post<m.BankAccountResponse>(`${API_BASE}/profile/bank-accounts`, payload);
  }

  listBankAccounts(): Observable<m.BankAccountResponse[]> {
    return this.http.get<m.BankAccountResponse[]>(`${API_BASE}/profile/bank-accounts`);
  }

  // --- Use Case 2: Payment Processing ---
  // Note: the backend returns HTTP 202 (not 2xx-success-shaped) when fraud
  // detection flags a payment as REQUIRES_VERIFICATION — observe the full
  // response so that case can be routed into the error channel instead of
  // being parsed as a successful TransactionResponse.
  payUpi(payload: m.UpiPaymentRequest): Observable<m.TransactionResponse> {
    return this.postPayment(`${API_BASE}/payments/upi`, payload);
  }

  payBankTransfer(payload: m.BankTransferRequest): Observable<m.TransactionResponse> {
    return this.postPayment(`${API_BASE}/payments/bank-transfer`, payload);
  }

  private postPayment(url: string, payload: unknown): Observable<m.TransactionResponse> {
    return this.http.post(url, payload, { observe: 'response' }).pipe(
      switchMap((res) => {
        if (res.status === 202) {
          return throwError(() => new HttpErrorResponse({ status: 202, error: res.body, url: res.url ?? undefined }));
        }
        return of(res.body as m.TransactionResponse);
      }),
    );
  }

  // --- Use Case 3: Transaction Management ---
  listTransactions(filters?: { type?: string; status?: string }): Observable<m.TransactionResponse[]> {
    let params = new HttpParams();
    if (filters?.type) params = params.set('type', filters.type);
    if (filters?.status) params = params.set('status', filters.status);
    return this.http.get<m.TransactionResponse[]>(`${API_BASE}/transactions`, { params });
  }

  trackTransaction(transactionId: string): Observable<m.TransactionResponse> {
    return this.http.get<m.TransactionResponse>(`${API_BASE}/transactions/${transactionId}`);
  }

  // --- Use Case 4: Notifications & Reminders ---
  createReminder(payload: m.CreateReminderRequest): Observable<m.ReminderResponse> {
    return this.http.post<m.ReminderResponse>(`${API_BASE}/reminders`, payload);
  }

  listReminders(): Observable<m.ReminderResponse[]> {
    return this.http.get<m.ReminderResponse[]>(`${API_BASE}/reminders`);
  }

  sendReminderNow(reminderId: string): Observable<void> {
    return this.http.post<void>(`${API_BASE}/reminders/${reminderId}/send`, {});
  }

  markReminderPaid(reminderId: string): Observable<m.ReminderResponse> {
    return this.http.post<m.ReminderResponse>(`${API_BASE}/reminders/${reminderId}/mark-paid`, {});
  }

  listNotifications(): Observable<m.NotificationResponse[]> {
    return this.http.get<m.NotificationResponse[]>(`${API_BASE}/notifications`);
  }

  markNotificationRead(notificationId: string): Observable<m.NotificationResponse> {
    return this.http.post<m.NotificationResponse>(`${API_BASE}/notifications/${notificationId}/read`, {});
  }
}
