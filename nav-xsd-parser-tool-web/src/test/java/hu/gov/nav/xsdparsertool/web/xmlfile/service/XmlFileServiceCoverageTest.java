package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.partner.entity.PartnerEntity;
import hu.gov.nav.xsdparsertool.web.partner.service.PartnerService;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.service.XmlAccessPolicyService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.AutoRegisterServerFilesResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.CopyXmlFileRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.FileNameAvailabilityResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlFileDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlHeaderInfo;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlResourceResolutionInfo;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlResolverInfoDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlSaveRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileSessionEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileLockRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRevisionRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileSessionRepository;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.repository.XsdValidationRequestRepository;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.service.StreamingXsdValidationService;

@ExtendWith(MockitoExtension.class)
class XmlFileServiceCoverageTest {

    @TempDir
    Path tempDir;

    @Mock XmlFileRepository repository;
    @Mock XmlFileLockRepository lockRepository;
    @Mock XmlFileSessionRepository sessionRepository;
    @Mock XmlFileRevisionRepository revisionRepository;
    @Mock XsdValidationRequestRepository xsdValidationRequestRepository;
    @Mock XmlHeaderDetectionService headerDetectionService;
    @Mock XmlResourceResolutionService resourceResolutionService;
    @Mock CurrentUserService currentUserService;
    @Mock AuditLogService auditLogService;
    @Mock StreamingXsdValidationService streamingXsdValidationService;
    @Mock JdbcTemplate jdbcTemplate;
    @Mock LargeXmlMultiformPageService largeXmlMultiformPageService;
    @Mock PartnerService partnerService;
    @Mock XmlAccessPolicyService xmlAccessPolicyService;
    @Mock XmlMutationGuard mutationGuard;

    private XmlFileStorageProperties properties;
    private XmlFileService service;

    @BeforeEach
    void setUp() {
        properties = new XmlFileStorageProperties();
        properties.setUploadDir(tempDir.resolve("upload").toString());
        properties.setArchiveDir(tempDir.resolve("archive").toString());
        properties.setBackupDir(tempDir.resolve("backup").toString());
        properties.setXmlIndexDir(tempDir.resolve("index").toString());
        properties.getServerImport().setRootDir(tempDir.resolve("server").toString());
        properties.getLargeFile().setThreshold("1 MB");
        org.mockito.Mockito.lenient().when(currentUserService.getCurrentUsername()).thenReturn("tester");

        service = new XmlFileService(repository, lockRepository, sessionRepository, revisionRepository,
                xsdValidationRequestRepository, properties, headerDetectionService, resourceResolutionService,
                currentUserService, auditLogService, streamingXsdValidationService, jdbcTemplate,
                largeXmlMultiformPageService, partnerService, xmlAccessPolicyService, mutationGuard);
    }

    @Test
    void checkFileNameReportsAvailableDuplicateAndInvalidNames() {
        when(repository.existsByFileNameIgnoreCase("new.xml")).thenReturn(false);
        when(repository.existsByFileNameIgnoreCase("used.xml")).thenReturn(true);

        FileNameAvailabilityResponse available = service.checkFileName("new.xml");
        FileNameAvailabilityResponse duplicate = service.checkFileName("used.xml");
        FileNameAvailabilityResponse invalid = service.checkFileName("notes.txt");
        FileNameAvailabilityResponse traversal = service.checkFileName("../escape.xml");

        assertTrue(available.available());
        assertFalse(duplicate.available());
        assertFalse(invalid.available());
        assertFalse(traversal.available());
        assertTrue(invalid.message().contains(".xml"));
        assertTrue(traversal.message().contains("Érvénytelen fájlnév"));
    }

    @Test
    void accessAndPartnerQueriesUseRepositoryAndPolicy() {
        XmlFileEntity entity = entity(7L, "a.xml", tempDir.resolve("a.xml"));
        PartnerEntity partner = partner(4L, "123", "Partner");
        entity.setPartner(partner);
        when(RepositoryAccess.findById(repository, 7L)).thenReturn(Optional.of(entity));

        service.requireCurrentUserAccess(7L);
        assertTrue(service.hasPartner(7L));
        verify(xmlAccessPolicyService).requireCurrentUserAccess(entity);
    }

