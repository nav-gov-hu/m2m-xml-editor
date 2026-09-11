/**
 * @module xml/xml-new-file-ui
 *
 * Az Űrlapállományok oldalról indítható új XML létrehozási folyamat felületi vezérlése.
 */

let initialized = false;
let generationOptions = null;
let partnerSuggestTimer = null;
let lastSuggestedFileName = '';

/**
 * Inicializálja az Új XML gombot, a párbeszédablakot és a hozzá tartozó backend-hívásokat.
 */
export function initNewXmlUi(){
  if(initialized) return;
  initialized = true;

  const openButton = document.getElementById('createNewXmlButton');
  const modal = document.getElementById('newXmlModal');
  const form = document.getElementById('newXmlForm');
  if(!openButton || !modal || !form) return;

  const formType = document.getElementById('newXmlFormType');
  const formVersion = document.getElementById('newXmlFormVersion');
  const fileName = document.getElementById('newXmlFileName');
  const partnerSearch = document.getElementById('newXmlPartnerSearch');
  const partnerId = document.getElementById('newXmlPartnerId');
  const partnerSuggestions = document.getElementById('newXmlPartnerSuggestions');
  const partnerTaxNumber = document.getElementById('newXmlPartnerTaxNumber');
  const partnerName = document.getElementById('newXmlPartnerName');
  const partnerSaveButton = document.getElementById('newXmlPartnerSaveButton');
  const note = document.getElementById('newXmlNote');
  const submitButton = document.getElementById('newXmlSubmitButton');

  const showMessage = (message, type = 'info') => {
    const text = String(message || '').trim();
    if(!text) return;
    if(window.navShowToast){
      window.navShowToast(text, type);
      return;
    }
    const target = document.getElementById('messages');
    if(target){
      const item = document.createElement('div');
      item.className = `message message-${type}`;
      item.textContent = text;
      target.replaceChildren(item);
    }
  };

  const readError = async response => {
    const data = await response.json().catch(() => null);
    if(data?.message) return data.message;
    if(data?.error) return data.error;
    const text = await response.text().catch(() => '');
    return text || `A kérés sikertelen (${response.status}).`;
  };

  const normalizeRoles = roles => new Set((Array.isArray(roles) ? roles : [])
    .map(role => String(role || '').toUpperCase()));

  const refreshPermission = async () => {
    try{
      const response = await fetch('/api/security/current-user', { credentials:'same-origin', cache:'no-store' });
      if(!response.ok) throw new Error('current-user unavailable');
      const user = await response.json();
      const roles = normalizeRoles(user?.roles);
      const allowed = roles.has('ADMIN') || roles.has('ROLE_ADMIN') || roles.has('OPERATOR') || roles.has('ROLE_OPERATOR');
      openButton.disabled = !allowed;
      openButton.setAttribute('aria-disabled', String(!allowed));
      openButton.title = allowed
        ? 'Új XML létrehozása'
        : 'Új XML létrehozása csak ADMIN vagy OPERATOR jogosultsággal érhető el';
    }catch(_ignored){
      openButton.disabled = true;
      openButton.setAttribute('aria-disabled', 'true');
    }
  };

  const formatTaxNumberInput = value => {
    const digits = String(value || '').replace(/\D/g, '').slice(0, 11);
    return [digits.slice(0, 8), digits.slice(8, 9), digits.slice(9, 11)].filter(Boolean).join('-');
  };

  const isValidHungarianTaxNumber = value => {
    const normalized = formatTaxNumberInput(value);
    if(!/^\d{8}-\d-\d{2}$/.test(normalized)) return false;
    const core = normalized.slice(0, 8);
    const weights = [9, 7, 3, 1, 9, 7, 3];
    const sum = weights.reduce((total, weight, index) => total + Number(core[index]) * weight, 0);
    return Number(core[7]) === ((10 - (sum % 10)) % 10);
  };

  const buildSuggestedFileName = () => {
    const selectedForm = String(formType?.value || '').trim();
    const selectedVersion = String(formVersion?.value || '').trim();
    if(!selectedForm || !selectedVersion) return '';
    const now = new Date();
    const pad = value => String(value).padStart(2, '0');
    const stamp = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
    return `${selectedForm}_${selectedVersion}_${stamp}.xml`;
  };

  const updateSuggestedFileName = () => {
    if(!fileName) return;
    const suggested = buildSuggestedFileName();
    if(!suggested) return;
    const current = fileName.value.trim();
    if(!current || current === lastSuggestedFileName){
      fileName.value = suggested;
      lastSuggestedFileName = suggested;
    }
  };

  const populateVersions = () => {
    if(!formVersion || !formType) return;
    const selected = generationOptions?.find(option => option.formType === formType.value);
    formVersion.replaceChildren(new Option('Verzió kiválasztása...', ''));
    const versions = Array.isArray(selected?.versions) ? selected.versions : [];
    versions.forEach(version => formVersion.appendChild(new Option(version, version)));
    formVersion.disabled = versions.length === 0;
    if(versions.length === 1){
      formVersion.value = versions[0];
    }
    updateSuggestedFileName();
  };

  const loadGenerationOptions = async () => {
    if(generationOptions) return generationOptions;
    const response = await fetch('/api/xml-files/generation-options', { credentials:'same-origin', cache:'no-store' });
    if(!response.ok) throw new Error(await readError(response));
    const data = await response.json();
    generationOptions = Array.isArray(data) ? data : [];
    if(!generationOptions.length){
      throw new Error('A konfigurált XSD repositoryban nem található generálható űrlapverzió.');
    }
    if(formType){
      formType.replaceChildren(new Option('Űrlap kiválasztása...', ''));
      generationOptions.forEach(option => formType.appendChild(new Option(option.formType, option.formType)));
    }
    return generationOptions;
  };

  const closeModal = () => {
    modal.hidden = true;
    if(partnerSuggestions){
      partnerSuggestions.hidden = true;
      partnerSuggestions.replaceChildren();
    }
  };

  const openModal = async () => {
    if(openButton.disabled) return;
    try{
      await loadGenerationOptions();
      form.reset();
      if(partnerId) partnerId.value = '';
      if(formVersion){
        formVersion.replaceChildren(new Option('Verzió kiválasztása...', ''));
        formVersion.disabled = true;
      }
      lastSuggestedFileName = '';
      modal.hidden = false;
      formType?.focus();
    }catch(error){
      showMessage(error.message || 'Az Új XML párbeszédablak nem nyitható meg.', 'error');
    }
  };

  const renderPartnerSuggestions = items => {
    if(!partnerSuggestions) return;
    partnerSuggestions.replaceChildren();
    (Array.isArray(items) ? items : []).forEach(partner => {
      const button = document.createElement('button');
      button.type = 'button';
      button.dataset.id = String(partner.id);
      button.dataset.label = `${partner.taxNumber || ''} - ${partner.name || ''}`;
      button.textContent = button.dataset.label;
      partnerSuggestions.appendChild(button);
    });
    partnerSuggestions.hidden = partnerSuggestions.childElementCount === 0;
  };

  partnerSearch?.addEventListener('input', () => {
    if(partnerId) partnerId.value = '';
    clearTimeout(partnerSuggestTimer);
    const query = partnerSearch.value.trim();
    if(query.length < 2){
      renderPartnerSuggestions([]);
      return;
    }
    partnerSuggestTimer = setTimeout(async () => {
      try{
        const response = await fetch(`/api/partners/suggest?q=${encodeURIComponent(query)}`, { credentials:'same-origin' });
        if(!response.ok) throw new Error(await readError(response));
        renderPartnerSuggestions(await response.json());
      }catch(error){
        renderPartnerSuggestions([]);
        showMessage(error.message || 'A partnerkeresés sikertelen.', 'error');
      }
    }, 220);
  });

  partnerSuggestions?.addEventListener('click', event => {
    const button = event.target.closest('button[data-id]');
    if(!button) return;
    if(partnerId) partnerId.value = button.dataset.id || '';
    if(partnerSearch) partnerSearch.value = button.dataset.label || '';
    partnerSuggestions.hidden = true;
  });

  partnerTaxNumber?.addEventListener('input', () => {
    partnerTaxNumber.value = formatTaxNumberInput(partnerTaxNumber.value);
  });

  partnerSaveButton?.addEventListener('click', async () => {
    const taxNumber = formatTaxNumberInput(partnerTaxNumber?.value);
    const name = String(partnerName?.value || '').trim();
    if(!/^\d{8}-\d-\d{2}$/.test(taxNumber)){
      showMessage('Az adószám formátuma 8-1-2 legyen, például 12345676-1-42.', 'error');
      partnerTaxNumber?.focus();
      return;
    }
    if(!isValidHungarianTaxNumber(taxNumber)){
      showMessage('Az adószám első nyolc számjegyének CDV ellenőrzése sikertelen.', 'error');
      partnerTaxNumber?.focus();
      return;
    }
    if(!name){
      showMessage('A partner neve kötelező.', 'error');
      partnerName?.focus();
      return;
    }
    const confirmed = window.navConfirm
      ? await window.navConfirm({
          title:'Partner rögzítése',
          message:`Biztosan rögzíteni szeretné ezt a partnert?\n${taxNumber} - ${name}`,
          confirmText:'Rögzítés',
          cancelText:'Mégsem'
        })
      : true;
    if(!confirmed) return;

    try{
      partnerSaveButton.disabled = true;
      const response = await fetch('/api/partners', {
        method:'POST',
        headers:{ 'Content-Type':'application/json' },
        credentials:'same-origin',
        body:JSON.stringify({ taxNumber, name, active:true })
      });
      if(!response.ok) throw new Error(await readError(response));
      const saved = await response.json();
      if(partnerId) partnerId.value = saved.id;
      if(partnerSearch) partnerSearch.value = `${saved.taxNumber} - ${saved.name}`;
      if(partnerTaxNumber) partnerTaxNumber.value = '';
      if(partnerName) partnerName.value = '';
      showMessage('A partner rögzítése sikeres.', 'success');
    }catch(error){
      showMessage(error.message || 'A partner rögzítése sikertelen.', 'error');
    }finally{
      partnerSaveButton.disabled = false;
    }
  });

  formType?.addEventListener('change', populateVersions);
  formVersion?.addEventListener('change', updateSuggestedFileName);
  fileName?.addEventListener('input', () => {
    if(fileName.value.trim() !== lastSuggestedFileName) lastSuggestedFileName = '';
  });

  form.addEventListener('submit', async event => {
    event.preventDefault();
    const selectedForm = String(formType?.value || '').trim();
    const selectedVersion = String(formVersion?.value || '').trim();
    const selectedFileName = String(fileName?.value || '').trim();
    const selectedPartnerId = String(partnerId?.value || '').trim();
    if(!selectedForm){ showMessage('Válasszon űrlapot.', 'error'); formType?.focus(); return; }
    if(!selectedVersion){ showMessage('Válasszon űrlapverziót.', 'error'); formVersion?.focus(); return; }
    if(!selectedFileName){ showMessage('Adja meg az XML fájlnevét.', 'error'); fileName?.focus(); return; }
    if(!selectedPartnerId){ showMessage('Válasszon ki egy partnert.', 'error'); partnerSearch?.focus(); return; }

    try{
      submitButton.disabled = true;
      submitButton.textContent = 'Létrehozás folyamatban...';
      const response = await fetch('/api/xml-files/generate', {
        method:'POST',
        headers:{ 'Content-Type':'application/json' },
        credentials:'same-origin',
        body:JSON.stringify({
          formType:selectedForm,
          formVersion:selectedVersion,
          fileName:selectedFileName,
          partnerId:Number(selectedPartnerId),
          userNote:String(note?.value || '').trim()
        })
      });
      if(!response.ok) throw new Error(await readError(response));
      const created = await response.json();
      showMessage(`Új XML létrehozva: ${created.fileName || selectedFileName}`, 'success');
      try{ sessionStorage.removeItem('navXsdToolActiveXmlFile'); }catch(_ignored){}
      window.location.assign(`/form.html?xmlFileId=${encodeURIComponent(created.id)}&newXml=true`);
    }catch(error){
      showMessage(error.message || 'Az új XML létrehozása sikertelen.', 'error');
      submitButton.disabled = false;
      submitButton.textContent = 'Létrehozás';
    }
  });

  openButton.addEventListener('click', openModal);
  modal.querySelectorAll('[data-close-new-xml="true"]').forEach(element => {
    element.addEventListener('click', closeModal);
  });
  document.addEventListener('keydown', event => {
    if(event.key === 'Escape' && !modal.hidden) closeModal();
  });

  refreshPermission();
}
