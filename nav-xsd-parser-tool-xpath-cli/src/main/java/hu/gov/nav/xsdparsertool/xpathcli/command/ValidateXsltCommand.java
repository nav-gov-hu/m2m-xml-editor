package hu.gov.nav.xsdparsertool.xpathcli.command;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.xpathcli.model.XsltValidationResult;
import hu.gov.nav.xsdparsertool.xpathcli.service.XsltValidationService;
import net.sf.saxon.s9api.SaxonApiException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * A {@code validate-xslt} parancs végrehajtója.
 *
 * <p>A parancs a megadott XSL/XSLT állománnyal validációs transzformációt futtat
 * egy XML bemeneten, majd a transzformáció eredményéből kiolvasott
 * {@code Hiba}/{@code hiba} elemek alapján állapítja meg a parancs kimenetelét.
 * A tényleges XSLT végrehajtást és eredményfeldolgozást az
 * {@link XsltValidationService} végzi.</p>
 *
 * <p>Ha a {@code --result-file} paraméter meg van adva, a nyers transzformációs
 * eredmény privát fájlírási segédlettel lemezre kerül. A parancs kilépési kódjai:
 * {@code 0} sikeres transzformáció validációs hiba nélkül, {@code 2} sikeres
 * transzformáció validációs hibákkal, {@code 1} pedig technikai XSLT- vagy
 * fájlkezelési hiba esetén.</p>
 */
@Command(
        name = "validate-xslt",
        mixinStandardHelpOptions = true,
        description = "Runs an XSLT-based XML validation and prints the extracted <hiba> messages."
)
public class ValidateXsltCommand implements Callable<Integer> {

    private final XsltValidationService validationService = new XsltValidationService();

    @Option(names = "--xsl", required = true, description = "Path to the XSL/XSLT file.")
    private Path xslPath;

    @Option(names = "--xml", required = true, description = "Path to the XML file to validate.")
    private Path xmlPath;

    @Option(names = "--rules-root", description = "Value of the XSLT parameter rules-root.")
    private String rulesRoot;

    @Option(names = "--rules-dir", defaultValue = "", description = "Value of the XSLT parameter rules-dir.")
    private String rulesDir = "";

    @Option(names = "--form-name", required = true, description = "Value of the XSLT parameter form-name.")
    private String formName;

    @Option(names = "--form-version", required = true, description = "Value of the XSLT parameter form-version.")
    private String formVersion;

    @Option(names = "--rules-file", description = "Value of the XSLT parameter rules-file. Defaults to the XML path.")
    private String rulesFile;

    @Option(names = "--result-file", description = "Optional path where the raw XSLT result XML should be saved.")
    private Path resultFile;

    @Option(names = "--encoding", defaultValue = "UTF-8", description = "Output XML encoding. Default: ${DEFAULT-VALUE}")
    private Charset encoding = StandardCharsets.UTF_8;

    @Option(names = "--print-errors", defaultValue = "false", description = "Print validation errors to console.")
    private boolean printErrors;

    /**
     * Végrehajtja az XSLT-alapú validációs parancsot.
     *
     * <p>Elsőként ellenőrzi, hogy az XSL és az XML bemeneti fájl valóban létezik
     * és szabályos fájl. Ezután előállítja a hiányzó {@code rules-root} és
     * {@code rules-file} alapértékeket, meghívja a validációs szolgáltatást,
     * opcionálisan lemezre írja a nyers eredmény-XML-t, végül a megtalált
     * validációs hibák alapján választ kilépési kódot.</p>
     *
     * <p>A Saxon- vagy I/O-hibák nem propagálódnak tovább a parancssorból:
     * a metódus hibaüzenetet ír a standard hibakimenetre és {@code 1}-es
     * kilépési kódot ad vissza. A bemeneti fájlok hiányát jelző
     * {@link IllegalArgumentException} a Picocli feldolgozására marad.</p>
     *
     * @return {@code 0}, ha nincs validációs hiba; {@code 2}, ha van legalább egy
     *         kinyert validációs hiba; {@code 1}, ha a transzformáció vagy a
     *         fájlkezelés technikai hibával leáll
     * @throws Exception a {@link Callable} szerződés miatt deklarált kivétel;
     *                   a metódus a Saxon- és I/O-hibákat saját maga kezeli
     */
    @Override
    public Integer call() throws Exception {
        validateInputs();
        String effectiveRulesRoot = rulesRoot != null ? rulesRoot : defaultRulesRoot();
        String effectiveRulesFile = rulesFile != null ? rulesFile : xmlPath.toAbsolutePath().normalize().toString();

        try {
            XsltValidationResult result = validationService.validate(
                    xslPath.toAbsolutePath().normalize(),
                    xmlPath.toAbsolutePath().normalize(),
                    effectiveRulesRoot,
                    rulesDir,
                    formName,
                    formVersion,
                    effectiveRulesFile,
                    encoding
            );

            if (resultFile != null) {
                Path normalizedResultFile = resultFile.toAbsolutePath().normalize();
                if (normalizedResultFile.getParent() != null) {
                    ExceptionSafeOperations.createDirectories(normalizedResultFile.getParent());
                }
                SecureFileOperations.writePrivateString(normalizedResultFile, result.rawOutputXml(), encoding);
            }

            if (result.hasErrors()) {
                System.out.println("Hibák száma: " + result.errorMessages().size());
                if (printErrors) {
                    System.out.println("XPath/XSLT validacios hibak:");
                    for (String errorMessage : result.errorMessages()) {
                        System.out.println(" - " + errorMessage);
                    }
                }
                return 2;
            }

            System.out.println("Hibák száma: 0");
            System.out.println("Nem talalhato <hiba> elem. A transzformacio lefutott.");
            return 0;
        } catch (SaxonApiException | IOException exception) {
            System.err.println("XSLT validacios hiba: " + exception.getMessage());
            return 1;
        }
    }

    /**
     * Ellenőrzi a kötelező XSL és XML bemeneti állományok elérhetőségét.
     *
     * <p>A vizsgálat az {@link ExceptionSafeOperations#isRegularFile(Path)}
     * segédet használja, ezért könyvtár vagy nem létező útvonal nem fogadható el
     * bemenetként.</p>
     *
     * @throws IllegalArgumentException ha az XSL vagy az XML útvonal nem létező,
     *                                  illetve nem szabályos fájlra mutat
     */
    private void validateInputs() {
        if (!ExceptionSafeOperations.isRegularFile(xslPath)) {
            throw new IllegalArgumentException("Az XSL fajl nem talalhato: " + xslPath);
        }
        if (!ExceptionSafeOperations.isRegularFile(xmlPath)) {
            throw new IllegalArgumentException("Az XML fajl nem talalhato: " + xmlPath);
        }
    }

    /**
     * Meghatározza a {@code rules-root} paraméter alapértékét.
     *
     * <p>Ha a felhasználó nem ad meg külön szabálygyökér-könyvtárat, a bemeneti
     * XML normalizált abszolút szülőkönyvtára lesz az alapérték. Ha az XML
     * útvonalának nincs szülője, a jelenlegi könyvtárat jelölő {@code "."}
     * érték kerül visszaadásra.</p>
     *
     * @return az alapértelmezett szabálygyökér elérési útja
     */
    private String defaultRulesRoot() {
        Path parent = xmlPath.toAbsolutePath().normalize().getParent();
        return parent != null ? parent.toString() : ".";
    }
}
