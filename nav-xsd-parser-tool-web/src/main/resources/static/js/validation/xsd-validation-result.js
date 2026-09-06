/**
 * @module validation/xsd-validation-result
 *
 * A XSD/XPath validációs működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A validációs állapotot és a hibák mezőhöz navigálását a szerveroldali eredményekkel összhangban kezeli.
 */

import { countXsdValidationErrors } from './validation-status-utils.js';
import {
  escapeHtml,
  renderMessageCell,
  renderValidationSeverityCell,
  renderXsdInfoCell,
  toggleMessageCell,
  toggleXsdInfoCell
} from './validation-result-renderer.js';

/**
 * A <code>installXsdValidationResultGlobal</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 */
export function installXsdValidationResultGlobal(){
  window.NavXsdValidationResultUi = {
    ensureXsdValidationDrawer,
    openXsdValidationDrawer,
    closeXsdValidationDrawer,
    toggleXsdValidationDrawer,
    updateXsdDrawerTab,
    renderStoredXsdValidationResult,
    renderInlineXsdValidationResult,
    renderXsdPrevalidationErrorsFromXpath,
    renderXsdStoredSummary,
    renderXsdInlineSummary,
    renderXsdErrors,
    dedupeXsdIssues,
    countXsdValidationErrors
  };
}

/**
 * A <code>ensureXsdValidationDrawer</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 */
export function ensureXsdValidationDrawer(){
  window.NavFormRuntime?.ensureFormXsdValidationDrawer?.();
}

/**
 * A <code>openXsdValidationDrawer</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 */
export function openXsdValidationDrawer(){
  window.NavFormRuntime?.openFormXsdValidationDrawer?.();
}

/**
 * Elrejti vagy lezárja a close xsd validation drawer felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 */
export function closeXsdValidationDrawer(){
  window.NavFormRuntime?.closeFormXsdValidationDrawer?.();
}

/**
 * A <code>toggleXsdValidationDrawer</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 */
export function toggleXsdValidationDrawer(){
  window.NavFormRuntime?.toggleFormXsdValidationDrawer?.();
}

/**
 * Szinkronizálja vagy frissíti a update xsd drawer tab által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
 * @param {*} state a függvény state bemeneti értéke
 * @param {*} label a függvény label bemeneti értéke
 * @param {*} detail a függvény detail bemeneti értéke
 */
export function updateXsdDrawerTab(state, label, detail){
  window.NavFormRuntime?.updateFormXsdDrawerTab?.(state, label, detail);
}

/**
 * Megjeleníti vagy újrarendereli a render stored xsd validation result állapotát a felhasználói felületen.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} data a függvény data bemeneti értéke
 * @param {*} options a művelet opcionális beállításai
 */
export function renderStoredXsdValidationResult(data, options = {}){
  if(!window.NavFormRuntime?.isFormXsdDrawerAvailable?.()) return;
  ensureXsdValidationDrawer();
  const request = data?.request || {};
  const errors = Array.isArray(data?.errors) ? data.errors : [];
  logXsdValidationDiagnostics(request, errors);
  const issues = normalizeStoredXsdErrors(request, errors);
  const resultStatus = String(request.resultStatus || request.status || '').toUpperCase();
  const errorCount = Math.max(Number(request.errorCount || 0), countXsdValidationErrors(issues));

  window.NavFormRuntime?.setCurrentXsdValidationState?.(request, issues);
  window.NavFormRuntime?.applyCurrentXsdValidationHighlights?.();

  const isValid = resultStatus === 'VALID';
  const isCancelled = resultStatus === 'CANCELLED';
  const isFailed = resultStatus === 'FAILED';

  if(isValid){
    updateXsdDrawerTab('ok', 'OK', 'OK · nincs XSD hiba');
  } else if(isCancelled){
    updateXsdDrawerTab('warning', 'Megszakítva', 'WARNING · XSD validáció megszakítva');
  } else if(isFailed){
    updateXsdDrawerTab('error', 'Hiba', request.technicalErrorMessage || 'ERROR · XSD validációs hiba');
  } else {
    updateXsdDrawerTab('error', errorCount ? `${errorCount} hiba` : 'Hiba', errorCount ? `ERROR · ${errorCount} XSD hiba` : 'ERROR · XSD validációs hiba');
  }

  renderXsdStoredSummary(request, issues);
  renderXsdErrors(issues);
  renderStoredXsdMessage(request, errorCount, { isValid, isCancelled, isFailed });

  const drawer = document.getElementById('formXsdValidationDrawer');
  if(options.openDrawerOnResult !== false || drawer?.classList.contains('open')){
    openXsdValidationDrawer();
  }
}

/**
 * Megjeleníti vagy újrarendereli a render inline xsd validation result állapotát a felhasználói felületen.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} data a függvény data bemeneti értéke
 */
