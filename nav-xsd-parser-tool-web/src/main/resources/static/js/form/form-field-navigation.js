/**
 * @module form/form-field-navigation
 *
 * A űrlap-megjelenítési és mezőkötési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

/**
 * Űrlapmező-keresés, XML/űrlap kijelölési navigáció,
 * összecsukható űrlapkártyák és XPath-vágólap kezelés.
 */

function normalizeXmlPathSegmentName(name){
  return String(name || '')
    .replace(/^Q\{[^}]*\}/, '')
    .replace(/^.*:/, '');
}

/**
 * Ellenőrzi a canonicalize xml path feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
export function canonicalizeXmlPath(path){
  if(!path) return '';
  const parts = String(path).split('/').filter(Boolean).map(segment => {
    const match = segment.match(/^(.+?)(?:\[(\d+)\])?$/);
    if(!match) return normalizeXmlPathSegmentName(segment);
    const name = normalizeXmlPathSegmentName(match[1]);
    const index = match[2] || '1';
    return `${name}[${index}]`;
  });
  return `/${parts.join('/')}`;
}

/**
 * A <code>pathMatches</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} left a függvény left bemeneti értéke
 * @param {*} right a függvény right bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function pathMatches(left, right){
  if(!left || !right) return false;
  return canonicalizeXmlPath(left) === canonicalizeXmlPath(right);
}

/**
 * A <code>cssEscape</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function cssEscape(value){
  return String(value).replaceAll('\\', '\\\\').replaceAll('"', '\\"');
}

/**
 * Előkészíti és elindítja a create form field navigation állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} arg1 a függvény arg1 bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function createFormFieldNavigation({ elements, getSelection, setSelection, callbacks }){
  const {
    formContainer,
    formScroll,
    xmlTreePanel,
    fieldSearchInput,
    fieldSearchResults,
    fieldSearchPrevButton,
    fieldSearchNextButton,
    fieldSearchCounter,
    selectedXPathLabel,
    xpathCopySuccess,
    toggleAllFormCollapseButton
  } = elements;

  let fieldSearchMatches = [];
  let fieldSearchIndex = -1;
  let fieldSearchDebounce = null;
  let xpathCopySuccessTimeout = null;
  const collapseDelegationScopes = new WeakSet();

    /**
   * Betölti vagy lekéri a get active form search scope művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @returns {*} a feldolgozás eredménye
   */
function getActiveFormSearchScope(){
    const activeMultiformPanel = document.querySelector('#formContainer .multiform-runtime-panel.active');
    return activeMultiformPanel || formContainer || document;
  }

    /**
   * Feloldja a find field id by xml path eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
   * @returns {*} a feldolgozás eredménye
   */
function findFieldIdByXmlPath(path){
    if(!path) return null;
    const canonicalTarget = canonicalizeXmlPath(path);
        /**
     * Feloldja a find eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @returns {*} a feldolgozás eredménye
     */
const find = () => {
      const fields = [...document.querySelectorAll('.form-field[data-xml-path]')];
      const direct = fields.find(element => pathMatches(element.dataset.xmlPath || '', canonicalTarget));
      if(direct) return direct.dataset.fieldId;
      const targetLastPart = canonicalTarget.split('/').pop();
      const fallback = fields.find(element => {
        const ownPath = canonicalizeXmlPath(element.dataset.xmlPath || '');
        return ownPath.split('/').pop() === targetLastPart;
      });
      return fallback ? fallback.dataset.fieldId : null;
    };
    let fieldId = find();
    if(!fieldId){
      callbacks.ensureFormSectionsForSearch?.(canonicalTarget);
      fieldId = find();
    }
    if(!fieldId){
      callbacks.ensureAllFormSectionsRendered?.();
      fieldId = find();
    }
    return fieldId;
  }

    /**
   * Betölti vagy lekéri a get form collapsible cards művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @returns {*} a feldolgozás eredménye
   */
function getFormCollapsibleCards(){
    return [...getActiveFormSearchScope().querySelectorAll('.collapsible-card')];
  }

    /**
   * Szinkronizálja vagy frissíti a update toggle all form collapse button által kezelt állapotot a megadott adatok alapján.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   */
