# Audit-Report StorageKeeper

Stand: 2026-11-10, Commit `12897b7` (Branch `main` = `claude/tender-carson-7e05u0`). Reines Lese-Audit, am Code wurde nichts geändert.

## 1. Zusammenfassung

Insgesamt ein **überdurchschnittlich sauberes Projekt**: konsequente Ports-&-Adapters-Struktur mit ArchUnit-Prüfung, reiches Domain-Modell mit Value Objects,
getrennte JPA-Entities, Flyway-Migrationen mit Migrationstest, 214 grüne Tests mit 98,2 % Zeilen- und 81,5 % Branch-Abdeckung, sehr ausführliche Doku.
Die Schwächen liegen vor allem bei **Doku, die dem Code hinterherläuft**, beim **Deployment-Default** und bei **Randfällen der Fehlerbehandlung**.

**Top 5:**
1. **S-01 (Hoch):** `deploy/compose.yaml` importiert den Dev-Realm in die Produktion – mit den Testnutzern `alice/alice`, `bob/bob`, `carol/carol` und Password-Grant.
2. **D-01 / A-01 (Hoch):** `DOMAIN_MODEL.md` behauptet, die Application hänge „nur vom JDK, nicht von Spring“ ab – sie nutzt `@Service`, `@Transactional`, `@Component`; ArchUnit prüft das nicht.
3. **C-01 (Mittel, unsicher):** Check-then-act ohne Behandlung von `DataIntegrityViolationException` → bei parallelen Requests `500` statt `409`.
4. **D-02 / D-05 / D-06 (Hoch/Mittel):** Falsche Javadoc/Doku: `ExpiringPantryItemResponse` hat nicht „dieselben Felder“, Javadoc von `AddManualShoppingListEntryRequest` und `PantryItemFellBelowMinimum` beschreibt das Falsche.
5. **R-03 / R-04 (Mittel):** Keine Releases/Tags, Version seit M0 `0.0.1-SNAPSHOT`; CI läuft bei jedem PR doppelt, Qodana mit JDK 21 statt 25.

**Bereits gut:** klare Schichtung, Domain frei von Frameworks (geprüft), Autorisierung im Application-Layer mit 404 statt 403 für fremde IDs,
DB-Constraints als zweite Verteidigungslinie (FK `(storage_location_id, household_id)`), testbare Zeit über `Clock`, ProblemDetail für alle Fehler inkl. 401,
getrennter Actuator-Port, strukturierte Logs, Buildpacks-Image, keine Secrets im Repo oder in der Historie gefunden.

## 2. Was ausgeführt wurde

| Befehl / Prüfung | Ergebnis |
|---|---|
| `./gradlew build` (erster Versuch) | **Fehlgeschlagen**: Toolchain Java 25 nicht lokal vorhanden, Foojay-Download vom Netzwerk-Proxy blockiert (`403`). Umgebungsproblem, kein Projektfehler. |
| `apt-get install openjdk-25-jdk-headless` (nur im Container) | OpenJDK 25.0.5 installiert |
| `./gradlew build -Dorg.gradle.java.installations.paths=…` (2. Versuch) | Fehlgeschlagen: Maven Central `429 Too Many Requests` (transient) |
| 3. Versuch, ohne Docker-Daemon | Kompiliert, `bootJar` ok; **213 Tests, 60 rot** – exakt alle Testcontainers-Tests (kein Docker). Belegt T-04. |
| `dockerd` gestartet, `docker pull postgres:18-alpine` (nach `429`-Retries) | ok |
| `./gradlew clean build jacocoTestReport` | **BUILD SUCCESSFUL in 57 s, 214 Tests, 0 Fehler, 0 übersprungen.** JaCoCo: Instruktionen 98,0 %, Branches 81,5 %, Zeilen 98,2 %, Methoden 98,0 %, Klassen 100 % |
| `compileJava compileTestJava` mit `-Xlint:all` (per Init-Skript im Scratchpad, Repo unverändert) | 14 Warnungen Main, 1 Test (siehe B-04, B-05) |
| `java -jar build/libs/storagekeeper-0.0.1-SNAPSHOT.jar` | Startet (`Starting StorageKeeperApplication v0.0.1-SNAPSHOT using Java 25`) – package-private `static void main` funktioniert |
| `docker compose config -q` für `compose.yaml` und `deploy/compose.yaml` | beide gültig; `deploy` bricht ohne Passwörter wie dokumentiert ab |
| Secret-Scan Arbeitsbaum + `git log --all -p` (Regex auf key/secret/token/password) | nur Dev-/Test-Werte (`storagekeeper`, `ops-test-token`, Realm-Testnutzer) |
| GitHub-Metadaten (Search-API), Releases, Actions-Runs (GitHub MCP) | Beschreibung leer, Topics `household`, `organisation`, Lizenz MIT, 0 Releases, 0 Tags, letzte CI- und Qodana-Runs auf `main` grün |

