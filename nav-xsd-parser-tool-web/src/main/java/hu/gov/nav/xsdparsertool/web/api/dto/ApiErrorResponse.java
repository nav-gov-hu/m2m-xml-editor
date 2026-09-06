package hu.gov.nav.xsdparsertool.web.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * A webes API-k egységes, kliens számára sorosítható hiba-válaszreprezentációja.
 * Az osztály a dto csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class ApiErrorResponse {
    @Schema(description = "HU: error mező. EN: error field.")
    private String error;
    /**
     * Létrehozza a {@code ApiErrorResponse} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     */
    public ApiErrorResponse() {}
    /**
     * Létrehozza a {@code ApiErrorResponse} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param error a művelet bemeneti {@code error} értéke
     */
    public ApiErrorResponse(String error) { this.error = error; }
/**
 * Visszaadja a {@code error} mező aktuális értékét.
 * @return a {@code error} mező értéke
 */
    public String getError() { return error; }
/**
 * Beállítja a {@code error} mező értékét.
 * @param error a beállítandó új érték
 */
    public void setError(String error) { this.error = error; }
}