function updateToggleAllFormCollapseButton(){
    if(!toggleAllFormCollapseButton) return;
    const cards = getFormCollapsibleCards();
    if(!cards.length){
      toggleAllFormCollapseButton.disabled = true;
      toggleAllFormCollapseButton.title = 'Nincs nyitható űrlapelem';
      toggleAllFormCollapseButton.setAttribute('aria-label', 'Nincs nyitható űrlapelem');
      toggleAllFormCollapseButton.classList.remove('is-collapse-mode');
      return;
    }
    toggleAllFormCollapseButton.disabled = false;
    const hasExpanded = cards.some(card => !card.classList.contains('collapsed'));
    const label = hasExpanded ? 'Összes elem becsukása' : 'Összes elem kinyitása';
    toggleAllFormCollapseButton.title = label;
    toggleAllFormCollapseButton.setAttribute('aria-label', label);
    toggleAllFormCollapseButton.classList.toggle('is-collapse-mode', hasExpanded);
  }

    /**
   * Szinkronizálja vagy frissíti a update collapse toggle által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} card a függvény card bemeneti értéke
   */
function updateCollapseToggle(card){
    const toggle = card?.querySelector(':scope > .collapse-toggle');
    if(!toggle) return;
    toggle.setAttribute('aria-expanded', card.classList.contains('collapsed') ? 'false' : 'true');
  }

    /**
   * A <code>toggleAllFormCollapsibles</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   */
function toggleAllFormCollapsibles(){
    let cards = getFormCollapsibleCards();
    if(!cards.length) return;
    const shouldCollapse = cards.some(card => !card.classList.contains('collapsed'));
    if(!shouldCollapse){
      callbacks.ensureAllFormSectionsRendered?.();
      cards = getFormCollapsibleCards();
    }
    cards.forEach(card => {
      card.classList.toggle('collapsed', shouldCollapse);
      updateCollapseToggle(card);
    });
    updateToggleAllFormCollapseButton();
  }

    /**
   * Kezeli vagy beköti a bind collapse toggles esemény- és inicializációs folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} scope a függvény scope bemeneti értéke
   */
function bindCollapseToggles(scope = document){
    if(!scope?.querySelectorAll) return;
    const coveredByFormContainer = scope !== formContainer
      && !!formContainer?.contains?.(scope)
      && collapseDelegationScopes.has(formContainer);
    if(!coveredByFormContainer && !collapseDelegationScopes.has(scope)){
      collapseDelegationScopes.add(scope);
      scope.addEventListener('click', event => {
        const toggle = event.target.closest?.('.collapse-toggle');
        if(!toggle || (scope !== document && !scope.contains(toggle))) return;
        event.preventDefault();
        const card = toggle.closest('.collapsible-card');
        if(!card) return;
        if(card.classList.contains('collapsed')) callbacks.ensureSectionRendered?.(card);
        card.classList.toggle('collapsed');
        updateCollapseToggle(card);
        updateToggleAllFormCollapseButton();
      });
    }
    scope.querySelectorAll('.collapse-toggle').forEach(toggle => {
      const card = toggle.closest('.collapsible-card');
      if(card) updateCollapseToggle(card);
    });
  }

    /**
   * A <code>openAncestorsForSelectedField</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} targetOverride a célobjektum technikai azonosítója
   */
