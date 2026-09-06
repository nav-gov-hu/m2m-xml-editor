/**
 * @module xml/xml-session-ui
 *
 * A XML-szerkesztési és állománykezelési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

/**
 * Aktív XML munkamenet, session polling, lock-release kérelmek,
 * automatikus megnyitás és bezárás kezelése.
 */
export function createXmlSessionUi({ elements, getState, setState, callbacks }){
  const { xmlPathInput, schemaDirInput, generalXsdDirInput, closeActiveXmlButton } = elements;
  let activeSessionPollTimer = null;
  let lockReleasePollTimer = null;
  const shownLockReleaseRequestIds = new Set();

    /**
   * Elindítja a start active session polling aszinkron vagy több lépéses frontend folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function startActiveSessionPolling(){
    stopActiveSessionPolling();
    const state = getState();
    if(!state.currentActiveXmlFile?.id || !state.currentActiveXmlFileSessionId) return;
    activeSessionPollTimer = setInterval(checkActiveSessionState, 10000);
  }

    /**
   * A <code>stopActiveSessionPolling</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function stopActiveSessionPolling(){
    if(activeSessionPollTimer){
      clearInterval(activeSessionPollTimer);
      activeSessionPollTimer = null;
    }
  }

    /**
   * Ellenőrzi a check active session state feltételeit, és a hívó számára döntési eredményt állít elő.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function checkActiveSessionState(){
    const state = getState();
    if(!state.currentActiveXmlFile?.id || !state.currentActiveXmlFileSessionId) return;
    try{
      const response = await fetch(`/api/xml-files/${encodeURIComponent(state.currentActiveXmlFile.id)}/session-state?sessionId=${encodeURIComponent(state.currentActiveXmlFileSessionId)}`, { cache:'no-store', credentials:'same-origin' });
      const data = await response.json().catch(() => ({}));
      if(!response.ok) return;
      if(data?.active === false){
        stopActiveSessionPolling();
        try{
          sessionStorage.setItem('navXmlFilesMessage', JSON.stringify({
            type:'warning',
            text:`Az XML munkamenetet lezárták${data.closedBy ? ` (${data.closedBy})` : ''}. ${data.closeReason || ''}`
          }));
        }catch(_ignored){}
        sessionStorage.removeItem('navXsdToolActiveXmlFile');
        window.location.href = '/xml-files.html';
      }
    }catch(_ignored){}
  }

    /**
   * Elindítja a start lock release request polling aszinkron vagy több lépéses frontend folyamatát.
   *
   * <p>A hálózati hívás eredményét egységes kliensoldali hibakezeléssel adja tovább, és a felhasználói felületet csak a válasz feldolgozása után módosítja.</p>
   */
function startLockReleaseRequestPolling(){
    if(lockReleasePollTimer || !document.body || document.body.dataset.initialTab !== 'formTab') return;
    lockReleasePollTimer = setInterval(checkPendingLockReleaseRequests, 8000);
  }

    /**
   * A <code>stopLockReleaseRequestPolling</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A hálózati hívás eredményét egységes kliensoldali hibakezeléssel adja tovább, és a felhasználói felületet csak a válasz feldolgozása után módosítja.</p>
   */
function stopLockReleaseRequestPolling(){
    if(lockReleasePollTimer){
      clearInterval(lockReleasePollTimer);
      lockReleasePollTimer = null;
    }
  }

    /**
   * Ellenőrzi a check pending lock release requests feltételeit, és a hívó számára döntési eredményt állít elő.
   *
   * <p>A hálózati hívás eredményét egységes kliensoldali hibakezeléssel adja tovább, és a felhasználói felületet csak a válasz feldolgozása után módosítja.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function checkPendingLockReleaseRequests(){
    try{
      const response = await fetch('/api/xml-files/lock-release-requests/pending', { cache:'no-store', credentials:'same-origin' });
      if(!response.ok) return;
      const requests = await response.json() || [];
      for(const request of requests){
        if(!request || shownLockReleaseRequestIds.has(request.id)) continue;
        shownLockReleaseRequestIds.add(request.id);
        const accepted = window.navConfirm ? await window.navConfirm({
          title:'XML munkamenet lezárási kérelem',
          message:`${request.requesterUsername || 'Egy felhasználó'} lezárást kér az alábbi XML-hez:\n${request.fileName || ''}\n\nIndok: ${request.message || '-'}`,
          confirmText:'Munkamenet lezárása',
          cancelText:'Elutasítás',
          variant:'warning'
        }) : false;
        await respondToLockReleaseRequest(request.id, accepted);
      }
    }catch(_ignored){}
  }

    /**
   * A <code>respondToLockReleaseRequest</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A hálózati hívás eredményét egységes kliensoldali hibakezeléssel adja tovább, és a felhasználói felületet csak a válasz feldolgozása után módosítja.</p>
   * @param {*} requestId a célobjektum technikai azonosítója
   * @param {*} accepted a függvény accepted bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function respondToLockReleaseRequest(requestId, accepted){
    const action = accepted ? 'accept' : 'reject';
    const response = await fetch(`/api/xml-files/lock-release-requests/${encodeURIComponent(requestId)}/${action}`, {
      method:'POST',
      credentials:'same-origin',
      headers:{ 'Content-Type':'application/json' },
      body:JSON.stringify({ message:accepted ? 'Lezárva.' : 'Elutasítva.' })
    });
    if(!response.ok) return;
    if(accepted){
      try{
        sessionStorage.setItem('navXmlFilesMessage', JSON.stringify({ type:'warning', text:'Az XML munkamenetet lezárási kérelem alapján lezártad.' }));
      }catch(_ignored){}
      sessionStorage.removeItem('navXsdToolActiveXmlFile');
      window.location.href = '/xml-files.html';
    }
  }


    /**
   * Megjeleníti vagy újrarendereli a show initial large xml process állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
   */
function showInitialLargeXmlProcess(message){
    let overlay = document.getElementById('largeXmlProcessOverlay');
    if(!overlay){
      overlay = document.createElement('div');
      overlay.id = 'largeXmlProcessOverlay';
      overlay.className = 'large-xml-process-overlay';
      overlay.innerHTML = '<div class="large-xml-process-dialog" role="status" aria-live="polite"><div class="large-xml-process-spinner"></div><h2>Nagy XML megnyitása</h2><p></p></div>';
      document.body.appendChild(overlay);
    }
    overlay.querySelector('h2').textContent = 'Nagy XML megnyitása';
    overlay.querySelector('p').textContent = message || 'A főlap és a melléklap-információk streaming feldolgozása folyamatban...';
    overlay.hidden = false;
  }

    /**
   * Elrejti vagy lezárja a hide initial large xml process felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function hideInitialLargeXmlProcess(){
    const overlay = document.getElementById('largeXmlProcessOverlay');
    if(overlay) overlay.hidden = true;
  }

    /**
   * A <code>autoLoadFromQuery</code> függvény a XML-szerkesztési és állománykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function autoLoadFromQuery(){
    const params = new URLSearchParams(window.location.search || '');
    let xmlFileId = params.get('xmlFileId');
    if(!xmlPathInput || !document.body || document.body.dataset.initialTab !== 'formTab') return;
    let opened = null;
    try{
      const stored = sessionStorage.getItem('navXsdToolActiveXmlFile');
      if(stored){
        const parsed = JSON.parse(stored);
        if(!xmlFileId) xmlFileId = parsed?.file?.id || null;
        if(String(parsed?.file?.id || '') === String(xmlFileId || '')) opened = parsed;
      }
    }catch(error){
      console.warn('Aktív Űrlapállomány session adat nem olvasható.', error);
    }
    if(!xmlFileId) return;
    if(!opened){
      const readOnly = params.get('readOnly') === 'true';
      const response = await fetch(`/api/xml-files/${encodeURIComponent(xmlFileId)}/open`, {
        method:'POST',
        credentials:'same-origin',
        headers:{ 'Content-Type':'application/json' },
        body:JSON.stringify({ readOnly })
      });
      const data = await response.json().catch(() => ({}));
      if(!response.ok) throw new Error(data.message || data.error || 'Az Űrlapállomány megnyitása sikertelen.');
      opened = data;
    }
    const filePath = opened?.file?.filePath;
    if(!filePath) return;
    if(opened?.schemaVersionFallback === true){
      const warningKey = `navXsdToolSchemaFallbackWarning:${opened.sessionId || xmlFileId}`;
      let warningShown = false;
      try{ warningShown = sessionStorage.getItem(warningKey) === 'true'; }catch(_ignored){}
      if(!warningShown){
        const message = opened.message
          || `Az XML űrlapverziója ${opened.xmlFormVersion || '-'}, a feloldott XSD verziója ${opened.resolvedXsdVersion || '-'}. Az űrlap csak olvasható kompatibilitási módban nyílik meg.`;
        if(window.navInfo){
          await window.navInfo({
            eyebrow:'Figyelmeztetés',
            title:'Nincs pontos XSD-verzió',
            message,
            cancelText:'Megnyitás csak olvasásra',
            variant:'warning'
          });
        }else{
          callbacks.showMessage(message, 'warning');
        }
        try{ sessionStorage.setItem(warningKey, 'true'); }catch(_ignored){}
      }
    }
    xmlPathInput.value = filePath;
    const pathRadio = document.querySelector('input[name="xmlMode"][value="path"]');
    if(pathRadio) pathRadio.checked = true;
    callbacks.syncMode();
    callbacks.syncSelectedXmlSourceDisplay();
    callbacks.showMessage(`${opened.readOnly ? 'Olvasási módban megnyitva' : 'Szerkesztésre megnyitva'}: ${opened.file.fileName}`, 'success');

    setState({
      currentActiveXmlFile:opened.file || null,
      currentActiveXmlFileSessionId:opened.sessionId || null,
      currentXmlFileReadOnlyMode:opened.readOnly === true
    });
    try{
      sessionStorage.setItem('navXsdToolActiveXmlFile', JSON.stringify(opened));
      sessionStorage.setItem('navXsdToolLastFormUrl', callbacks.buildActiveFormUrl());
    }catch(_ignored){}
    callbacks.updateFormNavigationLinks();
    callbacks.clearFormDirty();
    callbacks.updateCloseActiveXmlButton();
    callbacks.applyLargeFileModeForActiveXml();
    startActiveSessionPolling();
    startLockReleaseRequestPolling();
    document.body.classList.toggle('xml-file-readonly-mode', opened.readOnly === true);
    if(opened.readOnly === true){
      setState({ currentUiModelMissingFieldsVisible:false });
      callbacks.updateFormRendererSwitch();
      callbacks.persistUiState();
    }

    const requestData = new FormData();
    if(schemaDirInput?.value?.trim()) requestData.append('schemaDir', schemaDirInput.value.trim());
    if(generalXsdDirInput?.value?.trim()) requestData.append('generalXsdDir', generalXsdDirInput.value.trim());
    requestData.append('xmlPath', filePath);
    if(opened?.file?.largeFileMode === true) requestData.append('largeFileMode', 'true');
    callbacks.setBusy(true);
    if(opened?.file?.largeFileMode === true){
      showInitialLargeXmlProcess('Az állomány szerkezetének vizsgálata, a főlap kiemelése és a melléklapok számlálása folyamatban. Nagy állománynál ez több időt vehet igénybe.');
    }
    try{
      const response = await fetch('/api/validate', { method:'POST', body:requestData });
      const data = await response.json();
      if(!response.ok) throw new Error(data.error || data.message || 'Az XML betöltése nem sikerült.');
      callbacks.renderValidate(data, { preserveExistingFormOnInvalid:true });
      callbacks.clearLargeXmlSourceIfNeeded();
      if(data?.largeFileMode === true && data?.largeFileMessage){
        callbacks.showMessage(data.largeFileMessage, 'warning');
      }
      if(data.formDefinition && (data.xmlView?.rawXml || data.partialPreview === true || data.largeFileMode === true)) callbacks.activateTab('formTab');
      if(opened.readOnly){
        setTimeout(() => document.querySelectorAll('#formContainer input, #formContainer select, #formContainer textarea').forEach(element => {
          // Az olvasási mód az üzleti mezőket tiltja, a melléklap-lista keresője
          // azonban navigációs vezérlő, ezért olvasási módban is használható marad.
          if(element.dataset?.multiformSearchQuery === 'true') return;
          element.disabled = true;
        }), 0);
      }else{
        document.body.classList.remove('xml-file-readonly-mode');
      }
      callbacks.updateFormRendererSwitch();
      // Az Űrlapmegtekintőre történő visszanavigáláskor ne induljon el
      // automatikus XSD-validáció. A validáció továbbra is kézzel indítható
      // az XML-validáció gombbal vagy a validációs drawerből.
    }finally{
      hideInitialLargeXmlProcess();
      callbacks.setBusy(false);
    }
  }

    /**
   * Elrejti vagy lezárja a close active xml file felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function closeActiveXmlFile(){
    const state = getState();
    if(!state.currentActiveXmlFile?.id){
      callbacks.showMessage('Nincs aktív Űrlapállomány.', 'info');
      return;
    }
    if(state.currentFormHasUnsavedChanges){
      const confirmed = window.navConfirm ? await window.navConfirm({
        title:'XML bezárása',
        message:'Biztosan be akarod zárni az XML-t? Mentetlen módosítások történtek.',
        confirmText:'Bezárás mentés nélkül',
        cancelText:'Mégsem',
        variant:'warning'
      }) : false;
      if(!confirmed) return;
    }
    try{
      const response = await fetch(`/api/xml-files/${encodeURIComponent(state.currentActiveXmlFile.id)}/close`, {
        method:'POST',
        credentials:'same-origin',
        headers:{ 'Content-Type':'application/json' },
        body:JSON.stringify({ reason:'Felhasználói bezárás az Űrlapmegtekintő oldalról.', sessionId:state.currentActiveXmlFileSessionId })
      });
      const data = await response.json().catch(() => ({}));
      if(!response.ok) throw new Error(data.message || data.error || 'Az XML munkamenet lezárása sikertelen.');
    }catch(error){
      console.error('XML bezárási hiba', error);
      callbacks.showMessage(error.message || 'Az XML munkamenet lezárása sikertelen.', 'error');
      return;
    }
    stopActiveSessionPolling();
    sessionStorage.removeItem('navXsdToolActiveXmlFile');
    setState({
      currentActiveXmlFile:null,
      currentActiveXmlFileSessionId:null,
      currentXmlFileReadOnlyMode:false,
      currentXsdValidationState:{ status:'UNKNOWN', invalid:false, errorCount:0, errors:[] }
    });
    callbacks.clearXsdValidationHighlights();
    document.body.classList.remove('xml-file-readonly-mode');
    callbacks.clearFormDirty();
    callbacks.clearResults();
    callbacks.resetFormTab();
    callbacks.closeFormXpathValidationPopup();
    callbacks.closeFormXsdValidationDrawer();
    callbacks.updateFormXpathDrawerTab('neutral', 'Ellenőrzés', 'Nincs ellenőrzés');
    callbacks.updateFormXsdDrawerTab('neutral', 'Ellenőrzés', 'Nincs XSD ellenőrzési eredmény.');
    if(window.history && window.location.search) window.history.replaceState({}, document.title, window.location.pathname);
    try{
      sessionStorage.setItem('navXmlFilesMessage', JSON.stringify({ type:'success', text:'Űrlapállomány bezárva, a munkamenet lezárva.' }));
    }catch(_ignored){}
    window.location.href = '/xml-files.html';
  }

    /**
   * Kezeli vagy beköti a init esemény- és inicializációs folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function init(){
    closeActiveXmlButton?.addEventListener('click', closeActiveXmlFile);
    startLockReleaseRequestPolling();
  }

  return {
    init,
    autoLoadFromQuery,
    closeActiveXmlFile,
    startActiveSessionPolling,
    stopActiveSessionPolling,
    startLockReleaseRequestPolling,
    stopLockReleaseRequestPolling,
    checkActiveSessionState,
    checkPendingLockReleaseRequests
  };
}
