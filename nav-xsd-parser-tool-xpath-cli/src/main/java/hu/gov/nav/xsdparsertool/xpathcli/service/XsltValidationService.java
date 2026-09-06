package hu.gov.nav.xsdparsertool.xpathcli.service;

import hu.gov.nav.xsdparsertool.xpathcli.model.XsltValidationResult;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * XSLT-alapú XML-validációt végző, önálló CLI-szolgáltatás.
 *
 * <p>A szolgáltatás Saxon-HE használatával lefordítja a megadott XSL/XSLT
 * állományt, beállítja a NAV validációs sémák által használt stylesheet
 * paramétereket, majd lefuttatja a transzformációt az XML bemeneten. A teljes
 * transzformációs eredményt XML szövegként megőrzi, és abból külön listába
 * gyűjti a {@code Hiba}/{@code hiba} elemekből kinyerhető üzeneteket.</p>
 *
 * <p>Az XSLT által behúzott további erőforrások feloldása korlátozott:
 * kizárólag {@code file:} sémájú, a fő XSL állomány könyvtárán belül maradó
 * erőforrás olvasható. Ez megakadályozza a hálózati erőforrások és a megadott
 * könyvtáron kívüli fájlok XSLT-ből történő elérését.</p>
 *
 * <p>A típus nem Spring bean; a {@code xpath-cli} modul közvetlenül példányosítja.</p>
 */
