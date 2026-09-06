package hu.gov.nav.xsdparsertool.processing.xmlview;

import hu.gov.nav.xsdparsertool.core.model.xmlview.XmlDocumentView;

import java.nio.file.Path;


/**
 * XML állományból nyers forrást és navigálható fa-struktúrát készítő szolgáltatás.
 *
 * <p>A fa előfordulási indexekkel kiegészített XML-útvonalakat használ, így azonos nevű
 * testvérelemek között is egyértelmű marad a navigáció.</p>
 */
public interface XmlViewBuilderService {
/**
 * Felépíti az XML dokumentum megjelenítési modelljét.
 * @param xmlFile a beolvasandó XML állomány
 * @return a nyers XML-t és a fa gyökérelemét tartalmazó nézetmodell
 */
    XmlDocumentView build(Path xmlFile);
}
