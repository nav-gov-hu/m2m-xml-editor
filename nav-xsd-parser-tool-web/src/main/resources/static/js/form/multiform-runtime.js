/**
 * @module form/multiform-runtime
 *
 * A űrlap-megjelenítési és mezőkötési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A modul megőrzi a főlap/melléklap szétválasztást és a teljes indexelt XML-útvonal alapú kötést.
 */

/**
 * Runtime multiform discovery, indexing, list/detail navigation and diagnostics.
 * Shared runtime state is initialized by runtime-context.js.
 */


function showMultiformWarning(message){
  const text = String(message || '').trim();
  if(!text) return;
  if(window.navShowToast){
    window.navShowToast(text, 'warning');
    return;
  }
  console.warn(text);
}

/**
 * Ellenőrzi a is ui model missing field feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} valueObj a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function isUiModelMissingField(valueObj){
  return !valueObj || valueObj.present !== true;
}

/**
 * A <code>xmlLocalName</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @returns {*} a feldolgozás eredménye
 */
function xmlLocalName(node){
  return node?.localName || node?.nodeName || '';
}

/**
 * A <code>directElementChildren</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @returns {*} a feldolgozás eredménye
 */
function directElementChildren(node){
  return Array.from(node?.children || []).filter(child => child.nodeType === 1);
}

/**
 * Feloldja a detect runtime multiform parts eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} xmlDoc a feldolgozandó XML-tartalom vagy XML DOM-objektum
 * @returns {*} a feldolgozás eredménye
 */
function detectRuntimeMultiformParts(xmlDoc){
  const root = xmlDoc?.documentElement;
  if(!root) return [];
  const groups = new Map();
  directElementChildren(root).forEach(child => {
    const name = xmlLocalName(child);
    if(!/^Form_/i.test(name)) return;
    if(!groups.has(name)) groups.set(name, []);
    groups.get(name).push(child);
  });
  if(groups.size < 2 && !Array.from(groups.values()).some(items => items.length > 1)) return [];
  const entries = Array.from(groups.entries());
  const hasRepeatedInstances = entries.some(([, elements]) => elements.length > 1);
  const largeRepeatingName = root.getAttribute('data-large-repeating-form-name') || '';
  const largeRepeatingCount = Number(root.getAttribute('data-large-repeating-form-count') || 0);
  return entries.map(([name, elements], index) => ({
    name,
    // Normál XML-ben az ismétlődő melléklapot a több példány jelzi. Nagy XML
    // részleges előnézetben azonban memória-védelemből csak az első melléklap
    // kerül átadásra. Ilyenkor a gyökér első Form_* gyermeke a főlap, az első
    // eltérő nevű további Form_* gyermek pedig a melléklap mintapéldánya.
    role: elements.length > 1 || name === largeRepeatingName || (!hasRepeatedInstances && entries.length > 1 && index > 0)
      ? 'REPEATING'
      : 'MAIN',
    count: name === largeRepeatingName && largeRepeatingCount > 0 ? largeRepeatingCount : elements.length,
    previewCount: elements.length,
    elements
  }));
}

/**
 * Feldolgozza a normalize runtime form name bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function normalizeRuntimeFormName(value){
  return String(value || '').replace(/^Doc_/i, '').replace(/^Form_/i, '').replace(/^NAV_/i, '').trim();
}

/**
 * Feldolgozza a normalize runtime structural path bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function normalizeRuntimeStructuralPath(value){
  const path = String(value || '').trim().replace(/\[\d+\]/g, '').replace(/\/+$/g, '');
  if(!path) return '';
  return path.startsWith('/') ? path : `/${path}`;
}

/**
 * Elindítja a runtime structural label map aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function runtimeStructuralLabelMap(){
  const labels = { ...(currentFormDefinition?.structuralLabelsByPath || {}) };
  if(!currentSchemaBundle?.uiModelFile) return labels;
  (currentFormDefinition?.tabs || []).forEach(tab => {
    (tab?.sections || []).forEach(section => {
      (section?.rows || []).forEach(row => {
        const rowPath = normalizeRuntimeStructuralPath(row?.xmlPath);
        if(!rowPath) return;
        if(row?.title) labels[rowPath] = row.title;
        const parentPath = rowPath.includes('/') ? rowPath.slice(0, rowPath.lastIndexOf('/')) : '';
        if(parentPath && section?.title) labels[parentPath] = section.title;
      });
    });
  });
  return labels;
}

/**
 * Elindítja a runtime structural label for path aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @param {*} fallback a függvény fallback bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function runtimeStructuralLabelForPath(path, fallback = ''){
  const normalized = normalizeRuntimeStructuralPath(path);
  if(!normalized) return fallback;
  const labels = runtimeStructuralLabelMap();
  const direct = labels[normalized];
  if(direct) return String(direct);
  const suffix = Object.entries(labels).find(([key]) => normalizeRuntimeStructuralPath(key).endsWith(normalized));
  return suffix?.[1] ? String(suffix[1]) : fallback;
}

/**
 * Elindítja a runtime part path aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
function runtimePartPath(partName){
  const rootName = currentXmlDocument?.documentElement ? xmlLocalName(currentXmlDocument.documentElement) : '';
  return `/${[rootName, partName].filter(Boolean).join('/')}`;
}

/**
 * Elindítja a runtime form part label aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
function runtimeFormPartLabel(partName){
  return runtimeStructuralLabelForPath(runtimePartPath(partName), partName);
}

/**
 * Elindítja a runtime structural group label aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 * @param {*} relativePath a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
function runtimeStructuralGroupLabel(partName, relativePath){
  const segments = String(relativePath || '').split('/').map(value => value.trim()).filter(Boolean);
  const rootName = currentXmlDocument?.documentElement ? xmlLocalName(currentXmlDocument.documentElement) : '';
  const prefix = [rootName, partName].filter(Boolean);
  const labels = segments.map((segment, index) => {
    const fullPath = `/${[...prefix, ...segments.slice(0, index + 1)].join('/')}`;
    return runtimeStructuralLabelForPath(fullPath, segment.replace(/\[\d+\]$/, ''));
  });
  return labels.filter((label, index) => index === 0 || label !== labels[index - 1]).join(' / ');
}

/**
 * Elindítja a runtime detail group descriptor aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 * @param {*} field a függvény field bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function runtimeDetailGroupDescriptor(partName, field){
  const path = String(field?.path || '');
  const segments = path.split('/').filter(Boolean);
  const chainElementIndex = segments.findIndex(segment => /^Chain_elem(?:\[\d+\])?$/i.test(segment));
  if(chainElementIndex > 0){
    const chainSegmentIndex = chainElementIndex - 1;
    const chainSegment = segments[chainSegmentIndex];
    const match = segments[chainElementIndex].match(/\[(\d+)\]$/);
    const occurrence = Number(match?.[1] || 1);
    const chainRelativePath = segments.slice(0, chainSegmentIndex + 1).join('/');
    const rootName = currentXmlDocument?.documentElement ? xmlLocalName(currentXmlDocument.documentElement) : '';
    const fullChainPath = `/${[rootName, partName, ...segments.slice(0, chainSegmentIndex + 1)].filter(Boolean).join('/')}`;
    const chainLabel = runtimeStructuralLabelForPath(fullChainPath, chainSegment.replace(/\[\d+\]$/, ''));
    return {
      key: `chain:${chainRelativePath}:${occurrence}`,
      label: `${chainLabel || 'Lánc elem'} / ${occurrence}. elem`,
      chain: true,
      occurrence
    };
  }
  const relativeGroupPath = path.includes('/') ? segments.slice(0, -1).join('/') : '';
  return {
    key: `group:${relativeGroupPath || 'root'}`,
    label: relativeGroupPath ? runtimeStructuralGroupLabel(partName, relativeGroupPath) : 'Adatok',
    chain: false,
    occurrence: 0
  };
}

/**
 * Elindítja a runtime field path aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} root a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} leaf a függvény leaf bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function runtimeFieldPath(root, leaf){
  const names = [];
  let node = leaf;
  while(node && node !== root && node.nodeType === 1){
    const name = xmlLocalName(node);
    const siblings = node.parentElement
      ? directElementChildren(node.parentElement).filter(item => xmlLocalName(item) === name)
      : [];
    const occurrence = siblings.length > 1 ? siblings.indexOf(node) + 1 : 0;
    names.unshift(occurrence > 0 ? `${name}[${occurrence}]` : name);
    node = node.parentElement;
  }
  return names.join('/');
}

/**
 * Feldolgozza a collect runtime leaf fields bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} element a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} limit az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
 * @returns {*} a feldolgozás eredménye
 */
function collectRuntimeLeafFields(element, limit = 250){
  const fields = [];
    /**
   * A <code>walk</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
   */
const walk = (node) => {
    if(fields.length >= limit) return;
    const children = directElementChildren(node);
    if(!children.length){
      const value = String(node.textContent || '').trim();
      const name = xmlLocalName(node);
      fields.push({
        name,
        label: name,
        path: runtimeFieldPath(element, node),
        value,
        node
      });
      return;
    }
    children.forEach(walk);
  };
  walk(element);
  return fields;
}

/**
 * Feldolgozza a collect runtime label lookup bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
function collectRuntimeLabelLookup(partName){
  const lookup = new Map();
  const plainCandidates = new Map();
  const normalizedPartName = normalizeRuntimeSchemaPath(partName);

    /**
   * A <code>putPath</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
   * @param {*} label a függvény label bemeneti értéke
   */
const putPath = (path, label) => {
    const normalizedPath = normalizeRuntimeSchemaPath(path);
    const normalizedLabel = String(label || '').trim();
    if(!normalizedPath || !normalizedLabel) return;
    if(/^Field_/i.test(normalizedLabel)) return;
    lookup.set(`path:${normalizedPath}`, normalizedLabel);
  };

    /**
   * A <code>putPlainCandidate</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} key a függvény key bemeneti értéke
   * @param {*} label a függvény label bemeneti értéke
   * @param {*} nodePath a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
   */
const putPlainCandidate = (key, label, nodePath) => {
    const normalizedKey = String(key || '').trim();
    const normalizedLabel = String(label || '').trim();
    if(!normalizedKey || !normalizedLabel) return;
    if(/^Field_/i.test(normalizedLabel) || normalizedLabel === normalizedKey) return;
    const normalizedNodePath = normalizeRuntimeSchemaPath(nodePath);
    if(normalizedPartName && normalizedNodePath && !normalizedNodePath.includes(normalizedPartName)) return;
    const canonicalKey = normalizedKey.replace(/^Field_/i, '');
    const values = plainCandidates.get(canonicalKey) || new Set();
    values.add(normalizedLabel);
    plainCandidates.set(canonicalKey, values);
  };

    /**
   * A <code>walk</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
   * @param {*} depth a függvény depth bemeneti értéke
   */
