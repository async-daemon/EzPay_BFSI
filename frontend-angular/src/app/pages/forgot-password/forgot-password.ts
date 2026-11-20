import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { ToastService } from '../../core/toast.service';
import { extractErrorMessage } from '../../core/error.util';
import { AuthShell } from '../../shared/auth-shell';

@Component({
  selector: 'app-forgot-password',
  imports: [FormsModule, RouterLink, AuthShell],
  templateUrl: './forgot-password.html',
  styleUrl: '../auth-pages.css',
})
export class ForgotPassword {
  identifier = '';
  devResetToken: string | null = null;
  message: string | null = null;
  loading = signal(false);

  constructor(
    private readonly api: ApiService,
    private readonly toast: ToastService,
    private readonly router: Router,
  ) {}

  useDevToken(): void {
    if (this.devResetToken) {
      this.router.navigate(['/reset-password'], { state: { resetToken: this.devResetToken } });
    }
  }

  submit(): void {
    if (!this.identifier) return;
    this.loading.set(true);
    this.api.forgotPassword({ identifier: this.identifier }).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.message = res.message;
        this.devResetToken = res.devResetToken;
        if (!res.devResetToken) {
          this.toast.success(res.message);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.toast.error(extractErrorMessage(err));
      },
    });
  }
}
