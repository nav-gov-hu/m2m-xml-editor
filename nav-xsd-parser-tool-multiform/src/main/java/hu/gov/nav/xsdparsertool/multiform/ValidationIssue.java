package hu.gov.nav.xsdparsertool.multiform;

/** One XSD validation problem with optional source position. */
public record ValidationIssue(int line, int column, String message) {
}
