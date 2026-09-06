package hu.gov.nav.xsdparsertool.web.setup;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class BootstrapDefaultsEnvironmentPostProcessorTest {

    @Test
    void cleanStartupProvidesAppDataDirForNestedPlaceholders(@TempDir Path tempDir) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.datasource.url", "");
        environment.setProperty("app.data.dir", tempDir.toString());
        environment.setProperty("m2m.xml.editor.bootstrap.auto-load-enabled", "false");

        new BootstrapDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication(Object.class));

        String dataDir = environment.getProperty("app.data.dir");
        assertThat(dataDir).isNotBlank();
        assertThat(environment.resolveRequiredPlaceholders(
                "${app.data.dir:${nav.xsdparsertool.data-directory}}/data/import"))
                .isEqualTo(dataDir + "/data/import");
        assertThat(environment.getProperty("nav.xsdparsertool.database.type")).isEqualTo("H2");
        assertThat(environment.getProperty("nav.xsdparsertool.database.schema")).isEqualTo("PUBLIC");
        assertThat(environment.getProperty("spring.h2.console.enabled", Boolean.class)).isTrue();
        assertThat(Path.of(environment.getProperty("nav.xsdparsertool.xml-file.upload-dir")))
                .isEqualTo(tempDir.resolve("data/xml").toAbsolutePath().normalize());
        assertThat(Path.of(environment.getProperty("nav.xsdparsertool.xml-file.backup-dir")))
                .isEqualTo(tempDir.resolve("backup").toAbsolutePath().normalize());
        assertThat(Path.of(environment.getProperty("nav.xsdparsertool.xml-file.archive-dir")))
                .isEqualTo(tempDir.resolve("data/archive").toAbsolutePath().normalize());
        assertThat(Path.of(environment.getProperty("nav.xsdparsertool.xml-file.xml-index-dir")))
                .isEqualTo(tempDir.resolve("data/xml-index").toAbsolutePath().normalize());
    }

    @Test
    void oracleDatasourceUsesTimestampForInstantJdbcType(@TempDir Path tempDir) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.datasource.url", "jdbc:oracle:thin:@localhost:1521/XEPDB1");
        environment.setProperty("nav.xsdparsertool.database.type", "ORACLE");
        environment.setProperty("app.data.dir", tempDir.toString());
        environment.setProperty("m2m.xml.editor.bootstrap.auto-load-enabled", "false");

        new BootstrapDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty(
                "spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type"))
                .isEqualTo("TIMESTAMP");
    }

    @Test
    void nonOracleDatasourceDoesNotForceInstantJdbcType(@TempDir Path tempDir) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/nav_xsd_parser_tool");
        environment.setProperty("nav.xsdparsertool.database.type", "MYSQL");
        environment.setProperty("app.data.dir", tempDir.toString());
        environment.setProperty("m2m.xml.editor.bootstrap.auto-load-enabled", "false");

        new BootstrapDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertThat(environment.getProperty(
                "spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type"))
                .isNull();
    }
}
