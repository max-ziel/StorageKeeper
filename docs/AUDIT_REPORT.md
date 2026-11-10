# Audit-Report StorageKeeper – offene Punkte

Stand: 2026-11-10, `main` @ `135d5f1` (M7 per Squash-Merge `9c22ced`, danach sechs Dependabot-Updates: Spring Boot 4.1.2, Gradle 9.9.0,
Keycloak 26.7, Prometheus v3.16.0, `gradle/actions` v6, Qodana-Action 2026.2).

Dieser Bericht enthält **nur noch offene Punkte**. Alles, was in den Audits zu M6 und M7 gefunden und inzwischen behoben wurde, ist entfernt;
die vollständigen früheren Fassungen stehen in der Git-Historie dieser Datei und von `docs/AUDIT_REPORT_M7.md`.
Die Befund-IDs der früheren Berichte bleiben erhalten, neue Befunde haben das Präfix `N-`.

## 1. Zusammenfassung

Das Projekt ist in gutem Zustand: Build, alle Tests, Coverage-Gate und ArchUnit sind grün, die CI auf `main` ist grün und veröffentlicht das Image.
**Kein offener Befund hat den Schweregrad Kritisch oder Hoch.** Offen sind der Abschluss des Releases 0.7.0, Repo-Einstellungen,
Pflegethemen (Formatter, Qodana, Lockfile/Scan) und einige Konsistenzpunkte.

## 2. Was ausgeführt wurde (Stand `135d5f1`)

| Prüfung | Ergebnis |
|---|---|
| `./gradlew clean build jacocoTestReport` (JDK 25, Docker) | BUILD SUCCESSFUL in 59 s; **241 Tests, 0 Fehler, 0 übersprungen**; 98,3 % Zeilen, 84,0 % Branches. Erster Versuch scheiterte an `429 Too Many Requests` von Maven Central (Umgebung, kein Projektfehler) |
| `--warning-mode all` | eine Deprecation unter Gradle 9.9, siehe N-10 |
| GitHub Actions auf `main` | alle CI-Runs seit dem Merge grün; Job `image` in Run 42826845887 erfolgreich, Veröffentlichung nach GHCR funktioniert |
| Dev- und Produktions-Realm in **Keycloak 26.7** importiert | beide ok; `storagekeeper-web` vorhanden, Login-Endpunkt mit PKCE antwortet `200` |
| `dependencyInsight jackson-databind` | springdoc 3.2.0 fordert weiterhin Jackson 2.21.7, der Override auf 2.21.8 in `build.gradle.kts` ist also weiter nötig |
| `git ls-remote --tags origin` | **keine Tags** |

Nicht geprüft: Browser-Login in der Swagger UI, Upgrade eines M6-Volumes (siehe N-05), Repo-Einstellungen (nur manuell möglich), Schwachstellen-Scan der Abhängigkeiten (kein Tool konfiguriert).

## 3. Offene Befunde

Schweregrade: **Kritisch** (Build/Sicherheit/Datenverlust), **Hoch** (falsches Verhalten, irreführende Doku), **Mittel** (Wartbarkeit, Inkonsistenz), **Niedrig** (Stil, Kosmetik).
Aufwand: S < 1 h, M ≤ 1 Tag, L > 1 Tag.

