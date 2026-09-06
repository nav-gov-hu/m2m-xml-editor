package hu.gov.nav.xsdparsertool.web.processing.service;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.processing.dto.ProcessingJobDto;
import hu.gov.nav.xsdparsertool.web.processing.entity.ProcessingJobEntity;
import hu.gov.nav.xsdparsertool.web.processing.entity.ProcessingJobStatus;
import hu.gov.nav.xsdparsertool.web.processing.repository.ProcessingJobRepository;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;

@ExtendWith(MockitoExtension.class)
class ProcessingJobServiceCoverageTest {

    @Mock
    private ProcessingJobRepository jobs;
    @Mock
    private XmlFileRepository xmlFiles;
    @Mock
    private CurrentUserService currentUser;
    @Mock
    private AuditLogService auditLog;

    private ProcessingJobService service;

    @BeforeEach
    void setUp() {
        service = new ProcessingJobService(jobs, xmlFiles, currentUser, auditLog);
        lenient().when(jobs.save(any(ProcessingJobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void startupClosesAbandonedActiveJobsAndUsesSystemForMissingCreator() {
        ProcessingJobEntity first = job("job-1", ProcessingJobStatus.RUNNING, "alice");
        first.setXmlFile(xmlFile(11L));
        ProcessingJobEntity second = job("job-2", ProcessingJobStatus.PENDING, null);
        when(jobs.findByStatusIn(any())).thenReturn(List.of(first, second));

        service.closeAbandonedActiveJobsOnStartup();

        assertEquals(ProcessingJobStatus.FAILED.name(), first.getStatus());
        assertEquals(ProcessingJobStatus.FAILED.name(), second.getStatus());
        assertNotNull(first.getFinishedAt());
        assertNotNull(second.getFinishedAt());
        assertEquals("alice", first.getUpdatedBy());
        assertEquals("system", second.getUpdatedBy());
        verify(jobs).save(first);
        verify(jobs).save(second);
        verify(auditLog).log(eq("PROCESSING_JOB_FAILED"), eq(11L), eq("job-1"), eq(null), eq("alice"), eq("ERROR"), any(), eq(null));
        verify(auditLog).log(eq("PROCESSING_JOB_FAILED"), eq(null), eq("job-2"), eq(null), eq(null), eq("ERROR"), any(), eq(null));
    }

    @Test
    void readApisMapEntitiesAndMissingJobFails() {
        ProcessingJobEntity active = job("active", ProcessingJobStatus.RUNNING, "alice");
        ProcessingJobEntity recent = job("recent", ProcessingJobStatus.FINISHED, "bob");
        when(jobs.findByJobId("active")).thenReturn(Optional.of(active));
        when(jobs.findByJobId("missing")).thenReturn(Optional.empty());
        when(jobs.findFirstByStatusInOrderByCreatedAtAsc(any())).thenReturn(Optional.of(active));
        when(jobs.findTop20ByOrderByCreatedAtDesc()).thenReturn(List.of(recent, active));

        assertEquals("active", service.getJob("active").jobId());
        assertEquals("active", service.getActiveJobOrNull().jobId());
        assertEquals(List.of("recent", "active"), service.listRecentJobs().stream().map(ProcessingJobDto::jobId).toList());
        assertThrows(IllegalArgumentException.class, () -> service.getJob("missing"));

        when(jobs.findFirstByStatusInOrderByCreatedAtAsc(any())).thenReturn(Optional.empty());
        assertNull(service.getActiveJobOrNull());
    }

    @Test
    void startJobAppliesDefaultsUnknownUserAndOptionalXmlLink() {
        when(jobs.findFirstByStatusInOrderByCreatedAtAsc(any())).thenReturn(Optional.empty());
        when(currentUser.getCurrentUsername()).thenReturn("   ");
        XmlFileEntity xml = xmlFile(21L);
        when(RepositoryAccess.findById(xmlFiles, 21L)).thenReturn(Optional.of(xml));

        ProcessingJobDto dto = service.startJob(" ", 21L, null);

        assertTrue(dto.jobId().startsWith("JOB-"));
        assertEquals("UNKNOWN", dto.jobType());
        assertEquals(ProcessingJobStatus.PENDING.name(), dto.status());
        assertEquals(0, dto.progressPercent());
        assertEquals("Feldolgozás várakozik.", dto.progressMessage());
        assertEquals("unknown", dto.createdBy());
        assertEquals(21L, dto.xmlFileId());
        verify(auditLog).log(eq("PROCESSING_JOB_STARTED"), eq(21L), eq(dto.jobId()), eq(null), eq("unknown"), eq("SUCCESS"), any(), eq(null));
    }

    @Test
    void startJobRejectsExistingActiveJobAndMissingXml() {
        ProcessingJobEntity active = job("busy", ProcessingJobStatus.RUNNING, "alice");
        when(jobs.findFirstByStatusInOrderByCreatedAtAsc(any())).thenReturn(Optional.of(active));

        ProcessingJobService.ActiveProcessingJobException ex = assertThrows(
                ProcessingJobService.ActiveProcessingJobException.class,
                () -> service.startJob("XSD", null, "start"));
        assertEquals("busy", ex.getJobId());
        assertEquals("TYPE", ex.getJobType());
        assertEquals(ProcessingJobStatus.RUNNING.name(), ex.getStatus());

        when(jobs.findFirstByStatusInOrderByCreatedAtAsc(any())).thenReturn(Optional.empty());
        when(currentUser.getCurrentUsername()).thenReturn("alice");
        when(RepositoryAccess.findById(xmlFiles, 999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.startJob("XSD", 999L, "start"));
    }

    @Test
    void requestCancelTransitionsRunningJobAndLeavesTerminalJobUntouched() {
        ProcessingJobEntity running = job("run", ProcessingJobStatus.RUNNING, "alice");
        running.setXmlFile(xmlFile(31L));
        when(jobs.findByJobId("run")).thenReturn(Optional.of(running));
        when(currentUser.getCurrentUsername()).thenReturn("bob");

        ProcessingJobDto cancelled = service.requestCancel("run");

        assertEquals(ProcessingJobStatus.CANCELLED.name(), cancelled.status());
        assertNotNull(cancelled.requestedCancelAt());
        assertNotNull(cancelled.finishedAt());
        assertEquals("bob", running.getUpdatedBy());
        verify(auditLog).log(eq("PROCESSING_JOB_CANCEL_REQUESTED"), eq(31L), eq("run"), eq(null), eq("bob"), eq("SUCCESS"), any(), eq(null));
        verify(auditLog).log(eq("PROCESSING_JOB_CANCELLED"), eq(31L), eq("run"), eq(null), eq("bob"), eq("WARNING"), any(), eq(null));

        ProcessingJobEntity terminal = job("done", ProcessingJobStatus.FINISHED, "alice");
        when(jobs.findByJobId("done")).thenReturn(Optional.of(terminal));
        assertEquals(ProcessingJobStatus.FINISHED.name(), service.requestCancel("done").status());
        verify(jobs, never()).save(terminal);
    }

    @Test
    void markRunningSetsStartOnlyOnceAndIgnoresTerminalJob() {
        ProcessingJobEntity pending = job("pending", ProcessingJobStatus.PENDING, "alice");
        when(jobs.findByJobId("pending")).thenReturn(Optional.of(pending));

        ProcessingJobDto running = service.markRunning("pending", "running");
        LocalDateTime startedAt = running.startedAt();
        assertNotNull(startedAt);
        assertEquals(ProcessingJobStatus.RUNNING.name(), running.status());

        service.markRunning("pending", "still running");
        assertEquals(startedAt, pending.getStartedAt());

        ProcessingJobEntity failed = job("failed", ProcessingJobStatus.FAILED, "alice");
        when(jobs.findByJobId("failed")).thenReturn(Optional.of(failed));
        assertEquals(ProcessingJobStatus.FAILED.name(), service.markRunning("failed", "ignored").status());
        verify(jobs, never()).save(failed);
    }

    @Test
    void updateProgressClampsPercentAndDoesNotReviveCancelledJob() {
        ProcessingJobEntity job = job("progress", ProcessingJobStatus.PENDING, "alice");
        when(jobs.findByJobId("progress")).thenReturn(Optional.of(job));

        assertEquals(0, service.updateProgress("progress", -20, "low").progressPercent());
        assertEquals(100, service.updateProgress("progress", 120, "high").progressPercent());
        assertEquals(ProcessingJobStatus.RUNNING.name(), job.getStatus());
        assertNotNull(job.getStartedAt());

        ProcessingJobEntity cancelled = job("cancelled", ProcessingJobStatus.CANCELLED, "alice");
        cancelled.setProgressPercent(40);
        when(jobs.findByJobId("cancelled")).thenReturn(Optional.of(cancelled));
        ProcessingJobDto unchanged = service.updateProgress("cancelled", 90, "ignored");
        assertEquals(40, unchanged.progressPercent());
        assertEquals(ProcessingJobStatus.CANCELLED.name(), unchanged.status());
        verify(jobs, never()).save(cancelled);
    }

    @Test
    void isCancelRequestedHandlesRequestedCancelledRunningAndMissing() {
        when(jobs.findByJobId("requested")).thenReturn(Optional.of(job("requested", ProcessingJobStatus.CANCEL_REQUESTED, "alice")));
        when(jobs.findByJobId("cancelled")).thenReturn(Optional.of(job("cancelled", ProcessingJobStatus.CANCELLED, "alice")));
        when(jobs.findByJobId("running")).thenReturn(Optional.of(job("running", ProcessingJobStatus.RUNNING, "alice")));
        when(jobs.findByJobId("missing")).thenReturn(Optional.empty());

        assertTrue(service.isCancelRequested("requested"));
        assertTrue(service.isCancelRequested("cancelled"));
        assertFalse(service.isCancelRequested("running"));
        assertFalse(service.isCancelRequested("missing"));
    }

    @Test
    void finishUsesDefaultMessageAuditsAndCannotFinishCancelledJob() {
        ProcessingJobEntity running = job("finish", ProcessingJobStatus.RUNNING, "alice");
        running.setXmlFile(xmlFile(41L));
        when(jobs.findByJobId("finish")).thenReturn(Optional.of(running));

        ProcessingJobDto finished = service.finish("finish", " ");
        assertEquals(ProcessingJobStatus.FINISHED.name(), finished.status());
        assertEquals(100, finished.progressPercent());
        assertEquals("Feldolgozás befejezve.", finished.progressMessage());
        assertNotNull(finished.finishedAt());
        verify(auditLog).log(eq("PROCESSING_JOB_FINISHED"), eq(41L), eq("finish"), eq(null), eq("alice"), eq("SUCCESS"), any(), eq(null));

        ProcessingJobEntity cancelled = job("cancelled-finish", ProcessingJobStatus.CANCELLED, "alice");
        when(jobs.findByJobId("cancelled-finish")).thenReturn(Optional.of(cancelled));
        assertEquals(ProcessingJobStatus.CANCELLED.name(), service.finish("cancelled-finish", "ignored").status());
        verify(jobs, never()).save(cancelled);
    }

    @Test
    void cancelAndFailPersistTerminalStatesAndAuditThem() {
        ProcessingJobEntity running = job("cancel", ProcessingJobStatus.RUNNING, "alice");
        when(jobs.findByJobId("cancel")).thenReturn(Optional.of(running));
        ProcessingJobDto cancelled = service.cancel("cancel", null);
        assertEquals(ProcessingJobStatus.CANCELLED.name(), cancelled.status());
        assertEquals("Feldolgozás megszakítva.", cancelled.progressMessage());
        verify(auditLog).log(eq("PROCESSING_JOB_CANCELLED"), eq(null), eq("cancel"), eq(null), eq("alice"), eq("WARNING"), any(), eq(null));

        ProcessingJobEntity failing = job("fail", ProcessingJobStatus.RUNNING, "bob");
        when(jobs.findByJobId("fail")).thenReturn(Optional.of(failing));
        ProcessingJobDto failed = service.fail("fail", "boom");
        assertEquals(ProcessingJobStatus.FAILED.name(), failed.status());
        assertEquals("boom", failed.errorMessage());
        assertEquals("Feldolgozás hibával leállt.", failed.progressMessage());
        verify(auditLog).log(eq("PROCESSING_JOB_FAILED"), eq(null), eq("fail"), eq(null), eq("bob"), eq("ERROR"), eq("boom"), eq(null));
    }

    @Test
    void failDoesNotOverwriteCancelledJob() {
        ProcessingJobEntity cancelled = job("already-cancelled", ProcessingJobStatus.CANCELLED, "alice");
        when(jobs.findByJobId("already-cancelled")).thenReturn(Optional.of(cancelled));

        ProcessingJobDto result = service.fail("already-cancelled", "late failure");

        assertEquals(ProcessingJobStatus.CANCELLED.name(), result.status());
        assertNull(result.errorMessage());
        verify(jobs, never()).save(cancelled);
    }

    private static ProcessingJobEntity job(String jobId, ProcessingJobStatus status, String createdBy) {
        ProcessingJobEntity entity = new ProcessingJobEntity();
        entity.setJobId(jobId);
        entity.setJobType("TYPE");
        entity.setStatus(status.name());
        entity.setProgressPercent(0);
        entity.setProgressMessage("message");
        entity.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        entity.setUpdatedAt(LocalDateTime.now().minusMinutes(1));
        entity.setCreatedBy(createdBy);
        entity.setUpdatedBy(createdBy);
        return entity;
    }

    private static XmlFileEntity xmlFile(Long id) {
        XmlFileEntity entity = new XmlFileEntity();
        entity.setId(id);
        return entity;
    }
}
