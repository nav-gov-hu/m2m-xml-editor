package hu.gov.nav.xsdparsertool.web.xmlindex.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import hu.gov.nav.xsdparsertool.web.xmlindex.config.XmlIndexConfigProperties;
import hu.gov.nav.xsdparsertool.web.xmlindex.dto.XmlIndexDtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code XmlIndexConfigService} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class XmlIndexConfigService {
    private static final Logger log = LoggerFactory.getLogger(XmlIndexConfigService.class);
    private static final Pattern BLOCK_PATTERN = Pattern.compile("Block_\\d+");
    private static final Pattern CHAIN_PATTERN = Pattern.compile("Chain_\\d+");
    private static final Pattern FIELD_GROUP_PATTERN = Pattern.compile("FieldGroup_\\d+");
    private static final Pattern FIELD_PATTERN = Pattern.compile("Field_[A-Za-z0-9_]+");
    private static final Pattern DOCUMENT_ELEMENT_PATTERN = Pattern.compile("<(?:[A-Za-z_][A-Za-z0-9_.-]*:)?element\\b[^>]*\\bname\\s*=\\s*[\"\']Doc_([^\"\']+)[\"\']", Pattern.CASE_INSENSITIVE);

    private final XmlIndexConfigProperties properties;
    private final PathConfigurationProperties pathProperties;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;

    /**
     * Létrehozza a {@code XmlIndexConfigService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param properties a művelethez szükséges konfigurációs adatok
     * @param pathProperties a feldolgozásban részt vevő fájl vagy elérési út
     * @param auditLogService a művelet bemeneti {@code auditLogService} értéke
     * @param currentUserService a művelet felhasználói kontextusa vagy felhasználóneve
     */
    public XmlIndexConfigService(XmlIndexConfigProperties properties,
                                 PathConfigurationProperties pathProperties,
                                 AuditLogService auditLogService,
                                 CurrentUserService currentUserService) {
        this.properties = properties;
        this.pathProperties = pathProperties;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
    }

    /**
     * A {@code listForms} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    public FormsResponse listForms() {
        Map<String, Set<String>> versions = new TreeMap<>();
        Map<String, String> labels = new TreeMap<>();
        for (XsdSource source : scanSchemaRepository()) {
            versions.computeIfAbsent(source.formName(), k -> new TreeSet<>());
            if (StringUtils.hasText(source.version())) {
                versions.get(source.formName()).add(source.version());
            }
            labels.putIfAbsent(source.formName(), source.formName());
        }
        Map<String, IndexFormConfigDto> saved = loadAllConfigs();
        saved.keySet().forEach(form -> versions.computeIfAbsent(form, k -> new TreeSet<>()));
        List<FormOptionDto> forms = versions.entrySet().stream()
                .map(e -> new FormOptionDto(e.getKey(), labels.getOrDefault(e.getKey(), e.getKey()), new ArrayList<>(e.getValue()), saved.containsKey(e.getKey())))
                .collect(Collectors.toList());
        return new FormsResponse(forms, configPath().toString());
    }

    /**
     * A {@code structure} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @param sourceVersion a művelet bemeneti {@code sourceVersion} értéke
     * @return a művelet feldolgozási eredménye
     */
    public StructureResponse structure(String formName, String sourceVersion) {
        String normalizedForm = requireForm(formName);
        XsdSource source = chooseSourceXsd(normalizedForm, sourceVersion).orElse(null);
        ParsedStructure parsed = source == null
                ? new ParsedStructure(List.of(), List.of(), List.of(), List.of())
                : parseXsdStructure(source.path());
        IndexFormConfigDto saved = loadAllConfigs().get(normalizedForm);
        List<IndexFieldDto> fields = mergeSavedFlags(parsed.fields(), saved);
        List<FormPartDto> parts = applyConfiguredFlags(parsed.formParts(), fields);
        return new StructureResponse(normalizedForm,
                normalizedForm,
                source == null ? null : source.version(),
                source == null ? null : source.path().toString(),
                parts,
                parsed.tree(),
                fields,
                parsed.chains(),
                saved);
    }

    /**
     * A {@code save} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet feldolgozási eredménye
     */
    public SaveResponse save(IndexFormConfigDto request) {
        if (request == null || !StringUtils.hasText(request.getFormName())) {
            throw new IllegalArgumentException("Űrlap megadása kötelező.");
        }
        String formName = normalizeFormName(request.getFormName());
        request.setFormName(formName);
        request.setFields(sanitizeFields(collectRequestFields(request)));
        request.setChains(new ArrayList<>());
        Map<String, IndexFormConfigDto> all = loadAllConfigs();
        all.put(formName, request);
        writeAllConfigs(all);
        int fieldCount = request.getFields().size();
        auditLogService.log("INDEX_CONFIG_CHANGED", currentUserService.getCurrentUsername(), "SUCCESS",
                "XML index konfiguráció módosítva: " + formName + ", field=" + fieldCount);
        return new SaveResponse(formName, configPath().toString(), 0, fieldCount);
    }

    /**
     * A {@code scanSchemaRepository} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet eredményeként előállított elemek listája
     */
    private List<XsdSource> scanSchemaRepository() {
        Path root = schemaRoot();
        if (root == null || !ExceptionSafeOperations.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xsd"))
                    .map(this::toXsdSource)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(XsdSource::formName).thenComparing(XsdSource::version, Comparator.nullsLast(String::compareTo)))
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("XSD repo beolvasása sikertelen index konfigurációhoz: {}", root, ex);
            return List.of();
        }
    }

    /**
     * A {@code chooseSourceXsd} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @param version a művelet bemeneti {@code version} értéke
     * @return a feloldott érték, vagy üres {@link java.util.Optional}, ha nincs alkalmazható találat
     */
    private Optional<XsdSource> chooseSourceXsd(String formName, String version) {
        return scanSchemaRepository().stream()
                .filter(s -> formName.equalsIgnoreCase(s.formName()))
                .filter(s -> !StringUtils.hasText(version) || version.equals(s.version()))
                // Az összetett repo több azonos űrlapnevű XSD-t is tartalmazhat.
                // Elsőként azt válasszuk, amely ténylegesen deklarálja a Doc_<form> gyökérelemet.
                .sorted(Comparator.<XsdSource>comparingInt(s -> documentSchemaScore(s.path(), formName)).reversed()
                        .thenComparing(XsdSource::version, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(s -> s.path().toString(), Comparator.reverseOrder()))
                .findFirst();
    }

    /**
     * A {@code documentSchemaScore} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @return a művelet feldolgozási eredménye
     */
    private int documentSchemaScore(Path path, String formName) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            int score = 0;
            if (content.contains("name=\"Doc_" + formName + "\"")) score += 100;
            if (content.contains("name=\"Doc_" + formName + "_Type\"")) score += 50;
            if (content.contains("name=\"Form_" + formName)) score += 20;
            return score;
        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * A {@code toXsdSource} művelet előállítja a hívó réteg által használt reprezentációt.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    private XsdSource toXsdSource(Path path) {
        String file = path.getFileName().toString();
        if (file.equalsIgnoreCase("common.xsd") || file.equalsIgnoreCase("general.xsd") || file.equalsIgnoreCase("form_xsd_metaschema.xsd")) {
            return null;
        }
        String base = file.substring(0, file.length() - 4);
        String form = null;
        String version = null;

        Matcher matcher = Pattern.compile("^((?:NAV_)?(?:.+?))_([0-9]+(?:[._][0-9]+)*(?:[A-Za-z0-9.-]*))(?:__.*)?$").matcher(base);
        if (matcher.matches()) {
            // A NAV_ előtag a technikai dokumentumtípus része (például NAV_F07),
            // ezért XSD-, könyvtár- és konfigurációs kulcsként is meg kell őrizni.
            form = matcher.group(1);
            version = matcher.group(2).replace('_', '.');
        }
        if (!StringUtils.hasText(form)) {
            Path parent = path.getParent();
            Path grandParent = parent == null ? null : parent.getParent();
            if (grandParent != null && grandParent.getFileName() != null) {
                String parentName = parent.getFileName().toString();
                String grandParentName = grandParent.getFileName().toString();
                if (parentName.matches("[0-9][A-Za-z0-9.-]*")) {
                    form = grandParentName;
                    version = parentName;
                }
            }
        }
        String declaredDocumentForm = detectDeclaredDocumentForm(path);
        if (StringUtils.hasText(declaredDocumentForm)) {
            form = declaredDocumentForm;
        }
        if (!StringUtils.hasText(form)) {
            form = base;
        }
        return new XsdSource(normalizeFormName(form), version, path);
    }

    /**
     * A {@code detectDeclaredDocumentForm} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    private String detectDeclaredDocumentForm(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            Matcher matcher = DOCUMENT_ELEMENT_PATTERN.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ex) {
            log.debug("XSD dokumentumgyökér-alapú űrlapnév felismerése sikertelen: {}", path, ex);
        }
        return null;
    }

    /**
     * A {@code schemaRoot} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    private Path schemaRoot() {
        if (pathProperties == null || !StringUtils.hasText(pathProperties.getSchemaDir())) {
            return null;
        }
        return Path.of(pathProperties.getSchemaDir());
    }

    /**
     * A {@code parseXsdStructure} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xsdPath a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    private ParsedStructure parseXsdStructure(Path xsdPath) {
        if (!ExceptionSafeOperations.isRegularFile(xsdPath)) {
            return new ParsedStructure(List.of(), List.of(), List.of(), List.of());
        }
        try {
            Document doc = secureDocumentBuilder().parse(xsdPath.toFile());
            SchemaIndex schema = buildSchemaIndex(doc);
            List<IndexTreeNodeDto> tree = new ArrayList<>();
            List<IndexFieldDto> fields = new ArrayList<>();
            Map<String, IndexChainDto> chains = new LinkedHashMap<>();
            List<FormPartDto> formParts = discoverDirectFormParts(schema);

            Element rootElement = chooseDocumentRoot(schema.globalElements());
            try {
                if (rootElement != null) {
                    List<FormPartDto> traversedParts = new ArrayList<>();
                    traverseTypedElement(rootElement, schema, null, tree, fields, chains, traversedParts, new ArrayDeque<>(), null, null, 0);
                    if (!traversedParts.isEmpty()) {
                        formParts = traversedParts;
                    }
                } else {
                    // Fallback for older or non-standard XSDs: keep the previous generic field discovery behavior.
                    traverseFullStructure(doc.getDocumentElement(), null, tree, fields, chains, new ArrayDeque<>(), null, null);
                }
            } catch (RuntimeException traversalError) {
                log.warn("XML index részletes mezőstruktúra feldolgozása sikertelen, az űrlaprészek ettől még visszaadásra kerülnek: {}", xsdPath, traversalError);
            }
            if (formParts.isEmpty()) {
                formParts = discoverFormPartsFallback(doc);
            }
            return new ParsedStructure(tree, fields, new ArrayList<>(chains.values()), formParts);
        } catch (Exception ex) {
            log.warn("XML index struktúra XSD olvasása sikertelen: {}", xsdPath, ex);
            return new ParsedStructure(List.of(), List.of(), List.of(), List.of());
        }
    }



    /**
     * Hibatűrő tartalék űrlaprész-felderítés nagy vagy szokatlan sorrendű XSD-khez.
     * A teljes mezőfa sikerétől függetlenül megkeresi a névvel deklarált Form_* elemeket.
     */
    private List<FormPartDto> discoverFormPartsFallback(Document doc) {
        LinkedHashMap<String, FormPartDto> parts = new LinkedHashMap<>();
        NodeList elements = doc.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "element");
        for (int i = 0; i < elements.getLength(); i++) {
            if (!(elements.item(i) instanceof Element element)) continue;
            String name = element.getAttribute("name");
            if (!StringUtils.hasText(name) || !name.startsWith("Form_")) continue;
            String maxOccurs = firstText(element.getAttribute("maxOccurs"), "1");
            FormPartDto part = new FormPartDto();
            part.setName(name);
            part.setLabel(readAnnotation(element, name));
            part.setXmlPath("/" + name);
            part.setRole(isRepeatingMaxOccurs(maxOccurs) ? "REPEATING" : (parts.isEmpty() ? "MAIN" : "SINGLE"));
            part.setMinOccurs(parseInteger(element.getAttribute("minOccurs")));
            part.setMaxOccurs(maxOccurs);
            parts.putIfAbsent(name, part);
        }
        return new ArrayList<>(parts.values());
    }

    /**
     * A {@code discoverDirectFormParts} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param schema a művelet bemeneti {@code schema} értéke
     * @return a művelet eredményeként előállított elemek listája
     */
    private List<FormPartDto> discoverDirectFormParts(SchemaIndex schema) {
        Element rootElement = chooseDocumentRoot(schema.globalElements());
        if (rootElement == null) return new ArrayList<>();
        Element rootType = schema.complexTypes().get(stripNamespace(rootElement.getAttribute("type")));
        if (rootType == null) rootType = firstDirectChild(rootElement, "complexType");
        if (rootType == null) return new ArrayList<>();

        List<FormPartDto> parts = new ArrayList<>();
        String rootName = rootElement.getAttribute("name");
        for (Element child : childXsdElements(rootType)) {
            String name = child.getAttribute("name");
            if (!StringUtils.hasText(name) || !name.startsWith("Form_")) continue;
            String maxOccurs = firstText(child.getAttribute("maxOccurs"), "1");
            FormPartDto part = new FormPartDto();
            part.setName(name);
            part.setLabel(readAnnotation(child, name));
            part.setXmlPath("/" + rootName + "/" + name);
            part.setRole(isRepeatingMaxOccurs(maxOccurs) ? "REPEATING" : (parts.isEmpty() ? "MAIN" : "SINGLE"));
            part.setMinOccurs(parseInteger(child.getAttribute("minOccurs")));
            part.setMaxOccurs(maxOccurs);
            parts.add(part);
        }
        return parts;
    }

    /**
     * A {@code buildSchemaIndex} művelet előállítja a hívó réteg által használt reprezentációt.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param doc a művelet bemeneti {@code doc} értéke
     * @return a művelet feldolgozási eredménye
     */
    private SchemaIndex buildSchemaIndex(Document doc) {
        Map<String, Element> complexTypes = new LinkedHashMap<>();
        Map<String, Element> globalElements = new LinkedHashMap<>();
        Element root = doc.getDocumentElement();
        for (Element child : directChildElements(root)) {
            String localName = child.getLocalName();
            String name = child.getAttribute("name");
            if (!StringUtils.hasText(name)) continue;
            if ("complexType".equals(localName)) complexTypes.put(name, child);
            if ("element".equals(localName)) globalElements.put(name, child);
        }
        return new SchemaIndex(complexTypes, globalElements);
    }

    /**
     * A {@code chooseDocumentRoot} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param globalElements a művelet bemeneti {@code globalElements} értéke
     * @return a művelet feldolgozási eredménye
     */
    private Element chooseDocumentRoot(Map<String, Element> globalElements) {
        for (Map.Entry<String, Element> entry : globalElements.entrySet()) {
            if (entry.getKey().startsWith("Doc_")) return entry.getValue();
        }
        for (Element element : globalElements.values()) {
            String name = element.getAttribute("name");
            if (StringUtils.hasText(name) && !name.startsWith("Form_")) return element;
        }
        return globalElements.values().stream().findFirst().orElse(null);
    }

    /**
     * A {@code traverseTypedElement} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param element a művelet bemeneti {@code element} értéke
     * @param schema a művelet bemeneti {@code schema} értéke
     * @param currentNode a művelet bemeneti {@code currentNode} értéke
     * @param roots a feldolgozandó elemek kollekciója
     * @param fields a feldolgozandó elemek kollekciója
     * @param chains a művelet bemeneti {@code chains} értéke
     * @param formParts a feldolgozandó elemek kollekciója
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @param currentFormPart a művelet bemeneti {@code currentFormPart} értéke
     * @param currentParentInfo a művelet bemeneti {@code currentParentInfo} értéke
     * @param depth a művelet bemeneti {@code depth} értéke
     */
    private void traverseTypedElement(Element element,
                                      SchemaIndex schema,
                                      IndexTreeNodeDto currentNode,
                                      List<IndexTreeNodeDto> roots,
                                      List<IndexFieldDto> fields,
                                      Map<String, IndexChainDto> chains,
                                      List<FormPartDto> formParts,
                                      Deque<String> path,
                                      FormPartContext currentFormPart,
                                      String currentParentInfo,
                                      int depth) {
        if (depth > 80) return;
        String name = element.getAttribute("name");
        if (!StringUtils.hasText(name)) return;

        path.addLast(name);
        String xmlPath = toPath(path);
        IndexTreeNodeDto node = new IndexTreeNodeDto();
        node.setName(name);
        node.setLabel(readAnnotation(element, name));
        node.setXmlPath(xmlPath);
        node.setType(nodeType(name));
        node.setConfigurable(FIELD_PATTERN.matcher(name).matches());
        if (currentNode == null) roots.add(node); else currentNode.getChildren().add(node);

        FormPartContext formPart = currentFormPart;
        if (name.startsWith("Form_")) {
            String maxOccurs = firstText(element.getAttribute("maxOccurs"), "1");
            String role = isRepeatingMaxOccurs(maxOccurs) ? "REPEATING" : (currentFormPart == null ? "MAIN" : "SINGLE");
            FormPartDto part = new FormPartDto();
            part.setName(name);
            part.setLabel(node.getLabel());
            part.setXmlPath(xmlPath);
            part.setRole(role);
            part.setMinOccurs(parseInteger(element.getAttribute("minOccurs")));
            part.setMaxOccurs(maxOccurs);
            formParts.add(part);
            formPart = new FormPartContext(name, role, xmlPath);
        }

        String nextParentInfo = currentParentInfo;
        if (BLOCK_PATTERN.matcher(name).matches() || CHAIN_PATTERN.matcher(name).matches() || FIELD_GROUP_PATTERN.matcher(name).matches()) {
            nextParentInfo = StringUtils.hasText(currentParentInfo) ? currentParentInfo + " / " + name : name;
        }

        if (FIELD_PATTERN.matcher(name).matches()) {
            IndexFieldDto field = new IndexFieldDto();
            field.setName(name);
            field.setLabel(node.getLabel());
            field.setXmlPath(xmlPath);
            if (formPart != null) {
                field.setFormPartName(formPart.name());
                field.setFormPartRole(formPart.role());
            }
            field.setParentInfo(nextParentInfo);
            field.setMatchMode("contains");
            fields.add(field);
        }
        if (CHAIN_PATTERN.matcher(name).matches()) {
            IndexChainDto chain = new IndexChainDto();
            chain.setName(name);
            chain.setLabel(node.getLabel());
            chain.setXmlPath(xmlPath);
            chains.putIfAbsent(xmlPath, chain);
        }

        Element type = schema.complexTypes().get(stripNamespace(element.getAttribute("type")));
        if (type == null) type = firstDirectChild(element, "complexType");
        if (type != null) {
            for (Element childElement : childXsdElements(type)) {
                traverseTypedElement(childElement, schema, node, roots, fields, chains, formParts, path, formPart, nextParentInfo, depth + 1);
            }
        }
        path.removeLast();
    }

    /**
     * A {@code traverseFullStructure} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param element a művelet bemeneti {@code element} értéke
     * @param currentNode a művelet bemeneti {@code currentNode} értéke
     * @param roots a feldolgozandó elemek kollekciója
     * @param fields a feldolgozandó elemek kollekciója
     * @param chains a művelet bemeneti {@code chains} értéke
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @param currentFormPart a művelet bemeneti {@code currentFormPart} értéke
     * @param currentParentInfo a művelet bemeneti {@code currentParentInfo} értéke
     */
    private void traverseFullStructure(Element element,
                                       IndexTreeNodeDto currentNode,
                                       List<IndexTreeNodeDto> roots,
                                       List<IndexFieldDto> fields,
                                       Map<String, IndexChainDto> chains,
                                       Deque<String> path,
                                       FormPartContext currentFormPart,
                                       String currentParentInfo) {
        String name = element.getAttribute("name");
        IndexTreeNodeDto nextNode = currentNode;
        FormPartContext formPart = currentFormPart;
        String nextParentInfo = currentParentInfo;
        boolean namedXmlNode = isRelevantXmlNode(name);
        if (namedXmlNode) {
            path.addLast(name);
            IndexTreeNodeDto node = new IndexTreeNodeDto();
            node.setName(name);
            node.setLabel(readAnnotation(element, name));
            node.setXmlPath(toPath(path));
            node.setType(nodeType(name));
            node.setConfigurable(FIELD_PATTERN.matcher(name).matches());
            if (currentNode == null) roots.add(node); else currentNode.getChildren().add(node);
            nextNode = node;
            if (name.startsWith("Form_")) {
                String maxOccurs = firstText(element.getAttribute("maxOccurs"), "1");
                String role = isRepeatingMaxOccurs(maxOccurs) ? "REPEATING" : "MAIN";
                formPart = new FormPartContext(name, role, node.getXmlPath());
            }
            if (BLOCK_PATTERN.matcher(name).matches() || CHAIN_PATTERN.matcher(name).matches() || FIELD_GROUP_PATTERN.matcher(name).matches()) {
                nextParentInfo = StringUtils.hasText(currentParentInfo) ? currentParentInfo + " / " + name : name;
            }
            if (FIELD_PATTERN.matcher(name).matches()) {
                IndexFieldDto field = new IndexFieldDto();
                field.setName(name);
                field.setLabel(node.getLabel());
                field.setXmlPath(node.getXmlPath());
                if (formPart != null) {
                    field.setFormPartName(formPart.name());
                    field.setFormPartRole(formPart.role());
                }
                field.setParentInfo(nextParentInfo);
                field.setMatchMode("contains");
                fields.add(field);
            }
            if (CHAIN_PATTERN.matcher(name).matches()) {
                IndexChainDto chain = new IndexChainDto();
                chain.setName(name);
                chain.setLabel(node.getLabel());
                chain.setXmlPath(node.getXmlPath());
                chains.putIfAbsent(node.getXmlPath(), chain);
            }
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element child) {
                traverseFullStructure(child, nextNode, roots, fields, chains, path, formPart, nextParentInfo);
            }
        }
        if (namedXmlNode) path.removeLast();
    }

    /**
     * A {@code childXsdElements} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param typeOrElement a művelet bemeneti {@code typeOrElement} értéke
     * @return a művelet eredményeként előállított elemek listája
     */
    private List<Element> childXsdElements(Element typeOrElement) {
        List<Element> result = new ArrayList<>();
        collectChildXsdElements(typeOrElement, result);
        return result;
    }

    /**
     * A {@code collectChildXsdElements} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param parent a művelet bemeneti {@code parent} értéke
     * @param result a feldolgozandó elemek kollekciója
     */
    private void collectChildXsdElements(Element parent, List<Element> result) {
        for (Element child : directChildElements(parent)) {
            String local = child.getLocalName();
            if ("element".equals(local) && StringUtils.hasText(child.getAttribute("name"))) {
                result.add(child);
            } else if ("sequence".equals(local) || "choice".equals(local) || "all".equals(local) || "complexContent".equals(local) || "extension".equals(local)) {
                collectChildXsdElements(child, result);
            }
        }
    }

    /**
     * A {@code directChildElements} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param parent a művelet bemeneti {@code parent} értéke
     * @return a művelet eredményeként előállított elemek listája
     */
    private List<Element> directChildElements(Element parent) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element) result.add(element);
        }
        return result;
    }

    /**
     * A {@code firstDirectChild} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param parent a művelet bemeneti {@code parent} értéke
     * @param localName a feloldáshoz vagy azonosításhoz használt név
     * @return a művelet feldolgozási eredménye
     */
    private Element firstDirectChild(Element parent, String localName) {
        for (Element child : directChildElements(parent)) {
            if (localName.equals(child.getLocalName())) return child;
        }
        return null;
    }

    /**
     * A {@code isRepeatingMaxOccurs} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param maxOccurs a művelet bemeneti {@code maxOccurs} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isRepeatingMaxOccurs(String maxOccurs) {
        if (!StringUtils.hasText(maxOccurs)) return false;
        if ("unbounded".equals(maxOccurs)) return true;
        try { return Integer.parseInt(maxOccurs) > 1; } catch (Exception ex) { return false; }
    }

    /**
     * A {@code parseInteger} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) return null;
        try { return Integer.parseInt(value); } catch (Exception ex) { return null; }
    }

    /**
     * A {@code stripNamespace} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String stripNamespace(String value) {
        if (!StringUtils.hasText(value)) return null;
        String v = value.trim();
        int idx = v.indexOf(':');
        return idx >= 0 ? v.substring(idx + 1) : v;
    }

    /**
     * A {@code isRelevantXmlNode} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>Az XML-adatot a alkalmazási folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param name a feloldáshoz vagy azonosításhoz használt név
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isRelevantXmlNode(String name) {
        if (!StringUtils.hasText(name)) return false;
        return name.startsWith("Doc_")
                || name.startsWith("Form_")
                || BLOCK_PATTERN.matcher(name).matches()
                || CHAIN_PATTERN.matcher(name).matches()
                || "Chain_elem".equals(name)
                || FIELD_GROUP_PATTERN.matcher(name).matches()
                || FIELD_PATTERN.matcher(name).matches();
    }

    /**
     * A {@code nodeType} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param name a feloldáshoz vagy azonosításhoz használt név
     * @return a művelet feldolgozási eredménye
     */
    private String nodeType(String name) {
        if (FIELD_PATTERN.matcher(name).matches()) return "FIELD";
        if (CHAIN_PATTERN.matcher(name).matches()) return "CHAIN";
        if ("Chain_elem".equals(name)) return "CHAIN_ELEM";
        if (FIELD_GROUP_PATTERN.matcher(name).matches()) return "FIELD_GROUP";
        if (BLOCK_PATTERN.matcher(name).matches()) return "BLOCK";
        if (name.startsWith("Form_")) return "FORM_PART";
        if (name.startsWith("Doc_")) return "DOCUMENT";
        return "NODE";
    }

    /**
     * A {@code readAnnotation} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param element a művelet bemeneti {@code element} értéke
     * @param fallback a művelet bemeneti {@code fallback} értéke
     * @return a feloldott vagy lekért érték
     */
    private String readAnnotation(Element element, String fallback) {
        for (Element child : directChildElements(element)) {
            if (!"annotation".equals(child.getLocalName())) continue;
            NodeList descendants = child.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "documentation");
            for (int i = 0; i < descendants.getLength(); i++) {
                String text = descendants.item(i).getTextContent();
                if (StringUtils.hasText(text)) return text.trim().replaceAll("\\s+", " ");
            }
        }
        return fallback;
    }

    /**
     * A {@code toPath} művelet előállítja a hívó réteg által használt reprezentációt.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    private String toPath(Deque<String> path) {
        return path.stream().collect(Collectors.joining("/", "/", ""));
    }

    /**
     * A {@code applyConfiguredFlags} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @param parts a feldolgozandó elemek kollekciója
     * @param fields a feldolgozandó elemek kollekciója
     * @return a művelet eredményeként előállított elemek listája
     */
    private List<FormPartDto> applyConfiguredFlags(List<FormPartDto> parts, List<IndexFieldDto> fields) {
        if (parts == null || parts.isEmpty()) return List.of();
        Map<String, Long> fieldsByPart = fields.stream()
                .filter(f -> StringUtils.hasText(f.getFormPartName()))
                .collect(Collectors.groupingBy(IndexFieldDto::getFormPartName, LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> configuredByPart = fields.stream()
                .filter(f -> StringUtils.hasText(f.getFormPartName()))
                .filter(f -> f.isSearchable() || f.isDisplay() || f.isDefaultSearch())
                .collect(Collectors.groupingBy(IndexFieldDto::getFormPartName, LinkedHashMap::new, Collectors.counting()));
        for (FormPartDto part : parts) {
            part.setFieldCount(fieldsByPart.getOrDefault(part.getName(), 0L).intValue());
            part.setConfigured(configuredByPart.getOrDefault(part.getName(), 0L) > 0);
        }
        return parts;
    }

    /**
     * A {@code mergeSavedFlags} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param parsedFields a feldolgozandó elemek kollekciója
     * @param saved a művelet bemeneti {@code saved} értéke
     * @return a művelet eredményeként előállított elemek listája
     */
    private List<IndexFieldDto> mergeSavedFlags(List<IndexFieldDto> parsedFields, IndexFormConfigDto saved) {
        if (parsedFields == null) return new ArrayList<>();
        Map<String, IndexFieldDto> savedByPath = new LinkedHashMap<>();
        Map<String, IndexFieldDto> savedByPartAndName = new LinkedHashMap<>();
        Map<String, IndexFieldDto> savedByName = new LinkedHashMap<>();
        Map<String, Long> parsedNameCounts = parsedFields.stream()
                .filter(Objects::nonNull)
                .filter(field -> StringUtils.hasText(field.getName()))
                .collect(Collectors.groupingBy(IndexFieldDto::getName, LinkedHashMap::new, Collectors.counting()));
        if (saved != null) {
            for (IndexFieldDto field : collectRequestFields(saved)) {
                if (StringUtils.hasText(field.getXmlPath())) savedByPath.put(field.getXmlPath(), field);
                if (StringUtils.hasText(field.getFormPartName()) && StringUtils.hasText(field.getName())) {
                    savedByPartAndName.put(field.getFormPartName() + "|" + field.getName(), field);
                }
                if (StringUtils.hasText(field.getName())) savedByName.putIfAbsent(field.getName(), field);
            }
        }
        for (IndexFieldDto field : parsedFields) {
            IndexFieldDto savedField = savedByPath.get(field.getXmlPath());
            if (savedField == null && StringUtils.hasText(field.getFormPartName())) savedField = savedByPartAndName.get(field.getFormPartName() + "|" + field.getName());
            // Legacy név-alapú fallback csak egyértelmű mezőnévnél engedhető meg.
            // Multiform/ismétlődő struktúrában ugyanaz a fieldId több teljes XML path-on is
            // előfordulhat; ilyenkor a globális név-fallback összekeverné a form partokat.
            if (savedField == null && parsedNameCounts.getOrDefault(field.getName(), 0L) == 1L) {
                savedField = savedByName.get(field.getName());
            }
            if (savedField != null) {
                field.setSearchable(savedField.isSearchable());
                field.setDisplay(savedField.isDisplay());
                field.setDefaultSearch(savedField.isDefaultSearch());
                field.setMatchMode(StringUtils.hasText(savedField.getMatchMode()) ? savedField.getMatchMode() : "contains");
            }
        }
        return parsedFields;
    }

    /**
     * A {@code collectRequestFields} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param request a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a művelet eredményeként előállított elemek listája
     */
    private List<IndexFieldDto> collectRequestFields(IndexFormConfigDto request) {
        List<IndexFieldDto> result = new ArrayList<>();
        if (request == null) return result;
        if (request.getFields() != null) result.addAll(request.getFields());
        if (request.getChains() != null) {
            for (IndexChainDto chain : request.getChains()) {
                if (chain != null && chain.getFields() != null) result.addAll(chain.getFields());
            }
        }
        return result;
    }

    /**
     * A {@code sanitizeFields} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param fields a feldolgozandó elemek kollekciója
     * @return a művelet eredményeként előállított elemek listája
     */
    private List<IndexFieldDto> sanitizeFields(List<IndexFieldDto> fields) {
        if (fields == null) return new ArrayList<>();
        Map<String, IndexFieldDto> sanitized = new LinkedHashMap<>();
        for (IndexFieldDto field : fields) {
            if (field == null || !StringUtils.hasText(field.getName())) continue;
            if (!field.isSearchable() && !field.isDisplay() && !field.isDefaultSearch()) continue;
            IndexFieldDto f = new IndexFieldDto();
            f.setName(field.getName());
            f.setLabel(firstText(field.getLabel(), field.getName()));
            f.setXmlPath(field.getXmlPath());
            f.setFormPartName(field.getFormPartName());
            f.setFormPartRole(field.getFormPartRole());
            f.setParentInfo(field.getParentInfo());
            f.setSearchable(field.isSearchable() || field.isDefaultSearch());
            f.setDisplay(field.isDisplay());
            f.setDefaultSearch(field.isDefaultSearch());
            f.setMatchMode(normalizeMatchMode(field.getMatchMode()));
            sanitized.put(StringUtils.hasText(f.getXmlPath()) ? f.getXmlPath() : firstText(f.getFormPartName(), "") + "|" + f.getName(), f);
        }
        return new ArrayList<>(sanitized.values());
    }

    /**
     * A {@code normalizeMatchMode} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String normalizeMatchMode(String value) {
        if (value == null) return "contains";
        String v = value.trim();
        if ("exact".equals(v) || "startsWith".equals(v) || "contains".equals(v)) return v;
        return "contains";
    }

    /**
     * A {@code loadAllConfigs} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    private Map<String, IndexFormConfigDto> loadAllConfigs() {
        Path path = configPath();
        if (!ExceptionSafeOperations.isRegularFile(path)) {
            return new TreeMap<>();
        }
        try {
            Document doc = secureDocumentBuilder().parse(path.toFile());
            Map<String, IndexFormConfigDto> result = new TreeMap<>();
            NodeList forms = doc.getDocumentElement().getElementsByTagName("form");
            for (int i = 0; i < forms.getLength(); i++) {
                Element form = (Element) forms.item(i);
                IndexFormConfigDto dto = new IndexFormConfigDto();
                dto.setFormName(normalizeFormName(form.getAttribute("formName")));
                dto.setLabel(form.getAttribute("label"));
                dto.setStructureSourceVersion(form.getAttribute("structureSourceVersion"));
                List<IndexFieldDto> fields = new ArrayList<>();

                NodeList directChildren = form.getChildNodes();
                for (int c = 0; c < directChildren.getLength(); c++) {
                    Node child = directChildren.item(c);
                    if (child instanceof Element element && "field".equals(element.getTagName())) {
                        fields.add(readFieldElement(element, null, null));
                    } else if (child instanceof Element part && "part".equals(part.getTagName())) {
                        String partName = firstText(part.getAttribute("name"), part.getAttribute("formPartName"));
                        String partRole = part.getAttribute("role");
                        NodeList partFields = part.getChildNodes();
                        for (int f = 0; f < partFields.getLength(); f++) {
                            Node partField = partFields.item(f);
                            if (partField instanceof Element element && "field".equals(element.getTagName())) {
                                fields.add(readFieldElement(element, partName, partRole));
                            }
                        }
                    }
                }

                // Legacy Sprint 11 v1 compatibility: <form><chain><field .../></chain></form>
                NodeList chainNodes = form.getElementsByTagName("chain");
                for (int c = 0; c < chainNodes.getLength(); c++) {
                    Element chain = (Element) chainNodes.item(c);
                    NodeList fieldNodes = chain.getElementsByTagName("field");
                    for (int f = 0; f < fieldNodes.getLength(); f++) {
                        fields.add(readFieldElement((Element) fieldNodes.item(f), null, null));
                    }
                }
                dto.setFields(sanitizeFields(fields));
                if (StringUtils.hasText(dto.getFormName())) result.put(dto.getFormName(), dto);
            }
            return result;
        } catch (Exception ex) {
            log.warn("XML index konfiguráció beolvasása sikertelen: {}", path, ex);
            return new TreeMap<>();
        }
    }

    /**
     * A {@code readFieldElement} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param field a művelet bemeneti {@code field} értéke
     * @param defaultPartName a feloldáshoz vagy azonosításhoz használt név
     * @param defaultPartRole a művelet bemeneti {@code defaultPartRole} értéke
     * @return a feloldott vagy lekért érték
     */
    private IndexFieldDto readFieldElement(Element field, String defaultPartName, String defaultPartRole) {
        IndexFieldDto fieldDto = new IndexFieldDto();
        fieldDto.setName(field.getAttribute("name"));
        fieldDto.setLabel(field.getAttribute("label"));
        fieldDto.setXmlPath(firstText(field.getAttribute("path"), field.getAttribute("xmlPath")));
        fieldDto.setFormPartName(firstText(field.getAttribute("formPart"), firstText(field.getAttribute("formPartName"), defaultPartName)));
        fieldDto.setFormPartRole(firstText(field.getAttribute("formPartRole"), defaultPartRole));
        fieldDto.setParentInfo(field.getAttribute("parentInfo"));
        fieldDto.setSearchable(Boolean.parseBoolean(field.getAttribute("searchable")));
        fieldDto.setDisplay(Boolean.parseBoolean(field.getAttribute("display")));
        fieldDto.setDefaultSearch(Boolean.parseBoolean(field.getAttribute("defaultSearch")));
        fieldDto.setMatchMode(firstText(field.getAttribute("matchMode"), "contains"));
        return fieldDto;
    }

    /**
     * A {@code writeAllConfigs} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @param forms a művelet bemeneti {@code forms} értéke
     */
    private void writeAllConfigs(Map<String, IndexFormConfigDto> forms) {
        try {
            Path path = configPath();
            ExceptionSafeOperations.createDirectories(path.getParent());
            Document doc = secureDocumentBuilder().newDocument();
            Element root = doc.createElement("xmlIndexConfig");
            doc.appendChild(root);
            for (IndexFormConfigDto form : forms.values()) {
                Element formEl = doc.createElement("form");
                formEl.setAttribute("formName", form.getFormName());
                if (StringUtils.hasText(form.getLabel())) formEl.setAttribute("label", form.getLabel());
                if (StringUtils.hasText(form.getStructureSourceVersion())) formEl.setAttribute("structureSourceVersion", form.getStructureSourceVersion());
                root.appendChild(formEl);

                Map<String, List<IndexFieldDto>> byPart = form.getFields().stream()
                        .collect(Collectors.groupingBy(field -> firstText(field.getFormPartName(), "__ROOT__"), LinkedHashMap::new, Collectors.toList()));
                for (Map.Entry<String, List<IndexFieldDto>> entry : byPart.entrySet()) {
                    if ("__ROOT__".equals(entry.getKey())) {
                        for (IndexFieldDto field : entry.getValue()) formEl.appendChild(toFieldElement(doc, field));
                    } else {
                        Element partEl = doc.createElement("part");
                        partEl.setAttribute("name", entry.getKey());
                        String role = entry.getValue().stream().map(IndexFieldDto::getFormPartRole).filter(StringUtils::hasText).findFirst().orElse("SINGLE");
                        partEl.setAttribute("role", role);
                        formEl.appendChild(partEl);
                        for (IndexFieldDto field : entry.getValue()) partEl.appendChild(toFieldElement(doc, field));
                    }
                }
            }
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(doc), new StreamResult(SecureFileOperations.newPrivateBufferedWriter(path, StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("XML index konfiguráció mentése sikertelen: " + ex.getMessage(), ex);
        }
    }

    /**
     * A {@code toFieldElement} művelet előállítja a hívó réteg által használt reprezentációt.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param doc a művelet bemeneti {@code doc} értéke
     * @param field a művelet bemeneti {@code field} értéke
     * @return a művelet feldolgozási eredménye
     */
    private Element toFieldElement(Document doc, IndexFieldDto field) {
        Element fieldEl = doc.createElement("field");
        fieldEl.setAttribute("name", field.getName());
        fieldEl.setAttribute("label", firstText(field.getLabel(), field.getName()));
        if (StringUtils.hasText(field.getXmlPath())) fieldEl.setAttribute("path", field.getXmlPath());
        if (StringUtils.hasText(field.getParentInfo())) fieldEl.setAttribute("parentInfo", field.getParentInfo());
        fieldEl.setAttribute("searchable", Boolean.toString(field.isSearchable()));
        fieldEl.setAttribute("display", Boolean.toString(field.isDisplay()));
        fieldEl.setAttribute("defaultSearch", Boolean.toString(field.isDefaultSearch()));
        fieldEl.setAttribute("matchMode", normalizeMatchMode(field.getMatchMode()));
        return fieldEl;
    }

    /**
     * A {@code secureDocumentBuilder} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private DocumentBuilder secureDocumentBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder();
    }

    /**
     * A {@code configPath} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @return a művelet feldolgozási eredménye
     */
    private Path configPath() {
        return Path.of(properties.getConfigPath());
    }

    /**
     * A {@code requireForm} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @return a művelet feldolgozási eredménye
     */
    private String requireForm(String formName) {
        String normalized = normalizeFormName(formName);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("Űrlap megadása kötelező.");
        }
        return normalized;
    }

    /**
     * A {@code normalizeFormName} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String normalizeFormName(String value) {
        if (!StringUtils.hasText(value)) return null;
        // A NAV_ előtag nem megjelenítési díszítés, hanem a technikai
        // dokumentumtípus része. Rövidítés kizárólag a frontend címkéiben történhet.
        return value.trim();
    }

    /**
     * A {@code firstText} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @param fallback a művelet bemeneti {@code fallback} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String firstText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    /**
     * A web modul alkalmazási területének közös alkalmazási típusa.
     *
     * <p>A {@code ParsedStructure} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record ParsedStructure(List<IndexTreeNodeDto> tree, List<IndexFieldDto> fields, List<IndexChainDto> chains, List<FormPartDto> formParts) {}
    /**
     * A web modul alkalmazási területének közös alkalmazási típusa.
     *
     * <p>A {@code XsdSource} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record XsdSource(String formName, String version, Path path) {}
    /**
     * A web modul alkalmazási területének közös alkalmazási típusa.
     *
     * <p>A {@code FormPartContext} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record FormPartContext(String name, String role, String xmlPath) {}
    /**
     * A web modul alkalmazási területének közös alkalmazási típusa.
     *
     * <p>A {@code SchemaIndex} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record SchemaIndex(Map<String, Element> complexTypes, Map<String, Element> globalElements) {}
}
