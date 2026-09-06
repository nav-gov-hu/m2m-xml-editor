/**
 * A <code>apiGetJson</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A hálózati hívás eredményét egységes kliensoldali hibakezeléssel adja tovább, és a felhasználói felületet csak a válasz feldolgozása után módosítja.</p>
 * @param {*} url a függvény url bemeneti értéke
 * @param {*} options a művelet opcionális beállításai
 * @returns {Promise<*>} a feldolgozás eredménye
 */
/**
 * @module core/api-client
 *
 * A közös frontend infrastruktúra- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

export async function apiGetJson(url, options = {}){
  const response = await fetch(url, {
    credentials: 'same-origin',
    ...options,
    headers: {
      ...buildDefaultHeaders(),
      ...(options.headers || {})
    }
  });
  return handleJsonResponse(response);
}

/**
 * A <code>apiPostJson</code> függvény a közös frontend infrastruktúra- folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A hálózati hívás eredményét egységes kliensoldali hibakezeléssel adja tovább, és a felhasználói felületet csak a válasz feldolgozása után módosítja.</p>
 * @param {*} url a függvény url bemeneti értéke
 * @param {*} body a függvény body bemeneti értéke
 * @param {*} options a művelet opcionális beállításai
 * @returns {Promise<*>} a feldolgozás eredménye
 */
export async function apiPostJson(url, body, options = {}){
  const response = await fetch(url, {
    method: 'POST',
    credentials: 'same-origin',
    ...options,
    headers: {
      ...buildDefaultHeaders(),
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    body: JSON.stringify(body ?? {})
  });
  return handleJsonResponse(response);
}

/**
 * Feldolgozza a build default headers bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @returns {*} a feldolgozás eredménye
 */
export function buildDefaultHeaders(){
  const headers = {};
  const apiKey = window.localStorage?.getItem('navApiKey');
  if(apiKey){
    headers['X-API-Key'] = apiKey;
  }
  return headers;
}

/**
 * Kezeli vagy beköti a handle json response esemény- és inicializációs folyamatát.
 *
 * <p>A hálózati hívás eredményét egységes kliensoldali hibakezeléssel adja tovább, és a felhasználói felületet csak a válasz feldolgozása után módosítja.</p>
 * @param {*} response a backend-hívás feldolgozandó válasza
 * @returns {Promise<*>} a feldolgozás eredménye
 */
async function handleJsonResponse(response){
  const text = await response.text();
  let body = null;
  if(text){
    try{
      body = JSON.parse(text);
    }catch(_error){
      body = text;
    }
  }
  if(!response.ok){
    const error = new Error(`A kiszolgáló a kérést nem tudta teljesíteni (HTTP ${response.status}).`);
    error.status = response.status;
    throw error;
  }
  return body;
}
