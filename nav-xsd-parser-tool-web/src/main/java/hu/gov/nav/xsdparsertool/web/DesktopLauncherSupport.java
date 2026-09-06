package hu.gov.nav.xsdparsertool.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A web modul alkalmazási területének közös alkalmazási típusa.
 *
 * <p>A {@code DesktopLauncherSupport} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public final class DesktopLauncherSupport {

    private static final Logger log = LoggerFactory.getLogger(DesktopLauncherSupport.class);
    private static final String APPLICATION_NAME = "XML Editor MINTA";
    private static final String LOGO_RESOURCE = "/static/images/SET_logo.png";
    private static final AtomicBoolean BROWSER_OPENED = new AtomicBoolean(false);
    private static TrayIcon trayIcon;

    /**
     * Létrehozza a {@code DesktopLauncherSupport} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     */
    private DesktopLauncherSupport() {
    }
/**
 * Inicializálja a desktop integrációt az alkalmazáskörnyezet és a tényleges szerverport alapján.
 * @param applicationContext a {@code applicationContext} paraméter átadott értéke
 * @param port a {@code port} paraméter átadott értéke
 */

    public static void initialize(ConfigurableApplicationContext applicationContext, int port) {
        if (!DesktopIntegrationSettings.isDesktopEnabled(applicationContext.getEnvironment())) {
            log.info("Desktop integration is disabled by configuration.");
            return;
        }

        String applicationUrl = "http://localhost:" + port;
        if (DesktopIntegrationSettings.isBrowserOpenEnabled(applicationContext.getEnvironment())) {
            openBrowserOnStartup(applicationUrl);
        } else {
            log.info("Desktop browser auto-open is disabled by configuration. Open manually: {}", applicationUrl);
        }
        if (DesktopIntegrationSettings.isTrayEnabled(applicationContext.getEnvironment())) {
            installTrayIcon(applicationContext, applicationUrl);
        } else {
            log.info("Desktop system tray is disabled by configuration.");
        }
    }
/**
 * A konfiguráció alapján eldönti, hogy induláskor meg kell-e nyitni az alkalmazást az alapértelmezett böngészőben.
 * @param applicationUrl a {@code applicationUrl} paraméter átadott értéke
 */

    private static void openBrowserOnStartup(String applicationUrl) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            log.info("Desktop browse action is not supported. Open manually: {}", applicationUrl);
            return;
        }
        if (!BROWSER_OPENED.compareAndSet(false, true)) {
            return;
        }
        openBrowser(applicationUrl);
    }
/**
 * Megnyitja az alkalmazás URL-jét az operációs rendszer alapértelmezett böngészőjében, ha a desktop környezet ezt támogatja.
 * @param applicationUrl a {@code applicationUrl} paraméter átadott értéke
 */

    private static void openBrowser(String applicationUrl) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            log.info("Desktop browse action is not supported. Open manually: {}", applicationUrl);
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(applicationUrl));
        } catch (IOException | URISyntaxException ex) {
            log.warn("Could not open browser automatically. Open manually: {}", applicationUrl, ex);
        }
    }
/**
 * Létrehozza és eseménykezelőkkel látja el a rendszer-tálca ikont a desktop futási módhoz.
 * @param applicationContext a {@code applicationContext} paraméter átadott értéke
 * @param applicationUrl a {@code applicationUrl} paraméter átadott értéke
 */

    private static void installTrayIcon(ConfigurableApplicationContext applicationContext, String applicationUrl) {
        if (!SystemTray.isSupported()) {
            log.info("System tray is not supported on this environment.");
            return;
        }

        try {
            if (trayIcon != null) {
                return;
            }

            PopupMenu popupMenu = new PopupMenu();

            MenuItem openItem = new MenuItem("Megnyitás");
            openItem.addActionListener(event -> openBrowser(applicationUrl));
            popupMenu.add(openItem);

            MenuItem exitItem = new MenuItem("Kilépés");
            exitItem.addActionListener(event -> shutdown(applicationContext));
            popupMenu.addSeparator();
            popupMenu.add(exitItem);

            trayIcon = new TrayIcon(loadTrayImage(), APPLICATION_NAME, popupMenu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(event -> openBrowser(applicationUrl));

            SystemTray.getSystemTray().add(trayIcon);
            trayIcon.displayMessage(APPLICATION_NAME, "Az alkalmazás elindult.", TrayIcon.MessageType.INFO);
        } catch (AWTException ex) {
            log.warn("Could not initialize system tray icon.", ex);
        }
    }
/**
 * Leállítja a Spring alkalmazáskörnyezetet a desktop integrációból kezdeményezett kilépéskor.
 * @param applicationContext a {@code applicationContext} paraméter átadott értéke
 */

    private static void shutdown(ConfigurableApplicationContext applicationContext) {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
        applicationContext.close();
    }
/**
 * Betölti a rendszer-tálca ikonhoz használt képet, és sikertelen erőforrás-feloldáskor biztonságos fallbacket alkalmaz.
 * @return a metódus által előállított eredmény
 */

    private static Image loadTrayImage() {
        try (InputStream inputStream = DesktopLauncherSupport.class.getResourceAsStream(LOGO_RESOURCE)) {
            if (inputStream != null) {
                BufferedImage original = ImageIO.read(inputStream);
                if (original != null) {
                    return original.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                }
            }
        } catch (IOException ex) {
            log.warn("Could not load tray icon resource: {}", LOGO_RESOURCE, ex);
        }

        BufferedImage fallback = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        var graphics = fallback.createGraphics();
        graphics.setColor(java.awt.Color.decode("#0B5FFF"));
        graphics.fillRoundRect(0, 0, 16, 16, 4, 4);
        graphics.dispose();
        return fallback;
    }
}
