package hu.gov.nav.xsdparsertool.web.githubupdater.repo;

import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository a GitHub Űrlapsablon release-katalógusának kezeléséhez.
 */
public interface GitHubTemplateReleaseRepository extends JpaRepository<GitHubTemplateRelease, Long> {
    /**
     * A Spring Data lekérdezési névkonvenciója alapján visszaadja a feltételnek megfelelő perzisztált rekordokat.
     *
     * @param repositoryName a GitHub repository neve
     * @return a művelet eredménye
     */
    List<GitHubTemplateRelease> findByRepositoryNameOrderByReleaseTagAsc(String repositoryName);

    /**
     * Törli a megadott feltételhez tartozó perzisztált rekordokat, és visszaadja az érintett sorok számát.
     *
     * @param repositoryName a GitHub repository neve
     * @return a művelet eredménye
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from GitHubTemplateRelease release where release.repositoryName = :repositoryName")
    int deleteByRepositoryName(@Param("repositoryName") String repositoryName);
}
