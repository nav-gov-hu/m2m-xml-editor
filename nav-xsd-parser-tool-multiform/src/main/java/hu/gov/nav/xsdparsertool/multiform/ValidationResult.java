package hu.gov.nav.xsdparsertool.multiform;

import java.util.List;

/** Immutable XSD validation result. */
public record ValidationResult(boolean valid, List<ValidationIssue> issues) {
    public ValidationResult {
        issues = List.copyOf(issues);
    }

    public static ValidationResult validResult() {
        return new ValidationResult(true, List.of());
    }
}
