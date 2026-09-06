/**
 * @module print/print-ui
 *
 * A nyomtatási működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

/**
 * Nyomtatasi menu, bongeszos nyomtatas es PDF letoltes kezelese.
 */
export function createPrintUi({
  elements,
  showMessage,
  parseXml,
  suggestXmlFileName,
  getCurrentMode,
  getCurrentXmlText
}){
  const {
    schemaDirInput,
    generalXsdDirInput,
    xmlFileInput,
    xmlPathInput,
    printHtmlButton,
    printPdfButton,
    printMenuButton,
    printMenu,
    printRunButton,
    printShowFieldIdsCheckbox,
    printOnlyFilledFieldsCheckbox,
    printUiModelPathInput
  } = elements;

    /**
   * A <code>togglePrintMenu</code> függvény a nyomtatási folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   */
function togglePrintMenu(event){
    event?.stopPropagation?.();
    if(!printMenu || !printMenuButton) return;
    const willOpen = printMenu.hidden;
    printMenu.hidden = !willOpen;
    printMenuButton.setAttribute('aria-expanded', String(willOpen));
  }

    /**
   * Elrejti vagy lezárja a close print menu felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   */
function closePrintMenu(){
    if(!printMenu || !printMenuButton) return;
    printMenu.hidden = true;
    printMenuButton.setAttribute('aria-expanded', 'false');
  }

    /**
   * Betölti vagy lekéri a get selected print format művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @returns {*} a feldolgozás eredménye
   */
function getSelectedPrintFormat(){
    const selected = document.querySelector('input[name="printFormat"]:checked');
    return selected?.value === 'pdf' ? 'pdf' : 'html';
  }

    /**
   * Feldolgozza a build print form data bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @returns {*} a feldolgozás eredménye
   */
function buildPrintFormData(){
    const formData = new FormData();
    const schemaDir = schemaDirInput?.value?.trim();
    const generalXsdDir = generalXsdDirInput?.value?.trim();
    const uiModelPath = printUiModelPathInput?.value?.trim();
    if(schemaDir) formData.append('schemaDir', schemaDir);
    if(generalXsdDir) formData.append('generalXsdDir', generalXsdDir);
    if(uiModelPath) formData.append('uiModelPath', uiModelPath);
    formData.append('showFieldIds', printShowFieldIdsCheckbox?.checked ? 'true' : 'false');
    formData.append('onlyFilledFields', printOnlyFilledFieldsCheckbox?.checked ? 'true' : 'false');

    const currentXmlText = getCurrentXmlText();
    if(currentXmlText && currentXmlText.trim()){
      parseXml(currentXmlText);
      formData.append('xmlFile', new File([currentXmlText], suggestXmlFileName(), { type:'application/xml' }));
      return formData;
    }

    const mode = getCurrentMode();
    if(mode === 'upload'){
      const file = xmlFileInput?.files?.[0];
      if(!file){
        throw new Error('Nyomtatashoz elobb tolts be XML-t, valassz ki XML fajlt vagy adj meg szerver oldali XML utvonalat.');
      }
      formData.append('xmlFile', file, file.name);
      return formData;
    }

    const xmlPath = xmlPathInput?.value?.trim();
    if(!xmlPath){
      throw new Error('Nyomtatashoz elobb tolts be XML-t vagy add meg az XML eleresi utjat.');
    }
    formData.append('xmlPath', xmlPath);
    return formData;
  }

    /**
   * Betölti vagy lekéri a read print error művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   * @param {*} response a backend-hívás feldolgozandó válasza
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function readPrintError(response){
    const text = await response.text();
    if(!text) return 'A nyomtatasi nezett eloallitasa nem sikerult.';
    try{
      const json = JSON.parse(text);
      return json?.message || text;
    }catch(_error){
      return text;
    }
  }

    /**
   * Előkészíti és elindítja a create print frame állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function createPrintFrame(){
    const frame = document.createElement('iframe');
    frame.setAttribute('title', 'Nyomtatasi nezett');
    frame.setAttribute('aria-hidden', 'true');
    frame.style.position = 'fixed';
    frame.style.left = '-10000px';
    frame.style.top = '0';
    frame.style.width = '1px';
    frame.style.height = '1px';
    frame.style.border = '0';
    frame.style.opacity = '0';
    frame.style.pointerEvents = 'none';
    return frame;
  }

    /**
   * Eltávolítja vagy alaphelyzetbe állítja a remove print frame művelethez tartozó kliensoldali állapotot.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} frame a függvény frame bemeneti értéke
   */
function removePrintFrame(frame){
    try{ frame?.remove?.(); }catch(_error){ /* best effort cleanup */ }
  }

    /**
   * A <code>waitForFrameResources</code> függvény a nyomtatási folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} frame a függvény frame bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function waitForFrameResources(frame){
    const doc = frame.contentDocument;
    if(!doc) return;
    try{ await doc.fonts?.ready; }catch(_error){ /* optional */ }
    const pendingImages = Array.from(doc.images || [])
      .filter(image => !image.complete)
      .map(image => new Promise(resolve => {
        image.addEventListener('load', resolve, { once:true });
        image.addEventListener('error', resolve, { once:true });
      }));
    if(pendingImages.length) await Promise.all(pendingImages);
  }

    /**
   * A <code>printHtmlInPlace</code> függvény a nyomtatási folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} html a függvény html bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function printHtmlInPlace(html){
    const frame = createPrintFrame();
    let cleaned = false;
        /**
     * A <code>cleanup</code> függvény a nyomtatási folyamat egy önálló feldolgozási lépését valósítja meg.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     */
