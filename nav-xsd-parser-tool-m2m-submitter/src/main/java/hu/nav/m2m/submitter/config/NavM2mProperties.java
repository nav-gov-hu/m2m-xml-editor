package hu.nav.m2m.submitter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

/**
 * A NAV M2M integráció külső konfigurációját leképező beállításcsoport; a végpontokat, hitelesítési adatokat, proxy/TLS és polling paramétereket egy helyen teszi elérhetővé.
 */
@ConfigurationProperties(prefix = "nav.m2m")
public class NavM2mProperties {
    /**
     * Eltávolítja a konfigurációs érték körül véletlenül megmaradt literális vagy valódi idézőjelpárokat.
     *
     * <p>A művelet ismételten bontja le a külső idézőjeleket, így például a konfigurációból
     * érkező {@code \"secret\"} vagy {@code "secret"} értékből {@code secret} lesz.</p>
     *
     * @param value a tisztítandó konfigurációs érték
     * @return a megtisztított érték; {@code null} bemenetnél {@code null}
     */
    public static String clean(String value) {
        if (value == null) return null;
        String result = value.trim();
        boolean changed = true;
        while (changed && result.length() >= 2) {
            changed = false;
            if ((result.startsWith("\\\"") && result.endsWith("\\\""))
                    || (result.startsWith("\\'") && result.endsWith("\\'"))) {
                result = result.substring(2, result.length() - 2).trim();
                changed = true;
            }
            if ((result.startsWith("\"") && result.endsWith("\""))
                    || (result.startsWith("'") && result.endsWith("'"))) {
                result = result.substring(1, result.length() - 1).trim();
                changed = true;
            }
        }
        return result;
    }
    private boolean mockMode = true;
    private String storageDirectory = "./data/uploads";
    private long maxInMemoryBizonylatApiBytes = 524_288_000L;
    private Endpoints endpoints = new Endpoints();
    private Auth auth = new Auth();
    private Signature signature = new Signature();
    private Taxpayer taxpayer = new Taxpayer();
    private Polling polling = new Polling();
    private StatusPoll statusPoll = new StatusPoll();
    private Submission submission = new Submission();
    private Attachment attachment = new Attachment();

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    public boolean isMockMode() { return mockMode; }
    /**
     * Beállítja a(z) mockMode értékét a domain objektumon.
     *
     * @param mockMode a művelethez átadott {@code mockMode} érték
     */
    public void setMockMode(boolean mockMode) { this.mockMode = mockMode; }
    /**
     * Visszaadja a(z) storageDirectory aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public String getStorageDirectory() { return storageDirectory; }
    /**
     * Beállítja a(z) storageDirectory értékét a domain objektumon.
     *
     * @param storageDirectory a művelethez átadott {@code storageDirectory} érték
     */
    public void setStorageDirectory(String storageDirectory) { this.storageDirectory = clean(storageDirectory); }
    /**
     * Visszaadja a(z) maxInMemoryBizonylatApiBytes aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public long getMaxInMemoryBizonylatApiBytes() { return maxInMemoryBizonylatApiBytes; }
    /**
     * Beállítja a(z) maxInMemoryBizonylatApiBytes értékét a domain objektumon.
     *
     * @param maxInMemoryBizonylatApiBytes a művelethez átadott {@code maxInMemoryBizonylatApiBytes} érték
     */
    public void setMaxInMemoryBizonylatApiBytes(long maxInMemoryBizonylatApiBytes) { this.maxInMemoryBizonylatApiBytes = maxInMemoryBizonylatApiBytes; }
    /**
     * Visszaadja a(z) endpoints aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Endpoints getEndpoints() { return endpoints; }
    /**
     * Beállítja a(z) endpoints értékét a domain objektumon.
     *
     * @param endpoints a művelethez átadott {@code endpoints} érték
     */
    public void setEndpoints(Endpoints endpoints) { this.endpoints = endpoints; }
    /**
     * Visszaadja a(z) auth aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Auth getAuth() { return auth; }
    /**
     * Beállítja a(z) auth értékét a domain objektumon.
     *
     * @param auth a művelethez átadott {@code auth} érték
     */
    public void setAuth(Auth auth) { this.auth = auth; }
    /**
     * Visszaadja a(z) signature aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Signature getSignature() { return signature; }
    /**
     * Beállítja a(z) signature értékét a domain objektumon.
     *
     * @param signature a művelethez átadott {@code signature} érték
     */
    public void setSignature(Signature signature) { this.signature = signature; }
    /**
     * Visszaadja a(z) taxpayer aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Taxpayer getTaxpayer() { return taxpayer; }
    /**
     * Beállítja a(z) taxpayer értékét a domain objektumon.
     *
     * @param taxpayer a művelethez átadott {@code taxpayer} érték
     */
    public void setTaxpayer(Taxpayer taxpayer) { this.taxpayer = taxpayer; }
    /**
     * Visszaadja a(z) polling aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Polling getPolling() { return polling; }
    /**
     * Beállítja a(z) polling értékét a domain objektumon.
     *
     * @param polling a művelethez átadott {@code polling} érték
     */
    public void setPolling(Polling polling) { this.polling = polling; }
    /**
     * Visszaadja a(z) statusPoll aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public StatusPoll getStatusPoll() { return statusPoll; }
    /**
     * Beállítja a(z) statusPoll értékét a domain objektumon.
     *
     * @param statusPoll a művelethez átadott {@code statusPoll} érték
     */
    public void setStatusPoll(StatusPoll statusPoll) { this.statusPoll = statusPoll; }
    /**
     * Visszaadja a(z) submission aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Submission getSubmission() { return submission; }
    /**
     * Beállítja a(z) submission értékét a domain objektumon.
     *
     * @param submission az aktuális M2M beküldési entitás
     */
    public void setSubmission(Submission submission) { this.submission = submission; }
    /**
     * Visszaadja a(z) attachment aktuális értékét.
     *
     * @return a művelet eredménye
     */
    public Attachment getAttachment() { return attachment; }
    /**
     * Beállítja a(z) attachment értékét a domain objektumon.
     *
     * @param attachment az aktuális csatolmány vagy csatolmányadat
     */
    public void setAttachment(Attachment attachment) { this.attachment = attachment; }

