"""
Generateur du rapport DriveEasy (HTML -> PDF via Chromium/Playwright).
Usage : python build_report.py   (lit ../driveeasy-backend, ../driveeasy-frontend, captures/ et api-tests.json)
Les extraits de code sont lus directement dans les sources : le rapport reste fidele au code livre.
"""
import html, json, os, re, sys, asyncio, datetime

ICI = os.path.dirname(os.path.abspath(__file__))
RACINE = os.path.dirname(ICI)
BACK = os.path.join(RACINE, 'driveeasy-backend')
FRONT = os.path.join(RACINE, 'driveeasy-frontend')
CAPTURES = os.path.join(ICI, 'captures')
MERMAID = os.environ.get('MERMAID_JS', '/tmp/rapport/node_modules/mermaid/dist/mermaid.min.js')

ETUDIANT = os.environ.get('ETUDIANT', 'Mohamed SAYARI')
GROUPE = os.environ.get('GROUPE', 'LIM3 B/C')
DEPOT_BACK = os.environ.get('DEPOT_BACK', 'https://github.com/<compte>/driveeasy-backend')
DEPOT_FRONT = os.environ.get('DEPOT_FRONT', 'https://github.com/<compte>/driveeasy-frontend')

figures, tableaux, sections = [], [], []
compteur_fig = compteur_tab = 0


def lire(chemin):
    with open(os.path.join(RACINE, chemin), encoding='utf-8') as f:
        return f.read()


def extrait(chemin, debut, fin=None, max_lignes=60, occurrence=1):
    """Extrait les lignes de `chemin` entre la 1re ligne contenant `debut` et la n-ieme contenant `fin` (incluse)."""
    lignes = lire(chemin).splitlines()
    i = next(k for k, l in enumerate(lignes) if debut in l)
    j = i
    if fin:
        j = [k for k in range(i, len(lignes)) if fin in lignes[k]][occurrence - 1]
    else:
        j = min(len(lignes) - 1, i + max_lignes)
    bloc = lignes[i:j + 1]
    indent = min((len(l) - len(l.lstrip()) for l in bloc if l.strip()), default=0)
    return '\n'.join(l[indent:] for l in bloc)


def code(texte, langage='java', titre=None):
    t = f'<div class="code-titre">{html.escape(titre)}</div>' if titre else ''
    return f'{t}<pre class="code {langage}"><code>{html.escape(texte)}</code></pre>'


def figure(contenu, legende):
    global compteur_fig
    compteur_fig += 1
    figures.append((compteur_fig, legende))
    return f'<figure>{contenu}<figcaption>Figure {compteur_fig} – {html.escape(legende)}</figcaption></figure>'


def mermaid(src, legende):
    return figure(f'<div class="mermaid">{html.escape(src)}</div>', legende)


def capture(fichier, legende, largeur='100%'):
    chemin = os.path.join(CAPTURES, fichier)
    if os.path.exists(chemin):
        contenu = f'<img src="file://{chemin}" style="width:{largeur}" />'
    else:
        contenu = f'<div class="capture-manquante">Capture à insérer : {html.escape(fichier)}</div>'
    return figure(contenu, legende)


def tableau(entetes, lignes, legende):
    global compteur_tab
    compteur_tab += 1
    tableaux.append((compteur_tab, legende))
    th = ''.join(f'<th>{h}</th>' for h in entetes)
    tr = ''.join('<tr>' + ''.join(f'<td>{c}</td>' for c in l) + '</tr>' for l in lignes)
    return f'<table class="tab"><caption>Tableau {compteur_tab} – {html.escape(legende)}</caption><thead><tr>{th}</tr></thead><tbody>{tr}</tbody></table>'


def h(niveau, titre):
    num = '.'.join(str(n) for n in h.compteurs[:niveau]) if niveau else ''
    h.compteurs[niveau - 1] += 1
    for k in range(niveau, 4):
        h.compteurs[k] = 0
    num = '.'.join(str(n) for n in h.compteurs[:niveau])
    ident = 's' + num.replace('.', '-')
    sections.append((niveau, num, titre, ident))
    saut = ' class="nouvelle-page"' if niveau == 1 else ''
    return f'<h{niveau} id="{ident}"{saut}>{num}. {html.escape(titre)}</h{niveau}>'


h.compteurs = [0, 0, 0, 0]


def api_tests():
    chemin = os.path.join(ICI, 'api-tests.json')
    if not os.path.exists(chemin):
        return '<div class="capture-manquante">Résultats des tests d\'API à générer (scripts/test-api.ps1)</div>'
    tests = json.load(open(chemin, encoding='utf-8-sig'))
    blocs = []
    for t in tests:
        corps = t.get('reponse', '')
        try:
            corps = json.dumps(json.loads(corps), ensure_ascii=False, indent=2)
        except Exception:
            pass
        lignes = corps.splitlines()
        if len(lignes) > 9:
            corps = '\n'.join(lignes[:9]) + '\n  …'
        requete = f"{t['methode']} {t['url']}"
        if t.get('corps'):
            requete += '\n' + t['corps']
        attendu = t.get('attendu')
        ok = '✓' if str(t['statut']) == str(attendu) else '✗'
        blocs.append(f'''<div class="api-test"><div class="api-titre"><span>{html.escape(t["nom"])}</span>
<span class="statut s{str(t["statut"])[0]}">HTTP {t["statut"]} {ok}</span></div>
<pre class="code"><code><b>&gt; {html.escape(requete)}</b>\n{html.escape(corps)}</code></pre></div>''')
    return '<div class="api-grille">' + ''.join(blocs) + '</div>'


