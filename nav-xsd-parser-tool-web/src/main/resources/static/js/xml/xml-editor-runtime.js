/**
 * @module xml/xml-editor-runtime
 *
 * A XML-szerkesztési és állománykezelési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

/**
 * XML source/tree rendering, snapshots, save operations, DOM path mutation and form synchronization.
 * Shared runtime state is initialized by runtime-context.js.
 */

function normalizeLayoutWidth(value) {
    const width = Number.parseInt(value, 10);

    if ([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].includes(width)) {
        return width;
    }

    return 6;
}

const XML_SOURCE_HIGHLIGHT_MAX_CHARS = 600000;
const XML_SOURCE_HIGHLIGHT_DEBOUNCE_MS = 140;
let xmlTreeRenderDirty = true;
let xmlSourceRenderDirty = true;
let xmlSourceHighlightDebounceTimeout = null;
let formFieldDelegationInstalled = false;
let formValueDelegationInstalled = false;
let xmlClickDelegationInstalled = false;
let indexedFormDataRef = null;
let formDataValueReferenceIndex = null;
const XML_NODE_PATH_CACHE_MAX = 512;
let xmlNodePathCacheDocument = null;
const xmlNodePathCache = new Map();

/**
 * A <code>markXmlViewsDirty</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 */
function markXmlViewsDirty(){
  xmlTreeRenderDirty = true;
  xmlSourceRenderDirty = true;
}

/**
 * Eltávolítja vagy alaphelyzetbe állítja a reset xml render state művelethez tartozó kliensoldali állapotot.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function resetXmlRenderState(){
  xmlTreeRenderDirty = true;
  xmlSourceRenderDirty = true;
  clearXmlNodePathCache();
  if(xmlSourceHighlightDebounceTimeout){
    clearTimeout(xmlSourceHighlightDebounceTimeout);
    xmlSourceHighlightDebounceTimeout = null;
  }
}

/**
 * Ellenőrzi a is xml pane visible feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function isXmlPaneVisible(){
  return !paneState?.xmlCollapsed && currentViewMode !== 'table';
}

/**
 * A <code>activeMultiformXmlTreeTarget</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function activeMultiformXmlTreeTarget(){
  const state = globalThis.currentMultiformState;
  if(!state?.activePartName || !currentXmlDocument?.documentElement) return null;

  const partName = String(state.activePartName || '').trim();
  if(!partName) return null;

  const matching = Array.from(currentXmlDocument.documentElement.children || [])
    .filter(node => resolveNodeName(node) === partName);
  if(!matching.length) return null;

  const repeating = state.repeatingPart?.name === partName;
  const occurrenceIndex = repeating ? Number(state.selectedIndex) : 1;
  if(repeating && (!Number.isInteger(occurrenceIndex) || occurrenceIndex < 1)){
    return { pendingSelection:true, partName };
  }

  const node = matching[(occurrenceIndex || 1) - 1] || null;
  if(!node) return { pendingSelection:true, partName };
  const rootName = resolveNodeName(currentXmlDocument.documentElement);
  return {
    node,
    path:`/${rootName}/${partName}[${occurrenceIndex || 1}]`,
    partName,
    occurrenceIndex:occurrenceIndex || 1
  };
}

/**
 * Megjeleníti vagy újrarendereli a render xml tree if needed állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} force a függvény force bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function renderXmlTreeIfNeeded(force = false){
  if(!xmlContainer || (!force && !xmlTreeRenderDirty)) return false;
  const target = activeMultiformXmlTreeTarget();
  if(target?.pendingSelection){
    xmlContainer.innerHTML = '<div class="xml-part-selection-placeholder">Válassz egy melléklapot az XML-részlet megjelenítéséhez.</div>';
  }else if(target?.node){
    renderXmlDocumentSubtree(target.node, target.path);
  }else{
    renderXmlDocumentTree(currentXmlDocument);
  }
  xmlTreeRenderDirty = false;
  return true;
}

/**
 * Megjeleníti vagy újrarendereli a render xml source if needed állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} force a függvény force bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function renderXmlSourceIfNeeded(force = false){
  if(!xmlSourceEditor || (!force && !xmlSourceRenderDirty)) return false;
  if(xmlSourceDirtySinceLastApply && !force) return false;
  if(currentActiveXmlFile?.largeFileMode === true){
    xmlSourceEditor.value = 'Nagy XML mód aktív. A teljes XML forrás böngésző oldali megjelenítése tiltott.';
    xmlSourceEditor.readOnly = true;
    xmlSourceDirtySinceLastApply = false;
    xmlSourceRenderDirty = false;
    syncXmlSourceHighlight({ immediate:true });
    return true;
  }
  xmlSourceEditor.readOnly = currentXmlFileReadOnlyMode === true;
  xmlSourceEditor.value = currentXmlDocument ? serializeXml(currentXmlDocument) : '';
  xmlSourceDirtySinceLastApply = false;
  xmlSourceRenderDirty = false;
  syncXmlSourceHighlight({ immediate:true });
  return true;
}

/**
 * A <code>ensureActiveXmlViewRendered</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} view a függvény view bemeneti értéke
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
function ensureActiveXmlViewRendered(view = activeXmlView, options = {}){
  const force = options.force === true;
  if(!isXmlPaneVisible() && !force) return false;
  if(view === 'source') return renderXmlSourceIfNeeded(force);
  return renderXmlTreeIfNeeded(force);
}

/**
 * Megjeleníti vagy újrarendereli a render xml from current state állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
function renderXmlFromCurrentState(options = {}){
  if(options.markDirty !== false) markXmlViewsDirty();
  updateM2mSubmitMenuState();
  return ensureActiveXmlViewRendered(activeXmlView, options);
}

/**
 * Megjeleníti vagy újrarendereli a render xml source highlight now állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function renderXmlSourceHighlightNow(){
  if(!xmlSourceEditor || !xmlSourceHighlight) return;
  const text = xmlSourceEditor.value || '';
  const shell = xmlSourceEditor.closest?.('.xml-editor-shell');
  const disabled = text.length > XML_SOURCE_HIGHLIGHT_MAX_CHARS;
  shell?.classList.toggle('xml-highlight-disabled', disabled);
  if(disabled){
    xmlSourceHighlight.replaceChildren();
    xmlSourceEditor.dataset.highlightDisabled = 'true';
  }else{
    xmlSourceHighlight.innerHTML = highlightXml(text);
    delete xmlSourceEditor.dataset.highlightDisabled;
  }
  syncXmlSourceScroll();
}

/**
 * Szinkronizálja vagy frissíti a sync xml source highlight által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} options a művelet opcionális beállításai
 */
function syncXmlSourceHighlight(options = {}){
  if(!xmlSourceEditor || !xmlSourceHighlight) return;
  if(xmlSourceHighlightDebounceTimeout) clearTimeout(xmlSourceHighlightDebounceTimeout);
  if(options.immediate === true){
    xmlSourceHighlightDebounceTimeout = null;
    renderXmlSourceHighlightNow();
    return;
  }
  xmlSourceHighlightDebounceTimeout = setTimeout(() => {
    xmlSourceHighlightDebounceTimeout = null;
    renderXmlSourceHighlightNow();
  }, XML_SOURCE_HIGHLIGHT_DEBOUNCE_MS);
}

/**
 * Szinkronizálja vagy frissíti a sync xml source scroll által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function syncXmlSourceScroll(){
  if(!xmlSourceEditor || !xmlSourceHighlight) return;
  xmlSourceHighlight.scrollTop = xmlSourceEditor.scrollTop;
  xmlSourceHighlight.scrollLeft = xmlSourceEditor.scrollLeft;
}

/**
 * A <code>highlightXml</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} xml a feldolgozandó XML-tartalom vagy XML DOM-objektum
 * @returns {*} a feldolgozás eredménye
 */
function highlightXml(xml){
  const escaped = escapeHtml(xml);
  return escaped
    .replace(/(&lt;\/?)([A-Za-z_][\w:.-]*)/g, '$1<span class="xml-src-tag">$2</span>')
    .replace(/([A-Za-z_:][\w:.-]*)(=)(&quot;.*?&quot;)/g, '<span class="xml-src-attr">$1</span>$2<span class="xml-src-value">$3</span>');
}

/**
 * A <code>prettyPrintCurrentXml</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function prettyPrintCurrentXml(){
  try{
    if(!xmlSourceEditor) return;
    const doc = parseXmlString(xmlSourceEditor.value);
    xmlSourceEditor.value = formatXmlDocument(doc);
    syncXmlSourceHighlight();
    showMessage('Az XML formázása elkészült.', 'success');
  }catch(error){
    console.error('Pretty print hiba', error);
    showMessage('A pretty print nem sikerült, az XML nem feldolgozható.', 'error');
  }
}

/**
 * Feldolgozza a format xml document bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} doc a függvény doc bemeneti értéke
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
function formatXmlDocument(doc, options = {}){
  const lines = [];
  if(options.includeDeclaration !== false && doc.xmlVersion){
    const encoding = doc.xmlEncoding || 'UTF-8';
    lines.push(`<?xml version="${doc.xmlVersion}" encoding="${encoding}"?>`);
  }
  const root = doc.documentElement;
  if(root) appendPrettyXmlNode(root, 0, lines);
  return lines.join('\n');
}

/**
 * A <code>appendPrettyXmlNode</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} level a függvény level bemeneti értéke
 * @param {*} lines a függvény lines bemeneti értéke
 */
function appendPrettyXmlNode(node, level, lines){
  const indent = '  '.repeat(level);
  switch(node.nodeType){
    case Node.ELEMENT_NODE: {
      const attrs = Array.from(node.attributes || []).map(attr => ` ${attr.name}="${escapeXmlAttribute(attr.value)}"`).join('');
      const childNodes = Array.from(node.childNodes || []);
      const elementChildren = childNodes.filter(child => child.nodeType === Node.ELEMENT_NODE);
      const meaningfulTextNodes = childNodes
        .filter(child => child.nodeType === Node.TEXT_NODE)
        .map(child => child.textContent || '')
        .filter(textValue => textValue.trim().length > 0);
      const hasSpecialChildren = childNodes.some(child => child.nodeType === Node.CDATA_SECTION_NODE || child.nodeType === Node.COMMENT_NODE || child.nodeType === Node.PROCESSING_INSTRUCTION_NODE);

      if(!childNodes.length || (!elementChildren.length && !meaningfulTextNodes.length && !hasSpecialChildren)){
        lines.push(`${indent}<${node.nodeName}${attrs}/>`);
        return;
      }

      if(!elementChildren.length && meaningfulTextNodes.length === 1 && !hasSpecialChildren){
        lines.push(`${indent}<${node.nodeName}${attrs}>${escapeXmlText(meaningfulTextNodes[0])}</${node.nodeName}>`);
        return;
      }

      lines.push(`${indent}<${node.nodeName}${attrs}>`);
      childNodes.forEach(child => {
        if(child.nodeType === Node.TEXT_NODE){
          const textValue = (child.textContent || '').trim();
          if(textValue) lines.push(`${indent}  ${escapeXmlText(textValue)}`);
          return;
        }
        appendPrettyXmlNode(child, level + 1, lines);
      });
      lines.push(`${indent}</${node.nodeName}>`);
      return;
    }
    case Node.CDATA_SECTION_NODE:
      lines.push(`${indent}<![CDATA[${node.nodeValue || ''}]]>`);
      return;
    case Node.COMMENT_NODE:
      lines.push(`${indent}<!--${node.nodeValue || ''}-->`);
      return;
    case Node.PROCESSING_INSTRUCTION_NODE:
      lines.push(`${indent}<?${node.nodeName} ${node.nodeValue || ''}?>`);
      return;
    default:
      return;
  }
}

