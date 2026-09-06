package hu.nav.m2m.submitter.repo;

import hu.nav.m2m.submitter.domain.XmlAttachmentReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository az XML csatolmányhivatkozások eléréséhez.
 */
public interface XmlAttachmentReferenceRepository extends JpaRepository<XmlAttachmentReference, UUID> {
    /** A beküldés XML csatolmányhivatkozásait az XML-beli sorrendet őrző sorszám szerint adja vissza. */
    List<XmlAttachmentReference> findBySubmissionIdOrderBySequenceNo(UUID submissionId);
    /** Törli a megadott beküldéshez tartozó korábban kinyert XML csatolmányhivatkozásokat. */
    void deleteBySubmissionId(UUID submissionId);
}
