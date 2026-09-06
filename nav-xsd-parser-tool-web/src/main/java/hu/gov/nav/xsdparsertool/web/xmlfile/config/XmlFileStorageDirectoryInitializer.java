package hu.gov.nav.xsdparsertool.web.xmlfile.config;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A külső konfigurációból érkező XML tárhely-könyvtárak ellenőrzése és létrehozása.
 */
@Component
public class XmlFileStorageDirectoryInitializer {
    private static final Logger log = LoggerFactory.getLogger(XmlFileStorageDirectoryInitializer.class);

    private final XmlFileStorageProperties properties;
    private final Environment environment;

    /**
     * Létrehozza a {@code XmlFileStorageDirectoryInitializer} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param properties a művelethez szükséges konfigurációs adatok
     * @param environment a művelet bemeneti {@code environment} értéke
     */
    public XmlFileStorageDirectoryInitializer(XmlFileStorageProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    /**
     * A {@code initialize} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    @PostConstruct
    public void initialize() {
        if (!environment.getProperty("nav.xsdparsertool.setup.completed", Boolean.class, false)) {
            log.info("Az XML tárhelykönyvtárak ellenőrzése az első beállítás befejezéséig nem fut le.");
            return;
        }
        Map<String, Path> directories = new LinkedHashMap<>();
        directories.put("upload-dir", resolve(properties.getUploadDir()));
        directories.put("backup-dir", resolve(properties.getBackupDir()));
        directories.put("archive-dir", resolve(properties.getArchiveDir()));
        directories.put("xml-index-dir", resolve(properties.getXmlIndexDir()));

        for (Map.Entry<String, Path> entry : directories.entrySet()) {
            ensureDirectory(entry.getKey(), entry.getValue());
        }
        log.info("XML tárhely könyvtárak: upload={}, backup={}, archive={}, xmlIndex={}",
                directories.get("upload-dir"), directories.get("backup-dir"),
                directories.get("archive-dir"), directories.get("xml-index-dir"));
    }

    /**
     * A {@code resolve} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param configured a művelethez szükséges konfigurációs adatok
     * @return a feloldott vagy lekért érték
     */
    private Path resolve(String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Hiányzó XML tárhely könyvtár konfiguráció.");
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    /**
     * A {@code ensureDirectory} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param propertyName a feloldáshoz vagy azonosításhoz használt név
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     */
    private void ensureDirectory(String propertyName, Path path) {
        try {
            ExceptionSafeOperations.createDirectories(path);
            if (!ExceptionSafeOperations.isDirectory(path)) {
                throw new IllegalStateException(propertyName + " nem könyvtár: " + path);
            }
            if (!Files.isWritable(path)) {
                throw new IllegalStateException(propertyName + " nem írható: " + path);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Nem készíthető elő a(z) " + propertyName + " könyvtár: " + path, ex);
        }
    }
}
