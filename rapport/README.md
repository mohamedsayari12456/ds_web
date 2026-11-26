# Rapport DriveEasy

`build_report.py` génère `Rapport_DriveEasy.pdf` à partir du code source (extraits toujours à jour),
des captures du dossier `captures/` et des résultats `api-tests.json`.

1. Démarrer le backend (`..\driveeasy-backend\start-all.ps1`) et le frontend (`npm start`).
2. `..\driveeasy-backend\scripts\test-api.ps1 -Sortie api-tests.json`
3. Placer les captures dans `captures/` (noms attendus : eureka.png, config-server.png, zipkin-traces.png,
   zipkin-trace.png, front-*.png — un cadre rouge indique chaque capture manquante dans le PDF).
4. `pip install playwright && playwright install chromium`, `npm i mermaid` puis
   `set ETUDIANT=Prénom NOM` et `python build_report.py` (variable MERMAID_JS = chemin de mermaid.min.js).
