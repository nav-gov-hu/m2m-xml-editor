package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class GitHubSchemaUpdaterZipSecurityTest {

    @TempDir Path tempDir;

    @Test
    void archiveWithTooManyEntriesMustBeRejected() throws Exception {
        Path archive = tempDir.resolve("too-many.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (int i = 0; i < 20_001; i++) {
                zip.putNextEntry(new ZipEntry("entry-" + i));
                zip.closeEntry();
            }
        }
        Path target = Files.createDirectory(tempDir.resolve("out"));
        GitHubSchemaUpdaterService service = mock(GitHubSchemaUpdaterService.class, CALLS_REAL_METHODS);
        Method extract = GitHubSchemaUpdaterService.class.getDeclaredMethod(
                "extractZipStrippingRoot", Path.class, Path.class);
        extract.setAccessible(true);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                () -> extract.invoke(service, archive, target));

        IOException cause = assertInstanceOf(IOException.class, thrown.getCause());
        assertTrue(cause.getMessage().contains("túl sok bejegyzést"));
    }
    @Test
    void archiveWithParentTraversalEntryMustBeRejected() throws Exception {
        Path archive = tempDir.resolve("traversal.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("repo-root/../../escape.txt"));
            zip.write("blocked".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        Path target = Files.createDirectory(tempDir.resolve("traversal-out"));
        GitHubSchemaUpdaterService service = mock(GitHubSchemaUpdaterService.class, CALLS_REAL_METHODS);
        Method extract = GitHubSchemaUpdaterService.class.getDeclaredMethod(
                "extractZipStrippingRoot", Path.class, Path.class);
        extract.setAccessible(true);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                () -> extract.invoke(service, archive, target));

        IOException cause = assertInstanceOf(IOException.class, thrown.getCause());
        assertTrue(cause.getMessage().contains("Unsafe ZIP entry path"));
        assertTrue(Files.notExists(tempDir.resolve("escape.txt")));
    }

}
