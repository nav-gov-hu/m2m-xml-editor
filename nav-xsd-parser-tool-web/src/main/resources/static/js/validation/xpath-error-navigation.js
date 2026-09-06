/**
 * @module validation/xpath-error-navigation
 *
 * A XSD/XPath validációs működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

const FIELD_SELECTOR = '.form-field[data-field-id], .uimodel-field[data-field-id], input[data-field-id], select[data-field-id], textarea[data-field-id]';
const FIELD_PATH_SELECTOR = '.form-field[data-xml-path], .uimodel-field[data-xml-path], input[data-xml-path], select[data-xml-path], textarea[data-xml-path]';

/**
 * A <code>stripClarkNotationNamespaces</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
function stripClarkNotationNamespaces(path){
  return String(path || '').replace(/Q\{[^}]*\}/g, '');
}

/**
 * Feldolgozza a normalize field candidate bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} element a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @returns {*} a feldolgozás eredménye
 */
function normalizeFieldCandidate(element){
  if(!element) return null;
  return element.closest?.('.form-field, .uimodel-field') || element;
}

/**
 * A <code>uniqueFieldCandidates</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} root a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} selector a függvény selector bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function uniqueFieldCandidates(root, selector){
  return [...new Set([...root.querySelectorAll(selector)].map(normalizeFieldCandidate).filter(Boolean))];
}

/**
 * Feldolgozza a normalize nav xpath bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
export function normalizeNavXpath(path){
  return canonicalizeXmlPath(path);
}

/**
 * Ellenőrzi a canonicalize xml path feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
export function canonicalizeXmlPath(path){
  if(!path) return '';
  const namespaceFreePath = stripClarkNotationNamespaces(path);
  const parts = namespaceFreePath.split('/').filter(Boolean).map(segment => {
    const match = segment.match(/^(.+?)(?:\[(\d+)\])?$/);
    if(!match) return normalizeXmlPathSegmentName(segment);
    const name = normalizeXmlPathSegmentName(match[1]);
    const index = match[2] || '1';
    return `${name}[${index}]`;
  });
  return `/${parts.join('/')}`;
}

/**
 * Feldolgozza a normalize xml path segment name bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} name a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
export function normalizeXmlPathSegmentName(name){
  return String(name || '')
    .replace(/^Q\{[^}]*\}/, '')
    .replace(/^.*:/, '');
}

/**
 * Ellenőrzi a canonical path parts feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
export function canonicalPathParts(path){
  return canonicalizeXmlPath(path).split('/').filter(Boolean).map(segment => {
    const match = segment.match(/^(.+?)(?:\[(\d+)\])?$/);
    return {
      name: match ? match[1] : segment,
      index: match?.[2] ? Number(match[2]) : 1,
      raw: segment
    };
  });
}

/**
 * A <code>pathMatches</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} left a függvény left bemeneti értéke
 * @param {*} right a függvény right bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function pathMatches(left, right){
  if(!left || !right) return false;
  return canonicalizeXmlPath(left) === canonicalizeXmlPath(right);
}

/**
 * A <code>splitCanonicalPath</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
export function splitCanonicalPath(path){
  return canonicalizeXmlPath(path).split('/').filter(Boolean);
}

/**
 * A <code>pathSuffixScore</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} candidatePath a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @param {*} targetPath a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
export function pathSuffixScore(candidatePath, targetPath){
  const candidate = splitCanonicalPath(candidatePath);
  const target = splitCanonicalPath(targetPath);
  if(!candidate.length || !target.length) return 0;
  let score = 0;
  while(score < candidate.length && score < target.length && candidate[candidate.length - 1 - score] === target[target.length - 1 - score]){
    score += 1;
  }
  return score;
}

/**
 * Feloldja a find best field by xml path suffix eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} fieldsWithPath a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @param {*} canonicalTarget a függvény canonicalTarget bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function findBestFieldByXmlPathSuffix(fieldsWithPath, canonicalTarget){
  const targetLast = splitCanonicalPath(canonicalTarget).pop() || '';
  let best = null;
  let bestScore = 0;
  fieldsWithPath.forEach(el => {
    const own = el.dataset.xmlPath || el.querySelector?.('[data-xml-path]')?.dataset?.xmlPath || '';
    const ownLast = splitCanonicalPath(own).pop() || '';
    if(!ownLast || ownLast !== targetLast) return;
    const score = pathSuffixScore(own, canonicalTarget);
    if(score > bestScore){
      best = el;
      bestScore = score;
    }
  });
  return bestScore >= 3 ? best : null;
}

/**
 * Feldolgozza a normalize maybe field id bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
export function normalizeMaybeFieldId(value){
  if(!value) return '';
  const text = String(value).trim();
  if(!text) return '';
  if(/^field[_-]?form$/i.test(text) || /^form[_-]?field$/i.test(text)) return '';
  return text;
}

/**
 * Ellenőrzi a is useful field id feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
export function isUsefulFieldId(value){
  if(!value) return false;
  return !/^field[_-]?form$/i.test(value) && !/^form[_-]?field$/i.test(value);
}

/**
 * Feldolgozza a extract field id from text bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} text a függvény text bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function extractFieldIdFromText(text){
  if(!text) return '';
  const matches = String(text).match(/Field_[A-Za-z0-9]+/g);
  return matches?.length ? matches[matches.length - 1] : '';
}

/**
 * Feldolgozza a extract field id from xpath path bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
export function extractFieldIdFromXpathPath(path){
  if(!path) return '';
  const matches = String(path).match(/Field_[A-Za-z0-9]+/g);
  return matches?.length ? matches[matches.length - 1] : '';
}

/**
 * Feloldja a resolve display element id for xpath error eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} error a függvény error bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function resolveDisplayElementIdForXpathError(error){
  const elementId = normalizeMaybeFieldId(error?.elementId || error?.elem || error?.fieldId || '');
  const pathFieldId = normalizeMaybeFieldId(extractFieldIdFromXpathPath(error?.path || ''));
  const messageFieldId = normalizeMaybeFieldId(extractFieldIdFromText(error?.errorMessage || error?.hibaszoveg || ''));
  if(isUsefulFieldId(elementId)) return elementId;
  if(isUsefulFieldId(pathFieldId)) return pathFieldId;
  if(isUsefulFieldId(messageFieldId)) return messageFieldId;
  return '';
}

/**
 * Feldolgozza a normalize field id variants bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
export function normalizeFieldIdVariants(value){
  const normalized = String(value || '').trim();
  if(!normalized) return [];
  const withoutPrefix = normalized.startsWith('Field_') ? normalized.substring('Field_'.length) : normalized;
  const withPrefix = normalized.startsWith('Field_') ? normalized : `Field_${normalized}`;
  return [...new Set([normalized, withPrefix, withoutPrefix].filter(Boolean))];
}

/**
 * A <code>formFieldMatchesElementId</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} el a függvény el bemeneti értéke
 * @param {*} elementId a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @returns {*} a feldolgozás eredménye
 */
