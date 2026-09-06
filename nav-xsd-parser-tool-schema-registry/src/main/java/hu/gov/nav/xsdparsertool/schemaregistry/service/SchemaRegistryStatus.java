
package hu.gov.nav.xsdparsertool.schemaregistry.service;



/**
 * A séma-regiszter indexelési állapotának pillanatfelvétele.
 * A webes/admin felület ebből tudja megjeleníteni az előtöltés vagy újraindexelés állapotát és előrehaladását.
 */
public class SchemaRegistryStatus {
    private boolean loading;
    private boolean ready;
    private String phase;
    private int processedFiles;
    private int totalFiles;
    private int percentage;
    private int cacheEntryCount;
    private String activeSchemaRootDir;
    private String activeGeneralXsdDir;


    /**


     * Visszaadja, hogy folyamatban van-e sémaindexelés.


     * @return folyamatban van-e sémaindexelés.


     */


    public boolean isLoading() {
        return loading;
    }


    /**


     * Beállítja: folyamatban van-e sémaindexelés.


     * @param loading folyamatban van-e sémaindexelés.


     */


    public void setLoading(boolean loading) {
        this.loading = loading;
    }


    /**


     * Visszaadja, hogy rendelkezésre áll-e használható cache.


     * @return rendelkezésre áll-e használható cache.


     */


    public boolean isReady() {
        return ready;
    }


    /**


     * Beállítja: rendelkezésre áll-e használható cache.


     * @param ready rendelkezésre áll-e használható cache.


     */


    public void setReady(boolean ready) {
        this.ready = ready;
    }


    /**


     * Visszaadja: az aktuális indexelési fázis leírása.


     * @return az aktuális indexelési fázis leírása.


     */


    public String getPhase() {
        return phase;
    }


    /**


     * Beállítja: az aktuális indexelési fázis leírása.


     * @param phase az aktuális indexelési fázis leírása.


     */


    public void setPhase(String phase) {
        this.phase = phase;
    }


    /**


     * Visszaadja: a feldolgozott XSD fájlok száma.


     * @return a feldolgozott XSD fájlok száma.


     */


    public int getProcessedFiles() {
        return processedFiles;
    }


    /**


     * Beállítja: a feldolgozott XSD fájlok száma.


     * @param processedFiles a feldolgozott XSD fájlok száma.


     */


    public void setProcessedFiles(int processedFiles) {
        this.processedFiles = processedFiles;
    }


    /**


     * Visszaadja: az összes feldolgozandó XSD fájl száma.


     * @return az összes feldolgozandó XSD fájl száma.


     */


    public int getTotalFiles() {
        return totalFiles;
    }


    /**


     * Beállítja: az összes feldolgozandó XSD fájl száma.


     * @param totalFiles az összes feldolgozandó XSD fájl száma.


     */


    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }


    /**


     * Visszaadja: az indexelés százalékos előrehaladása.


     * @return az indexelés százalékos előrehaladása.


     */


    public int getPercentage() {
        return percentage;
    }


    /**


     * Beállítja: az indexelés százalékos előrehaladása.


     * @param percentage az indexelés százalékos előrehaladása.


     */


    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }


    /**


     * Visszaadja: a cache-bejegyzések száma.


     * @return a cache-bejegyzések száma.


     */


    public int getCacheEntryCount() {
        return cacheEntryCount;
    }


    /**


     * Beállítja: a cache-bejegyzések száma.


     * @param cacheEntryCount a cache-bejegyzések száma.


     */


    public void setCacheEntryCount(int cacheEntryCount) {
        this.cacheEntryCount = cacheEntryCount;
    }


    /**


     * Visszaadja: az aktív nyomtatvány-XSD gyökérkönyvtár.


     * @return az aktív nyomtatvány-XSD gyökérkönyvtár.


     */


    public String getActiveSchemaRootDir() {
        return activeSchemaRootDir;
    }


    /**


     * Beállítja: az aktív nyomtatvány-XSD gyökérkönyvtár.


     * @param activeSchemaRootDir az aktív nyomtatvány-XSD gyökérkönyvtár.


     */


    public void setActiveSchemaRootDir(String activeSchemaRootDir) {
        this.activeSchemaRootDir = activeSchemaRootDir;
    }


    /**


     * Visszaadja: az aktív közös XSD gyökérkönyvtár.


     * @return az aktív közös XSD gyökérkönyvtár.


     */


    public String getActiveGeneralXsdDir() {
        return activeGeneralXsdDir;
    }


    /**


     * Beállítja: az aktív közös XSD gyökérkönyvtár.


     * @param activeGeneralXsdDir az aktív közös XSD gyökérkönyvtár.


     */


    public void setActiveGeneralXsdDir(String activeGeneralXsdDir) {
        this.activeGeneralXsdDir = activeGeneralXsdDir;
    }
}
