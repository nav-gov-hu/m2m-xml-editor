package hu.nav.m2m.submitter.controller;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.nav.m2m.submitter.support.RepositoryAccess;

import hu.nav.m2m.submitter.domain.GatewayMode;
import hu.nav.m2m.submitter.domain.CompressionType;
import hu.nav.m2m.submitter.dto.EventDto;
import hu.nav.m2m.submitter.dto.SubmissionResponse;
import hu.nav.m2m.submitter.dto.ValidationErrorDetailsResponse;
import hu.nav.m2m.submitter.repo.M2mSubmissionEventRepository;
import hu.nav.m2m.submitter.repo.M2mSubmissionRepository;
import hu.nav.m2m.submitter.repo.M2mAttachmentRepository;
import hu.nav.m2m.submitter.service.SubmissionService;
import hu.nav.m2m.submitter.service.M2mAvailabilityService;
import hu.nav.m2m.submitter.service.ManagedStoragePathPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Az M2M beküldési életciklus REST belépési pontja: XML-csatolás, beküldésre jelölés, csatolmányok, NAV műveletek, státusz- és naplólekérdezések kezelését delegálja a szolgáltatási rétegnek.
 */
@RestController
@RequestMapping("/api/submissions")
@Tag(name = "NAV M2M beküldések", description = "Kész XML bizonylatok beküldése a NAV Bizonylat API irányába")
public class SubmissionController {
    private final SubmissionService submissionService;
    private final M2mSubmissionEventRepository eventRepository;
    private final M2mSubmissionRepository submissionRepository;
    private final M2mAttachmentRepository attachmentRepository;
    private final M2mAvailabilityService availabilityService;
    private final ManagedStoragePathPolicy storagePathPolicy;

    /**
     * Létrehozza a(z) {@code SubmissionController} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param submissionService a művelethez átadott {@code submissionService} érték
     * @param eventRepository a művelethez átadott {@code eventRepository} érték
     * @param submissionRepository a művelethez átadott {@code submissionRepository} érték
     * @param attachmentRepository a művelethez átadott {@code attachmentRepository} érték
     * @param availabilityService a művelethez átadott {@code availabilityService} érték
     * @param storagePathPolicy a művelethez átadott {@code storagePathPolicy} érték
     */
    public SubmissionController(SubmissionService submissionService, M2mSubmissionEventRepository eventRepository,
                                M2mSubmissionRepository submissionRepository, M2mAttachmentRepository attachmentRepository,
                                M2mAvailabilityService availabilityService, ManagedStoragePathPolicy storagePathPolicy) {
        this.submissionService = submissionService;
        this.eventRepository = eventRepository;
        this.submissionRepository = submissionRepository;
        this.attachmentRepository = attachmentRepository;
        this.availabilityService = availabilityService;
        this.storagePathPolicy = storagePathPolicy;
    }

