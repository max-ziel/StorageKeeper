# Audit-Report StorageKeeper – Stand Milestone 7

Stand: 2026-11-10, Branch `milestones/milestone7`, Commit `ee780fb` („M7 (3/3): Harden the deployment and version the releases“).

> **Nachtrag:** Abschnitt 8 prüft die Nacharbeiten bis Commit `b119691` („Remove duplicate class“). Ergebnis: Build grün, die neuen Befunde
> N-01 bis N-04, N-07 und N-09 sind behoben, keiner mit Schweregrad Kritisch oder Hoch ist mehr offen. Offen sind noch einige Mittel-Befunde
> ohne Einfluss auf das Verhalten (K-01, B-03, R-02, R-05) sowie Niedrig-Befunde und Repo-Einstellungen.

Reines Lese-Audit, am Code wurde nichts geändert. Vergleichsbasis ist der erste Bericht ([AUDIT_REPORT.md](AUDIT_REPORT.md), Commit `12897b7` auf `main`);
die Befund-IDs von dort werden hier weiterverwendet, neue Befunde haben das Präfix `N-`.

## 1. Zusammenfassung

M7 hat den ersten Bericht **sehr gründlich abgearbeitet**: Von den 59 Einträgen der Statusliste (Abschnitt 3) sind 40 behoben, 9 teilweise behoben, 5 bewusst unverändert und 5 offen (davon einer nur manuell prüfbar, einer verschlechtert).
Build, alle 236 Tests, Coverage-Schwelle und ArchUnit (jetzt auch für Application, `config` und den Event-Port) laufen grün; Abdeckung 98,3 % Zeilen / 82,9 % Branches.
Das Deployment ist deutlich härter (eigener Produktions-Realm ohne Testnutzer und ohne Passwort-Grant, getrennte DB-Logins ohne Superuser-Rechte, beides von mir praktisch geprüft).

**Top 5 (aktueller Stand):**
1. **N-01 (Hoch):** Der Swagger-Login über Keycloak funktioniert **lokal nicht**: `application.yaml:28` setzt `storagekeeper-web`, der Dev-Realm `dev/keycloak/storagekeeper-realm.json` enthält diesen Client aber nicht. README:45 und DOMAIN_MODEL.md:630 behaupten das Gegenteil.
2. **N-02 (Mittel):** Jede `DataIntegrityViolationException` wird zu `409` „changed at the same time“ – auch CHECK-/NOT-NULL-Verletzungen, die Programmfehler sind.
3. **N-03 (Mittel):** Der Release-Tag `:0.7.0` des Images wird bei jedem Push auf `main` überschrieben (dokumentiert, aber ein veränderlicher Versions-Tag); es gibt weiterhin keinen einzigen Git-Tag, obwohl der CHANGELOG `0.0.1`–`0.7.0` aufführt.
4. **B-06 (Niedrig, verschlechtert):** Wildcard-Imports haben zugenommen (jetzt 9 statt 2 im Produktivcode); weiterhin kein Formatter (C-06).
5. **K-01 (Mittel, offen):** Sprachmix der API (deutsche OpenAPI-Tags, englische Fehlermeldungen) unverändert.

**Gut gelöst:** `DomainValidationException` statt pauschaler `IllegalArgumentException`, Catch-all ohne interne Details, erzwungener Race-Test (`ConcurrentRequestsIntegrationTest` mit `CyclicBarrier`), `unitTest` ohne Docker, Coverage-Gate, Dependabot, `SECURITY.md`, `CHANGELOG.md`, README-Abschnitte Tests/Mitwirken/Lizenz, Upgrade-Anleitung für bestehende Installationen.

## 2. Was ausgeführt wurde

