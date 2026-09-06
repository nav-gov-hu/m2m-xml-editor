package hu.gov.nav.xsdparsertool.core.model.validation;

import hu.gov.nav.xsdparsertool.core.enums.Severity;
/**
 * Egy validációs vagy feldolgozási probléma strukturált leírása.
 *
 * <p>A probléma kódját, lehetőség szerint teljes XML-útvonalát, ember számára
 * olvasható üzenetét és súlyosságát tartalmazza.</p>
 */
public class ValidationIssue {
    private String code;
    private String path;
    private String message;
    private Severity severity;

    /**
     * Privát konstruktor; a ValidationIssue segédosztály példányosítását megakadályozza.
     */
    public ValidationIssue() {
    }

    /**
     * Privát konstruktor; a ValidationIssue segédosztály példányosítását megakadályozza.
     */
    public ValidationIssue(String code, String path, String message, Severity severity) {
        this.code = code;
        this.path = path;
        this.message = message;
        this.severity = severity;
    }
/**
 * Visszaadja a következő modellértéket: a validációs probléma technikai kódja.
 *
 * @return a validációs probléma technikai kódja
 */
public String getCode() {
        return code;
    }
/**
 * Beállítja a következő modellértéket: a validációs probléma technikai kódja.
 *
 * @param code a validációs probléma technikai kódja
 */
public void setCode(String code) {
        this.code = code;
    }
/**
 * Visszaadja a következő modellértéket: a csomópont vagy probléma teljes XML-útvonala.
 *
 * @return a csomópont vagy probléma teljes XML-útvonala
 */
public String getPath() {
        return path;
    }
/**
 * Beállítja a következő modellértéket: a csomópont vagy probléma teljes XML-útvonala.
 *
 * @param path a csomópont vagy probléma teljes XML-útvonala
 */
public void setPath(String path) {
        this.path = path;
    }
/**
 * Visszaadja a következő modellértéket: a probléma ember számára olvasható leírása.
 *
 * @return a probléma ember számára olvasható leírása
 */
public String getMessage() {
        return message;
    }
/**
 * Beállítja a következő modellértéket: a probléma ember számára olvasható leírása.
 *
 * @param message a probléma ember számára olvasható leírása
 */
public void setMessage(String message) {
        this.message = message;
    }
/**
 * Visszaadja a következő modellértéket: a probléma súlyossági szintje.
 *
 * @return a probléma súlyossági szintje
 */
public Severity getSeverity() {
        return severity;
    }
/**
 * Beállítja a következő modellértéket: a probléma súlyossági szintje.
 *
 * @param severity a probléma súlyossági szintje
 */
public void setSeverity(Severity severity) {
        this.severity = severity;
    }
}
