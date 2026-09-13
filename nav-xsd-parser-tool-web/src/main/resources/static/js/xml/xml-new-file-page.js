/**
 * @module xml/xml-new-file-page
 *
 * Az Űrlapállományok oldalon inicializálja az új XML létrehozási funkciót.
 */

import { initNewXmlUi } from './xml-new-file-ui.js';

if(document.readyState === 'loading'){
  document.addEventListener('DOMContentLoaded', initNewXmlUi, { once:true });
}else{
  initNewXmlUi();
}
