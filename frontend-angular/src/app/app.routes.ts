import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { Otp } from './pages/otp/otp';
import { ForgotPassword } from './pages/forgot-password/forgot-password';
import { ResetPassword } from './pages/reset-password/reset-password';
import { Shell } from './shell/shell';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: Login, canActivate: [guestGuard] },
  { path: 'register', component: Register, canActivate: [guestGuard] },
  { path: 'otp', component: Otp, canActivate: [guestGuard] },
  { path: 'forgot-password', component: ForgotPassword, canActivate: [guestGuard] },
  { path: 'reset-password', component: ResetPassword, canActivate: [guestGuard] },
  { path: 'app', component: Shell, canActivate: [authGuard] },
  { path: '**', redirectTo: 'login' },
];
