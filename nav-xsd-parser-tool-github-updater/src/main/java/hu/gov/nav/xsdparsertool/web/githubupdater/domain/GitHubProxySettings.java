package hu.gov.nav.xsdparsertool.web.githubupdater.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A GitHub hálózati eléréséhez használt proxy-, hitelesítési és truststore-beállításokat reprezentáló perzisztens entitás.
 */
@Entity
@Table(name = "github_proxy_settings")
public class GitHubProxySettings {
    @Id private Long id = 1L;
    @Column(name = "enabled", nullable = false) private boolean enabled;
    @Column(name = "proxy_url", length = 1000) private String proxyUrl;
    @Column(name = "proxy_port") private Integer proxyPort;
    @Column(name = "proxy_username", length = 512) private String username;
    @Column(name = "proxy_password", length = 2000) private String password;
    @Column(name = "ssl_verification_disabled", nullable = false) private boolean sslVerificationDisabled;
    @Column(name = "trust_store_path", length = 1000) private String trustStorePath;
    @Column(name = "trust_store_password", length = 2000) private String trustStorePassword;
    @Column(name = "trust_store_type", length = 30) private String trustStoreType = "JKS";
    @Column(name = "updated_at") private Instant updatedAt;
    /**
     * Frissíti a proxy/TLS konfiguráció utolsó módosítási időpontját létrehozás és módosítás előtt.
     */
    @PrePersist @PreUpdate void touch(){ updatedAt = Instant.now(); }
    /**
     * Visszaadja a(z) adatbázis-azonosító aktuális értékét.
     *
     * @return adatbázis-azonosító
     */
    public Long getId(){return id;}
    /**
     * Beállítja a(z) adatbázis-azonosító értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setId(Long v){id=v;}
    /**
     * Visszaadja a(z) engedélyezettság aktuális értékét.
     *
     * @return engedélyezettság
     */
    public boolean isEnabled(){return enabled;}
    /**
     * Beállítja a(z) engedélyezettság értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setEnabled(boolean v){enabled=v;}
    /**
     * Visszaadja a(z) proxy címe aktuális értékét.
     *
     * @return proxy címe
     */
    public String getProxyUrl(){return proxyUrl;}
    /**
     * Beállítja a(z) proxy címe értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setProxyUrl(String v){proxyUrl=v;}
    /**
     * Visszaadja a(z) proxy portja aktuális értékét.
     *
     * @return proxy portja
     */
    public Integer getProxyPort(){return proxyPort;}
    /**
     * Beállítja a(z) proxy portja értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setProxyPort(Integer v){proxyPort=v;}
    /**
     * Visszaadja a(z) proxy felhasználónév aktuális értékét.
     *
     * @return proxy felhasználónév
     */
    public String getUsername(){return username;}
    /**
     * Beállítja a(z) proxy felhasználónév értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setUsername(String v){username=v;}
    /**
     * Visszaadja a(z) proxy jelszó aktuális értékét.
     *
     * @return proxy jelszó
     */
    public String getPassword(){return password;}
    /**
     * Beállítja a(z) proxy jelszó értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setPassword(String v){password=v;}
    /**
     * Visszaadja a(z) SSL tanúsítvány-ellenőrzés kikapcsolási jelzője aktuális értékét.
     *
     * @return SSL tanúsítvány-ellenőrzés kikapcsolási jelzője
     */
    public boolean isSslVerificationDisabled(){return sslVerificationDisabled;}
    /**
     * Beállítja a(z) SSL tanúsítvány-ellenőrzés kikapcsolási jelzője értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setSslVerificationDisabled(boolean v){sslVerificationDisabled=v;}
    /**
     * Visszaadja a(z) egyedi truststore útvonala aktuális értékét.
     *
     * @return egyedi truststore útvonala
     */
    public String getTrustStorePath(){return trustStorePath;}
    /**
     * Beállítja a(z) egyedi truststore útvonala értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setTrustStorePath(String v){trustStorePath=v;}
    /**
     * Visszaadja a(z) truststore jelszó aktuális értékét.
     *
     * @return truststore jelszó
     */
    public String getTrustStorePassword(){return trustStorePassword;}
    /**
     * Beállítja a(z) truststore jelszó értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setTrustStorePassword(String v){trustStorePassword=v;}
    /**
     * Visszaadja a(z) truststore típusa aktuális értékét.
     *
     * @return truststore típusa
     */
    public String getTrustStoreType(){return trustStoreType;}
    /**
     * Beállítja a(z) truststore típusa értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setTrustStoreType(String v){trustStoreType=v;}
    /**
     * Visszaadja a(z) utolsó módosítás időpontja aktuális értékét.
     *
     * @return utolsó módosítás időpontja
     */
    public Instant getUpdatedAt(){return updatedAt;}
    /**
     * Beállítja a(z) utolsó módosítás időpontja értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setUpdatedAt(Instant v){updatedAt=v;}
}
