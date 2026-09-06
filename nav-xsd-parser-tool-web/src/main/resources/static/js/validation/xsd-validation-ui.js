/**
 * @module validation/xsd-validation-ui
 *
 * A XSD/XPath validációs működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A validációs állapotot és a hibák mezőhöz navigálását a szerveroldali eredményekkel összhangban kezeli.
 */

import {
  ensureXsdValidationDrawer,
  openXsdValidationDrawer,
  renderInlineXsdValidationResult,
  renderStoredXsdValidationResult,
  updateXsdDrawerTab
} from './xsd-validation-result.js';

/**
 * Kezeli vagy beköti a init xsd validation ui esemény- és inicializációs folyamatát.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 */
export function initXsdValidationUi(){
  ensureXsdValidationDrawer();
}

export { updateXsdDrawerTab, openXsdValidationDrawer, renderStoredXsdValidationResult, renderInlineXsdValidationResult };
