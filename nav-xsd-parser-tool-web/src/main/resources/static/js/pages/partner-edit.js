/**
 * @module pages/partner-edit
 *
 * A oldalspecifikus felhasználói felület- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

(()=>{
  const form=document.getElementById('partnerEditForm');
  const idInput=document.getElementById('partnerEditId');
  const title=document.getElementById('partnerEditTitle');
  const subtitle=document.getElementById('partnerEditSubtitle');
  const message=document.getElementById('partnerEditMessage');
  const saveButton=document.getElementById('savePartnerButton');
  const params=new URLSearchParams(window.location.search);
  const partnerId=params.get('id');
  const taxNumberInput=document.getElementById('partnerTaxNumber');


    /**
   * Feldolgozza a format tax number input bemenetét, és a következő feldolgozási lépés számára normalizált reprezentációt készít.
   *
   * <p>Az űrlap- és XML-kötésnél a teljes kontextust és az indexelt útvonalakat meg kell őrizni; multiform esetben globális fieldId-alapú fallback nem tekinthető egyértelmű azonosításnak.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function formatTaxNumberInput(value){
    const digits=String(value||'').replace(/\D/g,'').slice(0,11);
    return [digits.slice(0,8),digits.slice(8,9),digits.slice(9,11)].filter(Boolean).join('-');
  }
    /**
   * Ellenőrzi a is valid hungarian tax number feltételeit, és a hívó számára döntési eredményt állít elő.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} value a feldolgozandó vagy beállítandó érték
   * @returns {*} a feldolgozás eredménye
   */
function isValidHungarianTaxNumber(value){
    const normalized=formatTaxNumberInput(value);
    if(!/^\d{8}-\d-\d{2}$/.test(normalized)) return false;
    const core=normalized.slice(0,8);
    const weights=[9,7,3,1,9,7,3];
    const sum=weights.reduce((total,weight,index)=>total+Number(core[index])*weight,0);
    return Number(core[7])===((10-(sum%10))%10);
  }

    /**
   * A <code>goBack</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   */
function goBack(){window.location.href='/partners.html';}
    /**
   * Megjeleníti vagy újrarendereli a show message állapotát a felhasználói felületen.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} type a függvény type bemeneti értéke
   * @param {*} text a függvény text bemeneti értéke
   */
function showMessage(type,text){message.className=`partner-edit-message ${type||''}`;message.textContent=text;message.hidden=false;}
    /**
   * A <code>fill</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
   *
   * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
   * @param {*} partner a függvény partner bemeneti értéke
   */
function fill(partner){
    idInput.value=partner?.id||'';
    for(const [key,value] of Object.entries(partner||{})){
      const field=form.elements[key];
      if(field) field.value=value??'';
    }
  }
    /**
   * Betölti vagy lekéri a load partner művelethez szükséges adatot a rendelkezésre álló kliens- vagy szerveroldali forrásból.
   *
   * <p>A feldolgozás megőrzi a főlap, melléklap vagy csatolmány konkrét előfordulásának kontextusát, hogy az ismétlődő elemek ne keveredjenek össze.</p>
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function loadPartner(){
    if(!partnerId){title.textContent='Új partner';subtitle.textContent='Új partner törzsadatainak rögzítése.';return;}
    const response=await fetch('/api/partners',{credentials:'same-origin'});
    if(!response.ok) throw new Error(await response.text()||'A partneradatok nem tölthetők be.');
    const partners=await response.json();
    const partner=partners.find(item=>String(item.id)===String(partnerId));
    if(!partner) throw new Error('A megadott partner nem található.');
    title.textContent='Partner szerkesztése';
    subtitle.textContent=`${partner.taxNumber||''}${partner.taxNumber&&partner.name?' – ':''}${partner.name||''}`;
    fill(partner);
  }
    /**
   * Előkészíti és elindítja a save állapotváltozást, majd feldolgozza annak kliensoldali eredményét.
   *
   * <p>A kliensoldali állapot a mentési UX-et vezérli, de a végleges módosíthatósági és jogosultsági döntés továbbra is a backend feladata.</p>
   * @param {*} event a feldolgozandó böngészőesemény
   * @returns {Promise<void>} a folyamat befejeződését jelző Promise
   */
async function save(event){
    event.preventDefault();message.hidden=true;
    if(taxNumberInput) taxNumberInput.value=formatTaxNumberInput(taxNumberInput.value);
    if(!form.reportValidity()) return;
    if(!isValidHungarianTaxNumber(taxNumberInput?.value)){showMessage('error','Az adószám első nyolc számjegyének CDV ellenőrzése sikertelen.');taxNumberInput?.focus();return;}
    const data=Object.fromEntries(new FormData(form));
    data.active=true;
    const id=idInput.value;
    saveButton.disabled=true;
    try{
      const response=await fetch(id?`/api/partners/${encodeURIComponent(id)}`:'/api/partners',{
        method:id?'PUT':'POST',
        headers:{'Content-Type':'application/json'},
        credentials:'same-origin',
        body:JSON.stringify(data)
      });
      if(!response.ok) throw new Error(await response.text()||'A partner mentése sikertelen.');
      const saved=await response.json();
      fill(saved);
      title.textContent='Partner szerkesztése';
      subtitle.textContent=`${saved.taxNumber||''}${saved.taxNumber&&saved.name?' – ':''}${saved.name||''}`;
      showMessage('success','A partner adatai sikeresen mentésre kerültek.');
      if(!id && saved.id){window.history.replaceState({},'',`/partner-edit.html?id=${encodeURIComponent(saved.id)}`);}
    }catch(error){showMessage('error',error.message||'A partner mentése sikertelen.');}
    finally{saveButton.disabled=false;}
  }

  document.getElementById('backToPartnerList')?.addEventListener('click',goBack);
  document.getElementById('cancelPartnerEdit')?.addEventListener('click',goBack);
  taxNumberInput?.addEventListener('input',()=>{taxNumberInput.value=formatTaxNumberInput(taxNumberInput.value);});
  form?.addEventListener('submit',save);
  loadPartner().catch(error=>{showMessage('error',error.message||'A partneradatok nem tölthetők be.');form.querySelectorAll('input,button').forEach(element=>{if(!['backToPartnerList','cancelPartnerEdit'].includes(element.id))element.disabled=true;});});
})();