| ID | Schweregrad | Ort | Befund | Empfohlener Fix | Aufwand |
|---|---|---|---|---|---|
| N-11 | Mittel | Git-Tags; `build.gradle.kts:9`; `README.md:303-305` | Release 0.7.0 ist nicht abgeschlossen: M7 ist auf `main`, aber `v0.7.0` fehlt. Damit gibt es noch kein Image `:0.7.0`, und die README-Empfehlung „im Betrieb `STORAGEKEEPER_VERSION` auf eine Version setzen“ ist nicht umsetzbar. Die Version steht noch auf `0.7.0`; laut README soll sie zu Beginn von M8 auf `0.8.0-SNAPSHOT`. | `git tag v0.7.0 135d5f1` (oder `9c22ced`) und pushen, im Actions-Log den Job `image` mit Tag `0.7.0` prüfen; danach auf dem M8-Branch `0.8.0-SNAPSHOT`. | S |
| R-09 | Mittel | Repo-Einstellungen (manuell) | Nicht per API prüfbar: **Private vulnerability reporting** (sonst läuft der Meldeweg aus `SECURITY.md` ins Leere), **Dependabot alerts / security updates**, **Branch Protection** für `main` mit Pflicht-Check `CI / build`, Sichtbarkeit des GHCR-Pakets (README verlangt „public“). | In *Settings → Code security* und *Settings → Branches* aktivieren. | S |
| R-05 | Mittel | `.github/workflows/qodana_code_quality.yml:29` | Qodana lässt alle 241 Tests (mit Testcontainers) ein zweites Mal laufen, nur um Coverage zu erzeugen, die der CI-Job schon hat. Doppelte Laufzeit und doppelte Docker-Hub-/Maven-Zugriffe. | Qodana als Job in `ci.yml` nach `build` und das Coverage-Artefakt per `actions/download-artifact` übernehmen, oder Qodana ohne Coverage laufen lassen. | S |
| B-03 | Mittel | `build.gradle.kts` | Kein Dependency-Locking und kein Schwachstellen-Scan im Build. Dependabot schlägt Versionen vor, meldet aber nur über die (ungeprüften) Alerts, ob eine *genutzte* Version verwundbar ist. | GitHub *Dependency Review* in PRs und Alerts aktivieren (R-09); optional `dependencyLocking { lockAllConfigurations() }` und OWASP Dependency-Check. | M |
| K-01 | Mittel | `HouseholdController.java:18`, `PantryItemController.java:24` u. a. (`@Tag`), `OpenApiConfiguration` vs. `application/*Exception.java` | Sprachmix in der API: OpenAPI-Tags und -Beschreibung Deutsch, Fehlermeldungen im `detail` Englisch („Create or join a household first.“); Default-Lagerort „Vorrat“ Deutsch. | Zielsprache für alles, was ein Client sieht, festlegen (vermutlich Deutsch) und vereinheitlichen; Tests, die auf Texte prüfen, anpassen. | M |
| R-02 | Niedrig | `.github/` | Es fehlen Issue- und PR-Vorlagen, `CODEOWNERS` und `CODE_OF_CONDUCT.md` (`SECURITY.md`, Mitwirken-Abschnitt und Dependabot sind vorhanden). Für ein Ein-Personen-Projekt optional. | Mindestens eine PR-Vorlage mit Checkliste (CI grün, CHANGELOG, Doku) und `CODEOWNERS`. | S |
| C-06 / N-06 | Niedrig | 11 Dateien: `adapter/in/web/{ApiExceptionHandler,HouseholdController,PantryItemController,ShoppingListController,StorageLocationController}.java`, `adapter/out/persistence/{HouseholdJpaEntity,ManualShoppingListEntryJpaEntity,PantryItemJpaEntity}.java`, `application/{HouseholdService,PantryItemService,ShoppingListService}.java` | Kein Formatter mit CI-Prüfung, nur `.editorconfig`. Folge: Wildcard-Imports in 11 Dateien (vor M7: 2), Formatierung hängt von der IDE ab (in M7 führte das zu Commits, die überwiegend Einrückung ändern). | Spotless (z. B. Palantir Java Format) einführen, einmal `spotlessApply` als eigener Commit, `spotlessCheck` in `check`. | M |
| N-10 | Niedrig | `build.gradle.kts:94` | `val unitTest by tasks.registering(Test::class)` ist seit Gradle 9.6 deprecated und wird in Gradle 10 entfernt. Ein Dependabot-Update auf Gradle 10 würde den Build brechen. | `val unitTest = tasks.register<Test>("unitTest") { … }`. | S |
| K-03 | Niedrig | `application/port/out/ManualShoppingListRepository.java` vs. `adapter/out/persistence/SpringDataManualShoppingListEntryRepository.java`; `ShoppingListRepository` (für `ShoppingListEntry`) | Uneinheitliche Benennung der Einkaufslisten-Repositories (mal mit, mal ohne `Entry`). | Einheitlich `…ShoppingListEntryRepository`. | S |
| C-09 | Niedrig | alle `Jpa*Repository.save` | Entities haben vorab vergebene UUIDs und kein `@Version`/`Persistable`; Spring Data macht deshalb vor jedem Insert ein `merge` mit zusätzlichem `SELECT`. Nicht gemessen; bei der Datenmenge eines Haushalts unkritisch. | `Persistable<UUID>` mit `isNew()` oder eine `@Version`-Spalte (Flyway-Migration). | M |
| S-06 | Niedrig | `deploy/compose.yaml:66` (`KC_HTTP_ENABLED`) | Der Stack spricht nur HTTP; TLS ist dokumentiert (README „HTTPS“), aber nicht mitgeliefert. Der Produktions-Realm verlangt `sslRequired: external`, d. h. ohne Proxy funktioniert der Login nur über `localhost`. | Optionales Profil `tls` mit Caddy (automatische Zertifikate) in `deploy/compose.yaml`. | M |
| N-05 | Niedrig (ungeprüft) | `README.md:353-373` | Die Upgrade-Anleitung für Installationen vor 0.7.0 wurde laut Commit gegen ein M6-Volume getestet (Keycloak 26.4), von mir nicht nachgestellt. Mit dem Dependabot-Update springt ein Upgrade jetzt zusätzlich von Keycloak 26.4 auf 26.7 (Keycloak migriert seine DB dabei selbst); Zeile 367 nennt noch 26.4. | Einmal mit einem M6-Volume und Keycloak 26.7 durchspielen, Zeile 367 anpassen. | S |
| B-05 | Niedrig | 8 Exception-Klassen in `application/` und `domain/DomainValidationException.java` | `javac -Xlint:all` meldet nur noch 8 × `[serial]`. Unkritisch, die Exceptions werden nicht serialisiert. | Optional `@Serial private static final long serialVersionUID = 1L;` oder `-Xlint:-serial`. | S |

