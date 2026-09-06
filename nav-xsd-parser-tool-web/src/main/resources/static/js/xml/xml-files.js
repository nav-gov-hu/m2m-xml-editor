/**
 * @module xml/xml-files
 *
 * A XML-szerkesztési és állománykezelési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

(function(){
  const messages = document.getElementById('xmlFileMessages');
  const tableBody = document.getElementById('xmlFilesTableBody');
  const uploadForm = document.getElementById('xmlUploadForm');
  const uploadFile = document.getElementById('xmlUploadFile');
  const uploadNote = document.getElementById('xmlUploadNote');
  const uploadPartnerSearch = document.getElementById('xmlUploadPartnerSearch');
  const uploadPartnerId = document.getElementById('xmlUploadPartnerId');
  const uploadPartnerSuggestions = document.getElementById('xmlUploadPartnerSuggestions');
  const uploadPartnerTaxNumber = document.getElementById('xmlUploadPartnerTaxNumber');
  const uploadPartnerName = document.getElementById('xmlUploadPartnerName');
  const uploadPartnerSaveButton = document.getElementById('xmlUploadPartnerSaveButton');
  const xmlPartnerModal = document.getElementById('xmlPartnerModal');
  const xmlPartnerForm = document.getElementById('xmlPartnerForm');
  const xmlPartnerFileId = document.getElementById('xmlPartnerFileId');
  const xmlPartnerSearch = document.getElementById('xmlPartnerSearch');
  const xmlPartnerId = document.getElementById('xmlPartnerId');
  const xmlPartnerSuggestions = document.getElementById('xmlPartnerSuggestions');
  const xmlPartnerCurrentHint = document.getElementById('xmlPartnerCurrentHint');
  const uploadSubmitButton = uploadForm ? uploadForm.querySelector('button[type=\"submit\"]') : null;
  let uploadInProgress = false;
  const refreshXmlFilesButton = document.getElementById('refreshXmlFilesButton');
  const openXmlUploadModalButton = document.getElementById('openXmlUploadModalButton');
  const toggleArchivedXmlFilesButton = document.getElementById('toggleArchivedXmlFilesButton');
  const xmlFileSearchInput = document.getElementById('xmlFileSearchInput');
  const xmlFilePartnerFilter = document.getElementById('xmlFilePartnerFilter');
  const xmlFilePartnerFilterId = document.getElementById('xmlFilePartnerFilterId');
  const xmlFilePartnerSuggestions = document.getElementById('xmlFilePartnerSuggestions');
  const xmlFileFormTypeFilter = document.getElementById('xmlFileFormTypeFilter');
  const xmlFileFormVersionFilter = document.getElementById('xmlFileFormVersionFilter');
  const xmlFileStatusFilter = document.getElementById('xmlFileStatusFilter');
  const xmlFilePageSizeSelect = document.getElementById('xmlFilePageSizeSelect');
  const xmlFilesPaginationInfo = document.getElementById('xmlFilesPaginationInfo');
  const xmlFilesPageIndicator = document.getElementById('xmlFilesPageIndicator');
  const xmlFilesFirstPageButton = document.getElementById('xmlFilesFirstPageButton');
  const xmlFilesPrevPageButton = document.getElementById('xmlFilesPrevPageButton');
  const xmlFilesNextPageButton = document.getElementById('xmlFilesNextPageButton');
  const xmlFilesLastPageButton = document.getElementById('xmlFilesLastPageButton');
  const xmlUploadModal = document.getElementById('xmlUploadModal');
  const xmlNoteModal = document.getElementById('xmlNoteModal');
  const xmlNoteForm = document.getElementById('xmlNoteForm');
  const xmlNoteFileId = document.getElementById('xmlNoteFileId');
  const xmlNoteText = document.getElementById('xmlNoteText');
  const xmlCopyModal = document.getElementById('xmlCopyModal');
  const xmlCopyForm = document.getElementById('xmlCopyForm');
  const xmlCopySourceId = document.getElementById('xmlCopySourceId');
  const xmlCopySourceHint = document.getElementById('xmlCopySourceHint');
  const xmlCopyFileName = document.getElementById('xmlCopyFileName');
  const xmlCopyFileNameState = document.getElementById('xmlCopyFileNameState');
  const xmlCopyFileNameMessage = document.getElementById('xmlCopyFileNameMessage');
  const xmlCopyNote = document.getElementById('xmlCopyNote');
  const xmlCopySubmitButton = document.getElementById('xmlCopySubmitButton');

  let allXmlFiles = [];
  let currentXmlFileMap = new Map();
  let highlightXmlFileId = null;
  let showArchivedFiles = false;
  let currentPage = 1;
  let pageSize = Number(xmlFilePageSizeSelect?.value || 20);
  let sortField = 'createdAt';
  let sortDirection = 'desc';
  let currentUserRoles = [];
  let currentUserPermissions = { canUpload: false, canEdit: false, canAdmin: false, canPhysicallyArchive: false };
  const actionMenuOriginalParents = new WeakMap();
  let copyFileNameCheckTimer = null;

    /**
   * Szinkronizálja vagy frissíti a setup partner predictive által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} input a függvény input bemeneti értéke
   * @param {*} hidden a célobjektum technikai azonosítója
   * @param {*} panel a függvény panel bemeneti értéke
   * @param {*} onSelect a függvény onSelect bemeneti értéke
   */
function setupPartnerPredictive(input, hidden, panel, onSelect){
    let timer = null;
    input?.addEventListener('input', () => {
      if(hidden) hidden.value = '';
      clearTimeout(timer);
      const q = input.value.trim();

      // A partner szűrő már gépelés közben is frissítse a listát; ne csak
      // egy javaslat kiválasztása után működjön.
      onSelect?.();

      if(q.length < 2){
        if(panel){
          panel.hidden = true;
          panel.innerHTML = '';
        }
        return;
      }
      timer = setTimeout(async () => {
        const r = await fetch(`/api/partners/suggest?q=${encodeURIComponent(q)}`);
        if(!r.ok) return;
        const items = await r.json();
        if(!panel) return;
        panel.innerHTML = items.map(p => `<button type="button" data-id="${p.id}" data-label="${escapeText(`${p.taxNumber} - ${p.name}`)}">${escapeText(`${p.taxNumber} - ${p.name}`)}</button>`).join('');
        panel.hidden = !items.length;
      }, 220);
    });
    panel?.addEventListener('click', e => {
      const b = e.target.closest('button[data-id]');
      if(!b) return;
      if(hidden) hidden.value = b.dataset.id;
      if(input) input.value = b.dataset.label;
      panel.hidden = true;
      onSelect?.();
    });
  }

    /**
   * Ellenőrzi a has any role feltételeit, és a hívó számára döntési eredményt állít elő.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} roles a függvény roles bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function hasAnyRole(...roles){
    const normalized = new Set(currentUserRoles.map(role => String(role).toUpperCase()));
    return roles.some(role => normalized.has(role) || normalized.has(`ROLE_${role}`));
  }

    /**
   * Szinkronizálja vagy frissíti a refresh permissions from roles által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function refreshPermissionsFromRoles(){
    const isAdmin = hasAnyRole('ADMIN');
    const isOperator = hasAnyRole('OPERATOR');
    currentUserPermissions = {
      canUpload: isAdmin || isOperator,
      canEdit: isAdmin || isOperator,
      canAdmin: isAdmin,
      canPhysicallyArchive: hasAnyRole('FILE_DELETE')
    };
    document.body.dataset.canEditXmlFiles = String(currentUserPermissions.canEdit);
    if(openXmlUploadModalButton){
      openXmlUploadModalButton.disabled = !currentUserPermissions.canUpload;
      openXmlUploadModalButton.title = currentUserPermissions.canUpload ? 'XML feltöltése' : 'XML feltöltése csak ADMIN vagy OPERATOR jogosultsággal érhető el';
      openXmlUploadModalButton.setAttribute('aria-disabled', String(!currentUserPermissions.canUpload));
    }
  }

    /**
   * Betölti vagy lekéri a load current user for permissions művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadCurrentUserForPermissions(){
    try{
      const response = await fetch('/api/security/current-user', { cache: 'no-store', credentials: 'same-origin' });
      if(!response.ok) throw new Error('current-user unavailable');
      const user = await response.json();
      currentUserRoles = Array.isArray(user.roles) ? user.roles : [];
    }catch(_ignored){
      currentUserRoles = [];
    }
    refreshPermissionsFromRoles();
  }

    /**
   * Megjeleníti vagy újrarendereli a show message állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} type a függvény type bemeneti értéke
   * @param {*} text a függvény text bemeneti értéke
   */
function showMessage(type, text){
    const messageText = String(text || '').trim();
    if(messages){
      messages.innerHTML = '';
    }
    if(!messageText) return;
    if(window.navShowToast){
      window.navShowToast(messageText, type || 'info');
      return;
    }
    if(messages){
      const div = document.createElement('div');
      const normalizedType = type === 'error' ? 'error' : type === 'warning' ? 'warning' : type === 'success' ? 'success' : 'info';
      div.className = `message ${normalizedType} message-${normalizedType}`;
      div.textContent = messageText;
      messages.appendChild(div);
    }
  }

    /**
   * Megjeleníti vagy újrarendereli a show toast állapotát a felhasználói felületen.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} text a függvény text bemeneti értéke
   * @param {*} type a függvény type bemeneti értéke
   */
