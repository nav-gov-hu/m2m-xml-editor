package hu.gov.nav.xsdparsertool.web.certificate.repository;

import hu.gov.nav.xsdparsertool.web.certificate.entity.TrustedCertificateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code TrustedCertificateRepository} interfész a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface TrustedCertificateRepository extends JpaRepository<TrustedCertificateEntity,Long> {
    /**
     * A {@code findBySha256Fingerprint} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param fingerprint a művelet bemeneti {@code fingerprint} értéke
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    Optional<TrustedCertificateEntity> findBySha256Fingerprint(String fingerprint);

    /**
     * A {@code updateMetadata} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param id a célobjektum vagy erőforrás azonosítója
     * @param alias a művelet bemeneti {@code alias} értéke
     * @param sourceHost a művelet bemeneti {@code sourceHost} értéke
     * @param sourcePort a művelet bemeneti {@code sourcePort} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TrustedCertificateEntity c
               set c.alias = :alias,
                   c.sourceHost = :sourceHost,
                   c.sourcePort = :sourcePort
             where c.id = :id
            """)
    int updateMetadata(@Param("id") Long id,
                       @Param("alias") String alias,
                       @Param("sourceHost") String sourceHost,
                       @Param("sourcePort") Integer sourcePort);
}
