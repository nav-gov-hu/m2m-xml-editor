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
 * A startup runnerek sikeres lefutása után az aktuális release-hez rendeli az adatbázist.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApplicationReleaseClaimRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationReleaseClaimRunner.class);
    private static final String RELEASE_PROPERTY = "app.full.release";

    private final ApplicationReleaseLockService releaseLockService;
    private final Environment environment;

    /**
     * Létrehozza a startup végi release-birtokbavételt.
     *
     * @param releaseLockService release-lock szolgáltatás
     * @param environment Spring környezet
     */
    public ApplicationReleaseClaimRunner(ApplicationReleaseLockService releaseLockService,
                                         Environment environment) {
        this.releaseLockService = releaseLockService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String currentRelease = environment.getProperty(RELEASE_PROPERTY);
        releaseLockService.claimAfterSuccessfulStartup(currentRelease);
        LOGGER.info("Az adatbázis az aktuális alkalmazás-release-hez rendelve: {}", currentRelease);
    }
}
