import { Jogo } from '../../models';

// Usado apenas quando a API não responde, para o front-end poder ser desenvolvido sem o back-end.
export const JOGOS_EXEMPLO: Jogo[] = [
  { id: 1,  titulo: 'Neon Drift',        descricao: 'Corrida arcade em cidades futuristas.',  preco: 59.9,  imagem: '', plataforma: 'PC',  categoria: { id: 1, nome: 'Corrida' } },
  { id: 2,  titulo: 'Dungeon Bytes',     descricao: 'RPG de masmorras com sorteio de fases.', preco: 79.9,  imagem: '', plataforma: 'PC',  categoria: { id: 2, nome: 'RPG' } },
  { id: 3,  titulo: 'Cozy Farm Stories', descricao: 'Plante, colha e decore sua fazenda.',    preco: 39.9,  imagem: '', plataforma: 'PC',  categoria: { id: 3, nome: 'Simulação' } },
  { id: 4,  titulo: 'Sky Raiders',       descricao: 'Combate aéreo cooperativo.',             preco: 99.9,  imagem: '', plataforma: 'PS5', categoria: { id: 4, nome: 'Ação' } },
  { id: 5,  titulo: 'Puzzle Orbit',      descricao: 'Quebra-cabeças em gravidade zero.',      preco: 19.9,  imagem: '', plataforma: 'PC',  categoria: { id: 5, nome: 'Puzzle' } },
  { id: 6,  titulo: 'Last Signal',       descricao: 'Sobrevivência e mistério numa estação.', preco: 89.9,  imagem: '', plataforma: 'Xbox', categoria: { id: 6, nome: 'Terror' } },
  { id: 7,  titulo: 'Kick Masters',      descricao: 'Futebol de rua com habilidades.',        preco: 69.9,  imagem: '', plataforma: 'PS5', categoria: { id: 7, nome: 'Esporte' } },
  { id: 8,  titulo: 'Blade Kingdom',     descricao: 'Estratégia medieval em tempo real.',     preco: 74.9,  imagem: '', plataforma: 'PC',  categoria: { id: 8, nome: 'Estratégia' } },
  { id: 9,  titulo: 'Pixel Quest',       descricao: 'Plataforma retrô com chefes épicos.',    preco: 24.9,  imagem: '', plataforma: 'Switch', categoria: { id: 9, nome: 'Plataforma' } },
  { id: 10, titulo: 'Deep Blue',         descricao: 'Explore o fundo do mar em mundo aberto.', preco: 94.9, imagem: '', plataforma: 'PC',  categoria: { id: 10, nome: 'Aventura' } }
];
