package hu.nav.m2m.submitter.service;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically polls NAV status for M2M submissions that are in a non-terminal NAV state.
 *
 * Controlled by properties:
 *   nav.m2m.status-poll.enabled=true|false
 *   nav.m2m.status-poll.fixed-delay-ms=60000
 */
@Component
public class M2mSubmissionStatusPollScheduler {
    private static final Logger log = LoggerFactory.getLogger(M2mSubmissionStatusPollScheduler.class);

    private final SubmissionService submissionService;
    private final NavM2mProperties properties;

    /**
     * Létrehozza a(z) {@code M2mSubmissionStatusPollScheduler} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param submissionService a művelethez átadott {@code submissionService} érték
     * @param properties az M2M külső konfiguráció
     */
    public M2mSubmissionStatusPollScheduler(SubmissionService submissionService, NavM2mProperties properties) {
        this.submissionService = submissionService;
        this.properties = properties;
    }

    /**
     * Az időzítő egy futásában elindítja az esedékes M2M státuszpolling feldolgozást.
     */
    @Scheduled(fixedDelayString = "${nav.m2m.status-poll.fixed-delay-ms:60000}")
    public void run() {
        if (properties.getStatusPoll() == null || !properties.getStatusPoll().isEnabled()) {
            return;
        }
        try {
            submissionService.pollDueSubmissions();
        } catch (Exception e) {
            log.warn("M2M automata státusz polling hiba: {}", e.getMessage(), e);
        }
    }
}
