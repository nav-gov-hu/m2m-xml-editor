package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.web.processing.dto.ProcessingJobDto;
import hu.gov.nav.xsdparsertool.web.processing.service.ProcessingJobService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.LargeXmlFragmentSaveRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileLockEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileSessionEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileLockRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LargeXmlFragmentSaveServiceFunctionalTest {

    @TempDir Path tempDir;
    @Mock XmlFileRepository xmlFiles;
    @Mock XmlFileSessionRepository sessions;
    @Mock XmlFileLockRepository locks;
    @Mock CurrentUserService currentUser;
    @Mock ProcessingJobService jobs;
    @Mock LargeXmlMultiformPageService pages;
    @Mock XmlMutationGuard mutationGuard;

    private XmlFileEntity file;
    private Path source;
    private LargeXmlFragmentSaveService service;

    @BeforeEach
    void setUp() throws Exception {
        source = tempDir.resolve("large.xml");
        Files.writeString(source, """
                <Doc_26HIPAK>
                  <Form_26HIPAKA><Field_ID>MAIN</Field_ID></Form_26HIPAKA>
                  <Form_26HIPAKM><Field_ID>M1</Field_ID></Form_26HIPAKM>
                  <Form_26HIPAKM><Field_ID>M2</Field_ID></Form_26HIPAKM>
                  <Form_26HIPAKM><Field_ID>M3</Field_ID></Form_26HIPAKM>
                </Doc_26HIPAK>
                """);
        file = new XmlFileEntity();
        file.setId(77L);
        file.setFilePath(source.toString());
        file.setFileName("large.xml");

        XmlFileSessionEntity session = new XmlFileSessionEntity();
        session.setXmlFile(file);
        session.setSessionId("session-1");
        session.setReadOnly(false);
        session.setCreatedBy("operator");
        session.setLockToken("lock-1");

        XmlFileLockEntity lock = new XmlFileLockEntity();
        lock.setXmlFile(file);
        lock.setStatus("ACTIVE");
        lock.setLockToken("lock-1");
        lock.setLockExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(currentUser.getCurrentUsername()).thenReturn("operator");
        when(sessions.findBySessionIdAndActiveTrue("session-1")).thenReturn(Optional.of(session));
        when(locks.findByXmlFileIdAndStatus(77L, "ACTIVE")).thenReturn(Optional.of(lock));
        when(RepositoryAccess.findById(xmlFiles, 77L)).thenReturn(Optional.of(file));
        lenient().when(xmlFiles.save(any(XmlFileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobs.startJob(eq("LARGE_XML_FRAGMENT_SAVE"), eq(77L), anyString()))
                .thenReturn(new ProcessingJobDto("job-1", 77L, "LARGE_XML_FRAGMENT_SAVE", "CREATED", 0,
                        "", null, null, null, null, "operator", null, null));
        lenient().when(jobs.isCancelRequested(anyString())).thenReturn(false);

        XmlFileStorageProperties storage = new XmlFileStorageProperties();
        storage.setBackupDir(tempDir.resolve("backup").toString());
        service = new LargeXmlFragmentSaveService(xmlFiles, sessions, locks, currentUser, jobs, pages, storage, mutationGuard);
    }

    @Test
    void savesOnlyRequestedOccurrenceAndKeepsOtherFormPartsUntouched() throws Exception {
        String replacement = "<Form_26HIPAKM><Field_ID>M2-UPDATED</Field_ID></Form_26HIPAKM>";
        LargeXmlFragmentSaveRequest request = new LargeXmlFragmentSaveRequest(
                "Form_26HIPAKM", 2L, replacement, "session-1", Files.size(source), Files.getLastModifiedTime(source).toMillis(), "test");

        service.start(77L, request);

        verify(jobs, timeout(5000)).finish(eq("job-1"), contains("sikeresen"));
        String saved = Files.readString(source);
        assertTrue(saved.contains("<Field_ID>MAIN</Field_ID>"));
        assertTrue(saved.contains("<Field_ID>M1</Field_ID>"));
        assertTrue(saved.contains("<Field_ID>M2-UPDATED</Field_ID>"));
        assertTrue(saved.contains("<Field_ID>M3</Field_ID>"));
        assertFalse(saved.contains("<Field_ID>M2</Field_ID>"));
        verify(pages).refreshAfterSave(77L, "Form_26HIPAKM", 2L, replacement);
        verify(mutationGuard, atLeast(2)).requireMutable(77L);
    }

    @Test
    void successfulSaveCreatesBackupBeforeReplacingSource() throws Exception {
        String original = Files.readString(source);
        String replacement = "<Form_26HIPAKM><Field_ID>M1-UPDATED</Field_ID></Form_26HIPAKM>";
        LargeXmlFragmentSaveRequest request = new LargeXmlFragmentSaveRequest(
                "Form_26HIPAKM", 1L, replacement, "session-1", Files.size(source), Files.getLastModifiedTime(source).toMillis(), null);

        service.start(77L, request);

        verify(jobs, timeout(5000)).finish(eq("job-1"), contains("sikeresen"));
        Path backupDir = tempDir.resolve("backup").resolve("77");
        try (var files = Files.list(backupDir)) {
            Path backup = files.filter(Files::isRegularFile).findFirst().orElseThrow();
            assertEquals(original, Files.readString(backup));
        }
    }

    @Test
    void wrongFragmentRootFailsJobWithoutChangingSource() throws Exception {
        String original = Files.readString(source);
        LargeXmlFragmentSaveRequest request = new LargeXmlFragmentSaveRequest(
                "Form_26HIPAKM", 1L, "<Form_OTHER><Field_ID>X</Field_ID></Form_OTHER>", "session-1",
                Files.size(source), Files.getLastModifiedTime(source).toMillis(), null);

        service.start(77L, request);

        verify(jobs, timeout(5000)).fail(eq("job-1"), contains("gyökéreleme"));
        assertEquals(original, Files.readString(source));
        verify(pages, never()).refreshAfterSave(anyLong(), anyString(), anyLong(), anyString());
    }
}
