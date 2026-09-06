
package hu.gov.nav.xsdparsertool.xsd.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;
import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.definition.BlockDefinition;
import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;
import hu.gov.nav.xsdparsertool.core.model.definition.FieldDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Az XSD-sémákból a szerkesztő és megjelenítő rétegek által használható {@link DocumentDefinition}
 * modellt előállító alapértelmezett parser implementáció.
 *
 * <p>A feldolgozás a {@link SchemaBundle} elsődleges és kapcsolódó XSD-fájljait közös indexbe
 * olvassa, majd a dokumentum gyökérelemétől kiindulva feltérképezi a blokk-, mezőcsoport- és
 * mezőstruktúrát. A parser az XSD annotációkból címkéket, a simple type megszorításokból
 * adattípust és enumértékeket, az occurrence attribútumokból pedig kötelezőségi és ismétlődési
 * információt állít elő.</p>
 *
 * <p>Az osztály nem Spring bean önmagában; a hívó réteg a {@link XsdParserService} szerződésen
 * keresztül használja. Az XSD-k XML-feldolgozása külső entitások és külső sémák elérésének
 * tiltásával történik.</p>
 */
public class BasicXsdParserService implements XsdParserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicXsdParserService.class);

    /**
     * A megadott séma-csomagból felépíti a dokumentum szerkezeti definícióját.
     *
     * <p>Először közös indexet készít az elsődleges és a csomagban felsorolt XSD-fájlok globális
     * elemeiből, complex type és simple type definícióiból. Ezután a konfigurált gyökérelemet
     * keresi; ha az nincs az indexben, az első globális elemet használja fallbackként.</p>
     *
     * <p>Complex type nélküli gyökér esetén egyetlen gyökérblokkot hoz létre. Normál esetben
     * rekurzívan járja be a struktúrát, és a {@code FieldGroup_}, {@code Block_} és mezőelemekből
     * építi fel a {@link DocumentDefinition} tartalmát. Ha a bejárás nem eredményez blokkot,
     * a gyökér complex type mezőiből tartalék blokk készül.</p>
     *
     * @param bundle a feldolgozandó séma-csomag; az elsődleges XSD megadása kötelező
     * @return az XSD alapján összeállított dokumentumdefiníció
     * @throws IllegalArgumentException ha a csomag vagy annak elsődleges XSD-je hiányzik
     * @throws IllegalStateException ha valamely feldolgozott XSD nem olvasható vagy nem parse-olható
     */
    @Override
    public DocumentDefinition parse(SchemaBundle bundle) {
        if (bundle == null || bundle.getPrimaryXsd() == null) {
            throw new IllegalArgumentException("SchemaBundle and primaryXsd must not be null");
        }

        XsdIndex index = buildIndex(bundle);
        DocumentDefinition definition = new DocumentDefinition();
        definition.setId(firstNonBlank(bundle.getDocumentType(), stripExtension(bundle.getPrimaryXsd().getFileName().toString())));
        definition.setName(firstNonBlank(bundle.getDocumentType(), stripExtension(bundle.getPrimaryXsd().getFileName().toString())));
        definition.setTitle(firstNonBlank(bundle.getDocumentType(), stripExtension(bundle.getPrimaryXsd().getFileName().toString())));
        definition.setRootElementName(bundle.getRootElementName());
        definition.setTargetNamespace(bundle.getTargetNamespace());

        Element rootElement = findRootElement(index, bundle);
        if (rootElement == null) {
            LOGGER.warn("XSD parse: no root element found for documentType={}, rootElementName={}", bundle.getDocumentType(), bundle.getRootElementName());
            return definition;
        }

        LOGGER.info("XSD parse start: rootElementName={}, documentType={}, primaryXsd={}",
                getElementName(rootElement), bundle.getDocumentType(), bundle.getPrimaryXsd());

        Element complexType = resolveComplexTypeForElement(rootElement, index);
        if (complexType == null) {
            BlockDefinition block = new BlockDefinition();
            block.setId("root");
            block.setName("root");
            block.setTitle(firstNonBlank(bundle.getRootElementName(), "Root"));
            FieldDefinition field = createField(rootElement, "/" + getElementName(rootElement), index);
            block.getFields().add(field);
            definition.getBlocks().add(block);
            LOGGER.info("XSD parse fallback single-root block: id={}, fieldCount={}", block.getId(), block.getFields().size());
            return definition;
        }

        walkElementsForBlocks(complexType, "/" + getElementName(rootElement), index, definition);

        if (definition.getBlocks().isEmpty()) {
            BlockDefinition fallback = new BlockDefinition();
            fallback.setId("root-block");
            fallback.setName("root-block");
            fallback.setTitle(firstNonBlank(bundle.getRootElementName(), "Root"));
            appendFieldsFromComplexType(fallback, complexType, "/" + getElementName(rootElement), index);
            if (!fallback.getFields().isEmpty()) {
                definition.getBlocks().add(fallback);
            }
            LOGGER.warn("XSD parse fallback root-block used: fieldCount={}", fallback.getFields().size());
        }

        LOGGER.info("XSD documentDefinition blocks summary START");
        for (BlockDefinition block : definition.getBlocks()) {
            LOGGER.debug("XSD block summary: id={}, name={}, title={}, fieldCount={}",
                    block.getId(), block.getName(), block.getTitle(),
                    block.getFields() == null ? 0 : block.getFields().size());
        }
        LOGGER.info("XSD documentDefinition blocks summary END");

        return definition;
    }

    /**
     * Bejárja egy complex type közvetlen kompozitorait, és az azokban található XSD-elemeket szerkezeti elemként feldolgozza.
     *
     * <p>Csak a {@code sequence}, {@code all} és {@code choice} kompozitorok közvetlen {@code element} gyermekeit adja tovább; a további rekurzióról a szerkezeti elem feldolgozása gondoskodik.</p>
     *
     * @param container a bejárandó complex type vagy más XSD-konténer
     * @param parentPath az aktuális konténer XML-útvonala
     * @param index a feloldáshoz használt XSD-index
     * @param definition a folyamatosan épülő dokumentumdefiníció
     */
    private void walkElementsForBlocks(Element container, String parentPath, XsdIndex index, DocumentDefinition definition) {
        for (Element child : childElements(container)) {
            String local = child.getLocalName();
            if ("sequence".equals(local) || "all".equals(local) || "choice".equals(local)) {
                for (Element nested : directElementChildren(child)) {
                    processStructuralElement(nested, parentPath, index, definition);
                }
            }
        }
    }

    /**
     * Egy XSD-elemet a NAV-os szerkezeti névkonvenciók és a feloldott complex type alapján dolgoz fel.
     *
     * <p>{@code FieldGroup_} esetén új blokkot épít, {@code Block_} és más beágyazott complex type esetén rekurzívan folytatja a bejárást, egyszerű levélelem esetén pedig az alapértelmezett blokkhoz ad mezőt. A Form/Block/FieldGroup jellegű struktúrák címkéjét a teljes XML-útvonalhoz regisztrálja.</p>
     *
     * @param element a feldolgozandó XSD {@code element}
     * @param parentPath a szülő kanonikus XML-útvonala
     * @param index az XSD-hivatkozások feloldásához használt index
     * @param definition a módosított dokumentumdefiníció
     */
    private void processStructuralElement(Element element, String parentPath, XsdIndex index, DocumentDefinition definition) {
        String name = getElementName(element);
        if (name == null) {
            return;
        }

        Element nestedComplexType = resolveComplexTypeForElement(element, index);
        String currentPath = parentPath + "/" + name;

        LOGGER.info("XSD processStructuralElement: name={}, currentPath={}, hasNestedComplexType={}",
                name, currentPath, nestedComplexType != null);

        if (nestedComplexType != null || name.startsWith("Form_") || name.startsWith("Block_") || name.startsWith("FieldGroup_")) {
            registerStructuralLabel(definition, currentPath, resolveStructuralLabel(element, index));
        }

        if (name.startsWith("FieldGroup_")) {
            BlockDefinition block = new BlockDefinition();
            block.setId(extractBlockId(name, definition.getBlocks().size() + 1));
            block.setName(name);
            block.setTitle(firstNonBlank(resolveStructuralLabel(element, index), toLabel(name)));

            LOGGER.info("XSD creating block from fieldgroup: xmlName={}, extractedBlockId={}", name, block.getId());

            if (nestedComplexType != null) {
                appendFieldsFromComplexType(block, nestedComplexType, currentPath, index);
            }
            if (!block.getFields().isEmpty()) {
                definition.getBlocks().add(block);
                LOGGER.info("XSD block added: id={}, name={}, fieldCount={}",
                        block.getId(), block.getName(), block.getFields().size());
            } else {
                LOGGER.warn("XSD fieldgroup produced empty block: id={}, name={}", block.getId(), block.getName());
            }
            return;
        }

        if (name.startsWith("Block_")) {
            LOGGER.info("XSD entering Block_ container recursively: name={}, path={}", name, currentPath);
            if (nestedComplexType != null) {
                walkElementsForBlocks(nestedComplexType, currentPath, index, definition);
            }
            return;
        }

        if (nestedComplexType != null) {
            LOGGER.info("XSD entering generic structural container recursively: name={}, path={}", name, currentPath);
            walkElementsForBlocks(nestedComplexType, currentPath, index, definition);
            return;
        }

        BlockDefinition block = ensureDefaultBlock(definition);
        block.getFields().add(createField(element, currentPath, index));
        LOGGER.info("XSD added field to default block: blockId={}, fieldCount={}", block.getId(), block.getFields().size());
    }

    /**
     * Visszaadja az aktuális alapértelmezett mezőblokkot, vagy szükség esetén létrehozza azt.
     *
     * <p>Ha a dokumentum utolsó blokkja {@code default-fields} azonosítójú, azt használja tovább; ellenkező esetben új „Alap mezők” blokkot fűz a definícióhoz.</p>
     *
     * @param definition a dokumentumdefiníció, amelyhez a blokk tartozik
     * @return a már létező vagy újonnan létrehozott alapértelmezett blokk
     */
    private BlockDefinition ensureDefaultBlock(DocumentDefinition definition) {
        if (!definition.getBlocks().isEmpty()
                && "default-fields".equals(definition.getBlocks().get(definition.getBlocks().size() - 1).getId())) {
            return definition.getBlocks().get(definition.getBlocks().size() - 1);
        }
        BlockDefinition block = new BlockDefinition();
        block.setId("default-fields");
        block.setName("default-fields");
        block.setTitle("Alap mezők");
        definition.getBlocks().add(block);
        LOGGER.info("XSD created default block");
        return block;
    }

    /**
     * A megadott complex type közvetlen kompozitoraiból mezőket gyűjt egy blokkba.
     *
     * <p>A {@code sequence}, {@code all} és {@code choice} elemek közvetlen XSD-elemeit dolgozza fel; a beágyazott complex type-ok további bejárását az egyes elemek feldolgozója végzi.</p>
     *
     * @param block a célblokk
     * @param complexType a feldolgozandó complex type
     * @param parentPath a complex type-hoz tartozó XML-útvonal
     * @param index az XSD-feloldási index
     */
    private void appendFieldsFromComplexType(BlockDefinition block, Element complexType, String parentPath, XsdIndex index) {
        for (Element child : childElements(complexType)) {
            String local = child.getLocalName();
            if ("sequence".equals(local) || "all".equals(local) || "choice".equals(local)) {
                for (Element element : directElementChildren(child)) {
                    appendFieldsFromElement(block, element, parentPath, index);
                }
            }
        }
    }

    /**
     * Egy XSD-elemből mezőt készít, vagy összetett szerkezet esetén rekurzívan tovább bontja azt.
     *
     * <p>A {@code Field_} nevű elemek mindig mezőként kerülnek a blokkba. Ugyanez történik akkor is, ha nincs complex type vagy annak nincs további közvetlen eleme. Más összetett elemnél a beágyazott mezők kerülnek feldolgozásra.</p>
     *
     * @param block a célblokk
     * @param element az aktuális XSD-elem
     * @param parentPath a szülő XML-útvonala
     * @param index az XSD-feloldási index
     */
    private void appendFieldsFromElement(BlockDefinition block, Element element, String parentPath, XsdIndex index) {
        String name = getElementName(element);
        if (name == null) {
            return;
        }

        String currentPath = parentPath + "/" + name;
        Element nestedComplexType = resolveComplexTypeForElement(element, index);

        if (name.startsWith("Field_") || nestedComplexType == null || !hasNestedElements(nestedComplexType)) {
            block.getFields().add(createField(element, currentPath, index));
            return;
        }

        appendFieldsFromComplexType(block, nestedComplexType, currentPath, index);
    }

    /**
     * Létrehozza egy XSD-elemből a hozzá tartozó {@link FieldDefinition} objektumot.
     *
     * <p>Beállítja a technikai azonosítót, XML-nevet és -útvonalat, feloldja az XSD-címkét, a {@code minOccurs}/{@code maxOccurs} adatokat, az adattípust és az esetleges enumértékeket. A kötelezőség a {@code minOccurs=0} eset kivételével igaz.</p>
     *
     * @param element a mezőt leíró XSD-elem
     * @param xmlPath a mező teljes XML-útvonala
     * @param index a hivatkozott típusok és elemek feloldási indexe
     * @return a feltöltött meződefiníció
     */
    private FieldDefinition createField(Element element, String xmlPath, XsdIndex index) {
        FieldDefinition field = new FieldDefinition();
        String xmlName = getElementName(element);
        field.setId(extractFieldId(xmlName, xmlPath));
        field.setXmlName(xmlName);
        field.setXmlPath(xmlPath);
        String resolvedXsdLabel = resolveFieldLabel(element, index, xmlName);
        field.setXsdLabel(resolvedXsdLabel);
        field.setLabel(resolvedXsdLabel);
        field.setRequired(!"0".equals(blankToNull(element.getAttribute("minOccurs"))));
        String minOccurs = blankToNull(element.getAttribute("minOccurs"));
        field.setMinOccurs(minOccurs == null ? 1 : parseInteger(minOccurs, 1));
        field.setMaxOccurs(blankToNull(element.getAttribute("maxOccurs")));
        String dataType = resolveDataType(element, index);
        field.setDataType(dataType);
        field.setEnumValues(resolveEnumValues(element, index));
        LOGGER.debug("XSD field created: id={}, xmlName={}, xmlPath={}, label={}, dataType={}",
                field.getId(), field.getXmlName(), field.getXmlPath(), field.getLabel(), field.getDataType());
        return field;
    }

    /**
     * Feloldja egy mező megjelenítendő XSD-címkéjét prioritási sorrend szerint.
     *
     * <p>A sorrend: közvetlen elem-annotáció, {@code ref} által hivatkozott globális elem annotációja, névvel hivatkozott simple type, névvel hivatkozott complex type, inline simple type, inline complex type. Ha egyik sem ad használható címkét, a technikai XML-névből képzett címke a fallback.</p>
     *
     * @param element a mezőt leíró XSD-elem
     * @param index az XSD-definíciók indexe
     * @param xmlName a mező technikai XML-neve
     * @return a megtisztított, megjelenítésre használható címke
     */
    private String resolveFieldLabel(Element element, XsdIndex index, String xmlName) {
        String direct = sanitizeResolvedLabel(cleanLabel(extractAnnotation(element)), xmlName);
        if (direct != null && !direct.isBlank()) {
            return direct;
        }

        String ref = blankToNull(element.getAttribute("ref"));
        if (ref != null) {
            Element referencedElement = index.globalElements.get(localNameFromQName(ref));
            String referencedLabel = sanitizeResolvedLabel(cleanLabel(extractAnnotation(referencedElement)), xmlName);
            if (referencedLabel != null && !referencedLabel.isBlank()) {
                return referencedLabel;
            }
        }

        String typeAttr = blankToNull(element.getAttribute("type"));
        if (typeAttr != null) {
            String typeName = localNameFromQName(typeAttr);

            Element simpleType = index.simpleTypes.get(typeName);
            String simpleTypeLabel = sanitizeResolvedLabel(cleanLabel(extractAnnotation(simpleType)), xmlName);
            if (simpleTypeLabel != null && !simpleTypeLabel.isBlank()) {
                return simpleTypeLabel;
            }

            Element complexType = index.complexTypes.get(typeName);
            String complexTypeLabel = sanitizeResolvedLabel(cleanLabel(extractAnnotation(complexType)), xmlName);
            if (complexTypeLabel != null && !complexTypeLabel.isBlank()) {
                return complexTypeLabel;
            }
        }

        Element inlineSimpleType = firstChild(element, "simpleType");
        String inlineSimpleTypeLabel = sanitizeResolvedLabel(cleanLabel(extractAnnotation(inlineSimpleType)), xmlName);
        if (inlineSimpleTypeLabel != null && !inlineSimpleTypeLabel.isBlank()) {
            return inlineSimpleTypeLabel;
        }

        Element inlineComplexType = firstChild(element, "complexType");
        String inlineComplexTypeLabel = sanitizeResolvedLabel(cleanLabel(extractAnnotation(inlineComplexType)), xmlName);
        if (inlineComplexTypeLabel != null && !inlineComplexTypeLabel.isBlank()) {
            return inlineComplexTypeLabel;
        }

        return toLabel(xmlName);
    }

    /**
     * Eltávolítja a feloldott címke végéről a redundánsan feltüntetett technikai mezőnevet.
     *
     * <p>Kezeli a „címke (Field_...)” és „címke: (Field_...)” formátumot, majd általános technikai zárójeles utótagot is levág. Ezzel elkerülhető, hogy a felületen ugyanaz a technikai név a címke mellett feleslegesen megjelenjen.</p>
     *
     * @param label a nyers vagy már részben tisztított címke
     * @param xmlName az aktuális XML-elem technikai neve
     * @return a normalizált címke; üres vagy {@code null} bemenetnél ennek megfelelő érték
     */
    private String sanitizeResolvedLabel(String label, String xmlName) {
        String cleaned = cleanLabel(label);
        if (cleaned == null || cleaned.isBlank()) {
            return cleaned;
        }
        if (xmlName != null && !xmlName.isBlank()) {
            String trimmedXmlName = xmlName.trim();
            String suffix = " (" + trimmedXmlName + ")";
            if (cleaned.endsWith(suffix)) {
                cleaned = cleaned.substring(0, cleaned.length() - suffix.length()).trim();
            }
            String suffixWithColon = ": (" + trimmedXmlName + ")";
            if (cleaned.endsWith(suffixWithColon)) {
                cleaned = cleaned.substring(0, cleaned.length() - suffixWithColon.length()).trim();
            }
        }
        cleaned = cleaned.replaceFirst("\\s*:?\\s*\\(([A-Za-z0-9_.-]+)\\)\\s*$", "").trim();
        return cleaned;
    }

    /**
     * A szerkezeti elem címkéjét a teljes XML-útvonalhoz rendeli a dokumentumdefinícióban.
     *
     * <p>Hiányzó definíció, útvonal vagy címke esetén nem módosítja a modellt. A path-alapú tárolás teszi lehetővé az azonos rövid nevű, de eltérő helyen szereplő struktúrák megkülönböztetését.</p>
     *
     * @param definition a módosítandó dokumentumdefiníció
     * @param xmlPath a szerkezeti elem teljes XML-útvonala
     * @param label a regisztrálandó címke
     */
    private void registerStructuralLabel(DocumentDefinition definition, String xmlPath, String label) {
        if (definition == null || xmlPath == null || xmlPath.isBlank() || label == null || label.isBlank()) {
            return;
        }
        definition.getStructuralLabelsByPath().put(xmlPath, label.trim());
    }

    /**
     * Feloldja egy Form/Block/FieldGroup jellegű szerkezeti elem XSD-címkéjét.
     *
     * <p>Elsődlegesen az elem saját dokumentációját használja. Ennek hiányában a {@code ref} által hivatkozott globális elem, majd a {@code type} által hivatkozott complex type annotációja következik. Ha egyik forrás sem ad címkét, {@code null} az eredmény.</p>
     *
     * @param element a szerkezeti XSD-elem
     * @param index az XSD-definíciók indexe
     * @return a feloldott címke vagy {@code null}
     */
    private String resolveStructuralLabel(Element element, XsdIndex index) {
        String direct = cleanLabel(extractAnnotation(element));
        if (direct != null && !direct.isBlank()) {
            return direct;
        }
        if (element == null || index == null) {
            return null;
        }
        String ref = blankToNull(element.getAttribute("ref"));
        if (ref != null) {
            String referenced = cleanLabel(extractAnnotation(index.globalElements.get(localNameFromQName(ref))));
            if (referenced != null && !referenced.isBlank()) {
                return referenced;
            }
        }
        String typeAttr = blankToNull(element.getAttribute("type"));
        if (typeAttr != null) {
            String typeName = localNameFromQName(typeAttr);
            String complexTypeLabel = cleanLabel(extractAnnotation(index.complexTypes.get(typeName)));
            if (complexTypeLabel != null && !complexTypeLabel.isBlank()) {
                return complexTypeLabel;
            }
        }
        return null;
    }

    /**
     * Kiolvassa egy XSD-elem első {@code annotation/documentation} szövegét.
     *
     * @param element az XSD-elem, amelyből a dokumentációt ki kell olvasni
     * @return a levágott dokumentációs szöveg, vagy {@code null}, ha nincs használható dokumentáció
     */
    private String extractAnnotation(Element element) {
        if (element == null) {
            return null;
        }
        Element annotation = firstChild(element, "annotation");
        if (annotation == null) {
            return null;
        }
        Element documentation = firstChild(annotation, "documentation");
        if (documentation == null) {
            return null;
        }
        String text = documentation.getTextContent();
        return text == null || text.isBlank() ? null : text.trim();
    }

    /**
     * Normalizálja az annotációból származó címkeszöveget.
     *
     * <p>Levágja a szélső whitespace karaktereket, és ha a teljes érték egyetlen zárójelpárban áll, a külső zárójeleket is eltávolítja.</p>
     *
     * @param value a tisztítandó szöveg
     * @return a normalizált címke vagy {@code null}
     */
    private String cleanLabel(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("(") && trimmed.endsWith(")") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    /**
     * Meghatározza a mező belső azonosítóját.
     *
     * <p>{@code Field_} prefix esetén annak levágott része lesz az azonosító. Más elemnévnél a teljes XML-útvonalból képzett biztonságos azonosító szolgál fallbackként.</p>
     *
     * @param xmlName az XML-elem neve
     * @param xmlPath a mező teljes XML-útvonala
     * @return a mező belső azonosítója
     */
    private String extractFieldId(String xmlName, String xmlPath) {
        if (xmlName != null && xmlName.startsWith("Field_")) {
            return xmlName.substring("Field_".length());
        }
        return safeId(xmlPath);
    }

    /**
     * Meghatározza egy blokk belső azonosítóját a technikai XML-névből.
     *
     * <p>{@code FieldGroup_} és {@code Block_} prefix esetén a prefix nélküli rész az elsődleges. Egyéb névnél normalizált azonosítót képez, név hiányában pedig a blokk sorszámából állít elő fallback értéket.</p>
     *
     * @param xmlName a szerkezeti elem technikai neve
     * @param blockCounter a fallback azonosítóhoz használható blokksorszám
     * @return a blokk belső azonosítója
     */
    private String extractBlockId(String xmlName, int blockCounter) {
        if (xmlName != null && xmlName.startsWith("FieldGroup_")) {
            return xmlName.substring("FieldGroup_".length());
        }
        if (xmlName != null && xmlName.startsWith("Block_")) {
            return xmlName.substring("Block_".length());
        }
        if (xmlName != null) {
            return safeId(xmlName);
        }
        return "block-" + blockCounter;
    }

    /**
     * Feloldja a mező egyszerű adattípusát.
     *
     * <p>Névvel hivatkozott simple type esetén annak restriction base típusát használja; ha nincs ilyen restriction, a {@code type} lokális neve az eredmény. Inline simple type esetén szintén a restriction base típust keresi. Végső fallback a {@code string}.</p>
     *
     * @param element a mezőt leíró XSD-elem
     * @param index a simple type definíciók indexe
     * @return a feloldott adattípus lokális neve
     */
    private String resolveDataType(Element element, XsdIndex index) {
        String typeAttr = blankToNull(element.getAttribute("type"));
        if (typeAttr != null) {
            Element simpleType = index.simpleTypes.get(localNameFromQName(typeAttr));
            if (simpleType != null) {
                Element restriction = firstChild(simpleType, "restriction");
                if (restriction != null) {
                    return localNameFromQName(restriction.getAttribute("base"));
                }
            }
            return localNameFromQName(typeAttr);
        }

        Element inlineSimpleType = firstChild(element, "simpleType");
        if (inlineSimpleType != null) {
            Element restriction = firstChild(inlineSimpleType, "restriction");
            if (restriction != null) {
                return localNameFromQName(restriction.getAttribute("base"));
            }
        }

        return "string";
    }

    /**
     * Összegyűjti a mezőhöz tartozó XSD enumeration értékeket.
     *
     * <p>Először a {@code type} attribútummal hivatkozott simple type-ot, ennek hiányában az inline simple type-ot vizsgálja. A restriction közvetlen {@code enumeration} gyermekeinek nem üres {@code value} attribútumait deklarációs sorrendben adja vissza.</p>
     *
     * @param element a mezőt leíró XSD-elem
     * @param index a simple type definíciók indexe
     * @return az enumértékek listája; enum hiányában üres lista
     */
    private List<String> resolveEnumValues(Element element, XsdIndex index) {
        List<String> values = new ArrayList<>();
        Element simpleType = null;
        String typeAttr = blankToNull(element.getAttribute("type"));
        if (typeAttr != null) {
            simpleType = index.simpleTypes.get(localNameFromQName(typeAttr));
        }
        if (simpleType == null) {
            simpleType = firstChild(element, "simpleType");
        }
        if (simpleType == null) {
            return values;
        }
        Element restriction = firstChild(simpleType, "restriction");
        if (restriction == null) {
            return values;
        }
        for (Element child : childElements(restriction)) {
            if ("enumeration".equals(child.getLocalName())) {
                String value = blankToNull(child.getAttribute("value"));
                if (value != null) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    /**
     * Feloldja az XSD-elemhez tartozó complex type definíciót.
     *
     * <p>Az inline {@code complexType} elsőbbséget élvez. Ha ilyen nincs, a {@code type} attribútum lokális neve alapján keres a globális complex type indexben.</p>
     *
     * @param element a vizsgált XSD-elem
     * @param index a complex type definíciók indexe
     * @return a complex type DOM-eleme vagy {@code null}
     */
    private Element resolveComplexTypeForElement(Element element, XsdIndex index) {
        Element inlineComplexType = firstChild(element, "complexType");
        if (inlineComplexType != null) {
            return inlineComplexType;
        }
        String typeAttr = blankToNull(element.getAttribute("type"));
        if (typeAttr == null) {
            return null;
        }
        return index.complexTypes.get(localNameFromQName(typeAttr));
    }

    /**
     * Megállapítja, hogy egy complex type tartalmaz-e közvetlenül feldolgozható beágyazott XSD-elemet.
     *
     * <p>A {@code sequence}, {@code all} és {@code choice} kompozitorokat vizsgálja.</p>
     *
     * @param complexType a vizsgált complex type
     * @return {@code true}, ha legalább egy kompozitorban van közvetlen {@code element} gyermek
     */
    private boolean hasNestedElements(Element complexType) {
        for (Element child : childElements(complexType)) {
            if ("sequence".equals(child.getLocalName()) || "all".equals(child.getLocalName()) || "choice".equals(child.getLocalName())) {
                if (!directElementChildren(child).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Kiválasztja a dokumentum feldolgozásának gyökérelemét az XSD-indexből.
     *
     * <p>Ha a {@link SchemaBundle} megadott gyökérelemneve megtalálható a globális elemek között, azt választja. Ellenkező esetben az index első globális eleme a fallback.</p>
     *
     * @param index a globális XSD-elemek indexe
     * @param bundle a feloldási metaadatokat tartalmazó séma-csomag
     * @return a kiválasztott gyökérelem vagy {@code null}, ha nincs globális elem
     */
    private Element findRootElement(XsdIndex index, SchemaBundle bundle) {
        if (bundle.getRootElementName() != null && index.globalElements.containsKey(bundle.getRootElementName())) {
            return index.globalElements.get(bundle.getRootElementName());
        }
        return index.globalElements.values().stream().findFirst().orElse(null);
    }

    /**
     * Közös XSD-indexet épít a séma-csomaghoz tartozó fájlokból.
     *
     * <p>Az elsődleges XSD és a {@code xsdFiles} lista egy rendezett halmazba kerül, így ugyanaz a fájl csak egyszer dolgozódik fel. Csak ténylegesen létező reguláris fájlokat parse-ol.</p>
     *
     * @param bundle a feldolgozandó séma-csomag
     * @return a globális elemeket, complex type-okat és simple type-okat tartalmazó index
     */
    private XsdIndex buildIndex(SchemaBundle bundle) {
        XsdIndex index = new XsdIndex();
        Set<Path> files = new LinkedHashSet<>();
        if (bundle.getPrimaryXsd() != null) files.add(bundle.getPrimaryXsd());
        if (bundle.getXsdFiles() != null) files.addAll(bundle.getXsdFiles());
        for (Path xsd : files) {
            if (!ExceptionSafeOperations.isRegularFile(xsd)) {
                continue;
            }
            parseSchemaFile(xsd, index);
        }
        return index;
    }

    /**
     * Biztonságos DOM-feldolgozással beolvassa egy XSD-fájl globális definícióit az indexbe.
     *
     * <p>A parser tiltja a DOCTYPE deklarációt, a külső entitásokat, a külső DTD- és sémahozzáférést, valamint az XInclude használatát. A séma közvetlen, névvel rendelkező {@code element}, {@code complexType} és {@code simpleType} gyermekeit rögzíti. Az elsőként indexelt azonos nevű definíció marad érvényben.</p>
     *
     * @param xsdFile a feldolgozandó XSD-fájl
     * @param index a bővítendő XSD-index
     * @throws IllegalStateException ha a fájl XML-ként nem dolgozható fel
     */
    private void parseSchemaFile(Path xsdFile, XsdIndex index) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(xsdFile.toFile());
            Element schema = document.getDocumentElement();
            LOGGER.info("XSD parse schema file: {}", xsdFile);
            for (Element child : childElements(schema)) {
                String local = child.getLocalName();
                String name = blankToNull(child.getAttribute("name"));
                if (name == null) {
                    continue;
                }
                switch (local) {
                    case "element" -> index.globalElements.putIfAbsent(name, child);
                    case "complexType" -> index.complexTypes.putIfAbsent(name, child);
                    case "simpleType" -> index.simpleTypes.putIfAbsent(name, child);
                    default -> {
                    }
                }
            }
            LOGGER.info("XSD index updated from file {}: globalElements={}, complexTypes={}, simpleTypes={}",
                    xsdFile, index.globalElements.size(), index.complexTypes.size(), index.simpleTypes.size());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse XSD: " + xsdFile, e);
        }
    }

    /**
     * Visszaadja egy DOM-elem összes közvetlen gyermekét, amelyek maguk is elemek.
     *
     * @param parent a vizsgált szülő DOM-elem
     * @return a közvetlen gyermekelemek eredeti sorrendben
     */
    private List<Element> childElements(Element parent) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Kiszűri egy XSD-kompozitor közvetlen {@code element} gyermekeit.
     *
     * @param compositor a {@code sequence}, {@code all} vagy {@code choice} elem
     * @return a közvetlen XSD-elemek deklarációs sorrendben
     */
    private List<Element> directElementChildren(Element compositor) {
        List<Element> result = new ArrayList<>();
        for (Element child : childElements(compositor)) {
            if ("element".equals(child.getLocalName())) {
                result.add(child);
            }
        }
        return result;
    }

    /**
     * Megkeresi az első, adott lokális nevű közvetlen gyermekelemet.
     *
     * @param parent a keresés szülőeleme; {@code null} esetén nincs találat
     * @param localName a keresett namespace-független lokális név
     * @return az első megfelelő gyermek vagy {@code null}
     */
    private Element firstChild(Element parent, String localName) {
        if (parent == null) {
            return null;
        }
        for (Element child : childElements(parent)) {
            if (localName.equals(child.getLocalName())) {
                return child;
            }
        }
        return null;
    }

    /**
     * Meghatározza egy XSD {@code element} tényleges lokális nevét.
     *
     * <p>{@code ref} attribútum esetén a hivatkozott QName lokális részét használja; egyébként a saját {@code name} attribútumot adja vissza.</p>
     *
     * @param element a vizsgált XSD-elem
     * @return az elem lokális neve vagy {@code null}
     */
    private String getElementName(Element element) {
        String ref = blankToNull(element.getAttribute("ref"));
        if (ref != null) return localNameFromQName(ref);
        return blankToNull(element.getAttribute("name"));
    }

    /**
     * Eltávolítja egy QName opcionális namespace-prefixét.
     *
     * @param qName a prefixelt vagy prefix nélküli QName
     * @return a kettőspont utáni lokális név, illetve prefix nélkül az eredeti érték
     */
    private String localNameFromQName(String qName) {
        if (qName == null) return null;
        int idx = qName.indexOf(':');
        return idx >= 0 ? qName.substring(idx + 1) : qName;
    }

    /**
     * Eltávolítja a fájlnév utolsó kiterjesztését.
     *
     * @param fileName a feldolgozandó fájlnév
     * @return a kiterjesztés nélküli név; pont hiányában az eredeti érték
     */
    private String stripExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }

    /**
     * Visszaadja a paraméterek közül az első nem null és nem üres szöveget.
     *
     * @param values a prioritási sorrendben vizsgálandó értékek
     * @return az első használható érték vagy {@code null}
     */
    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    /**
     * Egyszerű, technikai névből képzett fallback címkét állít elő.
     *
     * <p>A {@code FieldGroup_}, {@code Field_} és {@code Block_} prefixeket levágja; más esetben az aláhúzásjeleket szóközre cseréli. Hiányzó névnél a „Mező” értéket adja.</p>
     *
     * @param name a technikai XSD/XML-név
     * @return az ember számára olvasható fallback címke
     */
    private String toLabel(String name) {
        if (name == null || name.isBlank()) return "Mező";
        if (name.startsWith("FieldGroup_")) {
            return name.substring("FieldGroup_".length());
        }
        if (name.startsWith("Field_")) {
            return name.substring("Field_".length());
        }
        if (name.startsWith("Block_")) {
            return name.substring("Block_".length());
        }
        return name.replace('_', ' ');
    }

    /**
     * Technikai szövegből kisbetűs, kötőjeles belső azonosítót képez.
     *
     * <p>A nem alfanumerikus karaktercsoportokat kötőjelre cseréli és levágja a szélső kötőjeleket. {@code null} esetén {@code field} az eredmény.</p>
     *
     * @param value a normalizálandó szöveg
     * @return a biztonságosan használható belső azonosító
     */
    private String safeId(String value) {
        return value == null ? "field" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    /**
     * Egész számot olvas be fallback értékkel.
     *
     * @param value a parse-olandó szöveg
     * @param defaultValue hibás számformátum esetén visszaadandó érték
     * @return a beolvasott egész vagy a fallback
     */
    private Integer parseInteger(String value, int defaultValue) {
        try { return Integer.parseInt(value); } catch (Exception ex) { return defaultValue; }
    }

    /**
     * Az üres vagy csak whitespace karaktereket tartalmazó szöveget {@code null} értékre normalizálja.
     *
     * @param value a vizsgált szöveg
     * @return {@code null}, ha az érték hiányzik vagy üres; különben az eredeti érték
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * A több XSD-fájlból összegyűjtött név szerinti feloldási index.
     *
     * <p>Az index a parser egyetlen futásán belül tartja a globális elemeket, complex type és simple type
     * definíciókat. A {@link LinkedHashMap} használata megőrzi a beolvasás sorrendjét, ami a gyökérelem
     * fallback kiválasztásánál is számít.</p>
     */
    private static class XsdIndex {
        private final Map<String, Element> globalElements = new LinkedHashMap<>();
        private final Map<String, Element> complexTypes = new LinkedHashMap<>();
        private final Map<String, Element> simpleTypes = new LinkedHashMap<>();
    }
}