const walk = (node, depth = 0) => {
    if(!node || depth > 18) return;
    if(Array.isArray(node)){
      node.forEach(item => walk(item, depth + 1));
      return;
    }
    if(typeof node !== 'object') return;

    const label = node.uiLabel || node.xsdLabel || node.label || node.title || node.nameLabel;
    const nodePath = node.xmlPath || node.path || node.schemaPath || '';
    const normalizedNodePath = normalizeRuntimeSchemaPath(nodePath);
    const belongsToPart = !normalizedPartName || !normalizedNodePath || normalizedNodePath.includes(normalizedPartName);

    if(label && belongsToPart && normalizedNodePath){
      putPath(normalizedNodePath, label);
      if(normalizedPartName){
        const partPos = normalizedNodePath.indexOf(normalizedPartName);
        if(partPos >= 0){
          putPath(normalizedNodePath.substring(partPos), label);
          const relative = normalizedNodePath.substring(partPos + normalizedPartName.length).replace(/^\/+/, '');
          putPath(relative, label);
        }
      }
    }

    const keys = [node.id, node.fieldId, node.name, node.xmlName, node.technicalName, node.eazon, node.vid].filter(Boolean);
    keys.forEach(key => putPlainCandidate(key, label, nodePath));

    Object.values(node).forEach(value => {
      if(value && typeof value === 'object') walk(value, depth + 1);
    });
  };

  walk(currentFormDefinition);
  walk(currentSchemaBundle);

  // Rövid fieldId csak akkor használható fallbackként, ha az aktuális form-részen belül
  // pontosan egyetlen címkéhez tartozik. Így a HIPAKA és HIPAKM azonos mezőnevei
  // nem írhatják felül egymás címkéit.
  plainCandidates.forEach((labels, canonicalKey) => {
    if(labels.size !== 1) return;
    const [label] = labels;
    lookup.set(`id:${canonicalKey}`, label);
    lookup.set(`id:Field_${canonicalKey}`, label);
  });

  return lookup;
}

/**
 * Elindítja a runtime friendly field label aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @param {*} lookup a függvény lookup bemeneti értéke
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
function runtimeFriendlyFieldLabel(field, lookup, partName){
  const relativePath = normalizeRuntimeSchemaPath(field?.path);
  const qualifiedPath = normalizeRuntimeSchemaPath(`${partName || ''}/${relativePath}`);
  const pathKeys = [qualifiedPath, relativePath].filter(Boolean).map(path => `path:${path}`);
  for(const key of pathKeys){
    const label = lookup?.get(key);
    if(label) return label;
  }

  const canonicalId = String(field?.name || field?.path?.split('/').pop() || '').replace(/^Field_/i, '');
  const idKeys = [`id:${canonicalId}`, `id:Field_${canonicalId}`];
  for(const key of idKeys){
    const label = lookup?.get(key);
    if(label) return label;
  }
  return field?.label || field?.name || 'Mező';
}

/**
 * Elindítja a runtime field id from name aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} name a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
function runtimeFieldIdFromName(name){
  return String(name || '').replace(/^Field_/i, '');
}

/**
 * Elindítja a runtime occurrence xml path aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 * @param {*} occurrenceIndex az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
 * @param {*} relativePath a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
function runtimeOccurrenceXmlPath(partName, occurrenceIndex, relativePath){
  const rootName = currentXmlDocument?.documentElement ? xmlLocalName(currentXmlDocument.documentElement) : '';
  const rel = String(relativePath || '').replace(/^\/+/, '');
  const partSegment = `${partName}[${Number(occurrenceIndex) || 1}]`;
  return `/${[rootName, partSegment, rel].filter(Boolean).join('/')}`;
}

/**
 * Feldolgozza a normalize runtime schema path bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function normalizeRuntimeSchemaPath(value){
  return String(value || '')
    .replace(/\\/g, '/')
    .replace(/\[\d+\]/g, '')
    .replace(/^\/+|\/+$/g, '')
    .replace(/\/+/g, '/');
}

/**
 * Elindítja a runtime enum values from metadata aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @returns {*} a feldolgozás eredménye
 */
function runtimeEnumValuesFromMetadata(node){
  const candidates = [node?.enumValues, node?.enumerationValues, node?.allowedValues, node?.options];
  for(const candidate of candidates){
    if(!Array.isArray(candidate)) continue;
    const values = candidate
      .map(item => typeof item === 'object' ? (item?.value ?? item?.id ?? item?.code) : item)
      .filter(value => value !== null && value !== undefined)
      .map(value => String(value));
    if(values.length) return values;
  }
  return [];
}

/**
 * Feloldja a find runtime field metadata eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
function findRuntimeFieldMetadata(field, partName){
  const targetName = String(field?.name || '').trim();
  const targetId = runtimeFieldIdFromName(targetName);
  const relativePath = normalizeRuntimeSchemaPath(field?.path);
  const targetPath = normalizeRuntimeSchemaPath(`${partName || ''}/${relativePath}`);
  const debugThisField = NAV_FIELD_BINDING_DEBUG_ENABLED && isDebugFieldId(targetId, NAV_FIELD_BINDING_DEBUG_TARGET);
  const debugCandidates = [];
  let best = null;
  let bestScore = -1;
  const visited = new Set();

    /**
   * A <code>walk</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
   * @param {*} depth a függvény depth bemeneti értéke
   */
const walk = (node, depth = 0) => {
    if(!node || depth > 18) return;
    if(Array.isArray(node)){
      node.forEach(item => walk(item, depth + 1));
      return;
    }
    if(typeof node !== 'object' || visited.has(node)) return;
    visited.add(node);

    const nodeName = String(node.xmlName || node.name || node.fieldName || '').trim();
    const nodeId = runtimeFieldIdFromName(node.id || node.fieldId || nodeName);
    const nodePath = normalizeRuntimeSchemaPath(node.xmlPath || node.path || node.schemaPath || '');
    const enumValues = runtimeEnumValuesFromMetadata(node);
    let score = 0;
    if(targetName && nodeName === targetName) score += 40;
    if(targetId && nodeId === targetId) score += 30;
    if(targetPath && nodePath && (nodePath.endsWith(targetPath) || targetPath.endsWith(nodePath))) score += 120;
    else {
      if(partName && nodePath.includes(normalizeRuntimeSchemaPath(partName))) score += 45;
      if(relativePath && nodePath.endsWith(relativePath)) score += 55;
    }
    if(enumValues.length) score += 15;
    const nameMatches = nodeName === targetName || nodeId === targetId;
    if(debugThisField && nameMatches){
      debugCandidates.push({
        score,
        nodeName,
        nodeId,
        nodePath,
        partMatch: !!(partName && nodePath.includes(normalizeRuntimeSchemaPath(partName))),
        relativePathMatch: !!(relativePath && nodePath.endsWith(relativePath)),
        targetPathMatch: !!(targetPath && nodePath && (nodePath.endsWith(targetPath) || targetPath.endsWith(nodePath))),
        type: node.type || node.dataType || '',
        enumCount: enumValues.length,
        enumValues: enumValues.slice(0, 10),
        label: node.uiLabel || node.xsdLabel || node.label || ''
      });
    }
    if(score > bestScore && nameMatches){
      bestScore = score;
      best = { ...node, enumValues };
    }
    Object.values(node).forEach(value => {
      if(value && typeof value === 'object') walk(value, depth + 1);
    });
  };

  walk(currentFormDefinition);
  walk(currentSchemaBundle);
  if(debugThisField){
    console.group(`NAV ENUM_METADATA_RESOLUTION ${targetId}`);
    console.info('Keresett runtime mező', { targetName, targetId, partName, relativePath, targetPath });
    console.info('Metaadat jelöltek pontszám szerint');
    console.table(debugCandidates.sort((a, b) => b.score - a.score));
    console.info('Kiválasztott metaadat', {
      bestScore,
      xmlName: best?.xmlName || best?.name || best?.fieldName || '',
      id: best?.id || best?.fieldId || '',
      xmlPath: best?.xmlPath || best?.path || best?.schemaPath || '',
      type: best?.type || best?.dataType || '',
      enumValues: runtimeEnumValuesFromMetadata(best),
      raw: best
    });
    console.groupEnd();
  }
  return best;
}

