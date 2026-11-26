# DriveEasy — Backend microservices (Spring Boot 3 / Spring Cloud)

Prototype de plateforme de **location de véhicules** pour les agences DriveEasy : gestion du parc, des clients
et du cycle complet des locations (réservation → départ → retour → facturation, annulation), avec indicateurs
(chiffre d'affaires, taux d'occupation, véhicules les plus loués).

> Mini-projet LIM3 — Développement d'applications web et multimédia (J2EE). Enseignant : Karim Ben Romdhane.

## Architecture

```
                         ┌───────────────────────┐
 Angular (4200) ───────▶ │ API Gateway  :8080    │  routes /api/** → lb://service (StripPrefix=1)
                         │ CORS + filtre perso   │
                         └──────────┬────────────┘
                                    │ load balancing (Eureka)
        ┌───────────────────────────┼───────────────────────────┐
        ▼                           ▼                           ▼
 vehicule-service :8081     client-service :8082      location-service :8083
  parc, statuts, planning    clients, éligibilité       cycle de vie, prix, stats
        ▲                           ▲                           │
        └──────── REST synchrone (RestClient @LoadBalanced) ────┘

 service-registry (Eureka) :8761   config-server :8888 (config-repo/)   Zipkin :9411 (Docker)
```

| Composant | Technologie | Port | Rôle |
|---|---|---|---|
| service-registry | Spring Cloud Netflix Eureka Server | 8761 | Annuaire des services |
| config-server | Spring Cloud Config Server | 8888 | Configuration centralisée (`config-repo/`, profil `git` pour GitHub) |
| vehicule-service | Spring Boot Web | 8081 | Parc automobile, catégories, statuts, disponibilité |
| client-service | Spring Boot Web | 8082 | Clients, permis, éligibilité, fidélité |
| location-service | Spring Boot Web | 8083 | Réservations, départ, retour, pénalités, statistiques |
| api-gateway | Spring Cloud Gateway (WebFlux) | 8080 | Point d'entrée unique, CORS, filtre global personnalisé |
| zipkin | openzipkin/zipkin (Docker) | 9411 | Traçage distribué |

**Stockage** : uniquement en mémoire (RAM, `ConcurrentHashMap`) — aucune base de données, conformément au
cahier des charges. Un jeu de données de démonstration cohérent est chargé au démarrage de chaque service.

## Prérequis

- **JDK 21** (`java -version`)
- **Maven 3.9+** (`mvn -v`) — facultatif : sans Maven installé, utiliser le Maven Wrapper fourni (`mvnw.cmd` / `./mvnw`)
- **Docker Desktop** (pour Zipkin ; facultatif — les services fonctionnent sans)
- Ports libres : 8080-8083, 8761, 8888, 9411

## Installation et lancement

### Option 1 — script automatique (Windows)

```powershell
cd driveeasy-backend
.\start-all.ps1          # compile, lance Zipkin puis les 6 services dans le bon ordre
.\stop-all.ps1           # arrête tout
```

Linux / macOS / Git Bash : `./start-all.sh` puis `./stop-all.sh`. Les journaux sont dans `logs/`.

### Option 2 — pas à pas (un terminal par service)

```bash
cd driveeasy-backend
mvn clean package -DskipTests                       # compile les 6 modules (ou .\mvnw.cmd clean package -DskipTests)
docker compose up -d                                # 1. Zipkin  -> http://localhost:9411

java -jar service-registry/target/service-registry-1.0.0.jar   # 2. Eureka -> http://localhost:8761
java -jar config-server/target/config-server-1.0.0.jar         # 3. Config Server -> http://localhost:8888/vehicule-service/default
java -jar vehicule-service/target/vehicule-service-1.0.0.jar   # 4. microservices métier
java -jar client-service/target/client-service-1.0.0.jar
java -jar location-service/target/location-service-1.0.0.jar
java -jar api-gateway/target/api-gateway-1.0.0.jar             # 5. Gateway -> http://localhost:8080
```

