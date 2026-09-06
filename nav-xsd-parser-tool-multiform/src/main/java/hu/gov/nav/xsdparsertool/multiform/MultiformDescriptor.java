package hu.gov.nav.xsdparsertool.multiform;

import javax.xml.namespace.QName;

/** Description of a two-part multiform document discovered from the original XSD. */
public record MultiformDescriptor(
        QName documentElement,
        QName documentType,
        String targetNamespace,
        PartDescriptor mainPart,
        PartDescriptor repeatingPart) {
}
