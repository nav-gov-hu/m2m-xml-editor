/**
 * @module main
 *
 * A webes frontend működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

import { syncStateFromRuntime } from './core/app-state.js';
import { initFormToolbar } from './form/form-toolbar.js';
import { registerFormShortcuts } from './form/form-shortcuts.js';
import { initXpathValidation } from './validation/xpath-validation.js';
import { initXsdValidation } from './validation/xsd-validation.js';
import { initM2mUi } from './m2m/m2m-submission-ui.js';
import { installRuntimeBridge } from './runtime/runtime-bridge.js';

/**
 * A <code>bootstrapFrontendModules</code> függvény a webes frontend folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function bootstrapFrontendModules(){
  installRuntimeBridge();
  syncStateFromRuntime();
  initFormToolbar();
  initXsdValidation();
  initXpathValidation();
  registerFormShortcuts();
  initM2mUi();
}

/**
 * Elindítja a start frontend modules aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function startFrontendModules(){
  await (window.NavApplicationRuntimeReady || Promise.resolve());
  bootstrapFrontendModules();
}

if(document.readyState === 'loading'){
  document.addEventListener('DOMContentLoaded', startFrontendModules, { once: true });
}else{
  startFrontendModules();
}
