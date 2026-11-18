import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/guards';

export const routes: Routes = [
  { path: '', redirectTo: 'catalogo', pathMatch: 'full' },
  { path: 'catalogo', loadComponent: () => import('./pages/catalogo.component').then(m => m.CatalogoComponent) },
  { path: 'jogo/:id', loadComponent: () => import('./pages/jogo-detalhe.component').then(m => m.JogoDetalheComponent) },
  { path: 'carrinho', loadComponent: () => import('./pages/carrinho.component').then(m => m.CarrinhoComponent) },
  { path: 'login', loadComponent: () => import('./pages/login.component').then(m => m.LoginComponent) },
  { path: 'registro', loadComponent: () => import('./pages/registro.component').then(m => m.RegistroComponent) },
  { path: 'pedidos', canActivate: [authGuard], loadComponent: () => import('./pages/meus-pedidos.component').then(m => m.MeusPedidosComponent) },
  {
    path: 'admin', canActivate: [adminGuard],
    loadComponent: () => import('./pages/admin/admin.component').then(m => m.AdminComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./pages/admin/dashboard/dashboard.component').then(m => m.AdminDashboardComponent) },
      { path: 'jogos', loadComponent: () => import('./pages/admin/admin-jogos.component').then(m => m.AdminJogosComponent) },
      { path: 'usuarios', loadComponent: () => import('./pages/admin/admin-usuarios.component').then(m => m.AdminUsuariosComponent) },
      { path: 'pedidos', loadComponent: () => import('./pages/admin/admin-pedidos.component').then(m => m.AdminPedidosComponent) }
    ]
  },
  { path: '**', redirectTo: 'catalogo' }
];
