package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.ServerBrowserResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.ServerFileDto;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code ServerFileBrowserService} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class ServerFileBrowserService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final XmlFileStorageProperties properties;

    /**
     * Létrehozza a {@code ServerFileBrowserService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param properties a művelethez szükséges konfigurációs adatok
     */
    public ServerFileBrowserService(XmlFileStorageProperties properties) {
        this.properties = properties;
    }

    /**
     * A {@code listXmlFiles} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a művelet feldolgozási eredménye
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    public ServerBrowserResponse listXmlFiles() throws IOException {
        boolean enabled = properties.getServerBrowser().isEnabled();
        Path root = Path.of(properties.getServerImport().getRootDir()).toAbsolutePath().normalize();
        if (!enabled || !ExceptionSafeOperations.isDirectory(root)) {
            return new ServerBrowserResponse(enabled, root.toString(), List.of());
        }
        try (var stream = Files.list(root)) {
            List<ServerFileDto> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xml"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .map(this::toDto)
                    .toList();
            return new ServerBrowserResponse(true, root.toString(), files);
        }
    }

    /**
     * A {@code toDto} művelet előállítja a hívó réteg által használt reprezentációt.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    private ServerFileDto toDto(Path path) {
        try {
            long size = Files.size(path);
            Instant modified = Files.getLastModifiedTime(path).toInstant();
            return new ServerFileDto(
                    path.getFileName().toString(),
                    path.toAbsolutePath().normalize().toString(),
                    size,
                    formatSize(size),
                    DATE_TIME_FORMATTER.format(modified));
        } catch (IOException ex) {
            return new ServerFileDto(path.getFileName().toString(), path.toAbsolutePath().normalize().toString(), 0L, "-", "-");
        }
    }

    /**
     * A {@code formatSize} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param bytes a művelet bemeneti {@code bytes} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String formatSize(long bytes) {
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value = value / 1024;
            unitIndex++;
        }
        if (unitIndex == 0) {
            return bytes + " B";
        }
        return String.format(Locale.ROOT, "%.2f %s", value, units[unitIndex]);
    }
}
