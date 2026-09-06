/**
 * @module form/form-renderer-runtime
 *
 * A űrlap-megjelenítési és mezőkötési működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

/**
 * Classic and UIModel form rendering, controls, masks, tables and field metadata.
 * Shared runtime state is initialized by runtime-context.js.
 */

function renderForm(formDefinition, formData, schemaBundle) {
    restoreMultiformRuntimeToolbarControls();
    formLazyRenderer?.reset();
    rebuildFormDataValueReferenceIndex?.(formData);
    const result = currentFormRenderer === 'uimodel'
        ? renderUiModelForm(formDefinition, formData, schemaBundle)
        : renderClassicForm(formDefinition, formData, schemaBundle);
    enhanceMultiformRuntimeView();
    ensureStandardFormToolbarControlsVisible();
    applyCurrentXsdValidationHighlights();
    if(NAV_FIELD_BINDING_DEBUG_ENABLED) scheduleFieldBindingDebug('after renderForm');
    window.setTimeout(() => refreshFieldSearch({ showList:false }), 0);
    return result;
}

/**
 * Ellenőrzi a is m2m attachment technical xml path feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} path a mezőhöz vagy XML-csomóponthoz tartozó útvonal
 * @returns {*} a feldolgozás eredménye
 */
function isM2mAttachmentTechnicalXmlPath(path){
    return /(^|\/)attachment_1(?:\[\d+\])?\/(?:fileid|filename|filesize)(?:\[\d+\])?$/i
        .test(String(path || '').trim());
}

/**
 * Ellenőrzi a is m2m attachment technical field feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @param {*} valueObj a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function isM2mAttachmentTechnicalField(field, valueObj){
    return isM2mAttachmentTechnicalXmlPath(valueObj?.xmlPath || field?.xmlPath || '');
}

/**
 * Ellenőrzi a should render non attachment ui model field feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @param {*} valueObj a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function shouldRenderNonAttachmentUiModelField(field, valueObj){
    return !isM2mAttachmentTechnicalField(field, valueObj) && shouldRenderUiModelField(field, valueObj);
}

/**
 * A <code>classicSectionHasRenderableFields</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} section a függvény section bemeneti értéke
 * @param {*} valuesByFieldId a célobjektum technikai azonosítója
 * @param {*} rowInstancesByRowId a célobjektum technikai azonosítója
 * @returns {*} a feldolgozás eredménye
 */
function classicSectionHasRenderableFields(section, valuesByFieldId, rowInstancesByRowId){
    return (section?.rows || []).some(row => {
        const fields = (row?.fields || []).filter(field => field?.visible !== false);
        if(!fields.length) return false;
        if(row?.repeatable){
            const instances = rowInstancesByRowId?.[row.id] || [];
            if(instances.length){
                return instances.some(instance => fields.some(field =>
                    !isM2mAttachmentTechnicalField(field, instance?.valuesByFieldId?.[field.id])
                ));
            }
        }
        return fields.some(field => !isM2mAttachmentTechnicalField(field, valuesByFieldId?.[field.id]));
    });
}

/**
 * Ellenőrzi a is lazy form rendering allowed feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function isLazyFormRenderingAllowed(){
    return !!formLazyRenderer && !detectRuntimeMultiformParts(currentXmlDocument).length;
}

/**
 * A <code>sectionMatchesLazySearch</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} section a függvény section bemeneti értéke
 * @param {*} valuesByFieldId a célobjektum technikai azonosítója
 * @param {*} rowInstancesByRowId a célobjektum technikai azonosítója
 * @param {*} terms a függvény terms bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function sectionMatchesLazySearch(section, valuesByFieldId, rowInstancesByRowId, terms){
    const normalizedTerms = (terms || []).map(term => String(term || '').toLocaleLowerCase('hu-HU')).filter(Boolean);
    if(!normalizedTerms.length) return false;
    const sectionText = [section?.id, section?.title].filter(Boolean).join(' ');
    for(const row of (section?.rows || [])){
        const rowText = [sectionText, row?.id, row?.title].filter(Boolean).join(' ');
        for(const field of (row?.fields || [])){
            const metadata = [rowText, field?.id, field?.xmlName, field?.xmlPath, field?.uiLabel, field?.xsdLabel, field?.label]
                .filter(value => value !== null && value !== undefined)
                .join(' ');
            const directValue = valuesByFieldId?.[field?.id]?.value;
            const directText = `${metadata} ${directValue ?? ''}`.toLocaleLowerCase('hu-HU');
            if(normalizedTerms.every(term => directText.includes(term))) return true;
            if(row?.repeatable){
                for(const instance of (rowInstancesByRowId?.[row.id] || [])){
                    const value = instance?.valuesByFieldId?.[field?.id]?.value;
                    const instanceText = `${metadata} ${value ?? ''}`.toLocaleLowerCase('hu-HU');
                    if(normalizedTerms.every(term => instanceText.includes(term))) return true;
                }
            }
        }
    }
    return false;
}

/**
 * Megjeleníti vagy újrarendereli a render classic section content állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} target a függvény target bemeneti értéke
 * @param {*} section a függvény section bemeneti értéke
 * @param {*} valuesByFieldId a célobjektum technikai azonosítója
 * @param {*} rowInstancesByRowId a célobjektum technikai azonosítója
 */
function renderClassicSectionContent(target, section, valuesByFieldId, rowInstancesByRowId){
    let sectionHasFields = false;
    for (const row of (section.rows || [])) {
        if (row.repeatable) {
            const instances = rowInstancesByRowId[row.id] || [];
            instances.forEach((instance, idx) => {
                const group = createFieldGroupCard(row.title || row.id || 'Lánc elem', `#${idx + 1}`, true);
                const fieldsContainer = group.querySelector('.fieldgroup-fields');
                let groupHasFields = false;
                (row.fields || [])
                    .filter(field => field.visible !== false)
                    .forEach(field => {
                        const valueObj = instance.valuesByFieldId?.[field.id];
                        if(isM2mAttachmentTechnicalField(field, valueObj)) return;
                        const fieldElement = renderFieldElement(field, valueObj);
                        if (fieldElement) {
                            fieldsContainer.appendChild(fieldElement);
                            groupHasFields = true;
                        }
                    });
                if (groupHasFields) {
                    target.appendChild(group);
                    sectionHasFields = true;
                }
            });
            continue;
        }

        const group = createFieldGroupCard(row.title || row.id || 'FieldGroup', '', false);
        const fieldsContainer = group.querySelector('.fieldgroup-fields');
        let groupHasFields = false;
        (row.fields || [])
            .filter(field => field.visible !== false)
            .forEach(field => {
                const valueObj = valuesByFieldId[field.id];
                if(isM2mAttachmentTechnicalField(field, valueObj)) return;
                const fieldElement = renderFieldElement(field, valueObj);
                if (fieldElement) {
                    fieldsContainer.appendChild(fieldElement);
                    groupHasFields = true;
                }
            });
        if (groupHasFields) {
            target.appendChild(group);
            sectionHasFields = true;
        }
    }
    if (!sectionHasFields) {
        const emptyMessage = document.createElement('p');
        emptyMessage.textContent = 'Nincs megjeleníthető mező.';
        target.appendChild(emptyMessage);
    }
}

