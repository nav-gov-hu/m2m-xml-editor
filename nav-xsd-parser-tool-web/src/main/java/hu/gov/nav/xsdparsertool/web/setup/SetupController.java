package hu.gov.nav.xsdparsertool.web.setup;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code SetupController} osztály a web modul kezdeti beállítási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController
@RequestMapping("/api/setup")
public class SetupController {

    private final SetupStateService state;
    private final SetupService service;
    private final ApplicationRestartService restartService;

    /**
     * Létrehozza a {@code SetupController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param state a feldolgozandó elemek kollekciója
     * @param service a feldolgozandó elemek kollekciója
     * @param restartService a művelet bemeneti {@code restartService} értéke
     */
    public SetupController(
            SetupStateService state,
            SetupService service,
            ApplicationRestartService restartService) {
        this.state = state;
        this.service = service;
        this.restartService = restartService;
    }

    /**
     * A {@code status} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "completed", state.isCompleted(),
                "defaultDataDirectory", service.defaultDataDirectory(),
                "securityMode", service.currentSecurityMode(),
                "database", service.currentDatabaseSetup(),
                "installerPreset", service.currentInstallerPreset(),
                "pendingCompletion", service.hasPendingSetupCompletion(),
                "pendingCompletionError", service.pendingSetupCompletionError());
    }

    /**
     * A setupban megadott adatbázis-kapcsolatot tényleges JDBC kapcsolattal ellenőrzi.
     *
     * @param request az adatbázis-kapcsolati paraméterek
     * @param httpRequest a helyi kérés ellenőrzéséhez használt servlet kérés
     * @return a sikeres teszt eredménye és a végleges mentéshez szükséges rövid élettartamú token
     */
    @PostMapping("/database/test")
    public ResponseEntity<?> testDatabase(@RequestBody SetupDatabaseTestRequest request, HttpServletRequest httpRequest) {
        if (!isLocalRequest(httpRequest)) {
            return ResponseEntity.status(403).body(Map.of("message",
                    "Az adatbázis-kapcsolat tesztelése biztonsági okból csak a szerver helyi gépéről végezhető el."));
        }
        try {
            String dataDirectoryToken = SetupDataDirectorySelectionVault.issue(
                    request == null ? null : request.dataDirectory());
            SetupService.DatabaseConnectionTestResult result = service.testDatabaseConnection(
                    dataDirectoryToken,
                    request == null ? null : request.databaseType(),
                    request == null ? null : request.databaseHost(),
                    request == null ? null : request.databasePort(),
                    request == null ? null : request.databaseName(),
                    request == null ? null : request.databaseSchema(),
                    request == null ? null : request.databaseUsername(),
                    request == null ? null : request.databasePassword());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Az adatbázis-kapcsolat sikeres.",
                    "databaseTestToken", result.token(),
                    "databaseProduct", result.productName(),
                    "databaseVersion", result.productVersion()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body(
                    Map.of("message", "Az adatbázis-kapcsolat tesztelése nem hajtható végre: " + ex.getMessage()));
        }
    }

    /**
     * A {@code complete} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @param httpRequest a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    @PostMapping("/complete")
    public ResponseEntity<?> complete(@RequestBody SetupRequest request, HttpServletRequest httpRequest) {
        if (!isLocalRequest(httpRequest)) {
            return ResponseEntity.status(403).body(Map.of("message",
                    "A kezdeti rendszerbeállítás biztonsági okból csak a szerver helyi gépéről végezhető el."));
        }
        try {
            String dataDirectoryToken = SetupDataDirectorySelectionVault.issue(
                    request == null ? null : request.dataDirectory());
            SetupResult result = service.completeSelected(
                    dataDirectoryToken,
                    request == null ? null : request.securityMode(),
                    request == null ? null : request.databaseType(),
                    request == null ? null : request.databaseHost(),
                    request == null ? null : request.databasePort(),
                    request == null ? null : request.databaseName(),
                    request == null ? null : request.databaseSchema(),
                    request == null ? null : request.databaseUsername(),
                    request == null ? null : request.databasePassword(),
                    request == null ? null : request.adminUsername(),
                    request == null ? null : request.adminDisplayName(),
                    request == null ? null : request.adminEmail(),
                    request == null ? null : request.adminPassword(),
                    request == null ? null : request.adminPasswordConfirmation(),
                    request == null ? null : request.githubToken(),
                    request == null ? null : request.m2mApiKey(),
                    request == null ? null : request.m2mClientId(),
                    request == null ? null : request.m2mClientSecret(),
                    request == null ? null : request.databaseTestToken(),
                    true);
            if (result.restartRequired()) {
                boolean scheduled = restartService.scheduleRestart();
                String message = scheduled
                        ? "A beállítások mentése sikeres. Az alkalmazás automatikusan újraindul."
                        : manualRestartMessage(result);
                result = result.withRestartScheduled(scheduled, message);
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body(
                    Map.of("message", "A bootstrap konfiguráció nem menthető: " + ex.getMessage()));
        }
    }

    /**
     * A {@code manualRestartMessage} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param result a feldolgozandó elemek kollekciója
     * @return a művelet feldolgozási eredménye
     */
    private String manualRestartMessage(SetupResult result) {
        if ("BOOTSTRAP_RESTART".equals(result.phase()) || "DATA_DIRECTORY_RESTART".equals(result.phase())) {
            return "Az új adatkönyvtár és adatbázis-bootstrap mentése sikeres. Az automatikus újraindítás ebben a futtatási módban nem érhető el. "
                    + "Ha IDE-ből vagy mvn spring-boot:run paranccsal futtatja az alkalmazást, állítsa le, majd indítsa újra kézzel. "
                    + "A kezdeti beállítás az újraindítás után automatikusan befejeződik.";
        }
        return "A beállítások mentése sikeres. Az automatikus újraindítás ebben a futtatási módban nem érhető el. "
                + "Ha IDE-ből vagy mvn spring-boot:run paranccsal futtatja az alkalmazást, állítsa le, majd indítsa újra kézzel.";
    }

    /**
     * A {@code isLocalRequest} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isLocalRequest(HttpServletRequest request) {
        if (request == null || request.getRemoteAddr() == null || request.getRemoteAddr().isBlank()) {
            return false;
        }
        String remoteAddress = request.getRemoteAddr().trim();
        return "127.0.0.1".equals(remoteAddress)
                || "::1".equals(remoteAddress)
                || "0:0:0:0:0:0:0:1".equals(remoteAddress);
    }
}
