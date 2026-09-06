/**
 * @module m2m/m2m-api
 *
 * A M2M beküldési és csatolmánykezelési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A kliensoldali engedélyezés csak felhasználói visszajelzés; a tényleges M2M jogosultsági kontroll szerveroldali.
 */

import { buildDefaultHeaders } from '../core/api-client.js';

let m2mAvailability = { loaded:false, configured:false, missingKeys:[] };

/**
 * A <code>ensureAvailabilityDialog</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
 * @returns {*} a feldolgozás eredménye
 */
function ensureAvailabilityDialog(){
  let backdrop=document.getElementById('m2mConfigurationMissingDialog');
  if(backdrop) return backdrop;
  backdrop=document.createElement('div');
  backdrop.id='m2mConfigurationMissingDialog';
  backdrop.className='m2m-confirm-backdrop';
  backdrop.hidden=true;
  backdrop.innerHTML=`<section class="m2m-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="m2mConfigurationMissingTitle">
    <h3 id="m2mConfigurationMissingTitle">Az M2M kapcsolat nincs beállítva</h3>
    <p>Az M2M beküldés, a csatolmánykezelés, az online validáció és a kalkuláció csak a szükséges hitelesítési adatok megadása után használható.</p>
    <p id="m2mConfigurationMissingKeys" class="hint"></p>
    <div class="m2m-confirm-actions"><button type="button" data-m2m-config-close>Mégse</button><a class="button primary" href="/configuration.html?mode=advanced&category=M2M">M2M beállítások megnyitása</a></div>
  </section>`;
  backdrop.addEventListener('click',event=>{if(event.target===backdrop||event.target.closest('[data-m2m-config-close]'))backdrop.hidden=true;});
  document.body.appendChild(backdrop);
  return backdrop;
}

/**
 * Megjeleníti vagy újrarendereli a show m2m configuration missing dialog állapotát a felhasználói felületen.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 */
export function showM2mConfigurationMissingDialog(){
  const dialog=ensureAvailabilityDialog();
  const labels={
    'nav.m2m.auth.client-id':'kliensazonosító','nav.m2m.auth.client-secret':'kliens titok',
    'nav.m2m.auth.username':'technikai felhasználónév','nav.m2m.auth.password':'technikai jelszó',
    'nav.m2m.signature.key-first-part':'aláírókulcs első része','nav.m2m.signature.nonce':'aláírási nonce'
  };
  const missing=(m2mAvailability.missingKeys||[]).map(key=>labels[key]||key);
  const target=dialog.querySelector('#m2mConfigurationMissingKeys');
  if(target) target.textContent=missing.length?`Hiányzó beállítások: ${missing.join(', ')}.`:'';
  dialog.hidden=false;
}

/**
 * Betölti vagy lekéri a load m2m availability művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} force a függvény force bemeneti értéke
 * @returns {Promise<*>} a feldolgozás eredménye
 */
export async function loadM2mAvailability(force=false){
  if(m2mAvailability.loaded&&!force) return m2mAvailability;
  try{
    const response=await fetch('/api/m2m/availability',{credentials:'same-origin',cache:'no-store'});
    if(!response.ok) throw new Error(`HTTP ${response.status}`);
    const data=await response.json();
    m2mAvailability={loaded:true,configured:Boolean(data.configured),missingKeys:data.missingKeys||[]};
  }catch(_error){
    m2mAvailability={loaded:true,configured:false,missingKeys:[]};
  }
  applyM2mAvailabilityUi();
  window.dispatchEvent(new CustomEvent('m2m-availability-changed',{detail:m2mAvailability}));
  return m2mAvailability;
}

/**
 * Betölti vagy lekéri a get m2m availability művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @returns {*} a feldolgozás eredménye
 */
export function getM2mAvailability(){ return m2mAvailability; }

/**
 * Szinkronizálja vagy frissíti a apply m2m availability ui által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 */
function applyM2mAvailabilityUi(){
  const selectors=[
    '#m2mSubmitMenuButton','#m2mValidationMenuButton','#m2mCalculationMenuButton',
    '#m2mMarkForSubmitButton','#m2mUploadAttachmentsButton','#m2mCreateBizonylatButton',
    '[data-m2m-form-action]:not([data-m2m-form-action="show-m2m-logs"])',
    '[data-m2m-validation-action="online-validation"]','[data-m2m-calculation-action="online-calculation"]'
  ];
  document.querySelectorAll(selectors.join(',')).forEach(element=>{
    element.classList.toggle('m2m-unavailable',m2mAvailability.loaded&&!m2mAvailability.configured);
    element.setAttribute('aria-disabled',String(m2mAvailability.loaded&&!m2mAvailability.configured));
    if(m2mAvailability.loaded&&!m2mAvailability.configured) element.title='Az M2M hitelesítési adatok nincsenek beállítva.';
  });
}

let availabilityClickGuardBound=false;
/**
 * Kezeli vagy beköti a bind m2m availability click guard esemény- és inicializációs folyamatát.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 */
function bindM2mAvailabilityClickGuard(){
  if(availabilityClickGuardBound) return;
  availabilityClickGuardBound=true;
  document.addEventListener('click',event=>{
    const target=event.target.closest('.m2m-unavailable');
    if(!target||m2mAvailability.configured) return;
    event.preventDefault(); event.stopImmediatePropagation(); showM2mConfigurationMissingDialog();
  },true);
}