/**
 * Megjeleníti vagy újrarendereli a render classic form állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} formDefinition a függvény formDefinition bemeneti értéke
 * @param {*} formData a függvény formData bemeneti értéke
 * @param {*} schemaBundle a függvény schemaBundle bemeneti értéke
 */
function renderClassicForm(formDefinition, formData, schemaBundle) {
    safeReplaceElementChildren(formContainer);
    updateFormHeaderTitle(formDefinition, schemaBundle);
    applyPaneState();

    const valuesByFieldId = formData?.valuesByFieldId || {};
    const rowInstancesByRowId = formData?.rowInstancesByRowId || {};
    const lazy = isLazyFormRenderingAllowed();
    let hasRenderableContent = false;

    for (const tab of (formDefinition?.tabs || [])) {
        for (const section of (tab.sections || [])) {
            if(!classicSectionHasRenderableFields(section, valuesByFieldId, rowInstancesByRowId)) continue;
            const sec = document.createElement('section');
            sec.className = 'form-section collapsible-card collapsed';

            const sectionButton = document.createElement('button');
            sectionButton.type = 'button';
            sectionButton.className = 'block-title collapse-toggle';
            sectionButton.setAttribute('aria-expanded', 'false');

            const sectionTitle = document.createElement('span');
            sectionTitle.textContent = section.title || section.id || 'Blokk';
            const sectionChevron = document.createElement('span');
            sectionChevron.className = 'collapse-chevron';
            sectionChevron.setAttribute('aria-hidden', 'true');
            sectionChevron.textContent = '▾';
            sectionButton.appendChild(sectionTitle);
            sectionButton.appendChild(sectionChevron);

            const sectionContent = document.createElement('div');
            sectionContent.className = 'form-section-content collapsible-content';
            sec.appendChild(sectionButton);
            sec.appendChild(sectionContent);
            formContainer.appendChild(sec);

                        /**
             * Megjeleníti vagy újrarendereli a render content állapotát a felhasználói felületen.
             *
             * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
             * @param {*} target a függvény target bemeneti értéke
             */
const renderContent = target => renderClassicSectionContent(target, section, valuesByFieldId, rowInstancesByRowId);
            if(lazy){
                formLazyRenderer.register(sec, sectionContent, renderContent, {
                    observe:false,
                    eager:false,
                    matchesQuery:terms => sectionMatchesLazySearch(section, valuesByFieldId, rowInstancesByRowId, terms)
                });
            }else{
                renderContent(sectionContent);
            }
            hasRenderableContent = true;
        }
    }

    if (!hasRenderableContent) {
        const emptyMessage = document.createElement('p');
        emptyMessage.textContent = 'Nincs generálható űrlap.';
        formContainer.appendChild(emptyMessage);
    }

    bindCollapseToggles(formContainer);
    updateToggleAllFormCollapseButton();
    bindFieldClicks();
    bindFormValueSync();
    highlightSelections();
}

/**
 * Előkészíti és elindítja a create field group card állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} title a függvény title bemeneti értéke
 * @param {*} suffix a függvény suffix bemeneti értéke
 * @param {*} chainRow a függvény chainRow bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function createFieldGroupCard(title, suffix, chainRow) {
    const group = document.createElement('div');
    group.className = chainRow
        ? 'fieldgroup-card chain-row-card collapsible-card collapsed'
        : 'fieldgroup-card collapsible-card collapsed';

    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'fieldgroup-title collapse-toggle';
    button.setAttribute('aria-expanded', 'false');

    const titleElement = document.createElement('span');
    titleElement.textContent = suffix ? `${title} ${suffix}` : title;

    const chevron = document.createElement('span');
    chevron.className = 'collapse-chevron';
    chevron.setAttribute('aria-hidden', 'true');
    chevron.textContent = '▾';

    button.appendChild(titleElement);
    button.appendChild(chevron);

    const fields = document.createElement('div');
    fields.className = 'fieldgroup-fields collapsible-content';

    group.appendChild(button);
    group.appendChild(fields);

    return group;
}

/**
 * Ellenőrzi a is multiform binding context feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function isMultiformBindingContext(){
  return !!currentXmlDocument && detectRuntimeMultiformParts(currentXmlDocument).length > 0;
}

/**
 * A <code>activeFormPartNameForBinding</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @returns {*} a feldolgozás eredménye
 */
function activeFormPartNameForBinding(){
  const active = String(globalThis.currentMultiformState?.activePartName || '').trim();
  if(active) return active;
  const parts = detectRuntimeMultiformParts(currentXmlDocument);
  const main = parts.find(part => part.role !== 'REPEATING') || parts[0];
  return String(main?.name || '').trim();
}

/**
 * A <code>uiModelFieldXmlName</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function uiModelFieldXmlName(field){
  const raw = String(field?.xmlName || field?.id || '').trim();
  if(!raw) return '';
  return raw.startsWith('Field_') ? raw : `Field_${raw}`;
}

/**
 * A <code>declaredFormPartFromPath</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} xmlPath a feldolgozandó XML-tartalom vagy XML DOM-objektum
 * @returns {*} a feldolgozás eredménye
 */
function declaredFormPartFromPath(xmlPath){
  const canonical = canonicalizeXmlPath(xmlPath || '');
  const segment = canonical.split('/').find(part => /^Form_/i.test(part));
  return String(segment || '').replace(/\[\d+\]$/, '');
}

