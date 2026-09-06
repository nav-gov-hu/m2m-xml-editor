/**
 * @module core/frame-bust
 *
 * A közös frontend infrastruktúra- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

(() => {
  'use strict';
  if (window.top !== window.self) {
    document.documentElement.style.display = 'none';
  }
})();
