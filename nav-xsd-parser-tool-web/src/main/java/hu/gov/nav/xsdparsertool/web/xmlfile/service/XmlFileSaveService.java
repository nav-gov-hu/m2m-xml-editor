package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hu.gov.nav.xsdparsertool.core.xml.SecureXmlParserSupport;
import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.service.XmlAccessPolicyService;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlDiffEntryDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlDiffPreviewResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlRevisionDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlSaveRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlSaveResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileDiffEntryEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileLockEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileRevisionEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileSessionEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileDiffEntryRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileLockRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRevisionRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileSessionRepository;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.service.StreamingXsdValidationService;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code XmlFileSaveService} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class XmlFileSaveService {
    private static final int MAX_INTERACTIVE_XML_SAVE_BYTES = 150 * 1024 * 1024;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final XmlFileRepository xmlFileRepository;
    private final XmlFileSessionRepository sessionRepository;
    private final XmlFileLockRepository lockRepository;
    private final XmlFileRevisionRepository revisionRepository;
    private final XmlFileDiffEntryRepository diffEntryRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final StreamingXsdValidationService xsdValidationService;
    private final XmlFileStorageProperties storageProperties;
    private final XmlAccessPolicyService xmlAccessPolicyService;
    private final XmlMutationGuard mutationGuard;

    /**
     * Létrehozza a {@code XmlFileSaveService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param xmlFileRepository a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param sessionRepository a művelet bemeneti {@code sessionRepository} értéke
     * @param lockRepository a művelet bemeneti {@code lockRepository} értéke
     * @param revisionRepository a művelet bemeneti {@code revisionRepository} értéke
     * @param diffEntryRepository a művelet bemeneti {@code diffEntryRepository} értéke
     * @param currentUserService a művelet felhasználói kontextusa vagy felhasználóneve
     * @param auditLogService a művelet bemeneti {@code auditLogService} értéke
     * @param xsdValidationService a művelet bemeneti {@code xsdValidationService} értéke
     * @param storageProperties a művelethez szükséges konfigurációs adatok
     * @param xmlAccessPolicyService a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param mutationGuard a művelet bemeneti {@code mutationGuard} értéke
     */
    public XmlFileSaveService(XmlFileRepository xmlFileRepository,
                              XmlFileSessionRepository sessionRepository,
                              XmlFileLockRepository lockRepository,
                              XmlFileRevisionRepository revisionRepository,
                              XmlFileDiffEntryRepository diffEntryRepository,
                              CurrentUserService currentUserService,
                              AuditLogService auditLogService,
                              StreamingXsdValidationService xsdValidationService,
                              XmlFileStorageProperties storageProperties,
                              XmlAccessPolicyService xmlAccessPolicyService,
                              XmlMutationGuard mutationGuard) {
        this.xmlFileRepository = xmlFileRepository;
        this.sessionRepository = sessionRepository;
        this.lockRepository = lockRepository;
        this.revisionRepository = revisionRepository;
        this.diffEntryRepository = diffEntryRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.xsdValidationService = xsdValidationService;
        this.storageProperties = storageProperties;
        this.xmlAccessPolicyService = xmlAccessPolicyService;
        this.mutationGuard = mutationGuard;
    }

    /**
     * A {@code diffPreview} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional(readOnly = true)
    public XmlDiffPreviewResponse diffPreview(Long xmlFileId, XmlSaveRequest request) throws IOException {
        XmlFileEntity xmlFile = requireXmlFile(xmlFileId);
        String xmlContent = requireXmlContent(request);
        List<XmlDiffEntryDto> entries = buildDiffDtos(readString(Path.of(xmlFile.getFilePath())), xmlContent);
        return new XmlDiffPreviewResponse(xmlFile.getId(), xmlFile.getFileName(), entries.size(), entries);
    }

    /**
     * A {@code saveNewVersion} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional
    public XmlSaveResponse saveNewVersion(Long xmlFileId, XmlSaveRequest request) throws IOException {
        mutationGuard.requireMutable(xmlFileId);
        XmlFileEntity xmlFile = requireXmlFile(xmlFileId);
        requireOwnEditableLock(xmlFileId, request);
        String xmlContent = prettyPrintXml(requireXmlContent(request));
        List<XmlDiffEntryDto> diff = buildDiffDtos(readString(Path.of(xmlFile.getFilePath())), xmlContent);
        int revisionNo = nextRevisionNo(xmlFileId);
        String previousFileName = xmlFile.getFileName();
        Path targetPath = resolveNewVersionPath(xmlFile, request, revisionNo);
        writeXmlContentBounded(targetPath, xmlContent);

        xmlFile.setFileName(targetPath.getFileName().toString());
        xmlFile.setFilePath(targetPath.toString());
        xmlFile.setFileSizeBytes(Files.size(targetPath));
        xmlFile.setUpdatedAt(LocalDateTime.now());
        xmlFile.setUpdatedBy(currentUsername());
        xmlFileRepository.save(xmlFile);

        XmlFileRevisionEntity revision = saveRevision(xmlFile, revisionNo, "NEW_VERSION", targetPath, null, request, diff);
        auditLogService.log("XML_FILE_SAVE_NEW_VERSION", xmlFile.getId(), null, null, currentUsername(), "SUCCESS",
                "XML állomány új verzióként mentve: " + previousFileName + " -> " + xmlFile.getFileName(),
                "revisionId=" + revision.getId() + "; target=" + targetPath);
        maybeStartXsdValidation(xmlFile, request);
        return new XmlSaveResponse(xmlFile.getId(), xmlFile.getFileName(), "NEW_VERSION", revision.getId(), revision.getRevisionNo(),
                targetPath.toString(), null, diff.size(), "Az XML új verzióként mentve és megnyitva.");
    }

    /**
     * A {@code overwrite} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional
    public XmlSaveResponse overwrite(Long xmlFileId, XmlSaveRequest request) throws IOException {
        mutationGuard.requireMutable(xmlFileId);
        XmlFileEntity xmlFile = requireXmlFile(xmlFileId);
        requireOwnEditableLock(xmlFileId, request);
        String xmlContent = prettyPrintXml(requireXmlContent(request));
        Path path = Path.of(xmlFile.getFilePath());
        String oldXml = readString(path);
        List<XmlDiffEntryDto> diff = buildDiffDtos(oldXml, xmlContent);
        int revisionNo = nextRevisionNo(xmlFileId);
        Path backupPath = backupPath(path, revisionNo, xmlFileId);
        ExceptionSafeOperations.createDirectories(backupPath.getParent());
        SecureFileOperations.copyPrivate(path, backupPath, StandardCopyOption.REPLACE_EXISTING);

        // Existing XML files must be updated in place. The security hardening previously
        // replaced the managed file through a newly-created owner-only temporary file.
        // On Windows this can make a normally writable managed XML effectively non-replaceable
        // when the process may write file data but may not replace the directory entry / ACL.
        // In-place truncation preserves the already established file ACL and therefore keeps
        // both the security boundary and the original desktop save semantics.
        try {
            writeExistingXmlContentBounded(path, xmlContent);
            verifyPersistedXml(path, xmlContent);
        } catch (IOException | RuntimeException saveFailure) {
            restoreBackupAfterFailedOverwrite(backupPath, path, saveFailure);
            throw saveFailure;
        }
        xmlFile.setFileSizeBytes(Files.size(path));
        xmlFile.setUpdatedAt(LocalDateTime.now());
        xmlFile.setUpdatedBy(currentUsername());
        xmlFileRepository.save(xmlFile);
        XmlFileRevisionEntity revision = saveRevision(xmlFile, revisionNo, "OVERWRITE", path, backupPath, request, diff);
        auditLogService.log("XML_FILE_OVERWRITTEN", xmlFile.getId(), null, null, currentUsername(), "SUCCESS",
                "XML állomány felülírva: " + xmlFile.getFileName(), "revisionId=" + revision.getId() + "; backup=" + backupPath);
        maybeStartXsdValidation(xmlFile, request);
        return new XmlSaveResponse(xmlFile.getId(), xmlFile.getFileName(), "OVERWRITE", revision.getId(), revision.getRevisionNo(),
                path.toString(), backupPath.toString(), diff.size(), "Az XML szerver oldalon felülírva.");
    }

    /**
     * A {@code writeXmlContentBounded} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param target a művelet bemeneti {@code target} értéke
     * @param xmlContent a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private static void writeXmlContentBounded(Path target, String xmlContent) throws IOException {
        byte[] encoded = encodeXmlContentBounded(xmlContent);
        try (java.io.OutputStream out = SecureFileOperations.newPrivateOutputStream(target)) {
            writeAll(out, encoded);
        }
    }

    /**
     * A {@code writeExistingXmlContentBounded} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param target a művelet bemeneti {@code target} értéke
     * @param xmlContent a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private static void writeExistingXmlContentBounded(Path target, String xmlContent) throws IOException {
        Path normalized = target.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized) || Files.isSymbolicLink(normalized)) {
            throw new IOException("A mentendő XML célfájl nem írható biztonságosan: " + normalized);
        }
        byte[] encoded = encodeXmlContentBounded(xmlContent);
        try (java.io.OutputStream out = Files.newOutputStream(
                normalized, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writeAll(out, encoded);
        }
    }

    /**
     * A {@code encodeXmlContentBounded} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlContent a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @return a művelet feldolgozási eredménye
     */
    private static byte[] encodeXmlContentBounded(String xmlContent) {
        if (xmlContent == null || xmlContent.length() > MAX_INTERACTIVE_XML_SAVE_BYTES) {
            throw new IllegalArgumentException("Az interaktív XML mentés maximális mérete 150 MB.");
        }
        byte[] encoded = xmlContent.getBytes(StandardCharsets.UTF_8);
        if (encoded.length > MAX_INTERACTIVE_XML_SAVE_BYTES) {
            throw new IllegalArgumentException("Az interaktív XML mentés maximális mérete 150 MB.");
        }
        return encoded;
    }

    /**
     * A {@code writeAll} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param out a művelet bemeneti {@code out} értéke
     * @param encoded a művelet bemeneti {@code encoded} értéke
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private static void writeAll(java.io.OutputStream out, byte[] encoded) throws IOException {
        int offset = 0;
        while (offset < encoded.length) {
            int length = Math.min(8192, encoded.length - offset);
            out.write(encoded, offset, length);
            offset += length;
        }
    }

    /**
     * A {@code verifyPersistedXml} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param target a művelet bemeneti {@code target} értéke
     * @param expectedXml a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private static void verifyPersistedXml(Path target, String expectedXml) throws IOException {
        String persisted = Files.readString(target, StandardCharsets.UTF_8);
        if (!Objects.equals(persisted, expectedXml)) {
            throw new IOException("Az XML fájl fizikai visszaellenőrzése sikertelen: a visszaolvasott tartalom eltér a mentendő XML-től.");
        }
    }

    /**
     * A {@code restoreBackupAfterFailedOverwrite} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param backupPath a feldolgozásban részt vevő fájl vagy elérési út
     * @param target a művelet bemeneti {@code target} értéke
     * @param originalFailure a művelet bemeneti {@code originalFailure} értéke
     */
    private static void restoreBackupAfterFailedOverwrite(Path backupPath, Path target, Throwable originalFailure) {
        try {
            Files.copy(backupPath, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException restoreFailure) {
            originalFailure.addSuppressed(restoreFailure);
        }
    }

    /**
     * A {@code revisions} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    @Transactional(readOnly = true)
    public List<XmlRevisionDto> revisions(Long xmlFileId) {
        return revisionRepository.findByXmlFileIdOrderByRevisionNoDesc(xmlFileId).stream()
                .map(revision -> XmlRevisionDto.from(revision, List.of()))
                .toList();
    }

    /**
     * A {@code revision} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param revisionId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @Transactional(readOnly = true)
    public XmlRevisionDto revision(Long revisionId) {
        XmlFileRevisionEntity revision = RepositoryAccess.findById(revisionRepository, revisionId)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML revision: " + revisionId));
        xmlAccessPolicyService.requireCurrentUserAccess(revision.getXmlFile());
        List<XmlDiffEntryDto> entries = diffEntryRepository.findByRevisionIdOrderByIdAsc(revisionId).stream()
                .map(this::toDto)
                .toList();
        return XmlRevisionDto.from(revision, entries);
    }

    /**
     * A {@code saveRevision} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param revisionNo a művelet bemeneti {@code revisionNo} értéke
     * @param saveType a művelet bemeneti {@code saveType} értéke
     * @param targetPath a feldolgozásban részt vevő fájl vagy elérési út
     * @param backupPath a feldolgozásban részt vevő fájl vagy elérési út
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param diff a feldolgozandó elemek kollekciója
     * @return a művelet feldolgozási eredménye
     */
    private XmlFileRevisionEntity saveRevision(XmlFileEntity xmlFile, int revisionNo, String saveType, Path targetPath, Path backupPath,
                                               XmlSaveRequest request, List<XmlDiffEntryDto> diff) {
        XmlFileRevisionEntity revision = new XmlFileRevisionEntity();
        revision.setXmlFile(xmlFile);
        revision.setRevisionNo(revisionNo);
        revision.setSaveType(saveType);
        revision.setTargetFilePath(targetPath == null ? null : targetPath.toString());
        revision.setBackupFilePath(backupPath == null ? null : backupPath.toString());
        revision.setChangeCount(diff.size());
        revision.setDiffSummary(diffSummary(diff));
        revision.setXsdValidationRequested(Boolean.TRUE.equals(request == null ? null : request.runXsdValidation()));
        revision.setXsdValidationStatus(Boolean.TRUE.equals(request == null ? null : request.runXsdValidation()) ? "REQUESTED" : "SKIPPED");
        revision.setUserNote(blankToNull(request == null ? null : request.userNote()));
        revision.setCreatedAt(LocalDateTime.now());
        revision.setCreatedBy(currentUsername());
        XmlFileRevisionEntity saved = revisionRepository.save(revision);
        for (XmlDiffEntryDto dto : diff) {
            XmlFileDiffEntryEntity entry = new XmlFileDiffEntryEntity();
            entry.setRevision(saved);
            entry.setXmlFile(xmlFile);
            entry.setChangeType(dto.changeType());
            entry.setXmlPath(dto.xmlPath());
            entry.setOldValue(dto.oldValue());
            entry.setNewValue(dto.newValue());
            entry.setDisplayLabel(dto.displayLabel());
            diffEntryRepository.save(entry);
        }
        auditLogService.log("XML_FILE_DIFF_CREATED", xmlFile.getId(), null, null, currentUsername(), "SUCCESS",
                "XML diff létrehozva: " + xmlFile.getFileName(), "revisionId=" + saved.getId() + "; changes=" + diff.size());
        return saved;
    }

    /**
     * A {@code requireOwnEditableLock} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     */
    private void requireOwnEditableLock(Long xmlFileId, XmlSaveRequest request) {
        String sessionId = request == null ? null : request.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            throw new AccessDeniedException("Mentéshez aktív szerkesztési munkamenet szükséges.");
        }
        XmlFileSessionEntity session = sessionRepository.findBySessionIdAndActiveTrue(sessionId)
                .orElseThrow(() -> new AccessDeniedException("Mentéshez aktív XML munkamenet szükséges."));
        if (session.getXmlFile() == null || !xmlFileId.equals(session.getXmlFile().getId())) {
            throw new AccessDeniedException("A munkamenet nem ehhez az XML állományhoz tartozik.");
        }
        if (Boolean.TRUE.equals(session.getReadOnly())) {
            throw new AccessDeniedException("Olvasási módban megnyitott XML nem menthető.");
        }
        if (!Objects.equals(currentUsername(), session.getCreatedBy())) {
            throw new AccessDeniedException("Csak a saját XML munkamenet menthető.");
        }
        XmlFileLockEntity lock = lockRepository.findByXmlFileIdAndStatus(xmlFileId, "ACTIVE")
                .orElseThrow(() -> new AccessDeniedException("Mentéshez aktív szerkesztési lock szükséges."));
        if (lock.getLockExpiresAt() == null || lock.getLockExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AccessDeniedException("A szerkesztési lock lejárt. Nyisd meg újra az XML-t szerkesztésre.");
        }
        if (!Objects.equals(lock.getLockToken(), session.getLockToken())) {
            throw new AccessDeniedException("A szerkesztési lock nem ehhez a munkamenethez tartozik.");
        }
    }

    /**
     * A {@code maybeStartXsdValidation} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     */
    private void maybeStartXsdValidation(XmlFileEntity xmlFile, XmlSaveRequest request) {
        if (!Boolean.TRUE.equals(request == null ? null : request.runXsdValidation())) {
            return;
        }
        try {
            xsdValidationService.startValidationForXmlFile(xmlFile.getId(), null, "Mentés utáni XSD validáció indítása.");
        } catch (RuntimeException ex) {
            auditLogService.log("XML_FILE_SAVE_XSD_VALIDATION_SKIPPED", xmlFile.getId(), null, null, currentUsername(), "WARNING",
                    "Mentés utáni XSD validáció nem indult el: " + ex.getMessage(), null);
        }
    }

    /**
     * A {@code nextRevisionNo} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    private int nextRevisionNo(Long xmlFileId) {
        Integer max = revisionRepository.maxRevisionNo(xmlFileId);
        return (max == null ? 0 : max) + 1;
    }

    /**
     * A {@code backupPath} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param original a művelet bemeneti {@code original} értéke
     * @param revisionNo a művelet bemeneti {@code revisionNo} értéke
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    private Path backupPath(Path original, int revisionNo, Long xmlFileId) {
        String name = original.getFileName().toString();
        FileNameParts parts = fileNameParts(name);
        String base = normalizedOriginalBase(parts.baseName());
        String timestamp = LocalDateTime.now().format(TS);
        return Path.of(storageProperties.getBackupDir()).toAbsolutePath().normalize()
                .resolve(String.valueOf(xmlFileId))
                .resolve(base + "_" + timestamp + "_v" + revisionNo + ".bak");
    }


    /**
     * A {@code resolveNewVersionPath} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param revisionNo a művelet bemeneti {@code revisionNo} értéke
     * @return a feloldott vagy lekért érték
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private Path resolveNewVersionPath(XmlFileEntity xmlFile, XmlSaveRequest request, int revisionNo) throws IOException {
        Path original = Path.of(xmlFile.getFilePath()).toAbsolutePath().normalize();
        String requestedName = request == null ? null : request.newFileName();
        if (requestedName == null || requestedName.isBlank() || requestedName.equalsIgnoreCase(xmlFile.getFileName())) {
            return nextVersionPath(original, revisionNo);
        }
        String safeName = requireSafeXmlFileName(requestedName);
        if (xmlFileRepository.existsByFileNameIgnoreCase(safeName)) {
            throw new IllegalArgumentException("Már létezik ilyen nevű XML állomány: " + safeName);
        }
        Path parent = original.getParent();
        Path target = parent.resolve(safeName).toAbsolutePath().normalize();
        if (!target.getParent().equals(parent)) {
            throw new IllegalArgumentException("Az új fájlnév nem tartalmazhat könyvtárhivatkozást.");
        }
        if (ExceptionSafeOperations.fileExists(target)) {
            throw new IllegalArgumentException("Már létezik ilyen nevű XML állomány a fájlrendszerben: " + safeName);
        }
        return target;
    }

    /**
     * A {@code requireSafeXmlFileName} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param fileName a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    private String requireSafeXmlFileName(String fileName) {
        String value = fileName == null ? "" : fileName.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Az új fájlnév megadása kötelező.");
        }
        if (value.contains("/") || value.contains("\\") || value.contains("..")) {
            throw new IllegalArgumentException("A fájlnév nem tartalmazhat elérési utat.");
        }
        if (!endsWithIgnoreCase(value, ".xml")) {
            value += ".xml";
        }
        if (!value.matches("[\\p{L}\\p{N}._() -]+\\.xml")) {
            throw new IllegalArgumentException("A fájlnév nem támogatott karaktert tartalmaz.");
        }
        return value;
    }

    /**
     * A {@code nextVersionPath} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param original a művelet bemeneti {@code original} értéke
     * @param revisionNo a művelet bemeneti {@code revisionNo} értéke
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private Path nextVersionPath(Path original, int revisionNo) throws IOException {
        String name = original.getFileName().toString();
        FileNameParts parts = fileNameParts(name);
        String base = normalizedOriginalBase(parts.baseName());
        String ext = parts.extension().isBlank() ? ".xml" : parts.extension();
        Path parent = original.getParent();
        LocalDateTime candidateTime = LocalDateTime.now();
        Path candidate = parent.resolve(versionedFileName(base, candidateTime, revisionNo, ext));
        while (ExceptionSafeOperations.fileExists(candidate)) {
            candidateTime = candidateTime.plusSeconds(1);
            candidate = parent.resolve(versionedFileName(base, candidateTime, revisionNo, ext));
        }
        return candidate;
    }

    /**
     * A {@code versionedFileName} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param base a művelet bemeneti {@code base} értéke
     * @param timestamp a művelet bemeneti {@code timestamp} értéke
     * @param revisionNo a művelet bemeneti {@code revisionNo} értéke
     * @param ext a művelet bemeneti {@code ext} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String versionedFileName(String base, LocalDateTime timestamp, int revisionNo, String ext) {
        return base + "_" + timestamp.format(TS) + "_v" + revisionNo + ext;
    }

    /**
     * A {@code fileNameParts} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param fileName a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    private FileNameParts fileNameParts(String fileName) {
        String name = fileName == null || fileName.isBlank() ? "xml" : fileName;
        int dot = endsWithIgnoreCase(name, ".xml") ? name.length() - 4 : name.lastIndexOf('.');
        if (dot <= 0) {
            return new FileNameParts(name, ".xml");
        }
        return new FileNameParts(name.substring(0, dot), name.substring(dot));
    }

    /**
     * A {@code normalizedOriginalBase} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param baseName a feloldáshoz vagy azonosításhoz használt név
     * @return a művelet feldolgozási eredménye
     */
    private String normalizedOriginalBase(String baseName) {
        String base = baseName == null || baseName.isBlank() ? "xml" : baseName;
        base = base.replaceFirst("_\\d{14}_v\\d+$", "");
        base = base.replaceFirst("_rev\\d+_\\d{14}$", "");
        base = base.replaceFirst("_rev\\d+_\\d+$", "");
        return base;
    }

    /**
     * A {@code endsWithIgnoreCase} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @param suffix a művelet bemeneti {@code suffix} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private static boolean endsWithIgnoreCase(String value, String suffix) {
        if (value == null || suffix == null || value.length() < suffix.length()) {
            return false;
        }
        int offset = value.length() - suffix.length();
        return value.regionMatches(true, offset, suffix, 0, suffix.length());
    }

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code FileNameParts} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record FileNameParts(String baseName, String extension) {
    }

    /**
     * A {@code requireXmlFile} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    private XmlFileEntity requireXmlFile(Long xmlFileId) {
        return RepositoryAccess.findById(xmlFileRepository, xmlFileId)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány: " + xmlFileId));
    }

    /**
     * A {@code requireXmlContent} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    private String requireXmlContent(XmlSaveRequest request) {
        String xmlContent = request == null ? null : request.xmlContent();
        if (xmlContent == null || xmlContent.isBlank()) {
            throw new IllegalArgumentException("Nincs menthető XML tartalom.");
        }
        return xmlContent;
    }

    /**
     * A {@code readString} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @return a feloldott vagy lekért érték
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private String readString(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }


    /**
     * A {@code prettyPrintXml} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xml a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @return a művelet feldolgozási eredménye
     */
    private String prettyPrintXml(String xml) {
        try {
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
            Document document = factory.newDocumentBuilder().parse(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            removeWhitespaceOnlyTextNodes(document);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            SecureXmlParserSupport.configureSecureTransformerFactory(transformerFactory);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return normalizePrettyPrintedXml(writer.toString());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Az XML pretty print formázása nem sikerült: " + ex.getMessage(), ex);
        }
    }


    /**
     * A {@code normalizePrettyPrintedXml} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xml a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @return a művelet feldolgozási eredménye
     */
    private String normalizePrettyPrintedXml(String xml) {
        return xml.lines()
                .filter(line -> !line.isBlank())
                .collect(java.util.stream.Collectors.joining(System.lineSeparator()))
                .trim() + System.lineSeparator();
    }

    /**
     * A {@code removeWhitespaceOnlyTextNodes} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param node a művelet bemeneti {@code node} értéke
     */
    private void removeWhitespaceOnlyTextNodes(Node node) {
        if (node == null) return;
        NodeList children = node.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().isBlank()) {
                node.removeChild(child);
            } else {
                removeWhitespaceOnlyTextNodes(child);
            }
        }
    }

    /**
     * A {@code buildDiffDtos} művelet előállítja a hívó réteg által használt reprezentációt.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param oldXml a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param newXml a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @return a művelet eredményeként előállított elemek listája
     */
    private List<XmlDiffEntryDto> buildDiffDtos(String oldXml, String newXml) {
        try {
            Map<String, String> oldMap = flattenXml(oldXml);
            Map<String, String> newMap = flattenXml(newXml);
            List<XmlDiffEntryDto> result = new ArrayList<>();
            TreeSet<String> paths = new TreeSet<>();
            paths.addAll(oldMap.keySet());
            paths.addAll(newMap.keySet());
            for (String path : paths) {
                String oldValue = oldMap.get(path);
                String newValue = newMap.get(path);
                if (Objects.equals(oldValue, newValue)) continue;
                String type = oldValue == null ? "ADDED" : newValue == null ? "REMOVED" : "CHANGED";
                result.add(new XmlDiffEntryDto(null, type, path, oldValue, newValue, labelFromPath(path)));
            }
            return result;
        } catch (RuntimeException ex) {
            return lineDiff(oldXml, newXml);
        }
    }

    /**
     * A {@code flattenXml} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xml a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    private Map<String, String> flattenXml(String xml) {
        try {
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
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Map<String, String> map = new LinkedHashMap<>();
            flattenNode(document.getDocumentElement(), "", map);
            return map;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Az XML diff nem építhető DOM alapján: " + ex.getMessage(), ex);
        }
    }

    /**
     * A {@code flattenNode} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param node a művelet bemeneti {@code node} értéke
     * @param parentPath a feldolgozásban részt vevő fájl vagy elérési út
     * @param map a művelet bemeneti {@code map} értéke
     */
    private void flattenNode(Node node, String parentPath, Map<String, String> map) {
        if (node == null || node.getNodeType() != Node.ELEMENT_NODE) return;
        String path = parentPath + "/" + node.getNodeName();
        NamedNodeMap attrs = node.getAttributes();
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                map.put(path + "/@" + attr.getNodeName(), attr.getNodeValue());
            }
        }
        NodeList children = node.getChildNodes();
        boolean hasElement = false;
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                hasElement = true;
                flattenNode(child, path, map);
            } else if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                String value = child.getTextContent();
                if (value != null && !value.isBlank()) text.append(value.trim());
            }
        }
        if (!hasElement) {
            map.put(path, text.toString());
        }
    }

    /**
     * A {@code lineDiff} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param oldXml a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param newXml a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @return a művelet eredményeként előállított elemek listája
     */
    private List<XmlDiffEntryDto> lineDiff(String oldXml, String newXml) {
        if (Objects.equals(oldXml, newXml)) return List.of();
        return List.of(new XmlDiffEntryDto(null, "CHANGED", "/", abbreviate(oldXml, 4000), abbreviate(newXml, 4000), "Teljes XML tartalom"));
    }

    /**
     * A {@code diffSummary} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param diff a feldolgozandó elemek kollekciója
     * @return a művelet feldolgozási eredménye
     */
    private String diffSummary(List<XmlDiffEntryDto> diff) {
        long added = diff.stream().filter(e -> "ADDED".equals(e.changeType())).count();
        long changed = diff.stream().filter(e -> "CHANGED".equals(e.changeType())).count();
        long removed = diff.stream().filter(e -> "REMOVED".equals(e.changeType())).count();
        return "ADDED=" + added + "; CHANGED=" + changed + "; REMOVED=" + removed;
    }

    /**
     * A {@code toDto} művelet előállítja a hívó réteg által használt reprezentációt.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param entry a művelet bemeneti {@code entry} értéke
     * @return a művelet feldolgozási eredménye
     */
    private XmlDiffEntryDto toDto(XmlFileDiffEntryEntity entry) {
        return new XmlDiffEntryDto(entry.getId(), entry.getChangeType(), entry.getXmlPath(), entry.getOldValue(), entry.getNewValue(), entry.getDisplayLabel());
    }

    /**
     * A {@code labelFromPath} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    private String labelFromPath(String path) {
        if (path == null || path.isBlank()) return "XML";
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    /**
     * A {@code abbreviate} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @param max a művelet bemeneti {@code max} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String abbreviate(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max) + "...";
    }

    /**
     * A {@code blankToNull} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * A {@code currentUsername} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a művelet feldolgozási eredménye
     */
    private String currentUsername() {
        String username = currentUserService.getCurrentUsername();
        return username == null || username.isBlank() ? "system" : username;
    }
}
