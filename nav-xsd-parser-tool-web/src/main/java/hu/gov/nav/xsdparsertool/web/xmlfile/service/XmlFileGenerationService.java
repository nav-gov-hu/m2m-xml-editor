package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;
import hu.gov.nav.xsdparsertool.processing.service.XmlProcessingService;
import hu.gov.nav.xsdparsertool.schemaregistry.service.FileSystemSchemaRegistryService;
import hu.gov.nav.xsdparsertool.web.config.PathConfigurationProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.CreateXmlFileRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlFileDto;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.XmlGenerationOptionDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * A megadott űrlaptípus és verzió alapján új, megnyitható XML állományt hoz létre.
 *
 * <p>A szolgáltatás a közös processing modul XML-generátorát használja, majd a
 * létrejött fájlt a normál XML-állománykezelési folyamatba regisztrálja. Így a
 * generált XML ugyanazokat a partner-, jogosultság-, validációs és megnyitási
 * szabályokat kapja, mint a feltöltött állományok.</p>
 */
@Service
public class XmlFileGenerationService {
    private final XmlProcessingService xmlProcessingService;
    private final FileSystemSchemaRegistryService schemaRegistryService;
    private final PathConfigurationProperties pathProperties;
    private final XmlFileStorageProperties storageProperties;
    private final XmlFileService xmlFileService;

    /**
     * Létrehozza az XML-generálási szolgáltatást.
     *
     * @param xmlProcessingService a közös XML feldolgozó/generáló szolgáltatás
     * @param schemaRegistryService a konfigurált XSD repository séma-regisztere
     * @param pathProperties az XSD repository konfigurált útvonalai
     * @param storageProperties az XML állománytár konfigurációja
     * @param xmlFileService a normál XML-regisztrációs szolgáltatás
     */
    public XmlFileGenerationService(XmlProcessingService xmlProcessingService,
                                    FileSystemSchemaRegistryService schemaRegistryService,
                                    PathConfigurationProperties pathProperties,
                                    XmlFileStorageProperties storageProperties,
                                    XmlFileService xmlFileService) {
        this.xmlProcessingService = xmlProcessingService;
        this.schemaRegistryService = schemaRegistryService;
        this.pathProperties = pathProperties;
        this.storageProperties = storageProperties;
        this.xmlFileService = xmlFileService;
    }

    /**
     * Felsorolja az Új XML létrehozásához választható űrlapokat és verziókat.
     *
     * @return a választható űrlapok rendezett listája
     */
    public List<XmlGenerationOptionDto> listOptions() {
        Path schemaRoot = requireSchemaRoot();
        return schemaRegistryService.listDocumentOptions(schemaRoot).stream()
                .map(option -> new XmlGenerationOptionDto(option.documentType(), option.versions()))
                .toList();
    }

    /**
     * Létrehozza és regisztrálja az új XML állományt.
     *
     * @param request a felhasználó által megadott létrehozási adatok
     * @return a regisztrált XML állomány adatai
     * @throws IOException ha a fizikai fájl létrehozása vagy regisztrációja sikertelen
     */
    public XmlFileDto create(CreateXmlFileRequest request) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("Hiányzó XML létrehozási kérés.");
        }
        String formType = requireText(request.formType(), "Az űrlap kiválasztása kötelező.");
        String formVersion = requireText(request.formVersion(), "Az űrlapverzió kiválasztása kötelező.");
        if (request.partnerId() == null) {
            throw new IllegalArgumentException("Az új XML létrehozásához partner megadása kötelező.");
        }

        Path uploadDir = requireUploadRoot();
        ExceptionSafeOperations.createDirectories(uploadDir);
        Path target = uploadDir.resolve(UUID.randomUUID() + ".xml").toAbsolutePath().normalize();
        try {
            xmlProcessingService.generateOpenableXml(formType, formVersion, requireSchemaRoot(), target);
            return xmlFileService.registerGeneratedFile(
                    target, request.fileName(), request.userNote(), request.partnerId());
        } catch (IOException | RuntimeException ex) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException cleanupError) {
                ex.addSuppressed(cleanupError);
            }
            throw ex;
        }
    }

    private Path requireSchemaRoot() {
        String configured = pathProperties.getSchemaDir();
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Az XSD repository könyvtára nincs konfigurálva.");
        }
        Path path = Path.of(configured).toAbsolutePath().normalize();
        if (!ExceptionSafeOperations.isDirectory(path)) {
            throw new IllegalStateException("Az XSD repository könyvtára nem létezik: " + path);
        }
        return path;
    }

    private Path requireUploadRoot() {
        String configured = storageProperties.getUploadDir();
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Az XML állománytár könyvtára nincs konfigurálva.");
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
