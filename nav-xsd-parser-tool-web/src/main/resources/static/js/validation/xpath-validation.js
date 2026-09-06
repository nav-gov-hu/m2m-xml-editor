/**
 * @module validation/xpath-validation
 *
 * A XSD/XPath validációs működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A validációs állapotot és a hibák mezőhöz navigálását a szerveroldali eredményekkel összhangban kezeli.
 */

import { showMessage } from '../core/messages.js';
import { getAppState, syncStateFromRuntime, updateNestedState } from '../core/app-state.js';
import {
  getXpathValidationErrors,
  getXpathValidationStatus,
  isTerminalXpathStatus,
  startActiveXpathValidation,
  startSnapshotXpathValidation
} from './validation-api.js';
import {
  buildXpathToastMessage,
  isXpathResultError,
  renderXpathResult,
  renderXpathXsdBlocked,
  setXpathValidationBusy,
  showXpathError,
  showXpathLoading
} from './xpath-validation-ui.js';
import { renderXsdPrevalidationErrorsFromXpath } from './xsd-validation-result.js';

let xpathValidationBusy = false;

/**
 * Kezeli vagy beköti a init xpath validation esemény- és inicializációs folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
export function initXpathValidation(){
  // Button wiring is handled by form-toolbar.js in phase 3.
}

/**
 * Elindítja a run current form xpath validation aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {Promise<*>} a feldolgozás eredménye
 */
export async function runCurrentFormXpathValidation(){
  if(xpathValidationBusy){
    return null;
  }

  syncStateFromRuntime();
  const state = getAppState();
  const xmlFileId = state.activeXmlFileId;
  const xmlFileSessionId = state.activeXmlFileSessionId;

  if(!xmlFileId){
    showMessage('XPath ellenőrzés csak aktív Űrlapállományon indítható. Előbb nyiss meg egy XML-t az Űrlapállományok oldalon.', 'warning');
    return null;
  }

  xpathValidationBusy = true;
  setXpathValidationBusy(true);
  showXpathLoading('XPath ellenőrzés indítása az aktuális, mentetlen módosításokat is tartalmazó XML-en...');
  window.NavProcessingJobs?.showLocal?.({
    title: 'XPath ellenőrzés',
    message: 'Az aktuális, mentetlen űrlapállapot XPath ellenőrzése folyamatban...',
    status: 'Folyamatban',
    percentText: '',
    progressWidth: '35%'
  });

  try{
    const runtimeApi = window.NavFormRuntime;
    const largeXmlMode = runtimeApi?.isLargeXmlMode?.() === true;
    let status;
    if(largeXmlMode){
      // Large XML validation remains file based because the full document is intentionally not held in browser memory.
      showXpathLoading('Nagy XML XPath ellenőrzés indítása az aktív állományon...');
      status = await startActiveXpathValidation({ xmlFileId, xmlFileSessionId });
    } else {
      const xmlText = String(runtimeApi?.buildCurrentXmlSnapshot?.({ reason: 'xpath-validation' }) || '');
      if(!xmlText.trim()){
        throw new Error('Nincs XPath ellenőrizhető XML tartalom.');
      }
      const fileName = runtimeApi?.getActiveXmlFileName?.() || 'current-form.xml';
      showXpathLoading('XPath ellenőrzés fut az aktuális űrlapállapoton...');
      status = await startSnapshotXpathValidation({ xmlText, fileName });
    }

    if(status?.processingJobId && window.NavProcessingJobs?.startPolling){
      window.NavProcessingJobs.startPolling(status.processingJobId);
    }

    status = await waitForXpathValidationFinished(status);
    const errors = await getXpathValidationErrors(status?.requestId);

    if(isXsdPrevalidationFailure(status, errors)){
      updateNestedState('validation', {
        ...(getAppState().validation || {}),
        xpath: {
          status: status?.validatorStatus || null,
          errorCount: 0,
          lastRequestId: status?.requestId || null
        }
      });
      renderXpathXsdBlocked(status, errors.length);
      renderXsdPrevalidationErrorsFromXpath(status, errors);
      showMessage('Az XPath ellenőrzés nem futott le, mert az aktuális XML XSD hibás. Az XSD hibák az XSD drawerben láthatók.', 'error');
      return { status, errors, blockedByXsd: true };
    }

    updateNestedState('validation', {
      ...(getAppState().validation || {}),
      xpath: {
        status: status?.validatorStatus || null,
        errorCount: Number(status?.errorCount ?? errors.length),
        lastRequestId: status?.requestId || null
      }
    });

    renderXpathResult(status, errors);

    const toastType = isXpathResultError(status, errors) ? 'error' : 'success';
    showMessage(buildXpathToastMessage(status, errors), toastType);
    return { status, errors };
  }catch(error){
    console.error('Űrlapmegtekintő XPath ellenőrzési hiba', error);
    const message = error?.message || 'Az XPath ellenőrzés nem sikerült.';
    showXpathError(message);
    showMessage(message, 'error');
    return null;
  }finally{
    window.NavProcessingJobs?.hide?.();
    xpathValidationBusy = false;
    setXpathValidationBusy(false);
  }
}

/**
 * A <code>waitForXpathValidationFinished</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} initialStatus a kapcsolódó folyamat aktuális állapota
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function waitForXpathValidationFinished(initialStatus){
  let status = initialStatus;
  const startedAt = Date.now();
  while(status?.requestId && !isTerminalXpathStatus(status)){
    if(Date.now() - startedAt > 60000){
      return status;
    }
    await sleep(1000);
    try{
      status = await getXpathValidationStatus(status.requestId);
    }catch(_error){
      return status;
    }
  }
  return status;
}

/**
 * A <code>sleep</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} ms a függvény ms bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function sleep(ms){
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Ellenőrzi a is xsd prevalidation failure feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @param {*} errors a függvény errors bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function isXsdPrevalidationFailure(status, errors){
  const technical = String(status?.technicalErrorMessage || '').toUpperCase();
  const list = Array.isArray(errors) ? errors : [];
  if(technical.includes('XSD VALIDÁCIÓ MEGHIÚSULT') || technical.includes('XSD VALIDACIO MEGHIUSULT')) return true;
  return list.length > 0 && list.every(item => String(item?.errorCode || item?.code || '').toUpperCase().startsWith('XSD_'));
}
