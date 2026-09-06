/**
 * @module pages/xpath-validator-page
 *
 * A oldalspecifikus felhasználói felület- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

import { showMessage } from '../core/messages.js';


const PAGE_SIZE_KEY = 'xpathValidator.pageSize';
const REFRESH_KEY = 'xpathValidator.autoRefresh';

let selectedRequestId = null;
let autoRefreshHandle = null;
let initialized = false;

/**
 * A <code>byId</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} id a célobjektum technikai azonosítója
 */
const byId = id => document.getElementById(id);

/**
 * Kezeli vagy beköti a init xpath validator page esemény- és inicializációs folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
export function initXpathValidatorPage(){
  if(initialized || !byId('xpathValidatorForm')) return;
  initialized = true;

  const xmlFileInput = byId('xpathXmlFile');
  const pageSizeSelect = byId('xpathPageSizeSelect');
  const autoRefreshSelect = byId('xpathAutoRefreshSelect');
  const refreshButton = byId('xpathRefreshButton');
  const searchInput = byId('xpathSearchInput');
  const submitButton = byId('xpathSubmitButton');

  xmlFileInput?.addEventListener('change', syncSelectedXmlInputPath);
  submitButton?.addEventListener('click', submitValidation);
  pageSizeSelect?.addEventListener('change', () => {
    localStorage.setItem(PAGE_SIZE_KEY, pageSizeSelect.value);
    loadRequests();
  });
  autoRefreshSelect?.addEventListener('change', configureAutoRefresh);
  refreshButton?.addEventListener('click', async () => {
    await loadRequests();
    if(selectedRequestId) await loadRequestDetails(selectedRequestId);
  });
  searchInput?.addEventListener('input', () => {
    window.clearTimeout(searchInput._debounce);
    searchInput._debounce = window.setTimeout(loadRequests, 250);
  });

  syncSelectedXmlInputPath();
  loadConfig();
}

/**
 * Szinkronizálja vagy frissíti a sync selected xml input path által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function syncSelectedXmlInputPath(){
  const target = byId('selectedXpathXmlInputPath');
  if(!target) return;
  const file = byId('xpathXmlFile')?.files?.[0];
  target.textContent = file ? `Kiválasztott XML fájl: ${file.name}` : 'Nincs kiválasztott XML fájl.';
}

/**
 * Betölti vagy lekéri a load config művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function loadConfig(){
  try{
    const response = await fetch('/api/xpath-validator/config');
    if(!response.ok) return;
    const data = await response.json();
    const pageSizeSelect = byId('xpathPageSizeSelect');
    const autoRefreshSelect = byId('xpathAutoRefreshSelect');
    if(pageSizeSelect){
      pageSizeSelect.value = localStorage.getItem(PAGE_SIZE_KEY) || String(data.defaultPageSize || 10);
    }
    if(autoRefreshSelect){
      autoRefreshSelect.value = localStorage.getItem(REFRESH_KEY) || String(data.defaultAutoRefreshSeconds || 10);
    }
    configureAutoRefresh();
    await loadRequests();
  }catch(error){
    console.error('XPath validátor konfiguráció betöltési hiba', error);
  }
}

/**
 * Elindítja a submit validation aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function submitValidation(){
  const xmlFileInput = byId('xpathXmlFile');
  if(!xmlFileInput?.files?.length){
    showMessage('Válassz ki egy XML fájlt az XPATH validációhoz.', 'error');
    return;
  }
  const formData = new FormData();
  formData.append('file', xmlFileInput.files[0]);
  formData.append('createResult', 'ASYNC');
  try{
    const response = await fetch('/api/xpath-validator/requests', { method:'POST', body: formData });
    const text = await response.text();
    if(!response.ok) throw new Error(text || 'XPATH validáció indítása sikertelen');
    const data = JSON.parse(text);
    showMessage(`Az XPATH validáció elindult. Request ID: ${data.requestId} (${data.formName || ''} ${data.formVersion || ''})`, 'success');
    selectedRequestId = data.requestId;
    xmlFileInput.value = '';
    syncSelectedXmlInputPath();
    await loadRequests();
    await loadRequestDetails(selectedRequestId);
  }catch(error){
    console.error(error);
    showMessage(error.message || 'Az XPATH validáció indítása nem sikerült.', 'error');
  }
}

/**
 * Betölti vagy lekéri a load requests művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A hálózati hívás eredményét egységes kliensoldali hibakezeléssel adja tovább, és a felhasználói felületet csak a válasz feldolgozása után módosítja.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function loadRequests(){
  const tableBody = byId('xpathRequestsTableBody');
  if(!tableBody) return;
  const limit = byId('xpathPageSizeSelect')?.value || '10';
  const query = (byId('xpathSearchInput')?.value || '').trim();
  const url = new URL('/api/xpath-validator/requests', window.location.origin);
  url.searchParams.set('limit', limit);
  if(query) url.searchParams.set('query', query);
  try{
    const response = await fetch(url);
    if(!response.ok) throw new Error('XPATH kérések nem tölthetők be');
    const data = await response.json();
    const items = data.items || [];
    tableBody.replaceChildren();
    if(!items.length){
      const row = document.createElement('tr');
      const cell = document.createElement('td');
      cell.colSpan = 7;
      cell.textContent = 'Nincs adat.';
      row.appendChild(cell);
      tableBody.appendChild(row);
      return;
    }
        /**
     * A <code>safePlainText</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @param {*} value a feldolgozandó vagy beállítandó érték
     */
