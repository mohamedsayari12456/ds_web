"""
DriveEasy - tests de bout en bout de l'API via la gateway (equivalent Python de test-api.ps1).
Usage : python scripts/test_api.py [fichier_sortie.json]
"""
import json, sys, random, datetime, urllib.request, urllib.error

GATEWAY = "http://localhost:8080/api"
resultats = []


def d(j):
    return (datetime.date.today() + datetime.timedelta(days=j)).isoformat()


def test(nom, methode, chemin, attendu, corps=None):
    data = json.dumps(corps).encode() if corps is not None else None
    req = urllib.request.Request(GATEWAY + chemin, data=data, method=methode,
                                 headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            statut, reponse = r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        statut, reponse = e.code, e.read().decode()
    ok = statut == attendu
    print(f"{'OK ' if ok else 'KO '} {nom:<58} {statut} (attendu {attendu})")
    resultats.append({"nom": nom, "methode": methode, "url": "/api" + chemin,
                      "corps": json.dumps(corps, ensure_ascii=False) if corps else None,
                      "statut": statut, "attendu": attendu, "reponse": reponse})
    return reponse


nouveau = {"immatriculation": f"199 TU {random.randint(1000, 9999)}", "marque": "Skoda", "modele": "Octavia",
           "annee": 2024, "couleur": "Gris", "categorie": "BERLINE", "carburant": "DIESEL", "boite": "AUTOMATIQUE",
           "nbPlaces": 5, "kilometrage": 5000, "prixJournalier": 140}
test("Lister les vehicules diesel <= 200 TND", "GET", "/vehicules?carburant=DIESEL&prixMax=200", 200)
test("Vehicules disponibles sur une periode", "GET", f"/vehicules/disponibles?dateDebut={d(20)}&dateFin={d(23)}", 200)
test("Creer un vehicule", "POST", "/vehicules", 201, nouveau)
test("Creer un vehicule : immatriculation existante", "POST", "/vehicules", 409, {**nouveau, "immatriculation": "123 TU 4567"})
test("Creer un vehicule : prix negatif", "POST", "/vehicules", 400, {**nouveau, "immatriculation": "1 TU 1", "prixJournalier": -5})
test("Supprimer un vehicule loue", "DELETE", "/vehicules/5", 409)
test("Vehicule inexistant", "GET", "/vehicules/999", 404)
test("Eligibilite : client eligible", "GET", "/clients/1/eligibilite", 200)
test("Eligibilite : client de 19 ans", "GET", "/clients/7/eligibilite", 200)
test("Eligibilite : permis expire", "GET", "/clients/8/eligibilite", 200)
test("Supprimer un client ayant une location active", "DELETE", "/clients/2", 409)
test("Simulation (client VIP, SUV, 4 jours)", "POST", "/locations/simulation", 200,
     {"clientId": 5, "vehiculeId": 6, "dateDebut": d(20), "dateFin": d(24)})
loc = json.loads(test("Creer une reservation", "POST", "/locations", 201,
                      {"clientId": 5, "vehiculeId": 6, "dateDebut": d(20), "dateFin": d(24)}))
test("Reserver le meme vehicule sur la meme periode", "POST", "/locations", 409,
     {"clientId": 1, "vehiculeId": 6, "dateDebut": d(21), "dateFin": d(22)})
test("Reserver : client non eligible", "POST", "/locations", 400,
     {"clientId": 7, "vehiculeId": 1, "dateDebut": d(20), "dateFin": d(22)})
test("Reserver : vehicule en maintenance", "POST", "/locations", 409,
     {"clientId": 1, "vehiculeId": 12, "dateDebut": d(20), "dateFin": d(22)})
test("Depart de la reservation creee", "PATCH", f"/locations/{loc.get('id')}/depart", 200)
test("Annuler une location en cours", "PATCH", f"/locations/{loc.get('id')}/annulation", 409)
test("Retour de la location 2 (en retard -> penalite)", "PATCH", "/locations/2/retour", 200, {"kmRetour": 13600})
test("Annuler la reservation 4", "PATCH", "/locations/4/annulation", 200)
test("Detail enrichi d'une location", "GET", "/locations/1", 200)
test("Liste paginee des locations terminees", "GET", "/locations?statut=TERMINEE&page=0&size=3", 200)
test("Statistiques", "GET", "/locations/statistiques", 200)

ko = sum(1 for r in resultats if r["statut"] != r["attendu"])
print(f"\n{len(resultats) - ko} / {len(resultats)} tests conformes")
if len(sys.argv) > 1:
    json.dump(resultats, open(sys.argv[1], "w", encoding="utf-8"), ensure_ascii=False, indent=2)
sys.exit(1 if ko else 0)
