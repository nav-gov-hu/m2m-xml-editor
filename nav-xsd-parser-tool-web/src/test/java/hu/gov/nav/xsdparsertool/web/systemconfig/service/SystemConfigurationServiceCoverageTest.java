package hu.gov.nav.xsdparsertool.web.systemconfig.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.secret.service.RuntimeSecretBindingService;
import hu.gov.nav.xsdparsertool.web.secret.service.SystemSecretService;
import hu.gov.nav.xsdparsertool.web.systemconfig.dto.ConfigurationItemDto;
import hu.gov.nav.xsdparsertool.web.systemconfig.dto.ConfigurationSaveResponse;
import hu.gov.nav.xsdparsertool.web.systemconfig.entity.SystemConfigurationEntity;
import hu.gov.nav.xsdparsertool.web.systemconfig.repository.SystemConfigurationRepository;

@ExtendWith(MockitoExtension.class)
class SystemConfigurationServiceCoverageTest {

    @Mock
    private SystemConfigurationRepository repository;
    @Mock
    private Environment environment;
    @Mock
    private SystemSecretService secrets;
    @Mock
    private AuditLogService audit;
    @Mock
    private RuntimeSecretBindingService runtimeSecretBindingService;

    @TempDir
    Path tempDir;

    private Path bootstrapFile;
    private SystemConfigurationService service;

    @BeforeEach
    void setUp() {
        bootstrapFile = tempDir.resolve("config").resolve("paths.properties");
        lenient().when(environment.getProperty("nav.xsdparsertool.bootstrap-config-file")).thenReturn(bootstrapFile.toString());
        service = new SystemConfigurationService(repository, environment, secrets, audit,
                runtimeSecretBindingService);
    }

    @Test
    void listUsesDatabaseBootstrapEnvironmentAndEncryptedSecretSources() throws Exception {
        SystemConfigurationEntity databaseValue = entity("logging.level.root", "DEBUG");
        when(RepositoryAccess.findAll(repository)).thenReturn(List.of(databaseValue));
        when(secrets.exists("nav.xsdparsertool.api-key.value")).thenReturn(true);
        ExceptionSafeOperations.createDirectories(bootstrapFile.getParent());
        Files.writeString(bootstrapFile, "server.port=9090\n", StandardCharsets.UTF_8);
        when(environment.containsProperty(any())).thenAnswer(invocation ->
                "nav.xsdparsertool.desktop.enabled".equals(invocation.getArgument(0)));
        when(environment.getProperty("nav.xsdparsertool.desktop.enabled", "")).thenReturn("true");

        List<ConfigurationItemDto> result = service.list();

        ConfigurationItemDto db = item(result, "logging.level.root");
        assertEquals("DATABASE", db.source());
        assertEquals("DEBUG", db.value());
        assertTrue(db.databasePersisted());

        ConfigurationItemDto bootstrap = item(result, "server.port");
        assertEquals("BOOTSTRAP_FILE", bootstrap.source());
        assertEquals("9090", bootstrap.value());

        ConfigurationItemDto env = item(result, "nav.xsdparsertool.desktop.enabled");
        assertEquals("ENVIRONMENT", env.source());
        assertEquals("true", env.value());

        ConfigurationItemDto secret = item(result, "nav.xsdparsertool.api-key.value");
        assertEquals("ENCRYPTED_DATABASE", secret.source());
        assertEquals("", secret.value());
        assertFalse(secret.missing());
        assertTrue(secret.databasePersisted());
    }

