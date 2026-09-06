package hu.nav.m2m.submitter.dto;

import java.time.Instant;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A proxy- és TLS-beállítások REST reprezentációja; a titkos mezők kezelését a szolgáltatási réteg szabályozza.
 */
public class ProxySettingsDto {
    private boolean enabled;
    @Size(max = 2048)
    private String proxyUrl;
    @Min(1) @Max(65535)
    private Integer proxyPort;
    @Size(max = 256)
    private String username;
    @Size(max = 4096)
    private String password;
    private boolean passwordConfigured;
    private boolean clearPassword;
    private boolean sslVerificationDisabled;
    @Size(max = 2048)
    private String trustStorePath;
    @Size(max = 4096)
    private String trustStorePassword;
    private boolean trustStorePasswordConfigured;
    private boolean clearTrustStorePassword;
    @Pattern(regexp = "(?i)JKS|PKCS12")
    private String trustStoreType;
    private Instant updatedAt;

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    public boolean isEnabled() { return enabled; }
    /**
     * Beállítja a(z) engedélyezési jelző értékét a domain objektumon.
     *
     * @param enabled a művelethez átadott {@code enabled} érték
     */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    /**
     * Visszaadja a(z) proxyUrl aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getProxyUrl() { return proxyUrl; }
    /**
     * Beállítja a(z) proxyUrl értékét a domain objektumon.
     *
     * @param proxyUrl a művelethez átadott {@code proxyUrl} érték
     */
    public void setProxyUrl(String proxyUrl) { this.proxyUrl = proxyUrl; }
    /**
     * Visszaadja a(z) proxy port aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Integer getProxyPort() { return proxyPort; }
    /**
     * Beállítja a(z) proxy port értékét a domain objektumon.
     *
     * @param proxyPort a művelethez átadott {@code proxyPort} érték
     */
    public void setProxyPort(Integer proxyPort) { this.proxyPort = proxyPort; }
    /**
     * Visszaadja a(z) username aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getUsername() { return username; }
    /**
     * Beállítja a(z) username értékét a domain objektumon.
     *
     * @param username a művelethez átadott {@code username} érték
     */
    public void setUsername(String username) { this.username = username; }
    /**
     * Visszaadja a(z) password aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getPassword() { return password; }
    /**
     * Beállítja a(z) password értékét a domain objektumon.
     *
     * @param password a művelethez átadott {@code password} érték
     */
    public void setPassword(String password) { this.password = password; }
    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    public boolean isPasswordConfigured() { return passwordConfigured; }
    /**
     * Beállítja a(z) passwordConfigured értékét a domain objektumon.
     *
     * @param passwordConfigured a művelethez átadott {@code passwordConfigured} érték
     */
    public void setPasswordConfigured(boolean passwordConfigured) { this.passwordConfigured = passwordConfigured; }
    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    public boolean isClearPassword() { return clearPassword; }
    /**
     * Beállítja a(z) clearPassword értékét a domain objektumon.
     *
     * @param clearPassword a művelethez átadott {@code clearPassword} érték
     */
    public void setClearPassword(boolean clearPassword) { this.clearPassword = clearPassword; }
    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    public boolean isSslVerificationDisabled() { return sslVerificationDisabled; }
    /**
     * Beállítja a(z) sslVerificationDisabled értékét a domain objektumon.
     *
     * @param sslVerificationDisabled a művelethez átadott {@code sslVerificationDisabled} érték
     */
    public void setSslVerificationDisabled(boolean sslVerificationDisabled) { this.sslVerificationDisabled = sslVerificationDisabled; }
    /**
     * Visszaadja a(z) truststore útvonal aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getTrustStorePath() { return trustStorePath; }
    /**
     * Beállítja a(z) truststore útvonal értékét a domain objektumon.
     *
     * @param trustStorePath a művelethez átadott {@code trustStorePath} érték
     */
    public void setTrustStorePath(String trustStorePath) { this.trustStorePath = trustStorePath; }
    /**
     * Visszaadja a(z) trustStorePassword aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getTrustStorePassword() { return trustStorePassword; }
    /**
     * Beállítja a(z) trustStorePassword értékét a domain objektumon.
     *
     * @param trustStorePassword a művelethez átadott {@code trustStorePassword} érték
     */
    public void setTrustStorePassword(String trustStorePassword) { this.trustStorePassword = trustStorePassword; }
    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    public boolean isTrustStorePasswordConfigured() { return trustStorePasswordConfigured; }
    /**
     * Beállítja a(z) trustStorePasswordConfigured értékét a domain objektumon.
     *
     * @param trustStorePasswordConfigured a művelethez átadott {@code trustStorePasswordConfigured} érték
     */
    public void setTrustStorePasswordConfigured(boolean trustStorePasswordConfigured) { this.trustStorePasswordConfigured = trustStorePasswordConfigured; }
    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    public boolean isClearTrustStorePassword() { return clearTrustStorePassword; }
    /**
     * Beállítja a(z) clearTrustStorePassword értékét a domain objektumon.
     *
     * @param clearTrustStorePassword a művelethez átadott {@code clearTrustStorePassword} érték
     */
    public void setClearTrustStorePassword(boolean clearTrustStorePassword) { this.clearTrustStorePassword = clearTrustStorePassword; }
    /**
     * Visszaadja a(z) truststore típusa aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getTrustStoreType() { return trustStoreType; }
    /**
     * Beállítja a(z) truststore típusa értékét a domain objektumon.
     *
     * @param trustStoreType a művelethez átadott {@code trustStoreType} érték
     */
    public void setTrustStoreType(String trustStoreType) { this.trustStoreType = trustStoreType; }
    /**
     * Visszaadja a(z) utolsó módosítás időpontja aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Instant getUpdatedAt() { return updatedAt; }
    /**
     * Beállítja a(z) utolsó módosítás időpontja értékét a domain objektumon.
     *
     * @param updatedAt a művelethez átadott {@code updatedAt} érték
     */
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