# ============================================================================ CONTENU
C = []
C.append(h(1, 'Introduction et contexte'))
C.append(h(2, 'Présentation du sujet'))
C.append('''<p>La société <b>DriveEasy</b> loue des voitures de courte et moyenne durée. Son application monolithique
vieillissante ne permet plus de faire évoluer séparément la gestion du parc, des clients et des locations, ni de
supporter la montée en charge. Ce projet réalise un <b>prototype fonctionnel</b> reposant sur une
<b>architecture microservices</b> Spring Boot / Spring Cloud, complété (bonus) par une application web
<b>Angular 22</b> destinée aux agents d'agence.</p>''')
C.append(h(2, 'Problématique'))
C.append('''<p>Comment découper le système d'information d'un loueur en services autonomes, découvrables dynamiquement,
configurés de façon centralisée et observables, tout en garantissant la cohérence d'opérations métier qui
traversent plusieurs services (réservation, départ, retour) en l'absence de transaction distribuée ?</p>''')
C.append(h(2, 'Objectifs'))
C.append('''<ul>
<li><b>Métier</b> : gérer le parc (caractéristiques, statut, kilométrage), les clients et la validité de leur permis,
le cycle complet d'une location ; calculer automatiquement les prix (tarif journalier, coefficient de catégorie,
remise fidélité, pénalités de retard) ; fournir des indicateurs (taux d'occupation, chiffre d'affaires, top véhicules).</li>
<li><b>Techniques</b> : Service Registry (Eureka), configuration centralisée (Config Server), API Gateway avec load
balancing, communication synchrone inter-services avec gestion d'erreurs distribuées, traçage distribué (Zipkin),
frontend Angular consommant uniquement la gateway.</li>
</ul>''')
C.append(h(2, 'Périmètre fonctionnel'))
C.append(tableau(['Domaine', 'Fonctionnalités réalisées'], [
    ['Parc automobile', 'CRUD véhicules, filtres (catégorie, carburant, statut, prix max), statut DISPONIBLE / LOUE / MAINTENANCE, disponibilité sur une période, planning de réservations'],
    ['Clients', 'CRUD clients, recherche nom / CIN / email, unicité CIN-email-permis, éligibilité (≥ 21 ans, permis valide), catégorie de fidélité'],
    ['Locations', 'Réservation avec contrôles inter-services, simulation de prix, départ, retour avec pénalités, annulation, liste paginée filtrable, détail enrichi, statistiques'],
    ['Infrastructure', 'Eureka, Config Server (profils, dépôt Git), Gateway (routes lb://, CORS, filtre personnalisé), Zipkin, Actuator'],
    ['Frontend (bonus)', '8 écrans Angular 22 : tableau de bord, catalogue, formulaire véhicule, clients, fiche client, assistant de location, liste des locations, retour imprimable'],
], 'Périmètre fonctionnel du prototype'))

