import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { ToastService } from '../../core/toast.service';
import { extractErrorMessage } from '../../core/error.util';
import { BankAccountResponse, UserProfile } from '../../core/models';

@Component({
  selector: 'app-profile',
  imports: [DatePipe, FormsModule],
  templateUrl: './profile.html',
  styleUrl: '../app-pages.css',
})
export class Profile implements OnInit {
  profile = signal<UserProfile | null>(null);
  bankAccounts = signal<BankAccountResponse[]>([]);

  username = '';
  upiId = '';
  saving = signal(false);

  constructor(
    private readonly api: ApiService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit(): void {
    this.api.getProfile().subscribe((profile) => {
      this.profile.set(profile);
      this.username = profile.username;
      this.upiId = profile.upiId ?? '';
    });
    this.api.listBankAccounts().subscribe((accounts) => this.bankAccounts.set(accounts));
  }

  saveProfile(): void {
    this.saving.set(true);
    this.api.updateProfile({ username: this.username, upiId: this.upiId || undefined }).subscribe({
      next: (profile) => {
        this.saving.set(false);
        this.profile.set(profile);
        this.toast.success('Profile updated.');
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(extractErrorMessage(err, 'Could not update the profile.'));
      },
    });
  }
}
