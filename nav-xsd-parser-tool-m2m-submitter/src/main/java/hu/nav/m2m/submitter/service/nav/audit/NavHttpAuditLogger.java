package hu.nav.m2m.submitter.service.nav.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Centralized sink for already-formatted NAV HTTP audit messages. */
public final class NavHttpAuditLogger {
    private static final Logger LOG = LoggerFactory.getLogger("NAV_HTTP_AUDIT");

    /**
     * Létrehozza a(z) {@code NavHttpAuditLogger} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     */
    private NavHttpAuditLogger() {
    }

    /**
     * A(z) {@code trace} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param formattedTrace a művelethez átadott {@code formattedTrace} érték
     */
    public static void trace(String formattedTrace) {
        LOG.info("{}", singleLine(formattedTrace));
    }

    /**
     * A(z) {@code singleLine} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private static String singleLine(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r", "\\r").replace("\n", "\\n");
    }
}
