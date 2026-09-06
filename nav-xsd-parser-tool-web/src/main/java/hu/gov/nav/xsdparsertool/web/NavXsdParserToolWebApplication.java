package hu.gov.nav.xsdparsertool.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import hu.nav.m2m.submitter.config.NavM2mProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * A web modul alkalmazási területének közös alkalmazási típusa.
 *
 * <p>A {@code NavXsdParserToolWebApplication} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"hu.gov.nav.xsdparsertool", "hu.nav.m2m.submitter"})
@EntityScan(basePackages = {"hu.gov.nav.xsdparsertool", "hu.nav.m2m.submitter"})
@EnableJpaRepositories(basePackages = {"hu.gov.nav.xsdparsertool", "hu.nav.m2m.submitter"})
@EnableConfigurationProperties(NavM2mProperties.class)
public class NavXsdParserToolWebApplication {

    /**
     * A {@code main} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param args a művelet bemeneti {@code args} értéke
     */
    public static void main(String[] args) {
        if (DesktopIntegrationSettings.isDesktopEnabledFromSystemProperties()) {
            System.setProperty("java.awt.headless", "false");
        }

        SpringApplication application = new SpringApplication(NavXsdParserToolWebApplication.class);

        application.addListeners((ApplicationStartingEvent event) ->
                StartupSplashSupport.updateStatus("Spring Boot indítása..."));

        application.addListeners((ApplicationEnvironmentPreparedEvent event) -> {
            if (DesktopIntegrationSettings.isSplashEnabled(event.getEnvironment())) {
                StartupSplashSupport.initialize();
                StartupSplashSupport.updateStatus("Konfiguráció betöltése...");
            }
        });

        application.addListeners((ApplicationPreparedEvent event) ->
                StartupSplashSupport.updateStatus("Spring környezet előkészítése..."));

        application.addListeners((ApplicationStartedEvent event) ->
                StartupSplashSupport.updateStatus("Web modulok és szolgáltatások betöltése..."));

        application.addListeners((ApplicationReadyEvent event) -> {
            StartupSplashSupport.updateStatus("Alkalmazás kész.");
            StartupSplashSupport.close();
        });

        ConfigurableApplicationContext applicationContext = null;
        try {
            applicationContext = application.run(args);

            if (applicationContext instanceof WebServerApplicationContext webServerApplicationContext) {
                int port = webServerApplicationContext.getWebServer().getPort();
                DesktopLauncherSupport.initialize(applicationContext, port);
            }
        } catch (Throwable ex) {
            StartupSplashSupport.showStartupError("Az alkalmazás indítása sikertelen.", ex);
            if (applicationContext != null) {
                applicationContext.close();
            }
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (ex instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Az alkalmazás indítása sikertelen.", ex);
        }
    }
}