    /**
     * Létrehozza a kért M2M erőforrást vagy beküldési munkamenetet, és a szolgáltatási réteg eredményét REST válasszá alakítja.
     *
     * @return a művelet eredménye
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Új beküldési csomag létrehozása és opcionális azonnali beküldése")
    public SubmissionResponse create(
            @RequestParam(value = "gatewayMode", defaultValue = "MOCK") GatewayMode gatewayMode,
            @RequestParam(value = "bizonylatTipus", required = false) String bizonylatTipus,
            @RequestParam(value = "bizonylatVerzio", required = false) String bizonylatVerzio,
            @RequestParam(value = "compression", defaultValue = "NONE") CompressionType compression,
            @RequestPart("xml") MultipartFile xml,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
            @RequestParam(value = "submitNow", defaultValue = "false") boolean submitNow,
            @RequestParam(value = "xmlFileId", required = false) Long xmlFileId) {
        if (bizonylatTipus != null && bizonylatTipus.length() > 64) throw new IllegalArgumentException("A bizonylatTipus túl hosszú.");
        if (bizonylatVerzio != null && bizonylatVerzio.length() > 64) throw new IllegalArgumentException("A bizonylatVerzio túl hosszú.");
        if (submitNow) availabilityService.requireConfigured();
        GatewayMode validatedGatewayMode = switch (gatewayMode == null ? GatewayMode.MOCK : gatewayMode) {
            case MOCK -> GatewayMode.MOCK;
            case REAL -> GatewayMode.REAL;
        };
        CompressionType validatedCompression = switch (compression == null ? CompressionType.NONE : compression) {
            case NONE -> CompressionType.NONE;
            case GZIP -> CompressionType.GZIP;
        };
        String validatedType = validateOptionalFormIdentifier(bizonylatTipus, "bizonylatTipus");
        String validatedVersion = validateOptionalFormIdentifier(bizonylatVerzio, "bizonylatVerzio");
        Long validatedXmlFileId = validateOptionalPositiveId(xmlFileId, "xmlFileId");
        return submissionService.createAndOptionallySubmit(validatedType, validatedVersion, validatedCompression, validatedGatewayMode, xml, attachments, submitNow, validatedXmlFileId);
    }


    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param value a feldolgozandó érték
     * @param fieldName a művelethez átadott {@code fieldName} érték
     * @return a művelet eredménye
     */
    private static String validateOptionalFormIdentifier(String value, String fieldName) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (!normalized.matches("[A-Za-z0-9._-]{1,64}")) {
            throw new IllegalArgumentException("Érvénytelen " + fieldName + " érték.");
        }
        return normalized;
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param value a feldolgozandó érték
     * @param fieldName a művelethez átadott {@code fieldName} érték
     * @return a művelet eredménye
     */
    private static Long validateOptionalPositiveId(Long value, String fieldName) {
        if (value == null) return null;
        if (value <= 0) throw new IllegalArgumentException("Érvénytelen " + fieldName + " érték.");
        String decimal = Long.toString(value.longValue());
        if (!decimal.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("Érvénytelen " + fieldName + " érték.");
        }
        return Long.valueOf(decimal);
    }

    /**
     * Előkészíti vagy létrehozza az adott NAV M2M művelethez szükséges adatot, majd a következő feldolgozási lépésnek adja tovább.
     *
     * @return a művelet eredménye
     */
    @PostMapping(value = "/filestore-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Önálló Common Filestore fájlcsomag létrehozása és opcionális feltöltése")
    public SubmissionResponse createFilestoreFiles(
            @RequestParam(value = "gatewayMode", defaultValue = "MOCK") GatewayMode gatewayMode,
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(value = "uploadNow", defaultValue = "false") boolean uploadNow) {
        if (uploadNow) availabilityService.requireConfigured();
        GatewayMode validatedGatewayMode = switch (gatewayMode == null ? GatewayMode.MOCK : gatewayMode) {
            case MOCK -> GatewayMode.MOCK;
            case REAL -> GatewayMode.REAL;
        };
        return submissionService.createStandaloneFilestorePackage(validatedGatewayMode, files, uploadNow);
    }

    /**
     * Lekéri a hívó számára látható M2M erőforrások listáját.
     *
     * @return a művelet eredménye
     */
    @GetMapping
    @Operation(summary = "Beküldések listázása")
    public List<SubmissionResponse> list() {
        return submissionService.list();
    }

    /**
     * Lekéri a kért M2M erőforrást vagy aktuális konfigurációt.
     *
     * @return a művelet eredménye
     */
    @GetMapping("/{id}")
    @Operation(summary = "Beküldés lekérdezése")
    public SubmissionResponse get(@PathVariable("id") UUID id) {
        return submissionService.get(id);
    }

    /**
     * A(z) {@code addAttachments} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Új csatolmányok hozzáadása meglévő beküldési csomaghoz")
    public SubmissionResponse addAttachments(@PathVariable("id") UUID id,
                                             @RequestPart("attachments") List<MultipartFile> attachments) {
        availabilityService.requireConfigured();
        return submissionService.addAttachments(id, attachments);
    }

    /**
     * Beküldésre jelöli az XML-hez tartozó M2M munkamenetet, ha az életciklus és csatolmányállapot ezt engedi.
     *
     * @return a művelet eredménye
     */
    @PostMapping("/{id}/mark-for-submit")
    @Operation(summary = "XML/csomag megjelölése M2M beküldésre")
    public SubmissionResponse markForSubmit(@PathVariable("id") UUID id) {
        availabilityService.requireConfigured();
        return submissionService.markForSubmit(id);
    }

