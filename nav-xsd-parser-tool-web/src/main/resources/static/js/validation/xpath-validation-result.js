/**
 * @module validation/xpath-validation-result
 *
 * A XSD/XPath validációs működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A validációs állapotot és a hibák mezőhöz navigálását a szerveroldali eredményekkel összhangban kezeli.
 */

import {
  buildXpathToastMessage,
  firstLineText,
  isTerminalValidationFailure,
  normalizeResultStatus,
  normalizeValidatorStatus
} from './validation-status-utils.js';
import {
  copyTextToClipboard,
  escapeAttr,
  escapeHtml,
  renderMessageCell,
  renderValidationSeverityCell,
  normalizeValidationSeverityLabel,
  renderXpathPathCell,
  toggleMessageCell,
  toggleXpathPathCell
} from './validation-result-renderer.js';
import { extractFieldIdFromText, extractFieldIdFromXpathPath, normalizeMaybeFieldId, isUsefulFieldId, resolveDisplayElementIdForXpathError } from './xpath-error-navigation.js';

let currentXpathErrors = [];
let xpathErrorFilterState = new Set();
let xpathErrorSortDirection = 'asc';


/**
 * A <code>installXpathValidationResultGlobal</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
export function installXpathValidationResultGlobal(){
  window.NavXpathValidationResultUi = {
    ensureXpathValidationDrawer,
    openXpathValidationDrawer,
    closeXpathValidationDrawer,
    toggleXpathValidationDrawer,
    updateXpathDrawerTab,
    showXpathLoading,
    showXpathError,
    setCurrentXpathValidationState,
    renderXpathValidationPopup,
    renderXpathBlockedByXsd,
    renderXpathValidationSummary,
    renderXpathValidationErrors,
    setXpathErrorSortDirection,
    buildXpathToastMessage,
    isTerminalValidationFailure
  };
}


/**
 * Szinkronizálja vagy frissíti a set xpath error sort direction által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 */
export function setXpathErrorSortDirection(value){
  xpathErrorSortDirection = value === 'desc' ? 'desc' : 'asc';
  updateXpathCodeSortHeader();
  renderCurrentXpathValidationErrors();
}

/**
 * Szinkronizálja vagy frissíti a set current xpath validation state által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @param {*} errors a függvény errors bemeneti értéke
 */
export function setCurrentXpathValidationState(status, errors){
  window.NavFormRuntime?.setCurrentFormXpathValidationState?.(status, errors);
}

/**
 * A <code>ensureXpathValidationDrawer</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
export function ensureXpathValidationDrawer(){
  if(window.NavFormRuntime?.ensureFormXpathValidationPopup){
    window.NavFormRuntime.ensureFormXpathValidationPopup();
    return;
  }
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

  tab.addEventListener('click', () => toggleXpathValidationDrawer());
  document.getElementById('formXpathValidationPopupClose')?.addEventListener('click', closeXpathValidationDrawer);
  document.getElementById('formXpathValidationPopupRefresh')?.addEventListener('click', () => window.NavModularActions?.runCurrentFormXpathValidation?.());
  document.getElementById('formXpathValidationCodeSort')?.addEventListener('click', () => {
    xpathErrorSortDirection = xpathErrorSortDirection === 'asc' ? 'desc' : 'asc';
    updateXpathCodeSortHeader();
    renderCurrentXpathValidationErrors();
  });
}

/**
 * A <code>openXpathValidationDrawer</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
export function openXpathValidationDrawer(){
  if(window.NavFormRuntime?.openFormXpathValidationPopup){
    window.NavFormRuntime.openFormXpathValidationPopup();
    return;
  }
  ensureXpathValidationDrawer();
  const drawer = document.getElementById('formXpathValidationPopup');
  const tab = document.getElementById('formXpathValidationDrawerTab');
  drawer?.classList.add('open');
  tab?.setAttribute('aria-expanded', 'true');
}

/**
 * Elrejti vagy lezárja a close xpath validation drawer felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
export function closeXpathValidationDrawer(){
  if(window.NavFormRuntime?.closeFormXpathValidationPopup){
    window.NavFormRuntime.closeFormXpathValidationPopup();
    return;
  }
  const drawer = document.getElementById('formXpathValidationPopup');
  const tab = document.getElementById('formXpathValidationDrawerTab');
  drawer?.classList.remove('open');
  tab?.setAttribute('aria-expanded', 'false');
}

/**
 * A <code>toggleXpathValidationDrawer</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
export function toggleXpathValidationDrawer(){
  ensureXpathValidationDrawer();
  const drawer = document.getElementById('formXpathValidationPopup');
  if(drawer?.classList.contains('open')) closeXpathValidationDrawer();
  else openXpathValidationDrawer();
}

/**
 * Szinkronizálja vagy frissíti a update xpath drawer tab által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} state a függvény state bemeneti értéke
 * @param {*} label a függvény label bemeneti értéke
 * @param {*} detail a függvény detail bemeneti értéke
 */
