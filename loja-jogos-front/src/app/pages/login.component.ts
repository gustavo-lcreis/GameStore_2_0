import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { ToastService } from '../core/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="form-box">
      <h1>Entrar</h1>
      <label>E-mail <input type="email" name="email" [(ngModel)]="email" autocomplete="email"></label>
      <label>Senha <input type="password" name="senha" [(ngModel)]="senha" autocomplete="current-password" (keyup.enter)="entrar()"></label>
      @if (erro()) { <p class="erro">{{ erro() }}</p> }
      <button class="btn btn-primary" (click)="entrar()">Entrar</button>
      <p class="meta">Ainda não tem conta? <a routerLink="/registro"><u>Criar conta</u></a></p>
    </div>
  `
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);
  email = ''; senha = '';
  erro = signal('');

  entrar() {
    if (!this.email || !this.senha) { this.erro.set('Informe e-mail e senha.'); return; }
    this.auth.login({ username: this.email, password: this.senha }).subscribe({
      next: r => {
        this.toast.sucesso('Login realizado com sucesso!');
        this.router.navigate([r.tipo === 'ADMIN' || r.role === 'ROLE_ADMIN' ? '/admin' : '/catalogo']);
      },
      error: () => { this.erro.set('E-mail ou senha incorretos.'); this.toast.erro('E-mail ou senha incorretos.'); }
    });
  }
}
