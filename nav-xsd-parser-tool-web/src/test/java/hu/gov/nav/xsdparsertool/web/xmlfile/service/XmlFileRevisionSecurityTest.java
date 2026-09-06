package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.security.partneraccess.service.XmlAccessPolicyService;
import hu.gov.nav.xsdparsertool.web.security.service.CurrentUserService;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileRevisionEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.repository.*;
import hu.gov.nav.xsdparsertool.web.xsdvalidation.service.StreamingXsdValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class XmlFileRevisionSecurityTest {

    @Mock XmlFileRepository xmlFiles;
    @Mock XmlFileSessionRepository sessions;
    @Mock XmlFileLockRepository locks;
    @Mock XmlFileRevisionRepository revisions;
    @Mock XmlFileDiffEntryRepository diffs;
    @Mock CurrentUserService currentUser;
    @Mock AuditLogService audit;
    @Mock StreamingXsdValidationService xsdValidation;
    @Mock XmlFileStorageProperties storage;
    @Mock XmlAccessPolicyService accessPolicy;
    @Mock XmlMutationGuard mutationGuard;

    @Test
    void revisionLookupMustEnforceAccessBeforeReturningDiffEntries() {
        XmlFileEntity xml = new XmlFileEntity();
        xml.setId(90L);
        XmlFileRevisionEntity revision = new XmlFileRevisionEntity();
        revision.setId(901L);
        revision.setXmlFile(xml);
        when(RepositoryAccess.findById(revisions, 901L)).thenReturn(Optional.of(revision));
        doThrow(new AccessDeniedException("denied")).when(accessPolicy).requireCurrentUserAccess(xml);

        XmlFileSaveService service = new XmlFileSaveService(
                xmlFiles, sessions, locks, revisions, diffs, currentUser, audit,
                xsdValidation, storage, accessPolicy, mutationGuard);

        assertThrows(AccessDeniedException.class, () -> service.revision(901L));
        verify(accessPolicy).requireCurrentUserAccess(xml);
        verifyNoInteractions(diffs);
    }
}
