package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.schemaregistry.model.XmlProbeResult;
import hu.gov.nav.xsdparsertool.schemaregistry.service.FileSystemSchemaRegistryService;
import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import hu.gov.nav.xsdparsertool.web.path.ConfiguredPathSupport;
import hu.gov.nav.xsdparsertool.web.path.VersionedArtifactPathResolver;
import hu.gov.nav.xsdparsertool.web.xpath.config.XPathValidatorProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlHeaderInfo;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlResourceResolutionInfo;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code XmlResourceResolutionService} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class XmlResourceResolutionService {
    private static final Logger log = LoggerFactory.getLogger(XmlResourceResolutionService.class);

    private final PathConfigurationProperties pathProperties;
    private final XPathValidatorProperties xpathValidatorProperties;
    private final FileSystemSchemaRegistryService schemaRegistryService = new FileSystemSchemaRegistryService();

    /**
     * Létrehozza a {@code XmlResourceResolutionService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param pathProperties a feldolgozásban részt vevő fájl vagy elérési út
     * @param xpathValidatorProperties a feldolgozásban részt vevő fájl vagy elérési út
     */
    public XmlResourceResolutionService(PathConfigurationProperties pathProperties,
                                        XPathValidatorProperties xpathValidatorProperties) {
        this.pathProperties = pathProperties;
        this.xpathValidatorProperties = xpathValidatorProperties;
    }

    /**
     * A {@code resolve} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param headerInfo a művelet bemeneti {@code headerInfo} értéke
     * @return a feloldott vagy lekért érték
     */
    public XmlResourceResolutionInfo resolve(XmlHeaderInfo headerInfo) {
        log.info("[RESOLVER-INFO-BUILD] START headerDetected={} rootElement={} namespace={} schemaLocation={} noNamespaceSchemaLocation={} headerFormType={} headerFormVersion={}",
                headerInfo != null && headerInfo.detected(),
                headerInfo == null ? null : headerInfo.rootElement(),
                headerInfo == null ? null : headerInfo.namespaceUri(),
                headerInfo == null ? null : headerInfo.schemaLocation(),
                headerInfo == null ? null : headerInfo.noNamespaceSchemaLocation(),
                headerInfo == null ? null : headerInfo.formType(),
                headerInfo == null ? null : headerInfo.formVersion());
        log.info("[RESOLVER-INFO-BUILD] CONFIG schemaDir={} commonXsdDir={} uiModelDir={} xpathRuleDir={}",
                pathProperties.getSchemaDir(), pathProperties.getCommonXsdDir(),
                pathProperties.getUiModelDir(), xpathValidatorProperties.getRuleRootDir());
        if (headerInfo == null || !headerInfo.detected()) {
            return new XmlResourceResolutionInfo(null, null, null, null, null,
                    "HEADER_NOT_DETECTED", "Az XML fejléc nem felismerhető, ezért az erőforrás-feloldás nem futott.");
        }

        Path schemaRoot = directoryOrNull(pathProperties.getSchemaDir());
        Path commonXsdRoot = directoryOrNull(pathProperties.getCommonXsdDir());
        Path uiModelRoot = directoryOrNull(pathProperties.getUiModelDir());
        log.info("[RESOLVER-INFO-BUILD] CONFIG_NORMALIZED schemaRoot={} commonXsdRoot={} uiModelRoot={} xpathRuleRoot={}",
                schemaRoot, commonXsdRoot, uiModelRoot,
                isBlank(xpathValidatorProperties.getRuleRootDir()) ? null
                        : ConfiguredPathSupport.toAbsoluteNormalizedPath(xpathValidatorProperties.getRuleRootDir()));

        if (schemaRoot == null) {
            String xpathPath = resolveXpathRulesPath(headerInfo.formType(), headerInfo.formVersion());
            log.warn("[RESOLVER-INFO-BUILD] CONFIG_MISSING schemaRoot=null xpathCandidate={}", xpathPath);
            return new XmlResourceResolutionInfo(headerInfo.formType(), headerInfo.formVersion(), null, null,
                    xpathPath,
                    "CONFIG_MISSING", "A űrlap-specifikus XSD gyökérkönyvtár nincs beállítva vagy nem létezik.");
        }

        try {
            XmlProbeResult probe = new XmlProbeResult();
            probe.setRootElementName(headerInfo.rootElement());
            probe.setNamespace(headerInfo.namespaceUri());
            probe.setSchemaLocation(headerInfo.schemaLocation());
            probe.setNoNamespaceSchemaLocation(headerInfo.noNamespaceSchemaLocation());

            SchemaBundle bundle = schemaRegistryService.resolveByXmlProbe(probe, schemaRoot, commonXsdRoot, uiModelRoot);
            String documentType = firstNonBlank(headerInfo.formType(), bundle.getDocumentType());
            String documentVersion = firstNonBlank(headerInfo.formVersion(), bundle.getDocumentVersion());
            String resolvedSchemaVersion = bundle.getDocumentVersion();
            boolean schemaVersionFallback = versionsDiffer(headerInfo.formVersion(), resolvedSchemaVersion);
            String xpathRulesPath = resolveXpathRulesPath(documentType, documentVersion);
            log.info("[RESOLVER-INFO-BUILD] BUNDLE bundleDocumentType={} bundleDocumentVersion={} headerDocumentType={} headerDocumentVersion={} effectiveDocumentType={} effectiveDocumentVersion={} primaryXsd={} uiModel={} xpathCandidate={} matchReason={}",
                    bundle.getDocumentType(), bundle.getDocumentVersion(), headerInfo.formType(), headerInfo.formVersion(),
                    documentType, documentVersion, pathToString(bundle.getPrimaryXsd()),
                    pathToString(bundle.getUiModelFile()), xpathRulesPath, bundle.getMatchReason());
            return new XmlResourceResolutionInfo(
                    documentType,
                    documentVersion,
                    pathToString(bundle.getPrimaryXsd()),
                    pathToString(bundle.getUiModelFile()),
                    xpathRulesPath,
                    bundle.getPrimaryXsd() != null ? "RESOLVED" : "PARTIAL",
                    schemaVersionFallback
                            ? "Az XML űrlapverziója (" + headerInfo.formVersion() + ") eltér a feloldott XSD verziójától (" + resolvedSchemaVersion + "). "
                              + "Az űrlap kompatibilitási módban, csak olvashatóan nyitható meg."
                            : bundle.getMatchReason(),
                    resolvedSchemaVersion,
                    schemaVersionFallback
            );
        } catch (Exception ex) {
            log.warn("[RESOLVER-INFO-BUILD] ERROR headerFormType={} headerFormVersion={} message={}",
                    headerInfo.formType(), headerInfo.formVersion(), ex.getMessage(), ex);
            return new XmlResourceResolutionInfo(headerInfo.formType(), headerInfo.formVersion(), null, null,
                    resolveXpathRulesPath(headerInfo.formType(), headerInfo.formVersion()),
                    "NOT_RESOLVED", "Erőforrás-feloldás sikertelen: " + ex.getMessage());
        }
    }

    /**
     * Jelzi, hogy az XML fejlécében szereplő űrlapverzió és a ténylegesen feloldott XSD főverzió eltér-e.
     */
    private boolean versionsDiffer(String xmlVersion, String schemaVersion) {
        if (isBlank(xmlVersion) || isBlank(schemaVersion)) {
            return false;
        }
        return !xmlVersion.trim().equalsIgnoreCase(schemaVersion.trim());
    }

    /**
     * A {@code directoryOrNull} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private Path directoryOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Path path = ConfiguredPathSupport.toAbsoluteNormalizedPath(value);
        return ExceptionSafeOperations.isDirectory(path) ? path : null;
    }

    /**
     * A {@code resolveXpathRulesPath} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param formType a művelet bemeneti {@code formType} értéke
     * @param formVersion a művelet bemeneti {@code formVersion} értéke
     * @return a feloldott vagy lekért érték
     */
    private String resolveXpathRulesPath(String formType, String formVersion) {
        String rawXpathRuleDir = xpathValidatorProperties.getRuleRootDir();
        if (isBlank(rawXpathRuleDir) || isBlank(formType) || isBlank(formVersion)) {
            log.warn("[RESOLVER-INFO-BUILD] XPATH_PATH_SKIPPED rawXpathRuleDir={} formType={} formVersion={}",
                    rawXpathRuleDir, formType, formVersion);
            return null;
        }
        Path ruleRoot = ConfiguredPathSupport.toAbsoluteNormalizedPath(rawXpathRuleDir);
        Path formDirectory = ruleRoot.resolve(formType);
        Path versionDirectory = formDirectory.resolve(formVersion);
        Path result = VersionedArtifactPathResolver.resolveXpathRule(ruleRoot, formType, formVersion);
        log.info("[RESOLVER-INFO-BUILD] XPATH_PATH_BUILD rawXpathRuleDir={} normalizedRuleRoot={} formType={} formVersion={} formDirectory={} versionDirectory={} resolvedFileName={} result={} exists={}",
                rawXpathRuleDir, ruleRoot, formType, formVersion, formDirectory, versionDirectory,
                result.getFileName(), result, ExceptionSafeOperations.isRegularFile(result));
        return result.toString();
    }

    /**
     * A {@code pathToString} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     */
    private String pathToString(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize().toString();
    }

    /**
     * A {@code firstNonBlank} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param first a művelet bemeneti {@code first} értéke
     * @param second a művelet bemeneti {@code second} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : second;
    }

    /**
     * A {@code isBlank} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return {@code true}, ha az ellenőrzött feltétel teljesül, egyébként {@code false}
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
