package hu.gov.nav.xsdparsertool.multiform;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

/** Minimal dependency-free command line interface. */
public final class MultiformCli {
    private MultiformCli() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0]) || "--help".equalsIgnoreCase(args[0])) {
            usage();
            return;
        }
        String command = args[0];
        Map<String, String> options = parse(Arrays.copyOfRange(args, 1, args.length));
        Path xsd = Path.of(required(options, "--xsd"));
        MultiformService service = new MultiformService(xsd);

        switch (command) {
            case "analyze" -> printDescriptor(service.descriptor());
            case "adapter" -> {
                PartKind kind = PartKind.valueOf(required(options, "--part").toUpperCase());
                String text = service.adapterXsd(kind);
                if (options.containsKey("--output")) {
                    Files.writeString(Path.of(options.get("--output")), text, StandardCharsets.UTF_8);
                } else {
                    System.out.print(text);
                }
            }
            case "validate" -> {
                PartKind kind = PartKind.valueOf(required(options, "--part").toUpperCase());
                ValidationResult result = service.validatePart(Path.of(required(options, "--xml")), kind);
                printValidation(result);
                if (!result.valid()) System.exit(2);
            }
            case "validate-package" -> {
                var results = service.validatePackage(Path.of(required(options, "--zip")));
                boolean allValid = true;
                for (var entry : results.entrySet()) {
                    System.out.println(entry.getKey() + ": " + (entry.getValue().valid() ? "VALID" : "INVALID"));
                    if (!entry.getValue().valid()) {
                        allValid = false;
                        entry.getValue().issues().forEach(i -> System.out.println("  " + i));
                    }
                }
                if (!allValid) System.exit(2);
            }
            case "merge" -> {
                Path result = service.merge(Path.of(required(options, "--zip")), Path.of(required(options, "--output")));
                System.out.println("Elkészült: " + result);
            }
            default -> throw new IllegalArgumentException("Ismeretlen parancs: " + command);
        }
    }

    private static void printDescriptor(MultiformDescriptor d) {
        System.out.println("Document:  " + d.documentElement());
        System.out.println("Type:      " + d.documentType());
        System.out.println("Namespace: " + d.targetNamespace());
        System.out.println("Main:      " + d.mainPart());
        System.out.println("Repeating: " + d.repeatingPart());
    }

    private static void printValidation(ValidationResult result) {
        System.out.println(result.valid() ? "VALID" : "INVALID");
        result.issues().forEach(i -> System.out.println(i.line() + ":" + i.column() + " " + i.message()));
    }

    private static Map<String, String> parse(String[] args) {
        java.util.LinkedHashMap<String, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 >= args.length || !args[i].startsWith("--")) {
                throw new IllegalArgumentException("Hibás CLI argumentumok.");
            }
            map.put(args[i], args[i + 1]);
        }
        return map;
    }

    private static String required(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Hiányzó paraméter: " + key);
        return value;
    }

    private static void usage() {
        System.out.println("""
                Használat:
                  analyze --xsd NAV_2608.xsd
                  adapter --xsd NAV_2608.xsd --part MAIN|REPEATING [--output adapter.xsd]
                  validate --xsd NAV_2608.xsd --part MAIN|REPEATING --xml part.xml
                  validate-package --xsd NAV_2608.xsd --zip input.zip
                  merge --xsd NAV_2608.xsd --zip input.zip --output full.xml
                """);
    }
}
