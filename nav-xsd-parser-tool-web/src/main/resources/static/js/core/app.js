/**
 * @module core/app
 *
 * A közös frontend infrastruktúra- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

(function bootstrapApplicationRuntime(){
  const currentScript = document.currentScript;
  const version = currentScript?.src ? new URL(currentScript.src, window.location.href).search : '';
  const runtimeUrl = `/js/runtime/application-runtime.js${version}`;
  const pageUrl = `/js/pages/xpath-validator-page.js${version}`;

  window.NavApplicationRuntimeReady = import(runtimeUrl)
    .then(async () => {
      if(document.getElementById('xpathValidatorForm')){
        const page = await import(pageUrl);
        page.initXpathValidatorPage?.();
      }
    })
    .catch(error => {
      console.error('A frontend alkalmazás-runtime betöltése sikertelen.', error);
      const target = document.getElementById('messages') || document.getElementById('m2mMessages');
      if(target){
        target.innerHTML = '<div class="message error">A frontend inicializálása sikertelen. Frissítsd az oldalt vagy ellenőrizd a konzolt.</div>';
      }
      throw error;
    });
})();