    @Test
    void saveDatabaseValueTrimsPersistsAndAuditsChange() throws Exception {
        when(RepositoryAccess.findById(repository, "logging.level.root")).thenReturn(Optional.empty());
        when(repository.save(any(SystemConfigurationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfigurationSaveResponse response = service.save(Map.of("logging.level.root", " WARN "), Set.of(), "alice");

        assertEquals(1, response.savedDatabaseValues());
        assertEquals(0, response.savedBootstrapValues());
        assertTrue(response.restartRequired());
        assertEquals(List.of("logging.level.root"), response.changedKeys());
        ArgumentCaptor<SystemConfigurationEntity> captor = ArgumentCaptor.forClass(SystemConfigurationEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("WARN", captor.getValue().getValue());
        assertEquals("alice", captor.getValue().getUpdatedBy());
        verify(audit).log(eq("SYSTEM_CONFIGURATION_UPDATE"), eq("alice"), eq("SUCCESS"), any());
    }

    @Test
    void runtimeDatabaseBindingPublishesAndRemovesEnvironmentValue() {
        StandardEnvironment runtimeEnvironment = new StandardEnvironment();
        SystemConfigurationService runtimeService = new SystemConfigurationService(
                repository, runtimeEnvironment, secrets, audit, runtimeSecretBindingService);

        runtimeService.bindRuntimeDatabaseValue(
                "nav.xsdparsertool.paths.schema-dir", "C:\\ProgramData\\M2M-XML-EDITOR\\repo\\xsd");

        assertEquals("C:\\ProgramData\\M2M-XML-EDITOR\\repo\\xsd",
                runtimeEnvironment.getProperty("nav.xsdparsertool.paths.schema-dir"));

        runtimeService.removeRuntimeDatabaseValue("nav.xsdparsertool.paths.schema-dir");

        assertFalse(runtimeEnvironment.containsProperty("nav.xsdparsertool.paths.schema-dir"));
    }

    @Test
    void runtimeDatabaseBindingIgnoresBootstrapKeys() {
        StandardEnvironment runtimeEnvironment = new StandardEnvironment();
        SystemConfigurationService runtimeService = new SystemConfigurationService(
                repository, runtimeEnvironment, secrets, audit, runtimeSecretBindingService);

        runtimeService.bindRuntimeDatabaseValue("spring.datasource.url", "jdbc:should-not-bind");

        assertFalse(runtimeEnvironment.containsProperty("spring.datasource.url"));
    }

    @Test
    void saveEncryptedSecretRequiresConfirmationAndRefreshesRuntimeBinding() throws Exception {
        ConfigurationSaveResponse skipped = service.save(
                Map.of("nav.xsdparsertool.github-schema-updater.token", "secret-token"), Set.of(), "alice");
        assertTrue(skipped.changedKeys().isEmpty());
        verify(secrets, never()).save(any(), any(), any());

        ConfigurationSaveResponse saved = service.save(
                Map.of("nav.xsdparsertool.github-schema-updater.token", "secret-token"),
                Set.of("nav.xsdparsertool.github-schema-updater.token"), "alice");

        assertEquals(1, saved.savedDatabaseValues());
        verify(secrets).save("nav.xsdparsertool.github-schema-updater.token", "secret-token", "alice");
        verify(runtimeSecretBindingService).refresh();
    }

    @Test
    void datasourcePasswordUpdatesBootstrapConfigurationOnly() throws Exception {
        ExceptionSafeOperations.createDirectories(bootstrapFile.getParent());
        Files.writeString(bootstrapFile, "spring.datasource.password=old-pass\nserver.port=8080\n", StandardCharsets.UTF_8);

        ConfigurationSaveResponse response = service.save(
                Map.of("spring.datasource.password", "new-pass"),
                Set.of("spring.datasource.password"), "admin");

        assertEquals(1, response.savedBootstrapValues());
        assertTrue(response.restartRequired());
        assertEquals(List.of("spring.datasource.password"), response.changedKeys());
        String stored = Files.readString(bootstrapFile, StandardCharsets.UTF_8);
        assertTrue(stored.contains("spring.datasource.password=new-pass"));
        assertTrue(ExceptionSafeOperations.fileExists(bootstrapFile.resolveSibling("paths.properties.bak")));
    }

    @Test
    void saveSkipsUnknownUnconfirmedAndUnchangedValues() throws Exception {
        SystemConfigurationEntity existing = entity("logging.level.root", "INFO");
        when(RepositoryAccess.findById(repository, "logging.level.root")).thenReturn(Optional.of(existing));

        ConfigurationSaveResponse response = service.save(Map.of(
                "unknown.key", "x",
                "logging.level.root", " INFO ",
                "nav.xsdparsertool.api-key.value", "secret"), Set.of(), "alice");

        assertEquals(0, response.savedDatabaseValues());
        assertEquals(0, response.savedBootstrapValues());
        assertTrue(response.changedKeys().isEmpty());
        verify(repository, never()).save(any());
        verify(audit, never()).log(eq("SYSTEM_CONFIGURATION_UPDATE"), any(), any(), any());
    }

    @Test
    void resetRemovesBootstrapDatabaseAndSecretValuesAndRefreshesSecrets() throws Exception {
        ExceptionSafeOperations.createDirectories(bootstrapFile.getParent());
        Files.writeString(bootstrapFile, "server.port=9191\n", StandardCharsets.UTF_8);
        when(repository.existsById("logging.level.root")).thenReturn(true);
        when(secrets.exists("nav.xsdparsertool.github-schema-updater.token")).thenReturn(true);

        ConfigurationSaveResponse response = service.reset(List.of(
                "server.port",
                "logging.level.root",
                "nav.xsdparsertool.github-schema-updater.token",
                "unknown.key"), "admin");

        assertEquals(2, response.savedDatabaseValues());
        assertEquals(1, response.savedBootstrapValues());
        assertTrue(response.restartRequired());
        verify(repository).deleteById("logging.level.root");
        verify(secrets).delete("nav.xsdparsertool.github-schema-updater.token");
        verify(runtimeSecretBindingService).refresh();
        verify(audit).log(eq("SYSTEM_CONFIGURATION_RESET"), eq("admin"), eq("SUCCESS"), any());
        assertFalse(Files.readString(bootstrapFile, StandardCharsets.UTF_8).contains("server.port"));
    }

    @Test
    void resetEmptyInputIsNoOp() throws Exception {
        ConfigurationSaveResponse nullResult = service.reset(null, "admin");
        ConfigurationSaveResponse emptyResult = service.reset(List.of(), "admin");

        assertTrue(nullResult.changedKeys().isEmpty());
        assertTrue(emptyResult.changedKeys().isEmpty());
        verify(repository, never()).deleteById(any());
        verify(secrets, never()).delete(any());
    }

    @Test
    void saveValidatesRequiredNumberBooleanAndSelectValues() {
        assertThrows(IllegalArgumentException.class,
                () -> service.save(Map.of("server.port", "not-a-number"), Set.of(), "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.save(Map.of("nav.xsdparsertool.desktop.enabled", "maybe"), Set.of(), "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.save(Map.of("logging.level.root", "VERBOSE"), Set.of(), "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.save(Map.of("server.port", "   "), Set.of(), "admin"));
    }

    @Test
    void bootstrapPathUsesSpringConfigImportWhenExplicitPathMissing() {
        when(environment.getProperty("nav.xsdparsertool.bootstrap-config-file")).thenReturn(null);
        when(environment.getProperty("spring.config.import", "")).thenReturn("optional:file:" + tempDir.resolve("imported.properties"));

        Path resolved = service.bootstrapPath();

        assertEquals(tempDir.resolve("imported.properties").toAbsolutePath().normalize(), resolved);
    }

    private static ConfigurationItemDto item(List<ConfigurationItemDto> items, String key) {
        return items.stream().filter(item -> key.equals(item.key())).findFirst().orElseThrow();
    }

    private static SystemConfigurationEntity entity(String key, String value) {
        SystemConfigurationEntity entity = new SystemConfigurationEntity();
        entity.setKey(key);
        entity.setValue(value);
        return entity;
    }
}
