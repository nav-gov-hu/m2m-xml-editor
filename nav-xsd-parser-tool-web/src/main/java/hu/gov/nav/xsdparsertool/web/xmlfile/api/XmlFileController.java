package hu.gov.nav.xsdparsertool.web.xmlfile.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import hu.gov.nav.xsdparsertool.web.api.dto.ApiErrorResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.ArchiveXmlFileRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlSchemaVersionCompatibility;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.AutoRegisterServerFilesResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlSessionStateDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.LockReleaseRequestDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.LockReleaseRequestDecisionRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.LockReleaseRequestCreateRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.FileNameAvailabilityResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.CopyXmlFileRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.RegisterServerFileRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.OpenXmlFileRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.OpenXmlFileResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.RenewLockResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.ServerBrowserResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlFileDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlResolverInfoDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.UpdateXmlFileNoteRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.UpdateXmlFilePartnerRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlDiffPreviewResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlRevisionDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlSaveRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlSaveResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.ServerFileBrowserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.XmlFileService;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.XmlFileSessionService;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.XmlFileSaveService;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.XmlMutationGuard;

/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code XmlFileController} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController
@RequestMapping("/api/xml-files")
public class XmlFileController {
    private final XmlFileService xmlFileService;
    private final XmlFileSessionService xmlFileSessionService;
    private final ServerFileBrowserService serverFileBrowserService;
    private final XmlFileSaveService xmlFileSaveService;
    private final XmlMutationGuard mutationGuard;

    /**
     * Létrehozza a {@code XmlFileController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param xmlFileService a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param xmlFileSessionService a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param serverFileBrowserService a feldolgozásban részt vevő fájl vagy elérési út
     * @param xmlFileSaveService a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param mutationGuard a művelet bemeneti {@code mutationGuard} értéke
     */
    public XmlFileController(XmlFileService xmlFileService, XmlFileSessionService xmlFileSessionService, ServerFileBrowserService serverFileBrowserService, XmlFileSaveService xmlFileSaveService, XmlMutationGuard mutationGuard) {
        this.xmlFileService = xmlFileService;
        this.xmlFileSessionService = xmlFileSessionService;
        this.serverFileBrowserService = serverFileBrowserService;
        this.xmlFileSaveService = xmlFileSaveService;
        this.mutationGuard = mutationGuard;
    }

    /**
     * A {@code list} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param archived a művelet bemeneti {@code archived} értéke
     * @return a művelet eredményeként előállított elemek listája
     */
    @GetMapping
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public List<XmlFileDto> list(@RequestParam(name = "archived", defaultValue = "false") boolean archived) {
        return xmlFileService.list(archived);
    }

