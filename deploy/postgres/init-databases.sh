#!/bin/sh
# Runs only on the first start with an empty volume. Creates one database and one login per service, so the app
# cannot read Keycloak's data (password hashes) and Keycloak cannot read the pantry. Neither is a superuser;
# the superuser "postgres" (POSTGRES_PASSWORD) is only for administration.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
     -v app_password="$STORAGEKEEPER_DB_PASSWORD" \
     -v keycloak_password="$KEYCLOAK_DB_PASSWORD" <<'SQL'
CREATE USER storagekeeper WITH PASSWORD :'app_password';
CREATE DATABASE storagekeeper OWNER storagekeeper;
REVOKE ALL ON DATABASE storagekeeper FROM PUBLIC;

CREATE USER keycloak WITH PASSWORD :'keycloak_password';
CREATE DATABASE keycloak OWNER keycloak;
REVOKE ALL ON DATABASE keycloak FROM PUBLIC;
SQL