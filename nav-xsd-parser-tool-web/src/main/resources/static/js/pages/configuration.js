/**
 * @module pages/configuration
 *
 * A oldalspecifikus felhasználói felület- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

(()=>{
  const labels={ALKALMAZAS:'Alkalmazás',ADATBAZIS:'Adatbázis',HITELESITES:'Hitelesítés',KONYVTARAK:'Könyvtárak',BIZTONSAG:'Biztonság',JOGOSULTSAG:'Jogosultságok',XML:'XML-kezelés',VALIDACIO:'Validáció',M2M:'M2M integráció',GITHUB:'GitHub katalógus',NAPLOZAS:'Naplózás',TELJESITMENY:'Teljesítmény',HALOZAT:'Hálózat és proxy',TANUSITVANY:'Tanúsítványok és TLS'};
    /**
   * A <code>$</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} id a célobjektum technikai azonosítója
   */
const $=id=>document.getElementById(id); const query=new URLSearchParams(location.search); let items=[],activeCategory=(query.get('category')||'').toUpperCase(),advanced=query.get('mode')==='advanced',viewMode='list',missingOnly=localStorage.getItem('m2mConfigurationMissingOnly')==='true',dirty=new Set(),sensitiveTouched=new Set(),searchTimer=null,pendingFocusKey='';
    /**
   * A <code>esc</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} s a függvény s bemeneti értéke
   */
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    /**
   * Feldolgozza a normalize bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} s a függvény s bemeneti értéke
   */
