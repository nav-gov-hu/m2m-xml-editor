package hu.gov.nav.xsdparsertool.uimodel.service;

import hu.gov.nav.xsdparsertool.core.model.definition.BlockDefinition;
import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;
import hu.gov.nav.xsdparsertool.core.model.definition.FieldDefinition;
import hu.gov.nav.xsdparsertool.uimodel.model.UiModelMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * XML formátumú UIModel állományok feldolgozását és az azokból kinyert metaadatok alkalmazását végző szolgáltatás.
 *
 * <p>A parser a dokumentum fejlécét, a menü- vagy asszisztensszekciókat, a mezőcsoportokat és a mezők
 * megjelenítési tulajdonságait olvassa ki. A feldolgozás biztonságosan konfigurált DOM parserrel történik:
 * külső DTD-k, külső entitások és külső sémák betöltése tiltott.</p>
 *
 * <p>Az {@link #applyUiModel(DocumentDefinition, Path)} művelet az így előállított metaadatokat az XSD-ből
 * létrejött dokumentumdefinícióra vezeti rá. A UIModel mezőazonosítók alapján egészíti ki a mezők címkéjét,
 * típusát, maszkját és maximális hosszát, a mezőcsoportok címét pedig a megfelelő blokkokra alkalmazza.</p>
 */
public class XmlUiModelParserService implements UiModelParserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(XmlUiModelParserService.class);
    private static final int MAX_DIRECT_CHILD_SCAN_COUNT = 10_000;

    /**
     * Beolvassa és strukturált metaadatmodellé alakítja a megadott UIModel XML állományt.
     *
     * <p>Ha az állomány nem tartalmaz {@code Form} elemet, a dokumentumszintű metaadatokat akkor is
     * visszaadja. Menüszekciók jelenléte esetén azok határozzák meg a szekciókat; ellenkező esetben
     * az {@code Assistant} elemekből épül fel a szekciólista.</p>
     *
     * @param uiModelFile a feldolgozandó UIModel XML állomány
     * @return a UIModelből kiolvasott dokumentum-, szekció-, mezőcsoport- és mezőmetaadatok
     * @throws IllegalStateException ha az UIModel nem olvasható vagy XML-ként nem dolgozható fel
     */
    public UiModelMetadata parse(Path uiModelFile) {
        try {
            LOGGER.debug("UI model parse started.");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setNamespaceAware(false);
            Document doc = factory.newDocumentBuilder().parse(uiModelFile.toFile());
            Element root = doc.getDocumentElement();
            Element form = firstChild(root, "Form");
            LOGGER.debug("UI model root loaded.");
            UiModelMetadata meta = new UiModelMetadata();
            meta.setDocumentId(firstNonBlank(root.getAttribute("mainDocumentId"), root.getAttribute("id")));
            meta.setTitle(firstNonBlank(root.getAttribute("webName"), root.getAttribute("name")));
            meta.setInfo(resolveUiModelInfo(root));
            String major = root.getAttribute("major");
            String minor = root.getAttribute("minor");
            if (!major.isBlank() || !minor.isBlank()) {
                meta.setVersion((major.isBlank() ? "0" : major) + "." + (minor.isBlank() ? "0" : minor));
            }
            meta.setType(root.getAttribute("tipus"));
            if (form == null) {
                LOGGER.debug("UI model parse finished without <Form>.");
                return meta;
            }

            boolean hasMenuSections = parseMenuSections(meta, root);
            NodeList children = form.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (!(node instanceof Element element)) continue;
                switch (element.getTagName()) {
                    case "Assistant" -> {
                        if (!hasMenuSections) parseAssistant(meta, element);
                    }
                    case "FieldGroup" -> parseFieldGroup(meta, element);
                    default -> { }
                }
            }
            LOGGER.debug("UI model parse finished. sections={} blockGroups={} fields={}",
                    meta.getSections().size(), meta.getBlockGroupsById().size(), meta.getFieldsById().size());
            return meta;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse UI model: " + uiModelFile, e);
        }
    }



    /**
     * A UIModel metaadataival kiegészíti az XSD-ből már felépített dokumentumdefiníciót.
     *
     * <p>A művelet a dokumentum címét, a blokkcímeket, valamint a mezők UI-címkéjét, típusát,
     * maszkját és maximális hosszát módosíthatja. Az XSD-ben és UIModelben eltérően szereplő
     * blokk- és mezőazonosítókról naplóbejegyzést készít.</p>
     *
     * @param definition a módosítandó, XSD-alapú dokumentumdefiníció
     * @param uiModelFile az alkalmazandó UIModel XML állomány
     * @throws IllegalStateException ha a UIModel feldolgozása sikertelen
     */
    @Override
    public void applyUiModel(DocumentDefinition definition, Path uiModelFile) {
        LOGGER.debug("Applying UI model to document definition. blocks={}", definition.getBlocks() == null ? 0 : definition.getBlocks().size());
        UiModelMetadata meta = parse(uiModelFile);
        LOGGER.debug("Applying parsed UI model meta. title={} sections={} blockGroups={} fields={}", meta.getTitle(), meta.getSections().size(), meta.getBlockGroupsById().size(), meta.getFieldsById().size());
        if (meta.getTitle() != null && !meta.getTitle().isBlank()) {
            definition.setTitle(meta.getTitle());
        }
        Map<String, BlockDefinition> blocksById = new LinkedHashMap<>();
        Set<String> normalizedBlockIds = new LinkedHashSet<>();
        Set<String> xsdFieldIds = new LinkedHashSet<>();
        if (definition.getBlocks() != null) {
            for (BlockDefinition block : definition.getBlocks()) {
                LOGGER.debug("Applying UI model to XSD block. blockId={} originalName={} originalTitle={} fieldCount={}",
                        block.getId(), block.getName(), block.getTitle(), block.getFields() == null ? 0 : block.getFields().size());
                blocksById.put(block.getId(), block);
                normalizedBlockIds.add(normalizeGroupId(block.getId()));
                UiModelMetadata.BlockGroup groupMeta = meta.getBlockGroupsById().get(normalizeGroupId(block.getId()));
                if (groupMeta != null) {
                    LOGGER.debug("Matched XSD block to UI model block group. xsdBlockId={} normalizedGroupId={} uiGroupTitle={} uiFieldIds={}",
                            block.getId(), normalizeGroupId(block.getId()), groupMeta.getTitle(), groupMeta.getFieldIds());
                } else {
                    LOGGER.debug("No UI model block group found for XSD block. xsdBlockId={} normalizedGroupId={}", block.getId(), normalizeGroupId(block.getId()));
                }
                if (groupMeta != null && groupMeta.getTitle() != null && !groupMeta.getTitle().isBlank()) {
                    block.setName(groupMeta.getTitle());
                    block.setTitle(groupMeta.getTitle());
                }
                if (block.getFields() != null) {
                    for (FieldDefinition field : block.getFields()) {
                        xsdFieldIds.add(field.getId());
                        UiModelMetadata.FieldUi fieldUi = meta.getFieldsById().get(field.getId());
                        if (fieldUi != null) {
                            LOGGER.debug("Applying UI model field metadata. fieldId={} uiLabel={} uiType={} uiMask={} uiMaxLength={} uiKind={} uiLayoutWidth={}",
                                    field.getId(), fieldUi.getLabel(), fieldUi.getType(), fieldUi.getMask(), fieldUi.getMaxLength(), fieldUi.getKind(), fieldUi.getLayoutWidth());
                            if (fieldUi.getLabel() != null && !fieldUi.getLabel().isBlank()) {
                                field.setUiLabel(fieldUi.getLabel().trim());
                                field.setLabel(fieldUi.getLabel().trim());
                            }
                            if (fieldUi.getType() != null && !fieldUi.getType().isBlank()) field.setDataType(fieldUi.getType());
                            if (fieldUi.getMask() != null && !fieldUi.getMask().isBlank()) field.setMask(fieldUi.getMask());
                            if (fieldUi.getMaxLength() != null) field.setMaxLength(fieldUi.getMaxLength());
                        } else {
                            LOGGER.debug("No UI model field metadata found for XSD field. fieldId={} xmlName={} xmlPath={}", field.getId(), field.getXmlName(), field.getXmlPath());
                        }
                    }
                }
            }
        }
        LOGGER.debug("UI model application finished indexing. xsdFieldCount={} uiFieldCount={} uiBlockGroupCount={}", xsdFieldIds.size(), meta.getFieldsById().size(), meta.getBlockGroupsById().size());
        for (String blockId : meta.getBlockGroupsById().keySet()) {
            if (!normalizedBlockIds.contains(blockId)) {
                LOGGER.warn("UI model block group not found in XSD: {}", blockId);
            }
        }
        for (String fieldId : xsdFieldIds) {
            if (!meta.getFieldsById().containsKey(fieldId)) {
                LOGGER.warn("XSD field missing from UI model: {}", fieldId);
            }
        }
        for (Map.Entry<String, UiModelMetadata.FieldUi> entry : meta.getFieldsById().entrySet()) {
            if (!xsdFieldIds.contains(entry.getKey()) && "field".equalsIgnoreCase(entry.getValue().getKind())) {
                LOGGER.info("UI model field not found in XSD: {}", entry.getKey());
            }
        }
    }


    /**
     * A gyökérelem alatti {@code MenuItem} elemekből szekciókat épít, ha azok mezőcsoportokra hivatkoznak.
     *
     * @param meta a bővítendő metaadatmodell
     * @param root a UIModel gyökéreleme
     * @return {@code true}, ha legalább egy használható menüszekció létrejött
     */
    private boolean parseMenuSections(UiModelMetadata meta, Element root) {
        boolean found = false;
        NodeList menuItems = root.getElementsByTagName("MenuItem");
        for (int i = 0; i < menuItems.getLength(); i++) {
            Node node = menuItems.item(i);
            if (!(node instanceof Element menuItem)) continue;
            java.util.List<String> blockGroupIds = new java.util.ArrayList<>();
            NodeList children = menuItem.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child instanceof Element element && "FieldGroupId".equals(element.getTagName())) {
                    String ref = firstNonBlank(element.getAttribute("id"), element.getAttribute("fid"), text(element));
                    if (ref != null && !ref.isBlank()) blockGroupIds.add(ref.trim());
                }
            }
            if (blockGroupIds.isEmpty()) continue;
            UiModelMetadata.Section section = new UiModelMetadata.Section();
            section.setId(firstNonBlank(menuItem.getAttribute("id"), "section-" + (meta.getSections().size() + 1)));
            section.setTitle(firstNonBlank(menuItem.getAttribute("name"), section.getId()));
            section.setOrder(parseInt(menuItem.getAttribute("pos"), meta.getSections().size() + 1));
            section.getBlockGroupIds().addAll(blockGroupIds);
            LOGGER.debug("UI model MenuItem parsed. menuItemId={} menuItemName={} order={} blockGroupIds={}", section.getId(), section.getTitle(), section.getOrder(), section.getBlockGroupIds());
            meta.getSections().add(section);
            found = true;
        }
        return found;
    }

    /**
     * Egy {@code Assistant} elemből szekciót és mezőcsoport-hivatkozásokat épít.
     *
     * @param meta a bővítendő metaadatmodell
     * @param assistant a feldolgozandó asszisztens elem
     */
    private void parseAssistant(UiModelMetadata meta, Element assistant) {
        UiModelMetadata.Section section = new UiModelMetadata.Section();
        section.setId(assistant.getAttribute("id"));
        section.setTitle(firstNonBlank(assistant.getAttribute("name"), assistant.getAttribute("id")));
        section.setOrder(parseInt(assistant.getAttribute("pos"), meta.getSections().size() + 1));
        NodeList children = assistant.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element && "FieldGroupId".equals(element.getTagName())) {
                String ref = firstNonBlank(element.getAttribute("id"), element.getAttribute("fid"), text(element));
                if (ref != null && !ref.isBlank()) section.getBlockGroupIds().add(ref.trim());
            }
        }
        LOGGER.debug("UI model Assistant parsed. order={} blockGroupCount={}", section.getOrder(), section.getBlockGroupIds().size());
        meta.getSections().add(section);
    }

    /**
     * Feldolgoz egy {@code FieldGroup} elemet, összegyűjti a hozzá tartozó mezőazonosítókat és mezőmetaadatokat.
     *
     * @param meta a bővítendő metaadatmodell
     * @param fieldGroup a feldolgozandó mezőcsoport elem
     */
    private void parseFieldGroup(UiModelMetadata meta, Element fieldGroup) {
        UiModelMetadata.BlockGroup group = new UiModelMetadata.BlockGroup();
        group.setId(fieldGroup.getAttribute("id"));
        group.setTitle(firstNonBlank(fieldGroup.getAttribute("name"), fieldGroup.getAttribute("id")));
        group.setOrder(parseInt(fieldGroup.getAttribute("index"), meta.getBlockGroupsById().size()));
        LOGGER.debug("UI model FieldGroup parse started. order={}", group.getOrder());
        collectFieldIds(fieldGroup, group.getFieldIds());
        collectFields(fieldGroup, meta, null, null);
        meta.getBlockGroupsById().put(group.getId(), group);
        LOGGER.debug("UI model FieldGroup parsed. fieldCount={}", group.getFieldIds().size());
    }

    /**
     * Rekurzívan összegyűjti a megadott elem alatt található {@code Fid} hivatkozások azonosítóit.
     *
     * @param parent a bejárás aktuális gyökere
     * @param target a talált azonosítókat fogadó lista
     */
    private void collectFieldIds(Element parent, java.util.List<String> target) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element element)) continue;
            if ("Fid".equals(element.getTagName())) {
                String id = firstNonBlank(element.getAttribute("id"), text(element));
                if (id != null && !id.isBlank() && !target.contains(id.trim())) target.add(id.trim());
            }
            collectFieldIds(element, target);
        }
    }

    /**
     * Rekurzívan feldolgozza a UIModel mezőit és a környező layout-elemekből örökölt megjelenítési adatokat.
     *
     * <p>Azonos mezőazonosító többszöri előfordulásakor a meglévő metaadatot csak hiányzó vagy
     * technikai jellegű értékeknél egészíti ki. A kötelező és csak olvasható jelzők összevonásakor
     * a {@code true} érték megmarad.</p>
     *
     * @param parent a bejárás aktuális gyökere
     * @param meta a bővítendő metaadatmodell
     * @param inheritedSubtitleLabel a szülő layoutból örökölt alcím, ha van
     * @param inheritedLayoutWidth a szülő layoutból örökölt szélesség, ha van
     */
    private void collectFields(Element parent, UiModelMetadata meta, String inheritedSubtitleLabel, String inheritedLayoutWidth) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element element)) continue;
            String tag = element.getTagName();

            String nextSubtitleLabel = inheritedSubtitleLabel;
            String nextLayoutWidth = firstNonBlank(element.getAttribute("grid"), inheritedLayoutWidth);
            if ("Layout".equals(tag)) {
                String layoutType = element.getAttribute("type");
                String layoutLabel = firstNonBlank(element.getAttribute("label"), inheritedSubtitleLabel);
                if (("SUBTITLE".equalsIgnoreCase(layoutType) || "TITLEGROUP".equalsIgnoreCase(layoutType)) && layoutLabel != null && !layoutLabel.isBlank()) {
                    nextSubtitleLabel = layoutLabel;
                }
            }

            if ("Field".equals(tag)) {
                UiModelMetadata.FieldUi fieldUi = new UiModelMetadata.FieldUi();
                fieldUi.setId(element.getAttribute("id"));
                String ownLabel = element.getAttribute("label");
                String resolvedLabel = resolveFieldLabel(ownLabel, inheritedSubtitleLabel);
                fieldUi.setLabel(resolvedLabel);
                fieldUi.setType(element.getAttribute("type"));
                fieldUi.setMask(element.getAttribute("mask"));
                fieldUi.setMaxLength(parseInteger(element.getAttribute("maxLength")));
                fieldUi.setLayoutWidth(parseInteger(firstNonBlank(element.getAttribute("grid"), element.getAttribute("webLength"), inheritedLayoutWidth)));
                fieldUi.setReadonly(Boolean.parseBoolean(element.getAttribute("readonly")));
                fieldUi.setRequired(Boolean.parseBoolean(element.getAttribute("mandatory")));
                fieldUi.setKind("field");
                if (!fieldUi.getId().isBlank()) {
                    UiModelMetadata.FieldUi existing = meta.getFieldsById().get(fieldUi.getId());
                    if (existing == null) {
                        meta.getFieldsById().put(fieldUi.getId(), fieldUi);
                        LOGGER.debug("UI model Field parsed and stored. fieldId={} label={} type={} required={} readonly={} mask={} maxLength={} layoutWidth={}",
                                fieldUi.getId(), fieldUi.getLabel(), fieldUi.getType(), fieldUi.isRequired(), fieldUi.isReadonly(), fieldUi.getMask(), fieldUi.getMaxLength(), fieldUi.getLayoutWidth());
                    } else {
                        String previousLabel = existing.getLabel();
                        boolean existingLabelBlank = previousLabel == null || previousLabel.isBlank();
                        boolean existingLabelTechnical = looksLikeTechnicalUiFieldLabel(previousLabel);
                        boolean newLabelPresent = fieldUi.getLabel() != null && !fieldUi.getLabel().isBlank();

                        if ((existingLabelBlank || existingLabelTechnical) && newLabelPresent) {
                            existing.setLabel(fieldUi.getLabel());
                        }
                        if ((existing.getType() == null || existing.getType().isBlank()) && fieldUi.getType() != null && !fieldUi.getType().isBlank()) {
                            existing.setType(fieldUi.getType());
                        }
                        if ((existing.getMask() == null || existing.getMask().isBlank()) && fieldUi.getMask() != null && !fieldUi.getMask().isBlank()) {
                            existing.setMask(fieldUi.getMask());
                        }
                        if (existing.getMaxLength() == null && fieldUi.getMaxLength() != null) {
                            existing.setMaxLength(fieldUi.getMaxLength());
                        }
                        if (existing.getLayoutWidth() == null && fieldUi.getLayoutWidth() != null) {
                            existing.setLayoutWidth(fieldUi.getLayoutWidth());
                        }
                        if (!existing.isReadonly() && fieldUi.isReadonly()) {
                            existing.setReadonly(true);
                        }
                        if (!existing.isRequired() && fieldUi.isRequired()) {
                            existing.setRequired(true);
                        }

                        LOGGER.debug("UI model Field merged. fieldId={} previousLabel={} incomingLabel={} finalLabel={} finalType={} finalMask={} finalMaxLength={} finalLayoutWidth={}",
                                fieldUi.getId(), previousLabel, fieldUi.getLabel(), existing.getLabel(), existing.getType(), existing.getMask(), existing.getMaxLength(), existing.getLayoutWidth());
                    }
                }
            }
            collectFields(element, meta, nextSubtitleLabel, nextLayoutWidth);
        }
    }

    /**
     * Meghatározza a mező megjelenítési címkéjét a saját címke és az örökölt alcím alapján.
     *
     * @param ownLabel a mező saját UIModel-címkéje
     * @param inheritedSubtitleLabel a környező layoutból örökölt alcím
     * @return a felhasználható címke, üres szöveg, ha egyik forrásból sem áll rendelkezésre érték
     */
    private String resolveFieldLabel(String ownLabel, String inheritedSubtitleLabel) {
        String label = firstNonBlank(ownLabel);
        String subtitle = firstNonBlank(inheritedSubtitleLabel);
        if (subtitle == null || subtitle.isBlank()) {
            return label == null ? "" : label;
        }
        if (label == null || label.isBlank() || looksLikeTechnicalUiFieldLabel(label) || startsWithNumberedTableLabel(subtitle)) {
            return subtitle;
        }
        return label;
    }

    /**
     * Megvizsgálja, hogy a címke a támogatott technikai mezőkód-formátumok egyikére hasonlít-e.
     *
     * @param value a vizsgálandó címke
     * @return {@code true}, ha az érték technikai mezőkód mintáját követi
     */
    private boolean looksLikeTechnicalUiFieldLabel(String value) {
        if (value == null) return false;
        String v = value.trim();
        return v.matches("^\\[\\d{2}\\][A-Z]{1,3}\\d{1,3}$") || v.matches("^[A-Z]{1,3}\\d{1,3}$");
    }

    /**
     * Jelzi, hogy a szöveg kétjegyű sorszámmal kezdődő táblázatos címkének felel-e meg.
     *
     * @param value a vizsgálandó szöveg
     * @return {@code true}, ha a szöveg sorszámozott táblázati címkének felel meg
     */
    private boolean startsWithNumberedTableLabel(String value) {
        return value != null && value.trim().matches("^\\d{2}\\.\\s+.*");
    }

    /**
     * Megkeresi a megadott nevű közvetlen gyermekelemet, a túl nagy közvetlen gyermeklistát pedig elutasítja.
     *
     * @param parent a szülőelem
     * @param name a keresett elem neve
     * @return az első egyező közvetlen gyermek, vagy {@code null}, ha nincs ilyen
     * @throws IllegalArgumentException ha a közvetlen gyermekek száma meghaladja a biztonsági korlátot
     */
    private Element firstChild(Element parent, String name) {
        if (parent == null || name == null || name.isBlank()) {
            return null;
        }

        NodeList nodes = parent.getChildNodes();
        int childCount = nodes.getLength();

        if (childCount > MAX_DIRECT_CHILD_SCAN_COUNT) {
            LOGGER.warn(
                    "UI model element has too many direct child nodes. childCount={} maxAllowed={}",
                    childCount,
                    MAX_DIRECT_CHILD_SCAN_COUNT
            );
            throw new IllegalArgumentException(
                    "A UI model túl sok közvetlen child node-ot tartalmaz: " + childCount
            );
        }

        for (int i = 0; i < childCount; i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && name.equals(element.getTagName())) {
                return element;
            }
        }

        return null;
    }
    /**
     * Null-biztosan kiolvassa egy DOM elem teljes szöveges tartalmát.
     *
     * @param element a vizsgált elem
     * @return az elem szövege, vagy {@code null}, ha nincs elem
     */
    private String text(Element element) { return element == null ? null : element.getTextContent(); }
    /**
     * Feloldja a fejlécben használt kiegészítő UIModel-információt.
     * Elsődlegesen az {@code info}, másodlagosan a {@code webInfo} attribútumot használja.
     *
     * @param root a UIModel gyökéreleme
     * @return az első nem üres információs érték, vagy {@code null}
     */
    private String resolveUiModelInfo(Element root) {
        /*
         * A fejlécben a UI model WebUIModel/info attribútuma a mérvadó.
         * A webInfo több űrlapnál csak a rövid azonosítót ismétli
         * (például NAV_PMT25), ezért megnevezésként félrevezető.
         */
        return firstNonBlank(root.getAttribute("info"), root.getAttribute("webInfo"));
    }

    /**
     * A megadott értékek közül sorrendben az első nem üres szöveget választja ki és trimeli.
     *
     * @param values prioritási sorrendben vizsgált szövegek
     * @return az első használható érték, vagy {@code null}
     */
    private String firstNonBlank(String... values) { for (String v : values) if (v != null && !v.isBlank()) return v.trim(); return null; }
    /**
     * Egész számmá alakítja a szöveget, és hibás vagy hiányzó értéknél a megadott fallbacket használja.
     *
     * @param value a konvertálandó szöveg
     * @param fallback a sikertelen konverzió esetén visszaadandó érték
     * @return a konvertált egész vagy a fallback
     */
    private int parseInt(String value, int fallback) { try { return Integer.parseInt(value); } catch (Exception e) { return fallback; } }
    /**
     * Opcionális egész számmá alakítja a szöveget.
     *
     * @param value a konvertálandó szöveg
     * @return a konvertált érték, vagy {@code null}, ha a bemenet üres vagy nem egész szám
     */
    private Integer parseInteger(String value) { try { return value == null || value.isBlank() ? null : Integer.valueOf(value); } catch (Exception e) { return null; } }
    /**
     * Egységesíti a blokk- és mezőcsoport-azonosítókat az opcionális {@code FieldGroup_} előtag eltávolításával.
     *
     * @param value a normalizálandó azonosító
     * @return a normalizált azonosító, vagy {@code null}, ha a bemenet {@code null}
     */
    private String normalizeGroupId(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.startsWith("FieldGroup_")) return normalized.substring("FieldGroup_".length());
        return normalized;
    }
}
