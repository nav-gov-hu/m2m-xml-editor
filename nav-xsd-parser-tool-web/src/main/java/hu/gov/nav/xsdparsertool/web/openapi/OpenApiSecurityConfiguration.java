package hu.gov.nav.xsdparsertool.web.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI API kulcsos tesztelese.
 */
@Configuration
public class OpenApiSecurityConfiguration {

    /**
     * A {@code navXsdParserToolOpenApi} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    @Bean
    public OpenAPI navXsdParserToolOpenApi() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("apiKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("Kulső API kulcs. External API key.")))
                .addSecurityItem(new SecurityRequirement().addList("apiKeyAuth"));
    }
}
