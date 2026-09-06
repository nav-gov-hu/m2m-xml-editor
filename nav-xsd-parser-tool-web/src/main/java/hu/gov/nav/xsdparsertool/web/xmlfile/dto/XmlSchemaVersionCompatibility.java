package hu.gov.nav.xsdparsertool.web.xmlfile.dto;

/**
 * Az XML-ben deklarált űrlapverzió és a ténylegesen feloldott XSD-verzió kompatibilitási eredménye.
 */
public record XmlSchemaVersionCompatibility(
        String xmlFormVersion,
        String resolvedXsdVersion,
        boolean fallback,
        String message
) {
    /**
     * Visszaadja, hogy a kompatibilitási fallback miatt csak olvasható mód szükséges-e.
     * @return {@code true}, ha az XML és az XSD verziója eltér
     */
    public boolean requiresReadOnly() {
        return fallback;
    }
}
