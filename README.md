# StorageKeeper
[![Qodana](https://github.com/max-ziel/StorageKeeper/actions/workflows/qodana_code_quality.yml/badge.svg)](https://github.com/max-ziel/StorageKeeper/actions/workflows/qodana_code_quality.yml)

Pantry-App – Java 25, Spring Boot 4, Ports & Adapters, Gradle (Kotlin DSL).

Planung und Milestones: [docs/ROADMAP.md](docs/ROADMAP.md)
Domain-Modell und Request-Ablauf: [docs/domain-model.md](docs/DOMAIN_MODEL.md)

## Build & Start

```bash
./gradlew build      # kompiliert, testet (inkl. Architekturregeln)
./gradlew bootRun    # startet auf http://localhost:8080
curl localhost:8080/actuator/health
```

## REST-API

Basis-URL: `http://localhost:8080/api/pantry-items`

| Methode  | Pfad                              | Body                                        | Antwort                             |
|----------|-----------------------------------|---------------------------------------------|-------------------------------------|
| `POST`   | `/api/pantry-items`               | `{"name", "amount", "unit", "expiryDate"?}` | `201` + `Location`-Header + Artikel |
| `GET`    | `/api/pantry-items`               | –                                           | `200` + Liste                       |
| `PATCH`  | `/api/pantry-items/{id}/quantity` | `{"amount"}`                                | `200` + Artikel                     |
| `DELETE` | `/api/pantry-items/{id}`          | –                                           | `204`                               |

Erlaubte Einheiten: `PIECE`, `GRAM`, `KILOGRAM`, `MILLILITER`, `LITER`, `PACKAGE`.
Fehler kommen als `application/problem+json` (`400` bei ungültigen Eingaben, `404` bei unbekannter ID).

```bash
curl -i -X POST localhost:8080/api/pantry-items \
  -H 'Content-Type: application/json' \
  -d '{"name":"Reis","amount":1,"unit":"KILOGRAM","expiryDate":"2027-03-01"}'

curl localhost:8080/api/pantry-items

curl -X PATCH localhost:8080/api/pantry-items/<id>/quantity \
  -H 'Content-Type: application/json' -d '{"amount":0.5}'

curl -i -X DELETE localhost:8080/api/pantry-items/<id>
```

Die Daten liegen in M1 nur im Speicher und sind nach einem Neustart weg.


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
