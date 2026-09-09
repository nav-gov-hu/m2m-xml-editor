package hu.gov.nav.xsdparsertool.versioning;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Helyi Git-adatok alapján működő szemantikus verzióelemző és build-metaadat generátor.
 *
 * <p>A megvalósítás szándékosan nem használ külső függőségeket, így a buildet indító
 * segédprogramok a Maven futása előtt, közvetlenül a JDK-val is le tudják fordítani és
 * futtatni.</p>
 */
public final class VersioningTool {

    private static final Pattern SEMVER_TAG = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-\\d{8}-\\d{6})?$");
    private static final Pattern REST_MAPPING = Pattern.compile("@(Get|Post|Put|Patch|Delete|Request)Mapping\\b");
    private static final Pattern PUBLIC_API = Pattern.compile("^\\s*(public|protected)\\s+.*[({].*$");
    private static final Pattern PUBLIC_TYPE = Pattern.compile("\\bpublic\\s+(?:final\\s+|sealed\\s+|abstract\\s+)?(?:class|interface|enum|record)\\b");
    private static final Pattern CONFIG_LINE = Pattern.compile("^[+-]([A-Za-z0-9_.-]+)\\s*[=:].*$");
    private static final DateTimeFormatter BUILD_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String VERSIONING_IGNORE_FILE = ".versioningignore";

    private VersioningTool() {
    }

    /** A program belépési pontja. */
    public static void main(String[] args) throws Exception {
        Options options = Options.parse(args);
        Path repo = options.repo().toAbsolutePath().normalize();
        List<PathMatcher> versioningIgnoreMatchers = loadVersioningIgnoreMatchers(repo);
        boolean gitAvailable = Files.exists(repo.resolve(".git"));

        Baseline baseline;
        List<Change> changes;
        List<Reason> reasons = new ArrayList<>();
        Bump detected;
        String commit;
        boolean dirty;
        if (gitAvailable) {
            ensureGitRepository(repo);
            baseline = resolveBaseline(repo, options);
            changes = collectChanges(repo, baseline.ref(), versioningIgnoreMatchers);
            detected = classify(repo, baseline, changes, reasons);
            commit = git(repo, "rev-parse", "--short=12", "HEAD").trim();
            dirty = !git(repo, "status", "--porcelain").isBlank();
        } else {
            String fallback = options.baseVersion() != null ? options.baseVersion() : readFallbackVersion(repo);
            baseline = new Baseline("NO_GIT", SemVer.parse(fallback));
            changes = List.of();
            detected = Bump.PATCH;
            reasons.add(new Reason(Bump.PATCH, "A Git metaadatai nem érhetők el; biztonsági tartalékként PATCH verzióemelést használunk."));
            commit = "NO_GIT";
            dirty = true;
        }
        Bump effective = options.override() == Bump.AUTO ? detected : options.override();
        SemVer next = baseline.version().bump(effective);

        OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());
        String timestamp = now.format(BUILD_STAMP);

        BuildInfo info = new BuildInfo(
                baseline.version(), baseline.ref(), detected, effective, next,
                now, timestamp, commit, dirty, changes, reasons);
        writeOutputs(repo.resolve(options.outputDir()).normalize(), info);
        printReport(info);
    }

    private static void ensureGitRepository(Path repo) throws IOException, InterruptedException {
        String inside = git(repo, "rev-parse", "--is-inside-work-tree").trim();
        if (!"true".equalsIgnoreCase(inside)) {
            throw new IllegalStateException("A megadott könyvtár nem Git munkakönyvtár: " + repo);
        }
    }

    private static Baseline resolveBaseline(Path repo, Options options) throws IOException, InterruptedException {
        if (options.baseRef() != null) {
            SemVer version = options.baseVersion() != null
                    ? SemVer.parse(options.baseVersion())
                    : versionFromRef(options.baseRef());
            return new Baseline(options.baseRef(), version);
        }

        SemVer releasedFloor = SemVer.parse(readReleasedBaseVersion(repo));
        String tags = git(repo, "tag", "--merged", "HEAD", "--sort=-version:refname");
        for (String line : tags.lines().toList()) {
            Matcher matcher = SEMVER_TAG.matcher(line.trim());
            if (matcher.matches()) {
                SemVer taggedVersion = SemVer.fromMatcher(matcher);
                if (taggedVersion.compareTo(releasedFloor) >= 0) {
                    return new Baseline(line.trim(), taggedVersion);
                }
                break;
            }
        }

        String fallback = options.baseVersion() != null ? options.baseVersion() : readFallbackVersion(repo);
        SemVer fallbackVersion = SemVer.parse(fallback);
        SemVer effectiveBase = fallbackVersion.compareTo(releasedFloor) >= 0 ? fallbackVersion : releasedFloor;
        return new Baseline("HEAD", effectiveBase);
    }

    private static SemVer versionFromRef(String ref) {
        Matcher matcher = SEMVER_TAG.matcher(ref.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("A --base-ref nem szemantikus verziót tartalmazó Git tag; add meg a --base-version értékét is: " + ref);
        }
        return SemVer.fromMatcher(matcher);
    }

    private static String readFallbackVersion(Path repo) throws IOException {
        return readVersioningProperty(repo, "default.base.version");
    }

    private static String readReleasedBaseVersion(Path repo) throws IOException {
        return readVersioningProperty(repo, "released.base.version");
    }

    private static String readVersioningProperty(Path repo, String key) throws IOException {
        Path config = repo.resolve("nav-xsd-parser-tool-versioning/versioning.properties");
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Hiányzik a " + key + " érték ebből az állományból: " + config);
        }
        return value.trim();
    }

    private static List<Change> collectChanges(
            Path repo,
            String baseRef,
            List<PathMatcher> versioningIgnoreMatchers) throws IOException, InterruptedException {

        Map<String, Change> result = new LinkedHashMap<>();

        String statuses = git(
                repo,
                "-c",
                "core.quotepath=false",
                "diff",
                "--name-status",
                "--find-renames",
                baseRef,
                "--");

        for (String line : statuses.lines().toList()) {
            if (line.isBlank()) {
                continue;
            }

            String[] parts = line.split("\\t");
            String status = parts[0];
            String path = parts[parts.length - 1];

            if (!isGeneratedPath(path) && !isVersioningIgnored(path, versioningIgnoreMatchers)) {
                result.put(path, new Change(status, path, false));
            }
        }

        String untracked = git(
                repo,
                "-c",
                "core.quotepath=false",
                "ls-files",
                "--others",
                "--exclude-standard");

        for (String path : untracked.lines().toList()) {
            if (!path.isBlank()
                    && !isGeneratedPath(path)
                    && !isVersioningIgnored(path, versioningIgnoreMatchers)) {
                result.put(path, new Change("A", path, true));
            }
        }

        return new ArrayList<>(result.values());
    }

    /**
     * Betölti a repository gyökerében található .versioningignore állomány
     * glob mintáit.
     *
     * <p>Az üres sorok és a # karakterrel kezdődő megjegyzések
     * figyelmen kívül maradnak. A minták repository-relatív útvonalakra
     * vonatkoznak.</p>
     *
     * @param repo a Git repository gyökérkönyvtára
     * @return a betöltött útvonalminták
     */
    private static List<PathMatcher> loadVersioningIgnoreMatchers(Path repo) {
        Path ignoreFile = repo.resolve(VERSIONING_IGNORE_FILE);

        if (!Files.isRegularFile(ignoreFile)) {
            return List.of();
        }

        try {
            List<PathMatcher> matchers = new ArrayList<>();

            for (String line : Files.readAllLines(ignoreFile, StandardCharsets.UTF_8)) {
                String pattern = line.trim();

                if (pattern.isEmpty() || pattern.startsWith("#")) {
                    continue;
                }

                String normalizedPattern = pattern.replace('\\', '/');
                matchers.add(repo.getFileSystem().getPathMatcher("glob:" + normalizedPattern));
            }

            return List.copyOf(matchers);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Nem sikerült beolvasni a " + VERSIONING_IGNORE_FILE + " állományt: " + ignoreFile,
                    ex);
        }
    }

    /**
     * Megállapítja, hogy az adott repository-relatív útvonalat figyelmen
     * kívül kell-e hagyni a verziószámítás során.
     *
     * @param path repository-relatív útvonal
     * @param matchers ignore minták
     * @return true, ha az útvonal kizárt
     */
    private static boolean isVersioningIgnored(String path, List<PathMatcher> matchers) {
        if (path == null || path.isBlank() || matchers.isEmpty()) {
            return false;
        }

        String normalized = path.replace('\\', '/');
        Path relativePath = Path.of(normalized);

        return matchers.stream().anyMatch(matcher -> matcher.matches(relativePath));
    }

    private static Bump classify(Path repo, Baseline baseline, List<Change> changes, List<Reason> reasons)
            throws IOException, InterruptedException {
        Bump result = Bump.NONE;

        if (!"HEAD".equals(baseline.ref())) {
            String messages = git(repo, "log", "--format=%s%n%b", baseline.ref() + "..HEAD");
            String lower = messages.toLowerCase(Locale.ROOT);
            if (lower.contains("breaking change") || Pattern.compile("(?m)^[a-z]+(?:\\([^)]*\\))?!:").matcher(lower).find()) {
                result = raise(result, Bump.MAJOR, reasons, "A Git commit BREAKING CHANGE jelölést tartalmaz.");
            } else if (Pattern.compile("(?m)^feat(?:\\([^)]*\\))?:").matcher(lower).find()) {
                result = raise(result, Bump.MINOR, reasons, "Conventional Commit 'feat' bejegyzés észlelve.");
            } else if (Pattern.compile("(?m)^fix(?:\\([^)]*\\))?:").matcher(lower).find()) {
                result = raise(result, Bump.PATCH, reasons, "Conventional Commit 'fix' bejegyzés észlelve.");
            }
        }

        if (changes.isEmpty()) {
            return result;
        }

        for (Change change : changes) {
            Path file = repo.resolve(change.path());
            String diff = change.untracked() ? readIfText(file) : git(repo, "diff", "--unified=0", baseline.ref(), "--", change.path());
            String normalized = change.path().replace('\\', '/');
            boolean deleted = change.status().startsWith("D");
            boolean added = change.status().startsWith("A") || change.untracked();

            if (normalized.equals("pom.xml")) {
                if (hasAddedModule(diff)) {
                    result = raise(result, Bump.MINOR, reasons, "Új Maven modul került a gyökér pom.xml állományba.");
                } else {
                    result = raise(result, Bump.PATCH, reasons, "Módosult a build konfiguráció a gyökér pom.xml állományban.");
                }
            }

            if (isFlyway(normalized)) {
                String upper = diff.toUpperCase(Locale.ROOT);
                if (upper.contains("DROP TABLE") || upper.contains("DROP COLUMN") || upper.contains("TRUNCATE TABLE")) {
                    result = raise(result, Bump.MAJOR, reasons, "Destruktív Flyway adatbázis-migráció: " + normalized);
                } else if (added) {
                    result = raise(result, Bump.MINOR, reasons, "Új kompatibilis Flyway migráció: " + normalized);
                } else {
                    result = raise(result, Bump.PATCH, reasons, "Meglévő migrációs/build SQL módosult: " + normalized);
                }
                continue;
            }

            if (isProductionJava(normalized)) {
                if (deleted && oldFileContainsPublicApi(repo, baseline.ref(), normalized)) {
                    result = raise(result, Bump.MAJOR, reasons, "Éles Java API vagy típus eltávolítva: " + normalized);
                    continue;
                }
                if (containsRemovedRestMapping(diff)) {
                    result = raise(result, Bump.MAJOR, reasons, "REST végpont-hozzárendelés eltávolítva: " + normalized);
                    continue;
                }
                if (containsRemovedPublicApi(diff)) {
                    result = raise(result, Bump.MAJOR, reasons, "Publikus vagy protected Java API-szignatúra eltávolítva: " + normalized);
                    continue;
                }
                if (containsAddedRestMapping(diff)) {
                    result = raise(result, Bump.MINOR, reasons, "Új REST végpont-hozzárendelés: " + normalized);
                    continue;
                }
                if (added) {
                    result = raise(result, Bump.MINOR, reasons, "Új éles Java forrásfájl: " + normalized);
                    continue;
                }
                result = raise(result, Bump.PATCH, reasons, "Éles Java implementáció módosult: " + normalized);
                continue;
            }

            if (isConfig(normalized)) {
                if (added) {
                    result = raise(result, Bump.MINOR, reasons, "Új konfigurációs állomány került bevezetésre: " + normalized);
                    continue;
                }
                ConfigDelta delta = analyzeConfigDiff(diff);
                if (delta.removed()) {
                    result = raise(result, Bump.MAJOR, reasons, "Konfigurációs kulcs eltávolítva vagy átnevezve: " + normalized);
                } else if (delta.added()) {
                    result = raise(result, Bump.MINOR, reasons, "Új konfigurációs kulcs került bevezetésre: " + normalized);
                } else {
                    result = raise(result, Bump.PATCH, reasons, "Konfigurációs érték vagy alapértelmezés módosult: " + normalized);
                }
                continue;
            }

            if (isTestOrDocs(normalized)) {
                result = raise(result, Bump.PATCH, reasons, "Teszt- vagy dokumentációs módosítás: " + normalized);
                continue;
            }

            if (normalized.endsWith(".iss") || normalized.endsWith(".bat") || normalized.endsWith(".cmd")
                    || normalized.endsWith(".ps1") || normalized.endsWith(".sh")) {
                result = raise(result, Bump.PATCH, reasons, "Build- vagy telepítőscript módosult: " + normalized);
                continue;
            }

            if (added && normalized.contains("/src/main/")) {
                result = raise(result, Bump.MINOR, reasons, "Új éles erőforrás vagy forrás került hozzáadásra: " + normalized);
            } else {
                result = raise(result, Bump.PATCH, reasons, "Projektállomány módosult: " + normalized);
            }
        }
        return result;
    }

    private static boolean hasAddedModule(String diff) {
        return diff.lines().anyMatch(line -> line.startsWith("+") && line.contains("<module>"));
    }

    private static boolean isGeneratedPath(String path) {
        String normalized = path.replace('\\', '/');
        return normalized.startsWith("target/") || normalized.contains("/target/")
                || normalized.equals(".flattened-pom.xml") || normalized.endsWith("/.flattened-pom.xml")
                || normalized.startsWith("installer/windows/build/")
                || normalized.startsWith(".idea/") || normalized.startsWith(".vscode/");
    }

    private static boolean isFlyway(String path) {
        return path.contains("/src/main/resources/db/migration/") && path.toLowerCase(Locale.ROOT).endsWith(".sql");
    }

    private static boolean isProductionJava(String path) {
        return path.contains("/src/main/java/") && path.endsWith(".java");
    }

    private static boolean isConfig(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".properties") || lower.endsWith(".yml") || lower.endsWith(".yaml");
    }

    private static boolean isTestOrDocs(String path) {
        return path.startsWith("docs/") || path.endsWith("/README.md") || path.equals("README.md")
                || path.contains("/src/test/") || path.startsWith("developer_docs/");
    }

    private static boolean containsAddedRestMapping(String diff) {
        return diff.lines().anyMatch(line -> line.startsWith("+") && !line.startsWith("+++") && REST_MAPPING.matcher(line).find());
    }

    private static boolean containsRemovedRestMapping(String diff) {
        return diff.lines().anyMatch(line -> line.startsWith("-") && !line.startsWith("---") && REST_MAPPING.matcher(line).find());
    }

    private static boolean containsRemovedPublicApi(String diff) {
        java.util.Set<String> removed = new java.util.HashSet<>();
        java.util.Set<String> added = new java.util.HashSet<>();
        for (String line : diff.lines().toList()) {
            if ((line.startsWith("-") && !line.startsWith("---")) || (line.startsWith("+") && !line.startsWith("+++"))) {
                String body = line.substring(1);
                if (PUBLIC_API.matcher(body).matches() || PUBLIC_TYPE.matcher(body).find()) {
                    String signature = normalizeApiSignature(body);
                    if (line.startsWith("-")) removed.add(signature); else added.add(signature);
                }
            }
        }
        removed.removeAll(added);
        return !removed.isEmpty();
    }

    private static String normalizeApiSignature(String line) {
        String value = line.trim().replaceAll("\\s+", " ");
        int brace = value.indexOf('{');
        if (brace >= 0) value = value.substring(0, brace).trim();
        return value;
    }

    private static boolean oldFileContainsPublicApi(Path repo, String baseRef, String path) throws IOException, InterruptedException {
        if ("HEAD".equals(baseRef)) {
            String old = git(repo, "show", "HEAD:" + path);
            return old.lines().anyMatch(line -> PUBLIC_API.matcher(line).matches());
        }
        String old = git(repo, "show", baseRef + ":" + path);
        return old.lines().anyMatch(line -> PUBLIC_API.matcher(line).matches() || PUBLIC_TYPE.matcher(line).find());
    }

    private static ConfigDelta analyzeConfigDiff(String diff) {
        java.util.Set<String> addedKeys = new java.util.HashSet<>();
        java.util.Set<String> removedKeys = new java.util.HashSet<>();
        for (String line : diff.lines().toList()) {
            Matcher matcher = CONFIG_LINE.matcher(line);
            if (!matcher.matches() || line.startsWith("+++") || line.startsWith("---")) {
                continue;
            }
            if (line.startsWith("+")) {
                addedKeys.add(matcher.group(1));
            } else if (line.startsWith("-")) {
                removedKeys.add(matcher.group(1));
            }
        }
        java.util.Set<String> removedOnly = new java.util.HashSet<>(removedKeys);
        removedOnly.removeAll(addedKeys);
        java.util.Set<String> addedOnly = new java.util.HashSet<>(addedKeys);
        addedOnly.removeAll(removedKeys);
        return new ConfigDelta(!addedOnly.isEmpty(), !removedOnly.isEmpty());
    }

    private static String readIfText(Path file) throws IOException {
        if (!Files.isRegularFile(file) || Files.size(file) > 2_000_000) {
            return "";
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "";
        }
    }

    private static Bump raise(Bump current, Bump candidate, List<Reason> reasons, String text) {
        reasons.add(new Reason(candidate, text));
        return candidate.rank > current.rank ? candidate : current;
    }

    private static void writeOutputs(Path outputDir, BuildInfo info) throws IOException {
        Files.createDirectories(outputDir);
        Properties properties = new Properties();
        properties.setProperty("version.base", info.base().toString());
        properties.setProperty("version.baseRef", info.baseRef());
        properties.setProperty("version.detectedBump", info.detected().name());
        properties.setProperty("version.bump", info.effective().name());
        properties.setProperty("version.release", info.next().toString());
        properties.setProperty("version.build", info.next() + "+" + info.timestamp().replace("-", "."));
        properties.setProperty("version.date", info.now().toLocalDate().toString());
        properties.setProperty("version.time", info.now().toLocalTime().withNano(0).toString());
        properties.setProperty("version.timestamp", info.timestamp());
        properties.setProperty("version.commit", info.commit());
        properties.setProperty("version.dirty", Boolean.toString(info.dirty()));
        properties.setProperty("version.changedFiles", Integer.toString(info.changes().size()));
        try (var writer = Files.newBufferedWriter(outputDir.resolve("build-version.properties"), StandardCharsets.UTF_8)) {
            properties.store(writer, "A nav-xsd-parser-tool-versioning által generálva");
        }

        String env = "VERSION_BASE=" + info.base() + "\n"
                + "VERSION_BUMP=" + info.effective() + "\n"
                + "VERSION_RELEASE=" + info.next() + "\n"
                + "VERSION_BUILD=" + info.next() + "+" + info.timestamp().replace("-", ".") + "\n"
                + "VERSION_TIMESTAMP=" + info.timestamp() + "\n"
                + "VERSION_COMMIT=" + info.commit() + "\n";
        Files.writeString(outputDir.resolve("build-version.env"), env, StandardCharsets.UTF_8);
        Files.writeString(outputDir.resolve("version-report.txt"), report(info), StandardCharsets.UTF_8);
    }

    private static void printReport(BuildInfo info) {
        System.out.print(report(info));
    }

    private static String report(BuildInfo info) {
        StringBuilder out = new StringBuilder();
        out.append("Előző verzió: ").append(info.base()).append("\n");
        out.append("Bázis referencia: ").append(info.baseRef()).append("\n");
        out.append("Módosult fájlok: ").append(info.changes().size()).append("\n\n");
        if (info.reasons().isEmpty()) {
            out.append("Észlelt változások: nincs\n");
        } else {
            out.append("Észlelt változások:\n");
            info.reasons().stream()
                    .sorted(Comparator.comparingInt((Reason r) -> r.bump().rank).reversed())
                    .limit(40)
                    .forEach(reason -> out.append("  ").append(String.format("%-5s", reason.bump())).append("  ").append(reason.text()).append("\n"));
        }
        out.append("\nDöntés: ").append(info.effective());
        if (info.detected() != info.effective()) {
            out.append(" (az automatikus döntés: ").append(info.detected()).append(")");
        }
        out.append("\nKövetkező verzió: ").append(info.next()).append("\n");
        out.append("Build azonosító: ").append(info.next()).append('+').append(info.timestamp().replace("-", ".")).append("\n");
        out.append("Commit: ").append(info.commit()).append(info.dirty() ? " (nem tiszta munkakönyvtár)" : "").append("\n");
        return out.toString();
    }

    private static String git(Path repo, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command)
                .directory(repo.toFile())
                .start();

        CompletableFuture<byte[]> stdoutFuture = readProcessStream(process.getInputStream());
        CompletableFuture<byte[]> stderrFuture = readProcessStream(process.getErrorStream());
        int exit = process.waitFor();

        String stdout = decodeProcessStream(stdoutFuture);
        String stderr = decodeProcessStream(stderrFuture);
        if (exit != 0) {
            String details = stderr.isBlank() ? stdout : stderr + (stdout.isBlank() ? "" : "\n" + stdout);
            throw new IllegalStateException("A Git parancs sikertelen (" + String.join(" ", command) + "):\n" + details);
        }
        if (!stderr.isBlank()) {
            System.err.print(stderr);
            if (!stderr.endsWith("\n")) {
                System.err.println();
            }
        }
        return stdout;
    }

    private static CompletableFuture<byte[]> readProcessStream(java.io.InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (stream) {
                return stream.readAllBytes();
            } catch (IOException ex) {
                throw new CompletionException(ex);
            }
        });
    }

    private static String decodeProcessStream(CompletableFuture<byte[]> future) throws IOException {
        try {
            return new String(future.join(), StandardCharsets.UTF_8);
        } catch (CompletionException ex) {
            if (ex.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw ex;
        }
    }

    private enum Bump {
        AUTO(-1), NONE(0), PATCH(1), MINOR(2), MAJOR(3);
        private final int rank;
        Bump(int rank) { this.rank = rank; }
    }

    private record SemVer(int major, int minor, int patch) implements Comparable<SemVer> {
        static SemVer parse(String value) {
            Matcher matcher = SEMVER_TAG.matcher(value.trim());
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Érvénytelen szemantikus verzió: " + value);
            }
            return fromMatcher(matcher);
        }
        static SemVer fromMatcher(Matcher matcher) {
            return new SemVer(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        }
        @Override
        public int compareTo(SemVer other) {
            int majorCompare = Integer.compare(major, other.major);
            if (majorCompare != 0) return majorCompare;
            int minorCompare = Integer.compare(minor, other.minor);
            if (minorCompare != 0) return minorCompare;
            return Integer.compare(patch, other.patch);
        }
        SemVer bump(Bump bump) {
            return switch (bump) {
                case MAJOR -> new SemVer(major + 1, 0, 0);
                case MINOR -> new SemVer(major, minor + 1, 0);
                case PATCH -> new SemVer(major, minor, patch + 1);
                case NONE, AUTO -> this;
            };
        }
        @Override public String toString() { return major + "." + minor + "." + patch; }
    }

    private record Baseline(String ref, SemVer version) { }
    private record Change(String status, String path, boolean untracked) { }
    private record Reason(Bump bump, String text) { }
    private record ConfigDelta(boolean added, boolean removed) { }
    private record BuildInfo(SemVer base, String baseRef, Bump detected, Bump effective, SemVer next,
                             OffsetDateTime now, String timestamp, String commit, boolean dirty,
                             List<Change> changes, List<Reason> reasons) { }

    private record Options(Path repo, String baseRef, String baseVersion, Bump override, Path outputDir) {
        static Options parse(String[] args) {
            Path repo = Path.of(".");
            String baseRef = null;
            String baseVersion = null;
            Bump override = Bump.AUTO;
            Path outputDir = Path.of("target/generated-version");
            for (String arg : args) {
                if (arg.startsWith("--repo=")) repo = Path.of(arg.substring("--repo=".length()));
                else if (arg.startsWith("--base-ref=")) baseRef = arg.substring("--base-ref=".length());
                else if (arg.startsWith("--base-version=")) baseVersion = arg.substring("--base-version=".length());
                else if (arg.startsWith("--override=")) override = Bump.valueOf(arg.substring("--override=".length()).toUpperCase(Locale.ROOT));
                else if (arg.startsWith("--output-dir=")) outputDir = Path.of(arg.substring("--output-dir=".length()));
                else if (arg.equals("--help") || arg.equals("-h")) {
                    System.out.println("Használat: VersioningTool [--repo=.] [--base-ref=v1.2.3] [--base-version=1.2.3] [--override=auto|major|minor|patch|none] [--output-dir=target/generated-version]");
                    System.exit(0);
                } else {
                    throw new IllegalArgumentException("Ismeretlen argumentum: " + arg);
                }
            }
            return new Options(repo, baseRef, baseVersion, override, outputDir);
        }
    }
}
