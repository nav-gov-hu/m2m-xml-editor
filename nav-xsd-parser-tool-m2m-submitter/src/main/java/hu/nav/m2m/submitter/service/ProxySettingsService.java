package hu.nav.m2m.submitter.service;

import hu.nav.m2m.submitter.support.RepositoryAccess;

import hu.nav.m2m.submitter.domain.ProxySettings;
import hu.nav.m2m.submitter.dto.ProxySettingsDto;
import hu.nav.m2m.submitter.repo.ProxySettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A proxy/TLS konfiguráció perzisztens tárolását, DTO-leképezését, titokmegőrzési és alapérték-szabályait kezelő szolgáltatás.
 */
@Service
public class ProxySettingsService {
    private static final Long SINGLETON_ID = 1L;
    private final ProxySettingsRepository repository;

    /**
     * Létrehozza a(z) {@code ProxySettingsService} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param repository a perzisztencia repository
     */
    public ProxySettingsService(ProxySettingsRepository repository) {
        this.repository = repository;
    }

    /**
     * Visszaadja a(z) entity aktuális értékét.
     *
     * @return a művelet eredménye
     */
    @Transactional(readOnly = true)
    public ProxySettings getEntity() {
        return RepositoryAccess.findById(repository, SINGLETON_ID).orElseGet(() -> {
            ProxySettings s = new ProxySettings();
            s.setId(SINGLETON_ID);
            return s;
        });
    }

    /**
     * Lekéri a kért M2M erőforrást vagy aktuális konfigurációt.
     *
     * @return a művelet eredménye
     */
    @Transactional(readOnly = true)
    public ProxySettingsDto get() {
        return toDto(getEntity());
    }

    /**
     * Validálás után elmenti a megadott beállítást vagy domain állapotot.
     *
     * @param dto a művelethez átadott {@code dto} érték
     * @return a művelet eredménye
     */
    @Transactional
    public ProxySettingsDto save(ProxySettingsDto dto) {
        ProxySettings settings = RepositoryAccess.findById(repository, SINGLETON_ID).orElseGet(() -> {
            ProxySettings s = new ProxySettings();
            s.setId(SINGLETON_ID);
            return s;
        });
        settings.setEnabled(dto.isEnabled());
        settings.setProxyUrl(safeStoredText(dto.getProxyUrl(), 2048));
        settings.setProxyPort(safePort(dto.getProxyPort()));
        settings.setUsername(safeStoredText(dto.getUsername(), 256));
        if (dto.isClearPassword()) {
            settings.setPassword(null);
        } else if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            settings.setPassword(safeSecret(dto.getPassword()));
        }

        settings.setSslVerificationDisabled(dto.isSslVerificationDisabled());
        settings.setTrustStorePath(safeStoredText(dto.getTrustStorePath(), 2048));
        settings.setTrustStoreType(defaultTrustStoreType(dto.getTrustStoreType()));
        if (dto.isClearTrustStorePassword()) {
            settings.setTrustStorePassword(null);
        } else if (dto.getTrustStorePassword() != null && !dto.getTrustStorePassword().isBlank()) {
            settings.setTrustStorePassword(safeSecret(dto.getTrustStorePassword()));
        }

        // The persisted row is a fixed singleton. Keep record selection completely independent
        // from request data: create a trusted default row when needed, then update only that row.
        if (!repository.existsById(SINGLETON_ID)) {
            ProxySettings initial = new ProxySettings();
            initial.setId(SINGLETON_ID);
            repository.save(initial);
        }
        java.time.Instant updatedAt = java.time.Instant.now();
        repository.updateSingleton(settings.isEnabled(), settings.getProxyUrl(), settings.getProxyPort(),
                settings.getUsername(), settings.getPassword(), settings.isSslVerificationDisabled(),
                settings.getTrustStorePath(), settings.getTrustStorePassword(), settings.getTrustStoreType(), updatedAt);
        settings.setUpdatedAt(updatedAt);
        return toDto(settings);
    }

    /**
     * A bemeneti domain/transport adatokból a következő feldolgozási réteg által igényelt reprezentációt állítja elő.
     *
     * @param settings az aktuális proxy/TLS beállítások
     * @return a művelet eredménye
     */
    public ProxySettingsDto toDto(ProxySettings settings) {
        ProxySettingsDto dto = new ProxySettingsDto();
        dto.setEnabled(settings.isEnabled());
        dto.setProxyUrl(settings.getProxyUrl());
        dto.setProxyPort(settings.getProxyPort());
        dto.setUsername(settings.getUsername());
        dto.setPassword(null);
        dto.setPasswordConfigured(settings.getPassword() != null && !settings.getPassword().isBlank());
        dto.setSslVerificationDisabled(settings.isSslVerificationDisabled());
        dto.setTrustStorePath(settings.getTrustStorePath());
        dto.setTrustStorePassword(null);
        dto.setTrustStorePasswordConfigured(settings.getTrustStorePassword() != null && !settings.getTrustStorePassword().isBlank());
        dto.setTrustStoreType(defaultTrustStoreType(settings.getTrustStoreType()));
        dto.setUpdatedAt(settings.getUpdatedAt());
        return dto;
    }

    /**
     * A(z) {@code safeStoredText} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param raw a művelethez átadott {@code raw} érték
     * @param maxLength a művelethez átadott {@code maxLength} érték
     * @return a művelet eredménye
     */
    private String safeStoredText(String raw, int maxLength) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        String sanitized = value.replaceAll("[\r\n\u0000]", "");
        if (!sanitized.equals(value) || sanitized.length() > maxLength) throw new IllegalArgumentException("Érvénytelen proxy paraméter.");
        return sanitized;
    }

    /**
     * A(z) {@code safePort} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private Integer safePort(Integer value) {
        if (value == null) return null;
        if (value < 1 || value > 65535) throw new IllegalArgumentException("Érvénytelen proxy port.");
        return Integer.valueOf(value.intValue());
    }

    /**
     * A(z) {@code safeSecret} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param raw a művelethez átadott {@code raw} érték
     * @return a művelet eredménye
     */
    private String safeSecret(String raw) {
        if (raw == null) return null;
        String sanitized = raw.replace("\0", "");
        if (!sanitized.equals(raw) || sanitized.length() > 4096) throw new IllegalArgumentException("Érvénytelen proxy titokérték.");
        return sanitized;
    }

    /**
     * A(z) {@code defaultTrustStoreType} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String defaultTrustStoreType(String value) {
        if (value == null || value.isBlank()) return "JKS";
        String sanitized = value.trim().replaceAll("[^A-Za-z0-9]", "");
        if (!(sanitized.equalsIgnoreCase("JKS") || sanitized.equalsIgnoreCase("PKCS12"))) throw new IllegalArgumentException("Nem támogatott truststore típus.");
        return sanitized.equalsIgnoreCase("PKCS12") ? "PKCS12" : "JKS";
    }
}
