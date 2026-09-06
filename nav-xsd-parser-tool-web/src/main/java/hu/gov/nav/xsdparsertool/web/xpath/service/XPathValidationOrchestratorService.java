package hu.gov.nav.xsdparsertool.web.xpath.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.processing.ValidationResult;
import hu.gov.nav.xsdparsertool.core.model.validation.ValidationIssue;
import hu.gov.nav.xsdparsertool.processing.validation.XsdValidationService;
import hu.gov.nav.xsdparsertool.processing.xml.XmlProbeService;
import hu.gov.nav.xsdparsertool.schemaregistry.model.XmlProbeResult;
import hu.gov.nav.xsdparsertool.schemaregistry.service.FileSystemSchemaRegistryService;
import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import hu.gov.nav.xsdparsertool.web.processing.dto.ProcessingJobDto;
import hu.gov.nav.xsdparsertool.web.processing.service.ProcessingJobService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileSessionEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileSessionRepository;
import hu.gov.nav.xsdparsertool.web.xpath.config.XPathValidatorProperties;
import hu.gov.nav.xsdparsertool.web.path.ConfiguredPathSupport;
import hu.gov.nav.xsdparsertool.web.path.VersionedArtifactPathResolver;
import hu.gov.nav.xsdparsertool.web.xpath.dto.XPathValidationIssueDto;
import hu.gov.nav.xsdparsertool.web.xpath.dto.XPathValidationJournalDto;
import hu.gov.nav.xsdparsertool.web.xpath.dto.XPathValidationListItemDto;
import hu.gov.nav.xsdparsertool.web.xpath.dto.XPathValidationListResponseDto;
import hu.gov.nav.xsdparsertool.web.xpath.dto.XPathValidationRequestStatusDto;
import hu.gov.nav.xsdparsertool.web.xpath.entity.XPathValidationErrorEntity;
import hu.gov.nav.xsdparsertool.web.xpath.entity.XPathValidationRequestEntity;
import hu.gov.nav.xsdparsertool.web.xpath.entity.XPathValidationRequestJournalEntity;
import hu.gov.nav.xsdparsertool.web.xpath.model.CreateResultMode;
import hu.gov.nav.xsdparsertool.web.xpath.model.ResultStatus;
import hu.gov.nav.xsdparsertool.web.xpath.model.ValidatorStatus;
import hu.gov.nav.xsdparsertool.web.xpath.model.XPathValidationExecutionResult;
import hu.gov.nav.xsdparsertool.web.xpath.model.XPathValidationIssue;
import hu.gov.nav.xsdparsertool.web.xpath.repository.XPathValidationErrorRepository;
import hu.gov.nav.xsdparsertool.web.xpath.repository.XPathValidationRequestJournalRepository;
import hu.gov.nav.xsdparsertool.web.xpath.repository.XPathValidationRequestRepository;
import hu.gov.nav.xsdparsertool.web.xpath.util.UuidV7Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
/**
 * A XPath-validációs kérések létrehozását, háttérfeldolgozását, XSD-előellenőrzését, állapotváltásait és naplózását koordináló szolgáltatás.
 * Az osztály a service csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: @Service.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: @Service.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


@Service
public class XPathValidationOrchestratorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(XPathValidationOrchestratorService.class);
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9]{18}");
    private static final long MAX_UPLOAD_SIZE_BYTES = 150L * 1024L * 1024L;
    private static final int MAX_LOGGED_PAYLOAD_CHARS = 4000;

    private final XPathValidationRequestRepository requestRepository;
    private final XPathValidationRequestJournalRepository journalRepository;
    private final XPathValidationErrorRepository errorRepository;
    private final XPathValidatorProperties properties;
    private final ThreadPoolTaskExecutor xpathValidatorExecutor;
    private final XsltValidationService xsltValidationService;
    private final FileSystemSchemaRegistryService schemaRegistryService;
    private final XmlProbeService xmlProbeService;
    private final XsdValidationService xsdValidationService;
    private final PathConfigurationProperties pathConfigurationProperties;
    private final XmlFileRepository xmlFileRepository;
    private final XmlFileSessionRepository xmlFileSessionRepository;
    private final ProcessingJobService processingJobService;
    private final CurrentUserService currentUserService;

    /**
     * Létrehozza a {@code XPathValidationOrchestratorService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param requestRepository a művelet bemeneti kérésadatait tartalmazó objektum
     * @param journalRepository a művelet bemeneti {@code journalRepository} értéke
     * @param errorRepository a művelet bemeneti {@code errorRepository} értéke
     * @param properties a művelethez szükséges konfigurációs adatok
     * @param xpathValidatorExecutor a feldolgozásban részt vevő fájl vagy elérési út
     * @param xsltValidationService a művelet bemeneti {@code xsltValidationService} értéke
     * @param schemaRegistryService a művelet bemeneti {@code schemaRegistryService} értéke
     * @param xmlProbeService a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param xsdValidationService a művelet bemeneti {@code xsdValidationService} értéke
     * @param pathConfigurationProperties a feldolgozásban részt vevő fájl vagy elérési út
     * @param xmlFileRepository a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param xmlFileSessionRepository a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param processingJobService a művelet bemeneti {@code processingJobService} értéke
     * @param currentUserService a művelet felhasználói kontextusa vagy felhasználóneve
     */
    public XPathValidationOrchestratorService(XPathValidationRequestRepository requestRepository,
                                              XPathValidationRequestJournalRepository journalRepository,
                                              XPathValidationErrorRepository errorRepository,
                                              XPathValidatorProperties properties,
                                              ThreadPoolTaskExecutor xpathValidatorExecutor,
                                              XsltValidationService xsltValidationService,
                                              FileSystemSchemaRegistryService schemaRegistryService,
                                              XmlProbeService xmlProbeService,
                                              XsdValidationService xsdValidationService,
                                              PathConfigurationProperties pathConfigurationProperties,
                                              XmlFileRepository xmlFileRepository,
                                              XmlFileSessionRepository xmlFileSessionRepository,
                                              ProcessingJobService processingJobService,
                                              CurrentUserService currentUserService) {
        this.requestRepository = requestRepository;
        this.journalRepository = journalRepository;
        this.errorRepository = errorRepository;
        this.properties = properties;
        this.xpathValidatorExecutor = xpathValidatorExecutor;
        this.xsltValidationService = xsltValidationService;
        this.schemaRegistryService = schemaRegistryService;
        this.xmlProbeService = xmlProbeService;
        this.xsdValidationService = xsdValidationService;
        this.pathConfigurationProperties = pathConfigurationProperties;
        this.xmlFileRepository = xmlFileRepository;
        this.xmlFileSessionRepository = xmlFileSessionRepository;
        this.processingJobService = processingJobService;
        this.currentUserService = currentUserService;
    }
    /**
     * Létrehozza és sorba állítja az új XPath-validációs kérést a feltöltött bemenetek és futásidejű konfiguráció alapján.
     * @param file a {@code file} paraméter átadott értéke
     * @param mode a {@code mode} paraméter átadott értéke
     * @param sessionId a {@code sessionId} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     * @throws Exception Hiba esetén dobott kivétel.
     */

    public XPathValidationRequestStatusDto submit(MultipartFile file,
                                                  CreateResultMode mode,
                                                  String sessionId) throws Exception {
        LOGGER.info("XPATH validation submit START. mode={}", mode);
        validateInput(file);
        LOGGER.info("Upload validated.");

        Instant requestTimestampUtc = Instant.now();
        String requestId = generateUniqueRequestId();
        MDC.put("requestId", requestId);

        LOGGER.info("Generated request metadata. requestId={}, timestampUtc={}",
                requestId, requestTimestampUtc);

        IncomingXmlFile incomingXmlFile = writeTempIncomingFile(file, requestId);
        Path tempXml = incomingXmlFile.path();
        LOGGER.info("Temporary incoming XML written. path={}, sizeBytes={}", tempXml, incomingXmlFile.sizeBytes());
        try {
            SchemaBundle resolvedSchemaBundle = resolveSchemaBundleFromUploadedXml(tempXml);
            LOGGER.info("Uploaded XML resolved by shared discovery logic. requestId={}, documentType={}, documentVersion={}, primaryXsd={}, matchReason={}",
                    requestId,
                    resolvedSchemaBundle.getDocumentType(),
                    resolvedSchemaBundle.getDocumentVersion(),
                    resolvedSchemaBundle.getPrimaryXsd(),
                    resolvedSchemaBundle.getMatchReason());

            // A hiányzó XPath szabály üzleti hiba, ezért még az aszinkron kérés
            // rögzítése és worker indítása előtt ellenőrizzük. Így a REST réteg
            // 422 válasszal, tiszta felhasználói üzenettel tud visszatérni, és
            // nem kerül kivétel-stack trace a validation státuszba.
            resolveRulesFilePath(resolvedSchemaBundle.getDocumentType(), resolvedSchemaBundle.getDocumentVersion());

            XPathValidationRequestEntity entity = new XPathValidationRequestEntity();
            Instant now = Instant.now();
            entity.setId(UuidV7Generator.newUuidV7String());
            entity.setRequestId(requestId);
            entity.setRequestTimestampUtc(requestTimestampUtc);
            entity.setFormName(resolvedSchemaBundle.getDocumentType());
            entity.setFormVersion(resolvedSchemaBundle.getDocumentVersion());
            entity.setCreateResultMode(mode);
            entity.setSessionId(sessionId);
            entity.setValidatorStatus(ValidatorStatus.SENT);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setCreatedBy("api");
            entity.setUpdatedBy("api");
            requestRepository.saveAndFlush(entity);
            LOGGER.info("Request row saved. entityId={}, validatorStatus={}, formName={}, formVersion={}",
                    entity.getId(), entity.getValidatorStatus(), entity.getFormName(), entity.getFormVersion());
            saveJournal(entity, null, ValidatorStatus.SENT, null, null, "Kérés rögzítve.");

            XsdPreValidationResult preValidationResult = validateUploadedXmlAgainstXsd(tempXml, entity, resolvedSchemaBundle);
            if (!preValidationResult.valid()) {
                logPayloadPreview(tempXml);
                LOGGER.error("Uploaded XML is not XSD valid. requestId={}, xsdPath={}, issueCount={}",
                        entity.getRequestId(),
                        preValidationResult.schemaBundle() != null && preValidationResult.schemaBundle().getPrimaryXsd() != null ? preValidationResult.schemaBundle().getPrimaryXsd() : null,
                        preValidationResult.issues().size());
                persistXsdErrors(entity, preValidationResult.issues());
                entity.setTechnicalErrorMessage(buildXsdTechnicalMessage(preValidationResult));
                entity.setErrorCount(preValidationResult.issues().size());
                requestRepository.saveAndFlush(entity);
                transition(entity, ValidatorStatus.ABORTED, ResultStatus.ERROR,
                        "XSD validációs hiba miatt a feldolgozás megszakadt.");
                return mapToStatusDto(getRequiredByRequestId(requestId), false, false);
            }

            LOGGER.info("Uploaded XML passed XSD validation. xsdPath={}",
                    preValidationResult.schemaBundle() != null && preValidationResult.schemaBundle().getPrimaryXsd() != null ? preValidationResult.schemaBundle().getPrimaryXsd() : null);
            logPayloadPreview(tempXml);

            Path uploadedXml = moveValidatedIncomingFile(tempXml, requestId);
            LOGGER.info("Validated incoming XML moved to processing location. path={}", uploadedXml);

            try {
                Future<XPathValidationRequestStatusDto> future = xpathValidatorExecutor.submit(() -> processRequest(entity.getId(), uploadedXml));
                LOGGER.info("Validation task submitted to executor.");
                if (mode == CreateResultMode.SYNC) {
                    try {
                        LOGGER.info("SYNC mode active. Waiting up to {} seconds.", properties.getSyncTimeoutSeconds());
                        XPathValidationRequestStatusDto result = future.get(properties.getSyncTimeoutSeconds(), TimeUnit.SECONDS);
                        LOGGER.info("SYNC mode finished within timeout. validatorStatus={}, resultStatus={}, errorCount={}",
                                result.validatorStatus(), result.resultStatus(), result.errorCount());
                        return result;
                    } catch (TimeoutException timeoutException) {
                        LOGGER.warn("SYNC mode timed out after {} seconds. Returning current status.", properties.getSyncTimeoutSeconds());
                        return mapToStatusDto(getRequiredByRequestId(requestId), true, false);
                    }
                }
                LOGGER.info("ASYNC mode accepted. Returning SENT state.");
                return mapToStatusDto(entity, false, false);
            } catch (TaskRejectedException taskRejectedException) {
                LOGGER.error("Validation task rejected because executor queue is full.", taskRejectedException);
                Files.deleteIfExists(uploadedXml);
                throw new QueueCapacityExceededException();
            }
        } finally {
            Files.deleteIfExists(tempXml);
            MDC.remove("requestId");
        }
    }



    /**
     * A {@code submitActiveXmlFile} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>Az XML-adatot a XPath-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param xmlFileSessionId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    public XPathValidationRequestStatusDto submitActiveXmlFile(Long xmlFileId, String xmlFileSessionId) throws Exception {
        if (xmlFileId == null) {
            throw new BadRequestException("Nincs aktív XML állomány az XPath ellenőrzéshez.");
        }
        XmlFileEntity xmlFile = RepositoryAccess.findById(xmlFileRepository, xmlFileId)
                .orElseThrow(() -> new NotFoundException("Nem található XML állomány: " + xmlFileId));
        if (xmlFile.getFilePath() == null || xmlFile.getFilePath().isBlank()) {
            throw new BadRequestException("Az XML állomány fizikai útvonala nem ismert.");
        }
        Path sourceXml = Path.of(xmlFile.getFilePath()).toAbsolutePath().normalize();
        if (!ExceptionSafeOperations.isRegularFile(sourceXml)) {
            throw new BadRequestException("Az XML állomány nem található a fájlrendszerben: " + sourceXml);
        }
        String formName = firstNonBlank(xmlFile.getFormType(), "UNKNOWN");
        String formVersion = firstNonBlank(xmlFile.getFormVersion(), "UNKNOWN");
        // Az XPath ellenőrzés csak akkor indulhat el, ha a űrlaphoz tartozó
        // szabály XML ténylegesen megtalálható. Így nem egy későbbi, félrevezető
        // Saxon paraméterhibával szakad meg az aszinkron feldolgozás.
        resolveRulesFilePath(formName, formVersion);

        XmlFileSessionEntity session = null;
        if (xmlFileSessionId != null && !xmlFileSessionId.isBlank()) {
            session = xmlFileSessionRepository.findBySessionIdAndActiveTrue(xmlFileSessionId)
                    .orElseThrow(() -> new BadRequestException("Nincs aktív XML munkamenet ezzel az azonosítóval: " + xmlFileSessionId));
            if (session.getXmlFile() == null || !xmlFileId.equals(session.getXmlFile().getId())) {
                throw new BadRequestException("Az aktív XML munkamenet nem ehhez az állományhoz tartozik.");
            }
        }
        ProcessingJobDto job = processingJobService.startJob("XPATH_VALIDATION", xmlFileId, "XPath ellenőrzés előkészítése.");
        Instant requestTimestampUtc = Instant.now();
        String requestId = generateUniqueRequestId();
        String username = currentUserService.getCurrentUsername();

        XPathValidationRequestEntity entity = new XPathValidationRequestEntity();
        Instant now = Instant.now();
        entity.setId(UuidV7Generator.newUuidV7String());
        entity.setRequestId(requestId);
        entity.setRequestTimestampUtc(requestTimestampUtc);
        entity.setFormName(formName);
        entity.setFormVersion(formVersion);
        entity.setCreateResultMode(CreateResultMode.ASYNC);
        entity.setSessionId(session != null ? session.getSessionId() : "XML-FILE-" + xmlFileId);
        entity.setXmlFile(xmlFile);
        entity.setXmlFileSessionId(session != null ? session.getSessionId() : null);
        entity.setProcessingJobId(job.jobId());
        entity.setValidatorStatus(ValidatorStatus.SENT);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(username);
        entity.setUpdatedBy(username);
        requestRepository.saveAndFlush(entity);
        saveJournal(entity, null, ValidatorStatus.SENT, null, null, "Aktív XML állományhoz kötött XPath ellenőrzés rögzítve.");

        Path processingXml = copyActiveXmlToProcessingLocation(sourceXml, requestId);
        xpathValidatorExecutor.submit(() -> runActiveXmlValidationJob(entity.getId(), processingXml, job.jobId()));
        return mapToStatusDto(entity, false, false);
    }

    /**
     * A {@code getLatestForXmlFile} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XPath-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a feloldott vagy lekért érték
     */
    public XPathValidationRequestStatusDto getLatestForXmlFile(Long xmlFileId) {
        return requestRepository.findFirstByXmlFileIdOrderByCreatedAtDesc(xmlFileId)
                .map(entity -> mapToStatusDto(entity, false, false))
                .orElseThrow(() -> new NotFoundException("Ehhez az XML állományhoz még nincs XPath ellenőrzési eredmény."));
    }

    /**
     * A {@code runActiveXmlValidationJob} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>Az XML-adatot a XPath-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param entityId a célobjektum vagy erőforrás azonosítója
     * @param processingXml a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param jobId a célobjektum vagy erőforrás azonosítója
     */
    private void runActiveXmlValidationJob(String entityId, Path processingXml, String jobId) {
        try {
            processingJobService.markRunning(jobId, "XPath ellenőrzés fut.");
            XPathValidationRequestStatusDto status = processRequest(entityId, processingXml);
            int errors = status.errorCount() == null ? 0 : status.errorCount();

            if (status.validatorStatus() == ValidatorStatus.FINISHED && status.resultStatus() == ResultStatus.OK) {
                processingJobService.finish(jobId, "XPath ellenőrzés sikeres, nincs hiba.");
            } else if (status.validatorStatus() == ValidatorStatus.FINISHED) {
                processingJobService.finish(jobId, "XPath ellenőrzés befejezve, hibák száma: " + errors);
            } else {
                String message = status.technicalErrorMessage() == null || status.technicalErrorMessage().isBlank()
                        ? "XPath ellenőrzés megszakadt: " + status.validatorStatus()
                        : "XPath ellenőrzés megszakadt: " + firstLine(status.technicalErrorMessage());
                processingJobService.fail(jobId, message);
            }
        } catch (Exception ex) {
            processingJobService.fail(jobId, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }

    /**
     * A {@code copyActiveXmlToProcessingLocation} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a XPath-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param sourceXml a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private Path copyActiveXmlToProcessingLocation(Path sourceXml, String requestId) throws IOException {
        Path incomingDir = ConfiguredPathSupport.toAbsoluteNormalizedPath(
                        requireConfigured(properties.getResultDir(), "result dir"))
                .resolve("active-xml");
        ExceptionSafeOperations.createDirectories(incomingDir);
        Path target = incomingDir.resolve(requestId + "-active.xml");
        SecureFileOperations.copyPrivate(sourceXml, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
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
     * A {@code firstNonBlank} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @param fallback a művelet bemeneti {@code fallback} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * A {@code getStatus} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a feloldott vagy lekért érték
     */
    @Transactional(readOnly = true)
/**
 * Visszaadja a {@code status} mező aktuális értékét.
 * @param requestId a {@code requestId} paraméter átadott értéke
 * @return a {@code status} mező értéke
 */
    public XPathValidationRequestStatusDto getStatus(String requestId) {
        LOGGER.info("Loading validation status for requestId={}", safeForLog(requestId));
        return mapToStatusDto(getRequiredByRequestId(requestId), false, false);
    }

    /**
     * A {@code list} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param limit a lapozási vagy mennyiségi korlátot meghatározó érték
     * @param query a művelet bemeneti {@code query} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional(readOnly = true)
/**
 * A jogosult felhasználó számára lapozható és szűrhető XPath-validációs kéréslistát állít össze.
 * @param limit a {@code limit} paraméter átadott értéke
 * @param query a {@code query} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */
    public XPathValidationListResponseDto list(int limit, String query) {
        int effectiveLimit = switch (limit) {
            case 5, 10, 20, 50, 100 -> limit;
            default -> properties.getDefaultPageSize();
        };
        LOGGER.info("Listing validation requests. requestedLimit={}, effectiveLimit={}, query={}", limit, effectiveLimit, safeForLog(query));
        var pageable = PageRequest.of(0, effectiveLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        var page = StringUtils.hasText(query)
                ? requestRepository.findByRequestIdContainingIgnoreCase(query.trim(), pageable)
                : requestRepository.findAll(pageable);
        List<XPathValidationListItemDto> items = page.getContent().stream().map(entity -> new XPathValidationListItemDto(
                entity.getRequestId(), entity.getRequestTimestampUtc(), entity.getFormName(), entity.getFormVersion(),
                entity.getValidatorStatus(), entity.getResultStatus(), entity.getErrorCount(), entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getResultFilePath() != null)).toList();
        return new XPathValidationListResponseDto(items, effectiveLimit, query);
    }

    /**
     * A {@code getErrors} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    @Transactional(readOnly = true)
/**
 * Visszaadja a {@code errors} mező aktuális értékét.
 * @param requestId a {@code requestId} paraméter átadott értéke
 * @return a {@code errors} mező értéke
 */
    public List<XPathValidationIssueDto> getErrors(String requestId) {
        LOGGER.info("Loading validation errors for requestId={}", safeForLog(requestId));
        getRequiredByRequestId(requestId);
        return errorRepository.findByRequestIdOrderByCreatedAtAsc(requestId).stream()
                .map(error -> new XPathValidationIssueDto(error.getErrorCode(), error.getErrorMessage(), error.getSeverity(), error.getDynamicPageIndex(), error.getElementId(), error.getRuleId(), error.getPath()))
                .toList();
    }

    /**
     * A {@code getJournal} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    @Transactional(readOnly = true)
/**
 * Visszaadja a {@code journal} mező aktuális értékét.
 * @param requestId a {@code requestId} paraméter átadott értéke
 * @return a {@code journal} mező értéke
 */
    public List<XPathValidationJournalDto> getJournal(String requestId) {
        LOGGER.info("Loading validation journal for requestId={}", safeForLog(requestId));
        getRequiredByRequestId(requestId);
        return journalRepository.findByRequestIdOrderByEventTimestampUtcDesc(requestId).stream()
                .map(item -> new XPathValidationJournalDto(item.getEventTimestampUtc(), item.getOldValidatorStatus(), item.getNewValidatorStatus(), item.getOldResultStatus(), item.getNewResultStatus(), item.getMessage()))
                .toList();
    }

    /**
     * A {@code getResultXml} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XPath-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a feloldott vagy lekért érték
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional(readOnly = true)
/**
 * Visszaadja a {@code resultXml} mező aktuális értékét.
 * @param requestId a {@code requestId} paraméter átadott értéke
 * @return a {@code resultXml} mező értéke
 * @throws IOException Hiba esetén dobott kivétel.
 */
    public String getResultXml(String requestId) throws IOException {
        LOGGER.info("Loading result XML for requestId={}", safeForLog(requestId));
        XPathValidationRequestEntity entity = getRequiredByRequestId(requestId);
        if (entity.getResultFilePath() != null) {
            Path path = Path.of(entity.getResultFilePath());
            LOGGER.info("Result XML file path found in DB. path={}", path);
            if (ExceptionSafeOperations.isRegularFile(path)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            }
            LOGGER.warn("Result XML file path stored but file does not exist. path={}", path);
        }
        if (entity.getResult() != null) {
            LOGGER.info("Returning result XML from DB CLOB.");
            return entity.getResult();
        }
        throw new NotFoundException("A megadott kéréshez még nincs eredmény XML.");
    }
    /**
     * Visszaadja a {@code requiredByRequestId} mező aktuális értékét.
     * @param requestId a {@code requestId} paraméter átadott értéke
     * @return a {@code requiredByRequestId} mező értéke
     */

    private XPathValidationRequestEntity getRequiredByRequestId(String requestId) {
        return requestRepository.findByRequestId(requestId).orElseThrow(() -> new NotFoundException("A megadott requestId nem található: " + requestId));
    }

    /**
     * A {@code processRequest} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param entityId a célobjektum vagy erőforrás azonosítója
     * @param uploadedXml a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
/**
 * Végrehajtja egy korábban rögzített XPath-validációs kérés teljes háttérfeldolgozási életciklusát.
 * @param entityId a {@code entityId} paraméter átadott értéke
 * @param uploadedXml a {@code uploadedXml} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */
    protected XPathValidationRequestStatusDto processRequest(String entityId, Path uploadedXml) {
        XPathValidationRequestEntity entity = RepositoryAccess.findById(requestRepository, entityId).orElseThrow();
        MDC.put("sessionId", entity.getSessionId());
        MDC.put("requestId", entity.getRequestId());
        LOGGER.info("Validation processing START. entityId={}, requestId={}", entityId, entity.getRequestId());
        try {
            transition(entity, ValidatorStatus.PROCESSING, entity.getResultStatus(), "Validáció elindult.");

            Path xslPath = resolveXslPath();
            Path rulesFile = resolveRulesFilePath(entity.getFormName(), entity.getFormVersion());
            Path resultFile = resolveResultFile(entity.getRequestId());
            ExceptionSafeOperations.createDirectories(resultFile.getParent());

            LOGGER.info("Resolved validation resources. xslPath={}, rulesFile={}, resultFile={}", xslPath, rulesFile, resultFile);
            LOGGER.info("Launching XSLT validation. xmlPath={}, formName={}, formVersion={}", uploadedXml, entity.getFormName(), entity.getFormVersion());

            XPathValidationExecutionResult executionResult = xsltValidationService.validate(
                    xslPath,
                    uploadedXml,
                    resultFile.getParent().toString(),
                    rulesFile.getParent().toString(),
                    entity.getFormName(),
                    entity.getFormVersion(),
                    rulesFile.toString());

            LOGGER.info("XSLT validation finished. rawXmlLength={}, issueCount={}",
                    executionResult.rawOutputXml() != null ? executionResult.rawOutputXml().length() : 0,
                    executionResult.issues() != null ? executionResult.issues().size() : 0);

            SecureFileOperations.writePrivateString(resultFile, executionResult.rawOutputXml(), StandardCharsets.UTF_8);
            LOGGER.info("Result XML written to disk. path={}", resultFile);

            entity.setResult(executionResult.rawOutputXml());
            entity.setResultFilePath(resultFile.toString());
            entity.setErrorCount(executionResult.issues().size());
            entity.setTechnicalErrorMessage(null);
            entity.setUpdatedAt(Instant.now());
            entity.setUpdatedBy("system");
            entity.setResultStatus(executionResult.issues().isEmpty() ? ResultStatus.OK : ResultStatus.ERROR);
            requestRepository.saveAndFlush(entity);
            LOGGER.info("Request row updated with result metadata. resultStatus={}, errorCount={}", entity.getResultStatus(), entity.getErrorCount());

            errorRepository.deleteByRequestId(entity.getRequestId());
            LOGGER.info("Previous parsed errors deleted for requestId={}", entity.getRequestId());
            Instant now = Instant.now();
            List<XPathValidationErrorEntity> errors = executionResult.issues().stream().map(issue -> mapIssue(entity, issue, now)).toList();
            if (!errors.isEmpty()) {
                errorRepository.saveAll(errors);
                LOGGER.info("Parsed validation errors saved. count={}", errors.size());
            } else {
                LOGGER.info("No validation errors detected in result XML.");
            }

            transition(entity, ValidatorStatus.FINISHED, entity.getResultStatus(), "Validáció befejezve.");
            LOGGER.info("Validation processing FINISHED. validatorStatus={}, resultStatus={}, errorCount={}",
                    entity.getValidatorStatus(), entity.getResultStatus(), entity.getErrorCount());
        } catch (Exception exception) {
            String technicalErrorMessage = exception instanceof MissingXPathRuleException
                    ? exception.getMessage()
                    : stackTraceOf(exception);
            LOGGER.error("Validation processing FAILED.", exception);
            entity.setTechnicalErrorMessage(technicalErrorMessage);
            entity.setErrorCount(entity.getErrorCount() == null ? 0 : entity.getErrorCount());
            requestRepository.saveAndFlush(entity);
            transition(entity, ValidatorStatus.ABORTED, ResultStatus.ERROR,
                    "Validáció megszakadt: " + firstLine(exception.getMessage()));
        } finally {
            try {
                Files.deleteIfExists(uploadedXml);
                LOGGER.info("Temporary upload file deleted. path={}", uploadedXml);
            } catch (IOException ioException) {
                LOGGER.warn("Temporary upload file could not be deleted. path={}, error={}", uploadedXml, ioException.getMessage());
            }
            LOGGER.info("Validation processing END. requestId={}", entity.getRequestId());
            MDC.remove("requestId");
            MDC.remove("sessionId");
        }
        return mapToStatusDto(RepositoryAccess.findById(requestRepository, entityId).orElseThrow(), false, true);
    }
    /**
     * Az ideiglenesen feltöltött XML probe adatai alapján feloldja a hozzá tartozó SchemaBundle-t.
     * @param xmlPath a {@code xmlPath} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     */

    private SchemaBundle resolveSchemaBundleFromUploadedXml(Path xmlPath) {
        Path schemaRoot = toPath(pathConfigurationProperties.getSchemaDir());
        Path commonXsdDir = toPath(pathConfigurationProperties.getCommonXsdDir());
        LOGGER.info("Resolving schema bundle from uploaded XML with shared discovery flow.");
        XmlProbeResult probeResult = xmlProbeService.probe(xmlPath);
        LOGGER.info("XML probe completed. rootDetected={}, schemaLocationPresent={}, noNamespaceSchemaLocationPresent={}",
                probeResult.getRootElementName() != null && !probeResult.getRootElementName().isBlank(),
                probeResult.getSchemaLocation() != null && !probeResult.getSchemaLocation().isBlank(),
                probeResult.getNoNamespaceSchemaLocation() != null && !probeResult.getNoNamespaceSchemaLocation().isBlank());
        SchemaBundle bundle = schemaRegistryService.resolveByXmlProbe(probeResult, schemaRoot, commonXsdDir);
        if (bundle == null || bundle.getPrimaryXsd() == null) {
            throw new BadRequestException("A feltöltött XML alapján nem sikerült XSD-t azonosítani.");
        }
        if (!StringUtils.hasText(bundle.getDocumentType()) || !StringUtils.hasText(bundle.getDocumentVersion())) {
            throw new BadRequestException("A feltöltött XML alapján nem sikerült a űrlap típusát és verzióját beazonosítani.");
        }
        return bundle;
    }
    /**
     * A feltöltött XML-t a feloldott elsődleges és kapcsolódó XSD-k szerint ellenőrzi.
     * @param xmlPath a {@code xmlPath} paraméter átadott értéke
     * @param entity a {@code entity} paraméter átadott értéke
     * @param bundle a {@code bundle} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     */



    private XsdPreValidationResult validateUploadedXmlAgainstXsd(Path xmlPath, XPathValidationRequestEntity entity, SchemaBundle bundle) {
        LOGGER.info("Starting XSD pre-validation. requestId={}, xmlPath={}", entity.getRequestId(), xmlPath);
        Path commonXsdDir = toPath(pathConfigurationProperties.getCommonXsdDir());
        LOGGER.info("Starting shared XSD pre-validation using resolved bundle. requestId={}, formName={}, formVersion={}, primaryXsd={}, commonXsdDir={}",
                entity.getRequestId(),
                entity.getFormName(),
                entity.getFormVersion(),
                bundle != null ? bundle.getPrimaryXsd() : null,
                commonXsdDir);
        ValidationResult validationResult = xsdValidationService.validate(xmlPath, bundle, commonXsdDir);
        LOGGER.info("XSD pre-validation finished. valid={}, issueCount={}", validationResult.isValid(),
                validationResult.getIssues() != null ? validationResult.getIssues().size() : 0);
        return new XsdPreValidationResult(validationResult.isValid(), bundle, validationResult.getIssues());
    }
    /**
     * A feldolgozás során talált XSD-hibákat perzisztens validációs hiba rekordokká alakítja.
     * @param entity a {@code entity} paraméter átadott értéke
     * @param issues a {@code issues} paraméter átadott értéke
     */

    private void persistXsdErrors(XPathValidationRequestEntity entity, List<ValidationIssue> issues) {
        errorRepository.deleteByRequestId(entity.getRequestId());
        Instant now = Instant.now();
        List<XPathValidationErrorEntity> errors = issues.stream()
                .map(issue -> mapXsdIssue(entity, issue, now))
                .toList();
        if (!errors.isEmpty()) {
            errorRepository.saveAll(errors);
            LOGGER.info("Persisted XSD validation errors. count={}", errors.size());
        }
    }
    /**
     * Egy XSD-validációs issue-t a XPath validáció perzisztens hibaentitására képez le.
     * @param entity a {@code entity} paraméter átadott értéke
     * @param issue a {@code issue} paraméter átadott értéke
     * @param now a {@code now} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     */

    private XPathValidationErrorEntity mapXsdIssue(XPathValidationRequestEntity entity, ValidationIssue issue, Instant now) {
        XPathValidationErrorEntity error = new XPathValidationErrorEntity();
        error.setId(UuidV7Generator.newUuidV7String());
        error.setRequestEntityId(entity.getId());
        error.setRequestId(entity.getRequestId());
        error.setErrorCode(issue.getCode());
        error.setErrorMessage(issue.getMessage());
        error.setSeverity(issue.getSeverity() != null ? issue.getSeverity().name() : null);
        error.setPath(issue.getPath());
        error.setCreatedAt(now);
        error.setCreatedBy("system");
        return error;
    }
    /**
     * Korlátozott hosszúságú, diagnosztikai célú payload-előnézetet ír a naplóba érzékeny adatok kontrollja mellett.
     * @param xmlFile a {@code xmlFile} paraméter átadott értéke
     */

    private void logPayloadPreview(Path xmlFile) {
        if (xmlFile == null || !ExceptionSafeOperations.isRegularFile(xmlFile)) {
            LOGGER.info("Uploaded XML payload preview is not available. path={}", xmlFile);
            return;
        }
        try (InputStream inputStream = Files.newInputStream(xmlFile, StandardOpenOption.READ)) {
            byte[] buffer = inputStream.readNBytes(MAX_LOGGED_PAYLOAD_CHARS + 1);
            String payload = new String(buffer, 0, Math.min(buffer.length, MAX_LOGGED_PAYLOAD_CHARS), StandardCharsets.UTF_8);
            String truncated = buffer.length <= MAX_LOGGED_PAYLOAD_CHARS
                    ? payload
                    : payload + "\n...[TRUNCATED]...";
            LOGGER.info("Uploaded XML payload START\n{}\nUploaded XML payload END", truncated);
        } catch (IOException ex) {
            LOGGER.warn("Could not read uploaded XML payload preview. path={}", xmlFile, ex);
        }
    }
    /**
     * Egy XSLT/XPath validációs issue-t perzisztálható hibaentitássá alakít.
     * @param entity a {@code entity} paraméter átadott értéke
     * @param issue a {@code issue} paraméter átadott értéke
     * @param now a {@code now} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     */

    private XPathValidationErrorEntity mapIssue(XPathValidationRequestEntity entity, XPathValidationIssue issue, Instant now) {
        XPathValidationErrorEntity error = new XPathValidationErrorEntity();
        error.setId(UuidV7Generator.newUuidV7String());
        error.setRequestEntityId(entity.getId());
        error.setRequestId(entity.getRequestId());
        error.setErrorCode(issue.errorCode());
        error.setErrorMessage(issue.errorMessage());
        error.setSeverity(issue.severity());
        error.setDynamicPageIndex(issue.dynamicPageIndex());
        error.setElementId(issue.elementId());
        error.setRuleId(issue.ruleId());
        error.setPath(issue.path());
        error.setCreatedAt(now);
        error.setCreatedBy("system");
        return error;
    }
    /**
     * Átvezeti a validációs kérést a következő életciklusállapotba, és rögzíti az állapotváltás metaadatait.
     * @param entity a {@code entity} paraméter átadott értéke
     * @param newValidatorStatus a {@code newValidatorStatus} paraméter átadott értéke
     * @param newResultStatus a {@code newResultStatus} paraméter átadott értéke
     * @param message a {@code message} paraméter átadott értéke
     */

    private void transition(XPathValidationRequestEntity entity, ValidatorStatus newValidatorStatus, ResultStatus newResultStatus, String message) {
        ValidatorStatus oldValidatorStatus = entity.getValidatorStatus();
        ResultStatus oldResultStatus = entity.getResultStatus();
        LOGGER.info("State transition. oldValidatorStatus={}, newValidatorStatus={}, oldResultStatus={}, newResultStatus={}, message={}",
                oldValidatorStatus, newValidatorStatus, oldResultStatus, newResultStatus, message);
        entity.setValidatorStatus(newValidatorStatus);
        entity.setResultStatus(newResultStatus);
        entity.setUpdatedAt(Instant.now());
        entity.setUpdatedBy("system");
        requestRepository.saveAndFlush(entity);
        saveJournal(entity, oldValidatorStatus, newValidatorStatus, oldResultStatus, newResultStatus, message);
    }
    /**
     * Naplóbejegyzést rögzít a validációs kérés egy jelentős életciklus-eseményéről.
     * @param entity a {@code entity} paraméter átadott értéke
     * @param oldValidatorStatus a {@code oldValidatorStatus} paraméter átadott értéke
     * @param newValidatorStatus a {@code newValidatorStatus} paraméter átadott értéke
     * @param oldResultStatus a {@code oldResultStatus} paraméter átadott értéke
     * @param newResultStatus a {@code newResultStatus} paraméter átadott értéke
     * @param message a {@code message} paraméter átadott értéke
     */

    private void saveJournal(XPathValidationRequestEntity entity,
                             ValidatorStatus oldValidatorStatus,
                             ValidatorStatus newValidatorStatus,
                             ResultStatus oldResultStatus,
                             ResultStatus newResultStatus,
                             String message) {
        XPathValidationRequestJournalEntity journal = new XPathValidationRequestJournalEntity();
        journal.setId(UuidV7Generator.newUuidV7String());
        journal.setRequestEntityId(entity.getId());
        journal.setRequestId(entity.getRequestId());
        journal.setEventTimestampUtc(Instant.now());
        journal.setOldValidatorStatus(oldValidatorStatus);
        journal.setNewValidatorStatus(newValidatorStatus);
        journal.setOldResultStatus(oldResultStatus);
        journal.setNewResultStatus(newResultStatus);
        journal.setMessage(message);
        journal.setSessionId(entity.getSessionId());
        journal.setCreatedAt(Instant.now());
        journal.setCreatedBy("system");
        journalRepository.save(journal);
        LOGGER.info("Journal row saved. message={}", message);
    }
    /**
     * A feltöltött tartalmat korlátozott méretű ideiglenes bemeneti fájlba írja.
     * @param file a {@code file} paraméter átadott értéke
     * @param requestId a {@code requestId} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     * @throws IOException Hiba esetén dobott kivétel.
     */

    private IncomingXmlFile writeTempIncomingFile(MultipartFile file, String requestId) throws IOException {
        Path incomingDir = ConfiguredPathSupport.toAbsoluteNormalizedPath(
                        requireConfigured(properties.getResultDir(), "result dir"))
                .resolve("incoming");
        ExceptionSafeOperations.createDirectories(incomingDir);

        Path incomingFile = incomingDir.resolve(requestId + ".xml.tmp").normalize();
        if (!incomingFile.startsWith(incomingDir)) {
            throw new IOException("A feltöltött XML célútvonala kilépne az incoming könyvtárból.");
        }

        long copiedBytes = 0L;
        byte[] buffer = new byte[8192];

        try (InputStream inputStream = file.getInputStream();
             OutputStream outputStream = SecureFileOperations.newPrivateOutputStream(
                     incomingFile,
                     StandardOpenOption.CREATE_NEW,
                     StandardOpenOption.WRITE)) {

            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                copiedBytes += read;
                if (copiedBytes > MAX_UPLOAD_SIZE_BYTES) {
                    throw new BadRequestException("A feltöltött XML maximális mérete 150 MB lehet.");
                }
                outputStream.write(buffer, 0, read);
            }
        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(incomingFile);
            throw ex;
        }

        if (copiedBytes == 0L) {
            Files.deleteIfExists(incomingFile);
            throw new BadRequestException("Az XML fájl feltöltése kötelező.");
        }

        return new IncomingXmlFile(incomingFile, copiedBytes);
    }
    /**
     * A már ellenőrzött ideiglenes bemeneti fájlt a végleges kéréskönyvtárba mozgatja.
     * @param tempFile a {@code tempFile} paraméter átadott értéke
     * @param requestId a {@code requestId} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     * @throws IOException Hiba esetén dobott kivétel.
     */

    private Path moveValidatedIncomingFile(Path tempFile, String requestId) throws IOException {
        Path incomingDir = ConfiguredPathSupport.toAbsoluteNormalizedPath(requireConfigured(properties.getResultDir(), "result dir")).resolve("incoming");
        ExceptionSafeOperations.createDirectories(incomingDir);
        Path incomingFile = incomingDir.resolve(requestId + ".xml.upload");
        return SecureFileOperations.movePrivate(tempFile, incomingFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    /**
     * Feloldja a validációhoz használandó XSL/XSLT szabályfájl útvonalát a konfigurációból.
     * @return a metódus által előállított eredmény
     */

    private Path resolveXslPath() {
        Path path = ConfiguredPathSupport.toAbsoluteNormalizedPath(requireConfigured(properties.getXslRootDir(), "XSL root dir"))
                .resolve(requireConfigured(properties.getFixedXslName(), "fixed xsl name"))
                .toAbsolutePath().normalize();
        if (!ExceptionSafeOperations.isRegularFile(path)) {
            throw new NotFoundException("Az XSL fájl nem található: " + path);
        }
        return path;
    }
    /**
     * Feloldja az XSLT-nek átadandó szabályfájl útvonalát az aktuális kérés és konfiguráció alapján.
     * @param formName a {@code formName} paraméter átadott értéke
     * @param formVersion a {@code formVersion} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     */

    private Path resolveRulesFilePath(String formName, String formVersion) {
        String rawRuleRoot = requireConfigured(properties.getRuleRootDir(), "rule root dir");
        Path normalizedRuleRoot = ConfiguredPathSupport.toAbsoluteNormalizedPath(rawRuleRoot);
        Path path = VersionedArtifactPathResolver.resolveXpathRule(normalizedRuleRoot, formName, formVersion);
        LOGGER.info("[XPATH-VALIDATOR-PATH] rawRuleRoot={} normalizedRuleRoot={} formName={} formVersion={} result={} exists={}",
                rawRuleRoot, normalizedRuleRoot, formName, formVersion, path, ExceptionSafeOperations.isRegularFile(path));
        if (!ExceptionSafeOperations.isRegularFile(path)) {
            throw new MissingXPathRuleException("Az ellenőrzés sikertelen: Az Űrlap állományhoz nincs XPath állomány regisztrálva a rendszerbe.");
        }
        return path;
    }
    /**
     * Meghatározza az XSLT nyers eredményének opcionális célfájlját.
     * @param requestId a {@code requestId} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     */

    private Path resolveResultFile(String requestId) {
        return ConfiguredPathSupport.toAbsoluteNormalizedPath(requireConfigured(properties.getResultDir(), "result dir")).resolve(requestId + ".xml");
    }
    /**
     * Kötelező konfigurációs értéket ellenőriz, és egyértelmű hibát jelez hiányzó beállításnál.
     * @param value a {@code value} paraméter átadott értéke
     * @param label a {@code label} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     */

    private String requireConfigured(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Hiányzó konfiguráció: " + label);
        }
        return value.trim();
    }
    /**
     * Ellenőrzi a validációs kérés kötelező bemeneti mezőit és azok alapvető konzisztenciáját.
     * @param file a {@code file} paraméter átadott értéke
     */

    private void validateInput(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Az XML fájl feltöltése kötelező.");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            throw new BadRequestException("A feltöltött XML maximális mérete 150 MB lehet.");
        }
        String contentType = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        boolean xmlContentType = contentType.isBlank() || contentType.contains("xml") || contentType.equals("application/octet-stream");
        boolean xmlFileName = originalFilename.endsWith(".xml");
        if (!(xmlContentType || xmlFileName)) {
            throw new UnsupportedMediaTypeException("Csak XML content type támogatott.");
        }
    }
    /**
     * A validációs kérés aktuális perzisztens állapotát kliensoldali státusz DTO-vá alakítja.
     * @param entity a {@code entity} paraméter átadott értéke
     * @param timedOut a {@code timedOut} paraméter átadott értéke
     * @param includeResultXml a {@code includeResultXml} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     */

    private XPathValidationRequestStatusDto mapToStatusDto(XPathValidationRequestEntity entity, boolean timedOut, boolean includeResultXml) {
        return new XPathValidationRequestStatusDto(
                entity.getRequestId(), entity.getSessionId(), entity.getRequestTimestampUtc(), entity.getFormName(), entity.getFormVersion(),
                entity.getCreateResultMode(), entity.getValidatorStatus(), entity.getResultStatus(), entity.getErrorCount(),
                entity.getResultFilePath() != null, entity.getResultFilePath() != null ? "/api/xpath-validator/requests/" + entity.getRequestId() + "/result" : null,
                timedOut, entity.getCreatedAt(), entity.getUpdatedAt(), entity.getTechnicalErrorMessage(), includeResultXml ? entity.getResult() : null, entity.getProcessingJobId());
    }
    /**
     * Ütközésellenőrzéssel létrehoz egy új technikai validációs kérésazonosítót.
     * @return a metódus által előállított eredmény
     */

    private String generateUniqueRequestId() {
        String requestId;
        do {
            requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase(Locale.ROOT);
        } while (!REQUEST_ID_PATTERN.matcher(requestId).matches() || requestRepository.existsByRequestId(requestId));
        return requestId;
    }
    /**
     * A konfigurációs szöveget opcionális, normalizált fájlrendszeri útvonallá alakítja.
     * @param value a {@code value} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     */

    private Path toPath(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return ConfiguredPathSupport.toAbsoluteNormalizedPath(value);
    }
    /**
     * Az XSD-validáció technikai hibáiból rövid, naplózható összefoglaló üzenetet készít.
     * @param result a {@code result} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     */

    private String buildXsdTechnicalMessage(XsdPreValidationResult result) {
        String xsdPath = result.schemaBundle() != null && result.schemaBundle().getPrimaryXsd() != null
                ? result.schemaBundle().getPrimaryXsd().toString()
                : "ismeretlen XSD";
        String issueSummary = result.issues().stream()
                .map(issue -> issue.getCode() + ": " + issue.getMessage())
                .limit(10)
                .reduce((a, b) -> a + System.lineSeparator() + b)
                .orElse("ismeretlen XSD hiba");
        return "XSD validáció meghiúsult. XSD=" + xsdPath + System.lineSeparator() + issueSummary;
    }
    /**
     * Egy kivétel stack trace-ét szöveges diagnosztikai reprezentációvá alakítja.
     * @param exception a {@code exception} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     */

    private String stackTraceOf(Exception exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
    /**
     * Egy több soros diagnosztikai szöveg első sorát adja vissza rövid összefoglalóként.
     * @param value a {@code value} paraméter átadott értéke
     * @return a metódus által előállított eredmény
     */

    private String firstLine(String value) {
        if (value == null || value.isBlank()) {
            return "ismeretlen hiba";
        }
        int idx = value.indexOf('\n');
        return idx >= 0 ? value.substring(0, idx) : value;
    }


    /**
     * A web modul XPath-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code IncomingXmlFile} rekord a web modul XPath-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record IncomingXmlFile(Path path, long sizeBytes) {}

    /**
     * A web modul XPath-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code XsdPreValidationResult} rekord a web modul XPath-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record XsdPreValidationResult(boolean valid, SchemaBundle schemaBundle, List<ValidationIssue> issues) {}

    /**
     * A web modul XPath-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code DuplicateRequestIdException} osztály a web modul XPath-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class DuplicateRequestIdException extends RuntimeException {
        /**
         * Létrehozza a {@code DuplicateRequestIdException} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param requestId a célobjektum vagy erőforrás azonosítója
         */
        public DuplicateRequestIdException(String requestId) {
            super("A requestId már létezik: " + requestId);
        }
    }

    /**
     * A web modul XPath-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code QueueCapacityExceededException} osztály a web modul XPath-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class QueueCapacityExceededException extends RuntimeException {
        /**
         * Létrehozza a {@code QueueCapacityExceededException} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         */
        public QueueCapacityExceededException() {
            super("Az aszinkron feldolgozási sor megtelt.");
        }
    }

    /**
     * A web modul XPath-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code MissingXPathRuleException} osztály a web modul XPath-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class MissingXPathRuleException extends RuntimeException {
        /**
         * Létrehozza a {@code MissingXPathRuleException} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param message a művelet bemeneti {@code message} értéke
         */
        public MissingXPathRuleException(String message) {
            super(message);
        }
    }

    /**
     * A web modul XPath-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code NotFoundException} osztály a web modul XPath-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class NotFoundException extends RuntimeException {
        /**
         * Létrehozza a {@code NotFoundException} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param message a művelet bemeneti {@code message} értéke
         */
        public NotFoundException(String message) {
            super(message);
        }
    }

    /**
     * A web modul XPath-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code BadRequestException} osztály a web modul XPath-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class BadRequestException extends RuntimeException {
        /**
         * Létrehozza a {@code BadRequestException} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param message a művelet bemeneti {@code message} értéke
         */
        public BadRequestException(String message) {
            super(message);
        }
    }

    /**
     * A web modul XPath-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code UnsupportedMediaTypeException} osztály a web modul XPath-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class UnsupportedMediaTypeException extends RuntimeException {
        /**
         * Létrehozza a {@code UnsupportedMediaTypeException} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param message a művelet bemeneti {@code message} értéke
         */
        public UnsupportedMediaTypeException(String message) {
            super(message);
        }
    }
}
