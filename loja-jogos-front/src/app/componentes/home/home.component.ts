import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { CartService } from '../../core/cart.service';
import { Jogo } from '../../models';
import { JOGOS_EXEMPLO } from './jogos-exemplo'; 
import { CapaCssPipe } from '../../core/capa-css.pipe';
imports: [RouterLink, CurrencyPipe, CapaCssPipe]

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  private api = inject(ApiService);
  cart = inject(CartService);

  jogos = signal<Jogo[]>([]);
  aviso = signal('');

  ngOnInit() {
    this.api.listarJogos().subscribe({
      next: lista => this.jogos.set(lista.slice(0, 10)),
      error: () => {
        this.jogos.set(JOGOS_EXEMPLO);
        this.aviso.set('API indisponível: exibindo jogos de exemplo.');
      }
    });
  }
}
