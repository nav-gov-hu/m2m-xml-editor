package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.service.XmlAccessPolicyService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlDiffPreviewResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlSaveRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlSaveResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileDiffEntryEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileLockEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileRevisionEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileSessionEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileDiffEntryRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileLockRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRevisionRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileSessionRepository;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.service.StreamingXsdValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class XmlFileSaveServiceFunctionalTest {

    @TempDir Path tempDir;

    @Mock XmlFileRepository xmlFiles;
    @Mock XmlFileSessionRepository sessions;
    @Mock XmlFileLockRepository locks;
    @Mock XmlFileRevisionRepository revisions;
    @Mock XmlFileDiffEntryRepository diffs;
    @Mock CurrentUserService currentUser;
    @Mock AuditLogService audit;
    @Mock StreamingXsdValidationService xsdValidation;
    @Mock XmlAccessPolicyService accessPolicy;
    @Mock XmlMutationGuard mutationGuard;

    private XmlFileStorageProperties storage;
    private XmlFileSaveService service;
    private XmlFileEntity xmlFile;
    private Path xmlPath;
    private final AtomicLong revisionIds = new AtomicLong(1000L);
    private final AtomicLong diffIds = new AtomicLong(2000L);

    @BeforeEach
    void setUp() throws Exception {
        storage = new XmlFileStorageProperties();
        storage.setBackupDir(tempDir.resolve("backup").toString());
        storage.setUploadDir(tempDir.resolve("upload").toString());

        xmlPath = tempDir.resolve("sample.xml");
        Files.writeString(xmlPath, "<Root><A>old</A><B>keep</B></Root>", StandardCharsets.UTF_8);

        xmlFile = new XmlFileEntity();
        xmlFile.setId(10L);
        xmlFile.setFileName("sample.xml");
        xmlFile.setOriginalFileName("sample.xml");
        xmlFile.setFilePath(xmlPath.toString());
        xmlFile.setFileSizeBytes(Files.size(xmlPath));
        xmlFile.setSourceType("UPLOAD");
        xmlFile.setStatus("REGISTERED");
        xmlFile.setArchived(false);
        xmlFile.setLargeFileMode(false);
        xmlFile.setCreatedAt(LocalDateTime.now().minusDays(1));
        xmlFile.setCreatedBy("tester");

        lenient().when(currentUser.getCurrentUsername()).thenReturn("tester");
        lenient().when(RepositoryAccess.findById(xmlFiles, 10L)).thenReturn(Optional.of(xmlFile));
        lenient().when(revisions.maxRevisionNo(10L)).thenReturn(0);
        lenient().when(revisions.save(any(XmlFileRevisionEntity.class))).thenAnswer(invocation -> {
            XmlFileRevisionEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) entity.setId(revisionIds.incrementAndGet());
            return entity;
        });
        lenient().when(diffs.save(any(XmlFileDiffEntryEntity.class))).thenAnswer(invocation -> {
            XmlFileDiffEntryEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) entity.setId(diffIds.incrementAndGet());
            return entity;
        });

        service = new XmlFileSaveService(
                xmlFiles, sessions, locks, revisions, diffs, currentUser, audit,
                xsdValidation, storage, accessPolicy, mutationGuard);
    }

    @Test
    void diffPreviewMustReportChangedAddedAndRemovedValuesWithoutWritingAnything() throws Exception {
        XmlSaveRequest request = request("<Root><A>new</A><C>added</C></Root>", false, null, null);

        XmlDiffPreviewResponse response = service.diffPreview(10L, request);

        assertEquals(3, response.changeCount());
        assertTrue(response.entries().stream().anyMatch(e -> "CHANGED".equals(e.changeType()) && "/Root/A".equals(e.xmlPath())));
        assertTrue(response.entries().stream().anyMatch(e -> "REMOVED".equals(e.changeType()) && "/Root/B".equals(e.xmlPath())));
        assertTrue(response.entries().stream().anyMatch(e -> "ADDED".equals(e.changeType()) && "/Root/C".equals(e.xmlPath())));
        assertEquals("<Root><A>old</A><B>keep</B></Root>", Files.readString(xmlPath));
        verifyNoInteractions(revisions, diffs, audit, xsdValidation);
    }

    @Test
    void overwriteRejectsDoctypeBeforeWritingOrCreatingRevision() throws Exception {
        enableEditableSession("session-xxe", "lock-xxe");
        Path secret = tempDir.resolve("secret.txt");
        Files.writeString(secret, "TOP-SECRET", StandardCharsets.UTF_8);
        String malicious = "<!DOCTYPE Root [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]><Root><A>&xxe;</A></Root>";

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.overwrite(10L, request(malicious, false, null, null, "session-xxe")));

        assertTrue(error.getMessage().startsWith("Az XML pretty print formázása nem sikerült:"));
        assertEquals("<Root><A>old</A><B>keep</B></Root>", Files.readString(xmlPath));
        verifyNoInteractions(revisions, diffs, audit, xsdValidation);
        verify(xmlFiles, never()).save(any(XmlFileEntity.class));
    }

    @Test
    void overwriteMustPrettyPrintCreateBackupRevisionAndDiffEntries() throws Exception {
        enableEditableSession("session-1", "lock-1");
        XmlSaveRequest request = request("<Root>\n\n<A>new</A>\n<C>added</C>\n</Root>", false, "  megjegyzes  ", null, "session-1");

        XmlSaveResponse response = service.overwrite(10L, request);

        assertEquals("OVERWRITE", response.saveType());
        assertEquals(1, response.revisionNo());
        assertEquals(3, response.changeCount());
        assertNotNull(response.backupFilePath());
        assertTrue(ExceptionSafeOperations.fileExists(Path.of(response.backupFilePath())));
        assertEquals("<Root><A>old</A><B>keep</B></Root>", Files.readString(Path.of(response.backupFilePath())));

        String saved = Files.readString(xmlPath);
        assertTrue(saved.contains("<A>new</A>"));
        assertTrue(saved.contains("<C>added</C>"));
        assertTrue(saved.lines().noneMatch(String::isBlank), "A pretty print nem hagyhat ures formázási sorokat.");

        ArgumentCaptor<XmlFileRevisionEntity> revisionCaptor = ArgumentCaptor.forClass(XmlFileRevisionEntity.class);
        verify(revisions).save(revisionCaptor.capture());
        XmlFileRevisionEntity revision = revisionCaptor.getValue();
        assertEquals("OVERWRITE", revision.getSaveType());
        assertEquals("megjegyzes", revision.getUserNote());
        assertEquals(3, revision.getChangeCount());
        assertEquals("ADDED=1; CHANGED=1; REMOVED=1", revision.getDiffSummary());
        assertEquals(Boolean.FALSE, revision.getXsdValidationRequested());
        assertEquals("SKIPPED", revision.getXsdValidationStatus());
        verify(diffs, times(3)).save(any(XmlFileDiffEntryEntity.class));
        verify(xmlFiles).save(xmlFile);
        verify(mutationGuard).requireMutable(10L);
    }

    @Test
    void overwriteMustPersistIntoTheExistingPhysicalFileWithoutReplacingItsIdentity() throws Exception {
        enableEditableSession("session-in-place", "lock-in-place");
        Object beforeFileKey = Files.readAttributes(xmlPath, BasicFileAttributes.class).fileKey();

        XmlSaveResponse response = service.overwrite(10L,
                request("<Root><A>physically-saved</A><B>keep</B></Root>", false, null, null, "session-in-place"));

        assertEquals(xmlPath.toString(), response.targetFilePath());
        assertTrue(Files.readString(xmlPath, StandardCharsets.UTF_8).contains("<A>physically-saved</A>"));
        Object afterFileKey = Files.readAttributes(xmlPath, BasicFileAttributes.class).fileKey();
        if (beforeFileKey != null && afterFileKey != null) {
            assertEquals(beforeFileKey, afterFileKey,
                    "A felülírásnak a már kezelt fizikai fájlt kell módosítania, nem új ACL-ű fájlra cserélnie.");
        }
    }

    @Test
    void overwriteWithValidationRequestedMustStartValidationAndPersistRequestedStatus() throws Exception {
        enableEditableSession("session-2", "lock-2");
        XmlSaveRequest request = request("<Root><A>new</A><B>keep</B></Root>", true, null, null, "session-2");

        XmlSaveResponse response = service.overwrite(10L, request);

        assertEquals(1, response.changeCount());
        verify(xsdValidation).startValidationForXmlFile(10L, null, "Mentés utáni XSD validáció indítása.");
        ArgumentCaptor<XmlFileRevisionEntity> revisionCaptor = ArgumentCaptor.forClass(XmlFileRevisionEntity.class);
        verify(revisions).save(revisionCaptor.capture());
        assertEquals(Boolean.TRUE, revisionCaptor.getValue().getXsdValidationRequested());
        assertEquals("REQUESTED", revisionCaptor.getValue().getXsdValidationStatus());
    }

    @Test
    void validationStartupFailureMustNotRollbackSuccessfulFileSave() throws Exception {
        enableEditableSession("session-3", "lock-3");
        doThrow(new IllegalStateException("validator unavailable"))
                .when(xsdValidation).startValidationForXmlFile(eq(10L), isNull(), anyString());

        XmlSaveResponse response = service.overwrite(10L,
                request("<Root><A>new</A><B>keep</B></Root>", true, null, null, "session-3"));

        assertEquals("OVERWRITE", response.saveType());
        assertTrue(Files.readString(xmlPath).contains("<A>new</A>"));
        verify(audit).log(eq("XML_FILE_SAVE_XSD_VALIDATION_SKIPPED"), eq(10L), isNull(), isNull(), eq("tester"), eq("WARNING"), contains("validator unavailable"), isNull());
    }

    @Test
    void saveNewVersionWithCustomFileNameMustSwitchActiveEntityToNewFileAndKeepOriginalFile() throws Exception {
        enableEditableSession("session-4", "lock-4");
        XmlSaveRequest request = request("<Root><A>new</A><B>keep</B></Root>", false, "v2", "custom-version.xml", "session-4");

        XmlSaveResponse response = service.saveNewVersion(10L, request);

        Path newPath = tempDir.resolve("custom-version.xml");
        assertEquals("NEW_VERSION", response.saveType());
        assertEquals("custom-version.xml", response.fileName());
        assertEquals(newPath.toString(), response.targetFilePath());
        assertTrue(ExceptionSafeOperations.fileExists(newPath));
        assertTrue(ExceptionSafeOperations.fileExists(xmlPath), "Az elozo verzio fajlja nem torlodhet.");
        assertEquals("custom-version.xml", xmlFile.getFileName());
        assertEquals(newPath.toString(), xmlFile.getFilePath());
        assertEquals("<Root><A>old</A><B>keep</B></Root>", Files.readString(xmlPath));
        assertTrue(Files.readString(newPath).contains("<A>new</A>"));
    }

    @Test
    void saveNewVersionMustRejectDuplicateRepositoryFileNameBeforeFileCreation() {
        enableEditableSession("session-5", "lock-5");
        when(xmlFiles.existsByFileNameIgnoreCase("duplicate.xml")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.saveNewVersion(10L, request("<Root><A>x</A></Root>", false, null, "duplicate.xml", "session-5")));

        assertTrue(ex.getMessage().contains("Már létezik"));
        assertFalse(ExceptionSafeOperations.fileExists(tempDir.resolve("duplicate.xml")));
        verify(revisions, never()).save(any());
    }

    @Test
    void saveNewVersionMustRejectPathTraversalFileName() {
        enableEditableSession("session-6", "lock-6");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.saveNewVersion(10L, request("<Root><A>x</A></Root>", false, null, "../escape.xml", "session-6")));

        assertTrue(ex.getMessage().contains("elérési utat"));
        assertFalse(ExceptionSafeOperations.fileExists(tempDir.getParent().resolve("escape.xml")));
    }

    @Test
    void expiredLockMustRejectOverwriteWithoutTouchingOriginalFile() throws Exception {
        XmlFileSessionEntity session = editableSession("session-expired", "lock-expired");
        XmlFileLockEntity lock = editableLock("lock-expired");
        lock.setLockExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(sessions.findBySessionIdAndActiveTrue("session-expired")).thenReturn(Optional.of(session));
        when(locks.findByXmlFileIdAndStatus(10L, "ACTIVE")).thenReturn(Optional.of(lock));
        String before = Files.readString(xmlPath);

        assertThrows(AccessDeniedException.class,
                () -> service.overwrite(10L, request("<Root><A>new</A></Root>", false, null, null, "session-expired")));

        assertEquals(before, Files.readString(xmlPath));
        verify(revisions, never()).save(any());
        verify(xmlFiles, never()).save(any());
    }

    @Test
    void revisionListMustPreserveRepositoryOrderingAndNotLoadDiffs() {
        XmlFileRevisionEntity r2 = revision(102L, 2, "OVERWRITE");
        XmlFileRevisionEntity r1 = revision(101L, 1, "NEW_VERSION");
        when(revisions.findByXmlFileIdOrderByRevisionNoDesc(10L)).thenReturn(List.of(r2, r1));

        var result = service.revisions(10L);

        assertEquals(List.of(2, 1), result.stream().map(r -> r.revisionNo()).toList());
        assertTrue(result.stream().allMatch(r -> r.diffEntries().isEmpty()));
        verifyNoInteractions(diffs);
    }

    @Test
    void revisionDetailMustReturnPersistedDiffEntriesInRepositoryOrder() {
        XmlFileRevisionEntity revision = revision(105L, 5, "OVERWRITE");
        when(RepositoryAccess.findById(revisions, 105L)).thenReturn(Optional.of(revision));
        XmlFileDiffEntryEntity d1 = diff(201L, revision, "CHANGED", "/Root/A", "old", "new");
        XmlFileDiffEntryEntity d2 = diff(202L, revision, "ADDED", "/Root/C", null, "x");
        when(diffs.findByRevisionIdOrderByIdAsc(105L)).thenReturn(List.of(d1, d2));

        var result = service.revision(105L);

        assertEquals(2, result.diffEntries().size());
        assertEquals(List.of(201L, 202L), result.diffEntries().stream().map(e -> e.id()).toList());
        verify(accessPolicy).requireCurrentUserAccess(xmlFile);
    }

    private XmlSaveRequest request(String xml, boolean validate, String note, String newFileName) {
        return request(xml, validate, note, newFileName, "session-default");
    }

    private XmlSaveRequest request(String xml, boolean validate, String note, String newFileName, String sessionId) {
        return new XmlSaveRequest(xml, sessionId, validate, false, note, newFileName);
    }

    private void enableEditableSession(String sessionId, String lockToken) {
        XmlFileSessionEntity session = editableSession(sessionId, lockToken);
        XmlFileLockEntity lock = editableLock(lockToken);
        when(sessions.findBySessionIdAndActiveTrue(sessionId)).thenReturn(Optional.of(session));
        when(locks.findByXmlFileIdAndStatus(10L, "ACTIVE")).thenReturn(Optional.of(lock));
    }

    private XmlFileSessionEntity editableSession(String sessionId, String lockToken) {
        XmlFileSessionEntity session = new XmlFileSessionEntity();
        session.setXmlFile(xmlFile);
        session.setSessionId(sessionId);
        session.setActive(true);
        session.setReadOnly(false);
        session.setLockToken(lockToken);
        session.setCreatedBy("tester");
        session.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        return session;
    }

    private XmlFileLockEntity editableLock(String lockToken) {
        XmlFileLockEntity lock = new XmlFileLockEntity();
        lock.setXmlFile(xmlFile);
        lock.setLockedBy("tester");
        lock.setLockToken(lockToken);
        lock.setStatus("ACTIVE");
        lock.setLockedAt(LocalDateTime.now().minusMinutes(1));
        lock.setLockExpiresAt(LocalDateTime.now().plusMinutes(30));
        lock.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        return lock;
    }

    private XmlFileRevisionEntity revision(Long id, int no, String type) {
        XmlFileRevisionEntity revision = new XmlFileRevisionEntity();
        revision.setId(id);
        revision.setXmlFile(xmlFile);
        revision.setRevisionNo(no);
        revision.setSaveType(type);
        revision.setChangeCount(0);
        revision.setXsdValidationRequested(false);
        revision.setXsdValidationStatus("SKIPPED");
        revision.setCreatedAt(LocalDateTime.now());
        revision.setCreatedBy("tester");
        return revision;
    }

    private XmlFileDiffEntryEntity diff(Long id, XmlFileRevisionEntity revision, String type,
                                        String path, String oldValue, String newValue) {
        XmlFileDiffEntryEntity entry = new XmlFileDiffEntryEntity();
        entry.setId(id);
        entry.setRevision(revision);
        entry.setXmlFile(xmlFile);
        entry.setChangeType(type);
        entry.setXmlPath(path);
        entry.setOldValue(oldValue);
        entry.setNewValue(newValue);
        entry.setDisplayLabel(path.substring(path.lastIndexOf('/') + 1));
        return entry;
    }
}
