import { Component, OnInit, signal } from '@angular/core';
import { DatePipe, TitleCasePipe } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { NotificationResponse } from '../../core/models';

@Component({
  selector: 'app-notifications',
  imports: [DatePipe, TitleCasePipe],
  templateUrl: './notifications.html',
  styleUrl: '../app-pages.css',
})
export class Notifications implements OnInit {
  notifications = signal<NotificationResponse[]>([]);
  loading = signal(true);

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.api.listNotifications().subscribe((notifs) => {
      this.notifications.set(notifs);
      this.loading.set(false);
    });
  }

  markRead(notificationId: string): void {
    this.api.markNotificationRead(notificationId).subscribe((updated) => {
      this.notifications.update((list) => list.map((n) => (n.notificationId === updated.notificationId ? updated : n)));
    });
  }
}
