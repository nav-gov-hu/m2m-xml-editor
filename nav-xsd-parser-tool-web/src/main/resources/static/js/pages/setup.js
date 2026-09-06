/**
 * @module pages/setup
 *
 * A oldalspecifikus felhasználói felület- működéshez tartozó ES modul. A fájl a hozzá tartozó UI-állapotot, eseménykezelést és backend-kommunikációt a modul felelősségi határán belül tartja.
 */

(() => {
    const steps = ['base', 'admin', 'integrations', 'review'];
    let index = 0;
    let currentSecurityMode = 'MULTI_USER';
    let installerAdminPasswordAvailable = false;
    let databaseTestToken = null;
    let pendingCompletionWaitStarted = false;
        /**
     * A <code>q</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @param {*} selector a függvény selector bemeneti értéke
     */
const q = selector => document.querySelector(selector);
    const panels = [...document.querySelectorAll('[data-panel]')];
    const tabs = [...document.querySelectorAll('[data-step]')];
        /**
     * A <code>mode</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     */
const mode = () => currentSecurityMode;

        /**
     * Megjeleníti vagy újrarendereli a show message állapotát a felhasználói felületen.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
     * @param {*} type a függvény type bemeneti értéke
     */
function showMessage(message, type = 'error') {
        const target = q('#setupMessage');
        target.hidden = false;
        target.className = `setup-message ${type}`;
        target.textContent = message;
    }

        /**
     * Eltávolítja vagy alaphelyzetbe állítja a clear message művelethez tartozó kliensoldali állapotot.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     */
function clearMessage() {
        const target = q('#setupMessage');
        target.hidden = true;
        target.textContent = '';
    }

        /**
     * A <code>escapeHtml</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @param {*} value a feldolgozandó vagy beállítandó érték
     * @returns {*} a feldolgozás eredménye
     */
function escapeHtml(value) {
        const element = document.createElement('div');
        element.textContent = value || '';
        return element.innerHTML;
    }

        /**
     * A <code>activateStep</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @param {*} stepIndex az előfordulást, lapozást vagy mennyiségi korlátot meghatározó érték
     */
function activateStep(stepIndex) {
        index = Math.max(0, Math.min(stepIndex, steps.length - 1));
        render();
    }

        /**
     * A <code>focusInvalid</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @param {*} input a függvény input bemeneti értéke
     * @param {*} message a megjelenítendő vagy feldolgozandó üzenet
     * @returns {*} a feldolgozás eredménye
     */
function focusInvalid(input, message) {
        const panel = input.closest('[data-panel]');
        const panelIndex = panels.indexOf(panel);
        if (panelIndex >= 0) {
            activateStep(panelIndex);
        }
        showMessage(message || input.validationMessage || 'A mező értéke hibás.');
        window.setTimeout(() => input.focus(), 0);
        return false;
    }

        /**
     * Ellenőrzi a validate base feltételeit, és a hívó számára döntési eredményt állít elő.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @returns {*} a feldolgozás eredménye
     */
function validateBaseFields() {
        const dataDirectory = q('#dataDirectory');
        if (!dataDirectory.value.trim()) {
            return focusInvalid(dataDirectory, 'Az adatkönyvtár megadása kötelező.');
        }
        if (q('#databaseType').value !== 'H2') {
            for (const id of ['databaseHost','databasePort','databaseName','databaseUsername']) {
                const input=q(`#${id}`);
                if (!input.value.trim()) return focusInvalid(input, 'A külső adatbázis kapcsolati mezőinek megadása kötelező.');
            }
        }
        return true;
    }

    /**
     * Ellenőrzi az adatbázis-kapcsolati teszt meglétét az aktuális alapbeállításokhoz.
     * @returns {boolean} igaz, ha a kapcsolati teszt sikeres és még érvényes
     */
function validateBase() {
        if (!validateBaseFields()) {
            return false;
        }
        if (!databaseTestToken) {
            showMessage('A továbblépés előtt tesztelje sikeresen az adatbázis-kapcsolatot.');
            q('#databaseTestButton').focus();
            return false;
        }
        return true;
    }

        /**
     * Ellenőrzi a validate admin feltételeit, és a hívó számára döntési eredményt állít elő.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @returns {*} a feldolgozás eredménye
     */
function validateAdmin() {
        const username = q('#adminUsername');
        const displayName = q('#adminDisplayName');
        const email = q('#adminEmail');
        const password = q('#adminPassword');
        const confirmation = q('#adminPasswordConfirmation');

        if (!username.value.trim()) {
            return focusInvalid(username, 'A kezdő admin felhasználónevének megadása kötelező.');
        }
        if (!displayName.value.trim()) {
            return focusInvalid(displayName, 'A kezdő admin megjelenített nevének megadása kötelező.');
        }
        if (email.value.trim() && !email.validity.valid) {
            return focusInvalid(email, 'Az e-mail-cím formátuma hibás.');
        }
        if (!password.value && !installerAdminPasswordAvailable) {
            return focusInvalid(password, 'A kezdő admin jelszavának megadása kötelező.');
        }
        if (!password.value && installerAdminPasswordAvailable) {
            return true;
        }
        if (password.value !== confirmation.value) {
            return focusInvalid(confirmation, 'A kezdő admin jelszavai nem egyeznek.');
        }
        if (password.value.length < 8 || !/[a-z]/.test(password.value) || !/[A-Z]/.test(password.value) || !/[0-9]/.test(password.value) || !/[^A-Za-z0-9]/.test(password.value)) {
            return focusInvalid(password, 'A jelszó legalább 8 karakteres legyen, és tartalmazzon kisbetűt, nagybetűt, számot és speciális karaktert.');
        }
        return true;
    }

    /**
     * Ellenőrzi az opcionális integrációs adatok formai összefüggéseit.
     * @returns {boolean} igaz, ha az adatok üresek vagy érvényes formátumúak
     */
function validateIntegrations() {
        const apiKey = q('#m2mApiKey');
        const clientId = q('#m2mClientId');
        const clientSecret = q('#m2mClientSecret');
        const apiKeyValue = apiKey.value.trim();

        if (apiKeyValue) {
            const parts = apiKeyValue.split('-');
            if (parts.length !== 4 || parts.some(part => !part.trim())) {
                return focusInvalid(apiKey, 'Az M2M API-kulcsnak négy, kötőjellel elválasztott részből kell állnia.');
            }
        }

        const hasClientId = Boolean(clientId.value.trim());
        const hasClientSecret = Boolean(clientSecret.value.trim());
        if (hasClientId !== hasClientSecret) {
            return focusInvalid(hasClientId ? clientSecret : clientId, 'Az M2M Client ID és Client Secret csak együtt adható meg.');
        }
        return true;
    }

        /**
     * Ellenőrzi a setup lépéseit az aktuális pontig.
     * @param {*} stepIndex az ellenőrizendő utolsó lépés indexe
     * @returns {*} a feldolgozás eredménye
     */
function validateThrough(stepIndex) {
        clearMessage();
        if (stepIndex >= 0 && !validateBase()) {
            return false;
        }
        if (stepIndex >= 1 && !validateAdmin()) {
            return false;
        }
        if (stepIndex >= 2 && !validateIntegrations()) {
            return false;
        }
        return true;
    }

        /**
     * Megjeleníti vagy újrarendereli a render állapotát a felhasználói felületen.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     */
function render() {
        panels.forEach(panel => panel.classList.toggle('active', panel.dataset.panel === steps[index]));
        tabs.forEach((tab, tabIndex) => tab.classList.toggle('active', tabIndex === index));
        q('#setupBack').disabled = index === 0;
        q('#setupNext').hidden = index === steps.length - 1;
        q('#setupNext').disabled = index === 0 && !databaseTestToken;
        q('#setupSave').hidden = index !== steps.length - 1;

        if (steps[index] === 'review') {
            const review = q('#setupReview');
            const dl = document.createElement('dl');
            const configured = value => value && value.trim() ? 'Megadva' : 'Nincs megadva';
            [
                ['Adatkönyvtár', q('#dataDirectory').value],
                ['Adatbázis', q('#databaseType').value],
                ['Kezdő admin', q('#adminUsername').value],
                ['GitHub token', configured(q('#githubToken').value)],
                ['M2M API-kulcs', configured(q('#m2mApiKey').value)],
                ['M2M kliensregisztráció', q('#m2mClientId').value.trim() && q('#m2mClientSecret').value.trim() ? 'Megadva' : 'Nincs megadva']
            ].forEach(([label, value]) => {
                const dt = document.createElement('dt');
                const dd = document.createElement('dd');
                dt.textContent = label;
                dd.textContent = String(value || '');
                dl.append(dt, dd);
            });
            review.replaceChildren(dl);
        }
    }

    function updateDatabaseTestStatus(text, type = '') {
        const status = q('#databaseTestStatus');
        status.textContent = text;
        status.className = `setup-database-test-status${type ? ` ${type}` : ''}`;
    }

    function invalidateDatabaseTest() {
        databaseTestToken = null;
        updateDatabaseTestStatus('A továbblépéshez sikeres kapcsolati teszt szükséges.');
        if (index === 0) {
            q('#setupNext').disabled = true;
        }
    }

    function databasePayload() {
        return {
            dataDirectory: q('#dataDirectory').value.trim(),
            databaseType: q('#databaseType').value,
            databaseHost: q('#databaseHost').value.trim(),
            databasePort: q('#databasePort').value.trim(),
            databaseName: q('#databaseName').value.trim(),
            databaseSchema: q('#databaseSchema').value.trim(),
            databaseUsername: q('#databaseType').value === 'H2' ? 'sa' : q('#databaseUsername').value.trim(),
            databasePassword: q('#databaseType').value === 'H2' ? '' : q('#databasePassword').value
        };
    }

    async function testDatabaseConnection() {
        clearMessage();
        if (!validateBaseFields()) {
            return;
        }
        invalidateDatabaseTest();
        const button = q('#databaseTestButton');
        button.disabled = true;
        updateDatabaseTestStatus('Kapcsolat tesztelése folyamatban...');
        try {
            const response = await fetch('/api/setup/database/test', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(databasePayload())
            });
            const body = await response.json().catch(() => ({}));
            if (!response.ok || !body.databaseTestToken) {
                throw new Error(body.message || 'Az adatbázis-kapcsolat tesztelése sikertelen.');
            }
            databaseTestToken = body.databaseTestToken;
            const product = [body.databaseProduct, body.databaseVersion].filter(Boolean).join(' ');
            updateDatabaseTestStatus(product ? `Sikeres kapcsolat: ${product}` : 'Az adatbázis-kapcsolat sikeres.', 'success');
            q('#setupNext').disabled = false;
        } catch (error) {
            databaseTestToken = null;
            updateDatabaseTestStatus(error.message || String(error), 'error');
            showMessage(error.message || String(error));
            q('#setupNext').disabled = true;
        } finally {
            button.disabled = false;
        }
    }

    function applyDatabaseDefaults(reset=true) {
        const type=q('#databaseType').value;
        const external=type!=='H2';
        q('#externalDatabaseFields').hidden=!external;
        const defaults={MYSQL:['3306','nav_xsd_parser_tool','nav_xsd_parser_tool'],POSTGRESQL:['5432','nav_xsd_parser_tool','public'],ORACLE:['1521','FREEPDB1','NAV_USER']};
        if (external && reset) {
            const d=defaults[type];
            q('#databasePort').value=d[0];q('#databaseName').value=d[1];q('#databaseSchema').value=d[2];
            if (!q('#databaseUsername').value.trim()) q('#databaseUsername').value=type==='ORACLE'?'NAV_USER':'nav_user';
        }
        invalidateDatabaseTest();
    }
    q('#databaseType').addEventListener('change',()=>applyDatabaseDefaults(true));
    q('#databaseTestButton').addEventListener('click', testDatabaseConnection);
    ['dataDirectory','databaseHost','databasePort','databaseName','databaseSchema','databaseUsername','databasePassword']
        .forEach(id => q(`#${id}`).addEventListener('input', invalidateDatabaseTest));

    q('#setupNext').addEventListener('click', () => {
        if (!validateThrough(index)) {
            return;
        }
        if (index < steps.length - 1) {
            activateStep(index + 1);
        }
    });

    q('#setupBack').addEventListener('click', () => {
        if (index > 0) {
            clearMessage();
            activateStep(index - 1);
        }
    });

    tabs.forEach((tab, tabIndex) => {
        tab.addEventListener('click', () => {
            if (tabIndex > index && !validateThrough(tabIndex - 1)) {
                return;
            }
            clearMessage();
            activateStep(tabIndex);
        });
    });

    fetch('/api/setup/status')
        .then(response => response.json())
        .then(status => {
            if (status.completed) {
                location.replace('/login.html');
                return;
            }
            if (status.pendingCompletion) {
                if (status.pendingCompletionError) {
                    q('#setupForm').hidden = false;
                    showMessage(`Az automatikus setup-véglegesítés sikertelen: ${status.pendingCompletionError}`);
                    pendingCompletionWaitStarted = false;
                    return;
                }
                pendingCompletionWaitStarted = true;
                showMessage('A korábban megadott beállítások automatikus véglegesítése folyamatban. Kérjük, várjon...', 'success');
                waitForPendingCompletion();
                return;
            }
            q('#setupForm').hidden = false;
            q('#dataDirectory').value = status.defaultDataDirectory || '';
            currentSecurityMode = status.securityMode || 'MULTI_USER';
            const preset=status.installerPreset||{};
            installerAdminPasswordAvailable=Boolean(preset.hasAdminPassword);
            if(preset.adminUsername)q('#adminUsername').value=preset.adminUsername;
            if(preset.adminDisplayName)q('#adminDisplayName').value=preset.adminDisplayName;
            if(preset.adminEmail)q('#adminEmail').value=preset.adminEmail;
            if(installerAdminPasswordAvailable){
                q('#adminPassword').placeholder='A Windows telepítőben megadva';
                q('#adminPasswordConfirmation').placeholder='A Windows telepítőben megadva';
            }
            const db=status.database||{};
            q('#databaseType').value=db.type||'H2';
            q('#databaseHost').value=db.host||'localhost';
            q('#databasePort').value=db.port||'';
            q('#databaseName').value=db.databaseName||'';
            q('#databaseUsername').value=db.username||((db.type||'H2')==='H2'?'sa':'nav_user');
            q('#databaseSchema').value=db.schema||'';
            applyDatabaseDefaults(false);
            render();
        })
        .catch(error => {
            q('#setupForm').hidden = false;
            showMessage(`A setup állapota nem tölthető be: ${error.message || error}`);
            render();
        });


    async function waitForPendingCompletion() {
        const deadline = Date.now() + 120000;
        let missingPendingSince = null;
        while (Date.now() < deadline) {
            await new Promise(resolve => window.setTimeout(resolve, 750));
            try {
                const response = await fetch('/api/setup/status', {cache: 'no-store'});
                if (!response.ok) {
                    continue;
                }
                const status = await response.json();
                if (status.completed) {
                    location.replace('/login.html');
                    return;
                }
                if (status.pendingCompletion) {
                    if (status.pendingCompletionError) {
                        q('#setupForm').hidden = false;
                        showMessage(`Az automatikus setup-véglegesítés sikertelen: ${status.pendingCompletionError}`);
                        pendingCompletionWaitStarted = false;
                        render();
                        return;
                    }
                    missingPendingSince = null;
                    continue;
                }
                if (missingPendingSince === null) {
                    missingPendingSince = Date.now();
                    continue;
                }
                if (Date.now() - missingPendingSince >= 5000) {
                    q('#setupForm').hidden = false;
                    showMessage('Az automatikus setup-véglegesítés nem fejeződött be. A beállítások helyreállítási módban ellenőrizhetők és újra menthetők.');
                    pendingCompletionWaitStarted = false;
                    render();
                    return;
                }
            } catch (error) {
                // Az újraindítás közbeni átmeneti kapcsolatvesztés normális; tovább próbálkozunk.
            }
        }
        q('#setupForm').hidden = false;
        showMessage('Az automatikus setup-véglegesítés nem fejeződött be időben. Ellenőrizze a naplót, majd szükség esetén mentse újra a beállításokat.');
        pendingCompletionWaitStarted = false;
        render();
    }

        /**
     * A <code>waitForRestart</code> függvény a oldalspecifikus felhasználói felület- folyamat egy önálló feldolgozási lépését valósítja meg.
     *
     * <p>A függvény mellékhatása lehet DOM- vagy runtime-state módosítás; a hívó a visszatérési értéket és az aszinkron befejeződést a konkrét hívási kontextus szerint kezeli.</p>
     * @param {*} completedAfterRestart a függvény completedAfterRestart bemeneti értéke
     * @returns {Promise<void>} a folyamat befejeződését jelző Promise
     */
async function waitForRestart(completedAfterRestart) {
        const saveButton = q('#setupSave');
        const nextButton = q('#setupNext');
        const backButton = q('#setupBack');
        saveButton.disabled = true;
        nextButton.disabled = true;
        backButton.disabled = true;

        let connectionWasLost = false;
        let missingPendingSince = null;
        const deadline = Date.now() + 120000;

        while (Date.now() < deadline) {
            await new Promise(resolve => window.setTimeout(resolve, 1000));
            try {
                const response = await fetch('/api/setup/status', {cache: 'no-store'});
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}`);
                }
                const status = await response.json();
                if (!connectionWasLost) {
                    continue;
                }
                if (completedAfterRestart || status.completed) {
                    location.replace('/login.html');
                    return;
                }
                if (status.pendingCompletion) {
                    missingPendingSince = null;
                    showMessage('Az adatbázis elindult. A kezdeti beállítás automatikus véglegesítése folyamatban...', 'success');
                    continue;
                }
                // A Tomcat a Spring ApplicationRunner-ek előtt már fogadhat kérést. Nem töltjük
                // vissza azonnal a setup űrlapot, hanem rövid ideig várunk az automatikus véglegesítésre.
                if (missingPendingSince === null) {
                    missingPendingSince = Date.now();
                    continue;
                }
                if (Date.now() - missingPendingSince >= 5000) {
                    location.reload();
                    return;
                }
                continue;
            } catch (error) {
                connectionWasLost = true;
                showMessage('Az alkalmazás újraindítása folyamatban. Kérjük, várjon...', 'success');
            }
        }

        showMessage(
            'Az automatikus újraindítás nem fejeződött be időben. Indítsa el újra az M2M XML EDITOR alkalmazást.',
            'error'
        );
        backButton.disabled = false;
    }

    q('#setupForm').addEventListener('keydown', event => {
        if (event.key !== 'Enter' || event.isComposing) {
            return;
        }
        if (event.target instanceof HTMLButtonElement) {
            return;
        }
        event.preventDefault();
        if (index < steps.length - 1 && !q('#setupNext').disabled) {
            q('#setupNext').click();
        }
    });

    q('#setupSave').addEventListener('click', async () => {
        if (index !== steps.length - 1 || !validateThrough(2)) {
            return;
        }

        const payload = {
            dataDirectory: q('#dataDirectory').value.trim(),
            securityMode: mode(),
            databaseType: q('#databaseType').value,
            databaseHost: q('#databaseHost').value.trim(),
            databasePort: q('#databasePort').value.trim(),
            databaseName: q('#databaseName').value.trim(),
            databaseSchema: q('#databaseSchema').value.trim(),
            databaseUsername: q('#databaseType').value === 'H2' ? 'sa' : q('#databaseUsername').value.trim(),
            databasePassword: q('#databaseType').value === 'H2' ? '' : q('#databasePassword').value,
            adminUsername: q('#adminUsername').value.trim(),
            adminDisplayName: q('#adminDisplayName').value.trim(),
            adminEmail: q('#adminEmail').value.trim(),
            adminPassword: q('#adminPassword').value,
            adminPasswordConfirmation: q('#adminPasswordConfirmation').value,
            githubToken: q('#githubToken').value.trim(),
            m2mApiKey: q('#m2mApiKey').value.trim(),
            m2mClientId: q('#m2mClientId').value.trim(),
            m2mClientSecret: q('#m2mClientSecret').value.trim(),
            databaseTestToken
        };

        try {
            const response = await fetch('/api/setup/complete', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            });
            const body = await response.json().catch(() => ({}));
            showMessage(body.message || 'Ismeretlen hiba.', response.ok ? 'success' : 'error');
            if (response.ok) {
                q('#setupSave').disabled = true;
                q('#setupNext').disabled = true;
                if (body.restartScheduled) {
                    await waitForRestart(Boolean(body.completed));
                } else if (body.restartRequired) {
                    q('#setupBack').disabled = true;
                }
            }
        } catch (error) {
            showMessage(`A beállítás mentése nem sikerült: ${error.message || error}`);
        }
    });

    render();
})();
