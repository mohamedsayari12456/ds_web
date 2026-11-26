# DriveEasy — Frontend Angular 22 (espace agence)

Application web des agents d'agence DriveEasy : tableau de bord, catalogue et gestion des véhicules, clients,
assistant de réservation, suivi et clôture des locations.

**Toutes les requêtes passent par l'API Gateway** — jamais directement par les microservices.
Le code appelle `/api/...` : en développement, `ng serve` relaie ces appels vers `http://localhost:8080`
(`proxy.conf.json`) ; dans l'image Docker, c'est nginx qui les relaie vers `api-gateway:8080` (`nginx.conf`).
La gateway autorise aussi l'origine `http://localhost:4200` en CORS pour un appel direct.

## Prérequis

- Node.js **22.12+ ou 24** et npm
- Le backend DriveEasy démarré (voir `driveeasy-backend/README.md`), gateway sur le port 8080

## Installation et lancement

```bash
cd driveeasy-frontend
npm install
npm start            # = ng serve  ->  http://localhost:4200
```

Build de production : `npm run build` (sortie dans `dist/driveeasy-frontend`). Tests unitaires : `npm test`.

## Écrans

| Route | Écran | Contenu |
|---|---|---|
| `/dashboard` | Tableau de bord | Véhicules disponibles / loués / maintenance, CA du mois et total, locations en cours et en retard, graphiques (CA sur 6 mois, état du parc, locations par statut), top véhicules |
| `/vehicules` | Catalogue | Grille de cartes avec image, filtres (catégorie, carburant, statut, prix max, texte), tri, pagination ; maintenance / suppression |
| `/vehicules/new`, `/vehicules/:id/edit` | Formulaire véhicule | Formulaire réactif validé (année ≤ année courante, prix > 0…), gestion du statut, erreurs serveur affichées |
| `/clients` | Liste des clients | Tableau paginé, recherche nom / CIN / email, badge STANDARD / FIDÈLE / VIP, alerte permis expiré ou expirant |
| `/clients/:id` | Fiche client | Informations, éligibilité (âge, permis), historique des locations |
| `/clients/new`, `/clients/:id/edit` | Formulaire client | Création / modification |
| `/locations/new` | Nouvelle location | Assistant en 5 étapes : client → période → véhicule disponible → simulation du prix → confirmation |
| `/locations` | Liste des locations | Filtres statut / période, pagination serveur, actions départ / retour / annulation |
| `/locations/:id/retour` | Retour de véhicule | Saisie du kilométrage, aperçu des pénalités, récapitulatif final imprimable |

## Architecture

```
src/app/
├── app.ts / app.html            coquille : menu latéral, barre de chargement, notifications
├── app.routes.ts                routes chargées à la demande (loadComponent) + guards
├── app.config.ts                HttpClient + intercepteurs, router (withComponentInputBinding), locale fr
├── core/
│   ├── models/                  interfaces TypeScript (Vehicule, Client, Location, Page, Statistiques…)
│   ├── services/                VehiculeService, ClientService, LocationService (HTTP), Notification, Loading
│   ├── interceptors/            loading.interceptor (barre de progression), error.interceptor (messages ApiError)
│   └── guards/                  unsaved-changes (canDeactivate), id-param (canActivate)
├── shared/                      pagination, graphique Chart.js, pipe libelle
└── pages/                       dashboard, vehicules, clients, locations (composants standalone)
public/assets/vehicules/         illustrations par catégorie (images par défaut)
```

Choix techniques : composants **standalone**, **signals** et `computed`, nouveau control flow (`@if`, `@for`,
`@switch`), application **zoneless** (défaut Angular 22), formulaires réactifs typés, intercepteurs fonctionnels,
lazy loading par écran, Chart.js pour les graphiques.
