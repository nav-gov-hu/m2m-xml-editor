package hu.gov.nav.xsdparsertool.web.xpath.service;

import hu.gov.nav.xsdparsertool.web.xpath.model.XPathValidationExecutionResult;
import hu.gov.nav.xsdparsertool.web.xpath.model.XPathValidationIssue;
import net.sf.saxon.s9api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * A webes XPath-validációhoz tartozó XSLT transzformációt, paraméterezést és eredményfeldolgozást végző szolgáltatás.
 * Az osztály a service csomagból érhető el, és a projekt többi rétege innen hívja a publikus API-ját.
 * Spring regisztráció: Nincs közvetlen Spring bean regisztráció.
 * Interfész/implementáció kapcsolat: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *
 * Spring registration: Nincs közvetlen Spring bean regisztráció.
 * Interface/implementation relationship: Az osztály önálló típusként működik, és ahol van, ott interfészhez vagy Spring szerződéshez kapcsolódik.
 *

 */


public class XsltValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(XsltValidationService.class);
/**
 * Lefuttatja az XSLT-alapú validációt a megadott XML-en, és strukturált hibákká alakítja a transzformáció eredményét.
 * @param xslPath a {@code xslPath} paraméter átadott értéke
 * @param xmlInputPath a {@code xmlInputPath} paraméter átadott értéke
 * @param rulesRoot a {@code rulesRoot} paraméter átadott értéke
 * @param rulesDir a {@code rulesDir} paraméter átadott értéke
 * @param formName a {@code formName} paraméter átadott értéke
 * @param formVersion a {@code formVersion} paraméter átadott értéke
 * @param rulesFile a {@code rulesFile} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 * @throws IOException Hiba esetén dobott kivétel.
 * @throws SaxonApiException Hiba esetén dobott kivétel.
 */

    public XPathValidationExecutionResult validate(Path xslPath,
                                                   Path xmlInputPath,
                                                   String rulesRoot,
                                                   String rulesDir,
                                                   String formName,
                                                   String formVersion,
                                                   String rulesFile) throws IOException, SaxonApiException {
        LOGGER.info("XSLT validate START. xslPath={}, xmlInputPath={}, rulesRoot={}, rulesDir={}, formName={}, formVersion={}, rulesFile={}",
                xslPath, xmlInputPath, rulesRoot, rulesDir, formName, formVersion, rulesFile);

        Processor processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();
        compiler.setURIResolver(restrictedResolver(xslPath.getParent()));
        LOGGER.info("Compiling XSLT stylesheet...");
        XsltExecutable executable = compiler.compile(new StreamSource(Files.newInputStream(xslPath), xslPath.toUri().toString()));
        Xslt30Transformer transformer = executable.load30();
        Map<QName, XdmValue> parameters = createParameters(rulesRoot, rulesDir, formName, formVersion, rulesFile);
        transformer.setStylesheetParameters(parameters);
        LOGGER.info("XSLT stylesheet compiled. parameterCount={}", parameters.size());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Serializer serializer = processor.newSerializer(outputStream);
        serializer.setOutputProperty(Serializer.Property.ENCODING, StandardCharsets.UTF_8.name());
        serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "no");

        LOGGER.info("Executing XSLT transform...");
        transformer.transform(new StreamSource(Files.newInputStream(xmlInputPath), xmlInputPath.toUri().toString()), serializer);
        String rawXml = outputStream.toString(StandardCharsets.UTF_8);
        LOGGER.info("XSLT transform finished. rawXmlLength={}", rawXml.length());
        List<XPathValidationIssue> issues = parseIssues(rawXml);
        LOGGER.info("XSLT result parsed. issueCount={}", issues.size());
        return new XPathValidationExecutionResult(rawXml, issues);
    }
/**
 * Összeállítja az XSLT futtatásához szükséges stylesheet-paramétereket a validációs kérésből.
 * @param rulesRoot a {@code rulesRoot} paraméter átadott értéke
 * @param rulesDir a {@code rulesDir} paraméter átadott értéke
 * @param formName a {@code formName} paraméter átadott értéke
 * @param formVersion a {@code formVersion} paraméter átadott értéke
 * @param rulesFile a {@code rulesFile} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private URIResolver restrictedResolver(Path allowedRoot) {
        Path root = allowedRoot.toAbsolutePath().normalize();
        return (href, base) -> {
            try {
                java.net.URI resolved = base == null ? root.resolve(href).toUri() : java.net.URI.create(base).resolve(href);
                if (!"file".equalsIgnoreCase(resolved.getScheme())) {
                    throw new TransformerException("Külső XSLT erőforrás-séma tiltott: " + resolved.getScheme());
                }
                Path target = Path.of(resolved).toAbsolutePath().normalize();
                if (!target.startsWith(root)) {
                    throw new TransformerException("Az XSLT erőforrás kilép az engedélyezett könyvtárból: " + target);
                }
                return new StreamSource(Files.newInputStream(target), target.toUri().toString());
            } catch (IOException | IllegalArgumentException ex) {
                throw new TransformerException("Az XSLT erőforrás nem oldható fel biztonságosan.", ex);
            }
        };
    }

    /**
     * A {@code createParameters} művelet létrehozza vagy tartósítja a kért állapotváltozást.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param rulesRoot a művelet bemeneti {@code rulesRoot} értéke
     * @param rulesDir a művelet bemeneti {@code rulesDir} értéke
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @param formVersion a művelet bemeneti {@code formVersion} értéke
     * @param rulesFile a feldolgozásban részt vevő fájl vagy elérési út
     * @return a feldolgozás során felépített kulcs-érték leképezés
     */
    private Map<QName, XdmValue> createParameters(String rulesRoot, String rulesDir, String formName, String formVersion, String rulesFile) {
        Map<QName, XdmValue> parameters = new HashMap<>();
        putIfPresent(parameters, "rules-root", normalizePath(rulesRoot));
        putIfPresent(parameters, "rules-dir", normalizePath(rulesDir));
        putIfPresent(parameters, "form-name", formName);
        putIfPresent(parameters, "form-version", formVersion);
        putIfPresent(parameters, "rules-file", normalizePath(rulesFile));
        return parameters;
    }
