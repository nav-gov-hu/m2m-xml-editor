package hu.nav.m2m.submitter.service;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import hu.nav.m2m.submitter.domain.M2mAttachment;
import hu.nav.m2m.submitter.domain.M2mSubmission;
import hu.nav.m2m.submitter.domain.XmlAttachmentReference;
import hu.nav.m2m.submitter.dto.AttachmentReferenceDto;
import hu.nav.m2m.submitter.dto.SubmissionResponse;
import hu.nav.m2m.submitter.dto.UploadedAttachmentDto;
import hu.nav.m2m.submitter.repo.M2mAttachmentRepository;
import hu.nav.m2m.submitter.repo.XmlAttachmentReferenceRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Az M2M domain entitásokat kliensoldali DTO-vá alakítja, és kiszámítja a megjelenítéshez szükséges származtatott állapotokat.
 */
@Component
public class SubmissionMapper {
    private final XmlAttachmentReferenceRepository referenceRepository;
    private final M2mAttachmentRepository attachmentRepository;
    private final NavM2mProperties properties;

    /**
     * Létrehozza a(z) {@code SubmissionMapper} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param referenceRepository a művelethez átadott {@code referenceRepository} érték
     * @param attachmentRepository a művelethez átadott {@code attachmentRepository} érték
     * @param properties az M2M külső konfiguráció
     */
    public SubmissionMapper(
            XmlAttachmentReferenceRepository referenceRepository,
            M2mAttachmentRepository attachmentRepository,
            NavM2mProperties properties
    ) {
        this.referenceRepository = referenceRepository;
        this.attachmentRepository = attachmentRepository;
        this.properties = properties;
    }

    /**
     * A bemeneti domain/transport adatokból a következő feldolgozási réteg által igényelt reprezentációt állítja elő.
     *
     * @param s a művelethez átadott {@code s} érték
     * @return a művelet eredménye
     */
    public SubmissionResponse toResponse(M2mSubmission s) {
        List<AttachmentReferenceDto> refs = referenceRepository.findBySubmissionIdOrderBySequenceNo(s.getId())
                .stream().map(this::toReferenceDto).toList();
        List<UploadedAttachmentDto> attachments = attachmentRepository.findBySubmissionIdOrderByCreatedAtAsc(s.getId())
                .stream().map(this::toAttachmentDto).toList();
        return new SubmissionResponse(
                s.getId(), s.getInterfaceType(), s.getBizonylatTipus(), s.getBizonylatVerzio(),
                s.getGatewayMode(),
                s.getXmlFileName(), s.getXmlFileSize(), s.getXmlSha256Hex(), s.getCompression(), s.getNavFileId(),
                s.getNavUgyAzonosito(), s.getNavErkeztetesiSzam(), s.getNavStatus(), s.getInternalStatus(),
                s.getResultCode(), s.getResultMessage(), s.getNavBefogadasIdopontja(), s.getNavMegjegyzes(),
                s.getNavValidaciosHibak(), s.getNavResponseBody(), s.getNavHttpStatus(),
                s.getSubmissionStartedAt(), s.getSubmissionFinishedAt(), s.getSubmissionDurationMs(),
                s.getMessageId(), s.getCorrelationId(),
                s.getCreatedAt(), s.getUpdatedAt(),
                s.getM2mSubmitMarkedAt(), s.getM2mSubmittedAt(), s.getM2mFinalizedAt(),
                s.getM2mNextPollAt(), s.getM2mLastPollAt(), s.getM2mPollAttempts(),
                s.getM2mTerminal(), s.getM2mResubmittable(),
                s.getNavValidacioUgyAzonosito(), s.getNavValidacioStatusz(),
                s.getNavValidacioResultCode(), s.getNavValidacioResultMessage(),
                s.getNavValidacioHibak(), s.getNavValidaciosTanusitvany(), s.getNavValidacioResponseBody(),
                s.getNavValidacioStartedAt(), s.getNavValidacioFinishedAt(), s.getNavValidacioLastCheckedAt(),
                s.getNavValidacioMessageId(), s.getNavValidacioCorrelationId(),
                s.getNavValidacioPayloadSha256(), isFastTrackEligible(s), s.getFastTrackSubmissionUsed(),
                s.getNavKalkulacioUgyAzonosito(), s.getNavKalkulacioStatusz(),
                s.getNavKalkulacioResultCode(), s.getNavKalkulacioResultMessage(),
                s.getNavKalkulacioHibaKod(), s.getNavKalkulacioHibaUzenet(),
                s.getNavKalkulacioMezoAzonosito(), s.getNavKalkulacioSzabalyAzonosito(),
                s.getNavKalkulacioTomorites(), s.getNavKalkulaltXml(), s.getNavKalkulacioResponseBody(),
                s.getNavKalkulacioStartedAt(), s.getNavKalkulacioFinishedAt(), s.getNavKalkulacioLastCheckedAt(),
                s.getNavKalkulacioMessageId(), s.getNavKalkulacioCorrelationId(),
                refs, attachments);
    }

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param s a művelethez átadott {@code s} érték
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private boolean isFastTrackEligible(M2mSubmission s) {
        return "SIKERES".equalsIgnoreCase(s.getNavValidacioStatusz())
                && "SIKERES".equalsIgnoreCase(s.getNavValidacioResultCode())
                && s.getNavValidaciosTanusitvany() != null && !s.getNavValidaciosTanusitvany().isBlank()
                && s.getNavValidacioPayloadSha256() != null
                && s.getNavValidacioPayloadSha256().equalsIgnoreCase(s.getXmlSha256Hex());
    }

    /**
     * A bemeneti domain/transport adatokból a következő feldolgozási réteg által igényelt reprezentációt állítja elő.
     *
     * @param r a művelethez átadott {@code r} érték
     * @return a művelet eredménye
     */
    private AttachmentReferenceDto toReferenceDto(XmlAttachmentReference r) {
        return new AttachmentReferenceDto(r.getSequenceNo(), r.getElementName(), r.getFileId(), r.getFileName(), r.getFileSize());
    }

    /**
     * A bemeneti domain/transport adatokból a következő feldolgozási réteg által igényelt reprezentációt állítja elő.
     *
     * @param a a művelethez átadott {@code a} érték
     * @return a művelet eredménye
     */
    private UploadedAttachmentDto toAttachmentDto(M2mAttachment a) {
        AttachmentLifecycleEvaluator.Evaluation lifecycle =
                AttachmentLifecycleEvaluator.evaluate(a, properties, Instant.now());
        return new UploadedAttachmentDto(a.getId(), a.getOriginalFileName(), a.getFileSize(), a.getSha256Hex(), a.getNavFileId(),
                a.isXmlReferencePresent(), a.getNavUploadedAt(), a.getNavExpiresAt(), a.getNavLastRefreshedAt(),
                a.getNavUploadResultCode(), a.getNavUploadResultMessage(),
                lifecycle.state().name(), lifecycle.label(), lifecycle.refreshAllowed(),
                lifecycle.reason(), lifecycle.localFileAvailable());
    }
}