/**
 * A <code>uiModelDefinitionBelongsToActivePart</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @param {*} row a függvény row bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function uiModelDefinitionBelongsToActivePart(field, row){
  if(!isMultiformBindingContext()) return true;
  const activePart = activeFormPartNameForBinding();
  if(!activePart) return true;
  const declared = declaredFormPartFromPath(field?.xmlPath || '') || declaredFormPartFromPath(row?.xmlPath || '');
  return !declared || declared === activePart;
}

/**
 * A <code>rowInstancesForActiveFormPart</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} instances a függvény instances bemeneti értéke
 * @param {*} row a függvény row bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function rowInstancesForActiveFormPart(instances, row){
  const list = Array.isArray(instances) ? instances : [];
  if(!isMultiformBindingContext()) return list;
  const activePart = activeFormPartNameForBinding();
  if(!activePart) return list;
  return list.filter(instance => Object.values(instance?.valuesByFieldId || {}).some(valueObj => {
    const path = String(valueObj?.xmlPath || '').trim();
    return path && pathBelongsToFormPart(path, activePart);
  }));
}

/**
 * A <code>deriveUiModelFieldPath</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @param {*} row a függvény row bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function deriveUiModelFieldPath(field, row){
  const activePart = activeFormPartNameForBinding();
  if(activePart && !uiModelDefinitionBelongsToActivePart(field, row)) return '';
  const fieldName = uiModelFieldXmlName(field);
  const explicit = canonicalizeXmlPath(field?.xmlPath || '');
  if(explicit && (!activePart || declaredFormPartFromPath(explicit) === activePart)) return explicit;

  const rowPath = canonicalizeXmlPath(row?.xmlPath || '');
  if(rowPath && fieldName){
    let candidate = `${rowPath}/${fieldName}[1]`;
    candidate = canonicalizeXmlPath(candidate);
    if(!activePart || candidate.includes(`/${activePart}[`)) return candidate;

    const segments = candidate.split('/').filter(Boolean);
    const structuralStart = segments.findIndex(segment => /^(Block_|FieldGroup_|Chain_)/.test(segment));
    const rootName = currentXmlDocument?.documentElement
      ? (currentXmlDocument.documentElement.localName || currentXmlDocument.documentElement.nodeName || '')
      : '';
    if(activePart && rootName && structuralStart >= 0){
      return canonicalizeXmlPath(`/${rootName}/${activePart}/${segments.slice(structuralStart).join('/')}`);
    }
  }

  if(explicit && activePart){
    const segments = explicit.split('/').filter(Boolean);
    const structuralStart = segments.findIndex(segment => /^(Block_|FieldGroup_|Chain_)/.test(segment));
    const rootName = currentXmlDocument?.documentElement
      ? (currentXmlDocument.documentElement.localName || currentXmlDocument.documentElement.nodeName || '')
      : '';
    if(rootName && structuralStart >= 0){
      return canonicalizeXmlPath(`/${rootName}/${activePart}/${segments.slice(structuralStart).join('/')}`);
    }
  }
  return explicit;
}

/**
 * Feloldja a resolve ui model value object eredményét a rendelkezésre álló DOM-, konfigurációs vagy runtime-adatokból.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @param {*} valuesByFieldId a célobjektum technikai azonosítója
 * @param {*} row a függvény row bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function resolveUiModelValueObject(field, valuesByFieldId, row){
  if(!field) return null;
  const fieldId = String(field.id || '');
  const multiform = isMultiformBindingContext();
  const fieldPath = deriveUiModelFieldPath(field, row);
  const direct = fieldId ? valuesByFieldId?.[fieldId] : null;

  if(fieldPath && currentXmlDocument){
    const node = findNodeByPath(currentXmlDocument, fieldPath);
    return {
      fieldId,
      key: fieldId,
      xmlPath: fieldPath,
      value: node && !node.children?.length ? (node.textContent || '') : '',
      present: !!node
    };
  }

  if(direct){
    const directPath = canonicalizeXmlPath(direct.xmlPath || '');
    if(!multiform || (fieldPath && directPath && pathMatches(directPath, fieldPath))) return direct;
  }

  // Multiform documents must never fall back to a globally keyed fieldId value,
  // because identical field IDs can occur in the main form and in every attachment.
  return multiform ? null : (direct || null);
}

/**
 * A <code>uiModelSectionHasRenderableFields</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} section a függvény section bemeneti értéke
 * @param {*} valuesByFieldId a célobjektum technikai azonosítója
 * @param {*} rowInstancesByRowId a célobjektum technikai azonosítója
 * @returns {*} a feldolgozás eredménye
 */
function uiModelSectionHasRenderableFields(section, valuesByFieldId, rowInstancesByRowId){
  for(const row of (section.rows || [])){
    if(row.repeatable){
      const instances = rowInstancesForActiveFormPart(rowInstancesByRowId[row.id] || [], row);
      if(instances.some(instance => (row.fields || []).some(field =>
        uiModelDefinitionBelongsToActivePart(field, row)
        && shouldRenderNonAttachmentUiModelField(field, instance.valuesByFieldId?.[field.id])
      ))) return true;
      continue;
    }
    if((row.fields || []).some(field =>
      uiModelDefinitionBelongsToActivePart(field, row)
      && shouldRenderNonAttachmentUiModelField(field, resolveUiModelValueObject(field, valuesByFieldId, row))
    )) return true;
  }
  return false;
}

/**
 * Megjeleníti vagy újrarendereli a render ui model section content állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} target a függvény target bemeneti értéke
 * @param {*} section a függvény section bemeneti értéke
 * @param {*} valuesByFieldId a célobjektum technikai azonosítója
 * @param {*} rowInstancesByRowId a célobjektum technikai azonosítója
 */