public class XsltValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(XsltValidationService.class);

    /**
     * Lefuttatja az XSLT-alapú validációt a megadott XML dokumentumon.
     *
     * <p>A feldolgozás sorrendje: Saxon processor létrehozása, korlátozott
     * URI-feloldó beállítása, stylesheet fordítása, paraméterek beállítása,
     * transzformáció memóriabeli XML kimenetre, majd a validációs hibaelemek
     * kinyerése. Ha a {@code encoding} értéke {@code null}, a JVM alapértelmezett
     * karakterkódolása kerül használatra.</p>
     *
     * @param xslPath a lefordítandó XSL/XSLT állomány elérési útja; nem lehet
     *                {@code null}
     * @param xmlInputPath a validálandó XML állomány elérési útja; nem lehet
     *                     {@code null}
     * @param rulesRoot a stylesheet {@code rules-root} paraméterének értéke
     * @param rulesDir a stylesheet {@code rules-dir} paraméterének értéke
     * @param formName a stylesheet {@code form-name} paraméterének értéke
     * @param formVersion a stylesheet {@code form-version} paraméterének értéke
     * @param rulesFile a stylesheet {@code rules-file} paraméterének értéke
     * @param encoding a transzformáció XML kimenetének karakterkódolása;
     *                 {@code null} esetén a platform alapértelmezett kódolása
     * @return a teljes transzformációs kimenet és a belőle kinyert hibaüzenetek
     * @throws IOException ha az XSL/XSLT vagy XML állomány nem olvasható
     * @throws SaxonApiException ha a stylesheet fordítása vagy a transzformáció
     *                           Saxon szinten hibával leáll
     * @throws NullPointerException ha az {@code xslPath} vagy az
     *                              {@code xmlInputPath} {@code null}
     */
    public XsltValidationResult validate(Path xslPath,
                                         Path xmlInputPath,
                                         String rulesRoot,
                                         String rulesDir,
                                         String formName,
                                         String formVersion,
                                         String rulesFile,
                                         Charset encoding) throws IOException, SaxonApiException {
        Objects.requireNonNull(xslPath, "xslPath must not be null");
        Objects.requireNonNull(xmlInputPath, "xmlInputPath must not be null");

        Charset effectiveEncoding = encoding != null ? encoding : Charset.defaultCharset();
        LOGGER.info("Starting XSLT validation. xslPath={}, xmlInputPath={}", xslPath, xmlInputPath);

        Processor processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();
        compiler.setURIResolver(restrictedResolver(xslPath.getParent()));
        XsltExecutable executable = compiler.compile(new StreamSource(Files.newInputStream(xslPath), xslPath.toUri().toString()));
        Xslt30Transformer transformer = executable.load30();
        transformer.setStylesheetParameters(createParameters(rulesRoot, rulesDir, formName, formVersion, rulesFile));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Serializer serializer = processor.newSerializer(outputStream);
        serializer.setOutputProperty(Serializer.Property.ENCODING, effectiveEncoding.name());
        serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
        serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "no");

        transformer.transform(new StreamSource(Files.newInputStream(xmlInputPath), xmlInputPath.toUri().toString()), serializer);

        String rawXml = outputStream.toString(effectiveEncoding);
        List<String> errorMessages = parseHibaElements(rawXml);
        LOGGER.info("XSLT validation finished. errorCount={}", errorMessages.size());
        return new XsltValidationResult(rawXml, errorMessages);
    }

    /**
     * Olyan URI-feloldót készít, amely az XSLT külső erőforrásait a megadott
     * gyökérkönyvtárra korlátozza.
     *
     * <p>A feloldás a {@code base} URI-t használja, ha az rendelkezésre áll;
     * különben az {@code href} értéket az engedélyezett gyökérhez képest oldja
     * fel. A feloldott URI kizárólag {@code file:} sémájú lehet, és a
     * normalizált célútvonalnak az engedélyezett gyökér alatt kell maradnia.
     * Minden más séma vagy könyvtárból történő kilépés hibát eredményez.</p>
     *
     * @param allowedRoot az XSLT által elérhető helyi erőforrások gyökérkönyvtára
     * @return a Saxon compilerhez beállítható korlátozott URI-feloldó
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
     * Összeállítja a stylesheet futtatásakor átadandó paramétertérképet.
     *
     * <p>A fájlrendszeri jellegű {@code rules-root} és {@code rules-file}
     * értékekben a Windows könyvtárelválasztó karakterek perjelre cserélődnek.
     * Üres vagy csak whitespace karaktereket tartalmazó érték nem kerül be a
     * paramétertérképbe.</p>
     *
     * @param rulesRoot a {@code rules-root} paraméter nyers értéke
     * @param rulesDir a {@code rules-dir} paraméter értéke
     * @param formName a {@code form-name} paraméter értéke
     * @param formVersion a {@code form-version} paraméter értéke
     * @param rulesFile a {@code rules-file} paraméter nyers értéke
     * @return a Saxon számára átadható stylesheet-paraméterek
     */
    private Map<QName, XdmValue> createParameters(String rulesRoot,
                                                  String rulesDir,
                                                  String formName,
                                                  String formVersion,
                                                  String rulesFile) {
        Map<QName, XdmValue> parameters = new HashMap<>();
        putIfPresent(parameters, "rules-root", normalizePath(rulesRoot));
        putIfPresent(parameters, "rules-dir", rulesDir);
        putIfPresent(parameters, "form-name", formName);
        putIfPresent(parameters, "form-version", formVersion);
        putIfPresent(parameters, "rules-file", normalizePath(rulesFile));
        return parameters;
    }

    /**
     * Hozzáad egy stylesheet-paramétert a célmaphez, ha annak értéke érdemi.
     *
     * <p>{@code null}, üres vagy csak whitespace karaktereket tartalmazó érték
     * esetén a paraméter szándékosan kimarad, így a stylesheet saját
     * alapértelmezése érvényesülhet.</p>
     *
     * @param target a módosítandó paramétertérkép
     * @param parameterName a stylesheet-paraméter neve
     * @param parameterValue a beállítandó szöveges érték
     */
    private void putIfPresent(Map<QName, XdmValue> target, String parameterName, String parameterValue) {
        if (parameterValue != null && !parameterValue.isBlank()) {
            target.put(new QName(parameterName), new XdmAtomicValue(parameterValue));
        }
    }

    /**
     * Egységesíti a fájlrendszeri útvonalak könyvtárelválasztó karakterét az
     * XSLT-paraméterként történő átadás előtt.
     *
     * @param value a normalizálandó útvonal vagy {@code null}
     * @return az érték perjeles könyvtárelválasztókkal, vagy {@code null}, ha a
     *         bemenet is {@code null}
     */
    private String normalizePath(String value) {
        return value == null ? null : value.replace('\\', '/');
    }

    /**
     * Kinyeri a validációs hibaüzeneteket a transzformáció eredmény-XML-jéből.
     *
     * <p>A keresés namespace-től és kis-/nagybetűs eltéréstől részben független
     * fallback sorrendet használ: először namespace-független {@code Hiba}, majd
     * egyszerű {@code Hiba}, ezt követően namespace-független {@code hiba}, végül
     * egyszerű {@code hiba} elemeket keres. Egy hibaelem üzenetének prioritása:
     * {@code hibaszoveg} attribútum, {@code message} attribútum, majd az elem
     * teljes szöveges tartalma.</p>
     *
     * <p>Az eredmény feldolgozásához használt DOM parser letiltja a DOCTYPE és
     * külső entitások feldolgozását. Ha az eredmény nem dolgozható fel XML-ként,
     * a metódus naplózza a problémát és egy technikai hibaüzenetet tesz a
     * visszaadott listába; ezzel a CLI a futást hibás validációként tudja jelezni.</p>
     *
     * @param xml a Saxon transzformáció nyers XML kimenete
     * @return a megtalált hibaüzenetek; üres lista, ha nincs feldolgozható
     *         {@code Hiba}/{@code hiba} elem
     */
    private List<String> parseHibaElements(String xml) {
        List<String> messages = new ArrayList<>();
        if (xml == null || xml.isBlank()) {
            return messages;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList hibaNodes = document.getElementsByTagNameNS("*", "Hiba");
            if (hibaNodes.getLength() == 0) {
                hibaNodes = document.getElementsByTagName("Hiba");
            }
            if (hibaNodes.getLength() == 0) {
                hibaNodes = document.getElementsByTagNameNS("*", "hiba");
            }
            if (hibaNodes.getLength() == 0) {
                hibaNodes = document.getElementsByTagName("hiba");
            }
            for (int index = 0; index < hibaNodes.getLength(); index++) {
                Element element = (Element) hibaNodes.item(index);
                String message = firstNonBlank(
                        element.getAttribute("hibaszoveg"),
                        element.getAttribute("message"),
                        element.getTextContent()
                );
                if (message != null) {
                    messages.add(message.trim());
                }
            }
        } catch (Exception exception) {
            LOGGER.warn("Could not parse XSLT result XML for Hiba/hiba elements: {}", exception.getMessage());
            messages.add("A validacios XSLT eredmenye nem volt feldolgozhato XML-kent.");
        }
        return messages;
    }

    /**
     * Visszaadja a megadott értékek közül az első nem üres szöveget.
     *
     * <p>A segédmetódus a hibaüzenet-feloldás prioritását valósítja meg; a
     * sorrend ezért jelentéssel bír, és nem rendezhető át tetszőlegesen.</p>
     *
     * @param values prioritási sorrendben vizsgálandó értékek
     * @return az első nem {@code null}, nem üres érték; {@code null}, ha egyik
     *         jelölt sem tartalmaz érdemi szöveget
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
