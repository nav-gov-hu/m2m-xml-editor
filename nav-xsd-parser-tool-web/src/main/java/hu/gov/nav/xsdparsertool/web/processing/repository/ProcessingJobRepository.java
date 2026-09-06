package hu.gov.nav.xsdparsertool.web.processing.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.gov.nav.xsdparsertool.web.processing.entity.ProcessingJobEntity;

/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code ProcessingJobRepository} interfész a web modul feldolgozási job területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface ProcessingJobRepository extends JpaRepository<ProcessingJobEntity, Long> {
    /**
     * A {@code findByJobId} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    Optional<ProcessingJobEntity> findByJobId(String jobId);
    /**
     * A {@code findFirstByStatusInOrderByCreatedAtAsc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param statuses a feldolgozás aktuális vagy beállítandó állapota
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    Optional<ProcessingJobEntity> findFirstByStatusInOrderByCreatedAtAsc(Collection<String> statuses);
    /**
     * A {@code findByStatusIn} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param statuses a feldolgozás aktuális vagy beállítandó állapota
     * @return a művelet eredményeként előállított elemek listája
     */
    List<ProcessingJobEntity> findByStatusIn(Collection<String> statuses);
    /**
     * A {@code findTop20ByOrderByCreatedAtDesc} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a feldolgozási job komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    List<ProcessingJobEntity> findTop20ByOrderByCreatedAtDesc();
}
