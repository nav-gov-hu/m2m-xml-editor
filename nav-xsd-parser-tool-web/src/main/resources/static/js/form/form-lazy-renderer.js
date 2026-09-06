/**
 * @module form/form-lazy-renderer
 *
 * A űrlap-megjelenítési és mezőkötési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

/**
 * Viewport/collapse driven form section hydration.
 * Keeps off-screen and collapsed sections out of the DOM until they are needed.
 */
export function createFormLazyRenderer({ getScrollRoot, onHydrated } = {}){
  let entries = new Map();
  let observer = null;

    /**
   * Előkészíti és elindítja a create observer állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function createObserver(){
    if(observer || typeof IntersectionObserver !== 'function') return observer;
    observer = new IntersectionObserver(records => {
      records.forEach(record => {
        if(record.isIntersecting) hydrate(record.target);
      });
    }, {
      root: typeof getScrollRoot === 'function' ? getScrollRoot() : null,
      rootMargin: '700px 0px',
      threshold: 0.01
    });
    return observer;
  }

    /**
   * Eltávolítja vagy alaphelyzetbe állítja a reset művelethez tartozó kliensoldali állapotot.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function reset(){
    observer?.disconnect();
    observer = null;
    entries.clear();
  }

    /**
   * A <code>placeholder</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} content a függvény content bemeneti értéke
   * @param {*} label a függvény label bemeneti értéke
   */
function placeholder(content, label){
    const node = document.createElement('div');
    node.className = 'form-lazy-placeholder';
    node.setAttribute('aria-hidden', 'true');
    node.textContent = label || 'A mezők szükség esetén töltődnek be…';
    content.replaceChildren(node);
  }

    /**
   * Előkészíti és elindítja a register állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} card a függvény card bemeneti értéke
   * @param {*} content a függvény content bemeneti értéke
   * @param {*} renderer a függvény renderer bemeneti értéke
   * @param {*} options a művelet opcionális beállításai
   */
function register(card, content, renderer, options = {}){
    if(!card || !content || typeof renderer !== 'function') return;
    const entry = {
      card,
      content,
      renderer,
      rendered: false,
      observe: options.observe !== false,
      label: options.label || '',
      searchText: String(options.searchText || '').toLocaleLowerCase('hu-HU'),
      matchesQuery: typeof options.matchesQuery === 'function' ? options.matchesQuery : null
    };
    entries.set(card, entry);
    card.dataset.lazyFormSection = 'true';
    content.dataset.lazyFormContent = 'true';
    placeholder(content, entry.label);

    if(options.eager === true){
      hydrate(card);
      return;
    }

    if(entry.observe){
      const activeObserver = createObserver();
      if(activeObserver) activeObserver.observe(card);
      else hydrate(card);
    }
  }

    /**
   * A <code>hydrate</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} card a függvény card bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function hydrate(card){
    const entry = entries.get(card);
    if(!entry || entry.rendered) return false;
    entry.rendered = true;
    observer?.unobserve(card);
    entry.content.replaceChildren();
    entry.renderer(entry.content);
    card.dataset.lazyFormRendered = 'true';
    try{ onHydrated?.(card, entry.content); }catch(error){ console.warn('Lazy form section post-render failed', error); }
    return true;
  }

    /**
   * A <code>ensureSection</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} card a függvény card bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function ensureSection(card){
    return hydrate(card);
  }

    /**
   * A <code>ensureAll</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function ensureAll(){
    entries.forEach((_entry, card) => hydrate(card));
  }

    /**
   * A <code>ensureMatching</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} query a függvény query bemeneti értéke
   * @returns {*} a feldolgozás eredménye
   */
function ensureMatching(query){
    const terms = Array.isArray(query)
      ? query.map(value => String(value || '').trim().toLocaleLowerCase('hu-HU')).filter(Boolean)
      : String(query || '').trim().toLocaleLowerCase('hu-HU').split(/\s+/).filter(Boolean);
    if(!terms.length) return 0;
    let matched = 0;
    entries.forEach((entry, card) => {
      const matches = entry.matchesQuery
        ? entry.matchesQuery(terms)
        : terms.every(term => entry.searchText.includes(term));
      if(matches){
        matched += 1;
        hydrate(card);
      }
    });
    return matched;
  }

    /**
   * A <code>pendingCount</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {*} a feldolgozás eredménye
   */
function pendingCount(){
    let count = 0;
    entries.forEach(entry => { if(!entry.rendered) count += 1; });
    return count;
  }

  return { reset, register, hydrate, ensureSection, ensureAll, ensureMatching, pendingCount };
}
