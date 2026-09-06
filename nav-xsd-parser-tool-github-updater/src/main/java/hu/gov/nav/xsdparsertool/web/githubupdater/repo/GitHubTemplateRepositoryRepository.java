package hu.gov.nav.xsdparsertool.web.githubupdater.repo;

import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository a GitHub Űrlapsablon repository-katalógusának kezeléséhez.
 */
public interface GitHubTemplateRepositoryRepository extends JpaRepository<GitHubTemplateRepository, String> { }