| Befehl / Prüfung | Ergebnis |
|---|---|
| `git fetch origin milestones/milestone7`, `git checkout` | 5 Commits über `main` (davon 1 Merge, 1 doppelter Commit „M7 (1/3)“), 84 Dateien, +1124 / −206 Zeilen |
| Vollständiger Diff `main..milestones/milestone7` gelesen | Grundlage der Statusliste in Abschnitt 3 |
| `./gradlew clean build jacocoTestReport` (JDK 25, Docker an) | **BUILD SUCCESSFUL in 1 m 16 s; 236 Tests, 0 Fehler, 0 übersprungen**; `jacocoTestCoverageVerification` lief als Teil von `check` |
| JaCoCo | Instruktionen 98,2 %, Branches 82,9 %, Zeilen 98,3 %, Methoden 98,0 %, Klassen 100 % |
| `./gradlew unitTest --rerun-tasks` **mit gestopptem Docker-Daemon** | BUILD SUCCESSFUL in 14 s, 170 Tests in 22 Klassen – die Trennung funktioniert |
| `compileJava compileTestJava` mit `-Xlint:all` (Init-Skript im Scratchpad) | nur noch 8 × `[serial]`; `auxiliaryclass`, `this-escape`, `try`, `unchecked` sind weg |
| `dependencyInsight --dependency jackson-databind` | bestätigt den neuen Kommentar in `build.gradle.kts`: springdoc 3.2.0 fordert Jackson 2.21.7, Override hebt auf 2.21.8 |
| `deploy/postgres/init-databases.sh` in Wegwerf-Container `postgres:18-alpine` | Rollen `storagekeeper` und `keycloak` ohne Superuser, je Eigentümer ihrer DB; `storagekeeper` → DB `keycloak`: „User does not have CONNECT privilege“ ✔ |
| `deploy/keycloak/storagekeeper-realm.json` in Keycloak 26.4 importiert (`STORAGEKEEPER_URL=https://pantry.example.org`) | Import ok; Redirect-URI aufgelöst zu `https://pantry.example.org/swagger-ui/oauth2-redirect.html`; PKCE `S256`; 0 Nutzer; Passwort-Grant ergibt `400` ✔ |
| `docker compose config` für `deploy/` mit leerer `.env.example` | bricht wie dokumentiert mit „required variable … is missing a value“ ab ✔ |
| GitHub-Search-API | About: „Pantry-App – Java 25, Spring Boot 4, Ports & Adapters“, Topics `hexagonal-architecture`, `java`, `keycloak`, `pantry`, `spring-boot` ✔; `git ls-remote --tags`: **keine Tags** |

**Nicht geprüft:** `bootRun` mit Dev-Keycloak und der Browser-Login in der Swagger UI (N-01 ist aus den Dateien eindeutig belegt, aber nicht im Browser nachgestellt);
der Komplett-Stack `deploy/compose.yaml up` mit App-Image; die Upgrade-Anleitung für Installationen vor 0.7.0 (README:337-353); `bootBuildImage`; Qodana-Ergebnisse; Schwachstellen-Scan der Abhängigkeiten.

## 3. Status der Befunde aus dem ersten Audit

✅ behoben · ◐ teilweise · ❌ offen · ➖ bewusst nicht geändert

