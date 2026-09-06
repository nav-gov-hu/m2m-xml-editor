package hu.gov.nav.xsdparsertool.web.api.dto;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormData;
import hu.gov.nav.xsdparsertool.core.model.form.FormDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormFieldDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormValue;
import hu.gov.nav.xsdparsertool.core.model.form.FormRowDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormSectionDefinition;
import hu.gov.nav.xsdparsertool.core.model.form.FormTabDefinition;
import hu.gov.nav.xsdparsertool.core.model.processing.ProcessingResult;
import hu.gov.nav.xsdparsertool.core.model.xmlview.XmlDocumentView;
import hu.gov.nav.xsdparsertool.core.model.xmlview.XmlNodeView;
import hu.gov.nav.xsdparsertool.core.model.processing.ValidationResult;
import hu.gov.nav.xsdparsertool.core.model.validation.ValidationIssue;
import hu.gov.nav.xsdparsertool.schemaregistry.model.XmlProbeResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A web modul REST API területének közös alkalmazási típusa.
 *
 * <p>A {@code ApiResponseMapper} osztály a web modul REST API területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
public final class ApiResponseMapper {
    /**
     * Létrehozza a {@code ApiResponseMapper} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     */
    private ApiResponseMapper() {}
/**
 * A processing inspect eredményét webes inspect válasz DTO-vá alakítja.
 * @param probe a {@code probe} paraméter átadott értéke
 * @param processingResult a {@code processingResult} paraméter átadott értéke
 * @param originalFileName a {@code originalFileName} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    public static InspectResponse toInspectResponse(XmlProbeResult probe, ProcessingResult processingResult, String originalFileName) {
        InspectResponse response = new InspectResponse();
        response.setXml(toXmlProbeDto(probe, originalFileName));
        response.setSchemaBundle(toSchemaBundleDto(processingResult.getSchemaBundle()));
        response.setDocumentDefinition(toDocumentDefinitionDto(processingResult.getDocumentDefinition()));
        return response;
    }
/**
 * A validációs eredményt webes validációs válasz DTO-vá alakítja.
 * @param probe a {@code probe} paraméter átadott értéke
 * @param inspection a {@code inspection} paraméter átadott értéke
 * @param validation a {@code validation} paraméter átadott értéke
 * @param formDefinition a {@code formDefinition} paraméter átadott értéke
 * @param formData a {@code formData} paraméter átadott értéke
 * @param xmlView a {@code xmlView} paraméter átadott értéke
 * @param originalFileName a {@code originalFileName} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    public static ValidateResponse toValidateResponse(XmlProbeResult probe, ProcessingResult inspection, ValidationResult validation, FormDefinition formDefinition, FormData formData, XmlDocumentView xmlView, String originalFileName) {
        ValidateResponse response = new ValidateResponse();
        response.setValid(validation.isValid());
        response.setXml(toXmlProbeDto(probe, originalFileName));
        response.setSchemaBundle(toSchemaBundleDto(inspection.getSchemaBundle()));
        response.setDocumentDefinition(toDocumentDefinitionDto(inspection.getDocumentDefinition()));
        response.setFormDefinition(formDefinition != null ? toFormDefinitionDto(formDefinition) : null);
        response.setFormData(formData != null ? toFormDataDto(formData) : null);
        response.setXmlView(xmlView != null ? toXmlViewDto(xmlView) : null);
        for (ValidationIssue issue : validation.getIssues()) {
            ValidationIssueDto dto = new ValidationIssueDto();
            dto.setCode(issue.getCode());
            dto.setPath(issue.getPath());
            dto.setMessage(issue.getMessage());
            dto.setSeverity(issue.getSeverity() != null ? issue.getSeverity().name() : null);
            response.getIssues().add(dto);
        }
        return response;
    }
/**
 * Az XML probe domain eredményét REST DTO-vá alakítja.
 * @param probe a {@code probe} paraméter átadott értéke
 * @param originalFileName a {@code originalFileName} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    @Schema(description = "HU: dto mező. EN: dto field.")

    private static XmlProbeDto toXmlProbeDto(XmlProbeResult probe, String originalFileName) {
        XmlProbeDto dto = new XmlProbeDto();
        dto.setFileName(originalFileName);
        if (probe != null) {
            dto.setRootElementName(probe.getRootElementName());
            dto.setNamespace(probe.getNamespace());
            dto.setSchemaLocation(probe.getSchemaLocation());
            dto.setNoNamespaceSchemaLocation(probe.getNoNamespaceSchemaLocation());
        }
        return dto;
    }
/**
 * A feloldott SchemaBundle adatait REST válaszreprezentációvá alakítja.
 * @param schemaBundle a {@code schemaBundle} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private static SchemaBundleDto toSchemaBundleDto(SchemaBundle schemaBundle) {
        if (schemaBundle == null) return null;
        SchemaBundleDto dto = new SchemaBundleDto();
        dto.setDocumentType(schemaBundle.getDocumentType());
        dto.setDocumentVersion(schemaBundle.getDocumentVersion());
        dto.setRootElementName(schemaBundle.getRootElementName());
        dto.setTargetNamespace(schemaBundle.getTargetNamespace());
        dto.setMatchReason(schemaBundle.getMatchReason());
        dto.setPrimaryXsd(schemaBundle.getPrimaryXsd() != null ? schemaBundle.getPrimaryXsd().toString() : null);
        dto.setUiModelFile(schemaBundle.getUiModelFile() != null ? schemaBundle.getUiModelFile().toString() : null);
        dto.setPageSchemaFile(schemaBundle.getPageSchemaFile() != null ? schemaBundle.getPageSchemaFile().toString() : null);
        if (schemaBundle.getUiModelFile() != null) {
            try {
                hu.gov.nav.xsdparsertool.uimodel.model.UiModelMetadata uiMeta =
                        new hu.gov.nav.xsdparsertool.uimodel.service.XmlUiModelParserService().parse(schemaBundle.getUiModelFile());
                if (uiMeta.getTitle() != null && !uiMeta.getTitle().isBlank()) {
                    dto.setFormName(uiMeta.getTitle());
                }
                if (uiMeta.getInfo() != null && !uiMeta.getInfo().isBlank()) {
                    dto.setFormInfo(uiMeta.getInfo());
                }
                if (uiMeta.getVersion() != null && !uiMeta.getVersion().isBlank()) {
                    dto.setFormVersion(uiMeta.getVersion());
                }
                if (uiMeta.getType() != null && !uiMeta.getType().isBlank()) {
                    dto.setFormType(uiMeta.getType());
                }
            } catch (Exception ignored) {
            }
        }
        if (schemaBundle.getXsdFiles() != null) {
            schemaBundle.getXsdFiles().forEach(path -> dto.getXsdFiles().add(path.toString()));
        }
        return dto;
    }
/**
 * A dokumentumdefiníciót a frontend számára sorosítható DTO-vá alakítja.
 * @param definition a {@code definition} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private static DocumentDefinitionDto toDocumentDefinitionDto(DocumentDefinition definition) {
        if (definition == null) return null;
        DocumentDefinitionDto dto = new DocumentDefinitionDto();
        dto.setId(definition.getId());
        dto.setName(definition.getName());
        dto.setTitle(definition.getTitle());
        dto.setRootElementName(definition.getRootElementName());
        dto.setTargetNamespace(definition.getTargetNamespace());
        dto.setBlockCount(definition.getBlocks() != null ? definition.getBlocks().size() : 0);
        return dto;
    }
/**
 * Az űrlapadat-domain modellt REST DTO-vá alakítja.
 * @param data a {@code data} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */


    @Schema(description = "HU: dto mező. EN: dto field.")


    private static FormDataDto toFormDataDto(FormData data) {
        FormDataDto dto = new FormDataDto();
        data.getValuesByFieldId().forEach((k, v) -> {
            FormValueDto fv = toFormValueDto(v);
            dto.getValuesByFieldId().put(k, fv);
        });
        data.getRowInstancesByRowId().forEach((rowId, instances) -> {
            for (hu.gov.nav.xsdparsertool.core.model.form.FormRowInstance instance : instances) {
                FormRowInstanceDto instanceDto = new FormRowInstanceDto();
                instanceDto.setId(instance.getId());
                instanceDto.setXmlPath(instance.getXmlPath());
                instance.getValuesByFieldId().forEach((fieldId, value) -> instanceDto.getValuesByFieldId().put(fieldId, toFormValueDto(value)));
                dto.getRowInstancesByRowId().computeIfAbsent(rowId, key -> new java.util.ArrayList<>()).add(instanceDto);
            }
        });
        return dto;
    }
/**
 * Egy mezőérték domain objektumot REST DTO-vá alakít.
 * @param v a {@code v} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    @Schema(description = "HU: fv mező. EN: fv field.")

    private static FormValueDto toFormValueDto(FormValue v) {
        FormValueDto fv = new FormValueDto();
        fv.setKey(v.getKey());
        fv.setFieldId(v.getFieldId());
        fv.setXmlPath(v.getXmlPath());
        fv.setValue(v.getValue());
        fv.setPresent(v.isPresent());
        return fv;
    }
/**
 * Az XML nézetmodellt REST válasz DTO-vá alakítja.
 * @param view a {@code view} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    @Schema(description = "HU: dto mező. EN: dto field.")

    private static XmlDocumentViewDto toXmlViewDto(XmlDocumentView view) {
        XmlDocumentViewDto dto = new XmlDocumentViewDto();
        dto.setRawXml(view.getRawXml());
        dto.setRoot(toXmlNodeDto(view.getRoot()));
        return dto;
    }
/**
 * Rekurzívan REST DTO-vá alakít egy XML-fa csomópontot és gyermekelemeit.
 * @param node a {@code node} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    private static XmlNodeViewDto toXmlNodeDto(XmlNodeView node) {
        if (node == null) return null;
        XmlNodeViewDto dto = new XmlNodeViewDto();
        dto.setName(node.getName());
        dto.setPath(node.getPath());
        dto.setTextValue(node.getTextValue());
        dto.setElement(node.isElement());
        dto.setAttributes(node.getAttributes());
        for (XmlNodeView child : node.getChildren()) {
            dto.getChildren().add(toXmlNodeDto(child));
        }
        return dto;
    }
/**
 * Az űrlapdefiníciót frontend renderelésre alkalmas REST DTO-vá alakítja.
 * @param definition a {@code definition} paraméter átadott értéke
 * @return a metódus által előállított eredmény
 */