    /**
     * A NAV M2M submitter modul {@code Endpoints} típusának felelősségét megvalósító típus.
     */
    public static class Endpoints {
        private String commonBaseUrl;
        private String bizonylatBaseUrl;
        private String tokenPath = "/NavM2mCommon/tokenService/Token";
        private String noncePath = "/NavM2mCommon/userregistrationService/Nonce";
        private String activationPath = "/NavM2mCommon/userregistrationService/Activation";
        private String fileUploadPath = "/NavM2mCommon/filestoreUploadService/File";
        private String fileStatusPath = "/NavM2mCommon/filestoreDownloadService/File/{fileId}";
        private String bizonylatPath = "/NavM2mBizonylat/bizonylatService/Bizonylat";
        private String validacioPath = "/NavM2mBizonylat/bizonylatService/Validacio";
        private String kalkulacioPath = "/NavM2mBizonylat/bizonylatService/Kalkulacio";
        /**
         * Visszaadja a(z) commonBaseUrl aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getCommonBaseUrl() { return commonBaseUrl; }
        /**
         * Beállítja a(z) commonBaseUrl értékét a domain objektumon.
         *
         * @param commonBaseUrl a művelethez átadott {@code commonBaseUrl} érték
         */
        public void setCommonBaseUrl(String commonBaseUrl) { this.commonBaseUrl = clean(commonBaseUrl); }
        /**
         * Visszaadja a(z) bizonylatBaseUrl aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getBizonylatBaseUrl() { return bizonylatBaseUrl; }
        /**
         * Beállítja a(z) bizonylatBaseUrl értékét a domain objektumon.
         *
         * @param bizonylatBaseUrl a művelethez átadott {@code bizonylatBaseUrl} érték
         */
        public void setBizonylatBaseUrl(String bizonylatBaseUrl) { this.bizonylatBaseUrl = clean(bizonylatBaseUrl); }
        /**
         * Visszaadja a(z) tokenPath aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getTokenPath() { return tokenPath; }
        /**
         * Beállítja a(z) tokenPath értékét a domain objektumon.
         *
         * @param tokenPath a művelethez átadott {@code tokenPath} érték
         */
        public void setTokenPath(String tokenPath) { this.tokenPath = clean(tokenPath); }
        /**
         * Visszaadja a(z) noncePath aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getNoncePath() { return noncePath; }
        /**
         * Beállítja a(z) noncePath értékét a domain objektumon.
         *
         * @param noncePath a művelethez átadott {@code noncePath} érték
         */
        public void setNoncePath(String noncePath) { this.noncePath = clean(noncePath); }
        /**
         * Visszaadja a(z) activationPath aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getActivationPath() { return activationPath; }
        /**
         * Beállítja a(z) activationPath értékét a domain objektumon.
         *
         * @param activationPath a művelethez átadott {@code activationPath} érték
         */
        public void setActivationPath(String activationPath) { this.activationPath = clean(activationPath); }
        /**
         * Visszaadja a(z) fileUploadPath aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getFileUploadPath() { return fileUploadPath; }
        /**
         * Beállítja a(z) fileUploadPath értékét a domain objektumon.
         *
         * @param fileUploadPath a művelethez átadott {@code fileUploadPath} érték
         */
        public void setFileUploadPath(String fileUploadPath) { this.fileUploadPath = clean(fileUploadPath); }
        /**
         * Visszaadja a(z) fileStatusPath aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getFileStatusPath() { return fileStatusPath; }
        /**
         * Beállítja a(z) fileStatusPath értékét a domain objektumon.
         *
         * @param fileStatusPath a művelethez átadott {@code fileStatusPath} érték
         */
        public void setFileStatusPath(String fileStatusPath) { this.fileStatusPath = clean(fileStatusPath); }
        /**
         * Visszaadja a(z) bizonylatPath aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getBizonylatPath() { return bizonylatPath; }
        /**
         * Beállítja a(z) bizonylatPath értékét a domain objektumon.
         *
         * @param bizonylatPath a művelethez átadott {@code bizonylatPath} érték
         */
        public void setBizonylatPath(String bizonylatPath) { this.bizonylatPath = clean(bizonylatPath); }
        /**
         * Visszaadja a(z) validacioPath aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getValidacioPath() { return validacioPath; }
        /**
         * Beállítja a(z) validacioPath értékét a domain objektumon.
         *
         * @param validacioPath a művelethez átadott {@code validacioPath} érték
         */
        public void setValidacioPath(String validacioPath) { this.validacioPath = clean(validacioPath); }
        /**
         * Visszaadja a(z) kalkulacioPath aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getKalkulacioPath() { return kalkulacioPath; }
        /**
         * Beállítja a(z) kalkulacioPath értékét a domain objektumon.
         *
         * @param kalkulacioPath a művelethez átadott {@code kalkulacioPath} érték
         */
        public void setKalkulacioPath(String kalkulacioPath) { this.kalkulacioPath = clean(kalkulacioPath); }
    }

