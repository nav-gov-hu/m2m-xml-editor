/**
 * @module m2m/m2m-submitter-page
 *
 * A M2M beküldési és csatolmánykezelési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A kliensoldali engedélyezés csak felhasználói visszajelzés; a tényleges M2M jogosultsági kontroll szerveroldali.
 */

import { m2mApi } from './m2m-api.js';
import { applySubmissionActionButtonState, isM2mTerminal } from './m2m-status-ui.js';
import { renderSubmissionEvents } from './m2m-submission-events-ui.js';

const state = {
  selectedId: null,
  rows: [],
  proxyLoaded: false,
  page: 1,
  pageSize: 10,
  busy: false,
  pollTimer: null
};

/**
 * A <code>byId</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} id a célobjektum technikai azonosítója
 */
const byId = id => document.getElementById(id);

/**
 * A <code>escapeHtml</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function escapeHtml(value){
  return String(value ?? '').replace(/[&<>\"]/g, char => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;'
  }[char]));
}

/**
 * Feldolgozza a normalized form url bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} candidate a célobjektum technikai azonosítója
 * @returns {*} a feldolgozás eredménye
 */
function normalizedFormUrl(candidate){
  try{
    const resolved = new URL(candidate, window.location.origin);
    if(resolved.origin !== window.location.origin || resolved.pathname !== '/form.html') return null;
    const fileId = resolved.searchParams.get('xmlFileId');
    if(fileId && !/^\d+$/.test(fileId)) return null;
    const readOnly = resolved.searchParams.get('readOnly');
    if(readOnly !== null && readOnly !== 'true') return null;
    const params = new URLSearchParams();
    if(fileId) params.set('xmlFileId', fileId);
    if(readOnly === 'true') params.set('readOnly', 'true');
    const query = params.toString();
    return query ? `/form.html?${query}` : '/form.html';
  }catch(_ignored){
    return null;
  }
}

/**
 * Feldolgozza a build return form url bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function buildReturnFormUrl(){
  try{
    const last = normalizedFormUrl(sessionStorage.getItem('navXsdToolLastFormUrl'));
    if(last) return last;
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
 * Szinkronizálja vagy frissíti a update form navigation link által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function updateFormNavigationLink(){
  const url = buildReturnFormUrl();
  document.querySelectorAll('a[href="/form.html"], a#formTabButton')
    .forEach(link => { link.href = url; });
}

/**
 * Megjeleníti vagy újrarendereli a show message állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
 * @param {*} type a függvény type bemeneti értéke
 */
function showMessage(message, type = 'info'){
  const target = byId('m2mMessages');
  if(target) target.innerHTML = `<div class="message ${type}">${escapeHtml(message)}</div>`;
}

/**
 * Feloldja a selected row eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function selectedRow(){
  return state.rows.find(row => String(row.id) === String(state.selectedId)) || null;
}

/**
 * A <code>actionElements</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function actionElements(){
  return {
    mark: byId('m2mMarkForSubmitButton'),
    withdraw: byId('m2mWithdrawSubmitMarkButton'),
    uploadAttachments: byId('m2mUploadAttachmentsButton'),
    createBizonylat: byId('m2mCreateBizonylatButton')
  };
}

/**
 * Szinkronizálja vagy frissíti a update action buttons által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function updateActionButtons(){
  const elements = actionElements();
  applySubmissionActionButtonState(selectedRow(), elements);
  if(state.busy) Object.values(elements).filter(Boolean).forEach(button => { button.disabled = true; });
}

/**
 * Szinkronizálja vagy frissíti a set busy által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} busy a függvény busy bemeneti értéke
 */
function setBusy(busy){
  state.busy = Boolean(busy);
  document.querySelectorAll('#m2mCreateForm button, #m2mRefreshButton, #m2mProxyTestButton, #m2mProxySettingsToggleButton, #m2mProxySettingsForm button, #m2mPrevPageButton, #m2mNextPageButton')
    .forEach(button => { button.disabled = state.busy; });
  const pages = totalPages(state.rows.length);
  if(byId('m2mPrevPageButton')) byId('m2mPrevPageButton').disabled = state.busy || state.page <= 1;
  if(byId('m2mNextPageButton')) byId('m2mNextPageButton').disabled = state.busy || state.page >= pages;
  updateActionButtons();
}

/**
 * Előkészíti és elindítja a created at millis állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} row a függvény row bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function createdAtMillis(row){
  const millis = Date.parse(row?.createdAt || '');
  return Number.isFinite(millis) ? millis : 0;
}

/**
 * A <code>sortedRows</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function sortedRows(){
  return [...state.rows].sort((left, right) => createdAtMillis(right) - createdAtMillis(left));
}

/**
 * A <code>totalPages</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} total a függvény total bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function totalPages(total){
  return Math.max(1, Math.ceil(total / state.pageSize));
}

/**
 * Megjeleníti vagy újrarendereli a render submissions table állapotát a felhasználói felületen.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 */
