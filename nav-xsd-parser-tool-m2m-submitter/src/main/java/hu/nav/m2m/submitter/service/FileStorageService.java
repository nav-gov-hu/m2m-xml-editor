package hu.nav.m2m.submitter.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.nav.m2m.submitter.config.NavM2mProperties;
import hu.nav.m2m.submitter.util.Sha256Util;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.UUID;

/**
 * A beküldéshez és csatolmányokhoz tartozó fájlok kontrollált, menedzselt tárhelyre mentését és hash-képzését végző szolgáltatás.
 */
@Service
public class FileStorageService {
    private final Path baseDirectory;

    /**
     * Létrehozza a(z) {@code FileStorageService} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param properties az M2M külső konfiguráció
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public FileStorageService(NavM2mProperties properties) throws IOException {
        this.baseDirectory = Path.of(properties.getStorageDirectory()).toAbsolutePath().normalize();
        ExceptionSafeOperations.createDirectories(baseDirectory);
    }

    /**
     * A beküldési munkakönyvtárba menti a kapott MultipartFile tartalmát, biztonságos fájlnevet képez, majd méretet és SHA-256 hash-t számít.
     *
     * @param submissionId a cél M2M beküldés azonosítója
     * @param multipartFile a kliens által feltöltött fájl
     * @param prefix a tárolt fájlnévhez használt előtag
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public StoredFile store(UUID submissionId, MultipartFile multipartFile, String prefix) throws IOException {
        Path submissionDir = baseDirectory.resolve(submissionId.toString());
        ExceptionSafeOperations.createDirectories(submissionDir);
        String originalName = originalFileName(multipartFile);
        String serverExtension = "xml".equals(prefix) ? ".xml" : ".bin";
        Path target = submissionDir.resolve(prefix + "_" + UUID.randomUUID() + serverExtension).normalize();
        if (!target.startsWith(submissionDir)) {
            throw new IOException("Érvénytelen fájlnév: " + multipartFile.getOriginalFilename());
        }
        try (InputStream in = multipartFile.getInputStream();
             java.io.OutputStream out = SecureFileOperations.newPrivateOutputStream(target)) {
            in.transferTo(out);
        }
        String sha256;
        try (InputStream in = Files.newInputStream(target)) {
            sha256 = Sha256Util.sha256Hex(in);
        }
        return new StoredFile(originalName, target.toString(), Files.size(target), sha256);
    }


    /**
     * Az XML-fájl és csatolmány azonosítójához tartozó menedzselt könyvtárba menti a csatolmányt.
     *
     * @param xmlFileId az érintett XML-fájl adatbázis-azonosítója
     * @param attachmentId a cél csatolmány azonosítója
     * @param multipartFile a kliens által feltöltött fájl
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public StoredFile storeAttachment(Long xmlFileId, UUID attachmentId, MultipartFile multipartFile) throws IOException {
        if (xmlFileId == null) {
            return store(attachmentId, multipartFile, "attachment");
        }
        Path attachmentDir = baseDirectory.resolve("xml-files").resolve(String.valueOf(xmlFileId))
                .resolve("attachments").resolve(attachmentId.toString()).normalize();
        ExceptionSafeOperations.createDirectories(attachmentDir);
        String originalName = originalFileName(multipartFile);
        Path target = attachmentDir.resolve(attachmentId + ".bin").normalize();
        if (!target.startsWith(attachmentDir)) throw new IOException("Érvénytelen fájlnév: " + multipartFile.getOriginalFilename());
        try (InputStream in = multipartFile.getInputStream();
             java.io.OutputStream out = SecureFileOperations.newPrivateOutputStream(target)) {
            in.transferTo(out);
        }
        String sha256;
        try (InputStream in = Files.newInputStream(target)) { sha256 = Sha256Util.sha256Hex(in); }
        return new StoredFile(originalName, target.toString(), Files.size(target), sha256);
    }

    /**
     * A(z) {@code originalFileName} művelethez tartozó M2M feldolgozási lépést hajtja végre a típus felelősségi körében.
     *
     * @param multipartFile a kliens által feltöltött fájl
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public String originalFileName(MultipartFile multipartFile) throws IOException {
        String value = multipartFile == null ? null : multipartFile.getOriginalFilename();
        if (value == null || value.isBlank()) {
            throw new IOException("A csatolmány fájlneve hiányzik.");
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFC);
        if (normalized.contains("/") || normalized.contains("\\")
                || normalized.indexOf('\0') >= 0
                || normalized.equals(".") || normalized.equals("..")
                || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IOException("Érvénytelen csatolmány-fájlnév: " + value);
        }
        return normalized;
    }

    /**
     * A NAV M2M submitter modul {@code StoredFile} típusának felelősségét megvalósító típus.
     */
    /**
     * Létrehozza a(z) {@code StoredFile} példányt a működéshez szükséges függőségekkel vagy kezdeti állapottal.
     *
     * @param originalFileName a művelethez átadott {@code originalFileName} érték
     * @param storagePath a művelethez átadott {@code storagePath} érték
     * @param fileSize a művelethez átadott {@code fileSize} érték
     * @param sha256Hex a művelethez átadott {@code sha256Hex} érték
     */
    public record StoredFile(String originalFileName, String storagePath, long fileSize, String sha256Hex) {}
}
