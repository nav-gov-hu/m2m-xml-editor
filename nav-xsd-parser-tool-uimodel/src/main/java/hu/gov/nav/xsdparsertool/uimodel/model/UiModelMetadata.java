package hu.gov.nav.xsdparsertool.uimodel.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Egy UIModel XML állományból kiolvasott, megjelenítéshez szükséges metaadatokat összefogó modell.
 *
 * <p>A modell a dokumentumszintű adatokat, a képernyőszekciók sorrendjét, a mezőcsoportokat
 * és az egyes mezőkhöz tartozó megjelenítési információkat tartja nyilván. A gyűjtemények
 * beszúrási sorrendet megőrző implementációt használnak, ezért a parser által felépített sorrend
 * a további feldolgozás során is rendelkezésre áll.</p>
 *
 * <p>A példányt elsősorban az UIModel parser állítja elő, majd a feldolgozó és
 * űrlapdefiníció-építő réteg használja fel.</p>
 */
public class UiModelMetadata {
    private String documentId;
    private String title;
    private String info;
    private String version;
    private String type;
    private final List<Section> sections = new ArrayList<>();
    private final Map<String, BlockGroup> blockGroupsById = new LinkedHashMap<>();
    private final Map<String, FieldUi> fieldsById = new LinkedHashMap<>();

    /**
     * Visszaadja a UIModel által azonosított dokumentum azonosítóját.
     *
     * @return a dokumentum technikai azonosítója
     */
    public String getDocumentId() { return documentId; }

    /**
     * Beállítja a UIModel által azonosított dokumentum azonosítóját.
     *
     * @param documentId a dokumentum technikai azonosítója
     */
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    /**
     * Visszaadja a dokumentum UIModelből származó megjelenítési címét.
     *
     * @return a megjelenítési cím
     */
    public String getTitle() { return title; }

    /**
     * Beállítja a dokumentum UIModelből származó megjelenítési címét.
     *
     * @param title a megjelenítési cím
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Visszaadja a UIModel fejlécéből feloldott kiegészítő információt.
     *
     * @return a kiegészítő információ
     */
    public String getInfo() { return info; }

    /**
     * Beállítja a UIModel fejlécéből feloldott kiegészítő információt.
     *
     * @param info a kiegészítő információ
     */
    public void setInfo(String info) { this.info = info; }

    /**
     * Visszaadja a UIModel fő- és alverziójából összeállított verziószámot.
     *
     * @return a UIModel verziószáma
     */
    public String getVersion() { return version; }

    /**
     * Beállítja a UIModel verziószámát.
     *
     * @param version a verziószám
     */
    public void setVersion(String version) { this.version = version; }

    /**
     * Visszaadja a UIModelben megadott dokumentumtípust.
     *
     * @return a dokumentumtípus
     */
    public String getType() { return type; }

    /**
     * Beállítja a UIModelben megadott dokumentumtípust.
     *
     * @param type a dokumentumtípus
     */
    public void setType(String type) { this.type = type; }

    /**
     * Visszaadja a megjelenítési szekciókat a parser által felépített sorrendben.
     *
     * @return a módosítható szekciólista
     */
    public List<Section> getSections() { return sections; }

    /**
     * Visszaadja a mezőcsoportokat azok UIModel-azonosítója szerint indexelve.
     *
     * @return a módosítható mezőcsoport-index
     */
    public Map<String, BlockGroup> getBlockGroupsById() { return blockGroupsById; }

    /**
     * Visszaadja a mezők megjelenítési metaadatait mezőazonosító szerint indexelve.
     *
     * @return a módosítható mezőmetaadat-index
     */
    public Map<String, FieldUi> getFieldsById() { return fieldsById; }

    /**
     * Egy UIModel menü- vagy asszisztensszekció leírása.
     * A szekció a hozzá tartozó mezőcsoportok azonosítóit sorrendhelyesen tartalmazza.
     */
    public static class Section {
        private String id;
        private String title;
        private int order;
        private final List<String> blockGroupIds = new ArrayList<>();

        /** @return a szekció technikai azonosítója */
        public String getId() { return id; }

        /** @param id a szekció technikai azonosítója */
        public void setId(String id) { this.id = id; }

        /** @return a szekció megjelenítési címe */
        public String getTitle() { return title; }