    /**
     * A {@code resolverInfo} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @return a feloldott vagy lekért érték
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @GetMapping("/{id}/resolver-info")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public XmlResolverInfoDto resolverInfo(@PathVariable Long id) throws IOException {
        xmlFileService.requireCurrentUserAccess(id);
        return xmlFileService.resolveInfo(id);
    }

    /**
     * A {@code checkFileName} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param fileName a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    @GetMapping("/check-filename")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public FileNameAvailabilityResponse checkFileName(@RequestParam("fileName") String fileName) {
        return xmlFileService.checkFileName(fileName);
    }

    /**
     * A {@code upload} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @param userNote a művelet felhasználói kontextusa vagy felhasználóneve
     * @param partnerId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public XmlFileDto upload(@RequestPart("file") MultipartFile file,
                             @RequestParam(name = "userNote", required = false) String userNote,
                             @RequestParam(name = "partnerId") Long partnerId) throws IOException {
        return xmlFileService.upload(file, userNote, partnerId);
    }

    /**
     * A {@code serverBrowser} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @GetMapping("/server-browser")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public ServerBrowserResponse serverBrowser() throws IOException {
        return serverFileBrowserService.listXmlFiles();
    }

    /**
     * A {@code registerServerFile} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping("/register-server-file")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public XmlFileDto registerServerFile(@RequestBody RegisterServerFileRequest request) throws IOException {
        return xmlFileService.registerServerFile(request.path(), request.userNote());
    }

    /**
     * A {@code autoRegisterServerFiles} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping("/auto-register-server-files")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public AutoRegisterServerFilesResponse autoRegisterServerFiles() throws IOException {
        return xmlFileService.autoRegisterServerFiles();
    }



    /**
     * A {@code updatePartner} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    @PutMapping("/{id}/partner")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public XmlFileDto updatePartner(@PathVariable Long id, @RequestBody UpdateXmlFilePartnerRequest request) {
        xmlFileService.requireCurrentUserAccess(id);
        return xmlFileService.updatePartner(id, request == null ? null : request.partnerId());
    }

    /**
     * A {@code saveAs} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping("/{id}/save-as")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public XmlFileDto saveAs(@PathVariable Long id, @RequestBody XmlSaveRequest request) throws IOException {
        XmlSaveRequest safeRequest = validateXmlSaveRequest(request);
        xmlFileService.requireCurrentUserAccess(id);
        ensureXmlFileMutable(id);
        return xmlFileService.saveAs(id, safeRequest);
    }

    /**
     * A {@code copyXmlFile} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping("/{id}/copy")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public XmlFileDto copyXmlFile(@PathVariable Long id, @RequestBody(required = false) CopyXmlFileRequest request) throws IOException {
        xmlFileService.requireCurrentUserAccess(id);
        return xmlFileService.copy(id, request);
    }

    /**
     * A {@code open} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    @PostMapping("/{id}/open")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public OpenXmlFileResponse open(@PathVariable Long id,
                                    @RequestBody(required = false) OpenXmlFileRequest request) {
        xmlFileService.requireCurrentUserAccess(id);
        XmlSchemaVersionCompatibility schemaCompatibility = xmlFileService.resolveSchemaVersionCompatibility(id);
        boolean readOnly = mutationGuard.isFinal(id)
                || schemaCompatibility.requiresReadOnly()
                || (request != null && request.isReadOnly());
        if (!xmlFileService.hasPartner(id) && !currentUserHasRole("ROLE_ADMIN")) {
            throw new AccessDeniedException("Az XML állományhoz nincs partner rendelve. A fájlt csak adminisztrátor nyithatja meg.");
        }
        if (!readOnly && !currentUserHasRole("ROLE_ADMIN") && !currentUserHasRole("ROLE_OPERATOR")) {
            throw new AccessDeniedException("Nincs jogosultságod az XML állomány szerkesztésre megnyitásához.");
        }
        return xmlFileSessionService.open(
                id,
                readOnly,
                schemaCompatibility.requiresReadOnly() ? schemaCompatibility.message() : null,
                schemaCompatibility.fallback(),
                schemaCompatibility.xmlFormVersion(),
                schemaCompatibility.resolvedXsdVersion());
    }

    /**
     * A {@code renewLock} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @PostMapping("/{id}/lock/renew")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public RenewLockResponse renewLock(@PathVariable Long id) {
        xmlFileService.requireCurrentUserAccess(id);
        return xmlFileSessionService.renew(id);
    }

    /**
     * A {@code close} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @PostMapping("/{id}/close")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public Map<String, Object> close(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> request) {
        xmlFileService.requireCurrentUserAccess(id);
        xmlFileSessionService.close(id, request == null ? null : request.get("reason"), request == null ? null : request.get("sessionId"));
        return Map.of("status", "CLOSED");
    }

    /**
     * A {@code sessionState} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param sessionId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @GetMapping("/{id}/session-state")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public XmlSessionStateDto sessionState(@PathVariable Long id, @RequestParam("sessionId") String sessionId) {
        xmlFileService.requireCurrentUserAccess(id);
        return xmlFileSessionService.sessionState(id, sessionId);
    }

    /**
     * A {@code requestLockRelease} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    @PostMapping("/{id}/lock-release-requests")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public LockReleaseRequestDto requestLockRelease(@PathVariable Long id, @RequestBody(required = false) LockReleaseRequestCreateRequest request) {
        xmlFileService.requireCurrentUserAccess(id);
        return xmlFileSessionService.requestLockRelease(id, validateOptionalDecisionMessage(request == null ? null : request.message()));
    }

    /**
     * A {@code pendingLockReleaseRequests} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    @GetMapping("/lock-release-requests/pending")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public List<LockReleaseRequestDto> pendingLockReleaseRequests() {
        return xmlFileSessionService.pendingLockReleaseRequestsForCurrentSession();
    }

    /**
     * A {@code myLockReleaseRequests} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    @GetMapping("/lock-release-requests/mine")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public List<LockReleaseRequestDto> myLockReleaseRequests() {
        return xmlFileSessionService.myLockReleaseRequests();
    }

    /**
     * A {@code acceptLockReleaseRequest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    @PostMapping("/lock-release-requests/{requestId}/accept")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public LockReleaseRequestDto acceptLockReleaseRequest(@PathVariable Long requestId, @RequestBody(required = false) LockReleaseRequestDecisionRequest request) {
        return xmlFileSessionService.acceptLockReleaseRequest(requestId, validateOptionalDecisionMessage(request == null ? null : request.message()));
    }

    /**
     * A {@code rejectLockReleaseRequest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    @PostMapping("/lock-release-requests/{requestId}/reject")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public LockReleaseRequestDto rejectLockReleaseRequest(@PathVariable Long requestId, @RequestBody(required = false) LockReleaseRequestDecisionRequest request) {
        return xmlFileSessionService.rejectLockReleaseRequest(requestId, validateOptionalDecisionMessage(request == null ? null : request.message()));
    }

    /**
     * A {@code forceCloseLockReleaseRequest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    @PostMapping("/lock-release-requests/{requestId}/force-close")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.ADMIN_ONLY)
    public LockReleaseRequestDto forceCloseLockReleaseRequest(@PathVariable Long requestId, @RequestBody(required = false) LockReleaseRequestDecisionRequest request) {
        return xmlFileSessionService.forceCloseLockReleaseRequest(requestId, validateOptionalDecisionMessage(request == null ? null : request.message()));
    }

    /**
     * A {@code forceReleaseLock} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @PostMapping("/{id}/lock/force-release")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.ADMIN_ONLY)
    public Map<String, Object> forceReleaseLock(@PathVariable Long id) {
        xmlFileSessionService.forceReleaseLock(id);
        return Map.of("status", "RELEASED");
    }



    /**
     * A {@code diffPreview} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping("/{id}/diff-preview")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public XmlDiffPreviewResponse diffPreview(@PathVariable Long id,
                                              @RequestBody(required = false) XmlSaveRequest request) throws IOException {
        if (request != null && request.xmlContent() != null && request.xmlContent().length() > 16 * 1024 * 1024) {
            throw new IllegalArgumentException("A diff előnézethez kapott XML tartalom túl nagy.");
        }
        XmlSaveRequest safeRequest = validateXmlSaveRequest(request);
        xmlFileService.requireCurrentUserAccess(id);
        return xmlFileSaveService.diffPreview(id, safeRequest);
    }

    /**
     * A {@code saveNewVersion} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping("/{id}/save-new-version")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public XmlSaveResponse saveNewVersion(@PathVariable String id,
                                          @RequestBody(required = false) XmlSaveRequest request) throws IOException {
        if (id == null || !id.matches("[1-9][0-9]{0,17}")) {
            throw new IllegalArgumentException("Érvénytelen XML fájl azonosító.");
        }
        final Long safeId;
        try {
            safeId = Long.valueOf(id);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Érvénytelen XML fájl azonosító.", ex);
        }
        XmlSaveRequest safeRequest = validateXmlSaveRequest(request);
        xmlFileService.requireCurrentUserAccess(safeId);
        ensureXmlFileMutable(safeId);
        return xmlFileSaveService.saveNewVersion(safeId, safeRequest);
    }

    /**
     * A {@code overwrite} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping("/{id}/overwrite")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public XmlSaveResponse overwrite(@PathVariable String id,
                                     @RequestBody(required = false) XmlSaveRequest request) throws IOException {
        Long safeId = parsePositiveId(id);
        XmlSaveRequest safeRequest = validateXmlSaveRequest(request);
        xmlFileService.requireCurrentUserAccess(safeId);
        ensureXmlFileMutable(safeId);
        return xmlFileSaveService.overwrite(safeId, safeRequest);
    }


    /**
     * A {@code parsePositiveId} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param raw a művelet bemeneti {@code raw} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static Long parsePositiveId(String raw) {
        if (raw == null || !raw.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("Érvénytelen XML fájl azonosító.");
        }
        try { return Long.valueOf(raw); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Érvénytelen XML fájl azonosító."); }
    }

    /**
     * A {@code validateXmlSaveRequest} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    private static XmlSaveRequest validateXmlSaveRequest(XmlSaveRequest request) {
        if (request == null) return null;
        String sessionId = safeToken(request.sessionId(), 128, "Érvénytelen munkamenet-azonosító.");
        String userNote = safeText(request.userNote(), 4000, "Érvénytelen megjegyzés.");
        String newFileName = safeFileName(request.newFileName());
        String xmlContent = request.xmlContent();
        if (xmlContent != null) {
            if (xmlContent.indexOf('\0') >= 0) throw new IllegalArgumentException("Érvénytelen XML tartalom.");
            if (xmlContent.length() > 157_286_400) throw new IllegalArgumentException("Az XML tartalom túl nagy.");
        }
        return new XmlSaveRequest(xmlContent, sessionId, request.runXsdValidation(), request.allowInvalidXml(), userNote, newFileName);
    }

    /**
     * A {@code safeToken} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param raw a művelet bemeneti {@code raw} értéke
     * @param max a művelet bemeneti {@code max} értéke
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String safeToken(String raw, int max, String message) {
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
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String safeText(String raw, int max, String message) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() > max || value.chars().anyMatch(ch -> ch < 0x20 && ch != '\t' && ch != '\n' && ch != '\r')) throw new IllegalArgumentException(message);
        return value;
    }

    /**
     * A {@code safeFileName} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param raw a művelet bemeneti {@code raw} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String safeFileName(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() > 255 || value.contains("/") || value.contains("\\") || value.contains("..") || value.indexOf('\0') >= 0)
            throw new IllegalArgumentException("Érvénytelen fájlnév.");
        return value;
    }

    /**
     * A {@code ensureXmlFileMutable} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     */
    private void ensureXmlFileMutable(Long xmlFileId) {
        mutationGuard.requireMutable(xmlFileId);
    }

