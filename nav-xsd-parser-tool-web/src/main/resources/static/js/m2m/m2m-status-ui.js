/**
 * Feldolgozza a normalize m2m status bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
/**
 * @module m2m/m2m-status-ui
 *
 * A M2M beküldési és csatolmánykezelési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A kliensoldali engedélyezés csak felhasználói visszajelzés; a tényleges M2M jogosultsági kontroll szerveroldali.
 */

export function normalizeM2mStatus(value){
  return String(value || '').trim().toUpperCase();
}

/**
 * Ellenőrzi a is marked for submission feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} rowOrStatus a kapcsolódó folyamat aktuális állapota
 * @returns {*} a feldolgozás eredménye
 */
export function isMarkedForSubmission(rowOrStatus){
  const status = typeof rowOrStatus === 'object' ? rowOrStatus?.internalStatus : rowOrStatus;
  return normalizeM2mStatus(status) === 'MARKED_FOR_SUBMISSION';
}

/**
 * Ellenőrzi a is m2m terminal feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} rowOrStatus a kapcsolódó folyamat aktuális állapota
 * @returns {*} a feldolgozás eredménye
 */
export function isM2mTerminal(rowOrStatus){
  if(typeof rowOrStatus === 'object' && rowOrStatus?.m2mTerminal === true) return true;
  const status = typeof rowOrStatus === 'object' ? rowOrStatus?.internalStatus : rowOrStatus;
  return ['SUBMITTED_OK', 'SUBMITTED_WITH_ERROR'].includes(normalizeM2mStatus(status));
}

/**
 * Ellenőrzi a is m2m successful terminal feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} rowOrStatus a kapcsolódó folyamat aktuális állapota
 * @returns {*} a feldolgozás eredménye
 */
export function isM2mSuccessfulTerminal(rowOrStatus){
  const status = typeof rowOrStatus === 'object' ? rowOrStatus?.internalStatus : rowOrStatus;
  return normalizeM2mStatus(status) === 'SUBMITTED_OK';
}

/**
 * Ellenőrzi a is m2m in progress feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} rowOrStatus a kapcsolódó folyamat aktuális állapota
 * @returns {*} a feldolgozás eredménye
 */
export function isM2mInProgress(rowOrStatus){
  const status = typeof rowOrStatus === 'object' ? rowOrStatus?.internalStatus : rowOrStatus;
  return ['SUBMITTING', 'SUBMISSION_IN_PROGRESS', 'SUBMIT_PENDING'].includes(normalizeM2mStatus(status));
}

/**
 * Szinkronizálja vagy frissíti a apply submission action button state által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} row a függvény row bemeneti értéke
 * @param {*} elements a feldolgozásban részt vevő DOM-elem vagy DOM-gyökér
 */
export function applySubmissionActionButtonState(row, elements){
  const hasSelection = !!row;
  const marked = isMarkedForSubmission(row);
  const terminal = isM2mTerminal(row);
  const inProgress = isM2mInProgress(row);
  const all = [
    elements.mark,
    elements.withdraw,
    elements.uploadAttachments,
    elements.createBizonylat
  ].filter(Boolean);
  all.forEach(button => { button.disabled = !hasSelection; });
  if(elements.mark) elements.mark.disabled = !hasSelection || marked || terminal || inProgress;
  if(elements.withdraw) elements.withdraw.disabled = !hasSelection || !marked;
  if(elements.uploadAttachments) elements.uploadAttachments.disabled = !hasSelection || terminal || inProgress;
  if(elements.createBizonylat) elements.createBizonylat.disabled = !hasSelection || !marked || terminal || inProgress;
}