function renderUiModelSectionContent(target, section, valuesByFieldId, rowInstancesByRowId){
  for (const row of (section.rows || [])) {
    if (row.repeatable) {
      const instances = rowInstancesForActiveFormPart(rowInstancesByRowId[row.id] || [], row);
      instances.forEach((instance, idx) => {
        const group = createUiModelFieldGroup(row.title || row.id || 'Lánc elem', `#${idx + 1}`);
        const grid = group.querySelector('.uimodel-fields-grid');
        (row.fields || []).forEach(field => {
          if(!uiModelDefinitionBelongsToActivePart(field, row)) return;
          const valueObj = instance.valuesByFieldId?.[field.id];
          if(!shouldRenderNonAttachmentUiModelField(field, valueObj)) return;
          const fieldElement = renderUiModelFieldElement(field, valueObj);
          if(fieldElement) grid.appendChild(fieldElement);
        });
        if(grid.children.length) target.appendChild(group);
      });
      continue;
    }
    if (isUiModelTableBlock(row)) {
      const tableBlock = renderUiModelTableBlock(row, valuesByFieldId);
      if (tableBlock) target.appendChild(tableBlock);
      continue;
    }

    const group = createUiModelFieldGroup(row.title || row.id || 'Mezőcsoport', '');
    const grid = group.querySelector('.uimodel-fields-grid');
    (row.fields || []).forEach(field => {
      if(!uiModelDefinitionBelongsToActivePart(field, row)) return;
      const valueObj = resolveUiModelValueObject(field, valuesByFieldId, row);
      if(!shouldRenderNonAttachmentUiModelField(field, valueObj)) return;
      const fieldElement = renderUiModelFieldElement(field, valueObj);
      if(fieldElement) grid.appendChild(fieldElement);
    });
    if(grid.children.length) target.appendChild(group);
  }
}

/**
 * Megjeleníti vagy újrarendereli a render ui model form állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} formDefinition a függvény formDefinition bemeneti értéke
 * @param {*} formData a függvény formData bemeneti értéke
 * @param {*} schemaBundle a függvény schemaBundle bemeneti értéke
 */
function renderUiModelForm(formDefinition, formData, schemaBundle) {
    safeReplaceElementChildren(formContainer);
    updateFormHeaderTitle(formDefinition, schemaBundle);
    applyPaneState();

    const shell = document.createElement('div');
    shell.className = 'uimodel-form-shell';
    formContainer.appendChild(shell);
    const valuesByFieldId = formData?.valuesByFieldId || {};
    const rowInstancesByRowId = formData?.rowInstancesByRowId || {};
    const lazy = isLazyFormRenderingAllowed();
    let sectionIndex = 0;
    let visibleSectionIndex = 0;
    let hasRenderableContent = false;

    for (const tab of (formDefinition?.tabs || [])) {
      for (const section of (tab.sections || [])) {
        sectionIndex += 1;
        if(!uiModelSectionHasRenderableFields(section, valuesByFieldId, rowInstancesByRowId)) continue;
        visibleSectionIndex += 1;

        const sectionCard = document.createElement('section');
        sectionCard.className = 'uimodel-section-card collapsible-card';
        const header = document.createElement('button');
        header.type = 'button';
        header.className = 'uimodel-section-header collapse-toggle';
        header.setAttribute('aria-expanded', 'true');
        const number = document.createElement('span');
        number.className = 'uimodel-section-number';
        number.textContent = String(sectionIndex);
        const sectionTitle = document.createElement('h3');
        sectionTitle.textContent = section.title || section.id || 'Szekció';
        const chevron = document.createElement('span');
        chevron.className = 'collapse-chevron';
        chevron.setAttribute('aria-hidden', 'true');
        chevron.textContent = '▾';
        header.appendChild(number);
        header.appendChild(sectionTitle);
        header.appendChild(chevron);
        sectionCard.appendChild(header);

        const sectionContent = document.createElement('div');
        sectionContent.className = 'collapsible-content uimodel-section-content';
        sectionCard.appendChild(sectionContent);
        shell.appendChild(sectionCard);

                /**
         * Megjeleníti vagy újrarendereli a render content állapotát a felhasználói felületen.
         *
         * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
         * @param {*} target a függvény target bemeneti értéke
         */
const renderContent = target => renderUiModelSectionContent(target, section, valuesByFieldId, rowInstancesByRowId);
        if(lazy){
          formLazyRenderer.register(sectionCard, sectionContent, renderContent, {
            eager: visibleSectionIndex === 1,
            observe: true,
            label: 'A szekció mezői görgetéskor töltődnek be…',
            matchesQuery: terms => sectionMatchesLazySearch(section, valuesByFieldId, rowInstancesByRowId, terms)
          });
        }else{
          renderContent(sectionContent);
        }
        hasRenderableContent = true;
      }
    }

    if(!hasRenderableContent){
      const empty = document.createElement('p');
      empty.className = 'empty-state';
      empty.textContent = 'Nincs megjeleníthető mező.';
      shell.appendChild(empty);
    }

    bindCollapseToggles(formContainer);
    updateToggleAllFormCollapseButton();
    bindFieldClicks();
    bindFormValueSync();
    highlightSelections();
}

