package hu.gov.nav.xsdparsertool.web.certificate.dto;
import java.time.Instant;
/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code CertificateDto} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record CertificateDto(Long id,String alias,String subjectDn,String issuerDn,String serialNumber,String sha256Fingerprint,Instant validFrom,Instant validUntil,String sourceHost,Integer sourcePort,String status,Instant createdAt,String createdBy){}