/**
 * Előkészíti és elindítja a create runtime ui model field element állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @param {*} row a függvény row bemeneti értéke
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
function createRuntimeUiModelFieldElement(field, row, partName){
  const fieldId = runtimeFieldIdFromName(field?.name);
  const xmlPath = runtimeOccurrenceXmlPath(partName, row?.index, field?.path);
  const metadata = findRuntimeFieldMetadata(field, partName);
  const enumValues = runtimeEnumValuesFromMetadata(metadata);
  const resolvedType = enumValues.length
    ? 'select'
    : String(metadata?.type || metadata?.dataType || 'text').toLowerCase();
  const fieldModel = {
    id: fieldId,
    xmlName: field?.name,
    xmlPath,
    uiLabel: field?.label || metadata?.uiLabel || metadata?.xsdLabel || metadata?.label || field?.name || 'Mező',
    label: field?.label || metadata?.uiLabel || metadata?.xsdLabel || metadata?.label || field?.name || 'Mező',
    type: resolvedType,
    enumValues,
    layoutWidth: Number(metadata?.layoutWidth) || 4,
    maxLength: metadata?.maxLength ?? null,
    mask: metadata?.mask || '',
    readonly: !!currentXmlFileReadOnlyMode || metadata?.readonly === true
  };
  const valueObj = {
    key: fieldId,
    value: field?.value ?? '',
    xmlPath,
    present: true
  };
  const element = renderUiModelFieldElement(fieldModel, valueObj);
  if(!element) return null;
  element.classList.add('multiform-uimodel-field');
  element.dataset.formPart = String(partName || '');
  element.dataset.occurrenceIndex = String(row?.index || '');
  element.dataset.runtimeLeafIndex = String(row?.leaves?.indexOf(field) ?? '');
  const control = element.querySelector('input, select, textarea');
  if(control){
    control.dataset.runtimeLeafIndex = String(row?.leaves?.indexOf(field) ?? '');
    control.dataset.formPart = String(partName || '');
    control.dataset.occurrenceIndex = String(row?.index || '');
  }
  return element;
}

/**
 * A <code>ensureRuntimeRowSummary</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} row a függvény row bemeneti értéke
 * @param {*} displayFields a függvény displayFields bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function ensureRuntimeRowSummary(row, displayFields){
  if(!row) return row;
  if(row.values && typeof row.searchText === 'string') return row;
  const displayNames = new Set((displayFields || []).map(field => field.name));
  const values = {};
  const searchParts = [];
  collectRuntimeLeafFields(row.element).forEach(field => {
    const text = String(field.value || '').trim();
    if(text) searchParts.push(text);
    if(displayNames.has(field.name) && values[field.name] === undefined && text){
      values[field.name] = text;
    }
  });
  row.values = values;
  row.searchText = searchParts.join('').toLocaleLowerCase('hu-HU');
  return row;
}

/**
 * Feldolgozza a build runtime option label bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} row a függvény row bemeneti értéke
 * @param {*} part a függvény part bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function buildRuntimeOptionLabel(row, part){
  if(row?.serverLabel) return row.serverLabel;
  ensureRuntimeRowSummary(row, part?.displayFields);
  const values = (part?.displayFields || [])
    .map(field => String(row.values?.[field.name] || '').trim())
    .filter(Boolean);
  return `${row.index} - ${values.length ? values.join(' ') : 'Melléklap'}`;
}

/**
 * A <code>ensureRuntimeRowLeaves</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} row a függvény row bemeneti értéke
 * @param {*} labelLookup a függvény labelLookup bemeneti értéke
 * @param {*} displayFields a függvény displayFields bemeneti értéke
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
function ensureRuntimeRowLeaves(row, labelLookup, displayFields, partName){
  if(!row) return [];
  ensureRuntimeRowSummary(row, displayFields);
  if(Array.isArray(row.leaves)) return row.leaves;
  row.leaves = collectRuntimeLeafFields(row.element);
  row.leaves.forEach(field => {
    field.label = runtimeFriendlyFieldLabel(field, labelLookup, partName);
  });
  return row.leaves;
}

/**
 * Elindítja a runtime row search text aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} row a függvény row bemeneti értéke
 * @param {*} part a függvény part bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function runtimeRowSearchText(row, part){
  ensureRuntimeRowSummary(row, part?.displayFields);
  return row?.searchText || '';
}

/**
 * Feldolgozza a build runtime part index bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
 * @param {*} part a függvény part bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function buildRuntimePartIndex(part){
  const labelLookup = collectRuntimeLabelLookup(part.name);
  const fieldNameSet = new Set();
  const fieldDefs = [];
  const sampleCount = Math.min(part.elements.length, 24);
  const sampleRows = [];

  for(let index = 0; index < sampleCount; index += 1){
    const leaves = collectRuntimeLeafFields(part.elements[index]);
    const values = {};
    leaves.forEach(field => {
      field.label = runtimeFriendlyFieldLabel(field, labelLookup, part.name);
      if(values[field.name] === undefined && field.value !== '') values[field.name] = field.value;
      if(!fieldNameSet.has(field.name)){
        fieldNameSet.add(field.name);
        fieldDefs.push({ name:field.name, label:field.label, path:field.path });
      }
    });
    sampleRows.push(values);
  }

  const preferredNames = fieldDefs
    .filter(field => sampleRows.some(values => String(values[field.name] || '').trim() !== ''))
    .slice(0, 5);
  const displayFields = preferredNames.length ? preferredNames : fieldDefs.slice(0, 5);
  const rows = part.elements.map((element, index) => ({
    index:index + 1,
    element,
    values:null,
    searchText:null,
    leaves:null
  }));

  return {
    name:part.name,
    label:runtimeFormPartLabel(part.name),
    role:part.role,
    count:part.count,
    rows,
    fieldDefs,
    displayFields,
    searchFields:fieldDefs,
    labelLookup
  };
}

/**
 * Betölti vagy lekéri a get runtime multiform label művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} part a függvény part bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function getRuntimeMultiformLabel(part){
  const label = part.label || runtimeFormPartLabel(part.name);
  if(part.role === 'REPEATING') return `Melléklapok - ${label} (${part.count})`;
  return `Főlap - ${label}`;
}

/**
 * A <code>formPartPathPrefix</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
function formPartPathPrefix(partName){
  const rootName = currentXmlDocument?.documentElement ? xmlLocalName(currentXmlDocument.documentElement) : '';
  const cleanPart = String(partName || '').trim();
  if(!cleanPart) return '';
  return `/${[rootName, cleanPart].filter(Boolean).join('/')}`;
}

/**
 * A <code>pathBelongsToFormPart</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} xmlPath a feldolgozandó XML-tartalom vagy XML DOM-objektum
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
function pathBelongsToFormPart(xmlPath, partName){
  const path = canonicalizeXmlPath(xmlPath || '');
  const prefix = canonicalizeXmlPath(formPartPathPrefix(partName));
  if(!prefix) return true;
  return path === prefix || path.startsWith(prefix + '/') || path.startsWith(prefix + '[');
}

/**
 * A <code>pruneRenderedPanelToFormPart</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} panel a függvény panel bemeneti értéke
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 */
function pruneRenderedPanelToFormPart(panel, partName){
  if(!panel || !partName) return;
  const seenPaths = new Set();
  panel.querySelectorAll('.form-field[data-xml-path], .uimodel-field[data-xml-path]').forEach(field => {
    const path = field.dataset.xmlPath || '';
    if(path && !pathBelongsToFormPart(path, partName)){
      field.remove();
      return;
    }
    const canonicalPath = canonicalizeXmlPath(path);
    if(canonicalPath && seenPaths.has(canonicalPath)){
      field.remove();
      return;
    }
    if(canonicalPath) seenPaths.add(canonicalPath);
  });
  panel.querySelectorAll('.uimodel-fieldgroup, .fieldgroup, .uimodel-section-card, .form-section, .collapsible-card').forEach(card => {
    const hasField = card.querySelector('.form-field[data-xml-path], .uimodel-field[data-xml-path], .uimodel-subtitle, .form-subtitle');
    const isShell = card.classList.contains('uimodel-form-shell') || card.classList.contains('multiform-runtime-shell');
    if(!hasField && !isShell){
      card.remove();
    }
  });
}