| ID | Status | Beleg / Rest |
|---|---|---|
| D-01 Application „nur JDK“ | ✅ | Aussage korrigiert, README:401 nennt die erlaubten Spring-Annotationen; ArchUnit-Regel `applicationUsesSpringOnlyForWiringAndTransactions` |
| D-02 `ExpiringPantryItemResponse` | ✅ | Felder ergänzt (`ExpiringPantryItemResponse.java`), Doku DOMAIN_MODEL.md:940-942 stimmt |
| D-03 Request-Ablauf-Diagramm | ✅ | enthält jetzt `CurrentHousehold`, `belongsTo`, `publish` |
| D-04 Veraltete Zeitbezüge | ✅ | alle vier Stellen entfernt |
| D-05 / D-06 Falsche Javadoc | ✅ | |
| D-07 Paketstruktur | ✅ | README:385-397 mit `config` und `application.port` |
| D-08 Fehlende README-Abschnitte | ✅ | Tests, Mitwirken, Lizenz, CHANGELOG-Link |
| D-09 Voraussetzungen | ✅ | inkl. Foojay-Hinweis (B-07) |
| D-10 Tippfehler | ◐ | alle bis auf `SecurityConfiguration.java:53`: „standard-bearer token entry point“ (gemeint: Standard-Entry-Point für Bearer-Tokens) |
| D-11 V2-Kommentar | ➖ | richtig so (Migration nicht ändern) |
| D-12 Javadoc `StorageKeeperApplication`, `ClockConfiguration` | ✅ | |
| R-01 GitHub About/Topics | ✅ | |
| R-02 Community-Dateien | ◐ | `SECURITY.md`, `dependabot.yml` neu; `CONTRIBUTING.md` als README-Abschnitt; Issue-/PR-Templates, `CODEOWNERS`, `CODE_OF_CONDUCT.md` fehlen weiter |
| R-03 Versionierung | ◐ | Version `0.7.0`, CHANGELOG, Doku zum Taggen; aber noch **kein Tag** und veränderlicher Image-Tag (siehe N-03) |
| R-04 Doppelte CI-Runs | ✅ | `ci.yml:3-7` push nur auf `main` |
| R-05 Qodana | ◐ | JDK 25, `contents: read`; führt aber weiter alle Tests ein zweites Mal aus (`qodana_code_quality.yml:29`) |
| R-06 CI `build` | ✅ | |
| R-07 CI-Badge | ✅ | |
| R-08 `.gitignore` | ✅ | |
| R-09 Repo-Settings | ❌ (manuell) | weiter nicht per API prüfbar; neu relevant: „Private vulnerability reporting“ muss aktiviert sein, sonst läuft der Meldeweg in `SECURITY.md` ins Leere |
| B-01 Swagger-Plugin | ✅ | |
| B-02 Jackson-Override | ✅ | Kommentar ergänzt und von mir per `dependencyInsight` bestätigt |
| B-03 Locking/Scan | ◐ | Dependabot ja; kein Lockfile, kein Schwachstellen-Scan im Build |
| B-04 Auxiliary class | ✅ | Datei umbenannt |
| B-05 Lint-Warnungen | ◐ | nur noch 8 × `[serial]` (unkritisch) |
| B-06 Wildcard-Imports | ❌ verschlechtert | siehe N-06 |
| B-07 Foojay | ✅ | README-Hinweis |
| C-01 Race → 500 | ✅ | Handler `ApiExceptionHandler.java:87`, `@Transactional` an `StorageLocationService.add`; Test erzwingt das Rennen |
| C-02 `IllegalArgumentException` → 400 | ✅ | `DomainValidationException`; Catch-all `ApiExceptionHandler.java:110` ohne Details (siehe aber N-02) |
| C-03 `ManualShoppingListEntry` | ✅ | |
| C-04 Transaktionsgrenzen | ✅ | Regel in DOMAIN_MODEL.md:465-470 |
| C-05 Magic Numbers | ✅ | `InviteCode.LENGTH`/`GROUP_SIZE`, `ItemName.MAX_LENGTH` |
| C-06 Formatter | ❌ | weiterhin kein Spotless/Checkstyle/`.editorconfig` |
| C-07 Sichtbarkeit | ✅ | `PantryItemSpecifications` jetzt `final` und package-private |
| C-08 Doppeltes Laden in `checkOff` | ➖ | unverändert, unkritisch |
| C-09 `Persistable` / Extra-SELECT | ❌ | unverändert |
| C-10 `Location`-Header | ✅ | alle `201`-Antworten |
| A-01 Spring in Application | ✅ | dokumentiert + ArchUnit |
| A-02 `config` in ArchUnit | ✅ | |
| A-03 Interner Event-Port | ✅ | ArchUnit-Regel `shoppingListIsOnlyUpdatedFromDomainEvents` |
| A-04 „Vorrat“ in der Domain | ➖ | unverändert, vertretbar |
| T-01 Coverage-Gate | ✅ | 90 % Zeilen, 75 % Branches |
| T-02 Nebenläufigkeitstests | ✅ | `ConcurrentRequestsIntegrationTest` |
| T-03 Teststrategie im README | ✅ | README „Tests“ |
| T-04 Tests ohne Docker | ✅ | `unitTest`, von mir ohne Daemon geprüft |
| T-05 Unchecked / Lücke `SpringSecurityCurrentUserProvider` | ✅ | neuer `SpringSecurityCurrentUserProviderTest` |
| S-01 Dev-Realm in Produktion | ✅ | `deploy/keycloak/storagekeeper-realm.json`, praktisch geprüft |
| S-02 Gemeinsamer DB-Nutzer | ✅ | `init-databases.sh`, praktisch geprüft |
| S-03 Swagger in Produktion | ✅ | `STORAGEKEEPER_API_DOCS_ENABLED`, `ApiDocsDisabledIntegrationTest` |
| S-04 Actuator ohne Auth | ➖ | unverändert, bewusst und dokumentiert (SECURITY.md) |
| S-05 `change-me` | ✅ | leere Werte, Compose bricht ab (geprüft) |
| S-06 Kein TLS | ◐ | Doku erweitert (`STORAGEKEEPER_URL` als https), kein TLS-Profil |
| S-07 Log-Aufbewahrung | ✅ | Docker-Log-Rotation `deploy/compose.yaml:25-29`, README-Hinweis |
| S-09 Schwachstellen-Scan | ◐ | Dependabot Version Updates; Alerts nur in den Repo-Settings prüfbar |
| K-01 Sprachmix API | ❌ | unverändert |
| K-02 JDK in Qodana | ✅ | |
| K-03 Benennung Einkaufsliste | ◐ | Datei umbenannt; `ManualShoppingListRepository` vs. `…EntryRepository` unverändert |
| K-04 Projektname/Topics | ✅ | Topics passen jetzt |
| K-05 SQL-Stil | ➖ | betrifft nur künftige Migrationen |

