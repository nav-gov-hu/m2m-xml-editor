package hu.gov.nav.xsdparsertool.multiform;

import javax.xml.namespace.QName;

/** XML entry detected inside the input ZIP. */
public record PackageEntry(String zipEntryName, QName rootElement, PartKind kind) {
}