/**
 * Betölti vagy lekéri a get form unified toolbar művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function getFormUnifiedToolbar(){
  return document.getElementById('formUnifiedToolbar');
}

/**
 * A <code>ensureToolbarGroup</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} parent a függvény parent bemeneti értéke
 * @param {*} selector a függvény selector bemeneti értéke
 * @param {*} className a feloldáshoz vagy megjelenítéshez használt név
 * @param {*} ariaLabel a függvény ariaLabel bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function ensureToolbarGroup(parent, selector, className, ariaLabel){
  if(!parent) return null;
  let group = parent.querySelector(selector);
  if(!group){
    group = document.createElement('div');
    group.className = className;
    if(ariaLabel) group.setAttribute('aria-label', ariaLabel);
    parent.appendChild(group);
  }
  return group;
}

/**
 * A <code>restoreMultiformRuntimeToolbarControls</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function restoreMultiformRuntimeToolbarControls(){
  const paneHeader = document.querySelector('#formPanel .pane-header');
  const unifiedToolbar = getFormUnifiedToolbar();
  const fieldToolbar = document.querySelector('.field-search-toolbar');
  const paneActions = document.querySelector('.pane-header-actions');
  const xmlToolbar = document.querySelector('.xml-toolbar-buttons-inline');
  if(unifiedToolbar && fieldToolbar && fieldToolbar.closest('.multiform-runtime-toolbar')){
    const xmlGroup = ensureToolbarGroup(unifiedToolbar, '.form-unified-toolbar-section-xml', 'form-unified-toolbar-section form-unified-toolbar-section-xml', 'XML műveletek');
    if(xmlGroup) xmlGroup.appendChild(fieldToolbar);
  }
  if(unifiedToolbar){
    const formGroup = ensureToolbarGroup(unifiedToolbar, '.form-unified-toolbar-section-form', 'form-unified-toolbar-section form-unified-toolbar-section-form', 'Űrlap műveletek');
    const xmlGroup = ensureToolbarGroup(unifiedToolbar, '.form-unified-toolbar-section-xml', 'form-unified-toolbar-section form-unified-toolbar-section-xml', 'XML műveletek');
    if(paneActions && formGroup && !formGroup.contains(paneActions)){
      formGroup.appendChild(paneActions);
    }
    if(xmlToolbar && xmlGroup && !xmlGroup.contains(xmlToolbar)){
      xmlGroup.appendChild(xmlToolbar);
    }
  }
}

/**
 * A <code>ensureStandardFormToolbarControlsVisible</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function ensureStandardFormToolbarControlsVisible(){
  document.querySelectorAll('.multiform-selector-mount[data-runtime-owned="true"]').forEach(node => node.remove());
  const paneHeader = document.querySelector('#formPanel .pane-header');
  const unifiedToolbar = getFormUnifiedToolbar();
  if(!paneHeader && !unifiedToolbar) return;
  let actions = document.querySelector('.pane-header-actions');
  let xmlToolbar = document.querySelector('.xml-toolbar-buttons-inline');
  if(unifiedToolbar){
    unifiedToolbar.hidden = false;
    unifiedToolbar.style.removeProperty('display');
    unifiedToolbar.style.removeProperty('visibility');
    const formGroup = ensureToolbarGroup(unifiedToolbar, '.form-unified-toolbar-section-form', 'form-unified-toolbar-section form-unified-toolbar-section-form', 'Űrlap műveletek');
    const xmlGroup = ensureToolbarGroup(unifiedToolbar, '.form-unified-toolbar-section-xml', 'form-unified-toolbar-section form-unified-toolbar-section-xml', 'XML műveletek');
    if(actions && formGroup && !formGroup.contains(actions)){
      formGroup.appendChild(actions);
    }
    if(xmlToolbar && xmlGroup && !xmlGroup.contains(xmlToolbar)){
      xmlGroup.appendChild(xmlToolbar);
    }
  } else if(actions && paneHeader && !paneHeader.contains(actions)){
    paneHeader.appendChild(actions);
  }
  actions = document.querySelector('.pane-header-actions');
  xmlToolbar = document.querySelector('.xml-toolbar-buttons-inline');
  [actions, xmlToolbar].filter(Boolean).forEach(group => {
    group.hidden = false;
    group.style.removeProperty('display');
    group.style.removeProperty('visibility');
    group.querySelectorAll('button, input').forEach(control => {
      if(control.id === 'formXmlLoadInput' || control.id === 'm2mAttachmentInput') return;
      control.hidden = false;
      control.style.removeProperty('display');
      control.style.removeProperty('visibility');
    });
  });
  updateToggleAllFormCollapseButton();
  updateFormRendererSwitch();
}

/**
 * Előkészíti és elindítja a create multiform runtime toolbar állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function createMultiformRuntimeToolbar(){
  const toolbar = document.createElement('div');
  toolbar.className = 'multiform-runtime-toolbar';

  const fieldToolbar = document.querySelector('.field-search-toolbar');

  document.querySelectorAll('.multiform-selector-mount[data-runtime-owned="true"]').forEach(node => node.remove());
  const selectorMount = document.createElement('div');
  selectorMount.className = 'multiform-selector-mount';
  selectorMount.dataset.runtimeOwned = 'true';
  const unifiedToolbar = getFormUnifiedToolbar();
  const xmlGroup = unifiedToolbar
    ? ensureToolbarGroup(unifiedToolbar, '.form-unified-toolbar-section-xml', 'form-unified-toolbar-section form-unified-toolbar-section-xml', 'XML műveletek')
    : null;
  if(xmlGroup){
    if(fieldToolbar) xmlGroup.appendChild(fieldToolbar);
    xmlGroup.appendChild(selectorMount);
  } else {
    if(fieldToolbar) toolbar.appendChild(fieldToolbar);
    toolbar.appendChild(selectorMount);
  }

  // The form and XML action buttons live in the unified form toolbar under the page header.
  // Do not move them into the multiform runtime toolbar, otherwise normal-form navigation
  // can lose the shared controls when switching between form modes.
  return toolbar;
}

/**
 * A <code>enhanceMultiformRuntimeView</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
function enhanceMultiformRuntimeView(){
  if(!formContainer || !currentXmlDocument){
    currentMultiformState = null;
    ensureStandardFormToolbarControlsVisible();
    return;
  }
  const parts = detectRuntimeMultiformParts(currentXmlDocument);
  if(!parts.length){
    currentMultiformState = null;
    ensureStandardFormToolbarControlsVisible();
    return;
  }
  const mainPart = parts.find(part => part.role !== 'REPEATING') || parts[0];
  const repeatingPart = parts.find(part => part.role === 'REPEATING' || part.count > 1);
  if(!mainPart || !repeatingPart){
    currentMultiformState = null;
    ensureStandardFormToolbarControlsVisible();
    return;
  }

  const originalNodes = Array.from(formContainer.childNodes);
  const shell = document.createElement('div');
  shell.className = 'multiform-runtime-shell';

  const tabs = document.createElement('div');
  tabs.className = 'multiform-runtime-tabs';
  tabs.setAttribute('role', 'tablist');

  const mainButton = document.createElement('button');
  mainButton.type = 'button';
  mainButton.className = 'multiform-runtime-tab active';
  mainButton.textContent = getRuntimeMultiformLabel(mainPart);
  mainButton.dataset.partName = mainPart.name;

  const repeatingButton = document.createElement('button');
  repeatingButton.type = 'button';
  repeatingButton.className = 'multiform-runtime-tab';
  repeatingButton.textContent = getRuntimeMultiformLabel(repeatingPart);
  repeatingButton.dataset.partName = repeatingPart.name;

  tabs.appendChild(mainButton);
  tabs.appendChild(repeatingButton);

  const runtimeToolbar = createMultiformRuntimeToolbar();

  const mainPanel = document.createElement('div');
  mainPanel.className = 'multiform-runtime-panel active';
  mainPanel.dataset.partPanel = mainPart.name;
  originalNodes.forEach(node => mainPanel.appendChild(node));
  pruneRenderedPanelToFormPart(mainPanel, mainPart.name);
  syncRenderedFieldValuesFromXml(mainPanel);

  const repeatingPanel = document.createElement('div');
  repeatingPanel.className = 'multiform-runtime-panel';
  repeatingPanel.dataset.partPanel = repeatingPart.name;

  const indexedRepeatingPart = buildRuntimePartIndex(repeatingPart);
  currentMultiformState = {
    repeatingPart: indexedRepeatingPart,
    mainPartName: mainPart.name,
    activePartName: mainPart.name,
    query: '',
    searchField: indexedRepeatingPart.searchFields[0]?.name || '',
    matchMode: 'contains',
    selectedIndex: null,
    suggestOffset: 0,
    suggestLimit: 50,
    suggestionRows: [],
    dropdownOpen: false,
    hasMoreSuggestions: false,
    loadingSuggestions: false,
    activePanelKey: mainPart.name,
    openDetailRows: new Map(),
    dirtyPanelKey: null,
    tabsElement: tabs,
    shellElement: shell,
    selectorMount: runtimeToolbar.querySelector('.multiform-selector-mount'),
    // Minden nyilvántartott multiform XML a konfigurációvezérelt szerveroldali indexet használja.
    // Ez biztosítja, hogy a normál méretű és a nagy XML-ek táblázata azonos display/searchable
    // mezőkonfiguráció alapján épüljön fel.
    serverPaged: Boolean(activeXmlFileIdForLargeMultiform()),
    serverPage: 0,
    serverPageSize: 20,
    serverLoadedQuery: null,
    serverLoading: false
  };

  repeatingPanel.appendChild(renderRuntimeRepeatingPartPanel());
  shell.appendChild(tabs);
  shell.appendChild(runtimeToolbar);
  shell.appendChild(mainPanel);
  shell.appendChild(repeatingPanel);
  safeReplaceElementChildren(formContainer, shell);

    /**
   * A <code>activate</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} panelKey a függvény panelKey bemeneti értéke
   * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
   * @param {*} options a művelet opcionális beállításai
   * @returns {*} a feldolgozás eredménye
   */
