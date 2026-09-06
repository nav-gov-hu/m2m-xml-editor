package hu.nav.m2m.submitter.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Az M2M HTTP kommunikációhoz használt proxy- és truststore-beállítások perzisztens entitása.
 */
@Entity
@Table(name = "m2m_proxy_settings")
public class ProxySettings {
    @Id
    private Long id = 1L;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    @Column(name = "proxy_url", length = 1000)
    private String proxyUrl;

    @Column(name = "proxy_port")
    private Integer proxyPort;

    @Column(name = "proxy_username", length = 512)
    private String username;

    @Column(name = "proxy_password", length = 2000)
    private String password;

    @Column(name = "ssl_verification_disabled", nullable = false)
    private boolean sslVerificationDisabled = false;

    @Column(name = "trust_store_path", length = 1000)
    private String trustStorePath;

    @Column(name = "trust_store_password", length = 2000)
    private String trustStorePassword;

    @Column(name = "trust_store_type", length = 30)
    private String trustStoreType = "JKS";

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Beállítja a proxy/TLS konfiguráció utolsó módosítási időpontját létrehozás és frissítés előtt. */
    @PrePersist
    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    /**
     * Visszaadja a(z) azonosító aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Long getId() { return id; }
    /**
     * Beállítja a(z) azonosító értékét a domain objektumon.
     *
     * @param id a művelethez átadott {@code id} érték
     */
    public void setId(Long id) { this.id = id; }
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
