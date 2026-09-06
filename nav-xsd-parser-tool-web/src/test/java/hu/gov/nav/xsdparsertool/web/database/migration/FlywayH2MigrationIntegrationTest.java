package hu.gov.nav.xsdparsertool.web.database.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayH2MigrationIntegrationTest {

    private static final int EXPECTED_LATEST_VERSION = 29;

    @Test
    void allH2MigrationsMustRunAndCreateCurrentSchema() throws Exception {
        Database database = database();
        Flyway flyway = database.flyway();

        MigrateResult result = flyway.migrate();

        assertEquals(EXPECTED_LATEST_VERSION, result.migrationsExecuted);
        assertNotNull(flyway.info().current());
        assertEquals(String.valueOf(EXPECTED_LATEST_VERSION), flyway.info().current().getVersion().toString());

        try (Connection connection = database.connection()) {
            assertTableExists(connection, "XML_FILE");
            assertTableExists(connection, "XML_FILE_REVISION");
            assertTableExists(connection, "XML_FILE_DIFF_ENTRY");
            assertTableExists(connection, "PARTNER");
            assertTableExists(connection, "SYSTEM_CONFIGURATION");
            assertTableExists(connection, "SYSTEM_SECRET");
            assertTableExists(connection, "TRUSTED_CERTIFICATE");
            assertTableExists(connection, "USER_PARTNER_PERMISSION");
            assertTableExists(connection, "USER_TAX_PERMISSION_RULE");

            assertColumnExists(connection, "XML_FILE", "PARTNER_ID");
            assertColumnExists(connection, "XML_FILE", "PARTNER_IMPORT_STATUS");
            assertColumnExists(connection, "XML_FILE", "PARTNER_IMPORT_MESSAGE");
            assertColumnMissing(connection, "APP_USER", "AUTHENTICATION_SOURCE");
        }
    }

    @Test
    void secondMigrationRunMustBeIdempotent() {
        Database database = database();
        Flyway flyway = database.flyway();

        MigrateResult first = flyway.migrate();
        MigrateResult second = flyway.migrate();

        assertEquals(EXPECTED_LATEST_VERSION, first.migrationsExecuted);
        assertEquals(0, second.migrationsExecuted);
        assertEquals(String.valueOf(EXPECTED_LATEST_VERSION), flyway.info().current().getVersion().toString());
    }

    @Test
    void migrationDataMustContainCurrentPermissionsAndRemoveObsoleteXpathDatabaseSettings() throws Exception {
        Database database = database();
        database.flyway().migrate();

        try (Connection connection = database.connection()) {
            assertEquals(1, count(connection,
                    "select count(*) from app_role where role_code = 'XML_INDEX_CONFIG_MANAGE'"));
            assertTrue(count(connection,
                    "select count(*) from system_configuration where config_key = 'nav.xsdparsertool.security.password-policy.minimum-length'") > 0);
            assertEquals(0, count(connection,
                    "select count(*) from system_configuration where config_key in (" +
                            "'nav.xsdparsertool.xpath-validator.db.path'," +
                            "'nav.xsdparsertool.xpath-validator.db.username'," +
                            "'nav.xsdparsertool.xpath-validator.db.password')"));
            assertEquals(0, count(connection,
                    "select count(*) from system_secret where secret_key = 'nav.xsdparsertool.xpath-validator.db.password'"));
            assertEquals(0, count(connection,
                    "select count(*) from system_configuration where config_key = 'nav.xsdparsertool.security.authentication-mode' " +
                            "or config_key like 'nav.xsdparsertool.security.active-directory.%' " +
                            "or config_key like 'nav.xsdparsertool.ad-role.%'"));
        }
    }

    @Test
    void latestPermissionTablesMustEnforceUniquenessAndForeignKeys() throws Exception {
        Database database = database();
        database.flyway().migrate();

        try (Connection connection = database.connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into app_user(username, password_hash, enabled, created_at) values ('p15-user', 'x', true, current_timestamp)");
            statement.executeUpdate("insert into partner(name, tax_number, active, created_at) values ('P15 partner', '12345678', true, current_timestamp)");

            long userId = scalarLong(connection, "select id from app_user where username = 'p15-user'");
            long partnerId = scalarLong(connection, "select id from partner where tax_number = '12345678'");

            statement.executeUpdate("insert into user_partner_permission(user_id, partner_id, created_at, created_by) values (" +
                    userId + "," + partnerId + ",current_timestamp,'test')");

            assertTrue(sqlFails(connection,
                    "insert into user_partner_permission(user_id, partner_id, created_at, created_by) values (" +
                            userId + "," + partnerId + ",current_timestamp,'duplicate')"));
            assertTrue(sqlFails(connection,
                    "insert into user_partner_permission(user_id, partner_id, created_at, created_by) values (" +
                            userId + ",999999,current_timestamp,'bad-fk')"));

            statement.executeUpdate("insert into user_tax_permission_rule(user_id, rule_type, tax_number, sort_order, created_at, created_by) values (" +
                    userId + ",'ALLOW','12345678',1,current_timestamp,'test')");
            assertEquals(1, count(connection, "select count(*) from user_tax_permission_rule where user_id = " + userId));
        }
    }

    private Database database() {
        String name = "p15_" + UUID.randomUUID().toString().replace("-", "");
        Path databaseDirectory;
        try {
            databaseDirectory = Files.createTempDirectory("m2m-h2-migration-");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Nem hozható létre ideiglenes H2 tesztkönyvtár.", ex);
        }
        String databasePath = databaseDirectory.resolve(name).toAbsolutePath().toString().replace('\\', '/');
        String url = "jdbc:h2:file:" + databasePath + ";AUTO_SERVER=TRUE";
        String testCredential = "p" + UUID.randomUUID().toString().replace("-", "");
        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", testCredential)
                .locations("classpath:db/migration/H2")
                .load();
        return new Database(url, testCredential, flyway);
    }

    private int count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private long scalarLong(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private boolean sqlFails(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            return false;
        } catch (SQLException expected) {
            return true;
        }
    }

    private void assertTableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(null, null, tableName.toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
            assertTrue(tables.next(), "Hiányzó tábla: " + tableName);
        }
    }

    private void assertColumnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(null, null,
                tableName.toUpperCase(Locale.ROOT), columnName.toUpperCase(Locale.ROOT))) {
            assertTrue(columns.next(), "Hiányzó oszlop: " + tableName + "." + columnName);
        }
    }

    private void assertColumnMissing(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(null, null,
                tableName.toUpperCase(Locale.ROOT), columnName.toUpperCase(Locale.ROOT))) {
            assertFalse(columns.next(), "Nem várt oszlop: " + tableName + "." + columnName);
        }
    }

    private record Database(String url, String testCredential, Flyway flyway) {
        Connection connection() throws SQLException {
            return DriverManager.getConnection(url, "sa", testCredential);
        }
    }
}
