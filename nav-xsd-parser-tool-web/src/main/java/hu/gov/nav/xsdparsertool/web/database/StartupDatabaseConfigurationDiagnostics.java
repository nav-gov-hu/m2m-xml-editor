package hu.gov.nav.xsdparsertool.web.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Az alkalmazás indulása után kilogolja a tényleges adatbázis-, Hibernate- és
 * Flyway-konfigurációt. A diagnosztika célja a konfigurációs források és a
 * kiválasztott Hibernate dialektus egyértelmű azonosítása.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StartupDatabaseConfigurationDiagnostics implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupDatabaseConfigurationDiagnostics.class);

    private static final String[] DIAGNOSTIC_KEYS = {
            "nav.xsdparsertool.database.type",
            "spring.datasource.url",
            "spring.datasource.driver-class-name",
            "spring.jpa.database-platform",
            "spring.jpa.properties.hibernate.dialect",
            "spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type",
            "spring.flyway.enabled",
            "spring.flyway.locations",
            "nav.xsdparsertool.bootstrap-config-file",
            "nav.xsdparsertool.setup.completed"
    };

    private final ConfigurableEnvironment environment;
    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;

    /**
     * Létrehozza a {@code StartupDatabaseConfigurationDiagnostics} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param environment a művelet bemeneti {@code environment} értéke
     * @param dataSource a művelet bemeneti {@code dataSource} értéke
     * @param entityManagerFactory a művelet bemeneti {@code entityManagerFactory} értéke
     */
    public StartupDatabaseConfigurationDiagnostics(
            ConfigurableEnvironment environment,
            DataSource dataSource,
            EntityManagerFactory entityManagerFactory) {
        this.environment = environment;
        this.dataSource = dataSource;
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * A {@code run} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a adatbázis-konfigurációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param args a művelet bemeneti {@code args} értéke
     */
    @Override
    public void run(ApplicationArguments args) {
        LOGGER.info("Adatbázis-konfigurációs diagnosztika indul.");

        for (String key : DIAGNOSTIC_KEYS) {
            String value = environment.getProperty(key);
            LOGGER.info("Konfiguráció: {}={} [forrás={}]",
                    key,
                    printableValue(value),
                    resolvePropertySource(key));
        }

        logJdbcMetadata();
        logHibernateDialect();
        LOGGER.info("Adatbázis-konfigurációs diagnosztika befejeződött.");
    }

    /**
     * A {@code logJdbcMetadata} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a adatbázis-konfigurációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    private void logJdbcMetadata() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            LOGGER.info(
                    "Tényleges JDBC kapcsolat: databaseProduct={} {}, driver={} {}, jdbcUrl={}, user={}",
                    metadata.getDatabaseProductName(),
                    metadata.getDatabaseProductVersion(),
                    metadata.getDriverName(),
                    metadata.getDriverVersion(),
                    metadata.getURL(),
                    metadata.getUserName());
        } catch (Exception ex) {
            LOGGER.warn("A tényleges JDBC kapcsolat metaadatai nem olvashatók ki: {}", ex.getMessage());
        }
    }

    /**
     * A {@code logHibernateDialect} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a adatbázis-konfigurációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    private void logHibernateDialect() {
        try {
            SessionFactoryImplementor sessionFactory =
                    entityManagerFactory.unwrap(SessionFactoryImplementor.class);
            Object dialect = sessionFactory.getJdbcServices().getDialect();
            LOGGER.info("Tényleges Hibernate dialect: {}", dialect.getClass().getName());
        } catch (Exception ex) {
            LOGGER.warn("A tényleges Hibernate dialect nem olvasható ki: {}", ex.getMessage());
        }
    }

    /**
     * A {@code resolvePropertySource} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @return a feloldott vagy lekért érték
     */
    private String resolvePropertySource(String key) {
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource.containsProperty(key)) {
                return propertySource.getName();
            }
        }
        return "nincs explicit forrás / alapértelmezés";
    }

    /**
     * A {@code printableValue} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a adatbázis-konfigurációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String printableValue(String value) {
        if (value == null) {
            return "<nincs beállítva>";
        }
        if (value.isBlank()) {
            return "<üres>";
        }
        return value;
    }
}
