/**
 * @module runtime/application-shell
 *
 * A alkalmazási runtime- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

/**
 * Application shell, tabs, layout, result rendering and shared UI state helpers.
 * Shared runtime state is initialized by runtime-context.js.
 */

function updateCloseActiveXmlButton(){
  if(!closeActiveXmlButton) return;
  closeActiveXmlButton.disabled = !currentActiveXmlFile?.id;
  closeActiveXmlButton.classList.toggle('is-dirty', !!currentFormHasUnsavedChanges);
  closeActiveXmlButton.title = currentActiveXmlFile?.id ? (currentFormHasUnsavedChanges ? 'XML bezárása - mentetlen módosítások vannak' : 'XML bezárása') : 'Nincs aktív XML';
}

/**
 * A <code>markFormDirty</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function markFormDirty(){
  if(currentXmlFileReadOnlyMode) return;
  if(!currentActiveXmlFile?.id && !currentFormDefinition && !currentXmlDocument) return;
  currentFormHasUnsavedChanges = true;
  updateCloseActiveXmlButton();
  updateQuickSaveXmlButtonState();
}

/**
 * Eltávolítja vagy alaphelyzetbe állítja a clear form dirty művelethez tartozó kliensoldali állapotot.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function clearFormDirty(){
  currentFormHasUnsavedChanges = false;
  updateCloseActiveXmlButton();
  updateQuickSaveXmlButtonState();
}

/**
 * A <code>markFormClean</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function markFormClean(){
  clearFormDirty();
}

/**
 * A <code>safeReplaceElementChildren</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} element a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} children a függvény children bemeneti értéke
 */
function safeReplaceElementChildren(element, ...children){
  if(!element) return;
  try{
    element.replaceChildren(...children);
  }catch(error){
    if(error?.name !== 'NotFoundError') throw error;
    try{
      Array.from(element.childNodes || []).forEach(child => {
        if(child.parentNode === element) element.removeChild(child);
      });
      if(children && children.length) element.append(...children);
    }catch(fallbackError){
      console.warn('Safe DOM replace failed', fallbackError);
    }
  }
}

/**
 * Ellenőrzi a should render xml live feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function shouldRenderXmlLive(){
  return currentViewMode === 'dual' || currentViewMode === 'xml-tree';
}

/**
 * A <code>scheduleXmlFromCurrentState</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} delay a függvény delay bemeneti értéke
 */
function scheduleXmlFromCurrentState(delay = 250){
  if(!currentXmlDocument) return;
  markXmlViewsDirty();
  if(!shouldRenderXmlLive()) return;
  if(xmlRenderDebounceTimeout) clearTimeout(xmlRenderDebounceTimeout);
  xmlRenderDebounceTimeout = setTimeout(() => {
    xmlRenderDebounceTimeout = null;
    ensureActiveXmlViewRendered();
  }, delay);
}

/**
 * A <code>rememberFormFocusTarget</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} element a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 */
function rememberFormFocusTarget(element){
  if(!element) return;
  const target = element.closest?.('input[data-field-id], select[data-field-id], textarea, [contenteditable="true"]') || element;
  if(!target) return;
  const isFormElement = !!target.closest?.('#formContainer, #xmlSourcePanel, #xmlSourceEditor');
  if(!isFormElement && target !== xmlSourceEditor) return;
  lastFormFocusTarget = target;
  const fieldId = target.dataset?.fieldId || target.closest?.('.form-field[data-field-id]')?.dataset?.fieldId;
  if(fieldId){
    lastFormFocusSelector = `[data-field-id="${cssEscape(fieldId)}"]`;
  } else if(target.id){
    lastFormFocusSelector = `#${cssEscape(target.id)}`;
  } else {
    lastFormFocusSelector = null;
  }
}

/**
 * A <code>restoreFormFocusTarget</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function restoreFormFocusTarget(){
  const target = (lastFormFocusTarget && lastFormFocusTarget.isConnected)
    ? lastFormFocusTarget
    : (lastFormFocusSelector ? document.querySelector(lastFormFocusSelector) : null);
  if(!target || typeof target.focus !== 'function') return;
  try{
    target.focus({ preventScroll: true });
    if(typeof target.setSelectionRange === 'function'){
      const len = String(target.value || '').length;
      target.setSelectionRange(len, len);
    }
  }catch(e){
    try{ target.focus(); }catch(ignore){}
  }
}

/**
 * A <code>captureFormViewportState</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function captureFormViewportState(){
  return {
    activeElement: document.activeElement,
    formScrollTop: formScroll ? formScroll.scrollTop : null,
    xmlScrollTop: xmlContainer ? xmlContainer.scrollTop : null,
    sourceSelectionStart: xmlSourceEditor ? xmlSourceEditor.selectionStart : null,
    sourceSelectionEnd: xmlSourceEditor ? xmlSourceEditor.selectionEnd : null,
    sourceScrollTop: xmlSourceEditor ? xmlSourceEditor.scrollTop : null,
    paneState: { ...paneState }
  };
}

/**
 * A <code>restoreFormViewportState</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} state a függvény state bemeneti értéke
 */
function restoreFormViewportState(state){
  if(!state) return;
  paneState = { ...(state.paneState || paneState) };
  applyPaneState();
  if(formScroll && state.formScrollTop !== null) formScroll.scrollTop = state.formScrollTop;
  if(xmlContainer && state.xmlScrollTop !== null) xmlContainer.scrollTop = state.xmlScrollTop;
  if(xmlSourceEditor){
    if(state.sourceScrollTop !== null) xmlSourceEditor.scrollTop = state.sourceScrollTop;
    if(state.activeElement === xmlSourceEditor && state.sourceSelectionStart !== null && state.sourceSelectionEnd !== null){
      try{ xmlSourceEditor.setSelectionRange(state.sourceSelectionStart, state.sourceSelectionEnd); }catch(e){}
    }
  }
  restoreFormFocusTarget();
}