# ---------------------------------------------------------------- Analyse
C.append(h(1, 'Analyse et conception'))
C.append(h(2, 'Acteurs'))
C.append(tableau(['Acteur', 'Rôle'], [
    ['Agent d\'agence', 'Utilisateur principal : gère véhicules et clients, crée et suit les locations, consulte les indicateurs'],
    ['Responsable d\'agence', 'Consulte le tableau de bord (CA, taux d\'occupation, véhicules les plus loués)'],
    ['location-service (acteur système)', 'Pilote automatiquement le statut des véhicules et la fidélité des clients'],
], 'Acteurs du système'))
C.append(h(2, 'Besoins fonctionnels'))
C.append(mermaid('''flowchart LR
  A((Agent d'agence))
  R((Responsable))
  subgraph DriveEasy
    UC1([Gérer le parc de véhicules])
    UC2([Changer le statut / maintenance])
    UC3([Gérer les clients])
    UC4([Vérifier l'éligibilité])
    UC5([Simuler le prix d'une location])
    UC6([Réserver un véhicule])
    UC7([Enregistrer le départ])
    UC8([Enregistrer le retour et les pénalités])
    UC9([Annuler une réservation])
    UC10([Consulter les statistiques])
  end
  A --- UC1 & UC2 & UC3 & UC5 & UC6 & UC7 & UC8 & UC9
  R --- UC10
  UC6 -. include .-> UC4
  UC6 -. include .-> UC5
''', 'Diagramme de cas d\'utilisation'))
C.append(h(2, 'Besoins non fonctionnels'))
C.append('''<ul>
<li><b>Scalabilité et découplage</b> : un microservice par domaine, déployable et extensible indépendamment.</li>
<li><b>Découverte dynamique</b> : aucune adresse de service codée en dur (Eureka + <code>lb://</code>).</li>
<li><b>Configuration externalisée</b> : ports, Eureka, Zipkin et règles métier modifiables sans recompilation.</li>
<li><b>Observabilité</b> : Actuator (health, info) sur chaque service, traçage distribué Zipkin à 100 %.</li>
<li><b>Robustesse</b> : délais d'attente sur les appels distants, codes HTTP explicites (400, 404, 409, 503), compensation.</li>
<li><b>Contrainte</b> : stockage exclusivement en mémoire (aucune base de données).</li>
<li><b>Ergonomie</b> : interface responsive, validations immédiates, messages d'erreur métier lisibles.</li>
</ul>''')
C.append(h(2, 'Diagrammes de classes par microservice'))
C.append(mermaid('''classDiagram
  direction LR
  class Vehicule {
    Long id
    String immatriculation
    String marque
    String modele
    Integer annee
    String couleur
    Categorie categorie
    Carburant carburant
    Boite boite
    Integer nbPlaces
    Integer kilometrage
    BigDecimal prixJournalier
    StatutVehicule statut
    String imageUrl
  }
  class PeriodeReservation {
    <<record>>
    Long locationId
    LocalDate dateDebut
    LocalDate dateFin
    chevauche(debut, fin) boolean
  }
  class StatutVehicule { <<enum>> DISPONIBLE LOUE MAINTENANCE }
  class Categorie { <<enum>> ECONOMIQUE COMPACTE BERLINE SUV UTILITAIRE LUXE }
  class Carburant { <<enum>> ESSENCE DIESEL HYBRIDE ELECTRIQUE GPL }
  class Boite { <<enum>> MANUELLE AUTOMATIQUE }
  Vehicule "1" *-- "0..*" PeriodeReservation : reservations
  Vehicule --> StatutVehicule
  Vehicule --> Categorie
  Vehicule --> Carburant
  Vehicule --> Boite
''', 'Diagramme de classes – vehicule-service'))
C.append(mermaid('''classDiagram
  direction LR
  class Client {
    Long id
    String cin
    String nom
    String prenom
    LocalDate dateNaissance
    String email
    String telephone
    String adresse
    String numeroPermis
    LocalDate dateExpirationPermis
    CategorieClient categorieClient
    LocalDate dateInscription
  }
  class CategorieClient { <<enum>> STANDARD FIDELE VIP }
  class EligibiliteResponse {
    <<record DTO>>
    boolean eligible
    int age
    boolean ageSuffisant
    boolean permisValide
    List~String~ motifs
  }
  Client --> CategorieClient
  Client ..> EligibiliteResponse : calcule
''', 'Diagramme de classes – client-service'))
C.append(mermaid('''classDiagram
  direction LR
  class Location {
    Long id
    Long clientId
    Long vehiculeId
    LocalDate dateDebut
    LocalDate dateFinPrevue
    LocalDate dateRetourEffectif
    Integer kmDepart
    Integer kmRetour
    BigDecimal prixJournalierApplique
    BigDecimal coefficientCategorie
    BigDecimal tauxRemise
    BigDecimal montantRemise
    BigDecimal montantBase
    BigDecimal penalite
    BigDecimal montantTotal
    StatutLocation statut
    LocalDateTime dateCreation
    isActive() boolean
  }
  class StatutLocation { <<enum>> RESERVEE EN_COURS TERMINEE ANNULEE }
  class TarificationService {
    calculer(nbJours, prix, categorieVehicule, categorieClient) Devis
    penalite(prix, joursRetard) BigDecimal
    categoriePour(nbTerminees) String
  }
  class ClientServiceClient
  class VehiculeServiceClient
  class LocationService
  Location --> StatutLocation
  LocationService --> TarificationService
  LocationService --> ClientServiceClient : REST
  LocationService --> VehiculeServiceClient : REST
''', 'Diagramme de classes – location-service'))
C.append(mermaid('''stateDiagram-v2
  [*] --> RESERVEE : POST /locations
  RESERVEE --> EN_COURS : PATCH depart (véhicule → LOUE)
  RESERVEE --> ANNULEE : PATCH annulation (période libérée)
  EN_COURS --> TERMINEE : PATCH retour (pénalités, véhicule → DISPONIBLE)
  TERMINEE --> [*]
  ANNULEE --> [*]
''', 'Cycle de vie d\'une location'))
C.append(h(2, 'Justification des choix techniques'))
C.append(tableau(['Choix', 'Justification'], [
    ['Java 21, Spring Boot 3.5, Spring Cloud 2025.0', 'Versions imposées (Java 21, Boot 3.x) ; Spring Cloud 2025.0 est le train compatible avec Boot 3.5'],
    ['Projet Maven multi-modules', 'Un POM parent centralise les versions ; chaque service reste un jar exécutable indépendant'],
    ['Couches controller / service / repository / dto', 'Séparation claire des responsabilités ; les entités ne sont jamais exposées directement (records DTO)'],
    ['ConcurrentHashMap + AtomicLong', 'Stockage en RAM imposé ; structures thread-safe ; méthodes synchronized pour les règles d\'unicité et de réservation'],
    ['RestClient @LoadBalanced', 'Client HTTP synchrone moderne de Spring 6, résolution des noms via Eureka, instrumenté par Micrometer (propagation du traceId)'],
    ['Planning de réservations dans vehicule-service', 'Répond à « disponible sur une période » sans appel croisé et rend la réservation atomique (évite la double réservation)'],
    ['Compensation (saga simplifiée)', 'Pas de transaction distribuée : chaque étape distante réussie est annulée si l\'étape suivante échoue'],
    ['Micrometer Tracing + Brave + Zipkin', 'Standard Spring Boot 3 (Sleuth n\'existe plus) ; traces envoyées à Zipkin, traceId dans les logs'],
    ['Angular 22 standalone, signals, zoneless', 'Approche recommandée par Angular : moins de code, détection de changements fine, lazy loading par écran'],
], 'Choix techniques'))

