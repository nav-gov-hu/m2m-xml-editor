package hu.gov.nav.xsdparsertool.web.setup;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * A telepített asztali alkalmazás késleltetett, felhasználói beavatkozás nélküli
 * újraindítását végzi a setup sikeres befejezése után.
 */
@Service
public class ApplicationRestartService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationRestartService.class);
    private static final Duration RESPONSE_GRACE_PERIOD = Duration.ofMillis(900);
    private static final AtomicBoolean RESTART_SCHEDULED = new AtomicBoolean(false);

    private final ConfigurableApplicationContext applicationContext;

    /**
     * Létrehozza a {@code ApplicationRestartService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param applicationContext a művelet bemeneti {@code applicationContext} értéke
     */
    public ApplicationRestartService(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Elindít egy leválasztott Windows PowerShell segédfolyamatot, amely megvárja
     * a jelenlegi példány leállását, majd újra elindítja a jpackage alkalmazást.
     *
     * @return true, ha az automatikus újraindítás ütemezhető volt
     */
    public boolean isRestartAvailable() {
        if (!isWindows()) return false;
        String applicationPath = ExceptionSafeOperations.systemProperty("jpackage.app-path");
        return StringUtils.hasText(applicationPath)
                && ExceptionSafeOperations.isRegularFile(Path.of(applicationPath).toAbsolutePath().normalize());
    }

    /**
     * A {@code scheduleRestart} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    public boolean scheduleRestart() {
        if (!isWindows()) {
            log.warn("Automatic setup restart is currently supported only on Windows.");
            return false;
        }

        String applicationPath = ExceptionSafeOperations.systemProperty("jpackage.app-path");
        if (!StringUtils.hasText(applicationPath)) {
            log.warn("Automatic setup restart is unavailable because jpackage.app-path is not set.");
            return false;
        }

        Path executable = Path.of(applicationPath).toAbsolutePath().normalize();
        if (!ExceptionSafeOperations.isRegularFile(executable)) {
            log.warn("Automatic setup restart is unavailable because the application launcher does not exist: {}", executable);
            return false;
        }

        if (!RESTART_SCHEDULED.compareAndSet(false, true)) {
            log.info("Application restart has already been scheduled.");
            return true;
        }

        try {
            launchDelayedWindowsRestart(executable);
        } catch (IOException ex) {
            RESTART_SCHEDULED.set(false);
            log.error("Could not start the detached application restart helper.", ex);
            return false;
        }

        Thread shutdownThread = new Thread(() -> {
            try {
                Thread.sleep(RESPONSE_GRACE_PERIOD.toMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            log.info("Stopping the current application instance for automatic restart.");
            try {
                applicationContext.close();
            } finally {
                System.exit(0);
            }
        }, "setup-automatic-restart");
        shutdownThread.setDaemon(false);
        shutdownThread.start();

        log.info("Automatic application restart scheduled with launcher: {}", executable);
        return true;
    }

    /**
     * A {@code launchDelayedWindowsRestart} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param executable a művelet bemeneti {@code executable} értéke
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void launchDelayedWindowsRestart(Path executable) throws IOException {
        String escapedPath = executable.toString().replace("'", "''");
        String command = "Start-Sleep -Seconds 3; Start-Process -FilePath '" + escapedPath
                + "' -ArgumentList '--nav.xsdparsertool.desktop.browser-open=false'";

        ProcessBuilder processBuilder = new ProcessBuilder(
                "powershell.exe",
                "-NoLogo",
                "-NoProfile",
                "-NonInteractive",
                "-WindowStyle",
                "Hidden",
                "-Command",
                command);
        processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
        processBuilder.start();
    }

    /**
     * A {@code isWindows} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a kezdeti beállítási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isWindows() {
        return ExceptionSafeOperations.systemProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
