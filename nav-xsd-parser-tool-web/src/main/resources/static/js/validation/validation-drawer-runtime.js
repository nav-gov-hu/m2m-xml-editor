/**
 * @module validation/validation-drawer-runtime
 *
 * A XSD/XPath validációs működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A validációs állapotot és a hibák mezőhöz navigálását a szerveroldali eredményekkel összhangban kezeli.
 */

/**
 * XPath/XSD drawers, validation state highlighting and field focus integration.
 * Shared runtime state is initialized by runtime-context.js.
 */

async function runCurrentFormXpathValidation(){
  return runModularXpathValidation();
}

/**
 * A <code>ensureFormXpathValidationPopup</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function ensureFormXpathValidationPopup(){
  if(document.getElementById('formXpathValidationPopup')) return;

  const tab = document.createElement('button');
  tab.id = 'formXpathValidationDrawerTab';
  tab.type = 'button';
  tab.className = 'xpath-drawer-tab neutral';
  tab.setAttribute('aria-controls', 'formXpathValidationPopup');
  tab.setAttribute('aria-expanded', 'false');
  tab.innerHTML = '<span class="xpath-drawer-tab-label"><span class="xpath-drawer-tab-title">XPath</span><span class="xpath-drawer-tab-separator"> · </span><span class="xpath-drawer-tab-count">Ellenőrzés</span></span>';
  document.body.appendChild(tab);

  const drawer = document.createElement('section');
  drawer.id = 'formXpathValidationPopup';
  drawer.className = 'xpath-validation-drawer';
  drawer.setAttribute('aria-live', 'polite');
  drawer.setAttribute('aria-label', 'XPath ellenőrzés eredménye');
  drawer.innerHTML = `
    <div class="xpath-drawer-header">
      <div class="xpath-drawer-heading">
        <p class="eyebrow">Űrlapmegtekintő</p>
        <h2>XPath ellenőrzés eredménye</h2>
        <div id="formXpathValidationDrawerStatus" class="xpath-drawer-status">Nincs ellenőrzés</div>
      </div>
      <div id="formXpathValidationErrorControls" class="xpath-error-controls xpath-error-controls-header hidden" aria-label="XPath hibalista szűrése">
        <div id="formXpathValidationSeverityFilters" class="xpath-error-filter-options"></div>
      </div>
      <button type="button" class="secondary mini-button" id="formXpathValidationPopupClose" title="Bezárás" aria-label="Bezárás">×</button>
    </div>
    <div class="xpath-drawer-content">
      <div id="formXpathValidationPopupMessage" class="xpath-popup-message hidden"></div>
      <div class="table-scroll xpath-popup-summary-scroll">
        <table class="xpath-table xpath-popup-summary-table">
          <thead><tr><th>Request ID</th><th>Időbélyeg</th><th>Űrlap</th><th>Validator státusz</th><th>Eredmény</th><th>Hibák</th><th>Letöltés</th></tr></thead>
          <tbody id="formXpathValidationSummaryBody"><tr><td colspan="7">Nincs adat.</td></tr></tbody>
        </table>
      </div>
      <div class="table-scroll xpath-popup-errors-scroll">
        <table class="xpath-table xpath-popup-errors-table">
          <thead><tr><th class="xpath-code-sort-header" aria-sort="ascending"><button type="button" id="formXpathValidationCodeSort" class="xpath-code-sort-button" title="Rendezés hibakód szerint">Kód <span class="xpath-code-sort-indicator" aria-hidden="true">▲</span></button></th><th>Üzenet</th><th>Szint</th><th>Elem</th><th>Rule ID</th><th>Path</th></tr></thead>
          <tbody id="formXpathValidationErrorsBody"><tr><td colspan="6">Nincs adat.</td></tr></tbody>
        </table>
      </div>
    </div>
    <div class="xpath-drawer-footer">
      <button type="button" class="secondary" id="formXpathValidationPopupRefresh">Ellenőrzés</button>
    </div>`;
  document.body.appendChild(drawer);

  tab.addEventListener('click', () => toggleFormXpathValidationPopup());
  document.getElementById('formXpathValidationPopupClose')?.addEventListener('click', closeFormXpathValidationPopup);
  document.getElementById('formXpathValidationPopupRefresh')?.addEventListener('click', () => (window.NavModularActions?.runCurrentFormXpathValidation || runCurrentFormXpathValidation)());
  document.getElementById('formXpathValidationCodeSort')?.addEventListener('click', () => {
    const header = document.querySelector('.xpath-code-sort-header');
    const next = header?.getAttribute('aria-sort') === 'descending' ? 'asc' : 'desc';
    window.NavXpathValidationResultUi?.setXpathErrorSortDirection?.(next);
  });
}

/**
 * Szinkronizálja vagy frissíti a update validation drawer layout state által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 */