const cleanup = () => {
      if(cleaned) return;
      cleaned = true;
      removePrintFrame(frame);
    };

    try{
      await new Promise((resolve, reject) => {
        const timeout = window.setTimeout(() => reject(new Error('A nyomtatasi nezet betoltese tullepte az idokeretet.')), 15000);
        frame.addEventListener('load', () => {
          window.clearTimeout(timeout);
          resolve();
        }, { once:true });
        frame.srcdoc = html;
        document.body.appendChild(frame);
      });
      await waitForFrameResources(frame);
      const printWindow = frame.contentWindow;
      if(!printWindow) throw new Error('A nyomtatasi nezet nem erheto el.');
      printWindow.addEventListener('afterprint', cleanup, { once:true });
      printWindow.focus();
      printWindow.print();
      window.setTimeout(cleanup, 60000);
    }catch(error){
      cleanup();
      throw error;
    }
  }

    /**
   * A <code>suggestedPdfFileName</code> függvény a nyomtatási folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function suggestedPdfFileName(){
    const xmlName = suggestXmlFileName?.() || 'nyomtatas.xml';
    const base = xmlName.replace(/\.xml$/i, '') || 'nyomtatas';
    return `${base}.pdf`;
  }

    /**
   * A <code>downloadPdfBlob</code> függvény a nyomtatási folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} blob a függvény blob bemeneti értéke
   */
function downloadPdfBlob(blob){
    const blobUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = blobUrl;
    link.download = suggestedPdfFileName();
    link.style.display = 'none';
    document.body.appendChild(link);
    try{
      link.click();
    }finally{
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(blobUrl), 60000);
    }
  }

    /**
   * Elindítja a run print aszinkron vagy több lépéses frontend folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} format a függvény format bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function runPrint(format){
    try{
      const formData = buildPrintFormData();
      const response = await fetch(`/api/print/${format}`, { method:'POST', body: formData });
      if(!response.ok){
        throw new Error(await readPrintError(response));
      }

      if(format === 'pdf'){
        downloadPdfBlob(await response.blob());
        showMessage('A PDF elkészült és letöltésre került.', 'success');
        return;
      }

      await printHtmlInPlace(await response.text());
    }catch(error){
      console.error(error);
      showMessage(error?.message || 'Ismeretlen hiba történt a nyomtatás közben.', 'error');
    }
  }

    /**
   * Kezeli vagy beköti a init esemény- és inicializációs folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function init(){
    printHtmlButton?.addEventListener('click', () => runPrint('html'));
    printPdfButton?.addEventListener('click', () => runPrint('pdf'));
    printMenuButton?.addEventListener('click', togglePrintMenu);
    printRunButton?.addEventListener('click', () => {
      closePrintMenu();
      runPrint(getSelectedPrintFormat());
    });
    document.addEventListener('click', event => {
      if(!printMenu || printMenu.hidden) return;
      if(event.target.closest('#printDropdown')) return;
      closePrintMenu();
    });
    document.addEventListener('keydown', event => {
      if(event.key === 'Escape') closePrintMenu();
    });
  }

  return { init, runPrint, closePrintMenu };
}
