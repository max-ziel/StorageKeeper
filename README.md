# StorageKeeper
[![Qodana](https://github.com/max-ziel/StorageKeeper/actions/workflows/qodana_code_quality.yml/badge.svg)](https://github.com/max-ziel/StorageKeeper/actions/workflows/qodana_code_quality.yml)

Pantry-App – Java 25, Spring Boot 4, Ports & Adapters, Gradle (Kotlin DSL).

Planung und Milestones: [docs/ROADMAP.md](docs/ROADMAP.md)  
Domain-Modell und Request-Ablauf: [docs/DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md)

## Build & Start

Voraussetzungen: JDK 25 und ein laufendes Docker (für PostgreSQL).

```bash
./gradlew build      # kompiliert, testet (inkl. Architekturregeln und Tests gegen PostgreSQL per Testcontainers)
./gradlew bootRun    # startet PostgreSQL per Docker Compose (compose.yaml) und die App auf http://localhost:8080
curl localhost:8080/actuator/health
```

`bootRun` startet den Datenbank-Container automatisch und stoppt ihn beim Beenden wieder; die Daten bleiben erhalten.
Das Schema legt Flyway beim Start an (`src/main/resources/db/migration`).

## REST-API

### Vorratsartikel – `/api/pantry-items`

| Methode  | Pfad                              | Body / Parameter                                                                    | Antwort                                        |
|----------|-----------------------------------|-------------------------------------------------------------------------------------|------------------------------------------------|
| `POST`   | `/api/pantry-items`               | `{"name", "amount", "unit", "expiryDate"?, "storageLocationId"?, "minimumAmount"?}` | `201` + `Location`-Header + Artikel            |
| `GET`    | `/api/pantry-items`               | `?storageLocationId=&belowMinimum=&page=&size=` (alle optional)                     | `200` + Seite (`content`, `totalElements`, …)  |
| `GET`    | `/api/pantry-items/expiring`      | `?days=` (optional, 0–365, Standard 7)                                              | `200` + Liste mit `daysUntilExpiry`, `expired` |
| `PATCH`  | `/api/pantry-items/{id}/quantity` | `{"amount"}`                                                                        | `200` + Artikel                                |
| `PATCH`  | `/api/pantry-items/{id}/minimum`  | `{"amount"}` (`0` = kein Mindestbestand)                                            | `200` + Artikel                                |
| `DELETE` | `/api/pantry-items/{id}`          | –                                                                                   | `204`                                          |

- Erlaubte Einheiten: `PIECE`, `GRAM`, `KILOGRAM`, `MILLILITER`, `LITER`, `PACKAGE`.
- Ohne `storageLocationId` landet ein Artikel im Basis-Lagerort „Vorrat“, ohne `minimumAmount` hat er keinen Mindestbestand.
- Die Liste ist nach Name sortiert; Standard ist Seite `0` mit `20` Einträgen, höchstens `100` pro Seite.
  `belowMinimum=true` zeigt nur Artikel, deren Menge unter dem Mindestbestand liegt.
- `/expiring` liefert alles, was in den nächsten `days` Tagen abläuft oder schon abgelaufen ist (`daysUntilExpiry` negativ).

### Einkaufsliste – `/api/shopping-list`

| Methode  | Pfad                                                | Body                           | Antwort                                                     |
|----------|-----------------------------------------------------|--------------------------------|-------------------------------------------------------------|
| `GET`    | `/api/shopping-list`                                | –                              | `200` + `{"stockPurchases": [...], "manualEntries": [...]}` |
| `POST`   | `/api/shopping-list/stock-purchases/{id}/check-off` | `{"amount"}` (> 0)             | `200` + Artikel mit neuem Bestand                           |
| `POST`   | `/api/shopping-list/manual-entries`                 | `{"name", "amount"?, "unit"?}` | `201` + Eintrag                                             |
| `DELETE` | `/api/shopping-list/manual-entries/{id}`            | –                              | `204`                                                       |

Die Liste hat zwei Abschnitte, beide älteste zuerst:

- **Vorratseinkäufe** (`stockPurchases`) pflegen sich selbst: Fällt ein Artikel unter seinen Mindestbestand,
  kommt er darauf, erreicht er ihn wieder, verschwindet er. Jeder Eintrag zeigt `missingAmount`.
  Abhaken mit der gekauften Menge erhöht den Bestand. Wurde weniger gekauft als fehlt, bleibt der Eintrag
  mit dem Rest stehen.
- **Einkaufsliste** (`manualEntries`) enthält, was der Nutzer selbst einträgt, z. B. „Geburtstagskerzen“.
  Menge und Einheit sind optional, aber nur zusammen. Abhaken = löschen; der Bestand ändert sich nicht.

### Lagerorte – `/api/storage-locations`

| Methode | Pfad                     | Body       | Antwort                                         |
|---------|--------------------------|------------|-------------------------------------------------|
| `POST`  | `/api/storage-locations` | `{"name"}` | `201` + Lagerort; `409` wenn der Name existiert |
| `GET`   | `/api/storage-locations` | –          | `200` + Liste, sortiert nach Name               |

Der Basis-Lagerort „Vorrat“ existiert immer und ist in der Liste mit `"defaultLocation": true` markiert.

Fehler kommen als `application/problem+json` (`400` bei ungültigen Eingaben oder unbekanntem Lagerort,
`404` bei unbekannter Artikel- oder Eintrags-ID, `409` bei doppeltem Lagerort-Namen).

```bash
curl localhost:8080/api/storage-locations

curl -X POST localhost:8080/api/storage-locations \
  -H 'Content-Type: application/json' -d '{"name":"Keller"}'

curl -i -X POST localhost:8080/api/pantry-items \
  -H 'Content-Type: application/json' \
  -d '{"name":"Reis","amount":1,"unit":"KILOGRAM","expiryDate":"2027-03-01","minimumAmount":2}'

curl -X POST localhost:8080/api/pantry-items \
  -H 'Content-Type: application/json' \
  -d '{"name":"Wein","amount":6,"unit":"PIECE","storageLocationId":"<id vom Keller>"}'

curl 'localhost:8080/api/pantry-items?size=10'
curl 'localhost:8080/api/pantry-items?belowMinimum=true'
curl 'localhost:8080/api/pantry-items?storageLocationId=<id vom Keller>'
curl 'localhost:8080/api/pantry-items/expiring?days=3'
curl localhost:8080/api/shopping-list

curl -X POST localhost:8080/api/shopping-list/stock-purchases/<eintrag-id>/check-off \
  -H 'Content-Type: application/json' -d '{"amount":1}'

curl -X POST localhost:8080/api/shopping-list/manual-entries \
  -H 'Content-Type: application/json' -d '{"name":"Geburtstagskerzen","amount":12,"unit":"PIECE"}'

curl -i -X DELETE localhost:8080/api/shopping-list/manual-entries/<eintrag-id>

curl -X PATCH localhost:8080/api/pantry-items/<id>/quantity \
  -H 'Content-Type: application/json' -d '{"amount":0.5}'

curl -X PATCH localhost:8080/api/pantry-items/<id>/minimum \
  -H 'Content-Type: application/json' -d '{"amount":3}'

curl -i -X DELETE localhost:8080/api/pantry-items/<id>
```

Die Daten werden in PostgreSQL gespeichert und bleiben über Neustarts erhalten.

## Paketstruktur

```
io.github.maxziel.storagekeeper
├── domain                      # Fachmodell, frei von Spring/Jakarta
├── application                 # Services, implementieren die Inbound-Ports
│   └── port
│       ├── in                  # Use Cases (von Adaptern aufgerufen)
│       └── out                 # was die Anwendung von außen braucht
└── adapter
    ├── in/web                  # REST-Controller
    └── out/persistence         # Persistenz, implementiert Outbound-Ports
```

`ArchitectureTest` (ArchUnit) erzwingt die Abhängigkeitsrichtung: Adapter → Application → Domain, nie umgekehrt.
