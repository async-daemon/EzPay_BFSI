import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { ToastService } from '../../core/toast.service';
import { extractErrorMessage } from '../../core/error.util';
import { ReminderResponse } from '../../core/models';

@Component({
  selector: 'app-reminders',
  imports: [DatePipe, FormsModule],
  templateUrl: './reminders.html',
  styleUrl: '../app-pages.css',
})
export class Reminders implements OnInit {
  reminders = signal<ReminderResponse[]>([]);
  loading = signal(true);

  title = '';
  amount: number | null = null;
  dueDate = '';
  createLoading = signal(false);

  constructor(
    private readonly api: ApiService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.api.listReminders().subscribe((reminders) => {
      this.reminders.set(reminders);
      this.loading.set(false);
    });
  }

  create(): void {
    if (!this.title || !this.amount || !this.dueDate) return;
    this.createLoading.set(true);
    this.api.createReminder({ title: this.title, amount: this.amount, dueDate: new Date(this.dueDate).toISOString() }).subscribe({
      next: () => {
        this.createLoading.set(false);
        this.toast.success('Reminder created.');
        this.title = '';
        this.amount = null;
        this.dueDate = '';
        this.reload();
      },
      error: (err) => {
        this.createLoading.set(false);
        this.toast.error(extractErrorMessage(err, 'Could not create the reminder.'));
      },
    });
  }

  sendNow(reminderId: string): void {
    this.api.sendReminderNow(reminderId).subscribe({
      next: () => this.toast.success('Reminder notification sent.'),
      error: (err) => this.toast.error(extractErrorMessage(err)),
    });
  }

  markPaid(reminderId: string): void {
    this.api.markReminderPaid(reminderId).subscribe({
      next: () => {
        this.toast.success('Marked as paid.');
        this.reload();
      },
      error: (err) => this.toast.error(extractErrorMessage(err)),
    });
  }
}
