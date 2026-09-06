package hu.gov.nav.xsdparsertool.multiform;

import java.nio.file.Path;

/** Generates a small no-namespace adapter XSD that exposes a local part as a standalone root element. */
public final class AdapterSchemaGenerator {

    public String generate(Path originalXsd, MultiformDescriptor descriptor, PartKind kind) {
        PartDescriptor part = kind == PartKind.MAIN ? descriptor.mainPart() : descriptor.repeatingPart();
        String schemaUri = originalXsd.toAbsolutePath().normalize().toUri().toString();
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                           xmlns:tns="%s">
                    <xs:import namespace="%s" schemaLocation="%s"/>
                    <xs:element name="%s" type="tns:%s"/>
                </xs:schema>
                """.formatted(
                xml(descriptor.targetNamespace()),
                xml(descriptor.targetNamespace()),
                xml(schemaUri),
                xml(part.elementName().getLocalPart()),
                xml(part.typeName().getLocalPart()));
    }

    private String xml(String value) {
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
