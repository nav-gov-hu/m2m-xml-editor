package hu.gov.nav.xsdparsertool.web.xsdvalidation.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hu.gov.nav.xsdparsertool.web.api.dto.ApiErrorResponse;
import hu.gov.nav.xsdparsertool.web.processing.dto.ProcessingJobDto;
import hu.gov.nav.xsdparsertool.web.processing.service.ProcessingJobService;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.dto.XsdValidationErrorDto;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.dto.XsdValidationResultDto;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.service.StreamingXsdValidationService;

/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code XsdValidationController} osztály a web modul XSD-validációs területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController
@RequestMapping("/api/xsd-validation")
public class XsdValidationController {
    private final StreamingXsdValidationService streamingXsdValidationService;

    /**
     * Létrehozza a {@code XsdValidationController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param streamingXsdValidationService a művelet bemeneti {@code streamingXsdValidationService} értéke
     */
    public XsdValidationController(StreamingXsdValidationService streamingXsdValidationService) {
        this.streamingXsdValidationService = streamingXsdValidationService;
    }

    /**
     * A {@code startActiveXmlValidation} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>Az XML-adatot a XSD-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a művelet feldolgozási eredménye
     */
    @PostMapping("/active/start")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public ProcessingJobDto startActiveXmlValidation() {
        return streamingXsdValidationService.startValidationForActiveXmlFile();
    }

    /**
     * A {@code latestForActiveXmlFile} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a XSD-validációs folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a művelet feldolgozási eredménye
     */
    @GetMapping("/active/latest")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public XsdValidationResultDto latestForActiveXmlFile() {
        return streamingXsdValidationService.getLatestForActiveXmlFile();
    }

    /**
     * A {@code request} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    @GetMapping("/requests/{requestId}")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public XsdValidationResultDto request(@PathVariable String requestId) {
        return streamingXsdValidationService.getRequest(requestId);
    }

    /**
     * A {@code errors} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param requestId a célobjektum vagy erőforrás azonosítója
     * @return a művelet eredményeként előállított elemek listája
     */
    @GetMapping("/requests/{requestId}/errors")
    @PreAuthorize(hu.gov.nav.xsdparsertool.core.security.AuthorizationRules.AUTHENTICATED_READ)
    public List<XsdValidationErrorDto> errors(@PathVariable String requestId) {
        return streamingXsdValidationService.getErrors(requestId);
    }

    /**
     * A {@code activeJob} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
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
     * <p>A művelet a XSD-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param ex a művelet bemeneti {@code ex} értéke
     * @return a művelet feldolgozási eredménye
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> badRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(ex.getMessage()));
    }
}
