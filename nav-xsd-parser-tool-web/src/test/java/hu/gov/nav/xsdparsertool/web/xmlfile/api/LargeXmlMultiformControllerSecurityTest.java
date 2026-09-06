package hu.gov.nav.xsdparsertool.web.xmlfile.api;

import hu.gov.nav.xsdparsertool.web.xmlfile.dto.LargeXmlFragmentSaveRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.LargeXmlFragmentSaveService;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.LargeXmlMultiformPageService;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.XmlFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LargeXmlMultiformControllerSecurityTest {

    @Mock LargeXmlMultiformPageService pageService;
    @Mock LargeXmlFragmentSaveService fragmentSaveService;
    @Mock XmlFileService xmlFileService;

    @Test
    void rowsMustCheckPartnerAccessBeforeReadingData() throws Exception {
        doThrow(new AccessDeniedException("denied")).when(xmlFileService).requireCurrentUserAccess(31L);
        LargeXmlMultiformController controller = controller();

        assertThrows(AccessDeniedException.class, () -> controller.rows(31L, "Form_A", 0, 50, ""));

        verify(xmlFileService).requireCurrentUserAccess(31L);
        verifyNoInteractions(pageService, fragmentSaveService);
    }

    @Test
    void configurationStatusMustCheckPartnerAccessBeforeReadingData() {
        doThrow(new AccessDeniedException("denied")).when(xmlFileService).requireCurrentUserAccess(32L);
        LargeXmlMultiformController controller = controller();

        assertThrows(AccessDeniedException.class, () -> controller.configurationStatus(32L, "Form_A"));

        verify(xmlFileService).requireCurrentUserAccess(32L);
        verifyNoInteractions(pageService, fragmentSaveService);
    }

    @Test
    void fragmentSaveMustCheckPartnerAccessBeforeMutation() throws Exception {
        doThrow(new AccessDeniedException("denied")).when(xmlFileService).requireCurrentUserAccess(33L);
        LargeXmlMultiformController controller = controller();

        assertThrows(AccessDeniedException.class,
                () -> controller.saveFragment(33L, mock(LargeXmlFragmentSaveRequest.class)));

        verify(xmlFileService).requireCurrentUserAccess(33L);
        verifyNoInteractions(pageService, fragmentSaveService);
    }

    private LargeXmlMultiformController controller() {
        return new LargeXmlMultiformController(pageService, fragmentSaveService, xmlFileService);
    }
}
