/**
 * @module core/ui-shell
 *
 * A közös frontend infrastruktúra- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

(function(){
    /**
   * Ellenőrzi a is login page feltételeit, és a hívó számára döntési eredményt állít elő.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function isLoginPage(){
    return window.location.pathname.endsWith('/login.html') || window.location.pathname === '/login';
  }

    /**
   * A <code>loginUrlWithExpiredMessage</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function loginUrlWithExpiredMessage(){
    const target = new URL('/login.html', window.location.origin);
    target.searchParams.set('sessionExpired', 'true');
    const current = `${window.location.pathname}${window.location.search || ''}${window.location.hash || ''}`;
    if(current && !current.includes('/login.html')){
      target.searchParams.set('redirect', current);
    }
    return target.toString();
  }

    /**
   * A <code>navHandleSessionExpired</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function navHandleSessionExpired(){
    if(isLoginPage()) return;
    try{
      sessionStorage.setItem('nav-session-expired-message', 'A munkamenet lejárt. Jelentkezz be újra.');
    }catch(_ignored){}
    window.location.replace(loginUrlWithExpiredMessage());
  }

  const nativeFetch = window.fetch ? window.fetch.bind(window) : null;
  if(nativeFetch && !window.__navSessionFetchWrapped){
    window.__navSessionFetchWrapped = true;
    window.fetch = async function(input, init){
      const response = await nativeFetch(input, init);
      try{
        const url = typeof input === 'string' ? input : (input && input.url ? input.url : '');
        const resolved = new URL(url, window.location.origin);
        const isApi = resolved.pathname.startsWith('/api/');
        if(response.status === 401 && isApi && !isLoginPage()){
          navHandleSessionExpired();
        }
      }catch(_ignored){}
      return response;
    };
  }

  window.navHandleSessionExpired = navHandleSessionExpired;
})();

/**
 * A <code>ensureShellSecurityHeaderArea</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
function ensureShellSecurityHeaderArea(){
  const headerInner = document.querySelector('.site-header-inner');
  if(!headerInner) return null;

  let area = headerInner.querySelector('#securityHeaderArea')
    || headerInner.querySelector('.security-header-area')
    || headerInner.querySelector('.site-header-right.security-user-area');

  if(!area){
    area = document.createElement('div');
    area.id = 'securityHeaderArea';
    area.hidden = true;
    headerInner.appendChild(area);
  }

  area.classList.add('security-header-area', 'site-header-right', 'security-user-area');
  area.setAttribute('aria-live', 'polite');

  let username = area.querySelector('#securityHeaderUsername');
  let logoutForm = area.querySelector('form[action="/logout"]');
  let logoutButton = area.querySelector('button[type="submit"]');

  if(!username || !logoutForm || !logoutButton){
    area.innerHTML = `
      <div class="security-user-pill" title="Bejelentkezett felhasználó">
        <span class="security-user-icon" aria-hidden="true">👤</span>
        <span class="security-user-label">Felhasználó:</span>
        <strong class="security-user-name" id="securityHeaderUsername">-</strong>
      </div>
      <form class="security-logout-form" method="post" action="/logout">
        <button type="submit" class="security-logout-button" title="Kijelentkezés">Kijelentkezés</button>
      </form>
    `;
  } else {
    logoutForm.classList.add('security-logout-form');
    logoutButton.classList.add('security-logout-button');
    username.classList.add('security-user-name');
  }

  return area;
}

/**
 * Szinkronizálja vagy frissíti a apply shell role based menu visibility által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
 * @param {*} user a függvény user bemeneti értéke
 */
function applyShellRoleBasedMenuVisibility(user){
  const roles = Array.isArray(user?.roles) ? user.roles.map(role => String(role).toUpperCase()) : [];
  const isStandalone = String(user?.mode || '').toUpperCase() === 'STANDALONE';
  const isAdmin = isStandalone || roles.includes('ROLE_ADMIN') || roles.includes('ADMIN');
  document.querySelectorAll('.header-nav a[href="/validate.html"], .header-nav a[href="/xpath-validator.html"]').forEach(link => {
    link.hidden = true;
    link.classList.add('hidden-by-role');
    link.style.display = 'none';
    link.setAttribute('aria-hidden', 'true');
    link.tabIndex = -1;
  });
  document.querySelectorAll('.header-nav a[href="/admin.html"], .header-nav a[href="/users.html"], .header-nav a[href="/configuration.html"], .header-nav a[href="/console-log.html"], .header-nav a[href="/audit-log.html"]').forEach(link => {
    link.hidden = !isAdmin;
    link.classList.toggle('hidden-by-role', !isAdmin);
    link.style.display = isAdmin ? '' : 'none';
    link.setAttribute('aria-hidden', isAdmin ? 'false' : 'true');
    link.tabIndex = isAdmin ? 0 : -1;
  });
}