## 4. Neue Befunde

Schweregrade wie im ersten Bericht. Aufwand: S < 1 h, M ≤ 1 Tag, L > 1 Tag.

| ID | Kat. | Schweregrad | Ort (Datei:Zeile) | Befund | Empfohlener Fix | Aufwand |
|---|---|---|---|---|---|---|
| N-01 | Doku / Konfiguration | **Hoch** | `src/main/resources/application.yaml:28`; `dev/keycloak/storagekeeper-realm.json:6-27`; `README.md:44-46, 67-69`; `docs/DOMAIN_MODEL.md:630` | Die Swagger UI ist auf den Client `storagekeeper-web` eingestellt. Der Dev-Realm (in M7 nicht geändert, letzter Commit `2d8134a`) kennt nur `storagekeeper-cli` mit `standardFlowEnabled: false`. Lokal endet „Authorize → keycloak“ daher mit „Client not found“. README („funktioniert lokal und im Deployment gleich“) und DOMAIN_MODEL.md („Entwicklung, Produktion“) widersprechen dem Realm. Der Weg „Token einfügen“ (`bearerAuth`) funktioniert weiter. *Belegt aus den Dateien, nicht im Browser nachgestellt.* | Client `storagekeeper-web` (Redirect `http://localhost:8080/swagger-ui/oauth2-redirect.html`, PKCE, Audience-Mapper) in den Dev-Realm aufnehmen; ein Test, der beide Realm-Dateien auf diesen Client prüft, verhindert das Auseinanderlaufen. | S |
| N-02 | Clean Code | Mittel | `adapter/in/web/ApiExceptionHandler.java:32-33, 87-91` | Jede `DataIntegrityViolationException` wird zu `409` mit „conflicts with data that was changed at the same time“. Das stimmt für Unique-/PK-Verletzungen aus Rennen, nicht aber für CHECK-, NOT-NULL- oder FK-Verletzungen – das sind Programmfehler, die so als scheinbarer Benutzerkonflikt erscheinen und nur als `WARN` geloggt werden. | Nur `DuplicateKeyException` bzw. SQL-State `23505` (unique_violation) auf `409` mappen; Rest an den Catch-all (`500`, `ERROR`-Log). | S |
| N-03 | Repo / Release | Mittel | `build.gradle.kts:10, 58-61`; `README.md:289-292`; `CHANGELOG.md:4`; Git-Tags | (a) Mit `version = "0.7.0"` überschreibt jeder Push auf `main` das Image `:0.7.0` – ein Versions-Tag, der sich ändert, untergräbt Reproduzierbarkeit (Rollback auf „0.7.0“ liefert nicht unbedingt den Release-Stand). Das ist dokumentiert, aber unüblich. (b) Der CHANGELOG führt `0.0.1`–`0.6.0`, es existiert aber kein einziger Git-Tag; diese Versionen gab es im Build nie (alle waren `0.0.1-SNAPSHOT`). | Zwischen Releases `0.8.0-SNAPSHOT` bauen und den Versions-Tag nur aus einem Git-Tag (`on: push: tags: v*`) veröffentlichen; historische Merge-Commits nachträglich taggen oder im CHANGELOG als „nicht getaggt“ kennzeichnen. | M |
| N-04 | Deployment | Niedrig | `deploy/postgres/init-databases.sh` (Modus `100644`) | Das Skript ist nicht ausführbar, deshalb **sourct** der Postgres-Entrypoint es („sourcing … init-databases.sh“ im Log). Sein `set -eu` gilt dann für den Rest des Entrypoints. Funktioniert heute (von mir geprüft), ist aber fragil. | `git update-index --chmod=+x deploy/postgres/init-databases.sh`. | S |
| N-05 | Deployment | Niedrig (unsicher) | `README.md:337-353` | Die Upgrade-Anleitung für Installationen vor 0.7.0 überträgt nur Tabellen und Schema an `keycloak`, keine Sequenzen, Views oder Funktionen; außerdem existiert der Superuser `postgres` bei alten Volumes nicht (`POSTGRES_PASSWORD` wirkt dort nicht). Ob Keycloak 26.4 eigene Sequenzen anlegt, habe ich nicht geprüft. | Anleitung einmal gegen ein M6-Volume durchspielen; ggf. `REASSIGN OWNED BY storagekeeper TO keycloak` in der Keycloak-DB verwenden. | M |
| N-06 | Clean Code | Niedrig | `PantryItemService.java:5, 9`; `PantryItemController.java:4, 13`; `HouseholdController.java:9`; `ShoppingListController.java:13`; `StorageLocationController.java:9`; `PantryItemJpaEntity.java:3-4` | M7 hat beim Aufräumen weitere Wildcard-Imports eingeführt (vermutlich IDE-Automatik); jetzt 9 im Produktivcode. Uneinheitlich zum Rest. | Einheitliche Regel, idealerweise per Formatter (C-06) erzwungen. | S |
| N-07 | Repo | Niedrig | `.github/dependabot.yml:4-17` | Dependabot deckt Gradle und Actions ab, nicht aber die Container-Images in `compose.yaml` und `deploy/compose.yaml` (`postgres:18-alpine`, `keycloak/keycloak:26.4`, `prom/prometheus:v3.5.0`) – gerade Keycloak braucht Sicherheitsupdates. | Ecosystem `docker-compose` für `/` und `/deploy` ergänzen. | S |
| N-08 | Repo | Niedrig | Commit-Historie `135e865`, `66e041a`, `ad6a57e` | „M7 (1/3)“ existiert zweimal, verbunden über einen Merge des eigenen Remote-Branches – dasselbe Muster wie bei M3/M4. | Vor dem Push `git pull --rebase` statt Merge; Historie nicht nachträglich umschreiben. | S |
| N-09 | Clean Code | Niedrig | `deploy/keycloak/storagekeeper-realm.json:10-11`; `ExpiringPantryItemResponse.java` | Kosmetik: Leerzeile mit Leerzeichen im `clients`-Array; `ExpiringPantryItemResponse` dupliziert nun alle 8 Felder von `PantryItemResponse` (Erweiterungen müssen an zwei Stellen erfolgen). | Leerzeile entfernen; optional `PantryItemResponse` per `@JsonUnwrapped` einbetten. | S |

