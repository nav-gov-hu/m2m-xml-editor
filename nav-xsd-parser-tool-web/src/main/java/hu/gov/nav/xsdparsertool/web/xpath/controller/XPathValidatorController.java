package hu.gov.nav.xsdparsertool.web.xpath.controller;

import hu.gov.nav.xsdparsertool.web.xpath.config.XPathValidatorProperties;
import hu.gov.nav.xsdparsertool.web.xpath.dto.*;
import hu.gov.nav.xsdparsertool.web.xpath.model.CreateResultMode;
import hu.gov.nav.xsdparsertool.web.xpath.model.ValidatorStatus;
import hu.gov.nav.xsdparsertool.web.xpath.service.XPathValidationOrchestratorService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
/**
 * A XPath-validáció indítási, lekérdezési, listázási és kapcsolódó hibakezelési REST végpontjait biztosító controller.
 * Az osztály a controller csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: @RestController.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: @RestController.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


@Tag(name = "XPathValidator", description = "XPath/XSLT validációs REST végpontok. / XPath/XSLT validation REST endpoints.")
@RestController
@RequestMapping("/api/xpath-validator")
@PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
public class XPathValidatorController {
    private static final Logger LOGGER = LoggerFactory.getLogger(XPathValidatorController.class);

    private final XPathValidationOrchestratorService service;
    private final XPathValidatorProperties properties;

    /**
     * Létrehozza a {@code XPathValidatorController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param service a művelet bemeneti {@code service} értéke
     * @param properties a művelethez szükséges konfigurációs adatok
     */
    public XPathValidatorController(XPathValidationOrchestratorService service, XPathValidatorProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    
    /**
     * A {@code config} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @return a művelet feldolgozási eredménye
     */
    @Operation(summary = "HU: config REST művelet. EN: config REST operation.", description = "HU: Dokumentált REST végpont. EN: Documented REST endpoint.")

    @ApiResponses({

        @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

        @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

        @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@GetMapping("/config")
/**
 * Visszaadja az adott controller felületének működéséhez szükséges futásidejű konfigurációt.
 * @return a metódus által előállított eredmény
 */
    public XPathValidatorConfigDto config() {
        LOGGER.info("XPATH validator config requested.");
        return new XPathValidatorConfigDto(properties.getDefaultPageSize(), properties.getDefaultAutoRefreshSeconds());
    }

    
    /**
     * A {@code createRequest} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @param createResult a művelet bemeneti {@code createResult} értéke
     * @return a művelet feldolgozási eredménye
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Operation(summary = "XPath validációs kérés indítása / Create XPath validation request", description = "HU: Dokumentált REST művelet. EN: Documented REST operation.")

    @ApiResponses({

            @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

            @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

            @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@PostMapping(path = "/requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<XPathValidationRequestStatusDto> createRequest(HttpServletRequest request,@Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @RequestParam(name = "file") MultipartFile file, @Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @RequestParam(name = "createResult", required = false, defaultValue = "ASYNC") String createResult) throws Exception {
        LOGGER.info("Incoming XPATH validation REST call.");

        CreateResultMode mode = CreateResultMode.valueOf(createResult.trim().toUpperCase(java.util.Locale.ROOT));
        XPathValidationRequestStatusDto status = service.submit(file, mode, currentSessionId());
        HttpStatus httpStatus = (mode == CreateResultMode.SYNC && !status.timedOut() && status.validatorStatus() == ValidatorStatus.FINISHED)
                ? HttpStatus.OK : HttpStatus.ACCEPTED;
        LOGGER.info("XPATH validation REST call finished. requestId={}, validatorStatus={}, resultStatus={}, httpStatus={}",
                status.requestId(), status.validatorStatus(), status.resultStatus(), httpStatus.value());
        return ResponseEntity.status(httpStatus).body(status);
    }

    
    /**
     * A {@code list} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param limit a lapozási vagy mennyiségi korlátot meghatározó érték
     * @param query a művelet bemeneti {@code query} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Operation(summary = "Validációs kérések listázása / List validation requests", description = "HU: Dokumentált REST művelet. EN: Documented REST operation.")

    @ApiResponses({

            @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

            @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

            @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@GetMapping("/requests")
    public XPathValidationListResponseDto list(@Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @RequestParam(name = "limit", defaultValue = "10") int limit, @Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @RequestParam(name = "query", required = false) String query) {
        LOGGER.info("List endpoint called. limit={}, query={}", limit, safeForLog(query));
        return service.list(limit, query);
    }

    
    /**
     * A {@code getStatus} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a feloldott vagy lekért érték
     */
    @Operation(summary = "Validációs kérés állapotának lekérdezése / Read validation request status", description = "HU: Dokumentált REST művelet. EN: Documented REST operation.")

    @ApiResponses({

            @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

            @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

            @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@GetMapping("/requests/{requestId}")
    public XPathValidationRequestStatusDto getStatus(@Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @PathVariable("requestId") String requestId) {
        LOGGER.info("Status endpoint called. requestId={}", safeForLog(requestId));
        return service.getStatus(requestId);
    }

    
    /**
     * A {@code errors} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    @Operation(summary = "Validációs hibák lekérdezése / Read validation errors", description = "HU: Dokumentált REST művelet. EN: Documented REST operation.")

    @ApiResponses({

            @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

            @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

            @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@GetMapping("/requests/{requestId}/errors")
    public List<XPathValidationIssueDto> errors(@Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @PathVariable("requestId") String requestId) {
        LOGGER.info("Errors endpoint called. requestId={}", safeForLog(requestId));
        return service.getErrors(requestId);
    }

    
    /**
     * A {@code journal} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    @Operation(summary = "Validációs napló lekérdezése / Read validation journal", description = "HU: Dokumentált REST művelet. EN: Documented REST operation.")

    @ApiResponses({

            @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

            @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

            @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@GetMapping("/requests/{requestId}/journal")
    public List<XPathValidationJournalDto> journal(@Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @PathVariable("requestId") String requestId) {
        LOGGER.info("Journal endpoint called. requestId={}", safeForLog(requestId));
        return service.getJournal(requestId);
    }

    
    /**
     * A {@code result} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Operation(summary = "Validációs eredmény letöltése / Download validation result", description = "HU: Dokumentált REST művelet. EN: Documented REST operation.")

    @ApiResponses({

            @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

            @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

            @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
    @GetMapping(value = "/requests/{requestId}/result", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> result(
            @Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.")
            @PathVariable("requestId") String requestId) throws IOException {

        LOGGER.info("Result endpoint called. requestId={}", safeForLog(requestId));

        String xml = service.getResultXml(requestId);
        byte[] content = xml.getBytes(StandardCharsets.UTF_8);

        String safeFileName = sanitizeDownloadFileName(requestId) + ".xml";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("X-Content-Type-Options", "nosniff")
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(safeFileName, StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(content);
    }
/**
 * Korlátozott méretben beolvassa a validációhoz érkező XML request body tartalmát.
 * @param file a {@code file} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private String readXmlPayload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "[NO XML PAYLOAD]";
        }
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOGGER.error("Failed to read posted XML payload for logging.", ex);
            return "[XML PAYLOAD COULD NOT BE READ: " + ex.getMessage() + "]";
        }
    }
/**
 * Naplózási korlátra rövidíti a szöveget úgy, hogy a diagnosztikai előnézet kezelhető méretű maradjon.
 * @param text a {@code text} paraméter átadott értéke
 * @param maxLength a {@code maxLength} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private String truncateForLog(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + System.lineSeparator() + "...[TRUNCATED AFTER " + maxLength + " CHARS]...";
    }
/**
 * A jelenlegi HTTP kéréshez társított session-/korrelációs azonosítót adja vissza.
 * @return a metódus által előállított eredmény
 */

    private String currentSessionId() {
        String value = MDC.get("sessionId");
        return value != null ? value : "UNKNOWN";
    }
/**
 * A bejövő HTTP kérésből összeállítja a naplózáshoz használt teljes URL-t.
 * @param request a {@code request} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private String buildFullUrl(HttpServletRequest request) {
        return request.getRequestURL().toString();
    }

    /**
     * A {@code safeForLog} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String safeForLog(String value) {
        if (value == null) {
            return null;
        }
        return value.replace('\r', ' ').replace('\n', ' ');
    }



    /**
     * A {@code startActiveXmlValidation} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>Az XML-adatot a XPath-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping(path = "/active/start", consumes = MediaType.APPLICATION_JSON_VALUE)
    public XPathValidationRequestStatusDto startActiveXmlValidation(@RequestBody ActiveXPathValidationRequest request) throws Exception {
        if (request == null || request.xmlFileId() == null || request.xmlFileId() <= 0) {
            throw new IllegalArgumentException("Érvénytelen XML fájl azonosító.");
        }
        String safeSessionId = request.xmlFileSessionId() == null ? "" : request.xmlFileSessionId().trim();
        if (safeSessionId.isEmpty() || safeSessionId.length() > 128 || !safeSessionId.matches("[A-Za-z0-9._:@/+\\-]+")) {
            throw new IllegalArgumentException("Érvénytelen XML munkamenet-azonosító.");
        }
        long safeXmlFileId = request.xmlFileId().longValue();
        LOGGER.info("Active XML XPath validation requested. xmlFileId={}", safeXmlFileId);
        return service.submitActiveXmlFile(Long.valueOf(safeXmlFileId), safeSessionId);
    }

    /**
     * A {@code latestActiveXmlValidation} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a XPath-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @GetMapping("/active/latest")
    public XPathValidationRequestStatusDto latestActiveXmlValidation(@RequestParam("xmlFileId") Long xmlFileId) {
        LOGGER.info("Latest active XML XPath validation requested. xmlFileId={}", xmlFileId);
        return service.getLatestForXmlFile(xmlFileId);
    }

    /**
     * A webes rétegek közötti adatátadás strukturált modellje.
     *
     * <p>A {@code ActiveXPathValidationRequest} rekord a web modul XPath-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public record ActiveXPathValidationRequest(Long xmlFileId, String xmlFileSessionId) {}

    /**
     * A {@code duplicate} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler(XPathValidationOrchestratorService.DuplicateRequestIdException.class)
/**
 * A már létező vagy ütköző validációs kéréshez egységes konfliktusválaszt készít.
 * @param ex a {@code ex} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */
    public ResponseEntity<String> duplicate(XPathValidationOrchestratorService.DuplicateRequestIdException ex) {
        LOGGER.error("Duplicate requestId error.", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    /**
     * A {@code queueFull} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler(XPathValidationOrchestratorService.QueueCapacityExceededException.class)
/**
 * A megtelt validációs végrehajtási sorhoz egységes túlterhelési választ készít.
 * @param ex a {@code ex} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */
    public ResponseEntity<String> queueFull(XPathValidationOrchestratorService.QueueCapacityExceededException ex) {
        LOGGER.error("Executor queue full.", ex);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ex.getMessage());
    }

    /**
     * A {@code missingXPathRule} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler(XPathValidationOrchestratorService.MissingXPathRuleException.class)
    public ResponseEntity<String> missingXPathRule(XPathValidationOrchestratorService.MissingXPathRuleException ex) {
        LOGGER.warn("XPath validation cannot start because no rule XML is registered for the form.");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ex.getMessage());
    }

    /**
     * A {@code notFound} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler(XPathValidationOrchestratorService.NotFoundException.class)
/**
 * A nem található validációs erőforráshoz egységes HTTP 404 választ készít.
 * @param ex a {@code ex} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */
    public ResponseEntity<String> notFound(XPathValidationOrchestratorService.NotFoundException ex) {
        LOGGER.error("Requested validation resource was not found.", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * A {@code badRequest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler(XPathValidationOrchestratorService.BadRequestException.class)
/**
 * A hibás validációs kéréshez egységes HTTP 400 választ készít.
 * @param ex a {@code ex} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */
    public ResponseEntity<String> badRequest(XPathValidationOrchestratorService.BadRequestException ex) {
        LOGGER.error("Bad request during XPATH validation.", ex);
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    /**
     * A {@code unsupported} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler(XPathValidationOrchestratorService.UnsupportedMediaTypeException.class)
/**
 * A nem támogatott művelethez vagy bemenethez egységes klienshibaválaszt készít.
 * @param ex a {@code ex} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */
    public ResponseEntity<String> unsupported(XPathValidationOrchestratorService.UnsupportedMediaTypeException ex) {
        LOGGER.error("Unsupported media type during XPATH validation.", ex);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(ex.getMessage());
    }

    /**
     * A {@code generic} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler(Exception.class)
/**
 * A nem várt XPath controller hibát egységes szerverhibaválasszá alakítja.
 * @param ex a {@code ex} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */
    public ResponseEntity<String> generic(Exception ex) {
        LOGGER.error("Unhandled XPATH validator controller exception.", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Váratlan belső hiba történt az XPath validáció során.");
    }

    /**
     * Biztonságos fájlnévvé alakítja a request azonosítót letöltési headerhez.
     *
     * <p>A metódus csak betűket, számokat, pontot, kötőjelet és aláhúzást enged át.
     * Minden más karakter aláhúzásra cserélődik, így a fájlnév nem tud
     * Content-Disposition header törést vagy nem kívánt vezérlő karaktert tartalmazni.</p>
     *
     * @param value a request azonosító
     * @return biztonságos fájlnév-komponens
     */
    private String sanitizeDownloadFileName(String value) {
        if (value == null || value.isBlank()) {
            return "xpath-validation-result";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
