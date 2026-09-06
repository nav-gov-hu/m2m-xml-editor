package hu.gov.nav.xsdparsertool.web.setup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Az adatbázis- vagy adatkönyvtár-váltás miatt kétfázisúvá váló első setup második
 * fázisát automatikusan véglegesíti az újraindítás után.
 *
 * <p>A runner csak a privát, egyszer használatos setup handoff jelenlétében dolgozik.
 * Sikertelen automatikus véglegesítéskor nem állítja le az alkalmazást: a setup oldal
 * helyreállítási lehetőségként elérhető marad, a technikai ok pedig naplóba kerül.</p>
 */
@Component
@Order(75)
public class PendingSetupCompletionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PendingSetupCompletionRunner.class);

    private final SetupService setupService;

    /**
     * Létrehozza a pending setup automatikus véglegesítőjét.
     * @param setupService a setup üzleti szolgáltatása
     */
    public PendingSetupCompletionRunner(SetupService setupService) {
        this.setupService = setupService;
    }

    /**
     * Megkísérli a korábbi setup-mentés után függőben maradt második fázis befejezését.
     * @param args az alkalmazás indítási argumentumai
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            if (setupService.completePendingAfterRestart()) {
                log.info("A pending kezdeti rendszerbeállítás automatikusan befejeződött az újraindítás után.");
            }
        } catch (Exception ex) {
            log.error("A pending kezdeti rendszerbeállítás automatikus befejezése sikertelen. A setup oldal helyreállítási célból elérhető marad.", ex);
        }
    }
}
