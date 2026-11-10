# Sicherheit

## Eine Schwachstelle melden

Bitte **kein öffentliches Issue** anlegen. Stattdessen über GitHub melden:
*Security → Report a vulnerability* in diesem Repository (private Meldung, nur für die Maintainer sichtbar).

Hilfreich sind: betroffene Version oder Commit, Schritte zum Nachstellen und die mögliche Auswirkung.
Eine erste Antwort kommt in der Regel innerhalb einer Woche.

## Unterstützte Versionen

Sicherheitskorrekturen gibt es nur für den aktuellen Stand von `main` und das zuletzt veröffentlichte Image
(`ghcr.io/max-ziel/storagekeeper:latest`).

## Hinweise zum Betrieb

- Für ein öffentlich erreichbares Deployment `deploy/` verwenden, nicht `compose.yaml` aus dem Wurzelverzeichnis:
  Dessen Realm enthält Testnutzer mit bekannten Passwörtern.
- Den Actuator-Port (`8081`) nie veröffentlichen, er ist ohne Anmeldung erreichbar.
- Vor die App und Keycloak gehört ein Reverse Proxy mit TLS.