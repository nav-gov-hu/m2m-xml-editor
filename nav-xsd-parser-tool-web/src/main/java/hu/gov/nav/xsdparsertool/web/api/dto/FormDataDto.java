package hu.gov.nav.xsdparsertool.web.api.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * Az XML-ből felépített űrlapadatokat és ismétlődő sorpéldányokat összefogó DTO.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class FormDataDto {
    @Schema(description = "HU: valuesByFieldId mező. EN: valuesByFieldId field.")
    private Map<String, FormValueDto> valuesByFieldId = new LinkedHashMap<>();
    @Schema(description = "HU: rowInstancesByRowId mező. EN: rowInstancesByRowId field.")
    private Map<String, List<FormRowInstanceDto>> rowInstancesByRowId = new LinkedHashMap<>();
/**
 * Visszaadja a {@code valuesByFieldId} mező aktuális értékét.
 * @return a {@code valuesByFieldId} mező értéke
 */

    public Map<String, FormValueDto> getValuesByFieldId() { return valuesByFieldId; }
/**
 * Beállítja a {@code valuesByFieldId} mező értékét.
 * @param valuesByFieldId a beállítandó új érték
 */
    public void setValuesByFieldId(Map<String, FormValueDto> valuesByFieldId) { this.valuesByFieldId = valuesByFieldId; }
/**
 * Visszaadja a {@code rowInstancesByRowId} mező aktuális értékét.
 * @return a {@code rowInstancesByRowId} mező értéke
 */
    public Map<String, List<FormRowInstanceDto>> getRowInstancesByRowId() { return rowInstancesByRowId; }
/**
 * Beállítja a {@code rowInstancesByRowId} mező értékét.
 * @param rowInstancesByRowId a beállítandó új érték
 */
    public void setRowInstancesByRowId(Map<String, List<FormRowInstanceDto>> rowInstancesByRowId) { this.rowInstancesByRowId = rowInstancesByRowId; }
}
