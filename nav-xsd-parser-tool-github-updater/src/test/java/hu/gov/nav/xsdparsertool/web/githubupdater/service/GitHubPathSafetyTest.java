package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitHubPathSafetyTest {

    @TempDir
    Path tempDir;

    @Test
    void safeSegmentRejectsDotSegmentsAndMapsSeparatorsToSingleSegment() {
        assertThrows(IllegalArgumentException.class, () -> GitHubPathSafety.safeSegment("."));
        assertThrows(IllegalArgumentException.class, () -> GitHubPathSafety.safeSegment(".."));
        assertEquals("release_1.2.3", GitHubPathSafety.safeSegment("release/1.2.3"));
        assertEquals("NAV-2608", GitHubPathSafety.safeSegment("NAV-2608"));
    }

    @Test
    void resolveRelativeInsideRejectsAbsoluteAndParentTraversal() {
        Path nested = GitHubPathSafety.resolveRelativeInside(tempDir, Path.of("xsd/forms/sample.xsd"));

        assertTrue(nested.startsWith(tempDir.toAbsolutePath().normalize()));
        assertEquals(tempDir.resolve("xsd/forms/sample.xsd").toAbsolutePath().normalize(), nested);
        assertThrows(IllegalArgumentException.class,
                () -> GitHubPathSafety.resolveRelativeInside(tempDir, Path.of("../escape.xsd")));
        assertThrows(IllegalArgumentException.class,
                () -> GitHubPathSafety.resolveRelativeInside(tempDir, tempDir.resolve("absolute.xsd").toAbsolutePath()));
    }

    @Test
    void resolveInsideNeverLeavesConfiguredRoot() {
        Path resolved = GitHubPathSafety.resolveInside(tempDir, "NAV-2608", "v1.2.3");

        assertTrue(resolved.startsWith(tempDir.toAbsolutePath().normalize()));
        assertEquals(tempDir.resolve("NAV-2608").resolve("v1.2.3").toAbsolutePath().normalize(), resolved);
        assertThrows(IllegalArgumentException.class, () -> GitHubPathSafety.resolveInside(tempDir, ".."));
    }
}