/**
 * Szinkronizálja vagy frissíti a update shell security header által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {Promise<void>} a folyamat befejeződését jelző Promise
 */
async function updateShellSecurityHeader(){
  const area = ensureShellSecurityHeaderArea();
  if(!area) return;
  try{
    const response = await fetch('/api/security/current-user', { cache: 'no-store', credentials: 'same-origin' });
    if(!response.ok){
      area.hidden = true;
      applyShellRoleBasedMenuVisibility({ mode: 'MULTI_USER', roles: [] });
      return;
    }
    const user = await response.json();
    applyShellRoleBasedMenuVisibility(user);
    const multiUser = String(user.mode || '').toUpperCase() === 'MULTI_USER';
    const authenticated = user.authenticated === true;
    if(!multiUser || !authenticated){
      area.hidden = true;
      return;
    }
    const usernameEl = area.querySelector('#securityHeaderUsername');
    if(usernameEl) usernameEl.textContent = user.username || 'Felhasználó';
    area.hidden = false;
  }catch(error){
    console.warn('A bejelentkezett felhasználó adatai nem tölthetők be.', error);
    area.hidden = true;
    applyShellRoleBasedMenuVisibility({ mode: 'MULTI_USER', roles: [] });
  }
}

/**
 * Feldolgozza a normalize primary navigation order bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 */
function normalizePrimaryNavigationOrder(){
  document.querySelectorAll('.header-nav.classic-nav').forEach(nav => {
  document.querySelectorAll('.system-menu-panel').forEach(panel => {
    if(!panel.querySelector('a[href="/users.html"]')){
      const link=document.createElement('a'); link.href='/users.html'; link.textContent='Felhasználók';
      const admin=panel.querySelector('a[href="/admin.html"]'); panel.insertBefore(link, admin || null);
    }
    if(!panel.querySelector('a[href="/configuration.html"]')){
      const link=document.createElement('a'); link.href='/configuration.html'; link.textContent='Konfiguráció';
      const admin=panel.querySelector('a[href="/admin.html"]'); panel.insertBefore(link, admin || null);
    }
    if(!panel.querySelector('a[href="/console-log.html"]')){ const link=document.createElement('a'); link.href='/console-log.html'; link.textContent='Konzolnapló'; const admin=panel.querySelector('a[href="/admin.html"]'); panel.insertBefore(link, admin || null); }
    if(!panel.querySelector('a[href="/audit-log.html"]')){ const link=document.createElement('a'); link.href='/audit-log.html'; link.textContent='Auditnapló'; const admin=panel.querySelector('a[href="/admin.html"]'); panel.insertBefore(link, admin || null); }
  });
    const xmlFiles = nav.querySelector('[data-tab="xmlFilesTab"]');
    const templates = nav.querySelector('[data-tab="githubTemplatesTab"]');
    const form = nav.querySelector('[data-tab="formTab"]');
    const partners = nav.querySelector('[data-tab="partnersTab"]');
    const system = nav.querySelector('.system-menu');
    [xmlFiles, templates, form, partners, system].forEach(item => {
      if(item) nav.appendChild(item);
    });
  });
}

document.addEventListener('DOMContentLoaded', () => {
  normalizePrimaryNavigationOrder();
  const densitySelect = document.getElementById('densityModeSelect');
  const body = document.body;
  const key = 'nav-xsd-parser-density';
  const saved = localStorage.getItem(key);
  if (saved && densitySelect) densitySelect.value = saved;
    /**
   * Szinkronizálja vagy frissíti a apply density által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   */
const applyDensity = (value) => {
    body.dataset.density = value || 'dense';
    localStorage.setItem(key, body.dataset.density);
  };
  if (densitySelect) {
    applyDensity(densitySelect.value);
    densitySelect.addEventListener('change', () => applyDensity(densitySelect.value));
  }
  updateShellSecurityHeader();
});