**Nicht geprüft:**
- `./gradlew bootRun` mit Keycloak und `deploy/compose.yaml up` (Keycloak-/App-Image nicht gezogen; Docker Hub rate-limitiert) → README-Schritte „Anmeldung“, „Deployment“ **ungeprüft** (nur statisch gegen Code/Config verglichen).
- `./gradlew bootBuildImage` (braucht Buildpack-Images aus dem Netz) → **ungeprüft**; laut Actions-Historie läuft der `image`-Job auf `main` grün.
- Schwachstellen-Scan der Abhängigkeiten: kein Tool konfiguriert (kein OWASP Dependency-Check, kein Dependabot) → **ungeprüft**.
- Qodana-Ergebnisse: nur in Qodana Cloud einsehbar → **ungeprüft**.
- Repo-Einstellungen (Branch Protection, Secret Scanning, Package-Sichtbarkeit von `ghcr.io/max-ziel/storagekeeper`) → manuell zu prüfen, siehe Kategorie 2.

## 3. Befunde je Kategorie

Schweregrade: **Kritisch** (Build/Sicherheit/Datenverlust), **Hoch** (falsches Verhalten, irreführende Doku), **Mittel** (Wartbarkeit, Inkonsistenz), **Niedrig** (Stil, Kosmetik).
Aufwand: S < 1 h, M ≤ 1 Tag, L > 1 Tag.

### 3.1 README und Projektdokumentation

| ID | Schweregrad | Ort (Datei:Zeile) | Befund | Empfohlener Fix | Aufwand |
|---|---|---|---|---|---|
| D-01 | Hoch | `docs/DOMAIN_MODEL.md:513-514` | „Die Application hängt weiterhin nur vom JDK ab, nicht von Spring.“ Falsch: `HouseholdService.java:17-18` (`@Service`, `@Transactional`), `CurrentHousehold.java:7` (`@Component`), ebenso alle Services. | Aussage korrigieren („nur von Spring-Annotationen für Wiring/Transaktionen“) oder A-01 umsetzen. | S |
| D-02 | Hoch | `docs/DOMAIN_MODEL.md:862-863` | „`ExpiringPantryItemResponse` enthält dieselben Felder wie `PantryItemResponse` plus …“ – es fehlen `minimumAmount` und `belowMinimum` (`ExpiringPantryItemResponse.java:16-25` vs. `PantryItemResponse.java:17-18`). | Doku korrigieren oder die beiden Felder ergänzen (API-Entscheidung). | S |
| D-03 | Mittel | `docs/DOMAIN_MODEL.md:812-835` | Diagramm „Ablauf eines Requests“ zeigt den Stand M2: ohne `CurrentHousehold.require()`, `belongsTo`, Event-Publishing. Widerspricht den Diagrammen weiter oben. | Diagramm aktualisieren oder durch Verweis auf „Wo die Prüfung stattfindet“ ersetzen. | S |
| D-04 | Mittel | `docs/DOMAIN_MODEL.md:210, 232, 429`; `PantryItemRepository.java:17` | Veraltete Zeitbezüge: „später aus der Datenbank“, „sobald es weitere gibt (z. B. Lagerorte in M3)“, „In M5/M6 können weitere Reaktionen …“, „in M1 in memory, from M2 on with JPA“. | Auf heutigen Stand formulieren. | S |
| D-05 | Mittel | `adapter/in/web/AddManualShoppingListEntryRequest.java:13-16` | Javadoc ist von `CheckOffStockPurchaseRequest` kopiert: nennt Endpunkt `…/check-off` und `@param amount the amount bought`. Tatsächlich `POST /api/shopping-list/manual-entries`. | Javadoc korrigieren. | S |
| D-06 | Mittel | `domain/PantryItemFellBelowMinimum.java:4` | Javadoc „has **reached** its minimum stock; before the change it was not“ – beschreibt das Gegenteil (das ist `PantryItemReachedMinimum`). | „fell below its minimum stock; before it was not“. | S |
| D-07 | Mittel | `README.md:302-317` | Paketstruktur unvollständig: Paket `config` (`ClockConfiguration`) und `application.port` (`PageQuery`, `ResultPage`) fehlen. | Baum ergänzen. | S |
| D-08 | Mittel | `README.md` (gesamt) | Fehlende Abschnitte: Lizenz (MIT-`LICENSE` existiert, wird nicht erwähnt), Mitwirken/Kontakt, eigener Abschnitt „Tests“ (Teststrategie nur als Kommentar in Zeile 16), Changelog/Versionen. | Kurze Abschnitte „Tests“, „Lizenz“, „Mitwirken“ ergänzen. | S |
| D-09 | Niedrig | `README.md:11` | „ein laufendes Docker (für PostgreSQL)“ – `bootRun` startet auch Keycloak, Tests brauchen Docker zwingend (siehe T-04). | „für PostgreSQL, Keycloak und die Tests“. | S |
| D-10 | Niedrig | `CheckOffStockPurchaseUseCase.java:9` („Checks iff“), `PantryItemController.java:60` („nema“), `PantryItemSpecifications.java:9` („arte“), `PantryItem.java:58` („hte“), `Quantity.java:23` („give“), `SecurityConfiguration.java:53` („standard-bearer“), `ShoppingListEntryId.java:8` (leeres `@param value`), `ChangeMinimumAmountRequest.java:9` (Satz ohne Trenner), `ManualShoppingListEntryId.java:7` | Tippfehler/unvollständige Javadoc. | Korrigieren. | S |
| D-11 | Niedrig | `src/main/resources/db/migration/V2__create_storage_location.sql:10` | Kommentar verweist auf `StorageLocationId.Default`, das es nicht mehr gibt (und nie so hieß: `DEFAULT`). | **Migration nicht ändern** (Flyway-Checksumme!). Nur in `DOMAIN_MODEL.md` den historischen Kontext belassen. Kein Fix nötig. | – |
| D-12 | Niedrig | `StorageKeeperApplication.java:6`, `config/ClockConfiguration.java:10`, Getter in `domain/*` | Fehlende Javadoc an einigen öffentlichen Typen/Methoden; die meisten öffentlichen Schnittstellen (Ports, Domain) sind dagegen gut dokumentiert. | Klassen-Javadoc für die beiden Klassen ergänzen. | S |

