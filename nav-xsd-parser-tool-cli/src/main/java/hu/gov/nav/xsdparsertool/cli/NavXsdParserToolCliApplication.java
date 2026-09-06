package hu.gov.nav.xsdparsertool.cli;

import hu.gov.nav.xsdparsertool.cli.command.GenerateCommand;
import hu.gov.nav.xsdparsertool.cli.command.InspectCommand;
import hu.gov.nav.xsdparsertool.cli.command.PrintHtmlCommand;
import hu.gov.nav.xsdparsertool.cli.command.ValidateCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * A NAV XSD Parser Tool általános parancssori belépési pontja.
 *
 * <p>Az osztály a Picocli gyökérparancsát definiálja, és innen érhetők el az
 * XML-vizsgálati, XSD-validációs, üres XML-generálási és nyomtatható HTML
 * előállítási alparancsok. Ha a felhasználó alparancs nélkül indítja a CLI-t,
 * a {@link #run()} a használati súgót írja ki.</p>
 *
 * <p>Az alkalmazás nem Spring Boot alkalmazás: a parancsok közvetlenül hozzák
 * létre a szükséges szolgáltatásimplementációkat.</p>
 */
@Command(
        name = "nav-xsd-parser-tool",
        mixinStandardHelpOptions = true,
        version = "1.0.0-SNAPSHOT",
        description = "NAV XSD Parser Tool CLI",
        subcommands = {
                InspectCommand.class,
                ValidateCommand.class,
                GenerateCommand.class,
                PrintHtmlCommand.class,
        }
)


public class NavXsdParserToolCliApplication implements Runnable {
    /**
     * Elindítja a Picocli parancssori feldolgozást, majd a parancs eredményének
     * megfelelő folyamatszintű kilépési kóddal befejezi a JVM futását.
     *
     * @param args a parancssorból kapott argumentumok
     */

    public static void main(String[] args) {
        int exitCode = new CommandLine(new NavXsdParserToolCliApplication()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Kiírja a gyökérparancs használati súgóját.
     *
     * <p>Ez az ág akkor fut le, amikor a CLI-t konkrét alparancs nélkül
     * indítják. Feldolgozást vagy fájlműveletet nem végez.</p>
     */
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
