package hu.gov.nav.xsdparsertool.web.certificate.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A webes rétegek közötti adatátadás strukturált modellje.
 *
 * <p>A {@code RemoteCertificateRequest} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public record RemoteCertificateRequest(
        @Pattern(regexp = "^[A-Za-z0-9.-]{1,253}$") String host,
        @Min(1) @Max(65535) Integer port,
        @Size(max = 128) @Pattern(regexp = "^[\\p{L}\\p{N}._@ +\\-]*$") String alias,
        Boolean importCertificate) {}
