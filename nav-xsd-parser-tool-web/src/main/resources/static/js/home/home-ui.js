/**
 * @module home/home-ui
 *
 * A kezdőoldali működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

const README_SOURCE_URL = '/docs/README.md';

/**
 * Kezdőoldali health/config információk és README megjelenítés.
 */
export function createHomeUi({ elements, escapeHtml, onConfigLoaded, syncSelectedXmlSourceDisplay }){
  const {
    healthBadge,
    healthSummary,
    configBadge,
    homeConfigSummary,
    appVersionFooter,
    schemaDirInput,
    generalXsdDirInput,
    xmlPathInput
  } = elements;

    /**
   * Ellenőrzi a check health feltételeit, és a hívó számára döntési eredményt állít elő.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function checkHealth(){
    try{
      const response = await fetch('/api/health');
      const data = await response.json();
      if(healthBadge) healthBadge.textContent = `${data.application} · ${data.status}`;
      if(healthSummary) healthSummary.textContent = `${data.application} · ${data.status}`;
    }catch{
      if(healthBadge) healthBadge.textContent = 'A szerver nem érhető el';
      if(healthSummary) healthSummary.textContent = 'A szerver nem érhető el';
    }
  }

    /**
   * Betölti vagy lekéri a load config művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<*>} a feldolgozás eredménye
   */
async function loadConfig(){
    try{
      const response = await fetch('/api/config', { cache:'no-store' });
      if(!response.ok) throw new Error('A konfiguráció nem érhető el');
      const data = await response.json();
      onConfigLoaded?.(data);
      if(appVersionFooter && data.appVersion) appVersionFooter.textContent = `Verzió: ${data.appVersion}`;
      if(data.schemaDir && schemaDirInput && !schemaDirInput.value.trim()) schemaDirInput.value = data.schemaDir;
      if(data.generalXsdPath && generalXsdDirInput && !generalXsdDirInput.value.trim()) generalXsdDirInput.value = data.generalXsdPath;
      if(data.defaultXmlPath && xmlPathInput && !xmlPathInput.value.trim()) xmlPathInput.value = data.defaultXmlPath;
      syncSelectedXmlSourceDisplay?.();
      const parts = [
        data.schemaDir ? `XSD mappa:\n  ${data.schemaDir}` : null,
        data.generalXsdPath ? `Common XSD:\n  ${data.generalXsdPath}` : null,
        data.xpathRuleDir ? `XPath szabályok:\n  ${data.xpathRuleDir}` : null
      ].filter(Boolean);
      const configText = parts.join('\n\n') || 'Nincs beállított konfiguráció';
      if(configBadge) configBadge.textContent = configText;
      if(homeConfigSummary) homeConfigSummary.innerHTML = configText.replace(/\n/g, '<br>');
      return data;
    }catch(error){
      console.warn('Konfiguráció betöltési hiba', error);
      if(configBadge) configBadge.textContent = 'A konfiguráció nem érhető el';
      if(homeConfigSummary) homeConfigSummary.innerHTML = 'A konfiguráció nem érhető el';
      return null;
    }
  }

    /**
   * Feldolgozza a parse readme sections bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} markdown a függvény markdown bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function parseReadmeSections(markdown){
    const lines = String(markdown || '').replace(/\r\n/g, '\n').split('\n');
    const sections = [];
    let current = null;
    let inFence = false;
    let fenceLines = [];

        /**
     * A <code>pushCurrent</code> függvény a kezdőoldali folyamat egy önálló feldolgozási lépését valósítja meg.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     */
function pushCurrent(){ if(current) sections.push(current); }
        /**
     * A <code>appendBlock</code> függvény a kezdőoldali folyamat egy önálló feldolgozási lépését valósítja meg.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @param {*} html a függvény html bemeneti értéke
     */
function appendBlock(html){ if(current) current.blocks.push(html); }
        /**
     * Megjeleníti vagy újrarendereli a render inline állapotát a felhasználói felületen.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @param {*} text a függvény text bemeneti értéke
     * @returns {*} a feldolgozás eredménye
     */
function renderInline(text){
      return escapeHtml(text || '')
        .replace(/`([^`]+)`/g, '<code>$1</code>')
        .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    }
        /**
     * A <code>flushFence</code> függvény a kezdőoldali folyamat egy önálló feldolgozási lépését valósítja meg.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     */
function flushFence(){
      appendBlock(`<pre><code>${escapeHtml(fenceLines.join('\n'))}</code></pre>`);
      fenceLines = [];
      inFence = false;
    }

    for(let i = 0; i < lines.length; i += 1){
      const raw = lines[i];
      const line = raw.trimEnd();
      const fenceMatch = line.match(/^```\s*(.*)$/);
      if(fenceMatch){
        if(inFence) flushFence();
        else { inFence = true; fenceLines = []; }
        continue;
      }
      if(inFence){ fenceLines.push(raw); continue; }
      const h1 = line.match(/^#\s+(.+)$/);
      if(h1){ pushCurrent(); current = { level:1, title:h1[1].trim(), blocks:[], wide:true }; continue; }
      const h2 = line.match(/^##\s+(.+)$/);
      if(h2){ pushCurrent(); current = { level:2, title:h2[1].trim(), blocks:[], wide:false }; continue; }
      const h3 = line.match(/^###\s+(.+)$/);
      if(h3){ appendBlock(`<h4>${renderInline(h3[1].trim())}</h4>`); continue; }
      if(!current) current = { level:1, title:'README', blocks:[], wide:true };
      if(!line.trim()) continue;
      if(line.startsWith('|') && lines[i + 1]?.trim().startsWith('|') && lines[i + 1].includes('---')){
        const tableLines = [line];
        i += 2;
        while(i < lines.length && lines[i].trim().startsWith('|')){ tableLines.push(lines[i].trim()); i += 1; }
        i -= 1;
        const rows = tableLines.map(row => row.split('|').slice(1, -1).map(cell => renderInline(cell.trim())));
        const header = rows.shift() || [];
        appendBlock(`<div class="readme-table-wrap"><table class="readme-table"><thead><tr>${header.map(cell => `<th>${cell}</th>`).join('')}</tr></thead><tbody>${rows.map(row => `<tr>${row.map(cell => `<td>${cell}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`);
        continue;
      }
      if(/^[-*]\s+/.test(line)){
        const items = [];
        let j = i;
        while(j < lines.length && /^[-*]\s+/.test(lines[j].trim())){ items.push(lines[j].trim().replace(/^[-*]\s+/, '')); j += 1; }
        appendBlock(`<ul>${items.map(item => `<li>${renderInline(item)}</li>`).join('')}</ul>`);
        i = j - 1;
        continue;
      }
      if(/^\d+\.\s+/.test(line)){
        const items = [];
        let j = i;
        while(j < lines.length && /^\d+\.\s+/.test(lines[j].trim())){ items.push(lines[j].trim().replace(/^\d+\.\s+/, '')); j += 1; }
        appendBlock(`<ol>${items.map(item => `<li>${renderInline(item)}</li>`).join('')}</ol>`);
        i = j - 1;
        continue;
      }
      appendBlock(`<p>${renderInline(line.trim())}</p>`);
    }
    if(inFence) flushFence();
    pushCurrent();
    return sections.filter(section => section.title || section.blocks.length);
  }

    /**
   * Megjeleníti vagy újrarendereli a render readme home állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} markdown a függvény markdown bemeneti értéke
   */
function renderReadmeHome(markdown){
    const status = document.getElementById('readmeStatus');
    const grid = document.getElementById('readmeSectionGrid');
    const toc = document.getElementById('readmeToc');
    if(!grid) return;
    const sections = parseReadmeSections(markdown);
    if(!sections.length){
      grid.innerHTML = '<article class="markdown-card markdown-card-wide"><h3>README nem tartalmaz megjeleníthető tartalmat.</h3></article>';
      if(status) status.textContent = 'README betöltve, de nincs megjeleníthető tartalom.';
      return;
    }
    const first = sections[0];
    const contentSections = sections.slice(1);
    if(status) status.textContent = `${sections.length} README fejezet betöltve.`;
    if(toc){
      toc.hidden = false;
      toc.innerHTML = contentSections.slice(0, 12).map((section, index) => `<a href="#readme-section-${index}">${escapeHtml(section.title)}</a>`).join('');
    }
    const introHtml = `<article class="markdown-card readme-intro-card markdown-card-wide"><h3>${escapeHtml(first.title)}</h3>${first.blocks.join('')}</article>`;
    const sectionHtml = contentSections.map((section, index) => {
      const textLength = section.blocks.join('').length;
      const wide = section.wide || textLength > 900 || /konfigur|képerny|jogosults|fő funkció|indítás|build/i.test(section.title);
      return `<article id="readme-section-${index}" class="markdown-card readme-generated-card ${wide ? 'markdown-card-wide' : ''}"><h3>${escapeHtml(section.title)}</h3>${section.blocks.join('')}</article>`;
    }).join('');
    grid.innerHTML = introHtml + sectionHtml;
  }

    /**
   * Betölti vagy lekéri a load readme home művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadReadmeHome(){
    const grid = document.getElementById('readmeSectionGrid');
    const status = document.getElementById('readmeStatus');
    if(!grid) return;
    try{
      if(status) status.textContent = 'README betöltése…';
      const response = await fetch(`${README_SOURCE_URL}?v=${encodeURIComponent(Date.now())}`, { cache:'no-store' });
      if(!response.ok) throw new Error(`README nem tölthető be (${response.status})`);
      renderReadmeHome(await response.text());
    }catch(error){
      console.error(error);
      if(status) status.textContent = 'README nem érhető el.';
      grid.innerHTML = `<article class="markdown-card markdown-card-wide"><h3>README betöltési hiba</h3><p>${escapeHtml(error.message || 'A README.md nem tölthető be.')}</p></article>`;
    }
  }

    /**
   * Kezeli vagy beköti a init esemény- és inicializációs folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function init(){
    document.getElementById('readmeReloadButton')?.addEventListener('click', loadReadmeHome);
  }

  return { init, checkHealth, loadConfig, loadReadmeHome, parseReadmeSections, renderReadmeHome };
}
