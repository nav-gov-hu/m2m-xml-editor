package hu.gov.nav.xsdparsertool.processing.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.core.enums.Severity;
import hu.gov.nav.xsdparsertool.core.model.processing.ExportResult;
import hu.gov.nav.xsdparsertool.core.model.processing.ProcessingResult;
import hu.gov.nav.xsdparsertool.core.model.processing.ValidationResult;
import hu.gov.nav.xsdparsertool.core.model.validation.ValidationIssue;
import hu.gov.nav.xsdparsertool.pageschema.service.NoOpPageSchemaParserService;
import hu.gov.nav.xsdparsertool.pageschema.service.PageSchemaParserService;
import hu.gov.nav.xsdparsertool.processing.validation.XsdValidationService;
import hu.gov.nav.xsdparsertool.processing.xml.XmlProbeService;
import hu.gov.nav.xsdparsertool.schemaregistry.model.XmlProbeResult;
import hu.gov.nav.xsdparsertool.schemaregistry.service.FileSystemSchemaRegistryService;
import hu.gov.nav.xsdparsertool.schemaregistry.service.SchemaRegistryService;
import hu.gov.nav.xsdparsertool.uimodel.service.NoOpUiModelParserService;
import hu.gov.nav.xsdparsertool.uimodel.service.UiModelParserService;
import hu.gov.nav.xsdparsertool.xsd.service.BasicXsdParserService;
import hu.gov.nav.xsdparsertool.xsd.service.XsdParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Az {@link XmlProcessingService} alapértelmezett megvalósítása.
 *
 * <p>Az XML probe, Schema Registry, XSD parser, UIModel parser, Page Schema parser és
 * XSD-validátor komponenseket egyetlen magas szintű feldolgozási folyamatba szervezi.</p>
 */
public class DefaultXmlProcessingService implements XmlProcessingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultXmlProcessingService.class);

    private final XmlProbeService xmlProbeService;
    private final SchemaRegistryService schemaRegistryService;
    private final XsdParserService xsdParserService;
    private final UiModelParserService uiModelParserService;
    private final PageSchemaParserService pageSchemaParserService;
    private final XsdValidationService xsdValidationService;

/**
 * Létrehozza a szolgáltatást a modul alapértelmezett komponenseivel.
 *
 * <p>Közvetlen Java- és CLI-használatra alkalmas.</p>
 */
    public DefaultXmlProcessingService() {
        this(
                new XmlProbeService(),
                new FileSystemSchemaRegistryService(),
                new BasicXsdParserService(),
                new NoOpUiModelParserService(),
                new NoOpPageSchemaParserService(),
                new XsdValidationService()
        );
    }