function updateValidationDrawerLayoutState(){
  const xpathDrawer = document.getElementById('formXpathValidationPopup');
  const xsdDrawer = document.getElementById('formXsdValidationDrawer');
  const openDrawer = [xpathDrawer, xsdDrawer].find(drawer => drawer?.classList.contains('open'));

  // A validációs panelek valódi overlay-k: nem módosítják a splitter,
  // az űrlap vagy az XML panel állapotát, és nem foglalnak helyet a layoutban.
  document.body.classList.toggle('validation-drawer-open', Boolean(openDrawer));
}

/**
 * A <code>openFormXpathValidationPopup</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function openFormXpathValidationPopup(){
  ensureFormXpathValidationPopup();
  closeFormXsdValidationDrawer();
  const drawer = document.getElementById('formXpathValidationPopup');
  const tab = document.getElementById('formXpathValidationDrawerTab');
  drawer?.classList.add('open');
  tab?.setAttribute('aria-expanded', 'true');
  updateValidationDrawerLayoutState();
}

/**
 * Elrejti vagy lezárja a close form xpath validation popup felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function closeFormXpathValidationPopup(){
  const drawer = document.getElementById('formXpathValidationPopup');
  const tab = document.getElementById('formXpathValidationDrawerTab');
  drawer?.classList.remove('open');
  tab?.setAttribute('aria-expanded', 'false');
  updateValidationDrawerLayoutState();
}

/**
 * A <code>toggleFormXpathValidationPopup</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function toggleFormXpathValidationPopup(){
  ensureFormXpathValidationPopup();
  const drawer = document.getElementById('formXpathValidationPopup');
  if(drawer?.classList.contains('open')) closeFormXpathValidationPopup();
  else openFormXpathValidationPopup();
}

/**
 * Szinkronizálja vagy frissíti a update form xpath drawer tab által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} state a függvény state bemeneti értéke
 * @param {*} label a függvény label bemeneti értéke
 * @param {*} detail a függvény detail bemeneti értéke
 */
function updateFormXpathDrawerTab(state, label, detail){
  ensureFormXpathValidationPopup();
  const tab = document.getElementById('formXpathValidationDrawerTab');
  const status = document.getElementById('formXpathValidationDrawerStatus');
  const normalizedState = state || 'neutral';
  if(tab){
    tab.className = `xpath-drawer-tab ${normalizedState}`;
    const title = tab.querySelector('.xpath-drawer-tab-title');
    const count = tab.querySelector('.xpath-drawer-tab-count');
    if(title) title.textContent = 'XPath';
    if(count) count.textContent = label || 'Ellenőrzés';
  }
  if(status){
    status.className = `xpath-drawer-status ${normalizedState}`;
    status.textContent = detail || label || 'Nincs ellenőrzés';
  }
}

