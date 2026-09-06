package hu.gov.nav.xsdparsertool.processing.validation;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.ls.LSInput;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MultiPathResourceResolverTest {

    @TempDir Path tempDir;

    @Test
    void blankOrMissingSystemIdReturnsNull() {
        MultiPathResourceResolver resolver = new MultiPathResourceResolver(tempDir, null);
        assertNull(resolver.resolveResource(null, null, null, null, null));
        assertNull(resolver.resolveResource(null, null, null, "   ", null));
        assertNull(resolver.resolveResource(null, null, null, "missing.xsd", null));
    }

    @Test
    void resolvesAbsoluteFileUriAndPreservesPublicId() throws Exception {
        Path xsd = Files.writeString(tempDir.resolve("absolute.xsd"), "<xsd/>");
        LSInput input = new MultiPathResourceResolver(null, null)
                .resolveResource(null, null, "PUBLIC", xsd.toUri().toString(), null);

        assertNotNull(input);
        assertEquals("PUBLIC", input.getPublicId());
        assertEquals(xsd.toUri().toString(), input.getSystemId());
        assertEquals("<xsd/>", new String(input.getByteStream().readAllBytes(), StandardCharsets.UTF_8));
        input.getByteStream().close();
    }

    @Test
    void resolvesRelativeSystemIdAgainstFileBaseUri() throws Exception {
        Path baseDir = Files.createDirectory(tempDir.resolve("base"));
        Path xsd = Files.writeString(baseDir.resolve("included.xsd"), "<included/>");
        String baseUri = baseDir.resolve("main.xsd").toUri().toString();

        LSInput input = new MultiPathResourceResolver(null, null)
                .resolveResource(null, null, null, "included.xsd", baseUri);

        assertNotNull(input);
        assertEquals(xsd.toUri().toString(), input.getSystemId());
        input.getByteStream().close();
    }

    @Test
    void resolvesFromPrimaryBeforeGeneralAndWalksNestedDirectories() throws Exception {
        Path primary = Files.createDirectory(tempDir.resolve("primary"));
        Path general = Files.createDirectory(tempDir.resolve("general"));
        Files.writeString(primary.resolve("same.xsd"), "primary");
        Files.writeString(general.resolve("same.xsd"), "general");
        Path nested = ExceptionSafeOperations.createDirectories(primary.resolve("a/b"));
        Path nestedXsd = Files.writeString(nested.resolve("deep.xsd"), "deep");
        MultiPathResourceResolver resolver = new MultiPathResourceResolver(primary, general);

        LSInput same = resolver.resolveResource(null, null, null, "same.xsd", null);
        assertNotNull(same);
        assertEquals("primary", new String(same.getByteStream().readAllBytes(), StandardCharsets.UTF_8));
        same.getByteStream().close();

        LSInput deep = resolver.resolveResource(null, null, null, "foreign/path/deep.xsd", null);
        assertNotNull(deep);
        assertEquals(nestedXsd.toUri().toString(), deep.getSystemId());
        deep.getByteStream().close();
    }

    @Test
    void resolvesAbsolutePathAndGeneralRoot() throws Exception {
        Path absolute = Files.writeString(tempDir.resolve("absolute-path.xsd"), "absolute-path");
        LSInput absoluteInput = new MultiPathResourceResolver(null, null)
                .resolveResource(null, null, null, absolute.toString(), null);
        assertNotNull(absoluteInput);
        assertEquals("absolute-path", new String(absoluteInput.getByteStream().readAllBytes(), StandardCharsets.UTF_8));
        absoluteInput.getByteStream().close();

        Path general = Files.createDirectory(tempDir.resolve("general-direct"));
        Path generalXsd = Files.writeString(general.resolve("general.xsd"), "general");
        LSInput generalInput = new MultiPathResourceResolver(null, general.resolve("nested/.."))
                .resolveResource(null, null, null, "general.xsd", null);
        assertNotNull(generalInput);
        assertEquals(generalXsd.toUri().toString(), generalInput.getSystemId());
        generalInput.getByteStream().close();
    }

    @Test
    void generalWalkResolvesUriAndBackslashFileNamesCaseInsensitively() throws Exception {
        Path general = Files.createDirectory(tempDir.resolve("general-walk"));
        Path nested = ExceptionSafeOperations.createDirectories(general.resolve("nested/path"));
        Path xsd = Files.writeString(nested.resolve("Deep.XSD"), "deep-general");
        MultiPathResourceResolver resolver = new MultiPathResourceResolver(null, general);

        LSInput fromUri = resolver.resolveResource(null, null, null, "http://example.invalid/other/deep.xsd", null);
        assertNotNull(fromUri);
        assertEquals(xsd.toUri().toString(), fromUri.getSystemId());
        fromUri.getByteStream().close();

        LSInput fromBackslash = resolver.resolveResource(null, null, null, "foreign\\path\\deep.xsd", null);
        assertNotNull(fromBackslash);
        assertEquals(xsd.toUri().toString(), fromBackslash.getSystemId());
        fromBackslash.getByteStream().close();
    }

    @Test
    void existingDirectoryTargetIsReportedAsResolutionFailure() throws Exception {
        Path primary = Files.createDirectory(tempDir.resolve("primary-directory"));
        Files.createDirectory(primary.resolve("directory.xsd"));
        MultiPathResourceResolver resolver = new MultiPathResourceResolver(primary, null);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> resolver.resolveResource(null, null, null, "directory.xsd", null));

        assertTrue(error.getMessage().contains("directory.xsd"));
    }

}
