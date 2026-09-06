package hu.nav.m2m.submitter.repo;

import hu.nav.m2m.submitter.domain.M2mSubmission;
import hu.nav.m2m.submitter.domain.SubmissionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository az M2M beküldési entitások és polling lekérdezések eléréséhez.
 */
public interface M2mSubmissionRepository extends JpaRepository<M2mSubmission, UUID> {
    /** A beküldéseket létrehozási idő szerint csökkenő sorrendben adja vissza. */
    List<M2mSubmission> findAllByOrderByCreatedAtDesc();
    /** Az adott XML SHA-256 hashhez tartozó beküldéseket adja vissza, legújabbal kezdve. */
    List<M2mSubmission> findByXmlSha256HexOrderByCreatedAtDesc(String xmlSha256Hex);
    /** Az XML-fájlhoz tartozó legutóbb módosított beküldést keresi meg. */
    java.util.Optional<M2mSubmission> findFirstByXmlFileIdOrderByUpdatedAtDesc(Long xmlFileId);
    /** Eldönti, hogy az XML-fájlhoz létezik-e a megadott belső állapotú beküldés. */
    boolean existsByXmlFileIdAndInternalStatus(Long xmlFileId, SubmissionStatus internalStatus);

    /** A megadott állapotú, nem terminális és pollingra esedékes beküldéseket adja vissza a következő polling idő szerint növekvően. */
    List<M2mSubmission> findByInternalStatusInAndM2mTerminalFalseAndM2mNextPollAtLessThanEqualOrderByM2mNextPollAtAsc(
            List<SubmissionStatus> statuses,
            Instant nextPollAt,
            Pageable pageable
    );
}