/**
 * Feldolgozza a build minimal ui state bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function buildMinimalUiState(){
  return {
    selectedXmlPath,
    appConfig,
    currentFormRenderer,
    currentUiModelDetailsVisible,
    currentUiModelMissingFieldsVisible,
    currentViewMode,
    activeXmlView,
    paneState: { ...paneState },
    schemaDir: schemaDirInput?.value || '',
    generalXsdDir: generalXsdDirInput?.value || '',
    xmlPath: xmlPathInput?.value || '',
    activeMultiformPart: currentMultiformState?.activePartName || null,
    activeMultiformPage: currentMultiformState?.page || 0,
    activeMultiformSelectedIndex: currentMultiformState?.selectedIndex || null,
    currentActiveXmlFile,
    currentActiveXmlFileSessionId,
    currentXmlFileReadOnlyMode,
    currentM2mFormSubmissionId,
    currentM2mFormMarkedForSubmit,
    currentM2mFormInterfaceType,
    currentM2mFormAttachments
  };
}

/**
 * A <code>persistUiState</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function persistUiState(){
  try{
    sessionStorage.setItem(UI_STATE_STORAGE_KEY, JSON.stringify(buildMinimalUiState()));
    try{ localStorage.removeItem(UI_STATE_STORAGE_KEY); }catch(_ignored){}
  }catch(error){
    console.warn('UI state persist skipped', error);
  }
}

/**
 * A <code>persistTransientFormState</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function persistTransientFormState(){
  if(currentActiveXmlFile?.id){
    try{ sessionStorage.removeItem(TRANSIENT_FORM_STATE_STORAGE_KEY); }catch(_ignored){}
    return true;
  }
  if(!currentFormDefinition || !currentXmlDocument) return false;
  try{
    const transientState = {
      currentFormDefinition,
      currentFormData,
      currentSchemaBundle,
      currentXml: serializeXml(currentXmlDocument)
    };
    const serialized = JSON.stringify(transientState);
    if(serialized.length > 3500000){
      console.warn('Transient form state is too large for session storage.');
      return false;
    }
    sessionStorage.setItem(TRANSIENT_FORM_STATE_STORAGE_KEY, serialized);
    return true;
  }catch(error){
    console.warn('Transient form state persist skipped', error);
    return false;
  }
}

/**
 * A <code>restoreTransientFormState</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function restoreTransientFormState(){
  if(document.body?.dataset?.initialTab !== 'formTab') return false;
  try{
    const raw = sessionStorage.getItem(TRANSIENT_FORM_STATE_STORAGE_KEY);
    if(!raw) return false;
    sessionStorage.removeItem(TRANSIENT_FORM_STATE_STORAGE_KEY);
    const state = JSON.parse(raw);
    if(!state.currentFormDefinition || !state.currentXml) return false;
    currentFormDefinition = state.currentFormDefinition;
    currentFormData = state.currentFormData || { valuesByFieldId:{} };
    currentSchemaBundle = state.currentSchemaBundle || null;
    currentXmlDocument = parseXmlString(state.currentXml);
    if(formTabButton) formTabButton.disabled = false;
    if(formContainer) renderForm(currentFormDefinition, currentFormData, currentSchemaBundle || null);
    renderXmlFromCurrentState({ force:true });
    return true;
  }catch(error){
    console.warn('Transient form state restore failed', error);
    try{ sessionStorage.removeItem(TRANSIENT_FORM_STATE_STORAGE_KEY); }catch(_ignored){}
    return false;
  }
}

/**
 * A <code>restoreUiState</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function restoreUiState(){
  try{
    const raw = sessionStorage.getItem(UI_STATE_STORAGE_KEY);
    if(raw){
      const state = JSON.parse(raw);
      if(state.appConfig) appConfig = state.appConfig;
      if(state.currentFormRenderer) currentFormRenderer = normalizeFormRendererMode(state.currentFormRenderer);
      currentUiModelDetailsVisible = state.currentUiModelDetailsVisible === true;
      currentUiModelMissingFieldsVisible = state.currentUiModelMissingFieldsVisible === true;
      currentViewMode = 'table';
      activeXmlView = state.activeXmlView === 'source' ? 'source' : 'tree';
      if(state.paneState) paneState = { ...paneState, ...state.paneState };
      if(uiModelDetailsToggle) uiModelDetailsToggle.setAttribute('aria-pressed', currentUiModelDetailsVisible ? 'true' : 'false');
      if(toggleEmptyUiModelFieldsButton) toggleEmptyUiModelFieldsButton.setAttribute('aria-pressed', currentUiModelMissingFieldsVisible ? 'true' : 'false');
      if(schemaDirInput && state.schemaDir) schemaDirInput.value = state.schemaDir;
      if(generalXsdDirInput && state.generalXsdDir) generalXsdDirInput.value = state.generalXsdDir;
      if(xmlPathInput && state.xmlPath) xmlPathInput.value = state.xmlPath;
      selectedXmlPath = state.selectedXmlPath || null;
      currentActiveXmlFile = state.currentActiveXmlFile || currentActiveXmlFile || null;
      currentActiveXmlFileSessionId = state.currentActiveXmlFileSessionId || currentActiveXmlFileSessionId || null;
      currentXmlFileReadOnlyMode = state.currentXmlFileReadOnlyMode === true;
      currentM2mFormSubmissionId = state.currentM2mFormSubmissionId || currentM2mFormSubmissionId || null;
      currentM2mFormMarkedForSubmit = state.currentM2mFormMarkedForSubmit === true;
      currentM2mFormInterfaceType = state.currentM2mFormInterfaceType || currentM2mFormInterfaceType || null;
      currentM2mFormAttachments = Array.isArray(state.currentM2mFormAttachments) ? state.currentM2mFormAttachments : (currentM2mFormAttachments || []);
      document.body?.classList?.toggle('xml-file-readonly-mode', currentXmlFileReadOnlyMode);
      updateQuickSaveXmlButtonState();
      updateM2mSubmitMenuState();
      updateCloseActiveXmlButton();
      updateFormNavigationLinks();
    }
    restoreTransientFormState();
  }catch(error){
    console.warn('UI state restore failed', error);
  }
}

/**
 * A <code>sanitizedFormUrl</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} candidate a célobjektum technikai azonosítója
 * @returns {*} a feldolgozás eredménye
 */
