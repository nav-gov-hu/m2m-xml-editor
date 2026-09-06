/**
 * @module pages/users
 *
 * A oldalspecifikus felhasználói felület- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

(()=>{let users=[],page=1;/**
 * A <code>$</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} id a célobjektum technikai azonosítója
 */
const $=id=>document.getElementById(id),rows=$('userRows'),search=$('userSearch'),pageSize=$('userPageSize');/**
 * A <code>esc</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} s a függvény s bemeneti értéke
 */
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));/**
 * Betölti vagy lekéri a load művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function load(){const r=await fetch('/api/users');if(!r.ok)throw new Error(await r.text());users=await r.json();render();}/**
 * A <code>filtered</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function filtered(){const q=(search.value||'').toLowerCase();return users.filter(u=>!q||`${u.username} ${u.displayName||''} ${u.email||''} ${(u.roles||[]).join(' ')}`.toLowerCase().includes(q));}/**
 * Megjeleníti vagy újrarendereli a render állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function render(){const data=filtered(),size=Number(pageSize.value)||20,pages=Math.max(1,Math.ceil(data.length/size));page=Math.min(Math.max(page,1),pages);const start=(page-1)*size;rows.innerHTML=data.slice(start,start+size).map(u=>`<tr><td>${esc(u.username)}</td><td>${esc(u.displayName||'-')}</td><td>${esc(u.email||'-')}</td><td>${esc((u.roles||[]).join(', '))}</td><td>${u.enabled?'Igen':'Nem'}</td><td><button class="secondary mini-button" data-edit="${u.id}">Szerkesztés</button> <button class="secondary mini-button" data-toggle="${u.id}" data-enabled="${u.enabled}">${u.enabled?'Letiltás':'Engedélyezés'}</button></td></tr>`).join('')||'<tr><td colspan="6">Nincs találat.</td></tr>';$('userPaginationInfo').textContent=data.length?`${start+1}–${Math.min(start+size,data.length)} / ${data.length} felhasználó`:'0 felhasználó';$('userPageIndicator').textContent=`${page} / ${pages}`;$('userFirstPage').disabled=$('userPrevPage').disabled=page<=1;$('userNextPage').disabled=$('userLastPage').disabled=page>=pages;}$('newUser').onclick=()=>location.href='/user-edit.html';search.oninput=()=>{page=1;render()};pageSize.onchange=()=>{page=1;render()};$('userFirstPage').onclick=()=>{page=1;render()};$('userPrevPage').onclick=()=>{page--;render()};$('userNextPage').onclick=()=>{page++;render()};$('userLastPage').onclick=()=>{page=999999;render()};rows.onclick=async e=>{const edit=e.target.dataset.edit;if(edit)return location.href=`/user-edit.html?id=${edit}`;const id=e.target.dataset.toggle;if(id){const enabled=e.target.dataset.enabled!=='true';const r=await fetch(`/api/users/${id}/enabled`,{method:'PATCH',headers:{'Content-Type':'application/json'},body:JSON.stringify({enabled})});if(!r.ok){let x;try{x=await r.json()}catch{};return window.showAppToast?.(x?.message||'A módosítás sikertelen.','error')}await load();}};load().catch(e=>rows.innerHTML=`<tr><td colspan="6">Betöltési hiba: ${esc(e.message)}</td></tr>`);})();