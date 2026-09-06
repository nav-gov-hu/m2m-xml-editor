package hu.gov.nav.xsdparsertool.web.xmlfile.api;

import hu.gov.nav.xsdparsertool.web.xmlfile.service.LargeXmlMultiformPageService;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.LargeXmlFragmentSaveService;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.XmlFileService;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.LargeXmlFragmentSaveRequest;
import hu.gov.nav.xsdparsertool.web.processing.dto.ProcessingJobDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code LargeXmlMultiformController} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController
@RequestMapping("/api/xml-files/{xmlFileId}/large-multiform")
public class LargeXmlMultiformController {
    private final LargeXmlMultiformPageService service;
    private final LargeXmlFragmentSaveService fragmentSaveService;
    private final XmlFileService xmlFileService;

    /**
     * Létrehozza a {@code LargeXmlMultiformController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param service a művelet bemeneti {@code service} értéke
     * @param fragmentSaveService a művelet bemeneti {@code fragmentSaveService} értéke
     * @param xmlFileService a feldolgozandó XML-hez tartozó adat vagy tartalom
     */
    public LargeXmlMultiformController(LargeXmlMultiformPageService service, LargeXmlFragmentSaveService fragmentSaveService, XmlFileService xmlFileService) {
        this.service = service;
        this.fragmentSaveService = fragmentSaveService;
        this.xmlFileService = xmlFileService;
    }

    /**
     * A {@code configurationStatus} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @return a művelet feldolgozási eredménye
     */
    @GetMapping("/configuration-status")
    public LargeXmlMultiformPageService.ConfigurationStatus configurationStatus(
            @PathVariable Long xmlFileId,
            @RequestParam String formName) {
        xmlFileService.requireCurrentUserAccess(xmlFileId);
        return service.configurationStatus(xmlFileId, formName);
    }

    /**
     * A {@code rows} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @param page a lapozási vagy mennyiségi korlátot meghatározó érték
     * @param size a lapozási vagy mennyiségi korlátot meghatározó érték
     * @param q a művelet bemeneti {@code q} értéke
     * @return a művelet feldolgozási eredménye
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @GetMapping("/rows")
    public LargeXmlMultiformPageService.PageResult rows(
            @PathVariable Long xmlFileId,
            @RequestParam String formName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "") String q) throws Exception {
        xmlFileService.requireCurrentUserAccess(xmlFileId);
        String safeFormName = requiredToken(formName, 128, "Érvénytelen űrlapnév.");
        if (page < 0 || page > 1_000_000) throw new IllegalArgumentException("Érvénytelen oldalszám.");
        if (size < 1 || size > 500) throw new IllegalArgumentException("Érvénytelen oldalméret.");
        String safeQuery = q == null ? "" : q.trim();
        if (safeQuery.length() > 512) throw new IllegalArgumentException("A keresőkifejezés túl hosszú.");
        return service.page(xmlFileId, safeFormName, page, size, safeQuery);
    }
    /**
     * A {@code saveFragment} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping("/save-fragment")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public ProcessingJobDto saveFragment(@PathVariable Long xmlFileId,
                                         @RequestBody LargeXmlFragmentSaveRequest request) throws Exception {
        xmlFileService.requireCurrentUserAccess(xmlFileId);
        if (request == null) throw new IllegalArgumentException("A mentési kérés hiányzik.");
        String formName = optionalToken(request.formName(), 128, "Érvénytelen űrlapnév.");
        Long occurrenceIndex = request.occurrenceIndex();
        if (occurrenceIndex != null && occurrenceIndex < 0) throw new IllegalArgumentException("Érvénytelen előfordulás-index.");
        String sessionId = optionalToken(request.sessionId(), 128, "Érvénytelen munkamenet-azonosító.");
        Long sourceFileSize = request.sourceFileSize();
        Long sourceLastModified = request.sourceLastModified();
        if (sourceFileSize != null && sourceFileSize < 0) throw new IllegalArgumentException("Érvénytelen forrásfájl-méret.");
        if (sourceLastModified != null && sourceLastModified < 0) throw new IllegalArgumentException("Érvénytelen módosítási idő.");
        String xmlFragment = request.xmlFragment();
        if (xmlFragment != null && xmlFragment.indexOf('\0') >= 0) throw new IllegalArgumentException("Érvénytelen XML fragmentum.");
        String userNote = safeText(request.userNote(), 4000);
        LargeXmlFragmentSaveRequest safe = new LargeXmlFragmentSaveRequest(formName, occurrenceIndex, xmlFragment, sessionId, sourceFileSize, sourceLastModified, userNote);
        return fragmentSaveService.start(xmlFileId, safe);
    }

    /**
     * A {@code requiredToken} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param raw a művelet bemeneti {@code raw} értéke
     * @param max a művelet bemeneti {@code max} értéke
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String requiredToken(String raw, int max, String message) {
        String value = optionalToken(raw, max, message);
        if (value == null) throw new IllegalArgumentException(message);
        return value;
    }

    /**
     * A {@code optionalToken} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param raw a művelet bemeneti {@code raw} értéke
     * @param max a művelet bemeneti {@code max} értéke
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String optionalToken(String raw, int max, String message) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() > max || !value.matches("[A-Za-z0-9._:@/+\\-]+")) throw new IllegalArgumentException(message);
        return value;
    }

    /**
     * A {@code safeText} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param raw a művelet bemeneti {@code raw} értéke
     * @param max a művelet bemeneti {@code max} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String safeText(String raw, int max) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() > max || value.chars().anyMatch(ch -> ch < 0x20 && ch != '\t' && ch != '\n' && ch != '\r')) throw new IllegalArgumentException("Érvénytelen megjegyzés.");
        return value;
    }
}
