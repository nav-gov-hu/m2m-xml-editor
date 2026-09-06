package hu.gov.nav.xsdparsertool.web.xpath.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * A XPath/XSLT validátor futásidejű útvonal-, korlát- és végrehajtási beállításait hordozó konfigurációs objektum.
 * Az osztály a config csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: @Configuration, @ConfigurationProperties.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: @Configuration, @ConfigurationProperties.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


@ConfigurationProperties(prefix = "nav.xsdparsertool.xpath-validator")
public class XPathValidatorProperties {
    @Schema(description = "HU: Az XSL állományok gyökérkönyvtára. Alapértelmezett érték nincs, érvényes érték létező könyvtár, és a transzformációk betöltési helyét befolyásolja. EN: Root directory of XSL files. No default value is defined, the valid value is an existing directory, and it affects where transformations are loaded from.")
    private String xslRootDir;
    @Schema(description = "HU: A szabályleíró állományok gyökérkönyvtára. Alapértelmezett érték nincs, érvényes érték létező könyvtár, és a validációs szabályok feloldását befolyásolja. EN: Root directory of rule descriptor files. No default value is defined, the valid value is an existing directory, and it affects validation rule resolution.")
    private String ruleRootDir;
    @Schema(description = "HU: Az elkészült eredmény XML-ek célkönyvtára. Alapértelmezett érték: ./result. Érvényes érték írható könyvtár, és a kimeneti eredmények tárolását befolyásolja. EN: Target directory of generated result XML files. Default value: ./result. Valid value is a writable directory, and it affects output storage.")
    private String resultDir;
    @Schema(description = "HU: A szinkron kérés legfeljebb ennyi másodpercig vár a befejezésre. Alapértelmezett érték: 60. Érvényes tartomány: pozitív egész szám, és a SYNC mód HTTP válaszidejét befolyásolja. EN: Maximum number of seconds a synchronous request waits for completion. Default value: 60. Valid range: positive integer, and it affects SYNC mode HTTP response timing.")
    private int syncTimeoutSeconds = 60;
    @Schema(description = "HU: Az aszinkron feldolgozó szálak száma. Alapértelmezett érték: 4. Érvényes tartomány: pozitív egész szám, és a párhuzamos feldolgozási kapacitást befolyásolja. EN: Number of asynchronous worker threads. Default value: 4. Valid range: positive integer, and it affects concurrent processing capacity.")
    private int asyncThreadCount = 4;
    @Schema(description = "HU: Az aszinkron feldolgozó sor kapacitása. Alapértelmezett érték: 500. Érvényes tartomány: pozitív egész szám, és a várakozó kérések maximális számát befolyásolja. EN: Capacity of the asynchronous processing queue. Default value: 500. Valid range: positive integer, and it affects the maximum number of queued requests.")
    private int asyncQueueCapacity = 500;
    @Schema(description = "HU: A kliensoldali automatikus frissítés alapértelmezett másodperc értéke. Alapértelmezett érték: 10. Érvényes tartomány: pozitív egész szám, és a státuszlista ajánlott frissítési ütemét befolyásolja. EN: Default client-side auto-refresh interval in seconds. Default value: 10. Valid range: positive integer, and it affects the recommended refresh cadence of status lists.")
    private int defaultAutoRefreshSeconds = 10;
    @Schema(description = "HU: A listaoldalak alapértelmezett lapmérete. Alapértelmezett érték: 10. Érvényes tartomány: pozitív egész szám, és a listázó REST végpontok alapértelmezett elemszámát befolyásolja. EN: Default page size for list pages. Default value: 10. Valid range: positive integer, and it affects the default item count of list REST endpoints.")
    private int defaultPageSize = 10;
    @Schema(description = "HU: A kötelezően futtatott XSL fájl neve. Alapértelmezett érték: full_check_core_public.xsl. Érvényes érték létező fájlnév az xsl-root-dir alatt, és a végrehajtott transzformációt befolyásolja. EN: File name of the mandatory XSL transformation. Default value: full_check_core_public.xsl. Valid value is an existing file name under xsl-root-dir, and it affects the executed transformation.")
    private String fixedXslName = "full_check_core_public.xsl";
/**
 * Visszaadja a {@code xslRootDir} mező aktuális értékét.
 * @return a {@code xslRootDir} mező értéke
 */

