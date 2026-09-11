package hu.gov.nav.xsdparsertool.schemaregistry.model;

import java.util.List;

/**
 * Egy generálható űrlaptípus és a hozzá tartozó elérhető űrlapverziók.
 *
 * @param documentType az űrlap technikai azonosítója
 * @param versions az elérhető verziók rendezett listája
 */
public record SchemaDocumentOption(String documentType, List<String> versions) {
}
