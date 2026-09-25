package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Biztonságosan beolvassa a catalog repository content/artifact-catalog.xml állományát. */
@Component
public class ArtifactCatalogParser {

    public ArtifactCatalog parse(String xml) {
        if (!StringUtils.hasText(xml)) throw new IllegalArgumentException("Az artifact-catalog.xml üres.");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();
            if (root == null || !"artifactCatalog".equals(root.getLocalName())) {
                throw new IllegalArgumentException("Az XML gyökéreleme nem artifactCatalog.");
            }
            String schemaVersion = firstText(root, "catalogSchemaVersion");
            Instant generatedAt = parseInstant(firstText(root, "generatedAt"));
            List<FormEntry> forms = new ArrayList<>();
            NodeList formNodes = root.getElementsByTagNameNS("*", "form");
            for (int i = 0; i < formNodes.getLength(); i++) {
                Element form = (Element) formNodes.item(i);
                String formId = directChildText(form, "formId");
                String formName = directChildText(form, "formName");
                if (!StringUtils.hasText(formId)) continue;
                List<VersionEntry> versions = new ArrayList<>();
                NodeList versionNodes = form.getElementsByTagNameNS("*", "version");
                for (int j = 0; j < versionNodes.getLength(); j++) {
                    Element version = (Element) versionNodes.item(j);
                    String formVersion = directChildText(version, "formVersion");
                    if (!StringUtils.hasText(formVersion)) continue;
                    versions.add(new VersionEntry(formVersion.trim(),
                            parseInstant(directChildText(version, "validFrom")),
                            parseInstant(directChildText(version, "validTo")),
                            Boolean.parseBoolean(directChildText(version, "disabled"))));
                }
                forms.add(new FormEntry(formId.trim(), formName, List.copyOf(versions)));
            }
            return new ArtifactCatalog(schemaVersion, generatedAt, List.copyOf(forms));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Az artifact-catalog.xml nem dolgozható fel: " + ex.getMessage(), ex);
        }
    }

    private String firstText(Element root, String localName) {
        NodeList nodes = root.getElementsByTagNameNS("*", localName);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private String directChildText(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element && localName.equals(element.getLocalName())) {
                return element.getTextContent() == null ? "" : element.getTextContent().trim();
            }
        }
        return "";
    }

    private Instant parseInstant(String value) {
        if (!StringUtils.hasText(value)) return null;
        try { return OffsetDateTime.parse(value.trim()).toInstant(); }
        catch (Exception ex) { return Instant.parse(value.trim()); }
    }

    public record ArtifactCatalog(String schemaVersion, Instant generatedAt, List<FormEntry> forms) {}
    public record FormEntry(String formId, String formName, List<VersionEntry> versions) {}
    public record VersionEntry(String formVersion, Instant validFrom, Instant validTo, boolean disabled) {}
}
