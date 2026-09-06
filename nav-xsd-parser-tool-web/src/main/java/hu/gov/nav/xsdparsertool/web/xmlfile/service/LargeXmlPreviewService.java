package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.core.xml.SecureXmlParserSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.namespace.QName;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Nagy XML dokumentumból kis memóriaigényű megnyitási előnézetet készít.
 *
 * <p>A szolgáltatás StAX eseményfolyammal dolgozik, ezért az eredeti dokumentumot
 * nem tölti teljes egészében memóriába. A gyökérelemet, az első közvetlen főlapot és az első eltérő nevű
 * {@code Form_*} melléklapot másolja át egy ideiglenes, jól formált XML fájlba. A
 * meglévő XSD/UIModel/form pipeline ezen a kis részleten biztonságosan lefuthat.</p>
 *
 * <p>Ez a megnyitási előnézet nem helyettesíti a teljes dokumentum validációját.
 * Célja, hogy a főlap megjeleníthető legyen anélkül, hogy a több száz megabájtos
 * XML-hez DOM, teljes FormData vagy teljes XML-fa épülne.</p>
 *
 * @since 1.0
 */
@Service
public class LargeXmlPreviewService {
    private static final Logger log = LoggerFactory.getLogger(LargeXmlPreviewService.class);

    /**
     * Elkészíti a nagy XML főlapját és első melléklapját tartalmazó előnézetet.
     *
     * @param source az eredeti nagy XML fájl
     * @return automatikusan törlendő ideiglenes előnézeti fájl
     * @throws IOException ha az XML nem olvasható vagy nem található benne közvetlen Form elem
     */
    public PreviewResult createMainFormPreview(Path source) throws IOException {
        if (source == null || !ExceptionSafeOperations.isRegularFile(source)) {
            throw new IllegalArgumentException("A nagy XML előnézet forrásfájlja nem létezik: " + source);
        }

        Path preview = SecureFileOperations.createPrivateTempFile("nav-xsd-large-preview-", ".xml");
        boolean success = false;
        String selectedForm = null;
        String selectedSecondaryForm = null;
        long secondaryFormCount = 0L;

        XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        SecureXmlParserSupport.configureSecureXmlInputFactory(inputFactory);
        XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();

        try (InputStream input = new BufferedInputStream(Files.newInputStream(source), 1024 * 1024);
             OutputStream output = new BufferedOutputStream(SecureFileOperations.newPrivateOutputStream(preview), 1024 * 1024)) {
            XMLEventReader reader = inputFactory.createXMLEventReader(input);
            XMLEventWriter writer = outputFactory.createXMLEventWriter(output, "UTF-8");
            int depth = 0;
            boolean rootWritten = false;
            QName rootName = null;
            boolean copyingForm = false;
            int formDepth = -1;
            boolean mainFormCopied = false;

            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();

                if (event.isStartDocument()) {
                    writer.add(javax.xml.stream.XMLEventFactory.newFactory().createStartDocument("UTF-8", "1.0"));
                    continue;
                }
                if (event.isStartElement()) {
                    StartElement start = event.asStartElement();
                    depth++;
                    if (depth == 1) {
                        writer.add(event);
                        rootWritten = true;
                        rootName = start.getName();
                        continue;
                    }
                    String localName = start.getName().getLocalPart();
                    if (!copyingForm && depth == 2 && localName.startsWith("Form_")) {
                        if (!mainFormCopied) {
                            copyingForm = true;
                            formDepth = depth;
                            selectedForm = localName;
                        } else if (!localName.equals(selectedForm)) {
                            if (selectedSecondaryForm == null) {
                                selectedSecondaryForm = localName;
                            }
                            if (localName.equals(selectedSecondaryForm)) {
                                secondaryFormCount++;
                                if (secondaryFormCount == 1L) {
                                    copyingForm = true;
                                    formDepth = depth;
                                }
                            }
                        }
                    }
                    if (copyingForm) {
                        writer.add(event);
                    }
                    continue;
                }
                if (event.isEndElement()) {
                    EndElement end = event.asEndElement();
                    if (copyingForm) {
                        writer.add(event);
                        if (depth == formDepth) {
                            copyingForm = false;
                            if (!mainFormCopied) {
                                mainFormCopied = true;
                            }
                        }
                    }
                    depth--;
                    continue;
                }
                if (copyingForm) {
                    writer.add(event);
                }
            }

            if (!rootWritten || selectedForm == null) {
                throw new IOException("A nagy XML-ben nem található közvetlen Form_* dokumentumrész.");
            }
            // A kiválasztott form után lezárjuk az eredeti gyökérelemet és a dokumentumot.
            writer.add(javax.xml.stream.XMLEventFactory.newFactory().createEndElement(rootName, null));
            writer.add(javax.xml.stream.XMLEventFactory.newFactory().createEndDocument());
            writer.flush();
            writer.close();
            reader.close();
            success = true;
            log.info("Nagy XML megnyitási előnézet elkészült. source={}, preview={}, selectedForm={}, selectedSecondaryForm={}, secondaryFormCount={}, previewBytes={}",
                    source, preview, selectedForm, selectedSecondaryForm, secondaryFormCount, Files.size(preview));
            return new PreviewResult(preview, selectedForm, selectedSecondaryForm, secondaryFormCount);
        } catch (Exception ex) {
            if (ex instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("A nagy XML megnyitási előnézete nem készíthető el: " + ex.getMessage(), ex);
        } finally {
            if (!success) {
                Files.deleteIfExists(preview);
            }
        }
    }

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code PreviewResult} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public record PreviewResult(Path previewPath, String mainFormName, String repeatingFormName, long repeatingFormCount) {}
}
