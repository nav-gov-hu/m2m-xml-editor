package hu.gov.nav.xsdparsertool.web.systemconfig.api;

import hu.gov.nav.xsdparsertool.core.security.AuthorizationRules;
import hu.gov.nav.xsdparsertool.web.systemconfig.dto.ConfigurationItemDto;
import hu.gov.nav.xsdparsertool.web.systemconfig.dto.ConfigurationSaveRequest;
import hu.gov.nav.xsdparsertool.web.systemconfig.dto.ConfigurationSaveResponse;
import hu.gov.nav.xsdparsertool.web.systemconfig.dto.ConfigurationResetRequest;
import hu.gov.nav.xsdparsertool.web.systemconfig.service.SystemConfigurationService;
import hu.gov.nav.xsdparsertool.web.systemconfig.transfer.ConfigurationImportResult;
import hu.gov.nav.xsdparsertool.web.systemconfig.transfer.ConfigurationTransferDocument;
import hu.gov.nav.xsdparsertool.web.systemconfig.transfer.ConfigurationTransferService;
import hu.gov.nav.xsdparsertool.web.setup.ApplicationRestartService;
import hu.gov.nav.xsdparsertool.web.security.SecurityMode;
import hu.gov.nav.xsdparsertool.web.security.SecurityModeProperties;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code SystemConfigurationController} osztály a web modul REST API területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController
@RequestMapping("/api/admin/configuration")
@PreAuthorize(AuthorizationRules.ADMIN_ONLY)
public class SystemConfigurationController {
    private final SystemConfigurationService service;
    private final ApplicationRestartService restartService;
    private final SecurityModeProperties securityModeProperties;
    private final ConfigurationTransferService transferService;

    /**
     * Létrehozza a {@code SystemConfigurationController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param service a művelet bemeneti {@code service} értéke
     * @param restartService a művelet bemeneti {@code restartService} értéke
     * @param securityModeProperties a művelethez szükséges konfigurációs adatok
     * @param transferService teljes konfiguráció export/import szolgáltatás
     */
    public SystemConfigurationController(SystemConfigurationService service,
                                         ApplicationRestartService restartService,
                                         SecurityModeProperties securityModeProperties,
                                         ConfigurationTransferService transferService) {
        this.service = service;
        this.restartService = restartService;
        this.securityModeProperties = securityModeProperties;
        this.transferService = transferService;
    }

    /**
     * A {@code list} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    @GetMapping
    public List<ConfigurationItemDto> list() { return service.list(); }

    /**
     * A {@code runtime} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @GetMapping("/runtime")
    public Map<String, Object> runtime() {
        boolean standalone = securityModeProperties.getSecurityMode() == SecurityMode.STANDALONE;
        return Map.of("standalone", standalone, "restartAvailable", standalone && restartService.isRestartAvailable());
    }

    /**
     * A {@code restart} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    @PostMapping("/restart")
    public Map<String, Object> restart() {
        if (securityModeProperties.getSecurityMode() != SecurityMode.STANDALONE) {
            throw new IllegalStateException("Az alkalmazás újraindítása csak standalone módban érhető el.");
        }
        boolean scheduled = restartService.scheduleRestart();
        if (!scheduled) {
            throw new IllegalStateException("Az automatikus újraindítás ebben a futtatási környezetben nem érhető el.");
        }
        return Map.of("scheduled", true);
    }

    /**
     * A {@code save} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping
    public ConfigurationSaveResponse save(@RequestBody ConfigurationSaveRequest request) throws IOException {
        String username = currentActor();
        return service.save(request == null ? Map.of() : request.values(),
                request == null || request.confirmedSensitiveKeys() == null ? Set.of() : request.confirmedSensitiveKeys(), username);
    }
    /**
     * A {@code reset} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @PostMapping("/reset")
    public ConfigurationSaveResponse reset(@RequestBody ConfigurationResetRequest request) throws IOException {
        String username = currentActor();
        return service.reset(request == null ? List.of() : request.keys(), username);
    }

    /** Teljes konfigurációs snapshot exportja.
     * @return a teljes hordozható konfigurációs dokumentum
     * @throws IOException fájlkonfiguráció olvasási hiba esetén
     */
    @GetMapping("/export")
    public ConfigurationTransferDocument exportConfiguration() throws IOException {
        return transferService.exportConfiguration();
    }

    /** Teljes konfigurációs snapshot MERGE importja.
     * @param document az importálandó konfigurációs dokumentum
     * @return az alkalmazott merge összesítése
     * @throws IOException fájlkonfiguráció írási hiba esetén
     */
    @PostMapping("/import")
    public ConfigurationImportResult importConfiguration(@RequestBody ConfigurationTransferDocument document) throws IOException {
        return transferService.importConfiguration(document, currentActor());
    }

    /**
     * A {@code currentActor} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    private String currentActor() {
        String username = CurrentUserService.resolveAuthenticatedUsername();
        return username == null ? "system" : username;
    }

}
