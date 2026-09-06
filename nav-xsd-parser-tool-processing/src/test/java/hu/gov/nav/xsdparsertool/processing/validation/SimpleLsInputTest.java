package hu.gov.nav.xsdparsertool.processing.validation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

class SimpleLsInputTest {

    @Test
    void exposesMutableCoreLsInputPropertiesAndFixedDefaults() {
        ByteArrayInputStream first = new ByteArrayInputStream(new byte[]{1});
        SimpleLsInput input = new SimpleLsInput("public", "system", first);

        assertEquals("public", input.getPublicId());
        assertEquals("system", input.getSystemId());
        assertSame(first, input.getByteStream());
        assertEquals("UTF-8", input.getEncoding());
        assertNull(input.getCharacterStream());
        assertNull(input.getStringData());
        assertNull(input.getBaseURI());
        assertFalse(input.getCertifiedText());

        ByteArrayInputStream second = new ByteArrayInputStream(new byte[]{2});
        input.setPublicId("p2");
        input.setSystemId("s2");
        input.setByteStream(second);
        input.setCharacterStream(null);
        input.setStringData("ignored");
        input.setBaseURI("ignored");
        input.setEncoding("ISO-8859-2");
        input.setCertifiedText(true);

        assertEquals("p2", input.getPublicId());
        assertEquals("s2", input.getSystemId());
        assertSame(second, input.getByteStream());
        assertEquals("UTF-8", input.getEncoding());
        assertFalse(input.getCertifiedText());
    }
}
