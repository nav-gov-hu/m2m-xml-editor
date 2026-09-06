/**
 * @module pages/console-log
 *
 * A oldalspecifikus felhasználói felület- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

(()=>{const out=document.getElementById('consoleOutput'),status=document.getElementById('consoleStatus'),filter=document.getElementById('consoleFilter'),level=document.getElementById('consoleLevel'),auto=document.getElementById('consoleAutoScroll'),pause=document.getElementById('consolePause');let paused=false,entries=[];/**
 * A <code>fmt</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} e a függvény e bemeneti értéke
 */
const fmt=e=>`${e.timestamp||''} ${String(e.level||'').padEnd(5)} ${e.logger||''} - ${e.message||''}`;/**
 * Megjeleníti vagy újrarendereli a render állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function render(){const needle=filter.value.toLowerCase();out.textContent=entries.filter(e=>(!level.value||e.level===level.value)&&(!needle||fmt(e).toLowerCase().includes(needle))).map(fmt).join('\n');if(auto.checked)out.scrollTop=out.scrollHeight;}fetch('/api/admin/console-log?limit=600').then(r=>r.json()).then(v=>{entries=v;render();});const es=new EventSource('/api/admin/console-log/stream');es.addEventListener('connected',()=>status.textContent='Élő kapcsolat aktív');es.addEventListener('log',ev=>{if(paused)return;entries.push(JSON.parse(ev.data));if(entries.length>3000)entries.shift();render();});es.onerror=()=>status.textContent='Újracsatlakozás…';filter.oninput=render;level.onchange=render;pause.onclick=()=>{paused=!paused;pause.textContent=paused?'Folytatás':'Szünet';status.textContent=paused?'Megjelenítés szüneteltetve':'Élő kapcsolat aktív';};document.getElementById('consoleClear').onclick=()=>{entries=[];render();};})();
