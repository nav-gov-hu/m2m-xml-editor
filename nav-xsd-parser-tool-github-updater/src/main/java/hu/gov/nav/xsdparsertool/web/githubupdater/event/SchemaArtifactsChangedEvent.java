package hu.gov.nav.xsdparsertool.web.githubupdater.event;

/**
 * Jelzi, hogy GitHub-letöltés vagy lokális csomagimport következtében
 * megváltozott az aktív XSD/common XSD állománykészlet, ezért a séma-regiszter
 * cache újraépítése szükséges.
 *
 * @param source a változást kiváltó művelet rövid azonosítója
 */
public record SchemaArtifactsChangedEvent(String source) {
}
