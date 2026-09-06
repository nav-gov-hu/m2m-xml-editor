package hu.nav.m2m.submitter.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.nav.m2m.submitter.support.RepositoryAccess;

import hu.nav.m2m.submitter.domain.*;
import hu.nav.m2m.submitter.config.NavM2mProperties;
import hu.nav.m2m.submitter.dto.SubmissionResponse;
import hu.nav.m2m.submitter.dto.EventDto;
import hu.nav.m2m.submitter.dto.ValidationErrorDetailsResponse;
import hu.nav.m2m.submitter.dto.ValidationErrorItem;
import hu.nav.m2m.submitter.service.nav.NavRegistrationService;
import hu.nav.m2m.submitter.repo.M2mAttachmentRepository;
import hu.nav.m2m.submitter.repo.M2mSubmissionEventRepository;
import hu.nav.m2m.submitter.repo.M2mSubmissionRepository;
import hu.nav.m2m.submitter.repo.XmlAttachmentReferenceRepository;
import hu.nav.m2m.submitter.service.nav.NavGateway;
import hu.nav.m2m.submitter.service.nav.NavOperationExceptionFactory;
import hu.nav.m2m.submitter.service.nav.audit.NavHttpAuditHolder;
import hu.nav.m2m.submitter.service.nav.audit.NavHttpTrace;
import hu.nav.m2m.submitter.service.nav.audit.NavHttpAuditFormatter;
import jakarta.transaction.Transactional;
import hu.nav.m2m.submitter.util.Sha256Util;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.Base64;
import java.text.Normalizer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Az M2M beküldési üzleti folyamat fő orchestrátora: életciklus, csatolmányok, NAV feltöltés/bizonylat/validáció/kalkuláció, státuszpolling és naplózás összehangolása.
 */
