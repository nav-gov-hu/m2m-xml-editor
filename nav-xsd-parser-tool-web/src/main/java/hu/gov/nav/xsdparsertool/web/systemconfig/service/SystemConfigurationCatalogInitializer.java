package hu.gov.nav.xsdparsertool.web.systemconfig.service;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.web.systemconfig.entity.SystemConfigurationEntity;
import hu.gov.nav.xsdparsertool.web.systemconfig.repository.SystemConfigurationRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A Flyway migráció után gondoskodik arról, hogy a system_configuration tábla
 * a katalógus minden DATABASE tárolású kulcsát tartalmazza.
 *
 * A már meglévő értékeket nem írja felül. Hiányzó kulcsot friss telepítésnél és
 * verziófrissítés után is létrehoz. A telepítőből érkező környezeti érték az elsődleges,
 * ennek hiányában a katalógus alapértéke.
 * Az érzékeny konfigurációk üres normál DB-rekorddal jönnek létre, tényleges
 * értékük kizárólag a titkosított rendszerkonfigurációs táblában tárolható.
 */
@Component
@Order(100)
public class SystemConfigurationCatalogInitializer implements ApplicationRunner {
    private final SystemConfigurationRepository repository;
    private final Environment environment;

    /**
     * Létrehozza a {@code SystemConfigurationCatalogInitializer} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param repository a művelet bemeneti {@code repository} értéke
     * @param environment a művelet bemeneti {@code environment} értéke
     */
    public SystemConfigurationCatalogInitializer(SystemConfigurationRepository repository, Environment environment) {
        this.repository = repository;
        this.environment = environment;
    }

    /**
     * A {@code run} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param args a művelet bemeneti {@code args} értéke
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // A katalógus hiányzó DATABASE kulcsait befejezett telepítésnél is pótolni kell.
        // Ez szükséges friss installer-es H2 adatbázisnál és verziófrissítéskor újonnan
        // bekerülő konfigurációs kulcsoknál is. Meglévő értéket továbbra sem írunk felül.
        Instant now = Instant.now();
        List<SystemConfigurationEntity> missing = new ArrayList<>();
        for (ConfigurationCatalog.Spec spec : ConfigurationCatalog.ITEMS) {
            if (!"DATABASE".equals(spec.storage())) {
                continue;
            }
            SystemConfigurationEntity existing = RepositoryAccess.findById(repository, spec.key()).orElse(null);
            if (existing != null) {
                continue;
            }
            SystemConfigurationEntity entity = new SystemConfigurationEntity();
            entity.setKey(spec.key());
            entity.setValue(initialValue(spec));
            entity.setUpdatedAt(now);
            entity.setUpdatedBy("catalog-initializer");
            missing.add(entity);
        }
        if (!missing.isEmpty()) {
            repository.saveAll(missing);
        }
    }

    /**
     * A {@code initialValue} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param spec a művelet bemeneti {@code spec} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String initialValue(ConfigurationCatalog.Spec spec) {
        if (spec.sensitive()) {
            return "";
        }
        String configured = environment.getProperty(spec.key());
        return configured == null ? spec.defaultValue() : configured;
    }
}
