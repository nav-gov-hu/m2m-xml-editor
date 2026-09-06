package hu.gov.nav.xsdparsertool.web.secret.repository;
import hu.gov.nav.xsdparsertool.web.secret.entity.SystemSecretEntity;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * A perzisztens adatok elérését biztosító repository szerződés.
 *
 * <p>A {@code SystemSecretRepository} interfész a web modul titokkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public interface SystemSecretRepository extends JpaRepository<SystemSecretEntity,String> {}
