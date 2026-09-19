package hu.gov.nav.xsdparsertool.web.form;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiformNewPartialDocumentFrontendContractTest {

    private static String source;
    private static String applicationShellSource;
    private static String xmlEditorSource;

    @BeforeAll
    static void loadSource() throws Exception {
        source = normalizeLineEndings(new ClassPathResource("static/js/form/multiform-runtime.js")
                .getContentAsString(StandardCharsets.UTF_8));
        applicationShellSource = normalizeLineEndings(new ClassPathResource("static/js/runtime/application-shell.js")
                .getContentAsString(StandardCharsets.UTF_8));
        xmlEditorSource = normalizeLineEndings(new ClassPathResource("static/js/xml/xml-editor-runtime.js")
                .getContentAsString(StandardCharsets.UTF_8));
    }

    private static String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    @Test
    void multiformDetectionAlsoUsesFormDefinitionWhenXmlContainsOnlyRoot() {
        assertTrue(source.contains("function runtimeDefinedFormPartNames()"));
        assertTrue(source.contains("const definedNames = runtimeDefinedFormPartNames();"));
        assertTrue(source.contains("schemaDeclaresMultipleParts"));
    }

    @Test
    void mainPanelKeepsOnlyBlocksBelongingToMainFormPart() {
        assertTrue(source.contains("const allowedBlockNames = new Set("));
        assertTrue(source.contains("runtimeFormPartBlockDescriptors(partName)"));
        assertTrue(source.contains("if(!belongsToPart) blockPanel.remove();"));
    }

    @Test
    void newRepeatingPartStartsAsDraftWithoutChangingXmlOrList() {
        assertTrue(source.contains("function createRuntimeRepeatingPartDraft(part)"));
        assertTrue(source.contains("isDraft: true"));
        assertTrue(source.contains("state.draftRow = createRuntimeRepeatingPartDraft(part);"));
        assertTrue(source.contains("state.repeatingView = 'draft';"));
        assertFalse(source.substring(source.indexOf("async function addRuntimeRepeatingPartOccurrence()"),
                source.indexOf("function renderRuntimeRepeatingPartPanel"))
                .contains("createNodeByPath(currentXmlDocument"));
    }

    @Test
    void draftHasSeparateTopLevelTabAndOwnSaveAction() {
        assertTrue(source.contains("function ensureRuntimeDraftShellPanel(options = {})"));
        assertTrue(source.contains("tab.className = 'multiform-runtime-tab multiform-runtime-draft-tab';"));
        assertTrue(source.contains("tab.textContent = 'Új melléklap';"));
        assertTrue(source.contains("panel.className = 'multiform-runtime-panel multiform-runtime-draft-panel';"));
        assertTrue(source.contains("saveButton.textContent = 'Melléklap mentése';"));
        assertTrue(source.contains("saveButton.addEventListener('click', materializeRuntimeRepeatingPartDraft);"));
    }

    @Test
    void draftSaveRequiresConfiguredIndexFieldsBeforeMaterialization() {
        assertTrue(source.contains("function validateRuntimeRepeatingPartDraft(row)"));
        assertTrue(source.contains("runtimeDraftRequiredIndexFields(state)"));
        assertTrue(source.contains("if(!validateRuntimeRepeatingPartDraft(row)) return;"));
        assertTrue(source.contains("const element = createNodeByPath(currentXmlDocument, partPath);"));
    }

    @Test
    void emptyFormPartIsNotRenderedAsEditableFormNameField() {
        assertTrue(source.contains("if(rootNode) return;"));
        assertTrue(source.contains("Maga a Form_* részbizonylat nem mező"));
    }

    @Test
    void viewTogglesPreserveActiveMultiformPanelAndDraft() {
        assertTrue(source.contains("function captureMultiformRuntimeViewState()"));
        assertTrue(source.contains("function restoreMultiformRuntimeViewState(snapshot)"));
        assertTrue(applicationShellSource.contains("const multiformViewState = globalThis.captureMultiformRuntimeViewState?.() || null;"));
        assertTrue(applicationShellSource.contains("globalThis.restoreMultiformRuntimeViewState?.(multiformViewState);"));
    }

    @Test
    void newRepeatingOccurrenceCanRenderDefinitionFieldsWithoutImmediateMaterialization() {
        assertTrue(source.contains("function isM2mAttachmentTechnicalXmlPath(path)"));
        assertTrue(source.contains("function runtimeDefinitionLeafFields(partName, occurrenceIndex)"));
        assertTrue(source.contains("row?.isDraft !== true"));
        assertTrue(source.contains("row.draftValues[leaf.path] = input.value;"));
    }
    @Test
    void draftInputIsExcludedFromGlobalXmlValueSyncUntilDraftSave() {
        assertTrue(xmlEditorSource.contains("if(input.closest('.multiform-draft-form-host')) return;"));
        assertTrue(source.contains("if(event.target.closest?.('.multiform-draft-form-host')) return;"));
    }

    @Test
    void existingRepeatingOccurrenceOpensBesideRepeatingListAsTopLevelTab() {
        assertTrue(source.contains("function ensureRuntimeDetailShellPanel(row, options = {})"));
        assertTrue(source.contains("tab.className = 'multiform-runtime-tab multiform-runtime-detail-tab';"));
        assertTrue(source.contains("tab.textContent = `${row.index}. melléklap`;"));
        assertTrue(source.contains("panel.className = 'multiform-runtime-panel multiform-runtime-detail-panel';"));
        assertTrue(source.contains("state.tabsElement?.appendChild(tab);"));
        assertTrue(source.contains("multiform-detail-tab-host"));
        assertFalse(source.contains("multiform-repeating-view-tab"));
    }

    @Test
    void existingRepeatingOccurrenceCanBeDeletedFromItsDetailHeader() {
        assertTrue(source.contains("deleteButton.textContent = 'Melléklap törlése';"));
        assertTrue(source.contains("deleteButton.addEventListener('click', () => deleteRuntimeRepeatingPartOccurrence(row));"));
        assertTrue(source.contains("async function deleteRuntimeRepeatingPartOccurrence(row)"));
        assertTrue(source.contains("const currentOccurrences = directElementChildren(root).filter(child => xmlLocalName(child) === part.name);"));
        assertTrue(source.contains("const element = currentOccurrences[Number(row.index) - 1] || null;"));
        assertTrue(source.contains("element.parentNode.removeChild(element);"));
        assertTrue(source.contains("globalThis.clearXmlNodePathCache?.();"));
        assertTrue(xmlEditorSource.contains("clearXmlNodePathCache,"));
        assertTrue(source.contains("rebuildRuntimeRepeatingRowsFromCurrentDocument();"));
        assertTrue(source.contains("state.localStructuralChanges = true;"));
        assertTrue(source.contains("confirmText:'Melléklap törlése'"));
        assertTrue(source.contains("removeRuntimeShellPanel(detailPanelKey);"));
        assertTrue(source.contains("state.activatePanel?.(part.name, part.name, { validationNavigation:true });"));
        assertTrue(source.contains("state.markPanelDirty?.(part.name);"));
    }

    @Test
    void localStructuralChangesDoNotReloadStaleRowsFromServerIndexBeforeSave() {
        assertTrue(source.contains("if(state.localStructuralChanges === true)"));
        assertTrue(source.contains("refreshRuntimeLocalTablePage(page);"));
        assertTrue(source.contains("localStructuralChanges:state.localStructuralChanges === true"));
        assertTrue(source.contains("state.localStructuralChanges = snapshot.localStructuralChanges === true;"));
    }

    @Test
    void toolbarRerenderRebuildsFormDataAndReloadsActiveRepeatingList() {
        assertTrue(applicationShellSource.contains("function rerenderCurrentUiModelPreservingMultiformView()"));
        assertTrue(applicationShellSource.contains("currentFormData = buildFormDataFromDocument(currentFormDefinition, currentXmlDocument);"));
        assertTrue(source.contains("serverPage:Number(state.serverPage || 0)"));
        assertTrue(source.contains("tableColumns:Array.isArray(state.tableColumns) ? state.tableColumns.map(column => ({...column})) : []"));
        assertTrue(source.contains("void loadRuntimeTablePage(state.serverPage).then(() =>"));
        assertTrue(source.contains("refreshRuntimeRepeatingPanel(panel, { keepSuggestions:true });"));
    }

    @Test
    void formMetadataToolbarButtonsAreHiddenOnlyOnRepeatingListTab() {
        assertTrue(source.contains("function updateMultiformListToolbarControlVisibility(panelKey)"));
        assertTrue(source.contains("panelKey === state.repeatingPart.name"));
        assertTrue(source.contains("['uiModelDetailsToggle', 'toggleEmptyUiModelFieldsButton']"));
        assertTrue(source.contains("control.hidden = repeatingListActive;"));
        assertTrue(source.contains("updateMultiformListToolbarControlVisibility(panelKey);"));
        assertTrue(source.contains("updateMultiformListToolbarControlVisibility(null);"));
    }

    @Test
    void repeatingTabCounterTracksCurrentXmlOccurrenceCount() {
        assertTrue(source.contains("function updateRuntimeRepeatingTabLabel()"));
        assertTrue(source.contains("part.count = rows.length;"));
        assertTrue(source.contains("updateRuntimeRepeatingTabLabel();"));
        assertTrue(source.contains("state.repeatingPart.count = state.totalSuggestions;"));
    }

}
