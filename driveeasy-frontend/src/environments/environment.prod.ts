/**
 * Build de production (image Docker) : l'API est appelee sur la meme origine (/api),
 * nginx relaie ces requetes vers l'API Gateway (api-gateway:8080). Le frontend passe donc
 * toujours par la gateway, sans requete cross-origin.
 */
export const environment = {
  production: true,
  apiUrl: '/api',
};
