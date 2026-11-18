export const environment = {
  production: false,
  // '/api' (relativo) + proxy.conf.json: o ng serve repassa /api para http://localhost:8080.
  // Alternativa sem proxy: 'http://localhost:8080/api' (exige o CORS configurado no back).
  apiUrl: '/api'
};