function sanitizedFormUrl(candidate){
  try{
    const resolved = new URL(candidate, window.location.origin);
    if(resolved.origin !== window.location.origin || resolved.pathname !== '/form.html') return '/form.html';
    const fileId = resolved.searchParams.get('xmlFileId');
    if(fileId && !/^\d+$/.test(fileId)) return '/form.html';
    const params = new URLSearchParams();
    if(fileId) params.set('xmlFileId', fileId);
    if(resolved.searchParams.get('readOnly') === 'true') params.set('readOnly', 'true');
    const query = params.toString();
    return query ? `/form.html?${query}` : '/form.html';
  }catch(_ignored){
    return '/form.html';
  }
}

/**
 * Feldolgozza a build active form url bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function buildActiveFormUrl(){
  try{
    const activeFileId = String(currentActiveXmlFile?.id ?? '');
    if(/^\d+$/.test(activeFileId)){
      const params = new URLSearchParams({ xmlFileId:activeFileId });
      if(currentXmlFileReadOnlyMode) params.set('readOnly', 'true');
      return `/form.html?${params.toString()}`;
    }
    const raw = sessionStorage.getItem('navXsdToolActiveXmlFile');
    if(raw){
      const opened = JSON.parse(raw);
      const fileId = String(opened?.file?.id ?? '');
      if(/^\d+$/.test(fileId)){
        const params = new URLSearchParams({ xmlFileId:fileId });
        if(opened.readOnly === true) params.set('readOnly', 'true');
        return `/form.html?${params.toString()}`;
      }
    }
  }catch(_ignored){}
  return '/form.html';
}

/**
 * Szinkronizálja vagy frissíti a update form navigation links által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function updateFormNavigationLinks(){
  const formUrl = sanitizedFormUrl(buildActiveFormUrl());
  try{ sessionStorage.setItem('navXsdToolLastFormUrl', formUrl); }catch(_ignored){}
  document.querySelectorAll('a[href="/form.html"], a#formTabButton').forEach(link => {
    if(link && link.getAttribute('href') !== formUrl){
      link.href = formUrl;
    }
  });
}

/**
 * A <code>installNavigationStatePersistence</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function installNavigationStatePersistence(){
  document.querySelectorAll('a.tab-button').forEach(link => {
    link.addEventListener('click', () => {
      persistUiState();
      updateFormNavigationLinks();
    });
  });
  window.addEventListener('pagehide', () => persistUiState());
  window.addEventListener('beforeunload', () => persistUiState());
}

/**
 * A <code>activateInitialTabFromPage</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function activateInitialTabFromPage(){
  const initialTab = document.body?.dataset?.initialTab;
  if(initialTab){
    activateTab(initialTab);
  }
}

/**
 * Ellenőrzi a is validate page feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function isValidatePage(){
  const path = window.location?.pathname || '';
  return path.endsWith('/validate.html') || path === '/validate.html' || path.endsWith('/validate');
}

/**
 * Szinkronizálja vagy frissíti a sync selected xml source display által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function syncSelectedXmlSourceDisplay(){
  if(selectedXmlInputPath){
    if(xmlFileInput?.files?.length){
      const browserPath = xmlFileInput.value?.trim();
      const fileName = xmlFileInput.files[0]?.name || '';
      selectedXmlInputPath.textContent = browserPath || fileName || 'Nincs kiválasztott XML fájl.';
      selectedXmlInputPath.title = browserPath || fileName || '';
    } else {
      selectedXmlInputPath.textContent = 'Nincs kiválasztott XML fájl.';
      selectedXmlInputPath.title = '';
    }
  }
  if(selectedXmlServerPath){
    const pathValue = xmlPathInput?.value?.trim();
    selectedXmlServerPath.textContent = pathValue || 'Nincs megadott XML elérési út.';
    selectedXmlServerPath.title = pathValue || '';
  }
}

/**
 * A <code>activateTab</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} tabId a célobjektum technikai azonosítója
 */
function activateTab(tabId){
  document.querySelectorAll('.tab-button').forEach(btn => btn.classList.toggle('active', btn.dataset.tab === tabId));
  document.querySelectorAll('.tab-panel').forEach(panel => panel.classList.toggle('active', panel.id === tabId));
}

/**
 * A <code>activateXmlView</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} view a függvény view bemeneti értéke
 */
function activateXmlView(view){
  activeXmlView = view === 'source' ? 'source' : 'tree';
  document.querySelectorAll('.xml-tab-button').forEach(btn => btn.classList.toggle('active', btn.dataset.xmlView === activeXmlView));
  const treePanel = document.getElementById('xmlTreePanel');
  const sourcePanel = document.getElementById('xmlSourcePanel');
  if(treePanel) treePanel.classList.toggle('active', activeXmlView === 'tree');
  if(sourcePanel) sourcePanel.classList.toggle('active', activeXmlView === 'source');
  ensureActiveXmlViewRendered(activeXmlView);
  persistUiState();
}

