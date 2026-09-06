/**
 * Feldolgozza a normalize validator status bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @returns {*} a feldolgozás eredménye
 */
/**
 * @module validation/validation-status-utils
 *
 * A XSD/XPath validációs működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A validációs állapotot és a hibák mezőhöz navigálását a szerveroldali eredményekkel összhangban kezeli.
 */

export function normalizeValidatorStatus(status){
  return String(status?.validatorStatus || '').toUpperCase();
}

/**
 * Feldolgozza a normalize result status bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @returns {*} a feldolgozás eredménye
 */
export function normalizeResultStatus(status){
  return String(status?.resultStatus || '').toUpperCase();
}

const MISSING_XPATH_RULE_MESSAGE = 'Az ellenőrzés sikertelen: Az Űrlap állományhoz nincs XPath állomány regisztrálva a rendszerbe.';

/**
 * A <code>firstLineText</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
export function firstLineText(value){
  const firstLine = String(value || '').split(/\r?\n/)[0];
  // Régebbi / már eltárolt aszinkron hibákban a Java exception osztálynév is
  // szerepelhetett. A felhasználói felületen ebből csak az üzleti üzenet kell.
  if(firstLine.includes('MissingXPathRuleException') || firstLine.includes(MISSING_XPATH_RULE_MESSAGE)){
    return MISSING_XPATH_RULE_MESSAGE;
  }
  return firstLine;
}

/**
 * Ellenőrzi a is terminal validation failure feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @param {*} errors a függvény errors bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function isTerminalValidationFailure(status, errors = []){
  const validatorStatus = normalizeValidatorStatus(status);
  const resultStatus = normalizeResultStatus(status);
  const count = Number(status?.errorCount ?? (Array.isArray(errors) ? errors.length : 0));
  return validatorStatus === 'ABORTED' || validatorStatus === 'FAILED' || resultStatus === 'ERROR' || count > 0;
}

/**
 * Feldolgozza a build xpath toast message bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} status a kapcsolódó folyamat aktuális állapota
 * @param {*} errors a függvény errors bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function buildXpathToastMessage(status, errors = []){
  const validatorStatus = normalizeValidatorStatus(status);
  const count = Number(status?.errorCount ?? (Array.isArray(errors) ? errors.length : 0));
  if(validatorStatus === 'ABORTED' || validatorStatus === 'FAILED'){
    const technicalMessage = firstLineText(status?.technicalErrorMessage);
    return technicalMessage || `XPath ellenőrzés megszakadt: ${validatorStatus}`;
  }
  return `XPath ellenőrzés lefutott. Hibák száma: ${count}`;
}

/**
 * Ellenőrzi a is xsd validation error issue feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} issue a függvény issue bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function isXsdValidationErrorIssue(issue){
  const severity = String(issue?.severity || '').trim().toUpperCase();
  return severity !== 'INFO';
}

/**
 * A <code>countXsdValidationErrors</code> függvény a XSD/XPath validációs folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A kliensoldali eredmény a backend validációs válaszát jeleníti meg és navigálhatóvá teszi; nem írja felül a szerveroldali validáció döntését.</p>
 * @param {*} issues a függvény issues bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
export function countXsdValidationErrors(issues){
  if(!Array.isArray(issues)) return 0;
  return issues.filter(isXsdValidationErrorIssue).length;
}
