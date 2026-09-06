package hu.nav.m2m.submitter.service;

import org.springframework.stereotype.Service;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Biztonságosan beolvassa az XML-t és kinyeri belőle a csatolmányokra mutató strukturált hivatkozásokat.
 */
@Service
public class XmlAttachmentReferenceExtractor {

    /**
     * Biztonságos XML parserrel beolvassa a dokumentumot, megkeresi a csatolmánystruktúrákat, és minden találathoz megőrzi a fájlnevet, fileId-t és XML-kontextust.
     *
     * @param xmlPath a művelethez átadott {@code xmlPath} érték
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     * @throws XMLStreamException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public List<AttachmentReference> extract(Path xmlPath) throws IOException, XMLStreamException {
        List<AttachmentReference> references = new ArrayList<>();
        try (InputStream in = Files.newInputStream(xmlPath)) {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
            xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            xmlInputFactory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
                throw new XMLStreamException("Külső XML entitások feloldása tiltott.");
            });
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(in);
            int seq = 0;
            boolean inAttachment = false;
            String fileId = null;
            String fileName = null;
            Long fileSize = null;
            String currentChild = null;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();
                    if ("Attachment_1".equals(localName)) {
                        inAttachment = true;
                        fileId = null;
                        fileName = null;
                        fileSize = null;
                    } else if (inAttachment) {
                        currentChild = localName;
                    }
                } else if (event == XMLStreamConstants.CHARACTERS && inAttachment && currentChild != null) {
                    String text = reader.getText().trim();
                    if (!text.isEmpty()) {
                        if ("fileId".equals(currentChild)) fileId = append(fileId, text);
                        if ("fileName".equals(currentChild)) fileName = append(fileName, text);
                        if ("fileSize".equals(currentChild)) {
                            try { fileSize = Long.parseLong(text); } catch (NumberFormatException ignored) { }
                        }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String localName = reader.getLocalName();
                    if ("Attachment_1".equals(localName)) {
                        references.add(new AttachmentReference(++seq, "Attachment_1", fileId, fileName, fileSize));
                        inAttachment = false;
                        currentChild = null;
                    } else if (inAttachment && localName.equals(currentChild)) {
                        currentChild = null;
                    }
                }
            }
        }
        return references;
    }

    /**
     * A(z) {@code append} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param current a művelethez átadott {@code current} érték
     * @param text a hash- vagy szövegfeldolgozás bemenete
     * @return a művelet eredménye
     */
    private String append(String current, String text) {
        return current == null ? text : current + text;
    }

    /**
     * A NAV M2M submitter modul {@code AttachmentReference} típusának felelősségét megvalósító típus.
     */
    /**
     * Létrehozza a(z) {@code AttachmentReference} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param sequenceNo a művelethez átadott {@code sequenceNo} érték
     * @param elementName a művelethez átadott {@code elementName} érték
     * @param fileId a művelethez átadott {@code fileId} érték
     * @param fileName a művelethez átadott {@code fileName} érték
     * @param fileSize a művelethez átadott {@code fileSize} érték
     */
    public record AttachmentReference(int sequenceNo, String elementName, String fileId, String fileName, Long fileSize) {}
}