> Lancer les commandes depuis le dossier `driveeasy-backend` : le Config Server lit `./config-repo`.
> Depuis un IDE (IntelliJ / VS Code), lancer les classes `*Application` dans le même ordre.

Attendre ~30 s que tous les services apparaissent dans Eureka avant d'appeler la gateway.

### Vérification rapide

```bash
curl http://localhost:8080/api/vehicules
curl http://localhost:8080/api/clients/1/eligibilite
curl http://localhost:8080/api/locations/statistiques
```

Tests de bout en bout automatisés (23 scénarios : cas nominaux + erreurs 400/404/409, réservation,
départ, retour avec pénalité, annulation, statistiques) :

```bash
python scripts/test_api.py          # ou, sous Windows : .\scripts\test-api.ps1
```

Une collection Postman équivalente est fournie : `docs/DriveEasy.postman_collection.json`.

## Configuration centralisée (Spring Cloud Config)

Chaque service ne contient localement que son nom et `spring.config.import=configserver:http://localhost:8888`.
Tout le reste est externalisé dans **`config-repo/`** :

| Fichier | Contenu |
|---|---|
| `application.yml` | commun : Eureka, Actuator, **Zipkin** (`management.tracing.sampling.probability: 1.0`), logs avec traceId |
| `vehicule-service.yml` | port 8081, stockage, propriétés métier (`driveeasy.vehicule.*`) |
| `client-service.yml` | port 8082, âge minimum (21), données de démo |
| `location-service.yml` | port 8083, coefficients par catégorie, remises fidélité, coefficient de pénalité, seuils FIDELE/VIP |
| `location-service-prod.yml` | profil `prod` : échantillonnage 20 %, pas de données de démo |
| `application-dev.yml` | profil `dev` : logs DEBUG |
| `api-gateway.yml` | port 8080, routes `lb://`, CORS, seuil du filtre « requête lente » |

**Profils** : `java -jar location-service/target/location-service-1.0.0.jar --spring.profiles.active=prod`

**Dépôt GitHub** : pousser ce dépôt (ou le seul dossier `config-repo/`) sur GitHub puis lancer le Config Server
avec le profil `git` :

```bash
set CONFIG_PROFILE=git
set CONFIG_GIT_URI=https://github.com/<compte>/driveeasy-backend
java -jar config-server/target/config-server-1.0.0.jar
```

## API (via la gateway `http://localhost:8080/api`)

### vehicule-service — `/api/vehicules`
| Méthode | URI | Description |
|---|---|---|
| GET | `/vehicules?categorie=&carburant=&statut=&prixMax=&q=` | Liste filtrée |
| GET | `/vehicules/{id}` | Détail |
| POST | `/vehicules` | Création (201, 409 si immatriculation existante, 400 si prix ≤ 0 ou année future) |
| PUT | `/vehicules/{id}` | Modification |
| DELETE | `/vehicules/{id}` | Suppression (204 ; 409 si loué ou réservé) |
| PATCH | `/vehicules/{id}/statut` | Changement de statut (+ kilométrage) — utilisé par location-service |
| GET | `/vehicules/disponibles?dateDebut=&dateFin=` | Véhicules réservables sur une période |
| POST / DELETE | `/vehicules/{id}/reservations[/{locationId}]` | Blocage / libération d'une période (interne) |

### client-service — `/api/clients`
| Méthode | URI | Description |
|---|---|---|
| GET | `/clients?q=` (ou `nom=`, `cin=`, `email=`) | Recherche |
| GET | `/clients/{id}` | Détail |
| POST / PUT | `/clients`, `/clients/{id}` | Création / modification (409 si CIN, email ou permis déjà utilisé) |
| DELETE | `/clients/{id}` | Suppression (409 si location active — vérifié auprès de location-service) |
| GET | `/clients/{id}/eligibilite` | Âge ≥ 21 ans et permis valide |
| PATCH | `/clients/{id}/categorie` | Mise à jour de la fidélité |

