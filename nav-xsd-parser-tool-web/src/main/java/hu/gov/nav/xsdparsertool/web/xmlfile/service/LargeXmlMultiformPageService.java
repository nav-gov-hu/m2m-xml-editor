package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.core.support.SecureFileOperations;
import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.core.xml.SecureXmlParserSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;
import hu.gov.nav.xsdparsertool.web.xmlindex.dto.XmlIndexDtos.IndexFieldDto;
import hu.gov.nav.xsdparsertool.web.xmlindex.dto.XmlIndexDtos.StructureResponse;
import hu.gov.nav.xsdparsertool.web.xmlindex.service.XmlIndexConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code LargeXmlMultiformPageService} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class LargeXmlMultiformPageService {
    private static final int MAX_INDEX_FRAGMENT_BYTES = 16 * 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(LargeXmlMultiformPageService.class);
    private static final int MAX_LABEL_VALUES = 5;

    private final XmlFileRepository xmlFileRepository;
    private final XmlIndexConfigService xmlIndexConfigService;
    private final ObjectMapper objectMapper;
    private final XmlFileStorageProperties storageProperties;
    private final Map<String, CachedIndex> memoryCache = new ConcurrentHashMap<>();

    /**
     * Létrehozza a {@code LargeXmlMultiformPageService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param xmlFileRepository a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param xmlIndexConfigService a feldolgozandó XML-hez tartozó adat vagy tartalom
     * @param objectMapper a művelet bemeneti {@code objectMapper} értéke
     * @param storageProperties a művelethez szükséges konfigurációs adatok
     */
    public LargeXmlMultiformPageService(XmlFileRepository xmlFileRepository,
                                        XmlIndexConfigService xmlIndexConfigService,
                                        ObjectMapper objectMapper,
                                        XmlFileStorageProperties storageProperties) {
        this.xmlFileRepository = xmlFileRepository;
        this.xmlIndexConfigService = xmlIndexConfigService;
        this.objectMapper = objectMapper;
        this.storageProperties = storageProperties;
    }

    /**
     * A {@code page} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @param page a lapozási vagy mennyiségi korlátot meghatározó érték
     * @param size a lapozási vagy mennyiségi korlátot meghatározó érték
     * @param query a művelet bemeneti {@code query} értéke
     * @return a művelet feldolgozási eredménye
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    public PageResult page(Long xmlFileId, String formName, int page, int size, String query) throws Exception {
        String requestedFormName = requireSafeFormName(formName);
        XmlFileEntity file = requireFile(xmlFileId);
        Path source = Path.of(file.getFilePath());
        String safeFormName = resolveExistingFormName(source, requestedFormName);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(100, size));
        long from = (long) safePage * safeSize;
        String normalizedQuery = normalize(query);

        CachedIndex index = loadOrBuildIndex(file, source, safeFormName);
        java.util.regex.Pattern queryPattern = normalizedQuery.isEmpty()
                ? null
                : java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(normalizedQuery),
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);
        List<IndexEntry> matches = queryPattern == null
                ? index.entries
                : index.entries.stream().filter(entry -> queryPattern.matcher(entry.searchText).find()).toList();

        List<RowResult> rows = new ArrayList<>();
        long to = Math.min(matches.size(), from + safeSize);
        if (from < matches.size()) {
            try (RandomAccessFile fragments = new RandomAccessFile(index.dataPath.toFile(), "r")) {
                for (long i = from; i < to; i++) {
                    IndexEntry entry = matches.get((int) i);
                    byte[] xmlBytes = new byte[entry.fragmentLength];
                    fragments.seek(entry.fragmentOffset);
                    fragments.readFully(xmlBytes);
                    rows.add(new RowResult(entry.occurrence, entry.label, entry.values, new String(xmlBytes, StandardCharsets.UTF_8)));
                }
            }
        }
        List<IndexFieldDto> displayFields = index.fields.stream().filter(IndexFieldDto::isDisplay).toList();
        if (displayFields.isEmpty()) {
            displayFields = index.fields;
        }
        List<ColumnResult> columns = displayFields.stream()
                .map(field -> new ColumnResult(field.getName(), field.getLabel())).toList();
        log.info("Multiform melléklap oldal lekérdezés: xmlFileId={}, page={}, size={}, total={}, returnedRows={}, columnCount={}",
                xmlFileId, safePage, safeSize, matches.size(), rows.size(), columns.size());
        return new PageResult(rows, columns, matches.size(), safePage, safeSize, to < matches.size());
    }

    /**
     * A célzott fragment-mentés után csak az érintett indexrekordot frissíti.
     * A teljes XML indexelése nem fut újra; az új forrás-hash egyszer kerül kiszámításra.
     */
    public synchronized void refreshAfterSave(Long xmlFileId, String formName, long occurrence, String xmlFragment) throws Exception {
        String requestedFormName = requireSafeFormName(formName);
        XmlFileEntity file = requireFile(xmlFileId);
        Path source = Path.of(file.getFilePath());
        String safeFormName = resolveExistingFormName(source, requestedFormName);
        String key = cacheKey(xmlFileId, safeFormName);
        CachedIndex cached = memoryCache.get(key);
        if (cached == null) return;

        ExtractedValues extracted = extractValues(xmlFragment, cached.fields);
        byte[] bytes = xmlFragment.getBytes(StandardCharsets.UTF_8);
        long offset;
        try (RandomAccessFile data = new RandomAccessFile(cached.dataPath.toFile(), "rw")) {
            offset = data.length();
            data.seek(offset);
            data.write(bytes);
        }
        int listIndex = Math.toIntExact(occurrence - 1);
        if (listIndex < 0 || listIndex >= cached.entries.size()) {
            memoryCache.remove(key);
            return;
        }
        IndexEntry replacement = new IndexEntry(occurrence,
                occurrence + " - " + extracted.label,
                extracted.searchText,
                extracted.values,
                offset,
                bytes.length);
        cached.entries.set(listIndex, replacement);
        cached.sourceSize = Files.size(source);
        cached.sourceLastModified = Files.getLastModifiedTime(source).toMillis();
        cached.sourceSha256 = sha256(source);
        writeMetadata(cached);
    }

    /**
     * A {@code invalidate} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az ellenőrzési eredményt a webes megjelenítés és a további üzleti döntések számára konzisztens formában állítja elő.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     */
    public void invalidate(Long xmlFileId, String formName) {
        memoryCache.remove(cacheKey(xmlFileId, requireSafeFormName(formName)));
    }

    /**
     * A {@code removeAllForFile} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param source a művelet bemeneti {@code source} értéke
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    public void removeAllForFile(Long xmlFileId, Path source) throws IOException {
        String prefix = xmlFileId + ":";
        memoryCache.keySet().removeIf(key -> key.startsWith(prefix));
        Path indexDir = xmlIndexDirectory(xmlFileId);
        deleteRecursively(indexDir);
    }

    /**
     * A {@code deleteRecursively} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @throws IOException ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void deleteRecursively(Path path) throws IOException {
        if (path == null || !ExceptionSafeOperations.fileExists(path)) return;
        try (var stream = Files.walk(path)) {
            for (Path item : stream.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(item);
            }
        }
    }

    /**
     * A {@code resolveExistingFormName} művelet feloldja a megfelelő erőforrást, állapotot vagy értéket a rendelkezésre álló jelöltek közül.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param source a művelet bemeneti {@code source} értéke
     * @param requestedFormName a művelet bemeneti kérésadatait tartalmazó objektum
     * @return a feloldott vagy lekért érték
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private String resolveExistingFormName(Path source, String requestedFormName) throws Exception {
        XMLInputFactory factory = secureInputFactory();
        try (InputStream input = Files.newInputStream(source)) {
            XMLEventReader reader = factory.createXMLEventReader(input);
            int depth = 0;
            try {
                while (reader.hasNext()) {
                    XMLEvent event = reader.nextEvent();
                    if (event.isStartElement()) {
                        depth++;
                        if (depth == 2) {
                            String discovered = event.asStartElement().getName().getLocalPart();
                            if (requestedFormName.equals(discovered)) return discovered;
                        }
                    } else if (event.isEndElement()) {
                        depth--;
                    }
                }
            } finally {
                reader.close();
            }
        }
        throw new IllegalArgumentException("A kért űrlaprész nem található az XML-ben: " + requestedFormName);
    }

    /**
     * A {@code loadOrBuildIndex} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @param source a művelet bemeneti {@code source} értéke
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @return a feloldott vagy lekért érték
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private CachedIndex loadOrBuildIndex(XmlFileEntity file, Path source, String formName) throws Exception {
        String key = cacheKey(file.getId(), formName);
        long size = Files.size(source);
        long modified = Files.getLastModifiedTime(source).toMillis();
        CachedIndex cached = memoryCache.get(key);
        if (cached != null && cached.sourceSize == size && cached.sourceLastModified == modified) return cached;

        IndexFieldSelection selection = indexFieldSelection(file, formName);
        Path indexDir = xmlIndexDirectory(file.getId());
        ExceptionSafeOperations.createDirectories(indexDir);
        Path metaPath = indexDir.resolve(formName + ".index.json");
        Path dataPath = indexDir.resolve(formName + ".fragments.bin");
        String sourceHash = sha256(source);

        if (ExceptionSafeOperations.isRegularFile(metaPath) && ExceptionSafeOperations.isRegularFile(dataPath)) {
            IndexMetadata metadata = objectMapper.readValue(metaPath.toFile(), IndexMetadata.class);
            if (Objects.equals(metadata.sourceSha256, sourceHash)
                    && Objects.equals(metadata.configurationHash, selection.configurationHash)
                    && metadata.sourceSize == size) {
                CachedIndex loaded = new CachedIndex(metaPath, dataPath, metadata.entries,
                        selection.fields, size, modified, sourceHash, selection.configurationHash);
                memoryCache.put(key, loaded);
                return loaded;
            }
        }

        CachedIndex built = buildIndex(source, indexDir, metaPath, dataPath, formName,
                selection, size, modified, sourceHash);
        memoryCache.put(key, built);
        return built;
    }

    /**
     * A {@code buildIndex} művelet előállítja a hívó réteg által használt reprezentációt.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param source a művelet bemeneti {@code source} értéke
     * @param indexDir a művelet bemeneti {@code indexDir} értéke
     * @param metaPath a feldolgozásban részt vevő fájl vagy elérési út
     * @param dataPath a feldolgozásban részt vevő fájl vagy elérési út
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @param selection a művelet bemeneti {@code selection} értéke
     * @param sourceSize a lapozási vagy mennyiségi korlátot meghatározó érték
     * @param sourceLastModified a művelet bemeneti {@code sourceLastModified} értéke
     * @param sourceHash a művelet bemeneti {@code sourceHash} értéke
     * @return a művelet feldolgozási eredménye
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private CachedIndex buildIndex(Path source, Path indexDir, Path metaPath, Path dataPath,
                                   String formName, IndexFieldSelection selection,
                                   long sourceSize, long sourceLastModified, String sourceHash) throws Exception {
        Path tempData = indexDir.resolve(formName + ".fragments.bin.tmp");
        List<IndexEntry> entries = new ArrayList<>();
        XMLInputFactory inputFactory = secureInputFactory();
        XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        long occurrence = 0;

        try (InputStream input = Files.newInputStream(source);
             RandomAccessFile data = new RandomAccessFile(tempData.toFile(), "rw")) {
            data.setLength(0);
            XMLEventReader reader = inputFactory.createXMLEventReader(input);
            int depth = 0;
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) {
                    depth++;
                    StartElement start = event.asStartElement();
                    if (depth == 2 && formName.equals(start.getName().getLocalPart())) {
                        occurrence++;
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream(16384);
                        XMLEventWriter writer = outputFactory.createXMLEventWriter(buffer, "UTF-8");
                        writer.add(event);
                        int formDepth = depth;
                        Map<String, String> values = new LinkedHashMap<>();
                        List<String> fallbackValues = new ArrayList<>();
                        String activeField = null;
                        while (reader.hasNext()) {
                            XMLEvent nested = reader.nextEvent();
                            writer.add(nested);
                            if (nested.isStartElement()) {
                                depth++;
                                activeField = nested.asStartElement().getName().getLocalPart();
                            } else if (nested.isCharacters()) {
                                String text = nested.asCharacters().getData().trim();
                                if (!text.isEmpty()) {
                                    if (activeField != null && selection.fieldNames.contains(activeField)) {
                                        values.merge(activeField, text, (left, right) -> left + " " + right);
                                    }
                                    if (fallbackValues.size() < MAX_LABEL_VALUES) fallbackValues.add(text);
                                }
                            } else if (nested.isEndElement()) {
                                activeField = null;
                                depth--;
                                if (depth < formDepth) break;
                            }
                        }
                        writer.flush();
                        writer.close();
                        byte[] fragment = buffer.toByteArray();
                        if (fragment.length > MAX_INDEX_FRAGMENT_BYTES) {
                            throw new IOException("Egy multiform rekord indexelendő XML fragmentuma túl nagy.");
                        }
                        long offset = data.length();
                        data.seek(offset);
                        data.write(fragment);
                        ExtractedValues extracted = composeValues(values, fallbackValues, selection.fields);
                        entries.add(new IndexEntry(occurrence,
                                occurrence + " - " + extracted.label,
                                extracted.searchText,
                                extracted.values,
                                offset,
                                fragment.length));
                    }
                } else if (event.isEndElement()) {
                    depth--;
                }
            }
            reader.close();
        } catch (Exception ex) {
            Files.deleteIfExists(tempData);
            throw ex;
        }
        try {
            SecureFileOperations.movePrivate(tempData, dataPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            SecureFileOperations.movePrivate(tempData, dataPath, StandardCopyOption.REPLACE_EXISTING);
        }
        CachedIndex result = new CachedIndex(metaPath, dataPath, entries, selection.fields,
                sourceSize, sourceLastModified, sourceHash, selection.configurationHash);
        writeMetadata(result);
        return result;
    }

    /**
     * A {@code configurationStatus} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @return a művelet feldolgozási eredménye
     */
    public ConfigurationStatus configurationStatus(Long xmlFileId, String formName) {
        String safeFormName = requireSafeFormName(formName);
        XmlFileEntity file = requireFile(xmlFileId);
        IndexFieldSelection selection = indexFieldSelection(file, safeFormName);
        boolean hasDisplay = selection.fields().stream().anyMatch(IndexFieldDto::isDisplay);
        boolean hasSearchable = selection.fields().stream().anyMatch(field -> field.isSearchable() || field.isDefaultSearch());
        String declaration = resolveDeclarationName(file, safeFormName);
        return new ConfigurationStatus(!(hasDisplay && hasSearchable), declaration, file.getFormVersion(), safeFormName, hasDisplay, hasSearchable);
    }

    /**
     * A {@code indexFieldSelection} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param file a feldolgozásban részt vevő fájl vagy elérési út
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @return a művelet feldolgozási eredménye
     */
    private IndexFieldSelection indexFieldSelection(XmlFileEntity file, String formName) {
        String declaration = resolveDeclarationName(file, formName);
        StructureResponse structure = xmlIndexConfigService.structure(declaration, file.getFormVersion());

        List<IndexFieldDto> parsedConfigured = structure.fields() == null ? List.of() : structure.fields().stream()
                .filter(field -> formName.equals(field.getFormPartName()))
                .filter(field -> field.isSearchable() || field.isDisplay() || field.isDefaultSearch())
                .toList();

        // Egyes összetett XSD-k esetén a struktúrafelderítés nem adja vissza az összes mezőt.
        // A felhasználó által mentett indexkonfiguráció ettől még teljes és elsődleges forrás lehet.
        List<IndexFieldDto> savedConfigured = structure.savedConfig() == null || structure.savedConfig().getFields() == null
                ? List.of()
                : structure.savedConfig().getFields().stream()
                .filter(field -> formName.equals(field.getFormPartName()))
                .filter(field -> field.isSearchable() || field.isDisplay() || field.isDefaultSearch())
                .toList();

        Map<String, IndexFieldDto> merged = new LinkedHashMap<>();
        savedConfigured.forEach(field -> merged.put(fieldIdentity(field), field));
        parsedConfigured.forEach(field -> merged.put(fieldIdentity(field), field));
        List<IndexFieldDto> configured = new ArrayList<>(merged.values());

        log.info("Nagy XML indexmező-kiválasztás: xmlFileId={}, parsedFieldCount={}, savedFieldCount={}, selectedFieldCount={}",
                file.getId(), parsedConfigured.size(), savedConfigured.size(), configured.size());

        if (configured.isEmpty()) {
            log.warn("Nincs használható XML indexmező: xmlFileId={}, parsedFields={}, savedConfigPresent={}",
                    file.getId(), structure.fields() == null ? 0 : structure.fields().size(), structure.savedConfig() != null);
        }

        String configHash = sha256Text("index-schema-v4-saved-config-fallback;" + configured.stream()
                .map(field -> field.getName() + '|' + field.getXmlPath() + '|' + field.isSearchable() + '|' + field.isDisplay() + '|' + field.isDefaultSearch())
                .reduce("", (a, b) -> a + ';' + b));
        List<String> names = configured.stream().map(IndexFieldDto::getName).filter(Objects::nonNull).toList();
        return new IndexFieldSelection(configured, names, configHash);
    }

    /**
     * A {@code fieldIdentity} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param field a művelet bemeneti {@code field} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String fieldIdentity(IndexFieldDto field) {
        if (field == null) return "";
        if (field.getXmlPath() != null && !field.getXmlPath().isBlank()) return field.getXmlPath();
        return Objects.toString(field.getFormPartName(), "") + '|' + Objects.toString(field.getName(), "");
    }

    /**
     * Az XML index konfiguráció űrlapazonosítóját a dokumentum gyökéreleméből
     * határozza meg. A formType üzleti kategória is lehet (például BEVALLAS), ezért
     * nem alkalmas konfigurációs kulcsnak.
     */
    private String resolveDeclarationName(XmlFileEntity file, String formName) {
        String rootElement = file.getRootElement();
        if (rootElement != null && !rootElement.isBlank()) {
            String localName = rootElement.trim();
            int namespaceSeparator = localName.indexOf(':');
            if (namespaceSeparator >= 0 && namespaceSeparator + 1 < localName.length()) {
                localName = localName.substring(namespaceSeparator + 1);
            }
            if (localName.startsWith("Doc_") && localName.length() > 4) {
                return localName.substring(4);
            }
        }
        if (formName != null && !formName.isBlank()) {
            return formName.replaceFirst("^Form_", "").replaceAll("[AM]$", "");
        }
        String formType = file.getFormType();
        return formType == null ? "" : formType.trim();
    }

    /**
     * A {@code extractValues} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param fragment a művelet bemeneti {@code fragment} értéke
     * @param fields a feldolgozandó elemek kollekciója
     * @return a művelet feldolgozási eredménye
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private ExtractedValues extractValues(String fragment, List<IndexFieldDto> fields) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        List<String> fallback = new ArrayList<>();
        XMLInputFactory factory = secureInputFactory();
        try (InputStream input = new java.io.ByteArrayInputStream(fragment.getBytes(StandardCharsets.UTF_8))) {
            XMLEventReader reader = factory.createXMLEventReader(input);
            String active = null;
            List<String> selectedNames = fields.stream().map(IndexFieldDto::getName).toList();
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) active = event.asStartElement().getName().getLocalPart();
                else if (event.isCharacters()) {
                    String text = event.asCharacters().getData().trim();
                    if (!text.isEmpty()) {
                        if (active != null && selectedNames.contains(active)) values.merge(active, text, (a, b) -> a + " " + b);
                        if (fallback.size() < MAX_LABEL_VALUES) fallback.add(text);
                    }
                } else if (event.isEndElement()) active = null;
            }
            reader.close();
        }
        return composeValues(values, fallback, fields);
    }

    /**
     * A {@code composeValues} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param values a művelet bemeneti {@code values} értéke
     * @param fallback a feldolgozandó elemek kollekciója
     * @param fields a feldolgozandó elemek kollekciója
     * @return a művelet feldolgozási eredménye
     */
    private ExtractedValues composeValues(Map<String, String> values, List<String> fallback, List<IndexFieldDto> fields) {
        List<String> display = fields.stream().filter(IndexFieldDto::isDisplay).map(IndexFieldDto::getName)
                .map(values::get).filter(Objects::nonNull).filter(v -> !v.isBlank()).toList();
        if (display.isEmpty()) display = fields.stream().map(IndexFieldDto::getName).map(values::get)
                .filter(Objects::nonNull).filter(v -> !v.isBlank()).limit(MAX_LABEL_VALUES).toList();
        if (display.isEmpty()) display = fallback;
        String label = display.isEmpty() ? "Melléklap" : String.join(" ", display);

        List<String> searchable = fields.stream().filter(field -> field.isSearchable() || field.isDefaultSearch())
                .map(IndexFieldDto::getName).map(values::get).filter(Objects::nonNull).toList();
        if (searchable.isEmpty()) searchable = new ArrayList<>(values.values());
        if (searchable.isEmpty()) searchable = fallback;
        return new ExtractedValues(label, normalize(String.join(" ", searchable)), new LinkedHashMap<>(values));
    }

    /**
     * A {@code writeMetadata} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param index a művelet bemeneti {@code index} értéke
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void writeMetadata(CachedIndex index) throws Exception {
        IndexMetadata metadata = new IndexMetadata();
        metadata.sourceSize = index.sourceSize;
        metadata.sourceLastModified = index.sourceLastModified;
        metadata.sourceSha256 = index.sourceSha256;
        metadata.configurationHash = index.configurationHash;
        metadata.createdAt = Instant.now().toString();
        metadata.entries = index.entries;
        Path temp = index.metaPath.resolveSibling(index.metaPath.getFileName() + ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), metadata);
        try {
            SecureFileOperations.movePrivate(temp, index.metaPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            SecureFileOperations.movePrivate(temp, index.metaPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * A {@code requireFile} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    private XmlFileEntity requireFile(Long xmlFileId) {
        XmlFileEntity file = RepositoryAccess.findById(xmlFileRepository, xmlFileId)
                .orElseThrow(() -> new IllegalArgumentException("Nem található XML állomány: " + xmlFileId));
        Path source = Path.of(file.getFilePath());
        if (!ExceptionSafeOperations.isRegularFile(source)) throw new IllegalArgumentException("Az XML állomány nem található: " + source);
        return file;
    }

    /**
     * A {@code secureInputFactory} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    private XMLInputFactory secureInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        SecureXmlParserSupport.configureSecureXmlInputFactory(factory);
        return factory;
    }

    /**
     * A {@code sha256} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param path a feldolgozásban részt vevő fájl vagy elérési út
     * @return a művelet feldolgozási eredménye
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) if (read > 0) digest.update(buffer, 0, read);
        }
        return hex(digest.digest());
    }

    /**
     * A {@code sha256Text} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String sha256Text(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return hex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * A {@code hex} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param bytes a művelet bemeneti {@code bytes} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }

    /**
     * A {@code normalize} művelet feldolgozza és normalizálja a bemeneti adatot a további feldolgozás számára.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param value a művelet bemeneti {@code value} értéke
     * @return a művelet feldolgozási eredménye
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }


    /**
     * A {@code requireSafeFormName} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @return a művelet feldolgozási eredménye
     */
    private String requireSafeFormName(String formName) {
        String value = formName == null ? "" : formName.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Az űrlaprész neve kötelező.");
        }
        if (value.indexOf('/') >= 0 || value.indexOf('\\') >= 0 || ".".equals(value) || "..".equals(value)) {
            throw new IllegalArgumentException("Érvénytelen űrlaprész név.");
        }
        Path segment = Path.of(value);
        if (segment.isAbsolute() || segment.getNameCount() != 1 || !value.equals(segment.getFileName().toString())) {
            throw new IllegalArgumentException("Érvénytelen űrlaprész név.");
        }
        return value;
    }

    /**
     * A {@code xmlIndexDirectory} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>Az XML-adatot a XML-állománykezelési folyamat részeként kezeli, és megőrzi a dokumentumhoz tartozó útvonal-, állapot- és jogosultsági kontextust.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @return a művelet feldolgozási eredménye
     */
    private Path xmlIndexDirectory(Long xmlFileId) {
        return Path.of(storageProperties.getXmlIndexDir())
                .toAbsolutePath()
                .normalize()
                .resolve(String.valueOf(xmlFileId));
    }

    /**
     * A {@code cacheKey} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a XML-állománykezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param xmlFileId a célobjektum vagy erőforrás azonosítója
     * @param formName a feloldáshoz vagy azonosításhoz használt név
     * @return a művelet feldolgozási eredménye
     */
    private String cacheKey(Long xmlFileId, String formName) {
        return xmlFileId + ":" + formName;
    }

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code RowResult} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public record RowResult(long index, String label, Map<String, String> values, String xml) {}
    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code ColumnResult} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public record ColumnResult(String name, String label) {}
    /**
     * A kapcsolódó folyamat lehetséges állapotait vagy működési módjait rögzítő típus.
     *
     * <p>A {@code ConfigurationStatus} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public record ConfigurationStatus(boolean configurationRequired, String formName, String sourceVersion, String formPartName, boolean hasDisplayFields, boolean hasSearchableFields) {}

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code PageResult} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public record PageResult(List<RowResult> rows, List<ColumnResult> columns, long total, int page, int size, boolean hasMore) {}

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code IndexMetadata} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class IndexMetadata {
        public long sourceSize;
        public long sourceLastModified;
        public String sourceSha256;
        public String configurationHash;
        public String createdAt;
        public List<IndexEntry> entries = new ArrayList<>();
    }

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code IndexEntry} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    public static class IndexEntry {
        public long occurrence;
        public String label;
        public String searchText;
        public Map<String, String> values = new LinkedHashMap<>();
        public long fragmentOffset;
        public int fragmentLength;

        /**
         * Létrehozza a {@code IndexEntry} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         */
        public IndexEntry() {}
        /**
         * Létrehozza a {@code IndexEntry} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param occurrence a művelet bemeneti {@code occurrence} értéke
         * @param label a művelet bemeneti {@code label} értéke
         * @param searchText a művelet bemeneti {@code searchText} értéke
         * @param values a művelet bemeneti {@code values} értéke
         * @param fragmentOffset a lapozási vagy mennyiségi korlátot meghatározó érték
         * @param fragmentLength a művelet bemeneti {@code fragmentLength} értéke
         */
        public IndexEntry(long occurrence, String label, String searchText, Map<String, String> values, long fragmentOffset, int fragmentLength) {
            this.occurrence = occurrence;
            this.label = label;
            this.searchText = searchText;
            this.values = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
            this.fragmentOffset = fragmentOffset;
            this.fragmentLength = fragmentLength;
        }
    }

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code CachedIndex} osztály a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private static class CachedIndex {
        private final Path metaPath;
        private final Path dataPath;
        private final List<IndexEntry> entries;
        private final List<IndexFieldDto> fields;
        private long sourceSize;
        private long sourceLastModified;
        private String sourceSha256;
        private final String configurationHash;

        /**
         * Létrehozza a {@code CachedIndex} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
         *
         * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
         * @param metaPath a feldolgozásban részt vevő fájl vagy elérési út
         * @param dataPath a feldolgozásban részt vevő fájl vagy elérési út
         * @param entries a feldolgozandó elemek kollekciója
         * @param fields a feldolgozandó elemek kollekciója
         * @param sourceSize a lapozási vagy mennyiségi korlátot meghatározó érték
         * @param sourceLastModified a művelet bemeneti {@code sourceLastModified} értéke
         * @param sourceSha256 a művelet bemeneti {@code sourceSha256} értéke
         * @param configurationHash a művelethez szükséges konfigurációs adatok
         */
        private CachedIndex(Path metaPath, Path dataPath, List<IndexEntry> entries, List<IndexFieldDto> fields,
                            long sourceSize, long sourceLastModified, String sourceSha256, String configurationHash) {
            this.metaPath = metaPath;
            this.dataPath = dataPath;
            this.entries = new ArrayList<>(entries);
            this.fields = new ArrayList<>(fields);
            this.sourceSize = sourceSize;
            this.sourceLastModified = sourceLastModified;
            this.sourceSha256 = sourceSha256;
            this.configurationHash = configurationHash;
        }
    }

    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code IndexFieldSelection} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record IndexFieldSelection(List<IndexFieldDto> fields, List<String> fieldNames, String configurationHash) {}
    /**
     * A web modul XML-állománykezelési területének közös alkalmazási típusa.
     *
     * <p>A {@code ExtractedValues} rekord a web modul XML-állománykezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
     */
    private record ExtractedValues(String label, String searchText, Map<String, String> values) {}
}