function openAncestorsForSelectedField(targetOverride = null){
    const selection = getSelection();
    let target = targetOverride;
    if(!target && selection.selectedXmlPath){
      target = callbacks.findFormFieldByXmlPath(selection.selectedXmlPath);
    }
    if(!target && selection.selectedFieldId){
      target = document.querySelector(`.form-field[data-field-id="${cssEscape(selection.selectedFieldId)}"]`);
    }
    if(!target && (selection.selectedXmlPath || selection.selectedFieldId)){
      callbacks.ensureFormSectionsForSearch?.(selection.selectedXmlPath || selection.selectedFieldId);
      if(selection.selectedXmlPath) target = callbacks.findFormFieldByXmlPath(selection.selectedXmlPath);
      if(!target && selection.selectedFieldId){
        target = document.querySelector(`.form-field[data-field-id="${cssEscape(selection.selectedFieldId)}"]`);
      }
    }
    if(!target && (selection.selectedXmlPath || selection.selectedFieldId)){
      callbacks.ensureAllFormSectionsRendered?.();
      if(selection.selectedXmlPath) target = callbacks.findFormFieldByXmlPath(selection.selectedXmlPath);
      if(!target && selection.selectedFieldId){
        target = document.querySelector(`.form-field[data-field-id="${cssEscape(selection.selectedFieldId)}"]`);
      }
    }
    if(!target) return;
    target.closest('.form-pane')?.querySelectorAll('.collapsible-card').forEach(card => {
      if(card.contains(target)){
        card.classList.remove('collapsed');
        updateCollapseToggle(card);
      }
    });
    updateToggleAllFormCollapseButton();
  }

    /**
   * Szinkronizálja vagy frissíti a update selected xpath ui által kezelt állapotot a megadott adatok alapján.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   */
function updateSelectedXPathUi(){
    const { selectedXmlPath } = getSelection();
    if(selectedXPathLabel){
      selectedXPathLabel.textContent = selectedXmlPath || 'Nincs kijelölt XML elem';
      selectedXPathLabel.title = selectedXmlPath || '';
    }
    if(xpathCopySuccess && !selectedXmlPath) xpathCopySuccess.classList.remove('visible');
  }

    /**
   * A <code>highlightSelections</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function highlightSelections(){
    const { selectedFieldId, selectedXmlPath } = getSelection();
    const canonicalSelectedPath = selectedXmlPath ? canonicalizeXmlPath(selectedXmlPath) : '';
    document.querySelectorAll('.form-field').forEach(element => {
      const ownPath = element.dataset.xmlPath || '';
      const pathSelected = canonicalSelectedPath && ownPath && pathMatches(ownPath, canonicalSelectedPath);
      const fieldSelected = !canonicalSelectedPath && selectedFieldId && element.dataset.fieldId === selectedFieldId;
      element.classList.toggle('field-selected', Boolean(pathSelected || fieldSelected));
    });
    document.querySelectorAll('.xml-node').forEach(element => {
      element.classList.toggle('selected', pathMatches(element.dataset.xmlPath || '', selectedXmlPath || ''));
    });
    updateSelectedXPathUi();
  }

    /**
   * A <code>scrollElementWithinContainer</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} container a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
   * @param {*} target a függvény target bemeneti értéke
   * @param {*} margin a függvény margin bemeneti értéke
   */
function scrollElementWithinContainer(container, target, margin = 16){
    if(!container || !target) return;
    const containerRect = container.getBoundingClientRect();
    const targetRect = target.getBoundingClientRect();
    const currentTop = container.scrollTop;
    const targetTop = targetRect.top - containerRect.top + currentTop;
    const targetCenter = targetTop + (targetRect.height / 2);
    const desiredTop = Math.max(targetCenter - (container.clientHeight / 2), 0);
    const maxTop = Math.max(container.scrollHeight - container.clientHeight, 0);
    const nextTop = Math.min(Math.max(desiredTop - margin, 0), maxTop);
    container.scrollTo({ top:nextTop, behavior:'smooth' });
  }

    /**
   * A <code>scrollXmlToSelected</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function scrollXmlToSelected(){
    const { selectedXmlPath } = getSelection();
    if(!selectedXmlPath) return;
    callbacks.ensureXmlTreeRendered?.();
    const targetNode = [...document.querySelectorAll('.xml-node[data-xml-path]')]
      .find(element => pathMatches(element.dataset.xmlPath || '', selectedXmlPath));
    const target = targetNode?.querySelector(':scope > .xml-line');
    if(target) scrollElementWithinContainer(xmlTreePanel, target, 18);
  }

    /**
   * A <code>scrollFormToSelected</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   */
