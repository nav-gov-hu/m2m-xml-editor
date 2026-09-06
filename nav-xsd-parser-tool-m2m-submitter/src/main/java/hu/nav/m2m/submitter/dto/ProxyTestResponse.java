package hu.nav.m2m.submitter.dto;

/**
 * A proxy/TLS kapcsolatpróba eredményét hordozó válaszobjektum.
 */
public class ProxyTestResponse {
    private boolean success;
    private boolean proxyEnabled;
    private String testUrl;
    private Integer httpStatus;
    private long durationMs;
    private String message;
    private String proxySnapshot;

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    public boolean isSuccess() { return success; }
    /**
     * Beállítja a(z) success értékét a domain objektumon.
     *
     * @param success a művelethez átadott {@code success} érték
     */
    public void setSuccess(boolean success) { this.success = success; }
    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    public boolean isProxyEnabled() { return proxyEnabled; }
    /**
     * Beállítja a(z) proxyEnabled értékét a domain objektumon.
     *
     * @param proxyEnabled a művelethez átadott {@code proxyEnabled} érték
     */
    public void setProxyEnabled(boolean proxyEnabled) { this.proxyEnabled = proxyEnabled; }
    /**
     * Visszaadja a(z) testUrl aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getTestUrl() { return testUrl; }
    /**
     * Beállítja a(z) testUrl értékét a domain objektumon.
     *
     * @param testUrl a művelethez átadott {@code testUrl} érték
     */
    public void setTestUrl(String testUrl) { this.testUrl = testUrl; }
    /**
     * Visszaadja a(z) httpStatus aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Integer getHttpStatus() { return httpStatus; }
    /**
     * Beállítja a(z) httpStatus értékét a domain objektumon.
     *
     * @param httpStatus a művelethez átadott {@code httpStatus} érték
     */
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    /**
     * Visszaadja a(z) durationMs aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public long getDurationMs() { return durationMs; }
    /**
     * Beállítja a(z) durationMs értékét a domain objektumon.
     *
     * @param durationMs a művelethez átadott {@code durationMs} érték
     */
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    /**
     * Visszaadja a(z) message aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getMessage() { return message; }
    /**
     * Beállítja a(z) message értékét a domain objektumon.
     *
     * @param message a művelethez átadott {@code message} érték
     */
    public void setMessage(String message) { this.message = message; }
    /**
     * Visszaadja a(z) proxySnapshot aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getProxySnapshot() { return proxySnapshot; }
    /**
     * Beállítja a(z) proxySnapshot értékét a domain objektumon.
     *
     * @param proxySnapshot a művelethez átadott {@code proxySnapshot} érték
     */
    public void setProxySnapshot(String proxySnapshot) { this.proxySnapshot = proxySnapshot; }
}