function renderSubmissionsTable(){
  const tbody = document.querySelector('#m2mSubmissionsTable tbody');
  if(!tbody) return;
  const rows = sortedRows();
  const total = rows.length;
  const pages = totalPages(total);
  state.page = Math.min(Math.max(state.page, 1), pages);
  const start = (state.page - 1) * state.pageSize;
  const pageRows = rows.slice(start, start + state.pageSize);
  tbody.innerHTML = pageRows.map(row => `
    <tr class="${String(row.id) === String(state.selectedId) ? 'selected' : ''}">
      <td>${escapeHtml(row.createdAt || '')}</td>
      <td>${escapeHtml(row.interfaceType || '')}</td>
      <td>${escapeHtml(row.xmlFileName || '')}</td>
      <td>${escapeHtml(row.navUgyAzonosito || row.navFileId || row.navErkeztetesiSzam || '')}</td>
      <td>${escapeHtml(row.internalStatus || row.navStatus || '')}</td>
      <td><button type="button" class="secondary mini-button" data-m2m-select="${escapeHtml(row.id)}">Megnyitás</button></td>
    </tr>`).join('') || '<tr><td colspan="6">Nincs beküldési csomag.</td></tr>';
  tbody.querySelectorAll('[data-m2m-select]').forEach(button => {
    button.addEventListener('click', () => selectSubmission(button.dataset.m2mSelect));
  });

  const summary = byId('m2mTableSummary');
  if(summary){
    summary.textContent = total === 0
      ? 'Nincs beküldési csomag.'
      : `${start + 1}-${Math.min(start + pageRows.length, total)} / ${total} csomag, rendezés: létrehozás szerint csökkenő`;
  }
  if(byId('m2mPageInfo')) byId('m2mPageInfo').textContent = `${state.page} / ${pages}`;
  if(byId('m2mPrevPageButton')) byId('m2mPrevPageButton').disabled = state.busy || state.page <= 1;
  if(byId('m2mNextPageButton')) byId('m2mNextPageButton').disabled = state.busy || state.page >= pages;
  updateActionButtons();
}

/**
 * Szinkronizálja vagy frissíti a refresh submissions által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function refreshSubmissions(){
  const rows = await m2mApi.listSubmissions();
  state.rows = Array.isArray(rows) ? rows : [];
  if(state.selectedId && !selectedRow()){
    state.selectedId = null;
    if(byId('m2mStatusPill')) byId('m2mStatusPill').textContent = 'Nincs kijelölt csomag';
    renderSubmissionEvents(byId('m2mEventLog'), []);
  }
  renderSubmissionsTable();
}

/**
 * Betölti vagy lekéri a load events művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function loadEvents(){
  if(!state.selectedId) return;
  const events = await m2mApi.getSubmissionEvents(state.selectedId);
  renderSubmissionEvents(byId('m2mEventLog'), events || []);
}

/**
 * Feloldja a select submission eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} id a célobjektum technikai azonosítója
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function selectSubmission(id){
  state.selectedId = id || null;
  if(byId('m2mStatusPill')){
    byId('m2mStatusPill').textContent = id ? `Kijelölt csomag: ${id}` : 'Nincs kijelölt csomag';
  }
  updateActionButtons();
  await Promise.all([loadEvents(), refreshSubmissions()]);
}

/**
 * Előkészíti és elindítja a create submission állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} event a feldolgozandó böngészőesemény
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function createSubmission(event){
  event.preventDefault();
  const formData = new FormData();
  const interfaceType = byId('m2mInterfaceType').value;
  formData.append('gatewayMode', byId('m2mGatewayMode').value);
  formData.append('compression', byId('m2mCompression').value);
  formData.append('submitNow', byId('m2mSubmitNow').value);
  const xml = byId('m2mXmlFile').files[0];
  const files = Array.from(byId('m2mAttachmentFiles').files || []);
  setBusy(true);
  try{
    let data;
    if(interfaceType === 'COMMON_FILESTORE'){
      files.forEach(file => formData.append('files', file, file.name));
      formData.append('uploadNow', byId('m2mSubmitNow').value);
      data = await m2mApi.createFilestoreSubmission(formData);
    }else{
      if(!xml) throw new Error('XML bizonylat kiválasztása szükséges.');
      formData.append('xml', xml, xml.name);
      files.forEach(file => formData.append('attachments', file, file.name));
      data = await m2mApi.createSubmission(formData);
    }
    showMessage('Beküldési csomag létrehozva.', 'success');
    await selectSubmission(data?.id);
  }catch(error){
    showMessage(error.message, 'error');
  }finally{
    setBusy(false);
  }
}

/**
 * Elindítja a run selected action aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} action a függvény action bemeneti értéke
 * @param {*} okMessage a megjelenítendő vagy feldolgozandó üzenet
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function runSelectedAction(action, okMessage){
  if(!state.selectedId) return;
  setBusy(true);
  try{
    if(action === 'mark') await m2mApi.markForSubmit(state.selectedId);
    else if(action === 'withdraw') await m2mApi.withdrawSubmitMark(state.selectedId);
    else await m2mApi.postSubmissionStep(state.selectedId, action);
    showMessage(okMessage, 'success');
    await Promise.all([loadEvents(), refreshSubmissions()]);
  }catch(error){
    showMessage(error.message, 'error');
  }finally{
    setBusy(false);
  }
}

/**
 * A <code>proxyFormToPayload</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function proxyFormToPayload(){
  const portValue = byId('m2mProxyPort')?.value?.trim();
  return {
    enabled: !!byId('m2mProxyEnabled')?.checked,
    proxyUrl: byId('m2mProxyUrl')?.value?.trim() || '',
    proxyPort: portValue ? Number(portValue) : null,
    username: byId('m2mProxyUsername')?.value?.trim() || '',
    password: byId('m2mProxyPassword')?.value || '',
    clearPassword: !!byId('m2mProxyClearPassword')?.checked,
    sslVerificationDisabled: false,
    trustStorePath: byId('m2mTrustStorePath')?.value?.trim() || '',
    trustStorePassword: byId('m2mTrustStorePassword')?.value || '',
    clearTrustStorePassword: !!byId('m2mTrustStoreClearPassword')?.checked,
    trustStoreType: byId('m2mTrustStoreType')?.value || 'JKS',
  };
}

/**
 * A <code>fillProxyForm</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} settings a függvény settings bemeneti értéke
 */
