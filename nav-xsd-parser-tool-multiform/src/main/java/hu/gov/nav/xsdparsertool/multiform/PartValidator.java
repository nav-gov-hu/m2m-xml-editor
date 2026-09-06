package hu.gov.nav.xsdparsertool.multiform;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Compiles reusable adapter schemas and validates standalone A/M XML documents. */
public final class PartValidator {
    private final Schema mainSchema;
    private final Schema repeatingSchema;

    public PartValidator(Path originalXsd, MultiformDescriptor descriptor) {
        AdapterSchemaGenerator generator = new AdapterSchemaGenerator();
        this.mainSchema = compile(generator.generate(originalXsd, descriptor, PartKind.MAIN), originalXsd);
        this.repeatingSchema = compile(generator.generate(originalXsd, descriptor, PartKind.REPEATING), originalXsd);
    }

    public ValidationResult validate(Path xml, PartKind kind) {
        return validate(new StreamSource(xml.toFile()), kind);
    }

    ValidationResult validate(StreamSource source, PartKind kind) {
        Schema schema = kind == PartKind.MAIN ? mainSchema : repeatingSchema;
        Validator validator = schema.newValidator();
        List<ValidationIssue> issues = new ArrayList<>();
        validator.setErrorHandler(new CollectingErrorHandler(issues));
        try {
            validator.validate(source);
        } catch (SAXParseException e) {
            addIfMissing(issues, e);
        } catch (Exception e) {
            issues.add(new ValidationIssue(-1, -1, e.getMessage()));
        }
        return new ValidationResult(issues.isEmpty(), issues);
    }

    private Schema compile(String adapterXsd, Path originalXsd) {
        try {
            StreamSource source = new StreamSource(new StringReader(adapterXsd));
            source.setSystemId(originalXsd.toAbsolutePath().normalize().getParent().toUri().toString());
            return XmlSecurity.schemaFactory().newSchema(source);
        } catch (SAXException e) {
            throw new IllegalArgumentException("Az adapter XSD nem fordítható le.", e);
        }
    }

    private void addIfMissing(List<ValidationIssue> issues, SAXParseException e) {
        boolean exists = issues.stream().anyMatch(i -> i.line() == e.getLineNumber()
                && i.column() == e.getColumnNumber()
                && String.valueOf(i.message()).equals(e.getMessage()));
        if (!exists) {
            issues.add(new ValidationIssue(e.getLineNumber(), e.getColumnNumber(), e.getMessage()));
        }
    }

    private static final class CollectingErrorHandler implements org.xml.sax.ErrorHandler {
        private final List<ValidationIssue> issues;

        private CollectingErrorHandler(List<ValidationIssue> issues) {
            this.issues = issues;
        }

        @Override
        public void warning(SAXParseException exception) {
            issues.add(issue(exception));
        }

        @Override
        public void error(SAXParseException exception) {
            issues.add(issue(exception));
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            issues.add(issue(exception));
            throw exception;
        }

        private ValidationIssue issue(SAXParseException e) {
            return new ValidationIssue(e.getLineNumber(), e.getColumnNumber(), e.getMessage());
        }
    }
}