### Bewusst akzeptiert (kein Handlungsbedarf)

| ID | Punkt | Begründung |
|---|---|---|
| S-04 | Actuator-Port `8081` ohne Anmeldung | nur im internen Netz, in README und `SECURITY.md` dokumentiert |
| A-04 | Default-Lagerort „Vorrat“ in der Domain | deutschsprachige App; wird mit K-01 ggf. neu bewertet |
| C-08 | `ShoppingListService.checkOff` lädt den Artikel zweimal | vernachlässigbar, hält die Autorisierung an einer Stelle |
| D-11 | Kommentar `StorageLocationId.Default` in `V2__create_storage_location.sql` | ausgeführte Migrationen werden nicht geändert (Flyway-Checksumme) |

## 4. Reihenfolge der Änderungen

Jeder Schritt ist ein eigener, kleiner Commit bzw. PR mit grüner CI. Die Reihenfolge folgt zwei Regeln: erst das, was andere Schritte voraussetzt
oder absichert, und **den Formatter-Lauf allein und vor allen Code-Änderungen**, damit spätere Diffs lesbar bleiben.

| Schritt | Befunde | Was | Warum an dieser Stelle | Aufwand |
|---|---|---|---|---|
| **1** | N-11 | `v0.7.0` auf `main` taggen und pushen; im Actions-Log prüfen, dass der Job `image` das Image `:0.7.0` veröffentlicht. | Schließt M7 ab; Voraussetzung dafür, dass der Betrieb eine feste Version nutzen kann. Unabhängig von allem anderen. | S |
| **2** | R-09, (B-03) | Repo-Einstellungen: Private vulnerability reporting, Dependabot alerts und security updates, Branch Protection für `main` (Pflicht-Check `CI / build`), GHCR-Paket öffentlich. | Nur Klicks, kein Code. Branch Protection sichert alle folgenden Schritte ab; die Alerts decken den Schwachstellen-Teil von B-03 bereits ab. | S |
| **3** | – | Neuen Branch `milestones/milestone8` anlegen, Version auf `0.8.0-SNAPSHOT`, CHANGELOG-Abschnitt `[0.8.0] – unveröffentlicht`. | Laut README-Release-Ablauf; ab jetzt heißen lokale Builds nicht mehr wie ein Release. | S |
| **4** | N-10 | `tasks.registering` durch `tasks.register<Test>("unitTest")` ersetzen. | Klein und isoliert; verhindert, dass ein Dependabot-PR auf Gradle 10 den Build bricht. | S |
| **5** | R-05 | Qodana an den CI-Job hängen bzw. Coverage-Artefakt übernehmen statt Tests doppelt laufen zu lassen. | Halbiert die CI-Zeit für alle folgenden PRs; reine Workflow-Änderung. | S |
| **6** | C-06, N-06 | Spotless einführen. Commit 6a: nur Plugin und Konfiguration. Commit 6b: `spotlessApply` über das ganze Projekt, **ohne** andere Änderungen. Commit 6c: `spotlessCheck` in `check`. | Muss vor allen weiteren Code-Änderungen kommen: Danach sind Diffs frei von Formatierungsrauschen, und die Wildcard-Imports verschwinden automatisch. | M |
| **7** | K-03 | Repositories einheitlich `…ShoppingListEntryRepository` benennen (IDE-Refactoring). | Reine Umbenennung nach dem Formatter, damit der Diff nur Namen zeigt. | S |
| **8** | B-05 | `serialVersionUID` oder `-Xlint:-serial`; optional `-Xlint:all -Werror` im Build, damit neue Warnungen auffallen. | Kosmetik, profitiert aber vom sauberen Stand nach 6 und 7. | S |
| **9** | B-03 (Rest), R-02 | Dependency Review Action für PRs; optional Dependency-Locking. PR-Vorlage mit Checkliste und `CODEOWNERS`. | Repo-Pflege; die PR-Vorlage hilft ab hier bei jedem Milestone-PR. | S–M |
| **10** | K-01 | Sprache der Client-Texte festlegen und vereinheitlichen (Fehlermeldungen, OpenAPI), Tests mit Textprüfungen anpassen; dabei A-04 neu bewerten. | Erste Änderung mit sichtbarer API-Wirkung → im CHANGELOG unter „Geändert“; kommt nach den Aufräumschritten, damit der Diff nur Texte enthält. | M |
| **11** | C-09 | `Persistable<UUID>` (oder `@Version` mit Flyway-Migration V8) und Test, dass ein Insert ohne vorheriges `SELECT` läuft. | Verhaltens- bzw. Schemaänderung im Persistenz-Adapter; braucht eigene Tests, daher spät und einzeln. | M |
| **12** | S-06, N-05 | Optionales TLS-Profil (Caddy) in `deploy/compose.yaml`; Upgrade-Anleitung mit Keycloak 26.7 gegen ein M6-Volume durchspielen und README:367 aktualisieren. | Deployment-Themen zum Schluss; erfordern einen manuellen End-to-End-Test des Stacks. | M |
| **13** | – | M8 abschließen: CHANGELOG `[0.8.0]`, Version `0.8.0`, PR, Merge, Tag `v0.8.0`, danach `0.9.0-SNAPSHOT`. | Gleicher Release-Ablauf wie in Schritt 1 und 3. | S |

