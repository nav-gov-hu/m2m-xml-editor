package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubProxySettings;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubProxySettingsDto;
import hu.gov.nav.xsdparsertool.web.githubupdater.spi.GitHubProxyConfigurationStore;
import org.springframework.stereotype.Service;

/**
 * A GitHub proxy- és truststore-beállítások alkalmazási szolgáltatása. A perzisztencia-független konfigurációs store és a REST DTO között végez biztonságos konverziót, miközben kezeli a titkok megtartását és törlését.
 */
@Service
public class GitHubProxySettingsService {
    private final GitHubProxyConfigurationStore configurationStore;

    /**
     * Létrehozza a(z) {@code GitHubProxySettingsService} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     *
     * @param configurationStore a művelethez átadott {@code configurationStore} érték
     */
    public GitHubProxySettingsService(GitHubProxyConfigurationStore configurationStore) {
        this.configurationStore = configurationStore;
    }

    /**
     * Visszaadja a(z) entity aktuális értékét.
     *
     * @return a(z) entity érték
     */
    public GitHubProxySettings getEntity() {
        return configurationStore.load();
    }

    /**
     * Visszaadja a(z) érték aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public GitHubProxySettingsDto get() {
        return toDto(getEntity());
    }

    /**
     * Normalizálja és perzisztálja a proxybeállításokat. Üresen hagyott titok esetén megtartja a korábbi értéket, explicit törlési jelző esetén eltávolítja, majd maszkolt DTO-t ad vissza.
     *
     * @param dto a REST rétegből érkező proxybeállítás DTO
     * @return a művelet eredménye
     */
    public GitHubProxySettingsDto save(GitHubProxySettingsDto dto) {
        GitHubProxySettings settings = getEntity();
        settings.setEnabled(dto.isEnabled());
        settings.setProxyUrl(safeText(dto.getProxyUrl(), 2048));
        settings.setProxyPort(safePort(dto.getProxyPort()));
        settings.setUsername(safeText(dto.getUsername(), 256));
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            settings.setPassword(safeSecret(dto.getPassword()));
        }
        settings.setSslVerificationDisabled(dto.isSslVerificationDisabled());
        settings.setTrustStorePath(safeText(dto.getTrustStorePath(), 2048));
        settings.setTrustStoreType(type(dto.getTrustStoreType()));
        if (dto.getTrustStorePassword() != null && !dto.getTrustStorePassword().isBlank()) {
            settings.setTrustStorePassword(safeSecret(dto.getTrustStorePassword()));
        }
        return toDto(configurationStore.save(settings, dto.isClearPassword(), dto.isClearTrustStorePassword()));
    }

    /**
     * A perzisztált proxybeállítást REST DTO-vá alakítja úgy, hogy a jelszavakat nem adja vissza; csak azt jelzi, hogy titok konfigurálva van-e.
     *
     * @param settings az aktuális proxy/TLS beállítások
     * @return a művelet eredménye
     */
    private GitHubProxySettingsDto toDto(GitHubProxySettings settings) {
        GitHubProxySettingsDto dto = new GitHubProxySettingsDto();
        dto.setEnabled(settings.isEnabled());
        dto.setProxyUrl(settings.getProxyUrl());
        dto.setProxyPort(settings.getProxyPort());
        dto.setUsername(settings.getUsername());
        dto.setPasswordConfigured(settings.getPassword() != null && !settings.getPassword().isBlank());
        dto.setSslVerificationDisabled(settings.isSslVerificationDisabled());
        dto.setTrustStorePath(settings.getTrustStorePath());
        dto.setTrustStorePasswordConfigured(settings.getTrustStorePassword() != null && !settings.getTrustStorePassword().isBlank());
        dto.setTrustStoreType(type(settings.getTrustStoreType()));
        dto.setUpdatedAt(settings.getUpdatedAt());
        return dto;
    }

    /**
     * A konfigurált vagy bejövő értéket biztonságos tartományra normalizálja, és szükség esetén kontrollált fallbacket alkalmaz.
     *
     * @param raw a nyers bemeneti szöveg
     * @param maxLength a megengedett maximális hossz
     * @return a művelet eredménye
     */
    private String safeText(String raw, int maxLength) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        String sanitized = value.replaceAll("[\r\n\u0000]", "");
        if (!sanitized.equals(value) || sanitized.length() > maxLength) throw new IllegalArgumentException("Érvénytelen GitHub proxy paraméter.");
        return sanitized;
    }

    /**
     * A konfigurált vagy bejövő értéket biztonságos tartományra normalizálja, és szükség esetén kontrollált fallbacket alkalmaz.
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
     * A konfigurált vagy bejövő értéket biztonságos tartományra normalizálja, és szükség esetén kontrollált fallbacket alkalmaz.
     *
     * @param raw a nyers bemeneti szöveg
     * @return a művelet eredménye
     */
    private String safeSecret(String raw) {
        if (raw == null) return null;
        String sanitized = raw.replace("\0", "");
        if (!sanitized.equals(raw) || sanitized.length() > 4096) throw new IllegalArgumentException("Érvénytelen GitHub proxy titokérték.");
        return sanitized;
    }

    /**
     * A truststore-típus értékét normalizálja: csak a támogatott JKS vagy PKCS12 értéket tartja meg, egyébként a modul alapértelmezett JKS típusára esik vissza.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String type(String value) {
        if (value == null || value.isBlank()) return "JKS";
        String sanitized = value.trim().replaceAll("[^A-Za-z0-9]", "");
        if (!(sanitized.equalsIgnoreCase("JKS") || sanitized.equalsIgnoreCase("PKCS12"))) throw new IllegalArgumentException("Nem támogatott truststore típus.");
        return sanitized.equalsIgnoreCase("PKCS12") ? "PKCS12" : "JKS";
    }
}
