/**
 * @module m2m/m2m-http-trace-ui
 *
 * A M2M kommunikációs és eseménynapló megjelenítését kezeli.
 */

function escapeHtml(value){
  return String(value ?? '').replace(/[&<>\"]/g, char => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;'
  }[char]));
}

/** Kiszűri a request/response kommunikációt tartalmazó M2M eseményeket. */
export function filterCommunicationEvents(events){
  return (Array.isArray(events) ? events : []).filter(event =>
    event?.eventType === 'NAV_HTTP_TRACE' || event?.requestPayload || event?.responsePayload || event?.requestHeaders
  );
}

/** Szöveges kommunikációs naplót készít a kompatibilitást igénylő felületek számára. */
export function formatM2mCommunicationEvents(events, options = {}){
  const rows = options.filter === false ? (Array.isArray(events) ? events : []) : filterCommunicationEvents(events);
  const emptyText = options.emptyText || 'Nincs kommunikációs request/response esemény.';
  return rows.map((event, index) => {
    const title = `#${index + 1} ${event.createdAt || ''} ${event.navOperation || event.eventType || ''} ${event.responseCode || ''}`.trim();
    const requestValue = event.requestPayload || (options.requestHeadersFallback !== false ? event.requestHeaders : '') || '-';
    const responseValue = event.responsePayload || '-';
    return [title, `REQUEST:\n${requestValue}`, `RESPONSE:\n${responseValue}`].join('\n\n');
  }).join('\n\n==============================\n\n') || emptyText;
}

const RESPONSE_PREVIEW_LENGTH = 200;

function formatTimestamp(value){
  if(!value) return '–';
  const date = new Date(value);
  if(Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString('hu-HU', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
  });
}

function responsePreview(value){
  const text = String(value || '–');
  if(text.length <= RESPONSE_PREVIEW_LENGTH) return text;
  return `${text.slice(0, RESPONSE_PREVIEW_LENGTH)}…`;
}

function statusClass(value){
  const normalized = String(value || '').toUpperCase();
  if(normalized === 'OK' || normalized === 'SUCCESS' || /^2\d\d$/.test(normalized)) return 'success';
  if(normalized.includes('WARN')) return 'warning';
  if(normalized.includes('ERROR') || normalized.includes('FAIL') || /^[45]\d\d$/.test(normalized)) return 'error';
  return 'neutral';
}

function compareValues(left, right, key, direction){
  let a = left?.[key] ?? '';
  let b = right?.[key] ?? '';
  if(key === 'createdAt'){
    a = Date.parse(a) || 0;
    b = Date.parse(b) || 0;
  }else{
    a = String(a).toLocaleLowerCase('hu-HU');
    b = String(b).toLocaleLowerCase('hu-HU');
  }
  const result = a < b ? -1 : a > b ? 1 : 0;
  return direction === 'asc' ? result : -result;
}

function renderRows(events){
  if(!events.length){
    return '<tr><td colspan="7" class="m2m-log-empty">Nincs M2M esemény.</td></tr>';
  }
  return events.map((event, index) => {
    const response = String(event.responsePayload || '–');
    const expandable = response.length > RESPONSE_PREVIEW_LENGTH;
    const rowId = `m2mLogDetails${index}`;
    return `
      <tr class="m2m-log-event-row">
        <td class="m2m-log-date">${escapeHtml(formatTimestamp(event.createdAt))}</td>
        <td>${escapeHtml(event.eventType || '–')}</td>
        <td>${escapeHtml(event.navOperation || '–')}</td>
        <td><span class="m2m-log-status ${statusClass(event.responseCode)}">${escapeHtml(event.responseCode || '–')}</span></td>
        <td class="m2m-log-message-id">${escapeHtml(event.requestMessageId || '–')}</td>
        <td class="m2m-log-response-preview"><span>${escapeHtml(responsePreview(response))}</span></td>
        <td class="m2m-log-actions">${expandable ? `<button type="button" class="secondary mini-button m2m-log-expand" data-details-id="${rowId}" aria-expanded="false">Kibontás</button>` : '–'}</td>
      </tr>
      ${expandable ? `<tr id="${rowId}" class="m2m-log-details-row" hidden><td colspan="7"><div class="m2m-log-details"><div class="m2m-log-details-title">Teljes response payload</div><pre>${escapeHtml(response)}</pre></div></td></tr>` : ''}`;
  }).join('');
}

