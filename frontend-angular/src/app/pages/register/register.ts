import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { ToastService } from '../../core/toast.service';
import { extractErrorMessage } from '../../core/error.util';
import { AuthShell } from '../../shared/auth-shell';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink, AuthShell],
  templateUrl: './register.html',
  styleUrl: '../auth-pages.css',
})
export class Register {
  username = '';
  email = '';
  mobileNumber = '';
  password = '';
  showPassword = signal(false);
  loading = signal(false);

  constructor(
    private readonly api: ApiService,
    private readonly toast: ToastService,
    private readonly router: Router,
  ) {}

  submit(): void {
    if (!this.username || !this.email || !this.mobileNumber || !this.password) return;
    this.loading.set(true);
    this.api
      .register({
        username: this.username,
        email: this.email,
        mobileNumber: this.mobileNumber,
        password: this.password,
      })
      .subscribe({
        next: (res) => {
          if (res.devOtp) {
            this.toast.show('No SMS gateway configured — your code is shown on the next screen', 'info');
          } else {
            this.toast.success('Verification code sent to your mobile number');
          }
          this.router.navigate(['/otp'], { state: { mobileNumber: res.mobileNumber, devOtp: res.devOtp } });
        },
        error: (err) => {
          this.loading.set(false);
          this.toast.error(extractErrorMessage(err, 'Could not create the account.'));
        },
      });
  }
}
