package hu.gov.nav.xsdparsertool.web.database.releaselock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * A többi startup runner előtt ellenőrzi az adatbázis release-kompatibilitását.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApplicationReleaseLockRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationReleaseLockRunner.class);
    private static final String RELEASE_PROPERTY = "app.full.release";

    private final ApplicationReleaseLockService releaseLockService;
    private final Environment environment;

    /**
     * Létrehozza az induláskori release-lock ellenőrzést.
     *
     * @param releaseLockService release-lock szolgáltatás
     * @param environment Spring környezet
     */
    public ApplicationReleaseLockRunner(ApplicationReleaseLockService releaseLockService,
                                        Environment environment) {
        this.releaseLockService = releaseLockService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String currentRelease = environment.getProperty(RELEASE_PROPERTY);
        releaseLockService.validateBeforeStartup(currentRelease);
        LOGGER.info("Adatbázis release-kompatibilitási ellenőrzés sikeres: {}", currentRelease);
    }
}
