/**
 * @module validation/validation-api
 *
 * A XSD/XPath validációs működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A validációs állapotot és a hibák mezőhöz navigálását a szerveroldali eredményekkel összhangban kezeli.
 */

import { apiGetJson, apiPostJson } from '../core/api-client.js';

export const XPATH_TERMINAL_STATUSES = new Set(['FINISHED', 'ABORTED', 'FAILED']);


/**
 * Elindítja a start snapshot xpath validation aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} arg1 a függvény arg1 bemeneti értéke
 * @returns {Promise<*>} a feldolgozás eredménye
 */
export async function startSnapshotXpathValidation({ xmlText, fileName = 'current-form.xml' } = {}){
  const content = String(xmlText || '');
  if(!content.trim()){
    throw new Error('Nincs XPath ellenőrizhető XML tartalom.');
  }
  const formData = new FormData();
  formData.append('file', new File([content], fileName || 'current-form.xml', { type: 'application/xml' }));
  formData.append('createResult', 'ASYNC');
  const response = await fetch('/api/xpath-validator/requests', {
    method: 'POST',
    body: formData,
    credentials: 'same-origin'
  });
  const text = await response.text();
  let body = {};
  if(text){
    try{ body = JSON.parse(text); }
    catch(_error){ body = { message: text }; }
  }
  if(!response.ok){
    if(response.status === 422){
      throw new Error('Az ellenőrzés sikertelen: Az Űrlap állományhoz nincs XPath állomány regisztrálva a rendszerbe.');
    }
    throw new Error(body?.message || body?.error || `Az XPath ellenőrzés nem indítható (HTTP ${response.status}).`);
  }
  return body;
}

/**
 * Elindítja a start active xpath validation aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} arg1 a függvény arg1 bemeneti értéke
 * @returns {Promise<*>} a feldolgozás eredménye
 */
export async function startActiveXpathValidation({ xmlFileId, xmlFileSessionId } = {}){
  try{
    return await apiPostJson('/api/xpath-validator/active/start', {
      xmlFileId,
      xmlFileSessionId
    });
  }catch(error){
    if(error?.status === 422){
      throw new Error('Az ellenőrzés sikertelen: Az Űrlap állományhoz nincs XPath állomány regisztrálva a rendszerbe.');
    }
    throw error;
  }
}

/**
 * Betölti vagy lekéri a get xpath validation status művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} requestId a célobjektum technikai azonosítója
 * @returns {Promise<*>} a feldolgozás eredménye
 */
export async function getXpathValidationStatus(requestId){
  if(!requestId){
    throw new Error('Hiányzó XPath requestId.');
  }
  return apiGetJson(`/api/xpath-validator/requests/${encodeURIComponent(requestId)}`);
}

/**
 * Betölti vagy lekéri a get xpath validation errors művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} requestId a célobjektum technikai azonosítója
 * @returns {Promise<*>} a feldolgozás eredménye
 */
export async function getXpathValidationErrors(requestId){
  if(!requestId){
    return [];
  }
  const result = await apiGetJson(`/api/xpath-validator/requests/${encodeURIComponent(requestId)}/errors`);
  return Array.isArray(result) ? result : [];
}


/**
 * Ellenőrzi a is terminal xpath status feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @returns {*} a feldolgozás eredménye
 */
export function isTerminalXpathStatus(status){
  return XPATH_TERMINAL_STATUSES.has(String(status?.validatorStatus || '').toUpperCase());
}