## 5. Widerspruchsliste (Doku ↔ Realität), aktueller Stand

Alle 13 Widersprüche des ersten Berichts sind aufgelöst, bis auf Nr. 13 (V2-Kommentar, bewusst) und Nr. 11 in veränderter Form (siehe N-03). Neu:

| # | Doku sagt | Realität | Beleg |
|---|---|---|---|
| 1 | Swagger-Login über `storagekeeper-web` „funktioniert lokal und im Deployment gleich“ (`README.md:44-46`); Client existiert in „Entwicklung, Produktion“ (`DOMAIN_MODEL.md:630`) | Dev-Realm enthält nur `storagekeeper-cli` | `dev/keycloak/storagekeeper-realm.json:6-27` |
| 2 | `409` bedeutet „zwei gleichzeitige Requests kollidieren“ (`README.md:136-139`, `CHANGELOG.md:11`) | Auch CHECK-/NOT-NULL-/FK-Verletzungen ergeben diese `409` | `ApiExceptionHandler.java:87-91` |
| 3 | „Nach dem Merge eines Milestones … wird der Commit mit `v<Version>` getaggt“ (`CHANGELOG.md:4`), Versionen 0.0.1–0.6.0 aufgeführt | Keine Tags vorhanden; diese Versionen wurden nie gebaut | `git ls-remote --tags origin` (leer) |
| 4 | `Location`-Header nennt „die URL des neuen Objekts“ (`README.md:140`) | Für Lagerorte und manuelle Einträge gibt es unter dieser URL keinen `GET` (README:141 sagt das selbst) – ein Klick liefert `404`/`405` | `StorageLocationController.java`, `ShoppingListController.java` |

