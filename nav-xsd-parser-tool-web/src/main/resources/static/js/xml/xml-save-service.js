/**
 * @module xml/xml-save-service
 *
 * A XML-szerkesztési és állománykezelési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A mentési folyamat során a kliensoldali állapot csak a szerveroldali jogosultság- és végállapot-ellenőrzést egészíti ki.
 */

import { apiPostJson } from '../core/api-client.js';
import { showMessage } from '../core/messages.js';
import { isFormDirty, syncStateFromRuntime, updateAppState } from '../core/app-state.js';
import { buildCurrentXmlSnapshotInfo } from './xml-snapshot-builder.js';

/**
 * A <code>quickSaveCurrentXmlFile</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @param {*} options a művelet opcionális beállításai
 * @returns {Promise<*>} a feldolgozás eredménye
 */
export async function quickSaveCurrentXmlFile(options = {}){
  const { quiet = false, allowDisabledButton = false, skipIfClean = true } = options;
  const runtimeApi = window.NavFormRuntime;
  syncStateFromRuntime();

  // Nagy XML részleges módban kizárólag a runtime célzott fragment-mentése használható.
  // A normál /overwrite végpont részleges XML-lel felülírná a teljes forrásállományt.
  if(runtimeApi?.isLargeXmlMode?.() === true){
    return await runtimeApi.quickSaveCurrentXmlFile?.(options);
  }

  const button = document.getElementById('quickSaveXmlFileButton');
  if(!button){
    return false;
  }
  if(button.disabled && !allowDisabledButton){
    return false;
  }
  if(!runtimeApi?.hasActiveEditableXmlForServerSave?.()){
    runtimeApi?.requireActiveEditableXmlForServerSave?.();
    return false;
  }
  if(skipIfClean && !isFormDirty() && runtimeApi?.hasUnsyncedFormControlChanges?.() !== true){
    if(!quiet){
      showMessage('Nincs mentetlen módosítás.', 'info');
    }
    return true;
  }

  const snapshot = buildCurrentXmlSnapshotInfo();
  const xmlContent = snapshot.xml;
  if(!String(xmlContent || '').trim()){
    showMessage('Nincs gyorsmenthető XML tartalom.', 'error');
    return false;
  }

  const fileId = runtimeApi.getActiveXmlFileId?.();
  const sessionId = runtimeApi.getActiveXmlFileSessionId?.();
  if(!fileId || !sessionId){
    showMessage('Nincs aktív, szerkeszthető XML állomány.', 'warning');
    return false;
  }

  button.disabled = true;
  button.classList.add('is-saving');
  try{
    const result = await apiPostJson(`/api/xml-files/${encodeURIComponent(fileId)}/overwrite`, {
      xmlContent,
      sessionId,
      runXsdValidation: true,
      allowInvalidXml: true,
      userNote: 'Gyorsmentés az űrlapnézetből'
    });

    runtimeApi.markFormClean?.();
    globalThis.currentMultiformState?.markActivePanelSaved?.();
    updateAppState({ lastSavedXmlHash: snapshot.hash, currentSnapshotHash: snapshot.hash });
    syncStateFromRuntime();

    if(!quiet){
      showMessage(`${result?.message || 'Gyorsmentés kész'}${result?.revisionNo ? ' Revision: ' + result.revisionNo : ''}`, 'success');
    }
    return true;
  }catch(error){
    showMessage(error.message || 'A gyorsmentés nem sikerült.', 'error');
    return false;
  }finally{
    button.classList.remove('is-saving');
    runtimeApi.updateQuickSaveXmlButtonState?.();
    syncStateFromRuntime();
  }
}