if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',()=>{bindM2mAvailabilityClickGuard();loadM2mAvailability();});
else { bindM2mAvailabilityClickGuard(); loadM2mAvailability(); }

/**
 * A <code>ensureM2mAvailable</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @returns {Promise<*>} a feldolgozás eredménye
 */
export async function ensureM2mAvailable(){
  const state=await loadM2mAvailability();
  if(state.configured) return true;
  showM2mConfigurationMissingDialog();
  return false;
}


/**
 * A <code>requestJson</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A hálózati hívás eredményét egységes kliensoldali hibakezeléssel adja tovább, és a felhasználói felületet csak a válasz feldolgozása után módosítja.</p>
 * @param {*} url a függvény url bemeneti értéke
 * @param {*} options a művelet opcionális beállításai
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function requestJson(url, options = {}){
  const response = await fetch(url, {
    credentials: 'same-origin',
    ...options,
    headers: {
      ...buildDefaultHeaders(),
      ...(options.headers || {})
    }
  });
  const text = await response.text();
  let data = null;
  if(text){
    try{
      data = JSON.parse(text);
    }catch(_error){
      data = text;
    }
  }
  if(!response.ok){
    const message = data?.message || data?.error || (typeof data === 'string' ? data : null) || `HTTP ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    error.body = data;
    throw error;
  }
  return data;
}

/**
 * A <code>submissionUrl</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} id a célobjektum technikai azonosítója
 * @param {*} suffix a függvény suffix bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function submissionUrl(id, suffix = ''){
  if(!id) throw new Error('Nincs M2M beküldési csomag.');
  return `/api/submissions/${encodeURIComponent(id)}${suffix}`;
}

/**
 * A <code>assertM2mOperationSucceeded</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} data a függvény data bemeneti értéke
 * @param {*} operation a függvény operation bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function assertM2mOperationSucceeded(data, operation){
  const internalStatus = String(data?.internalStatus || '').toUpperCase();
  const resultCode = String(data?.resultCode || '').toUpperCase();
  const failedStatuses = new Set([
    'TECHNICAL_FAILED',
    'SUBMIT_FAILED',
    'VALIDATION_FAILED',
    'SUBMITTED_WITH_ERROR'
  ]);
  if(failedStatuses.has(internalStatus) || resultCode.includes('ERROR')){
    throw new Error(data?.resultMessage || data?.resultCode || `M2M művelet technikai hibával zárult: ${operation}`);
  }
  return data;
}

export const m2mApi = {
  requestJson,
  listSubmissions: () => requestJson('/api/submissions', { cache: 'no-store' }),
  createSubmission: formData => requestJson('/api/submissions', { method: 'POST', body: formData }),
  createFilestoreSubmission: formData => requestJson('/api/submissions/filestore-files', { method: 'POST', body: formData }),
  updateSubmissionXml: (id, formData) => requestJson(submissionUrl(id, '/xml-content'), { method: 'POST', body: formData }),
  addAttachments: (id, formData) => requestJson(submissionUrl(id, '/attachments'), { method: 'POST', body: formData }),
  markForSubmit: id => requestJson(submissionUrl(id, '/mark-for-submit'), { method: 'POST' }),
  withdrawSubmitMark: id => requestJson(submissionUrl(id, '/withdraw-submit-mark'), { method: 'POST' }),
  getSubmissionEvents: id => requestJson(submissionUrl(id, '/events'), { cache: 'no-store' }),
  getSubmissionLogs: id => requestJson(submissionUrl(id, '/m2m-logs'), { cache: 'no-store' }),
  onlineValidation: id => requestJson(submissionUrl(id, '/validation/online'), { method: 'POST' }),
  validationStatus: id => requestJson(submissionUrl(id, '/validation/status'), { cache: 'no-store' }),
  validationErrors: id => requestJson(submissionUrl(id, '/validation/errors'), { cache: 'no-store' }),
  onlineCalculation: id => requestJson(submissionUrl(id, '/calculation/online'), { method: 'POST' }),
  calculationResult: id => requestJson(submissionUrl(id, '/calculation/result'), { cache: 'no-store' }),
  refreshAttachment: (id, attachmentId) => requestJson(submissionUrl(id, `/attachments/${encodeURIComponent(attachmentId)}/refresh`), { method: 'POST' }),
  deleteAttachment: (id, attachmentId) => requestJson(submissionUrl(id, `/attachments/${encodeURIComponent(attachmentId)}`), { method: 'DELETE' }),
  latestSubmissionForXmlFile: xmlFileId => requestJson(`/api/submissions/xml-files/${encodeURIComponent(xmlFileId)}/latest`, { cache: 'no-store' }),
  attachmentContentUrl: (id, attachmentId, download = false) => submissionUrl(id, `/attachments/${encodeURIComponent(attachmentId)}/content${download ? '?download=true' : ''}`),
  postSubmissionStep: async (id, path) => {
    const data = await requestJson(submissionUrl(id, path), { method: 'POST' });
    return assertM2mOperationSucceeded(data, path);
  },
  loadProxySettings: () => requestJson('/api/m2m-proxy-settings', { cache: 'no-store' }),
  saveProxySettings: payload => requestJson('/api/m2m-proxy-settings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload || {})
  }),
  testProxySettings: payload => requestJson('/api/m2m-proxy-settings/test', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload || {})
  })
};