Geprüft und übereinstimmend: neue Umgebungsvariablen (`STORAGEKEEPER_API_DOCS_ENABLED`, `STORAGEKEEPER_DB_PASSWORD`, `KEYCLOAK_DB_PASSWORD`, `STORAGEKEEPER_URL`) in README, `.env.example`, Compose und `application.yaml`; Coverage-Schwellen README ↔ `build.gradle.kts`; Testtabelle README ↔ Testklassen; Realm-Eigenschaften README:330-334 ↔ Realm-Datei (praktisch geprüft); Jackson-Kommentar ↔ Abhängigkeitsbaum.

## 6. Priorisierter Maßnahmenplan

**Quick Wins**
1. N-01: `storagekeeper-web` in den Dev-Realm aufnehmen (blockiert den dokumentierten Login-Weg lokal).
2. N-02: nur Unique-Verletzungen auf `409`.
3. N-04: Init-Skript ausführbar machen; N-07: Dependabot für Compose-Images; N-09, D-10-Rest.
4. R-09: In den Repo-Settings „Private vulnerability reporting“ und Dependabot Alerts aktivieren, Branch Protection für `main` prüfen.

**Sollte bald**
1. N-03: Release-Ablauf auf Git-Tags umstellen und `v0.7.0` nach dem Merge setzen.
2. C-06 + N-06: Spotless und `.editorconfig`, im CI-Build prüfen.
3. N-05: Upgrade-Anleitung gegen ein echtes M6-Volume testen.
4. R-05-Rest: Qodana die Coverage aus dem CI-Job übernehmen lassen statt Tests doppelt laufen zu lassen.

**Langfristig**
1. K-01: Sprachkonzept für Client-Texte.
2. C-09: `Persistable` gegen Extra-SELECTs.
3. B-03-Rest: Dependency-Locking und Schwachstellen-Scan im Build.
4. S-06-Rest: optionales TLS-Profil (z. B. Caddy) in `deploy/compose.yaml`.

## 7. Checkliste

