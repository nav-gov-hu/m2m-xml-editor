package hu.gov.nav.xsdparsertool.multiform;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipFile;

/** Public facade for standalone Java use. */
public final class MultiformService {
    private final Path xsd;
    private final MultiformDescriptor descriptor;
    private final PartValidator partValidator;
    private final FullDocumentValidator fullValidator;

    public MultiformService(Path xsd) {
        this.xsd = xsd.toAbsolutePath().normalize();
        this.descriptor = new MultiformSchemaAnalyzer().analyze(this.xsd);
        this.partValidator = new PartValidator(this.xsd, descriptor);
        this.fullValidator = new FullDocumentValidator(this.xsd);
    }

    public MultiformDescriptor descriptor() {
        return descriptor;
    }

    public String adapterXsd(PartKind kind) {
        return new AdapterSchemaGenerator().generate(xsd, descriptor, kind);
    }

    public ValidationResult validatePart(Path xml, PartKind kind) {
        return partValidator.validate(xml, kind);
    }

    public ValidationResult validateDocument(Path xml) {
        return fullValidator.validate(xml);
    }

    /** Validates every part in the ZIP. Keys are ZIP entry names. */
    public Map<String, ValidationResult> validatePackage(Path zip) {
        PackageInventory inventory = new ZipPackageInspector().inspect(zip, descriptor);
        Map<String, ValidationResult> results = new LinkedHashMap<>();
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            validateZipEntry(zipFile, inventory.mainEntry(), results);
            for (PackageEntry entry : inventory.repeatingEntries()) {
                validateZipEntry(zipFile, entry, results);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("A ZIP részbizonylatai nem validálhatók.", e);
        }
        return results;
    }

    /** Validates every part, streams the final XML, then validates the final XML with the original XSD. */
    public Path merge(Path zip, Path output) {
        PackageInventory inventory = new ZipPackageInspector().inspect(zip, descriptor);
        Map<String, ValidationResult> partResults = validatePackage(zip);
        var invalid = partResults.entrySet().stream().filter(e -> !e.getValue().valid()).findFirst();
        if (invalid.isPresent()) {
            throw new IllegalArgumentException("Hibás részbizonylat: " + invalid.get().getKey() + " -> " + invalid.get().getValue().issues());
        }

        Path tempOutput = output.toAbsolutePath().normalize().resolveSibling(output.getFileName() + ".assembling");
        Path assembled = new StreamingAssembler().assemble(zip, tempOutput, descriptor, inventory);
        ValidationResult finalResult = fullValidator.validate(assembled);
        if (!finalResult.valid()) {
            try { Files.deleteIfExists(assembled); } catch (Exception ignored) { }
            throw new IllegalArgumentException("Az összefűzött XML nem felel meg az eredeti XSD-nek: " + finalResult.issues());
        }
        try {
            return Files.move(assembled, output.toAbsolutePath().normalize(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new IllegalArgumentException("A validált kimenet nem mozgatható a célhelyre.", e);
        }
    }

    private void validateZipEntry(ZipFile zipFile, PackageEntry entry, Map<String, ValidationResult> results) throws Exception {
        try (InputStream in = zipFile.getInputStream(zipFile.getEntry(entry.zipEntryName()))) {
            StreamSource source = new StreamSource(in);
            source.setSystemId("zip:" + entry.zipEntryName());
            results.put(entry.zipEntryName(), partValidator.validate(source, entry.kind()));
        }
    }
}
