package hu.gov.nav.xsdparsertool.core.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExceptionSafeOperationsTest {

    @TempDir
    Path tempDir;

    @Test
    void missingAndNullPathsReturnFalseWithoutPropagatingRuntimeFailures() {
        assertFalse(ExceptionSafeOperations.fileExists(null));
        assertFalse(ExceptionSafeOperations.isRegularFile(null));
        assertFalse(ExceptionSafeOperations.isDirectory(null));
        assertFalse(ExceptionSafeOperations.fileExists(tempDir.resolve("missing.txt")));
    }

    @Test
    void missingSystemPropertyUsesFallback() {
        assertEquals("fallback",
                ExceptionSafeOperations.systemProperty("nav.test.missing.property", "fallback"));
    }
}
