/**
 * Előkészíti és elindítja a create m2m attachment service állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
 * @param {*} context a függvény context bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
/**
 * @module m2m/m2m-attachments-ui
 *
 * A M2M beküldési és csatolmánykezelési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A kliensoldali engedélyezés csak felhasználói visszajelzés; a tényleges M2M jogosultsági kontroll szerveroldali.
 */

export function createM2mAttachmentService(context){
    /**
   * A <code>state</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
const state = () => context.getState();
  const xml = context.xml;
  let selectionBridgeInstalled = false;

    /**
   * Kezeli vagy beköti a attachment definition paths esemény- és inicializációs folyamatát.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} source a függvény source bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function attachmentDefinitionPaths(source){
    const paths = [];
        /**
     * A <code>visit</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @param {*} value a feldolgozandó vagy beállítandó érték
     */
const visit = value => {
      if(value == null) return;
      if(Array.isArray(value)){
        value.forEach(visit);
        return;
      }
      if(typeof value !== 'object') return;
      Object.entries(value).forEach(([key, child]) => {
        if((key === 'xmlPath' || key === 'path') && typeof child === 'string'){
          const canonical = xml.canonicalizeXmlPath(child);
          if(/(^|\/)attachment_1(?:\[\d+\])?(?:\/|$)/i.test(canonical)) paths.push(canonical);
        }
        visit(child);
      });
    };
    visit(source);
    return [...new Set(paths)];
  }

    /**
   * A <code>formDefinitionHasAttachment1</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} source a függvény source bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function formDefinitionHasAttachment1(source){
    return attachmentDefinitionPaths(source).length > 0;
  }

    /**
   * A <code>xmlDocumentHasAttachment1</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} document a függvény document bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function xmlDocumentHasAttachment1(document){
    if(!document?.documentElement) return false;
    return Array.from(document.getElementsByTagName('*'))
      .some(element => xml.resolveNodeName(element).toLowerCase() === 'attachment_1');
  }

    /**
   * Feldolgozza a normalize file name bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} name a feloldáshoz vagy megjelenítéshez használt név
   * @returns {*} a feldolgozás eredménye
   */
function normalizeFileName(name){
    return String(name || '').trim().normalize('NFC').toLowerCase();
  }

    /**
   * Feloldja a find file name eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} attachment a függvény attachment bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function findFileName(attachment){
    const children = Array.from(attachment?.children || []);
    return children.find(child => xml.resolveNodeName(child).toLowerCase() === 'filename')?.textContent || '';
  }

    /**
   * Feloldja a find file id node eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} attachment a függvény attachment bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function findFileIdNode(attachment){
    return Array.from(attachment?.children || [])
      .find(child => xml.resolveNodeName(child).toLowerCase() === 'fileid') || null;
  }

    /**
   * Feloldja a find file id value eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} attachment a függvény attachment bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function findFileIdValue(attachment){
    return findFileIdNode(attachment)?.textContent?.trim() || '';
  }

    /**
   * Szinkronizálja vagy frissíti a set file id által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} attachment a függvény attachment bemeneti értéke
   * @param {*} fileId a célobjektum technikai azonosítója
   * @returns {*} a feldolgozás eredménye
   */
function setFileId(attachment, fileId){
    const document = state().currentXmlDocument;
    if(!attachment || !fileId || !document) return false;
    let fileIdNode = findFileIdNode(attachment);
    if(!fileIdNode){
      fileIdNode = document.createElement('fileId');
      attachment.insertBefore(fileIdNode, attachment.firstChild || null);
    }
    fileIdNode.textContent = fileId;
    return true;
  }

    /**
   * Feldolgozza a collect nodes bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function collectNodes(){
    const document = state().currentXmlDocument;
    if(!document) return [];
    return Array.from(document.getElementsByTagName('*'))
      .filter(element => xml.resolveNodeName(element).toLowerCase() === 'attachment_1');
  }

    /**
   * Feldolgozza a collect file names bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function collectFileNames(){
    return collectNodes().map(findFileName).filter(Boolean);
  }

    /**
   * Feloldja a find duplicate file name eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} newNames a feloldáshoz vagy megjelenítéshez használt név
   * @returns {*} a feldolgozás eredménye
   */
