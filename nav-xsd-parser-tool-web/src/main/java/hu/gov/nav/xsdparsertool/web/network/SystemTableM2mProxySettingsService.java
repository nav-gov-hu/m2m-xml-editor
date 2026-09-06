package hu.gov.nav.xsdparsertool.web.network;

import hu.gov.nav.xsdparsertool.web.secret.service.SystemSecretService;
import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;
import hu.gov.nav.xsdparsertool.web.systemconfig.entity.SystemConfigurationEntity;
import hu.gov.nav.xsdparsertool.web.systemconfig.repository.SystemConfigurationRepository;
import hu.nav.m2m.submitter.domain.ProxySettings;
import hu.nav.m2m.submitter.dto.ProxySettingsDto;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A NAV M2M proxy- és TLS-beállítások központi rendszerkonfigurációs tárolója.
 *
 * <p>A nem titkos értékeket a {@code system_configuration}, a proxy- és truststore-jelszavakat
 * a {@code system_secret} tárolja. Ez a szolgáltatás a webalkalmazás egyetlen M2M proxy/TLS
 * igazságforrása; a régi {@code m2m_proxy_settings} tábla csak az önálló submitter modul
 * kompatibilitási fallbackje marad.</p>
 */
@Service
public class SystemTableM2mProxySettingsService {
    public static final String PREFIX = "nav.xsdparsertool.network.proxy.";
    public static final String ENABLED = PREFIX + "enabled";
    public static final String HOST = PREFIX + "host";
    public static final String PORT = PREFIX + "port";
    public static final String USERNAME = PREFIX + "username";
    public static final String PASSWORD = PREFIX + "password";
    public static final String SSL_VERIFICATION_DISABLED = PREFIX + "ssl-verification-disabled";
    public static final String TRUST_STORE_PATH = PREFIX + "trust-store-path";
    public static final String TRUST_STORE_TYPE = PREFIX + "trust-store-type";
    public static final String TRUST_STORE_PASSWORD = PREFIX + "trust-store-password";

    private static final String UPDATED_BY = "m2m-proxy-settings";

    private final Environment environment;
    private final SystemConfigurationRepository repository;
    private final SystemSecretService secrets;

    /**
     * Létrehozza a központi M2M proxy/TLS konfigurációs szolgáltatást.
     *
     * @param environment Spring környezeti fallback a még nem perzisztált értékekhez
     * @param repository központi konfigurációs repository
     * @param secrets titkosított konfigurációs tároló
     */
    public SystemTableM2mProxySettingsService(Environment environment,
                                              SystemConfigurationRepository repository,
                                              SystemSecretService secrets) {
        this.environment = environment;
        this.repository = repository;
        this.secrets = secrets;
    }

    /**
     * Betölti a NAV M2M HTTP kliens által ténylegesen használt proxy- és TLS-beállításokat.
     *
     * @return az aktuális központi konfiguráció
     */
    @Transactional(readOnly = true)
    public ProxySettings getEntity() {
        ProxySettings settings = new ProxySettings();
        settings.setEnabled(booleanValue(ENABLED, false));
        settings.setProxyUrl(trimToNull(value(HOST).orElse(null)));
        settings.setProxyPort(integerValue(PORT).orElse(8080));
        settings.setUsername(trimToNull(value(USERNAME).orElse(null)));
        settings.setPassword(readSecretSafely(PASSWORD));
        settings.setSslVerificationDisabled(false);
        settings.setTrustStorePath(trimToNull(value(TRUST_STORE_PATH).orElse(null)));
        settings.setTrustStoreType(value(TRUST_STORE_TYPE).filter(StringUtils::hasText).orElse("JKS"));
        settings.setTrustStorePassword(readSecretSafely(TRUST_STORE_PASSWORD));
        settings.setUpdatedAt(latestUpdate());
        return settings;
    }