/**
 * A <code>currentMode</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function currentMode(){
  const selectedMode = document.querySelector('input[name="xmlMode"]:checked');
  return selectedMode ? selectedMode.value : 'upload';
}

/**
 * Szinkronizálja vagy frissíti a sync mode által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function syncMode(){
  if(!uploadPane || !pathPane) {
    return;
  }
  const mode = currentMode();
  uploadPane.classList.toggle('hidden', mode !== 'upload');
  pathPane.classList.toggle('hidden', mode !== 'path');
  syncSelectedXmlSourceDisplay();
}

/**
 * Szinkronizálja vagy frissíti a set busy által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} busy a függvény busy bemeneti értéke
 */
function setBusy(busy){
  if(inspectButton) inspectButton.disabled = busy;
  if(validateButton) validateButton.disabled = busy;
}

/**
 * Eltávolítja vagy alaphelyzetbe állítja a clear results művelethez tartozó kliensoldali állapotot.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function clearResults(){
  if(messages) messages.innerHTML = '';
  if(summaryGrid) summaryGrid.innerHTML = '';
  if(detailSections) detailSections.innerHTML = '';
}

/**
 * Eltávolítja vagy alaphelyzetbe állítja a reset form tab művelethez tartozó kliensoldali állapotot.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function resetFormTab(){
  resetXmlRenderState?.();
  formLazyRenderer?.reset();
  if(formTabButton) formTabButton.disabled = true;
  if(formContainer) formContainer.innerHTML = '';
  if(xmlContainer) xmlContainer.innerHTML = '';
  if(xmlSourceEditor) xmlSourceEditor.value = '';
  syncSelectedXmlSourceDisplay();
  if(xmlSourceHighlight) xmlSourceHighlight.innerHTML = '';
  if(formTitle) formTitle.textContent = 'Nincs generált űrlap';
  if(formViewHeading){
    formViewHeading.classList.remove('form-view-heading-rich');
    formViewHeading.textContent = 'Űrlap';
  }
  selectedFieldId = null;
  selectedXmlPath = null;
  formFieldNavigationUi?.resetSearch();
  currentFormDefinition = null;
  currentFormData = null;
  currentXmlDocument = null;
  currentSchemaBundle = null;
  currentM2mFormSubmissionId = null;
  currentM2mFormSubmissionData = null;
  currentM2mFormMarkedForSubmit = false;
  currentM2mFormAttachments = [];
  currentM2mFormInterfaceType = null;
  currentM2mAddAndFetchAfterFileSelect = false;
  if(m2mAttachmentInput) m2mAttachmentInput.value = '';
  updateM2mSubmitMenuState();
  updateCloseActiveXmlButton();
}

/**
 * Megjeleníti vagy újrarendereli a show message állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} text a függvény text bemeneti értéke
 * @param {*} type a függvény type bemeneti értéke
 */
function showMessage(text, type='info'){
  const messageText = String(text || '');
  if(messages){
    messages.innerHTML = messageText
      ? `<div class="message ${type}">${escapeHtml(messageText)}</div>`
      : '';
  }
}

/**
 * Előkészíti és elindítja a add summary item állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} label a függvény label bemeneti értéke
 * @param {*} value a feldolgozandó vagy beállítandó érték
 */
function addSummaryItem(label,value){
  if(!value) return;
  const item = document.createElement('dl');
  item.className = 'summary-item';
  item.innerHTML = `<dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value)}</dd>`;
  summaryGrid.appendChild(item);
}

/**
 * Előkészíti és elindítja a create kv card állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} title a függvény title bemeneti értéke
 * @param {*} rows a függvény rows bemeneti értéke
 * @param {*} listValues a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function createKvCard(title, rows, listValues) {
    const wrapper = document.createElement('section');
    wrapper.className = 'detail-card';

    const heading = document.createElement('h3');
    heading.textContent = title || '';
    wrapper.appendChild(heading);

    const table = document.createElement('table');
    table.className = 'kv-table';

    const filteredRows = (rows || []).filter(([, value]) => value && value !== 'null');

    filteredRows.forEach(([label, value]) => {
        const tr = document.createElement('tr');

        const labelCell = document.createElement('td');
        labelCell.textContent = label != null ? String(label) : '';

        const valueCell = document.createElement('td');
        valueCell.textContent = value != null ? String(value) : '';

        tr.appendChild(labelCell);
        tr.appendChild(valueCell);
        table.appendChild(tr);
    });

    wrapper.appendChild(table);

    if (Array.isArray(listValues) && listValues.length) {
        const subheading = document.createElement('h4');
        subheading.textContent = 'Kapcsolódó XSD-k';
        wrapper.appendChild(subheading);

        const list = document.createElement('ul');
        list.className = 'code-list';

        listValues.forEach(value => {
            const item = document.createElement('li');
            item.textContent = value != null ? String(value) : '';
            list.appendChild(item);
        });

        wrapper.appendChild(list);
    }

    return wrapper;
}

/**
 * Előkészíti és elindítja a create issues card állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} issues a függvény issues bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function createIssuesCard(issues) {
    const wrapper = document.createElement('section');
    wrapper.className = 'card';

    const title = document.createElement('h3');
    title.textContent = 'Validációs üzenetek';
    wrapper.appendChild(title);

    const list = document.createElement('ul');
    list.className = 'issue-list';

    (issues || []).forEach(issue => {
        const severity = normalizeSeverity(issue && issue.severity);

        const item = document.createElement('li');
        item.classList.add('issue-item', severity.toLowerCase());

        const severityElement = document.createElement('strong');
        severityElement.textContent = severity;
        item.appendChild(severityElement);

        item.appendChild(document.createTextNode(' · '));

        const codeText = document.createTextNode(issue && issue.code ? String(issue.code) : '');
        item.appendChild(codeText);

        item.appendChild(document.createElement('br'));

        const messageText = document.createTextNode(issue && issue.message ? String(issue.message) : '');
        item.appendChild(messageText);

        item.appendChild(document.createElement('br'));

        const pathElement = document.createElement('small');
        pathElement.textContent = issue && issue.path ? String(issue.path) : '';
        item.appendChild(pathElement);

        list.appendChild(item);
    });

    wrapper.appendChild(list);
    return wrapper;
}

/**
 * Feldolgozza a normalize severity bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function normalizeSeverity(value) {
    const severity = String(value || 'INFO').toUpperCase();

    if (severity === 'ERROR') {
        return 'ERROR';
    }
    if (severity === 'WARNING') {
        return 'WARNING';
    }
    if (severity === 'INFO') {
        return 'INFO';
    }

    return 'INFO';
}

/**
 * Megjeleníti vagy újrarendereli a render common állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} data a függvény data bemeneti értéke
 */
