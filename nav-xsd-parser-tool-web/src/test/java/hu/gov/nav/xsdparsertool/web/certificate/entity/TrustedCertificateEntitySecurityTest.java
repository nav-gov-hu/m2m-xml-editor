package hu.gov.nav.xsdparsertool.web.certificate.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class TrustedCertificateEntitySecurityTest {

    @Test
    void certificateBytesAreDefensivelyCopiedOnSetAndGet() {
        TrustedCertificateEntity entity = new TrustedCertificateEntity();
        byte[] source = {1, 2, 3};

        entity.setCertificateDer(source);
        source[0] = 9;

        byte[] firstRead = entity.getCertificateDer();
        assertArrayEquals(new byte[]{1, 2, 3}, firstRead);
        assertNotSame(source, firstRead);

        firstRead[1] = 8;
        byte[] secondRead = entity.getCertificateDer();
        assertArrayEquals(new byte[]{1, 2, 3}, secondRead);
        assertNotSame(firstRead, secondRead);
    }
}
