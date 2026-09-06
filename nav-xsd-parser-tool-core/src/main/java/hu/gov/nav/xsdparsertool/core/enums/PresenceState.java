package hu.gov.nav.xsdparsertool.core.enums;
/**
 * Az XML-példány egy elemének jelenléti állapotát írja le.
 *
 * <p>A feldolgozási és megjelenítési rétegek ezzel különböztetik meg a ténylegesen
 * meglévő elemet, a hiányzó elemet és a csak előnézeti/kommentelt állapotot.</p>
 */
public enum PresenceState {
    PRESENT,
    ABSENT,
    COMMENTED_PREVIEW
}