function renderCommon(data){
  addSummaryItem('XML root', data.xml?.rootElementName);
  addSummaryItem('Namespace', data.xml?.namespace);
  addSummaryItem('Dokumentumtípus', data.schemaBundle?.documentType);
  addSummaryItem('Elsődleges XSD', data.schemaBundle?.primaryXsd);
  addSummaryItem('Illesztés oka', data.schemaBundle?.matchReason);
  addSummaryItem('UI model XML', data.schemaBundle?.uiModelFile);
  addSummaryItem('XPath szabály XML', data.xpathRuleFile);

  detailSections.appendChild(createKvCard('XML probe', [
    ['Fájlnév', data.xml?.fileName],
    ['Root elem', data.xml?.rootElementName],
    ['Namespace', data.xml?.namespace],
    ['schemaLocation', data.xml?.schemaLocation],
    ['noNamespaceSchemaLocation', data.xml?.noNamespaceSchemaLocation]
  ]));

  detailSections.appendChild(createKvCard('Schema bundle', [
    ['Dokumentumtípus', data.schemaBundle?.documentType],
    ['Root elem', data.schemaBundle?.rootElementName],
    ['Target namespace', data.schemaBundle?.targetNamespace],
    ['Elsődleges XSD', data.schemaBundle?.primaryXsd],
    ['UI model XML', data.schemaBundle?.uiModelFile],
    ['XPath szabály XML', data.xpathRuleFile]
  ], data.schemaBundle?.xsdFiles));

  if(data.documentDefinition){
    detailSections.appendChild(createKvCard('Dokumentum definíció', [
      ['Azonosító', data.documentDefinition?.id],
      ['Név', data.documentDefinition?.name],
      ['Cím', data.documentDefinition?.title],
      ['Root elem', data.documentDefinition?.rootElementName],
      ['Target namespace', data.documentDefinition?.targetNamespace],
      ['Block darabszám', String(data.documentDefinition?.blockCount ?? 0)]
    ]));
  }
  if(data.issues){
    detailSections.appendChild(createIssuesCard(data.issues));
  }
}

/**
 * Megjeleníti vagy újrarendereli a render inspect állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} data a függvény data bemeneti értéke
 */
function renderInspect(data){
  resultMode.textContent = 'Felderítés';
  showMessage('Az XSD-felderítés sikeresen lefutott.', 'success');
  renderCommon(data);
}

/**
 * Megjeleníti vagy újrarendereli a render validate állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} data a függvény data bemeneti értéke
 * @param {*} options a művelet opcionális beállításai
 */
function renderValidate(data, options = {}){
  resultMode.textContent = 'Validálás';
  if(options.suppressStatusMessage !== true){
    showMessage(data.valid ? 'Az XML valid a kiválasztott XSD alapján.' : 'Az XML validáció hibát talált.', data.valid ? 'success' : 'error');
  }
  renderCommon(data);
  if(options.renderInlineXsdResult === true || !currentActiveXmlFile?.id){
    updateFormXsdValidationDrawerFromValidateResponse(data);
  }

  const rawXml = data.xmlView?.rawXml || '';
  const formRuntimeXml = rawXml || data.formRuntimePreviewXml || '';
  const partialPreview = data.partialPreview === true || data.largeFileMode === true;
  const canRenderFormForRepair = !!data.formDefinition && (!!rawXml || partialPreview);
  console.info('[large-xml-open] renderValidate', {
    valid:data.valid,
    largeFileMode:data.largeFileMode === true,
    partialPreview,
    hasFormDefinition:!!data.formDefinition,
    hasFormData:!!data.formData,
    hasSchemaBundle:!!data.schemaBundle,
    hasRawXml:!!rawXml,
    hasFormRuntimePreviewXml:!!data.formRuntimePreviewXml
  });
  if(canRenderFormForRepair){
    if(!options.preserveCurrentFormState){
      currentFormDefinition = data.formDefinition;
      currentFormData = data.formData || { valuesByFieldId: {} };
      currentXmlDocument = formRuntimeXml ? parseXmlString(formRuntimeXml) : null;
      if(currentXmlDocument?.documentElement && data.largeXmlRepeatingFormName){
        currentXmlDocument.documentElement.setAttribute('data-large-repeating-form-name', String(data.largeXmlRepeatingFormName));
        currentXmlDocument.documentElement.setAttribute('data-large-repeating-form-count', String(data.largeXmlRepeatingFormCount || 0));
      }
      currentSchemaBundle = data.schemaBundle || null;
      clearFormDirty();
      formTabButton.disabled = false;
      renderForm(currentFormDefinition, currentFormData, data.schemaBundle || null);
      if(rawXml && currentXmlDocument) renderXmlFromCurrentState();
      applyCurrentXsdValidationHighlights();
      persistUiState();
    } else {
      formTabButton.disabled = false;
      applyCurrentXsdValidationHighlights();
      persistUiState();
    }

    if(partialPreview){
      showMessage(data.largeFileMessage || 'Nagy XML részleges előnézet: a főlap űrlapja a teljes XML böngészőbe töltése nélkül jelent meg.', 'warning');
    } else if(!data.valid){
      showMessage('Az XML XSD hibás, de jól formált; az űrlap javításra megnyitható.', 'warning');
    }

    if (isValidatePage()) {
      persistTransientFormState();
      window.location.href = '/form.html';
      return;
    }

    if(!options.skipTabActivation){
      activateTab('formTab');
    }
  } else {
    if(!options.preserveExistingFormOnInvalid){
      resetFormTab();
      persistUiState();
    }
  }
}


