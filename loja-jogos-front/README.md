# loja-jogos-front

Frontend da GameStore desenvolvido em Angular 18.

## Requisitos
- Node.js 18.19+ (ou 20+)
- npm
- API/backend da loja disponível em `http://localhost:8080`

## Instalação e execução
No terminal, entre nesta pasta e execute:

```bash
npm install
npm start
```

Abra `http://localhost:4200`.

## Build de produção
```bash
npm run build
```

## API
As chamadas são feitas para `/api`. O proxy em `proxy.conf.json` encaminha essas chamadas para `http://localhost:8080`.
Se o backend estiver em outra porta, ajuste o arquivo `proxy.conf.json`.

## Observações
- Login aceita resposta de usuário direta ou o formato `{ "token": "...", "usuario": { ... } }`.
- O token retornado pela API é usado no cabeçalho `Authorization: Bearer ...`.
- O cadastro de usuário ainda depende de um endpoint de cadastro implementado no backend.
