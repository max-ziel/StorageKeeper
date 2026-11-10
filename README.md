# StorageKeeper
[![Qodana](https://github.com/max-ziel/StorageKeeper/actions/workflows/qodana_code_quality.yml/badge.svg)](https://github.com/max-ziel/StorageKeeper/actions/workflows/qodana_code_quality.yml)

Pantry-App – Java 25, Spring Boot 4, Ports & Adapters, Gradle (Kotlin DSL).

Planung und Milestones: [docs/ROADMAP.md](docs/ROADMAP.md)  
Domain-Modell und Request-Ablauf: [docs/DOMAIN_MODEL.md](docs/DOMAIN_MODEL.md)

## Build & Start

Voraussetzungen: JDK 25 und ein laufendes Docker (für PostgreSQL).

```bash
./gradlew build      # kompiliert, testet (inkl. Architekturregeln und Tests gegen PostgreSQL per Testcontainers)
./gradlew bootRun    # startet PostgreSQL und Keycloak per Docker Compose (compose.yaml) und die App auf http://localhost:8080
curl localhost:8081/actuator/health
```

`bootRun` startet die Container automatisch und stoppt sie beim Beenden wieder; die Daten in PostgreSQL bleiben erhalten.
Das Schema legt Flyway beim Start an (`src/main/resources/db/migration`).

## Anmeldung

Die API ist ein OAuth2 Resource Server: Jeder Aufruf unter `/api/**` braucht ein JWT im Header
`Authorization: Bearer <token>`, sonst kommt `401`. Öffentlich sind nur die Health-Probes `/livez` und `/readyz`
und die API-Dokumentation. Actuator (Health-Details, Metriken) läuft auf einem eigenen Port, siehe „Betrieb“.

Für die lokale Entwicklung läuft Keycloak auf http://localhost:8180 (Admin-Konsole: `admin` / `admin`) mit dem Realm
`storagekeeper` aus `dev/keycloak/storagekeeper-realm.json`. Darin gibt es die Testnutzer `alice` / `alice`,
`bob` / `bob` und `carol` / `carol`. Ein Token (1 Stunde gültig) holt man so:

```bash
TOKEN=$(curl -s localhost:8180/realms/storagekeeper/protocol/openid-connect/token \
  -d grant_type=password -d client_id=storagekeeper-cli -d username=alice -d password=alice | jq -r .access_token)
```

Die App akzeptiert nur Tokens, die vom eingestellten Issuer stammen und für diese API ausgestellt sind
(`aud` enthält `storagekeeper-api`). Einen anderen Issuer setzt man mit der Umgebungsvariable `STORAGEKEEPER_ISSUER_URI`.

## Haushalte

Alle Daten (Artikel, Lagerorte, Einkaufsliste) gehören zu einem Haushalt, und jeder sieht nur die seines eigenen.
Ein Nutzer ist in höchstens einem Haushalt. Ohne Haushalt antworten alle anderen Endpunkte mit `403`.

1. Der Erste legt den Haushalt an (`POST /api/household`) und bekommt einen Einladungscode, z. B. `K7M2-QX9P`.
   Der Haushalt startet mit dem Lagerort „Vorrat“.
2. Weitere Nutzer treten mit diesem Code bei (`POST /api/household/join`). Danach sehen und ändern alle
   Mitglieder denselben Vorrat.

Daten, die vor M5 angelegt wurden, liegen in einem Haushalt „Haushalt“ ohne Mitglieder. Übernehmen kann sie, wer
mit dessen Code beitritt; der Code steht nur in der Datenbank (`SELECT invite_code FROM household;`).

## API-Dokumentation

- Swagger UI: http://localhost:8080/swagger-ui.html – über „Authorize“ das Token (ohne `Bearer `) eintragen.
- OpenAPI-Dokument (JSON): http://localhost:8080/v3/api-docs

Die Dokumentation wird von springdoc aus den Controllern erzeugt und ist daher immer auf dem Stand des Codes.

## REST-API

### Haushalt – `/api/household`

| Methode | Pfad                  | Body             | Antwort                                                      |
|---------|-----------------------|------------------|--------------------------------------------------------------|
| `POST`  | `/api/household`      | `{"name"}`       | `201` + Haushalt; `409` wenn man schon in einem Haushalt ist |
| `GET`   | `/api/household`      | –                | `200` + eigener Haushalt; `403` ohne Haushalt                |
| `POST`  | `/api/household/join` | `{"inviteCode"}` | `200` + Haushalt; `400` bei unbekanntem Code, `409` wie oben |

Ein Haushalt sieht so aus: `{"id", "name", "inviteCode", "members": ["alice", "bob"]}`; die Mitglieder stehen in der
Reihenfolge, in der sie beigetreten sind. Beim Code ist Groß-/Kleinschreibung egal.

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
- Ohne `storageLocationId` landet ein Artikel im Basis-Lagerort „Vorrat“ des Haushalts, ohne `minimumAmount` hat er
  keinen Mindestbestand. Ein angegebener Lagerort muss zum eigenen Haushalt gehören.
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

Jeder Haushalt hat genau einen Basis-Lagerort „Vorrat“; er ist in der Liste mit `"defaultLocation": true` markiert.
Namen müssen nur innerhalb eines Haushalts eindeutig sein.

Fehler kommen als `application/problem+json` (`400` bei ungültigen Eingaben oder unbekanntem Lagerort,
`403` ohne Haushalt, `404` bei unbekannter Artikel- oder Eintrags-ID, `409` bei doppeltem Lagerort-Namen).
Artikel und Einträge eines anderen Haushalts gelten als unbekannt (`404`), damit niemand fremde IDs ausprobieren kann.
Ein fehlendes oder ungültiges Token ergibt `401`, ebenfalls als `problem+json`; der Header `WWW-Authenticate` nennt zusätzlich den Grund.

Alle Beispiele brauchen das Token aus „Anmeldung“. Damit es nicht jedes Mal angegeben werden muss:

```bash
alias curl='curl -H "Authorization: Bearer $TOKEN"'
```

```bash
curl -X POST localhost:8080/api/household \
  -H 'Content-Type: application/json' -d '{"name":"WG Lindenstraße"}'

curl localhost:8080/api/household

# als anderer Nutzer (eigenes Token), mit dem Code aus der Antwort oben
curl -X POST localhost:8080/api/household/join \
  -H 'Content-Type: application/json' -d '{"inviteCode":"K7M2-QX9P"}'

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

## Betrieb

Die App lauscht auf zwei Ports:

| Port   | Inhalt                                         | Anmeldung | Erreichbar für                       |
|--------|------------------------------------------------|-----------|--------------------------------------|
| `8080` | REST-API, API-Dokumentation, `/livez`/`/readyz` | JWT (API) | Clients, Load Balancer               |
| `8081` | Actuator: Health mit Details, Info, Metriken   | keine     | nur Monitoring im internen Netz      |

Port `8081` braucht kein Token, damit Prometheus und Health-Checks ohne Zugangsdaten auskommen. Er darf deshalb
**nie öffentlich erreichbar** sein (nicht ins Internet weiterleiten, im Container nicht nach außen veröffentlichen).

### Endpunkte

| Methode | Pfad                          | Port   | Antwort                                                                   |
|---------|-------------------------------|--------|---------------------------------------------------------------------------|
| `GET`   | `/livez`                      | `8080` | `200` + `{"status":"UP"}`; läuft die App überhaupt?                       |
| `GET`   | `/readyz`                     | `8080` | `200` wenn bereit (inkl. Datenbank), sonst `503`; ohne Details            |
| `GET`   | `/actuator/health`            | `8081` | Gesamtstatus mit Details aller Prüfungen (Datenbank, Plattenplatz, …)     |
| `GET`   | `/actuator/health/liveness`   | `8081` | wie `/livez`                                                              |
| `GET`   | `/actuator/health/readiness`  | `8081` | wie `/readyz`                                                             |
| `GET`   | `/actuator/info`              | `8081` | Name und Version des Builds, Java-Version, Betriebssystem                 |
| `GET`   | `/actuator/metrics`           | `8081` | Namen aller Metriken; `/actuator/metrics/{name}` zeigt eine davon         |
| `GET`   | `/actuator/prometheus`        | `8081` | alle Metriken im Prometheus-Textformat zum Abholen (Scrape)               |

**Liveness vs. Readiness:** Liveness prüft nur, ob die App läuft; schlägt sie fehl, wird die App neu gestartet.
Readiness prüft zusätzlich die Datenbank; schlägt sie fehl, bekommt die App vorübergehend keine Requests.
Fällt die Datenbank aus, ist die App also „nicht bereit“, wird aber nicht sinnlos neu gestartet.

### Metriken

Alle Metriken tragen das Label `application="storagekeeper"`. Neben den Standardmetriken von Spring Boot
(JVM, Speicher, Threads, Datenbank-Pool `hikaricp_*`, Requests `http_server_requests_seconds_*` als Histogramm
je Endpunkt und Status) gibt es eine fachliche:

| Metrik (Prometheus)                 | Labels | Bedeutung                                                                 |
|-------------------------------------|--------|---------------------------------------------------------------------------|
| `storagekeeper_domain_events_total` | `type` | Anzahl der Domain-Events je Typ, z. B. `type="PantryItemFellBelowMinimum"` |

```bash
curl -s localhost:8081/actuator/prometheus | grep storagekeeper_domain_events
curl -s localhost:8081/actuator/metrics/http.server.requests
```

### Logging

Standardmäßig schreibt die App lesbaren Text auf die Konsole. Mit `STORAGEKEEPER_LOG_FORMAT=ecs` kommt stattdessen
eine JSON-Zeile pro Logeintrag im Elastic Common Schema, die Log-Sammler (Loki, Elasticsearch, …) direkt auswerten
können (alternativ `logstash` oder `gelf`). Während eines API-Requests enthält jede Zeile die ID des angemeldeten
Nutzers (`user.id`, der `sub` aus dem Token).

```bash
STORAGEKEEPER_LOG_FORMAT=ecs ./gradlew bootRun
```

```json
{"@timestamp":"2026-11-06T17:13:24.486Z","log":{"level":"INFO","logger":"io.github.maxziel.storagekeeper.application.HouseholdService"},"process":{"pid":7048,"thread":{"name":"http-nio-8080-exec-4"}},"service":{"name":"storagekeeper"},"message":"Household 26b9e61a-8784-4c6b-a33e-334d1042b9fb founded","user":{"id":"d138cc95-e810-45a0-9447-78418fe29218"},"ecs":{"version":"8.11"}}
```

### Umgebungsvariablen

| Variable                        | Standard                                         | Bedeutung                                   |
|---------------------------------|--------------------------------------------------|---------------------------------------------|
| `STORAGEKEEPER_ISSUER_URI`      | `http://localhost:8180/realms/storagekeeper`     | Issuer der JWTs (Keycloak-Realm)            |
| `STORAGEKEEPER_MANAGEMENT_PORT` | `8081`                                           | Port für Actuator                           |
| `STORAGEKEEPER_LOG_FORMAT`      | leer (Text)                                      | `ecs`, `logstash` oder `gelf` für JSON-Logs |

## Deployment

Das Deployment-Ziel ist ein Rechner mit Docker: Die App läuft als Container-Image zusammen mit PostgreSQL,
Keycloak und optional Prometheus, beschrieben in `deploy/compose.yaml`.

### Image bauen

```bash
./gradlew bootBuildImage
```

Spring Boot baut das Image mit Cloud Native Buildpacks (Paketo), ohne Dockerfile: Java-25-Laufzeit (BellSoft
Liberica JRE), Speicher-Einstellungen passend zum Container-Limit, Start als Nicht-Root-Nutzer. Das Ergebnis ist
`ghcr.io/max-ziel/storagekeeper:0.0.1-SNAPSHOT`, zusätzlich getaggt als `:latest`. Ein anderes Repository setzt man
mit `-PimageRepository=...`, einen weiteren Tag mit `-PimageTag=...`.

Die CI (`.github/workflows/ci.yml`, Job `image`) baut das Image bei jedem Push auf `main`, nachdem die Tests grün
sind, und veröffentlicht es in der GitHub Container Registry, mit den Tags `latest`, Version und Kurz-Hash des Commits.
Damit andere das Image ohne Anmeldung ziehen können, muss das Paket auf GitHub einmal auf „public“ gestellt werden
(Package settings → Change visibility).

### Starten

```bash
cd deploy
cp .env.example .env      # Passwörter in .env ändern
docker compose --env-file .env up -d
```

| Dienst       | Port (Host)      | Inhalt                                                                      |
|--------------|------------------|-----------------------------------------------------------------------------|
| `app`        | `8080`           | StorageKeeper; Actuator-Port `8081` nur im Compose-Netz                      |
| `keycloak`   | `8180`           | Anmeldung, Realm `storagekeeper` aus `dev/keycloak` (beim ersten Start)      |
| `postgres`   | –                | Datenbanken `storagekeeper` und `keycloak`, Daten im Volume `postgres-data` |
| `prometheus` | `127.0.0.1:9090` | nur mit `--profile monitoring`; holt alle 15 s `app:8081/actuator/prometheus` |

| Variable in `.env`        | Bedeutung                                                                     |
|---------------------------|-------------------------------------------------------------------------------|
| `STORAGEKEEPER_VERSION`   | Image-Tag, Standard `latest`                                                  |
| `POSTGRES_PASSWORD`       | Passwort der Datenbank (Pflicht)                                              |
| `KEYCLOAK_ADMIN_PASSWORD` | Passwort des Keycloak-Admins `admin` (Pflicht)                                |
| `KEYCLOAK_URL`            | URL, unter der Clients Keycloak erreichen; Standard `http://localhost:8180` |

Danach funktioniert alles wie unter „Anmeldung“ und „REST-API“ beschrieben. Die Logs der App sind JSON (ECS):
`docker compose logs -f app`. Mit Prometheus: `docker compose --env-file .env --profile monitoring up -d`,
dann http://localhost:9090.

Hinweise:

- **Issuer:** Tokens tragen die öffentliche Keycloak-URL (`KEYCLOAK_URL`) als `iss`. Die App prüft genau diesen
  Issuer, holt die Schlüssel aber direkt im Compose-Netz (`http://keycloak:8080/.../certs`).
- **Testnutzer:** Der importierte Realm enthält `alice`, `bob` und `carol` mit gleichlautenden Passwörtern. Für
  echten Betrieb diese Nutzer in der Admin-Konsole löschen und eigene anlegen.
- **HTTPS:** Die Compose-Datei spricht HTTP. Im Internet gehört ein Reverse Proxy mit TLS davor (z. B. Caddy oder
  Traefik); `KEYCLOAK_URL` ist dann die `https://`-Adresse.
- **Stoppen:** `docker compose down` behält die Daten, `docker compose down -v` löscht sie.

## Paketstruktur

```
io.github.maxziel.storagekeeper
├── domain                      # Fachmodell, frei von Spring/Jakarta
├── application                 # Services, implementieren die Inbound-Ports
│   └── port
│       ├── in                  # Use Cases (von Adaptern aufgerufen)
│       └── out                 # was die Anwendung von außen braucht
└── adapter
    ├── in/web                  # REST-Controller, Security- und OpenAPI-Konfiguration, Logging-Kontext
    ├── in/event                # reagiert auf Domain-Events (Einkaufsliste, Metriken)
    ├── out/persistence         # Persistenz, implementiert Outbound-Ports
    ├── out/event               # veröffentlicht Domain-Events
    └── out/security            # liefert der Application den angemeldeten Nutzer
```

`ArchitectureTest` (ArchUnit) erzwingt die Abhängigkeitsrichtung: Adapter → Application → Domain, nie umgekehrt.