function showToast(text, type){
    if(window.navShowToast){
      window.navShowToast(String(text || ''), type || 'info');
      return;
    }
    showMessage(type || 'info', text || '');
  }


    /**
   * Megjeleníti vagy újrarendereli a show deferred xml files message állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function showDeferredXmlFilesMessage(){
    let raw = null;
    try{
      raw = sessionStorage.getItem('navXmlFilesMessage');
      if(raw) sessionStorage.removeItem('navXmlFilesMessage');
    }catch(_ignored){
      raw = null;
    }
    if(!raw) return;
    try{
      const parsed = JSON.parse(raw);
      showMessage(parsed.type || 'info', parsed.text || '');
    }catch(_ignored){
      showMessage('info', raw);
    }
  }

    /**
   * Betölti vagy lekéri a read error művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   * @param {*} response a backend-hívás feldolgozandó válasza
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function readError(response){
    try{
      const data = await response.json();
      return data.message || data.error || response.statusText;
    }catch{
      return response.statusText || 'Ismeretlen hiba';
    }
  }

    /**
   * Feldolgozza a format date bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function formatDate(value){
    if(!value) return '-';
    const d = new Date(value);
    if(Number.isNaN(d.getTime())) return value;
    return d.toLocaleString('hu-HU');
  }

    /**
   * A <code>dateValue</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function dateValue(value){
    if(!value) return 0;
    const d = new Date(value);
    if(Number.isNaN(d.getTime())) return 0;
    return d.getTime();
  }

    /**
   * A <code>escapeText</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function escapeText(value){
    return String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
  }

    /**
   * Feldolgozza a normalize boolean bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function normalizeBoolean(value){
    return value === true || value === 'true';
  }

    /**
   * Ellenőrzi a is openable xml file feltételeit, és a hívó számára döntési eredményt állít elő.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function isOpenableXmlFile(file){
    const status = String(file?.status || '').toUpperCase();
    const resolutionStatus = String(file?.resolutionStatus || '').toUpperCase();
    if(status === 'REGISTERED') return true; // megnyitáskor a backend újra megkísérli az XSD-erőforrások feloldását
    return status === 'READY' && (!resolutionStatus || resolutionStatus === 'RESOLVED');
  }

    /**
   * A <code>baseXmlStatus</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function baseXmlStatus(file){
    return String(file?.status || file?.displayStatus || '-').toUpperCase();
  }

    /**
   * Megjeleníti vagy újrarendereli a display xml status állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function displayXmlStatus(file){
    return baseXmlStatus(file);
  }

    /**
   * Ellenőrzi a has xsd errors feltételeit, és a hívó számára döntési eredményt állít elő.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function hasXsdErrors(file){
    const result = String(file?.latestXsdResultStatus || '').toUpperCase();
    const status = String(file?.latestXsdStatus || '').toUpperCase();
    const errors = Number(file?.latestXsdErrorCount || 0);
    return errors > 0 || result === 'INVALID' || result === 'FAILED' || status === 'FAILED';
  }

    /**
   * Feldolgozza a format xsd status label bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function formatXsdStatusLabel(file){
    const result = String(file?.latestXsdResultStatus || '').toUpperCase();
    const status = String(file?.latestXsdStatus || '').toUpperCase();
    const errors = Number(file?.latestXsdErrorCount || 0);
    if(hasXsdErrors(file)){
      return `XSD hibás${errors ? ` (${errors} hiba)` : ''}`;
    }
    if(result === 'VALID') return 'XSD rendben';
    if(status) return `XSD státusz: ${status}`;
    return 'Nincs XSD eredmény';
  }

    /**
   * A <code>statusIconMeta</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function statusIconMeta(file){
    const status = baseXmlStatus(file);
    switch(status){
      case 'READY': return { icon: '✓', className: 'ready', label: 'READY - használatra kész' };
      case 'REGISTERED': return { icon: '●', className: 'registered', label: 'REGISTERED - regisztrált' };
      case 'UNKNOWN': return { icon: '?', className: 'unknown', label: 'UNKNOWN - ismeretlen' };
      case 'ARCHIVED': return { icon: '▣', className: 'archived', label: 'ARCHIVED - archivált' };
      case 'FAILED': return { icon: '!', className: 'failed', label: 'FAILED - hibás' };
      case 'NOT_RESOLVED': return { icon: '⚠', className: 'warning', label: 'NOT_RESOLVED - nincs teljesen feloldva' };
      default: return { icon: '•', className: 'default', label: status || '-' };
    }
  }

    /**
   * Megjeleníti vagy újrarendereli a render status cell állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function renderStatusCell(file){
    const meta = statusIconMeta(file);
    const locked = normalizeBoolean(file?.locked);
    const revisionCount = Number(file?.revisionCount || 0);
    const hasChanges = revisionCount > 0;
    const titleParts = [meta.label];
    const xsdErrors = hasXsdErrors(file);
    if(xsdErrors) titleParts.push(formatXsdStatusLabel(file));
    if(hasChanges) titleParts.push(`${revisionCount} mentett változás`);
    if(locked) titleParts.push('zárolva');
    const title = titleParts.join('; ');
    return `<span class="xml-status-cell" title="${escapeText(title)}" aria-label="${escapeText(title)}">` +
      `<span class="xml-status-icon xml-status-${escapeText(meta.className)}" aria-hidden="true">${escapeText(meta.icon)}</span>` +
      (xsdErrors ? `<span class="xml-status-xsd-error-icon" aria-hidden="true">!</span>` : '') +
      (hasChanges ? `<span class="xml-status-change-icon" aria-hidden="true">Δ</span>` : '') +
      (locked ? '<span class="xml-status-lock-icon" aria-hidden="true">🔒</span>' : '') +
      `<span class="sr-only">${escapeText(title)}</span>` +
      `</span>`;
  }

    /**
   * Ellenőrzi a has resolver info feltételeit, és a hívó számára döntési eredményt állít elő.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function hasResolverInfo(file){
    return !!(file.rootElement || file.namespaceUri || file.schemaLocation || file.noNamespaceSchemaLocation ||
      file.xsdPath || file.uiModelPath || file.xpathRulesPath || file.resolutionStatus || file.resolutionMessage ||
      file.formType || file.formVersion || file.filePath);
  }

    /**
   * A <code>uniqueSorted</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} values a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function uniqueSorted(values){
    return Array.from(new Set(values.filter(value => value !== null && value !== undefined && String(value).trim() !== '')
      .map(value => String(value).trim()))).sort((a, b) => a.localeCompare(b, 'hu'));
  }

    /**
   * A <code>preserveSelectValue</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} select a függvény select bemeneti értéke
   * @param {*} options a művelet opcionális beállításai
   * @param {*} label a függvény label bemeneti értéke
   */
function preserveSelectValue(select, options, label){
    if(!select) return;
    const current = select.value;
    select.innerHTML = `<option value="">${escapeText(label)}</option>` +
      options.map(value => `<option value="${escapeText(value)}">${escapeText(value)}</option>`).join('');
    if(options.includes(current)) select.value = current;
  }

    /**
   * Szinkronizálja vagy frissíti a refresh version filter options által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function refreshVersionFilterOptions(){
    if(!xmlFileFormVersionFilter) return;
    const selectedFormType = xmlFileFormTypeFilter?.value || '';
    if(!selectedFormType){
      xmlFileFormVersionFilter.innerHTML = '<option value="">Előbb válassz űrlapt</option>';
      xmlFileFormVersionFilter.value = '';
      xmlFileFormVersionFilter.disabled = true;
      return;
    }
    xmlFileFormVersionFilter.disabled = false;
    const versions = uniqueSorted(allXmlFiles
      .filter(file => String(file.formType || '') === selectedFormType)
      .map(file => file.formVersion));
    preserveSelectValue(xmlFileFormVersionFilter, versions, 'Minden verzió');
  }

    /**
   * Szinkronizálja vagy frissíti a refresh filter options által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} files a függvény files bemeneti értéke
   */
function refreshFilterOptions(files){
    preserveSelectValue(xmlFileFormTypeFilter, uniqueSorted(files.map(file => file.formType)), 'Minden űrlap');
    preserveSelectValue(xmlFileStatusFilter, uniqueSorted(files.map(file => displayXmlStatus(file))), 'Minden státusz');
    refreshVersionFilterOptions();
  }

    /**
   * Feldolgozza a normalize filter text bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function normalizeFilterText(value){
    return String(value ?? '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLocaleLowerCase('hu-HU')
      .trim();
  }

    /**
   * A <code>filteredFiles</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function filteredFiles(){
    const q = normalizeFilterText(xmlFileSearchInput?.value || '');
    const partnerQuery = normalizeFilterText(xmlFilePartnerFilter?.value || '');
    const formType = xmlFileFormTypeFilter?.value || '';
    const formVersion = xmlFileFormVersionFilter?.value || '';
    const status = xmlFileStatusFilter?.value || '';
    const partnerId = String(xmlFilePartnerFilterId?.value || '');
    return allXmlFiles.filter(file => {
      if(formType && String(file.formType || '') !== formType) return false;
      if(formVersion && String(file.formVersion || '') !== formVersion) return false;
      if(status && displayXmlStatus(file) !== status) return false;

      if(partnerId){
        if(String(file.partnerId || '') !== partnerId) return false;
      }else if(partnerQuery){
        const partnerHaystack = normalizeFilterText(`${file.partnerTaxNumber || ''} ${file.partnerName || ''}`);
        if(!partnerHaystack.includes(partnerQuery)) return false;
      }

      if(q){
        // Az állományszűrő a tárolt fájlnév, az eredeti fájlnév és az
        // állomány aktuális felhasználói megjegyzése között egyaránt keres.
        const haystack = normalizeFilterText(`${file.fileName || ''} ${file.originalFileName || ''} ${file.userNote || ''}`);
        if(!haystack.includes(q)) return false;
      }
      return true;
    });
  }

    /**
   * A <code>sortedFiles</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} files a függvény files bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function sortedFiles(files){
    const direction = sortDirection === 'asc' ? 1 : -1;
    return [...files].sort((a, b) => {
      if(sortField === 'createdAt' || sortField === 'updatedAt'){
        const av = dateValue(a[sortField]);
        const bv = dateValue(b[sortField]);
        if(av !== bv) return (av - bv) * direction;
        return ((Number(a.id) || 0) - (Number(b.id) || 0)) * direction;
      }
      const av = String(a[sortField] || '');
      const bv = String(b[sortField] || '');
      return av.localeCompare(bv, 'hu') * direction;
    });
  }

    /**
   * A <code>currentFilteredSortedFiles</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function currentFilteredSortedFiles(){
    return sortedFiles(filteredFiles());
  }

    /**
   * A <code>totalPages</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} total a függvény total bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function totalPages(total){
    return Math.max(1, Math.ceil(total / pageSize));
  }

    /**
   * Szinkronizálja vagy frissíti a update sort indicators által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function updateSortIndicators(){
    document.querySelectorAll('.xml-files-sort-button').forEach(button => {
      const indicator = button.querySelector('span');
      const active = button.dataset.sortField === sortField;
      button.classList.toggle('is-active', active);
      button.setAttribute('aria-sort', active ? (sortDirection === 'asc' ? 'ascending' : 'descending') : 'none');
      if(indicator) indicator.textContent = active ? (sortDirection === 'asc' ? '▲' : '▼') : '';
    });
  }

    /**
   * A <code>paginationWindow</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} current a függvény current bemeneti értéke
   * @param {*} pages az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
   * @returns {*} a feldolgozás eredménye
   */
function paginationWindow(current, pages){
    if(pages <= 7){
      return Array.from({ length: pages }, (_, index) => index + 1);
    }
    const set = new Set([1, pages, current, current - 1, current + 1]);
    if(current <= 3){
      [2, 3, 4].forEach(value => set.add(value));
    }
    if(current >= pages - 2){
      [pages - 3, pages - 2, pages - 1].forEach(value => set.add(value));
    }
    return Array.from(set).filter(value => value >= 1 && value <= pages).sort((a, b) => a - b);
  }

    /**
   * Megjeleníti vagy újrarendereli a render pagination numbers állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} pages az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
   */
function renderPaginationNumbers(pages){
    if(!xmlFilesPageIndicator) return;
    const numbers = paginationWindow(currentPage, pages);
    const html = [];
    let previous = 0;
    numbers.forEach(page => {
      if(previous && page - previous > 1){
        html.push('<span class="xml-files-page-gap" aria-hidden="true">…</span>');
      }
      html.push(`<button type="button" class="xml-files-page-number ${page === currentPage ? 'is-active' : ''}" data-page="${page}" ${page === currentPage ? 'aria-current="page"' : ''}>${page}</button>`);
      previous = page;
    });
    xmlFilesPageIndicator.innerHTML = html.join('');
    xmlFilesPageIndicator.querySelectorAll('.xml-files-page-number').forEach(button => {
      button.addEventListener('click', () => gotoPage(Number(button.dataset.page || 1)));
    });
  }

    /**
   * Szinkronizálja vagy frissíti a update pagination által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} total a függvény total bemeneti értéke
   */
function updatePagination(total){
    const pages = totalPages(total);
    if(currentPage > pages) currentPage = pages;
    if(currentPage < 1) currentPage = 1;
    const from = total === 0 ? 0 : ((currentPage - 1) * pageSize) + 1;
    const to = Math.min(total, currentPage * pageSize);
    if(xmlFilesPaginationInfo) xmlFilesPaginationInfo.textContent = `${from}-${to} / ${total} állomány`;
    renderPaginationNumbers(pages);
    const atFirst = currentPage <= 1;
    const atLast = currentPage >= pages;
    [xmlFilesFirstPageButton, xmlFilesPrevPageButton].forEach(button => { if(button) button.disabled = atFirst; });
    [xmlFilesNextPageButton, xmlFilesLastPageButton].forEach(button => { if(button) button.disabled = atLast; });
  }


    /**
   * A <code>actionMenuItem</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} className a feloldáshoz vagy megjelenítéshez használt név
   * @param {*} actionClass a függvény actionClass bemeneti értéke
   * @param {*} id a célobjektum technikai azonosítója
   * @param {*} icon a függvény icon bemeneti értéke
   * @param {*} label a függvény label bemeneti értéke
   * @param {*} extraAttributes a függvény extraAttributes bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function actionMenuItem(className, actionClass, id, icon, label, extraAttributes){
    const attrs = extraAttributes || '';
    return `<button type="button" class="xml-file-menu-item ${className} ${actionClass}" data-id="${escapeText(id)}" ${attrs}><span class="xml-file-menu-icon" aria-hidden="true">${icon}</span><span>${escapeText(label)}</span></button>`;
  }

    /**
   * A <code>restoreActionMenu</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} menu a függvény menu bemeneti értéke
   */
