package hu.gov.nav.xsdparsertool.core.model.processing;

import hu.gov.nav.xsdparsertool.core.model.validation.ValidationIssue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
/**
 * Egy exportművelet összesített eredménye.
 *
 * <p>Jelzi a művelet sikerességét, a létrehozott kimeneti fájlt és az export során
 * keletkezett validációs vagy feldolgozási problémákat.</p>
 */
public class ExportResult {
    private boolean success;
    private Path outputFile;
    private List<ValidationIssue> issues = new ArrayList<>();
/**
 * Visszaadja a következő modellértéket: az exportművelet sikerességi jelzője.
 *
 * @return az exportművelet sikerességi jelzője
 */
public boolean isSuccess() {
        return success;
    }
/**
 * Beállítja a következő modellértéket: az exportművelet sikerességi jelzője.
 *
 * @param success az exportművelet sikerességi jelzője
 */
public void setSuccess(boolean success) {
        this.success = success;
    }
/**
 * Visszaadja a következő modellértéket: a létrehozott exportfájl útvonala.
 *
 * @return a létrehozott exportfájl útvonala
 */
public Path getOutputFile() {
        return outputFile;
    }
/**
 * Beállítja a következő modellértéket: a létrehozott exportfájl útvonala.
 *
 * @param outputFile a létrehozott exportfájl útvonala
 */
public void setOutputFile(Path outputFile) {
        this.outputFile = outputFile;
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
