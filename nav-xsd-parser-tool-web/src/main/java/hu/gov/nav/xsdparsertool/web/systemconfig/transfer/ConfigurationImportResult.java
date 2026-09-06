package hu.gov.nav.xsdparsertool.web.systemconfig.transfer;

import java.util.List;

/** A MERGE konfigurációimport eredménye. */
public record ConfigurationImportResult(
        int databaseUpdated,
        int databaseInserted,
        int secretsUpdated,
        int secretsInserted,
        int certificatesUpdated,
        int certificatesInserted,
        int bootstrapMerged,
        int propertyFilesMerged,
        int textFilesReplaced,
        boolean restartRequired,
        List<String> protectedKeysSkipped) {}
