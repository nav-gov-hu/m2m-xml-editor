package hu.gov.nav.xsdparsertool.web.githubupdater.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

/** Central exception boundary for Spring Data repository read operations. */
public final class RepositoryAccess {

    /**
     * Létrehozza a(z) {@code RepositoryAccess} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     */
    private RepositoryAccess() {
    }

    /**
     * A Spring Data {@code findById} hívást egységes kivételburkolással hajtja végre, hogy az infrastruktúrahiba alkalmazási kontextussal kerüljön tovább.
     *
     * @param repository a GitHub repository neve
     * @param id a művelethez átadott {@code id} érték
     * @return a művelet eredménye
     */
    public static <T, ID> Optional<T> findById(CrudRepository<T, ID> repository, ID id) {
        try {
            return repository.findById(id);
        } catch (RuntimeException ex) {
            throw repositoryFailure("findById", ex);
        }
    }

    /**
     * A Spring Data {@code findAll} eredményét stabil {@link java.util.List} formára másolja, és az esetleges repository hibát egységes kivétellé alakítja.
     *
     * @param repository a GitHub repository neve
     * @return a művelet eredménye
     */
    public static <T> List<T> findAll(CrudRepository<T, ?> repository) {
        try {
            List<T> result = new ArrayList<>();
            repository.findAll().forEach(result::add);
            return result;
        } catch (RuntimeException ex) {
            throw repositoryFailure("findAll", ex);
        }
    }

    /**
     * Repository műveleti hibából kontextust tartalmazó {@link IllegalStateException} példányt készít az eredeti kivétel megőrzésével.
     *
     * @param operation a diagnosztikában szereplő GitHub művelet neve
     * @param cause a művelethez átadott {@code cause} érték
     * @return a művelet eredménye
     */
    private static IllegalStateException repositoryFailure(String operation, RuntimeException cause) {
        return new IllegalStateException("Adatbázis-művelet sikertelen: " + operation, cause);
    }
}
