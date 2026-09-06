/**
 * @module form/form-shortcuts
 *
 * A űrlap-megjelenítési és mezőkötési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

import { quickSaveCurrentXmlFile } from '../xml/xml-save-service.js';
import { runCurrentFormXpathValidation } from '../validation/xpath-validation.js';

/**
 * Előkészíti és elindítja a register form shortcuts állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
export function registerFormShortcuts(){
  document.addEventListener('keydown', async event => {
    if(!isFormEditorShortcutContext()){
      return;
    }
    if(event.key === 'Escape'){
      return;
    }

    const key = String(event.key || '').toLowerCase();
    const hasModifier = (event.ctrlKey || event.metaKey) && !event.shiftKey && !event.altKey;
    if(!hasModifier){
      return;
    }

    if(key === 'm'){
      event.preventDefault();
      event.stopPropagation();
      window.NavFormRuntime?.closeXmlSaveMenu?.();
      await quickSaveCurrentXmlFile({ quiet: false, skipIfClean: true });
      return;
    }

    if(key === 'e'){
      event.preventDefault();
      event.stopPropagation();
      window.NavFormRuntime?.closeXmlSaveMenu?.();
      await runCurrentFormXpathValidation();
    }
  });
}

/**
 * Ellenőrzi a is form editor shortcut context feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function isFormEditorShortcutContext(){
  return Boolean(
    document.getElementById('formTab') ||
    document.querySelector('.form-unified-toolbar') ||
    document.getElementById('formPreview') ||
    document.getElementById('xmlSourceEditor')
  );
}