/**
 * Létrehozza a szolgáltatást explicit függőségekkel.
 * @param xmlProbeService az XML azonosítását végző szolgáltatás
 * @param schemaRegistryService a séma-csomagot feloldó registry
 * @param xsdParserService az XSD dokumentumdefinícióját felépítő parser
 * @param uiModelParserService a UIModel metaadatokat alkalmazó parser
 * @param pageSchemaParserService a Page Schema metaadatokat alkalmazó parser
 * @param xsdValidationService az XSD-validációt végző szolgáltatás
 */
    public DefaultXmlProcessingService(XmlProbeService xmlProbeService,
                                       SchemaRegistryService schemaRegistryService,
                                       XsdParserService xsdParserService,
                                       UiModelParserService uiModelParserService,
                                       PageSchemaParserService pageSchemaParserService,
                                       XsdValidationService xsdValidationService) {
        this.xmlProbeService = xmlProbeService;
        this.schemaRegistryService = schemaRegistryService;
        this.xsdParserService = xsdParserService;
        this.uiModelParserService = uiModelParserService;
        this.pageSchemaParserService = pageSchemaParserService;
        this.xsdValidationService = xsdValidationService;
    }

    /**
     * Feldolgozza az XML-t a megadott séma-gyökér használatával.
     *
     * @param xmlFile a feldolgozandó XML állomány
     * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
     * @return a feloldott séma-csomagot és dokumentumdefiníciót tartalmazó eredmény
     */
    @Override
    public ProcessingResult inspect(Path xmlFile, Path schemaRootDir) {
        return inspect(xmlFile, schemaRootDir, null, null);
    }

    /**
     * Feldolgozza az XML-t külön általános XSD-könyvtár figyelembevételével.
     *
     * @param xmlFile a feldolgozandó XML állomány
     * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
     * @param generalXsdDir a közös/általános XSD-k könyvtára
     * @return a feldolgozás eredménye
     */
    @Override
    public ProcessingResult inspect(Path xmlFile, Path schemaRootDir, Path generalXsdDir) {
        return inspect(xmlFile, schemaRootDir, generalXsdDir, null);
    }

    /**
     * Végrehajtja a teljes XML-felderítési és dokumentumdefiníció-építési folyamatot.
     *
     * <p>Probe-olja az XML-t, újraindexeli és lekérdezi a Schema Registryt, XSD-ből
     * felépíti a dokumentumdefiníciót, majd alkalmazza az opcionális UIModel és Page Schema
     * feldolgozási lépéseket.</p>
     *
     * @param xmlFile a feldolgozandó XML állomány
     * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
     * @param generalXsdDir a közös/általános XSD-k könyvtára
     * @param uiModelDir a UIModel állományok gyökérkönyvtára
     * @return a feldolgozás eredménye
     */
    @Override
    public ProcessingResult inspect(Path xmlFile, Path schemaRootDir, Path generalXsdDir, Path uiModelDir) {
        LOGGER.info(
                "Inspecting XML file {} with schema root {}, general XSD dir {} and UI model dir {}",
                xmlFile, schemaRootDir, generalXsdDir, uiModelDir
        );

        XmlProbeResult probe = xmlProbeService.probe(xmlFile);
        var bundle = schemaRegistryService.resolveByXmlProbe(probe, schemaRootDir, generalXsdDir, uiModelDir);

        ProcessingResult result = new ProcessingResult();
        result.setSchemaBundle(bundle);
        result.setDocumentDefinition(xsdParserService.parse(bundle));

        if (bundle.getUiModelFile() != null) {
            uiModelParserService.applyUiModel(result.getDocumentDefinition(), bundle.getUiModelFile());
        }
        if (bundle.getPageSchemaFile() != null) {
            pageSchemaParserService.applyPageSchema(result.getDocumentDefinition(), bundle.getPageSchemaFile());
        }


        return result;
    }

    /**
     * XSD szerint validálja az XML-t a megadott séma-gyökérrel.
     *
     * @param xmlFile a validálandó XML állomány
     * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
     * @return a validáció eredménye és hibái
     */
    @Override
    public ValidationResult validate(Path xmlFile, Path schemaRootDir) {
        return validate(xmlFile, schemaRootDir, null, null);
    }

    /**
     * XSD szerint validálja az XML-t külön általános XSD-könyvtárral.
     *
     * @param xmlFile a validálandó XML állomány
     * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
     * @param generalXsdDir a közös/általános XSD-k könyvtára
     * @return a validáció eredménye és hibái
     */
    @Override
    public ValidationResult validate(Path xmlFile, Path schemaRootDir, Path generalXsdDir) {
        return validate(xmlFile, schemaRootDir, generalXsdDir, null);
    }

    /**
     * A teljes sémafeloldási környezet használatával validálja az XML-t.
     *
     * <p>A validáció előtt ugyanazzal a probe és Schema Registry folyamattal választja ki
     * a séma-csomagot, amelyet az inspect művelet is használ, majd az
     * {@link XsdValidationService} végzi a tényleges XSD-ellenőrzést.</p>
     *
     * @param xmlFile a validálandó XML állomány
     * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
     * @param generalXsdDir a közös/általános XSD-k könyvtára
     * @param uiModelDir a UIModel könyvtár, amely a registry újraindexeléséhez használható
     * @return a validáció eredménye és hibái
     */
    @Override
    public ValidationResult validate(Path xmlFile,
                                     Path schemaRootDir,
                                     Path generalXsdDir,
                                     Path uiModelDir) {
        List<ValidationIssue> preflightIssues = new ArrayList<>();

        if (xmlFile == null || !ExceptionSafeOperations.fileExists(xmlFile) || !ExceptionSafeOperations.isRegularFile(xmlFile)) {
            preflightIssues.add(new ValidationIssue(
                    "FILE_NOT_FOUND",
                    xmlFile == null ? null : xmlFile.toString(),
                    "XML file does not exist",
                    Severity.ERROR
            ));
        }
        if (schemaRootDir == null || !ExceptionSafeOperations.isDirectory(schemaRootDir)) {
            preflightIssues.add(new ValidationIssue(
                    "SCHEMA_DIR_NOT_FOUND",
                    schemaRootDir == null ? null : schemaRootDir.toString(),
                    "Schema directory does not exist",
                    Severity.ERROR
            ));
        }
        if (generalXsdDir != null && !ExceptionSafeOperations.isDirectory(generalXsdDir)) {
            preflightIssues.add(new ValidationIssue(
                    "GENERAL_XSD_DIR_NOT_FOUND",
                    generalXsdDir.toString(),
                    "General XSD directory does not exist",
                    Severity.ERROR
            ));
        }
        if (uiModelDir != null && !ExceptionSafeOperations.isDirectory(uiModelDir)) {
            preflightIssues.add(new ValidationIssue(
                    "UI_MODEL_DIR_NOT_FOUND",
                    uiModelDir.toString(),
                    "UI model directory does not exist",
                    Severity.ERROR
            ));
        }

        if (!preflightIssues.isEmpty()) {
            ValidationResult result = new ValidationResult();
            result.setIssues(preflightIssues);
            result.setValid(false);
            return result;
        }

        ProcessingResult inspection = inspect(xmlFile, schemaRootDir, generalXsdDir, uiModelDir);
        ValidationResult validationResult = xsdValidationService.validate(xmlFile, inspection.getSchemaBundle(), generalXsdDir);

        if (inspection.getSchemaBundle() != null && inspection.getSchemaBundle().getPrimaryXsd() != null) {
            validationResult.getIssues().add(
                    0,
                    new ValidationIssue(
                            "SCHEMA_RESOLVED",
                            inspection.getSchemaBundle().getPrimaryXsd().toString(),
                            "Resolved primary XSD: " + inspection.getSchemaBundle().getPrimaryXsd().getFileName(),
                            Severity.INFO
                    )
            );
        }
        if (inspection.getSchemaBundle() != null && inspection.getSchemaBundle().getUiModelFile() != null) {
            validationResult.getIssues().add(
                    1,
                    new ValidationIssue(
                            "UI_MODEL_RESOLVED",
                            inspection.getSchemaBundle().getUiModelFile().toString(),
                            "Resolved UI model: " + inspection.getSchemaBundle().getUiModelFile().getFileName(),
                            Severity.INFO
                    )
            );
        }

        return validationResult;
    }

    /**
     * Minimális XML állományt generál a dokumentumtípus és séma-gyökér alapján.
     *
     * @param documentType a dokumentumtípus technikai azonosítója
     * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
     * @param outputFile a létrehozandó XML célfájlja
     * @return az export eredménye
     */
    @Override
    public ExportResult generateEmptyXml(String documentType, Path schemaRootDir, Path outputFile) {
        return generateEmptyXml(documentType, schemaRootDir, null, null, outputFile);
    }

    /**
     * Minimális XML állományt generál külön általános XSD-könyvtár figyelembevételével.
     *
     * @param documentType a dokumentumtípus technikai azonosítója
     * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
     * @param generalXsdDir a közös/általános XSD-k könyvtára
     * @param outputFile a létrehozandó XML célfájlja
     * @return az export eredménye
     */
    @Override
    public ExportResult generateEmptyXml(String documentType, Path schemaRootDir, Path generalXsdDir, Path outputFile) {
        return generateEmptyXml(documentType, schemaRootDir, generalXsdDir, null, outputFile);
    }

    /**
     * Feloldja a dokumentumtípus séma-csomagját, és abból létrehozza a minimális XML-t.
     *
     * <p>A generált dokumentum a feloldott XSD gyökérelemét és namespace-ét használja;
     * namespace esetén a sémahely hivatkozása is bekerül. A célkönyvtár szükség esetén
     * létrejön, az írás pedig a biztonságos fájlműveleti segéd használatával történik.</p>
     *
     * @param documentType a dokumentumtípus technikai azonosítója
     * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
     * @param generalXsdDir a közös/általános XSD-k könyvtára
     * @param uiModelDir a UIModel könyvtár a registry indexeléséhez
     * @param outputFile a létrehozandó XML célfájlja
     * @return az export sikerességét és célútvonalát tartalmazó eredmény
     */
    public ExportResult generateEmptyXml(String documentType,
                                         Path schemaRootDir,
                                         Path generalXsdDir,
                                         Path uiModelDir,
                                         Path outputFile) {
        var bundle = schemaRegistryService.resolveByDocumentType(documentType, schemaRootDir, generalXsdDir, uiModelDir);

        String rootName = bundle.getRootElementName() != null ? bundle.getRootElementName() : bundle.getDocumentType();
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<" + rootName + "></" + rootName + ">\n";

        try {
            Path parent = outputFile.getParent();
            if (parent != null) {
                ExceptionSafeOperations.createDirectories(parent);
            }
            SecureFileOperations.writePrivateString(outputFile, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write output XML: " + outputFile, e);
        }

        ExportResult result = new ExportResult();
        result.setSuccess(true);
        result.setOutputFile(outputFile);
        return result;
    }
}