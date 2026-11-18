// loja-jogos-front/src/app/pages/registro.component.ts
import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { ToastService } from '../core/toast.service';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="form-box">
      <h1>Criar conta</h1>
      <label>Nome <input type="text" name="nome" [(ngModel)]="nome" autocomplete="name"></label>
      <label>E-mail <input type="email" name="email" [(ngModel)]="email" autocomplete="email"></label>
      <label>Senha (mínimo 6 caracteres) <input type="password" name="senha" [(ngModel)]="senha" autocomplete="new-password" (keyup.enter)="criar()"></label>
      @if (erro()) { <p class="erro" role="alert">{{ erro() }}</p> }
      <button class="btn btn-primary" (click)="criar()" [disabled]="enviando()">
        {{ enviando() ? 'Criando...' : 'Criar conta' }}
      </button>
      <p class="meta">Já tem conta? <a routerLink="/login"><u>Entrar</u></a></p>
    </div>
  `
})
export class RegistroComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);
  nome = ''; email = ''; senha = '';
  erro = signal('');
  enviando = signal(false);

  criar() {
    if (!this.nome.trim() || !this.email.trim() || this.senha.length < 6) {
      this.erro.set('Preencha nome, e-mail e uma senha com pelo menos 6 caracteres.');
      return;
    }
    this.erro.set('');
    this.enviando.set(true);
    this.auth.registrar({ nome: this.nome.trim(), email: this.email.trim(), senha: this.senha }).subscribe({
      next: () => { this.toast.sucesso('Conta criada com sucesso!'); this.router.navigate(['/catalogo']); },
      error: (e: HttpErrorResponse) => {
        console.error('Falha ao criar conta:', e);
        this.erro.set(this.mensagemDeErro(e));
        this.toast.erro(this.mensagemDeErro(e));
        this.enviando.set(false);
      }
    });
  }

  private mensagemDeErro(e: HttpErrorResponse): string {
    if (e.status === 0) return 'Sem conexão com a API. Verifique se o backend está rodando em http://localhost:8080.';
    const msg = e.error?.mensagem; // corpo padrão do @ControllerAdvice
    if (typeof msg === 'string' && msg) return msg;
    if (e.status === 404 || e.status === 405) return 'O backend ainda não tem o cadastro. Reinicie o backend com o código novo.';
    return `Não foi possível criar a conta (HTTP ${e.status}).`;
  }
}