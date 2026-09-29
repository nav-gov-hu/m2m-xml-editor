package hu.gov.nav.xsdparsertool.web.database.releaselock;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Az adatbázist egyetlen teljes alkalmazás-release használatához köti.
 *
 * <p>Az indulás elején csak kompatibilitási ellenőrzés történik. Az adatbázis
 * tényleges birtokbavétele kizárólag a többi startup runner sikeres lefutása
 * után történik. Így egy későbbi indulási hiba nem állítja át idő előtt a
 * release-lockot.</p>
 */
@Service
public class ApplicationReleaseLockService {

    static final int LOCK_ID = 1;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Létrehozza a release-lock szolgáltatást.
     *
     * @param jdbcTemplate adatbázis-elérési segéd
     */
    public ApplicationReleaseLockService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Ellenőrzi, hogy az aktuális release használhatja-e az adatbázist.
     *
     * <p>Üres lock, azonos release vagy bizonyíthatóan újabb aktuális release
     * esetén az indulás folytatódhat. Régebbi vagy nem összehasonlítható eltérő
     * release esetén az alkalmazás fail-fast módon leáll.</p>
     *
     * @param currentRelease az aktuális alkalmazás teljes release azonosítója
     */
    @Transactional(readOnly = true)
    public void validateBeforeStartup(String currentRelease) {
        String current = requireRelease(currentRelease);
        String databaseRelease = loadRelease(false);
        validateCompatibility(databaseRelease, current);
    }

    /**
     * A sikeres startup-folyamat végén az aktuális release-hez rendeli az adatbázist.
     *
     * <p>A rekord sorzárral kerül beolvasásra, ezért párhuzamosan induló eltérő
     * verziók közül egy régebbi release nem tudja felülírni egy közben már újabb
     * release által megszerzett lockot.</p>
     *
     * @param currentRelease az aktuális alkalmazás teljes release azonosítója
     */
    @Transactional
    public void claimAfterSuccessfulStartup(String currentRelease) {
        String current = requireRelease(currentRelease);
        String databaseRelease = loadRelease(true);
        validateCompatibility(databaseRelease, current);

        if (!StringUtils.hasText(databaseRelease)
                || !databaseRelease.trim().equalsIgnoreCase(current)) {
            updateLock(current);
        }
    }

    private void validateCompatibility(String databaseRelease, String currentRelease) {
        if (!StringUtils.hasText(databaseRelease)) {
            return;
        }

        String locked = databaseRelease.trim();
        if (locked.equalsIgnoreCase(currentRelease)) {
            return;
        }

        if (canUpgrade(locked, currentRelease)) {
            return;
        }

        throw new IllegalStateException(
                "Az adatbázis másik M2M XML Editor release-hez van rendelve. "
                        + "Adatbázis release: " + locked
                        + ", aktuális alkalmazás release: " + currentRelease
                        + ". Az alkalmazás indítása leállt az adatbázis védelme érdekében.");
    }

    private String loadRelease(boolean lockRow) {
        String sql = "select application_release from application_release_lock where lock_id = ?"
                + (lockRow ? " for update" : "");
        List<String> values = jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> nullableString(resultSet, "application_release"),
                LOCK_ID);
        if (values.size() != 1) {
            throw new IllegalStateException(
                    "Az application_release_lock tábla nem tartalmazza az egyetlen kötelező lock rekordot (lock_id=1).");
        }
        return values.get(0);
    }

    private String nullableString(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getString(column);
    }

    private boolean canUpgrade(String lockedRelease, String currentRelease) {
        if (!ApplicationReleaseId.isSupported(lockedRelease)
                || !ApplicationReleaseId.isSupported(currentRelease)) {
            return false;
        }
        ApplicationReleaseId locked = ApplicationReleaseId.parse(lockedRelease);
        ApplicationReleaseId current = ApplicationReleaseId.parse(currentRelease);
        return current.compareTo(locked) > 0;
    }

    private String requireRelease(String release) {
        if (!StringUtils.hasText(release) || release.contains("@app.full.release@")) {
            throw new IllegalStateException(
                    "Az alkalmazás teljes release azonosítója nem érhető el; az adatbázis release-lock nem ellenőrizhető.");
        }
        return release.trim();
    }

    private void updateLock(String release) {
        int updated = jdbcTemplate.update(
                "update application_release_lock set application_release = ?, updated_at = current_timestamp, updated_by = ? where lock_id = ?",
                release,
                "application-startup",
                LOCK_ID);
        if (updated != 1) {
            throw new IllegalStateException("Az adatbázis release-lock frissítése sikertelen.");
        }
    }
}
