package hu.nav.m2m.submitter.service;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import hu.nav.m2m.submitter.domain.M2mAttachment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AttachmentLifecycleEvaluatorTest {

    @TempDir
    Path tempDir;

    @Test
    void attachmentWithoutNavFileIdCanBeRefreshedWhenLocalFileExists() throws IOException {
        Instant now = Instant.parse("2026-08-07T12:00:00Z");
        M2mAttachment attachment = attachmentWithLocalFile();

        AttachmentLifecycleEvaluator.Evaluation evaluation =
                AttachmentLifecycleEvaluator.evaluate(attachment, properties(), now);

        assertEquals(AttachmentLifecycleEvaluator.State.NOT_UPLOADED, evaluation.state());
        assertTrue(evaluation.refreshAllowed());
        assertTrue(evaluation.localFileAvailable());
    }

    @Test
    void expiredAttachmentCanBeRefreshedWhenLocalFileExists() throws IOException {
        Instant now = Instant.parse("2026-08-07T12:00:00Z");
        M2mAttachment attachment = uploadedAttachment(now.minus(Duration.ofDays(4)), now.minusSeconds(1));

        AttachmentLifecycleEvaluator.Evaluation evaluation =
                AttachmentLifecycleEvaluator.evaluate(attachment, properties(), now);

        assertEquals(AttachmentLifecycleEvaluator.State.EXPIRED, evaluation.state());
        assertTrue(evaluation.refreshAllowed());
    }

    @Test
    void attachmentInsideSafetyMarginIsExpiringSoonAndRefreshable() throws IOException {
        Instant now = Instant.parse("2026-08-07T12:00:00Z");
        M2mAttachment attachment = uploadedAttachment(now.minus(Duration.ofDays(1)), now.plus(Duration.ofHours(1)));

        AttachmentLifecycleEvaluator.Evaluation evaluation =
                AttachmentLifecycleEvaluator.evaluate(attachment, properties(), now);

        assertEquals(AttachmentLifecycleEvaluator.State.EXPIRING_SOON, evaluation.state());
        assertTrue(evaluation.refreshAllowed());
    }

    @Test
    void validAttachmentOutsideSafetyMarginCannotBeRefreshed() throws IOException {
        Instant now = Instant.parse("2026-08-07T12:00:00Z");
        M2mAttachment attachment = uploadedAttachment(now.minus(Duration.ofHours(1)), now.plus(Duration.ofDays(2)));

        AttachmentLifecycleEvaluator.Evaluation evaluation =
                AttachmentLifecycleEvaluator.evaluate(attachment, properties(), now);

        assertEquals(AttachmentLifecycleEvaluator.State.VALID, evaluation.state());
        assertFalse(evaluation.refreshAllowed());
        assertTrue(evaluation.localFileAvailable());
    }

    @Test
    void missingLocalFilePreventsRefreshEvenWhenAttachmentExpired() {
        Instant now = Instant.parse("2026-08-07T12:00:00Z");
        M2mAttachment attachment = new M2mAttachment();
        attachment.setStoragePath(tempDir.resolve("missing.bin").toString());
        attachment.setNavFileId("NAV-1");
        attachment.setNavUploadedAt(now.minus(Duration.ofDays(4)));
        attachment.setNavExpiresAt(now.minusSeconds(1));

        AttachmentLifecycleEvaluator.Evaluation evaluation =
                AttachmentLifecycleEvaluator.evaluate(attachment, properties(), now);

        assertEquals(AttachmentLifecycleEvaluator.State.EXPIRED, evaluation.state());
        assertFalse(evaluation.refreshAllowed());
        assertFalse(evaluation.localFileAvailable());
    }

    private M2mAttachment uploadedAttachment(Instant uploadedAt, Instant expiresAt) throws IOException {
        M2mAttachment attachment = attachmentWithLocalFile();
        attachment.setNavFileId("NAV-1");
        attachment.setNavUploadedAt(uploadedAt);
        attachment.setNavExpiresAt(expiresAt);
        return attachment;
    }

    private M2mAttachment attachmentWithLocalFile() throws IOException {
        Path file = tempDir.resolve("attachment.bin");
        Files.writeString(file, "attachment");
        M2mAttachment attachment = new M2mAttachment();
        attachment.setStoragePath(file.toString());
        return attachment;
    }

    private NavM2mProperties properties() {
        NavM2mProperties properties = new NavM2mProperties();
        properties.getAttachment().setExpirySafetyMargin(Duration.ofHours(2));
        properties.getAttachment().setValidityDuration(Duration.ofDays(3));
        return properties;
    }
}