### 3.2 GitHub-Repository-Metadaten

| ID | Schweregrad | Ort | Befund | Empfohlener Fix | Aufwand |
|---|---|---|---|---|---|
| R-01 | Mittel | GitHub „About“ | `description: null`, `homepage: ""`, Topics nur `household`, `organisation` – kein Bezug zu README („Pantry-App – Java 25, Spring Boot 4 …“). | Beschreibung aus README-Zeile 4 übernehmen; Topics z. B. `pantry`, `spring-boot`, `java`, `hexagonal-architecture`, `keycloak`. | S |
| R-02 | Mittel | Repo-Root, `.github/` | Es fehlen `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, Issue-/PR-Templates, `CODEOWNERS`, `dependabot.yml`. `.github/` enthält nur `workflows/`. | Mindestens `SECURITY.md` und `dependabot.yml` (Gradle + GitHub Actions); Rest je nach Anspruch. | S–M |
| R-03 | Mittel | `build.gradle.kts:10`; GitHub Releases/Tags | Version seit M0 `0.0.1-SNAPSHOT`, obwohl M0–M6 abgeschlossen; 0 Tags, 0 Releases, kein `CHANGELOG`. Der CI-Image-Tag „Version“ (`:0.0.1-SNAPSHOT`) wird bei jedem Push auf `main` überschrieben und ist damit wertlos. | Semver einführen (z. B. `0.6.0` für M6), Tags pro Milestone, Release-Notes/CHANGELOG. | S |
| R-04 | Mittel | `.github/workflows/ci.yml:3-5` | `on: push` + `pull_request` ohne Branch-Filter → jeder PR-Commit läuft doppelt (belegt: Runs #72 push und #74 pull_request für SHA `5a39950`). | `push` auf `main` beschränken, PRs über `pull_request`. | S |
| R-05 | Mittel | `.github/workflows/qodana_code_quality.yml:22-26, 14, 37` | JDK 21 statt 25 (Toolchain wird nur dank Foojay nachgeladen); `permissions: contents: write` breiter als nötig; `pr-mode: false`, obwohl Zeile 19-20 einen PR-Checkout für PR-Analyse vorbereiten. Tests laufen zusätzlich zur CI noch einmal komplett. | JDK 25; `contents: read`; Coverage-Artefakt aus CI wiederverwenden oder Qodana in CI integrieren. | S |
| R-06 | Niedrig | `.github/workflows/ci.yml:28` | Build-Job führt `test jacocoTestReport` statt `build` aus → `bootJar`/`assemble` wird bei PRs nicht geprüft; keine Coverage-Schwelle. | `./gradlew build jacocoTestReport`. | S |
| R-07 | Niedrig | `README.md:2` | Nur Qodana-Badge, kein CI-Badge. | CI-Badge ergänzen. | S |
| R-08 | Niedrig | `.gitignore`; Git-Historie | `.qodana/` (wird in CI angelegt), `*.log`, Root-`.env` nicht ignoriert. Historie enthält das gelöschte `graphify-out/` (~1,6 MB Blobs, Commits `a6df341`, `32c7a61`, `4f0909b`) sowie doppelte Commits („M3 (1/3)“, „M4 (1/2)“ je zweimal). | `.gitignore` ergänzen. Historie nicht umschreiben (öffentliches Repo), nur zur Kenntnis. | S |
| R-09 | – | Repo-Einstellungen (manuell prüfen) | Nicht per API prüfbar: Branch Protection für `main` (Pflicht-Checks CI), Secret Scanning/Push Protection, Dependabot Alerts, Sichtbarkeit des GHCR-Pakets (README:263-264 verlangt „public“), Wiki aktiviert, aber leer? | In den Repo-Settings kontrollieren. | S |

### 3.3 Imports, Abhängigkeiten und Build

| ID | Schweregrad | Ort (Datei:Zeile) | Befund | Empfohlener Fix | Aufwand |
|---|---|---|---|---|---|
| B-01 | Mittel | `build.gradle.kts:6` | Plugin `io.swagger.core.v3.swagger-gradle-plugin` wird angewendet, aber nirgends konfiguriert (keine `resolve`-Task-Konfiguration); OpenAPI kommt zur Laufzeit von springdoc. Tote Build-Abhängigkeit. | Plugin entfernen. | S |
| B-02 | Mittel (unsicher) | `build.gradle.kts:72` | `extra["jackson-2-bom.version"] = "2.21.8"` ohne Kommentar, warum (vermutlich CVE-Fix für eine transitive Jackson-2-Abhängigkeit). Ob Jackson 2 überhaupt noch transitiv genutzt wird, ist ungeklärt (Boot 4 nutzt Jackson 3). Zur Klärung: `./gradlew dependencyInsight --dependency com.fasterxml.jackson.core`. | Begründung als Kommentar ergänzen oder Override entfernen. | S |
| B-03 | Mittel | `build.gradle.kts` | Kein Dependency-Locking (`gradle.lockfile`), kein automatisches Update (Dependabot/Renovate), kein Schwachstellen-Scan. Plugin-Versionen sind fest, Bibliotheken über die Boot-BOM – reproduzierbar, solange die BOM gleich bleibt. | Dependabot (R-02) + optional `dependencyLocking { lockAllConfigurations() }`; OWASP Dependency-Check oder GitHub Dependency Review. | M |
| B-04 | Mittel | `adapter/out/persistence/ShoppingListJpaEntity.java:17` | Datei heißt `ShoppingListJpaEntity.java`, enthält aber die Klasse `ShoppingListEntryJpaEntity`. `javac -Xlint` meldet 5× `[auxiliaryclass] … should not be accessed from outside its own source file` (`JpaShoppingListRepository.java:26,33,50`, `SpringDataShoppingListEntryRepository.java:11,23`). | Datei in `ShoppingListEntryJpaEntity.java` umbenennen. | S |
| B-05 | Niedrig | `domain/Household.java:31`; `UserLoggingContextFilter.java:40`; 7 Exception-Klassen in `application/`; `HouseholdServiceTest.java:65` | Weitere `-Xlint:all`-Warnungen: `[this-escape]` (Konstruktor ruft überschreibbares `join` auf, Klasse nicht `final`), `[try]` (gewollt), `[serial]` ×7, `[unchecked]` im Test. | `Household` `final` machen oder privaten Helfer nutzen; `[try]` mit `@SuppressWarnings("try")`; Rest optional. | S |
| B-06 | Niedrig | `PantryItemController.java:4`, `PantryItemService.java:5`, `PantryItemControllerTest.java:7`, `ShoppingListControllerTest.java:4` | Wildcard-Imports `port.in.*`, sonst überall explizite Imports. | Einheitlich explizit. | S |
| B-07 | Niedrig | `settings.gradle.kts:2`, `build.gradle.kts:13-17` | Build hängt an Foojay-Auto-Provisioning; in Netzen ohne Zugriff auf foojay.io schlägt er fehl (hier verifiziert). | README-Hinweis: JDK 25 lokal installieren oder `-Dorg.gradle.java.installations.paths=…`. | S |

Keine fehlenden oder zirkulären Imports, keine Referenzen auf nicht existierende Klassen/Ressourcen gefunden (Build und `ddl-auto: validate` laufen). Konfigurationsschlüssel `storagekeeper.time-zone` existiert (`application.yaml`). Kein toter Produktivcode gefunden (alle Klassen von Tests erreicht, Klassenabdeckung 100 %).

### 3.4 Code-Qualität (Clean Code)

| ID | Schweregrad | Ort (Datei:Zeile) | Befund | Empfohlener Fix | Aufwand |
|---|---|---|---|---|---|
| C-01 | Mittel (unsicher, nicht reproduziert) | `StorageLocationService.java:28-35`, `HouseholdService.java:40-51, 60-74, 83-89`, `ApiExceptionHandler.java` | Check-then-act: Name-/Mitgliedschafts-/Code-Prüfung und Insert sind nicht atomar (`StorageLocationService.add` hat nicht einmal `@Transactional`). Bei parallelen Requests greifen die DB-Constraints (`uq_storage_location_household_name`, PK `household_member.user_id`, `UNIQUE invite_code`); für `DataIntegrityViolationException` gibt es keinen Handler → `500` statt `409`. Zur Bestätigung: Test mit zwei parallelen `POST /api/storage-locations`. | Handler für `DataIntegrityViolationException` → `409`, oder Exception im Repository-Adapter in die passende Application-Exception übersetzen; `@Transactional` ergänzen. | M |
| C-02 | Mittel | `ApiExceptionHandler.java:37-40` | Jede `IllegalArgumentException` wird zu `400` mit Original-Message – auch solche aus Bibliotheken oder Programmierfehlern; interne Texte gelangen an den Client. `IllegalStateException` (z. B. `PantryItemService.java:134`, `Household.java:47`) wird gar nicht behandelt → generische `500`. | Eigene `DomainValidationException` (oder Value-Object-spezifische) einführen und nur diese auf `400` mappen. | M |
| C-03 | Mittel | `domain/ManualShoppingListEntry.java:32-40, 80` | `addedAt` wird nicht auf `null` geprüft (anders als `ShoppingListEntry.java:20`); ein `null` fiele erst als DB-Fehler auf. Mengen-Prüfung vor den Null-Checks; `equals` per `getClass()` statt `instanceof` wie in allen anderen Entities. | `Objects.requireNonNull(addedAt, …)`; Reihenfolge und `equals` angleichen. | S |
| C-04 | Mittel | `StorageLocationService.java:28-40`; `PantryItemService.java:76-91`; `JpaPantryItemRepository.java:49`; `JpaShoppingListRepository.java:43` | Uneinheitliche Transaktionsgrenzen: `StorageLocationService` ganz ohne `@Transactional`, Lesemethoden in `PantryItemService` ohne `readOnly`, während `HouseholdService.getCurrent` und `ShoppingListService.getShoppingList` `readOnly` haben; zusätzlich vereinzelt `@Transactional` auf Adapter-Methoden. | Regel festlegen: Transaktionen nur an Use-Case-Methoden, Lesen mit `readOnly = true`. | S |
| C-05 | Niedrig | `ManualShoppingListEntryJpaEntity.java:31` (`length = 100`), `HouseholdJpaEntity.java:38` (`length = 9`), `InviteCode.java:40-41` (`8`, `4`) | Magic Numbers; an anderen Stellen wird `ItemName.MAX_LENGTH` verwendet (`PantryItemJpaEntity.java:25`). | Konstanten nutzen bzw. `InviteCode.LENGTH`/`GROUP_SIZE` einführen. | S |
| C-06 | Niedrig | `ItemName.java:18`, `Quantity.java:18,26`, `ShoppingListEntryId.java:22`, `PantryItemFilter.java:15`, `ExpiringPantryItemResponse.java:23,29`, `JpaPantryItemRepository.java:35,56`, `JpaStorageLocationRepository.java:27`, `PantryItemJpaEntity.java:43`, `PageQuery.java:15`, `ResultPage.java:20`, `AddManualShoppingListEntryRequest.java:22` | Formatierungsfehler (doppelte Leerzeichen, `page < 0 )`, `size -1`, Leerzeile in Record-Komponenten, doppelte Leerzeilen). Kein Formatter/Linter konfiguriert (kein Spotless, Checkstyle, `.editorconfig`); nur Qodana in der Cloud. | Spotless (z. B. Palantir/Google Java Format) + `.editorconfig`, im CI-Build prüfen. | M |
| C-07 | Niedrig | `PantryItemSpecifications.java:11`; `adapter/in/web/*Request.java`, `*Response.java` | Sichtbarkeit uneinheitlich: `PantryItemSpecifications` ist `public`, alle anderen Persistence-Klassen package-private; DTO-Records `public`, Controller package-private. | `PantryItemSpecifications` package-private (`final`). | S |
| C-08 | Niedrig | `ShoppingListService.java:89-94` | `checkOff` lädt den Artikel zweimal (Filter, dann erneut in `restock`). | Akzeptabel; bei Bedarf Household-Check in `restock` belassen und den Vorab-Load sparen. | S |
| C-09 | Niedrig (unsicher) | `Jpa*Repository.save(...)` | Entities haben vorab vergebene UUIDs, kein `@Version`/`Persistable` → Spring Data hält jede Entity für „nicht neu“ und macht `merge` (zusätzliches `SELECT` vor jedem Insert). Nicht gemessen. | `Persistable<UUID>` mit `isNew` oder `@Version`-Spalte. | M |
| C-10 | Niedrig | `PantryItemController.java:54` vs. `StorageLocationController.java:26-30`, `HouseholdController.java:36-40`, `ShoppingListController.java:61-66` | Nur `POST /api/pantry-items` liefert einen `Location`-Header; die anderen `201`-Antworten nicht. | Einheitlich `Location` setzen (ggf. mit `ServletUriComponentsBuilder`). | S |

Positiv: keine verschluckten Exceptions, kein auskommentierter Code, keine TODO/FIXME-Altlasten, durchgehend Null-Checks in Konstruktoren, MDC wird per try-with-resources wieder entfernt, keine sensiblen Daten in Logs außer der pseudonymen `user.id` (siehe S-07).

### 3.5 Architektur (Clean Architecture)

| ID | Schweregrad | Ort (Datei:Zeile) | Befund | Empfohlener Fix | Aufwand |
|---|---|---|---|---|---|
| A-01 | Mittel | `application/*Service.java`, `CurrentHousehold.java:7,15`; `ArchitectureTest.java:25-29` | Application-Layer hängt von Spring ab (`@Service`, `@Component`, `@Transactional`). Die ArchUnit-Regel `domainIsFrameworkFree` prüft nur `domain`. Pragmatisch vertretbar, widerspricht aber der Doku (D-01). | Entweder bewusst akzeptieren und dokumentieren (+ ArchUnit-Regel: `application` nur `org.springframework.stereotype`/`transaction`), oder Beans in einer `@Configuration` im Adapter/Config verdrahten. | S (Doku) / M (Umbau) |
| A-02 | Mittel | `ArchitectureTest.java:15-23`; `config/ClockConfiguration.java` | Paket `config` gehört zu keiner Schicht und wird nicht geprüft; `application.port` (`PageQuery`, `ResultPage`) ist weder `in` noch `out`. | `config` als eigene Schicht/Adapter in die Regel aufnehmen; `PageQuery`/`ResultPage` bewusst dokumentieren. | S |
| A-03 | Mittel | `domain/ShoppingListEntry.java:10-14`; `UpdateShoppingListUseCase.java:8-15`; `ShoppingListService.java:66-78` | `ShoppingListEntry` kennt seinen Haushalt nicht; `UpdateShoppingListUseCase` ist ein öffentlicher Inbound-Port ohne Autorisierung (dokumentiert, `DOMAIN_MODEL.md` „Einkaufsliste aus Events“). Jeder künftige treibende Adapter, der ihn aufruft, umgeht die Haushaltsprüfung. | Port in ein internes Paket verschieben oder ArchUnit-Regel: nur `adapter.in.event` darf ihn nutzen. | S |
| A-04 | Niedrig | `domain/StorageLocation.java:13` | Deutscher Default-Name `"Vorrat"` fest in der Domain – koppelt Fachmodell an eine Sprache. | Akzeptabel für eine deutschsprachige App; sonst über Konfiguration/Application setzen. | S |

Positiv: Abhängigkeitsrichtung Adapter → Application → Domain wird von `onionArchitecture()` erzwungen und hält; Domain ohne Spring/Jakarta (geprüft); Persistenz mit eigenen JPA-Entities; Konfiguration und Secrets ausschließlich über Umgebungsvariablen. Die dokumentierte Paketstruktur entspricht bis auf D-07 der Realität.

### 3.6 Tests

| ID | Schweregrad | Ort | Befund | Empfohlener Fix | Aufwand |
|---|---|---|---|---|---|
| T-01 | Mittel | `build.gradle.kts:78-83` | JaCoCo erzeugt nur einen Report; keine `jacocoTestCoverageVerification`-Schwelle, die Abdeckung kann unbemerkt sinken. | Schwelle (z. B. Zeilen 90 %, Branches 75 %) in `check` einhängen. | S |
| T-02 | Mittel | – | Keine Tests für Nebenläufigkeit/Constraint-Verletzungen (C-01) und keine für `IllegalStateException`-Pfade (C-02). | Integrationstest mit parallelen Requests bzw. direktem Constraint-Verstoß. | M |
| T-03 | Niedrig | `README.md:16` | Teststrategie im README nur als Kommentar; Details stehen verteilt in `DOMAIN_MODEL.md`. | Abschnitt „Tests“ (Unit / Web-Slice / JPA mit Testcontainers / End-to-End / ArchUnit, Befehl, Coverage-Report-Pfad). | S |
| T-04 | Niedrig | alle Testcontainers-Tests | Ohne Docker schlagen 60 von 213/214 Tests fehl (verifiziert); es gibt keine Möglichkeit, nur die schnellen Unit-Tests laufen zu lassen. | `@Tag("integration")` + eigene Gradle-Task oder `@Testcontainers(disabledWithoutDocker = true)`. | S |
| T-05 | Niedrig | `HouseholdServiceTest.java:65`; `SpringSecurityCurrentUserProvider.java:27` | Unchecked-Warnung im Test; der Zweig „kein angemeldeter Nutzer“ ist ungetestet (1 Zeile Coverage-Lücke). | Kleiner Unit-Test. | S |

Ergebnis: **214 Tests, alle grün, keine übersprungenen oder deaktivierten Tests**, kein `Thread.sleep`, keine auskommentierten Tests. Struktur: Domain-Unit-Tests, Service-Tests mit Mockito, `@WebMvcTest` mit echter `SecurityConfiguration`, `@DataJpaTest` mit Testcontainers, End-to-End-Tests (`HouseholdIsolationIntegrationTest`, `ShoppingListFlowIntegrationTest`, `OperationsIntegrationTest`), Migrationstest V6 → V7 und ArchUnit. Testnamen beschreiben Verhalten (`householdsDoNotSeeEachOthersData`), Arrange/Act/Assert erkennbar, feste `Clock` statt Systemzeit.

### 3.7 Sicherheit und Konfiguration

| ID | Schweregrad | Ort (Datei:Zeile) | Befund | Empfohlener Fix | Aufwand |
|---|---|---|---|---|---|
| S-01 | **Hoch** | `deploy/compose.yaml:43, 59`; `dev/keycloak/storagekeeper-realm.json:3, 10-14, 37, 46, 55` | Die Produktions-Compose-Datei importiert den Realm aus `dev/`, der laut eigener Beschreibung „Nur für lokale Entwicklung“ ist: Testnutzer mit Passwort = Benutzername und ein Public Client mit Resource-Owner-Password-Grant (`directAccessGrantsEnabled: true`, in OAuth 2.1 abgeschafft). Absicherung hängt allein am README-Hinweis (`README.md:296-297`), die Nutzer manuell zu löschen. | Eigenen Produktions-Realm `deploy/keycloak/` ohne Nutzer, Client mit Authorization Code + PKCE; Dev-Realm nur in `compose.yaml`. | M |
| S-02 | Mittel | `deploy/compose.yaml:52`; `deploy/postgres/init-keycloak.sql:2` | App und Keycloak nutzen denselben DB-Nutzer `storagekeeper` (Owner beider Datenbanken). Kompromittierung der App ⇒ Zugriff auf Keycloak-Daten (Passwort-Hashes) und umgekehrt. | Eigener DB-Nutzer und Passwort für Keycloak. | S |
| S-03 | Mittel | `adapter/in/web/SecurityConfiguration.java:39` | Swagger UI und `/v3/api-docs` sind auch in Produktion öffentlich. | Über Property abschaltbar machen (`springdoc.api-docs.enabled=false` im Deployment) oder hinter Auth. | S |
| S-04 | Mittel | `SecurityConfiguration.java:73-87`; `application.yaml` (`show-details: always`) | Actuator-Port ohne jede Authentifizierung inkl. Health-Details; Schutz nur über Netzwerk (bewusst, gut dokumentiert). Keine zweite Verteidigungslinie, falls der Port doch veröffentlicht wird. | Optional Basic-Auth für Prometheus oder `show-details: when-authorized`. | S |
| S-05 | Niedrig | `deploy/.env.example:3-4` | Platzhalter `change-me` wird nicht abgewiesen; Stack startet damit. | Kurzer Check im README oder Entrypoint, alternativ keinen Default-Wert vorgeben. | S |
| S-06 | Niedrig | `deploy/compose.yaml:57` (`KC_HTTP_ENABLED`) | HTTP ohne TLS; README:298-299 verlangt Reverse Proxy, Compose bringt keinen mit. | Optionales Caddy-Profil oder deutlicher Warnhinweis in der Compose-Datei. | M |
| S-07 | Niedrig | `UserLoggingContextFilter.java:40` | Jede Logzeile enthält `user.id` (`sub`) – pseudonymisierte personenbezogene Daten (DSGVO); Aufbewahrung nicht dokumentiert. | Hinweis zur Log-Aufbewahrung im README „Betrieb“. | S |
| S-08 | – | – | **Keine** echten Secrets im Arbeitsbaum oder in der Historie gefunden; `deploy/.env` ist ignoriert; Qodana-Token kommt aus `secrets`. Eingaben werden per Bean Validation und Value Objects geprüft; SQL nur über JPA/Criteria (keine String-Konkatenation) → kein SQL-Injection-Risiko gefunden; keine Dateipfade aus Eingaben, keine Java-Deserialisierung. | – | – |
| S-09 | ungeprüft | Abhängigkeiten | Kein Schwachstellen-Scan konfiguriert oder lokal ausführbar (siehe B-03). | GitHub Dependabot Alerts aktivieren / Dependency-Check. | S |

Beispiel-Konfiguration `deploy/.env.example` ist vorhanden und vollständig (alle in `deploy/compose.yaml` referenzierten Variablen). Für die lokale Entwicklung ist keine nötig.

### 3.8 Konsistenz über alles hinweg

| ID | Schweregrad | Ort | Befund | Empfohlener Fix | Aufwand |
|---|---|---|---|---|---|
| K-01 | Mittel | `HouseholdController.java:21`, `PantryItemController.java:24`, `OpenApiConfiguration.java:26` vs. `NoHouseholdException.java:9`, `ApiExceptionHandler.java:73` u. a. | Sprachmix in der API: OpenAPI-Tags/Beschreibung Deutsch, Fehlermeldungen (`detail`) Englisch, Default-Lagerort Deutsch (`StorageLocation.java:13`), Realm-Anzeige Deutsch. Code/Bezeichner einheitlich Englisch (gut). | Zielsprache für Client-Texte festlegen und vereinheitlichen. | M |
| K-02 | Mittel | `.github/workflows/qodana_code_quality.yml:22-26` vs. `build.gradle.kts:15`, `ci.yml:17-21`, `README.md:4,11` | Java-Version: überall 25, nur Qodana 21. | Siehe R-05. | S |
| K-03 | Niedrig | `ManualShoppingListRepository` vs. `SpringDataManualShoppingListEntryRepository`; `ShoppingListRepository` (für Entries); `ShoppingListJpaEntity.java` vs. Klasse `ShoppingListEntryJpaEntity` | Uneinheitliche Benennung der Einkaufslisten-Typen. | Einheitlich `…ShoppingListEntry…`. | S |
| K-04 | Niedrig | Projektname | „StorageKeeper“ (Repo, README), `storagekeeper` (Artefakt, Image), fachlich „Pantry“/„Vorrat“ im Code; GitHub-Topics passen zu keinem davon. | Im README einen Satz zur Namensgebung; Topics anpassen (R-01). | S |
| K-05 | Niedrig | `V5__create_shopping_list_entry.sql:4` | SQL-Stil: `primary key` klein, sonst `PRIMARY KEY`. | Nur in künftigen Migrationen beachten (bestehende nicht ändern). | – |

## 4. Widerspruchsliste (Doku ↔ Realität)

| # | Doku sagt | Realität | Beleg |
|---|---|---|---|
| 1 | „Die Application hängt weiterhin nur vom JDK ab, nicht von Spring.“ (`DOMAIN_MODEL.md:513-514`) | Services nutzen `@Service`, `@Component`, `@Transactional` | `HouseholdService.java:17-18`, `CurrentHousehold.java:7` |
| 2 | `ExpiringPantryItemResponse` hat „dieselben Felder wie `PantryItemResponse`“ + 2 (`DOMAIN_MODEL.md:862`) | `minimumAmount`, `belowMinimum` fehlen | `ExpiringPantryItemResponse.java:16-25` |
| 3 | Request-Ablauf: Controller → Service → Repository → Domain (`DOMAIN_MODEL.md:812-835`) | Zusätzlich `CurrentHousehold.require()`, `belongsTo`, Event-Publishing | `PantryItemService.java:93-130, 149-153` |
| 4 | Javadoc: Request für `POST …/check-off`, `amount` = gekaufte Menge | Request für `POST /api/shopping-list/manual-entries` | `AddManualShoppingListEntryRequest.java:13-16`, `ShoppingListController.java:61-63` |
| 5 | `PantryItemFellBelowMinimum`: „has reached its minimum stock“ | Event wird beim **Unterschreiten** ausgelöst | `PantryItemFellBelowMinimum.java:4`, `PantryItem.java:119-120` |
| 6 | Paketstruktur ohne `config` und `application.port` (`README.md:302-317`) | Beide Pakete existieren | `config/ClockConfiguration.java`, `application/port/PageQuery.java` |
| 7 | Realm „Nur für lokale Entwicklung“ (`storagekeeper-realm.json:3,10`) | Wird in der Produktions-Compose importiert | `deploy/compose.yaml:59` |
| 8 | „Voraussetzungen: JDK 25 und Docker (für PostgreSQL)“ (`README.md:11`) | Docker auch für Keycloak und zwingend für 60 Tests | `compose.yaml:13-21`, Testlauf 1 |
| 9 | Repository „in M1 in memory, from M2 on with JPA“ (`PantryItemRepository.java:17`) | Nur JPA-Adapter vorhanden | `adapter/out/persistence/` |
| 10 | Java 25 (README, Build, CI) | Qodana-Workflow richtet JDK 21 ein | `qodana_code_quality.yml:22-26` |
| 11 | Image-Tag „Version“ (`README.md:258-262`) | Immer `0.0.1-SNAPSHOT`, wird bei jedem Push überschrieben | `build.gradle.kts:10`, `ci.yml:64` |
| 12 | GitHub-About: keine Beschreibung, Topics `household`, `organisation` | README: „Pantry-App – Java 25, Spring Boot 4, Ports & Adapters“ | GitHub-API, `README.md:4` |
| 13 | V2-Kommentar: „its id is `StorageLocationId.Default` in the code“ | Konstante existiert nicht mehr (seit M5) | `V2__create_storage_location.sql:10` (nicht ändern) |

Geprüft und **übereinstimmend**: Ports 8080/8081/8180/9090, Umgebungsvariablen (`STORAGEKEEPER_ISSUER_URI`, `STORAGEKEEPER_MANAGEMENT_PORT`, `STORAGEKEEPER_LOG_FORMAT`, `.env`-Variablen), alle REST-Endpunkte, Statuscodes und Fehler-Mapping, Paging-Grenzen (20/100), `days` 0–365, Einheiten, Metrikname `storagekeeper.domain.events`, Image-Name, Health-Gruppen, Migrationsbeschreibungen V1–V7, Versionen (Spring Boot 4.1.1, Gradle 9.4.1, PostgreSQL 18, Keycloak 26.4).

## 5. Priorisierter Maßnahmenplan

**Quick Wins (je < 1 h, wenig Risiko)**
1. Doku-Korrekturen: D-01, D-02, D-03, D-04, D-05, D-06, D-07, D-09, D-10, D-12 (ein Commit „docs“).
2. Build-Aufräumen: B-01 (Swagger-Plugin raus), B-02 (Jackson-Override begründen/entfernen), B-04 (Datei umbenennen), B-06 (Wildcard-Imports), C-07.
3. CI: R-04 (Trigger), R-05/K-02 (Qodana JDK 25, `contents: read`), R-06 (`build` statt `test`), R-07 (Badge), R-08 (`.gitignore`).
4. Kleine Code-Fixes: C-03 (`addedAt`-Check, `equals`), C-05 (Konstanten), C-04 (Transaktionsannotationen vereinheitlichen), B-05 (`Household` final).
5. GitHub-About und Topics setzen (R-01), Repo-Settings prüfen (R-09).

**Sollte bald**
1. S-01: Produktions-Realm ohne Testnutzer und ohne Password-Grant.
2. S-02: Eigener DB-Nutzer für Keycloak.
3. C-01 + C-02 + T-02: Constraint-Verletzungen auf `409` mappen, eigene Domain-Validierungs-Exception, Tests dazu.
4. R-03: Versionierung, Tags und CHANGELOG ab M6.
5. T-01, T-04: Coverage-Schwelle, Integrationstests taggen.
6. B-03/R-02: Dependabot, `SECURITY.md`, Schwachstellen-Scan.
7. S-03, S-07, D-08, T-03: Swagger in Produktion abschaltbar, README-Abschnitte Tests/Lizenz/Mitwirken/Log-Aufbewahrung.

**Langfristig**
1. A-01/A-02/A-03: ArchUnit-Regeln für Application-Layer, `config` und internen Event-Port.
2. C-06: Formatter (Spotless) + `.editorconfig` im CI.
3. K-01: Sprachkonzept für Client-Texte (ggf. i18n).
4. C-09, C-10, S-04, S-06: `Persistable`, einheitliche `Location`-Header, Actuator-Härtung, TLS-Profil in Compose.

## 6. Checkliste

| Kategorie | Status | Anmerkung |
|---|---|---|
| 1. README und Projektdokumentation | geprüft | Anleitungen `bootRun`/Deployment nur statisch geprüft (Keycloak-/App-Image nicht gestartet) |
| 2. GitHub-Repository-Metadaten | teilweise | About/Topics/Lizenz/Releases/Actions per API geprüft; Branch Protection, Security-Settings, GHCR-Sichtbarkeit nur manuell prüfbar |
| 3. Imports, Abhängigkeiten und Build | geprüft | Clean Build mit JDK 25 erfolgreich, `-Xlint:all` ausgewertet; `bootBuildImage` nicht ausgeführt |
| 4. Code-Qualität | geprüft | Alle 115 Java-Produktivdateien gelesen; C-01 und C-09 nicht reproduziert (unsicher) |
| 5. Architektur | geprüft | ArchUnit läuft grün; Abweichungen siehe A-01 bis A-04 |
| 6. Tests | geprüft | 214/214 grün, Coverage 98,2 % Zeilen / 81,5 % Branches |
| 7. Sicherheit und Konfiguration | teilweise | Secret-Scan inkl. Historie, Konfiguration geprüft; Schwachstellen-Scan der Abhängigkeiten nicht möglich (kein Tool) |
| 8. Konsistenz | geprüft | Widerspruchsliste Abschnitt 4 |
