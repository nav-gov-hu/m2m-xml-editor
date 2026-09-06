package hu.gov.nav.xsdparsertool.web.xmlfile.api;

import hu.gov.nav.xsdparsertool.web.testsupport.TestSecurityConfiguration;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.ServerFileBrowserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.XmlFileSaveService;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.XmlFileService;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.XmlFileSessionService;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.XmlMutationGuard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = XmlFileController.class)
@ContextConfiguration(classes = {XmlFileController.class, TestSecurityConfiguration.class})
class XmlFileControllerAuthorizationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockBean XmlFileService xmlFileService;
    @MockBean XmlFileSessionService xmlFileSessionService;
    @MockBean ServerFileBrowserService serverFileBrowserService;
    @MockBean XmlFileSaveService xmlFileSaveService;
    @MockBean XmlMutationGuard mutationGuard;

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerMayReadRevisions() throws Exception {
        when(xmlFileSaveService.revisions(5L)).thenReturn(List.of());

        mockMvc.perform(get("/api/xml-files/5/revisions"))
                .andExpect(status().isOk());

        verify(xmlFileService).requireCurrentUserAccess(5L);
        verify(xmlFileSaveService).revisions(5L);
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerMayNotOverwriteXml() throws Exception {
        mockMvc.perform(post("/api/xml-files/5/overwrite"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(mutationGuard, xmlFileSaveService);
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorMayOverwriteXmlAfterAccessAndFinalStateChecks() throws Exception {
        mockMvc.perform(post("/api/xml-files/5/overwrite"))
                .andExpect(status().isOk());

        verify(xmlFileService).requireCurrentUserAccess(5L);
        verify(mutationGuard).requireMutable(5L);
        verify(xmlFileSaveService).overwrite(eq(5L), any());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorMayNotPermanentlyDeleteXml() throws Exception {
        mockMvc.perform(delete("/api/xml-files/5/permanent"))
                .andExpect(status().isForbidden());

        verify(xmlFileService, never()).permanentlyDelete(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminMayPermanentlyDeleteXml() throws Exception {
        mockMvc.perform(delete("/api/xml-files/5/permanent"))
                .andExpect(status().isOk());

        verify(xmlFileService).permanentlyDelete(5L, null);
    }

    @Test
    @WithMockUser(roles = "FILE_DELETE")
    void fileDeleteRoleMayPhysicallyArchiveXml() throws Exception {
        mockMvc.perform(post("/api/xml-files/5/physical-archive"))
                .andExpect(status().isOk());

        verify(xmlFileService).requireCurrentUserAccess(5L);
        verify(xmlFileService).physicalArchive(5L, null);
    }
}
