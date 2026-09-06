package hu.gov.nav.xsdparsertool.web.githubupdater.dto;

import java.time.Instant;
import java.util.List;

/**
 * A GitHub Űrlapsablon-katalógus REST műveleteihez tartozó, egymáshoz kapcsolódó immutable DTO recordokat összefogó segédosztály.
 */
public final class GitHubTemplateCatalogDtos {
    /**
     * Létrehozza a(z) {@code GitHubTemplateCatalogDtos} példányt a működéshez szükséges kezdeti állapottal és függőségekkel.
     */
    private GitHubTemplateCatalogDtos() {}

    /**
     * A katalógus egy megjeleníthető repository/release sorát hordozza az űrlaptípussal, főverzióval, release taggel, lokális ismertségi/elérhetőségi jelzőkkel és GitHub hivatkozásokkal.
     */
    public record TemplateRow(
            String repository,
            String formType,
            String version,
            String releaseTag,
            Instant repositoryUpdatedAt,
            String title,
            String validityStart,
            String validityEnd,
            boolean locallyKnown,
            boolean locallyAvailable,
            String repositoryUrl,
            String readmeUrl) {}

    /**
     * A katalóguslekérdezés teljes válasza: organization és tokenállapot, alkalmazott szűrés, darabszámok, utolsó sikeres szinkron és a megjelenítendő sorok.
     */
    public record CatalogResponse(
            String organization,
            boolean tokenConfigured,
            boolean preferredOnly,
            int repositoryCount,
            int rowCount,
            Instant lastSuccessfulSyncAt,
            boolean initialized,
            List<TemplateRow> rows) {}

    /**
     * A távoli GitHub organization és a lokális katalógus összehasonlításának eredménye, a módosult és eltávolított repository-k neveivel és számlálóival.
     */
    public record ChangeCheckResponse(
            String organization,
            boolean initialized,
            boolean changesDetected,
            int organizationRepositoryCount,
            int changedRepositoryCount,
            int removedRepositoryCount,
            Instant lastSuccessfulSyncAt,
            Instant checkedAt,
            List<String> changedRepositories,
            List<String> removedRepositories) {}

    /**
     * A háttérben végzett katalógusfrissítés indításának elfogadását és felhasználói üzenetét hordozza.
     */
    public record RefreshStartResponse(boolean started, String message) {}

    /**
     * A háttérfrissítés pillanatnyi állapotát, fázisát, aktuális repository-ját, feldolgozási számlálóit, időbélyegeit és esetleges hibaüzenetét hordozza.
     */
    public record RefreshStatusResponse(
            boolean running,
            boolean completed,
            boolean successful,
            String phase,
            String currentRepository,
            int organizationRepositoryCount,
            int changedRepositoryCount,
            int processedChangedRepositoryCount,
            int removedRepositoryCount,
            int processedRemovedRepositoryCount,
            int releaseCount,
            Instant startedAt,
            Instant completedAt,
            String errorMessage) {}

    /**
     * Egy letöltésre kijelölt GitHub repository és release tag párja.
     */
    public record DownloadItem(String repository, String tag) {}
    /**
     * A felhasználó által kijelölt release-ek letöltési kérése; a force jelzővel a már helyben lévő release újratelepítése is kérhető.
     */
    public record DownloadRequest(List<DownloadItem> items, boolean force) {}

    /** A lokálisan eltávolítandó repository/release párok kérése. */
    public record LocalDeleteRequest(List<DownloadItem> items) {}

    /** A lokális katalógus- és fájlrendszertörlés összesített eredménye. */
    public record LocalDeleteResponse(int requestedCount, int deletedRepositoryCount, int deletedReleaseCount, int deletedFileSystemEntryCount, List<String> messages) {}

    /**
     * A legutóbbi változásellenőrzésből származó értesítési összefoglaló, amely megmondja, van-e új vagy eltávolított repository.
     */
    public record NotificationResponse(
            boolean updateAvailable,
            int organizationRepositoryCount,
            int changedRepositoryCount,
            int removedRepositoryCount,
            Instant checkedAt,
            List<String> changedRepositories,
            List<String> removedRepositories) {}
}
