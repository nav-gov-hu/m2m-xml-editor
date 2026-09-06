package hu.gov.nav.xsdparsertool.web.githubupdater.dto;
import java.time.Instant;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
/**
 * A GitHub proxybeállítások REST rétegben használt adatátviteli objektuma. A titkok tényleges értéke helyett külön jelzők támogatják a már konfigurált jelszó megtartását vagy törlését.
 */
public class GitHubProxySettingsDto {
    private boolean enabled; @Size(max = 2048) private String proxyUrl; @Min(1) @Max(65535) private Integer proxyPort;
    @Size(max = 256) private String username; @Size(max = 4096) private String password;
    private boolean passwordConfigured; private boolean clearPassword; private boolean sslVerificationDisabled;
    @Size(max = 2048) private String trustStorePath; @Size(max = 4096) private String trustStorePassword; private boolean trustStorePasswordConfigured;
    private boolean clearTrustStorePassword; @Pattern(regexp = "(?i)JKS|PKCS12") private String trustStoreType; private Instant updatedAt;
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
     * Visszaadja a(z) proxy jelszó konfiguráltsági jelzője aktuális értékét.
     *
     * @return proxy jelszó konfiguráltsági jelzője
     */
    public boolean isPasswordConfigured(){return passwordConfigured;}
    /**
     * Beállítja a(z) proxy jelszó konfiguráltsági jelzője értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setPasswordConfigured(boolean v){passwordConfigured=v;}
    /**
     * Visszaadja a(z) proxy jelszó törlési jelzője aktuális értékét.
     *
     * @return proxy jelszó törlési jelzője
     */
    public boolean isClearPassword(){return clearPassword;}
    /**
     * Beállítja a(z) proxy jelszó törlési jelzője értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setClearPassword(boolean v){clearPassword=v;}
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
     * Visszaadja a(z) truststore jelszó konfiguráltsági jelzője aktuális értékét.
     *
     * @return truststore jelszó konfiguráltsági jelzője
     */
    public boolean isTrustStorePasswordConfigured(){return trustStorePasswordConfigured;}
    /**
     * Beállítja a(z) truststore jelszó konfiguráltsági jelzője értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setTrustStorePasswordConfigured(boolean v){trustStorePasswordConfigured=v;}
    /**
     * Visszaadja a(z) truststore jelszó törlési jelzője aktuális értékét.
     *
     * @return truststore jelszó törlési jelzője
     */
    public boolean isClearTrustStorePassword(){return clearTrustStorePassword;}
    /**
     * Beállítja a(z) truststore jelszó törlési jelzője értékét.
     *
     * @param v a művelethez átadott {@code v} érték
     */
    public void setClearTrustStorePassword(boolean v){clearTrustStorePassword=v;}
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