/**
 * Előkészíti és elindítja a create ui model field group állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} title a függvény title bemeneti értéke
 * @param {*} metaText a függvény metaText bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function createUiModelFieldGroup(title, metaText){
  const group = document.createElement('article');
  group.className = 'uimodel-fieldgroup collapsible-card';
  const header = document.createElement('button');
  header.type = 'button';
  header.className = 'uimodel-fieldgroup-header collapse-toggle';
  header.setAttribute('aria-expanded', 'true');
  const h = document.createElement('h4');
  h.textContent = formatUiModelFieldGroupTitle(title || 'Mezőcsoport', metaText);
  header.appendChild(h);
  const chevron = document.createElement('span');
  chevron.className = 'collapse-chevron';
  chevron.setAttribute('aria-hidden', 'true');
  chevron.textContent = '▾';
  header.appendChild(chevron);
  const grid = document.createElement('div');
  grid.className = 'uimodel-fields-grid collapsible-content';
  group.appendChild(header);
  group.appendChild(grid);
  return group;
}

/**
 * Feldolgozza a format ui model field group title bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} title a függvény title bemeneti értéke
 * @param {*} metaText a függvény metaText bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function formatUiModelFieldGroupTitle(title, metaText){
  const cleanTitle = String(title || '').trim() || 'Mezőcsoport';
  const cleanMeta = String(metaText || '').trim();
  if(!cleanMeta) return cleanTitle;
  const match = cleanMeta.match(/(\d+)/);
  if(!match) return cleanTitle;
  return `${match[1]}.) ${cleanTitle}`;
}

/**
 * Megjeleníti vagy újrarendereli a render ui model field element állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @param {*} valueObj a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function renderUiModelFieldElement(field, valueObj){
  const value = valueObj?.value ?? '';
  const path = valueObj?.xmlPath || field.xmlPath || '';
  if(isM2mAttachmentTechnicalXmlPath(path)) return null;
  const fieldKey = valueObj?.key || field.id;
  const type = (field.type || 'text').toLowerCase();
  if(NAV_FIELD_BINDING_DEBUG_ENABLED && isDebugFieldId(field.id || field.xmlName || fieldKey, NAV_FIELD_BINDING_DEBUG_TARGET)){
    console.info('NAV FIELD_RENDER_DEBUG', {
      fieldId: field.id,
      xmlName: field.xmlName,
      fieldKey,
      path,
      type,
      enumCount: Array.isArray(field.enumValues) ? field.enumValues.length : 0,
      enumValuesSample: Array.isArray(field.enumValues) ? field.enumValues.slice(0, 5) : [],
      value,
      valueObj
    });
  }
  if(type === 'subtitle'){
    const subtitle = document.createElement('div');
    subtitle.className = 'uimodel-subtitle';
    subtitle.textContent = field.uiLabel || field.xsdLabel || field.label || field.xmlName || field.id || '';
    return subtitle;
  }

  const wrapper = document.createElement('div');
  const width = normalizeLayoutWidth(field.layoutWidth);
  wrapper.className = `uimodel-field form-field field-w-${width}`;
  if(type === 'link') wrapper.classList.add('uimodel-field-link');
  if(isUiModelMissingField(valueObj)) wrapper.classList.add('uimodel-missing-field');
  if(selectedFieldId === fieldKey) wrapper.classList.add('field-selected');
  wrapper.dataset.fieldId = String(fieldKey || '');
  wrapper.dataset.xmlPath = String(path || '');
  if(isM2mAttachmentTechnicalXmlPath(path)) wrapper.classList.add('m2m-attachment-source-field');

  const label = document.createElement('label');
  label.className = 'uimodel-label';
  const labelText = document.createElement('span');
  labelText.className = 'uimodel-label-text';
  labelText.textContent = `${field.uiLabel || field.xsdLabel || field.label || field.xmlName || field.id || 'Mező'}${field.required ? ' *' : ''}`;
  label.appendChild(labelText);
  if(field.id && currentUiModelDetailsVisible){
    const id = document.createElement('span');
    id.className = 'uimodel-field-id';
    id.textContent = field.id;
    label.appendChild(id);
  }
  wrapper.appendChild(label);

  if(type === 'link'){
    const link = document.createElement('button');
    link.type = 'button';
    link.className = 'uimodel-link-button';
    link.textContent = field.uiLabel || field.label || 'Kapcsolódó blokk';
    wrapper.appendChild(link);
    return wrapper;
  }

  const readonly = !!field.readonly || currentXmlFileReadOnlyMode;
  let control;
  if(type === 'select' && Array.isArray(field.enumValues) && field.enumValues.length > 0 && !shouldRenderSelectAsEditableText(field, value)){
    control = document.createElement('select');
    control.dataset.fieldId = String(fieldKey || '');
    control.disabled = readonly;
    const emptyOption = document.createElement('option');
    emptyOption.value = '';
    emptyOption.textContent = '';
    control.appendChild(emptyOption);
    (field.enumValues || []).forEach(enumValue => {
      const option = document.createElement('option');
      option.value = String(enumValue);
      option.textContent = String(enumValue);
      option.selected = String(enumValue) === String(value);
      control.appendChild(option);
    });
  } else if(type === 'select'){
    control = document.createElement('input');
    control.type = 'text';
    control.value = formatUiModelInitialValue(value, field);
    control.dataset.fieldId = String(fieldKey || '');
    control.disabled = readonly;
    configureUiModelInputControl(control, field);
    attachEnumDatalist(control, field);
  } else if(type === 'checkbox'){
    const row = document.createElement('div');
    row.className = 'uimodel-checkbox-row';
    control = document.createElement('input');
    control.type = 'checkbox';
    control.dataset.fieldId = String(fieldKey || '');
    control.checked = ['true', '1', 'x', 'X', 'I', 'i'].includes(String(value));
    control.disabled = readonly;
    row.appendChild(control);
    wrapper.appendChild(row);
    appendUiModelHints(wrapper, field);
    return wrapper;
  } else {
    control = document.createElement('input');
    control.type = uiModelInputType(type);
    control.value = formatUiModelInitialValue(value, field);
    control.dataset.fieldId = String(fieldKey || '');
    control.disabled = readonly;
    configureUiModelInputControl(control, field);
  }
  wrapper.appendChild(control);
  if(control && control._pendingDatalist){
    wrapper.appendChild(control._pendingDatalist);
    delete control._pendingDatalist;
  }
  appendUiModelHints(wrapper, field);
  return wrapper;
}

/**
 * A <code>appendUiModelHints</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} wrapper a függvény wrapper bemeneti értéke
 * @param {*} field a függvény field bemeneti értéke
 */
function appendUiModelHints(wrapper, field){
  if(!currentUiModelDetailsVisible) return;
  const hints = [];
  if(field.type) hints.push(`Típus: ${field.type}`);
  if(field.maxLength) hints.push(`Hossz: ${field.maxLength}`);
  if(field.required) hints.push('Kötelező mező');
  if(field.readonly) hints.push('Csak olvasható');
  if(!hints.length) return;
  const hint = document.createElement('div');
  hint.className = 'uimodel-field-hints';
  hint.textContent = hints.join(' · ');
  wrapper.appendChild(hint);
}

/**
 * Ellenőrzi a is ui model table block feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} row a függvény row bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function isUiModelTableBlock(row){
  const fields = (row?.fields || []).filter(field => field && field.visible !== false && !['link','subtitle'].includes(String(field.type || '').toLowerCase()));
  if(fields.length < 3) return false;
  const labels = fields.map(field => uiModelFieldLabel(field));
  const numberedLabels = labels.filter(label => /^\d{1,3}\.\s+/.test(label)).length;
  const technicalLineLabels = labels.filter(label => /^\[\d{2}\][A-Z]{1,3}\d{1,3}$/i.test(label)).length;
  const sequentialIds = fields.filter(field => /C\d{4}|G\d{4}|H\d{4}|F\d{3,4}/i.test(String(field.id || ''))).length;
  const amountLike = fields.filter(field => String(field.mask || '').includes('ezer') || /összeg|bevétel|adó|értékhatár|forint/i.test(uiModelFieldLabel(field))).length;
  const equalFourColumnGrid = fields.length >= 3 && fields.every(field => normalizeLayoutWidth(field.layoutWidth) === 4);
  const longNumberedSeries = numberedLabels >= 2 && fields.filter(field => uiModelFieldLabel(field).length >= 24).length >= 2;
  return longNumberedSeries || technicalLineLabels >= 3 || (fields.length >= 4 && amountLike >= 2 && sequentialIds >= 2) || (equalFourColumnGrid && numberedLabels >= 2);

}

/**
 * Megjeleníti vagy újrarendereli a render ui model table block állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} row a függvény row bemeneti értéke
 * @param {*} valuesByFieldId a célobjektum technikai azonosítója
 * @returns {*} a feldolgozás eredménye
 */