export function renderInlineXsdValidationResult(data){
  if(!window.NavFormRuntime?.isFormXsdDrawerAvailable?.()) return;
  ensureXsdValidationDrawer();
  const issues = dedupeXsdIssues(Array.isArray(data?.issues) ? data.issues : []);
  const errorCount = countXsdValidationErrors(issues);
  window.NavFormRuntime?.setCurrentXsdValidationState?.({ resultStatus: data?.valid ? 'VALID' : 'INVALID', errorCount }, issues);
  window.NavFormRuntime?.applyCurrentXsdValidationHighlights?.();

  if(data?.valid){
    updateXsdDrawerTab('ok', 'OK', 'OK · nincs XSD hiba');
    renderXsdInlineSummary(data, issues);
    renderXsdErrors(issues);
    setXsdMessage('Az aktuális XML XSD validációja sikeres.', 'success');
    openXsdValidationDrawer();
    return;
  }

  updateXsdDrawerTab('error', errorCount ? `${errorCount} hiba` : 'Hiba', errorCount ? `ERROR · ${errorCount} XSD hiba` : 'ERROR · XSD validációs hiba');
  renderXsdInlineSummary(data, issues);
  renderXsdErrors(issues);
  setXsdMessage(errorCount
    ? `Az aktuális XML XSD validációja ${errorCount} hibát talált.`
    : 'Az aktuális XML XSD validációja hibás eredménnyel zárult.', 'error');
  openXsdValidationDrawer();
}


/**
 * Megjeleníti vagy újrarendereli a render xsd prevalidation errors from xpath állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @param {*} errors a függvény errors bemeneti értéke
 */
export function renderXsdPrevalidationErrorsFromXpath(status, errors = []){
  const issues = (Array.isArray(errors) ? errors : []).map(item => ({
    code: item?.errorCode || item?.code || 'XSD_ERROR',
    message: item?.errorMessage || item?.message || 'XSD validációs hiba.',
    severity: item?.severity || 'ERROR',
    path: item?.path || ''
  }));
  const technical = String(status?.technicalErrorMessage || '');
  const xsdMatch = technical.match(/XSD=([^\r\n]+)/);
  renderInlineXsdValidationResult({
    valid: false,
    issues,
    xml: { fileName: window.NavFormRuntime?.getActiveXmlFileName?.() || 'aktuális XML' },
    schemaBundle: {
      documentType: [status?.formName, status?.formVersion].filter(Boolean).join(' '),
      primaryXsd: xsdMatch ? xsdMatch[1].trim() : '-'
    }
  });
  setXsdMessage('Az XPath ellenőrzés nem indult el, mert az aktuális XML XSD hibás. Az alábbi hibákat az XSD ellenőrzés találta.', 'error');
}

/**
 * Feldolgozza a normalize stored xsd errors bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} request a backend-hívás kérésadata
 * @param {*} errors a függvény errors bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function normalizeStoredXsdErrors(request, errors){
  const issues = dedupeXsdIssues(errors.map(item => ({
    code: item.code || '-',
    message: item.message || '-',
    severity: item.severity || '-',
    path: [item.path, item.lineNumber ? `sor=${item.lineNumber}` : null, item.columnNumber ? `oszlop=${item.columnNumber}` : null].filter(Boolean).join(' · '),
    lineNumber: item.lineNumber || null,
    columnNumber: item.columnNumber || null
  })));
  const resultStatus = String(request.resultStatus || request.status || '').toUpperCase();
  if(resultStatus === 'FAILED' && !issues.length && request.technicalErrorMessage){
    issues.push({
      code: 'SCHEMA',
      message: request.technicalErrorMessage,
      severity: 'ERROR',
      path: 'XSD séma feloldás / validátor előkészítés'
    });
  }
  if(!['VALID','CANCELLED'].includes(resultStatus) && !issues.length){
    issues.push({
      code: resultStatus || 'STATUS',
      message: request.technicalErrorMessage || 'Az XSD validáció nem zárult le érvényes eredménnyel. Részletes XSD hiba nem keletkezett.',
      severity: resultStatus === 'FAILED' ? 'ERROR' : 'WARNING',
      path: 'XSD validáció státusz'
    });
  }
  return issues;
}


/**
 * A <code>xsdIssueInvalidValue</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} issue a függvény issue bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function xsdIssueInvalidValue(issue){
  const message = String(issue?.message || '');
  const match = message.match(/(?:Value|The value)\s+'([^']*)'/i);
  return match ? match[1] : '';
}

/**
 * A <code>xsdIssueLogicalTarget</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} issue a függvény issue bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function xsdIssueLogicalTarget(issue){
  const fieldId = resolveXsdDisplayFieldId(issue);
  if(fieldId) return fieldId;
  const path = String(issue?.path || '').replace(/\s*·\s*(?:sor|oszlop)=\d+/gi, '').trim();
  return path;
}

/**
 * Ellenőrzi a is generic xsd type echo feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} issue a függvény issue bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function isGenericXsdTypeEcho(issue){
  return /\bcvc-type\.3\.1\.3\b/i.test(String(issue?.message || ''));
}

/**
 * A <code>dedupeXsdIssues</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} issues a függvény issues bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function dedupeXsdIssues(issues = []){
  const source = Array.isArray(issues) ? issues : [];
  return source.filter((issue, index) => {
    if(!isGenericXsdTypeEcho(issue)) return true;
    const target = xsdIssueLogicalTarget(issue);
    const invalidValue = xsdIssueInvalidValue(issue);
    if(!target || !invalidValue) return true;
    return !source.some((other, otherIndex) => {
      if(otherIndex === index || isGenericXsdTypeEcho(other)) return false;
      if(String(other?.severity || '').toUpperCase() === 'INFO') return false;
      return xsdIssueLogicalTarget(other) === target && xsdIssueInvalidValue(other) === invalidValue;
    });
  });
}

/**
 * Megjeleníti vagy újrarendereli a render xsd stored summary állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} request a backend-hívás kérésadata
 * @param {*} issues a függvény issues bemeneti értéke
 */