/** Megjeleníti az M2M eseménynaplót rendezhető, részletezhető táblázatban. */
export function showM2mLogsModal({ events, submissionId }){
  const host = document.getElementById('appDialogHost') || (() => {
    const element = document.createElement('div');
    element.id = 'appDialogHost';
    document.body.appendChild(element);
    return element;
  })();
  const modal = document.createElement('div');
  modal.className = 'app-dialog-modal m2m-log-modal';
  const state = {
    events: Array.isArray(events) ? [...events] : [],
    sortKey: 'createdAt',
    sortDirection: 'desc'
  };

  modal.innerHTML = `
    <div class="app-dialog-backdrop" data-close="true"></div>
    <section class="app-dialog-card" role="dialog" aria-modal="true" aria-labelledby="m2mLogsTitle">
      <button type="button" class="app-dialog-close" data-close="true" aria-label="Bezárás">&times;</button>
      <p class="eyebrow">NAV M2M</p>
      <h2 id="m2mLogsTitle">M2M eseménynapló</h2>
      <p class="xml-diff-summary">Csomagazonosító: ${escapeHtml(submissionId || '')}</p>
      <div class="m2m-log-table-wrap">
        <table class="data-table m2m-log-table">
          <thead><tr>
            <th><button type="button" class="m2m-log-sort active" data-sort="createdAt">Dátum</button></th>
            <th><button type="button" class="m2m-log-sort" data-sort="eventType">Esemény</button></th>
            <th><button type="button" class="m2m-log-sort" data-sort="navOperation">M2M művelet</button></th>
            <th><button type="button" class="m2m-log-sort" data-sort="responseCode">Állapot</button></th>
            <th><button type="button" class="m2m-log-sort" data-sort="requestMessageId">Üzenetazonosító</button></th>
            <th><button type="button" class="m2m-log-sort" data-sort="responsePayload">Response payload</button></th>
            <th>Részletek</th>
          </tr></thead>
          <tbody class="m2m-log-table-body"></tbody>
        </table>
      </div>
      <div class="app-dialog-actions"><button type="button" class="primary" data-close="true">Bezárás</button></div>
    </section>`;

  const tbody = modal.querySelector('.m2m-log-table-body');
  const rerender = () => {
    state.events.sort((a, b) => compareValues(a, b, state.sortKey, state.sortDirection));
    tbody.innerHTML = renderRows(state.events);
    modal.querySelectorAll('.m2m-log-sort').forEach(button => {
      const active = button.dataset.sort === state.sortKey;
      button.classList.toggle('active', active);
      const existingArrow = button.querySelector('[data-sort-arrow]');
      if(existingArrow) existingArrow.remove();
      if(active){
        const arrow = document.createElement('span');
        arrow.dataset.sortArrow = 'true';
        arrow.setAttribute('aria-hidden', 'true');
        arrow.textContent = state.sortDirection === 'asc' ? '▲' : '▼';
        button.append(' ', arrow);
      }
    });
  };

  modal.addEventListener('click', event => {
    if(event.target?.dataset?.close){
      modal.remove();
      return;
    }
    const sortButton = event.target.closest?.('.m2m-log-sort');
    if(sortButton){
      const key = sortButton.dataset.sort;
      if(state.sortKey === key){
        state.sortDirection = state.sortDirection === 'asc' ? 'desc' : 'asc';
      }else{
        state.sortKey = key;
        state.sortDirection = key === 'createdAt' ? 'desc' : 'asc';
      }
      rerender();
      return;
    }
    const expandButton = event.target.closest?.('.m2m-log-expand');
    if(expandButton){
      const detailsRow = modal.querySelector(`#${CSS.escape(expandButton.dataset.detailsId)}`);
      if(!detailsRow) return;
      const expanded = expandButton.getAttribute('aria-expanded') === 'true';
      detailsRow.hidden = expanded;
      expandButton.setAttribute('aria-expanded', String(!expanded));
      expandButton.textContent = expanded ? 'Kibontás' : 'Összecsukás';
    }
  });

  host.appendChild(modal);
  rerender();
  return modal;
}