Schritte 1–2 gehen sofort und ohne Branch. Schritte 3–9 sind Aufräumarbeit ohne sichtbare Änderung für Nutzer und passen zusammen in M8;
10–12 können wahlweise in M8 oder einen eigenen Milestone, weil sie API-Texte, Persistenz bzw. Deployment ändern.

## 5. Checkliste

| Kategorie | Status | Anmerkung |
|---|---|---|
| 1. README und Projektdokumentation | geprüft | offen nur README:367 (N-05) |
| 2. GitHub-Repository-Metadaten | teilweise | Tags, Actions per API; Einstellungen nur manuell (R-09) |
| 3. Imports, Abhängigkeiten und Build | geprüft | Clean Build grün; N-10, B-03, B-05 |
| 4. Code-Qualität | geprüft | C-06/N-06, K-03, C-09 |
| 5. Architektur | geprüft | ArchUnit grün, keine offenen Befunde |
| 6. Tests | geprüft | 241/241 grün, Coverage-Gate erfüllt, keine offenen Befunde |
| 7. Sicherheit und Konfiguration | teilweise | beide Realms mit Keycloak 26.7 geprüft; Scan und Repo-Einstellungen offen (B-03, R-09), S-06 |
| 8. Konsistenz | geprüft | K-01, K-03 |
