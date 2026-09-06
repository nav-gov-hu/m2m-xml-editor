package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileLockProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.OpenXmlFileResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.RenewLockResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlSessionStateDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileLockEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileLockReleaseRequestEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileSessionEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileLockReleaseRequestRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileLockRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileSessionRepository;

@ExtendWith(MockitoExtension.class)
class XmlFileSessionServiceCoverageTest {
    @Mock XmlFileRepository files;
    @Mock XmlFileSessionRepository sessions;
    @Mock XmlFileLockRepository locks;
    @Mock XmlFileLockReleaseRequestRepository releaseRequests;
    @Mock CurrentUserService currentUser;
    @Mock AuditLogService audit;
    @Mock XmlFileService xmlFileService;

    XmlFileLockProperties properties;
    XmlFileSessionService service;

    @BeforeEach
    void setUp() {
        properties = new XmlFileLockProperties();
        properties.setTimeoutMinutes(10);
        properties.setRenewMinutes(5);
        service = new XmlFileSessionService(files, sessions, locks, releaseRequests, properties, currentUser, audit, xmlFileService);
        lenient().when(currentUser.getCurrentUsername()).thenReturn("alice");
        lenient().when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(locks.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void openReadOnlyCreatesSessionWithoutLock() {
        XmlFileEntity file = file(1L, "READY", "RESOLVED");
        when(RepositoryAccess.findById(files, 1L)).thenReturn(Optional.of(file));
        when(sessions.findByCreatedByAndActiveTrue("alice")).thenReturn(List.of());

        OpenXmlFileResponse response = service.open(1L, true);

        assertTrue(response.readOnly());
        assertFalse(response.locked());
        assertNotNull(response.sessionId());
        assertNotNull(response.message());
        verify(sessions).save(argThat(s -> Boolean.TRUE.equals(s.getReadOnly()) && "NO_HTTP_REQUEST".equals(s.getBrowserSessionId())));
        verifyNoInteractions(locks);
    }

    @Test
    void openRegisteredRetriesResolutionBeforeOpen() throws Exception {
        XmlFileEntity registered = file(11L, "REGISTERED", "NOT_RESOLVED");
        XmlFileEntity resolved = file(11L, "READY", "RESOLVED");
        resolved.setXsdPath("C:/repo/xsd/FORM/1.0/FORM.xsd");
        when(RepositoryAccess.findById(files, 11L)).thenReturn(Optional.of(registered));
        when(xmlFileService.refreshResolutionIfNeeded(11L)).thenReturn(resolved);
        when(sessions.findByCreatedByAndActiveTrue("alice")).thenReturn(List.of());

        OpenXmlFileResponse response = service.open(11L, true);

        assertEquals("READY", response.file().status());
        verify(xmlFileService).refreshResolutionIfNeeded(11L);
    }

    @Test
    void openEditableAcquiresNewLockAndSession() {
        XmlFileEntity file = file(2L, "XSD_INVALID", null);
        when(RepositoryAccess.findById(files, 2L)).thenReturn(Optional.of(file));
        when(sessions.findByCreatedByAndActiveTrue("alice")).thenReturn(List.of());
        when(locks.findByXmlFileIdAndStatus(2L, "ACTIVE")).thenReturn(Optional.empty());
        when(locks.findByXmlFileId(2L)).thenReturn(Optional.empty());

        OpenXmlFileResponse response = service.open(2L, false);

        assertFalse(response.readOnly());
        assertTrue(response.locked());
        assertEquals("alice", response.lockOwner());
        verify(locks).save(argThat(l -> "ACTIVE".equals(l.getStatus()) && "alice".equals(l.getLockedBy()) && l.getLockToken().startsWith("LOCK-")));
    }

    @Test
    void openRejectsArchivedAndUnresolvedFiles() throws Exception {
        XmlFileEntity archived = file(3L, "READY", "RESOLVED");
        archived.setArchived(true);
        when(RepositoryAccess.findById(files, 3L)).thenReturn(Optional.of(archived));
        assertThrows(IllegalStateException.class, () -> service.open(3L, true));

        XmlFileEntity unresolved = file(4L, "READY", "FAILED");
        when(RepositoryAccess.findById(files, 4L)).thenReturn(Optional.of(unresolved));
        when(xmlFileService.refreshResolutionIfNeeded(4L)).thenReturn(unresolved);
        assertThrows(IllegalStateException.class, () -> service.open(4L, true));
    }

    @Test
    void renewOwnLockExtendsExpiry() {
        XmlFileEntity file = file(5L, "READY", "RESOLVED");
        XmlFileLockEntity lock = lock(file, "alice", "NO_HTTP_REQUEST", LocalDateTime.now().plusMinutes(1));
        when(RepositoryAccess.findById(files, 5L)).thenReturn(Optional.of(file));
        when(locks.findByXmlFileIdAndStatus(5L, "ACTIVE")).thenReturn(Optional.of(lock));

        RenewLockResponse response = service.renew(5L);

        assertEquals(5L, response.xmlFileId());
        assertEquals("alice", response.lockedBy());
        assertTrue(response.lockExpiresAt().isAfter(LocalDateTime.now().plusMinutes(3)));
    }

    @Test
    void renewOtherBrowserLockFailsWithOwnerDetails() {
        XmlFileEntity file = file(6L, "READY", "RESOLVED");
        XmlFileLockEntity lock = lock(file, "alice", "OTHER", LocalDateTime.now().plusMinutes(2));
        lock.setLockClientIp("10.0.0.7");
        when(RepositoryAccess.findById(files, 6L)).thenReturn(Optional.of(file));
        when(locks.findByXmlFileIdAndStatus(6L, "ACTIVE")).thenReturn(Optional.of(lock));

        XmlFileSessionService.FileLockedByOtherUserException ex = assertThrows(
                XmlFileSessionService.FileLockedByOtherUserException.class, () -> service.renew(6L));
        assertTrue(ex.getLockedBy().contains("10.0.0.7"));
        assertNotNull(ex.getLockExpiresAt());
    }

    @Test
    void closeBySessionIdClosesOwnedEditableSessionAndReleasesMatchingLock() {
        XmlFileEntity file = file(7L, "READY", "RESOLVED");
        XmlFileSessionEntity session = session(file, "S-7", false, "alice", "TOKEN");
        XmlFileLockEntity lock = lock(file, "alice", "NO_HTTP_REQUEST", LocalDateTime.now().plusMinutes(5));
        lock.setLockToken("TOKEN");
        when(sessions.findBySessionIdAndActiveTrue("S-7")).thenReturn(Optional.of(session));
        when(locks.findByXmlFileIdAndStatus(7L, "ACTIVE")).thenReturn(Optional.of(lock));

        service.close(7L, "done", "S-7");

        assertFalse(session.getActive());
        assertEquals("done", session.getCloseReason());
        assertEquals("RELEASED", lock.getStatus());
    }

    @Test
    void closeBySessionIdRejectsWrongFileAndWrongOwner() {
        XmlFileEntity file = file(8L, "READY", "RESOLVED");
        XmlFileSessionEntity session = session(file, "S-8", false, "alice", "T");
        when(sessions.findBySessionIdAndActiveTrue("S-8")).thenReturn(Optional.of(session));
        assertThrows(IllegalStateException.class, () -> service.close(99L, null, "S-8"));

        session.setCreatedBy("bob");
        assertThrows(IllegalStateException.class, () -> service.close(8L, null, "S-8"));
    }

    @Test
    void sessionStateCoversMissingActiveUnknownAndClosedCases() {
        XmlSessionStateDto missing = service.sessionState(9L, " ");
        assertFalse(missing.active());
        assertNull(missing.sessionId());

        XmlFileEntity file = file(9L, "READY", "RESOLVED");
        XmlFileSessionEntity active = session(file, "ACTIVE", true, "alice", null);
        when(sessions.findBySessionIdAndActiveTrue("ACTIVE")).thenReturn(Optional.of(active));
        when(locks.findByXmlFileIdAndStatus(9L, "ACTIVE")).thenReturn(Optional.of(lock(file, "alice", "NO_HTTP_REQUEST", LocalDateTime.now().plusMinutes(1))));
        assertTrue(service.sessionState(9L, "ACTIVE").lockActive());

        when(sessions.findBySessionIdAndActiveTrue("NONE")).thenReturn(Optional.empty());
        when(RepositoryAccess.findAll(sessions)).thenReturn(List.of());
        assertTrue(service.sessionState(9L, "NONE").closeReason().contains("nem található"));

        XmlFileSessionEntity closed = session(file, "CLOSED", true, "alice", null);
        closed.setActive(false);
        closed.setClosedBy("admin");
        closed.setClosedAt(LocalDateTime.now());
        closed.setCloseReason("forced");
        when(sessions.findBySessionIdAndActiveTrue("CLOSED")).thenReturn(Optional.empty());
        when(RepositoryAccess.findAll(sessions)).thenReturn(List.of(closed));
        XmlSessionStateDto state = service.sessionState(9L, "CLOSED");
        assertEquals("admin", state.closedBy());
        assertEquals("forced", state.closeReason());
    }

    @Test
    void existingExpiredLockIsExpiredThenReplaced() throws Exception {
        XmlFileEntity file = file(10L, "REGISTERED", "RESOLVED");
        XmlFileLockEntity old = lock(file, "bob", "B", LocalDateTime.now().minusMinutes(1));
        old.setId(100L);
        XmlFileLockEntity reusable = new XmlFileLockEntity();
        reusable.setId(101L);
        reusable.setXmlFile(file);
        when(RepositoryAccess.findById(files, 10L)).thenReturn(Optional.of(file));
        when(xmlFileService.refreshResolutionIfNeeded(10L)).thenReturn(file);
        when(sessions.findByCreatedByAndActiveTrue("alice")).thenReturn(List.of());
        when(locks.findByXmlFileIdAndStatus(10L, "ACTIVE")).thenReturn(Optional.of(old));
        when(locks.findByXmlFileId(10L)).thenReturn(Optional.of(reusable));

        OpenXmlFileResponse response = service.open(10L, false);

        assertTrue(response.locked());
        assertEquals("EXPIRED", old.getStatus());
        assertEquals("ACTIVE", reusable.getStatus());
        assertEquals("alice", reusable.getLockedBy());
    }


    @Test
    void requestAndListLockReleaseRequestsMapOwnershipAndMessages() {
        XmlFileEntity file = file(30L, "READY", "RESOLVED");
        XmlFileLockEntity lock = lock(file, "bob", "BOB-BROWSER", LocalDateTime.now().plusMinutes(5));
        when(RepositoryAccess.findById(files, 30L)).thenReturn(Optional.of(file));
        when(locks.findByXmlFileIdAndStatus(30L, "ACTIVE")).thenReturn(Optional.of(lock));
        when(releaseRequests.save(any(XmlFileLockReleaseRequestEntity.class))).thenAnswer(inv -> {
            XmlFileLockReleaseRequestEntity entity = inv.getArgument(0);
            entity.setId(300L);
            return entity;
        });

        var created = service.requestLockRelease(30L, "  kérlek zárd le  ");
        assertEquals(300L, created.id());
        assertEquals("alice", created.requesterUsername());
        assertEquals("bob", created.ownerUsername());
        assertEquals("PENDING", created.status());
        assertEquals("kérlek zárd le", created.message());

        XmlFileLockReleaseRequestEntity pending = releaseRequest(301L, file, "alice", "NO_HTTP_REQUEST", "PENDING");
        pending.setRequesterUsername("carol");
        when(releaseRequests.findByOwnerUsernameAndOwnerBrowserSessionIdAndStatusOrderByRequestedAtDesc(
                "alice", "NO_HTTP_REQUEST", "PENDING")).thenReturn(List.of(pending));
        assertEquals(1, service.pendingLockReleaseRequestsForCurrentSession().size());

        XmlFileLockReleaseRequestEntity mine1 = releaseRequest(302L, file, "bob", "OTHER", "PENDING");
        XmlFileLockReleaseRequestEntity mine2 = releaseRequest(303L, file, "bob", "OTHER", "REJECTED");
        mine1.setRequesterUsername("alice");
        mine1.setRequesterBrowserSessionId("NO_HTTP_REQUEST");
        mine2.setRequesterUsername("alice");
        mine2.setRequesterBrowserSessionId("NO_HTTP_REQUEST");
        when(releaseRequests.findByRequesterUsernameAndRequesterBrowserSessionIdOrderByRequestedAtDesc(
                "alice", "NO_HTTP_REQUEST")).thenReturn(List.of(mine1, mine2));
        assertEquals(2, service.myLockReleaseRequests().size());
    }

    @Test
    void acceptLockReleaseRequestClosesSessionsAndReleasesActiveLock() {
        XmlFileEntity file = file(31L, "READY", "RESOLVED");
        XmlFileLockReleaseRequestEntity request = releaseRequest(310L, file, "alice", "NO_HTTP_REQUEST", "PENDING");
        XmlFileSessionEntity active = session(file, "S-31", false, "alice", "LOCK-31");
        XmlFileLockEntity lock = lock(file, "alice", "NO_HTTP_REQUEST", LocalDateTime.now().plusMinutes(5));
        lock.setLockToken("LOCK-31");
        when(RepositoryAccess.findById(releaseRequests, 310L)).thenReturn(Optional.of(request));
        when(releaseRequests.save(any(XmlFileLockReleaseRequestEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(RepositoryAccess.findAll(sessions)).thenReturn(List.of(active));
        when(locks.findByXmlFileIdAndStatus(31L, "ACTIVE")).thenReturn(Optional.of(lock));

        var dto = service.acceptLockReleaseRequest(310L, "rendben");

        assertEquals("ACCEPTED", dto.status());
        assertEquals("rendben", dto.responseMessage());
        assertEquals("alice", dto.closedBy());
        assertFalse(active.getActive());
        assertEquals("RELEASED", lock.getStatus());
    }

    @Test
    void rejectLockReleaseRequestRequiresPendingMatchingOwner() {
        XmlFileEntity file = file(32L, "READY", "RESOLVED");
        XmlFileLockReleaseRequestEntity request = releaseRequest(320L, file, "alice", "NO_HTTP_REQUEST", "PENDING");
        when(RepositoryAccess.findById(releaseRequests, 320L)).thenReturn(Optional.of(request));
        when(releaseRequests.save(any(XmlFileLockReleaseRequestEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("REJECTED", service.rejectLockReleaseRequest(320L, "most nem").status());

        request.setStatus("ACCEPTED");
        assertThrows(IllegalStateException.class, () -> service.rejectLockReleaseRequest(320L, "x"));

        request.setStatus("PENDING");
        request.setOwnerUsername("bob");
        assertThrows(IllegalStateException.class, () -> service.rejectLockReleaseRequest(320L, "x"));
    }

    @Test
    void forceCloseReleaseRequestClosesSessionsAndForceReleasesLock() {
        XmlFileEntity file = file(33L, "READY", "RESOLVED");
        XmlFileLockReleaseRequestEntity request = releaseRequest(330L, file, "bob", "OTHER", "PENDING");
        XmlFileSessionEntity active = session(file, "S-33", false, "bob", "LOCK-33");
        XmlFileLockEntity lock = lock(file, "bob", "OTHER", LocalDateTime.now().plusMinutes(5));
        when(RepositoryAccess.findById(releaseRequests, 330L)).thenReturn(Optional.of(request));
        when(releaseRequests.save(any(XmlFileLockReleaseRequestEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(RepositoryAccess.findAll(sessions)).thenReturn(List.of(active));
        when(locks.findByXmlFileIdAndStatus(33L, "ACTIVE")).thenReturn(Optional.of(lock));

        var dto = service.forceCloseLockReleaseRequest(330L, "admin döntés");

        assertEquals("FORCE_CLOSED", dto.status());
        assertNotNull(dto.forceClosedAt());
        assertEquals("alice", dto.closedBy());
        assertFalse(active.getActive());
        assertEquals("FORCE_RELEASED", lock.getStatus());
    }

    @Test
    void forceReleaseLockClosesAllActiveSessions() {
        XmlFileEntity file = file(34L, "READY", "RESOLVED");
        XmlFileLockEntity lock = lock(file, "bob", "OTHER", LocalDateTime.now().plusMinutes(5));
        XmlFileSessionEntity same = session(file, "S-34", false, "bob", "T");
        XmlFileSessionEntity otherFile = session(file(35L, "READY", "RESOLVED"), "S-35", false, "bob", "T");
        when(RepositoryAccess.findById(files, 34L)).thenReturn(Optional.of(file));
        when(locks.findByXmlFileIdAndStatus(34L, "ACTIVE")).thenReturn(Optional.of(lock));
        when(RepositoryAccess.findAll(sessions)).thenReturn(List.of(same, otherFile));

        service.forceReleaseLock(34L);

        assertEquals("FORCE_RELEASED", lock.getStatus());
        assertFalse(same.getActive());
        assertTrue(otherFile.getActive());
    }

    @Test
    void closeWithoutSessionIdClosesOnlyCurrentBrowserSessionsAndOwnLock() {
        XmlFileEntity file = file(36L, "READY", "RESOLVED");
        XmlFileSessionEntity ownBrowser = session(file, "S-36", false, "alice", "T36");
        XmlFileSessionEntity otherBrowser = session(file, "S-36B", false, "alice", "T36B");
        otherBrowser.setBrowserSessionId("OTHER");
        XmlFileLockEntity lock = lock(file, "alice", "NO_HTTP_REQUEST", LocalDateTime.now().plusMinutes(5));
        when(sessions.findByCreatedByAndActiveTrue("alice")).thenReturn(List.of(ownBrowser, otherBrowser));
        when(locks.findByXmlFileIdAndStatus(36L, "ACTIVE")).thenReturn(Optional.of(lock));

        service.close(36L, null);

        assertFalse(ownBrowser.getActive());
        assertTrue(otherBrowser.getActive());
        assertEquals("RELEASED", lock.getStatus());
    }

    @Test
    void openReusesOwnActiveLockAndRejectsOtherOwnerLock() {
        XmlFileEntity file = file(37L, "READY", "RESOLVED");
        when(RepositoryAccess.findById(files, 37L)).thenReturn(Optional.of(file));
        when(sessions.findByCreatedByAndActiveTrue("alice")).thenReturn(List.of());
        XmlFileLockEntity own = lock(file, "alice", "NO_HTTP_REQUEST", LocalDateTime.now().plusMinutes(2));
        when(locks.findByXmlFileIdAndStatus(37L, "ACTIVE")).thenReturn(Optional.of(own));

        OpenXmlFileResponse reused = service.open(37L, false);
        assertTrue(reused.locked());
        verify(audit).log(eq("XML_FILE_LOCK_REUSED"), eq(37L), isNull(), isNull(), eq("alice"), eq("SUCCESS"),
                contains("újrahasználva"), contains("browserSessionId=NO_HTTP_REQUEST"));

        XmlFileLockEntity other = lock(file, "bob", "OTHER", LocalDateTime.now().plusMinutes(2));
        when(locks.findByXmlFileIdAndStatus(37L, "ACTIVE")).thenReturn(Optional.of(other));
        assertThrows(XmlFileSessionService.FileLockedByOtherUserException.class, () -> service.open(37L, false));
    }

    @Test
    void missingResourcesAndReleaseRequestsReportDomainErrors() {
        when(RepositoryAccess.findById(files, 40L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.open(40L, true));
        assertThrows(IllegalArgumentException.class, () -> service.requestLockRelease(40L, null));
        when(RepositoryAccess.findById(releaseRequests, 400L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.acceptLockReleaseRequest(400L, null));
        assertThrows(IllegalArgumentException.class, () -> service.rejectLockReleaseRequest(400L, null));
        assertThrows(IllegalArgumentException.class, () -> service.forceCloseLockReleaseRequest(400L, null));
    }

    private static XmlFileLockReleaseRequestEntity releaseRequest(Long id, XmlFileEntity file, String owner,
                                                                   String ownerBrowser, String status) {
        XmlFileLockReleaseRequestEntity request = new XmlFileLockReleaseRequestEntity();
        request.setId(id);
        request.setXmlFile(file);
        request.setRequesterUsername("requester");
        request.setRequesterBrowserSessionId("REQUESTER-BROWSER");
        request.setOwnerUsername(owner);
        request.setOwnerBrowserSessionId(ownerBrowser);
        request.setStatus(status);
        request.setMessage("message");
        request.setRequestedAt(LocalDateTime.now());
        return request;
    }

    private static XmlFileEntity file(Long id, String status, String resolutionStatus) {
        XmlFileEntity file = new XmlFileEntity();
        file.setId(id);
        file.setFileName("test-" + id + ".xml");
        file.setOriginalFileName(file.getFileName());
        file.setFilePath("test-" + id + ".xml");
        file.setFileSizeBytes(10L);
        file.setStatus(status);
        file.setResolutionStatus(resolutionStatus);
        file.setArchived(false);
        file.setCreatedAt(LocalDateTime.now());
        return file;
    }

    private static XmlFileLockEntity lock(XmlFileEntity file, String owner, String browser, LocalDateTime expires) {
        XmlFileLockEntity lock = new XmlFileLockEntity();
        lock.setId(1L);
        lock.setXmlFile(file);
        lock.setLockedBy(owner);
        lock.setLockedAt(LocalDateTime.now());
        lock.setLockExpiresAt(expires);
        lock.setLockToken("TOKEN");
        lock.setLockBrowserSessionId(browser);
        lock.setStatus("ACTIVE");
        lock.setCreatedAt(LocalDateTime.now());
        return lock;
    }

    private static XmlFileSessionEntity session(XmlFileEntity file, String id, boolean readOnly, String owner, String token) {
        XmlFileSessionEntity session = new XmlFileSessionEntity();
        session.setXmlFile(file);
        session.setSessionId(id);
        session.setActive(true);
        session.setReadOnly(readOnly);
        session.setCreatedBy(owner);
        session.setCreatedAt(LocalDateTime.now());
        session.setBrowserSessionId("NO_HTTP_REQUEST");
        session.setLockToken(token);
        return session;
    }
}