    /**
     * A NAV M2M submitter modul {@code Auth} típusának felelősségét megvalósító típus.
     */
    public static class Auth {
        private String clientId;
        private String clientSecret;
        private String username;
        private String password;
        /**
         * Visszaadja a(z) clientId aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getClientId() { return clientId; }
        /**
         * Beállítja a(z) clientId értékét a domain objektumon.
         *
         * @param clientId a művelethez átadott {@code clientId} érték
         */
        public void setClientId(String clientId) { this.clientId = clean(clientId); }
        /**
         * Visszaadja a(z) clientSecret aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getClientSecret() { return clientSecret; }
        /**
         * Beállítja a(z) clientSecret értékét a domain objektumon.
         *
         * @param clientSecret a művelethez átadott {@code clientSecret} érték
         */
        public void setClientSecret(String clientSecret) { this.clientSecret = clean(clientSecret); }
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
        public void setUsername(String username) { this.username = clean(username); }
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
        public void setPassword(String password) { this.password = clean(password); }
    }

    /**
     * A NAV M2M submitter modul {@code Signature} típusának felelősségét megvalósító típus.
     */
    public static class Signature {
        /**
         * API Key 3. eleme: az aláírókulcs első fele.
         * Korábbi konfigurációs alias: key-part-1.
         */
        private String keyFirstPart;