/**
 * A <code>escapeXmlText</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function escapeXmlText(value){
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

/**
 * A <code>escapeXmlAttribute</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function escapeXmlAttribute(value){
  return escapeXmlText(value)
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

/**
 * A <code>positionXmlSaveMenu</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 */
function positionXmlSaveMenu(){
  if(!xmlSaveMenu || !saveXmlFileButton || xmlSaveMenu.hidden) return;
  const margin = 10;
  const buttonRect = saveXmlFileButton.getBoundingClientRect();
  const menuRect = xmlSaveMenu.getBoundingClientRect();
  const viewportWidth = window.innerWidth || document.documentElement.clientWidth;
  const viewportHeight = window.innerHeight || document.documentElement.clientHeight;
  let left = buttonRect.right - menuRect.width;
  left = Math.max(margin, Math.min(left, viewportWidth - menuRect.width - margin));
  const spaceBelow = viewportHeight - buttonRect.bottom;
  const spaceAbove = buttonRect.top;
  let top;
  if(spaceBelow < menuRect.height + margin && spaceAbove > spaceBelow){
    top = buttonRect.top - menuRect.height - 8;
  } else {
    top = buttonRect.bottom + 8;
  }
  top = Math.max(margin, Math.min(top, viewportHeight - menuRect.height - margin));
  xmlSaveMenu.style.left = `${Math.round(left)}px`;
  xmlSaveMenu.style.top = `${Math.round(top)}px`;
  xmlSaveMenu.style.right = 'auto';
}

/**
 * A <code>toggleXmlSaveMenu</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @param {*} event a feldolgozandó böngészőesemény
 */
function toggleXmlSaveMenu(event){
  event?.stopPropagation?.();
  if(!xmlSaveMenu || !saveXmlFileButton) return;
  closeM2mSubmitMenu();
  const willOpen = xmlSaveMenu.hidden;
  xmlSaveMenu.hidden = !willOpen;
  saveXmlFileButton.setAttribute('aria-expanded', String(willOpen));
  updateXmlSaveMenuState();
  if(willOpen){
    positionXmlSaveMenu();
  }
}

/**
 * Elrejti vagy lezárja a close xml save menu felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 */
function closeXmlSaveMenu(){
  if(!xmlSaveMenu || !saveXmlFileButton) return;
  xmlSaveMenu.hidden = true;
  xmlSaveMenu.style.left = '';
  xmlSaveMenu.style.top = '';
  xmlSaveMenu.style.right = '';
  saveXmlFileButton.setAttribute('aria-expanded', 'false');
}

/**
 * Szinkronizálja vagy frissíti a update xml save menu state által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 */
function updateXmlSaveMenuState(){
  if(!xmlSaveMenu) return;
  const canServerSave = !!currentActiveXmlFile?.id && !!currentActiveXmlFileSessionId && !currentXmlFileReadOnlyMode;
  xmlSaveMenu.querySelectorAll('[data-xml-save-action]').forEach(button => {
    const action = button.dataset.xmlSaveAction;
    button.disabled = ['new-version','save-as','overwrite','diff-preview'].includes(action) && !canServerSave;
    if(button.disabled){
      button.title = currentXmlFileReadOnlyMode ? 'Olvasási módban nincs szerver oldali mentés.' : 'Szerver oldali mentéshez aktív szerkesztési munkamenet szükséges.';
    } else {
      button.removeAttribute('title');
    }
  });
}

/**
 * A <code>positionM2mSubmitMenu</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @returns {*} a feldolgozás eredménye
 */
function positionM2mSubmitMenu(){
  return window.NavM2mFormUi?.positionMenu?.();
}

/**
 * A <code>toggleM2mSubmitMenu</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} event a feldolgozandó böngészőesemény
 * @returns {*} a feldolgozás eredménye
 */
function toggleM2mSubmitMenu(event){
  closeXmlSaveMenu();
  return window.NavM2mFormUi?.toggleMenu?.(event);
}

/**
 * Elrejti vagy lezárja a close m2m submit menu felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @returns {*} a feldolgozás eredménye
 */
function closeM2mSubmitMenu(){
  return window.NavM2mFormUi?.closeMenu?.();
}

/**
 * Szinkronizálja vagy frissíti a update m2m submit menu state által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @returns {*} a feldolgozás eredménye
 */
function updateM2mSubmitMenuState(){
  return window.NavM2mFormUi?.updateMenuState?.();
}

/**
 * Kezeli vagy beköti a handle m2m form action esemény- és inicializációs folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} action a függvény action bemeneti értéke
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function handleM2mFormAction(action){
  return window.NavM2mFormUi?.handleAction?.(action);
}

/**
 * Kezeli vagy beköti a handle m2m attachment input change esemény- és inicializációs folyamatát.
 *
 * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function handleM2mAttachmentInputChange(){
  return window.NavM2mFormUi?.handleAttachmentInputChange?.();
}

/**
 * Kezeli vagy beköti a handle xml save action esemény- és inicializációs folyamatát.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @param {*} action a függvény action bemeneti értéke
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function handleXmlSaveAction(action){
  closeXmlSaveMenu();
  if(action === 'download'){
    saveXmlFile();
    return;
  }
  if(action === 'diff-preview'){
    await previewServerXmlDiff();
    return;
  }
  if(action === 'new-version'){
    await saveXmlToServer('new-version');
    return;
  }
  if(action === 'save-as'){
    await saveXmlToServer('save-as');
    return;
  }
  if(action === 'overwrite'){
    await saveXmlToServer('overwrite');
  }
}

/**
 * A <code>currentSerializedXmlForSave</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @returns {*} a feldolgozás eredménye
 */
function currentSerializedXmlForSave(){
  return buildCurrentXmlSnapshotForSave().xml;
}