| Kategorie | Status | Anmerkung |
|---|---|---|
| 1. README und Projektdokumentation | geprüft | gesamter Diff gelesen; Swagger-Login im Browser nicht nachgestellt |
| 2. GitHub-Repository-Metadaten | teilweise | About/Topics/Tags per API; Settings nur manuell |
| 3. Imports, Abhängigkeiten und Build | geprüft | Clean Build, `-Xlint:all`, `dependencyInsight` |
| 4. Code-Qualität | geprüft | alle geänderten Produktivdateien gelesen |
| 5. Architektur | geprüft | neue ArchUnit-Regeln laufen grün |
| 6. Tests | geprüft | 236/236 grün, `unitTest` ohne Docker 170/170 |
| 7. Sicherheit und Konfiguration | teilweise | Prod-Realm und DB-Init praktisch geprüft; Komplett-Stack, Upgrade-Pfad und Schwachstellen-Scan nicht |
| 8. Konsistenz | geprüft | Abschnitt 5 |

## 8. Nachtrag: Nacharbeiten bis `b119691`

Geprüfter Verlauf auf `milestones/milestone7` nach `ee780fb`:

| Commit | Inhalt | Prüfergebnis |
|---|---|---|
| `c627969` | „M7 (4/4): Fix the local Swagger login and tighten conflicts and releases“ | **fehlerhaft:** Client `storagekeeper-web` im Produktions-Realm statt im Dev-Realm eingefügt (doppelt, Keycloak startet nicht: „Duplicate resource error“); Dev-Realm unverändert (`KeycloakRealmFilesTest` rot); `publishRegistry`-Zugangsdaten und `BP_JVM_VERSION` aus `bootBuildImage` entfernt; Testdatei `KeycloakRealFilesTest.java` falsch benannt |
| `3cd3cd4` + Merge `de553ba` | Korrektur der drei Punkte | Realms und `bootBuildImage` korrekt; **Build rot**: die alte Datei `KeycloakRealFilesTest.java` blieb neben `KeycloakRealmFilesTest.java` liegen → `error: duplicate class` |
| `b119691` | „Remove duplicate class“ | **grün** |

### 8.1 Was ausgeführt wurde (Stand `b119691`)

| Prüfung | Ergebnis |
|---|---|
| `./gradlew clean build jacocoTestReport` (JDK 25, Docker) | BUILD SUCCESSFUL in 59 s; **241 Tests, 0 Fehler, 0 übersprungen**; Abdeckung 98,3 % Zeilen, 84,0 % Branches; Coverage-Gate bestanden |
| Dev-Realm `dev/keycloak` in Keycloak 26.4 importiert | Import ok; `storagekeeper-web` mit Redirect `http://localhost:8080/swagger-ui/oauth2-redirect.html`, PKCE `S256`; Login-Endpunkt `…/auth?client_id=storagekeeper-web&…` antwortet `200` |
| Produktions-Realm `deploy/keycloak` importiert (`STORAGEKEEPER_URL=https://pantry.example.org`) | Import ok, genau ein Client `storagekeeper-web`, Redirect aus der Variable aufgelöst; Login-Endpunkt `200` |
| `./gradlew -q properties --property version` (wie im neuen CI-Schritt) | liefert `0.7.0` → Abgleich Git-Tag ↔ Version funktioniert |
| Neuer `KeycloakRealmFilesTest` | prüft beide Realm-Dateien gegen `application.yaml` (Client-ID, PKCE, Audience) und den Produktions-Realm auf „keine Nutzer, kein Passwort-Grant“ |

Nicht ausgeführt: Veröffentlichung nach GHCR (Job `image`, nur mit Push auf `main`/Tag), Browser-Login in der Swagger UI, Upgrade-Anleitung gegen ein M6-Volume.

### 8.2 Status der Befunde aus diesem Bericht

