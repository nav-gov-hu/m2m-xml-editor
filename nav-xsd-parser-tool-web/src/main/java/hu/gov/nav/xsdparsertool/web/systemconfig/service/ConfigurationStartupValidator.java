package hu.gov.nav.xsdparsertool.web.systemconfig.service;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.web.secret.service.SystemSecretService;
import hu.gov.nav.xsdparsertool.web.systemconfig.entity.SystemConfigurationEntity;
import hu.gov.nav.xsdparsertool.web.systemconfig.repository.SystemConfigurationRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A befejezett telepítés teljes konfigurációs katalógusát ellenőrzi.
 *
 * Nincs futásidejű fallback: minden nem opcionális kulcsnak a saját tárolási
 * helyén kell szerepelnie, és használható értékkel kell rendelkeznie.
 */
@Component
@Order(200)
public class ConfigurationStartupValidator implements ApplicationRunner {
    private final SystemConfigurationRepository repository;
    private final SystemSecretService secrets;
    private final Environment environment;

    /**
     * Létrehozza a {@code ConfigurationStartupValidator} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param repository a művelet bemeneti {@code repository} értéke
     * @param secrets a művelet bemeneti {@code secrets} értéke
     * @param environment a művelet bemeneti {@code environment} értéke
     */
    public ConfigurationStartupValidator(SystemConfigurationRepository repository,
                                         SystemSecretService secrets,
                                         Environment environment) {
        this.repository = repository;
        this.secrets = secrets;
        this.environment = environment;
    }

    /**
     * A {@code run} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param args a művelet bemeneti {@code args} értéke
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!environment.getProperty("nav.xsdparsertool.setup.completed", Boolean.class, false)) {
            return;
        }

        Map<String, SystemConfigurationEntity> databaseValues = RepositoryAccess.findAll(repository).stream()
                .collect(Collectors.toMap(SystemConfigurationEntity::getKey, Function.identity()));
        List<String> errors = new ArrayList<>();

        for (ConfigurationCatalog.Spec spec : ConfigurationCatalog.ITEMS) {
            if (ConfigurationCatalog.isOptionalIntegrationKey(spec.key())) {
                continue;
            }
            if (isDisabledApiKeySecret(spec.key())) {
                continue;
            }
            if (!isDatabaseSpecificBootstrapKeyApplicable(spec.key())) {
                continue;
            }

            if ("BOOTSTRAP".equals(spec.storage())) {
                validateBootstrap(spec, errors);
            } else if (ConfigurationCatalog.ENCRYPTED_SECRET_KEYS.contains(spec.key())) {
                if (!secrets.exists(spec.key())) {
                    errors.add(spec.key() + " (hiányzó titkosított adatbázis-érték)");
                }
            } else {
                SystemConfigurationEntity entity = databaseValues.get(spec.key());
                if (entity == null) {
                    errors.add(spec.key() + " (hiányzó adatbázis-kulcs)");
                } else if (!StringUtils.hasText(entity.getValue())) {
                    errors.add(spec.key() + " (üres adatbázis-érték)");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                    "A rendszer konfigurációja hiányos. Az alkalmazás nem indítható el. Hiányzó vagy üres elemek: "
                            + String.join(", ", errors));
        }
    }

    /**
     * A {@code validateBootstrap} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param spec a művelet bemeneti {@code spec} értéke
     * @param errors a feldolgozandó elemek kollekciója
     */
    private void validateBootstrap(ConfigurationCatalog.Spec spec, List<String> errors) {
        if (!environment.containsProperty(spec.key())) {
            errors.add(spec.key() + " (hiányzó bootstrap-kulcs)");
            return;
        }
        String value = environment.getProperty(spec.key());
        // A jelszó típusú bootstrap-beállításnál az explicit üres érték is lehet
        // technikailag érvényes (például új H2 adatbázis esetén). A kulcsnak ettől
        // még szerepelnie kell a konfigurációban.
        if (!"PASSWORD".equals(spec.type()) && !StringUtils.hasText(value)) {
            errors.add(spec.key() + " (üres bootstrap-érték)");
        }
    }

    /**
     * Megadja, hogy az adatbázis-specifikus bootstrap kulcs az aktuális
     * adatbázistípus mellett része-e a kötelező indulási konfigurációnak.
     *
     * <p>A Hibernate Instant JDBC típus felülbírálása kizárólag Oracle alatt
     * szükséges. H2, MySQL és PostgreSQL esetén a kulcs szándékosan hiányzik,
     * ezért annak hiánya nem konfigurációs hiba.</p>
     * @param key az ellenőrzött konfigurációs kulcs
     * @return {@code true}, ha a kulcsot az aktuális adatbázistípusnál validálni kell
     */
    private boolean isDatabaseSpecificBootstrapKeyApplicable(String key) {
        if (!"spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type".equals(key)) {
            return true;
        }
        return "ORACLE".equalsIgnoreCase(environment.getProperty("nav.xsdparsertool.database.type", ""));
    }

    /**
     * A {@code isDisabledApiKeySecret} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isDisabledApiKeySecret(String key) {
        return "nav.xsdparsertool.api-key.value".equals(key)
                && !environment.getProperty("nav.xsdparsertool.api-key.enabled", Boolean.class, false);
    }
}
