package hu.nav.m2m.submitter.service;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime-only tarolo a nonce bevaltas soran kapott signatureKeySecondPart ertekhez.
 * Nem ir adatbazisba es nem irja vissza a kulso YAML fajlt.
 * Az ertek addig el, amig az alkalmazas fut, vagy amig uj nonce bevaltas felul nem irja.
 */
@Service
public class RuntimeSignatureKeyService {
    private final NavM2mProperties properties;

    private volatile String runtimeKeySecondPart;
    private volatile Instant redeemedAt;
    private volatile String source;

    /**
     * Létrehozza a(z) {@code RuntimeSignatureKeyService} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param properties az M2M külső konfiguráció
     */
    public RuntimeSignatureKeyService(NavM2mProperties properties) {
        this.properties = properties;
    }

    /**
     * Az M2M életciklus vagy feldolgozási eredmény alapján frissíti a kezelt domain/runtime állapotot; a változás a hívó tranzakciójának része lehet.
     *
     * @param signatureKeySecondPart a művelethez átadott {@code signatureKeySecondPart} érték
     */
    public synchronized void storeRedeemedSecondPart(String signatureKeySecondPart) {
        if (signatureKeySecondPart == null || signatureKeySecondPart.isBlank()) {
            return;
        }
        this.runtimeKeySecondPart = signatureKeySecondPart;
        this.redeemedAt = Instant.now();
        this.source = "RUNTIME_REDEEM_NONCE";
    }

    /**
     * Az M2M életciklus vagy feldolgozási eredmény alapján frissíti a kezelt domain/runtime állapotot; a változás a hívó tranzakciójának része lehet.
     */
    public synchronized void clearRuntimeSecondPart() {
        this.runtimeKeySecondPart = null;
        this.redeemedAt = null;
        this.source = null;
    }

    /**
     * Az aláíráshoz használandó második kulcsrészt oldja fel: elsőbbséget élvez a nonce beváltásból származó runtime érték, ennek hiányában a konfigurált fallback használható.
     *
     * @return a művelet eredménye
     */
    public String effectiveKeySecondPart() {
        if (runtimeKeySecondPart != null && !runtimeKeySecondPart.isBlank()) {
            return runtimeKeySecondPart;
        }
        return properties.getSignature().getKeySecondPart() == null ? "" : properties.getSignature().getKeySecondPart();
    }

    /**
     * A(z) {@code effectiveSource} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    public String effectiveSource() {
        if (runtimeKeySecondPart != null && !runtimeKeySecondPart.isBlank()) {
            return source == null ? "RUNTIME_REDEEM_NONCE" : source;
        }
        String configured = properties.getSignature().getKeySecondPart();
        return configured == null || configured.isBlank() ? "MISSING" : "CONFIG_YAML";
    }

    /**
     * A(z) {@code hasRuntimeSecondPart} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    public boolean hasRuntimeSecondPart() {
        return runtimeKeySecondPart != null && !runtimeKeySecondPart.isBlank();
    }

    /**
     * A(z) {@code redeemedAt} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    public Instant redeemedAt() {
        return redeemedAt;
    }

    /**
     * A(z) {@code snapshot} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    public Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("keyFirstPart", properties.getSignature().getKeyFirstPart());
        m.put("nonce", properties.getSignature().getNonce());
        m.put("configuredKeySecondPart", properties.getSignature().getKeySecondPart());
        m.put("runtimeKeySecondPart", runtimeKeySecondPart);
        m.put("effectiveKeySecondPart", effectiveKeySecondPart());
        m.put("effectiveKeySecondPartSource", effectiveSource());
        m.put("runtimeRedeemedAt", redeemedAt == null ? null : redeemedAt.toString());
        m.put("note", "A runtimeKeySecondPart csak memoriai ertek: uj nonce bevaltasig vagy alkalmazas ujrainditasig el.");
        return m;
    }
}
