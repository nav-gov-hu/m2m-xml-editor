/**
 * @module validation/xpath-validation-ui
 *
 * A XSD/XPath validációs működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A validációs állapotot és a hibák mezőhöz navigálását a szerveroldali eredményekkel összhangban kezeli.
 */

import { showMessage } from '../core/messages.js';
import { buildXpathToastMessage as buildToastMessage, isTerminalValidationFailure } from './validation-status-utils.js';
import {
  ensureXpathValidationDrawer,
  renderXpathValidationPopup,
  renderXpathBlockedByXsd,
  setCurrentXpathValidationState,
  showXpathError as renderXpathError,
  showXpathLoading as renderXpathLoading
} from './xpath-validation-result.js';

/**
 * Betölti vagy lekéri a get xpath validation ui művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
export function getXpathValidationUi(){
  return window.NavFormRuntime || {};
}

/**
 * A <code>ensureXpathValidationUi</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
export function ensureXpathValidationUi(){
  ensureXpathValidationDrawer();
  return getXpathValidationUi();
}

/**
 * Szinkronizálja vagy frissíti a set xpath validation busy által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} disabled a függvény disabled bemeneti értéke
 */
export function setXpathValidationBusy(disabled){
  const runtimeApi = getXpathValidationUi();
  if(typeof runtimeApi.setFormXpathValidationButtonDisabled === 'function'){
    runtimeApi.setFormXpathValidationButtonDisabled(Boolean(disabled));
    return;
  }
  const button = document.getElementById('formXPathValidateButton');
  if(button){
    button.disabled = Boolean(disabled);
  }
}

/**
 * Megjeleníti vagy újrarendereli a show xpath loading állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
 */
export function showXpathLoading(message){
  renderXpathLoading(message);
}

/**
 * Megjeleníti vagy újrarendereli a show xpath error állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
 */
export function showXpathError(message){
  ensureXpathValidationUi();
  if(message){
    renderXpathError(message);
  }else{
    renderXpathError('Az XPath ellenőrzés nem sikerült.');
  }
  if(!document.getElementById('formXpathValidationPopup')){
    showMessage(message || 'Az XPath ellenőrzés nem sikerült.', 'error');
  }
}

/**
 * Megjeleníti vagy újrarendereli a render xpath result állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @param {*} errors a függvény errors bemeneti értéke
 */
export function renderXpathResult(status, errors){
  setCurrentXpathValidationState(status, errors);
  renderXpathValidationPopup(status, Array.isArray(errors) ? errors : []);
}

/**
 * Feldolgozza a build xpath toast message bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @param {*} errors a függvény errors bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function buildXpathToastMessage(status, errors){
  return buildToastMessage(status, errors);
}

/**
 * Ellenőrzi a is xpath result error feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @param {*} errors a függvény errors bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function isXpathResultError(status, errors){
  return isTerminalValidationFailure(status, errors);
}

/**
 * Megjeleníti vagy újrarendereli a render xpath xsd blocked állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @param {*} xsdErrorCount a függvény xsdErrorCount bemeneti értéke
 */
export function renderXpathXsdBlocked(status, xsdErrorCount){
  renderXpathBlockedByXsd(status, xsdErrorCount);
}
