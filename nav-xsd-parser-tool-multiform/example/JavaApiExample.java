import hu.gov.nav.xsdparsertool.multiform.MultiformDescriptor;
import hu.gov.nav.xsdparsertool.multiform.MultiformService;
import hu.gov.nav.xsdparsertool.multiform.PartKind;
import hu.gov.nav.xsdparsertool.multiform.ValidationResult;

import java.nio.file.Path;

/**
 * Minimal standalone Java example for the multiform library.
 */
public final class JavaApiExample {
    private JavaApiExample() {
    }

    public static void main(String[] args) {
        Path xsd = Path.of("example/2608/schema/NAV_2608.xsd");
        Path packageZip = Path.of("example/2608/input.zip");
        Path output = Path.of("example/2608/generated/2608-full.xml");

        MultiformService service = new MultiformService(xsd);

        MultiformDescriptor descriptor = service.descriptor();
        System.out.println("Dokumentum: " + descriptor.documentElement());
        System.out.println("Főlap: " + descriptor.mainPart().elementName());
        System.out.println("Melléklap: " + descriptor.repeatingPart().elementName());

        ValidationResult mainResult = service.validatePart(
                Path.of("example/2608/input/2608A.xml"),
                PartKind.MAIN);
        if (!mainResult.valid()) {
            throw new IllegalStateException("A főlap hibás: " + mainResult.issues());
        }

        ValidationResult attachmentResult = service.validatePart(
                Path.of("example/2608/input/2608M_000001.xml"),
                PartKind.REPEATING);
        if (!attachmentResult.valid()) {
            throw new IllegalStateException("A melléklap hibás: " + attachmentResult.issues());
        }

        Path result = service.merge(packageZip, output);
        System.out.println("Elkészült: " + result.toAbsolutePath());
    }
}
