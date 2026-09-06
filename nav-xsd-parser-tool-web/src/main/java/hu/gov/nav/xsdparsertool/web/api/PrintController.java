package hu.gov.nav.xsdparsertool.web.api;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.print.model.PrintOptions;
import hu.gov.nav.xsdparsertool.print.service.DefaultXmlPrintService;
import hu.gov.nav.xsdparsertool.print.service.XmlPrintService;
import hu.gov.nav.xsdparsertool.web.api.dto.ApiErrorResponse;
import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code PrintController} osztály a web modul REST API területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Tag(name = "Print", description = "Nyomtatási HTML és PDF előállítás XML + UI model alapján.")
@RestController
@RequestMapping("/api/print")
public class PrintController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrintController.class);
    private final PathConfigurationProperties pathConfigurationProperties;
    private final XmlPrintService xmlPrintService = new DefaultXmlPrintService();
    private final String appVersion;
    private static final long MAX_UI_MODEL_OVERRIDE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final String UI_MODEL_FILE_EXTENSION = ".xml";

    /**
     * Létrehozza a {@code PrintController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param pathConfigurationProperties a feldolgozásban részt vevő fájl vagy elérési út
     * @param appVersion a művelet bemeneti {@code appVersion} értéke
     */
    public PrintController(PathConfigurationProperties pathConfigurationProperties,
                           @Value("${app.version:1.0.0-SNAPSHOT-20260420103318}") String appVersion) {
        this.pathConfigurationProperties = pathConfigurationProperties;
        this.appVersion = appVersion;
    }

    /**
     * A {@code generateHtml} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param schemaDir a művelet bemeneti {@code schemaDir} értéke
     * @param generalXsdDir a művelet bemeneti {@code generalXsdDir} értéke
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param xmlPath a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param uiModelPath a feldolgozásban részt vevő fájl vagy elérési út
     * @param showFieldIds a művelet bemeneti {@code showFieldIds} értéke
     * @param onlyFilledFields a művelet bemeneti {@code onlyFilledFields} értéke
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Operation(summary = "Nyomtatható HTML előállítása", description = "XML és UI model alapján A4-es nyomtatási HTML előállítása. UI model inspect alapján oldódik fel, de manuálisan felülírható.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTML sikeresen előállítva", content = @Content(mediaType = "text/html")),
            @ApiResponse(responseCode = "400", description = "Hibás kérés", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Belső szerverhiba", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping(path = "/html", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> generateHtml(
            @Parameter(description = "Schema gyökérkönyvtár") @RequestParam(value = "schemaDir", required = false) String schemaDir,
            @Parameter(description = "Általános XSD könyvtár") @RequestParam(value = "generalXsdDir", required = false) String generalXsdDir,
            @Parameter(description = "XML feltöltés") @RequestParam(value = "xmlFile", required = false) MultipartFile xmlFile,
            @Parameter(description = "Szerver oldali XML elérési út") @RequestParam(value = "xmlPath", required = false) String xmlPath,
            @Parameter(description = "Manuális UI model override fájlútvonal") @RequestParam(value = "uiModelPath", required = false) String uiModelPath,
            @Parameter(description = "Mezőazonosítók megjelenítése") @RequestParam(value = "showFieldIds", required = false, defaultValue = "false") boolean showFieldIds,
            @Parameter(description = "Csak kitöltött mezők megjelenítése") @RequestParam(value = "onlyFilledFields", required = false, defaultValue = "false") boolean onlyFilledFields) throws IOException {
        if (uiModelPath != null && (uiModelPath.length() > 4096 || uiModelPath.indexOf('\0') >= 0)) {
            throw new IllegalArgumentException("A UI model útvonal túl hosszú vagy érvénytelen.");
        }
        RequestFiles requestFiles = prepareRequestFiles(schemaDir, generalXsdDir, xmlFile, xmlPath);
        try {
            PrintOptions options = preparePrintOptions(uiModelPath, showFieldIds, onlyFilledFields);
            String html = xmlPrintService.generateHtml(requestFiles.xmlFile(), requestFiles.schemaDir(), requestFiles.generalXsdDir(), requestFiles.uiModelDir(), options);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline()
                                    .filename(requestFiles.safeHtmlFileName(), StandardCharsets.UTF_8)
                                    .build()
                                    .toString()
                    )
                    .body(html);
        } finally {
            requestFiles.cleanup();
        }
    }

    /**
     * A {@code generatePdf} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param schemaDir a művelet bemeneti {@code schemaDir} értéke
     * @param generalXsdDir a művelet bemeneti {@code generalXsdDir} értéke
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param xmlPath a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param uiModelPath a feldolgozásban részt vevő fájl vagy elérési út
     * @param showFieldIds a művelet bemeneti {@code showFieldIds} értéke
     * @param onlyFilledFields a művelet bemeneti {@code onlyFilledFields} értéke
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Operation(summary = "Nyomtatható PDF előállítása", description = "XML és UI model alapján A4-es PDF előállítása.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF sikeresen előállítva", content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "400", description = "Hibás kérés", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Belső szerverhiba", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping(path = "/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generatePdf(
            @Parameter(description = "Schema gyökérkönyvtár") @RequestParam(value = "schemaDir", required = false) String schemaDir,
            @Parameter(description = "Általános XSD könyvtár") @RequestParam(value = "generalXsdDir", required = false) String generalXsdDir,
            @Parameter(description = "XML feltöltés") @RequestParam(value = "xmlFile", required = false) MultipartFile xmlFile,
            @Parameter(description = "Szerver oldali XML elérési út") @RequestParam(value = "xmlPath", required = false) String xmlPath,
            @Parameter(description = "Manuális UI model override fájlútvonal") @RequestParam(value = "uiModelPath", required = false) String uiModelPath,
            @Parameter(description = "Mezőazonosítók megjelenítése") @RequestParam(value = "showFieldIds", required = false, defaultValue = "false") boolean showFieldIds,
            @Parameter(description = "Csak kitöltött mezők megjelenítése") @RequestParam(value = "onlyFilledFields", required = false, defaultValue = "false") boolean onlyFilledFields) throws IOException {
        if (uiModelPath != null && (uiModelPath.length() > 4096 || uiModelPath.indexOf('\0') >= 0)) {
            throw new IllegalArgumentException("A UI model útvonal túl hosszú vagy érvénytelen.");
        }
        RequestFiles requestFiles = prepareRequestFiles(schemaDir, generalXsdDir, xmlFile, xmlPath);
        try {
            PrintOptions options = preparePrintOptions(uiModelPath, showFieldIds, onlyFilledFields);
            byte[] pdf = xmlPrintService.generatePdf(requestFiles.xmlFile(), requestFiles.schemaDir(), requestFiles.generalXsdDir(), requestFiles.uiModelDir(), options);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.inline()
                                    .filename(requestFiles.safePdfFileName(), StandardCharsets.UTF_8)
                                    .build()
                                    .toString()
                    )
                    .body(pdf);
        } finally {
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
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        LOGGER.error("Unhandled print controller exception.", ex);
        return ResponseEntity.internalServerError()
                .body(new ApiErrorResponse("Váratlan belső hiba történt a nyomtatás során."));
    }

    /**
     * A {@code preparePrintOptions} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param uiModelPath a feldolgozásban részt vevő fájl vagy elérési út
     * @param showFieldIds a művelet bemeneti {@code showFieldIds} értéke
     * @param onlyFilledFields a művelet bemeneti {@code onlyFilledFields} értéke
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private PrintOptions preparePrintOptions(String uiModelPath, boolean showFieldIds, boolean onlyFilledFields) throws IOException {
        PrintOptions options = new PrintOptions();
        options.setShowFieldIds(showFieldIds);
        options.setOnlyFilledFields(onlyFilledFields);
        options.setAppVersion(appVersion);

        if (StringUtils.hasText(uiModelPath)) {
            options.setUiModelOverrideFile(resolveSafeUiModelOverrideFile(uiModelPath));
        }

        return options;
    }

    /**
     * A {@code resolveSafeUiModelOverrideFile} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param uiModelPath a feldolgozásban részt vevő fájl vagy elérési út
     * @return a feloldott vagy lekért érték
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private Path resolveSafeUiModelOverrideFile(String uiModelPath) throws IOException {
        if (!StringUtils.hasText(uiModelPath)) {
            return null;
        }

        Path configuredUiModelDir = resolveOptionalDirectory(
                null,
                pathConfigurationProperties.getUiModelDir(),
                "A megadott UI model könyvtár nem létezik: "
        );

        if (configuredUiModelDir == null) {
            throw new IllegalArgumentException("Manuális UI model override csak konfigurált UI model könyvtár mellett használható.");
        }

        Path baseDir = configuredUiModelDir.toAbsolutePath().normalize().toRealPath(LinkOption.NOFOLLOW_LINKS);
        Path requestedPath = Path.of(uiModelPath.trim());

        Path resolvedPath = requestedPath.isAbsolute()
                ? requestedPath.toAbsolutePath().normalize()
                : baseDir.resolve(requestedPath).toAbsolutePath().normalize();

        if (!resolvedPath.startsWith(baseDir)) {
            throw new IllegalArgumentException("A megadott UI model fájl kívül esik az engedélyezett UI model könyvtáron: " + resolvedPath);
        }

        if (!ExceptionSafeOperations.isRegularFile(resolvedPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("A megadott UI model fájl nem létezik: " + resolvedPath);
        }

        if (!Files.isReadable(resolvedPath)) {
            throw new IllegalArgumentException("A megadott UI model fájl nem olvasható: " + resolvedPath);
        }

        String fileName = resolvedPath.getFileName() == null
                ? ""
                : resolvedPath.getFileName().toString();

        if (!endsWithIgnoreCase(fileName, UI_MODEL_FILE_EXTENSION)) {
            throw new IllegalArgumentException("A manuális UI model override csak .xml fájl lehet: " + resolvedPath);
        }

        long fileSize = Files.size(resolvedPath);
        if (fileSize > MAX_UI_MODEL_OVERRIDE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "A manuális UI model fájl túl nagy. Maximális méret: "
                            + MAX_UI_MODEL_OVERRIDE_SIZE_BYTES
                            + " byte."
            );
        }

        return resolvedPath;
    }
    /**
     * A {@code prepareRequestFiles} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param schemaDirValue a művelet bemeneti {@code schemaDirValue} értéke
     * @param generalXsdDirValue a művelet bemeneti {@code generalXsdDirValue} értéke
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param xmlPathValue a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
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
            String originalFileName = sanitizeDownloadFileName(
                    Objects.toString(xmlFile.getOriginalFilename(), "uploaded.xml")
            );

            Path tempFile = SecureFileOperations.createPrivateTempFile("nav-xsd-parser-tool-print-", ".xml");

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
                    sanitizeDownloadFileName(xmlPath.getFileName().toString()),
                    false
            );
        }

        throw new IllegalArgumentException("Adj meg XML fájlt feltöltéssel vagy szerver oldali elérési úttal.");
    }

    /**
     * A {@code sanitizeDownloadFileName} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String sanitizeDownloadFileName(String value) {
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
     * A {@code resolveRequiredDirectory} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param explicitValue a művelet bemeneti {@code explicitValue} értéke
     * @param configuredValue a művelethez szükséges konfigurációs adatok
     * @param missingMessage a művelet bemeneti {@code missingMessage} értéke
     * @return a feloldott vagy lekért érték
     */
    private Path resolveRequiredDirectory(String explicitValue, String configuredValue, String missingMessage) {
        String effectiveValue = StringUtils.hasText(explicitValue) ? explicitValue.trim() : StringUtils.hasText(configuredValue) ? configuredValue.trim() : null;
        if (!StringUtils.hasText(effectiveValue)) {
            throw new IllegalArgumentException(missingMessage);
        }
        Path directory = Path.of(effectiveValue).toAbsolutePath().normalize();
        if (!ExceptionSafeOperations.isDirectory(directory)) {
            throw new IllegalArgumentException("A megadott schema gyökérkönyvtár nem létezik: " + directory);
        }
        return directory;
    }

    /**
     * A {@code resolveOptionalDirectory} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param explicitValue a művelet bemeneti {@code explicitValue} értéke
     * @param configuredValue a művelethez szükséges konfigurációs adatok
     * @param invalidPrefix a művelet bemeneti {@code invalidPrefix} értéke
     * @return a feloldott vagy lekért érték
     */
    private Path resolveOptionalDirectory(String explicitValue, String configuredValue, String invalidPrefix) {
        String effectiveValue = StringUtils.hasText(explicitValue) ? explicitValue.trim() : StringUtils.hasText(configuredValue) ? configuredValue.trim() : null;
        if (!StringUtils.hasText(effectiveValue)) {
            return null;
        }
        Path directory = Path.of(effectiveValue).toAbsolutePath().normalize();
        if (!ExceptionSafeOperations.isDirectory(directory)) {
            throw new IllegalArgumentException(invalidPrefix + directory);
        }
        return directory;
    }

    /**
     * A {@code endsWithIgnoreCase} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @param suffix a művelet bemeneti {@code suffix} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private static boolean endsWithIgnoreCase(String value, String suffix) {
        if (value == null || suffix == null || value.length() < suffix.length()) {
            return false;
        }
        return value.regionMatches(true, value.length() - suffix.length(), suffix, 0, suffix.length());
    }

    /**
     * A web modul REST API területének közös alkalmazási típusa.
     *
     * <p>A {@code RequestFiles} rekord a web modul REST API területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record RequestFiles(Path xmlFile,
                                Path schemaDir,
                                Path generalXsdDir,
                                Path uiModelDir,
                                String originalFileName,
                                boolean temporaryUpload) {

        /**
         * A {@code safeHtmlFileName} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
         * @return a művelet feldolgozási eredménye
         */
        String safeHtmlFileName() {
            return baseNameWithoutXmlExtension() + ".html";
        }

        /**
         * A {@code safePdfFileName} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
         * @return a művelet feldolgozási eredménye
         */
        String safePdfFileName() {
            return baseNameWithoutXmlExtension() + ".pdf";
        }

        /**
         * A {@code baseNameWithoutXmlExtension} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>Az XML-adatot a REST API folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
         * @return a művelet feldolgozási eredménye
         */
        private String baseNameWithoutXmlExtension() {
            String baseName = originalFileName;
            if (endsWithIgnoreCase(baseName, ".xml")) {
                baseName = baseName.substring(0, baseName.length() - 4);
            }
            return baseName;
        }

        /**
         * A {@code cleanup} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         */
        void cleanup() {
            if (!temporaryUpload) {
                return;
            }
            try {
                Files.deleteIfExists(xmlFile);
            } catch (IOException ignored) {
            }
        }
    }
}
