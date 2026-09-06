package hu.gov.nav.xsdparsertool.multiform;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipFile;

/** Creates the final document by streaming each standalone part from the ZIP; no full DOM is built. */
public final class StreamingAssembler {

    public Path assemble(Path zip, Path output, MultiformDescriptor descriptor, PackageInventory inventory) {
        Path absoluteOutput = output.toAbsolutePath().normalize();
        Path temp = absoluteOutput.resolveSibling(absoluteOutput.getFileName() + ".tmp");
        try {
            Path parent = absoluteOutput.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ZipFile zipFile = new ZipFile(zip.toFile());
                 OutputStream out = Files.newOutputStream(temp)) {
                XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(out, "UTF-8");
                writer.writeStartDocument("UTF-8", "1.0");
                String ns = descriptor.targetNamespace();
                writer.setPrefix("tns", ns);
                writer.writeStartElement("tns", descriptor.documentElement().getLocalPart(), ns);
                writer.writeNamespace("tns", ns);

                copyEntry(zipFile, inventory.mainEntry().zipEntryName(), writer);
                for (PackageEntry entry : inventory.repeatingEntries()) {
                    copyEntry(zipFile, entry.zipEntryName(), writer);
                }

                writer.writeEndElement();
                writer.writeEndDocument();
                writer.close();
            }
            try {
                return Files.move(temp, absoluteOutput, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                return Files.move(temp, absoluteOutput, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            try {
                Files.deleteIfExists(temp);
            } catch (Exception ignored) {
                // Preserve the original assembly exception.
            }
            throw new IllegalArgumentException("A teljes multiform XML nem állítható elő.", e);
        }
    }

    private void copyEntry(ZipFile zipFile, String entryName, XMLStreamWriter writer) throws Exception {
        var zipEntry = zipFile.getEntry(entryName);
        if (zipEntry == null) {
            throw new IllegalArgumentException("Hiányzó ZIP bejegyzés: " + entryName);
        }
        try (InputStream in = zipFile.getInputStream(zipEntry)) {
            XMLStreamReader reader = XmlSecurity.inputFactory().createXMLStreamReader(in);
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT -> writeStart(reader, writer);
                        case XMLStreamConstants.END_ELEMENT -> writer.writeEndElement();
                        case XMLStreamConstants.CHARACTERS, XMLStreamConstants.SPACE -> writer.writeCharacters(reader.getText());
                        case XMLStreamConstants.CDATA -> writer.writeCData(reader.getText());
                        case XMLStreamConstants.COMMENT -> writer.writeComment(reader.getText());
                        case XMLStreamConstants.PROCESSING_INSTRUCTION -> writer.writeProcessingInstruction(
                                reader.getPITarget(), reader.getPIData() == null ? "" : reader.getPIData());
                        default -> {
                            // XML declaration, DTD and document boundaries are intentionally not copied.
                        }
                    }
                }
            } finally {
                reader.close();
            }
        }
    }

    private void writeStart(XMLStreamReader reader, XMLStreamWriter writer) throws Exception {
        String ns = safe(reader.getNamespaceURI());
        String prefix = safe(reader.getPrefix());
        if (ns.isEmpty()) {
            writer.writeStartElement(reader.getLocalName());
        } else {
            writer.writeStartElement(prefix, reader.getLocalName(), ns);
        }
        for (int i = 0; i < reader.getNamespaceCount(); i++) {
            String nsPrefix = reader.getNamespacePrefix(i);
            String nsUri = safe(reader.getNamespaceURI(i));
            if (nsPrefix == null || nsPrefix.isEmpty()) {
                writer.writeDefaultNamespace(nsUri);
            } else {
                writer.writeNamespace(nsPrefix, nsUri);
            }
        }
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attrNs = safe(reader.getAttributeNamespace(i));
            String attrPrefix = safe(reader.getAttributePrefix(i));
            if (attrNs.isEmpty()) {
                writer.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            } else {
                writer.writeAttribute(attrPrefix, attrNs, reader.getAttributeLocalName(i), reader.getAttributeValue(i));
            }
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
