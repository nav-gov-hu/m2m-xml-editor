package hu.gov.nav.xsdparsertool.processing.validation;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;
import hu.gov.nav.xsdparsertool.core.xml.SecureXmlParserSupport;

import hu.gov.nav.xsdparsertool.core.enums.Severity;
import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.processing.ValidationResult;
import hu.gov.nav.xsdparsertool.core.model.validation.ValidationIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;


/**
 * XML állományok XSD-séma szerinti validációját végző szolgáltatás.
 *
 * <p>A Schema Registry által feloldott elsődleges XSD-t használja, az include/import
 * hivatkozásokat pedig {@link MultiPathResourceResolver} segítségével oldja fel, külső hálózati
 * séma- és DTD-hozzáférés nélkül.</p>
 *
 * <p>A validációs hibákhoz streaming XML-bejárással lehetőség szerint pontos, előfordulási
 * indexeket is tartalmazó XML-útvonalat rendel.</p>
 */
public class XsdValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(XsdValidationService.class);
    private static final String INTERNAL_VALIDATION_ERROR = "Validation failed due to an internal processing error.";
    private static final Pattern VALIDATION_ELEMENT_PATTERN = Pattern.compile("element [\'\"]([^\'\"]+)[\'\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALIDATION_FIELD_PATTERN = Pattern.compile("Field_[A-Za-z0-9_]+");

/**
 * Validálja az XML-t a megadott séma-csomag alapján.
 * @param xmlFile a validálandó XML állomány
 * @param schemaBundle a feloldott séma-csomag
 * @return a validáció eredménye és az észlelt hibák
 */
    public ValidationResult validate(Path xmlFile, SchemaBundle schemaBundle) {
        return validate(xmlFile, schemaBundle, null);
    }

/**
 * Validálja az XML-t a séma-csomag és general XSD könyvtár használatával.
 * @param xmlFile a validálandó XML állomány
 * @param schemaBundle a feloldott séma-csomag
 * @param generalXsdDir az általános XSD-k könyvtára, vagy {@code null}
 * @return a validáció eredménye; a hibák lehetőség szerint pontos XML-útvonalat is tartalmaznak
 */
    public ValidationResult validate(Path xmlFile, SchemaBundle schemaBundle, Path generalXsdDir) {
        ValidationResult result = new ValidationResult();
        List<ValidationIssue> issues = new ArrayList<>();

        if (xmlFile == null || !ExceptionSafeOperations.isRegularFile(xmlFile)) {
            issues.add(new ValidationIssue("XML_FILE_NOT_FOUND", xmlFile == null ? null : xmlFile.toString(), "XML file does not exist", Severity.ERROR));
            result.setValid(false);
            result.setIssues(issues);
            return result;
        }
        if (schemaBundle == null || schemaBundle.getPrimaryXsd() == null) {
            issues.add(new ValidationIssue("PRIMARY_XSD_NOT_FOUND", null, "Primary XSD was not resolved", Severity.ERROR));
            result.setValid(false);
            result.setIssues(issues);
            return result;
        }
        Path primaryXsd = schemaBundle.getPrimaryXsd();
        if (!ExceptionSafeOperations.isRegularFile(primaryXsd)) {
            issues.add(new ValidationIssue("PRIMARY_XSD_MISSING", primaryXsd.toString(), "Primary XSD file does not exist", Severity.ERROR));
            result.setValid(false);
            result.setIssues(issues);
            return result;
        }

        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
            schemaFactory.setResourceResolver(new MultiPathResourceResolver(primaryXsd.getParent(), generalXsdDir));
            CollectingErrorHandler errorHandler = new CollectingErrorHandler();

            StreamSource xsdSource = new StreamSource(primaryXsd.toFile());
            xsdSource.setSystemId(primaryXsd.toUri().toString());
            Schema schema = schemaFactory.newSchema(xsdSource);
            Validator validator = schema.newValidator();
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
            validator.setErrorHandler(errorHandler);

            StreamSource xmlSource = new StreamSource(xmlFile.toFile());
            xmlSource.setSystemId(xmlFile.toUri().toString());
            validator.validate(xmlSource);
            issues.addAll(resolveCollectedIssues(xmlFile, errorHandler.issues()));

            result.setValid(issues.stream().noneMatch(issue -> issue.getSeverity() == Severity.ERROR));
            result.setIssues(issues);
            return result;
        } catch (Exception e) {
            LOGGER.error("Unexpected XSD validation failure.", e);
            issues.add(new ValidationIssue("VALIDATION_EXCEPTION", xmlFile.toString(), INTERNAL_VALIDATION_ERROR, Severity.ERROR));
            result.setValid(false);
            result.setIssues(issues);
            return result;
        }
    }

    /**
     * A SAX validátor által összegyűjtött hibákból alkalmazásszintű validációs hibákat készít.
     *
     * <p>A sor/oszlop koordinátákat előbb XML-csomópontokra és kanonikus indexelt
     * útvonalakra oldja, majd az eredeti hibaüzenetet helyinformációval együtt adja át
     * a {@link ValidationIssue} modellnek.</p>
     *
     * @param xmlFile a validált XML állomány
     * @param collected a validátor által összegyűjtött nyers hibák
     * @return az XML-útvonalakkal kiegészített alkalmazásszintű validációs hibák
     */
    private static List<ValidationIssue> resolveCollectedIssues(Path xmlFile, List<CollectedIssue> collected){
        Map<IssueLocationKey, ResolvedXmlLocation> resolved = resolveXmlLocations(xmlFile, collected);
        List<ValidationIssue> issues = new ArrayList<>();
        for (CollectedIssue issue : collected) {
            IssueLocationKey key = new IssueLocationKey(issue.lineNumber(), normalizedColumn(issue.columnNumber()));
            ResolvedXmlLocation location = resolved.get(key);
            String message = "line " + issue.lineNumber() + ", column " + issue.columnNumber() + ": " + issue.message();
            issues.add(new ValidationIssue(issue.code(), location == null ? null : location.path(), message, issue.severity()));
        }
        return issues;
    }

    /**
     * Streaming bejárással XML-útvonalat rendel a validációs hibák sor/oszlop koordinátáihoz.
     *
     * <p>StAX olvasás közben előfordulási indexekkel ellátott útvonalvermet tart fenn.
     * A jelöltek közül előnyben részesíti azt, amelynek elemneve a validátor üzenetéből
     * kinyert elvárt elemnévvel pontosan egyezik; azonos pontosságnál a mélyebb XML-elemet.</p>
     *
     * @param xmlPath a validált XML állomány
     * @param issues a sor/oszlop információt tartalmazó nyers validációs hibák
     * @return a hibahely kulcsáról a legjobb feloldott XML-helyre mutató térkép
     */
    private static Map<IssueLocationKey, ResolvedXmlLocation> resolveXmlLocations(Path xmlPath, List<CollectedIssue> issues) {
        Map<IssueLocationKey, ResolvedXmlLocation> result = new LinkedHashMap<>();
        if (xmlPath == null || !ExceptionSafeOperations.isRegularFile(xmlPath) || issues == null || issues.isEmpty()) return result;
        Map<IssueLocationKey, String> targets = new LinkedHashMap<>();
        for (CollectedIssue issue : issues) {
            if (issue.lineNumber() > 0) {
                IssueLocationKey key = new IssueLocationKey(issue.lineNumber(), normalizedColumn(issue.columnNumber()));
                String expected = extractValidationElementName(issue.message());
                targets.merge(key, expected, (left, right) -> left == null || left.isBlank() ? right : left);
            }
        }
        if (targets.isEmpty()) return result;
        try (InputStream input = Files.newInputStream(xmlPath)) {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            SecureXmlParserSupport.configureSecureXmlInputFactory(factory);
            XMLStreamReader reader = factory.createXMLStreamReader(input);
            Deque<XmlPathFrame> stack = new ArrayDeque<>();
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    XmlPathFrame parent = stack.peekLast();
                    String localName = reader.getLocalName();
                    int index = parent == null ? 1 : parent.nextChildIndex(localName);
                    stack.addLast(new XmlPathFrame(localName, index,
                            Math.max(1, reader.getLocation().getLineNumber()),
                            Math.max(1, reader.getLocation().getColumnNumber())));
                } else if (event == XMLStreamConstants.END_ELEMENT && !stack.isEmpty()) {
                    XmlPathFrame frame = stack.peekLast();
                    frame.close(Math.max(1, reader.getLocation().getLineNumber()),
                            Math.max(1, reader.getLocation().getColumnNumber()));
                    ResolvedXmlLocation candidate = snapshotLocation(stack);
                    for (Map.Entry<IssueLocationKey, String> target : targets.entrySet()) {
                        IssueLocationKey key = target.getKey();
                        String expected = target.getValue();
                        boolean exactElement = expected != null && expected.equals(frame.name());
                        if (!frame.contains(key.line(), key.column()) && !(exactElement && frame.touchesLine(key.line()))) continue;
                        ResolvedXmlLocation scored = candidate.withExactElementMatch(exactElement);
                        ResolvedXmlLocation current = result.get(key);
                        if (current == null || scored.isBetterThan(current)) result.put(key, scored);
                    }
                    stack.removeLast();
                }
            }
            reader.close();
        } catch (Exception e) {
            LOGGER.warn("XSD live validation node resolution failed. xmlPath={} message={}", xmlPath, e.getMessage());
        }
        return result;
    }

    /**
     * Egytől induló, használható oszlopszámra normalizálja a parser által adott értéket.
     *
     * @param column a nyers oszlopszám
     * @return legalább 1 értékű oszlopszám
     */
    private static int normalizedColumn(int column) { return column < 1 ? 1 : column; }

    /**
     * Megpróbálja kinyerni a validátor hibaüzenetéből az érintett XML-elem nevét.
     *
     * <p>Elsőként az „element 'név'” jellegű szöveget keresi, majd NAV mezők esetén
     * a {@code Field_*} mintára esik vissza. A név a sor/oszlop alapú csomópontfeloldás
     * pontosítására szolgál.</p>
     *
     * @param message a nyers XSD-validációs hibaüzenet
     * @return a felismert elemnév, vagy üres szöveg, ha nincs azonosítható elem
     */
    private static String extractValidationElementName(String message) {
        String safe = message == null ? "" : message;
        Matcher elementMatcher = VALIDATION_ELEMENT_PATTERN.matcher(safe);
        if (elementMatcher.find()) return elementMatcher.group(1);
        Matcher fieldMatcher = VALIDATION_FIELD_PATTERN.matcher(safe);
        return fieldMatcher.find() ? fieldMatcher.group() : "";
    }

    /**
     * A pillanatnyi StAX útvonalveremből változatlan helypillanatképet készít.
     *
     * @param stack az aktuálisan nyitott XML-elemek útvonalverme
     * @return a teljes indexelt útvonalat, aktuális elemnevet és mélységet tartalmazó hely
     */
    private static ResolvedXmlLocation snapshotLocation(Deque<XmlPathFrame> stack) {
        StringBuilder path = new StringBuilder();
        XmlPathFrame last = null;
        for (XmlPathFrame frame : stack) {
            path.append('/').append(frame.name()).append('[').append(frame.index()).append(']');
            last = frame;
        }
        return new ResolvedXmlLocation(path.toString(), last == null ? null : last.name(), stack.size(), false);
    }

    /**
     * Egy validációs hiba normalizált sor- és oszlopkoordinátáját azonosítja.
     *
     * @param line az XML-beli sorszám
     * @param column a normalizált oszlopszám
     */
    private record IssueLocationKey(int line, int column) {}

    /**
     * Egy validációs hiba feloldott XML-helyét és annak illeszkedési minőségét tárolja.
     *
     * @param path a kanonikus, indexelt XML-útvonal
     * @param elementName a feloldott legbelső elem neve
     * @param depth az XML-hierarchiában mért mélység
     * @param exactElementMatch jelzi, hogy az elemnév a hibaüzenetből kinyert névvel pontosan egyezett-e
     */
    private record ResolvedXmlLocation(String path, String elementName, int depth, boolean exactElementMatch) {
        /**
         * Azonos helyadatokkal, de megadott pontos-elem egyezési jelzővel készít új példányt.
         *
         * @param exact az új pontos egyezési jelző
         * @return az aktualizált, immutábilis helypéldány
         */
        private ResolvedXmlLocation withExactElementMatch(boolean exact) { return new ResolvedXmlLocation(path, elementName, depth, exact); }
        /**
         * Összehasonlítja két helyjelölt minőségét.
         *
         * <p>A pontos elemnév-egyezés elsőbbséget élvez; azonos egyezési státusznál
         * a mélyebben fekvő XML-csomópont számít jobb találatnak.</p>
         *
         * @param other a jelenlegi összehasonlítási alap
         * @return {@code true}, ha ez a helyjelölt pontosabbnak tekintendő
         */
        private boolean isBetterThan(ResolvedXmlLocation other) {
            if (exactElementMatch != other.exactElementMatch) return exactElementMatch;
            return depth > other.depth;
        }
    }

    /**
     * Egy nyitott XML-elem StAX feldolgozás közbeni útvonal- és forráspozíció-állapotát tartja nyilván.
     *
     * <p>A gyermeknevenkénti számlálók biztosítják az egytől induló előfordulási indexeket,
     * a kezdő és záró koordináták pedig lehetővé teszik annak eldöntését, hogy egy
     * validációs hiba pozíciója az adott elem tartományába esik-e.</p>
     */
    private static final class XmlPathFrame {
        private final String name;
        private final int index;
        private final int startLine;
        private final int startColumn;
        private int endLine;
        private int endColumn;
        private final Map<String, Integer> childCounters = new HashMap<>();

        /**
         * Létrehoz egy útvonalkeretet az XML-elem kezdőpozíciójával.
         *
         * @param name az elem lokális neve
         * @param index az azonos nevű testvérek közötti egyalapú index
         * @param startLine az elem kezdő sora
         * @param startColumn az elem kezdő oszlopa
         */
        private XmlPathFrame(String name, int index, int startLine, int startColumn) {
            this.name = name;
            this.index = index;
            this.startLine = startLine;
            this.startColumn = startColumn;
            this.endLine = startLine;
            this.endColumn = startColumn;
        }
        /**
         * Rögzíti az XML-elem zárópozícióját.
         *
         * @param line a záró sorszám
         * @param column a záró oszlopszám
         */
        private void close(int line, int column) { this.endLine = line; this.endColumn = column; }
        /**
         * Megállapítja, hogy a megadott forráspozíció az elem kezdő- és zárópozíciója közé esik-e.
         *
         * @param line a vizsgált sorszám
         * @param column a vizsgált oszlopszám
         * @return {@code true}, ha a pozíció az elem forrástartományán belül található
         */
        private boolean contains(int line, int column) {
            if (line < startLine || line > endLine) return false;
            if (line == startLine && line == endLine) return column >= startColumn && column <= endColumn;
            if (line == startLine) return column >= startColumn;
            if (line == endLine) return column <= endColumn;
            return true;
        }
        /**
         * Ellenőrzi, hogy az elem forrástartománya érinti-e a megadott sort.
         *
         * @param line a vizsgált sorszám
         * @return {@code true}, ha a sor a kezdő és záró sor közé esik
         */
        private boolean touchesLine(int line) { return line >= startLine && line <= endLine; }
        /**
         * @return az aktuális XML-elem lokális neve
         */
        private String name() { return name; }
        /**
         * @return az azonos nevű testvérek közötti egyalapú előfordulási index
         */
        private int index() { return index; }
        /**
         * Növeli és visszaadja a megadott nevű következő gyermekelem előfordulási indexét.
         *
         * @param childName a gyermekelem lokális neve
         * @return az adott nevű következő gyermek egyalapú indexe
         */
        private int nextChildIndex(String childName) {
            int next = childCounters.getOrDefault(childName, 0) + 1;
            childCounters.put(childName, next);
            return next;
        }
    }

    /**
     * A SAX {@link ErrorHandler} által gyűjtött nyers validációs hiba reprezentációja.
     *
     * @param code az alkalmazásszintű hibakód
     * @param message a parser eredeti hibaüzenete
     * @param severity a hiba súlyossága
     * @param lineNumber a parser által jelzett sorszám
     * @param columnNumber a parser által jelzett oszlopszám
     */
    private record CollectedIssue(String code, String message, Severity severity, int lineNumber, int columnNumber) {}

    /**
     * A validátor callbackjeit kivételdobás helyett rendezett hibagyűjteménybe tereli.
     *
     * <p>A warning, error és fatal error eseményeket egységes {@link CollectedIssue}
     * modellé alakítja, hogy a validáció végén egy lépésben lehessen őket XML-útvonalhoz kötni.</p>
     */
    private static class CollectingErrorHandler implements ErrorHandler {
        private final List<CollectedIssue> issues = new ArrayList<>();
        /**
         * {@inheritDoc}
         */
        @Override public void warning(SAXParseException exception) { issues.add(toIssue("XSD_WARNING", exception, Severity.WARNING)); }
        /**
         * {@inheritDoc}
         */
        @Override public void error(SAXParseException exception) { issues.add(toIssue("XSD_ERROR", exception, Severity.ERROR)); }
        /**
         * {@inheritDoc}
         */
        @Override public void fatalError(SAXParseException exception) { issues.add(toIssue("XSD_FATAL_ERROR", exception, Severity.ERROR)); }
        /**
         * SAX parserhibából belső, helyinformációt megőrző validációs hibát készít.
         *
         * @param code az alkalmazásszintű hibakód
         * @param exception a parser által átadott hiba
         * @param severity a hozzárendelendő súlyosság
         * @return a normalizált nyers hiba
         */
        private CollectedIssue toIssue(String code, SAXParseException exception, Severity severity) {
            return new CollectedIssue(code, exception.getMessage(), severity, exception.getLineNumber(), exception.getColumnNumber());
        }
        /**
         * @return a validáció során eddig összegyűjtött hibák listája
         */
        private List<CollectedIssue> issues() { return issues; }
    }

}