### location-service — `/api/locations`
| Méthode | URI | Description |
|---|---|---|
| GET | `/locations?statut=&clientId=&vehiculeId=&dateDebut=&dateFin=&page=&size=` | Liste paginée |
| GET | `/locations/{id}` | Détail enrichi (client + véhicule agrégés) |
| POST | `/locations` | Réservation (vérifie éligibilité + disponibilité) |
| POST | `/locations/simulation` | Simulation du prix sans création |
| PATCH | `/locations/{id}/depart` | RESERVEE → EN_COURS, véhicule → LOUE |
| PATCH | `/locations/{id}/retour` | `{"kmRetour":…}` → TERMINEE, pénalités, véhicule → DISPONIBLE, fidélité |
| PATCH | `/locations/{id}/annulation` | RESERVEE → ANNULEE |
| GET | `/locations/statistiques` | CA (total, mois, 6 mois), taux d'occupation, top véhicules |

### Règles de gestion implémentées
- **Prix** = nbJours × prixJournalier × coefficient catégorie − remise fidélité (FIDELE 5 %, VIP 10 %).
  Coefficients : ECONOMIQUE 1.00, COMPACTE 1.05, BERLINE 1.10, SUV 1.20, UTILITAIRE 1.15, LUXE 1.35 (configurables).
- **Pénalité** = 1,5 × prix journalier × jours de retard.
- **Fidélité** : FIDELE après 5 locations terminées, VIP après 15 (calculé par location-service au retour).
- **Cohérence distribuée** : le véhicule est bloqué (planning) avant l'enregistrement de la location ; en cas
  d'échec, la réservation est compensée (libération). Départ / retour / annulation ne modifient l'état local
  qu'après succès de l'appel distant. Panne d'un service → **503** explicite.
- Codes HTTP : 200, 201 (+ en-tête Location), 204, 400 (validation détaillée champ par champ), 404, 409, 503, 500.

## Jeu de données de test

| Données | Détail |
|---|---|
| 12 véhicules | 6 catégories ; n°5 (Tucson) et n°7 (Classe C) **loués**, n°12 (Fiat 500) **en maintenance** |
| 10 clients | n°5 **VIP**, n°2 et n°9 **FIDELE**, n°7 **19 ans** (non éligible), n°8 **permis expiré**, n°10 permis expirant bientôt |
| 41 locations | 2 en cours (dont n°2 **en retard** → pénalité au retour), 2 réservées, 1 annulée, 36 terminées sur 6 mois |

Aucun compte utilisateur : l'application est destinée aux agents (pas d'authentification exigée).

## Traçage distribué (Zipkin)

Micrometer Tracing (Brave) + reporter Zipkin dans **tous** les services (gateway comprise), échantillonnage 1.0.
Le `RestClient` est construit avec `RestClientBuilderConfigurer` : le traceId est propagé entre services.
Exemple de requête traversant 3 services : `POST /api/locations` (gateway → location-service → client-service
→ vehicule-service). Ouvrir http://localhost:9411, « Run Query », puis cliquer sur la trace.

## Filtre personnalisé de la gateway

`RequestTracingGlobalFilter` : ajoute/propage `X-Request-Id`, mesure la durée et renvoie `X-Response-Time`,
journalise chaque requête (route, statut, durée) et signale les requêtes plus lentes que
`driveeasy.gateway.seuil-requete-lente-ms`.

## Tests

```bash
mvn test       # tests unitaires des règles tarifaires et des règles du parc
```

## Structure

```
driveeasy-backend/
├── pom.xml                      POM parent (Java 21, Spring Boot 3.5, Spring Cloud 2025.0)
├── config-repo/                 configuration externalisée (servie par le Config Server)
├── service-registry/            Eureka Server
├── config-server/               Config Server
├── api-gateway/                 Spring Cloud Gateway + filtre global
├── vehicule-service/            controller / service / repository / model / dto / exception / config
├── client-service/              … + client/ (appel location-service)
├── location-service/            … + client/ (appels client-service et vehicule-service)
├── docker-compose.yml           Zipkin
├── docs/                        collection Postman, captures
└── start-all.ps1 / start-all.sh / stop-all.*
```
