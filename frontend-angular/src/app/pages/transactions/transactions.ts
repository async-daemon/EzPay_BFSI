import { Component, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { TransactionResponse } from '../../core/models';

@Component({
  selector: 'app-transactions',
  imports: [DatePipe, DecimalPipe, FormsModule],
  templateUrl: './transactions.html',
  styleUrl: '../app-pages.css',
})
export class Transactions implements OnInit {
  transactions = signal<TransactionResponse[]>([]);
  loading = signal(true);
  typeFilter = '';
  statusFilter = '';

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.api
      .listTransactions({ type: this.typeFilter || undefined, status: this.statusFilter || undefined })
      .subscribe((txns) => {
        this.transactions.set(txns);
        this.loading.set(false);
      });
  }
}