/**
 * A <code>ensureFormXsdValidationDrawer</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function ensureFormXsdValidationDrawer(){
  if(document.getElementById('formXsdValidationDrawer')) return;

  const tab = document.createElement('button');
  tab.id = 'formXsdValidationDrawerTab';
  tab.type = 'button';
  tab.className = 'xpath-drawer-tab xsd-drawer-tab neutral';
  tab.setAttribute('aria-controls', 'formXsdValidationDrawer');
  tab.setAttribute('aria-expanded', 'false');
  tab.innerHTML = '<span class="xpath-drawer-tab-label"><span class="xpath-drawer-tab-title">XSD</span><span class="xpath-drawer-tab-separator"> · </span><span class="xpath-drawer-tab-count">Ellenőrzés</span></span>';
  document.body.appendChild(tab);

  const drawer = document.createElement('section');
  drawer.id = 'formXsdValidationDrawer';
  drawer.className = 'xpath-validation-drawer xsd-validation-drawer';
  drawer.setAttribute('aria-live', 'polite');
  drawer.setAttribute('aria-label', 'XSD validáció hibái');
  drawer.innerHTML = `
    <div class="xpath-drawer-header">
      <div>
        <p class="eyebrow">Űrlapmegtekintő</p>
        <h2>XSD validáció hibái</h2>
        <div id="formXsdValidationDrawerStatus" class="xpath-drawer-status">Nincs XSD ellenőrzési eredmény</div>
      </div>
      <button type="button" class="secondary mini-button" id="formXsdValidationDrawerClose" title="Bezárás" aria-label="Bezárás">×</button>
    </div>
    <div class="xpath-drawer-content">
      <div id="formXsdValidationDrawerMessage" class="xpath-popup-message hidden"></div>
      <div class="table-scroll xpath-popup-summary-scroll">
        <table class="xpath-table xpath-popup-summary-table">
          <thead><tr><th>XML fájl</th><th>Dokumentumtípus</th><th>Elsődleges XSD</th><th>Eredmény</th><th>Hibák</th></tr></thead>
          <tbody id="formXsdValidationSummaryBody"><tr><td colspan="5">Nincs adat.</td></tr></tbody>
        </table>
      </div>
      <div class="table-scroll xpath-popup-errors-scroll">
        <table class="xpath-table xpath-popup-errors-table">
          <thead><tr><th>Kód</th><th>Üzenet</th><th>Súlyosság</th><th>Érintett mező</th><th>Egyéb információ</th></tr></thead>
          <tbody id="formXsdValidationErrorsBody"><tr><td colspan="5">Nincs adat.</td></tr></tbody>
        </table>
      </div>
    </div>
    <div class="xpath-drawer-footer">
      <button type="button" class="secondary" id="formXsdValidationDrawerValidate">Ellenőrzés</button>
    </div>`;
  document.body.appendChild(drawer);

  tab.addEventListener('click', () => toggleFormXsdValidationDrawer());
  document.getElementById('formXsdValidationDrawerClose')?.addEventListener('click', closeFormXsdValidationDrawer);
  document.getElementById('formXsdValidationDrawerValidate')?.addEventListener('click', () => (window.NavModularActions?.runCurrentFormXsdValidation || validateCurrentXml)());
}

/**
 * A <code>openFormXsdValidationDrawer</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function openFormXsdValidationDrawer(){
  ensureFormXsdValidationDrawer();
  closeFormXpathValidationPopup();
  const drawer = document.getElementById('formXsdValidationDrawer');
  const tab = document.getElementById('formXsdValidationDrawerTab');
  drawer?.classList.add('open');
  tab?.setAttribute('aria-expanded', 'true');
  updateValidationDrawerLayoutState();
}

/**
 * Elrejti vagy lezárja a close form xsd validation drawer felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function closeFormXsdValidationDrawer(){
  const drawer = document.getElementById('formXsdValidationDrawer');
  const tab = document.getElementById('formXsdValidationDrawerTab');
  drawer?.classList.remove('open');
  tab?.setAttribute('aria-expanded', 'false');
  updateValidationDrawerLayoutState();
}

/**
 * A <code>toggleFormXsdValidationDrawer</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function toggleFormXsdValidationDrawer(){
  ensureFormXsdValidationDrawer();
  const drawer = document.getElementById('formXsdValidationDrawer');
  if(drawer?.classList.contains('open')) closeFormXsdValidationDrawer();
  else openFormXsdValidationDrawer();
}

/**
 * Szinkronizálja vagy frissíti a update form xsd drawer tab által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} state a függvény state bemeneti értéke
 * @param {*} label a függvény label bemeneti értéke
 * @param {*} detail a függvény detail bemeneti értéke
 */