function restoreActionMenu(menu){
    if(!menu) return;
    const originalParent = actionMenuOriginalParents.get(menu);
    if(originalParent && originalParent.isConnected && menu.parentElement !== originalParent){
      originalParent.appendChild(menu);
    }
    menu.classList.remove('is-floating');
    menu.style.left = '';
    menu.style.top = '';
  }

    /**
   * Elrejti vagy lezárja a close action menus felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   */
function closeActionMenus(){
    document.querySelectorAll('.xml-file-menu').forEach(menu => {
      menu.hidden = true;
      restoreActionMenu(menu);
    });
  }

    /**
   * A <code>prepareFloatingActionMenu</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} button a függvény button bemeneti értéke
   * @param {*} menu a függvény menu bemeneti értéke
   */
function prepareFloatingActionMenu(button, menu){
    if(!button || !menu) return;
    if(!actionMenuOriginalParents.has(menu) && menu.parentElement){
      actionMenuOriginalParents.set(menu, menu.parentElement);
    }
    if(menu.parentElement !== document.body){
      document.body.appendChild(menu);
    }
    menu.classList.add('is-floating');
  }

    /**
   * A <code>positionActionMenu</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} button a függvény button bemeneti értéke
   * @param {*} menu a függvény menu bemeneti értéke
   */
function positionActionMenu(button, menu){
    if(!button || !menu) return;
    prepareFloatingActionMenu(button, menu);
    const margin = 12;
    const gap = 6;
    const rect = button.getBoundingClientRect();

    const width = Math.max(menu.offsetWidth || 240, 240);
    const height = Math.max(menu.offsetHeight || 0, 0);
    const viewportWidth = window.innerWidth || document.documentElement.clientWidth || 0;
    const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 0;

    const left = Math.max(margin, Math.min(viewportWidth - width - margin, rect.right - width));
    const spaceBelow = viewportHeight - rect.bottom - margin;
    const spaceAbove = rect.top - margin;
    let top;

    if(height && spaceBelow < height + gap && spaceAbove > spaceBelow){
      top = rect.top - height - gap;
    } else {
      top = rect.bottom + gap;
    }

    top = Math.max(margin, Math.min(viewportHeight - height - margin, top));
    menu.style.left = `${left}px`;
    menu.style.top = `${top}px`;
  }

    /**
   * Megjeleníti vagy újrarendereli a render actions állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function renderActions(file){
    const items = [];
    const canEdit = currentUserPermissions.canEdit;
    if(!file.archived && isOpenableXmlFile(file)){
      if(canEdit){
        items.push(actionMenuItem('open-edit', 'open-xml-file-button', file.id, '↗', 'Megnyitás szerkesztésre', 'data-readonly="false"'));
      }
      items.push(actionMenuItem('open-readonly', 'open-xml-file-button', file.id, '◉', 'Megnyitás csak olvasásra', 'data-readonly="true"'));
      if(normalizeBoolean(file.locked)){
        items.push(actionMenuItem('close-session', 'close-xml-file-button', file.id, '⏏', 'Munkamenet lezárása'));
      }
    }
    if(normalizeBoolean(file.locked)){
      items.push(actionMenuItem('lock-info', 'lock-info-button', file.id, '🔒', 'Zárolási információk'));
    }
    items.push(actionMenuItem('download', 'download-xml-file-button', file.id, '⇩', 'Letöltés'));
    if(hasXsdErrors(file)){
      items.push(actionMenuItem('xsd-errors', 'xsd-errors-button', file.id, '!', 'XSD hibák'));
    }
    if(!file.archived && canEdit){
      items.push(actionMenuItem('copy', 'copy-xml-file-button', file.id, '⧉', 'Másolás'));
      items.push(actionMenuItem('change-partner', 'change-partner-xml-file-button', file.id, '👤', 'Partner módosítása'));
    }
    if(hasResolverInfo(file)){
      items.push(actionMenuItem('resolver-info', 'resolver-info-button', file.id, 'i', 'Resolver információ'));
    }
    items.push(actionMenuItem('changes-info', 'changes-info-button', file.id, '≋', 'Változások'));
    if(canEdit){
      items.push(actionMenuItem('edit-note', 'edit-note-button', file.id, '✎', 'Megjegyzés szerkesztése'));
    }
    if(!file.archived && canEdit){
      items.push(actionMenuItem('archive', 'archive-xml-file-button', file.id, '⌧', 'Archiválás'));
    }
    if(!file.archived && currentUserPermissions.canPhysicallyArchive){
      items.push(actionMenuItem('physical-archive', 'physical-archive-xml-file-button', file.id, '▤', 'Fizikai archiválás'));
    }
    if(currentUserPermissions.canAdmin){
      items.push(actionMenuItem('permanent-delete', 'permanent-delete-xml-file-button', file.id, '🗑', 'Végleges törlés'));
    }
    if(!items.length){
      items.push('<button type="button" class="xml-file-menu-item" disabled><span class="xml-file-menu-icon" aria-hidden="true">·</span><span>Nincs elérhető művelet</span></button>');
    }
    return `
      <div class="xml-file-action-menu">
        <button type="button" class="xml-file-kebab-button" data-action-menu-toggle="${escapeText(file.id)}" aria-label="Műveletek megnyitása" title="Műveletek">⋮</button>
        <div class="xml-file-menu" data-action-menu="${escapeText(file.id)}" hidden>
          ${items.join('')}
        </div>
      </div>`;
  }

    /**
   * Feldolgozza a build resolver details bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function buildResolverDetails(file){
    const rows = [
      { label: 'Fájlnév', value: file.fileName },
      { label: 'Teljes útvonal', value: file.filePath },
      { label: 'Root elem', value: file.rootElement },
      { label: 'Namespace URI', value: file.namespaceUri },
      { label: 'schemaLocation', value: file.schemaLocation },
      { label: 'noNamespaceSchemaLocation', value: file.noNamespaceSchemaLocation },
      { label: 'Űrlap', value: file.formType },
      { label: 'Verzió', value: file.formVersion },
      { label: 'Elsődleges XSD', value: file.xsdPath, resource: true, exists: file.xsdExists },
      { label: 'UI model', value: file.uiModelPath, resource: true, exists: file.uiModelExists },
      { label: 'XPath szabályok', value: file.xpathRulesPath, resource: true, exists: file.xpathRulesExists },
      { label: 'Resolver státusz', value: file.resolutionStatus },
      { label: 'Resolver üzenet', value: file.resolutionMessage }
    ];
    if(file.storedFormType !== undefined){
      rows.push(
        { label: 'Tárolt űrlap', value: file.storedFormType },
        { label: 'Tárolt verzió', value: file.storedFormVersion },
        { label: 'Tárolt elsődleges XSD', value: file.storedXsdPath },
        { label: 'Tárolt UI model', value: file.storedUiModelPath },
        { label: 'Tárolt XPath szabályok', value: file.storedXpathRulesPath }
      );
    }
    return rows.map((row) => {
      const missing = row.resource && (!row.value || row.exists === false);
      const displayValue = row.value || (row.resource ? 'Nincs feloldott elérési út' : '-');
      const state = missing ? '<span class="resolver-resource-missing">A fájl nem létezik</span>' : '';
      return `
        <div class="resolver-info-row${missing ? ' resolver-info-row--missing' : ''}">
          <dt>${escapeText(row.label)}</dt>
          <dd>${escapeText(displayValue)}${state}</dd>
        </div>`;
    }).join('');
  }


    /**
   * A <code>ensureXsdErrorsModal</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   * @returns {*} a feldolgozás eredménye
   */
function ensureXsdErrorsModal(){
    let modal = document.getElementById('xmlXsdErrorsModal');
    if(modal) return modal;
    modal = document.createElement('div');
    modal.id = 'xmlXsdErrorsModal';
    modal.className = 'xml-xsd-errors-modal';
    modal.hidden = true;
    modal.innerHTML = `
      <div class="xml-xsd-errors-backdrop" data-close-xsd-errors="true"></div>
      <section class="xml-xsd-errors-dialog" role="dialog" aria-modal="true" aria-labelledby="xmlXsdErrorsTitle">
        <button type="button" class="xml-xsd-errors-close" data-close-xsd-errors="true" aria-label="Bezárás">×</button>
        <p class="eyebrow">Űrlapállománykezelés</p>
        <h2 id="xmlXsdErrorsTitle">XSD hibák</h2>
        <p id="xmlXsdErrorsSubtitle" class="xml-xsd-errors-subtitle"></p>
        <div id="xmlXsdErrorsContent" class="xml-xsd-errors-content"></div>
      </section>`;
    document.body.appendChild(modal);
    modal.addEventListener('click', (event) => {
      if(event.target && event.target.dataset && event.target.dataset.closeXsdErrors){
        closeXsdErrorsInfo();
      }
    });
    return modal;
  }

    /**
   * Elrejti vagy lezárja a close xsd errors info felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   */