function findDuplicateFileName(newNames){
    const names = new Set();
    for(const name of collectFileNames().concat(newNames || [])){
      const key = normalizeFileName(name);
      if(!key) continue;
      if(names.has(key)) return name;
      names.add(key);
    }
    return null;
  }

    /**
   * Kezeli vagy beköti a attachment base path esemény- és inicializációs folyamatát.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @returns {*} a feldolgozás eredménye
   */
function attachmentBasePath(){
    const path = attachmentDefinitionPaths(state().currentFormDefinition)[0] || '';
    if(!path) return '';
    const match = path.match(/^(.*?\/attachment_1)(?:\[\d+\])?(?:\/.*)?$/i);
    return match ? `${match[1].replace(/\/attachment_1$/i, '')}/Attachment_1` : '';
  }

    /**
   * Feloldja a resolve parent for new node eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function resolveParentForNewNode(){
    const current = state();
    const document = current.currentXmlDocument;
    if(!document?.documentElement) throw new Error('Nincs aktív XML dokumentum.');
    if(!formDefinitionHasAttachment1(current.currentFormDefinition)){
      throw new Error('A csatolmány hozzáadása csak olyan XML-nél engedélyezett, amelynek XSD-jében szerepel az Attachment_1 elem.');
    }
    const basePath = attachmentBasePath();
    if(!basePath){
      throw new Error('Az XSD alapján nem határozható meg az Attachment_1 pontos XML-útvonala.');
    }
    let parent = document.documentElement;
    let parentPath = `/${xml.resolveNodeName(parent)}`;
    if(basePath){
      const parts = xml.canonicalizeXmlPath(basePath).split('/').filter(Boolean);
      if(parts.length > 1){
        parentPath = '/' + parts.slice(0, -1).join('/');
        parent = xml.findNodeByPath(document, parentPath) || xml.createNodeByPath(document, parentPath);
      }
    }
    if(!parent) throw new Error('Az Attachment_1 szülő eleme nem található vagy nem hozható letre.');
    return { parent, parentPath };
  }

    /**
   * Előkészíti és elindítja a add placeholder állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function addPlaceholder(file){
    const document = state().currentXmlDocument;
    const { parent, parentPath } = resolveParentForNewNode();
    const attachment = document.createElement('Attachment_1');
    const fileId = document.createElement('fileId');
    const fileName = document.createElement('fileName');
    const fileSize = document.createElement('fileSize');
    fileId.textContent = '';
    fileName.textContent = file.name;
    fileSize.textContent = String(file.size || 0);
    attachment.append(fileId, fileName, fileSize);
    xml.insertXmlElementInSchemaOrder(parent, attachment, parentPath);
    return attachment;
  }

    /**
   * Előkészíti és elindítja a add placeholders állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} files a függvény files bemeneti értéke
   */
function addPlaceholders(files){
    (files || []).forEach(addPlaceholder);
  }

    /**
   * Feloldja a find uploaded by file name eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} uploadedAttachments a függvény uploadedAttachments bemeneti értéke
   * @param {*} fileName a feloldáshoz vagy megjelenítéshez használt név
   * @returns {*} a feldolgozás eredménye
   */
function findUploadedByFileName(uploadedAttachments, fileName){
    const key = normalizeFileName(fileName);
    return (uploadedAttachments || []).find(item =>
      normalizeFileName(item?.originalFileName || item?.fileName) === key
    ) || null;
  }

    /**
   * Szinkronizálja vagy frissíti a apply uploaded ids által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} uploadedAttachments a függvény uploadedAttachments bemeneti értéke
   * @param {*} options a művelet opcionális beállításai
   * @returns {*} a feldolgozás eredménye
   */
function applyUploadedIds(uploadedAttachments, options = {}){
    if(!state().currentXmlDocument) return { updated: 0, missing: [] };
    const updateExistingFileIds = options.updateExistingFileIds === true;
    const originalNames = options.onlyFileNames || [];
    const requestedNames = originalNames.map(normalizeFileName).filter(Boolean);
    const requestedNameSet = new Set(requestedNames);
    const uploadedByName = new Map();
    (uploadedAttachments || []).forEach(item => {
      const fileId = item?.navFileId || item?.fileId || '';
      const key = normalizeFileName(item?.originalFileName || item?.fileName);
      if(key && fileId) uploadedByName.set(key, fileId);
    });

    let updated = 0;
    const updatedKeys = new Set();
    collectNodes().forEach(attachment => {
      const key = normalizeFileName(findFileName(attachment));
      if(!key || (requestedNameSet.size && !requestedNameSet.has(key))) return;
      if(findFileIdValue(attachment) && !updateExistingFileIds) return;
      const fileId = uploadedByName.get(key);
      if(fileId && setFileId(attachment, fileId)){
        updated += 1;
        updatedKeys.add(key);
      }
    });

    const missing = requestedNames
      .filter(key => !updatedKeys.has(key))
      .map(key => originalNames.find(name => normalizeFileName(name) === key) || key);
    return { updated, missing };
  }

    /**
   * Kezeli vagy beköti a attachment metadata esemény- és inicializációs folyamatát.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @returns {*} a feldolgozás eredménye
   */