function scrollFormToSelected(){
    const selection = getSelection();
    if(!selection.selectedFieldId && !selection.selectedXmlPath) return;
    const scope = getActiveFormSearchScope();
    let target = null;
    if(selection.selectedFieldId){
      const selector = `.form-field[data-field-id="${cssEscape(selection.selectedFieldId)}"]`;
      target = scope.querySelector(selector) || document.querySelector(selector);
    }
    if(!target && selection.selectedXmlPath){
      target = callbacks.findFormFieldByXmlPath(selection.selectedXmlPath, scope) || callbacks.findFormFieldByXmlPath(selection.selectedXmlPath);
    }
    if(!target){
      callbacks.ensureFormSectionsForSearch?.(selection.selectedXmlPath || selection.selectedFieldId);
      if(selection.selectedFieldId){
        const selector = `.form-field[data-field-id="${cssEscape(selection.selectedFieldId)}"]`;
        target = scope.querySelector(selector) || document.querySelector(selector);
      }
      if(!target && selection.selectedXmlPath){
        target = callbacks.findFormFieldByXmlPath(selection.selectedXmlPath, scope) || callbacks.findFormFieldByXmlPath(selection.selectedXmlPath);
      }
    }
    if(!target){
      callbacks.ensureAllFormSectionsRendered?.();
      if(selection.selectedFieldId){
        const selector = `.form-field[data-field-id="${cssEscape(selection.selectedFieldId)}"]`;
        target = scope.querySelector(selector) || document.querySelector(selector);
      }
      if(!target && selection.selectedXmlPath){
        target = callbacks.findFormFieldByXmlPath(selection.selectedXmlPath, scope) || callbacks.findFormFieldByXmlPath(selection.selectedXmlPath);
      }
    }
    if(target) scrollElementWithinContainer(formScroll, target, 18);
  }

    /**
   * Feldolgozza a collect field search items bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @returns {*} a feldolgozás eredménye
   */
function collectFieldSearchItems(){
    const fields = [...getActiveFormSearchScope().querySelectorAll('.form-field[data-field-id]')];
    return fields.map((element, index) => {
      const fieldId = String(element.dataset.fieldId || '').trim();
      const eazon = fieldId.startsWith('Field_') ? fieldId.substring('Field_'.length) : fieldId;
      const labelNode = element.querySelector('.uimodel-label-text, .field-label-text, .uimodel-table-label');
      const label = String(labelNode?.textContent || '').replace(/\s+/g, ' ').trim();
      const valueNode = element.querySelector('input, select, textarea');
      const value = valueNode ? String(valueNode.value || '').trim() : '';
      const xmlPath = String(element.dataset.xmlPath || '').trim();
      const text = [fieldId, eazon, label, value, xmlPath].join(' ').toLowerCase();
      return { el:element, index, fieldId, eazon, label, value, xmlPath, text };
    }).filter(item => item.fieldId || item.label);
  }

    /**
   * Feldolgozza a normalize field search query bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function normalizeFieldSearchQuery(value){
    return String(value || '').trim().toLowerCase();
  }

    /**
   * Szinkronizálja vagy frissíti a update field search controls által kezelt állapotot a megadott adatok alapján.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   */
function updateFieldSearchControls(){
    const total = fieldSearchMatches.length;
    const current = fieldSearchIndex >= 0 ? fieldSearchIndex + 1 : 0;
    if(fieldSearchCounter) fieldSearchCounter.textContent = `${current}/${total}`;
    const navEnabled = total > 1;
    if(fieldSearchPrevButton) fieldSearchPrevButton.disabled = !navEnabled;
    if(fieldSearchNextButton) fieldSearchNextButton.disabled = !navEnabled;
  }

    /**
   * Megjeleníti vagy újrarendereli a render field search results állapotát a felhasználói felületen.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} show a függvény show bemeneti értéke
   */