    @Test
    void accessAndPartnerQueriesRejectMissingXml() {
        when(RepositoryAccess.findById(repository, 99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.requireCurrentUserAccess(99L));
        assertThrows(IllegalArgumentException.class, () -> service.hasPartner(99L));
    }

    @Test
    void updateNoteTrimsPersistsAndRejectsTooLongValue() {
        XmlFileEntity entity = entity(1L, "note.xml", tempDir.resolve("note.xml"));
        when(RepositoryAccess.findById(repository, 1L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        XmlFileDto dto = service.updateNote(1L, "  megjegyzes  ");

        assertEquals("megjegyzes", dto.userNote());
        assertEquals("tester", entity.getUpdatedBy());
        assertNotNull(entity.getUpdatedAt());
        assertThrows(IllegalArgumentException.class, () -> service.updateNote(1L, "x".repeat(1001)));
    }

    @Test
    void downloadPathReturnsExistingFileAndRejectsMissingOne() throws Exception {
        Path existing = tempDir.resolve("download.xml");
        Files.writeString(existing, "<root/>", StandardCharsets.UTF_8);
        XmlFileEntity entity = entity(2L, "download.xml", existing);
        when(RepositoryAccess.findById(repository, 2L)).thenReturn(Optional.of(entity));
        assertEquals(existing.toAbsolutePath().normalize(), service.downloadPath(2L));

        XmlFileEntity missing = entity(3L, "missing.xml", tempDir.resolve("missing.xml"));
        when(RepositoryAccess.findById(repository, 3L)).thenReturn(Optional.of(missing));
        assertThrows(IllegalStateException.class, () -> service.downloadPath(3L));
    }

    @Test
    void updatePartnerReplacesPartnerAndImportState() {
        XmlFileEntity entity = entity(5L, "partner.xml", tempDir.resolve("partner.xml"));
        entity.setPartner(partner(1L, "OLD", "Old"));
        PartnerEntity replacement = partner(2L, "NEW", "New");
        when(RepositoryAccess.findById(repository, 5L)).thenReturn(Optional.of(entity));
        when(partnerService.require(2L)).thenReturn(replacement);
        when(repository.save(entity)).thenReturn(entity);

        XmlFileDto dto = service.updatePartner(5L, 2L);

        assertEquals(2L, dto.partnerId());
        assertEquals("ASSIGNED", entity.getPartnerImportStatus());
        assertEquals("tester", entity.getUpdatedBy());
    }

    @Test
    void listFiltersByAccessAndBuildsDtoWithoutActiveLock() {
        XmlFileEntity first = entity(10L, "one.xml", tempDir.resolve("one.xml"));
        XmlFileEntity second = entity(11L, "two.xml", tempDir.resolve("two.xml"));
        when(repository.findByArchivedFalseOrderByCreatedAtDesc()).thenReturn(List.of(first, second));
        when(xmlAccessPolicyService.filterCurrentUser(List.of(first, second))).thenReturn(List.of(second));
        when(lockRepository.findByXmlFileIdAndStatus(11L, "ACTIVE")).thenReturn(Optional.empty());
        when(xsdValidationRequestRepository.findFirstByXmlFileIdOrderByCreatedAtDesc(11L)).thenReturn(Optional.empty());
        when(revisionRepository.countByXmlFileId(11L)).thenReturn(3L);

        List<XmlFileDto> result = service.list(false);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).id());
        assertEquals(3L, result.get(0).revisionCount());
        assertFalse(result.get(0).locked());
    }

    @Test
    void uploadCreatesFileAssignsPartnerAndPersistsResolvedMetadata() throws Exception {
        PartnerEntity partner = partner(8L, "12345678", "Upload Partner");
        when(repository.existsByFileNameIgnoreCase("upload.xml")).thenReturn(false);
        when(partnerService.require(8L)).thenReturn(partner);
        mockResolvedMetadata();
        when(repository.save(any(XmlFileEntity.class))).thenAnswer(invocation -> {
            XmlFileEntity saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });
        MockMultipartFile file = new MockMultipartFile("file", "upload.xml", "application/xml",
                "<Doc/>".getBytes(StandardCharsets.UTF_8));

        XmlFileDto dto = service.upload(file, " note ", 8L);

        assertEquals(101L, dto.id());
        assertEquals("upload.xml", dto.fileName());
        assertEquals(8L, dto.partnerId());
        assertEquals("READY", dto.status());
        assertEquals("FORM", dto.formType());
        assertTrue(ExceptionSafeOperations.isRegularFile(Path.of(dto.filePath())));
    }

    @Test
    void uploadRejectsEmptyFileDuplicateNameAndMissingPartner() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.xml", "application/xml", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> service.upload(empty, null, 1L));

        MockMultipartFile duplicate = new MockMultipartFile("file", "dup.xml", "application/xml", "<x/>".getBytes());
        when(repository.existsByFileNameIgnoreCase("dup.xml")).thenReturn(true);
        assertThrows(XmlFileService.DuplicateXmlFileNameException.class, () -> service.upload(duplicate, null, 1L));

        MockMultipartFile traversal = new MockMultipartFile("file", "../escape.xml", "application/xml", "<x/>".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.upload(traversal, null, 1L));

        MockMultipartFile noPartner = new MockMultipartFile("file", "nopartner.xml", "application/xml", "<x/>".getBytes());
        when(repository.existsByFileNameIgnoreCase("nopartner.xml")).thenReturn(false);
        mockResolvedMetadata();
        assertThrows(IllegalArgumentException.class, () -> service.upload(noPartner, null, null));
        verify(repository, never()).save(any(XmlFileEntity.class));
    }

