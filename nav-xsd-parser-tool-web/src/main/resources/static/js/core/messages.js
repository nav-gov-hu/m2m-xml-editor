/**
 * Megjeleníti vagy újrarendereli a show message állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
 * @param {*} type a függvény type bemeneti értéke
 */
/**
 * @module core/messages
 *
 * A közös frontend infrastruktúra- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

export function showMessage(message, type = 'info'){
  if(window.NavFormRuntime?.showMessage){
    window.NavFormRuntime.showMessage(message, type);
    return;
  }
  const level = type === 'error' ? 'error' : type === 'warning' ? 'warn' : 'log';
  console[level](message);
}
