import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ToastService } from '../../core/toast.service';
import { extractErrorMessage } from '../../core/error.util';
import { AuthShell } from '../../shared/auth-shell';

@Component({
  selector: 'app-otp',
  imports: [FormsModule, RouterLink, AuthShell],
  templateUrl: './otp.html',
  styleUrl: '../auth-pages.css',
})
export class Otp {
  mobileNumber = '';
  otp = '';
  devOtp: string | null = null;
  loading = signal(false);

  constructor(
    private readonly api: ApiService,
    private readonly auth: AuthService,
    private readonly toast: ToastService,
    private readonly router: Router,
  ) {
    const state = this.router.getCurrentNavigation()?.extras.state ?? (history.state as Record<string, unknown>);
    this.mobileNumber = (state?.['mobileNumber'] as string) ?? '';
    this.devOtp = (state?.['devOtp'] as string) ?? null;
    if (!this.mobileNumber) {
      this.router.navigate(['/register']);
    }
  }

  useDevCode(): void {
    if (this.devOtp) this.otp = this.devOtp;
  }

  submit(): void {
    if (!this.mobileNumber || !this.otp) return;
    this.loading.set(true);
    this.api.verifyOtp({ mobileNumber: this.mobileNumber, otp: this.otp }).subscribe({
      next: (res) => {
        this.auth.setSession(res.accessToken, res.userId, res.username);
        this.toast.success('Account verified! Welcome to EzPay.');
        this.router.navigate(['/app']);
      },
      error: (err) => {
        this.loading.set(false);
        this.toast.error(extractErrorMessage(err, 'Invalid or expired verification code.'));
      },
    });
  }
}