/**
 * Feldolgozza a normalize form header name bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function normalizeFormHeaderName(value){
  const raw = String(value || '').trim();
  if(!raw) return 'Űrlap';
  return raw.replace(/^Doc_NAV_/i, '').replace(/^NAV_/i, '');
}

/**
 * Betölti vagy lekéri a get active xml display file name művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function getActiveXmlDisplayFileName(){
  return currentActiveXmlFile?.fileName || currentActiveXmlFile?.name || currentActiveXmlFile?.filename || '';
}

/**
 * Feldolgozza a build form header parts bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} formDefinition a függvény formDefinition bemeneti értéke
 * @param {*} schemaBundle a függvény schemaBundle bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function buildFormHeaderParts(formDefinition, schemaBundle){
  const rawName = schemaBundle?.formName || schemaBundle?.documentFormName || schemaBundle?.documentType || formDefinition?.formName || formDefinition?.name || formDefinition?.webName || formDefinition?.title || 'Űrlap';
  const name = normalizeFormHeaderName(rawName);
  const info = schemaBundle?.formInfo || formDefinition?.webInfo || formDefinition?.description || '';
  const type = schemaBundle?.formType || formDefinition?.type || formDefinition?.tipus || '';
  const version = schemaBundle?.formVersion || schemaBundle?.documentVersion || formDefinition?.version || '';
  const fileName = getActiveXmlDisplayFileName();
  return { name, info, type, version, fileName };
}

/**
 * Megjeleníti vagy újrarendereli a show full form heading állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} fullText a függvény fullText bemeneti értéke
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function showFullFormHeading(fullText){
  if(!fullText) return;
  if(window.navInfo){
    await window.navInfo({
      title: 'Űrlap teljes megnevezése',
      message: fullText,
      cancelText: 'Bezárás',
      variant: 'default',
      eyebrow: 'Információ'
    });
    return;
  }
  if(window.navConfirm){
    await window.navConfirm({
      title: 'Űrlap teljes megnevezése',
      message: fullText,
      confirmText: 'Bezárás',
      cancelText: '',
      variant: 'default'
    });
    return;
  }
  showMessage(fullText, 'info');
}

/**
 * Szinkronizálja vagy frissíti a update form header title által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} formDefinition a függvény formDefinition bemeneti értéke
 * @param {*} schemaBundle a függvény schemaBundle bemeneti értéke
 */
function updateFormHeaderTitle(formDefinition, schemaBundle){
  const parts = buildFormHeaderParts(formDefinition, schemaBundle);
  const fallbackText = `${parts.name}${parts.version ? ` · v${parts.version}` : ''}`;

  if(formTitle){
    formTitle.textContent = fallbackText;
  }

  if(!formViewHeading) return;

  const mainText = parts.info ? `${parts.name} · ${parts.info}` : parts.name;
  const metaHtml = [
    parts.type ? `<span><strong>Típus:</strong>&nbsp;${escapeHtml(parts.type)}</span>` : '',
    parts.version ? `<span><strong>Verzió:</strong> ${escapeHtml(parts.version)}</span>` : '',
    parts.fileName ? `<span><strong>Fájl:</strong> ${escapeHtml(parts.fileName)}</span>` : ''
  ].filter(Boolean).join('');

  formViewHeading.classList.add('form-view-heading-rich');
  formViewHeading.innerHTML = `
    <span class="form-heading-title-row">
      <span class="form-heading-primary" title="${escapeHtml(mainText)}">${escapeHtml(mainText)}</span>
      ${parts.info ? `<button type="button" class="form-heading-more" id="formHeadingMoreButton" aria-label="Teljes űrlap megnevezés megjelenítése" title="Teljes megnevezés">…</button>` : ''}
    </span>
    <span class="form-heading-meta">${metaHtml}</span>`;

  const moreButton = document.getElementById('formHeadingMoreButton');
  if(moreButton){
    moreButton.addEventListener('click', () => showFullFormHeading(mainText));
  }
}

/**
 * Szinkronizálja vagy frissíti a set pane layout által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} mode a függvény mode bemeneti értéke
 */
function setPaneLayout(mode){
  paneState.formCollapsed = mode === 'xml-only';
  paneState.xmlCollapsed = mode === 'form-only';
  applyPaneState();
  if(!paneState.xmlCollapsed) ensureActiveXmlViewRendered();
}

/**
 * Feldolgozza a normalize view mode bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function normalizeViewMode(value){
  const mode = String(value || '').trim().toLowerCase();
  if(['xml-tree','table','dual'].includes(mode)) return mode;
  return 'table';
}

/**
 * Szinkronizálja vagy frissíti a set view mode által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} mode a függvény mode bemeneti értéke
 * @param {*} options a művelet opcionális beállításai
 */
