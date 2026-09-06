package hu.gov.nav.xsdparsertool.cli.command;

import hu.gov.nav.xsdparsertool.core.model.processing.ValidationResult;
import hu.gov.nav.xsdparsertool.core.model.validation.ValidationIssue;
import hu.gov.nav.xsdparsertool.processing.service.DefaultXmlProcessingService;
import hu.gov.nav.xsdparsertool.processing.service.XmlProcessingService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;
/**
 * A {@code validate} CLI alparancs megvalósítása.
 *
 * <p>Az XML-t a processing modulon keresztül a hozzá feloldott XSD-csomaggal
 * validálja. Sikeres validáció esetén rövid sikerüzenetet ír ki; hiba esetén
 * minden {@link ValidationIssue} súlyosságát, üzenetét és feloldott XML-útvonalát
 * felsorolja.</p>
 */


@Command(name = "validate", mixinStandardHelpOptions = true, description = "Validate XML against resolved schema bundle.")
public class ValidateCommand implements Callable<Integer> {

    @Option(names = "--xml", required = true, description = "Input XML file path.")
    private Path xmlFile;

    @Option(names = "--schema-dir", required = true, description = "Schema root directory.")
    private Path schemaDir;

    /**
     * Validálja az XML-fájlt a feloldott sémacsomaggal és kiírja az eredményt.
     *
     * <p>A tényleges validációt a {@link XmlProcessingService#validate(Path, Path)}
     * végzi. Érvénytelen XML esetén a validációs problémák listázása után
     * {@code 1} hibakódot ad vissza, így a parancs automatizált scriptekből is
     * felhasználható.</p>
     *
     * @return {@code 0}, ha az XML XSD szerint érvényes; egyébként {@code 1}
     */
    @Override
    public Integer call() {
        XmlProcessingService service = new DefaultXmlProcessingService();
        ValidationResult result = service.validate(xmlFile, schemaDir);
        if (result.isValid()) {
            System.out.println("Validation successful.");
            return 0;
        }
        System.out.println("Validation failed:");
        for (ValidationIssue issue : result.getIssues()) {
            System.out.printf("- [%s] %s (%s)%n", issue.getSeverity(), issue.getMessage(), issue.getPath());
        }
        return 1;
    }
}
