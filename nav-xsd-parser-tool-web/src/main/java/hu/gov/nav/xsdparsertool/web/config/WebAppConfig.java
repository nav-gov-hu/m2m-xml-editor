package hu.gov.nav.xsdparsertool.web.config;

import hu.gov.nav.xsdparsertool.web.path.ConfiguredPathSupport;
import hu.gov.nav.xsdparsertool.processing.form.DefaultFormDataBuilderService;
import hu.gov.nav.xsdparsertool.processing.form.DefaultFormDefinitionBuilderService;
import hu.gov.nav.xsdparsertool.processing.form.FormDataBuilderService;
import hu.gov.nav.xsdparsertool.processing.form.FormDefinitionBuilderService;
import hu.gov.nav.xsdparsertool.processing.service.DefaultXmlProcessingService;
import hu.gov.nav.xsdparsertool.processing.service.XmlProcessingService;
import hu.gov.nav.xsdparsertool.processing.validation.XsdValidationService;
import hu.gov.nav.xsdparsertool.processing.xml.XmlProbeService;
import hu.gov.nav.xsdparsertool.processing.xmlview.DefaultXmlViewBuilderService;
import hu.gov.nav.xsdparsertool.processing.xmlview.XmlViewBuilderService;
import hu.gov.nav.xsdparsertool.schemaregistry.service.FileSystemSchemaRegistryService;
import hu.gov.nav.xsdparsertool.xsd.service.BasicXsdParserService;
import hu.gov.nav.xsdparsertool.web.xpath.config.XPathValidatorProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xpath.service.XsltValidationService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
/**
 * A webalkalmazás fő feldolgozási, validációs és statikus erőforrás-kezelési komponenseit összeállító Spring konfiguráció.
 * Az osztály a config csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: @Configuration.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: @Configuration.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


@Configuration
@EnableConfigurationProperties({PathConfigurationProperties.class, XPathValidatorProperties.class, XmlFileStorageProperties.class})
public class WebAppConfig implements WebMvcConfigurer {

    private final UiMenuAccessInterceptor uiMenuAccessInterceptor;

    /**
     * Létrehozza a {@code WebAppConfig} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param uiMenuAccessInterceptor a művelet bemeneti {@code uiMenuAccessInterceptor} értéke
     */
    public WebAppConfig(UiMenuAccessInterceptor uiMenuAccessInterceptor) {
        this.uiMenuAccessInterceptor = uiMenuAccessInterceptor;
    }

    /**
     * A {@code addInterceptors} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param registry a művelet bemeneti {@code registry} értéke
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(uiMenuAccessInterceptor)
                .addPathPatterns("/", "/xml-files.html", "/github-templates.html", "/validate.html", "/xpath-validator.html", "/form.html", "/admin.html");
    }


    /**
     * A {@code addResourceHandlers} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param registry a művelet bemeneti {@code registry} értéke
     */
/**
 * Regisztrálja a webalkalmazás statikus erőforrásainak Spring MVC kiszolgálási szabályait.
 * @param registry a {@code registry} paraméter átadott értéke
 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noStore().mustRevalidate())
                .resourceChain(false);
    }

    /**
     * A {@code schemaRegistryService} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param properties a művelethez szükséges konfigurációs adatok
     * @return a művelet feldolgozási eredménye
     */
    @Bean
/**
 * Létrehozza a fájlrendszer-alapú Schema Registry szolgáltatást az alkalmazás konfigurált útvonalaival.
 * @param properties a {@code properties} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */
    public FileSystemSchemaRegistryService schemaRegistryService(PathConfigurationProperties properties) {
        Path schemaRoot = toPath(properties.getSchemaDir());
        Path generalRoot = toPath(properties.getCommonXsdDir());
        return new FileSystemSchemaRegistryService(schemaRoot, generalRoot);
    }


    /**
     * A {@code xsdValidationService} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @return a művelet feldolgozási eredménye
     */
    @Bean