function attachmentMetadata(){
    const list = state().m2mSubmission?.uploadedAttachments || [];
    const byName = new Map();
    const byFileId = new Map();
    list.forEach(item => {
      const nameKey = normalizeFileName(item?.originalFileName || item?.fileName);
      const fileId = String(item?.navFileId || item?.fileId || '').trim();
      if(nameKey) byName.set(nameKey, item);
      if(fileId) byFileId.set(fileId, item);
    });
    return { byName, byFileId };
  }

    /**
   * A <code>childValue</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} attachment a függvény attachment bemeneti értéke
   * @param {*} childName a feloldáshoz vagy megjelenítéshez használt név
   * @returns {*} a feldolgozás eredménye
   */
function childValue(attachment, childName){
    const expected = String(childName || '').toLowerCase();
    return Array.from(attachment?.children || [])
      .find(child => xml.resolveNodeName(child).toLowerCase() === expected)
      ?.textContent?.trim() || '';
  }

    /**
   * A <code>escapeHtml</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function escapeHtml(value){
    return String(value ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }

    /**
   * Feldolgozza a format date bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function formatDate(value){
    if(!value) return '–';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('hu-HU');
  }


    /**
   * Feloldja a resolve attachment lifecycle eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} meta a függvény meta bemeneti értéke
   * @param {*} xmlFileId a feldolgozandó XML-tartalom vagy XML DOM-objektum
   * @returns {*} a feldolgozás eredménye
   */
function resolveAttachmentLifecycle(meta, xmlFileId){
    const backendState = String(meta?.lifecycleState || '').trim().toUpperCase();
    const backendLabel = String(meta?.lifecycleLabel || '').trim();
    if(backendState){
      const stateClass = backendState === 'VALID'
        ? 'valid'
        : (backendState === 'EXPIRING_SOON' ? 'warning' : (backendState === 'EXPIRED' ? 'expired' : 'unknown'));
      return {
        state: backendState,
        stateClass,
        stateLabel: backendLabel || 'Ismeretlen',
        refreshAllowed: meta?.refreshAllowed === true,
        reason: String(meta?.lifecycleReason || '').trim(),
        localFileAvailable: meta?.localFileAvailable !== false
      };
    }

    const expiresAt = meta?.navExpiresAt ? new Date(meta.navExpiresAt) : null;
    const now = Date.now();
    const expired = !!expiresAt && expiresAt.getTime() <= now;
    const soon = !!expiresAt && !expired && expiresAt.getTime() - now <= 2 * 60 * 60 * 1000;
    const hasNavUpload = !!(meta?.navFileId || xmlFileId);
    const hasLifecycle = !!(meta?.navUploadedAt && meta?.navExpiresAt);
    const state = !hasNavUpload ? 'NOT_UPLOADED' : (!hasLifecycle ? 'UNKNOWN' : (expired ? 'EXPIRED' : (soon ? 'EXPIRING_SOON' : 'VALID')));
    const stateClass = state === 'VALID' ? 'valid' : (state === 'EXPIRING_SOON' ? 'warning' : (state === 'EXPIRED' ? 'expired' : 'unknown'));
    const stateLabel = state === 'VALID' ? 'Érvényes'
      : (state === 'EXPIRING_SOON' ? 'Hamarosan lejár' : (state === 'EXPIRED' ? 'Lejárt' : (state === 'NOT_UPLOADED' ? 'Nincs feltöltve' : 'Ismeretlen')));
    const reason = state === 'EXPIRING_SOON'
      ? 'A csatolmány a biztonsági időn belül lejár, ezért a beküldés előtt meg kell újítani.'
      : (state === 'EXPIRED'
        ? 'A csatolmány NAV-oldali érvényessége lejárt.'
        : (state === 'NOT_UPLOADED'
          ? 'A csatolmányhoz nincs érvényes NAV fileId.'
          : (state === 'UNKNOWN' ? 'A NAV feltöltési vagy lejárati idő nem áll rendelkezésre.' : 'A csatolmány még érvényes.')));
    return {
      state,
      stateClass,
      stateLabel,
      refreshAllowed: !!meta?.id && state !== 'VALID',
      reason,
      localFileAvailable: true
    };
  }

    /**
   * Feldolgozza a format file size bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function formatFileSize(value){
    const bytes = Number(value);
    if(!Number.isFinite(bytes) || bytes < 0) return '–';
    if(bytes < 1024) return `${bytes} B`;
    const units = ['KB','MB','GB'];
    let size = bytes / 1024;
    let unit = units[0];
    for(let index = 1; index < units.length && size >= 1024; index += 1){
      size /= 1024;
      unit = units[index];
    }
    return `${size >= 10 ? size.toFixed(1) : size.toFixed(2)} ${unit}`;
  }

    /**
   * Kezeli vagy beköti a attachment type esemény- és inicializációs folyamatát.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} fileName a feloldáshoz vagy megjelenítéshez használt név
   * @returns {*} a feldolgozás eredménye
   */
