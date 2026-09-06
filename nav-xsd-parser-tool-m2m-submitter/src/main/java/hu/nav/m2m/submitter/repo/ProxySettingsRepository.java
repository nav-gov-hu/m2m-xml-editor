package hu.nav.m2m.submitter.repo;

import hu.nav.m2m.submitter.domain.ProxySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/**
 * Spring Data repository az egyetlen aktív M2M proxy/TLS konfiguráció perzisztens kezeléséhez.
 */
public interface ProxySettingsRepository extends JpaRepository<ProxySettings, Long> {
    /**
     * Atomi update művelettel frissíti az egyetlen, 1-es azonosítójú proxy/TLS konfigurációt.
     *
     * @return a módosított rekordok száma; normál esetben {@code 1}
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ProxySettings p
               set p.enabled = :enabled,
                   p.proxyUrl = :proxyUrl,
                   p.proxyPort = :proxyPort,
                   p.username = :username,
                   p.password = :password,
                   p.sslVerificationDisabled = :sslVerificationDisabled,
                   p.trustStorePath = :trustStorePath,
                   p.trustStorePassword = :trustStorePassword,
                   p.trustStoreType = :trustStoreType,
                   p.updatedAt = :updatedAt
             where p.id = 1
            """)
    int updateSingleton(@Param("enabled") boolean enabled,
                        @Param("proxyUrl") String proxyUrl,
                        @Param("proxyPort") Integer proxyPort,
                        @Param("username") String username,
                        @Param("password") String password,
                        @Param("sslVerificationDisabled") boolean sslVerificationDisabled,
                        @Param("trustStorePath") String trustStorePath,
                        @Param("trustStorePassword") String trustStorePassword,
                        @Param("trustStoreType") String trustStoreType,
                        @Param("updatedAt") Instant updatedAt);
}
