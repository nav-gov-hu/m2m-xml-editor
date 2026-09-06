package hu.gov.nav.xsdparsertool.web.githubproxy;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubProxySettings;
import hu.gov.nav.xsdparsertool.web.githubupdater.spi.GitHubProxyConfigurationStore;
import hu.gov.nav.xsdparsertool.web.secret.service.SystemSecretService;
import hu.gov.nav.xsdparsertool.web.systemconfig.entity.SystemConfigurationEntity;
import hu.gov.nav.xsdparsertool.web.systemconfig.repository.SystemConfigurationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A web modul GitHub-integrációs területének közös alkalmazási típusa.
 *
 * <p>A {@code SystemTableGitHubProxyConfigurationStore} osztály a web modul GitHub-integrációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Component
public class SystemTableGitHubProxyConfigurationStore implements GitHubProxyConfigurationStore {
    public static final String ENABLED = "nav.xsdparsertool.github.proxy.enabled";
    public static final String HOST = "nav.xsdparsertool.github.proxy.host";
    public static final String PORT = "nav.xsdparsertool.github.proxy.port";
    public static final String USERNAME = "nav.xsdparsertool.github.proxy.username";
    public static final String PASSWORD = "nav.xsdparsertool.github.proxy.password";
    public static final String SSL_VERIFICATION_DISABLED = "nav.xsdparsertool.github.proxy.ssl-verification-disabled";
    public static final String TRUST_STORE_PATH = "nav.xsdparsertool.github.proxy.trust-store-path";
    public static final String TRUST_STORE_TYPE = "nav.xsdparsertool.github.proxy.trust-store-type";
    public static final String TRUST_STORE_PASSWORD = "nav.xsdparsertool.github.proxy.trust-store-password";

    private static final String UPDATED_BY = "github-proxy-settings";

    private final SystemConfigurationRepository repository;
    private final SystemSecretService secrets;

    /**
     * Létrehozza a {@code SystemTableGitHubProxyConfigurationStore} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param repository a művelet bemeneti {@code repository} értéke
     * @param secrets a művelet bemeneti {@code secrets} értéke
     */
    public SystemTableGitHubProxyConfigurationStore(SystemConfigurationRepository repository,
                                                     SystemSecretService secrets) {
        this.repository = repository;
        this.secrets = secrets;
    }

    /**
     * A {@code load} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a GitHub-integrációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    @Override
    @Transactional(readOnly = true)
    public GitHubProxySettings load() {
        GitHubProxySettings settings = new GitHubProxySettings();
        settings.setEnabled(booleanValue(ENABLED, false));
        settings.setProxyUrl(value(HOST).orElse(null));
        settings.setProxyPort(integerValue(PORT).orElse(null));
        settings.setUsername(value(USERNAME).orElse(null));
        settings.setPassword(secrets.read(PASSWORD).orElse(null));
        settings.setSslVerificationDisabled(booleanValue(SSL_VERIFICATION_DISABLED, false));
        settings.setTrustStorePath(value(TRUST_STORE_PATH).orElse(null));
        settings.setTrustStoreType(value(TRUST_STORE_TYPE).filter(StringUtils::hasText).orElse("JKS"));
        settings.setTrustStorePassword(secrets.read(TRUST_STORE_PASSWORD).orElse(null));
        settings.setUpdatedAt(latestUpdate());
        return settings;
    }

    /**
     * A {@code save} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a GitHub-integrációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param settings a művelet bemeneti {@code settings} értéke
     * @param clearPassword a művelet bemeneti {@code clearPassword} értéke
     * @param clearTrustStorePassword a művelet bemeneti {@code clearTrustStorePassword} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Override
    @Transactional
    public GitHubProxySettings save(GitHubProxySettings settings,
                                    boolean clearPassword,
                                    boolean clearTrustStorePassword) {
        Instant now = Instant.now();
        saveValue(ENABLED, Boolean.toString(settings.isEnabled()), now);
        saveValue(HOST, trimToEmpty(settings.getProxyUrl()), now);
        saveValue(PORT, settings.getProxyPort() == null ? "" : settings.getProxyPort().toString(), now);
        saveValue(USERNAME, trimToEmpty(settings.getUsername()), now);
        saveValue(SSL_VERIFICATION_DISABLED, Boolean.toString(settings.isSslVerificationDisabled()), now);
        saveValue(TRUST_STORE_PATH, trimToEmpty(settings.getTrustStorePath()), now);
        saveValue(TRUST_STORE_TYPE, StringUtils.hasText(settings.getTrustStoreType()) ? settings.getTrustStoreType().trim().toUpperCase(java.util.Locale.ROOT) : "JKS", now);

        if (clearPassword) {
            secrets.delete(PASSWORD);
        } else if (StringUtils.hasText(settings.getPassword())) {
            secrets.save(PASSWORD, settings.getPassword(), UPDATED_BY);
        }
        if (clearTrustStorePassword) {
            secrets.delete(TRUST_STORE_PASSWORD);
        } else if (StringUtils.hasText(settings.getTrustStorePassword())) {
            secrets.save(TRUST_STORE_PASSWORD, settings.getTrustStorePassword(), UPDATED_BY);
        }
        return load();
    }

    /**
     * A {@code value} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a GitHub-integrációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    private Optional<String> value(String key) {
        return RepositoryAccess.findById(repository, key).map(SystemConfigurationEntity::getValue);
    }

    /**
     * A {@code integerValue} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a GitHub-integrációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    private Optional<Integer> integerValue(String key) {
        return value(key).filter(StringUtils::hasText).flatMap(raw -> {
            try {
                return Optional.of(Integer.valueOf(raw.trim()));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        });
    }

    /**
     * A {@code booleanValue} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a GitHub-integrációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @param defaultValue a művelet bemeneti {@code defaultValue} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean booleanValue(String key, boolean defaultValue) {
        return value(key).filter(StringUtils::hasText).map(String::trim).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    /**
     * A {@code saveValue} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a GitHub-integrációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @param value a művelet bemeneti {@code value} értéke
     * @param updatedAt a művelet bemeneti {@code updatedAt} értéke
     */
    private void saveValue(String key, String value, Instant updatedAt) {
        // Configuration keys are application-owned constants. If a row is missing, create only a
        // trusted empty shell first; request-controlled values are persisted through a fixed-key
        // update and can never select which record is modified.
        if (!repository.existsById(key)) {
            SystemConfigurationEntity initial = new SystemConfigurationEntity();
            initial.setKey(key);
            initial.setValue("");
            initial.setUpdatedAt(updatedAt);
            initial.setUpdatedBy(UPDATED_BY);
            repository.save(initial);
        }
        repository.updateTrustedKey(key, value, updatedAt, UPDATED_BY);
    }

    /**
     * A {@code latestUpdate} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a GitHub-integrációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    private Instant latestUpdate() {
        return List.of(ENABLED, HOST, PORT, USERNAME, SSL_VERIFICATION_DISABLED, TRUST_STORE_PATH, TRUST_STORE_TYPE)
                .stream()
                .map(repository::findById)
                .flatMap(Optional::stream)
                .map(SystemConfigurationEntity::getUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
    }

    /**
     * A {@code trimToEmpty} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a GitHub-integrációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
