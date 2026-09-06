package hu.gov.nav.xsdparsertool.core.model.processing;

import hu.gov.nav.xsdparsertool.core.model.validation.ValidationIssue;

import java.util.ArrayList;
import java.util.List;
/**
 * Egy validációs futás összesített eredménye.
 *
 * <p>A {@code valid} jelző a teljes dokumentum érvényességét mutatja, az {@code issues}
 * lista pedig a részletes hibákat, figyelmeztetéseket és információs üzeneteket tartalmazza.</p>
 */
public class ValidationResult {
    private boolean valid;
    private List<ValidationIssue> issues = new ArrayList<>();
/**
 * Visszaadja a következő modellértéket: a teljes validáció sikerességi jelzője.
 *
 * @return a teljes validáció sikerességi jelzője
 */
public boolean isValid() {
        return valid;
    }
/**
 * Beállítja a következő modellértéket: a teljes validáció sikerességi jelzője.
 *
 * @param valid a teljes validáció sikerességi jelzője
 */
public void setValid(boolean valid) {
        this.valid = valid;
    }
/**
 * Visszaadja a következő modellértéket: a művelethez tartozó validációs problémák listája.
 *
 * @return a művelethez tartozó validációs problémák listája
 */
public List<ValidationIssue> getIssues() {
        return issues;
    }
/**
 * Beállítja a következő modellértéket: a művelethez tartozó validációs problémák listája.
 *
 * @param issues a művelethez tartozó validációs problémák listája
 */
public void setIssues(List<ValidationIssue> issues) {
        this.issues = issues;
    }
}
