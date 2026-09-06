package hu.gov.nav.xsdparsertool.print.model;

import java.nio.file.Path;

/**
 * A nyomtatható HTML- és PDF-kimenet előállítását befolyásoló beállításokat tartalmazza.
 *
 * <p>Az objektum a nyomtatási szolgáltatás számára átadott, opcionális paramétereket fogja össze.
 * Segítségével szabályozható a technikai mezőazonosítók megjelenítése, az üres mezők elhagyása,
 * egy konkrét UIModel állomány felülbírálásként történő használata, valamint a nyomtatási fejlécben
 * feltüntetendő alkalmazásverzió.</p>
 *
 * <p>Ha a hívó fél nem ad át {@code PrintOptions} példányt, a nyomtatási szolgáltatás
 * alapértelmezett beállításokkal dolgozik.</p>
 */
public class PrintOptions {
    /**
     * Jelzi, hogy a nyomtatási képben meg kell-e jeleníteni a mezők technikai azonosítóit.
     */
    private boolean showFieldIds;
    /**
     * Jelzi, hogy a nyomtatási kép csak a kitöltött mezőket tartalmazza-e.
     */
    private boolean onlyFilledFields;
    /**
     * Opcionális UIModel állomány, amely a sémafeloldás során meghatározott UIModel helyett használható.
     */
    private Path uiModelOverrideFile;
    /**
     * A nyomtatási metaadatok között feltüntetendő alkalmazásverzió.
     */
    private String appVersion;

    /**
     * Visszaadja, hogy a mezők technikai azonosítói megjelenjenek-e a nyomtatási képben.
     *
     * @return {@code true}, ha a mezőazonosítókat meg kell jeleníteni; egyébként {@code false}
     */
    public boolean isShowFieldIds() { return showFieldIds; }
    /**
     * Beállítja a mezők technikai azonosítóinak nyomtatási megjelenítését.
     *
     * @param showFieldIds {@code true} esetén a mezőazonosítók is bekerülnek a nyomtatási képbe
     */
    public void setShowFieldIds(boolean showFieldIds) { this.showFieldIds = showFieldIds; }
    /**
     * Visszaadja, hogy a nyomtatási kép kizárólag a kitöltött mezőket tartalmazza-e.
     *
     * @return {@code true}, ha az üres mezőket el kell hagyni; egyébként {@code false}
     */
    public boolean isOnlyFilledFields() { return onlyFilledFields; }
    /**
     * Beállítja, hogy az üres mezők kimaradjanak-e a nyomtatási képből.
     *
     * @param onlyFilledFields {@code true} esetén csak a kitöltött mezők kerülnek a kimenetbe
     */
    public void setOnlyFilledFields(boolean onlyFilledFields) { this.onlyFilledFields = onlyFilledFields; }
    /**
     * Visszaadja a nyomtatáshoz használni kívánt UIModel felülbíráló állományt.
     *
     * @return a felülbíráló UIModel elérési útja, vagy {@code null}, ha nincs megadva
     */
    public Path getUiModelOverrideFile() { return uiModelOverrideFile; }
    /**
     * Beállítja a nyomtatás során elsőbbséget élvező UIModel állományt.
     *
     * <p>A tényleges használhatóságot a nyomtatási szolgáltatás ellenőrzi; csak létező,
     * szabályos és olvasható fájl használható felülbírálásként.</p>
     *
     * @param uiModelOverrideFile a felülbíráló UIModel elérési útja; {@code null} esetén nincs felülbírálás
     */
    public void setUiModelOverrideFile(Path uiModelOverrideFile) { this.uiModelOverrideFile = uiModelOverrideFile; }
    /**
     * Visszaadja a nyomtatási metaadatok között megjelenítendő alkalmazásverziót.
     *
     * @return az alkalmazás verziószövege, vagy {@code null}, ha nincs külön megadva
     */
    public String getAppVersion() { return appVersion; }
    /**
     * Beállítja a nyomtatási metaadatok között megjelenítendő alkalmazásverziót.
     *
     * @param appVersion a megjelenítendő verziószöveg
     */
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
}

