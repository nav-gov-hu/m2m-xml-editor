package hu.gov.nav.xsdparsertool.web.systemconfig.repository;

import hu.gov.nav.xsdparsertool.web.systemconfig.entity.SystemConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code SystemConfigurationRepository} interfész a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface SystemConfigurationRepository extends JpaRepository<SystemConfigurationEntity, String> {
    /**
     * A {@code updateTrustedKey} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @param value a művelet bemeneti {@code value} értéke
     * @param updatedAt a művelet bemeneti {@code updatedAt} értéke
     * @param updatedBy a művelet bemeneti {@code updatedBy} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SystemConfigurationEntity c
               set c.value = :value,
                   c.updatedAt = :updatedAt,
                   c.updatedBy = :updatedBy
             where c.key = :key
            """)
    int updateTrustedKey(@Param("key") String key,
                         @Param("value") String value,
                         @Param("updatedAt") Instant updatedAt,
                         @Param("updatedBy") String updatedBy);
}
