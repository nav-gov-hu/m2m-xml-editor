package hu.gov.nav.xsdparsertool.multiform;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Finds and classifies XML files in an input ZIP using their root QName, never their filename. */
public final class ZipPackageInspector {

    public PackageInventory inspect(Path zip, MultiformDescriptor descriptor) {
        List<PackageEntry> mains = new ArrayList<>();
        List<PackageEntry> repeating = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().toLowerCase(Locale.ROOT).endsWith(".xml")) {
                    continue;
                }
                ensureSafeName(entry.getName());
                try (InputStream in = zipFile.getInputStream(entry)) {
                    QName root = readRoot(in);
                    PartKind kind = classify(root, descriptor);
                    PackageEntry item = new PackageEntry(entry.getName(), root, kind);
                    if (kind == PartKind.MAIN) {
                        mains.add(item);
                    } else {
                        repeating.add(item);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("A ZIP nem olvasható: " + zip, e);
        }

        if (mains.size() != 1) {
            throw new IllegalArgumentException("A ZIP pontosan egy főlap XML-t kell tartalmazzon; talált: " + mains.size());
        }
        if (repeating.size() < descriptor.repeatingPart().minOccurs()) {
            throw new IllegalArgumentException("A ZIP nem tartalmaz elegendő melléklapot; minimum: "
                    + descriptor.repeatingPart().minOccurs() + ", talált: " + repeating.size());
        }
        repeating.sort(Comparator.comparing(PackageEntry::zipEntryName));
        return new PackageInventory(mains.get(0), repeating);
    }

    private QName readRoot(InputStream in) throws Exception {
        XMLStreamReader reader = XmlSecurity.inputFactory().createXMLStreamReader(in);
        try {
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                    return reader.getName();
                }
            }
            throw new IllegalArgumentException("Az XML nem tartalmaz gyökérelemet.");
        } finally {
            reader.close();
        }
    }

    private PartKind classify(QName root, MultiformDescriptor descriptor) {
        if (same(root, descriptor.mainPart().elementName())) {
            return PartKind.MAIN;
        }
        if (same(root, descriptor.repeatingPart().elementName())) {
            return PartKind.REPEATING;
        }
        throw new IllegalArgumentException("Ismeretlen XML gyökérelem a ZIP-ben: " + root
                + "; várt: " + descriptor.mainPart().elementName() + " vagy " + descriptor.repeatingPart().elementName());
    }

    private boolean same(QName actual, QName expected) {
        return actual.getLocalPart().equals(expected.getLocalPart())
                && normalize(actual.getNamespaceURI()).equals(normalize(expected.getNamespaceURI()));
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }

    private void ensureSafeName(String name) {
        if (name.startsWith("/") || name.startsWith("\\") || name.contains("../") || name.contains("..\\")) {
            throw new IllegalArgumentException("Tiltott ZIP bejegyzésnév: " + name);
        }
    }
}
