package hu.nav.m2m.submitter.service;

import hu.nav.m2m.submitter.support.RepositoryAccess;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import hu.nav.m2m.submitter.domain.GatewayMode;
import hu.nav.m2m.submitter.domain.InterfaceType;
import hu.nav.m2m.submitter.domain.M2mSubmission;
import hu.nav.m2m.submitter.domain.SubmissionStatus;
import hu.nav.m2m.submitter.repo.M2mAttachmentRepository;
import hu.nav.m2m.submitter.repo.M2mSubmissionEventRepository;
import hu.nav.m2m.submitter.repo.M2mSubmissionRepository;
import hu.nav.m2m.submitter.repo.XmlAttachmentReferenceRepository;
import hu.nav.m2m.submitter.service.nav.NavGateway;
import hu.nav.m2m.submitter.service.nav.NavRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceLifecycleTest {

    @Mock M2mSubmissionRepository submissionRepository;
    @Mock M2mAttachmentRepository attachmentRepository;
    @Mock XmlAttachmentReferenceRepository referenceRepository;
    @Mock M2mSubmissionEventRepository eventRepository;
    @Mock FileStorageService fileStorageService;
    @Mock XmlAttachmentReferenceExtractor referenceExtractor;
    @Mock SubmissionMapper mapper;
    @Mock NavGateway mockNavGateway;
    @Mock NavGateway realNavGateway;
    @Mock M2mSignatureService signatureService;
    @Mock NavRegistrationService navRegistrationService;
    @Mock RuntimeSignatureKeyService runtimeSignatureKeyService;
    @Mock XmlAttachmentReferenceInjector referenceInjector;
    @Mock XmlBizonylatMetadataExtractor metadataExtractor;

    private NavM2mProperties properties;
    private SubmissionService service;

    @BeforeEach
    void setUp() {
        properties = new NavM2mProperties();
        properties.getStatusPoll().setEnabled(true);
        properties.getStatusPoll().setInterval(Duration.ofMinutes(1));
        properties.getStatusPoll().setMaxAge(Duration.ofHours(24));
        properties.getStatusPoll().setMaxAttempts(3);
        properties.getStatusPoll().setBatchSize(10);
        service = new SubmissionService(
                submissionRepository,
                attachmentRepository,
                referenceRepository,
                eventRepository,
                fileStorageService,
                referenceExtractor,
                mapper,
                mockNavGateway,
                realNavGateway,
                signatureService,
                navRegistrationService,
                runtimeSignatureKeyService,
                referenceInjector,
                metadataExtractor,
                properties
        );
        lenient().when(submissionRepository.save(any(M2mSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void pollStatusKeepsIntermediateNavStatePollableAndSchedulesNextPoll() {
        M2mSubmission submission = pollableSubmission();
        when(RepositoryAccess.findById(submissionRepository, submission.getId())).thenReturn(Optional.of(submission));
        when(mockNavGateway.getStatus(eq("UGY-1"), anyString(), eq("corr-1")))
                .thenReturn(new NavGateway.StatusResult("BEKULDES_ALATT", null, "SIKERES", "folyamatban"));

        service.pollStatus(submission.getId());

        assertEquals(SubmissionStatus.SUBMISSION_IN_PROGRESS, submission.getInternalStatus());
        assertFalse(Boolean.TRUE.equals(submission.getM2mTerminal()));
        assertFalse(Boolean.TRUE.equals(submission.getM2mResubmittable()));
        assertNotNull(submission.getM2mNextPollAt());
        assertNotNull(submission.getM2mLastPollAt());
        assertEquals(1, submission.getM2mPollAttempts());
    }

    @Test
    void pollStatusSuccessfulNavResultCreatesSubmittedOkTerminalState() {
        M2mSubmission submission = pollableSubmission();
        when(RepositoryAccess.findById(submissionRepository, submission.getId())).thenReturn(Optional.of(submission));
        when(mockNavGateway.getStatus(eq("UGY-1"), anyString(), eq("corr-1")))
                .thenReturn(new NavGateway.StatusResult("SIKERESEN_BEKULDVE", "ERK-1", "SIKERES", "ok"));

        service.pollStatus(submission.getId());

        assertEquals(SubmissionStatus.SUBMITTED_OK, submission.getInternalStatus());
        assertTrue(Boolean.TRUE.equals(submission.getM2mTerminal()));
        assertFalse(Boolean.TRUE.equals(submission.getM2mResubmittable()));
        assertNull(submission.getM2mNextPollAt());
        assertNotNull(submission.getM2mFinalizedAt());
        assertEquals("ERK-1", submission.getNavErkeztetesiSzam());
    }

    @Test
    void pollStatusNonSuccessResultCodeCreatesSubmittedWithErrorTerminalState() {
        M2mSubmission submission = pollableSubmission();
        when(RepositoryAccess.findById(submissionRepository, submission.getId())).thenReturn(Optional.of(submission));
        when(mockNavGateway.getStatus(eq("UGY-1"), anyString(), eq("corr-1")))
                .thenReturn(new NavGateway.StatusResult("BEKULDES_ALATT", null, "HIBAS", "NAV hiba"));

        service.pollStatus(submission.getId());

        assertEquals(SubmissionStatus.SUBMITTED_WITH_ERROR, submission.getInternalStatus());
        assertTrue(Boolean.TRUE.equals(submission.getM2mTerminal()));
        assertFalse(Boolean.TRUE.equals(submission.getM2mResubmittable()));
        assertNull(submission.getM2mNextPollAt());
        assertNotNull(submission.getM2mFinalizedAt());
    }

    @Test
    void refreshDoesNotCallNavAgainForTerminalSubmission() {
        M2mSubmission submission = pollableSubmission();
        submission.setInternalStatus(SubmissionStatus.SUBMITTED_OK);
        submission.setM2mTerminal(true);
        when(RepositoryAccess.findById(submissionRepository, submission.getId())).thenReturn(Optional.of(submission));

        service.refresh(submission.getId());

        verifyNoInteractions(mockNavGateway, realNavGateway);
        assertEquals(SubmissionStatus.SUBMITTED_OK, submission.getInternalStatus());
    }

    @Test
    void markForSubmitIsRejectedForSubmittedOk() {
        M2mSubmission submission = pollableSubmission();
        submission.setInternalStatus(SubmissionStatus.SUBMITTED_OK);
        submission.setM2mTerminal(true);
        when(RepositoryAccess.findById(submissionRepository, submission.getId())).thenReturn(Optional.of(submission));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.markForSubmit(submission.getId()));

        assertTrue(exception.getMessage().contains("végállapot"));
        verify(submissionRepository, never()).save(submission);
    }

    @Test
    void submittedOkBlocksAttachmentMutation() {
        M2mSubmission submission = pollableSubmission();
        submission.setInternalStatus(SubmissionStatus.SUBMITTED_OK);
        submission.setM2mTerminal(true);
        when(RepositoryAccess.findById(submissionRepository, submission.getId())).thenReturn(Optional.of(submission));

        assertThrows(IllegalStateException.class,
                () -> service.addAttachments(submission.getId(), List.of()));

        verifyNoInteractions(fileStorageService);
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void priorSuccessfulSubmissionBlocksCreatingAnotherPackageForSameXmlFile() {
        long xmlFileId = 42L;
        when(submissionRepository.existsByXmlFileIdAndInternalStatus(xmlFileId, SubmissionStatus.SUBMITTED_OK))
                .thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.createAndOptionallySubmit("FORM", "1.0", null, GatewayMode.MOCK, null, List.of(), false, xmlFileId));

        assertTrue(exception.getMessage().contains("végállapot"));
        verify(submissionRepository, never()).save(any(M2mSubmission.class));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void markAndWithdrawSubmissionUpdatesLifecycleFields() {
        M2mSubmission submission = pollableSubmission();
        submission.setInternalStatus(SubmissionStatus.CREATED);
        submission.setM2mTerminal(false);
        when(RepositoryAccess.findById(submissionRepository, submission.getId())).thenReturn(Optional.of(submission));

        service.markForSubmit(submission.getId());
        assertEquals(SubmissionStatus.MARKED_FOR_SUBMISSION, submission.getInternalStatus());
        assertNotNull(submission.getM2mSubmitMarkedAt());
        assertFalse(Boolean.TRUE.equals(submission.getM2mTerminal()));
        assertTrue(Boolean.TRUE.equals(submission.getM2mResubmittable()));

        service.withdrawSubmitMark(submission.getId());
        assertEquals(SubmissionStatus.SUBMISSION_MARK_WITHDRAWN, submission.getInternalStatus());
        assertNull(submission.getM2mNextPollAt());
        assertTrue(Boolean.TRUE.equals(submission.getM2mResubmittable()));
    }

    @Test
    void pollDueSubmissionsStopsAfterConfiguredMaxAgeWithoutCallingNav() {
        M2mSubmission submission = pollableSubmission();
        submission.setM2mSubmittedAt(Instant.now().minus(Duration.ofHours(2)));
        properties.getStatusPoll().setMaxAge(Duration.ofMinutes(30));
        when(submissionRepository.findByInternalStatusInAndM2mTerminalFalseAndM2mNextPollAtLessThanEqualOrderByM2mNextPollAtAsc(
                anyList(), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(submission));

        service.pollDueSubmissions();

        assertEquals(SubmissionStatus.SUBMISSION_TECHNICAL_FAILED, submission.getInternalStatus());
        assertEquals("POLL_MAX_AGE_EXPIRED", submission.getResultCode());
        assertNull(submission.getM2mNextPollAt());
        assertTrue(Boolean.TRUE.equals(submission.getM2mResubmittable()));
        verifyNoInteractions(mockNavGateway, realNavGateway);
    }

    @Test
    void pollDueSubmissionsMarksTechnicalFailureAfterMaxAttempts() {
        M2mSubmission submission = pollableSubmission();
        submission.setM2mPollAttempts(2);
        when(submissionRepository.findByInternalStatusInAndM2mTerminalFalseAndM2mNextPollAtLessThanEqualOrderByM2mNextPollAtAsc(
                anyList(), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(submission));
        when(RepositoryAccess.findById(submissionRepository, submission.getId())).thenReturn(Optional.of(submission));
        when(mockNavGateway.getStatus(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("kapcsolati hiba"));

        service.pollDueSubmissions();

        assertEquals(SubmissionStatus.SUBMISSION_TECHNICAL_FAILED, submission.getInternalStatus());
        assertEquals(3, submission.getM2mPollAttempts());
        assertEquals("POLL_TECHNICAL_FAILED", submission.getResultCode());
        assertNull(submission.getM2mNextPollAt());
        assertTrue(Boolean.TRUE.equals(submission.getM2mResubmittable()));
    }

    @Test
    void pollingDisabledDoesNotQueryRepository() {
        properties.getStatusPoll().setEnabled(false);

        service.pollDueSubmissions();

        verify(submissionRepository, never())
                .findByInternalStatusInAndM2mTerminalFalseAndM2mNextPollAtLessThanEqualOrderByM2mNextPollAtAsc(
                        anyList(), any(Instant.class), any(Pageable.class));
    }

    private M2mSubmission pollableSubmission() {
        M2mSubmission submission = new M2mSubmission();
        submission.setId(UUID.randomUUID());
        submission.setInterfaceType(InterfaceType.BIZONYLAT_API);
        submission.setGatewayMode(GatewayMode.MOCK);
        submission.setInternalStatus(SubmissionStatus.SUBMISSION_IN_PROGRESS);
        submission.setNavUgyAzonosito("UGY-1");
        submission.setMessageId("msg-1");
        submission.setCorrelationId("corr-1");
        submission.setM2mTerminal(false);
        submission.setM2mResubmittable(false);
        submission.setM2mNextPollAt(Instant.now().minusSeconds(1));
        submission.setM2mSubmittedAt(Instant.now());
        submission.setM2mPollAttempts(0);
        return submission;
    }
}