(function(){
    /**
   * A <code>ensureToastContainer</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @returns {*} a feldolgozás eredménye
   */
function ensureToastContainer(){
    let container = document.getElementById('appToastContainer');
    if(container) return container;
    container = document.createElement('div');
    container.id = 'appToastContainer';
    container.className = 'app-toast-container';
    container.setAttribute('aria-live', 'polite');
    container.setAttribute('aria-atomic', 'false');
    document.body.appendChild(container);
    return container;
  }

    /**
   * Feldolgozza a normalize toast type bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} type a függvény type bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function normalizeToastType(type){
    const normalized = String(type || 'info').toLowerCase();
    if(['success', 'error', 'warning', 'info'].includes(normalized)) return normalized;
    return 'info';
  }

    /**
   * Megjeleníti vagy újrarendereli a show toast állapotát a felhasználói felületen.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
   * @param {*} type a függvény type bemeneti értéke
   * @param {*} options a művelet opcionális beállításai
   */
function showToast(message, type, options = {}){
    const text = String(message || '').trim();
    if(!text) return;
    const container = ensureToastContainer();
    const normalized = normalizeToastType(type);
    const toast = document.createElement('div');
    toast.className = `app-toast app-toast-${normalized}`;
    toast.setAttribute('role', normalized === 'error' ? 'alert' : 'status');
    toast.innerHTML = `
      <span class="app-toast-icon" aria-hidden="true">${normalized === 'success' ? '✓' : normalized === 'error' ? '!' : normalized === 'warning' ? '⚠' : 'i'}</span>
      <span class="app-toast-message"></span>
      <button type="button" class="app-toast-close" aria-label="Értesítés bezárása">×</button>`;
    const messageEl = toast.querySelector('.app-toast-message');
    if(messageEl) messageEl.textContent = text;
        /**
     * Elrejti vagy lezárja a close felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     */
const close = () => {
      toast.classList.add('is-closing');
      setTimeout(() => toast.remove(), 180);
    };
    const closeButton = toast.querySelector('.app-toast-close');
    if(closeButton) closeButton.addEventListener('click', close);
    container.appendChild(toast);
    if(options?.persistent !== true){
      const timeout = normalized === 'error' ? 6500 : 4200;
      window.setTimeout(close, timeout);
    }
  }

    /**
   * A <code>ensureDialogHost</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @returns {*} a feldolgozás eredménye
   */
function ensureDialogHost(){
    let host = document.getElementById('appDialogHost');
    if(host) return host;
    host = document.createElement('div');
    host.id = 'appDialogHost';
    document.body.appendChild(host);
    return host;
  }

    /**
   * Megjeleníti vagy újrarendereli a show dialog állapotát a felhasználói felületen.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} options a művelet opcionális beállításai
   * @returns {*} a feldolgozás eredménye
   */
function showDialog(options){
    const opts = Object.assign({
      title: 'Megerősítés',
      message: '',
      confirmText: 'Rendben',
      cancelText: 'Mégsem',
      variant: 'default',
      textarea: false,
      fields: null,
      label: '',
      defaultValue: '',
      placeholder: '',
      showConfirm: true,
      showCancel: true,
      eyebrow: 'Megerősítés'
    }, options || {});
    return new Promise(resolve => {
      const host = ensureDialogHost();
      const modal = document.createElement('div');
      modal.className = `app-dialog-modal app-dialog-${String(opts.variant || 'default').toLowerCase()}`;
      modal.innerHTML = `
        <div class="app-dialog-backdrop" data-app-dialog-cancel="true"></div>
        <section class="app-dialog-card" role="dialog" aria-modal="true" aria-labelledby="appDialogTitle">
          <button type="button" class="app-dialog-close" data-app-dialog-cancel="true" aria-label="Bezárás">×</button>
          <p class="eyebrow app-dialog-eyebrow">Megerősítés</p>
          <h2 id="appDialogTitle"></h2>
          <p class="app-dialog-message"></p>
          <div class="app-dialog-input-wrap" hidden>
            <label class="app-dialog-label" for="appDialogInput"></label>
            <textarea id="appDialogInput" class="app-dialog-input" rows="5" maxlength="1000"></textarea>
            <div class="app-dialog-fields"></div>
          </div>
          <div class="app-dialog-actions">
            <button type="button" class="secondary" data-app-dialog-cancel="true"></button>
            <button type="button" class="primary" data-app-dialog-confirm="true"></button>
          </div>
        </section>`;
      host.appendChild(modal);
      modal.querySelector('#appDialogTitle').textContent = opts.title || 'Megerősítés';
      modal.querySelector('.app-dialog-message').textContent = opts.message || '';
      const eyebrow = modal.querySelector('.app-dialog-eyebrow');
      if(eyebrow) eyebrow.textContent = opts.eyebrow || 'Megerősítés';
      const confirmButton = modal.querySelector('[data-app-dialog-confirm]');
      const cancelButton = modal.querySelector('[data-app-dialog-cancel].secondary');
      if(confirmButton){
        confirmButton.textContent = opts.confirmText || 'Rendben';
        confirmButton.hidden = opts.showConfirm === false;
      }
      if(cancelButton){
        cancelButton.textContent = opts.cancelText || 'Mégsem';
        cancelButton.hidden = opts.showCancel === false;
      }
      const inputWrap = modal.querySelector('.app-dialog-input-wrap');
      const label = modal.querySelector('.app-dialog-label');
      const input = modal.querySelector('#appDialogInput');
      const fieldsContainer = modal.querySelector('.app-dialog-fields');
      const configuredFields = Array.isArray(opts.fields) ? opts.fields : [];
      const fieldInputs = new Map();
      if(configuredFields.length){
        inputWrap.hidden = false;
        label.hidden = true;
        input.hidden = true;
        configuredFields.forEach((field, index) => {
          const wrap = document.createElement('div');
          wrap.className = 'app-dialog-field';
          const fieldId = `appDialogField_${index}`;
          const fieldLabel = document.createElement('label');
          fieldLabel.className = 'app-dialog-label';
          fieldLabel.htmlFor = fieldId;
          fieldLabel.textContent = field.label || field.name || 'Érték';
          const fieldInput = document.createElement(field.type === 'textarea' ? 'textarea' : 'input');
          fieldInput.id = fieldId;
          fieldInput.className = 'app-dialog-input';
          fieldInput.value = field.defaultValue || '';
          fieldInput.placeholder = field.placeholder || '';
          if(field.type === 'textarea') fieldInput.rows = Number(field.rows || 4);
          else fieldInput.type = field.type || 'text';
          if(field.maxLength) fieldInput.maxLength = Number(field.maxLength);
          if(field.required) fieldInput.required = true;
          wrap.append(fieldLabel, fieldInput);
          fieldsContainer.appendChild(wrap);
          fieldInputs.set(String(field.name || index), fieldInput);
        });
        setTimeout(() => fieldInputs.values().next().value?.focus(), 0);
      }else if(opts.textarea){
        inputWrap.hidden = false;
        fieldsContainer.hidden = true;
        label.textContent = opts.label || 'Megjegyzés';
        input.placeholder = opts.placeholder || '';
        input.value = opts.defaultValue || '';
        setTimeout(() => input.focus(), 0);
      }else{
        fieldsContainer.hidden = true;
      }
      let closed = false;
            /**
       * Elrejti vagy lezárja a close felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
       *
       * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
       * @param {*} result a függvény result bemeneti értéke
       */
const close = result => {
        if(closed) return;
        closed = true;
        modal.classList.add('is-closing');
        setTimeout(() => modal.remove(), 160);
        resolve(result);
      };
      modal.addEventListener('click', event => {
        const target = event.target;
        if(target?.dataset?.appDialogCancel){
          close({ confirmed:false, value:null });
        }
        if(target?.dataset?.appDialogConfirm){
          if(configuredFields.length){
            const invalidField = Array.from(fieldInputs.values()).find(item => item.required && !String(item.value || '').trim());
            if(invalidField){
              invalidField.focus();
              return;
            }
            const values = {};
            fieldInputs.forEach((fieldInput, key) => { values[key] = fieldInput.value; });
            close({ confirmed:true, value: values });
          }else{
            close({ confirmed:true, value: opts.textarea ? input.value : true });
          }
        }
      });
            /**
       * A <code>keyHandler</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
       *
       * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
       * @param {*} event a feldolgozandó böngészőesemény
       */
const keyHandler = event => {
        if(!document.body.contains(modal)){
          document.removeEventListener('keydown', keyHandler);
          return;
        }
        if(event.key === 'Escape'){
          event.preventDefault();
          close({ confirmed:false, value:null });
          document.removeEventListener('keydown', keyHandler);
        }
      };
      document.addEventListener('keydown', keyHandler);
    });
  }

  window.navShowToast = showToast;
  window.navConfirm = async function(options){
    const result = await showDialog(Object.assign({}, options || {}, { textarea:false }));
    return !!result.confirmed;
  };
  window.navPrompt = async function(options){
    const result = await showDialog(Object.assign({}, options || {}, { textarea:true }));
    return result.confirmed ? String(result.value || '') : null;
  };
  window.navFormPrompt = async function(options){
    const result = await showDialog(Object.assign({}, options || {}, { textarea:false }));
    return result.confirmed ? (result.value || {}) : null;
  };
  window.navInfo = async function(options){
    await showDialog(Object.assign({}, options || {}, {
      textarea:false,
      showConfirm:false,
      showCancel:true,
      cancelText: options?.cancelText || 'Bezárás',
      eyebrow: options?.eyebrow || 'Információ'
    }));
  };
})();

