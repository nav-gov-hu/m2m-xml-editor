package hu.gov.nav.xsdparsertool.web.database.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationResourceParityTest {

    private static final List<String> DATABASES = List.of("H2", "MYSQL", "POSTGRESQL", "ORACLE");
    private static final Pattern VERSIONED_MIGRATION = Pattern.compile("^V(\\d+)__(.+)\\.sql$");

    @Test
    void everySupportedDatabaseMustHaveTheSameVersionedMigrationInventory() throws Exception {
        Map<String, Map<Integer, String>> inventory = new LinkedHashMap<>();
        for (String database : DATABASES) {
            inventory.put(database, migrations(database));
        }

        Map<Integer, String> h2 = inventory.get("H2");
        assertFalse(h2.isEmpty());
        for (String database : DATABASES) {
            assertEquals(h2, inventory.get(database),
                    "A migrációs verziók/fájlnevek eltérnek H2 és " + database + " között.");
        }
    }

    @Test
    void migrationVersionsMustBeContinuousWithoutDuplicates() throws Exception {
        for (String database : DATABASES) {
            Map<Integer, String> migrations = migrations(database);
            List<Integer> versions = new ArrayList<>(migrations.keySet());
            assertFalse(versions.isEmpty(), "Nincs migráció: " + database);

            int latest = versions.get(versions.size() - 1);
            assertEquals(29, latest, "A konfigurációs baseline legfrissebb migrációja V29.");
            assertEquals(latest, versions.size(), "Hiányzó vagy duplikált Flyway verzió: " + database);
            for (int expected = 1; expected <= latest; expected++) {
                assertTrue(migrations.containsKey(expected), "Hiányzó V" + expected + " migráció: " + database);
            }
        }
    }

    @Test
    void everyVersionedMigrationFileMustContainSql() throws Exception {
        for (String database : DATABASES) {
            Path directory = migrationDirectory(database);
            try (var files = Files.list(directory)) {
                for (Path file : files.filter(Files::isRegularFile).filter(this::isVersionedMigration).toList()) {
                    String sql = Files.readString(file);
                    assertFalse(sql.isBlank(), "Üres migrációs fájl: " + database + "/" + file.getFileName());
                    assertTrue(sql.lines().anyMatch(line -> {
                                String trimmed = line.trim();
                                return !trimmed.isEmpty() && !trimmed.startsWith("--");
                            }),
                            "A migráció nem tartalmaz végrehajtható SQL-t: " + database + "/" + file.getFileName());
                }
            }
        }
    }

    private Map<Integer, String> migrations(String database) throws IOException, URISyntaxException {
        Map<Integer, String> result = new TreeMap<>();
        try (var files = Files.list(migrationDirectory(database))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Matcher matcher = VERSIONED_MIGRATION.matcher(file.getFileName().toString());
                if (!matcher.matches()) {
                    continue;
                }
                int version = Integer.parseInt(matcher.group(1));
                String previous = result.put(version, file.getFileName().toString());
                assertTrue(previous == null,
                        "Duplikált Flyway verzió " + database + " V" + version + ": " + previous + ", " + file.getFileName());
            }
        }
        return result;
    }

    private boolean isVersionedMigration(Path file) {
        return VERSIONED_MIGRATION.matcher(file.getFileName().toString()).matches();
    }

    private Path migrationDirectory(String database) throws URISyntaxException {
        String resource = "db/migration/" + database;
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        assertNotNull(url, "Hiányzó migrációs resource könyvtár: " + resource);
        URI uri = url.toURI();
        assertEquals("file", uri.getScheme(), "A teszt fájlrendszeri test-resources könyvtárat vár: " + resource);
        return Path.of(uri);
    }
}
