/**
 * @module m2m/m2m-submission-ui
 *
 * A M2M beküldési és csatolmánykezelési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A kliensoldali engedélyezés csak felhasználói visszajelzés; a tényleges M2M jogosultsági kontroll szerveroldali.
 */

import { m2mApi, ensureM2mAvailable, getM2mAvailability, loadM2mAvailability, showM2mConfigurationMissingDialog } from './m2m-api.js';
import { createM2mAttachmentService } from './m2m-attachments-ui.js';
import { showM2mLogsModal } from './m2m-http-trace-ui.js';
import { isMarkedForSubmission, isM2mSuccessfulTerminal } from './m2m-status-ui.js';
import { createM2mProgress, showM2mResult, showM2mOperationResult } from './m2m-progress-ui.js';
import { runCurrentFormXsdValidation } from '../validation/xsd-validation.js';
import { runCurrentFormXpathValidation } from '../validation/xpath-validation.js';
import { normalizeValidationSeverityLabel } from '../validation/validation-result-renderer.js';

let activeController = null;

/**
 * Előkészíti és elindítja a create m2m form controller állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} context a függvény context bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function createM2mFormController(context){
  const elements = context.elements || {};
  const attachments = createM2mAttachmentService(context);
    /**
   * Betölti vagy lekéri a get state művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
const getState = () => context.getState();
    /**
   * Szinkronizálja vagy frissíti a set state által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} partial a függvény partial bemeneti értéke
   */
const setState = partial => context.setState(partial || {});
  const operationMenus = {
    validation: {
      dropdown: document.getElementById('m2mValidationDropdown'),
      button: document.getElementById('m2mValidationMenuButton'),
      menu: document.getElementById('m2mValidationMenu')
    },
    calculation: {
      dropdown: document.getElementById('m2mCalculationDropdown'),
      button: document.getElementById('m2mCalculationMenuButton'),
      menu: document.getElementById('m2mCalculationMenu')
    }
  };
  let operationMenuEventsBound = false;

    /**
   * A <code>pendingFiles</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function pendingFiles(){
    return (getState().attachments || []).filter(file => {
      if(!file || typeof file.name !== 'string') return false;
      return typeof Blob === 'undefined' || file instanceof Blob;
    });
  }

    /**
   * A <code>waitForNextPaint</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function waitForNextPaint(){
    return new Promise(resolve => {
      const schedule = typeof window.requestAnimationFrame === 'function'
        ? window.requestAnimationFrame.bind(window)
        : callback => window.setTimeout(callback, 0);
      schedule(() => schedule(resolve));
    });
  }

    /**
   * Feloldja a selected attachment node eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} nodes a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
   * @returns {*} a feldolgozás eredménye
   */
function selectedAttachmentNode(nodes){
    const selectedPath = context.getSelection?.().selectedXmlPath || '';
    if(!selectedPath) return null;
    const canonicalSelected = context.xml?.canonicalizeXmlPath?.(selectedPath) || selectedPath;
    return (nodes || []).find(node => {
      const nodePath = attachments.attachmentNodePath(node);
      return !!nodePath && (canonicalSelected === nodePath || canonicalSelected.startsWith(`${nodePath}/`));
    }) || null;
  }

    /**
   * Kezeli vagy beköti a attachment requires preparation esemény- és inicializációs folyamatát.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} meta a függvény meta bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function attachmentRequiresPreparation(meta){
    const lifecycleState = String(meta?.lifecycleState || '').trim().toUpperCase();
    return meta?.refreshAllowed === true
      || ['NOT_UPLOADED', 'UNKNOWN', 'EXPIRED', 'EXPIRING_SOON'].includes(lifecycleState);
  }

    /**
   * Kezeli vagy beköti a attachment display name esemény- és inicializációs folyamatát.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} meta a függvény meta bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function attachmentDisplayName(meta){
    return String(meta?.originalFileName || meta?.fileName || 'Névtelen csatolmány');
  }

    /**
   * A <code>escapeResult</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function escapeResult(value){
    return value == null || value === '' ? '–' : String(value);
  }

    /**
   * A <code>operationSucceeded</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} status a kapcsolódó folyamat aktuális állapota
   * @param {*} resultCode a függvény resultCode bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function operationSucceeded(status, resultCode){
    const normalizedStatus=String(status || '').toUpperCase();
    const normalizedCode=String(resultCode || '').toUpperCase();
    if(['SIKERTELEN','TECHNIKAI_HIBA'].includes(normalizedStatus)) return false;
    return !normalizedCode.includes('HIBA') && !normalizedCode.includes('ERROR') && normalizedCode !== 'SIKERTELEN';
  }

    /**
   * Elrejti vagy lezárja a close operation menus felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} exceptKey a függvény exceptKey bemeneti értéke
   */
function closeOperationMenus(exceptKey = null){
    Object.entries(operationMenus).forEach(([key,item]) => {
      if(key === exceptKey || !item.menu || !item.button) return;
      item.menu.hidden = true;
      item.menu.style.left = '';
      item.menu.style.top = '';
      item.button.setAttribute('aria-expanded','false');
    });
  }

    /**
   * A <code>positionOperationMenu</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} item a függvény item bemeneti értéke
   */
function positionOperationMenu(item){
    if(!item?.menu || !item?.button || item.menu.hidden) return;
    const margin=10;
    const buttonRect=item.button.getBoundingClientRect();
    const menuRect=item.menu.getBoundingClientRect();
    const viewportWidth=window.innerWidth || document.documentElement.clientWidth;
    const viewportHeight=window.innerHeight || document.documentElement.clientHeight;
    let left=Math.max(margin, Math.min(buttonRect.left, viewportWidth-menuRect.width-margin));
    let top=buttonRect.bottom+8;
    if(top+menuRect.height>viewportHeight-margin && buttonRect.top>menuRect.height+margin){
      top=buttonRect.top-menuRect.height-8;
    }
    item.menu.style.left=`${Math.round(left)}px`;
    item.menu.style.top=`${Math.round(Math.max(margin,top))}px`;
    item.menu.style.right='auto';
  }

    /**
   * A <code>toggleOperationMenu</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} key a függvény key bemeneti értéke
   * @param {*} event a feldolgozandó böngészőesemény
   */
function toggleOperationMenu(key, event){
    event?.stopPropagation?.();
    if(getM2mAvailability().loaded && !getM2mAvailability().configured){ showM2mConfigurationMissingDialog(); return; }
    updateOperationMenuState();
    const item=operationMenus[key];
    if(!item?.menu || !item?.button || item.button.disabled) return;
    const willOpen=item.menu.hidden;
    closeOperationMenus(willOpen ? key : null);
    closeMenu();
    item.menu.hidden=!willOpen;
    item.button.setAttribute('aria-expanded',String(willOpen));
    if(willOpen) positionOperationMenu(item);
  }

    /**
   * Szinkronizálja vagy frissíti a update operation menu state által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   */
