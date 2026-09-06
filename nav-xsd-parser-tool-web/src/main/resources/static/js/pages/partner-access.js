/**
 * @module pages/partner-access
 *
 * A oldalspecifikus felhasználói felület- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

(()=>{
  const userId = new URLSearchParams(location.search).get('userId');
    /**
   * A <code>byId</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} id a célobjektum technikai azonosítója
   */
const byId = id => document.getElementById(id);
  let partners = [];
  let rules = [];
  let page = 0;
  const size = 20;
  let searchTimer;

    /**
   * A <code>data</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} response a backend-hívás feldolgozandó válasza
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function data(response){
    const text = await response.text();
    let parsed = {};
    try{ parsed = text ? JSON.parse(text) : {}; }catch(_ignored){ parsed = { message:text }; }
    if(!response.ok) throw new Error(parsed.message || 'A művelet sikertelen.');
    return parsed;
  }

    /**
   * A <code>message</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} type a függvény type bemeneti értéke
   * @param {*} text a függvény text bemeneti értéke
   */
function message(type, text){
    const target = byId('message');
    target.className = `access-message ${type}`;
    target.textContent = text;
    target.hidden = false;
  }

    /**
   * Eltávolítja vagy alaphelyzetbe állítja a clear művelethez tartozó kliensoldali állapotot.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} node a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
   */
function clear(node){
    if(node) node.replaceChildren();
  }

    /**
   * A <code>element</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} tag a függvény tag bemeneti értéke
   * @param {*} text a függvény text bemeneti értéke
   * @param {*} className a feloldáshoz vagy megjelenítéshez használt név
   * @returns {*} a feldolgozás eredménye
   */
function element(tag, text, className){
    const node = document.createElement(tag);
    if(className) node.className = className;
    if(text !== undefined && text !== null) node.textContent = String(text);
    return node;
  }

    /**
   * A <code>payload</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function payload(){
    return {
      partnerIds: partners.map(p => p.partnerId),
      rules: [...document.querySelectorAll('.rule-row')].map((row, index) => ({
        ruleType: row.querySelector('[data-f=type]').value,
        taxNumber: row.querySelector('[data-f=tax]').value.trim() || null,
        vatCode: row.querySelector('[data-f=vat]').value.trim() || null,
        countyCode: row.querySelector('[data-f=county]').value.trim() || null,
        sortOrder: index
      }))
    };
  }

    /**
   * Megjeleníti vagy újrarendereli a render partners állapotát a felhasználói felületen.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   */
function renderPartners(){
    const host = byId('selectedPartners');
    clear(host);
    if(!partners.length){
      host.append(element('p', 'Nincs közvetlen partner hozzárendelve.', 'hint'));
      return;
    }
    partners.forEach(partner => {
      const item = element('div', null, 'selected-item');
      const label = element('span');
      label.append(element('strong', partner.partnerName || ''));
      label.append(document.createElement('br'));
      label.append(element('small', partner.taxNumber || ''));
      const remove = element('button', 'Eltávolítás', 'secondary');
      remove.type = 'button';
      remove.addEventListener('click', () => {
        partners = partners.filter(current => String(current.partnerId) !== String(partner.partnerId));
        renderPartners();
      });
      item.append(label, remove);
      host.append(item);
    });
  }

    /**
   * Előkészíti és elindítja a add rule állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} rule a függvény rule bemeneti értéke
   */
function addRule(rule = { ruleType:'ALLOW', taxNumber:'*', vatCode:'*', countyCode:'*' }){
    rules.push(rule);
    renderRules();
  }

    /**
   * A <code>fieldLabel</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} text a függvény text bemeneti értéke
   * @param {*} control a függvény control bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function fieldLabel(text, control){
    const label = element('label', text);
    label.append(control);
    return label;
  }

    /**
   * A <code>ruleInput</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} field a függvény field bemeneti értéke
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @param {*} maxLength a függvény maxLength bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function ruleInput(field, value, maxLength){
    const input = document.createElement('input');
    input.dataset.f = field;
    input.maxLength = maxLength;
    input.value = value ?? '';
    return input;
  }

    /**
   * Megjeleníti vagy újrarendereli a render rules állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function renderRules(){
    const host = byId('rules');
    clear(host);
    if(!rules.length){
      host.append(element('p', 'Nincs adószám alapú szabály.', 'hint'));
      return;
    }
    rules.forEach((rule, index) => {
      const row = element('div', null, 'rule-row');
      row.dataset.index = String(index);
      const select = document.createElement('select');
      select.dataset.f = 'type';
      [['ALLOW','Engedélyező'], ['DENY','Kizáró']].forEach(([value, label]) => {
        const option = element('option', label);
        option.value = value;
        option.selected = rule.ruleType === value;
        select.append(option);
      });
      const remove = element('button', 'Törlés', 'secondary');
      remove.type = 'button';
      remove.addEventListener('click', () => {
        rules.splice(index, 1);
        renderRules();
      });
      row.append(
        fieldLabel('Típus', select),
        fieldLabel('Adószám törzsszám', ruleInput('tax', rule.taxNumber, 8)),
        fieldLabel('Áfakód', ruleInput('vat', rule.vatCode, 1)),
        fieldLabel('Megyekód', ruleInput('county', rule.countyCode, 2)),
        remove
      );
      host.append(row);
    });
  }

    /**
   * Megjeleníti vagy újrarendereli a render suggestions állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} list a függvény list bemeneti értéke
   */