/**
 * Feldolgozza a format xml for persistence bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} xml a feldolgozandó XML-tartalom vagy XML DOM-objektum
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
function formatXmlForPersistence(xml, options = {}){
  const text = String(xml || '').trim();
  if(!text) return '';
  const doc = parseXmlString(text);
  return formatXmlDocument(doc, options);
}

/**
 * Feldolgozza a collect current form control snapshot bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function collectCurrentFormControlSnapshot(){
  const selector = [
    '.form-field input[data-field-id]',
    '.form-field select[data-field-id]',
    '.form-field textarea[data-field-id]',
    '.uimodel-field input[data-field-id]',
    '.uimodel-field select[data-field-id]',
    '.uimodel-field textarea[data-field-id]'
  ].join(',');
  const controls = [];
  const seen = new Set();
  document.querySelectorAll(selector).forEach(control => {
    if(!control || control.id === 'xmlSourceEditor' || seen.has(control)) return;
    seen.add(control);
    const fieldId = control.dataset.fieldId || '';
    const wrapper = control.closest('.form-field, .uimodel-field');
    const xmlPath = wrapper?.dataset?.xmlPath || control.dataset.xmlPath || currentFormData?.valuesByFieldId?.[fieldId]?.xmlPath || '';
    const value = readControlValueForXmlSnapshot(control);
    controls.push({
      fieldId,
      xmlPath,
      value,
      empty: isEmptyXmlFieldValue(control, value),
      control,
      wrapper
    });
  });
  return controls;
}

/**
 * Betölti vagy lekéri a read control value for xml snapshot művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} control a függvény control bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function readControlValueForXmlSnapshot(control){
  if(!control) return '';
  if(control.type === 'checkbox') return control.checked ? 'true' : 'false';
  return control.value ?? '';
}

/**
 * Ellenőrzi a has unsynced form control changes feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function hasUnsyncedFormControlChanges(){
  if(!currentXmlDocument) return false;
  return collectCurrentFormControlSnapshot().some(item => {
    const xmlPath = String(item.xmlPath || '').trim();
    if(!xmlPath) return false;
    const targetPath = canonicalizeXmlPath(xmlPath) || xmlPath;
    const node = findNodeByPath(currentXmlDocument, targetPath);
    if(item.empty) return !!node;
    if(!node) return true;
    return String(node.textContent ?? '') !== String(item.value ?? '');
  });
}

/**
 * Szinkronizálja vagy frissíti a sync form controls to document által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} doc a függvény doc bemeneti értéke
 * @param {*} controls a függvény controls bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function syncFormControlsToDocument(doc, controls = collectCurrentFormControlSnapshot()){
  if(!doc){
    return { controlCount: controls.length, updated: 0, created: 0, removed: 0, skipped: controls.length, reason: 'NO_XML_DOCUMENT' };
  }
  let updated = 0;
  let created = 0;
  let removed = 0;
  let skipped = 0;
  controls.forEach(item => {
    const xmlPath = String(item.xmlPath || '').trim();
    if(!xmlPath){
      skipped++;
      return;
    }
    const targetPath = canonicalizeXmlPath(xmlPath) || xmlPath;
    if(item.empty){
      if(removeXmlNodeByPath(doc, targetPath)) removed++;
      return;
    }
    const existed = !!findNodeByPath(doc, targetPath);
    const node = setXmlValueByPath(doc, targetPath, item.value, { createMissing: true });
    if(!node){
      skipped++;
      return;
    }
    if(existed) updated++; else created++;
  });
  return { controlCount: controls.length, updated, created, removed, skipped };
}

/**
 * Szinkronizálja vagy frissíti a sync current form controls to xml document által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
function syncCurrentFormControlsToXmlDocument(options = {}){
  const controls = collectCurrentFormControlSnapshot();
  if(!currentXmlDocument){
    return { controlCount: controls.length, updated: 0, created: 0, removed: 0, skipped: controls.length, reason: 'NO_XML_DOCUMENT' };
  }

  let updated = 0;
  let created = 0;
  let removed = 0;
  let skipped = 0;

  controls.forEach(item => {
    const fieldId = item.fieldId || '';
    const xmlPath = item.xmlPath || '';
    if(!fieldId || !xmlPath){
      skipped++;
      return;
    }

    const targetPath = canonicalizeXmlPath(xmlPath) || xmlPath;

    if(item.empty){
      const didRemove = removeXmlNodeByPath(currentXmlDocument, targetPath);
      if(didRemove) removed++;
      updateFormDataValueReferences(fieldId, targetPath, '', false);
      if(options.updateWrappers !== false && item.wrapper){
        item.wrapper.dataset.xmlPath = targetPath;
        item.wrapper.classList.add('uimodel-missing-field');
      }
      if(selectedXmlPath && targetPath && pathMatches(selectedXmlPath, targetPath)){
        selectedXmlPath = null;
      }
      return;
    }

    const existed = !!findNodeByPath(currentXmlDocument, targetPath);
    const updatedNode = setXmlValueByPath(currentXmlDocument, targetPath, item.value, { createMissing: true });
    if(!updatedNode){
      skipped++;
      return;
    }

    if(existed) updated++; else created++;
    const updatedPath = canonicalizeXmlPath(targetPath) || targetPath;
    updateFormDataValueReferences(fieldId, updatedPath, item.value, true);
    if(options.updateWrappers !== false && item.wrapper){
      item.wrapper.dataset.xmlPath = updatedPath;
      item.wrapper.classList.remove('uimodel-missing-field');
    }
  });

  return { controlCount: controls.length, updated, created, removed, skipped };
}

/**
 * Szinkronizálja vagy frissíti a sync active multiform detail to current document által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function syncActiveMultiformDetailToCurrentDocument(){
  if(!currentXmlDocument || currentActiveXmlFile?.largeFileMode === true) return { synced:false, reason:'NOT_APPLICABLE' };
  const state = currentMultiformState;
  const partName = String(state?.repeatingPart?.name || '').trim();
  const row = state?.detailRow || null;
  const occurrenceIndex = Number(row?.index || state?.selectedIndex || 0);
  if(!partName || !row?.element || !Number.isInteger(occurrenceIndex) || occurrenceIndex < 1){
    return { synced:false, reason:'NO_ACTIVE_MULTIFORM_DETAIL' };
  }

  const root = currentXmlDocument.documentElement;
  const matches = Array.from(root?.children || []).filter(child => resolveNodeName(child) === partName);
  const target = matches[occurrenceIndex - 1];
  if(!target){
    return { synced:false, reason:'TARGET_OCCURRENCE_NOT_FOUND', occurrenceIndex, partName };
  }

  const replacement = currentXmlDocument.importNode(row.element, true);
  target.replaceWith(replacement);
  clearXmlNodePathCache();
  return { synced:true, occurrenceIndex, partName };
}

/**
 * Feldolgozza a build current xml snapshot for save bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
function buildCurrentXmlSnapshotForSave(options = {}){
  const { preferSourceEdits = true, flushPendingRender = true } = options;
  if(flushPendingRender && xmlRenderDebounceTimeout){
    clearTimeout(xmlRenderDebounceTimeout);
    xmlRenderDebounceTimeout = null;
    if(currentXmlDocument && shouldRenderXmlLive()){
      ensureActiveXmlViewRendered();
    }
  }

  const sourceText = xmlSourceEditor?.value || '';
  const sourceTrimmed = sourceText.trim();
  const sourceUsable = sourceTrimmed && !sourceTrimmed.startsWith('Nagy XML mód aktív.');

  if(preferSourceEdits && xmlSourceDirtySinceLastApply && sourceUsable){
    const formattedXml = formatXmlForPersistence(sourceText);
    if(xmlSourceEditor && xmlSourceEditor.value !== formattedXml){
      xmlSourceEditor.value = formattedXml;
      syncXmlSourceHighlight({ immediate:true });
    }
    lastXmlSnapshotInfo = createCurrentXmlSnapshotInfo(formattedXml, 'XML_SOURCE_EDITOR_DIRTY');
    window.__NAV_LAST_XML_SNAPSHOT__ = lastXmlSnapshotInfo;
    return lastXmlSnapshotInfo;
  }

  if(currentXmlDocument){
    const multiformSync = syncActiveMultiformDetailToCurrentDocument();
    const controls = collectCurrentFormControlSnapshot();
    const saveDocument = parseXmlString(serializeXml(currentXmlDocument));
    const sync = syncFormControlsToDocument(saveDocument, controls);
    sync.multiform = multiformSync;
    currentXmlDocument = saveDocument;
    clearXmlNodePathCache();
    const xml = formatXmlForPersistence(serializeXml(currentXmlDocument));
    if(xmlSourceEditor && !xmlSourceDirtySinceLastApply){
      if(activeXmlView === 'source' && isXmlPaneVisible()){
        if(xmlSourceEditor.value !== xml) xmlSourceEditor.value = xml;
        xmlSourceRenderDirty = false;
        syncXmlSourceHighlight();
      }else{
        xmlSourceRenderDirty = true;
      }
    }
    lastXmlSnapshotInfo = createCurrentXmlSnapshotInfo(xml, 'CURRENT_XML_DOCUMENT', { sync });
    window.__NAV_LAST_XML_SNAPSHOT__ = lastXmlSnapshotInfo;
    return lastXmlSnapshotInfo;
  }

  if(sourceUsable){
    const formattedXml = formatXmlForPersistence(sourceText);
    if(xmlSourceEditor && xmlSourceEditor.value !== formattedXml){
      xmlSourceEditor.value = formattedXml;
      syncXmlSourceHighlight({ immediate:true });
    }
    lastXmlSnapshotInfo = createCurrentXmlSnapshotInfo(formattedXml, 'XML_SOURCE_EDITOR');
    window.__NAV_LAST_XML_SNAPSHOT__ = lastXmlSnapshotInfo;
    return lastXmlSnapshotInfo;
  }

  lastXmlSnapshotInfo = createCurrentXmlSnapshotInfo('', 'EMPTY');
  window.__NAV_LAST_XML_SNAPSHOT__ = lastXmlSnapshotInfo;
  return lastXmlSnapshotInfo;
}

/**
 * Előkészíti és elindítja a create current xml snapshot info állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} xml a feldolgozandó XML-tartalom vagy XML DOM-objektum
 * @param {*} source a függvény source bemeneti értéke
 * @param {*} extra a függvény extra bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function createCurrentXmlSnapshotInfo(xml, source, extra = {}){
  const text = String(xml || '');
  return {
    xml: text,
    source,
    byteLength: new Blob([text]).size,
    charLength: text.length,
    hash: lightweightStringHash(text),
    dirty: currentFormHasUnsavedChanges === true,
    xmlSourceDirty: xmlSourceDirtySinceLastApply === true,
    hasXmlDocument: !!currentXmlDocument,
    activeXmlFileId: currentActiveXmlFile?.id || null,
    sessionId: currentActiveXmlFileSessionId || null,
    createdAt: new Date().toISOString(),
    controlCount: Number(extra?.sync?.controlCount ?? collectCurrentFormControlSnapshot().length ?? 0),
    sync: extra?.sync || null
  };
}

/**
 * A <code>lightweightStringHash</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} text a függvény text bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function lightweightStringHash(text){
  let hash = 2166136261;
  for(let i = 0; i < text.length; i += 1){
    hash ^= text.charCodeAt(i);
    hash = Math.imul(hash, 16777619);
  }
  return (hash >>> 0).toString(16).padStart(8, '0');
}

/**
 * A <code>serverSaveOptions</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @param {*} action a függvény action bemeneti értéke
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function serverSaveOptions(action){
  const runXsd = await window.navConfirm?.({
    title: 'XSD validáció mentés előtt',
    message: 'Szeretnéd elindítani az XSD validációt a mentés után?',
    confirmText: 'Igen, indítsa',
    cancelText: 'Nem szükséges',
    variant: 'default'
  });
  if(action === 'overwrite'){
    const ok = await window.navConfirm?.({
      title: 'Meglévő XML felülírása',
      message: 'A szerver oldali XML felülírása előtt backup készül. Biztosan folytatod?',
      confirmText: 'Felülírás',
      cancelText: 'Mégsem',
      variant: 'warning'
    });
    if(!ok) return null;
  }
  if((action === 'new-version' || action === 'save-as') && window.navFormPrompt){
    const values = await window.navFormPrompt({
      title: action === 'save-as' ? 'Mentés másként' : 'Új verzió mentése',
      message: action === 'save-as' ? 'Add meg az új, önálló XML állomány fájlnevét és opcionális megjegyzését.' : 'Add meg az új XML fájlnevét és opcionálisan a mentési megjegyzést. Ha a fájlnevet változatlanul hagyod, a rendszer automatikus verziónevet készít.',
      confirmText: 'Mentés',
      cancelText: 'Mégsem',
      fields: [
        { name:'fileName', label:'Új fájlnév', type:'text', required:true, maxLength:255, defaultValue:currentActiveXmlFile?.fileName || '', placeholder:'pelda_uj_verzio.xml' },
        { name:'userNote', label:'Mentési megjegyzés', type:'textarea', maxLength:1000, rows:4, placeholder:'pl. javított mezőértékek, ügyfél-visszajelzés alapján' }
      ]
    });
    if(!values) return null;
    return {
      runXsdValidation: !!runXsd,
      userNote: String(values.userNote || '').trim() || null,
      newFileName: String(values.fileName || '').trim() || null
    };
  }
  const note = await window.navPrompt?.({
    title: 'Felülírás megjegyzése',
    message: 'Opcionális mentési megjegyzés adható meg.',
    label: 'Mentési megjegyzés',
    placeholder: 'pl. javított mezőértékek, ügyfél-visszajelzés alapján',
    confirmText: 'Tovább',
    cancelText: 'Kihagyás'
  });
  return { runXsdValidation: !!runXsd, userNote: note || null, newFileName:null };
}

/**
 * A <code>requireActiveEditableXmlForServerSave</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @returns {*} a feldolgozás eredménye
 */
function requireActiveEditableXmlForServerSave(){
  if(!currentActiveXmlFile?.id || !currentActiveXmlFileSessionId){
    showMessage('Szerver oldali mentéshez előbb nyiss meg egy Űrlapállományt szerkesztésre.', 'warning');
    return false;
  }
  if(currentXmlFileReadOnlyMode){
    showMessage('Olvasási módban megnyitott XML nem menthető.', 'warning');
    return false;
  }
  return true;
}


/**
 * A <code>activeLargeXmlFragmentSaveTarget</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @returns {*} a feldolgozás eredménye
 */
function activeLargeXmlFragmentSaveTarget(){
  if(currentActiveXmlFile?.largeFileMode !== true) return null;
  const state = globalThis.currentMultiformState;
  if(!state?.repeatingPart || state.activePartName !== state.repeatingPart.name) return null;
  const occurrenceIndex = Number(state.selectedIndex || 0);
  const row = state.repeatingPart.rows?.find(item => Number(item.index) === occurrenceIndex) || state.detailRow || null;
  if(!row?.element || !Number.isInteger(occurrenceIndex) || occurrenceIndex < 1) return null;
  return {
    formName:String(state.repeatingPart.name || ''),
    occurrenceIndex,
    xmlFragment:formatXmlForPersistence(new XMLSerializer().serializeToString(row.element), { includeDeclaration:false })
  };
}

/**
 * Megjeleníti vagy újrarendereli a show large xml save progress állapotát a felhasználói felületen.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @param {*} job a függvény job bemeneti értéke
 */
function showLargeXmlSaveProgress(job){
  let overlay = document.getElementById('largeXmlSaveProgressOverlay');
  if(!overlay){
    overlay = document.createElement('div');
    overlay.id = 'largeXmlSaveProgressOverlay';
    overlay.className = 'large-xml-process-overlay';
    overlay.innerHTML = `
      <div class="large-xml-process-dialog" role="status" aria-live="polite">
        <div class="large-xml-process-spinner"></div>
        <h2>Nagy XML melléklap mentése</h2>
        <p class="large-xml-save-progress-message">A mentés előkészítése.</p>
        <progress class="large-xml-save-progress-bar" max="100" value="0" style="width:100%;height:18px"></progress>
        <div class="large-xml-save-progress-percent" style="margin-top:.5rem;font-weight:600">0%</div>
      </div>`;
    document.body.appendChild(overlay);
  }
  const percent = Math.max(0, Math.min(100, Number(job?.progressPercent || 0)));
  overlay.querySelector('.large-xml-save-progress-message').textContent = job?.progressMessage || 'A mentés folyamatban...';
  overlay.querySelector('.large-xml-save-progress-bar').value = percent;
  overlay.querySelector('.large-xml-save-progress-percent').textContent = `${percent}%`;
  overlay.hidden = false;
}