export function formFieldMatchesElementId(el, elementId){
  const own = String(el?.dataset?.fieldId || el?.querySelector?.('[data-field-id]')?.dataset?.fieldId || '').trim();
  if(!own || !elementId) return false;
  const variants = normalizeFieldIdVariants(elementId);
  return variants.some(candidate => own === candidate || own.endsWith(candidate.replace(/^Field_/i, '')));
}

/**
 * Feldolgozza a extract occurrence index from xpath path bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @param {*} nodeName a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @returns {*} a feldolgozás eredménye
 */
export function extractOccurrenceIndexFromXpathPath(path, nodeName){
  if(nodeName){
    return extractNamedOccurrenceFromXpathPath(path, nodeName);
  }
  const parts = canonicalPathParts(path);
  if(!parts.length) return 1;
  for(let i = parts.length - 2; i >= 0; i--){
    const name = String(parts[i]?.name || '');
    const index = Number(parts[i]?.index || 1);
    if(index > 0 && /^Chain_elem$/i.test(name)) return index;
  }
  for(let i = parts.length - 2; i >= 0; i--){
    const index = Number(parts[i]?.index || 1);
    if(index > 1) return index;
  }
  return 1;
}

/**
 * Feldolgozza a extract named occurrence from xpath path bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @param {*} segmentName a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
export function extractNamedOccurrenceFromXpathPath(path, segmentName){
  const parts = canonicalPathParts(path);
  const wanted = String(segmentName || '').toLowerCase();
  for(let i = parts.length - 1; i >= 0; i--){
    if(String(parts[i]?.name || '').toLowerCase() === wanted){
      return Number(parts[i]?.index || 1);
    }
  }
  return 1;
}

/**
 * Ellenőrzi a is element actually visible feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} el a függvény el bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function isElementActuallyVisible(el){
  if(!el || !el.isConnected) return false;
  const rect = el.getBoundingClientRect?.();
  return !!rect && rect.width > 0 && rect.height > 0;
}

/**
 * Betölti vagy lekéri a get field path diagnostics művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} el a függvény el bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function getFieldPathDiagnostics(el){
  const path = el?.dataset?.xmlPath || el?.querySelector?.('[data-xml-path]')?.dataset?.xmlPath || '';
  return {
    fieldId: el?.dataset?.fieldId || el?.querySelector?.('[data-field-id]')?.dataset?.fieldId || '',
    xmlPath: path || el?.querySelector?.('[data-xml-path]')?.dataset?.xmlPath || '',
    canonicalPath: canonicalizeXmlPath(path),
    chainElemIndex: extractNamedOccurrenceFromXpathPath(path, 'Chain_elem'),
    visible: isElementActuallyVisible(el),
    className: el?.className || ''
  };
}

/**
 * Feloldja a find form field by xml path eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @param {*} root a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
export function findFormFieldByXmlPath(path, root = document, options = {}){
  if(!path) return null;
  const canonicalTarget = canonicalizeXmlPath(path);
  const fieldsWithPath = uniqueFieldCandidates(root, FIELD_PATH_SELECTOR);

  const fieldIdFromPath = extractFieldIdFromXpathPath(path);
  const matchingFieldCandidates = fieldIdFromPath
    ? uniqueFieldCandidates(root, FIELD_SELECTOR).filter(el => formFieldMatchesElementId(el, fieldIdFromPath))
    : [];
  const hasExplicitChainIndex = /(?:^|\/)Q\{[^}]*\}Chain_elem\[\d+\]|(?:^|\/)Chain_elem\[\d+\]/i.test(String(path || ''));
  const ambiguousRepeatedField = !hasExplicitChainIndex && matchingFieldCandidates.length > 1;

  if(!ambiguousRepeatedField){
    const direct = fieldsWithPath.find(el => pathMatches(el.dataset.xmlPath || el.querySelector?.('[data-xml-path]')?.dataset?.xmlPath || '', canonicalTarget));
    if(direct) return direct;

    const suffixMatch = findBestFieldByXmlPathSuffix(fieldsWithPath, canonicalTarget);
    if(suffixMatch) return suffixMatch;
  }

  return findFormFieldByElementIdAndOccurrence(fieldIdFromPath, path, root, options);
}

/**
 * Feloldja a find form field by element id and occurrence eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} elementId a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @param {*} root a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
export function findFormFieldByElementIdAndOccurrence(elementId, path, root = document, options = {}){
  if(!elementId) return null;
  const candidates = uniqueFieldCandidates(root, FIELD_SELECTOR)
    .filter(el => formFieldMatchesElementId(el, elementId));
  if(!candidates.length) return null;

  const canonicalTarget = canonicalizeXmlPath(path || '');
  const exactPathCandidate = candidates.find(el => pathMatches(el.dataset.xmlPath || el.querySelector?.('[data-xml-path]')?.dataset?.xmlPath || '', canonicalTarget));
  if(exactPathCandidate) return exactPathCandidate;

  const suffixCandidate = findBestFieldByXmlPathSuffix(candidates, canonicalTarget);
  if(suffixCandidate) return suffixCandidate;

  const occurrenceIndex = extractOccurrenceIndexFromXpathPath(path);
  const explicitChainIndex = extractNamedOccurrenceFromXpathPath(path, 'Chain_elem');
  const pathContainsExplicitChainIndex = /(?:^|\/)Q\{[^}]*\}Chain_elem\[\d+\]|(?:^|\/)Chain_elem\[\d+\]/i.test(String(path || ''));
  if(pathContainsExplicitChainIndex){
    const sameChainCandidates = candidates.filter(el => {
      const ownPath = el.dataset.xmlPath || el.querySelector?.('[data-xml-path]')?.dataset?.xmlPath || '';
      const ownIndex = extractNamedOccurrenceFromXpathPath(ownPath, 'Chain_elem');
      return ownIndex === explicitChainIndex;
    });
    if(sameChainCandidates.length === 1) return sameChainCandidates[0];
    if(sameChainCandidates.length > 1){
      const visible = sameChainCandidates.find(isElementActuallyVisible);
      return visible || sameChainCandidates[0];
    }
  }

  const duplicateOrdinal = Number(options?.duplicateOrdinal || 0);
  const visibleCandidates = candidates.filter(isElementActuallyVisible);
  const indexedCandidates = visibleCandidates.length ? visibleCandidates : candidates;
  if(duplicateOrdinal > 0){
    return indexedCandidates[Math.min(duplicateOrdinal - 1, indexedCandidates.length - 1)] || indexedCandidates[0] || null;
  }
  return indexedCandidates[Math.max(occurrenceIndex - 1, 0)] || indexedCandidates[0] || null;
}

/**
 * Feloldja a find form field by element id eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} elementId a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} root a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @returns {*} a feldolgozás eredménye
 */
