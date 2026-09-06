/**
 * @module github-templates
 *
 * A GitHub űrlapsablon-katalógus működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

const state={rows:[],filtered:[],selected:new Map(),page:0,pageSize:20,lastCatalog:null,pollTimer:null,tokenConfigured:false,pendingRefreshDownloadItems:[]};
/**
 * A <code>$</code> függvény a GitHub űrlapsablon-katalógus folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} id a célobjektum technikai azonosítója
 */
const $=id=>document.getElementById(id);
/**
 * A <code>esc</code> függvény a GitHub űrlapsablon-katalógus folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} v a függvény v bemeneti értéke
 */
const esc=v=>String(v??'').replace(/[&<>"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
/**
 * A <code>fmt</code> függvény a GitHub űrlapsablon-katalógus folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} v a függvény v bemeneti értéke
 */
const fmt=v=>v?new Intl.DateTimeFormat('hu-HU',{dateStyle:'medium',timeStyle:'short'}).format(new Date(v)):'—';
/**
 * A <code>localBundleHref</code> függvény a GitHub űrlapsablon-katalógus folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} r a függvény r bemeneti értéke
 */
const localBundleHref=r=>`/api/github-templates/local-bundle?repository=${encodeURIComponent(r.repository||'')}&tag=${encodeURIComponent(r.releaseTag||'')}`;
/**
 * A <code>processing</code> függvény a GitHub űrlapsablon-katalógus folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} show a függvény show bemeneti értéke
 * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
 * @param {*} counter a függvény counter bemeneti értéke
 */
function processing(show,message='Feldolgozás folyamatban...',counter='-'){ $('templateProcessingDialog').hidden=!show; $('templateProcessingMessage').textContent=message; $('templateProcessingCounter').textContent=counter; }
/**
 * Szinkronizálja vagy frissíti a set progress által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} percent a függvény percent bemeneti értéke
 */
function setProgress(percent){$('templateProgressBar').style.width=`${Math.max(0,Math.min(100,percent||0))}%`;}
/**
 * A <code>message</code> függvény a GitHub űrlapsablon-katalógus folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} text a függvény text bemeneti értéke
 * @param {*} error a függvény error bemeneti értéke
 */
function message(text,error=false){
  if(!text) return;
  const type=typeof error==='string' ? error : (error ? 'error' : 'success');
  if(typeof window.showAppToast==='function'){
    window.showAppToast(String(text),type);
    return;
  }
  if(typeof window.navInfo==='function'){
    void window.navInfo({
      eyebrow:type==='error'?'Hiba':'Tájékoztatás',
      title:type==='error'?'A művelet nem sikerült':'Tájékoztatás',
      message:String(text),
      variant:type==='error'?'warning':'default',
      cancelText:'Bezárás'
    });
  }
}

/**
 * Felhasználóbarát dialógusban jelzi, ha a GitHub katalógus nem érhető el.
 * A technikai backend hiba csak a böngésző konzoljában marad diagnosztikai célra.
 * @param {*} error az eredeti technikai hiba
 * @returns {Promise<void>} a dialógus bezárását jelző Promise
 */
async function showGithubConnectionError(error){
  console.warn('GitHub katalóguskapcsolati hiba:', error);
  const options={
    title:'GitHub kapcsolat sikertelen',
    message:'A katalóguslekérdezés sikertelen, mert nem lehet elérni a GitHub szervert. Kérem, ellenőrizze az internetkapcsolatot.',
    eyebrow:'Hiba',
    variant:'warning',
    cancelText:'Bezárás'
  };
  if(typeof window.navInfo==='function'){
    await window.navInfo(options);
    return;
  }
  message(options.message,true);
}
/**
 * Szinkronizálja vagy frissíti a set github token quick status által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} text a függvény text bemeneti értéke
 * @param {*} error a függvény error bemeneti értéke
 */
function setGithubTokenQuickStatus(text,error=false){
  const status=$('githubTokenQuickStatus');
  if(!status) return;
  status.textContent=text||'';
  status.classList.toggle('error-text',Boolean(error));
}
/**
 * Szinkronizálja vagy frissíti a update github token quick button által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function updateGithubTokenQuickButton(){
  const input=$('githubTokenQuickInput');
  const button=$('saveGithubTokenQuickButton');
  if(button) button.disabled=!String(input?.value||'').trim();
}
/**
 * Megjeleníti vagy újrarendereli a show github token missing dialog állapotát a felhasználói felületen.
 *
 * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
 */
function showGithubTokenMissingDialog(){
  const dialog=$('githubTokenMissingDialog');
  const input=$('githubTokenQuickInput');
  if(!dialog) return;
  dialog.hidden=false;
  if(input){
    input.value='';
    updateGithubTokenQuickButton();
    setGithubTokenQuickStatus('A token titkosítva kerül tárolásra. Az értéket mentés után nem jelenítjük meg újra.');
    requestAnimationFrame(()=>input.focus());
  }
}
/**
 * Elrejti vagy lezárja a hide github token missing dialog felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
 *
 * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
 */
function hideGithubTokenMissingDialog(){
  const dialog=$('githubTokenMissingDialog');
  const input=$('githubTokenQuickInput');
  if(input) input.value='';
  updateGithubTokenQuickButton();
  if(dialog) dialog.hidden=true;
}
/**
 * Előkészíti és elindítja a save github token from dialog állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function saveGithubTokenFromDialog(){
  const input=$('githubTokenQuickInput');
  const button=$('saveGithubTokenQuickButton');
  const token=String(input?.value||'').trim();
  if(!token){
    setGithubTokenQuickStatus('Add meg a GitHub tokent.',true);
    input?.focus();
    return;
  }
  if(button) button.disabled=true;
  if(input) input.disabled=true;
  setGithubTokenQuickStatus('A GitHub token mentése folyamatban...');
  try{
    const key='nav.xsdparsertool.github-schema-updater.token';
    const response=await fetch('/api/admin/configuration',{
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body:JSON.stringify({values:{[key]:token},confirmedSensitiveKeys:[key]})
    });
    if(!response.ok){
      const raw=await response.text();
      let detail=raw;
      try{
        const data=raw?JSON.parse(raw):null;
        detail=data?.message||data?.detail||raw;
      }catch{
        // A sima szöveges backend hibaüzenetet változatlanul használjuk.
      }
      if(response.status===403) throw new Error('A GitHub token módosításához adminisztrátori jogosultság szükséges.');
      throw new Error(detail||`A GitHub token mentése sikertelen (${response.status}).`);
    }
    if(input) input.value='';
    await loadCached(false);
    state.tokenConfigured=true;
    updateGithubActions();
    hideGithubTokenMissingDialog();
    message('A GitHub token sikeresen beállítva. A GitHub műveletek már használhatók.');
  }catch(error){
    setGithubTokenQuickStatus(error?.message||'A GitHub token mentése sikertelen.',true);
  }finally{
    if(input) input.disabled=false;
    updateGithubTokenQuickButton();
  }
}
/**
 * Megjeleníti vagy újrarendereli a show github token missing dialog once állapotát a felhasználói felületen.
 *
 * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
 */
function showGithubTokenMissingDialogOnce(){
  if(state.tokenConfigured||sessionStorage.getItem('githubTokenMissingDialogShown')==='true') return;
  sessionStorage.setItem('githubTokenMissingDialogShown','true');
  showGithubTokenMissingDialog();
}

/**
 * Megjeleníti vagy újrarendereli a show download result állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} data a függvény data bemeneti értéke
 */
function showDownloadResult(data){
  const downloaded=data.downloadedCount||0,skipped=data.skippedCount||0,failed=data.failedCount||0;
  $('templateDownloadResultSummary').textContent=`${downloaded} letöltve, ${skipped} már helyben volt, ${failed} hibás.`;
  const blocks=[];
  (data.repositories||[]).forEach(repo=>(repo.tags||[]).forEach(tag=>{
    const artifacts=tag.installedArtifacts||[];
    const artifactHtml=artifacts.length?`<div class="template-installed-artifacts">${artifacts.map(a=>`<div class="template-installed-artifact ${a.status==='FAILED'?'failed':''}"><strong>${esc(a.artifactType)}</strong><span>${esc(a.status==='FAILED'?(a.message||'Telepítési hiba'):a.targetFile)}</span></div>`).join('')}</div>`:`<div class="template-no-installed-artifact">${esc(tag.message||'Nem találtunk automatikusan telepíthető XSD, XPath vagy UIModel állományt; a teljes release az archív könyvtárban megmaradt.')}</div>`;
    blocks.push(`<section class="template-download-result-item"><h3>${esc(repo.repositoryName)} · ${esc(tag.tagName)}</h3><div class="template-download-status">${esc(tag.status)} · archívum: ${esc(tag.targetDirectory||'—')}</div>${artifactHtml}</section>`);
  }));
  $('templateDownloadResultBody').innerHTML=blocks.join('')||'<p>Nincs részletes eredmény.</p>';
  $('templateDownloadResultDialog').hidden=false;
}
/**
 * Elrejti vagy lezárja a hide download result felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function hideDownloadResult(){$('templateDownloadResultDialog').hidden=true;}
/**
 * A <code>sourceState</code> függvény a GitHub űrlapsablon-katalógus folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} data a függvény data bemeneti értéke
 */
function sourceState(data){const sync=data.lastSuccessfulSyncAt?`utolsó frissítés: ${fmt(data.lastSuccessfulSyncAt)}`:'még nincs DB-katalógus';$('templateSourceState').textContent=`${data.organization} · ${data.repositoryCount} repo · ${data.rowCount} release tag · ${sync} · PAT: ${data.tokenConfigured?'beállítva':'nincs'}`;}
/**
 * Betölti vagy lekéri a load cached művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} showMessage a megjelenítendő vagy feldolgozandó üzenet
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function loadCached(showMessage=false){try{const r=await fetch('/api/github-templates/catalog?preferredOnly=false',{cache:'no-store'});if(!r.ok)throw new Error(await r.text());const data=await r.json();state.lastCatalog=data;state.rows=data.rows||[];state.tokenConfigured=Boolean(data.tokenConfigured);state.selected.clear();sourceState(data);updateGithubActions();void updateLocalDeletePermission();buildFilters();apply();if(showMessage)message('A helyi űrlapsablon-lista újratöltése sikeres.');return data;}catch(e){state.rows=[];apply();message(`Betöltési hiba: ${e.message}`,true);throw e;}}
/**
 * Szinkronizálja vagy frissíti a update github actions által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
async function updateLocalDeletePermission(){const button=$('deleteSelectedLocalTemplatesButton');if(!button)return;try{const response=await fetch('/api/security/current-user',{cache:'no-store'});if(!response.ok)return;const user=await response.json();const roles=Array.isArray(user?.roles)?user.roles.map(role=>String(role).toUpperCase()):[];button.hidden=!(roles.includes('ROLE_ADMIN')||roles.includes('ADMIN'));}catch(_ignored){button.hidden=true;}}
function updateGithubActions(){const disabled=!state.tokenConfigured;$('manualGithubRefreshButton').disabled=disabled;$('manualGithubRefreshButton').title=disabled?'A GitHub token nincs beállítva':'Kézi GitHub frissítésellenőrzés';if(disabled)state.selected.clear();}
/**
 * A <code>reloadLocalList</code> függvény a GitHub űrlapsablon-katalógus folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function reloadLocalList(){processing(true,'A helyi űrlapsablon-lista újratöltése...','Adatbázis-katalógus betöltése');setProgress(25);try{await loadCached(false);setProgress(100);message(state.tokenConfigured?'A helyi űrlapsablon-lista újratöltése sikeres.':'A helyi lista betöltődött, de GitHub token nélkül külső lekérés és letöltés nem indítható.',state.tokenConfigured?'success':'warning');}catch(e){message(`Lista újratöltési hiba: ${e.message}`,true);}finally{processing(false);}}
/**
 * Megjeleníti vagy újrarendereli a show no changes állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function showNoChanges(){$('templateNoChangesDialog').hidden=false;}
/**
 * Elrejti vagy lezárja a hide no changes felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function hideNoChanges(){$('templateNoChangesDialog').hidden=true;}
/**
 * Ellenőrzi a check changes feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} automatic a függvény automatic bemeneti értéke
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function checkChanges(automatic=false){if(!state.tokenConfigured){if(!automatic)showGithubTokenMissingDialog();return null;}processing(true,'GitHub repositoryk ellenőrzése...','Változások keresése');setProgress(20);try{const r=await fetch('/api/github-templates/changes',{cache:'no-store'});if(!r.ok)throw new Error(await r.text());setProgress(100);const d=await r.json();processing(false);if(d.changesDetected){showRefreshConfirm(d);}else if(!automatic){showNoChanges();}return d;}catch(e){processing(false);if(!automatic)await showGithubConnectionError(e);else console.warn('Automatikus GitHub változásellenőrzés sikertelen:',e);return null;}}
/**
 * Megjeleníti vagy újrarendereli a show refresh confirm állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} d a függvény d bemeneti értéke
 */
function showRefreshConfirm(d){const initial=!d.initialized;const changed=d.changedRepositoryCount||0,removed=d.removedRepositoryCount||0;$('templateRefreshConfirmMessage').textContent=initial?`A helyi katalógus még üres. A GitHub organizációban ${d.organizationRepositoryCount} aktív repository található. Letöltöd a katalógust?`:`Az utolsó frissítés óta ${changed} repository változott és ${removed} repository került ki az aktív listából. Az organizációban összesen ${d.organizationRepositoryCount} aktív repository van. Frissíted a listát?`;$('templateRefreshConfirmDialog').hidden=false;}
/**
 * Elrejti vagy lezárja a hide refresh confirm felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function hideRefreshConfirm(){$('templateRefreshConfirmDialog').hidden=true;}
/**
 * Elindítja a start refresh aszinkron vagy több lépéses frontend folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function startRefresh(){hideRefreshConfirm();processing(true,'A katalógusfrissítés indítása...','Repositorylista előkészítése');setProgress(0);try{const r=await fetch('/api/github-templates/refresh',{method:'POST'});if(!r.ok)throw new Error(await r.text());const d=await r.json();if(!d.started)throw new Error(d.message||'A frissítés nem indult el.');pollRefresh();}catch(e){processing(false);await showGithubConnectionError(e);}}
/**
 * A <code>pollRefresh</code> függvény a GitHub űrlapsablon-katalógus folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function pollRefresh(){clearTimeout(state.pollTimer);try{const r=await fetch('/api/github-templates/refresh/status',{cache:'no-store'});if(!r.ok)throw new Error(await r.text());const d=await r.json();const changed=d.changedRepositoryCount||0,processed=d.processedChangedRepositoryCount||0,removed=d.removedRepositoryCount||0,removedDone=d.processedRemovedRepositoryCount||0,totalWork=changed+removed,done=processed+removedDone;const percent=totalWork?Math.round(done*100/totalWork):(d.completed?100:5);setProgress(percent);let text='Repositorylista ellenőrzése...';if(d.phase==='REFRESHING')text=d.currentRepository?`Release tagek lekérése: ${d.currentRepository}`:'Változott repositoryk feldolgozása...';if(d.phase==='REMOVING')text=`Törölt vagy archivált repository eltávolítása: ${d.currentRepository}`;if(d.phase==='COMPLETED')text='A katalógus frissítése elkészült.';if(d.phase==='FAILED')text='A katalógus frissítése hibával leállt.';const counter=d.organizationRepositoryCount?`${processed}/${changed} változott repo · ${removedDone}/${removed} eltávolítás · ${d.releaseCount||0} release tag · ${d.organizationRepositoryCount} repo az organizációban`:'Repositorylista lekérése';processing(true,text,counter);if(d.running){state.pollTimer=setTimeout(pollRefresh,600);return;}processing(false);if(d.successful){await loadCached(false);state.pendingRefreshDownloadItems=buildLatestMissingDownloadItems();showRefreshResult(d,state.pendingRefreshDownloadItems.length);}else{state.pendingRefreshDownloadItems=[];await showGithubConnectionError(new Error(d.errorMessage||'A katalógusfrissítés sikertelen.'));}}catch(e){processing(false);state.pendingRefreshDownloadItems=[];await showGithubConnectionError(e);}}
/**
 * Feldolgozza a build filters bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function buildFilters(){const types=[...new Set(state.rows.map(r=>r.formType).filter(Boolean))].sort();$('templateTypeFilter').innerHTML='<option value="">Minden űrlap</option>'+types.map(v=>`<option>${esc(v)}</option>`).join('');updateVersions();}
/**
 * Szinkronizálja vagy frissíti a update versions által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function updateVersions(){const type=$('templateTypeFilter').value;const el=$('templateVersionFilter');const versions=[...new Set(state.rows.filter(r=>!type||r.formType===type).map(r=>r.version).filter(Boolean))].sort();el.disabled=!type;el.innerHTML=type?'<option value="">Minden verzió</option>'+versions.map(v=>`<option>${esc(v)}</option>`).join(''):'<option value="">Előbb válassz űrlapt</option>';}
/**
 * Szinkronizálja vagy frissíti a apply által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function apply(){const q=$('templateSearchInput').value.trim().toLowerCase(),type=$('templateTypeFilter').value,version=$('templateVersionFilter').value,showLocal=$('localOnlyCheckbox').checked;state.filtered=state.rows.filter(r=>(!showLocal||Boolean(r.locallyAvailable))&&(!type||r.formType===type)&&(!version||r.version===version)&&(!q||Object.values(r).some(v=>String(v??'').toLowerCase().includes(q))));state.page=Math.min(state.page,Math.max(0,Math.ceil(state.filtered.length/state.pageSize)-1));render();}
/**
 * Megjeleníti vagy újrarendereli a render állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function render(){const start=state.page*state.pageSize,rows=state.filtered.slice(start,start+state.pageSize);$('templateTableBody').innerHTML=rows.length?rows.map(r=>{const key=`${r.repository}@@${r.releaseTag}`;return `<tr><td><input class="template-row-check" type="checkbox" data-key="${esc(key)}" ${state.selected.has(key)?'checked':''} ${r.releaseTag?'':'disabled'} title="${r.releaseTag?'Kijelölés művelethez':'Nincs release tag'}"></td><td><span class="template-form-cell">${r.locallyAvailable?`<a class="template-known-badge template-known-badge-local template-local-download" href="${esc(localBundleHref(r))}" title="Helyi nyomtatványcsomag letöltése">Helyi</a>`:'<span class="template-known-badge template-known-badge-github">Github</span>'}<a class="template-repo-link" href="${esc(r.repositoryUrl)}" target="_blank" rel="noopener">${esc(r.repository)}</a></span></td><td>${esc(r.version||'—')}</td><td>${esc(r.releaseTag||'—')}</td><td>${fmt(r.repositoryUpdatedAt)}</td><td>${esc(r.title||'—')} ${r.readmeUrl?`<a class="template-readme-link" href="${esc(r.readmeUrl)}" target="_blank" rel="noopener">README</a>`:''}</td><td>${esc(r.validityStart||'—')}</td><td>${esc(r.validityEnd||'—')}</td><td><button class="template-download-button" data-repo="${esc(r.repository)}" data-tag="${esc(r.releaseTag)}" ${r.releaseTag&&state.tokenConfigured?'':'disabled'} title="${state.tokenConfigured?'Release tag letöltése':'A GitHub token nincs beállítva'}">⇩</button></td></tr>`}).join(''):'<tr><td colspan="9">Nincs eltárolt találat.</td></tr>';$('templatePaginationInfo').textContent=state.filtered.length?`${state.filtered.length} találat · ${start+1}-${Math.min(start+rows.length,state.filtered.length)}`:'0 találat';$('templatePageIndicator').textContent=`${state.page+1} / ${Math.max(1,Math.ceil(state.filtered.length/state.pageSize))}`;const max=Math.max(0,Math.ceil(state.filtered.length/state.pageSize)-1);$('templateFirstPageButton').disabled=$('templatePrevPageButton').disabled=state.page===0;$('templateNextPageButton').disabled=$('templateLastPageButton').disabled=state.page>=max;$('downloadSelectedTemplatesButton').disabled=!state.tokenConfigured||state.selected.size===0;const deleteButton=$('deleteSelectedLocalTemplatesButton');if(deleteButton)deleteButton.disabled=![...state.selected.values()].some(item=>state.rows.some(row=>row.repository===item.repository&&row.releaseTag===item.tag&&row.locallyAvailable));}
/**
 * A <code>emptyDownloadResult</code> függvény a GitHub űrlapsablon-katalógus folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function buildLatestMissingDownloadItems(){
  const downloadableByRelease=new Map();
  for(const row of state.rows){
    const repository=String(row?.repository||'').trim();
    const tag=String(row?.releaseTag||'').trim();
    if(!repository||!tag||Boolean(row?.locallyAvailable)) continue;
    const key=`${repository}@@${tag}`;
    if(!downloadableByRelease.has(key)){
      downloadableByRelease.set(key,{repository,tag});
    }
  }
  return [...downloadableByRelease.values()];
}
function showRefreshResult(refreshStatus,downloadCandidateCount){
  const changed=refreshStatus?.changedRepositoryCount||0;
  const removed=refreshStatus?.removedRepositoryCount||0;
  const releases=refreshStatus?.releaseCount||0;
  $('templateRefreshResultSummary').textContent=`${changed} repository frissítve, ${removed} eltávolítva, ${releases} release tag feldolgozva.`;
  $('templateRefreshResultDownloadHint').textContent=downloadCandidateCount
    ? `${downloadCandidateCount} űrlapsablon tölthető le a friss katalógus alapján.`
    : 'Nincs letölthető új űrlapsablon.';
  $('templateRefreshResultDialog').hidden=false;
}
function closeRefreshResult(){
  $('templateRefreshResultDialog').hidden=true;
  const items=[...state.pendingRefreshDownloadItems];
  state.pendingRefreshDownloadItems=[];
  if(!items.length) return;
  state.selected.clear();
  items.forEach(item=>state.selected.set(`${item.repository}@@${item.tag}`,item));
  render();
  $('templateRefreshDownloadOfferMessage').textContent=`A letöltött katalógus file alapján ${items.length} db űrlapsablon tölthető le. Le kívánja tölteni az űrlapsablonokat?`;
  $('templateRefreshDownloadOfferDialog').hidden=false;
}
function hideRefreshDownloadOffer(){$('templateRefreshDownloadOfferDialog').hidden=true;}
async function confirmRefreshDownloadOffer(){
  hideRefreshDownloadOffer();
  await download([...state.selected.values()]);
}
function selectedLocalDeleteItems(){return [...state.selected.values()].filter(item=>state.rows.some(row=>row.repository===item.repository&&row.releaseTag===item.tag&&row.locallyAvailable));}
function showLocalDeleteConfirm(){const items=selectedLocalDeleteItems();if(!items.length){message('Nincs kijelölt, lokálisan letöltött release.',true);return;}$('templateLocalDeleteConfirmMessage').textContent=`${items.length} kijelölt lokális release törlődik a katalógusból és a telepített XSD, XPath és UIModel állományok közül.`;$('templateLocalDeleteConfirmDialog').hidden=false;}
function hideLocalDeleteConfirm(){$('templateLocalDeleteConfirmDialog').hidden=true;}
async function deleteSelectedLocal(){const items=selectedLocalDeleteItems();if(!items.length){hideLocalDeleteConfirm();return;}hideLocalDeleteConfirm();processing(true,'Lokális GitHub release-ek törlése...','Adatbázis és telepített állományok eltávolítása');try{const response=await fetch('/api/github-templates/local-delete',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({items})});if(!response.ok)throw new Error(await response.text());const result=await response.json();state.selected.clear();await loadCached(false);message(`Lokális törlés kész: ${result.deletedRepositoryCount||0} repository katalógusa és ${result.deletedFileSystemEntryCount||0} fájlrendszer-bejegyzés törölve. GitHub katalógusfrissítéssel ismét letölthető.`);}catch(error){message(`Lokális GitHub törlési hiba: ${error.message}`,true);}finally{processing(false);}}
function emptyDownloadResult(){return{downloadedCount:0,skippedCount:0,failedCount:0,repositories:[]};}
/**
 * A <code>mergeDownloadResult</code> függvény a GitHub űrlapsablon-katalógus folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} target a függvény target bemeneti értéke
 * @param {*} source a függvény source bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function mergeDownloadResult(target,source){
  target.downloadedCount+=(source.downloadedCount||0);
  target.skippedCount+=(source.skippedCount||0);
  target.failedCount+=(source.failedCount||0);
  target.repositories.push(...(source.repositories||[]));
  return target;
}
/**
 * A <code>appendDownloadFailure</code> függvény a GitHub űrlapsablon-katalógus folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} target a függvény target bemeneti értéke
 * @param {*} item a függvény item bemeneti értéke
 * @param {*} error a függvény error bemeneti értéke
 */
function appendDownloadFailure(target,item,error){
  target.failedCount+=1;
  target.repositories.push({repositoryName:item.repository,failedCount:1,tags:[{tagName:item.tag,status:'FAILED',targetDirectory:null,message:error?.message||String(error),installedArtifacts:[]}]});
}
/**
 * A <code>download</code> függvény a GitHub űrlapsablon-katalógus folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} items a függvény items bemeneti értéke
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function download(items,{automatic=false,forceOverride=null}={}){
  if(!state.tokenConfigured){showGithubTokenMissingDialog();return;}
  if(!items.length)return;
  const total=items.length;
  const force=forceOverride===null?$('forceDownloadCheckbox').checked:Boolean(forceOverride);
  const aggregate=emptyDownloadResult();
  processing(true,automatic?'Az automatikus repository-frissítés előkészítése...':'A kijelölt release tagek feldolgozásának előkészítése...',automatic?`0/${total} repository`:`0/${total} kijelölt release`);
  setProgress(0);
  try{
    for(let index=0;index<total;index+=1){
      const item=items[index];
      const position=index+1;
      processing(true,`Letöltés, kicsomagolás és telepítés: ${item.repository} · ${item.tag}`,automatic?`${position}/${total} repository`:`${position}/${total} kijelölt release`);
      setProgress(Math.round(index*100/total));
      try{
        const r=await fetch('/api/github-templates/download',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({items:[item],force})});
        if(!r.ok)throw new Error(await r.text());
        mergeDownloadResult(aggregate,await r.json());
      }catch(e){
        appendDownloadFailure(aggregate,item,e);
      }
      setProgress(Math.round(position*100/total));
    }
    processing(true,automatic?'Az automatikus repository-frissítés elkészült.':'A kijelölt release tagek feldolgozása elkészült.',automatic?`${total}/${total} repository`:`${total}/${total} kijelölt release`);
    message(`${automatic?'Automatikus letöltés':'Letöltés'} kész: ${aggregate.downloadedCount||0} letöltve, ${aggregate.skippedCount||0} már helyben volt, ${aggregate.failedCount||0} hibás.`,aggregate.failedCount>0);
    showDownloadResult(aggregate);
    state.selected.clear();
    await loadCached(false);
    render();
  }finally{
    processing(false);
  }
}
document.addEventListener('change',e=>{if(e.target.matches('.template-row-check')){const key=e.target.dataset.key;const [repository,tag]=key.split('@@');e.target.checked?state.selected.set(key,{repository,tag}):state.selected.delete(key);render();}});
document.addEventListener('click',e=>{const b=e.target.closest('.template-download-button');if(b)download([{repository:b.dataset.repo,tag:b.dataset.tag}]);});
$('deleteSelectedLocalTemplatesButton')?.addEventListener('click',showLocalDeleteConfirm);$('cancelTemplateLocalDeleteButton')?.addEventListener('click',hideLocalDeleteConfirm);$('confirmTemplateLocalDeleteButton')?.addEventListener('click',deleteSelectedLocal);$('closeGithubTokenMissingButton')?.addEventListener('click',hideGithubTokenMissingDialog);$('githubTokenQuickInput')?.addEventListener('input',updateGithubTokenQuickButton);$('githubTokenQuickInput')?.addEventListener('keydown',e=>{if(e.key==='Enter'&&!e.isComposing){e.preventDefault();saveGithubTokenFromDialog();}});$('saveGithubTokenQuickButton')?.addEventListener('click',saveGithubTokenFromDialog);$('closeTemplateDownloadResultButton').addEventListener('click',hideDownloadResult);$('closeTemplateRefreshResultButton')?.addEventListener('click',closeRefreshResult);$('cancelTemplateRefreshDownloadOfferButton')?.addEventListener('click',hideRefreshDownloadOffer);$('confirmTemplateRefreshDownloadOfferButton')?.addEventListener('click',confirmRefreshDownloadOffer);$('closeTemplateNoChangesButton').addEventListener('click',hideNoChanges);$('downloadSelectedTemplatesButton').addEventListener('click',()=>download([...state.selected.values()]));$('refreshTemplatesButton').addEventListener('click',reloadLocalList);$('manualGithubRefreshButton').addEventListener('click',()=>checkChanges(false));$('localOnlyCheckbox').addEventListener('change',()=>{state.page=0;apply();});$('confirmTemplateRefreshButton').addEventListener('click',startRefresh);$('cancelTemplateRefreshButton').addEventListener('click',hideRefreshConfirm);$('templateSearchInput').addEventListener('input',()=>{state.page=0;apply();});$('templateTypeFilter').addEventListener('change',()=>{updateVersions();state.page=0;apply();});$('templateVersionFilter').addEventListener('change',()=>{state.page=0;apply();});$('templatePageSizeSelect').addEventListener('change',e=>{state.pageSize=Number(e.target.value)||20;state.page=0;render();});$('selectAllTemplatesCheckbox').addEventListener('change',e=>{const start=state.page*state.pageSize;state.filtered.slice(start,start+state.pageSize).filter(r=>r.releaseTag).forEach(r=>{const key=`${r.repository}@@${r.releaseTag}`;e.target.checked?state.selected.set(key,{repository:r.repository,tag:r.releaseTag}):state.selected.delete(key);});render();});$('templateFirstPageButton').onclick=()=>{state.page=0;render();};$('templatePrevPageButton').onclick=()=>{state.page=Math.max(0,state.page-1);render();};$('templateNextPageButton').onclick=()=>{state.page++;render();};$('templateLastPageButton').onclick=()=>{state.page=Math.max(0,Math.ceil(state.filtered.length/state.pageSize)-1);render();};
loadCached(false).then(data=>{if(!data.tokenConfigured)showGithubTokenMissingDialogOnce();});
