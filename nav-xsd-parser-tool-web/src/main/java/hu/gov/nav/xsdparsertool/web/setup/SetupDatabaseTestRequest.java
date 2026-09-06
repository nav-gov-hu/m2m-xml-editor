package hu.gov.nav.xsdparsertool.web.setup;

/**
 * A kezdeti setup adatbázis-kapcsolati tesztjének bemeneti adatai.
 */
public record SetupDatabaseTestRequest(
        String dataDirectory,
        String databaseType,
        String databaseHost,
        String databasePort,
        String databaseName,
        String databaseSchema,
        String databaseUsername,
        String databasePassword) {
}
