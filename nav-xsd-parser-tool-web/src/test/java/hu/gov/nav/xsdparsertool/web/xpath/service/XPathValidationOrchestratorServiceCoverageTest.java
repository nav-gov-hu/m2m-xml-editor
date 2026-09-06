package hu.gov.nav.xsdparsertool.web.xpath.service;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.processing.validation.XsdValidationService;
import hu.gov.nav.xsdparsertool.core.enums.Severity;
import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.processing.ValidationResult;
import hu.gov.nav.xsdparsertool.core.model.validation.ValidationIssue;
import hu.gov.nav.xsdparsertool.processing.xml.XmlProbeService;
import hu.gov.nav.xsdparsertool.schemaregistry.service.FileSystemSchemaRegistryService;
import hu.gov.nav.xsdparsertool.schemaregistry.model.XmlProbeResult;
import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import hu.gov.nav.xsdparsertool.web.processing.service.ProcessingJobService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileSessionRepository;
import hu.gov.nav.xsdparsertool.web.xpath.config.XPathValidatorProperties;
import hu.gov.nav.xsdparsertool.web.xpath.dto.XPathValidationListResponseDto;
import hu.gov.nav.xsdparsertool.web.xpath.dto.XPathValidationRequestStatusDto;
import hu.gov.nav.xsdparsertool.web.xpath.entity.XPathValidationErrorEntity;
import hu.gov.nav.xsdparsertool.web.xpath.entity.XPathValidationRequestEntity;
import hu.gov.nav.xsdparsertool.web.xpath.entity.XPathValidationRequestJournalEntity;
import hu.gov.nav.xsdparsertool.web.xpath.model.CreateResultMode;
import hu.gov.nav.xsdparsertool.web.xpath.model.ResultStatus;
import hu.gov.nav.xsdparsertool.web.xpath.model.ValidatorStatus;
import hu.gov.nav.xsdparsertool.web.xpath.repository.XPathValidationErrorRepository;
import hu.gov.nav.xsdparsertool.web.xpath.repository.XPathValidationRequestJournalRepository;
import hu.gov.nav.xsdparsertool.web.xpath.repository.XPathValidationRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class XPathValidationOrchestratorServiceCoverageTest {

    @TempDir
    Path tempDir;

    private XPathValidationRequestRepository requestRepository;
    private XPathValidationRequestJournalRepository journalRepository;
    private XPathValidationErrorRepository errorRepository;
    private XPathValidatorProperties properties;
    private XmlFileRepository xmlFileRepository;
    private FileSystemSchemaRegistryService schemaRegistryService;
    private XmlProbeService xmlProbeService;
    private XsdValidationService xsdValidationService;
    private ThreadPoolTaskExecutor xpathValidatorExecutor;
    private XPathValidationOrchestratorService service;

    @BeforeEach
    void setUp() {
        requestRepository = mock(XPathValidationRequestRepository.class);
        journalRepository = mock(XPathValidationRequestJournalRepository.class);
        errorRepository = mock(XPathValidationErrorRepository.class);
        properties = new XPathValidatorProperties();
        properties.setDefaultPageSize(10);
        properties.setResultDir(tempDir.resolve("results").toString());
        xpathValidatorExecutor = mock(ThreadPoolTaskExecutor.class);
        schemaRegistryService = mock(FileSystemSchemaRegistryService.class);
        xmlProbeService = mock(XmlProbeService.class);
        xsdValidationService = mock(XsdValidationService.class);
        service = new XPathValidationOrchestratorService(
                requestRepository,
                journalRepository,
                errorRepository,
                properties,
                xpathValidatorExecutor,
                mock(XsltValidationService.class),
                schemaRegistryService,
                xmlProbeService,
                xsdValidationService,
                new PathConfigurationProperties(),
                xmlFileRepository = mock(XmlFileRepository.class),
                mock(XmlFileSessionRepository.class),
                mock(ProcessingJobService.class),
                mock(CurrentUserService.class));
    }

    @Test
    void submitRejectsMissingEmptyOversizedAndNonXmlInput() {
        assertThrows(XPathValidationOrchestratorService.BadRequestException.class,
                () -> service.submit(null, CreateResultMode.ASYNC, "s"));

        MockMultipartFile empty = new MockMultipartFile("file", "a.xml", "application/xml", new byte[0]);
        assertThrows(XPathValidationOrchestratorService.BadRequestException.class,
                () -> service.submit(empty, CreateResultMode.ASYNC, "s"));

        MultipartFile oversized = mock(MultipartFile.class);
        when(oversized.isEmpty()).thenReturn(false);
        when(oversized.getSize()).thenReturn(151L * 1024L * 1024L);
        assertThrows(XPathValidationOrchestratorService.BadRequestException.class,
                () -> service.submit(oversized, CreateResultMode.ASYNC, "s"));

        MockMultipartFile text = new MockMultipartFile("file", "payload.txt", "text/plain", "x".getBytes());
        assertThrows(XPathValidationOrchestratorService.UnsupportedMediaTypeException.class,
                () -> service.submit(text, CreateResultMode.ASYNC, "s"));
    }

    @Test
    void getStatusMapsResultAvailabilityAndMissingRequest() {
        XPathValidationRequestEntity entity = entity("REQ123456789012345");
        entity.setResultFilePath(tempDir.resolve("result.xml").toString());
        entity.setProcessingJobId("JOB-1");
        when(requestRepository.findByRequestId(entity.getRequestId())).thenReturn(Optional.of(entity));

        XPathValidationRequestStatusDto dto = service.getStatus(entity.getRequestId());

        assertEquals(entity.getRequestId(), dto.requestId());
        assertTrue(dto.resultAvailable());
        assertEquals("/api/xpath-validator/requests/" + entity.getRequestId() + "/result", dto.resultDownloadUrl());
        assertFalse(dto.timedOut());
        assertEquals("JOB-1", dto.processingJobId());

        when(requestRepository.findByRequestId("missing")).thenReturn(Optional.empty());
        assertThrows(XPathValidationOrchestratorService.NotFoundException.class, () -> service.getStatus("missing"));
    }

    @Test
    void listUsesRequestedAllowedLimitAndDefaultFallback() {
        XPathValidationRequestEntity entity = entity("REQ123456789012345");
        stubFindAll(entity);

        XPathValidationListResponseDto allowed = service.list(20, " ");
        assertEquals(20, allowed.limit());
        assertEquals(1, allowed.items().size());
        verify(requestRepository).findAll(argThat((Pageable pageable) -> pageable.getPageSize() == 20));

        reset(requestRepository);
        stubFindAll(entity);
        XPathValidationListResponseDto fallback = service.list(17, null);
        assertEquals(10, fallback.limit());
        verify(requestRepository).findAll(argThat((Pageable pageable) -> pageable.getPageSize() == 10));
    }

    @Test
    void listWithQueryUsesTrimmedSearch() {
        XPathValidationRequestEntity entity = entity("ABC123456789012345");
        when(requestRepository.findByRequestIdContainingIgnoreCase(eq("ABC"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        XPathValidationListResponseDto response = service.list(5, "  ABC  ");

        assertEquals(5, response.limit());
        assertEquals("  ABC  ", response.query());
        assertEquals("ABC123456789012345", response.items().get(0).requestId());
    }

    @Test
    void errorsAndJournalAreMappedAfterRequestExistenceCheck() {
        String requestId = "REQ123456789012345";
        when(requestRepository.findByRequestId(requestId)).thenReturn(Optional.of(entity(requestId)));

        XPathValidationErrorEntity error = mock(XPathValidationErrorEntity.class);
        when(error.getErrorCode()).thenReturn("E001");
        when(error.getErrorMessage()).thenReturn("hiba");
        when(error.getSeverity()).thenReturn("ERROR");
        when(error.getDynamicPageIndex()).thenReturn("2");
        when(error.getElementId()).thenReturn("Field_A");
        when(error.getRuleId()).thenReturn("R1");
        when(error.getPath()).thenReturn("/Root[1]/Field_A[1]");
        when(errorRepository.findByRequestIdOrderByCreatedAtAsc(requestId)).thenReturn(List.of(error));

        XPathValidationRequestJournalEntity journal = mock(XPathValidationRequestJournalEntity.class);
        Instant event = Instant.now();
        when(journal.getEventTimestampUtc()).thenReturn(event);
        when(journal.getOldValidatorStatus()).thenReturn(ValidatorStatus.SENT);
        when(journal.getNewValidatorStatus()).thenReturn(ValidatorStatus.FINISHED);
        when(journal.getOldResultStatus()).thenReturn(null);
        when(journal.getNewResultStatus()).thenReturn(ResultStatus.OK);
        when(journal.getMessage()).thenReturn("done");
        when(journalRepository.findByRequestIdOrderByEventTimestampUtcDesc(requestId)).thenReturn(List.of(journal));

        var errors = service.getErrors(requestId);
        var journals = service.getJournal(requestId);

        assertEquals(1, errors.size());
        assertEquals("E001", errors.get(0).errorCode());
        assertEquals("/Root[1]/Field_A[1]", errors.get(0).path());
        assertEquals(1, journals.size());
        assertEquals(event, journals.get(0).eventTimestampUtc());
        assertEquals(ValidatorStatus.FINISHED, journals.get(0).newValidatorStatus());
    }

    @Test
    void resultXmlPrefersExistingFileThenFallsBackToDatabaseValue() throws Exception {
        String requestId = "REQ123456789012345";
        Path resultFile = tempDir.resolve("result.xml");
        Files.writeString(resultFile, "<result>file</result>");
        XPathValidationRequestEntity fileEntity = entity(requestId);
        fileEntity.setResultFilePath(resultFile.toString());
        fileEntity.setResult("<result>db</result>");
        when(requestRepository.findByRequestId(requestId)).thenReturn(Optional.of(fileEntity));

        assertEquals("<result>file</result>", service.getResultXml(requestId));

        XPathValidationRequestEntity dbEntity = entity(requestId);
        dbEntity.setResultFilePath(tempDir.resolve("missing.xml").toString());
        dbEntity.setResult("<result>db</result>");
        when(requestRepository.findByRequestId(requestId)).thenReturn(Optional.of(dbEntity));
        assertEquals("<result>db</result>", service.getResultXml(requestId));

        dbEntity.setResult(null);
        assertThrows(XPathValidationOrchestratorService.NotFoundException.class, () -> service.getResultXml(requestId));
    }

    @Test
    void latestForXmlFileReturnsNewestOrNotFound() {
        XPathValidationRequestEntity entity = entity("REQ123456789012345");
        when(requestRepository.findFirstByXmlFileIdOrderByCreatedAtDesc(44L)).thenReturn(Optional.of(entity));

        assertEquals(entity.getRequestId(), service.getLatestForXmlFile(44L).requestId());

        when(requestRepository.findFirstByXmlFileIdOrderByCreatedAtDesc(45L)).thenReturn(Optional.empty());
        assertThrows(XPathValidationOrchestratorService.NotFoundException.class,
                () -> service.getLatestForXmlFile(45L));
    }

    @Test
    void submitActiveXmlFileRejectsMissingIdRepositoryFilePathAndPhysicalFile() throws Exception {
        assertThrows(XPathValidationOrchestratorService.BadRequestException.class,
                () -> service.submitActiveXmlFile(null, null));

        when(RepositoryAccess.findById(xmlFileRepository, 1L)).thenReturn(Optional.empty());
        assertThrows(XPathValidationOrchestratorService.NotFoundException.class,
                () -> service.submitActiveXmlFile(1L, null));

        XmlFileEntity noPath = new XmlFileEntity();
        noPath.setId(2L);
        when(RepositoryAccess.findById(xmlFileRepository, 2L)).thenReturn(Optional.of(noPath));
        assertThrows(XPathValidationOrchestratorService.BadRequestException.class,
                () -> service.submitActiveXmlFile(2L, null));

        XmlFileEntity missing = new XmlFileEntity();
        missing.setId(3L);
        missing.setFilePath(tempDir.resolve("does-not-exist.xml").toString());
        when(RepositoryAccess.findById(xmlFileRepository, 3L)).thenReturn(Optional.of(missing));
        assertThrows(XPathValidationOrchestratorService.BadRequestException.class,
                () -> service.submitActiveXmlFile(3L, null));
    }

    @Test
    void submitActiveXmlFileReportsMissingXpathRuleAsDedicatedBusinessError() throws Exception {
        Path xmlPath = tempDir.resolve("KSZERZ.xml");
        Files.writeString(xmlPath, "<Doc_KSZERZ/>");
        properties.setRuleRootDir(tempDir.resolve("xpath").toString());

        XmlFileEntity xmlFile = new XmlFileEntity();
        xmlFile.setId(4L);
        xmlFile.setFilePath(xmlPath.toString());
        xmlFile.setFormType("KSZERZ");
        xmlFile.setFormVersion("3.0");
        when(RepositoryAccess.findById(xmlFileRepository, 4L)).thenReturn(Optional.of(xmlFile));

        var ex = assertThrows(XPathValidationOrchestratorService.MissingXPathRuleException.class,
                () -> service.submitActiveXmlFile(4L, null));

        assertEquals("Az ellenőrzés sikertelen: Az Űrlap állományhoz nincs XPath állomány regisztrálva a rendszerbe.",
                ex.getMessage());
    }

    @Test
    void submitRejectsUnresolvedSchemaAndIncompleteMetadata() throws Exception {
        MockMultipartFile xml = new MockMultipartFile("file", "payload.xml", "application/xml", "<Root/>".getBytes());
        XmlProbeResult probe = new XmlProbeResult();
        probe.setRootElementName("Root");
        when(xmlProbeService.probe(any(Path.class))).thenReturn(probe);
        when(schemaRegistryService.resolveByXmlProbe(eq(probe), isNull(), isNull())).thenReturn(null);

        assertThrows(XPathValidationOrchestratorService.BadRequestException.class,
                () -> service.submit(xml, CreateResultMode.ASYNC, "session"));

        SchemaBundle incomplete = new SchemaBundle();
        incomplete.setPrimaryXsd(tempDir.resolve("schema.xsd"));
        when(schemaRegistryService.resolveByXmlProbe(eq(probe), isNull(), isNull())).thenReturn(incomplete);
        assertThrows(XPathValidationOrchestratorService.BadRequestException.class,
                () -> service.submit(xml, CreateResultMode.ASYNC, "session"));
    }

    @Test
    void submitInvalidXsdPersistsIssuesAndReturnsAbortedStatus() throws Exception {
        MockMultipartFile xml = new MockMultipartFile("file", "payload.xml", "application/xml", "<Root/>".getBytes());
        XmlProbeResult probe = new XmlProbeResult();
        SchemaBundle bundle = schemaBundle();
        properties.setRuleRootDir(tempDir.resolve("xpath").toString());
        Path ruleFile = tempDir.resolve("xpath").resolve("FORM").resolve("1.0").resolve("FORM_1.0_xpath.xml");
        Files.createDirectories(ruleFile.getParent());
        Files.writeString(ruleFile, "<Rules/>");

        when(xmlProbeService.probe(any(Path.class))).thenReturn(probe);
        when(schemaRegistryService.resolveByXmlProbe(eq(probe), isNull(), isNull())).thenReturn(bundle);

        ValidationResult validation = new ValidationResult();
        validation.setValid(false);
        validation.setIssues(List.of(new ValidationIssue("cvc-test", "/Root[1]", "invalid", Severity.ERROR)));
        when(xsdValidationService.validate(any(Path.class), same(bundle), isNull())).thenReturn(validation);

        AtomicReference<XPathValidationRequestEntity> saved = new AtomicReference<>();
        when(requestRepository.saveAndFlush(any(XPathValidationRequestEntity.class))).thenAnswer(invocation -> {
            XPathValidationRequestEntity entity = invocation.getArgument(0);
            saved.set(entity);
            return entity;
        });
        when(requestRepository.findByRequestId(anyString())).thenAnswer(invocation -> Optional.ofNullable(saved.get()));

        XPathValidationRequestStatusDto result = service.submit(xml, CreateResultMode.ASYNC, "session");

        assertEquals(ValidatorStatus.ABORTED, result.validatorStatus());
        assertEquals(ResultStatus.ERROR, result.resultStatus());
        assertEquals(1, result.errorCount());
        verify(errorRepository).deleteByRequestId(result.requestId());
        verify(errorRepository).saveAll(anyList());
        verify(journalRepository, atLeast(2)).save(any(XPathValidationRequestJournalEntity.class));
    }

    @Test
    void submitSnapshotReportsMissingXpathRuleBeforeAsyncRequestIsPersisted() throws Exception {
        MockMultipartFile xml = new MockMultipartFile("file", "payload.xml", "application/xml", "<Root/>".getBytes());
        XmlProbeResult probe = new XmlProbeResult();
        SchemaBundle bundle = schemaBundle();
        when(xmlProbeService.probe(any(Path.class))).thenReturn(probe);
        when(schemaRegistryService.resolveByXmlProbe(eq(probe), isNull(), isNull())).thenReturn(bundle);
        properties.setRuleRootDir(tempDir.resolve("missing-xpath-rules").toString());

        var ex = assertThrows(XPathValidationOrchestratorService.MissingXPathRuleException.class,
                () -> service.submit(xml, CreateResultMode.ASYNC, "session"));

        assertEquals("Az ellenőrzés sikertelen: Az Űrlap állományhoz nincs XPath állomány regisztrálva a rendszerbe.",
                ex.getMessage());
        verify(requestRepository, never()).saveAndFlush(any(XPathValidationRequestEntity.class));
        verify(xpathValidatorExecutor, never()).submit(any(Callable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitValidAsyncMovesUploadAndQueuesProcessing() throws Exception {
        MockMultipartFile xml = new MockMultipartFile("file", "payload.xml", "application/xml", "<Root/>".getBytes());
        XmlProbeResult probe = new XmlProbeResult();
        SchemaBundle bundle = schemaBundle();
        when(xmlProbeService.probe(any(Path.class))).thenReturn(probe);
        when(schemaRegistryService.resolveByXmlProbe(eq(probe), isNull(), isNull())).thenReturn(bundle);

        properties.setRuleRootDir(tempDir.resolve("xpath").toString());
        Path ruleFile = tempDir.resolve("xpath").resolve("FORM").resolve("1.0").resolve("FORM_1.0_xpath.xml");
        Files.createDirectories(ruleFile.getParent());
        Files.writeString(ruleFile, "<Rules/>");

        ValidationResult validation = new ValidationResult();
        validation.setValid(true);
        validation.setIssues(List.of());
        when(xsdValidationService.validate(any(Path.class), same(bundle), isNull())).thenReturn(validation);
        when(requestRepository.saveAndFlush(any(XPathValidationRequestEntity.class))).thenAnswer(i -> i.getArgument(0));
        Future<XPathValidationRequestStatusDto> future = mock(Future.class);
        when(xpathValidatorExecutor.submit(any(Callable.class))).thenReturn(future);

        XPathValidationRequestStatusDto result = service.submit(xml, CreateResultMode.ASYNC, "session");

        assertEquals(ValidatorStatus.SENT, result.validatorStatus());
        assertEquals("FORM", result.formName());
        verify(xpathValidatorExecutor).submit(any(Callable.class));
        Path incoming = tempDir.resolve("results").resolve("incoming");
        try (var files = Files.list(incoming)) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().endsWith(".xml.upload")));
        }
    }

    private SchemaBundle schemaBundle() {
        SchemaBundle bundle = new SchemaBundle();
        bundle.setDocumentType("FORM");
        bundle.setDocumentVersion("1.0");
        bundle.setPrimaryXsd(tempDir.resolve("schema.xsd"));
        bundle.setMatchReason("test");
        return bundle;
    }

    private static XPathValidationRequestEntity entity(String requestId) {
        XPathValidationRequestEntity entity = new XPathValidationRequestEntity();
        Instant now = Instant.now();
        entity.setId("entity-1");
        entity.setRequestId(requestId);
        entity.setRequestTimestampUtc(now);
        entity.setFormName("FORM");
        entity.setFormVersion("1.0");
        entity.setCreateResultMode(CreateResultMode.ASYNC);
        entity.setSessionId("session-1");
        entity.setValidatorStatus(ValidatorStatus.FINISHED);
        entity.setResultStatus(ResultStatus.OK);
        entity.setErrorCount(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
    private void stubFindAll(XPathValidationRequestEntity entity) {
        try {
            when(requestRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));
        } catch (RuntimeException ex) {
            throw new AssertionError("A teszt repository stub inicializálása sikertelen.", ex);
        }
    }

}
