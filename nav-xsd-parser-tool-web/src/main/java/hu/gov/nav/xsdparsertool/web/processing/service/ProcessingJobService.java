package hu.gov.nav.xsdparsertool.web.processing.service;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.processing.dto.ProcessingJobDto;
import hu.gov.nav.xsdparsertool.web.processing.entity.ProcessingJobEntity;
import hu.gov.nav.xsdparsertool.web.processing.entity.ProcessingJobStatus;
import hu.gov.nav.xsdparsertool.web.processing.repository.ProcessingJobRepository;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code ProcessingJobService} osztály a web modul feldolgozási job területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class ProcessingJobService {
    private static final Set<String> ACTIVE_STATUSES = Set.of(
            ProcessingJobStatus.PENDING.name(),
            ProcessingJobStatus.RUNNING.name(),
            ProcessingJobStatus.CANCEL_REQUESTED.name());

    private final ProcessingJobRepository processingJobRepository;
    private final XmlFileRepository xmlFileRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final ExecutorService demoExecutor;

    /**
     * Létrehozza a {@code ProcessingJobService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param processingJobRepository a művelet bemeneti {@code processingJobRepository} értéke
     * @param xmlFileRepository a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param currentUserService a művelet felhasználói kontextusa vagy felhasználóneve
     * @param auditLogService a művelet bemeneti {@code auditLogService} értéke
     */
    public ProcessingJobService(ProcessingJobRepository processingJobRepository,
                                XmlFileRepository xmlFileRepository,
                                CurrentUserService currentUserService,
                                AuditLogService auditLogService) {
        this.processingJobRepository = processingJobRepository;
        this.xmlFileRepository = xmlFileRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.demoExecutor = Executors.newSingleThreadExecutor(new ProcessingJobThreadFactory());
    }

    /**
     * A {@code closeAbandonedActiveJobsOnStartup} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void closeAbandonedActiveJobsOnStartup() {
        LocalDateTime now = LocalDateTime.now();
        List<ProcessingJobEntity> activeJobs = processingJobRepository.findByStatusIn(ACTIVE_STATUSES);
        for (ProcessingJobEntity job : activeJobs) {
            job.setStatus(ProcessingJobStatus.FAILED.name());
            job.setProgressMessage("Az alkalmazás újraindult, ezért a korábban futó feldolgozás lezárásra került.");
            job.setErrorMessage("Application restart while processing job was active.");
            job.setFinishedAt(now);
            job.setUpdatedAt(now);
            job.setUpdatedBy(job.getCreatedBy() == null ? "system" : job.getCreatedBy());
            processingJobRepository.save(job);
            auditLogService.log("PROCESSING_JOB_FAILED", xmlFileId(job), job.getJobId(), null, job.getCreatedBy(), "ERROR",
                    "Alkalmazásindításkor lezárt beragadt feldolgozási job: " + job.getJobType(), null);
        }
    }

    /**
     * A {@code getJob} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @return a feloldott vagy lekért érték
     */
    @Transactional(readOnly = true)
    public ProcessingJobDto getJob(String jobId) {
        return ProcessingJobDto.from(requireJob(jobId));
    }

    /**
     * A {@code getActiveJobOrNull} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    @Transactional(readOnly = true)
    public ProcessingJobDto getActiveJobOrNull() {
        return processingJobRepository.findFirstByStatusInOrderByCreatedAtAsc(ACTIVE_STATUSES)
                .map(ProcessingJobDto::from)
                .orElse(null);
    }

    /**
     * A {@code listRecentJobs} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    @Transactional(readOnly = true)
    public List<ProcessingJobDto> listRecentJobs() {
        return processingJobRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(ProcessingJobDto::from)
                .toList();
    }

    /**
     * A {@code startJob} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobType a művelet bemeneti {@code jobType} értéke
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param initialMessage a művelet bemeneti {@code initialMessage} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public synchronized ProcessingJobDto startJob(String jobType, Long xmlFileId, String initialMessage) {
        processingJobRepository.findFirstByStatusInOrderByCreatedAtAsc(ACTIVE_STATUSES)
                .ifPresent(active -> {
                    throw new ActiveProcessingJobException(active.getJobId(), active.getJobType(), active.getStatus());
                });
        String username = currentUsername();
        LocalDateTime now = LocalDateTime.now();
        ProcessingJobEntity job = new ProcessingJobEntity();
        job.setJobId("JOB-" + UUID.randomUUID());
        job.setJobType(jobType == null || jobType.isBlank() ? "UNKNOWN" : jobType);
        job.setStatus(ProcessingJobStatus.PENDING.name());
        job.setProgressPercent(0);
        job.setProgressMessage(initialMessage == null || initialMessage.isBlank() ? "Feldolgozás várakozik." : initialMessage);
        job.setCreatedAt(now);
        job.setCreatedBy(username);
        job.setUpdatedAt(now);
        job.setUpdatedBy(username);
        if (xmlFileId != null) {
            XmlFileEntity xmlFile = RepositoryAccess.findById(xmlFileRepository, xmlFileId)
                    .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány ezzel az azonosítóval: " + xmlFileId));
            job.setXmlFile(xmlFile);
        }
        ProcessingJobEntity saved = processingJobRepository.save(job);
        auditLogService.log("PROCESSING_JOB_STARTED", xmlFileId, saved.getJobId(), null, username, "SUCCESS",
                "Feldolgozási job létrehozva: " + saved.getJobType(), null);
        return ProcessingJobDto.from(saved);
    }

    /**
     * A {@code startDemoJob} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param durationSeconds a művelet bemeneti {@code durationSeconds} értéke
     * @return a művelet feldolgozási eredménye
     */
    public ProcessingJobDto startDemoJob(Long xmlFileId, int durationSeconds) {
        ProcessingJobDto dto = startJob("DEMO_PROCESSING", xmlFileId, "Teszt feldolgozás indítása.");
        demoExecutor.submit(() -> runDemoJob(dto.jobId(), durationSeconds));
        return dto;
    }

    /**
     * A {@code requestCancel} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public ProcessingJobDto requestCancel(String jobId) {
        ProcessingJobEntity job = requireJob(jobId);
        ProcessingJobStatus status = ProcessingJobStatus.valueOf(job.getStatus());
        if (status.isTerminal()) {
            return ProcessingJobDto.from(job);
        }
        LocalDateTime now = LocalDateTime.now();
        String username = currentUsername();
        job.setStatus(ProcessingJobStatus.CANCELLED.name());
        job.setRequestedCancelAt(now);
        job.setFinishedAt(now);
        job.setProgressMessage("Megszakítás kérése rögzítve. A feldolgozás lezárva megszakított állapotban.");
        job.setUpdatedAt(now);
        job.setUpdatedBy(username);
        ProcessingJobEntity saved = processingJobRepository.save(job);
        auditLogService.log("PROCESSING_JOB_CANCEL_REQUESTED", xmlFileId(saved), saved.getJobId(), null, username, "SUCCESS",
                "Feldolgozási job megszakítása kérve: " + saved.getJobType(), null);
        auditLogService.log("PROCESSING_JOB_CANCELLED", xmlFileId(saved), saved.getJobId(), null, username, "WARNING",
                "Feldolgozási job megszakítva felhasználói kérésre: " + saved.getJobType(), null);
        return ProcessingJobDto.from(saved);
    }

    /**
     * A {@code markRunning} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public ProcessingJobDto markRunning(String jobId, String message) {
        ProcessingJobEntity job = requireJob(jobId);
        if (ProcessingJobStatus.valueOf(job.getStatus()).isTerminal()) {
            return ProcessingJobDto.from(job);
        }
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(ProcessingJobStatus.RUNNING.name());
        if (job.getStartedAt() == null) {
            job.setStartedAt(now);
        }
        job.setProgressMessage(message);
        job.setUpdatedAt(now);
        job.setUpdatedBy(job.getCreatedBy());
        return ProcessingJobDto.from(processingJobRepository.save(job));
    }

    /**
     * A {@code updateProgress} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @param percent a művelet bemeneti {@code percent} értéke
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public ProcessingJobDto updateProgress(String jobId, int percent, String message) {
        ProcessingJobEntity job = requireJob(jobId);
        ProcessingJobStatus currentStatus = ProcessingJobStatus.valueOf(job.getStatus());
        if (currentStatus == ProcessingJobStatus.CANCEL_REQUESTED || currentStatus == ProcessingJobStatus.CANCELLED || currentStatus.isTerminal()) {
            return ProcessingJobDto.from(job);
        }
        job.setStatus(ProcessingJobStatus.RUNNING.name());
        if (job.getStartedAt() == null) {
            job.setStartedAt(LocalDateTime.now());
        }
        job.setProgressPercent(Math.max(0, Math.min(100, percent)));
        job.setProgressMessage(message);
        job.setUpdatedAt(LocalDateTime.now());
        job.setUpdatedBy(job.getCreatedBy());
        return ProcessingJobDto.from(processingJobRepository.save(job));
    }

    /**
     * A {@code isCancelRequested} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    @Transactional
    public boolean isCancelRequested(String jobId) {
        return processingJobRepository.findByJobId(jobId)
                .map(job -> ProcessingJobStatus.CANCEL_REQUESTED.name().equals(job.getStatus())
                        || ProcessingJobStatus.CANCELLED.name().equals(job.getStatus()))
                .orElse(false);
    }

    /**
     * A {@code finish} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public ProcessingJobDto finish(String jobId, String message) {
        ProcessingJobEntity job = requireJob(jobId);
        if (ProcessingJobStatus.valueOf(job.getStatus()) == ProcessingJobStatus.CANCELLED) {
            return ProcessingJobDto.from(job);
        }
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(ProcessingJobStatus.FINISHED.name());
        job.setProgressPercent(100);
        job.setProgressMessage(message == null || message.isBlank() ? "Feldolgozás befejezve." : message);
        job.setFinishedAt(now);
        job.setUpdatedAt(now);
        job.setUpdatedBy(job.getCreatedBy());
        ProcessingJobEntity saved = processingJobRepository.save(job);
        auditLogService.log("PROCESSING_JOB_FINISHED", xmlFileId(saved), saved.getJobId(), null, saved.getCreatedBy(), "SUCCESS",
                "Feldolgozási job befejezve: " + saved.getJobType(), null);
        return ProcessingJobDto.from(saved);
    }

    /**
     * A {@code cancel} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @param message a művelet bemeneti {@code message} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public ProcessingJobDto cancel(String jobId, String message) {
        ProcessingJobEntity job = requireJob(jobId);
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(ProcessingJobStatus.CANCELLED.name());
        job.setProgressMessage(message == null || message.isBlank() ? "Feldolgozás megszakítva." : message);
        job.setFinishedAt(now);
        job.setUpdatedAt(now);
        job.setUpdatedBy(job.getCreatedBy());
        ProcessingJobEntity saved = processingJobRepository.save(job);
        auditLogService.log("PROCESSING_JOB_CANCELLED", xmlFileId(saved), saved.getJobId(), null, saved.getCreatedBy(), "WARNING",
                "Feldolgozási job megszakítva: " + saved.getJobType(), null);
        return ProcessingJobDto.from(saved);
    }

    /**
     * A {@code fail} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @param errorMessage a művelet bemeneti {@code errorMessage} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    public ProcessingJobDto fail(String jobId, String errorMessage) {
        ProcessingJobEntity job = requireJob(jobId);
        if (ProcessingJobStatus.valueOf(job.getStatus()) == ProcessingJobStatus.CANCELLED) {
            return ProcessingJobDto.from(job);
        }
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(ProcessingJobStatus.FAILED.name());
        job.setErrorMessage(errorMessage);
        job.setProgressMessage("Feldolgozás hibával leállt.");
        job.setFinishedAt(now);
        job.setUpdatedAt(now);
        job.setUpdatedBy(job.getCreatedBy());
        ProcessingJobEntity saved = processingJobRepository.save(job);
        auditLogService.log("PROCESSING_JOB_FAILED", xmlFileId(saved), saved.getJobId(), null, saved.getCreatedBy(), "ERROR",
                errorMessage, null);
        return ProcessingJobDto.from(saved);
    }

    /**
     * A {@code runDemoJob} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @param durationSeconds a művelet bemeneti {@code durationSeconds} értéke
     */
    private void runDemoJob(String jobId, int durationSeconds) {
        try {
            markRunning(jobId, "Teszt feldolgozás fut.");
            int steps = Math.max(3, durationSeconds);
            for (int i = 1; i <= steps; i++) {
                Thread.sleep(1000L);
                if (isCancelRequested(jobId)) {
                    cancel(jobId, "Teszt feldolgozás felhasználói kérésre megszakítva.");
                    return;
                }
                int percent = (int) Math.round((i * 100.0d) / steps);
                updateProgress(jobId, percent, "Teszt feldolgozás folyamatban: " + percent + "%");
            }
            finish(jobId, "Teszt feldolgozás sikeresen befejezve.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(jobId, "A teszt feldolgozás megszakadt: " + e.getMessage());
        } catch (Exception e) {
            fail(jobId, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    /**
     * A {@code requireJob} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    private ProcessingJobEntity requireJob(String jobId) {
        return processingJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Nem található feldolgozási job: " + jobId));
    }

    /**
     * A {@code xmlFileId} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a feldolgozási job folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param job a művelet bemeneti {@code job} értéke
     * @return a művelet feldolgozási eredménye
     */
    private Long xmlFileId(ProcessingJobEntity job) {
        return job.getXmlFile() == null ? null : job.getXmlFile().getId();
    }

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
     * A web modul feldolgozási job területének közös alkalmazási típusa.
     *
     * <p>A {@code ProcessingJobThreadFactory} osztály a web modul feldolgozási job területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private static class ProcessingJobThreadFactory implements ThreadFactory {
        /**
         * A {@code newThread} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param runnable a művelet bemeneti {@code runnable} értéke
         * @return a művelet feldolgozási eredménye
         */
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "nav-processing-job-demo");
            thread.setDaemon(true);
            return thread;
        }
    }

    /**
     * A web modul feldolgozási job területének közös alkalmazási típusa.
     *
     * <p>A {@code ActiveProcessingJobException} osztály a web modul feldolgozási job területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class ActiveProcessingJobException extends RuntimeException {
        private final String jobId;
        private final String jobType;
        private final String status;

        /**
         * Létrehozza a {@code ActiveProcessingJobException} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param jobId a célobjektum vagy erőforrás azonosítója
         * @param jobType a művelet bemeneti {@code jobType} értéke
         * @param status a feldolgozás aktuális vagy beállítandó állapota
         */
        public ActiveProcessingJobException(String jobId, String jobType, String status) {
            super("Már fut aktív feldolgozási folyamat. Job: " + jobId + ", típus: " + jobType + ", státusz: " + status);
            this.jobId = jobId;
            this.jobType = jobType;
            this.status = status;
        }

        /**
         * A {@code getJobId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getJobId() { return jobId; }
        /**
         * A {@code getJobType} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getJobType() { return jobType; }
        /**
         * A {@code getStatus} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getStatus() { return status; }
    }
}
