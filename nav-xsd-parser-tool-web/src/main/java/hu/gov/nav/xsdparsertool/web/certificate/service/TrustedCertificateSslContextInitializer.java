package hu.gov.nav.xsdparsertool.web.certificate.service;

import hu.gov.nav.xsdparsertool.web.certificate.repository.TrustedCertificateRepository;
import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A központi adatbázisos tanúsítványtárból JVM-szintű, összetett TLS trust contextet épít.
 *
 * <p>A létrehozott kontextus a platform alapértelmezett CA-kat és az alkalmazásban importált,
 * aktív X.509 tanúsítványokat egyszerre fogadja el. Az importált tanúsítványok módosítása után
 * a {@link #reload()} művelettel futás közben is újraépíthető.</p>
 */
@Component
public class TrustedCertificateSslContextInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TrustedCertificateSslContextInitializer.class);

    private final TrustedCertificateRepository repository;
    private final Environment environment;
    private final SSLContext platformDefaultContext;

    /**
     * Létrehozza a tanúsítványtár TLS inicializálóját és megőrzi a JVM eredeti alapértelmezett
     * SSL kontextusát arra az esetre, ha az alkalmazásban nincs aktív egyedi tanúsítvány.
     *
     * @param repository a megbízható tanúsítványok repository-ja
     * @param environment az alkalmazás környezeti konfigurációja
     */
    public TrustedCertificateSslContextInitializer(TrustedCertificateRepository repository, Environment environment) {
        this.repository = repository;
        this.environment = environment;
        this.platformDefaultContext = currentDefaultContext();
    }

    /**
     * Alkalmazásindításkor aktiválja a központi tanúsítványtárat.
     *
     * @param args alkalmazásindítási argumentumok
     */
    @Override
    public void run(ApplicationArguments args) {
        reload();
    }

    /**
     * Újraépíti és azonnal aktiválja a JVM alapértelmezett TLS trust contextjét.
     *
     * <p>Ha a központi TLS-validáció ki van kapcsolva vagy nincs aktív importált tanúsítvány,
     * visszaállítja a JVM induláskori platform trust contextjét. Egyébként a platform CA-k és
     * az adatbázisos tanúsítványok összetett trust managerét telepíti.</p>
     */
    public synchronized void reload() {
        if (!environment.getProperty("nav.xsdparsertool.tls.validation-enabled", Boolean.class, true)) {
            install(platformDefaultContext);
            log.info("Adatbázisos tanúsítványtár nincs aktiválva: TLS-validáció konfiguráció szerint kikapcsolva.");
            return;
        }
        try {
            var entries = RepositoryAccess.findAll(repository);
            KeyStore custom = KeyStore.getInstance(KeyStore.getDefaultType());
            custom.load(null, null);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            int count = 0;
            for (var entry : entries) {
                if ("EXPIRED".equals(entry.getStatus()) || "DISABLED".equals(entry.getStatus())) {
                    continue;
                }
                X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(
                        new ByteArrayInputStream(entry.getCertificateDer()));
                custom.setCertificateEntry("m2m-xml-editor-" + entry.getId(), certificate);
                count++;
            }
            if (count == 0) {
                install(platformDefaultContext);
                log.info("Adatbázisos tanúsítványtár üres; a JVM platform truststore aktív.");
                return;
            }

            TrustManagerFactory customFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            customFactory.init(custom);
            TrustManagerFactory defaultFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            defaultFactory.init((KeyStore) null);
            X509TrustManager customTrustManager = find(customFactory.getTrustManagers());
            X509TrustManager defaultTrustManager = find(defaultFactory.getTrustManagers());

            X509TrustManager composite = new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    var all = new ArrayList<X509Certificate>();
                    all.addAll(Arrays.asList(defaultTrustManager.getAcceptedIssuers()));
                    all.addAll(Arrays.asList(customTrustManager.getAcceptedIssuers()));
                    return all.toArray(X509Certificate[]::new);
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws java.security.cert.CertificateException {
                    defaultTrustManager.checkClientTrusted(chain, authType);
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws java.security.cert.CertificateException {
                    try {
                        defaultTrustManager.checkServerTrusted(chain, authType);
                    } catch (java.security.cert.CertificateException ex) {
                        customTrustManager.checkServerTrusted(chain, authType);
                    }
                }
            };

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{composite}, new SecureRandom());
            install(context);
            log.info("Adatbázisos tanúsítványtár aktiválva. certificateCount={}", count);
        } catch (Exception ex) {
            log.error("Az adatbázisos tanúsítványtár nem aktiválható.", ex);
        }
    }

    private void install(SSLContext context) {
        SSLContext.setDefault(context);
        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
    }

    private SSLContext currentDefaultContext() {
        try {
            return SSLContext.getDefault();
        } catch (Exception ex) {
            throw new IllegalStateException("A JVM alapértelmezett SSLContext nem érhető el.", ex);
        }
    }

    private X509TrustManager find(TrustManager[] managers) {
        for (TrustManager manager : managers) {
            if (manager instanceof X509TrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }
        throw new IllegalStateException("X509TrustManager nem található.");
    }
}