function fillProxyForm(settings){
  byId('m2mProxyEnabled').checked = !!settings.enabled;
  byId('m2mProxyUrl').value = settings.proxyUrl || '';
  byId('m2mProxyPort').value = settings.proxyPort || '';
  byId('m2mProxyUsername').value = settings.username || '';
  byId('m2mProxyPassword').value = '';
  byId('m2mProxyClearPassword').checked = false;
  byId('m2mTrustStorePath').value = settings.trustStorePath || '';
  byId('m2mTrustStorePassword').value = '';
  byId('m2mTrustStoreClearPassword').checked = false;
  byId('m2mTrustStoreType').value = settings.trustStoreType || 'JKS';
  byId('m2mProxyUpdatedAt').textContent = settings.updatedAt ? `Mentve: ${settings.updatedAt}` : 'Nincs mentett adat';
  byId('m2mProxyPasswordHint').textContent = settings.passwordConfigured
    ? 'Mentett proxy jelszó van. Új jelszó megadasaval felülírható.'
    : 'Nincs mentett proxy jelszó.';
  byId('m2mTrustStorePasswordHint').textContent = settings.trustStorePasswordConfigured
    ? 'Mentett truststore jelszó van. Új jelszó megadasaval felülírható.'
    : 'Nincs mentett truststore jelszó.';
}

/**
 * Betölti vagy lekéri a load proxy settings művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function loadProxySettings(){
  const settings = await m2mApi.loadProxySettings();
  fillProxyForm(settings || {});
  state.proxyLoaded = true;
}

/**
 * Előkészíti és elindítja a save proxy settings állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @param {*} event a feldolgozandó böngészőesemény
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function saveProxySettings(event){
  event?.preventDefault();
  setBusy(true);
  try{
    const payload = proxyFormToPayload();
    delete payload.testUrl;
    fillProxyForm(await m2mApi.saveProxySettings(payload) || {});
    showMessage('Proxy/TLS beállítások mentve.', 'success');
  }catch(error){
    showMessage(error.message, 'error');
  }finally{
    setBusy(false);
  }
}

/**
 * Megjeleníti vagy újrarendereli a render proxy test result állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} result a függvény result bemeneti értéke
 */
function renderProxyTestResult(result){
  const lines = [
    result.success ? 'Sikeres proxy/TLS teszt.' : 'Sikertelen proxy/TLS teszt.',
    result.message ? `Üzenet: ${result.message}` : '',
    result.testUrl ? `Teszt URL: ${result.testUrl}` : '',
    result.httpStatus != null ? `HTTP státusz: ${result.httpStatus}` : '',
    result.durationMs != null ? `Időtartam: ${result.durationMs} ms` : '',
    result.proxyEnabled != null ? `Proxy aktív: ${result.proxyEnabled ? 'igen' : 'nem'}` : '',
    result.proxySnapshot ? `Proxy snapshot: ${result.proxySnapshot}` : ''
  ].filter(Boolean);
  if(byId('m2mProxyTestResult')) byId('m2mProxyTestResult').textContent = lines.join('\n');
}

