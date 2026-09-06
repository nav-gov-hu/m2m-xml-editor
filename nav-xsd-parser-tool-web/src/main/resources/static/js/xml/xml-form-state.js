/**
 * Feldolgozza a collect form controls from dom bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} root a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @returns {*} a feldolgozás eredménye
 */
/**
 * @module xml/xml-form-state
 *
 * A XML-szerkesztési és állománykezelési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

export function collectFormControlsFromDom(root = document){
  const selector = [
    '.form-field input[data-field-id]',
    '.form-field select[data-field-id]',
    '.form-field textarea[data-field-id]',
    '.uimodel-field input[data-field-id]',
    '.uimodel-field select[data-field-id]',
    '.uimodel-field textarea[data-field-id]'
  ].join(',');
  const controls = [];
  const seen = new Set();
  for(const control of root.querySelectorAll(selector)){
    if(!control || control.id === 'xmlSourceEditor'){
      continue;
    }
    if(seen.has(control)){
      continue;
    }
    seen.add(control);
    const fieldId = control.dataset.fieldId || '';
    const wrapper = control.closest('.form-field, .uimodel-field');
    const xmlPath = wrapper?.dataset?.xmlPath || control.dataset.xmlPath || '';
    controls.push({
      control,
      wrapper,
      fieldId,
      xmlPath,
      value: readControlValue(control),
      empty: isControlValueEmpty(control)
    });
  }
  return controls;
}

/**
 * Betölti vagy lekéri a read control value művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} control a függvény control bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function readControlValue(control){
  if(!control){
    return '';
  }
  if(control.type === 'checkbox'){
    return control.checked ? 'true' : 'false';
  }
  return control.value ?? '';
}

/**
 * Ellenőrzi a is control value empty feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} control a függvény control bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function isControlValueEmpty(control){
  if(!control){
    return true;
  }
  if(control.type === 'checkbox'){
    return false;
  }
  return String(readControlValue(control) ?? '').trim() === '';
}
