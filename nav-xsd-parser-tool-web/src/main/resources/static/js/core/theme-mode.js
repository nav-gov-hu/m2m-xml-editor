/**
 * @module core/theme-mode
 *
 * A közös frontend infrastruktúra- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

(function () {
  'use strict';

  const STORAGE_KEY = 'nav-xsd-parser-color-scheme';
  const DARK = 'dark';
  const LIGHT = 'light';

    /**
   * Betölti vagy lekéri a read stored mode művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function readStoredMode() {
    try {
      const value = localStorage.getItem(STORAGE_KEY);
      return value === DARK ? DARK : LIGHT;
    } catch (_ignored) {
      return LIGHT;
    }
  }

    /**
   * Szinkronizálja vagy frissíti a update logos által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} mode a függvény mode bemeneti értéke
   */
function updateLogos(mode) {
    const logoSource = mode === DARK ? '/images/SET_logo_dark.png' : '/images/SET_logo.png';
    document.querySelectorAll('img[src$="/images/SET_logo.png"], img[src$="/images/SET_logo_dark.png"]')
      .forEach(function (logo) {
        if (logo.getAttribute('src') !== logoSource) {
          logo.setAttribute('src', logoSource);
        }
      });
  }

    /**
   * Szinkronizálja vagy frissíti a apply mode által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} mode a függvény mode bemeneti értéke
   * @param {*} persist a függvény persist bemeneti értéke
   */
function applyMode(mode, persist) {
    const normalized = mode === DARK ? DARK : LIGHT;
    document.documentElement.dataset.colorScheme = normalized;
    document.documentElement.style.colorScheme = normalized;
    if (document.body) {
      document.body.dataset.colorScheme = normalized;
    }
    updateLogos(normalized);
    if (persist) {
      try {
        localStorage.setItem(STORAGE_KEY, normalized);
      } catch (_ignored) {
        // A megjelenés akkor is átváltható, ha a böngésző tiltja a localStorage használatát.
      }
    }
    updateToggle(normalized);
    window.dispatchEvent(new CustomEvent('nav-color-scheme-changed', { detail: { mode: normalized } }));
  }

    /**
   * Szinkronizálja vagy frissíti a update toggle által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} mode a függvény mode bemeneti értéke
   */
function updateToggle(mode) {
    const button = document.getElementById('colorSchemeToggle');
    if (!button) return;
    const dark = mode === DARK;
    button.setAttribute('aria-pressed', String(dark));
    button.setAttribute('aria-label', dark ? 'Világos megjelenés bekapcsolása' : 'Sötét megjelenés bekapcsolása');
    button.setAttribute('title', dark ? 'Világos megjelenés' : 'Sötét megjelenés');
    button.innerHTML = `<span class="theme-toggle-icon" aria-hidden="true">${dark ? '☀' : '☾'}</span><span class="theme-toggle-label">${dark ? 'Világos mód' : 'Sötét mód'}</span>`;
  }

    /**
   * Előkészíti és elindítja a create toggle állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function createToggle() {
    if (document.getElementById('colorSchemeToggle')) return;
    const button = document.createElement('button');
    button.type = 'button';
    button.id = 'colorSchemeToggle';
    button.className = 'theme-mode-toggle';
    button.addEventListener('click', function () {
      const current = document.documentElement.dataset.colorScheme === DARK ? DARK : LIGHT;
      applyMode(current === DARK ? LIGHT : DARK, true);
    });

    const headerInner = document.querySelector('.site-header-inner');
    const securityArea = headerInner && headerInner.querySelector('#securityHeaderArea, .security-header-area, .site-header-right.security-user-area');
    if (headerInner) {
      if (securityArea) {
        headerInner.insertBefore(button, securityArea);
      } else {
        headerInner.appendChild(button);
      }
    } else {
      button.classList.add('theme-mode-toggle-floating');
      document.body.appendChild(button);
    }
    updateToggle(document.documentElement.dataset.colorScheme || LIGHT);
  }

  const initialMode = readStoredMode();
  applyMode(initialMode, false);

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      if (document.body) document.body.dataset.colorScheme = initialMode;
      updateLogos(initialMode);
      createToggle();
    }, { once: true });
  } else {
    if (document.body) document.body.dataset.colorScheme = initialMode;
    updateLogos(initialMode);
    createToggle();
  }

  window.addEventListener('storage', function (event) {
    if (event.key === STORAGE_KEY) applyMode(event.newValue === DARK ? DARK : LIGHT, false);
  });

  window.navThemeMode = Object.freeze({
    getMode: function () { return document.documentElement.dataset.colorScheme || LIGHT; },
    setMode: function (mode) { applyMode(mode, true); },
    toggle: function () { applyMode(document.documentElement.dataset.colorScheme === DARK ? LIGHT : DARK, true); }
  });
})();
