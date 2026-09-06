package hu.gov.nav.xsdparsertool.web.githubupdater.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.githubupdater.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.web.githubupdater.config.GitHubSchemaUpdaterProperties;
import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateRelease;
import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateRepository;
import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubTemplateSyncState;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubSchemaUpdateRequest;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubSchemaUpdateResponse;
import hu.gov.nav.xsdparsertool.web.githubupdater.dto.GitHubTemplateCatalogDtos;
import hu.gov.nav.xsdparsertool.web.githubupdater.repo.GitHubTemplateReleaseRepository;
import hu.gov.nav.xsdparsertool.web.githubupdater.repo.GitHubTemplateRepositoryRepository;
import hu.gov.nav.xsdparsertool.web.githubupdater.repo.GitHubTemplateSyncStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A GitHub Űrlapsablon-katalógus alkalmazási szolgáltatása. Összerendezi a lokális adatbázis-katalógust a távoli GitHub állapottal, kezeli a háttérfrissítés progresszét, a kijelölt release-ek letöltését és a lokális csomag exportját.
 */
@Service
public class GitHubTemplateCatalogService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubTemplateCatalogService.class);
    private static final String SCHEMA_DIR = "nav.xsdparsertool.paths.schema-dir";
    private static final String COMMON_XSD_DIR = "nav.xsdparsertool.paths.common-xsd-dir";
    private static final String UI_MODEL_DIR = "nav.xsdparsertool.paths.ui-model-dir";
    private static final String XPATH_RULE_DIR = "nav.xsdparsertool.xpath-validator.rule-root-dir";
    private static final String XSL_ROOT_DIR = "nav.xsdparsertool.xpath-validator.xsl-root-dir";
    private static final String FULL_CHECK_CORE_PUBLIC_XSL = "full_check_core_public.xsl";
    private static final long SYNC_STATE_ID = 1L;
    private static final Duration INSPECTION_CACHE_DURATION = Duration.ofMinutes(5);

    private final GitHubApiClient apiClient;
    private final GitHubSchemaUpdaterService updaterService;
    private final GitHubSchemaUpdaterProperties properties;
    private final VersionTagComparator versionComparator;
    private final Environment environment;
    private final GitHubTemplateRepositoryRepository repositoryStore;
    private final GitHubTemplateReleaseRepository releaseStore;
    private final GitHubTemplateSyncStateRepository syncStateStore;
    private final GitHubTemplateCatalogPersistenceService persistenceService;

    private volatile Inspection latestInspection;
    private volatile GitHubTemplateCatalogDtos.ChangeCheckResponse latestChangeCheck;
    private volatile RefreshProgress progress = RefreshProgress.idle();

    /**
     * Létrehozza a(z) {@code GitHubTemplateCatalogService} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     *
     * @param apiClient a művelethez átadott {@code apiClient} érték
     * @param updaterService a művelethez átadott {@code updaterService} érték
     * @param properties a művelethez átadott {@code properties} érték
     * @param versionComparator a művelethez átadott {@code versionComparator} érték
     * @param environment a művelethez átadott {@code environment} érték
     * @param repositoryStore a művelethez átadott {@code repositoryStore} érték
     * @param releaseStore a művelethez átadott {@code releaseStore} érték
     * @param syncStateStore a művelethez átadott {@code syncStateStore} érték
     * @param persistenceService a művelethez átadott {@code persistenceService} érték
     */
    public GitHubTemplateCatalogService(GitHubApiClient apiClient,
                                        GitHubSchemaUpdaterService updaterService,
                                        GitHubSchemaUpdaterProperties properties,
                                        VersionTagComparator versionComparator,
                                        Environment environment,
                                        GitHubTemplateRepositoryRepository repositoryStore,
                                        GitHubTemplateReleaseRepository releaseStore,
                                        GitHubTemplateSyncStateRepository syncStateStore,
                                        GitHubTemplateCatalogPersistenceService persistenceService) {
        this.apiClient = apiClient;
        this.updaterService = updaterService;
        this.properties = properties;
        this.versionComparator = versionComparator;
        this.environment = environment;
        this.repositoryStore = repositoryStore;
        this.releaseStore = releaseStore;
        this.syncStateStore = syncStateStore;
        this.persistenceService = persistenceService;
    }

    /**
     * A perzisztált repository- és release-adatokból katalógusválaszt épít. A lokálisan ismert űrlaptípusokat összeveti a repository-nevekkel, igény szerint szűr, a release-eket verzió szerint csökkenően rendezi, és lokális elérhetőséget is számít.
     *
     * @param preferredOnly ha igaz, csak a lokálisan ismert űrlaptípusok repository-it tartalmazza a katalógus
     * @return az összeállított katalógusválasz
     */
    @Transactional(readOnly = true)
    public GitHubTemplateCatalogDtos.CatalogResponse catalog(boolean preferredOnly) {
        Set<String> localTypes = discoverLocalFormTypes();
        List<GitHubTemplateCatalogDtos.TemplateRow> rows = new ArrayList<>();
        List<GitHubTemplateRepository> repositories = RepositoryAccess.findAll(repositoryStore).stream()
                .filter(repo -> !repo.isArchived())
                .sorted(Comparator.comparing(GitHubTemplateRepository::getRepositoryUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int includedRepositories = 0;
        for (GitHubTemplateRepository repository : repositories) {
            boolean known = matchesLocalType(repository.getRepositoryName(), localTypes);
            if (preferredOnly && !known) continue;
            includedRepositories++;
            List<GitHubTemplateRelease> releases = new ArrayList<>(releaseStore.findByRepositoryNameOrderByReleaseTagAsc(repository.getRepositoryName()));
            releases.sort((left, right) -> versionComparator.reversed().compare(left.getReleaseTag(), right.getReleaseTag()));
            if (releases.isEmpty()) {
                rows.add(toRow(repository, "", known));
            } else {
                for (GitHubTemplateRelease release : releases) rows.add(toRow(repository, release.getReleaseTag(), known));
            }
        }
        GitHubTemplateSyncState state = RepositoryAccess.findById(syncStateStore, SYNC_STATE_ID).orElse(null);
        return new GitHubTemplateCatalogDtos.CatalogResponse(
                properties.getOrganization(), properties.hasToken(), preferredOnly, includedRepositories, rows.size(),
                state == null ? null : state.getLastSuccessfulSyncAt(), state != null && state.getLastSuccessfulSyncAt() != null, rows);
    }

    /**
     * Tokenhez kötött, szinkron változásellenőrzést végez: lekéri a távoli organization repository-it, összeveti őket a lokális snapshot metaadataival, azonosítja a módosult és eltávolított repository-kat, majd rövid életű inspection cache-t és értesítési állapotot frissít.
     *
     * @return a távoli és lokális állapot különbségeit összegző válasz
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     * @throws InterruptedException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public synchronized GitHubTemplateCatalogDtos.ChangeCheckResponse checkForChanges() throws IOException, InterruptedException {
        requireToken("GitHub változásellenőrzés");
        List<GitHubApiClient.RepositorySummary> remote = apiClient.listOrganizationRepositorySummaries().stream()
                .filter(repo -> !repo.archived())
                .toList();
        Map<String, GitHubTemplateRepository> local = new HashMap<>();
        RepositoryAccess.findAll(repositoryStore).forEach(repo -> local.put(repo.getRepositoryName(), repo));

        List<GitHubApiClient.RepositorySummary> changed = new ArrayList<>();
        Set<String> remoteNames = new HashSet<>();
        for (GitHubApiClient.RepositorySummary repository : remote) {
            remoteNames.add(repository.name());
            GitHubTemplateRepository saved = local.get(repository.name());
            if (saved == null || !Objects.equals(saved.getRepositoryUpdatedAt(), repository.updatedAt())
                    || !Objects.equals(saved.getDescription(), repository.description())
                    || !Objects.equals(saved.getRepositoryUrl(), repository.htmlUrl())) {
                changed.add(repository);
            }
        }
        List<String> removed = local.keySet().stream().filter(name -> !remoteNames.contains(name)).sorted().toList();
        Instant checkedAt = Instant.now();
        latestInspection = new Inspection(checkedAt, remote.size(), changed, removed);
        updateLastChecked(checkedAt, remote.size());

        GitHubTemplateSyncState state = RepositoryAccess.findById(syncStateStore, SYNC_STATE_ID).orElse(null);
        boolean initialized = state != null && state.getLastSuccessfulSyncAt() != null;
        GitHubTemplateCatalogDtos.ChangeCheckResponse response = new GitHubTemplateCatalogDtos.ChangeCheckResponse(
                properties.getOrganization(), initialized, !changed.isEmpty() || !removed.isEmpty(), remote.size(), changed.size(), removed.size(),
                state == null ? null : state.getLastSuccessfulSyncAt(), checkedAt,
                changed.stream().map(GitHubApiClient.RepositorySummary::name).toList(), removed);
        latestChangeCheck = response;
        return response;
    }

    /**
     * Elindítja a tényleges katalógusfrissítést háttérfeladatként, ha token rendelkezésre áll és nincs már futó frissítés.
     *
     * @return a háttérfrissítés indításának eredménye
     */
    public synchronized GitHubTemplateCatalogDtos.RefreshStartResponse startRefresh() {
        if (!properties.hasToken()) {
            return new GitHubTemplateCatalogDtos.RefreshStartResponse(false,
                    "A katalógusfrissítéshez GitHub token beállítása szükséges.");
        }
        if (progress.running) return new GitHubTemplateCatalogDtos.RefreshStartResponse(false, "A katalógus frissítése már folyamatban van.");
        progress = RefreshProgress.starting();
        CompletableFuture.runAsync(this::runRefresh);
        return new GitHubTemplateCatalogDtos.RefreshStartResponse(true, "A katalógus frissítése elindult.");
    }

    /**
     * A háttérben futó katalógusfrissítés aktuális immutable progresszállapotát REST válasz DTO-vá alakítva adja vissza.
     *
     * @return a háttérfrissítés aktuális állapota
     */
    public GitHubTemplateCatalogDtos.RefreshStatusResponse refreshStatus() {
        return progress.toDto();
    }

    /**
     * A háttérfrissítés teljes workflow-ja. Szükség esetén új változásellenőrzést végez, a változott repository-k tageit frissíti, az eltávolított repository-kat törli, majd sikeres sync állapotot és progresszt rögzít.
     */
    private void runRefresh() {
        try {
            Inspection inspection = latestInspection;
            if (inspection == null || Duration.between(inspection.checkedAt, Instant.now()).compareTo(INSPECTION_CACHE_DURATION) > 0) {
                progress = progress.withPhase("CHECKING", "Repositorylista ellenőrzése");
                checkForChanges();
                inspection = latestInspection;
            }
            if (inspection == null) throw new IllegalStateException("A GitHub repositorylista nem érhető el.");
            progress = RefreshProgress.running(inspection.organizationRepositoryCount, inspection.changed.size(), inspection.removed.size());

            int processedChanged = 0;
            int releaseCount = 0;
            for (GitHubApiClient.RepositorySummary repository : inspection.changed) {
                progress = progress.processing(repository.name(), processedChanged, releaseCount);
                List<String> tags = new ArrayList<>(apiClient.listRepositoryTags(repository.name()));
                tags.sort(versionComparator.reversed());
                saveRepositorySnapshot(repository, tags);
                processedChanged++;
                releaseCount += tags.size();
                progress = progress.processing(repository.name(), processedChanged, releaseCount);
            }

            int processedRemoved = 0;
            for (String repository : inspection.removed) {
                progress = progress.removing(repository, processedRemoved);
                removeRepositorySnapshot(repository);
                processedRemoved++;
                progress = progress.removing(repository, processedRemoved);
            }
            markSuccessfulSync(inspection.organizationRepositoryCount);
            latestInspection = null;
            progress = progress.completed(releaseCount);
        } catch (Exception ex) {
            LOGGER.error("GitHub template catalog refresh failed", ex);
            progress = progress.failed(ex.getMessage());
        }
    }

    /**
     * Elmenti vagy frissíti a megadott adatot a modul által kezelt perzisztens vagy memóriabeli állapotban.
     *
     * @param source a feldolgozandó forrásobjektum vagy fájl
     * @param tags a művelethez átadott {@code tags} érték
     */
    public void saveRepositorySnapshot(GitHubApiClient.RepositorySummary source, List<String> tags) {
        persistenceService.replaceRepositorySnapshot(source, tags);
    }

    /**
     * Eltávolítja a megadott elemet a modul által kezelt perzisztens vagy fájlrendszeri állapotból.
     *
     * @param repository a GitHub repository neve
     */
    public void removeRepositorySnapshot(String repository) {
        persistenceService.removeRepositorySnapshot(repository);
    }

    /**
     * Tranzakcióban rögzíti a sikeres teljes katalógusszinkron időpontját és repository-számát, majd a memóriabeli értesítési állapotot változásmentesre állítja.
     *
     * @param repositoryCount az organization aktuális repository-száma
     */
    @Transactional
    public void markSuccessfulSync(int repositoryCount) {
        GitHubTemplateSyncState state = RepositoryAccess.findById(syncStateStore, SYNC_STATE_ID).orElseGet(GitHubTemplateSyncState::new);
        state.setId(SYNC_STATE_ID);
        state.setOrganizationName(properties.getOrganization());
        state.setRepositoryCount(repositoryCount);
        state.setLastCheckedAt(Instant.now());
        state.setLastSuccessfulSyncAt(Instant.now());
        syncStateStore.save(state);
        latestChangeCheck = new GitHubTemplateCatalogDtos.ChangeCheckResponse(
                properties.getOrganization(), true, false, repositoryCount, 0, 0,
                state.getLastSuccessfulSyncAt(), state.getLastCheckedAt(), List.of(), List.of());
    }

    /**
     * A legutóbbi távoli ellenőrzés időpontját és az organization repository-számát perzisztálja anélkül, hogy sikeres teljes szinkronnak jelölné a műveletet.
     *
     * @param checkedAt a távoli ellenőrzés időpontja
     * @param repositoryCount az organization aktuális repository-száma
     */
    @Transactional
    public void updateLastChecked(Instant checkedAt, int repositoryCount) {
        GitHubTemplateSyncState state = RepositoryAccess.findById(syncStateStore, SYNC_STATE_ID).orElseGet(GitHubTemplateSyncState::new);
        state.setId(SYNC_STATE_ID);
        state.setOrganizationName(properties.getOrganization());
        state.setRepositoryCount(repositoryCount);
        state.setLastCheckedAt(checkedAt);
        syncStateStore.save(state);
    }

    /**
     * A kijelölt repository/tag párokat {@link GitHubSchemaUpdaterService} kérésévé alakítja és tényleges letöltésre továbbítja. Külső letöltéshez token meglétét követeli meg.
     *
     * @param request a végrehajtandó frissítés vagy letöltés paraméterei
     * @return a művelet eredménye
     */
    public GitHubSchemaUpdateResponse download(GitHubTemplateCatalogDtos.DownloadRequest request) {
        requireToken("GitHub release letöltés");
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Legalább egy repository és release tag kijelölése szükséges.");
        }
        GitHubSchemaUpdateRequest updateRequest = new GitHubSchemaUpdateRequest();
        updateRequest.setForceDownloadAll(request.force());
        java.util.Map<String, String> selectedRepositoryTags = request.items().stream()
                .filter(item -> item != null && StringUtils.hasText(item.repository()) && StringUtils.hasText(item.tag()))
                .collect(java.util.stream.Collectors.toMap(
                        GitHubTemplateCatalogDtos.DownloadItem::repository,
                        GitHubTemplateCatalogDtos.DownloadItem::tag,
                        (left, right) -> right,
                        java.util.LinkedHashMap::new));
        if (selectedRepositoryTags.isEmpty()) {
            throw new IllegalArgumentException(
                    "A kijelölt repositoryk egyikéhez sem tartozik letölthető release tag. " +
                    "Tag nélküli repository nem indíthat szervezetszintű letöltést.");
        }
        updateRequest.setRepositoryTags(selectedRepositoryTags);
        return updaterService.updateSchemas(updateRequest);
    }



    /**
     * A kijelölt release-ek helyi fájljait törli, majd az érintett repository-k teljes lokális katalógus-snapshotját eltávolítja.
     * Így a következő változásellenőrzés hiányzó repositoryként érzékeli őket, a katalógusfrissítés pedig újra felépíti a tageket.
     *
     * @param request a kijelölt repository/release párok
     * @return az eltávolítás összesített eredménye
     */
    public GitHubTemplateCatalogDtos.LocalDeleteResponse deleteLocal(GitHubTemplateCatalogDtos.LocalDeleteRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Legalább egy lokális repository/release kijelölése szükséges.");
        }
        int deletedFileSystemEntries = 0;
        int deletedReleases = 0;
        List<String> messages = new ArrayList<>();
        Set<String> repositories = new LinkedHashSet<>();
        for (GitHubTemplateCatalogDtos.DownloadItem item : request.items()) {
            if (item == null || !StringUtils.hasText(item.repository()) || !StringUtils.hasText(item.tag())) continue;
            repositories.add(item.repository());
            int count = updaterService.deleteLocalRelease(item.repository(), item.tag());
            deletedFileSystemEntries += count;
            deletedReleases++;
            messages.add(item.repository() + " / " + item.tag() + ": " + count + " helyi bejegyzés törölve.");
        }
        for (String repository : repositories) persistenceService.removeRepositorySnapshot(repository);
        latestInspection = null;
        latestChangeCheck = null;
        return new GitHubTemplateCatalogDtos.LocalDeleteResponse(
                request.items().size(), repositories.size(), deletedReleases, deletedFileSystemEntries, messages);
    }

    /**
     * Biztonságos, fájlnévként használható nevet képez a lokális repository/tag ZIP exporthoz.
     *
     * @param repository a GitHub repository neve
     * @param tag a release tag
     * @return biztonságos ZIP fájlnév
     */
    public String localBundleFileName(String repository, String tag) {
        String repositoryValue = requireText(repository, "repository");
        String tagValue = requireText(tag, "release tag");
        String safeRepository = GitHubPathSafety.safeSegment(repositoryValue);
        String safeTag = safeFilePart(tagValue);
        String version = GitHubPathSafety.safeSegment(normalizeVersion(tagValue));
        return safeRepository + "_" + version + "_" + safeTag + ".zip";
    }

    /**
     * A már telepített XSD, UIModel, XPath és kapcsolódó fájlokból laposabb ZIP csomagot épít és közvetlenül a kapott output streamre ír. Nem tölt le semmit a GitHubról.
     *
     * @param repository a GitHub repository neve
     * @param tag a release tag
     * @param output a ZIP tartalmát fogadó output stream
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    public void writeLocalBundle(String repository, String tag, OutputStream output) throws IOException {
        String repositoryName = requireText(repository, "repository");
        String releaseTag = requireText(tag, "release tag");
        String repositorySegment = GitHubPathSafety.safeSegment(repositoryName);
        String version = normalizeVersion(releaseTag);
        if (!StringUtils.hasText(version)) {
            throw new IllegalArgumentException("A release tagből nem állapítható meg verzió: " + releaseTag);
        }
        String versionSegment = GitHubPathSafety.safeSegment(version);

        Path schemaRoot = configuredDirectory(SCHEMA_DIR);
        Path commonRoot = configuredDirectory(COMMON_XSD_DIR);
        Path uiModelRoot = configuredDirectory(UI_MODEL_DIR);
        Path xpathRoot = configuredDirectory(XPATH_RULE_DIR);
        String legacyFormName = legacyFormName(repositoryName);

        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            Set<String> entries = new HashSet<>();
            addFlatDirectoryTree(zip, entries, commonRoot, null);
            addFormDirectory(zip, entries, schemaRoot, repositorySegment, legacyFormName, versionSegment, null);
            addFormDirectory(zip, entries, uiModelRoot, repositorySegment, legacyFormName, versionSegment, null);
            addFormDirectory(zip, entries, xpathRoot, repositorySegment, legacyFormName, versionSegment, null);
            if (entries.isEmpty()) {
                throw new IllegalStateException("A helyi csomaghoz nem található egyetlen releváns állomány sem: "
                        + repositoryName + " / " + releaseTag);
            }
        }
    }

    /**
     * A megadott erőforrást hozzáadja az épülő eredményhez, miközben kezeli az útvonalakat és az esetleges névütközéseket.
     *
     * @param zip a művelethez átadott {@code zip} érték
     * @param entries a művelethez átadott {@code entries} érték
     * @param root a művelet gyökérkönyvtára
     * @param repositoryName a GitHub repository neve
     * @param legacyFormName a művelethez átadott {@code legacyFormName} érték
     * @param version a művelethez átadott {@code version} érték
     * @param canonicalSingleFileName a művelethez átadott {@code canonicalSingleFileName} érték
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void addFormDirectory(ZipOutputStream zip, Set<String> entries, Path root,
                                  String repositoryName, String legacyFormName, String version,
                                  String canonicalSingleFileName) throws IOException {
        Path source = resolveExistingFormVersionDirectory(root, repositoryName, legacyFormName, version);
        if (source == null) return;
        addFlatDirectoryTree(zip, entries, source, canonicalSingleFileName);
    }

    /**
     * A repository névből képzett aktuális és legacy űrlaptípus-jelöltekkel megkeresi a már telepített verziókönyvtárat; csak a konfigurált gyökér alatti létező könyvtárat fogadja el.
     *
     * @param root a művelet gyökérkönyvtára
     * @param repositoryName a GitHub repository neve
     * @param legacyFormName a művelethez átadott {@code legacyFormName} érték
     * @param version a művelethez átadott {@code version} érték
     * @return a művelet eredménye
     */
    private Path resolveExistingFormVersionDirectory(Path root, String repositoryName,
                                                     String legacyFormName, String version) {
        Path trustedRoot = root.toAbsolutePath().normalize();
        try (Stream<Path> repositories = Files.list(trustedRoot)) {
            for (Path repositoryDir : repositories.filter(Files::isDirectory).toList()) {
                String discoveredRepository = repositoryDir.getFileName().toString();
                boolean repositoryMatch = discoveredRepository.equals(repositoryName)
                        || (StringUtils.hasText(legacyFormName) && discoveredRepository.equals(legacyFormName));
                if (!repositoryMatch) continue;
                try (Stream<Path> versions = Files.list(repositoryDir)) {
                    Path match = versions.filter(Files::isDirectory)
                            .filter(path -> path.getFileName().toString().equals(version))
                            .findFirst().orElse(null);
                    if (match != null) return match.toAbsolutePath().normalize();
                }
            }
            return null;
        } catch (IOException ex) {
            throw new IllegalStateException("A helyi űrlapsablon könyvtár nem olvasható.", ex);
        }
    }

    /**
     * Egy lokális könyvtár reguláris fájljait ZIP-be teszi úgy, hogy a csomagban ne keletkezzenek ütköző bejegyzésnevek; egyetlen fájl esetén kanonikus fájlnév is adható.
     *
     * @param zip a művelethez átadott {@code zip} érték
     * @param entries a művelethez átadott {@code entries} érték
     * @param sourceRoot a művelethez átadott {@code sourceRoot} érték
     * @param canonicalSingleFileName a művelethez átadott {@code canonicalSingleFileName} érték
     * @throws IOException ha a művelet végrehajtása közben a jelzett hiba bekövetkezik
     */
    private void addFlatDirectoryTree(ZipOutputStream zip, Set<String> entries, Path sourceRoot,
                                      String canonicalSingleFileName) throws IOException {
        if (!ExceptionSafeOperations.isDirectory(sourceRoot)) return;
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                Path relative = sourceRoot.relativize(file);
                String entryName = file.getFileName().toString();
                if (StringUtils.hasText(canonicalSingleFileName) && relative.getNameCount() == 1) {
                    entryName = canonicalSingleFileName;
                }
                if (!entries.add(entryName)) {
                    throw new IllegalStateException(
                            "Azonos fájlnév többször szerepelne a lapos helyi csomagban: " + entryName);
                }
                zip.putNextEntry(new ZipEntry(entryName));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
    }

    /**
     * A megadott konfigurációs property-k közül az első kitöltött könyvtárértéket abszolút és normalizált útvonallá oldja fel; ha nincs használható érték, konfigurációs hibát jelez.
     *
     * @param propertyNames a művelethez átadott {@code propertyNames} érték
     * @return a művelet eredménye
     */
    private Path configuredDirectory(String... propertyNames) {
        for (String propertyName : propertyNames) {
            String configured = environment.getProperty(propertyName);
            if (StringUtils.hasText(configured)) {
                return Path.of(configured.replace('\\', java.io.File.separatorChar).replace('/', java.io.File.separatorChar))
                        .toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("Hiányzó könyvtár-konfiguráció: " + String.join(" vagy ", propertyNames));
    }

    /**
     * A repository névből előállítja a korábbi elnevezési konvenció szerinti űrlaptípus-jelöltet, amelyet a lokális telepítés visszafelé kompatibilis feloldásánál használ.
     *
     * @param repositoryName a GitHub repository neve
     * @return a művelet eredménye
     */
    private String legacyFormName(String repositoryName) {
        if (!StringUtils.hasText(repositoryName)) return "";
        return repositoryName.replaceFirst("(?i)^(NAV|VPOP)[-_]", "").trim();
    }

    /**
     * Kötelező szöveges paramétert trimel és ellenőriz; hiányzó vagy üres értéknél a mező nevét tartalmazó argumentumhibát dob.
     *
     * @param value a feldolgozandó érték
     * @param field a validált mező neve
     * @return a művelet eredménye
     */
    private String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException("Hiányzó " + field + ".");
        return value.trim();
    }

    /**
     * A konfigurált vagy bejövő értéket biztonságos tartományra normalizálja, és szükség esetén kontrollált fallbacket alkalmaz.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String safeFilePart(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    /**
     * A bemeneti domain- vagy perzisztenciaobjektumot a hívó réteg számára szükséges reprezentációvá alakítja.
     *
     * @param repo a művelethez átadott {@code repo} érték
     * @param tag a release tag
     * @param known a művelethez átadott {@code known} érték
     * @return a művelet eredménye
     */
    private GitHubTemplateCatalogDtos.TemplateRow toRow(GitHubTemplateRepository repo, String tag, boolean known) {
        String type = repo.getRepositoryName();
        String title = StringUtils.hasText(repo.getDescription()) ? repo.getDescription() : type + " űrlapsablon";
        boolean local = isReleaseAvailableLocally(repo.getRepositoryName(), tag);
        String readmeUrl = StringUtils.hasText(tag) && StringUtils.hasText(repo.getRepositoryUrl())
                ? repo.getRepositoryUrl() + "/blob/" + tag + "/README.md" : repo.getRepositoryUrl();
        return new GitHubTemplateCatalogDtos.TemplateRow(repo.getRepositoryName(), type, deriveFormVersion(tag), tag,
                repo.getRepositoryUpdatedAt(), title, "", "", known, local, repo.getRepositoryUrl(), readmeUrl);
    }

    /**
     * A legutóbbi memóriabeli változásellenőrzésből értesítési DTO-t készít; ha még nincs ellenőrzési eredmény, változásmentes alapállapotot ad vissza.
     *
     * @return a legutóbbi változásellenőrzésből képzett értesítési állapot
     */
    public GitHubTemplateCatalogDtos.NotificationResponse notification() {
        GitHubTemplateCatalogDtos.ChangeCheckResponse current = latestChangeCheck;
        if (current == null) {
            return new GitHubTemplateCatalogDtos.NotificationResponse(false, 0, 0, 0, null, List.of(), List.of());
        }
        return new GitHubTemplateCatalogDtos.NotificationResponse(
                current.changesDetected(), current.organizationRepositoryCount(), current.changedRepositoryCount(),
                current.removedRepositoryCount(), current.checkedAt(), current.changedRepositories(), current.removedRepositories());
    }

    /**
     * Ellenőrzi az aktuális GitHub/katalógus állapotot, és a vizsgálat eredményét a hívó számára elérhető állapotba rendezi.
     */
    public void checkForChangesInBackground() {
        try {
            checkForChanges();
        } catch (Exception ex) {
            LOGGER.warn("Background GitHub template change check failed: {}", ex.getMessage());
        }
    }

    /**
     * Külső GitHub művelet előtt ellenőrzi a token konfiguráltságát, és hiány esetén felhasználóbarát konfigurációs hibát dob.
     *
     * @param operation a diagnosztikában szereplő GitHub művelet neve
     */
    private void requireToken(String operation) {
        if (!properties.hasToken()) {
            throw new IllegalStateException(operation + " nem indítható, mert a GitHub token nincs beállítva.");
        }
    }

    /**
     * Repository-típus szerint ellenőrzi, hogy a megadott release erőforrásai már ténylegesen megtalálhatók-e a konfigurált lokális célkönyvtárakban.
     *
     * @param repository a GitHub repository neve
     * @param tag a release tag
     * @return a(z) releaseAvailableLocally érték
     */
    private boolean isReleaseAvailableLocally(String repository, String tag) {
        if (!StringUtils.hasText(repository) || !StringUtils.hasText(tag)) return false;
        try {
            if (isFullCheckCorePublicRepository(repository)) {
                return ExceptionSafeOperations.isRegularFile(configuredDirectory(XSL_ROOT_DIR).resolve(FULL_CHECK_CORE_PUBLIC_XSL));
            }
            if (isCommonRepository(repository)) {
                return containsRegularFile(configuredDirectory(COMMON_XSD_DIR));
            }
            Path root = updaterService.resolveTargetSchemaDir();
            Path localRelease = GitHubPathSafety.resolveInside(root, repository, tag);
            return ExceptionSafeOperations.isDirectory(localRelease);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * A bemeneti állapot és a modul szabályai alapján eldönti, hogy a vizsgált feltétel teljesül-e.
     *
     * @param directory a vizsgált könyvtár
     * @return a művelet eredménye
     */
    private boolean containsRegularFile(Path directory) {
        if (!ExceptionSafeOperations.isDirectory(directory)) return false;
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.anyMatch(Files::isRegularFile);
        } catch (IOException ex) {
            LOGGER.debug("A helyi repository-allapot nem ellenorizheto: {}", directory, ex);
            return false;
        }
    }

    /**
     * A megadott konfigurációs property-k közül az első kitöltött könyvtárértéket abszolút és normalizált útvonallá oldja fel; ha nincs használható érték, konfigurációs hibát jelez.
     *
     * @param propertyName a művelethez átadott {@code propertyName} érték
     * @return a művelet eredménye
     */
    private Path configuredDirectory(String propertyName) {
        String configured = environment.getProperty(propertyName);
        if (!StringUtils.hasText(configured)) {
            throw new IllegalStateException("Hianyzo konyvtar-konfiguracio: " + propertyName);
        }
        return Path.of(configured.trim()).toAbsolutePath().normalize();
    }

    /**
     * Visszaadja a(z) fullCheckCorePublicRepository aktuális értékét.
     *
     * @param repositoryName a GitHub repository neve
     * @return a(z) fullCheckCorePublicRepository érték
     */
    private boolean isFullCheckCorePublicRepository(String repositoryName) {
        if (!StringUtils.hasText(repositoryName)) return false;
        String normalized = repositoryName.replaceFirst("(?i)^nav[-_]", "").trim();
        return "full_check_core_public".equalsIgnoreCase(normalized)
                || "full-check-core-public".equalsIgnoreCase(normalized);
    }

    /**
     * Visszaadja a(z) commonRepository aktuális értékét.
     *
     * @param repositoryName a GitHub repository neve
     * @return a(z) commonRepository érték
     */
    private boolean isCommonRepository(String repositoryName) {
        if (!StringUtils.hasText(repositoryName)) return false;
        String normalized = repositoryName.replaceFirst("(?i)^nav[-_]", "").trim();
        return "common".equalsIgnoreCase(normalized);
    }

    /**
     * A megadott értéket a modul belső összehasonlítási és elérési szabályainak megfelelő kanonikus formára alakítja.
     *
     * @param tag a release tag
     * @return a művelet eredménye
     */
    private String normalizeVersion(String tag) {
        if (!StringUtils.hasText(tag)) return "";
        return tag.replaceFirst("(?i)^(release[-_/]?|v)", "");
    }

    /**
     * A GitHub release tag teljes artifact-verziójából meghatározza a nyomtatvány
     * főverzióját. A harmadik numerikus komponens a release patch verziója, ezért
     * például az 1.12.1 release taghez az 1.12 nyomtatványverzió tartozik.
     */
    private String deriveFormVersion(String tag) {
        String releaseVersion = normalizeVersion(tag);
        if (!StringUtils.hasText(releaseVersion)) return "";
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:[-+_].*)?$")
                .matcher(releaseVersion.trim());
        if (!matcher.matches()) return releaseVersion;
        return matcher.group(1) + "." + matcher.group(2);
    }

    /**
     * A megadott értéket a modul belső összehasonlítási és elérési szabályainak megfelelő kanonikus formára alakítja.
     *
     * @param repository a GitHub repository neve
     * @return a művelet eredménye
     */
    private String normalizeFormType(String repository) {
        if (!StringUtils.hasText(repository)) return "";
        return repository.trim();
    }

    /**
     * Kanonizált összehasonlítással ellenőrzi, hogy a repository egy lokálisan ismert űrlaptípushoz tartozik-e.
     *
     * @param repository a GitHub repository neve
     * @param localTypes a művelethez átadott {@code localTypes} érték
     * @return a művelet eredménye
     */
    private boolean matchesLocalType(String repository, Set<String> localTypes) {
        String candidate = canonical(normalizeFormType(repository));
        return localTypes.stream().anyMatch(local -> local.equals(candidate) || local.contains(candidate) || candidate.contains(local));
    }

    /**
     * A konfigurált lokális séma- és kapcsolódó könyvtárak alapján összegyűjti az alkalmazás által már ismert űrlaptípusokat.
     *
     * @return a művelet eredménye
     */
    private Set<String> discoverLocalFormTypes() {
        Set<String> result = new LinkedHashSet<>();
        String configured = environment.getProperty(SCHEMA_DIR);
        if (!StringUtils.hasText(configured)) return result;
        Path root = Path.of(configured).toAbsolutePath().normalize();
        if (!ExceptionSafeOperations.isDirectory(root)) return result;
        try (Stream<Path> paths = Files.walk(root, 3)) {
            paths.forEach(path -> {
                String name = path.getFileName() == null ? "" : path.getFileName().toString();
                if (ExceptionSafeOperations.isDirectory(path) || name.toLowerCase(Locale.ROOT).endsWith(".xsd")) {
                    String stem = name.replaceFirst("(?i)\\.xsd$", "");
                    if (stem.matches(".*[A-Za-z].*[0-9].*|.*[0-9].*[A-Za-z].*")) result.add(canonical(stem));
                }
            });
        } catch (IOException ignored) { }
        result.remove("");
        return result;
    }

    /**
     * A megadott értéket a modul belső összehasonlítási és elérési szabályainak megfelelő kanonikus formára alakítja.
     *
     * @param value a feldolgozandó érték
     * @return a művelet eredménye
     */
    private String canonical(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    /**
     * Egy GitHub organization-változásellenőrzés rövid ideig cache-elt eredménye: a vizsgált repository-k száma, a megváltozott repository-k és az eltávolított nevek.
     */
    private record Inspection(Instant checkedAt, int organizationRepositoryCount,
                              List<GitHubApiClient.RepositorySummary> changed, List<String> removed) { }

    /**
     * A háttérben futó katalógusfrissítés aktuális progresszét és befejezési állapotát hordozó belső immutable állapotobjektum.
     */
    private static final class RefreshProgress {
        private final boolean running;
        private final boolean completed;
        private final boolean successful;
        private final String phase;
        private final String currentRepository;
        private final int organizationRepositoryCount;
        private final int changedRepositoryCount;
        private final int processedChangedRepositoryCount;
        private final int removedRepositoryCount;
        private final int processedRemovedRepositoryCount;
        private final int releaseCount;
        private final Instant startedAt;
        private final Instant completedAt;
        private final String errorMessage;

        /**
         * Létrehozza a(z) {@code RefreshProgress} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
         *
         * @param running a művelethez átadott {@code running} érték
         * @param completed a művelethez átadott {@code completed} érték
         * @param successful a művelethez átadott {@code successful} érték
         * @param phase a művelethez átadott {@code phase} érték
         * @param currentRepository a művelethez átadott {@code currentRepository} érték
         * @param organizationRepositoryCount a művelethez átadott {@code organizationRepositoryCount} érték
         * @param changedRepositoryCount a művelethez átadott {@code changedRepositoryCount} érték
         * @param processedChangedRepositoryCount a művelethez átadott {@code processedChangedRepositoryCount} érték
         * @param removedRepositoryCount a művelethez átadott {@code removedRepositoryCount} érték
         * @param processedRemovedRepositoryCount a művelethez átadott {@code processedRemovedRepositoryCount} érték
         * @param releaseCount a művelethez átadott {@code releaseCount} érték
         * @param startedAt a művelethez átadott {@code startedAt} érték
         * @param completedAt a művelethez átadott {@code completedAt} érték
         * @param errorMessage a művelethez átadott {@code errorMessage} érték
         */
        private RefreshProgress(boolean running, boolean completed, boolean successful, String phase, String currentRepository,
                                int organizationRepositoryCount, int changedRepositoryCount, int processedChangedRepositoryCount,
                                int removedRepositoryCount, int processedRemovedRepositoryCount, int releaseCount,
                                Instant startedAt, Instant completedAt, String errorMessage) {
            this.running = running; this.completed = completed; this.successful = successful; this.phase = phase;
            this.currentRepository = currentRepository; this.organizationRepositoryCount = organizationRepositoryCount;
            this.changedRepositoryCount = changedRepositoryCount; this.processedChangedRepositoryCount = processedChangedRepositoryCount;
            this.removedRepositoryCount = removedRepositoryCount; this.processedRemovedRepositoryCount = processedRemovedRepositoryCount;
            this.releaseCount = releaseCount; this.startedAt = startedAt; this.completedAt = completedAt; this.errorMessage = errorMessage;
        }
        /**
         * Létrehozza a katalógusfrissítés kezdeti, nem futó és még nem befejezett progresszállapotát.
         *
         * @return a művelet eredménye
         */
        static RefreshProgress idle() { return new RefreshProgress(false, false, false, "IDLE", "", 0, 0, 0, 0, 0, 0, null, null, ""); }
        /**
         * Létrehozza az aszinkron katalógusfrissítés induló állapotát kezdési időbélyeggel.
         *
         * @return a művelet eredménye
         */
        static RefreshProgress starting() { return new RefreshProgress(true, false, false, "STARTING", "", 0, 0, 0, 0, 0, 0, Instant.now(), null, ""); }
        /**
         * Az inspection eredménye alapján létrehozza a ténylegesen futó katalógusfrissítés összesített progresszállapotát.
         *
         * @param total a művelethez átadott {@code total} érték
         * @param changed a művelethez átadott {@code changed} érték
         * @param removed a művelethez átadott {@code removed} érték
         * @return a művelet eredménye
         */
        static RefreshProgress running(int total, int changed, int removed) { return new RefreshProgress(true, false, false, "REFRESHING", "", total, changed, 0, removed, 0, 0, Instant.now(), null, ""); }
        /**
         * Az aktuális progressz minden számlálóját megőrizve új állapotot hoz létre a megadott fázissal és aktuális repository/üzenet értékkel.
         *
         * @param phase a művelethez átadott {@code phase} érték
         * @param current a művelethez átadott {@code current} érték
         * @return a művelet eredménye
         */
        RefreshProgress withPhase(String phase, String current) { return new RefreshProgress(true, false, false, phase, current, organizationRepositoryCount, changedRepositoryCount, processedChangedRepositoryCount, removedRepositoryCount, processedRemovedRepositoryCount, releaseCount, startedAt == null ? Instant.now() : startedAt, null, ""); }
        /**
         * Új progresszállapotot készít egy változott repository feldolgozásához, frissítve a feldolgozott repository-k és release-ek számlálóit.
         *
         * @param repo a művelethez átadott {@code repo} érték
         * @param processed a művelethez átadott {@code processed} érték
         * @param releases a művelethez átadott {@code releases} érték
         * @return a művelet eredménye
         */
        RefreshProgress processing(String repo, int processed, int releases) { return new RefreshProgress(true, false, false, "REFRESHING", repo, organizationRepositoryCount, changedRepositoryCount, processed, removedRepositoryCount, processedRemovedRepositoryCount, releases, startedAt, null, ""); }
        /**
         * Új progresszállapotot készít egy távolról eltávolított repository lokális katalógusból történő kivezetéséhez.
         *
         * @param repo a művelethez átadott {@code repo} érték
         * @param processed a művelethez átadott {@code processed} érték
         * @return a művelet eredménye
         */
        RefreshProgress removing(String repo, int processed) { return new RefreshProgress(true, false, false, "REMOVING", repo, organizationRepositoryCount, changedRepositoryCount, processedChangedRepositoryCount, removedRepositoryCount, processed, releaseCount, startedAt, null, ""); }
        /**
         * A futó progresszből sikeresen befejezett állapotot készít, rögzítve a végleges release-számot és a befejezés időpontját.
         *
         * @param releases a művelethez átadott {@code releases} érték
         * @return a művelet eredménye
         */
        RefreshProgress completed(int releases) { return new RefreshProgress(false, true, true, "COMPLETED", "", organizationRepositoryCount, changedRepositoryCount, changedRepositoryCount, removedRepositoryCount, removedRepositoryCount, releases, startedAt, Instant.now(), ""); }
        /**
         * A futó progresszből hibásan befejezett állapotot készít, megőrizve az addigi számlálókat és eltárolva a hibaüzenetet.
         *
         * @param error a művelethez átadott {@code error} érték
         * @return a művelet eredménye
         */
        RefreshProgress failed(String error) { return new RefreshProgress(false, true, false, "FAILED", currentRepository, organizationRepositoryCount, changedRepositoryCount, processedChangedRepositoryCount, removedRepositoryCount, processedRemovedRepositoryCount, releaseCount, startedAt, Instant.now(), error == null ? "Ismeretlen hiba" : error); }
        /**
         * A bemeneti domain- vagy perzisztenciaobjektumot a hívó réteg számára szükséges reprezentációvá alakítja.
         *
         * @return a művelet eredménye
         */
        GitHubTemplateCatalogDtos.RefreshStatusResponse toDto() { return new GitHubTemplateCatalogDtos.RefreshStatusResponse(running, completed, successful, phase, currentRepository, organizationRepositoryCount, changedRepositoryCount, processedChangedRepositoryCount, removedRepositoryCount, processedRemovedRepositoryCount, releaseCount, startedAt, completedAt, errorMessage); }
    }
}
