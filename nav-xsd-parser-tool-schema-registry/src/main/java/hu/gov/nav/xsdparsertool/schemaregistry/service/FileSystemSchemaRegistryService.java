package hu.gov.nav.xsdparsertool.schemaregistry.service;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.schemaregistry.model.XmlProbeResult;
import hu.gov.nav.xsdparsertool.schemaregistry.model.XsdFileDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;



/**
 * Fájlrendszer-alapú séma-regiszter, amely a konfigurált XSD könyvtárakból memóriabeli metaadat-indexet épít, majd ebből választja ki a dokumentumhoz legjobban illeszkedő sémát.
 * A kiválasztás schemaLocation, noNamespaceSchemaLocation, gyökérelem, namespace, dokumentumtípus és release-verzió alapján történik. A feloldás eredménye {@link SchemaBundle}, amely az elsődleges XSD mellett a kapcsolódó XSD-ket, valamint lehetőség szerint a UIModel- és oldalséma-fájlt is tartalmazza.
 */
public class FileSystemSchemaRegistryService implements SchemaRegistryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemSchemaRegistryService.class);

    private final ConcurrentHashMap<String, List<XsdFileDescriptor>> descriptorsCache = new ConcurrentHashMap<>();
    private final Object progressLock = new Object();

    private final Path preloadedSchemaRootDir;
    private final Path preloadedGeneralXsdDir;

    private volatile boolean loading;
    private volatile String phase = "Nincs index";
    private volatile int processedFiles;
    private volatile int totalFiles;
    private volatile String loadingKey;
    private volatile Path activeSchemaRootDir;
    private volatile Path activeGeneralXsdDir;
    /**
     * Létrehoz egy előre konfigurált könyvtárak nélküli séma-regisztert. Az index az első feloldási kéréskor, a metódusnak átadott könyvtárak alapján épül fel.
     */

    public FileSystemSchemaRegistryService() {
        this(null, null);
    }
    /**
     * Létrehoz egy séma-regisztert a megadott XSD könyvtárakkal. Ha legalább az egyik könyvtár meg van adva, a konstruktor aszinkron előtöltést indít.
     * @param schemaRootDir a nyomtatványspecifikus XSD-k gyökérkönyvtára.
     * @param generalXsdDir a közös/general XSD-k könyvtára; opcionális.
     */

    public FileSystemSchemaRegistryService(Path schemaRootDir, Path generalXsdDir) {
        this.preloadedSchemaRootDir = normalizeDir(schemaRootDir);
        this.preloadedGeneralXsdDir = normalizeDir(generalXsdDir);
        this.activeSchemaRootDir = this.preloadedSchemaRootDir;
        this.activeGeneralXsdDir = this.preloadedGeneralXsdDir;
        if (this.preloadedSchemaRootDir != null || this.preloadedGeneralXsdDir != null) {
            preloadAsync();
        }
    }
    

    /**
    

     * Aszinkron indexépítést indít a konstruktorban megadott könyvtárakra.
    

     */
    

    public void preloadAsync() {
        startAsyncLoad(preloadedSchemaRootDir, preloadedGeneralXsdDir);
    }
    

    /**
    

     * Törli a megadott könyvtárkombináció cache-ét, majd aszinkron újraindexelést indít.
    

     * @param schemaRootDir az új nyomtatvány-XSD gyökérkönyvtár; {@code null} esetén az előre konfigurált érték használatos.
    

     * @param generalXsdDir az új közös XSD könyvtár; {@code null} esetén az előre konfigurált érték használatos.
    

     */
    

    public void reloadAsync(Path schemaRootDir, Path generalXsdDir) {
        Path normalizedSchemaRoot = normalizeDir(schemaRootDir != null ? schemaRootDir : preloadedSchemaRootDir);
        Path normalizedGeneralRoot = normalizeDir(generalXsdDir != null ? generalXsdDir : preloadedGeneralXsdDir);
        String key = cacheKey(normalizedSchemaRoot, normalizedGeneralRoot);
        descriptorsCache.remove(key);
        startAsyncLoad(normalizedSchemaRoot, normalizedGeneralRoot);
    }
    

    /**
    

     * Pillanatfelvételt készít az indexelés és a memóriacache aktuális állapotáról.
    

     * @return a séma-regiszter aktuális {@link SchemaRegistryStatus} állapota.
    

     */
    

    public SchemaRegistryStatus getStatus() {
        SchemaRegistryStatus status = new SchemaRegistryStatus();
        status.setLoading(loading);
        status.setReady(!loading && !descriptorsCache.isEmpty());
        status.setPhase(phase);
        status.setProcessedFiles(processedFiles);
        status.setTotalFiles(totalFiles);
        status.setPercentage(totalFiles <= 0 ? (loading ? 0 : 100) : Math.min(100, (processedFiles * 100) / totalFiles));
        status.setCacheEntryCount(descriptorsCache.size());
        status.setActiveSchemaRootDir(activeSchemaRootDir == null ? null : activeSchemaRootDir.toString());
        status.setActiveGeneralXsdDir(activeGeneralXsdDir == null ? null : activeGeneralXsdDir.toString());
        return status;
    }
    

    /**
     * Háttérszálon elindítja a megadott séma-gyökerek registry indexelését, ha ugyanahhoz a cache-kulcshoz még nem fut betöltés.
     *
     * <p>A metódus a progress állapotot a közös zárolás alatt inicializálja, majd a betöltés eredményét cache-be teszi.
     * A párhuzamos, azonos gyökerekre irányuló indítási kéréseket összevonja.</p>
     *
     * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
     * @param generalXsdDir az általános/common XSD-k gyökérkönyvtára
     */
    private void startAsyncLoad(Path schemaRootDir, Path generalXsdDir) {
        String key = cacheKey(schemaRootDir, generalXsdDir);
        synchronized (progressLock) {
            if (loading && Objects.equals(loadingKey, key)) {
                return;
            }
            loading = true;
            loadingKey = key;
            phase = "Schema registry index építése";
            processedFiles = 0;
            totalFiles = 0;
            activeSchemaRootDir = schemaRootDir;
            activeGeneralXsdDir = generalXsdDir;
        }

        Thread thread = new Thread(() -> {
            try {
                LOGGER.info("Starting async schema registry indexing for schemaRootDir={} generalXsdDir={}", schemaRootDir, generalXsdDir);
                List<XsdFileDescriptor> descriptors = loadDescriptorsWithProgress(schemaRootDir, generalXsdDir);
                descriptorsCache.put(key, descriptors);
                synchronized (progressLock) {
                    loading = false;
                    phase = "Schema registry kész";
                    processedFiles = totalFiles;
                    progressLock.notifyAll();
                }
                LOGGER.info("Schema registry indexing finished. descriptors={}", descriptors.size());
            } catch (Exception e) {
                synchronized (progressLock) {
                    loading = false;
                    phase = "Schema registry hiba: " + e.getMessage();
                    progressLock.notifyAll();
                }
                LOGGER.error("Schema registry indexing failed", e);
            }
        }, "schema-registry-preload");
        thread.setDaemon(true);
        thread.start();
    }
    /**
     * Az XML elővizsgálati adatai alapján feloldja a legjobban illeszkedő séma-csomagot.
     * @param probeResult az XML elővizsgálati adatai.
     * @param schemaRootDir a nyomtatványspecifikus XSD-k gyökérkönyvtára.
     * @return a kiválasztott {@link SchemaBundle}.
     */

    @Override

    public SchemaBundle resolveByXmlProbe(XmlProbeResult probeResult, Path schemaRootDir) {
        return resolveByXmlProbe(probeResult, schemaRootDir, null, null);
    }
    /**
     * Az XML elővizsgálati adatai alapján feloldja a legjobban illeszkedő séma-csomagot.
     * @param probeResult az XML elővizsgálati adatai.
     * @param schemaRootDir a nyomtatványspecifikus XSD-k gyökérkönyvtára.
     * @param generalXsdDir a közös/general XSD-k könyvtára; opcionális.
     * @return a kiválasztott {@link SchemaBundle}.
     */

    @Override

    public SchemaBundle resolveByXmlProbe(XmlProbeResult probeResult, Path schemaRootDir, Path generalXsdDir) {
        return resolveByXmlProbe(probeResult, schemaRootDir, generalXsdDir, null);
    }
    /**
     * Az XML elővizsgálati adatai alapján feloldja a legjobban illeszkedő séma-csomagot.
     * @param probeResult az XML elővizsgálati adatai.
     * @param schemaRootDir a nyomtatványspecifikus XSD-k gyökérkönyvtára.
     * @param generalXsdDir a közös/general XSD-k könyvtára; opcionális.
     * @param uiModelDir a UIModel keresési gyökérkönyvtára; opcionális.
     * @return a kiválasztott {@link SchemaBundle}.
     */

    @Override
    public SchemaBundle resolveByXmlProbe(XmlProbeResult probeResult, Path schemaRootDir, Path generalXsdDir, Path uiModelDir) {
        Path normalizedSchemaRoot = normalizeDir(schemaRootDir);
        Path normalizedGeneralRoot = normalizeDir(generalXsdDir);
        Path normalizedUiModelRoot = normalizeDir(uiModelDir);
        List<XsdFileDescriptor> descriptors = scanDescriptors(normalizedSchemaRoot, normalizedGeneralRoot);

        ScoredDescriptor best = descriptors.stream()
                .map(descriptor -> scoreByProbe(descriptor, probeResult, normalizedSchemaRoot, normalizedGeneralRoot))
                .filter(scored -> scored.score() > 0)
                .max(preferredDescriptorComparator())
                .orElse(null);

        if (best != null) {
            return fromDescriptor(best.descriptor(), best.reason(), descriptors, normalizedSchemaRoot, normalizedUiModelRoot);
        }
        return resolveByDocumentType(probeResult.getRootElementName(), normalizedSchemaRoot, normalizedGeneralRoot, normalizedUiModelRoot);
    }
    /**
     * Dokumentumtípus alapján feloldja a legjobban illeszkedő séma-csomagot.
     * @param documentType a keresett dokumentumtípus.
     * @param schemaRootDir a nyomtatványspecifikus XSD-k gyökérkönyvtára.
     * @return a kiválasztott {@link SchemaBundle}.
     * @throws IllegalArgumentException ha nem található megfelelő XSD.
     */

    @Override

    public SchemaBundle resolveByDocumentType(String documentType, Path schemaRootDir) {
        return resolveByDocumentType(documentType, schemaRootDir, null, null);
    }
    /**
     * Dokumentumtípus alapján feloldja a legjobban illeszkedő séma-csomagot.
     * @param documentType a keresett dokumentumtípus.
     * @param schemaRootDir a nyomtatványspecifikus XSD-k gyökérkönyvtára.
     * @param generalXsdDir a közös/general XSD-k könyvtára; opcionális.
     * @return a kiválasztott {@link SchemaBundle}.
     * @throws IllegalArgumentException ha nem található megfelelő XSD.
     */

    @Override

    public SchemaBundle resolveByDocumentType(String documentType, Path schemaRootDir, Path generalXsdDir) {
        return resolveByDocumentType(documentType, schemaRootDir, generalXsdDir, null);
    }
    /**
     * Dokumentumtípus alapján feloldja a legjobban illeszkedő séma-csomagot.
     * @param documentType a keresett dokumentumtípus.
     * @param schemaRootDir a nyomtatványspecifikus XSD-k gyökérkönyvtára.
     * @param generalXsdDir a közös/general XSD-k könyvtára; opcionális.
     * @param uiModelDir a UIModel keresési gyökérkönyvtára; opcionális.
     * @return a kiválasztott {@link SchemaBundle}.
     * @throws IllegalArgumentException ha nem található megfelelő XSD.
     */

    @Override
    public SchemaBundle resolveByDocumentType(String documentType, Path schemaRootDir, Path generalXsdDir, Path uiModelDir) {
        Path normalizedSchemaRoot = normalizeDir(schemaRootDir);
        Path normalizedGeneralRoot = normalizeDir(generalXsdDir);
        Path normalizedUiModelRoot = normalizeDir(uiModelDir);
        List<XsdFileDescriptor> descriptors = scanDescriptors(normalizedSchemaRoot, normalizedGeneralRoot);
        String normalized = normalize(documentType);

        ScoredDescriptor best = findBestByDocumentType(descriptors, normalized);

        if (best == null) {
            LOGGER.info("No schema match in cached registry for documentType={}, refreshing descriptors once", documentType);
            descriptors = refreshDescriptors(normalizedSchemaRoot, normalizedGeneralRoot);
            best = findBestByDocumentType(descriptors, normalized);
        }

        if (best == null) {
            throw new IllegalArgumentException("No matching XSD found for document type: " + documentType);
        }
        return fromDescriptor(best.descriptor(), best.reason(), descriptors, normalizedSchemaRoot, normalizedUiModelRoot);
    }
    


    /**
     * Kiválasztja a dokumentumtípushoz legjobban illeszkedő XSD-leírót a pontozási és release-prioritási szabályok alapján.
     *
     * @param descriptors a vizsgálandó XSD-leírók
     * @param normalizedDocumentType normalizált dokumentumtípus-azonosító
     * @return a legjobb pozitív pontszámú jelölt, vagy {@code null}, ha nincs megfelelő találat
     */
    private ScoredDescriptor findBestByDocumentType(List<XsdFileDescriptor> descriptors, String normalizedDocumentType) {
        return descriptors.stream()
                .map(descriptor -> scoreByDocumentType(descriptor, normalizedDocumentType))
                .filter(scored -> scored.score() > 0)
                .max(preferredDescriptorComparator())
                .orElse(null);
    }

    /**
     * Kényszerítetten újraolvassa a megadott séma-gyökereket, majd az eredményt a hozzájuk tartozó cache-kulcson tárolja.
     *
     * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
     * @param generalXsdDir az általános/common XSD-k gyökérkönyvtára
     * @return az újonnan felépített, cache-elt descriptorlista
     */
    private List<XsdFileDescriptor> refreshDescriptors(Path schemaRootDir, Path generalXsdDir) {
        String key = cacheKey(schemaRootDir, generalXsdDir);
        List<XsdFileDescriptor> refreshed = loadDescriptors(schemaRootDir, generalXsdDir);
        descriptorsCache.put(key, refreshed);
        return refreshed;
    }

    /**
     * Visszaadja a séma-gyökerekhez tartozó descriptorlistát cache-ből, vagy szükség esetén szinkron betöltéssel állítja elő.
     *
     * <p>Ha ugyanahhoz a cache-kulcshoz háttérbetöltés fut, rövid várakozási ciklusban megvárja annak eredményét.
     * Megszakításkor visszaállítja a szál interrupt állapotát, majd a rendelkezésre álló cache-t használja vagy saját betöltést végez.</p>
     *
     * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
     * @param generalXsdDir az általános/common XSD-k gyökérkönyvtára
     * @return a feloldáshoz használható XSD-leírók
     */
    private List<XsdFileDescriptor> scanDescriptors(Path schemaRootDir, Path generalXsdDir) {
        String key = cacheKey(schemaRootDir, generalXsdDir);
        List<XsdFileDescriptor> cached = descriptorsCache.get(key);
        if (cached != null) {
            return cached;
        }

        synchronized (progressLock) {
            while (loading && Objects.equals(loadingKey, key) && !descriptorsCache.containsKey(key)) {
                try {
                    progressLock.wait(200L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        cached = descriptorsCache.get(key);
        if (cached != null) {
            return cached;
        }

        LOGGER.info("Schema registry cache miss for schemaRootDir={} generalXsdDir={}, building synchronously", schemaRootDir, generalXsdDir);
        List<XsdFileDescriptor> loaded = loadDescriptors(schemaRootDir, generalXsdDir);
        descriptorsCache.put(key, loaded);
        return loaded;
    }
    

    /**
     * Felépíti az XSD-leírókat úgy, hogy közben frissíti a registry betöltési progress állapotát.
     *
     * <p>Először összegyűjti a ténylegesen létező séma-gyökereket és az alattuk található XSD-ket, majd fájlonként
     * descriptorokat készít. A feldolgozott és összes fájlszám a státuszlekérdezés számára is elérhetővé válik.</p>
     *
     * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
     * @param generalXsdDir az általános/common XSD-k gyökérkönyvtára
     * @return az egyedi, rendezett XSD-leírók listája
     */
    private List<XsdFileDescriptor> loadDescriptorsWithProgress(Path schemaRootDir, Path generalXsdDir) {
        LinkedHashSet<Path> roots = collectRoots(schemaRootDir, generalXsdDir);
        List<Path> xsdFiles = new ArrayList<>();
        for (Path root : roots) {
            xsdFiles.addAll(listXsdFiles(root));
        }

        synchronized (progressLock) {
            totalFiles = xsdFiles.size();
            processedFiles = 0;
            phase = "XSD-k beolvasása";
        }

        List<XsdFileDescriptor> descriptors = new ArrayList<>();
        for (Path xsdFile : xsdFiles) {
            descriptors.add(parseDescriptor(xsdFile));
            synchronized (progressLock) {
                processedFiles++;
            }
        }

        LinkedHashSet<XsdFileDescriptor> uniqueDescriptors = new LinkedHashSet<>(descriptors);
        return uniqueDescriptors.stream()
                .sorted(Comparator.comparing(descriptor -> descriptor.getPath().toString()))
                .toList();
    }
    

    /**
     * Progress-követés nélkül beolvassa a konfigurált séma-gyökerek XSD-leíróit, kiszűri a duplikációkat és útvonal szerint rendezi őket.
     *
     * @param schemaRootDir a dokumentumspecifikus XSD-k gyökérkönyvtára
     * @param generalXsdDir az általános/common XSD-k gyökérkönyvtára
     * @return az egyedi, determinisztikusan rendezett descriptorlista
     */
    private List<XsdFileDescriptor> loadDescriptors(Path schemaRootDir, Path generalXsdDir) {
        LinkedHashSet<Path> roots = collectRoots(schemaRootDir, generalXsdDir);
        List<XsdFileDescriptor> descriptors = new ArrayList<>();
        for (Path root : roots) {
            descriptors.addAll(scanSingleRoot(root));
        }
        LinkedHashSet<XsdFileDescriptor> uniqueDescriptors = new LinkedHashSet<>(descriptors);
        return uniqueDescriptors.stream()
                .sorted(Comparator.comparing(descriptor -> descriptor.getPath().toString()))
                .toList();
    }
    

    /**
     * Összegyűjti a ténylegesen létező séma-gyökereket, megőrizve az elsődleges és az általános könyvtár prioritási sorrendjét.
     *
     * @param schemaRootDir az elsődleges XSD-könyvtár
     * @param generalXsdDir az általános/common XSD-könyvtár
     * @return a vizsgálandó, létező könyvtárak duplikációmentes sorrendtartó halmaza
     */
    private LinkedHashSet<Path> collectRoots(Path schemaRootDir, Path generalXsdDir) {
        LinkedHashSet<Path> roots = new LinkedHashSet<>();
        if (schemaRootDir != null && ExceptionSafeOperations.isDirectory(schemaRootDir)) {
            roots.add(schemaRootDir);
        }
        if (generalXsdDir != null && ExceptionSafeOperations.isDirectory(generalXsdDir)) {
            roots.add(generalXsdDir);
        }
        return roots;
    }
    

    /**
     * Stabil cache-kulcsot képez a dokumentumspecifikus és általános séma-gyökér útvonalpárból.
     *
     * @param schemaRootDir az elsődleges XSD-könyvtár
     * @param generalXsdDir az általános/common XSD-könyvtár
     * @return a két útvonalat elkülönítve tartalmazó cache-kulcs
     */
    private String cacheKey(Path schemaRootDir, Path generalXsdDir) {
        String schema = schemaRootDir == null ? "" : schemaRootDir.toString();
        String general = generalXsdDir == null ? "" : generalXsdDir.toString();
        return schema + "|" + general;
    }
    

    /**
     * Abszolút, normalizált könyvtárútvonalra alakítja a kapott path-ot.
     *
     * @param dir a normalizálandó útvonal
     * @return az abszolút normalizált útvonal, vagy {@code null}
     */
    private Path normalizeDir(Path dir) {
        return dir == null ? null : dir.toAbsolutePath().normalize();
    }
    

    /**
     * Rekurzívan felsorolja a megadott gyökér alatti reguláris {@code .xsd} fájlokat determinisztikus útvonalsorrendben.
     *
     * @param schemaRootDir a bejárandó séma-gyökér
     * @return az XSD fájlok rendezett listája
     * @throws IllegalStateException ha a könyvtár nem járható be
     */
    private List<Path> listXsdFiles(Path schemaRootDir) {
        try (Stream<Path> paths = Files.walk(schemaRootDir)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xsd"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan schema directory: " + schemaRootDir, e);
        }
    }
    

    /**
     * Egyetlen séma-gyökér minden XSD fájljából descriptorobjektumot készít.
     *
     * @param schemaRootDir a bejárandó séma-gyökér
     * @return a gyökérhez tartozó XSD-leírók
     */
    private List<XsdFileDescriptor> scanSingleRoot(Path schemaRootDir) {
        return listXsdFiles(schemaRootDir).stream().map(this::parseDescriptor).toList();
    }
    

    /**
    

     * Biztonságos XML parser-beállításokkal beolvassa egy XSD target namespace-ét, globális gyökérelemeit és import/include/redefine sémahelyeit.
    

     * @param xsdFile a feldolgozandó XSD fájl.
    

     * @return az XSD-ből képzett {@link XsdFileDescriptor}.
    

     * @throws IllegalStateException ha az XSD metaadatai nem olvashatók.
    

     */
    

    private XsdFileDescriptor parseDescriptor(Path xsdFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(xsdFile.toFile());

            Element schemaElement = document.getDocumentElement();
            XsdFileDescriptor descriptor = new XsdFileDescriptor();
            descriptor.setPath(xsdFile);
            descriptor.setTargetNamespace(blankToNull(schemaElement.getAttribute("targetNamespace")));

            NodeList childNodes = schemaElement.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (!(node instanceof Element element)) {
                    continue;
                }
                String localName = element.getLocalName();
                if ("element".equals(localName)) {
                    String name = blankToNull(element.getAttribute("name"));
                    if (name != null) {
                        descriptor.getRootElementNames().add(name);
                    }
                }
                if ("import".equals(localName) || "include".equals(localName) || "redefine".equals(localName)) {
                    String schemaLocation = blankToNull(element.getAttribute("schemaLocation"));
                    if (schemaLocation != null) {
                        descriptor.getRelatedSchemaLocations().add(schemaLocation);
                    }
                }
            }
            return descriptor;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse XSD file metadata: " + xsdFile, e);
        }
    }
    

    
    /**
    

    
     * A sémajelölteket pontszám, majd repository release-verzió, végül elérési út alapján rendezi; ezzel azonos főverzión belül a magasabb patch release kap elsőbbséget.
    

    
     * @return a jelöltek prioritási összehasonlítója.
    

    
     */
    

    
    private Comparator<ScoredDescriptor> preferredDescriptorComparator() {
        return Comparator.comparingInt(ScoredDescriptor::score)
                .thenComparing(scored -> releaseVersionOf(scored.descriptor().getPath()), this::compareReleaseVersions)
                .thenComparing(scored -> scored.descriptor().getPath().toString(), String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * A sémafájl közvetlen szülőkönyvtárának nevéből olvassa ki a release-verzióként használt tokent.
     *
     * @param path a sémafájl útvonala
     * @return a szülőkönyvtár neve, vagy üres szöveg, ha nem határozható meg
     */
    private String releaseVersionOf(Path path) {
        if (path == null || path.getParent() == null || path.getParent().getFileName() == null) return "";
        return path.getParent().getFileName().toString();
    }

    /**
     * Összehasonlít két release-verziót numerikus fő/minor/patch komponensek szerint.
     *
     * <p>Ha valamelyik token nem értelmezhető támogatott verzióként, kis- és nagybetűtől független lexikális
     * összehasonlításra esik vissza. Azonos numerikus komponensek után a teljes token dönti el a sorrendet.</p>
     *
     * @param left bal oldali release-verzió
     * @param right jobb oldali release-verzió
     * @return negatív, nulla vagy pozitív érték a szokásos comparator-szerződés szerint
     */
    private int compareReleaseVersions(String left, String right) {
        int[] leftParts = releaseVersionParts(left);
        int[] rightParts = releaseVersionParts(right);
        if (leftParts == null || rightParts == null) return left.compareToIgnoreCase(right);
        for (int index = 0; index < leftParts.length; index++) {
            int compared = Integer.compare(leftParts[index], rightParts[index]);
            if (compared != 0) return compared;
        }
        return left.compareToIgnoreCase(right);
    }

    /**
     * A támogatott release-verzió tokent három numerikus komponensre bontja, a hiányzó minor vagy patch részt nullával pótolva.
     *
     * @param version a feldolgozandó release-verzió
     * @return háromelemű numerikus tömb, vagy {@code null}, ha a token nem illeszkedik a támogatott formára
     */
    private int[] releaseVersionParts(String version) {
        if (version == null) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[-+_].*)?$")
                .matcher(version.trim());
        if (!matcher.matches()) return null;
        return new int[] {
                Integer.parseInt(matcher.group(1)),
                matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2)),
                matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3))
        };
    }

    /**
     * Pontozza az XSD-leírót az XML probe adatai alapján.
     *
     * <p>A pontszám figyelembe veszi többek között a schemaLocation egyezést, a namespace-t és a gyökérelem-nevet;
     * a visszaadott indoklás rögzíti, mely jelek járultak hozzá a találathoz.</p>
     *
     * @param descriptor a pontozandó XSD-leíró
     * @param probeResult az XML-ből kiolvasott felismerési adatok
     * @param schemaRootDir az elsődleges séma-gyökér
     * @param generalXsdDir az általános/common séma-gyökér
     * @return a descriptor, a számított pontszám és a találati indoklás
     */
    private ScoredDescriptor scoreByProbe(XsdFileDescriptor descriptor, XmlProbeResult probeResult, Path schemaRootDir, Path generalXsdDir) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        String schemaLocationHint = extractSchemaLocationForNamespace(
                probeResult.getSchemaLocation(),
                probeResult.getNamespace()
        );
        if (matchesSchemaLocation(descriptor.getPath(), schemaLocationHint, schemaRootDir, generalXsdDir)) {
            score += 100;
            reasons.add("schemaLocation");
        }

        if (matchesSchemaLocation(descriptor.getPath(), probeResult.getNoNamespaceSchemaLocation(), schemaRootDir, generalXsdDir)) {
            score += 100;
            reasons.add("noNamespaceSchemaLocation");
        }

        if (probeResult.getRootElementName() != null && descriptor.getRootElementNames().contains(probeResult.getRootElementName())) {
            score += 50;
            reasons.add("root-element");
        }

        if (Objects.equals(blankToNull(probeResult.getNamespace()), blankToNull(descriptor.getTargetNamespace()))) {
            if (probeResult.getNamespace() != null) {
                score += 50;
                reasons.add("namespace");
            } else {
                score += 10;
                reasons.add("no-namespace");
            }
        }

        if (probeResult.getRootElementName() != null && descriptor.getRootElementNames().contains(probeResult.getRootElementName())
                && Objects.equals(blankToNull(probeResult.getNamespace()), blankToNull(descriptor.getTargetNamespace()))) {
            score += 25;
            reasons.add("root+namespace");
        }

        return new ScoredDescriptor(descriptor, score, String.join(", ", reasons));
    }
    

    /**
     * Pontozza az XSD-leírót normalizált dokumentumtípus alapján.
     *
     * <p>A fájlnév, a globális gyökérelemek és a belőlük származtatott dokumentumkód egyezése külön súlyt kap;
     * a pozitív jelek szöveges indoklásként is bekerülnek az eredménybe.</p>
     *
     * @param descriptor a pontozandó XSD-leíró
     * @param normalizedDocumentType a keresett dokumentumtípus normalizált alakja
     * @return a descriptor pontszámmal és indoklással
     */
    private ScoredDescriptor scoreByDocumentType(XsdFileDescriptor descriptor, String normalizedDocumentType) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        String fileName = normalize(stripExtension(descriptor.getPath().getFileName().toString()));

        if (!normalizedDocumentType.isBlank() && fileName.contains(normalizedDocumentType)) {
            score += 40;
            reasons.add("filename");
        }

        for (String rootElementName : descriptor.getRootElementNames()) {
            String normalizedRoot = normalize(rootElementName);
            if (normalizedRoot.equals(normalizedDocumentType)) {
                score += 80;
                reasons.add("root-element");
            } else if (normalizedRoot.contains(normalizedDocumentType)) {
                score += 35;
                reasons.add("root-element-partial");
            }
        }

        return new ScoredDescriptor(descriptor, score, String.join(", ", reasons));
    }
    

    /**
     * Ellenőrzi, hogy egy descriptor fizikai útvonala megfelel-e az XML schemaLocation hivatkozásának.
     *
     * <p>Közvetlen végződés- és fájlnév-egyezést vizsgál, majd a konfigurált séma-gyökerekhez képest feloldható
     * relatív útvonalakat is figyelembe veszi. A vizsgálat kizárólag helyi útvonal-összevetés.</p>
     *
     * @param path a vizsgált XSD útvonala
     * @param schemaLocation az XML-ben talált sémahely-hivatkozás
     * @param schemaRootDir az elsődleges séma-gyökér
     * @param generalXsdDir az általános/common séma-gyökér
     * @return {@code true}, ha a sémahely a vizsgált XSD-re mutathat
     */
    private boolean matchesSchemaLocation(Path path, String schemaLocation, Path schemaRootDir, Path generalXsdDir) {
        if (schemaLocation == null || schemaLocation.isBlank()) {
            return false;
        }
        String normalizedHint = schemaLocation.replace('\\', '/');
        String normalizedPath = path.normalize().toString().replace('\\', '/');
        String fileNameHint = extractFileName(normalizedHint);

        if (normalizedPath.endsWith(normalizedHint) || path.getFileName().toString().equalsIgnoreCase(fileNameHint)) {
            return true;
        }

        List<Path> roots = new ArrayList<>();
        if (schemaRootDir != null) roots.add(schemaRootDir);
        if (generalXsdDir != null) roots.add(generalXsdDir);

        for (Path root : roots) {
            Path normalizedRoot = root.toAbsolutePath().normalize();
            Path normalizedFile = path.toAbsolutePath().normalize();
            if (!normalizedFile.startsWith(normalizedRoot)) {
                continue;
            }
            String relative = normalizedRoot.relativize(normalizedFile).toString().replace('\\', '/');
            if (relative.equals(normalizedHint)) {
                return true;
            }
        }
        return false;
    }
    

    /**
     * Platformfüggetlenül leválasztja egy slash vagy backslash karaktereket tartalmazó sémahely utolsó fájlnév-részét.
     *
     * @param location a sémahely szövege
     * @return a fájlnév, vagy üres szöveg hiányzó bemenetnél
     */
    private String extractFileName(String location) {
        if (location == null || location.isBlank()) {
            return "";
        }
        String normalized = location.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }
    

    /**
    

     * Az {@code xsi:schemaLocation} namespace–XSD párjai közül kiválasztja a gyökérelem namespace-éhez tartozó sémahelyet.
    

     * @param schemaLocation az xsi:schemaLocation teljes értéke.
    

     * @param namespace az XML gyökérelem namespace URI-ja.
    

     * @return a namespace-hez tartozó XSD-hely, vagy {@code null}.
    

     */
    

    private String extractSchemaLocationForNamespace(String schemaLocation, String namespace) {
        String normalizedNamespace = blankToNull(namespace);
        if (schemaLocation == null || schemaLocation.isBlank() || normalizedNamespace == null) {
            return null;
        }

        String[] tokens = schemaLocation.trim().split("\\s+");
        for (int index = 0; index + 1 < tokens.length; index += 2) {
            if (normalizedNamespace.equals(tokens[index])) {
                return blankToNull(tokens[index + 1]);
            }
        }
        return null;
    }
    

    /**
    

     * A kiválasztott XSD leíróból teljes séma-csomagot állít össze, beleértve a dokumentumtípust, verziót, kapcsolódó XSD-ket és kísérőfájlokat.
    

     * @return a feloldás teljes {@link SchemaBundle} eredménye.
    

     */
    

    private SchemaBundle fromDescriptor(XsdFileDescriptor descriptor, String reason, List<XsdFileDescriptor> allDescriptors, Path schemaRootDir, Path uiModelRootDir) {
        SchemaBundle bundle = new SchemaBundle();
        bundle.setDocumentType(deriveDocumentType(descriptor));
        bundle.setDocumentVersion(deriveDocumentVersion(descriptor, bundle.getDocumentType()));
        bundle.setRootElementName(descriptor.getRootElementNames().stream().findFirst().orElse(null));
        bundle.setTargetNamespace(descriptor.getTargetNamespace());
        bundle.setMatchReason(reason);
        bundle.setPrimaryXsd(descriptor.getPath());
        bundle.setXsdFiles(resolveRelatedXsdFiles(descriptor, allDescriptors));

        Path parent = descriptor.getPath().getParent();
        List<String> aliases = buildAliases(descriptor, bundle.getDocumentType());
        bundle.setUiModelFile(findUiModel(parent, bundle.getDocumentType(), bundle.getDocumentVersion()));
        bundle.setPageSchemaFile(findPageSchema(parent, aliases));

        if (bundle.getUiModelFile() == null && uiModelRootDir != null) {
            bundle.setUiModelFile(findUiModel(uiModelRootDir, bundle.getDocumentType(), bundle.getDocumentVersion()));
        }
        if (bundle.getUiModelFile() == null && schemaRootDir != null) {
            bundle.setUiModelFile(findUiModel(schemaRootDir, bundle.getDocumentType(), bundle.getDocumentVersion()));
        }
        if (bundle.getPageSchemaFile() == null && schemaRootDir != null) {
            bundle.setPageSchemaFile(findBestMatchingFile(schemaRootDir, aliases,
                    path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".schema")));
        }

        return bundle;
    }
    

    /**
     * Feloldja az elsődleges XSD include/import/redefine kapcsolatait tényleges fájlokra.
     *
     * <p>Elsőként az elsődleges XSD saját könyvtárához képest próbálja a hivatkozott sémahelyet. Ha ez nem létezik,
     * a már indexelt descriptorok között keres fájlnév-egyezést. Az eredmény mindig tartalmazza az elsődleges XSD-t is,
     * és duplikációmentes sorrendben kerül visszaadásra.</p>
     *
     * @param descriptor az elsődleges XSD leírója
     * @param allDescriptors az indexelt XSD-leírók teljes készlete
     * @return az elsődleges és kapcsolódó XSD-k útvonalai
     */
    private List<Path> resolveRelatedXsdFiles(XsdFileDescriptor descriptor, List<XsdFileDescriptor> allDescriptors) {
        Set<Path> result = new LinkedHashSet<>();
        result.add(descriptor.getPath());

        for (String relatedLocation : descriptor.getRelatedSchemaLocations()) {
            Path resolved = descriptor.getPath().getParent().resolve(relatedLocation).normalize();
            if (ExceptionSafeOperations.fileExists(resolved)) {
                result.add(resolved);
                continue;
            }
            String relatedFileName = extractFileName(relatedLocation);
            allDescriptors.stream()
                    .map(XsdFileDescriptor::getPath)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase(relatedFileName))
                    .findFirst()
                    .ifPresent(result::add);
        }
        return new ArrayList<>(result);
    }
    

    /**
    

     * A dokumentumtípushoz és főverzióhoz illeszkedő UIModel fájlt választja ki; több azonos főverziójú jelölt közül a legnagyobb release-patch verziót részesíti előnyben.
    

     * @return a kiválasztott UIModel fájl, vagy {@code null}.
    

     */
    

    private Path findUiModel(Path searchRoot, String documentType, String version) {
        if (searchRoot == null || !ExceptionSafeOperations.isDirectory(searchRoot) || documentType == null || documentType.isBlank()) {
            return null;
        }

        String normalizedDocumentType = normalize(documentType);
        String normalizedVersion = normalize(version);
        List<Path> accepted = new ArrayList<>();

        for (Path candidate : listUiModelFiles(searchRoot)) {
            CompanionDescriptor descriptor = parseUiModelDescriptor(candidate);
            if (descriptor == null) {
                LOGGER.debug("UI model candidate skipped because descriptor could not be parsed. candidatePath={}", candidate);
                continue;
            }

            boolean documentTypeMatch = descriptor.documentType() != null
                    && descriptor.documentType().equals(normalizedDocumentType);
            String candidateReleaseVersion = releaseVersionOf(candidate);
            boolean versionMatch = normalizedVersion == null || normalizedVersion.isBlank()
                    || descriptor.version() != null && descriptor.version().equals(normalizedVersion)
                    || sameFormVersion(candidateReleaseVersion, version);

            LOGGER.debug("UI model candidate checked. requestedDocumentType={} requestedVersion={} candidatePath={} candidateDocumentType={} candidateVersion={} candidateReleaseVersion={} accepted={} reason={}",
                    normalizedDocumentType, normalizedVersion, candidate, descriptor.documentType(), descriptor.version(),
                    candidateReleaseVersion, documentTypeMatch && versionMatch,
                    !documentTypeMatch ? "documentType-mismatch" : (!versionMatch ? "version-mismatch" : "main-version-match"));

            if (documentTypeMatch && versionMatch) accepted.add(candidate);
        }

        return accepted.stream()
                .max(Comparator.comparing(this::releaseVersionOf, this::compareReleaseVersions)
                        .thenComparing(Path::toString, String.CASE_INSENSITIVE_ORDER))
                .orElse(null);
    }

    /**
     * Megállapítja, hogy a release-verzió és az űrlap főverziója azonos major és minor komponenshez tartozik-e.
     *
     * <p>A patch és az esetleges utótag nem része ennek az egyezési feltételnek.</p>
     *
     * @param releaseVersion a repository release-verziója
     * @param formVersion az űrlap deklarált verziója
     * @return {@code true}, ha mindkettő értelmezhető és major/minor szinten azonos
     */
    private boolean sameFormVersion(String releaseVersion, String formVersion) {
        int[] release = releaseVersionParts(releaseVersion);
        int[] form = releaseVersionParts(formVersion);
        return release != null && form != null && release[0] == form[0] && release[1] == form[1];
    }

    

    /**
     * Megkeresi a megadott gyökér alatt az aliasokhoz legjobban illeszkedő {@code .schema} lapleíró fájlt.
     *
     * @param searchRoot a keresés gyökere
     * @param aliases a dokumentum lehetséges nevei és azonosítói prioritás nélkül
     * @return a legjobb találat útvonala, vagy {@code null}
     */
    private Path findPageSchema(Path searchRoot, List<String> aliases) {
        return findBestMatchingFile(searchRoot, aliases,
                path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".schema"));
    }


    /**
     * Rekurzívan felsorolja a UIModel névkonvencióknak megfelelő XML fájlokat a megadott gyökér alatt.
     *
     * @param searchRoot a keresés gyökere
     * @return a determinisztikusan rendezett UIModel-jelöltek
     * @throws IllegalStateException ha a könyvtár nem járható be
     */
    private List<Path> listUiModelFiles(Path searchRoot) {
        try (Stream<Path> paths = Files.walk(searchRoot)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return lower.endsWith(".uimodel.xml") || lower.equals("uimodel.xml") || lower.startsWith("uimodel_") && lower.endsWith(".xml");
                    })
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to search UI model files in " + searchRoot, e);
        }
    }

    /**
     * A UIModel fájl elérési útjából és fájlnevéből dokumentumtípus- és verziómetaadatot vezet le.
     *
     * <p>A könyvtárstruktúra szolgál elsődleges jelként; a fájlnév normalizált alakja kiegészítő fallbacket ad az aliasos
     * UIModel-nevekhez. Az eredmény csak a companion-fájl kiválasztásához szükséges minimális metaadatot tartalmazza.</p>
     *
     * @param file a vizsgált UIModel fájl
     * @return a levezetett companion descriptor
     */
    private CompanionDescriptor parseUiModelDescriptor(Path file) {
        Path parent = file.getParent();
        String version = parent != null ? normalize(blankToNull(parent.getFileName().toString())) : null;
        Path documentTypeDir = parent != null ? parent.getParent() : null;
        String documentType = documentTypeDir != null ? normalize(blankToNull(documentTypeDir.getFileName().toString())) : null;

        String baseName = stripExtension(stripExtension(file.getFileName().toString()));
        String normalizedBase = normalize(baseName);
        if (normalizedBase != null && normalizedBase.startsWith("uimodel")) {
            String originalBase = stripExtension(stripExtension(file.getFileName().toString()));
            String lowerOriginalBase = originalBase.toLowerCase(Locale.ROOT);
            if (lowerOriginalBase.startsWith("uimodel_")) {
                String suffix = originalBase.substring("uimodel_".length());
                if ((documentType == null || documentType.isBlank()) || (version == null || version.isBlank())) {
                    int lastUnderscore = suffix.lastIndexOf('_');
                    if (lastUnderscore > 0 && lastUnderscore < suffix.length() - 1) {
                        String parsedDocumentType = suffix.substring(0, lastUnderscore);
                        String parsedVersion = suffix.substring(lastUnderscore + 1);
                        if (documentType == null || documentType.isBlank()) {
                            documentType = normalize(parsedDocumentType);
                        }
                        if (version == null || version.isBlank()) {
                            version = normalize(parsedVersion);
                        }
                    }
                }
            }
        }

        if ((documentType == null || documentType.isBlank()) && (version == null || version.isBlank())) {
            return null;
        }

        return new CompanionDescriptor(documentType, version);
    }
    

    /**
     * Megkeresi a keresési gyökér alatti, típusfilternek megfelelő fájlok közül az aliasokhoz legjobban illeszkedőt.
     *
     * <p>A jelöltek determinisztikusan rendezve kerülnek vizsgálatra; az alias-normalizálás lehetővé teszi a technikai
     * előtagok és elválasztók eltéréseinek tolerálását. Nem létező keresési gyökérnél nincs fallback külső helyre.</p>
     *
     * @param searchRoot a keresés gyökere
     * @param aliases a dokumentum lehetséges azonosítói
     * @param extraFilter a fájltípust vagy névkonvenciót korlátozó további feltétel
     * @return a legjobb illeszkedő fájl, vagy {@code null}
     */
    private Path findBestMatchingFile(Path searchRoot, List<String> aliases, Predicate<Path> extraFilter) {
        if (searchRoot == null || !ExceptionSafeOperations.isDirectory(searchRoot)) {
            return null;
        }
        try (Stream<Path> paths = Files.walk(searchRoot)) {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(extraFilter)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();

            for (String alias : aliases) {
                String normalizedAlias = normalize(alias);
                for (Path file : files) {
                    String normalizedFile = normalize(file.getFileName().toString());
                    if (normalizedFile.equals(normalizedAlias)
                            || normalizedFile.startsWith(normalizedAlias)
                            || normalizedFile.contains(normalizedAlias)) {
                        return file;
                    }
                }
            }
            return files.isEmpty() ? null : files.get(0);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to search companion schema files in " + searchRoot, e);
        }
    }
    

    /**
     * Összeállítja a companion erőforrások kereséséhez használt dokumentumaliasokat.
     *
     * <p>A lista a dokumentumtípust, az XSD fájl alapnevét, a globális gyökérelemeket és az azokból kinyerhető
     * dokumentumkódokat egyesíti, majd az üres és duplikált értékeket kiszűri.</p>
     *
     * @param descriptor az elsődleges XSD leírója
     * @param documentType a feloldott dokumentumtípus
     * @return a kereséshez használható aliasok
     */
    private List<String> buildAliases(XsdFileDescriptor descriptor, String documentType) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        aliases.add(documentType);
        aliases.add(stripExtension(descriptor.getPath().getFileName().toString()));
        descriptor.getRootElementNames().forEach(aliases::add);
        descriptor.getRootElementNames().stream()
                .map(this::extractDocumentCode)
                .filter(Objects::nonNull)
                .forEach(aliases::add);
        aliases.removeIf(value -> value == null || value.isBlank());
        return new ArrayList<>(aliases);
    }
    

    /**
     * Dokumentumtípust vezet le az XSD-leíróból.
     *
     * <p>Elsődlegesen a globális gyökérelemekből kinyerhető dokumentumkódot használja; ha ilyen nincs,
     * az XSD fájl kiterjesztés nélküli neve lesz a fallback.</p>
     *
     * @param descriptor az XSD leírója
     * @return a levezetett dokumentumtípus
     */
    private String deriveDocumentType(XsdFileDescriptor descriptor) {
        return descriptor.getRootElementNames().stream()
                .map(this::extractDocumentCode)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> stripExtension(descriptor.getPath().getFileName().toString()));
    }
    

    /**
     * Technikai gyökérelem- vagy fájlnévből dokumentumkódot próbál kinyerni.
     *
     * <p>A {@code Doc_} előtagot közvetlenül eltávolítja; egyéb aláhúzásos névnél a technikai előtag utáni részt használja.</p>
     *
     * @param value a vizsgált technikai név
     * @return a kinyert dokumentumkód, vagy {@code null}, ha nincs használható bemenet
     */
    private String extractDocumentCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.startsWith("Doc_")) {
            return value.substring(4);
        }
        int underscore = value.indexOf('_');
        if (underscore > 0) {
            return value.substring(0, underscore);
        }
        return value;
    }
    

    
    /**
    

    
     * A dokumentum verzióját elsődlegesen a target namespace-ből, másodlagosan a verziókönyvtárból, végül a fájlnévből állapítja meg.
    

    
     * @return a dokumentum verziója, vagy {@code null}.
    

    
     */
    

    
    private String deriveDocumentVersion(XsdFileDescriptor descriptor, String documentType) {
        if (descriptor == null) {
            return null;
        }

        String namespaceVersion = extractVersionFromNamespace(descriptor.getTargetNamespace());
        if (namespaceVersion != null) {
            return namespaceVersion;
        }

        Path parent = descriptor.getPath() == null ? null : descriptor.getPath().getParent();
        if (parent != null && parent.getFileName() != null) {
            String directoryVersion = normalizeVersionToken(parent.getFileName().toString());
            if (directoryVersion != null) {
                return directoryVersion;
            }
        }

        return extractVersion(descriptor.getPath().getFileName().toString(), documentType);
    }

    /**
     * Verziótokent próbál kinyerni az XSD namespace URI utolsó releváns szegmenséből.
     *
     * <p>A query és fragment részeket levágja, majd a szegmenseket hátulról vizsgálja, és csak numerikus,
     * ponttal vagy aláhúzással tagolt verzióformát fogad el.</p>
     *
     * @param namespace a target namespace
     * @return a normalizált verzió, vagy {@code null}
     */
    private String extractVersionFromNamespace(String namespace) {
        String normalized = blankToNull(namespace);
        if (normalized == null) {
            return null;
        }
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        int slash = normalized.lastIndexOf('/');
        return normalizeVersionToken(slash >= 0 ? normalized.substring(slash + 1) : normalized);
    }

    /**
     * Ellenőrzi és ponttal tagolt formára normalizálja a tisztán numerikus verziótokent.
     *
     * @param value a vizsgált token
     * @return a normalizált verzió, vagy {@code null}, ha a token nem támogatott formátumú
     */
    private String normalizeVersionToken(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("^(\\d+(?:[._]\\d+)*)$").matcher(normalized);
        return matcher.matches() ? matcher.group(1).replace('_', '.') : null;
    }

    /**
     * A fájl kiterjesztés nélküli nevének végéről numerikus verziót próbál kinyerni.
     *
     * @param fileName a vizsgált fájlnév
     * @return ponttal normalizált verzió, vagy {@code null}, ha nincs verzióutótag
     */
    private String extractVersion(String fileName) {
        String base = stripExtension(fileName);
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("(\\d+(?:[._]\\d+)*)$").matcher(base);
        if (matcher.find()) {
            return matcher.group(1).replace('_', '.');
        }
        return null;
    }
    

    /**
     * Dokumentumtípus-kontekstuálisan próbál verziót kinyerni a fájlnévből.
     *
     * <p>Ha nincs dokumentumtípus, az általános fájlnév-végi kinyerést használja. Azonos dokumentumtípusnév esetén
     * nincs külön verzió; egyébként a dokumentumtípus után álló elválasztott verziórészt részesíti előnyben.</p>
     *
     * @param fileName a vizsgált fájlnév
     * @param documentType a kapcsolódó dokumentumtípus
     * @return a kinyert verzió, vagy {@code null}
     */
    private String extractVersion(String fileName, String documentType) {
        String base = stripExtension(fileName);

        if (documentType == null || documentType.isBlank()) {
            return extractVersion(fileName);
        }

        String normalizedDocumentType = documentType.trim();
        if (base.equalsIgnoreCase(normalizedDocumentType)) {
            return null;
        }

        String marker = normalizedDocumentType + "_";
        int idx = base.indexOf(marker);

        if (idx < 0) {
            return extractVersion(fileName);
        }

        String suffix = base.substring(idx + marker.length());

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("^(\\d+(?:[._]\\d+)*)").matcher(suffix);

        if (matcher.find()) {
            return matcher.group(1).replace('_', '.');
        }

        return extractVersion(fileName);
    }
    

    /**
     * Eltávolítja a fájlnév utolsó kiterjesztését.
     *
     * @param fileName a fájlnév
     * @return a kiterjesztés nélküli név
     */
    private String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }
    

    /**
     * Az üres vagy csak whitespace karaktereket tartalmazó szöveget {@code null} értékre normalizálja.
     *
     * @param value a vizsgált szöveg
     * @return az eredeti nem üres érték, vagy {@code null}
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
    

    /**
     * Keresési összevetéshez kisbetűs, elválasztóktól megtisztított technikai azonosítót képez.
     *
     * @param value a normalizálandó érték
     * @return a kisbetűs, kötőjel/aláhúzás/pont nélküli alak; {@code null} bemenetnél üres szöveg
     */
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(".", "");
    }

    /**
     * Egy companion UIModel fájl kiválasztásához szükséges levezetett dokumentumtípust és verziót hordozza.
     *
     * @param documentType a companion fájlhoz társított dokumentumtípus
     * @param version a companion fájlhoz társított verzió
     */
    private record CompanionDescriptor(String documentType, String version) {
    }
    

    /**
     * Egy XSD-jelölt pontozási eredménye a kiválasztási algoritmusban.
     *
     * @param descriptor a pontozott XSD-leíró
     * @param score az illeszkedési pontszám
     * @param reason a pontszámot indokló rövid jelölés
     */
    private record ScoredDescriptor(XsdFileDescriptor descriptor, int score, String reason) {
    }
}