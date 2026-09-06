package hu.gov.nav.xsdparsertool.web.githubupdater.config;

/**
 * A GitHub release-archívumok letöltési stratégiáját meghatározó konfigurációs mód.
 */
public enum GitHubSchemaDownloadMode {
    /** Az archívumot a GitHub REST API zipball végpontján keresztül tölti le. */
    API_ZIPBALL,

    /** Az archívumot a github.com webes archívum URL-jéről tölti le. */
    WEB_ARCHIVE,

    /** Elsőként a github.com webes archívum URL-jét próbálja, majd hiba esetén a REST API zipball végpontra esik vissza. */
    WEB_ARCHIVE_WITH_API_FALLBACK
}