function updateOperationMenuState(){
    const state=getState();
    const hasXml=!!state.currentXmlDocument || !!String(context.serializeCurrentXml?.() || '').trim();
    const submission=state.m2mSubmission || {};
    const successfulTerminal=isM2mSuccessfulTerminal(submission);
    const m2mConfigured=getM2mAvailability().configured;
    Object.values(operationMenus).forEach(item => { if(item.button){ item.button.disabled=!hasXml || successfulTerminal; item.button.classList.toggle('m2m-unavailable',!m2mConfigured); item.button.setAttribute('aria-disabled',String(!m2mConfigured)); if(!m2mConfigured)item.button.title='Az M2M hitelesítési adatok nincsenek beállítva.'; } });
    const validationStatus=operationMenus.validation.menu?.querySelector('[data-m2m-validation-action="validation-status"]');
    const onlineValidation=operationMenus.validation.menu?.querySelector('[data-m2m-validation-action="online-validation"]');
    const onlineCalculation=operationMenus.calculation.menu?.querySelector('[data-m2m-calculation-action="online-calculation"]');
    const calculationResult=operationMenus.calculation.menu?.querySelector('[data-m2m-calculation-action="calculation-result"]');
    if(onlineValidation){
      onlineValidation.disabled=!hasXml || successfulTerminal;
      onlineValidation.title=hasXml ? '' : 'Nincs betöltött XML.';
    }
    if(validationStatus){
      validationStatus.disabled=!hasXml || !state.submissionId || !submission.navValidacioUgyAzonosito;
      validationStatus.title=validationStatus.disabled ? 'Nincs lekérdezhető online validációazonosító.' : '';
    }
    if(onlineCalculation){
      onlineCalculation.disabled=!hasXml || successfulTerminal;
      onlineCalculation.title=hasXml ? '' : 'Nincs betöltött XML.';
    }
    if(calculationResult){
      calculationResult.disabled=!hasXml || !state.submissionId || !submission.navKalkulacioUgyAzonosito;
      calculationResult.title=calculationResult.disabled ? 'Nincs lekérdezhető online kalkulációazonosító.' : '';
    }
  }

    /**
   * A <code>validationResultRows</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   * @param {*} data a függvény data bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function validationResultRows(data){
    return [
      ['Validációazonosító', data?.navValidacioUgyAzonosito],
      ['Validáció státusza', data?.navValidacioStatusz],
      ['Eredménykód', data?.navValidacioResultCode],
      ['Eredményüzenet', data?.navValidacioResultMessage],
      ['Validációs hibák', data?.navValidacioHibak ? 'Rendelkezésre áll (BZip2/Base64 tartalom)' : 'Nincs'],
      ['Validációs tanúsítvány', data?.navValidaciosTanusitvany ? 'Rendelkezésre áll' : 'Nincs'],
      ['Indítás időpontja', data?.navValidacioStartedAt],
      ['Befejezés időpontja', data?.navValidacioFinishedAt],
      ['Utolsó lekérdezés', data?.navValidacioLastCheckedAt],
      ['Message ID', data?.navValidacioMessageId],
      ['Correlation ID', data?.navValidacioCorrelationId]
    ];
  }


    /**
   * A <code>validationDrawerErrors</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   * @param {*} details a függvény details bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function validationDrawerErrors(details){
    return (details?.errors || []).map((item,index)=>({
      errorCode:item?.errorCode || `NAV-${index+1}`,
      message:item?.message || 'Ismeretlen validációs hiba',
      severity:item?.severity || 'HIBA',
      elem:item?.element || '',
      ruleId:item?.ruleId || '',
      path:item?.path || '',
      additionalInformation:item?.additionalInformation || ''
    }));
  }

    /**
   * Betölti vagy lekéri a load online validation errors művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function loadOnlineValidationErrors(){
    if(!getState().submissionId) throw new Error('Nincs M2M beküldési csomag.');
    return m2mApi.validationErrors(getState().submissionId);
  }

    /**
   * Megjeleníti vagy újrarendereli a show online validation errors in drawer állapotát a felhasználói felületen.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   * @param {*} details a függvény details bemeneti értéke
   */
function showOnlineValidationErrorsInDrawer(details){
    const errors=validationDrawerErrors(details);
    window.NavXpathValidationResultUi?.renderXpathValidationPopup?.({
      requestId:details?.validationId || details?.messageId || 'NAV-ONLINE',
      requestTimestampUtc:details?.lastCheckedAt || details?.finishedAt || details?.startedAt,
      formName:getState().m2mSubmission?.bizonylatTipus || getState().formName || 'NAV online validáció',
      formVersion:getState().m2mSubmission?.bizonylatVerzio || getState().formVersion || '',
      validatorStatus:'FINISHED',
      resultStatus:errors.length ? 'ERROR' : 'OK',
      errorCount:errors.length,
      resultAvailable:false
    },errors);
  }

    /**
   * Feldolgozza a format validation report bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} data a függvény data bemeneti értéke
   * @param {*} details a függvény details bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function formatValidationReport(data,details){
    const lines=[
      'NAV M2M online validáció ellenőrzési jelentés',
      '============================================',
      '',
      `Validációazonosító: ${details?.validationId || data?.navValidacioUgyAzonosito || '–'}`,
      `Validáció státusza: ${details?.validationStatus || data?.navValidacioStatusz || '–'}`,
      `Eredménykód: ${details?.resultCode || data?.navValidacioResultCode || '–'}`,
      `Eredményüzenet: ${details?.resultMessage || data?.navValidacioResultMessage || '–'}`,
      `Indítás időpontja: ${details?.startedAt || data?.navValidacioStartedAt || '–'}`,
      `Befejezés időpontja: ${details?.finishedAt || data?.navValidacioFinishedAt || '–'}`,
      `Utolsó lekérdezés: ${details?.lastCheckedAt || data?.navValidacioLastCheckedAt || '–'}`,
      `Message ID: ${details?.messageId || data?.navValidacioMessageId || '–'}`,
      `Correlation ID: ${details?.correlationId || data?.navValidacioCorrelationId || '–'}`,
      `Hibák száma: ${details?.errorCount ?? details?.errors?.length ?? 0}`,
      '',
      'Kibontott validációs hibák',
      '---------------------------'
    ];
    const errors=validationDrawerErrors(details);
    if(!errors.length) lines.push('Nincs validációs hiba.');
    errors.forEach((error,index)=>{
      lines.push('',`${index+1}. hiba`,`Kód: ${error.errorCode || '–'}`,`Súlyosság: ${normalizeValidationSeverityLabel(error.severity).label || '–'}`,`Üzenet: ${error.message || '–'}`,`Elem: ${error.elem || '–'}`,`Rule ID: ${error.ruleId || '–'}`,`XPath: ${error.path || '–'}`);
      if(error.additionalInformation) lines.push(`Egyéb információ: ${error.additionalInformation}`);
    });
    return lines.join('\r\n');
  }

    /**
   * A <code>downloadTextFile</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} content a függvény content bemeneti értéke
   * @param {*} fileName a feloldáshoz vagy megjelenítéshez használt név
   */
