package hu.gov.nav.xsdparsertool.processing.form;

import hu.gov.nav.xsdparsertool.core.model.form.FormData;
import hu.gov.nav.xsdparsertool.core.model.form.FormDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormFieldDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormRowDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormRowInstance;
import hu.gov.nav.xsdparsertool.core.model.form.FormSectionDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormTabDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * A {@link FormDataBuilderService} DOM-alapú alapértelmezett implementációja.
 *
 * <p>Biztonságos XML parserrel olvas, majd a tab/szekció/sor/mező hierarchia alapján
 * teljes XML-útvonalakhoz és mezőazonosítókhoz rendeli az értékeket.</p>
 */
public class DefaultFormDataBuilderService implements FormDataBuilderService {
    /**
     * Beolvassa a konkrét XML állományt, és az űrlapdefinícióban szereplő mezőkhöz
     * hozzárendeli a dokumentumban megtalált értékeket.
     *
     * <p>Az XML feldolgozása biztonságosan konfigurált, namespace-aware DOM parserrel
     * történik. Az egyszerű sorok mezői közvetlenül, az ismétlődő sorok pedig külön
     * {@link FormRowInstance} példányokként kerülnek a kimeneti modellbe.</p>
     *
     * @param formDefinition a megjelenítendő űrlap szerkezeti és meződefiníciója
     * @param xmlFile a kitöltendő értékeket tartalmazó XML állomány
     * @return a mezőértékeket és ismétlődő sorpéldányokat tartalmazó űrlapadat
     * @throws IllegalStateException ha az XML nem olvasható vagy nem dolgozható fel
     */
    @Override
    public FormData build(FormDefinition formDefinition, Path xmlFile) {
        FormData formData = new FormData();
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
            Document document = factory.newDocumentBuilder().parse(xmlFile.toFile());

            for (FormTabDefinition tab : formDefinition.getTabs()) {
                for (FormSectionDefinition section : tab.getSections()) {
                    for (FormRowDefinition row : section.getRows()) {
                        if (row.isRepeatable() && row.getXmlPath() != null && !row.getXmlPath().isBlank()) {
                            buildRepeatableRowData(formData, document.getDocumentElement(), row);
                        } else {
                            buildSimpleRowData(formData, document.getDocumentElement(), row);
                        }
                    }
                }
            }
            return formData;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build form data from XML: " + xmlFile, e);
        }
    }

    /**
     * Feldolgozza a nem ismétlődő űrlapsor mezőit.
     *
     * <p>Minden mezőhöz az űrlapdefinícióban tárolt XML-útvonal alapján készít
     * {@link FormValue} példányt, majd azt a mező kulcsával a {@link FormData}
     * globális értéktérképébe helyezi.</p>
     *
     * @param formData a feltöltendő űrlapadat-modell
     * @param root az XML dokumentum gyökéreleme
     * @param row a feldolgozandó, nem ismétlődő sor definíciója
     */
    private void buildSimpleRowData(FormData formData, Element root, FormRowDefinition row) {
        for (FormFieldDefinition field : row.getFields()) {
            FormValue value = buildFormValue(field.getId(), field, root, field.getXmlPath());
            formData.getValuesByFieldId().put(value.getKey(), value);
        }
    }

    /**
     * Felépíti egy ismétlődő űrlapsor összes, XML-ben ténylegesen előforduló példányát.
     *
     * <p>A sor {@code xmlPath} értéke alapján először megkeresi az összes megfelelő
     * XML-csomópontot. Minden előforduláshoz külön {@link FormRowInstance} készül,
     * amely saját kanonikus, indexelt XML-útvonalat és saját mezőértékeket kap.</p>
     *
     * <p>A mezők globális kulcsa a sorazonosítót, a nullától induló sorindexet és a
     * mezőazonosítót is tartalmazza, így azonos technikai mezőnév több ismétlődő
     * példányban sem írja felül egymást.</p>
     *
     * @param formData a feltöltendő űrlapadat-modell
     * @param root az XML dokumentum gyökéreleme
     * @param row az ismétlődő sor definíciója
     */
    private void buildRepeatableRowData(FormData formData, Element root, FormRowDefinition row) {
        List<NodeWithPath> rowNodes = findNodesByPath(root, row.getXmlPath());
        int rowIndex = 0;
        for (NodeWithPath rowNode : rowNodes) {
            FormRowInstance instance = new FormRowInstance();
            instance.setId(row.getId() + "#" + (rowIndex + 1));
            instance.setXmlPath(rowNode.path());

            for (FormFieldDefinition field : row.getFields()) {
                String fieldPath = rowNode.path() + "/" + field.getXmlName();
                String key = row.getId() + "#" + rowIndex + ":" + field.getId();
                FormValue value = buildFormValue(key, field, rowNode.node(), fieldPath);
                instance.getValuesByFieldId().put(field.getId(), value);
                formData.getValuesByFieldId().put(value.getKey(), value);
            }

            formData.getOrCreateRowInstances(row.getId()).add(instance);
            rowIndex++;
        }
    }

    /**
     * Létrehozza egy mező konkrét XML-értékét reprezentáló modellt.
     *
     * <p>Elsődlegesen a teljes XML-útvonalat oldja fel. Ha azon nem talál értéket,
     * de a mező XML-neve ismert, kompatibilitási fallbackként a megadott kontextus
     * alatt rekurzívan megkeresi az első ilyen nevű elemet.</p>
     *
     * @param key a {@link FormValue} egyedi kulcsa
     * @param field a mező definíciója
     * @param contextNode az XML-keresés kiinduló csomópontja
     * @param xmlPath a mező teljes, feloldandó XML-útvonala
     * @return a mezőazonosítót, útvonalat, értéket és jelenléti jelzőt tartalmazó modell
     */
    private FormValue buildFormValue(String key, FormFieldDefinition field, Node contextNode, String xmlPath) {
        FormValue value = new FormValue();
        value.setKey(key);
        value.setFieldId(field.getId());
        value.setXmlPath(xmlPath);

        String extracted = extractSimpleValue(contextNode, xmlPath);
        if (extracted == null && field.getXmlName() != null && !field.getXmlName().isBlank()) {
            extracted = extractByLastSegment(contextNode, field.getXmlName());
        }
        value.setValue(extracted);
        value.setPresent(extracted != null);
        return value;
    }

    /**
     * Kiolvassa a pontos XML-útvonalon található elem szöveges értékét.
     *
     * @param root a keresés kiinduló XML-csomópontja
     * @param xmlPath a feloldandó, szükség esetén indexeket is tartalmazó XML-útvonal
     * @return a trimmelt szöveges érték, vagy {@code null}, ha az elem nem található
     *         vagy nincs érdemi szöveges tartalma
     */
    private String extractSimpleValue(Node root, String xmlPath) {
        Node found = findNodeByExactPath(root, xmlPath);
        if (found == null) {
            return null;
        }
        String text = found.getTextContent();
        return text == null || text.isBlank() ? null : text.trim();
    }

    /**
     * Fallbackként az első, megadott lokális nevű XML-elemből olvas ki értéket.
     *
     * <p>Ez a keresés nem teljes útvonal alapján történik, ezért csak akkor használatos,
     * ha a pontos útvonalas feloldás nem adott eredményt.</p>
     *
     * @param root a rekurzív keresés kiinduló csomópontja
     * @param xmlName a keresett XML-elem lokális neve
     * @return a trimmelt szöveges érték, vagy {@code null}, ha nincs találat vagy érték
     */
    private String extractByLastSegment(Node root, String xmlName) {
        Node found = findElementRecursive(root, xmlName);
        if (found == null) return null;
        String text = found.getTextContent();
        return text == null || text.isBlank() ? null : text.trim();
    }

    /**
     * Feloldja a megadott XML-útvonal első találatát.
     *
     * <p>A tényleges útvonal-feldolgozást a {@link #findNodesByPath(Node, String)}
     * végzi; ez a segédmetódus az első csomópontot adja vissza.</p>
     *
     * @param root a keresés kiinduló XML-csomópontja
     * @param path a feloldandó XML-útvonal
     * @return az első illeszkedő csomópont, vagy {@code null}, ha nincs találat
     */
    private Node findNodeByExactPath(Node root, String path) {
        List<NodeWithPath> nodes = findNodesByPath(root, path);
        return nodes.isEmpty() ? null : nodes.get(0).node();
    }

    /**
     * Feloldja a megadott XML-útvonalhoz tartozó elemeket a kiinduló DOM-csomópont alatt.
     *
     * <p>Az útvonal szegmensei opcionálisan egyalapú előfordulási indexet is
     * tartalmazhatnak, például {@code Form[2]/Block[1]}. Index nélküli szegmensnél
     * az adott nevű összes közvetlen gyermek továbbhalad a következő szintre.</p>
     *
     * <p>Minden találathoz a tényleges, kanonikus, előfordulási indexekkel kiegészített
     * XML-útvonal is megmarad. Ez különbözteti meg az azonos nevű ismétlődő elemeket.</p>
     *
     * @param root a keresés kiinduló DOM-csomópontja
     * @param path a feloldandó XML-útvonal
     * @return a megtalált csomópontok és kanonikus útvonalaik; üres lista, ha az
     *         útvonal nem oldható fel
     */
    private List<NodeWithPath> findNodesByPath(Node root, String path) {
        List<NodeWithPath> current = new ArrayList<>();
        if (root == null || path == null || path.isBlank()) {
            return current;
        }

        String normalized = path.startsWith("/") ? path.substring(1) : path;
        String[] parts = normalized.split("/");
        current.add(new NodeWithPath(root, "/" + resolveNodeName(root)));

        int startIndex = 0;
        if (parts.length > 0 && localNameEquals(root, parts[0])) {
            startIndex = 1;
        }

        for (int i = startIndex; i < parts.length; i++) {
            PathSegment segment = parseSegment(parts[i]);
            List<NodeWithPath> nextLevel = new ArrayList<>();
            for (NodeWithPath candidate : current) {
                List<NodeWithPath> children = findChildElements(candidate, segment.name());
                if (segment.index() != null) {
                    int childIndex = segment.index() - 1;
                    if (childIndex >= 0 && childIndex < children.size()) {
                        nextLevel.add(children.get(childIndex));
                    }
                } else {
                    nextLevel.addAll(children);
                }
            }
            current = nextLevel;
            if (current.isEmpty()) {
                return current;
            }
        }

        return current;
    }

    /**
     * Megkeresi a szülő közvetlen, adott lokális nevű gyermekelemeit.
     *
     * <p>A találatokat azonos nevű testvéreken belül egytől induló előfordulási
     * indexszel egészíti ki, és ebből építi tovább a kanonikus XML-útvonalat.</p>
     *
     * @param parent a szülő csomópont és annak már felépített útvonala
     * @param localName a keresett gyermekelem lokális neve
     * @return a megfelelő közvetlen gyermekelemek és indexelt útvonalaik
     */
    private List<NodeWithPath> findChildElements(NodeWithPath parent, String localName) {
        List<NodeWithPath> result = new ArrayList<>();
        Node parentNode = parent.node();
        for (int i = 0; i < parentNode.getChildNodes().getLength(); i++) {
            Node child = parentNode.getChildNodes().item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && localNameEquals(child, localName)) {
                String childPath = parent.path() + "/" + resolveNodeName(child) + "[" + (result.size() + 1) + "]";
                result.add(new NodeWithPath(child, childPath));
            }
        }
        return result;
    }

    /**
     * Mélységi kereséssel megkeresi az első, adott lokális nevű XML-elemet.
     *
     * @param parent a keresés aktuális kiinduló csomópontja
     * @param localName a keresett elem lokális neve
     * @return az első illeszkedő elem, vagy {@code null}, ha a részfában nincs találat
     */
    private Node findElementRecursive(Node parent, String localName) {
        if (parent == null) return null;
        if (parent.getNodeType() == Node.ELEMENT_NODE && localNameEquals(parent, localName)) {
            return parent;
        }
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            Node child = parent.getChildNodes().item(i);
            Node found = findElementRecursive(child, localName);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Namespace-aware módon összehasonlítja egy DOM-csomópont nevét a várt lokális névvel.
     *
     * <p>Ha a DOM biztosít lokális nevet, azt használja; ellenkező esetben a teljes
     * {@code nodeName} értékre esik vissza.</p>
     *
     * @param node a vizsgált DOM-csomópont
     * @param expected a várt elemnév
     * @return {@code true}, ha a csomópont neve megegyezik a várt névvel
     */
    private boolean localNameEquals(Node node, String expected) {
        String local = node.getLocalName();
        String name = local != null ? local : node.getNodeName();
        return expected.equals(name);
    }

    /**
     * Meghatározza a DOM-csomópont útvonalépítéshez használt nevét.
     *
     * @param node a vizsgált DOM-csomópont
     * @return a lokális név, vagy annak hiányában a DOM {@code nodeName} értéke
     */
    private String resolveNodeName(Node node) {
        String local = node.getLocalName();
        return local != null ? local : node.getNodeName();
    }

    /**
     * Feldolgozza az XML-útvonal egyetlen szegmensét névre és opcionális indexre.
     *
     * <p>A {@code Field[3]} alakból {@code Field} nevet és {@code 3} indexet készít;
     * index nélküli szegmensnél az index {@code null}. A {@code null} bemenet üres
     * névként kerül reprezentálásra.</p>
     *
     * @param segment a feldolgozandó útvonalszegmens
     * @return a szegmens neve és opcionális egyalapú előfordulási indexe
     */
    private PathSegment parseSegment(String segment) {
        if (segment == null) {
            return new PathSegment("", null);
        }
        int bracket = segment.indexOf('[');
        int closing = segment.endsWith("]") ? segment.length() - 1 : -1;
        if (bracket > 0 && closing > bracket) {
            String name = segment.substring(0, bracket);
            Integer index = Integer.parseInt(segment.substring(bracket + 1, closing));
            return new PathSegment(name, index);
        }
        return new PathSegment(segment, null);
    }

    /**
     * Egy feloldott DOM-csomópontot és annak kanonikus, indexelt XML-útvonalát fogja össze.
     *
     * @param node a feloldott DOM-csomópont
     * @param path a csomóponthoz tartozó kanonikus XML-útvonal
     */
    private record NodeWithPath(Node node, String path) {}
    /**
     * Egy XML-útvonalszegmens felbontott reprezentációja.
     *
     * @param name az elem lokális neve
     * @param index az opcionális, egytől induló előfordulási index
     */
    private record PathSegment(String name, Integer index) {}
}