    /**
     * A központi M2M proxy/TLS konfiguráció REST reprezentációját adja vissza titkok nélkül.
     *
     * @return maszkolt konfigurációs DTO
     */
    @Transactional(readOnly = true)
    public ProxySettingsDto get() {
        return toDto(getEntity());
    }

    /**
     * Elmenti a validált M2M proxy/TLS beállításokat a központi konfigurációs táblákba.
     * Üres jelszómező megtartja a korábbi titkot, explicit törlési jelző eltávolítja azt.
     *
     * @param dto mentendő konfiguráció
     * @return a mentés utáni, titkokat nem tartalmazó DTO
     */
    @Transactional
    public ProxySettingsDto save(ProxySettingsDto dto) {
        Instant now = Instant.now();
        saveValue(ENABLED, Boolean.toString(dto.isEnabled()), now);
        saveValue(HOST, trimToEmpty(dto.getProxyUrl()), now);
        saveValue(PORT, dto.getProxyPort() == null ? "" : dto.getProxyPort().toString(), now);
        saveValue(USERNAME, trimToEmpty(dto.getUsername()), now);
        // A NAV M2M TLS tanúsítvány-ellenőrzése kötelező; korábbi true értékeket is false-ra normalizálunk.
        saveValue(SSL_VERIFICATION_DISABLED, Boolean.FALSE.toString(), now);
        saveValue(TRUST_STORE_PATH, trimToEmpty(dto.getTrustStorePath()), now);
        saveValue(TRUST_STORE_TYPE, normalizeTrustStoreType(dto.getTrustStoreType()), now);

        saveSecret(PASSWORD, dto.getPassword(), dto.isClearPassword());
        saveSecret(TRUST_STORE_PASSWORD, dto.getTrustStorePassword(), dto.isClearTrustStorePassword());
        return get();
    }

    private ProxySettingsDto toDto(ProxySettings settings) {
        ProxySettingsDto dto = new ProxySettingsDto();
        dto.setEnabled(settings.isEnabled());
        dto.setProxyUrl(settings.getProxyUrl());
        dto.setProxyPort(settings.getProxyPort());
        dto.setUsername(settings.getUsername());
        dto.setPasswordConfigured(StringUtils.hasText(settings.getPassword()));
        dto.setSslVerificationDisabled(settings.isSslVerificationDisabled());
        dto.setTrustStorePath(settings.getTrustStorePath());
        dto.setTrustStoreType(normalizeTrustStoreType(settings.getTrustStoreType()));
        dto.setTrustStorePasswordConfigured(StringUtils.hasText(settings.getTrustStorePassword()));
        dto.setUpdatedAt(settings.getUpdatedAt());
        return dto;
    }

    private Optional<String> value(String key) {
        return RepositoryAccess.findById(repository, key)
                .map(SystemConfigurationEntity::getValue)
                .or(() -> Optional.ofNullable(environment.getProperty(key)));
    }

    private Optional<Integer> integerValue(String key) {
        return value(key).filter(StringUtils::hasText).flatMap(raw -> {
            try {
                return Optional.of(Integer.valueOf(raw.trim()));
            } catch (NumberFormatException ex) {
                throw new IllegalStateException("Hibás proxy port a rendszerkonfigurációban: " + raw, ex);
            }
        });
    }

    private boolean booleanValue(String key, boolean defaultValue) {
        return value(key).filter(StringUtils::hasText).map(String::trim).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    private String readSecretSafely(String key) {
        try {
            return secrets.read(key).orElse(null);
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    private void saveSecret(String key, String rawValue, boolean clear) {
        if (clear) {
            secrets.delete(key);
        } else if (StringUtils.hasText(rawValue)) {
            secrets.save(key, rawValue, UPDATED_BY);
        }
    }

    private void saveValue(String key, String value, Instant updatedAt) {
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

    private String normalizeTrustStoreType(String value) {
        if (!StringUtils.hasText(value)) {
            return "JKS";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!"JKS".equals(normalized) && !"PKCS12".equals(normalized)) {
            throw new IllegalArgumentException("Nem támogatott truststore típus.");
        }
        return normalized;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
