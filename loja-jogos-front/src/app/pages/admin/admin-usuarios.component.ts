import { Component, HostListener, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { ToastService } from '../../core/toast.service';
import { Usuario } from '../../models';

@Component({
  selector: 'app-admin-usuarios',
  standalone: true,
  template: `
    <div class="toolbar">
      <input type="search" placeholder="Buscar por nome ou e-mail" aria-label="Buscar usuários"
             [value]="busca()" (input)="busca.set($any($event.target).value)">
    </div>

    @if (erro()) { <p class="erro">{{ erro() }}</p> <button class="btn" (click)="carregar()">Tentar novamente</button> }
    @else if (carregando()) { <p class="empty">Carregando usuários…</p> }
    @else if (filtrados().length === 0) {
      <p class="empty">{{ busca() ? 'Nenhum usuário encontrado para essa busca.' : 'Nenhum usuário cadastrado.' }}</p>
    }

    <div class="table-wrap">
      <table>
        <thead><tr><th>ID</th><th>Nome</th><th>E-mail</th><th>Tipo</th><th></th></tr></thead>
        <tbody>
          @for (u of filtrados(); track u.id) {
            <tr>
              <td>{{ u.id }}</td><td>{{ u.nome }}</td><td>{{ u.email }}</td><td>{{ u.tipo }}</td>
              <td>
                @if (u.tipo === 'CLIENTE') {
                  <button class="btn btn-danger" (click)="pedirExclusao(u)">Excluir</button>
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>

    @if (alvo(); as u) {
      <div class="fundo" (click)="cancelar()">
        <div class="modal" role="dialog" aria-modal="true" aria-labelledby="titulo-exclusao" (click)="$event.stopPropagation()">
          <h2 id="titulo-exclusao">Excluir cliente?</h2>
          <p><strong>{{ u.nome }}</strong> ({{ u.email }}) perderá o acesso à conta. Esta ação não pode ser desfeita.</p>
          <p class="meta">Os pedidos dele continuam no histórico de vendas, sem vínculo com a conta.</p>
          <label>Para confirmar, digite o e-mail do cliente:
            <input [value]="confirmacao()" (input)="confirmacao.set($any($event.target).value)"
                   autocomplete="off" [placeholder]="u.email">
          </label>
          <div class="actions fim">
            <button class="btn" (click)="cancelar()" [disabled]="excluindo()">Cancelar</button>
            <button class="btn btn-danger" (click)="confirmar()" [disabled]="!podeConfirmar() || excluindo()">
              {{ excluindo() ? 'Excluindo…' : 'Excluir definitivamente' }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .fundo { position: fixed; inset: 0; background: rgba(28, 26, 46, .55); display: grid; place-items: center; padding: 1rem; z-index: 50; }
    .modal { background: var(--card); border-radius: var(--radius); padding: 1.5rem; max-width: 460px; width: 100%; }
    .fim { justify-content: flex-end; }
  `]
})
export class AdminUsuariosComponent implements OnInit {
  private api = inject(ApiService);
  private toast = inject(ToastService);

  usuarios = signal<Usuario[]>([]);
  carregando = signal(true);
  erro = signal<string | null>(null);

  busca = signal('');
  filtrados = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    if (!termo) return this.usuarios();
    return this.usuarios().filter(u =>
      (u.nome ?? '').toLowerCase().includes(termo) || (u.email ?? '').toLowerCase().includes(termo));
  });

  alvo = signal<Usuario | null>(null);
  confirmacao = signal('');
  excluindo = signal(false);
  podeConfirmar = computed(() =>
    !!this.alvo() && this.confirmacao().trim().toLowerCase() === this.alvo()!.email.toLowerCase());

  ngOnInit() { this.carregar(); }

  carregar() {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.listarUsuarios().subscribe({
      next: u => { this.usuarios.set(u); this.carregando.set(false); },
      error: (e: HttpErrorResponse) => {
        this.carregando.set(false);
        this.erro.set(`Erro ao carregar usuários (HTTP ${e.status}).`);
      }
    });
  }

  pedirExclusao(u: Usuario) {
    if (u.tipo !== 'CLIENTE') return; // a UI esconde o botão; o back também recusa (409)
    this.confirmacao.set('');
    this.alvo.set(u);
  }

  @HostListener('document:keydown.escape')
  cancelar() {
    if (this.excluindo()) return;
    this.alvo.set(null);
  }

  confirmar() {
    const u = this.alvo();
    if (!u || !this.podeConfirmar() || this.excluindo()) return;
    this.excluindo.set(true);
    this.api.excluirUsuario(u.id).subscribe({
      next: r => {
        this.usuarios.update(lista => lista.filter(x => x.id !== u.id));
        this.toast.sucesso(r.mensagem);
        this.fechar();
      },
      error: (e: HttpErrorResponse) => {
        if (e.status === 404) { // já não existe (outro admin excluiu): some da lista também
          this.usuarios.update(lista => lista.filter(x => x.id !== u.id));
        }
        if (e.status !== 401) this.toast.erro(this.mensagemDeErro(e));
        this.fechar();
      }
    });
  }

  private fechar() { this.excluindo.set(false); this.alvo.set(null); }

  private mensagemDeErro(e: HttpErrorResponse): string {
    if (e.error?.mensagem) return e.error.mensagem;
    if (e.status === 403) return 'Você não tem permissão para excluir usuários.';
    if (e.status === 0) return 'Sem conexão com o servidor. Tente novamente.';
    return `Não foi possível excluir o usuário (HTTP ${e.status}).`;
  }
}
