import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { ToastService } from '../../core/toast.service';
import { extractErrorMessage } from '../../core/error.util';
import { AuthShell } from '../../shared/auth-shell';

@Component({
  selector: 'app-reset-password',
  imports: [FormsModule, RouterLink, AuthShell],
  templateUrl: './reset-password.html',
  styleUrl: '../auth-pages.css',
})
export class ResetPassword {
  resetToken = '';
  newPassword = '';
  showPassword = signal(false);
  loading = signal(false);

  constructor(
    private readonly api: ApiService,
    private readonly toast: ToastService,
    private readonly router: Router,
  ) {
    const state = this.router.getCurrentNavigation()?.extras.state ?? (history.state as Record<string, unknown>);
    this.resetToken = (state?.['resetToken'] as string) ?? '';
  }

  submit(): void {
    if (!this.resetToken || !this.newPassword) return;
    this.loading.set(true);
    this.api.resetPassword({ resetToken: this.resetToken, newPassword: this.newPassword }).subscribe({
      next: () => {
        this.toast.success('Password updated. Please log in.');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading.set(false);
        this.toast.error(extractErrorMessage(err, 'Reset link is invalid or has expired.'));
      },
    });
  }
}
