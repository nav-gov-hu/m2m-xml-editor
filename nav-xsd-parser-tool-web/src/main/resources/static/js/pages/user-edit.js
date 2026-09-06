/**
 * @module pages/user-edit
 *
 * A oldalspecifikus felhasználói felület- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

(()=>{
    /**
   * A <code>byId</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} id a célobjektum technikai azonosítója
   */
const byId = id => document.getElementById(id);
  const form = byId('userEditForm');
  const id = new URLSearchParams(location.search).get('id');
  const messageTarget = byId('userEditMessage');
  let roles = [];

    /**
   * A <code>back</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function back(){ window.location.href = '/users.html'; }

    /**
   * Megjeleníti vagy újrarendereli a show állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} type a függvény type bemeneti értéke
   * @param {*} text a függvény text bemeneti értéke
   */
function show(type, text){
    messageTarget.className = `partner-edit-message ${type}`;
    messageTarget.textContent = text;
    messageTarget.hidden = false;
  }

    /**
   * A <code>json</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} response a backend-hívás feldolgozandó válasza
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function json(response){
    const text = await response.text();
    let data = {};
    try{ data = text ? JSON.parse(text) : {}; }catch(_ignored){ data = { message:text }; }
    if(!response.ok) throw new Error('A művelet sikertelen.');
    return data;
  }

    /**
   * Megjeleníti vagy újrarendereli a render roles állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} selected a függvény selected bemeneti értéke
   */
function renderRoles(selected = []){
    const host = byId('roleOptions');
    host.replaceChildren();
    roles.forEach(role => {
      const label = document.createElement('label');
      label.className = 'partner-field';
      const line = document.createElement('span');
      const input = document.createElement('input');
      input.type = 'checkbox';
      input.name = 'roles';
      input.value = String(role.code || '');
      input.checked = selected.includes(role.code);
      const strong = document.createElement('strong');
      strong.textContent = role.code || '';
      line.append(input, document.createTextNode(' '), strong);
      const small = document.createElement('small');
      small.textContent = role.name || '';
      label.append(line, small);
      host.append(label);
    });
  }

    /**
   * Betölti vagy lekéri a load művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function load(){
    const mode = await json(await fetch('/api/security/mode', { credentials:'same-origin' }));
    if(id && /^\d+$/.test(id) && mode.mode === 'MULTI_USER'){
      byId('partnerAccessButton').hidden = false;
      byId('partnerAccessButton').onclick = () => {
        const navigationForm = document.createElement('form');
        navigationForm.method = 'GET';
        navigationForm.action = '/partner-access.html';
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'userId';
        input.value = id;
        navigationForm.appendChild(input);
        document.body.appendChild(navigationForm);
        navigationForm.submit();
      };
    }
    roles = await json(await fetch('/api/users/roles', { credentials:'same-origin' }));
    const policy = await json(await fetch('/api/users/password-policy', { credentials:'same-origin' }));
    byId('password').minLength = policy.minimumLength;
    byId('password').maxLength = policy.maximumLength;
    byId('passwordPolicyText').textContent = `Minimum ${policy.minimumLength} karakter, maximum ${policy.maximumLength} karakter. Az utolsó ${policy.historySize} jelszó nem használható újra. ${policy.maximumFailedAttempts} hibás belépés után ${policy.lockDurationMinutes} perces zárolás történik. Nincs időszakos kötelező jelszócsere.`;
    if(!id){
      renderRoles();
      byId('password').required = true;
      return;
    }
    if(!/^\d+$/.test(id)) throw new Error('Érvénytelen felhasználóazonosító.');
    const user = await json(await fetch(`/api/users/${encodeURIComponent(id)}`, { credentials:'same-origin' }));
    renderRoles(user.roles || []);
    byId('userEditTitle').textContent = 'Felhasználó szerkesztése';
    byId('userEditSubtitle').textContent = String(user.username ?? '');
    ['username', 'displayName', 'email'].forEach(field => { byId(field).value = String(user[field] ?? ''); });
    byId('enabled').checked = Boolean(user.enabled);
    byId('passwordChangeRequired').checked = Boolean(user.passwordChangeRequired);
    byId('passwordHint').textContent = '(csak módosításkor töltse ki)';
    byId('password').required = false;
  }

    /**
   * Előkészíti és elindítja a save állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function save(event){
    event.preventDefault();
    messageTarget.hidden = true;
    const selectedRoles = [...document.querySelectorAll('input[name=roles]:checked')].map(input => input.value);
    const payload = {
      username:byId('username').value.trim(),
      displayName:byId('displayName').value.trim(),
      email:byId('email').value.trim(),
      enabled:byId('enabled').checked,
      passwordChangeRequired:byId('passwordChangeRequired').checked,
      roles:selectedRoles,
      password:byId('password').value
    };
    try{
      const editing = id && /^\d+$/.test(id);
      const response = await fetch(editing ? `/api/users/${encodeURIComponent(id)}` : '/api/users', {
        method:editing ? 'PUT' : 'POST', credentials:'same-origin', headers:{ 'Content-Type':'application/json' }, body:JSON.stringify(payload)
      });
      const saved = await json(response);
      show('success', 'A felhasználó mentése sikeres.');
      if(!editing && saved.id){ history.replaceState({}, '', `/user-edit.html?id=${encodeURIComponent(saved.id)}`); }
      byId('password').value = '';
      byId('password').required = false;
    }catch(error){ show('error', error.message); }
  }

  byId('backToUserList').onclick = byId('cancelUserEdit').onclick = back;
  form.onsubmit = save;
  load().catch(error => show('error', error.message));
})();
