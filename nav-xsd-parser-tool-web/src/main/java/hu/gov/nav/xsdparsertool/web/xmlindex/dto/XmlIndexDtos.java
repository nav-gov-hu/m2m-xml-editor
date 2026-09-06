package hu.gov.nav.xsdparsertool.web.xmlindex.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * A web modul alkalmazási területének közös alkalmazási típusa.
 *
 * <p>A {@code XmlIndexDtos} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public final class XmlIndexDtos {
    /**
     * Létrehozza a {@code XmlIndexDtos} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     */
    private XmlIndexDtos() {}

    /**
     * A webes rétegek közötti adatátadás strukturált modellje.
     *
     * <p>A {@code FormOptionDto} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public record FormOptionDto(String formName, String label, List<String> versions, boolean configured) {}
    /**
     * A webes rétegek közötti adatátadás strukturált modellje.
     *
     * <p>A {@code FormsResponse} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public record FormsResponse(List<FormOptionDto> forms, String configPath) {}

    /**
     * A webes rétegek közötti adatátadás strukturált modellje.
     *
     * <p>A {@code FormPartDto} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class FormPartDto {
        private String name;
        private String label;
        private String xmlPath;
        private String role = "SINGLE";
        private Integer minOccurs;
        private String maxOccurs;
        private int fieldCount;
        private boolean configured;

        /**
         * A {@code getName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getName() { return name; }
        /**
         * A {@code setName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param name a feloldáshoz vagy azonosításhoz használt név
         */
        public void setName(String name) { this.name = name; }
        /**
         * A {@code getLabel} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getLabel() { return label; }
        /**
         * A {@code setLabel} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param label a művelet bemeneti {@code label} értéke
         */
        public void setLabel(String label) { this.label = label; }
        /**
         * A {@code getXmlPath} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>Az XML-adatot a alkalmazási folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getXmlPath() { return xmlPath; }
        /**
         * A {@code setXmlPath} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>Az XML-adatot a alkalmazási folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
         * @param xmlPath a feldolgozandó XML-hez tartozó adat vagy tartalom
         */
        public void setXmlPath(String xmlPath) { this.xmlPath = xmlPath; }
        /**
         * A {@code getRole} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getRole() { return role; }
        /**
         * A {@code setRole} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
         * @param role a művelet bemeneti {@code role} értéke
         */
        public void setRole(String role) { this.role = role; }
        /**
         * A {@code getMinOccurs} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public Integer getMinOccurs() { return minOccurs; }
        /**
         * A {@code setMinOccurs} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param minOccurs a művelet bemeneti {@code minOccurs} értéke
         */
        public void setMinOccurs(Integer minOccurs) { this.minOccurs = minOccurs; }
        /**
         * A {@code getMaxOccurs} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getMaxOccurs() { return maxOccurs; }
        /**
         * A {@code setMaxOccurs} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param maxOccurs a művelet bemeneti {@code maxOccurs} értéke
         */
        public void setMaxOccurs(String maxOccurs) { this.maxOccurs = maxOccurs; }
        /**
         * A {@code getFieldCount} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public int getFieldCount() { return fieldCount; }
        /**
         * A {@code setFieldCount} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param fieldCount a művelet bemeneti {@code fieldCount} értéke
         */
        public void setFieldCount(int fieldCount) { this.fieldCount = fieldCount; }
        /**
         * A {@code isConfigured} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        public boolean isConfigured() { return configured; }
        /**
         * A {@code setConfigured} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
         * @param configured a művelethez szükséges konfigurációs adatok
         */
        public void setConfigured(boolean configured) { this.configured = configured; }
    }

    /**
     * A webes rétegek közötti adatátadás strukturált modellje.
     *
     * <p>A {@code IndexFieldDto} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class IndexFieldDto {
        private String name;
        private String label;
        private String xmlPath;
        private String formPartName;
        private String formPartRole;
        private String parentInfo;
        private boolean searchable;
        private boolean display;
        private boolean defaultSearch;
        private String matchMode = "contains";

        /**
         * A {@code getName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getName() { return name; }
        /**
         * A {@code setName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param name a feloldáshoz vagy azonosításhoz használt név
         */
        public void setName(String name) { this.name = name; }
        /**
         * A {@code getLabel} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getLabel() { return label; }
        /**
         * A {@code setLabel} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param label a művelet bemeneti {@code label} értéke
         */
        public void setLabel(String label) { this.label = label; }
        /**
         * A {@code getXmlPath} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>Az XML-adatot a alkalmazási folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getXmlPath() { return xmlPath; }
        /**
         * A {@code setXmlPath} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>Az XML-adatot a alkalmazási folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
         * @param xmlPath a feldolgozandó XML-hez tartozó adat vagy tartalom
         */
        public void setXmlPath(String xmlPath) { this.xmlPath = xmlPath; }
        /**
         * A {@code getFormPartName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getFormPartName() { return formPartName; }
        /**
         * A {@code setFormPartName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param formPartName a feloldáshoz vagy azonosításhoz használt név
         */
        public void setFormPartName(String formPartName) { this.formPartName = formPartName; }
        /**
         * A {@code getFormPartRole} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getFormPartRole() { return formPartRole; }
        /**
         * A {@code setFormPartRole} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
         * @param formPartRole a művelet bemeneti {@code formPartRole} értéke
         */
        public void setFormPartRole(String formPartRole) { this.formPartRole = formPartRole; }
        /**
         * A {@code getParentInfo} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getParentInfo() { return parentInfo; }
        /**
         * A {@code setParentInfo} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param parentInfo a művelet bemeneti {@code parentInfo} értéke
         */
        public void setParentInfo(String parentInfo) { this.parentInfo = parentInfo; }
        /**
         * A {@code isSearchable} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        public boolean isSearchable() { return searchable; }
        /**
         * A {@code setSearchable} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param searchable a művelet bemeneti {@code searchable} értéke
         */
        public void setSearchable(boolean searchable) { this.searchable = searchable; }
        /**
         * A {@code isDisplay} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        public boolean isDisplay() { return display; }
        /**
         * A {@code setDisplay} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param display a művelet bemeneti {@code display} értéke
         */
        public void setDisplay(boolean display) { this.display = display; }
        /**
         * A {@code isDefaultSearch} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        public boolean isDefaultSearch() { return defaultSearch; }
        /**
         * A {@code setDefaultSearch} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param defaultSearch a művelet bemeneti {@code defaultSearch} értéke
         */
        public void setDefaultSearch(boolean defaultSearch) { this.defaultSearch = defaultSearch; }
        /**
         * A {@code getMatchMode} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getMatchMode() { return matchMode; }
        /**
         * A {@code setMatchMode} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param matchMode a művelet bemeneti {@code matchMode} értéke
         */
        public void setMatchMode(String matchMode) { this.matchMode = matchMode; }
    }

    /**
     * Legacy DTO kept for backwards compatibility with the first Sprint 11 configuration shape.
     * New configurations are field/path based and use IndexFormConfigDto.fields.
     */
    public static class IndexChainDto {
        private String name;
        private String label;
        private String xmlPath;
        private List<IndexFieldDto> fields = new ArrayList<>();

        /**
         * A {@code getName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getName() { return name; }
        /**
         * A {@code setName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param name a feloldáshoz vagy azonosításhoz használt név
         */
        public void setName(String name) { this.name = name; }
        /**
         * A {@code getLabel} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getLabel() { return label; }
        /**
         * A {@code setLabel} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param label a művelet bemeneti {@code label} értéke
         */
        public void setLabel(String label) { this.label = label; }
        /**
         * A {@code getXmlPath} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>Az XML-adatot a alkalmazási folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getXmlPath() { return xmlPath; }
        /**
         * A {@code setXmlPath} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>Az XML-adatot a alkalmazási folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
         * @param xmlPath a feldolgozandó XML-hez tartozó adat vagy tartalom
         */
        public void setXmlPath(String xmlPath) { this.xmlPath = xmlPath; }
        /**
         * A {@code getFields} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a művelet eredményeként előállított elemek listája
         */
        public List<IndexFieldDto> getFields() { return fields; }
        /**
         * A {@code setFields} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param fields a feldolgozandó elemek kollekciója
         */
        public void setFields(List<IndexFieldDto> fields) { this.fields = fields == null ? new ArrayList<>() : fields; }
    }

    /**
     * A webes rétegek közötti adatátadás strukturált modellje.
     *
     * <p>A {@code IndexTreeNodeDto} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class IndexTreeNodeDto {
        private String name;
        private String label;
        private String xmlPath;
        private String type;
        private boolean configurable;
        private List<IndexTreeNodeDto> children = new ArrayList<>();

        /**
         * A {@code getName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getName() { return name; }
        /**
         * A {@code setName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param name a feloldáshoz vagy azonosításhoz használt név
         */
        public void setName(String name) { this.name = name; }
        /**
         * A {@code getLabel} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getLabel() { return label; }
        /**
         * A {@code setLabel} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param label a művelet bemeneti {@code label} értéke
         */
        public void setLabel(String label) { this.label = label; }
        /**
         * A {@code getXmlPath} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>Az XML-adatot a alkalmazási folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getXmlPath() { return xmlPath; }
        /**
         * A {@code setXmlPath} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>Az XML-adatot a alkalmazási folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
         * @param xmlPath a feldolgozandó XML-hez tartozó adat vagy tartalom
         */
        public void setXmlPath(String xmlPath) { this.xmlPath = xmlPath; }
        /**
         * A {@code getType} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getType() { return type; }
        /**
         * A {@code setType} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param type a művelet bemeneti {@code type} értéke
         */
        public void setType(String type) { this.type = type; }
        /**
         * A {@code isConfigurable} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
         *
         * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
         * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
         */
        public boolean isConfigurable() { return configurable; }
        /**
         * A {@code setConfigurable} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
         * @param configurable a művelethez szükséges konfigurációs adatok
         */
        public void setConfigurable(boolean configurable) { this.configurable = configurable; }
        /**
         * A {@code getChildren} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a művelet eredményeként előállított elemek listája
         */
        public List<IndexTreeNodeDto> getChildren() { return children; }
        /**
         * A {@code setChildren} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param children a feldolgozandó elemek kollekciója
         */
        public void setChildren(List<IndexTreeNodeDto> children) { this.children = children == null ? new ArrayList<>() : children; }
    }

    /**
     * A webes rétegek közötti adatátadás strukturált modellje.
     *
     * <p>A {@code IndexFormConfigDto} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class IndexFormConfigDto {
        private String formName;
        private String label;
        private String structureSourceVersion;
        private List<IndexFieldDto> fields = new ArrayList<>();
        private List<IndexChainDto> chains = new ArrayList<>();

        /**
         * A {@code getFormName} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getFormName() { return formName; }
        /**
         * A {@code setFormName} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param formName a feloldáshoz vagy azonosításhoz használt név
         */
        public void setFormName(String formName) { this.formName = formName; }
        /**
         * A {@code getLabel} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getLabel() { return label; }
        /**
         * A {@code setLabel} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param label a művelet bemeneti {@code label} értéke
         */
        public void setLabel(String label) { this.label = label; }
        /**
         * A {@code getStructureSourceVersion} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a feloldott vagy lekért érték
         */
        public String getStructureSourceVersion() { return structureSourceVersion; }
        /**
         * A {@code setStructureSourceVersion} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param structureSourceVersion a művelet bemeneti {@code structureSourceVersion} értéke
         */
        public void setStructureSourceVersion(String structureSourceVersion) { this.structureSourceVersion = structureSourceVersion; }
        /**
         * A {@code getFields} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a művelet eredményeként előállított elemek listája
         */
        public List<IndexFieldDto> getFields() { return fields; }
        /**
         * A {@code setFields} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param fields a feldolgozandó elemek kollekciója
         */
        public void setFields(List<IndexFieldDto> fields) { this.fields = fields == null ? new ArrayList<>() : fields; }
        /**
         * A {@code getChains} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @return a művelet eredményeként előállított elemek listája
         */
        public List<IndexChainDto> getChains() { return chains; }
        /**
         * A {@code setChains} művelet frissíti a kapcsolódó állapotot a megadott adatok alapján.
         *
         * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
         * @param chains a feldolgozandó elemek kollekciója
         */
        public void setChains(List<IndexChainDto> chains) { this.chains = chains == null ? new ArrayList<>() : chains; }
    }

    /**
     * A webes rétegek közötti adatátadás strukturált modellje.
     *
     * <p>A {@code StructureResponse} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public record StructureResponse(String formName,
                                    String label,
                                    String sourceVersion,
                                    String xsdPath,
                                    List<FormPartDto> formParts,
                                    List<IndexTreeNodeDto> tree,
                                    List<IndexFieldDto> fields,
                                    List<IndexChainDto> chains,
                                    IndexFormConfigDto savedConfig) {}
    /**
     * A webes rétegek közötti adatátadás strukturált modellje.
     *
     * <p>A {@code SaveResponse} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public record SaveResponse(String formName, String configPath, int chainCount, int fieldCount) {}
}
