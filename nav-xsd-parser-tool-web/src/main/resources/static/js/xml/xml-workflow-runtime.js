/**
 * @module xml/xml-workflow-runtime
 *
 * A XML-szerkesztési és állománykezelési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

/**
 * XML upload/validate workflow, splitter and large-file mode.
 * Shared runtime state is initialized by runtime-context.js.
 */

async function loadXmlFileFromFormPanel(file){
  if(!file) return;
  clearResults();
  setBusy(true);
  resultMode.textContent = 'Validálás';

  const requestData = new FormData();
  const schemaDir = schemaDirInput?.value?.trim();
  const generalXsdDir = generalXsdDirInput?.value?.trim();
  if(schemaDir) requestData.append('schemaDir', schemaDir);
  if(generalXsdDir) requestData.append('generalXsdDir', generalXsdDir);
  requestData.append('xmlFile', file, file.name || 'input.xml');

  try{
    const response = await fetch('/api/validate', { method:'POST', body: requestData });
    const data = await response.json();
    if(!response.ok) throw new Error(data.error || 'Az XML betöltése nem sikerült.');

    // Keep the selected file available for existing UI helpers that rely on xmlFileInput.
    try{
      if(xmlFileInput && window.DataTransfer){
        const dt = new DataTransfer();
        dt.items.add(file);
        xmlFileInput.files = dt.files;
        document.querySelector('input[name="xmlMode"][value="upload"]')?.click();
        syncSelectedXmlSourceDisplay();
      }
    }catch(e){
      console.warn('A kiválasztott XML fájl nem szinkronizálható az alap fájlmezővel.', e);
    }

    renderValidate(data, { preserveExistingFormOnInvalid: true });
    clearLargeXmlSourceIfNeeded();
    if(data.formDefinition && (data.xmlView?.rawXml || data.partialPreview === true || data.largeFileMode === true)) activateTab('formTab');
  }catch(error){
    console.error('XML betöltési hiba', error);
    showMessage(error.message || 'Ismeretlen hiba történt az XML betöltésekor.', 'error');
    showMessage(error.message || 'Ismeretlen hiba történt az XML betöltésekor.', 'error');
  }finally{
    setBusy(false);
    if(formXmlLoadInput) formXmlLoadInput.value = '';
  }
}

/**
 * Elindítja a run action aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} endpoint a függvény endpoint bemeneti értéke
 * @param {*} modeLabel a függvény modeLabel bemeneti értéke
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function runAction(endpoint, modeLabel){
  clearResults();
  resetFormTab();
  setBusy(true);
  resultMode.textContent = modeLabel;

  const requestData = new FormData();
  if(schemaDirInput.value.trim()) requestData.append('schemaDir', schemaDirInput.value.trim());
  if(generalXsdDirInput.value.trim()) requestData.append('generalXsdDir', generalXsdDirInput.value.trim());

  if(currentMode() === 'upload'){
    if(!xmlFileInput.files.length){
      setBusy(false);
      showMessage('Válassz ki egy XML fájlt.', 'error');
      return;
    }
    requestData.append('xmlFile', xmlFileInput.files[0]);
  } else {
    if(!xmlPathInput.value.trim()){
      setBusy(false);
      showMessage('Add meg az XML elérési útját.', 'error');
      return;
    }
    requestData.append('xmlPath', xmlPathInput.value.trim());
  }

  try{
    const response = await fetch(endpoint, { method:'POST', body:requestData });
    const data = await response.json();
    if(!response.ok) throw new Error(data.error || 'A kérés feldolgozása nem sikerült.');
    if(endpoint.endsWith('/inspect')) renderInspect(data);
    else renderValidate(data);
  }catch(error){
    console.error('Futtatási hiba', error);
    showMessage(error.message || 'Ismeretlen hiba történt.', 'error');
  }finally{
    setBusy(false);
  }
}

/**
 * Kezeli vagy beköti a init splitter esemény- és inicializációs folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function initSplitter(){
  if(!splitter || !splitContainer || !formPanel || !xmlPanel) return;
  let dragging = false;
    /**
   * Elindítja a start aszinkron vagy több lépéses frontend folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   */
const start = event => {
    if(window.innerWidth <= 900) return;
    dragging = true;
    splitter.classList.add('dragging');
    document.body.style.userSelect = 'none';
    event.preventDefault();
  };
    /**
   * A <code>move</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   */
const move = event => {
    if(!dragging || paneState.formCollapsed || paneState.xmlCollapsed) return;
    const rect = splitContainer.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const percent = Math.max(20, Math.min(80, (x / rect.width) * 100));
    paneState.formWidthPercent = percent;
    formPanel.style.flex = `0 0 ${percent}%`;
    xmlPanel.style.flex = '1 1 auto';
  };
    /**
   * A <code>stop</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
const stop = () => {
    if(!dragging || paneState.formCollapsed || paneState.xmlCollapsed) return;
    dragging = false;
    splitter.classList.remove('dragging');
    document.body.style.userSelect = '';
  };
  splitter.addEventListener('mousedown', start);
  window.addEventListener('mousemove', move);
  window.addEventListener('mouseup', stop);
  window.addEventListener('resize', applyPaneState);
}

/**
 * Eltávolítja vagy alaphelyzetbe állítja a clear large xml source if needed művelethez tartozó kliensoldali állapotot.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function clearLargeXmlSourceIfNeeded(){
  if(!(currentActiveXmlFile && currentActiveXmlFile.largeFileMode === true)) return;
  if(xmlSourceEditor){
    xmlSourceEditor.value = 'Nagy XML mód aktív. A teljes XML forrás böngésző oldali megjelenítése tiltott.';
    xmlSourceEditor.readOnly = true;
  }
  if(xmlSourceHighlight){
    xmlSourceHighlight.textContent = 'Nagy XML mód aktív. A teljes XML forrás böngésző oldali megjelenítése tiltott.';
  }
}

/**
 * Szinkronizálja vagy frissíti a apply large file mode for active xml által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function applyLargeFileModeForActiveXml(){
  const large = currentActiveXmlFile && currentActiveXmlFile.largeFileMode === true;
  document.body.classList.toggle('large-file-mode-active', !!large);
  document.querySelectorAll('[data-xml-view="tree"], input[name="formViewMode"][value="xml-tree"]').forEach(el => {
    el.disabled = !!large;
    el.title = large ? 'Nagy XML módban az XML fa nézet tiltott.' : '';
  });
  const xmlSourceButton = document.getElementById('showXmlSourceButton');
  if(xmlSourceButton){
    xmlSourceButton.disabled = !!large;
    xmlSourceButton.title = large ? 'Nagy XML módban a teljes XML forrás nézet korlátozott.' : 'XML forrás';
  }
  if(large){
    showMessage('Nagy XML mód aktív: az XML fa és teljes XML forrás nézet korlátozott. Az űrlapnézet az elsődleges.', 'warning');
    setViewMode('table');
  }
}

Object.assign(globalThis, {
  loadXmlFileFromFormPanel,
  runAction,
  initSplitter,
  clearLargeXmlSourceIfNeeded,
  applyLargeFileModeForActiveXml
});