function renderFieldSearchResults(show){
    if(!fieldSearchResults) return;
    const query = normalizeFieldSearchQuery(fieldSearchInput?.value || '');
    if(!show || query.length < 3){
      fieldSearchResults.hidden = true;
      fieldSearchResults.innerHTML = '';
      return;
    }
    if(!fieldSearchMatches.length){
      fieldSearchResults.hidden = false;
      fieldSearchResults.innerHTML = '<div class="field-search-empty">Nincs találat.</div>';
      return;
    }
    const visible = fieldSearchMatches.slice(0, 40);
    fieldSearchResults.hidden = false;
    fieldSearchResults.innerHTML = visible.map((item, index) => {
      const active = index === fieldSearchIndex ? ' active' : '';
      const title = item.label || item.fieldId || 'Mező';
      const metaParts = [
        item.eazon && item.eazon !== item.fieldId ? item.eazon : item.fieldId,
        item.value ? `Érték: ${item.value}` : ''
      ].filter(Boolean);
      return `<button type="button" class="field-search-result${active}" data-field-search-index="${index}">
        <span class="field-search-result-label">${callbacks.escapeHtml(title)}</span>
        <span class="field-search-result-meta">${callbacks.escapeHtml(metaParts.join(' · '))}</span>
      </button>`;
    }).join('') + (fieldSearchMatches.length > visible.length
      ? `<div class="field-search-more">+${fieldSearchMatches.length - visible.length} további találat. Szűkítsd a keresést.</div>`
      : '');
  }

    /**
   * Szinkronizálja vagy frissíti a refresh field search által kezelt állapotot a megadott adatok alapján.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} options a művelet opcionális beállításai
   */
function refreshFieldSearch(options = {}){
    if(fieldSearchInput?.dataset.multiformIndexSearchActive === 'true'){
      if(fieldSearchResults) fieldSearchResults.hidden = true;
      return;
    }
    const query = normalizeFieldSearchQuery(fieldSearchInput?.value || '');
    if(!fieldSearchInput || query.length < 3){
      fieldSearchMatches = [];
      fieldSearchIndex = -1;
      renderFieldSearchResults(false);
      updateFieldSearchControls();
      return;
    }
    const matchedSections = callbacks.ensureFormSectionsForSearch?.(query) || 0;
    if(!matchedSections) callbacks.ensureAllFormSectionsRendered?.();
    const terms = query.split(/\s+/).filter(Boolean);
    fieldSearchMatches = collectFieldSearchItems().filter(item => terms.every(term => item.text.includes(term)));
    if(fieldSearchMatches.length){
      const currentFieldId = getSelection().selectedFieldId || '';
      const selectedIndex = fieldSearchMatches.findIndex(item => item.fieldId === currentFieldId || `Field_${item.fieldId}` === currentFieldId);
      if(selectedIndex >= 0) fieldSearchIndex = selectedIndex;
      else if(fieldSearchIndex < 0 || fieldSearchIndex >= fieldSearchMatches.length) fieldSearchIndex = 0;
    }else{
      fieldSearchIndex = -1;
    }
    renderFieldSearchResults(options.showList !== false);
    updateFieldSearchControls();
  }

    /**
   * A <code>goToFieldSearchMatch</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} index az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
   */
function goToFieldSearchMatch(index){
    const item = fieldSearchMatches[index];
    if(!item?.el) return;
    const fieldId = item.el.dataset.fieldId || item.fieldId || '';
    setSelection({ selectedFieldId:fieldId || null, selectedXmlPath:item.el.dataset.xmlPath || null });
    openAncestorsForSelectedField();
    highlightSelections();
    scrollElementWithinContainer(formScroll, item.el, 22);
    const focusable = item.el.querySelector('input:not([type="hidden"]), select, textarea, button');
    if(focusable && typeof focusable.focus === 'function') focusable.focus({ preventScroll:true });
    item.el.classList.add('field-search-jump-highlight');
    setTimeout(() => item.el.classList.remove('field-search-jump-highlight'), 1800);
    renderFieldSearchResults(!fieldSearchResults?.hidden);
    updateFieldSearchControls();
  }

    /**
   * A <code>navigateFieldSearch</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} delta a függvény delta bemeneti értéke
   */