function downloadTextFile(content,fileName){
    const blob=new Blob([content],{type:'text/plain;charset=utf-8'});
    const url=URL.createObjectURL(blob);
    const link=document.createElement('a');
    link.href=url;
    link.download=fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

    /**
   * Megjeleníti vagy újrarendereli a show online validation result állapotát a felhasználói felületen.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   * @param {*} data a függvény data bemeneti értéke
   * @param {*} statusOnly a kapcsolódó folyamat aktuális állapota
   * @param {*} success a függvény success bemeneti értéke
   */
function showOnlineValidationResult(data,statusOnly,success){
    let cachedDetails=null;
        /**
     * Betölti vagy lekéri a load művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @returns {Promise<void>} a folyamat befejeződését jelző Promise
     */
const load=async()=> cachedDetails || (cachedDetails=await loadOnlineValidationErrors());
    showM2mOperationResult({
      title:statusOnly?'Validáció státusza':'Online validáció eredménye',
      success,
      rows:validationResultRows(data),
      rowActions:data?.navValidacioHibak ? {
        'Validációs hibák':async({close})=>{
          const details=await load();
          close();
          showOnlineValidationErrorsInDrawer(details);
        }
      } : {},
      footerActions:[{
        label:'Ellenőrzés mentése',
        className:'secondary',
        handler:async()=>{
          const details=data?.navValidacioHibak ? await load() : {errors:[],errorCount:0};
          const validationId=String(details?.validationId || data?.navValidacioUgyAzonosito || 'online-validacio').replace(/[^a-zA-Z0-9._-]+/g,'_');
          downloadTextFile(formatValidationReport(data,details),`online-validacio-${validationId}.txt`);
        }
      }]
    });
  }

    /**
   * A <code>calculationResultRows</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} data a függvény data bemeneti értéke
   * @param {*} xmlApplied a feldolgozandó XML-tartalom vagy XML DOM-objektum
   * @returns {*} a feldolgozás eredménye
   */
function calculationResultRows(data, xmlApplied){
    return [
      ['Kalkulációazonosító', data?.navKalkulacioUgyAzonosito],
      ['Kalkuláció státusza', data?.navKalkulacioStatusz],
      ['Eredménykód', data?.navKalkulacioResultCode],
      ['Eredményüzenet', data?.navKalkulacioResultMessage],
      ['Kalkulációs hibakód', data?.navKalkulacioHibaKod],
      ['Kalkulációs hiba', data?.navKalkulacioHibaUzenet],
      ['Érintett mező', data?.navKalkulacioMezoAzonosito],
      ['Szabályazonosító', data?.navKalkulacioSzabalyAzonosito],
      ['Tömörítés', data?.navKalkulacioTomorites || 'Nincs'],
      ['Kalkulált XML', data?.navKalkulaltXml ? (xmlApplied ? 'Betöltve és elmentve az aktuális XML helyére' : 'Rendelkezésre áll') : 'Még nem áll rendelkezésre'],
      ['Indítás időpontja', data?.navKalkulacioStartedAt],
      ['Befejezés időpontja', data?.navKalkulacioFinishedAt],
      ['Utolsó lekérdezés', data?.navKalkulacioLastCheckedAt],
      ['Message ID', data?.navKalkulacioMessageId],
      ['Correlation ID', data?.navKalkulacioCorrelationId]
    ];
  }

    /**
   * Szinkronizálja vagy frissíti a apply calculated xml által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} data a függvény data bemeneti értéke
   * @param {*} progress a függvény progress bemeneti értéke
   * @param {*} phaseIndex az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function applyCalculatedXml(data, progress, phaseIndex){
    const xml=String(data?.navKalkulaltXml || '');
    if(!xml.trim()) return false;
    if(context.isLargeXmlMode?.()){
      throw new Error('A kalkulált teljes XML automatikus visszatöltése nagy XML módban nem támogatott.');
    }
    progress.update(phaseIndex, 'A kalkulált XML betöltése és a teljes űrlapstruktúra újraépítése.');
    context.replaceCurrentXmlFromText?.(xml, {showMessage:false,renderForm:false});
    if(typeof context.rebuildCurrentFormFromXmlText !== 'function'){
      throw new Error('A kalkulált XML teljes űrlap-újraépítési funkciója nem érhető el.');
    }
    await context.rebuildCurrentFormFromXmlText(xml, {suppressStatusMessage:true});
    context.markFormDirty?.();
    if(context.hasActiveEditableXmlForServerSave?.()){
      const saved=await context.quickSaveCurrentXmlFile?.({quiet:true,allowDisabledButton:true,skipIfClean:false});
      if(saved !== true) throw new Error('A kalkulált XML betöltődött, de a szerver oldali gyorsmentés nem sikerült.');
    }
    const synchronized=await createOrUpdateSubmission();
    setState({m2mSubmission:synchronized || data});
    context.renderXmlFromCurrentState?.({force:true});
    context.persistUiState?.();
    updateMenuState();
    return true;
  }

    /**
   * Elindítja a run offline validation aszinkron vagy több lépéses frontend folyamatát.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function runOfflineValidation(){
    const phases=['Aktuális XML mentése','XSD validáció','XPath validáció','Eredmények összesítése'];
    const progress=createM2mProgress('Offline validáció',phases);
    try{
      progress.update(0,'Az aktuális XML mentése és az XSD ellenőrzés előkészítése.');
      await waitForNextPaint();
      progress.update(1,'XSD validáció futtatása.');
      await runCurrentFormXsdValidation({openDrawerOnResult:false});
      progress.update(2,'XPath validáció futtatása az XSD ellenőrzés után.');
      const xpathResult=await runCurrentFormXpathValidation();
      if(!xpathResult) throw new Error('Az XPath validáció nem adott vissza feldolgozható eredményt.');
      const errorCount=Number(xpathResult?.status?.errorCount ?? xpathResult?.errors?.length ?? 0);
      progress.update(3,'Az offline validáció eredményeinek összesítése.');
      progress.close();
      showM2mOperationResult({
        title:errorCount>0?'Offline validáció hibákkal zárult':'Offline validáció befejeződött',
        success:errorCount===0,
        summary:'Az XSD validáció, majd az XPath validáció egymás után lefutott. A részletek az XSD és XPath eredménypaneleken láthatók.',
        rows:[['XSD ellenőrzés','Lefutott'],['XPath státusz',escapeResult(xpathResult?.status?.validatorStatus)],['XPath hibák száma',errorCount]]
      });
    }catch(error){
      progress.close();
      throw error;
    }
  }

    /**
   * Elindítja a run online validation aszinkron vagy több lépéses frontend folyamatát.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   * @param {*} statusOnly a kapcsolódó folyamat aktuális állapota
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function runOnlineValidation(statusOnly=false){
    const phases=statusOnly
      ? ['Beküldési csomag ellenőrzése','Validációazonosító ellenőrzése','NAV validációs státusz lekérdezése','Eredmény mentése']
      : ['Aktuális XML mentése','Beküldési csomag előkészítése','Token, nonce és aláírás előkészítése','NAV online validáció','Eredmény mentése'];
    const progress=createM2mProgress(statusOnly?'Validáció státusza':'Online validáció',phases);
    try{
      let data;
      if(statusOnly){
        progress.update(0,'A beküldési csomag és a korábbi validáció adatainak betöltése.');
        if(!getState().submissionId) throw new Error('Nincs M2M beküldési csomag.');
        progress.update(1,'A validációazonosító ellenőrzése.');
        if(!getState().m2mSubmission?.navValidacioUgyAzonosito) throw new Error('Nincs lekérdezhető online validációazonosító.');
        progress.update(2,'GET Validacio/{ugyAzonosito} hívás a NAV felé.');
        data=await m2mApi.validationStatus(getState().submissionId);
      }else{
        progress.update(0,'Az aktuális XML mentése.');
        data=await createOrUpdateSubmission();
        progress.update(1,'A Bizonylat API beküldési csomag előkészítése.');
        progress.update(2,'Token, nonce, payload hash és aláírás előkészítése.');
        progress.update(3,'POST Validacio hívás a NAV felé.');
        data=await m2mApi.onlineValidation(getState().submissionId);
      }
      progress.update(phases.length-1,'A validáció mezőszintű eredményének adatbázisba mentése.');
      setState({m2mSubmission:data || getState().m2mSubmission});
      updateOperationMenuState();
      progress.close();
      const success=operationSucceeded(data?.navValidacioStatusz,data?.navValidacioResultCode);
      showOnlineValidationResult(data,statusOnly,success);
    }catch(error){
      progress.close();
      throw error;
    }
  }

    /**
   * Elindítja a run online calculation aszinkron vagy több lépéses frontend folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} resultOnly a függvény resultOnly bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function runOnlineCalculation(resultOnly=false){
    const phases=resultOnly
      ? ['Beküldési csomag ellenőrzése','Kalkulációazonosító ellenőrzése','NAV kalkulációs eredmény lekérdezése','Kalkulált XML feldolgozása','XML mentése','Eredmény mentése']
      : ['Aktuális XML mentése','Beküldési csomag előkészítése','Token, nonce és aláírás előkészítése','NAV online kalkuláció','Kalkulált XML feldolgozása','XML mentése','Eredmény mentése'];
    const progress=createM2mProgress(resultOnly?'Online kalkuláció eredménye':'Online kalkuláció',phases);
    try{
      let data;
      if(resultOnly){
        progress.update(0,'A beküldési csomag és a korábbi kalkuláció adatainak betöltése.');
        if(!getState().submissionId) throw new Error('Nincs M2M beküldési csomag.');
        progress.update(1,'A kalkulációazonosító ellenőrzése.');
        if(!getState().m2mSubmission?.navKalkulacioUgyAzonosito) throw new Error('Nincs lekérdezhető online kalkulációazonosító.');
        progress.update(2,'GET Kalkulacio/{ugyAzonosito} hívás a NAV felé.');
        data=await m2mApi.calculationResult(getState().submissionId);
      }else{
        progress.update(0,'Az aktuális XML mentése.');
        data=await createOrUpdateSubmission();
        progress.update(1,'A Bizonylat API beküldési csomag előkészítése.');
        progress.update(2,'Token, nonce, payload hash és aláírás előkészítése.');
        progress.update(3,'POST Kalkulacio hívás a NAV felé. A NAV legfeljebb 30 másodpercig adhat szinkron eredményt.');
        data=await m2mApi.onlineCalculation(getState().submissionId);
      }
      setState({m2mSubmission:data || getState().m2mSubmission});
      const xmlPhase=resultOnly?3:4;
      let xmlApplied=false;
      const calculationSuccessful=String(data?.navKalkulacioStatusz || '').toUpperCase()==='SIKERES'
        && String(data?.navKalkulacioResultCode || '').toUpperCase()==='SIKERES';
      if(data?.navKalkulaltXml && calculationSuccessful){
        xmlApplied=await applyCalculatedXml(data,progress,xmlPhase);
        data=getState().m2mSubmission || data;
      }else{
        const calculationStatus=String(data?.navKalkulacioStatusz || '').toUpperCase();
        progress.update(xmlPhase, data?.navKalkulaltXml && !calculationSuccessful
          ? 'A NAV válasz tartalmazott XML-t, de a kalkuláció nem SIKERES állapotú, ezért az XML nem került betöltésre.'
          : calculationStatus==='FOLYAMATBAN'
            ? 'A kalkuláció folyamatban van; a kitöltött XML később az eredmény lekérdezésével tölthető be.'
            : 'A NAV válasz nem tartalmazott kalkulált XML-t.');
      }
      progress.update(resultOnly?4:5,xmlApplied?'A kalkulált XML gyorsmentése befejeződött.':'Nincs mentendő kalkulált XML.');
      progress.update(phases.length-1,'A kalkuláció mezőszintű eredményének adatbázisba mentése.');
      updateOperationMenuState();
      progress.close();
      const success=operationSucceeded(data?.navKalkulacioStatusz,data?.navKalkulacioResultCode);
      showM2mOperationResult({title:resultOnly?'Online kalkuláció eredménye':'Online kalkuláció eredménye',success,rows:calculationResultRows(data,xmlApplied)});
    }catch(error){
      progress.close();
      throw error;
    }
  }

    /**
   * Kezeli vagy beköti a handle validation action esemény- és inicializációs folyamatát.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   * @param {*} action a függvény action bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function handleValidationAction(action){
    closeOperationMenus();
    try{
      if(action==='offline-validation') await runOfflineValidation();
      if(action==='online-validation') await runOnlineValidation(false);
      if(action==='validation-status') await runOnlineValidation(true);
    }catch(error){
      console.error('M2M validation action error',error);
      context.showMessage?.(error.message || 'A validációs művelet nem sikerült.','error');
    }finally{
      updateOperationMenuState();
    }
  }

    /**
   * Kezeli vagy beköti a handle calculation action esemény- és inicializációs folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} action a függvény action bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function handleCalculationAction(action){
    closeOperationMenus();
    try{
      if(action==='online-calculation') await runOnlineCalculation(false);
      if(action==='calculation-result') await runOnlineCalculation(true);
    }catch(error){
      console.error('M2M calculation action error',error);
      context.showMessage?.(error.message || 'A kalkulációs művelet nem sikerült.','error');
    }finally{
      updateOperationMenuState();
    }
  }

    /**
   * Kezeli vagy beköti a bind operation menu events esemény- és inicializációs folyamatát.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   */
function bindOperationMenuEvents(){
    if(operationMenuEventsBound) return;
    operationMenuEventsBound=true;
    operationMenus.validation.button?.addEventListener('click',event=>toggleOperationMenu('validation',event));
    operationMenus.calculation.button?.addEventListener('click',event=>toggleOperationMenu('calculation',event));
    operationMenus.validation.menu?.querySelectorAll('[data-m2m-validation-action]').forEach(button=>button.addEventListener('click',()=>handleValidationAction(button.dataset.m2mValidationAction)));
    operationMenus.calculation.menu?.querySelectorAll('[data-m2m-calculation-action]').forEach(button=>button.addEventListener('click',()=>handleCalculationAction(button.dataset.m2mCalculationAction)));
    document.addEventListener('click',event=>{
      if(!event.target.closest('#m2mValidationDropdown') && !event.target.closest('#m2mCalculationDropdown')) closeOperationMenus();
    });
    document.addEventListener('keydown',event=>{ if(event.key==='Escape') closeOperationMenus(); });
    window.addEventListener('resize',()=>Object.values(operationMenus).forEach(positionOperationMenu));
    window.addEventListener('scroll',()=>Object.values(operationMenus).forEach(positionOperationMenu),true);
  }

    /**
   * A <code>positionMenu</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   */
function positionMenu(){
    const menu = elements.menu;
    const button = elements.menuButton;
    if(!menu || !button || menu.hidden) return;
    const margin = 10;
    const buttonRect = button.getBoundingClientRect();
    const menuRect = menu.getBoundingClientRect();
    const viewportWidth = window.innerWidth || document.documentElement.clientWidth;
    const viewportHeight = window.innerHeight || document.documentElement.clientHeight;
    let left = buttonRect.right - menuRect.width;
    left = Math.max(margin, Math.min(left, viewportWidth - menuRect.width - margin));
    const spaceBelow = viewportHeight - buttonRect.bottom;
    const spaceAbove = buttonRect.top;
    let top = spaceBelow < menuRect.height + margin && spaceAbove > spaceBelow
      ? buttonRect.top - menuRect.height - 8
      : buttonRect.bottom + 8;
    top = Math.max(margin, Math.min(top, viewportHeight - menuRect.height - margin));
    menu.style.left = `${Math.round(left)}px`;
    menu.style.top = `${Math.round(top)}px`;
    menu.style.right = 'auto';
  }

    /**
   * A <code>toggleMenu</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   */
function toggleMenu(event){
    event?.stopPropagation?.();
    if(getM2mAvailability().loaded && !getM2mAvailability().configured){ showM2mConfigurationMissingDialog(); return; }
    const menu = elements.menu;
    const button = elements.menuButton;
    if(!menu || !button) return;
    updateMenuState();
    const willOpen = menu.hidden;
    if(willOpen) closeOperationMenus();
    menu.hidden = !willOpen;
    button.setAttribute('aria-expanded', String(willOpen));
    if(willOpen) positionMenu();
  }

    /**
   * Elrejti vagy lezárja a close menu felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   */
function closeMenu(){
    const menu = elements.menu;
    const button = elements.menuButton;
    if(!menu || !button) return;
    menu.hidden = true;
    menu.style.left = '';
    menu.style.top = '';
    menu.style.right = '';
    button.setAttribute('aria-expanded', 'false');
  }

    /**
   * A <code>previewAttachment</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} meta a függvény meta bemeneti értéke
   */
function previewAttachment(meta, attachmentNode){
    const id = getState().submissionId;
    if(id && meta?.id){
      window.open(m2mApi.attachmentContentUrl(id, meta.id, false), '_blank', 'noopener');
      return;
    }

    const fileName = attachments.findFileName(attachmentNode);
    const file = pendingFiles().find(item =>
      attachments.normalizeFileName(item.name) === attachments.normalizeFileName(fileName));
    if(!file){
      context.showMessage?.('A helyi csatolmány már nem érhető el megtekintésre.', 'error');
      return;
    }

    const objectUrl = URL.createObjectURL(file);
    const previewWindow = window.open(objectUrl, '_blank', 'noopener');
    if(!previewWindow){
      URL.revokeObjectURL(objectUrl);
      context.showMessage?.('A böngésző letiltotta a csatolmány megnyitását. Engedélyezze a felugró ablakot.', 'error');
      return;
    }
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
  }

    /**
   * A <code>confirmAttachmentDeletion</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} fileName a feloldáshoz vagy megjelenítéshez használt név
   * @returns {*} a feldolgozás eredménye
   */
function confirmAttachmentDeletion(fileName){
    return new Promise(resolve => {
      const backdrop = document.createElement('div');
      backdrop.className = 'm2m-confirm-backdrop';
      backdrop.innerHTML = `<section class="m2m-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="m2mDeleteAttachmentTitle">
        <h3 id="m2mDeleteAttachmentTitle">Csatolmány törlése</h3>
        <p>Biztosan törli a(z) <strong>${String(fileName || '').replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;')}</strong> csatolmányt?</p>
        <p>A művelet eltávolítja a csatolmányt az XML-ből és a helyi csatolmánytárból.</p>
        <div class="m2m-confirm-actions"><button type="button" data-result="cancel">Mégse</button><button type="button" class="danger" data-result="delete">Csatolmány törlése</button></div>
      </section>`;
            /**
       * Elrejti vagy lezárja a close felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
       *
       * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
       * @param {*} result a függvény result bemeneti értéke
       */
const close = result => { backdrop.remove(); resolve(result); };
      backdrop.addEventListener('click', event => {
        const button = event.target.closest('button[data-result]');
        if(button) close(button.dataset.result === 'delete');
      });
      backdrop.addEventListener('keydown', event => {
        if(event.key === 'Escape') close(false);
      });
      document.body.append(backdrop);
      backdrop.querySelector('[data-result="cancel"]')?.focus();
    });
  }

    /**
   * A <code>chooseAttachmentForDeletion</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} nodes a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
   * @returns {*} a feldolgozás eredménye
   */
function chooseAttachmentForDeletion(nodes){
    const selected = selectedAttachmentNode(nodes);
    if(selected || nodes.length === 1){
      const node = selected || nodes[0];
      return confirmAttachmentDeletion(attachments.findFileName(node)).then(confirmed => confirmed ? node : null);
    }

    return new Promise(resolve => {
      const backdrop = document.createElement('div');
      backdrop.className = 'm2m-confirm-backdrop';
      const options = nodes.map((node, index) => {
        const name = attachments.findFileName(node) || `Csatolmány ${index + 1}`;
        const escaped = String(name).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
        return `<label class="m2m-confirm-choice"><input type="radio" name="m2m-delete-attachment" value="${index}" ${index === 0 ? 'checked' : ''}><span>${escaped}</span></label>`;
      }).join('');
      backdrop.innerHTML = `<section class="m2m-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="m2mChooseAttachmentTitle">
        <h3 id="m2mChooseAttachmentTitle">Csatolmány törlése</h3>
        <p>Válassza ki a törlendő csatolmányt. A művelet eltávolítja az XML-ből és a helyi csatolmánytárból is.</p>
        <div class="m2m-confirm-choice-list">${options}</div>
        <div class="m2m-confirm-actions"><button type="button" data-result="cancel">Mégse</button><button type="button" class="danger" data-result="delete">Csatolmány törlése</button></div>
      </section>`;
            /**
       * Elrejti vagy lezárja a close felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
       *
       * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
       * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
       */
const close = node => { backdrop.remove(); resolve(node); };
      backdrop.addEventListener('click', event => {
        const button = event.target.closest('button[data-result]');
        if(!button) return;
        if(button.dataset.result !== 'delete'){
          close(null);
          return;
        }
        const selectedIndex = Number.parseInt(backdrop.querySelector('input[name="m2m-delete-attachment"]:checked')?.value || '-1', 10);
        close(Number.isInteger(selectedIndex) && selectedIndex >= 0 ? nodes[selectedIndex] : null);
      });
      backdrop.addEventListener('keydown', event => {
        if(event.key === 'Escape') close(null);
      });
      document.body.append(backdrop);
      backdrop.querySelector('input[name="m2m-delete-attachment"]')?.focus();
    });
  }

    /**
   * A <code>performAttachmentDeletion</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} meta a függvény meta bemeneti értéke
   * @param {*} attachmentNode a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
   * @param {*} options a művelet opcionális beállításai
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function performAttachmentDeletion(meta, attachmentNode, options = {}){
    const id = getState().submissionId;
    const fileName = meta?.originalFileName || attachments.findFileName(attachmentNode) || 'Névtelen csatolmány';
    if(options.confirmed !== true && !await confirmAttachmentDeletion(fileName)) return false;

    const phases = [
      'Csatolmány ellenőrzése',
      'Adatbázis- és fájlkapcsolat törlése',
      'Attachment_1 elem eltávolítása az XML-ből',
      'XML mentése',
      'Felület frissítése'
    ];
    const progress = createM2mProgress('Csatolmány törlése', phases);
    try{
      progress.update(0, `Csatolmány ellenőrzése: ${fileName}`);
      await waitForNextPaint();

      let data = getState().m2mSubmission || null;
      if(meta?.id && id){
        progress.update(1, `Helyi fájl és adatbázisrekord törlése: ${fileName}`);
        data = await m2mApi.deleteAttachment(id, meta.id);
        setState({m2mSubmission:data || null});
      }else{
        progress.update(1, 'Nincs adatbázisban tárolt csatolmányrekord; csak az XML hivatkozása törlődik.');
      }

      progress.update(2, `Attachment_1 eltávolítása: ${fileName}`);
      attachmentNode?.parentNode?.removeChild(attachmentNode);
      const selectedKey = attachments.normalizeFileName(fileName);
      const remainingFiles = pendingFiles().filter(file => attachments.normalizeFileName(file.name) !== selectedKey);
      setState({attachments:remainingFiles, markedForSubmit:false});

      progress.update(3, 'A módosított XML mentése.');
      if(id){
        const saved = await createOrUpdateSubmission();
        setState({m2mSubmission:saved || data || null});
      }

      progress.update(4, 'Csatolmánykártyák és XML-nézet frissítése.');
      context.renderXmlFromCurrentState?.();
      context.markFormDirty?.();
      context.persistUiState?.();
      updateMenuState();
      progress.close();
      context.showMessage?.(`Csatolmány törölve: ${fileName}`, 'success');
      return true;
    }catch(error){
      progress.close();
      throw error;
    }
  }

    /**
   * Eltávolítja vagy alaphelyzetbe állítja a delete card attachment művelethez tartozó kliensoldali állapotot.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} meta a függvény meta bemeneti értéke
   * @param {*} attachmentNode a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function deleteCardAttachment(meta, attachmentNode){
    return performAttachmentDeletion(meta, attachmentNode);
  }

    /**
   * Feltölti a még NAV fileId-val nem rendelkező, helyileg hozzáadott csatolmányt.
   *
   * @param {*} attachmentNode az XML Attachment_1 csomópontja
   * @returns {Promise<void>} a feltöltés befejeződését jelző Promise
   */
async function uploadSingleAttachment(attachmentNode){
    const fileName = attachments.findFileName(attachmentNode);
    const normalizedName = attachments.normalizeFileName(fileName);
    const file = pendingFiles().find(item => attachments.normalizeFileName(item.name) === normalizedName);
    if(!file) throw new Error('A feltöltendő helyi csatolmány nem található. Adja hozzá újra a fájlt.');

    const progress = createM2mProgress('Csatolmány feltöltése', [
      'Csatolmány ellenőrzése',
      'M2M beküldési csomag előkészítése',
      'Helyi csatolmány mentése',
      'Csatolmány feltöltése a NAV-hoz',
      'NAV fileId feldolgozása',
      'XML frissítése és mentése'
    ]);
    try{
      progress.update(0, `Csatolmány ellenőrzése: ${file.name}`);
      if(!getState().submissionId){
        progress.update(1, 'M2M beküldési csomag létrehozása.');
        await createOrUpdateSubmission({includePendingAttachments:false});
      }else{
        progress.update(1, 'Meglévő M2M beküldési csomag használata.');
      }

      progress.update(2, `Helyi csatolmány mentése: ${file.name}`);
      await storePendingAttachments([file]);

      progress.update(3, `Csatolmány feltöltése a NAV-hoz: ${file.name}`);
      const data = await postStep('/step/upload-attachments');

      progress.update(4, 'A visszakapott NAV fileId XML-be írása.');
      const result = attachments.applyUploadedIds(data?.uploadedAttachments || [], {
        onlyFileNames:[file.name],
        updateExistingFileIds:false
      });
      if(result.missing.length || result.updated !== 1){
        throw new Error('A csatolmány feltöltése nem adott vissza egyértelműen hozzárendelhető NAV fileId értéket.');
      }

      const remainingFiles = pendingFiles().filter(item =>
        attachments.normalizeFileName(item.name) !== normalizedName);
      setState({attachments:remainingFiles, m2mSubmission:data || getState().m2mSubmission || null, markedForSubmit:false});

      progress.update(5, 'A NAV fileId-val frissített XML mentése.');
      const saved = await createOrUpdateSubmission();
      setState({m2mSubmission:saved || data || getState().m2mSubmission || null});
      context.renderXmlFromCurrentState?.();
      context.markFormDirty?.();
      context.persistUiState?.();
      updateMenuState();
      progress.close();
      context.showMessage?.(`A csatolmány feltöltése sikeres: ${file.name}`, 'success');
    }catch(error){
      progress.close();
      throw error;
    }
  }

  /**
   * A kártya feltöltés/megújítás műveletét az aktuális csatolmányállapot szerint irányítja.
   *
   * @param {*} meta a szerveroldali csatolmány metaadata
   * @param {*} attachmentNode az XML Attachment_1 csomópontja
   * @returns {Promise<void>} a művelet befejeződését jelző Promise
   */
async function refreshCardAttachment(meta, attachmentNode){
    if(meta?.id){
      await refreshSingleAttachment(meta);
      return;
    }
    await uploadSingleAttachment(attachmentNode);
  }

      /**
   * Megújítja a már M2M beküldési csomaghoz kapcsolt csatolmány NAV feltöltését.
   *
   * @param {*} meta a szerveroldali csatolmány metaadata
   * @returns {Promise<void>} a művelet befejeződését jelző Promise
   */
async function refreshSingleAttachment(meta){
    const id=getState().submissionId;
    if(!id || !meta?.id) throw new Error('A csatolmány még nem kapcsolódik M2M beküldési csomaghoz.');
    const progress=createM2mProgress('Csatolmány megújítása', ['Csatolmány ellenőrzése','Token és nonce előkészítése','Csatolmány újrafeltöltése','Új NAV azonosító feldolgozása','XML frissítése és mentése']);
    try{
      progress.update(0); progress.update(1);
      progress.update(2); const data=await m2mApi.refreshAttachment(id, meta.id);
      progress.update(3); const refreshed=(data?.uploadedAttachments||[]).find(item=>item.id===meta.id);
      if(!refreshed?.navFileId) throw new Error('Az újrafeltöltés nem adott vissza új NAV fileId értéket.');
      const applied=attachments.applyUploadedIds(data.uploadedAttachments||[], {onlyFileNames:[meta.originalFileName], updateExistingFileIds:true});
      if(applied.updated!==1) throw new Error('Az új NAV fileId nem köthető egyértelműen az XML megfelelő Attachment_1 eleméhez.');
      progress.update(4); const saved=await createOrUpdateSubmission();
      setState({m2mSubmission:saved||data}); context.renderXmlFromCurrentState?.(); context.persistUiState?.();
      progress.close(); context.showMessage?.('A csatolmány megújítása sikeres, az XML új NAV azonosítóval frissítve és elmentve.', 'success'); updateMenuState();
    }catch(error){ progress.close(); throw error; }
  }

    /**
   * Szinkronizálja vagy frissíti a update menu state által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   */
function updateMenuState(){
    updateOperationMenuState();
    const current = getState();
    const successfulTerminal = isM2mSuccessfulTerminal(current.m2mSubmission);
    window.setTimeout(() => attachments.decorateAttachmentSections({
      preview:previewAttachment,
      refresh:successfulTerminal ? null : refreshCardAttachment,
      delete:successfulTerminal ? null : deleteCardAttachment
    }), 0);
    const button = elements.menuButton;
    if(!button) return;
    const hasXml = !!current.currentXmlDocument || !!String(context.serializeCurrentXml?.() || '').trim();
    const supportsAttachmentByXsd = attachments.formDefinitionHasAttachment1(current.currentFormDefinition);
    const currentAttachments = pendingFiles();
    button.disabled = !hasXml;
    const m2mConfigured=getM2mAvailability().configured;
    button.classList.toggle('m2m-unavailable',!m2mConfigured);
    button.setAttribute('aria-disabled',String(!m2mConfigured));
    if(!m2mConfigured) button.title='Az M2M hitelesítési adatok nincsenek beállítva.';
    if(!elements.menu) return;
    const xmlAttachmentNames = attachments.collectFileNames();
    elements.menu.querySelectorAll('[data-m2m-form-action]').forEach(actionButton => {
      const action = actionButton.dataset.m2mFormAction;
      let disabled = !hasXml || (successfulTerminal && action !== 'show-m2m-logs');
      if(['add-attachment', 'add-and-fetch-attachment-info', 'fetch-attachment-info'].includes(action)){
        disabled = disabled || !supportsAttachmentByXsd;
      }
      if(action === 'fetch-attachment-info') disabled = disabled || !currentAttachments.length;
      if(action === 'delete-attachment') disabled = disabled || !xmlAttachmentNames.length;
      if(action === 'withdraw-submit-mark') disabled = disabled || !current.markedForSubmit || !current.submissionId;
      if(action === 'show-m2m-logs') disabled = disabled || !current.submissionId;
      if(action === 'submit-document') disabled = disabled || !current.markedForSubmit;
      actionButton.disabled = disabled;
      if(['add-attachment', 'add-and-fetch-attachment-info'].includes(action) && !supportsAttachmentByXsd){
        actionButton.title = 'Csak olyan XML-nél aktív, amelynek XSD-jében szerepel az Attachment_1 elem.';
      }else if(action === 'fetch-attachment-info' && !currentAttachments.length){
        actionButton.title = 'Előbb adj hozzá csatolmányt.';
      }else if(action === 'delete-attachment' && !xmlAttachmentNames.length){
        actionButton.title = 'Nincs törölhető csatolmány az XML-ben.';
      }else if(action === 'withdraw-submit-mark' && !current.markedForSubmit){
        actionButton.title = 'Csak beküldésre megjelölt XML jelölése vonható vissza.';
      }else if(action === 'show-m2m-logs' && !current.submissionId){
        actionButton.title = 'Még nincs M2M beküldési csomag.';
      }else if(action === 'submit-document' && !current.markedForSubmit){
        actionButton.title = 'A beküldés csak akkor aktív, ha előtte megjelölted az XML-t beküldésre.';
      }else{
        actionButton.removeAttribute('title');
      }
    });
  }

    /**
   * A <code>currentXmlAsFile</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} fileName a feloldáshoz vagy megjelenítéshez használt név
   * @returns {*} a feldolgozás eredménye
   */
function currentXmlAsFile(fileName = 'schema-explorer-current.xml'){
    const xml = String(context.serializeCurrentXml?.() || '');
    if(!xml.trim()) throw new Error('Nincs XML tartalom.');
    return new File([xml], fileName, { type: 'application/xml' });
  }


    /**
   * Előkészíti és elindítja a create or update submission állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function createOrUpdateSubmission(options = {}){
    const current = getState();
    const includePendingAttachments = options.includePendingAttachments !== false;
    const fileName = context.getActiveXmlDisplayFileName?.() || 'schema-explorer-current.xml';
    if(current.submissionId){
      const formData = new FormData();
      formData.append('xml', currentXmlAsFile(fileName));
      const xmlFileId = new URLSearchParams(window.location.search).get('xmlFileId');
      if(xmlFileId) formData.append('xmlFileId', xmlFileId);
      const data = await m2mApi.updateSubmissionXml(current.submissionId, formData);
      setState({
        interfaceType: data?.interfaceType || current.interfaceType || 'BIZONYLAT_API',
        markedForSubmit: isMarkedForSubmission(data) || current.markedForSubmit,
        m2mSubmission: data || current.m2mSubmission || null
      });
      return data || {};
    }
    const formData = new FormData();
    formData.append('gatewayMode', 'REAL');
    formData.append('compression', 'NONE');
    formData.append('submitNow', 'false');
    const xmlFileId = new URLSearchParams(window.location.search).get('xmlFileId');
    if(xmlFileId) formData.append('xmlFileId', xmlFileId);
    formData.append('xml', currentXmlAsFile(fileName));
    if(includePendingAttachments){
      pendingFiles().forEach(file => formData.append('attachments', file, file.name));
    }
    const data = await m2mApi.createSubmission(formData);
    setState({
      submissionId: data?.id || null,
      interfaceType: data?.interfaceType || 'BIZONYLAT_API',
      markedForSubmit: isMarkedForSubmission(data),
      m2mSubmission: data || null
    });
    return data || {};
  }

    /**
   * A <code>storePendingAttachments</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} files a függvény files bemeneti értéke
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function storePendingAttachments(files){
    const selectedFiles = (files || []).filter(file => file && (typeof Blob === 'undefined' || file instanceof Blob));
    if(!selectedFiles.length) return getState().m2mSubmission || {};

    if(!getState().submissionId){
      return createOrUpdateSubmission();
    }

    const storedNames = new Set((getState().m2mSubmission?.uploadedAttachments || [])
      .map(item => attachments.normalizeFileName(item?.originalFileName || item?.fileName))
      .filter(Boolean));
    const filesToStore = selectedFiles.filter(file => !storedNames.has(attachments.normalizeFileName(file.name)));
    await createOrUpdateSubmission();
    if(!filesToStore.length) return getState().m2mSubmission || {};

    const formData = new FormData();
    filesToStore.forEach(file => formData.append('attachments', file, file.name));
    const data = await m2mApi.addAttachments(getState().submissionId, formData);
    setState({m2mSubmission:data || getState().m2mSubmission || null});
    return data || {};
  }

    /**
   * A <code>postStep</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function postStep(path){
    return m2mApi.postSubmissionStep(getState().submissionId, path);
  }

    /**
   * A <code>markCurrentXmlForSubmit</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function markCurrentXmlForSubmit(){
    await createOrUpdateSubmission();
    const data = await m2mApi.markForSubmit(getState().submissionId);
    setState({
      markedForSubmit: isMarkedForSubmission(data),
      interfaceType: data?.interfaceType || getState().interfaceType || 'BIZONYLAT_API'
    });
    updateMenuState();
    context.persistUiState?.();
    context.showMessage?.(`XML megjelölve NAV M2M Bizonylat API beküldésre. Csomagazonosító: ${getState().submissionId}`, 'success');
  }

    /**
   * A <code>withdrawCurrentXmlSubmitMark</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function withdrawCurrentXmlSubmitMark(){
    const current = getState();
    if(!current.submissionId) throw new Error('Nincs M2M beküldési csomag.');
    await m2mApi.withdrawSubmitMark(current.submissionId);
    setState({ markedForSubmit: false });
    updateMenuState();
    context.persistUiState?.();
    context.showMessage?.('M2M beküldésre jelölés visszavonva.', 'success');
  }

    /**
   * Megjeleníti vagy újrarendereli a show current submission logs állapotát a felhasználói felületen.
   *
   * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function showCurrentSubmissionLogs(){
    const current = getState();
    if(!current.submissionId) throw new Error('Nincs M2M beküldési csomag.');
    const events = await m2mApi.getSubmissionLogs(current.submissionId);
    showM2mLogsModal({ events: events || [], submissionId: current.submissionId });
  }

    /**
   * Betölti vagy lekéri a fetch attachment info művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function fetchAttachmentInfo(){
    const files = pendingFiles();
    if(!files.length) throw new Error('Nincs hozzáadott csatolmány.');
    const phases = [
      'Csatolmányok ellenőrzése',
      'Helyi csatolmányok mentése',
      'Token és nonce előkészítése',
      'Csatolmányok feltöltése',
      'XML fileId adatok frissítése',
      'XML mentése',
      'Felület frissítése'
    ];
    const progress = createM2mProgress('Csatolmányadatok lekérése', phases);
    try{
      progress.update(0, `${files.length} csatolmány ellenőrzése.`);
      progress.update(1, 'A helyi csatolmányok tartós mentése.');
      await storePendingAttachments(files);
      progress.update(2, 'NAV token és nonce előkészítése.');
      progress.update(3, `${files.length} csatolmány feltöltése a NAV-hoz.`);
      const data = await postStep('/step/upload-attachments');
      progress.update(4, 'A visszakapott NAV fileId értékek XML-be írása.');
      const result = attachments.applyUploadedIds(data?.uploadedAttachments || [], {
        onlyFileNames: files.map(file => file.name),
        updateExistingFileIds: false
      });
      if(result.missing.length){
        throw new Error(`A csatolmány feltöltés nem adott vissza fileId értéket ezekhez: ${result.missing.join(', ')}. Az XML nem lett fileId-val frissítve.`);
      }
      if(result.updated === 0){
        throw new Error('A csatolmány feltöltés sikeresnek tűnik, de nem található üres fileId-jú, azonos fileName értékű Attachment_1 node. Az XML nem lett módosítva.');
      }
      progress.update(5, 'A csatolmányadatokkal frissített XML mentése.');
      const saved = await createOrUpdateSubmission();
      setState({attachments: [], m2mSubmission:saved || data || null});
      progress.update(6, 'Csatolmánykártyák és XML-nézet frissítése.');
      context.renderXmlFromCurrentState?.();
      context.markFormDirty?.();
      context.persistUiState?.();
      updateMenuState();
      progress.close();
      context.showMessage?.('A csatolmány fileId adatok bekerültek az XML-be, és a beküldési csomag XML tartalma frissült.', 'success');
    }catch(error){
      progress.close();
      throw error;
    }
  }

    /**
   * Előkészíti és elindítja a add upload and refresh attachments állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} files a függvény files bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function addUploadAndRefreshAttachments(files){
    if(!files.length) return;
    if(!attachments.formDefinitionHasAttachment1(getState().currentFormDefinition)){
      throw new Error('A csatolmány hozzáadása csak olyan XML-nél engedélyezett, amelynek XSD-jében szerepel az Attachment_1 elem.');
    }
    const duplicate = attachments.findDuplicateFileName(files.map(file => file.name));
    if(duplicate) throw new Error(`Az XML-en belül a csatolmány fájlnevek nem lehetnek egyformák: ${duplicate}`);

    const phases = [
      'Fájlok ellenőrzése',
      'Attachment_1 elemek létrehozása',
      'Helyi csatolmányok mentése',
      'Token és nonce előkészítése',
      'Csatolmányok feltöltése',
      'XML fileId adatok frissítése',
      'XML mentése és felületfrissítés'
    ];
    const progress = createM2mProgress('Csatolmány hozzáadása és feltöltése', phases);
    let addedNodes = [];
    try{
      progress.update(0, `${files.length} kiválasztott fájl ellenőrzése.`);
      progress.update(1, 'Új Attachment_1 elemek létrehozása az XML-ben.');
      addedNodes = files.map(file => attachments.addPlaceholder(file));
      setState({ attachments: files, markedForSubmit: false });
      progress.update(2, 'A csatolmányok tartós mentése.');
      await storePendingAttachments(files);
      progress.update(3, 'NAV token és nonce előkészítése.');
      progress.update(4, `${files.length} csatolmány feltöltése a NAV-hoz.`);
      const data = await postStep('/step/upload-attachments');
      progress.update(5, 'A NAV fileId értékek hozzárendelése a megfelelő Attachment_1 elemekhez.');
      const result = attachments.applyUploadedIds(data?.uploadedAttachments || [], {
        onlyFileNames: files.map(file => file.name),
        updateExistingFileIds: false
      });
      if(result.missing.length){
        throw new Error(`A csatolmány feltöltés nem adott vissza fileId értéket: ${result.missing.join(', ')}`);
      }
      if(result.updated !== files.length){
        throw new Error('Nem minden új csatolmány NAV fileId értéke köthető egyértelműen az XML megfelelő Attachment_1 eleméhez.');
      }
      progress.update(6, 'A frissített XML mentése és a kártyák frissítése.');
      const saved = await createOrUpdateSubmission();
      setState({ attachments: [], m2mSubmission:saved || data || null });
      context.renderXmlFromCurrentState?.();
      context.markFormDirty?.();
      context.persistUiState?.();
      updateMenuState();
      progress.close();
      context.showMessage?.(`${files.length} csatolmány feltöltve, és a megfelelő Attachment_1/fileId mezők frissítve.`, 'success');
    }catch(error){
      addedNodes.forEach(node => node?.parentNode?.removeChild(node));
      setState({ attachments: [] });
      context.renderXmlFromCurrentState?.();
      updateMenuState();
      progress.close();
      throw error;
    }
  }

    /**
   * Eltávolítja vagy alaphelyzetbe állítja a delete attachment művelethez tartozó kliensoldali állapotot.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function deleteAttachment(){
    const nodes = attachments.collectNodes();
    if(!nodes.length) throw new Error('Nincs törölhető Attachment_1 csatolmány az XML-ben.');
    const selectedNode = await chooseAttachmentForDeletion(nodes);
    if(!selectedNode) return;
    const selected = attachments.findFileName(selectedNode);
    const meta = attachments.findUploadedByFileName(getState().m2mSubmission?.uploadedAttachments || [], selected);
    await performAttachmentDeletion(meta, selectedNode, {confirmed:true});
  }

    /**
   * A <code>prepareAttachmentsForSubmission</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @param {*} progress a függvény progress bemeneti értéke
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function prepareAttachmentsForSubmission(progress){
    progress.update(2, 'Az XML-ben és az adatbázisban nyilvántartott csatolmányok összevetése.');
    let currentSubmission = getState().m2mSubmission || {};
    let xmlChanged = false;
    const files = pendingFiles();

    if(files.length){
      progress.update(3, `${files.length} csak helyileg csatolt állomány mentése és NAV-feltöltése.`);
      await storePendingAttachments(files);
      const uploaded = await postStep('/step/upload-attachments');
      const result = attachments.applyUploadedIds(uploaded?.uploadedAttachments || [], {
        onlyFileNames: files.map(file => file.name),
        updateExistingFileIds: false
      });
      if(result.missing.length){
        throw new Error(`A csatolmány feltöltés nem adott vissza fileId értéket: ${result.missing.join(', ')}`);
      }
      if(result.updated !== files.length){
        throw new Error('Nem minden csak helyileg csatolt állomány NAV fileId értéke köthető egyértelműen az XML megfelelő Attachment_1 eleméhez.');
      }
      currentSubmission = uploaded || currentSubmission;
      setState({attachments:[], m2mSubmission:currentSubmission});
      xmlChanged = true;
    }

    const metadata = currentSubmission?.uploadedAttachments || getState().m2mSubmission?.uploadedAttachments || [];
    const toRefresh = metadata.filter(attachmentRequiresPreparation);
    for(let index = 0; index < toRefresh.length; index += 1){
      const meta = toRefresh[index];
      const fileName = attachmentDisplayName(meta);
      if(!meta?.id){
        throw new Error(`A(z) „${fileName}” csatolmányhoz nem található adatbázis-azonosító; az automatikus megújítás nem indítható.`);
      }
      if(meta?.localFileAvailable === false){
        throw new Error(`A(z) „${fileName}” csatolmány megújítása szükséges, de a helyi fájl nem található.`);
      }
      progress.update(3, `Csatolmány megújítása (${index + 1}/${toRefresh.length}): ${fileName}`);
      const refreshedSubmission = await m2mApi.refreshAttachment(getState().submissionId, meta.id);
      const refreshed = (refreshedSubmission?.uploadedAttachments || []).find(item => item.id === meta.id);
      if(!refreshed?.navFileId){
        throw new Error(`A(z) „${fileName}” csatolmány újrafeltöltése nem adott vissza NAV fileId értéket.`);
      }
      const applied = attachments.applyUploadedIds(refreshedSubmission.uploadedAttachments || [], {
        onlyFileNames:[fileName],
        updateExistingFileIds:true
      });
      if(applied.updated !== 1){
        throw new Error(`A(z) „${fileName}” új NAV fileId értéke nem köthető egyértelműen az XML megfelelő Attachment_1 eleméhez.`);
      }
      currentSubmission = refreshedSubmission;
      setState({m2mSubmission:currentSubmission});
      xmlChanged = true;
    }

    const xmlNodes = attachments.collectNodes();
    const finalMetadata = currentSubmission?.uploadedAttachments || [];
    for(const node of xmlNodes){
      const fileName = attachments.findFileName(node);
      const meta = attachments.findUploadedByFileName(finalMetadata, fileName);
      if(!meta){
        throw new Error(`A(z) „${fileName || 'névtelen'}” XML-csatolmányhoz nem található helyi csatolmányrekord; a beküldés nem indítható.`);
      }
      if(attachmentRequiresPreparation(meta) || !meta.navFileId){
        throw new Error(`A(z) „${fileName}” csatolmány a megújítás után sem érvényes; a beküldés nem indítható.`);
      }
      if(attachments.findFileIdValue(node) !== String(meta.navFileId)){
        attachments.setFileId(node, meta.navFileId);
        xmlChanged = true;
      }
    }

    progress.update(4, xmlChanged
      ? 'A megújított csatolmányazonosítókat tartalmazó XML mentése.'
      : 'A csatolmányok érvényesek; nincs szükség újrafeltöltésre.');
    if(xmlChanged){
      const saved = await createOrUpdateSubmission();
      currentSubmission = saved || currentSubmission;
      setState({m2mSubmission:currentSubmission});
      context.renderXmlFromCurrentState?.();
      context.persistUiState?.();
    }
    return currentSubmission;
  }

    /**
   * Elindítja a submit current xml aszinkron vagy több lépéses frontend folyamatát.
   *
   * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function submitCurrentXml(){
    if(!getState().markedForSubmit) throw new Error('A beküldés előtt jelöld meg az XML-t beküldésre.');
    const phases=[
      'Beküldés előkészítése',
      'Jogosultság és állapot ellenőrzése',
      'Csatolmányok állapotának ellenőrzése',
      'Csatolmányok feltöltése vagy megújítása',
      'XML csatolmányadatok frissítése és mentése',
      'Token és nonce előkészítése',
      'Bizonylat feltöltése',
      'NAV válasz feldolgozása',
      'Adatbázis mentése'
    ];
    const progress=createM2mProgress('NAV M2M beküldés', phases); let finalData=null;
    try{
      progress.update(0, 'Az aktuális XML és a beküldési csomag összehangolása.');
      const submission=await createOrUpdateSubmission();
      progress.update(1, 'A beküldési jogosultság és a csomag állapotának ellenőrzése.');
      const interfaceType=submission?.interfaceType||getState().interfaceType||'BIZONYLAT_API';
      setState({interfaceType, m2mSubmission:submission || getState().m2mSubmission});

      await prepareAttachmentsForSubmission(progress);

      progress.update(5,'Token és nonce előkészítése a NAV beküldéshez.');
      const fastTrackExpected=Boolean(getState().m2mSubmission?.fastTrackSubmissionEligible);
      progress.update(6, fastTrackExpected
        ? 'Gyorsított pályás beküldés: a sikeres online validáció tanúsítványának továbbítása.'
        : 'Bizonylat beküldése a NAV Bizonylat API végpontjára.');
      finalData=await postStep('/step/create-bizonylat');
      progress.update(7, finalData?.fastTrackSubmissionUsed
        ? 'A NAV válasz feldolgozása. Gyorsított pályás beküldés történt.'
        : 'A NAV válasz feldolgozása.');
      setState({markedForSubmit:false, m2mSubmission:finalData||getState().m2mSubmission});
      updateMenuState();
      context.persistUiState?.();
      progress.update(8, 'A beküldési eredmény adatbázisba mentése befejeződött.');
      progress.close();
      showM2mResult(finalData,true);
    }catch(error){
      progress.close();
      showM2mResult({resultCode:error?.body?.resultCode||'SIKERTELEN',resultMessage:error.message},false);
      throw error;
    }
  }

    /**
   * Kezeli vagy beköti a handle action esemény- és inicializációs folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} action a függvény action bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function handleAction(action){
    closeMenu();
    if(action !== 'show-m2m-logs' && !(await ensureM2mAvailable())) return;
    if(isM2mSuccessfulTerminal(getState().m2mSubmission) && action !== 'show-m2m-logs'){
      context.showMessage?.('A sikeresen beküldött űrlap végállapotban van, ezért kizárólag megtekinthető.', 'info');
      return;
    }
    if(action === 'add-attachment' || action === 'add-and-fetch-attachment-info'){
      if(!attachments.formDefinitionHasAttachment1(getState().currentFormDefinition)){
        context.showMessage?.('A csatolmány hozzáadása csak olyan XML-nél engedélyezett, amelynek XSD-jében szerepel az Attachment_1 elem.', 'error');
        return;
      }
      setState({ addAndFetchAfterFileSelect: action === 'add-and-fetch-attachment-info' });
      elements.attachmentInput?.click();
      return;
    }
    try{
      if(action === 'mark-submit') await markCurrentXmlForSubmit();
      if(action === 'withdraw-submit-mark') await withdrawCurrentXmlSubmitMark();
      if(action === 'show-m2m-logs') await showCurrentSubmissionLogs();
      if(action === 'fetch-attachment-info') await fetchAttachmentInfo();
      if(action === 'delete-attachment') await deleteAttachment();
      if(action === 'submit-document') await submitCurrentXml();
    }catch(error){
      console.error('M2M form action error', error);
      context.showMessage?.(error.message || 'A NAV M2M művelet nem sikerült.', 'error');
    }finally{
      updateMenuState();
    }
  }

    /**
   * Kezeli vagy beköti a handle attachment input change esemény- és inicializációs folyamatát.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function handleAttachmentInputChange(){
    if(!(await ensureM2mAvailable())){ if(elements.attachmentInput) elements.attachmentInput.value=''; return; }
    const files = Array.from(elements.attachmentInput?.files || []);
    if(!files.length) return;
    try{
      if(getState().addAndFetchAfterFileSelect){
        setState({ addAndFetchAfterFileSelect: false });
        await addUploadAndRefreshAttachments(files);
        return;
      }
      const phases = ['Fájlok ellenőrzése','Attachment_1 elemek létrehozása','XML és csatolmánykártyák frissítése'];
      const progress = createM2mProgress('Csatolmány hozzáadása', phases);
      try{
        progress.update(0, `${files.length} kiválasztott fájl ellenőrzése.`);
        const duplicate = attachments.findDuplicateFileName(files.map(file => file.name));
        if(duplicate){
          throw new Error(`Az XML-en belül a csatolmány fájlnevek nem lehetnek egyformák: ${duplicate}`);
        }
        if(!attachments.formDefinitionHasAttachment1(getState().currentFormDefinition)){
          throw new Error('A csatolmány hozzáadása csak olyan XML-nél engedélyezett, amelynek XSD-jében szerepel az Attachment_1 elem.');
        }
        progress.update(1, 'Új Attachment_1 elemek létrehozása az XML-ben.');
        attachments.addPlaceholders(files);
        setState({ attachments: files, markedForSubmit: false });
        progress.update(2, 'Az XML-nézet és a Csatolmányok blokk frissítése.');
        context.renderXmlFromCurrentState?.();
        context.markFormDirty?.();
        context.persistUiState?.();
        updateMenuState();
        await waitForNextPaint();
        progress.close();
        context.showMessage?.(`${files.length} csatolmány adatai bekerültek az XML Attachment_1 elemébe. A fileId egyelőre üres.`, 'success');
      }catch(error){
        progress.close();
        throw error;
      }
    }catch(error){
      console.error('Attachment add error', error);
      context.showMessage?.(error.message || 'A csatolmány hozzáadása nem sikerült.', 'error');
    }finally{
      if(elements.attachmentInput) elements.attachmentInput.value = '';
    }
  }


    /**
   * Betölti vagy lekéri a load latest submission for current xml művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadLatestSubmissionForCurrentXml(){
    const xmlFileId = new URLSearchParams(window.location.search).get('xmlFileId');
    if(!xmlFileId) return;
    try{
      const data = await m2mApi.latestSubmissionForXmlFile(xmlFileId);
      setState({submissionId:data?.id || null, m2mSubmission:data || null, interfaceType:data?.interfaceType || 'BIZONYLAT_API', markedForSubmit:isMarkedForSubmission(data)});
      if(isM2mSuccessfulTerminal(data)){
        context.setSuccessfulSubmissionReadOnly?.();
        context.showMessage?.('Az űrlap sikeresen beküldött végállapotban van, ezért csak megtekinthető.', 'info');
      }
      updateMenuState();
    }catch(error){
      if(error?.status !== 404) console.warn('M2M csatolmány-metaadatok betöltése sikertelen', error);
    }
  }

  return {
    loadLatestSubmissionForCurrentXml,
    bindOperationMenuEvents,
    updateOperationMenuState,
    positionMenu,
    toggleMenu,
    closeMenu,
    updateMenuState,
    handleAction,
    handleAttachmentInputChange,
    createOrUpdateSubmission,
    markCurrentXmlForSubmit,
    withdrawCurrentXmlSubmitMark,
    showCurrentSubmissionLogs,
    fetchAttachmentInfo,
    deleteAttachment,
    submitCurrentXml
  };
}

/**
 * Kezeli vagy beköti a init m2m ui esemény- és inicializációs folyamatát.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @returns {*} a feldolgozás eredménye
 */
export function initM2mUi(){
  if(activeController) return activeController;
  const context = window.NavM2mRuntime;
  if(!context) return null;
  activeController = createM2mFormController(context);
  window.NavM2mFormUi = activeController;
  activeController.bindOperationMenuEvents();
  activeController.updateMenuState();
  loadM2mAvailability().then(()=>activeController?.updateMenuState());
  window.addEventListener('m2m-availability-changed',()=>activeController?.updateMenuState());
  activeController.loadLatestSubmissionForCurrentXml();
  return activeController;
}
