package hu.gov.nav.xsdparsertool.processing.form;

import hu.gov.nav.xsdparsertool.core.model.form.FormData;
import hu.gov.nav.xsdparsertool.core.model.form.FormDefinition;

import java.nio.file.Path;


/**
 * Az űrlapdefiníció és egy konkrét XML alapján kitöltött {@code FormData} modellt építő szolgáltatás.
 *
 * <p>Az XML-útvonalak alapján mezőértékeket köt a definícióhoz, ismétlődő soroknál pedig
 * külön sorpéldányokat hoz létre.</p>
 */
public interface FormDataBuilderService {
/**
 * Kiolvassa az XML mezőértékeit a megadott űrlapdefiníció szerint.
 * @param formDefinition az űrlap szerkezeti és meződefiníciója
 * @param xmlFile a feldolgozandó XML állomány
 * @return a mezőértékeket és ismétlődő sorpéldányokat tartalmazó űrlapadat
 */
    FormData build(FormDefinition formDefinition, Path xmlFile);
}