function renderUiModelTableBlock(row, valuesByFieldId){
  const allFields = (row?.fields || []).filter(field => field && !['link','subtitle'].includes(String(field.type || '').toLowerCase()));
  const fields = allFields.filter(field => shouldRenderNonAttachmentUiModelField(field, resolveUiModelValueObject(field, valuesByFieldId, row)));
  if(!fields.length) return null;

  const block = document.createElement('article');
  block.className = 'uimodel-table-block collapsible-card';

  const header = document.createElement('button');
  header.type = 'button';
  header.className = 'uimodel-table-header collapse-toggle';
  header.setAttribute('aria-expanded', 'true');
  const title = document.createElement('h4');
  title.textContent = row.title || row.id || 'Táblázat';
  const chevron = document.createElement('span');
  chevron.className = 'collapse-chevron';
  chevron.setAttribute('aria-hidden', 'true');
  chevron.textContent = '▾';
  header.appendChild(title);
  header.appendChild(chevron);
  block.appendChild(header);

  const tableWrap = document.createElement('div');
  tableWrap.className = 'uimodel-table-scroll collapsible-content';
  const table = document.createElement('table');
  table.className = 'uimodel-entry-table';
  table.innerHTML = '<colgroup><col class="col-row-no"><col class="col-description"><col class="col-value"></colgroup><thead><tr><th class="col-row-no">Sor</th><th class="col-description">Megnevezés</th><th class="col-value">Érték</th></tr></thead>';
  const tbody = document.createElement('tbody');

  fields.forEach((field, index) => {
    const valueObj = resolveUiModelValueObject(field, valuesByFieldId, row);
    const tr = document.createElement('tr');
    tr.className = 'form-field uimodel-table-row';
    if(isUiModelMissingField(valueObj)) tr.classList.add('uimodel-missing-field');
    const path = valueObj?.xmlPath || field.xmlPath || '';
    const fieldKey = valueObj?.key || field.id;
    tr.dataset.fieldId = String(fieldKey || '');
    tr.dataset.xmlPath = String(path || '');
    if(selectedFieldId === fieldKey) tr.classList.add('field-selected');

    const parsed = parseUiModelTableLabel(uiModelFieldLabel(field), index);

    const tdNo = document.createElement('td');
    tdNo.className = 'uimodel-table-row-no';
    tdNo.textContent = parsed.rowNo;
    tr.appendChild(tdNo);

    const tdLabel = document.createElement('td');
    tdLabel.className = 'uimodel-table-description';
    const labelText = document.createElement('div');
    labelText.className = 'uimodel-table-label';
    labelText.textContent = parsed.description || uiModelFieldLabel(field) || field.id || '';
    tdLabel.appendChild(labelText);
    if(field.id && currentUiModelDetailsVisible){
      const id = document.createElement('div');
      id.className = 'uimodel-table-field-id';
      id.textContent = field.id;
      tdLabel.appendChild(id);
    }
    tr.appendChild(tdLabel);

    const tdValue = document.createElement('td');
    tdValue.className = 'uimodel-table-value';
    tdValue.appendChild(renderUiModelTableControl(field, valueObj));
    const hints = uiModelTableHints(field);
    if(hints){
      const hint = document.createElement('div');
      hint.className = 'uimodel-table-field-hints uimodel-table-value-hints';
      hint.textContent = hints;
      tdValue.appendChild(hint);
    }
    tr.appendChild(tdValue);

    tbody.appendChild(tr);
  });

  table.appendChild(tbody);
  tableWrap.appendChild(table);
  block.appendChild(tableWrap);
  return block;
}

/**
 * Megjeleníti vagy újrarendereli a render ui model table control állapotát a felhasználói felületen.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @param {*} valueObj a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function renderUiModelTableControl(field, valueObj){
  const value = valueObj?.value ?? '';
  const type = String(field.type || 'text').toLowerCase();
  const readonly = !!field.readonly || currentXmlFileReadOnlyMode;
  const holder = document.createElement('div');
  holder.className = 'uimodel-table-control';

  if(type === 'checkbox'){
    const input = document.createElement('input');
    input.type = 'checkbox';
    input.dataset.fieldId = String(valueObj?.key || field.id || '');
    input.checked = ['true','1','x','X','I','i'].includes(String(value));
    input.disabled = readonly;
    holder.appendChild(input);
    return holder;
  }

  if(type === 'select' && Array.isArray(field.enumValues) && field.enumValues.length > 0 && !shouldRenderSelectAsEditableText(field, value)){
    const select = document.createElement('select');
    select.dataset.fieldId = String(valueObj?.key || field.id || '');
    select.disabled = readonly;
    const emptyOption = document.createElement('option');
    emptyOption.value = '';
    emptyOption.textContent = '';
    select.appendChild(emptyOption);
    (field.enumValues || []).forEach(enumValue => {
      const option = document.createElement('option');
      option.value = String(enumValue);
      option.textContent = String(enumValue);
      option.selected = String(enumValue) === String(value);
      select.appendChild(option);
    });
    holder.appendChild(select);
    return holder;
  }

  const input = document.createElement('input');
  input.type = uiModelInputType(type);
  input.value = formatUiModelInitialValue(value, field);
  input.dataset.fieldId = String(valueObj?.key || field.id || '');
  input.disabled = readonly;
  configureUiModelInputControl(input, field);
  holder.appendChild(input);
  const unit = uiModelUnitFromMask(field.mask);
  if(unit){
    const unitSpan = document.createElement('span');
    unitSpan.className = 'uimodel-table-unit';
    unitSpan.textContent = unit;
    holder.appendChild(unitSpan);
  }
  return holder;
}

/**
 * A <code>uiModelFieldLabel</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function uiModelFieldLabel(field){
  return String(field?.uiLabel || field?.xsdLabel || field?.label || field?.xmlName || field?.id || '').trim();
}

/**
 * Feldolgozza a parse ui model table label bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} label a függvény label bemeneti értéke
 * @param {*} index az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
 * @returns {*} a feldolgozás eredménye
 */
