package hu.nav.m2m.submitter.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import hu.nav.m2m.submitter.domain.M2mAttachment;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Egységesen meghatározza a NAV-oldali csatolmány-életciklust és azt,
 * hogy a csatolmány megújítása az aktuális állapotban engedélyezett-e.
 */
public final class AttachmentLifecycleEvaluator {

    /**
     * Létrehozza a(z) {@code AttachmentLifecycleEvaluator} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     */
    private AttachmentLifecycleEvaluator() {
    }

    /**
     * A NAV M2M submitter modul {@code State} típusának felelősségét megvalósító típus.
     */
    public enum State {
        VALID,
        EXPIRING_SOON,
        EXPIRED,
        NOT_UPLOADED,
        UNKNOWN
    }

    /**
     * A NAV M2M submitter modul {@code Evaluation} típusának felelősségét megvalósító típus.
     */
    /**
     * Létrehozza a(z) {@code Evaluation} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param state a művelethez átadott {@code state} érték
     * @param label a művelethez átadott {@code label} érték
     * @param refreshAllowed a művelethez átadott {@code refreshAllowed} érték
     * @param reason a művelethez átadott {@code reason} érték
     * @param localFileAvailable a művelethez átadott {@code localFileAvailable} érték
     */
    public record Evaluation(
            State state,
            String label,
            boolean refreshAllowed,
            String reason,
            boolean localFileAvailable
    ) {
    }

    /**
     * A(z) {@code evaluate} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param attachment az aktuális csatolmány vagy csatolmányadat
     * @param properties az M2M külső konfiguráció
     * @param now a művelethez átadott {@code now} érték
     * @return a művelet eredménye
     */
    public static Evaluation evaluate(
            M2mAttachment attachment,
            NavM2mProperties properties,
            Instant now
    ) {
        Instant evaluationTime = now == null ? Instant.now() : now;
        boolean localFileAvailable = isLocalFileAvailable(attachment == null ? null : attachment.getStoragePath());

        if (attachment == null) {
            return evaluation(
                    State.UNKNOWN,
                    "Ismeretlen",
                    false,
                    "A csatolmány adatai nem állnak rendelkezésre.",
                    false
            );
        }

        String navFileId = attachment.getNavFileId();
        if (navFileId == null || navFileId.isBlank()) {
            return evaluation(
                    State.NOT_UPLOADED,
                    "Nincs feltöltve",
                    localFileAvailable,
                    appendLocalFileReason(
                            "A csatolmányhoz nincs érvényes NAV fileId, ezért a beküldés előtt meg kell újítani.",
                            localFileAvailable
                    ),
                    localFileAvailable
            );
        }

        Instant uploadedAt = attachment.getNavUploadedAt();
        Instant expiresAt = attachment.getNavExpiresAt();
        if (uploadedAt == null || expiresAt == null) {
            return evaluation(
                    State.UNKNOWN,
                    "Ismeretlen",
                    localFileAvailable,
                    appendLocalFileReason(
                            "A NAV feltöltési vagy lejárati idő nem áll rendelkezésre, ezért a csatolmány érvényessége nem igazolható.",
                            localFileAvailable
                    ),
                    localFileAvailable
            );
        }

        if (!expiresAt.isAfter(evaluationTime)) {
            return evaluation(
                    State.EXPIRED,
                    "Lejárt",
                    localFileAvailable,
                    appendLocalFileReason(
                            "A csatolmány NAV-oldali érvényessége lejárt.",
                            localFileAvailable
                    ),
                    localFileAvailable
            );
        }

        Duration safetyMargin = properties == null || properties.getAttachment() == null
                ? Duration.ZERO
                : properties.getAttachment().getExpirySafetyMargin();
        if (safetyMargin == null || safetyMargin.isNegative()) {
            safetyMargin = Duration.ZERO;
        }

        Instant refreshThreshold = expiresAt.minus(safetyMargin);
        if (!refreshThreshold.isAfter(evaluationTime)) {
            return evaluation(
                    State.EXPIRING_SOON,
                    "Hamarosan lejár",
                    localFileAvailable,
                    appendLocalFileReason(
                            "A csatolmány a beállított biztonsági időn belül lejár, ezért a beküldés előtt meg kell újítani.",
                            localFileAvailable
                    ),
                    localFileAvailable
            );
        }

        String reason = "A csatolmány még érvényes; megújítás csak a biztonsági időszakban vagy lejárat után indítható.";
        if (!localFileAvailable) {
            reason += " A helyi csatolmányfájl nem található, ezért később sem lesz automatikusan megújítható.";
        }
        return evaluation(State.VALID, "Érvényes", false, reason, localFileAvailable);
    }

    /**
     * A(z) {@code evaluation} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param state a művelethez átadott {@code state} érték
     * @param label a művelethez átadott {@code label} érték
     * @param refreshAllowed a művelethez átadott {@code refreshAllowed} érték
     * @param reason a művelethez átadott {@code reason} érték
     * @param localFileAvailable a művelethez átadott {@code localFileAvailable} érték
     * @return a művelet eredménye
     */
    private static Evaluation evaluation(
            State state,
            String label,
            boolean refreshAllowed,
            String reason,
            boolean localFileAvailable
    ) {
        return new Evaluation(state, label, refreshAllowed, reason, localFileAvailable);
    }

    /**
     * A(z) {@code appendLocalFileReason} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param reason a művelethez átadott {@code reason} érték
     * @param localFileAvailable a művelethez átadott {@code localFileAvailable} érték
     * @return a művelet eredménye
     */
    private static String appendLocalFileReason(String reason, boolean localFileAvailable) {
        if (localFileAvailable) {
            return reason;
        }
        return reason + " A helyi csatolmányfájl nem található, ezért a megújítás nem indítható.";
    }

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param storagePath a művelethez átadott {@code storagePath} érték
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private static boolean isLocalFileAvailable(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return false;
        }
        try {
            return ExceptionSafeOperations.isRegularFile(Path.of(storagePath));
        } catch (InvalidPathException | SecurityException ignored) {
            return false;
        }
    }
}