function closeXsdErrorsInfo(){
    const modal = document.getElementById('xmlXsdErrorsModal');
    if(modal) modal.hidden = true;
  }

    /**
   * Megjeleníti vagy újrarendereli a render xsd error rows állapotát a felhasználói felületen.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   * @param {*} errors a függvény errors bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function renderXsdErrorRows(errors){
    if(!Array.isArray(errors) || !errors.length){
      return '<div class="xml-xsd-errors-empty">Nem található részletes XSD hibalista az utolsó eredményhez.</div>';
    }
    return `
      <div class="xml-xsd-errors-table-wrap">
        <table class="xml-xsd-errors-table">
          <thead>
            <tr>
              <th>Súlyosság</th>
              <th>Sor</th>
              <th>Oszlop</th>
              <th>Kód</th>
              <th>Üzenet</th>
            </tr>
          </thead>
          <tbody>
            ${errors.map(error => `
              <tr>
                <td>${escapeText(error.severity || '-')}</td>
                <td>${escapeText(error.lineNumber ?? '-')}</td>
                <td>${escapeText(error.columnNumber ?? '-')}</td>
                <td>${escapeText(error.errorCode || error.code || '-')}</td>
                <td>${escapeText(error.errorMessage || error.message || '-')}</td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;
  }

    /**
   * A <code>openXsdErrorsInfo</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function openXsdErrorsInfo(file){
    if(!file || !hasXsdErrors(file)) return;
    const requestId = file.latestXsdRequestId;
    const modal = ensureXsdErrorsModal();
    const subtitle = modal.querySelector('#xmlXsdErrorsSubtitle');
    const content = modal.querySelector('#xmlXsdErrorsContent');
    if(subtitle){
      subtitle.textContent = `${file.fileName || 'Űrlapállomány'} · ${formatXsdStatusLabel(file)}`;
    }
    if(content){
      content.innerHTML = '<div class="xml-xsd-errors-empty">XSD hibák betöltése...</div>';
    }
    modal.hidden = false;
    if(!requestId){
      if(content){
        content.innerHTML = '<div class="message warning message-warning">Az XSD hiba jelzett, de nem található hozzá validációs request azonosító.</div>';
      }
      return;
    }
    try{
      const response = await fetch(`/api/xsd-validation/requests/${encodeURIComponent(requestId)}/errors`, { cache: 'no-store', credentials: 'same-origin' });
      if(!response.ok) throw new Error(await readError(response));
      const errors = await response.json() || [];
      if(content){
        content.innerHTML = `
          <div class="xml-xsd-errors-summary">
            <span>Request ID: <strong>${escapeText(requestId)}</strong></span>
            <span>Hibák: <strong>${escapeText(file.latestXsdErrorCount ?? 0)}</strong></span>
            <span>Figyelmeztetések: <strong>${escapeText(file.latestXsdWarningCount ?? 0)}</strong></span>
            <span>Befejezés: <strong>${escapeText(formatDate(file.latestXsdFinishedAt))}</strong></span>
          </div>
          ${renderXsdErrorRows(errors)}`;
      }
    }catch(error){
      if(content) content.innerHTML = `<div class="message error message-error">${escapeText(error.message)}</div>`;
      showToast(error.message, 'error');
    }
  }

    /**
   * A <code>ensureResolverModal</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function ensureResolverModal(){
    let modal = document.getElementById('resolverInfoModal');
    if(modal) return modal;
    modal = document.createElement('div');
    modal.id = 'resolverInfoModal';
    modal.className = 'resolver-info-modal';
    modal.hidden = true;
    modal.innerHTML = `
      <div class="resolver-info-backdrop" data-close-resolver-info="true"></div>
      <section class="resolver-info-dialog" role="dialog" aria-modal="true" aria-labelledby="resolverInfoTitle">
        <button type="button" class="resolver-info-close" data-close-resolver-info="true" aria-label="Bezárás">×</button>
        <p class="eyebrow">Űrlapállománykezelés</p>
        <h2 id="resolverInfoTitle">Resolver információ</h2>
        <dl id="resolverInfoContent" class="resolver-info-content"></dl>
      </section>`;
    document.body.appendChild(modal);
    modal.addEventListener('click', (event) => {
      if(event.target && event.target.dataset && event.target.dataset.closeResolverInfo){
        closeResolverInfo();
      }
    });
    return modal;
  }

    /**
   * A <code>openResolverInfo</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function openResolverInfo(file){
    const modal = ensureResolverModal();
    const content = modal.querySelector('#resolverInfoContent');
    if(content){
      content.innerHTML = '<div class="resolver-info-row"><dt>Állapot</dt><dd>Resolver adatok friss feloldása...</dd></div>';
    }
    modal.hidden = false;
    try{
      const response = await fetch(`/api/xml-files/${encodeURIComponent(file.id)}/resolver-info`, {
        cache: 'no-store',
        credentials: 'same-origin'
      });
      if(!response.ok) throw new Error(await readError(response));
      const resolved = await response.json();
      console.groupCollapsed('[RESOLVER-INFO-UI] Frissen feloldott resolver információ');
      console.log('[RESOLVER-INFO-UI] listRecord', file || {});
      console.log('[RESOLVER-INFO-UI] refreshedResolverData', resolved || {});
      console.groupEnd();
      if(content){
        content.innerHTML = buildResolverDetails(resolved || {});
      }
    }catch(error){
      console.error('[RESOLVER-INFO-UI] Resolver információ frissítése sikertelen', error);
      if(content){
        content.innerHTML = `<div class="message error message-error">${escapeText(error.message)}</div>${buildResolverDetails(file || {})}`;
      }
      showToast(error.message, 'error');
    }
  }

    /**
   * Elrejti vagy lezárja a close resolver info felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function closeResolverInfo(){
    const modal = document.getElementById('resolverInfoModal');
    if(modal) modal.hidden = true;
  }

    /**
   * Feldolgozza a build lock details bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function buildLockDetails(file){
    const rows = [
      ['Fájlnév', file?.fileName],
      ['Zárolási állapot', displayXmlStatus(file)],
      ['Zárolta', file?.lockedBy],
      ['Aktív XML session ID', file?.activeSessionId],
      ['Lock böngésző session ID', file?.lockBrowserSessionId],
      ['Zárolás kezdete', formatDate(file?.lockedAt)],
      ['Zárolás lejárata', formatDate(file?.lockExpiresAt)],
      ['Kliens IP', file?.lockClientIp],
      ['Böngésző / user agent', file?.lockUserAgent]
    ];
    return rows.map(([label, value]) => `
      <div class="resolver-info-row">
        <dt>${escapeText(label)}</dt>
        <dd>${escapeText(value || '-')}</dd>
      </div>`).join('');
  }

    /**
   * A <code>ensureLockInfoModal</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function ensureLockInfoModal(){
    let modal = document.getElementById('lockInfoModal');
    if(modal) return modal;
    modal = document.createElement('div');
    modal.id = 'lockInfoModal';
    modal.className = 'resolver-info-modal lock-info-modal';
    modal.hidden = true;
    modal.innerHTML = `
      <div class="resolver-info-backdrop" data-close-lock-info="true"></div>
      <section class="resolver-info-dialog" role="dialog" aria-modal="true" aria-labelledby="lockInfoTitle">
        <button type="button" class="resolver-info-close" data-close-lock-info="true" aria-label="Bezárás">×</button>
        <p class="eyebrow">Űrlapállománykezelés</p>
        <h2 id="lockInfoTitle">Zárolási információk</h2>
        <dl id="lockInfoContent" class="resolver-info-content"></dl>
      </section>`;
    document.body.appendChild(modal);
    modal.addEventListener('click', (event) => {
      if(event.target && event.target.dataset && event.target.dataset.closeLockInfo){
        closeLockInfo();
      }
    });
    return modal;
  }

    /**
   * A <code>openLockInfo</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   */
function openLockInfo(file){
    const modal = ensureLockInfoModal();
    const content = modal.querySelector('#lockInfoContent');
    if(content){
      content.innerHTML = buildLockDetails(file || {});
    }
    modal.hidden = false;
  }

    /**
   * Elrejti vagy lezárja a close lock info felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function closeLockInfo(){
    const modal = document.getElementById('lockInfoModal');
    if(modal) modal.hidden = true;
  }

    /**
   * Feldolgozza a format revision save type bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function formatRevisionSaveType(value){
    const type = String(value || '').toUpperCase();
    if(type === 'NEW_VERSION') return 'Új verzióként mentés';
    if(type === 'OVERWRITE') return 'Felülírás';
    return value || '-';
  }

    /**
   * Feldolgozza a format diff change type bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function formatDiffChangeType(value){
    const type = String(value || '').toUpperCase();
    if(type === 'ADDED') return 'Hozzáadva';
    if(type === 'CHANGED') return 'Módosítva';
    if(type === 'REMOVED') return 'Törölve';
    return value || '-';
  }

    /**
   * A <code>ensureChangesModal</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function ensureChangesModal(){
    let modal = document.getElementById('xmlChangesModal');
    if(modal) return modal;
    modal = document.createElement('div');
    modal.id = 'xmlChangesModal';
    modal.className = 'resolver-info-modal xml-changes-modal';
    modal.hidden = true;
    modal.innerHTML = `
      <div class="resolver-info-backdrop" data-close-xml-changes="true"></div>
      <section class="resolver-info-dialog xml-changes-dialog" role="dialog" aria-modal="true" aria-labelledby="xmlChangesTitle">
        <button type="button" class="resolver-info-close" data-close-xml-changes="true" aria-label="Bezárás">×</button>
        <p class="eyebrow">Űrlapállománykezelés</p>
        <h2 id="xmlChangesTitle">Változások</h2>
        <p id="xmlChangesSubtitle" class="xml-changes-subtitle"></p>
        <div id="xmlChangesContent" class="xml-changes-content"></div>
      </section>`;
    document.body.appendChild(modal);
    modal.addEventListener('click', (event) => {
      if(event.target && event.target.dataset && event.target.dataset.closeXmlChanges){
        closeChangesInfo();
      }
    });
    return modal;
  }

    /**
   * Elrejti vagy lezárja a close changes info felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function closeChangesInfo(){
    const modal = document.getElementById('xmlChangesModal');
    if(modal) modal.hidden = true;
  }

    /**
   * Megjeleníti vagy újrarendereli a render revision summary állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} revision a függvény revision bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function renderRevisionSummary(revision){
    const note = revision?.userNote ? `<div class="xml-changes-note">${escapeText(revision.userNote)}</div>` : '';
    return `
      <button type="button" class="xml-revision-card" data-revision-id="${escapeText(revision.id)}">
        <span class="xml-revision-main">
          <strong>Revízió ${escapeText(revision.revisionNo || revision.id || '-')}</strong>
          <span>${escapeText(formatRevisionSaveType(revision.saveType))}</span>
        </span>
        <span class="xml-revision-meta">
          <span>${escapeText(formatDate(revision.createdAt))}</span>
          <span>${escapeText(revision.createdBy || '-')}</span>
          <span>${escapeText(revision.changeCount ?? 0)} változás</span>
        </span>
        ${note}
      </button>`;
  }

    /**
   * Megjeleníti vagy újrarendereli a render diff entries állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} entries a függvény entries bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function renderDiffEntries(entries){
    if(!Array.isArray(entries) || !entries.length){
      return '<div class="xml-changes-empty">Ehhez a revízióhoz nincs diff bejegyzés.</div>';
    }
    return `
      <div class="xml-diff-table-wrap">
        <table class="xml-diff-table">
          <thead>
            <tr>
              <th>Típus</th>
              <th>XML path</th>
              <th>Régi érték</th>
              <th>Új érték</th>
            </tr>
          </thead>
          <tbody>
            ${entries.map(entry => `
              <tr class="xml-diff-row xml-diff-${escapeText(String(entry.changeType || '').toLowerCase())}">
                <td>${escapeText(formatDiffChangeType(entry.changeType))}</td>
                <td title="${escapeText(entry.xmlPath || '')}">${escapeText(entry.xmlPath || '-')}</td>
                <td title="${escapeText(entry.oldValue || '')}">${escapeText(entry.oldValue || '-')}</td>
                <td title="${escapeText(entry.newValue || '')}">${escapeText(entry.newValue || '-')}</td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;
  }


    /**
   * Feldolgozza a normalize file path for compare bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function normalizeFilePathForCompare(value){
    return String(value || '').replace(/\\/g, '/').toLowerCase();
  }

    /**
   * A <code>baseNameFromPath</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function baseNameFromPath(value){
    const normalized = String(value || '').replace(/\\/g, '/');
    return normalized.split('/').filter(Boolean).pop() || normalized;
  }

    /**
   * Feloldja a find xml file by path or name eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
   * @returns {*} a feldolgozás eredménye
   */
function findXmlFileByPathOrName(path){
    const normalizedPath = normalizeFilePathForCompare(path);
    const baseName = baseNameFromPath(path).toLowerCase();
    return allXmlFiles.find(file => normalizeFilePathForCompare(file.filePath) === normalizedPath)
      || allXmlFiles.find(file => String(file.fileName || '').toLowerCase() === baseName);
  }

    /**
   * Eltávolítja vagy alaphelyzetbe állítja a reset xml file list filters művelethez tartozó kliensoldali állapotot.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function resetXmlFileListFilters(){
    if(xmlFileSearchInput) xmlFileSearchInput.value = '';
    if(xmlFileFormTypeFilter) xmlFileFormTypeFilter.value = '';
    if(xmlFileFormVersionFilter){
      xmlFileFormVersionFilter.value = '';
      xmlFileFormVersionFilter.disabled = true;
    }
    if(xmlFileStatusFilter) xmlFileStatusFilter.value = '';
    refreshFilterOptions(allXmlFiles);
  }

    /**
   * A <code>highlightXmlFileRow</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} xmlFileId a feldolgozandó XML-tartalom vagy XML DOM-objektum
   */
