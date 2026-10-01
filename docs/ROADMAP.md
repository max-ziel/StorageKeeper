# Roadmap

Pantry-App: Vorräte im Haushalt verwalten. Jeder Milestone ist ein eigener PR mit grüner CI,
und die ArchUnit-Regeln bleiben bestehen.

## Entscheidungen

- Java 25, Spring Boot
- Ports & Adapters (Hexagonal), Abhängigkeitsrichtung per ArchUnit erzwungen
- Gradle mit Kotlin DSL

## Milestones

| MS | Thema | Fachlich | Technisch |
|----|-------|----------|-----------|
| M0 ✅ | Walking Skeleton | – | Spring Boot 4, Gradle KTS, ArchUnit, CI |
| M1 | Vorratsartikel | Artikel anlegen, auflisten, Menge ändern, entfernen (Name, Menge, Einheit, Ablaufdatum) | Domain-Modell mit Value Objects, Use-Case-Ports, REST-Adapter, In-Memory-Adapter, Bean Validation, Fehler als `ProblemDetail`, Unit- und Web-Slice-Tests |
| M2 | Echte Persistenz | – (gleiches Verhalten) | PostgreSQL, Spring Data JPA im Persistence-Adapter (eigene Entities, Mapping zur Domain), Flyway, Testcontainers, Docker Compose für lokal |
| M3 | Lagerorte & Ablauf | Lagerorte (Kühlschrank, Keller …), „läuft bald ab“-Abfrage, Mindestbestand je Artikel | Domain-Logik mit Clock-Port (testbare Zeit), Query-Use-Cases, Paging/Filter |
| M4 | Einkaufsliste | Liste entsteht automatisch, wenn der Mindestbestand unterschritten wird; ein abgehakter Eintrag erhöht den Bestand | Domain-Events (als Spring `ApplicationEvent`s im Adapter), transaktionale Use Cases |
| M5 | API & Sicherheit | Mehrere Haushalte/Nutzer, jeder sieht nur seinen Vorrat | springdoc-OpenAPI, Spring Security mit JWT (OAuth2 Resource Server), Autorisierung im Application-Layer |
| M6 | Betrieb | – | Docker-Image (Buildpacks via `bootBuildImage`), Actuator-Metriken, strukturiertes Logging, Deployment-Ziel nach Wahl |

### Optional danach

- Web-Frontend
- Barcode-Scan (Open Food Facts API als weiterer Outbound-Adapter)
- Benachrichtigungen bei Ablauf
