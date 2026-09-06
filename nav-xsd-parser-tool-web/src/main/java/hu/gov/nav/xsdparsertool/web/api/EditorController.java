package hu.gov.nav.xsdparsertool.web.api;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.core.model.form.FormData;
import hu.gov.nav.xsdparsertool.core.model.form.FormDefinition;
import hu.gov.nav.xsdparsertool.core.model.processing.ProcessingResult;
import hu.gov.nav.xsdparsertool.core.model.processing.ValidationResult;
import hu.gov.nav.xsdparsertool.core.model.xmlview.XmlDocumentView;
import hu.gov.nav.xsdparsertool.processing.form.FormDataBuilderService;
import hu.gov.nav.xsdparsertool.processing.form.FormDefinitionBuilderService;
import hu.gov.nav.xsdparsertool.processing.service.XmlProcessingService;
import hu.gov.nav.xsdparsertool.processing.xml.XmlProbeService;
import hu.gov.nav.xsdparsertool.processing.xmlview.XmlViewBuilderService;
import hu.gov.nav.xsdparsertool.schemaregistry.model.XmlProbeResult;
import hu.gov.nav.xsdparsertool.web.api.dto.*;
import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import hu.gov.nav.xsdparsertool.web.path.ConfiguredPathSupport;
import hu.gov.nav.xsdparsertool.web.path.VersionedArtifactPathResolver;
import hu.gov.nav.xsdparsertool.web.config.UiMenuVisibilityService;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.LargeXmlPreviewService;
import hu.gov.nav.xsdparsertool.web.xpath.config.XPathValidatorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
/**
 * Az XML felderítési, validációs és űrlap-előkészítési műveletek webes belépési pontjait biztosító controller.
 * Az osztály a api csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: @RestController.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: @RestController.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


@Tag(name = "Editor", description = "XML inspektálási és validálási REST végpontok. / XML inspection and validation REST endpoints.")
@RestController
@RequestMapping("/api")
public class EditorController {
    private static final Logger log = LoggerFactory.getLogger(EditorController.class);
    private final XmlProcessingService xmlProcessingService;
    private final XmlProbeService xmlProbeService;
    private final FormDefinitionBuilderService formDefinitionBuilderService;
    private final FormDataBuilderService formDataBuilderService;
    private final XmlViewBuilderService xmlViewBuilderService;
    private final PathConfigurationProperties pathConfigurationProperties;
    private final String appVersion;
    private final UiMenuVisibilityService uiMenuVisibilityService;
    private final Environment environment;
    private final XmlFileStorageProperties xmlFileStorageProperties;
    private final XPathValidatorProperties xpathValidatorProperties;
    private final LargeXmlPreviewService largeXmlPreviewService;

    /**
     * Létrehozza a {@code EditorController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param xmlProcessingService a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param xmlProbeService a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param formDefinitionBuilderService a művelet bemeneti {@code formDefinitionBuilderService} értéke
     * @param formDataBuilderService a művelet bemeneti {@code formDataBuilderService} értéke
     * @param xmlViewBuilderService a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param pathConfigurationProperties a feldolgozásban részt vevő fájl vagy elérési út
     * @param appVersion a művelet bemeneti {@code appVersion} értéke
     * @param uiMenuVisibilityService a művelet bemeneti {@code uiMenuVisibilityService} értéke
     * @param environment a művelet bemeneti {@code environment} értéke
     * @param xmlFileStorageProperties a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param xpathValidatorProperties a feldolgozásban részt vevő fájl vagy elérési út
     * @param largeXmlPreviewService a feldolgozandó XML-hez tartozó adat vagy tartalom
     */
    public EditorController(XmlProcessingService xmlProcessingService,
                            XmlProbeService xmlProbeService,
                            FormDefinitionBuilderService formDefinitionBuilderService,
                            FormDataBuilderService formDataBuilderService,
                            XmlViewBuilderService xmlViewBuilderService,
                            PathConfigurationProperties pathConfigurationProperties,
                            @Value("${app.version:dev}") String appVersion,
                            UiMenuVisibilityService uiMenuVisibilityService,
                            Environment environment,
                            XmlFileStorageProperties xmlFileStorageProperties,
                            XPathValidatorProperties xpathValidatorProperties,
                            LargeXmlPreviewService largeXmlPreviewService) {
        this.xmlProcessingService = xmlProcessingService;
        this.xmlProbeService = xmlProbeService;
        this.formDefinitionBuilderService = formDefinitionBuilderService;
        this.formDataBuilderService = formDataBuilderService;
        this.xmlViewBuilderService = xmlViewBuilderService;
        this.pathConfigurationProperties = pathConfigurationProperties;
        this.appVersion = appVersion;
        this.uiMenuVisibilityService = uiMenuVisibilityService;
        this.environment = environment;
        this.xmlFileStorageProperties = xmlFileStorageProperties;
        this.xpathValidatorProperties = xpathValidatorProperties;
        this.largeXmlPreviewService = largeXmlPreviewService;
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
    public AppConfigResponse config() {
        return new AppConfigResponse(
                pathConfigurationProperties.getSchemaDir(),
                pathConfigurationProperties.getGeneralXsdPath(),
                xmlFileStorageProperties.getUploadDir(),
                xpathValidatorProperties.getRuleRootDir(),
                pathConfigurationProperties.getUiModelDir(),
                appVersion,
                normalizeFormRenderer(environment.getProperty("nav.xsdparsertool.form.renderer.default", "uimodel")),
                headerMenuVisibility(),
                normalizeValidationDrawerSide(environment.getProperty("nav.xsdparsertool.form.validation-drawer.side", "right"))
        );
    }

    /**
     * A {@code headerMenuVisibility} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    private java.util.Map<String, Boolean> headerMenuVisibility() {
        return uiMenuVisibilityService.headerMenuVisibility();
    }

    /**
     * A {@code normalizeValidationDrawerSide} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String normalizeValidationDrawerSide(String value) {
        if (value == null) {
            return "right";
        }
        String normalized = value.trim();
        return "left".equalsIgnoreCase(normalized) ? "left" : "right";
    }

    /**
     * A {@code normalizeFormRenderer} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String normalizeFormRenderer(String value) {
        if (value == null) {
            return "uimodel";
        }
        String normalized = value.trim();
        if ("uimodel".equalsIgnoreCase(normalized) || "ui-model".equalsIgnoreCase(normalized) || "ui_model".equalsIgnoreCase(normalized)) {
            return "uimodel";
        }
        return "classic";
    }


    
    /**
     * A {@code inspect} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param schemaDir a művelet bemeneti {@code schemaDir} értéke
     * @param generalXsdDir a művelet bemeneti {@code generalXsdDir} értéke
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param xmlPath a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Operation(summary = "XML dokumentum inspektálása / Inspect XML document", description = "HU: Dokumentált REST művelet. EN: Documented REST operation.")

    @ApiResponses({

            @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

            @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

            @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@PostMapping(path = "/inspect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InspectResponse inspect(@Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @RequestParam(value = "schemaDir", required = false) String schemaDir, @Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @RequestParam(value = "generalXsdDir", required = false) String generalXsdDir, @Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @RequestParam(value = "xmlFile", required = false) MultipartFile xmlFile, @Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @RequestParam(value = "xmlPath", required = false) String xmlPath) throws IOException {
        RequestFiles requestFiles = prepareRequestFiles(schemaDir, generalXsdDir, xmlFile, xmlPath);
        try {
            XmlProbeResult probe = xmlProbeService.probe(requestFiles.xmlFile());
            ProcessingResult processingResult = xmlProcessingService.inspect(requestFiles.xmlFile(), requestFiles.schemaDir(), requestFiles.generalXsdDir(), requestFiles.uiModelDir());
            return ApiResponseMapper.toInspectResponse(probe, processingResult, requestFiles.originalFileName());
        } finally {
            requestFiles.cleanup();
        }
    }

    
    /**
     * A {@code validate} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param schemaDir a művelet bemeneti {@code schemaDir} értéke
     * @param generalXsdDir a művelet bemeneti {@code generalXsdDir} értéke
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param xmlPath a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param requestedLargeFileMode a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Operation(summary = "XML dokumentum validálása / Validate XML document", description = "HU: Dokumentált REST művelet. EN: Documented REST operation.")

    @ApiResponses({

            @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

            @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

            @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@PostMapping(path = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ValidateResponse validate(
            @Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @RequestParam(value = "schemaDir", required = false) String schemaDir,
            @Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @RequestParam(value = "generalXsdDir", required = false) String generalXsdDir,
            @Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @RequestParam(value = "xmlFile", required = false) MultipartFile xmlFile,
            @Parameter(description = "HU: Végpont paraméter. EN: Endpoint parameter.") @RequestParam(value = "xmlPath", required = false) String xmlPath,
            @RequestParam(value = "largeFileMode", required = false, defaultValue = "false") boolean requestedLargeFileMode) throws IOException {
        RequestFiles requestFiles = prepareRequestFiles(schemaDir, generalXsdDir, xmlFile, xmlPath);
        Path processingXml = requestFiles.xmlFile();
        Path largePreview = null;
        LargeXmlPreviewService.PreviewResult largePreviewResult = null;
        try {
            long sourceBytes = Files.size(requestFiles.xmlFile());
            boolean largeFileMode = requestedLargeFileMode
                    || sourceBytes >= xmlFileStorageProperties.getLargeFile().thresholdBytes();
            log.info("XML validate útvonal kiválasztva. source={}, sourceBytes={}, requestedLargeFileMode={}, largeFileMode={}",
                    requestFiles.xmlFile(), sourceBytes, requestedLargeFileMode, largeFileMode);
            if (largeFileMode) {
                largePreviewResult = largeXmlPreviewService.createMainFormPreview(requestFiles.xmlFile());
                largePreview = largePreviewResult.previewPath();
                processingXml = largePreview;
                log.info("Nagy XML preview kerül feldolgozásra. preview={}, previewBytes={}",
                        largePreview, Files.size(largePreview));
            }

            XmlProbeResult probe = xmlProbeService.probe(processingXml);
            ProcessingResult inspection = xmlProcessingService.inspect(processingXml, requestFiles.schemaDir(), requestFiles.generalXsdDir(), requestFiles.uiModelDir());
            ValidationResult validation = xmlProcessingService.validate(processingXml, requestFiles.schemaDir(), requestFiles.generalXsdDir(), requestFiles.uiModelDir());
            // XSD-invalid, but well-formed XML must still be repairable from the form viewer.
            // Nagy XML esetén kizárólag a StAX-szal kinyert főlap-előnézeten fut a
            // meglévő form pipeline, ezért a teljes dokumentumhoz nem épül DOM,
            // FormData, XML-fa vagy raw XML String.
            FormDefinition formDefinition = formDefinitionBuilderService.build(inspection.getDocumentDefinition(), inspection.getSchemaBundle());
            FormData formData = formDefinition != null ? formDataBuilderService.build(formDefinition, processingXml) : null;
            XmlDocumentView xmlView = null;
            if (!largeFileMode
                    || (!xmlFileStorageProperties.getLargeFile().isDisableXmlTree()
                    && !xmlFileStorageProperties.getLargeFile().isDisableXmlSource())) {
                xmlView = xmlViewBuilderService.build(processingXml);
            }
            ValidateResponse response = ApiResponseMapper.toValidateResponse(probe, inspection, validation, formDefinition, formData, xmlView, requestFiles.originalFileName());
            response.setXpathRuleFile(resolveXpathRuleFile(inspection));
            response.setLargeFileMode(largeFileMode);
            response.setPartialPreview(largeFileMode);
            response.setFullDocumentValidationPerformed(!largeFileMode);
            if (largeFileMode) {
                response.setFormRuntimePreviewXml(Files.readString(processingXml));
                if (largePreviewResult != null) {
                    response.setLargeXmlRepeatingFormName(largePreviewResult.repeatingFormName());
                    response.setLargeXmlRepeatingFormCount(largePreviewResult.repeatingFormCount());
                }
                response.setLargeFileMessage("Nagy XML mód: a teljes állomány memóriába töltése nélkül a főlap és az első melléklap előnézete nyílt meg. Az XML-fa, a teljes XML-forrás és az automatikus teljes dokumentum-validáció nem készült el.");
            }
            log.info("XML validate válasz összeállt. largeFileMode={}, partialPreview={}, valid={}, formDefinition={}, formData={}, schemaBundle={}, xmlView={}",
                    response.isLargeFileMode(), response.isPartialPreview(), response.isValid(),
                    response.getFormDefinition() != null, response.getFormData() != null,
                    response.getSchemaBundle() != null, response.getXmlView() != null);
            return response;
        } finally {
            if (largePreview != null) {
                Files.deleteIfExists(largePreview);
            }
            requestFiles.cleanup();
        }
    }

    /**
     * A {@code handleBadRequest} művelet kezeli a kapcsolódó eseményt vagy feldolgozási ágat.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler(IllegalArgumentException.class)
/**
 * A klienshibát egységes HTTP 400 válaszobjektummá alakítja.
 * @param ex a {@code ex} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(ex.getMessage()));
    }

    /**
     * A {@code handleUnexpected} művelet kezeli a kapcsolódó eseményt vagy feldolgozási ágat.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler(Exception.class)
/**
 * A nem várt szerveroldali hibát egységes HTTP 500 válaszobjektummá alakítja és naplózza.
 * @param ex a {@code ex} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(ex.getMessage() == null ? "Unexpected server error" : ex.getMessage()));
    }
/**
 * Ellenőrzi és feloldja az editor kéréshez szükséges XML-, séma- és UIModel útvonalakat.
 * @param schemaDirValue a {@code schemaDirValue} paraméter átadott értéke
 * @param generalXsdDirValue a {@code generalXsdDirValue} paraméter átadott értéke
 * @param xmlFile a {@code xmlFile} paraméter átadott értéke
 * @param xmlPathValue a {@code xmlPathValue} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 * @throws IOException Hiba esetén dobott kivétel.
 */

private RequestFiles prepareRequestFiles(
        String schemaDirValue,
        String generalXsdDirValue,
        MultipartFile xmlFile,
        String xmlPathValue) throws IOException {

    Path schemaDir = resolveRequiredDirectory(
            schemaDirValue,
            pathConfigurationProperties.getSchemaDir(),
            "A schema gyökérkönyvtár megadása kötelező."
    );
    Path generalXsdDir = resolveOptionalDirectory(
            generalXsdDirValue,
            pathConfigurationProperties.getCommonXsdDir(),
            "A megadott common XSD könyvtár nem létezik: "
    );
    Path uiModelDir = resolveOptionalDirectory(
            null,
            pathConfigurationProperties.getUiModelDir(),
            "A megadott UI model könyvtár nem létezik: "
    );

    if (xmlFile != null && !xmlFile.isEmpty()) {
        String originalFileName = sanitizeFileName(
                Objects.toString(xmlFile.getOriginalFilename(), "uploaded.xml")
        );

        Path tempFile = SecureFileOperations.createPrivateTempFile("nav-xsd-parser-tool-", ".xml");

        try (InputStream inputStream = xmlFile.getInputStream()) {
            SecureFileOperations.copyPrivate(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return new RequestFiles(
                tempFile,
                schemaDir,
                generalXsdDir,
                uiModelDir,
                originalFileName,
                true
        );
    }

    if (StringUtils.hasText(xmlPathValue)) {
        Path xmlPath = Path.of(xmlPathValue.trim()).toAbsolutePath().normalize();

        if (!ExceptionSafeOperations.isRegularFile(xmlPath)) {
            throw new IllegalArgumentException("A megadott XML fájl nem létezik: " + xmlPath);
        }

        return new RequestFiles(
                xmlPath,
                schemaDir,
                generalXsdDir,
                uiModelDir,
                sanitizeFileName(xmlPath.getFileName().toString()),
                false
        );
    }



    throw new IllegalArgumentException("Adj meg XML fájlt feltöltéssel vagy szerver oldali elérési úttal.");
}


    /**
     * A {@code sanitizeFileName} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String sanitizeFileName(String value) {
        if (!StringUtils.hasText(value)) {
            return "uploaded.xml";
        }

        String fileName = Path.of(value).getFileName().toString();
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");

        if (!StringUtils.hasText(sanitized)) {
            return "uploaded.xml";
        }

        return sanitized;
    }
/**
 * Felold egy kötelező konfigurációs könyvtárat, és hibát jelez, ha nincs használható érték.
 * @param explicitValue a {@code explicitValue} paraméter átadott értéke
 * @param configuredValue a {@code configuredValue} paraméter átadott értéke
 * @param missingMessage a {@code missingMessage} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private Path resolveRequiredDirectory(String explicitValue, String configuredValue, String missingMessage) {
        String effectiveValue = StringUtils.hasText(explicitValue) ? explicitValue.trim() : StringUtils.hasText(configuredValue) ? configuredValue.trim() : null;
        if (!StringUtils.hasText(effectiveValue)) throw new IllegalArgumentException(missingMessage);
        Path directory = Path.of(effectiveValue).toAbsolutePath().normalize();
        if (!ExceptionSafeOperations.isDirectory(directory)) throw new IllegalArgumentException("A megadott schema gyökérkönyvtár nem létezik: " + directory);
        return directory;
    }
/**
 * Felold egy opcionális konfigurációs könyvtárat; hiányzó vagy üres beállításnál nem kényszerít fallbacket.
 * @param explicitValue a {@code explicitValue} paraméter átadott értéke
 * @param configuredValue a {@code configuredValue} paraméter átadott értéke
 * @param invalidPrefix a {@code invalidPrefix} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private Path resolveOptionalDirectory(String explicitValue, String configuredValue, String invalidPrefix) {
        String effectiveValue = StringUtils.hasText(explicitValue) ? explicitValue.trim() : StringUtils.hasText(configuredValue) ? configuredValue.trim() : null;
        if (!StringUtils.hasText(effectiveValue)) return null;
        Path directory = Path.of(effectiveValue).toAbsolutePath().normalize();
        if (!ExceptionSafeOperations.isDirectory(directory)) throw new IllegalArgumentException(invalidPrefix + directory);
        return directory;
    }


    /**
     * A {@code resolveXpathRuleFile} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param inspection a művelet bemeneti {@code inspection} értéke
     * @return a feloldott vagy lekért érték
     */
    private String resolveXpathRuleFile(ProcessingResult inspection) {
        if (inspection == null || inspection.getSchemaBundle() == null) return null;
        String documentType = inspection.getSchemaBundle().getDocumentType();
        String version = inspection.getSchemaBundle().getDocumentVersion();
        if (!StringUtils.hasText(documentType) || !StringUtils.hasText(version) || !StringUtils.hasText(xpathValidatorProperties.getRuleRootDir())) {
            return null;
        }
        String rawXpathRuleDir = xpathValidatorProperties.getRuleRootDir();
        Path path = VersionedArtifactPathResolver.resolveXpathRule(
                ConfiguredPathSupport.toAbsoluteNormalizedPath(rawXpathRuleDir), documentType, version);
        log.info("[XPATH-VALIDATOR-PATH] rawXpathRuleDir={} documentType={} documentVersion={} result={} exists={}",
                rawXpathRuleDir, documentType, version, path, ExceptionSafeOperations.isRegularFile(path));
        return ExceptionSafeOperations.isRegularFile(path) ? path.toString() : null;
    }


    /**
     * Az editor kéréshez már ellenőrzött fájl- és könyvtárútvonalakat hordozó belső értékobjektum.
     * @param xmlFile a {@code xmlFile} paraméter átadott értéke
     * @param schemaDir a {@code schemaDir} paraméter átadott értéke
     * @param generalXsdDir a {@code generalXsdDir} paraméter átadott értéke
     * @param uiModelDir a {@code uiModelDir} paraméter átadott értéke
     * @param originalFileName a {@code originalFileName} paraméter átadott értéke
     * @param temporaryUpload a {@code temporaryUpload} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     */
    private record RequestFiles(Path xmlFile, Path schemaDir, Path generalXsdDir, Path uiModelDir, String originalFileName, boolean temporaryUpload) {
        /**
         * A {@code cleanup} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         */
        void cleanup() {
            if (!temporaryUpload) return;
            try { Files.deleteIfExists(xmlFile); } catch (IOException ignored) {}
        }
    }
}
