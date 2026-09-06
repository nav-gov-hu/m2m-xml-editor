/**
 * @module validation/xsd-validation
 *
 * A XSD/XPath validációs működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A validációs állapotot és a hibák mezőhöz navigálását a szerveroldali eredményekkel összhangban kezeli.
 */

import { showMessage } from '../core/messages.js';
import { syncStateFromRuntime } from '../core/app-state.js';
import { initXsdValidationUi } from './xsd-validation-ui.js';

/**
 * Kezeli vagy beköti a init xsd validation esemény- és inicializációs folyamatát.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 */
export function initXsdValidation(){
  initXsdValidationUi();
  // Button wiring is owned by form-toolbar.js in this phase.
}

/**
 * Elindítja a run current form xsd validation aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} options a művelet opcionális beállításai
 * @returns {Promise<*>} a feldolgozás eredménye
 */
export async function runCurrentFormXsdValidation(options = {}){
  syncStateFromRuntime();
  const runtimeApi = window.NavFormRuntime;
  if(typeof runtimeApi?.validateCurrentXml === 'function'){
    const result = await runtimeApi.validateCurrentXml(options);
    syncStateFromRuntime();
    return result;
  }
  if(typeof runtimeApi?.startActiveFileXsdValidation === 'function'){
    const result = await runtimeApi.startActiveFileXsdValidation(options);
    syncStateFromRuntime();
    return result;
  }
  showMessage('Az XSD ellenorzes modul nem erheto el.', 'error');
  return null;
}

/**
 * Elindítja a start active file xsd validation aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} options a művelet opcionális beállításai
 * @returns {Promise<*>} a feldolgozás eredménye
 */
export async function startActiveFileXsdValidation(options = {}){
  syncStateFromRuntime();
  const runtimeApi = window.NavFormRuntime;
  if(typeof runtimeApi?.startActiveFileXsdValidation === 'function'){
    const result = await runtimeApi.startActiveFileXsdValidation(options);
    syncStateFromRuntime();
    return result;
  }
  showMessage('Az aktiv allomany XSD ellenorzese nem erheto el.', 'error');
  return null;
}