function attachmentType(fileName){
    const name = String(fileName || '');
    return name.includes('.') ? name.split('.').pop().toUpperCase() : 'Fájl';
  }

    /**
   * Kezeli vagy beköti a attachment node path esemény- és inicializációs folyamatát.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
   * @returns {*} a feldolgozás eredménye
   */
function attachmentNodePath(node){
    if(!node || node.nodeType !== Node.ELEMENT_NODE) return '';
    const segments = [];
    let current = node;
    while(current && current.nodeType === Node.ELEMENT_NODE){
      const name = xml.resolveNodeName(current);
      if(current.parentElement){
        const sameNameSiblings = Array.from(current.parentElement.children)
          .filter(sibling => xml.resolveNodeName(sibling) === name);
        const index = Math.max(1, sameNameSiblings.indexOf(current) + 1);
        segments.unshift(`${name}[${index}]`);
      }else{
        segments.unshift(name);
      }
      current = current.parentElement;
    }
    return xml.canonicalizeXmlPath('/' + segments.join('/'));
  }

    /**
   * Feloldja a selected path belongs to attachment eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} selectedPath a mezőhöz vagy XML-csomóponthoz tartozó útvonal
   * @param {*} attachmentPath a mezőhöz vagy XML-csomóponthoz tartozó útvonal
   * @returns {*} a feldolgozás eredménye
   */
function selectedPathBelongsToAttachment(selectedPath, attachmentPath){
    const selected = xml.canonicalizeXmlPath(selectedPath || '');
    const attachment = xml.canonicalizeXmlPath(attachmentPath || '');
    return !!selected && !!attachment && (selected === attachment || selected.startsWith(`${attachment}/`));
  }

    /**
   * Szinkronizálja vagy frissíti a sync selected attachment card által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} selectedPath a mezőhöz vagy XML-csomóponthoz tartozó útvonal
   * @param {*} scrollIntoView a függvény scrollIntoView bemeneti értéke
   */
