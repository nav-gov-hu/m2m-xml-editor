package hu.gov.nav.xsdparsertool.web.systemconfig.api;

import hu.gov.nav.xsdparsertool.web.security.SecurityModeProperties;
import hu.gov.nav.xsdparsertool.web.setup.ApplicationRestartService;
import hu.gov.nav.xsdparsertool.web.systemconfig.service.SystemConfigurationService;
import hu.gov.nav.xsdparsertool.web.systemconfig.transfer.ConfigurationTransferService;
import hu.gov.nav.xsdparsertool.web.testsupport.TestSecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SystemConfigurationController.class)
@ContextConfiguration(classes = {SystemConfigurationController.class, TestSecurityConfiguration.class})
class SystemConfigurationControllerAuthorizationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockBean SystemConfigurationService service;
    @MockBean ApplicationRestartService restartService;
    @MockBean SecurityModeProperties securityModeProperties;
    @MockBean ConfigurationTransferService transferService;

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorMayNotReadSystemConfiguration() throws Exception {
        mockMvc.perform(get("/api/admin/configuration"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminMayReadSystemConfiguration() throws Exception {
        when(service.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/configuration"))
                .andExpect(status().isOk());

        verify(service).list();
    }
}