# ---------------------------------------------------------------- Architecture
C.append(h(1, 'Architecture globale'))
C.append(h(2, 'Schéma d\'architecture'))
C.append(figure(open(os.path.join(ICI, 'architecture.svg'), encoding='utf-8').read(), 'Architecture microservices de DriveEasy'))
C.append(h(2, 'Rôle des composants, ports et dépendances'))
C.append(tableau(['Composant', 'Port', 'Rôle', 'Dépend de'], [
    ['service-registry', '8761', 'Annuaire Eureka (ne s\'enregistre pas lui-même)', '—'],
    ['config-server', '8888', 'Sert config-repo/*.yml (profil native) ou un dépôt GitHub (profil git)', 'Eureka (enregistrement)'],
    ['vehicule-service', '8081', 'Parc, statuts, planning de disponibilité', 'Config Server, Eureka, Zipkin'],
    ['client-service', '8082', 'Clients, éligibilité, fidélité', 'Config Server, Eureka, Zipkin, location-service'],
    ['location-service', '8083', 'Locations, tarification, statistiques, orchestration', 'Config Server, Eureka, Zipkin, client-service, vehicule-service'],
    ['api-gateway', '8080', 'Point d\'entrée unique, routage, CORS, filtre personnalisé', 'Config Server, Eureka, Zipkin'],
    ['Zipkin', '9411', 'Collecte et visualisation des traces', 'Docker'],
    ['Frontend Angular', '4200', 'Interface des agents', 'api-gateway uniquement'],
], 'Composants, ports et dépendances'))
C.append('<p><b>Ordre de démarrage</b> : Zipkin → Eureka → Config Server → vehicule / client / location-service → API Gateway → frontend. Le script <code>start-all.ps1</code> automatise cet ordre en attendant que chaque service réponde sur <code>/actuator/health</code>.</p>')
C.append(h(2, 'Flux de communication : création d\'une réservation'))
C.append(mermaid('''sequenceDiagram
  autonumber
  participant UI as Angular
  participant GW as api-gateway
  participant LS as location-service
  participant CS as client-service
  participant VS as vehicule-service
  UI->>GW: POST /api/locations
  GW->>LS: POST /locations (lb://, StripPrefix=1)
  LS->>CS: GET /clients/{id}/eligibilite
  CS-->>LS: eligible=true, categorie=VIP
  LS->>VS: GET /vehicules/{id}
  VS-->>LS: prixJournalier, categorie, statut=DISPONIBLE
  Note over LS: prix = jours × prix × coef − remise
  LS->>VS: POST /vehicules/{id}/reservations
  alt période libre
    VS-->>LS: 200 (période bloquée)
    LS->>LS: enregistrement RESERVEE
    alt échec de l'enregistrement
      LS->>VS: DELETE /reservations/{locationId} (compensation)
    end
    LS-->>GW: 201 Created
  else déjà réservé / panne
    VS-->>LS: 409 ou délai dépassé
    LS-->>GW: 409 / 503 (+ libération par sécurité)
  end
  GW-->>UI: réponse + X-Response-Time
''', 'Séquence de création d\'une réservation avec compensation'))

