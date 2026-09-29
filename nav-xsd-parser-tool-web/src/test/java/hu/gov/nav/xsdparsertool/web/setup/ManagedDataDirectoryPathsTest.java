package hu.gov.nav.xsdparsertool.web.setup;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ManagedDataDirectoryPathsTest {

    @Test
    void rebasesOnlyPathBelowLegacyDataDirectory(@TempDir Path tempDir) {
        Path legacy = tempDir.resolve("4.0.0-OLD");
        Path current = tempDir.resolve("4.1.0-NEW");
        String legacyValue = legacy.resolve("repo/xsd").toString();

        String rebased = ManagedDataDirectoryPaths.rebaseIfUnderLegacyRoot(
                legacyValue, legacy.toString(), current.toString());

        assertThat(Path.of(rebased)).isEqualTo(current.resolve("repo/xsd").toAbsolutePath().normalize());
    }

    @Test
    void keepsExternalPathUnchanged(@TempDir Path tempDir) {
        Path legacy = tempDir.resolve("4.0.0-OLD");
        Path current = tempDir.resolve("4.1.0-NEW");
        Path external = tempDir.resolve("shared/xsd").toAbsolutePath().normalize();

        String rebased = ManagedDataDirectoryPaths.rebaseIfUnderLegacyRoot(
                external.toString(), legacy.toString(), current.toString());

        assertThat(rebased).isEqualTo(external.toString());
    }

    @Test
    void convertsManagedPathToPortableValue(@TempDir Path tempDir) {
        assertThat(ManagedDataDirectoryPaths.toPortableValue(tempDir, tempDir.resolve("data/xml")))
                .isEqualTo("${app.data.dir}/data/xml");
    }
}