const normalize=s=>String(s??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
    /**
   * A <code>visibleItems</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function visibleItems(){return items.filter(x=>missingOnly?x.missing:(advanced||!x.advanced));}
    /**
   * A <code>missingCount</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function missingCount(){return items.filter(x=>x.missing).length;}
    /**
   * A <code>categories</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function categories(){return [...new Set(visibleItems().map(x=>x.category))];}
    /**
   * Megjeleníti vagy újrarendereli a render tabs állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function renderTabs(){const cats=categories(); if(!cats.includes(activeCategory))activeCategory=cats[0]||'';$('configurationTabs').innerHTML=cats.map(c=>`<button type="button" class="configuration-tab ${c===activeCategory?'active':''}" data-category="${c}">${esc(labels[c]||c)}<span>${visibleItems().filter(x=>x.category===c).length}</span></button>`).join('');}
    /**
   * A <code>proxyGroupForKey</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} key a függvény key bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function proxyGroupForKey(key){if(key.startsWith('nav.xsdparsertool.github.proxy.'))return 'github';if(key.startsWith('nav.xsdparsertool.network.proxy.'))return 'm2m';return '';}
    /**
   * A <code>proxyEnabledKey</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} group a függvény group bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function proxyEnabledKey(group){return group==='github'?'nav.xsdparsertool.github.proxy.enabled':group==='m2m'?'nav.xsdparsertool.network.proxy.enabled':'';}
    /**
   * A <code>proxyControlDisabled</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} x a függvény x bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function proxyControlDisabled(x){const group=proxyGroupForKey(x.key);if(!group||x.key===proxyEnabledKey(group))return false;const enabled=items.find(item=>item.key===proxyEnabledKey(group));return String(enabled?.value)!=='true';}
    /**
   * A <code>control</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} x a függvény x bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function control(x){const id='cfg-'+btoa(unescape(encodeURIComponent(x.key))).replace(/[^a-z0-9]/gi,''); const proxyGroup=proxyGroupForKey(x.key);const disabled=proxyControlDisabled(x);const common=`id="${id}" data-key="${esc(x.key)}" data-original="${esc(x.value??'')}"${proxyGroup?` data-proxy-config-group="${proxyGroup}"`:''}${disabled?' disabled':''}`;
    if(x.type==='BOOLEAN')return `<select ${common}><option value="true" ${String(x.value)==='true'?'selected':''}>Bekapcsolva</option><option value="false" ${String(x.value)==='false'?'selected':''}>Kikapcsolva</option></select>`;
    if(x.type==='SELECT')return `<select ${common}>${(x.options||[]).map(o=>`<option value="${esc(o)}" ${o===x.value?'selected':''}>${esc(o)}</option>`).join('')}</select>`;
    if(x.type==='TEXTAREA')return `<textarea ${common} rows="4" placeholder="${esc(x.defaultValue||'')}">${esc(x.value||'')}</textarea>`;
    const type=x.type==='PASSWORD'?'password':x.type==='NUMBER'?'number':'text';
    const sensitiveAttrs=x.sensitive?` data-sensitive="true" name="configuration-secret-${id}" autocomplete="new-password" readonly`:'';
    const input=`<input ${common}${sensitiveAttrs} type="${type}" value="${esc(x.value||'')}" placeholder="${x.sensitive?'Változatlanul hagyáshoz maradjon üres':esc(x.defaultValue||'')}">`;
    if(x.type!=='PASSWORD')return input;
    return `<div class="configuration-password-field">${input}<button type="button" class="configuration-password-toggle" data-password-toggle="${id}" aria-label="Érték megjelenítése" aria-pressed="false" title="Érték megjelenítése"><svg aria-hidden="true" viewBox="0 0 24 24"><path d="M2.4 12s3.5-6 9.6-6 9.6 6 9.6 6-3.5 6-9.6 6-9.6-6-9.6-6Z"></path><circle cx="12" cy="12" r="2.8"></circle></svg></button></div>`;}
    /**
   * A <code>fieldTags</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} x a függvény x bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function fieldTags(x){const dbState=x.storage==='DATABASE'?`<span class="database-state-tag ${x.databasePersisted?'persisted':'catalog-only'}">${x.databasePersisted?'DB-ben mentve':'Nincs DB-rekord'}</span>`:'';return `<div class="configuration-tags"><span class="storage-tag ${x.storage.toLowerCase()}">${x.storage==='BOOTSTRAP'?'Bootstrap':'Adatbázis'}</span>${dbState}${x.restartRequired?'<span class="restart-tag">Újraindítás</span>':''}</div>`;}
    /**
   * A <code>cardMarkup</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} x a függvény x bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function cardMarkup(x){return `<article class="configuration-field ${dirty.has(x.key)?'changed':''} ${proxyControlDisabled(x)?'configuration-dependent-disabled':''}" data-config-item="${esc(x.key)}"><div class="configuration-field-title"><label>${esc(x.label)}${x.required?'<sup>*</sup>':''}</label>${fieldTags(x)}</div><p>${esc(x.description)}</p>${control(x)}<div class="configuration-meta"><code>${esc(x.key)}</code><span>Forrás: ${esc(sourceLabel(x.source))}</span>${x.defaultValue!==''?`<span>Alapérték: ${esc(x.defaultValue)}</span>`:''}<button type="button" class="configuration-reset-link" data-reset-key="${esc(x.key)}">Alapérték visszaállítása</button></div></article>`;}
    /**
   * A <code>sourceLabel</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} source a függvény source bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function sourceLabel(source){return ({DATABASE:'Adatbázis',ENCRYPTED_DATABASE:'Titkosított adatbázis',BOOTSTRAP_FILE:'Bootstrap fájl',ENVIRONMENT:'Konfigurációs fájl / környezet',DEFAULT:'Alapérték'})[source]||source;}
    /**
   * A <code>listTable</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} data a függvény data bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function listTable(data){return `<div class="configuration-list-wrap"><table class="configuration-list"><thead><tr><th>Beállítás</th><th>Érték</th><th>Tárolás</th><th>Forrás</th><th></th></tr></thead><tbody>${data.map(x=>`<tr class="configuration-list-row ${dirty.has(x.key)?'changed':''} ${proxyControlDisabled(x)?'configuration-dependent-disabled':''}" data-config-item="${esc(x.key)}"><td><strong>${esc(x.label)}${x.required?'<sup>*</sup>':''}</strong><p>${esc(x.description)}</p><code>${esc(x.key)}</code></td><td class="configuration-list-value">${control(x)}</td><td>${fieldTags(x)}</td><td><span>${esc(sourceLabel(x.source))}</span>${x.defaultValue!==''?`<small>Alapérték: ${esc(x.defaultValue)}</small>`:''}</td><td><button type="button" class="configuration-reset-link" data-reset-key="${esc(x.key)}">Visszaállítás</button></td></tr>`).join('')}</tbody></table></div>`;}
    /**
   * A <code>listMarkup</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function listMarkup(){return categories().map(category=>{const data=visibleItems().filter(x=>x.category===category);return `<section class="configuration-list-category" id="configuration-category-${esc(category)}" data-category-block="${esc(category)}"><div class="configuration-list-category-head"><h2>${esc(labels[category]||category)}</h2><span>${data.length} beállítás</span></div>${listTable(data)}</section>`;}).join('');}
    /**
   * Megjeleníti vagy újrarendereli a render content állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function renderContent(){const data=visibleItems().filter(x=>x.category===activeCategory);if(!visibleItems().length){$('configurationContent').innerHTML=`<div class="configuration-empty-state"><h2>Nincs hiányzó beállítás</h2><p>A jelenlegi szűrés szerint minden konfigurációs kulcshoz tartozik kitöltött érték.</p></div>`;updateSummary();return;}if(viewMode==='list'){$('configurationContent').innerHTML=`<div class="configuration-section-head"><div><h2>Összes konfiguráció</h2><p>${advanced?'Minden technikai paraméter látható.':'Az egyszerű módban elérhető beállítások.'} A beállítások kategóriánkénti blokkokban, egy oldalon szerkeszthetők.</p></div></div><div class="configuration-all-list">${listMarkup()}</div>`;updateSummary();focusPendingItem();return;}let extra='';if(activeCategory==='HALOZAT')extra=proxyManagerMarkup();if(activeCategory==='TANUSITVANY')extra=certificateManagerMarkup();$('configurationContent').innerHTML=`<div class="configuration-section-head"><div><h2>${esc(labels[activeCategory]||activeCategory)}</h2><p>${advanced?'Minden technikai paraméter látható.':'A leggyakrabban szükséges beállítások.'} Kártyás szerkesztőnézet.</p></div></div><div class="configuration-grid">${data.map(cardMarkup).join('')}</div>${extra}`;updateSummary();if(activeCategory==='HALOZAT')loadDedicatedProxySettings().catch(e=>show(e.message,'error'));if(activeCategory==='TANUSITVANY')loadCertificates();focusPendingItem();}
    /**
   * A <code>focusPendingItem</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function focusPendingItem(){if(!pendingFocusKey)return;const el=document.querySelector(`[data-key="${CSS.escape(pendingFocusKey)}"]`);const row=document.querySelector(`[data-config-item="${CSS.escape(pendingFocusKey)}"]`);pendingFocusKey='';row?.scrollIntoView({behavior:'smooth',block:'center'});setTimeout(()=>el?.focus({preventScroll:true}),180);}
    /**
   * A <code>searchMatches</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} term a függvény term bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function searchMatches(term){const q=normalize(term).trim();if(!q)return[];return items.filter(x=>normalize(`${x.key} ${x.label} ${x.description} ${labels[x.category]||x.category}`).includes(q)).slice(0,12);}
    /**
   * Megjeleníti vagy újrarendereli a render search results állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function renderSearchResults(){const input=$('configurationSearch'),box=$('configurationSearchResults'),matches=searchMatches(input.value);if(!input.value.trim()){box.hidden=true;input.setAttribute('aria-expanded','false');box.innerHTML='';return;}box.innerHTML=matches.length?matches.map((x,i)=>`<button type="button" role="option" data-search-key="${esc(x.key)}" class="configuration-search-result"><span><strong>${esc(x.label)}</strong><small>${esc(labels[x.category]||x.category)}</small></span><code>${esc(x.key)}</code><p>${esc(x.description)}</p></button>`).join(''):'<div class="configuration-search-empty">Nincs találat.</div>';box.hidden=false;input.setAttribute('aria-expanded','true');}
    /**
   * Feloldja a select search result eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} key a függvény key bemeneti értéke
   */
function selectSearchResult(key){const item=items.find(x=>x.key===key);if(!item)return;if(item.advanced)advanced=true;activeCategory=item.category;pendingFocusKey=item.key;$('configurationSearchResults').hidden=true;$('configurationSearch').setAttribute('aria-expanded','false');render();}
    /**
   * A <code>proxyManagerMarkup</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function proxyManagerMarkup(){return `<section class="configuration-tool-card proxy-manager"><div class="tool-card-head"><div><h3>Alkalmazásspecifikus proxyk</h3><p>A GitHub és a NAV M2M kapcsolat külön proxykonfigurációt használhat. A GitHub proxy a <code>system_configuration</code> és <code>system_secret</code> táblák GitHub-specifikus kulcsaiból, az M2M proxy a központi hálózati proxykulcsokból töltődik be.</p></div></div><div class="dedicated-proxy-grid">${proxyFormMarkup('github','GitHub proxy','Az Űrlapsablon-katalógus GitHub API és release letöltéseihez.','https://api.github.com')}${proxyFormMarkup('m2m','M2M proxy','A NAV M2M beküldési és státuszlekérdezési HTTP klienshez.','https://m2m-dev.nav.gov.hu')}</div><section class="configuration-tool-card nested-tool-card"><h3>Általános kapcsolat tesztelése</h3><div class="inline-tool-row"><input id="networkTestUrl" value="https://api.github.com" placeholder="https://..."><button type="button" id="networkTestButton" class="secondary">Kapcsolat tesztelése</button></div><div id="networkTestResult" class="tool-result"></div></section></section>`;}
    /**
   * A <code>proxyFormMarkup</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} prefix a függvény prefix bemeneti értéke
   * @param {*} title a függvény title bemeneti értéke
   * @param {*} description a függvény description bemeneti értéke
   * @param {*} testUrl a függvény testUrl bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function proxyFormMarkup(prefix,title,description,testUrl){const tlsControl=prefix==='m2m'?'<span class="configuration-muted">A TLS tanúsítvány-ellenőrzés kötelező.</span>':`<label class="inline-checkbox"><input id="${prefix}SslVerificationDisabled" type="checkbox"> TLS ellenőrzés kikapcsolása</label>`;return `<form id="${prefix}ProxyForm" class="dedicated-proxy-card"><div class="tool-card-head"><div><h4>${title}</h4><p>${description}</p></div><label class="inline-checkbox"><input id="${prefix}ProxyEnabled" type="checkbox"> Bekapcsolva</label></div><div class="proxy-form-grid"><label>Proxy host vagy URL<input id="${prefix}ProxyUrl" type="text" placeholder="proxy.example.hu vagy http://proxy.example.hu"></label><label>Port<input id="${prefix}ProxyPort" type="number" min="1" max="65535" placeholder="8080"></label><label>Felhasználónév<input id="${prefix}ProxyUsername" type="text"></label><label>Jelszó<input id="${prefix}ProxyPassword" type="password" placeholder="Változatlanul hagyáshoz maradjon üres"><small id="${prefix}ProxyPasswordHint"></small></label><label>Truststore útvonal<input id="${prefix}TrustStorePath" type="text"></label><label>Truststore típusa<select id="${prefix}TrustStoreType"><option value="JKS">JKS</option><option value="PKCS12">PKCS12</option></select></label><label>Truststore jelszó<input id="${prefix}TrustStorePassword" type="password" placeholder="Változatlanul hagyáshoz maradjon üres"><small id="${prefix}TrustStorePasswordHint"></small></label>${tlsControl}<label class="inline-checkbox"><input id="${prefix}ProxyClearPassword" type="checkbox"> Mentett proxy jelszó törlése</label><label class="inline-checkbox"><input id="${prefix}TrustStoreClearPassword" type="checkbox"> Mentett truststore jelszó törlése</label></div><div class="proxy-card-actions"><span id="${prefix}ProxyUpdatedAt" class="configuration-muted">Betöltés…</span><span class="configuration-muted">Teszt cél: konfigurált M2M common endpoint</span><button type="button" class="secondary" data-test-proxy="${prefix}" ${prefix==='github'?'hidden':''}>Teszt</button><button type="submit" class="primary">Mentés</button></div><pre id="${prefix}ProxyTestResult" class="tool-result proxy-test-result"></pre></form>`;}
    /**
   * A <code>proxyPayload</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} prefix a függvény prefix bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function proxyPayload(prefix){const port=$(`${prefix}ProxyPort`).value.trim();return {enabled:$(`${prefix}ProxyEnabled`).checked,proxyUrl:$(`${prefix}ProxyUrl`).value.trim(),proxyPort:port?Number(port):null,username:$(`${prefix}ProxyUsername`).value.trim(),password:$(`${prefix}ProxyPassword`).value,clearPassword:$(`${prefix}ProxyClearPassword`).checked,sslVerificationDisabled:prefix==='m2m'?false:!!$(`${prefix}SslVerificationDisabled`)?.checked,trustStorePath:$(`${prefix}TrustStorePath`).value.trim(),trustStorePassword:$(`${prefix}TrustStorePassword`).value,clearTrustStorePassword:$(`${prefix}TrustStoreClearPassword`).checked,trustStoreType:$(`${prefix}TrustStoreType`).value||'JKS'};}
    /**
   * A <code>fillProxyForm</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} prefix a függvény prefix bemeneti értéke
   * @param {*} data a függvény data bemeneti értéke
   */
function fillProxyForm(prefix,data={}){$(`${prefix}ProxyEnabled`).checked=!!data.enabled;$(`${prefix}ProxyUrl`).value=data.proxyUrl||'';$(`${prefix}ProxyPort`).value=data.proxyPort||'';$(`${prefix}ProxyUsername`).value=data.username||'';$(`${prefix}ProxyPassword`).value='';$(`${prefix}ProxyClearPassword`).checked=false;const tlsToggle=$(`${prefix}SslVerificationDisabled`);if(tlsToggle)tlsToggle.checked=!!data.sslVerificationDisabled;$(`${prefix}TrustStorePath`).value=data.trustStorePath||'';$(`${prefix}TrustStorePassword`).value='';$(`${prefix}TrustStoreClearPassword`).checked=false;$(`${prefix}TrustStoreType`).value=data.trustStoreType||'JKS';$(`${prefix}ProxyUpdatedAt`).textContent=data.updatedAt?`Mentve: ${new Date(data.updatedAt).toLocaleString('hu-HU')}`:'Nincs mentett időpont';$(`${prefix}ProxyPasswordHint`).textContent=data.passwordConfigured?'Mentett proxy jelszó van.':'Nincs mentett proxy jelszó.';$(`${prefix}TrustStorePasswordHint`).textContent=data.trustStorePasswordConfigured?'Mentett truststore jelszó van.':'Nincs mentett truststore jelszó.';syncDedicatedProxyState(prefix);}

    /**
   * Szinkronizálja vagy frissíti a sync dedicated proxy state által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} prefix a függvény prefix bemeneti értéke
   */
function syncDedicatedProxyState(prefix){const enabled=$(`${prefix}ProxyEnabled`)?.checked===true;const form=$(`${prefix}ProxyForm`);if(!form)return;form.classList.toggle('proxy-disabled',!enabled);form.querySelectorAll('.proxy-form-grid input,.proxy-form-grid select,[data-test-proxy]').forEach(control=>{if(control.id===`${prefix}ProxyEnabled`)return;control.disabled=!enabled;});}
    /**
   * Szinkronizálja vagy frissíti a sync configuration proxy group által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} group a függvény group bemeneti értéke
   * @param {*} enabled a függvény enabled bemeneti értéke
   */
function syncConfigurationProxyGroup(group,enabled){document.querySelectorAll(`[data-proxy-config-group="${group}"]`).forEach(control=>{const isMaster=control.dataset.key===proxyEnabledKey(group);if(isMaster)return;control.disabled=!enabled;control.closest('[data-config-item]')?.classList.toggle('configuration-dependent-disabled',!enabled);});}
    /**
   * Betölti vagy lekéri a load dedicated proxy settings művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadDedicatedProxySettings(){for(const [prefix,url] of [['github','/api/github-proxy-settings'],['m2m','/api/m2m-proxy-settings']]){const r=await fetch(url,{cache:'no-store'});if(!r.ok)throw new Error(`${prefix.toUpperCase()} proxy betöltési hiba: ${await r.text()}`);fillProxyForm(prefix,await r.json());}}
    /**
   * Előkészíti és elindítja a save dedicated proxy állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
   * @param {*} prefix a függvény prefix bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function saveDedicatedProxy(prefix){const url=prefix==='github'?'/api/github-proxy-settings':'/api/m2m-proxy-settings';const payload=proxyPayload(prefix);delete payload.testUrl;const r=await fetch(url,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});const body=await r.json().catch(()=>({}));if(!r.ok)throw new Error('A proxybeállítás mentése sikertelen.');fillProxyForm(prefix,body);show(`${prefix==='github'?'GitHub':'M2M'} proxybeállítások mentve.`,'success');}
    /**
   * A <code>testDedicatedProxy</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} prefix a függvény prefix bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function testDedicatedProxy(prefix){if(prefix!=='m2m')return;const r=await fetch('/api/m2m-proxy-settings/test',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(proxyPayload(prefix))});const body=await r.json().catch(()=>({}));if(!r.ok){$(`${prefix}ProxyTestResult`).textContent='Az M2M proxyteszt sikertelen.';return;}$(`${prefix}ProxyTestResult`).textContent=[body.success?'Sikeres proxy/TLS teszt.':'Sikertelen proxy/TLS teszt.',body.httpStatus!=null&&`HTTP státusz: ${body.httpStatus}`,body.durationMs!=null&&`Időtartam: ${body.durationMs} ms`].filter(Boolean).join('\n');}

    /**
   * A <code>certificateManagerMarkup</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function certificateManagerMarkup(){return `<section class="configuration-tool-card certificate-manager"><div class="tool-card-head"><div><h3>Megbízható tanúsítványok</h3><p>Központi alkalmazás-truststore: PEM, CRT, CER, JKS, P12 vagy PFX importálása, illetve távoli HTTPS lánc előnézete és jóváhagyott importja. Az M2M kliens ezt használja, ha nincs külön truststore útvonal megadva.</p></div><button type="button" id="reloadCertificates" class="secondary">Frissítés</button></div><div class="certificate-tools"><form id="certificateUploadForm" class="certificate-tool"><h4>Fájl importálása</h4><input id="certificateFile" type="file" accept=".pem,.crt,.cer,.jks,.p12,.pfx" required><input id="certificatePassword" type="password" placeholder="JKS/P12 jelszó, ha szükséges"><button class="secondary" type="submit">Importálás</button></form><form id="remoteCertificateForm" class="certificate-tool"><h4>Távoli HTTPS-végpont</h4><input id="remoteCertificateHost" placeholder="pelda.hu" required><input id="remoteCertificatePort" type="number" value="443" min="1" max="65535"><input id="remoteCertificateAlias" placeholder="Alias"><div><button class="secondary" type="button" id="previewRemoteCertificate">Előnézet</button><button class="primary" type="submit">Lekérés és importálás</button></div></form></div><div id="certificateResult" class="tool-result"></div><div class="certificate-table-wrap"><table class="certificate-table"><thead><tr><th>Alias</th><th>Subject</th><th>Issuer</th><th>Érvényes</th><th>Állapot</th><th></th></tr></thead><tbody id="certificateRows"><tr><td colspan="6">Betöltés…</td></tr></tbody></table></div></section>`;}
    /**
   * Betölti vagy lekéri a load certificates művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadCertificates(){const rows=$('certificateRows');if(!rows)return;const r=await fetch('/api/admin/certificates',{cache:'no-store'});if(!r.ok){rows.innerHTML=`<tr><td colspan="6">${esc(await r.text())}</td></tr>`;return;}const list=await r.json();rows.innerHTML=list.length?list.map(c=>`<tr><td>${esc(c.alias)}</td><td title="${esc(c.subjectDn)}">${esc(c.subjectDn)}</td><td title="${esc(c.issuerDn)}">${esc(c.issuerDn)}</td><td>${esc(new Date(c.validUntil).toLocaleDateString('hu-HU'))}</td><td><span class="certificate-status ${esc(String(c.status).toLowerCase())}">${esc(c.status)}</span></td><td><button type="button" class="danger-link" data-delete-certificate="${c.id}">Törlés</button></td></tr>`).join(''):'<tr><td colspan="6">Nincs importált tanúsítvány.</td></tr>';}
    /**
   * A <code>remoteCertificate</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} importCertificate a függvény importCertificate bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function remoteCertificate(importCertificate){const host=$('remoteCertificateHost').value.trim(),port=Number($('remoteCertificatePort').value||443),alias=$('remoteCertificateAlias').value.trim();const r=await fetch('/api/admin/certificates/remote',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({host,port,alias,importCertificate})});const body=await r.json().catch(()=>[]);if(!r.ok)throw new Error('A tanúsítvány lekérése sikertelen.');$('certificateResult').textContent=`${body.length} tanúsítvány ${importCertificate?'importálva':'lekérve'}.`;if(importCertificate)await loadCertificates();else {const result=$('certificateResult');result.replaceChildren();body.forEach(c=>{const row=document.createElement('div'),strong=document.createElement('strong'),code=document.createElement('code');strong.textContent=String(c.subjectDn||'');code.textContent=String(c.sha256Fingerprint||'');row.append(strong,document.createElement('br'),code,document.createElement('br'),document.createTextNode(`Érvényes: ${new Date(c.validUntil).toLocaleString('hu-HU')}`));result.append(row);});}}

    /**
   * Eltávolítja vagy alaphelyzetbe állítja a reset configuration key művelethez tartozó kliensoldali állapotot.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} key a függvény key bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function resetConfigurationKey(key){
    const confirmed=await window.navConfirm({
      eyebrow:'Megerősítés',
      title:'Beállítás alaphelyzetbe állítása',
      message:`Biztosan visszaállítja alapértékre ezt a beállítást?\n${key}`,
      confirmText:'Visszaállítás',
      cancelText:'Mégse'
    });
    if(!confirmed)return;
    const r=await fetch('/api/admin/configuration/reset',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({keys:[key]})});
    const body=await r.json().catch(()=>({}));
    if(!r.ok)throw new Error('Az alapérték visszaállítása sikertelen.');
    show(body.restartRequired?'A beállítás alapértékre állt. Az alkalmazást újra kell indítani.':'A beállítás alapértékre állt.','success');
    await load();
  }
    /**
   * A <code>exportConfiguration</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
async function exportConfiguration(){
    const confirmed=await window.navConfirm({
      eyebrow:'Biztonsági figyelmeztetés',
      title:'Teljes konfiguráció exportálása',
      message:'Az export a teljes rendszerkonfigurációt tartalmazza. A rendszer-secretek titkosított formában maradnak, de a bootstrap fájl adatbázis-kapcsolati adatai is bekerülnek. Az exportfájlt bizalmasan kell kezelni.',
      confirmText:'Exportálás',
      cancelText:'Mégse'
    });
    if(!confirmed)return;
    const response=await fetch('/api/admin/configuration/export',{cache:'no-store'});
    if(!response.ok)throw new Error('A teljes konfiguráció exportja sikertelen.');
    const exportDocument=await response.json();
    const blob=new Blob([JSON.stringify(exportDocument,null,2)],{type:'application/json;charset=utf-8'});
    const a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download=`m2m-xml-editor-configuration-${new Date().toISOString().replace(/[:.]/g,'-')}.json`;a.click();URL.revokeObjectURL(a.href);
  }
    /**
   * A <code>importConfigurationFile</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} file a függvény file bemeneti értéke
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function importConfigurationFile(file){
    const data=JSON.parse(await file.text());
    const response=await fetch('/api/admin/configuration/import',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(data)});
    const result=await response.json().catch(()=>({}));
    if(!response.ok)throw new Error(result.message||'A konfiguráció importja sikertelen.');
    await load();
    const merged=(result.databaseUpdated||0)+(result.databaseInserted||0)+(result.secretsUpdated||0)+(result.secretsInserted||0)+(result.certificatesUpdated||0)+(result.certificatesInserted||0)+(result.bootstrapMerged||0)+(result.propertyFilesMerged||0)+(result.textFilesReplaced||0);
    show(`${merged} konfigurációs érték MERGE importja megtörtént.${result.restartRequired?' Az alkalmazást újra kell indítani.':''}`,'success');
  }

    /**
   * Szinkronizálja vagy frissíti a update summary által kezelt állapotot a megadott adatok alapján.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function updateSummary(){const count=dirty.size;$('configurationSummary').textContent=count?`${count} módosított beállítás vár mentésre.`:'Nincs mentetlen módosítás.';}
    /**
   * Megjeleníti vagy újrarendereli a render állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function render(){renderTabs();renderContent();$('simpleMode').classList.toggle('active',!advanced);$('advancedMode').classList.toggle('active',advanced);$('simpleMode').setAttribute('aria-pressed',String(!advanced));$('advancedMode').setAttribute('aria-pressed',String(advanced));$('cardView').classList.toggle('active',viewMode==='card');$('listView').classList.toggle('active',viewMode==='list');$('cardView').setAttribute('aria-pressed',String(viewMode==='card'));$('listView').setAttribute('aria-pressed',String(viewMode==='list'));$('missingOnlyFilter').classList.toggle('active',missingOnly);$('missingOnlyFilter').setAttribute('aria-pressed',String(missingOnly));$('missingConfigurationCount').textContent=String(missingCount());document.querySelector('.configuration-tab.active')?.scrollIntoView({block:'nearest',inline:'nearest'});}
    /**
   * Betölti vagy lekéri a load runtime művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadRuntime(){const r=await fetch('/api/admin/configuration/runtime',{cache:'no-store'});if(!r.ok)return;const runtime=await r.json();const button=$('restartApplication');button.hidden=!(runtime.standalone&&runtime.restartAvailable);}
    /**
   * Elrejti vagy lezárja a close standalone browser window felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function closeStandaloneBrowserWindow(){
    window.setTimeout(()=>{
      try{window.open('','_self');window.close();}catch(ignore){}
      window.setTimeout(()=>{
        if(!window.closed){
          document.documentElement.innerHTML='<head><title>Alkalmazás újraindítása</title></head><body class=\"nav-application-theme\"><main style=\"min-height:100vh;display:grid;place-items:center;padding:24px;font-family:Arial,sans-serif\"><section style=\"max-width:560px;text-align:center\"><h1>Az alkalmazás újraindul</h1><p>Ez a böngészőablak automatikusan bezáródik. Amennyiben a böngésző biztonsági beállítása ezt megakadályozza, az ablak kézzel bezárható.</p></section></main></body>';
          try{window.close();}catch(ignore){}
        }
      },250);
    },120);
  }
    /**
   * A <code>restartApplication</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function restartApplication(){
    const confirmed=window.navConfirm?await window.navConfirm({
      title:'Alkalmazás újraindítása',
      message:'Az alkalmazás leáll, majd automatikusan újraindul. A jelenlegi böngészőablak bezáródik. Folytatja?',
      confirmText:'Újraindítás',
      cancelText:'Mégsem',
      variant:'warning',
      eyebrow:'Rendszerművelet'
    }):false;
    if(!confirmed)return;
    const r=await fetch('/api/admin/configuration/restart',{method:'POST',keepalive:true});
    const body=await r.json().catch(()=>({}));
    if(!r.ok)throw new Error(body.message||'Az újraindítás nem indítható el.');
    show('Az alkalmazás újraindítása folyamatban van…','info');
    closeStandaloneBrowserWindow();
  }
    /**
   * Betölti vagy lekéri a load művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function load(){dirty.clear();sensitiveTouched.clear();$('configurationContent').innerHTML='<div class="configuration-loading">Konfiguráció betöltése…</div>';const r=await fetch('/api/admin/configuration',{cache:'no-store'});if(!r.ok)throw new Error(await r.text());items=await r.json();render();}
    /**
   * Előkészíti és elindítja a save állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function save(){const values={};document.querySelectorAll('[data-key]').forEach(el=>{if(dirty.has(el.dataset.key))values[el.dataset.key]=el.value;});if(!Object.keys(values).length)return show('Nincs mentendő módosítás.','info');const r=await fetch('/api/admin/configuration',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({values,confirmedSensitiveKeys:[...sensitiveTouched]})});let data;try{data=await r.json()}catch{data={message:await r.text()}}if(!r.ok)throw new Error(data.message||'A mentés sikertelen.');show(data.restartRequired?'A beállítások mentése sikeres. Az érintett módosítások alkalmazásához az alkalmazást kézzel újra kell indítani.':'A beállítások mentése sikeres.','success');await load();}
    /**
   * Megjeleníti vagy újrarendereli a show állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
   * @param {*} type a függvény type bemeneti értéke
   */
function show(message,type){const n=$('configurationNotice');n.hidden=false;n.className='configuration-notice '+type;n.textContent=message;n.scrollIntoView({behavior:'smooth',block:'nearest'});}
  $('configurationTabs').onclick=e=>{const b=e.target.closest('[data-category]');if(!b)return;activeCategory=b.dataset.category;if(viewMode==='list'){document.querySelectorAll('.configuration-tab').forEach(tab=>tab.classList.toggle('active',tab.dataset.category===activeCategory));document.getElementById(`configuration-category-${activeCategory}`)?.scrollIntoView({behavior:'smooth',block:'start'});}else render();};
    /**
   * A <code>activateSensitiveInput</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   */
function activateSensitiveInput(event){const el=event.target.closest('[data-key][data-sensitive="true"]');if(!el)return;el.readOnly=false;sensitiveTouched.add(el.dataset.key);}
  $('configurationContent').addEventListener('pointerdown',activateSensitiveInput);
  $('configurationContent').addEventListener('keydown',activateSensitiveInput);
  $('configurationContent').addEventListener('input',e=>{const dedicatedToggle=e.target.closest('#githubProxyEnabled,#m2mProxyEnabled');if(dedicatedToggle){syncDedicatedProxyState(dedicatedToggle.id.startsWith('github')?'github':'m2m');return;}const el=e.target.closest('[data-key]');if(!el)return;if(el.dataset.sensitive==='true'&&!sensitiveTouched.has(el.dataset.key)){el.value=el.dataset.original||'';return;}const changed=el.value!==el.dataset.original;changed?dirty.add(el.dataset.key):dirty.delete(el.dataset.key);el.closest('[data-config-item]')?.classList.toggle('changed',changed);const group=proxyGroupForKey(el.dataset.key);if(group&&el.dataset.key===proxyEnabledKey(group))syncConfigurationProxyGroup(group,String(el.value)==='true');updateSummary();});
  $('simpleMode').onclick=()=>{advanced=false;render();};$('advancedMode').onclick=()=>{advanced=true;render();};$('cardView').onclick=()=>{viewMode='card';localStorage.setItem('m2mConfigurationView',viewMode);render();};$('listView').onclick=()=>{viewMode='list';localStorage.setItem('m2mConfigurationView',viewMode);render();};$('missingOnlyFilter').onclick=()=>{missingOnly=!missingOnly;localStorage.setItem('m2mConfigurationMissingOnly',String(missingOnly));render();};$('configurationSearch').addEventListener('input',()=>{clearTimeout(searchTimer);searchTimer=setTimeout(renderSearchResults,100);});$('configurationSearch').addEventListener('keydown',e=>{if(e.key==='Escape'){$('configurationSearchResults').hidden=true;e.currentTarget.setAttribute('aria-expanded','false');}if(e.key==='ArrowDown'){e.preventDefault();$('configurationSearchResults').querySelector('button')?.focus();}});$('configurationSearchResults').addEventListener('click',e=>{const result=e.target.closest('[data-search-key]');if(result)selectSearchResult(result.dataset.searchKey);});document.addEventListener('click',e=>{if(!e.target.closest('.configuration-search-wrap')){$('configurationSearchResults').hidden=true;$('configurationSearch').setAttribute('aria-expanded','false');}});  /**
   * Elrejti vagy lezárja a close tools menu felületi állapotát, és szükség esetén rendezi a kapcsolódó UI-state-et.
   *
   * <p>A függvény a közös alkalmazás-komponenseket használja, és a hozzá tartozó nyitott/zárt, fókusz- vagy visszajelzési állapotot konzisztensen tartja.</p>
   */
function closeToolsMenu(){const menu=$('configurationToolsMenu');if(menu)menu.open=false;}$('exportConfiguration').onclick=()=>{closeToolsMenu();exportConfiguration().catch(e=>show(e.message,'error'));};$('importConfiguration').onclick=()=>{closeToolsMenu();$('importConfigurationFile').click();};$('importConfigurationFile').onchange=e=>{const file=e.target.files?.[0];if(file)importConfigurationFile(file).catch(err=>show(err.message,'error'));e.target.value='';};$('reloadConfiguration').onclick=()=>{closeToolsMenu();load().catch(e=>show(e.message,'error'));};$('restartApplication').onclick=()=>{closeToolsMenu();restartApplication().catch(e=>show(e.message,'error'));};$('saveConfiguration').onclick=$('saveConfigurationBottom').onclick=()=>save().catch(e=>show(e.message,'error'));document.addEventListener('click',e=>{const menu=$('configurationToolsMenu');if(menu?.open&&!e.target.closest('#configurationToolsMenu'))menu.open=false;});
  $('configurationContent').addEventListener('click',async e=>{try{const passwordToggle=e.target.closest('[data-password-toggle]');if(passwordToggle){const input=document.getElementById(passwordToggle.dataset.passwordToggle);if(input){activateSensitiveInput({target:input});const visible=input.type==='text';input.type=visible?'password':'text';const label=visible?'Érték megjelenítése':'Érték elrejtése';passwordToggle.setAttribute('aria-label',label);passwordToggle.setAttribute('title',label);passwordToggle.setAttribute('aria-pressed',String(!visible));}return;}const reset=e.target.closest('[data-reset-key]');if(reset){await resetConfigurationKey(reset.dataset.resetKey);return;}const proxyTest=e.target.closest('[data-test-proxy]');if(proxyTest)await testDedicatedProxy(proxyTest.dataset.testProxy);if(e.target.id==='networkTestButton'){const r=await fetch('/api/admin/network/test',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({url:$('networkTestUrl').value})});const b=await r.json().catch(()=>({}));if(!r.ok)throw new Error(b.message||'A teszt sikertelen.');$('networkTestResult').textContent=`Sikeres kapcsolat: HTTP ${b.status}, ${b.elapsedMs} ms`;}if(e.target.id==='reloadCertificates')await loadCertificates();if(e.target.id==='previewRemoteCertificate')await remoteCertificate(false);const d=e.target.closest('[data-delete-certificate]');if(d){const confirmed=await window.navConfirm({eyebrow:'Megerősítés',title:'Tanúsítvány törlése',message:'Biztosan törli a tanúsítványt?',confirmText:'Törlés',cancelText:'Mégse'});if(confirmed){const r=await fetch('/api/admin/certificates/'+d.dataset.deleteCertificate,{method:'DELETE'});if(!r.ok)throw new Error(await r.text());await loadCertificates();}}}catch(err){show(err.message,'error');}});
  $('configurationContent').addEventListener('submit',async e=>{try{if(e.target.id==='githubProxyForm'||e.target.id==='m2mProxyForm'){e.preventDefault();await saveDedicatedProxy(e.target.id.startsWith('github')?'github':'m2m');}if(e.target.id==='certificateUploadForm'){e.preventDefault();const fd=new FormData();fd.append('file',$('certificateFile').files[0]);fd.append('password',$('certificatePassword').value);const r=await fetch('/api/admin/certificates/import',{method:'POST',body:fd});if(!r.ok)throw new Error(await r.text());$('certificateResult').textContent='A tanúsítvány importálása sikeres.';await loadCertificates();}if(e.target.id==='remoteCertificateForm'){e.preventDefault();await remoteCertificate(true);}}catch(err){show(err.message,'error');}});
  const searchInput=$('configurationSearch');searchInput.value='';  /**
   * A <code>unlockSearch</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
const unlockSearch=()=>{searchInput.readOnly=false;if(searchInput.value==='admin')searchInput.value='';};searchInput.addEventListener('pointerdown',unlockSearch,{once:true});searchInput.addEventListener('keydown',unlockSearch,{once:true});searchInput.addEventListener('focus',unlockSearch,{once:true});loadRuntime().catch(()=>{});
  window.addEventListener('beforeunload',e=>{if(dirty.size){e.preventDefault();e.returnValue='';}});load().catch(e=>show(e.message,'error'));
})();