function parseUiModelTableLabel(label, index){
  const text = String(label || '').trim();
  let m = text.match(/^(\d{1,3}\.)\s*(.*)$/);
  if(m) return { rowNo: m[1], description: m[2] || text };
  m = text.match(/^\[(\d{2})\]([A-Z]{1,3}\d{1,3})$/i);
  if(m) return { rowNo: `${m[1]}.`, description: text };
  return { rowNo: String(index + 1).padStart(2, '0') + '.', description: text };
}

/**
 * A <code>uiModelTableHints</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function uiModelTableHints(field){
  if(!currentUiModelDetailsVisible) return '';
  const hints = [];
  if(field.type) hints.push(`Típus: ${field.type}`);
  if(field.maxLength) hints.push(`Hossz: ${field.maxLength}`);
  if(field.required) hints.push('Kötelező mező');
  if(field.readonly) hints.push('Csak olvasható');
  return hints.join(' · ');
}

/**
 * A <code>uiModelInputType</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} type a függvény type bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function uiModelInputType(type){
  if(type === 'date') return 'date';
  if(type === 'number') return 'number';
  return 'text';
}

/**
 * A <code>configureUiModelInputControl</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} input a függvény input bemeneti értéke
 * @param {*} field a függvény field bemeneti értéke
 */
function configureUiModelInputControl(input, field){
  if(field.maxLength) input.maxLength = Number(field.maxLength);
  if(input.type === 'date'){
    input.placeholder = '';
  }
  if(shouldUseNumericInputMode(field)){
    input.inputMode = 'numeric';
    input.autocomplete = 'off';
  }
}

/**
 * Ellenőrzi a should render select as editable text feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function shouldRenderSelectAsEditableText(field, value){
  const enumValues = Array.isArray(field?.enumValues) ? field.enumValues.map(v => String(v)) : [];
  const valueText = String(value ?? '');
  // A dokumentum globális XSD-hibás állapota önmagában nem teheti át az enumot
  // datalistes szövegmezővé. Ha az aktuális érték érvényes enumérték, valódi
  // select marad, így megnyitáskor mindig az összes értékkészlet látható.
  // Csak a konkrét mező enumon kívüli értékénél kell szabad szöveges javítás.
  return !!valueText && enumValues.length > 0 && !enumValues.includes(valueText);
}

/**
 * Kezeli vagy beköti a attach enum datalist esemény- és inicializációs folyamatát.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} input a függvény input bemeneti értéke
 * @param {*} field a függvény field bemeneti értéke
 */
function attachEnumDatalist(input, field){
  const enumValues = Array.isArray(field?.enumValues) ? field.enumValues : [];
  if(!enumValues.length || !input) return;
  const idBase = String(field?.id || field?.xmlName || 'enum').replace(/[^A-Za-z0-9_-]/g, '_');
  const randomPart = Array.from(crypto.getRandomValues(new Uint8Array(6)), value => value.toString(16).padStart(2, '0')).join('');
  const listId = `enum-values-${idBase}-${randomPart}`;
  const list = document.createElement('datalist');
  list.id = listId;
  enumValues.forEach(enumValue => {
    const option = document.createElement('option');
    option.value = String(enumValue);
    list.appendChild(option);
  });
  input.setAttribute('list', listId);
  input.dataset.enumDatalistId = listId;
  // A datalistet a mező mellé tesszük, ezért a renderelő wrapper append után is működik.
  input._pendingDatalist = list;
}

/**
 * Ellenőrzi a should use numeric input mode feltételeit, és a hívó számára döntési eredményt állít elő.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function shouldUseNumericInputMode(field){
  const mask = String(field?.mask || '');
  return mask.includes('ezer') || /adóazonosító|adószám|összeg|bevétel|forint/i.test(uiModelFieldLabel(field));
}

/**
 * Feldolgozza a format ui model initial value bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @param {*} field a függvény field bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function formatUiModelInitialValue(value, field){
  const str = String(value ?? '');
  const type = String(field?.type || '').toLowerCase();
  if(type === 'date') return normalizeUiModelDateValue(str);
  const mask = String(field?.mask || '');
  if(!str || !mask) return str;
  if(mask.includes('ezer')) return normalizeUiModelNumericText(str);
  return str;
}

/**
 * Feldolgozza a format ui model input value bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} input a függvény input bemeneti értéke
 */
function formatUiModelInputValue(input){
  if(!input || input.type === 'checkbox' || input.tagName === 'SELECT') return;
  const mask = input.dataset?.mask || '';
  if(!mask) return;
  const before = input.value;
  let after = before;
  if(mask.includes('ezer')) after = normalizeUiModelNumericText(before);
  if(after !== before){
    input.value = after;
    try { input.setSelectionRange(after.length, after.length); } catch {}
  }
}

/**
 * Szinkronizálja vagy frissíti a apply ui model mask által kezelt állapotot a megadott adatok alapján.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @param {*} mask a függvény mask bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function applyUiModelMask(value, mask){
  const digits = String(value || '').replace(/\D/g, '');
  let result = '';
  let digitIndex = 0;
  const maxDigits = (String(mask).match(/#/g) || []).length;
  const limitedDigits = maxDigits ? digits.slice(0, maxDigits) : digits;
  for(const ch of String(mask)){
    if(ch === '#'){
      if(digitIndex >= limitedDigits.length) break;
      result += limitedDigits[digitIndex++];
    }else if(digitIndex < limitedDigits.length){
      result += ch;
    }
  }
  return result;
}

/**
 * Feldolgozza a normalize ui model numeric text bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function normalizeUiModelNumericText(value){
  return String(value || '').replace(/[^0-9,.-]/g, '');
}

/**
 * Feldolgozza a normalize ui model date value bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} value a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function normalizeUiModelDateValue(value){
  const str = String(value ?? '').trim();
  if(!str) return '';
  let m = str.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if(m) return str;
  m = str.match(/^(\d{4})[.](\d{2})[.](\d{2})$/);
  if(m) return `${m[1]}-${m[2]}-${m[3]}`;
  return str;
}

/**
 * A <code>uiModelMaskExample</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} mask a függvény mask bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function uiModelMaskExample(mask){
  const m = String(mask || '');
  if(!m) return '';
  if(m === '########-#-##') return '12345676-1-42';
  if(m === '##########') return '1234567890';
  if(m === '###########') return '12345678901';
  if(m === '####.##.##') return '2026.01.31';
  if(/^20\d{2}\.##\.##/.test(m)) return m.replace('##.##', '01.31');
  if(m.includes('ezer')) return '';
  return m;
}

/**
 * A <code>uiModelUnitFromMask</code> függvény a űrlap-megjelenítési és mezőkötési folyamat egy önálló feldolgozási lépését valósítja meg.
 *
 * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
 * @param {*} mask a függvény mask bemeneti értéke
 * @returns {*} a feldolgozás eredménye
 */
