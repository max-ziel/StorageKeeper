# Roadmap

Pantry-App: Vorräte im Haushalt verwalten. Jeder Milestone ist ein eigener PR mit grüner CI,
und die ArchUnit-Regeln bleiben bestehen.

## Entscheidungen

- Java 25, Spring Boot
- Ports & Adapters (Hexagonal), Abhängigkeitsrichtung per ArchUnit erzwungen
- Gradle mit Kotlin DSL
- Aufbau des Domain-Modells: [DOMAIN_MODEL.md](DOMAIN_MODEL.md)

## Milestones

| MS    | Thema              | Fachlich                                                                                                           | Technisch                                                                                                                                                |
|-------|--------------------|--------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| M0 ✅ | Walking Skeleton   | –                                                                                                                  | Spring Boot 4, Gradle KTS, ArchUnit, CI                                                                                                                  |
| M1 ✅ | Vorratsartikel     | Artikel anlegen, auflisten, Menge ändern, entfernen (Name, Menge, Einheit, Ablaufdatum)                            | Domain-Modell mit Value Objects, Use-Case-Ports, REST-Adapter, In-Memory-Adapter, Bean Validation, Fehler als `ProblemDetail`, Unit- und Web-Slice-Tests |
| M2 ✅ | Echte Persistenz   | – (gleiches Verhalten)                                                                                             | PostgreSQL, Spring Data JPA im Persistence-Adapter (eigene Entities, Mapping zur Domain), Flyway, Testcontainers, Docker Compose für lokal               |
| M3 ✅ | Lagerorte & Ablauf | Lagerorte (Kühlschrank, Keller …), „läuft bald ab“-Abfrage, Mindestbestand je Artikel                              | Domain-Logik mit Clock-Port (testbare Zeit), Query-Use-Cases, Paging/Filter                                                                              |
| M4 ✅ | Einkaufsliste      | Liste entsteht automatisch, wenn der Mindestbestand unterschritten wird; ein abgehakter Eintrag erhöht den Bestand | Domain-Events (als Spring `ApplicationEvent`s im Adapter), transaktionale Use Cases                                                                      |
| M5 ✅ | API & Sicherheit   | Mehrere Haushalte/Nutzer, jeder sieht nur seinen Vorrat                                                            | springdoc-OpenAPI, Spring Security mit JWT (OAuth2 Resource Server), Autorisierung im Application-Layer                                                  |
| M6 ✅ | Betrieb            | –                                                                                                                  | Docker-Image (Buildpacks via `bootBuildImage`), Actuator-Metriken, strukturiertes Logging, Deployment mit Docker Compose                                 |
| M7    | Audit-Nacharbeiten | – (bis auf Kleinigkeiten gleiches Verhalten)                                                                       | Befunde aus dem Code-Audit: Doku ↔ Code, Build und CI, Fehlerbehandlung, Härtung des Deployments                                                       |

### M6 im Detail

- [x] Teil 1: Actuator auf eigenem Port, Health-Probes (`/livez`, `/readyz`), Prometheus-Metriken mit fachlichem
  Event-Zähler, strukturiertes Logging (ECS) mit Nutzer-ID
- [x] Teil 2: Docker-Image per `bootBuildImage`, Veröffentlichung in der GitHub Container Registry durch die CI,
  Deployment mit Docker Compose (`deploy/`: App, PostgreSQL, Keycloak, optional Prometheus)

### M7 im Detail

- [x] Teil 1: Aufräumen ohne Verhaltensänderung: Doku an den Code angeglichen (Schichten, Request-Ablauf,
  veraltete Milestone-Bezüge, Paketstruktur), Javadoc und Tippfehler, Transaktionsregel für Use Cases,
  `ShoppingListEntryJpaEntity` in eigener Datei, Konstanten statt Magic Numbers, CI baut `build` und läuft je
  Änderung nur einmal, Qodana mit JDK 25; `/expiring` liefert jetzt auch `minimumAmount` und `belowMinimum`
- [x] Teil 2: Fehlerbehandlung: Constraint-Verletzungen bei parallelen Requests als `409`, eigene
  Validierungs-Exception statt jeder `IllegalArgumentException`, einheitliche `Location`-Header, Coverage-Schwelle,
  Integrationstests getrennt lauffähig, README-Abschnitte „Tests“, „Lizenz“, „Mitwirken“
- [ ] Teil 3: Betrieb und Sicherheit: Produktions-Realm ohne Testnutzer, eigener DB-Nutzer für Keycloak,
  Swagger UI abschaltbar, Versionen und Tags, Dependabot, `SECURITY.md`, ArchUnit-Regeln für Application und Event-Port

### Optional danach

- Web-Frontend
- Barcode-Scan (Open Food Facts API als weiterer Outbound-Adapter)
- Benachrichtigungen bei Ablauf
