
package hu.gov.nav.xsdparsertool.web.api;

import hu.gov.nav.xsdparsertool.schemaregistry.service.FileSystemSchemaRegistryService;
import hu.gov.nav.xsdparsertool.schemaregistry.service.SchemaRegistryStatus;
import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
/**
 * A Schema Registry állapotlekérdezési és újratöltési műveleteit publikáló REST controller.
 * Az osztály a api csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: @RestController.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: @RestController.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


@Tag(name = "SchemaRegistry", description = "Séma-regiszter állapotát és újratöltését kezelő végpontok. / Endpoints for schema registry status and reload.")
@RestController
@RequestMapping("/api/schema-registry")
public class SchemaRegistryController {
    private final FileSystemSchemaRegistryService schemaRegistryService;
    private final PathConfigurationProperties pathConfigurationProperties;

    /**
     * Létrehozza a {@code SchemaRegistryController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param schemaRegistryService a művelet bemeneti {@code schemaRegistryService} értéke
     * @param pathConfigurationProperties a feldolgozásban részt vevő fájl vagy elérési út
     */
    public SchemaRegistryController(FileSystemSchemaRegistryService schemaRegistryService,
                                    PathConfigurationProperties pathConfigurationProperties) {
        this.schemaRegistryService = schemaRegistryService;
        this.pathConfigurationProperties = pathConfigurationProperties;
    }

    
    /**
     * A {@code status} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    @Operation(summary = "HU: status REST művelet. EN: status REST operation.", description = "HU: Dokumentált REST végpont. EN: Documented REST endpoint.")

    @ApiResponses({

        @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

        @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

        @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@GetMapping("/status")
/**
 * Visszaadja a Schema Registry aktuális betöltési és cache állapotát.
 * @return a metódus által előállított eredmény
 */
    public SchemaRegistryStatus status() {
        return schemaRegistryService.getStatus();
    }

    
    /**
     * A {@code reload} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    @Operation(summary = "HU: reload REST művelet. EN: reload REST operation.", description = "HU: Dokumentált REST végpont. EN: Documented REST endpoint.")

    @ApiResponses({

        @ApiResponse(responseCode = "200", description = "HU: Sikeres végrehajtás. EN: Successful execution."),

        @ApiResponse(responseCode = "400", description = "HU: Hibás kérés. EN: Bad request.", content = @Content(schema = @Schema(implementation = String.class))),

        @ApiResponse(responseCode = "500", description = "HU: Belső szerverhiba. EN: Internal server error.", content = @Content(schema = @Schema(implementation = String.class)))

    })
@PostMapping("/reload")
/**
 * Újraindítja a Schema Registry indexelését az aktuálisan konfigurált séma-gyökerekkel.
 * @return a metódus által előállított eredmény
 */
    public SchemaRegistryStatus reload() {
        Path schemaRoot = toPath(pathConfigurationProperties.getSchemaDir());
        Path generalRoot = toPath(pathConfigurationProperties.getCommonXsdDir());
        schemaRegistryService.reloadAsync(schemaRoot, generalRoot);
        return schemaRegistryService.getStatus();
    }
/**
 * A konfigurációs szöveget opcionális, normalizált fájlrendszeri útvonallá alakítja.
 * @param value a {@code value} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private Path toPath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Path.of(value.trim()).toAbsolutePath().normalize();
    }
}