export function updateXpathDrawerTab(state, label, detail){
  ensureXpathValidationDrawer();
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
 * Megjeleníti vagy újrarendereli a show xpath loading állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
 */
export function showXpathLoading(message){
  openXpathValidationDrawer();
  updateXpathDrawerTab('running', 'Fut...', 'Az XPath ellenőrzés folyamatban van.');
  const messageBox = document.getElementById('formXpathValidationPopupMessage');
  if(messageBox){
    messageBox.textContent = message || 'Folyamatban...';
    messageBox.className = 'xpath-popup-message loading';
  }
  const refresh = document.getElementById('formXpathValidationPopupRefresh');
  if(refresh) refresh.disabled = true;
}

/**
 * Megjeleníti vagy újrarendereli a show xpath error állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
 */
export function showXpathError(message){
  openXpathValidationDrawer();
  updateXpathDrawerTab('error', 'Hiba', 'Az XPath ellenőrzés hibára futott.');
  const messageBox = document.getElementById('formXpathValidationPopupMessage');
  if(messageBox){
    messageBox.textContent = message || 'Hiba történt.';
    messageBox.className = 'xpath-popup-message error';
  }
  const refresh = document.getElementById('formXpathValidationPopupRefresh');
  if(refresh) refresh.disabled = false;
}


/**
 * Megjeleníti vagy újrarendereli a render xpath blocked by xsd állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @param {*} xsdErrorCount a függvény xsdErrorCount bemeneti értéke
 */
export function renderXpathBlockedByXsd(status, xsdErrorCount = 0){
  ensureXpathValidationDrawer();
  setCurrentXpathValidationState(status, []);
  updateXpathDrawerTab('warning', 'Nem futott le', `Az XPath ellenőrzés nem futott le · ${xsdErrorCount} XSD hiba`);
  const messageBox = document.getElementById('formXpathValidationPopupMessage');
  if(messageBox){
    messageBox.textContent = 'Az XPath ellenőrzés nem indult el, mert az aktuális XML XSD hibás. Az XSD hibák külön, az XSD drawerben jelennek meg.';
    messageBox.className = 'xpath-popup-message warning';
  }
  renderXpathValidationSummary({ ...status, errorCount: 0, resultAvailable: false }, []);
  const body = document.getElementById('formXpathValidationErrorsBody');
  if(body){
    body.innerHTML = '<tr><td colspan="6">Nincs XPath hibalista, mert az XPath szabályellenőrzés az XSD hibák miatt nem indult el.</td></tr>';
  }
  const refresh = document.getElementById('formXpathValidationPopupRefresh');
  if(refresh) refresh.disabled = false;
}

/**
 * Megjeleníti vagy újrarendereli a render xpath validation popup állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @param {*} errors a függvény errors bemeneti értéke
 */
export function renderXpathValidationPopup(status, errors = []){
  openXpathValidationDrawer();
  setCurrentXpathValidationState(status, errors);

  const count = Number(status?.errorCount ?? errors.length);
  const validatorStatus = normalizeValidatorStatus(status);
  const resultStatus = normalizeResultStatus(status);
  const aborted = validatorStatus === 'ABORTED' || validatorStatus === 'FAILED';
  const finished = validatorStatus === 'FINISHED';

  let state = 'ok';
  let label = 'OK';
  let detail = `OK · nincs hiba · ${validatorStatus || 'FINISHED'}`;
  let message = 'Az ellenőrzés nem talált hibát.';
  let messageClass = 'xpath-popup-message success';

  if(aborted){
    state = 'error';
    label = 'Hiba';
    detail = `ERROR · az XPath ellenőrzés megszakadt · ${validatorStatus}`;
    message = firstLineText(status?.technicalErrorMessage) || 'Az XPath ellenőrzés technikai hibával megszakadt.';
    messageClass = 'xpath-popup-message error';
  } else if(!finished){
    state = 'running';
    label = 'Fut...';
    detail = `Az XPath ellenőrzés még nem zárult le · ${validatorStatus || 'ISMERETLEN'}`;
    message = 'Az XPath ellenőrzés még folyamatban van.';
    messageClass = 'xpath-popup-message loading';
  } else if(resultStatus === 'ERROR' || count > 0){
    state = 'error';
    label = `${count} hiba`;
    detail = `ERROR · ${count} hiba · FINISHED`;
    message = count ? `Az ellenőrzés ${count} hibát talált.` : 'Az XPath ellenőrzés hibás eredménnyel zárult.';
    messageClass = 'xpath-popup-message error';
  }

  updateXpathDrawerTab(state, label, detail);
  const messageBox = document.getElementById('formXpathValidationPopupMessage');
  if(messageBox){
    messageBox.textContent = message;
    messageBox.className = messageClass;
  }
  renderXpathValidationSummary(status, errors);
  renderXpathValidationErrors(errors, status);
  const refresh = document.getElementById('formXpathValidationPopupRefresh');
  if(refresh) refresh.disabled = false;
}

/**
 * Megjeleníti vagy újrarendereli a render xpath validation summary állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @param {*} errors a függvény errors bemeneti értéke
 */
export function renderXpathValidationSummary(status, errors = []){
  const body = document.getElementById('formXpathValidationSummaryBody');
  if(!body) return;
  const requestId = status?.requestId || '-';
  const timestamp = status?.requestTimestampUtc || status?.createdAt || '-';
  const formText = `${status?.formName || ''} ${status?.formVersion || ''}`.trim() || '-';
  const validatorStatus = status?.validatorStatus || '-';
  const resultStatus = status?.resultStatus || '-';
  const errorCount = status?.errorCount ?? errors.length;
  const downloadUrl = status?.resultDownloadUrl || (status?.requestId ? `/api/xpath-validator/requests/${encodeURIComponent(status.requestId)}/result` : null);
  const download = downloadUrl && status?.resultAvailable !== false
    ? `<a class="xpath-download-link" href="${escapeAttr(downloadUrl)}" target="_blank" rel="noopener">Letöltés</a>`
    : '-';
  body.innerHTML = `<tr>
    <td>${escapeHtml(requestId)}</td>
    <td>${escapeHtml(timestamp)}</td>
    <td>${escapeHtml(formText)}</td>
    <td>${escapeHtml(validatorStatus)}</td>
    <td>${escapeHtml(resultStatus)}</td>
    <td>${escapeHtml(String(errorCount))}</td>
    <td>${download}</td>
  </tr>`;
}

/**
 * Feldolgozza a normalize xpath severity bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function normalizeXpathSeverity(value){
  const severity = normalizeValidationSeverityLabel(value);
  return severity.label && severity.label !== '-' ? severity.label : 'ISMERETLEN';
}

/**
 * Szinkronizálja vagy frissíti a update xpath code sort header által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function updateXpathCodeSortHeader(){
  const header = document.querySelector('.xpath-code-sort-header');
  const indicator = document.querySelector('.xpath-code-sort-indicator');
  const descending = xpathErrorSortDirection === 'desc';
  header?.setAttribute('aria-sort', descending ? 'descending' : 'ascending');
  if(indicator) indicator.textContent = descending ? '▼' : '▲';
}

/**
 * A <code>compareXpathErrorCodes</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} left a függvény left bemeneti értéke
 * @param {*} right a függvény right bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function compareXpathErrorCodes(left, right){
  const a = String(left?.errorCode || left?.code || '').trim();
  const b = String(right?.errorCode || right?.code || '').trim();
  const result = a.localeCompare(b, 'hu', { numeric: true, sensitivity: 'base' });
  return xpathErrorSortDirection === 'desc' ? -result : result;
}

/**
 * A <code>duplicateNavigationKey</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} item a függvény item bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function duplicateNavigationKey(item){
  const path = String(item?.path || '').replace(/\[\d+\]/g, '[]');
  const fieldId = extractFieldIdFromXpathPath(item?.path || '') || resolveXpathDisplayElementId(item);
  return `${path}|${fieldId}`;
}

/**
 * A <code>decorateXpathErrorsForNavigation</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} errors a függvény errors bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function decorateXpathErrorsForNavigation(errors){
  const counters = new Map();
  return errors.map((item, originalIndex) => {
    const key = duplicateNavigationKey(item);
    const duplicateOrdinal = (counters.get(key) || 0) + 1;
    counters.set(key, duplicateOrdinal);
    return { ...item, __navOriginalIndex: originalIndex, __navDuplicateOrdinal: duplicateOrdinal };
  });
}

/**
 * Megjeleníti vagy újrarendereli a render xpath severity filters állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} errors a függvény errors bemeneti értéke
 */
function renderXpathSeverityFilters(errors){
  const controls = document.getElementById('formXpathValidationErrorControls');
  const container = document.getElementById('formXpathValidationSeverityFilters');
  if(!controls || !container) return;
  if(!errors.length){
    controls.classList.add('hidden');
    container.innerHTML = '';
    return;
  }
  const counts = new Map();
  errors.forEach(item => {
    const severity = normalizeXpathSeverity(item?.severity);
    counts.set(severity, (counts.get(severity) || 0) + 1);
  });
  const severities = [...counts.keys()].sort((a, b) => a.localeCompare(b, 'hu'));
  if(!xpathErrorFilterState.size){
    severities.forEach(severity => xpathErrorFilterState.add(severity));
  } else {
    severities.forEach(severity => {
      if(!xpathErrorFilterState.has(severity) && !container.querySelector(`input[value="${CSS.escape(severity)}"]`)){
        xpathErrorFilterState.add(severity);
      }
    });
  }
  container.innerHTML = severities.map(severity => {
    const badge = renderValidationSeverityCell(severity);
    return `
      <label class="xpath-error-filter-option" title="${escapeAttr(severity)} hibák megjelenítése vagy elrejtése">
        <input type="checkbox" value="${escapeAttr(severity)}" ${xpathErrorFilterState.has(severity) ? 'checked' : ''}>
        ${badge}
        <span class="xpath-error-filter-count">${counts.get(severity)}</span>
      </label>`;
  }).join('');
  container.querySelectorAll('input[type="checkbox"]').forEach(input => {
    input.addEventListener('change', () => {
      if(input.checked) xpathErrorFilterState.add(input.value);
      else xpathErrorFilterState.delete(input.value);
      renderCurrentXpathValidationErrors();
    });
  });
  controls.classList.remove('hidden');
}

/**
 * Megjeleníti vagy újrarendereli a render current xpath validation errors állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 */
function renderCurrentXpathValidationErrors(status = null){
  updateXpathCodeSortHeader();
  const body = document.getElementById('formXpathValidationErrorsBody');
  if(!body) return;
  const filtered = currentXpathErrors
    .filter(item => xpathErrorFilterState.has(normalizeXpathSeverity(item?.severity)))
    .sort(compareXpathErrorCodes);
  if(!filtered.length){
    body.innerHTML = '<tr><td colspan="6">A kiválasztott hibatípusokhoz nincs megjeleníthető találat.</td></tr>';
    return;
  }
  body.innerHTML = filtered.map((item, index) => {
    const displayElementId = resolveXpathDisplayElementId(item);
    const elemCell = displayElementId
      ? `<button type="button" class="xpath-element-link" data-xpath-popup-error-index="${escapeAttr(index)}">${escapeHtml(displayElementId)}</button>`
      : '-';
    return `<tr>
      <td>${escapeHtml(item.errorCode || item.code || '-')}</td>
      <td>${renderMessageCell(item.errorMessage || item.message || '-', index)}</td>
      <td>${renderValidationSeverityCell(item.severity || '-')}</td>
      <td>${elemCell}</td>
      <td>${escapeHtml(item.ruleId || '-')}</td>
      <td>${renderXpathPathCell(item.path || '', index)}</td>
    </tr>`;
  }).join('');

  body.querySelectorAll('button[data-xpath-message-toggle-index]').forEach(button => {
    button.addEventListener('click', () => toggleMessageCell(button));
  });
  body.querySelectorAll('button[data-xpath-path-toggle-index]').forEach(button => {
    button.addEventListener('click', () => toggleXpathPathCell(button));
  });
  body.querySelectorAll('button[data-xpath-path-copy-index]').forEach(button => {
    button.addEventListener('click', () => {
      const item = filtered[Number(button.dataset.xpathPathCopyIndex)];
      copyTextToClipboard(item?.path || '', button);
    });
  });
  body.querySelectorAll('button[data-xpath-path-jump-index], button[data-xpath-popup-error-index]').forEach(button => {
    button.addEventListener('click', () => {
      const index = Number(button.dataset.xpathPathJumpIndex ?? button.dataset.xpathPopupErrorIndex);
      focusXpathError(filtered[index]);
    });
  });
}

/**
 * Megjeleníti vagy újrarendereli a render xpath validation errors állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} errors a függvény errors bemeneti értéke
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 */
export function renderXpathValidationErrors(errors = [], status = null){
  const body = document.getElementById('formXpathValidationErrorsBody');
  if(!body) return;
  const validatorStatus = normalizeValidatorStatus(status);
  currentXpathErrors = decorateXpathErrorsForNavigation(Array.isArray(errors) ? errors : []);
  xpathErrorFilterState = new Set(currentXpathErrors.map(item => normalizeXpathSeverity(item?.severity)));
  renderXpathSeverityFilters(currentXpathErrors);
  if(!currentXpathErrors.length){
    if(validatorStatus === 'ABORTED' || validatorStatus === 'FAILED'){
      body.innerHTML = '<tr><td colspan="6">Nincs feldolgozható XPath hibalista, mert az ellenőrzés technikai hibával megszakadt.</td></tr>';
    } else {
      body.innerHTML = '<tr><td colspan="6">Nincs hiba.</td></tr>';
    }
    return;
  }
  renderCurrentXpathValidationErrors(status);
}

/**
 * Feloldja a resolve xpath display element id eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} error a függvény error bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function resolveXpathDisplayElementId(error){
  const elementId = normalizeMaybeFieldId(error?.elementId || error?.elem || error?.fieldId || '');
  const pathFieldId = normalizeMaybeFieldId(extractFieldIdFromXpathPath(error?.path || ''));
  const messageFieldId = normalizeMaybeFieldId(extractFieldIdFromText(error?.errorMessage || error?.hibaszoveg || error?.message || ''));
  if(isUsefulFieldId(elementId)) return elementId;
  if(isUsefulFieldId(pathFieldId)) return pathFieldId;
  if(isUsefulFieldId(messageFieldId)) return messageFieldId;
  return '';
}

/**
 * A <code>focusXpathError</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} error a függvény error bemeneti értéke
 */
function focusXpathError(error){
  if(!error) return;
  const runtimeApi = window.NavFormRuntime;
  if(typeof runtimeApi?.focusFormFieldFromXpathError === 'function'){
    runtimeApi.focusFormFieldFromXpathError(error);
    return;
  }
  const targetInfo = window.NavModularActions?.resolveXpathErrorTarget?.(error);
  const target = targetInfo?.target;
  if(!target){
    const elementId = resolveDisplayElementIdForXpathError(error);
    window.NavFormRuntime?.showMessage?.(`Nem található űrlapmező ehhez az elemhez: ${elementId || error.path || ''}`, 'error');
    return;
  }
  target.scrollIntoView({ behavior: 'smooth', block: 'center' });
  const focusable = target.querySelector('input, select, textarea, button');
  focusable?.focus?.({ preventScroll: true });
}
