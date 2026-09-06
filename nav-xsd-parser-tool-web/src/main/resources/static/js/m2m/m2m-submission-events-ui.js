/**
 * @module m2m/m2m-submission-events-ui
 *
 * A M2M beküldési és csatolmánykezelési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja. A kliensoldali engedélyezés csak felhasználói visszajelzés; a tényleges M2M jogosultsági kontroll szerveroldali.
 */

import { formatM2mCommunicationEvents } from './m2m-http-trace-ui.js';

/**
 * Megjeleníti vagy újrarendereli a render submission events állapotát a felhasználói felületen.
 *
 * <p>A művelet az M2M életciklus kliensoldali reprezentációját kezeli; a SUBMITTED_OK végállapotból eredő tiltások szerveroldali kontrollját nem helyettesíti.</p>
 * @param {*} target a függvény target bemeneti értéke
 * @param {*} events a feldolgozandó böngészőesemény
 */
export function renderSubmissionEvents(target, events){
  if(!target) return;
  target.textContent = formatM2mCommunicationEvents(events, {
    filter: true,
    requestHeadersFallback: false,
    emptyText: 'Nincs kommunikációs request/response esemény.'
  });
}