@Service
public class SubmissionService {
    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);
    private final M2mSubmissionRepository submissionRepository;
    private final M2mAttachmentRepository attachmentRepository;
    private final XmlAttachmentReferenceRepository referenceRepository;
    private final M2mSubmissionEventRepository eventRepository;
    private final FileStorageService fileStorageService;
    private final XmlAttachmentReferenceExtractor referenceExtractor;
    private final SubmissionMapper mapper;
    private final NavGateway mockNavGateway;
    private final NavGateway realNavGateway;
    private final M2mSignatureService signatureService;
    private final NavRegistrationService navRegistrationService;
    private final RuntimeSignatureKeyService runtimeSignatureKeyService;
    private final XmlAttachmentReferenceInjector referenceInjector;
    private final XmlBizonylatMetadataExtractor metadataExtractor;
    private final NavM2mProperties properties;

    /**
     * Létrehozza az M2M beküldési folyamat fő orchestrátorát a perzisztencia-, fájlkezelési,
     * XML-metaadat-, aláírási és NAV gateway függőségekkel.
     *
     * <p>A konstruktor csak a függőségeket rögzíti; hálózati hívást vagy perzisztens
     * állapotváltozást nem indít.</p>
     */
    public SubmissionService(M2mSubmissionRepository submissionRepository,
                             M2mAttachmentRepository attachmentRepository,
                             XmlAttachmentReferenceRepository referenceRepository,
                             M2mSubmissionEventRepository eventRepository,
                             FileStorageService fileStorageService,
                             XmlAttachmentReferenceExtractor referenceExtractor,
                             SubmissionMapper mapper,
                             @Qualifier("mockNavGateway") NavGateway mockNavGateway,
                             @Qualifier("realNavGateway") NavGateway realNavGateway,
                             M2mSignatureService signatureService,
                             NavRegistrationService navRegistrationService,
                             RuntimeSignatureKeyService runtimeSignatureKeyService,
                             XmlAttachmentReferenceInjector referenceInjector,
                             XmlBizonylatMetadataExtractor metadataExtractor,
                             NavM2mProperties properties) {
        this.submissionRepository = submissionRepository;
        this.attachmentRepository = attachmentRepository;
        this.referenceRepository = referenceRepository;
        this.eventRepository = eventRepository;
        this.fileStorageService = fileStorageService;
        this.referenceExtractor = referenceExtractor;
        this.mapper = mapper;
        this.mockNavGateway = mockNavGateway;
        this.realNavGateway = realNavGateway;
        this.signatureService = signatureService;
        this.navRegistrationService = navRegistrationService;
        this.runtimeSignatureKeyService = runtimeSignatureKeyService;
        this.referenceInjector = referenceInjector;
        this.metadataExtractor = metadataExtractor;
        this.properties = properties;
    }

    /**
     * Létrehozza vagy előkészíti az XML-hez tartozó beküldési munkamenetet, feldolgozza az opcionális fájlokat és a kérés szerint elindíthatja a beküldési folyamatot.
     *
     * @param bizonylatTipus a művelethez átadott {@code bizonylatTipus} érték
     * @param bizonylatVerzio a művelethez átadott {@code bizonylatVerzio} érték
     * @param compression a művelethez átadott {@code compression} érték
     * @param gatewayMode a művelethez átadott {@code gatewayMode} érték
     * @param xml a művelethez átadott {@code xml} érték
     * @param attachments a feldolgozandó csatolmányok
     * @param submitNow a művelethez átadott {@code submitNow} érték
     * @param xmlFileId az érintett XML-fájl adatbázis-azonosítója
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse createAndOptionallySubmit(String bizonylatTipus,
                                                        String bizonylatVerzio,
                                                        CompressionType compression,
                                                        GatewayMode gatewayMode,
                                                        MultipartFile xml,
                                                        List<MultipartFile> attachments,
                                                        boolean submitNow,
                                                        Long xmlFileId) {
        try {
            ensureXmlFileNotSuccessfullyFinalized(xmlFileId);
            M2mSubmission submission = new M2mSubmission();
            submission.setId(UUID.randomUUID());
            submission.setInterfaceType(InterfaceType.BIZONYLAT_API);
            submission.setXmlFileId(xmlFileId);
            // A requestben kapott típus/verzió csak ellenőrzési hint; fájlútvonalat vagy perzisztált állapotot nem vezérelhet.
            submission.setGatewayMode(gatewayMode == null ? GatewayMode.MOCK : gatewayMode);
            submission.setInternalStatus(SubmissionStatus.CREATED);
            resetM2mLifecycleForNewSubmission(submission);
            submission.setCompression(compression == null ? CompressionType.NONE : compression);
            submission.setMessageId(UUID.randomUUID().toString());
            submission.setCorrelationId(UUID.randomUUID().toString());
            submission = submissionRepository.save(submission);

            FileStorageService.StoredFile storedXml = fileStorageService.store(submission.getId(), xml, "xml");
            submission.setXmlFileName(storedXml.originalFileName());
            submission.setXmlStoragePath(storedXml.storagePath());
            submission.setXmlFileSize(storedXml.fileSize());
            submission.setXmlSha256Hex(storedXml.sha256Hex());
            detectAndApplyBizonylatMetadata(submission, Path.of(storedXml.storagePath()));
            verifyRequestedMetadata(bizonylatTipus, bizonylatVerzio, submission);
            submissionRepository.save(submission);

            saveXmlReferences(submission, Path.of(storedXml.storagePath()));
            validateAttachmentFilesAgainstXml(submission, attachments);
            saveUploadedAttachments(submission, attachments);
            logEvent(submission, "CREATED", "LOCAL", "OK", "XML és opcionális csatolmányok letárolva");

            if (submitNow) {
                submission.setInternalStatus(SubmissionStatus.MARKED_FOR_SUBMISSION);
                submissionRepository.save(submission);
                logEvent(submission, "M2M_MARKED_FOR_SUBMISSION", "LOCAL_STATE", "OK", "submitNow=true miatt automatikus beküldésre jelölés.");
                submission = submitInternal(submission);
            }
            return mapper.toResponse(submission);
        } catch (Exception e) {
            throw new IllegalStateException("Beküldési csomag létrehozása sikertelen: " + e.getMessage(), e);
        }
    }

    /**
     * Összeveti a kérésben kapott dokumentumtípus/verzió metaadatot az XML-ből ténylegesen feloldott Bizonylat metaadattal, és eltérés esetén megakadályozza a hibás route használatát.
     *
     * @param requestedType a művelethez átadott {@code requestedType} érték
     * @param requestedVersion a művelethez átadott {@code requestedVersion} érték
     * @param submission az aktuális M2M beküldési entitás
     */
    private void verifyRequestedMetadata(String requestedType, String requestedVersion, M2mSubmission submission) {
        if (requestedType != null && !requestedType.isBlank()
                && !requestedType.equals(submission.getBizonylatTipus())) {
            throw new IllegalArgumentException("A megadott bizonylatTipus nem egyezik az XML-ből felismert típussal.");
        }
        if (requestedVersion != null && !requestedVersion.isBlank()
                && !requestedVersion.equals(submission.getBizonylatVerzio())) {
            throw new IllegalArgumentException("A megadott bizonylatVerzio nem egyezik az XML-ből felismert verzióval.");
        }
    }

    /**
     * A(z) {@code addAttachments} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param id a művelethez átadott {@code id} érték
     * @param attachments a feldolgozandó csatolmányok
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse addAttachments(UUID id, List<MultipartFile> attachments) {
        M2mSubmission submission = RepositoryAccess.findById(submissionRepository, id).orElseThrow();
        ensureNotSuccessfullyFinalized(submission);
        try {
            validateAttachmentFilesAgainstXml(submission, attachments);
            saveUploadedAttachments(submission, attachments);
            logEvent(submission, "ATTACHMENTS_ADDED", "LOCAL", "OK",
                    "Új csatolmányok hozzáadva a meglévő csomaghoz. count=" + countNonEmptyFiles(attachments));
            return mapper.toResponse(submissionRepository.save(submission));
        } catch (Exception e) {
            throw new IllegalStateException("Csatolmányok hozzáadása sikertelen: " + e.getMessage(), e);
        }
    }

    /**
     * Elindítja a beküldéshez tartozó üzleti folyamatot a szükséges preflight ellenőrzésekkel.
     *
     * @param id a művelethez átadott {@code id} érték
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse submit(UUID id) {
        M2mSubmission submission = RepositoryAccess.findById(submissionRepository, id).orElseThrow();
        ensureNotSuccessfullyFinalized(submission);
        ensureResubmissionAllowed(submission);
        ensureAttachmentsValidForSubmission(submission);
        if (submission.getInternalStatus() != SubmissionStatus.MARKED_FOR_SUBMISSION) {
            logEvent(submission, "M2M_SUBMIT_BLOCKED_BY_STATUS", "SUBMIT", "BLOCKED", "Csak MARKED_FOR_SUBMISSION státuszú XML/csomag küldhető be. Jelenlegi státusz: " + submission.getInternalStatus());
            throw new IllegalStateException("Csak beküldésre megjelölt XML/csomag küldhető be. Jelenlegi státusz: " + submission.getInternalStatus());
        }
        if (Boolean.TRUE.equals(submission.getM2mTerminal())) {
            logEvent(submission, "M2M_SUBMIT_BLOCKED_BY_STATUS", "SUBMIT", "TERMINAL", "Végállapotban lévő csomag nem küldhető újra. Jelenlegi státusz: " + submission.getInternalStatus());
            throw new IllegalStateException("Végállapotban lévő csomag nem küldhető újra. Jelenlegi státusz: " + submission.getInternalStatus());
        }
        return mapper.toResponse(submitInternal(submission));
    }

    /**
     * Beküldésre jelöli az XML-hez tartozó M2M munkamenetet, ha az életciklus és csatolmányállapot ezt engedi.
     *
     * @param id a művelethez átadott {@code id} érték
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse markForSubmit(UUID id) {
        M2mSubmission submission = RepositoryAccess.findById(submissionRepository, id).orElseThrow();
        ensureNotSuccessfullyFinalized(submission);
        SubmissionStatus currentStatus = submission.getInternalStatus();

        if (isFinalStatus(currentStatus)) {
            logEvent(submission, "M2M_MARK_FOR_SUBMISSION_BLOCKED", "LOCAL_STATE", "TERMINAL_STATUS",
                    "Végállapotban lévő csomag nem jelölhető újra beküldésre. internalStatus=" + currentStatus
                            + ", m2mTerminal=" + submission.getM2mTerminal());
            throw new IllegalStateException("Végállapotban lévő csomag nem jelölhető újra beküldésre. Jelenlegi státusz: " + currentStatus);
        }

        if (Boolean.TRUE.equals(submission.getM2mTerminal())) {
            logEvent(submission, "M2M_TERMINAL_FLAG_RESET", "LOCAL_STATE", "OK",
                    "Nem végállapotú csomagon beragadt m2m_terminal jelző törölve megjelölés előtt. internalStatus="
                            + currentStatus + ", m2mTerminal=true");
            submission.setM2mTerminal(false);
            submission.setM2mFinalizedAt(null);
        }

        if (isInProgressStatus(currentStatus)) {
            throw new IllegalStateException("Folyamatban lévő csomag nem jelölhető újra beküldésre. Jelenlegi státusz: " + currentStatus);
        }
        Instant now = Instant.now();
        submission.setInternalStatus(SubmissionStatus.MARKED_FOR_SUBMISSION);
        submission.setM2mSubmitMarkedAt(now);
        submission.setM2mSubmittedAt(null);
        submission.setM2mFinalizedAt(null);
        submission.setM2mNextPollAt(null);
        submission.setM2mLastPollAt(null);
        submission.setM2mPollAttempts(0);
        submission.setM2mTerminal(false);
        submission.setM2mResubmittable(true);
        submission.setResultCode("MARKED_FOR_SUBMISSION");
        submission.setResultMessage("XML megjelölve M2M beküldésre.");
        submissionRepository.save(submission);
        logEvent(submission, "M2M_MARKED_FOR_SUBMISSION", "LOCAL_STATE", "OK", "XML megjelölve beküldésre.");
        return mapper.toResponse(submission);
    }

    /**
     * Visszavonja a beküldésre jelölést, amennyiben a beküldés még nem került végleges vagy aktív NAV állapotba.
     *
     * @param id a művelethez átadott {@code id} érték
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse withdrawSubmitMark(UUID id) {
        M2mSubmission submission = RepositoryAccess.findById(submissionRepository, id).orElseThrow();
        ensureNotSuccessfullyFinalized(submission);
        if (submission.getInternalStatus() != SubmissionStatus.MARKED_FOR_SUBMISSION) {
            throw new IllegalStateException("Csak beküldésre megjelölt csomag jelölése vonható vissza. Jelenlegi státusz: " + submission.getInternalStatus());
        }
        submission.setInternalStatus(SubmissionStatus.SUBMISSION_MARK_WITHDRAWN);
        submission.setM2mNextPollAt(null);
        submission.setM2mTerminal(false);
        submission.setM2mResubmittable(true);
        submission.setResultCode("SUBMISSION_MARK_WITHDRAWN");
        submission.setResultMessage("M2M beküldésre jelölés visszavonva.");
        submissionRepository.save(submission);
        logEvent(submission, "M2M_SUBMISSION_MARK_WITHDRAWN", "LOCAL_STATE", "OK", "Beküldésre jelölés visszavonva.");
        return mapper.toResponse(submission);
    }

    /**
     * A(z) {@code replaceXmlContent} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param id a művelethez átadott {@code id} érték
     * @param xml a művelethez átadott {@code xml} érték
     * @param xmlFileId az érintett XML-fájl adatbázis-azonosítója
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse replaceXmlContent(UUID id, MultipartFile xml, Long xmlFileId) {
        try {
            M2mSubmission submission = RepositoryAccess.findById(submissionRepository, id).orElseThrow();
            ensureNotSuccessfullyFinalized(submission);
            if (xmlFileId != null && submission.getXmlFileId() == null) {
                submission.setXmlFileId(xmlFileId);
                migrateAttachmentStorageToXmlFile(submission);
            }
            if (xml == null || xml.isEmpty()) {
                throw new IllegalArgumentException("A módosított XML tartalom hiányzik.");
            }
            FileStorageService.StoredFile storedXml = fileStorageService.store(submission.getId(), xml, "xml_updated");
            submission.setXmlFileName(storedXml.originalFileName());
            submission.setXmlStoragePath(storedXml.storagePath());
            submission.setXmlFileSize(storedXml.fileSize());
            submission.setXmlSha256Hex(storedXml.sha256Hex());
            submission.setNavFileId(null);
            SubmissionStatus previousStatus = submission.getInternalStatus();
            if (previousStatus == SubmissionStatus.MARKED_FOR_SUBMISSION) {
                submission.setInternalStatus(SubmissionStatus.MARKED_FOR_SUBMISSION);
            } else {
                submission.setInternalStatus(SubmissionStatus.CREATED);
            }
            submission.setResultCode("XML_REPLACED");
            submission.setResultMessage("XML tartalom frissítve a Schema Explorer űrlapnézetből.");
            referenceRepository.deleteBySubmissionId(submission.getId());
            saveXmlReferences(submission, Path.of(storedXml.storagePath()));
            detectAndApplyBizonylatMetadata(submission, Path.of(storedXml.storagePath()));
            submissionRepository.save(submission);
            logEvent(submission, "XML_REPLACED", "LOCAL", "OK", "XML tartalom frissítve: " + storedXml.originalFileName());
            return mapper.toResponse(submission);
        } catch (Exception e) {
            throw new IllegalStateException("XML tartalom frissítése sikertelen: " + e.getMessage(), e);
        }
    }

    /**
     * Frissíti a megadott beküldés NAV oldali állapotát és visszaadja az aktuális reprezentációt.
     *
     * @param id a művelethez átadott {@code id} érték
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse refresh(UUID id) {
        return mapper.toResponse(refreshStatusInternal(RepositoryAccess.findById(submissionRepository, id).orElseThrow(), "STATUS_REFRESHED"));
    }

    /**
     * A NAV oldali aktuális állapot lekérdezésével frissíti a helyi M2M életciklust és szükség szerint további pollingot ütemez.
     *
     * @param id a művelethez átadott {@code id} érték
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse pollStatus(UUID id) {
        M2mSubmission submission = RepositoryAccess.findById(submissionRepository, id).orElseThrow();
        if (!isPollableStatus(submission.getInternalStatus())) {
            return mapper.toResponse(submission);
        }
        return mapper.toResponse(refreshStatusInternal(submission, "M2M_STATUS_POLL_RESPONSE"));
    }

    /**
     * Megkeresi a pollingra esedékes, nem végállapotú beküldéseket, és egyenként megkísérli NAV státuszuk frissítését.
     */
    @Transactional
    public void pollDueSubmissions() {
        if (properties.getStatusPoll() == null || !properties.getStatusPoll().isEnabled()) {
            return;
        }
        java.time.Instant now = java.time.Instant.now();
        int batchSize = Math.max(1, properties.getStatusPoll().getBatchSize());
        java.util.List<M2mSubmission> due = submissionRepository.findByInternalStatusInAndM2mTerminalFalseAndM2mNextPollAtLessThanEqualOrderByM2mNextPollAtAsc(
                java.util.List.of(SubmissionStatus.SUBMITTING, SubmissionStatus.SUBMISSION_IN_PROGRESS, SubmissionStatus.SUBMIT_PENDING),
                now,
                org.springframework.data.domain.PageRequest.of(0, batchSize)
        );
        for (M2mSubmission submission : due) {
            try {
                java.time.Duration maxAge = properties.getStatusPoll().getMaxAge();
                if (maxAge != null && !maxAge.isZero() && !maxAge.isNegative()
                        && submission.getM2mSubmittedAt() != null
                        && submission.getM2mSubmittedAt().plus(maxAge).isBefore(now)) {
                    submission.setInternalStatus(SubmissionStatus.SUBMISSION_TECHNICAL_FAILED);
                    submission.setM2mNextPollAt(null);
                    submission.setM2mResubmittable(true);
                    submission.setResultCode("POLL_MAX_AGE_EXPIRED");
                    submission.setResultMessage("Az automata státuszlekérdezés elérte a konfigurált max-age időtartamot: " + maxAge);
                    submissionRepository.save(submission);
                    logEvent(submission, "M2M_STATUS_POLL_EXPIRED", "GET_STATUS", "MAX_AGE", "maxAge=" + maxAge);
                    continue;
                }
                refreshStatusInternal(submission, "M2M_STATUS_POLL_RESPONSE");
            } catch (Exception e) {
                submission = RepositoryAccess.findById(submissionRepository, submission.getId()).orElse(submission);
                int attempts = safeInt(submission.getM2mPollAttempts()) + 1;
                submission.setM2mPollAttempts(attempts);
                submission.setM2mLastPollAt(now);
                if (attempts >= properties.getStatusPoll().getMaxAttempts()) {
                    submission.setInternalStatus(SubmissionStatus.SUBMISSION_TECHNICAL_FAILED);
                    submission.setM2mNextPollAt(null);
                    submission.setM2mResubmittable(true);
                    submission.setResultCode("POLL_TECHNICAL_FAILED");
                    submission.setResultMessage(e.getMessage());
                    logEvent(submission, "M2M_STATUS_POLL_FAILED", "GET_STATUS", "MAX_ATTEMPTS", e.getMessage());
                } else {
                    scheduleNextPoll(submission);
                    logEvent(submission, "M2M_STATUS_POLL_FAILED", "GET_STATUS", "TECHNICAL_ERROR", e.getMessage());
                }
                submissionRepository.save(submission);
            }
        }
    }




    /**
     * Feltölti vagy feltöltésre előkészíti a megadott fájlt a NAV filestore irányába, és az eredményt a beküldési állapothoz kapcsolja.
     *
     * @param id a művelethez átadott {@code id} érték
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse uploadAttachmentsStep(UUID id) {
        M2mSubmission submission = RepositoryAccess.findById(submissionRepository, id).orElseThrow();
        ensureNotSuccessfullyFinalized(submission);
        try {
            List<M2mAttachment> attachments = attachmentRepository.findBySubmissionIdOrderByCreatedAtAsc(submission.getId());
            if (attachments.isEmpty()) {
                logEvent(submission, "ATTACHMENT_UPLOAD_SKIPPED", "COMMON_FILE_UPLOAD", "SKIPPED", "Nincs csatolmány feltöltve a csomaghoz");
            }
            ensureNavRuntimeReadyForSignedOperation(submission, "ATTACHMENT_UPLOAD");
            for (M2mAttachment attachment : attachments) {
                if (attachment.getNavFileId() != null && !attachment.getNavFileId().isBlank()) {
                    logEvent(submission, "ATTACHMENT_UPLOAD_SKIPPED", "COMMON_FILE_UPLOAD", "SKIPPED", attachment.getOriginalFileName() + " már fel van töltve: " + attachment.getNavFileId());
                    continue;
                }
                Path attachmentPath = Path.of(attachment.getStoragePath());
                String actualAttachmentHash = recomputeStoredFileHash(submission, "ATTACHMENT_UPLOAD", attachmentPath, attachment.getSha256Hex());
                if (!actualAttachmentHash.equalsIgnoreCase(attachment.getSha256Hex())) {
                    attachment.setSha256Hex(actualAttachmentHash);
                    attachment.setFileSize(Files.size(attachmentPath));
                    attachmentRepository.save(attachment);
                }
                String messageId = nextMessageId(submission);
                NavGateway.UploadedFile uploaded = gateway(submission).uploadFile(attachmentPath, attachment.getOriginalFileName(), actualAttachmentHash, Files.size(attachmentPath), messageId, submission.getCorrelationId());
                logHttpTraceEvents(submission);
                requireUploadedFileId(uploaded, "ATTACHMENT_UPLOAD", attachment.getOriginalFileName());
                attachment.setNavFileId(uploaded.fileId());
                Instant uploadedAt = Instant.now();
                attachment.setNavUploadedAt(uploadedAt);
                attachment.setNavLastRefreshedAt(uploadedAt);
                attachment.setNavExpiresAt(uploadedAt.plus(properties.getAttachment().getValidityDuration()));
                attachment.setNavUploadResultCode(uploaded.resultCode());
                attachment.setNavUploadResultMessage("virusScan=" + uploaded.virusScanResultCode());
                attachmentRepository.save(attachment);
                logEvent(submission, "ATTACHMENT_UPLOADED", "COMMON_FILE_UPLOAD", uploaded.resultCode(), attachment.getOriginalFileName() + " -> " + uploaded.fileId() + ", virusScan=" + uploaded.virusScanResultCode());
            }
            markUploadSuccess(submission, "ATTACHMENT_UPLOAD", "UPLOAD_SUCCESS", "Fájlfeltöltési lépés lefutott");
            return mapper.toResponse(submissionRepository.save(submission));
        } catch (Exception e) {
            logHttpTraceEvents(submission);
            return mapper.toResponse(markTechnicalFailed(submission, "ATTACHMENT_UPLOAD", e));
        }
    }



    /**
     * Előkészíti vagy létrehozza az adott NAV M2M művelethez szükséges adatot, majd a következő feldolgozási lépésnek adja tovább.
     *
     * @param id a művelethez átadott {@code id} érték
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse createBizonylatStep(UUID id) {
        M2mSubmission submission = RepositoryAccess.findById(submissionRepository, id).orElseThrow();
        ensureNotSuccessfullyFinalized(submission);
        try {
            ensureResubmissionAllowed(submission);
            ensureAttachmentsValidForSubmission(submission);
            ensureBizonylatRouteFromXmlMetadata(submission, "BIZONYLAT_STEP_ROUTE_DETECTION");
            requireBizonylatApiSubmission(submission);
            requireMarkedForSubmit(submission);
            if (submission.getSubmissionStartedAt() == null) submission.setSubmissionStartedAt(Instant.now());
            submission.setInternalStatus(SubmissionStatus.SUBMIT_PENDING);
            submissionRepository.save(submission);
            List<M2mAttachment> attachments = attachmentRepository.findBySubmissionIdOrderByCreatedAtAsc(submission.getId());
            List<NavGateway.UploadedFile> uploadedAttachments = uploadedAttachmentsFromDb(submission);
            List<XmlAttachmentReferenceInjector.UploadedAttachmentForXml> attachmentsForXml = new ArrayList<>();
            for (M2mAttachment attachment : attachments) {
                NavGateway.UploadedFile matching = uploadedAttachments.stream().filter(u -> u.fileId().equals(attachment.getNavFileId())).findFirst().orElse(null);
                if (matching != null) attachmentsForXml.add(new XmlAttachmentReferenceInjector.UploadedAttachmentForXml(attachment.getOriginalFileName(), attachment.getFileSize(), matching));
            }
            Path xmlForSubmit = Path.of(submission.getXmlStoragePath());
            if (!attachmentsForXml.isEmpty()) {
                xmlForSubmit = referenceInjector.injectAttachmentReferences(xmlForSubmit, attachmentsForXml);
                logEvent(submission, "XML_ATTACHMENT_REFERENCES_INJECTED", "LOCAL_XML_TRANSFORM", "OK", "attachmentCount=" + attachmentsForXml.size() + ", xmlWithAttachments=" + xmlForSubmit);
            }
            ensureNavRuntimeReadyForSignedOperation(submission, "BIZONYLAT_CREATE");
            BizonylatPayload payload = prepareBizonylatPayload(submission, xmlForSubmit);
            String messageId = nextMessageId(submission);
            M2mSignatureService.SignatureDebug signatureDebug = signatureService.createSignatureDebug(messageId, payload.sha256Hex());
            String signature = signatureDebug.signatureBase64Upper();
            String validationCertificate = eligibleValidationCertificate(submission, payload.sha256Hex());
            submission.setFastTrackSubmissionUsed(validationCertificate != null);
            logBizonylatRequestData(submission, payload, uploadedAttachments.size(), signature, validationCertificate, messageId);
            NavGateway.BizonylatCreateResult result = gateway(submission).createBizonylat(
                    submission.getBizonylatTipus(), submission.getBizonylatVerzio(), payload.path(), payload.compression(), uploadedAttachments, signature, signatureDebug, payload.sha256Hex(), validationCertificate, messageId, submission.getCorrelationId());
            logHttpTraceEvents(submission);
            submission.setNavUgyAzonosito(result.ugyAzonosito());
            submission.setNavStatus(result.navStatus());
            submission.setNavErkeztetesiSzam(result.erkeztetesiSzam());
            submission.setResultCode(result.resultCode());
            submission.setResultMessage(result.message());
            submission.setNavResponseBody(result.responseBody());
            submission.setNavMegjegyzes(result.megjegyzes());
            submission.setNavValidaciosHibak(result.validaciosHibak());
            if (result.befogadasIdopontja() != null && !result.befogadasIdopontja().isBlank()) {
                try { submission.setNavBefogadasIdopontja(Instant.parse(result.befogadasIdopontja())); }
                catch (Exception ignored) { logEvent(submission, "M2M_RESPONSE_TIME_PARSE_WARNING", "BIZONYLAT_CREATE", "WARNING", result.befogadasIdopontja()); }
            }
            submission.setSubmissionFinishedAt(Instant.now());
            if (submission.getSubmissionStartedAt() != null) {
                submission.setSubmissionDurationMs(java.time.Duration.between(submission.getSubmissionStartedAt(), submission.getSubmissionFinishedAt()).toMillis());
            }
            submission.setInternalStatus(mapStatus(result.navStatus(), result.resultCode()));
            applyStatusLifecycle(submission);
            logEvent(submission, "BIZONYLAT_SUBMITTED", "BIZONYLAT_CREATE", result.resultCode(), result.message());
            return mapper.toResponse(submissionRepository.save(submission));
        } catch (Exception e) {
            logHttpTraceEvents(submission);
            return mapper.toResponse(markTechnicalFailed(submission, "BIZONYLAT_CREATE", e));
        }
    }


    /**
     * A(z) {@code onlineValidacio} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param id a művelethez átadott {@code id} érték
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse onlineValidacio(UUID id) {
        M2mSubmission submission = RepositoryAccess.findById(submissionRepository, id).orElseThrow();
        ensureNotSuccessfullyFinalized(submission);
        submission.setNavValidacioUgyAzonosito(null);
        submission.setNavValidacioStartedAt(Instant.now());
        submission.setNavValidacioFinishedAt(null);
        submission.setNavValidacioLastCheckedAt(null);
        submission.setNavValidacioMessageId(null);
        submission.setNavValidacioCorrelationId(null);
        submission.setNavValidacioStatusz("INDITAS");
        submission.setNavValidacioResultCode(null);
        submission.setNavValidacioResultMessage(null);
        submission.setNavValidacioHibak(null);
        submission.setNavValidaciosTanusitvany(null);
        submission.setNavValidacioResponseBody(null);
        submission.setNavValidacioPayloadSha256(null);
        submissionRepository.save(submission);
        try {
            prepareBizonylatOperation(submission, "VALIDACIO_CREATE");
            BizonylatPayload payload = prepareBizonylatPayload(submission, Path.of(submission.getXmlStoragePath()));
            submission.setNavValidacioPayloadSha256(payload.sha256Hex());
            String messageId = nextMessageId(submission);
            submission.setNavValidacioMessageId(messageId);
            submission.setNavValidacioCorrelationId(submission.getCorrelationId());
            M2mSignatureService.SignatureDebug signatureDebug = signatureService.createSignatureDebug(messageId, payload.sha256Hex());
            NavGateway.ValidacioResult result = gateway(submission).createValidacio(
                    submission.getBizonylatTipus(), submission.getBizonylatVerzio(), payload.path(), payload.compression(),
                    signatureDebug.signatureBase64Upper(), signatureDebug, payload.sha256Hex(), messageId, submission.getCorrelationId());
            logHttpTraceEvents(submission);
            applyValidacioResult(submission, result, false);
            logEvent(submission, "M2M_VALIDACIO_CREATED", "VALIDACIO_CREATE", result.resultCode(),
                    "status=" + result.status() + ", ugyAzonosito=" + result.ugyAzonosito());
            return mapper.toResponse(submissionRepository.save(submission));
        } catch (Exception e) {
            logHttpTraceEvents(submission);
            return mapper.toResponse(markValidacioFailed(submission, "VALIDACIO_CREATE", e));
        }
    }

    /**
     * A(z) {@code validacioStatus} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param id a művelethez átadott {@code id} érték
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse validacioStatus(UUID id) {
        M2mSubmission submission = RepositoryAccess.findById(submissionRepository, id).orElseThrow();
        String ugyAzonosito = requireOperationIdentifier(submission.getNavValidacioUgyAzonosito(), "Nincs lekérdezhető online validációazonosító.");
        try {
            String messageId = nextMessageId(submission);
            submission.setNavValidacioMessageId(messageId);
            submission.setNavValidacioCorrelationId(submission.getCorrelationId());
            NavGateway.ValidacioResult result = gateway(submission).getValidacio(ugyAzonosito, messageId, submission.getCorrelationId());
            logHttpTraceEvents(submission);
            applyValidacioResult(submission, result, true);
            logEvent(submission, "M2M_VALIDACIO_STATUS_LOADED", "VALIDACIO_GET", result.resultCode(),
                    "status=" + result.status() + ", ugyAzonosito=" + ugyAzonosito);
            return mapper.toResponse(submissionRepository.save(submission));
        } catch (Exception e) {
            logHttpTraceEvents(submission);
            return mapper.toResponse(markValidacioFailed(submission, "VALIDACIO_GET", e));
        }
    }

    /**
     * A(z) {@code onlineKalkulacio} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param id a művelethez átadott {@code id} érték
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse onlineKalkulacio(UUID id) {
        M2mSubmission submission = RepositoryAccess.findById(submissionRepository, id).orElseThrow();
        ensureNotSuccessfullyFinalized(submission);
        submission.setNavKalkulacioUgyAzonosito(null);
        submission.setNavKalkulacioStartedAt(Instant.now());
        submission.setNavKalkulacioFinishedAt(null);
        submission.setNavKalkulacioLastCheckedAt(null);
        submission.setNavKalkulacioMessageId(null);
        submission.setNavKalkulacioCorrelationId(null);
        submission.setNavKalkulacioStatusz("INDITAS");
        clearKalkulacioResult(submission);
        submissionRepository.save(submission);
        try {
            prepareBizonylatOperation(submission, "KALKULACIO_CREATE");
            BizonylatPayload payload = prepareBizonylatPayload(submission, Path.of(submission.getXmlStoragePath()));
            String messageId = nextMessageId(submission);
            submission.setNavKalkulacioMessageId(messageId);
            submission.setNavKalkulacioCorrelationId(submission.getCorrelationId());
            M2mSignatureService.SignatureDebug signatureDebug = signatureService.createSignatureDebug(messageId, payload.sha256Hex());
            NavGateway.KalkulacioResult result = gateway(submission).createKalkulacio(
                    submission.getBizonylatTipus(), submission.getBizonylatVerzio(), payload.path(), payload.compression(),
                    signatureDebug.signatureBase64Upper(), signatureDebug, payload.sha256Hex(), messageId, submission.getCorrelationId());
            logHttpTraceEvents(submission);
            applyKalkulacioResult(submission, result, false);
            logEvent(submission, "M2M_KALKULACIO_CREATED", "KALKULACIO_CREATE", result.resultCode(),
                    "status=" + result.status() + ", ugyAzonosito=" + result.ugyAzonosito()
                            + ", calculatedXmlPresent=" + (submission.getNavKalkulaltXml() != null));
            return mapper.toResponse(submissionRepository.save(submission));
        } catch (Exception e) {
            logHttpTraceEvents(submission);
            return mapper.toResponse(markKalkulacioFailed(submission, "KALKULACIO_CREATE", e));
        }
    }

    /**
     * A(z) {@code kalkulacioEredmeny} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param id a művelethez átadott {@code id} érték
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse kalkulacioEredmeny(UUID id) {
        M2mSubmission submission = RepositoryAccess.findById(submissionRepository, id).orElseThrow();
        String ugyAzonosito = requireOperationIdentifier(submission.getNavKalkulacioUgyAzonosito(), "Nincs lekérdezhető online kalkulációazonosító.");
        try {
            String messageId = nextMessageId(submission);
            submission.setNavKalkulacioMessageId(messageId);
            submission.setNavKalkulacioCorrelationId(submission.getCorrelationId());
            NavGateway.KalkulacioResult result = gateway(submission).getKalkulacio(ugyAzonosito, messageId, submission.getCorrelationId());
            logHttpTraceEvents(submission);
            applyKalkulacioResult(submission, result, true);
            logEvent(submission, "M2M_KALKULACIO_RESULT_LOADED", "KALKULACIO_GET", result.resultCode(),
                    "status=" + result.status() + ", ugyAzonosito=" + ugyAzonosito
                            + ", calculatedXmlPresent=" + (submission.getNavKalkulaltXml() != null));
            return mapper.toResponse(submissionRepository.save(submission));
        } catch (Exception e) {
            logHttpTraceEvents(submission);
            return mapper.toResponse(markKalkulacioFailed(submission, "KALKULACIO_GET", e));
        }
    }

    /**
     * Közös előkészítést végez a Bizonylat API online műveleteihez: ellenőrzi a beküldés állapotát, route/metaadatot és szükséges NAV runtime feltételeket.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param operation a NAV vagy életciklus művelet neve
     */
    private void prepareBizonylatOperation(M2mSubmission submission, String operation) {
        ensureBizonylatRouteFromXmlMetadata(submission, operation + "_ROUTE_DETECTION");
        if (submission.getInterfaceType() != InterfaceType.BIZONYLAT_API) {
            throw new IllegalStateException("Az online validáció és kalkuláció csak Bizonylat API típusú XML-nél érhető el.");
        }
        if (submission.getBizonylatTipus() == null || submission.getBizonylatTipus().isBlank()
                || submission.getBizonylatVerzio() == null || submission.getBizonylatVerzio().isBlank()) {
            throw new IllegalStateException("A bizonylat típusa vagy verziója nem állapítható meg az XML és a sémaregiszter alapján.");
        }
        if (submission.getXmlStoragePath() == null || !ExceptionSafeOperations.isRegularFile(Path.of(submission.getXmlStoragePath()))) {
            throw new IllegalStateException("A validálandó vagy kalkulálandó XML helyi állománya nem található.");
        }
        ensureNavRuntimeReadyForSignedOperation(submission, operation);
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param value a feldolgozandó érték
     * @param message a művelethez átadott {@code message} érték
     * @return a művelet eredménye
     */
    private String requireOperationIdentifier(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalStateException(message);
        return value;
    }

    /**
     * A NAV validációs válaszából frissíti a beküldés validációs állapotát, eredménykódját, technikai adatát és a felhasználónak megjeleníthető eredménymezőket.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param result az épülő eredménykollekció
     * @param statusQuery a művelethez átadott {@code statusQuery} érték
     */
    private void applyValidacioResult(M2mSubmission submission, NavGateway.ValidacioResult result, boolean statusQuery) {
        if (result.ugyAzonosito() != null && !result.ugyAzonosito().isBlank()) {
            submission.setNavValidacioUgyAzonosito(result.ugyAzonosito());
        }
        submission.setNavValidacioStatusz(result.status());
        submission.setNavValidacioResultCode(result.resultCode());
        submission.setNavValidacioResultMessage(result.message());
        submission.setNavValidacioHibak(result.validaciosHibak());
        submission.setNavValidaciosTanusitvany(result.validaciosTanusitvany());
        submission.setNavValidacioResponseBody(result.responseBody());
        if (statusQuery) submission.setNavValidacioLastCheckedAt(Instant.now());
        submission.setNavValidacioFinishedAt(isOperationTerminal(result.status()) ? Instant.now() : null);
    }

    /**
     * Az M2M életciklus vagy feldolgozási eredmény alapján frissíti a kezelt domain/runtime állapotot; a változás a hívó tranzakciójának része lehet.
     *
     * @param submission az aktuális M2M beküldési entitás
     */
    private void clearKalkulacioResult(M2mSubmission submission) {
        submission.setNavKalkulacioResultCode(null);
        submission.setNavKalkulacioResultMessage(null);
        submission.setNavKalkulacioHibaKod(null);
        submission.setNavKalkulacioHibaUzenet(null);
        submission.setNavKalkulacioMezoAzonosito(null);
        submission.setNavKalkulacioSzabalyAzonosito(null);
        submission.setNavKalkulacioTomorites(null);
        submission.setNavKalkulaltXml(null);
        submission.setNavKalkulacioResponseBody(null);
    }

    /**
     * A NAV kalkulációs válaszából frissíti a művelet állapotát és – sikeres eredmény esetén – dekódolja/ellenőrzi a visszakapott számított XML-t.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param result az épülő eredménykollekció
     * @param statusQuery a művelethez átadott {@code statusQuery} érték
     * @throws Exception ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void applyKalkulacioResult(M2mSubmission submission, NavGateway.KalkulacioResult result, boolean statusQuery) throws Exception {
        if (result.ugyAzonosito() != null && !result.ugyAzonosito().isBlank()) {
            submission.setNavKalkulacioUgyAzonosito(result.ugyAzonosito());
        }
        submission.setNavKalkulacioStatusz(result.status());
        submission.setNavKalkulacioResultCode(result.resultCode());
        submission.setNavKalkulacioResultMessage(result.message());
        submission.setNavKalkulacioHibaKod(result.hibaKod());
        submission.setNavKalkulacioHibaUzenet(result.hibaUzenet());
        submission.setNavKalkulacioMezoAzonosito(result.mezoAzonosito());
        submission.setNavKalkulacioSzabalyAzonosito(result.szabalyAzonosito());
        submission.setNavKalkulacioTomorites(result.tomorites());
        submission.setNavKalkulacioResponseBody(result.responseBody());
        if (result.bizonylatXmlBase64() != null && !result.bizonylatXmlBase64().isBlank()) {
            DecodedCalculatedXml decoded = decodeCalculatedXml(result.bizonylatXmlBase64(), result.tomorites());
            submission.setNavKalkulaltXml(decoded.xml());
            submission.setNavKalkulacioTomorites(decoded.compression());
        }
        if (statusQuery) submission.setNavKalkulacioLastCheckedAt(Instant.now());
        submission.setNavKalkulacioFinishedAt(isOperationTerminal(result.status()) ? Instant.now() : null);
    }

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param status a vizsgált beküldési státusz
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private boolean isOperationTerminal(String status) {
        return status != null && !status.isBlank() && !"FOLYAMATBAN".equalsIgnoreCase(status.trim());
    }

    /**
     * A NAV által visszaadott Base64 payloadot a jelzett tömörítési mód szerint kitömöríti, majd well-formed XML-ként ellenőrzi, mielőtt az alkalmazás tovább használná.
     *
     * @param encoded a művelethez átadott {@code encoded} érték
     * @param compression a művelethez átadott {@code compression} érték
     * @return a művelet eredménye
     * @throws Exception ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private DecodedCalculatedXml decodeCalculatedXml(String encoded, String compression) throws Exception {
        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("A kalkulációs válasz bizonylatXml mezője nem érvényes Base64 tartalom.", e);
        }

        if ("GZIP".equalsIgnoreCase(compression)) {
            byte[] xmlBytes = decompressGzip(payload);
            String xml = new String(xmlBytes, StandardCharsets.UTF_8);
            assertWellFormedCalculatedXml(xml);
            return new DecodedCalculatedXml(xml, "GZIP");
        }
        if ("BZIP2".equalsIgnoreCase(compression) || "BZIP".equalsIgnoreCase(compression)) {
            byte[] xmlBytes = decompressBzip2(payload);
            String xml = new String(xmlBytes, StandardCharsets.UTF_8);
            assertWellFormedCalculatedXml(xml);
            return new DecodedCalculatedXml(xml, "BZIP2");
        }

        String xml = new String(payload, StandardCharsets.UTF_8);
        try {
            assertWellFormedCalculatedXml(xml);
            return new DecodedCalculatedXml(xml, compression);
        } catch (Exception firstParseError) {
            if (!isPrologError(firstParseError)) throw firstParseError;

            log.warn("A NAV sikeres kalkulációs válaszának XML-feldolgozása prolog hibával leállt; BZip2 fallback indul.");
            try {
                byte[] xmlBytes = decompressBzip2(payload);
                String decompressedXml = new String(xmlBytes, StandardCharsets.UTF_8);
                assertWellFormedCalculatedXml(decompressedXml);
                log.info("A kalkulációs válasz BZip2 fallback kibontása sikeres. "
                        + "compressedBytes={}, decompressedBytes={}, xmlPrefixHex={}",
                        payload.length, xmlBytes.length, firstBytesHex(xmlBytes, 8));
                return new DecodedCalculatedXml(decompressedXml, "BZIP2");
            } catch (Exception bzipError) {
                firstParseError.addSuppressed(bzipError);
                throw new IllegalStateException(
                        "A kalkulációs válasz nem értelmezhető közvetlen XML-ként, és a BZip2 fallback kibontás után sem lett érvényes XML.",
                        firstParseError);
            }
        }
    }

    /**
     * A(z) {@code validationErrorDetails} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param id a művelethez átadott {@code id} érték
     * @return a művelet eredménye
     */
    @Transactional
    public ValidationErrorDetailsResponse validationErrorDetails(UUID id) {
        M2mSubmission submission = RepositoryAccess.findById(submissionRepository, id).orElseThrow();
        String encoded = submission.getNavValidacioHibak();
        if (encoded == null || encoded.isBlank()) {
            return validationErrorResponse(submission, List.of());
        }
        try {
            byte[] decoded = Base64.getMimeDecoder().decode(encoded.trim());
            byte[] xmlBytes = decompressBzip2(decoded);
            List<ValidationErrorItem> errors = parseValidationErrors(xmlBytes);
            return validationErrorResponse(submission, errors);
        } catch (Exception error) {
            throw new IllegalStateException("A validációs hibák BZip2/Base64 tartalma nem bontható ki vagy nem értelmezhető XML-ként.", error);
        }
    }

    /**
     * A(z) {@code validationErrorResponse} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param errors a művelethez átadott {@code errors} érték
     * @return a művelet eredménye
     */
    private ValidationErrorDetailsResponse validationErrorResponse(M2mSubmission submission, List<ValidationErrorItem> errors) {
        return new ValidationErrorDetailsResponse(
                submission.getNavValidacioUgyAzonosito(),
                submission.getNavValidacioStatusz(),
                submission.getNavValidacioResultCode(),
                submission.getNavValidacioResultMessage(),
                stringValue(submission.getNavValidacioStartedAt()),
                stringValue(submission.getNavValidacioFinishedAt()),
                stringValue(submission.getNavValidacioLastCheckedAt()),
                submission.getNavValidacioMessageId(),
                submission.getNavValidacioCorrelationId(),
                errors.size(),
                errors
        );
    }

    /**
     * A(z) {@code stringValue} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * A NAV technikai vagy domain válaszából strukturált validációs hibalistát állít elő, több lehetséges válaszformátumot és fallbacket kezelve.
     *
     * @param xmlBytes a művelethez átadott {@code xmlBytes} érték
     * @return a művelet eredménye
     * @throws Exception ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private List<ValidationErrorItem> parseValidationErrors(byte[] xmlBytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes));
        List<ValidationErrorItem> result = new ArrayList<>();
        collectValidationErrors(document.getDocumentElement(), result);
        if (result.isEmpty()) {
            String message = document.getDocumentElement() == null ? "" : document.getDocumentElement().getTextContent();
            if (message != null && !message.isBlank()) {
                result.add(new ValidationErrorItem("", message.trim(), "HIBA", "", "", "", ""));
            }
        }
        return result;
    }

    /**
     * A(z) {@code collectValidationErrors} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param element a művelethez átadott {@code element} érték
     * @param result az épülő eredménykollekció
     */
    private void collectValidationErrors(Element element, List<ValidationErrorItem> result) {
        String code = firstNonBlank(
                attributeText(element, "kod", "hibakod", "errorcode", "code"),
                childText(element, "hibakod", "errorcode", "code", "kod"));
        String message = firstNonBlank(
                attributeText(element, "hibaszoveg", "hibauzenet", "errormessage", "message", "uzenet", "leiras"),
                childText(element, "hibaszoveg", "hibauzenet", "errormessage", "message", "uzenet", "leiras"));
        String path = firstNonBlank(
                attributeText(element, "path", "xpath", "eleresiut", "xmlpath"),
                childText(element, "xpath", "path", "eleresiut", "xmlpath"));
        String ruleId = firstNonBlank(
                attributeText(element, "ruleid", "szabalyazonosito", "rule"),
                childText(element, "ruleid", "szabalyazonosito", "rule"));
        String severity = firstNonBlank(
                attributeText(element, "szint", "severity", "sulyossag", "level"),
                childText(element, "severity", "sulyossag", "szint", "level"));
        String field = firstNonBlank(
                attributeText(element, "elem", "element", "fieldid", "mezoazonosito", "mezo"),
                childText(element, "element", "elem", "fieldid", "mezoazonosito", "mezo"));
        String additional = buildValidationAdditionalInformation(element);
        if (!message.isBlank() && (!code.isBlank() || !path.isBlank() || !ruleId.isBlank() || isValidationErrorElement(element))) {
            result.add(new ValidationErrorItem(
                    code,
                    message,
                    normalizeValidationSeverity(severity),
                    field,
                    ruleId,
                    path,
                    additional));
            return;
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement) collectValidationErrors(childElement, result);
        }
    }

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param element a művelethez átadott {@code element} érték
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private boolean isValidationErrorElement(Element element) {
        String name = localName(element);
        return "hiba".equals(name) || "error".equals(name) || "validationresult".equals(name);
    }

    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param element a művelethez átadott {@code element} érték
     * @param acceptedNames a művelethez átadott {@code acceptedNames} érték
     * @return a művelet eredménye
     */
    private String attributeText(Element element, String... acceptedNames) {
        Set<String> names = Set.of(acceptedNames);
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            Node attribute = element.getAttributes().item(i);
            String name = attribute.getLocalName();
            if (name == null || name.isBlank()) name = attribute.getNodeName();
            int colon = name.indexOf(':');
            if (colon >= 0) name = name.substring(colon + 1);
            name = name.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
            if (names.contains(name)) {
                String value = attribute.getNodeValue();
                return value == null ? "" : value.trim();
            }
        }
        return "";
    }

    /**
     * A bemeneti domain/transport adatokból a következő feldolgozási réteg által igényelt reprezentációt állítja elő.
     *
     * @param element a művelethez átadott {@code element} érték
     * @return a művelet eredménye
     */
    private String buildValidationAdditionalInformation(Element element) {
        List<String> details = new ArrayList<>();
        appendValidationDetail(details, "Dinamikus lap index", attributeText(element, "dinamikuslapindex"));
        appendValidationDetail(details, "SDG aktivitás", attributeText(element, "activityinsdg"));
        return String.join("; ", details);
    }

    /**
     * A(z) {@code appendValidationDetail} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param details a művelethez átadott {@code details} érték
     * @param label a művelethez átadott {@code label} érték
     * @param value a feldolgozandó érték
     */
    private void appendValidationDetail(List<String> details, String label, String value) {
        if (value != null && !value.isBlank()) details.add(label + ": " + value);
    }

    /**
     * A NAV különböző súlyossági jelöléseit az alkalmazás egységes validációs severity értékeire normalizálja.
     *
     * @param severity a művelethez átadott {@code severity} érték
     * @return a művelet eredménye
     */
    private String normalizeValidationSeverity(String severity) {
        if (severity == null || severity.isBlank()) return "HIBA";
        return switch (severity.trim().toUpperCase(Locale.ROOT)) {
            case "1", "CRITICAL", "FATAL", "KRITIKUS" -> "CRITICAL";
            case "2", "WARNING", "WARN", "FIGYELMEZTETES", "FIGYELMEZTETÉS" -> "WARNING";
            case "3", "INFO", "INFORMATION", "INFORMACIO", "INFORMÁCIÓ" -> "INFO";
            case "ERROR", "HIBA" -> "ERROR";
            default -> severity.trim();
        };
    }

    /**
     * A megadott jelöltek közül az első nem üres értéket választja fallback sorrendben.
     *
     * @param values a művelethez átadott {@code values} érték
     * @return a művelet eredménye
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }


    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param element a művelethez átadott {@code element} érték
     * @param acceptedNames a művelethez átadott {@code acceptedNames} érték
     * @return a művelet eredménye
     */
    private String childText(Element element, String... acceptedNames) {
        Set<String> names = Set.of(acceptedNames);
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement && names.contains(localName(childElement))) {
                String value = childElement.getTextContent();
                return value == null ? "" : value.trim();
            }
        }
        return "";
    }

    /**
     * Namespace-től független helyi XML elemnevet ad vissza.
     *
     * @param element a művelethez átadott {@code element} érték
     * @return a művelet eredménye
     */
    private String localName(Element element) {
        String name = element.getLocalName();
        if (name == null || name.isBlank()) name = element.getNodeName();
        int colon = name.indexOf(':');
        if (colon >= 0) name = name.substring(colon + 1);
        return name.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
    }

    /**
     * A NAV válaszban kapott kódolt vagy tömörített tartalmat a várt formátum szerint visszaalakítja további XML-feldolgozáshoz.
     *
     * @param payload a naplózandó, szükség szerint maszkolt payload
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private byte[] decompressGzip(byte[] payload) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(payload));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            gzip.transferTo(out);
            return out.toByteArray();
        }
    }

    /**
     * A NAV válaszban kapott kódolt vagy tömörített tartalmat a várt formátum szerint visszaalakítja további XML-feldolgozáshoz.
     *
     * @param payload a naplózandó, szükség szerint maszkolt payload
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private byte[] decompressBzip2(byte[] payload) throws IOException {
        try (BZip2CompressorInputStream bzip2 = new BZip2CompressorInputStream(
                new ByteArrayInputStream(payload), true);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            bzip2.transferTo(out);
            return out.toByteArray();
        }
    }

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param error a művelethez átadott {@code error} érték
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private boolean isPrologError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("content is not allowed in prolog")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param bytes a feldolgozandó bájttömb
     * @param maxBytes a művelethez átadott {@code maxBytes} érték
     * @return a művelet eredménye
     */
    private String firstBytesHex(byte[] bytes, int maxBytes) {
        if (bytes == null || bytes.length == 0) return "";
        StringBuilder result = new StringBuilder();
        int length = Math.min(bytes.length, Math.max(0, maxBytes));
        for (int i = 0; i < length; i++) {
            if (i > 0) result.append(' ');
            result.append(String.format(Locale.ROOT, "%02X", bytes[i] & 0xFF));
        }
        return result.toString();
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param xml a művelethez átadott {@code xml} érték
     * @throws Exception ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void assertWellFormedCalculatedXml(String xml) throws Exception {
        if (xml == null || xml.isBlank()) throw new IllegalStateException("A kalkulációs válasz üres XML-t tartalmaz.");
        if (xml.regionMatches(true, 0, "<!DOCTYPE", 0, "<!DOCTYPE".length())
                || xml.toUpperCase(Locale.ROOT).contains("<!DOCTYPE")) {
            throw new IllegalStateException("DOCTYPE deklaráció nem engedélyezett a kalkulációs XML-ben.");
        }

        javax.xml.stream.XMLInputFactory factory = javax.xml.stream.XMLInputFactory.newFactory();
        factory.setProperty(javax.xml.stream.XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        factory.setProperty(javax.xml.stream.XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);

        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            javax.xml.stream.XMLStreamReader reader = factory.createXMLStreamReader(in);
            try {
                while (reader.hasNext()) {
                    reader.next();
                }
            } finally {
                reader.close();
            }
        }
    }

    /**
     * A NAV M2M submitter modul {@code DecodedCalculatedXml} típusának felelősségét megvalósító típus.
     */
    /**
     * Létrehozza a(z) {@code DecodedCalculatedXml} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param xml a művelethez átadott {@code xml} érték
     * @param compression a művelethez átadott {@code compression} érték
     */
    private record DecodedCalculatedXml(String xml, String compression) { }

    /**
     * A(z) {@code technicalResponseBody} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param e a feldolgozás közben kapott kivétel
     * @return a művelet eredménye
     */
    private String technicalResponseBody(Exception e) {
        if (e instanceof RestClientResponseException responseException) {
            String responseBody = responseException.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isBlank()) return responseBody;
        }
        return e == null ? null : e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    /**
     * A(z) {@code markValidacioFailed} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param operation a NAV vagy életciklus művelet neve
     * @param e a feldolgozás közben kapott kivétel
     * @return a művelet eredménye
     */
    private M2mSubmission markValidacioFailed(M2mSubmission submission, String operation, Exception e) {
        submission.setNavValidacioStatusz("TECHNIKAI_HIBA");
        submission.setNavValidacioResultCode("TECHNICAL_ERROR");
        submission.setNavValidacioResultMessage(e.getMessage());
        submission.setNavValidacioResponseBody(technicalResponseBody(e));
        submission.setNavValidacioFinishedAt(Instant.now());
        logEvent(submission, "M2M_VALIDACIO_FAILED", operation, "TECHNICAL_ERROR", e.getMessage());
        return submissionRepository.save(submission);
    }

    /**
     * A(z) {@code markKalkulacioFailed} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param operation a NAV vagy életciklus művelet neve
     * @param e a feldolgozás közben kapott kivétel
     * @return a művelet eredménye
     */
    private M2mSubmission markKalkulacioFailed(M2mSubmission submission, String operation, Exception e) {
        submission.setNavKalkulacioStatusz("TECHNIKAI_HIBA");
        submission.setNavKalkulacioResultCode("TECHNICAL_ERROR");
        submission.setNavKalkulacioResultMessage(e.getMessage());
        submission.setNavKalkulacioResponseBody(technicalResponseBody(e));
        submission.setNavKalkulacioFinishedAt(Instant.now());
        logEvent(submission, "M2M_KALKULACIO_FAILED", operation, "TECHNICAL_ERROR", e.getMessage());
        return submissionRepository.save(submission);
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param operation a NAV vagy életciklus művelet neve
     */
    private void ensureBizonylatRouteFromXmlMetadata(M2mSubmission submission, String operation) {
        if (submission == null || submission.getXmlStoragePath() == null || submission.getXmlStoragePath().isBlank()) {
            return;
        }
        InterfaceType previousInterface = submission.getInterfaceType();
        String previousType = submission.getBizonylatTipus();
        String previousVersion = submission.getBizonylatVerzio();
        submission.setInterfaceType(InterfaceType.BIZONYLAT_API);
        detectAndApplyBizonylatMetadata(submission, Path.of(submission.getXmlStoragePath()));
        submissionRepository.save(submission);
        if (previousInterface != InterfaceType.BIZONYLAT_API
                || !java.util.Objects.equals(previousType, submission.getBizonylatTipus())
                || !java.util.Objects.equals(previousVersion, submission.getBizonylatVerzio())) {
            logEvent(submission, "M2M_SUBMIT_ROUTE_FORCED_TO_BIZONYLAT", operation, "BIZONYLAT_API",
                    "previousInterface=" + previousInterface
                            + ", effectiveInterface=" + submission.getInterfaceType()
                            + ", previousBizonylat=" + previousType + "/" + previousVersion
                            + ", effectiveBizonylat=" + submission.getBizonylatTipus() + "/" + submission.getBizonylatVerzio()
                            + ", reason=a projekt minden XML-t Bizonylat API csomagként kezel");
        }
    }

    /**
     * A művelet diagnosztikai vagy audit információját rögzíti úgy, hogy érzékeny token vagy hitelesítési adat ne kerüljön a felhasználói naplóba.
     *
     * @param submission az aktuális M2M beküldési entitás
     */
    private void logSubmitRouteSelected(M2mSubmission submission) {
        String endpoint = baseUrl(properties.getEndpoints().getBizonylatBaseUrl())
                + properties.getEndpoints().getBizonylatPath();
        logEvent(submission, "M2M_SUBMIT_ROUTE_SELECTED", "BIZONYLAT_CREATE", "ROUTE",
                "interfaceType=" + submission.getInterfaceType()
                        + ", endpoint=" + endpoint
                        + ", bizonylatTipus=" + submission.getBizonylatTipus()
                        + ", bizonylatVerzio=" + submission.getBizonylatVerzio()
                        + ", xmlFile=" + submission.getXmlFileName());
    }

    /**
     * A(z) {@code baseUrl} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String baseUrl(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }


    /**
     * A(z) {@code detectAndApplyBizonylatMetadata} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param xmlPath a művelethez átadott {@code xmlPath} érték
     */
    private void detectAndApplyBizonylatMetadata(M2mSubmission submission, Path xmlPath) {
        InterfaceType previousInterface = submission.getInterfaceType();
        submission.setInterfaceType(InterfaceType.BIZONYLAT_API);
        var detectedMetadata = metadataExtractor.extract(xmlPath);
        if (detectedMetadata.isEmpty()) {
            return;
        }
        var meta = detectedMetadata.get();
        String previousType = submission.getBizonylatTipus();
        String previousVersion = submission.getBizonylatVerzio();
        submission.setBizonylatTipus(meta.bizonylatTipus());
        submission.setBizonylatVerzio(meta.bizonylatVerzio());
        logEvent(submission, "BIZONYLAT_METADATA_DETECTED", meta.source(), "OK",
                "detected.bizonylatTipus=" + meta.bizonylatTipus()
                        + ", detected.bizonylatVerzio=" + meta.bizonylatVerzio()
                        + ", detected.source=" + meta.source()
                        + ", detected.namespace=" + meta.namespace()
                        + ", detected.primaryXsd=" + meta.primaryXsd()
                        + ", detected.matchReason=" + meta.matchReason()
                        + ", previous=" + previousType + "/" + previousVersion
                        + ", previousInterface=" + previousInterface
                        + ", effectiveInterface=" + submission.getInterfaceType());
    }


    /**
     * Előkészíti vagy létrehozza az adott NAV M2M művelethez szükséges adatot, majd a következő feldolgozási lépésnek adja tovább.
     *
     * @param gatewayMode a művelethez átadott {@code gatewayMode} érték
     * @param files a feldolgozandó fájlok listája
     * @param uploadNow a művelethez átadott {@code uploadNow} érték
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse createStandaloneFilestorePackage(GatewayMode gatewayMode,
                                                              List<MultipartFile> files,
                                                              boolean uploadNow) {
        try {
            if (files == null || files.stream().noneMatch(f -> f != null && !f.isEmpty())) {
                throw new IllegalArgumentException("Legalább egy feltöltendő fájlt ki kell választani.");
            }
            M2mSubmission submission = new M2mSubmission();
            submission.setId(UUID.randomUUID());
            submission.setInterfaceType(InterfaceType.COMMON_FILESTORE);
            submission.setGatewayMode(gatewayMode == null ? GatewayMode.MOCK : gatewayMode);
            submission.setInternalStatus(SubmissionStatus.CREATED);
            resetM2mLifecycleForNewSubmission(submission);
            submission.setCompression(CompressionType.NONE);
            submission.setMessageId(UUID.randomUUID().toString());
            submission.setCorrelationId(UUID.randomUUID().toString());
            submission = submissionRepository.save(submission);

            saveStandaloneFiles(submission, files);
            logEvent(submission, "CREATED", "LOCAL_FILESTORE_PACKAGE", "OK", "Önálló Common Filestore fájlcsomag létrehozva. fileCount=" + attachmentRepository.findBySubmissionIdOrderByCreatedAtAsc(submission.getId()).size());

            SubmissionResponse response = mapper.toResponse(submission);
            if (uploadNow) {
                response = uploadAttachmentsStep(submission.getId());
            }
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Önálló fájlcsomag létrehozása sikertelen: " + e.getMessage(), e);
        }
    }

    /**
     * Lekéri a kért M2M erőforrást vagy aktuális konfigurációt.
     *
     * @param id a művelethez átadott {@code id} érték
     * @return a művelet eredménye
     */
    public SubmissionResponse get(UUID id) {
        return mapper.toResponse(RepositoryAccess.findById(submissionRepository, id).orElseThrow());
    }

    /**
     * Lekéri a hívó számára látható M2M erőforrások listáját.
     *
     * @return a művelet eredménye
     */
    public List<SubmissionResponse> list() {
        return submissionRepository.findAllByOrderByCreatedAtDesc().stream().map(mapper::toResponse).toList();
    }

    /**
     * A(z) {@code submitInternal} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @return a művelet eredménye
     */
    private M2mSubmission submitInternal(M2mSubmission submission) {
        try {
            ensureResubmissionAllowed(submission);
            ensureAttachmentsValidForSubmission(submission);
            if (submission.getSubmissionStartedAt() == null) submission.setSubmissionStartedAt(Instant.now());
            submission.setInternalStatus(SubmissionStatus.SUBMIT_PENDING);
            submissionRepository.save(submission);
            logEvent(submission, "M2M_SUBMIT_STARTED", "SUBMIT", "STARTED", "Megjelölt XML/csomag beküldése elindult.");
            ensureBizonylatRouteFromXmlMetadata(submission, "SUBMIT_ROUTE_DETECTION");
            requireBizonylatApiSubmission(submission);
            logSubmitRouteSelected(submission);
            ensureNavRuntimeReadyForSignedOperation(submission, "SUBMIT");
            List<M2mAttachment> attachments = attachmentRepository.findBySubmissionIdOrderByCreatedAtAsc(submission.getId());
            List<NavGateway.UploadedFile> uploadedAttachments = new ArrayList<>();
            List<XmlAttachmentReferenceInjector.UploadedAttachmentForXml> attachmentsForXml = new ArrayList<>();
            for (M2mAttachment attachment : attachments) {
                Path attachmentPath = Path.of(attachment.getStoragePath());
                String actualAttachmentHash = recomputeStoredFileHash(submission, "ATTACHMENT_UPLOAD", attachmentPath, attachment.getSha256Hex());
                if (!actualAttachmentHash.equalsIgnoreCase(attachment.getSha256Hex())) {
                    attachment.setSha256Hex(actualAttachmentHash);
                    attachment.setFileSize(Files.size(attachmentPath));
                    attachmentRepository.save(attachment);
                }
                String uploadMessageId = nextMessageId(submission);
                NavGateway.UploadedFile uploaded = gateway(submission).uploadFile(attachmentPath, attachment.getOriginalFileName(), actualAttachmentHash, Files.size(attachmentPath), uploadMessageId, submission.getCorrelationId());
                logHttpTraceEvents(submission);
                requireUploadedFileId(uploaded, "ATTACHMENT_UPLOAD", attachment.getOriginalFileName());
                attachment.setNavFileId(uploaded.fileId());
                Instant uploadedAt = Instant.now();
                attachment.setNavUploadedAt(uploadedAt);
                attachment.setNavLastRefreshedAt(uploadedAt);
                attachment.setNavExpiresAt(uploadedAt.plus(properties.getAttachment().getValidityDuration()));
                attachment.setNavUploadResultCode(uploaded.resultCode());
                attachment.setNavUploadResultMessage("virusScan=" + uploaded.virusScanResultCode());
                attachmentRepository.save(attachment);
                uploadedAttachments.add(uploaded);
                attachmentsForXml.add(new XmlAttachmentReferenceInjector.UploadedAttachmentForXml(attachment.getOriginalFileName(), attachment.getFileSize(), uploaded));
                logEvent(submission, "ATTACHMENT_UPLOADED", "COMMON_FILE_UPLOAD", uploaded.resultCode(), attachment.getOriginalFileName() + " -> " + uploaded.fileId());
            }

            Path xmlForSubmit = Path.of(submission.getXmlStoragePath());
            if (!attachmentsForXml.isEmpty()) {
                xmlForSubmit = referenceInjector.injectAttachmentReferences(xmlForSubmit, attachmentsForXml);
                logEvent(submission, "XML_ATTACHMENT_REFERENCES_INJECTED", "LOCAL_XML_TRANSFORM", "OK", "attachmentCount=" + attachmentsForXml.size() + ", xmlWithAttachments=" + xmlForSubmit);
            }

            BizonylatPayload payload = prepareBizonylatPayload(submission, xmlForSubmit);
            String bizonylatMessageId = nextMessageId(submission);
            M2mSignatureService.SignatureDebug bizonylatSignatureDebug = signatureService.createSignatureDebug(bizonylatMessageId, payload.sha256Hex());
            String bizonylatSignature = bizonylatSignatureDebug.signatureBase64Upper();
            String validationCertificate = eligibleValidationCertificate(submission, payload.sha256Hex());
            submission.setFastTrackSubmissionUsed(validationCertificate != null);
            logBizonylatRequestData(submission, payload, uploadedAttachments.size(), bizonylatSignature, validationCertificate, bizonylatMessageId);
            NavGateway.BizonylatCreateResult result = gateway(submission).createBizonylat(
                    submission.getBizonylatTipus(), submission.getBizonylatVerzio(), payload.path(), payload.compression(), uploadedAttachments, bizonylatSignature, bizonylatSignatureDebug, payload.sha256Hex(), validationCertificate, bizonylatMessageId, submission.getCorrelationId());
            logHttpTraceEvents(submission);
            submission.setNavUgyAzonosito(result.ugyAzonosito());
            submission.setNavStatus(result.navStatus());
            submission.setNavErkeztetesiSzam(result.erkeztetesiSzam());
            submission.setResultCode(result.resultCode());
            submission.setResultMessage(result.message());
            submission.setNavResponseBody(result.responseBody());
            submission.setNavMegjegyzes(result.megjegyzes());
            submission.setNavValidaciosHibak(result.validaciosHibak());
            if (result.befogadasIdopontja() != null && !result.befogadasIdopontja().isBlank()) {
                try { submission.setNavBefogadasIdopontja(Instant.parse(result.befogadasIdopontja())); } catch (Exception ignored) { }
            }
            submission.setSubmissionFinishedAt(Instant.now());
            if (submission.getSubmissionStartedAt() != null) {
                submission.setSubmissionDurationMs(java.time.Duration.between(submission.getSubmissionStartedAt(), submission.getSubmissionFinishedAt()).toMillis());
            }
            submission.setInternalStatus(mapStatus(result.navStatus(), result.resultCode()));
            applyStatusLifecycle(submission);
            logEvent(submission, "BIZONYLAT_SUBMITTED", "BIZONYLAT_CREATE", result.resultCode(), result.message());
            return submissionRepository.save(submission);
        } catch (Exception e) {
            logHttpTraceEvents(submission);
            submission.setInternalStatus(SubmissionStatus.TECHNICAL_FAILED);
            submission.setResultCode("TECHNICAL_ERROR");
            submission.setResultMessage(e.getMessage());
            logEvent(submission, "TECHNICAL_FAILED", "SUBMIT", "TECHNICAL_ERROR", e.getMessage());
            return submissionRepository.save(submission);
        }
    }

    /**
     * A(z) {@code markUploadSuccess} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param operation a NAV vagy életciklus művelet neve
     * @param resultCode a NAV eredménykód
     * @param message a művelethez átadott {@code message} érték
     */
    private void markUploadSuccess(M2mSubmission submission, String operation, String resultCode, String message) {
        SubmissionStatus previousStatus = submission.getInternalStatus();
        submission.setInternalStatus(SubmissionStatus.UPLOAD_SUCCESS);
        submission.setNavStatus("UPLOAD_SUCCESS");
        submission.setResultCode(resultCode == null || resultCode.isBlank() ? "UPLOAD_SUCCESS" : resultCode);
        submission.setResultMessage(message);
        logEvent(submission, "M2M_STATUS_TRANSITION", operation, "UPLOAD_SUCCESS",
                "oldStatus=" + previousStatus
                        + ", newStatus=" + submission.getInternalStatus()
                        + ", navFileId=" + submission.getNavFileId()
                        + ", resultCode=" + submission.getResultCode());
    }




    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param submission az aktuális M2M beküldési entitás
     */
    private void requireBizonylatApiSubmission(M2mSubmission submission) {
        if (submission.getInterfaceType() != InterfaceType.BIZONYLAT_API) {
            throw new IllegalStateException("Csak XML-t tartalmazó Bizonylat API csomag küldhető be. Az önálló Common Filestore csomag nem bizonylat.");
        }
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param submission az aktuális M2M beküldési entitás
     */
    private void requireMarkedForSubmit(M2mSubmission submission) {
        if (submission.getInternalStatus() != SubmissionStatus.MARKED_FOR_SUBMISSION
                && submission.getInternalStatus() != SubmissionStatus.SUBMIT_PENDING) {
            throw new IllegalStateException("Csak beküldésre megjelölt XML/csomag küldhető be. Jelenlegi státusz: " + submission.getInternalStatus());
        }
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param uploaded a művelethez átadott {@code uploaded} érték
     * @param operation a NAV vagy életciklus művelet neve
     * @param fileName a művelethez átadott {@code fileName} érték
     */
    private void requireUploadedFileId(NavGateway.UploadedFile uploaded, String operation, String fileName) {
        if (uploaded == null || uploaded.fileId() == null || uploaded.fileId().isBlank()) {
            String resultCode = uploaded == null ? "NO_RESPONSE" : uploaded.resultCode();
            String virusScan = uploaded == null ? null : uploaded.virusScanResultCode();
            throw NavOperationExceptionFactory.missingUploadedFileId(operation, fileName, resultCode, virusScan);
        }
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param operation a NAV vagy életciklus művelet neve
     */
    private void ensureNavRuntimeReadyForSignedOperation(M2mSubmission submission, String operation) {
        if (submission.getGatewayMode() != GatewayMode.REAL) {
            return;
        }

        M2mSignatureService.SignatureDebug currentDebug = signatureService.createSignatureDebug(nextMessageId(submission), "RUNTIME_CHECK");
        if (currentDebug.keyFirstPart() == null || currentDebug.keyFirstPart().isBlank()) {
            throw new IllegalStateException("Hiányzik a nav.m2m.signature.key-first-part érték. A nonce beváltás önmagában nem elég, az aláíráshoz az API kulcs 3. eleme is szükséges.");
        }

        if (runtimeSignatureKeyService.effectiveKeySecondPart() != null && !runtimeSignatureKeyService.effectiveKeySecondPart().isBlank()) {
            return;
        }

        logEvent(submission, "AUTO_REDEEM_NONCE_STARTED", operation, "STARTED", "Nincs runtime/config signatureKeySecondPart, ezért a beküldési lépés előtt automatikus nonce beváltás indul.");
        java.util.Map<String, Object> nonceResult = navRegistrationService.redeemNonce();
        logTraceDtoEvents(submission, nonceResult);
        Object nonceSuccess = nonceResult.get("success");
        String nonceResultCode = String.valueOf(nonceResult.getOrDefault("resultCode", "UNKNOWN"));
        String nonceResultMessage = String.valueOf(nonceResult.getOrDefault("resultMessage", nonceResult.getOrDefault("message", "")));
        if (!Boolean.TRUE.equals(nonceSuccess) || runtimeSignatureKeyService.effectiveKeySecondPart() == null || runtimeSignatureKeyService.effectiveKeySecondPart().isBlank()) {
            logEvent(submission, "AUTO_REDEEM_NONCE_FAILED", operation, nonceResultCode, nonceResultMessage);
            throw NavOperationExceptionFactory.nonceRedeemFailure(nonceResultCode, nonceResultMessage);
        }
        logEvent(submission, "AUTO_REDEEM_NONCE_DONE", operation, nonceResultCode, "Runtime signatureKeySecondPart eltárolva. source=" + runtimeSignatureKeyService.effectiveSource());

        java.util.Map<String, Object> activationResult = navRegistrationService.activateUserRegistration();
        logTraceDtoEvents(submission, activationResult);
        Object activationSuccess = activationResult.get("success");
        String activationResultCode = String.valueOf(activationResult.getOrDefault("resultCode", "UNKNOWN"));
        String activationMessage = String.valueOf(activationResult.getOrDefault("resultMessage", activationResult.getOrDefault("message", "")));
        if (Boolean.TRUE.equals(activationSuccess)) {
            logEvent(submission, "AUTO_ACTIVATION_DONE", operation, activationResultCode, activationMessage);
        } else {
            logEvent(submission, "AUTO_ACTIVATION_WARNING", operation, activationResultCode, "Az automatikus aktiválás nem adott sikeres választ, de a folyamat folytatódik. message=" + activationMessage);
        }
    }

    /**
     * A művelet diagnosztikai vagy audit információját rögzíti úgy, hogy érzékeny token vagy hitelesítési adat ne kerüljön a felhasználói naplóba.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param result az épülő eredménykollekció
     */
    @SuppressWarnings("unchecked")
    private void logTraceDtoEvents(M2mSubmission submission, java.util.Map<String, Object> result) {
        Object tracesObj = result == null ? null : result.get("traces");
        if (!(tracesObj instanceof List<?> traces)) {
            return;
        }
        for (Object item : traces) {
            if (!(item instanceof EventDto trace)) {
                continue;
            }
            M2mSubmissionEvent event = new M2mSubmissionEvent();
            event.setSubmission(submission);
            event.setEventType(trace.eventType());
            event.setNavOperation(trace.navOperation());
            event.setRequestMessageId(trace.requestMessageId() == null ? submission.getMessageId() : trace.requestMessageId());
            event.setResponseCode(trace.responseCode());
            event.setRequestHeaders(trace.requestHeaders());
            event.setRequestPayload(NavHttpAuditFormatter.limit(trace.requestPayload()));
            event.setResponseHeaders(NavHttpAuditFormatter.limit(trace.responseHeaders()));
            event.setResponsePayload(NavHttpAuditFormatter.limit(trace.responsePayload()));
            event.setConfigSnapshot(NavHttpAuditFormatter.limit(trace.configSnapshot()));
            eventRepository.save(event);
        }
    }

    /**
     * A(z) {@code recomputeStoredFileHash} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param operation a NAV vagy életciklus művelet neve
     * @param filePath a művelethez átadott {@code filePath} érték
     * @param previousHash a művelethez átadott {@code previousHash} érték
     * @return a művelet eredménye
     */
    private String recomputeStoredFileHash(M2mSubmission submission, String operation, Path filePath, String previousHash) {
        try (InputStream in = Files.newInputStream(filePath)) {
            String actualHash = Sha256Util.sha256Hex(in);
            boolean mismatch = previousHash != null && !previousHash.equalsIgnoreCase(actualHash);
            logEvent(submission, "FILE_HASH_CHECK", operation, mismatch ? "MISMATCH_FIXED" : "OK",
                    "filePath=" + filePath
                            + ", storedHash=" + previousHash
                            + ", actualHash=" + actualHash
                            + ", hashUsed=" + actualHash
                            + ", mismatch=" + mismatch);
            return actualHash;
        } catch (Exception e) {
            throw new IllegalStateException("Nem sikerült újraszámolni a fájl SHA-256 hash értékét: " + filePath, e);
        }
    }

    /**
     * Az aktuális konfiguráció alapján kiválasztja a használni kívánt valós vagy mock NAV gateway implementációt.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @return a művelet eredménye
     */
    private NavGateway gateway(M2mSubmission submission) {
        return submission.getGatewayMode() == GatewayMode.REAL ? realNavGateway : mockNavGateway;
    }


    /**
     * Feltölti vagy feltöltésre előkészíti a megadott fájlt a NAV filestore irányába, és az eredményt a beküldési állapothoz kapcsolja.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @return a művelet eredménye
     */
    private List<NavGateway.UploadedFile> uploadedAttachmentsFromDb(M2mSubmission submission) {
        List<NavGateway.UploadedFile> result = new ArrayList<>();
        for (M2mAttachment attachment : attachmentRepository.findBySubmissionIdOrderByCreatedAtAsc(submission.getId())) {
            if (attachment.getNavFileId() != null && !attachment.getNavFileId().isBlank()) {
                result.add(new NavGateway.UploadedFile(attachment.getNavFileId(), "ALREADY_UPLOADED", null));
            }
        }
        return result;
    }

    /**
     * A(z) {@code markTechnicalFailed} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param operation a NAV vagy életciklus művelet neve
     * @param e a feldolgozás közben kapott kivétel
     * @return a művelet eredménye
     */
    private M2mSubmission markTechnicalFailed(M2mSubmission submission, String operation, Exception e) {
        submission.setInternalStatus(SubmissionStatus.TECHNICAL_FAILED);
        submission.setResultCode("TECHNICAL_ERROR");
        submission.setResultMessage(e.getMessage());
        logEvent(submission, "TECHNICAL_FAILED", operation, "TECHNICAL_ERROR", e.getMessage());
        return submissionRepository.save(submission);
    }


    /**
     * Új, NAV kommunikációhoz használható messageId értéket állít elő.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @return a művelet eredménye
     */
    private String nextMessageId(M2mSubmission submission) {
        String messageId = UUID.randomUUID().toString();
        submission.setMessageId(messageId);
        submissionRepository.save(submission);
        return messageId;
    }

    /**
     * A(z) {@code eligibleValidationCertificate} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param currentPayloadHash a művelethez átadott {@code currentPayloadHash} érték
     * @return a művelet eredménye
     */
    private String eligibleValidationCertificate(M2mSubmission submission, String currentPayloadHash) {
        boolean successful = "SIKERES".equalsIgnoreCase(submission.getNavValidacioStatusz())
                && "SIKERES".equalsIgnoreCase(submission.getNavValidacioResultCode());
        boolean certificatePresent = submission.getNavValidaciosTanusitvany() != null
                && !submission.getNavValidaciosTanusitvany().isBlank();
        boolean samePayload = submission.getNavValidacioPayloadSha256() != null
                && currentPayloadHash != null
                && submission.getNavValidacioPayloadSha256().equalsIgnoreCase(currentPayloadHash);
        if (successful && certificatePresent && samePayload) {
            return submission.getNavValidaciosTanusitvany();
        }
        return null;
    }

    /**
     * A művelet diagnosztikai vagy audit információját rögzíti úgy, hogy érzékeny token vagy hitelesítési adat ne kerüljön a felhasználói naplóba.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param payload a naplózandó, szükség szerint maszkolt payload
     * @param uploadedAttachmentCount a művelethez átadott {@code uploadedAttachmentCount} érték
     * @param signature a művelethez átadott {@code signature} érték
     * @param validationCertificate a művelethez átadott {@code validationCertificate} érték
     * @param messageId a NAV kérés egyedi messageId értéke
     */
    private void logBizonylatRequestData(M2mSubmission submission, BizonylatPayload payload, int uploadedAttachmentCount, String signature, String validationCertificate, String messageId) {
        String endpoint = baseUrl(properties.getEndpoints().getBizonylatBaseUrl()) + properties.getEndpoints().getBizonylatPath();
        long payloadBytes = -1L;
        try {
            payloadBytes = Files.size(payload.path());
        } catch (Exception ignored) {
            // diagnosztikai naplozasnal nem akasztjuk meg a bekuldest
        }
        logEvent(submission, "M2M_BIZONYLAT_REQUEST_DATA", "BIZONYLAT_CREATE", "PREPARED",
                "endpoint=" + endpoint
                        + ", requestData.bizonylatTipus=" + submission.getBizonylatTipus()
                        + ", requestData.bizonylatVerzio=" + submission.getBizonylatVerzio()
                        + ", requestData.bizonylatXml.present=true"
                        + ", requestData.bizonylatXml.payloadBytes=" + payloadBytes
                        + ", requestData.bizonylatXml.sha256=" + payload.sha256Hex()
                        + ", requestData.tomorites=" + (payload.compression() == CompressionType.GZIP ? "GZIP" : "null")
                        + ", requestData.signature.present=" + (signature != null && !signature.isBlank())
                        + ", requestData.signature.length=" + (signature == null ? 0 : signature.length())
                        + ", requestData.megjegyzes.present=false"
                        + ", requestData.valaszTarhelyAzonosito.present=false"
                        + ", requestData.validaciosTanusitvany.present=" + (validationCertificate != null && !validationCertificate.isBlank())
                        + ", fastTrackSubmission=" + (validationCertificate != null && !validationCertificate.isBlank())
                        + ", uploadedAttachmentCount=" + uploadedAttachmentCount
                        + ", messageId=" + messageId
                        + ", metadataSource=SCHEMA_REGISTRY_EXPECTED"
                        + ", xmlFile=" + submission.getXmlFileName());
    }

    /**
     * Előkészíti vagy létrehozza az adott NAV M2M művelethez szükséges adatot, majd a következő feldolgozási lépésnek adja tovább.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param xmlForSubmit a művelethez átadott {@code xmlForSubmit} érték
     * @return a művelet eredménye
     * @throws Exception ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private BizonylatPayload prepareBizonylatPayload(M2mSubmission submission, Path xmlForSubmit) throws Exception {
        CompressionType compression = submission.getCompression() == null ? CompressionType.NONE : submission.getCompression();
        Path payloadPath = xmlForSubmit;
        if (compression == CompressionType.GZIP) {
            payloadPath = gzipToTempFile(xmlForSubmit);
        }
        String payloadHash;
        try (java.io.InputStream hashInput = Files.newInputStream(payloadPath)) {
            payloadHash = Sha256Util.sha256Hex(hashInput);
        }
        logEvent(submission, "BIZONYLAT_PAYLOAD_PREPARED", "LOCAL_PAYLOAD", "OK",
                "compression=" + compression
                        + ", hashInput=" + (compression == CompressionType.GZIP ? "GZIP_BYTES" : "RAW_XML_BYTES")
                        + ", payloadPath=" + payloadPath
                        + ", payloadSha256Hex=" + payloadHash);
        return new BizonylatPayload(payloadPath, compression, payloadHash);
    }

    /**
     * A(z) {@code gzipToTempFile} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param source a művelethez átadott {@code source} érték
     * @return a művelet eredménye
     * @throws Exception ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private Path gzipToTempFile(Path source) throws Exception {
        Path temp = SecureFileOperations.createPrivateTempFile("nav-m2m-bizonylat-", ".xml.gz");
        try (java.io.InputStream in = Files.newInputStream(source);
             OutputStream out = new GZIPOutputStream(SecureFileOperations.newPrivateOutputStream(temp))) {
            in.transferTo(out);
        }
        return temp;
    }

    /**
     * A NAV M2M submitter modul {@code BizonylatPayload} típusának felelősségét megvalósító típus.
     */
    /**
     * Létrehozza a(z) {@code BizonylatPayload} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param path a feldolgozandó vagy ellenőrzendő fájlútvonal
     * @param compression a művelethez átadott {@code compression} érték
     * @param sha256Hex a művelethez átadott {@code sha256Hex} érték
     */
    private record BizonylatPayload(Path path, CompressionType compression, String sha256Hex) {}

    /**
     * Az M2M életciklus vagy feldolgozási eredmény alapján frissíti a kezelt domain/runtime állapotot; a változás a hívó tranzakciójának része lehet.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param xmlPath a művelethez átadott {@code xmlPath} érték
     * @throws Exception ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void saveXmlReferences(M2mSubmission submission, Path xmlPath) throws Exception {
        List<XmlAttachmentReferenceExtractor.AttachmentReference> refs = referenceExtractor.extract(xmlPath);
        for (XmlAttachmentReferenceExtractor.AttachmentReference ref : refs) {
            XmlAttachmentReference entity = new XmlAttachmentReference();
            entity.setSubmission(submission);
            entity.setElementName(ref.elementName());
            entity.setFileId(ref.fileId());
            entity.setFileName(ref.fileName());
            entity.setFileSize(ref.fileSize());
            entity.setSequenceNo(ref.sequenceNo());
            referenceRepository.save(entity);
        }
    }



    /**
     * Az M2M életciklus vagy feldolgozási eredmény alapján frissíti a kezelt domain/runtime állapotot; a változás a hívó tranzakciójának része lehet.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void migrateAttachmentStorageToXmlFile(M2mSubmission submission) throws IOException {
        if (submission.getXmlFileId() == null) return;
        for (M2mAttachment attachment : attachmentRepository.findBySubmissionIdOrderByCreatedAtAsc(submission.getId())) {
            if (attachment.getStoragePath() == null) continue;
            Path source = Path.of(attachment.getStoragePath()).toAbsolutePath().normalize();
            if (!ExceptionSafeOperations.isRegularFile(source)) continue;
            Path targetDir = Path.of(properties.getStorageDirectory()).toAbsolutePath().normalize()
                    .resolve("xml-files").resolve(String.valueOf(submission.getXmlFileId()))
                    .resolve("attachments").resolve(attachment.getId().toString()).normalize();
            ExceptionSafeOperations.createDirectories(targetDir);
            Path target = targetDir.resolve(source.getFileName().toString()).normalize();
            if (!target.startsWith(targetDir)) throw new IOException("Érvénytelen csatolmány célútvonal.");
            SecureFileOperations.copyPrivate(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            attachment.setStoragePath(target.toString());
            attachmentRepository.save(attachment);
        }
    }

    /**
     * Az M2M életciklus vagy feldolgozási eredmény alapján frissíti a kezelt domain/runtime állapotot; a változás a hívó tranzakciójának része lehet.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param files a feldolgozandó fájlok listája
     * @throws Exception ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void saveStandaloneFiles(M2mSubmission submission, List<MultipartFile> files) throws Exception {
        if (files == null) return;
        int i = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            FileStorageService.StoredFile stored = fileStorageService.store(submission.getId(), file, "filestore_" + (++i));
            M2mAttachment a = new M2mAttachment();
            a.setSubmission(submission);
            a.setOriginalFileName(stored.originalFileName());
            a.setStoragePath(stored.storagePath());
            a.setFileSize(stored.fileSize());
            a.setSha256Hex(stored.sha256Hex());
            a.setXmlReferencePresent(false);
            attachmentRepository.save(a);
        }
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param attachments a feldolgozandó csatolmányok
     * @throws Exception ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void validateAttachmentFilesAgainstXml(M2mSubmission submission, List<MultipartFile> attachments) throws Exception {
        if (attachments == null || attachments.stream().noneMatch(file -> file != null && !file.isEmpty())) return;
        if (submission.getXmlStoragePath() == null || submission.getXmlStoragePath().isBlank()) {
            throw new IllegalStateException("A csatolmányok ellenőrzéséhez hiányzik a beküldési csomag XML-je.");
        }
        Set<String> xmlFileNames = new HashSet<>();
        for (XmlAttachmentReferenceExtractor.AttachmentReference reference
                : referenceExtractor.extract(Path.of(submission.getXmlStoragePath()))) {
            String key = fileNameKey(reference.fileName());
            if (key.isBlank()) continue;
            if (!xmlFileNames.add(key)) {
                throw new IllegalStateException("Az XML-en belül a csatolmány fájlnevek nem lehetnek egyformák: " + reference.fileName());
            }
        }
        if (xmlFileNames.isEmpty()) {
            throw new IllegalStateException("Az XML nem tartalmaz Attachment_1 elemet. Csatolmány csak azt támogató XSD alapján létrehozott XML-hez adható.");
        }
        for (MultipartFile file : attachments) {
            if (file == null || file.isEmpty()) continue;
            String originalName = fileStorageService.originalFileName(file);
            if (!xmlFileNames.contains(fileNameKey(originalName))) {
                throw new IllegalStateException("A csatolmányhoz nem található azonos fájlnevű Attachment_1 elem az XML-ben: " + originalName);
            }
        }
    }

    /**
     * Az M2M életciklus vagy feldolgozási eredmény alapján frissíti a kezelt domain/runtime állapotot; a változás a hívó tranzakciójának része lehet.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param attachments a feldolgozandó csatolmányok
     * @throws Exception ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void saveUploadedAttachments(M2mSubmission submission, List<MultipartFile> attachments) throws Exception {
        if (attachments == null) return;
        Set<String> names = new HashSet<>();
        attachmentRepository.findBySubmissionIdOrderByCreatedAtAsc(submission.getId()).stream()
                .map(M2mAttachment::getOriginalFileName)
                .map(this::fileNameKey)
                .filter(value -> !value.isBlank())
                .forEach(names::add);
        for (MultipartFile file : attachments) {
            if (file == null || file.isEmpty()) continue;
            String originalName = fileStorageService.originalFileName(file);
            String nameKey = fileNameKey(originalName);
            if (!names.add(nameKey)) {
                throw new IllegalStateException("Az XML-en belül a csatolmány fájlnevek nem lehetnek egyformák: " + originalName);
            }
            UUID attachmentId = UUID.randomUUID();
            FileStorageService.StoredFile stored = fileStorageService.storeAttachment(submission.getXmlFileId(), attachmentId, file);
            M2mAttachment a = new M2mAttachment();
            a.setId(attachmentId);
            a.setSubmission(submission);
            a.setOriginalFileName(stored.originalFileName());
            a.setStoragePath(stored.storagePath());
            a.setFileSize(stored.fileSize());
            a.setSha256Hex(stored.sha256Hex());
            a.setXmlReferencePresent(false);
            attachmentRepository.save(a);
        }
    }

    /**
     * A(z) {@code fileNameKey} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String fileNameKey(String value) {
        return Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Megszámolja a bemeneti kollekció feldolgozható, nem üres elemeit.
     *
     * @param files a feldolgozandó fájlok listája
     * @return a kiszámított darabszám vagy méret
     */
    private long countNonEmptyFiles(List<MultipartFile> files) {
        return files == null ? 0L : files.stream().filter(file -> file != null && !file.isEmpty()).count();
    }

    /**
     * A NAV oldali aktuális állapot lekérdezésével frissíti a helyi M2M életciklust és szükség szerint további pollingot ütemez.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param eventType a rögzítendő esemény típusa
     * @return a művelet eredménye
     */
    private M2mSubmission refreshStatusInternal(M2mSubmission submission, String eventType) {
        if (Boolean.TRUE.equals(submission.getM2mTerminal()) || isFinalStatus(submission.getInternalStatus())) {
            logEvent(submission, "M2M_STATUS_POLL_SKIPPED", "GET_STATUS", "TERMINAL", "A csomag végállapotban van, nincs további státuszlekérdezés. status=" + submission.getInternalStatus());
            return submissionRepository.save(submission);
        }
        if (submission.getInterfaceType() != InterfaceType.BIZONYLAT_API) {
            logEvent(submission, "M2M_STATUS_POLL_SKIPPED", "GET_STATUS", "NOT_SUPPORTED", "Csak Bizonylat API csomag pollolható. interfaceType=" + submission.getInterfaceType());
            return submissionRepository.save(submission);
        }
        String identifier = statusIdentifier(submission);
        if (identifier == null || identifier.isBlank()) {
            logEvent(submission, "M2M_STATUS_POLL_SKIPPED", "GET_STATUS", "MISSING_IDENTIFIER", "Nincs NAV azonosító státuszlekérdezéshez. ugyAzonosito=" + submission.getNavUgyAzonosito() + ", navFileId=" + submission.getNavFileId());
            return submissionRepository.save(submission);
        }

        String messageId = nextMessageId(submission);
        NavGateway.StatusResult status = gateway(submission).getStatus(identifier, messageId, submission.getCorrelationId());
        logHttpTraceEvents(submission);

        submission.setNavStatus(status.navStatus());
        if (status.erkeztetesiSzam() != null && !status.erkeztetesiSzam().isBlank()) {
            submission.setNavErkeztetesiSzam(status.erkeztetesiSzam());
        }
        submission.setResultCode(status.resultCode());
        submission.setResultMessage(status.message());
        submission.setM2mLastPollAt(Instant.now());
        submission.setM2mPollAttempts(safeInt(submission.getM2mPollAttempts()) + 1);
        submission.setInternalStatus(mapStatus(status.navStatus(), status.resultCode()));
        applyStatusLifecycle(submission);
        logEvent(submission, eventType, "GET_STATUS", status.resultCode(), "navStatus=" + status.navStatus() + ", erkeztetesiSzam=" + status.erkeztetesiSzam() + ", internalStatus=" + submission.getInternalStatus());
        return submissionRepository.save(submission);
    }

    /**
     * A technikai állapotot diagnosztikai vagy kliensoldali felhasználásra alkalmas, kontrollált szöveges reprezentációvá alakítja.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @return a művelet eredménye
     */
    private String statusIdentifier(M2mSubmission submission) {
        return submission.getNavUgyAzonosito();
    }


    /**
     * Az M2M életciklus vagy feldolgozási eredmény alapján frissíti a kezelt domain/runtime állapotot; a változás a hívó tranzakciójának része lehet.
     *
     * @param submission az aktuális M2M beküldési entitás
     */
    private void resetM2mLifecycleForNewSubmission(M2mSubmission submission) {
        submission.setM2mSubmitMarkedAt(null);
        submission.setM2mSubmittedAt(null);
        submission.setM2mFinalizedAt(null);
        submission.setM2mNextPollAt(null);
        submission.setM2mLastPollAt(null);
        submission.setM2mPollAttempts(0);
        submission.setM2mTerminal(false);
        submission.setM2mResubmittable(true);
    }

    /**
     * Az M2M életciklus vagy feldolgozási eredmény alapján frissíti a kezelt domain/runtime állapotot; a változás a hívó tranzakciójának része lehet.
     *
     * @param submission az aktuális M2M beküldési entitás
     */
    private void applyStatusLifecycle(M2mSubmission submission) {
        Instant now = Instant.now();
        if (submission.getM2mSubmittedAt() == null && (submission.getInternalStatus() == SubmissionStatus.SUBMITTING
                || submission.getInternalStatus() == SubmissionStatus.SUBMISSION_IN_PROGRESS
                || submission.getInternalStatus() == SubmissionStatus.SUBMITTED_OK
                || submission.getInternalStatus() == SubmissionStatus.SUBMITTED_WITH_ERROR)) {
            submission.setM2mSubmittedAt(now);
        }

        if (isFinalStatus(submission.getInternalStatus())) {
            submission.setM2mTerminal(true);
            submission.setM2mResubmittable(false);
            submission.setM2mNextPollAt(null);
            if (submission.getM2mFinalizedAt() == null) {
                submission.setM2mFinalizedAt(now);
            }
            logEvent(submission, "M2M_TERMINAL_STATUS_REACHED", "STATUS_CLASSIFICATION", "TERMINAL", "internalStatus=" + submission.getInternalStatus() + ", navStatus=" + submission.getNavStatus() + ", resultCode=" + submission.getResultCode());
            return;
        }

        if (isPollableStatus(submission.getInternalStatus())) {
            submission.setM2mTerminal(false);
            submission.setM2mResubmittable(false);
            scheduleNextPoll(submission);
            logEvent(submission, "M2M_STATUS_POLL_SCHEDULED", "STATUS_CLASSIFICATION", "POLLABLE", "internalStatus=" + submission.getInternalStatus() + ", nextPollAt=" + submission.getM2mNextPollAt());
            return;
        }

        submission.setM2mTerminal(false);
        submission.setM2mNextPollAt(null);
        submission.setM2mResubmittable(submission.getInternalStatus() == SubmissionStatus.SUBMISSION_TECHNICAL_FAILED
                || submission.getInternalStatus() == SubmissionStatus.TECHNICAL_FAILED
                || submission.getInternalStatus() == SubmissionStatus.SUBMISSION_MARK_WITHDRAWN
                || submission.getInternalStatus() == SubmissionStatus.CREATED);
    }

    /**
     * A(z) {@code scheduleNextPoll} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param submission az aktuális M2M beküldési entitás
     */
    private void scheduleNextPoll(M2mSubmission submission) {
        java.time.Duration interval = properties.getStatusPoll() == null ? java.time.Duration.ofSeconds(60) : properties.getStatusPoll().getInterval();
        if (interval == null || interval.isNegative() || interval.isZero()) {
            interval = java.time.Duration.ofSeconds(60);
        }
        submission.setM2mNextPollAt(Instant.now().plus(interval));
    }

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param status a vizsgált beküldési státusz
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private boolean isFinalStatus(SubmissionStatus status) {
        return status == SubmissionStatus.SUBMITTED_OK || status == SubmissionStatus.SUBMITTED_WITH_ERROR;
    }

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param status a vizsgált beküldési státusz
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private boolean isInProgressStatus(SubmissionStatus status) {
        return status == SubmissionStatus.SUBMITTING || status == SubmissionStatus.SUBMISSION_IN_PROGRESS || status == SubmissionStatus.SUBMIT_PENDING;
    }

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param status a vizsgált beküldési státusz
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private boolean isPollableStatus(SubmissionStatus status) {
        return status == SubmissionStatus.SUBMITTING || status == SubmissionStatus.SUBMISSION_IN_PROGRESS || status == SubmissionStatus.SUBMIT_PENDING;
    }

    /**
     * A(z) {@code safeInt} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * A bemeneti domain/transport adatokból a következő feldolgozási réteg által igényelt reprezentációt állítja elő.
     *
     * @param navStatus a NAV válaszában kapott státuszérték
     * @param resultCode a NAV eredménykód
     * @return a művelet eredménye
     */
    private SubmissionStatus mapStatus(String navStatus, String resultCode) {
        String normalizedResultCode = resultCode == null ? null : resultCode.trim().toUpperCase(java.util.Locale.ROOT);
        String normalizedStatus = navStatus == null ? null : navStatus.trim().toUpperCase(java.util.Locale.ROOT);

        if (normalizedResultCode != null
                && !normalizedResultCode.isBlank()
                && !"SIKERES".equals(normalizedResultCode)
                && !"OK".equals(normalizedResultCode)
                && !"SUCCESS".equals(normalizedResultCode)) {
            return SubmissionStatus.SUBMITTED_WITH_ERROR;
        }

        if (normalizedStatus == null || normalizedStatus.isBlank()) return SubmissionStatus.SUBMISSION_IN_PROGRESS;
        return switch (normalizedStatus) {
            case "VALIDACIO_ALATT", "BEKULDES_ALATT", "SIKERESEN_VALIDALT" -> SubmissionStatus.SUBMISSION_IN_PROGRESS;
            case "SIKERES", "SIKERESEN_BEKULDVE" -> SubmissionStatus.SUBMITTED_OK;
            case "VALIDACIOS_HIBA", "SIKERTELEN_ELOELLENORZES", "SIKERTELEN_BEKULDES" -> SubmissionStatus.SUBMITTED_WITH_ERROR;
            default -> SubmissionStatus.SUBMISSION_IN_PROGRESS;
        };
    }

    /**
     * Perzisztens M2M eseménynapló-bejegyzést hoz létre a megadott műveleti és payload adatokkal.
     *
     * @param submission az aktuális M2M beküldési entitás
     * @param eventType a rögzítendő esemény típusa
     * @param operation a NAV vagy életciklus művelet neve
     * @param code a művelet eredmény- vagy hibakódja
     * @param payload a naplózandó, szükség szerint maszkolt payload
     */
    private void logEvent(M2mSubmission submission, String eventType, String operation, String code, String payload) {
        M2mSubmissionEvent event = new M2mSubmissionEvent();
        event.setSubmission(submission);
        event.setEventType(eventType);
        event.setNavOperation(operation);
        event.setRequestMessageId(submission.getMessageId());
        event.setResponseCode(code);
        event.setResponsePayload(NavHttpAuditFormatter.limit(payload));
        eventRepository.save(event);
    }

    /**
     * Az aktuális NAV gateway hívás során összegyűlt HTTP trace eseményeket a beküldés eseménynaplójához rendeli.
     *
     * @param submission az aktuális M2M beküldési entitás
     */
    private void logHttpTraceEvents(M2mSubmission submission) {
        for (NavHttpTrace trace : NavHttpAuditHolder.drain()) {
            M2mSubmissionEvent event = new M2mSubmissionEvent();
            event.setSubmission(submission);
            event.setEventType("NAV_HTTP_TRACE");
            event.setNavOperation(trace.operation());
            event.setRequestMessageId(submission.getMessageId());
            event.setResponseCode(trace.responseStatus());
            event.setRequestHeaders(trace.method() + " " + trace.url() + "\n" + nullToEmpty(trace.requestHeaders()));
            event.setRequestPayload(NavHttpAuditFormatter.limit(trace.requestPayload()));
            event.setResponseHeaders(NavHttpAuditFormatter.limit(trace.responseHeaders()));
            event.setResponsePayload(NavHttpAuditFormatter.limit(trace.responsePayload()));
            event.setConfigSnapshot(NavHttpAuditFormatter.limit(trace.configSnapshot()));
            eventRepository.save(event);
        }
    }

    /**
     * Null szöveget üres szöveggé alakít, egyébként változatlanul adja vissza az értéket.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Újraértékeli vagy újrafeltölti a csatolmányt az XML/NAV állapothoz igazodva.
     *
     * @param submissionId a cél M2M beküldés azonosítója
     * @param attachmentId a cél csatolmány azonosítója
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse refreshAttachment(UUID submissionId, UUID attachmentId) {
        M2mSubmission submission = RepositoryAccess.findById(submissionRepository, submissionId).orElseThrow();
        ensureNotSuccessfullyFinalized(submission);
        M2mAttachment attachment = RepositoryAccess.findById(attachmentRepository, attachmentId).orElseThrow();
        if (!attachment.getSubmission().getId().equals(submissionId)) {
            throw new IllegalArgumentException("A csatolmány nem ehhez a beküldési csomaghoz tartozik.");
        }
        AttachmentLifecycleEvaluator.Evaluation lifecycle =
                AttachmentLifecycleEvaluator.evaluate(attachment, properties, Instant.now());
        if (!lifecycle.refreshAllowed()) {
            throw new IllegalStateException(lifecycle.reason());
        }
        try {
            Path path = Path.of(attachment.getStoragePath());
            if (!ExceptionSafeOperations.isRegularFile(path)) throw new IllegalStateException("A helyi csatolmány nem található: " + attachment.getOriginalFileName());
            ensureNavRuntimeReadyForSignedOperation(submission, "ATTACHMENT_REFRESH");
            String hash = recomputeStoredFileHash(submission, "ATTACHMENT_REFRESH", path, attachment.getSha256Hex());
            NavGateway.UploadedFile uploaded = gateway(submission).uploadFile(path, attachment.getOriginalFileName(), hash,
                    Files.size(path), nextMessageId(submission), submission.getCorrelationId());
            requireUploadedFileId(uploaded, "ATTACHMENT_REFRESH", attachment.getOriginalFileName());
            Instant now = Instant.now();
            attachment.setNavFileId(uploaded.fileId());
            attachment.setNavUploadedAt(now);
            attachment.setNavLastRefreshedAt(now);
            attachment.setNavExpiresAt(now.plus(properties.getAttachment().getValidityDuration()));
            attachment.setNavUploadResultCode(uploaded.resultCode());
            attachment.setNavUploadResultMessage("virusScan=" + uploaded.virusScanResultCode());
            attachmentRepository.save(attachment);
            logEvent(submission, "ATTACHMENT_REFRESHED", "COMMON_FILE_UPLOAD", uploaded.resultCode(),
                    attachment.getOriginalFileName() + " -> " + uploaded.fileId());
            return mapper.toResponse(submission);
        } catch (Exception e) {
            attachment.setNavUploadResultCode("REFRESH_FAILED");
            attachment.setNavUploadResultMessage(e.getMessage());
            attachmentRepository.save(attachment);
            throw new IllegalStateException("A csatolmány információinak frissítése sikertelen: " + e.getMessage(), e);
        }
    }


    /**
     * Törli a csatolmányt, ha a beküldési végállapot és az XML-kapcsolat szabályai ezt megengedik.
     *
     * @param submissionId a cél M2M beküldés azonosítója
     * @param attachmentId a cél csatolmány azonosítója
     * @return a művelet eredménye
     */
    @Transactional
    public SubmissionResponse deleteAttachment(UUID submissionId, UUID attachmentId) {
        M2mSubmission submission = RepositoryAccess.findById(submissionRepository, submissionId).orElseThrow();
        ensureNotSuccessfullyFinalized(submission);
        M2mAttachment attachment = RepositoryAccess.findById(attachmentRepository, attachmentId).orElseThrow();
        if (!attachment.getSubmission().getId().equals(submissionId)) {
            throw new IllegalArgumentException("A csatolmány nem ehhez a beküldési csomaghoz tartozik.");
        }
        Path path = attachment.getStoragePath() == null ? null : Path.of(attachment.getStoragePath());
        if (path != null) {
            try { Files.deleteIfExists(path); }
            catch (IOException e) { throw new IllegalStateException("A csatolmány fizikai törlése sikertelen: " + e.getMessage(), e); }
        }
        int deleted = attachmentRepository.deleteByIdAndSubmissionId(attachmentId, submissionId);
        if (deleted != 1) {
            throw new IllegalStateException("A csatolmány adatbázisrekordjának törlése sikertelen.");
        }
        logEvent(submission, "ATTACHMENT_DELETED", "LOCAL", "OK", attachment.getOriginalFileName());
        return mapper.toResponse(submission);
    }


    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param xmlFileId az érintett XML-fájl adatbázis-azonosítója
     */
    private void ensureXmlFileNotSuccessfullyFinalized(Long xmlFileId) {
        if (xmlFileId == null) return;
        if (submissionRepository.existsByXmlFileIdAndInternalStatus(xmlFileId, SubmissionStatus.SUBMITTED_OK)) {
            throw new IllegalStateException("Az űrlap korábban már sikeresen beküldésre került, ezért végállapotban van és csak megtekinthető.");
        }
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param submission az aktuális M2M beküldési entitás
     */
    private void ensureNotSuccessfullyFinalized(M2mSubmission submission) {
        if (submission == null) return;
        if (submission.getInternalStatus() == SubmissionStatus.SUBMITTED_OK
                || (submission.getXmlFileId() != null
                && submissionRepository.existsByXmlFileIdAndInternalStatus(submission.getXmlFileId(), SubmissionStatus.SUBMITTED_OK))) {
            throw new IllegalStateException("A sikeresen beküldött űrlap végállapotban van. Nem módosítható, nem csatolható hozzá állomány és nem küldhető be újra; kizárólag megtekinthető.");
        }
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param submission az aktuális M2M beküldési entitás
     */
    private void ensureResubmissionAllowed(M2mSubmission submission) {
        if (properties.getSubmission().isAllowResubmit()) return;
        boolean priorSuccess = submissionRepository.findByXmlSha256HexOrderByCreatedAtDesc(submission.getXmlSha256Hex()).stream()
                .filter(previous -> !previous.getId().equals(submission.getId()))
                .anyMatch(previous -> "SIKERES".equalsIgnoreCase(previous.getResultCode())
                        || "SIKERES".equalsIgnoreCase(previous.getNavStatus()));
        if (priorSuccess || "SIKERES".equalsIgnoreCase(submission.getResultCode()) || "SIKERES".equalsIgnoreCase(submission.getNavStatus())) {
            throw new IllegalStateException("Az állomány korábban már sikeresen beküldésre került. Az ismételt beküldés ebben a környezetben nem engedélyezett.");
        }
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param submission az aktuális M2M beküldési entitás
     */
    private void ensureAttachmentsValidForSubmission(M2mSubmission submission) {
        Instant limit = Instant.now().plus(properties.getAttachment().getExpirySafetyMargin());
        List<M2mAttachment> invalid = attachmentRepository.findBySubmissionIdOrderByCreatedAtAsc(submission.getId()).stream()
                .filter(a -> a.getNavFileId() == null || a.getNavFileId().isBlank()
                        || a.getNavExpiresAt() == null || !a.getNavExpiresAt().isAfter(limit))
                .toList();
        if (!invalid.isEmpty()) {
            String names = invalid.stream().map(M2mAttachment::getOriginalFileName).collect(java.util.stream.Collectors.joining(", "));
            throw new IllegalStateException("A beküldés nem indítható, mert lejárt vagy nem feltöltött csatolmány található: " + names
                    + ". Használja a Csatolmány megújítása műveletet.");
        }
    }

}
