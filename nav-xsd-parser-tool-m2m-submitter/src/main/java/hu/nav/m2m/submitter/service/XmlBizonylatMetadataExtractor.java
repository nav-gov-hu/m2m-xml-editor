package hu.nav.m2m.submitter.service;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.processing.xml.XmlProbeService;
import hu.gov.nav.xsdparsertool.schemaregistry.service.FileSystemSchemaRegistryService;
import hu.gov.nav.xsdparsertool.schemaregistry.service.SchemaRegistryService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Az XML-ből és szükség esetén a Schema Registryből feloldja a Bizonylat API útvonalához szükséges dokumentumtípus- és verziómetaadatokat.
 */
@Component
public class XmlBizonylatMetadataExtractor {
    private static final Pattern NAV_MODEL_NAMESPACE = Pattern.compile(
            "https://soap\\.api\\.nav\\.gov\\.hu/definitions/model/2\\.0/([^/\\s\"'<>]+)/([^/\\s\"'<>]+)"
    );

    private final ObjectProvider<SchemaRegistryService> schemaRegistryProvider;
    private final Environment environment;
    private final XmlProbeService xmlProbeService = new XmlProbeService();

    /**
     * Létrehozza a(z) {@code XmlBizonylatMetadataExtractor} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param schemaRegistryProvider a művelethez átadott {@code schemaRegistryProvider} érték
     * @param environment a művelethez átadott {@code environment} érték
     */
    public XmlBizonylatMetadataExtractor(ObjectProvider<SchemaRegistryService> schemaRegistryProvider,
                                         Environment environment) {
        this.schemaRegistryProvider = schemaRegistryProvider;
        this.environment = environment;
    }

    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param xmlPath a művelethez átadott {@code xmlPath} érték
     * @return a művelet eredménye
     */
    public Optional<BizonylatMetadata> extract(Path xmlPath) {
        Optional<BizonylatMetadata> registryMetadata = extractFromSchemaRegistry(xmlPath);
        if (registryMetadata.isPresent()) {
            return registryMetadata;
        }
        try {
            String text = Files.readString(xmlPath, StandardCharsets.UTF_8);
            return extract(text);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * A bemeneti struktúrából biztonságosan kiolvassa a művelethez szükséges értéket, és hiányzó adat esetén a metódus szerinti fallbacket alkalmazza.
     *
     * @param xmlText a művelethez átadott {@code xmlText} érték
     * @return a művelet eredménye
     */
    public Optional<BizonylatMetadata> extract(String xmlText) {
        if (xmlText == null || xmlText.isBlank()) {
            return Optional.empty();
        }
        if (xmlText.length() > 16 * 1024 * 1024) {
            return Optional.empty();
        }
        Matcher matcher = NAV_MODEL_NAMESPACE.matcher(xmlText);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String type = normalize(matcher.group(1));
        String version = normalize(matcher.group(2));
        if (type != null && version != null) {
            return Optional.of(new BizonylatMetadata(type, version, matcher.group(0), "XML_NAMESPACE_FALLBACK", null, null));
        }
        return Optional.empty();
    }

    /**
     * A Schema Registry által feloldott séma-csomag metaadataiból állítja elő a Bizonylat API útvonalához használható dokumentumtípus és verzió értékeket.
     *
     * @param xmlPath a művelethez átadott {@code xmlPath} érték
     * @return a művelet eredménye
     */
    private Optional<BizonylatMetadata> extractFromSchemaRegistry(Path xmlPath) {
        if (xmlPath == null) {
            return Optional.empty();
        }
        Path schemaRoot = configuredPath("nav.xsdparsertool.paths.schema-dir");
        if (schemaRoot == null) {
            return Optional.empty();
        }
        try {
            Path generalXsdRoot = configuredPath("nav.xsdparsertool.paths.common-xsd-dir");
            Path uiModelRoot = configuredPath("nav.xsdparsertool.paths.ui-model-dir");
            SchemaRegistryService registry = schemaRegistryProvider.getIfAvailable(
                    () -> new FileSystemSchemaRegistryService(schemaRoot, generalXsdRoot)
            );
            SchemaBundle bundle = registry.resolveByXmlProbe(
                    xmlProbeService.probe(xmlPath),
                    schemaRoot,
                    generalXsdRoot,
                    uiModelRoot
            );
            if (bundle == null) {
                return Optional.empty();
            }
            String type = normalize(bundle.getDocumentType());
            String version = normalize(bundle.getDocumentVersion());
            if (type == null || version == null) {
                return Optional.empty();
            }
            return Optional.of(new BizonylatMetadata(
                    type,
                    version,
                    bundle.getTargetNamespace(),
                    "SCHEMA_REGISTRY",
                    pathToString(bundle.getPrimaryXsd()),
                    bundle.getMatchReason()
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * A(z) {@code configuredPath} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param key a művelethez átadott {@code key} érték
     * @return igaz, ha a vizsgált feltétel teljesül; egyébként hamis
     */
    private Path configuredPath(String key) {
        String value = environment == null ? null : environment.getProperty(key);
        value = normalize(value);
        if (value == null) {
            return null;
        }
        return Path.of(value);
    }

    /**
     * A(z) {@code pathToString} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param path a feldolgozandó vagy ellenőrzendő fájlútvonal
     * @return a művelet eredménye
     */
    private String pathToString(Path path) {
        return path == null ? null : path.toString();
    }

    /**
     * A bemeneti szöveget a későbbi összehasonlításhoz vagy route-képzéshez kanonikus formára alakítja.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * A NAV M2M submitter modul {@code BizonylatMetadata} típusának felelősségét megvalósító típus.
     */
    /**
     * Létrehozza a(z) {@code BizonylatMetadata} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param bizonylatTipus a művelethez átadott {@code bizonylatTipus} érték
     * @param bizonylatVerzio a művelethez átadott {@code bizonylatVerzio} érték
     * @param namespace a művelethez átadott {@code namespace} érték
     * @param source a művelethez átadott {@code source} érték
     * @param primaryXsd a művelethez átadott {@code primaryXsd} érték
     * @param matchReason a művelethez átadott {@code matchReason} érték
     */
    public record BizonylatMetadata(String bizonylatTipus,
                                    String bizonylatVerzio,
                                    String namespace,
                                    String source,
                                    String primaryXsd,
                                    String matchReason) {}
}