function updateFormXsdDrawerTab(state, label, detail){
  ensureFormXsdValidationDrawer();
  const tab = document.getElementById('formXsdValidationDrawerTab');
  const status = document.getElementById('formXsdValidationDrawerStatus');
  const normalizedState = state || 'neutral';
  if(tab){
    tab.className = `xpath-drawer-tab xsd-drawer-tab ${normalizedState}`;
    const title = tab.querySelector('.xpath-drawer-tab-title');
    const count = tab.querySelector('.xpath-drawer-tab-count');
    if(title) title.textContent = 'XSD';
    if(count) count.textContent = label || 'Ellenőrzés';
  }
  if(status){
    status.className = `xpath-drawer-status ${normalizedState}`;
    status.textContent = detail || label || 'Nincs XSD ellenőrzési eredmény';
  }
}

const xsdEditedPathsSinceValidation = new Set();

/**
 * Szinkronizálja vagy frissíti a set current xsd validation state által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} request a backend-hívás kérésadata
 * @param {*} errors a függvény errors bemeneti értéke
 */
function setCurrentXsdValidationState(request, errors){
  xsdEditedPathsSinceValidation.clear();
  const status = String(request?.resultStatus || request?.status || '').toUpperCase();
  const list = Array.isArray(errors) ? errors : [];
  const errorCount = Math.max(Number(request?.errorCount || 0), countXsdValidationErrors(list));
  currentXsdValidationState = {
    status: status || 'UNKNOWN',
    invalid: status === 'INVALID' || errorCount > 0,
    errorCount,
    errors: list
  };
}

/**
 * Eltávolítja vagy alaphelyzetbe állítja a clear xsd validation highlights művelethez tartozó kliensoldali állapotot.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} scope a függvény scope bemeneti értéke
 */
function clearXsdValidationHighlights(scope = document){
  const root = scope?.querySelectorAll ? scope : document;
  root.querySelectorAll('.form-field.xsd-invalid-field, .uimodel-field.xsd-invalid-field').forEach(el => {
    el.classList.remove('xsd-invalid-field');
    el.removeAttribute('data-xsd-error-message');
    el.removeAttribute('title');
  });
}

