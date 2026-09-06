/**
 * @module runtime/runtime-bridge
 *
 * A alkalmazási runtime- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

import { quickSaveCurrentXmlFile } from '../xml/xml-save-service.js';
import { runCurrentFormXpathValidation } from '../validation/xpath-validation.js';
import { runCurrentFormXsdValidation, startActiveFileXsdValidation } from '../validation/xsd-validation.js';
import { installXpathErrorNavigationGlobal, resolveXpathErrorTarget } from '../validation/xpath-error-navigation.js';
import { installXpathValidationResultGlobal } from '../validation/xpath-validation-result.js';
import { installXsdValidationResultGlobal } from '../validation/xsd-validation-result.js';

/**
 * A <code>installRuntimeBridge</code> függvény a alkalmazási runtime- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
export function installRuntimeBridge(){
  installXpathErrorNavigationGlobal();
  installXpathValidationResultGlobal();
  installXsdValidationResultGlobal();

  window.NavModularActions = {
    quickSaveCurrentXmlFile,
    runCurrentFormXpathValidation,
    resolveXpathErrorTarget,
    runCurrentFormXsdValidation,
    startActiveFileXsdValidation
  };
}
