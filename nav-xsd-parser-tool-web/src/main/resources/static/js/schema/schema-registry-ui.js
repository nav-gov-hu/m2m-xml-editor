/**
 * @module schema/schema-registry-ui
 *
 * A séma-registry működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

/**
 * Séma-regisztri állapotkijelzés, újratöltés és polling.
 */
export function createSchemaRegistryUi({ elements, showMessage }){
  const { schemaRegistryText, schemaRegistryFill, reloadSchemaRegistryButton } = elements;
  let pollHandle = null;

    /**
   * Megjeleníti vagy újrarendereli a render status állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} status a kapcsolódó folyamat aktuális állapota
   */
function renderStatus(status){
    if(!schemaRegistryText || !schemaRegistryFill || !reloadSchemaRegistryButton) return;
    if(!status){
      schemaRegistryText.textContent = 'A sématár állapota nem elérhető';
      schemaRegistryFill.style.width = '0%';
      reloadSchemaRegistryButton.disabled = false;
      return;
    }
    const pct = Number(status.percentage || 0);
    schemaRegistryFill.style.width = `${pct}%`;
    if(status.loading){
      schemaRegistryText.textContent = `${status.phase || 'Beolvasás'} · ${status.processedFiles || 0}/${status.totalFiles || 0} XSD (${pct}%)`;
      reloadSchemaRegistryButton.disabled = true;
    }else{
      const countText = status.cacheEntryCount ? ` · ${status.cacheEntryCount} cache bejegyzés` : '';
      schemaRegistryText.textContent = `${status.phase || 'Kész'}${countText}`;
      reloadSchemaRegistryButton.disabled = false;
    }
  }

    /**
   * Betölti vagy lekéri a fetch status művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function fetchStatus(){
    try{
      const response = await fetch('/api/schema-registry/status');
      if(!response.ok) throw new Error('A sématár állapota nem érhető el');
      const status = await response.json();
      renderStatus(status);

      if(status.loading && !pollHandle){
        pollHandle = setInterval(async () => {
          try{
            const pollResponse = await fetch('/api/schema-registry/status');
            if(!pollResponse.ok) throw new Error('Polling hiba');
            const pollStatus = await pollResponse.json();
            renderStatus(pollStatus);
            if(!pollStatus.loading){
              clearInterval(pollHandle);
              pollHandle = null;
            }
          }catch(error){
            console.error('Sématár lekérdezési hiba', error);
            clearInterval(pollHandle);
            pollHandle = null;
          }
        }, 600);
      }
      if(!status.loading && pollHandle){
        clearInterval(pollHandle);
        pollHandle = null;
      }
    }catch(error){
      console.error('Sématár állapot hiba', error);
      renderStatus(null);
    }
  }

    /**
   * A <code>reload</code> függvény a séma-registry folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function reload(){
    try{
      if(!reloadSchemaRegistryButton || !schemaRegistryText || !schemaRegistryFill) return;
      reloadSchemaRegistryButton.disabled = true;
      schemaRegistryText.textContent = 'Sématár frissítés indítása…';
      schemaRegistryFill.style.width = '0%';
      const response = await fetch('/api/schema-registry/reload', { method:'POST' });
      if(!response.ok) throw new Error('A sématár frissítése sikertelen');
      renderStatus(await response.json());
      await fetchStatus();
    }catch(error){
      console.error('Sématár frissítési hiba', error);
      if(schemaRegistryText) schemaRegistryText.textContent = 'A sématár frissítése sikertelen';
      if(reloadSchemaRegistryButton) reloadSchemaRegistryButton.disabled = false;
      showMessage('A schema registry újraolvasása nem sikerült.', 'error');
    }
  }

    /**
   * Kezeli vagy beköti a init esemény- és inicializációs folyamatát.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function init(){
    reloadSchemaRegistryButton?.addEventListener('click', reload);
  }

  return { init, fetchStatus, reload, renderStatus };
}