    public String getXslRootDir() { return xslRootDir; }
/**
 * Beállítja a {@code xslRootDir} mező értékét.
 * @param xslRootDir a beállítandó új érték
 */
    public void setXslRootDir(String xslRootDir) { this.xslRootDir = xslRootDir; }
/**
 * Visszaadja a {@code ruleRootDir} mező aktuális értékét.
 * @return a {@code ruleRootDir} mező értéke
 */
    public String getRuleRootDir() { return ruleRootDir; }
/**
 * Beállítja a {@code ruleRootDir} mező értékét.
 * @param ruleRootDir a beállítandó új érték
 */
    public void setRuleRootDir(String ruleRootDir) { this.ruleRootDir = ruleRootDir; }
/**
 * Visszaadja a {@code resultDir} mező aktuális értékét.
 * @return a {@code resultDir} mező értéke
 */
    public String getResultDir() { return resultDir; }
/**
 * Beállítja a {@code resultDir} mező értékét.
 * @param resultDir a beállítandó új érték
 */
    public void setResultDir(String resultDir) { this.resultDir = resultDir; }
/**
 * Visszaadja a {@code syncTimeoutSeconds} mező aktuális értékét.
 * @return a {@code syncTimeoutSeconds} mező értéke
 */
    public int getSyncTimeoutSeconds() { return syncTimeoutSeconds; }
/**
 * Beállítja a {@code syncTimeoutSeconds} mező értékét.
 * @param syncTimeoutSeconds a beállítandó új érték
 */
    public void setSyncTimeoutSeconds(int syncTimeoutSeconds) { this.syncTimeoutSeconds = syncTimeoutSeconds; }
/**
 * Visszaadja a {@code asyncThreadCount} mező aktuális értékét.
 * @return a {@code asyncThreadCount} mező értéke
 */
    public int getAsyncThreadCount() { return asyncThreadCount; }
/**
 * Beállítja a {@code asyncThreadCount} mező értékét.
 * @param asyncThreadCount a beállítandó új érték
 */
    public void setAsyncThreadCount(int asyncThreadCount) { this.asyncThreadCount = asyncThreadCount; }
/**
 * Visszaadja a {@code asyncQueueCapacity} mező aktuális értékét.
 * @return a {@code asyncQueueCapacity} mező értéke
 */
    public int getAsyncQueueCapacity() { return asyncQueueCapacity; }
/**
 * Beállítja a {@code asyncQueueCapacity} mező értékét.
 * @param asyncQueueCapacity a beállítandó új érték
 */
    public void setAsyncQueueCapacity(int asyncQueueCapacity) { this.asyncQueueCapacity = asyncQueueCapacity; }
/**
 * Visszaadja a {@code defaultAutoRefreshSeconds} mező aktuális értékét.
 * @return a {@code defaultAutoRefreshSeconds} mező értéke
 */
    public int getDefaultAutoRefreshSeconds() { return defaultAutoRefreshSeconds; }
/**
 * Beállítja a {@code defaultAutoRefreshSeconds} mező értékét.
 * @param defaultAutoRefreshSeconds a beállítandó új érték
 */
    public void setDefaultAutoRefreshSeconds(int defaultAutoRefreshSeconds) { this.defaultAutoRefreshSeconds = defaultAutoRefreshSeconds; }
/**
 * Visszaadja a {@code defaultPageSize} mező aktuális értékét.
 * @return a {@code defaultPageSize} mező értéke
 */
    public int getDefaultPageSize() { return defaultPageSize; }
/**
 * Beállítja a {@code defaultPageSize} mező értékét.
 * @param defaultPageSize a beállítandó új érték
 */
    public void setDefaultPageSize(int defaultPageSize) { this.defaultPageSize = defaultPageSize; }
/**
 * Visszaadja a {@code fixedXslName} mező aktuális értékét.
 * @return a {@code fixedXslName} mező értéke
 */
    public String getFixedXslName() { return fixedXslName; }
/**
 * Beállítja a {@code fixedXslName} mező értékét.
 * @param fixedXslName a beállítandó új érték
 */
    public void setFixedXslName(String fixedXslName) { this.fixedXslName = fixedXslName; }
}
