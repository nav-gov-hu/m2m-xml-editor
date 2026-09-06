package hu.gov.nav.xsdparsertool.xpathcli;

import hu.gov.nav.xsdparsertool.xpathcli.command.ValidateXsltCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Az önálló XPath/XSLT validációs parancssori alkalmazás fő belépési pontja.
 *
 * <p>A típus a Picocli gyökérparancsát definiálja, és a konkrét validációs
 * műveleteket alparancsokhoz delegálja. A támogatott alparancs a
 * {@link ValidateXsltCommand}, amely XSLT transzformációval futtat
 * XML-validációt.</p>
 *
 * <p>Paraméter nélküli futtatáskor a gyökérparancs használati útmutatóját írja
 * a standard kimenetre. A folyamat kilépési kódját a Picocli által végrehajtott
 * parancs eredménye határozza meg.</p>
 */
@Command(
        name = "nav-xsd-parser-tool-xpath-cli",
        mixinStandardHelpOptions = true,
        version = "1.0.0-SNAPSHOT",
        description = "Standalone CLI for XSL/XPath based XML validation.",
        subcommands = {
                ValidateXsltCommand.class
        }
)
public class NavXsdParserToolXPathCliApplication implements Runnable {

    /**
     * Elindítja a Picocli parancsértelmezőt a kapott parancssori argumentumokkal.
     *
     * <p>A metódus a végrehajtás után a Picocli által visszaadott kilépési kóddal
     * lezárja a JVM folyamatot.</p>
     *
     * @param args a parancssorból kapott argumentumok
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new NavXsdParserToolXPathCliApplication()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Megjeleníti a gyökérparancs használati útmutatóját.
     *
     * <p>Ezt a Picocli akkor hívja, amikor a gyökérparancs konkrét alparancs
     * nélkül fut le.</p>
     */
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
