import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { ToastService } from '../../core/toast.service';
import { AuthService } from '../../core/auth.service';
import { CartService } from '../../core/cart.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent {
  auth = inject(AuthService);
  cart = inject(CartService);
  private router = inject(Router);
  private toast = inject(ToastService);

  sair() {
    this.auth.logout();
    this.toast.info('Você saiu da sua conta.');
    this.router.navigate(['/catalogo']);
  }
}