# ---------------------------------------------------------------- Backend
C.append(h(1, 'Réalisation backend'))
C.append(h(2, 'Service Registry (Eureka)'))
C.append('<p>Application annotée <code>@EnableEurekaServer</code> sur le port 8761. L\'auto-enregistrement est désactivé ; l\'auto-préservation est désactivée en développement pour voir rapidement les instances arrêtées.</p>')
C.append(code(extrait('driveeasy-backend/service-registry/src/main/java/tn/lim3/driveeasy/registry/ServiceRegistryApplication.java', '@SpringBootApplication', 'public class'), 'java', 'ServiceRegistryApplication.java'))
C.append(code(extrait('driveeasy-backend/service-registry/src/main/resources/application.yml', 'eureka:', 'eviction-interval'), 'yaml', 'service-registry/application.yml'))
C.append(capture('eureka.png', 'Tableau de bord Eureka : les 5 instances enregistrées (gateway, config-server, 3 microservices)'))
C.append(h(2, 'Config Server'))
C.append('<p>Le Config Server (<code>@EnableConfigServer</code>, port 8888) sert un fichier par service depuis <code>config-repo/</code> (profil <code>native</code>, par défaut) ou depuis GitHub (profil <code>git</code>, URI dans <code>CONFIG_GIT_URI</code>). Les services n\'ont localement que leur nom et l\'import du Config Server.</p>')
C.append(code(lire('driveeasy-backend/vehicule-service/src/main/resources/application.yml'), 'yaml', 'vehicule-service/src/main/resources/application.yml'))
C.append(code(extrait('driveeasy-backend/config-repo/location-service.yml', 'server:', 'LUXE'), 'yaml', 'config-repo/location-service.yml – propriétés métier externalisées'))
C.append(tableau(['Fichier', 'Propriétés externalisées'], [
    ['application.yml', 'Eureka (defaultZone, instance-id), Actuator, Zipkin (endpoint, probabilité 1.0), format des logs avec traceId'],
    ['vehicule-service.yml / client-service.yml / location-service.yml', 'port, « datasource » (stockage mémoire), propriétés métier driveeasy.*'],
    ['location-service-prod.yml', 'profil prod : échantillonnage 20 %, pas de données de démonstration'],
    ['application-dev.yml', 'profil dev : niveau DEBUG pour le code DriveEasy'],
    ['api-gateway.yml', 'port, routes lb://, CORS, seuil de requête lente'],
], 'Configuration centralisée'))
C.append(capture('config-server.png', 'Configuration de location-service servie par le Config Server (/location-service/default)'))
C.append(h(2, 'Microservice vehicule-service'))
C.append('<p>Architecture en couches : <code>VehiculeController</code> (REST) → <code>VehiculeService</code> (règles) → <code>VehiculeRepository</code> (RAM). Les requêtes/réponses sont des <i>records</i> DTO validés par Bean Validation. Un planning de <code>PeriodeReservation</code> permet de calculer la disponibilité sur une période et de bloquer atomiquement un véhicule.</p>')
C.append(code(extrait('driveeasy-backend/vehicule-service/src/main/java/tn/lim3/driveeasy/vehicule/service/VehiculeService.java', 'public synchronized Vehicule reserver', 'return repository.save(v);\n'.strip()), 'java', 'VehiculeService.reserver – réservation atomique'))
C.append(code(extrait('driveeasy-backend/vehicule-service/src/main/java/tn/lim3/driveeasy/vehicule/exception/GlobalExceptionHandler.java', '@ExceptionHandler(ResourceNotFoundException.class)', 'return build(HttpStatus.BAD_REQUEST, "Donnees invalides"'), 'java', 'GlobalExceptionHandler – traduction des exceptions en codes HTTP'))
C.append(h(2, 'Microservice client-service'))
C.append('<p>Gère l\'identité des clients et l\'unicité du CIN, de l\'email et du numéro de permis (409). L\'endpoint d\'éligibilité vérifie l\'âge minimum (21 ans, configurable) et la validité du permis, et renvoie les motifs de refus. La suppression interroge location-service pour refuser la suppression d\'un client ayant une location active.</p>')
C.append(code(extrait('driveeasy-backend/client-service/src/main/java/tn/lim3/driveeasy/client/service/ClientService.java', 'public EligibiliteResponse eligibilite', 'return new EligibiliteResponse'), 'java', 'ClientService.eligibilite'))
C.append(h(2, 'Microservice location-service'))
C.append('<p>Cœur métier et orchestrateur. La tarification est isolée dans <code>TarificationService</code> (testée unitairement) et paramétrée par le Config Server.</p>')
C.append(code(extrait('driveeasy-backend/location-service/src/main/java/tn/lim3/driveeasy/location/service/TarificationService.java', 'public Devis calculer', 'return new Devis'), 'java', 'TarificationService.calculer – prix = jours × prix × coefficient − remise'))
C.append(code(extrait('driveeasy-backend/location-service/src/main/java/tn/lim3/driveeasy/location/service/LocationService.java', 'Long id = repository.nextId();', 'throw e;', occurrence=2), 'java', 'LocationService.creer – blocage du véhicule puis compensation en cas d\'échec'))
C.append(code(extrait('driveeasy-backend/location-service/src/main/java/tn/lim3/driveeasy/location/service/LocationService.java', 'public synchronized Location retour', 'mettreAJourFidelite(l.getClientId());'), 'java', 'LocationService.retour – pénalités, véhicule DISPONIBLE, fidélité'))
C.append(h(2, 'Communication inter-services et gestion des erreurs distribuées'))
C.append(code(extrait('driveeasy-backend/location-service/src/main/java/tn/lim3/driveeasy/location/config/RestClientConfig.java', '@Configuration', None, 14), 'java', 'RestClient @LoadBalanced instrumenté (Micrometer)'))
C.append(code(extrait('driveeasy-backend/location-service/src/main/java/tn/lim3/driveeasy/location/client/RemoteErrorHelper.java', 'static RuntimeException traduire', 'return new ServiceUnavailableException("Erreur'), 'java', 'Traduction des erreurs distantes (404 / 409 / 400 / 503)'))
C.append(tableau(['Situation', 'Comportement'], [
    ['Client non éligible', '400 avec les motifs (âge, permis expiré)'],
    ['Véhicule loué / en maintenance / déjà réservé', '409 (contrôle local puis contrôle atomique dans vehicule-service)'],
    ['Service distant arrêté ou délai dépassé (3 s / 5 s)', '503 explicite, état local inchangé, libération de sécurité de la période'],
    ['Échec après blocage du véhicule', 'Compensation : DELETE /vehicules/{id}/reservations/{locationId}'],
    ['client-service indisponible au retour', 'Location terminée quand même ; mise à jour de fidélité reportée (journalisée)'],
    ['Statistiques avec vehicule-service arrêté', 'Indicateurs partiels + avertissement (dégradation gracieuse)'],
], 'Gestion des erreurs distribuées'))
C.append(h(2, 'Tests de l\'API via la gateway'))
C.append('<p>Les requêtes ci-dessous ont été exécutées sur le système démarré, à travers la gateway (<code>http://localhost:8080/api</code>), avec le script <code>scripts/test-api.ps1</code>. Une collection Postman équivalente est fournie (<code>docs/DriveEasy.postman_collection.json</code>).</p>')
C.append(figure(api_tests(), 'Requêtes de test et réponses obtenues (cas nominaux et cas d\'erreur)'))
C.append(h(2, 'API Gateway'))
C.append('<p>Spring Cloud Gateway (WebFlux) expose le port 8080, s\'enregistre dans Eureka et route avec <code>lb://</code> (load balancing côté client). Les routes, le filtre <code>StripPrefix=1</code> et le CORS pour <code>http://localhost:4200</code> sont dans le Config Server.</p>')
C.append(code(extrait('driveeasy-backend/config-repo/api-gateway.yml', 'routes:', 'max-age'), 'yaml', 'config-repo/api-gateway.yml – routes et CORS'))
C.append(code(extrait('driveeasy-backend/api-gateway/src/main/java/tn/lim3/driveeasy/gateway/filter/RequestTracingGlobalFilter.java', 'public Mono<Void> filter', 'log.info(message, args);'), 'java', 'Filtre global personnalisé : X-Request-Id, X-Response-Time, journalisation'))
C.append(h(2, 'Monitoring et traçage distribué (Zipkin)'))
C.append('<p>Zipkin est lancé par <code>docker compose up -d</code> (image <code>openzipkin/zipkin</code>, port 9411). Tous les services embarquent <code>micrometer-tracing-bridge-brave</code> et <code>zipkin-reporter-brave</code> ; la probabilité d\'échantillonnage vaut 1.0 en développement (config commune). Le traceId est propagé par la gateway et par le RestClient instrumenté, et apparaît dans chaque ligne de log.</p>')
C.append(code(extrait('driveeasy-backend/config-repo/application.yml', 'tracing:', 'spans}'), 'yaml', 'Configuration Zipkin commune'))
C.append(capture('zipkin-traces.png', 'Zipkin : liste des traces collectées'))
C.append(capture('zipkin-trace.png', 'Zipkin : trace d\'une réservation traversant gateway → location-service → client-service → vehicule-service'))
C.append(f'<div id="analyse-zipkin">{open(os.path.join(ICI, "analyse-zipkin.html"), encoding="utf-8").read() if os.path.exists(os.path.join(ICI, "analyse-zipkin.html")) else "<p class=capture-manquante>Analyse des temps de réponse à compléter après la capture des traces.</p>"}</div>')