| ID | Status | Beleg |
|---|---|---|
| N-01 Swagger-Client im Dev-Realm | ✅ | `dev/keycloak/storagekeeper-realm.json`, per Import und Test geprüft |
| N-02 `409` für alle Constraints | ✅ | `ApiExceptionHandler`: nur SQL-State `23505`/`DuplicateKeyException` → `409`, Rest → `500`; drei neue Controller-Tests |
| N-03 Veränderlicher Versions-Tag | ✅ | `ci.yml`: `main` → `latest` + Kurz-Hash, Tag `v*` → Version (mit Abgleich); CHANGELOG markiert 0.0.1–0.6.0 als „nicht getaggt“. Tag `v0.7.0` folgt laut README nach dem Merge |
| N-04 Init-Skript nicht ausführbar | ✅ | Modus `100755` |
| N-05 Upgrade-Anleitung | ◐ | README ergänzt (nur Tabellen, kein `postgres`-Superuser bei alten Volumes), laut Commit gegen ein M6-Volume geprüft; von mir nicht nachgestellt |
| N-06 Wildcard-Imports | ❌ verschlechtert | jetzt 11 Produktivdateien (z. B. `ApiExceptionHandler`, `HouseholdJpaEntity`, `PantryItemJpaEntity`) |
| N-07 Dependabot für Compose-Images | ✅ | Ecosystem `docker-compose` für `/` und `/deploy` |
| N-08 Doppelte Commits | ❌ | erneut: „M7 (4/4)“ als `c627969` und `3cd3cd4` plus Merge `de553ba` |
| N-09 Kosmetik Realm / doppelte Felder | ✅ | Leerzeile entfernt; `ExpiringPantryItemResponse` nutzt `@JsonUnwrapped PantryItemResponse` (JSON bleibt flach, Test prüft `$[0].name`) |
| D-10 Rest „standard-bearer“ | ✅ | `SecurityConfiguration.java:30` |
| C-06 Formatter | ◐ | `.editorconfig` neu; kein Spotless/Checkstyle, CI prüft das Format nicht. Die Umstellung hat viele Dateien nur umformatiert (Commit `c627969`: +1604/−1277 Zeilen, überwiegend Einrückung) |
| R-05 Qodana | ◐ | weiterhin zweiter vollständiger Testlauf (`qodana_code_quality.yml:29`) |
| R-02 Community-Dateien | ◐ | Issue-/PR-Vorlagen, `CODEOWNERS`, `CODE_OF_CONDUCT.md` fehlen |
| R-09 Repo-Einstellungen | ❌ (manuell) | Private Vulnerability Reporting (für `SECURITY.md`), Dependabot Alerts, Branch Protection für `main` |
| B-03 Lockfile / Schwachstellen-Scan | ❌ | unverändert |
| C-09 `Persistable` | ❌ | unverändert |
| K-01 Sprachmix API | ❌ | unverändert |
| K-03 Benennung Einkaufsliste | ◐ | `ManualShoppingListRepository` vs. `SpringDataManualShoppingListEntryRepository` |
| S-06 TLS | ◐ | nur dokumentiert |
| A-04, C-08, D-11, S-04 | ➖ | bewusst unverändert, weiterhin vertretbar |

### 8.3 Fazit

M7 ist aus Audit-Sicht **bereit zum Merge**: kein offener Befund mit Schweregrad Kritisch oder Hoch mehr, Build und alle Tests grün, beide Realms in Keycloak geprüft.
Die noch offenen Mittel-Befunde (K-01 Sprachmix, B-03 Lockfile/Scan, R-02 Community-Dateien, R-05 Qodana) betreffen Konsistenz und Repo-Pflege, nicht das Verhalten.
Nach dem Merge `v0.7.0` taggen und die erste Veröffentlichung im Actions-Log kontrollieren (Job `image`, Tags `latest`/Kurz-Hash bzw. `0.7.0`).

Für einen späteren Milestone bleiben: Formatter mit CI-Prüfung (C-06, beseitigt auch N-06), Qodana ohne zweiten Testlauf (R-05), `Persistable` (C-09), Sprachkonzept der API (K-01),
Lockfile und Schwachstellen-Scan (B-03), Community-Dateien (R-02) sowie die Repo-Einstellungen (R-09). Gegen doppelte Commits (N-08) hilft `git pull --rebase` vor dem Push.
