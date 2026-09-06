package hu.gov.nav.xsdparsertool.pageschema.service;

import hu.gov.nav.xsdparsertool.core.model.definition.DocumentDefinition;

import java.nio.file.Path;

/**
 * A lapleíró séma feldolgozásának szolgáltatási szerződése.
 *
 * <p>A szolgáltatás a már XSD alapján felépített {@link DocumentDefinition}
 * objektumot egészítheti ki egy külön lapleíró ({@code .schema}) állományban
 * található megjelenítési vagy elrendezési információkkal. A feldolgozási
 * folyamatban ez a lépés az XSD feldolgozása és az opcionális UIModel
 * alkalmazása után hajtható végre.</p>
 *
 * <p>Az interfész jelenleg bővítési pontként szolgál. A rendelkezésre álló
 * alapértelmezett megvalósítás, a
 * {@link NoOpPageSchemaParserService}, szándékosan nem módosítja a
 * dokumentumdefiníciót.</p>
 */
public interface PageSchemaParserService {

    /**
     * Alkalmazza a megadott lapleíró séma információit a dokumentumdefinícióra.
     *
     * <p>A konkrét implementáció határozza meg, hogy a lapleíró állományból
     * milyen metaadatokat olvas ki, és azokat hogyan rendeli hozzá a
     * dokumentumdefinícióhoz.</p>
     *
     * @param definition a korábban XSD alapján létrehozott, kiegészítendő
     *                   dokumentumdefiníció
     * @param pageSchemaFile a feldolgozandó lapleíró sémafájl elérési útja
     */
    void applyPageSchema(DocumentDefinition definition, Path pageSchemaFile);
}
