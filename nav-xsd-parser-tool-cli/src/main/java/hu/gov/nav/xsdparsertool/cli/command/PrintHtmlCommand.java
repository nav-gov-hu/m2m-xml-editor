package hu.gov.nav.xsdparsertool.cli.command;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.print.model.PrintOptions;
import hu.gov.nav.xsdparsertool.print.service.DefaultXmlPrintService;
import hu.gov.nav.xsdparsertool.print.service.XmlPrintService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * A {@code print-html} CLI alparancs megvalósítása.
 *
 * <p>A parancs a megadott XML-ből és sémaforrásokból a print modul
 * {@link XmlPrintService} szolgáltatásával nyomtatható HTML-t készít. A
 * {@link PrintOptions} objektumba átadja a mezőazonosítók megjelenítését, a
 * csak kitöltött mezők szűrését és az opcionális kézi UIModel-felülbírálást.</p>
 *
 * <p>A létrehozott HTML-t UTF-8 kódolással írja a célfájlba. A szülőkönyvtár
 * létrehozásához és a fájl írásához a {@link SecureFileOperations} biztonságos
 * fájlműveleteit használja.</p>
 */
@Command(name = "print-html", mixinStandardHelpOptions = true, description = "Generate printable HTML from XML and UI model.")
public class PrintHtmlCommand implements Callable<Integer> {

    @Option(names = "--xml", required = true, description = "Input XML file path.")
    private Path xmlFile;

    @Option(names = "--schema-dir", required = true, description = "Schema root directory.")
    private Path schemaDir;

    @Option(names = "--general-xsd-dir", description = "General XSD directory.")
    private Path generalXsdDir;

    @Option(names = "--ui-model-dir", description = "UI model directory.")
    private Path uiModelDir;


    @Option(names = "--ui-model", description = "Manual UI model override file.")
    private Path uiModelOverrideFile;

    @Option(names = "--show-field-ids", defaultValue = "false", description = "Show field identifiers in output.")
    private boolean showFieldIds;

    @Option(names = "--only-filled-fields", defaultValue = "false", description = "Render only filled fields.")
    private boolean onlyFilledFields;

    @Option(names = "--out", required = true, description = "Output HTML file path.")
    private Path outputFile;

    /**
     * Előállítja és fájlba írja a nyomtatható HTML-nézetet.
     *
     * <p>A generálást a {@link DefaultXmlPrintService} végzi. Ha a célfájl
     * szülőkönyvtára még nem létezik, a metódus létrehozza azt, majd a HTML-t
     * privát jogosultságokra törekvő fájlművelettel menti. Siker esetén a
     * normalizált abszolút kimeneti útvonalat kiírja.</p>
     *
     * @return sikeres HTML-generálás és mentés esetén {@code 0}
     * @throws Exception ha a nyomtatási feldolgozás vagy a kimeneti fájl írása meghiúsul
     */
    @Override
    public Integer call() throws Exception {
        XmlPrintService service = new DefaultXmlPrintService();
        PrintOptions options = new PrintOptions();
        options.setShowFieldIds(showFieldIds);
        options.setOnlyFilledFields(onlyFilledFields);
        options.setUiModelOverrideFile(uiModelOverrideFile);
        String html = service.generateHtml(xmlFile, schemaDir, generalXsdDir, uiModelDir, options);
        Path parent = outputFile.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            SecureFileOperations.createPrivateDirectories(parent);
        }
        SecureFileOperations.writePrivateString(outputFile, html, StandardCharsets.UTF_8);
        System.out.println("Printable HTML created: " + outputFile.toAbsolutePath().normalize());
        return 0;
    }
}