/**
 * Csak nem üres paraméterértéket tesz az XSLT paramétertérképbe.
 * @param target a {@code target} paraméter átadott értéke
 * @param parameterName a {@code parameterName} paraméter átadott értéke
 * @param parameterValue a {@code parameterValue} paraméter átadott értéke
 */

    private void putIfPresent(Map<QName, XdmValue> target, String parameterName, String parameterValue) {
        if (parameterValue != null && !parameterValue.isBlank()) {
            target.put(new QName(parameterName), new XdmAtomicValue(parameterValue));
            LOGGER.info("XSLT parameter set. {}={}", parameterName, parameterValue);
        } else {
            LOGGER.info("XSLT parameter skipped because value is blank. parameterName={}", parameterName);
        }
    }
/**
 * Az XSLT számára egységes perjeles formára normalizálja a fájlrendszeri útvonalat.
 * @param value a {@code value} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private String normalizePath(String value) {
        return value == null ? null : value.replace('\\', '/');
    }
/**
 * A validáció eredmény-XML-jéből kinyeri és strukturált XPath validációs hibákká alakítja a hibaelemeket.
 * @param xml a {@code xml} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private List<XPathValidationIssue> parseIssues(String xml) {
        List<XPathValidationIssue> issues = new ArrayList<>();
        if (xml == null || xml.isBlank()) {
            LOGGER.warn("XSLT result XML is empty, no issues can be parsed.");
            return issues;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList nodes = document.getElementsByTagNameNS("*", "Hiba");
            if (nodes.getLength() == 0) nodes = document.getElementsByTagName("Hiba");
            if (nodes.getLength() == 0) nodes = document.getElementsByTagNameNS("*", "hiba");
            if (nodes.getLength() == 0) nodes = document.getElementsByTagName("hiba");
            LOGGER.info("Issue nodes discovered in result XML. nodeCount={}", nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                String message = firstNonBlank(attr(element, "hibaszoveg"), attr(element, "message"), element.getTextContent());
                String path = attr(element, "path");
                XPathValidationIssue issue = new XPathValidationIssue(
                        attr(element, "kod"),
                        message,
                        attr(element, "szint"),
                        attr(element, "dinamikusLapIndex"),
                        resolveElementId(attr(element, "elem"), message, path),
                        attr(element, "ruleId"),
                        path
                );
                issues.add(issue);
                if (i < 5) {
                    LOGGER.info("Parsed issue[{}]. code={}, severity={}, ruleId={}, elementId={}", i, issue.errorCode(), issue.severity(), issue.ruleId(), issue.elementId());
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Could not parse XSLT result XML: {}", ex.getMessage(), ex);
            issues.add(new XPathValidationIssue("PARSE_ERROR", "A validációs eredmény XML nem volt feldolgozható.", "TECHNICAL", null, null, null, null));
        }
        return issues;
    }
    /**
     * A {@code resolveElementId} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param elem a művelet bemeneti {@code elem} értéke
     * @param message a művelet bemeneti {@code message} értéke
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @return a feloldott vagy lekért érték
     */
    private String resolveElementId(String elem, String message, String path) {
        String fromElem = normalizeElementId(elem);
        String fromPath = normalizeElementId(extractLastFieldId(path));
        String fromMessage = normalizeElementId(extractLastFieldId(message));

        if (isUsefulElementId(fromElem)) return fromElem;
        if (isUsefulElementId(fromPath)) return fromPath;
        if (isUsefulElementId(fromMessage)) return fromMessage;
        return null;
    }

    /**
     * A {@code normalizeElementId} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String normalizeElementId(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    /**
     * A {@code isUsefulElementId} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isUsefulElementId(String value) {
        if (value == null || value.isBlank()) return false;
        return !"field_form".equalsIgnoreCase(value.trim())
                && !"form_field".equalsIgnoreCase(value.trim())
                && !"field-form".equalsIgnoreCase(value.trim())
                && !"form-field".equalsIgnoreCase(value.trim());
    }

    /**
     * A {@code extractLastFieldId} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a XPath-validációs komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param text a művelet bemeneti {@code text} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String extractLastFieldId(String text) {
        if (text == null || text.isBlank()) return null;
        Matcher matcher = Pattern.compile("Field_[A-Za-z0-9]+").matcher(text);
        String last = null;
        while (matcher.find()) {
            last = matcher.group();
        }
        return last;
    }

/**
 * Egy XML elem attribútumértékét olvassa ki null-biztosan.
 * @param element a {@code element} paraméter átadott értéke
 * @param name a {@code name} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private String attr(Element element, String name) {
        String value = element.getAttribute(name);
        return value == null || value.isBlank() ? null : value;
    }
/**
 * Prioritási sorrendben az első nem üres szöveges értéket választja ki.
 * @param values a {@code values} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }
}
