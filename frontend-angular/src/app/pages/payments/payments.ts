import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { ToastService } from '../../core/toast.service';
import { extractErrorMessage } from '../../core/error.util';
import { BankAccountResponse } from '../../core/models';

type Tab = 'upi' | 'bank' | 'add-account';

@Component({
  selector: 'app-payments',
  imports: [FormsModule],
  templateUrl: './payments.html',
  styleUrl: '../app-pages.css',
})
export class Payments implements OnInit {
  tab = signal<Tab>('upi');
  bankAccounts = signal<BankAccountResponse[]>([]);

  // UPI form
  upiRecipient = '';
  upiAmount: number | null = null;
  upiNote = '';
  upiLoading = signal(false);

  // Bank transfer form
  fromAccountId = '';
  recipientAccountNumber = '';
  recipientIfscCode = '';
  transferAmount: number | null = null;
  transferPurpose = '';
  transferLoading = signal(false);

  // Add bank account form
  newAccountNumber = '';
  newIfscCode = '';
  newAccountHolderName = '';
  addAccountLoading = signal(false);

  constructor(
    private readonly api: ApiService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit(): void {
    this.loadAccounts();
  }

  private loadAccounts(): void {
    this.api.listBankAccounts().subscribe((accounts) => this.bankAccounts.set(accounts));
  }

  payUpi(): void {
    if (!this.upiRecipient || !this.upiAmount) return;
    this.upiLoading.set(true);
    this.api.payUpi({ recipientUpiId: this.upiRecipient, amount: this.upiAmount, note: this.upiNote || undefined }).subscribe({
      next: (txn) => {
        this.upiLoading.set(false);
        this.toast.success(`Payment of ₹${txn.amount.toFixed(2)} ${txn.status === 'COMPLETED' ? 'completed' : txn.status.toLowerCase()}.`);
        this.upiRecipient = '';
        this.upiAmount = null;
        this.upiNote = '';
      },
      error: (err) => {
        this.upiLoading.set(false);
        this.toast.error(extractErrorMessage(err, 'Payment could not be completed.'));
      },
    });
  }

  payBankTransfer(): void {
    if (!this.fromAccountId || !this.recipientAccountNumber || !this.recipientIfscCode || !this.transferAmount) return;
    this.transferLoading.set(true);
    this.api
      .payBankTransfer({
        fromAccountId: this.fromAccountId,
        recipientAccountNumber: this.recipientAccountNumber,
        recipientIfscCode: this.recipientIfscCode,
        amount: this.transferAmount,
        purpose: this.transferPurpose || undefined,
      })
      .subscribe({
        next: (txn) => {
          this.transferLoading.set(false);
          this.toast.success(`Transfer of ₹${txn.amount.toFixed(2)} ${txn.status === 'COMPLETED' ? 'completed' : txn.status.toLowerCase()}.`);
          this.recipientAccountNumber = '';
          this.recipientIfscCode = '';
          this.transferAmount = null;
          this.transferPurpose = '';
        },
        error: (err) => {
          this.transferLoading.set(false);
          this.toast.error(extractErrorMessage(err, 'Transfer could not be completed.'));
        },
      });
  }

  addBankAccount(): void {
    if (!this.newAccountNumber || !this.newIfscCode || !this.newAccountHolderName) return;
    this.addAccountLoading.set(true);
    this.api
      .addBankAccount({
        accountNumber: this.newAccountNumber,
        ifscCode: this.newIfscCode,
        accountHolderName: this.newAccountHolderName,
      })
      .subscribe({
        next: () => {
          this.addAccountLoading.set(false);
          this.toast.success('Bank account added.');
          this.newAccountNumber = '';
          this.newIfscCode = '';
          this.newAccountHolderName = '';
          this.loadAccounts();
          this.tab.set('bank');
        },
        error: (err) => {
          this.addAccountLoading.set(false);
          this.toast.error(extractErrorMessage(err, 'Could not add the bank account.'));
        },
      });
  }
}