# ---------------------------------------------------------------- Frontend
C.append(h(1, 'Réalisation frontend (bonus)'))
C.append(h(2, 'Architecture de l\'application Angular'))
C.append('<p>Application Angular 22 générée avec Angular CLI : composants <b>standalone</b>, <b>signals</b>, nouveau control flow (<code>@if</code>, <code>@for</code>, <code>@switch</code>), application <b>zoneless</b>, formulaires réactifs typés et chargement à la demande de chaque écran. Le frontend ne connaît que l\'URL de la gateway (<code>environment.apiUrl</code>).</p>')
C.append(code('''src/app/
├── app.ts / app.html / app.css    coquille (menu, barre de chargement, notifications)
├── app.routes.ts                  routes lazy (loadComponent) + guards
├── app.config.ts                  provideHttpClient(withInterceptors), router, locale fr
├── core/
│   ├── models/                    vehicule.ts, client.ts, location.ts
│   ├── services/                  vehicule / client / location.service.ts, notification, loading
│   ├── interceptors/              loading.interceptor.ts, error.interceptor.ts
│   └── guards/                    unsaved-changes.guard.ts, id-param.guard.ts
├── shared/                        pagination, chart (Chart.js), pipes libelle et tnd
└── pages/                         dashboard/, vehicules/, clients/, locations/''', 'text', 'Structure des dossiers'))
C.append(h(2, 'Services, routage, guards et intercepteurs'))
C.append(code(extrait('driveeasy-frontend/src/app/core/services/location.service.ts', '@Injectable', None, 22), 'typescript', 'LocationService – accès à /api/locations via la gateway'))
C.append(code(extrait('driveeasy-frontend/src/app/app.routes.ts', "path: 'vehicules/:id/edit'", 'canActivate: [idParamGuard], canDeactivate'), 'typescript', 'Extrait du routage : lazy loading et guards'))
C.append(code(extrait('driveeasy-frontend/src/app/core/interceptors/error.interceptor.ts', 'export const errorInterceptor', 'return throwError'), 'typescript', 'Intercepteur d\'erreurs (messages ApiError)'))
C.append(tableau(['Élément', 'Rôle'], [
    ['loadingInterceptor', 'Compte les requêtes en cours et affiche une barre de progression globale'],
    ['errorInterceptor', 'Transforme les erreurs ApiError (message, détails de validation) en notification lisible ; signale un serveur injoignable'],
    ['unsavedChangesGuard (canDeactivate)', 'Demande confirmation avant de quitter un formulaire modifié non enregistré'],
    ['idParamGuard (canActivate)', 'Refuse un identifiant de route non numérique et redirige vers la liste'],
], 'Intercepteurs et guards'))
C.append(h(2, 'Interfaces'))
ECRANS = [
    ('front-dashboard.png', 'Tableau de bord (/dashboard) : véhicules disponibles / loués / en maintenance, CA du mois, locations en cours, graphiques'),
    ('front-vehicules.png', 'Catalogue des véhicules (/vehicules) : cartes avec image, filtres, tri, pagination'),
    ('front-vehicule-form.png', 'Formulaire véhicule (/vehicules/new) : validations réactives (prix strictement positif, champs obligatoires)'),
    ('front-clients.png', 'Liste des clients (/clients) : recherche, badges de catégorie, alerte permis expiré'),
    ('front-client-detail.png', 'Fiche client (/clients/:id) : informations, éligibilité, historique des locations'),
    ('front-location-new-1.png', 'Nouvelle location – étape 1 : choix du client et contrôle d\'éligibilité'),
    ('front-location-new-3.png', 'Nouvelle location – étape 3 : véhicules disponibles sur la période'),
    ('front-location-new-4.png', 'Nouvelle location – étape 4 : simulation détaillée du prix'),
    ('front-location-new-5.png', 'Nouvelle location – étape 5 : réservation confirmée'),
    ('front-locations.png', 'Liste des locations (/locations) : filtres, pagination, actions départ / retour / annulation'),
    ('front-retour.png', 'Retour de véhicule (/locations/:id/retour) : saisie du kilométrage et aperçu des pénalités'),
    ('front-recapitulatif.png', 'Récapitulatif final imprimable après le retour (pénalité de retard incluse)'),
]
for f, l in ECRANS:
    C.append(capture(f, l))