/**
 * Elrejti vagy lezárja a hide large xml save progress felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 */
function hideLargeXmlSaveProgress(){
  const overlay = document.getElementById('largeXmlSaveProgressOverlay');
  if(overlay) overlay.hidden = true;
}

/**
 * A <code>waitForLargeXmlSaveJob</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @param {*} initialJob a függvény initialJob bemeneti értéke
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function waitForLargeXmlSaveJob(initialJob){
  let job = initialJob;
  showLargeXmlSaveProgress(job);
  while(job && !['FINISHED','FAILED','CANCELLED'].includes(String(job.status || ''))){
    await new Promise(resolve => setTimeout(resolve, 750));
    const response = await fetch('/api/jobs/status', { method:'POST', credentials:'same-origin', headers:{'Content-Type':'application/json'}, body:JSON.stringify({jobId:String(job.jobId || '')}) });
    const data = await response.json().catch(() => ({}));
    if(!response.ok) throw new Error(data.message || data.error || 'A mentési folyamat állapota nem kérdezhető le.');
    job = data;
    showLargeXmlSaveProgress(job);
  }
  if(job?.status !== 'FINISHED') throw new Error(job?.errorMessage || job?.progressMessage || 'A nagy XML mentése sikertelen.');
  return job;
}

/**
 * Előkészíti és elindítja a save large xml selected fragment állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @param {*} userNote a függvény userNote bemeneti értéke
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function saveLargeXmlSelectedFragment(userNote){
  const target = activeLargeXmlFragmentSaveTarget();
  if(!target){
    showMessage('Nagy XML módban válassz ki egy melléklapot a célzott mentéshez.', 'warning');
    return false;
  }
  showLargeXmlSaveProgress({ progressPercent:0, progressMessage:'A mentési folyamat indítása.' });
  try{
    const response = await fetch(`/api/xml-files/${encodeURIComponent(currentActiveXmlFile.id)}/large-multiform/save-fragment`, {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      credentials:'same-origin',
      body:JSON.stringify({
        formName:target.formName,
        occurrenceIndex:target.occurrenceIndex,
        xmlFragment:target.xmlFragment,
        sessionId:currentActiveXmlFileSessionId,
        sourceFileSize:currentActiveXmlFile.fileSizeBytes || null,
        sourceLastModified:currentActiveXmlFile.updatedAtMillis || null,
        userNote:userNote || null
      })
    });
    const job = await response.json().catch(() => ({}));
    if(!response.ok) throw new Error(job.message || job.error || 'A nagy XML mentési folyamat nem indítható.');
    const finished = await waitForLargeXmlSaveJob(job);
    markFormClean();
    globalThis.currentMultiformState?.markActivePanelSaved?.();
    showMessage(finished.progressMessage || 'A melléklap mentése sikeresen befejeződött.', 'success');
    return true;
  }catch(error){
    showMessage(error.message || 'A nagy XML melléklap mentése sikertelen.', 'error');
    return false;
  }finally{
    hideLargeXmlSaveProgress();
  }
}

/**
 * Előkészíti és elindítja a save xml to server állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @param {*} action a függvény action bemeneti értéke
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function saveXmlToServer(action){
  if(!requireActiveEditableXmlForServerSave()) return;
  const xmlContent = currentSerializedXmlForSave();
  if(!xmlContent.trim()){
    showMessage('Nincs menthető XML tartalom.', 'error');
    return;
  }
  const invalidOk = await confirmSaveWithXsdErrorsIfNeeded();
  if(!invalidOk) return;
  const options = await serverSaveOptions(action);
  if(!options) return;
  if(currentActiveXmlFile?.largeFileMode === true){
    await saveLargeXmlSelectedFragment(options.userNote);
    return;
  }
  const endpoint = action === 'overwrite' ? 'overwrite' : (action === 'save-as' ? 'save-as' : 'save-new-version');
  try{
    const response = await fetch(`/api/xml-files/${encodeURIComponent(currentActiveXmlFile.id)}/${endpoint}`, {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify({
        xmlContent,
        sessionId: currentActiveXmlFileSessionId,
        runXsdValidation: options.runXsdValidation,
        allowInvalidXml: true,
        userNote: options.userNote,
        newFileName: options.newFileName || null
      })
    });
    if(!response.ok){
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || error.error || 'A szerver oldali mentés nem sikerült.');
    }
    const result = await response.json();
    if(action === 'new-version' && result.fileName){
      currentActiveXmlFile = {
        ...(currentActiveXmlFile || {}),
        fileName:result.fileName,
        filePath:result.targetFilePath || currentActiveXmlFile?.filePath
      };
      updateFormHeaderTitle(currentFormDefinition, currentSchemaBundle || null);
      persistUiState();
    }
    markFormClean();
    globalThis.currentMultiformState?.markActivePanelSaved?.();
    const noteInfo = options.userNote ? ' A mentési megjegyzés az Űrlapállományok Változások/Revíziók nézetében látható.' : '';
    if(action === 'save-as'){ showMessage(`Az XML új állományként elmentve: ${result.fileName || '-'}.`, 'success'); return; }
    showMessage(`${result.message || 'Mentés kész'} Fájl: ${result.fileName || '-'}, revízió: ${result.revisionNo}, módosítások: ${result.changeCount}.${noteInfo}`, 'success');
  }catch(error){
    showMessage(error.message || 'A szerver oldali mentés nem sikerült.', 'error');
  }
}

/**
 * A <code>quickSaveCurrentXmlFile</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @param {*} options a művelet opcionális beállításai
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function quickSaveCurrentXmlFile(options = {}){
  const { quiet = false, allowDisabledButton = false, skipIfClean = true } = options;
  if(!quickSaveXmlFileButton) return false;
  if(quickSaveXmlFileButton.disabled && !allowDisabledButton) return false;
  if(!requireActiveEditableXmlForServerSave()) return false;
  if(skipIfClean && !currentFormHasUnsavedChanges && !hasUnsyncedFormControlChanges()){
    if(!quiet) showMessage('Nincs mentetlen módosítás.', 'info');
    return true;
  }
  if(currentActiveXmlFile?.largeFileMode === true){
    quickSaveXmlFileButton.disabled = true;
    quickSaveXmlFileButton.classList.add('is-saving');
    try{
      return await saveLargeXmlSelectedFragment('Nagy XML célzott melléklap gyorsmentése');
    }finally{
      quickSaveXmlFileButton.classList.remove('is-saving');
      updateQuickSaveXmlButtonState();
    }
  }
  const xmlContent = currentSerializedXmlForSave();
  if(!xmlContent.trim()){
    showMessage('Nincs gyorsmenthető XML tartalom.', 'error');
    return false;
  }
  quickSaveXmlFileButton.disabled = true;
  quickSaveXmlFileButton.classList.add('is-saving');
  try{
    const response = await fetch(`/api/xml-files/${encodeURIComponent(currentActiveXmlFile.id)}/overwrite`, {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify({
        xmlContent,
        sessionId: currentActiveXmlFileSessionId,
        runXsdValidation: true,
        allowInvalidXml: true,
        userNote: 'Gyorsmentés az űrlapnézetből'
      })
    });
    if(!response.ok){
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || error.error || 'A gyorsmentés nem sikerült.');
    }
    const result = await response.json().catch(() => ({}));
    markFormClean();
    globalThis.currentMultiformState?.markActivePanelSaved?.();
    if(!quiet){
      showMessage(`${result.message || 'Gyorsmentés kész'}${result.revisionNo ? ' Revision: ' + result.revisionNo : ''}`, 'success');
    }
    return true;
  }catch(error){
    showMessage(error.message || 'A gyorsmentés nem sikerült.', 'error');
    return false;
  }finally{
    quickSaveXmlFileButton.classList.remove('is-saving');
    updateQuickSaveXmlButtonState();
  }
}

/**
 * Szinkronizálja vagy frissíti a update quick save xml button state által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 */
function updateQuickSaveXmlButtonState(){
  if(!quickSaveXmlFileButton) return;
  const enabled = !!currentActiveXmlFile?.id && !!currentActiveXmlFileSessionId && !currentXmlFileReadOnlyMode;
  quickSaveXmlFileButton.disabled = !enabled;
  quickSaveXmlFileButton.title = enabled
    ? 'Gyorsmentés: aktuális űrlapállapot mentése (Ctrl+M)'
    : 'Gyorsmentés csak szerkesztésre megnyitott Űrlapállománynál érhető el';
}

/**
 * A <code>previewServerXmlDiff</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function previewServerXmlDiff(){
  if(!requireActiveEditableXmlForServerSave()) return;
  const xmlContent = currentSerializedXmlForSave();
  if(!xmlContent.trim()){
    showMessage('Nincs összehasonlítható XML tartalom.', 'error');
    return;
  }
  try{
    const response = await fetch(`/api/xml-files/${encodeURIComponent(currentActiveXmlFile.id)}/diff-preview`, {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify({ xmlContent, sessionId: currentActiveXmlFileSessionId })
    });
    if(!response.ok){
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || error.error || 'A diff előnézet nem sikerült.');
    }
    const data = await response.json();
    showXmlDiffPreview(data);
  }catch(error){
    showMessage(error.message || 'A diff előnézet nem sikerült.', 'error');
  }
}

/**
 * Megjeleníti vagy újrarendereli a show xml diff preview állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} data a függvény data bemeneti értéke
 */
function showXmlDiffPreview(data){
  const host = document.getElementById('appDialogHost') || (() => { const h = document.createElement('div'); h.id = 'appDialogHost'; document.body.appendChild(h); return h; })();
  const modal = document.createElement('div');
  modal.className = 'app-dialog-modal xml-diff-modal';
  const entries = Array.isArray(data?.entries) ? data.entries : [];
  modal.innerHTML = `
    <div class="app-dialog-backdrop" data-close="true"></div>
    <section class="app-dialog-card" role="dialog" aria-modal="true" aria-labelledby="xmlDiffTitle">
      <button type="button" class="app-dialog-close" data-close="true" aria-label="Bezárás">×</button>
      <p class="eyebrow">XML DIFF</p>
      <h2 id="xmlDiffTitle">Diff előnézet</h2>
      <p class="xml-diff-summary">${escapeHtml(data?.fileName || '')} — ${entries.length} módosítás</p>
      <div class="xml-diff-table-wrap">
        <table class="xml-diff-table">
          <thead><tr><th>Típus</th><th>XML path</th><th>Régi érték</th><th>Új érték</th></tr></thead>
          <tbody>${entries.length ? entries.map(entry => `
            <tr>
              <td><span class="xml-diff-type ${escapeAttr(entry.changeType || '')}">${escapeHtml(entry.changeType || '')}</span></td>
              <td>${escapeHtml(entry.xmlPath || '')}</td>
              <td>${escapeHtml(entry.oldValue || '')}</td>
              <td>${escapeHtml(entry.newValue || '')}</td>
            </tr>`).join('') : '<tr><td colspan="4">Nincs változás.</td></tr>'}</tbody>
        </table>
      </div>
      <div class="app-dialog-actions"><button type="button" class="primary" data-close="true">Bezárás</button></div>
    </section>`;
  host.appendChild(modal);
  modal.addEventListener('click', event => { if(event.target?.dataset?.close){ modal.remove(); } });
}

