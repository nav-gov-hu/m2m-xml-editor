package hu.gov.nav.xsdparsertool.web.xmlfile.scheduler;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import hu.gov.nav.xsdparsertool.web.setup.SetupStateService;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.AutoRegisterServerFilesResponse;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.XmlFileService;

/**
 * A web modul XML-állománykezelési területének közös alkalmazási típusa.
 *
 * <p>A {@code XmlServerFolderAutoRegistrationScheduler} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Component
public class XmlServerFolderAutoRegistrationScheduler {
    private static final Logger log = LoggerFactory.getLogger(XmlServerFolderAutoRegistrationScheduler.class);

    private final XmlFileService xmlFileService;
    private final XmlFileStorageProperties properties;
    private final SetupStateService setupStateService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Létrehozza a {@code XmlServerFolderAutoRegistrationScheduler} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param xmlFileService a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param properties a művelethez szükséges konfigurációs adatok
     * @param setupStateService a feldolgozandó elemek kollekciója
     */
    public XmlServerFolderAutoRegistrationScheduler(XmlFileService xmlFileService,
                                                    XmlFileStorageProperties properties,
                                                    SetupStateService setupStateService) {
        this.xmlFileService = xmlFileService;
        this.properties = properties;
        this.setupStateService = setupStateService;
    }

    /**
     * A {@code runOnStartup} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        if (!setupStateService.isCompleted()) {
            log.debug("Szerver oldali XML startup háttér-regisztráció kihagyva: az első beállítás még nincs befejezve.");
            return;
        }
        if (properties.getServerBrowser().isAutoRegisterOnStartup()) {
            Thread startupScan = new Thread(() -> runScan("startup"), "xml-server-folder-auto-registration-startup");
            startupScan.setDaemon(true);
            startupScan.start();
        }
    }

    /**
     * A {@code runScheduledScan} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     */
    @Scheduled(
            initialDelayString = "${nav.xsdparsertool.xml-file.server-browser.auto-register-interval-ms:30000}",
            fixedDelayString = "${nav.xsdparsertool.xml-file.server-browser.auto-register-interval-ms:30000}")
    public void runScheduledScan() {
        runScan("scheduled");
    }

    /**
     * A {@code runScan} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param trigger a művelet bemeneti {@code trigger} értéke
     */
    private void runScan(String trigger) {
        if (!setupStateService.isCompleted()) {
            log.debug("Szerver oldali XML háttér-regisztráció kihagyva: az első beállítás még nincs befejezve. trigger={}", trigger);
            return;
        }
        if (!properties.getServerBrowser().isEnabled() || !properties.getServerBrowser().isAutoRegisterEnabled()) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.info("Szerver oldali XML háttér-regisztráció kihagyva, mert már fut egy scan. trigger={}", trigger);
            return;
        }
        log.info("Szerver oldali XML háttér-regisztráció indul. trigger={}, root={}",
                trigger, properties.getServerImport().getRootDir());
        long startedAt = System.currentTimeMillis();
        try {
            AutoRegisterServerFilesResponse response = xmlFileService.autoRegisterServerFiles("system-background");
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.info("Szerver oldali XML háttér-regisztráció vége. trigger={}, scanned={}, registered={}, skipped={}, warnings={}, elapsedMs={}",
                    trigger, response.scannedCount(), response.registeredCount(), response.skippedCount(), response.warnings().size(), elapsedMs);
        } catch (IOException | RuntimeException ex) {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.warn("Szerver oldali XML háttér-regisztráció hibával leállt. trigger={}, elapsedMs={}, error={}",
                    trigger, elapsedMs, ex.getMessage(), ex);
        } finally {
            running.set(false);
        }
    }
}
