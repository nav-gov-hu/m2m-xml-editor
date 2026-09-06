package hu.nav.m2m.submitter.repo;

import hu.nav.m2m.submitter.domain.M2mAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository az M2M csatolmány entitások perzisztens eléréséhez.
 */
public interface M2mAttachmentRepository extends JpaRepository<M2mAttachment, UUID> {
    /** A beküldéshez tartozó összes csatolmányt lekéri. */
    List<M2mAttachment> findBySubmissionId(UUID submissionId);
    /** A beküldés csatolmányait létrehozási sorrendben adja vissza. */
    List<M2mAttachment> findBySubmissionIdOrderByCreatedAtAsc(UUID submissionId);
    /** Az XML-fájlhoz tartozó beküldések csatolmányait létrehozási sorrendben adja vissza. */
    List<M2mAttachment> findBySubmissionXmlFileIdOrderByCreatedAtAsc(Long xmlFileId);
    /** Megszámolja az XML-fájlhoz kapcsolódó M2M csatolmányokat. */
    long countBySubmissionXmlFileId(Long xmlFileId);

    /** A csatolmányt csak akkor adja vissza, ha a megadott beküldéshez tartozik; a beküldést join fetch segítségével együtt tölti be. */
    @Query("select a from M2mAttachment a join fetch a.submission s where a.id = :attachmentId and s.id = :submissionId")
    Optional<M2mAttachment> findForSubmission(@Param("attachmentId") UUID attachmentId,
                                              @Param("submissionId") UUID submissionId);

    /** A csatolmányt csak akkor adja vissza, ha a megadott XML-fájlhoz kapcsolódik. */
    @Query("select a from M2mAttachment a join fetch a.submission s where a.id = :attachmentId and s.xmlFileId = :xmlFileId")
    Optional<M2mAttachment> findForXmlFile(@Param("attachmentId") UUID attachmentId,
                                           @Param("xmlFileId") Long xmlFileId);

    /** A csatolmányt csak a megadott beküldésen belül törli, így idegen beküldés azonosítója nem használható törlésre. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from M2mAttachment a where a.id = :attachmentId and a.submission.id = :submissionId")
    int deleteByIdAndSubmissionId(@Param("attachmentId") UUID attachmentId,
                                  @Param("submissionId") UUID submissionId);
}
