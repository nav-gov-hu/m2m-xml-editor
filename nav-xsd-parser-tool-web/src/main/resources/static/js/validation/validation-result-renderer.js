/**
 * A <code>escapeHtml</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
/**
 * @module validation/validation-result-renderer
 *
 * A XSD/XPath validációs működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A validációs állapotot és a hibák mezőhöz navigálását a szerveroldali eredményekkel összhangban kezeli.
 */

export function escapeHtml(value){
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

/**
 * A <code>escapeAttr</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
export function escapeAttr(value){
  return escapeHtml(value).replaceAll('`', '&#96;');
}

/**
 * Feldolgozza a normalize validation severity label bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
export function normalizeValidationSeverityLabel(value){
  const raw = String(value ?? '').trim();
  if(!raw || raw === '-') return { label: '-', css: 'unknown', code: '' };
  const upper = raw.toUpperCase();
  const map = {
    '1': { label: 'FIGYELMEZTETÉS', css: 'warning', code: '1' },
    'WARNING': { label: 'FIGYELMEZTETÉS', css: 'warning', code: '1' },
    'WARN': { label: 'FIGYELMEZTETÉS', css: 'warning', code: '1' },
    '2': { label: 'HIBA', css: 'error', code: '2' },
    'ERROR': { label: 'HIBA', css: 'error', code: '2' },
    'HIBA': { label: 'HIBA', css: 'error', code: '2' },
    '3': { label: 'KRITIKUS', css: 'critical', code: '3' },
    'CRITICAL': { label: 'KRITIKUS', css: 'critical', code: '3' },
    'FATAL': { label: 'KRITIKUS', css: 'critical', code: '3' },
    'INFO': { label: 'INFORMÁCIÓ', css: 'info', code: '' },
    'INFORMATION': { label: 'INFORMÁCIÓ', css: 'info', code: '' },
    'FIGYELMEZTETÉS': { label: 'FIGYELMEZTETÉS', css: 'warning', code: '1' },
    'FIGYELMEZTETES': { label: 'FIGYELMEZTETÉS', css: 'warning', code: '1' },
    'INFORMÁCIÓ': { label: 'INFORMÁCIÓ', css: 'info', code: '' },
    'INFORMACIO': { label: 'INFORMÁCIÓ', css: 'info', code: '' },
    'KRITIKUS': { label: 'KRITIKUS', css: 'critical', code: '3' }
  };
  return map[upper] || { label: upper, css: 'unknown', code: raw };
}

/**
 * Megjeleníti vagy újrarendereli a render validation severity cell állapotát a felhasználói felületen.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
export function renderValidationSeverityCell(value){
  const severity = normalizeValidationSeverityLabel(value);
  if(severity.label === '-') return '-';
  const title = severity.code ? `Eredeti szint: ${severity.code}` : severity.label;
  return `<span class="validation-severity-badge validation-severity-${severity.css}" title="${escapeAttr(title)}">${escapeHtml(severity.label)}</span>`;
}

/**
 * Feldolgozza a normalize popup message bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
 * @returns {*} a feldolgozás eredménye
 */
export function normalizePopupMessage(message){
  return String(message || '-').replace(/\r\n/g, '\n').replace(/\r/g, '\n');
}

/**
 * Ellenőrzi a is popup message long feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
 * @param {*} maxLines a függvény maxLines bemeneti értéke
 * @param {*} maxCharsPerLine a függvény maxCharsPerLine bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function isPopupMessageLong(message, maxLines = 4, maxCharsPerLine = 100){
  const normalized = normalizePopupMessage(message);
  const lines = normalized.split('\n');
  if(lines.length > maxLines) return true;
  if(lines.some(line => line.length > maxCharsPerLine)) return true;
  return normalized.length > maxLines * maxCharsPerLine;
}

/**
 * Megjeleníti vagy újrarendereli a render message cell állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
 * @param {*} index az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
export function renderMessageCell(message, index, options = {}){
  const normalized = normalizePopupMessage(message);
  const togglePrefix = options.togglePrefix || 'xpath-message';
  const needsToggle = isPopupMessageLong(normalized, options.maxLines || 4, options.maxCharsPerLine || 100);
  if(!needsToggle){
    return `<div class="xpath-message-cell"><div class="xpath-message-preview">${escapeHtml(normalized)}</div></div>`;
  }
  return `<div class="xpath-message-cell is-collapsed">
    <div class="xpath-message-preview">${escapeHtml(normalized)}</div>
    <div class="xpath-message-full hidden">${escapeHtml(normalized)}</div>
    <button type="button" class="xpath-message-toggle" data-${togglePrefix}-toggle-index="${escapeAttr(index)}">Kinyitás</button>
  </div>`;
}

/**
 * A <code>toggleMessageCell</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} button a függvény button bemeneti értéke
 */
export function toggleMessageCell(button){
  const cell = button.closest('.xpath-message-cell');
  if(!cell) return;
  const preview = cell.querySelector('.xpath-message-preview');
  const full = cell.querySelector('.xpath-message-full');
  const collapsed = cell.classList.toggle('is-collapsed');
  if(preview) preview.classList.toggle('hidden', !collapsed);
  if(full) full.classList.toggle('hidden', collapsed);
  button.textContent = collapsed ? 'Kinyitás' : 'Bezárás';
}

/**
 * Megjeleníti vagy újrarendereli a render xpath path cell állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @param {*} index az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
 * @returns {*} a feldolgozás eredménye
 */
export function renderXpathPathCell(path, index){
  const normalized = String(path || '').trim();
  if(!normalized || normalized === '-') return '-';
  return `<div class="xpath-path-cell" data-xpath-path-expanded="false">
    <div class="xpath-path-actions">
      <button type="button" class="xpath-path-jump" data-xpath-path-jump-index="${escapeAttr(index)}">Ugrás a mezőre</button>
      <button type="button" class="xpath-path-toggle" data-xpath-path-toggle-index="${escapeAttr(index)}">XPath megtekintése</button>
      <button type="button" class="xpath-path-copy" data-xpath-path-copy-index="${escapeAttr(index)}" title="XPath másolása vágólapra" aria-label="XPath másolása vágólapra">Másolás</button>
    </div>
    <pre class="xpath-path-full hidden" hidden>${escapeHtml(normalized)}</pre>
  </div>`;
}

/**
 * A <code>toggleXpathPathCell</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} button a függvény button bemeneti értéke
 */
export function toggleXpathPathCell(button){
  const cell = button.closest('.xpath-path-cell');
  if(!cell) return;
  const full = cell.querySelector('.xpath-path-full');
  if(!full) return;
  const opening = full.hidden || full.classList.contains('hidden');
  full.hidden = !opening;
  full.classList.toggle('hidden', !opening);
  cell.dataset.xpathPathExpanded = opening ? 'true' : 'false';
  button.textContent = opening ? 'XPath bezárása' : 'XPath megtekintése';
}

/**
 * A <code>copyTextToClipboard</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} text a függvény text bemeneti értéke
 * @param {*} button a függvény button bemeneti értéke
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
export async function copyTextToClipboard(text, button){
  if(!text) return;
  try{
    await navigator.clipboard.writeText(text);
    if(button){
      button.classList.add('field-copy-ok');
      const oldTitle = button.title;
      button.title = 'XPath vágólapra másolva';
      setTimeout(() => {
        button.classList.remove('field-copy-ok');
        button.title = oldTitle;
      }, 1200);
    }
    window.NavFormRuntime?.showXPathCopySuccess?.();
  }catch(error){
    console.warn('Vágólapra másolás sikertelen', error);
  }
}

/**
 * Megjeleníti vagy újrarendereli a render xsd info cell állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} info a függvény info bemeneti értéke
 * @param {*} index az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
 * @returns {*} a feldolgozás eredménye
 */
export function renderXsdInfoCell(info, index){
  const normalized = String(info || '').trim();
  if(!normalized || normalized === '-') return '-';
  const needsToggle = isXsdInfoLong(normalized);
  const toggle = needsToggle
    ? `<button type="button" class="xsd-info-toggle" data-xsd-info-toggle-index="${escapeAttr(index)}">Több</button>`
    : '';
  return `<div class="xsd-info-cell${needsToggle ? ' is-collapsed' : ''}">
    <div class="xsd-info-preview">${escapeHtml(normalized)}</div>
    <div class="xsd-info-full hidden">${escapeHtml(normalized)}</div>
    ${toggle}
  </div>`;
}

/**
 * Ellenőrzi a is xsd info long feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} text a függvény text bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function isXsdInfoLong(text){
  const value = String(text || '');
  const lines = value.split(/\r\n|\r|\n/);
  if(lines.length > 2) return true;
  return lines.some(line => line.length > 90);
}

/**
 * A <code>toggleXsdInfoCell</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} button a függvény button bemeneti értéke
 */
export function toggleXsdInfoCell(button){
  const cell = button.closest('.xsd-info-cell');
  if(!cell) return;
  const full = cell.querySelector('.xsd-info-full');
  if(!full) return;
  const opening = full.classList.contains('hidden');
  full.classList.toggle('hidden', !opening);
  cell.classList.toggle('is-collapsed', !opening);
  button.textContent = opening ? 'Kevesebb' : 'Több';
}
