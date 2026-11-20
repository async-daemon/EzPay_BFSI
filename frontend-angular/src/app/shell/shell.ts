import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { ApiService } from '../core/api.service';
import { ToastService } from '../core/toast.service';
import { Dashboard } from '../pages/dashboard/dashboard';
import { Payments } from '../pages/payments/payments';
import { Transactions } from '../pages/transactions/transactions';
import { Reminders } from '../pages/reminders/reminders';
import { Notifications } from '../pages/notifications/notifications';
import { Profile } from '../pages/profile/profile';

type Section = 'dashboard' | 'payments' | 'transactions' | 'reminders' | 'notifications' | 'profile';

@Component({
  selector: 'app-shell',
  imports: [Dashboard, Payments, Transactions, Reminders, Notifications, Profile],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell {
  section = signal<Section>('dashboard');
  mobileNavOpen = signal(false);

  readonly navItems: { id: Section; label: string }[] = [
    { id: 'dashboard', label: 'Dashboard' },
    { id: 'payments', label: 'Payments' },
    { id: 'transactions', label: 'Transactions' },
    { id: 'reminders', label: 'Reminders' },
    { id: 'notifications', label: 'Notifications' },
    { id: 'profile', label: 'Profile' },
  ];

  constructor(
    protected readonly auth: AuthService,
    private readonly api: ApiService,
    private readonly toast: ToastService,
    private readonly router: Router,
  ) {}

  goTo(section: Section): void {
    this.section.set(section);
    this.mobileNavOpen.set(false);
  }

  logout(): void {
    const finish = () => {
      this.auth.clearSession();
      this.router.navigate(['/login']);
    };
    this.api.logout().subscribe({ next: finish, error: finish });
  }
}