export function renderXsdStoredSummary(request, issues){
  const body = document.getElementById('formXsdValidationSummaryBody');
  if(!body) return;
  const docType = [request.formType, request.formVersion].filter(Boolean).join(' ') || '-';
  const result = request.resultStatus || request.status || '-';
  body.innerHTML = `<tr>
    <td>${escapeHtml(request.xmlFileName || '-')}</td>
    <td>${escapeHtml(docType)}</td>
    <td>${escapeHtml(request.xsdPath || '-')}</td>
    <td>${escapeHtml(result)}</td>
    <td>${escapeHtml(String(Math.max(Number(request.errorCount || 0), countXsdValidationErrors(issues))))}</td>
  </tr>`;
}

/**
 * Megjeleníti vagy újrarendereli a render xsd inline summary állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} data a függvény data bemeneti értéke
 * @param {*} issues a függvény issues bemeneti értéke
 */
export function renderXsdInlineSummary(data, issues){
  const body = document.getElementById('formXsdValidationSummaryBody');
  if(!body) return;
  const fileName = data?.xml?.fileName || '-';
  const docType = data?.schemaBundle?.documentType || data?.xml?.rootElementName || '-';
  const primaryXsd = data?.schemaBundle?.primaryXsd || '-';
  const result = data?.valid ? 'OK' : 'ERROR';
  body.innerHTML = `<tr>
    <td>${escapeHtml(fileName)}</td>
    <td>${escapeHtml(docType)}</td>
    <td>${escapeHtml(primaryXsd)}</td>
    <td>${escapeHtml(result)}</td>
    <td>${escapeHtml(String(countXsdValidationErrors(issues)))}</td>
  </tr>`;
}

/**
 * Megjeleníti vagy újrarendereli a render xsd errors állapotát a felhasználói felületen.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} issues a függvény issues bemeneti értéke
 */
export function renderXsdErrors(issues = []){
  const body = document.getElementById('formXsdValidationErrorsBody');
  if(!body) return;
  if(!issues.length){
    body.innerHTML = '<tr><td colspan="5">Nincs XSD hiba.</td></tr>';
    return;
  }
  body.innerHTML = issues.map((item, index) => {
    const fieldId = resolveXsdDisplayFieldId(item);
    const fieldCell = fieldId
      ? `<button type="button" class="xpath-element-link" data-xsd-field-index="${index}">${escapeHtml(fieldId)}</button>`
      : '-';
    return `<tr>
      <td>${escapeHtml(item.code || '-')}</td>
      <td>${renderMessageCell(item.message || '-', `xsd-${index}`, { togglePrefix: 'xsd-message' })}</td>
      <td>${renderValidationSeverityCell(item.severity || '-')}</td>
      <td>${fieldCell}</td>
      <td>${renderXsdInfoCell(item.path || '', `xsd-info-${index}`)}</td>
    </tr>`;
  }).join('');
  body.querySelectorAll('button[data-xsd-message-toggle-index]').forEach(button => {
    button.addEventListener('click', () => toggleMessageCell(button));
  });
  body.querySelectorAll('button[data-xsd-info-toggle-index]').forEach(button => {
    button.addEventListener('click', () => toggleXsdInfoCell(button));
  });
  body.querySelectorAll('button[data-xsd-field-index]').forEach(button => {
    button.addEventListener('click', () => focusXsdIssue(issues[Number(button.dataset.xsdFieldIndex)]));
  });
}