# ---------------------------------------------------------------- Conclusion
C.append(h(1, 'Conclusion et perspectives'))
C.append(h(2, 'Bilan'))
C.append('''<p>Le prototype couvre l'ensemble du cahier des charges : six composants Spring Cloud (registre, configuration,
passerelle, trois services métier), le traçage Zipkin dans tous les services, toutes les routes et règles de gestion
demandées, et une application Angular 22 de huit écrans. La cohérence des opérations multi-services est assurée sans
transaction distribuée grâce au blocage atomique du véhicule et à la compensation.</p>''')
C.append(h(2, 'Limites actuelles'))
C.append('''<ul>
<li>Stockage en mémoire : les données sont perdues à l'arrêt d'un service (contrainte du sujet) et ne sont pas partagées entre plusieurs instances d'un même service.</li>
<li>Pas d'authentification des agents (non demandée).</li>
<li>Compensation synchrone « best effort » : si la compensation elle-même échoue, l'incohérence est seulement journalisée.</li>
<li>Pas de disjoncteur (circuit breaker) ni de nouvelle tentative automatique.</li>
</ul>''')
C.append(h(2, 'Améliorations possibles'))
C.append('''<ul>
<li>Persistance (PostgreSQL / MongoDB) avec une base par service.</li>
<li>Resilience4j (circuit breaker, retry, time limiter) via Spring Cloud Circuit Breaker.</li>
<li>Communication asynchrone (Kafka / RabbitMQ) pour la fidélité et une saga orchestrée avec journal de compensation.</li>
<li>Sécurité : Spring Security + OAuth2 / Keycloak au niveau de la gateway, rôles agent / responsable.</li>
<li>Conteneurisation de tous les services (Dockerfile + docker-compose complet) et déploiement Kubernetes.</li>
<li>Tests d'intégration (Testcontainers, WireMock) et pipeline CI GitHub Actions.</li>
</ul>''')

C.append(h(1, 'Annexes'))
C.append(h(2, 'Dépôts GitHub'))
C.append(f'<ul><li>Backend : <a href="{DEPOT_BACK}">{DEPOT_BACK}</a></li><li>Frontend : <a href="{DEPOT_FRONT}">{DEPOT_FRONT}</a></li></ul>')
C.append(h(2, 'Lancement rapide'))
C.append(code('''# Option 1 - tout sous Docker (seul Docker Desktop est requis)
cd driveeasy-backend
docker compose -f docker-compose.full.yml build
docker compose -f docker-compose.full.yml up -d      # http://localhost:4200

# Option 2 - JDK 21 + Node.js installes
cd driveeasy-backend
.\\start-all.ps1              # Zipkin + 6 services dans l'ordre (Maven Wrapper si Maven absent)
cd ..\\driveeasy-frontend
npm install
npm start                    # http://localhost:4200''', 'text'))
C.append(h(2, 'Webographie'))
C.append('''<ul>
<li>Spring Boot – https://docs.spring.io/spring-boot/</li>
<li>Spring Cloud Netflix (Eureka) – https://docs.spring.io/spring-cloud-netflix/</li>
<li>Spring Cloud Config – https://docs.spring.io/spring-cloud-config/</li>
<li>Spring Cloud Gateway – https://docs.spring.io/spring-cloud-gateway/</li>
<li>Micrometer Tracing – https://docs.micrometer.io/tracing/</li>
<li>Zipkin – https://zipkin.io/</li>
<li>Angular – https://angular.dev/</li>
<li>Chart.js – https://www.chartjs.org/</li>
</ul>''')

# ============================================================================ ASSEMBLAGE
corps = '\n'.join(C)
toc = ''.join(f'<li class="n{n}"><a href="#{i}">{num}. {html.escape(t)}</a></li>' for n, num, t, i in sections if n <= 2)
tdf = ''.join(f'<li>Figure {n} – {html.escape(l)}</li>' for n, l in figures)
tdt = ''.join(f'<li>Tableau {n} – {html.escape(l)}</li>' for n, l in tableaux)
annee = datetime.date.today().year
annee_univ = f'{annee - 1}-{annee}' if datetime.date.today().month < 9 else f'{annee}-{annee + 1}'