        /** @param title a szekció megjelenítési címe */
        public void setTitle(String title) { this.title = title; }

        /** @return a szekció UIModel szerinti sorrendi értéke */
        public int getOrder() { return order; }

        /** @param order a szekció UIModel szerinti sorrendi értéke */
        public void setOrder(int order) { this.order = order; }

        /** @return a szekcióhoz rendelt mezőcsoport-azonosítók módosítható listája */
        public List<String> getBlockGroupIds() { return blockGroupIds; }
    }

    /**
     * Egy UIModel {@code FieldGroup} elemének feldolgozott leírása.
     * A modell a csoport címét, sorrendjét és a csoporthoz tartozó mezőazonosítókat őrzi.
     */
    public static class BlockGroup {
        private String id;
        private String title;
        private int order;
        private final List<String> fieldIds = new ArrayList<>();

        /** @return a mezőcsoport UIModel-azonosítója */
        public String getId() { return id; }

        /** @param id a mezőcsoport UIModel-azonosítója */
        public void setId(String id) { this.id = id; }

        /** @return a mezőcsoport megjelenítési címe */
        public String getTitle() { return title; }

        /** @param title a mezőcsoport megjelenítési címe */
        public void setTitle(String title) { this.title = title; }

        /** @return a mezőcsoport UIModel szerinti sorrendi értéke */
        public int getOrder() { return order; }

        /** @param order a mezőcsoport UIModel szerinti sorrendi értéke */
        public void setOrder(int order) { this.order = order; }

        /** @return a mezőcsoporthoz tartozó mezőazonosítók módosítható listája */
        public List<String> getFieldIds() { return fieldIds; }
    }

    /**
     * Egy UIModel {@code Field} elem megjelenítési és beviteli metaadatai.
     * Az itt tárolt adatok az XSD-ből felépített meződefiníciók UIModel szerinti kiegészítésére használhatók.
     */
    public static class FieldUi {
        private String id;
        private String label;
        private String type;
        private String mask;
        private Integer maxLength;
        private Integer layoutWidth;
        private boolean readonly;
        private boolean required;
        private String kind;

        /** @return a mező UIModel-azonosítója */
        public String getId() { return id; }

        /** @param id a mező UIModel-azonosítója */
        public void setId(String id) { this.id = id; }

        /** @return a mező UIModelből feloldott felirata */
        public String getLabel() { return label; }

        /** @param label a mező megjelenítési felirata */
        public void setLabel(String label) { this.label = label; }

        /** @return a mező UIModelben megadott típusa */
        public String getType() { return type; }

        /** @param type a UIModel szerinti mezőtípus */
        public void setType(String type) { this.type = type; }

        /** @return a mezőhöz tartozó beviteli maszk */
        public String getMask() { return mask; }

        /** @param mask a mezőhöz tartozó beviteli maszk */
        public void setMask(String mask) { this.mask = mask; }

        /** @return a UIModelben megadott maximális mezőhossz, vagy {@code null} */
        public Integer getMaxLength() { return maxLength; }

        /** @param maxLength a maximális mezőhossz, vagy {@code null}, ha nincs megadva */
        public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }

        /** @return a UIModelből feloldott rács- vagy webes szélesség, vagy {@code null} */
        public Integer getLayoutWidth() { return layoutWidth; }

        /** @param layoutWidth a megjelenítési szélesség, vagy {@code null}, ha nincs megadva */
        public void setLayoutWidth(Integer layoutWidth) { this.layoutWidth = layoutWidth; }

        /** @return {@code true}, ha a UIModel a mezőt csak olvashatóként jelöli */
        public boolean isReadonly() { return readonly; }

        /** @param readonly {@code true}, ha a mező csak olvasható */
        public void setReadonly(boolean readonly) { this.readonly = readonly; }

        /** @return {@code true}, ha a UIModel a mezőt kötelezőként jelöli */
        public boolean isRequired() { return required; }

        /** @param required {@code true}, ha a mező kötelező */
        public void setRequired(boolean required) { this.required = required; }

        /** @return a feldolgozott UI elem jellege */
        public String getKind() { return kind; }

        /** @param kind a feldolgozott UI elem jellege */
        public void setKind(String kind) { this.kind = kind; }
    }
}