        /**
         * API Key 4. eleme: egyszer használatos nonce.
         * Ez NEM része közvetlenül az aláírási kulcsnak.
         */
        private String nonce;

        /**
         * Nonce beváltás válaszából kapott signatureKeySecondPart.
         * Korábbi konfigurációs alias: key-part-2, de ez nem lehet a nonce.
         */
        private String keySecondPart;

        /**
         * Visszaadja a(z) keyFirstPart aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getKeyFirstPart() { return keyFirstPart; }
        /**
         * Beállítja a(z) keyFirstPart értékét a domain objektumon.
         *
         * @param keyFirstPart a művelethez átadott {@code keyFirstPart} érték
         */
        public void setKeyFirstPart(String keyFirstPart) { this.keyFirstPart = clean(keyFirstPart); }
        /**
         * Visszaadja a(z) nonce aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getNonce() { return nonce; }
        /**
         * Beállítja a(z) nonce értékét a domain objektumon.
         *
         * @param nonce a művelethez átadott {@code nonce} érték
         */
        public void setNonce(String nonce) { this.nonce = clean(nonce); }
        /**
         * Visszaadja a(z) keySecondPart aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getKeySecondPart() { return keySecondPart; }
        /**
         * Beállítja a(z) keySecondPart értékét a domain objektumon.
         *
         * @param keySecondPart a művelethez átadott {@code keySecondPart} érték
         */
        public void setKeySecondPart(String keySecondPart) { this.keySecondPart = clean(keySecondPart); }

        /** Visszafelé kompatibilitás a korábbi key-part-1 property-hez. */
        public String getKeyPart1() { return keyFirstPart; }
        /**
         * Beállítja a(z) keyPart1 értékét a domain objektumon.
         *
         * @param keyPart1 a művelethez átadott {@code keyPart1} érték
         */
        public void setKeyPart1(String keyPart1) { this.keyFirstPart = clean(keyPart1); }

        /** Visszafelé kompatibilitás a korábbi key-part-2 property-hez. */
        public String getKeyPart2() { return keySecondPart; }
        /**
         * Beállítja a(z) keyPart2 értékét a domain objektumon.
         *
         * @param keyPart2 a művelethez átadott {@code keyPart2} érték
         */
        public void setKeyPart2(String keyPart2) { this.keySecondPart = clean(keyPart2); }
    }

    /**
     * A NAV M2M submitter modul {@code Taxpayer} típusának felelősségét megvalósító típus.
     */
    public static class Taxpayer {
        private String testTaxNumber;
        private String realTaxNumber;
        /**
         * Visszaadja a(z) testTaxNumber aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getTestTaxNumber() { return testTaxNumber; }
        /**
         * Beállítja a(z) testTaxNumber értékét a domain objektumon.
         *
         * @param testTaxNumber a művelethez átadott {@code testTaxNumber} érték
         */
        public void setTestTaxNumber(String testTaxNumber) { this.testTaxNumber = clean(testTaxNumber); }
        /**
         * Visszaadja a(z) realTaxNumber aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public String getRealTaxNumber() { return realTaxNumber; }
        /**
         * Beállítja a(z) realTaxNumber értékét a domain objektumon.
         *
         * @param realTaxNumber a művelethez átadott {@code realTaxNumber} érték
         */
        public void setRealTaxNumber(String realTaxNumber) { this.realTaxNumber = clean(realTaxNumber); }
    }

    /**
     * A NAV M2M submitter modul {@code Polling} típusának felelősségét megvalósító típus.
     */
    public static class Polling {
        private long intervalMs = 5000;
        private int maxAttempts = 20;
        /**
         * Visszaadja a(z) intervalMs aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public long getIntervalMs() { return intervalMs; }
        /**
         * Beállítja a(z) intervalMs értékét a domain objektumon.
         *
         * @param intervalMs a művelethez átadott {@code intervalMs} érték
         */
        public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
        /**
         * Visszaadja a(z) maxAttempts aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public int getMaxAttempts() { return maxAttempts; }
        /**
         * Beállítja a(z) maxAttempts értékét a domain objektumon.
         *
         * @param maxAttempts a művelethez átadott {@code maxAttempts} érték
         */
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    }

