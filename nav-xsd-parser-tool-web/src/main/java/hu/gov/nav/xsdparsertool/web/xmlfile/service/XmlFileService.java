package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.partner.service.PartnerService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.service.XmlAccessPolicyService;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.AutoRegisterServerFilesResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.FileNameAvailabilityResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.CopyXmlFileRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlFileDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlHeaderInfo;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlResourceResolutionInfo;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlSchemaVersionCompatibility;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlResolverInfoDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlSaveRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileLockEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileSessionEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileLockRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileSessionRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRevisionRepository;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.service.StreamingXsdValidationService;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.entity.XsdValidationRequestEntity;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.repository.XsdValidationRequestRepository;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code XmlFileService} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class XmlFileService {
    private static final Logger log = LoggerFactory.getLogger(XmlFileService.class);
    private static final ThreadLocal<String> ACTOR_OVERRIDE = new ThreadLocal<>();

    private final XmlFileRepository repository;
    private final XmlFileLockRepository lockRepository;
    private final XmlFileSessionRepository sessionRepository;
    private final XmlFileRevisionRepository revisionRepository;
    private final XsdValidationRequestRepository xsdValidationRequestRepository;
    private final XmlFileStorageProperties properties;
    private final XmlHeaderDetectionService headerDetectionService;
    private final XmlResourceResolutionService resourceResolutionService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final StreamingXsdValidationService streamingXsdValidationService;
    private final JdbcTemplate jdbcTemplate;
    private final LargeXmlMultiformPageService largeXmlMultiformPageService;
    private final PartnerService partnerService;
    private final XmlAccessPolicyService xmlAccessPolicyService;
    private final XmlMutationGuard mutationGuard;

    /**
     * Létrehozza a {@code XmlFileService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param repository a művelet bemeneti {@code repository} értéke
     * @param lockRepository a művelet bemeneti {@code lockRepository} értéke
     * @param sessionRepository a művelet bemeneti {@code sessionRepository} értéke
     * @param revisionRepository a művelet bemeneti {@code revisionRepository} értéke
     * @param xsdValidationRequestRepository a művelet bemeneti kérésadatait tartalmazó objektum
     * @param properties a művelethez szükséges konfigurációs adatok
     * @param headerDetectionService a művelet bemeneti {@code headerDetectionService} értéke
     * @param resourceResolutionService a művelet bemeneti {@code resourceResolutionService} értéke
     * @param currentUserService a művelet felhasználói kontextusa vagy felhasználóneve
     * @param auditLogService a művelet bemeneti {@code auditLogService} értéke
     * @param streamingXsdValidationService a művelet bemeneti {@code streamingXsdValidationService} értéke
     * @param jdbcTemplate a művelet bemeneti {@code jdbcTemplate} értéke
     * @param largeXmlMultiformPageService a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param partnerService a művelet bemeneti {@code partnerService} értéke
     * @param xmlAccessPolicyService a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param mutationGuard a művelet bemeneti {@code mutationGuard} értéke
     */
    public XmlFileService(XmlFileRepository repository,
                          XmlFileLockRepository lockRepository,
                          XmlFileSessionRepository sessionRepository,
                          XmlFileRevisionRepository revisionRepository,
                          XsdValidationRequestRepository xsdValidationRequestRepository,
                          XmlFileStorageProperties properties,
                          XmlHeaderDetectionService headerDetectionService,
                          XmlResourceResolutionService resourceResolutionService,
                          CurrentUserService currentUserService,
                          AuditLogService auditLogService,
                          StreamingXsdValidationService streamingXsdValidationService,
                          JdbcTemplate jdbcTemplate,
                          LargeXmlMultiformPageService largeXmlMultiformPageService,
                          PartnerService partnerService,
                          XmlAccessPolicyService xmlAccessPolicyService,
                          XmlMutationGuard mutationGuard) {
        this.repository = repository;
        this.lockRepository = lockRepository;
        this.sessionRepository = sessionRepository;
        this.revisionRepository = revisionRepository;
        this.xsdValidationRequestRepository = xsdValidationRequestRepository;
        this.properties = properties;
        this.headerDetectionService = headerDetectionService;
        this.resourceResolutionService = resourceResolutionService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.streamingXsdValidationService = streamingXsdValidationService;
        this.jdbcTemplate = jdbcTemplate;
        this.largeXmlMultiformPageService = largeXmlMultiformPageService;
        this.partnerService = partnerService;
        this.xmlAccessPolicyService = xmlAccessPolicyService;
        this.mutationGuard = mutationGuard;
    }

    /**
     * A {@code list} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param archived a művelet bemeneti {@code archived} értéke
     * @return a művelet eredményeként előállított elemek listája
     */
    @Transactional(readOnly = true)
    public List<XmlFileDto> list(boolean archived) {
        List<XmlFileEntity> entities = archived
                ? repository.findByArchivedTrueOrderByCreatedAtDesc()
                : repository.findByArchivedFalseOrderByCreatedAtDesc();
        entities = xmlAccessPolicyService.filterCurrentUser(entities);
        return entities.stream().map(entity -> {
            XmlFileLockEntity activeLock = activeLockFor(entity.getId());
            XsdValidationRequestEntity latestXsdRequest = latestXsdRequestFor(entity.getId());
            return XmlFileDto.from(entity, activeLock, activeSessionIdFor(entity.getId(), activeLock), revisionRepository.countByXmlFileId(entity.getId()), latestXsdRequest);
        }).toList();
    }


    /**
     * A {@code latestXsdRequestFor} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    private XsdValidationRequestEntity latestXsdRequestFor(Long xmlFileId) {
        if (xmlFileId == null) {
            return null;
        }
        return xsdValidationRequestRepository.findFirstByXmlFileIdOrderByCreatedAtDesc(xmlFileId).orElse(null);
    }

    /**
     * A {@code activeSessionIdFor} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param activeLock a művelet bemeneti {@code activeLock} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String activeSessionIdFor(Long xmlFileId, XmlFileLockEntity activeLock) {
        if (xmlFileId == null || activeLock == null) {
            return null;
        }
        return sessionRepository.findByXmlFileIdAndActiveTrue(xmlFileId).stream()
                .filter(session -> !Boolean.TRUE.equals(session.getReadOnly()))
                .filter(session -> activeLock.getLockToken() == null || activeLock.getLockToken().equals(session.getLockToken()))
                .filter(session -> activeLock.getLockBrowserSessionId() == null || activeLock.getLockBrowserSessionId().equals(session.getBrowserSessionId()))
                .map(XmlFileSessionEntity::getSessionId)
                .findFirst()
                .orElse(null);
    }

    /**
     * A {@code activeLockFor} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    private XmlFileLockEntity activeLockFor(Long xmlFileId) {
        if (xmlFileId == null) {
            return null;
        }
        XmlFileLockEntity lock = lockRepository.findByXmlFileIdAndStatus(xmlFileId, "ACTIVE").orElse(null);
        if (lock == null || lock.getLockExpiresAt() == null || lock.getLockExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }
        return lock;
    }

    /**
     * A {@code upload} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @param userNote a művelet felhasználói kontextusa vagy felhasználóneve
     * @param partnerId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional
    public XmlFileDto upload(MultipartFile file, String userNote, Long partnerId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Nincs feltöltött XML állomány.");
        }
        String originalName = requireSafeXmlFileName(file.getOriginalFilename());
        ensureUniqueFileName(originalName);
        Path uploadDir = normalize(Path.of(properties.getUploadDir()));
        ExceptionSafeOperations.createDirectories(uploadDir);
        Path target = normalize(uploadDir.resolve(UUID.randomUUID() + ".xml"));
        ensureInsideRoot(uploadDir, target, "A feltöltési célkönyvtár érvénytelen.");
        try (InputStream in = file.getInputStream();
             java.io.OutputStream out = SecureFileOperations.newPrivateOutputStream(target)) {
            in.transferTo(out);
        }
        XmlFileEntity entity = createEntity(originalName, originalName, target, userNote, "UPLOAD");
        if (partnerId == null) {
            throw new IllegalArgumentException("XML feltöltésekor a partner megadása kötelező.");
        }
        entity.setPartner(partnerService.require(partnerId));
        entity.setPartnerImportStatus("ASSIGNED");
        entity.setPartnerImportMessage(null);
        XmlFileEntity saved = repository.save(entity);
        auditLogService.log("XML_FILE_UPLOADED", saved.getId(), null, null, currentUsername(), "SUCCESS",
                "XML állomány feltöltve: " + saved.getFileName(), null);
        startAutomaticXsdValidationIfPossible(saved, "XML feltöltés utáni automatikus XSD validáció előkészítése.");
        return XmlFileDto.from(saved);
    }

    /**
     * A {@code registerServerFile} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param pathValue a feldolgozásban részt vevő fájl vagy elérési út
     * @param userNote a művelet felhasználói kontextusa vagy felhasználóneve
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional
    public XmlFileDto registerServerFile(String pathValue, String userNote) throws IOException {
        if (!properties.getServerBrowser().isEnabled()) {
            throw new IllegalStateException("A szerver oldali XML tallózás nincs engedélyezve.");
        }
        if (pathValue == null || pathValue.isBlank()) {
            throw new IllegalArgumentException("Nincs megadva szerver oldali XML útvonal.");
        }
        Path configuredRoot = normalize(Path.of(properties.getServerImport().getRootDir()));
        if (!ExceptionSafeOperations.isDirectory(configuredRoot)) {
            throw new IllegalArgumentException("A konfigurált szerver oldali root könyvtár nem létezik: " + configuredRoot);
        }
        Path root = configuredRoot.toRealPath();
        Path file = resolveServerBrowserSelection(root, pathValue);
        String fileName = requireSafeXmlFileName(file.getFileName().toString());
        ensureUniqueFileName(fileName);
        XmlFileEntity entity = createEntity(fileName, fileName, file, userNote, "SERVER_FILE");
        XmlFileEntity saved = repository.save(entity);
        auditLogService.log("XML_FILE_REGISTERED", saved.getId(), null, null, currentUsername(), "SUCCESS",
                "Szerver oldali XML regisztrálva: " + saved.getFileName(), null);
        startAutomaticXsdValidationIfPossible(saved, "XML regisztráció utáni automatikus XSD validáció előkészítése.");
        return XmlFileDto.from(saved);
    }


    /**
     * A {@code resolveServerBrowserSelection} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param trustedRoot a művelet bemeneti {@code trustedRoot} értéke
     * @param requestedPath a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a feloldott vagy lekért érték
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private Path resolveServerBrowserSelection(Path trustedRoot, String requestedPath) throws IOException {
        String requested = Path.of(requestedPath).toAbsolutePath().normalize().toString();
        try (java.util.stream.Stream<Path> candidates = Files.walk(trustedRoot)) {
            return candidates
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        try { return path.toRealPath(); } catch (IOException ex) { return null; }
                    })
                    .filter(java.util.Objects::nonNull)
                    .filter(path -> path.startsWith(trustedRoot))
                    .filter(path -> path.toString().equals(requested))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("A kiválasztott XML nem található a konfigurált szerver oldali root könyvtár alatt."));
        }
    }


    /**
     * A {@code autoRegisterServerFiles} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    public AutoRegisterServerFilesResponse autoRegisterServerFiles() throws IOException {
        return autoRegisterServerFiles(null);
    }

    /**
     * A {@code autoRegisterServerFiles} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param actorUsername a művelet felhasználói kontextusa vagy felhasználóneve
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    public AutoRegisterServerFilesResponse autoRegisterServerFiles(String actorUsername) throws IOException {
        if (actorUsername != null && !actorUsername.isBlank()) {
            ACTOR_OVERRIDE.set(actorUsername);
        }
        try {
            return doAutoRegisterServerFiles();
        } finally {
            if (actorUsername != null && !actorUsername.isBlank()) {
                ACTOR_OVERRIDE.remove();
            }
        }
    }

    /**
     * A {@code doAutoRegisterServerFiles} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private AutoRegisterServerFilesResponse doAutoRegisterServerFiles() throws IOException {
        String configuredRoot = properties.getServerImport().getRootDir();
        if (configuredRoot == null || configuredRoot.isBlank()) {
            return new AutoRegisterServerFilesResponse(
                    properties.getServerBrowser().isEnabled(),
                    "",
                    0,
                    0,
                    0,
                    List.of(),
                    List.of("A szerver oldali XML root könyvtár nincs konfigurálva."));
        }
        Path root = normalize(Path.of(configuredRoot));
        if (!properties.getServerBrowser().isEnabled()) {
            return new AutoRegisterServerFilesResponse(false, root.toString(), 0, 0, 0, List.of(), List.of("A szerver oldali XML tallózás nincs engedélyezve."));
        }
        if (!ExceptionSafeOperations.isDirectory(root)) {
            return new AutoRegisterServerFilesResponse(true, root.toString(), 0, 0, 0, List.of(), List.of("A konfigurált szerver oldali root könyvtár nem létezik: " + root));
        }
        List<XmlFileDto> registered = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int scanned = 0;
        int skipped = 0;
        try (var stream = Files.list(root)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xml"))
                    .sorted((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()))
                    .toList();
            scanned = files.size();
            for (Path file : files) {
                String fileName;
                try {
                    fileName = requireSafeXmlFileName(file.getFileName().toString());
                } catch (RuntimeException ex) {
                    skipped++;
                    warnings.add(file.getFileName() + ": " + ex.getMessage());
                    continue;
                }
                String physicalPath = file.toAbsolutePath().normalize().toString();
                if (isManagedUploadStorageFile(root, file)) {
                    skipped++;
                    continue;
                }
                if (repository.existsByFilePathIgnoreCase(physicalPath)
                        || repository.existsByFileNameIgnoreCase(fileName)) {
                    skipped++;
                    continue;
                }
                try {
                    XmlFileEntity entity = createEntity(fileName, fileName, file, null, "SERVER_FILE");
                    applyPartnerSidecar(file, entity);
                    XmlFileEntity saved = repository.save(entity);
                    auditLogService.log("XML_FILE_AUTO_REGISTERED", saved.getId(), null, null, currentUsername(), "SUCCESS",
                            "Szerver oldali XML automatikusan regisztrálva: " + saved.getFileName(), null);
                    startAutomaticXsdValidationIfPossible(saved, "Automatikus szerver oldali XML regisztráció utáni XSD validáció előkészítése.");
                    registered.add(XmlFileDto.from(saved));
                } catch (RuntimeException | IOException ex) {
                    skipped++;
                    warnings.add(file.getFileName() + ": " + ex.getMessage());
                    auditLogService.log("XML_FILE_AUTO_REGISTER_FAILED", null, null, null, currentUsername(), "WARNING",
                            "Szerver oldali XML automatikus regisztrációja sikertelen: " + file.getFileName(), ex.getMessage());
                }
            }
        }
        return new AutoRegisterServerFilesResponse(true, root.toString(), scanned, registered.size(), skipped, registered, warnings);
    }


    /**
     * A {@code isManagedUploadStorageFile} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param serverRoot a művelet bemeneti {@code serverRoot} értéke
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isManagedUploadStorageFile(Path serverRoot, Path file) {
        if (serverRoot == null || file == null || file.getFileName() == null) return false;
        String uploadDirValue = properties.getUploadDir();
        if (uploadDirValue == null || uploadDirValue.isBlank()) return false;
        Path uploadRoot = normalize(Path.of(uploadDirValue));
        Path normalizedServerRoot = normalize(serverRoot);
        if (!normalizedServerRoot.equals(uploadRoot)) return false;
        String name = file.getFileName().toString();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".xml")) return false;
        String baseName = name.substring(0, name.length() - 4);
        try {
            UUID.fromString(baseName);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * A {@code applyPartnerSidecar} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param entity a művelet bemeneti {@code entity} értéke
     */
    private void applyPartnerSidecar(Path xmlFile, XmlFileEntity entity) {
        try {
            String line = readFirstNonBlankPartnerSidecarLine(xmlFile);
            if (line == null) {
                entity.setPartnerImportStatus("MISSING");
                entity.setPartnerImportMessage("Nincs partner hozzárendelve; a fájl csak adminisztrátorként nyitható meg.");
                return;
            }
            if (line.isBlank()) throw new IllegalArgumentException("A partner kísérőfájl üres.");
            String[] values = line.split(";", -1);
            if (values.length < 2) throw new IllegalArgumentException("A partner kísérőfájl formátuma: \"adószám\";\"partner neve\".");
            String taxNumber = unquote(values[0]);
            String partnerName = unquote(values[1]);
            entity.setPartner(partnerService.resolveOrCreateImportedPartner(taxNumber, partnerName));
            entity.setPartnerImportStatus("ASSIGNED");
            entity.setPartnerImportMessage(null);
        } catch (Exception ex) {
            entity.setPartnerImportStatus("ERROR");
            entity.setPartnerImportMessage("Partner kísérőfájl hiba: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
        }
    }

    /**
     * A {@code readFirstNonBlankPartnerSidecarLine} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @return a feloldott vagy lekért érték
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private String readFirstNonBlankPartnerSidecarLine(Path xmlFile) throws IOException {
        if (xmlFile == null) {
            throw new IOException("Hiányzó XML állomány útvonal.");
        }
        Path canonicalXml = xmlFile.toRealPath();
        Path parent = canonicalXml.getParent();
        if (parent == null) {
            throw new IOException("Az XML állomány könyvtára nem határozható meg.");
        }
        String fileName = canonicalXml.getFileName().toString();
        boolean xmlSuffix = fileName.length() >= 4
                && fileName.regionMatches(true, fileName.length() - 4, ".xml", 0, 4);
        String stem = xmlSuffix ? fileName.substring(0, fileName.length() - 4) : fileName;
        String[] allowedNames = {stem + ".partner", fileName + ".partner"};
        for (String allowedName : allowedNames) {
            Path sidecar = parent.resolve(allowedName).normalize();
            if (!sidecar.getParent().equals(parent) || !ExceptionSafeOperations.isRegularFile(sidecar)) {
                continue;
            }
            Path realSidecar = sidecar.toRealPath();
            if (!realSidecar.getParent().equals(parent)) {
                throw new IOException("A partner kísérőfájl nem az XML állomány könyvtárában található.");
            }
            try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(realSidecar, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) return line;
                }
                return "";
            }
        }
        return null;
    }

    /**
     * A {@code unquote} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String unquote(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) text = text.substring(1, text.length() - 1);
        return text.trim();
    }

    /**
     * A {@code requireCurrentUserAccess} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     */
    @Transactional(readOnly = true)
    public void requireCurrentUserAccess(Long id) {
        XmlFileEntity entity = RepositoryAccess.findById(repository, id).orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány: " + id));
        xmlAccessPolicyService.requireCurrentUserAccess(entity);
    }

    /**
     * A {@code hasPartner} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    @Transactional(readOnly = true)
    public boolean hasPartner(Long id) {
        return RepositoryAccess.findById(repository, id).map(entity -> entity.getPartner() != null).orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány: " + id));
    }

    /**
     * Meghatározza, hogy az XML űrlapverziója megegyezik-e a ténylegesen feloldott XSD főverziójával.
     * Eltérés esetén a fájl csak olvasható kompatibilitási módban nyitható meg.
     *
     * @param id az XML állomány azonosítója
     * @return verzió-kompatibilitási eredmény
     */
    @Transactional(readOnly = true)
    public XmlSchemaVersionCompatibility resolveSchemaVersionCompatibility(Long id) {
        XmlFileEntity entity = RepositoryAccess.findById(repository, id)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány: " + id));
        if (entity.getFilePath() == null || entity.getFilePath().isBlank()) {
            return new XmlSchemaVersionCompatibility(entity.getFormVersion(), null, false, null);
        }
        Path file = normalize(Path.of(entity.getFilePath()));
        if (!ExceptionSafeOperations.isRegularFile(file)) {
            return new XmlSchemaVersionCompatibility(entity.getFormVersion(), null, false, null);
        }
        try {
            XmlHeaderInfo headerInfo = headerDetectionService.detect(file);
            XmlResourceResolutionInfo resolutionInfo = resourceResolutionService.resolve(headerInfo);
            if (!resolutionInfo.schemaVersionFallback()) {
                return new XmlSchemaVersionCompatibility(headerInfo.formVersion(), resolutionInfo.resolvedSchemaVersion(), false, null);
            }
            String message = "Az XML űrlapverziója " + headerInfo.formVersion()
                    + ", de ehhez nem található pontos XSD. A feloldott XSD verziója " + resolutionInfo.resolvedSchemaVersion()
                    + ". Az űrlap ezért kompatibilitási módban, csak olvashatóan nyílik meg.";
            return new XmlSchemaVersionCompatibility(headerInfo.formVersion(), resolutionInfo.resolvedSchemaVersion(), true, message);
        } catch (Exception ex) {
            log.warn("XML/XSD verzió-kompatibilitás ellenőrzése sikertelen. xmlFileId={} message={}", id, ex.getMessage());
            return new XmlSchemaVersionCompatibility(entity.getFormVersion(), null, false, null);
        }
    }

    /**
     * A {@code updateNote} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param userNote a művelet felhasználói kontextusa vagy felhasználóneve
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public XmlFileDto updateNote(Long id, String userNote) {
        XmlFileEntity entity = RepositoryAccess.findById(repository, id)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány ezzel az azonosítóval: " + id));
        String normalizedNote = blankToNull(userNote);
        if (normalizedNote != null && normalizedNote.length() > 1000) {
            throw new IllegalArgumentException("A megjegyzés legfeljebb 1000 karakter lehet.");
        }
        entity.setUserNote(normalizedNote);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUsername());
        XmlFileEntity saved = repository.save(entity);
        auditLogService.log("XML_FILE_NOTE_UPDATED", saved.getId(), null, null, currentUsername(), "SUCCESS",
                "XML állomány megjegyzése módosítva: " + saved.getFileName(), null);
        return XmlFileDto.from(saved);
    }

    /**
     * A {@code downloadPath} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @Transactional(readOnly = true)
    public Path downloadPath(Long id) {
        XmlFileEntity entity = RepositoryAccess.findById(repository, id)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány ezzel az azonosítóval: " + id));
        Path path = normalize(Path.of(entity.getFilePath()));
        if (!ExceptionSafeOperations.isRegularFile(path)) {
            throw new IllegalStateException("A letöltendő XML állomány nem található a fájlrendszerben: " + path);
        }
        return path;
    }



    /**
     * A {@code updatePartner} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param partnerId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public XmlFileDto updatePartner(Long id, Long partnerId) {
        XmlFileEntity entity = RepositoryAccess.findById(repository, id).orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány: " + id));
        String previous = entity.getPartner() == null ? "-" : entity.getPartner().getTaxNumber() + " - " + entity.getPartner().getName();
        entity.setPartner(partnerService.require(partnerId));
        entity.setPartnerImportStatus("ASSIGNED");
        entity.setPartnerImportMessage(null);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUsername());
        XmlFileEntity saved = repository.save(entity);
        String current = saved.getPartner() == null ? "-" : saved.getPartner().getTaxNumber() + " - " + saved.getPartner().getName();
        auditLogService.log("XML_FILE_PARTNER_CHANGED", saved.getId(), null, null, currentUsername(), "SUCCESS", "XML partner módosítva", "previous=" + previous + "; current=" + current);
        return XmlFileDto.from(saved);
    }

    /**
     * A {@code saveAs} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param sourceId a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional
    public XmlFileDto saveAs(Long sourceId, XmlSaveRequest request) throws IOException {
        mutationGuard.requireMutable(sourceId);
        XmlFileEntity source = RepositoryAccess.findById(repository, sourceId).orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány: " + sourceId));
        String targetName = requireSafeXmlFileName(request == null ? null : request.newFileName());
        ensureUniqueFileName(targetName);
        String xmlContent = request == null ? null : request.xmlContent();
        if (xmlContent == null || xmlContent.isBlank()) throw new IllegalArgumentException("Nincs menthető XML tartalom.");
        if (xmlContent.length() > 157_286_400) throw new IllegalArgumentException("Az XML tartalom túl nagy.");
        Path uploadDir = normalize(Path.of(properties.getUploadDir()));
        ExceptionSafeOperations.createDirectories(uploadDir);
        Path target = normalize(uploadDir.resolve(UUID.randomUUID() + ".xml"));
        ensureInsideRoot(uploadDir, target, "A mentési célkönyvtár érvénytelen.");
        if (ExceptionSafeOperations.fileExists(target)) throw new DuplicateXmlFileNameException("Már létezik ilyen nevű XML állomány a fájlrendszerben: " + targetName);
        SecureFileOperations.writePrivateString(target, xmlContent, StandardCharsets.UTF_8);
        XmlFileEntity created = createEntity(targetName, targetName, target, request.userNote(), "SAVE_AS");
        created.setPartner(source.getPartner());
        XmlFileEntity saved = repository.save(created);
        auditLogService.log("XML_FILE_SAVE_AS", saved.getId(), null, null, currentUsername(), "SUCCESS", "XML mentés másként: " + source.getFileName() + " -> " + targetName, "sourceXmlFileId=" + sourceId);
        startAutomaticXsdValidationIfPossible(saved, "Mentés másként utáni automatikus XSD validáció előkészítése.");
        return XmlFileDto.from(saved);
    }

    /**
     * A {@code checkFileName} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param fileName a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    @Transactional(readOnly = true)
    public FileNameAvailabilityResponse checkFileName(String fileName) {
        try {
            String safeName = requireSafeXmlFileName(fileName);
            boolean available = !repository.existsByFileNameIgnoreCase(safeName);
            return new FileNameAvailabilityResponse(available, safeName,
                    available ? "A fájlnév használható." : "Már létezik ilyen nevű XML állomány.");
        } catch (RuntimeException ex) {
            return new FileNameAvailabilityResponse(false, fileName, ex.getMessage());
        }
    }

    /**
     * A {@code copy} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional
    public XmlFileDto copy(Long id, CopyXmlFileRequest request) throws IOException {
        XmlFileEntity sourceEntity = RepositoryAccess.findById(repository, id)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány ezzel az azonosítóval: " + id));
        Path source = normalize(Path.of(sourceEntity.getFilePath()));
        if (!ExceptionSafeOperations.isRegularFile(source)) {
            throw new IllegalStateException("A másolandó XML állomány nem található a fájlrendszerben: " + source);
        }
        String targetName = requireSafeXmlFileName(request == null ? null : request.fileName());
        ensureUniqueFileName(targetName);
        Path uploadDir = normalize(Path.of(properties.getUploadDir()));
        ExceptionSafeOperations.createDirectories(uploadDir);
        // The request controls only the logical/display filename. The physical path is
        // generated solely by the server and therefore cannot contain request path data.
        Path target = normalize(uploadDir.resolve(UUID.randomUUID() + ".xml"));
        ensureInsideRoot(uploadDir, target, "A másolási célkönyvtár érvénytelen.");
        if (ExceptionSafeOperations.fileExists(target)) {
            throw new DuplicateXmlFileNameException("Már létezik ilyen nevű XML állomány a fájlrendszerben: " + targetName);
        }
        SecureFileOperations.copyPrivate(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        XmlFileEntity copied = createEntity(targetName, sourceEntity.getOriginalFileName() == null ? sourceEntity.getFileName() : sourceEntity.getOriginalFileName(),
                target, request == null ? null : request.userNote(), "COPY");
        XmlFileEntity saved = repository.save(copied);
        auditLogService.log("XML_FILE_COPIED", saved.getId(), null, null, currentUsername(), "SUCCESS",
                "XML állomány másolva: " + sourceEntity.getFileName() + " -> " + saved.getFileName(),
                "sourceXmlFileId=" + sourceEntity.getId());
        startAutomaticXsdValidationIfPossible(saved, "XML másolás utáni automatikus XSD validáció előkészítése.");
        return XmlFileDto.from(saved);
    }

    /**
     * A {@code physicalArchive} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param reason a művelet bemeneti {@code reason} értéke
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional
    public XmlFileDto physicalArchive(Long id, String reason) throws IOException {
        XmlFileEntity entity = RepositoryAccess.findById(repository, id)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány ezzel az azonosítóval: " + id));
        Path source = normalize(Path.of(entity.getFilePath()));
        if (!ExceptionSafeOperations.isRegularFile(source)) {
            throw new IllegalStateException("A fizikai archiváláshoz nem található az XML fájl: " + source);
        }
        Path archiveDir = normalize(Path.of(properties.getArchiveDir()));
        ExceptionSafeOperations.createDirectories(archiveDir);
        Path target = uniqueArchiveTarget(archiveDir, source.getFileName().toString());
        SecureFileOperations.movePrivate(source, target, StandardCopyOption.REPLACE_EXISTING);

        entity.setFilePath(target.toString());
        entity.setArchived(Boolean.TRUE);
        entity.setArchivedAt(LocalDateTime.now());
        entity.setArchivedBy(currentUsername());
        entity.setStatus("ARCHIVED");
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUsername());
        XmlFileEntity saved = repository.save(entity);
        auditLogService.log("XML_FILE_PHYSICAL_ARCHIVED", saved.getId(), null, null, currentUsername(), "SUCCESS",
                "XML állomány fizikailag archív mappába mozgatva: " + saved.getFileName(),
                "source=" + source + "; target=" + target + (reason == null || reason.isBlank() ? "" : "; reason=" + reason));
        return XmlFileDto.from(saved);
    }

    /**
     * A {@code uniqueArchiveTarget} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param archiveDir a művelet bemeneti {@code archiveDir} értéke
     * @param fileName a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    private Path uniqueArchiveTarget(Path archiveDir, String fileName) {
        String safeName = requireSafeXmlFileName(fileName);
        Path target = archiveDir.resolve(safeName);
        if (!ExceptionSafeOperations.fileExists(target)) {
            return target;
        }
        String base = safeName.substring(0, safeName.length() - 4);
        String stamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int counter = 1;
        do {
            target = archiveDir.resolve(base + "_archived_" + stamp + "_" + counter + ".xml");
            counter++;
        } while (ExceptionSafeOperations.fileExists(target));
        return target;
    }


    /**
     * A {@code permanentlyDelete} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param reason a művelet bemeneti {@code reason} értéke
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional
    public void permanentlyDelete(Long id, String reason) throws IOException {
        XmlFileEntity entity = RepositoryAccess.findById(repository, id)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány ezzel az azonosítóval: " + id));
        if (activeLockFor(id) != null || !sessionRepository.findByXmlFileIdAndActiveTrue(id).isEmpty()) {
            throw new IllegalStateException("Az állomány aktív munkamenetben van. A végleges törlés előtt zárd le a munkamenetet.");
        }
        Path source = normalize(Path.of(entity.getFilePath()));
        String actor = currentUsername();
        String fileName = entity.getFileName();

        // A tárolt revision backup útvonalakat csak szöveges policy-ellenőrzéshez olvassuk vissza.
        // Fontos: ezekből az értékekből nem készül Path/File és nem kerülnek fájlrendszer API-ba.
        // A tényleges törlés továbbra is kizárólag alkalmazás által számított kontrollált gyökerekből történik.
        validateStoredRevisionBackupPathStrings(id, source);

        // Az auditbejegyzést megőrizzük, de a törlendő rekord FK-kapcsolatát leválasztjuk.
        auditLogService.log("XML_FILE_PERMANENT_DELETE", id, null, null, actor, "SUCCESS",
                "XML állomány végleges törlése: " + fileName, reason);

        jdbcTemplate.update("delete from xml_file_diff_entry where xml_file_id = ?", id);
        jdbcTemplate.update("delete from xml_file_revision where xml_file_id = ?", id);
        jdbcTemplate.update("delete from xsd_validation_error where xml_file_id = ?", id);
        jdbcTemplate.update("delete from xsd_validation_request where xml_file_id = ?", id);
        jdbcTemplate.update("delete from xml_file_lock_release_request where xml_file_id = ?", id);
        jdbcTemplate.update("delete from xml_file_lock where xml_file_id = ?", id);
        jdbcTemplate.update("delete from xml_file_session where xml_file_id = ?", id);
        jdbcTemplate.update("delete from processing_job where xml_file_id = ?", id);
        jdbcTemplate.update("update operation_audit_log set xml_file_id = null where xml_file_id = ?", id);
        repository.delete(entity);
        repository.flush();

        largeXmlMultiformPageService.removeAllForFile(id, source);
        Files.deleteIfExists(source);
        Files.deleteIfExists(source.resolveSibling(source.getFileName() + ".fragment-save.tmp"));
        // A revision rekordok útvonalát nem használjuk fájlrendszer-sink bemeneteként.
        // A revision backupok kizárólag a kontrollált backup könyvtárból, illetve a
        // kompatibilitási, forrásfájl melletti név-prefix alapján kerülnek törlésre.
        deleteRelatedBackups(id, source);
    }


    /**
     * A {@code validateStoredRevisionBackupPathStrings} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param source a művelet bemeneti {@code source} értéke
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void validateStoredRevisionBackupPathStrings(Long xmlFileId, Path source) throws IOException {
        List<String> storedPaths = jdbcTemplate.queryForList(
                "select backup_file_path from xml_file_revision where xml_file_id = ? and backup_file_path is not null",
                String.class, xmlFileId);
        if (storedPaths == null || storedPaths.isEmpty()) return;

        String configuredRoot = policyPathString(normalize(Path.of(properties.getBackupDir())).resolve(String.valueOf(xmlFileId)).toString());
        String configuredPrefix = configuredRoot.endsWith("/") ? configuredRoot : configuredRoot + "/";
        String sourceParent = source == null || source.getParent() == null ? null : policyPathString(source.getParent().toString());
        String legacyPrefix = source == null || source.getFileName() == null ? null : source.getFileName().toString() + ".backup-";

        for (String storedPath : storedPaths) {
            if (storedPath == null || storedPath.isBlank()) continue;
            String candidate = policyPathString(storedPath);
            boolean configured = pathStartsWith(candidate, configuredPrefix);
            boolean legacy = false;
            if (!configured && sourceParent != null && legacyPrefix != null) {
                int slash = candidate.lastIndexOf('/');
                String parent = slash < 0 ? "" : candidate.substring(0, slash);
                String name = slash < 0 ? candidate : candidate.substring(slash + 1);
                legacy = pathEquals(parent, sourceParent) && name.startsWith(legacyPrefix);
            }
            if (!configured && !legacy) {
                throw new IOException("A revision backup útvonala kívül esik az engedélyezett backup területen.");
            }
        }
    }

    /**
     * A {@code policyPathString} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param raw a művelet bemeneti {@code raw} értéke
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private static String policyPathString(String raw) throws IOException {
        if (raw == null || raw.indexOf('\0') >= 0) throw new IOException("Érvénytelen tárolt backup útvonal.");
        String value = raw.replace('\\', '/');
        while (value.contains("//")) value = value.replace("//", "/");
        String[] parts = value.split("/");
        for (String part : parts) {
            if (".".equals(part) || "..".equals(part)) throw new IOException("Érvénytelen tárolt backup útvonal.");
        }
        while (value.length() > 1 && value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    /**
     * A {@code pathStartsWith} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param candidate a művelet bemeneti {@code candidate} értéke
     * @param trustedPrefix a művelet bemeneti {@code trustedPrefix} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private static boolean pathStartsWith(String candidate, String trustedPrefix) {
        if (candidate == null || trustedPrefix == null || candidate.length() < trustedPrefix.length()) return false;
        if (java.io.File.separatorChar == '\\') {
            return candidate.regionMatches(true, 0, trustedPrefix, 0, trustedPrefix.length());
        }
        return candidate.startsWith(trustedPrefix);
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
        if (value == null || suffix == null || value.length() < suffix.length()) return false;
        int start = value.length() - suffix.length();
        return value.regionMatches(true, start, suffix, 0, suffix.length());
    }

    /**
     * A {@code pathEquals} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param left a művelet bemeneti {@code left} értéke
     * @param right a művelet bemeneti {@code right} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private static boolean pathEquals(String left, String right) {
        return java.io.File.separatorChar == '\\' ? left.equalsIgnoreCase(right) : left.equals(right);
    }

    /*
     * Revision backup deletion intentionally does not consume stored revision path values from the
     * database. Backups are deleted only from the configured per-file backup directory and from
     * the legacy sibling namespace derived from the already-authorized XML source path. This
     * keeps stored path metadata out of all filesystem APIs.
     */

    /**
     * A {@code deleteRelatedBackups} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param source a művelet bemeneti {@code source} értéke
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void deleteRelatedBackups(Long xmlFileId, Path source) throws IOException {
        Path configuredDirectory = normalize(Path.of(properties.getBackupDir())).resolve(String.valueOf(xmlFileId));
        deleteDirectoryRecursively(configuredDirectory);

        // Kompatibilitási takarítás a korábbi, upload melletti backup fájlokra.
        if (source == null || source.getParent() == null || source.getFileName() == null) return;
        String prefix = source.getFileName().toString() + ".backup-";
        try (var paths = Files.list(source.getParent())) {
            for (Path candidate : paths.filter(path -> path.getFileName().toString().startsWith(prefix)).toList()) {
                Files.deleteIfExists(candidate);
            }
        }
    }

    /**
     * A {@code deleteDirectoryRecursively} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param directory a művelet bemeneti {@code directory} értéke
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (directory == null || !ExceptionSafeOperations.fileExists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path candidate : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(candidate);
            }
        }
    }

    /**
     * A {@code archive} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param reason a művelet bemeneti {@code reason} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public XmlFileDto archive(Long id, String reason) {
        XmlFileEntity entity = RepositoryAccess.findById(repository, id)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány ezzel az azonosítóval: " + id));
        entity.setArchived(Boolean.TRUE);
        entity.setArchivedAt(LocalDateTime.now());
        entity.setArchivedBy(currentUsername());
        entity.setStatus("ARCHIVED");
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUsername());
        XmlFileEntity saved = repository.save(entity);
        auditLogService.log("XML_FILE_ARCHIVED", saved.getId(), null, null, currentUsername(), "SUCCESS",
                "XML állomány archiválva: " + saved.getFileName(), reason);
        return XmlFileDto.from(saved);
    }


    /**
     * A {@code startAutomaticXsdValidationIfPossible} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param entity a művelet bemeneti {@code entity} értéke
     * @param initialMessage a művelet bemeneti {@code initialMessage} értéke
     */
    private void startAutomaticXsdValidationIfPossible(XmlFileEntity entity, String initialMessage) {
        if (entity == null || entity.getId() == null || entity.getXsdPath() == null || entity.getXsdPath().isBlank()) {
            return;
        }
        Runnable starter = () -> startAutomaticXsdValidationNow(entity.getId(), initialMessage);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                /**
                 * A {@code afterCommit} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
                 *
                 * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
                 */
                @Override
                public void afterCommit() {
                    starter.run();
                }
            });
        } else {
            starter.run();
        }
    }

    /**
     * A {@code startAutomaticXsdValidationNow} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param initialMessage a művelet bemeneti {@code initialMessage} értéke
     */
    private void startAutomaticXsdValidationNow(Long xmlFileId, String initialMessage) {
        try {
            streamingXsdValidationService.startValidationForXmlFile(xmlFileId, null, initialMessage);
        } catch (RuntimeException ex) {
            auditLogService.log("XSD_VALIDATION_AUTOSTART_SKIPPED", xmlFileId, null, null, currentUsername(), "WARNING",
                    "Automatikus XSD validáció nem indult el: " + ex.getMessage(), null);
        }
    }

    /**
     * A {@code refreshResolutionIfNeeded} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional
    public XmlFileEntity refreshResolutionIfNeeded(Long id) throws IOException {
        XmlFileEntity entity = RepositoryAccess.findById(repository, id)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány: " + id));
        if (Boolean.TRUE.equals(entity.getArchived())) {
            return entity;
        }
        String storedResolution = entity.getResolutionStatus();
        if ("READY".equalsIgnoreCase(entity.getStatus())
                && (storedResolution == null || storedResolution.isBlank() || "RESOLVED".equalsIgnoreCase(storedResolution))
                && entity.getXsdPath() != null && !entity.getXsdPath().isBlank()) {
            return entity;
        }
        if (entity.getFilePath() == null || entity.getFilePath().isBlank()) {
            throw new IllegalStateException("Az XML állományhoz nincs eltárolt fájlútvonal.");
        }
        Path file = normalize(Path.of(entity.getFilePath()));
        if (!ExceptionSafeOperations.isRegularFile(file)) {
            throw new IllegalStateException("Az XML állomány nem található: " + file);
        }

        log.info("[RESOLUTION-RETRY] START xmlFileId={} fileName={} previousStatus={} previousResolutionStatus={}",
                entity.getId(), entity.getFileName(), entity.getStatus(), entity.getResolutionStatus());
        XmlHeaderInfo headerInfo = headerDetectionService.detect(file);
        XmlResourceResolutionInfo resolutionInfo = resourceResolutionService.resolve(headerInfo);
        entity.setRootElement(headerInfo.rootElement());
        entity.setNamespaceUri(headerInfo.namespaceUri());
        entity.setSchemaLocation(headerInfo.schemaLocation());
        entity.setNoNamespaceSchemaLocation(headerInfo.noNamespaceSchemaLocation());
        entity.setFormType(firstNonBlank(resolutionInfo.documentType(), headerInfo.formType()));
        entity.setFormVersion(firstNonBlank(resolutionInfo.documentVersion(), headerInfo.formVersion()));
        entity.setXsdPath(resolutionInfo.xsdPath());
        entity.setUiModelPath(resolutionInfo.uiModelPath());
        entity.setXpathRulesPath(resolutionInfo.xpathRulesPath());
        entity.setResolutionStatus(resolutionInfo.status());
        entity.setResolutionMessage(blankToNull(firstNonBlank(resolutionInfo.message(), headerInfo.errorMessage())));
        entity.setStatus(determineStatus(headerInfo, resolutionInfo));
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUsername());
        XmlFileEntity saved = repository.save(entity);

        boolean resolvedNow = "READY".equalsIgnoreCase(saved.getStatus())
                && "RESOLVED".equalsIgnoreCase(saved.getResolutionStatus())
                && saved.getXsdPath() != null && !saved.getXsdPath().isBlank();
        auditLogService.log(resolvedNow ? "XML_RESOLUTION_RETRY_RESOLVED" : "XML_RESOLUTION_RETRY_UNRESOLVED",
                saved.getId(), null, null, currentUsername(), resolvedNow ? "SUCCESS" : "WARNING",
                resolvedNow
                        ? "Korábban fel nem oldott XML erőforrásai sikeresen feloldva: " + saved.getFileName()
                        : "Az XML erőforrás-feloldása ismét sikertelen: " + saved.getFileName(),
                "resolutionStatus=" + saved.getResolutionStatus() + "; xsdPath=" + saved.getXsdPath());
        log.info("[RESOLUTION-RETRY] END xmlFileId={} status={} resolutionStatus={} xsdPath={}",
                saved.getId(), saved.getStatus(), saved.getResolutionStatus(), saved.getXsdPath());
        if (resolvedNow) {
            startAutomaticXsdValidationIfPossible(saved, "Újrafeloldás utáni automatikus XSD validáció előkészítése.");
        }
        return saved;
    }

    /**
     * A {@code resolveInfo} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @return a feloldott vagy lekért érték
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Transactional(readOnly = true)
    public XmlResolverInfoDto resolveInfo(Long id) throws IOException {
        XmlFileEntity entity = RepositoryAccess.findById(repository, id)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány: " + id));
        if (entity.getFilePath() == null || entity.getFilePath().isBlank()) {
            throw new IllegalStateException("Az XML állományhoz nincs eltárolt fájlútvonal.");
        }
        Path file = normalize(Path.of(entity.getFilePath()));
        if (!ExceptionSafeOperations.isRegularFile(file)) {
            throw new IllegalStateException("Az XML állomány nem található: " + file);
        }

        log.info("[RESOLVER-INFO-REFRESH] START xmlFileId={} fileName={} filePath={}", id, entity.getFileName(), file);
        XmlHeaderInfo headerInfo = headerDetectionService.detect(file);
        log.info("[RESOLVER-INFO-REFRESH] HEADER detected={} rootElement={} namespace={} schemaLocation={} noNamespaceSchemaLocation={} formType={} formVersion={} error={}",
                headerInfo.detected(), headerInfo.rootElement(), headerInfo.namespaceUri(), headerInfo.schemaLocation(),
                headerInfo.noNamespaceSchemaLocation(), headerInfo.formType(), headerInfo.formVersion(), headerInfo.errorMessage());
        XmlResourceResolutionInfo resolutionInfo = resourceResolutionService.resolve(headerInfo);
        String formType = firstNonBlank(resolutionInfo.documentType(), headerInfo.formType());
        String formVersion = firstNonBlank(resolutionInfo.documentVersion(), headerInfo.formVersion());
        log.info("[RESOLVER-INFO-REFRESH] RESULT formType={} formVersion={} xsdPath={} uiModelPath={} xpathRulesPath={} status={} message={}",
                formType, formVersion, resolutionInfo.xsdPath(), resolutionInfo.uiModelPath(), resolutionInfo.xpathRulesPath(),
                resolutionInfo.status(), resolutionInfo.message());
        log.info("[RESOLVER-INFO-REFRESH] STORED formType={} formVersion={} xsdPath={} uiModelPath={} xpathRulesPath={}",
                entity.getFormType(), entity.getFormVersion(), entity.getXsdPath(), entity.getUiModelPath(), entity.getXpathRulesPath());

        return new XmlResolverInfoDto(
                entity.getId(), entity.getFileName(), entity.getFilePath(),
                headerInfo.rootElement(), headerInfo.namespaceUri(), headerInfo.schemaLocation(), headerInfo.noNamespaceSchemaLocation(),
                formType, formVersion, resolutionInfo.xsdPath(), resolutionInfo.uiModelPath(), resolutionInfo.xpathRulesPath(),
                XmlResolverInfoDto.isRegularFile(resolutionInfo.xsdPath()),
                XmlResolverInfoDto.isRegularFile(resolutionInfo.uiModelPath()),
                XmlResolverInfoDto.isRegularFile(resolutionInfo.xpathRulesPath()),
                resolutionInfo.status(), blankToNull(firstNonBlank(resolutionInfo.message(), headerInfo.errorMessage())),
                entity.getFormType(), entity.getFormVersion(), entity.getXsdPath(), entity.getUiModelPath(), entity.getXpathRulesPath());
    }

    /**
     * A {@code createEntity} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param fileName a feldolgozásban részt vevő fájl vagy elérési út
     * @param originalFileName a feldolgozásban részt vevő fájl vagy elérési út
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @param userNote a művelet felhasználói kontextusa vagy felhasználóneve
     * @param sourceType a művelet bemeneti {@code sourceType} értéke
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private XmlFileEntity createEntity(String fileName, String originalFileName, Path file, String userNote, String sourceType) throws IOException {
        XmlHeaderInfo headerInfo = headerDetectionService.detect(file);
        XmlResourceResolutionInfo resolutionInfo = resourceResolutionService.resolve(headerInfo);
        long size = Files.size(file);
        LocalDateTime now = LocalDateTime.now();
        XmlFileEntity entity = new XmlFileEntity();
        entity.setFileName(fileName);
        entity.setOriginalFileName(originalFileName);
        entity.setFilePath(file.toString());
        entity.setFileSizeBytes(size);
        entity.setRootElement(headerInfo.rootElement());
        entity.setNamespaceUri(headerInfo.namespaceUri());
        entity.setSchemaLocation(headerInfo.schemaLocation());
        entity.setNoNamespaceSchemaLocation(headerInfo.noNamespaceSchemaLocation());
        entity.setFormType(firstNonBlank(resolutionInfo.documentType(), headerInfo.formType()));
        entity.setFormVersion(firstNonBlank(resolutionInfo.documentVersion(), headerInfo.formVersion()));
        entity.setXsdPath(resolutionInfo.xsdPath());
        entity.setUiModelPath(resolutionInfo.uiModelPath());
        entity.setXpathRulesPath(resolutionInfo.xpathRulesPath());
        entity.setResolutionStatus(resolutionInfo.status());
        entity.setResolutionMessage(blankToNull(firstNonBlank(resolutionInfo.message(), headerInfo.errorMessage())));
        log.info("[RESOLVER-INFO-PERSIST] resolutionStatus={} hasXsd={} hasUiModel={} hasXpathRules={}",
                entity.getResolutionStatus(), entity.getXsdPath() != null, entity.getUiModelPath() != null, entity.getXpathRulesPath() != null);
        entity.setUserNote(blankToNull(userNote));
        entity.setSourceType(sourceType);
        entity.setStatus(determineStatus(headerInfo, resolutionInfo));
        entity.setLargeFileMode(size >= properties.getLargeFile().thresholdBytes());
        entity.setArchived(Boolean.FALSE);
        entity.setCreatedAt(now);
        entity.setCreatedBy(currentUsername());
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(currentUsername());
        auditHeaderAndResolution(entity, headerInfo, resolutionInfo);
        if (Boolean.TRUE.equals(entity.getLargeFileMode())) {
            auditLogService.log("LARGE_FILE_DETECTED", null, null, null, currentUsername(), "WARNING",
                    "Nagy XML állomány felismerve: " + fileName, "sizeBytes=" + size + "; thresholdBytes=" + properties.getLargeFile().thresholdBytes());
        }
        return entity;
    }

    /**
     * A {@code determineStatus} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param headerInfo a művelet bemeneti {@code headerInfo} értéke
     * @param resolutionInfo a művelet bemeneti {@code resolutionInfo} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String determineStatus(XmlHeaderInfo headerInfo, XmlResourceResolutionInfo resolutionInfo) {
        if (headerInfo == null || !headerInfo.detected()) {
            return "UNKNOWN";
        }
        if (resolutionInfo != null && resolutionInfo.xsdResolved()) {
            return "READY";
        }
        return "REGISTERED";
    }

    /**
     * A {@code auditHeaderAndResolution} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param entity a művelet bemeneti {@code entity} értéke
     * @param headerInfo a művelet bemeneti {@code headerInfo} értéke
     * @param resolutionInfo a művelet bemeneti {@code resolutionInfo} értéke
     */
    private void auditHeaderAndResolution(XmlFileEntity entity, XmlHeaderInfo headerInfo, XmlResourceResolutionInfo resolutionInfo) {
        String username = currentUsername();
        if (headerInfo != null && headerInfo.detected()) {
            auditLogService.log("HEADER_DETECTED", null, null, null, username, "SUCCESS",
                    "XML fejléc felismerve: " + headerInfo.rootElement(),
                    "root=" + headerInfo.rootElement() + "; namespace=" + headerInfo.namespaceUri());
        } else {
            auditLogService.log("HEADER_DETECTION_FAILED", null, null, null, username, "FAILED",
                    headerInfo != null ? headerInfo.errorMessage() : "XML fejléc felismerése sikertelen.", null);
        }
        if (resolutionInfo != null && resolutionInfo.xsdResolved()) {
            auditLogService.log("XML_RESOURCES_RESOLVED", null, null, null, username, "SUCCESS",
                    "XML erőforrások feloldva: " + resolutionInfo.xsdPath(),
                    "xsd=" + resolutionInfo.xsdPath() + "; uimodel=" + resolutionInfo.uiModelPath() + "; xpath=" + resolutionInfo.xpathRulesPath());
        } else if (resolutionInfo != null) {
            auditLogService.log("XML_RESOURCES_NOT_RESOLVED", null, null, null, username, "WARNING",
                    resolutionInfo.message(), null);
        }
    }

    /**
     * A {@code ensureUniqueFileName} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param fileName a feldolgozásban részt vevő fájl vagy elérési út
     */
    private void ensureUniqueFileName(String fileName) {
        if (repository.existsByFileNameIgnoreCase(fileName)) {
            throw new DuplicateXmlFileNameException("Már létezik ilyen nevű XML állomány: " + fileName);
        }
    }

    /**
     * A {@code requireSafeXmlFileName} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String requireSafeXmlFileName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hiányzó fájlnév.");
        }
        String fileName = value.trim();
        if (fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0
                || fileName.indexOf('\0') >= 0
                || fileName.chars().anyMatch(Character::isISOControl)
                || fileName.matches(".*[<>:\"|?*].*")) {
            throw new IllegalArgumentException("Érvénytelen fájlnév: " + value);
        }
        try {
            Path segment = Path.of(fileName);
            if (segment.isAbsolute() || segment.getNameCount() != 1
                    || ".".equals(fileName) || "..".equals(fileName)
                    || !fileName.equals(segment.getFileName().toString())) {
                throw new IllegalArgumentException("Érvénytelen fájlnév: " + value);
            }
        } catch (java.nio.file.InvalidPathException ex) {
            throw new IllegalArgumentException("Érvénytelen fájlnév: " + value, ex);
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            throw new IllegalArgumentException("Csak .xml állomány regisztrálható.");
        }
        return fileName;
    }

    /**
     * A {@code ensureInsideRoot} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param root a művelet bemeneti {@code root} értéke
     * @param target a művelet bemeneti {@code target} értéke
     * @param message a művelet bemeneti {@code message} értéke
     */
    private void ensureInsideRoot(Path root, Path target, String message) {
        Path normalizedRoot = normalize(root);
        Path normalizedTarget = normalize(target);
        if (!normalizedTarget.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * A {@code normalize} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    private Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
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
     * A {@code firstNonBlank} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param first a művelet bemeneti {@code first} értéke
     * @param second a művelet bemeneti {@code second} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    /**
     * A {@code currentUsername} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a művelet feldolgozási eredménye
     */
    private String currentUsername() {
        String override = ACTOR_OVERRIDE.get();
        if (override != null && !override.isBlank()) {
            return override;
        }
        String username = currentUserService.getCurrentUsername();
        return username == null || username.isBlank() ? "system" : username;
    }

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code DuplicateXmlFileNameException} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class DuplicateXmlFileNameException extends RuntimeException {
        /**
         * Létrehozza a {@code DuplicateXmlFileNameException} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param message a művelet bemeneti {@code message} értéke
         */
        public DuplicateXmlFileNameException(String message) {
            super(message);
        }
    }
}