const activate = (panelKey, partName = panelKey, options = {}) => {
    const dirtyPanelKey = currentMultiformState.dirtyPanelKey;
    if(options.validationNavigation !== true && dirtyPanelKey && dirtyPanelKey !== panelKey){
      const dirtyTab = shell.querySelector(`.multiform-runtime-tab[data-panel-key="${CSS.escape(dirtyPanelKey)}"]`);
      showMultiformWarning('Az aktuális lapon mentetlen módosítások vannak. Mentse el a lapot, mielőtt másik főlapot vagy melléklapot nyit meg.');
      dirtyTab?.focus();
      return false;
    }
    currentMultiformState.activePanelKey = panelKey;
    currentMultiformState.activePartName = partName;
    shell.querySelectorAll('.multiform-runtime-tab').forEach(button => button.classList.toggle('active', button.dataset.panelKey === panelKey));
    shell.querySelectorAll('.multiform-runtime-panel').forEach(panel => panel.classList.toggle('active', panel.dataset.partPanel === panelKey));
    if(currentMultiformState?.selectorMount){
      currentMultiformState.selectorMount.hidden = true;
    }
    const sharedSearch = document.getElementById('fieldSearchInput');
    if(sharedSearch){
      const indexMode = panelKey === currentMultiformState.repeatingPart?.name;
      sharedSearch.dataset.multiformIndexSearchActive = indexMode ? 'true' : 'false';
      sharedSearch.placeholder = indexMode ? 'Keresés a konfigurált indexmezőkben...' : 'Mezőkereső';
      if(indexMode) sharedSearch.value = currentMultiformState.query || '';
    }
    refreshFieldSearch({ showList:false });
    updateToggleAllFormCollapseButton();
    markXmlViewsDirty();
    ensureActiveXmlViewRendered('tree', { force:true });
    persistUiState();
    return true;
  };
  mainButton.dataset.panelKey = mainPart.name;
  mainButton.dataset.saveState = 'clean';
  repeatingButton.dataset.panelKey = repeatingPart.name;
  repeatingButton.dataset.saveState = 'clean';
  currentMultiformState.activatePanel = activate;
  mainButton.addEventListener('click', () => activate(mainPart.name, mainPart.name));
  repeatingButton.addEventListener('click', async () => {
    if(!activate(repeatingPart.name, repeatingPart.name)) return;
    if(currentMultiformState.serverPaged && !currentMultiformState.serverLoadedQuery){
      try{
        await loadRuntimeTablePage(0);
        const panel = shell.querySelector(`[data-part-panel="${repeatingPart.name}"] .multiform-repeating-selector-layout`);
        if(panel) refreshRuntimeRepeatingPanel(panel, { keepSuggestions:true });
      }catch(error){ window.alert(error.message); }
    }
  });


    /**
   * A <code>markPanelDirty</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
   * @param {*} panelKey a függvény panelKey bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
const markPanelDirty = panelKey => {
    if(!panelKey || currentXmlFileReadOnlyMode) return true;
    const dirtyPanelKey = currentMultiformState.dirtyPanelKey;
    if(dirtyPanelKey && dirtyPanelKey !== panelKey){
      window.alert('Egy másik lapon mentetlen módosítások vannak. Térjen vissza a pirossal jelölt tabra, és mentse el.');
      currentMultiformState.activatePanel(dirtyPanelKey, dirtyPanelKey.startsWith('detail:') ? repeatingPart.name : dirtyPanelKey);
      return false;
    }
    currentMultiformState.dirtyPanelKey = panelKey;
    const tab = shell.querySelector(`.multiform-runtime-tab[data-panel-key="${CSS.escape(panelKey)}"]`);
    if(tab){
      tab.dataset.dirty = 'true';
      tab.dataset.saveState = 'dirty';
      tab.classList.add('is-dirty');
      tab.classList.remove('is-saved');
      tab.setAttribute('aria-label', `${tab.textContent.trim()} - mentetlen módosítás`);
    }
    return true;
  };
  currentMultiformState.markPanelDirty = markPanelDirty;
  currentMultiformState.markActivePanelSaved = () => {
    const panelKey = currentMultiformState.dirtyPanelKey || currentMultiformState.activePanelKey;
    if(!panelKey) return;
    const tab = shell.querySelector(`.multiform-runtime-tab[data-panel-key="${CSS.escape(panelKey)}"]`);
    if(tab){
      tab.dataset.dirty = 'false';
      tab.dataset.saveState = 'saved';
      tab.classList.remove('is-dirty');
      tab.classList.add('is-saved');
      tab.setAttribute('aria-label', `${tab.textContent.trim()} - mentve`);
    }
    currentMultiformState.dirtyPanelKey = null;
  };
  shell.addEventListener('beforeinput', event => {
    const panel = event.target.closest?.('.multiform-runtime-panel');
    if(!panel || !currentMultiformState) return;
    if(!markPanelDirty(panel.dataset.partPanel)){
      event.preventDefault();
      event.stopPropagation();
    }
  }, true);
  shell.addEventListener('change', event => {
    const panel = event.target.closest?.('.multiform-runtime-panel');
    if(panel) markPanelDirty(panel.dataset.partPanel);
  }, true);

  // The XML tree may have been rendered before the runtime multiform state existed.
  // Re-render once so the initial main-form tab also shows only its own XML subtree.
  markXmlViewsDirty();
  ensureActiveXmlViewRendered('tree', { force:true });
}

/**
 * Elindítja a runtime matches aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @param {*} query a függvény query bemeneti értéke
 * @param {*} mode a függvény mode bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function runtimeMatches(value, query, mode){
  const left = String(value || '').toLocaleLowerCase('hu-HU');
  const right = String(query || '').toLocaleLowerCase('hu-HU');
  if(!right) return true;
  if(mode === 'exact') return left === right;
  if(mode === 'startsWith') return left.startsWith(right);
  return left.includes(right);
}

/**
 * A <code>activeXmlFileIdForLargeMultiform</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function activeXmlFileIdForLargeMultiform(){
  const params = new URLSearchParams(window.location.search || '');
  return params.get('xmlFileId') || null;
}

/**
 * Feldolgozza a parse server runtime row bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} dto a függvény dto bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function parseServerRuntimeRow(dto){
  const parsed = new DOMParser().parseFromString(String(dto?.xml || ''), 'application/xml');
  const element = parsed.documentElement;
  return {
    index:Number(dto?.index || 0),
    element,
    serverLabel:String(dto?.label || ''),
    values:(dto?.values && typeof dto.values === 'object') ? dto.values : {},
    searchText:String(dto?.label || '').toLocaleLowerCase('hu-HU'),
    leaves:null
  };
}

/**
 * Betölti vagy lekéri a load server runtime suggestions művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} reset a függvény reset bemeneti értéke
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function loadServerRuntimeSuggestions(reset = false){
  const state = currentMultiformState;
  if(!state?.serverPaged || state.serverLoading) return [];
  const xmlFileId = activeXmlFileIdForLargeMultiform();
  if(!xmlFileId) return [];
  const query = String(state.query || '').trim();
  if(query.length > 0 && query.length < 3) return [];
  if(reset){
    state.suggestionRows = [];
    state.repeatingPart.rows = [];
  }
  state.serverLoading = true;
  showLargeXmlProcessDialog(reset ? 'Melléklaplista betöltése' : 'További melléklapok betöltése',
    query ? `Keresés a melléklap-indexben: ${query}` : 'A melléklap-index ellenőrzése vagy első létrehozása folyamatban. Az első alkalom több időt vehet igénybe...');
  try{
    const statusParams = new URLSearchParams({ formName:state.repeatingPart.name });
    const statusResponse = await fetch(`/api/xml-files/${encodeURIComponent(xmlFileId)}/large-multiform/configuration-status?${statusParams}`, { credentials:'same-origin' });
    const status = await statusResponse.json().catch(() => ({}));
    if(statusResponse.ok && status.configurationRequired){
      hideLargeXmlProcessDialog();
      const formPartName = String(status.formPartName || state.repeatingPart.name || '');
      const missingParts = [];
      if(status.hasSearchableFields !== true) missingParts.push('legalább egy Kereshető mezőt');
      if(status.hasDisplayFields !== true) missingParts.push('legalább egy Lista mezőt');
      const missingText = missingParts.length ? ` Be kell állítani ${missingParts.join(' és ')}.` : '';
      const message = `A melléklapok megnyitásához előbb be kell állítani, hogy mely mezők alapján lehessen keresni, és mely mezők jelenjenek meg a találati listában.${missingText} A konfiguráció mentése után térjen vissza az Űrlapmegtekintőhöz.`;
      const confirmed = window.navConfirm
        ? await window.navConfirm({
            title:'Melléklap index konfiguráció szükséges',
            message,
            eyebrow:'XML index konfiguráció',
            confirmText:'Beállítás megnyitása',
            cancelText:'Mégsem',
            variant:'warning'
          })
        : false;
      if(!confirmed){
        showMultiformWarning('A melléklapok megnyitásához XML index konfiguráció szükséges.');
        return [];
      }
      const configParams = new URLSearchParams({
        formName:String(status.formName || ''),
        sourceVersion:String(status.sourceVersion || ''),
        formPartName,
        returnMode:new URLSearchParams(location.search).get('readOnly') === 'true' ? 'read' : 'edit',
        returnUrl:`${location.pathname}${location.search}`,
        reason:'multiform-index-required'
      });
      const navigationForm = document.createElement('form');
      navigationForm.action = '/xml-index-config.html';
      navigationForm.method = 'GET';
      navigationForm.hidden = true;
      for(const [name, value] of configParams.entries()){
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = name;
        input.value = value;
        navigationForm.appendChild(input);
      }
      document.body.appendChild(navigationForm);
      navigationForm.submit();
      return [];
    }
    const params = new URLSearchParams({
      formName:state.repeatingPart.name,
      page:String(state.serverPage),
      size:String(state.serverPageSize),
      q:query
    });
    const response = await fetch(`/api/xml-files/${encodeURIComponent(xmlFileId)}/large-multiform/rows?${params}`, { credentials:'same-origin' });
    const data = await response.json().catch(() => ({}));
    if(!response.ok) throw new Error(data.message || data.error || 'A melléklaplista betöltése sikertelen.');
    const added = (data.rows || []).map(parseServerRuntimeRow);
    state.repeatingPart.rows = reset ? added : state.repeatingPart.rows.concat(added);
    state.suggestionRows = state.repeatingPart.rows.slice();
    state.totalSuggestions = Number(data.total || state.repeatingPart.count || 0);
    state.tableColumns = Array.isArray(data.columns) ? data.columns : [];
    state.hasMoreSuggestions = data.hasMore === true;
    if(!reset) state.serverPage += 1;
    state.serverLoadedQuery = query;
    console.info('[Multiform index] Melléklaptáblázat betöltve', {
      xmlFileId,
      formPartName: state.repeatingPart.name,
      query,
      page: state.serverPage,
      pageSize: state.serverPageSize,
      total: state.totalSuggestions,
      returnedRows: added.length,
      columns: state.tableColumns.map(column => ({ name:column.name, label:column.label })),
      firstRowValues: added[0]?.values || {}
    });
    return added;
  }finally{
    state.serverLoading = false;
    hideLargeXmlProcessDialog();
  }
}

/**
 * Megjeleníti vagy újrarendereli a show large xml process dialog állapotát a felhasználói felületen.
 *
 * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
 * @param {*} title a függvény title bemeneti értéke
 * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
 */
function showLargeXmlProcessDialog(title, message){
  let overlay = document.getElementById('largeXmlProcessOverlay');
  if(!overlay){
    overlay = document.createElement('div');
    overlay.id = 'largeXmlProcessOverlay';
    overlay.className = 'large-xml-process-overlay';
    overlay.innerHTML = '<div class="large-xml-process-dialog" role="status" aria-live="polite"><div class="large-xml-process-spinner"></div><h2></h2><p></p></div>';
    document.body.appendChild(overlay);
  }
  overlay.querySelector('h2').textContent = title || 'Feldolgozás';
  overlay.querySelector('p').textContent = message || 'Kérjük, várjon...';
  overlay.hidden = false;
}

/**
 * Elrejti vagy lezárja a hide large xml process dialog felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
 *
 * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
 */
function hideLargeXmlProcessDialog(){
  const overlay = document.getElementById('largeXmlProcessOverlay');
  if(overlay) overlay.hidden = true;
}

/**
 * A <code>computeRuntimeSuggestions</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} reset a függvény reset bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function computeRuntimeSuggestions(reset = false){
  const state = currentMultiformState;
  if(!state?.repeatingPart) return [];
  if(reset){
    state.suggestOffset = 0;
    state.suggestionRows = [];
  }
  const query = String(state.query || '').trim();
  if(query.length > 0 && query.length < 3){
    state.hasMoreSuggestions = false;
    return [];
  }
  const normalizedQuery = query.toLocaleLowerCase('hu-HU');
  const rows = query
    ? state.repeatingPart.rows.filter(row => runtimeRowSearchText(row, state.repeatingPart).includes(normalizedQuery)
        || runtimeMatches(buildRuntimeOptionLabel(row, state.repeatingPart), query, 'contains'))
    : state.repeatingPart.rows;
  const nextRows = rows.slice(state.suggestOffset, state.suggestOffset + state.suggestLimit);
  state.suggestionRows = reset ? nextRows : state.suggestionRows.concat(nextRows);
  state.suggestOffset += nextRows.length;
  state.hasMoreSuggestions = state.suggestOffset < rows.length;
  state.totalSuggestions = rows.length;
  return nextRows;
}

/**
 * Szinkronizálja vagy frissíti a refresh runtime repeating panel által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} container a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} options a művelet opcionális beállításai
 */
function refreshRuntimeRepeatingPanel(container, options = {}){
  const replacement = renderRuntimeRepeatingPartPanel(options);
  container.replaceWith(replacement);
  persistUiState();
}

/**
 * Elindítja a runtime detail tab title aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} row a függvény row bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function runtimeDetailTabTitle(row){
  const raw = String(row?.serverLabel || buildRuntimeOptionLabel(row, currentMultiformState?.repeatingPart) || '').replace(/^\d+\s*-\s*/, '').trim();
  const compact = raw.length > 70 ? `${raw.slice(0, 67)}...` : raw;
  return `${row?.index || ''} - ${compact || 'Melléklap'}`;
}

/**
 * A <code>openRuntimeDetailTab</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} row a függvény row bemeneti értéke
 * @param {*} options a művelet opcionális beállításai
 */