function highlightXmlFileRow(xmlFileId){
    highlightXmlFileId = String(xmlFileId);
    renderCurrentView();
    window.setTimeout(() => {
      const row = document.querySelector(`tr[data-xml-file-row="${CSS.escape(String(xmlFileId))}"]`);
      if(!row){
        showToast('A célfájl sora nem látható az aktuális listában.', 'warning');
        return;
      }
      row.classList.add('is-highlighted', 'is-jump-highlighted');
      row.setAttribute('tabindex', '-1');
      row.scrollIntoView({ block: 'center', behavior: 'smooth' });
      try { row.focus({ preventScroll: true }); } catch (_) { /* focus is only visual aid */ }
      window.setTimeout(() => {
        highlightXmlFileId = null;
        row.classList.remove('is-highlighted', 'is-jump-highlighted');
        row.removeAttribute('tabindex');
      }, 6000);
    }, 120);
  }

    /**
   * A <code>jumpToXmlFileFromRevision</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} targetPath a mezőhöz vagy XML-csomóponthoz tartozó útvonal
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function jumpToXmlFileFromRevision(targetPath){
    let target = findXmlFileByPathOrName(targetPath);

    // The new-version target may have been auto-registered by the background scanner
    // after the changes modal was opened. Refresh once before giving up.
    if(!target){
      try{
        const response = await fetch(`/api/xml-files?archived=${showArchivedFiles ? 'true' : 'false'}`, { cache: 'no-store', credentials: 'same-origin' });
        if(response.ok){
          allXmlFiles = await response.json() || [];
          refreshFilterOptions(allXmlFiles);
          target = findXmlFileByPathOrName(targetPath);
        }
      }catch(_){
        // The normal not-found message below is enough for the user.
      }
    }

    if(!target){
      showToast('A célfájl nem található az XML listában. Lehet, hogy a háttérregisztráció még nem futott le.', 'warning');
      return;
    }

    const changesModal = document.getElementById('xmlChangesModal');
    if(changesModal) changesModal.hidden = true;
    resetXmlFileListFilters();

    const files = currentFilteredSortedFiles();
    const index = files.findIndex(file => String(file.id) === String(target.id));
    if(index >= 0){
      currentPage = Math.floor(index / pageSize) + 1;
    }else{
      currentPage = 1;
    }
    highlightXmlFileRow(target.id);
    showToast(`Ugrás a célfájlhoz: ${target.fileName || baseNameFromPath(targetPath)}`, 'success');
  }

    /**
   * Betölti vagy lekéri a load revision detail művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} revisionId a célobjektum technikai azonosítója
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadRevisionDetail(revisionId){
    const detailContainer = document.getElementById('xmlChangesRevisionDetail');
    if(!detailContainer || !revisionId) return;
    detailContainer.innerHTML = '<div class="xml-changes-empty">Revízió részleteinek betöltése...</div>';
    try{
      const response = await fetch(`/api/xml-files/revisions/${encodeURIComponent(revisionId)}`, { cache: 'no-store', credentials: 'same-origin' });
      if(!response.ok) throw new Error(await readError(response));
      const revision = await response.json();
      detailContainer.innerHTML = `
        <div class="xml-revision-detail-header">
          <h3>Revízió ${escapeText(revision.revisionNo || revision.id || '-')}</h3>
          <div>${escapeText(formatRevisionSaveType(revision.saveType))} · ${escapeText(formatDate(revision.createdAt))} · ${escapeText(revision.createdBy || '-')}</div>
        </div>
        <dl class="xml-revision-detail-meta">
          <div class="xml-revision-target-row"><dt>Cél fájl</dt><dd><span>${escapeText(revision.targetFilePath || '-')}</span>${revision.targetFilePath ? `<button type="button" class="xml-revision-jump-file" data-target-file-path="${escapeText(revision.targetFilePath)}" title="Ugrás a fájlhoz az XML listában" aria-label="Ugrás a fájlhoz az XML listában">↗</button>` : ''}</dd></div>
          <div><dt>Backup fájl</dt><dd>${escapeText(revision.backupFilePath || '-')}</dd></div>
          <div><dt>XSD validáció</dt><dd>${escapeText(revision.xsdValidationStatus || '-')}</dd></div>
          <div><dt>Megjegyzés</dt><dd>${escapeText(revision.userNote || '-')}</dd></div>
        </dl>
        ${renderDiffEntries(revision.diffEntries || [])}`;
      detailContainer.querySelectorAll('.xml-revision-jump-file').forEach(button => {
        button.addEventListener('click', () => jumpToXmlFileFromRevision(button.dataset.targetFilePath));
      });
    }catch(error){
      detailContainer.innerHTML = `<div class="message error message-error">${escapeText(error.message)}</div>`;
    }
  }

    /**
   * A <code>openChangesInfo</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function openChangesInfo(file){
    const modal = ensureChangesModal();
    const subtitle = modal.querySelector('#xmlChangesSubtitle');
    const content = modal.querySelector('#xmlChangesContent');
    if(subtitle) subtitle.textContent = file?.fileName ? `Állomány: ${file.fileName}` : '';
    if(content) content.innerHTML = '<div class="xml-changes-empty">Változások betöltése...</div>';
    modal.hidden = false;
    try{
      const response = await fetch(`/api/xml-files/${encodeURIComponent(file.id)}/revisions`, { cache: 'no-store', credentials: 'same-origin' });
      if(!response.ok) throw new Error(await readError(response));
      const revisions = await response.json() || [];
      if(!revisions.length){
        if(content) content.innerHTML = '<div class="xml-changes-empty">Ehhez az Űrlapállományhoz még nincs mentett revízió vagy diff.</div>';
        return;
      }
      if(content){
        content.innerHTML = `
          <div class="xml-changes-layout">
            <div class="xml-revision-list" aria-label="Revíziók">
              ${revisions.map(renderRevisionSummary).join('')}
            </div>
            <div id="xmlChangesRevisionDetail" class="xml-revision-detail">
              <div class="xml-changes-empty">Válassz revíziót a diff megtekintéséhez.</div>
            </div>
          </div>`;
        content.querySelectorAll('.xml-revision-card').forEach(button => {
          button.addEventListener('click', () => {
            content.querySelectorAll('.xml-revision-card').forEach(item => item.classList.remove('is-active'));
            button.classList.add('is-active');
            loadRevisionDetail(button.dataset.revisionId);
          });
        });
        const first = content.querySelector('.xml-revision-card');
        if(first){
          first.classList.add('is-active');
          await loadRevisionDetail(first.dataset.revisionId);
        }
      }
    }catch(error){
      if(content) content.innerHTML = `<div class="message error message-error">${escapeText(error.message)}</div>`;
      showMessage('error', error.message);
    }
  }

    /**
   * A <code>openUploadModal</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function openUploadModal(){
    if(!currentUserPermissions.canUpload){
      showMessage('error', 'XML feltöltés csak ADMIN vagy OPERATOR jogosultsággal érhető el.');
      return;
    }
    if(xmlUploadModal) xmlUploadModal.hidden = false;
  }

    /**
   * Elrejti vagy lezárja a close upload modal felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function closeUploadModal(){
    if(xmlUploadModal) xmlUploadModal.hidden = true;
  }

    /**
   * A <code>openNoteModal</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   */
function openNoteModal(file){
    if(!currentUserPermissions.canEdit){
      showMessage('error', 'Megjegyzés szerkesztése csak ADMIN vagy OPERATOR jogosultsággal érhető el.');
      return;
    }
    if(!xmlNoteModal || !xmlNoteFileId || !xmlNoteText) return;
    xmlNoteFileId.value = file?.id || '';
    xmlNoteText.value = file?.userNote || '';
    xmlNoteModal.hidden = false;
    xmlNoteText.focus();
  }

    /**
   * Elrejti vagy lezárja a close note modal felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function closeNoteModal(){
    if(xmlNoteModal) xmlNoteModal.hidden = true;
  }

    /**
   * Betölti vagy lekéri a load xml files művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadXmlFiles(){
    if(!tableBody) return;
    tableBody.innerHTML = '<tr><td colspan="12">Betöltés...</td></tr>';
    const archived = showArchivedFiles ? 'true' : 'false';
    try{
      const response = await fetch(`/api/xml-files?archived=${archived}`, { cache: 'no-store', credentials: 'same-origin' });
      if(!response.ok) throw new Error(await readError(response));
      allXmlFiles = await response.json() || [];
      refreshFilterOptions(allXmlFiles);
      renderCurrentView();
    }catch(error){
      tableBody.innerHTML = `<tr><td colspan="12">${escapeText(error.message)}</td></tr>`;
      updatePagination(0);
      showMessage('error', error.message);
    }
  }

    /**
   * Szinkronizálja vagy frissíti a apply filters által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function applyFilters(){
    currentPage = 1;
    renderCurrentView();
  }

    /**
   * Feldolgozza a format attachment size bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function formatAttachmentSize(value){
    const bytes = Number(value);
    if(!Number.isFinite(bytes) || bytes < 0) return '–';
    if(bytes < 1024) return `${bytes} B`;
    const units = ['KB','MB','GB'];
    let size = bytes / 1024;
    let unit = units[0];
    for(let index = 1; index < units.length && size >= 1024; index += 1){
      size /= 1024;
      unit = units[index];
    }
    return `${size >= 10 ? size.toFixed(1) : size.toFixed(2)} ${unit}`;
  }

    /**
   * Kezeli vagy beköti a attachment type label esemény- és inicializációs folyamatát.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} item a függvény item bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function attachmentTypeLabel(item){
    const contentType = String(item?.contentType || '').trim();
    if(contentType && contentType !== 'application/octet-stream') return contentType;
    const fileName = String(item?.fileName || '');
    const extension = fileName.includes('.') ? fileName.split('.').pop().toUpperCase() : '';
    return extension || 'Bináris állomány';
  }

    /**
   * Megjeleníti vagy újrarendereli a render current view állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function renderCurrentView(){
    const files = currentFilteredSortedFiles();
    const pages = totalPages(files.length);
    if(currentPage > pages) currentPage = pages;
    const start = (currentPage - 1) * pageSize;
    const pageFiles = files.slice(start, start + pageSize);
    renderXmlFiles(pageFiles, files.length);
  }

    /**
   * Megjeleníti vagy újrarendereli a render xml files állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} files a függvény files bemeneti értéke
   * @param {*} total a függvény total bemeneti értéke
   */
