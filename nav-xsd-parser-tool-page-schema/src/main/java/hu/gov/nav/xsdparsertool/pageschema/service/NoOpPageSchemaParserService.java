package hu.gov.nav.xsdparsertool.pageschema.service;

import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;

import java.nio.file.Path;

/**
 * A {@link PageSchemaParserService} semleges, módosítást nem végző
 * implementációja.
 *
 * <p>Az osztály akkor használható, amikor a feldolgozási pipeline-nak szüksége
 * van {@link PageSchemaParserService} példányra, de tényleges lapleíró-séma
 * feldolgozás nincs bekapcsolva vagy még nincs implementálva. Ez a modul
 * alapértelmezett megvalósítása.</p>
 *
 * <p>Az {@link #applyPageSchema(DocumentDefinition, Path)} metódus szándékosan
 * nem módosítja a kapott dokumentumdefiníciót, és a lapleíró fájlt sem olvassa
 * be. Ez biztosítja, hogy a page-schema lépés opcionálisan illeszthető legyen a
 * teljes XML-feldolgozási folyamatba.</p>
 */
public class NoOpPageSchemaParserService implements PageSchemaParserService {
    /**
     * Változtatás nélkül hagyja a dokumentumdefiníciót.
     *
     * <p>A metódus a no-op implementáció része, ezért a paramétereket nem
     * dolgozza fel. A metódus jelenléte lehetővé teszi, hogy a processing modul
     * egységesen hívja a page-schema szolgáltatást akkor is, ha nincs aktív
     * lapleíró-feldolgozó implementáció.</p>
     *
     * @param definition a dokumentumdefiníció, amelyet ez az implementáció nem
     *                   módosít
     * @param pageSchemaFile a lapleíró sémafájl elérési útja, amelyet ez az
     *                       implementáció nem olvas be
     */
    @Override
    public void applyPageSchema(DocumentDefinition definition, Path pageSchemaFile) {
        // A no-op implementáció szándékosan nem alkalmaz lapleíró metaadatokat.
    }
}
