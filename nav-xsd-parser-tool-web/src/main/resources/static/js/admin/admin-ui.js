/**
 * @module admin/admin-ui
 *
 * A adminisztrációs működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

import { apiGetJson, apiPostJson } from '../core/api-client.js';
/**
 * Admin oldal vezérlője.
 *
 * A modul kezeli a rendszerinformációkat, loggingot, konfigurációt,
 * cache műveleteket, XML index konfigurációt és a GitHub sémafrissítőt.
 */
export function createAdminUi({
  elements,
  showMessage,
  escapeHtml,
  isCurrentUserAdmin,
  getCurrentSecurityUser,
  fetchSchemaRegistryStatus
}){
  const {
    refreshAdminButton,
    adminApplicationName,
    adminVersion,
    adminUptime,
    adminJavaVersion,
    adminOsInfo,
    adminConfigDir,
    adminLogFile,
    adminRootLogLevel,
    saveAdminLoggingButton,
    adminLogViewer,
    validateConfigButton,
    adminConfigProperties,
    adminConfigDiagnostics,
    reloadConfigButton,
    githubSchemaDryRunButton,
    githubSchemaUpdateButton,
    githubSchemaTargetDir,
    githubSchemaOrganization,
    githubSchemaTokenState,
    githubSchemaDownloadMode,
    githubSchemaArchiveTemplate,
    githubSchemaRateLimitState,
    githubSchemaRateLimitRetries,
    githubSchemaUpdaterResult,
    adminSchemaCacheCount,
    adminCacheFiles,
    clearCacheButton,
    reloadCacheButton,
    xmlIndexFormSelect,
    xmlIndexVersionSelect,
    xmlIndexPartSelect,
    xmlIndexReloadButton,
    xmlIndexSaveButton,
    xmlIndexConfigPath,
    xmlIndexTree,
    xmlIndexSummary,
    xmlIndexFieldSearch,
    xmlIndexFieldSelect,
    xmlIndexAddFieldButton
  } = elements;


    /**
   * Szinkronizálja vagy frissíti a set text által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} target a függvény target bemeneti értéke
   * @param {*} value a feldolgozandó vagy beállítandó érték
   */
function setText(target, value){
    if(target) target.textContent = value ?? '-';
  }
    /**
   * Megjeleníti vagy újrarendereli a render config diagnostics állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} target a függvény target bemeneti értéke
   * @param {*} diagnostics a függvény diagnostics bemeneti értéke
   */
function renderConfigDiagnostics(target, diagnostics){
    if(!target) return;
    if(!diagnostics){
      target.innerHTML = '<div class="diag-card"><span class="v">Nincs diagnosztikai adat.</span></div>';
      return;
    }
    const cards = [
      { label: 'Schema gyökér', path: diagnostics.schemaRoot, exists: diagnostics.schemaRootExists },
      { label: 'Common XSD', path: diagnostics.commonXsd, exists: diagnostics.commonXsdExists },
      { label: 'XPath szabályok', path: diagnostics.xpathRules, exists: diagnostics.xpathRulesExists },
      { label: 'Log könyvtár', path: diagnostics.logDirectory, exists: diagnostics.logDirectoryExists }
    ];
    target.innerHTML = cards.map(card => {
      const status = card.exists ? 'létezik · könyvtár · olvasható · írható' : 'nem található';
      const path = card.path ?? '-';
      return `<div class="diag-card"><span class="k">${escapeHtml(card.label)}</span><span class="diag-status">${escapeHtml(status)}</span><span class="v">${escapeHtml(String(path))}</span></div>`;
    }).join('');
  }
    /**
   * Betölti vagy lekéri a load admin system művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadAdminSystem(){
    try{
      const data = await apiGetJson('/api/admin/system');
      setText(adminApplicationName, data.applicationName);
      setText(adminVersion, data.version);
      setText(adminUptime, data.uptime);
      setText(adminJavaVersion, data.java);
      setText(adminOsInfo, data.os);
      setText(adminConfigDir, data.configDirectory);
      setText(adminLogFile, data.logFile);
    }catch(error){ console.error(error); }
  }
    /**
   * Betölti vagy lekéri a load admin logging művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadAdminLogging(){
    try{
      const data = await apiGetJson('/api/admin/logging');
      if(adminRootLogLevel && data.rootLevel) adminRootLogLevel.value = data.rootLevel;
      if(adminLogViewer) adminLogViewer.value = (data.tailLines || []).join('\n');
      if(adminLogFile && data.logFile) adminLogFile.textContent = data.logFile;
    }catch(error){ console.error(error); }
  }
    /**
   * Előkészíti és elindítja a save admin logging állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function saveAdminLogging(){
    try{
      const data = await apiPostJson('/api/admin/logging', {
        rootLevel: adminRootLogLevel?.value,
        saveToExternalConfig: true
      });
      if(adminLogViewer) adminLogViewer.value = (data.tailLines || []).join('\n');
      showMessage('A logging beállítások elmentve.', 'success');
    }catch(error){
      console.error(error);
      showMessage('A logging beállítások mentése nem sikerült.', 'error');
    }
  }
    /**
   * Betölti vagy lekéri a load admin config művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadAdminConfig(){
    try{
      const response = await fetch('/api/admin/config');
      if(!response.ok) throw new Error('Konfiguráció nem érhető el');
      const data = await response.json();
      renderConfigDiagnostics(adminConfigDiagnostics, data.diagnostics);
      const activeProperties = data.activeProperties || {};
      adminConfigProperties.textContent = Object.entries(activeProperties).map(([key, value]) => `${key}=${value ?? ''}`).join('\n') || 'Nincs aktív property adat.';
    }catch(error){ console.error(error); }
  }
    /**
   * Ellenőrzi a validate admin config feltételeit, és a hívó számára döntési eredményt állít elő.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function validateAdminConfig(){
    await loadAdminConfig();
    showMessage('A konfiguráció ellenőrzése lefutott.', 'success');
  }
    /**
   * A <code>reloadAdminConfig</code> függvény a adminisztrációs folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function reloadAdminConfig(){
    try{
      const response = await fetch('/api/admin/config/reload', { method:'POST' });
      if(!response.ok) throw new Error('Konfiguráció újraolvasása sikertelen');
      const data = await response.json();
      renderConfigDiagnostics(adminConfigDiagnostics, data.diagnostics);
      const activeProperties = data.activeProperties || {};
      adminConfigProperties.textContent = Object.entries(activeProperties).map(([key, value]) => `${key}=${value ?? ''}`).join('\n') || 'Nincs aktív property adat.';
      showMessage('A konfiguráció újraolvasása kész.', 'success');
    }catch(error){
      console.error(error);
      showMessage('A konfiguráció újraolvasása nem sikerült.', 'error');
    }
  }
    /**
   * Betölti vagy lekéri a load admin cache művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadAdminCache(){
    try{
      const response = await fetch('/api/admin/cache');
      if(!response.ok) throw new Error('Cache információ nem érhető el');
      const data = await response.json();
      setText(adminSchemaCacheCount, data.schemaRegistry?.cacheEntryCount);
      adminCacheFiles.textContent = data.schemaRegistry?.phase || 'Nincs cache adat.';
    }catch(error){ console.error(error); }
  }
    /**
   * Eltávolítja vagy alaphelyzetbe állítja a clear admin caches művelethez tartozó kliensoldali állapotot.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function clearAdminCaches(){
    try{
      const response = await fetch('/api/admin/cache/clear', { method:'POST' });
      if(!response.ok) throw new Error('Cache ürítés sikertelen');
      await loadAdminCache();
      showMessage('A cache-ek kiürítve.', 'success');
    }catch(error){
      console.error(error);
      showMessage('A cache-ek ürítése nem sikerült.', 'error');
    }
  }
    /**
   * A <code>reloadAdminCaches</code> függvény a adminisztrációs folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function reloadAdminCaches(){
    try{
      const response = await fetch('/api/admin/cache/reload', { method:'POST' });
      if(!response.ok) throw new Error('Cache újratöltés sikertelen');
      await loadAdminCache();
      await fetchSchemaRegistryStatus();
      showMessage('A cache-ek újratöltése elindult.', 'success');
    }catch(error){
      console.error(error);
      showMessage('A cache-ek újratöltése nem sikerült.', 'error');
    }
  }

  let xmlIndexForms = [];
  let xmlIndexCurrentStructure = null;
  let xmlIndexSelectedConfig = null;
  let xmlIndexAvailableFields = [];
  let xmlIndexFormParts = [];

    /**
   * Megjeleníti vagy újrarendereli a render xml index context notice állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function renderXmlIndexContextNotice(){
    const notice = document.getElementById('xmlIndexContextNotice');
    if(!notice) return;
    const params = new URLSearchParams(location.search);
    notice.hidden = params.get('reason') !== 'multiform-index-required';
  }

    /**
   * Betölti vagy lekéri a load xml index forms művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadXmlIndexForms(){
    if(!xmlIndexFormSelect) return;
    renderXmlIndexContextNotice();
    try{
      const response = await fetch('/api/xml-index-config/forms', { credentials:'same-origin' });
      if(!response.ok) throw new Error(await response.text() || 'Az XML index űrlaplista betöltése sikertelen.');
      const data = await response.json();
      xmlIndexForms = data.forms || [];
      if(xmlIndexConfigPath) xmlIndexConfigPath.textContent = data.configPath || '-';
      const current = xmlIndexFormSelect.value;
      xmlIndexFormSelect.innerHTML = '<option value="">Válassz űrlapt...</option>' + xmlIndexForms.map(form => {
        const marker = form.configured ? ' ✓' : '';
        return `<option value="${escapeHtml(form.formName)}">${escapeHtml(form.formName + marker)}</option>`;
      }).join('');
      if(current && xmlIndexForms.some(f => f.formName === current)) xmlIndexFormSelect.value = current;
      const params = new URLSearchParams(location.search);
      const requestedForm = params.get('formName');
      if(requestedForm && xmlIndexForms.some(f => f.formName === requestedForm)) xmlIndexFormSelect.value = requestedForm;
      const returnButton = document.getElementById('xmlIndexSaveReturnButton');
      if(returnButton) returnButton.disabled = !params.get('returnUrl');
      syncXmlIndexVersionOptions();
    }catch(error){
      console.error(error);
      if(xmlIndexTree) xmlIndexTree.innerHTML = `<p class="message error">${escapeHtml(error.message || 'Az XML index űrlaplista betöltése sikertelen.')}</p>`;
    }
  }

    /**
   * Szinkronizálja vagy frissíti a sync xml index version options által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function syncXmlIndexVersionOptions(){
    if(!xmlIndexFormSelect || !xmlIndexVersionSelect) return;
    const form = xmlIndexForms.find(item => item.formName === xmlIndexFormSelect.value);
    const versions = form?.versions || [];
    xmlIndexVersionSelect.disabled = versions.length === 0;
    xmlIndexVersionSelect.innerHTML = '<option value="">Automatikus / legfrissebb</option>' + versions.map(version => `<option value="${escapeHtml(version)}">${escapeHtml(version)}</option>`).join('');
    const requestedVersion = new URLSearchParams(location.search).get('sourceVersion');
    if(requestedVersion && versions.includes(requestedVersion)) xmlIndexVersionSelect.value = requestedVersion;
  }

    /**
   * Szinkronizálja vagy frissíti a sync xml index part options által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   */
function syncXmlIndexPartOptions(){
    if(!xmlIndexPartSelect) return;
    const current = xmlIndexPartSelect.value;
    const options = (xmlIndexFormParts || []).map(part => {
      const role = part.role === 'REPEATING' ? 'melléklap' : (part.role === 'MAIN' ? 'főlap' : 'lap');
      const count = Number(part.fieldCount || 0);
      const marker = part.configured ? ' ✓' : '';
      return `<option value="${escapeHtml(part.name || '')}">${escapeHtml((part.label || part.name || '') + ' · ' + role + ' · ' + count + ' mező' + marker)}</option>`;
    }).join('');
    xmlIndexPartSelect.disabled = !xmlIndexFormParts.length;
    xmlIndexPartSelect.innerHTML = '<option value="">Összes űrlaprész</option>' + options;
    if(current && xmlIndexFormParts.some(part => part.name === current)) xmlIndexPartSelect.value = current;
  }

    /**
   * Feloldja a selected xml index part name eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @returns {*} a feldolgozás eredménye
   */
function selectedXmlIndexPartName(){
    return xmlIndexPartSelect?.value || '';
  }

    /**
   * A <code>xmlIndexPartLabel</code> függvény a adminisztrációs folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} name a feloldáshoz vagy megjelenítéshez használt név
   * @returns {*} a feldolgozás eredménye
   */
function xmlIndexPartLabel(name){
    const part = (xmlIndexFormParts || []).find(item => item.name === name);
    if(!part) return name || '';
    const role = part.role === 'REPEATING' ? 'melléklap' : (part.role === 'MAIN' ? 'főlap' : 'lap');
    return `${part.label || part.name} · ${role}`;
  }

    /**
   * Betölti vagy lekéri a load xml index structure művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadXmlIndexStructure(){
    if(!xmlIndexFormSelect || !xmlIndexTree) return;
    const formName = xmlIndexFormSelect.value;
    if(!formName){
      xmlIndexCurrentStructure = null;
      xmlIndexSelectedConfig = null;
      xmlIndexAvailableFields = [];
      xmlIndexFormParts = [];
      syncXmlIndexPartOptions();
      xmlIndexTree.innerHTML = '<p class="hint">Válassz űrlapt az XSD repo alapján felismert mezőlista betöltéséhez.</p>';
      syncXmlIndexFieldPicker();
      renderXmlIndexSummary();
      if(xmlIndexSaveButton) xmlIndexSaveButton.disabled = true;
      return;
    }
    try{
      xmlIndexTree.innerHTML = '<p class="hint">Teljes XSD struktúra betöltése...</p>';
      const version = xmlIndexVersionSelect?.value || '';
      const url = `/api/xml-index-config/structure?formName=${encodeURIComponent(formName)}${version ? `&sourceVersion=${encodeURIComponent(version)}` : ''}`;
      const response = await fetch(url, { credentials:'same-origin' });
      if(!response.ok) throw new Error(await response.text() || 'Az XML index struktúra betöltése sikertelen.');
      xmlIndexCurrentStructure = await response.json();
      xmlIndexAvailableFields = xmlIndexCurrentStructure.fields || [];
      xmlIndexFormParts = xmlIndexCurrentStructure.formParts || [];
      syncXmlIndexPartOptions();
      const requestedPart = new URLSearchParams(location.search).get('formPartName');
      if(requestedPart && xmlIndexFormParts.some(part => part.name === requestedPart)) xmlIndexPartSelect.value = requestedPart;
      xmlIndexSelectedConfig = structureToConfig(xmlIndexCurrentStructure);
      if(xmlIndexFieldSearch) xmlIndexFieldSearch.value = "";
      renderXmlIndexTree();
      renderXmlIndexSummary();
      if(xmlIndexSaveButton) xmlIndexSaveButton.disabled = false;
      if(new URLSearchParams(location.search).get('reason') === 'multiform-index-required'){
        setTimeout(() => xmlIndexFieldSearch?.focus(), 0);
      }
    }catch(error){
      console.error(error);
      xmlIndexTree.innerHTML = `<p class="message error">${escapeHtml(error.message || 'Az XML index struktúra betöltése sikertelen.')}</p>`;
      if(xmlIndexSaveButton) xmlIndexSaveButton.disabled = true;
    }
  }

    /**
   * A <code>structureToConfig</code> függvény a adminisztrációs folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} structure a függvény structure bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function structureToConfig(structure){
    const savedFields = (structure?.savedConfig?.fields || []).map(field => ({
      name: field.name,
      label: field.label,
      xmlPath: field.xmlPath,
      formPartName: field.formPartName,
      formPartRole: field.formPartRole,
      parentInfo: field.parentInfo,
      searchable: !!field.searchable,
      display: !!field.display,
      defaultSearch: !!field.defaultSearch,
      matchMode: field.matchMode || 'contains'
    }));
    const selectedByKey = new Map(savedFields.map(field => [xmlIndexFieldKey(field), field]));
    // Backwards compatibility if an older backend still sends chain based saved config.
    (structure?.savedConfig?.chains || []).forEach(chain => (chain.fields || []).forEach(field => {
      const item = {
        name: field.name,
        label: field.label,
        xmlPath: field.xmlPath,
        formPartName: field.formPartName,
        formPartRole: field.formPartRole,
        parentInfo: field.parentInfo,
        searchable: !!field.searchable,
        display: !!field.display,
        defaultSearch: !!field.defaultSearch,
        matchMode: field.matchMode || 'contains'
      };
      selectedByKey.set(xmlIndexFieldKey(item), item);
    }));
    return {
      formName: structure?.formName || '',
      label: structure?.label || structure?.formName || '',
      structureSourceVersion: structure?.sourceVersion || '',
      fields: Array.from(selectedByKey.values()),
      chains: []
    };
  }

    /**
   * A <code>xmlIndexFieldKey</code> függvény a adminisztrációs folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} field a függvény field bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function xmlIndexFieldKey(field){
    return field?.xmlPath || field?.name || '';
  }

    /**
   * Feldolgozza a normalize xml index search text bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function normalizeXmlIndexSearchText(value){
    return String(value || '').toLocaleLowerCase('hu-HU');
  }

    /**
   * Ellenőrzi a is xml index field selected feltételeit, és a hívó számára döntési eredményt állít elő.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} field a függvény field bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function isXmlIndexFieldSelected(field){
    const key = xmlIndexFieldKey(field);
    return (xmlIndexSelectedConfig?.fields || []).some(selected => xmlIndexFieldKey(selected) === key);
  }

    /**
   * A <code>filteredXmlIndexAvailableFields</code> függvény a adminisztrációs folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @returns {*} a feldolgozás eredménye
   */
function filteredXmlIndexAvailableFields(){
    const query = normalizeXmlIndexSearchText(xmlIndexFieldSearch?.value || '').trim();
    const partName = selectedXmlIndexPartName();
    let fields = xmlIndexAvailableFields || [];
    if(partName) fields = fields.filter(field => field.formPartName === partName || (field.xmlPath || '').includes('/' + partName + '/'));
    if(!query) return fields;
    return fields.filter(field => normalizeXmlIndexSearchText(`${field.label || ''} ${field.name || ''} ${field.xmlPath || ''} ${field.parentInfo || ''} ${field.formPartName || ''}`).includes(query));
  }

    /**
   * Megjeleníti vagy újrarendereli a render xml index tree állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function renderXmlIndexTree(){
    if(!xmlIndexTree) return;
    if(!xmlIndexSelectedConfig){
      xmlIndexTree.innerHTML = '<p class="hint">Válassz űrlapt az XSD repo alapján felismert mezőlista betöltéséhez.</p>';
      syncXmlIndexFieldPicker();
      return;
    }
    const fields = filteredXmlIndexAvailableFields();
    syncXmlIndexFieldPicker(fields);
    if(!xmlIndexAvailableFields.length){
      xmlIndexTree.innerHTML = '<p class="hint">Ehhez a űrlaphoz vagy kiválasztott űrlaprészhez nem található XSD mező. Ellenőrizd a nav.xsdparsertool.paths.schema-dir mappát és a kiválasztott struktúraforrás verziót.</p>';
      return;
    }
    if(!fields.length){
      xmlIndexTree.innerHTML = '<p class="hint">Nincs találat a megadott keresésre.</p>';
      return;
    }
    xmlIndexTree.innerHTML = `${renderXmlIndexPartInfo()}<div class="xml-index-field-list">${fields.slice(0, 200).map(renderXmlIndexAvailableField).join('')}</div>${fields.length > 200 ? `<p class="hint">Csak az első 200 találat jelenik meg. Szűkítsd a keresést.</p>` : ''}`;
  }

    /**
   * Megjeleníti vagy újrarendereli a render xml index part info állapotát a felhasználói felületen.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @returns {*} a feldolgozás eredménye
   */
function renderXmlIndexPartInfo(){
    if(!xmlIndexFormParts?.length) return '';
    const partName = selectedXmlIndexPartName();
    if(!partName){
      const repeating = xmlIndexFormParts.filter(part => part.role === 'REPEATING').length;
      return `<p class="hint">Felismert űrlaprészek: ${xmlIndexFormParts.length}. Ismétlődő melléklap: ${repeating}. A mezők szűrhetők konkrét főlapra vagy melléklapra.</p>`;
    }
    return `<p class="hint">Aktív űrlaprész: ${escapeHtml(xmlIndexPartLabel(partName))}. A mentés továbbra is űrlaponként történik, de a mezők form part + path adattal kerülnek XML-be.</p>`;
  }

    /**
   * Megjeleníti vagy újrarendereli a render xml index available field állapotát a felhasználói felületen.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} field a függvény field bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function renderXmlIndexAvailableField(field){
    const selected = isXmlIndexFieldSelected(field);
    const key = escapeHtml(xmlIndexFieldKey(field));
    return `<button type="button" class="xml-index-available-field${selected ? ' selected' : ''}" data-field-key="${key}">
      <span class="xml-index-available-field-title">${escapeHtml(field.label || field.name || '')}</span>
      <span class="xml-index-available-field-meta">${escapeHtml(field.name || '')}${field.formPartName ? ' · ' + escapeHtml(xmlIndexPartLabel(field.formPartName)) : ''}</span>
      <span class="xml-index-available-field-path">${escapeHtml(field.xmlPath || '')}</span>
      ${field.parentInfo ? `<span class="xml-index-available-field-path">Szülő: ${escapeHtml(field.parentInfo)}</span>` : ''}
      <span class="xml-index-available-field-action">${selected ? 'Hozzáadva' : 'Hozzáadás'}</span>
    </button>`;
  }

    /**
   * Szinkronizálja vagy frissíti a sync xml index field picker által kezelt állapotot a megadott adatok alapján.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} fields a függvény fields bemeneti értéke
   */
function syncXmlIndexFieldPicker(fields = filteredXmlIndexAvailableFields()){
    if(!xmlIndexFieldSelect || !xmlIndexAddFieldButton) return;
    const selectable = (fields || xmlIndexAvailableFields).filter(field => !isXmlIndexFieldSelected(field));
    xmlIndexFieldSelect.disabled = !xmlIndexSelectedConfig || selectable.length === 0;
    xmlIndexAddFieldButton.disabled = xmlIndexFieldSelect.disabled || !xmlIndexFieldSelect.value;
    xmlIndexFieldSelect.innerHTML = '<option value="">Válassz mezőt...</option>' + selectable.slice(0, 300).map(field => `<option value="${escapeHtml(xmlIndexFieldKey(field))}">${escapeHtml(field.label || field.name || '')} · ${escapeHtml(field.name || '')}</option>`).join('');
  }

    /**
   * Előkészíti és elindítja a add xml index field by key állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} key a függvény key bemeneti értéke
   */
function addXmlIndexFieldByKey(key){
    if(!xmlIndexSelectedConfig || !key) return;
    const source = xmlIndexAvailableFields.find(field => xmlIndexFieldKey(field) === key);
    if(!source || isXmlIndexFieldSelected(source)) return;
    xmlIndexSelectedConfig.fields.push({
      name: source.name,
      label: source.label,
      xmlPath: source.xmlPath,
      formPartName: source.formPartName,
      formPartRole: source.formPartRole,
      parentInfo: source.parentInfo,
      searchable: true,
      display: true,
      defaultSearch: false,
      matchMode: 'contains'
    });
    renderXmlIndexTree();
    renderXmlIndexSummary();
    if(xmlIndexSaveButton) xmlIndexSaveButton.disabled = false;
  }

    /**
   * Szinkronizálja vagy frissíti a update xml index selected field által kezelt állapotot a megadott adatok alapján.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} index az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
   * @param {*} patch a függvény patch bemeneti értéke
   */
function updateXmlIndexSelectedField(index, patch){
    const field = xmlIndexSelectedConfig?.fields?.[index];
    if(!field) return;
    Object.assign(field, patch);
    if(field.defaultSearch) field.searchable = true;
    renderXmlIndexSummary();
  }

    /**
   * Eltávolítja vagy alaphelyzetbe állítja a remove xml index selected field művelethez tartozó kliensoldali állapotot.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} index az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
   */
function removeXmlIndexSelectedField(index){
    if(!xmlIndexSelectedConfig?.fields) return;
    xmlIndexSelectedConfig.fields.splice(index, 1);
    renderXmlIndexTree();
    renderXmlIndexSummary();
  }

    /**
   * Megjeleníti vagy újrarendereli a render xml index summary állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function renderXmlIndexSummary(){
    if(!xmlIndexSummary) return;
    if(!xmlIndexSelectedConfig){
      xmlIndexSummary.innerHTML = '<p class="hint">Még nincs kiválasztott mező.</p>';
      return;
    }
    const selected = xmlIndexSelectedConfig.fields || [];
    if(!selected.length){
      xmlIndexSummary.innerHTML = '<p class="hint">Még nincs kiválasztott mező. A bal oldali mezőlistából válassz mezőt, majd add hozzá a konfigurációhoz.</p>';
      return;
    }
    xmlIndexSummary.innerHTML = selected.map((field, index) => `<div class="xml-index-summary-row xml-index-selected-editor">
      <div class="xml-index-selected-header">
        <div><strong>${escapeHtml(field.label || field.name)}</strong><span>${escapeHtml(field.name)}${field.formPartName ? ' · ' + escapeHtml(xmlIndexPartLabel(field.formPartName)) : ''} · ${escapeHtml(field.xmlPath || '')}${field.parentInfo ? ' · Szülő: ' + escapeHtml(field.parentInfo) : ''}</span></div>
        <button type="button" class="secondary compact" data-remove-index-field="${index}">Eltávolítás</button>
      </div>
      <div class="xml-index-selected-controls">
        <label class="xml-index-check"><input type="checkbox" data-selected-index="${index}" data-selected-flag="searchable" ${field.searchable ? 'checked' : ''}> Kereshető</label>
        <label class="xml-index-check"><input type="checkbox" data-selected-index="${index}" data-selected-flag="display" ${field.display ? 'checked' : ''}> Lista</label>
        <label class="xml-index-check"><input type="checkbox" data-selected-index="${index}" data-selected-flag="defaultSearch" ${field.defaultSearch ? 'checked' : ''}> Alapértelmezett</label>
        <select class="xml-index-match" data-selected-index="${index}" data-selected-flag="matchMode">
          <option value="contains" ${field.matchMode === 'contains' ? 'selected' : ''}>Tartalmazza</option>
          <option value="exact" ${field.matchMode === 'exact' ? 'selected' : ''}>Pontos</option>
          <option value="startsWith" ${field.matchMode === 'startsWith' ? 'selected' : ''}>Ezzel kezdődik</option>
        </select>
      </div>
    </div>`).join('');
  }


    /**
   * Kezeli vagy beköti a on xml index field list click esemény- és inicializációs folyamatát.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   */
function onXmlIndexFieldListClick(event){
    const button = event.target?.closest?.('[data-field-key]');
    if(!button) return;
    addXmlIndexFieldByKey(button.dataset.fieldKey || '');
  }

    /**
   * Kezeli vagy beköti a on xml index summary change esemény- és inicializációs folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   */
function onXmlIndexSummaryChange(event){
    const target = event.target;
    if(!target?.dataset?.selectedFlag) return;
    const index = Number(target.dataset.selectedIndex);
    const flag = target.dataset.selectedFlag;
    if(flag === 'matchMode') updateXmlIndexSelectedField(index, { matchMode: target.value || 'contains' });
    else updateXmlIndexSelectedField(index, { [flag]: !!target.checked });
  }

    /**
   * Kezeli vagy beköti a on xml index summary click esemény- és inicializációs folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   */
function onXmlIndexSummaryClick(event){
    const button = event.target?.closest?.('[data-remove-index-field]');
    if(!button) return;
    removeXmlIndexSelectedField(Number(button.dataset.removeIndexField));
  }

    /**
   * Előkészíti és elindítja a save xml index config állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function saveXmlIndexConfig(){
    if(!xmlIndexSelectedConfig?.formName) return;
    try{
      const response = await fetch(`/api/xml-index-config/forms/${encodeURIComponent(xmlIndexSelectedConfig.formName)}`, {
        method:'POST',
        credentials:'same-origin',
        headers:{'Content-Type':'application/json'},
        body: JSON.stringify(xmlIndexSelectedConfig)
      });
      if(!response.ok) throw new Error(await response.text() || 'Az XML index konfiguráció mentése sikertelen.');
      const data = await response.json();
      showMessage(`XML index konfiguráció mentve: ${data.formName} (${data.fieldCount} mező).`, 'success');
      await loadXmlIndexForms();
      return true;
    }catch(error){
      console.error(error);
      showMessage(error.message || 'Az XML index konfiguráció mentése sikertelen.', 'error');
      return false;
    }
  }

    /**
   * Betölti vagy lekéri a load xml index admin művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadXmlIndexAdmin(){
    if(!xmlIndexFormSelect) return;
    await loadXmlIndexForms();
    if(xmlIndexFormSelect.value) await loadXmlIndexStructure();
  }
    /**
   * Betölti vagy lekéri a load git hub schema updater config művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadGitHubSchemaUpdaterConfig(){
    if(!githubSchemaTargetDir) return;
    try{
      const response = await fetch('/api/admin/github-schema-updater/config', { credentials:'same-origin' });
      if(!response.ok) throw new Error(await response.text() || 'GitHub sémafrissítő konfiguráció nem érhető el.');
      const data = await response.json();
      setText(githubSchemaTargetDir, data.targetSchemaDir);
      setText(githubSchemaOrganization, data.organization);
      setText(githubSchemaTokenState, data.tokenConfigured ? 'beállítva' : 'nincs token');
      setText(githubSchemaDownloadMode, data.downloadMode || '-');
      setText(githubSchemaArchiveTemplate, data.archiveUrlTemplate || '-');
      setText(githubSchemaRateLimitState, data.rateLimitEnabled ? 'bekapcsolva' : 'kikapcsolva');
      setText(githubSchemaRateLimitRetries, data.rateLimitMaxRetries ?? '-');
    }catch(error){
      console.error(error);
      if(githubSchemaUpdaterResult) githubSchemaUpdaterResult.textContent = error.message || 'GitHub sémafrissítő konfiguráció nem érhető el.';
    }
  }

    /**
   * Megjeleníti vagy újrarendereli a render git hub schema update result állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} data a függvény data bemeneti értéke
   */
function renderGitHubSchemaUpdateResult(data){
    if(!githubSchemaUpdaterResult) return;
    if(!data){ githubSchemaUpdaterResult.textContent = 'Nincs futtatási eredmény.'; return; }
    const lines = [];
    lines.push(`Szervezet: ${data.organization || '-'}`);
    lines.push(`Célkönyvtár: ${data.targetSchemaDir || '-'}`);
    lines.push(`Mód: ${data.dryRun ? 'ellenőrzés letöltés nélkül' : 'frissítés'}`);
    lines.push(`Repók: ${data.repositoryCount ?? 0}, letöltve: ${data.downloadedCount ?? 0}, kihagyva: ${data.skippedCount ?? 0}, hibás: ${data.failedCount ?? 0}`);
    lines.push('');
    (data.repositories || []).forEach(repo => {
      lines.push(`[${repo.repositoryName}] lokális legmagasabb tag: ${repo.localHighestTag || '-'}, remote tagek: ${repo.remoteTagCount ?? 0}`);
      (repo.tags || []).forEach(tag => {
        lines.push(`  - ${tag.tagName}: ${tag.status}${tag.message ? ' - ' + tag.message : ''}`);
      });
    });
    githubSchemaUpdaterResult.textContent = lines.join('\n');
  }

    /**
   * Elindítja a run git hub schema update aszinkron vagy több lépéses frontend folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} dryRun a függvény dryRun bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function runGitHubSchemaUpdate(dryRun){
    if(!githubSchemaUpdaterResult) return;
    githubSchemaUpdaterResult.textContent = dryRun ? 'GitHub sémafrissítés ellenőrzése folyamatban...' : 'GitHub sémafrissítés folyamatban...';
    if(githubSchemaDryRunButton) githubSchemaDryRunButton.disabled = true;
    if(githubSchemaUpdateButton) githubSchemaUpdateButton.disabled = true;
    try{
      const response = await fetch(dryRun ? '/api/admin/github-schema-updater/dry-run' : '/api/admin/github-schema-updater/update', {
        method:'POST',
        credentials:'same-origin',
        headers:{'Content-Type':'application/json'},
        body: JSON.stringify({ dryRun })
      });
      if(!response.ok) throw new Error(await response.text() || 'GitHub sémafrissítés sikertelen.');
      const data = await response.json();
      renderGitHubSchemaUpdateResult(data);
      showMessage(dryRun ? 'GitHub sémafrissítés ellenőrzése kész.' : 'GitHub sémafrissítés kész.', data.failedCount ? 'warning' : 'success');
    }catch(error){
      console.error(error);
      githubSchemaUpdaterResult.textContent = error.message || 'GitHub sémafrissítés sikertelen.';
      showMessage('GitHub sémafrissítés sikertelen.', 'error');
    }finally{
      if(githubSchemaDryRunButton) githubSchemaDryRunButton.disabled = false;
      if(githubSchemaUpdateButton) githubSchemaUpdateButton.disabled = false;
    }
  }

    /**
   * Betölti vagy lekéri a load admin data művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadAdminData(){
    const initialTab = document.body?.dataset?.initialTab;
    const isAdminPage = initialTab === 'adminTab';
    const isXmlIndexPage = initialTab === 'xmlIndexConfigTab';
    if(isXmlIndexPage){
      await loadXmlIndexAdmin();
      return;
    }
    if(!isAdminPage || !adminApplicationName) return;
    const currentSecurityUser = getCurrentSecurityUser();
    if(currentSecurityUser && !isCurrentUserAdmin(currentSecurityUser)){
      showMessage('Az Admin felület csak ADMIN jogosultsággal érhető el.', 'warning');
      return;
    }
    await Promise.all([loadAdminSystem(), loadAdminLogging(), loadAdminConfig(), loadAdminCache(), loadXmlIndexAdmin(), loadGitHubSchemaUpdaterConfig()]);
  }
    /**
   * Kezeli vagy beköti a init esemény- és inicializációs folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function init(){
    refreshAdminButton?.addEventListener('click', loadAdminData);
    saveAdminLoggingButton?.addEventListener('click', saveAdminLogging);
    validateConfigButton?.addEventListener('click', validateAdminConfig);
    reloadConfigButton?.addEventListener('click', reloadAdminConfig);
    githubSchemaDryRunButton?.addEventListener('click', () => runGitHubSchemaUpdate(true));
    githubSchemaUpdateButton?.addEventListener('click', () => runGitHubSchemaUpdate(false));
    clearCacheButton?.addEventListener('click', clearAdminCaches);
    reloadCacheButton?.addEventListener('click', reloadAdminCaches);
    xmlIndexFormSelect?.addEventListener('change', () => { syncXmlIndexVersionOptions(); loadXmlIndexStructure(); });
    xmlIndexVersionSelect?.addEventListener('change', loadXmlIndexStructure);
    xmlIndexPartSelect?.addEventListener('change', renderXmlIndexTree);
    xmlIndexReloadButton?.addEventListener('click', () => { loadXmlIndexForms().then(loadXmlIndexStructure); });
    xmlIndexSaveButton?.addEventListener('click', saveXmlIndexConfig);
    xmlIndexTree?.addEventListener('click', onXmlIndexFieldListClick);
    xmlIndexSummary?.addEventListener('change', onXmlIndexSummaryChange);
    xmlIndexSummary?.addEventListener('click', onXmlIndexSummaryClick);
    xmlIndexFieldSearch?.addEventListener('input', renderXmlIndexTree);
    xmlIndexFieldSelect?.addEventListener('change', () => { if(xmlIndexAddFieldButton) xmlIndexAddFieldButton.disabled = !xmlIndexFieldSelect.value; });
    xmlIndexAddFieldButton?.addEventListener('click', () => addXmlIndexFieldByKey(xmlIndexFieldSelect?.value || ''));
    const returnButton = document.getElementById('xmlIndexSaveReturnButton');
    returnButton?.addEventListener('click', async () => {
      const saved = await saveXmlIndexConfig();
      if(!saved) return;
      const returnUrl = new URLSearchParams(location.search).get('returnUrl');
      if(returnUrl){
        try{
          const target = new URL(returnUrl, window.location.origin);
          const fileId = target.searchParams.get('fileId');
          const validTarget = target.origin === window.location.origin
            && target.pathname === '/form.html'
            && (!fileId || /^\d+$/.test(fileId));
          if(validTarget){
            window.location.href = target.pathname + target.search + target.hash;
            return;
          }
        }catch(_ignored){
          // Invalid return URL falls back to browser history or the fixed form page.
        }
      }
      if(window.history.length > 1) window.history.back();
      else window.location.href = '/form.html';
    });

  }

  return { init, loadAdminData };
}