function uiModelUnitFromMask(mask){
  const m = String(mask || '').toLowerCase();
  if(m.includes('ezer')) return 'ezer';
  return '';
}

/**
 * Megjeleníti vagy újrarendereli a render field element állapotát a felhasználói felületen.
 *
 * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
 * @param {*} field a függvény field bemeneti értéke
 * @param {*} valueObj a feldolgozandó vagy beállítandó érték
 * @returns {*} a feldolgozás eredménye
 */
function renderFieldElement(field, valueObj) {
    const value = valueObj?.value ?? '';
    const path = valueObj?.xmlPath || field.xmlPath || '';
    if(isM2mAttachmentTechnicalXmlPath(path)) return null;
    const fieldKey = valueObj?.key || field.id;
    const type = (field.type || 'text').toLowerCase();

    const uiLabelRaw = typeof field.uiLabel === 'string' ? field.uiLabel.trim() : '';
    const xsdLabelRaw = typeof field.xsdLabel === 'string' ? field.xsdLabel.trim() : '';
    const fallbackLabelRaw = typeof field.xmlName === 'string' && field.xmlName.trim()
        ? field.xmlName.trim()
        : (field.id || 'Mező');

    const visibleLabelRaw = uiLabelRaw || xsdLabelRaw || fallbackLabelRaw;

    if (type === 'subtitle') {
        const subtitle = document.createElement('div');
        subtitle.className = 'form-subtitle';
        subtitle.textContent = field.uiLabel || field.xsdLabel || field.label || field.xmlName || field.id || '';
        return subtitle;
    }

    const wrapper = document.createElement('div');

    const width = normalizeLayoutWidth(field.layoutWidth);
    wrapper.className = type === 'link'
        ? `form-field form-field-link field-w-${width}`
        : `form-field field-w-${width}`;

    if (selectedFieldId === fieldKey) {
        wrapper.classList.add('field-selected');
    }

    wrapper.dataset.fieldId = String(fieldKey || '');
    wrapper.dataset.xmlPath = String(path || '');
    if (isM2mAttachmentTechnicalXmlPath(path)) wrapper.classList.add('m2m-attachment-source-field');

    const labelRow = document.createElement('div');
    labelRow.className = 'field-label-row';

    const labelText = document.createElement('span');
    labelText.className = 'field-label-text';
    labelText.textContent = `${visibleLabelRaw}${field.required ? ' *' : ''}`;

    if (uiLabelRaw && xsdLabelRaw && xsdLabelRaw !== uiLabelRaw) {
        labelText.title = xsdLabelRaw;
    }

    labelRow.appendChild(labelText);
    wrapper.appendChild(labelRow);

    if (type === 'link') {
        const hint = document.createElement('div');
        hint.className = 'field-link-hint';
        hint.textContent = 'Kapcsolódó blokk';
        wrapper.appendChild(hint);
        return wrapper;
    }

    const readonly = !!field.readonly || currentXmlFileReadOnlyMode;

    if (type === 'select' && Array.isArray(field.enumValues) && field.enumValues.length > 0 && !shouldRenderSelectAsEditableText(field, value)) {
        const select = document.createElement('select');
        select.dataset.fieldId = String(fieldKey || '');
        select.disabled = readonly;

        const emptyOption = document.createElement('option');
        emptyOption.value = '';
        emptyOption.textContent = '';
        select.appendChild(emptyOption);

        (field.enumValues || []).forEach(enumValue => {
            const option = document.createElement('option');
            option.value = String(enumValue);
            option.textContent = String(enumValue);
            option.selected = String(enumValue) === String(value);
            select.appendChild(option);
        });

        wrapper.appendChild(select);
        return wrapper;
    }

    if (type === 'checkbox') {
        const checkboxRow = document.createElement('div');
        checkboxRow.className = 'checkbox-row';

        const input = document.createElement('input');
        input.type = 'checkbox';
        input.dataset.fieldId = String(fieldKey || '');
        input.checked = ['true', '1', 'x', 'X'].includes(String(value));
        input.disabled = readonly;

        checkboxRow.appendChild(input);
        wrapper.appendChild(checkboxRow);
        return wrapper;
    }

    const input = document.createElement('input');
    input.type = type === 'number' ? 'number' : type === 'date' ? 'date' : 'text';
    input.value = String(value);
    input.dataset.fieldId = String(fieldKey || '');
    input.disabled = readonly;
    if (field.maxLength) {
        input.maxLength = Number(field.maxLength);
    }

    if (field.mask) {
        input.dataset.mask = String(field.mask);
        input.title = `Maszk: ${field.mask}`;
        input.placeholder = String(field.mask);
    }

    wrapper.appendChild(input);

    if (field.mask) {
        const maskHint = document.createElement('div');
        maskHint.className = 'field-mask-hint';
        maskHint.textContent = `Maszk: ${field.mask}`;
        wrapper.appendChild(maskHint);
    }

    return wrapper;
}

Object.assign(globalThis, {
  renderForm,
  renderClassicForm,
  createFieldGroupCard,
  resolveUiModelValueObject,
  renderUiModelForm,
  createUiModelFieldGroup,
  formatUiModelFieldGroupTitle,
  renderUiModelFieldElement,
  appendUiModelHints,
  isUiModelTableBlock,
  renderUiModelTableBlock,
  renderUiModelTableControl,
  uiModelFieldLabel,
  parseUiModelTableLabel,
  uiModelTableHints,
  uiModelInputType,
  configureUiModelInputControl,
  shouldRenderSelectAsEditableText,
  attachEnumDatalist,
  shouldUseNumericInputMode,
  formatUiModelInitialValue,
  formatUiModelInputValue,
  applyUiModelMask,
  normalizeUiModelNumericText,
  normalizeUiModelDateValue,
  uiModelMaskExample,
  uiModelUnitFromMask,
  renderFieldElement
});
