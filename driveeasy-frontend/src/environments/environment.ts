/**
 * Developpement (ng serve) : les appels /api sont relayes vers l'API Gateway (http://localhost:8080)
 * par proxy.conf.json. Le frontend ne parle donc qu'a la gateway, jamais directement aux microservices.
 * (La gateway autorise aussi http://localhost:4200 en CORS pour un appel direct.)
 */
export const environment = {
  production: false,
  apiUrl: '/api',
};
