package hu.gov.nav.xsdparsertool.web.githubupdater.dto;

import java.util.List;

/** Több lokális űrlapsablon ZIP importjának összesített eredménye. */
public record PackageImportResponse(int requestedCount, int importedCount, int skippedCount, int failedCount,
                                    List<PackageImportItem> items) {
    public record PackageImportItem(String fileName, String repository, String releaseTag, String status,
                                    String message, List<InstalledArtifactResult> installedArtifacts) {}
}
