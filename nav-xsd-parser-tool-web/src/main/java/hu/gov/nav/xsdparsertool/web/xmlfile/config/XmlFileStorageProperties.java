package hu.gov.nav.xsdparsertool.web.xmlfile.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A külső konfiguráció kapcsolódó beállításait típusosan összefogó konfigurációs modell.
 *
 * <p>A {@code XmlFileStorageProperties} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@ConfigurationProperties(prefix = "nav.xsdparsertool.xml-file")
public class XmlFileStorageProperties {
    private String uploadDir;
    private String archiveDir;
    private String backupDir;
    private String xmlIndexDir;
    private final ServerBrowser serverBrowser = new ServerBrowser();
    private final ServerImport serverImport = new ServerImport();
    private final LargeFile largeFile = new LargeFile();

    /**
     * A {@code getUploadDir} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getUploadDir() {
        return uploadDir;
    }

    /**
     * A {@code setUploadDir} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param uploadDir a művelet bemeneti {@code uploadDir} értéke
     */
    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    /**
     * A {@code getArchiveDir} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getArchiveDir() {
        return archiveDir;
    }

    /**
     * A {@code setArchiveDir} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param archiveDir a művelet bemeneti {@code archiveDir} értéke
     */
    public void setArchiveDir(String archiveDir) {
        this.archiveDir = archiveDir;
    }

    /**
     * A {@code getBackupDir} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getBackupDir() {
        return backupDir;
    }

    /**
     * A {@code setBackupDir} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param backupDir a művelet bemeneti {@code backupDir} értéke
     */
    public void setBackupDir(String backupDir) {
        this.backupDir = backupDir;
    }

    /**
     * A {@code getXmlIndexDir} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @return a feloldott vagy lekért érték
     */
    public String getXmlIndexDir() {
        return xmlIndexDir;
    }

    /**
     * A {@code setXmlIndexDir} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlIndexDir a feldolgozandó XML-hez tartozó adat vagy tartalom
     */
    public void setXmlIndexDir(String xmlIndexDir) {
        this.xmlIndexDir = xmlIndexDir;
    }

    /**
     * A {@code getServerBrowser} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public ServerBrowser getServerBrowser() {
        return serverBrowser;
    }

    /**
     * A {@code getServerImport} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a feloldott vagy lekért érték
     */
    public ServerImport getServerImport() {
        return serverImport;
    }

    /**
     * A {@code getLargeFile} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a feloldott vagy lekért érték
     */
    public LargeFile getLargeFile() {
        return largeFile;
    }

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code LargeFile} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class LargeFile {
        private String threshold = "20 MB";
        private boolean disableXmlTree = true;
        private boolean disableXmlSource = true;

        /**
         * Large XML detection threshold. Accepted format: integer value + unit, for example
         * "1 KB", "1 MB", "234 MB" or "1 GB". Decimal values are intentionally not
         * accepted so that operational configuration stays unambiguous.
         */
        public String getThreshold() { return threshold; }
        /**
         * A {@code setThreshold} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param threshold a művelet bemeneti {@code threshold} értéke
         */
        public void setThreshold(String threshold) { this.threshold = threshold; }
        /**
         * A {@code isDisableXmlTree} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        public boolean isDisableXmlTree() { return disableXmlTree; }
        /**
         * A {@code setDisableXmlTree} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
         * @param disableXmlTree a feldolgozandó XML-hez tartozó adat vagy tartalom
         */
        public void setDisableXmlTree(boolean disableXmlTree) { this.disableXmlTree = disableXmlTree; }
        /**
         * A {@code isDisableXmlSource} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        public boolean isDisableXmlSource() { return disableXmlSource; }
        /**
         * A {@code setDisableXmlSource} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
         * @param disableXmlSource a feldolgozandó XML-hez tartozó adat vagy tartalom
         */
        public void setDisableXmlSource(boolean disableXmlSource) { this.disableXmlSource = disableXmlSource; }

        /**
         * A {@code thresholdBytes} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
         *
         * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a művelet feldolgozási eredménye
         */
        public long thresholdBytes() {
            return parseThresholdToBytes(threshold);
        }

        /**
         * A {@code parseThresholdToBytes} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
         *
         * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param rawThreshold a művelet bemeneti {@code rawThreshold} értéke
         * @return a művelet feldolgozási eredménye
         */
        private static long parseThresholdToBytes(String rawThreshold) {
            String raw = rawThreshold == null ? "" : rawThreshold.trim();
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("^(\\d+)\\s*(KB|MB|GB)$", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(raw);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                        "Invalid large file threshold: '" + rawThreshold + "'. Expected format: integer value with unit, for example '1 KB', '1 MB', '234 MB' or '1 GB'.");
            }

            long value = Long.parseLong(matcher.group(1));
            if (value <= 0L) {
                throw new IllegalArgumentException(
                        "Invalid large file threshold: '" + rawThreshold + "'. Value must be greater than zero.");
            }

            String unit = matcher.group(2).toUpperCase(java.util.Locale.ROOT);
            long multiplier;
            switch (unit) {
                case "KB":
                    multiplier = 1024L;
                    break;
                case "MB":
                    multiplier = 1024L * 1024L;
                    break;
                case "GB":
                    multiplier = 1024L * 1024L * 1024L;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported large file threshold unit: " + unit);
            }
            return Math.multiplyExact(value, multiplier);
        }
    }

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code ServerImport} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class ServerImport {
        private String rootDir;

        /**
         * A {@code getRootDir} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getRootDir() {
            return rootDir;
        }

        /**
         * A {@code setRootDir} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param rootDir a művelet bemeneti {@code rootDir} értéke
         */
        public void setRootDir(String rootDir) {
            this.rootDir = rootDir;
        }
    }

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code ServerBrowser} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class ServerBrowser {
        private boolean enabled = true;
        private boolean autoRegisterEnabled = true;
        private boolean autoRegisterOnStartup = true;
        private long autoRegisterIntervalMs = 30000L;

        /**
         * A {@code isEnabled} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * A {@code setEnabled} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param enabled a művelet bemeneti {@code enabled} értéke
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }


        /**
         * A {@code isAutoRegisterEnabled} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        public boolean isAutoRegisterEnabled() {
            return autoRegisterEnabled;
        }

        /**
         * A {@code setAutoRegisterEnabled} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param autoRegisterEnabled a művelet bemeneti {@code autoRegisterEnabled} értéke
         */
        public void setAutoRegisterEnabled(boolean autoRegisterEnabled) {
            this.autoRegisterEnabled = autoRegisterEnabled;
        }

        /**
         * A {@code isAutoRegisterOnStartup} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        public boolean isAutoRegisterOnStartup() {
            return autoRegisterOnStartup;
        }

        /**
         * A {@code setAutoRegisterOnStartup} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param autoRegisterOnStartup a művelet bemeneti {@code autoRegisterOnStartup} értéke
         */
        public void setAutoRegisterOnStartup(boolean autoRegisterOnStartup) {
            this.autoRegisterOnStartup = autoRegisterOnStartup;
        }

        /**
         * A {@code getAutoRegisterIntervalMs} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public long getAutoRegisterIntervalMs() {
            return autoRegisterIntervalMs;
        }

        /**
         * A {@code setAutoRegisterIntervalMs} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param autoRegisterIntervalMs a művelet bemeneti {@code autoRegisterIntervalMs} értéke
         */
        public void setAutoRegisterIntervalMs(long autoRegisterIntervalMs) {
            this.autoRegisterIntervalMs = autoRegisterIntervalMs;
        }
    }
}
