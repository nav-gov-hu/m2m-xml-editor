package hu.gov.nav.xsdparsertool.web.xmlfile.service;

import hu.nav.m2m.submitter.domain.SubmissionStatus;
import hu.nav.m2m.submitter.repo.M2mSubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XmlMutationGuardTest {

    @Mock M2mSubmissionRepository submissions;

    @Test
    void submittedOkXmlMustBeImmutable() {
        when(submissions.existsByXmlFileIdAndInternalStatus(11L, SubmissionStatus.SUBMITTED_OK)).thenReturn(true);
        XmlMutationGuard guard = new XmlMutationGuard(submissions);

        assertThrows(IllegalStateException.class, () -> guard.requireMutable(11L));
    }

    @Test
    void nonFinalXmlMayBeModified() {
        when(submissions.existsByXmlFileIdAndInternalStatus(12L, SubmissionStatus.SUBMITTED_OK)).thenReturn(false);
        XmlMutationGuard guard = new XmlMutationGuard(submissions);

        assertDoesNotThrow(() -> guard.requireMutable(12L));
    }
}
