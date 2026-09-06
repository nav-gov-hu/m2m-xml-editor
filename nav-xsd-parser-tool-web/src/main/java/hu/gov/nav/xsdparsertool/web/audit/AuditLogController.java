package hu.gov.nav.xsdparsertool.web.audit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hu.gov.nav.xsdparsertool.core.security.AuthorizationRules;

/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code AuditLogController} osztály a web modul auditnaplózási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController
@RequestMapping("/api/admin/audit-log")
@PreAuthorize(AuthorizationRules.ADMIN_ONLY)
public class AuditLogController {
    private final JdbcTemplate jdbc;
    /**
     * Létrehozza a {@code AuditLogController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param jdbc a művelet bemeneti {@code jdbc} értéke
     */
    public AuditLogController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /**
     * A {@code list} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a auditnaplózási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param limit a lapozási vagy mennyiségi korlátot meghatározó érték
     * @return a művelet eredményeként előállított elemek listája
     */
    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "300") int limit) {
        int safe = Math.max(1, Math.min(limit, 2000));
        try {
            return jdbc.query(connection -> {
                var statement = connection.prepareStatement("select id, operation_type, username, result, message, created_at from operation_audit_log order by created_at desc");
                statement.setMaxRows(safe);
                return statement;
            }, (resultSet, rowNum) -> row(resultSet));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Az auditnapló lekérdezése sikertelen.", ex);
        }
    }

    /**
     * A {@code row} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a auditnaplózási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param rs a művelet bemeneti {@code rs} értéke
     * @return a feldolgozás során felépített kulcs-érték leképezés
     * @throws SQLException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private Map<String, Object> row(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getLong("id"));
        row.put("operationType", rs.getString("operation_type"));
        row.put("username", rs.getString("username"));
        row.put("result", rs.getString("result"));
        row.put("message", rs.getString("message"));
        row.put("createdAt", rs.getTimestamp("created_at"));
        return row;
    }
}
