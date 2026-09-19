package hu.gov.nav.xsdparsertool.web.setup;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A setup újraindítás utáni böngészőlap-kezelésének frontend szerződését ellenőrzi.
 */
class SetupRestartWindowCloseFrontendContractTest {

    private static final Path SETUP_JS = Path.of(
            "src/main/resources/static/js/pages/setup.js");

    @Test
    void successfulRestartClosesOldSetupTabWithLoginFallback() throws IOException {
        String source = Files.readString(SETUP_JS, StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(source.contains("function closeSetupWindowAfterRestart()"));
        assertTrue(source.contains("window.close();"));
        assertTrue(source.contains("if (!window.closed)"));
        assertTrue(source.contains("location.replace('/login.html');"));
        assertTrue(source.contains("if (completedAfterRestart || status.completed) {\n"
                + "                    closeSetupWindowAfterRestart();"));
    }
}
