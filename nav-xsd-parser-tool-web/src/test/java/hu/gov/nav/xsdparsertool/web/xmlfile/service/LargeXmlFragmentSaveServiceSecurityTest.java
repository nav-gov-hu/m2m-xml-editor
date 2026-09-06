package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.web.processing.service.ProcessingJobService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.dto.LargeXmlFragmentSaveRequest;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileLockRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileRepository;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.XmlFileSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LargeXmlFragmentSaveServiceSecurityTest {

    @Mock XmlFileRepository xmlFiles;
    @Mock XmlFileSessionRepository sessions;
    @Mock XmlFileLockRepository locks;
    @Mock CurrentUserService currentUser;
    @Mock ProcessingJobService jobs;
    @Mock LargeXmlMultiformPageService pages;
    @Mock XmlFileStorageProperties storage;
    @Mock XmlMutationGuard mutationGuard;

    @Test
    void finalStateGuardMustRunBeforeAnyFragmentSaveWork() throws Exception {
        doThrow(new IllegalStateException("final")).when(mutationGuard).requireMutable(77L);
        LargeXmlFragmentSaveService service = new LargeXmlFragmentSaveService(
                xmlFiles, sessions, locks, currentUser, jobs, pages, storage, mutationGuard);

        assertThrows(IllegalStateException.class, () -> service.start(77L, mock(LargeXmlFragmentSaveRequest.class)));

        verify(mutationGuard).requireMutable(77L);
        verifyNoInteractions(xmlFiles, sessions, locks, currentUser, jobs, pages, storage);
    }
}