/**
 * Előkészíti és elindítja a save xml file állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 */
function saveXmlFile(){
  if(!currentXmlDocument && !(xmlSourceEditor && xmlSourceEditor.value && xmlSourceEditor.value.trim())){
    showMessage('Nincs menthető XML tartalom.', 'error');
    return;
  }
  const xml = currentSerializedXmlForSave();
  const blob = new Blob([xml], { type: 'application/xml;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = suggestXmlFileName();
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
  showMessage('Az XML fájl letöltése elindult.', 'success');
}

/**
 * A <code>suggestXmlFileName</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function suggestXmlFileName(){
  const fromPath = (xmlPathInput?.value || '').trim().split(/[\\/]/).pop();
  const fromUpload = xmlFileInput?.files?.[0]?.name;
  return fromPath || fromUpload || 'generated.xml';
}

/**
 * Ellenőrzi a validate current xml feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function validateCurrentXml(){
  // Form validation must always use the current in-memory XML snapshot.
  // Saving is a separate user action; unsaved edits must be validated as they are visible on the form.
  await validateCurrentXmlInBrowserContext();
}

/**
 * Ellenőrzi a validate current xml in browser context feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function validateCurrentXmlInBrowserContext(){
  const viewportState = captureFormViewportState();
  try{
    rememberFormFocusTarget(viewportState.activeElement);
    const editorText = xmlSourceEditor?.value || '';
    const xmlText = xmlSourceDirtySinceLastApply && editorText.trim()
      ? editorText
      : currentSerializedXmlForSave();
    parseXmlString(xmlText);
    const requestData = new FormData();
    if(schemaDirInput.value.trim()) requestData.append('schemaDir', schemaDirInput.value.trim());
    if(generalXsdDirInput.value.trim()) requestData.append('generalXsdDir', generalXsdDirInput.value.trim());
    const fileName = suggestXmlFileName();
    requestData.append('xmlFile', new File([xmlText], fileName, { type: 'application/xml' }));
    validateCurrentXmlButton.disabled = true;
    ensureFormXsdValidationDrawer();
    openFormXsdValidationDrawer();
    updateFormXsdDrawerTab('running', 'Fut...', 'Az XSD ellenőrzés az aktuális, mentetlen űrlapállapoton fut...');
    window.NavProcessingJobs?.showLocal?.({
      title: 'XSD ellenőrzés',
      message: 'Az aktuális, mentetlen űrlapállapot XSD ellenőrzése folyamatban...',
      status: 'Folyamatban',
      percentText: '',
      progressWidth: '45%'
    });
    const response = await fetch('/api/validate', { method:'POST', body: requestData });
    const data = await response.json();
    if(!response.ok) throw new Error(data.error || 'Az XML validáció nem sikerült.');
    if(data.valid){
      showMessage('Az aktuális XML valid.', 'success');
    } else {
      showMessage('Az aktuális XML nem valid. Lásd az XSD fület.', 'error');
    }
    clearResults();
    renderValidate(data, {
      preserveExistingFormOnInvalid: true,
      preserveCurrentFormState: true,
      skipTabActivation: true,
      renderInlineXsdResult: true
    });
  }catch(error){
    console.error('Aktuális XML validációs hiba', error);
    showMessage(error.message || 'Az aktuális XML validációja nem sikerült.', 'error');
  }finally{
    window.NavProcessingJobs?.hide?.();
    validateCurrentXmlButton.disabled = false;
    restoreFormViewportState(viewportState);
  }
}

/**
 * Szinkronizálja vagy frissíti a set xsd validation buttons disabled által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} disabled a függvény disabled bemeneti értéke
 */
function setXsdValidationButtonsDisabled(disabled){
  if(validateCurrentXmlButton) validateCurrentXmlButton.disabled = disabled;
  const drawerValidate = document.getElementById('formXsdValidationDrawerValidate');
  if(drawerValidate) drawerValidate.disabled = disabled;
}

/**
 * Elindítja a start active file xsd validation aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} options a művelet opcionális beállításai
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function startActiveFileXsdValidation(options = {}){
  const openDrawerOnResult = options.openDrawerOnResult !== false;
  try{
    setXsdValidationButtonsDisabled(true);
    updateFormXsdDrawerTab('running', 'Fut', 'XSD validáció indítása aktív Űrlapállományon...');
    const response = await fetch('/api/xsd-validation/active/start', { method:'POST', credentials:'same-origin' });
    const data = await response.json().catch(() => ({}));
    if(!response.ok){
      if(response.status === 409 && data.activeJobId){
        showMessage(data.error || 'Már fut feldolgozás. Az aktív job megjelenik.', 'error');
        window.NavProcessingJobs?.startPolling?.(data.activeJobId);
        if(String(data.activeJobType || '').toUpperCase() === 'XSD_VALIDATION'){
          await waitForXsdValidationJobAndLoadLatest(data.activeJobId, { openDrawerOnResult });
        }
        return;
      }
      throw new Error(data.message || data.error || 'Az XSD validáció indítása sikertelen.');
    }
    showMessage(`XSD validáció elindult: ${data.jobId}`, 'success');
    window.NavProcessingJobs?.startPolling?.(data.jobId);
    await waitForXsdValidationJobAndLoadLatest(data.jobId, { openDrawerOnResult });
  }catch(error){
    console.error('Aktív állomány XSD validációs hiba', error);
    updateFormXsdDrawerTab('error', 'Hiba', error.message || 'XSD validációs hiba');
    showMessage(error.message || 'Az XSD validáció indítása nem sikerült.', 'error');
  }finally{
    setXsdValidationButtonsDisabled(false);
  }
}

/**
 * A <code>waitForXsdValidationJobAndLoadLatest</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} jobId a célobjektum technikai azonosítója
 * @param {*} options a művelet opcionális beállításai
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function waitForXsdValidationJobAndLoadLatest(jobId, options = {}){
  const terminal = new Set(['FINISHED','FAILED','CANCELLED']);
  for(let i=0; i<720; i++){
    await new Promise(resolve => setTimeout(resolve, 1000));
    const response = await fetch(`/api/jobs/${encodeURIComponent(jobId)}`, { credentials:'same-origin', cache:'no-store' });
    const job = await response.json().catch(() => ({}));
    if(!response.ok) throw new Error(job.message || job.error || 'A job státusz nem kérdezhető le.');
    if(terminal.has(String(job.status || '').toUpperCase())){
      await loadLatestActiveXsdValidationResult(options);
      return;
    }
  }
}

/**
 * Betölti vagy lekéri a load latest active xsd validation result művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} options a művelet opcionális beállításai
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function loadLatestActiveXsdValidationResult(options = {}){
  const response = await fetch('/api/xsd-validation/active/latest', { credentials:'same-origin', cache:'no-store' });
  const data = await response.json().catch(() => ({}));
  if(!response.ok) throw new Error(data.message || data.error || 'Az XSD validációs eredmény nem tölthető be.');
  updateFormXsdValidationDrawerFromStoredResult(data, options);
}

/**
 * A <code>autoStartXsdValidationAfterActiveXmlOpen</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function autoStartXsdValidationAfterActiveXmlOpen(){
  if(!currentActiveXmlFile?.id) return;
  try{
    await startActiveFileXsdValidation({ openDrawerOnResult: false });
  }catch(error){
    console.warn('Automatikus XSD validáció indítása sikertelen.', error);
  }
}

/**
 * Megjeleníti vagy újrarendereli a render xml view állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} xmlView a feldolgozandó XML-tartalom vagy XML DOM-objektum
 */
function renderXmlView(xmlView){
  xmlContainer.innerHTML = '';
  if(!xmlView || !xmlView.root){
    xmlContainer.innerHTML = '<p>Nincs XML nézet.</p>';
    return;
  }
  xmlContainer.innerHTML = renderXmlNode(xmlView.root);
  bindXmlClicks();
  highlightSelections();
}

/**
 * Megjeleníti vagy újrarendereli a render xml node állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @returns {*} a feldolgozás eredménye
 */
function renderXmlNode(node){
  const selectedClass = selectedXmlPath === node.path ? ' selected' : '';
  const attrs = Object.entries(node.attributes || {}).map(([k,v]) => ` <span class="xml-attr">${escapeHtml(k)}</span>="<span class="xml-value">${escapeHtml(v)}</span>"`).join('');
  const textPart = node.textValue ? `<span class="xml-text">${escapeHtml(node.textValue)}</span>` : '';
  const childrenHtml = (node.children || []).map(child => renderXmlNode(child)).join('');
  if(node.children && node.children.length){
    return `<div class="xml-node${selectedClass}" data-xml-path="${escapeAttr(node.path)}"><div class="xml-line">&lt;<span class="xml-tag">${escapeHtml(node.name)}</span>${attrs}&gt;</div><div class="xml-children">${childrenHtml}</div><div class="xml-line">&lt;/<span class="xml-tag">${escapeHtml(node.name)}</span>&gt;</div></div>`;
  }
  return `<div class="xml-node${selectedClass}" data-xml-path="${escapeAttr(node.path)}"><div class="xml-line">&lt;<span class="xml-tag">${escapeHtml(node.name)}</span>${attrs}&gt;${textPart}&lt;/<span class="xml-tag">${escapeHtml(node.name)}</span>&gt;</div></div>`;
}


/**
 * Megjeleníti vagy újrarendereli a render xml document tree állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} doc a függvény doc bemeneti értéke
 */
function renderXmlDocumentTree(doc){
  if(!xmlContainer) return;
  if(!doc?.documentElement){
    xmlContainer.innerHTML = '<p>Nincs XML nézet.</p>';
    return;
  }
  const rootName = resolveNodeName(doc.documentElement);
  xmlContainer.innerHTML = renderXmlDomNode(doc.documentElement, `/${rootName}`);
  bindXmlClicks();
  highlightSelections();
}

/**
 * Megjeleníti vagy újrarendereli a render xml document subtree állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 */
function renderXmlDocumentSubtree(node, path){
  if(!xmlContainer) return;
  if(!node){
    xmlContainer.innerHTML = '<p>Nincs XML-részlet.</p>';
    return;
  }
  xmlContainer.innerHTML = renderXmlDomNode(node, path);
  bindXmlClicks();
  highlightSelections();
}

/**
 * Megjeleníti vagy újrarendereli a render xml dom node állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
function renderXmlDomNode(node, path){
  const selectedClass = selectedXmlPath === path ? ' selected' : '';
  const attrs = Array.from(node.attributes || [])
    .map(attr => ` <span class="xml-attr">${escapeHtml(attr.name)}</span>="<span class="xml-value">${escapeHtml(attr.value)}</span>"`)
    .join('');
  const children = Array.from(node.children || []);
  if(children.length){
    const counters = Object.create(null);
    const childrenHtml = children.map(child => {
      const name = resolveNodeName(child);
      counters[name] = (counters[name] || 0) + 1;
      return renderXmlDomNode(child, `${path}/${name}[${counters[name]}]`);
    }).join('');
    return `<div class="xml-node${selectedClass}" data-xml-path="${escapeAttr(path)}"><div class="xml-line">&lt;<span class="xml-tag">${escapeHtml(resolveNodeName(node))}</span>${attrs}&gt;</div><div class="xml-children">${childrenHtml}</div><div class="xml-line">&lt;/<span class="xml-tag">${escapeHtml(resolveNodeName(node))}</span>&gt;</div></div>`;
  }
  const textValue = String(node.textContent || '').trim();
  const textPart = textValue ? `<span class="xml-text">${escapeHtml(textValue)}</span>` : '';
  return `<div class="xml-node${selectedClass}" data-xml-path="${escapeAttr(path)}"><div class="xml-line">&lt;<span class="xml-tag">${escapeHtml(resolveNodeName(node))}</span>${attrs}&gt;${textPart}&lt;/<span class="xml-tag">${escapeHtml(resolveNodeName(node))}</span>&gt;</div></div>`;
}

/**
 * Kezeli vagy beköti a bind field clicks esemény- és inicializációs folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function bindFieldClicks(){
  if(formFieldDelegationInstalled || !formContainer) return;
  formFieldDelegationInstalled = true;
  formContainer.addEventListener('click', event => {
    const field = event.target.closest?.('.form-field[data-field-id]');
    if(!field || !formContainer.contains(field)) return;
    selectedFieldId = field.dataset.fieldId;
    selectedXmlPath = field.dataset.xmlPath || null;
    highlightSelections();
    scrollXmlToSelected();
  });
}

/**
 * Kezeli vagy beköti a bind form value sync esemény- és inicializációs folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function bindFormValueSync(){
  if(formValueDelegationInstalled || !formContainer) return;
  formValueDelegationInstalled = true;
    /**
   * A <code>delegate</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   */
const delegate = event => {
    const input = event.target.closest?.('.form-field input[data-field-id], .form-field select[data-field-id], .form-field textarea[data-field-id]');
    if(!input || !formContainer.contains(input)) return;
    handleFormValueChange(event);
  };
  formContainer.addEventListener('input', delegate);
  formContainer.addEventListener('change', delegate);
}

/**
 * Kezeli vagy beköti a handle form value change esemény- és inicializációs folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} event a feldolgozandó böngészőesemény
 */
function handleFormValueChange(event){
  const input = event.target;
  const fieldId = input.dataset.fieldId;
  if(!fieldId || !currentFormData) return;

  markFormDirty();
  const fieldWrapper = input.closest('.form-field');
  globalThis.clearEditedFieldXsdHighlight?.(input);
  const xmlPath = fieldWrapper?.dataset.xmlPath || currentFormData?.valuesByFieldId?.[fieldId]?.xmlPath;

  // Ne formázzunk minden billentyűleütésre, mert ez lassítja a gépelést és elmozdíthatja a kurzort.
  if(event.type !== 'input'){
    formatUiModelInputValue(input);
  }

  let value;
  if(input.type === 'checkbox') value = input.checked ? 'true' : 'false';
  else value = input.value;

  if(!currentFormData.valuesByFieldId){
    currentFormData.valuesByFieldId = {};
  }
  if(!currentFormData.valuesByFieldId[fieldId]){
    currentFormData.valuesByFieldId[fieldId] = { fieldId, xmlPath, value:'', present:false };
  }

  const valueIsEmpty = isEmptyXmlFieldValue(input, value);
  const canonicalPath = xmlPath ? canonicalizeXmlPath(xmlPath) : '';

  if(valueIsEmpty){
    if(canonicalPath && currentXmlDocument){
      removeXmlNodeByPath(currentXmlDocument, canonicalPath);
    }
    updateFormDataValueReferences(fieldId, canonicalPath || xmlPath, '', false);
    if(fieldWrapper){
      fieldWrapper.dataset.xmlPath = canonicalPath || xmlPath || '';
      fieldWrapper.classList.add('uimodel-missing-field');
    }
    if(selectedXmlPath && canonicalPath && pathMatches(selectedXmlPath, canonicalPath)){
      selectedXmlPath = null;
    }
    scheduleXmlFromCurrentState();
    if(event.type !== 'input') highlightSelections();
    return;
  }

  updateFormDataValueReferences(fieldId, canonicalPath || xmlPath, value, true);

  if(xmlPath && currentXmlDocument){
    const updatedNode = setXmlValueByPath(currentXmlDocument, xmlPath, value, { createMissing: true });
    if(updatedNode){
      const updatedPath = canonicalizeXmlPath(xmlPath);
      updateFormDataValueReferences(fieldId, updatedPath, value, true);
      if(fieldWrapper){
        fieldWrapper.dataset.xmlPath = updatedPath;
        fieldWrapper.classList.remove('uimodel-missing-field');
      }
      selectedXmlPath = updatedPath;
      scheduleXmlFromCurrentState();
      if(event.type !== 'input') highlightSelections();
    }
  }
}

/**
 * Ellenőrzi a is empty xml field value feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} input a függvény input bemeneti értéke
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function isEmptyXmlFieldValue(input, value){
  if(!input) return String(value ?? '') === '';
  if(input.type === 'checkbox') return false;
  return String(value ?? '').trim() === '';
}

/**
 * Előkészíti és elindítja a add form data value reference állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} index az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
 * @param {*} fieldId a célobjektum technikai azonosítója
 * @param {*} valueObj a feldolgozandó vagy beállítandó érték
 */
function addFormDataValueReference(index, fieldId, valueObj){
  if(!valueObj) return;
  const normalizedFieldId = String(valueObj.fieldId || valueObj.key || fieldId || '');
  if(normalizedFieldId){
    if(!index.byField.has(normalizedFieldId)) index.byField.set(normalizedFieldId, new Set());
    index.byField.get(normalizedFieldId).add(valueObj);
  }
  const path = canonicalizeXmlPath(valueObj.xmlPath || '');
  if(path){
    if(!index.byPath.has(path)) index.byPath.set(path, new Set());
    index.byPath.get(path).add(valueObj);
  }
}

/**
 * A <code>rebuildFormDataValueReferenceIndex</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} formData a függvény formData bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function rebuildFormDataValueReferenceIndex(formData = currentFormData){
  indexedFormDataRef = formData || null;
  formDataValueReferenceIndex = { byField:new Map(), byPath:new Map() };
  if(!formData) return formDataValueReferenceIndex;
  // Keep the permanent index compact: repeated row instances are indexed lazily,
  // only when the user edits a field from that occurrence.
  Object.entries(formData.valuesByFieldId || {}).forEach(([fieldId, valueObj]) => {
    addFormDataValueReference(formDataValueReferenceIndex, fieldId, valueObj);
  });
  return formDataValueReferenceIndex;
}

/**
 * A <code>ensureFormDataValueReferenceIndex</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function ensureFormDataValueReferenceIndex(){
  if(indexedFormDataRef !== currentFormData || !formDataValueReferenceIndex){
    rebuildFormDataValueReferenceIndex(currentFormData);
  }
  return formDataValueReferenceIndex;
}

/**
 * A <code>cacheMatchingRowValueReferences</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} index az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
 * @param {*} fieldId a célobjektum technikai azonosítója
 * @param {*} canonicalPath a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
function cacheMatchingRowValueReferences(index, fieldId, canonicalPath){
  const matches = new Set();
  Object.values(currentFormData?.rowInstancesByRowId || {}).forEach(instances => {
    (instances || []).forEach(instance => {
      Object.entries(instance?.valuesByFieldId || {}).forEach(([entryFieldId, valueObj]) => {
        if(!valueObj) return;
        const sameField = String(valueObj.fieldId || valueObj.key || entryFieldId) === String(fieldId);
        const samePath = canonicalPath && valueObj.xmlPath && pathMatches(valueObj.xmlPath, canonicalPath);
        if(canonicalPath ? samePath : sameField){
          matches.add(valueObj);
          addFormDataValueReference(index, entryFieldId, valueObj);
        }
      });
    });
  });
  return matches;
}

/**
 * Szinkronizálja vagy frissíti a update form data value references által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} fieldId a célobjektum technikai azonosítója
 * @param {*} xmlPath a feldolgozandó XML-tartalom vagy XML DOM-objektum
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @param {*} present a függvény present bemeneti értéke
 */
function updateFormDataValueReferences(fieldId, xmlPath, value, present){
  if(!currentFormData || !fieldId) return;
  const canonicalPath = xmlPath ? canonicalizeXmlPath(xmlPath) : '';
  if(!currentFormData.valuesByFieldId) currentFormData.valuesByFieldId = {};

  let direct = currentFormData.valuesByFieldId[fieldId];
  if(!direct){
    direct = { fieldId, key:fieldId, xmlPath:canonicalPath || xmlPath || '', value:'', present:false };
    currentFormData.valuesByFieldId[fieldId] = direct;
    addFormDataValueReference(ensureFormDataValueReferenceIndex(), fieldId, direct);
  }

  const index = ensureFormDataValueReferenceIndex();
  let candidates = canonicalPath ? index.byPath.get(canonicalPath) : index.byField.get(String(fieldId));
  if(!candidates || !candidates.size){
    const rowMatches = cacheMatchingRowValueReferences(index, fieldId, canonicalPath);
    if(rowMatches.size) candidates = rowMatches;
  }
  if((!candidates || !candidates.size) && direct){
    const directPath = canonicalizeXmlPath(direct.xmlPath || '');
    if(!canonicalPath || !directPath || pathMatches(directPath, canonicalPath)){
      candidates = new Set([direct]);
    }
  }

  (candidates || []).forEach(valueObj => {
    valueObj.fieldId = valueObj.fieldId || fieldId;
    valueObj.key = valueObj.key || fieldId;
    if(canonicalPath){
      valueObj.xmlPath = canonicalPath;
      if(!index.byPath.has(canonicalPath)) index.byPath.set(canonicalPath, new Set());
      index.byPath.get(canonicalPath).add(valueObj);
    }
    valueObj.value = value;
    valueObj.present = present === true;
  });
}

/**
 * Kezeli vagy beköti a bind xml clicks esemény- és inicializációs folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function bindXmlClicks(){
  if(xmlClickDelegationInstalled || !xmlContainer) return;
  xmlClickDelegationInstalled = true;
  xmlContainer.addEventListener('click', event => {
    const line = event.target.closest?.('.xml-node[data-xml-path] > .xml-line');
    if(!line || !xmlContainer.contains(line)) return;
    event.stopPropagation();
    const node = line.parentElement;
    selectedXmlPath = node.dataset.xmlPath || null;
    selectedFieldId = findFieldIdByXmlPath(selectedXmlPath);
    highlightSelections();
    openAncestorsForSelectedField();
    scrollFormToSelected();
  });
}

/**
 * A <code>replaceCurrentXmlFromText</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} xmlText a feldolgozandó XML-tartalom vagy XML DOM-objektum
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
function replaceCurrentXmlFromText(xmlText, options = {}){
  const source = String(xmlText || '');
  if(!source.trim()) throw new Error('A betöltendő XML tartalom üres.');
  currentXmlDocument = parseXmlString(source);
  xmlSourceDirtySinceLastApply = false;
  if(options.renderForm !== false && currentFormDefinition){
    currentFormData = buildFormDataFromDocument(currentFormDefinition, currentXmlDocument);
    renderForm(currentFormDefinition, currentFormData, currentSchemaBundle || null);
  }
  renderXmlFromCurrentState({ force:true });
  markFormDirty();
  persistUiState();
  if(options.showMessage !== false){
    showMessage(options.message || 'Az XML tartalom frissítve lett.', 'success');
  }
  return true;
}

/**
 * A <code>rebuildCurrentFormFromXmlText</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} xmlText a feldolgozandó XML-tartalom vagy XML DOM-objektum
 * @param {*} options a művelet opcionális beállításai
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function rebuildCurrentFormFromXmlText(xmlText, options = {}){
  const source = String(xmlText || '');
  if(!source.trim()) throw new Error('Az űrlap újraépítéséhez nincs XML tartalom.');
  parseXmlString(source);

  const requestData = new FormData();
  const schemaDir = schemaDirInput?.value?.trim();
  const generalXsdDir = generalXsdDirInput?.value?.trim();
  if(schemaDir) requestData.append('schemaDir', schemaDir);
  if(generalXsdDir) requestData.append('generalXsdDir', generalXsdDir);
  const fileName = options.fileName || getActiveXmlDisplayFileName() || suggestXmlFileName();
  requestData.append('xmlFile', new File([source], fileName || 'calculated.xml', { type:'application/xml' }));

  const formContainerScrollTop = formContainer?.scrollTop || 0;
  const formScrollTop = formScroll?.scrollTop || 0;
  const documentScrollTop = document.scrollingElement?.scrollTop || 0;
  const response = await fetch('/api/validate', { method:'POST', body:requestData, credentials:'same-origin' });
  const data = await response.json().catch(() => ({}));
  if(!response.ok){
    throw new Error(data.error || data.message || 'A kalkulált XML űrlapdefiníciójának újraépítése nem sikerült.');
  }
  if(!data.formDefinition){
    throw new Error('A kalkulált XML feldolgozása nem adott vissza űrlapdefiníciót.');
  }

  renderValidate(data, {
    preserveExistingFormOnInvalid:true,
    skipTabActivation:true,
    suppressStatusMessage:options.suppressStatusMessage !== false
  });
  if(!currentFormDefinition || !currentXmlDocument){
    throw new Error('A kalkulált XML űrlapállapota nem épült fel teljesen.');
  }

    /**
   * A <code>restoreViewport</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
const restoreViewport = () => {
    if(formContainer) formContainer.scrollTop = formContainerScrollTop;
    if(formScroll) formScroll.scrollTop = formScrollTop;
    if(document.scrollingElement) document.scrollingElement.scrollTop = documentScrollTop;
    if(selectedFieldId || selectedXmlPath) highlightSelections();
  };
  if(typeof window.requestAnimationFrame === 'function') window.requestAnimationFrame(restoreViewport);
  else window.setTimeout(restoreViewport, 0);
  return data;
}

/**
 * Szinkronizálja vagy frissíti a apply xml source changes által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function applyXmlSourceChanges(){
  try{
    if(!xmlSourceEditor) return;
    currentXmlDocument = parseXmlString(xmlSourceEditor.value);
    xmlSourceDirtySinceLastApply = false;
    if(currentFormDefinition){
      currentFormData = buildFormDataFromDocument(currentFormDefinition, currentXmlDocument);
      renderForm(currentFormDefinition, currentFormData, currentSchemaBundle || null);
    }
    renderXmlFromCurrentState();
    markFormDirty();
    persistUiState();
    showMessage('Az XML forrás alkalmazva lett.', 'success');
  }catch(error){
    console.error('XML forrás alkalmazási hiba', error);
    showMessage(error.message || 'Az XML forrás nem feldolgozható.', 'error');
  }
}

/**
 * Feldolgozza a build form data from document bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} formDefinition a függvény formDefinition bemeneti értéke
 * @param {*} xmlDocument a feldolgozandó XML-tartalom vagy XML DOM-objektum
 * @returns {*} a feldolgozás eredménye
 */
function buildFormDataFromDocument(formDefinition, xmlDocument){
  const valuesByFieldId = {};
  for(const tab of (formDefinition?.tabs || [])){
    for(const section of (tab.sections || [])){
      for(const row of (section.rows || [])){
        for(const field of (row.fields || [])){
          if(!field?.id) continue;
          const path = field.xmlPath || '';
          const existingNode = findNodeByPath(xmlDocument, path);
          const value = existingNode ? getXmlValueByPath(xmlDocument, path) || '' : '';
          valuesByFieldId[field.id] = {
            fieldId: field.id,
            xmlPath: path,
            value,
            present: !!existingNode
          };
        }
      }
    }
  }
  return { valuesByFieldId };
}

/**
 * Feldolgozza a parse xml string bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} xml a feldolgozandó XML-tartalom vagy XML DOM-objektum
 * @returns {*} a feldolgozás eredménye
 */
function parseXmlString(xml){
  const parser = new DOMParser();
  const doc = parser.parseFromString(xml, 'application/xml');
  const err = doc.querySelector('parsererror');
  if(err) throw new Error(err.textContent || 'Hibás XML');
  return doc;
}

/**
 * A <code>serializeXml</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} doc a függvény doc bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function serializeXml(doc){
  if(!doc) return '';
  return new XMLSerializer().serializeToString(doc);
}

/**
 * Feldolgozza a parse path segment bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} segment a függvény segment bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function parsePathSegment(segment){
  const match = String(segment || '').match(/^(.*?)(?:\[(\d+)\])?$/);
  return { name: match?.[1] || '', index: match?.[2] ? Number(match[2]) : null };
}

/**
 * Eltávolítja vagy alaphelyzetbe állítja a clear xml node path cache művelethez tartozó kliensoldali állapotot.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function clearXmlNodePathCache(){
  xmlNodePathCacheDocument = null;
  xmlNodePathCache.clear();
}

/**
 * A <code>cacheXmlNodePath</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} doc a függvény doc bemeneti értéke
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @returns {*} a feldolgozás eredménye
 */
function cacheXmlNodePath(doc, path, node){
  if(!doc || !path || !node) return node;
  if(xmlNodePathCacheDocument !== doc){
    xmlNodePathCacheDocument = doc;
    xmlNodePathCache.clear();
  }
  if(xmlNodePathCache.has(path)) xmlNodePathCache.delete(path);
  xmlNodePathCache.set(path, node);
  while(xmlNodePathCache.size > XML_NODE_PATH_CACHE_MAX){
    const oldestKey = xmlNodePathCache.keys().next().value;
    xmlNodePathCache.delete(oldestKey);
  }
  return node;
}

/**
 * Feloldja a find node by path eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} doc a függvény doc bemeneti értéke
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
function findNodeByPath(doc, path){
  if(!doc || !path) return null;
  const cacheKey = String(path);
  if(xmlNodePathCacheDocument !== doc){
    xmlNodePathCacheDocument = doc;
    xmlNodePathCache.clear();
  }else if(xmlNodePathCache.has(cacheKey)){
    const cached = xmlNodePathCache.get(cacheKey);
    xmlNodePathCache.delete(cacheKey);
    xmlNodePathCache.set(cacheKey, cached);
    return cached;
  }
  const parts = cacheKey.split('/').filter(Boolean);
  let current = doc.documentElement;
  let index = 0;
  const first = parsePathSegment(parts[0]);
  if(current && resolveNodeName(current) === first.name) index = 1;
  for(let i=index; i<parts.length; i++){
    const segment = parsePathSegment(parts[i]);
    const matches = [...current.children].filter(child => resolveNodeName(child) === segment.name);
    const next = segment.index ? matches[segment.index - 1] : matches[0];
    if(!next) return null;
    current = next;
  }
  return cacheXmlNodePath(doc, cacheKey, current);
}

/**
 * Betölti vagy lekéri a get xml value by path művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} doc a függvény doc bemeneti értéke
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
function getXmlValueByPath(doc, path){
  const node = findNodeByPath(doc, path);
  if(!node) return '';
  return node.children.length ? '' : (node.textContent || '').trim();
}

/**
 * Szinkronizálja vagy frissíti a set xml value by path által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} doc a függvény doc bemeneti értéke
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
function setXmlValueByPath(doc, path, value, options = {}){
  if(!doc || !path) return null;
  let node = findNodeByPath(doc, path);
  if(!node && options.createMissing !== false){
    node = createNodeByPath(doc, path);
  }
  if(node){
    node.textContent = value;
  }
  return node;
}

/**
 * Eltávolítja vagy alaphelyzetbe állítja a remove xml node by path művelethez tartozó kliensoldali állapotot.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} doc a függvény doc bemeneti értéke
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
function removeXmlNodeByPath(doc, path){
  if(!doc || !path) return false;
  const node = findNodeByPath(doc, path);
  if(!node || !node.parentNode || node === doc.documentElement) return false;
  node.parentNode.removeChild(node);
  clearXmlNodePathCache();
  return true;
}

/**
 * Előkészíti és elindítja a create node by path állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} doc a függvény doc bemeneti értéke
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
function createNodeByPath(doc, path){
  if(!doc || !doc.documentElement || !path) return null;
  clearXmlNodePathCache();
  const parts = String(path).split('/').filter(Boolean);
  if(!parts.length) return null;

  let current = doc.documentElement;
  let startIndex = 0;
  const first = parsePathSegment(parts[0]);
  if(resolveNodeName(current) === first.name){
    startIndex = 1;
  }

  const walkedParts = [];
  if(startIndex === 1){
    walkedParts.push(withDefaultIndex(first.name, first.index));
  }

  for(let i = startIndex; i < parts.length; i++){
    const segment = parsePathSegment(parts[i]);
    if(!segment.name) return null;

    const desiredIndex = segment.index || 1;
    let matches = [...current.children].filter(child => resolveNodeName(child) === segment.name);

    while(matches.length < desiredIndex){
      const created = createXmlElementForPathSegment(doc, segment.name);
      const parentPath = '/' + walkedParts.join('/');
      insertXmlElementInSchemaOrder(current, created, parentPath);
      matches = [...current.children].filter(child => resolveNodeName(child) === segment.name);
    }

    current = matches[desiredIndex - 1];
    walkedParts.push(withDefaultIndex(segment.name, desiredIndex));
  }

  return current;
}

/**
 * A <code>withDefaultIndex</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} name a feloldáshoz vagy megjelenítéshez használt név
 * @param {*} index az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
 * @returns {*} a feldolgozás eredménye
 */
function withDefaultIndex(name, index){
  return `${name}[${index || 1}]`;
}

/**
 * Előkészíti és elindítja a create xml element for path segment állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} doc a függvény doc bemeneti értéke
 * @param {*} name a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
function createXmlElementForPathSegment(doc, name){
  // The form paths stored by the backend are local-name based. Most generated form
  // child nodes are namespace-less even when the document root has a namespace, so
  // createElement is the safest default here. If a parent already has a prefix and
  // the path segment includes that prefix, the browser will preserve it.
  return doc.createElement(name);
}

/**
 * A <code>insertXmlElementInSchemaOrder</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} parent a függvény parent bemeneti értéke
 * @param {*} created a függvény created bemeneti értéke
 * @param {*} parentPath a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 */
function insertXmlElementInSchemaOrder(parent, created, parentPath){
  if(!parent || !created){
    return;
  }

  const order = getXmlChildOrderForParentPath(parentPath);
  const createdName = resolveNodeName(created);
  const createdGeneratedKey = getGeneratedXmlNodeSortKey(createdName);

  const before = [...parent.children].find(child => {
    if(child === created) return false;
    const childName = resolveNodeName(child);

    // NAV generated XSD element names contain their stable numeric identifier
    // (for example Block_120651, Chain_120687, FieldGroup_120688).
    // When a missing parent block has to be created from a field edit, the UI
    // model order can be incomplete or can reflect rendering order instead of
    // the real XSD sequence. In that case the numeric generated-name order is
    // the safest local fallback and prevents invalid insertions such as placing
    // Block_120651 after Block_120812 under Form_NAV_PMT25.
    const childGeneratedKey = getGeneratedXmlNodeSortKey(childName);
    if(createdGeneratedKey && childGeneratedKey && createdGeneratedKey.prefix === childGeneratedKey.prefix){
      return childGeneratedKey.number > createdGeneratedKey.number;
    }

    if(order && order.size){
      const createdOrder = order.has(createdName) ? order.get(createdName) : Number.MAX_SAFE_INTEGER;
      const childOrder = order.has(childName) ? order.get(childName) : Number.MAX_SAFE_INTEGER;
      return childOrder > createdOrder;
    }

    return false;
  });

  if(before){
    parent.insertBefore(created, before);
  }else{
    parent.appendChild(created);
  }
}

/**
 * Betölti vagy lekéri a get generated xml node sort key művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} name a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
function getGeneratedXmlNodeSortKey(name){
  const match = String(name || '').match(/^(Block|Chain|FieldGroup)_(\d+)$/);
  if(!match) return null;
  return { prefix: match[1], number: Number(match[2]) };
}

/**
 * Betölti vagy lekéri a get xml child order for parent path művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} parentPath a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
function getXmlChildOrderForParentPath(parentPath){
  const normalizedParentPath = canonicalizeXmlPath(parentPath || '');
  if(!normalizedParentPath || !currentFormDefinition){
    return null;
  }

  const order = new Map();
  collectXmlPathChildOrder(currentFormDefinition, normalizedParentPath, order);
  return order;
}

/**
 * Feldolgozza a collect xml path child order bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} source a függvény source bemeneti értéke
 * @param {*} normalizedParentPath a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @param {*} order a függvény order bemeneti értéke
 */
function collectXmlPathChildOrder(source, normalizedParentPath, order){
  if(!source) return;

  if(Array.isArray(source)){
    source.forEach(item => collectXmlPathChildOrder(item, normalizedParentPath, order));
    return;
  }

  if(typeof source !== 'object') return;

  const xmlPath = source.xmlPath;
  if(xmlPath){
    addImmediateChildFromKnownPath(xmlPath, normalizedParentPath, order);
  }

  for(const value of Object.values(source)){
    if(value && typeof value === 'object'){
      collectXmlPathChildOrder(value, normalizedParentPath, order);
    }
  }
}

/**
 * Előkészíti és elindítja a add immediate child from known path állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} xmlPath a feldolgozandó XML-tartalom vagy XML DOM-objektum
 * @param {*} normalizedParentPath a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @param {*} order a függvény order bemeneti értéke
 */
function addImmediateChildFromKnownPath(xmlPath, normalizedParentPath, order){
  const parts = canonicalPathParts(xmlPath);
  if(parts.length < 2) return;

  for(let i = 0; i < parts.length - 1; i++){
    const candidateParent = '/' + parts.slice(0, i + 1).join('/');
    if(candidateParent !== normalizedParentPath) continue;

    const childName = parsePathSegment(parts[i + 1]).name;
    if(childName && !order.has(childName)){
      order.set(childName, order.size);
    }
    break;
  }
}

/**
 * Ellenőrzi a canonical path parts feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
function canonicalPathParts(path){
  if(!path) return [];
  return canonicalizeXmlPath(path).split('/').filter(Boolean);
}

/**
 * Feldolgozza a build xml view model from document bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} doc a függvény doc bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function buildXmlViewModelFromDocument(doc){
  if(!doc || !doc.documentElement) return null;
  return {
    root: buildXmlNodeView(doc.documentElement, `/${resolveNodeName(doc.documentElement)}`)
  };
}

/**
 * Feldolgozza a build xml node view bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
function buildXmlNodeView(node, path){
  const attrs = {};
  for(const attr of [...node.attributes || []]) attrs[attr.name] = attr.value;
  const children = [...node.children];
  const counters = {};
  return {
    name: resolveNodeName(node),
    path,
    textValue: children.length ? '' : (node.textContent || '').trim(),
    element: true,
    attributes: attrs,
    children: children.map(child => {
      const name = resolveNodeName(child);
      counters[name] = (counters[name] || 0) + 1;
      return buildXmlNodeView(child, `${path}/${name}[${counters[name]}]`);
    })
  };
}

/**
 * Feloldja a resolve node name eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @returns {*} a feldolgozás eredménye
 */
function resolveNodeName(node){ return node.localName || node.nodeName; }

/**
 * Feloldja a find field id by xml path eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
function findFieldIdByXmlPath(path){ return formFieldNavigationUi.findFieldIdByXmlPath(path); }

/**
 * Szinkronizálja vagy frissíti a update toggle all form collapse button által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function updateToggleAllFormCollapseButton(){ return formFieldNavigationUi.updateToggleAllFormCollapseButton(); }

/**
 * A <code>toggleAllFormCollapsibles</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function toggleAllFormCollapsibles(){ return formFieldNavigationUi.toggleAllFormCollapsibles(); }

/**
 * Kezeli vagy beköti a bind collapse toggles esemény- és inicializációs folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} scope a függvény scope bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function bindCollapseToggles(scope = document){ return formFieldNavigationUi.bindCollapseToggles(scope); }

/**
 * A <code>openAncestorsForSelectedField</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} targetOverride a célobjektum technikai azonosítója
 * @returns {*} a feldolgozás eredménye
 */
function openAncestorsForSelectedField(targetOverride = null){ return formFieldNavigationUi.openAncestorsForSelectedField(targetOverride); }

/**
 * A <code>highlightSelections</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function highlightSelections(){
  const result = formFieldNavigationUi.highlightSelections();
  document.dispatchEvent(new CustomEvent('nav:xml-selection-changed', {
    detail: { selectedFieldId, selectedXmlPath }
  }));
  return result;
}

/**
 * A <code>scrollElementWithinContainer</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} container a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} target a függvény target bemeneti értéke
 * @param {*} margin a függvény margin bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function scrollElementWithinContainer(container, target, margin = 16){ return formFieldNavigationUi.scrollElementWithinContainer(container, target, margin); }

/**
 * A <code>scrollXmlToSelected</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function scrollXmlToSelected(){ return formFieldNavigationUi.scrollXmlToSelected(); }

/**
 * A <code>scrollFormToSelected</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function scrollFormToSelected(){ return formFieldNavigationUi.scrollFormToSelected(); }

/**
 * Szinkronizálja vagy frissíti a refresh field search által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
function refreshFieldSearch(options = {}){ return formFieldNavigationUi.refreshFieldSearch(options); }

/**
 * Megjeleníti vagy újrarendereli a show xpath copy success állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function showXPathCopySuccess(){ return formFieldNavigationUi.showXPathCopySuccess(); }

/**
 * A <code>escapeHtml</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function escapeHtml(value){
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

/**
 * A <code>escapeAttr</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function escapeAttr(value){ return escapeHtml(value); }

/**
 * A <code>cssEscape</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function cssEscape(value){ return String(value).replaceAll('\\', '\\\\').replaceAll('"', '\\"'); }

Object.assign(globalThis, {
  normalizeLayoutWidth,
  markXmlViewsDirty,
  resetXmlRenderState,
  ensureActiveXmlViewRendered,
  renderXmlFromCurrentState,
  syncXmlSourceHighlight,
  syncXmlSourceScroll,
  highlightXml,
  prettyPrintCurrentXml,
  formatXmlDocument,
  appendPrettyXmlNode,
  escapeXmlText,
  escapeXmlAttribute,
  positionXmlSaveMenu,
  toggleXmlSaveMenu,
  closeXmlSaveMenu,
  updateXmlSaveMenuState,
  positionM2mSubmitMenu,
  toggleM2mSubmitMenu,
  closeM2mSubmitMenu,
  updateM2mSubmitMenuState,
  handleM2mFormAction,
  handleM2mAttachmentInputChange,
  handleXmlSaveAction,
  currentSerializedXmlForSave,
  collectCurrentFormControlSnapshot,
  readControlValueForXmlSnapshot,
  hasUnsyncedFormControlChanges,
  syncCurrentFormControlsToXmlDocument,
  buildCurrentXmlSnapshotForSave,
  createCurrentXmlSnapshotInfo,
  lightweightStringHash,
  serverSaveOptions,
  requireActiveEditableXmlForServerSave,
  saveXmlToServer,
  quickSaveCurrentXmlFile,
  updateQuickSaveXmlButtonState,
  previewServerXmlDiff,
  showXmlDiffPreview,
  saveXmlFile,
  suggestXmlFileName,
  validateCurrentXml,
  validateCurrentXmlInBrowserContext,
  setXsdValidationButtonsDisabled,
  startActiveFileXsdValidation,
  waitForXsdValidationJobAndLoadLatest,
  loadLatestActiveXsdValidationResult,
  autoStartXsdValidationAfterActiveXmlOpen,
  renderXmlView,
  renderXmlNode,
  bindFieldClicks,
  bindFormValueSync,
  handleFormValueChange,
  isEmptyXmlFieldValue,
  rebuildFormDataValueReferenceIndex,
  updateFormDataValueReferences,
  bindXmlClicks,
  replaceCurrentXmlFromText,
  rebuildCurrentFormFromXmlText,
  applyXmlSourceChanges,
  buildFormDataFromDocument,
  parseXmlString,
  serializeXml,
  parsePathSegment,
  findNodeByPath,
  getXmlValueByPath,
  setXmlValueByPath,
  removeXmlNodeByPath,
  createNodeByPath,
  withDefaultIndex,
  createXmlElementForPathSegment,
  insertXmlElementInSchemaOrder,
  getGeneratedXmlNodeSortKey,
  getXmlChildOrderForParentPath,
  collectXmlPathChildOrder,
  addImmediateChildFromKnownPath,
  canonicalPathParts,
  buildXmlViewModelFromDocument,
  buildXmlNodeView,
  resolveNodeName,
  findFieldIdByXmlPath,
  updateToggleAllFormCollapseButton,
  toggleAllFormCollapsibles,
  bindCollapseToggles,
  openAncestorsForSelectedField,
  highlightSelections,
  scrollElementWithinContainer,
  scrollXmlToSelected,
  scrollFormToSelected,
  refreshFieldSearch,
  showXPathCopySuccess,
  escapeHtml,
  escapeAttr,
  cssEscape
});
