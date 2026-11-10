package io.github.maxziel.storagekeeper.adapter.out.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks migration V7 with data as it existed before M5: the schema is migrated up to V6, filled, and then
 * migrated to the latest version. Uses its own database, because the other tests always start from the latest
 * schema.
 */
@Tag("integration")
class HouseholdMigrationTest {

    private static final String OLD_DEFAULT_LOCATION = "00000000-0000-0000-0000-000000000001";
    private static final String MIGRATED_HOUSEHOLD = "00000000-0000-0000-0000-000000000001";

    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @BeforeAll
    static void startDatabase() {
        postgres.start();
    }

    @AfterAll
    static void stopDatabase() {
        postgres.stop();
    }

    @BeforeEach
    void emptyDatabase() {
        flyway(null).clean();
    }

    @Test
    void existingDataMovesToAHouseholdWithARandomInviteCode() throws SQLException {
        flyway("6").migrate();
        execute("""
                INSERT INTO storage_location (id, name) VALUES ('00000000-0000-0000-0000-0000000000c1', 'Keller');
                INSERT INTO pantry_item (id, name, amount, unit, storage_location_id, minimum_amount)
                VALUES ('00000000-0000-0000-0000-0000000000a1', 'Reis', 1, 'KILOGRAM', '%s', 2),
                       ('00000000-0000-0000-0000-0000000000a2', 'Wein', 6, 'PIECE',
                        '00000000-0000-0000-0000-0000000000c1', 0);
                INSERT INTO shopping_list_entry (id, pantry_item_id, added_at)
                VALUES (gen_random_uuid(), '00000000-0000-0000-0000-0000000000a1', now());
                INSERT INTO manual_shopping_list_entry (id, name, added_at)
                VALUES (gen_random_uuid(), 'Kerzen', now());
                """.formatted(OLD_DEFAULT_LOCATION));

        flyway(null).migrate();

        assertThat(query("SELECT id || ' ' || name FROM household"))
                .containsExactly(MIGRATED_HOUSEHOLD + " Haushalt");
        assertThat(query("SELECT invite_code FROM household").getFirst())
                .matches("[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}");
        assertThat(query("SELECT count(*) FROM household_member")).containsExactly("0");
        assertThat(query("SELECT DISTINCT household_id FROM pantry_item")).containsExactly(MIGRATED_HOUSEHOLD);
        assertThat(query("SELECT DISTINCT household_id FROM storage_location")).containsExactly(MIGRATED_HOUSEHOLD);
        assertThat(query("SELECT household_id FROM manual_shopping_list_entry")).containsExactly(MIGRATED_HOUSEHOLD);
        assertThat(query("SELECT count(*) FROM shopping_list_entry")).containsExactly("1");
        // The old base location stays the default location; "Keller" is an ordinary one.
        assertThat(query("SELECT name || ' ' || default_location FROM storage_location ORDER BY name"))
                .containsExactly("Keller false", "Vorrat true");
    }

    @Test
    void emptyDatabaseGetsNoHouseholdAndNoLocation() throws SQLException {
        flyway(null).migrate();

        assertThat(query("SELECT count(*) FROM household")).containsExactly("0");
        assertThat(query("SELECT count(*) FROM storage_location")).containsExactly("0");
    }

    /** @param target version to migrate to, {@code null} for the latest */
    private static Flyway flyway(String target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private static void execute(String sql) throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static List<String> query(String sql) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                rows.add(result.getString(1));
            }
        }
        return rows;
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
