/**
 * @module runtime/application-runtime
 *
 * A alkalmazási runtime- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

import { runCurrentFormXpathValidation as runModularXpathValidation } from '../validation/xpath-validation.js';
import { countXsdValidationErrors as countModularXsdValidationErrors } from '../validation/validation-status-utils.js';
import {
  renderInlineXsdValidationResult as renderModularInlineXsdValidationResult,
  renderStoredXsdValidationResult as renderModularStoredXsdValidationResult
} from '../validation/xsd-validation-result.js';
import {
  findFormFieldByXmlPath as findModularFormFieldByXmlPath,
  resolveXpathErrorTarget as resolveModularXpathErrorTarget
} from '../validation/xpath-error-navigation.js';
import { createAdminUi } from '../admin/admin-ui.js';
import { createPrintUi } from '../print/print-ui.js';
import { createSchemaRegistryUi } from '../schema/schema-registry-ui.js';
import { createSecurityHeaderUi } from '../security/security-header-ui.js';
import { createHomeUi } from '../home/home-ui.js';
import { createXmlSessionUi } from '../xml/xml-session-ui.js';
import { createFormFieldNavigation, canonicalizeXmlPath, pathMatches } from '../form/form-field-navigation.js';
import { createFormLazyRenderer } from '../form/form-lazy-renderer.js';
import { initializeRuntimeContext } from './runtime-context.js';

initializeRuntimeContext();
Object.assign(globalThis, {
  runModularXpathValidation,
  countModularXsdValidationErrors,
  renderModularInlineXsdValidationResult,
  renderModularStoredXsdValidationResult,
  findModularFormFieldByXmlPath,
  resolveModularXpathErrorTarget,
  canonicalizeXmlPath,
  pathMatches
});

await import('./application-shell.js');
await import('../form/multiform-runtime.js');
await import('../form/form-renderer-runtime.js');
await import('../xml/xml-editor-runtime.js');
await import('../xml/xml-workflow-runtime.js');
await import('../validation/validation-drawer-runtime.js');

globalThis.formLazyRenderer = createFormLazyRenderer({
  getScrollRoot: () => formScroll || null,
  onHydrated: (_card, content) => {
    applyCurrentXsdValidationHighlights(content);
    if(selectedFieldId || selectedXmlPath) highlightSelections();
  }
});

document.addEventListener('focusin', event => rememberFormFocusTarget(event.target));

globalThis.schemaRegistryUi = createSchemaRegistryUi({
  elements: { schemaRegistryText, schemaRegistryFill, reloadSchemaRegistryButton },
  showMessage
});
schemaRegistryUi.init();

globalThis.formFieldNavigationUi = createFormFieldNavigation({
  elements: {
    formContainer, formScroll, xmlTreePanel, fieldSearchInput, fieldSearchResults,
    fieldSearchPrevButton, fieldSearchNextButton, fieldSearchCounter,
    selectedXPathLabel, xpathCopySuccess,
    toggleAllFormCollapseButton
  },
  getSelection: () => ({ selectedFieldId, selectedXmlPath }),
  setSelection: next => {
    if(!next || typeof next !== 'object') return;
    if(Object.prototype.hasOwnProperty.call(next, 'selectedFieldId')) selectedFieldId = next.selectedFieldId || null;
    if(Object.prototype.hasOwnProperty.call(next, 'selectedXmlPath')) selectedXmlPath = next.selectedXmlPath || null;
  },
  callbacks: {
    escapeHtml,
    findFormFieldByXmlPath: findModularFormFieldByXmlPath,
    ensureSectionRendered: card => formLazyRenderer.ensureSection(card),
    ensureAllFormSectionsRendered: () => formLazyRenderer.ensureAll(),
    ensureFormSectionsForSearch: query => formLazyRenderer.ensureMatching(query),
    ensureXmlTreeRendered: () => ensureActiveXmlViewRendered('tree')
  }
});

globalThis.printUi = createPrintUi({
  elements: {
    schemaDirInput, generalXsdDirInput, xmlFileInput, xmlPathInput,
    printHtmlButton, printPdfButton, printMenuButton, printMenu, printRunButton,
    printShowFieldIdsCheckbox, printOnlyFilledFieldsCheckbox, printUiModelPathInput
  },
  showMessage,
  escapeHtml,
  parseXml: parseXmlString,
  suggestXmlFileName,
  getCurrentMode: currentMode,
  getCurrentXmlText: () => {
    const editorValue = xmlSourceEditor?.value;
    if(xmlSourceDirtySinceLastApply && editorValue && editorValue.trim()) return editorValue;
    if(currentXmlDocument) return serializeXml(currentXmlDocument);
    return editorValue?.trim() ? editorValue : '';
  }
});
printUi.init();

globalThis.securityUi = createSecurityHeaderUi();
globalThis.homeUi = createHomeUi({
  elements: { healthBadge, healthSummary, configBadge, homeConfigSummary, appVersionFooter, schemaDirInput, generalXsdDirInput, xmlPathInput },
  escapeHtml,
  syncSelectedXmlSourceDisplay,
  onConfigLoaded: data => {
    appConfig = data;
    if(!sessionStorage.getItem(UI_STATE_STORAGE_KEY)){
      currentFormRenderer = normalizeFormRendererMode(data.formRendererDefault || 'uimodel');
      currentViewMode = 'table';
    }
    updateFormRendererSwitch();
    securityUi.applyHeaderMenuVisibility(data.headerMenuVisibility);
  }
});
homeUi.init();

globalThis.xmlSessionUi = createXmlSessionUi({
  elements: { xmlPathInput, schemaDirInput, generalXsdDirInput, closeActiveXmlButton },
  getState: () => ({ currentActiveXmlFile, currentActiveXmlFileSessionId, currentXmlFileReadOnlyMode, currentFormHasUnsavedChanges, currentUiModelMissingFieldsVisible, currentXsdValidationState }),
  setState: next => {
    if(!next || typeof next !== 'object') return;
    if(Object.prototype.hasOwnProperty.call(next, 'currentActiveXmlFile')) currentActiveXmlFile = next.currentActiveXmlFile;
    if(Object.prototype.hasOwnProperty.call(next, 'currentActiveXmlFileSessionId')) currentActiveXmlFileSessionId = next.currentActiveXmlFileSessionId;
    if(Object.prototype.hasOwnProperty.call(next, 'currentXmlFileReadOnlyMode')) currentXmlFileReadOnlyMode = next.currentXmlFileReadOnlyMode === true;
    if(Object.prototype.hasOwnProperty.call(next, 'currentUiModelMissingFieldsVisible')) currentUiModelMissingFieldsVisible = next.currentUiModelMissingFieldsVisible === true;
    if(Object.prototype.hasOwnProperty.call(next, 'currentXsdValidationState')) currentXsdValidationState = next.currentXsdValidationState;
  },
  callbacks: {
    syncMode, syncSelectedXmlSourceDisplay, showMessage, buildActiveFormUrl,
    updateFormNavigationLinks, clearFormDirty, updateCloseActiveXmlButton,
    applyLargeFileModeForActiveXml, updateFormRendererSwitch, persistUiState,
    setBusy, renderValidate, clearLargeXmlSourceIfNeeded, activateTab,
    autoStartXsdValidationAfterActiveXmlOpen, clearXsdValidationHighlights,
    clearResults, resetFormTab, closeFormXpathValidationPopup,
    closeFormXsdValidationDrawer, updateFormXpathDrawerTab, updateFormXsdDrawerTab
  }
});
xmlSessionUi.init();

globalThis.adminUi = createAdminUi({
  elements: {
    refreshAdminButton, adminApplicationName, adminVersion, adminUptime,
    adminJavaVersion, adminOsInfo, adminConfigDir, adminLogFile,
    adminRootLogLevel, saveAdminLoggingButton,
    adminLogViewer, validateConfigButton, adminConfigProperties,
    adminConfigDiagnostics, reloadConfigButton, githubSchemaDryRunButton,
    githubSchemaUpdateButton, githubSchemaTargetDir, githubSchemaOrganization,
    githubSchemaTokenState, githubSchemaDownloadMode, githubSchemaArchiveTemplate,
    githubSchemaRateLimitState, githubSchemaRateLimitRetries, githubSchemaUpdaterResult,
    adminSchemaCacheCount, adminCacheFiles, clearCacheButton, reloadCacheButton,
    xmlIndexFormSelect, xmlIndexVersionSelect, xmlIndexPartSelect,
    xmlIndexReloadButton, xmlIndexSaveButton, xmlIndexConfigPath, xmlIndexTree,
    xmlIndexSummary, xmlIndexFieldSearch, xmlIndexFieldSelect, xmlIndexAddFieldButton
  },
  showMessage,
  escapeHtml,
  isCurrentUserAdmin: securityUi.isCurrentUserAdmin,
  getCurrentSecurityUser: securityUi.getCurrentUser,
  fetchSchemaRegistryStatus: schemaRegistryUi.fetchStatus
});
adminUi.init();

/**
 * Kezeli vagy beköti a bind runtime events esemény- és inicializációs folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function bindRuntimeEvents(){
  document.querySelectorAll('.tab-button').forEach(btn => btn.addEventListener('click', () => { if(!btn.disabled) activateTab(btn.dataset.tab); }));
  document.querySelectorAll('input[name="xmlMode"]').forEach(radio => radio.addEventListener('change', syncMode));
  document.querySelectorAll('.xml-tab-button').forEach(btn => btn.addEventListener('click', () => activateXmlView(btn.dataset.xmlView)));
  inspectButton?.addEventListener('click', () => runAction('/api/inspect', 'Inspect'));
  validateButton?.addEventListener('click', () => runAction('/api/validate', 'Validate'));
  fillExampleButton?.addEventListener('click', () => {
    if(appConfig.schemaDir) schemaDirInput.value = appConfig.schemaDir;
    if(appConfig.generalXsdPath) generalXsdDirInput.value = appConfig.generalXsdPath;
    if(appConfig.defaultXmlPath) xmlPathInput.value = appConfig.defaultXmlPath;
    const pathModeRadio = document.querySelector('input[name="xmlMode"][value="path"]');
    if(pathModeRadio) pathModeRadio.checked = true;
    syncMode();
    syncSelectedXmlSourceDisplay();
  });
  applyXmlSourceButton?.addEventListener('click', applyXmlSourceChanges);
  saveXmlFileButton?.addEventListener('click', toggleXmlSaveMenu);
  xmlSaveMenu?.querySelectorAll('[data-xml-save-action]')?.forEach(button => button.addEventListener('click', () => {
    const action = button.dataset.xmlSaveAction;
    closeXmlSaveMenu();
    handleXmlSaveAction(action);
  }));
  m2mSubmitMenuButton?.addEventListener('click', toggleM2mSubmitMenu);
  m2mSubmitMenu?.querySelectorAll('[data-m2m-form-action]')?.forEach(button => button.addEventListener('click', () => {
    const action = button.dataset.m2mFormAction;
    closeM2mSubmitMenu();
    handleM2mFormAction(action);
  }));
  m2mAttachmentInput?.addEventListener('change', handleM2mAttachmentInputChange);
  document.addEventListener('click', event => {
    if(xmlSaveMenu && !xmlSaveMenu.hidden && !event.target.closest('#xmlSaveDropdown')) closeXmlSaveMenu();
    if(m2mSubmitMenu && !m2mSubmitMenu.hidden && !event.target.closest('#m2mSubmitDropdown')) closeM2mSubmitMenu();
  });
  document.addEventListener('keydown', event => {
    if(event.key === 'Escape'){
      closeXmlSaveMenu();
      closeM2mSubmitMenu();
    }
  });
  window.addEventListener('resize', () => { positionXmlSaveMenu(); positionM2mSubmitMenu(); });
  window.addEventListener('scroll', () => { positionXmlSaveMenu(); positionM2mSubmitMenu(); }, true);
  prettyPrintXmlButton?.addEventListener('click', prettyPrintCurrentXml);
  formXmlLoadButton?.addEventListener('click', () => formXmlLoadInput?.click());
  formXmlLoadInput?.addEventListener('change', () => {
    const file = formXmlLoadInput.files?.[0];
    if(file) loadXmlFileFromFormPanel(file);
  });
  xmlSourceEditor?.addEventListener('input', () => { xmlSourceDirtySinceLastApply = true; markFormDirty(); syncXmlSourceHighlight(); });
  xmlSourceEditor?.addEventListener('scroll', syncXmlSourceScroll);
  toggleFormPaneButton?.addEventListener('click', () => togglePane('form'));
  toggleXmlPaneButton?.addEventListener('click', () => togglePane('xml'));
  showBothPanesButton?.addEventListener('click', () => setPaneLayout('both'));
  showOnlyFormPaneButton?.addEventListener('click', () => setPaneLayout('form-only'));
  showOnlyXmlPaneButton?.addEventListener('click', () => setPaneLayout('xml-only'));
  xmlFileInput?.addEventListener('change', syncSelectedXmlSourceDisplay);
  xmlPathInput?.addEventListener('input', syncSelectedXmlSourceDisplay);
  uiModelFormRendererButton?.addEventListener('click', () => setFormRendererMode('uimodel'));
}

bindRuntimeEvents();
updateQuickSaveXmlButtonState();
if(formXPathValidateButton){
  ensureFormXpathValidationPopup();
  updateFormXpathDrawerTab('neutral', 'Ellenőrzés', 'Nincs ellenőrzés');
}
if(formXmlLoadButton){
  ensureFormXsdValidationDrawer();
  updateFormXsdDrawerTab('neutral', 'Ellenőrzés', 'Nincs XSD ellenőrzési eredmény.');
}
setupViewMenu();
formFieldNavigationUi.init();

syncMode();
restoreUiState();
installNavigationStatePersistence();
updateFormNavigationLinks();
activateInitialTabFromPage();
securityUi.updateHeader();
homeUi.checkHealth();
homeUi.loadConfig().then(() => xmlSessionUi.autoLoadFromQuery()).catch(async error => {
  console.error('Aktív Űrlapállomány automatikus betöltési hiba', error);
  const rawMessage = String(error?.message || '');
  let physicalFileName = '';
  let originalFileName = '';
  const pathMatch = rawMessage.match(/(?:^|[\\/])([^\\/]+\.xml)(?:$|\s)/i);
  if(pathMatch?.[1]) physicalFileName = pathMatch[1];
  try{
    const stored = JSON.parse(sessionStorage.getItem('navXsdToolActiveXmlFile') || 'null');
    originalFileName = String(stored?.file?.originalFileName || stored?.file?.fileName || '').trim();
    if(!physicalFileName){
      const storedPath = String(stored?.file?.filePath || '').trim();
      const storedPathMatch = storedPath.match(/(?:^|[\\/])([^\\/]+\.xml)$/i);
      if(storedPathMatch?.[1]) physicalFileName = storedPathMatch[1];
    }
  }catch(_ignored){}
  const missingPhysicalFile = /nem létezik/i.test(rawMessage) && /xml/i.test(rawMessage);
  if(missingPhysicalFile && typeof window.navInfo === 'function') {
    const names = [
      originalFileName ? `Eredeti fájlnév: ${originalFileName}` : '',
      physicalFileName ? `Fizikai fájlnév: ${physicalFileName}` : ''
    ].filter(Boolean).join('\n');
    await window.navInfo({
      title:'Az Űrlapállomány nem nyitható meg',
      eyebrow:'Megnyitási hiba',
      message:`A kért állomány nem nyitható meg, mert a fizikai XML fájl nem létezik.${names ? `\n\n${names}` : ''}`,
      cancelText:'Bezárás',
      variant:'error'
    });
    return;
  }
  showMessage(rawMessage || 'Az aktív Űrlapállomány betöltése sikertelen.', 'error');
});
if(document.getElementById('readmeSectionGrid')) homeUi.loadReadmeHome();
schemaRegistryUi.fetchStatus();
activateXmlView(activeXmlView || 'tree');
initSplitter();
setViewMode('table', { skipRender:true });
if(document.getElementById('adminApplicationName') || document.getElementById('xmlIndexFormSelect')) adminUi.loadAdminData();
syncSelectedXmlSourceDisplay();
updateCloseActiveXmlButton();

window.NavM2mRuntime = {
  elements: { menuButton: m2mSubmitMenuButton, menu: m2mSubmitMenu, dropdown: m2mSubmitDropdown, attachmentInput: m2mAttachmentInput },
  getState: () => ({
    currentXmlDocument, currentFormDefinition,
    submissionId: currentM2mFormSubmissionId,
    m2mSubmission: currentM2mFormSubmissionData,
    markedForSubmit: currentM2mFormMarkedForSubmit,
    attachments: currentM2mFormAttachments,
    interfaceType: currentM2mFormInterfaceType,
    addAndFetchAfterFileSelect: currentM2mAddAndFetchAfterFileSelect
  }),
  setState: next => {
    if(!next || typeof next !== 'object') return;
    if(Object.prototype.hasOwnProperty.call(next, 'submissionId')) currentM2mFormSubmissionId = next.submissionId;
    if(Object.prototype.hasOwnProperty.call(next, 'm2mSubmission')) currentM2mFormSubmissionData = next.m2mSubmission || null;
    if(Object.prototype.hasOwnProperty.call(next, 'markedForSubmit')) currentM2mFormMarkedForSubmit = next.markedForSubmit === true;
    if(Object.prototype.hasOwnProperty.call(next, 'attachments')) currentM2mFormAttachments = Array.isArray(next.attachments) ? next.attachments : [];
    if(Object.prototype.hasOwnProperty.call(next, 'interfaceType')) currentM2mFormInterfaceType = next.interfaceType || null;
    if(Object.prototype.hasOwnProperty.call(next, 'addAndFetchAfterFileSelect')) currentM2mAddAndFetchAfterFileSelect = next.addAndFetchAfterFileSelect === true;
  },
  serializeCurrentXml: currentSerializedXmlForSave,
  getActiveXmlDisplayFileName,
  getSelection: () => ({ selectedFieldId, selectedXmlPath }),
  selectXmlPath: path => {
    const canonicalPath = canonicalizeXmlPath(path || '');
    if(!canonicalPath) return;
    selectedXmlPath = canonicalPath;
    selectedFieldId = findFieldIdByXmlPath(canonicalPath);
    ensureActiveXmlViewRendered('tree', { force:true });
    highlightSelections();
    openAncestorsForSelectedField();
    scrollXmlToSelected();
  },
  bindCollapseToggles,
  updateToggleAllFormCollapseButton,
  renderXmlFromCurrentState,
  replaceCurrentXmlFromText,
  rebuildCurrentFormFromXmlText,
  quickSaveCurrentXmlFile,
  hasActiveEditableXmlForServerSave: () => !!currentActiveXmlFile?.id && !!currentActiveXmlFileSessionId && !currentXmlFileReadOnlyMode,
  setSuccessfulSubmissionReadOnly: () => {
    currentXmlFileReadOnlyMode = true;
    currentFormHasUnsavedChanges = false;
    document.body?.classList?.add('xml-file-readonly-mode');
    if(xmlSourceEditor) xmlSourceEditor.readOnly = true;
    updateQuickSaveXmlButtonState();
    updateCloseActiveXmlButton();
    updateFormNavigationLinks();
    if(currentFormDefinition && currentXmlDocument){
      currentFormData = buildFormDataFromDocument(currentFormDefinition, currentXmlDocument);
      renderForm(currentFormDefinition, currentFormData, currentSchemaBundle || null);
    }
    persistUiState();
  },
  isLargeXmlMode: () => currentActiveXmlFile?.largeFileMode === true,
  refreshFormFromCurrentXml: () => {
    if(!currentFormDefinition || !currentXmlDocument) return;
    const formScrollTop = formContainer?.scrollTop || 0;
    const documentScrollTop = document.scrollingElement?.scrollTop || 0;
    currentFormData = buildFormDataFromDocument(currentFormDefinition, currentXmlDocument);
    renderForm(currentFormDefinition, currentFormData, currentSchemaBundle || null);
    if(formContainer) formContainer.scrollTop = formScrollTop;
    if(document.scrollingElement) document.scrollingElement.scrollTop = documentScrollTop;
  },
  markFormDirty,
  persistUiState,
  showMessage,
  xml: { resolveNodeName, canonicalizeXmlPath, findNodeByPath, createNodeByPath, insertXmlElementInSchemaOrder }
};

window.NavFormRuntime = {
  quickSaveCurrentXmlFile,
  validateCurrentXml,
  startActiveFileXsdValidation,
  runCurrentFormXpathValidation,
  showMessage,
  closeXmlSaveMenu,
  updateQuickSaveXmlButtonState,
  updateToggleAllFormCollapseButton,
  toggleAllFormCollapsibles,
  setUiModelDetailsVisible,
  setUiModelMissingFieldsVisible,
  ensureFormXpathValidationPopup,
  openFormXpathValidationPopup,
  closeFormXpathValidationPopup,
  toggleFormXpathValidationPopup,
  updateFormXpathDrawerTab,
  ensureFormXsdValidationDrawer,
  openFormXsdValidationDrawer,
  closeFormXsdValidationDrawer,
  toggleFormXsdValidationDrawer,
  updateFormXsdDrawerTab,
  updateFormXsdValidationDrawerFromStoredResult,
  updateFormXsdValidationDrawerFromValidateResponse,
  requireActiveEditableXmlForServerSave,
  hasActiveEditableXmlForServerSave: () => !!currentActiveXmlFile?.id && !!currentActiveXmlFileSessionId && !currentXmlFileReadOnlyMode,
  setSuccessfulSubmissionReadOnly: () => {
    currentXmlFileReadOnlyMode = true;
    currentFormHasUnsavedChanges = false;
    document.body?.classList?.add('xml-file-readonly-mode');
    if(xmlSourceEditor) xmlSourceEditor.readOnly = true;
    updateQuickSaveXmlButtonState();
    updateCloseActiveXmlButton();
    updateFormNavigationLinks();
    if(currentFormDefinition && currentXmlDocument){
      currentFormData = buildFormDataFromDocument(currentFormDefinition, currentXmlDocument);
      renderForm(currentFormDefinition, currentFormData, currentSchemaBundle || null);
    }
    persistUiState();
  },
  isLargeXmlMode: () => currentActiveXmlFile?.largeFileMode === true,
  buildCurrentXmlSnapshot: options => buildCurrentXmlSnapshotForSave(options).xml,
  buildCurrentXmlSnapshotInfo: options => buildCurrentXmlSnapshotForSave(options),
  buildCurrentXmlSnapshotDetails: options => buildCurrentXmlSnapshotForSave(options),
  syncCurrentFormControlsToXmlDocument: options => syncCurrentFormControlsToXmlDocument(options),
  collectCurrentFormControlSnapshot: () => collectCurrentFormControlSnapshot().map(item => ({ fieldId: item.fieldId, xmlPath: item.xmlPath, value: item.value, empty: item.empty })),
  getLastXmlSnapshotInfo: () => lastXmlSnapshotInfo,
  markFormClean,
  markFormDirty,
  isFormDirty: () => !!currentFormHasUnsavedChanges,
  isReadOnly: () => !!currentXmlFileReadOnlyMode,
  isUiModelDetailsVisible: () => currentUiModelDetailsVisible === true,
  isUiModelMissingFieldsVisible: () => currentUiModelMissingFieldsVisible === true,
  setFormXpathValidationButtonDisabled: disabled => { if(formXPathValidateButton) formXPathValidateButton.disabled = Boolean(disabled); },
  focusFormFieldFromXpathError,
  showXPathCopySuccess,
  isFormXsdDrawerAvailable: () => !!(formXmlLoadButton || validateCurrentXmlButton),
  isFormXmlLoadAvailable: () => !!formXmlLoadButton,
  setCurrentXsdValidationState,
  applyCurrentXsdValidationHighlights,
  getActiveXmlFileId: () => currentActiveXmlFile?.id || null,
  getActiveXmlFileName: () => currentActiveXmlFile?.fileName || currentActiveXmlFile?.name || null,
  getActiveXmlFileSessionId: () => currentActiveXmlFileSessionId || null,
  getDocumentType: () => currentSchemaBundle?.documentType || currentFormDefinition?.formName || null,
  getDocumentVersion: () => currentSchemaBundle?.documentVersion || currentFormDefinition?.formVersion || null
};
