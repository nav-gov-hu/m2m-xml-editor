package hu.gov.nav.xsdparsertool.cli.command;

import hu.gov.nav.xsdparsertool.core.model.processing.ExportResult;
import hu.gov.nav.xsdparsertool.processing.service.DefaultXmlProcessingService;
import hu.gov.nav.xsdparsertool.processing.service.XmlProcessingService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * A {@code generate} CLI alparancs megvalósítása.
 *
 * <p>A parancs az űrlaptípus és a pontos űrlapverzió alapján a
 * {@link XmlProcessingService#generateOpenableXml(String, String, Path, Path)}
 * műveletet hívja meg. A generáláshoz kizárólag az űrlapspecifikus XSD
 * repository szükséges; UIModel vagy külön common XSD útvonal nem része a
 * generálási szerződésnek.</p>
 */
@Command(name = "generate", mixinStandardHelpOptions = true,
        description = "Megnyitásra alkalmas új XML generálása űrlaptípus és verzió alapján.")
public class GenerateCommand implements Callable<Integer> {

    @Option(names = {"--form", "--document-type"}, required = true,
            description = "Űrlap technikai azonosítója, például NAV_F10.")
    private String documentType;

    @Option(names = "--version", required = true,
            description = "Űrlapverzió, például 1.12.")
    private String documentVersion;

    @Option(names = "--schema-dir", required = true, description = "XSD gyökérkönyvtár.")
    private Path schemaDir;

    @Option(names = "--out", required = true, description = "Kimeneti XML fájl.")
    private Path outputFile;

    /**
     * Végrehajtja a megnyitásra alkalmas új XML generálását a parancssori opciók alapján.
     *
     * @return {@code 0}, ha az export sikeres; egyébként {@code 5}
     */
    @Override
    public Integer call() {
        XmlProcessingService service = new DefaultXmlProcessingService();
        ExportResult result = service.generateOpenableXml(
                documentType, documentVersion, schemaDir, outputFile);
        System.out.println("Generált XML: " + result.getOutputFile());
        return result.isSuccess() ? 0 : 5;
    }
}
