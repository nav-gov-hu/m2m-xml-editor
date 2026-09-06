package hu.nav.m2m.submitter.service;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class M2mSubmissionStatusPollSchedulerTest {

    @Test
    void disabledSchedulerDoesNotPoll() {
        SubmissionService service = mock(SubmissionService.class);
        NavM2mProperties properties = new NavM2mProperties();
        properties.getStatusPoll().setEnabled(false);
        M2mSubmissionStatusPollScheduler scheduler = new M2mSubmissionStatusPollScheduler(service, properties);

        scheduler.run();

        verifyNoInteractions(service);
    }

    @Test
    void enabledSchedulerDelegatesToSubmissionService() {
        SubmissionService service = mock(SubmissionService.class);
        NavM2mProperties properties = new NavM2mProperties();
        properties.getStatusPoll().setEnabled(true);
        M2mSubmissionStatusPollScheduler scheduler = new M2mSubmissionStatusPollScheduler(service, properties);

        scheduler.run();

        verify(service).pollDueSubmissions();
    }

    @Test
    void schedulerContainsPollingFailureAndKeepsScheduledTaskAlive() {
        SubmissionService service = mock(SubmissionService.class);
        NavM2mProperties properties = new NavM2mProperties();
        properties.getStatusPoll().setEnabled(true);
        doThrow(new IllegalStateException("temporary failure")).when(service).pollDueSubmissions();
        M2mSubmissionStatusPollScheduler scheduler = new M2mSubmissionStatusPollScheduler(service, properties);

        assertDoesNotThrow(scheduler::run);
        verify(service).pollDueSubmissions();
    }
}
