/**
 * @module pages/audit-log
 *
 * A oldalspecifikus felhasználói felület- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

(()=>{let rows=[];const body=document.getElementById('auditRows'),filter=document.getElementById('auditFilter');/**
 * A <code>esc</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} v a függvény v bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
const esc=v=>{const d=document.createElement('div');d.textContent=v??'';return d.innerHTML;};/**
 * Megjeleníti vagy újrarendereli a render állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function render(){const n=filter.value.toLowerCase();body.innerHTML=rows.filter(r=>!n||JSON.stringify(r).toLowerCase().includes(n)).map(r=>`<tr><td>${esc(r.createdAt)}</td><td>${esc(r.operationType)}</td><td>${esc(r.username)}</td><td>${esc(r.result)}</td><td>${esc(r.message)}</td></tr>`).join('');}/**
 * Betölti vagy lekéri a load művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function load(){const r=await fetch('/api/admin/audit-log?limit=500');rows=await r.json();render();}filter.oninput=render;document.getElementById('auditRefresh').onclick=load;load();})();
