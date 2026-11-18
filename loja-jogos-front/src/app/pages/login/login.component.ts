import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <h2>Login Admin</h2>
        <form (submit)="doLogin()">
          <div class="form-group">
            <label>Usuário</label>
            <input type="text" [(ngModel)]="credentials.username" name="username" required>
          </div>
          <div class="form-group">
            <label>Senha</label>
            <input type="password" [(ngModel)]="credentials.password" name="password" required>
          </div>
          <button type="submit" [disabled]="loading">
            {{ loading ? 'Entrando...' : 'Entrar' }}
          </button>
        </form>
        <p *ngIf="errorMessage" class="error">{{ errorMessage }}</p>
      </div>
    </div>
    <style>
      .login-container { display: flex; justify-content: center; align-items: center; height: 100vh; background: #f0f2f5; }
      .login-card { background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); width: 350px; }
      .form-group { margin-bottom: 1rem; display: flex; flex-direction: column; }
      input { padding: 0.5rem; margin-top: 0.25rem; border: 1px solid #ccc; border-radius: 4px; }
      button { width: 100%; padding: 0.75rem; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; }
      button:disabled { background: #ccc; }
      .error { color: red; font-size: 0.8rem; margin-top: 1rem; text-align: center; }
    </style>
  `
})
export class LoginComponent {
  credentials = { username: '', password: '' };
  loading = false;
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  doLogin() {
    this.loading = true;
    this.errorMessage = '';
    this.authService.login(this.credentials).subscribe({
      next: (user) => {
        if (user.role === 'ROLE_ADMIN') {
          this.router.navigate(['/admin']);
        } else {
          this.errorMessage = 'Acesso negado. Você não é admin.';
        }
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Usuário ou senha incorretos.';
        this.loading = false;
      }
    });
  }
}
