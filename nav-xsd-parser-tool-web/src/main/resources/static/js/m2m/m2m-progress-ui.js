/**
 * A <code>escapeHtml</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
/**
 * @module m2m/m2m-progress-ui
 *
 * A M2M beküldési és csatolmánykezelési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A kliensoldali engedélyezés csak felhasználói visszajelzés; a tényleges M2M jogosultsági kontroll szerveroldali.
 */

function escapeHtml(value){ return String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
/**
 * Előkészíti és elindítja a create m2m progress állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} title a függvény title bemeneti értéke
 * @param {*} phases a függvény phases bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function createM2mProgress(title, phases){
  const overlay=document.createElement('div'); overlay.className='large-xml-process-overlay m2m-process-overlay';
  overlay.innerHTML=`<div class="large-xml-process-dialog m2m-process-dialog" role="status" aria-live="polite"><div class="large-xml-process-spinner"></div><h2>${escapeHtml(title)}</h2><p class="m2m-process-phase"></p><progress max="100" value="0"></progress><ol class="m2m-process-steps"></ol></div>`;
  document.body.append(overlay); const started=Date.now();
    /**
   * Szinkronizálja vagy frissíti a update által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} index az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
   * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
   */
const update=(index,message)=>{ const pct=Math.round(((index+1)/phases.length)*100); overlay.querySelector('progress').value=pct; overlay.querySelector('.m2m-process-phase').textContent=message||phases[index]; overlay.querySelector('.m2m-process-steps').innerHTML=phases.map((p,i)=>`<li class="${i<index?'done':i===index?'active':''}">${escapeHtml(p)}</li>`).join(''); };
  update(0, phases[0]);
  return { update,   /**
   * Elrejti vagy lezárja a close felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
close(){overlay.remove();},   /**
   * A <code>elapsed</code> függvény a M2M beküldési és csatolmánykezelési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
elapsed(){return Date.now()-started;} };
}
/**
 * Megjeleníti vagy újrarendereli a show m2m result állapotát a felhasználói felületen.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} data a függvény data bemeneti értéke
 * @param {*} success a függvény success bemeneti értéke
 */
export function showM2mResult(data, success=true){
  const overlay=document.createElement('div'); overlay.className='m2m-result-overlay';
  const rows=[['Eredmény',success?'Sikeres':'Sikertelen'],['Befogadás időpontja',data?.navBefogadasIdopontja],['Bizonylat státusza',data?.navStatus],['Érkeztetési szám',data?.navErkeztetesiSzam],['Eredménykód',data?.resultCode],['Eredményüzenet',data?.resultMessage],['Megjegyzés',data?.navMegjegyzes],['Validációs hibák',data?.navValidaciosHibak],['Beküldési mód',data?.fastTrackSubmissionUsed?'Gyorsított pályás beküldés':'Normál beküldés'],['Időtartam',data?.submissionDurationMs!=null?`${data.submissionDurationMs} ms`:null]];
  overlay.innerHTML=`<div class="m2m-result-dialog"><h2 class="${success?'success':'error'}">Beküldés ${success?'sikeres':'sikertelen'}</h2><table><tbody>${rows.map(([k,v])=>`<tr><th>${escapeHtml(k)}</th><td>${escapeHtml(v||'–')}</td></tr>`).join('')}</tbody></table><button type="button">Bezárás</button></div>`;
  overlay.querySelector('button').addEventListener('click',()=>overlay.remove()); document.body.append(overlay);
}

/**
 * Megjeleníti vagy újrarendereli a show m2m operation result állapotát a felhasználói felületen.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} arg1 a függvény arg1 bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function showM2mOperationResult({ title, success = true, summary = '', rows = [], rowActions = {}, footerActions = [] } = {}){
  const overlay=document.createElement('div');
  overlay.className='m2m-result-overlay';
  const normalizedRows=(rows || []).filter(([,value]) => value !== undefined && value !== null && String(value) !== '');
  const actionEntries=Object.entries(rowActions || {});
  overlay.innerHTML=`<div class="m2m-result-dialog m2m-operation-result-dialog" role="dialog" aria-modal="true" tabindex="-1">
    <h2 class="${success?'success':'error'}">${escapeHtml(title || (success ? 'Művelet sikeres' : 'Művelet sikertelen'))}</h2>
    ${summary ? `<p class="m2m-result-summary">${escapeHtml(summary)}</p>` : ''}
    <table><tbody>${normalizedRows.map(([key,value])=>{
      const hasAction=Object.prototype.hasOwnProperty.call(rowActions || {},key);
      return `<tr><th>${escapeHtml(key)}</th><td>${escapeHtml(value)}${hasAction?` <button type="button" class="secondary mini-button m2m-inline-result-action" data-row-action="${escapeHtml(key)}">Megtekintés</button>`:''}</td></tr>`;
    }).join('')}</tbody></table>
    <div class="m2m-result-actions">
      ${(footerActions || []).map((action,index)=>`<button type="button" class="${escapeHtml(action?.className || 'secondary')}" data-footer-action="${index}">${escapeHtml(action?.label || 'Művelet')}</button>`).join('')}
      <button type="button" class="secondary" data-close-result>Bezárás</button>
    </div>
  </div>`;
    /**
   * Elrejti vagy lezárja a close felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
const close=()=>overlay.remove();
  overlay.querySelector('[data-close-result]')?.addEventListener('click',close);
  actionEntries.forEach(([key,handler])=>{
    overlay.querySelector(`[data-row-action="${CSS.escape(key)}"]`)?.addEventListener('click',async event=>{
      event.currentTarget.disabled=true;
      try{ await handler?.({close,overlay}); }
      finally{ if(event.currentTarget?.isConnected) event.currentTarget.disabled=false; }
    });
  });
  (footerActions || []).forEach((action,index)=>{
    overlay.querySelector(`[data-footer-action="${index}"]`)?.addEventListener('click',async event=>{
      event.currentTarget.disabled=true;
      try{ await action?.handler?.({close,overlay}); }
      finally{ if(event.currentTarget?.isConnected) event.currentTarget.disabled=false; }
    });
  });
  overlay.addEventListener('click',event=>{ if(event.target===overlay) close(); });
  overlay.addEventListener('keydown',event=>{ if(event.key==='Escape') close(); });
  document.body.append(overlay);
  overlay.querySelector('[data-close-result]')?.focus();
  return {close,overlay};
}

