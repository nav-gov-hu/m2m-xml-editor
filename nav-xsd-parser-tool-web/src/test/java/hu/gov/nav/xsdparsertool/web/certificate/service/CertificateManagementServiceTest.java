package hu.gov.nav.xsdparsertool.web.certificate.service;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.certificate.entity.TrustedCertificateEntity;
import hu.gov.nav.xsdparsertool.web.certificate.repository.TrustedCertificateRepository;
import hu.gov.nav.xsdparsertool.web.network.SystemTableM2mProxySettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.x500.X500Principal;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateManagementServiceTest {

    @Mock
    private TrustedCertificateRepository repository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private TrustedCertificateSslContextInitializer sslContextInitializer;
    @Mock
    private SystemTableM2mProxySettingsService proxySettingsService;
    @Mock
    private X509Certificate certificate;

    @Test
    void certificateAliasAndSourceMetadataMustBePresentOnInitialInsert() throws Exception {
        CertificateManagementService service = new CertificateManagementService(
                repository,
                auditLogService,
                sslContextInitializer,
                proxySettingsService);
        prepareCertificate();
        when(repository.findBySha256Fingerprint(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(TrustedCertificateEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        invokeStoreWithMetadata(service, "m2m-dev.nav.gov.hu-1", "m2m-dev.nav.gov.hu", 443);

        ArgumentCaptor<TrustedCertificateEntity> captor = ArgumentCaptor.forClass(TrustedCertificateEntity.class);
        verify(repository).save(captor.capture());
        TrustedCertificateEntity saved = captor.getValue();
        assertEquals("m2m-dev.nav.gov.hu-1", saved.getAlias());
        assertEquals("m2m-dev.nav.gov.hu", saved.getSourceHost());
        assertEquals(443, saved.getSourcePort());
    }

    @Test
    void missingAliasMustReceiveDeterministicFallbackBeforeInsert() throws Exception {
        CertificateManagementService service = new CertificateManagementService(
                repository,
                auditLogService,
                sslContextInitializer,
                proxySettingsService);
        prepareCertificate();
        when(repository.findBySha256Fingerprint(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(TrustedCertificateEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        invokeStoreWithMetadata(service, null, null, null);

        ArgumentCaptor<TrustedCertificateEntity> captor = ArgumentCaptor.forClass(TrustedCertificateEntity.class);
        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue().getAlias());
        assertTrue(captor.getValue().getAlias().startsWith("certificate-"));
    }

    private void prepareCertificate() throws Exception {
        when(certificate.getEncoded()).thenReturn(new byte[]{1, 2, 3, 4});
        when(certificate.getSubjectX500Principal()).thenReturn(new X500Principal("CN=Test Certificate"));
        when(certificate.getIssuerX500Principal()).thenReturn(new X500Principal("CN=Test Issuer"));
        when(certificate.getSerialNumber()).thenReturn(BigInteger.ONE);
        when(certificate.getNotBefore()).thenReturn(Date.from(Instant.now().minusSeconds(3600)));
        when(certificate.getNotAfter()).thenReturn(Date.from(Instant.now().plusSeconds(86400)));
    }

    private void invokeStoreWithMetadata(
            CertificateManagementService service,
            String alias,
            String host,
            Integer port) throws Exception {
        Method method = CertificateManagementService.class.getDeclaredMethod(
                "storeWithMetadata",
                X509Certificate.class,
                String.class,
                String.class,
                Integer.class,
                String.class);
        method.setAccessible(true);
        method.invoke(service, certificate, alias, host, port, "tester");
    }
}
