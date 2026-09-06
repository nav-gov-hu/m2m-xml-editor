package hu.gov.nav.xsdparsertool.processing.form;

import hu.gov.nav.xsdparsertool.core.support.ExceptionSafeOperations;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.definition.BlockDefinition;
import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;
import hu.gov.nav.xsdparsertool.core.model.definition.FieldDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormFieldDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormRowDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormSectionDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormTabDefinition;
import hu.gov.nav.xsdparsertool.uimodel.model.UiModelMetadata;
import hu.gov.nav.xsdparsertool.uimodel.service.XmlUiModelParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A {@link FormDefinitionBuilderService} UIModel-first alapértelmezett implementációja.
 *
 * <p>Elsődlegesen a UIModel vizuális szerkezetét használja; annak hiányában az XSD-ből
 * felépített {@code DocumentDefinition} alapján strukturális fallback űrlapot készít.</p>
 *
 * <p>A mezőösszerendelés teljes blokk- és mezőkörnyezetet vesz figyelembe, ami multiform
 * dokumentumoknál és ismétlődő technikai mezőazonosítóknál különösen fontos.</p>
 */
public class DefaultFormDefinitionBuilderService implements FormDefinitionBuilderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFormDefinitionBuilderService.class);

    private final XmlUiModelParserService uiModelParserService = new XmlUiModelParserService();

    /**
     * Felépíti a megjelenítéshez használható űrlapdefiníciót.
     *
     * <p>Ha a séma-csomaghoz elérhető UIModel állomány, elsődlegesen annak vizuális
     * szerkezetét használja. Ha az UIModel hiányzik vagy üres eredményt ad, az XSD-ből
     * felépített {@link DocumentDefinition} alapján strukturális fallback űrlapot készít.</p>
     *
     * @param documentDefinition az XSD-ből származó dokumentum- és meződefiníció
     * @param schemaBundle a feloldott séma-csomag, benne az opcionális UIModellel
     * @return a renderelhető űrlapdefiníció
     * @throws IllegalArgumentException ha a dokumentumdefiníció {@code null}
     */
    @Override
    public FormDefinition build(DocumentDefinition documentDefinition, SchemaBundle schemaBundle) {
        if (documentDefinition == null) {
            throw new IllegalArgumentException("DocumentDefinition must not be null");
        }

        boolean hasUiModel = schemaBundle != null
                && schemaBundle.getUiModelFile() != null
                && ExceptionSafeOperations.isRegularFile(schemaBundle.getUiModelFile());

        if (hasUiModel) {
            LOGGER.debug("Form builder prefers UI model layout. documentId={}", documentDefinition.getId());
            FormDefinition formDefinition = buildFromUiModel(documentDefinition, schemaBundle);
            if (formDefinition != null && formDefinition.getTabs() != null && !formDefinition.getTabs().isEmpty()) {
                return formDefinition;
            }
            LOGGER.warn("UI model based form build produced empty result, falling back. documentId={}", documentDefinition.getId());
        }


        LOGGER.debug("Form builder uses structural fallback. documentId={}", documentDefinition.getId());
        return buildFallback(documentDefinition);
    }

    /**
     * A UIModel vizuális hierarchiája alapján építi fel az űrlap tab/szekció/sor/mező szerkezetét.
     *
     * <p>A UIModel block group azonosítóit az XSD-ből származó blokkokhoz és mezőkhöz
     * köti, megőrzi a mezők eredeti sorrendjét, valamint külön szekcióba gyűjti azokat
     * az XSD-mezőket, amelyeket a UIModel egyik csoportja sem használt fel.</p>
     *
     * @param documentDefinition az XSD-ből felépített dokumentumdefiníció
     * @param schemaBundle a UIModel állományt is tartalmazó séma-csomag
     * @return a UIModel szerinti űrlapdefiníció, vagy {@code null}, ha nem épült használható szekció
     */
    private FormDefinition buildFromUiModel(DocumentDefinition documentDefinition, SchemaBundle schemaBundle) {
        UiModelMetadata meta = uiModelParserService.parse(schemaBundle.getUiModelFile());
        Map<String, FieldDefinition> fieldsById = indexFields(documentDefinition);
        logFieldBindingDebug(documentDefinition, "buildFromUiModel");
        Map<String, Integer> fieldOrderById = indexFieldOrder(documentDefinition);
        Map<String, BlockDefinition> blocksByGroupId = indexBlocksByGroupId(documentDefinition);

        FormDefinition formDefinition = new FormDefinition();
        formDefinition.setId(firstNonBlank(documentDefinition.getId(), documentDefinition.getName(), meta.getDocumentId(), "form"));
        formDefinition.setTitle(firstNonBlank(documentDefinition.getTitle(), documentDefinition.getName(), meta.getTitle(), "Űrlap"));
        formDefinition.setStructuralLabelsByPath(documentDefinition.getStructuralLabelsByPath());

        FormTabDefinition mainTab = new FormTabDefinition();
        mainTab.setId("main");
        mainTab.setTitle("Űrlap");

        Set<String> usedFieldIds = new LinkedHashSet<String>();

        List<UiModelMetadata.Section> sections = new ArrayList<UiModelMetadata.Section>(meta.getSections());
        sections.sort(Comparator.comparingInt(UiModelMetadata.Section::getOrder));

        LOGGER.debug("Building form from UI model. documentId={} sections={} uiGroups={} indexedBlocks={} indexedFields={}",
                documentDefinition.getId(), sections.size(), meta.getBlockGroupsById().size(), blocksByGroupId.size(), fieldsById.size());

        for (UiModelMetadata.Section uiSection : sections) {
            FormSectionDefinition section = new FormSectionDefinition();
            section.setId(firstNonBlank(uiSection.getId(), "section-" + mainTab.getSections().size()));
            section.setTitle(firstNonBlank(uiSection.getTitle(), section.getId(), "Blokk"));

            LOGGER.debug("UI section build started. sectionId={} sectionTitle={} blockGroupIds={}",
                    section.getId(), section.getTitle(), uiSection.getBlockGroupIds());

            for (String groupId : uiSection.getBlockGroupIds()) {
                UiModelMetadata.BlockGroup uiGroup = resolveUiBlockGroup(meta, groupId);
                BlockDefinition block = resolveBlock(blocksByGroupId, groupId);

                if (uiGroup == null && block == null) {
                    LOGGER.warn("UI section group could not be resolved. sectionId={} requestedGroupId={}",
                            section.getId(), groupId);
                    continue;
                }

                FormRowDefinition row = new FormRowDefinition();
                row.setId(firstNonBlank(groupId, uiGroup != null ? uiGroup.getId() : null, "fieldgroup-" + section.getRows().size()));
                row.setTitle(firstNonBlank(
                        uiGroup != null ? uiGroup.getTitle() : null,
                        block != null ? block.getTitle() : null,
                        block != null ? block.getName() : null,
                        row.getId(),
                        "FieldGroup"
                ));
                row.setType("fieldgroup");

                List<FieldDefinition> fieldDefs = resolveUiGroupFields(uiGroup, block, fieldsById);
                fieldDefs.sort(
                        Comparator.<FieldDefinition, Integer>comparing(field -> fieldOrder(field, fieldOrderById))
                                .thenComparing((FieldDefinition field) -> field.getXmlName() == null ? "" : field.getXmlName())
                );

                row.setXmlPath(resolveRowXmlPath(fieldDefs));
                row.setRepeatable(isChainRow(fieldDefs));

                LOGGER.debug("UI row resolved. sectionId={} rowId={} rowTitle={} groupId={} matchedBlockId={} fieldCount={}",
                        section.getId(), row.getId(), row.getTitle(), groupId, block != null ? block.getId() : null, fieldDefs.size());

                for (FieldDefinition field : fieldDefs) {
                    UiModelMetadata.FieldUi fieldUi = meta.getFieldsById().get(field.getId());
                    row.getFields().add(mapField(field, fieldUi));
                    if (field.getId() != null) {
                        usedFieldIds.add(field.getId());
                    }
                }

                if (!row.getFields().isEmpty()) {
                    section.getRows().add(row);
                } else {
                    LOGGER.warn("UI row resolved without fields. sectionId={} rowId={} groupId={} matchedBlockId={}",
                            section.getId(), row.getId(), groupId, block != null ? block.getId() : null);
                }
            }

            if (!section.getRows().isEmpty()) {
                mainTab.getSections().add(section);
            } else {
                LOGGER.warn("UI section resolved without rows. sectionId={} sectionTitle={}", section.getId(), section.getTitle());
            }
        }

        FormSectionDefinition ungroupedSection = new FormSectionDefinition();
        ungroupedSection.setId("ungrouped");
        ungroupedSection.setTitle("További mezők");

        FormRowDefinition ungroupedRow = new FormRowDefinition();
        ungroupedRow.setId("ungrouped-fields");
        ungroupedRow.setTitle("Egyéb mezők");
        ungroupedRow.setType("fieldgroup");
        ungroupedRow.setRepeatable(false);

        if (documentDefinition.getBlocks() != null) {
            for (BlockDefinition block : documentDefinition.getBlocks()) {
                if (block.getFields() == null) {
                    continue;
                }
                for (FieldDefinition field : block.getFields()) {
                    if (field.getId() != null && !usedFieldIds.contains(field.getId())) {
                        UiModelMetadata.FieldUi fieldUi = meta.getFieldsById().get(field.getId());
                        ungroupedRow.getFields().add(mapField(field, fieldUi));
                    }
                }
            }
        }

        if (!ungroupedRow.getFields().isEmpty()) {
            LOGGER.debug("UI model build found ungrouped fields. count={}", ungroupedRow.getFields().size());
            ungroupedSection.getRows().add(ungroupedRow);
            mainTab.getSections().add(ungroupedSection);
        }

        if (mainTab.getSections().isEmpty()) {
            LOGGER.warn("UI model build produced no sections. documentId={}", documentDefinition.getId());
            return null;
        }

        formDefinition.getTabs().add(mainTab);
        return formDefinition;
    }

    /**
     * Több lehetséges technikai azonosító alapján indexeli a dokumentum blokkjait.
     *
     * <p>Az index a blokk azonosítójának és nevének normalizált változatait is kezeli,
     * így a UIModel {@code FieldGroup_*} hivatkozásai nagyobb eséllyel köthetők az XSD blokkhoz.</p>
     *
     * @param documentDefinition az indexelendő dokumentumdefiníció
     * @return a normalizált csoportazonosítóról blokkra mutató index
     */
    private Map<String, BlockDefinition> indexBlocksByGroupId(DocumentDefinition documentDefinition) {
        Map<String, BlockDefinition> result = new LinkedHashMap<String, BlockDefinition>();
        if (documentDefinition.getBlocks() == null) {
            return result;
        }

        for (BlockDefinition block : documentDefinition.getBlocks()) {
            if (block == null) {
                continue;
            }
            if (block.getId() != null) {
                result.put(block.getId(), block);
            }
            String normalized = normalizeGroupId(block.getId());
            if (normalized != null) {
                result.put(normalized, block);
                result.put("FieldGroup_" + normalized, block);
            }
        }
        return result;
    }

    /**
     * Feloldja a UIModel block group metaadatát a megadott csoportazonosító alapján.
     *
     * <p>Közvetlen egyezés mellett a {@code FieldGroup_} előtaggal és anélkül képzett
     * normalizált azonosítókat is figyelembe veszi.</p>
     *
     * @param meta a feldolgozott UIModel metaadatai
     * @param groupId a feloldandó csoportazonosító
     * @return a megfelelő UIModel csoport, vagy {@code null}, ha nincs találat
     */
    private UiModelMetadata.BlockGroup resolveUiBlockGroup(UiModelMetadata meta, String groupId) {
        if (meta == null || groupId == null) {
            return null;
        }
        UiModelMetadata.BlockGroup direct = meta.getBlockGroupsById().get(groupId);
        if (direct != null) {
            return direct;
        }
        String normalized = normalizeGroupId(groupId);
        if (normalized == null) {
            return null;
        }
        UiModelMetadata.BlockGroup normalizedGroup = meta.getBlockGroupsById().get(normalized);
        if (normalizedGroup != null) {
            return normalizedGroup;
        }
        return meta.getBlockGroupsById().get("FieldGroup_" + normalized);
    }

    /**
     * Kikeresi a UIModel csoportazonosítóhoz tartozó XSD blokkot az előkészített indexből.
     *
     * @param blocksByGroupId a normalizált blokkindex
     * @param groupId a UIModel csoportazonosítója
     * @return a megfelelő blokk, vagy {@code null}, ha nem oldható fel
     */
    private BlockDefinition resolveBlock(Map<String, BlockDefinition> blocksByGroupId, String groupId) {
        if (blocksByGroupId == null || groupId == null) {
            return null;
        }
        BlockDefinition direct = blocksByGroupId.get(groupId);
        if (direct != null) {
            return direct;
        }
        String normalized = normalizeGroupId(groupId);
        if (normalized == null) {
            return null;
        }
        BlockDefinition normalizedBlock = blocksByGroupId.get(normalized);
        if (normalizedBlock != null) {
            return normalizedBlock;
        }
        return blocksByGroupId.get("FieldGroup_" + normalized);
    }

    /**
     * Meghatározza, mely XSD-mezők tartoznak egy UIModel block grouphoz.
     *
     * <p>Ha ismert a konkrét XSD blokk, a mezőket elsődlegesen abban keresi, ezzel
     * elkerülve a multiform dokumentumok azonos technikai mezőazonosítóinak globális
     * összekeverését. A globális mezőindex csak fallbackként használatos.</p>
     *
     * @param uiGroup a feldolgozandó UIModel csoport
     * @param block a csoporthoz feloldott XSD blokk, ha van
     * @param fieldsById globális, első előfordulást megőrző fallback mezőindex
     * @return a csoporthoz feloldott meződefiníciók
     */
    private List<FieldDefinition> resolveUiGroupFields(UiModelMetadata.BlockGroup uiGroup,
                                                       BlockDefinition block,
                                                       Map<String, FieldDefinition> fieldsById) {
        List<FieldDefinition> result = new ArrayList<FieldDefinition>();
        if (uiGroup != null && uiGroup.getFieldIds() != null) {
            for (String fieldId : uiGroup.getFieldIds()) {
                // Multiform XSD-ben ugyanaz a rövid fieldId több form-részben eltérő
                // típussal fordulhat elő. Elsődlegesen mindig az aktuális FieldGroup
                // (és ezzel az aktuális Form_* útvonal) mezőjét kell feloldani.
                FieldDefinition field = findFieldInBlock(block, fieldId);
                if (field == null) {
                    field = fieldsById.get(fieldId);
                }
                if (field != null) {
                    result.add(field);
                } else {
                    LOGGER.warn("UI model field id not found in XSD field index. fieldId={} uiGroupId={}",
                            fieldId, uiGroup.getId());
                }
            }
        }
        if (result.isEmpty() && block != null && block.getFields() != null) {
            result.addAll(block.getFields());
        }
        return result;
    }







    /**
     * Egy konkrét blokkon belül keresi meg a megadott technikai azonosítójú mezőt.
     *
     * <p>Az összehasonlítás kezeli a {@code Field_} előtaggal és anélkül tárolt
     * azonosítókat, valamint az XML-nevet is. A blokk-korlát azért fontos, mert
     * multiform dokumentumban ugyanaz a mezőazonosító több form part alatt is előfordulhat.</p>
     *
     * @param block a keresésre kijelölt XSD blokk
     * @param fieldId a UIModelből érkező mezőazonosító
     * @return a blokkon belül megtalált mező, vagy {@code null}
     */
    private FieldDefinition findFieldInBlock(BlockDefinition block, String fieldId) {
        if (block == null || block.getFields() == null || fieldId == null) {
            return null;
        }
        String normalized = fieldId.startsWith("Field_") ? fieldId.substring("Field_".length()) : fieldId;
        for (FieldDefinition field : block.getFields()) {
            if (field == null) {
                continue;
            }
            String candidateId = field.getId();
            String candidateName = field.getXmlName();
            if (fieldId.equals(candidateId) || fieldId.equals(candidateName)) {
                return field;
            }
            if (normalized.equals(candidateId)
                    || normalized.equals(candidateName)
                    || (candidateId != null && normalized.equals(candidateId.replaceFirst("^Field_", "")))
                    || (candidateName != null && normalized.equals(candidateName.replaceFirst("^Field_", "")))) {
                return field;
            }
        }
        return null;
    }

    /**
     * Visszaadja a mező XSD-feldolgozásból származó eredeti sorrendi pozícióját.
     *
     * @param field a vizsgált mező
     * @param fieldOrderById a mezőazonosítóhoz rendelt sorrendi indexek
     * @return a mező sorrendi indexe, vagy {@link Integer#MAX_VALUE}, ha nem indexelhető
     */
    private int fieldOrder(FieldDefinition field, Map<String, Integer> fieldOrderById) {
        Integer order = fieldOrderById.get(field.getId());
        return order == null ? Integer.MAX_VALUE : order;
    }


    /**
     * Diagnosztikai naplót készít egy ismert, többször előforduló mező kötési adatairól.
     *
     * <p>A napló célja a multiform mezők azonosító-, XML-útvonal-, típus-, enum- és
     * címkeeltéréseinek vizsgálata. A metódus nem módosítja a dokumentumdefiníciót.</p>
     *
     * @param documentDefinition a vizsgált dokumentumdefiníció
     * @param context a naplóban megjelenő feldolgozási kontextus
     */
    private void logFieldBindingDebug(DocumentDefinition documentDefinition, String context) {
        if (documentDefinition == null || documentDefinition.getBlocks() == null) {
            return;
        }
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (BlockDefinition block : documentDefinition.getBlocks()) {
            if (block.getFields() == null) {
                continue;
            }
            for (FieldDefinition field : block.getFields()) {
                if (field.getId() == null) {
                    continue;
                }
                counts.put(field.getId(), counts.getOrDefault(field.getId(), 0) + 1);
            }
        }
        String debugFieldId = "0A0001D003A";
        Integer targetCount = counts.get(debugFieldId);
        Integer prefixedTargetCount = counts.get("Field_" + debugFieldId);
        int totalTarget = (targetCount == null ? 0 : targetCount) + (prefixedTargetCount == null ? 0 : prefixedTargetCount);
        if (totalTarget <= 0) {
            return;
        }
        LOGGER.info("FIELD_BIND_DEBUG context={} documentId={} targetField={} occurrenceCount={} duplicateFieldIds={}",
                context,
                documentDefinition.getId(),
                debugFieldId,
                totalTarget,
                counts.entrySet().stream().filter(e -> e.getValue() != null && e.getValue() > 1).map(e -> e.getKey() + "=" + e.getValue()).toList());
        for (BlockDefinition block : documentDefinition.getBlocks()) {
            if (block.getFields() == null) {
                continue;
            }
            for (FieldDefinition field : block.getFields()) {
                String id = field.getId();
                if (!debugFieldId.equals(id) && !("Field_" + debugFieldId).equals(id)) {
                    continue;
                }
                LOGGER.info("FIELD_BIND_DEBUG occurrence context={} blockId={} blockName={} fieldId={} xmlName={} xmlPath={} dataType={} enumCount={} enumValues={} maxLength={} xsdLabel={} uiLabel={}",
                        context,
                        block.getId(),
                        block.getName(),
                        field.getId(),
                        field.getXmlName(),
                        field.getXmlPath(),
                        field.getDataType(),
                        field.getEnumValues() == null ? 0 : field.getEnumValues().size(),
                        field.getEnumValues() == null ? List.of() : field.getEnumValues().stream().limit(10).toList(),
                        field.getMaxLength(),
                        field.getXsdLabel(),
                        field.getUiLabel());
            }
        }
    }

    /**
     * Fallback mezőindexet készít technikai mezőazonosító alapján.
     *
     * <p>Az index szándékosan az első előfordulást őrzi meg {@code putIfAbsent}
     * használatával. Multiform XSD-ben ugyanaz a mezőazonosító eltérő form partokban
     * különböző definícióval szerepelhet, ezért ez az index nem használható pontos
     * path/form-part kontextus helyett.</p>
     *
     * @param documentDefinition az indexelendő dokumentumdefiníció
     * @return mezőazonosítóról az első előforduló meződefinícióra mutató fallback index
     */
    private Map<String, FieldDefinition> indexFields(DocumentDefinition documentDefinition) {
        Map<String, FieldDefinition> result = new LinkedHashMap<String, FieldDefinition>();
        if (documentDefinition.getBlocks() == null) {
            return result;
        }

        for (BlockDefinition block : documentDefinition.getBlocks()) {
            if (block.getFields() == null) {
                continue;
            }
            for (FieldDefinition field : block.getFields()) {
                if (field.getId() != null) {
                    // Multiform XSD-kben ugyanaz a Field_* azonosító több form part alatt is előfordulhat.
                    // Példa: 26HIPAK esetén a Field_0A0001C001A a főlapban egyszerű input,
                    // a melléklapon viszont enum típus is lehet. A korábbi put() felülírta
                    // a főlap meződefinícióját a későbbi melléklapi definícióval, ezért a főlap
                    // tévesen legördülőként jelent meg. A globális id index csak fallback célú:
                    // őrizze meg az első előfordulást, a pontos multiform kötést pedig path/formPart
                    // alapján kell kezelni.
                    result.putIfAbsent(field.getId(), field);
                }
            }
        }
        return result;
    }

    /**
     * Rögzíti a mezők bejárási sorrendjét az űrlap UIModel szerinti rendezéséhez.
     *
     * @param documentDefinition a sorrend forrásául szolgáló dokumentumdefiníció
     * @return mezőazonosítóról növekvő sorrendi indexre mutató térkép
     */
    private Map<String, Integer> indexFieldOrder(DocumentDefinition documentDefinition) {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        if (documentDefinition.getBlocks() == null) {
            return result;
        }

        int i = 0;
        for (BlockDefinition block : documentDefinition.getBlocks()) {
            if (block.getFields() == null) {
                continue;
            }
            for (FieldDefinition field : block.getFields()) {
                if (field.getId() != null) {
                    result.put(field.getId(), i++);
                }
            }
        }

        return result;
    }

    /**
     * Egységesíti a block group azonosítókat a {@code FieldGroup_} előtag eltávolításával.
     *
     * @param value a normalizálandó csoportazonosító
     * @return az előtag nélküli, trimmelt azonosító, vagy {@code null}, ha nincs érték
     */
    private String normalizeGroupId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.startsWith("FieldGroup_")) {
            return trimmed.substring("FieldGroup_".length());
        }

        return trimmed;
    }

    /**
     * Megállapítja, hogy a mezőcsoport ismétlődő chain elemhez tartozik-e.
     *
     * <p>A felismerés a mezők XSD/XML útvonalában szereplő {@code /Chain_elem/}
     * szegmens alapján történik.</p>
     *
     * @param fields a sorhoz tartozó meződefiníciók
     * @return {@code true}, ha legalább egy mező chain elem alatt található
     */
    private boolean isChainRow(List<FieldDefinition> fields) {
        if (fields == null) {
            return false;
        }
        for (FieldDefinition field : fields) {
            String xmlPath = field == null ? null : field.getXmlPath();
            if (xmlPath != null && xmlPath.contains("/Chain_elem/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Meghatározza egy űrlapsor közös XML-szülőútvonalát a mezők teljes útvonalaiból.
     *
     * <p>Azonos szülőútvonal esetén azt adja vissza; eltérő szülők esetén a már
     * feldolgozott és az aktuális útvonal leghosszabb közös előtagjára szűkít.</p>
     *
     * @param fields a sorhoz tartozó mezők
     * @return a közös sor-/csoportútvonal, vagy {@code null}, ha nem határozható meg
     */
    private String resolveRowXmlPath(List<FieldDefinition> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }

        String common = null;
        for (FieldDefinition field : fields) {
            if (field == null || field.getXmlPath() == null || field.getXmlPath().isBlank()) {
                continue;
            }
            String groupPath = parentPath(field.getXmlPath());
            if (groupPath == null) {
                continue;
            }
            if (common == null) {
                common = groupPath;
            } else if (!common.equals(groupPath)) {
                return commonPrefixPath(common, groupPath);
            }
        }
        return common;
    }

    /**
     * Eltávolítja az XML-útvonal utolsó szegmensét.
     *
     * @param path a teljes mezőútvonal
     * @return a szülőútvonal, vagy {@code null}, ha a bemenet üres
     */
    private String parentPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        int idx = path.lastIndexOf('/');
        return idx <= 0 ? path : path.substring(0, idx);
    }

    /**
     * Két XML-útvonal leghosszabb, teljes szegmensekből álló közös előtagját számítja ki.
     *
     * @param left az első XML-útvonal
     * @param right a második XML-útvonal
     * @return a közös útvonal-előtag, vagy {@code null}, ha nincs közös nem üres szegmens
     */
    private String commonPrefixPath(String left, String right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        String[] a = left.split("/");
        String[] b = right.split("/");
        StringBuilder result = new StringBuilder();
        int limit = Math.min(a.length, b.length);
        for (int i = 0; i < limit; i++) {
            if (!a[i].equals(b[i])) {
                break;
            }
            if (!a[i].isEmpty()) {
                result.append('/').append(a[i]);
            }
        }
        return result.length() == 0 ? null : result.toString();
    }

    /**
     * UIModel hiányában az XSD dokumentumdefiníció szerkezete alapján épít alap űrlapot.
     *
     * <p>Minden XSD blokk külön szekcióvá, a blokk mezői pedig egy fieldgroup sorrá
     * alakulnak. Az XML-útvonal, ismétlődés és mezőtípus továbbra is az XSD-metaadatokból
     * származik.</p>
     *
     * @param documentDefinition az XSD-ből felépített dokumentumdefiníció
     * @return a strukturális fallback űrlapdefiníció
     */
    private FormDefinition buildFallback(DocumentDefinition documentDefinition) {
        FormDefinition formDefinition = new FormDefinition();
        formDefinition.setId(documentDefinition.getId());
        formDefinition.setTitle(firstNonBlank(documentDefinition.getTitle(), documentDefinition.getName(), "Űrlap"));
        formDefinition.setStructuralLabelsByPath(documentDefinition.getStructuralLabelsByPath());

        FormTabDefinition mainTab = new FormTabDefinition();
        mainTab.setId("main");
        mainTab.setTitle("Űrlap");

        if (documentDefinition.getBlocks() != null) {
            for (BlockDefinition block : documentDefinition.getBlocks()) {
                FormSectionDefinition section = new FormSectionDefinition();
                section.setId(firstNonBlank(block.getId(), block.getName(), "section"));
                section.setTitle(firstNonBlank(block.getTitle(), block.getName(), block.getId(), "Blokk"));

                FormRowDefinition row = new FormRowDefinition();
                row.setId(section.getId() + "-group");
                row.setTitle(firstNonBlank(block.getTitle(), block.getName(), "Mezők"));
                row.setType("fieldgroup");
                row.setXmlPath(resolveRowXmlPath(block.getFields()));
                row.setRepeatable(isChainRow(block.getFields()));

                if (block.getFields() != null) {
                    for (FieldDefinition field : block.getFields()) {
                        row.getFields().add(mapField(field, null));
                    }
                }

                if (!row.getFields().isEmpty()) {
                    section.getRows().add(row);
                }
                if (!section.getRows().isEmpty()) {
                    mainTab.getSections().add(section);
                }
            }
        }

        formDefinition.getTabs().add(mainTab);
        return formDefinition;
    }

    /**
     * Egy XSD meződefinícióból és opcionális UIModel metaadatból renderelhető űrlapmezőt készít.
     *
     * <p>A címkénél UIModel-first sorrendet használ, majd XSD címkére és technikai
     * névre esik vissza. Az enumértékeket, kötelezőséget, ismétlődést, maszkot,
     * maximális hosszt, layout-szélességet és szerkesztési típust is átvezeti.</p>
     *
     * @param field az XSD-ből származó meződefiníció
     * @param fieldUi az opcionális UIModel mezőmetaadat
     * @return a megjelenítéshez használható űrlapmező-definíció
     */
    private FormFieldDefinition mapField(FieldDefinition field, UiModelMetadata.FieldUi fieldUi) {
        FormFieldDefinition formField = new FormFieldDefinition();
        formField.setId(firstNonBlank(field.getId(), field.getXmlName(), field.getXmlPath()));
        formField.setXmlName(field.getXmlName());
        formField.setXmlPath(field.getXmlPath());

        String uiLabel = firstNonBlank(
                fieldUi != null ? fieldUi.getLabel() : null,
                field.getUiLabel()
        );
        String xsdLabel = firstNonBlank(field.getXsdLabel());

        formField.setUiLabel(uiLabel);
        formField.setXsdLabel(xsdLabel);
        formField.setLabel(firstNonBlank(uiLabel, xsdLabel, field.getXmlName(), field.getId(), "Mező"));

        formField.setRequired(field.isRequired());
        formField.setRepeatable(isRepeatable(field.getMaxOccurs()));
        formField.setType(mapUiFieldType(fieldUi, field));
        formField.setLayoutWidth(resolveLayoutWidth(fieldUi));
        formField.setMask(firstNonBlank(fieldUi != null ? fieldUi.getMask() : null, field.getMask()));
        formField.setMaxLength(resolveMaxLength(field, fieldUi));
        formField.setReadonly(fieldUi != null && fieldUi.isReadonly());
        formField.setVisible(true);

        if (field.getEnumValues() != null) {
            formField.getEnumValues().addAll(field.getEnumValues());
        }

        if ("0A0001D003A".equals(field.getId()) || "Field_0A0001D003A".equals(field.getId())
                || "Field_0A0001D003A".equals(field.getXmlName())) {
            LOGGER.info("FIELD_BIND_DEBUG mapped fieldId={} xmlName={} xmlPath={} formFieldType={} enumCount={} label={} maxLength={} mask={} fieldUiLabel={} fieldUiType={}",
                    field.getId(),
                    field.getXmlName(),
                    field.getXmlPath(),
                    formField.getType(),
                    formField.getEnumValues() == null ? 0 : formField.getEnumValues().size(),
                    formField.getLabel(),
                    formField.getMaxLength(),
                    formField.getMask(),
                    fieldUi == null ? null : fieldUi.getLabel(),
                    fieldUi == null ? null : fieldUi.getType());
        }

        return formField;
    }

    /**
     * Meghatározza a mező maximális beviteli hosszát UIModel-first prioritással.
     *
     * @param field az XSD meződefiníció
     * @param fieldUi az opcionális UIModel metaadat
     * @return az UIModelben megadott hossz, ennek hiányában az XSD-ből származó érték
     */
    private Integer resolveMaxLength(FieldDefinition field, UiModelMetadata.FieldUi fieldUi) {
        if (fieldUi != null && fieldUi.getMaxLength() != null) {
            return fieldUi.getMaxLength();
        }
        return field != null ? field.getMaxLength() : null;
    }

    /**
     * A UIModel rácsszélességét a frontend által használt oszlopszélességre képezi.
     *
     * <p>Hiányzó értéknél 6-os alapértéket ad; a nagyobb UIModel grid értékeket
     * 12, 8, 6, 4 vagy 3 szélességi kategóriára normalizálja.</p>
     *
     * @param fieldUi az opcionális UIModel mezőmetaadat
     * @return a normalizált layout-szélesség
     */
    private Integer resolveLayoutWidth(UiModelMetadata.FieldUi fieldUi) {
        if (fieldUi == null || fieldUi.getLayoutWidth() == null) {
            return 6;
        }

        int grid = fieldUi.getLayoutWidth();
        if (grid >= 10) return 12;
        if (grid >= 7) return 8;
        if (grid >= 5) return 6;
        if (grid >= 3) return 4;
        return 3;
    }

    /**
     * Meghatározza a frontend mezőtípusát az XSD és UIModel metaadatok alapján.
     *
     * <p>Az enumértékek mindig {@code select} típust eredményeznek. Egyébként előbb
     * a UIModel típusjelölését, majd az XSD adattípust vizsgálja; ezekből checkbox,
     * date, number vagy text típus készül.</p>
     *
     * @param fieldUi az opcionális UIModel mezőmetaadat
     * @param field az XSD-ből származó meződefiníció
     * @return a frontend által értelmezett mezőtípus neve
     */
    private String mapUiFieldType(UiModelMetadata.FieldUi fieldUi, FieldDefinition field) {
        if (field.getEnumValues() != null && !field.getEnumValues().isEmpty()) {
            return "select";
        }

        if (fieldUi != null && fieldUi.getType() != null && !fieldUi.getType().isBlank()) {
            String uiType = fieldUi.getType().toLowerCase(Locale.ROOT);
            if (uiType.contains("checkbox") || uiType.contains("boolean")) {
                return "checkbox";
            }
            if (uiType.contains("date")) {
                return "date";
            }
            if (uiType.contains("number") || uiType.contains("numeric")) {
                return "number";
            }
            if (uiType.contains("text")) {
                return "text";
            }
        }

        String dataType = field.getDataType();
        if (dataType == null) {
            return "text";
        }

        String normalized = dataType.toLowerCase(Locale.ROOT);
        if (normalized.contains("boolean")) {
            return "checkbox";
        }
        if (normalized.contains("date")) {
            return "date";
        }
        if (normalized.contains("decimal")
                || normalized.contains("int")
                || normalized.contains("integer")
                || normalized.contains("double")
                || normalized.contains("float")
                || normalized.contains("nonnegativeinteger")) {
            return "number";
        }

        return "text";
    }

    /**
     * Megállapítja az XSD {@code maxOccurs} értékéből, hogy a mező ismétlődhet-e.
     *
     * @param maxOccurs az XSD előfordulási felső korlátja
     * @return {@code true} {@code unbounded} vagy egynél nagyobb numerikus érték esetén
     */
    private boolean isRepeatable(String maxOccurs) {
        if (maxOccurs == null || maxOccurs.isBlank()) {
            return false;
        }
        if ("unbounded".equalsIgnoreCase(maxOccurs)) {
            return true;
        }
        try {
            return Integer.parseInt(maxOccurs) > 1;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /**
     * Visszaadja a felsorolt jelöltek közül az első nem üres szöveget.
     *
     * @param values prioritási sorrendben vizsgált szövegek
     * @return az első nem {@code null} és nem blank érték, vagy {@code null}
     */
    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