/**
 * Feloldja a resolve xsd display field id eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} issue a függvény issue bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function resolveXsdDisplayFieldId(issue){
  const text = [issue?.path, issue?.message, issue?.code].filter(Boolean).join(' ');
  const match = text.match(/Field_[A-Za-z0-9_]+/);
  return match ? match[0] : '';
}

/**
 * A <code>focusXsdIssue</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} issue a függvény issue bemeneti értéke
 */
function focusXsdIssue(issue){
  const fieldId = resolveXsdDisplayFieldId(issue);
  const runtimeApi = window.NavFormRuntime;
  if(typeof runtimeApi?.focusFormFieldFromXpathError === 'function'){
    runtimeApi.focusFormFieldFromXpathError({
      path: issue?.path || '',
      elementId: fieldId,
      errorMessage: issue?.message || ''
    });
    return;
  }
  const escaped = globalThis.CSS?.escape ? CSS.escape(fieldId) : fieldId;
  const target = fieldId ? document.querySelector(`.form-field[data-field-id="${escaped}"], .uimodel-field[data-field-id="${escaped}"]`) : null;
  target?.scrollIntoView?.({ behavior: 'smooth', block: 'center' });
  target?.querySelector?.('input, select, textarea, button')?.focus?.({ preventScroll: true });
}

/**
 * Megjeleníti vagy újrarendereli a render stored xsd message állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} request a backend-hívás kérésadata
 * @param {*} errorCount a függvény errorCount bemeneti értéke
 * @param {*} flags a függvény flags bemeneti értéke
 */
function renderStoredXsdMessage(request, errorCount, flags){
  if(flags.isValid){
    setXsdMessage('Az aktív Űrlapállomány XSD validációja sikeres.', 'success');
  } else if(flags.isCancelled){
    setXsdMessage('Az XSD validáció felhasználói kérésre megszakadt.', 'warning');
  } else if(flags.isFailed){
    setXsdMessage(request.technicalErrorMessage || 'Az XSD validáció technikai hibával leállt.', 'error');
  } else {
    const limitText = request.maxErrorsReached ? ' A hibalimit elérése miatt a validáció megállt.' : '';
    if(errorCount){
      setXsdMessage(`Az aktív Űrlapállomány XSD validációja ${errorCount} hibát talált.${limitText}`, 'error');
    } else {
      setXsdMessage('Az XSD validáció nem zárult le érvényes eredménnyel. Részletes XSD hiba nem keletkezett.', 'warning');
    }
  }
}

/**
 * Szinkronizálja vagy frissíti a set xsd message által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
 * @param {*} type a függvény type bemeneti értéke
 */
function setXsdMessage(message, type){
  const messageBox = document.getElementById('formXsdValidationDrawerMessage');
  if(!messageBox) return;
  messageBox.textContent = message;
  messageBox.className = `xpath-popup-message ${type}`;
}


/**
 * A <code>logXsdValidationDiagnostics</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} request a backend-hívás kérésadata
 * @param {*} errors a függvény errors bemeneti értéke
 */
function logXsdValidationDiagnostics(request, errors){
  if(!Array.isArray(errors) || !errors.length) return;
  errors.forEach((item, index) => {
    const message = String(item?.message || '');
    const path = String(item?.path || '');
    const fieldMatch = `${path} ${message}`.match(/Field_[A-Za-z0-9_]+/);
    const fieldId = fieldMatch ? fieldMatch[0] : '';
    const controls = fieldId
      ? Array.from(document.querySelectorAll('[data-field-id]')).filter(control => {
          const value = String(control.dataset?.fieldId || '');
          return value === fieldId || value.endsWith(`:${fieldId}`) || value.includes(fieldId);
        })
      : [];
    const candidates = controls.map(control => {
      const wrapper = control.closest('[data-xml-path], .form-field, .uimodel-field');
      return {
        tagName: control.tagName,
        fieldId: control.dataset?.fieldId || '',
        value: control.value,
        xmlPath: wrapper?.dataset?.xmlPath || '',
        visible: !!(control.offsetWidth || control.offsetHeight || control.getClientRects().length)
      };
    });
    console.group(`XSD_VALIDATION_UI_BINDING #${index + 1} ${fieldId || item?.code || ''}`);
    console.log('request', {
      requestId: request?.requestId,
      xmlFileId: request?.xmlFileId,
      resultStatus: request?.resultStatus,
      activeInitialTab: document.body?.dataset?.initialTab || ''
    });
    console.log('rawError', item);
    console.log('resolved', {
      fieldId,
      validationPath: path,
      lineNumber: item?.lineNumber,
      columnNumber: item?.columnNumber,
      candidateFieldCount: candidates.length,
      candidates
    });
    console.groupEnd();
  });
}
