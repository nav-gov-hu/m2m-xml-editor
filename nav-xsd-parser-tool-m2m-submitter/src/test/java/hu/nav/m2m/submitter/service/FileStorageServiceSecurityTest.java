package hu.nav.m2m.submitter.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import hu.nav.m2m.submitter.config.NavM2mProperties;

class FileStorageServiceSecurityTest {

    @TempDir
    Path tempDir;

    @Test
    void storeAttachmentRejectsPathBearingOriginalFileName() throws Exception {
        FileStorageService service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file", "../escape.xml", "application/xml", "<root/>".getBytes());

        assertThrows(IOException.class, () -> service.storeAttachment(12L, UUID.randomUUID(), file));
        assertFalse(ExceptionSafeOperations.fileExists(tempDir.resolve("escape.xml")));
    }

    @Test
    void storeAttachmentKeepsGeneratedTargetBelowConfiguredStorageRoot() throws Exception {
        FileStorageService service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file", "attachment.xml", "application/xml", "<root/>".getBytes());

        FileStorageService.StoredFile stored = service.storeAttachment(12L, UUID.randomUUID(), file);
        Path storedPath = Path.of(stored.storagePath()).toAbsolutePath().normalize();

        assertTrue(storedPath.startsWith(tempDir.toAbsolutePath().normalize()));
        assertTrue(ExceptionSafeOperations.isRegularFile(storedPath));
    }

    private FileStorageService service() throws IOException {
        NavM2mProperties properties = new NavM2mProperties();
        properties.setStorageDirectory(tempDir.toString());
        return new FileStorageService(properties);
    }
}
