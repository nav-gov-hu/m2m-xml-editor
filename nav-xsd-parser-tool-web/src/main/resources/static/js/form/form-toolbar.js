/**
 * @module form/form-toolbar
 *
 * A űrlap-megjelenítési és mezőkötési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

import { quickSaveCurrentXmlFile } from '../xml/xml-save-service.js';
import { runCurrentFormXpathValidation } from '../validation/xpath-validation.js';
import { runCurrentFormXsdValidation } from '../validation/xsd-validation.js';
import { initFormToolbarState, refreshFormToolbarState } from './form-toolbar-state.js';

/**
 * Kezeli vagy beköti a init form toolbar esemény- és inicializációs folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
export function initFormToolbar(){
  bindQuickSaveButton();
  bindXsdValidationButton();
  bindXpathValidationButton();
  bindToggleAllButton();
  bindUiModelDetailsButton();
  bindMissingFieldsButton();
  initFormToolbarState();
}

/**
 * Kezeli vagy beköti a bind once esemény- és inicializációs folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} button a függvény button bemeneti értéke
 * @param {*} marker a függvény marker bemeneti értéke
 * @param {*} handler a függvény handler bemeneti értéke
 */
function bindOnce(button, marker, handler){
  if(!button || button.dataset[marker] === 'true') return;
  button.dataset[marker] = 'true';
  button.addEventListener('click', handler);
}

/**
 * Kezeli vagy beköti a bind quick save button esemény- és inicializációs folyamatát.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 */
function bindQuickSaveButton(){
  const button = document.getElementById('quickSaveXmlFileButton');
  if(button){
    button.title = 'Gyorsmentes: aktualis urlapallapot mentese (Ctrl+M)';
  }
  bindOnce(button, 'modularQuickSaveBound', () => quickSaveCurrentXmlFile({ quiet: false, skipIfClean: true }));
}

/**
 * Kezeli vagy beköti a bind xsd validation button esemény- és inicializációs folyamatát.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 */
function bindXsdValidationButton(){
  const button = document.getElementById('validateCurrentXmlButton');
  if(button){
    button.title = button.title || 'XSD ellenorzes';
  }
  bindOnce(button, 'modularXsdBound', () => runCurrentFormXsdValidation());
}

/**
 * Kezeli vagy beköti a bind xpath validation button esemény- és inicializációs folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function bindXpathValidationButton(){
  const button = document.getElementById('formXPathValidateButton');
  if(button){
    button.title = 'XPath Ellenőrzés';
  }
  bindOnce(button, 'modularXpathBound', () => runCurrentFormXpathValidation());
}

/**
 * Kezeli vagy beköti a bind toggle all button esemény- és inicializációs folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function bindToggleAllButton(){
  const button = document.getElementById('toggleAllFormCollapseButton');
  bindOnce(button, 'modularToggleAllBound', () => {
    window.NavFormRuntime?.toggleAllFormCollapsibles?.();
    refreshFormToolbarState();
  });
}

/**
 * Kezeli vagy beköti a bind ui model details button esemény- és inicializációs folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function bindUiModelDetailsButton(){
  const button = document.getElementById('uiModelDetailsToggle');
  bindOnce(button, 'modularUiModelDetailsBound', () => {
    const visible = window.NavFormRuntime?.isUiModelDetailsVisible?.() === true;
    window.NavFormRuntime?.setUiModelDetailsVisible?.(!visible);
    refreshFormToolbarState();
  });
}

/**
 * Kezeli vagy beköti a bind missing fields button esemény- és inicializációs folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function bindMissingFieldsButton(){
  const button = document.getElementById('toggleEmptyUiModelFieldsButton');
  bindOnce(button, 'modularMissingFieldsBound', () => {
    if(window.NavFormRuntime?.isReadOnly?.()){
      window.NavFormRuntime?.setUiModelMissingFieldsVisible?.(false);
      refreshFormToolbarState();
      return;
    }
    const visible = window.NavFormRuntime?.isUiModelMissingFieldsVisible?.() === true;
    window.NavFormRuntime?.setUiModelMissingFieldsVisible?.(!visible);
    refreshFormToolbarState();
  });
}