(function(){
  const TERMINAL_STATUSES = new Set(['FINISHED', 'FAILED', 'CANCELLED']);
  let pollingTimer = null;
  let currentJobId = null;

    /**
   * A <code>ensureProcessingOverlay</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function ensureProcessingOverlay(){
    let overlay = document.getElementById('processingJobOverlay');
    if(overlay) return overlay;
    overlay = document.createElement('div');
    overlay.id = 'processingJobOverlay';
    overlay.className = 'processing-overlay';
    overlay.hidden = true;
    overlay.innerHTML = `
      <div class="processing-dialog" role="dialog" aria-modal="true" aria-labelledby="processingJobTitle">
        <div class="processing-spinner" aria-hidden="true"></div>
        <div class="processing-copy">
          <p class="eyebrow">Feldolgozás</p>
          <h2 id="processingJobTitle">Művelet folyamatban</h2>
          <p id="processingJobMessage" class="processing-message">Állapot lekérdezése...</p>
          <div class="processing-progress" aria-label="Feldolgozás állapota">
            <div id="processingJobProgressBar" class="processing-progress-bar" style="width:0%"></div>
          </div>
          <div class="processing-meta">
            <span id="processingJobStatus" class="status-pill">-</span>
            <span id="processingJobPercent">0%</span>
          </div>
          <div class="button-row processing-actions">
            <button type="button" class="secondary" id="processingJobCancelButton">Megszakítás</button>
          </div>
          <p class="hint">A megszakításhoz használd a gombot vagy az Escape billentyűt.</p>
        </div>
      </div>`;
    document.body.appendChild(overlay);
    const cancelButton = overlay.querySelector('#processingJobCancelButton');
    if(cancelButton){
      cancelButton.addEventListener('click', () => requestCancelCurrentJob());
    }
    return overlay;
  }

    /**
   * Feldolgozza a format job type bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} jobType a függvény jobType bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function formatJobType(jobType){
    const value = String(jobType || '').toUpperCase();
    const labels = {
      XSD_VALIDATION: 'XSD ellenőrzés',
      XPATH_VALIDATION: 'XPath ellenőrzés',
      XML_INDEXING: 'XML indexelés',
      XML_SAVE: 'XML mentés',
      XML_EXPORT: 'XML export',
      DEMO: 'Teszt feldolgozás'
    };
    return labels[value] || 'Feldolgozási művelet';
  }

    /**
   * Feldolgozza a format job status bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} status a kapcsolódó folyamat aktuális állapota
   * @returns {*} a feldolgozás eredménye
   */
function formatJobStatus(status){
    const value = String(status || '').toUpperCase();
    const labels = {
      PENDING: 'Előkészítés',
      RUNNING: 'Folyamatban',
      FINISHED: 'Befejezve',
      FAILED: 'Hibával leállt',
      CANCEL_REQUESTED: 'Megszakítás folyamatban',
      CANCELLED: 'Megszakítva'
    };
    return labels[value] || 'Ismeretlen állapot';
  }

    /**
   * Megjeleníti vagy újrarendereli a render processing job állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} job a függvény job bemeneti értéke
   */
function renderProcessingJob(job){
    const overlay = ensureProcessingOverlay();
    const title = overlay.querySelector('#processingJobTitle');
    const message = overlay.querySelector('#processingJobMessage');
    const status = overlay.querySelector('#processingJobStatus');
    const percentText = overlay.querySelector('#processingJobPercent');
    const bar = overlay.querySelector('#processingJobProgressBar');
    const cancelButton = overlay.querySelector('#processingJobCancelButton');
    const percent = Number.isFinite(Number(job?.progressPercent)) ? Math.max(0, Math.min(100, Number(job.progressPercent))) : 0;
    const rawStatus = String(job?.status || '').toUpperCase();
    if(title) title.textContent = formatJobType(job?.jobType);
    if(message) message.textContent = job?.progressMessage || job?.errorMessage || 'Állapot lekérdezése...';
    if(status){
      status.textContent = formatJobStatus(rawStatus);
      status.title = rawStatus || '';
    }
    if(percentText) percentText.textContent = `${percent}%`;
    if(bar) bar.style.width = `${percent}%`;
    if(cancelButton){
      const cancellable = job && !TERMINAL_STATUSES.has(rawStatus);
      cancelButton.disabled = !cancellable || rawStatus === 'CANCEL_REQUESTED';
      cancelButton.textContent = rawStatus === 'CANCEL_REQUESTED' ? 'Megszakítás folyamatban...' : 'Megszakítás';
    }
  }

    /**
   * Betölti vagy lekéri a fetch json művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} url a függvény url bemeneti értéke
   * @param {*} options a művelet opcionális beállításai
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function fetchJson(url, options){
    const response = await fetch(url, Object.assign({ credentials:'same-origin', cache:'no-store' }, options || {}));
    if(!response.ok){
      let message = `${response.status} ${response.statusText}`;
      try{
        const body = await response.json();
        message = body.error || message;
        if(body.activeJobId){
          return { conflict:true, activeJobId: body.activeJobId, error: message };
        }
      }catch(_ignored){}
      throw new Error(message);
    }
    return response.json();
  }

    /**
   * A <code>pollJobOnce</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} jobId a célobjektum technikai azonosítója
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function pollJobOnce(jobId){
    const job = await fetchJson(`/api/jobs/${encodeURIComponent(jobId)}`);
    renderProcessingJob(job);
    refreshAdminProcessingPanel(job);
    if(TERMINAL_STATUSES.has(String(job.status || '').toUpperCase())){
      stopProcessingPolling(false);
      setTimeout(() => hideProcessingOverlay(), 1200);
    }
    return job;
  }

    /**
   * Elindítja a start processing polling aszinkron vagy több lépéses frontend folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} jobId a célobjektum technikai azonosítója
   */
function startProcessingPolling(jobId){
    if(!jobId) return;
    currentJobId = jobId;
    ensureProcessingOverlay().hidden = false;
    if(pollingTimer) clearInterval(pollingTimer);
    pollJobOnce(jobId).catch(error => console.warn('Job polling hiba', error));
    pollingTimer = setInterval(() => pollJobOnce(jobId).catch(error => {
      console.warn('Job polling hiba', error);
      stopProcessingPolling(false);
    }), 1000);
  }

    /**
   * A <code>stopProcessingPolling</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} clearJob a függvény clearJob bemeneti értéke
   */
function stopProcessingPolling(clearJob){
    if(pollingTimer){
      clearInterval(pollingTimer);
      pollingTimer = null;
    }
    if(clearJob) currentJobId = null;
  }

    /**
   * Megjeleníti vagy újrarendereli a show local processing overlay állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} options a művelet opcionális beállításai
   */
function showLocalProcessingOverlay(options){
    const overlay = ensureProcessingOverlay();
    const title = overlay.querySelector('#processingJobTitle');
    const message = overlay.querySelector('#processingJobMessage');
    const status = overlay.querySelector('#processingJobStatus');
    const percentText = overlay.querySelector('#processingJobPercent');
    const bar = overlay.querySelector('#processingJobProgressBar');
    const cancelButton = overlay.querySelector('#processingJobCancelButton');
    if(title) title.textContent = options?.title || 'Feldolgozás';
    if(message) message.textContent = options?.message || 'A művelet folyamatban van...';
    if(status){
      status.textContent = options?.status || 'Folyamatban';
      status.title = 'LOCAL_OPERATION';
    }
    if(percentText) percentText.textContent = options?.percentText || '';
    if(bar) bar.style.width = options?.progressWidth || '35%';
    if(cancelButton){
      cancelButton.disabled = true;
      cancelButton.hidden = true;
    }
    overlay.hidden = false;
  }

    /**
   * Elrejti vagy lezárja a hide processing overlay felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function hideProcessingOverlay(){
    const overlay = ensureProcessingOverlay();
    const cancelButton = overlay.querySelector('#processingJobCancelButton');
    if(cancelButton){
      cancelButton.hidden = false;
      cancelButton.disabled = false;
      cancelButton.textContent = 'Megszakítás';
    }
    overlay.hidden = true;
    currentJobId = null;
  }

    /**
   * A <code>requestCancelCurrentJob</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A hálózati hívás eredményét egységes kliensoldali hibakezeléssel adja tovább, és a felhasználói felületet csak a válasz feldolgozása után módosítja.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function requestCancelCurrentJob(){
    if(!currentJobId) return;
    try{
      const job = await fetchJson(`/api/jobs/${encodeURIComponent(currentJobId)}/cancel`, { method:'POST' });
      renderProcessingJob(job);
    }catch(error){
      console.warn('Megszakítási kérés sikertelen', error);
      const message = ensureProcessingOverlay().querySelector('#processingJobMessage');
      if(message) message.textContent = `Megszakítási kérés sikertelen: ${error.message}`;
    }
  }

    /**
   * A <code>restoreActiveJob</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function restoreActiveJob(){
    try{
      const active = await fetchJson('/api/jobs/active');
      if(active && active.active && active.job && active.job.jobId){
        startProcessingPolling(active.job.jobId);
      }
    }catch(error){
      // A feldolgozási API nem minden oldalon vagy auth állapotban érhető el; ez nem blokkoló hiba.
    }
  }

    /**
   * Elindítja a start demo job aszinkron vagy több lépéses frontend folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function startDemoJob(){
    const resultEl = document.getElementById('processingJobDemoResult');
    try{
      if(resultEl) resultEl.textContent = 'Teszt job indítása...';
      const result = await fetchJson('/api/jobs/demo', {
        method:'POST',
        headers:{ 'Content-Type':'application/json' },
        body: JSON.stringify({ durationSeconds: 15 })
      });
      if(result?.conflict && result.activeJobId){
        if(resultEl) resultEl.textContent = result.error;
        startProcessingPolling(result.activeJobId);
        return;
      }
      if(resultEl) resultEl.textContent = `Elindult: ${result.jobId}`;
      startProcessingPolling(result.jobId);
    }catch(error){
      if(resultEl) resultEl.textContent = `Hiba: ${error.message}`;
    }
  }

    /**
   * Szinkronizálja vagy frissíti a refresh admin processing panel által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} job a függvény job bemeneti értéke
   */
function refreshAdminProcessingPanel(job){
    const statusEl = document.getElementById('processingJobAdminStatus');
    const messageEl = document.getElementById('processingJobAdminMessage');
    if(statusEl) statusEl.textContent = job ? `${job.status || '-'} / ${job.progressPercent ?? 0}%` : 'Nincs aktív job';
    if(messageEl) messageEl.textContent = job ? (job.progressMessage || job.errorMessage || '-') : '-';
  }

    /**
   * Szinkronizálja vagy frissíti a refresh active job panel által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function refreshActiveJobPanel(){
    try{
      const active = await fetchJson('/api/jobs/active');
      if(active && active.active && active.job){
        refreshAdminProcessingPanel(active.job);
      }else{
        refreshAdminProcessingPanel(null);
      }
    }catch(error){
      const statusEl = document.getElementById('processingJobAdminStatus');
      if(statusEl) statusEl.textContent = 'Nem elérhető';
    }
  }

  document.addEventListener('keydown', event => {
    if(event.key === 'Escape' && currentJobId){
      event.preventDefault();
      requestCancelCurrentJob();
    }
  });

  document.addEventListener('DOMContentLoaded', () => {
    ensureProcessingOverlay();
    const demoButton = document.getElementById('startDemoProcessingJobButton');
    if(demoButton) demoButton.addEventListener('click', startDemoJob);
    const refreshButton = document.getElementById('refreshProcessingJobButton');
    if(refreshButton) refreshButton.addEventListener('click', refreshActiveJobPanel);
    restoreActiveJob();
    refreshActiveJobPanel();
  });

  window.NavProcessingJobs = {
    startPolling: startProcessingPolling,
    requestCancel: requestCancelCurrentJob,
    refreshActive: refreshActiveJobPanel,
    showLocal: showLocalProcessingOverlay,
    hide: hideProcessingOverlay
  };
})();

(function(){
  let githubNotificationData = null;

    /**
   * A <code>ensureGitHubUpdateNotification</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function ensureGitHubUpdateNotification(){
    const area = document.getElementById('securityHeaderArea') || document.querySelector('.security-header-area');
    if(!area || document.getElementById('githubUpdateNotificationButton')) return;
    const button = document.createElement('button');
    button.type = 'button';
    button.id = 'githubUpdateNotificationButton';
    button.className = 'github-update-notification';
    button.hidden = true;
    button.innerHTML = '<span class="github-update-bell" aria-hidden="true">🔔</span><span>Frissítés elérhető</span>';
    button.addEventListener('click', showGitHubUpdateDialog);
    area.insertBefore(button, area.firstChild);
  }

    /**
   * Megjeleníti vagy újrarendereli a show git hub update dialog állapotát a felhasználói felületen.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   */
function showGitHubUpdateDialog(){
    const data = githubNotificationData;
    if(!data || !data.updateAvailable) return;
    const changed = Array.isArray(data.changedRepositories) ? data.changedRepositories : [];
    const removed = Array.isArray(data.removedRepositories) ? data.removedRepositories : [];
    const items = [
      ...changed.map(name => `<li><strong>${escapeNotificationHtml(name)}</strong> – új vagy módosult repository</li>`),
      ...removed.map(name => `<li><strong>${escapeNotificationHtml(name)}</strong> – eltávolított vagy archivált repository</li>`)
    ];
    const host = document.createElement('div');
    host.className = 'github-update-dialog';
    host.innerHTML = `<div class="app-dialog-backdrop"></div><section class="app-dialog-card" role="dialog" aria-modal="true" aria-labelledby="githubUpdateDialogTitle"><button type="button" class="app-dialog-close" aria-label="Bezárás">×</button><p class="eyebrow">GitHub űrlapsablonok</p><h2 id="githubUpdateDialogTitle">Frissítés érhető el</h2><p>${data.changedRepositoryCount || 0} módosult és ${data.removedRepositoryCount || 0} eltávolított repository található.</p><ul class="github-update-change-list">${items.join('') || '<li>A részletes változáslista nem érhető el.</li>'}</ul><p class="hint">A frissítés a Űrlapsablonok oldalon végezhető el.</p><div class="app-dialog-actions"><button type="button" class="secondary github-update-close">Bezárás</button><a class="primary github-update-open" href="/github-templates.html">Űrlapsablonok megnyitása</a></div></section>`;
    document.body.appendChild(host);
        /**
     * Elrejti vagy lezárja a close felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     */
const close = () => host.remove();
    host.querySelector('.app-dialog-backdrop')?.addEventListener('click', close);
    host.querySelector('.app-dialog-close')?.addEventListener('click', close);
    host.querySelector('.github-update-close')?.addEventListener('click', close);
  }

    /**
   * A <code>escapeNotificationHtml</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function escapeNotificationHtml(value){
    return String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
  }

    /**
   * Szinkronizálja vagy frissíti a refresh git hub update notification által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function refreshGitHubUpdateNotification(){
    ensureGitHubUpdateNotification();
    const button = document.getElementById('githubUpdateNotificationButton');
    if(!button) return;
    try{
      const response = await fetch('/api/github-templates/notification', { cache:'no-store' });
      if(!response.ok){ button.hidden = true; return; }
      githubNotificationData = await response.json();
      button.hidden = githubNotificationData.updateAvailable !== true;
    }catch(_error){
      button.hidden = true;
    }
  }

  document.addEventListener('DOMContentLoaded', () => {
    window.setTimeout(refreshGitHubUpdateNotification, 500);
    window.setInterval(refreshGitHubUpdateNotification, 60000);
  });
  window.NavGitHubTemplateNotification = { refresh: refreshGitHubUpdateNotification };
})();


/** Adminisztrátori H2 futtatásnál dinamikusan megjeleníti a Rendszer menü H2 Konzol elemét. */
async function installH2ConsoleSystemMenu(){
  const panels=[...document.querySelectorAll('.system-menu-panel')];
  if(!panels.length || panels.some(panel=>panel.querySelector('[data-system-h2-console="true"]'))) return;
  try{
    const response=await fetch('/api/database/status',{cache:'no-store'});
    if(!response.ok) return;
    const status=await response.json();
    if(String(status?.configuredDatabaseType||'').toUpperCase()!=='H2' || status?.h2ConsoleEnabled===false) return;
    const consolePath=String(status?.h2ConsolePath||'/h2-console').trim()||'/h2-console';
    for(const panel of panels){
      const link=document.createElement('a');
      link.href=consolePath; link.textContent='H2 Konzol'; link.dataset.systemH2Console='true';
      link.title='H2 adatbázis konzol megnyitása'; panel.appendChild(link);
    }
  }catch(_ignored){}
}
document.addEventListener('DOMContentLoaded',()=>{ void installH2ConsoleSystemMenu(); });
