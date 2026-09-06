package hu.gov.nav.xsdparsertool.web.processing.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hu.gov.nav.xsdparsertool.web.api.dto.ApiErrorResponse;
import hu.gov.nav.xsdparsertool.web.processing.dto.ProcessingJobDto;
import hu.gov.nav.xsdparsertool.web.processing.dto.StartDemoJobRequest;
import hu.gov.nav.xsdparsertool.web.processing.service.ProcessingJobService;

/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code ProcessingJobController} osztály a web modul feldolgozási job területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController
@RequestMapping("/api/jobs")
public class ProcessingJobController {
    private final ProcessingJobService processingJobService;

    /**
     * Létrehozza a {@code ProcessingJobController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param processingJobService a művelet bemeneti {@code processingJobService} értéke
     */
    public ProcessingJobController(ProcessingJobService processingJobService) {
        this.processingJobService = processingJobService;
    }

    /**
     * A {@code getJob} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @return a feloldott vagy lekért érték
     */
    @GetMapping("/{jobId}")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public ProcessingJobDto getJob(@PathVariable String jobId) {
        return processingJobService.getJob(jobId);
    }


    /**
     * A {@code getJobStatus} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a feloldott vagy lekért érték
     */
    @PostMapping("/status")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public ProcessingJobDto getJobStatus(@RequestBody Map<String, String> request) {
        String jobId = request == null ? null : request.get("jobId");
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("Hiányzó feldolgozási job azonosító.");
        }
        return processingJobService.getJob(jobId);
    }

    /**
     * A {@code getActiveJob} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    @GetMapping("/active")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public ResponseEntity<?> getActiveJob() {
        ProcessingJobDto job = processingJobService.getActiveJobOrNull();
        if (job == null) {
            return ResponseEntity.ok(Map.of("active", false));
        }
        return ResponseEntity.ok(Map.of("active", true, "job", job));
    }

    /**
     * A {@code recentJobs} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    @GetMapping("/recent")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public List<ProcessingJobDto> recentJobs() {
        return processingJobService.listRecentJobs();
    }

    /**
     * A {@code cancel} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @PostMapping("/{jobId}/cancel")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public ProcessingJobDto cancel(@PathVariable String jobId) {
        return processingJobService.requestCancel(jobId);
    }

    /**
     * A {@code startDemo} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    @PostMapping("/demo")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.OPERATOR_WRITE)
    public ProcessingJobDto startDemo(@RequestBody(required = false) StartDemoJobRequest request) {
        StartDemoJobRequest effectiveRequest = request == null ? new StartDemoJobRequest(null, null) : request;
        return processingJobService.startDemoJob(effectiveRequest.xmlFileId(), effectiveRequest.normalizedDurationSeconds());
    }

    /**
     * A {@code activeJob} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler(ProcessingJobService.ActiveProcessingJobException.class)
    public ResponseEntity<Map<String, Object>> activeJob(ProcessingJobService.ActiveProcessingJobException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", ex.getMessage(),
                "activeJobId", ex.getJobId(),
                "activeJobType", ex.getJobType(),
                "activeJobStatus", ex.getStatus()
        ));
    }

    /**
     * A {@code badRequest} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> badRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(ex.getMessage()));
    }
}