function setViewMode(mode, options = {}){
  currentViewMode = normalizeViewMode(mode);
  const shouldRender = !options.skipRender;
  if(currentViewMode === 'xml-tree'){
    setPaneLayout('xml-only');
    if(shouldRender && currentXmlDocument) renderXmlFromCurrentState();
  } else if(currentViewMode === 'table'){
    setFormRendererMode('uimodel', { skipRender:true });
    setPaneLayout('form-only');
    if(shouldRender && currentFormDefinition && currentFormData) renderForm(currentFormDefinition, currentFormData, currentSchemaBundle || null);
  } else if(currentViewMode === 'dual'){
    setPaneLayout('both');
    if(shouldRender && currentXmlDocument) renderXmlFromCurrentState();
  }
  updateViewMenuControls();
  persistUiState();
}

/**
 * Szinkronizálja vagy frissíti a setup view menu által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
 */
function setupViewMenu(){
  if(!viewMenuButton || !viewMenu) return;
  viewMenuButton.addEventListener('click', (event) => {
    event.stopPropagation();
    const isOpen = !viewMenu.hidden;
    viewMenu.hidden = isOpen;
    viewMenuButton.setAttribute('aria-expanded', isOpen ? 'false' : 'true');
  });
  viewMenu.querySelectorAll('input[name="formViewMode"]').forEach(input => {
    input.addEventListener('change', () => {
      if(input.checked){
        setViewMode(input.value);
        viewMenu.hidden = true;
        viewMenuButton.setAttribute('aria-expanded', 'false');
      }
    });
  });
  document.addEventListener('click', (event) => {
    if(viewMenu.hidden) return;
    if(event.target === viewMenuButton || viewMenuButton.contains(event.target) || viewMenu.contains(event.target)) return;
    viewMenu.hidden = true;
    viewMenuButton.setAttribute('aria-expanded', 'false');
  });
  updateViewMenuControls();
}

/**
 * Szinkronizálja vagy frissíti a update view menu controls által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
 */
function updateViewMenuControls(){
  const labels = {
    'xml-tree': 'XML Fa nézet',
    table: 'Táblázat nézet',
    dual: 'Duális nézet'
  };
  if(viewMenuCurrentLabel) viewMenuCurrentLabel.textContent = labels[currentViewMode] || labels.table;
  viewMenu?.querySelectorAll('input[name="formViewMode"]').forEach(input => { input.checked = input.value === currentViewMode; });
}

/**
 * A <code>togglePane</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} pane a függvény pane bemeneti értéke
 */
function togglePane(pane){
  if(pane === 'form'){
    paneState.formCollapsed = !paneState.formCollapsed;
    if(paneState.formCollapsed) paneState.xmlCollapsed = false;
  } else {
    paneState.xmlCollapsed = !paneState.xmlCollapsed;
    if(paneState.xmlCollapsed) paneState.formCollapsed = false;
  }
  applyPaneState();
  if(!paneState.xmlCollapsed) ensureActiveXmlViewRendered();
}

/**
 * Szinkronizálja vagy frissíti a apply pane state által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function applyPaneState(){
  if(!splitContainer || !formPanel || !xmlPanel || !splitter) return;
  document.body.classList.toggle('form-only-layout', paneState.xmlCollapsed && !paneState.formCollapsed);
  document.body.classList.toggle('xml-only-layout', paneState.formCollapsed && !paneState.xmlCollapsed);
  splitContainer.classList.toggle('form-collapsed', paneState.formCollapsed);
  splitContainer.classList.toggle('xml-collapsed', paneState.xmlCollapsed);
  formPanel.hidden = paneState.formCollapsed;
  xmlPanel.hidden = paneState.xmlCollapsed;
  splitter.hidden = paneState.formCollapsed || paneState.xmlCollapsed;
  if(toggleFormPaneButton){
    toggleFormPaneButton.textContent = paneState.formCollapsed ? 'Űrlap nézet megjelenítése' : 'Űrlap nézet elrejtése';
  }
  if(toggleXmlPaneButton){
    toggleXmlPaneButton.textContent = paneState.xmlCollapsed ? 'XML nézet megjelenítése' : 'XML nézet elrejtése';
  }
  showBothPanesButton?.classList.toggle('active', !paneState.formCollapsed && !paneState.xmlCollapsed);
  showOnlyFormPaneButton?.classList.toggle('active', paneState.xmlCollapsed);
  showOnlyXmlPaneButton?.classList.toggle('active', paneState.formCollapsed);
  if(showOnlyFormPaneButton) showOnlyFormPaneButton.textContent = paneState.xmlCollapsed ? 'XML nézet megjelenítése' : 'XML nézet elrejtése';
  if(showOnlyXmlPaneButton) showOnlyXmlPaneButton.textContent = paneState.formCollapsed ? 'Űrlap nézet megjelenítése' : 'Űrlap nézet elrejtése';
  updateViewMenuControls();
  if(paneState.formCollapsed){
    formPanel.style.flex = '0 0 0';
    xmlPanel.style.flex = '1 1 100%';
  } else if(paneState.xmlCollapsed){
    formPanel.style.flex = '1 1 100%';
    xmlPanel.style.flex = '0 0 0';
  } else if(window.innerWidth > 900) {
    formPanel.style.flex = `0 0 ${paneState.formWidthPercent}%`;
    xmlPanel.style.flex = '1 1 auto';
  } else {
    formPanel.style.flex = '';
    xmlPanel.style.flex = '';
  }
}

/**
 * Feldolgozza a normalize form renderer mode bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function normalizeFormRendererMode(){
  return 'uimodel';
}

/**
 * Szinkronizálja vagy frissíti a set form renderer mode által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} mode a függvény mode bemeneti értéke
 * @param {*} options a művelet opcionális beállításai
 */
