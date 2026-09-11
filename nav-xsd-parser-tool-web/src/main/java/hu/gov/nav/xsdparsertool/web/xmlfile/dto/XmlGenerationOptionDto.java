package hu.gov.nav.xsdparsertool.web.xmlfile.dto;

import java.util.List;

/**
 * Az Új XML párbeszédablakban választható űrlaptípus és verziók.
 *
 * @param formType az űrlap technikai azonosítója
 * @param versions az elérhető űrlapverziók
 */
public record XmlGenerationOptionDto(String formType, List<String> versions) {
}
