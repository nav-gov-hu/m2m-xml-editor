package hu.nav.m2m.submitter.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

/** Central exception boundary for Spring Data repository read operations. */
public final class RepositoryAccess {

    /**
     * Létrehozza a(z) {@code RepositoryAccess} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     */
    private RepositoryAccess() {
    }

    /**
     * A Spring Data névkonvenciója alapján lekérdezi a feltételeknek megfelelő perzisztált rekordokat.
     *
     * @param repository a perzisztencia repository
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
     * A(z) {@code findAll} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param repository a perzisztencia repository
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
     * A(z) {@code repositoryFailure} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param operation a NAV vagy életciklus művelet neve
     * @param cause a művelethez átadott {@code cause} érték
     * @return a művelet eredménye
     */
    private static IllegalStateException repositoryFailure(String operation, RuntimeException cause) {
        return new IllegalStateException("Adatbázis-művelet sikertelen: " + operation, cause);
    }
}