    @Schema(description = "HU: dto mező. EN: dto field.")

    private static FormDefinitionDto toFormDefinitionDto(FormDefinition definition) {
        FormDefinitionDto dto = new FormDefinitionDto();
        dto.setId(definition.getId());
        dto.setTitle(definition.getTitle());
        dto.setStructuralLabelsByPath(definition.getStructuralLabelsByPath());
        for (FormTabDefinition tab : definition.getTabs()) {
            FormTabDto tabDto = new FormTabDto();
            tabDto.setId(tab.getId());
            tabDto.setTitle(tab.getTitle());
            for (FormSectionDefinition section : tab.getSections()) {
                FormSectionDto sectionDto = new FormSectionDto();
                sectionDto.setId(section.getId());
                sectionDto.setTitle(section.getTitle());
                for (FormRowDefinition row : section.getRows()) {
                    FormRowDto rowDto = new FormRowDto();
                    rowDto.setId(row.getId());
                    rowDto.setTitle(row.getTitle());
                    rowDto.setType(row.getType());
                    rowDto.setRepeatable(row.isRepeatable());
                    rowDto.setXmlPath(row.getXmlPath());
                    for (FormFieldDefinition field : row.getFields()) {
                        FormFieldDto fieldDto = new FormFieldDto();
                        fieldDto.setId(field.getId());
                        fieldDto.setXmlName(field.getXmlName());
                        fieldDto.setXmlPath(field.getXmlPath());
                        fieldDto.setLabel(field.getLabel());
                        fieldDto.setUiLabel(field.getUiLabel());
                        fieldDto.setXsdLabel(field.getXsdLabel());
                        fieldDto.setType(field.getType());
                        fieldDto.setRequired(field.isRequired());
                        fieldDto.setRepeatable(field.isRepeatable());
                        fieldDto.setVisible(field.isVisible());
                        fieldDto.setReadonly(field.isReadonly());
                        fieldDto.setMask(field.getMask());
                        fieldDto.setMaxLength(field.getMaxLength());
                        fieldDto.setLayoutWidth(field.getLayoutWidth());
                        fieldDto.getEnumValues().addAll(field.getEnumValues());
                        rowDto.getFields().add(fieldDto);
                    }
                    sectionDto.getRows().add(rowDto);
                }
                tabDto.getSections().add(sectionDto);
            }
            dto.getTabs().add(tabDto);
        }
        return dto;
    }
}