function renderXmlFiles(files, total){
    currentXmlFileMap = new Map(allXmlFiles.map(file => [String(file.id), file]));
    updateSortIndicators();
    updatePagination(typeof total === 'number' ? total : files.length);
    if(!files.length){
      tableBody.innerHTML = '<tr><td colspan="12">Nincs megjeleníthető Űrlapállomány.</td></tr>';
      return;
    }
    tableBody.innerHTML = files.map(file => `
      <tr data-xml-file-row="${escapeText(file.id)}" class="${file.archived ? 'is-archived' : ''} ${file.locked ? 'is-locked' : ''} ${highlightXmlFileId && String(highlightXmlFileId) === String(file.id) ? 'is-highlighted' : ''}">
        <td>${escapeText(file.id)}</td>
        <td class="xml-attachment-toggle-cell"><button type="button" class="xml-attachment-toggle" hidden data-attachment-toggle="${escapeText(file.id)}" title="Csatolmányok megjelenítése" aria-label="Csatolmányok megjelenítése" aria-expanded="false">📎</button></td>
        <td class="xml-file-name-cell"><strong>${escapeText(file.fileName)}</strong></td>
        <td>${file.partnerTaxNumber && file.partnerName ? escapeText(`${file.partnerTaxNumber} - ${file.partnerName}`) : `<span class="xml-partner-import-error" title="${escapeText(file.partnerImportMessage || 'Nincs partner hozzárendelve')}">${escapeText(file.partnerImportStatus === 'ERROR' ? file.partnerImportMessage : 'Nincs partner')}</span>`}</td>
        <td>${escapeText(formatDate(file.createdAt))}</td>
        <td>${escapeText(formatDate(file.updatedAt))}</td>
        <td>${escapeText(file.formType || '-')}</td>
        <td>${escapeText(file.formVersion || '-')}</td>
        <td>${escapeText(file.fileSizeDisplay || '-')}</td>
        <td class="xml-file-note-cell">${escapeText(file.userNote || '-')}</td>
        <td>${renderStatusCell(file)}</td>
        <td>${renderActions(file)}</td>
      </tr>
      <tr class="xml-attachment-detail-row" data-attachment-row="${escapeText(file.id)}" hidden><td colspan="12"><div class="xml-attachment-detail-content">Betöltés...</div></td></tr>`).join('');
    const attachmentIds = files.map(file => file.id).filter(id => id != null);
    if(attachmentIds.length){
      fetch(`/api/submissions/xml-files/attachment-counts?${attachmentIds.map(id => `ids=${encodeURIComponent(id)}`).join('&')}`, { credentials:'same-origin', cache:'no-store' })
        .then(response => response.ok ? response.json() : {})
        .then(counts => {
          document.querySelectorAll('.xml-attachment-toggle').forEach(button => {
            const count = Number(counts?.[button.dataset.attachmentToggle] || 0);
            button.hidden = count < 1;
            if(count > 0){
              button.textContent = '📎';
              button.title = `${count} csatolmány megjelenítése`;
              button.setAttribute('aria-label', `${count} csatolmány megjelenítése`);
            }
          });
        })
        .catch(error => console.debug('A csatolmánydarabszám nem tölthető be.', error));
    }

    document.querySelectorAll('.xml-file-kebab-button').forEach(button => {
      button.addEventListener('click', (event) => {
        event.stopPropagation();
        const id = String(button.dataset.actionMenuToggle || '');
        document.querySelectorAll('.xml-file-menu').forEach(menu => {
          if(menu.dataset.actionMenu !== id){
            menu.hidden = true;
            restoreActionMenu(menu);
          }
        });
        const menu = document.querySelector(`.xml-file-menu[data-action-menu="${CSS.escape(id)}"]`);
        if(menu){
          const shouldOpen = menu.hidden;
          menu.hidden = !shouldOpen;
          if(shouldOpen){
            positionActionMenu(button, menu);
          }
        }
      });
    });
    document.querySelectorAll('.xml-attachment-toggle').forEach(button => {
      button.addEventListener('click', async (event) => {
        event.stopPropagation();
        const id = String(button.dataset.attachmentToggle || '');
        const row = document.querySelector(`tr[data-attachment-row="${CSS.escape(id)}"]`);
        if(!row) return;
        const open = row.hidden;
        row.hidden = !open;
        button.setAttribute('aria-expanded', String(open));
        if(!open || row.dataset.loaded === 'true') return;
        const content = row.querySelector('.xml-attachment-detail-content');
        try{
          const response = await fetch(`/api/submissions/xml-files/${encodeURIComponent(id)}/attachments`, { credentials:'same-origin', cache:'no-store' });
          if(!response.ok) throw new Error(`A csatolmánylista nem tölthető be (${response.status}).`);
          const attachments = await response.json();
          if(!Array.isArray(attachments) || !attachments.length){
            content.textContent = 'Nincs csatolmány.';
            button.hidden = true;
          }else{
            content.innerHTML = `<div class="xml-attachment-table-wrap"><table class="xml-attachment-table"><thead><tr><th>#</th><th>Fájlnév</th><th>Méret</th><th>Típus</th><th>Csatolás dátuma</th><th>Művelet</th></tr></thead><tbody>${attachments.map((item, index) => `<tr><td>${index + 1}</td><td>${escapeText(item.fileName || '–')}</td><td>${escapeText(formatAttachmentSize(item.fileSize))}</td><td>${escapeText(attachmentTypeLabel(item))}</td><td>${escapeText(formatDate(item.createdAt))}</td><td><a class="xml-attachment-view-link" target="_blank" rel="noopener" title="Csatolmány megtekintése" aria-label="Csatolmány megtekintése" href="/api/submissions/xml-files/${encodeURIComponent(id)}/attachments/${encodeURIComponent(item.id)}/content"><span aria-hidden="true">↗</span></a></td></tr>`).join('')}</tbody></table></div>`;
          }
          row.dataset.loaded = 'true';
        }catch(error){ content.textContent = error.message || 'A csatolmánylista betöltése sikertelen.'; }
      });
    });

    document.querySelectorAll('.open-xml-file-button').forEach(button => {
      button.addEventListener('click', () => openXmlFile(button.dataset.id, normalizeBoolean(button.dataset.readonly)));
    });
    document.querySelectorAll('.copy-xml-file-button').forEach(button => {
      button.addEventListener('click', () => openCopyModal(currentXmlFileMap.get(String(button.dataset.id))));
    });
    document.querySelectorAll('.close-xml-file-button').forEach(button => {
      button.addEventListener('click', () => closeXmlFile(button.dataset.id));
    });
    document.querySelectorAll('.archive-xml-file-button').forEach(button => {
      button.addEventListener('click', () => archiveXmlFile(button.dataset.id));
    });
    document.querySelectorAll('.physical-archive-xml-file-button').forEach(button => {
      button.addEventListener('click', () => physicalArchiveXmlFile(button.dataset.id));
    });
    document.querySelectorAll('.change-partner-xml-file-button').forEach(button => {
      button.addEventListener('click', () => openPartnerChangeModal(allXmlFiles.find(file => String(file.id) === String(button.dataset.id))));
    });
    document.querySelectorAll('.permanent-delete-xml-file-button').forEach(button => {
      button.addEventListener('click', () => permanentlyDeleteXmlFile(button.dataset.id));
    });
    document.querySelectorAll('.resolver-info-button').forEach(button => {
      button.addEventListener('click', () => openResolverInfo(currentXmlFileMap.get(String(button.dataset.id))));
    });
    document.querySelectorAll('.lock-info-button').forEach(button => {
      button.addEventListener('click', () => openLockInfo(currentXmlFileMap.get(String(button.dataset.id))));
    });
    document.querySelectorAll('.xsd-errors-button').forEach(button => {
      button.addEventListener('click', () => openXsdErrorsInfo(currentXmlFileMap.get(String(button.dataset.id))));
    });
    document.querySelectorAll('.download-xml-file-button').forEach(button => {
      button.addEventListener('click', () => downloadXmlFile(button.dataset.id));
    });
    document.querySelectorAll('.changes-info-button').forEach(button => {
      button.addEventListener('click', () => openChangesInfo(currentXmlFileMap.get(String(button.dataset.id))));
    });
    document.querySelectorAll('.edit-note-button').forEach(button => {
      button.addEventListener('click', () => openNoteModal(currentXmlFileMap.get(String(button.dataset.id))));
    });
  }

    /**
   * Feldolgozza a format tax number input bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function formatTaxNumberInput(value){
    const digits = String(value || '').replace(/\D/g, '').slice(0, 11);
    return [digits.slice(0,8), digits.slice(8,9), digits.slice(9,11)].filter(Boolean).join('-');
  }


    /**
   * Ellenőrzi a is valid hungarian tax number feltételeit, és a hívó számára döntési eredményt állít elő.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function isValidHungarianTaxNumber(value){
    const normalized = formatTaxNumberInput(value);
    if(!/^\d{8}-\d-\d{2}$/.test(normalized)) return false;
    const core = normalized.slice(0, 8);
    const weights = [9, 7, 3, 1, 9, 7, 3];
    const sum = weights.reduce((total, weight, index) => total + Number(core[index]) * weight, 0);
    const expectedCheckDigit = (10 - (sum % 10)) % 10;
    return Number(core[7]) === expectedCheckDigit;
  }

    /**
   * Előkészíti és elindítja a save quick upload partner állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function saveQuickUploadPartner(){
    const taxNumber = formatTaxNumberInput(uploadPartnerTaxNumber?.value);
    const name = String(uploadPartnerName?.value || '').trim();
    if(!/^\d{8}-\d-\d{2}$/.test(taxNumber)){ showMessage('error', 'Az adószám formátuma 8-1-2 legyen, például 12345676-1-42.'); return; }
    if(!isValidHungarianTaxNumber(taxNumber)){ showMessage('error', 'Az adószám első nyolc számjegyének CDV ellenőrzése sikertelen.'); return; }
    if(!name){ showMessage('error', 'A partner neve kötelező.'); return; }
    const ok = await window.navConfirm?.({title:'Partner rögzítése', message:`Biztosan rögzíteni szeretné ezt a partnert?\n${taxNumber} - ${name}`, confirmText:'Rögzítés', cancelText:'Mégsem'});
    if(!ok) return;
    const response = await fetch('/api/partners',{method:'POST',headers:{'Content-Type':'application/json'},credentials:'same-origin',body:JSON.stringify({taxNumber,name,active:true})});
    const data = await response.json().catch(()=>({}));
    if(!response.ok){ showMessage('error', data.message || data.error || 'A partner rögzítése sikertelen.'); return; }
    uploadPartnerId.value = data.id; uploadPartnerSearch.value = `${data.taxNumber} - ${data.name}`;
    uploadPartnerTaxNumber.value=''; uploadPartnerName.value='';
    showMessage('success', 'A partner rögzítése sikeres.');
  }

    /**
   * A <code>openPartnerChangeModal</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} file a függvény file bemeneti értéke
   */
