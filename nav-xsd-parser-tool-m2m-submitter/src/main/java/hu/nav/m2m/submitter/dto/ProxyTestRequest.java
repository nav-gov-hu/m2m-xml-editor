package hu.nav.m2m.submitter.dto;

/**
 * A proxy/TLS kapcsolatpróba bemeneti adatait hordozó kérésobjektum.
 */
public class ProxyTestRequest {
    private boolean enabled;
    private String proxyUrl;
    private Integer proxyPort;
    private String username;
    private String password;
    private boolean clearPassword;
    private boolean sslVerificationDisabled;
    private String trustStorePath;
    private String trustStorePassword;
    private boolean clearTrustStorePassword;
    private String trustStoreType;

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
}
