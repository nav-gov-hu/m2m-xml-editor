package hu.gov.nav.xsdparsertool.web.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ClientDomSecurityRegressionTest {

    @Test
    void partnerAccessMustRenderServerDataWithoutInnerHtml() throws IOException {
        String script = resource("/static/js/pages/partner-access.js");
        assertFalse(script.contains("innerHTML"), "Partner access server data must not be rendered through innerHTML.");
        assertTrue(script.contains("textContent"));
        assertTrue(script.contains("replaceChildren"));
    }

    @Test
    void userEditMustRenderRolesWithoutInnerHtml() throws IOException {
        String script = resource("/static/js/pages/user-edit.js");
        assertFalse(script.contains("innerHTML"), "User edit API data must not be rendered through innerHTML.");
        assertTrue(script.contains("textContent"));
    }

    @Test
    void storedFormNavigationMustValidateSameOriginAndNumericFileId() throws IOException {
        String m2m = resource("/static/js/m2m/m2m-submitter-page.js");
        String shell = resource("/static/js/runtime/application-shell.js");
        assertTrue(m2m.contains("resolved.origin !== window.location.origin"));
        assertTrue(m2m.contains("resolved.pathname !== '/form.html'"));
        assertTrue(m2m.contains("!/^\\d+$/.test(fileId)"));
        assertTrue(shell.contains("resolved.origin !== window.location.origin"));
        assertTrue(shell.contains("resolved.pathname !== '/form.html'"));
        assertTrue(shell.contains("!/^\\d+$/.test(fileId)"));
    }

    @Test
    void adminReturnNavigationMustRemainSameOrigin() throws IOException {
        String script = resource("/static/js/admin/admin-ui.js");
        assertTrue(script.contains("window.history.back()"));
        assertTrue(script.contains("window.location.href = '/form.html'"));
        assertTrue(script.contains("target.origin === window.location.origin"));
        assertTrue(script.contains("target.pathname === '/form.html'"));
        assertTrue(script.contains("/^\\d+$/.test(fileId)"));
        assertFalse(script.contains("window.location.assign(target.pathname + target.search + target.hash)"));
        assertFalse(script.contains("if(returnUrl) location.href = returnUrl"));
        assertFalse(script.contains("returnUrl.startsWith('/')"));
    }

    @Test
    void multiformConfigurationNavigationMustUseFixedLocalDestination() throws IOException {
        String script = resource("/static/js/form/multiform-runtime.js");
        assertTrue(script.contains("navigationForm.action = '/xml-index-config.html'"));
        assertTrue(script.contains("navigationForm.method = 'GET'"));
        assertTrue(script.contains("navigationForm.submit()"));
        assertFalse(script.contains("window.location.assign('/xml-index-config.html?' + configParams.toString())"));
        assertFalse(script.contains("location.href = `/xml-index-config.html?${configParams}`"));
        assertFalse(script.contains("window.location.href = `/xml-index-config.html?${configParams.toString()}`"));
    }

    @Test
    void formValidationMustUseUnsavedInMemoryXmlWithoutQuickSave() throws IOException {
        String xsd = resource("/static/js/validation/xsd-validation.js");
        String xpath = resource("/static/js/validation/xpath-validation.js");
        String validationApi = resource("/static/js/validation/validation-api.js");
        String runtime = resource("/static/js/xml/xml-editor-runtime.js");
        String shell = resource("/static/js/runtime/application-shell.js");

        assertFalse(xsd.contains("ensureCurrentXmlSavedForValidation"),
                "Form XSD validation must not persist the XML before validation.");
        assertFalse(xpath.contains("ensureCurrentXmlSavedForValidation"),
                "Form XPath validation must not persist the XML before validation.");
        assertTrue(runtime.contains("await validateCurrentXmlInBrowserContext()"),
                "Form XSD validation must validate the current browser XML snapshot.");
        assertTrue(runtime.contains("renderInlineXsdResult: true"));
        assertTrue(shell.contains("options.renderInlineXsdResult === true || !currentActiveXmlFile?.id"),
                "Active file validation must render transient in-memory XSD errors in the form drawer.");
        assertTrue(xpath.contains("buildCurrentXmlSnapshot?.({ reason: 'xpath-validation' })"),
                "Form XPath validation must build a path-bound in-memory XML snapshot.");
        assertTrue(xpath.contains("startSnapshotXpathValidation({ xmlText, fileName })"));
        assertTrue(validationApi.contains("/api/xpath-validator/requests"),
                "Unsaved XPath validation must use the multipart snapshot endpoint.");
    }

    @Test
    void validationButtonsMustOpenDrawersAndRenderInlineXsdErrors() throws IOException {
        String runtime = resource("/static/js/xml/xml-editor-runtime.js");
        String xsdResult = resource("/static/js/validation/xsd-validation-result.js");
        String xpathResult = resource("/static/js/validation/xpath-validation-result.js");

        assertTrue(runtime.contains("openFormXsdValidationDrawer()"),
                "Manual XSD validation must open the XSD drawer while validation is running.");
        assertTrue(runtime.contains("updateFormXsdDrawerTab('running'"));
        assertTrue(xsdResult.contains("isFormXsdDrawerAvailable"),
                "Inline XSD results for an active registered XML must not depend on the XML-load button.");
        assertTrue(xsdResult.contains("renderXsdErrors(issues)"));
        assertTrue(xsdResult.contains("openXsdValidationDrawer()"));
        assertTrue(xpathResult.contains("export function showXpathLoading"));
        assertTrue(xpathResult.contains("openXpathValidationDrawer()"),
                "XPath validation must remain drawer-first instead of toast-only.");
    }

    @Test
    void xsdAndXpathDrawersMustStaySeparatedAndExposeAffectedFieldNavigation() throws IOException {
        String xpath = resource("/static/js/validation/xpath-validation.js");
        String xpathResult = resource("/static/js/validation/xpath-validation-result.js");
        String xsdResult = resource("/static/js/validation/xsd-validation-result.js");
        String drawerRuntime = resource("/static/js/validation/validation-drawer-runtime.js");

        assertTrue(xpath.contains("isXsdPrevalidationFailure(status, errors)"));
        assertTrue(xpath.contains("renderXpathXsdBlocked(status, errors.length)"));
        assertTrue(xpath.contains("renderXsdPrevalidationErrorsFromXpath(status, errors)"));
        assertTrue(xpathResult.contains("Nincs XPath hibalista, mert az XPath szabályellenőrzés az XSD hibák miatt nem indult el."));
        assertTrue(xsdResult.contains("data-xsd-field-index"));
        assertTrue(xsdResult.contains("focusFormFieldFromXpathError"));
        assertTrue(drawerRuntime.contains("<th>Érintett mező</th>"));
        assertTrue(xpathResult.contains("resultDownloadUrl"));
    }

    @Test
    void xsdFieldHighlightMustNotRenderErrorTextAndMustClearOnEdit() throws IOException {
        String drawerRuntime = resource("/static/js/validation/validation-drawer-runtime.js");
        String editorRuntime = resource("/static/js/xml/xml-editor-runtime.js");
        String multiformRuntime = resource("/static/js/form/multiform-runtime.js");
        String multiformCss = resource("/static/styles/multiform.css");

        assertFalse(drawerRuntime.contains("field.dataset.xsdErrorMessage ="),
                "XSD error text must remain in the drawer, not be injected below the field.");
        assertFalse(drawerRuntime.contains("field.title = msg"),
                "XSD error details must not be duplicated as a field tooltip.");
        assertFalse(multiformCss.contains("content: attr(data-xsd-error-message)"),
                "The red field marker must not render the XSD message inline.");
        assertTrue(drawerRuntime.contains("function clearEditedFieldXsdHighlight(control)"));
        assertTrue(editorRuntime.contains("globalThis.clearEditedFieldXsdHighlight?.(input)"),
                "Editing a normal form field must clear its stale XSD marker immediately.");
        assertTrue(multiformRuntime.contains("globalThis.clearEditedFieldXsdHighlight?.(input)"),
                "Editing a multiform detail field must clear its stale XSD marker immediately.");
        assertTrue(drawerRuntime.contains("xsdEditedPathsSinceValidation.clear()"),
                "A fresh XSD result must be authoritative and may reapply the marker if the field is still invalid.");
    }

    @Test
    void normalMultiformSnapshotMustMergeActiveDetailBackIntoFullXml() throws IOException {
        String runtime = resource("/static/js/xml/xml-editor-runtime.js");
        assertTrue(runtime.contains("function syncActiveMultiformDetailToCurrentDocument()"));
        assertTrue(runtime.contains("const occurrenceIndex = Number(row?.index || state?.selectedIndex || 0)"));
        assertTrue(runtime.contains("const replacement = currentXmlDocument.importNode(row.element, true)"));
        assertTrue(runtime.contains("target.replaceWith(replacement)"));
        assertTrue(runtime.contains("const multiformSync = syncActiveMultiformDetailToCurrentDocument()"));
        assertTrue(runtime.contains("currentActiveXmlFile?.largeFileMode === true"),
                "Large XML must keep using its dedicated fragment-save path.");
    }

    @Test
    void validationMustShowProgressOverlayAndKeepDrawerResultFlow() throws IOException {
        String runtime = resource("/static/js/xml/xml-editor-runtime.js");
        String xpath = resource("/static/js/validation/xpath-validation.js");

        assertTrue(runtime.contains("NavProcessingJobs?.showLocal?.({"),
                "Manual XSD validation must restore the processing progress dialog.");
        assertTrue(runtime.contains("title: 'XSD ellenőrzés'"));
        assertTrue(runtime.contains("NavProcessingJobs?.hide?.()"));
        assertTrue(xpath.contains("NavProcessingJobs?.showLocal?.({"),
                "Manual XPath validation must restore the processing progress dialog.");
        assertTrue(xpath.contains("title: 'XPath ellenőrzés'"));
        assertTrue(xpath.contains("NavProcessingJobs?.hide?.()"));
    }

    @Test
    void multiformMainPartMustNotRebindAttachmentFieldsAndChainElementsMustBeSeparated() throws IOException {
        String renderer = resource("/static/js/form/form-renderer-runtime.js");
        String multiform = resource("/static/js/form/multiform-runtime.js");

        assertTrue(renderer.contains("function declaredFormPartFromPath(xmlPath)"));
        assertTrue(renderer.contains("!uiModelDefinitionBelongsToActivePart(field, row)"),
                "UIModel fields declared for a different Form_* part must not be rebound into the main form.");
        assertTrue(renderer.contains("rowInstancesForActiveFormPart"),
                "Repeatable rows must be filtered by the active Form_* part.");
        assertTrue(multiform.contains("const seenPaths = new Set()"),
                "The main multiform panel must not render the same concrete XML path more than once.");
        assertTrue(multiform.contains("Chain_elem"));
        assertTrue(multiform.contains("runtimeDetailGroupDescriptor(partName, field)"));
        assertTrue(multiform.contains("${chainLabel || 'Lánc elem'} / ${occurrence}. elem"),
                "Each Chain_elem must render in its own numbered detail block.");
        assertTrue(multiform.contains("multiform-detail-chain-card"));
    }

    @Test
    void quickSaveMustDetectPathBoundDomChangesEvenWhenDirtyFlagWasMissed() throws IOException {
        String runtime = resource("/static/js/xml/xml-editor-runtime.js");
        String service = resource("/static/js/xml/xml-save-service.js");
        assertTrue(runtime.contains("function hasUnsyncedFormControlChanges()"));
        assertTrue(runtime.contains("!currentFormHasUnsavedChanges && !hasUnsyncedFormControlChanges()"));
        assertTrue(service.contains("runtimeApi?.hasUnsyncedFormControlChanges?.() !== true"));
        assertTrue(runtime.contains("const saveDocument = parseXmlString(serializeXml(currentXmlDocument))"));
        assertTrue(runtime.contains("syncFormControlsToDocument(saveDocument, controls)"));
        assertTrue(service.contains("runXsdValidation: true"));
    }


    @Test
    void xsdDrawerMustOpenAffectedMultiformOccurrenceAndCollapseGenericDuplicateErrors() throws IOException {
        String multiform = resource("/static/js/form/multiform-runtime.js");
        String drawerRuntime = resource("/static/js/validation/validation-drawer-runtime.js");
        String xsdResult = resource("/static/js/validation/xsd-validation-result.js");

        assertTrue(multiform.contains("ensureMultiformValidationTargetVisible"));
        assertTrue(multiform.contains("extractFormPartOccurrenceFromValidationPath"));
        assertTrue(multiform.contains("openRuntimeDetailTab(row, { validationNavigation:true })"),
                "Validation navigation must open the concrete attachment occurrence before resolving the field.");
        assertTrue(drawerRuntime.contains("await globalThis.ensureMultiformValidationTargetVisible"),
                "XSD/XPath field navigation must switch to the affected multiform occurrence before focusing the field.");
        assertTrue(xsdResult.contains("export function dedupeXsdIssues"));
        assertTrue(xsdResult.contains("cvc-type\\.3\\.1\\.3"),
                "The generic cvc-type echo must be collapsed when a more specific XSD error describes the same value and field.");
    }

    @Test
    void printingMustNotDependOnPopupWindows() throws IOException {
        String printUi = resource("/static/js/print/print-ui.js");
        String form = resource("/static/form.html");

        assertFalse(printUi.contains("window.open("),
                "Printing must not depend on popup windows because browsers or enterprise policy may block them.");
        assertTrue(printUi.contains("frame.srcdoc = html"),
                "Browser printing must render the generated print HTML in an in-page iframe.");
        assertTrue(printUi.contains("printWindow.print()"),
                "Browser printing must invoke print on the in-page print document.");
        assertTrue(printUi.contains("link.download = suggestedPdfFileName()"),
                "PDF generation must use a direct download instead of a popup preview.");
        assertTrue(form.contains("Böngésző nyomtatás"));
        assertTrue(form.contains("PDF letöltés"));
    }

    @Test
    void toastUxMustStayBottomRightAndValidationMustNotDuplicateDrawerResults() throws IOException {
        String dialogsCss = resource("/static/styles/dialogs.css");
        String applicationShell = resource("/static/js/runtime/application-shell.js");
        String xmlFiles = resource("/static/js/xml/xml-files.js");
        String multiform = resource("/static/js/form/multiform-runtime.js");

        assertTrue(dialogsCss.contains("bottom: 22px;"),
                "Desktop toast notifications must stay in the less intrusive bottom-right corner.");
        assertFalse(dialogsCss.contains("top: 92px;"));
        assertFalse(applicationShell.substring(applicationShell.indexOf("function showMessage"),
                        applicationShell.indexOf("function addSummaryItem"))
                        .contains("navShowToast"),
                "Form and validation status messages must remain inline/drawer based instead of duplicating as toasts.");
        assertTrue(xmlFiles.contains("window.navShowToast(messageText, type || 'info')"),
                "Short XML file operations may continue to use transient toast feedback.");
        assertFalse(multiform.contains("{ persistent:true }"),
                "Multiform warnings must not create persistent toast notifications.");
    }

    private String resource(String path) throws IOException {
        try (var input = getClass().getResourceAsStream(path)) {
            if (input == null) {
                throw new IOException("Missing test resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
