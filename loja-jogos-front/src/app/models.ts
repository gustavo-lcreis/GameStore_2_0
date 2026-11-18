export interface Categoria { id: number; nome: string; }

// Formato de resposta paginada do Spring (Page<T>). Só usado caso o back passe a paginar.
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; }

export interface Jogo {
  id?: number;
  titulo: string;
  descricao: string;
  preco: number;
  imagem: string;
  plataforma: string;
  categoria?: Categoria;
  categoriaId?: number | null;
}

export interface Usuario { id: number; nome: string; email: string; tipo: 'CLIENTE' | 'ADMIN'; role?: string; }

export interface ItemCarrinho { jogo: Jogo; quantidade: number; }

/** Espelha ItemPedidoResponse do back. */
export interface ItemPedido { jogoId: number | null; titulo: string; quantidade: number; precoUnitario: number; subtotal: number; }

/** Espelha PedidoResponse do back (antes o front esperava data/usuarioNome/jogoTitulo, que o back nunca enviou). */
export interface Pedido {
  id: number; dataHora: string; clienteNome: string; clienteEmail: string;
  status: string; quantidadeTotal: number; total: number; itens: ItemPedido[];
}

export interface VendasResposta {
  resumo: { quantidadeVendas: number; valorTotal: number };
  vendas: Pedido[]; page: number; size: number; totalElements: number; totalPages: number;
}

export interface ExclusaoUsuarioResposta { mensagem: string; pedidosPreservados: number; }

export type Agrupamento = 'DIA' | 'MES';

export interface DashboardResposta {
  inicio: string; fim: string; agrupamento: Agrupamento;
  kpis: { totalPedidosGeral: number; totalPedidos: number; faturamento: number; ticketMedio: number; jogosVendidos: number };
  porStatus: { status: string; pedidos: number; faturamento: number }[];
  topJogos: { jogoId: number | null; titulo: string; quantidade: number; receita: number }[];
  evolucao: PontoEvolucao[];
}
export interface PontoEvolucao { periodo: string; pedidos: number; faturamento: number; }

export interface LoginResposta { token: string; usuario: Usuario; }

export type JogoFormDados = Pick<Jogo, 'titulo' | 'descricao' | 'preco' | 'plataforma' | 'categoriaId'>;