function openPartnerChangeModal(file){
    if(!file || !xmlPartnerModal) return;
    xmlPartnerFileId.value = file.id; xmlPartnerId.value=''; xmlPartnerSearch.value='';
    xmlPartnerCurrentHint.textContent = `Jelenlegi partner: ${file.partnerTaxNumber && file.partnerName ? `${file.partnerTaxNumber} - ${file.partnerName}` : 'nincs hozzárendelve'}`;
    xmlPartnerModal.hidden=false; xmlPartnerSearch.focus();
  }

    /**
   * Elindítja a submit partner change aszinkron vagy több lépéses frontend folyamatát.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function submitPartnerChange(event){
    event.preventDefault();
    const fileId=xmlPartnerFileId.value, partnerId=xmlPartnerId.value;
    if(!partnerId){ showMessage('error', 'Válassz ki egy partnert.'); return; }
    const ok = await window.navConfirm?.({title:'Partner módosítása',message:'Biztosan módosítani szeretnéd az állományhoz rendelt partnert?',confirmText:'Módosítás',cancelText:'Mégsem'});
    if(!ok) return;
    const response=await fetch(`/api/xml-files/${encodeURIComponent(fileId)}/partner`,{method:'PUT',headers:{'Content-Type':'application/json'},credentials:'same-origin',body:JSON.stringify({partnerId:Number(partnerId)})});
    const data=await response.json().catch(()=>({}));
    if(!response.ok){ showMessage('error', data.message || data.error || 'A partner módosítása sikertelen.'); return; }
    xmlPartnerModal.hidden=true; await loadXmlFiles(); showMessage('success', 'A partner módosítása sikeres.');
  }

    /**
   * A <code>uploadXmlFile</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function uploadXmlFile(event){
    event.preventDefault();
    if(uploadInProgress) return;
    showMessage('', '');
    if(!uploadFile || !uploadFile.files || !uploadFile.files.length){
      showMessage('error', 'Válassz ki egy XML fájlt.');
      return;
    }
    if(!uploadPartnerId?.value){ showMessage('error', 'XML feltöltésekor a partner kiválasztása kötelező.'); uploadPartnerSearch?.focus(); return; }
    const selectedFileName = uploadFile.files[0]?.name || 'Űrlapállomány';
    const formData = new FormData();
    formData.append('file', uploadFile.files[0]);
    formData.append('userNote', uploadNote ? uploadNote.value : '');
    formData.append('partnerId', uploadPartnerId.value);
    uploadInProgress = true;
    if(uploadSubmitButton){
      uploadSubmitButton.disabled = true;
      uploadSubmitButton.dataset.originalText = uploadSubmitButton.textContent || 'Feltöltés';
      uploadSubmitButton.textContent = 'Feltöltés folyamatban...';
    }
    if(uploadFile) uploadFile.disabled = true;
    if(uploadNote) uploadNote.disabled = true;
    if(window.NavProcessingJobs?.showLocal){
      window.NavProcessingJobs.showLocal({
        title: 'XML feltöltés',
        message: `Az állomány feltöltése és regisztrációja folyamatban: ${selectedFileName}`,
        status: 'Feltöltés folyamatban',
        percentText: ''
      });
    }
    try{
      const response = await fetch('/api/xml-files/upload', { method: 'POST', body: formData, credentials: 'same-origin' });
      if(!response.ok) throw new Error(await readError(response));
      const saved = await response.json();
      showMessage('success', `Űrlapállomány feltöltve: ${saved.fileName}`);
      uploadForm.reset();
      closeUploadModal();
      currentPage = 1;
      await loadXmlFiles();
    }catch(error){
      showMessage('error', error.message);
    }finally{
      uploadInProgress = false;
      if(uploadSubmitButton){
        uploadSubmitButton.disabled = false;
        uploadSubmitButton.textContent = uploadSubmitButton.dataset.originalText || 'Feltöltés';
      }
      if(uploadFile) uploadFile.disabled = false;
      if(uploadNote) uploadNote.disabled = false;
      if(window.NavProcessingJobs?.hide){
        window.NavProcessingJobs.hide();
      }
    }
  }

    /**
   * Előkészíti és elindítja a save xml note állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function saveXmlNote(event){
    event.preventDefault();
    const id = xmlNoteFileId ? xmlNoteFileId.value : '';
    if(!id) return;
    try{
      const response = await fetch(`/api/xml-files/${encodeURIComponent(id)}/note`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userNote: xmlNoteText ? xmlNoteText.value : '' })
      });
      if(!response.ok) throw new Error(await readError(response));
      closeNoteModal();
      showMessage('success', 'Megjegyzés mentve.');
      await loadXmlFiles();
    }catch(error){
      showMessage('error', error.message);
    }
  }


    /**
   * A <code>suggestedCopyFileName</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} fileName a feloldáshoz vagy megjelenítéshez használt név
   * @returns {*} a feldolgozás eredménye
   */
function suggestedCopyFileName(fileName){
    const name = String(fileName || 'masolat.xml');
    const base = name.toLowerCase().endsWith('.xml') ? name.slice(0, -4) : name;
    return `${base} másolata.xml`;
  }

    /**
   * Szinkronizálja vagy frissíti a set copy file name state által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} ok a függvény ok bemeneti értéke
   * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
   */
function setCopyFileNameState(ok, message){
    if(xmlCopyFileNameState){
      xmlCopyFileNameState.textContent = ok ? '✓' : '✕';
      xmlCopyFileNameState.classList.toggle('ok', !!ok);
      xmlCopyFileNameState.classList.toggle('error', !ok);
    }
    if(xmlCopyFileNameMessage) xmlCopyFileNameMessage.textContent = message || '';
    if(xmlCopySubmitButton) xmlCopySubmitButton.disabled = !ok;
  }

    /**
   * A <code>openCopyModal</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   */
function openCopyModal(file){
    if(!currentUserPermissions.canEdit){
      showMessage('error', 'XML másolása csak ADMIN vagy OPERATOR jogosultsággal érhető el.');
      return;
    }
    if(!file || !xmlCopyModal) return;
    if(xmlCopySourceId) xmlCopySourceId.value = file.id || '';
    if(xmlCopySourceHint) xmlCopySourceHint.textContent = `Forrás: ${file.fileName || '-'}`;
    if(xmlCopyFileName) xmlCopyFileName.value = suggestedCopyFileName(file.fileName);
    if(xmlCopyNote) xmlCopyNote.value = file.userNote || '';
    setCopyFileNameState(false, 'Fájlnév ellenőrzése...');
    xmlCopyModal.hidden = false;
    checkCopyFileNameAvailability();
    setTimeout(() => xmlCopyFileName?.focus(), 0);
  }

    /**
   * Elrejti vagy lezárja a close copy modal felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function closeCopyModal(){
    if(xmlCopyModal) xmlCopyModal.hidden = true;
  }

    /**
   * Ellenőrzi a check copy file name availability feltételeit, és a hívó számára döntési eredményt állít elő.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function checkCopyFileNameAvailability(){
    const fileName = xmlCopyFileName ? xmlCopyFileName.value.trim() : '';
    if(!fileName){
      setCopyFileNameState(false, 'Adj meg fájlnevet.');
      return;
    }
    try{
      const response = await fetch(`/api/xml-files/check-filename?fileName=${encodeURIComponent(fileName)}`, { cache:'no-store', credentials:'same-origin' });
      const data = await response.json().catch(() => ({}));
      if(!response.ok) throw new Error(data.message || data.error || 'A fájlnév ellenőrzése sikertelen.');
      setCopyFileNameState(data.available === true, data.message || (data.available ? 'A fájlnév használható.' : 'A fájlnév nem használható.'));
    }catch(error){
      setCopyFileNameState(false, error.message || 'A fájlnév ellenőrzése sikertelen.');
    }
  }

    /**
   * A <code>scheduleCopyFileNameCheck</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function scheduleCopyFileNameCheck(){
    if(copyFileNameCheckTimer) clearTimeout(copyFileNameCheckTimer);
    setCopyFileNameState(false, 'Fájlnév ellenőrzése...');
    copyFileNameCheckTimer = setTimeout(checkCopyFileNameAvailability, 350);
  }

    /**
   * Elindítja a submit copy xml aszinkron vagy több lépéses frontend folyamatát.
   *
   * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function submitCopyXml(event){
    event.preventDefault();
    const id = xmlCopySourceId ? xmlCopySourceId.value : '';
    if(!id) return;
    try{
      const response = await fetch(`/api/xml-files/${encodeURIComponent(id)}/copy`, {
        method:'POST',
        credentials:'same-origin',
        headers:{ 'Content-Type':'application/json' },
        body: JSON.stringify({ fileName: xmlCopyFileName ? xmlCopyFileName.value.trim() : '', userNote: xmlCopyNote ? xmlCopyNote.value : '' })
      });
      const data = await response.json().catch(() => ({}));
      if(!response.ok) throw new Error(data.message || data.error || 'Az XML másolása sikertelen.');
      closeCopyModal();
      showMessage('success', `Űrlapállomány másolva: ${data.fileName || ''}`);
      highlightXmlFileId = data.id || null;
      await loadXmlFiles();
    }catch(error){
      showMessage('error', error.message || 'Az XML másolása sikertelen.');
    }
  }


    /**
   * A <code>forceReleaseLockFromConflict</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} id a célobjektum technikai azonosítója
   * @param {*} reason a függvény reason bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function forceReleaseLockFromConflict(id, reason){
    const response = await fetch(`/api/xml-files/${encodeURIComponent(id)}/lock/force-release`, {
      method:'POST',
      credentials:'same-origin',
      headers:{ 'Content-Type':'application/json' },
      body: JSON.stringify({ reason: reason || '' })
    });
    const data = await response.json().catch(() => ({}));
    if(!response.ok) throw new Error(data.message || data.error || 'A kényszerített lezárás sikertelen.');
    showMessage('success', 'A zárolás kényszerítve lezárva. Az XML újra megnyitható szerkesztésre.');
    await loadXmlFiles();
  }

    /**
   * A <code>sendLockReleaseRequest</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A hálózati hívás eredményét egységes kliensoldali hibakezeléssel adja tovább, és a felhasználói felületet csak a válasz feldolgozása után módosítja.</p>
   * @param {*} id a célobjektum technikai azonosítója
   * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function sendLockReleaseRequest(id, message){
    const response = await fetch(`/api/xml-files/${encodeURIComponent(id)}/lock-release-requests`, {
      method:'POST',
      credentials:'same-origin',
      headers:{ 'Content-Type':'application/json' },
      body: JSON.stringify({ message: message || '' })
    });
    const data = await response.json().catch(() => ({}));
    if(!response.ok) throw new Error(data.message || data.error || 'A lezárási kérelem küldése sikertelen.');
    showMessage('success', 'Lezárási kérelem elküldve a zároló felhasználónak.');
  }

    /**
   * A <code>openXmlFile</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} id a célobjektum technikai azonosítója
   * @param {*} readOnly a függvény readOnly bemeneti értéke
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function openXmlFile(id, readOnly){
    if(!id) return;
    if(!readOnly && !currentUserPermissions.canEdit){
      showMessage('error', 'Szerkesztésre megnyitás csak ADMIN vagy OPERATOR jogosultsággal érhető el.');
      return;
    }
    try{
      const response = await fetch(`/api/xml-files/${encodeURIComponent(id)}/open`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ readOnly: !!readOnly })
      });
      if(response.status === 409 && !readOnly){
        const message = await readError(response);
        const openReadOnly = window.navConfirm ? await window.navConfirm({
          title: 'Szerkesztési lock ütközés',
          message: `${message}

Megnyitod csak olvasási módban?`,
          confirmText: 'Megnyitás csak olvasásra',
          cancelText: 'Lezárási kérelem',
          variant: 'warning'
        }) : false;
        if(openReadOnly){
          return openXmlFile(id, true);
        }
        if(currentUserPermissions.canAdmin){
          const forceReason = window.navPrompt ? await window.navPrompt({
            title: 'Kényszerített munkamenet lezárás',
            message: 'ADMIN jogosultsággal az XML munkamenet azonnal lezárható. A zároló felhasználó képernyője visszalép az Űrlapállományok listára.',
            label: 'Indok',
            placeholder: 'pl. adminisztrátori lezárás...',
            defaultValue: '',
            confirmText: 'Kényszerített lezárás',
            cancelText: 'Lezárási kérelem',
            variant: 'danger'
          }) : null;
          if(forceReason !== null){
            await forceReleaseLockFromConflict(id, forceReason);
            return openXmlFile(id, false);
          }
        }
        const requestMessage = window.navPrompt ? await window.navPrompt({
          title: 'Lezárási kérelem küldése',
          message: 'Küldhetsz lezárási kérelmet a zároló felhasználónak.',
          label: 'Indok',
          placeholder: 'pl. sürgős javítás szükséges...',
          defaultValue: '',
          confirmText: 'Kérelem küldése',
          cancelText: 'Mégsem',
          variant: 'warning'
        }) : null;
        if(requestMessage !== null){
          await sendLockReleaseRequest(id, requestMessage);
          return;
        }
        throw new Error(message);
      }
      if(!response.ok) throw new Error(await readError(response));
      const opened = await response.json();
      sessionStorage.setItem('navXsdToolActiveXmlFile', JSON.stringify(opened));
      const query = new URLSearchParams({ xmlFileId: String(id), readOnly: String(!!opened.readOnly) });
      window.location.href = `/form.html?${query.toString()}`;
    }catch(error){
      showMessage('error', error.message);
    }
  }

    /**
   * Elrejti vagy lezárja a close xml file felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} id a célobjektum technikai azonosítója
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function closeXmlFile(id){
    if(!id) return;
    try{
      const response = await fetch(`/api/xml-files/${encodeURIComponent(id)}/close`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: 'Felhasználói lezárás az Űrlapállományok oldalról.', sessionId: currentXmlFileMap.get(String(id))?.activeSessionId || null })
      });
      if(!response.ok) throw new Error(await readError(response));
      showMessage('success', 'Űrlapállomány munkamenet lezárva.');
      await loadXmlFiles();
    }catch(error){
      showMessage('error', error.message);
    }
  }

    /**
   * A <code>downloadXmlFile</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} id a célobjektum technikai azonosítója
   */
