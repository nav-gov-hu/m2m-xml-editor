package hu.nav.m2m.submitter.service.nav.audit;

import java.util.ArrayList;
import java.util.List;

/**
 * Szálhoz kötötten gyűjti az aktuális NAV HTTP művelet audit trace eseményeit, majd átadja azokat a magasabb rétegnek.
 */
public final class NavHttpAuditHolder {
    private static final ThreadLocal<List<NavHttpTrace>> TRACES = ThreadLocal.withInitial(ArrayList::new);

    /**
     * Létrehozza a(z) {@code NavHttpAuditHolder} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     */
    private NavHttpAuditHolder() {}

    /**
     * A(z) {@code add} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param trace a művelethez átadott {@code trace} érték
     */
    public static void add(NavHttpTrace trace) {
        if (trace != null) {
            TRACES.get().add(trace);
        }
    }

    /**
     * Visszaadja és egyúttal kiüríti az aktuális szálhoz összegyűjtött HTTP trace eseményeket, így azok pontosan egy magasabb szintű műveleti naplóhoz rendelhetők.
     *
     * @return a művelet eredménye
     */
    public static List<NavHttpTrace> drain() {
        List<NavHttpTrace> copy = new ArrayList<>(TRACES.get());
        TRACES.get().clear();
        return copy;
    }

    /**
     * Az M2M életciklus vagy feldolgozási eredmény alapján frissíti a kezelt domain/runtime állapotot; a változás a hívó tranzakciójának része lehet.
     */
    public static void clear() {
        TRACES.remove();
    }
}
