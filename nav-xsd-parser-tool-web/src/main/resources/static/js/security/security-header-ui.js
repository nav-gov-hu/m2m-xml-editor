/**
 * @module security/security-header-ui
 *
 * A biztonsági felület- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

/**
 * Bejelentkezett felhasználó fejlécének és a szerepkör/config alapú
 * főmenü-láthatóságnak a kezelése.
 */
export function createSecurityHeaderUi(){
  let currentUser = null;
  let configuredVisibility = {};

    /**
   * A <code>ensureHeaderArea</code> függvény a biztonsági felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function ensureHeaderArea(){
    const headerInner = document.querySelector('.site-header-inner');
    if(!headerInner) return null;
    let area = headerInner.querySelector('#securityHeaderArea') || headerInner.querySelector('.site-header-right.security-user-area');
    if(area){
      area.classList.add('site-header-right', 'security-user-area');
      const logoutButton = area.querySelector('button[type="submit"]');
      if(logoutButton && !logoutButton.classList.contains('security-logout-button')) logoutButton.classList.add('security-logout-button');
      const logoutForm = area.querySelector('form[action="/logout"]');
      if(logoutForm && !logoutForm.classList.contains('security-logout-form')) logoutForm.classList.add('security-logout-form');
      return area;
    }

    area = document.createElement('div');
    area.id = 'securityHeaderArea';
    area.className = 'site-header-right security-user-area';
    area.hidden = true;
    area.innerHTML = `
      <div class="security-user-pill" title="Bejelentkezett felhasználó">
        <span class="security-user-icon" aria-hidden="true">👤</span>
        <span class="security-user-name" id="securityHeaderUsername"></span>
      </div>
      <form class="security-logout-form" method="post" action="/logout">
        <button type="submit" class="security-logout-button" title="Kijelentkezés">Kijelentkezés</button>
      </form>
    `;
    headerInner.appendChild(area);
    return area;
  }

    /**
   * Ellenőrzi a is current user admin feltételeit, és a hívó számára döntési eredményt állít elő.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} user a függvény user bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function isCurrentUserAdmin(user = currentUser){
    const mode = String(user?.mode || '').toUpperCase();
    if(mode === 'STANDALONE') return true;
    const roles = Array.isArray(user?.roles) ? user.roles.map(role => String(role).toUpperCase()) : [];
    return roles.includes('ROLE_ADMIN') || roles.includes('ADMIN');
  }

    /**
   * Elrejti vagy lezárja a hide validation links felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
   */
function hideValidationLinks(){
    document.querySelectorAll('.header-nav a[href="/validate.html"], .header-nav a[href="/xpath-validator.html"]').forEach(link => {
      link.hidden = true;
      link.classList.add('hidden-by-role');
      link.style.display = 'none';
      link.setAttribute('aria-hidden', 'true');
      link.tabIndex = -1;
    });
  }

    /**
   * Szinkronizálja vagy frissíti a apply role based menu visibility által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} user a függvény user bemeneti értéke
   */
function applyRoleBasedMenuVisibility(user = currentUser){
    hideValidationLinks();
    const isAdmin = isCurrentUserAdmin(user);
    const visibleByConfig = configuredVisibility.admin !== false;
    const visible = isAdmin && visibleByConfig;
    document.querySelectorAll('.header-nav a[href="/admin.html"]').forEach(link => {
      link.hidden = !visible;
      link.classList.toggle('hidden-by-role', !isAdmin);
      link.classList.toggle('hidden-by-config', !visibleByConfig);
      link.style.display = visible ? '' : 'none';
      link.setAttribute('aria-hidden', visible ? 'false' : 'true');
      link.tabIndex = visible ? 0 : -1;
    });
  }

    /**
   * Szinkronizálja vagy frissíti a apply header menu visibility által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   * @param {*} headerMenuVisibility a függvény headerMenuVisibility bemeneti értéke
   */
function applyHeaderMenuVisibility(headerMenuVisibility){
    const visibility = headerMenuVisibility || {};
    configuredVisibility = { ...visibility };
    hideValidationLinks();
    const menuItems = [
      { key: 'home', selector: '.header-nav a[href="/home.html"]' },
      { key: 'xmlFiles', selector: '.header-nav a[href="/xml-files.html"]' },
      { key: 'githubTemplates', selector: '.header-nav a[href="/github-templates.html"]' },
      { key: 'form', selector: '.header-nav a[href="/form.html"]' },
      { key: 'admin', selector: '.header-nav a[href="/admin.html"]' }
    ];

    menuItems.forEach(item => {
      document.querySelectorAll(item.selector).forEach(link => {
        const visibleByConfig = visibility[item.key] !== false;
        const visibleByRole = item.key !== 'admin' || isCurrentUserAdmin();
        const visible = visibleByConfig && visibleByRole;
        link.classList.toggle('hidden-by-config', !visibleByConfig);
        link.classList.toggle('hidden-by-role', !visibleByRole);
        link.style.display = visible ? '' : 'none';
        link.setAttribute('aria-hidden', visible ? 'false' : 'true');
        link.tabIndex = visible ? 0 : -1;
      });
    });
  }

    /**
   * Szinkronizálja vagy frissíti a update header által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function updateHeader(){
    const area = ensureHeaderArea();
    if(!area) return null;
    try{
      const response = await fetch('/api/security/current-user', { cache:'no-store', credentials:'same-origin' });
      if(!response.ok){
        area.hidden = true;
        currentUser = { mode:'MULTI_USER', roles:[] };
        applyRoleBasedMenuVisibility();
        return currentUser;
      }
      currentUser = await response.json();
      applyRoleBasedMenuVisibility();
      const multiUser = String(currentUser.mode || '').toUpperCase() === 'MULTI_USER';
      const authenticated = currentUser.authenticated === true;
      if(!multiUser || !authenticated){
        area.hidden = true;
        return currentUser;
      }
      const usernameEl = area.querySelector('#securityHeaderUsername');
      if(usernameEl) usernameEl.textContent = currentUser.username || 'Felhasználó';
      area.hidden = false;
      return currentUser;
    }catch(error){
      console.warn('A bejelentkezett felhasználó adatai nem tölthetők be.', error);
      area.hidden = true;
      currentUser = { mode:'MULTI_USER', roles:[] };
      applyRoleBasedMenuVisibility();
      return currentUser;
    }
  }

  return {
    updateHeader,
    applyRoleBasedMenuVisibility,
    applyHeaderMenuVisibility,
    isCurrentUserAdmin,
    getCurrentUser: () => currentUser
  };
}
