package hu.gov.nav.xsdparsertool.web.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
/**
 * Az alkalmazás egyszerű elérhetőségi és állapotellenőrző REST végpontját biztosító controller.
 * Az osztály a api csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: @RestController.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: @RestController.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


@Tag(name = "Health", description = "Egészségügyi és életjel végpont. / Health and liveness endpoint.")
@RestController
public class HealthController {
        /**
         * A {@code health} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feldolgozás során felépített kulcs-érték leképezés
         */
        @Operation(summary = "HU: health REST művelet. EN: health REST operation.", description = "HU: Dokumentált REST végpont. EN: Documented REST endpoint.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),
        @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))
    })
@GetMapping("/api/health")
/**
 * Egyszerű állapotválaszt ad vissza az alkalmazás elérhetőségének ellenőrzéséhez.
 * @return a metódus által előállított eredmény
 */
    public Map<String, String> health() {
        return Map.of("application", "nav-xsd-parser-tool-web", "status", "UP");
    }
}
