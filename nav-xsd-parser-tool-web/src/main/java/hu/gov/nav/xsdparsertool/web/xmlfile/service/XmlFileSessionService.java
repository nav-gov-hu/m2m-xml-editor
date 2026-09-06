package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileLockProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.OpenXmlFileResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.RenewLockResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlFileDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileLockReleaseRequestRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileLockReleaseRequestEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlSessionStateDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.LockReleaseRequestDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileLockEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileSessionEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileLockRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileSessionRepository;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code XmlFileSessionService} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class XmlFileSessionService {
    private final XmlFileRepository xmlFileRepository;
    private final XmlFileSessionRepository sessionRepository;
    private final XmlFileLockRepository lockRepository;
    private final XmlFileLockReleaseRequestRepository lockReleaseRequestRepository;
    private final XmlFileLockProperties lockProperties;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final XmlFileService xmlFileService;

    /**
     * Létrehozza a {@code XmlFileSessionService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param xmlFileRepository a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param sessionRepository a művelet bemeneti {@code sessionRepository} értéke
     * @param lockRepository a művelet bemeneti {@code lockRepository} értéke
     * @param lockReleaseRequestRepository a művelet bemeneti kérésadatait tartalmazó objektum
     * @param lockProperties a művelethez szükséges konfigurációs adatok
     * @param currentUserService a művelet felhasználói kontextusa vagy felhasználóneve
     * @param auditLogService a művelet bemeneti {@code auditLogService} értéke
     * @param xmlFileService a feldolgozandó XML-hez tartozó adat vagy tartalom
     */
    public XmlFileSessionService(XmlFileRepository xmlFileRepository,
                                 XmlFileSessionRepository sessionRepository,
                                 XmlFileLockRepository lockRepository,
                                 XmlFileLockReleaseRequestRepository lockReleaseRequestRepository,
                                 XmlFileLockProperties lockProperties,
                                 CurrentUserService currentUserService,
                                 AuditLogService auditLogService,
                                 XmlFileService xmlFileService) {
        this.xmlFileRepository = xmlFileRepository;
        this.sessionRepository = sessionRepository;
        this.lockRepository = lockRepository;
        this.lockReleaseRequestRepository = lockReleaseRequestRepository;
        this.lockProperties = lockProperties;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.xmlFileService = xmlFileService;
    }

    /**
     * A {@code open} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param readOnly a művelet bemeneti {@code readOnly} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public OpenXmlFileResponse open(Long xmlFileId, boolean readOnly) {
        return open(xmlFileId, readOnly, null, false, null, null);
    }

    /**
     * Megnyitja az XML állományt, és opcionálisan továbbadja a verzió-fallback miatti csak olvasható mód indoklását.
     */
    @Transactional
    public OpenXmlFileResponse open(Long xmlFileId, boolean readOnly, String readOnlyMessage,
                                    boolean schemaVersionFallback, String xmlFormVersion, String resolvedXsdVersion) {
        XmlFileEntity xmlFile = requireActiveXmlFile(xmlFileId);
        String username = currentUsername();
        ClientContext clientContext = currentClientContext();
        closeActiveSessionsForCurrentBrowser(username, clientContext, "Másik XML állomány megnyitása.");

        XmlFileLockEntity lock = null;
        boolean effectiveReadOnly = readOnly;
        String message = null;
        if (!readOnly) {
            lock = acquireOrRenewLock(xmlFile, username, clientContext);
        } else {
            message = readOnlyMessage == null || readOnlyMessage.isBlank()
                    ? "Az XML állomány csak olvasási módban lett megnyitva."
                    : readOnlyMessage;
        }

        XmlFileSessionEntity session = new XmlFileSessionEntity();
        session.setXmlFile(xmlFile);
        session.setSessionId("XMLS-" + UUID.randomUUID());
        session.setActive(Boolean.TRUE);
        session.setReadOnly(effectiveReadOnly);
        session.setLockToken(lock == null ? null : lock.getLockToken());
        session.setBrowserSessionId(clientContext.browserSessionId());
        session.setClientIp(clientContext.clientIp());
        session.setUserAgent(clientContext.userAgent());
        session.setCreatedAt(LocalDateTime.now());
        session.setCreatedBy(username);
        XmlFileSessionEntity savedSession = sessionRepository.save(session);

        auditLogService.log(effectiveReadOnly ? "XML_FILE_OPENED_READONLY" : "XML_FILE_OPENED",
                xmlFile.getId(), null, null, username, "SUCCESS",
                (effectiveReadOnly ? "XML állomány megnyitva olvasásra: " : "XML állomány megnyitva szerkesztésre: ") + xmlFile.getFileName(),
                "sessionId=" + savedSession.getSessionId() + ", browserSessionId=" + clientContext.browserSessionId() + ", clientIp=" + clientContext.clientIp());

        return new OpenXmlFileResponse(
                XmlFileDto.from(xmlFile),
                savedSession.getSessionId(),
                effectiveReadOnly,
                lock != null,
                lock == null ? null : lock.getLockedBy(),
                lock == null ? null : lock.getLockExpiresAt(),
                message,
                schemaVersionFallback,
                xmlFormVersion,
                resolvedXsdVersion
        );
    }

    /**
     * A {@code openReadOnlyBecauseLocked} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public OpenXmlFileResponse openReadOnlyBecauseLocked(Long xmlFileId) {
        return open(xmlFileId, true);
    }

    /**
     * A {@code renew} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public RenewLockResponse renew(Long xmlFileId) {
        XmlFileEntity xmlFile = requireActiveXmlFile(xmlFileId);
        String username = currentUsername();
        ClientContext clientContext = currentClientContext();
        XmlFileLockEntity lock = lockRepository.findByXmlFileIdAndStatus(xmlFile.getId(), "ACTIVE")
                .orElseThrow(() -> new IllegalStateException("Az XML állományhoz nincs aktív szerkesztési zárolás."));
        LocalDateTime now = LocalDateTime.now();
        if (!isSameLockOwner(lock, username, clientContext)) {
            throw new FileLockedByOtherUserException(lockOwnerLabel(lock), lock.getLockExpiresAt(),
                    "Az XML állomány zárolását csak a zárolást birtokló böngésző-munkamenet újíthatja meg.");
        }
        lock.setLockExpiresAt(now.plusMinutes(Math.max(1L, lockProperties.getRenewMinutes())));
        lock.setUpdatedAt(now);
        XmlFileLockEntity saved = lockRepository.save(lock);
        auditLogService.log("XML_FILE_LOCK_RENEWED", xmlFile.getId(), null, null, username, "SUCCESS",
                "XML állomány szerkesztési zárolás megújítva: " + xmlFile.getFileName(),
                "lockExpiresAt=" + saved.getLockExpiresAt() + ", browserSessionId=" + clientContext.browserSessionId() + ", clientIp=" + clientContext.clientIp());
        return new RenewLockResponse(xmlFile.getId(), saved.getLockToken(), saved.getLockedBy(), saved.getLockExpiresAt(), saved.getStatus());
    }

    /**
     * A {@code close} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param reason a művelet bemeneti {@code reason} értéke
     */
    @Transactional
    public void close(Long xmlFileId, String reason) {
        close(xmlFileId, reason, null);
    }

    /**
     * A {@code close} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param reason a művelet bemeneti {@code reason} értéke
     * @param sessionId a célobjektum vagy erőforrás azonosítója
     */
    @Transactional
    public void close(Long xmlFileId, String reason, String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            closeBySessionId(xmlFileId, reason, sessionId);
            return;
        }
        String username = currentUsername();
        LocalDateTime now = LocalDateTime.now();
        ClientContext clientContext = currentClientContext();
        List<XmlFileSessionEntity> sessions = sessionRepository.findByCreatedByAndActiveTrue(username).stream()
                .filter(session -> session.getXmlFile() != null && xmlFileId.equals(session.getXmlFile().getId()))
                .filter(session -> isSameBrowserSession(session.getBrowserSessionId(), clientContext))
                .toList();
        for (XmlFileSessionEntity session : sessions) {
            closeSessionEntity(session, username, now, reason);
        }
        releaseOwnLock(xmlFileId, username, clientContext, "Felhasználói lezárás.");
    }

    /**
     * A {@code closeBySessionId} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param reason a művelet bemeneti {@code reason} értéke
     * @param sessionId a célobjektum vagy erőforrás azonosítója
     */
    private void closeBySessionId(Long xmlFileId, String reason, String sessionId) {
        String username = currentUsername();
        LocalDateTime now = LocalDateTime.now();
        XmlFileSessionEntity session = sessionRepository.findBySessionIdAndActiveTrue(sessionId)
                .orElseThrow(() -> new IllegalStateException("Nem található aktív XML munkamenet ezzel az azonosítóval: " + sessionId));
        if (session.getXmlFile() == null || !xmlFileId.equals(session.getXmlFile().getId())) {
            throw new IllegalStateException("A munkamenet nem ehhez az XML állományhoz tartozik.");
        }
        if (!Objects.equals(username, session.getCreatedBy())) {
            throw new IllegalStateException("Csak a saját XML munkamenet zárható le.");
        }
        closeSessionEntity(session, username, now, reason);
        if (!Boolean.TRUE.equals(session.getReadOnly())) {
            releaseLockForSession(session, username, reason);
        }
    }

    /**
     * A {@code closeSessionEntity} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param session a művelet bemeneti {@code session} értéke
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param now a művelet bemeneti {@code now} értéke
     * @param reason a művelet bemeneti {@code reason} értéke
     */
    private void closeSessionEntity(XmlFileSessionEntity session, String username, LocalDateTime now, String reason) {
        session.setActive(Boolean.FALSE);
        session.setClosedAt(now);
        session.setClosedBy(username);
        session.setCloseReason(reason == null || reason.isBlank() ? "Felhasználói lezárás." : reason);
        sessionRepository.save(session);
    }

    /**
     * A {@code forceReleaseLock} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     */
    @Transactional
    public void forceReleaseLock(Long xmlFileId) {
        XmlFileEntity xmlFile = requireActiveXmlFile(xmlFileId);
        String username = currentUsername();
        ClientContext clientContext = currentClientContext();
        XmlFileLockEntity lock = lockRepository.findByXmlFileIdAndStatus(xmlFile.getId(), "ACTIVE")
                .orElseThrow(() -> new IllegalStateException("Az XML állományhoz nincs aktív szerkesztési zárolás."));
        lock.setStatus("FORCE_RELEASED");
        lock.setUpdatedAt(LocalDateTime.now());
        lockRepository.save(lock);
        closeSessionsForFile(xmlFile.getId(), "Admin kényszerített lock feloldás.");
        auditLogService.log("XML_FILE_LOCK_FORCE_RELEASED", xmlFile.getId(), null, null, username, "SUCCESS",
                "XML állomány zárolás kényszerített feloldása: " + xmlFile.getFileName(),
                "previousLockedBy=" + lock.getLockedBy());
    }


    /**
     * A {@code requestLockRelease} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public LockReleaseRequestDto requestLockRelease(Long xmlFileId, String message) {
        XmlFileEntity xmlFile = RepositoryAccess.findById(xmlFileRepository, xmlFileId)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány ezzel az azonosítóval: " + xmlFileId));
        XmlFileLockEntity lock = lockRepository.findByXmlFileIdAndStatus(xmlFile.getId(), "ACTIVE")
                .orElseThrow(() -> new IllegalStateException("Az XML állomány jelenleg nincs zárolva."));
        ClientContext clientContext = currentClientContext();
        String username = currentUsername();
        XmlFileLockReleaseRequestEntity entity = new XmlFileLockReleaseRequestEntity();
        entity.setXmlFile(xmlFile);
        entity.setRequesterUsername(username);
        entity.setRequesterBrowserSessionId(clientContext.browserSessionId());
        entity.setOwnerUsername(lock.getLockedBy());
        entity.setOwnerBrowserSessionId(lock.getLockBrowserSessionId());
        entity.setStatus("PENDING");
        entity.setMessage(message == null || message.isBlank() ? "Lezárási kérelem" : message.trim());
        entity.setRequestedAt(LocalDateTime.now());
        XmlFileLockReleaseRequestEntity saved = lockReleaseRequestRepository.save(entity);
        auditLogService.log("LOCK_RELEASE_REQUESTED", xmlFile.getId(), null, null, username, "SUCCESS",
                "XML munkamenet lezárási kérelem küldve: " + xmlFile.getFileName(),
                "owner=" + lock.getLockedBy() + "; requestId=" + saved.getId());
        return LockReleaseRequestDto.from(saved);
    }

    /**
     * A {@code pendingLockReleaseRequestsForCurrentSession} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    @Transactional(readOnly = true)
    public List<LockReleaseRequestDto> pendingLockReleaseRequestsForCurrentSession() {
        String username = currentUsername();
        ClientContext clientContext = currentClientContext();
        return lockReleaseRequestRepository
                .findByOwnerUsernameAndOwnerBrowserSessionIdAndStatusOrderByRequestedAtDesc(username, clientContext.browserSessionId(), "PENDING")
                .stream().map(LockReleaseRequestDto::from).toList();
    }

    /**
     * A {@code myLockReleaseRequests} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    @Transactional(readOnly = true)
    public List<LockReleaseRequestDto> myLockReleaseRequests() {
        String username = currentUsername();
        ClientContext clientContext = currentClientContext();
        return lockReleaseRequestRepository
                .findByRequesterUsernameAndRequesterBrowserSessionIdOrderByRequestedAtDesc(username, clientContext.browserSessionId())
                .stream().limit(20).map(LockReleaseRequestDto::from).toList();
    }

    /**
     * A {@code acceptLockReleaseRequest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public LockReleaseRequestDto acceptLockReleaseRequest(Long requestId, String message) {
        XmlFileLockReleaseRequestEntity request = RepositoryAccess.findById(lockReleaseRequestRepository, requestId)
                .orElseThrow(() -> new IllegalArgumentException("Nem található lezárási kérelem: " + requestId));
        requirePendingOwner(request);
        request.setStatus("ACCEPTED");
        request.setResponseMessage(message);
        request.setRespondedAt(LocalDateTime.now());
        request.setClosedBy(currentUsername());
        XmlFileEntity xmlFile = request.getXmlFile();
        closeSessionsForFile(xmlFile.getId(), "Lezárási kérelem elfogadva.");
        XmlFileLockEntity lock = lockRepository.findByXmlFileIdAndStatus(xmlFile.getId(), "ACTIVE").orElse(null);
        if (lock != null) {
            lock.setStatus("RELEASED");
            lock.setUpdatedAt(LocalDateTime.now());
            lockRepository.save(lock);
        }
        XmlFileLockReleaseRequestEntity saved = lockReleaseRequestRepository.save(request);
        auditLogService.log("LOCK_RELEASE_REQUEST_ACCEPTED", xmlFile.getId(), null, null, currentUsername(), "SUCCESS",
                "XML munkamenet lezárási kérelem elfogadva: " + xmlFile.getFileName(), "requestId=" + requestId);
        return LockReleaseRequestDto.from(saved);
    }

    /**
     * A {@code rejectLockReleaseRequest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public LockReleaseRequestDto rejectLockReleaseRequest(Long requestId, String message) {
        XmlFileLockReleaseRequestEntity request = RepositoryAccess.findById(lockReleaseRequestRepository, requestId)
                .orElseThrow(() -> new IllegalArgumentException("Nem található lezárási kérelem: " + requestId));
        requirePendingOwner(request);
        request.setStatus("REJECTED");
        request.setResponseMessage(message);
        request.setRespondedAt(LocalDateTime.now());
        XmlFileLockReleaseRequestEntity saved = lockReleaseRequestRepository.save(request);
        auditLogService.log("LOCK_RELEASE_REQUEST_REJECTED", request.getXmlFile().getId(), null, null, currentUsername(), "SUCCESS",
                "XML munkamenet lezárási kérelem elutasítva: " + request.getXmlFile().getFileName(), "requestId=" + requestId);
        return LockReleaseRequestDto.from(saved);
    }

    /**
     * A {@code forceCloseLockReleaseRequest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public LockReleaseRequestDto forceCloseLockReleaseRequest(Long requestId, String message) {
        XmlFileLockReleaseRequestEntity request = RepositoryAccess.findById(lockReleaseRequestRepository, requestId)
                .orElseThrow(() -> new IllegalArgumentException("Nem található lezárási kérelem: " + requestId));
        request.setStatus("FORCE_CLOSED");
        request.setResponseMessage(message);
        request.setRespondedAt(LocalDateTime.now());
        request.setClosedBy(currentUsername());
        request.setForceClosedAt(LocalDateTime.now());
        XmlFileEntity xmlFile = request.getXmlFile();
        closeSessionsForFile(xmlFile.getId(), "Admin kényszerített lezárás: " + (message == null ? "" : message));
        XmlFileLockEntity lock = lockRepository.findByXmlFileIdAndStatus(xmlFile.getId(), "ACTIVE").orElse(null);
        if (lock != null) {
            lock.setStatus("FORCE_RELEASED");
            lock.setUpdatedAt(LocalDateTime.now());
            lockRepository.save(lock);
        }
        XmlFileLockReleaseRequestEntity saved = lockReleaseRequestRepository.save(request);
        auditLogService.log("LOCK_FORCE_RELEASED", xmlFile.getId(), null, null, currentUsername(), "SUCCESS",
                "XML munkamenet admin által kényszerítve lezárva: " + xmlFile.getFileName(), "requestId=" + requestId + "; reason=" + message);
        return LockReleaseRequestDto.from(saved);
    }

    /**
     * A {@code sessionState} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param sessionId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @Transactional(readOnly = true)
    public XmlSessionStateDto sessionState(Long xmlFileId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return new XmlSessionStateDto(false, xmlFileId, null, false, null, null, "Hiányzó munkamenet azonosító.");
        }
        XmlFileSessionEntity session = sessionRepository.findBySessionIdAndActiveTrue(sessionId).orElse(null);
        if (session != null) {
            XmlFileLockEntity lock = lockRepository.findByXmlFileIdAndStatus(xmlFileId, "ACTIVE").orElse(null);
            return new XmlSessionStateDto(true, xmlFileId, sessionId, lock != null, null, null, null);
        }
        XmlFileSessionEntity closed = RepositoryAccess.findAll(sessionRepository).stream()
                .filter(item -> sessionId.equals(item.getSessionId()))
                .findFirst().orElse(null);
        if (closed == null) {
            return new XmlSessionStateDto(false, xmlFileId, sessionId, false, null, null, "A munkamenet már nem található.");
        }
        return new XmlSessionStateDto(false, xmlFileId, sessionId, false, closed.getClosedBy(), closed.getClosedAt(), closed.getCloseReason());
    }

    /**
     * A {@code requirePendingOwner} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     */
    private void requirePendingOwner(XmlFileLockReleaseRequestEntity request) {
        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("A lezárási kérelem már nem függőben van.");
        }
        ClientContext clientContext = currentClientContext();
        String username = currentUsername();
        if (!Objects.equals(username, request.getOwnerUsername()) || !Objects.equals(clientContext.browserSessionId(), request.getOwnerBrowserSessionId())) {
            throw new IllegalStateException("Csak a zárolást birtokló munkamenet válaszolhat a lezárási kérelemre.");
        }
    }

    /**
     * A {@code acquireOrRenewLock} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param clientContext a művelet bemeneti {@code clientContext} értéke
     * @return a művelet feldolgozási eredménye
     */
    private XmlFileLockEntity acquireOrRenewLock(XmlFileEntity xmlFile, String username, ClientContext clientContext) {
        LocalDateTime now = LocalDateTime.now();
        XmlFileLockEntity existing = lockRepository.findByXmlFileIdAndStatus(xmlFile.getId(), "ACTIVE").orElse(null);
        if (existing != null) {
            if (existing.getLockExpiresAt() != null && existing.getLockExpiresAt().isBefore(now)) {
                existing.setStatus("EXPIRED");
                existing.setUpdatedAt(now);
                lockRepository.save(existing);
                auditLogService.log("XML_FILE_LOCK_EXPIRED", xmlFile.getId(), null, null, username, "WARNING",
                        "Lejárt XML szerkesztési zárolás lezárva: " + xmlFile.getFileName(),
                        "previousLockedBy=" + existing.getLockedBy());
            } else if (isSameLockOwner(existing, username, clientContext)) {
                existing.setLockExpiresAt(now.plusMinutes(Math.max(1L, lockProperties.getRenewMinutes())));
                existing.setUpdatedAt(now);
                auditLogService.log("XML_FILE_LOCK_REUSED", xmlFile.getId(), null, null, username, "SUCCESS",
                        "Saját XML szerkesztési zárolás újrahasználva: " + xmlFile.getFileName(),
                        "browserSessionId=" + clientContext.browserSessionId() + ", clientIp=" + clientContext.clientIp());
                return lockRepository.save(existing);
            } else {
                throw new FileLockedByOtherUserException(lockOwnerLabel(existing), existing.getLockExpiresAt(),
                        "Az XML állományt jelenleg másik felhasználó vagy ugyanennek a felhasználónak egy másik böngésző-munkamenete szerkeszti.");
            }
        }

        XmlFileLockEntity lock = lockRepository.findByXmlFileId(xmlFile.getId()).orElseGet(XmlFileLockEntity::new);
        if (lock.getId() == null) {
            lock.setXmlFile(xmlFile);
            lock.setCreatedAt(now);
        }
        lock.setLockedBy(username);
        lock.setLockedAt(now);
        lock.setLockExpiresAt(now.plusMinutes(Math.max(1L, lockProperties.getTimeoutMinutes())));
        lock.setLockToken("LOCK-" + UUID.randomUUID());
        lock.setLockBrowserSessionId(clientContext.browserSessionId());
        lock.setLockClientIp(clientContext.clientIp());
        lock.setLockUserAgent(clientContext.userAgent());
        lock.setStatus("ACTIVE");
        lock.setUpdatedAt(now);
        XmlFileLockEntity saved = lockRepository.save(lock);
        auditLogService.log("XML_FILE_LOCK_ACQUIRED", xmlFile.getId(), null, null, username, "SUCCESS",
                "XML állomány szerkesztési zárolás létrehozva: " + xmlFile.getFileName(),
                "lockExpiresAt=" + saved.getLockExpiresAt() + ", browserSessionId=" + clientContext.browserSessionId() + ", clientIp=" + clientContext.clientIp());
        return saved;
    }

    /**
     * A {@code requireActiveXmlFile} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    private XmlFileEntity requireActiveXmlFile(Long xmlFileId) {
        XmlFileEntity xmlFile = RepositoryAccess.findById(xmlFileRepository, xmlFileId)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány ezzel az azonosítóval: " + xmlFileId));
        if (shouldRetryResolution(xmlFile)) {
            try {
                xmlFile = xmlFileService.refreshResolutionIfNeeded(xmlFileId);
            } catch (java.io.IOException ex) {
                throw new IllegalStateException("Az XML erőforrás-feloldásának ismételt ellenőrzése sikertelen: " + ex.getMessage(), ex);
            }
        }
        if (Boolean.TRUE.equals(xmlFile.getArchived()) || "ARCHIVED".equalsIgnoreCase(xmlFile.getStatus())) {
            throw new IllegalStateException("Archivált XML állomány nem nyitható meg.");
        }
        if (!isOpenable(xmlFile)) {
            throw new IllegalStateException("Az XML állomány nem nyitható meg űrlapként, mert nem jól formált vagy az erőforrás-feloldás nem sikeres. XSD tartalmi hibás, de jól formált XML javításra megnyitható.");
        }
        return xmlFile;
    }

    /**
     * A {@code shouldRetryResolution} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean shouldRetryResolution(XmlFileEntity xmlFile) {
        if (xmlFile == null || Boolean.TRUE.equals(xmlFile.getArchived())) {
            return false;
        }
        String status = xmlFile.getStatus();
        String resolutionStatus = xmlFile.getResolutionStatus();
        return "REGISTERED".equalsIgnoreCase(status)
                || (resolutionStatus != null && !resolutionStatus.isBlank() && !"RESOLVED".equalsIgnoreCase(resolutionStatus));
    }

    /**
     * A {@code isOpenable} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFile a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isOpenable(XmlFileEntity xmlFile) {
        String status = xmlFile.getStatus();
        String resolutionStatus = xmlFile.getResolutionStatus();
        boolean statusAllowsOpen = "READY".equalsIgnoreCase(status)
                || "XSD_INVALID".equalsIgnoreCase(status)
                || "INVALID".equalsIgnoreCase(status)
                || "REGISTERED".equalsIgnoreCase(status);
        boolean resourcesResolved = resolutionStatus == null
                || resolutionStatus.isBlank()
                || "RESOLVED".equalsIgnoreCase(resolutionStatus);
        return statusAllowsOpen && resourcesResolved;
    }

    /**
     * A {@code closeActiveSessionsForCurrentBrowser} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param clientContext a művelet bemeneti {@code clientContext} értéke
     * @param reason a művelet bemeneti {@code reason} értéke
     */
    private void closeActiveSessionsForCurrentBrowser(String username, ClientContext clientContext, String reason) {
        LocalDateTime now = LocalDateTime.now();
        for (XmlFileSessionEntity session : sessionRepository.findByCreatedByAndActiveTrue(username)) {
            if (!isSameBrowserSession(session.getBrowserSessionId(), clientContext)) {
                continue;
            }
            session.setActive(Boolean.FALSE);
            session.setClosedAt(now);
            session.setClosedBy(username);
            session.setCloseReason(reason);
            sessionRepository.save(session);
            if (!Boolean.TRUE.equals(session.getReadOnly())) {
                releaseOwnLock(session.getXmlFile().getId(), username, clientContext, reason);
            }
            auditLogService.log("XML_FILE_SESSION_CLOSED", session.getXmlFile().getId(), null, null, username, "SUCCESS",
                    "Aktív XML munkamenet lezárva: " + session.getXmlFile().getFileName(), reason);
        }
    }

    /**
     * A {@code closeSessionsForFile} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param reason a művelet bemeneti {@code reason} értéke
     */
    private void closeSessionsForFile(Long xmlFileId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        for (XmlFileSessionEntity session : RepositoryAccess.findAll(sessionRepository)) {
            if (session.getXmlFile() == null || !xmlFileId.equals(session.getXmlFile().getId()) || !Boolean.TRUE.equals(session.getActive())) {
                continue;
            }
            session.setActive(Boolean.FALSE);
            session.setClosedAt(now);
            session.setClosedBy(currentUsername());
            session.setCloseReason(reason);
            sessionRepository.save(session);
        }
    }

    /**
     * A {@code releaseLockForSession} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param session a művelet bemeneti {@code session} értéke
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param reason a művelet bemeneti {@code reason} értéke
     */
    private void releaseLockForSession(XmlFileSessionEntity session, String username, String reason) {
        if (session == null || session.getXmlFile() == null) {
            return;
        }
        XmlFileLockEntity lock = lockRepository.findByXmlFileIdAndStatus(session.getXmlFile().getId(), "ACTIVE").orElse(null);
        if (lock == null) {
            return;
        }
        if (!Objects.equals(username, lock.getLockedBy())) {
            return;
        }
        if (session.getLockToken() != null && !session.getLockToken().isBlank() && !Objects.equals(session.getLockToken(), lock.getLockToken())) {
            return;
        }
        lock.setStatus("RELEASED");
        lock.setUpdatedAt(LocalDateTime.now());
        lockRepository.save(lock);
        auditLogService.log("XML_FILE_LOCK_RELEASED", session.getXmlFile().getId(), null, null, username, "SUCCESS",
                "XML állomány szerkesztési zárolás feloldva munkamenet alapján.",
                "sessionId=" + session.getSessionId() + ", reason=" + reason);
    }

    /**
     * A {@code releaseOwnLock} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param clientContext a művelet bemeneti {@code clientContext} értéke
     * @param reason a művelet bemeneti {@code reason} értéke
     */
    private void releaseOwnLock(Long xmlFileId, String username, ClientContext clientContext, String reason) {
        XmlFileLockEntity lock = lockRepository.findByXmlFileIdAndStatus(xmlFileId, "ACTIVE").orElse(null);
        if (lock == null) {
            return;
        }
        if (!isSameLockOwner(lock, username, clientContext)) {
            return;
        }
        lock.setStatus("RELEASED");
        lock.setUpdatedAt(LocalDateTime.now());
        lockRepository.save(lock);
        auditLogService.log("XML_FILE_LOCK_RELEASED", xmlFileId, null, null, username, "SUCCESS",
                "XML állomány szerkesztési zárolás feloldva.", reason);
    }


    /**
     * A {@code currentClientContext} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    private ClientContext currentClientContext() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return new ClientContext("NO_HTTP_REQUEST", "unknown", "unknown");
        }
        HttpServletRequest request = attributes.getRequest();
        String browserSessionId = request.getSession(true).getId();
        String clientIp = firstHeader(request, "X-Forwarded-For");
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.substring(0, clientIp.indexOf(',')).trim();
        }
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = firstHeader(request, "X-Real-IP");
        }
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        }
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 1000) {
            userAgent = userAgent.substring(0, 1000);
        }
        return new ClientContext(browserSessionId, blankToUnknown(clientIp), blankToUnknown(userAgent));
    }

    /**
     * A {@code firstHeader} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param headerName a feloldáshoz vagy azonosításhoz használt név
     * @return a művelet feldolgozási eredménye
     */
    private String firstHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * A {@code isSameLockOwner} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param lock a művelet bemeneti {@code lock} értéke
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param clientContext a művelet bemeneti {@code clientContext} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isSameLockOwner(XmlFileLockEntity lock, String username, ClientContext clientContext) {
        if (lock == null || !Objects.equals(username, lock.getLockedBy())) {
            return false;
        }
        String lockBrowserSessionId = lock.getLockBrowserSessionId();
        if (lockBrowserSessionId == null || lockBrowserSessionId.isBlank()) {
            return false;
        }
        return Objects.equals(lockBrowserSessionId, clientContext.browserSessionId());
    }

    /**
     * A {@code isSameBrowserSession} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param storedBrowserSessionId a célobjektum vagy erőforrás azonosítója
     * @param clientContext a művelet bemeneti {@code clientContext} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isSameBrowserSession(String storedBrowserSessionId, ClientContext clientContext) {
        if (storedBrowserSessionId == null || storedBrowserSessionId.isBlank()) {
            return false;
        }
        return Objects.equals(storedBrowserSessionId, clientContext.browserSessionId());
    }

    /**
     * A {@code lockOwnerLabel} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
     * @param lock a művelet bemeneti {@code lock} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String lockOwnerLabel(XmlFileLockEntity lock) {
        if (lock == null) {
            return "unknown";
        }
        String client = lock.getLockClientIp() == null || lock.getLockClientIp().isBlank() ? "ismeretlen kliens" : lock.getLockClientIp();
        return lock.getLockedBy() + " / " + client;
    }

    /**
     * A {@code blankToUnknown} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String blankToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code ClientContext} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record ClientContext(String browserSessionId, String clientIp, String userAgent) {}

    /**
     * A {@code currentUsername} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @return a művelet feldolgozási eredménye
     */
    private String currentUsername() {
        String username = currentUserService.getCurrentUsername();
        return username == null || username.isBlank() ? "unknown" : username;
    }

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code FileLockedByOtherUserException} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class FileLockedByOtherUserException extends RuntimeException {
        private final String lockedBy;
        private final LocalDateTime lockExpiresAt;

        /**
         * Létrehozza a {@code FileLockedByOtherUserException} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param lockedBy a művelet bemeneti {@code lockedBy} értéke
         * @param lockExpiresAt a művelet bemeneti {@code lockExpiresAt} értéke
         * @param message a művelet bemeneti {@code message} értéke
         */
        public FileLockedByOtherUserException(String lockedBy, LocalDateTime lockExpiresAt, String message) {
            super(message);
            this.lockedBy = lockedBy;
            this.lockExpiresAt = lockExpiresAt;
        }

        /**
         * A {@code getLockedBy} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getLockedBy() { return lockedBy; }
        /**
         * A {@code getLockExpiresAt} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A végrehajtás során figyelembe veszi az XML-szerkesztési zárolást és a jogosultsági feltételeket; a zárolási állapot megkerülése nem része a fallback viselkedésnek.</p>
         * @return a feloldott vagy lekért érték
         */
        public LocalDateTime getLockExpiresAt() { return lockExpiresAt; }
    }
}
