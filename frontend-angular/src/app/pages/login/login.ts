import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ToastService } from '../../core/toast.service';
import { extractErrorMessage } from '../../core/error.util';
import { AuthShell } from '../../shared/auth-shell';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink, AuthShell],
  templateUrl: './login.html',
  styleUrl: '../auth-pages.css',
})
export class Login {
  identifier = '';
  password = '';
  showPassword = signal(false);
  loading = signal(false);

  constructor(
    private readonly api: ApiService,
    private readonly auth: AuthService,
    private readonly toast: ToastService,
    private readonly router: Router,
  ) {}

  submit(): void {
    if (!this.identifier || !this.password) return;
    this.loading.set(true);
    this.api.login({ identifier: this.identifier, password: this.password }).subscribe({
      next: (res) => {
        this.auth.setSession(res.accessToken, res.userId, res.username);
        this.toast.success(`Welcome back, ${res.username}!`);
        this.router.navigate(['/app']);
      },
      error: (err) => {
        this.loading.set(false);
        this.toast.error(extractErrorMessage(err, 'Invalid credentials.'));
      },
    });
  }
}
