package hu.gov.nav.xsdparsertool.web.githubupdater.repo;
import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubProxySettings;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data JPA repository a GitHub proxybeállítások perzisztens eléréséhez.
 */
public interface GitHubProxySettingsRepository extends JpaRepository<GitHubProxySettings, Long> {}
