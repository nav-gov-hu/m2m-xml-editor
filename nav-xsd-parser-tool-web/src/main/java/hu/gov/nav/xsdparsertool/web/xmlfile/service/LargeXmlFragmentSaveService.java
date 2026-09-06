package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.core.xml.SecureXmlParserSupport;
import hu.gov.nav.xsdparsertool.web.processing.dto.ProcessingJobDto;
import hu.gov.nav.xsdparsertool.web.processing.service.ProcessingJobService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.LargeXmlFragmentSaveRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileLockEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileSessionEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileLockRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code LargeXmlFragmentSaveService} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class LargeXmlFragmentSaveService {
    private static final Logger log = LoggerFactory.getLogger(LargeXmlFragmentSaveService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int COPY_BUFFER_SIZE = 1024 * 1024;

    private final XmlFileRepository xmlFileRepository;
    private final XmlFileSessionRepository sessionRepository;
    private final XmlFileLockRepository lockRepository;
    private final CurrentUserService currentUserService;
    private final ProcessingJobService processingJobService;
    private final LargeXmlMultiformPageService multiformPageService;
    private final XmlFileStorageProperties storageProperties;
    private final XmlMutationGuard mutationGuard;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "large-xml-fragment-save");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Létrehozza a {@code LargeXmlFragmentSaveService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param xmlFileRepository a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param sessionRepository a művelet bemeneti {@code sessionRepository} értéke
     * @param lockRepository a művelet bemeneti {@code lockRepository} értéke
     * @param currentUserService a művelet felhasználói kontextusa vagy felhasználóneve
     * @param processingJobService a művelet bemeneti {@code processingJobService} értéke
     * @param multiformPageService a lapozási vagy mennyiségi korlátot meghatározó érték
     * @param storageProperties a művelethez szükséges konfigurációs adatok
     * @param mutationGuard a művelet bemeneti {@code mutationGuard} értéke
     */
    public LargeXmlFragmentSaveService(XmlFileRepository xmlFileRepository,
                                       XmlFileSessionRepository sessionRepository,
                                       XmlFileLockRepository lockRepository,
                                       CurrentUserService currentUserService,
                                       ProcessingJobService processingJobService,
                                       LargeXmlMultiformPageService multiformPageService,
                                       XmlFileStorageProperties storageProperties,
                                       XmlMutationGuard mutationGuard) {
        this.xmlFileRepository = xmlFileRepository;
        this.sessionRepository = sessionRepository;
        this.lockRepository = lockRepository;
        this.currentUserService = currentUserService;
        this.processingJobService = processingJobService;
        this.multiformPageService = multiformPageService;
        this.storageProperties = storageProperties;
        this.mutationGuard = mutationGuard;
    }

    /**
     * A {@code start} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    public ProcessingJobDto start(Long xmlFileId, LargeXmlFragmentSaveRequest request) throws IOException {
        mutationGuard.requireMutable(xmlFileId);
        XmlFileEntity file = requireEditableSession(xmlFileId, request);
        validateRequest(request);
        Path source = Path.of(file.getFilePath());
        long size = Files.size(source);
        long modified = Files.getLastModifiedTime(source).toMillis();
        // A kliensben tárolt méret és módosítási idő korábbi sikeres részmentés után
        // elavulhat. A tényleges konkurens módosítást a futás végén, az itt rögzített
        // szerveroldali size/mtime értékekhez viszonyítva ellenőrizzük.
        ProcessingJobDto job = processingJobService.startJob("LARGE_XML_FRAGMENT_SAVE", xmlFileId,
                "A nagy XML melléklap mentése előkészítés alatt.");
        String username = currentUserService.getCurrentUsername();
        executor.submit(() -> run(job.jobId(), file.getId(), source, size, modified, request, username));
        return job;
    }

    /**
     * A {@code run} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param source a művelet bemeneti {@code source} értéke
     * @param originalSize a lapozási vagy mennyiségi korlátot meghatározó érték
     * @param originalModified a művelet bemeneti {@code originalModified} értéke
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     */
    private void run(String jobId, Long xmlFileId, Path source, long originalSize, long originalModified,
                     LargeXmlFragmentSaveRequest request, String username) {
        Path temp = source.resolveSibling(source.getFileName() + ".fragment-save.tmp");
        Path backupDirectory = Path.of(storageProperties.getBackupDir())
                .toAbsolutePath().normalize().resolve(String.valueOf(xmlFileId));
        Path backup = backupDirectory.resolve(source.getFileName() + ".backup-" + TS.format(LocalDateTime.now()) + ".bak");
        try {
            log.info("Nagy XML részmentés indult: jobId={}, xmlFileId={}, source={}, formPart={}, occurrence={}, originalSize={}, originalModified={}, fragmentBytes={}",
                    jobId, xmlFileId, source, request.formName(), request.occurrenceIndex(), originalSize, originalModified,
                    request.xmlFragment().getBytes(StandardCharsets.UTF_8).length);
            processingJobService.markRunning(jobId, "A módosított melléklap ellenőrzése.");
            validateFragment(request.formName(), request.xmlFragment());
            processingJobService.updateProgress(jobId, 10, "A célmelléklap pozíciójának megkeresése az eredeti XML-ben.");
            Range range = locateOccurrence(source, request.formName(), request.occurrenceIndex(), jobId, originalSize);
            log.info("Nagy XML célmelléklap megtalálva: jobId={}, rangeStart={}, rangeEnd={}, originalFragmentBytes={}",
                    jobId, range.start(), range.end(), range.end() - range.start());
            processingJobService.updateProgress(jobId, 35, "Az új XML állomány felépítése: az eredeti elejének másolása.");
            buildReplacementFile(source, temp, range, request.xmlFragment().getBytes(StandardCharsets.UTF_8), jobId, originalSize);
            processingJobService.updateProgress(jobId, 78, "Az elkészült XML jólformáltságának streaming ellenőrzése.");
            validateWholeXml(temp, jobId, Files.size(temp));
            if (Files.size(source) != originalSize || Files.getLastModifiedTime(source).toMillis() != originalModified) {
                throw new IllegalStateException("Az eredeti XML a mentés közben megváltozott. A csere nem történt meg.");
            }
            mutationGuard.requireMutable(xmlFileId);
            processingJobService.updateProgress(jobId, 92, "Biztonsági mentés készítése az eredeti XML-ről.");
            ExceptionSafeOperations.createDirectories(backupDirectory);
            SecureFileOperations.copyPrivate(source, backup, StandardCopyOption.REPLACE_EXISTING);
            log.info("Nagy XML backup elkészült: jobId={}, backup={}, backupSize={}", jobId, backup, Files.size(backup));
            processingJobService.updateProgress(jobId, 97, "Az új XML atomi cseréje folyamatban.");
            try {
                SecureFileOperations.movePrivate(temp, source, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                log.info("Nagy XML atomi csere sikeres: jobId={}, source={}", jobId, source);
            } catch (AtomicMoveNotSupportedException ex) {
                log.warn("Az atomi fájlcsere nem támogatott, normál cserére váltás: jobId={}, source={}", jobId, source);
                SecureFileOperations.movePrivate(temp, source, StandardCopyOption.REPLACE_EXISTING);
            }
            verifySavedFragment(source, request.formName(), request.occurrenceIndex(), request.xmlFragment());
            log.info("Nagy XML részmentés visszaellenőrizve: jobId={}, sourceSize={}, sourceModified={}",
                    jobId, Files.size(source), Files.getLastModifiedTime(source).toMillis());
            XmlFileEntity entity = RepositoryAccess.findById(xmlFileRepository, xmlFileId).orElseThrow();
            entity.setFileSizeBytes(Files.size(source));
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setUpdatedBy(username);
            xmlFileRepository.save(entity);
            processingJobService.updateProgress(jobId, 99, "A melléklap-index érintett rekordjának frissítése.");
            multiformPageService.refreshAfterSave(xmlFileId, request.formName(), request.occurrenceIndex(), request.xmlFragment());
            processingJobService.finish(jobId, "A(z) " + request.formName() + "[" + request.occurrenceIndex()
                    + "] melléklap mentése sikeresen befejeződött. Backup: " + backup.getFileName());
        } catch (Exception ex) {
            log.error("Nagy XML részmentés sikertelen: jobId={}, xmlFileId={}, source={}, formPart={}, occurrence={}, temp={}, backup={}",
                    jobId, xmlFileId, source, request.formName(), request.occurrenceIndex(), temp, backup, ex);
            try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            processingJobService.fail(jobId, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }

    /**
     * A {@code verifySavedFragment} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param source a művelet bemeneti {@code source} értéke
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @param occurrence a művelet bemeneti {@code occurrence} értéke
     * @param expectedFragment a művelet bemeneti {@code expectedFragment} értéke
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void verifySavedFragment(Path source, String formName, long occurrence, String expectedFragment) throws Exception {
        Range savedRange = locateOccurrenceWithoutProgress(source, formName, occurrence);
        long length = savedRange.end() - savedRange.start();
        if (length > Integer.MAX_VALUE) {
            throw new IOException("A visszaellenőrzendő XML-részlet túl nagy.");
        }
        byte[] actual = new byte[(int) length];
        try (FileChannel channel = FileChannel.open(source, StandardOpenOption.READ)) {
            channel.position(savedRange.start());
            ByteBuffer buffer = ByteBuffer.wrap(actual);
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) { }
        }
        byte[] expected = expectedFragment.getBytes(StandardCharsets.UTF_8);
        if (!java.util.Arrays.equals(actual, expected)) {
            throw new IOException("A fájlcsere után visszaolvasott melléklap eltér a mentendő XML-részlettől.");
        }
    }

    /**
     * A {@code locateOccurrenceWithoutProgress} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param source a művelet bemeneti {@code source} értéke
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @param target a művelet bemeneti {@code target} értéke
     * @return a művelet feldolgozási eredménye
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private Range locateOccurrenceWithoutProgress(Path source, String formName, long target) throws Exception {
        byte[] startToken = ("<" + formName + ">").getBytes(StandardCharsets.UTF_8);
        byte[] endToken = ("</" + formName + ">").getBytes(StandardCharsets.UTF_8);
        long occurrence = 0;
        long position = 0;
        int startMatch = 0;
        int endMatch = 0;
        long startOffset = -1;
        try (InputStream raw = Files.newInputStream(source); BufferedInputStream input = new BufferedInputStream(raw, COPY_BUFFER_SIZE)) {
            int value;
            while ((value = input.read()) >= 0) {
                byte current = (byte) value;
                position++;
                if (startOffset < 0) {
                    startMatch = advance(startToken, startMatch, current);
                    if (startMatch == startToken.length) {
                        occurrence++;
                        long candidate = position - startToken.length;
                        startMatch = 0;
                        if (occurrence == target) startOffset = candidate;
                    }
                } else {
                    endMatch = advance(endToken, endMatch, current);
                    if (endMatch == endToken.length) return new Range(startOffset, position);
                }
            }
        }
        throw new IOException("A mentett melléklap visszaellenőrzésekor nem található a célrészlet.");
    }

    /**
     * A {@code requireEditableSession} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    private XmlFileEntity requireEditableSession(Long xmlFileId, LargeXmlFragmentSaveRequest request) {
        if (request == null || request.sessionId() == null || request.sessionId().isBlank()) {
            throw new AccessDeniedException("Mentéshez aktív szerkesztési munkamenet szükséges.");
        }
        XmlFileSessionEntity session = sessionRepository.findBySessionIdAndActiveTrue(request.sessionId())
                .orElseThrow(() -> new AccessDeniedException("Mentéshez aktív XML munkamenet szükséges."));
        if (session.getXmlFile() == null || !xmlFileId.equals(session.getXmlFile().getId())) {
            throw new AccessDeniedException("A munkamenet nem ehhez az XML állományhoz tartozik.");
        }
        if (Boolean.TRUE.equals(session.getReadOnly())) throw new AccessDeniedException("Olvasási módban megnyitott XML nem menthető.");
        if (!Objects.equals(currentUserService.getCurrentUsername(), session.getCreatedBy())) {
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
        return RepositoryAccess.findById(xmlFileRepository, xmlFileId)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány: " + xmlFileId));
    }

    /**
     * A {@code validateRequest} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     */
    private void validateRequest(LargeXmlFragmentSaveRequest request) {
        if (request == null || request.formName() == null || !request.formName().matches("Form_[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Érvénytelen űrlaprésznév.");
        }
        if (request.occurrenceIndex() == null || request.occurrenceIndex() < 1) {
            throw new IllegalArgumentException("Érvénytelen melléklap-sorszám.");
        }
        if (request.xmlFragment() == null || request.xmlFragment().isBlank()) {
            throw new IllegalArgumentException("A módosított melléklap XML-részlete üres.");
        }
    }

    /**
     * A {@code validateFragment} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @param fragment a művelet bemeneti {@code fragment} értéke
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void validateFragment(String formName, String fragment) throws Exception {
        XMLInputFactory factory = secureFactory();
        try (InputStream input = new ByteArrayInputStream(fragment.getBytes(StandardCharsets.UTF_8))) {
            XMLStreamReader reader = factory.createXMLStreamReader(input);
            while (reader.hasNext() && !reader.isStartElement()) reader.next();
            if (!reader.isStartElement() || !formName.equals(reader.getLocalName())) {
                throw new IllegalArgumentException("A mentendő XML-részlet gyökéreleme nem " + formName + ".");
            }
            while (reader.hasNext()) reader.next();
            reader.close();
        }
    }

    /**
     * A {@code locateOccurrence} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param source a művelet bemeneti {@code source} értéke
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @param target a művelet bemeneti {@code target} értéke
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @param fileSize a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private Range locateOccurrence(Path source, String formName, long target, String jobId, long fileSize) throws Exception {
        byte[] startToken = ("<" + formName + ">").getBytes(StandardCharsets.UTF_8);
        byte[] endToken = ("</" + formName + ">").getBytes(StandardCharsets.UTF_8);
        long occurrence = 0;
        long position = 0;
        int startMatch = 0;
        int endMatch = 0;
        long startOffset = -1;
        long lastProgress = -1;
        try (InputStream raw = Files.newInputStream(source); BufferedInputStream input = new BufferedInputStream(raw, COPY_BUFFER_SIZE)) {
            int value;
            while ((value = input.read()) >= 0) {
                byte current = (byte) value;
                position++;
                if (startOffset < 0) {
                    startMatch = advance(startToken, startMatch, current);
                    if (startMatch == startToken.length) {
                        occurrence++;
                        long candidate = position - startToken.length;
                        startMatch = 0;
                        if (occurrence == target) startOffset = candidate;
                    }
                } else {
                    endMatch = advance(endToken, endMatch, current);
                    if (endMatch == endToken.length) return new Range(startOffset, position);
                }
                long pct = fileSize == 0 ? 0 : Math.min(34, 10 + (position * 24 / fileSize));
                if (pct != lastProgress && pct % 2 == 0) {
                    processingJobService.updateProgress(jobId, (int) pct,
                            "A célmelléklap keresése: " + occurrence + ". előfordulás feldolgozva.");
                    lastProgress = pct;
                }
                if (processingJobService.isCancelRequested(jobId)) throw new IOException("A mentést a felhasználó megszakította.");
            }
        }
        throw new IllegalArgumentException("Nem található a(z) " + formName + "[" + target + "] melléklap.");
    }

    /**
     * A {@code advance} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param token a művelet bemeneti {@code token} értéke
     * @param matched a művelet bemeneti {@code matched} értéke
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private int advance(byte[] token, int matched, byte value) {
        if (value == token[matched]) return matched + 1;
        return value == token[0] ? 1 : 0;
    }

    /**
     * A {@code buildReplacementFile} művelet előállítja a hívó réteg által használt reprezentációt.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param source a művelet bemeneti {@code source} értéke
     * @param temp a művelet bemeneti {@code temp} értéke
     * @param range a művelet bemeneti {@code range} értéke
     * @param replacement a művelet bemeneti {@code replacement} értéke
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @param sourceSize a lapozási vagy mennyiségi korlátot meghatározó érték
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void buildReplacementFile(Path source, Path temp, Range range, byte[] replacement,
                                      String jobId, long sourceSize) throws Exception {
        try (FileChannel in = FileChannel.open(source, StandardOpenOption.READ);
             FileChannel out = FileChannel.open(temp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            copyRange(in, out, 0, range.start(), jobId, sourceSize, 35, 55, "Az XML elejének másolása");
            ByteBuffer replacementBuffer = ByteBuffer.wrap(replacement);
            while (replacementBuffer.hasRemaining()) out.write(replacementBuffer);
            processingJobService.updateProgress(jobId, 56, "A módosított melléklap beillesztve. Az XML végének másolása.");
            copyRange(in, out, range.end(), sourceSize - range.end(), jobId, sourceSize, 56, 77, "Az XML végének másolása");
            out.force(true);
        }
    }

    /**
     * A {@code copyRange} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param in a művelet bemeneti {@code in} értéke
     * @param out a művelet bemeneti {@code out} értéke
     * @param start a művelet bemeneti {@code start} értéke
     * @param length a művelet bemeneti {@code length} értéke
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @param totalSize a lapozási vagy mennyiségi korlátot meghatározó érték
     * @param fromPct a művelet bemeneti {@code fromPct} értéke
     * @param toPct a művelet bemeneti {@code toPct} értéke
     * @param message a művelet bemeneti {@code message} értéke
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void copyRange(FileChannel in, FileChannel out, long start, long length, String jobId,
                           long totalSize, int fromPct, int toPct, String message) throws Exception {
        long copied = 0;
        long position = start;
        ByteBuffer buffer = ByteBuffer.allocateDirect(COPY_BUFFER_SIZE);
        while (copied < length) {
            buffer.clear();
            buffer.limit((int) Math.min(buffer.capacity(), length - copied));
            int read = in.read(buffer, position);
            if (read < 0) break;
            buffer.flip();
            while (buffer.hasRemaining()) out.write(buffer);
            copied += read;
            position += read;
            int pct = calculateProgressPercent(fromPct, toPct, copied, length);
            processingJobService.updateProgress(jobId, pct, message + ": " + pct + "%");
            if (processingJobService.isCancelRequested(jobId)) throw new IOException("A mentést a felhasználó megszakította.");
        }
    }

    /**
     * A {@code calculateProgressPercent} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param fromPct a művelet bemeneti {@code fromPct} értéke
     * @param toPct a művelet bemeneti {@code toPct} értéke
     * @param copied a művelet bemeneti {@code copied} értéke
     * @param length a művelet bemeneti {@code length} értéke
     * @return a művelet feldolgozási eredménye
     */
    private int calculateProgressPercent(int fromPct, int toPct, long copied, long length) {
        if (fromPct < 0 || toPct < fromPct || toPct > 100) {
            throw new IllegalArgumentException("Érvénytelen progress tartomány.");
        }
        if (copied < 0 || length < 0) {
            throw new IllegalArgumentException("Érvénytelen másolási méret.");
        }
        if (length == 0) {
            return toPct;
        }
        double ratio = Math.min(1.0d, copied / (double) length);
        double calculated = fromPct + ((toPct - fromPct) * ratio);
        return Math.max(fromPct, Math.min(toPct, (int) Math.floor(calculated)));
    }

    /**
     * A {@code validateWholeXml} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @param size a lapozási vagy mennyiségi korlátot meghatározó érték
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void validateWholeXml(Path file, String jobId, long size) throws Exception {
        XMLInputFactory factory = secureFactory();
        try (InputStream input = Files.newInputStream(file)) {
            XMLStreamReader reader = factory.createXMLStreamReader(input);
            long events = 0;
            while (reader.hasNext()) {
                reader.next();
                events++;
                if (events % 100000 == 0) {
                    processingJobService.updateProgress(jobId, 85,
                            "Az új XML streaming ellenőrzése folyamatban. Feldolgozott XML események: " + events);
                    if (processingJobService.isCancelRequested(jobId)) throw new IOException("A mentést a felhasználó megszakította.");
                }
            }
            reader.close();
        }
        processingJobService.updateProgress(jobId, 90, "Az új XML jól formált, a fájlcsere előkészítése.");
    }

    /**
     * A {@code secureFactory} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    private XMLInputFactory secureFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        SecureXmlParserSupport.configureSecureXmlInputFactory(factory);
        return factory;
    }

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code Range} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record Range(long start, long end) {}
}