    /**
     * Visszavonja a beküldésre jelölést, amennyiben a beküldés még nem került végleges vagy aktív NAV állapotba.
     *
     * @return a művelet eredménye
     */
    @PostMapping("/{id}/withdraw-submit-mark")
    @Operation(summary = "M2M beküldésre jelölés visszavonása")
    public SubmissionResponse withdrawSubmitMark(@PathVariable("id") UUID id) {
        return submissionService.withdrawSubmitMark(id);
    }

    /**
     * A(z) {@code m2mLogs} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    @GetMapping("/{id}/m2m-logs")
    @Operation(summary = "M2M esemény- és kommunikációs napló lekérdezése")
    public List<EventDto> m2mLogs(@PathVariable("id") UUID id) {
        return events(id);
    }

    /**
     * Elindítja a beküldéshez tartozó üzleti folyamatot a szükséges preflight ellenőrzésekkel.
     *
     * @return a művelet eredménye
     */
    @PostMapping("/{id}/submit")
    @Operation(summary = "Korábban létrehozott, beküldésre megjelölt csomag beküldése")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.M2M_SUBMIT)
    public SubmissionResponse submit(@PathVariable("id") UUID id) {
        availabilityService.requireConfigured();
        return submissionService.submit(id);
    }

    /**
     * A(z) {@code replaceXmlContent} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    @PostMapping(value = "/{id}/xml-content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Beküldési csomag XML tartalmának frissítése az űrlapnézetből")
    public SubmissionResponse replaceXmlContent(@PathVariable("id") UUID id, @RequestPart("xml") MultipartFile xml,
                                                @RequestParam(value = "xmlFileId", required = false) Long xmlFileId) {
        Long validatedXmlFileId = validateOptionalPositiveId(xmlFileId, "xmlFileId");
        return submissionService.replaceXmlContent(id, xml, validatedXmlFileId);
    }



    /**
     * Feltölti vagy feltöltésre előkészíti a megadott fájlt a NAV filestore irányába, és az eredményt a beküldési állapothoz kapcsolja.
     *
     * @return a művelet eredménye
     */
    @PostMapping("/{id}/step/upload-attachments")
    @Operation(summary = "Lépésenkénti teszt: csatolmányok feltöltése Common Filestore-ba")
    public SubmissionResponse uploadAttachmentsStep(@PathVariable("id") UUID id) {
        availabilityService.requireConfigured();
        return submissionService.uploadAttachmentsStep(id);
    }



    /**
     * Előkészíti vagy létrehozza az adott NAV M2M művelethez szükséges adatot, majd a következő feldolgozási lépésnek adja tovább.
     *
     * @return a művelet eredménye
     */
    @PostMapping("/{id}/step/create-bizonylat")
    @Operation(summary = "Lépésenkénti teszt: Bizonylat API createBizonylat hívás")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.M2M_SUBMIT)
    public SubmissionResponse createBizonylatStep(@PathVariable("id") UUID id) {
        availabilityService.requireConfigured();
        return submissionService.createBizonylatStep(id);
    }

    /**
     * A(z) {@code onlineValidation} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    @PostMapping("/{id}/validation/online")
    @Operation(summary = "Online NAV Bizonylat validáció indítása")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.M2M_SUBMIT)
    public SubmissionResponse onlineValidation(@PathVariable("id") UUID id) {
        availabilityService.requireConfigured();
        return submissionService.onlineValidacio(id);
    }

    /**
     * A(z) {@code validationStatus} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    @GetMapping("/{id}/validation/status")
    @Operation(summary = "Online NAV Bizonylat validáció státuszának lekérdezése")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.M2M_SUBMIT)
    public SubmissionResponse validationStatus(@PathVariable("id") UUID id) {
        return submissionService.validacioStatus(id);
    }

    /**
     * A(z) {@code validationErrors} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    @GetMapping("/{id}/validation/errors")
    @Operation(summary = "Az online validáció BZip2/Base64 hibalistájának kibontása")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.M2M_SUBMIT)
    public ValidationErrorDetailsResponse validationErrors(@PathVariable("id") UUID id) {
        return submissionService.validationErrorDetails(id);
    }

    /**
     * A(z) {@code onlineCalculation} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    @PostMapping("/{id}/calculation/online")
    @Operation(summary = "Online NAV Bizonylat kalkuláció indítása")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.M2M_SUBMIT)
    public SubmissionResponse onlineCalculation(@PathVariable("id") UUID id) {
        availabilityService.requireConfigured();
        return submissionService.onlineKalkulacio(id);
    }

    /**
     * A(z) {@code calculationResult} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    @GetMapping("/{id}/calculation/result")
    @Operation(summary = "Online NAV Bizonylat kalkuláció eredményének lekérdezése")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.M2M_SUBMIT)
    public SubmissionResponse calculationResult(@PathVariable("id") UUID id) {
        return submissionService.kalkulacioEredmeny(id);
    }

    /**
     * Frissíti a megadott beküldés NAV oldali állapotát és visszaadja az aktuális reprezentációt.
     *
     * @return a művelet eredménye
     */
    @PostMapping("/{id}/refresh")
    @Operation(summary = "NAV státusz frissítése")
    public SubmissionResponse refresh(@PathVariable("id") UUID id) {
        return submissionService.refresh(id);
    }

    /**
     * Lekéri és REST DTO-vá alakítja a beküldés eseménynaplóját.
     *
     * @return a művelet eredménye
     */
    @GetMapping("/{id}/events")
    @Operation(summary = "Beküldés eseménynaplója")
    public List<EventDto> events(@PathVariable("id") UUID id) {
        return eventRepository.findBySubmissionIdOrderByCreatedAtAsc(id).stream()
                .map(e -> new EventDto(e.getEventType(), e.getNavOperation(), e.getRequestMessageId(), e.getResponseCode(),
                        e.getRequestHeaders(), e.getRequestPayload(), e.getResponseHeaders(), e.getResponsePayload(),
                        e.getConfigSnapshot(), e.getCreatedAt()))
                .toList();
    }



    /**
     * A(z) {@code latestForXmlFile} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param xmlFileId az érintett XML-fájl adatbázis-azonosítója
     * @return a művelet eredménye
     */
    @GetMapping("/xml-files/{xmlFileId}/latest")
    @Operation(summary = "Az XML-állomány legutóbbi M2M beküldési csomagjának lekérdezése")
    public ResponseEntity<SubmissionResponse> latestForXmlFile(@PathVariable Long xmlFileId) {
        return submissionRepository.findFirstByXmlFileIdOrderByUpdatedAtDesc(xmlFileId)
                .map(submission -> ResponseEntity.ok(submissionService.get(submission.getId())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Törli a csatolmányt, ha a beküldési végállapot és az XML-kapcsolat szabályai ezt megengedik.
     *
     * @return a művelet eredménye
     */
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @Operation(summary = "Csatolmány törlése a helyi tárból és a beküldési csomagból")
    public SubmissionResponse deleteAttachment(@PathVariable("id") UUID id, @PathVariable("attachmentId") UUID attachmentId) {
        return submissionService.deleteAttachment(id, attachmentId);
    }

    /**
     * Újraértékeli vagy újrafeltölti a csatolmányt az XML/NAV állapothoz igazodva.
     *
     * @return a művelet eredménye
     */
    @PostMapping("/{id}/attachments/{attachmentId}/refresh")
    @Operation(summary = "Egy csatolmány ismételt NAV feltöltése és lejárati adatainak frissítése")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.M2M_SUBMIT)
    public SubmissionResponse refreshAttachment(@PathVariable("id") UUID id, @PathVariable("attachmentId") UUID attachmentId) {
        availabilityService.requireConfigured();
        return submissionService.refreshAttachment(id, attachmentId);
    }

    /**
     * A kért csatolmányt csak a menedzselt tárhely- és jogosultsági szabályok teljesülése esetén adja vissza.
     *
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    @GetMapping("/{id}/attachments/{attachmentId}/content")
    @Operation(summary = "Csatolmány megtekintése vagy letöltése")
    public ResponseEntity<byte[]> attachmentContent(@PathVariable("id") UUID id,
                                                     @PathVariable("attachmentId") UUID attachmentId,
                                                     @RequestParam(value = "download", defaultValue = "false") boolean download) throws IOException {
        var attachment = attachmentRepository.findForSubmission(attachmentId, id).orElseThrow();
        MediaType type = resolveMediaType(attachment.getOriginalFileName());
        String disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(safeName(attachment.getOriginalFileName()), StandardCharsets.UTF_8)
                .build().toString();
        byte[] content = attachment.getSubmission() != null && attachment.getSubmission().getXmlFileId() != null
                ? storagePathPolicy.readXmlFileAttachment(attachment.getSubmission().getXmlFileId(), attachment.getId(), attachment.getStoragePath())
                : storagePathPolicy.readSubmissionAttachment(id, attachment.getStoragePath());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, disposition).contentType(type).body(content);
    }


    /**
     * A(z) {@code xmlFileAttachmentCounts} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @return a művelet eredménye
     */
    @GetMapping("/xml-files/attachment-counts")
    @Operation(summary = "Űrlapállományok csatolmánydarabszámának lekérdezése")
    public java.util.Map<Long, Long> xmlFileAttachmentCounts(@RequestParam("ids") List<Long> ids) {
        java.util.Map<Long, Long> result = new java.util.LinkedHashMap<>();
        if (ids != null) ids.stream().filter(java.util.Objects::nonNull).distinct()
                .forEach(id -> result.put(id, attachmentRepository.findBySubmissionXmlFileIdOrderByCreatedAtAsc(id).stream()
                        .filter(a -> isManagedAttachmentReadable(a.getStoragePath()))
                        .count()));
        return result;
    }

    /**
     * A(z) {@code xmlFileAttachments} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param xmlFileId az érintett XML-fájl adatbázis-azonosítója
     * @return a művelet eredménye
     */
    @GetMapping("/xml-files/{xmlFileId}/attachments")
    @Operation(summary = "Űrlapállomány csatolmányainak listázása")
    public List<XmlFileAttachmentDto> xmlFileAttachments(@PathVariable Long xmlFileId) {
        return attachmentRepository.findBySubmissionXmlFileIdOrderByCreatedAtAsc(xmlFileId).stream()
                .filter(a -> isManagedAttachmentReadable(a.getStoragePath()))
                .map(a -> new XmlFileAttachmentDto(a.getId(), a.getOriginalFileName(), a.getFileSize(), contentTypeValue(a.getOriginalFileName()), a.getCreatedAt()))
                .toList();
    }

    /**
     * A(z) {@code xmlFileAttachmentContent} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param xmlFileId az érintett XML-fájl adatbázis-azonosítója
     * @param attachmentId a cél csatolmány azonosítója
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    @GetMapping("/xml-files/{xmlFileId}/attachments/{attachmentId}/content")
    @Operation(summary = "Űrlapállomány csatolmányának megnyitása")
    public ResponseEntity<byte[]> xmlFileAttachmentContent(@PathVariable Long xmlFileId, @PathVariable UUID attachmentId) throws IOException {
        var attachment = attachmentRepository.findForXmlFile(attachmentId, xmlFileId).orElseThrow();
        MediaType type = resolveMediaType(attachment.getOriginalFileName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(safeName(attachment.getOriginalFileName()), StandardCharsets.UTF_8)
                        .build().toString())
                .contentType(type).body(storagePathPolicy.readXmlFileAttachment(xmlFileId, attachmentId, attachment.getStoragePath()));
    }

    /**
     * A fájlnév/kiterjesztés alapján biztonságos HTTP Content-Type értéket választ letöltéshez.
     *
     * @param originalFileName a művelethez átadott {@code originalFileName} érték
     * @return a művelet eredménye
     */
    private MediaType resolveMediaType(String originalFileName) {
        return MediaTypeFactory.getMediaType(originalFileName == null ? "" : originalFileName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    /**
     * A fájlhoz tartozó médiatípust HTTP headerben használható szöveggé alakítja.
     *
     * @param originalFileName a művelethez átadott {@code originalFileName} érték
     * @return a művelet eredménye
     */
    private String contentTypeValue(String originalFileName) {
        return resolveMediaType(originalFileName).toString();
    }

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param storagePath a művelethez átadott {@code storagePath} érték
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private boolean isManagedAttachmentReadable(String storagePath) {
        return storagePathPolicy.isReadableFile(storagePath);
    }

    /**
     * A NAV M2M submitter modul {@code XmlFileAttachmentDto} típusának felelősségét megvalósító típus.
     */
    /**
     * Létrehozza a(z) {@code XmlFileAttachmentDto} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param id a művelethez átadott {@code id} érték
     * @param fileName a művelethez átadott {@code fileName} érték
     * @param fileSize a művelethez átadott {@code fileSize} érték
     * @param contentType a művelethez átadott {@code contentType} érték
     * @param createdAt a művelethez átadott {@code createdAt} érték
     */
    public record XmlFileAttachmentDto(UUID id, String fileName, Long fileSize, String contentType, java.time.Instant createdAt) {}

    /**
     * A beküldéshez tartozó letölthető fájlokból ZIP választ készít.
     *
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    @GetMapping(value = "/{id}/files/download", produces = "application/zip")
    @Operation(summary = "Beküldéshez tartozó bizonylat és csatolmányok letöltése ZIP-ben")
    public ResponseEntity<byte[]> downloadFiles(@PathVariable("id") UUID id) throws IOException {
        byte[] zip = buildFilesZip(List.of(id));
        return zipResponse(zip, "nav-m2m-files-" + id + ".zip");
    }

    /**
     * A kliens által kijelölt, jogosultan elérhető fájlokból ZIP választ készít.
     *
     * @param ids a kliens által kijelölt azonosítók
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    @PostMapping(value = "/files/download", produces = "application/zip")
    @Operation(summary = "Több beküldéshez tartozó bizonylatok és csatolmányok letöltése ZIP-ben")
    public ResponseEntity<byte[]> downloadSelectedFiles(@RequestBody List<UUID> ids) throws IOException {
        byte[] zip = buildFilesZip(ids == null ? List.of() : ids);
        return zipResponse(zip, "nav-m2m-selected-files.zip");
    }

    /**
     * A(z) {@code zipResponse} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param zip az épülő ZIP kimenet
     * @param fileName a művelethez átadott {@code fileName} érték
     * @return a művelet eredménye
     */
    private ResponseEntity<byte[]> zipResponse(byte[] zip, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    /**
     * A kijelölt, hozzáférés-ellenőrzött fájlokat ZIP archívumba csomagolja letöltéshez.
     *
     * @param ids a kliens által kijelölt azonosítók
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private byte[] buildFilesZip(List<UUID> ids) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (UUID id : ids) {
                var submissionOpt = RepositoryAccess.findById(submissionRepository, id);
                if (submissionOpt.isEmpty()) continue;
                var submission = submissionOpt.get();
                String folder = safeName(submission.getXmlFileName() == null ? id.toString() : stripExtension(submission.getXmlFileName())) + "_" + id + "/";
                if (submission.getXmlStoragePath() != null) {
                    addPathToZip(zip, Path.of(submission.getXmlStoragePath()), folder + "01_bizonylat_" + safeName(submission.getXmlFileName()));
                }
                var attachments = attachmentRepository.findBySubmissionIdOrderByCreatedAtAsc(id);
                int index = 1;
                for (var attachment : attachments) {
                    if (attachment.getStoragePath() == null) continue;
                    addPathToZip(zip, Path.of(attachment.getStoragePath()), folder + "attachments/" + String.format("%02d_", index++) + safeName(attachment.getOriginalFileName()));
                }
            }
        }
        return out.toByteArray();
    }

    /**
     * Egy ellenőrzött fájlt ZIP bejegyzésként hozzáad az épülő archívumhoz.
     *
     * @param zip az épülő ZIP kimenet
     * @param path a feldolgozandó vagy ellenőrzendő fájlútvonal
     * @param entryName a ZIP bejegyzés neve
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void addPathToZip(ZipOutputStream zip, Path path, String entryName) throws IOException {
        if (!ExceptionSafeOperations.fileExists(path) || !ExceptionSafeOperations.isRegularFile(path)) return;
        ZipEntry entry = new ZipEntry(entryName);
        zip.putNextEntry(entry);
        Files.copy(path, zip);
        zip.closeEntry();
    }

    /**
     * Felhasználói vagy fájlrendszeri eredetű fájlnevet ZIP-bejegyzésként biztonságosan használható névre alakít.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String safeName(String value) {
        if (value == null || value.isBlank()) return "file.bin";
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFC);
        String safe = normalized.replaceAll("[\\\\/\\p{Cntrl}]", "_");
        return safe.equals(".") || safe.equals("..") || safe.isBlank() ? "file.bin" : safe;
    }

    /**
     * Eltávolítja a fájlnév utolsó kiterjesztését, ha van.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String stripExtension(String value) {
        if (value == null) return "submission";
        int idx = value.lastIndexOf('.');
        return idx > 0 ? value.substring(0, idx) : value;
    }

}