export function findFormFieldByElementId(elementId, root = document){
  if(!elementId) return null;
  const candidates = uniqueFieldCandidates(root, FIELD_SELECTOR)
    .filter(el => formFieldMatchesElementId(el, elementId));
  const visible = candidates.find(isElementActuallyVisible);
  return visible || candidates[0] || null;
}

/**
 * Feldolgozza a build xpath jump diagnostics bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} error a függvény error bemeneti értéke
 * @param {*} target a függvény target bemeneti értéke
 * @param {*} resolutionStep a függvény resolutionStep bemeneti értéke
 * @param {*} root a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @returns {*} a feldolgozás eredménye
 */
export function buildXpathJumpDiagnostics(error, target, resolutionStep, root = document){
  const path = error?.path || '';
  const elementId = resolveDisplayElementIdForXpathError(error);
  const fieldIdFromPath = extractFieldIdFromXpathPath(path);
  const lookupFieldId = fieldIdFromPath || elementId;
  const candidates = lookupFieldId
    ? uniqueFieldCandidates(root, FIELD_SELECTOR)
        .filter(el => formFieldMatchesElementId(el, lookupFieldId))
        .map(getFieldPathDiagnostics)
    : [];
  return {
    resolutionStep,
    errorCode: error?.errorCode || error?.code || '',
    ruleId: error?.ruleId || '',
    elementId,
    fieldIdFromPath,
    requestedPath: path,
    canonicalRequestedPath: canonicalizeXmlPath(path),
    requestedChainElemIndex: extractNamedOccurrenceFromXpathPath(path, 'Chain_elem'),
    requestedOccurrenceIndex: extractOccurrenceIndexFromXpathPath(path),
    duplicateOrdinal: Number(error?.__navDuplicateOrdinal || 0),
    target: target ? getFieldPathDiagnostics(target) : null,
    candidateCount: candidates.length,
    candidates: candidates.slice(0, 40)
  };
}

