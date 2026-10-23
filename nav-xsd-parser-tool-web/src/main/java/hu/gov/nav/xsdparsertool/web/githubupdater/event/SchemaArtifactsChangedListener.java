package hu.gov.nav.xsdparsertool.web.githubupdater.event;

import hu.gov.nav.xsdparsertool.schemaregistry.service.FileSystemSchemaRegistryService;
import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * A csomagfrissítések után automatikusan érvényteleníti és újraépíti a
 * FileSystemSchemaRegistryService cache-ét, hogy az új XSD-k azonnal feloldhatók legyenek.
 */
@Component
public class SchemaArtifactsChangedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaArtifactsChangedListener.class);

    private final FileSystemSchemaRegistryService schemaRegistryService;
    private final PathConfigurationProperties pathConfigurationProperties;

    public SchemaArtifactsChangedListener(FileSystemSchemaRegistryService schemaRegistryService,
                                          PathConfigurationProperties pathConfigurationProperties) {
        this.schemaRegistryService = schemaRegistryService;
        this.pathConfigurationProperties = pathConfigurationProperties;
    }

    @EventListener
    public void onSchemaArtifactsChanged(SchemaArtifactsChangedEvent event) {
        Path schemaRoot = toPath(pathConfigurationProperties.getSchemaDir());
        Path commonRoot = toPath(pathConfigurationProperties.getCommonXsdDir());
        LOGGER.info("Séma-regiszter cache automatikus újratöltése csomagváltozás után: source={}", event.source());
        schemaRegistryService.reloadAsync(schemaRoot, commonRoot);
    }

    private Path toPath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Path.of(value.trim()).toAbsolutePath().normalize();
    }
}
