package hu.gov.nav.xsdparsertool.core.model.form;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * Az űrlapdefinícióhoz tartozó konkrét XML-példány értékeit tárolja.
 *
 * <p>A közvetlen mezőértékek mezőazonosító szerint, az ismétlődő sorok példányai
 * pedig sorazonosító szerint érhetők el. Ismétlődő struktúráknál a sorpéldányok
 * őrzik a konkrét, indexelt XML-útvonalat.</p>
 */
public class FormData {
    private Map<String, FormValue> valuesByFieldId = new LinkedHashMap<>();
    private Map<String, List<FormRowInstance>> rowInstancesByRowId = new LinkedHashMap<>();
/**
 * Visszaadja a következő modellértéket: a mezőazonosító szerint elérhető konkrét űrlapértékek térképe.
 *
 * @return a mezőazonosító szerint elérhető konkrét űrlapértékek térképe
 */
public Map<String, FormValue> getValuesByFieldId() {
        return valuesByFieldId;
    }
/**
 * Beállítja a következő modellértéket: a mezőazonosító szerint elérhető konkrét űrlapértékek térképe.
 *
 * @param Map<String a mezőazonosító szerint elérhető konkrét űrlapértékek térképe
 */
public void setValuesByFieldId(Map<String, FormValue> valuesByFieldId) {
        this.valuesByFieldId = valuesByFieldId;
    }
/**
 * Visszaadja a következő modellértéket: az ismétlődő sorazonosítóhoz tartozó konkrét sorpéldányok térképe.
 *
 * @return az ismétlődő sorazonosítóhoz tartozó konkrét sorpéldányok térképe
 */
public Map<String, List<FormRowInstance>> getRowInstancesByRowId() {
        return rowInstancesByRowId;
    }
/**
 * Beállítja a következő modellértéket: az ismétlődő sorazonosítóhoz tartozó konkrét sorpéldányok térképe.
 *
 * @param Map<String az ismétlődő sorazonosítóhoz tartozó konkrét sorpéldányok térképe
 */
public void setRowInstancesByRowId(Map<String, List<FormRowInstance>> rowInstancesByRowId) {
        this.rowInstancesByRowId = rowInstancesByRowId;
    }
/**
 * Visszaadja a következő modellértéket: a kapcsolódó modellérték.
 *
 * @return a kapcsolódó modellérték
 */
public List<FormRowInstance> getOrCreateRowInstances(String rowId) {
        return rowInstancesByRowId.computeIfAbsent(rowId, key -> new ArrayList<>());
    }
}
