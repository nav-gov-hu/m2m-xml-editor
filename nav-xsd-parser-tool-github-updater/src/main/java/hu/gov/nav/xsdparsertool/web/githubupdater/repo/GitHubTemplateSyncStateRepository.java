package hu.gov.nav.xsdparsertool.web.githubupdater.repo;

import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository a GitHub katalógus szinkronizációs állapotának kezeléséhez.
 */
public interface GitHubTemplateSyncStateRepository extends JpaRepository<GitHubTemplateSyncState, Long> { }
