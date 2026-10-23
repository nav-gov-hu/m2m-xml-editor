package hu.gov.nav.xsdparsertool.web.database.releaselock;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A teljes alkalmazás-release azonosító összehasonlítható reprezentációja.
 *
 * <p>A támogatott kiadási forma: {@code major.minor.patch-yyyyMMdd-HHmmss}
 * opcionális {@code -RCn} utótaggal. Az összehasonlítás először a szemantikus
 * verziót, majd a build-időbélyeget, végül az RC sorszámot vizsgálja. Azonos
 * verzió és időbélyeg mellett a végleges kiadás újabb az RC kiadásnál.</p>
 */
final class ApplicationReleaseId implements Comparable<ApplicationReleaseId> {

    private static final Pattern RELEASE_PATTERN = Pattern.compile(
            "^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-(\\d{8})-(\\d{6})(?:-RC(\\d+))?)?$",
            Pattern.CASE_INSENSITIVE);

    private final String source;
    private final int major;
    private final int minor;
    private final int patch;
    private final long timestamp;
    private final Integer releaseCandidate;

    private ApplicationReleaseId(String source,
                                 int major,
                                 int minor,
                                 int patch,
                                 long timestamp,
                                 Integer releaseCandidate) {
        this.source = source;
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.timestamp = timestamp;
        this.releaseCandidate = releaseCandidate;
    }

    /**
     * Feldolgozza a teljes release azonosítót.
     *
     * @param value teljes release azonosító
     * @return feldolgozott release
     * @throws IllegalArgumentException ha az azonosító formátuma nem támogatott
     */
    static ApplicationReleaseId parse(String value) {
        String normalized = Objects.requireNonNull(value, "A release azonosító nem lehet null.").trim();
        Matcher matcher = RELEASE_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Nem támogatott teljes alkalmazás-release azonosító: " + value);
        }

        String date = matcher.group(4);
        String time = matcher.group(5);
        long timestamp = date == null ? 0L : Long.parseLong(date + time);
        Integer releaseCandidate = matcher.group(6) == null ? null : Integer.valueOf(matcher.group(6));

        return new ApplicationReleaseId(
                normalized,
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)),
                timestamp,
                releaseCandidate);
    }

    /**
     * Megadja, hogy az érték megfelel-e a támogatott teljes release-formátumnak.
     *
     * @param value vizsgált érték
     * @return {@code true}, ha feldolgozható release azonosító
     */
    static boolean isSupported(String value) {
        return value != null && RELEASE_PATTERN.matcher(value.trim()).matches();
    }

    @Override
    public int compareTo(ApplicationReleaseId other) {
        int result = Integer.compare(major, other.major);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(minor, other.minor);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(patch, other.patch);
        if (result != 0) {
            return result;
        }
        result = Long.compare(timestamp, other.timestamp);
        if (result != 0) {
            return result;
        }
        if (releaseCandidate == null && other.releaseCandidate == null) {
            return 0;
        }
        if (releaseCandidate == null) {
            return 1;
        }
        if (other.releaseCandidate == null) {
            return -1;
        }
        return Integer.compare(releaseCandidate, other.releaseCandidate);
    }

    @Override
    public String toString() {
        return source;
    }
}
