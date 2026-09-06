package hu.gov.nav.xsdparsertool.web.xmlfile.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hu.gov.nav.xsdparsertool.web.setup.SetupStateService;
import hu.gov.nav.xsdparsertool.web.xmlfile.config.XmlFileStorageProperties;
import hu.gov.nav.xsdparsertool.web.xmlfile.service.XmlFileService;

@ExtendWith(MockitoExtension.class)
class XmlServerFolderAutoRegistrationSchedulerTest {

    @Mock XmlFileService xmlFileService;
    @Mock SetupStateService setupStateService;

    @Test
    void scheduledScanIsSkippedUntilInitialSetupIsCompleted() throws Exception {
        XmlFileStorageProperties properties = new XmlFileStorageProperties();
        properties.getServerBrowser().setEnabled(true);
        properties.getServerBrowser().setAutoRegisterEnabled(true);
        properties.getServerImport().setRootDir(null);
        when(setupStateService.isCompleted()).thenReturn(false);

        XmlServerFolderAutoRegistrationScheduler scheduler =
                new XmlServerFolderAutoRegistrationScheduler(xmlFileService, properties, setupStateService);

        scheduler.runScheduledScan();

        verify(xmlFileService, never()).autoRegisterServerFiles("system-background");
    }
}
