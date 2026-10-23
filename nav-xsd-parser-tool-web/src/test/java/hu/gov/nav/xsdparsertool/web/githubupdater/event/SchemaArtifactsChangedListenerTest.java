package hu.gov.nav.xsdparsertool.web.githubupdater.event;

import hu.gov.nav.xsdparsertool.schemaregistry.service.FileSystemSchemaRegistryService;
import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SchemaArtifactsChangedListenerTest {

    @Test
    void packageChangeReloadsSchemaRegistryWithConfiguredRoots() {
        FileSystemSchemaRegistryService registry = mock(FileSystemSchemaRegistryService.class);
        PathConfigurationProperties properties = new PathConfigurationProperties();
        properties.setSchemaDir("schema-root");
        properties.setCommonXsdDir("common-root");
        SchemaArtifactsChangedListener listener = new SchemaArtifactsChangedListener(registry, properties);

        listener.onSchemaArtifactsChanged(new SchemaArtifactsChangedEvent("test"));

        verify(registry).reloadAsync(
                Path.of("schema-root").toAbsolutePath().normalize(),
                Path.of("common-root").toAbsolutePath().normalize());
    }
}