const safePlainText = value => String(value ?? '')
      .normalize('NFKC')
      .replace(/[<>&"'`]/g, '')
      .replace(/[\u0000-\u001F\u007F]/g, '')
      .slice(0, 512);
    items.forEach(item => {
      const requestId = String(item.requestId ?? '').trim();
      if(!/^[A-Za-z0-9_-]{1,80}$/.test(requestId)) return;
      const row = document.createElement('tr');
      row.dataset.requestId = requestId;
      if(requestId === String(selectedRequestId ?? '')) row.classList.add('active');
      [requestId, item.requestTimestampUtc || '-', `${item.formName || ''} ${item.formVersion || ''}`.trim(), item.validatorStatus || '-', item.resultStatus || '-', String(item.errorCount ?? '-')].forEach(value => {
        const cell = document.createElement('td');
        cell.textContent = safePlainText(value);
        row.appendChild(cell);
      });
      const actionCell = document.createElement('td');
      if(item.resultAvailable){
        const link = document.createElement('a');
        link.className = 'xpath-download-link';
        link.href = `/api/xpath-validator/requests/${encodeURIComponent(requestId)}/result`;
        link.textContent = 'Letöltés';
        actionCell.appendChild(link);
      }else actionCell.textContent = '-';
      row.appendChild(actionCell);
      tableBody.appendChild(row);
    });
    tableBody.querySelectorAll('tr[data-request-id]').forEach(row => {
      row.addEventListener('click', async event => {
        if(event.target.closest('a')) return;
        selectedRequestId = row.dataset.requestId;
        await loadRequests();
        await loadRequestDetails(selectedRequestId);
      });
    });
  }catch(error){
    console.error(error);
  }
}

/**
 * Betölti vagy lekéri a load request details művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A hálózati hívás eredményét egységes kliensoldali hibakezeléssel adja tovább, és a felhasználói felületet csak a válasz feldolgozása után módosítja.</p>
 * @param {*} requestId a célobjektum technikai azonosítója
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function loadRequestDetails(requestId){
  const tableBody = byId('xpathErrorsTableBody');
  if(!requestId || !tableBody) return;
  const selectedLabel = byId('xpathSelectedRequestLabel');
  if(selectedLabel) selectedLabel.textContent = `Kiválasztott kérés: ${requestId}`;
  try{
    const response = await fetch(`/api/xpath-validator/requests/${encodeURIComponent(requestId)}/errors`);
    if(!response.ok) throw new Error('XPATH hibák nem tölthetők be');
    const items = await response.json();
    tableBody.replaceChildren();
    if(!items.length){
      const row = document.createElement('tr');
      const cell = document.createElement('td');
      cell.colSpan = 6;
      cell.textContent = 'A kiválasztott kéréshez nincs kitárt hiba.';
      row.appendChild(cell);
      tableBody.appendChild(row);
      return;
    }
    items.forEach(item => {
      const row = document.createElement('tr');
      [item.errorCode || '-', item.errorMessage || '-', item.severity || '-', item.elementId || '-', item.ruleId || '-', item.path || '-'].forEach(value => {
        const cell = document.createElement('td');
        cell.textContent = String(value);
        row.appendChild(cell);
      });
      tableBody.appendChild(row);
    });
  }catch(error){
    console.error(error);
  }
}

/**
 * A <code>configureAutoRefresh</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function configureAutoRefresh(){
  if(autoRefreshHandle){
    clearInterval(autoRefreshHandle);
    autoRefreshHandle = null;
  }
  const seconds = Number(byId('xpathAutoRefreshSelect')?.value || 0);
  localStorage.setItem(REFRESH_KEY, String(seconds));
  if(seconds > 0){
    autoRefreshHandle = setInterval(() => {
      loadRequests();
      if(selectedRequestId) loadRequestDetails(selectedRequestId);
    }, seconds * 1000);
  }
}
