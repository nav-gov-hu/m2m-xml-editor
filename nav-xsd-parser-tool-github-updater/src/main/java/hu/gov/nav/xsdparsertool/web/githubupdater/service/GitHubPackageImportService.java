package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;
import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.web.githubupdater.config.GitHubSchemaUpdaterProperties;
import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateRelease;
import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateRepository;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.PackageImportResponse;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.TagUpdateResult;
import hu.gov.nav.xsdparsertool.web.githubupdater.repo.GitHubTemplateReleaseRepository;
import hu.gov.nav.xsdparsertool.web.githubupdater.repo.GitHubTemplateRepositoryRepository;
import hu.gov.nav.xsdparsertool.web.githubupdater.support.RepositoryAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Több régi vagy content/ alapú repository ZIP lokális importját koordinálja. */
@Service
public class GitHubPackageImportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPackageImportService.class);
    private static final Pattern VERSIONED_NAME = Pattern.compile("^(.+?)[-_](v?\\d+(?:\\.\\d+){1,3}(?:[-+].*)?)$");
    private static final Pattern TARGET_NAMESPACE_VERSION = Pattern.compile("targetNamespace\\s*=\\s*[\"']([^\"']+/([0-9]+(?:\\.[0-9]+){1,3}))[\"']");

    private final GitHubSchemaUpdaterService updaterService;
    private final GitHubTemplateCatalogPersistenceService persistenceService;
    private final GitHubTemplateRepositoryRepository repositoryStore;
    private final GitHubTemplateReleaseRepository releaseStore;
    private final VersionTagComparator versionComparator;
    private final GitHubSchemaUpdaterProperties properties;

    public GitHubPackageImportService(GitHubSchemaUpdaterService updaterService,
                                      GitHubTemplateCatalogPersistenceService persistenceService,
                                      GitHubTemplateRepositoryRepository repositoryStore,
                                      GitHubTemplateReleaseRepository releaseStore,
                                      VersionTagComparator versionComparator,
                                      GitHubSchemaUpdaterProperties properties) {
        this.updaterService = updaterService;
        this.persistenceService = persistenceService;
        this.repositoryStore = repositoryStore;
        this.releaseStore = releaseStore;
        this.versionComparator = versionComparator;
        this.properties = properties;
    }

    public PackageImportResponse importPackages(List<MultipartFile> files, boolean force) {
        if (files == null || files.isEmpty()) throw new IllegalArgumentException("Legalább egy ZIP állomány kiválasztása szükséges.");
        List<PackageImportResponse.PackageImportItem> items = new ArrayList<>();
        int imported = 0, skipped = 0, failed = 0;
        for (MultipartFile file : files) {
            String fileName = file == null ? "" : file.getOriginalFilename();
            try {
                if (file == null || file.isEmpty()) throw new IllegalArgumentException("Az állomány üres.");
                if (!StringUtils.hasText(fileName) || !fileName.toLowerCase(Locale.ROOT).endsWith(".zip"))
                    throw new IllegalArgumentException("Csak ZIP állomány importálható.");
                Path tempRoot = updaterService.resolveTargetSchemaDir().resolve(properties.getTempDirectoryName());
                ExceptionSafeOperations.createDirectories(tempRoot);
                Path temp = tempRoot.resolve("upload-" + UUID.randomUUID() + ".zip");
                try (InputStream in = file.getInputStream()) { SecureFileOperations.copyPrivate(in, temp); }
                try {
                    PackageIdentity identity = identify(temp, fileName);
                    TagUpdateResult result = updaterService.importLocalArchive(temp, identity.repository(), identity.tag(), force);
                    if ("IMPORTED".equals(result.getStatus())) {
                        persistenceService.registerLocalRelease(identity.repository(), identity.tag(), repositoryUrl(identity.repository()));
                    }
                    if ("FAILED".equals(result.getStatus())) failed++;
                    else if ("SKIPPED".equals(result.getStatus())) skipped++;
                    else imported++;
                    items.add(new PackageImportResponse.PackageImportItem(fileName, identity.repository(), identity.tag(),
                            result.getStatus(), result.getMessage(), result.getInstalledArtifacts()));
                } finally { Files.deleteIfExists(temp); }
            } catch (Exception ex) {
                failed++;
                LOGGER.warn("Local package import failed for {}: {}", fileName, ex.getMessage());
                items.add(new PackageImportResponse.PackageImportItem(fileName, "", "", "FAILED", ex.getMessage(), List.of()));
            }
        }
        return new PackageImportResponse(files.size(), imported, skipped, failed, List.copyOf(items));
    }

    private PackageIdentity identify(Path zip, String fileName) throws IOException {
        try (ZipFile archive = new ZipFile(zip.toFile(), StandardCharsets.UTF_8)) {
            String root = archiveRootName(archive, fileName);
            List<String> knownRepositories = RepositoryAccess.findAll(repositoryStore).stream()
                    .map(GitHubTemplateRepository::getRepositoryName).sorted(Comparator.comparingInt(String::length).reversed()).toList();
            String repository = knownRepositories.stream().filter(name -> root.equals(name) || root.startsWith(name + "-") || root.startsWith(name + "_"))
                    .findFirst().orElseGet(() -> deriveRepository(root, fileName));
            String suffix = stripRepositoryPrefix(root, repository);
            String tag = isBranchName(suffix) ? resolveTagFromCatalogAndXsd(archive, repository) : suffix;
            if (!StringUtils.hasText(tag)) throw new IllegalArgumentException("A release tag/verzió nem azonosítható a ZIP-ből.");
            return new PackageIdentity(repository, tag);
        }
    }

    private String archiveRootName(ZipFile archive, String fileName) {
        String commonRoot = null;
        boolean sharedRoot = true;
        boolean anyEntry = false;
        for (ZipEntry entry : archive.stream().toList()) {
            String name = entry.getName() == null ? "" : entry.getName().replace('\\', '/');
            while (name.startsWith("/")) name = name.substring(1);
            if (!StringUtils.hasText(name)) continue;
            anyEntry = true;
            int slash = name.indexOf('/');
            if (slash <= 0) {
                sharedRoot = false;
                continue;
            }
            String top = name.substring(0, slash);
            if (commonRoot == null) commonRoot = top;
            else if (!commonRoot.equals(top)) sharedRoot = false;
        }
        if (!anyEntry) throw new IllegalArgumentException("A ZIP nem tartalmaz feldolgozható állományt.");
        if (sharedRoot && StringUtils.hasText(commonRoot)) return commonRoot;
        return fileName.replaceFirst("(?i)\\.zip$", "");
    }

    private String deriveRepository(String root, String fileName) {
        for (String candidate : List.of(root, fileName.replaceFirst("(?i)\\.zip$", ""))) {
            if (candidate.endsWith("-main") || candidate.endsWith("-master")) return candidate.substring(0, candidate.lastIndexOf('-'));
            Matcher matcher = VERSIONED_NAME.matcher(candidate);
            if (matcher.matches()) return matcher.group(1);
        }
        throw new IllegalArgumentException("A repository neve nem azonosítható. Használjon <repository>-<releaseTag>.zip nevű release csomagot.");
    }

    private String stripRepositoryPrefix(String root, String repository) {
        if (root.equals(repository)) return "";
        if (root.startsWith(repository + "-") || root.startsWith(repository + "_")) return root.substring(repository.length() + 1);
        Matcher matcher = VERSIONED_NAME.matcher(root);
        return matcher.matches() ? matcher.group(2) : "";
    }

    private boolean isBranchName(String value) {
        return !StringUtils.hasText(value) || "main".equalsIgnoreCase(value) || "master".equalsIgnoreCase(value);
    }

    private String resolveTagFromCatalogAndXsd(ZipFile archive, String repository) throws IOException {
        String xsdVersion = findXsdVersion(archive, repository);
        List<String> candidates = releaseStore.findByRepositoryNameOrderByReleaseTagAsc(repository).stream()
                .map(GitHubTemplateRelease::getReleaseTag)
                .filter(tag -> !StringUtils.hasText(xsdVersion) || normalizedVersion(tag).startsWith(xsdVersion))
                .sorted(versionComparator.reversed()).toList();
        if (!candidates.isEmpty()) return candidates.get(0);
        throw new IllegalArgumentException("A main/master ZIP release-patch verziója nem állapítható meg. Frissítse előbb a Catalog katalógust, vagy importáljon tag alapján készült ZIP-et.");
    }

    private String findXsdVersion(ZipFile archive, String repository) throws IOException {
        for (ZipEntry entry : archive.stream().filter(e -> !e.isDirectory() && e.getName().toLowerCase(Locale.ROOT).endsWith(".xsd")).toList()) {
            if (entry.getSize() > 32L * 1024 * 1024) continue;
            try (InputStream in = archive.getInputStream(entry)) {
                String text = new String(in.readNBytes(128 * 1024), StandardCharsets.UTF_8);
                Matcher matcher = TARGET_NAMESPACE_VERSION.matcher(text);
                if (matcher.find()) return matcher.group(2);
            }
        }
        return "";
    }

    private String normalizedVersion(String tag) { return tag == null ? "" : tag.replaceFirst("(?i)^(release[-_/]?|v)", ""); }
    private String repositoryUrl(String repository) { return "https://github.com/" + properties.getOrganization() + "/" + repository; }
    private record PackageIdentity(String repository, String tag) {}
}
