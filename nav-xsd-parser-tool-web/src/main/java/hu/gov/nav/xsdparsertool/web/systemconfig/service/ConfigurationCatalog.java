package hu.gov.nav.xsdparsertool.web.systemconfig.service;

import java.util.List;
import java.util.Set;

/**
 * A webes konfigurációs felület által kezelt alkalmazásbeállítások teljes katalógusa.
 * A BOOTSTRAP elemek az alkalmazáskörnyezet felépítése előtt szükségesek, a DATABASE
 * elemek a system_configuration táblából kerülnek Spring property source-ként betöltésre.
 */
public final class ConfigurationCatalog {
    /**
     * A web modul alkalmazási területének közös alkalmazási típusa.
     *
     * <p>A {@code Spec} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public record Spec(String key, String label, String description, String category, String storage,
                       String type, String defaultValue, boolean sensitive, boolean restartRequired,
                       boolean advanced, boolean required, List<String> options) {}
    /**
     * A {@code s} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param k a művelet bemeneti {@code k} értéke
     * @param l a művelet bemeneti {@code l} értéke
     * @param d a művelet bemeneti {@code d} értéke
     * @param c a művelet bemeneti {@code c} értéke
     * @param st a művelet bemeneti {@code st} értéke
     * @param t a művelet bemeneti {@code t} értéke
     * @param def a művelet bemeneti {@code def} értéke
     * @param sec a művelet bemeneti {@code sec} értéke
     * @param restart a művelet bemeneti {@code restart} értéke
     * @param adv a művelet bemeneti {@code adv} értéke
     * @param req a művelet bemeneti {@code req} értéke
     * @param opts a művelet bemeneti {@code opts} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static Spec s(String k,String l,String d,String c,String st,String t,String def,boolean sec,boolean restart,boolean adv,boolean req,String... opts){
        return new Spec(k,l,d,c,st,t,def,sec,restart,adv,req,List.of(opts));
    }
    /**
     * A {@code b} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param k a művelet bemeneti {@code k} értéke
     * @param l a művelet bemeneti {@code l} értéke
     * @param d a művelet bemeneti {@code d} értéke
     * @param c a művelet bemeneti {@code c} értéke
     * @param t a művelet bemeneti {@code t} értéke
     * @param def a művelet bemeneti {@code def} értéke
     * @param sec a művelet bemeneti {@code sec} értéke
     * @param adv a művelet bemeneti {@code adv} értéke
     * @param req a művelet bemeneti {@code req} értéke
     * @param opts a művelet bemeneti {@code opts} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static Spec b(String k,String l,String d,String c,String t,String def,boolean sec,boolean adv,boolean req,String... opts){
        return s(k,l,d,c,"BOOTSTRAP",t,def,sec,true,adv,req,opts);
    }
    /**
     * A {@code d} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param k a művelet bemeneti {@code k} értéke
     * @param l a művelet bemeneti {@code l} értéke
     * @param desc a művelet bemeneti {@code desc} értéke
     * @param c a művelet bemeneti {@code c} értéke
     * @param t a művelet bemeneti {@code t} értéke
     * @param def a művelet bemeneti {@code def} értéke
     * @param sec a művelet bemeneti {@code sec} értéke
     * @param adv a művelet bemeneti {@code adv} értéke
     * @param req a művelet bemeneti {@code req} értéke
     * @param opts a művelet bemeneti {@code opts} értéke
     * @return a művelet feldolgozási eredménye
     */
    private static Spec d(String k,String l,String desc,String c,String t,String def,boolean sec,boolean adv,boolean req,String... opts){
        return s(k,l,desc,c,"DATABASE",t,def,sec,true,adv,req,opts);
    }