/**
 * Létrehozza az XSD-validációt végző szolgáltatást.
 * @return a metódus által előállított eredmény
 */
    public XsdValidationService xsdValidationService() { return new XsdValidationService(); }

    /**
     * A {@code xmlProcessingService} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a alkalmazási folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param schemaRegistryService a művelet bemeneti {@code schemaRegistryService} értéke
     * @param xsdValidationService a művelet bemeneti {@code xsdValidationService} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Bean
/**
 * Összeállítja az XML feldolgozási pipeline fő szolgáltatását a szükséges parser és registry komponensekkel.
 * @param schemaRegistryService a {@code schemaRegistryService} paraméter átadott értéke
 * @param xsdValidationService a {@code xsdValidationService} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */
    public XmlProcessingService xmlProcessingService(FileSystemSchemaRegistryService schemaRegistryService,
                                                     XsdValidationService xsdValidationService) {
        return new DefaultXmlProcessingService(
                new XmlProbeService(),
                schemaRegistryService,
                new BasicXsdParserService(),
                new hu.gov.nav.xsdparsertool.uimodel.service.NoOpUiModelParserService(),
                new hu.gov.nav.xsdparsertool.pageschema.service.NoOpPageSchemaParserService(),
                xsdValidationService
        );
    }

    /**
     * A {@code xmlProbeService} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a alkalmazási folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a művelet feldolgozási eredménye
     */
    @Bean
/**
 * Létrehozza az XML gyökér- és namespace-felderítést végző szolgáltatást.
 * @return a metódus által előállított eredmény
 */
    public XmlProbeService xmlProbeService() { return new XmlProbeService(); }
    /**
     * A {@code formDefinitionBuilderService} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    @Bean
/**
 * Létrehozza az űrlapdefiníciót UIModel-first és XSD fallback szabályokkal előállító szolgáltatást.
 * @return a metódus által előállított eredmény
 */
    public FormDefinitionBuilderService formDefinitionBuilderService() { return new DefaultFormDefinitionBuilderService(); }
    /**
     * A {@code formDataBuilderService} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    @Bean
/**
 * Létrehozza az XML értékeit teljes útvonal alapján űrlapadatokká alakító szolgáltatást.
 * @return a metódus által előállított eredmény
 */
    public FormDataBuilderService formDataBuilderService() { return new DefaultFormDataBuilderService(); }
    /**
     * A {@code xmlViewBuilderService} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a alkalmazási folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a művelet feldolgozási eredménye
     */
    @Bean
/**
 * Létrehozza a nyers XML és fa-nézet reprezentációját előállító szolgáltatást.
 * @return a metódus által előállított eredmény
 */
    public XmlViewBuilderService xmlViewBuilderService() { return new DefaultXmlViewBuilderService(); }

    /**
     * A {@code xpathXsltValidationService} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a művelet feldolgozási eredménye
     */
    @Bean
/**
 * Létrehozza az XPath/XSLT validáció webes szolgáltatását.
 * @return a metódus által előállított eredmény
 */
    public XsltValidationService xpathXsltValidationService() { return new XsltValidationService(); }

    /**
     * A {@code xpathValidatorExecutor} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param properties a művelethez szükséges konfigurációs adatok
     * @return a művelet feldolgozási eredménye
     */
    @Bean(name = "xpathValidatorExecutor")
    public org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor xpathValidatorExecutor(XPathValidatorProperties properties) {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("xpath-validator-");
        executor.setCorePoolSize(Math.max(1, properties.getAsyncThreadCount()));
        executor.setMaxPoolSize(Math.max(1, properties.getAsyncThreadCount()));
        executor.setQueueCapacity(Math.max(1, properties.getAsyncQueueCapacity()));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
/**
 * A konfigurációs szöveget opcionális, normalizált fájlrendszeri útvonallá alakítja.
 * @param value a {@code value} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */


    private Path toPath(String value) {
        if (value == null || value.isBlank()) return null;
        return ConfiguredPathSupport.toAbsoluteNormalizedPath(value);
    }
}