/**
 * A <code>xsdIssueSearchText</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} issue a függvény issue bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function xsdIssueSearchText(issue){
  return [issue?.path, issue?.message, issue?.code].filter(Boolean).join(' ');
}

/**
 * Feldolgozza a extract quoted issue values bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} issue a függvény issue bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function extractQuotedIssueValues(issue){
  const text = xsdIssueSearchText(issue);
  const values = [];
  const patterns = [/['"]([^'"]{1,200})['"]/g, /value\s+([^.;]{1,200})/ig];
  patterns.forEach(pattern => {
    let match;
    while((match = pattern.exec(text))){
      const value = String(match[1] || '').trim();
      if(value) values.push(value);
    }
  });
  return values;
}

/**
 * Feldolgozza a extract issue xml path bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} issue a függvény issue bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function extractIssueXmlPath(issue){
  const raw = String(issue?.path || '').trim();
  if(!raw) return '';
  const match = raw.match(/(\/(?:[^\s·]+\/?)+)/);
  return match ? canonicalizeXmlPath(match[1].replace(/\/$/, '')) : '';
}

/**
 * Ellenőrzi a issue mentions field feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} issue a függvény issue bemeneti értéke
 * @param {*} fieldId a célobjektum technikai azonosítója
 * @param {*} xmlPath a feldolgozandó XML-tartalom vagy XML DOM-objektum
 * @param {*} fieldValue a feldolgozandó vagy beállítandó érték
 * @param {*} duplicateCount a függvény duplicateCount bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function issueMentionsField(issue, fieldId, xmlPath, fieldValue, duplicateCount){
  const haystack = xsdIssueSearchText(issue);
  if(!haystack) return false;
  const duplicate = (Number(duplicateCount) || 0) > 1;
  const issueCanonicalPath = extractIssueXmlPath(issue);
  if(xmlPath){
    const canonical = canonicalizeXmlPath(xmlPath);
    const exactPathMatch = !!canonical && !!issueCanonicalPath && (
      issueCanonicalPath === canonical
      || issueCanonicalPath.endsWith(canonical)
      || canonical.endsWith(issueCanonicalPath)
    );
    if(exactPathMatch) return true;

    // A concrete backend field path is authoritative. Never fall back to the
    // short fieldId when it points to a different form part or occurrence.
    if(issueCanonicalPath && canonical && issueCanonicalPath !== canonical) return false;

    if(!duplicate){
      if(canonical && haystack.includes(canonical)) return true;
      const noIndex = canonical.replace(/\[\d+\]/g, '');
      if(noIndex && haystack.includes(noIndex)) return true;
    }
  }
  if(duplicate || issueCanonicalPath){
    return false;
  }
  if(fieldId && haystack.includes(fieldId)){
    const value = String(fieldValue || '').trim();
    if(value){
      const issueValues = extractQuotedIssueValues(issue);
      if(issueValues.some(issueValue => issueValue === value || issueValue.includes(value) || value.includes(issueValue))) return true;
      if(haystack.includes(value)) return true;
    }
    return true;
  }
  return false;
}

/**
 * Szinkronizálja vagy frissíti a apply current xsd validation highlights által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} scope a függvény scope bemeneti értéke
 */
function applyCurrentXsdValidationHighlights(scope = document){
  const root = scope?.querySelectorAll ? scope : document;
  clearXsdValidationHighlights(root);
  const state = currentXsdValidationState || {};
  const errors = Array.isArray(state.errors) ? state.errors : [];
  if(!errors.length) return;
  const fieldIdCounts = {};
  document.querySelectorAll('.form-field[data-field-id], .uimodel-field[data-field-id]').forEach(field => {
    const fieldId = field.dataset.fieldId || '';
    if(fieldId) fieldIdCounts[fieldId] = (fieldIdCounts[fieldId] || 0) + 1;
  });
  root.querySelectorAll('.form-field[data-field-id], .uimodel-field[data-field-id]').forEach(field => {
    const inactivePanel = field.closest('.multiform-runtime-panel:not(.active)');
    if(inactivePanel) return;
    const fieldId = field.dataset.fieldId || '';
    const xmlPath = field.dataset.xmlPath || '';
    const control = field.querySelector('input, select, textarea');
    const fieldValue = control ? (control.type === 'checkbox' ? String(control.checked) : control.value) : '';
    const match = errors.find(issue => issueMentionsField(issue, fieldId, xmlPath, fieldValue, fieldIdCounts[fieldId] || 0));
    if(!match) return;
    const canonicalFieldPath = canonicalizeXmlPath(xmlPath || '');
    if(canonicalFieldPath && xsdEditedPathsSinceValidation.has(canonicalFieldPath)) return;
    field.classList.add('xsd-invalid-field');
  });
}


/**
 * Eltávolítja vagy alaphelyzetbe állítja a clear edited field xsd highlight művelethez tartozó kliensoldali állapotot.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} control a függvény control bemeneti értéke
 */
function clearEditedFieldXsdHighlight(control){
  const field = control?.closest?.('.form-field[data-field-id], .uimodel-field[data-field-id]');
  if(!field) return;
  const canonicalPath = canonicalizeXmlPath(field.dataset.xmlPath || '');
  if(canonicalPath) xsdEditedPathsSinceValidation.add(canonicalPath);
  field.classList.remove('xsd-invalid-field');
  field.removeAttribute('data-xsd-error-message');
  field.removeAttribute('title');
}