function navigateFieldSearch(delta){
    if(!fieldSearchMatches.length) return;
    fieldSearchIndex = (fieldSearchIndex + delta + fieldSearchMatches.length) % fieldSearchMatches.length;
    renderFieldSearchResults(!fieldSearchResults?.hidden);
    updateFieldSearchControls();
    goToFieldSearchMatch(fieldSearchIndex);
  }

    /**
   * Kezeli vagy beköti a bind field search results esemény- és inicializációs folyamatát.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   */
function bindFieldSearchResults(){
    fieldSearchResults?.addEventListener('mousedown', event => {
      const button = event.target.closest?.('[data-field-search-index]');
      if(button) event.preventDefault();
    });
    fieldSearchResults?.addEventListener('click', event => {
      const button = event.target.closest?.('[data-field-search-index]');
      if(!button) return;
      const index = Number(button.dataset.fieldSearchIndex);
      if(Number.isFinite(index)){
        fieldSearchIndex = index;
        goToFieldSearchMatch(index);
        fieldSearchResults.hidden = true;
      }
    });
  }

    /**
   * Megjeleníti vagy újrarendereli a show xpath copy success állapotát a felhasználói felületen.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   */
function showXPathCopySuccess(){
    if(!xpathCopySuccess) return;
    xpathCopySuccess.classList.add('visible');
    if(xpathCopySuccessTimeout) clearTimeout(xpathCopySuccessTimeout);
    xpathCopySuccessTimeout = setTimeout(() => xpathCopySuccess.classList.remove('visible'), 5000);
  }

    /**
   * Eltávolítja vagy alaphelyzetbe állítja a reset search művelethez tartozó kliensoldali állapotot.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function resetSearch(){
    fieldSearchMatches = [];
    fieldSearchIndex = -1;
    if(fieldSearchResults){
      fieldSearchResults.hidden = true;
      fieldSearchResults.innerHTML = '';
    }
    updateFieldSearchControls();
    updateSelectedXPathUi();
  }

    /**
   * Kezeli vagy beköti a init esemény- és inicializációs folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function init(){
    fieldSearchInput?.addEventListener('input', () => {
      window.clearTimeout(fieldSearchDebounce);
      fieldSearchDebounce = window.setTimeout(() => refreshFieldSearch({ showList:true }), 120);
    });
    fieldSearchInput?.addEventListener('focus', () => refreshFieldSearch({ showList:true }));
    fieldSearchInput?.addEventListener('keydown', event => {
      if(event.key === 'Enter'){
        event.preventDefault();
        if(fieldSearchMatches.length) goToFieldSearchMatch(fieldSearchIndex >= 0 ? fieldSearchIndex : 0);
        if(fieldSearchResults) fieldSearchResults.hidden = true;
      }else if(event.key === 'ArrowDown'){
        event.preventDefault();
        navigateFieldSearch(1);
      }else if(event.key === 'ArrowUp'){
        event.preventDefault();
        navigateFieldSearch(-1);
      }else if(event.key === 'Escape' && fieldSearchResults){
        fieldSearchResults.hidden = true;
      }
    });
    fieldSearchPrevButton?.addEventListener('click', () => navigateFieldSearch(-1));
    fieldSearchNextButton?.addEventListener('click', () => navigateFieldSearch(1));
    bindFieldSearchResults();
    document.addEventListener('click', event => {
      if(fieldSearchResults && !event.target.closest?.('.field-search-toolbar')) fieldSearchResults.hidden = true;
    });
    updateFieldSearchControls();
    updateSelectedXPathUi();
  }

  return {
    init,
    resetSearch,
    canonicalizeXmlPath,
    pathMatches,
    findFieldIdByXmlPath,
    getActiveFormSearchScope,
    updateToggleAllFormCollapseButton,
    toggleAllFormCollapsibles,
    bindCollapseToggles,
    openAncestorsForSelectedField,
    highlightSelections,
    scrollElementWithinContainer,
    scrollXmlToSelected,
    scrollFormToSelected,
    refreshFieldSearch,
    showXPathCopySuccess
  };
}