DOC = f'''<!DOCTYPE html><html lang="fr"><head><meta charset="utf-8"><title>Rapport DriveEasy</title>
<style>
@page {{ size: A4; margin: 18mm 16mm 18mm 16mm; }}
body {{ font-family: "Segoe UI", "DejaVu Sans", Arial, sans-serif; font-size: 10.5pt; color: #1e293b; line-height: 1.5; }}
h1 {{ font-size: 19pt; color: #0f172a; border-bottom: 3px solid #e4572e; padding-bottom: 6px; margin-top: 0; }}
h2 {{ page-break-after: avoid; font-size: 13.5pt; color: #0f172a; margin: 18px 0 8px; }}
.nouvelle-page {{ page-break-before: always; }}
p, li {{ text-align: justify; }}
code {{ font-family: Consolas, "DejaVu Sans Mono", monospace; font-size: 9pt; background: #f1f5f9; padding: 0 3px; border-radius: 3px; }}
pre.code {{ background: #0f172a; color: #e2e8f0; padding: 10px 12px; border-radius: 6px; font-size: 8pt; line-height: 1.35; white-space: pre-wrap; page-break-inside: avoid; margin: 0 0 12px; }}
pre.code code {{ background: none; color: inherit; padding: 0; font-size: 8pt; }}
pre, table, figure {{ break-inside: avoid; }}
.code-titre {{ page-break-after: avoid;  font-size: 8.5pt; color: #64748b; margin: 10px 0 2px; font-weight: 600; }}
figure {{ margin: 12px 0 16px; text-align: center; page-break-inside: avoid; }}
figure img {{ border: 1px solid #cbd5e1; border-radius: 4px; max-height: 215mm; object-fit: contain; }}
figcaption, caption {{ font-size: 9pt; color: #475569; font-style: italic; margin-top: 5px; }}
caption {{ caption-side: bottom; padding-top: 4px; }}
table.tab {{ width: 100%; border-collapse: collapse; margin: 10px 0 16px; font-size: 9.5pt; page-break-inside: avoid; }}
table.tab th {{ background: #0f172a; color: #fff; text-align: left; padding: 5px 8px; }}
table.tab td {{ border-bottom: 1px solid #e2e8f0; padding: 5px 8px; vertical-align: top; }}
.mermaid {{ display: flex; justify-content: center; }}
.capture-manquante {{ border: 2px dashed #e4572e; color: #b91c1c; padding: 40px; font-weight: 600; }}
pre.code code b {{ color: #fbbf24; }}
.api-grille {{ display: grid; grid-template-columns: 1fr 1fr; gap: 6px 12px; }}
.api-grille pre.code {{ font-size: 7pt; }}
.api-test {{ text-align: left; margin-bottom: 8px; page-break-inside: avoid; }}
.api-titre {{ display: flex; justify-content: space-between; font-weight: 600; font-size: 9.5pt; margin-bottom: 2px; }}
.statut {{ font-size: 8.5pt; padding: 1px 8px; border-radius: 10px; }}
.s2 {{ background: #dcfce7; color: #166534; }} .s4 {{ background: #fef3c7; color: #92400e; }} .s5 {{ background: #fee2e2; color: #991b1b; }}
.garde {{ height: 255mm; display: flex; flex-direction: column; justify-content: space-between; text-align: center; }}
.garde .bandeau {{ background: #0f172a; color: #fff; padding: 18px; border-radius: 8px; }}
.garde h1 {{ border: none; font-size: 30pt; margin: 0; color: #e4572e; }}
.garde .sous {{ font-size: 15pt; color: #0f172a; }}
.garde table {{ margin: 0 auto; font-size: 11.5pt; text-align: left; border-collapse: collapse; }}
.garde td {{ padding: 5px 14px; }} .garde td:first-child {{ color: #64748b; }}
.toc li {{ list-style: none; margin: 2px 0; }} .toc .n2 {{ margin-left: 20px; font-size: 10pt; }}
.toc a {{ color: #1e293b; text-decoration: none; }}
.listes li {{ list-style: none; font-size: 9.5pt; }}
</style></head><body>
<section class="garde">
  <div class="bandeau">LICENCE EN INFORMATIQUE ET MULTIMÉDIA — LIM3<br/>Module : Développement d'applications web et multimédia (J2EE, .Net…)</div>
  <div>
    <div class="sous">Rapport de mini-projet</div>
    <h1>DriveEasy</h1>
    <div class="sous">Plateforme de location de véhicules<br/>Architecture microservices Spring Boot / Spring Cloud & Angular 22</div>
  </div>
  <table>
    <tr><td>Réalisé par</td><td><b>{html.escape(ETUDIANT)}</b></td></tr>
    <tr><td>Groupe</td><td>{html.escape(GROUPE)}</td></tr>
    <tr><td>Enseignant</td><td>Karim Ben Romdhane</td></tr>
    <tr><td>Année universitaire</td><td>{annee_univ}</td></tr>
  </table>
  <div style="color:#64748b">Java 21 · Spring Boot 3.5 · Spring Cloud 2025.0 · Eureka · Config Server · Gateway · Zipkin · Angular 22</div>
</section>
<section class="nouvelle-page"><h1 style="border:none">Table des matières</h1><ul class="toc">{toc}</ul></section>
<section class="nouvelle-page listes"><h1 style="border:none">Table des figures</h1><ul>{tdf}</ul>
<h1 style="border:none; margin-top:24px">Liste des tableaux</h1><ul>{tdt}</ul></section>
{corps}
<script src="file://{MERMAID}"></script>
<script>mermaid.initialize({{ startOnLoad: false, theme: 'neutral', flowchart: {{ useMaxWidth: true }}, fontFamily: 'Segoe UI, DejaVu Sans, Arial' }});
mermaid.run().then(() => document.body.setAttribute('data-pret', '1'));</script>
</body></html>'''

sortie_html = os.path.join(ICI, 'rapport.html')
open(sortie_html, 'w', encoding='utf-8').write(DOC)


async def pdf():
    from playwright.async_api import async_playwright
    async with async_playwright() as p:
        b = await p.chromium.launch()
        pg = await b.new_page()
        await pg.goto('file://' + sortie_html)
        await pg.wait_for_selector('body[data-pret="1"]', timeout=60000)
        await pg.pdf(path=os.path.join(ICI, 'Rapport_DriveEasy.pdf'), format='A4', print_background=True,
                     margin={'top': '16mm', 'bottom': '18mm', 'left': '15mm', 'right': '15mm'},
                     display_header_footer=True, header_template='<span></span>',
                     footer_template='<div style="font-size:8px;width:100%;text-align:center;color:#64748b">DriveEasy – Rapport de mini-projet · <span class="pageNumber"></span> / <span class="totalPages"></span></div>')
        await b.close()

asyncio.run(pdf())
print('Rapport genere :', os.path.join(ICI, 'Rapport_DriveEasy.pdf'), f'({compteur_fig} figures, {compteur_tab} tableaux)')
