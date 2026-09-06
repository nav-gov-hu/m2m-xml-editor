package hu.gov.nav.xsdparsertool.cli.command;

import hu.gov.nav.xsdparsertool.core.model.processing.ProcessingResult;
import hu.gov.nav.xsdparsertool.processing.service.DefaultXmlProcessingService;
import hu.gov.nav.xsdparsertool.processing.service.XmlProcessingService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;
/**
 * Az {@code inspect} CLI alparancs megvalósítása.
 *
 * <p>A parancs egy XML-fájlt vizsgál meg, feloldja a hozzá tartozó
 * sémacsomagot, majd kiírja a dokumentumtípust, az elsődleges XSD-t, a
 * UIModel-fájlt és a page-schema fájlt. A tényleges feloldást a processing
 * modul {@link XmlProcessingService#inspect(Path, Path)} szolgáltatása végzi.</p>
 */


@Command(name = "inspect", mixinStandardHelpOptions = true, description = "Inspect XML and resolve matching schema bundle.")
public class InspectCommand implements Callable<Integer> {

    @Option(names = "--xml", required = true, description = "Input XML file path.")
    private Path xmlFile;

    @Option(names = "--schema-dir", required = true, description = "Schema root directory.")
    private Path schemaDir;

    /**
     * Megvizsgálja a megadott XML-t és kiírja a feloldott sémacsomag fő adatait.
     *
     * <p>A metódus közvetlenül {@link DefaultXmlProcessingService} példányt
     * használ. A sikeres feldolgozás eredményét standard kimenetre írja; a
     * szolgáltatás által jelzett kivételeket nem alakítja át külön CLI
     * hibakóddá.</p>
     *
     * @return sikeres vizsgálat esetén {@code 0}
     */
    @Override
    public Integer call() {
        XmlProcessingService service = new DefaultXmlProcessingService();
        ProcessingResult result = service.inspect(xmlFile, schemaDir);
        System.out.println("Document type : " + result.getSchemaBundle().getDocumentType());
        System.out.println("Primary XSD   : " + result.getSchemaBundle().getPrimaryXsd());
        System.out.println("UI model      : " + result.getSchemaBundle().getUiModelFile());
        System.out.println("Page schema   : " + result.getSchemaBundle().getPageSchemaFile());
        return 0;
    }
}