    @Test
    void saveAsCreatesIndependentFileAndKeepsPartner() throws Exception {
        XmlFileEntity source = entity(20L, "source.xml", tempDir.resolve("source.xml"));
        source.setPartner(partner(3L, "TAX", "Partner"));
        when(RepositoryAccess.findById(repository, 20L)).thenReturn(Optional.of(source));
        when(repository.existsByFileNameIgnoreCase("copy-as.xml")).thenReturn(false);
        mockResolvedMetadata();
        when(repository.save(any(XmlFileEntity.class))).thenAnswer(invocation -> {
            XmlFileEntity saved = invocation.getArgument(0);
            saved.setId(21L);
            return saved;
        });

        XmlSaveRequest request = new XmlSaveRequest("<Doc><A>1</A></Doc>", null, false, false, "uj", "copy-as.xml");
        XmlFileDto dto = service.saveAs(20L, request);

        verify(mutationGuard).requireMutable(20L);
        assertEquals(21L, dto.id());
        assertEquals(3L, dto.partnerId());
        assertEquals("<Doc><A>1</A></Doc>", Files.readString(Path.of(dto.filePath())));
    }

    @Test
    void saveAsRejectsMissingContentAndDuplicateTargetName() {
        XmlFileEntity source = entity(30L, "source.xml", tempDir.resolve("source.xml"));
        when(RepositoryAccess.findById(repository, 30L)).thenReturn(Optional.of(source));
        when(repository.existsByFileNameIgnoreCase("empty.xml")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.saveAs(30L,
                new XmlSaveRequest(" ", null, false, false, null, "empty.xml")));

        when(repository.existsByFileNameIgnoreCase("dup.xml")).thenReturn(true);
        assertThrows(XmlFileService.DuplicateXmlFileNameException.class, () -> service.saveAs(30L,
                new XmlSaveRequest("<x/>", null, false, false, null, "dup.xml")));
    }

    @Test
    void copyCopiesPhysicalFileAndPersistsNewEntity() throws Exception {
        Path sourcePath = tempDir.resolve("source-copy.xml");
        Files.writeString(sourcePath, "<Doc/>");
        XmlFileEntity source = entity(40L, "source-copy.xml", sourcePath);
        source.setOriginalFileName("original.xml");
        when(RepositoryAccess.findById(repository, 40L)).thenReturn(Optional.of(source));
        when(repository.existsByFileNameIgnoreCase("copied.xml")).thenReturn(false);
        mockResolvedMetadata();
        when(repository.save(any(XmlFileEntity.class))).thenAnswer(invocation -> {
            XmlFileEntity saved = invocation.getArgument(0);
            saved.setId(41L);
            return saved;
        });

        XmlFileDto dto = service.copy(40L, new CopyXmlFileRequest("copied.xml", "copy note"));

        assertEquals(41L, dto.id());
        assertEquals("original.xml", dto.originalFileName());
        assertEquals("<Doc/>", Files.readString(Path.of(dto.filePath())));
    }


