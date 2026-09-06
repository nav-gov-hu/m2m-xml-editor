package hu.nav.m2m.submitter.service;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagedStoragePathPolicyTest {

    @TempDir
    Path tempDir;

    @Test
    void acceptsExistingFileInsideConfiguredStorageRoot() throws Exception {
        Path root = tempDir.resolve("storage");
        Files.createDirectories(root);
        Path file = root.resolve("submission.xml");
        Files.writeString(file, "<Doc/>");
        ManagedStoragePathPolicy policy = policy(root);

        assertEquals(file.toRealPath(), policy.requireReadableFile(file));
    }

    @Test
    void rejectsStoredPathOutsideConfiguredStorageRoot() throws Exception {
        Path root = tempDir.resolve("storage");
        Files.createDirectories(root);
        Path outside = tempDir.resolve("secret.txt");
        Files.writeString(outside, "secret");
        ManagedStoragePathPolicy policy = policy(root);

        assertThrows(IOException.class, () -> policy.requireReadableFile(outside));
    }


    @Test
    void readsSubmissionAttachmentOnlyFromTrustedSubmissionDirectory() throws Exception {
        Path root = tempDir.resolve("storage");
        java.util.UUID submissionId = java.util.UUID.randomUUID();
        Path dir = root.resolve(submissionId.toString());
        Files.createDirectories(dir);
        Path file = dir.resolve("filestore_1_document.txt");
        Files.writeString(file, "payload");
        ManagedStoragePathPolicy policy = policy(root);

        assertEquals("payload", new String(policy.readSubmissionAttachment(submissionId,
                tempDir.resolve("elsewhere").resolve(file.getFileName()).toString()), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void readsXmlAttachmentOnlyFromTrustedXmlAttachmentDirectory() throws Exception {
        Path root = tempDir.resolve("storage");
        long xmlFileId = 42L;
        java.util.UUID attachmentId = java.util.UUID.randomUUID();
        Path dir = root.resolve("xml-files").resolve(Long.toString(xmlFileId)).resolve("attachments").resolve(attachmentId.toString());
        Files.createDirectories(dir);
        Path file = dir.resolve("attachment.xml");
        Files.writeString(file, "<Doc/>");
        ManagedStoragePathPolicy policy = policy(root);

        assertEquals("<Doc/>", new String(policy.readXmlFileAttachment(xmlFileId, attachmentId,
                tempDir.resolve("fake").resolve(file.getFileName()).toString()), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void rejectsAttachmentWhenTrustedDirectoryDoesNotContainStoredBasename() throws Exception {
        Path root = tempDir.resolve("storage");
        java.util.UUID submissionId = java.util.UUID.randomUUID();
        Files.createDirectories(root.resolve(submissionId.toString()));
        ManagedStoragePathPolicy policy = policy(root);

        assertThrows(IOException.class, () -> policy.readSubmissionAttachment(submissionId,
                tempDir.resolve("outside.txt").toString()));
    }

    private ManagedStoragePathPolicy policy(Path root) {
        NavM2mProperties properties = new NavM2mProperties();
        properties.setStorageDirectory(root.toString());
        return new ManagedStoragePathPolicy(properties);
    }
}