function setFormRendererMode(mode, options = {}){
  currentFormRenderer = normalizeFormRendererMode(mode);
  updateFormRendererSwitch();
  if(!options.skipRender && currentFormDefinition && currentFormData){
    renderForm(currentFormDefinition, currentFormData, currentSchemaBundle || null);
    bindFieldClicks();
    bindFormValueSync();
    highlightSelections();
  }
  persistUiState();
}

/**
 * Szinkronizálja vagy frissíti a update form renderer switch által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function updateFormRendererSwitch(){
  uiModelFormRendererButton?.classList.toggle('active', currentFormRenderer === 'uimodel');
  uiModelFormRendererButton?.setAttribute('aria-pressed', currentFormRenderer === 'uimodel' ? 'true' : 'false');
  document.body.classList.toggle('uimodel-form-renderer-active', currentFormRenderer === 'uimodel');
  document.body.classList.toggle('uimodel-details-visible', currentUiModelDetailsVisible);
  document.body.classList.toggle('uimodel-show-missing-fields', currentUiModelMissingFieldsVisible);
  if(uiModelDetailsToggle){
    uiModelDetailsToggle.classList.toggle('active', currentUiModelDetailsVisible);
    uiModelDetailsToggle.setAttribute('aria-pressed', currentUiModelDetailsVisible ? 'true' : 'false');
  }
  if(toggleEmptyUiModelFieldsButton){
    const missingFieldsToggleDisabled = currentXmlFileReadOnlyMode || document.body.classList.contains('xml-file-readonly-mode');
    toggleEmptyUiModelFieldsButton.classList.toggle('active', currentUiModelMissingFieldsVisible);
    toggleEmptyUiModelFieldsButton.setAttribute('aria-pressed', currentUiModelMissingFieldsVisible ? 'true' : 'false');
    toggleEmptyUiModelFieldsButton.disabled = missingFieldsToggleDisabled;
    toggleEmptyUiModelFieldsButton.title = missingFieldsToggleDisabled
      ? 'Csak olvasás módban a nem létező mezők nem jeleníthetők meg.'
      : 'XML-ben nem szereplő mezők megjelenítése';
  }
  updateViewMenuControls();
}

/**
 * Szinkronizálja vagy frissíti a set ui model details visible által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} visible a függvény visible bemeneti értéke
 * @param {*} options a művelet opcionális beállításai
 */
function setUiModelDetailsVisible(visible, options = {}){
  currentUiModelDetailsVisible = visible === true;
  updateFormRendererSwitch();
  if(!options.skipRender && currentFormRenderer === 'uimodel' && currentFormDefinition && currentFormData){
    renderForm(currentFormDefinition, currentFormData, currentSchemaBundle || null);
    bindFieldClicks();
    bindFormValueSync();
    highlightSelections();
  }
  persistUiState();
}

/**
 * Szinkronizálja vagy frissíti a set ui model missing fields visible által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} visible a függvény visible bemeneti értéke
 * @param {*} options a művelet opcionális beállításai
 */
function setUiModelMissingFieldsVisible(visible, options = {}){
  if(currentXmlFileReadOnlyMode || document.body.classList.contains('xml-file-readonly-mode')){
    visible = false;
  }
  currentUiModelMissingFieldsVisible = visible === true;
  updateFormRendererSwitch();
  if(!options.skipRender && currentFormRenderer === 'uimodel' && currentFormDefinition && currentFormData){
    renderForm(currentFormDefinition, currentFormData, currentSchemaBundle || null);
    bindFieldClicks();
    bindFormValueSync();
    highlightSelections();
  }
  persistUiState();
}

/**
 * Ellenőrzi a should render ui model field feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @param {*} valueObj a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function shouldRenderUiModelField(field, valueObj){
  if(!field || field.visible === false) return false;
  const type = String(field.type || '').toLowerCase();
  if(['link','subtitle','separator'].includes(type)) return true;
  if(currentUiModelMissingFieldsVisible) return true;
  if(valueObj && valueObj.present === true) return true;
  const value = valueObj?.value;
  return value !== undefined && value !== null && String(value) !== '';
}

Object.assign(globalThis, {
  updateCloseActiveXmlButton,
  markFormDirty,
  clearFormDirty,
  markFormClean,
  safeReplaceElementChildren,
  shouldRenderXmlLive,
  scheduleXmlFromCurrentState,
  rememberFormFocusTarget,
  restoreFormFocusTarget,
  captureFormViewportState,
  restoreFormViewportState,
  persistUiState,
  restoreUiState,
  buildActiveFormUrl,
  updateFormNavigationLinks,
  installNavigationStatePersistence,
  activateInitialTabFromPage,
  isValidatePage,
  syncSelectedXmlSourceDisplay,
  activateTab,
  activateXmlView,
  currentMode,
  syncMode,
  setBusy,
  clearResults,
  resetFormTab,
  showMessage,
  addSummaryItem,
  createKvCard,
  createIssuesCard,
  normalizeSeverity,
  renderCommon,
  renderInspect,
  renderValidate,
  normalizeFormHeaderName,
  getActiveXmlDisplayFileName,
  buildFormHeaderParts,
  showFullFormHeading,
  updateFormHeaderTitle,
  setPaneLayout,
  normalizeViewMode,
  setViewMode,
  setupViewMenu,
  updateViewMenuControls,
  togglePane,
  applyPaneState,
  normalizeFormRendererMode,
  setFormRendererMode,
  updateFormRendererSwitch,
  setUiModelDetailsVisible,
  setUiModelMissingFieldsVisible,
  shouldRenderUiModelField
});
