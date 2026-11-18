import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <h1>Administração</h1>
    <nav class="abas">
      <a routerLink="dashboard" routerLinkActive="ativa">Dashboard</a>
      <a routerLink="jogos" routerLinkActive="ativa">Jogos</a>
      <a routerLink="usuarios" routerLinkActive="ativa">Usuários</a>
      <a routerLink="pedidos" routerLinkActive="ativa">Pedidos</a>
    </nav>
    <router-outlet />
  `,
  styles: [`
    .abas { display: flex; gap: 1.25rem; border-bottom: 1px solid var(--line); margin-bottom: 1.5rem; }
    .abas a { padding: .5rem 0; color: var(--muted); }
    .abas a.ativa { color: var(--ink); border-bottom: 3px solid var(--accent); }
  `]
})
export class AdminComponent {}
