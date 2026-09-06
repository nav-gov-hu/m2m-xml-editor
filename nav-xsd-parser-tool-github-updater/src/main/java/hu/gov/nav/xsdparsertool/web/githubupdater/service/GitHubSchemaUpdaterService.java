package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.githubupdater.config.GitHubSchemaUpdaterProperties;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubSchemaUpdateRequest;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubSchemaUpdateResponse;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.InstalledArtifactResult;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.RepositoryUpdateResult;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.TagUpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A GitHub repository-k release-einek letöltését, biztonságos kicsomagolását és típus szerinti telepítését koordináló szolgáltatás. A normál Űrlapsablon repository-k mellett külön kezeli a közvetlenül legfrissebbként telepítendő technikai repository-kat.
 */
@Service
public class GitHubSchemaUpdaterService {
    private static final int MAX_ZIP_ENTRY_COUNT = 20000;
    private static final long MAX_ZIP_ENTRY_BYTES = 256L * 1024 * 1024;
    private static final long MAX_ZIP_TOTAL_BYTES = 2L * 1024 * 1024 * 1024;
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubSchemaUpdaterService.class);
    private static final String SCHEMA_DIR_PROPERTY = "nav.xsdparsertool.paths.schema-dir";
    private static final String XPATH_RULE_DIR_PROPERTY = "nav.xsdparsertool.xpath-validator.rule-root-dir";
    private static final String XSL_ROOT_DIR_PROPERTY = "nav.xsdparsertool.xpath-validator.xsl-root-dir";
    private static final String UI_MODEL_DIR_PROPERTY = "nav.xsdparsertool.paths.ui-model-dir";
    private static final String COMMON_XSD_DIR_PROPERTY = "nav.xsdparsertool.paths.common-xsd-dir";
    private static final String INSTALLATION_MARKER_FILE = ".m2m-installation-complete";
    private static final String FULL_CHECK_CORE_PUBLIC_XSL = "full_check_core_public.xsl";

    private final GitHubSchemaUpdaterProperties properties;
    private final GitHubApiClient gitHubApiClient;
    private final VersionTagComparator versionTagComparator;
    private final Environment environment;
    private final ReentrantLock updateLock = new ReentrantLock();

    /**
     * Létrehozza a(z) {@code GitHubSchemaUpdaterService} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     *
     * @param properties a művelethez átadott {@code properties} érték
     * @param gitHubApiClient a művelethez átadott {@code gitHubApiClient} érték
     * @param versionTagComparator a művelethez átadott {@code versionTagComparator} érték
     * @param environment a művelethez átadott {@code environment} érték
     */
    public GitHubSchemaUpdaterService(GitHubSchemaUpdaterProperties properties,
                                      GitHubApiClient gitHubApiClient,
                                      VersionTagComparator versionTagComparator,
                                      Environment environment) {
        this.properties = properties;
        this.gitHubApiClient = gitHubApiClient;
        this.versionTagComparator = versionTagComparator;
        this.environment = environment;
    }

    /**
     * A teljes frissítési folyamat szinkron belépési pontja. Ellenőrzi az updater engedélyezettségét, egy folyamaton belüli lockkal kizárja a párhuzamos futást, majd összesített eredményt ad.
     *
     * @param request a végrehajtandó frissítés vagy letöltés paraméterei
     * @return a teljes frissítési futás összesített eredménye
     */
    public GitHubSchemaUpdateResponse updateSchemas(GitHubSchemaUpdateRequest request) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("GitHub schema updater is disabled by configuration.");
        }
        if (!updateLock.tryLock()) {
            throw new IllegalStateException("A GitHub schema update is already running.");
        }
        try {
            return doUpdate(request == null ? new GitHubSchemaUpdateRequest() : request);
        } finally {
            updateLock.unlock();
        }
    }

    /**
     * Feloldja a release-archívumok alapértelmezett célkönyvtárát. Elsőként a modul saját property-jét használja, ennek hiányában a központi schema-dir konfiguráció kötelező értékére esik vissza.
     *
     * @return az abszolút, normalizált célkönyvtár
     */
    public Path resolveTargetSchemaDir() {
        if (properties.getTargetSchemaDir() != null) {
            return properties.getTargetSchemaDir().toAbsolutePath().normalize();
        }
        String configured = environment.getProperty(SCHEMA_DIR_PROPERTY);
        if (!StringUtils.hasText(configured)) {
            throw new IllegalStateException("Missing required configuration: " + SCHEMA_DIR_PROPERTY);
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    /**
     * Felépíti egy frissítési futás válaszát, feloldja a repository-listát, sorban feldolgozza a repository-kat és összegzi a letöltött, kihagyott és hibás tételeket. Egy felső szintű hiba is strukturált eredményként kerül a válaszba.
     *
     * @param request a végrehajtandó frissítés vagy letöltés paraméterei
     * @return a művelet eredménye
     */
    private GitHubSchemaUpdateResponse doUpdate(GitHubSchemaUpdateRequest request) {
        Instant startedAt = Instant.now();
        Path targetSchemaDir = resolveTargetSchemaDir();
        GitHubSchemaUpdateResponse response = new GitHubSchemaUpdateResponse();
        response.setStartedAt(startedAt);
        response.setOrganization(properties.getOrganization());
        response.setTargetSchemaDir(targetSchemaDir.toString());
        response.setDryRun(request.isDryRun());

        try {
            if (!request.isDryRun()) {
                ExceptionSafeOperations.createDirectories(targetSchemaDir);
            }
            List<String> repositories = resolveRepositories(request);
            response.setRepositoryCount(repositories.size());
            for (String repository : repositories) {
                RepositoryUpdateResult repoResult = updateRepository(repository, targetSchemaDir, request);
                response.getRepositories().add(repoResult);
                response.setDownloadedCount(response.getDownloadedCount() + repoResult.getDownloadedCount());
                response.setSkippedCount(response.getSkippedCount() + repoResult.getSkippedCount());
                response.setFailedCount(response.getFailedCount() + repoResult.getFailedCount());
            }
        } catch (Exception ex) {
            response.setFailedCount(response.getFailedCount() + 1);
            RepositoryUpdateResult error = new RepositoryUpdateResult();
            error.setRepositoryName("<organization>");
            error.setFailedCount(1);
            error.getTags().add(new TagUpdateResult("<repositories>", "FAILED", null, ex.getMessage()));
            response.getRepositories().add(error);
            LOGGER.error("GitHub schema update failed.", ex);
        } finally {
            response.setFinishedAt(Instant.now());
        }
        return response;
    }

    /**
     * A feldolgozandó repository-kat prioritási sorrendben választja: explicit repository→tag térkép, explicit repository-lista, konfigurált lista, végül GitHub organization-lekérdezés. Ezután alkalmazza a repository névprefix-szűrést a technikai direkt repository-k kivételével.
     *
     * @param request a végrehajtandó frissítés vagy letöltés paraméterei
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     * @throws InterruptedException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private List<String> resolveRepositories(GitHubSchemaUpdateRequest request) throws IOException, InterruptedException {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (!request.getRepositoryTags().isEmpty()) {
            result.addAll(request.getRepositoryTags().keySet());
        } else if (!request.getRepositories().isEmpty()) {
            result.addAll(request.getRepositories());
        } else if (!properties.getRepositories().isEmpty()) {
            result.addAll(properties.getRepositories());
        } else {
            result.addAll(gitHubApiClient.listOrganizationRepositories());
        }
        String prefix = properties.getRepositoryNamePrefix();
        if (StringUtils.hasText(prefix)) {
            String expected = prefix.toLowerCase(Locale.ROOT);
            result.removeIf(repo -> repo == null
                    || (!repo.toLowerCase(Locale.ROOT).startsWith(expected) && !isDirectLatestRepository(repo)));
        } else {
            result.removeIf(repo -> !StringUtils.hasText(repo));
        }
        return new ArrayList<>(result);
    }

    /**
     * Egy normál repository összes kijelölt/távoli tagját feldolgozza, meghatározza a lokálisan legmagasabb verziót, alkalmazza a skip/force szabályokat, majd letölt, kicsomagol és típus szerint telepít.
     *
     * @param repositoryName a GitHub repository neve
     * @param targetSchemaDir a séma-release-ek alap célkönyvtára
     * @param request a végrehajtandó frissítés vagy letöltés paraméterei
     * @return a művelet eredménye
     */
    private RepositoryUpdateResult updateRepository(String repositoryName, Path targetSchemaDir, GitHubSchemaUpdateRequest request) {
        RepositoryUpdateResult result = new RepositoryUpdateResult();
        result.setRepositoryName(repositoryName);
        if (isDirectLatestRepository(repositoryName)) {
            return updateDirectLatestRepository(repositoryName, targetSchemaDir, request, result);
        }
        Path repositoryDir = GitHubPathSafety.resolveInside(targetSchemaDir, repositoryName);
        if (!repositoryDir.startsWith(targetSchemaDir)) {
            throw new IllegalArgumentException("Unsafe repository path: " + repositoryName);
        }

        try {
            List<String> remoteTags;
            String selectedTag = request.getRepositoryTags().get(repositoryName);
            if (StringUtils.hasText(selectedTag)) {
                remoteTags = new ArrayList<>(List.of(selectedTag));
            } else {
                remoteTags = new ArrayList<>(gitHubApiClient.listRepositoryTags(repositoryName));
            }
            remoteTags.sort(versionTagComparator);
            result.setRemoteTagCount(remoteTags.size());
            List<String> localTags = listLocalTags(repositoryDir);
            String localHighestTag = localTags.stream().max(versionTagComparator).orElse(null);
            result.setLocalHighestTag(localHighestTag);

            for (String tagName : remoteTags) {
                Path tagTargetDir = GitHubPathSafety.resolveInside(repositoryDir, tagName);
                if (!tagTargetDir.startsWith(repositoryDir)) {
                    result.getTags().add(new TagUpdateResult(tagName, "FAILED", tagTargetDir.toString(), "Unsafe tag path."));
                    result.setFailedCount(result.getFailedCount() + 1);
                    continue;
                }
                if (shouldSkipTag(tagName, tagTargetDir, localHighestTag, request)) {
                    TagUpdateResult skipped = new TagUpdateResult(tagName, "SKIPPED", tagTargetDir.toString(),
                            "A release már helyben megtalálható; a típus szerinti telepítés ellenőrzése megtörtént.");
                    if (ExceptionSafeOperations.isDirectory(tagTargetDir) && !request.isDryRun()) {
                        List<InstalledArtifactResult> installedArtifacts = installReleaseArtifacts(
                                repositoryName, tagName, tagTargetDir, targetSchemaDir);
                        skipped.setInstalledArtifacts(installedArtifacts);
                        if (installedArtifacts.stream().noneMatch(artifact -> "FAILED".equals(artifact.getStatus()))) {
                            writeInstallationMarker(tagTargetDir, installedArtifacts);
                        }
                    }
                    result.getTags().add(skipped);
                    result.setSkippedCount(result.getSkippedCount() + 1);
                    continue;
                }
                if (request.isDryRun()) {
                    result.getTags().add(new TagUpdateResult(tagName, "WOULD_DOWNLOAD", tagTargetDir.toString(), "Dry run: archive was not downloaded."));
                    result.setSkippedCount(result.getSkippedCount() + 1);
                    continue;
                }
                try {
                    String downloadSource = downloadAndExtract(repositoryName, tagName, targetSchemaDir, tagTargetDir);
                    TagUpdateResult downloaded = new TagUpdateResult(tagName, "DOWNLOADED", tagTargetDir.toString(),
                            "Letöltve és kicsomagolva: " + downloadSource + ".");
                    List<InstalledArtifactResult> installedArtifacts = installReleaseArtifacts(
                            repositoryName, tagName, tagTargetDir, targetSchemaDir);
                    downloaded.setInstalledArtifacts(installedArtifacts);
                    boolean installationFailed = installedArtifacts.stream()
                            .anyMatch(artifact -> "FAILED".equals(artifact.getStatus()));
                    if (installationFailed) {
                        downloaded.setStatus("FAILED");
                        downloaded.setMessage("A release letöltődött, de legalább egy állomány telepítése vagy visszaellenőrzése sikertelen volt.");
                        result.setFailedCount(result.getFailedCount() + 1);
                    } else {
                        writeInstallationMarker(tagTargetDir, installedArtifacts);
                        result.setDownloadedCount(result.getDownloadedCount() + 1);
                    }
                    result.getTags().add(downloaded);
                } catch (Exception ex) {
                    result.getTags().add(new TagUpdateResult(tagName, "FAILED", tagTargetDir.toString(), ex.getMessage()));
                    result.setFailedCount(result.getFailedCount() + 1);
                    LOGGER.warn("Failed to download {} tag {}", repositoryName, tagName, ex);
                }
            }
        } catch (Exception ex) {
            result.getTags().add(new TagUpdateResult("<tags>", "FAILED", repositoryDir.toString(), ex.getMessage()));
            result.setFailedCount(result.getFailedCount() + 1);
            LOGGER.warn("Failed to update repository {}", repositoryName, ex);
        }
        return result;
    }


    /**
     * A közvetlenül legfrissebb release-ként kezelt technikai repository-t dolgozza fel. Verzióalmappa helyett a dedikált aktív célkönyvtár tartalmát szinkronizálja a kiválasztott legfrissebb taggal.
     *
     * @param repositoryName a GitHub repository neve
     * @param targetSchemaDir a séma-release-ek alap célkönyvtára
     * @param request a végrehajtandó frissítés vagy letöltés paraméterei
     * @param result a művelethez átadott {@code result} érték
     * @return a művelet eredménye
     */
    private RepositoryUpdateResult updateDirectLatestRepository(String repositoryName,
                                                                 Path targetSchemaDir,
                                                                 GitHubSchemaUpdateRequest request,
                                                                 RepositoryUpdateResult result) {
        Path targetDirectory = resolveDirectLatestRepositoryTarget(repositoryName);
        try {
            List<String> remoteTags = new ArrayList<>(gitHubApiClient.listRepositoryTags(repositoryName));
            remoteTags.sort(versionTagComparator);
            result.setRemoteTagCount(remoteTags.size());
            if (remoteTags.isEmpty()) {
                result.getTags().add(new TagUpdateResult("<latest>", "FAILED", targetDirectory.toString(),
                        "A repositoryhoz nem található release tag."));
                result.setFailedCount(1);
                return result;
            }

            String latestTag = remoteTags.get(remoteTags.size() - 1);
            if (request.isDryRun()) {
                result.getTags().add(new TagUpdateResult(latestTag, "WOULD_DOWNLOAD", targetDirectory.toString(),
                        "Dry run: a legfrissebb release közvetlen kicsomagolása nem történt meg."));
                result.setSkippedCount(1);
                return result;
            }

            String downloadSource = downloadAndExtractDirect(repositoryName, latestTag, targetSchemaDir, targetDirectory);
            long installedFileCount = countRegularFiles(targetDirectory);
            if (installedFileCount == 0) {
                throw new IOException("A release kibontása után a célkönyvtár nem tartalmaz állományt: " + targetDirectory);
            }
            TagUpdateResult downloaded = new TagUpdateResult(latestTag, "DOWNLOADED", targetDirectory.toString(),
                    "A legfrissebb release közvetlenül kicsomagolva: " + downloadSource
                            + ". Verziózott almappa és telepítési marker nem készült.");
            downloaded.setInstalledArtifacts(List.of(new InstalledArtifactResult(
                    "DIRECT_RELEASE",
                    repositoryName + "@" + latestTag,
                    targetDirectory.toString(),
                    "INSTALLED",
                    "A teljes release közvetlen kibontása sikeres. Telepített állományok száma: " + installedFileCount)));
            result.getTags().add(downloaded);
            result.setDownloadedCount(1);
            result.setLocalHighestTag(latestTag);
            LOGGER.info("Installed latest direct GitHub repository release without version directory or marker: repository={}, tag={}, target={}",
                    repositoryName, latestTag, targetDirectory);
        } catch (Exception ex) {
            result.getTags().add(new TagUpdateResult("<latest>", "FAILED", targetDirectory.toString(), ex.getMessage()));
            result.setFailedCount(result.getFailedCount() + 1);
            LOGGER.warn("Failed to install latest direct repository {}", repositoryName, ex);
        }
        return result;
    }

    /**
     * Megszámolja a vizsgált fájlrendszeri vagy katalóguselemek közül a feltételnek megfelelő tételeket.
     *
     * @param directory a vizsgált könyvtár
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private long countRegularFiles(Path directory) throws IOException {
        if (!ExceptionSafeOperations.isDirectory(directory)) {
            return 0L;
        }
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    /**
     * A bemenet és a konfiguráció alapján feloldja a művelethez szükséges konkrét erőforrást, útvonalat vagy értéket.
     *
     * @param repositoryName a GitHub repository neve
     * @return a művelet eredménye
     */
    private Path resolveDirectLatestRepositoryTarget(String repositoryName) {
        if (isFullCheckCorePublicRepository(repositoryName)) {
            return configuredDirectory(XSL_ROOT_DIR_PROPERTY);
        }
        if (isCommonRepository(repositoryName)) {
            return configuredDirectory(COMMON_XSD_DIR_PROPERTY);
        }
        throw new IllegalArgumentException("Nem támogatott közvetlen repository: " + repositoryName);
    }

    /**
     * Visszaadja a(z) directLatestRepository aktuális értékét.
     *
     * @param repositoryName a GitHub repository neve
     * @return a(z) directLatestRepository érték
     */
    private boolean isDirectLatestRepository(String repositoryName) {
        return isFullCheckCorePublicRepository(repositoryName) || isCommonRepository(repositoryName);
    }

    /**
     * Visszaadja a(z) fullCheckCorePublicRepository aktuális értékét.
     *
     * @param repositoryName a GitHub repository neve
     * @return a(z) fullCheckCorePublicRepository érték
     */
    private boolean isFullCheckCorePublicRepository(String repositoryName) {
        if (!StringUtils.hasText(repositoryName)) {
            return false;
        }
        String normalized = repositoryName.replaceFirst("(?i)^nav[-_]", "").trim();
        return "full_check_core_public".equalsIgnoreCase(normalized)
                || "full-check-core-public".equalsIgnoreCase(normalized);
    }

    /**
     * Bejárja a kicsomagolt release reguláris fájljait, típus szerint osztályozza őket, feloldja a végleges XSD/UIModel/XPath/common célhelyet, majd másolással és visszaellenőrzéssel telepíti az artefaktumokat. Az egyedi fájlhibákat tételszintű eredményként gyűjti.
     *
     * @param repositoryName a GitHub repository neve
     * @param tagName a release tag neve
     * @param releaseRoot a művelethez átadott {@code releaseRoot} érték
     * @param schemaRoot a művelethez átadott {@code schemaRoot} érték
     * @return a művelet eredménye
     */
    private List<InstalledArtifactResult> installReleaseArtifacts(String repositoryName,
                                                                   String tagName,
                                                                   Path releaseRoot,
                                                                   Path schemaRoot) {
        List<InstalledArtifactResult> results = new ArrayList<>();
        String formType = normalizeFormType(repositoryName);
        String version = normalizeVersion(tagName);
        boolean commonRepository = isCommonRepository(repositoryName);
        try {
            List<Path> releaseFiles;
            try (Stream<Path> files = Files.walk(releaseRoot)) {
                releaseFiles = files.filter(Files::isRegularFile).toList();
            }
            for (Path source : releaseFiles) {
                ArtifactType artifactType = classifyArtifact(releaseRoot, source);
                if (artifactType == ArtifactType.ARCHIVE_ONLY) {
                    continue;
                }
                try {
                    Path target = resolveArtifactTarget(
                            artifactType, formType, version, releaseRoot, source, schemaRoot, commonRepository);
                    long expectedSize = Files.size(source);
                    installAndVerifyArtifact(source, target, expectedSize);
                    if (artifactType != ArtifactType.XSD && !source.equals(target)) {
                        Files.deleteIfExists(source);
                        deleteEmptyParentDirectories(source.getParent(), releaseRoot);
                    }
                    results.add(new InstalledArtifactResult(artifactType.label, source.toString(), target.toString(),
                            "INSTALLED", "Az állomány telepítése és lemezes visszaellenőrzése sikeres."));
                    LOGGER.info("Installed and verified GitHub template artifact: type={}, source={}, target={}, size={}",
                            artifactType.label, source, target, expectedSize);
                } catch (Exception ex) {
                    results.add(new InstalledArtifactResult(artifactType.label, source.toString(), null,
                            "FAILED", ex.getMessage()));
                    LOGGER.warn("Failed to install or verify GitHub template artifact {} from {}/{}",
                            source, repositoryName, tagName, ex);
                }
            }
        } catch (IOException ex) {
            results.add(new InstalledArtifactResult("RELEASE", releaseRoot.toString(), null, "FAILED", ex.getMessage()));
        }
        return results;
    }



    /**
     * Egy repository/release lokális telepítését eltávolítja az archive, XSD, XPath és UIModel célhelyekről.
     * A törlés kizárólag konfigurált gyökérkönyvtárakon belül történhet.
     *
     * @param repositoryName a GitHub repository neve
     * @param tagName a release tag neve
     * @return a törölt fájl- és könyvtárbejegyzések száma
     */
    public int deleteLocalRelease(String repositoryName, String tagName) {
        String repository = GitHubPathSafety.safeSegment(repositoryName);
        String tag = GitHubPathSafety.safeSegment(tagName);
        String version = normalizeVersion(tagName);
        int deleted = 0;
        Path schemaRoot = resolveTargetSchemaDir();

        if (isFullCheckCorePublicRepository(repositoryName)) {
            Path xslRoot = configuredDirectory(XSL_ROOT_DIR_PROPERTY);
            deleted += deleteRegularFileInside(xslRoot, xslRoot.resolve(FULL_CHECK_CORE_PUBLIC_XSL));
            return deleted;
        }
        if (isCommonRepository(repositoryName)) {
            Path commonRoot = configuredDirectory(COMMON_XSD_DIR_PROPERTY);
            deleted += deleteDirectoryContentsInside(commonRoot, commonRoot);
            return deleted;
        }

        Path releaseRoot = GitHubPathSafety.resolveInside(schemaRoot, repository, tag);
        deleted += deleteInstalledTargetsFromMarker(releaseRoot, allowedArtifactRoots(schemaRoot));

        String formType = normalizeFormType(repositoryName);
        deleted += deleteDirectoryTreeInside(schemaRoot, GitHubPathSafety.resolveInside(schemaRoot, formType, version));
        Path xpathRoot = configuredDirectory(XPATH_RULE_DIR_PROPERTY);
        deleted += deleteDirectoryTreeInside(xpathRoot, GitHubPathSafety.resolveInside(xpathRoot, formType, version));
        Path uiModelRoot = configuredDirectory(UI_MODEL_DIR_PROPERTY);
        deleted += deleteDirectoryTreeInside(uiModelRoot, GitHubPathSafety.resolveInside(uiModelRoot, formType, version));
        deleted += deleteDirectoryTreeInside(schemaRoot, releaseRoot);

        Path repositoryRoot = GitHubPathSafety.resolveInside(schemaRoot, repository);
        try { deleteEmptyParentDirectories(repositoryRoot, schemaRoot); }
        catch (IOException ex) { LOGGER.debug("A kiürült repository-könyvtár nem törölhető: {}", repositoryRoot, ex); }
        return deleted;
    }

    /** A telepítési markerből visszaolvassa és biztonságosan törli a korábban telepített célfájlokat. */
    private int deleteInstalledTargetsFromMarker(Path releaseRoot, List<Path> allowedRoots) {
        Path marker = releaseRoot.resolve(INSTALLATION_MARKER_FILE).toAbsolutePath().normalize();
        if (!ExceptionSafeOperations.isRegularFile(marker)) return 0;
        int deleted = 0;
        try {
            for (String line : Files.readAllLines(marker, StandardCharsets.UTF_8)) {
                int separator = line.indexOf('=');
                if (separator <= 0 || line.startsWith("installedAt=")) continue;
                String rawPath = line.substring(separator + 1).trim();
                if (!StringUtils.hasText(rawPath)) continue;
                Path target = Path.of(rawPath).toAbsolutePath().normalize();
                Path root = allowedRoots.stream().filter(target::startsWith).findFirst().orElse(null);
                if (root == null) {
                    LOGGER.warn("Telepítési marker tiltott célútvonalát nem töröljük: {}", target);
                    continue;
                }
                deleted += deleteRegularFileInside(root, target);
                try { deleteEmptyParentDirectories(target.getParent(), root); } catch (IOException ignored) { }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("A GitHub telepítési marker nem olvasható: " + marker, ex);
        }
        return deleted;
    }

    /** Visszaadja a lokális GitHub artefaktumok megengedett gyökérkönyvtárait. */
    private List<Path> allowedArtifactRoots(Path schemaRoot) {
        List<Path> roots = new ArrayList<>();
        roots.add(schemaRoot.toAbsolutePath().normalize());
        roots.add(configuredDirectory(XPATH_RULE_DIR_PROPERTY));
        roots.add(configuredDirectory(UI_MODEL_DIR_PROPERTY));
        roots.add(configuredDirectory(COMMON_XSD_DIR_PROPERTY));
        roots.add(configuredDirectory(XSL_ROOT_DIR_PROPERTY));
        return roots;
    }

    /** Egy reguláris fájlt kizárólag a megadott megbízható gyökér alatt töröl. */
    private int deleteRegularFileInside(Path trustedRoot, Path target) {
        Path root = trustedRoot.toAbsolutePath().normalize();
        Path normalized = target.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) throw new IllegalArgumentException("Tiltott törlési útvonal: " + normalized);
        try { return Files.deleteIfExists(normalized) ? 1 : 0; }
        catch (IOException ex) { throw new IllegalStateException("A lokális GitHub állomány nem törölhető: " + normalized, ex); }
    }

    /** Egy könyvtárfát kizárólag a megadott megbízható gyökér alatt töröl. */
    private int deleteDirectoryTreeInside(Path trustedRoot, Path target) {
        Path root = trustedRoot.toAbsolutePath().normalize();
        Path normalized = target.toAbsolutePath().normalize();
        if (!normalized.startsWith(root) || normalized.equals(root) || !Files.exists(normalized)) return 0;
        try (Stream<Path> paths = Files.walk(normalized)) {
            List<Path> ordered = paths.sorted(Comparator.reverseOrder()).toList();
            int deleted = 0;
            for (Path path : ordered) if (Files.deleteIfExists(path)) deleted++;
            return deleted;
        } catch (IOException ex) {
            throw new IllegalStateException("A lokális GitHub könyvtár nem törölhető: " + normalized, ex);
        }
    }

    /** Egy technikai repository aktív könyvtárának tartalmát törli, magát a konfigurált gyökeret megtartva. */
    private int deleteDirectoryContentsInside(Path trustedRoot, Path directory) {
        Path root = trustedRoot.toAbsolutePath().normalize();
        Path normalized = directory.toAbsolutePath().normalize();
        if (!normalized.equals(root)) throw new IllegalArgumentException("Tiltott technikai repository törlési útvonal.");
        if (!Files.isDirectory(normalized)) return 0;
        try (Stream<Path> children = Files.list(normalized)) {
            int deleted = 0;
            for (Path child : children.toList()) {
                if (Files.isDirectory(child)) deleted += deleteDirectoryTreeInside(root, child);
                else deleted += deleteRegularFileInside(root, child);
            }
            return deleted;
        } catch (IOException ex) {
            throw new IllegalStateException("A technikai repository könyvtár nem üríthető: " + normalized, ex);
        }
    }

    /**
     * Biztonságosan létrehozza a cél szülőkönyvtárát, lecseréli a célfájlt a release artefaktumára, majd méret és létezés alapján azonnal visszaellenőrzi a telepítést.
     *
     * @param source a feldolgozandó forrásobjektum vagy fájl
     * @param target a művelethez átadott {@code target} érték
     * @param expectedSize a művelethez átadott {@code expectedSize} érték
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void installAndVerifyArtifact(Path source, Path target, long expectedSize) throws IOException {
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        ExceptionSafeOperations.createDirectories(normalizedTarget.getParent());

        if (!normalizedSource.equals(normalizedTarget)) {
            Path tempTarget = normalizedTarget.resolveSibling(
                    normalizedTarget.getFileName() + ".installing-" + UUID.randomUUID());
            try {
                SecureFileOperations.copyPrivate(normalizedSource, tempTarget, StandardCopyOption.REPLACE_EXISTING);
                verifyInstalledFile(tempTarget, expectedSize);
                try {
                    SecureFileOperations.movePrivate(tempTarget, normalizedTarget,
                            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException atomicMoveFailed) {
                    SecureFileOperations.movePrivate(tempTarget, normalizedTarget, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tempTarget);
            }
        }
        verifyInstalledFile(normalizedTarget, expectedSize);
    }

    /**
     * Ellenőrzi, hogy a telepített cél valóban reguláris fájl-e és a mérete megegyezik-e a forrásnál mért elvárt mérettel; eltérés esetén a telepítést hibásnak tekinti.
     *
     * @param target a művelethez átadott {@code target} érték
     * @param expectedSize a művelethez átadott {@code expectedSize} érték
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void verifyInstalledFile(Path target, long expectedSize) throws IOException {
        if (!ExceptionSafeOperations.isRegularFile(target)) {
            throw new IOException("A telepített állomány nem található a lemezen: " + target);
        }
        long actualSize = Files.size(target);
        if (actualSize != expectedSize) {
            throw new IOException("A telepített állomány mérete eltér. target=" + target
                    + ", expectedSize=" + expectedSize + ", actualSize=" + actualSize);
        }
        try (InputStream input = Files.newInputStream(target, StandardOpenOption.READ)) {
            // Egy tényleges olvasási művelet ellenőrzi, hogy az állomány nem csak megnyitható, hanem olvasható is.
            input.read();
        }
    }

    /**
     * Eltávolítja a megadott elemet a modul által kezelt perzisztens vagy fájlrendszeri állapotból.
     *
     * @param directory a vizsgált könyvtár
     * @param stopExclusive a művelethez átadott {@code stopExclusive} érték
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void deleteEmptyParentDirectories(Path directory, Path stopExclusive) throws IOException {
        Path current = directory;
        while (current != null && !current.equals(stopExclusive) && current.startsWith(stopExclusive)) {
            try (Stream<Path> children = Files.list(current)) {
                if (children.findAny().isPresent()) return;
            }
            Files.deleteIfExists(current);
            current = current.getParent();
        }
    }

    /**
     * A release-en belüli fájlt kiterjesztése és könyvtári kontextusa alapján XSD, UIModel, XPath/XSL vagy egyéb telepítési kategóriába sorolja.
     *
     * @param releaseRoot a művelethez átadott {@code releaseRoot} érték
     * @param source a feldolgozandó forrásobjektum vagy fájl
     * @return a művelet eredménye
     */
    private ArtifactType classifyArtifact(Path releaseRoot, Path source) {
        String relative = releaseRoot.relativize(source).toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        String fileName = source.getFileName().toString().toLowerCase(Locale.ROOT);
        if ((relative.contains("uimodel") || relative.contains("ui-model") || fileName.contains("uimodel") || fileName.contains("ui_model"))
                && (fileName.endsWith(".xml") || fileName.endsWith(".json"))) {
            return ArtifactType.UI_MODEL;
        }
        if ((relative.contains("xpath") || fileName.contains("xpath")) && fileName.endsWith(".xml")) {
            return ArtifactType.XPATH;
        }
        if (fileName.endsWith(".xsd")) {
            return ArtifactType.XSD;
        }
        return ArtifactType.ARCHIVE_ONLY;
    }

    /**
     * Az artefaktum típusából, űrlaptípusából és verziójából meghatározza a végleges konfigurált célkönyvtárat és relatív fájlutat. A common és technikai repository-k eltérő elhelyezési szabályait is kezeli.
     *
     * @param type a művelethez átadott {@code type} érték
     * @param formType a művelethez átadott {@code formType} érték
     * @param version a művelethez átadott {@code version} érték
     * @param releaseRoot a művelethez átadott {@code releaseRoot} érték
     * @param source a feldolgozandó forrásobjektum vagy fájl
     * @param schemaRoot a művelethez átadott {@code schemaRoot} érték
     * @param commonRepository a művelethez átadott {@code commonRepository} érték
     * @return a művelet eredménye
     */
    private Path resolveArtifactTarget(ArtifactType type,
                                       String formType,
                                       String version,
                                       Path releaseRoot,
                                       Path source,
                                       Path schemaRoot,
                                       boolean commonRepository) {
        Path root;
        Path relativeTarget;
        if (commonRepository) {
            if (type != ArtifactType.XSD) {
                throw new IllegalArgumentException("A common repositoryban csak XSD állomány telepíthető automatikusan: " + source);
            }
            root = configuredDirectory(COMMON_XSD_DIR_PROPERTY);
            relativeTarget = artifactRelativePath(releaseRoot, source, "xsd", "schema", "schemas", "common");
            return GitHubPathSafety.resolveRelativeInside(root, relativeTarget);
        }
        if (type == ArtifactType.XPATH) {
            root = configuredDirectory(XPATH_RULE_DIR_PROPERTY);
            relativeTarget = artifactRelativePath(releaseRoot, source, "xpath");
        } else if (type == ArtifactType.UI_MODEL) {
            root = configuredDirectory(UI_MODEL_DIR_PROPERTY);
            relativeTarget = artifactRelativePath(releaseRoot, source, "uimodel", "ui-model");
        } else {
            root = schemaRoot;
            relativeTarget = artifactRelativePath(releaseRoot, source, "xsd", "schema", "schemas");
        }
        Path releaseTargetRoot = GitHubPathSafety.resolveInside(root, formType, version);
        return GitHubPathSafety.resolveRelativeInside(releaseTargetRoot, relativeTarget);
    }

    /**
     * A release-en belüli forrásútból a megadott marker könyvtárak egyikétől kezdődő relatív artefaktumutat képezi. Ha nincs marker, a release gyökeréhez viszonyított relatív út marad érvényben.
     *
     * @param releaseRoot a művelethez átadott {@code releaseRoot} érték
     * @param source a feldolgozandó forrásobjektum vagy fájl
     * @param markerDirectories a művelethez átadott {@code markerDirectories} érték
     * @return a művelet eredménye
     */
    private Path artifactRelativePath(Path releaseRoot, Path source, String... markerDirectories) {
        Path relative = releaseRoot.relativize(source).normalize();
        for (int i = 0; i < relative.getNameCount(); i++) {
            String segment = relative.getName(i).toString().toLowerCase(Locale.ROOT);
            for (String marker : markerDirectories) {
                if (segment.equals(marker) && i + 1 < relative.getNameCount()) {
                    return relative.subpath(i + 1, relative.getNameCount());
                }
            }
        }
        return Path.of(source.getFileName().toString());
    }

    /**
     * A felsorolt property-nevek közül az első kitöltött könyvtárkonfigurációt oldja fel abszolút, normalizált útvonallá; ha egyik sem létezik, konfigurációs hibát jelez.
     *
     * @param propertyNames a művelethez átadott {@code propertyNames} érték
     * @return a művelet eredménye
     */
    private Path configuredDirectory(String... propertyNames) {
        for (String propertyName : propertyNames) {
            String configured = environment.getProperty(propertyName);
            if (StringUtils.hasText(configured)) {
                return Path.of(configured).toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("Hiányzó célkönyvtár-konfiguráció: " + String.join(" vagy ", propertyNames));
    }


    /**
     * Visszaadja a(z) commonRepository aktuális értékét.
     *
     * @param repositoryName a GitHub repository neve
     * @return a(z) commonRepository érték
     */
    private boolean isCommonRepository(String repositoryName) {
        if (!StringUtils.hasText(repositoryName)) {
            return false;
        }
        String normalized = repositoryName.replaceFirst("(?i)^nav[-_]", "").trim();
        return "common".equalsIgnoreCase(normalized);
    }

    /**
     * A megadott értéket a modul belső összehasonlítási és elérési szabályainak megfelelő kanonikus formára alakítja.
     *
     * @param repositoryName a GitHub repository neve
     * @return a művelet eredménye
     */
    private String normalizeFormType(String repositoryName) {
        String normalized = repositoryName == null ? "" : repositoryName.trim();
        return GitHubPathSafety.safeSegment(normalized);
    }

    /**
     * A megadott értéket a modul belső összehasonlítási és elérési szabályainak megfelelő kanonikus formára alakítja.
     *
     * @param tagName a release tag neve
     * @return a művelet eredménye
     */
    private String normalizeVersion(String tagName) {
        String normalized = tagName == null ? "" : tagName.replaceFirst("(?i)^(release[-_/]?|v)", "").trim();
        return GitHubPathSafety.safeSegment(normalized);
    }

    /**
     * A release-archívumból felismert telepítendő artefaktum típusát jelölő belső felsorolás.
     */
    private enum ArtifactType {
        XSD("XSD"), XPATH("XPATH"), UI_MODEL("UIMODEL"), ARCHIVE_ONLY("ARCHIVE");
        private final String label;
        /**
         * Létrehozza a(z) {@code ArtifactType} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
         *
         * @param label a művelethez átadott {@code label} érték
         */
        ArtifactType(String label) { this.label = label; }
    }

    /**
     * Eldönti, hogy egy tag letöltése kihagyható-e. Figyelembe veszi a dry-run/force beállítást, a telepítési markert, a meglévő tagkönyvtárat és a verzió-összehasonlítást.
     *
     * @param tagName a release tag neve
     * @param tagTargetDir a művelethez átadott {@code tagTargetDir} érték
     * @param localHighestTag a művelethez átadott {@code localHighestTag} érték
     * @param request a végrehajtandó frissítés vagy letöltés paraméterei
     * @return a művelet eredménye
     */
    private boolean shouldSkipTag(String tagName, Path tagTargetDir, String localHighestTag, GitHubSchemaUpdateRequest request) {
        if (request.isForceDownloadAll()) {
            return false;
        }
        boolean existingInstallation = isInstalledTagDirectory(tagTargetDir);
        if (StringUtils.hasText(localHighestTag)
                && versionTagComparator.compare(tagName, localHighestTag) <= 0) {
            return existingInstallation;
        }
        return properties.isSkipExistingTagDirectories() && existingInstallation;
    }

    /**
     * Visszaadja a(z) installedTagDirectory aktuális értékét.
     *
     * @param tagTargetDir a művelethez átadott {@code tagTargetDir} érték
     * @return a(z) installedTagDirectory érték
     */
    private boolean isInstalledTagDirectory(Path tagTargetDir) {
        if (!ExceptionSafeOperations.isDirectory(tagTargetDir)) {
            return false;
        }
        if (ExceptionSafeOperations.isRegularFile(tagTargetDir.resolve(INSTALLATION_MARKER_FILE))) {
            return true;
        }
        try (Stream<Path> files = Files.walk(tagTargetDir)) {
            return files.filter(Files::isRegularFile)
                    .anyMatch(file -> classifyArtifact(tagTargetDir, file) != ArtifactType.ARCHIVE_ONLY);
        } catch (IOException ex) {
            LOGGER.warn("A helyi release könyvtár nem ellenőrizhető, ezért újra letöltjük: {}", tagTargetDir, ex);
            return false;
        }
    }

    /**
     * Sikeres típus szerinti telepítés után markerfájlt ír a release könyvtárba a telepített artefaktumok eredményeivel; ez a későbbi skip-döntés egyik bemenete.
     *
     * @param tagTargetDir a művelethez átadott {@code tagTargetDir} érték
     * @param installedArtifacts a művelethez átadott {@code installedArtifacts} érték
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void writeInstallationMarker(Path tagTargetDir,
                                         List<InstalledArtifactResult> installedArtifacts) throws IOException {
        if (installedArtifacts != null && installedArtifacts.size() > 10_000) {
            throw new IOException("Túl sok telepített artifact került egy release-be.");
        }
        List<String> lines = new ArrayList<>();
        lines.add("installedAt=" + Instant.now());
        if (installedArtifacts != null) {
            for (InstalledArtifactResult artifact : installedArtifacts) {
                if ("INSTALLED".equals(artifact.getStatus()) && StringUtils.hasText(artifact.getTargetFile())) {
                    String markerLine = artifact.getArtifactType() + "=" + artifact.getTargetFile();
                    if (markerLine.length() > 4096) throw new IOException("Túl hosszú installation marker bejegyzés.");
                    lines.add(markerLine);
                }
            }
        }
        if (lines.size() > 10_001) throw new IOException("Az installation marker túl nagy.");
        SecureFileOperations.writePrivateLines(tagTargetDir.resolve(INSTALLATION_MARKER_FILE), lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    /**
     * A repository lokális release-könyvtáraiból összegyűjti a már jelen lévő tagneveket; nem létező repository-könyvtár esetén üres listát ad.
     *
     * @param repositoryDir a művelethez átadott {@code repositoryDir} érték
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private List<String> listLocalTags(Path repositoryDir) throws IOException {
        if (!ExceptionSafeOperations.isDirectory(repositoryDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(repositoryDir)) {
            return stream.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> !name.equals(properties.getTempDirectoryName()))
                    .toList();
        }
    }


    /**
     * Ideiglenes ZIP-be letölti a technikai repository kijelölt tagját, biztonságosan kicsomagolja egy staging könyvtárba, majd a cél aktív könyvtárba szinkronizálja.
     *
     * @param repositoryName a GitHub repository neve
     * @param tagName a release tag neve
     * @param targetSchemaDir a séma-release-ek alap célkönyvtára
     * @param targetDirectory a célkönyvtár
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     * @throws InterruptedException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private String downloadAndExtractDirect(String repositoryName, String tagName, Path targetSchemaDir, Path targetDirectory)
            throws IOException, InterruptedException {
        Path tempRoot = targetSchemaDir.resolve(properties.getTempDirectoryName());
        ExceptionSafeOperations.createDirectories(tempRoot);
        Path workDir = tempRoot.resolve(GitHubPathSafety.safeSegment(repositoryName) + "-" + GitHubPathSafety.safeSegment(tagName) + "-direct-" + UUID.randomUUID());
        Path archive = workDir.resolve("archive.zip");
        Path extractDir = workDir.resolve("extract");
        try {
            ExceptionSafeOperations.createDirectories(extractDir);
            String downloadSource = gitHubApiClient.downloadArchive(repositoryName, tagName, archive);
            LOGGER.info("Downloaded direct GitHub archive for {}/{} using {}.", repositoryName, tagName, downloadSource);
            extractZipStrippingRoot(archive, extractDir);
            if (isFullCheckCorePublicRepository(repositoryName)) {
                installFullCheckCorePublicXsl(extractDir, targetDirectory);
            } else {
                synchronizeDirectRelease(extractDir, targetDirectory);
            }
            return downloadSource;
        } finally {
            deleteRecursively(workDir);
        }
    }


    /**
     * A full_check_core_public repository-ból megkeresi és a konfigurált aktív XSL célkönyvtárba telepíti a szükséges {@code full_check_core_public.xsl} állományt.
     *
     * @param sourceDirectory a forráskönyvtár
     * @param targetDirectory a célkönyvtár
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void installFullCheckCorePublicXsl(Path sourceDirectory, Path targetDirectory) throws IOException {
        List<Path> matches;
        try (Stream<Path> files = Files.walk(sourceDirectory)) {
            matches = files.filter(Files::isRegularFile)
                    .filter(path -> FULL_CHECK_CORE_PUBLIC_XSL.equalsIgnoreCase(path.getFileName().toString()))
                    .toList();
        }
        if (matches.isEmpty()) {
            throw new IOException("A full_check_core_public release nem tartalmazza a kötelező "
                    + FULL_CHECK_CORE_PUBLIC_XSL + " állományt.");
        }
        if (matches.size() > 1) {
            throw new IOException("A full_check_core_public release több " + FULL_CHECK_CORE_PUBLIC_XSL
                    + " állományt tartalmaz; a telepítés nem egyértelmű.");
        }
        Path source = matches.get(0);
        Path target = targetDirectory.resolve(FULL_CHECK_CORE_PUBLIC_XSL).normalize();
        if (!target.startsWith(targetDirectory)) {
            throw new IOException("Unsafe full_check_core_public target path: " + target);
        }
        long expectedSize = Files.size(source);
        installAndVerifyArtifact(source, target, expectedSize);
        LOGGER.info("Installed full_check_core_public XSL into configured XSL root: source={}, target={}, size={}",
                source, target, expectedSize);
    }

    /**
     * A staging könyvtár és az aktív célkönyvtár tartalmát fájlszinten szinkronizálja: frissíti a forrásban lévő fájlokat, a már nem létező célfájlokat törli, majd az üres könyvtárakat takarítja.
     *
     * @param sourceDirectory a forráskönyvtár
     * @param targetDirectory a célkönyvtár
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void synchronizeDirectRelease(Path sourceDirectory, Path targetDirectory) throws IOException {
        ExceptionSafeOperations.createDirectories(targetDirectory);
        Set<Path> expectedRelativeFiles = new HashSet<>();
        try (Stream<Path> sourceFiles = Files.walk(sourceDirectory)) {
            for (Path source : sourceFiles.filter(Files::isRegularFile).toList()) {
                Path relative = sourceDirectory.relativize(source);
                Path target = targetDirectory.resolve(relative).normalize();
                if (!target.startsWith(targetDirectory)) {
                    throw new IOException("Unsafe direct release target path: " + relative);
                }
                ExceptionSafeOperations.createDirectories(target.getParent());
                SecureFileOperations.copyPrivate(source, target, StandardCopyOption.REPLACE_EXISTING);
                expectedRelativeFiles.add(relative);
            }
        }
        if (expectedRelativeFiles.isEmpty()) {
            throw new IOException("A letöltött release archívum nem tartalmaz telepíthető állományt.");
        }

        // A célkönyvtár aktív használatban lehet Windows alatt. A már nem szükséges
        // fájlok törlése ezért best-effort; egy zárolt régi fájl nem akadályozhatja
        // az új release állományainak felülírását és használatát.
        try (Stream<Path> targetFiles = Files.walk(targetDirectory)) {
            for (Path target : targetFiles.filter(Files::isRegularFile).toList()) {
                Path relative = targetDirectory.relativize(target);
                if (!expectedRelativeFiles.contains(relative)) {
                    try {
                        Files.deleteIfExists(target);
                    } catch (IOException deleteFailure) {
                        LOGGER.warn("Could not remove stale direct release file {}; keeping it temporarily.", target, deleteFailure);
                    }
                }
            }
        }
        deleteEmptyDirectoriesBestEffort(targetDirectory);
    }

    /**
     * Eltávolítja a megadott elemet a modul által kezelt perzisztens vagy fájlrendszeri állapotból.
     *
     * @param root a művelet gyökérkönyvtára
     */
    private void deleteEmptyDirectoriesBestEffort(Path root) {
        try (Stream<Path> directories = Files.walk(root)) {
            List<Path> reverseOrder = directories.filter(Files::isDirectory)
                    .filter(path -> !path.equals(root))
                    .sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .toList();
            for (Path directory : reverseOrder) {
                try (Stream<Path> children = Files.list(directory)) {
                    if (children.findAny().isEmpty()) {
                        Files.deleteIfExists(directory);
                    }
                } catch (IOException cleanupFailure) {
                    LOGGER.debug("Could not remove empty direct release directory {}.", directory, cleanupFailure);
                }
            }
        } catch (IOException cleanupFailure) {
            LOGGER.debug("Could not inspect direct release target directory {} for cleanup.", root, cleanupFailure);
        }
    }

    /**
     * A normál repository release ZIP-jét ideiglenes fájlba tölti, biztonságosan kibontja a tag célkönyvtárába, majd az ideiglenes állományt eltávolítja.
     *
     * @param repositoryName a GitHub repository neve
     * @param tagName a release tag neve
     * @param targetSchemaDir a séma-release-ek alap célkönyvtára
     * @param tagTargetDir a művelethez átadott {@code tagTargetDir} érték
     * @return a művelet eredménye
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     * @throws InterruptedException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private String downloadAndExtract(String repositoryName, String tagName, Path targetSchemaDir, Path tagTargetDir) throws IOException, InterruptedException {
        Path tempRoot = targetSchemaDir.resolve(properties.getTempDirectoryName());
        ExceptionSafeOperations.createDirectories(tempRoot);
        Path workDir = tempRoot.resolve(GitHubPathSafety.safeSegment(repositoryName) + "-" + GitHubPathSafety.safeSegment(tagName) + "-" + UUID.randomUUID());
        Path archive = workDir.resolve("archive.zip");
        Path extractDir = workDir.resolve("extract");
        try {
            ExceptionSafeOperations.createDirectories(extractDir);
            String downloadSource = gitHubApiClient.downloadArchive(repositoryName, tagName, archive);
            LOGGER.info("Downloaded GitHub archive for {}/{} using {}.", repositoryName, tagName, downloadSource);
            extractZipStrippingRoot(archive, extractDir);
            if (ExceptionSafeOperations.fileExists(tagTargetDir)) {
                deleteRecursively(tagTargetDir);
            }
            ExceptionSafeOperations.createDirectories(tagTargetDir.getParent());
            try {
                SecureFileOperations.movePrivate(extractDir, tagTargetDir, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailed) {
                SecureFileOperations.movePrivate(extractDir, tagTargetDir, StandardCopyOption.REPLACE_EXISTING);
            }
            return downloadSource;
        } finally {
            deleteRecursively(workDir);
        }
    }

    /**
     * ZIP archívumot bont ki úgy, hogy a GitHub által hozzáadott első gyökérkönyvtár-szegmenst elhagyja. Bejegyzésszám-, egyedi fájlméret- és összméretkorlátot alkalmaz, valamint minden célutat a kicsomagolási gyökér alatt tart.
     *
     * @param archive a kibontandó ZIP archívum
     * @param targetDir a kicsomagolási célkönyvtár
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void extractZipStrippingRoot(Path archive, Path targetDir) throws IOException {
        Set<String> createdDirs = new HashSet<>();
        try (InputStream fileInput = Files.newInputStream(archive);
             ZipInputStream zipInput = new ZipInputStream(fileInput)) {
            ZipEntry entry;
            int entryCount = 0;
        long totalBytes = 0;
        while ((entry = zipInput.getNextEntry()) != null) {
            if (++entryCount > MAX_ZIP_ENTRY_COUNT) {
                throw new IOException("A ZIP túl sok bejegyzést tartalmaz.");
            }
                String strippedName = stripFirstPathSegment(entry.getName());
                if (!StringUtils.hasText(strippedName)) {
                    continue;
                }
                Path output;
                try {
                    output = GitHubPathSafety.resolveRelativeInside(targetDir, Path.of(strippedName));
                } catch (IllegalArgumentException ex) {
                    throw new IOException("Unsafe ZIP entry path: " + entry.getName(), ex);
                }
                if (entry.isDirectory()) {
                    if (createdDirs.add(output.toString())) {
                        ExceptionSafeOperations.createDirectories(output);
                    }
                } else {
                    ExceptionSafeOperations.createDirectories(output.getParent());
                    try (java.io.OutputStream out = SecureFileOperations.newPrivateOutputStream(output)) {
                    byte[] buffer = new byte[64 * 1024];
                    long entryBytes = 0;
                    int read;
                    while ((read = zipInput.read(buffer)) >= 0) {
                        entryBytes += read;
                        totalBytes += read;
                        if (entryBytes > MAX_ZIP_ENTRY_BYTES || totalBytes > MAX_ZIP_TOTAL_BYTES) {
                            throw new IOException("A ZIP kibontott mérete meghaladja a biztonsági korlátot.");
                        }
                        out.write(buffer, 0, read);
                    }
                }
                }
                zipInput.closeEntry();
            }
        }
    }

    /**
     * Eltávolítja a ZIP-bejegyzés első útvonalszegmensét, hogy a GitHub zipball mesterséges gyökérkönyvtára ne kerüljön a telepített release struktúrába.
     *
     * @param entryName a ZIP bejegyzés eredeti útvonala
     * @return a művelet eredménye
     */
    private String stripFirstPathSegment(String entryName) {
        String normalized = entryName.replace('\\', '/');
        int firstSlash = normalized.indexOf('/');
        if (firstSlash < 0 || firstSlash == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(firstSlash + 1);
    }

    /**
     * Eltávolítja a megadott elemet a modul által kezelt perzisztens vagy fájlrendszeri állapotból.
     *
     * @param root a művelet gyökérkönyvtára
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void deleteRecursively(Path root) throws IOException {
        if (root == null || !ExceptionSafeOperations.fileExists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            /**
             * A rekurzív fájlrendszeri törlés egyik bejárási lépését hajtja végre; a fájl vagy a már kiürült könyvtár törlése után folytatja a bejárást.
             *
             * @param file a fájlbejáró aktuális fájlja
             * @param attrs az aktuális fájl alap attribútumai
             * @return a művelet eredménye
             * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
             */
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            /**
             * A rekurzív fájlrendszeri törlés egyik bejárási lépését hajtja végre; a fájl vagy a már kiürült könyvtár törlése után folytatja a bejárást.
             *
             * @param dir a fájlbejáró aktuális könyvtára
             * @param exc a könyvtárbejárás közben kapott kivétel, ha volt
             * @return a művelet eredménye
             * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
             */
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
