/**
 * @module form/form-toolbar-state
 *
 * A űrlap-megjelenítési és mezőkötési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

import { onStateChanged, syncStateFromRuntime } from '../core/app-state.js';

/**
 * Kezeli vagy beköti a init form toolbar state esemény- és inicializációs folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
export function initFormToolbarState(){
  syncStateFromRuntime();
  refreshFormToolbarState();
  onStateChanged(refreshFormToolbarState);
}

/**
 * Szinkronizálja vagy frissíti a refresh form toolbar state által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
export function refreshFormToolbarState(){
  const runtimeApi = window.NavFormRuntime;
  runtimeApi?.updateQuickSaveXmlButtonState?.();
  runtimeApi?.updateToggleAllFormCollapseButton?.();
}
