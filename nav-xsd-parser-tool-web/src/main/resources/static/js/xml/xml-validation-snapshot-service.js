import { isFormDirty, syncStateFromRuntime } from '../core/app-state.js';
import { quickSaveCurrentXmlFile } from './xml-save-service.js';

export async function ensureCurrentXmlSavedForValidation(options = {}){
  const {
    validationLabel = 'ellenőrzés',
    quiet = true,
    allowDisabledButton = true
  } = options;

  syncStateFromRuntime();

  if(!isFormDirty()){
    return { ok: true, saved: false, skipped: true, reason: 'clean' };
  }

  const ok = await quickSaveCurrentXmlFile({
    quiet,
    allowDisabledButton,
    skipIfClean: true
  });

  if(!ok){
    return {
      ok: false,
      saved: false,
      skipped: false,
      reason: `${validationLabel} előtti gyorsmentés sikertelen`
    };
  }

  return { ok: true, saved: true, skipped: false, reason: 'saved' };
}