/**
 * A <code>confirmSaveWithXsdErrorsIfNeeded</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function confirmSaveWithXsdErrorsIfNeeded(){
  const state = currentXsdValidationState || {};
  if(!state.invalid) return true;
  const count = Number(state.errorCount || 0);
  if(window.navConfirm){
    return await window.navConfirm({
      title: 'XSD hibás XML mentése',
      message: `Az XML jelenleg XSD hibás${count ? ` (${count} hiba)` : ''}. Biztosan menteni akarod?`,
      confirmText: 'Mentés hibásként',
      cancelText: 'Mégsem',
      variant: 'warning'
    });
  }
  return window.confirm(`Az XML jelenleg XSD hibás${count ? ` (${count} hiba)` : ''}. Biztosan menteni akarod?`);
}

/**
 * Szinkronizálja vagy frissíti a update form xsd validation drawer from stored result által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} data a függvény data bemeneti értéke
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
function updateFormXsdValidationDrawerFromStoredResult(data, options = {}){
  return renderModularStoredXsdValidationResult(data, options);
}

/**
 * Szinkronizálja vagy frissíti a update form xsd validation drawer from validate response által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} data a függvény data bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function updateFormXsdValidationDrawerFromValidateResponse(data){
  return renderModularInlineXsdValidationResult(data);
}

/**
 * A <code>countXsdValidationErrors</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} issues a függvény issues bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function countXsdValidationErrors(issues){
  return countModularXsdValidationErrors(Array.isArray(issues) ? issues : []);
}

/**
 * Eltávolítja vagy alaphelyzetbe állítja a clear persistent validation jump highlight művelethez tartozó kliensoldali állapotot.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 */
function clearPersistentValidationJumpHighlight(){
  if(lastValidationJumpTarget && lastValidationJumpTarget.isConnected){
    lastValidationJumpTarget.classList.remove('xpath-jump-highlight', 'xpath-jump-persistent');
  }
  lastValidationJumpTarget = null;
}

/**
 * A <code>markPersistentValidationJumpTarget</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} target a függvény target bemeneti értéke
 */
function markPersistentValidationJumpTarget(target){
  if(!target) return;
  if(lastValidationJumpTarget && lastValidationJumpTarget !== target && lastValidationJumpTarget.isConnected){
    lastValidationJumpTarget.classList.remove('xpath-jump-highlight', 'xpath-jump-persistent');
  }
  lastValidationJumpTarget = target;
  target.classList.add('xpath-jump-highlight', 'xpath-jump-persistent');
}

