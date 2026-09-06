package hu.gov.nav.xsdparsertool.web.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/** A setup DB-specifikus bootstrap-feloldásának regressziós tesztjei. */
class SetupDatabaseConfigurationTest {

    @Test
    void mysqlSetupBuildsConsistentBootstrapValues() {
        SetupService.DatabaseSetup setup = SetupService.DatabaseSetup.resolve(
                "MYSQL", "db.example", "3307", "m2m", "m2m", "nav_user", "secret", Path.of("data"));

        assertEquals("MYSQL", setup.type());
        assertEquals("com.mysql.cj.jdbc.Driver", setup.driver());
        assertEquals("classpath:db/migration/MYSQL", setup.flywayLocation());
        assertEquals("jdbc:mysql://db.example:3307/m2m?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Europe/Budapest&allowPublicKeyRetrieval=true&useSSL=false", setup.url());
        assertFalse(setup.h2Console());
    }

    @Test
    void h2SetupKeepsSaAndH2MigrationByDefault() {
        SetupService.DatabaseSetup setup = SetupService.DatabaseSetup.resolve(
                "H2", null, null, null, null, null, null, Path.of("data"));

        assertEquals("sa", setup.username());
        assertEquals("org.h2.Driver", setup.driver());
        assertEquals("classpath:db/migration/H2", setup.flywayLocation());
        assertTrue(setup.h2Console());
    }

    @Test
    void currentJdbcLocationCanBeRestoredAfterBootstrapRestart() {
        SetupService.DatabaseLocation mysql = SetupService.DatabaseLocation.parse(
                "MYSQL", "jdbc:mysql://db.example:3307/m2m?useUnicode=true");
        assertEquals("db.example", mysql.host());
        assertEquals("3307", mysql.port());
        assertEquals("m2m", mysql.databaseName());

        SetupService.DatabaseLocation oracle = SetupService.DatabaseLocation.parse(
                "ORACLE", "jdbc:oracle:thin:@oracle.example:1522/FREEPDB1");
        assertEquals("oracle.example", oracle.host());
        assertEquals("1522", oracle.port());
        assertEquals("FREEPDB1", oracle.databaseName());
    }
}
