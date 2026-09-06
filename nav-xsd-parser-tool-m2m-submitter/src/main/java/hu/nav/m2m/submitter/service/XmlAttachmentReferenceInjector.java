package hu.nav.m2m.submitter.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.nav.m2m.submitter.service.nav.NavGateway;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A NAV fileId és fájlmetaadatokat a megfelelő XML csatolmánycsomópontokba írja vissza, fájlnév és konkrét XML-struktúra alapján.
 */
@Service
public class XmlAttachmentReferenceInjector {

    /**
     * Bizonylat API csatolmányos beküldéshez a feltöltött állományok NAV fileId/fileName/fileSize
     * adatait írja be az XML-be.
     *
     * Fontos: ez a metódus űrlapfüggetlen. Nem feltételez konkrét Form_* elemnevet,
     * hanem a tényleges XML-struktúrából választja ki a csatolmányokat tartalmazó űrlapelemet.
     * DOM alapján módosítunk, ezért a kimenet minden esetben jól formált XML.
     *
     * Szabályok:
     * - a már kitöltött fileId értékeket nem írjuk felül;
     * - ha van azonos fileName értékű Attachment_1 üres fileId-vel, csak azt töltjük;
     * - ha az adott feltöltött fájlhoz még nincs Attachment_1 node, új node-ot szúrunk be;
     * - ha minden érintett Attachment_1 már ki van töltve, a forrás XML-t változatlanul adjuk vissza.
     */
    public Path injectAttachmentReferences(Path sourceXml,
                                                List<UploadedAttachmentForXml> attachments) throws IOException {
        if (attachments == null || attachments.isEmpty()) {
            return sourceXml;
        }

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

            Document document = factory.newDocumentBuilder().parse(sourceXml.toFile());
            Element form = findAttachmentTargetForm(document);

            boolean changed = false;
            for (UploadedAttachmentForXml attachment : attachments) {
                if (attachment == null || attachment.uploadedFile() == null || isBlank(attachment.uploadedFile().fileId())) {
                    continue;
                }
                changed |= applySingleAttachment(document, form, attachment);
            }

            if (!changed) {
                return sourceXml;
            }

            Path target = sourceXml.resolveSibling(sourceXml.getFileName().toString().replaceFirst("\\.xml$", "") + "_with_attachments.xml");
            writeXml(document, target);
            return target;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Attachment_1 referenciák XML-be írása sikertelen: " + ex.getMessage(), ex);
        }
    }

    /**
     * Az M2M életciklus vagy feldolgozási eredmény alapján frissíti a kezelt domain/runtime állapotot; a változás a hívó tranzakciójának része lehet.
     *
     * @param document a feldolgozott DOM dokumentum
     * @param form a csatolmányt tartalmazó űrlap DOM eleme
     * @param attachment az aktuális csatolmány vagy csatolmányadat
     * @return a művelet eredménye
     */
    private boolean applySingleAttachment(Document document, Element form, UploadedAttachmentForXml attachment) {
        List<Element> nodes = directChildElementsByLocalName(form, "Attachment_1");
        String requestedNameKey = normalizeFileName(attachment.fileName());

        for (Element node : nodes) {
            String xmlNameKey = normalizeFileName(textOfDirectChild(node, "fileName"));
            if (!requestedNameKey.equals(xmlNameKey)) {
                continue;
            }
            Element fileId = directChild(node, "fileId");
            if (fileId == null) {
                fileId = createElementInParentNamespace(document, node, "fileId");
                node.insertBefore(fileId, node.getFirstChild());
            }
            if (isBlank(fileId.getTextContent())) {
                fileId.setTextContent(attachment.uploadedFile().fileId());
                ensureChildText(document, node, "fileName", attachment.fileName());
                ensureChildText(document, node, "fileSize", String.valueOf(attachment.fileSize()));
                return true;
            }
            // A node már ki van töltve. Nem írjuk felül és nem hozunk létre duplikált node-ot.
            return false;
        }

        Element created = createElementInParentNamespace(document, form, "Attachment_1");
        appendTextElement(document, created, "fileId", attachment.uploadedFile().fileId());
        appendTextElement(document, created, "fileName", attachment.fileName());
        appendTextElement(document, created, "fileSize", String.valueOf(attachment.fileSize()));
        form.appendChild(document.createTextNode("\n   "));
        form.appendChild(created);
        form.appendChild(document.createTextNode("\n"));
        return true;
    }


    /**
     * A tényleges XML alapján választja ki azt a Form_* elemet, amelybe az Attachment_1
     * hivatkozások írhatók. Elsőbbséget élvez az a form, amely már tartalmaz közvetlen
     * Attachment_1 gyermeket. Ha ilyen nincs, pontosan egy Form_* elem esetén az kerül kiválasztásra.
     * Több lehetséges form esetén nem találgatunk, hanem egyértelmű hibát adunk.
     */
    private static Element findAttachmentTargetForm(Document document) throws IOException {
        Element root = document.getDocumentElement();
        if (root == null) {
            throw new IOException("Az XML dokumentumnak nincs gyökéreleme, ezért az Attachment_1 referenciák nem szúrhatók be.");
        }

        List<Element> formCandidates = new ArrayList<>();
        if (isFormElement(root)) {
            formCandidates.add(root);
        }
        collectFormElements(root, formCandidates);

        List<Element> formsWithAttachment = formCandidates.stream()
                .filter(form -> !directChildElementsByLocalName(form, "Attachment_1").isEmpty())
                .toList();

        if (formsWithAttachment.size() == 1) {
            return formsWithAttachment.get(0);
        }
        if (formsWithAttachment.size() > 1) {
            throw new IOException("Több Form_* elem tartalmaz Attachment_1 csomópontot ("
                    + joinElementNames(formsWithAttachment)
                    + "), ezért a csatolmány célhelye nem határozható meg egyértelműen.");
        }
        if (formCandidates.size() == 1) {
            return formCandidates.get(0);
        }
        if (formCandidates.isEmpty()) {
            throw new IOException("Nem található Form_* elem, ezért az Attachment_1 referenciák nem szúrhatók be automatikusan.");
        }
        throw new IOException("Több Form_* elem található (" + joinElementNames(formCandidates)
                + "), de egyik sem tartalmaz Attachment_1 csomópontot; a célhely nem határozható meg egyértelműen.");
    }

    /**
     * A(z) {@code collectFormElements} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param parent a vizsgált szülő DOM elem
     * @param result az épülő eredménykollekció
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private static void collectFormElements(Element parent, List<Element> result) throws IOException {
        collectFormElements(parent, result, 0, new int[]{0});
    }

    /**
     * A(z) {@code collectFormElements} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param parent a vizsgált szülő DOM elem
     * @param result az épülő eredménykollekció
     * @param depth az aktuális rekurziós mélység
     * @param visited a bejárt XML elemek számát nyilvántartó számláló
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private static void collectFormElements(Element parent, List<Element> result, int depth, int[] visited) throws IOException {
        if (depth > 128 || visited[0] > 100_000) {
            throw new IOException("Az XML szerkezete túl nagy a csatolmányhivatkozások biztonságos feldolgozásához.");
        }
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            visited[0]++;
            if (!(node instanceof Element element)) continue;
            if (isFormElement(element)) result.add(element);
            collectFormElements(element, result, depth + 1, visited);
        }
    }

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param element a művelethez átadott {@code element} érték
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private static boolean isFormElement(Element element) {
        String name = localName(element);
        return name != null && name.startsWith("Form_");
    }

    /**
     * A(z) {@code joinElementNames} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param elements a művelethez átadott {@code elements} érték
     * @return a művelet eredménye
     */
    private static String joinElementNames(List<Element> elements) {
        return elements.stream().map(XmlAttachmentReferenceInjector::localName).distinct().sorted().reduce((a, b) -> a + ", " + b).orElse("");
    }

    /**
     * Előkészíti vagy létrehozza az adott NAV M2M művelethez szükséges adatot, majd a következő feldolgozási lépésnek adja tovább.
     *
     * @param document a feldolgozott DOM dokumentum
     * @param parent a vizsgált szülő DOM elem
     * @param localName a namespace-független XML elemnév
     * @return a művelet eredménye
     */
    private static Element createElementInParentNamespace(Document document, Element parent, String localName) {
        String namespaceUri = parent.getNamespaceURI();
        String prefix = parent.getPrefix();
        if (namespaceUri == null || namespaceUri.isBlank()) {
            return document.createElement(localName);
        }
        String qualifiedName = prefix == null || prefix.isBlank() ? localName : prefix + ":" + localName;
        return document.createElementNS(namespaceUri, qualifiedName);
    }

    /**
     * A(z) {@code directChildElementsByLocalName} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param parent a vizsgált szülő DOM elem
     * @param localName a namespace-független XML elemnév
     * @return a művelet eredménye
     */
    private static List<Element> directChildElementsByLocalName(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element && localName.equals(localName(element))) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * A(z) {@code directChild} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param parent a vizsgált szülő DOM elem
     * @param localName a namespace-független XML elemnév
     * @return a művelet eredménye
     */
    private static Element directChild(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element && localName.equals(localName(element))) {
                return element;
            }
        }
        return null;
    }

    /**
     * A(z) {@code textOfDirectChild} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param parent a vizsgált szülő DOM elem
     * @param localName a namespace-független XML elemnév
     * @return a művelet eredménye
     */
    private static String textOfDirectChild(Element parent, String localName) {
        Element child = directChild(parent, localName);
        return child == null ? "" : trimToEmpty(child.getTextContent());
    }

    /**
     * Ellenőrzi a művelet kötelező előfeltételeit és inkonzisztens vagy nem engedélyezett állapot esetén kontrollált kivétellel megszakítja a feldolgozást.
     *
     * @param document a feldolgozott DOM dokumentum
     * @param parent a vizsgált szülő DOM elem
     * @param localName a namespace-független XML elemnév
     * @param value a feldolgozandó érték
     */
    private static void ensureChildText(Document document, Element parent, String localName, String value) {
        Element child = directChild(parent, localName);
        if (child == null) {
            appendTextElement(document, parent, localName, value);
        } else if (isBlank(child.getTextContent())) {
            child.setTextContent(value == null ? "" : value);
        }
    }

    /**
     * A(z) {@code appendTextElement} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param document a feldolgozott DOM dokumentum
     * @param parent a vizsgált szülő DOM elem
     * @param localName a namespace-független XML elemnév
     * @param value a feldolgozandó érték
     */
    private static void appendTextElement(Document document, Element parent, String localName, String value) {
        Element child = createElementInParentNamespace(document, parent, localName);
        child.setTextContent(value == null ? "" : value);
        parent.appendChild(child);
    }

    /**
     * Namespace-től független helyi XML elemnevet ad vissza.
     *
     * @param node a művelethez átadott {@code node} érték
     * @return a művelet eredménye
     */
    private static String localName(Node node) {
        String local = node.getLocalName();
        if (local != null && !local.isBlank()) {
            return local;
        }
        String name = node.getNodeName();
        int colon = name == null ? -1 : name.indexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : name;
    }

    /**
     * A(z) {@code normalizeFileName} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private static String normalizeFileName(String value) {
        return Normalizer.normalize(trimToEmpty(value), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    /**
     * A(z) {@code trimToEmpty} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * A jelenlegi állapot és az M2M életciklusszabályok alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param value a feldolgozandó érték
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * A(z) {@code writeXml} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param document a feldolgozott DOM dokumentum
     * @param target a létrehozandó célfájl
     * @throws Exception ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private static void writeXml(Document document, Path target) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        // Keep the XXE/XSLT restrictions directly next to the transform sink. Besides being
        // easier to audit, this ensures static analyzers can prove that the transformer
        // cannot resolve external DTDs, stylesheets or URI resources.
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        transformerFactory.setURIResolver((href, base) -> {
            throw new TransformerException("Külső XML/XSLT erőforrás feloldása tiltott.");
        });
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        try (OutputStream out = SecureFileOperations.newPrivateOutputStream(target)) {
            transformer.transform(new DOMSource(document), new StreamResult(out));
        }
    }

    /**
     * A NAV M2M submitter modul {@code UploadedAttachmentForXml} típusának felelősségét megvalósító típus.
     */
    /**
     * Létrehozza a(z) {@code UploadedAttachmentForXml} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param fileName a művelethez átadott {@code fileName} érték
     * @param fileSize a művelethez átadott {@code fileSize} érték
     * @param uploadedFile a művelethez átadott {@code uploadedFile} érték
     */
    public record UploadedAttachmentForXml(String fileName, long fileSize, NavGateway.UploadedFile uploadedFile) {}
}
