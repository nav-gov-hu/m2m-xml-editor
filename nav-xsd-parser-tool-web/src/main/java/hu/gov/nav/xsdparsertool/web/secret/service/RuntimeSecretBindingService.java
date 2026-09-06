package hu.gov.nav.xsdparsertool.web.secret.service;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.web.githubupdater.config.GitHubSchemaUpdaterProperties;
import hu.gov.nav.xsdparsertool.web.security.apikey.ApiKeySecurityProperties;
import hu.gov.nav.xsdparsertool.web.systemconfig.repository.SystemConfigurationRepository;
import hu.nav.m2m.submitter.config.NavM2mProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

/**
 * A titkosított adatbázisban tárolt értékeket a már létrejött, típusos
 * konfigurációs beanekbe tölti. STANDALONE módban ez bejelentkezés után,
 * MULTI_USER módban pedig alkalmazásinduláskor történik meg.
 */
@Service
public class RuntimeSecretBindingService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(RuntimeSecretBindingService.class);
    private final SystemSecretService secrets;
    private final NavM2mProperties m2m;
    private final GitHubSchemaUpdaterProperties github;
    private final ApiKeySecurityProperties apiKey;
    private final SystemConfigurationRepository systemConfigurationRepository;

    /**
     * Létrehozza a {@code RuntimeSecretBindingService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param secrets a művelet bemeneti {@code secrets} értéke
     * @param m2m a művelet bemeneti {@code m2m} értéke
     * @param github a művelet bemeneti {@code github} értéke
     * @param apiKey a művelet bemeneti {@code apiKey} értéke
     * @param systemConfigurationRepository a művelethez szükséges konfigurációs adatok
     */
    public RuntimeSecretBindingService(SystemSecretService secrets,
                                       NavM2mProperties m2m,
                                       GitHubSchemaUpdaterProperties github,
                                       ApiKeySecurityProperties apiKey,
                                       SystemConfigurationRepository systemConfigurationRepository) {
        this.secrets = secrets;
        this.m2m = m2m;
        this.github = github;
        this.apiKey = apiKey;
        this.systemConfigurationRepository = systemConfigurationRepository;
    }

    /**
     * A {@code run} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param args a művelet bemeneti {@code args} értéke
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            refresh();
        } catch (IllegalStateException ex) {
            log.warn("Runtime secret binding skipped during startup: {}. In STANDALONE mode this can be expected until the master key is unlocked.", ex.getMessage(), ex);
        }
    }

    /**
     * A {@code refresh} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    public synchronized void refresh() {
        secrets.read("nav.m2m.auth.client-secret").ifPresent(m2m.getAuth()::setClientSecret);
        secrets.read("nav.m2m.auth.password").ifPresent(m2m.getAuth()::setPassword);
        secrets.read("nav.m2m.signature.key-first-part").ifPresent(m2m.getSignature()::setKeyFirstPart);
        secrets.read("nav.m2m.signature.key-second-part").ifPresent(m2m.getSignature()::setKeySecondPart);
        secrets.read("nav.m2m.signature.nonce").ifPresent(m2m.getSignature()::setNonce);
        secrets.read("nav.xsdparsertool.github-schema-updater.token").ifPresent(github::setToken);
        boolean enabledRecordExists = systemConfigurationRepository.existsById("nav.xsdparsertool.api-key.enabled");
        RepositoryAccess.findById(systemConfigurationRepository, "nav.xsdparsertool.api-key.enabled")
                .map(entity -> entity.getValue())
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .ifPresent(value -> apiKey.setEnabled(Boolean.parseBoolean(value)));
        RepositoryAccess.findById(systemConfigurationRepository, "nav.xsdparsertool.api-key.header-name")
                .map(entity -> entity.getValue())
                .ifPresent(apiKey::setHeaderName);
        RepositoryAccess.findById(systemConfigurationRepository, "nav.xsdparsertool.api-key.principal-name")
                .map(entity -> entity.getValue())
                .ifPresent(apiKey::setPrincipalName);

        boolean secretRecordExists = secrets.exists("nav.xsdparsertool.api-key.value");
        try {
            secrets.read("nav.xsdparsertool.api-key.value").ifPresent(apiKey::setApiKey);
        } catch (IllegalStateException ex) {
            log.error("API key secret could not be decrypted: key=nav.xsdparsertool.api-key.value, secretRecordExists={}, cause={}",
                    secretRecordExists, ex.getMessage(), ex);
            throw ex;
        }

        log.info("API key runtime binding completed: enabledRecordExists={}, configuredEnabled={}, secretRecordExists={}, decryptedKeyPresent={}, headerName={}, principalName={}",
                enabledRecordExists, apiKey.isConfiguredEnabled(), secretRecordExists, apiKey.hasApiKey(),
                apiKey.getHeaderName(), apiKey.getPrincipalName());
    }
}