    @Test
    void copyAndSaveAsRejectPathBearingTargetNames() throws Exception {
        Path sourcePath = tempDir.resolve("source-path.xml");
        Files.writeString(sourcePath, "<Doc/>", StandardCharsets.UTF_8);
        XmlFileEntity source = entity(55L, "source-path.xml", sourcePath);
        when(RepositoryAccess.findById(repository, 55L)).thenReturn(Optional.of(source));

        assertThrows(IllegalArgumentException.class,
                () -> service.copy(55L, new CopyXmlFileRequest("../copy.xml", null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.saveAs(55L, new XmlSaveRequest("<Doc/>", null, false, false, null, "../save.xml")));
    }

    @Test
    void physicalArchiveMovesFileAndMarksEntityArchived() throws Exception {
        Path source = tempDir.resolve("physical.xml");
        Files.writeString(source, "<Doc/>");
        XmlFileEntity entity = entity(60L, "physical.xml", source);
        when(RepositoryAccess.findById(repository, 60L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        XmlFileDto dto = service.physicalArchive(60L, "cleanup");

        assertTrue(dto.archived());
        assertEquals("ARCHIVED", dto.status());
        assertFalse(ExceptionSafeOperations.fileExists(source));
        assertTrue(ExceptionSafeOperations.isRegularFile(Path.of(dto.filePath())));
        assertTrue(Path.of(dto.filePath()).startsWith(tempDir.resolve("archive")));
    }

    @Test
    void registerServerFilePersistsExistingFileWhenBrowserIsEnabled() throws Exception {
        Path serverRoot = tempDir.resolve("server");
        ExceptionSafeOperations.createDirectories(serverRoot);
        Path serverFile = serverRoot.resolve("registered.xml");
        Files.writeString(serverFile, "<Doc/>", StandardCharsets.UTF_8);
        properties.getServerBrowser().setEnabled(true);
        when(repository.existsByFileNameIgnoreCase("registered.xml")).thenReturn(false);
        mockResolvedMetadata();
        when(repository.save(any(XmlFileEntity.class))).thenAnswer(invocation -> {
            XmlFileEntity saved = invocation.getArgument(0);
            saved.setId(70L);
            return saved;
        });

        XmlFileDto dto = service.registerServerFile(serverFile.toString(), "server note");

        assertEquals(70L, dto.id());
        assertEquals("SERVER_FILE", dto.sourceType());
        assertEquals(serverFile.toAbsolutePath().normalize().toString(), dto.filePath());
        verify(streamingXsdValidationService).startValidationForXmlFile(eq(70L), eq(null), any(String.class));
    }

    @Test
    void registerServerFileRejectsExistingFileOutsideConfiguredRoot() throws Exception {
        Path serverRoot = tempDir.resolve("server");
        ExceptionSafeOperations.createDirectories(serverRoot);
        Path outside = tempDir.resolve("outside.xml");
        Files.writeString(outside, "<Doc/>", StandardCharsets.UTF_8);
        properties.getServerBrowser().setEnabled(true);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.registerServerFile(outside.toString(), null));

        assertTrue(thrown.getMessage().contains("root könyvtár alatt"));
        verify(repository, never()).save(any(XmlFileEntity.class));
    }

    @Test
    void registerServerFileRejectsSymlinkThatEscapesConfiguredRootWhenSupported() throws Exception {
        Path serverRoot = tempDir.resolve("server");
        ExceptionSafeOperations.createDirectories(serverRoot);
        Path outside = tempDir.resolve("outside-symlink-target.xml");
        Files.writeString(outside, "<Doc/>", StandardCharsets.UTF_8);
        Path link = serverRoot.resolve("linked.xml");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | java.io.IOException | SecurityException ex) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "A futtatókörnyezet nem támogat tesztelhető symlink létrehozást.");
            return;
        }
        properties.getServerBrowser().setEnabled(true);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.registerServerFile(link.toString(), null));

