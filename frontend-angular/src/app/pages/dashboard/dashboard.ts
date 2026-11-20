import { Component, EventEmitter, OnInit, Output, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { TransactionResponse, NotificationResponse, ReminderResponse } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './dashboard.html',
  styleUrl: '../app-pages.css',
})
export class Dashboard implements OnInit {
  @Output() navigate = new EventEmitter<'payments' | 'transactions' | 'reminders' | 'notifications'>();

  transactions = signal<TransactionResponse[]>([]);
  reminders = signal<ReminderResponse[]>([]);
  notifications = signal<NotificationResponse[]>([]);
  loading = signal(true);

  constructor(
    private readonly api: ApiService,
    protected readonly auth: AuthService,
  ) {}

  ngOnInit(): void {
    this.api.listTransactions().subscribe((txns) => this.transactions.set(txns));
    this.api.listReminders().subscribe((reminders) => this.reminders.set(reminders));
    this.api.listNotifications().subscribe((notifs) => {
      this.notifications.set(notifs);
      this.loading.set(false);
    });
  }

  get pendingReminders(): number {
    return this.reminders().filter((r) => r.status !== 'PAID').length;
  }

  get unreadNotifications(): number {
    return this.notifications().filter((n) => !n.isRead).length;
  }

  get recentTransactions(): TransactionResponse[] {
    return this.transactions().slice(0, 5);
  }
}