/**
 * A <code>testProxySettings</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function testProxySettings(){
  setBusy(true);
  try{
    const result = await m2mApi.testProxySettings(proxyFormToPayload());
    renderProxyTestResult(result || {});
    showMessage(result?.message || result?.status || 'Proxy/TLS teszt lefutott.', result?.success === false ? 'error' : 'success');
  }catch(error){
    if(byId('m2mProxyTestResult')) byId('m2mProxyTestResult').textContent = error.message;
    showMessage(error.message, 'error');
  }finally{
    setBusy(false);
  }
}

/**
 * A <code>toggleProxyPanel</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function toggleProxyPanel(){
  const panel = byId('m2mProxySettingsPanel');
  if(!panel) return;
  const willOpen = panel.hidden;
  panel.hidden = !willOpen;
  if(willOpen){
    if(!state.proxyLoaded){
      try{
        await loadProxySettings();
      }catch(error){
        showMessage(error.message, 'error');
      }
    }
    panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

/**
 * A <code>pollSelectedSubmission</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function pollSelectedSubmission(){
  if(document.hidden || state.busy) return;
  const current = selectedRow();
  if(current && isM2mTerminal(current)) return;
  try{
    await refreshSubmissions();
    if(state.selectedId) await loadEvents();
  }catch(error){
    console.warn('M2M polling failed', error);
  }
}

/**
 * Elindítja a start polling aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function startPolling(){
  if(state.pollTimer) window.clearInterval(state.pollTimer);
  state.pollTimer = window.setInterval(pollSelectedSubmission, 15000);
}

/**
 * Kezeli vagy beköti a bind events esemény- és inicializációs folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function bindEvents(){
  byId('m2mCreateForm')?.addEventListener('submit', createSubmission);
  byId('m2mRefreshButton')?.addEventListener('click', () => refreshSubmissions().catch(error => showMessage(error.message, 'error')));
  byId('m2mProxySettingsToggleButton')?.addEventListener('click', toggleProxyPanel);
  byId('m2mProxyTestButton')?.addEventListener('click', async () => {
    const panel = byId('m2mProxySettingsPanel');
    if(panel?.hidden) panel.hidden = false;
    if(!state.proxyLoaded){
      try{
        await loadProxySettings();
      }catch(error){
        showMessage(error.message, 'error');
      }
    }
    await testProxySettings();
  });
  byId('m2mProxySettingsForm')?.addEventListener('submit', saveProxySettings);
  byId('m2mProxyTestFromPanelButton')?.addEventListener('click', testProxySettings);
  byId('m2mProxyReloadButton')?.addEventListener('click', () => loadProxySettings().catch(error => showMessage(error.message, 'error')));
  byId('m2mPageSize')?.addEventListener('change', event => {
    state.pageSize = Number(event.target.value) || 10;
    state.page = 1;
    renderSubmissionsTable();
  });
  byId('m2mPrevPageButton')?.addEventListener('click', () => {
    if(state.page > 1){
      state.page -= 1;
      renderSubmissionsTable();
    }
  });
  byId('m2mNextPageButton')?.addEventListener('click', () => {
    if(state.page < totalPages(state.rows.length)){
      state.page += 1;
      renderSubmissionsTable();
    }
  });
  byId('m2mMarkForSubmitButton')?.addEventListener('click', () => runSelectedAction('mark', 'Csomag megjelölve beküldésre.'));
  byId('m2mWithdrawSubmitMarkButton')?.addEventListener('click', () => runSelectedAction('withdraw', 'Beküldésre jelölés visszavonva.'));
  byId('m2mUploadAttachmentsButton')?.addEventListener('click', () => runSelectedAction('/step/upload-attachments', 'Csatolmány feltöltés lefutott.'));
  byId('m2mCreateBizonylatButton')?.addEventListener('click', () => runSelectedAction('/step/create-bizonylat', 'Bizonylat beküldés lefutott.'));
}

/**
 * Kezeli vagy beköti a init m2m submitter page esemény- és inicializációs folyamatát.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 */
export function initM2mSubmitterPage(){
  updateFormNavigationLink();
  bindEvents();
  updateActionButtons();
  startPolling();
  refreshSubmissions().catch(error => showMessage(error.message, 'error'));
}

if(document.readyState === 'loading'){
  document.addEventListener('DOMContentLoaded', initM2mSubmitterPage, { once: true });
}else{
  initM2mSubmitterPage();
}
