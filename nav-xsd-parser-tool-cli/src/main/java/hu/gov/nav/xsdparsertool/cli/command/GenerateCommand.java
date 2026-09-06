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
 * <p>A parancs a dokumentumtípus és a séma-gyökérkönyvtár alapján a
 * {@link XmlProcessingService#generateEmptyXml(String, Path, Path)} műveletet
 * hívja meg, amely minimális XML-dokumentumot állít elő a megadott kimeneti
 * fájlba. A parancs siker esetén {@code 0}, sikertelen export esetén
 * {@code 5} kilépési kódot ad vissza.</p>
 */


@Command(name = "generate", mixinStandardHelpOptions = true, description = "Generate a minimal XML document for a document type.")
public class GenerateCommand implements Callable<Integer> {

    @Option(names = "--document-type", required = true, description = "Document type identifier.")
    private String documentType;

    @Option(names = "--schema-dir", required = true, description = "Schema root directory.")
    private Path schemaDir;

    @Option(names = "--out", required = true, description = "Output XML file path.")
    private Path outputFile;

    /**
     * Végrehajtja az üres XML generálását a parancssori opciók alapján.
     *
     * <p>A feldolgozáshoz közvetlenül {@link DefaultXmlProcessingService}
     * példányt hoz létre. A létrejött fájl elérési útját standard kimenetre
     * írja.</p>
     *
     * @return {@code 0}, ha az export sikeres; egyébként {@code 5}
     */
    @Override
    public Integer call() {
        XmlProcessingService service = new DefaultXmlProcessingService();
        ExportResult result = service.generateEmptyXml(documentType, schemaDir, outputFile);
        System.out.println("Generated XML: " + result.getOutputFile());
        return result.isSuccess() ? 0 : 5;
    }
}