function downloadXmlFile(id){
    if(!id) return;
    window.location.href = `/api/xml-files/${encodeURIComponent(id)}/download`;
  }

    /**
   * A <code>permanentlyDeleteXmlFile</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} id a célobjektum technikai azonosítója
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function permanentlyDeleteXmlFile(id){
    if(!currentUserPermissions.canAdmin){
      showMessage('error', 'A végleges törlés csak ADMIN jogosultsággal érhető el.');
      return;
    }
    const file = currentXmlFileMap.get(String(id));
    const fileName = file?.fileName || `#${id}`;
    const confirmed = window.navConfirm ? await window.navConfirm({
      title: 'Űrlapállomány végleges törlése',
      message: `A(z) ${fileName} állomány, az adatbázis-bejegyzései, mentési előzményei és nagy XML indexe véglegesen törlődik. A művelet nem vonható vissza.`,
      confirmText: 'Végleges törlés',
      cancelText: 'Mégsem',
      variant: 'danger'
    }) : window.confirm(`A(z) ${fileName} állomány véglegesen törlődik. A művelet nem vonható vissza. Folytatod?`);
    if(!confirmed) return;
    const promptResult = window.navFormPrompt ? await window.navFormPrompt({
      title: 'Törlés megerősítése',
      message: `A végleges törléshez írd be pontosan ezt az állománynevet: ${fileName}`,
      confirmText: 'Törlés',
      cancelText: 'Mégsem',
      variant: 'danger',
      fields: [{
        name: 'fileName',
        label: 'Állománynév',
        type: 'text',
        placeholder: fileName,
        required: true,
        maxLength: 512
      }]
    }) : null;
    const typedName = promptResult !== null
      ? promptResult?.fileName
      : (window.navPrompt ? await window.navPrompt({
          title: 'Törlés megerősítése',
          message: `A végleges törléshez írd be pontosan ezt az állománynevet: ${fileName}`,
          label: 'Állománynév',
          placeholder: fileName,
          confirmText: 'Törlés',
          cancelText: 'Mégsem',
          variant: 'danger'
        }) : window.prompt('Írd be pontosan a törlendő állomány nevét:', ''));
    if(typedName === null || typedName === undefined) return;
        /**
     * Feldolgozza a normalize confirmation file name bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @param {*} value a feldolgozandó vagy beállítandó érték
     */
const normalizeConfirmationFileName = value => String(value ?? '')
      .trim()
      .replace(/[\u200B-\u200D\u2060\uFEFF]/g, '')
      .normalize('NFC');
    const normalizedTypedName = normalizeConfirmationFileName(typedName);
    const normalizedExpectedName = normalizeConfirmationFileName(fileName);
    if(normalizedTypedName !== normalizedExpectedName){
      console.warn('[XML-PERMANENT-DELETE] Filename confirmation mismatch', {
        typedName,
        expectedName: fileName,
        normalizedTypedName,
        normalizedExpectedName,
        typedCodePoints: Array.from(normalizedTypedName).map(character => character.codePointAt(0).toString(16)),
        expectedCodePoints: Array.from(normalizedExpectedName).map(character => character.codePointAt(0).toString(16))
      });
      showMessage('error', `A megadott állománynév nem egyezik. Elvárt érték: ${fileName}`);
      return;
    }
    try{
      const response = await fetch(`/api/xml-files/${encodeURIComponent(id)}/permanent`, {
        method:'DELETE', credentials:'same-origin', headers:{'Content-Type':'application/json'},
        body:JSON.stringify({ reason:'Adminisztrátori végleges törlés az Űrlapállományok oldalról.' })
      });
      if(!response.ok) throw new Error(await readError(response));
      showMessage('success', 'Az Űrlapállomány véglegesen törölve lett.');
      await loadXmlFiles();
    }catch(error){
      showMessage('error', error.message);
    }
  }

    /**
   * A <code>physicalArchiveXmlFile</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} id a célobjektum technikai azonosítója
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function physicalArchiveXmlFile(id){
    if(!currentUserPermissions.canPhysicallyArchive){
      showMessage('error', 'Fizikai archiválás csak FILE_DELETE jogosultsággal érhető el.');
      return;
    }
    const file = currentXmlFileMap.get(String(id));
    const reason = window.navPrompt ? await window.navPrompt({
      title: 'Űrlapállomány fizikai archiválása',
      message: `Az Űrlapállomány az archív mappába lesz mozgatva${file?.fileName ? `: ${file.fileName}` : ''}. A művelet naplózásra kerül.`,
      label: 'Fizikai archiválás oka',
      placeholder: 'pl. téves feltöltés, takarítás, archiválási döntés...',
      defaultValue: '',
      confirmText: 'Fizikai archiválás',
      cancelText: 'Mégsem',
      variant: 'danger'
    }) : null;
    if(reason === null) return;
    try{
      const response = await fetch(`/api/xml-files/${encodeURIComponent(id)}/physical-archive`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason })
      });
      if(!response.ok) throw new Error(await readError(response));
      showMessage('success', 'Űrlapállomány fizikailag archív mappába mozgatva.');
      await loadXmlFiles();
    }catch(error){
      showMessage('error', error.message);
    }
  }

    /**
   * A <code>archiveXmlFile</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} id a célobjektum technikai azonosítója
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function archiveXmlFile(id){
    if(!currentUserPermissions.canEdit){
      showMessage('error', 'Archiválás csak ADMIN vagy OPERATOR jogosultsággal érhető el.');
      return;
    }
    const file = currentXmlFileMap.get(String(id));
    const reason = window.navPrompt ? await window.navPrompt({
      title: 'Űrlapállomány archiválása',
      message: `Biztosan archiválod az Űrlapállományt${file?.fileName ? `: ${file.fileName}` : ''}?`,
      label: 'Archiválás oka (opcionális)',
      placeholder: 'pl. hibás feltöltés, tesztállomány, duplikátum...',
      defaultValue: '',
      confirmText: 'Archiválás',
      cancelText: 'Mégsem',
      variant: 'danger'
    }) : null;
    if(reason === null) return;
    try{
      const response = await fetch(`/api/xml-files/${encodeURIComponent(id)}`, {
        method: 'DELETE',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason })
      });
      if(!response.ok) throw new Error(await readError(response));
      showMessage('success', 'Űrlapállomány archiválva.');
      await loadXmlFiles();
    }catch(error){
      showMessage('error', error.message);
    }
  }

    /**
   * Szinkronizálja vagy frissíti a set archived toggle state által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function setArchivedToggleState(){
    if(!toggleArchivedXmlFilesButton) return;
    toggleArchivedXmlFilesButton.setAttribute('aria-pressed', String(showArchivedFiles));
    toggleArchivedXmlFilesButton.classList.toggle('is-active', showArchivedFiles);
    toggleArchivedXmlFilesButton.title = showArchivedFiles ? 'Aktív állományok megjelenítése' : 'Archivált állományok megjelenítése';
    toggleArchivedXmlFilesButton.setAttribute('aria-label', toggleArchivedXmlFilesButton.title);
  }

    /**
   * A <code>gotoPage</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} page az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
   */
function gotoPage(page){
    const total = currentFilteredSortedFiles().length;
    currentPage = Math.min(Math.max(1, page), totalPages(total));
    renderCurrentView();
  }

  setupPartnerPredictive(uploadPartnerSearch, uploadPartnerId, uploadPartnerSuggestions);
  setupPartnerPredictive(xmlPartnerSearch, xmlPartnerId, xmlPartnerSuggestions);
  uploadPartnerTaxNumber?.addEventListener('input', () => { uploadPartnerTaxNumber.value = formatTaxNumberInput(uploadPartnerTaxNumber.value); });
  uploadPartnerSaveButton?.addEventListener('click', saveQuickUploadPartner);
  xmlPartnerForm?.addEventListener('submit', submitPartnerChange);
  document.querySelectorAll('[data-close-xml-partner]').forEach(element => element.addEventListener('click', () => { xmlPartnerModal.hidden=true; }));
  setupPartnerPredictive(xmlFilePartnerFilter, xmlFilePartnerFilterId, xmlFilePartnerSuggestions, applyFilters);
  if(uploadForm) uploadForm.addEventListener('submit', uploadXmlFile);
  if(xmlNoteForm) xmlNoteForm.addEventListener('submit', saveXmlNote);
  if(refreshXmlFilesButton) refreshXmlFilesButton.addEventListener('click', async () => {
    await loadXmlFiles();
    showMessage('success', 'Lista frissítve.');
  });
  if(openXmlUploadModalButton) openXmlUploadModalButton.addEventListener('click', openUploadModal);
  xmlCopyForm?.addEventListener('submit', submitCopyXml);
  xmlCopyFileName?.addEventListener('input', scheduleCopyFileNameCheck);
  if(toggleArchivedXmlFilesButton) toggleArchivedXmlFilesButton.addEventListener('click', async () => {
    showArchivedFiles = !showArchivedFiles;
    setArchivedToggleState();
    currentPage = 1;
    await loadXmlFiles();
  });
  [xmlFileSearchInput, xmlFileFormVersionFilter, xmlFileStatusFilter].forEach(control => {
    if(control) control.addEventListener('input', applyFilters);
    if(control) control.addEventListener('change', applyFilters);
  });
  if(xmlFileFormTypeFilter){
    xmlFileFormTypeFilter.addEventListener('change', () => {
      refreshVersionFilterOptions();
      applyFilters();
    });
  }
  if(xmlFilePageSizeSelect) xmlFilePageSizeSelect.addEventListener('change', () => {
    pageSize = Number(xmlFilePageSizeSelect.value || 20);
    currentPage = 1;
    renderCurrentView();
  });
  if(xmlFilesFirstPageButton) xmlFilesFirstPageButton.addEventListener('click', () => gotoPage(1));
  if(xmlFilesPrevPageButton) xmlFilesPrevPageButton.addEventListener('click', () => gotoPage(currentPage - 1));
  if(xmlFilesNextPageButton) xmlFilesNextPageButton.addEventListener('click', () => gotoPage(currentPage + 1));
  if(xmlFilesLastPageButton) xmlFilesLastPageButton.addEventListener('click', () => gotoPage(totalPages(currentFilteredSortedFiles().length)));
  document.querySelectorAll('.xml-files-sort-button').forEach(button => {
    button.addEventListener('click', () => {
      const field = button.dataset.sortField;
      if(!field) return;
      if(sortField === field){
        sortDirection = sortDirection === 'asc' ? 'desc' : 'asc';
      }else{
        sortField = field;
        sortDirection = 'desc';
      }
      currentPage = 1;
      renderCurrentView();
    });
  });
  document.addEventListener('click', (event) => {
    if(!event.target.closest || (!event.target.closest('.xml-file-action-menu') && !event.target.closest('.xml-file-menu'))){
      closeActionMenus();
    }
    if(event.target && event.target.dataset && event.target.dataset.closeXmlUpload){
      closeUploadModal();
    }
    if(event.target && event.target.dataset && event.target.dataset.closeXmlNote){
      closeNoteModal();
    }
  });
  document.addEventListener('keydown', (event) => {
    if(event.key === 'Escape'){
      closeUploadModal();
      closeNoteModal();
      closeResolverInfo();
      closeLockInfo();
      closeChangesInfo();
    }
  });

  (async function initXmlFilesPage(){
    setArchivedToggleState();
    updateSortIndicators();
    await loadCurrentUserForPermissions();
    await loadXmlFiles();
    showDeferredXmlFilesMessage();
  })();
})();
