package hu.gov.nav.xsdparsertool.web.xmlfile.api;

import hu.gov.nav.xsdparsertool.web.testsupport.TestSecurityConfiguration;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.LargeXmlFragmentSaveService;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.LargeXmlMultiformPageService;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.XmlFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LargeXmlMultiformController.class)
@ContextConfiguration(classes = {LargeXmlMultiformController.class, TestSecurityConfiguration.class})
class LargeXmlMultiformAuthorizationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockBean LargeXmlMultiformPageService pageService;
    @MockBean LargeXmlFragmentSaveService fragmentSaveService;
    @MockBean XmlFileService xmlFileService;

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerMayReadLargeXmlRows() throws Exception {
        mockMvc.perform(get("/api/xml-files/41/large-multiform/rows")
                        .param("formName", "Form_A"))
                .andExpect(status().isOk());

        verify(xmlFileService).requireCurrentUserAccess(41L);
        verify(pageService).page(41L, "Form_A", 0, 50, "");
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerMayNotSaveLargeXmlFragment() throws Exception {
        mockMvc.perform(post("/api/xml-files/41/large-multiform/save-fragment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(fragmentSaveService);
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorMayReachLargeXmlFragmentSaveAfterPartnerCheck() throws Exception {
        mockMvc.perform(post("/api/xml-files/41/large-multiform/save-fragment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(xmlFileService).requireCurrentUserAccess(41L);
        verify(fragmentSaveService).start(eq(41L), any());
    }
}
