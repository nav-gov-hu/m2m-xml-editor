package hu.gov.nav.xsdparsertool.core.support;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tulajdonosra korlátozott jogosultságokkal végzett fájl- és könyvtárműveletek központi segédosztálya.
 *
 * <p>POSIX fájlrendszeren explicit jogosultsági attribútumokat használ, más platformokon
 * ACL-alapú, végső esetben {@link java.io.File} alapú best-effort korlátozást alkalmaz.
 * A cél, hogy az alkalmazás által létrehozott érzékeny állományok alapértelmezésben ne legyenek más felhasználók számára hozzáférhetők.</p>
 */
public final class SecureFileOperations {

    private static final Set<PosixFilePermission> PRIVATE_FILE_PERMISSIONS = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE);
    private static final Set<PosixFilePermission> PRIVATE_DIRECTORY_PERMISSIONS = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE);
    private static final FileAttribute<Set<PosixFilePermission>> PRIVATE_FILE_ATTRIBUTE =
            PosixFilePermissions.asFileAttribute(PRIVATE_FILE_PERMISSIONS);
    private static final FileAttribute<Set<PosixFilePermission>> PRIVATE_DIRECTORY_ATTRIBUTE =
            PosixFilePermissions.asFileAttribute(PRIVATE_DIRECTORY_PERMISSIONS);

    /**
     * Privát konstruktor; a SecureFileOperations segédosztály példányosítását megakadályozza.
     */
    private SecureFileOperations() {
    }

    /**
     * Létrehozza a teljes könyvtárhierarchiát tulajdonosra korlátozott jogosultságokkal.
     *
     * <p>POSIX fájlrendszeren létrehozási attribútumként owner-only jogosultságot kér; más platformon létrehozás után alkalmazza a projekt ACL/fallback korlátozását. Már létező könyvtár esetén nem módosítja annak jogosultságait.</p>
     * @param path a létrehozandó könyvtár útvonala
     * @return a normalizált könyvtárútvonal
     * @throws IOException ha az útvonal hiányzik vagy a létrehozás sikertelen
     */
    public static Path createPrivateDirectories(Path path) throws IOException {
        if (path == null) {
            throw new IOException("A könyvtár elérési útja hiányzik.");
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (Files.exists(normalized)) {
            return normalized;
        }
        if (supportsPosix(normalized)) {
            return Files.createDirectories(normalized, PRIVATE_DIRECTORY_ATTRIBUTE);
        }
        Path created = Files.createDirectories(normalized);
        restrictOwnerOnly(created, true);
        return created;
    }

    /**
     * Egyetlen könyvtárat hoz létre tulajdonosra korlátozott jogosultságokkal.
     *
     * <p>A szülőkönyvtárnak már léteznie kell. POSIX rendszeren explicit permission attribútumot, más platformon ACL/fallback jogosultságkorlátozást használ.</p>
     * @param path a létrehozandó könyvtár útvonala
     * @return a létrehozott, normalizált könyvtárútvonal
     * @throws IOException ha a könyvtár nem hozható létre
     */
    public static Path createPrivateDirectory(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        Path created;
        if (supportsPosix(normalized)) {
            created = Files.createDirectory(normalized, PRIVATE_DIRECTORY_ATTRIBUTE);
            Files.setPosixFilePermissions(created, PRIVATE_DIRECTORY_PERMISSIONS);
        } else {
            created = Files.createDirectory(normalized);
            restrictOwnerOnly(created, true);
        }
        return created;
    }

    /**
     * Privát jogosultságú ideiglenes fájlt hoz létre a JVM temp könyvtárában.
     *
     * <p>POSIX fájlrendszeren már a létrehozáskor owner read/write jogosultságot kér, más platformon közvetlenül a létrehozás után szűkíti a hozzáférést.</p>
     * @param prefix az ideiglenes fájlnév előtagja
     * @param suffix az ideiglenes fájlnév utótagja
     * @return a létrehozott ideiglenes fájl útvonala
     * @throws IOException ha a fájl nem hozható létre
     */
    public static Path createPrivateTempFile(String prefix, String suffix) throws IOException {
        Path tempDirectory = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
        if (supportsPosix(tempDirectory)) {
            return Files.createTempFile(prefix, suffix, PRIVATE_FILE_ATTRIBUTE);
        }
        Path created = Files.createTempFile(prefix, suffix);
        restrictOwnerOnly(created, false);
        return created;
    }


    /**
     * Fájlt vagy stream tartalmát másol a célútvonalra, és új cél esetén privát jogosultságot alkalmaz.
     *
     * <p>A célútvonal szülőkönyvtárát szükség esetén privát könyvtárként létrehozza. Már létező cél jogosultságait nem írja át.</p>
     * @param source a másolás forrása
     * @param target a célútvonal
     * @param options a {@link Files#copy} művelet opciói
     * @return a másolt célútvonal
     * @throws IOException ha a másolás vagy a jogosultságkezelés sikertelen
     */
    public static Path copyPrivate(Path source, Path target, CopyOption... options) throws IOException {
        Path normalizedTarget = target.toAbsolutePath().normalize();
        ensureParent(normalizedTarget);
        boolean existed = Files.exists(normalizedTarget);
        Path copied = Files.copy(source, normalizedTarget, options);
        if (!existed) {
            restrictOwnerOnly(copied, Files.isDirectory(copied));
        }
        return copied;
    }

    /**
     * Fájlt vagy stream tartalmát másol a célútvonalra, és új cél esetén privát jogosultságot alkalmaz.
     *
     * <p>A célútvonal szülőkönyvtárát szükség esetén privát könyvtárként létrehozza. Már létező cél jogosultságait nem írja át.</p>
     * @param source a másolás forrása
     * @param target a célútvonal
     * @param options a {@link Files#copy} művelet opciói
     * @return a másolt bájtok száma
     * @throws IOException ha a másolás vagy a jogosultságkezelés sikertelen
     */
    public static long copyPrivate(InputStream source, Path target, CopyOption... options) throws IOException {
        Path normalizedTarget = target.toAbsolutePath().normalize();
        ensureParent(normalizedTarget);
        boolean existed = Files.exists(normalizedTarget);
        long copied = Files.copy(source, normalizedTarget, options);
        if (!existed) {
            restrictOwnerOnly(normalizedTarget, false);
        }
        return copied;
    }

    /**
     * Áthelyez egy fájlt vagy könyvtárat a célútvonalra, és új cél esetén privát jogosultságot alkalmaz.
     *
     * <p>A cél szülőkönyvtárát szükség esetén létrehozza. Már létező cél jogosultságait nem módosítja.</p>
     * @param source az áthelyezendő forrásútvonal
     * @param target a célútvonal
     * @param options a {@link Files#move} opciói
     * @return az áthelyezett célútvonal
     * @throws IOException ha az áthelyezés vagy a jogosultságkezelés sikertelen
     */
    public static Path movePrivate(Path source, Path target, CopyOption... options) throws IOException {
        Path normalizedTarget = target.toAbsolutePath().normalize();
        ensureParent(normalizedTarget);
        boolean existed = Files.exists(normalizedTarget);
        Path moved = Files.move(source, normalizedTarget, options);
        if (!existed) {
            restrictOwnerOnly(moved, Files.isDirectory(moved));
        }
        return moved;
    }

    /**
     * Privát fájlba író output streamet nyit.
     *
     * <p>Az írási opciókat normalizálja: explicit opciók hiányában CREATE, TRUNCATE_EXISTING és WRITE használatos; megadott opcióknál a WRITE és CREATE biztosított, kivéve CREATE_NEW esetén. Új fájl létrehozásakor tulajdonosra korlátozza a hozzáférést.</p>
     * @param path az írandó fájl útvonala
     * @param options a megnyitási opciók
     * @return a megnyitott output stream
     * @throws IOException ha a fájl nem nyitható meg vagy a jogosultság nem állítható be
     */
    public static OutputStream newPrivateOutputStream(Path path, OpenOption... options) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        ensureParent(normalized);
        boolean existed = Files.exists(normalized);
        Set<OpenOption> openOptions = normalizeWriteOptions(options);
        if (supportsPosix(normalized)) {
            SeekableByteChannel channel = Files.newByteChannel(normalized, openOptions, PRIVATE_FILE_ATTRIBUTE);
            if (!existed) {
                Files.setPosixFilePermissions(normalized, PRIVATE_FILE_PERMISSIONS);
            }
            return Channels.newOutputStream(channel);
        }
        OutputStream out = Files.newOutputStream(normalized, openOptions.toArray(OpenOption[]::new));
        if (!existed) {
            restrictOwnerOnly(normalized, false);
        }
        return out;
    }

    /**
     * Pufferelt karakteres írót nyit privát fájlműveleten keresztül.
     *
     * <p>A jogosultsági és megnyitási szabályokat a {@link #newPrivateOutputStream(Path, OpenOption...)} biztosítja.</p>
     * @param path az írandó fájl útvonala
     * @param charset a karakterkódolás
     * @param options a megnyitási opciók
     * @return a megnyitott BufferedWriter
     * @throws IOException ha a fájl nem nyitható meg
     */
    public static BufferedWriter newPrivateBufferedWriter(Path path, Charset charset, OpenOption... options)
            throws IOException {
        return new BufferedWriter(new OutputStreamWriter(newPrivateOutputStream(path, options), charset));
    }

    /**
     * Teljes karakterláncot ír privát fájlba a megadott karakterkódolással.
     *
     * <p>{@code null} tartalom esetén üres szöveget ír. A writer automatikusan lezárásra kerül.</p>
     * @param path a célfájl útvonala
     * @param content a kiírandó tartalom
     * @param charset a karakterkódolás
     * @param options a megnyitási opciók
     * @return a célfájl útvonala
     * @throws IOException ha az írás sikertelen
     */
    public static Path writePrivateString(Path path, CharSequence content, Charset charset, OpenOption... options)
            throws IOException {
        try (Writer writer = newPrivateBufferedWriter(path, charset, options)) {
            writer.append(content == null ? "" : content);
        }
        return path;
    }

    /**
     * Karakterláncok sorozatát írja privát fájlba soronként.
     *
     * <p>A sorok között platformfüggetlen writer sortörést használ, nem üres bemenet esetén a fájlt záró sortöréssel fejezi be; {@code null} sor üres sorként íródik ki.</p>
     * @param path a célfájl útvonala
     * @param lines a kiírandó sorok
     * @param charset a karakterkódolás
     * @param options a megnyitási opciók
     * @return a célfájl útvonala
     * @throws IOException ha az írás sikertelen
     */
    public static Path writePrivateLines(Path path, Iterable<? extends CharSequence> lines, Charset charset,
                                         OpenOption... options) throws IOException {
        try (BufferedWriter writer = newPrivateBufferedWriter(path, charset, options)) {
            boolean first = true;
            for (CharSequence line : lines) {
                if (!first) {
                    writer.newLine();
                }
                writer.append(line == null ? "" : line);
                first = false;
            }
            if (!first) {
                writer.newLine();
            }
        }
        return path;
    }

    /**
     * A megadott fájl vagy könyvtár hozzáférését a tulajdonosra próbálja korlátozni.
     *
     * <p>Prioritás: POSIX permission beállítás; ha ez nem érhető el, tulajdonosi ACL; végső fallbackként {@link File} jogosultság-beállítások. Nem létező vagy null útvonal esetén nincs művelet.</p>
     * @param path a korlátozandó útvonal
     * @param directory true, ha könyvtárjogosultságot kell alkalmazni
     * @throws IOException ha a támogatott jogosultsági mechanizmus beállítása sikertelen
     */
    public static void restrictOwnerOnly(Path path, boolean directory) throws IOException {
        if (path == null || !Files.exists(path)) {
            return;
        }
        if (supportsPosix(path)) {
            Files.setPosixFilePermissions(path,
                    directory ? PRIVATE_DIRECTORY_PERMISSIONS : PRIVATE_FILE_PERMISSIONS);
            return;
        }
        AclFileAttributeView aclView = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (aclView != null) {
            AclEntry ownerEntry = AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(aclView.getOwner())
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                    .build();
            aclView.setAcl(List.of(ownerEntry));
            return;
        }

        // Last-resort fallback for providers exposing neither POSIX nor ACL views.
        // java.io.File permission setters are best-effort on such file systems.
        File file = path.toFile();
        file.setReadable(false, false);
        file.setWritable(false, false);
        file.setExecutable(false, false);
        file.setReadable(true, true);
        file.setWritable(true, true);
        if (directory) {
            file.setExecutable(true, true);
        }
    }

    /**
     * Biztosítja, hogy a célútvonal szülőkönyvtára létezzen.
     *
     * <p>Hiányzó szülő esetén azt a {@link #createPrivateDirectories(Path)} szabályai szerint hozza létre.</p>
     * @param path az a célútvonal, amelynek szülőjét ellenőrizni kell
     * @throws IOException ha a hiányzó szülőkönyvtár nem hozható létre
     */
    private static void ensureParent(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            createPrivateDirectories(parent);
        }
    }

    /**
     * Normalizálja a privát fájlíráshoz kapott megnyitási opciókat.
     *
     * <p>Üres bemenetnél CREATE, TRUNCATE_EXISTING és WRITE kerül beállításra. Megadott opcióknál mindig hozzáadja a WRITE opciót, és CREATE_NEW hiányában a CREATE opciót is.</p>
     * @param options a hívótól érkező megnyitási opciók
     * @return a normalizált, módosítható opcióhalmaz
     */
    private static Set<OpenOption> normalizeWriteOptions(OpenOption... options) {
        Set<OpenOption> result = new HashSet<>();
        if (options != null) {
            for (OpenOption option : options) {
                if (option != null) {
                    result.add(option);
                }
            }
        }
        if (result.isEmpty()) {
            result.add(StandardOpenOption.CREATE);
            result.add(StandardOpenOption.TRUNCATE_EXISTING);
            result.add(StandardOpenOption.WRITE);
        } else {
            result.add(StandardOpenOption.WRITE);
            if (!result.contains(StandardOpenOption.CREATE_NEW)) {
                result.add(StandardOpenOption.CREATE);
            }
        }
        return result;
    }

    /**
     * Megállapítja, hogy az útvonalhoz tartozó fájlrendszer támogatja-e a POSIX fájlattribútumokat.
     *
     * <p>A vizsgálat a legközelebbi létező ősútvonal FileStore-ján történik. I/O vagy biztonsági hiba esetén {@code false} az eredmény.</p>
     * @param path a vizsgált útvonal
     * @return true, ha a kapcsolódó FileStore POSIX attribútumokat támogat
     */
    private static boolean supportsPosix(Path path) {
        Path probe = nearestExistingPath(path);
        if (probe == null) {
            return false;
        }
        try {
            FileStore store = Files.getFileStore(probe);
            return store.supportsFileAttributeView(PosixFileAttributeView.class);
        } catch (IOException | SecurityException ex) {
            return false;
        }
    }

    /**
     * Megkeresi a megadott útvonal legközelebbi létező elemét önmagától a gyökér felé haladva.
     *
     * <p>Erre akkor van szükség, amikor a létrehozandó cél még nem létezik, de a fájlrendszer képességeit egy létező ősön kell lekérdezni. SecurityException esetén null-lal tér vissza.</p>
     * @param path a kiinduló útvonal
     * @return a legközelebbi létező útvonal, vagy null, ha ilyen nem állapítható meg
     */
    private static Path nearestExistingPath(Path path) {
        Path current = path == null ? null : path.toAbsolutePath().normalize();
        while (current != null) {
            try {
                if (Files.exists(current)) {
                    return current;
                }
            } catch (SecurityException ignored) {
                return null;
            }
            current = current.getParent();
        }
        return null;
    }
}
