package hu.gov.nav.xsdparsertool.web.xsdvalidation.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

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
import hu.gov.nav.xsdparsertool.web.xsdvalidation.dto.XsdValidationResultDto;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.entity.XsdValidationErrorEntity;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.entity.XsdValidationRequestEntity;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.repository.XsdValidationErrorRepository;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.repository.XsdValidationRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StreamingXsdValidationServiceCoverageTest {

    private XmlFileRepository xmlFileRepository;
    private XmlFileSessionRepository sessionRepository;
    private XsdValidationRequestRepository requestRepository;
    private XsdValidationErrorRepository errorRepository;
    private ProcessingJobService processingJobService;
    private CurrentUserService currentUserService;
    private AuditLogService auditLogService;
    private StreamingXsdValidationService service;

    @BeforeEach
    void setUp() {
        xmlFileRepository = mock(XmlFileRepository.class);
        sessionRepository = mock(XmlFileSessionRepository.class);
        requestRepository = mock(XsdValidationRequestRepository.class);
        errorRepository = mock(XsdValidationErrorRepository.class);
        processingJobService = mock(ProcessingJobService.class);
        currentUserService = mock(CurrentUserService.class);
        auditLogService = mock(AuditLogService.class);
        XsdValidationProperties properties = new XsdValidationProperties();
        PathConfigurationProperties paths = new PathConfigurationProperties();
        service = new StreamingXsdValidationService(
                xmlFileRepository, sessionRepository, requestRepository, errorRepository,
                processingJobService, currentUserService, auditLogService, properties, paths, new XPathValidatorProperties());
    }

    @AfterEach
    void cleanSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createRequestInitializesPersistentState() {
        XmlFileEntity xml = xmlFile(7L, "/tmp/a.xml", "/tmp/a.xsd");
        when(requestRepository.save(any(XsdValidationRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        XsdValidationRequestEntity request = service.createRequest(xml, "session-1", "job-1", "tester");

        assertNotNull(request.getRequestId());
        assertTrue(request.getRequestId().startsWith("XSD-"));
        assertSame(xml, request.getXmlFile());
        assertEquals("session-1", request.getXmlFileSessionId());
        assertEquals("job-1", request.getJobId());
        assertEquals("/tmp/a.xsd", request.getXsdPath());
        assertEquals("PENDING", request.getStatus());
        assertEquals("UNKNOWN", request.getResultStatus());
        assertEquals("tester", request.getCreatedBy());
        verify(requestRepository).save(request);
    }

    @Test
    void startValidationRejectsMissingArchivedAndUnresolvedXmlFiles() {
        when(RepositoryAccess.findById(xmlFileRepository, 1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.startValidationForXmlFile(1L, null, null));

        XmlFileEntity archived = xmlFile(2L, "/tmp/a.xml", "/tmp/a.xsd");
        archived.setArchived(true);
        when(RepositoryAccess.findById(xmlFileRepository, 2L)).thenReturn(Optional.of(archived));
        assertThrows(IllegalStateException.class, () -> service.startValidationForXmlFile(2L, null, null));

        XmlFileEntity missingPath = xmlFile(3L, null, "/tmp/a.xsd");
        when(RepositoryAccess.findById(xmlFileRepository, 3L)).thenReturn(Optional.of(missingPath));
        assertThrows(IllegalStateException.class, () -> service.startValidationForXmlFile(3L, null, null));

        XmlFileEntity missingXsd = xmlFile(4L, "/tmp/a.xml", " ");
        when(RepositoryAccess.findById(xmlFileRepository, 4L)).thenReturn(Optional.of(missingXsd));
        assertThrows(IllegalStateException.class, () -> service.startValidationForXmlFile(4L, null, null));
    }

    @Test
    void startValidationCreatesJobAndDefersAsyncWorkUntilCommit() {
        XmlFileEntity xml = xmlFile(9L, "/tmp/a.xml", "/tmp/a.xsd");
        ProcessingJobDto job = new ProcessingJobDto("job-9", 9L, "XSD_VALIDATION", "PENDING", 0,
                null, null, null, null, null, "tester", LocalDateTime.now(), LocalDateTime.now());
        when(RepositoryAccess.findById(xmlFileRepository, 9L)).thenReturn(Optional.of(xml));
        when(currentUserService.getCurrentUsername()).thenReturn("tester");
        when(processingJobService.startJob(eq("XSD_VALIDATION"), eq(9L), anyString())).thenReturn(job);
        when(requestRepository.save(any(XsdValidationRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        ProcessingJobDto returned = service.startValidationForXmlFile(9L, "S-9", " ");

        assertSame(job, returned);
        verify(processingJobService).startJob("XSD_VALIDATION", 9L, "XSD validáció előkészítése.");
        verify(auditLogService).log(eq("XSD_VALIDATION_STARTED"), eq(9L), eq("job-9"), isNull(), eq("tester"),
                eq("SUCCESS"), contains("XSD validáció elindítva"), contains("requestId="));
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
    }

    @Test
    void markFinishCancelAndFailUpdateState() {
        XmlFileEntity xml = xmlFile(10L, "/tmp/a.xml", "/tmp/a.xsd");
        XsdValidationRequestEntity request = request("R-10", xml, "job-10", "tester");
        when(requestRepository.findByRequestId("R-10")).thenReturn(Optional.of(request));
        when(requestRepository.save(any(XsdValidationRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        XsdValidationRequestEntity running = service.markRequestRunning("R-10");
        assertEquals("RUNNING", running.getStatus());
        assertNotNull(running.getStartedAt());

        running.setErrorCount(0);
        XsdValidationRequestEntity finished = service.finishRequest("R-10");
        assertEquals("FINISHED", finished.getStatus());
        assertEquals("VALID", finished.getResultStatus());
        verify(auditLogService).log(eq("XSD_VALIDATION_FINISHED"), eq(10L), eq("job-10"), isNull(), eq("tester"),
                eq("SUCCESS"), contains("VALID"), eq("requestId=R-10"));

        service.cancelRequest("R-10", "stopped");
        assertEquals("CANCELLED", request.getStatus());
        assertEquals("CANCELLED", request.getResultStatus());
        assertEquals("stopped", request.getTechnicalErrorMessage());

        when(errorRepository.findByRequestIdOrderByIdAsc("R-10")).thenReturn(List.of());
        request.setErrorCount(0);
        service.failRequest("R-10", "schema_reference failed");
        assertEquals("FAILED", request.getStatus());
        assertEquals("FAILED", request.getResultStatus());
        assertEquals(1, request.getErrorCount());
        verify(errorRepository).save(argThat(error -> "SCHEMA".equals(error.getErrorCode())
                && "schema_reference failed".equals(error.getErrorMessage())));
        verify(auditLogService).log(eq("XSD_VALIDATION_FAILED"), eq(10L), eq("job-10"), isNull(), eq("tester"),
                eq("ERROR"), eq("schema_reference failed"), eq("requestId=R-10"));
    }

    @Test
    void finishMarksInvalidWhenErrorsExist() {
        XmlFileEntity xml = xmlFile(11L, "/tmp/a.xml", "/tmp/a.xsd");
        XsdValidationRequestEntity request = request("R-11", xml, "job-11", "tester");
        request.setErrorCount(3);
        when(requestRepository.findByRequestId("R-11")).thenReturn(Optional.of(request));
        when(requestRepository.save(any(XsdValidationRequestEntity.class))).thenAnswer(i -> i.getArgument(0));

        XsdValidationRequestEntity result = service.finishRequest("R-11");

        assertEquals("INVALID", result.getResultStatus());
        verify(auditLogService).log(eq("XSD_VALIDATION_FINISHED"), eq(11L), eq("job-11"), isNull(), eq("tester"),
                eq("ERROR"), contains("INVALID"), eq("requestId=R-11"));
    }

    @Test
    void failDoesNotDuplicateExistingTechnicalError() {
        XmlFileEntity xml = xmlFile(12L, "/tmp/a.xml", "/tmp/a.xsd");
        XsdValidationRequestEntity request = request("R-12", xml, "job-12", "tester");
        XsdValidationErrorEntity existing = new XsdValidationErrorEntity();
        existing.setErrorCode("SYSTEM");
        when(requestRepository.findByRequestId("R-12")).thenReturn(Optional.of(request));
        when(errorRepository.findByRequestIdOrderByIdAsc("R-12")).thenReturn(List.of(existing));

        service.failRequest("R-12", null);

        verify(errorRepository, never()).save(any());
        assertEquals("FAILED", request.getStatus());
        assertEquals(1, request.getErrorCount());
    }

    @Test
    void getRequestAndErrorsMapEntitiesAndMissingRequestFails() {
        XmlFileEntity xml = xmlFile(13L, "/tmp/a.xml", "/tmp/a.xsd");
        XsdValidationRequestEntity request = request("R-13", xml, "job-13", "tester");
        XsdValidationErrorEntity error = new XsdValidationErrorEntity();
        error.setId(5L);
        error.setSeverity("ERROR");
        error.setErrorCode("cvc-test");
        error.setErrorMessage("bad value");
        error.setLineNumber(7);
        error.setColumnNumber(9);
        error.setPath("/Root[1]/Field_A[1]");
        when(requestRepository.findByRequestId("R-13")).thenReturn(Optional.of(request));
        when(errorRepository.findByRequestIdOrderByIdAsc("R-13")).thenReturn(List.of(error));

        XsdValidationResultDto result = service.getRequest("R-13");
        assertEquals("R-13", result.request().requestId());
        assertEquals(1, result.errors().size());
        assertEquals("cvc-test", result.errors().get(0).code());
        assertEquals("/Root[1]/Field_A[1]", result.errors().get(0).path());
        assertEquals(1, service.getErrors("R-13").size());

        when(requestRepository.findByRequestId("missing")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.getRequest("missing"));
    }

    @Test
    void activeXmlValidationUsesCurrentBrowserSessionAndDefersWork() {
        XmlFileEntity xml = xmlFile(20L, "/tmp/active.xml", "/tmp/active.xsd");
        XmlFileSessionEntity session = new XmlFileSessionEntity();
        session.setSessionId("S-ACTIVE");
        session.setXmlFile(xml);
        when(currentUserService.getCurrentUsername()).thenReturn("tester");
        when(sessionRepository.findByCreatedByAndBrowserSessionIdAndActiveTrueOrderByCreatedAtDesc("tester", "NO_HTTP_REQUEST"))
                .thenReturn(List.of(session));
        ProcessingJobDto job = new ProcessingJobDto("job-20", 20L, "XSD_VALIDATION", "PENDING", 0,
                null, null, null, null, null, "tester", LocalDateTime.now(), LocalDateTime.now());
        when(RepositoryAccess.findById(xmlFileRepository, 20L)).thenReturn(Optional.of(xml));
        when(processingJobService.startJob("XSD_VALIDATION", 20L, "XSD validáció előkészítése.")).thenReturn(job);
        when(requestRepository.save(any(XsdValidationRequestEntity.class))).thenAnswer(i -> i.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        ProcessingJobDto result = service.startValidationForActiveXmlFile();

        assertSame(job, result);
        verify(requestRepository).save(argThat(request -> "S-ACTIVE".equals(request.getXmlFileSessionId())));
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
    }

    @Test
    void activeXmlValidationRejectsMissingSessionAndSkipsNullXmlSessions() {
        when(currentUserService.getCurrentUsername()).thenReturn("tester");
        XmlFileSessionEntity empty = new XmlFileSessionEntity();
        empty.setSessionId("EMPTY");
        when(sessionRepository.findByCreatedByAndBrowserSessionIdAndActiveTrueOrderByCreatedAtDesc("tester", "NO_HTTP_REQUEST"))
                .thenReturn(List.of(empty));

        IllegalStateException error = assertThrows(IllegalStateException.class, service::startValidationForActiveXmlFile);
        assertTrue(error.getMessage().contains("Nincs aktív XML állomány"));
    }

    @Test
    void latestForActiveXmlFileReturnsLatestResultAndReportsMissingResult() {
        XmlFileEntity xml = xmlFile(21L, "/tmp/active.xml", "/tmp/active.xsd");
        XmlFileSessionEntity session = new XmlFileSessionEntity();
        session.setSessionId("S-21");
        session.setXmlFile(xml);
        when(currentUserService.getCurrentUsername()).thenReturn("tester");
        when(sessionRepository.findByCreatedByAndBrowserSessionIdAndActiveTrueOrderByCreatedAtDesc("tester", "NO_HTTP_REQUEST"))
                .thenReturn(List.of(session));
        XsdValidationRequestEntity request = request("R-21", xml, "job-21", "tester");
        when(requestRepository.findFirstByXmlFileIdOrderByCreatedAtDesc(21L)).thenReturn(Optional.of(request));
        when(errorRepository.findByRequestIdOrderByIdAsc("R-21")).thenReturn(List.of());

        assertEquals("R-21", service.getLatestForActiveXmlFile().request().requestId());

        when(requestRepository.findFirstByXmlFileIdOrderByCreatedAtDesc(21L)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, service::getLatestForActiveXmlFile);
    }

    @Test
    void startValidationUsesUnknownUserCustomMessageAndRejectsArchivedStatus() {
        XmlFileEntity archived = xmlFile(22L, "/tmp/a.xml", "/tmp/a.xsd");
        archived.setStatus("ARCHIVED");
        when(RepositoryAccess.findById(xmlFileRepository, 22L)).thenReturn(Optional.of(archived));
        assertThrows(IllegalStateException.class, () -> service.startValidationForXmlFile(22L, null, "custom"));

        XmlFileEntity xml = xmlFile(23L, "/tmp/b.xml", "/tmp/b.xsd");
        when(RepositoryAccess.findById(xmlFileRepository, 23L)).thenReturn(Optional.of(xml));
        when(currentUserService.getCurrentUsername()).thenReturn(" ");
        ProcessingJobDto job = new ProcessingJobDto("job-23", 23L, "XSD_VALIDATION", "PENDING", 0,
                null, null, null, null, null, "unknown", LocalDateTime.now(), LocalDateTime.now());
        when(processingJobService.startJob("XSD_VALIDATION", 23L, "custom message")).thenReturn(job);
        when(requestRepository.save(any(XsdValidationRequestEntity.class))).thenAnswer(i -> i.getArgument(0));
        TransactionSynchronizationManager.initSynchronization();

        assertSame(job, service.startValidationForXmlFile(23L, null, "custom message"));
        verify(requestRepository).save(argThat(request -> "unknown".equals(request.getCreatedBy())));
    }

    @Test
    void streamingValidationResolvesImportedSchemaByFileNameFallback() throws Exception {
        Path root = Files.createTempDirectory("xsd-file-fallback-");
        Path schemaDir = ExceptionSafeOperations.createDirectories(root.resolve("schema"));
        Path commonDir = ExceptionSafeOperations.createDirectories(root.resolve("common/deep"));
        Path shared = commonDir.resolve("shared.xsd");
        Files.writeString(shared, """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                           targetNamespace="urn:shared" elementFormDefault="qualified">
                  <xs:element name="Child" type="xs:string"/>
                </xs:schema>
                """);
        Path primary = schemaDir.resolve("primary.xsd");
        Files.writeString(primary, """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:s="urn:shared">
                  <xs:import namespace="urn:shared" schemaLocation="legacy/path/shared.xsd"/>
                  <xs:element name="Root">
                    <xs:complexType><xs:sequence><xs:element ref="s:Child"/></xs:sequence></xs:complexType>
                  </xs:element>
                </xs:schema>
                """);
        Path xmlPath = root.resolve("valid.xml");
        Files.writeString(xmlPath, "<Root xmlns:s=\"urn:shared\"><s:Child>ok</s:Child></Root>");

        StreamingXsdValidationService resolverService = asyncService(root, xmlPath, primary, commonDir.getParent());
        ProcessingJobDto job = resolverService.startValidationForXmlFile(31L, "S-31", "resolver test");

        assertEquals("job-31", job.jobId());
        verify(processingJobService, timeout(5000)).finish("job-31", "XSD validáció sikeres. Nincs XSD hiba.");
        verify(processingJobService, never()).fail(eq("job-31"), anyString());
    }

    @Test
    void streamingValidationResolvesImportedSchemaByTargetNamespace() throws Exception {
        Path root = Files.createTempDirectory("xsd-namespace-fallback-");
        Path schemaDir = ExceptionSafeOperations.createDirectories(root.resolve("schema"));
        Path commonDir = ExceptionSafeOperations.createDirectories(root.resolve("common/nested"));
        Files.writeString(commonDir.resolve("namespace-only.xsd"), """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                           targetNamespace='urn:shared' elementFormDefault="qualified">
                  <xs:element name="Child" type="xs:string"/>
                </xs:schema>
                """);
        Path primary = schemaDir.resolve("primary.xsd");
        Files.writeString(primary, """
                <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:s="urn:shared">
                  <xs:import namespace="urn:shared" schemaLocation="missing/not-present.xsd"/>
                  <xs:element name="Root">
                    <xs:complexType><xs:sequence><xs:element ref="s:Child"/></xs:sequence></xs:complexType>
                  </xs:element>
                </xs:schema>
                """);
        Path xmlPath = root.resolve("valid.xml");
        Files.writeString(xmlPath, "<Root xmlns:s=\"urn:shared\"><s:Child>ok</s:Child></Root>");

        StreamingXsdValidationService resolverService = asyncService(root, xmlPath, primary, commonDir.getParent());
        ProcessingJobDto job = resolverService.startValidationForXmlFile(32L, null, null);

        assertEquals("job-32", job.jobId());
        verify(processingJobService, timeout(5000)).finish("job-32", "XSD validáció sikeres. Nincs XSD hiba.");
        verify(processingJobService, never()).fail(eq("job-32"), anyString());
    }

    private StreamingXsdValidationService asyncService(Path root, Path xmlPath, Path primaryXsd, Path commonRoot) {
        long id = root.getFileName().toString().contains("namespace") ? 32L : 31L;
        XmlFileEntity xml = xmlFile(id, xmlPath.toString(), primaryXsd.toString());
        when(RepositoryAccess.findById(xmlFileRepository, id)).thenReturn(Optional.of(xml));
        when(currentUserService.getCurrentUsername()).thenReturn("tester");
        ProcessingJobDto job = new ProcessingJobDto("job-" + id, id, "XSD_VALIDATION", "PENDING", 0,
                null, null, null, null, null, "tester", LocalDateTime.now(), LocalDateTime.now());
        when(processingJobService.startJob(eq("XSD_VALIDATION"), eq(id), anyString())).thenReturn(job);

        Map<String, XsdValidationRequestEntity> requests = new ConcurrentHashMap<>();
        when(requestRepository.save(any(XsdValidationRequestEntity.class))).thenAnswer(invocation -> {
            XsdValidationRequestEntity request = invocation.getArgument(0);
            requests.put(request.getRequestId(), request);
            return request;
        });
        when(requestRepository.findByRequestId(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(requests.get(invocation.getArgument(0))));
        when(errorRepository.findByRequestIdOrderByIdAsc(anyString())).thenReturn(List.of());

        XsdValidationProperties properties = new XsdValidationProperties();
        PathConfigurationProperties paths = new PathConfigurationProperties();
        paths.setCommonXsdDir(commonRoot.toString());
        paths.setSchemaDir(root.resolve("unused-schema-root").toString());
        XPathValidatorProperties xpathProperties = new XPathValidatorProperties();
        xpathProperties.setRuleRootDir(" ");
        return new StreamingXsdValidationService(
                xmlFileRepository, sessionRepository, requestRepository, errorRepository,
                processingJobService, currentUserService, auditLogService, properties, paths, xpathProperties);
    }

    private static XmlFileEntity xmlFile(Long id, String filePath, String xsdPath) {
        XmlFileEntity xml = new XmlFileEntity();
        xml.setId(id);
        xml.setFileName("test.xml");
        xml.setFilePath(filePath);
        xml.setXsdPath(xsdPath);
        xml.setStatus("READY");
        xml.setArchived(false);
        return xml;
    }

    private static XsdValidationRequestEntity request(String requestId, XmlFileEntity xml, String jobId, String user) {
        XsdValidationRequestEntity request = new XsdValidationRequestEntity();
        request.setRequestId(requestId);
        request.setXmlFile(xml);
        request.setJobId(jobId);
        request.setXsdPath(xml.getXsdPath());
        request.setStatus("PENDING");
        request.setResultStatus("UNKNOWN");
        request.setCreatedAt(LocalDateTime.now());
        request.setCreatedBy(user);
        request.setUpdatedAt(LocalDateTime.now());
        request.setUpdatedBy(user);
        return request;
    }
}
