# Changelog

Alle wichtigen Änderungen an StorageKeeper. Die Version folgt dem Milestone: `0.<Milestone>.<Patch>`.
Nach dem Merge eines Milestones nach `main` wird der Commit mit `v<Version>` getaggt.

## [0.7.0] – M7 Audit-Nacharbeiten

### Geändert
- `/api/pantry-items/expiring` liefert zusätzlich `minimumAmount` und `belowMinimum`.
- `POST` auf Lagerorte, Haushalt und manuelle Einträge liefert einen `Location`-Header.
- Zwei gleichzeitige Requests, die an einem Datenbank-Constraint scheitern, ergeben `409` statt `500`.
- Nur noch Domain-Regeln (`DomainValidationException`) ergeben `400`; andere Programmfehler ergeben `500` ohne
  interne Details.
- Swagger UI meldet über Keycloak an (Authorization Code mit PKCE); API-Doku per `STORAGEKEEPER_API_DOCS_ENABLED`
  abschaltbar.
- Deployment: eigener Produktions-Realm ohne Testnutzer und ohne Passwort-Grant, eigene Datenbanknutzer für App und
  Keycloak, Passwörter ohne Platzhalter, begrenzte Log-Dateien.

### Intern
- CI baut bei jedem Pull Request einmal mit `build`; Qodana mit JDK 25; Dependabot für Gradle und Actions.
- Abdeckungsschwelle (90 % Zeilen, 75 % Branches); `./gradlew unitTest` läuft ohne Docker.
- ArchUnit prüft zusätzlich die Spring-Nutzung der Application, das Paket `config` und den internen Event-Port.
- Doku an den Code angeglichen.

## [0.6.0] – M6 Betrieb
- Container-Image per Buildpacks, Veröffentlichung in der GitHub Container Registry, Deployment mit Docker Compose.
- Actuator auf eigenem Port, Health-Probes, Prometheus-Metriken, strukturiertes Logging (ECS).

## [0.5.0] – M5 API und Sicherheit
- Haushalte: jeder Nutzer sieht nur den Vorrat seines Haushalts.
- OAuth2 Resource Server mit JWT (Keycloak), OpenAPI-Dokumentation.

## [0.4.0] – M4 Einkaufsliste
- Einkaufsliste aus Domain-Events beim Unterschreiten des Mindestbestands; manuelle Einträge.

## [0.3.0] – M3 Lagerorte und Ablauf
- Lagerorte, „läuft bald ab“-Abfrage, Mindestbestand, Paging und Filter.

## [0.2.0] – M2 Echte Persistenz
- PostgreSQL mit Spring Data JPA, Flyway, Testcontainers.

## [0.1.0] – M1 Vorratsartikel
- Artikel anlegen, auflisten, Menge ändern, entfernen.

## [0.0.1] – M0 Walking Skeleton
- Spring Boot, Gradle, ArchUnit, CI.
