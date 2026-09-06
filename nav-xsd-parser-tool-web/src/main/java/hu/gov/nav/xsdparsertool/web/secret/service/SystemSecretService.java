package hu.gov.nav.xsdparsertool.web.secret.service;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.web.secret.entity.SystemSecretEntity;
import hu.gov.nav.xsdparsertool.web.secret.repository.SystemSecretRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code SystemSecretService} osztály a web modul titokkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class SystemSecretService {
    private static final int VERSION=1; private static final SecureRandom RANDOM=new SecureRandom();
    private final SystemSecretRepository repository; private final MasterKeyService masterKeyService;
    /**
     * Létrehozza a {@code SystemSecretService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param repository a művelet bemeneti {@code repository} értéke
     * @param masterKeyService a művelet bemeneti {@code masterKeyService} értéke
     */
    public SystemSecretService(SystemSecretRepository repository,MasterKeyService masterKeyService){this.repository=repository;this.masterKeyService=masterKeyService;}
    /**
     * A {@code exists} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean exists(String key){return repository.existsById(key);}
    /**
     * A {@code read} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    public Optional<String> read(String key){return RepositoryAccess.findById(repository, key).map(e->decrypt(e.getEncryptedValue()));}
    /**
     * A {@code delete} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param key a művelet bemeneti {@code key} értéke
     */
    @Transactional public void delete(String key){repository.deleteById(key);}
    /**
     * A {@code save} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @param value a művelet bemeneti {@code value} értéke
     * @param username a művelet felhasználói kontextusa vagy felhasználóneve
     */
    @Transactional public void save(String key,String value,String username){SystemSecretEntity e=RepositoryAccess.findById(repository, key).orElseGet(SystemSecretEntity::new);e.setKey(key);e.setEncryptedValue(encrypt(value));e.setEncryptionVersion(VERSION);e.setUpdatedAt(Instant.now());e.setUpdatedBy(username);repository.save(e);}
    /**
     * A {@code encrypt} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String encrypt(String value){try{byte[] iv=new byte[12];RANDOM.nextBytes(iv);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,masterKeyService.getOrCreate(),new GCMParameterSpec(128,iv));byte[] encrypted=c.doFinal(value.getBytes(StandardCharsets.UTF_8));return Base64.getEncoder().encodeToString(iv)+"."+Base64.getEncoder().encodeToString(encrypted);}catch(Exception ex){throw new IllegalStateException("A titkos érték nem titkosítható.",ex);}}
    /**
     * A {@code decrypt} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a titokkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param stored a művelet bemeneti {@code stored} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String decrypt(String stored){try{String[] p=stored.split("\\.",2);byte[] iv=Base64.getDecoder().decode(p[0]);byte[] enc=Base64.getDecoder().decode(p[1]);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,masterKeyService.getOrCreate(),new GCMParameterSpec(128,iv));return new String(c.doFinal(enc),StandardCharsets.UTF_8);}catch(Exception ex){throw new IllegalStateException("A titkos érték nem fejthető vissza.",ex);}}
}
