# DriveEasy - tests de bout en bout de l'API via la gateway (backend demarre).
# Usage : .\scripts\test-api.ps1  [-Sortie ..\rapport\api-tests.json]
# Chaque test compare le code HTTP obtenu au code attendu ; un resume est affiche a la fin.
param([string]$Gateway = "http://localhost:8080/api", [string]$Sortie = "")
$ErrorActionPreference = "Continue"
$aujourdhui = Get-Date
function D([int]$j) { $aujourdhui.AddDays($j).ToString("yyyy-MM-dd") }
$resultats = @()

function Test-Api($nom, $methode, $chemin, $attendu, $corps = $null) {
    $url = "$Gateway$chemin"
    $json = if ($corps) { $corps | ConvertTo-Json -Compress } else { $null }
    try {
        $p = @{ Uri = $url; Method = $methode; UseBasicParsing = $true; ContentType = "application/json; charset=utf-8" }
        if ($json) { $p.Body = [System.Text.Encoding]::UTF8.GetBytes($json) }
        $r = Invoke-WebRequest @p
        $statut = [int]$r.StatusCode; $reponse = $r.Content
    } catch {
        $statut = [int]$_.Exception.Response.StatusCode
        $reponse = $_.ErrorDetails.Message
    }
    $ok = if ($statut -eq $attendu) { "OK  " } else { "KO  " }
    Write-Host ("{0}{1,-58} {2} (attendu {3})" -f $ok, $nom, $statut, $attendu) -ForegroundColor $(if ($statut -eq $attendu) { "Green" } else { "Red" })
    $script:resultats += [pscustomobject]@{ nom = $nom; methode = $methode; url = "/api$chemin"; corps = $json; statut = $statut; attendu = $attendu; reponse = $reponse }
    return $reponse
}

Test-Api "Lister les vehicules diesel <= 200 TND" GET "/vehicules?carburant=DIESEL&prixMax=200" 200 | Out-Null
Test-Api "Vehicules disponibles sur une periode" GET "/vehicules/disponibles?dateDebut=$(D 20)&dateFin=$(D 23)" 200 | Out-Null
$nouveau = @{ immatriculation = "199 TU $(Get-Random -Maximum 9999)"; marque = "Skoda"; modele = "Octavia"; annee = 2024; couleur = "Gris"; categorie = "BERLINE"; carburant = "DIESEL"; boite = "AUTOMATIQUE"; nbPlaces = 5; kilometrage = 5000; prixJournalier = 140 }
Test-Api "Creer un vehicule" POST "/vehicules" 201 $nouveau | Out-Null
Test-Api "Creer un vehicule : immatriculation existante" POST "/vehicules" 409 ($nouveau.Clone() | ForEach-Object { $_.immatriculation = "123 TU 4567"; $_ }) | Out-Null
Test-Api "Creer un vehicule : prix negatif" POST "/vehicules" 400 ($nouveau.Clone() | ForEach-Object { $_.immatriculation = "1 TU 1"; $_.prixJournalier = -5; $_ }) | Out-Null
Test-Api "Supprimer un vehicule loue" DELETE "/vehicules/5" 409 | Out-Null
Test-Api "Vehicule inexistant" GET "/vehicules/999" 404 | Out-Null
Test-Api "Eligibilite : client eligible" GET "/clients/1/eligibilite" 200 | Out-Null
Test-Api "Eligibilite : client de 19 ans" GET "/clients/7/eligibilite" 200 | Out-Null
Test-Api "Eligibilite : permis expire" GET "/clients/8/eligibilite" 200 | Out-Null
Test-Api "Supprimer un client ayant une location active" DELETE "/clients/2" 409 | Out-Null
Test-Api "Simulation (client VIP, SUV, 4 jours)" POST "/locations/simulation" 200 @{ clientId = 5; vehiculeId = 6; dateDebut = (D 20); dateFin = (D 24) } | Out-Null
$loc = Test-Api "Creer une reservation" POST "/locations" 201 @{ clientId = 5; vehiculeId = 6; dateDebut = (D 20); dateFin = (D 24) }
$id = ($loc | ConvertFrom-Json).id
Test-Api "Reserver le meme vehicule sur la meme periode" POST "/locations" 409 @{ clientId = 1; vehiculeId = 6; dateDebut = (D 21); dateFin = (D 22) } | Out-Null
Test-Api "Reserver : client non eligible" POST "/locations" 400 @{ clientId = 7; vehiculeId = 1; dateDebut = (D 20); dateFin = (D 22) } | Out-Null
Test-Api "Reserver : vehicule en maintenance" POST "/locations" 409 @{ clientId = 1; vehiculeId = 12; dateDebut = (D 20); dateFin = (D 22) } | Out-Null
Test-Api "Depart de la reservation creee" PATCH "/locations/$id/depart" 200 | Out-Null
Test-Api "Annuler une location en cours" PATCH "/locations/$id/annulation" 409 | Out-Null
Test-Api "Retour de la location 2 (en retard -> penalite)" PATCH "/locations/2/retour" 200 @{ kmRetour = 13600 } | Out-Null
Test-Api "Annuler la reservation 4" PATCH "/locations/4/annulation" 200 | Out-Null
Test-Api "Detail enrichi d'une location" GET "/locations/1" 200 | Out-Null
Test-Api "Liste paginee des locations terminees" GET "/locations?statut=TERMINEE&page=0&size=3" 200 | Out-Null
Test-Api "Statistiques" GET "/locations/statistiques" 200 | Out-Null

$ko = ($resultats | Where-Object { $_.statut -ne $_.attendu }).Count
Write-Host "`n$($resultats.Count - $ko) / $($resultats.Count) tests conformes" -ForegroundColor $(if ($ko) { "Red" } else { "Green" })
if ($Sortie) { $resultats | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 $Sortie; Write-Host "Resultats ecrits dans $Sortie" }
