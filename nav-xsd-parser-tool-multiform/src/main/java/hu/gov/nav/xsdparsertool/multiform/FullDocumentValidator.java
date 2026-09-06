package hu.gov.nav.xsdparsertool.multiform;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Validates the assembled XML against the original complete XSD. */
public final class FullDocumentValidator {
    private final Schema schema;

    public FullDocumentValidator(Path xsd) {
        try {
            this.schema = XmlSecurity.schemaFactory().newSchema(xsd.toFile());
        } catch (SAXException e) {
            throw new IllegalArgumentException("Az eredeti XSD nem fordítható le: " + xsd, e);
        }
    }

    public ValidationResult validate(Path xml) {
        Validator validator = schema.newValidator();
        List<ValidationIssue> issues = new ArrayList<>();
        validator.setErrorHandler(new org.xml.sax.ErrorHandler() {
            @Override public void warning(SAXParseException e) { issues.add(issue(e)); }
            @Override public void error(SAXParseException e) { issues.add(issue(e)); }
            @Override public void fatalError(SAXParseException e) throws SAXException { issues.add(issue(e)); throw e; }
            private ValidationIssue issue(SAXParseException e) { return new ValidationIssue(e.getLineNumber(), e.getColumnNumber(), e.getMessage()); }
        });
        try {
            validator.validate(new javax.xml.transform.stream.StreamSource(xml.toFile()));
        } catch (SAXParseException e) {
            if (issues.isEmpty()) {
                issues.add(new ValidationIssue(e.getLineNumber(), e.getColumnNumber(), e.getMessage()));
            }
        } catch (Exception e) {
            issues.add(new ValidationIssue(-1, -1, e.getMessage()));
        }
        return new ValidationResult(issues.isEmpty(), issues);
    }
}
