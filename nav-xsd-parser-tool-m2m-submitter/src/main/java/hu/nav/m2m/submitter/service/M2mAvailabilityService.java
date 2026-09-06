package hu.nav.m2m.submitter.service;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Az M2M funkció használhatóságát ellenőrzi a kötelező konfiguráció és runtime feltételek alapján.
 */
@Service
public class M2mAvailabilityService {
    private final NavM2mProperties properties;

    /**
     * Létrehozza a(z) {@code M2mAvailabilityService} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param properties az M2M külső konfiguráció
     */
    public M2mAvailabilityService(NavM2mProperties properties) {
        this.properties = properties;
    }

    /**
     * Összeállítja az M2M funkció aktuális elérhetőségi állapotát a konfiguráció alapján.
     *
     * @return a művelet eredménye
     */
    public Availability availability() {
        Map<String, String> required = new LinkedHashMap<>();
        required.put("nav.m2m.auth.client-id", properties.getAuth().getClientId());
        required.put("nav.m2m.auth.client-secret", properties.getAuth().getClientSecret());
        required.put("nav.m2m.auth.username", properties.getAuth().getUsername());
        required.put("nav.m2m.auth.password", properties.getAuth().getPassword());
        required.put("nav.m2m.signature.key-first-part", properties.getSignature().getKeyFirstPart());
        required.put("nav.m2m.signature.nonce", properties.getSignature().getNonce());

        List<String> missing = new ArrayList<>();
        required.forEach((key, value) -> {
            if (value == null || value.isBlank()) missing.add(key);
        });
        return new Availability(missing.isEmpty(), List.copyOf(missing));
    }

    /**
     * Ellenőrzi a kötelező M2M konfiguráció meglétét, és hiány esetén kontrollált hibával megszakítja a műveletet.
     */
    public void requireConfigured() {
        Availability state = availability();
        if (!state.configured()) {
            throw new IllegalStateException("Az M2M művelethez szükséges hitelesítési adatok nincsenek teljesen beállítva. Hiányzó kulcsok: " + String.join(", ", state.missingKeys()));
        }
    }

    /**
     * A NAV M2M submitter modul {@code Availability} típusának felelősségét megvalósító típus.
     */
    /**
     * Létrehozza a(z) {@code Availability} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param configured a művelethez átadott {@code configured} érték
     * @param missingKeys a művelethez átadott {@code missingKeys} érték
     */
    public record Availability(boolean configured, List<String> missingKeys) {}
}
