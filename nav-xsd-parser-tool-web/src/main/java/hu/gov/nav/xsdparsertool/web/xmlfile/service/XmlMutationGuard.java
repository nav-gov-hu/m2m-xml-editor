package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.nav.m2m.submitter.domain.SubmissionStatus;
import hu.nav.m2m.submitter.repo.M2mSubmissionRepository;
import org.springframework.stereotype.Service;

/**
 * A műveletek végrehajthatóságát és a kapcsolódó invariánsokat ellenőrző védelmi komponens.
 *
 * <p>A {@code XmlMutationGuard} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class XmlMutationGuard {
    private final M2mSubmissionRepository submissions;

    /**
     * Létrehozza a {@code XmlMutationGuard} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param submissions a művelet bemeneti {@code submissions} értéke
     */
    public XmlMutationGuard(M2mSubmissionRepository submissions) {
        this.submissions = submissions;
    }

    /**
     * A {@code isFinal} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean isFinal(Long xmlFileId) {
        return submissions.existsByXmlFileIdAndInternalStatus(xmlFileId, SubmissionStatus.SUBMITTED_OK);
    }

    /**
     * A {@code requireMutable} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     */
    public void requireMutable(Long xmlFileId) {
        if (isFinal(xmlFileId)) {
            throw new IllegalStateException("A sikeresen beküldött űrlap végállapotban van, ezért csak megtekinthető.");
        }
    }
}