    /**
     * A NAV M2M submitter modul {@code StatusPoll} típusának felelősségét megvalósító típus.
     */
    public static class StatusPoll {
        private boolean enabled = true;
        private long fixedDelayMs = 60000L;
        private Duration interval = Duration.ofSeconds(60);
        private Duration maxAge = Duration.ofHours(24);
        private int batchSize = 50;
        private int maxAttempts = 1440;

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
         * Visszaadja a(z) fixedDelayMs aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public long getFixedDelayMs() { return fixedDelayMs; }
        /**
         * Beállítja a(z) fixedDelayMs értékét a domain objektumon.
         *
         * @param fixedDelayMs a művelethez átadott {@code fixedDelayMs} érték
         */
        public void setFixedDelayMs(long fixedDelayMs) { this.fixedDelayMs = fixedDelayMs; }
        /**
         * Visszaadja a(z) interval aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public Duration getInterval() { return interval; }
        /**
         * Beállítja a(z) interval értékét a domain objektumon.
         *
         * @param interval a művelethez átadott {@code interval} érték
         */
        public void setInterval(Duration interval) { this.interval = interval; }
        /**
         * Visszaadja a(z) maxAge aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public Duration getMaxAge() { return maxAge; }
        /**
         * Beállítja a(z) maxAge értékét a domain objektumon.
         *
         * @param maxAge a művelethez átadott {@code maxAge} érték
         */
        public void setMaxAge(Duration maxAge) { this.maxAge = maxAge; }
        /**
         * Visszaadja a(z) batchSize aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public int getBatchSize() { return batchSize; }
        /**
         * Beállítja a(z) batchSize értékét a domain objektumon.
         *
         * @param batchSize a művelethez átadott {@code batchSize} érték
         */
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        /**
         * Visszaadja a(z) maxAttempts aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public int getMaxAttempts() { return maxAttempts; }
        /**
         * Beállítja a(z) maxAttempts értékét a domain objektumon.
         *
         * @param maxAttempts a művelethez átadott {@code maxAttempts} érték
         */
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    }

    /**
     * A NAV M2M submitter modul {@code Submission} típusának felelősségét megvalósító típus.
     */
    public static class Submission {
        /** true: fejlesztői környezetben sikeres beküldés után is engedélyezett az újraküldés. */
        private boolean allowResubmit = true;
        /**
         * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
         *
         * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
         */
        public boolean isAllowResubmit() { return allowResubmit; }
        /**
         * Beállítja a(z) allowResubmit értékét a domain objektumon.
         *
         * @param allowResubmit a művelethez átadott {@code allowResubmit} érték
         */
        public void setAllowResubmit(boolean allowResubmit) { this.allowResubmit = allowResubmit; }
    }

    /**
     * A NAV M2M submitter modul {@code Attachment} típusának felelősségét megvalósító típus.
     */
    public static class Attachment {
        /** NAV oldali csatolmány-élettartam. */
        private Duration validityDuration = Duration.ofDays(3);
        /** Beküldés előtti biztonsági tartalék. */
        private Duration expirySafetyMargin = Duration.ofHours(2);
        /**
         * Visszaadja a(z) validityDuration aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public Duration getValidityDuration() { return validityDuration; }
        /**
         * Beállítja a(z) validityDuration értékét a domain objektumon.
         *
         * @param validityDuration a művelethez átadott {@code validityDuration} érték
         */
        public void setValidityDuration(Duration validityDuration) { this.validityDuration = validityDuration; }
        /**
         * Visszaadja a(z) expirySafetyMargin aktuális értékét.
         *
         * @return a művelet eredménye
         */
        public Duration getExpirySafetyMargin() { return expirySafetyMargin; }
        /**
         * Beállítja a(z) expirySafetyMargin értékét a domain objektumon.
         *
         * @param expirySafetyMargin a művelethez átadott {@code expirySafetyMargin} érték
         */
        public void setExpirySafetyMargin(Duration expirySafetyMargin) { this.expirySafetyMargin = expirySafetyMargin; }
    }

}
