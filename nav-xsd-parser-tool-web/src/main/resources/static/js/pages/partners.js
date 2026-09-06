/**
 * @module pages/partners
 *
 * A oldalspecifikus felhasználói felület- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

(()=>{
  let partners=[];
  let page=1;
  const rows=document.getElementById('partnerRows');
  const search=document.getElementById('partnerSearch');
  const pageSize=document.getElementById('partnerPageSize');
  const info=document.getElementById('partnerPaginationInfo');
  const indicator=document.getElementById('partnerPageIndicator');
  const first=document.getElementById('partnerFirstPage');
  const prev=document.getElementById('partnerPrevPage');
  const next=document.getElementById('partnerNextPage');
  const last=document.getElementById('partnerLastPage');
    /**
   * A <code>esc</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} s a függvény s bemeneti értéke
   */
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    /**
   * Betölti vagy lekéri a load művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function load(){const r=await fetch('/api/partners');partners=await r.json();page=1;render();}
    /**
   * A <code>filtered</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function filtered(){const q=(search.value||'').trim().toLowerCase();return partners.filter(p=>!q||`${p.taxNumber||''} ${p.name||''} ${p.email||''} ${p.phone||''}`.toLowerCase().includes(q));}
    /**
   * Megjeleníti vagy újrarendereli a render állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function render(){
    const data=filtered();const size=Number(pageSize.value)||20;const pages=Math.max(1,Math.ceil(data.length/size));page=Math.min(Math.max(1,page),pages);
    const start=(page-1)*size;const visible=data.slice(start,start+size);
    rows.innerHTML=visible.map(p=>`<tr><td>${esc(p.taxNumber)}</td><td>${esc(p.name)}</td><td>${esc(p.email||'-')}</td><td>${esc(p.phone||'-')}</td><td>${p.active?'Igen':'Nem'}</td><td><button data-edit="${p.id}" class="secondary mini-button">Szerkesztés</button></td></tr>`).join('')||'<tr><td colspan="6">Nincs találat.</td></tr>';
    info.textContent=data.length?`${start+1}–${Math.min(start+size,data.length)} / ${data.length} partner`:'0 partner';indicator.textContent=`${page} / ${pages}`;
    first.disabled=prev.disabled=page<=1;next.disabled=last.disabled=page>=pages;
  }
  document.getElementById('newPartner').onclick=()=>{window.location.href='/partner-edit.html';};
  search.oninput=()=>{page=1;render();};pageSize.onchange=()=>{page=1;render();};first.onclick=()=>{page=1;render();};prev.onclick=()=>{page--;render();};next.onclick=()=>{page++;render();};last.onclick=()=>{page=Math.max(1,Math.ceil(filtered().length/(Number(pageSize.value)||20)));render();};
  rows.onclick=e=>{const id=e.target.dataset.edit;if(id)window.location.href=`/partner-edit.html?id=${encodeURIComponent(id)}`;};
  load().catch(err=>{rows.innerHTML=`<tr><td colspan="6">Betöltési hiba: ${esc(err.message)}</td></tr>`;});
})();