    public static final List<Spec> ITEMS = List.of(
        b("server.port","HTTP port","Az alkalmazás HTTP portja.","ALKALMAZAS","NUMBER","8080",false,false,true),
        b("server.servlet.context-path","Context path","Az alkalmazás URL gyökérútvonala.","ALKALMAZAS","TEXT","/",false,true,false),
        b("server.servlet.session.timeout","Munkamenet időkorlát","A bejelentkezett munkamenet inaktivitási időkorlátja.","ALKALMAZAS","DURATION","30m",false,true,true),
        b("spring.servlet.multipart.max-file-size","Maximális fájlméret","Egy feltöltött állomány maximális mérete.","ALKALMAZAS","TEXT","512MB",false,true,true),
        b("spring.servlet.multipart.max-request-size","Maximális kérésméret","Egy multipart kérés teljes maximális mérete.","ALKALMAZAS","TEXT","512MB",false,true,true),
        b("nav.xsdparsertool.desktop.enabled","Desktop mód","Desktop alkalmazásintegráció engedélyezése.","ALKALMAZAS","BOOLEAN","false",false,true,false),
        b("nav.xsdparsertool.desktop.browser-open-enabled","Böngésző automatikus megnyitása","Induláskor nyissa meg a böngészőt.","ALKALMAZAS","BOOLEAN","true",false,true,false),
        b("nav.xsdparsertool.desktop.splash.enabled","Splash képernyő","Desktop indulóképernyő engedélyezése.","ALKALMAZAS","BOOLEAN","true",false,true,false),
        b("nav.xsdparsertool.desktop.tray-enabled","Tálcaikon","Desktop tálcaikon engedélyezése.","ALKALMAZAS","BOOLEAN","true",false,true,false),

        b("nav.xsdparsertool.database.type","Adatbázistípus","A támogatott adatbázis típusa.","ADATBAZIS","SELECT","H2",false,false,true,"H2","MYSQL","POSTGRESQL","ORACLE"),
        b("spring.datasource.url","JDBC URL","Az alkalmazás adatbázis-kapcsolati URL-je.","ADATBAZIS","TEXT","jdbc:h2:file:${app.data.dir}/database/schema-explorer;AUTO_SERVER=TRUE",false,false,true),
        b("spring.datasource.username","Adatbázis-felhasználó","Technikai adatbázis-felhasználó.","ADATBAZIS","TEXT","sa",false,false,true),
        b("spring.datasource.password","Adatbázis-jelszó","Az alkalmazás adatbázis-kapcsolatához használt jelszó. Csak a bootstrap konfigurációt módosítja; az adatbázis-felhasználó jelszavát az üzemeltetés kezeli.","ADATBAZIS","PASSWORD","",true,false,false),
        b("spring.datasource.driver-class-name","JDBC driver","Az adatbázishoz használt JDBC driver osztály.","ADATBAZIS","TEXT","org.h2.Driver",false,true,true),
        b("spring.jpa.database-platform","Hibernate dialect","A Hibernate által használt adatbázis-dialektus.","ADATBAZIS","TEXT","org.hibernate.dialect.H2Dialect",false,true,true),
        b("spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type","Hibernate Instant JDBC típus","Oracle esetén a Java Instant mezők JDBC TIMESTAMP leképezése; más adatbázisoknál üresen marad.","ADATBAZIS","TEXT","",false,true,false),
        b("spring.jpa.hibernate.ddl-auto","Hibernate DDL mód","A Hibernate séma-kezelési módja; produkcióban none javasolt.","ADATBAZIS","SELECT","none",false,true,true,"none","validate","update","create","create-drop"),
        b("spring.jpa.open-in-view","Open EntityManager in View","JPA session nyitva tartása a webes válaszig.","ADATBAZIS","BOOLEAN","false",false,true,true),
        b("spring.jpa.show-sql","SQL naplózás","Hibernate SQL parancsok naplózása.","ADATBAZIS","BOOLEAN","false",false,true,true),
        b("nav.xsdparsertool.database.schema","Adatbázisséma","Az alkalmazás adatbázissémája.","ADATBAZIS","TEXT","",false,true,false),
        b("nav.xsdparsertool.database.encoding","Adatbázis kódolása","A migrációknál és ellenőrzéseknél használt kódolás.","ADATBAZIS","TEXT","UTF-8",false,true,true),
        b("spring.flyway.enabled","Flyway migráció","Adatbázis-migrációk automatikus futtatása.","ADATBAZIS","BOOLEAN","true",false,true,true),
        b("spring.flyway.locations","Flyway migrációs hely","Az aktuális adatbázishoz tartozó migrációs könyvtár.","ADATBAZIS","TEXT","classpath:db/migration/H2",false,true,true),
        b("spring.flyway.baseline-on-migrate","Flyway baseline","Meglévő séma esetén baseline engedélyezése.","ADATBAZIS","BOOLEAN","true",false,true,true),
        b("spring.flyway.encoding","Flyway kódolás","A migrációs SQL állományok kódolása.","ADATBAZIS","TEXT","UTF-8",false,true,true),
        b("spring.h2.console.enabled","H2 konzol","A H2 webkonzol engedélyezése.","ADATBAZIS","BOOLEAN","true",false,true,false),
        b("spring.h2.console.path","H2 konzol útvonala","A H2 webkonzol URL-je.","ADATBAZIS","TEXT","/h2-console",false,true,false),

        b("nav.xsdparsertool.security.mode","Biztonsági üzemmód","Önálló vagy többfelhasználós működés.","HITELESITES","SELECT","MULTI_USER",false,false,true,"STANDALONE","MULTI_USER"),
        b("nav.xsdparsertool.security.standalone.username","Standalone felhasználó","A standalone módban használt technikai felhasználónév.","HITELESITES","TEXT","local-user",false,true,false),
        b("nav.xsdparsertool.api-key.enabled","API-kulcs engedélyezése","Külső API-kulcsos hozzáférés engedélyezése.","HITELESITES","BOOLEAN","false",false,true,false),
        b("nav.xsdparsertool.api-key.header-name","API-kulcs fejléc","Az API-kulcsot tartalmazó HTTP fejléc neve.","HITELESITES","TEXT","X-API-Key",false,true,false),
        d("nav.xsdparsertool.api-key.value","API-kulcs","A külső kliens titkos API-kulcsa.","HITELESITES","PASSWORD","",true,true,false),
        b("nav.xsdparsertool.api-key.principal-name","API-kulcs principal","Az API-kulccsal hitelesített technikai principal neve.","HITELESITES","TEXT","external-api-key-client",false,true,false),


        d("nav.xsdparsertool.paths.schema-dir","XSD könyvtár","Űrlapsablon XSD-k gyökérkönyvtára.","KONYVTARAK","PATH","",false,false,true),
        d("nav.xsdparsertool.paths.common-xsd-dir","Közös XSD könyvtár","Általános és közös sémák könyvtára.","KONYVTARAK","PATH","",false,false,false),
        d("nav.xsdparsertool.paths.ui-model-dir","UIModel könyvtár","UIModel XML állományok könyvtára.","KONYVTARAK","PATH","",false,false,true),
        d("nav.xsdparsertool.xml-file.upload-dir","Feltöltési könyvtár","Feltöltött XML-ek tárolási helye.","KONYVTARAK","PATH","",false,false,true),
        d("nav.xsdparsertool.xml-file.backup-dir","Biztonsági mentések","XML biztonsági mentések könyvtára.","KONYVTARAK","PATH","",false,true,true),
        d("nav.xsdparsertool.xml-file.archive-dir","Archív könyvtár","Archivált XML-ek könyvtára.","KONYVTARAK","PATH","",false,true,true),
        d("nav.xsdparsertool.xml-file.xml-index-dir","XML index könyvtár","A gyors XML indexek tárolási helye.","KONYVTARAK","PATH","",false,true,true),
        d("nav.xsdparsertool.xml-index.config-path","XML index konfiguráció","Az XML indexmezőket leíró konfigurációs állomány.","KONYVTARAK","PATH","",false,true,true),

        d("nav.xsdparsertool.security.password-policy.minimum-length","Minimális jelszóhossz","Helyi jelszavak minimális hossza.","BIZTONSAG","NUMBER","14",false,false,true),
        d("nav.xsdparsertool.security.password-policy.maximum-length","Maximális jelszóhossz","Helyi jelszavak maximális hossza.","BIZTONSAG","NUMBER","128",false,true,true),
        d("nav.xsdparsertool.security.password-policy.history-size","Jelszóelőzmény","Nem ismételhető korábbi jelszavak száma.","BIZTONSAG","NUMBER","5",false,false,true),
        d("nav.xsdparsertool.security.password-policy.maximum-failed-attempts","Sikertelen próbálkozások","Zárolás előtti sikertelen belépések száma.","BIZTONSAG","NUMBER","5",false,false,true),
        d("nav.xsdparsertool.security.password-policy.lock-duration","Zárolási idő","Ideiglenes fiókzárolás időtartama.","BIZTONSAG","DURATION","15m",false,false,true),
        d("nav.xsdparsertool.security.password-policy.forbidden-passwords","Tiltott jelszavak","Vesszővel tagolt tiltott jelszólista.","BIZTONSAG","TEXTAREA","jelszo,jelszó,password,password1,123456,admin",false,true,false),

        d("nav.xsdparsertool.xml-file.large-file.threshold","Nagy XML küszöb","E méret felett nagy XML mód aktiválódik.","XML","TEXT","20 MB",false,false,true),
        d("nav.xsdparsertool.xml-file.large-file.disable-xml-tree","XML-fa tiltása nagy XML-nél","Nagy XML esetén ne épüljön fel az XML-fa.","XML","BOOLEAN","true",false,true,true),
        d("nav.xsdparsertool.xml-file.large-file.disable-xml-source","XML-forrás tiltása nagy XML-nél","Nagy XML esetén ne töltődjön be a teljes XML-forrás.","XML","BOOLEAN","true",false,true,true),
        d("nav.xsdparsertool.xml-file.lock.timeout-minutes","Szerkesztési zár időkorlát","Ennyi perc után jár le a szerkesztési zár.","XML","NUMBER","30",false,true,true),
        d("nav.xsdparsertool.xml-file.lock.renew-minutes","Zármegújítás időköze","A szerkesztési zár megújításának időköze percben.","XML","NUMBER","30",false,true,true),
        d("nav.xsdparsertool.xml-file.server-browser.enabled","Szerveroldali tallózó","Szerveroldali XML-könyvtár tallózásának engedélyezése.","XML","BOOLEAN","true",false,false,false),
        d("nav.xsdparsertool.xml-file.server-import.root-dir","Szerveroldali import gyökere","A külső XML-ek importálására és automatikus regisztrációjára használt gyökérkönyvtár.","XML","PATH","${app.data.dir:${nav.xsdparsertool.data-directory}}/data/import",false,false,true),
        d("nav.xsdparsertool.xml-file.server-browser.auto-register-enabled","Automatikus regisztráció","A szerveroldali XML-ek automatikus adatbázis-regisztrációja.","XML","BOOLEAN","true",false,true,false),
        d("nav.xsdparsertool.xml-file.server-browser.auto-register-on-startup","Regisztráció induláskor","Induláskor fusson le a szerveroldali XML-ek regisztrációja.","XML","BOOLEAN","true",false,true,false),
        d("nav.xsdparsertool.xml-file.server-browser.auto-register-interval-ms","Regisztráció időköze","Automatikus háttér-regisztráció időköze milliszekundumban.","XML","NUMBER","30000",false,true,false),
        d("nav.xsdparsertool.form.renderer.default","Alapértelmezett űrlap-renderelő","Az űrlap megjelenítéséhez használt renderelő.","XML","TEXT","uimodel",false,true,false),
        d("nav.xsdparsertool.form.validation-drawer.side","Validációs panel oldala","A validációs drawer megjelenési oldala.","XML","SELECT","right",false,true,false,"left","right"),

        d("nav.xsdparsertool.xsd-validation.max-errors","Maximális XSD-hibaszám","Egy validációban visszaadott hibák felső korlátja.","VALIDACIO","NUMBER","500",false,false,true),
        d("nav.xsdparsertool.xpath-validator.xsl-root-dir","XPath XSL könyvtár","Az XSL transzformációk gyökérkönyvtára.","VALIDACIO","PATH","",false,false,false),
        d("nav.xsdparsertool.xpath-validator.rule-root-dir","XPath szabálykönyvtár","A szabályleíró állományok gyökérkönyvtára.","VALIDACIO","PATH","",false,false,false),
        d("nav.xsdparsertool.xpath-validator.result-dir","XPath eredménykönyvtár","Az XPath ellenőrzések eredménykönyvtára.","VALIDACIO","PATH","",false,true,false),
        d("nav.xsdparsertool.xpath-validator.fixed-xsl-name","Fix XSL fájlnév","A fixen használt XSL állomány neve.","VALIDACIO","TEXT","full_check_core_public.xsl",false,true,false),
        d("nav.xsdparsertool.xpath-validator.sync-timeout-seconds","Szinkron időkorlát","Szinkron XPath futtatás időkorlátja másodpercben.","VALIDACIO","NUMBER","60",false,true,true),
        d("nav.xsdparsertool.xpath-validator.async-thread-count","XPath feldolgozó szálak","Aszinkron XPath feldolgozó szálak száma.","VALIDACIO","NUMBER","4",false,true,false),
        d("nav.xsdparsertool.xpath-validator.async-queue-capacity","XPath várólista","Az aszinkron XPath feldolgozó várólistájának kapacitása.","VALIDACIO","NUMBER","100",false,true,false),
        d("nav.xsdparsertool.xpath-validator.default-page-size","Alapértelmezett oldalméret","XPath eredménylista alapértelmezett oldalmérete.","VALIDACIO","NUMBER","20",false,true,false),
        d("nav.xsdparsertool.xpath-validator.default-auto-refresh-seconds","Automatikus frissítés","XPath eredményoldal automatikus frissítési ideje másodpercben.","VALIDACIO","NUMBER","5",false,true,false),

        d("nav.m2m.mock-mode","M2M tesztmód","Valós hívások helyett mock működés.","M2M","BOOLEAN","true",false,false,true),
        d("nav.m2m.storage-directory","M2M tárolási könyvtár","Beküldési csomagok és kommunikációs állományok helye.","M2M","PATH","",false,false,false),
        d("nav.m2m.max-in-memory-bizonylat-api-bytes","M2M memóriakorlát","A bizonylat API memóriában kezelhető maximális mérete bájtban.","M2M","NUMBER","524288000",false,true,true),
        d("nav.m2m.endpoints.common-base-url","M2M common alap URL","NAV M2M common API alap URL.","M2M","TEXT","https://m2m-dev.nav.gov.hu/rest-api/1.1",false,false,false),
        d("nav.m2m.endpoints.bizonylat-base-url","M2M bizonylat alap URL","NAV M2M bizonylat API alap URL.","M2M","TEXT","https://m2m-dev.nav.gov.hu/rest-api/1.0",false,true,false),
        d("nav.m2m.endpoints.token-path","Token végpont","Tokenkérés relatív útvonala.","M2M","TEXT","/NavM2mCommon/tokenService/Token",false,true,false),
        d("nav.m2m.endpoints.nonce-path","Nonce végpont","Nonce-kérés relatív útvonala.","M2M","TEXT","/NavM2mCommon/userregistrationService/Nonce",false,true,false),
        d("nav.m2m.endpoints.activation-path","Aktivációs végpont","Aktiváció relatív útvonala.","M2M","TEXT","/NavM2mCommon/userregistrationService/Activation",false,true,false),
        d("nav.m2m.endpoints.file-upload-path","Fájlfeltöltési végpont","Csatolmányfeltöltés relatív útvonala.","M2M","TEXT","/NavM2mCommon/filestoreUploadService/File",false,true,false),
        d("nav.m2m.endpoints.file-status-path","Fájlstátusz végpont","Csatolmány státuszlekérdezés relatív útvonala.","M2M","TEXT","/NavM2mCommon/filestoreDownloadService/File/{fileId}",false,true,false),
        d("nav.m2m.endpoints.bizonylat-path","Bizonylat végpont","Bizonylatbeküldés relatív útvonala.","M2M","TEXT","/NavM2mBizonylat/bizonylatService/Bizonylat",false,true,false),
        d("nav.m2m.auth.client-id","M2M kliensazonosító","NAV M2M kliensazonosító.","M2M","TEXT","",false,false,false),
        d("nav.m2m.auth.client-secret","M2M kliens titok","NAV M2M kliens titok.","M2M","PASSWORD","",true,false,false),
        d("nav.m2m.auth.username","M2M felhasználónév","NAV M2M technikai felhasználónév.","M2M","TEXT","",false,true,false),
        d("nav.m2m.auth.password","M2M jelszó","NAV M2M technikai jelszó.","M2M","PASSWORD","",true,true,false),
        d("nav.m2m.signature.key-first-part","Aláírókulcs első rész","M2M aláírási kulcs első titkos része.","M2M","PASSWORD","",true,true,false),
        d("nav.m2m.signature.nonce","Aláírási nonce","M2M aláírási nonce konfiguráció.","M2M","PASSWORD","",true,true,false),
        d("nav.m2m.taxpayer.test-tax-number","Teszt adószám","Tesztkörnyezetben használt adószám.","M2M","TEXT","11111111",false,true,false),
        d("nav.m2m.taxpayer.real-tax-number","Éles adószám","Éles környezetben használt adószám.","M2M","TEXT","1234567890",false,true,false),
        d("nav.m2m.polling.interval-ms","Általános polling időköz","M2M polling időköze milliszekundumban.","M2M","NUMBER","5000",false,true,false),
        d("nav.m2m.polling.max-attempts","Általános polling próbálkozások","M2M polling maximális próbálkozásszáma.","M2M","NUMBER","20",false,true,false),
        d("nav.m2m.status-poll.enabled","Automatikus státuszlekérdezés","M2M státusz polling engedélyezése.","M2M","BOOLEAN","true",false,false,false),
        d("nav.m2m.status-poll.fixed-delay-ms","Státusz polling ütemezés","Ütemezett státuszlekérdezések közötti idő milliszekundumban.","M2M","NUMBER","60000",false,true,false),
        d("nav.m2m.status-poll.interval","Státusz újrapróbálási idő","Egy beküldés következő lekérdezésének időköze.","M2M","DURATION","60s",false,true,false),
        d("nav.m2m.status-poll.max-age","Státusz polling maximális életkor","Ennyi ideig maradhat automatikusan lekérdezendő egy beküldés.","M2M","DURATION","24h",false,true,false),
        d("nav.m2m.status-poll.batch-size","Státusz polling csomagméret","Egy futásban feldolgozott beküldések maximális száma.","M2M","NUMBER","50",false,true,false),
        d("nav.m2m.status-poll.max-attempts","Státusz polling próbálkozások","Egy beküldés maximális státuszlekérdezési próbálkozása.","M2M","NUMBER","1440",false,true,false),
        d("nav.m2m.submission.allow-resubmit","Újraküldés engedélyezése","Fejlesztői módban sikeres beküldés után is engedélyezze az újraküldést.","M2M","BOOLEAN","true",false,true,false),
        d("nav.m2m.attachment.validity-duration","Csatolmány érvényessége","NAV oldali csatolmány-élettartam.","M2M","DURATION","3d",false,true,false),
        d("nav.m2m.attachment.expiry-safety-margin","Csatolmány biztonsági tartalék","Beküldés előtti minimális hátralévő érvényesség.","M2M","DURATION","2h",false,true,false),

        d("nav.xsdparsertool.github-schema-updater.enabled","GitHub frissítő","GitHub sablonkatalógus-frissítő engedélyezése.","GITHUB","BOOLEAN","true",false,false,false),
        d("nav.xsdparsertool.github-schema-updater.organization","GitHub szervezet","A sablonrepository-k GitHub szervezete.","GITHUB","TEXT","nav-gov-hu-templates",false,false,false),
        d("nav.xsdparsertool.github-schema-updater.token","GitHub hozzáférési token","GitHub Personal Access Token az API-lekérésekhez és release-letöltésekhez. Az érték titkosítva kerül tárolásra.","GITHUB","PASSWORD","",true,true,false),
        d("nav.xsdparsertool.github-schema-updater.api-base-url","GitHub API alap URL","A GitHub REST API alap URL-je.","GITHUB","TEXT","https://api.github.com",false,true,false),
        d("nav.xsdparsertool.github-schema-updater.download-mode","Letöltési mód","A release archívum letöltési módja.","GITHUB","SELECT","API_ZIPBALL",false,true,false,"API_ZIPBALL","WEB_ARCHIVE"),
        d("nav.xsdparsertool.github-schema-updater.archive-url-template","Archívum URL sablon","A webes ZIP-letöltés URL-sablonja.","GITHUB","TEXT","https://github.com/{owner}/{repo}/archive/refs/tags/{tag}.zip",false,true,false),
        d("nav.xsdparsertool.github-schema-updater.request-timeout","GitHub kérési időkorlát","API- és letöltési kérések időkorlátja.","GITHUB","DURATION","60s",false,true,false),
        d("nav.xsdparsertool.github-schema-updater.max-pages","GitHub maximális oldalszám","Lapozott API-hívások biztonsági felső korlátja.","GITHUB","NUMBER","50",false,true,false),
        d("nav.xsdparsertool.github-schema-updater.skip-existing-tag-directories","Meglévő tagek kihagyása","A már meglévő repo/tag könyvtárak kihagyása.","GITHUB","BOOLEAN","true",false,true,false),
        d("nav.xsdparsertool.github-schema-updater.catalog-check-interval","Katalógus ellenőrzési idő","Automatikus katalógusellenőrzés időköze.","GITHUB","DURATION","15m",false,true,false),
        d("nav.xsdparsertool.github-schema-updater.rate-limit-enabled","Rate limit kezelés","GitHub rate-limit újrapróbálás engedélyezése.","GITHUB","BOOLEAN","true",false,true,false),
        d("nav.xsdparsertool.github-schema-updater.rate-limit-max-retries","Rate limit próbálkozások","Rate-limit esetén maximális újrapróbálás.","GITHUB","NUMBER","5",false,true,false),
        d("nav.xsdparsertool.github-schema-updater.rate-limit-default-secondary-wait","Másodlagos rate-limit várakozás","Retry-After hiányában alkalmazott várakozás.","GITHUB","DURATION","60s",false,true,false),
        d("nav.xsdparsertool.github-schema-updater.rate-limit-max-wait","Maximális rate-limit várakozás","Egy rate-limit várakozás biztonsági felső korlátja.","GITHUB","DURATION","15m",false,true,false),
        d("nav.xsdparsertool.github-schema-updater.rate-limit-print-headers","Rate-limit fejlécek naplózása","GitHub rate-limit fejlécek diagnosztikai naplózása.","GITHUB","BOOLEAN","false",false,true,false),
        d("nav.xsdparsertool.github.proxy.enabled","GitHub proxy használata","A GitHub API- és release-letöltések külön proxy használata.","GITHUB","BOOLEAN","false",false,false,false),
        d("nav.xsdparsertool.github.proxy.host","GitHub proxy host","A GitHub kapcsolatok proxy kiszolgálójának neve vagy URL-je.","GITHUB","TEXT","",false,false,false),
        d("nav.xsdparsertool.github.proxy.port","GitHub proxy port","A GitHub proxy kiszolgáló portja.","GITHUB","NUMBER","8080",false,false,false),
        d("nav.xsdparsertool.github.proxy.username","GitHub proxy felhasználónév","Opcionális proxyhitelesítési név. Üresen hagyva a kapcsolat hitelesítés nélkül épül fel.","GITHUB","TEXT","",false,true,false),
        d("nav.xsdparsertool.github.proxy.password","GitHub proxy jelszó","A GitHub proxy opcionális, titkosítva tárolt jelszava.","GITHUB","PASSWORD","",true,true,false),
        d("nav.xsdparsertool.github.proxy.ssl-verification-disabled","GitHub TLS-ellenőrzés kikapcsolása","Kizárólag diagnosztikai célra kikapcsolja a GitHub HTTPS tanúsítvány-ellenőrzést.","GITHUB","BOOLEAN","false",false,true,false),
        d("nav.xsdparsertool.github.proxy.trust-store-path","GitHub truststore útvonal","A GitHub kapcsolat külön truststore állományának útvonala.","GITHUB","PATH","",false,true,false),
        d("nav.xsdparsertool.github.proxy.trust-store-type","GitHub truststore típusa","A GitHub truststore formátuma.","GITHUB","SELECT","JKS",false,true,false,"JKS","PKCS12"),
        d("nav.xsdparsertool.github.proxy.trust-store-password","GitHub truststore jelszó","A GitHub truststore titkosítva tárolt jelszava.","GITHUB","PASSWORD","",true,true,false),

        d("nav.xsdparsertool.network.proxy.enabled","Általános proxy használata","Kimenő HTTP/HTTPS kérések általános proxy használata.","HALOZAT","BOOLEAN","false",false,false,false),
        d("nav.xsdparsertool.network.proxy.host","Általános proxy host","A proxy kiszolgáló neve vagy IP-címe.","HALOZAT","TEXT","",false,false,false),
        d("nav.xsdparsertool.network.proxy.port","Általános proxy port","A proxy kiszolgáló portja.","HALOZAT","NUMBER","8080",false,false,false),
        d("nav.xsdparsertool.network.proxy.username","Általános proxy felhasználónév","Opcionális proxy hitelesítési név.","HALOZAT","TEXT","",false,true,false),
        d("nav.xsdparsertool.network.proxy.password","Általános proxy jelszó","Titkosítva tárolt proxy jelszó.","HALOZAT","PASSWORD","",true,true,false),
        d("nav.xsdparsertool.network.proxy.ssl-verification-disabled","M2M TLS-ellenőrzés kikapcsolása","A NAV M2M kliens TLS-ellenőrzésének technikai kapcsolója. Biztonsági okból a kliens a kikapcsolást elutasítja.","HALOZAT","BOOLEAN","false",false,true,false),
        d("nav.xsdparsertool.network.proxy.trust-store-path","M2M truststore útvonal","Opcionális, kizárólag az M2M klienshez használt JKS vagy PKCS12 truststore abszolút útvonala. Üresen a központi tanúsítványkezelő tárát használja.","HALOZAT","PATH","",false,true,false),
        d("nav.xsdparsertool.network.proxy.trust-store-type","M2M truststore típusa","Az opcionális M2M truststore formátuma.","HALOZAT","SELECT","JKS",false,true,false,"JKS","PKCS12"),
        d("nav.xsdparsertool.network.proxy.trust-store-password","M2M truststore jelszó","Az opcionális M2M truststore titkosítva tárolt jelszava.","HALOZAT","PASSWORD","",true,true,false),
        d("nav.xsdparsertool.network.proxy.non-proxy-hosts","Proxy kivételek","Vesszővel tagolt közvetlenül elérendő hostlista.","HALOZAT","TEXTAREA","localhost,127.0.0.1",false,true,false),
        d("nav.xsdparsertool.network.connect-timeout-ms","Kapcsolódási timeout","Kimenő hálózati kapcsolat időkorlátja milliszekundumban.","HALOZAT","NUMBER","10000",false,true,false),
        d("nav.xsdparsertool.network.read-timeout-ms","Olvasási timeout","Kimenő hálózati válasz olvasási időkorlátja milliszekundumban.","HALOZAT","NUMBER","60000",false,true,false),
        d("nav.xsdparsertool.tls.validation-enabled","TLS-ellenőrzés","A kiszolgálói TLS-tanúsítvány ellenőrzésének engedélyezése.","TANUSITVANY","BOOLEAN","true",false,false,true),
        d("nav.xsdparsertool.tls.expiry-warning-days","Lejárati figyelmeztetés","Ennyi nappal lejárat előtt kapjon figyelmeztető állapotot a tanúsítvány.","TANUSITVANY","NUMBER","30",false,false,true),
        d("nav.xsdparsertool.tls.remote-fetch-enabled","Távoli tanúsítványlekérés","Távoli HTTPS tanúsítványlánc lekérésének engedélyezése.","TANUSITVANY","BOOLEAN","true",false,true,false),

        b("app.data.dir","Alkalmazás adatkönyvtára","A telepítő által kijelölt alkalmazás-adatkönyvtár abszolút elérési útja.","NAPLOZAS","PATH","",false,false,false),

        d("logging.level.root","Alap naplózási szint","Az alkalmazás gyökér loggerének szintje.","NAPLOZAS","SELECT","INFO",false,false,true,"TRACE","DEBUG","INFO","WARN","ERROR"),
        d("logging.level.hu.gov.nav.xsdparsertool.web.xpath","XPath naplózási szint","Az XPath modul részletes naplózási szintje.","NAPLOZAS","SELECT","INFO",false,true,false,"TRACE","DEBUG","INFO","WARN","ERROR"),
        b("logging.file.name","Naplófájl","Az alkalmazás naplófájljának abszolút elérési útja.","NAPLOZAS","PATH","",false,true,false),
        b("logging.pattern.console","Konzol naplóminta","A konzolon megjelenő naplóformátum.","NAPLOZAS","TEXT","%d{HH:mm:ss.SSS} %-5level [%X{correlationId}] [%X{user}] %msg%n",false,true,false),
        b("logging.pattern.file","Fájlos naplóminta","A naplófájlban használt formátum.","NAPLOZAS","TEXT","%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{correlationId}] [%X{user}] %logger - %msg%n",false,true,false)
    );

    public static final Set<String> ENCRYPTED_SECRET_KEYS = Set.of(
        "nav.xsdparsertool.api-key.value",
        "nav.xsdparsertool.github-schema-updater.token",
        "nav.xsdparsertool.github.proxy.password",
        "nav.xsdparsertool.github.proxy.trust-store-password",
        "nav.xsdparsertool.network.proxy.password",
        "nav.xsdparsertool.network.proxy.trust-store-password",
        "nav.m2m.auth.client-secret",
        "nav.m2m.auth.password",
        "nav.m2m.signature.key-first-part",
        "nav.m2m.signature.key-second-part",
        "nav.m2m.signature.nonce"
    );


    /**
     * A {@code isOptionalIntegrationKey} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param key a művelet bemeneti {@code key} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public static boolean isOptionalIntegrationKey(String key) {
        return key != null && (key.startsWith("nav.m2m.")
                || key.startsWith("nav.xsdparsertool.github-schema-updater.")
                || key.startsWith("nav.xsdparsertool.github.proxy.")
                || key.startsWith("nav.xsdparsertool.network.proxy."));
    }

    /**
     * Létrehozza a {@code ConfigurationCatalog} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     */
    private ConfigurationCatalog() {}
}
