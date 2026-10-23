package hu.gov.nav.xsdparsertool.web.database.releaselock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationReleaseIdTest {

    @Test
    void laterTimestampIsNewerWithinSameSemanticVersion() {
        ApplicationReleaseId oldRelease = ApplicationReleaseId.parse("4.0.0-20260921-123222-RC1");
        ApplicationReleaseId newRelease = ApplicationReleaseId.parse("4.0.0-20260926-160300-RC1");

        assertTrue(newRelease.compareTo(oldRelease) > 0);
    }

    @Test
    void laterRcIsNewerForSameBuildTimestamp() {
        ApplicationReleaseId rc1 = ApplicationReleaseId.parse("4.0.0-20260926-160300-RC1");
        ApplicationReleaseId rc2 = ApplicationReleaseId.parse("4.0.0-20260926-160300-RC2");

        assertTrue(rc2.compareTo(rc1) > 0);
    }

    @Test
    void finalReleaseIsNewerThanRcForSameBuildTimestamp() {
        ApplicationReleaseId rc = ApplicationReleaseId.parse("4.0.0-20260926-160300-RC2");
        ApplicationReleaseId finalRelease = ApplicationReleaseId.parse("4.0.0-20260926-160300");

        assertTrue(finalRelease.compareTo(rc) > 0);
    }

    @Test
    void unsupportedReleaseIsRejectedForAutomaticUpgradeComparison() {
        assertFalse(ApplicationReleaseId.isSupported("4.0.0-SNAPSHOT-20260926-160300"));
    }
}
