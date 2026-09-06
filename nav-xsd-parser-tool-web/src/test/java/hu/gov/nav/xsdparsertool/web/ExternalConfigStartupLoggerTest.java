package hu.gov.nav.xsdparsertool.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class ExternalConfigStartupLoggerTest {

    @TempDir
    Path tempDir;

    @Test
    void runUsesConfiguredApplicationDataDirectoryWithoutWindowsProgramDataDependency() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.data.dir", tempDir.toString());
        ExternalConfigStartupLogger logger = new ExternalConfigStartupLogger(environment, environment);

        assertDoesNotThrow(() -> logger.run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void runDoesNotFailWhenApplicationDataDirectoryIsMissing() {
        MockEnvironment environment = new MockEnvironment();
        ExternalConfigStartupLogger logger = new ExternalConfigStartupLogger(environment, environment);

        assertDoesNotThrow(() -> logger.run(new DefaultApplicationArguments(new String[0])));
    }
}
