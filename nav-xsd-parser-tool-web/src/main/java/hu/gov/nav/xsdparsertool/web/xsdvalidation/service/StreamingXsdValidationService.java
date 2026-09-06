package hu.gov.nav.xsdparsertool.web.xsdvalidation.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.core.xml.SecureXmlParserSupport;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import hu.gov.nav.xsdparsertool.web.processing.dto.ProcessingJobDto;
import hu.gov.nav.xsdparsertool.web.processing.service.ProcessingJobService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileSessionEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileSessionRepository;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.config.XsdValidationProperties;
import hu.gov.nav.xsdparsertool.web.xpath.config.XPathValidatorProperties;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.dto.XsdValidationErrorDto;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.dto.XsdValidationRequestDto;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.dto.XsdValidationResultDto;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.entity.XsdValidationErrorEntity;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.entity.XsdValidationRequestEntity;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.repository.XsdValidationErrorRepository;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.repository.XsdValidationRequestRepository;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code StreamingXsdValidationService} osztály a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class StreamingXsdValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingXsdValidationService.class);
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("^([^:]+):");
    private static final Pattern VALIDATION_ELEMENT_PATTERN = Pattern.compile("element [\'\"]([^\'\"]+)[\'\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALIDATION_FIELD_PATTERN = Pattern.compile("Field_[A-Za-z0-9_]+");

    private final XmlFileRepository xmlFileRepository;
    private final XmlFileSessionRepository xmlFileSessionRepository;
    private final XsdValidationRequestRepository requestRepository;
    private final XsdValidationErrorRepository errorRepository;
    private final ProcessingJobService processingJobService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final XsdValidationProperties properties;
    private final PathConfigurationProperties pathProperties;
    private final XPathValidatorProperties xpathValidatorProperties;
    private final ExecutorService executor;

    /**
     * Létrehozza a {@code StreamingXsdValidationService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param xmlFileRepository a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param xmlFileSessionRepository a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param requestRepository a művelet bemeneti kérésadatait tartalmazó objektum
     * @param errorRepository a művelet bemeneti {@code errorRepository} értéke
     * @param processingJobService a művelet bemeneti {@code processingJobService} értéke
     * @param currentUserService a művelet felhasználói kontextusa vagy felhasználóneve
     * @param auditLogService a művelet bemeneti {@code auditLogService} értéke
     * @param properties a művelethez szükséges konfigurációs adatok
     * @param pathProperties a feldolgozásban részt vevő fájl vagy elérési út
     * @param xpathValidatorProperties a feldolgozásban részt vevő fájl vagy elérési út
     */
    public StreamingXsdValidationService(XmlFileRepository xmlFileRepository,
                                         XmlFileSessionRepository xmlFileSessionRepository,
                                         XsdValidationRequestRepository requestRepository,
                                         XsdValidationErrorRepository errorRepository,
                                         ProcessingJobService processingJobService,
                                         CurrentUserService currentUserService,
                                         AuditLogService auditLogService,
                                         XsdValidationProperties properties,
                                         PathConfigurationProperties pathProperties,
                                         XPathValidatorProperties xpathValidatorProperties) {
        this.xmlFileRepository = xmlFileRepository;
        this.xmlFileSessionRepository = xmlFileSessionRepository;
        this.requestRepository = requestRepository;
        this.errorRepository = errorRepository;
        this.processingJobService = processingJobService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.properties = properties;
        this.pathProperties = pathProperties;
        this.xpathValidatorProperties = xpathValidatorProperties;
        this.executor = Executors.newSingleThreadExecutor(new XsdValidationThreadFactory());
    }

    /**
     * A {@code startValidationForActiveXmlFile} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>Az XML-adatot a XSD-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public ProcessingJobDto startValidationForActiveXmlFile() {
        XmlFileSessionEntity session = requireCurrentBrowserActiveSession("Nincs aktív XML állomány. Nyiss meg egy XML állományt az XML állományok oldalról.");
        return startValidationForXmlFile(session.getXmlFile().getId(), session.getSessionId(), "XSD validáció előkészítése.");
    }

    /**
     * A {@code startValidationForXmlFile} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>Az XML-adatot a XSD-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param sessionId a célobjektum vagy erőforrás azonosítója
     * @param initialMessage a művelet bemeneti {@code initialMessage} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public ProcessingJobDto startValidationForXmlFile(Long xmlFileId, String sessionId, String initialMessage) {
        String username = currentUsername();
        XmlFileEntity xmlFile = requireXmlFile(xmlFileId);
        validateXmlFileCanBeValidated(xmlFile);

        ProcessingJobDto job = processingJobService.startJob("XSD_VALIDATION", xmlFile.getId(), initialMessage == null || initialMessage.isBlank() ? "XSD validáció előkészítése." : initialMessage);
        XsdValidationRequestEntity request = createRequest(xmlFile, sessionId, job.jobId(), username);
        auditLogService.log("XSD_VALIDATION_STARTED", xmlFile.getId(), job.jobId(), null, username, "SUCCESS",
                "XSD validáció elindítva: " + xmlFile.getFileName(), "requestId=" + request.getRequestId());
        submitValidationAfterCommit(request.getRequestId());
        return job;
    }

    /**
     * A {@code getLatestForActiveXmlFile} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XSD-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a feloldott vagy lekért érték
     */
    @Transactional(readOnly = true)
    public XsdValidationResultDto getLatestForActiveXmlFile() {
        XmlFileSessionEntity session = requireCurrentBrowserActiveSession("Nincs aktív XML állomány.");
        XsdValidationRequestEntity request = requestRepository.findFirstByXmlFileIdOrderByCreatedAtDesc(session.getXmlFile().getId())
                .orElseThrow(() -> new IllegalStateException("Ehhez az XML állományhoz még nincs XSD validációs eredmény."));
        return toResult(request);
    }

    /**
     * A {@code getRequest} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a feloldott vagy lekért érték
     */
    @Transactional(readOnly = true)
    public XsdValidationResultDto getRequest(String requestId) {
        return toResult(requireRequest(requestId));
    }

    /**
     * A {@code getErrors} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    @Transactional(readOnly = true)
    public List<XsdValidationErrorDto> getErrors(String requestId) {
        return errorRepository.findByRequestIdOrderByIdAsc(requestId).stream()
                .map(XsdValidationErrorDto::from)
                .toList();
    }

    /**
     * A {@code createRequest} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param sessionId a célobjektum vagy erőforrás azonosítója
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    protected XsdValidationRequestEntity createRequest(XmlFileEntity xmlFile, String sessionId, String jobId, String username) {
        LocalDateTime now = LocalDateTime.now();
        XsdValidationRequestEntity request = new XsdValidationRequestEntity();
        request.setRequestId("XSD-" + UUID.randomUUID());
        request.setXmlFile(xmlFile);
        request.setXmlFileSessionId(sessionId);
        request.setJobId(jobId);
        request.setXsdPath(xmlFile.getXsdPath());
        request.setStatus("PENDING");
        request.setResultStatus("UNKNOWN");
        request.setCreatedAt(now);
        request.setCreatedBy(username);
        request.setUpdatedAt(now);
        request.setUpdatedBy(username);
        return requestRepository.save(request);
    }

    /**
     * A {@code submitValidationAfterCommit} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     */
    private void submitValidationAfterCommit(String requestId) {
        Runnable task = () -> executor.submit(() -> runValidation(requestId));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                /**
                 * A {@code afterCommit} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
                 *
                 * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
                 */
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    /**
     * A {@code runValidation} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     */
    private void runValidation(String requestId) {
        String jobId = null;
        try {
            XsdValidationRequestEntity request = markRequestRunning(requestId);
            jobId = request.getJobId();
            processingJobService.markRunning(jobId, "XSD validáció fut.");
            processingJobService.updateProgress(jobId, 10, "XSD séma betöltése.");
            executeStreamingValidation(requestId);
            XsdValidationRequestEntity finished = finishRequest(requestId);
            if ("VALID".equals(finished.getResultStatus())) {
                processingJobService.finish(jobId, "XSD validáció sikeres. Nincs XSD hiba.");
            } else {
                String suffix = Boolean.TRUE.equals(finished.getMaxErrorsReached()) ? " A hibalimit elérése miatt a validáció megállt." : "";
                processingJobService.finish(jobId, "XSD validáció befejezve: " + finished.getErrorCount() + " hiba." + suffix);
            }
        } catch (ValidationCancelledException e) {
            if (jobId != null) {
                cancelRequest(requestId, e.getMessage());
                processingJobService.cancel(jobId, e.getMessage());
            }
        } catch (Exception e) {
            String message = safeMessage(e);
            if (jobId != null) {
                failRequest(requestId, message);
                processingJobService.fail(jobId, message);
            }
        }
    }

    /**
     * A {@code executeStreamingValidation} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void executeStreamingValidation(String requestId) throws Exception {
        XsdValidationRequestEntity request = requireRequest(requestId);
        Path xmlPath = Path.of(request.getXmlFile().getFilePath()).normalize();
        Path xsdPath = Path.of(request.getXsdPath()).normalize();
        if (!ExceptionSafeOperations.isRegularFile(xmlPath)) {
            throw new IllegalStateException("Az XML fájl nem található: " + xmlPath);
        }
        if (!ExceptionSafeOperations.isRegularFile(xsdPath)) {
            throw new IllegalStateException("Az XSD fájl nem található: " + xsdPath);
        }
        CollectingErrorHandler errorHandler = new CollectingErrorHandler(requestId, Math.max(1, properties.getMaxErrors()));
        boolean persistedIssues = false;
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
            XsdFileResourceResolver resolver = new XsdFileResourceResolver(xsdPath, configuredXsdRoots());
            factory.setResourceResolver(resolver);
            factory.setErrorHandler(errorHandler);
            StreamSource schemaSource = new StreamSource(xsdPath.toFile());
            schemaSource.setSystemId(xsdPath.toUri().toString());
            Schema schema = factory.newSchema(schemaSource);
            Validator validator = schema.newValidator();
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
            validator.setResourceResolver(resolver);
            validator.setErrorHandler(errorHandler);
            processingJobService.updateProgress(request.getJobId(), 25, "XML állomány streaming validációja folyamatban.");
            try (InputStream input = new CancellableInputStream(Files.newInputStream(xmlPath), request.getJobId(), processingJobService)) {
                StreamSource source = new StreamSource(input);
                source.setSystemId(xmlPath.toUri().toString());
                validator.validate(source);
            } catch (XsdValidationLimitReachedException e) {
                // A hibalimit üzleti lezárás, nem technikai hiba.
            }
        } catch (Exception e) {
            if (!errorHandler.getIssues().isEmpty()) {
                persistErrors(requestId, errorHandler.getIssues(), errorHandler.isLimitReached());
                persistedIssues = true;
            }
            throw e;
        }
        if (!persistedIssues) {
            persistErrors(requestId, errorHandler.getIssues(), errorHandler.isLimitReached());
        }
    }

    /**
     * A {@code markRequestRunning} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    protected XsdValidationRequestEntity markRequestRunning(String requestId) {
        XsdValidationRequestEntity request = requireRequest(requestId);
        LocalDateTime now = LocalDateTime.now();
        request.setStatus("RUNNING");
        request.setStartedAt(now);
        request.setUpdatedAt(now);
        request.setUpdatedBy(request.getCreatedBy());
        return requestRepository.save(request);
    }

    /**
     * A {@code persistErrors} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @param issues a feldolgozandó elemek kollekciója
     * @param maxErrorsReached a művelet bemeneti {@code maxErrorsReached} értéke
     */
    @Transactional
    protected void persistErrors(String requestId, List<CollectedXsdIssue> issues, boolean maxErrorsReached) {
        XsdValidationRequestEntity request = requireRequest(requestId);
        int errors = 0;
        int warnings = 0;
        int infos = 0;
        LocalDateTime now = LocalDateTime.now();
        Path xmlPath = request.getXmlFile() == null || request.getXmlFile().getFilePath() == null
                ? null
                : Path.of(request.getXmlFile().getFilePath()).normalize();
        Map<IssueLocationKey, ResolvedXmlLocation> resolvedLocations = resolveXmlLocations(xmlPath, issues);
        for (CollectedXsdIssue issue : issues) {
            if ("INFO".equals(issue.severity())) {
                infos++;
            } else if ("WARNING".equals(issue.severity())) {
                warnings++;
            } else {
                errors++;
            }
            XsdValidationErrorEntity entity = new XsdValidationErrorEntity();
            entity.setRequest(request);
            entity.setRequestId(request.getRequestId());
            entity.setXmlFileId(request.getXmlFile().getId());
            entity.setSeverity(issue.severity());
            entity.setErrorCode(issue.code());
            entity.setErrorMessage(issue.message());
            entity.setLineNumber(issue.lineNumber());
            entity.setColumnNumber(issue.columnNumber());
            ResolvedXmlLocation resolvedLocation = issue.lineNumber() == null
                    ? null
                    : resolvedLocations.get(new IssueLocationKey(issue.lineNumber(), normalizedColumn(issue.columnNumber())));
            String resolvedPath = resolvedLocation == null || resolvedLocation.path() == null || resolvedLocation.path().isBlank()
                    ? issue.path()
                    : resolvedLocation.path();
            entity.setPath(resolvedPath);
            logValidationDiagnostic(request, issue, resolvedLocation);
            entity.setCreatedAt(now);
            entity.setCreatedBy(request.getCreatedBy());
            errorRepository.save(entity);
        }
        request.setErrorCount(errors);
        request.setWarningCount(warnings);
        request.setInfoCount(infos);
        request.setMaxErrorsReached(maxErrorsReached);
        request.setUpdatedAt(now);
        request.setUpdatedBy(request.getCreatedBy());
        requestRepository.save(request);
        processingJobService.updateProgress(request.getJobId(), 90, "XSD validációs eredmények mentése.");
    }

    /**
     * A {@code finishRequest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    protected XsdValidationRequestEntity finishRequest(String requestId) {
        XsdValidationRequestEntity request = requireRequest(requestId);
        LocalDateTime now = LocalDateTime.now();
        request.setStatus("FINISHED");
        request.setResultStatus(request.getErrorCount() != null && request.getErrorCount() > 0 ? "INVALID" : "VALID");
        request.setFinishedAt(now);
        request.setUpdatedAt(now);
        request.setUpdatedBy(request.getCreatedBy());
        XsdValidationRequestEntity saved = requestRepository.save(request);
        auditLogService.log("XSD_VALIDATION_FINISHED", saved.getXmlFile().getId(), saved.getJobId(), null, saved.getCreatedBy(),
                "VALID".equals(saved.getResultStatus()) ? "SUCCESS" : "ERROR",
                "XSD validáció befejezve: " + saved.getResultStatus() + ", hibák: " + saved.getErrorCount(),
                "requestId=" + saved.getRequestId());
        return saved;
    }

    /**
     * A {@code cancelRequest} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @param message a művelet bemeneti {@code message} értéke
     */
    @Transactional
    protected void cancelRequest(String requestId, String message) {
        XsdValidationRequestEntity request = requireRequest(requestId);
        LocalDateTime now = LocalDateTime.now();
        request.setStatus("CANCELLED");
        request.setResultStatus("CANCELLED");
        request.setTechnicalErrorMessage(message);
        request.setFinishedAt(now);
        request.setUpdatedAt(now);
        request.setUpdatedBy(request.getCreatedBy());
        requestRepository.save(request);
    }

    /**
     * A {@code failRequest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @param message a művelet bemeneti {@code message} értéke
     */
    @Transactional
    protected void failRequest(String requestId, String message) {
        XsdValidationRequestEntity request = requireRequest(requestId);
        LocalDateTime now = LocalDateTime.now();
        request.setStatus("FAILED");
        request.setResultStatus("FAILED");
        request.setTechnicalErrorMessage(message);
        request.setFinishedAt(now);
        request.setUpdatedAt(now);
        request.setUpdatedBy(request.getCreatedBy());
        request.setErrorCount(Math.max(1, request.getErrorCount() == null ? 0 : request.getErrorCount()));
        requestRepository.save(request);
        ensureTechnicalErrorRow(request, message);
        auditLogService.log("XSD_VALIDATION_FAILED", request.getXmlFile().getId(), request.getJobId(), null, request.getCreatedBy(), "ERROR",
                message, "requestId=" + request.getRequestId());
    }

    /**
     * A {@code ensureTechnicalErrorRow} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param message a művelet bemeneti {@code message} értéke
     */
    private void ensureTechnicalErrorRow(XsdValidationRequestEntity request, String message) {
        List<XsdValidationErrorEntity> existing = errorRepository.findByRequestIdOrderByIdAsc(request.getRequestId());
        boolean alreadyExists = existing.stream().anyMatch(error -> "SCHEMA".equals(error.getErrorCode()) || "SYSTEM".equals(error.getErrorCode()));
        if (alreadyExists) {
            return;
        }
        String safe = message == null || message.isBlank() ? "XSD validációs technikai hiba." : message;
        XsdValidationErrorEntity entity = new XsdValidationErrorEntity();
        entity.setRequest(request);
        entity.setRequestId(request.getRequestId());
        entity.setXmlFileId(request.getXmlFile() == null ? null : request.getXmlFile().getId());
        entity.setSeverity("ERROR");
        entity.setErrorCode(isSchemaResolutionMessage(safe) ? "SCHEMA" : "SYSTEM");
        entity.setErrorMessage(safe);
        entity.setPath("XSD séma feloldás / validátor előkészítés");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(request.getCreatedBy());
        errorRepository.save(entity);
    }

    /**
     * A {@code isSchemaResolutionMessage} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param message a művelet bemeneti {@code message} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private static boolean isSchemaResolutionMessage(String message) {
        String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
        return lower.contains("src-resolve")
                || lower.contains("cannot resolve")
                || lower.contains("schema_reference")
                || lower.contains("xsd")
                || lower.contains("schema");
    }

    /**
     * A {@code toResult} művelet előállítja a hívó réteg által használt reprezentációt.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    private XsdValidationResultDto toResult(XsdValidationRequestEntity request) {
        return new XsdValidationResultDto(
                XsdValidationRequestDto.from(request),
                errorRepository.findByRequestIdOrderByIdAsc(request.getRequestId()).stream().map(XsdValidationErrorDto::from).toList()
        );
    }

    /**
     * A {@code requireXmlFile} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az XML-adatot a XSD-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    private XmlFileEntity requireXmlFile(Long xmlFileId) {
        return RepositoryAccess.findById(xmlFileRepository, xmlFileId)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány ezzel az azonosítóval: " + xmlFileId));
    }

    /**
     * A {@code requireRequest} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    private XsdValidationRequestEntity requireRequest(String requestId) {
        return requestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XSD validációs kérés: " + requestId));
    }

    /**
     * A {@code configuredXsdRoots} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @return a művelet eredményeként előállított egyedi elemek halmaza
     */
    private Set<Path> configuredXsdRoots() {
        Set<Path> roots = new LinkedHashSet<>();
        addRoot(roots, pathProperties == null ? null : pathProperties.getCommonXsdDir());
        addRoot(roots, pathProperties == null ? null : pathProperties.getSchemaDir());
        addRoot(roots, xpathValidatorProperties == null ? null : xpathValidatorProperties.getRuleRootDir());
        return roots;
    }

    /**
     * A {@code addRoot} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param roots a feldolgozandó elemek kollekciója
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     */
    private static void addRoot(Set<Path> roots, String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        try {
            roots.add(Path.of(path).toAbsolutePath().normalize());
        } catch (Exception ignored) {
            // Invalid external path value: ignore it and keep validation controlled.
        }
    }

    /**
     * A {@code requireCurrentBrowserActiveSession} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param missingMessage a művelet bemeneti {@code missingMessage} értéke
     * @return a művelet feldolgozási eredménye
     */
    private XmlFileSessionEntity requireCurrentBrowserActiveSession(String missingMessage) {
        String username = currentUsername();
        String browserSessionId = currentBrowserSessionId();
        return xmlFileSessionRepository
                .findByCreatedByAndBrowserSessionIdAndActiveTrueOrderByCreatedAtDesc(username, browserSessionId)
                .stream()
                .filter(session -> session.getXmlFile() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(missingMessage));
    }

    /**
     * A {@code currentBrowserSessionId} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    private String currentBrowserSessionId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "NO_HTTP_REQUEST";
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getSession(true).getId();
    }

    /**
     * A {@code validateXmlFileCanBeValidated} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az XML-adatot a XSD-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     */
    private void validateXmlFileCanBeValidated(XmlFileEntity xmlFile) {
        if (Boolean.TRUE.equals(xmlFile.getArchived()) || "ARCHIVED".equalsIgnoreCase(xmlFile.getStatus())) {
            throw new IllegalStateException("Archivált XML állomány nem validálható.");
        }
        if (xmlFile.getFilePath() == null || xmlFile.getFilePath().isBlank()) {
            throw new IllegalStateException("Az XML állomány fizikai útvonala hiányzik.");
        }
        if (xmlFile.getXsdPath() == null || xmlFile.getXsdPath().isBlank()) {
            throw new IllegalStateException("Az XML állományhoz nincs feloldott elsődleges XSD útvonal. Előbb a resolvernek READY/RESOLVED állapotba kell hoznia az állományt.");
        }
    }

    /**
     * A {@code currentUsername} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a művelet feldolgozási eredménye
     */
    private String currentUsername() {
        String username = currentUserService.getCurrentUsername();
        return username == null || username.isBlank() ? "unknown" : username;
    }

    /**
     * A {@code safeMessage} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param e a művelet bemeneti {@code e} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    /**
     * A {@code severityCode} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param severity a művelet bemeneti {@code severity} értéke
     * @param e a művelet bemeneti {@code e} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String severityCode(String severity, SAXParseException e) {
        Matcher matcher = ERROR_CODE_PATTERN.matcher(e.getMessage() == null ? "" : e.getMessage());
        return matcher.find() ? matcher.group(1) : severity;
    }

    /**
     * A {@code locationText} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param e a művelet bemeneti {@code e} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String locationText(SAXParseException e) {
        return "line=" + e.getLineNumber() + ", column=" + e.getColumnNumber();
    }

    /**
     * A {@code logValidationDiagnostic} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param issue a művelet bemeneti {@code issue} értéke
     * @param resolvedLocation a művelet bemeneti {@code resolvedLocation} értéke
     */
    private static void logValidationDiagnostic(XsdValidationRequestEntity request,
                                                CollectedXsdIssue issue,
                                                ResolvedXmlLocation resolvedLocation) {
        Long xmlFileId = request.getXmlFile() == null ? null : request.getXmlFile().getId();
        LOGGER.error("XSD_VALIDATION_RAW requestId={} xmlFileId={} severity={} code={} line={} column={} message={}",
                request.getRequestId(), xmlFileId, issue.severity(), issue.code(), issue.lineNumber(),
                issue.columnNumber(), issue.message());
        LOGGER.error("XSD_VALIDATION_NODE_RESOLUTION requestId={} xmlFileId={} resolvedPath={} element={} value={} depth={} exactElementMatch={} rawLocation={}",
                request.getRequestId(), xmlFileId,
                resolvedLocation == null ? null : resolvedLocation.path(),
                resolvedLocation == null ? null : resolvedLocation.elementName(),
                resolvedLocation == null ? null : resolvedLocation.value(),
                resolvedLocation == null ? null : resolvedLocation.depth(),
                resolvedLocation == null ? null : resolvedLocation.exactElementMatch(),
                issue.path());
    }

    /**
     * A {@code resolveXmlLocations} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>Az XML-adatot a XSD-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlPath a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param issues a feldolgozandó elemek kollekciója
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    private static Map<IssueLocationKey, ResolvedXmlLocation> resolveXmlLocations(Path xmlPath, List<CollectedXsdIssue> issues) {
        Map<IssueLocationKey, ResolvedXmlLocation> result = new LinkedHashMap<>();
        if (xmlPath == null || !ExceptionSafeOperations.isRegularFile(xmlPath) || issues == null || issues.isEmpty()) {
            return result;
        }
        Map<IssueLocationKey, String> targets = new LinkedHashMap<>();
        for (CollectedXsdIssue issue : issues) {
            if (issue.lineNumber() != null && issue.lineNumber() > 0) {
                IssueLocationKey key = new IssueLocationKey(issue.lineNumber(), normalizedColumn(issue.columnNumber()));
                String expectedElement = extractValidationElementName(issue.message());
                targets.merge(key, expectedElement, (left, right) -> left == null || left.isBlank() ? right : left);
            }
        }
        if (targets.isEmpty()) {
            return result;
        }
        try (InputStream input = Files.newInputStream(xmlPath)) {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            SecureXmlParserSupport.configureSecureXmlInputFactory(factory);
            XMLStreamReader reader = factory.createXMLStreamReader(input);
            Deque<XmlPathFrame> stack = new ArrayDeque<>();
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    XmlPathFrame parent = stack.peekLast();
                    String localName = reader.getLocalName();
                    int index = parent == null ? 1 : parent.nextChildIndex(localName);
                    stack.addLast(new XmlPathFrame(localName, index,
                            Math.max(1, reader.getLocation().getLineNumber()),
                            Math.max(1, reader.getLocation().getColumnNumber())));
                } else if ((event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) && !stack.isEmpty()) {
                    stack.peekLast().appendText(reader.getText());
                } else if (event == XMLStreamConstants.END_ELEMENT && !stack.isEmpty()) {
                    XmlPathFrame frame = stack.peekLast();
                    frame.close(Math.max(1, reader.getLocation().getLineNumber()),
                            Math.max(1, reader.getLocation().getColumnNumber()));
                    ResolvedXmlLocation candidate = snapshotLocation(stack);
                    for (Map.Entry<IssueLocationKey, String> target : targets.entrySet()) {
                        IssueLocationKey key = target.getKey();
                        String expectedElement = target.getValue();
                        boolean exactElement = expectedElement != null && expectedElement.equals(frame.name());
                        if (!frame.contains(key.line(), key.column()) && !(exactElement && frame.touchesLine(key.line()))) {
                            continue;
                        }
                        ResolvedXmlLocation current = result.get(key);
                        ResolvedXmlLocation scored = candidate.withExactElementMatch(exactElement);
                        if (current == null || scored.isBetterThan(current)) {
                            result.put(key, scored);
                        }
                    }
                    stack.removeLast();
                }
            }
            reader.close();
        } catch (Exception e) {
            LOGGER.warn("XSD_VALIDATION_NODE_RESOLUTION_FAILED xmlPath={} message={}", xmlPath, e.getMessage());
        }
        return result;
    }

    /**
     * A {@code normalizedColumn} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param column a művelet bemeneti {@code column} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static int normalizedColumn(Integer column) {
        return column == null || column < 1 ? 1 : column;
    }

    /**
     * A {@code extractValidationElementName} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static String extractValidationElementName(String message) {
        String safe = message == null ? "" : message;
        Matcher elementMatcher = VALIDATION_ELEMENT_PATTERN.matcher(safe);
        if (elementMatcher.find()) {
            return elementMatcher.group(1);
        }
        Matcher fieldMatcher = VALIDATION_FIELD_PATTERN.matcher(safe);
        return fieldMatcher.find() ? fieldMatcher.group() : "";
    }

    /**
     * A {@code snapshotLocation} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param stack a művelet bemeneti {@code stack} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static ResolvedXmlLocation snapshotLocation(Deque<XmlPathFrame> stack) {
        StringBuilder path = new StringBuilder();
        XmlPathFrame last = null;
        for (XmlPathFrame frame : stack) {
            path.append('/').append(frame.name()).append('[').append(frame.index()).append(']');
            last = frame;
        }
        String value = last == null ? null : last.text().trim();
        if (value != null && value.length() > 200) {
            value = value.substring(0, 200) + "...";
        }
        return new ResolvedXmlLocation(path.toString(), last == null ? null : last.name(), value,
                stack.size(), false);
    }

    /**
     * A web modul XSD-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code IssueLocationKey} rekord a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record IssueLocationKey(int line, int column) {
    }

    /**
     * A web modul XSD-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code ResolvedXmlLocation} rekord a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record ResolvedXmlLocation(String path,
                                       String elementName,
                                       String value,
                                       int depth,
                                       boolean exactElementMatch) {
        /**
         * A {@code withExactElementMatch} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param exact a művelet bemeneti {@code exact} értéke
         * @return a művelet feldolgozási eredménye
         */
        private ResolvedXmlLocation withExactElementMatch(boolean exact) {
            return new ResolvedXmlLocation(path, elementName, value, depth, exact);
        }

        /**
         * A {@code isBetterThan} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param other a művelet bemeneti {@code other} értéke
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        private boolean isBetterThan(ResolvedXmlLocation other) {
            if (exactElementMatch != other.exactElementMatch) {
                return exactElementMatch;
            }
            return depth > other.depth;
        }
    }

    /**
     * A web modul XSD-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code XmlPathFrame} osztály a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private static final class XmlPathFrame {
        private final String name;
        private final int index;
        private final int startLine;
        private final int startColumn;
        private int endLine;
        private int endColumn;
        private final Map<String, Integer> childCounters = new HashMap<>();
        private final StringBuilder text = new StringBuilder();

        /**
         * Létrehozza a {@code XmlPathFrame} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param name a feloldáshoz vagy azonosításhoz használt név
         * @param index a művelet bemeneti {@code index} értéke
         * @param startLine a művelet bemeneti {@code startLine} értéke
         * @param startColumn a művelet bemeneti {@code startColumn} értéke
         */
        private XmlPathFrame(String name, int index, int startLine, int startColumn) {
            this.name = name;
            this.index = index;
            this.startLine = startLine;
            this.startColumn = startColumn;
            this.endLine = startLine;
            this.endColumn = startColumn;
        }

        /**
         * A {@code close} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param line a művelet bemeneti {@code line} értéke
         * @param column a művelet bemeneti {@code column} értéke
         */
        private void close(int line, int column) {
            this.endLine = line;
            this.endColumn = column;
        }

        /**
         * A {@code contains} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param line a művelet bemeneti {@code line} értéke
         * @param column a művelet bemeneti {@code column} értéke
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        private boolean contains(int line, int column) {
            if (line < startLine || line > endLine) return false;
            if (line == startLine && line == endLine) return column >= startColumn && column <= endColumn;
            if (line == startLine) return column >= startColumn;
            if (line == endLine) return column <= endColumn;
            return true;
        }

        /**
         * A {@code touchesLine} művelet előállítja a hívó réteg által használt reprezentációt.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param line a művelet bemeneti {@code line} értéke
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        private boolean touchesLine(int line) {
            return line >= startLine && line <= endLine;
        }

        /**
         * A {@code name} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a művelet feldolgozási eredménye
         */
        private String name() { return name; }
        /**
         * A {@code index} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a művelet feldolgozási eredménye
         */
        private int index() { return index; }
        /**
         * A {@code text} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a művelet feldolgozási eredménye
         */
        private String text() { return text.toString(); }
        /**
         * A {@code nextChildIndex} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param childName a feloldáshoz vagy azonosításhoz használt név
         * @return a művelet feldolgozási eredménye
         */
        private int nextChildIndex(String childName) {
            int next = childCounters.getOrDefault(childName, 0) + 1;
            childCounters.put(childName, next);
            return next;
        }
        /**
         * A {@code appendText} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param value a művelet bemeneti {@code value} értéke
         */
        private void appendText(String value) {
            if (value != null && text.length() < 500) {
                text.append(value);
            }
        }
    }

    /**
     * A web modul XSD-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code CollectedXsdIssue} rekord a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record CollectedXsdIssue(String severity, String code, String message, Integer lineNumber, Integer columnNumber, String path) {
    }

    /**
     * A web modul XSD-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code CollectingErrorHandler} osztály a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private static class CollectingErrorHandler extends DefaultHandler {
        private final String requestId;
        private final int maxErrors;
        private final List<CollectedXsdIssue> issues = new ArrayList<>();
        private int nonInfoErrors = 0;
        private boolean limitReached;

        /**
         * Létrehozza a {@code CollectingErrorHandler} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param requestId a célobjektum vagy erőforrás azonosítója
         * @param maxErrors a művelet bemeneti {@code maxErrors} értéke
         */
        CollectingErrorHandler(String requestId, int maxErrors) {
            this.requestId = requestId;
            this.maxErrors = maxErrors;
        }

        /**
         * A {@code warning} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param e a művelet bemeneti {@code e} értéke
         * @throws SAXException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
         */
        @Override
        public void warning(SAXParseException e) throws SAXException {
            add("WARNING", e);
        }

        /**
         * A {@code error} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param e a művelet bemeneti {@code e} értéke
         * @throws SAXException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
         */
        @Override
        public void error(SAXParseException e) throws SAXException {
            add("ERROR", e);
        }

        /**
         * A {@code fatalError} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param e a művelet bemeneti {@code e} értéke
         * @throws SAXException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
         */
        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            add("CRITICAL", e);
            throw e;
        }

        /**
         * A {@code add} művelet létrehozza vagy tartósítja a kért állapotváltozást.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param severity a művelet bemeneti {@code severity} értéke
         * @param e a művelet bemeneti {@code e} értéke
         * @throws SAXException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
         */
        private void add(String severity, SAXParseException e) throws SAXException {
            String normalized = severity == null ? "ERROR" : severity.toUpperCase(Locale.ROOT);
            issues.add(new CollectedXsdIssue(normalized, severityCode(normalized, e), e.getMessage(), e.getLineNumber(), e.getColumnNumber(), locationText(e)));
            if (!"INFO".equals(normalized)) {
                nonInfoErrors++;
            }
            if (nonInfoErrors >= maxErrors) {
                limitReached = true;
                throw new XsdValidationLimitReachedException("XSD hibalimit elérve. requestId=" + requestId + ", limit=" + maxErrors);
            }
        }

        /**
         * A {@code getIssues} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a művelet eredményeként előállított elemek listája
         */
        List<CollectedXsdIssue> getIssues() { return issues; }
        /**
         * A {@code isLimitReached} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        boolean isLimitReached() { return limitReached; }
    }

    /**
     * A web modul XSD-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code XsdValidationLimitReachedException} osztály a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private static class XsdValidationLimitReachedException extends SAXException {
        /**
         * Létrehozza a {@code XsdValidationLimitReachedException} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param message a művelet bemeneti {@code message} értéke
         */
        XsdValidationLimitReachedException(String message) { super(message); }
    }

    /**
     * A web modul XSD-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code ValidationCancelledException} osztály a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private static class ValidationCancelledException extends IOException {
        /**
         * Létrehozza a {@code ValidationCancelledException} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param message a művelet bemeneti {@code message} értéke
         */
        ValidationCancelledException(String message) { super(message); }
    }

    /**
     * A web modul XSD-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code CancellableInputStream} osztály a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private static class CancellableInputStream extends FilterInputStream {
        private final String jobId;
        private final ProcessingJobService processingJobService;
        private long reads;

        /**
         * Létrehozza a {@code CancellableInputStream} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param delegate a művelet bemeneti {@code delegate} értéke
         * @param jobId a célobjektum vagy erőforrás azonosítója
         * @param processingJobService a művelet bemeneti {@code processingJobService} értéke
         */
        CancellableInputStream(InputStream delegate, String jobId, ProcessingJobService processingJobService) {
            super(delegate);
            this.jobId = jobId;
            this.processingJobService = processingJobService;
        }

        /**
         * A {@code read} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
         */
        @Override
        public int read() throws IOException {
            checkCancel();
            return super.read();
        }

        /**
         * A {@code read} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param b a művelet bemeneti {@code b} értéke
         * @param off a művelet bemeneti {@code off} értéke
         * @param len a művelet bemeneti {@code len} értéke
         * @return a feloldott vagy lekért érték
         * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
         */
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            checkCancel();
            return super.read(b, off, len);
        }

        /**
         * A {@code checkCancel} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
         */
        private void checkCancel() throws IOException {
            reads++;
            if (reads % 128 == 0 && processingJobService.isCancelRequested(jobId)) {
                throw new ValidationCancelledException("XSD validáció felhasználói kérésre megszakítva.");
            }
        }
    }

    /**
     * A konfigurációs, séma- vagy erőforrás-hivatkozások determinisztikus feloldását végző komponens.
     *
     * <p>A {@code XsdFileResourceResolver} osztály a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private static class XsdFileResourceResolver implements LSResourceResolver {
        private final Path primaryXsd;
        private final Path primaryDirectory;
        private final Set<Path> searchRoots;

        /**
         * Létrehozza a {@code XsdFileResourceResolver} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param primaryXsd a művelet bemeneti {@code primaryXsd} értéke
         * @param configuredRoots a művelethez szükséges konfigurációs adatok
         */
        XsdFileResourceResolver(Path primaryXsd, Set<Path> configuredRoots) {
            this.primaryXsd = primaryXsd.toAbsolutePath().normalize();
            this.primaryDirectory = this.primaryXsd.getParent();
            this.searchRoots = new LinkedHashSet<>();
            addSearchRoot(this.searchRoots, this.primaryDirectory);
            if (configuredRoots != null) {
                configuredRoots.forEach(root -> addSearchRoot(this.searchRoots, root));
            }
        }

        /**
         * A {@code resolveResource} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param type a művelet bemeneti {@code type} értéke
         * @param namespaceURI a feloldáshoz vagy azonosításhoz használt név
         * @param publicId a célobjektum vagy erőforrás azonosítója
         * @param systemId a célobjektum vagy erőforrás azonosítója
         * @param baseURI a művelet bemeneti {@code baseURI} értéke
         * @return a feloldott vagy lekért érték
         */
        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
            try {
                Path resolved = resolvePath(systemId, baseURI, namespaceURI);
                if (resolved == null || !ExceptionSafeOperations.isRegularFile(resolved)) {
                    return null;
                }
                InputStream stream = Files.newInputStream(resolved);
                return new SimpleLsInput(publicId, resolved.toUri().toString(), stream);
            } catch (IOException | IllegalArgumentException e) {
                return null;
            }
        }

        /**
         * A {@code resolvePath} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
         *
         * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
         * @param systemId a célobjektum vagy erőforrás azonosítója
         * @param baseURI a művelet bemeneti {@code baseURI} értéke
         * @param namespaceURI a feloldáshoz vagy azonosításhoz használt név
         * @return a feloldott vagy lekért érték
         */
        private Path resolvePath(String systemId, String baseURI, String namespaceURI) {
            String trimmedSystemId = systemId == null ? "" : systemId.trim().replace('\\', '/');

            if (!trimmedSystemId.isBlank()) {
                Path direct = pathFromUriOrFileName(trimmedSystemId);
                if (isRegularFile(direct)) {
                    return direct.toAbsolutePath().normalize();
                }

                Path fromBase = resolveAgainstBase(trimmedSystemId, baseURI);
                if (isRegularFile(fromBase)) {
                    return fromBase;
                }

                for (Path root : searchRoots) {
                    Path fromRoot = resolveAgainstRoot(root, trimmedSystemId);
                    if (isRegularFile(fromRoot)) {
                        return fromRoot;
                    }
                }

                String fileName = extractFileName(trimmedSystemId);
                for (Path root : searchRoots) {
                    Path found = findByFileName(root, fileName);
                    if (isRegularFile(found)) {
                        return found;
                    }
                }
            }

            if (namespaceURI != null && !namespaceURI.isBlank()) {
                for (Path root : searchRoots) {
                    Path byNamespace = findByTargetNamespace(root, namespaceURI);
                    if (isRegularFile(byNamespace)) {
                        return byNamespace;
                    }
                }
            }

            return null;
        }

        /**
         * A {@code resolveAgainstBase} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param systemId a célobjektum vagy erőforrás azonosítója
         * @param baseURI a művelet bemeneti {@code baseURI} értéke
         * @return a feloldott vagy lekért érték
         */
        private Path resolveAgainstBase(String systemId, String baseURI) {
            if (baseURI == null || baseURI.isBlank()) {
                return null;
            }
            try {
                URI base = URI.create(baseURI);
                URI resolvedUri = base.resolve(systemId);
                if ("file".equalsIgnoreCase(resolvedUri.getScheme())) {
                    return Path.of(resolvedUri).toAbsolutePath().normalize();
                }
            } catch (IllegalArgumentException ignored) {
                // Fallback below.
            }

            Path basePath = pathFromUriOrFileName(baseURI);
            if (basePath != null) {
                Path baseDirectory = ExceptionSafeOperations.isDirectory(basePath) ? basePath : basePath.getParent();
                if (baseDirectory != null) {
                    return baseDirectory.resolve(systemId).toAbsolutePath().normalize();
                }
            }
            return null;
        }

        /**
         * A {@code resolveAgainstRoot} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param root a művelet bemeneti {@code root} értéke
         * @param systemId a célobjektum vagy erőforrás azonosítója
         * @return a feloldott vagy lekért érték
         */
        private Path resolveAgainstRoot(Path root, String systemId) {
            if (root == null || systemId == null || systemId.isBlank()) {
                return null;
            }
            try {
                return root.resolve(systemId).toAbsolutePath().normalize();
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * A {@code findByFileName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
         * @param root a művelet bemeneti {@code root} értéke
         * @param fileName a feldolgozásban részt vevő fájl vagy elérési út
         * @return a feloldott vagy lekért érték
         */
        private Path findByFileName(Path root, String fileName) {
            if (root == null || fileName == null || fileName.isBlank() || !ExceptionSafeOperations.isDirectory(root)) {
                return null;
            }
            try (Stream<Path> paths = Files.walk(root)) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().equalsIgnoreCase(fileName))
                        .findFirst()
                        .orElse(null);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * A {@code findByTargetNamespace} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param root a művelet bemeneti {@code root} értéke
         * @param namespaceURI a feloldáshoz vagy azonosításhoz használt név
         * @return a feloldott vagy lekért érték
         */
        private Path findByTargetNamespace(Path root, String namespaceURI) {
            if (root == null || namespaceURI == null || namespaceURI.isBlank() || !ExceptionSafeOperations.isDirectory(root)) {
                return null;
            }
            try (Stream<Path> paths = Files.walk(root)) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xsd"))
                        .filter(path -> hasTargetNamespace(path, namespaceURI))
                        .findFirst()
                        .orElse(null);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * A {@code hasTargetNamespace} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param path a feldolgozásban részt vevő fájl vagy elérési út
         * @param namespaceURI a feloldáshoz vagy azonosításhoz használt név
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        private boolean hasTargetNamespace(Path path, String namespaceURI) {
            try {
                String content = Files.readString(path);
                return content.contains("targetNamespace=\"" + namespaceURI + "\"")
                        || content.contains("targetNamespace='" + namespaceURI + "'");
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * A {@code isRegularFile} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
         * @param path a feldolgozásban részt vevő fájl vagy elérési út
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        private static boolean isRegularFile(Path path) {
            return path != null && ExceptionSafeOperations.isRegularFile(path);
        }

        /**
         * A {@code addSearchRoot} művelet létrehozza vagy tartósítja a kért állapotváltozást.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param roots a feldolgozandó elemek kollekciója
         * @param root a művelet bemeneti {@code root} értéke
         */
        private static void addSearchRoot(Set<Path> roots, Path root) {
            if (root != null) {
                roots.add(root.toAbsolutePath().normalize());
            }
        }

        /**
         * A {@code pathFromUriOrFileName} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
         * @param value a művelet bemeneti {@code value} értéke
         * @return a művelet feldolgozási eredménye
         */
        private Path pathFromUriOrFileName(String value) {
            try {
                URI uri = URI.create(value);
                if ("file".equalsIgnoreCase(uri.getScheme())) {
                    return Path.of(uri).toAbsolutePath().normalize();
                }
                if (uri.getScheme() != null) {
                    return null;
                }
            } catch (IllegalArgumentException ignored) {
                // Treat as file path below.
            }
            try {
                return Path.of(value).toAbsolutePath().normalize();
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        /**
         * A {@code extractFileName} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
         *
         * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
         * @param systemId a célobjektum vagy erőforrás azonosítója
         * @return a művelet feldolgozási eredménye
         */
        private String extractFileName(String systemId) {
            try {
                URI uri = URI.create(systemId);
                if (uri.getPath() != null && !uri.getPath().isBlank()) {
                    String p = uri.getPath().replace('\\', '/');
                    int idx = p.lastIndexOf('/');
                    return idx >= 0 ? p.substring(idx + 1) : p;
                }
            } catch (Exception ignored) {
                // Fallback below.
            }
            String normalized = systemId.replace('\\', '/');
            int idx = normalized.lastIndexOf('/');
            return idx >= 0 ? normalized.substring(idx + 1) : normalized;
        }
    }

    /**
     * A web modul XSD-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code SimpleLsInput} osztály a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private static class SimpleLsInput implements LSInput {
        private String publicId;
        private String systemId;
        private String baseURI;
        private InputStream byteStream;
        private Reader characterStream;
        private String stringData;
        private String encoding;
        private boolean certifiedText;

        /**
         * Létrehozza a {@code SimpleLsInput} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param publicId a célobjektum vagy erőforrás azonosítója
         * @param systemId a célobjektum vagy erőforrás azonosítója
         * @param byteStream a művelet bemeneti {@code byteStream} értéke
         */
        SimpleLsInput(String publicId, String systemId, InputStream byteStream) {
            this.publicId = publicId;
            this.systemId = systemId;
            this.baseURI = systemId;
            this.byteStream = byteStream;
        }

        /**
         * A {@code getCharacterStream} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        @Override public Reader getCharacterStream() { return characterStream; }
        /**
         * A {@code setCharacterStream} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param characterStream a művelet bemeneti {@code characterStream} értéke
         */
        @Override public void setCharacterStream(Reader characterStream) { this.characterStream = characterStream; }
        /**
         * A {@code getByteStream} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        @Override public InputStream getByteStream() { return byteStream; }
        /**
         * A {@code setByteStream} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param byteStream a művelet bemeneti {@code byteStream} értéke
         */
        @Override public void setByteStream(InputStream byteStream) { this.byteStream = byteStream; }
        /**
         * A {@code getStringData} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        @Override public String getStringData() { return stringData; }
        /**
         * A {@code setStringData} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param stringData a művelet bemeneti {@code stringData} értéke
         */
        @Override public void setStringData(String stringData) { this.stringData = stringData; }
        /**
         * A {@code getSystemId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        @Override public String getSystemId() { return systemId; }
        /**
         * A {@code setSystemId} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param systemId a célobjektum vagy erőforrás azonosítója
         */
        @Override public void setSystemId(String systemId) { this.systemId = systemId; }
        /**
         * A {@code getPublicId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        @Override public String getPublicId() { return publicId; }
        /**
         * A {@code setPublicId} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param publicId a célobjektum vagy erőforrás azonosítója
         */
        @Override public void setPublicId(String publicId) { this.publicId = publicId; }
        /**
         * A {@code getBaseURI} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        @Override public String getBaseURI() { return baseURI; }
        /**
         * A {@code setBaseURI} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param baseURI a művelet bemeneti {@code baseURI} értéke
         */
        @Override public void setBaseURI(String baseURI) { this.baseURI = baseURI; }
        /**
         * A {@code getEncoding} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        @Override public String getEncoding() { return encoding; }
        /**
         * A {@code setEncoding} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param encoding a művelet bemeneti {@code encoding} értéke
         */
        @Override public void setEncoding(String encoding) { this.encoding = encoding; }
        /**
         * A {@code getCertifiedText} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        @Override public boolean getCertifiedText() { return certifiedText; }
        /**
         * A {@code setCertifiedText} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param certifiedText a művelet bemeneti {@code certifiedText} értéke
         */
        @Override public void setCertifiedText(boolean certifiedText) { this.certifiedText = certifiedText; }
    }

    /**
     * A web modul XSD-validációs területének közös alkalmazási típusa.
     *
     * <p>A {@code XsdValidationThreadFactory} osztály a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private static class XsdValidationThreadFactory implements ThreadFactory {
        /**
         * A {@code newThread} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param runnable a művelet bemeneti {@code runnable} értéke
         * @return a művelet feldolgozási eredménye
         */
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "nav-xsd-validation-job");
            thread.setDaemon(true);
            return thread;
        }
    }
}