        assertTrue(thrown.getMessage().contains("root könyvtár alatt"));
        verify(repository, never()).save(any(XmlFileEntity.class));
    }


    @Test
    void autoRegisterServerFilesReturnsWarningWhenRootDirectoryIsNotConfigured() throws Exception {
        properties.getServerBrowser().setEnabled(true);
        properties.getServerImport().setRootDir(null);

        AutoRegisterServerFilesResponse response = service.autoRegisterServerFiles("system-background");

        assertTrue(response.enabled());
        assertEquals("", response.rootDir());
        assertEquals(0, response.scannedCount());
        assertEquals(0, response.registeredCount());
        assertEquals(0, response.skippedCount());
        assertEquals(List.of("A szerver oldali XML root könyvtár nincs konfigurálva."), response.warnings());
        verify(repository, never()).save(any(XmlFileEntity.class));
    }

    @Test
    void autoRegisterServerFilesUsesPartnerSidecarAndSkipsKnownFiles() throws Exception {
        Path serverRoot = tempDir.resolve("server");
        ExceptionSafeOperations.createDirectories(serverRoot);
        Path newXml = serverRoot.resolve("a-new.xml");
        Path knownXml = serverRoot.resolve("b-known.xml");
        Files.writeString(newXml, "<Doc/>", StandardCharsets.UTF_8);
        Files.writeString(knownXml, "<Doc/>", StandardCharsets.UTF_8);
        Files.writeString(serverRoot.resolve("a-new.partner"), "\"12345678\";\"Imported Partner\"\n", StandardCharsets.UTF_8);
        properties.getServerBrowser().setEnabled(true);
        when(repository.existsByFileNameIgnoreCase("a-new.xml")).thenReturn(false);
        when(repository.existsByFileNameIgnoreCase("b-known.xml")).thenReturn(true);
        PartnerEntity imported = partner(9L, "12345678", "Imported Partner");
        when(partnerService.resolveOrCreateImportedPartner("12345678", "Imported Partner")).thenReturn(imported);
        mockResolvedMetadata();
        when(repository.save(any(XmlFileEntity.class))).thenAnswer(invocation -> {
            XmlFileEntity saved = invocation.getArgument(0);
            saved.setId(71L);
            return saved;
        });

        AutoRegisterServerFilesResponse response = service.autoRegisterServerFiles("batch-user");

        assertTrue(response.enabled());
        assertEquals(2, response.scannedCount());
        assertEquals(1, response.registeredCount());
        assertEquals(1, response.skippedCount());
        assertTrue(response.warnings().isEmpty());
        assertEquals(9L, response.registeredFiles().get(0).partnerId());
        assertEquals("ASSIGNED", response.registeredFiles().get(0).partnerImportStatus());
        verify(streamingXsdValidationService).startValidationForXmlFile(eq(71L), eq(null), any(String.class));
    }


    @Test
    void autoRegisterServerFilesSkipsPhysicalFileAlreadyStoredUnderDifferentDisplayName() throws Exception {
        Path serverRoot = tempDir.resolve("server-physical-duplicate");
        ExceptionSafeOperations.createDirectories(serverRoot);
        Path storedXml = serverRoot.resolve("739df987-faa0-4ae6-949d-9ab36dde8b0c.xml");
        Files.writeString(storedXml, "<Doc/>", StandardCharsets.UTF_8);
        properties.getServerBrowser().setEnabled(true);
        properties.getServerImport().setRootDir(serverRoot.toString());
        when(repository.existsByFilePathIgnoreCase(storedXml.toAbsolutePath().normalize().toString())).thenReturn(true);

        AutoRegisterServerFilesResponse response = service.autoRegisterServerFiles("batch-user");

        assertEquals(1, response.scannedCount());
        assertEquals(0, response.registeredCount());
        assertEquals(1, response.skippedCount());
        verify(repository, never()).save(any(XmlFileEntity.class));
        verify(repository, never()).existsByFileNameIgnoreCase(anyString());
    }

    @Test
    void autoRegisterServerFilesSkipsManagedUuidFileEvenBeforeDatabaseRowIsVisible() throws Exception {
        Path sharedRoot = tempDir.resolve("shared-upload-root");
        ExceptionSafeOperations.createDirectories(sharedRoot);
        Path managedXml = sharedRoot.resolve("ad914bdb-4762-477e-8b54-52ebf3fec0f7.xml");
        Files.writeString(managedXml, "<Doc/>", StandardCharsets.UTF_8);
        properties.setUploadDir(sharedRoot.toString());
        properties.getServerBrowser().setEnabled(true);
        properties.getServerImport().setRootDir(sharedRoot.toString());

        AutoRegisterServerFilesResponse response = service.autoRegisterServerFiles("batch-user");

        assertEquals(1, response.scannedCount());
        assertEquals(0, response.registeredCount());
        assertEquals(1, response.skippedCount());
        verify(repository, never()).existsByFilePathIgnoreCase(anyString());
        verify(repository, never()).existsByFileNameIgnoreCase(anyString());
        verify(repository, never()).save(any(XmlFileEntity.class));
    }

    @Test
    void permanentlyDeleteRejectsActiveSessionBeforeDeletingAnything() {
        XmlFileEntity entity = entity(80L, "active.xml", tempDir.resolve("active.xml"));
        XmlFileSessionEntity activeSession = new XmlFileSessionEntity();
        activeSession.setSessionId("session-1");
        when(RepositoryAccess.findById(repository, 80L)).thenReturn(Optional.of(entity));
        when(lockRepository.findByXmlFileIdAndStatus(80L, "ACTIVE")).thenReturn(Optional.empty());
        when(sessionRepository.findByXmlFileIdAndActiveTrue(80L)).thenReturn(List.of(activeSession));

        assertThrows(IllegalStateException.class, () -> service.permanentlyDelete(80L, "cleanup"));

        verify(repository, never()).delete(any(XmlFileEntity.class));
        verify(jdbcTemplate, never()).update(any(String.class), anyLong());
    }

    @Test
    void permanentlyDeleteRemovesXmlTemporaryBackupAndRevisionFiles() throws Exception {
        Path source = tempDir.resolve("delete.xml");
        Path fragment = tempDir.resolve("delete.xml.fragment-save.tmp");
        Path legacyBackup = tempDir.resolve("delete.xml.backup-old");
        Path configuredBackup = tempDir.resolve("backup").resolve("81").resolve("nested").resolve("saved.xml");
        Path revisionBackup = tempDir.resolve("backup").resolve("81").resolve("revision-backup.xml");
        ExceptionSafeOperations.createDirectories(configuredBackup.getParent());
        Files.writeString(source, "<Doc/>");
        Files.writeString(fragment, "tmp");
        Files.writeString(legacyBackup, "backup");
        Files.writeString(configuredBackup, "backup");
        Files.writeString(revisionBackup, "revision");
        XmlFileEntity entity = entity(81L, "delete.xml", source);
        when(RepositoryAccess.findById(repository, 81L)).thenReturn(Optional.of(entity));
        when(lockRepository.findByXmlFileIdAndStatus(81L, "ACTIVE")).thenReturn(Optional.empty());
        when(sessionRepository.findByXmlFileIdAndActiveTrue(81L)).thenReturn(List.of());
        when(jdbcTemplate.queryForList(any(String.class), eq(String.class), eq(81L))).thenReturn(List.of(revisionBackup.toString()));

        service.permanentlyDelete(81L, "GDPR cleanup");

        verify(repository).delete(entity);
        verify(repository).flush();
        verify(largeXmlMultiformPageService).removeAllForFile(81L, source.toAbsolutePath().normalize());
        assertFalse(ExceptionSafeOperations.fileExists(source));
        assertFalse(ExceptionSafeOperations.fileExists(fragment));
        assertFalse(ExceptionSafeOperations.fileExists(legacyBackup));
        assertFalse(ExceptionSafeOperations.fileExists(tempDir.resolve("backup").resolve("81")));
        assertFalse(ExceptionSafeOperations.fileExists(revisionBackup));
    }


    @Test
    void permanentlyDeleteRejectsRevisionBackupOutsideConfiguredAndLegacyRoots() throws Exception {
        Path source = tempDir.resolve("outside-delete.xml");
        Path outsideRevision = tempDir.resolve("outside-revision.bak");
        Files.writeString(source, "<Doc/>");
        Files.writeString(outsideRevision, "must stay");
        XmlFileEntity entity = entity(82L, "outside-delete.xml", source);
        when(RepositoryAccess.findById(repository, 82L)).thenReturn(Optional.of(entity));
        when(lockRepository.findByXmlFileIdAndStatus(82L, "ACTIVE")).thenReturn(Optional.empty());
        when(sessionRepository.findByXmlFileIdAndActiveTrue(82L)).thenReturn(List.of());
        when(jdbcTemplate.queryForList(any(String.class), eq(String.class), eq(82L))).thenReturn(List.of(outsideRevision.toString()));

        assertThrows(java.io.IOException.class, () -> service.permanentlyDelete(82L, "cleanup"));

        assertTrue(ExceptionSafeOperations.fileExists(outsideRevision));
    }

    @Test
    void resolveInfoRefreshesDetectedResourcesWithoutOverwritingStoredMetadata() throws Exception {
        Path xml = tempDir.resolve("resolver.xml");
        Path xsd = tempDir.resolve("resolved.xsd");
        Files.writeString(xml, "<Doc/>");
        Files.writeString(xsd, "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");
        XmlFileEntity entity = entity(90L, "resolver.xml", xml);
        entity.setFormType("STORED_FORM");
        entity.setFormVersion("old");
        entity.setXsdPath("stored.xsd");
        when(RepositoryAccess.findById(repository, 90L)).thenReturn(Optional.of(entity));
        XmlHeaderInfo header = new XmlHeaderInfo("Doc", "urn:test", "urn:test resolved.xsd", null, "HEADER_FORM", "1", null);
        when(headerDetectionService.detect(xml.toAbsolutePath().normalize())).thenReturn(header);
        when(resourceResolutionService.resolve(header)).thenReturn(new XmlResourceResolutionInfo(
                "RESOLVED_FORM", "2", xsd.toString(), null, null, "RESOLVED", "fresh"));

        XmlResolverInfoDto info = service.resolveInfo(90L);

        assertEquals("RESOLVED_FORM", info.formType());
        assertEquals("2", info.formVersion());
        assertTrue(info.xsdExists());
        assertEquals("STORED_FORM", info.storedFormType());
        assertEquals("old", info.storedFormVersion());
        assertEquals("stored.xsd", info.storedXsdPath());
    }

    @Test
    void archiveMarksEntityAndPersistsAuditFields() {
        XmlFileEntity entity = entity(50L, "archive.xml", tempDir.resolve("archive.xml"));
        when(RepositoryAccess.findById(repository, 50L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        XmlFileDto dto = service.archive(50L, "cleanup");

        assertTrue(dto.archived());
        assertEquals("ARCHIVED", dto.status());
        assertEquals("tester", dto.archivedBy());
        assertNotNull(dto.archivedAt());
    }

    private void mockResolvedMetadata() throws Exception {
        when(headerDetectionService.detect(any(Path.class))).thenReturn(new XmlHeaderInfo(
                "Doc", "urn:test", "urn:test schema.xsd", null, "FORM", "1", null));
        when(resourceResolutionService.resolve(any(XmlHeaderInfo.class))).thenReturn(new XmlResourceResolutionInfo(
                "FORM", "1", "schema.xsd", null, null, "RESOLVED", null));
    }

    private XmlFileEntity entity(Long id, String name, Path path) {
        XmlFileEntity entity = new XmlFileEntity();
        entity.setId(id);
        entity.setFileName(name);
        entity.setOriginalFileName(name);
        entity.setFilePath(path.toString());
        entity.setFileSizeBytes(10L);
        entity.setStatus("READY");
        entity.setSourceType("TEST");
        entity.setArchived(false);
        entity.setLargeFileMode(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy("tester");
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy("tester");
        return entity;
    }

    private PartnerEntity partner(Long id, String taxNumber, String name) {
        PartnerEntity partner = new PartnerEntity();
        partner.setId(id);
        partner.setTaxNumber(taxNumber);
        partner.setName(name);
        return partner;
    }
}
