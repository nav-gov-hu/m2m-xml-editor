package hu.gov.nav.xsdparsertool.multiform;

import javax.xml.namespace.QName;

/** XSD metadata needed to create and validate one standalone form part. */
public record PartDescriptor(
        PartKind kind,
        QName elementName,
        QName typeName,
        int minOccurs,
        String maxOccurs) {

    public boolean unbounded() {
        return "unbounded".equals(maxOccurs);
    }
}
