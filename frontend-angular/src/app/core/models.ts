// TypeScript interfaces mirroring the Java backend's DTO records (camelCase JSON).

export interface RegisterRequest {
  username: string;
  email: string;
  mobileNumber: string;
  password: string;
}

export interface RegisterResponse {
  message: string;
  mobileNumber: string;
  devOtp: string | null;
}

export interface VerifyOtpRequest {
  mobileNumber: string;
  otp: string;
}

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  userId: string;
  username: string;
}

export interface ForgotPasswordRequest {
  identifier: string;
}

export interface ForgotPasswordResponse {
  message: string;
  devResetToken: string | null;
}

export interface ResetPasswordRequest {
  resetToken: string;
  newPassword: string;
}

export interface UserProfile {
  userId: string;
  username: string;
  email: string;
  mobileNumber: string;
  upiId: string | null;
  registrationDate: string;
}

export interface UpdateProfileRequest {
  username?: string;
  upiId?: string;
}

export interface AddUpiIdRequest {
  upiId: string;
}

export interface AddBankAccountRequest {
  accountNumber: string;
  ifscCode: string;
  accountHolderName: string;
}

export interface BankAccountResponse {
  accountId: string;
  maskedAccountNumber: string;
  ifscCode: string;
  accountHolderName: string;
  isVerified: boolean;
}

export interface UpiPaymentRequest {
  recipientUpiId: string;
  amount: number;
  note?: string;
}

export interface BankTransferRequest {
  fromAccountId: string;
  recipientAccountNumber: string;
  recipientIfscCode: string;
  amount: number;
  purpose?: string;
}

export interface TransactionResponse {
  transactionId: string;
  type: 'UPI' | 'BANK_TRANSFER';
  amount: number;
  counterparty: string;
  note: string | null;
  date: string;
  status: string;
  failureReason: string | null;
}

export interface NotificationResponse {
  notificationId: string;
  channel: string;
  category: string;
  message: string;
  createdAt: string;
  isRead: boolean;
}

export interface CreateReminderRequest {
  title: string;
  amount: number;
  dueDate: string;
}

export interface ReminderResponse {
  reminderId: string;
  title: string;
  amount: number;
  dueDate: string;
  status: string;
}

export interface ApiErrorBody {
  detail: unknown;
}