function openRuntimeDetailTab(row, options = {}){
  const state = currentMultiformState;
  if(!state || !row) return;
  const key = `detail:${row.index}`;
  if(options.validationNavigation !== true && state.dirtyPanelKey && state.dirtyPanelKey !== key){
    showMultiformWarning('Az aktuális lapon mentetlen módosítások vannak. Mentésig másik melléklap nem nyitható meg.');
    state.activatePanel(state.dirtyPanelKey, state.dirtyPanelKey.startsWith('detail:') ? state.repeatingPart.name : state.dirtyPanelKey);
    return;
  }
  for(const [openedKey, openedEntry] of Array.from(state.openDetailRows.entries())){
    if(openedKey === key) continue;
    if(openedEntry?.tab?.dataset?.dirty === 'true'){
      if(options.validationNavigation === true) continue;
      showMultiformWarning('A megnyitott melléklapon mentetlen módosítások vannak. Mentse el a módosításokat.');
      return;
    }
    openedEntry?.tab?.remove();
    openedEntry?.panel?.remove();
    state.openDetailRows.delete(openedKey);
  }
  let opened = state.openDetailRows.get(key);
  if(!opened){
    const tab = document.createElement('button');
    tab.type = 'button';
    tab.className = 'multiform-runtime-tab multiform-runtime-detail-tab';
    tab.dataset.panelKey = key;
    tab.dataset.partName = state.repeatingPart.name;
    tab.title = runtimeDetailTabTitle(row);
    const titleSpan = document.createElement('span');
    titleSpan.textContent = runtimeDetailTabTitle(row);
    const closeButton = document.createElement('span');
    closeButton.className = 'multiform-runtime-tab-close';
    closeButton.textContent = '×';
    closeButton.setAttribute('role','button');
    closeButton.setAttribute('aria-label','Melléklap bezárása');
    tab.append(titleSpan, closeButton);

    const panel = document.createElement('div');
    panel.className = 'multiform-runtime-panel';
    panel.dataset.partPanel = key;
    const detail = document.createElement('div');
    detail.className = 'multiform-selected-form-host';
    renderRuntimeDetailPane(detail, row, state.repeatingPart.name, state.repeatingPart.label, state.totalSuggestions || state.repeatingPart.count);
    panel.appendChild(detail);

    state.tabsElement.appendChild(tab);
    state.shellElement.appendChild(panel);
    tab.addEventListener('click', event => { if(!event.target.closest('.multiform-runtime-tab-close')) state.activatePanel(key, state.repeatingPart.name); });
    panel.addEventListener('input', () => state.markPanelDirty?.(key));
    panel.addEventListener('change', () => state.markPanelDirty?.(key));
    closeButton.addEventListener('click', event => {
      event.stopPropagation();
      if(tab.dataset.dirty === 'true'){ window.alert('A melléklap mentetlen módosításokat tartalmaz. Mentse el, mielőtt bezárja.'); return; }
      const wasActive = tab.classList.contains('active');
      state.openDetailRows.delete(key); tab.remove(); panel.remove();
      if(wasActive) state.activatePanel(state.repeatingPart.name, state.repeatingPart.name);
    });
    opened = { tab, panel, row };
    state.openDetailRows.set(key, opened);
  }
  state.selectedIndex = row.index;
  state.detailRow = row;
  state.activatePanel(key, state.repeatingPart.name, options);
  markXmlViewsDirty();
  ensureActiveXmlViewRendered('tree', { force:true });
  window.setTimeout(() => refreshFieldSearch({ showList:false }), 0);
}


/**
 * Feldolgozza a extract form part occurrence from validation path bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 * @returns {*} a feldolgozás eredménye
 */
function extractFormPartOccurrenceFromValidationPath(path, partName){
  const normalizedPart = String(partName || '').trim();
  if(!normalizedPart) return 0;
  const namespaceFree = String(path || '').replace(/Q\{[^}]*\}/g, '').replace(/^[^/]*:/, '');
  const escaped = normalizedPart.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = namespaceFree.match(new RegExp(`(?:^|/)${escaped}(?:\\[(\\d+)\\])?`, 'i'));
  return match ? Number(match[1] || 1) : 0;
}

/**
 * Betölti vagy lekéri a load runtime row for validation occurrence művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} occurrenceIndex az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function loadRuntimeRowForValidationOccurrence(occurrenceIndex){
  const state = currentMultiformState;
  const wanted = Number(occurrenceIndex || 0);
  if(!state?.repeatingPart || wanted < 1) return null;
  const existing = (state.repeatingPart.rows || []).find(row => Number(row?.index) === wanted)
    || (state.suggestionRows || []).find(row => Number(row?.index) === wanted);
  if(existing) return existing;
  if(!state.serverPaged) return null;

  const xmlFileId = activeXmlFileIdForLargeMultiform();
  if(!xmlFileId) return null;
  const pageSize = Math.max(1, Number(state.serverPageSize || 20));
  const page = Math.floor((wanted - 1) / pageSize);
  const params = new URLSearchParams({
    formName: state.repeatingPart.name,
    page: String(page),
    size: String(pageSize),
    q: ''
  });
  const response = await fetch(`/api/xml-files/${encodeURIComponent(xmlFileId)}/large-multiform/rows?${params}`, { credentials:'same-origin' });
  const data = await response.json().catch(() => ({}));
  if(!response.ok) throw new Error(data.message || data.error || 'A hibával érintett melléklap betöltése sikertelen.');
  return (data.rows || []).map(parseServerRuntimeRow).find(row => Number(row?.index) === wanted) || null;
}

/**
 * A <code>ensureMultiformValidationTargetVisible</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function ensureMultiformValidationTargetVisible(path){
  const state = currentMultiformState;
  if(!state?.repeatingPart) return false;
  const validationPath = String(path || '');
  const repeatingOccurrence = extractFormPartOccurrenceFromValidationPath(validationPath, state.repeatingPart.name);
  if(repeatingOccurrence > 0){
    const row = await loadRuntimeRowForValidationOccurrence(repeatingOccurrence);
    if(!row) return false;
    openRuntimeDetailTab(row, { validationNavigation:true });
    await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
    return true;
  }

  const mainOccurrence = extractFormPartOccurrenceFromValidationPath(validationPath, state.mainPartName);
  if(mainOccurrence > 0){
    state.activatePanel?.(state.mainPartName, state.mainPartName, { validationNavigation:true });
    await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
    return true;
  }
  return false;
}

/**
 * Betölti vagy lekéri a load runtime table page művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} page az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function loadRuntimeTablePage(page = 0){
  const state = currentMultiformState;
  if(!state?.serverPaged) return;
  state.serverPage = Math.max(0, page);
  await loadServerRuntimeSuggestions(true);
}

/**
 * Megjeleníti vagy újrarendereli a render runtime index predictions állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} rows a függvény rows bemeneti értéke
 */
function renderRuntimeIndexPredictions(rows){
  const state = currentMultiformState;
  const results = document.getElementById('fieldSearchResults');
  const counter = document.getElementById('fieldSearchCounter');
  if(!results || !state || state.activePanelKey !== state.repeatingPart?.name) return;
  const visible = (rows || []).slice(0, 12);
  if(counter) counter.textContent = visible.length ? `1/${visible.length}` : '0/0';
  if(!visible.length){
    results.innerHTML = '<div class="field-search-empty">Nincs találat.</div>';
    results.hidden = false;
    return;
  }
  results.innerHTML = '';
  visible.forEach(row => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'field-search-result';
    const title = document.createElement('span');
    title.className = 'field-search-result-label';
    title.textContent = row.serverLabel || `${row.index}. melléklap`;
    const meta = document.createElement('span');
    meta.className = 'field-search-result-meta';
    const values = Object.entries(row.values || {}).filter(([, value]) => String(value || '').trim()).slice(0, 4);
    meta.textContent = values.map(([name, value]) => `${name}: ${value}`).join(' · ');
    button.append(title, meta);
    button.addEventListener('click', async () => {
      const input = document.getElementById('fieldSearchInput');
      const selectedText = String(row.serverLabel || '').replace(/^\d+\s*-\s*/, '').trim();
      state.query = selectedText;
      if(input) input.value = selectedText;
      results.hidden = true;
      await loadRuntimeTablePage(0);
      const panel = state.shellElement.querySelector(`[data-part-panel="${state.repeatingPart.name}"] .multiform-repeating-selector-layout`);
      if(panel) refreshRuntimeRepeatingPanel(panel, { keepSuggestions:true });
    });
    results.appendChild(button);
  });
  results.hidden = false;
}

/**
 * A <code>paginationWindow</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} current a függvény current bemeneti értéke
 * @param {*} pages az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
 * @returns {*} a feldolgozás eredménye
 */
function paginationWindow(current, pages){
  if(pages <= 7) return Array.from({length:pages}, (_, index) => index + 1);
  const set = new Set([1, pages, current, current - 1, current + 1]);
  if(current <= 3) [2,3,4].forEach(value => set.add(value));
  if(current >= pages - 2) [pages - 3, pages - 2, pages - 1].forEach(value => set.add(value));
  return Array.from(set).filter(value => value >= 1 && value <= pages).sort((a,b) => a-b);
}

/**
 * Megjeleníti vagy újrarendereli a render runtime repeating part panel állapotát a felhasználói felületen.
 *
 * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
 * @param {*} options a művelet opcionális beállításai
 * @returns {*} a feldolgozás eredménye
 */