    /**
     * A {@code revisions} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    @GetMapping("/{id}/revisions")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public List<XmlRevisionDto> revisions(@PathVariable Long id) {
        xmlFileService.requireCurrentUserAccess(id);
        return xmlFileSaveService.revisions(id);
    }

    /**
     * A {@code revision} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param revisionId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @GetMapping("/revisions/{revisionId}")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public XmlRevisionDto revision(@PathVariable Long revisionId) {
        return xmlFileSaveService.revision(revisionId);
    }

    /**
     * A {@code updateNote} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    @PostMapping("/{id}/note")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public XmlFileDto updateNote(@PathVariable Long id,
                                 @RequestBody(required = false) UpdateXmlFileNoteRequest request) {
        xmlFileService.requireCurrentUserAccess(id);
        return xmlFileService.updateNote(id, request == null ? null : request.userNote());
    }

    /**
     * A {@code archive} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public XmlFileDto archive(@PathVariable Long id,
                              @RequestBody(required = false) ArchiveXmlFileRequest request) {
        xmlFileService.requireCurrentUserAccess(id);
        return xmlFileService.archive(id, request == null ? null : request.reason());
    }

    /**
     * A {@code download} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     * @throws MalformedURLException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @GetMapping("/{id}/download")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public ResponseEntity<Resource> download(@PathVariable Long id) throws MalformedURLException {
        xmlFileService.requireCurrentUserAccess(id);
        Path path = xmlFileService.downloadPath(id);
        Resource resource = new UrlResource(path.toUri());
        String filename = path.getFileName() == null ? "xml-file.xml" : path.getFileName().toString();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(resource);
    }

    /**
     * A {@code permanentlyDelete} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a feldolgozás során felépített kulcs-érték leképezés
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.ADMIN_ONLY)
    public Map<String, Object> permanentlyDelete(@PathVariable String id,
                                                  @RequestBody(required = false) ArchiveXmlFileRequest request) throws IOException {
        if (id == null || !id.matches("[1-9][0-9]{0,17}")) {
            throw new IllegalArgumentException("Érvénytelen XML fájl azonosító.");
        }
        final Long validatedId;
        try {
            validatedId = Long.valueOf(id);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Érvénytelen XML fájl azonosító.", ex);
        }
        String reason = request == null ? null : request.reason();
        if (reason != null && reason.length() > 1000) {
            throw new IllegalArgumentException("A törlés indoklása legfeljebb 1000 karakter lehet.");
        }
        xmlFileService.permanentlyDelete(validatedId, reason);
        return Map.of("status", "DELETED", "id", validatedId);
    }

    /**
     * A {@code physicalArchive} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping("/{id}/physical-archive")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.FILE_DELETE)
    public XmlFileDto physicalArchive(@PathVariable Long id,
                                      @RequestBody(required = false) ArchiveXmlFileRequest request) throws IOException {
        xmlFileService.requireCurrentUserAccess(id);
        return xmlFileService.physicalArchive(id, request == null ? null : request.reason());
    }


    /**
     * A {@code validateOptionalDecisionMessage} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String validateOptionalDecisionMessage(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > 1000 || normalized.chars().anyMatch(ch -> ch < 0x20 && ch != '\t')) {
            throw new IllegalArgumentException("Az üzenet legfeljebb 1000 karakteres, vezérlőkarakter nélküli szöveg lehet.");
        }
        return normalized;
    }

    /**
     * A {@code currentUserHasRole} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param role a művelet bemeneti {@code role} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean currentUserHasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    /**
     * A {@code duplicateFileName} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler(XmlFileService.DuplicateXmlFileNameException.class)
    public ResponseEntity<ApiErrorResponse> duplicateFileName(XmlFileService.DuplicateXmlFileNameException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(ex.getMessage()));
    }

    /**
     * A {@code fileLocked} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler(XmlFileSessionService.FileLockedByOtherUserException.class)
    public ResponseEntity<ApiErrorResponse> fileLocked(XmlFileSessionService.FileLockedByOtherUserException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(ex.getMessage() + " Zárolta: " + ex.getLockedBy() + ", lejárat: " + ex.getLockExpiresAt()));
    }

    /**
     * A {@code badRequest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> badRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(ex.getMessage()));
    }

    /**
     * A {@code ioError} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiErrorResponse> ioError(IOException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(ex.getMessage()));
    }
}
