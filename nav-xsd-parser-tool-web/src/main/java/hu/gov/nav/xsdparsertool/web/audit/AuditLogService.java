package hu.gov.nav.xsdparsertool.web.audit;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code AuditLogService} osztály a web modul auditnaplózási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * Létrehozza a {@code AuditLogService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param jdbcTemplate a művelet bemeneti {@code jdbcTemplate} értéke
     */
    public AuditLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * A {@code log} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a auditnaplózási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param operationType a művelet bemeneti {@code operationType} értéke
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param result a művelet bemeneti {@code result} értéke
     * @param message a művelet bemeneti {@code message} értéke
     */
    public void log(String operationType, String username, String result, String message) {
        log(operationType, null, null, null, username, result, message, null);
    }

    /**
     * A {@code log} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a auditnaplózási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param operationType a művelet bemeneti {@code operationType} értéke
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param jobId a célobjektum vagy erőforrás azonosítója
     * @param revisionId a célobjektum vagy erőforrás azonosítója
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     * @param result a művelet bemeneti {@code result} értéke
     * @param message a művelet bemeneti {@code message} értéke
     * @param detailsJson a művelet bemeneti {@code detailsJson} értéke
     */
    public void log(String operationType, Long xmlFileId, String jobId, Long revisionId,
                    String username, String result, String message, String detailsJson) {
        try {
            jdbcTemplate.update("""
                    insert into operation_audit_log
                    (operation_type, xml_file_id, job_id, revision_id, username, result, message, details_json, created_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    operationType,
                    xmlFileId,
                    jobId,
                    revisionId,
                    username,
                    result,
                    message,
                    detailsJson,
                    Timestamp.valueOf(LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("Audit log bejegyzes mentese sikertelen. operationType={}, username={}, result={}",
                    operationType, username, result, e);
        }
    }
}
