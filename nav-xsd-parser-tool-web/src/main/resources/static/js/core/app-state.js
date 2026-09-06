/**
 * @module core/app-state
 *
 * A közös frontend infrastruktúra- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

const state = {
  activeXmlFileId: null,
  activeXmlFileName: null,
  activeXmlFileSessionId: null,
  documentType: null,
  documentVersion: null,
  formDirty: false,
  readOnly: false,
  lastSavedXmlHash: null,
  currentSnapshotHash: null,
  currentSnapshotSource: null,
  currentSnapshotBuiltAt: null,
  validation: {
    xsd: { status: null, errorCount: 0 },
    xpath: { status: null, errorCount: 0, lastRequestId: null }
  },
  m2m: {
    activeSubmissionId: null,
    status: null
  }
};

const listeners = new Set();

/**
 * Betölti vagy lekéri a get app state művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
export function getAppState(){
  return state;
}

/**
 * Szinkronizálja vagy frissíti a update app state által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} patch a függvény patch bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function updateAppState(patch = {}){
  Object.assign(state, patch);
  notifyStateChanged();
  return state;
}

/**
 * Szinkronizálja vagy frissíti a update nested state által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} section a függvény section bemeneti értéke
 * @param {*} patch a függvény patch bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function updateNestedState(section, patch = {}){
  state[section] = { ...(state[section] || {}), ...patch };
  notifyStateChanged();
  return state[section];
}


/**
 * Ellenőrzi a is form dirty feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
export function isFormDirty(){
  return state.formDirty === true;
}

/**
 * Kezeli vagy beköti a on state changed esemény- és inicializációs folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} listener a függvény listener bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function onStateChanged(listener){
  if(typeof listener !== 'function'){
    return () => {};
  }
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/**
 * Szinkronizálja vagy frissíti a sync state from runtime által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
export function syncStateFromRuntime(){
  const runtimeApi = window.NavFormRuntime;
  if(!runtimeApi){
    return state;
  }
  updateAppState({
    activeXmlFileId: runtimeApi.getActiveXmlFileId?.() || null,
    activeXmlFileName: runtimeApi.getActiveXmlFileName?.() || null,
    activeXmlFileSessionId: runtimeApi.getActiveXmlFileSessionId?.() || null,
    formDirty: Boolean(runtimeApi.isFormDirty?.()),
    readOnly: Boolean(runtimeApi.isReadOnly?.()),
    documentType: runtimeApi.getDocumentType?.() || state.documentType || null,
    documentVersion: runtimeApi.getDocumentVersion?.() || state.documentVersion || null
  });
  return state;
}

/**
 * A <code>notifyStateChanged</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function notifyStateChanged(){
  for(const listener of listeners){
    try{
      listener(state);
    }catch(error){
      console.warn('App state listener error', error);
    }
  }
}