function syncSelectedAttachmentCard(selectedPath, scrollIntoView = true){
    let activeCard = null;
    document.querySelectorAll('.m2m-attachment-card[data-xml-path]').forEach(card => {
      const selected = selectedPathBelongsToAttachment(selectedPath, card.dataset.xmlPath || '');
      card.classList.toggle('field-selected', selected);
      card.setAttribute('aria-selected', String(selected));
      if(selected) activeCard = card;
    });
    if(activeCard && scrollIntoView){
      activeCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }

    /**
   * A <code>installSelectionBridge</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function installSelectionBridge(){
    if(selectionBridgeInstalled) return;
    selectionBridgeInstalled = true;
    document.addEventListener('nav:xml-selection-changed', event => {
      syncSelectedAttachmentCard(event.detail?.selectedXmlPath || '', true);
    });
  }

    /**
   * A <code>nextUiModelSectionNumber</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} shell a függvény shell bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function nextUiModelSectionNumber(shell){
    const numbers = Array.from(shell.querySelectorAll(':scope > .uimodel-section-card:not(.m2m-attachment-section) .uimodel-section-number'))
      .map(element => Number.parseInt(element.textContent || '', 10))
      .filter(Number.isFinite);
    return String((numbers.length ? Math.max(...numbers) : 0) + 1);
  }

    /**
   * Előkészíti és elindítja a create attachment section állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @returns {*} a feldolgozás eredménye
   */
function createAttachmentSection(){
    const formRoot = document.querySelector('#formContainer');
    if(!formRoot) return null;
    const uiShell = formRoot.querySelector(':scope > .uimodel-form-shell');
    const section = document.createElement('section');
    section.classList.add('collapsible-card', 'm2m-attachment-section');
    let content;

    if(uiShell){
      section.classList.add('uimodel-section-card');
      const header = document.createElement('button');
      header.type = 'button';
      header.className = 'uimodel-section-header collapse-toggle';
      header.setAttribute('aria-expanded', 'true');
      const number = document.createElement('span');
      number.className = 'uimodel-section-number';
      number.textContent = nextUiModelSectionNumber(uiShell);
      const title = document.createElement('h3');
      title.textContent = 'Csatolmányok';
      const chevron = document.createElement('span');
      chevron.className = 'collapse-chevron';
      chevron.setAttribute('aria-hidden', 'true');
      chevron.textContent = '▾';
      header.append(number, title, chevron);
      content = document.createElement('div');
      content.className = 'collapsible-content uimodel-section-content m2m-attachment-section-content';
      section.append(header, content);
      uiShell.append(section);
    }else{
      section.classList.add('form-section');
      const header = document.createElement('button');
      header.type = 'button';
      header.className = 'block-title collapse-toggle';
      header.setAttribute('aria-expanded', 'true');
      const title = document.createElement('span');
      title.textContent = 'Csatolmányok';
      const chevron = document.createElement('span');
      chevron.className = 'collapse-chevron';
      chevron.setAttribute('aria-hidden', 'true');
      chevron.textContent = '▾';
      header.append(title, chevron);
      content = document.createElement('div');
      content.className = 'form-section-content collapsible-content m2m-attachment-section-content';
      section.append(header, content);
      formRoot.append(section);
    }

    context.bindCollapseToggles?.(section);
    context.updateToggleAllFormCollapseButton?.();
    return content;
  }

    /**
   * Eltávolítja vagy alaphelyzetbe állítja a remove attachment presentation művelethez tartozó kliensoldali állapotot.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   */
function removeAttachmentPresentation(){
    document.querySelectorAll('.m2m-attachment-section, .m2m-attachment-cards')
      .forEach(element => element.remove());
  }

    /**
   * A <code>decorateAttachmentSections</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} actions a függvény actions bemeneti értéke
   */
function decorateAttachmentSections(actions = {}){
    const submissionId = state().submissionId;
    const { byName, byFileId } = attachmentMetadata();
    removeAttachmentPresentation();
    const nodes = collectNodes();
    if(!nodes.length){
      context.updateToggleAllFormCollapseButton?.();
      return;
    }
    const host = createAttachmentSection();
    if(!host) return;

    const container = document.createElement('div');
    container.className = 'm2m-attachment-cards';
    container.setAttribute('aria-label', 'Csatolmányok');

    nodes.forEach((attachment, index) => {
      const fileName = childValue(attachment, 'fileName');
      const xmlFileId = childValue(attachment, 'fileId');
      const xmlFileSize = childValue(attachment, 'fileSize');
      if(!fileName && !xmlFileId) return;
      const meta = byFileId.get(xmlFileId) || byName.get(normalizeFileName(fileName));
      const attachmentPath = attachmentNodePath(attachment);
      const card = document.createElement('section');
      card.className = 'm2m-attachment-card';
      card.dataset.attachmentIndex = String(index + 1);
      card.dataset.xmlPath = attachmentPath;
      card.tabIndex = 0;
      card.setAttribute('role', 'button');
      card.setAttribute('aria-label', `${fileName || 'Névtelen csatolmány'} kijelölése az XML-ben`);
      card.setAttribute('aria-selected', 'false');
      const lifecycle = resolveAttachmentLifecycle(meta, xmlFileId);
      const displaySize = meta?.fileSize ?? xmlFileSize;
      const localFile = (state().attachments || []).find(file =>
        normalizeFileName(file?.name) === normalizeFileName(fileName));
      const hasStoredAttachment = !!meta?.id && !!submissionId;
      const canPreview = !!actions.preview && (hasStoredAttachment || !!localFile);
      const isInitialUpload = lifecycle.state === 'NOT_UPLOADED' && !!localFile;
      const canRefresh = !!actions.refresh && (isInitialUpload || (hasStoredAttachment && lifecycle.refreshAllowed));
      const canDelete = !!actions.delete;
      const refreshLabel = isInitialUpload ? 'Csatolmány feltöltése' : 'Csatolmány megújítása';
      const refreshTitle = isInitialUpload
        ? 'A helyileg hozzáadott csatolmány feltöltése a NAV-hoz.'
        : (hasStoredAttachment
          ? (lifecycle.reason || 'A csatolmány ebben az állapotban nem újítható meg.')
          : 'A csatolmány még nem kapcsolódik M2M beküldési csomaghoz.');
      const showReason = lifecycle.state !== 'VALID' || !lifecycle.localFileAvailable;
      const reasonHtml = showReason && lifecycle.reason
        ? `<div class="m2m-attachment-status-reason ${lifecycle.stateClass}" role="status"><strong>Ok:</strong> ${escapeHtml(lifecycle.reason)}</div>`
        : '';
      card.innerHTML = `<div class="m2m-attachment-card-header"><div><span class="m2m-attachment-card-number">${index + 1}.</span> <strong>${escapeHtml(fileName || 'Névtelen csatolmány')}</strong><div class="m2m-attachment-card-meta">${escapeHtml(attachmentType(fileName))} · ${escapeHtml(formatFileSize(displaySize))}</div></div><span class="m2m-attachment-status-badge ${lifecycle.stateClass}">${escapeHtml(lifecycle.stateLabel)}</span></div>
        <dl class="m2m-attachment-card-details"><div><dt>NAV állapot</dt><dd>${escapeHtml(lifecycle.stateLabel)}</dd></div><div><dt>Feltöltve</dt><dd>${escapeHtml(formatDate(meta?.navUploadedAt))}</dd></div><div><dt>Érvényes eddig</dt><dd>${escapeHtml(formatDate(meta?.navExpiresAt))}</dd></div></dl>
        ${reasonHtml}
        <div class="m2m-attachment-inline-buttons">
          <button type="button" data-action="preview" ${!canPreview ? 'disabled' : ''}>Megtekintés</button>
          <button type="button" data-action="refresh" title="${escapeHtml(refreshTitle)}" ${!canRefresh ? 'disabled' : ''}>${escapeHtml(refreshLabel)}</button>
          <button type="button" data-action="delete" class="danger" ${!canDelete ? 'disabled' : ''}>Csatolmány törlése</button>
        </div>`;
      card.addEventListener('click', event => {
        const button = event.target.closest('button[data-action]');
        if(button){
          if(button.disabled) return;
          const action = button.dataset.action;
          if(action === 'preview') actions.preview?.(meta, attachment);
          if(action === 'refresh') actions.refresh?.(meta, attachment);
          if(action === 'delete') actions.delete?.(meta, attachment);
          return;
        }
        if(attachmentPath) context.selectXmlPath?.(attachmentPath);
      });
      card.addEventListener('keydown', event => {
        if(event.target.closest('button')) return;
        if((event.key === 'Enter' || event.key === ' ') && attachmentPath){
          event.preventDefault();
          context.selectXmlPath?.(attachmentPath);
        }
      });
      container.append(card);
    });
    if(container.childElementCount){
      host.append(container);
      installSelectionBridge();
      syncSelectedAttachmentCard(context.getSelection?.().selectedXmlPath || '', false);
    }else{
      host.closest('.m2m-attachment-section')?.remove();
    }
    context.updateToggleAllFormCollapseButton?.();
  }

  return {
    attachmentDefinitionPaths,
    formDefinitionHasAttachment1,
    xmlDocumentHasAttachment1,
    normalizeFileName,
    findFileName,
    findFileIdValue,
    setFileId,
    collectNodes,
    collectFileNames,
    findDuplicateFileName,
    addPlaceholder,
    addPlaceholders,
    findUploadedByFileName,
    applyUploadedIds,
    attachmentNodePath,
    decorateAttachmentSections
  };
}
