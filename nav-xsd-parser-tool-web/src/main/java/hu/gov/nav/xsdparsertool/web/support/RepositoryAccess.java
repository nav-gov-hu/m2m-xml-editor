package hu.gov.nav.xsdparsertool.web.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

/** Central exception boundary for Spring Data repository read operations. */
public final class RepositoryAccess {

    /**
     * Létrehozza a {@code RepositoryAccess} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     */
    private RepositoryAccess() {
    }

    /**
     * A {@code findById} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param repository a művelet bemeneti {@code repository} értéke
     * @param id a célobjektum vagy erőforrás azonosítója
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    public static <T, ID> Optional<T> findById(CrudRepository<T, ID> repository, ID id) {
        try {
            return repository.findById(id);
        } catch (RuntimeException ex) {
            throw repositoryFailure("findById", ex);
        }
    }

    /**
     * A {@code findAll} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param repository a művelet bemeneti {@code repository} értéke
     * @return a művelet eredményeként előállított elemek listája
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
     * A {@code repositoryFailure} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param operation a művelet bemeneti {@code operation} értéke
     * @param cause a művelet bemeneti {@code cause} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static IllegalStateException repositoryFailure(String operation, RuntimeException cause) {
        return new IllegalStateException("Adatbázis-művelet sikertelen: " + operation, cause);
    }
}
