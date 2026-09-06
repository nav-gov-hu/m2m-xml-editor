package hu.gov.nav.xsdparsertool.web.certificate.api;

import hu.gov.nav.xsdparsertool.web.certificate.service.CertificateManagementService;
import hu.gov.nav.xsdparsertool.web.testsupport.TestSecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CertificateManagementController.class)
@ContextConfiguration(classes = {CertificateManagementController.class, TestSecurityConfiguration.class})
class CertificateManagementControllerAuthorizationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockBean CertificateManagementService service;

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorMayNotListCertificates() throws Exception {
        mockMvc.perform(get("/api/admin/certificates"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminMayListCertificates() throws Exception {
        when(service.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/certificates"))
                .andExpect(status().isOk());

        verify(service).list();
    }

    @Test
    @WithMockUser(username = "admin-user", roles = "ADMIN")
    void adminMayUseMultipartCertificateImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "client.p12", "application/x-pkcs12", new byte[]{1, 2, 3});
        when(service.importFile(any(), eq("secret"), eq("admin-user"))).thenReturn(List.of());

        mockMvc.perform(multipart("/api/admin/certificates/import")
                        .file(file)
                        .param("password", "secret"))
                .andExpect(status().isOk());

        verify(service).importFile(any(), eq("secret"), eq("admin-user"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerMayNotUseMultipartCertificateImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "client.p12", "application/x-pkcs12", new byte[]{1});

        mockMvc.perform(multipart("/api/admin/certificates/import").file(file))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }
}