/**
 * A <code>focusFormFieldFromXpathError</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} error a függvény error bemeneti értéke
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function focusFormFieldFromXpathError(error){
  const hint = error?.path || error?.xmlPath || error?.elem || error?.fieldId || '';
  if(typeof globalThis.ensureMultiformValidationTargetVisible === 'function'){
    try{
      await globalThis.ensureMultiformValidationTargetVisible(error?.path || error?.xmlPath || '');
    }catch(multiformNavigationError){
      console.warn('A validációs hibához tartozó melléklap automatikus megnyitása sikertelen.', multiformNavigationError);
    }
  }
  globalThis.formLazyRenderer?.ensureMatching(hint);
  let result = resolveModularXpathErrorTarget(error);
  if(!result?.target && globalThis.formLazyRenderer?.pendingCount?.()){
    globalThis.formLazyRenderer.ensureAll();
    result = resolveModularXpathErrorTarget(error);
  }

  if(!result?.target && !document.body.classList.contains('uimodel-show-missing-fields')
      && typeof globalThis.setUiModelMissingFieldsVisible === 'function'){
    showMessage('A keresett mező nem szerepel az XML-ben, ezért jelenleg rejtve van. Az „XML-ben nem szereplő mezők” megjelenítésre kerülnek.', 'warning');
    globalThis.setUiModelMissingFieldsVisible(true);
    await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
    globalThis.formLazyRenderer?.ensureMatching(hint);
    result = resolveModularXpathErrorTarget(error);
    if(!result?.target && globalThis.formLazyRenderer?.pendingCount?.()){
      globalThis.formLazyRenderer.ensureAll();
      result = resolveModularXpathErrorTarget(error);
    }
  }

  const target = result?.target || null;
  const elementId = result?.elementId || '';
  const path = result?.path || error?.path || '';
  if(!target){
    showMessage(`Nem található űrlapmező ehhez az elemhez: ${elementId || path}`, 'error');
    return;
  }

  selectedFieldId = target.dataset.fieldId || null;
  selectedXmlPath = target.dataset.xmlPath || path || null;
  openAncestorsForSelectedField(target);
  highlightSelections();
  scrollElementWithinContainer(formScroll, target, 22);

  const focusable = target.querySelector('input, select, textarea, button');
  if(focusable && typeof focusable.focus === 'function') focusable.focus({ preventScroll:true });
  markPersistentValidationJumpTarget(target);
}

/**
 * Szinkronizálja vagy frissíti a update browser scrollbar safe area által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function updateBrowserScrollbarSafeArea(){
  const scrollbarWidth = Math.max(0, window.innerWidth - document.documentElement.clientWidth);
  document.documentElement.style.setProperty('--browser-scrollbar-width', `${scrollbarWidth}px`);
}

/**
 * Szinkronizálja vagy frissíti a apply validation drawer side által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} side a célobjektum technikai azonosítója
 */
function applyValidationDrawerSide(side){
  const normalized = String(side || '').trim().toLowerCase() === 'left' ? 'left' : 'right';
  document.body.classList.toggle('validation-drawer-side-left', normalized === 'left');
  document.body.classList.toggle('validation-drawer-side-right', normalized === 'right');
  document.documentElement.dataset.validationDrawerSide = normalized;
  updateBrowserScrollbarSafeArea();
}

/**
 * Kezeli vagy beköti a initialize validation drawer configuration esemény- és inicializációs folyamatát.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function initializeValidationDrawerConfiguration(){
  applyValidationDrawerSide('right');
  try {
    const response = await fetch('/api/config', { cache: 'no-store', credentials: 'same-origin' });
    if(!response.ok) return;
    const config = await response.json();
    applyValidationDrawerSide(config?.validationDrawerSide);
  } catch (error) {
    console.warn('A validációs drawer pozíciója nem tölthető be, a jobb oldali alapérték marad.', error);
  }
}

window.addEventListener('resize', updateBrowserScrollbarSafeArea, { passive: true });
initializeValidationDrawerConfiguration();

Object.assign(globalThis, {
  runCurrentFormXpathValidation,
  ensureFormXpathValidationPopup,
  updateValidationDrawerLayoutState,
  openFormXpathValidationPopup,
  closeFormXpathValidationPopup,
  toggleFormXpathValidationPopup,
  updateFormXpathDrawerTab,
  ensureFormXsdValidationDrawer,
  openFormXsdValidationDrawer,
  closeFormXsdValidationDrawer,
  toggleFormXsdValidationDrawer,
  updateFormXsdDrawerTab,
  setCurrentXsdValidationState,
  clearXsdValidationHighlights,
  xsdIssueSearchText,
  extractQuotedIssueValues,
  issueMentionsField,
  applyCurrentXsdValidationHighlights,
  clearEditedFieldXsdHighlight,
  confirmSaveWithXsdErrorsIfNeeded,
  updateFormXsdValidationDrawerFromStoredResult,
  updateFormXsdValidationDrawerFromValidateResponse,
  countXsdValidationErrors,
  clearPersistentValidationJumpHighlight,
  markPersistentValidationJumpTarget,
  focusFormFieldFromXpathError,
  applyValidationDrawerSide,
  updateBrowserScrollbarSafeArea
});