function renderSuggestions(list){
    const host = byId('partnerSuggestions');
    clear(host);
    list.forEach(partner => {
      const suggestion = element('div', null, 'suggestion');
      const label = element('span');
      label.append(element('strong', partner.name || ''));
      label.append(document.createElement('br'));
      label.append(element('small', partner.taxNumber || ''));
      suggestion.append(label, element('span', 'Hozzáadás'));
      suggestion.addEventListener('click', () => {
        if(!partners.some(current => String(current.partnerId) === String(partner.id))){
          partners.push({ partnerId:Number(partner.id), partnerName:String(partner.name || ''), taxNumber:String(partner.taxNumber || '') });
        }
        renderPartners();
        clear(host);
        byId('partnerSearch').value = '';
      });
      host.append(suggestion);
    });
  }

    /**
   * Betölti vagy lekéri a load művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function load(){
    if(!userId || !/^\d+$/.test(userId)) throw new Error('Hiányzó vagy érvénytelen felhasználóazonosító.');
    const mode = await data(await fetch('/api/security/mode', { credentials:'same-origin' }));
    if(mode.mode !== 'MULTI_USER') throw new Error('A partnerjogosultság csak multi-user módban érhető el.');
    const cfg = await data(await fetch(`/api/users/${encodeURIComponent(userId)}/partner-access`, { credentials:'same-origin' }));
    byId('userLabel').textContent = `Felhasználó: ${cfg.username || ''}`;
    partners = cfg.partners || [];
    rules = cfg.rules || [];
    renderPartners();
    renderRules();
  }

  byId('partnerSearch').addEventListener('input', () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(async () => {
      const query = byId('partnerSearch').value.trim();
      if(query.length < 2){ clear(byId('partnerSuggestions')); return; }
      try{
        const list = await data(await fetch(`/api/users/${encodeURIComponent(userId)}/partner-access/partners?q=${encodeURIComponent(query)}`, { credentials:'same-origin' }));
        renderSuggestions(list);
      }catch(error){
        message('error', error.message);
      }
    }, 250);
  });

    /**
   * Megjeleníti vagy újrarendereli a render test rows állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} content a függvény content bemeneti értéke
   */
function renderTestRows(content){
    const body = byId('testRows');
    clear(body);
    (content || []).forEach(item => {
      const row = document.createElement('tr');
      [item.fileName, item.partnerName, item.taxNumber, item.formType, item.status, item.createdAt]
        .forEach(value => row.append(element('td', value || '')));
      body.append(row);
    });
  }

    /**
   * Megjeleníti vagy újrarendereli a render pager állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} result a függvény result bemeneti értéke
   */
function renderPager(result){
    const host = byId('pager');
    clear(host);
    const previous = element('button', 'Előző', 'secondary');
    previous.type = 'button';
    previous.disabled = result.page <= 0;
    previous.addEventListener('click', () => test(page - 1));
    const status = element('span', `${result.totalPages ? result.page + 1 : 0} / ${result.totalPages || 0}`);
    const next = element('button', 'Következő', 'secondary');
    next.type = 'button';
    next.disabled = result.page + 1 >= result.totalPages;
    next.addEventListener('click', () => test(page + 1));
    host.append(previous, status, next);
  }

    /**
   * A <code>test</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} targetPage az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function test(targetPage = 0){
    try{
      page = targetPage;
      const result = await data(await fetch(`/api/users/${encodeURIComponent(userId)}/partner-access/test?page=${page}&size=${size}`, {
        method:'POST',
        credentials:'same-origin',
        headers:{ 'Content-Type':'application/json' },
        body:JSON.stringify(payload())
      }));
      byId('testSummary').textContent = `A feltételek alapján látható rekordok száma: ${result.totalElements}`;
      renderTestRows(result.content);
      renderPager(result);
    }catch(error){
      message('error', error.message);
    }
  }

  byId('addRule').onclick = () => addRule();
  byId('testButton').onclick = () => test(0);
  byId('saveButton').onclick = async () => {
    try{
      await data(await fetch(`/api/users/${encodeURIComponent(userId)}/partner-access`, {
        method:'PUT', credentials:'same-origin', headers:{ 'Content-Type':'application/json' }, body:JSON.stringify(payload())
      }));
      message('success', 'A partnerjogosultságok mentése sikeres.');
    }catch(error){ message('error', error.message); }
  };
  byId('backButton').onclick = () => { if(window.history.length > 1) window.history.back(); else window.location.href = '/users.html'; };

  load().catch(error => {
    message('error', error.message);
    document.querySelectorAll('button,input,select').forEach(control => {
      if(control.id !== 'backButton') control.disabled = true;
    });
  });
})();
