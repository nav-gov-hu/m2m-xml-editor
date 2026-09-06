package hu.nav.m2m.submitter.repo;

import hu.nav.m2m.submitter.domain.M2mSubmissionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository az M2M eseménynapló bejegyzéseinek eléréséhez.
 */
public interface M2mSubmissionEventRepository extends JpaRepository<M2mSubmissionEvent, UUID> {
    /** A beküldés eseménynaplóját időrendi sorrendben adja vissza. */
    List<M2mSubmissionEvent> findBySubmissionIdOrderByCreatedAtAsc(UUID submissionId);
}