function renderRuntimeRepeatingPartPanel(options = {}){
  const state = currentMultiformState;
  const part = state.repeatingPart;
  const container = document.createElement('div');
  container.className = 'multiform-repeating-selector-layout multiform-table-layout';

  if(state.selectorMount){
    state.selectorMount.replaceChildren();
    state.selectorMount.hidden = state.activePanelKey !== part.name;
  }

  const queryInput = document.getElementById('fieldSearchInput');
  if(queryInput){
    queryInput.placeholder = 'Keresés a konfigurált indexmezőkben...';
    queryInput.value = state.query || '';
    queryInput.dataset.multiformIndexSearch = 'true';
    if(queryInput.dataset.multiformTableBound !== 'true'){
      queryInput.dataset.multiformTableBound = 'true';
      let debounceTimer = null;
      queryInput.addEventListener('input', () => {
        const runtime = currentMultiformState;
        if(!runtime || runtime.activePanelKey !== runtime.repeatingPart?.name) return;
        runtime.query = queryInput.value;
        window.clearTimeout(debounceTimer);
        debounceTimer = window.setTimeout(async () => {
          const q = String(runtime.query || '').trim();
          if(q && q.length < 3){
            renderRuntimeIndexPredictions([]);
            return;
          }
          try{
            await loadRuntimeTablePage(0);
            renderRuntimeIndexPredictions(runtime.suggestionRows || []);
            const panel = runtime.shellElement.querySelector(`[data-part-panel="${runtime.repeatingPart.name}"] .multiform-repeating-selector-layout`);
            if(panel) refreshRuntimeRepeatingPanel(panel, { keepSuggestions:true });
          }catch(error){ window.alert(error.message); }
        }, 250);
      });
    }
  }

  const tableCard = document.createElement('section');
  tableCard.className = 'multiform-table-card';
  const tableHeader = document.createElement('div');
  tableHeader.className = 'multiform-table-header';
  const title = document.createElement('strong');
  title.textContent = `${part.label || runtimeFormPartLabel(part.name)} lista`;
  const summary = document.createElement('span');
  const total = Number(state.totalSuggestions || part.count || 0);
  summary.textContent = `${total.toLocaleString('hu-HU')} elem`;
  tableHeader.append(title, summary);
  tableCard.appendChild(tableHeader);

  const tableWrap = document.createElement('div');
  tableWrap.className = 'multiform-table-scroll';
  const table = document.createElement('table');
  table.className = 'xml-files-table multiform-record-table';
  const columns = Array.isArray(state.tableColumns) ? state.tableColumns : [];
  const thead = document.createElement('thead');
  const headRow = document.createElement('tr');
  ['Sorszám', ...columns.map(column => column.label || column.name)].forEach(label => {
    const th = document.createElement('th'); th.scope = 'col'; th.textContent = label; headRow.appendChild(th);
  });
  thead.appendChild(headRow);
  table.appendChild(thead);
  const tbody = document.createElement('tbody');
  const rows = state.suggestionRows || [];
  if(!rows.length){
    const tr = document.createElement('tr');
    const td = document.createElement('td'); td.colSpan = Math.max(1, columns.length + 1); td.className = 'multiform-table-empty'; td.textContent = 'Nincs megjeleníthető melléklap.'; tr.appendChild(td);
    tbody.appendChild(tr);
  }else{
    rows.forEach(row => {
      const tr = document.createElement('tr');
      const indexCell = document.createElement('td');
      const link = document.createElement('button');
      link.type = 'button';
      link.className = 'multiform-record-link';
      link.textContent = String(row.index);
      link.addEventListener('click', () => openRuntimeDetailTab(row));
      indexCell.appendChild(link);
      tr.appendChild(indexCell);
      columns.forEach(column => {
        const td = document.createElement('td');
        td.textContent = String(row.values?.[column.name] || '');
        tr.appendChild(td);
      });
      tbody.appendChild(tr);
    });
  }
  table.appendChild(tbody);
  tableWrap.appendChild(table);
  tableCard.appendChild(tableWrap);

  const pager = document.createElement('div');
  pager.className = 'xml-files-pagination multiform-table-pager';
  pager.setAttribute('aria-label', 'Melléklapok lapozása');
  const pageSize = Number(state.serverPageSize || 20);
  const page = Number(state.serverPage || 0);
  const pageCount = Math.max(1, Math.ceil(total / pageSize));
  const from = total === 0 ? 0 : page * pageSize + 1;
  const to = Math.min(total, (page + 1) * pageSize);

  const pageInfo = document.createElement('div');
  pageInfo.className = 'xml-files-pagination-info';
  pageInfo.textContent = `${from}-${to} / ${total.toLocaleString('hu-HU')} melléklap`;

  const actions = document.createElement('div');
  actions.className = 'xml-files-pagination-actions';
    /**
   * Előkészíti és elindítja a create pager button állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} label a függvény label bemeneti értéke
   * @param {*} ariaLabel a függvény ariaLabel bemeneti értéke
   * @param {*} targetPage az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
   * @param {*} disabled a függvény disabled bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
const createPagerButton = (label, ariaLabel, targetPage, disabled = false) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'secondary mini-button';
    button.textContent = label;
    button.setAttribute('aria-label', ariaLabel);
    button.disabled = disabled;
    button.addEventListener('click', async () => {
      try{ await loadRuntimeTablePage(targetPage); refreshRuntimeRepeatingPanel(container, {keepSuggestions:true}); }
      catch(error){ window.alert(error.message); }
    });
    return button;
  };
  actions.appendChild(createPagerButton('«', 'Első oldal', 0, page <= 0));
  actions.appendChild(createPagerButton('‹', 'Előző oldal', Math.max(0, page - 1), page <= 0));
  const numbers = document.createElement('span');
  numbers.className = 'xml-files-page-indicator';
  let previousNumber = 0;
  paginationWindow(page + 1, pageCount).forEach(pageNumber => {
    if(previousNumber && pageNumber - previousNumber > 1){
      const gap = document.createElement('span'); gap.className = 'xml-files-page-gap'; gap.textContent = '…'; numbers.appendChild(gap);
    }
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `xml-files-page-number${pageNumber === page + 1 ? ' is-active' : ''}`;
    button.textContent = String(pageNumber);
    if(pageNumber === page + 1) button.setAttribute('aria-current','page');
    button.addEventListener('click', async () => {
      try{ await loadRuntimeTablePage(pageNumber - 1); refreshRuntimeRepeatingPanel(container, {keepSuggestions:true}); }
      catch(error){ window.alert(error.message); }
    });
    numbers.appendChild(button); previousNumber = pageNumber;
  });
  actions.appendChild(numbers);
  actions.appendChild(createPagerButton('›', 'Következő oldal', Math.min(pageCount - 1, page + 1), page + 1 >= pageCount));
  actions.appendChild(createPagerButton('»', 'Utolsó oldal', pageCount - 1, page + 1 >= pageCount));
  pager.append(pageInfo, actions);
  tableCard.appendChild(pager);
  container.appendChild(tableCard);
  return container;
}

/**
 * Megjeleníti vagy újrarendereli a render runtime detail pane állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} container a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @param {*} row a függvény row bemeneti értéke
 * @param {*} partName a feloldáshoz vagy megjelenítéshez használt név
 * @param {*} partLabel a függvény partLabel bemeneti értéke
 * @param {*} total a függvény total bemeneti értéke
 */
function renderRuntimeDetailPane(container, row, partName, partLabel, total){
  container.replaceChildren();
  if(!row){
    const empty = document.createElement('div');
    empty.className = 'multiform-detail-empty';
    empty.textContent = 'Nincs kiválasztott melléklap. Válassz egy M lapot a fenti kereshető legördülőből.';
    container.appendChild(empty);
    return;
  }
  const header = document.createElement('div');
  header.className = 'multiform-detail-header';
  header.innerHTML = `<strong>${escapeHtml(partLabel || runtimeFormPartLabel(partName))} / ${row.index}. melléklap</strong><span>Találati halmaz: ${escapeHtml(String(total))} elem</span>`;
  container.appendChild(header);

  const rowLeaves = ensureRuntimeRowLeaves(row, currentMultiformState?.repeatingPart?.labelLookup, currentMultiformState?.repeatingPart?.displayFields, partName);
  const groups = new Map();
  rowLeaves.forEach(field => {
    const descriptor = runtimeDetailGroupDescriptor(partName, field);
    if(!groups.has(descriptor.key)) groups.set(descriptor.key, { descriptor, fields: [] });
    groups.get(descriptor.key).fields.push(field);
  });

  groups.forEach(group => {
    const { descriptor, fields } = group;
    const groupName = descriptor.label;
    const card = document.createElement('section');
    card.className = `uimodel-section-card multiform-detail-card collapsible-card${descriptor.chain ? ' multiform-detail-chain-card' : ''}`;
    if(descriptor.chain) card.dataset.chainOccurrence = String(descriptor.occurrence);
    const headerButton = document.createElement('button');
    headerButton.type = 'button';
    headerButton.className = 'uimodel-section-header collapse-toggle';
    headerButton.setAttribute('aria-expanded', 'true');
    headerButton.innerHTML = `<span class="uimodel-section-number">${escapeHtml(String(container.querySelectorAll('.multiform-detail-card').length + 1))}</span><h3>${escapeHtml(groupName || 'Adatok')}</h3><span class="collapse-chevron" aria-hidden="true">▾</span>`;
    const content = document.createElement('div');
    content.className = 'collapsible-content uimodel-section-content';
    const grid = document.createElement('div');
    grid.className = 'uimodel-fields-grid';
    fields.forEach(field => {
      const item = createRuntimeUiModelFieldElement(field, row, partName);
      if(item) grid.appendChild(item);
    });
    content.appendChild(grid);
    card.appendChild(headerButton);
    card.appendChild(content);
    container.appendChild(card);
  });
  container.addEventListener('input', event => {
    const input = event.target.closest('[data-runtime-leaf-index]');
    if(!input || currentXmlFileReadOnlyMode) return;
    globalThis.clearEditedFieldXsdHighlight?.(input);
    const leaf = rowLeaves[Number(input.dataset.runtimeLeafIndex)];
    if(!leaf?.node) return;
    leaf.node.textContent = input.value;
    leaf.value = input.value;
    if(Object.prototype.hasOwnProperty.call(row.values || {}, leaf.name)) row.values[leaf.name] = input.value;
    row.searchText = null;
    markXmlViewsDirty();
    scheduleXmlFromCurrentState();
    markFormDirty();
  });
  bindCollapseToggles(container);
}

/**
 * Szinkronizálja vagy frissíti a sync rendered field values from xml által kezelt állapotot a megadott adatok alapján.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} container a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 */
function syncRenderedFieldValuesFromXml(container){
  if(!container || !currentXmlDocument) return;
  container.querySelectorAll('.form-field[data-xml-path], .uimodel-field[data-xml-path]').forEach(wrapper => {
    const xmlPath = wrapper.dataset.xmlPath || '';
    if(!xmlPath) return;
    const node = findNodeByPath(currentXmlDocument, xmlPath);
    if(!node || node.children.length) return;
    const value = node.textContent || '';
    const control = wrapper.querySelector('input[data-field-id], select[data-field-id], textarea[data-field-id]');
    if(!control) return;
    if(control.type === 'checkbox'){
      control.checked = ['true', '1', 'x', 'X', 'I', 'i'].includes(String(value));
    } else if(control.tagName === 'SELECT'){
      const hasOption = Array.from(control.options || []).some(option => String(option.value) === String(value));
      if(hasOption) control.value = value;
    } else {
      control.value = formatUiModelInitialValue(value, { type: control.type });
    }
    wrapper.classList.remove('uimodel-missing-field');
  });
}

/**
 * Feldolgozza a normalize debug field id bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function normalizeDebugFieldId(value){
  return String(value || '').replace(/^Field_/, '');
}

/**
 * Ellenőrzi a is debug field id feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @param {*} requested a backend-hívás kérésadata
 * @returns {*} a feldolgozás eredménye
 */
function isDebugFieldId(value, requested){
  const normalized = normalizeDebugFieldId(value);
  const requestedNormalized = normalizeDebugFieldId(requested || '0A0001C001A');
  return normalized && normalized === requestedNormalized;
}

/**
 * Feldolgozza a collect form definition field debug bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} fieldId a célobjektum technikai azonosítója
 * @returns {*} a feldolgozás eredménye
 */
function collectFormDefinitionFieldDebug(fieldId){
  const rows = [];
  for(const tab of (currentFormDefinition?.tabs || [])){
    for(const section of (tab.sections || [])){
      for(const row of (section.rows || [])){
        for(const field of (row.fields || [])){
          if(!isDebugFieldId(field?.id || field?.xmlName, fieldId)) continue;
          rows.push({
            tab: tab.id || tab.title || '',
            section: section.id || section.title || '',
            row: row.id || row.title || '',
            rowTitle: row.title || '',
            fieldId: field.id || '',
            xmlName: field.xmlName || '',
            label: field.uiLabel || field.xsdLabel || field.label || '',
            type: field.type || '',
            enumCount: Array.isArray(field.enumValues) ? field.enumValues.length : 0,
            enumSample: Array.isArray(field.enumValues) ? field.enumValues.slice(0, 5).join(' | ') : '',
            xmlPath: field.xmlPath || '',
            maxLength: field.maxLength || '',
            mask: field.mask || '',
            layoutWidth: field.layoutWidth || ''
          });
        }
      }
    }
  }
  return rows;
}

/**
 * Feldolgozza a collect form data field debug bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} fieldId a célobjektum technikai azonosítója
 * @returns {*} a feldolgozás eredménye
 */
function collectFormDataFieldDebug(fieldId){
  const rows = [];
  const valuesByFieldId = currentFormData?.valuesByFieldId || {};
  Object.entries(valuesByFieldId).forEach(([key, valueObj]) => {
    if(isDebugFieldId(key, fieldId) || isDebugFieldId(valueObj?.fieldId || valueObj?.key, fieldId)){
      rows.push({
        source: 'valuesByFieldId',
        key,
        fieldId: valueObj?.fieldId || '',
        valueKey: valueObj?.key || '',
        value: valueObj?.value || '',
        present: valueObj?.present === true,
        xmlPath: valueObj?.xmlPath || ''
      });
    }
  });
  Object.entries(currentFormData?.rowInstancesByRowId || {}).forEach(([rowId, instances]) => {
    (instances || []).forEach((instance, index) => {
      Object.entries(instance?.valuesByFieldId || {}).forEach(([key, valueObj]) => {
        if(isDebugFieldId(key, fieldId) || isDebugFieldId(valueObj?.fieldId || valueObj?.key, fieldId)){
          rows.push({
            source: 'rowInstancesByRowId',
            rowId,
            instanceIndex: index + 1,
            key,
            fieldId: valueObj?.fieldId || '',
            valueKey: valueObj?.key || '',
            value: valueObj?.value || '',
            present: valueObj?.present === true,
            xmlPath: valueObj?.xmlPath || ''
          });
        }
      });
    });
  });
  return rows;
}

/**
 * Feldolgozza a collect dom field debug bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} fieldId a célobjektum technikai azonosítója
 * @returns {*} a feldolgozás eredménye
 */
function collectDomFieldDebug(fieldId){
  const rows = [];
  document.querySelectorAll('.form-field[data-field-id], .uimodel-field[data-field-id], .uimodel-table-row[data-field-id]').forEach((wrapper, index) => {
    const wrapperFieldId = wrapper.dataset.fieldId || '';
    if(!isDebugFieldId(wrapperFieldId, fieldId)) return;
    const control = wrapper.querySelector('input[data-field-id], select[data-field-id], textarea[data-field-id]');
    rows.push({
      domIndex: index,
      wrapperTag: wrapper.tagName,
      wrapperClass: wrapper.className,
      fieldId: wrapperFieldId,
      xmlPath: wrapper.dataset.xmlPath || '',
      controlTag: control?.tagName || '',
      controlType: control?.type || '',
      controlFieldId: control?.dataset?.fieldId || '',
      value: control ? (control.type === 'checkbox' ? String(control.checked) : control.value) : '',
      optionCount: control?.tagName === 'SELECT' ? control.options.length : 0,
      optionsSample: control?.tagName === 'SELECT' ? Array.from(control.options).slice(0, 5).map(o => o.value).join(' | ') : ''
    });
  });
  return rows;
}

/**
 * Feldolgozza a build debug xml path for node bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 * @returns {*} a feldolgozás eredménye
 */
function buildDebugXmlPathForNode(node){
  if(!node || node.nodeType !== 1) return '';
  const segments = [];
  let current = node;
  while(current && current.nodeType === 1){
    const name = resolveNodeName(current);
    let sameBefore = 0;
    let sameTotal = 0;
    if(current.parentElement){
      Array.from(current.parentElement.children).forEach(child => {
        if(resolveNodeName(child) === name){
          sameTotal += 1;
          if(child === current) sameBefore = sameTotal;
        }
      });
    }
    const indexed = sameTotal > 1 ? `${name}[${sameBefore || 1}]` : name;
    segments.unshift(indexed);
    current = current.parentElement;
  }
  return '/' + segments.join('/');
}

/**
 * Feldolgozza a collect xml node field debug bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} fieldId a célobjektum technikai azonosítója
 * @returns {*} a feldolgozás eredménye
 */
function collectXmlNodeFieldDebug(fieldId){
  const rows = [];
  if(!currentXmlDocument) return rows;
  const targetLocalName = `Field_${normalizeDebugFieldId(fieldId)}`;
  const walker = currentXmlDocument.createTreeWalker(currentXmlDocument.documentElement, NodeFilter.SHOW_ELEMENT);
  let node = currentXmlDocument.documentElement;
  while(node){
    if(resolveNodeName(node) === targetLocalName){
      rows.push({
        xmlPath: buildDebugXmlPathForNode(node),
        value: node.children?.length ? '[has children]' : (node.textContent || ''),
        parent: node.parentElement ? resolveNodeName(node.parentElement) : ''
      });
    }
    node = walker.nextNode();
  }
  return rows;
}

/**
 * Feldolgozza a collect xsd issue field debug bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} fieldId a célobjektum technikai azonosítója
 * @returns {*} a feldolgozás eredménye
 */
function collectXsdIssueFieldDebug(fieldId){
  const rows = [];
  const errors = Array.isArray(currentXsdValidationState?.errors) ? currentXsdValidationState.errors : [];
  errors.forEach((issue, index) => {
    const text = xsdIssueSearchText(issue);
    if(text.includes(normalizeDebugFieldId(fieldId)) || text.includes(`Field_${normalizeDebugFieldId(fieldId)}`)){
      rows.push({
        index,
        code: issue.code || '',
        severity: issue.severity || '',
        path: issue.path || '',
        message: issue.message || ''
      });
    }
  });
  return rows;
}

/**
 * A <code>debugFieldBinding</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} fieldId a célobjektum technikai azonosítója
 * @param {*} reason a függvény reason bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function debugFieldBinding(fieldId = NAV_FIELD_BINDING_DEBUG_TARGET || '0A0001D003A', reason = 'manual'){
  const normalized = normalizeDebugFieldId(fieldId);
  const defRows = collectFormDefinitionFieldDebug(normalized);
  const dataRows = collectFormDataFieldDebug(normalized);
  const domRows = collectDomFieldDebug(normalized);
  const xmlRows = collectXmlNodeFieldDebug(normalized);
  const xsdRows = collectXsdIssueFieldDebug(normalized);
  console.group(`NAV FIELD_BIND_DEBUG ${normalized} (${reason})`);
  console.info('Aktív multiform tab:', currentMultiformState?.activePartName || null, 'selectedIndex:', currentMultiformState?.selectedIndex || null);
  console.info('XSD state:', currentXsdValidationState || null);
  console.info('Form definition találatok:', defRows.length);
  console.table(defRows);
  console.info('Form data találatok:', dataRows.length);
  console.table(dataRows);
  console.info('DOM találatok:', domRows.length);
  console.table(domRows);
  console.info('XML node találatok:', xmlRows.length);
  console.table(xmlRows);
  console.info('XSD issue találatok:', xsdRows.length);
  console.table(xsdRows);
  const suspiciousSelects = domRows.filter(row => row.controlTag === 'SELECT');
  if(suspiciousSelects.length){
    console.warn('A problémás mező SELECT-ként renderelődött. Nézd meg a hozzá tartozó formDefinition sort: type/enumCount/xmlPath.', suspiciousSelects);
  }
  console.groupEnd();
  return { formDefinition: defRows, formData: dataRows, dom: domRows, xmlNodes: xmlRows, xsdIssues: xsdRows };
}

/**
 * A <code>scheduleFieldBindingDebug</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} reason a függvény reason bemeneti értéke
 */
function scheduleFieldBindingDebug(reason){
  if(!NAV_FIELD_BINDING_DEBUG_ENABLED || navFieldBindingAutoDebugScheduled) return;
  navFieldBindingAutoDebugScheduled = true;
  window.setTimeout(() => {
    navFieldBindingAutoDebugScheduled = false;
    const target = NAV_FIELD_BINDING_DEBUG_TARGET || '0A0001D003A';
    const prefixed = `Field_${target}`;
    const selector = [
      `.form-field[data-field-id="${target}"]`,
      `.form-field[data-field-id="${prefixed}"]`,
      `.uimodel-field[data-field-id="${target}"]`,
      `.uimodel-field[data-field-id="${prefixed}"]`
    ].join(',');
    const hasTarget = document.querySelector(selector);
    if(hasTarget) debugFieldBinding(target, reason || 'auto');
  }, 250);
}

Object.assign(globalThis, {
  isUiModelMissingField,
  xmlLocalName,
  directElementChildren,
  detectRuntimeMultiformParts,
  normalizeRuntimeFormName,
  normalizeRuntimeStructuralPath,
  runtimeStructuralLabelMap,
  runtimeStructuralLabelForPath,
  runtimePartPath,
  runtimeFormPartLabel,
  runtimeStructuralGroupLabel,
  runtimeFieldPath,
  collectRuntimeLeafFields,
  collectRuntimeLabelLookup,
  runtimeFriendlyFieldLabel,
  runtimeFieldIdFromName,
  runtimeOccurrenceXmlPath,
  createRuntimeUiModelFieldElement,
  buildRuntimeOptionLabel,
  buildRuntimePartIndex,
  getRuntimeMultiformLabel,
  formPartPathPrefix,
  pathBelongsToFormPart,
  pruneRenderedPanelToFormPart,
  getFormUnifiedToolbar,
  ensureToolbarGroup,
  restoreMultiformRuntimeToolbarControls,
  ensureStandardFormToolbarControlsVisible,
  createMultiformRuntimeToolbar,
  enhanceMultiformRuntimeView,
  runtimeMatches,
  computeRuntimeSuggestions,
  refreshRuntimeRepeatingPanel,
  ensureMultiformValidationTargetVisible,
  renderRuntimeRepeatingPartPanel,
  renderRuntimeDetailPane,
  syncRenderedFieldValuesFromXml,
  normalizeDebugFieldId,
  isDebugFieldId,
  collectFormDefinitionFieldDebug,
  collectFormDataFieldDebug,
  collectDomFieldDebug,
  buildDebugXmlPathForNode,
  collectXmlNodeFieldDebug,
  collectXsdIssueFieldDebug,
  debugFieldBinding,
  scheduleFieldBindingDebug
});

/**
 * A <code>markActiveMultiformPanelSaved</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 */
export function markActiveMultiformPanelSaved(){ currentMultiformState?.markActivePanelSaved?.(); }