/**
 * A <code>logXpathJumpDiagnostics</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} error a függvény error bemeneti értéke
 * @param {*} target a függvény target bemeneti értéke
 * @param {*} resolutionStep a függvény resolutionStep bemeneti értéke
 * @param {*} root a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @returns {*} a feldolgozás eredménye
 */
export function logXpathJumpDiagnostics(error, target, resolutionStep, root = document){
  const diagnostics = buildXpathJumpDiagnostics(error, target, resolutionStep, root);
  const logger = target ? console.info : console.warn;
  logger('NAV XPath mezougras diagnosztika', diagnostics);
  window.__NAV_LAST_XPATH_JUMP_DIAGNOSTICS__ = diagnostics;
  return diagnostics;
}

/**
 * Feloldja a resolve xpath error target eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} error a függvény error bemeneti értéke
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
export function resolveXpathErrorTarget(error, options = {}){
  const root = options.root || document;
  const path = error?.path || '';
  let resolutionStep = 'xml-path';
  let target = findFormFieldByXmlPath(path, root, { duplicateOrdinal: error?.__navDuplicateOrdinal });

  if(!target && path){
    resolutionStep = 'field-id-from-path-and-occurrence';
    target = findFormFieldByElementIdAndOccurrence(extractFieldIdFromXpathPath(path), path, root, { duplicateOrdinal: error?.__navDuplicateOrdinal });
  }
  if(!target){
    resolutionStep = 'field-id-fallback';
    target = findFormFieldByElementId(resolveDisplayElementIdForXpathError(error), root);
  }

  const diagnostics = logXpathJumpDiagnostics(error, target, resolutionStep, root);
  return {
    target,
    diagnostics,
    resolutionStep,
    elementId: resolveDisplayElementIdForXpathError(error),
    fieldIdFromPath: extractFieldIdFromXpathPath(path),
    path
  };
}


/**
 * A <code>installXpathErrorNavigationGlobal</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
export function installXpathErrorNavigationGlobal(){
  window.NavXpathErrorNavigation = {
    normalizeNavXpath,
    canonicalizeXmlPath,
    normalizeXmlPathSegmentName,
    canonicalPathParts,
    pathMatches,
    splitCanonicalPath,
    pathSuffixScore,
    findBestFieldByXmlPathSuffix,
    normalizeMaybeFieldId,
    isUsefulFieldId,
    extractFieldIdFromText,
    extractFieldIdFromXpathPath,
    resolveDisplayElementIdForXpathError,
    normalizeFieldIdVariants,
    formFieldMatchesElementId,
    extractOccurrenceIndexFromXpathPath,
    extractNamedOccurrenceFromXpathPath,
    isElementActuallyVisible,
    getFieldPathDiagnostics,
    findFormFieldByXmlPath,
    findFormFieldByElementIdAndOccurrence,
    findFormFieldByElementId,
    buildXpathJumpDiagnostics,
    logXpathJumpDiagnostics,
    resolveXpathErrorTarget
  };
}
