/**
 * @module xml/xml-snapshot-builder
 *
 * A XML-szerkesztési és állománykezelési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

import { syncStateFromRuntime, updateAppState } from '../core/app-state.js';
import { collectFormControlsFromDom } from './xml-form-state.js';

/**
 * Feldolgozza a build current xml snapshot bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
export function buildCurrentXmlSnapshot(options = {}){
  return buildCurrentXmlSnapshotInfo(options).xml;
}

/**
 * Feldolgozza a build current xml snapshot info bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
export function buildCurrentXmlSnapshotInfo(options = {}){
  const runtimeApi = window.NavFormRuntime;
  let info = null;

  if(typeof runtimeApi?.buildCurrentXmlSnapshotInfo === 'function'){
    info = runtimeApi.buildCurrentXmlSnapshotInfo(options);
  }else if(typeof runtimeApi?.buildCurrentXmlSnapshot === 'function'){
    info = createSnapshotInfo(runtimeApi.buildCurrentXmlSnapshot(options), 'RUNTIME_XML_SNAPSHOT');
  }else{
    const editorValue = document.getElementById('xmlSourceEditor')?.value;
    if(editorValue && editorValue.trim()){
      info = createSnapshotInfo(editorValue, 'XML_SOURCE_EDITOR_FALLBACK');
    }
  }

  if(!info){
    throw new Error('Nem sikerült előállítani az aktuális XML snapshotot.');
  }

  const normalized = normalizeSnapshotInfo(info);
  const formState = collectSnapshotFormState();
  normalized.formControlCount = formState.formControlCount;
  normalized.pathBoundControlCount = formState.pathBoundControlCount;
  normalized.missingPathControlCount = formState.missingPathControlCount;
  syncStateFromRuntime();
  updateAppState({
    currentSnapshotHash: normalized.hash,
    currentSnapshotSource: normalized.source,
    currentSnapshotBuiltAt: normalized.createdAt
  });
  return normalized;
}

/**
 * Betölti vagy lekéri a get last xml snapshot info művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
export function getLastXmlSnapshotInfo(){
  const runtimeInfo = window.NavFormRuntime?.getLastXmlSnapshotInfo?.();
  if(runtimeInfo){
    return normalizeSnapshotInfo(runtimeInfo);
  }
  return null;
}

/**
 * Feldolgozza a normalize snapshot info bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} info a függvény info bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function normalizeSnapshotInfo(info){
  const xml = String(info?.xml || '');
  return {
    ...info,
    xml,
    source: info?.source || 'UNKNOWN',
    charLength: Number(info?.charLength ?? xml.length),
    byteLength: Number(info?.byteLength ?? byteLength(xml)),
    hash: info?.hash || lightweightStringHash(xml),
    createdAt: info?.createdAt || new Date().toISOString()
  };
}

/**
 * Előkészíti és elindítja a create snapshot info állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} xml a feldolgozandó XML-tartalom vagy XML DOM-objektum
 * @param {*} source a függvény source bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function createSnapshotInfo(xml, source){
  const text = String(xml || '');
  return {
    xml: text,
    source,
    charLength: text.length,
    byteLength: byteLength(text),
    hash: lightweightStringHash(text),
    createdAt: new Date().toISOString()
  };
}

/**
 * A <code>byteLength</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} text a függvény text bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function byteLength(text){
  try{
    return new Blob([text]).size;
  }catch(_error){
    return text.length;
  }
}

/**
 * A <code>lightweightStringHash</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} text a függvény text bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function lightweightStringHash(text){
  const value = String(text || '');
  let hash = 2166136261;
  for(let i = 0; i < value.length; i += 1){
    hash ^= value.charCodeAt(i);
    hash = Math.imul(hash, 16777619);
  }
  return (hash >>> 0).toString(16).padStart(8, '0');
}

/**
 * Feldolgozza a collect snapshot form state bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function collectSnapshotFormState(){
  try{
    const controls = collectFormControlsFromDom();
    return {
      formControlCount: controls.length,
      pathBoundControlCount: controls.filter(item => item.xmlPath).length,
      missingPathControlCount: controls.filter(item => !item.xmlPath).length
    };
  }catch(_error){
    return { formControlCount: 0, pathBoundControlCount: 0, missingPathControlCount: 0 };
  }
}
